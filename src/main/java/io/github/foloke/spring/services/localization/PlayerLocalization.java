package io.github.foloke.spring.services.localization;

import org.springframework.stereotype.Component;

/**
 * Player messages localization
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@Component("playerLocalization")
public class PlayerLocalization extends BotLocalization {
	@Override
	public String getPackageName() {
		return "player";
	}
}
