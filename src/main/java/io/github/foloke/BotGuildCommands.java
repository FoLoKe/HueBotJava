package io.github.foloke;

import com.google.common.collect.Lists;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.DiscordObject;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest.Builder;
import discord4j.rest.entity.RestChannel;
import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.player.BotRepeatState;
import io.github.foloke.utils.BotLocalization;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static io.github.foloke.utils.BotPlayerCommandsUtils.*;

/**
 * Guild commnads
 *
 * @author Dmitry Marchenko
 * @since 11.02.2023
 */
// TODO: REFACTOR ALL!!!
public enum BotGuildCommands {
	player {
		@Override
		public void execute(ChatInputInteractionEvent event, Bot bot) {
			connectAndAddToQueue(event, bot, LINK_OPTION_NAME);
			createMessage(event, bot);
			event.editReply(BotLocalization.getPlayerMessage("player_created_message")).block();
		}

		@Override
		public List<ApplicationCommandOptionData> getOptions() {
			return Collections.singletonList(ApplicationCommandOptionData.builder()
				.name(LINK_OPTION_NAME)
				.description(BotLocalization.getPlayerMessage("link_description"))
				.type(Type.STRING.getValue())
				.required(false)
				.build()
			);
		}

		@Override
		public String getDescription() {
			return BotLocalization.getPlayerMessage("call_player_description");
		}

		@Override
		protected Boolean isEphemeral() {
			return true;
		}
	},
	q {
		@Override
		public void execute(ChatInputInteractionEvent event, Bot bot) {
			Optional<BotGuildPlayer> botGuildPlayerOptional = connectAndAddToQueue(event, bot, LINK_OPTION_NAME);
			if (botGuildPlayerOptional.isPresent() && !tryCreateMessage(event, bot)) {
				BotGuildPlayer botGuildPlayer = botGuildPlayerOptional.get();
				String responseMessage = BotLocalization.getPlayerMessage("track_added_message");
				if (botGuildPlayer.getBotRepeatState() == BotRepeatState.REPEAT) {
					responseMessage = BotLocalization.getPlayerMessage("repeat_enabled_warning");
				}
				event.editReply(responseMessage).block();
			} else {
				event.editReply(BotLocalization.getPlayerMessage("player_created_message"));
			}
		}

		@Override
		public List<ApplicationCommandOptionData> getOptions() {
			return Collections.singletonList(ApplicationCommandOptionData.builder()
				.name(LINK_OPTION_NAME)
				.description(BotLocalization.getPlayerMessage("link_description"))
				.type(Type.STRING.getValue())
				.required(true)
				.build()
			);
		}

		@Override
		public String getDescription() {
			return BotLocalization.getPlayerMessage("add_link_description");
		}

		@Override
		protected Boolean isEphemeral() {
			return true;
		}
	},
	volume {
		@Override
		public void execute(ChatInputInteractionEvent event, Bot bot) {
			Interaction interaction = event.getInteraction();
			interaction.getGuildId().ifPresent(guildId -> {
				BotGuildPlayer botGuildPlayer = bot.connect(guildId, interaction);
				boolean isPlayerNew = tryCreateMessage(event, bot);
				Optional<Long> volumeValue = event.getOption(AMOUNT_OPTION_NAME)
					.flatMap(ApplicationCommandInteractionOption::getValue)
					.map(ApplicationCommandInteractionOptionValue::asLong);
				if (volumeValue.isPresent()) {
					botGuildPlayer.setVolumePercent(volumeValue.get().floatValue());
					if (!isPlayerNew) {
						event.editReply(
							BotLocalization.getPlayerMessage("volume_set_message", botGuildPlayer.getVolumePercent())
						).block();
					}
				} else {
					if (!isPlayerNew) {
						event.editReply(
							BotLocalization.getPlayerMessage("volume_is_message", botGuildPlayer.getVolumePercent())
						).block();
					}
				}
			});
		}

		@Override
		public List<ApplicationCommandOptionData> getOptions() {
			return Collections.singletonList(ApplicationCommandOptionData.builder()
				.name(AMOUNT_OPTION_NAME)
				.description(BotLocalization.getPlayerMessage("volume_amount_description"))
				.type(Type.INTEGER.getValue())
				.required(false)
				.build()
			);
		}

		@Override
		String getDescription() {
			return BotLocalization.getPlayerMessage("volume_description") ;
		}

		@Override
		protected Boolean isEphemeral() {
			return true;
		}
	},
	roll {
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
			Optional<String> message = event.getOption(FOR_OPTION_NAME)
				.flatMap(ApplicationCommandInteractionOption::getValue)
				.map(ApplicationCommandInteractionOptionValue::asString);
			StringBuilder replyStringBuilder = new StringBuilder(
				BotLocalization.getPlayerMessage( "d_roll", d, count)
			);
			message.ifPresent(m -> replyStringBuilder.append(" ").append(m));
			replyStringBuilder.append(":\n");

			String caption = replyStringBuilder.toString();
			event.editReply(String.join(caption, BotLocalization.getPlayerMessage("rolling_message"))).block();
			IntStream.rangeClosed(1, count).forEach(index -> {
				int diceValue = 1 + random.nextInt(d);
				String diceReply = diceValue == 1 ? " " + BotLocalization.getPlayerMessage("critical_misfortune")
					: diceValue == d ? " " + BotLocalization.getPlayerMessage("critical") : "";

				replyStringBuilder.append(index).append(": \t\t").append(diceValue).append(diceReply).append("\n");
			});
			event.editReply(replyStringBuilder.toString()).block();
		}

		@Override
		String getDescription() {
			return BotLocalization.getPlayerMessage("roll_description");
		}

		@Override
		public List<ApplicationCommandOptionData> getOptions() {
			List<ApplicationCommandOptionData> options = new ArrayList<>();
			options.add(ApplicationCommandOptionData.builder()
				.name(COUNT_OPTION_NAME)
				.description(BotLocalization.getPlayerMessage("dices_count"))
				.type(Type.INTEGER.getValue())
				.maxValue(MAX_COUNT_VALUE)
				.required(false)
				.build()
			);
			options.add(ApplicationCommandOptionData.builder()
				.name(D_OPTION_NAME)
				.description(BotLocalization.getPlayerMessage("dice_type"))
				.type(Type.INTEGER.getValue())
				.maxValue(MAX_D_VALUE)
				.required(false)
				.build()
			);
			options.add(ApplicationCommandOptionData.builder()
				.name(FOR_OPTION_NAME)
				.description(BotLocalization.getPlayerMessage("for_option_description"))
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
				event.editReply(BotLocalization.getPlayerMessage("matchmaking_message", voiceChannel.getName())).block();

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

				StringBuilder stringBuilder = new StringBuilder(BotLocalization.getPlayerMessage("teams_message"))
					.append("\n");
				teamMembersToTeamNameMap.forEach((teamName, members) -> {
					stringBuilder.append(teamName).append(":\n");
					AtomicBoolean leader = new AtomicBoolean(true);
					members.forEach(member -> {
						if (leaders && leader.get()) {
							stringBuilder.append(BotLocalization.getPlayerMessage("team_leader_message"))
								.append("\t__").append(member.getDisplayName())
								.append("__\n").append(BotLocalization.getPlayerMessage("teammates_message"))
								.append("\t");
							leader.set(false);
						} else {
							stringBuilder.append("__").append(member.getDisplayName()).append("__\t");
						}
					});
					stringBuilder.append("\n\n");
				});

				event.editReply(stringBuilder.toString()).block();
			}, () -> event.editReply(BotLocalization.getPlayerMessage("not_in_the_channel_warning")).block());
		}

		@Override
		String getDescription() {
			return BotLocalization.getPlayerMessage("matchmaking_description");
		}

		@Override
		public List<ApplicationCommandOptionData> getOptions() {
			List<ApplicationCommandOptionData> optionDataList = new ArrayList<>();
			optionDataList.add(ApplicationCommandOptionData.builder()
				.name(TEAMS_OPTION_NAME)
				.type(Type.NUMBER.getValue())
				.maxValue(10d)
				.description(BotLocalization.getPlayerMessage("teams_count_description"))
				.required(false)
				.build()
			);
			optionDataList.add(ApplicationCommandOptionData.builder()
				.name(LEADERS_OPTION_NAME)
				.type(Type.BOOLEAN.getValue())
				.description(BotLocalization.getPlayerMessage("team_leaders_description"))
				.required(false)
				.build()
			);
			return optionDataList;
		}
	},
	clean {
		private final Lock messageDeleteMutex = new ReentrantLock();
		private final transient Executor threadPoolExecutor = new ScheduledThreadPoolExecutor(1);

		@Override
		void execute(ChatInputInteractionEvent event, Bot bot) {
			if (messageDeleteMutex.tryLock()) {
				threadPoolExecutor.execute(() -> cleanMessages(event, bot.getCleaningImageUrl()));
			} else {
				event.editReply(BotLocalization.getPlayerMessage("delete_operation_in_use")).block();
			}
		}

		private void cleanMessages(ChatInputInteractionEvent event, String cleaningImageUrl) {
			try {
				logger.log(Level.INFO, () -> "Started deletion thread " + Thread.currentThread());
				event.editReply()
					.withEmbeds(EmbedCreateSpec.create()
					.withImage(cleaningImageUrl)
					.withTitle(BotLocalization.getPlayerMessage("searching_messages_to_delete_message"))
				).block();
				AtomicInteger counter = new AtomicInteger();
				Snowflake channelId = event.getInteraction().getChannelId();
				event.getInteraction()
					.getGuildId()
					.flatMap(guildId -> event.getInteraction().getGuild().blockOptional())
					.ifPresent(guild -> {
						Snowflake botUserId = guild.getClient().getSelfId();
						guild.getChannelById(channelId).blockOptional().ifPresent(guildChannel -> {
							RestChannel restChannel = guildChannel.getRestChannel();
							List<Message> messages = findMessagesToDelete(guild, restChannel, botUserId);
							if (messages != null) {
								int size = messages.size();
								logger.log(Level.INFO, () -> "Deleteing " + size + " messages");
								counter.set(size);
								deleteMessages(messages);
							}
						});
					});
				logger.log(Level.INFO, "Deletion completed");
				event.editReply()
					.withEmbeds(EmbedCreateSpec.create()
					.withImage(cleaningImageUrl)
					.withTitle(BotLocalization.getPlayerMessage("found_and_deleted_message", counter.get())))
					.block();
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Mesage cleananing error", e);
			} finally {
				messageDeleteMutex.unlock();
			}
		}

		private void deleteMessages(List<Message> messages) {
			// NO BULK DELETE CUZ FUCKING DISCORD API 14 days limit
			messages.forEach(message -> {
				message.delete().block();
				logger.log(Level.INFO,
					() -> "Deleted: " + message.getId() + " " + message.getTimestamp());
			});
		}

		private List<Message> findMessagesToDelete(DiscordObject guild, RestChannel restChannel, Snowflake botUserId) {
			return restChannel.getMessagesBefore(Snowflake.of(Instant.now().minusMillis(10000)))
				.filter(messageData -> messageData.author().id().equals(Id.of(botUserId.asString())))
				.map(messageData -> new Message(guild.getClient(), messageData))
				.collectList()
				.block();
		}

		@Override
		String getDescription() {
			return BotLocalization.getPlayerMessage("delete_description");
		}
	};

	public static final String AMOUNT_OPTION_NAME = "amount";
	public static final String LINK_OPTION_NAME = "link";
	protected final Logger logger = Logger.getLogger(BotGuildCommands.class.getName());

	abstract void execute(ChatInputInteractionEvent event, Bot bot);

	public List<ApplicationCommandOptionData> getOptions() {
		return Collections.emptyList();
	}

	abstract String getDescription();

	/**
	 * Executes command if exists
	 */
	public static void runCommand(ChatInputInteractionEvent event, Bot bot) {
		Arrays.stream(values())
			.filter(command -> event.getCommandName().equals(command.name()))
			.findFirst()
			.ifPresent(command -> {
				event.deferReply().withEphemeral(command.isEphemeral()).block();
				command.execute(event, bot);
			});
	}

	protected Boolean isEphemeral() {
		return false;
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
