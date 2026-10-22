package de.latzig.tool.fronius.report.server.web;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Writes an object as JSON into the page.
 *
 * <p>
 * Used by the template to hand the tool tip model of the chart to the thin
 * client without an additional request.
 * </p>
 *
 * @author Mario Latzig
 */
@Component("json")
public class JsonWriter {

	// fields
	private final ObjectMapper objectMapper;

	/**
	 * Constructor.
	 *
	 * @param objectMapper
	 *            The object mapper of the application.
	 */
	public JsonWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * @param value
	 *            The object to write.
	 * @return The object as JSON, safe to be embedded into a script element.
	 */
	public String write(Object value) {
		try {
			// "</script>" inside a string would end the element too early
			return this.objectMapper.writeValueAsString(value).replace("</", "<\\/"); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Object could not be written as JSON.", e); //$NON-NLS-1$
		}
	}
}
