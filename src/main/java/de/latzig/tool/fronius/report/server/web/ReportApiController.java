package de.latzig.tool.fronius.report.server.web;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.latzig.tool.fronius.report.server.model.PeriodSelection;
import de.latzig.tool.fronius.report.server.model.PeriodView;

/**
 * The API used by the thin client.
 *
 * <p>
 * Delivers exactly the same view model the server renders into the page, with
 * all texts already localized and all numbers already formatted.
 * </p>
 *
 * @author Mario Latzig
 */
@RestController
@RequestMapping("/api")
public class ReportApiController {

	// fields
	private final ReportViewModelFactory viewModelFactory;

	/**
	 * Constructor.
	 *
	 * @param viewModelFactory
	 *            The view model factory.
	 */
	public ReportApiController(ReportViewModelFactory viewModelFactory) {
		this.viewModelFactory = viewModelFactory;
	}

	/**
	 * Returns the view model of a period.
	 *
	 * @param view
	 *            The wanted view: <code>overview</code>, <code>year</code> or
	 *            <code>month</code>.
	 * @param year
	 *            The wanted year, may be <code>null</code>.
	 * @param month
	 *            The wanted month, may be <code>null</code>.
	 * @return The view model of the period.
	 */
	@GetMapping("/report")
	public ResponseEntity<ReportViewModel> getReport( //
			@RequestParam(name = "view", required = false) String view, //
			@RequestParam(name = "year", required = false) Integer year, //
			@RequestParam(name = "month", required = false) Integer month) {
		PeriodSelection selection = this.viewModelFactory.resolve(PeriodView.fromCode(view, PeriodView.MONTH), year,
				month);
		return ResponseEntity.ok() //
				.cacheControl(CacheControl.noCache()) //
				.body(this.viewModelFactory.build(selection));
	}
}
