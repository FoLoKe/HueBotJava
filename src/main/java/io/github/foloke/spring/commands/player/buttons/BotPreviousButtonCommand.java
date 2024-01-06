package io.github.foloke.spring.commands.player.buttons;

import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.utils.commands.BotButtonCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * UI "⏮" Button
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
@Order(0)
public class BotPreviousButtonCommand implements BotButtonCommand {
	@Override
	public void execute(BotGuildPlayer botGuildPlayer) {
		botGuildPlayer.playPrevious();
	}

	@Override
	public String getCommandName() {
		return "previous";
	}

	@Override
	public String getButtonText() {
		return "⏮";
	}
}
