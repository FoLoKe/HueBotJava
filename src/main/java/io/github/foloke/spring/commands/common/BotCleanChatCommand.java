package io.github.foloke.spring.commands.common;

import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.DiscordObject;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.Id;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.rest.entity.RestChannel;
import io.github.foloke.spring.services.localization.BotLocalization;
import io.github.foloke.utils.commands.BotChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Clean all bot messages from chat
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
public class BotCleanChatCommand implements BotChatCommand {
	private static final int TIME_RUN_DELTA = 10000;
	private final Logger log = LoggerFactory.getLogger(BotCleanChatCommand.class);
	private final BotLocalization localization;

	@Value("${cleaningImageUrl}")
	private String cleaningImageUrl;

	@Autowired
	public BotCleanChatCommand(BotLocalization playerLocalization) {
		localization = playerLocalization;
	}

	private final Lock messageDeleteMutex = new ReentrantLock();
	private final Executor threadPoolExecutor = new ScheduledThreadPoolExecutor(1);

	@Override
	public void execute(ChatInputInteractionEvent event) {
		if (messageDeleteMutex.tryLock()) {
			threadPoolExecutor.execute(() -> cleanMessages(event, cleaningImageUrl));
		} else {
			event.editReply(localization.getMessage("delete_operation_in_use")).block();
		}
	}

	@Override
	public boolean isEphemeral() {
		return false;
	}

	private void cleanMessages(ChatInputInteractionEvent event, String cleaningImageUrl) {
		try {
			log.info(String.format("Started deletion thread %s", Thread.currentThread()));
			event.editReply()
				.withEmbeds(EmbedCreateSpec.create()
					.withImage(cleaningImageUrl)
					.withTitle(localization.getMessage("searching_messages_to_delete_message"))
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
							log.info(String.format("Deleteing %s messages", size));
							counter.set(size);
							deleteMessages(messages);
						}
					});
				});
			log.info("Deletion completed");
			event.editReply()
				.withEmbeds(EmbedCreateSpec.create()
					.withImage(cleaningImageUrl)
					.withTitle(localization.getMessage("found_and_deleted_message", counter.get())))
				.block();
		} catch (Exception e) {
			log.error("Mesage cleananing error", e);
		} finally {
			messageDeleteMutex.unlock();
		}
	}

	private void deleteMessages(List<Message> messages) {
		// NO BULK DELETE CUZ FUCKING DISCORD API 14 days limit
		messages.forEach(message -> {
			message.delete().block();
			log.info(String.format("Deleted: %s %s", message.getId().asString(), message.getTimestamp()));
		});
	}

	private List<Message> findMessagesToDelete(DiscordObject guild, RestChannel restChannel, Snowflake botUserId) {
		return restChannel.getMessagesBefore(Snowflake.of(Instant.now().minusMillis(TIME_RUN_DELTA)))
			.filter(messageData -> messageData.author().id().equals(Id.of(botUserId.asString())))
			.map(messageData -> new Message(guild.getClient(), messageData))
			.collectList()
			.block();
	}

	@Override
	public String getDescription() {
		return localization.getMessage("delete_description");
	}

	@Override
	public String getCommandName() {
		return "clean";
	}

	@Override
	public List<ApplicationCommandOptionData> getOptions() {
		return new ArrayList<>();
	}
}
