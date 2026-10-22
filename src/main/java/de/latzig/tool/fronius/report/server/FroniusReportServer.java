package de.latzig.tool.fronius.report.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main class of the Fronius Report Server.
 *
 * <p>
 * Reads the monthly CSV reports exported from Solar.web / the Fronius GEN24
 * inverter and renders them as charts, lazily on request.
 * </p>
 *
 * @author Mario Latzig
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class FroniusReportServer {

	/**
	 * Entry point.
	 *
	 * @param args
	 *            The command line arguments.
	 */
	public static void main(String[] args) {
		// charts are rendered server side, no display needed
		System.setProperty("java.awt.headless", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		SpringApplication.run(FroniusReportServer.class, args);
	}
}
