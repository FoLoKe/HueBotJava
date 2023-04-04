package io.github.foloke;

import com.google.common.collect.Lists;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest.Builder;
import io.github.foloke.player.BotPlayer;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.github.foloke.utils.BotPlayerCommandsUtils.*;

/**
 * Guild commnads
 *
 * @author Dmitry Marchenko
 * @since 11.02.2023
 */
public enum BotGuildCommands {
	player {
		@Override
		public void execute(ChatInputInteractionEvent event, Bot bot) {
			connectAndAddToQueue(event, bot, LINK_OPTION_NAME);
			createMessage(event, bot);
		}

		@Override
		public List<ApplicationCommandOptionData> getOptions() {
			return Collections.singletonList(ApplicationCommandOptionData.builder()
				.name(LINK_OPTION_NAME)
				.description(YOUTUBE_LINK_MESSAGE)
				.type(Type.STRING.getValue())
				.required(false)
				.build()
			);
		}

		@Override
		public String getDescription() {
			return "Call player to this channel";
		}
	},
	q {
		@Override
		public void execute(ChatInputInteractionEvent event, Bot bot) {
			connectAndAddToQueue(event, bot, LINK_OPTION_NAME);
			if (!tryCreateMessage(event, bot)) {
				event.reply(InteractionApplicationCommandCallbackSpec.builder()
					.content("Track added")
					.ephemeral(true)
					.build()
				).block();
			}
			;
		}

		@Override
		public List<ApplicationCommandOptionData> getOptions() {
			return Collections.singletonList(ApplicationCommandOptionData.builder()
				.name(LINK_OPTION_NAME)
				.description(YOUTUBE_LINK_MESSAGE)
				.type(Type.STRING.getValue())
				.required(true)
				.build()
			);
		}

		@Override
		public String getDescription() {
			return "Add track to queue";
		}
	},
	volume {
		@Override
		public void execute(ChatInputInteractionEvent event, Bot bot) {
			Interaction interaction = event.getInteraction();
			interaction.getGuildId().ifPresent(guildId -> {
				BotPlayer botPlayer = bot.connect(guildId, interaction);
				boolean isPlayerNew = tryCreateMessage(event, bot);
				Optional<Long> volumeValue = event.getOption(AMOUNT_OPTION_NAME)
					.flatMap(ApplicationCommandInteractionOption::getValue)
					.map(ApplicationCommandInteractionOptionValue::asLong);
				if (volumeValue.isPresent()) {
					botPlayer.setVolume(volumeValue.get());
					if (!isPlayerNew) {
						event.reply(
							InteractionApplicationCommandCallbackSpec.builder()
								.content("Volume set to: " + botPlayer.getVolume() + "%")
								.ephemeral(true)
								.build()
						).block();
					}
				} else {
					if (!isPlayerNew) {
						event.reply(InteractionApplicationCommandCallbackSpec.builder()
							.content("Volume is: " + botPlayer.getVolume() + "%")
							.ephemeral(true)
							.build()
						).block();
					}
				}
			});
		}

		@Override
		public List<ApplicationCommandOptionData> getOptions() {
			return Collections.singletonList(ApplicationCommandOptionData.builder()
				.name(AMOUNT_OPTION_NAME)
				.description("amount of volume to set")
				.type(Type.INTEGER.getValue())
				.required(false)
				.build()
			);
		}

		@Override
		String getDescription() {
			return "Get or set volume to the active player";
		}
	}, roll {
		private final Random random = new Random();
		private static final String D_OPTION_NAME = "d";
		private static final String COUNT_OPTION_NAME = "x";
		private static final String FOR_OPTION_NAME = "for";
		private static final long DEFAULT_D_VALUE = 6L;
		private static final long DEFAULT_COUNT_VALUE = 1L;

		private static final double MAX_D_VALUE = Integer.MAX_VALUE;
		private static final double MAX_COUNT_VALUE = 10;

		@Override
		void execute(ChatInputInteractionEvent event, Bot bot) {
			int d = event.getOption(D_OPTION_NAME).flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asLong)
				.orElse(DEFAULT_D_VALUE)
				.intValue();
			int count = event.getOption(COUNT_OPTION_NAME).flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asLong)
				.orElse(DEFAULT_COUNT_VALUE)
				.intValue();
			Optional<String> message = event.getOption("for")
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asString);
			StringBuilder replyStringBuilder = new StringBuilder("D").append(d).append(" roll x").append(count);
			message.ifPresent(m -> {
				replyStringBuilder.append(" ").append(m);
			});
			replyStringBuilder.append(":\n");

			String caption = replyStringBuilder.toString();
			event.reply(String.join(caption, "ROLLING...")).block();
			IntStream.rangeClosed(1, count).forEach(index -> {
				int diceValue = 1 + random.nextInt(d);
				String diceReply = diceValue == 1 ? " oh no..." : diceValue == d ? " Crit!" : "";

				replyStringBuilder.append(index).append(": \t\t").append(diceValue).append(diceReply).append("\n");
			});
			event.editReply(replyStringBuilder.toString()).block();
		}

		@Override
		String getDescription() {
			return "Roll dice!";
		}

		@Override
		public List<ApplicationCommandOptionData> getOptions() {
			List<ApplicationCommandOptionData> options = new ArrayList<>();
			options.add(ApplicationCommandOptionData.builder()
				.name(COUNT_OPTION_NAME)
				.description("Count of dices")
				.type(Type.INTEGER.getValue())
				.maxValue(MAX_COUNT_VALUE)
				.required(false)
				.build()
			);
			options.add(ApplicationCommandOptionData.builder().name(D_OPTION_NAME)
				.description("Dice type (D4, D6, D12 etc)")
				.type(Type.INTEGER.getValue())
				.maxValue(MAX_D_VALUE)
				.required(false)
				.build()
			);
			options.add(ApplicationCommandOptionData.builder()
				.name("for")
				.description("roll purpose")
				.type(Type.STRING.getValue())
				.required(false)
				.build()
			);
			return options;
		}
	}, matchmaking {

		private static final String TEAMS_OPTION_NAME = "teams";
		private static final String LEADERS_OPTION_NAME = "leaders";

		@Override
		void execute(ChatInputInteractionEvent event, Bot bot) {
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
				event.reply("Matchmaikng in channel " + voiceChannel.getName() + "...").block();

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

				StringBuilder stringBuilder = new StringBuilder("Teams are:\n");
				teamMembersToTeamNameMap.forEach((teamName, members) -> {
					stringBuilder.append(teamName).append(":\n");
					AtomicBoolean leader = new AtomicBoolean(true);
					members.forEach(member -> {
						if (leaders && leader.get()) {
							stringBuilder.append("Leader:\t__").append(member.getDisplayName())
								.append("__\nTeammates:\t");
							leader.set(false);
						} else {
							stringBuilder.append("__").append(member.getDisplayName()).append("__\t");
						}
					});
					stringBuilder.append("\n\n");
				});

				event.editReply(stringBuilder.toString()).block();
			}, () -> {
				event.reply("You are noy in the channel").block();
			});
		}

		@Override
		String getDescription() {
			return "voice-cahnnel matchmaking";
		}

		@Override
		public List<ApplicationCommandOptionData> getOptions() {
			List<ApplicationCommandOptionData> optionDataList = new ArrayList<>();
			optionDataList.add(ApplicationCommandOptionData.builder()
				.name(TEAMS_OPTION_NAME)
				.type(Type.NUMBER.getValue())
				.maxValue(10d)
				.description("Teams count")
				.required(false)
				.build()
			);
			optionDataList.add(ApplicationCommandOptionData.builder()
				.name(LEADERS_OPTION_NAME)
				.type(Type.BOOLEAN.getValue())
				.description("Choose leaders")
				.required(false)
				.build()
			);
			return optionDataList;
		}
	};

	public static final String AMOUNT_OPTION_NAME = "amount";
	public static final String LINK_OPTION_NAME = "link";
	public static final String YOUTUBE_LINK_MESSAGE = "YouTube track or playlist link";

	abstract void execute(ChatInputInteractionEvent event, Bot bot);

	public List<ApplicationCommandOptionData> getOptions() {
		return Collections.emptyList();
	}

	abstract String getDescription();

	/**
	 * Executes command if exists
	 */
	public static void runCommand(ChatInputInteractionEvent event, Bot bot) {
		event.deferReply();
		Arrays.stream(values())
			.filter(command -> event.getCommandName().equals(command.name()))
			.findFirst()
			.ifPresent(command -> command.execute(event, bot));
	}

	/**
	 * GetCommands to register
	 */
	public static List<ApplicationCommandRequest> getApplicationCommandRequestList() {
		return Arrays.stream(values()).map(botCommand -> {
			Builder playerCommandRequestBuilder = ApplicationCommandRequest.builder()
				.name(botCommand.name())
				.description(botCommand.getDescription());
			List<ApplicationCommandOptionData> optionDataList = botCommand.getOptions();
			if (!optionDataList.isEmpty()) {
				playerCommandRequestBuilder.addAllOptions(optionDataList).build();
			}
			return playerCommandRequestBuilder.build();
		}).collect(Collectors.toList());
	}


}
