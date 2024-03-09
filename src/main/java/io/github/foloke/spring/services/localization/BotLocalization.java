package io.github.foloke.spring.services.localization;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bot messages Loclaliztion
 *
 * @author Марченко Дмитрий
 * @since 05.01.2024
 */
@Component
public abstract class BotLocalization {

	private static final Map<String, ResourceBundle> packageTolocaliztionBundleMap = new ConcurrentHashMap<>();
	private static final String DEFAULT_LOCALE = "en";

	@Value("${locale}")
	private String localeName;

	/**
	 * Get message for current locale
	 */
	public String getMessage(String messageName) {
		try {
			String locale = localeName.equals(DEFAULT_LOCALE) ? "" : localeName;
			return packageTolocaliztionBundleMap.computeIfAbsent(localeName, pkgName ->
				 ResourceBundle.getBundle("locale." + getPackageName(), new Locale(locale))
			).getString(messageName);
		} catch (MissingResourceException missingResourceException) {
			return "No message found for: \"" + messageName + "\", for locale \""
				+ localeName + "\"";
		}
	}

	/**
	 * Get message for current locale with placeholder formatting
	 */
	public String getMessage(String messageName, Object... args) {
		return MessageFormat.format(getMessage(messageName), args);
	}

	public abstract String getPackageName();
}
