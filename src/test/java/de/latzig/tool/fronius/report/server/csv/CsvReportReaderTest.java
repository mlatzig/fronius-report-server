package de.latzig.tool.fronius.report.server.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.latzig.tool.fronius.report.server.config.FroniusProperties;
import de.latzig.tool.fronius.report.server.csv.CsvReportReader.EnergyBalanceRow;

/**
 * Tests for {@link CsvReportReader}.
 *
 * @author Mario Latzig
 */
class CsvReportReaderTest {

	// consts
	private static final double DELTA = 0.0001d;

	// fields
	private final CsvReportReader reader = new CsvReportReader(new FroniusProperties());

	/**
	 * The energy balance is converted from Wh to kWh.
	 *
	 * @param tempDir
	 *            The temporary directory.
	 * @throws IOException
	 *             If the test file could not be written.
	 */
	@Test
	void readEnergyBalanceConvertsToKilowattHours(@TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("Energiebilanz_Monatlicher_Report_2026_06.csv");
		Files.writeString(file, """
				Datum und Uhrzeit,Gesamt Erzeugung,Gesamt Verbrauch,Eigenverbrauch,Energie ins Netz eingespeist,Energie vom Netz bezogen
				[dd.MM.yyyy],[Wh],[Wh],[Wh],[Wh],[Wh]
				01.06.2026,44841.5759,8773.4752,8723.7584,35721.0000,50.0000
				02.06.2026,37770.8166,7594.04,7554.3382,29772.0000,40.0000
				""", StandardCharsets.ISO_8859_1);

		Map<LocalDate, EnergyBalanceRow> rows = this.reader.readEnergyBalance(file);

		assertEquals(2, rows.size());
		EnergyBalanceRow row = rows.get(LocalDate.of(2026, 6, 1));
		assertNotNull(row);
		assertEquals(44.8415759d, row.production(), DELTA);
		assertEquals(8.7734752d, row.consumption(), DELTA);
		assertEquals(8.7237584d, row.selfConsumption(), DELTA);
		assertEquals(35.721d, row.feedIn(), DELTA);
		assertEquals(0.05d, row.gridSupply(), DELTA);
	}

	/**
	 * The trailing separator of the older exports must not break the parsing.
	 *
	 * @param tempDir
	 *            The temporary directory.
	 * @throws IOException
	 *             If the test file could not be written.
	 */
	@Test
	void readEnergyBalanceHandlesTrailingSeparator(@TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("Energiebilanz_Monatlicher_Report_2021_03.csv");
		Files.writeString(file, """
				Datum und Uhrzeit,Gesamt Erzeugung,Gesamt Verbrauch,Eigenverbrauch,Energie ins Netz eingespeist,Energie vom Netz bezogen,
				[dd.MM.yyyy],[Wh],[Wh],[Wh],[Wh],[Wh],
				01.03.2021,24181.81,9850.09,8638.40,15125.0,1214.0,
				""", StandardCharsets.ISO_8859_1);

		Map<LocalDate, EnergyBalanceRow> rows = this.reader.readEnergyBalance(file);

		assertEquals(1, rows.size());
		assertEquals(24.18181d, rows.get(LocalDate.of(2021, 3, 1)).production(), DELTA);
	}

	/**
	 * Semicolon as delimiter, comma as decimal separator, quoted values and
	 * values already in kWh are detected.
	 *
	 * @param tempDir
	 *            The temporary directory.
	 * @throws IOException
	 *             If the test file could not be written.
	 */
	@Test
	void readEnergyBalanceDetectsSemicolonDecimalCommaAndKilowattHours(@TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("report.csv");
		Files.writeString(file, """
				"Datum und Uhrzeit";"Gesamt Erzeugung";"Gesamt Verbrauch";"Eigenverbrauch";"Energie ins Netz eingespeist";"Energie vom Netz bezogen"
				[dd.MM.yyyy];[kWh];[kWh];[kWh];[kWh];[kWh]
				"01.06.2026";"1.044,5";"8,75";"8,5";"1.036,0";"0,25"
				""", StandardCharsets.ISO_8859_1);

		EnergyBalanceRow row = this.reader.readEnergyBalance(file).get(LocalDate.of(2026, 6, 1));

		assertNotNull(row);
		assertEquals(1044.5d, row.production(), DELTA);
		assertEquals(8.75d, row.consumption(), DELTA);
		assertEquals(8.5d, row.selfConsumption(), DELTA);
		assertEquals(1036.0d, row.feedIn(), DELTA);
		assertEquals(0.25d, row.gridSupply(), DELTA);
	}

	/**
	 * Configured column names and date format are used for an export in another
	 * language.
	 *
	 * @param tempDir
	 *            The temporary directory.
	 * @throws IOException
	 *             If the test file could not be written.
	 */
	@Test
	void readEnergyBalanceUsesConfiguredFormat(@TempDir Path tempDir) throws IOException {
		FroniusProperties properties = new FroniusProperties();
		properties.getCsv().setDateFormat("MM/dd/yyyy");
		properties.getCsv().getColumns().setProduction("Production");
		properties.getCsv().getColumns().setConsumption("Consumption");
		properties.getCsv().getColumns().setSelfConsumption("Self consumption");
		properties.getCsv().getColumns().setFeedIn("Feed in");
		properties.getCsv().getColumns().setGridSupply("Grid supply");
		Path file = tempDir.resolve("report.csv");
		// columns in another order than usual
		Files.writeString(file, """
				Date,Grid supply,Feed in,Self consumption,Consumption,Production
				[MM/dd/yyyy],[Wh],[Wh],[Wh],[Wh],[Wh]
				06/01/2026,1000,7000,3000,4000,10000
				""", StandardCharsets.ISO_8859_1);

		EnergyBalanceRow row = new CsvReportReader(properties).readEnergyBalance(file).get(LocalDate.of(2026, 6, 1));

		assertNotNull(row);
		assertEquals(10.0d, row.production(), DELTA);
		assertEquals(4.0d, row.consumption(), DELTA);
		assertEquals(3.0d, row.selfConsumption(), DELTA);
		assertEquals(7.0d, row.feedIn(), DELTA);
		assertEquals(1.0d, row.gridSupply(), DELTA);
	}

	/**
	 * Unknown column names fall back to the usual column positions, ISO dates
	 * are always accepted.
	 *
	 * @param tempDir
	 *            The temporary directory.
	 * @throws IOException
	 *             If the test file could not be written.
	 */
	@Test
	void readEnergyBalanceFallsBackToColumnPositions(@TempDir Path tempDir) throws IOException {
		Path file = tempDir.resolve("report.csv");
		Files.writeString(file, """
				Date and time,Total production,Total consumption,Self consumption,Energy to grid,Energy from grid
				[yyyy-MM-dd],[Wh],[Wh],[Wh],[Wh],[Wh]
				2026-06-01,10000,4000,3000,7000,1000
				""", StandardCharsets.ISO_8859_1);

		EnergyBalanceRow row = this.reader.readEnergyBalance(file).get(LocalDate.of(2026, 6, 1));

		assertNotNull(row);
		assertEquals(10.0d, row.production(), DELTA);
		assertEquals(4.0d, row.consumption(), DELTA);
		assertEquals(3.0d, row.selfConsumption(), DELTA);
		assertEquals(7.0d, row.feedIn(), DELTA);
		assertEquals(1.0d, row.gridSupply(), DELTA);
	}

}
