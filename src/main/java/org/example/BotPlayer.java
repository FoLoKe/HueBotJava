package org.example;

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

import java.nio.ByteBuffer;

/**
 * Class to hold mani information about guild player
 *
 * @author Dmitry Marchenko
 * @since 04.02.2023
 */
public final class BotPlayer extends AudioProvider {
	public static final int DEFAULT_VOLUME = 20;
	public static final int REWIND_DELAY = 3000;
	private final String guildId;
	private final AudioPlayer audioPlayer;
	private final AudioPlayerManager playerManager;
	private final BotQueue botQueue;
	private final BotGifUpdater botGifUpdater;
	private final MutableAudioFrame frame = new MutableAudioFrame();
	private int volume = DEFAULT_VOLUME;

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

	private BotRepeatState botRepeatState = BotRepeatState.none;

	/**
	 * Creates a player instance associated with a guild.
	 **/
	public BotPlayer(String guildId) {
		super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
		this.guildId = guildId;
		playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);
		playerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
		audioPlayer = playerManager.createPlayer();
		botQueue = new BotQueue(audioPlayer, this);
		audioPlayer.addListener(botQueue);
		audioPlayer.setVolume(volume);
		botGifUpdater = new BotGifUpdater(this);
		botGifUpdater.start();
		frame.setBuffer(getBuffer());
	}

	@Override
	public boolean provide() {
		boolean didProvide = audioPlayer.provide(frame);
		if (didProvide) {
			getBuffer().flip();
		}
		return didProvide;
	}

	public void addToQueue(String link) {
		playerManager.loadItem(link, botQueue);
	}

	public void play() {
		if (audioPlayer.isPaused()) {
			audioPlayer.setPaused(false);
		} else if (audioPlayer.getPlayingTrack() != null) {
			audioPlayer.setPaused(true);
		} else {
			botQueue.next();
		}
	}

	public void skip() {
		botQueue.next();
		rewindTime = REWIND_DELAY;
	}

	public void prev() {
		if (rewindTime > 0) {
			botQueue.previous();
		}
		rewindTime = REWIND_DELAY;
		botQueue.rewind();
	}

	public void addVolume(int amount) {
		setVolume(volume += amount);
	}

	public void shuffle() {
		botQueue.shuffle(audioPlayer.getPlayingTrack());
	}

	public void repeatQ() {
		botRepeatState = BotRepeatState.getNextState(botRepeatState, BotRepeatState.repeatQ);
	}

	public void repeat() {
		botRepeatState = BotRepeatState.getNextState(botRepeatState, BotRepeatState.repeat);
	}

	public void unload() {
		botQueue.clear();
	}

	public void setVolume(int amount) {
		volume = Math.max(0, Math.min(100, amount));
		audioPlayer.setVolume(volume);
	}

	public int getVolume() {
		return volume;
	}

	public void linkMessage(Message message) {
		botGifUpdater.setMessage(message);
		botGifUpdater.unlockEdit();
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
}
