/*
 * Fronius Report Server - thin client.
 *
 * The page is rendered completely on the server and works without this file.
 * If javascript is available, changing the selection no longer reloads the
 * page: the view model is fetched from /api/report and the parts that changed
 * are replaced. All texts and numbers arrive ready formatted from the server,
 * so this client stays free of any locale knowledge. The chart itself stays a
 * server rendered png, only its src is swapped.
 */
(function () {
	'use strict';

	var API = '/api/report';

	/** Distance of the tooltip from the pointer. */
	var OFFSET_X = 14;
	var OFFSET_Y = 16;


	var form = document.getElementById('selector');
	var submit = document.getElementById('selector-submit');
	var selectYear = document.getElementById('select-year');
	var selectMonth = document.getElementById('select-month');
	var content = document.getElementById('content');
	var pageTitle = document.getElementById('page-title');
	var pageHint = document.getElementById('page-hint');
	var cards = document.getElementById('cards');
	var chartBalance = document.getElementById('chart-balance');
	var chartMapHolder = document.getElementById('chart-map-holder');
	var chartFrame = chartBalance ? chartBalance.parentNode : null;
	var tooltip = null;
	var chartHover = null;

	if (!form || !content) {
		return;
	}

	// without javascript the submit button is needed, with javascript it is not
	if (submit) {
		submit.hidden = true;
	}

	/** @return the currently selected view. */
	function selectedView() {
		var checked = form.querySelector('input[name="view"]:checked');
		return checked ? checked.value : 'month';
	}

	/** @return the query string for the current selection. */
	function buildQuery(view, year, month) {
		var params = new URLSearchParams();
		params.set('view', view);
		if (view !== 'overview' && year) {
			params.set('year', year);
		}
		if (view === 'month' && month) {
			params.set('month', month);
		}
		return params.toString();
	}

	/** Enables or disables the selects that do not apply to the view. */
	function applyEnabledState(view) {
		selectYear.disabled = (view === 'overview');
		selectMonth.disabled = (view !== 'month');
	}

	/** Rebuilds the month options of the currently selected year. */
	function fillMonths(months, selected) {
		selectMonth.innerHTML = '';
		months.forEach(function (month) {
			var option = document.createElement('option');
			option.value = month.value;
			option.textContent = month.label;
			option.selected = (month.value === selected);
			selectMonth.appendChild(option);
		});
	}

	/** Rebuilds the key figures. */
	function fillCards(metrics) {
		cards.innerHTML = '';
		metrics.forEach(function (metric) {
			var card = document.createElement('article');
			card.className = 'card';
			card.setAttribute('data-key', metric.key);

			var label = document.createElement('div');
			label.className = 'card-label';
			label.textContent = metric.label;
			card.appendChild(label);

			var value = document.createElement('div');
			value.className = 'card-value';
			var number = document.createElement('span');
			number.className = 'card-number';
			number.textContent = metric.value;
			var unit = document.createElement('span');
			unit.className = 'card-unit';
			unit.textContent = metric.unit;
			value.appendChild(number);
			value.appendChild(document.createTextNode(' '));
			value.appendChild(unit);
			card.appendChild(value);

			if (metric.percent !== null && metric.percent !== undefined) {
				var meter = document.createElement('div');
				meter.className = 'meter';
				var fill = document.createElement('div');
				fill.className = 'meter-fill';
				fill.style.width = metric.percent.toFixed(1) + '%';
				meter.appendChild(fill);
				card.appendChild(meter);
			}
			cards.appendChild(card);
		});
	}

	/** Applies a view model delivered by the server. */
	function apply(model) {
		pageTitle.textContent = model.title;
		pageHint.textContent = model.hint;
		fillCards(model.metrics);
		chartBalance.src = '/chart.png?' + model.chartQuery;
		// the areas of the image map belong to the new bars, they must be
		// replaced together with the image
		hideTooltip();
		readHoverModel(model.chartHover);
		if (chartMapHolder) {
			chartMapHolder.innerHTML = model.chartImageMap;
		}

		var radio = document.getElementById('view-' + model.view);
		if (radio) {
			radio.checked = true;
		}
		if (model.year) {
			selectYear.value = String(model.year);
		}
		fillMonths(model.months, model.month);
		applyEnabledState(model.view);

		document.title = document.title.split(' · ')[0] + ' · ' + model.title;
	}

	/*
	 * ---------------------------------------------------------------------
	 * Tool tips of the chart.
	 *
	 * The server delivers all values of every category together with the
	 * geometry of the drawn segments. Pointing anywhere inside a column shows
	 * the whole category at once and marks the segment under the pointer -
	 * no pixel hunting on a single bar segment.
	 *
	 * Without javascript the image map of the chart takes over and the browser
	 * shows its native tool tip of a single segment.
	 * ---------------------------------------------------------------------
	 */

	/** Takes the tool tips over from the browser and stores the model. */
	function readHoverModel(model) {
		if (model) {
			chartHover = model;
		} else {
			var node = document.getElementById('chart-hover');
			try {
				chartHover = node ? JSON.parse(node.textContent) : null;
			} catch (error) {
				chartHover = null;
			}
		}
		// the image map is only the fallback, it would add a second, delayed
		// tool tip on top of ours
		if (chartBalance) {
			chartBalance.removeAttribute('usemap');
		}
	}

	/** @return the category at the given position of the image, or null. */
	function findColumn(x, y) {
		if (!chartHover || !chartHover.plot || !chartHover.columns) {
			return null;
		}
		var plot = chartHover.plot;
		if (x < plot.x1 || x > plot.x2 || y < plot.y1 || y > plot.y2) {
			return null;
		}
		for (var i = 0; i < chartHover.columns.length; i++) {
			if (x >= chartHover.columns[i].x1 && x <= chartHover.columns[i].x2) {
				return chartHover.columns[i];
			}
		}
		return null;
	}

	/** @return the tooltip element, created on first use. */
	function getTooltip() {
		if (!tooltip) {
			tooltip = document.createElement('div');
			tooltip.className = 'chart-tooltip';
			tooltip.hidden = true;
			chartFrame.appendChild(tooltip);
		}
		return tooltip;
	}

	/**
	 * @return the index of the segment at the given height, or -1.
	 *
	 * Neighbouring segments share their border, so the interval is half open:
	 * a border belongs to the segment below it and exactly one row matches. A
	 * segment of height zero can never be pointed at, which is fine - it is
	 * invisible in the chart as well. The consumption line carries no geometry
	 * at all and is therefore never marked.
	 */
	function findCurrentRow(column, y) {
		for (var i = 0; i < column.rows.length; i++) {
			var row = column.rows[i];
			if (row.y1 !== null && row.y1 !== undefined && y >= row.y1 && y < row.y2) {
				return i;
			}
		}
		return -1;
	}

	/** Fills the tooltip with every value of the category. */
	function fillTooltip(element, column, y) {
		element.textContent = '';

		var title = document.createElement('div');
		title.className = 'chart-tooltip-title';
		title.textContent = column.title;
		element.appendChild(title);

		var current = findCurrentRow(column, y);
		column.rows.forEach(function (row, index) {
			var line = document.createElement('div');
			line.className = 'chart-tooltip-row';
			line.setAttribute('data-key', row.key);
			if (index === current) {
				line.className += ' is-current';
			}
			var label = document.createElement('span');
			label.className = 'chart-tooltip-label';
			label.textContent = row.label;
			var value = document.createElement('span');
			value.className = 'chart-tooltip-value';
			value.textContent = row.value;
			line.appendChild(label);
			line.appendChild(value);
			element.appendChild(line);
		});
	}

	/** Shows the tooltip of the category next to the pointer. */
	function showTooltip(event, column, y) {
		var element = getTooltip();
		fillTooltip(element, column, y);
		element.hidden = false;

		// position of the pointer inside the frame, scrolling included
		var frameRect = chartFrame.getBoundingClientRect();
		var pointerX = event.clientX - frameRect.left + chartFrame.scrollLeft;
		var pointerY = event.clientY - frameRect.top + chartFrame.scrollTop;

		// the image is the boundary; the frame itself must not be used for it,
		// because the tooltip is part of its scroll size and would push it
		var minX = chartBalance.offsetLeft;
		var maxX = minX + chartBalance.offsetWidth;
		var minY = chartBalance.offsetTop;
		var maxY = minY + chartBalance.offsetHeight;

		// place it beside the pointer, flip to the other side at an edge
		var x = pointerX + OFFSET_X;
		if (x + element.offsetWidth > maxX) {
			x = pointerX - OFFSET_X - element.offsetWidth;
		}
		var y = pointerY + OFFSET_Y;
		if (y + element.offsetHeight > maxY) {
			y = pointerY - OFFSET_Y - element.offsetHeight;
		}

		element.style.left = Math.max(minX, x) + 'px';
		element.style.top = Math.max(minY, y) + 'px';
	}

	/** Hides the tooltip. */
	function hideTooltip() {
		if (tooltip) {
			tooltip.hidden = true;
		}
	}

	if (chartBalance && chartFrame) {
		chartBalance.addEventListener('mousemove', function (event) {
			var rect = chartBalance.getBoundingClientRect();
			if (!rect.width || !rect.height) {
				return;
			}
			// the image is shown unscaled, the factors are a safety net
			var x = (event.clientX - rect.left) * (chartBalance.naturalWidth / rect.width);
			var y = (event.clientY - rect.top) * (chartBalance.naturalHeight / rect.height);
			var column = findColumn(x, y);
			if (column) {
				showTooltip(event, column, y);
			} else {
				hideTooltip();
			}
		});
		chartBalance.addEventListener('mouseleave', hideTooltip);
		readHoverModel(null);
	}

	/** Loads a selection from the server. */
	function load(view, year, month, push) {
		content.classList.add('is-loading');
		fetch(API + '?' + buildQuery(view, year, month), {
			headers: { 'Accept': 'application/json' }
		}).then(function (response) {
			if (!response.ok) {
				throw new Error('HTTP ' + response.status);
			}
			return response.json();
		}).then(function (model) {
			apply(model);
			if (push) {
				window.history.pushState({
					view: model.view, year: model.year, month: model.month
				}, '', model.path);
			}
		}).catch(function (error) {
			// fall back to a full page load, the server renders the error page
			window.console && window.console.warn('Reload after error:', error);
			window.location.href = '/select?' + buildQuery(view, year, month);
		}).finally(function () {
			content.classList.remove('is-loading');
		});
	}

	/** Loads whatever is currently selected in the form. */
	function loadSelection() {
		var view = selectedView();
		applyEnabledState(view);
		load(view, selectYear.value, selectMonth.value, true);
	}

	form.addEventListener('change', function (event) {
		if (event.target.name === 'view' || event.target.name === 'year'
			|| event.target.name === 'month') {
			loadSelection();
		}
	});

	form.addEventListener('submit', function (event) {
		event.preventDefault();
		loadSelection();
	});

	window.addEventListener('popstate', function (event) {
		var state = event.state;
		if (state) {
			load(state.view, state.year, state.month, false);
		} else {
			window.location.reload();
		}
	});
})();
