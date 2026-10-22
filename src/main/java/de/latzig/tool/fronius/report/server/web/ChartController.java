package de.latzig.tool.fronius.report.server.web;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.latzig.tool.fronius.report.server.config.FroniusProperties;
import de.latzig.tool.fronius.report.server.model.PeriodSelection;
import de.latzig.tool.fronius.report.server.model.PeriodView;
import de.latzig.tool.fronius.report.server.service.ChartService;
import de.latzig.tool.fronius.report.server.service.ReportRepository;

/**
 * Renders the charts as PNG images.
 *
 * <p>
 * The images are built on request, the underlying CSV files are read lazily by
 * the {@link ReportRepository}. Usually the page has just been built with the
 * same period, so the rendering is still held by the {@link ChartService}.
 * </p>
 *
 * @author Mario Latzig
 */
@RestController
public class ChartController {

	// consts
	private static final int MIN_SIZE = 200;
	private static final int MAX_SIZE = 4000;

	// fields
	private final ChartService chartService;
	private final ReportViewModelFactory viewModelFactory;
	private final FroniusProperties properties;

	/**
	 * Constructor.
	 *
	 * @param chartService
	 *            The chart service.
	 * @param viewModelFactory
	 *            The view model factory.
	 * @param properties
	 *            The configuration.
	 */
	public ChartController(ChartService chartService, ReportViewModelFactory viewModelFactory,
			FroniusProperties properties) {
		this.chartService = chartService;
		this.viewModelFactory = viewModelFactory;
		this.properties = properties;
	}

	/**
	 * Renders a chart as PNG.
	 *
	 * <p>
	 * A custom size renders a fresh image, but the image map of the page belongs
	 * to the default size - the tool tips would no longer match the bars.
	 * </p>
	 *
	 * @param view
	 *            The view: <code>overview</code>, <code>year</code> or
	 *            <code>month</code>.
	 * @param year
	 *            The year, may be <code>null</code>.
	 * @param month
	 *            The month, may be <code>null</code>.
	 * @param width
	 *            The width of the image, optional.
	 * @param height
	 *            The height of the image, optional.
	 * @return The rendered PNG image.
	 */
	@GetMapping(value = "/chart.png", produces = MediaType.IMAGE_PNG_VALUE)
	public ResponseEntity<byte[]> getChart( //
			@RequestParam(name = "view", required = false) String view, //
			@RequestParam(name = "year", required = false) Integer year, //
			@RequestParam(name = "month", required = false) Integer month, //
			@RequestParam(name = "width", required = false) Integer width, //
			@RequestParam(name = "height", required = false) Integer height) {

		PeriodSelection selection = this.viewModelFactory.resolve(PeriodView.fromCode(view, PeriodView.MONTH), year,
				month);

		byte[] png = this.chartService.render( //
				this.viewModelFactory.getTitle(selection), //
				this.viewModelFactory.getSummary(selection), //
				selection, //
				clamp(width, this.properties.getChartWidth()), //
				clamp(height, this.properties.getChartHeight())) //
				.png();

		return ResponseEntity.ok() //
				.contentType(MediaType.IMAGE_PNG) //
				.cacheControl(CacheControl.noCache()) //
				.body(png);
	}

	/**
	 * @param value
	 *            The requested size, may be <code>null</code>.
	 * @param fallback
	 *            The configured default size.
	 * @return The size to use.
	 */
	private int clamp(Integer value, int fallback) {
		if (value == null) {
			return fallback;
		}
		return Math.min(MAX_SIZE, Math.max(MIN_SIZE, value.intValue()));
	}
}
