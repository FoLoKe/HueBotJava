package io.github.foloke.spring.services;

import com.google.common.collect.Lists;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.InteractionReplyEditSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.VoiceChannelJoinSpec;
import discord4j.voice.VoiceConnection;
import io.github.foloke.PlayerAccessException;
import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.github.foloke.utils.commands.BotButtonCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utils for player commands
 *
 * @author Dmitry Marchenko
 * @since 11.02.2023
 */
@Service
public final class BotPlayerService {
	private static final String CONNECT_EVENT_LOG_MESSAGE = "Connect event";
	private static final String ALREADY_CONNECTED_LOG_MESSAGE = "already connected";
	private static final String DISCONNECTED_FROM_OLD_CHANNEL_LOG_MESSAGE = "disconnected from old channel";
	private final Logger log = LoggerFactory.getLogger(BotPlayerService.class);

	private final BotLocalization playerLocalization;
	private final GatewayDiscordClient discordClient;
	private final BotPlayersHolder botPlayersHolder;
	private final List<BotButtonCommand> botButtonCommandList;

	@Autowired
	private BotPlayerService(
		BotLocalization playerLocalization,
		@Lazy GatewayDiscordClient discordClient,
		BotPlayersHolder botPlayersHolder,
		List<BotButtonCommand> botButtonCommandList
	) {
		this.playerLocalization = playerLocalization;
		this.discordClient = discordClient;
		this.botPlayersHolder = botPlayersHolder;
		this.botButtonCommandList = new ArrayList<>(botButtonCommandList);
	}

	/**
	 * Connects player to the channel of the event aouthor and adds link to thq queue, if specified
	 * @return Optional of {@link BotGuildPlayer} if bot connected successfully
	 */
	public Optional<BotGuildPlayer> connectAndAddToQueue(ChatInputInteractionEvent event, String linkParamName) {
		Interaction interaction = event.getInteraction();
		return interaction.getGuildId().map(guildId -> {
			BotGuildPlayer botGuildPlayer = connect(guildId, interaction);
			event.getOption(linkParamName)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asString)
				.ifPresent(botGuildPlayer::addToQueue);
			return botGuildPlayer;
		});
	}

	/**
	 * Makes attempt to create player message
	 * @return if message already exists does nothing and returns false
	 */
	public boolean tryCreateMessage(ChatInputInteractionEvent event) {
		return event.getInteraction()
			.getGuildId()
			.map(guildId -> {
				BotGuildPlayer botGuildPlayer = botPlayersHolder.getBotPlayer(guildId);
				if (botGuildPlayer.getMessage() == null) {
					createMessage(event);
					return true;
				}
				return false;
			}).orElse(false);
	}

	/**
	 * Creates message with player components and sends it to the channel with interaction event.
	 */
	public void createMessage(ChatInputInteractionEvent event) {
		Interaction interaction = event.getInteraction();
		interaction.getChannel()
			.blockOptional()
			.flatMap(messageChannel -> messageChannel.createMessage(
					MessageCreateSpec.create().withComponents(getButtons())
				).blockOptional()
			).ifPresent(createdMessage -> interaction.getGuildId().ifPresent(guildId -> {
					BotGuildPlayer botGuildPlayer = botPlayersHolder.getBotPlayer(guildId);
					botGuildPlayer.setMessage(createdMessage);
				})
			);

	}

	public List<LayoutComponent> getButtons() {
		List<LayoutComponent> layoutComponentList = new ArrayList<>();
		Lists.partition(botButtonCommandList.stream()
				.map(command ->
					Button.secondary(command.getCommandName(), ReactionEmoji.unicode(command.getButtonText()))
				).collect(Collectors.toList()), 5
			).stream()
			.map(ActionRow::of)
			.forEach(layoutComponentList::add);
		return layoutComponentList;
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
			log.info(ALREADY_CONNECTED_LOG_MESSAGE);
			return true;
		}

		botVoiceConnection.ifPresent(voiceConnection -> {
			voiceConnection.disconnect().block(Duration.ofSeconds(7));
			log.info(DISCONNECTED_FROM_OLD_CHANNEL_LOG_MESSAGE);
		});
		return false;
	}

	/**
	 * Tries to connect to the channel.
	 *
	 * @return BotPlayer accociated with the guild
	 * @throws PlayerAccessException if palyer was accesed outside the voiceCahnnel
	 */
	public BotGuildPlayer connect(Snowflake guildId, Interaction interaction) {
		log.info(CONNECT_EVENT_LOG_MESSAGE);
		BotGuildPlayer guildBotGuildPlayer = botPlayersHolder.getBotPlayer(guildId);
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
			throw new PlayerAccessException(playerLocalization.getMessage("join_channel_warning_message"));
		}

		return guildBotGuildPlayer;
	}

	/**
	 * Replies to message with exception text
	 */
	public void replyToAccessError(DeferrableInteractionEvent event, PlayerAccessException playerAccessException) {
		event.editReply(InteractionReplyEditSpec.builder()
			.contentOrNull(playerAccessException.getMessage())
			.build()
		).block();
	}
}
