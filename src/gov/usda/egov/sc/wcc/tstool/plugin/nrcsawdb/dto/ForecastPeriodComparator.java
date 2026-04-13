package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.Comparator;

/**
 * Comparator for Collections.sort to sort ForecastPeriods, currently by name.
 */
public class ForecastPeriodComparator implements Comparator<ForecastPeriod> {

	/**
	 * Constructor.
	 */
	public ForecastPeriodComparator () {
	}
	
	/**
	 * If forecastPeriodA is < forecastPeriodB, return -1.
	 * If forecastPeriodA = forecastPeriodB, return 0.
	 * If forecastPeriodA is > forecastPeriodB, return 1
	 */
	public int compare(ForecastPeriod forecastPeriodA, ForecastPeriod forecastPeriodB) {
		String nameA = forecastPeriodA.getName();
		String nameB = forecastPeriodB.getName();

		return nameA.compareTo(nameB);
	}
}