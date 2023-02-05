package org.example;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resource handler to load images for gif renderer
 *
 * @author Dmitry Marchenko
 * @since 05.02.2023
 */
public class BotResourceHandler {
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

	public InputStream getFileFromResourceAsStream(String fileName) {

		// The class loader that loaded the class
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream inputStream = classLoader.getResourceAsStream(fileName);

		// the stream holding the file content
		if (inputStream == null) {
			throw new IllegalArgumentException("file not found! " + fileName);
		} else {
			return inputStream;
		}
	}

	private static final class InstanceHolder {
		static final BotResourceHandler instance = new BotResourceHandler();
	}

	private static BotResourceHandler getInstance() {
		return InstanceHolder.instance;
	}

	public static Optional<Image> getImageByPath(String resourcePath) {
		if (resourcePath != null && !resourcePath.isEmpty()) {
			try {
				Image image = getInstance().getImage(resourcePath);
				return Optional.of(image);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return Optional.empty();
	}
}
