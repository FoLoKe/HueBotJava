package io.github.foloke.player;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateFields.File;
import discord4j.rest.http.client.ClientException;
import io.github.foloke.BotResourceHandler;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread to update guild player. Every {@link this#DELAY_MILLIS} checks if there is anything updated and update and
 * creates new gif image if was.
 *
 * @author Dmitry Marchenko
 * @since 05.02.2023
 */
public class BotGuildPlayerUpdater extends Thread {
	public static final int DELAY_MILLIS = 1000;
	private static final String BG_UI_PLAYER_PNG = "ui/player.png";
	private static final String DEFAULT_GIF_NAME = "ui.gif";
	private static final String UPDATE_THREAD_STARTED_LOG_MESSAGE = "Update thread started: %s";
	private static final String PLAYING_MESSAGE = "BotPlayer for guild %s is %s at position %s, track: \"%s\"";
	private final Logger log = LoggerFactory.getLogger(getClass().getName() + ": " + Thread.currentThread().getName());
	private final BotGuildPlayer botGuildPlayer;
	private final Lock messageEditMutex = new ReentrantLock();
	private Message message;
	private boolean started = true;
	private BotPlayState previousBotPlayState = BotPlayState.STOP;
	private BotRepeatState previousBotRepeatState = BotRepeatState.NONE;
	private int previousQueueLen;
	private int previousQueuePosition;
	private float previousVolume;
	private String previousTrack;

	private final BotLocalization playerLocalization;

	/**
	 * Creates daemon thread to update player visuals (gif attachment). Use {@link this#setMessage(Message)} to
	 * attach and start updating message;
	 */
	public BotGuildPlayerUpdater(BotGuildPlayer botGuildPlayer, BotLocalization playerLocalization) {
		this.playerLocalization = playerLocalization;
		this.botGuildPlayer = botGuildPlayer;
		setDaemon(true);
	}

	/**
	 * Starts low-priority thread to update "UI" gif-image for the player
	 */
	@Override
	public void run() {
		Thread.currentThread().setPriority(MIN_PRIORITY);
		log.info(String.format(UPDATE_THREAD_STARTED_LOG_MESSAGE, Thread.currentThread()));
		while (!isInterrupted()) {
			try {
				Thread.sleep(DELAY_MILLIS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			AudioTrack audioTrack = botGuildPlayer.getCurrentTrack();
			botGuildPlayer.setRewindTime(Math.max(0, botGuildPlayer.getRewindTime() - DELAY_MILLIS));

			if (message != null && isNeedUpdate()) {
				updatePlayerAndEditMessage(audioTrack);
			}
			if (audioTrack != null && !botGuildPlayer.isPaused()) {
				log.info(String.format(
					PLAYING_MESSAGE,
					botGuildPlayer.getGuildId(),
					audioTrack.getState(),
					audioTrack.getPosition(),
					audioTrack.getInfo().title
				));
			}
		}
	}

	private void updatePlayerAndEditMessage(AudioTrack audioTrack) {
		previousBotRepeatState = botGuildPlayer.getBotRepeatState();
		previousBotPlayState = getPlayerState();
		previousQueueLen = botGuildPlayer.getWholeQueueLen();
		previousQueuePosition = botGuildPlayer.getPlayedQueueLen();
		previousTrack = botGuildPlayer.getLink();
		previousVolume = botGuildPlayer.getVolume();
		started = false;
		try {
			String modtText = botGuildPlayer.getMotd();
			String helpText = modtText.isEmpty() ? playerLocalization.getMessage("player_help_message") : modtText;
			String playerText = audioTrack == null ? helpText : audioTrack.getInfo().title;
			InputStream gifImageInputStream = new BotPlayerGifBuilder(
				previousQueuePosition,
				previousQueueLen,
				botGuildPlayer.getVolumePercent(),
				playerText
			).setBackgroundImage(BotResourceHandler.getImageByPath(BG_UI_PLAYER_PNG))
				.setStateImage(previousBotPlayState.getImage())
				.setRepeatStateImage(previousBotRepeatState.getImage())
				.buildInputStream();
			List<File> fileList = new ArrayList<>();
			fileList.add(File.of(new Date().getTime() + DEFAULT_GIF_NAME, gifImageInputStream));
			messageEditMutex.lock();
			message = message.edit().withAttachmentsOrNull(new ArrayList<>()).withFiles(fileList).block();
			messageEditMutex.unlock();
		} catch (Exception e) {
			log.error("Update cycle error", e);
		}
	}

	private boolean isNeedUpdate() {
		return started
			|| previousBotPlayState != getPlayerState()
			|| previousBotRepeatState != botGuildPlayer.getBotRepeatState()
			|| !Objects.equals(previousTrack, botGuildPlayer.getLink())
			|| previousQueueLen != botGuildPlayer.getWholeQueueLen()
			|| previousQueuePosition != botGuildPlayer.getPlayedQueueLen()
			|| Float.compare(previousVolume, botGuildPlayer.getVolume()) != 0;
	}

	public void setMessage(Message message) {
		messageEditMutex.lock();
		if (this.message != null && !message.getId().equals(this.message.getId())) {
			log.info("new interaction, message replaced");
			try {
				this.message.delete().block();
			} catch (ClientException clientException) {
				if(clientException.getStatus() == HttpResponseStatus.NOT_FOUND) {
					log.info("Message alredy deleted");
				} else {
					throw clientException;
				}
			}

			started = true;
		}
		this.message = message;
		messageEditMutex.unlock();
	}

	public Message getMessage() {
		return message;
	}

	public BotPlayState getPlayerState() {
		AudioTrack audioTrack = botGuildPlayer.getCurrentTrack();
		if (audioTrack != null) {
			if (botGuildPlayer.isPaused()) {
				return BotPlayState.PAUSE;
			}
			return BotPlayState.PLAY;
		}
		return BotPlayState.STOP;
	}
}
