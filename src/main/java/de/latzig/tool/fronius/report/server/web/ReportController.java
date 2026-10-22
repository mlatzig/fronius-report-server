package de.latzig.tool.fronius.report.server.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import de.latzig.tool.fronius.report.server.config.FroniusProperties;
import de.latzig.tool.fronius.report.server.model.PeriodSelection;
import de.latzig.tool.fronius.report.server.model.PeriodView;
import de.latzig.tool.fronius.report.server.service.ReportRepository;

/**
 * Delivers the HTML pages of the report server.
 *
 * <p>
 * Every page is rendered completely on the server, so the application also
 * works without javascript. The thin client on top of it only replaces the
 * parts that change when another period is selected.
 * </p>
 *
 * @author Mario Latzig
 */
@Controller
public class ReportController {

	// fields
	private final ReportViewModelFactory viewModelFactory;
	private final ReportRepository repository;
	private final FroniusProperties properties;

	/**
	 * Constructor.
	 *
	 * @param viewModelFactory
	 *            The view model factory.
	 * @param repository
	 *            The report repository, tells whether the sample report is
	 *            shown.
	 * @param properties
	 *            The configuration.
	 */
	public ReportController(ReportViewModelFactory viewModelFactory, ReportRepository repository,
			FroniusProperties properties) {
		this.viewModelFactory = viewModelFactory;
		this.repository = repository;
		this.properties = properties;
	}

	/**
	 * Redirects to the most recent month.
	 *
	 * @return The redirect.
	 */
	@GetMapping("/")
	public String getIndex() {
		return redirect(this.viewModelFactory.resolve(PeriodView.MONTH, null, null));
	}

	/**
	 * Shows the daily values of a month.
	 *
	 * @param year
	 *            The year.
	 * @param month
	 *            The month.
	 * @param model
	 *            The view model.
	 * @return The name of the template.
	 */
	@GetMapping("/month/{year}/{month}")
	public String getMonth(@PathVariable int year, @PathVariable int month, Model model) {
		return render(PeriodView.MONTH, Integer.valueOf(year), Integer.valueOf(month), model);
	}

	/**
	 * Shows the monthly values of a year.
	 *
	 * @param year
	 *            The year.
	 * @param model
	 *            The view model.
	 * @return The name of the template.
	 */
	@GetMapping("/year/{year}")
	public String getYear(@PathVariable int year, Model model) {
		return render(PeriodView.YEAR, Integer.valueOf(year), null, model);
	}

	/**
	 * Shows the yearly values of the whole history.
	 *
	 * @param model
	 *            The view model.
	 * @return The name of the template.
	 */
	@GetMapping("/overview")
	public String getOverview(Model model) {
		return render(PeriodView.OVERVIEW, null, null, model);
	}

	/**
	 * Target of the selection form, used if javascript is not available. Redirects
	 * to the canonical path of the selected period.
	 *
	 * @param view
	 *            The selected view.
	 * @param year
	 *            The selected year, may be <code>null</code>.
	 * @param month
	 *            The selected month, may be <code>null</code>.
	 * @return The redirect.
	 */
	@GetMapping("/select")
	public String select( //
			@RequestParam(name = "view", required = false) String view, //
			@RequestParam(name = "year", required = false) Integer year, //
			@RequestParam(name = "month", required = false) Integer month) {
		return redirect(this.viewModelFactory.resolve(PeriodView.fromCode(view, PeriodView.MONTH), year, month));
	}

	/**
	 * Fills the model and selects the template.
	 *
	 * @param view
	 *            The wanted view.
	 * @param year
	 *            The wanted year, may be <code>null</code>.
	 * @param month
	 *            The wanted month, may be <code>null</code>.
	 * @param model
	 *            The view model.
	 * @return The name of the template.
	 */
	private String render(PeriodView view, Integer year, Integer month, Model model) {
		model.addAttribute("report", this.viewModelFactory.build(this.viewModelFactory.resolve(view, year, month))); //$NON-NLS-1$
		model.addAttribute("plantName", this.properties.getPlantName()); //$NON-NLS-1$
		model.addAttribute("demo", Boolean.valueOf(this.repository.isDemo())); //$NON-NLS-1$
		return "report"; //$NON-NLS-1$
	}

	/**
	 * @param selection
	 *            The resolved selection.
	 * @return The redirect to the canonical path of the selection.
	 */
	private String redirect(PeriodSelection selection) {
		return "redirect:" + selection.path(); //$NON-NLS-1$
	}
}
