package de.latzig.tool.fronius.report.server.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.latzig.tool.fronius.report.server.config.FroniusProperties;
import de.latzig.tool.fronius.report.server.csv.CsvReportReader;
import de.latzig.tool.fronius.report.server.csv.CsvReportReader.EnergyBalanceRow;
import de.latzig.tool.fronius.report.server.model.DayValues;
import de.latzig.tool.fronius.report.server.model.MonthReport;
import de.latzig.tool.fronius.report.server.model.PeriodSummary;
import de.latzig.tool.fronius.report.server.model.ReportNotFoundException;

/**
 * Provides the reports of the CSV directory.
 *
 * <p>
 * The CSV files are read lazily on the first request of a month and are then
 * kept in memory. A cached month is dropped as soon as the underlying file has
 * been modified, so newly exported reports are picked up without a restart.
 * </p>
 *
 * <p>
 * As long as the configured directory holds no report at all, the bundled
 * sample report is shown instead (see {@link DemoReports}), unless
 * <code>fronius.demo-fallback</code> is switched off.
 * </p>
 *
 * @author Mario Latzig
 */
@Service
public class ReportRepository {

	// consts
	private static final Logger LOG = LoggerFactory.getLogger(ReportRepository.class);

	// fields
	private final FroniusProperties properties;
	private final CsvReportReader reader;
	private final DemoReports demoReports;
	private final Map<YearMonth, CacheEntry> cache = new ConcurrentHashMap<YearMonth, CacheEntry>();
	private volatile Boolean lastDemo;

	/**
	 * Constructor.
	 *
	 * @param properties
	 *            The configuration.
	 * @param reader
	 *            The CSV reader.
	 * @param demoReports
	 *            The bundled sample report.
	 */
	public ReportRepository(FroniusProperties properties, CsvReportReader reader, DemoReports demoReports) {
		this.properties = properties;
		this.reader = reader;
		this.demoReports = demoReports;
	}

	/**
	 * @return <code>true</code> if the bundled sample report is shown, because
	 *         the CSV directory holds no report.
	 */
	public boolean isDemo() {
		return getRoot().demo();
	}

	/**
	 * @return All years having at least one report, sorted ascending.
	 */
	public List<Integer> getAvailableYears() {
		return getAvailableYears(getRoot().path());
	}

	/**
	 * @param year
	 *            The year.
	 * @return All months of the year having a report, sorted ascending.
	 */
	public List<YearMonth> getAvailableMonths(int year) {
		return getAvailableMonths(getRoot().path(), year);
	}

	/**
	 * @return The most recent month having a report.
	 * @throws ReportNotFoundException
	 *             If there is no report at all.
	 */
	public YearMonth getLatestMonth() {
		Path root = getRoot().path();
		List<Integer> years = getAvailableYears(root);
		for (int i = years.size() - 1; i >= 0; i--) {
			List<YearMonth> months = getAvailableMonths(root, years.get(i).intValue());
			if (!months.isEmpty()) {
				return months.get(months.size() - 1);
			}
		}
		throw new ReportNotFoundException("No CSV reports found in " + root); //$NON-NLS-1$
	}

	/**
	 * Returns the report of the given month, reading it from disk if it is not
	 * cached or has been modified in the meantime.
	 *
	 * @param month
	 *            The wanted month.
	 * @return The report of the month.
	 * @throws ReportNotFoundException
	 *             If there is no report for the month.
	 */
	public MonthReport getMonthReport(YearMonth month) {
		return getMonthReport(getRoot().path(), month);
	}

	/**
	 * Returns all month reports of a year.
	 *
	 * @param year
	 *            The wanted year.
	 * @return The reports of the year, sorted ascending.
	 */
	public List<MonthReport> getYearReports(int year) {
		return getYearReports(getRoot().path(), year);
	}

	/**
	 * @param year
	 *            The wanted year.
	 * @return The aggregated values of the year.
	 */
	public PeriodSummary getYearSummary(int year) {
		return getYearSummary(getRoot().path(), year);
	}

	/**
	 * @return The aggregated values of every available year, sorted ascending.
	 */
	public List<PeriodSummary> getAllYearSummaries() {
		Path root = getRoot().path();
		List<PeriodSummary> summaries = new ArrayList<PeriodSummary>();
		for (Integer year : getAvailableYears(root)) {
			try {
				summaries.add(getYearSummary(root, year.intValue()));
			} catch (ReportNotFoundException e) {
				// year directory without reports, ignore
				LOG.debug("Year without reports: {}", year); //$NON-NLS-1$
			}
		}
		return summaries;
	}

	/**
	 * Rescans the CSV directory: reports whose file is new or has been modified
	 * are read into the cache, cached reports whose file has vanished are
	 * dropped.
	 *
	 * <p>
	 * This only keeps the cache warm. Reports are picked up without it as well,
	 * because every request checks the directory and the modification time.
	 * </p>
	 *
	 * @return What the rescan changed.
	 */
	public RefreshResult refresh() {
		Path root = getRoot().path();
		Set<YearMonth> available = new HashSet<YearMonth>();
		for (Integer year : getAvailableYears(root)) {
			available.addAll(getAvailableMonths(root, year.intValue()));
		}

		int dropped = 0;
		for (YearMonth cached : new ArrayList<YearMonth>(this.cache.keySet())) {
			if (!available.contains(cached)) {
				this.cache.remove(cached);
				dropped++;
			}
		}

		int reloaded = 0;
		for (YearMonth month : available) {
			CacheEntry entry = this.cache.get(month);
			Path file = getBalanceFile(root, month);
			if (entry == null || !entry.isValid(file, lastModified(file))) {
				getMonthReport(root, month);
				reloaded++;
			}
		}
		return new RefreshResult(reloaded, dropped, available.size());
	}

	/**
	 * Determines the directory to read from: the configured one, or the sample
	 * report if the configured one holds no report.
	 *
	 * @return The directory to read from.
	 */
	private Root getRoot() {
		String csvDir = this.properties.getCsvDir();
		Path configured = (csvDir == null || csvDir.isBlank()) ? null : Paths.get(csvDir);
		boolean demo = this.properties.isDemoFallback() && !hasReports(configured);
		if (!Boolean.valueOf(demo).equals(this.lastDemo)) {
			this.lastDemo = Boolean.valueOf(demo);
			if (demo && configured == null) {
				LOG.info("No CSV directory configured (fronius.csv-dir), showing the bundled sample report."); //$NON-NLS-1$
			} else if (demo) {
				LOG.info("No CSV reports found in {}, showing the bundled sample report.", configured); //$NON-NLS-1$
			} else if (configured == null || !Files.isDirectory(configured)) {
				LOG.warn("CSV directory does not exist: {}", configured); //$NON-NLS-1$
			}
		}
		if (demo) {
			return new Root(this.demoReports.getRoot(), true);
		}
		return new Root(configured, false);
	}

	/**
	 * @param root
	 *            The directory to check, may be <code>null</code>.
	 * @return <code>true</code> if the directory holds at least one report.
	 */
	private boolean hasReports(Path root) {
		for (Integer year : getAvailableYears(root)) {
			if (!getAvailableMonths(root, year.intValue()).isEmpty()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param root
	 *            The directory to read from, may be <code>null</code>.
	 * @return All years having a sub directory, sorted ascending.
	 */
	private List<Integer> getAvailableYears(Path root) {
		List<Integer> years = new ArrayList<Integer>();
		if (root == null || !Files.isDirectory(root)) {
			return years;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
			for (Path path : stream) {
				String name = path.getFileName().toString();
				if (Files.isDirectory(path) && name.matches("\\d{4}")) { //$NON-NLS-1$
					years.add(Integer.valueOf(name));
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		Collections.sort(years);
		return years;
	}

	/**
	 * @param root
	 *            The directory to read from, may be <code>null</code>.
	 * @param year
	 *            The year.
	 * @return All months of the year having a report, sorted ascending.
	 */
	private List<YearMonth> getAvailableMonths(Path root, int year) {
		List<YearMonth> months = new ArrayList<YearMonth>();
		if (root == null || !Files.isDirectory(root.resolve(String.valueOf(year)))) {
			return months;
		}
		for (int month = 1; month <= 12; month++) {
			if (Files.isReadable(getBalanceFile(root, YearMonth.of(year, month)))) {
				months.add(YearMonth.of(year, month));
			}
		}
		return months;
	}

	/**
	 * @param root
	 *            The directory to read from.
	 * @param month
	 *            The wanted month.
	 * @return The report of the month.
	 */
	private MonthReport getMonthReport(Path root, YearMonth month) {
		Path balanceFile = (root == null) ? null : getBalanceFile(root, month);
		if (balanceFile == null || !Files.isReadable(balanceFile)) {
			throw new ReportNotFoundException("No report available for " + month); //$NON-NLS-1$
		}
		long stamp = lastModified(balanceFile);
		CacheEntry entry = this.cache.get(month);
		if (entry != null && entry.isValid(balanceFile, stamp)) {
			return entry.report();
		}
		MonthReport report = loadMonthReport(balanceFile, month);
		this.cache.put(month, new CacheEntry(balanceFile, stamp, report));
		return report;
	}

	/**
	 * @param root
	 *            The directory to read from.
	 * @param year
	 *            The wanted year.
	 * @return The reports of the year, sorted ascending.
	 */
	private List<MonthReport> getYearReports(Path root, int year) {
		List<MonthReport> reports = new ArrayList<MonthReport>();
		for (YearMonth month : getAvailableMonths(root, year)) {
			reports.add(getMonthReport(root, month));
		}
		if (reports.isEmpty()) {
			throw new ReportNotFoundException("No report available for " + year); //$NON-NLS-1$
		}
		return reports;
	}

	/**
	 * @param root
	 *            The directory to read from.
	 * @param year
	 *            The wanted year.
	 * @return The aggregated values of the year.
	 */
	private PeriodSummary getYearSummary(Path root, int year) {
		PeriodSummary summary = new PeriodSummary(String.valueOf(year));
		for (MonthReport report : getYearReports(root, year)) {
			summary.add(report.getSummary());
		}
		return summary;
	}

	/**
	 * Reads the report of a month from disk.
	 *
	 * @param file
	 *            The file to read.
	 * @param month
	 *            The month to read.
	 * @return The report of the month.
	 */
	private MonthReport loadMonthReport(Path file, YearMonth month) {
		LOG.info("Reading report for {}", month); //$NON-NLS-1$
		try {
			Map<LocalDate, EnergyBalanceRow> balance = this.reader.readEnergyBalance(file);
			List<DayValues> days = new ArrayList<DayValues>();
			for (EnergyBalanceRow row : balance.values()) {
				DayValues day = new DayValues(row.date());
				day.setProduction(row.production());
				day.setConsumption(row.consumption());
				day.setSelfConsumption(row.selfConsumption());
				day.setFeedIn(row.feedIn());
				day.setGridSupply(row.gridSupply());
				days.add(day);
			}
			days.sort((a, b) -> a.getDate().compareTo(b.getDate()));
			return new MonthReport(month, days);
		} catch (IOException e) {
			throw new UncheckedIOException("Report could not be read: " + month, e); //$NON-NLS-1$
		}
	}

	/**
	 * @param root
	 *            The directory to read from.
	 * @param month
	 *            The month.
	 * @return The path of the energy balance file of the month.
	 */
	private Path getBalanceFile(Path root, YearMonth month) {
		return root.resolve(String.valueOf(month.getYear())).resolve(this.properties.getCsv().fileName(month));
	}

	/**
	 * @param file
	 *            The file to check.
	 * @return The last modification time or 0 if the file does not exist.
	 */
	private long lastModified(Path file) {
		try {
			return Files.isReadable(file) ? Files.getLastModifiedTime(file).toMillis() : 0L;
		} catch (IOException e) {
			return 0L;
		}
	}

	/**
	 * The directory the reports are read from.
	 *
	 * @param path
	 *            The directory, may be <code>null</code> if none is configured.
	 * @param demo
	 *            <code>true</code> if it holds the bundled sample report.
	 */
	private record Root(Path path, boolean demo) {
	}

	/**
	 * One cached month report together with its file and modification stamp.
	 *
	 * @param file
	 *            The CSV file the report was read from.
	 * @param stamp
	 *            The modification time of the CSV file.
	 * @param report
	 *            The cached report.
	 */
	private record CacheEntry(Path file, long stamp, MonthReport report) {

		/**
		 * @param currentFile
		 *            The file the month is read from now.
		 * @param currentStamp
		 *            Its current modification time.
		 * @return <code>true</code> if the cached report is still up to date.
		 */
		boolean isValid(Path currentFile, long currentStamp) {
			return this.file.equals(currentFile) && this.stamp == currentStamp;
		}
	}

	/**
	 * What a rescan of the CSV directory changed.
	 *
	 * @param reloaded
	 *            The number of new or modified reports read into the cache.
	 * @param dropped
	 *            The number of cached reports whose file has vanished.
	 * @param available
	 *            The number of reports currently present in the directory.
	 */
	public record RefreshResult(int reloaded, int dropped, int available) {

		/**
		 * @return <code>true</code> if the rescan changed anything.
		 */
		public boolean hasChanges() {
			return this.reloaded > 0 || this.dropped > 0;
		}
	}
}
