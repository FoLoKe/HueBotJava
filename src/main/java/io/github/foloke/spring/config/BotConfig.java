package io.github.foloke.spring.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest.Builder;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.service.ApplicationService;
import io.github.foloke.spring.listeners.EventListener;
import io.github.foloke.utils.commands.BotChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Bot configuration
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Configuration
public class BotConfig {
	private static final String BOT_IS_ACTIVE_FORMAT = "Bot is active %s";
	private static final Logger log = LoggerFactory.getLogger(BotConfig.class);

	@Value("${token}")
	private String token;
	@Value("${devGuildId}")
	private long devGuildId;
	private final List<BotChatCommand> chatCommandsList;

	@Autowired
	public BotConfig(List<BotChatCommand> chatCommandsList) {
		this.chatCommandsList = new ArrayList<>(chatCommandsList);
	}

	/**
	 * Initiates and configurates discord interactions
	 */
	@Bean
	public <T extends Event> GatewayDiscordClient gatewayDiscordClient(List<EventListener<T>> eventListenerList) {
		try {
			return DiscordClientBuilder.create(token)
				.build()
				.gateway()
				.setEnabledIntents(IntentSet.all())
				.login()
				.flatMap(client -> {
					eventListenerList.forEach(listener -> client.getEventDispatcher()
						.on(listener.getTypeClass())
						.subscribe(listener::executeCommand)
					);
					registerGuildCommands(client, devGuildId);
					log.info(String.format(BOT_IS_ACTIVE_FORMAT, Thread.currentThread()));
					return Mono.just(client);
				}).block();
		} catch (Exception e) {
			log.error("bot startup error", e);
		}
		return null;
	}


	/**
	 * Registers Commands for spectifed guild
	 */
	public void registerGuildCommands(GatewayDiscordClient discordClient, long guildId) {
		Long applicationId = discordClient.getRestClient().getApplicationId().block();
		ApplicationService applicationService = discordClient.getRestClient().getApplicationService();
		if (applicationId != null) {
			getApplicationCommandRequestList().forEach(commandRequest ->
				applicationService.createGuildApplicationCommand(
					applicationId,
					guildId,
					commandRequest
				).block()
			);
		}
	}

	public List<ApplicationCommandRequest> getApplicationCommandRequestList() {
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
