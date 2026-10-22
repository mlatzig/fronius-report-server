package de.latzig.tool.fronius.report.server.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Tests for {@link Messages} and the message files.
 *
 * @author Mario Latzig
 */
class MessagesTest {

	// fields
	private final Messages messages = new Messages(createMessageSource());

	/**
	 * Resets the locale changed by the tests.
	 */
	@AfterEach
	void resetLocale() {
		LocaleContextHolder.resetLocaleContext();
	}

	/**
	 * The german texts are used for a german locale.
	 */
	@Test
	void germanTextsAreUsedForGermanLocale() {
		LocaleContextHolder.setLocale(Locale.GERMAN);

		assertEquals("Monatssummen des Jahres", this.messages.get("view.year.hint"));
		assertEquals("PV-Erzeugung", this.messages.get("metric.production"));
	}

	/**
	 * The message files are encoded in ISO-8859-1, umlauts must survive that.
	 */
	@Test
	void umlautsAreDecodedCorrectly() {
		LocaleContextHolder.setLocale(Locale.GERMAN);

		assertEquals("Gesamtübersicht", this.messages.get("view.overview.title"));
	}

	/**
	 * A year must not be grouped into "2.025" by the message format.
	 */
	@Test
	void yearIsNotGrouped() {
		LocaleContextHolder.setLocale(Locale.GERMAN);

		assertEquals("Jahr 2025", this.messages.get("view.year.title", String.valueOf(2025)));
	}

	/**
	 * The english texts are the fallback.
	 */
	@Test
	void englishTextsAreTheFallback() {
		LocaleContextHolder.setLocale(Locale.ENGLISH);

		assertEquals("Year 2025", this.messages.get("view.year.title", String.valueOf(2025)));
		assertEquals("PV production", this.messages.get("metric.production"));
	}

	/**
	 * Every text has a translation in every language.
	 *
	 * @throws IOException
	 *             If a message file could not be read.
	 */
	@Test
	void allLanguagesHaveTheSameKeys() throws IOException {
		assertEquals(loadKeys("/messages.properties"), loadKeys("/messages_de.properties"));
	}

	/**
	 * @param resource
	 *            The message file.
	 * @return The keys of the message file.
	 * @throws IOException
	 *             If the file could not be read.
	 */
	private static Set<String> loadKeys(String resource) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = MessagesTest.class.getResourceAsStream(resource)) {
			properties.load(in);
		}
		return new TreeSet<String>(properties.stringPropertyNames());
	}

	/**
	 * @return The message source configured like the one of the application.
	 */
	private static ResourceBundleMessageSource createMessageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasename("messages");
		messageSource.setDefaultEncoding("ISO-8859-1");
		// without this an unknown locale would fall back to the system locale
		// instead of the english base file
		messageSource.setFallbackToSystemLocale(false);
		return messageSource;
	}
}
