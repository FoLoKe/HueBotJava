package io.github.foloke.spring.listeners;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import io.github.foloke.spring.services.localization.BotLocalization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.SpringVersion;
import org.springframework.stereotype.Service;

/**
 * Handles chat messages interactions
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Service
public class MessageListener implements EventListener<MessageCreateEvent> {
	private static final String DEBUG_COMMAND = "!ping";
	private final BotLocalization playerLocalization;
	@Value("${pom.version}")
	private String buildInfo;

	@Autowired
	public MessageListener(BotLocalization playerLocalization) {
		this.playerLocalization = playerLocalization;
	}

	@Override
	public void executeCommand(MessageCreateEvent event) {
		Message message = event.getMessage();
		if (DEBUG_COMMAND.equalsIgnoreCase(message.getContent())) {
			message.getChannel().flatMap(channel -> {
				String guildId = message.getGuildId().map(Snowflake::asString).orElse("");
				String channelId = message.getChannelId().asString();
				String userId = message.getUserData().id().asString();
				return channel.createMessage(playerLocalization.getMessage(
					"debug_message", guildId, channelId, userId, buildInfo, SpringVersion.getVersion()
				));
			}).block();
		}
	}

	@Override
	public Class<MessageCreateEvent> getTypeClass() {
		return MessageCreateEvent.class;
	}
}
