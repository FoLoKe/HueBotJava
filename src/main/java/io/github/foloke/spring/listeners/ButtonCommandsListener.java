package io.github.foloke.spring.listeners;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import io.github.foloke.PlayerAccessException;
import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.spring.services.BotPlayerService;
import io.github.foloke.utils.commands.BotButtonCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles buttons interactions
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Service
public class ButtonCommandsListener implements EventListener<ButtonInteractionEvent> {
	private static final String BUTTON_COMMAND_SUBSCRIPTION_ERROR_MESSAGE = "Button command subscription";
	private final Logger log = LoggerFactory.getLogger(ButtonCommandsListener.class);
	private final BotPlayerService botPlayerService;
	private final List<BotButtonCommand> buttonCommandList;
	@Autowired
	public ButtonCommandsListener(BotPlayerService botPlayerService, List<BotButtonCommand> buttonCommandList) {
		this.botPlayerService = botPlayerService;
		this.buttonCommandList = new ArrayList<>(buttonCommandList);
	}
	@Override
	public void executeCommand(ButtonInteractionEvent event) {
		try {
			event.getInteraction().getGuildId().ifPresent(guildId -> {
				BotGuildPlayer botGuildPlayer = botPlayerService.connect(guildId, event.getInteraction());
				if (event.getMessage().isPresent()) {
					botGuildPlayer.setMessage(event.getMessage().get());
				}

				buttonCommandList.stream()
					.filter(command -> event.getCustomId().equals(command.getCommandName()))
					.findFirst()
					.ifPresent(command -> {
						command.execute(botGuildPlayer);
						event.edit(InteractionApplicationCommandCallbackSpec.builder().build()).block();
					});

			});
		} catch (PlayerAccessException e) {
			botPlayerService.replyToAccessError(event, e);
		} catch (Exception e) {
			log.error(BUTTON_COMMAND_SUBSCRIPTION_ERROR_MESSAGE, e);
		}
	}

	@Override
	public Class<ButtonInteractionEvent> getTypeClass() {
		return ButtonInteractionEvent.class;
	}
}
