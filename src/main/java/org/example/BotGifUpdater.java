package org.example;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageCreateFields.File;
import discord4j.discordjson.json.MessageEditRequest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thread to update guild player. Every {@link this#DELAY_MILLIS} checks if there is anything updated and update and
 * creates new gif image if was.
 *
 * @author Dmitry Marchenko
 * @since 05.02.2023
 */
public class BotGifUpdater extends Thread {
	public static final int DELAY_MILLIS = 1000;
	private final BotPlayer botPlayer;
	private boolean editLock = true;
	private Message message;
	private boolean started = true;
	private BotPlayState previousBotPlayState = BotPlayState.stop;
	private BotRepeatState previousBotRepeatState = BotRepeatState.none;
	private int previousQueueLen;
	private int previousQueuePosition;
	private int previousVolume;
	private String previousTrack;

	/**
	 * Creates daemon thread to update player visuals (gif attachment). Use {@link this#setMessage(Message)} to
	 * attach updating image and {@link this#unlockEdit} to start updating message;
	 */
	public BotGifUpdater(BotPlayer botPlayer) {
		this.botPlayer = botPlayer;
		setDaemon(true);
	}

	@Override
	public void run() {
		while (!isInterrupted()) {
			try {
				Thread.sleep(DELAY_MILLIS);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			AudioTrack audioTrack = botPlayer.getCurrentTrack();

			botPlayer.setRewindTime(Math.max(0, botPlayer.getRewindTime() - DELAY_MILLIS));

			if (!editLock && isNeedUpdate()) {
				previousBotRepeatState = botPlayer.getBotRepeatState();
				previousBotPlayState = getPlayerState();
				previousQueueLen = botPlayer.getWholeQueueLen();
				previousQueuePosition = botPlayer.getPlayedQueueLen();
				previousTrack = botPlayer.getLink();
				previousVolume = botPlayer.getVolume();
				started = false;
				try {
					String text = previousQueueLen == 0 ?
						"Use the /q command to add YouTube tracks" :
						audioTrack == null ?
							"Hit that paly button! Or use the /q command to add more YouTube tracks" :
							audioTrack.getInfo().title;
					InputStream gifImageInputStream = new BotGifRenderer().makeAGifAsInputStream(
						previousQueuePosition,
						previousQueueLen,
						previousVolume,
						text,
						"ui/player.png",
						previousBotPlayState.getImage(),
						previousBotRepeatState.getImage()
					);


					List<File> fileList = new ArrayList<>();
					fileList.add(File.of("ui.gif", gifImageInputStream));
					// delete previous attachment file
					message.getRestMessage().edit(MessageEditRequest.builder()
						.attachments(new ArrayList<>()).build()).block();
					message = message.edit().withFiles(fileList).block();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (audioTrack != null) {
				System.out.println(
					"BotPlayer for guild "
						+ botPlayer.getGuildId()
						+ " is "
						+ audioTrack.getState()
						+ " at position "
						+ audioTrack.getPosition()
						+ " track \""
						+ audioTrack.getInfo().title
						+ "\""
				);
			}
		}
	}

	private boolean isNeedUpdate() {
		return started
			|| previousBotPlayState != getPlayerState()
			|| previousBotRepeatState != botPlayer.getBotRepeatState()
			|| !Objects.equals(previousTrack, botPlayer.getLink())
			|| previousQueueLen != botPlayer.getWholeQueueLen()
			|| previousQueuePosition != botPlayer.getPlayedQueueLen()
			|| previousVolume != botPlayer.getVolume();
	}

	/**
	 * Unlocks edidting of the message attached to this BotPlayer updater thread
	 */
	public void unlockEdit() {
		editLock = false;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public Message getMessage() {
		return message;
	}

	public BotPlayState getPlayerState() {
		AudioTrack audioTrack = botPlayer.getCurrentTrack();
		if (audioTrack != null) {
			if (botPlayer.isPaused()) {
				return BotPlayState.pause;
			}
			return BotPlayState.play;
		}
		return BotPlayState.stop;
	}
}
