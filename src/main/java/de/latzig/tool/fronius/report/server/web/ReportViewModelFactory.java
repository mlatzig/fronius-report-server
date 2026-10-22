package de.latzig.tool.fronius.report.server.web;

import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import de.latzig.tool.fronius.report.server.config.FroniusProperties;
import de.latzig.tool.fronius.report.server.i18n.Messages;
import de.latzig.tool.fronius.report.server.model.PeriodSelection;
import de.latzig.tool.fronius.report.server.model.PeriodSummary;
import de.latzig.tool.fronius.report.server.model.PeriodView;
import de.latzig.tool.fronius.report.server.model.ReportNotFoundException;
import de.latzig.tool.fronius.report.server.service.ChartService;
import de.latzig.tool.fronius.report.server.service.ChartService.RenderedChart;
import de.latzig.tool.fronius.report.server.service.ReportRepository;
import de.latzig.tool.fronius.report.server.web.ReportViewModel.MetricViewModel;
import de.latzig.tool.fronius.report.server.web.ReportViewModel.MonthOption;

/**
 * Builds the view model of a report page.
 *
 * @author Mario Latzig
 */
@Component
public class ReportViewModelFactory {

	// consts
	private static final String UNIT_KWH = "unit.kwh"; //$NON-NLS-1$
	private static final String UNIT_PERCENT = "unit.percent"; //$NON-NLS-1$

	// fields
	private final ReportRepository repository;
	private final Messages messages;
	private final ChartService chartService;
	private final FroniusProperties properties;

	/**
	 * Constructor.
	 *
	 * @param repository
	 *            The report repository.
	 * @param messages
	 *            The localized texts.
	 * @param chartService
	 *            The chart service, delivering the image map of the chart.
	 * @param properties
	 *            The configuration.
	 */
	public ReportViewModelFactory(ReportRepository repository, Messages messages, ChartService chartService,
			FroniusProperties properties) {
		this.repository = repository;
		this.messages = messages;
		this.chartService = chartService;
		this.properties = properties;
	}

	/**
	 * Completes an incomplete request: a missing year becomes the most recent one,
	 * a missing month the most recent one of the selected year.
	 *
	 * @param view
	 *            The wanted view.
	 * @param year
	 *            The wanted year, may be <code>null</code>.
	 * @param month
	 *            The wanted month, may be <code>null</code>.
	 * @return The resolved selection.
	 * @throws ReportNotFoundException
	 *             If the selection cannot be resolved to an existing report.
	 */
	public PeriodSelection resolve(PeriodView view, Integer year, Integer month) {
		if (view == PeriodView.OVERVIEW) {
			return new PeriodSelection(PeriodView.OVERVIEW, null, null);
		}
		int theYear = (year != null) ? year.intValue() : this.repository.getLatestMonth().getYear();
		if (view == PeriodView.YEAR) {
			return new PeriodSelection(PeriodView.YEAR, Integer.valueOf(theYear), null);
		}
		List<YearMonth> months = this.repository.getAvailableMonths(theYear);
		if (months.isEmpty()) {
			throw new ReportNotFoundException("No report available for " + theYear); //$NON-NLS-1$
		}
		int theMonth;
		if (month != null && months.contains(YearMonth.of(theYear, clampMonth(month.intValue())))) {
			theMonth = clampMonth(month.intValue());
		} else {
			theMonth = months.get(months.size() - 1).getMonthValue();
		}
		return new PeriodSelection(PeriodView.MONTH, Integer.valueOf(theYear), Integer.valueOf(theMonth));
	}

	/**
	 * @param selection
	 *            The resolved selection.
	 * @return The aggregated values of the selected period.
	 */
	public PeriodSummary getSummary(PeriodSelection selection) {
		return switch (selection.view()) {
		case OVERVIEW -> {
			PeriodSummary total = new PeriodSummary("overview"); //$NON-NLS-1$
			for (PeriodSummary summary : this.repository.getAllYearSummaries()) {
				total.add(summary);
			}
			yield total;
		}
		case YEAR -> this.repository.getYearSummary(selection.year().intValue());
		case MONTH -> this.repository.getMonthReport(selection.yearMonth()).getSummary();
		};
	}

	/**
	 * @param selection
	 *            The resolved selection.
	 * @return The localized title of the selected period.
	 */
	public String getTitle(PeriodSelection selection) {
		return switch (selection.view()) {
		case OVERVIEW -> this.messages.get("view.overview.title"); //$NON-NLS-1$
		// as string, otherwise the message format would group the digits: 2.025
		case YEAR -> this.messages.get("view.year.title", String.valueOf(selection.year())); //$NON-NLS-1$
		case MONTH -> getMonthLabel(selection.yearMonth(), TextStyle.FULL) + " " + selection.year(); //$NON-NLS-1$
		};
	}

	/**
	 * @param selection
	 *            The resolved selection.
	 * @return The complete view model of the selected period.
	 */
	public ReportViewModel build(PeriodSelection selection) {
		PeriodSummary summary = getSummary(selection);
		List<MonthOption> months = new ArrayList<MonthOption>();
		if (selection.year() != null) {
			for (YearMonth month : this.repository.getAvailableMonths(selection.year().intValue())) {
				months.add(new MonthOption(month.getMonthValue(), getMonthLabel(month, TextStyle.SHORT)));
			}
		}
		// renders the chart, so the page can carry the image map of its bars; the
		// following request of the image itself is then served from the cache
		String title = getTitle(selection);
		RenderedChart chart = this.chartService.render(title, summary, selection, this.properties.getChartWidth(),
				this.properties.getChartHeight());

		return new ReportViewModel(selection.view().code(), selection.year(), selection.month(), title,
				this.messages.get("view." + selection.view().code() + ".hint"), //$NON-NLS-1$ //$NON-NLS-2$
				buildMetrics(summary), selection.chartQuery(), chart.imageMap(), chart.hover(), chart.width(),
				chart.height(), selection.path(), this.repository.getAvailableYears(), months);
	}

	/**
	 * @param summary
	 *            The aggregated values of the shown period.
	 * @return The key figures of the period.
	 */
	private List<MetricViewModel> buildMetrics(PeriodSummary summary) {
		List<MetricViewModel> metrics = new ArrayList<MetricViewModel>();
		metrics.add(energy("production", summary.getProduction())); //$NON-NLS-1$
		metrics.add(energy("consumption", summary.getConsumption())); //$NON-NLS-1$
		metrics.add(energy("self-consumption", summary.getSelfConsumption())); //$NON-NLS-1$
		metrics.add(energy("feed-in", summary.getFeedIn())); //$NON-NLS-1$
		metrics.add(energy("grid-supply", summary.getGridSupply())); //$NON-NLS-1$
		metrics.add(percent("autarky", summary.getAutarky())); //$NON-NLS-1$
		metrics.add(percent("self-consumption-rate", summary.getSelfConsumptionRate())); //$NON-NLS-1$
		return metrics;
	}

	/**
	 * @param key
	 *            The key of the figure.
	 * @param value
	 *            The value in kWh.
	 * @return The key figure.
	 */
	private MetricViewModel energy(String key, double value) {
		return new MetricViewModel(key, label(key), format(value, 0), this.messages.get(UNIT_KWH), null);
	}

	/**
	 * @param key
	 *            The key of the figure.
	 * @param value
	 *            The value in percent.
	 * @return The key figure, including the fill level of its meter.
	 */
	private MetricViewModel percent(String key, double value) {
		double capped = Math.max(0.0d, Math.min(100.0d, value));
		return new MetricViewModel(key, label(key), format(value, 1), this.messages.get(UNIT_PERCENT),
				Double.valueOf(capped));
	}

	/**
	 * @param key
	 *            The key of the figure.
	 * @return The localized label of the figure.
	 */
	private String label(String key) {
		return this.messages.get("metric." + key); //$NON-NLS-1$
	}

	/**
	 * @param value
	 *            The value to format.
	 * @param fractionDigits
	 *            The number of fraction digits.
	 * @return The localized representation of the value.
	 */
	private String format(double value, int fractionDigits) {
		NumberFormat format = NumberFormat.getNumberInstance(this.messages.getLocale());
		format.setMinimumFractionDigits(fractionDigits);
		format.setMaximumFractionDigits(fractionDigits);
		return format.format(value);
	}

	/**
	 * @param month
	 *            The month.
	 * @param style
	 *            The wanted text style.
	 * @return The localized name of the month.
	 */
	private String getMonthLabel(YearMonth month, TextStyle style) {
		Locale locale = this.messages.getLocale();
		return month.getMonth().getDisplayName(style, locale);
	}

	/**
	 * @param month
	 *            The requested month.
	 * @return The month, limited to a valid value.
	 */
	private int clampMonth(int month) {
		return Math.max(1, Math.min(12, month));
	}
}
