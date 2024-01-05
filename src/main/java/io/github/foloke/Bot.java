package io.github.foloke;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.VoiceChannelJoinSpec;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.service.ApplicationService;
import discord4j.voice.VoiceConnection;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.player.BotPlayerButtonControls;
import io.github.foloke.utils.BotLocalization;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bot entry point, USE JAVA 11
 *
 * @author Dmitry Marchenko
 * @since 04.02.2023
 */
public class Bot {
	private static final Logger logger = Logger.getLogger(Thread.currentThread().getName());
	private static final String TOKEN_KEY = "TOKEN";
	private static final String DEV_GUILD_ID_KEY = "DEV_GUILD_ID";
	private static final String MOTD_KEY = "MOTD";
	private static final String DEFAULT_TRACK_KEY = "DEFAULT_TRACK";
	private static final String CLEANING_IMAGE_KEY = "CLEANING_IMAGE";
	private static final String TRACKS_REGEX = ",";
	private static final String API_CONNECT_ERROR_MESSAGE = "Couldn't connect ot the discord API";
	private static final String BOT_IS_ACTIVE_FORMAT = "Bot is active %s";
	private static final String DEBUG_COMMAND = "!ping";
	private static final String CHAT_COMMANDS_SUBSCRIPTION_ERROR_MESSAGE = "Chat command subscription";
	private static final String BUTTON_COMMAND_SUBSCRIPTION_ERROR_MESSAGE = "Button command subscription";
	private static final String CONNECT_EVENT_LOG_MESSAGE = "Connect event";
	private static final String NEW_PLAYER_CREATED_LOG_MESSAGE = "new player created";
	private static final String ALREADY_CONNECTED_LOG_MESSAGE = "already connected";
	private static final String DISCONNECTED_FROM_OLD_CHANNEL_LOG_MESSAGE = "disconnected from old channel";
	private static final String ENV_FILE_ERROR_MESSAGE = "please specify valid .env file with discord token and guild id";
	private final String token;
	private final String cleaningImageUrl;
	/**
	 * Default track for new queue (for debugging)
	 */
	private final List<String> defaultTrackList;
	/**
	 * Guild id for player (developed for single guild only (for now))
	 */
	private final long devGuildId;
	private final Map<String, BotGuildPlayer> guildIdToBotPlayers = new HashMap<>();

	private GatewayDiscordClient discordClient;

	private final String motd;

	/**
	 * Entry point. Creates Bot instance and runs it. No args, use .env file.
	 */
	public static void main(String[] args) {
		Logger.getGlobal().setLevel(Level.ALL);
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		try {
			new Bot().run();
		} catch (DotenvException dotenvException) {
			logger.log(Level.SEVERE, ENV_FILE_ERROR_MESSAGE, dotenvException);
		}
	}

	private Bot() {
		Dotenv dotenv = Dotenv.load();
		cleaningImageUrl = dotenv.get(CLEANING_IMAGE_KEY, "");
		token = dotenv.get(TOKEN_KEY);
		devGuildId = Long.parseLong(dotenv.get(DEV_GUILD_ID_KEY));
		motd = dotenv.get(MOTD_KEY);
		defaultTrackList = Arrays.asList(dotenv.get(DEFAULT_TRACK_KEY, "").split(TRACKS_REGEX));
	}

	private void run() {
		discordClient = DiscordClientBuilder.create(token).build().gateway()
			.setEnabledIntents(IntentSet.all())
			.login().block();
		if (discordClient == null) {
			logger.severe(API_CONNECT_ERROR_MESSAGE);
			return;
		}
		subscribeToDebugPingMessage();
		registerGuildCommands(discordClient, devGuildId);
		deleteGlobalCommands(discordClient);
		subscribeToCommands(discordClient);
		logger.log(Level.INFO, () -> String.format(BOT_IS_ACTIVE_FORMAT, Thread.currentThread()));
		discordClient.onDisconnect().block();
	}

	private void subscribeToDebugPingMessage() {
		discordClient.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
			Message message = event.getMessage();
			if (DEBUG_COMMAND.equalsIgnoreCase(message.getContent())) {
				message.getChannel().flatMap(channel ->
					channel.createMessage(BotLocalization.getPlayerMessage(
						"debug_message",
						message.getGuildId()
					))
				).block();
			}
		});
	}

	/**
	 * Registers Commands for spectifed guild
	 */
	private void registerGuildCommands(GatewayDiscordClient discordClient, long guildId) {
		Long applicationId = discordClient.getRestClient().getApplicationId().block();
		ApplicationService applicationService = discordClient.getRestClient().getApplicationService();
		if (applicationId != null) {
			BotGuildCommands.getApplicationCommandRequestList().forEach(applicationCommandRequest ->
				applicationService.createGuildApplicationCommand(
					applicationId,
					guildId,
					applicationCommandRequest
				).block()
			);
		}
	}

	private void deleteGlobalCommands(GatewayDiscordClient discordClient) {
		// DELETE OLD COMMANDS
//		long applicationId = discordClient.getRestClient().getApplicationId().block();
//		ApplicationService applicationService = discordClient.getRestClient().getApplicationService();
//		List<ApplicationCommandData> applicationCommandDataList =
//			applicationService.getGlobalApplicationCommands(applicationId).collectList().block();
//		applicationCommandDataList.forEach(applicationCommandData ->
//			applicationService.deleteGlobalApplicationCommand(applicationId, applicationCommandData.id().asLong())
//				.block()
//		);
	}

	private void subscribeToCommands(GatewayDiscordClient discordClient) {
		discordClient.on(ChatInputInteractionEvent.class).subscribe(event -> {
			try {
				BotGuildCommands.runCommand(event, this);
			} catch (PlayerAccessException e) {
				e.replyToEvent(event);
			} catch (Exception e) {
				logger.log(Level.SEVERE, CHAT_COMMANDS_SUBSCRIPTION_ERROR_MESSAGE, e);
			}
		});

		discordClient.on(ButtonInteractionEvent.class).subscribe(event -> {
			try {
				event.getInteraction().getGuildId().ifPresent(guildId -> {
					BotGuildPlayer botGuildPlayer = connect(guildId, event.getInteraction());
					if(event.getMessage().isPresent()) {
						botGuildPlayer.setMessage(event.getMessage().get());
					}
					BotPlayerButtonControls.runCommand(event, botGuildPlayer);
				});
			} catch (PlayerAccessException e) {
				e.replyToEvent(event);
			} catch (Exception e) {
				logger.log(Level.SEVERE, BUTTON_COMMAND_SUBSCRIPTION_ERROR_MESSAGE, e);
			}
		});
	}

	/**
	 * Tries to connect to the channel.
	 *
	 * @return BotPlayer accociated with the guild
	 * @throws PlayerAccessException if palyer was accesed outside the voiceCahnnel
	 */
	public BotGuildPlayer connect(Snowflake guildId, Interaction interaction) {
		logger.info(CONNECT_EVENT_LOG_MESSAGE);
		BotGuildPlayer guildBotGuildPlayer = getBotPlayer(guildId);
		VoiceChannelJoinSpec voiceChannelJoinSpec = VoiceChannelJoinSpec.builder()
			.provider(guildBotGuildPlayer)
			.selfDeaf(true)
			.selfMute(false)
			.ipDiscoveryRetrySpec(Retry.max(10))
			.build();

		Optional<VoiceChannel> voiceChannelOptional = interaction.getMember()
			.map(member -> member.getVoiceState().block())
			.map(voiceState -> voiceState.getChannel().block());

		if (voiceChannelOptional.isPresent()) {
			voiceChannelOptional.filter(voiceChannel -> !isAlreadyConnected(voiceChannel))
				.ifPresent(voiceChannel -> voiceChannel.join(voiceChannelJoinSpec)
					.block());
		} else {
			throw new PlayerAccessException(BotLocalization.getPlayerMessage("join_channel_warning_message"));
		}

		return guildBotGuildPlayer;
	}

	/**
	 * Get player by Guild id
	 */
	public BotGuildPlayer getBotPlayer(Snowflake guildId) {
		return guildIdToBotPlayers.computeIfAbsent(
			guildId.asString(),
			this::getNewBotPlayer
		);
	}

	private BotGuildPlayer getNewBotPlayer(String guildId) {
		BotGuildPlayer botGuildPlayer = new BotGuildPlayer(guildId, motd);
		defaultTrackList.forEach(botGuildPlayer::addToQueue);
		logger.info(NEW_PLAYER_CREATED_LOG_MESSAGE);
		return botGuildPlayer;
	}

	private boolean isAlreadyConnected(VoiceChannel userVoiceChannel) {
		Snowflake guildId = userVoiceChannel.getGuildId();
		Optional<VoiceConnection> botVoiceConnection = discordClient.getVoiceConnectionRegistry()
			.getVoiceConnection(guildId)
			.blockOptional()
			.filter(voiceConnection -> Boolean.TRUE.equals(voiceConnection.isConnected().block()));
		Optional<Snowflake> botVoiceChannelId =
			botVoiceConnection.map(voiceConnection -> voiceConnection.getChannelId().block());

		if (botVoiceChannelId.map(id -> id.equals(userVoiceChannel.getId())).orElse(false)) {
			logger.info(ALREADY_CONNECTED_LOG_MESSAGE);
			return true;
		}

		botVoiceConnection.ifPresent(voiceConnection -> {
			voiceConnection.disconnect().block(Duration.ofSeconds(7));
			logger.info(DISCONNECTED_FROM_OLD_CHANNEL_LOG_MESSAGE);
		});
		return false;
	}

	public String getCleaningImageUrl() {
		return cleaningImageUrl;
	}

	/**
	 * Exception with interaction reply
	 */
	public static class PlayerAccessException extends RuntimeException {

		private static final String ACCESS_FAIL_FORMAT = "Access fail: %s";

		/**
		 * Create exception with message text
		 */
		public PlayerAccessException(String message) {
			super(message);
		}

		/**
		 * Replies to provided DeferrableInteractionEvent with error message
		 */
		public void replyToEvent(DeferrableInteractionEvent event) {
			logger.info(() -> String.format(ACCESS_FAIL_FORMAT, getMessage()));
			event.reply(InteractionApplicationCommandCallbackSpec.builder()
				.ephemeral(true)
				.content(getMessage())
				.build()
			).block();
		}
	}
}