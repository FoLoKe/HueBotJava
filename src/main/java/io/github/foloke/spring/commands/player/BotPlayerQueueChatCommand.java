package io.github.foloke.spring.commands.player;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.player.BotRepeatState;
import io.github.foloke.spring.services.BotPlayerService;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.github.foloke.utils.commands.BotChatCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Adds track to queue
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
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
		Optional<BotGuildPlayer> botGuildPlayerOptional = service.connectAndAddToQueue(event, LINK_OPTION_NAME);
		if (botGuildPlayerOptional.isPresent() && !service.tryCreateMessage(event)) {
			BotGuildPlayer botGuildPlayer = botGuildPlayerOptional.get();
			String responseMessage = localization.getMessage("track_added_message");
			if (botGuildPlayer.getBotRepeatState() == BotRepeatState.REPEAT) {
				responseMessage = localization.getMessage("repeat_enabled_warning");
			}
			event.editReply(responseMessage).block();
		} else {
			event.editReply(localization.getMessage("player_created_message"));
		}
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
