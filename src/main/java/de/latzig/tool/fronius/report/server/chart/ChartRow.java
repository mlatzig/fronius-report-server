package de.latzig.tool.fronius.report.server.chart;

/**
 * One category of a chart, e.g. a single day, a month or a year.
 *
 * <p>
 * All energy values are given in kWh.
 * </p>
 *
 * @param label
 *            The label of the category shown at the domain axis.
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
 *
 * @author Mario Latzig
 */
public record ChartRow(String label, double production, double consumption, double selfConsumption, double feedIn,
		double gridSupply) {
}
