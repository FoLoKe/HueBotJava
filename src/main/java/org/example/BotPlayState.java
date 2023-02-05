package org.example;

import java.awt.*;
import java.util.Optional;

/**
 * BotPlayer play states
 *
 * @author Dmitry Marchenko
 * @since 05.02.2023
 */
public enum BotPlayState {
	stop,
	play {
		@Override
		public Optional<Image> getImage() {
			return BotResourceHandler.getImageByPath("ui/play.png");
		}
	},
	pause {
		@Override
		public Optional<Image> getImage() {
			return BotResourceHandler.getImageByPath("ui/pause.png");
		}
	};

	public Optional<Image> getImage() {
		return BotResourceHandler.getImageByPath("ui/stop.png");
	}
}
