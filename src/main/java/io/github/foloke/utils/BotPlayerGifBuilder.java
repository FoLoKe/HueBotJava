package io.github.foloke.utils;

import de.cerus.jgif.GifEncoder;
import de.cerus.jgif.GifImage;
import discord4j.core.spec.MessageCreateFields.File;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Renderer of GifImages for player
 *
 * @author Dmitry Marchenko
 * @since 05.02.2023
 */
public class BotPlayerGifBuilder {

	public static final int ANIMATION_FRAMES = 200;
	public static final int FRAME_DURATION = 50;
	public static final int FADE_IN_OUT_CYCLE = 2000;
	public static final float DEFAULT_ALPHA = 0.8f;
	public static final float PERCENT = 100f;
	public static final int VOLUME_SYMBOLS_COUNT = 5;
	public static final int VOLUME_SYMBOL_LEN = 2;
	public static final int IMAGE_WIDTH = 320;
	public static final int IMAGE_HEIGHT = 160;
	public static final int THIRD_ROW_IMAGES_Y = 74;
	public static final int THIRD_ROW_TEXT_Y = 86;
	public static final int FIRST_ROW_TEXT_Y = 40;
	public static final int START_X = 30;
	public static final int REPEAT_STATE_X = 225;
	public static final int DEFAULT_FONT_SIZE = 16;
	public static final int STATE_X = 200;
	public static final int SECOND_ROW_TEXT_Y = 65;
	private static final String SANS_FONT = "Sans";
	public static final float MIN_ALPHA = 0.3f;
	private static final String PGN_FORMAT = "PGN %s / %s";
	private static final String VOLUME_BEGIN_SYMBOLS = "|";
	private static final String NO_VOLUME_SYMBOLS = "__";
	private static final String EXISTING_VOLUME_SYMBOLS = "\uD83D\uDE7D";
	private static final String PERCENT_SYMBOL = "%";
	private static final String VOLUME_END_SYMBOLS = "|]  ";
	private final int queuePosition;
	private final int queueSize;
	private final float volume;
	private final String text;

	private Optional<Image> backgroundImage;
	private Optional<Image> stateImage;
	private Optional<Image> repeatStateImage;

	/**
	 * Creates builder for drawing image with basic player stats
	 */
	public BotPlayerGifBuilder(int queuePosition, int queueSize, float volume, String text) {
		this.queuePosition = queuePosition;
		this.queueSize = queueSize;
		this.volume = volume;
		this.text = text;
	}

	/**
	 * Creates {@link InputStream} that can be passed into {@link File}
	 */
	public InputStream buildInputStream() {
		GifImage gifImage = makeAGif();

		GifEncoder encoder = new GifEncoder();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		encoder.start(outputStream);
		encoder.setRepeat(0);
		encoder.setQuality(100);
		for (BufferedImage frame : gifImage.getFrames()) {
			encoder.addFrame(frame);
		}
		encoder.finish();
		byte[] bytes = outputStream.toByteArray();
		return new ByteArrayInputStream(bytes);
	}

	/**
	 * creates GifImage with passed parameters
	 */
	public GifImage makeAGif() {
		int frames = ANIMATION_FRAMES;
		int frameDuration = FRAME_DURATION;
		GifImage gifImage = new GifImage();
		gifImage.setBackground(Color.BLACK);
		gifImage.setDelay(frameDuration);
		for (int frame = 1; frame <= frames; frame++) {
			BotGifFrameBuilder botGifFrameBuilder = new BotGifFrameBuilder(
				IMAGE_WIDTH,
				IMAGE_HEIGHT,
				frames,
				frameDuration
			).setMinAlpha(MIN_ALPHA)
				.setMaxAlpha(DEFAULT_ALPHA);
			gifImage.addFrame(makeAFrame(botGifFrameBuilder, frame));
		}
		return gifImage;
	}

	private BufferedImage makeAFrame(BotGifFrameBuilder botGifFrameBuilder, int frameNumber) {
		backgroundImage.ifPresent(botGifFrameBuilder::addImage);
		stateImage.ifPresent(image ->
			botGifFrameBuilder.addBlinkingImage(image, frameNumber, FADE_IN_OUT_CYCLE, STATE_X, THIRD_ROW_IMAGES_Y)
		);
		repeatStateImage.ifPresent(
			image -> botGifFrameBuilder.addImage(image, REPEAT_STATE_X, THIRD_ROW_IMAGES_Y, DEFAULT_ALPHA)
		);

		botGifFrameBuilder.setFont(new Font(SANS_FONT, Font.BOLD, DEFAULT_FONT_SIZE))
			.addRunningText(text, START_X, SECOND_ROW_TEXT_Y, DEFAULT_ALPHA, frameNumber, 8)
			.addText(String.format(PGN_FORMAT, + queuePosition, queueSize), START_X, FIRST_ROW_TEXT_Y, DEFAULT_ALPHA)
			.addText(getVolumeSign(volume), START_X, THIRD_ROW_TEXT_Y, DEFAULT_ALPHA);
		return botGifFrameBuilder.build();
	}

	private String getVolumeSign(float volume) {
		float symbols = VOLUME_SYMBOLS_COUNT * volume / PERCENT;
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(VOLUME_BEGIN_SYMBOLS);
		IntStream.range(0, (int) symbols).forEach(i -> stringBuilder.append(EXISTING_VOLUME_SYMBOLS));
		IntStream.range((stringBuilder.length() - 1) / VOLUME_SYMBOL_LEN, VOLUME_SYMBOLS_COUNT)
			.forEach(i -> stringBuilder.append(NO_VOLUME_SYMBOLS));
		stringBuilder.append(VOLUME_END_SYMBOLS).append(volume).append(PERCENT_SYMBOL);
		return stringBuilder.toString();
	}

	public BotPlayerGifBuilder setBackgroundImage(Optional<Image> backgroundImage) {
		this.backgroundImage = backgroundImage;
		return this;
	}

	public BotPlayerGifBuilder setStateImage(Optional<Image> stateImage) {
		this.stateImage = stateImage;
		return this;
	}

	public BotPlayerGifBuilder setRepeatStateImage(Optional<Image> repeatStateImage) {
		this.repeatStateImage = repeatStateImage;
		return this;
	}

	public Optional<Image> getBackgroundImage() {
		return backgroundImage;
	}

	public Optional<Image> getStateImage() {
		return stateImage;
	}

	public Optional<Image> getRepeatStateImage() {
		return repeatStateImage;
	}
}
