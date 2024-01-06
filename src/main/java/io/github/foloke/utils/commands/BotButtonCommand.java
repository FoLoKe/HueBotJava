package io.github.foloke.utils.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
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
	void execute(BotGuildPlayer player);
	String getButtonText();
}
