package io.github.foloke.spring.commands.common;

import com.google.common.collect.Lists;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.github.foloke.utils.commands.BotChatCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Chat matchmaking command
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
@Qualifier("local")
public class BotMatchmakingChatCommand implements BotChatCommand {
	private static final String TEAMS_OPTION_NAME = "teams";
	private static final String LEADERS_OPTION_NAME = "leaders";
	private static final double MAX_TEAMS_COUNT = 10d;
	private final BotLocalization localization;

	@Autowired
	public BotMatchmakingChatCommand(BotLocalization playerLocalization) {
		localization = playerLocalization;
	}

	@Override
	public void execute(ChatInputInteractionEvent event) {
		Optional<VoiceChannel> voiceChannelOptional = event.getInteraction().getMember()
			.map(member -> member.getVoiceState().block()).map(voiceState -> voiceState.getChannel().block());

		int teams = event.getOption(TEAMS_OPTION_NAME)
			.flatMap(ApplicationCommandInteractionOption::getValue)
			.map(ApplicationCommandInteractionOptionValue::asLong)
			.map(Long::intValue)
			.orElse(2);
		boolean leaders = event.getOption(LEADERS_OPTION_NAME)
			.flatMap(ApplicationCommandInteractionOption::getValue)
			.map(ApplicationCommandInteractionOptionValue::asBoolean)
			.orElse(false);

		voiceChannelOptional.ifPresentOrElse(voiceChannel -> {
			event.editReply(localization.getMessage("matchmaking_message", voiceChannel.getName())).block();

			List<Member> memberList = Optional.ofNullable(voiceChannel.getVoiceStates().collectList().block())
				.orElseGet(Collections::emptyList)
				.stream()
				.map(state -> state.getMember().block())
				.filter(member -> !(member == null || member.isBot()))
				.collect(Collectors.toList());

			Collections.shuffle(memberList);
			int membersPerTeam = memberList.size() / teams;

			List<String> teamNameList = IntStream.rangeClosed(1, memberList.size())
				.boxed()
				.map(Object::toString)
				.collect(Collectors.toList());
			List<List<Member>> teamsList = Lists.partition(memberList, membersPerTeam);
			Map<String, List<Member>> teamMembersToTeamNameMap = IntStream.range(0, teamsList.size())
				.boxed()
				.collect(Collectors.toMap(teamNameList::get, teamsList::get));

			StringBuilder stringBuilder = new StringBuilder(localization.getMessage("teams_message"))
				.append("\n");
			teamMembersToTeamNameMap.forEach((teamName, members) -> {
				stringBuilder.append(teamName).append(":\n");
				AtomicBoolean leader = new AtomicBoolean(true);
				members.forEach(member -> {
					if (leaders && leader.get()) {
						stringBuilder.append(localization.getMessage("team_leader_message"))
							.append("\t__").append(member.getDisplayName())
							.append("__\n").append(localization.getMessage("teammates_message"))
							.append("\t");
						leader.set(false);
					} else {
						stringBuilder.append("__").append(member.getDisplayName()).append("__\t");
					}
				});
				stringBuilder.append("\n\n");
			});

			event.editReply(stringBuilder.toString()).block();
		}, () -> event.editReply(localization.getMessage("not_in_the_channel_warning")).block());
	}

	@Override
	public String getDescription() {
		return localization.getMessage("matchmaking_description");
	}

	@Override
	public String getCommandName() {
		return "matchmaking";
	}

	@Override
	public List<ApplicationCommandOptionData> getOptions() {
		List<ApplicationCommandOptionData> optionDataList = new ArrayList<>();
		optionDataList.add(ApplicationCommandOptionData.builder()
			.name(TEAMS_OPTION_NAME)
			.type(Type.NUMBER.getValue())
			.maxValue(MAX_TEAMS_COUNT)
			.description(localization.getMessage("teams_count_description"))
			.required(false)
			.build()
		);
		optionDataList.add(ApplicationCommandOptionData.builder()
			.name(LEADERS_OPTION_NAME)
			.type(Type.BOOLEAN.getValue())
			.description(localization.getMessage("team_leaders_description"))
			.required(false)
			.build()
		);
		return optionDataList;
	}

}
