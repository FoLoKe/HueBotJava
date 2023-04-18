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
import io.github.foloke.player.BotPlayer;
import io.github.foloke.player.BotPlayerButtonControls;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bot entry point, USE JAVA 11
 *
 * @author Dmitry Marchenko
 * @since 04.02.2023
 */
public class Bot {
	private final String token;
	private final long devGuildId;
	private GatewayDiscordClient discordClient;
	private final Map<String, BotPlayer> guildIdToBotPlayers = new HashMap<>();

	private Bot(String token, long devGuildId) {
		this.token = token;
		this.devGuildId = devGuildId;
	}

	private void run() {
		discordClient = DiscordClientBuilder.create(token).build().gateway()
			.setEnabledIntents(IntentSet.all())
			.login().block();
		if (discordClient == null) {
			System.out.println("Couldn't connect ot the discord API");
			return;
		}
		discordClient.getEventDispatcher().on(MessageCreateEvent.class)
			.subscribe(event -> {
				Message message = event.getMessage();
				if ("!ping".equalsIgnoreCase(message.getContent())) {
					message.getChannel()
						.flatMap(channel -> channel.createMessage("guildId: " + message.getGuildId())).block();
				}
			});
		registerGuildCommands(discordClient, devGuildId);
		registerGlobalCommands(discordClient);
		subscribeToCommands(discordClient);
		discordClient.onDisconnect().block();
	}

	/**
	 * Registers Commands for spectifed guild
	 */
	private void registerGuildCommands(GatewayDiscordClient discordClient, long guildId) {
		long applicationId = discordClient.getRestClient().getApplicationId().block();
		ApplicationService applicationService = discordClient.getRestClient().getApplicationService();
		BotGuildCommands.getApplicationCommandRequestList().forEach(applicationCommandRequest ->
			applicationService.createGuildApplicationCommand(
				applicationId,
				guildId,
				applicationCommandRequest
			).block()
		);
	}

	private void registerGlobalCommands(GatewayDiscordClient discordClient) {
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
				e.printStackTrace();
			}
		});

		discordClient.on(ButtonInteractionEvent.class).subscribe(event -> {
			try {
				event.deferReply();
				event.getInteraction().getGuildId().ifPresent(guildId -> {
					BotPlayer botPlayer = connect(guildId, event.getInteraction());
					BotPlayerButtonControls.runCommand(event, botPlayer);
				});
			} catch (PlayerAccessException e) {
				e.replyToEvent(event);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Tries to connect to the channel.
	 *
	 * @return BotPlayer accociated with the guild
	 * @throws PlayerAccessException if palyer was accesed outside of the voiceCahnnel
	 */
	public BotPlayer connect(Snowflake guildId, Interaction interaction) {
		System.out.println("connect event");
		BotPlayer guildBotPlayer = getBotPlayer(guildId);
		VoiceChannelJoinSpec voiceChannelJoinSpec = VoiceChannelJoinSpec.builder()
			.provider(guildBotPlayer)
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
			throw new PlayerAccessException("Join the channel first!");
		}

		return guildBotPlayer;
	}

	public BotPlayer getBotPlayer(Snowflake guildId) {
		return guildIdToBotPlayers.computeIfAbsent(
			guildId.asString(),
			this::getNewBotPlayer
		);
	}

	private BotPlayer getNewBotPlayer(String guildId) {
		BotPlayer botPlayer = new BotPlayer(guildId);
		System.out.println("new player created");
		return botPlayer;
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
			System.out.println("already connected");
			return true;
		}

		botVoiceConnection.ifPresent(voiceConnection -> {
			voiceConnection.disconnect().block(Duration.ofSeconds(7));
			System.out.println("disconnected from old channel");
		});
		return false;
	}

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
		Bot bot = new Bot(dotenv.get("TOKEN"), Long.parseLong(dotenv.get("DEV_GUILD_ID")));
		bot.run();
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
	}

	public static class PlayerAccessException extends RuntimeException {
		public PlayerAccessException(String message) {
			super(message);
		}

		/**
		 * Replies to provided DeferrableInteractionEvent with error message
		 */
		public void replyToEvent(DeferrableInteractionEvent event) {
			System.out.println("Access fail: " + getMessage());
			event.reply(InteractionApplicationCommandCallbackSpec.builder()
				.ephemeral(true)
				.content(getMessage())
				.build()
			).block();
		}
	}
}