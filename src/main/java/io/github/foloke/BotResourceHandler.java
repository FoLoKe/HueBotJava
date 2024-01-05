package io.github.foloke;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource handler to load images for gif renderer
 *
 * @author Dmitry Marchenko
 * @since 05.02.2023
 */
public class BotResourceHandler {
	private static final Logger logger = Logger.getLogger(BotResourceHandler.class.getName());
	private static final String IMAGE_GET_ERROR_MESSAGE = "Image get error";
	private static final String FILE_NOT_FOUND_ERROR_MESSAGE_FORMAT = "File not found: %s";
	private final Map<String, Image> imageMap;

	private BotResourceHandler() {
		imageMap = new HashMap<>();
	}

	private Image getImage(String resourcePath) throws IOException {
		Image image = imageMap.get(resourcePath);
		if (image == null) {
			image = ImageIO.read(getFileFromResourceAsStream(resourcePath));
			imageMap.put(resourcePath, image);
		}
		return image;
	}

	/**
	 * Creates input stream by specified file name
	 */
	public InputStream getFileFromResourceAsStream(String fileName) {

		// The class loader that loaded the class
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(fileName);

		// the stream holding the file content
		if (inputStream == null) {
			throw new IllegalArgumentException(String.format(FILE_NOT_FOUND_ERROR_MESSAGE_FORMAT, fileName));
		} else {
			return inputStream;
		}
	}

	/**
	 * Loads image from resources
	 */
	public static Optional<Image> getImageByPath(String resourcePath) {
		if (resourcePath != null && !resourcePath.isEmpty()) {
			try {
				Image image = getInstance().getImage(resourcePath);
				return Optional.of(image);
			} catch (IOException e) {
				logger.log(Level.SEVERE, IMAGE_GET_ERROR_MESSAGE, e);
			}
		}
		return Optional.empty();
	}

	private static BotResourceHandler getInstance() {
		return InstanceHolder.instance;
	}

	private static final class InstanceHolder {
		static final BotResourceHandler instance = new BotResourceHandler();
	}
}
