package de.latzig.tool.fronius.report.server.model;

import java.time.YearMonth;

/**
 * A resolved selection of a period.
 *
 * <p>
 * Created by the report view model factory, which fills in missing parts
 * of an incomplete request - e.g. the most recent month if only a year was
 * given.
 * </p>
 *
 * @param view
 *            The selected view.
 * @param year
 *            The selected year, <code>null</code> for the overview.
 * @param month
 *            The selected month, <code>null</code> unless the month view is
 *            selected.
 *
 * @author Mario Latzig
 */
public record PeriodSelection(PeriodView view, Integer year, Integer month) {

	/**
	 * @return The selected month.
	 */
	public YearMonth yearMonth() {
		return YearMonth.of(this.year.intValue(), this.month.intValue());
	}

	/**
	 * @return The canonical path of the selection.
	 */
	public String path() {
		return switch (this.view) {
		case OVERVIEW -> "/overview"; //$NON-NLS-1$
		case YEAR -> "/year/" + this.year; //$NON-NLS-1$
		case MONTH -> "/month/" + this.year + "/" + this.month; //$NON-NLS-1$ //$NON-NLS-2$
		};
	}

	/**
	 * @return The query string identifying the period at <code>/chart.png</code>,
	 *         without the chart type.
	 */
	public String chartQuery() {
		return switch (this.view) {
		case OVERVIEW -> "view=" + this.view.code(); //$NON-NLS-1$
		case YEAR -> "view=" + this.view.code() + "&year=" + this.year; //$NON-NLS-1$ //$NON-NLS-2$
		case MONTH -> "view=" + this.view.code() + "&year=" + this.year + "&month=" + this.month; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		};
	}
}
