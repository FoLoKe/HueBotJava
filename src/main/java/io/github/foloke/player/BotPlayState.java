package io.github.foloke.player;

import io.github.foloke.BotResourceHandler;

import java.awt.*;
import java.util.Optional;

/**
 * BotPlayer play states
 *
 * @author Dmitry Marchenko
 * @since 05.02.2023
 */
public enum BotPlayState {
	STOP,
	PLAY {
		@Override
		public Optional<Image> getImage() {
			return BotResourceHandler.getImageByPath(UI_PLAY_PNG);
		}
	},
	PAUSE {
		@Override
		public Optional<Image> getImage() {
			return BotResourceHandler.getImageByPath(UI_PAUSE_PNG);
		}
	};

	private static final String UI_PAUSE_PNG = "ui/pause.png";
	private static final String UI_PLAY_PNG = "ui/play.png";
	private static final String UI_STOP_PNG = "ui/stop.png";

	public Optional<Image> getImage() {
		return BotResourceHandler.getImageByPath(UI_STOP_PNG);
	}
}
