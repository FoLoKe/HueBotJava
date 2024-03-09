package io.github.foloke.spring.commands.player;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import io.github.foloke.player.AddToQueueException;
import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.spring.services.BotPlayerService;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.github.foloke.utils.commands.BotButtonCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Button for
 *
 * @author Марченко Дмитрий
 * @since 08.03.2024
 */
@Component
@Qualifier("local")
public class BotChooseTrackSearchResultButtonCommand implements BotButtonCommand {
	public static final String COMMNAD_NAME = "choose_track";
	private final Logger log = LoggerFactory.getLogger(getClass().getName());
	private final BotPlayerService botPlayerService;
	private final BotLocalization localization;

	@Autowired
	public BotChooseTrackSearchResultButtonCommand(BotPlayerService botPlayerService,
												   BotLocalization playerLocalization) {
		this.botPlayerService = botPlayerService;
		localization = playerLocalization;
	}

	@Override
	public void execute(ButtonInteractionEvent event, BotGuildPlayer player) {
		Arrays.stream(event.getCustomId().split(ID_PARAMS_SEPARATOR)).skip(1)
			.findFirst()
			.ifPresent(tarckId -> {
				botPlayerService.tryCreateMessage(event);
				try {
					event.reply(botPlayerService.connectAndAddToQueue(event, tarckId)).withEphemeral(true).block();
				} catch (AddToQueueException e) {
					String errorMessage = localization.getMessage("track_add_error_message");
					event.reply(errorMessage).withEphemeral(true).block();
					log.error(errorMessage, e);
				}
			});
	}

	@Override
	public String getButtonText() {
		return localization.getMessage("choose_found_track");
	}

	@Override
	public String getCommandName() {
		return COMMNAD_NAME;
	}
}
