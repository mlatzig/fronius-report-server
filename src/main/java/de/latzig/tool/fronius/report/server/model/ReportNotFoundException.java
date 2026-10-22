package de.latzig.tool.fronius.report.server.model;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown if a requested report does not exist.
 *
 * @author Mario Latzig
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ReportNotFoundException extends RuntimeException {

	// consts
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 *
	 * @param message
	 *            The message of the exception.
	 */
	public ReportNotFoundException(String message) {
		super(message);
	}
}
