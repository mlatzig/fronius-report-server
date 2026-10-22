package de.latzig.tool.fronius.report.server.chart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jfree.chart.ChartRenderingInfo;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.AbstractCategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Component;

import de.latzig.tool.fronius.report.server.chart.ChartHover.Bounds;
import de.latzig.tool.fronius.report.server.chart.ChartHover.Column;
import de.latzig.tool.fronius.report.server.chart.ChartHover.Row;
import de.latzig.tool.fronius.report.server.i18n.Messages;
import de.latzig.tool.fronius.report.server.model.PeriodSummary;

/**
 * Builds the charts of the energy reports.
 *
 * <p>
 * The look follows the Solar.web presentation: self consumed energy in orange,
 * energy taken from the grid in grey and energy fed into the grid in green. The
 * two lower segments therefore add up to the consumption - where the red line
 * ends - while the orange and the green segment add up to the production.
 * </p>
 *
 * @author Mario Latzig
 */
@Component
public class ChartFactory {

	// consts - colors
	/** Color of the self consumed pv energy. */
	public static final Color COLOR_SELF_CONSUMPTION = new Color(0xF2, 0xA1, 0x00);
	/** Color of the energy fed into the grid. */
	public static final Color COLOR_FEED_IN = new Color(0x7C, 0xB3, 0x42);
	/** Color of the energy taken from the grid. */
	public static final Color COLOR_GRID_SUPPLY = new Color(0x78, 0x90, 0x9C);
	/** Color of the consumption line. */
	public static final Color COLOR_CONSUMPTION = new Color(0xD3, 0x2F, 0x2F);

	// consts - style
	private static final Font FONT_TITLE = new Font(Font.SANS_SERIF, Font.BOLD, 15);
	private static final Font FONT_SUBTITLE = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final Font FONT_LABEL = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
	private static final Font FONT_AXIS = new Font(Font.SANS_SERIF, Font.BOLD, 11);
	private static final Color COLOR_GRID = new Color(0xE2, 0xE6, 0xEA);
	private static final Color COLOR_TEXT = new Color(0x33, 0x3B, 0x45);
	private static final BasicStroke STROKE_GRID = new BasicStroke(0.7f);
	private static final BasicStroke STROKE_LINE = new BasicStroke(2.0f);
	private static final int LABEL_ROTATION_THRESHOLD = 16;
	private static final int ENTITY_RADIUS = 6;

	// consts - series, the keys are also used as css modifiers by the client
	private static final String SERIES_SELF_CONSUMPTION = "chart.series.selfConsumption"; //$NON-NLS-1$
	private static final String SERIES_GRID_SUPPLY = "chart.series.gridSupply"; //$NON-NLS-1$
	private static final String SERIES_FEED_IN = "chart.series.feedIn"; //$NON-NLS-1$
	private static final String SERIES_CONSUMPTION = "chart.series.consumption"; //$NON-NLS-1$
	private static final String SERIES_PRODUCTION = "chart.series.production"; //$NON-NLS-1$
	private static final String KEY_SELF_CONSUMPTION = "self-consumption"; //$NON-NLS-1$
	private static final String KEY_GRID_SUPPLY = "grid-supply"; //$NON-NLS-1$
	private static final String KEY_FEED_IN = "feed-in"; //$NON-NLS-1$
	private static final String KEY_CONSUMPTION = "consumption"; //$NON-NLS-1$
	private static final String KEY_PRODUCTION = "production"; //$NON-NLS-1$

	// fields
	private final Messages messages;

	/**
	 * Constructor.
	 *
	 * @param messages
	 *            The localized texts.
	 */
	public ChartFactory(Messages messages) {
		this.messages = messages;
	}

	/**
	 * Creates the energy balance chart: stacked bars of self consumption, grid
	 * supply and feed in plus the total consumption as a line.
	 *
	 * @param title
	 *            The title of the chart.
	 * @param summary
	 *            The aggregated values of the period, shown as subtitle.
	 * @param rows
	 *            The categories to show.
	 * @return The chart.
	 */
	public JFreeChart createBalanceChart(String title, PeriodSummary summary, List<ChartRow> rows) {
		String seriesSelfConsumption = this.messages.get(SERIES_SELF_CONSUMPTION);
		String seriesGridSupply = this.messages.get(SERIES_GRID_SUPPLY);
		String seriesFeedIn = this.messages.get(SERIES_FEED_IN);
		String seriesConsumption = this.messages.get(SERIES_CONSUMPTION);

		DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
		DefaultCategoryDataset lineDataset = new DefaultCategoryDataset();
		for (ChartRow row : rows) {
			// order matters: the consumption block (self consumption + grid supply)
			// stays at the bottom, the surplus fed into the grid sits on top
			barDataset.addValue(row.selfConsumption(), seriesSelfConsumption, row.label());
			barDataset.addValue(row.gridSupply(), seriesGridSupply, row.label());
			barDataset.addValue(row.feedIn(), seriesFeedIn, row.label());
			lineDataset.addValue(row.consumption(), seriesConsumption, row.label());
		}

		StackedBarRenderer barRenderer = new StackedBarRenderer();
		barRenderer.setBarPainter(new StandardBarPainter());
		barRenderer.setShadowVisible(false);
		barRenderer.setDrawBarOutline(false);
		barRenderer.setSeriesPaint(0, COLOR_SELF_CONSUMPTION);
		barRenderer.setSeriesPaint(1, COLOR_GRID_SUPPLY);
		barRenderer.setSeriesPaint(2, COLOR_FEED_IN);
		barRenderer.setItemMargin(0.0d);

		LineAndShapeRenderer lineRenderer = new LineAndShapeRenderer(true, false);
		lineRenderer.setSeriesPaint(0, COLOR_CONSUMPTION);
		lineRenderer.setSeriesStroke(0, STROKE_LINE);

		// hover texts of the html image map built around the rendered png
		applyToolTips(barRenderer);
		applyToolTips(lineRenderer);

		CategoryPlot plot = new CategoryPlot();
		plot.setDataset(0, barDataset);
		plot.setRenderer(0, barRenderer);
		plot.setDataset(1, lineDataset);
		plot.setRenderer(1, lineRenderer);
		plot.setDomainAxis(createDomainAxis(rows.size()));
		plot.setRangeAxis(createRangeAxis(this.messages.get("chart.axis.energy"))); //$NON-NLS-1$
		plot.setOrientation(PlotOrientation.VERTICAL);
		plot.mapDatasetToRangeAxis(1, 0);
		// a category plot renders its datasets in reverse order by default, the
		// consumption line would end up behind the bars
		plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);
		stylePlot(plot);

		return createChart(title, buildBalanceSubtitle(summary), plot);
	}

	/**
	 * Collects what the client needs to show a tool tip on the rendered chart.
	 *
	 * <p>
	 * The geometry comes from the entities JFreeChart recorded while drawing, so
	 * it always matches the image. Every category becomes a column reaching
	 * halfway to its neighbours: pointing anywhere inside it shows all values of
	 * that category, and the segment under the pointer is marked.
	 * </p>
	 *
	 * @param info
	 *            The rendering info of the drawn chart.
	 * @param rows
	 *            The categories of the chart.
	 * @return The tool tip model of the chart.
	 */
	public ChartHover buildHover(ChartRenderingInfo info, List<ChartRow> rows) {
		Rectangle2D plot = info.getPlotInfo().getDataArea();
		Map<String, Map<String, Rectangle2D>> geometry = collectGeometry(info);

		// the horizontal center of every category, taken from its bar
		Map<String, Double> centers = new LinkedHashMap<String, Double>();
		for (ChartRow row : rows) {
			Rectangle2D bounds = union(geometry.get(row.label()));
			if (bounds != null) {
				centers.put(row.label(), Double.valueOf(bounds.getCenterX()));
			}
		}

		List<Column> columns = new ArrayList<Column>();
		List<String> labels = new ArrayList<String>(centers.keySet());
		for (int i = 0; i < labels.size(); i++) {
			String label = labels.get(i);
			double center = centers.get(label).doubleValue();
			// reach halfway to the neighbours, so the whole plot is covered
			double left = (i > 0) ? (centers.get(labels.get(i - 1)).doubleValue() + center) / 2.0d : plot.getMinX();
			double right = (i + 1 < labels.size()) ? (center + centers.get(labels.get(i + 1)).doubleValue()) / 2.0d
					: plot.getMaxX();
			columns.add(new Column(label, round(left), round(right), buildRows(findRow(rows, label), geometry.get(label))));
		}

		return new ChartHover(new Bounds(round(plot.getMinX()), round(plot.getMinY()), round(plot.getMaxX()),
				round(plot.getMaxY())), columns);
	}

	/**
	 * @param info
	 *            The rendering info of the drawn chart.
	 * @return The bounds of every drawn item, by category and series.
	 */
	private Map<String, Map<String, Rectangle2D>> collectGeometry(ChartRenderingInfo info) {
		Map<String, Map<String, Rectangle2D>> geometry = new HashMap<String, Map<String, Rectangle2D>>();
		EntityCollection entities = info.getEntityCollection();
		if (entities == null) {
			return geometry;
		}
		for (Object entity : entities.getEntities()) {
			if (entity instanceof CategoryItemEntity item && item.getArea() != null) {
				String category = String.valueOf(item.getColumnKey());
				String series = String.valueOf(item.getRowKey());
				geometry.computeIfAbsent(category, key -> new HashMap<String, Rectangle2D>()).put(series,
						item.getArea().getBounds2D());
			}
		}
		return geometry;
	}

	/**
	 * @param row
	 *            The values of the category, may be <code>null</code>.
	 * @param geometry
	 *            The bounds of the drawn segments, may be <code>null</code>.
	 * @return The values of the category, in the order of the stacked bar.
	 */
	private List<Row> buildRows(ChartRow row, Map<String, Rectangle2D> geometry) {
		List<Row> rows = new ArrayList<Row>();
		if (row == null) {
			return rows;
		}
		rows.add(buildRow(KEY_SELF_CONSUMPTION, SERIES_SELF_CONSUMPTION, row.selfConsumption(), geometry));
		rows.add(buildRow(KEY_GRID_SUPPLY, SERIES_GRID_SUPPLY, row.gridSupply(), geometry));
		rows.add(buildRow(KEY_FEED_IN, SERIES_FEED_IN, row.feedIn(), geometry));
		// sums of the segments above, no own bar to point at: the production is
		// self consumption plus feed in, the consumption is the red line
		rows.add(new Row(KEY_PRODUCTION, this.messages.get(SERIES_PRODUCTION), formatEnergy(row.production()), null,
				null));
		rows.add(new Row(KEY_CONSUMPTION, this.messages.get(SERIES_CONSUMPTION), formatEnergy(row.consumption()), null,
				null));
		return rows;
	}

	/**
	 * @param key
	 *            The technical key of the value.
	 * @param messageKey
	 *            The message key of the label.
	 * @param value
	 *            The value in kWh.
	 * @param geometry
	 *            The bounds of the drawn segments, may be <code>null</code>.
	 * @return The value of the category.
	 */
	private Row buildRow(String key, String messageKey, double value, Map<String, Rectangle2D> geometry) {
		String label = this.messages.get(messageKey);
		Rectangle2D bounds = (geometry != null) ? geometry.get(label) : null;
		Integer y1 = (bounds != null) ? Integer.valueOf(round(bounds.getMinY())) : null;
		Integer y2 = (bounds != null) ? Integer.valueOf(round(bounds.getMaxY())) : null;
		return new Row(key, label, formatEnergy(value), y1, y2);
	}

	/**
	 * @param rows
	 *            The categories of the chart.
	 * @param label
	 *            The label of the wanted category.
	 * @return The category or <code>null</code>.
	 */
	private ChartRow findRow(List<ChartRow> rows, String label) {
		for (ChartRow row : rows) {
			if (row.label().equals(label)) {
				return row;
			}
		}
		return null;
	}

	/**
	 * @param bounds
	 *            The bounds to combine, may be <code>null</code>.
	 * @return The enclosing rectangle or <code>null</code>.
	 */
	private Rectangle2D union(Map<String, Rectangle2D> bounds) {
		Rectangle2D result = null;
		if (bounds != null) {
			for (Rectangle2D single : bounds.values()) {
				result = (result == null) ? (Rectangle2D) single.clone() : result.createUnion(single);
			}
		}
		return result;
	}

	/**
	 * @param value
	 *            The value in kWh.
	 * @return The localized value including its unit.
	 */
	private String formatEnergy(double value) {
		return format(value, 1) + " " + this.messages.get("unit.kwh"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * @param value
	 *            The value to round.
	 * @return The nearest integer.
	 */
	private int round(double value) {
		return (int) Math.round(value);
	}

	/**
	 * Gives every item of a renderer a tool tip and a dummy url.
	 *
	 * <p>
	 * The url is needed because an <code>area</code> without <code>href</code> is
	 * not hit tested by the browsers, its tool tip would never show up. The thin
	 * client swallows the click, without javascript it is a jump to the top of the
	 * page.
	 * </p>
	 *
	 * @param renderer
	 *            The renderer to equip.
	 */
	private void applyToolTips(AbstractCategoryItemRenderer renderer) {
		NumberFormat valueFormat = NumberFormat.getNumberInstance(this.messages.getLocale());
		valueFormat.setMinimumFractionDigits(1);
		valueFormat.setMaximumFractionDigits(1);
		renderer.setDefaultToolTipGenerator(
				new StandardCategoryToolTipGenerator(this.messages.get("chart.tooltip"), valueFormat)); //$NON-NLS-1$
		renderer.setDefaultItemURLGenerator((dataset, series, item) -> "#"); //$NON-NLS-1$
		// a bar is its own hot spot, a point of the line only gets this radius -
		// the default of three pixel is barely hittable without javascript
		renderer.setDefaultEntityRadius(ENTITY_RADIUS);
	}

	/**
	 * @param summary
	 *            The aggregated values of the shown period.
	 * @return The subtitle of the energy balance chart.
	 */
	private String buildBalanceSubtitle(PeriodSummary summary) {
		return this.messages.get("chart.subtitle.balance", //$NON-NLS-1$
				format(summary.getProduction(), 0), format(summary.getConsumption(), 0),
				format(summary.getFeedIn(), 0), format(summary.getGridSupply(), 0), format(summary.getAutarky(), 1),
				format(summary.getSelfConsumptionRate(), 1));
	}

	/**
	 * Wraps the plot into a styled chart.
	 *
	 * @param title
	 *            The title of the chart.
	 * @param subtitle
	 *            The subtitle of the chart, may be <code>null</code>.
	 * @param plot
	 *            The plot to wrap.
	 * @return The chart.
	 */
	private JFreeChart createChart(String title, String subtitle, CategoryPlot plot) {
		JFreeChart chart = new JFreeChart(title, FONT_TITLE, plot, true);
		chart.setBackgroundPaint(Color.WHITE);
		chart.setBorderVisible(false);
		chart.setPadding(new RectangleInsets(8.0d, 8.0d, 8.0d, 12.0d));
		chart.getTitle().setPaint(COLOR_TEXT);
		if (subtitle != null && subtitle.trim().length() > 0) {
			TextTitle textTitle = new TextTitle(subtitle, FONT_SUBTITLE);
			textTitle.setPaint(COLOR_TEXT);
			chart.addSubtitle(textTitle);
		}
		chart.getLegend().setFrame(BlockBorder.NONE);
		chart.getLegend().setBackgroundPaint(Color.WHITE);
		chart.getLegend().setItemFont(FONT_LABEL);
		chart.getLegend().setItemPaint(COLOR_TEXT);
		return chart;
	}

	/**
	 * Applies the common styling to a plot.
	 *
	 * @param plot
	 *            The plot to style.
	 */
	private void stylePlot(CategoryPlot plot) {
		plot.setBackgroundPaint(Color.WHITE);
		plot.setOutlineVisible(false);
		plot.setDomainGridlinesVisible(false);
		plot.setRangeGridlinesVisible(true);
		plot.setRangeGridlinePaint(COLOR_GRID);
		plot.setRangeGridlineStroke(STROKE_GRID);
		plot.setAxisOffset(new RectangleInsets(2.0d, 2.0d, 2.0d, 2.0d));
	}

	/**
	 * @param categoryCount
	 *            The number of categories, used to decide the label rotation.
	 * @return The styled domain axis.
	 */
	private CategoryAxis createDomainAxis(int categoryCount) {
		CategoryAxis axis = new CategoryAxis();
		axis.setTickLabelFont(FONT_LABEL);
		axis.setTickLabelPaint(COLOR_TEXT);
		axis.setAxisLinePaint(COLOR_GRID);
		axis.setTickMarksVisible(false);
		axis.setLowerMargin(0.01d);
		axis.setUpperMargin(0.01d);
		axis.setCategoryMargin(0.15d);
		if (categoryCount > LABEL_ROTATION_THRESHOLD) {
			axis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
		}
		return axis;
	}

	/**
	 * @param label
	 *            The label of the axis.
	 * @return The styled range axis.
	 */
	private NumberAxis createRangeAxis(String label) {
		NumberAxis axis = new NumberAxis(label);
		axis.setLabelFont(FONT_AXIS);
		axis.setLabelPaint(COLOR_TEXT);
		axis.setTickLabelFont(FONT_LABEL);
		axis.setTickLabelPaint(COLOR_TEXT);
		axis.setAxisLinePaint(COLOR_GRID);
		axis.setTickMarksVisible(false);
		axis.setNumberFormatOverride(NumberFormat.getNumberInstance(this.messages.getLocale()));
		return axis;
	}

	/**
	 * @param value
	 *            The value to format.
	 * @param fractionDigits
	 *            The number of fraction digits.
	 * @return The localized representation of the value.
	 */
	private String format(double value, int fractionDigits) {
		Locale locale = this.messages.getLocale();
		NumberFormat format = NumberFormat.getNumberInstance(locale);
		format.setMinimumFractionDigits(fractionDigits);
		format.setMaximumFractionDigits(fractionDigits);
		return format.format(value);
	}
}
