package org.example;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.core.object.command.Interaction;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest.Builder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.example.utils.BotPlayerCommandsUtils.*;

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
			connectAndAddToQueue(event, bot, LINK);
			createMessage(event, bot);
		}

		@Override
		public List<ApplicationCommandOptionData> getOptions() {
			return Collections.singletonList(ApplicationCommandOptionData.builder()
				.name(LINK)
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
			connectAndAddToQueue(event, bot, LINK);
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
				.name(LINK)
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
				Optional<Long> volumeValue = event.getOption(AMOUNT)
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
				.name(AMOUNT)
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
	};

	public static final String AMOUNT = "amount";
	public static final String LINK = "link";
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
