package de.latzig.tool.fronius.report.server.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;

import org.springframework.stereotype.Component;

import de.latzig.tool.fronius.report.server.config.FroniusProperties;

/**
 * The bundled sample report, shown as long as the CSV directory holds no report.
 *
 * <p>
 * The sample is a single summer month with made up values. It lives inside the
 * jar and is copied to a temporary directory on first use, laid out like a real
 * CSV directory and named after the configured file pattern.
 * </p>
 *
 * @author Mario Latzig
 */
@Component
public class DemoReports {

	// consts
	/** The month of the sample report. */
	public static final YearMonth MONTH = YearMonth.of(2025, 7);
	private static final String RESOURCE = "/demo/Energiebilanz_Monatlicher_Report_2025_07.csv"; //$NON-NLS-1$

	// fields
	private final FroniusProperties properties;
	private Path root;

	/**
	 * Constructor.
	 *
	 * @param properties
	 *            The configuration.
	 */
	public DemoReports(FroniusProperties properties) {
		this.properties = properties;
	}

	/**
	 * @return The root directory holding the sample report.
	 */
	public synchronized Path getRoot() {
		if (this.root == null) {
			this.root = extract();
		}
		return this.root;
	}

	/**
	 * Copies the sample report from the jar to a temporary directory.
	 *
	 * @return The temporary root directory.
	 */
	private Path extract() {
		try (InputStream in = DemoReports.class.getResourceAsStream(RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException("Sample report missing: " + RESOURCE); //$NON-NLS-1$
			}
			Path dir = Files.createTempDirectory("fronius-demo"); //$NON-NLS-1$
			Path yearDir = Files.createDirectory(dir.resolve(String.valueOf(MONTH.getYear())));
			Path file = yearDir.resolve(this.properties.getCsv().fileName(MONTH));
			Files.copy(in, file);
			// deleted in reverse order of registration: file, year, root
			dir.toFile().deleteOnExit();
			yearDir.toFile().deleteOnExit();
			file.toFile().deleteOnExit();
			return dir;
		} catch (IOException e) {
			throw new UncheckedIOException("Sample report could not be extracted", e); //$NON-NLS-1$
		}
	}
}
