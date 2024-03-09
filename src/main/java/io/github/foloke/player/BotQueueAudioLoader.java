package io.github.foloke.player;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import io.github.foloke.spring.services.localization.BotLocalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Audio load handler with exceptions messages save
 *
 * @author Марченко Дмитрий
 * @since 08.03.2024
 */
public class BotQueueAudioLoader implements AudioLoadResultHandler {
	private final Logger log = LoggerFactory.getLogger(getClass().getName());
	private static final String LOADED_LOG_MESSAGE_FORMAT = "Loaded: %s";
	private final List<String> errors = new ArrayList<>();
	private final BotQueue botQueue;
	private final BotLocalization playerLocalization;

	public BotQueueAudioLoader(BotQueue botQueue, BotLocalization playerLocalization) {
		this.botQueue = botQueue;
		this.playerLocalization = playerLocalization;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		botQueue.addLoadedTrack(track);
		log.info(String.format(LOADED_LOG_MESSAGE_FORMAT, track.getInfo().title));
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		playlist.getTracks().forEach(botQueue::addLoadedTrack);
		log.info("Playlist loaded");
	}

	@Override
	public void noMatches() {
		log.info("No matching track, probably playlist");
		errors.add(playerLocalization.getMessage("track_not_found"));
	}

	@Override
	public void loadFailed(FriendlyException exception) {
		log.error("Load failed", exception);
		errors.add(exception.getMessage());
	}

	public List<String> getErrors() {
		return Collections.unmodifiableList(errors);
	}
}
