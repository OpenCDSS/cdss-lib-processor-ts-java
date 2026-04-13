
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB forecast point objects, for example:
 * <pre>
   "forecastPoint": {
      "name": "Alamosa Ck ab Terrace Reservoir",
      "forecaster": "agoodbody",
      "exceedenceProbabilities": [
        10,
        30,
        50,
        70,
        90
      ]
    },
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastPoint {

	// Alphabetize the items.

	/**
	 * Exceedence probabilities (note the different spelling from typical "exceedance").
	 */
	private List<Integer> exceedenceProbabilities = null;

	/**
	 * Forecaster (e.g., "agoodbody").
	 */
	private String forecaster = "";

    /**
     * Name (e.g., "Alamosa Ck ab Terrace Reservoir").
     */
    private String name = "";

    /**
     * Constructor.
     */
    public ForecastPoint () {
    }

    /**
     * Gets the exceedence probabilities.
     * @return the description
     */
    public List<Integer> getExceedenceProbabilities() {
        return this.exceedenceProbabilities;
    }

    /**
     * Gets the forecaster.
     * @return the forecaster
     */
    public String getForecaster() {
        return this.forecaster;
    }

    /**
     * Gets the name.
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the forecaster
     * @param forecaster forecaster
     */
    public void setForecaster ( String forecaster ) {
        this.forecaster = forecaster;
    }

    /**
     * Set the exceedence probabilities.
     * @param exceedenceProbabilities exceedence probabilities
     */
    public void setExceedenceProbabilities ( List<Integer> exceedenceProbabilities ) {
        this.exceedenceProbabilities = exceedenceProbabilities;
    }

    /**
     * Set the name.
     * @param name forecast name
     */
    public void setName ( String name ) {
        this.name = name;
    }

}