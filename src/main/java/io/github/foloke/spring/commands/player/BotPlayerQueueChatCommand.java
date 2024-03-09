package io.github.foloke.spring.commands.player;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import io.github.foloke.spring.services.BotPlayerService;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.github.foloke.utils.commands.BotChatCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Adds track to queue
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
@Qualifier("local")
public class BotPlayerQueueChatCommand implements BotChatCommand {
	public static final String LINK_OPTION_NAME = "link";
	private final BotPlayerService service;
	private final BotLocalization localization;

	@Autowired
	public BotPlayerQueueChatCommand(BotPlayerService botPlayerService, BotLocalization playerLocalization) {
		service = botPlayerService;
		localization = playerLocalization;
	}

	@Override
	public void execute(ChatInputInteractionEvent event) {
		event.editReply(service.connectAndAddToQueue(event, service.getParamValue(event, LINK_OPTION_NAME))).block();
	}

	@Override
	public List<ApplicationCommandOptionData> getOptions() {
		return Collections.singletonList(ApplicationCommandOptionData.builder()
			.name(LINK_OPTION_NAME)
			.description(localization.getMessage("link_description"))
			.type(Type.STRING.getValue())
			.required(true)
			.build()
		);
	}

	@Override
	public String getDescription() {
		return localization.getMessage("add_link_description");
	}

	@Override
	public boolean isEphemeral() {
		return true;
	}

	@Override
	public String getCommandName() {
		return "q";
	}
}
