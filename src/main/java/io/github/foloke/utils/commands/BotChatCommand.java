package io.github.foloke.utils.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Chat command interaction
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
public interface BotChatCommand extends BotCommand<ChatInputInteractionEvent> {
	boolean isEphemeral();
	String getDescription();
	List<ApplicationCommandOptionData> getOptions();
	void execute(ChatInputInteractionEvent event);
}
