package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.Comparator;

/**
 * Comparator for Collections.sort to sort Forecast, currently by forecast point name.
 */
public class ForecastComparator implements Comparator<Forecast> {

	/**
	 * Constructor.
	 */
	public ForecastComparator () {
	}
	
	/**
	 * If forecastA is < forecastB, return -1.
	 * If forecastA = forecastB, return 0.
	 * If forecastA is > forecastB, return 1
	 */
	public int compare(Forecast forecastA, Forecast forecastB) {
		String nameA = forecastA.getForecastPointName();
		String nameB = forecastB.getForecastPointName();

		return nameA.compareToIgnoreCase(nameB);
	}
}