package io.github.foloke.spring.listeners;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import io.github.foloke.PlayerAccessException;
import io.github.foloke.spring.services.BotPlayerService;
import io.github.foloke.utils.commands.BotChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles chat commands ("/") interactions
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Service
public class ChatCommandsListener implements EventListener<ChatInputInteractionEvent> {

	private static final String CHAT_COMMANDS_SUBSCRIPTION_ERROR_MESSAGE = "Chat command subscription";
	private final Logger log = LoggerFactory.getLogger(ChatCommandsListener.class);
	private final BotPlayerService botPlayerService;
	private final List<BotChatCommand> commandList;

	@Autowired
	public ChatCommandsListener(BotPlayerService botPlayerService, List<BotChatCommand> commandList) {
		this.botPlayerService = botPlayerService;
		this.commandList = new ArrayList<>(commandList);
	}

	@Override
	public void executeCommand(ChatInputInteractionEvent event) {
		try {
			commandList.stream()
				.filter(command -> command.getCommandName().equals(event.getCommandName()))
				.findFirst()
				.ifPresent(command -> {
					event.deferReply().withEphemeral(command.isEphemeral()).block();
					command.execute(event);
				});
		} catch (PlayerAccessException e) {
			botPlayerService.editReplyToAccessError(event, e);
		} catch (Exception e) {
			log.error(CHAT_COMMANDS_SUBSCRIPTION_ERROR_MESSAGE, e);
		}
	}

	@Override
	public Class<ChatInputInteractionEvent> getTypeClass() {
		return ChatInputInteractionEvent.class;
	}
}
