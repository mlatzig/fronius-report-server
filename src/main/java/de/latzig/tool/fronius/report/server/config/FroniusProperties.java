package de.latzig.tool.fronius.report.server.config;

import java.time.YearMonth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration of the Fronius Report Server.
 *
 * @author Mario Latzig
 */
@ConfigurationProperties(prefix = "fronius")
public class FroniusProperties {

	/** Root directory holding one sub directory per year with the CSV reports. */
	private String csvDir;

	/** Name of the pv plant, shown as tooltip of the branding. */
	private String plantName = ""; //$NON-NLS-1$

	/** Shows the bundled sample report if the CSV directory holds no report. */
	private boolean demoFallback = true;

	/** Cron expression of the periodic rescan, "-" disables it. */
	private String refreshCron = "0 5 * * * *"; //$NON-NLS-1$

	/** Default width of a rendered chart in pixel. */
	private int chartWidth = 1100;

	/** Default height of a rendered chart in pixel. */
	private int chartHeight = 480;

	/** Format of the CSV exports. */
	private final Csv csv = new Csv();

	/**
	 * @return The root directory of the CSV reports.
	 */
	public String getCsvDir() {
		return this.csvDir;
	}

	/**
	 * @param csvDir
	 *            The root directory of the CSV reports.
	 */
	public void setCsvDir(String csvDir) {
		this.csvDir = csvDir;
	}

	/**
	 * @return The name of the pv plant, may be empty.
	 */
	public String getPlantName() {
		return this.plantName;
	}

	/**
	 * @param plantName
	 *            The name of the pv plant.
	 */
	public void setPlantName(String plantName) {
		this.plantName = plantName;
	}

	/**
	 * @return <code>true</code> if the sample report is shown when the CSV
	 *         directory holds no report.
	 */
	public boolean isDemoFallback() {
		return this.demoFallback;
	}

	/**
	 * @param demoFallback
	 *            <code>true</code> to show the sample report when the CSV
	 *            directory holds no report.
	 */
	public void setDemoFallback(boolean demoFallback) {
		this.demoFallback = demoFallback;
	}

	/**
	 * @return The cron expression of the periodic rescan.
	 */
	public String getRefreshCron() {
		return this.refreshCron;
	}

	/**
	 * @param refreshCron
	 *            The cron expression of the periodic rescan.
	 */
	public void setRefreshCron(String refreshCron) {
		this.refreshCron = refreshCron;
	}

	/**
	 * @return The default chart width in pixel.
	 */
	public int getChartWidth() {
		return this.chartWidth;
	}

	/**
	 * @param chartWidth
	 *            The default chart width in pixel.
	 */
	public void setChartWidth(int chartWidth) {
		this.chartWidth = chartWidth;
	}

	/**
	 * @return The default chart height in pixel.
	 */
	public int getChartHeight() {
		return this.chartHeight;
	}

	/**
	 * @param chartHeight
	 *            The default chart height in pixel.
	 */
	public void setChartHeight(int chartHeight) {
		this.chartHeight = chartHeight;
	}

	/**
	 * @return The format of the CSV exports.
	 */
	public Csv getCsv() {
		return this.csv;
	}

	/**
	 * Format of the CSV exports. Solar.web localizes file names and column
	 * headers, the defaults match the german export.
	 */
	public static class Csv {

		/** File name of a monthly report, <code>{year}</code> and <code>{month}</code> are replaced. */
		private String filePattern = "Energiebilanz_Monatlicher_Report_{year}_{month}.csv"; //$NON-NLS-1$

		/** Format of the date column. */
		private String dateFormat = "dd.MM.yyyy"; //$NON-NLS-1$

		/** Header names of the value columns. */
		private final Columns columns = new Columns();

		/**
		 * @return The file name pattern of a monthly report.
		 */
		public String getFilePattern() {
			return this.filePattern;
		}

		/**
		 * @param filePattern
		 *            The file name pattern of a monthly report.
		 */
		public void setFilePattern(String filePattern) {
			this.filePattern = filePattern;
		}

		/**
		 * @param month
		 *            The month of the report.
		 * @return The file name of the monthly report.
		 */
		public String fileName(YearMonth month) {
			return this.filePattern //
					.replace("{year}", String.format("%04d", Integer.valueOf(month.getYear()))) //$NON-NLS-1$ //$NON-NLS-2$
					.replace("{month}", String.format("%02d", Integer.valueOf(month.getMonthValue()))); //$NON-NLS-1$ //$NON-NLS-2$
		}

		/**
		 * @return The format of the date column.
		 */
		public String getDateFormat() {
			return this.dateFormat;
		}

		/**
		 * @param dateFormat
		 *            The format of the date column.
		 */
		public void setDateFormat(String dateFormat) {
			this.dateFormat = dateFormat;
		}

		/**
		 * @return The header names of the value columns.
		 */
		public Columns getColumns() {
			return this.columns;
		}
	}

	/**
	 * Header names of the value columns of the energy balance report. A column
	 * whose name is not found is taken from its usual position.
	 */
	public static class Columns {

		private String production = "Gesamt Erzeugung"; //$NON-NLS-1$
		private String consumption = "Gesamt Verbrauch"; //$NON-NLS-1$
		private String selfConsumption = "Eigenverbrauch"; //$NON-NLS-1$
		private String feedIn = "Energie ins Netz eingespeist"; //$NON-NLS-1$
		private String gridSupply = "Energie vom Netz bezogen"; //$NON-NLS-1$

		/**
		 * @return The header of the production column.
		 */
		public String getProduction() {
			return this.production;
		}

		/**
		 * @param production
		 *            The header of the production column.
		 */
		public void setProduction(String production) {
			this.production = production;
		}

		/**
		 * @return The header of the consumption column.
		 */
		public String getConsumption() {
			return this.consumption;
		}

		/**
		 * @param consumption
		 *            The header of the consumption column.
		 */
		public void setConsumption(String consumption) {
			this.consumption = consumption;
		}

		/**
		 * @return The header of the self consumption column.
		 */
		public String getSelfConsumption() {
			return this.selfConsumption;
		}

		/**
		 * @param selfConsumption
		 *            The header of the self consumption column.
		 */
		public void setSelfConsumption(String selfConsumption) {
			this.selfConsumption = selfConsumption;
		}

		/**
		 * @return The header of the feed in column.
		 */
		public String getFeedIn() {
			return this.feedIn;
		}

		/**
		 * @param feedIn
		 *            The header of the feed in column.
		 */
		public void setFeedIn(String feedIn) {
			this.feedIn = feedIn;
		}

		/**
		 * @return The header of the grid supply column.
		 */
		public String getGridSupply() {
			return this.gridSupply;
		}

		/**
		 * @param gridSupply
		 *            The header of the grid supply column.
		 */
		public void setGridSupply(String gridSupply) {
			this.gridSupply = gridSupply;
		}
	}
}
