package io.github.foloke.spring.commands.player.buttons;

import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.utils.commands.BotButtonCommand;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import static io.github.foloke.spring.config.BotConfig.UI_BUTTONS_QUALIFIER;

/**
 * UI "⏯" button
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
@Qualifier(UI_BUTTONS_QUALIFIER)
@Order(1)
public class BotPlayButtonCommand implements BotButtonCommand {
	@Override
	public void execute(BotGuildPlayer botGuildPlayer) {
		botGuildPlayer.play();
	}

	@Override
	public String getButtonText() {
		return "⏯";
	}

	@Override
	public String getCommandName() {
		return "play";
	}
}
