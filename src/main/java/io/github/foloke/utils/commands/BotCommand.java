package io.github.foloke.utils.commands;

import discord4j.core.event.domain.Event;
import org.springframework.stereotype.Component;

/**
 * Command interface for enums commands
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
public interface BotCommand<T extends Event>  {
	String getCommandName();
}
