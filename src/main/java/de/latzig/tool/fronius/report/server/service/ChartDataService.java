package de.latzig.tool.fronius.report.server.service;

import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import de.latzig.tool.fronius.report.server.chart.ChartRow;
import de.latzig.tool.fronius.report.server.i18n.Messages;
import de.latzig.tool.fronius.report.server.model.DayValues;
import de.latzig.tool.fronius.report.server.model.MonthReport;
import de.latzig.tool.fronius.report.server.model.PeriodSelection;
import de.latzig.tool.fronius.report.server.model.PeriodSummary;

/**
 * Converts the reports into the categories of a chart.
 *
 * @author Mario Latzig
 */
@Service
public class ChartDataService {

	// fields
	private final ReportRepository repository;
	private final Messages messages;

	/**
	 * Constructor.
	 *
	 * @param repository
	 *            The report repository.
	 * @param messages
	 *            The localized texts.
	 */
	public ChartDataService(ReportRepository repository, Messages messages) {
		this.repository = repository;
		this.messages = messages;
	}

	/**
	 * @param selection
	 *            The resolved selection.
	 * @return The categories of the selected period: one per day, per month or per
	 *         year.
	 */
	public List<ChartRow> getRows(PeriodSelection selection) {
		return switch (selection.view()) {
		case OVERVIEW -> getOverviewRows();
		case YEAR -> getYearRows(selection.year().intValue());
		case MONTH -> getMonthRows(selection.yearMonth());
		};
	}

	/**
	 * @param month
	 *            The wanted month.
	 * @return One category per day of the month.
	 */
	public List<ChartRow> getMonthRows(YearMonth month) {
		MonthReport report = this.repository.getMonthReport(month);
		List<ChartRow> rows = new ArrayList<ChartRow>();
		for (DayValues day : report.getDays()) {
			rows.add(new ChartRow(String.valueOf(day.getDayOfMonth()), day.getProduction(), day.getConsumption(),
					day.getSelfConsumption(), day.getFeedIn(), day.getGridSupply()));
		}
		return rows;
	}

	/**
	 * @param year
	 *            The wanted year.
	 * @return One category per month of the year.
	 */
	public List<ChartRow> getYearRows(int year) {
		List<ChartRow> rows = new ArrayList<ChartRow>();
		for (MonthReport report : this.repository.getYearReports(year)) {
			String label = report.getMonth().getMonth().getDisplayName(TextStyle.SHORT, this.messages.getLocale());
			rows.add(toRow(label, report.getSummary()));
		}
		return rows;
	}

	/**
	 * @return One category per available year.
	 */
	public List<ChartRow> getOverviewRows() {
		List<ChartRow> rows = new ArrayList<ChartRow>();
		for (PeriodSummary summary : this.repository.getAllYearSummaries()) {
			rows.add(toRow(summary.getLabel(), summary));
		}
		return rows;
	}

	/**
	 * @param label
	 *            The label of the category.
	 * @param summary
	 *            The aggregated values.
	 * @return The category.
	 */
	private ChartRow toRow(String label, PeriodSummary summary) {
		return new ChartRow(label, summary.getProduction(), summary.getConsumption(), summary.getSelfConsumption(),
				summary.getFeedIn(), summary.getGridSupply());
	}
}
