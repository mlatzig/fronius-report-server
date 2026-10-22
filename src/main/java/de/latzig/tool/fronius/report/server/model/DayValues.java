package de.latzig.tool.fronius.report.server.model;

import java.time.LocalDate;

/**
 * The energy values of a single day.
 *
 * <p>
 * All energy values are given in kWh.
 * </p>
 *
 * @author Mario Latzig
 */
public class DayValues {

	// fields
	private final LocalDate date;
	private double production;
	private double consumption;
	private double selfConsumption;
	private double feedIn;
	private double gridSupply;

	/**
	 * Constructor.
	 *
	 * @param date
	 *            The date of the day.
	 */
	public DayValues(LocalDate date) {
		this.date = date;
	}

	/**
	 * @return The date of the day.
	 */
	public LocalDate getDate() {
		return this.date;
	}

	/**
	 * @return The day of month.
	 */
	public int getDayOfMonth() {
		return this.date.getDayOfMonth();
	}

	/**
	 * @return The total pv production in kWh.
	 */
	public double getProduction() {
		return this.production;
	}

	/**
	 * @param production
	 *            The total pv production in kWh.
	 */
	public void setProduction(double production) {
		this.production = production;
	}

	/**
	 * @return The total consumption in kWh.
	 */
	public double getConsumption() {
		return this.consumption;
	}

	/**
	 * @param consumption
	 *            The total consumption in kWh.
	 */
	public void setConsumption(double consumption) {
		this.consumption = consumption;
	}

	/**
	 * @return The self consumed pv energy in kWh.
	 */
	public double getSelfConsumption() {
		return this.selfConsumption;
	}

	/**
	 * @param selfConsumption
	 *            The self consumed pv energy in kWh.
	 */
	public void setSelfConsumption(double selfConsumption) {
		this.selfConsumption = selfConsumption;
	}

	/**
	 * @return The energy fed into the grid in kWh.
	 */
	public double getFeedIn() {
		return this.feedIn;
	}

	/**
	 * @param feedIn
	 *            The energy fed into the grid in kWh.
	 */
	public void setFeedIn(double feedIn) {
		this.feedIn = feedIn;
	}

	/**
	 * @return The energy taken from the grid in kWh.
	 */
	public double getGridSupply() {
		return this.gridSupply;
	}

	/**
	 * @param gridSupply
	 *            The energy taken from the grid in kWh.
	 */
	public void setGridSupply(double gridSupply) {
		this.gridSupply = gridSupply;
	}
}
