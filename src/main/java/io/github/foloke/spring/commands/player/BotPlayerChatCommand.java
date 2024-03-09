package io.github.foloke.spring.commands.player;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import io.github.foloke.spring.services.BotPlayerService;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.github.foloke.utils.commands.BotChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Calls player to tche voice channel
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
@Qualifier("local")
public class BotPlayerChatCommand implements BotChatCommand {
	public static final String LINK_OPTION_NAME = "link";
	private final Logger log = LoggerFactory.getLogger(getClass().getSimpleName());
	private final BotPlayerService service;
	private final BotLocalization localization;

	@Autowired
	public BotPlayerChatCommand(BotPlayerService botPlayerService, BotLocalization playerLocalization) {
		service = botPlayerService;
		localization = playerLocalization;
	}

	@Override
	public void execute(ChatInputInteractionEvent event) {
		String link = service.getParamValue(event, LINK_OPTION_NAME);
		if (link.isEmpty()) {
			service.createMessage(event);
			service.connect(event);
			event.editReply(localization.getMessage("player_created_message")).block();
		} else {
			event.editReply(service.connectAndAddToQueue(event, link)).block();
		}
	}

	@Override
	public List<ApplicationCommandOptionData> getOptions() {
		return Collections.singletonList(ApplicationCommandOptionData.builder()
			.name(LINK_OPTION_NAME)
			.description(localization.getMessage("link_description"))
			.type(Type.STRING.getValue())
			.required(false)
			.build()
		);
	}

	@Override
	public String getDescription() {
		return localization.getMessage("call_player_description");
	}

	@Override
	public String getCommandName() {
		return "player";
	}

	@Override
	public boolean isEphemeral() {
		return true;
	}
}
