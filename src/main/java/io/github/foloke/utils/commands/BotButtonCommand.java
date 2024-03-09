package io.github.foloke.utils.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import io.github.foloke.player.BotGuildPlayer;
import org.springframework.stereotype.Component;

/**
 * Interaction with button
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
public interface BotButtonCommand extends BotCommand<ButtonInteractionEvent> {
	String ID_PARAMS_SEPARATOR = "%%";

	/**
	 * Execute with automatic empty reply
	 */
	default void execute(BotGuildPlayer player) {

	}

	/**
	 * Execute with reply
	 */
	default void execute(ButtonInteractionEvent event, BotGuildPlayer player) {
		execute(player);
		event.edit(InteractionApplicationCommandCallbackSpec.builder().build()).block();
	}

	String getButtonText();
}
