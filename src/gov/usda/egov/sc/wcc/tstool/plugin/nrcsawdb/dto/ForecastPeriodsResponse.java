package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The ForecastPeriodsResponse class matches the `forecastPeriods` service response,
 * which is an array of ForecastPeriod.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class ForecastPeriodsResponse {

	/**
	 * List of ForecastPeriod data objects.
	 */
	private List<ForecastPeriod> forecastPeriods = new ArrayList<>();

	/**
	 * Constructor needed by Jackson.
	 */
	public ForecastPeriodsResponse () {
	}

	/**
	 * Return the list of ForecastPeriods objects.
	 * @return the list of ForecastPeriods objects
	 */
	public List<ForecastPeriod> getForecastPeriods () {
		return this.forecastPeriods;
	}
}