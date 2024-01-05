package io.github.foloke.utils;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Gif frame Renderer
 *
 * @author Марченко Дмитрий
 * @since 05.01.2024
 */
public final class BotGifFrameBuilder {
	public static final int MAX_RUNNING_TEXT_SIZE = 265;
	public static final double HALF = 0.5;
	private final BufferedImage frameImage;
	private final Graphics2D ctx;
	private final int framesCount;
	private final int frameDuration;
	private float minAlpha;
	private float maxAlpha = 1;
	private int maxRunningTextSize = MAX_RUNNING_TEXT_SIZE;

	/**
	 * @param frameWidth image width
	 * @param frameHeight image height
	 * @param framesCount frmaes count
	 * @param frameDuration one frame duration
	 */
	public BotGifFrameBuilder(int frameWidth, int frameHeight, int framesCount, int frameDuration) {
		frameImage = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_ARGB);
		ctx = (Graphics2D) frameImage.getGraphics().create();
		ctx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		this.frameDuration = frameDuration;
		this.framesCount = framesCount;
	}

	/**
	 * Get an image and dispose drawing context.
	 */
	public BufferedImage build() {
		ctx.dispose();
		return frameImage;
	}

	/**
	 * Draw image at 0-x 0-y
	 * @param image image to draw
	 * @return this inatance for chain usage
	 */
	public BotGifFrameBuilder addImage(Image image) {
		ctx.drawImage(image, 0, 0, null);
		return this;
	}
	/**
	 *
	 * Draw image at specified position with transparancy
	 * @param image image to draw
	 * @param x x-position of the image
	 * @param y y-position of the image
	 * @param alpha transparency
	 */
	public BotGifFrameBuilder addImage(Image image, int x, int y, float alpha) {
		AlphaComposite alphaComposite = getAlphaComposite(alpha);
		ctx.setComposite(alphaComposite);
		ctx.drawImage(image, x, y, null);
		return this;
	}

	/**
	 * Draw blinking image frame at specified position
	 * @param image image to draw
	 * @param frame current fram related to {@link #getFramesCount()} and {@link #getFrameDuration()}
	 * @param cycle blink cylce in seconds
	 * @param x x-position of the image
	 * @param y y-position of the image
	 */
	public BotGifFrameBuilder addBlinkingImage(Image image, int frame, int cycle, int x, int y) {
		float currentTimestamp = frame * frameDuration;
		float currentCycle = currentTimestamp / cycle;
		float currentCyclePosition = currentCycle % 1;
		float alpha = currentCyclePosition > HALF ? 2 - currentCyclePosition * 2 : currentCyclePosition * 2;
		alpha = Math.max(0, Math.min(1, (alpha + minAlpha) / (maxAlpha + minAlpha)));
		AlphaComposite alphaComposite = getAlphaComposite(alpha);
		ctx.setComposite(alphaComposite);
		ctx.drawImage(image, x, y, null);
		return this;
	}

	/**
	 * Draw "running" text at specified location with transparancy. running text emulated with space symbols
	 * @param text text to draw
	 * @param x x-position of the text
	 * @param y y-position of the text
	 * @param alpha text transparency
	 * @param frame number of the frame
	 * @param charactersPerSecond text run speed
	 * @return this inatance for chain usage
	 */
	public BotGifFrameBuilder addRunningText(
		String text,
		int x,
		int y,
		float alpha,
		int frame,
		float charactersPerSecond
	) {
		int animationLength = frameDuration * framesCount;
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
		addText(shrinkText(wholeText, fontMetrics), x, y, alpha);
		return this;
	}

	private void expandText(StringBuilder str, FontMetrics fontMetrics) {
		int strLen;
		do {
			strLen = fontMetrics.stringWidth(str.toString());
			str.append(" ");
		} while (strLen < maxRunningTextSize);
	}

	private String shrinkText(String string, FontMetrics fontMetrics) {
		String tmpString = string;
		int len;
		do {
			len = fontMetrics.stringWidth(tmpString);
			tmpString = tmpString.substring(0, tmpString.length() - 1);
		} while (len > maxRunningTextSize && tmpString.length() > 2);
		return tmpString;
	}

	/**
	 * Draw a text with speciified alpha transparancy
	 * @param text text to draw
	 * @param x x-position of the text
	 * @param y y-position of the text
	 * @param alpha trensparancy of the text
	 * @return this inatance for chain usage
	 */
	public BotGifFrameBuilder addText(String text, int x, int y, float alpha) {
		AlphaComposite alphaComposite = getAlphaComposite(alpha);
		ctx.setComposite(alphaComposite);
		ctx.setColor(Color.BLACK);
		ctx.drawString(text, x, y);
		return this;
	}

	private AlphaComposite getAlphaComposite(float alpha) {
		return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
	}

	public void setMaxRunningTextSize(int maxRunningTextSize) {
		this.maxRunningTextSize = maxRunningTextSize;
	}

	public BotGifFrameBuilder setFont(Font font) {
		ctx.setFont(font);
		return this;
	}

	public BotGifFrameBuilder setMinAlpha(float minAlpha) {
		this.minAlpha = minAlpha;
		return this;
	}

	public BotGifFrameBuilder setMaxAlpha(float maxAlpha) {
		this.maxAlpha = maxAlpha;
		return this;
	}

	public int getFramesCount() {
		return framesCount;
	}

	public int getFrameDuration() {
		return frameDuration;
	}

	public float getMinAlpha() {
		return minAlpha;
	}

	public float getMaxAlpha() {
		return maxAlpha;
	}

	public int getMaxRunningTextSize() {
		return maxRunningTextSize;
	}
}
