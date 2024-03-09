package io.github.foloke.spring.config;

import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.service.ApplicationService;
import io.github.foloke.spring.commands.BotChatCommandsService;
import io.github.foloke.spring.listeners.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Bot configuration
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Configuration
public class BotConfig {
	public static final String UI_BUTTONS_QUALIFIER = "ui";
	private static final String BOT_IS_ACTIVE_FORMAT = "Bot is active %s";
	private static final Logger log = LoggerFactory.getLogger(BotConfig.class);
	private static final String BOT_STARTUP_ERROR_MESSAGE = "Bot startup error, retry in %dms:";
	private static final int RETRY_INTERVAL_MS = 10000;

	@Value("${token}")
	private String token;
	@Value("${debug}")
	private boolean isDebug;
	private final BotChatCommandsService botChatCommandsService;

	@Autowired
	public BotConfig(BotChatCommandsService botChatCommandsService) {
		this.botChatCommandsService = botChatCommandsService;
	}

	/**
	 * Initiates and configurates discord interactions
	 */
	@Bean
	public <T extends Event> GatewayDiscordClient gatewayDiscordClient(List<EventListener<T>> eventListenerList)
		throws InterruptedException
	{
		return createDiscordClient(eventListenerList);
	}

	private <T extends Event> GatewayDiscordClient createDiscordClient(List<EventListener<T>> eventListenerList)
		throws InterruptedException
	{
		try {
			log.info("Trying to connect to Discord");
			GatewayDiscordClient gatewayDiscordClient = DiscordClientBuilder.create(token)
				.build()
				.gateway()
				.setEnabledIntents(IntentSet.all())
				.setInitialPresence(shardInfo -> ClientPresence.doNotDisturb(ClientActivity.custom("starting")))
				.login()
				.flatMap(client -> {
					eventListenerList.forEach(listener -> client.getEventDispatcher()
						.on(listener.getTypeClass())
						.subscribe(listener::executeCommand)
					);
					registerGlobalCommands(client);
					log.info(String.format(BOT_IS_ACTIVE_FORMAT, Thread.currentThread()));
					return Mono.just(client);
				}).block();
			if (gatewayDiscordClient != null) {
				if (isDebug) {
					gatewayDiscordClient.updatePresence(ClientPresence.doNotDisturb(ClientActivity.custom("debugging")))
						.block();
				} else {
					gatewayDiscordClient.updatePresence(ClientPresence.online(ClientActivity.listening("commands")))
						.block();
				}
			}
			return gatewayDiscordClient;
		} catch (Exception e) {
			log.error(String.format(BOT_STARTUP_ERROR_MESSAGE, RETRY_INTERVAL_MS), e);
		}
		Thread.sleep(RETRY_INTERVAL_MS);
		return createDiscordClient(eventListenerList);
	}


	/**
	 * Registers Commands for spectifed guild
	 */
	public void registerGlobalCommands(GatewayDiscordClient discordClient) {
		Long applicationId = discordClient.getRestClient().getApplicationId().block();
		ApplicationService applicationService = discordClient.getRestClient().getApplicationService();
		if (applicationId != null) {
			botChatCommandsService.getGlobalApplicationCommandRequestList().forEach(commandRequest ->
				applicationService.createGlobalApplicationCommand(applicationId, commandRequest).block()
			);
		}
	}
}
