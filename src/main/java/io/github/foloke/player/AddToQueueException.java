package io.github.foloke.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Somthing went wrong while processing tarcks load
 *
 * @author Марченко Дмитрий
 * @since 08.03.2024
 */
public class AddToQueueException extends RuntimeException{
	private final List<String> errors;

	/**
	 * Create exception with errors messages
	 */
	public AddToQueueException(List<String> errors) {
		this.errors = new ArrayList<>(errors);
	}

	/**
	 * Create exception with one error message
	 */
	public AddToQueueException(String error) {
		errors = Collections.singletonList(error);
	}

	public List<String> getErrors() {
		return new ArrayList<>(errors);
	}
}
