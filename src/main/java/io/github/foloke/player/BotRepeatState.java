package io.github.foloke.player;

import io.github.foloke.BotResourceHandler;

import java.awt.*;
import java.util.Optional;

/**
 * BotPlayer repeat states
 *
 * @since 05.02.2023
 */
public enum BotRepeatState {
	NONE,
	REPEAT {
		@Override
		public Optional<Image> getImage() {
			return BotResourceHandler.getImageByPath(UI_REPEAT_PNG);
		}
	},
	REPEAT_QUEUE {
		@Override
		public Optional<Image> getImage() {
			return BotResourceHandler.getImageByPath(UI_REPEAT_Q_PNG);
		}
	};

	private static final String UI_REPEAT_PNG = "ui/repeat.png";
	private static final String UI_REPEAT_Q_PNG = "ui/repeatQ.png";

	public Optional<Image> getImage() {
		return Optional.empty();
	}

	/**
	 * Returns player next state based on repeat state
	 */
	public static BotRepeatState getNextState(BotRepeatState botRepeatState, BotRepeatState botTargetRepeatState) {
		if (botRepeatState == botTargetRepeatState) {
			return NONE;
		}
		return botTargetRepeatState;
	}
}
