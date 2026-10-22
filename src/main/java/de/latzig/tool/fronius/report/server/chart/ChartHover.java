package de.latzig.tool.fronius.report.server.chart;

import java.util.List;

/**
 * Everything the client needs to show a tool tip on the chart.
 *
 * <p>
 * One entry per category - a day, a month or a year. Pointing anywhere inside
 * the column shows all values of that category at once; the value the pointer
 * actually sits on is highlighted.
 * </p>
 *
 * <p>
 * All coordinates are pixels of the rendered image.
 * </p>
 *
 * @param plot
 *            The area of the plot, outside of it nothing is shown.
 * @param columns
 *            The categories of the chart, ordered from left to right.
 *
 * @author Mario Latzig
 */
public record ChartHover(Bounds plot, List<Column> columns) {

	/**
	 * A rectangle of the rendered image.
	 *
	 * @param x1
	 *            The left edge.
	 * @param y1
	 *            The top edge.
	 * @param x2
	 *            The right edge.
	 * @param y2
	 *            The bottom edge.
	 */
	public record Bounds(int x1, int y1, int x2, int y2) {
	}

	/**
	 * One category of the chart.
	 *
	 * @param title
	 *            The label of the category.
	 * @param x1
	 *            The left edge of the column, halfway to the previous category.
	 * @param x2
	 *            The right edge of the column, halfway to the next category.
	 * @param rows
	 *            The values of the category.
	 */
	public record Column(String title, int x1, int x2, List<Row> rows) {
	}

	/**
	 * One value of a category.
	 *
	 * @param key
	 *            The technical key, used as css modifier for the color.
	 * @param label
	 *            The localized label.
	 * @param value
	 *            The formatted value including its unit.
	 * @param y1
	 *            The top edge of the segment, <code>null</code> if the value
	 *            cannot be pointed at.
	 * @param y2
	 *            The bottom edge of the segment, <code>null</code> if the value
	 *            cannot be pointed at.
	 */
	public record Row(String key, String label, String value, Integer y1, Integer y2) {
	}
}
