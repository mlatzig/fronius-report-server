package de.latzig.tool.fronius.report.server.csv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.latzig.tool.fronius.report.server.config.FroniusProperties;
import de.latzig.tool.fronius.report.server.config.FroniusProperties.Columns;

/**
 * Reads the CSV reports exported by Solar.web.
 *
 * <p>
 * Read is the monthly energy balance report (german:
 * <code>Energiebilanz_Monatlicher_Report_YYYY_MM.csv</code>), holding
 * production, consumption, self consumption, feed in and grid supply per day.
 * </p>
 *
 * <p>
 * The files carry two header lines: the column names and the units. Solar.web
 * localizes the column names, so they are configurable. A column is identified
 * by its name, so a changed column order does not break the parsing; a name
 * that is not found falls back to the usual position of the column. Delimiter
 * (<code>,</code> or <code>;</code>), decimal separator and the unit
 * (<code>[Wh]</code> or <code>[kWh]</code>) are detected.
 * </p>
 *
 * @author Mario Latzig
 */
@Component
public class CsvReportReader {

	// consts
	private static final Logger LOG = LoggerFactory.getLogger(CsvReportReader.class);
	private static final DateTimeFormatter SDF_ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final String UNIT_KWH = "[kwh]"; //$NON-NLS-1$

	// usual positions of the value columns, the date is the first column
	private static final int POS_PRODUCTION = 1;
	private static final int POS_CONSUMPTION = 2;
	private static final int POS_SELF_CONSUMPTION = 3;
	private static final int POS_FEED_IN = 4;
	private static final int POS_GRID_SUPPLY = 5;

	// fields
	private final FroniusProperties.Csv format;
	private final DateTimeFormatter dateFormat;

	/**
	 * Constructor.
	 *
	 * @param properties
	 *            The configuration.
	 */
	public CsvReportReader(FroniusProperties properties) {
		this.format = properties.getCsv();
		this.dateFormat = DateTimeFormatter.ofPattern(this.format.getDateFormat());
	}

	/**
	 * Reads the energy balance report and returns the values per date in kWh.
	 *
	 * @param file
	 *            The CSV file to read.
	 * @return The values per date, in file order.
	 * @throws IOException
	 *             If the file could not be read.
	 */
	public Map<LocalDate, EnergyBalanceRow> readEnergyBalance(Path file) throws IOException {
		List<String> lines = readLines(file);
		Map<LocalDate, EnergyBalanceRow> result = new LinkedHashMap<LocalDate, EnergyBalanceRow>();
		if (lines.size() < 3) {
			return result;
		}
		String separator = detectSeparator(lines.get(0));
		String[] header = split(lines.get(0), separator);
		String[] units = split(lines.get(1), separator);
		Columns columns = this.format.getColumns();
		int idxProduction = indexOfColumn(file, header, columns.getProduction(), POS_PRODUCTION);
		int idxConsumption = indexOfColumn(file, header, columns.getConsumption(), POS_CONSUMPTION);
		int idxSelfConsumption = indexOfColumn(file, header, columns.getSelfConsumption(), POS_SELF_CONSUMPTION);
		int idxFeedIn = indexOfColumn(file, header, columns.getFeedIn(), POS_FEED_IN);
		int idxGridSupply = indexOfColumn(file, header, columns.getGridSupply(), POS_GRID_SUPPLY);

		for (int i = 2; i < lines.size(); i++) {
			String[] values = split(lines.get(i), separator);
			LocalDate date = parseDate(values);
			if (date == null) {
				continue;
			}
			// we work with kWh
			EnergyBalanceRow row = new EnergyBalanceRow(date, //
					parseKilowattHours(values, units, idxProduction), //
					parseKilowattHours(values, units, idxConsumption), //
					parseKilowattHours(values, units, idxSelfConsumption), //
					parseKilowattHours(values, units, idxFeedIn), //
					parseKilowattHours(values, units, idxGridSupply));
			result.put(date, row);
		}
		return result;
	}

	/**
	 * Reads all non empty lines of the given file. The exports are ISO-8859-1
	 * encoded.
	 *
	 * @param file
	 *            The file to read.
	 * @return The non empty lines.
	 * @throws IOException
	 *             If the file could not be read.
	 */
	private List<String> readLines(Path file) throws IOException {
		List<String> result = new ArrayList<String>();
		for (String line : Files.readAllLines(file, StandardCharsets.ISO_8859_1)) {
			if (line.trim().length() > 0) {
				result.add(line);
			}
		}
		return result;
	}

	/**
	 * @param headerLine
	 *            The first line of the file.
	 * @return The delimiter of the file, <code>;</code> or <code>,</code>.
	 */
	private String detectSeparator(String headerLine) {
		return headerLine.indexOf(';') >= 0 ? ";" : ","; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @param line
	 *            The line to split.
	 * @param separator
	 *            The delimiter of the file.
	 * @return The trimmed values of the line, without surrounding quotes.
	 */
	private String[] split(String line, String separator) {
		String[] values = line.split(separator, -1);
		for (int i = 0; i < values.length; i++) {
			String value = values[i].trim();
			if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) { //$NON-NLS-1$ //$NON-NLS-2$
				value = value.substring(1, value.length() - 1).trim();
			}
			values[i] = value;
		}
		return values;
	}

	/**
	 * @param file
	 *            The file being read, for the log.
	 * @param header
	 *            The header values.
	 * @param name
	 *            The configured name of the wanted column.
	 * @param position
	 *            The usual position of the column, the fallback.
	 * @return The index of the column.
	 */
	private int indexOfColumn(Path file, String[] header, String name, int position) {
		for (int i = 0; i < header.length; i++) {
			if (name.equalsIgnoreCase(header[i])) {
				return i;
			}
		}
		LOG.warn("Column \"{}\" not found in {}, using column {} (\"{}\").", name, file.getFileName(), //$NON-NLS-1$
				Integer.valueOf(position + 1), position < header.length ? header[position] : ""); //$NON-NLS-1$
		return position;
	}

	/**
	 * @param values
	 *            The values of a data line.
	 * @return The date of the line or <code>null</code> if it is no data line.
	 */
	private LocalDate parseDate(String[] values) {
		if (values.length == 0 || values[0].length() == 0) {
			return null;
		}
		// a timestamp may follow the date
		String value = values[0].split(" ", 2)[0]; //$NON-NLS-1$
		try {
			return LocalDate.parse(value, this.dateFormat);
		} catch (RuntimeException e) {
			try {
				return LocalDate.parse(value, SDF_ISO_DATE);
			} catch (RuntimeException e2) {
				// no data line (e.g. a trailing summary line)
				return null;
			}
		}
	}

	/**
	 * @param values
	 *            The values of a data line.
	 * @param units
	 *            The values of the unit line.
	 * @param index
	 *            The index of the wanted value.
	 * @return The value in kWh, 0.0 if it is missing or not a number.
	 */
	private double parseKilowattHours(String[] values, String[] units, int index) {
		double value = parseDouble(values, index);
		boolean kiloWattHours = index < units.length && UNIT_KWH.equals(units[index].toLowerCase(Locale.ROOT));
		return kiloWattHours ? value : value / 1000.0d;
	}

	/**
	 * Parses a number. A comma is taken as decimal separator, a dot in front of
	 * it as thousands separator.
	 *
	 * @param values
	 *            The values of a data line.
	 * @param index
	 *            The index of the wanted value.
	 * @return The parsed value or 0.0 if it is missing or not a number.
	 */
	private double parseDouble(String[] values, int index) {
		if (index < 0 || index >= values.length) {
			return 0.0d;
		}
		String value = values[index];
		if (value.length() == 0) {
			return 0.0d;
		}
		if (value.indexOf(',') >= 0) {
			value = value.replace(".", "").replace(',', '.'); //$NON-NLS-1$ //$NON-NLS-2$
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			return 0.0d;
		}
	}

	/**
	 * One row of the energy balance report, all values in kWh.
	 *
	 * @param date
	 *            The date of the row.
	 * @param production
	 *            The total pv production.
	 * @param consumption
	 *            The total consumption.
	 * @param selfConsumption
	 *            The self consumed pv energy.
	 * @param feedIn
	 *            The energy fed into the grid.
	 * @param gridSupply
	 *            The energy taken from the grid.
	 */
	public record EnergyBalanceRow(LocalDate date, double production, double consumption, double selfConsumption,
			double feedIn, double gridSupply) {
	}
}
