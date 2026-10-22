package de.latzig.tool.fronius.report.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import de.latzig.tool.fronius.report.server.service.ReportRepository.RefreshResult;

/**
 * Rescans the CSV directory periodically.
 *
 * <p>
 * Newly exported reports are picked up without this task as well - every
 * request scans the directory and checks the modification time of the file.
 * The task only keeps the cache warm, so the first request after an export is
 * already fast, and it surfaces a broken export in the log right away instead
 * of on the next click.
 * </p>
 *
 * <p>
 * The schedule is configured by <code>fronius.refresh-cron</code>. The value
 * <code>-</code> disables the task.
 * </p>
 *
 * @author Mario Latzig
 */
@Component
public class ReportRefreshTask {

	// consts
	private static final Logger LOG = LoggerFactory.getLogger(ReportRefreshTask.class);

	// fields
	private final ReportRepository repository;

	/**
	 * Constructor.
	 *
	 * @param repository
	 *            The report repository.
	 */
	public ReportRefreshTask(ReportRepository repository) {
		this.repository = repository;
	}

	/**
	 * Fills the cache once the application is up, so the very first request does
	 * not have to read every report.
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void refreshOnStartup() {
		refresh();
	}

	/**
	 * Rescans the CSV directory on the configured schedule, by default five
	 * minutes past every hour.
	 */
	@Scheduled(cron = "${fronius.refresh-cron:0 5 * * * *}")
	public void refreshScheduled() {
		refresh();
	}

	/**
	 * Performs the rescan. A failure must never kill the scheduler, so every
	 * exception is caught and logged.
	 */
	private void refresh() {
		try {
			RefreshResult result = this.repository.refresh();
			if (result.hasChanges()) {
				LOG.info("Rescan of the report directory: {} report(s) read, {} dropped, {} available.", //$NON-NLS-1$
						Integer.valueOf(result.reloaded()), Integer.valueOf(result.dropped()),
						Integer.valueOf(result.available()));
			} else {
				LOG.debug("Rescan of the report directory: nothing changed, {} report(s) available.", //$NON-NLS-1$
						Integer.valueOf(result.available()));
			}
		} catch (RuntimeException e) {
			LOG.warn("Rescan of the report directory failed.", e); //$NON-NLS-1$
		}
	}
}
