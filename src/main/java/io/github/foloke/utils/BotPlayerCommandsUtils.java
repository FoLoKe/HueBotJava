package io.github.foloke.utils;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.spec.MessageCreateSpec;
import io.github.foloke.Bot;
import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.player.BotPlayerButtonControls;

import java.util.Optional;

/**
 * Utils for player commands
 *
 * @author Dmitry Marchenko
 * @since 11.02.2023
 */
public final class BotPlayerCommandsUtils {
	private BotPlayerCommandsUtils() {
	}

	/**
	 * Connects player to the channel of the event aouthor and adds link to thq queue, if specified
	 * @return Optional of {@link BotGuildPlayer} if bot connected successfully
	 */
	public static Optional<BotGuildPlayer> connectAndAddToQueue(
		ChatInputInteractionEvent event,
		Bot bot,
		String linkParamName
	) {
		Interaction interaction = event.getInteraction();
		return interaction.getGuildId().map(guildId -> {
			BotGuildPlayer botGuildPlayer = bot.connect(guildId, interaction);
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
	public static boolean tryCreateMessage(ChatInputInteractionEvent event, Bot bot) {
		return event.getInteraction()
			.getGuildId()
			.map(guildId -> {
				BotGuildPlayer botGuildPlayer = bot.getBotPlayer(guildId);
				if (botGuildPlayer.getMessage() == null) {
					createMessage(event, bot);
					return true;
				}
				return false;
			}).orElse(false);
	}

	/**
	 * Creates message with player components and sends it to the channel with interaction event.
	 */
	public static void createMessage(ChatInputInteractionEvent event, Bot bot) {
		Interaction interaction = event.getInteraction();
		interaction.getChannel()
			.blockOptional()
			.flatMap(messageChannel -> messageChannel.createMessage(
					MessageCreateSpec.create().withComponents(BotPlayerButtonControls.getButtons())
				).blockOptional()
			).ifPresent(createdMessage -> interaction.getGuildId().ifPresent(guildId -> {
					BotGuildPlayer botGuildPlayer = bot.getBotPlayer(guildId);
					botGuildPlayer.setMessage(createdMessage);
				})
			);

	}
}
