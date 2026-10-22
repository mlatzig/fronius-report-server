package de.latzig.tool.fronius.report.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.YearMonth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.latzig.tool.fronius.report.server.config.FroniusProperties;
import de.latzig.tool.fronius.report.server.csv.CsvReportReader;
import de.latzig.tool.fronius.report.server.model.ReportNotFoundException;
import de.latzig.tool.fronius.report.server.service.ReportRepository.RefreshResult;

/**
 * Tests for the rescan of {@link ReportRepository}.
 *
 * @author Mario Latzig
 */
class ReportRepositoryTest {

	// consts
	private static final double DELTA = 0.0001d;

	// fields
	@TempDir
	private Path root;
	private ReportRepository repository;

	/**
	 * Creates the repository on the temporary directory.
	 */
	@BeforeEach
	void createRepository() {
		FroniusProperties properties = new FroniusProperties();
		properties.setCsvDir(this.root.toString());
		properties.setDemoFallback(false);
		this.repository = new ReportRepository(properties, new CsvReportReader(properties), new DemoReports(properties));
	}

	/**
	 * A report added after the last rescan is read by the next one.
	 *
	 * @throws IOException
	 *             If the test file could not be written.
	 */
	@Test
	void rescanReadsNewReports() throws IOException {
		assertEquals(0, this.repository.refresh().available());

		writeReport(YearMonth.of(2026, 7), "01.07.2026,10000.0,4000.0,3000.0,7000.0,1000.0");

		RefreshResult result = this.repository.refresh();

		assertEquals(1, result.reloaded());
		assertEquals(1, result.available());
		assertTrue(result.hasChanges());
		assertEquals(10.0d, this.repository.getMonthReport(YearMonth.of(2026, 7)).getSummary().getProduction(), DELTA);
	}

	/**
	 * An unchanged report is not read again.
	 *
	 * @throws IOException
	 *             If the test file could not be written.
	 */
	@Test
	void rescanSkipsUnchangedReports() throws IOException {
		writeReport(YearMonth.of(2026, 7), "01.07.2026,10000.0,4000.0,3000.0,7000.0,1000.0");
		this.repository.refresh();

		RefreshResult result = this.repository.refresh();

		assertEquals(0, result.reloaded());
		assertEquals(1, result.available());
		assertFalse(result.hasChanges());
	}

	/**
	 * A modified report replaces the cached one.
	 *
	 * @throws IOException
	 *             If the test file could not be written.
	 */
	@Test
	void rescanPicksUpModifiedReports() throws IOException {
		YearMonth month = YearMonth.of(2026, 7);
		writeReport(month, "01.07.2026,10000.0,4000.0,3000.0,7000.0,1000.0");
		this.repository.refresh();

		// same month, new export with another value and a newer timestamp
		Path file = writeReport(month, "01.07.2026,20000.0,4000.0,3000.0,17000.0,1000.0");
		Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() + 5000L));

		RefreshResult result = this.repository.refresh();

		assertEquals(1, result.reloaded());
		assertEquals(20.0d, this.repository.getMonthReport(month).getSummary().getProduction(), DELTA);
	}

	/**
	 * A report removed from the directory is dropped from the cache.
	 *
	 * @throws IOException
	 *             If the test file could not be written.
	 */
	@Test
	void rescanDropsRemovedReports() throws IOException {
		YearMonth month = YearMonth.of(2026, 7);
		Path file = writeReport(month, "01.07.2026,10000.0,4000.0,3000.0,7000.0,1000.0");
		this.repository.refresh();

		Files.delete(file);

		RefreshResult result = this.repository.refresh();

		assertEquals(1, result.dropped());
		assertEquals(0, result.available());
	}

	/**
	 * Without the sample fallback an empty directory has no report at all.
	 */
	@Test
	void emptyDirectoryWithoutDemoFallbackHasNoReports() {
		assertFalse(this.repository.isDemo());
		assertThrows(ReportNotFoundException.class, () -> this.repository.getLatestMonth());
	}

	/**
	 * The sample report is shown as long as the directory holds no report, the
	 * first real report replaces it.
	 *
	 * @throws IOException
	 *             If the test file could not be written.
	 */
	@Test
	void sampleReportIsShownUntilTheFirstRealReport() throws IOException {
		FroniusProperties properties = new FroniusProperties();
		properties.setCsvDir(this.root.toString());
		ReportRepository demoRepository = new ReportRepository(properties, new CsvReportReader(properties),
				new DemoReports(properties));

		assertTrue(demoRepository.isDemo());
		assertEquals(DemoReports.MONTH, demoRepository.getLatestMonth());
		assertEquals(31, demoRepository.getMonthReport(DemoReports.MONTH).getDays().size());

		writeReport(YearMonth.of(2026, 7), "01.07.2026,10000.0,4000.0,3000.0,7000.0,1000.0");

		assertFalse(demoRepository.isDemo());
		assertEquals(YearMonth.of(2026, 7), demoRepository.getLatestMonth());
	}

	/**
	 * Without a configured directory the sample report is shown.
	 */
	@Test
	void sampleReportIsShownWithoutConfiguredDirectory() {
		FroniusProperties properties = new FroniusProperties();
		ReportRepository demoRepository = new ReportRepository(properties, new CsvReportReader(properties),
				new DemoReports(properties));

		assertTrue(demoRepository.isDemo());
		assertEquals(DemoReports.MONTH, demoRepository.getLatestMonth());
	}

	/**
	 * The file name of the reports follows the configured pattern.
	 *
	 * @throws IOException
	 *             If the test file could not be written.
	 */
	@Test
	void configuredFilePatternIsUsed() throws IOException {
		FroniusProperties properties = new FroniusProperties();
		properties.setCsvDir(this.root.toString());
		properties.setDemoFallback(false);
		properties.getCsv().setFilePattern("Energy_balance_{month}-{year}.csv");
		ReportRepository englishRepository = new ReportRepository(properties, new CsvReportReader(properties),
				new DemoReports(properties));
		Path dir = Files.createDirectories(this.root.resolve("2026"));
		Files.writeString(dir.resolve("Energy_balance_07-2026.csv"), """
				Datum und Uhrzeit,Gesamt Erzeugung,Gesamt Verbrauch,Eigenverbrauch,Energie ins Netz eingespeist,Energie vom Netz bezogen
				[dd.MM.yyyy],[Wh],[Wh],[Wh],[Wh],[Wh]
				01.07.2026,10000.0,4000.0,3000.0,7000.0,1000.0
				""", StandardCharsets.ISO_8859_1);

		assertEquals(YearMonth.of(2026, 7), englishRepository.getLatestMonth());
	}

	/**
	 * Writes a report file with a single data line.
	 *
	 * @param month
	 *            The month of the report.
	 * @param dataLine
	 *            The data line of the report.
	 * @return The written file.
	 * @throws IOException
	 *             If the file could not be written.
	 */
	private Path writeReport(YearMonth month, String dataLine) throws IOException {
		Path dir = this.root.resolve(String.valueOf(month.getYear()));
		Files.createDirectories(dir);
		Path file = dir.resolve(String.format("Energiebilanz_Monatlicher_Report_%04d_%02d.csv",
				Integer.valueOf(month.getYear()), Integer.valueOf(month.getMonthValue())));
		Files.writeString(file, """
				Datum und Uhrzeit,Gesamt Erzeugung,Gesamt Verbrauch,Eigenverbrauch,Energie ins Netz eingespeist,Energie vom Netz bezogen
				[dd.MM.yyyy],[Wh],[Wh],[Wh],[Wh],[Wh]
				""" + dataLine + "\n", StandardCharsets.ISO_8859_1);
		return file;
	}
}
