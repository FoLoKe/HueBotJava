package io.github.foloke.spring.commands.common;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.service.ApplicationService;
import io.github.foloke.spring.commands.BotChatCommandsService;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.github.foloke.utils.commands.BotChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Global command to re-init guild commnds
 *
 * @author Марченко Дмитрий
 * @since 09.03.2024
 */
@Component
@Qualifier("global")
public class BotInitGuildCommnd implements BotChatCommand {
	private static final String COMMAND_REGISTER_LOG_FORMAT = "Started to register command: %s, for guild: %d";
	private static final String COMMANDS_REGISTER_LOG_FORMAT = "Started to register commands for guild: %d";
	private final Logger log = LoggerFactory.getLogger(getClass().getName());

	private final BotLocalization playerLocalization;
	private final BotChatCommandsService botChatCommandsService;

	public BotInitGuildCommnd(
		BotLocalization playerLocalization,
		@Lazy BotChatCommandsService botChatCommandsService
	) {
		this.playerLocalization = playerLocalization;
		this.botChatCommandsService = botChatCommandsService;
	}

	@Override
	public String getDescription() {
		return playerLocalization.getMessage("register_guild_commands");
	}

	@Override
	public List<ApplicationCommandOptionData> getOptions() {
		return Collections.emptyList();
	}

	@Override
	public void execute(ChatInputInteractionEvent event) {
		try {
			event.getInteraction()
				.getGuildId()
				.ifPresent(guildId -> registerGuildCommands(event.getClient(), guildId.asLong()));
		} catch (Exception e) {
			log.error("Error while registering guild commands", e);
			event.editReply(playerLocalization.getMessage("guild_commands_register_error", e.getMessage())).block();
		}
		log.info("commands registered");
		event.editReply(playerLocalization.getMessage("guild_commands_registered")).block();
	}

	@Override
	public String getCommandName() {
		return "init";
	}

	/**
	 * Registers Commands for spectifed guild
	 */
	public void registerGuildCommands(GatewayDiscordClient discordClient, long guildId) {
		log.info(String.format(COMMANDS_REGISTER_LOG_FORMAT, guildId));
		Long applicationId = discordClient.getRestClient().getApplicationId().block();
		ApplicationService applicationService = discordClient.getRestClient().getApplicationService();
		if (applicationId != null) {
			botChatCommandsService.getLocalApplicationCommandRequestList().forEach(commandRequest -> {
				log.info(String.format(COMMAND_REGISTER_LOG_FORMAT, commandRequest.name(), guildId));
				applicationService.createGuildApplicationCommand(applicationId, guildId, commandRequest).block();
			});
		}
	}

	@Override
	public boolean isEphemeral() {
		return true;
	}
}
