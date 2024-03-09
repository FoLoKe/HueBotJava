package io.github.foloke.spring.listeners;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import io.github.foloke.PlayerAccessException;
import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.spring.services.BotPlayerService;
import io.github.foloke.utils.commands.BotButtonCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.foloke.utils.commands.BotButtonCommand.ID_PARAMS_SEPARATOR;

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
			String commandName = Arrays.stream(event.getCustomId().split(ID_PARAMS_SEPARATOR)).findFirst().orElse("");
			event.getInteraction().getGuildId().ifPresent(guildId -> {
				BotGuildPlayer botGuildPlayer = botPlayerService.connect(guildId, event.getInteraction());
				if (botPlayerService.isPlayerButtonCommand(commandName) && event.getMessage().isPresent()) {
					botGuildPlayer.setMessage(event.getMessage().get());
				}

				buttonCommandList.stream()
					.filter(command -> commandName.equals(command.getCommandName()))
					.findFirst()
					.ifPresent(command -> command.execute(event, botGuildPlayer));
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
