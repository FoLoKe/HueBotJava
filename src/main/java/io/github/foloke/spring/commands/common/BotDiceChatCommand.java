package io.github.foloke.spring.commands.common;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.ApplicationCommandOption.Type;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.github.foloke.utils.commands.BotChatCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Dice roll chat command
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
public class BotDiceChatCommand implements BotChatCommand {

	private final Random random = new Random();
	private static final String D_OPTION_NAME = "d";
	private static final String COUNT_OPTION_NAME = "x";
	private static final String FOR_OPTION_NAME = "for";
	private static final long DEFAULT_D_VALUE = 6L;
	private static final long DEFAULT_COUNT_VALUE = 1L;

	private static final double MAX_D_VALUE = Integer.MAX_VALUE;
	private static final double MAX_COUNT_VALUE = 10;
	private final BotLocalization localization;

	@Autowired
	public BotDiceChatCommand(BotLocalization playerLocalization) {
		localization = playerLocalization;
	}

	@Override
	public void execute(ChatInputInteractionEvent event) {
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
			localization.getMessage("d_roll", d, count)
		);
		message.ifPresent(m -> replyStringBuilder.append(" ").append(m));
		replyStringBuilder.append(":\n");

		String caption = replyStringBuilder.toString();
		event.editReply(String.join(caption, localization.getMessage("rolling_message"))).block();
		IntStream.rangeClosed(1, count).forEach(index -> {
			int diceValue = 1 + random.nextInt(d);
			String diceReply = diceValue == 1 ? " " + localization.getMessage("critical_misfortune")
				: diceValue == d ? " " + localization.getMessage("critical") : "";

			replyStringBuilder.append(index).append(": \t\t").append(diceValue).append(diceReply).append("\n");
		});
		event.editReply(replyStringBuilder.toString()).block();
	}

	@Override
	public String getDescription() {
		return localization.getMessage("roll_description");
	}

	@Override
	public String getCommandName() {
		return "roll";
	}

	@Override
	public List<ApplicationCommandOptionData> getOptions() {
		List<ApplicationCommandOptionData> options = new ArrayList<>();
		options.add(ApplicationCommandOptionData.builder()
			.name(COUNT_OPTION_NAME)
			.description(localization.getMessage("dices_count"))
			.type(Type.INTEGER.getValue())
			.maxValue(MAX_COUNT_VALUE)
			.required(false)
			.build()
		);
		options.add(ApplicationCommandOptionData.builder()
			.name(D_OPTION_NAME)
			.description(localization.getMessage("dice_type"))
			.type(Type.INTEGER.getValue())
			.maxValue(MAX_D_VALUE)
			.required(false)
			.build()
		);
		options.add(ApplicationCommandOptionData.builder()
			.name(FOR_OPTION_NAME)
			.description(localization.getMessage("for_option_description"))
			.type(Type.STRING.getValue())
			.required(false)
			.build()
		);
		return options;
	}

	@Override
	public boolean isEphemeral() {
		return false;
	}
}
