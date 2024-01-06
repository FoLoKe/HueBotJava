package io.github.foloke.spring.commands.player.buttons;

import io.github.foloke.player.BotGuildPlayer;
import io.github.foloke.utils.commands.BotButtonCommand;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * UI "🔁" button
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component
@Order(4)
public class BotRepeatQueueButtonCommand implements BotButtonCommand {
	@Override
	public void execute(BotGuildPlayer botGuildPlayer) {
		botGuildPlayer.toggleRepeatQueue();
	}

	@Override
	public String getButtonText() {
		return "🔁";
	}

	@Override
	public String getCommandName() {
		return "repeat_queue";
	}
}
