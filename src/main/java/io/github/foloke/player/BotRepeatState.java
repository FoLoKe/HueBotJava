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
	none,
	repeat {
		@Override
		public Optional<Image> getImage() {
			return BotResourceHandler.getImageByPath("ui/repeat.png");
		}
	},
	repeatQ {
		@Override
		public Optional<Image> getImage() {
			return BotResourceHandler.getImageByPath("ui/repeatQ.png");
		}
	};

	public Optional<Image> getImage() {
		return Optional.empty();
	}

	public static BotRepeatState getNextState(BotRepeatState botRepeatState, BotRepeatState botTargetRepeatState) {
		if (botRepeatState == botTargetRepeatState) {
			return none;
		}
		return botTargetRepeatState;
	}
}
