package io.github.foloke.spring.commands;

import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest.Builder;
import io.github.foloke.utils.commands.BotChatCommand;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for chat commands
 *
 * @author Марченко Дмитрий
 * @since 09.03.2024
 */
@Service
public class BotChatCommandsService {
	private final List<BotChatCommand> localChatCommandsList;
	private final List<BotChatCommand> globalChatCommandsList;
	public BotChatCommandsService(
		@Qualifier("local") List<BotChatCommand> localChatCommandsList,
		@Qualifier("global") List<BotChatCommand> globalChatCommandsList
	) {
		this.localChatCommandsList = new ArrayList<>(localChatCommandsList);
		this.globalChatCommandsList = new ArrayList<>(globalChatCommandsList);
	}

	public List<ApplicationCommandRequest> getLocalApplicationCommandRequestList() {
		return getApplicationCommandRequestList(localChatCommandsList);
	}

	public List<ApplicationCommandRequest> getGlobalApplicationCommandRequestList() {
		return getApplicationCommandRequestList(globalChatCommandsList);
	}

	private List<ApplicationCommandRequest> getApplicationCommandRequestList(List<BotChatCommand> chatCommandsList) {
		return chatCommandsList.stream().map(botCommand -> {
			Builder playerCommandRequestBuilder = ApplicationCommandRequest.builder()
				.name(botCommand.getCommandName())
				.description(botCommand.getDescription());
			List<ApplicationCommandOptionData> optionDataList = botCommand.getOptions();
			if (!optionDataList.isEmpty()) {
				playerCommandRequestBuilder.addAllOptions(optionDataList).build();
			}
			return playerCommandRequestBuilder.build();
		}).collect(Collectors.toList());
	}
}
