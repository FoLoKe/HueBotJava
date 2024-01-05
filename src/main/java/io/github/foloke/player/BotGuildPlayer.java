package io.github.foloke.player;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.object.entity.Message;
import discord4j.voice.AudioProvider;
import io.github.foloke.BotGuildPlayerUpdater;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

/**
 * Class to hold main information about guild player
 *
 * @author Dmitry Marchenko
 * @since 04.02.2023
 */
public final class BotGuildPlayer extends AudioProvider {
	private static final Logger logger = Logger.getLogger(BotGuildPlayer.class.getName());
	public static final float DEFAULT_VOLUME = 2;
	private static final float MAX_VOLUME = 16;
	private static final float VOLUME_STEP = MAX_VOLUME / 20;
	public static final int REWIND_DELAY = 3000;
	private static final int VOLUME_LERP_DELAY = 50;
	private final String guildId;
	private final AudioPlayer audioPlayer;
	private final AudioPlayerManager playerManager;
	private final BotQueue botQueue;
	private final BotGuildPlayerUpdater botGuildPlayerUpdater;
	private final MutableAudioFrame frame = new MutableAudioFrame();
	private float volume = DEFAULT_VOLUME;
	private final Executor threadPoolExecutor = new ScheduledThreadPoolExecutor(10);

	public BotQueue getBotQueue() {
		return botQueue;
	}

	public long getRewindTime() {
		return rewindTime;
	}

	public void setRewindTime(long rewindTime) {
		this.rewindTime = rewindTime;
	}

	private long rewindTime;

	private BotRepeatState botRepeatState = BotRepeatState.NONE;
	private final String motd;

	/**
	 * Creates a player instance associated with a guild.
	 **/
	public BotGuildPlayer(String guildId, String motd) {
		super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
		this.guildId = guildId;
		playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);
		playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
		audioPlayer = playerManager.createPlayer();
		botQueue = new BotQueue(audioPlayer, this);
		audioPlayer.addListener(botQueue);
		audioPlayer.setVolume((int) volume);
		botGuildPlayerUpdater = new BotGuildPlayerUpdater(this);
		botGuildPlayerUpdater.start();
		frame.setBuffer(getBuffer());
		this.motd = motd;
	}

	@Override
	public boolean provide() {
		boolean didProvide = audioPlayer.provide(frame);
		if (didProvide) {
			getBuffer().flip();
		}
		return didProvide;
	}

	/**
	 * Add track or playlist to queue by link
	 */
	public void addToQueue(String link) {
		playerManager.loadItem(link, botQueue);
	}

	/**
	 * Start player (play\pause or unpause)
	 */
	public void play() {
		if (audioPlayer.isPaused()) {
			audioPlayer.setPaused(false);
		} else if (audioPlayer.getPlayingTrack() != null) {
			audioPlayer.setPaused(true);
		} else {
			botQueue.next();
		}
	}

	/**
	 * Skip previous track
	 */
	public void skip() {
		botQueue.next();
		rewindTime = REWIND_DELAY;
	}

	/**
	 * Play previous track
	 */
	public void playPrevious() {
		if (rewindTime > 0) {
			botQueue.previous();
		}
		rewindTime = REWIND_DELAY;
		botQueue.rewind();
	}

	/**
	 * Add volume by {@link #VOLUME_STEP} amount
	 */
	public void addVolume() {
		setVolume(volume + VOLUME_STEP);
	}

	/**
	 * Reduce volume by {@link #VOLUME_STEP} amount
	 */
	public void reduceVolume() {
		setVolume(volume - VOLUME_STEP);
	}

	/**
	 * Shuffle queue
	 */
	public void shuffle() {
		botQueue.shuffle(audioPlayer.getPlayingTrack());
	}

	/**
	 * Toggle "repeate queue"
	 */
	public void toggleRepeatQueue() {
		botRepeatState = BotRepeatState.getNextState(botRepeatState, BotRepeatState.REPEAT_QUEUE);
	}

	/**
	 * Toggle "repeat current track"
	 */
	public void toggleRepeatTrack() {
		botRepeatState = BotRepeatState.getNextState(botRepeatState, BotRepeatState.REPEAT);
	}

	/**
	 * Drop queue and stop player
	 */
	public void unload() {
		botQueue.clear();
	}

	/**
	 * Set volume by percent
	 */
	public void setVolumePercent(float percentAmount) {
		setVolume(percentAmount / 100 * MAX_VOLUME);
	}

	/**
	 * Set volume by amount
	 */
	public void setVolume(float amount) {
		volume = Math.max(0, Math.min(MAX_VOLUME, amount));
		int currentVolume = audioPlayer.getVolume();
		threadPoolExecutor.execute(
			() -> {
				int segments = (int) Math.abs((currentVolume - volume) / MAX_VOLUME * 100);
				IntStream.rangeClosed(1, segments).forEach(i -> {
					try {
						Thread.sleep(VOLUME_LERP_DELAY);
						float target = (1 - (float) i / segments) * currentVolume + (float) i / segments * volume;
						audioPlayer.setVolume((int)target);
					} catch (InterruptedException e) {
						logger.log(Level.SEVERE, "Volume cahnge interrupted", e);
						Thread.currentThread().interrupt();
					}
				});
			});
	}

	public float getVolume() {
		return volume;
	}

	public float getVolumePercent() {
		return getVolume() / MAX_VOLUME  * 100;
	}

	public void setMessage(Message message) {
		botGuildPlayerUpdater.setMessage(message);
	}

	public Message getMessage() {
		return botGuildPlayerUpdater.getMessage();
	}

	public BotRepeatState getBotRepeatState() {
		return botRepeatState;
	}

	public AudioTrack getCurrentTrack() {
		return audioPlayer.getPlayingTrack();
	}

	public int getWholeQueueLen() {
		return botQueue.getWholeQueueLen();
	}

	public int getPlayedQueueLen() {
		return botQueue.getPlayedQueueLen();
	}

	public String getGuildId() {
		return guildId;
	}

	public String getLink() {
		return botQueue.getLink();
	}

	public boolean isPaused() {
		return audioPlayer.isPaused();
	}

	public String getMotd() {
		return motd;
	}
}
