package org.example;

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
public class BotGifRenderer {

	public static final int ANIMATION_FRAMES = 200;
	public static final int FRAME_DURATION = 50;
	public static final int FADE_IN_OUT_CYCLE = 2000;
	public static final float DEFAULT_ALPHA = 0.8f;
	public static final float PERCENT = 100f;
	public static final int VOLUME_SYMBOLS = 5;
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
	public static final float MIN_ALPHA = 0.3f;
	public static final double HALF = 0.5;
	public static final int MAX_RUNNING_TEXT_SIZE = 260;

	/**
	 * Creates {@link InputStream} that can be passed into {@link File}
	 */
	public InputStream makeAGifAsInputStream(
		int queuePosition,
		int queueSize,
		int volume,
		String text,
		String backgroundImagePath,
		Optional<Image> stateImage,
		Optional<Image> repeatStateImage
	) {
		GifImage gifImage = makeAGif(
			queuePosition,
			queueSize,
			volume,
			text,
			backgroundImagePath,
			stateImage,
			repeatStateImage
		);

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
	public GifImage makeAGif(
		int queuePosition,
		int queueSize,
		int volume,
		String text,
		String backgroundImagePath,
		Optional<Image> stateImage,
		Optional<Image> repeatStateImage
	) {
		int frames = ANIMATION_FRAMES;
		int frameDuration = FRAME_DURATION;
		GifImage gifImage = new GifImage();
		gifImage.setBackground(Color.BLACK);
		gifImage.setDelay(frameDuration);

		Optional<Image> backgroundImage = BotResourceHandler.getImageByPath(backgroundImagePath);
		for (int frame = 1; frame < frames; frame++) {
			gifImage.addFrame(
				makeAFrame(
					queuePosition,
					queueSize,
					volume,
					text,
					backgroundImage,
					stateImage,
					repeatStateImage,
					frame,
					frames,
					frameDuration
				)
			);
		}
		return gifImage;
	}

	private BufferedImage makeAFrame(
		int queuePosition,
		int queSize,
		int volume,
		String text,
		Optional<Image> backgroundImage,
		Optional<Image> stateImage,
		Optional<Image> repeatStateImage,
		int frame,
		int frames,
		float frameDuration
	) {
		BufferedImage frameImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D ctx = (Graphics2D) frameImage.getGraphics().create();
		ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		backgroundImage.ifPresent(image -> ctx.drawImage(image, 0, 0, null));
		stateImage.ifPresent(image ->
			addBlinkingImage(ctx, image, frame, frameDuration, FADE_IN_OUT_CYCLE, STATE_X, THIRD_ROW_IMAGES_Y)
		);
		repeatStateImage.ifPresent(image -> addImage(ctx, image, REPEAT_STATE_X, THIRD_ROW_IMAGES_Y, DEFAULT_ALPHA));

		Font font = new Font("Sans", Font.BOLD, DEFAULT_FONT_SIZE);
		ctx.setFont(font);
		addRunningText(ctx, text, START_X, SECOND_ROW_TEXT_Y, DEFAULT_ALPHA, frames, frame, frameDuration, 8);
		addText(ctx, "PGN " + queuePosition + " / " + queSize, START_X, FIRST_ROW_TEXT_Y, DEFAULT_ALPHA);
		addText(ctx, getVolumeSign(volume), START_X, THIRD_ROW_TEXT_Y, DEFAULT_ALPHA);
		ctx.dispose();
		return frameImage;
	}

	private String getVolumeSign(int previousVolume) {
		float symbols = VOLUME_SYMBOLS * previousVolume / PERCENT;
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("|");
		IntStream.range(0, (int) symbols).forEach(i -> stringBuilder.append("\uD83D\uDE7D"));
		IntStream.range((stringBuilder.length() - 1) / VOLUME_SYMBOL_LEN, VOLUME_SYMBOLS)
			.forEach(i -> stringBuilder.append("__"));
		stringBuilder.append("|]  ").append(previousVolume).append("%");
		return stringBuilder.toString();
	}

	private void addBlinkingImage(
		Graphics2D ctx,
		Image image,
		int frame,
		float frameDuration,
		int cycle,
		int x,
		int y
	) {
		float currentTimestamp = frame * frameDuration;
		float currentCycle = currentTimestamp / cycle;
		float currentCyclePosition = currentCycle % 1;
		float alpha = currentCyclePosition > HALF ? VOLUME_SYMBOL_LEN - currentCyclePosition * VOLUME_SYMBOL_LEN : currentCyclePosition * VOLUME_SYMBOL_LEN;
		alpha = Math.max(0, Math.min(1, (alpha + MIN_ALPHA) / (DEFAULT_ALPHA + MIN_ALPHA)));
		AlphaComposite alphaComposite = getAlphaComposite(alpha);
		ctx.setComposite(alphaComposite);
		ctx.drawImage(image, x, y, null);
	}

	private void addImage(Graphics2D ctx, Image image, int x, int y, float alpha) {
		AlphaComposite alphaComposite = getAlphaComposite(alpha);
		ctx.setComposite(alphaComposite);
		ctx.drawImage(image, x, y, null);
	}

	private void addRunningText(
		Graphics2D ctx,
		String text,
		int x,
		int y,
		float alpha,
		int frames,
		int frame,
		float frameDuration,
		float charactersPerSecond
	) {
		float animationLength = frameDuration * frames;
		String spacer = "     ";
		float timePerChar = 1000 / charactersPerSecond;
		int maxLineLen = (int) (animationLength / timePerChar - spacer.length());
		StringBuilder stringBuilder = new StringBuilder(text.substring(0, Math.max(0, Math.min(maxLineLen, text.length()))));
		stringBuilder.append(spacer);
		FontMetrics fontMetrics = ctx.getFontMetrics();
		expandText(stringBuilder, fontMetrics);

		int currentPosition = (int) Math.min(stringBuilder.length(), frame * frameDuration / timePerChar);
		String frontText = stringBuilder.substring(currentPosition, stringBuilder.length());
		String wholeText = frontText + stringBuilder.substring(0, currentPosition);
		shrinkText(wholeText, fontMetrics);
		addText(ctx, wholeText, x, y, alpha);
	}

	private void expandText(StringBuilder str, FontMetrics fontMetrics) {
		int strLen;
		do {
			strLen = fontMetrics.stringWidth(str.toString());
			str.append(" ");
		} while (strLen < MAX_RUNNING_TEXT_SIZE);
	}

	private void shrinkText(String string, FontMetrics fontMetrics) {
		int len;
		do {
			len = fontMetrics.stringWidth(string);
			string = string.substring(0, string.length() - 1);
		} while (len > MAX_RUNNING_TEXT_SIZE && string.length() > VOLUME_SYMBOL_LEN);
	}

	private void addText(Graphics2D ctx, String text, int x, int y, float alpha) {
		AlphaComposite alphaComposite = getAlphaComposite(alpha);
		ctx.setComposite(alphaComposite);
		ctx.setColor(Color.BLACK);
		ctx.drawString(text, x, y);
	}

	private AlphaComposite getAlphaComposite(float alpha) {
		return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
	}
}
