package de.latzig.tool.fronius.report.server.model;

/**
 * Aggregated energy values of a period (month, year or the whole history).
 *
 * <p>
 * All energy values are given in kWh.
 * </p>
 *
 * @author Mario Latzig
 */
public class PeriodSummary {

	// fields
	private final String label;
	private double production;
	private double consumption;
	private double selfConsumption;
	private double feedIn;
	private double gridSupply;
	private int days;

	/**
	 * Constructor.
	 *
	 * @param label
	 *            The label of the period, e.g. "Juni 2026".
	 */
	public PeriodSummary(String label) {
		this.label = label;
	}

	/**
	 * Adds the values of a single day to this summary.
	 *
	 * @param day
	 *            The day to add.
	 */
	public void add(DayValues day) {
		this.production += day.getProduction();
		this.consumption += day.getConsumption();
		this.selfConsumption += day.getSelfConsumption();
		this.feedIn += day.getFeedIn();
		this.gridSupply += day.getGridSupply();
		this.days++;
	}

	/**
	 * Adds another summary to this summary.
	 *
	 * @param other
	 *            The summary to add.
	 */
	public void add(PeriodSummary other) {
		this.production += other.production;
		this.consumption += other.consumption;
		this.selfConsumption += other.selfConsumption;
		this.feedIn += other.feedIn;
		this.gridSupply += other.gridSupply;
		this.days += other.days;
	}

	/**
	 * @return The label of the period.
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * @return The total pv production in kWh.
	 */
	public double getProduction() {
		return this.production;
	}

	/**
	 * @return The total consumption in kWh.
	 */
	public double getConsumption() {
		return this.consumption;
	}

	/**
	 * @return The self consumed pv energy in kWh.
	 */
	public double getSelfConsumption() {
		return this.selfConsumption;
	}

	/**
	 * @return The energy fed into the grid in kWh.
	 */
	public double getFeedIn() {
		return this.feedIn;
	}

	/**
	 * @return The energy taken from the grid in kWh.
	 */
	public double getGridSupply() {
		return this.gridSupply;
	}

	/**
	 * @return The number of days contained in this summary.
	 */
	public int getDays() {
		return this.days;
	}

	/**
	 * The degree of autarky: how much of the consumption was covered by the own pv
	 * plant.
	 *
	 * @return The autarky in percent.
	 */
	public double getAutarky() {
		return (this.consumption > 0.0d) ? (this.selfConsumption / this.consumption * 100.0d) : 0.0d;
	}

	/**
	 * The self consumption rate: how much of the produced energy was used by
	 * ourselves instead of being fed into the grid.
	 *
	 * @return The self consumption rate in percent.
	 */
	public double getSelfConsumptionRate() {
		return (this.production > 0.0d) ? (this.selfConsumption / this.production * 100.0d) : 0.0d;
	}
}
