package io.github.foloke.spring.services;

import discord4j.common.util.Snowflake;
import io.github.foloke.player.AddToQueueException;
import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.spring.services.localization.BotLocalization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guild's players holder (basicly factory)
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
public class BotPlayersHolder {
	private static final String TRACKS_REGEX = ",";
	private static final String NEW_PLAYER_CREATED_LOG_MESSAGE = "new player created";
	private final Logger log = LoggerFactory.getLogger(BotPlayersHolder.class);
	private final Map<String, BotGuildPlayer> guildIdToBotPlayers = new ConcurrentHashMap<>();
	private final BotLocalization playerLocalization;
	/**
	 * Default track for new queue (for debugging)
	 */
	@Value("${defaultTrackList}")
	private String defaultTrackList;
	@Value("${motd}")
	private String motd;

	public BotPlayersHolder(BotLocalization playerLocalization) {
		this.playerLocalization = playerLocalization;
	}

	/**
	 * Get player by Guild id
	 */
	public synchronized BotGuildPlayer getBotPlayer(Snowflake guildId) {
		return guildIdToBotPlayers.computeIfAbsent(
			guildId.asString(),
			this::getNewBotPlayer
		);
	}

	private BotGuildPlayer getNewBotPlayer(String guildId) {
		BotGuildPlayer botGuildPlayer = new BotGuildPlayer(guildId, motd, playerLocalization);
		Arrays.stream(defaultTrackList.split(TRACKS_REGEX)).forEach(trackLink -> {
			try {
				botGuildPlayer.addToQueue(trackLink);
			} catch (AddToQueueException e) {
				log.error("Initial track add error");
			}
		});
		log.info(NEW_PLAYER_CREATED_LOG_MESSAGE);
		return botGuildPlayer;
	}
}
