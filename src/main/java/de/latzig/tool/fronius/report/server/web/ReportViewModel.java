package de.latzig.tool.fronius.report.server.web;

import java.util.List;

import de.latzig.tool.fronius.report.server.chart.ChartHover;

/**
 * Everything a report page shows.
 *
 * <p>
 * The very same model is rendered server side by Thymeleaf and delivered as
 * JSON to the thin client, so both representations can never drift apart. All
 * texts are already localized and all numbers are already formatted - the thin
 * client does not need to know anything about locales.
 * </p>
 *
 * @param view
 *            The code of the shown period view.
 * @param year
 *            The shown year, <code>null</code> for the overview.
 * @param month
 *            The shown month, <code>null</code> unless the month view is shown.
 * @param title
 *            The localized title of the period.
 * @param hint
 *            The localized description of the shown values.
 * @param metrics
 *            The key figures of the period.
 * @param chartQuery
 *            The query string identifying the period at
 *            <code>/chart.png</code>.
 * @param chartImageMap
 *            The html image map carrying the tool tip of every bar, the
 *            fallback for a client without javascript.
 * @param chartHover
 *            The tool tip model of the chart: all values per category plus the
 *            geometry needed to mark the one under the pointer.
 * @param chartWidth
 *            The width of the chart image in pixel.
 * @param chartHeight
 *            The height of the chart image in pixel.
 * @param path
 *            The canonical path of the page.
 * @param years
 *            All years having a report.
 * @param months
 *            All months of the shown year having a report, empty for the
 *            overview.
 *
 * @author Mario Latzig
 */
public record ReportViewModel(String view, Integer year, Integer month, String title, String hint,
		List<MetricViewModel> metrics, String chartQuery, String chartImageMap, ChartHover chartHover, int chartWidth,
		int chartHeight, String path, List<Integer> years, List<MonthOption> months) {

	/**
	 * One key figure of a period.
	 *
	 * @param key
	 *            The technical key, also used as css modifier.
	 * @param label
	 *            The localized label.
	 * @param value
	 *            The formatted value.
	 * @param unit
	 *            The unit of the value.
	 * @param percent
	 *            The value as percentage for the meter bar, <code>null</code> if
	 *            the figure has no meter.
	 */
	public record MetricViewModel(String key, String label, String value, String unit, Double percent) {
	}

	/**
	 * One selectable month.
	 *
	 * @param value
	 *            The number of the month.
	 * @param label
	 *            The localized short name of the month.
	 */
	public record MonthOption(int value, String label) {
	}
}
