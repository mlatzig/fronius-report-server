package de.latzig.tool.fronius.report.server.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.StandardEntityCollection;
import org.jfree.chart.imagemap.ImageMapUtils;
import org.springframework.stereotype.Service;

import de.latzig.tool.fronius.report.server.chart.ChartFactory;
import de.latzig.tool.fronius.report.server.chart.ChartHover;
import de.latzig.tool.fronius.report.server.chart.ChartRow;
import de.latzig.tool.fronius.report.server.model.PeriodSelection;
import de.latzig.tool.fronius.report.server.model.PeriodSummary;

/**
 * Renders a chart into a png and the matching html image map.
 *
 * <p>
 * Both belong together: the map holds one area per bar, carrying the tool tip
 * of that value. The map is needed while the page is being built, the png only
 * with the following request of the browser - therefore the result of a
 * rendering is kept for a short while.
 * </p>
 *
 * <p>
 * The cache key contains the values of the chart, so a rescan that changed a
 * report automatically leads to a new rendering.
 * </p>
 *
 * @author Mario Latzig
 */
@Service
public class ChartService {

	// consts
	/** Name of the html image map belonging to the chart. */
	public static final String MAP_NAME = "chart-map"; //$NON-NLS-1$
	private static final int CACHE_SIZE = 16;

	// fields
	private final ChartDataService dataService;
	private final ChartFactory chartFactory;
	private final Map<String, RenderedChart> cache = Collections
			.synchronizedMap(new LruMap<String, RenderedChart>(CACHE_SIZE));

	/**
	 * Constructor.
	 *
	 * @param dataService
	 *            The chart data service.
	 * @param chartFactory
	 *            The chart factory.
	 */
	public ChartService(ChartDataService dataService, ChartFactory chartFactory) {
		this.dataService = dataService;
		this.chartFactory = chartFactory;
	}

	/**
	 * Renders the energy balance chart of a period.
	 *
	 * @param title
	 *            The title of the chart.
	 * @param summary
	 *            The aggregated values of the period, shown as subtitle.
	 * @param selection
	 *            The resolved selection.
	 * @param width
	 *            The width of the image.
	 * @param height
	 *            The height of the image.
	 * @return The rendered chart.
	 */
	public RenderedChart render(String title, PeriodSummary summary, PeriodSelection selection, int width,
			int height) {
		List<ChartRow> rows = this.dataService.getRows(selection);
		String key = String.format("%s|%dx%d|%d|%d", selection.chartQuery(), Integer.valueOf(width), //$NON-NLS-1$
				Integer.valueOf(height), Integer.valueOf(title.hashCode()), Integer.valueOf(rows.hashCode()));

		RenderedChart cached = this.cache.get(key);
		if (cached != null) {
			return cached;
		}

		JFreeChart chart = this.chartFactory.createBalanceChart(title, summary, rows);
		ChartRenderingInfo info = new ChartRenderingInfo(new StandardEntityCollection());
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			ChartUtils.writeChartAsPNG(os, chart, width, height, info);
			RenderedChart rendered = new RenderedChart(os.toByteArray(), ImageMapUtils.getImageMap(MAP_NAME, info),
					this.chartFactory.buildHover(info, rows), width, height);
			this.cache.put(key, rendered);
			return rendered;
		} catch (IOException e) {
			throw new UncheckedIOException("Chart could not be rendered.", e); //$NON-NLS-1$
		}
	}

	/**
	 * A rendered chart.
	 *
	 * @param png
	 *            The image.
	 * @param imageMap
	 *            The html image map holding one area per value, the fallback for
	 *            a client without javascript.
	 * @param hover
	 *            The tool tip model used by the thin client.
	 * @param width
	 *            The width of the image.
	 * @param height
	 *            The height of the image.
	 */
	public record RenderedChart(byte[] png, String imageMap, ChartHover hover, int width, int height) {
	}

	/**
	 * A map dropping the least recently used entry once it is full.
	 *
	 * @param <K>
	 *            The type of the keys.
	 * @param <V>
	 *            The type of the values.
	 */
	private static final class LruMap<K, V> extends LinkedHashMap<K, V> {

		// consts
		private static final long serialVersionUID = 1L;

		// fields
		private final int maxSize;

		/**
		 * Constructor.
		 *
		 * @param maxSize
		 *            The maximal number of entries.
		 */
		LruMap(int maxSize) {
			super(16, 0.75f, true);
			this.maxSize = maxSize;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
			return size() > this.maxSize;
		}
	}
}
