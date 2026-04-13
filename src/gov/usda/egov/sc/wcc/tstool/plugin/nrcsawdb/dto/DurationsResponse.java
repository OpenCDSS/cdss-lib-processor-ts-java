package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The DurationsResponse class matches the `durations` service response,
 * which is an array of Duration.
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class DurationsResponse {

	/**
	 * List of Duration data objects.
	 */
	private List<Duration> durations = new ArrayList<>();

	/**
	 * Constructor needed by Jackson.
	 */
	public DurationsResponse () {
	}

	/**
	 * Return the list of Duration objects.
	 * @return the list of Duration objects
	 */
	public List<Duration> getDurations () {
		return this.durations;
	}
}