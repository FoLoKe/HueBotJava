package io.github.foloke.spring.youtube;

/**
 * Thrown when API key is not specified
 *
 * @author Марченко Дмитрий
 * @since 09.03.2024
 */
public class NoApiKeyException extends RuntimeException {
	/**
	 * Create exception with message
	 */
	public NoApiKeyException(String message) {
		super(message);
	}
}
