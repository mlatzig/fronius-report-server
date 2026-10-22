package de.latzig.tool.fronius.report.server.model;

import java.util.Locale;

/**
 * The period a report page is showing.
 *
 * @author Mario Latzig
 */
public enum PeriodView {

	/** All years, one category per year. */
	OVERVIEW,

	/** One year, one category per month. */
	YEAR,

	/** One month, one category per day. */
	MONTH;

	/**
	 * @return The lower case code used inside URLs.
	 */
	public String code() {
		return name().toLowerCase(Locale.ROOT);
	}

	/**
	 * @param code
	 *            The code of the view, case insensitive, may be <code>null</code>.
	 * @param fallback
	 *            The view to use if the code is unknown.
	 * @return The matching view.
	 */
	public static PeriodView fromCode(String code, PeriodView fallback) {
		if (code != null) {
			for (PeriodView view : values()) {
				if (view.code().equalsIgnoreCase(code.trim())) {
					return view;
				}
			}
		}
		return fallback;
	}
}
