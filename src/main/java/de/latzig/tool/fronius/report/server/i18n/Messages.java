package de.latzig.tool.fronius.report.server.i18n;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Access to the localized texts.
 *
 * <p>
 * The texts live in <code>messages.properties</code> (english, the fallback)
 * and <code>messages_de.properties</code> (german, the configured default of
 * this application).
 * </p>
 *
 * @author Mario Latzig
 */
@Component
public class Messages {

	// fields
	private final MessageSource messageSource;

	/**
	 * Constructor.
	 *
	 * @param messageSource
	 *            The message source of the application.
	 */
	public Messages(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	/**
	 * @param key
	 *            The key of the text.
	 * @param args
	 *            The arguments of the text.
	 * @return The localized text.
	 */
	public String get(String key, Object... args) {
		return this.messageSource.getMessage(key, args, key, getLocale());
	}

	/**
	 * @return The locale of the current request.
	 */
	public Locale getLocale() {
		return LocaleContextHolder.getLocale();
	}
}
