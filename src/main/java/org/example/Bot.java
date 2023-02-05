package org.example;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.core.spec.VoiceChannelJoinSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.voice.VoiceConnection;
import io.github.cdimascio.dotenv.Dotenv;

import java.time.Duration;
import java.util.*;

/**
 * Bot entry point, USE JAVA 11
 *
 * @author Dmitry Marchenko
 * @since 04.02.2023
 */
public class Bot {
	private final String token;
	private GatewayDiscordClient discordClient;
	private final Map<String, BotPlayer> guildIdToBotPlayers = new HashMap<>();

	private Bot(String token) {
		this.token = token;
	}

	private void run() {
		discordClient = DiscordClientBuilder.create(token).build().login().block();
		if (discordClient == null) {
			System.out.println("coudnt connect ot the discord API");
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
		registerGuildCommands(discordClient, 251697634257928193L);
		subscribeToCommands(discordClient);
		discordClient.onDisconnect().block();
	}

	public void registerGuildCommands(GatewayDiscordClient discordClient, long guildId) {
		ApplicationCommandRequest playerCommandRequest = ApplicationCommandRequest.builder()
			.name("jplayer")
			.description("call player to this channel")
			.addOption(ApplicationCommandOptionData.builder()
				.name("link")
				.description("youtube track or playlist link")
				.type(Type.STRING.getValue())
				.required(false)
				.build()
			).build();
		long applicationId = discordClient.getRestClient().getApplicationId().block();
		discordClient.getRestClient().getApplicationService()
			.createGuildApplicationCommand(applicationId, guildId, playerCommandRequest)
			.subscribe();
	}

	private void subscribeToCommands(GatewayDiscordClient discordClient) {
		discordClient.on(ChatInputInteractionEvent.class).subscribe(event -> {
			try {
				Interaction interaction = event.getInteraction();
				if ("jplayer".equals(event.getCommandName())) {
					event.deferReply();
					interaction.getGuildId().ifPresent(guildId -> {
						BotPlayer botPlayer = connect(guildId, interaction);
						event.getOption("link")
							.flatMap(ApplicationCommandInteractionOption::getValue)
							.map(ApplicationCommandInteractionOptionValue::asString)
							.ifPresent(botPlayer::addToQueue);
					});
					event.reply().withComponents(getBotControls()).subscribe();

					interaction.getGuildId().ifPresent(guildId -> {
						BotPlayer botPlayer = getBotPlayer(guildId);
						Message message = event.getReply().block();
						botPlayer.linkMessage(message);
					});
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		discordClient.on(ButtonInteractionEvent.class).subscribe(event -> {
			try {
				event.edit(InteractionApplicationCommandCallbackSpec.builder().build()).block();
				event.getInteraction().getGuildId().ifPresent(guildId -> {
					BotPlayer botPlayer = connect(guildId, event.getInteraction());
					if ("play".equals(event.getCustomId())) {
						botPlayer.play();
					} else if ("skip".equals(event.getCustomId())) {
						botPlayer.skip();
					} else if ("prev".equals(event.getCustomId())) {
						botPlayer.prev();
					} else if ("plus".equals(event.getCustomId())) {
						botPlayer.addVolume(5);
					} else if ("minus".equals(event.getCustomId())) {
						botPlayer.addVolume(-5);
					} else if ("link".equals(event.getCustomId())) {
						botPlayer.getLink();
					} else if ("shuffle".equals(event.getCustomId())) {
						botPlayer.shuffle();
					} else if (BotRepeatState.repeatQ.name().equals(event.getCustomId())) {
						botPlayer.repeatQ();
					} else if (BotRepeatState.repeat.name().equals(event.getCustomId())) {
						botPlayer.repeat();
					} else if ("unload".equals(event.getCustomId())) {
						botPlayer.unload();
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	private List<LayoutComponent> getBotControls() {
		List<LayoutComponent> layoutComponentList = new ArrayList<>();
		Button prevButton = Button.secondary("prev", ReactionEmoji.unicode("⏮"));
		Button playButton = Button.secondary("play", ReactionEmoji.unicode("⏯"));
		Button skipButton = Button.secondary("skip", ReactionEmoji.unicode("⏭"));
		Button shuffleButton = Button.secondary("shuffle", ReactionEmoji.unicode("\uD83D\uDD00"));
		Button repeatQButton = Button.secondary(BotRepeatState.repeatQ.name(), ReactionEmoji.unicode("🔁"));
		layoutComponentList.add(ActionRow.of(prevButton, playButton, skipButton, shuffleButton, repeatQButton));
		Button repeatButton = Button.secondary(BotRepeatState.repeat.name(), ReactionEmoji.unicode("🔄"));
		Button linkButton = Button.secondary("link", ReactionEmoji.unicode("↗"));
		Button unloadButton = Button.secondary("unload", ReactionEmoji.unicode("⏏"));
		Button minusButton = Button.secondary("minus", ReactionEmoji.unicode("🔉"));
		Button plusButton = Button.secondary("plus", ReactionEmoji.unicode("🔊"));
		layoutComponentList.add(ActionRow.of(repeatButton, linkButton, unloadButton, minusButton, plusButton));
		return layoutComponentList;
	}

	private BotPlayer connect(Snowflake guildId, Interaction interaction) {
		System.out.println("connect event");
		BotPlayer guildBotPlayer = getBotPlayer(guildId);
		VoiceChannelJoinSpec voiceChannelJoinSpec = VoiceChannelJoinSpec.builder()
			.provider(guildBotPlayer)
			.selfDeaf(true)
			.selfMute(false)
			.timeout(Duration.ofSeconds(2))
			.build();

		interaction.getMember()
			.map(member -> member.getVoiceState().block())
			.map(voiceState -> voiceState.getChannel().block())
			.filter(voiceChannel -> !isAlreadyConnected(voiceChannel))
			.ifPresent(voiceChannel -> {
				System.out.println("reconnecting");
				voiceChannel.join(voiceChannelJoinSpec).block(Duration.ofSeconds(3));
			});

		return guildBotPlayer;
	}

	private BotPlayer getBotPlayer(Snowflake guildId) {
		BotPlayer guildBotPlayer = guildIdToBotPlayers.computeIfAbsent(
			guildId.asString(),
			this::getNewBotPlayer
		);
		return guildBotPlayer;
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

		botVoiceConnection.ifPresent(voiceConnection -> voiceConnection.disconnect().block());
		System.out.println("disconnected from old channel");
		return false;
	}

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
		Bot bot = new Bot(dotenv.get("TOKEN"));
		bot.run();
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
	}
}