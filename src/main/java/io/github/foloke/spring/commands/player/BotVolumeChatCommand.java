package io.github.foloke.spring.commands.player;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.command.Interaction;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.spring.services.BotPlayerService;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.github.foloke.utils.commands.BotChatCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Sets or shows volume
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
public class BotVolumeChatCommand implements BotChatCommand {

	public static final String AMOUNT_OPTION_NAME = "amount";
	private final BotPlayerService service;
	private final BotLocalization localization;

	@Autowired
	public BotVolumeChatCommand(BotPlayerService botPlayerService, BotLocalization playerLocalization) {
		service = botPlayerService;
		localization = playerLocalization;
	}

	@Override
	public void execute(ChatInputInteractionEvent event) {
		Interaction interaction = event.getInteraction();
		interaction.getGuildId().ifPresent(guildId -> {
			BotGuildPlayer botGuildPlayer = service.connect(guildId, interaction);
			service.tryCreateMessage(event);
			Optional<Long> volumeValue = event.getOption(AMOUNT_OPTION_NAME)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asLong);
			if (volumeValue.isPresent()) {
				botGuildPlayer.setVolumePercent(volumeValue.get().floatValue());
				event.editReply(
					localization.getMessage("volume_set_message", botGuildPlayer.getVolumePercent())
				).block();
			} else {
				event.editReply(
					localization.getMessage("volume_is_message", botGuildPlayer.getVolumePercent())
				).block();
			}
		});
	}

	@Override
	public List<ApplicationCommandOptionData> getOptions() {
		return Collections.singletonList(ApplicationCommandOptionData.builder()
			.name(AMOUNT_OPTION_NAME)
			.description(localization.getMessage("volume_amount_description"))
			.type(Type.INTEGER.getValue())
			.required(false)
			.build()
		);
	}

	@Override
	public String getDescription() {
		return localization.getMessage("volume_description");
	}

	@Override
	public String getCommandName() {
		return "volume";
	}

	@Override
	public boolean isEphemeral() {
		return true;
	}
}
