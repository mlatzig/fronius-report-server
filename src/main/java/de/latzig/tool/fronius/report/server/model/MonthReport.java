package de.latzig.tool.fronius.report.server.model;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

/**
 * The report of a single month, containing all daily values.
 *
 * @author Mario Latzig
 */
public class MonthReport {

	// fields
	private final YearMonth month;
	private final List<DayValues> days;
	private final PeriodSummary summary;

	/**
	 * Constructor.
	 *
	 * @param month
	 *            The month of the report.
	 * @param days
	 *            The daily values, sorted ascending by date.
	 */
	public MonthReport(YearMonth month, List<DayValues> days) {
		this.month = month;
		this.days = Collections.unmodifiableList(days);
		this.summary = new PeriodSummary(month.toString());
		for (DayValues day : days) {
			this.summary.add(day);
		}
	}

	/**
	 * @return The month of the report.
	 */
	public YearMonth getMonth() {
		return this.month;
	}

	/**
	 * @return The daily values, sorted ascending by date.
	 */
	public List<DayValues> getDays() {
		return this.days;
	}

	/**
	 * @return The aggregated values of the month.
	 */
	public PeriodSummary getSummary() {
		return this.summary;
	}
}
