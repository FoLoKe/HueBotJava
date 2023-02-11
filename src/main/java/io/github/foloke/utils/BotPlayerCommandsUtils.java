package io.github.foloke.utils;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Message;
import io.github.foloke.Bot;
import io.github.foloke.player.BotPlayer;
import io.github.foloke.player.BotPlayerButtonControls;

/**
 * Service for player commands
 *
 * @author Dmitry Marchenko
 * @since 11.02.2023
 */
public class BotPlayerCommandsUtils {
	public static void connectAndAddToQueue(ChatInputInteractionEvent event, Bot bot, String paramName) {
		Interaction interaction = event.getInteraction();
		interaction.getGuildId().ifPresent(guildId -> {
			BotPlayer botPlayer = bot.connect(guildId, interaction);
			event.getOption(paramName)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asString)
				.ifPresent(botPlayer::addToQueue);
		});
	}

	public static boolean tryCreateMessage(ChatInputInteractionEvent event, Bot bot) {
		return event.getInteraction()
			.getGuildId()
			.map(guildId -> {
				BotPlayer botPlayer = bot.getBotPlayer(guildId);
				if (botPlayer.getMessage() == null) {
					createMessage(event, bot);
					return true;
				}
				return false;
			}).orElse(false);
	}

	public static void createMessage(ChatInputInteractionEvent event, Bot bot) {
		event.reply().withComponents(BotPlayerButtonControls.getButtons()).subscribe();
		event.getInteraction()
			.getGuildId()
			.ifPresent(guildId -> {
				BotPlayer botPlayer = bot.getBotPlayer(guildId);
				Message message = event.getReply().block();
				botPlayer.setMessage(message);
			});
	}
}
