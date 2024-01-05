package io.github.foloke.utils;

import io.github.cdimascio.dotenv.Dotenv;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Bot messages Loclaliztion
 *
 * @author Марченко Дмитрий
 * @since 05.01.2024
 */
public class BotLocalization {
	private final ResourceBundle playerBundle;
	private final String localeName;

	private BotLocalization() {
		localeName = Dotenv.load().get("LOCALE", "en");
		Locale locale = new Locale(localeName);
		playerBundle = ResourceBundle.getBundle("locale.player", locale);
	}

	/**
	 * Get message for current locale
	 */
	public static String getPlayerMessage(String messageName) {
		 try {
			 return getInstance().playerBundle.getString(messageName);
		} catch (MissingResourceException missingResourceException) {
			 return "No message found for: \"" + messageName + "\", for locale \""
				 + getInstance().localeName + "\"";
		 }
	}

	/**
	 * Get message for current locale with placeholder formatting
	 */
	public static String getPlayerMessage(String messageName, Object... args) {
		return MessageFormat.format(getPlayerMessage(messageName), args);
	}

	private static final class BotLocalizationHolder {
		private static final BotLocalization botLocalization = new BotLocalization();
	}

	public static BotLocalization getInstance() {
		return BotLocalizationHolder.botLocalization;
	}
}
