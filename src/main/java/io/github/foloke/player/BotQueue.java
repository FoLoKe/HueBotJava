package io.github.foloke.player;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.FINISHED;
import static com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.LOAD_FAILED;

/**
 * BotPlayer queue and play\load events handler
 *
 * @author Dmitry Marchenko
 * @since 04.02.2023
 */
public class BotQueue extends AudioEventAdapter implements AudioLoadResultHandler {

	private final AudioPlayer player;
	/**
	 * Added tracks (Used for queue repeat)
	 */
	private final LinkedList<AudioTrack> queue = new LinkedList<>();
	/**
	 * Current working queue
	 */
	private final LinkedList<AudioTrack> workQueue = new LinkedList<>();
	/**
	 * Previous played tracks
	 */
	private final LinkedList<AudioTrack> previousQueue = new LinkedList<>();
	private final Logger logger = Logger.getLogger(getClass().getName());
	private final BotGuildPlayer botGuildPlayer;
	private Optional<AudioTrack> lastTrack;

	/**
	 * Creates queue instance to manage player
	 */
	public BotQueue(AudioPlayer player, BotGuildPlayer botGuildPlayer) {
		this.player = player;
		this.botGuildPlayer = botGuildPlayer;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		queue.add(track);
		workQueue.add(track);
		logger.info(() -> "Palying: " + player.getPlayingTrack().getInfo().title);
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		playlist.getTracks().forEach(track -> {
			queue.add(track);
			workQueue.add(track);
		});
		logger.info(() -> "Playlist loaded");
	}

	@Override
	public void noMatches() {
		player.stopTrack();
		logger.info(() -> "No matching track, probably playlist");
	}

	@Override
	public void loadFailed(FriendlyException exception) {
		player.stopTrack();
		logger.log(Level.SEVERE, "Load failed", exception);
	}

	@Override
	public void onPlayerPause(AudioPlayer player) {
		logger.info(() -> "Track paused");
	}

	@Override
	public void onPlayerResume(AudioPlayer player) {
		logger.info(() -> "Trcak resumed");
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		logger.info(() -> "Track started");
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		logger.info(() -> "Track ended: " + endReason);
		if (endReason == FINISHED || endReason == LOAD_FAILED) {
			next();
		}
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
		logger.log(Level.SEVERE, "Track exception", exception);
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		logger.info(() -> "Track is stuck");
		next();
	}

	/**
	 * forces player to play next track in queue.
	 * If repeat option toggled, puts current track in queue and plays it.
	 * If repeatq queue toggled and there is no more tracks in current queue - sets remembered queue as currend and
	 * plays from the start
	 * Stops player if there is no more tracks.
	 */
	public void next() {
		if (botGuildPlayer.getBotRepeatState() == BotRepeatState.REPEAT && lastTrack.isPresent()) {
			play(lastTrack.get().makeClone());
			return;
		}

		if (!workQueue.isEmpty()) {
			logger.info(() -> "Playing next");
			AudioTrack audioTrack = workQueue.pop();
			previousQueue.add(audioTrack.makeClone());
			play(audioTrack);
		} else {
			if (botGuildPlayer.getBotRepeatState() == BotRepeatState.REPEAT_QUEUE && !queue.isEmpty()) {
				workQueue.addAll(queue.stream().map(AudioTrack::makeClone).collect(Collectors.toList()));
				next();
			} else {
				lastTrack = Optional.empty();
				player.stopTrack();
				logger.info(() -> "Queue is empty");
			}
		}
	}

	private void play(AudioTrack track) {
		logger.info(() -> "play");
		player.setVolume((int) botGuildPlayer.getVolume());
		player.playTrack(track);
		lastTrack = Optional.ofNullable(track);
		logger.info(() -> "volume: " + player.getVolume());
	}

	/**
	 * Clears all the queues
	 */
	public void clear() {
		queue.clear();
		workQueue.clear();
		previousQueue.clear();
		player.stopTrack();
	}

	/**
	 * Forces player to play previous track, current track is added to beginning of the queue to play as next.
	 */
	public void previous() {
		if (!previousQueue.isEmpty()) {
			AudioTrack audioTrack = previousQueue.pop();
			workQueue.addFirst(player.getPlayingTrack().makeClone());
			player.playTrack(audioTrack);
		} else {
			rewind();
		}
	}

	/**
	 * Forces player to replay current track from the start.
	 */
	public void rewind() {
		player.playTrack(player.getPlayingTrack().makeClone());
	}

	/**
	 * Shuffles tracks in the current queue, if there was a track played, adds it to the queue.
	 */
	public void shuffle(AudioTrack currentTrack) {
		if (currentTrack != null) {
			workQueue.addFirst(currentTrack.makeClone());
		}
		Collections.shuffle(workQueue);
		next();
	}

	public int getQueueLen() {
		return workQueue.size();
	}

	/**
	 * workQueue	/////////
	 * queue 		/////////////
	 */
	public int getPlayedQueueLen() {
		return queue.size() - workQueue.size();
	}

	public int getWholeQueueLen() {
		return queue.size();
	}

	public String getLink() {
		AudioTrack audioTrack = player.getPlayingTrack();
		if (audioTrack != null) {
			return audioTrack.getInfo().uri;
		}
		return "";
	}

	public AudioTrack getTrack() {
		return player.getPlayingTrack();
	}
}