package io.github.foloke;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bot spring application
 *
 * @author Марченко Дмитрий
 * @since 06.01.2024
 */
@SpringBootApplication
public class BotApplication {
	/**
	 * Entry point, no args (use .env file)
	 */
	public static void main(String[] args) {
		Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
		SpringApplication.run(BotApplication.class, args);
	}
}
