package io.github.foloke.spring.listeners;

import discord4j.core.event.domain.Event;
import org.springframework.stereotype.Service;

/**
 * Base interface for interactions
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Service
public interface EventListener<T extends Event> {
	void executeCommand(T event);

	Class<T> getTypeClass();
}
