package org.example;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.Collections;
import java.util.LinkedList;
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
	private final LinkedList<AudioTrack> queue = new LinkedList<>();
	private final LinkedList<AudioTrack> workQueue = new LinkedList<>();
	private final LinkedList<AudioTrack> previousQueue = new LinkedList<>();
	private final BotPlayer botPlayer;
	private AudioTrack lastTrack;

	/**
	 * Creates queue instance to manage player
	 */
	public BotQueue(AudioPlayer player, BotPlayer botPlayer) {
		this.player = player;
		this.botPlayer = botPlayer;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		// LavaPlayer found an audio source for us to play
		queue.add(track);
		workQueue.add(track);

		System.out.println("palying" +
			player.getPlayingTrack().getInfo().title);
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		playlist.getTracks().forEach(track -> {
			queue.add(track);
			workQueue.add(track);
		});
		System.out.println("playlist");
	}

	@Override
	public void noMatches() {
		player.stopTrack();
		System.out.println("playlist");
	}

	@Override
	public void loadFailed(FriendlyException exception) {
		player.stopTrack();
		exception.printStackTrace();
		System.out.println("load failed");
	}

	@Override
	public void onPlayerPause(AudioPlayer player) {
		System.out.println("pause");
	}

	@Override
	public void onPlayerResume(AudioPlayer player) {
		System.out.println("resume");
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		System.out.println("start");
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		System.out.println("end: " + endReason);
		if (endReason == FINISHED || endReason == LOAD_FAILED) {
			next();
		}
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
		exception.printStackTrace();
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		System.out.println("stuck");
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
		if (botPlayer.getBotRepeatState() == BotRepeatState.repeat && lastTrack != null) {
			play(lastTrack.makeClone());
			return;
		}

		if (!workQueue.isEmpty()) {
			System.out.println("next");
			AudioTrack audioTrack = workQueue.pop();
			previousQueue.add(audioTrack.makeClone());
			play(audioTrack);
		} else {
			if (botPlayer.getBotRepeatState() == BotRepeatState.repeatQ && !queue.isEmpty()) {
				workQueue.addAll(queue.stream().map(AudioTrack::makeClone).collect(Collectors.toList()));
				next();
			} else {
				lastTrack = null;
				player.stopTrack();
				System.out.println("queue is empty");
			}
		}
	}

	private void play(AudioTrack track) {
		System.out.println("play");
		player.setVolume(botPlayer.getVolume());
		player.playTrack(track);
		lastTrack = track;
		System.out.println("volume: " + player.getVolume());
	}

	/**
	 * clears all the queues
	 */
	public void clear() {
		queue.clear();
		workQueue.clear();
		previousQueue.clear();
		player.stopTrack();
	}

	/**
	 * forces player to play previous track, current track is added to beginning of the queue to play as next.
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
	 * forces player to replay current track from the start.
	 */
	public void rewind() {
		player.playTrack(player.getPlayingTrack().makeClone());
	}

	/**
	 * shuffles tracks in the current queue, if there was a track played, adds it to the queue.
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

	public int getPlayedQueueLen() {
		return previousQueue.size();
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