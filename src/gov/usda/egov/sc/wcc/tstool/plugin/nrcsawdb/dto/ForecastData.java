
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB Forecast Data objects, for example the 'data' object from:
 * <pre>
[
  {
    "stationTriplet": "09430500:CO:USGS",
    "forecastPointName": "Gila R at Gila",
    "data": [
      {
        "elementCode": "SRVO",
        "forecastPeriod": [
          "04-01",
          "07-31"
        ],
        "forecastStatus": "final",
        "issueDate": "2023-10-02 07:45:52",
        "periodNormal": 13.6,
        "publicationDate": "2023-04-01",
        "unitCode": "kac_ft",
        "forecastValues": {
          "10": 36.5,
          "30": 28.3,
          "50": 24.1
        }
      }
    ]
  }
]
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastData {

	// Alphabetize the elements.

	/**
	 * 'elementCode'
	 */
	private String elementCode = "";

	/**
	 * 'forecastPeriod'
	 */
	private List<String> forecastPeriod = null;

	/**
	 * 'forecastStatus'
	 */
	private String forecastStatus = "";

	/**
	 * 'forecastValues'
	 */
	private Map<String,Double> forecastValues = null;

	/**
	 * 'issueDate'
	 */
	private String issueDate = "";

	/**
	 * 'periodNormal'
	 */
	private Double periodNormal = null;

	/**
	 * 'publicationDate'
	 */
	private String publicationDate = "";

	/**
	 * 'unitCode'
	 */
	private String unitCode = "";

    /**
     * Constructor.
     */
    public ForecastData () {
    }

    /**
     * Gets the element code
     * @return the element code
     */
    public String getElementCode () {
        return this.elementCode;
    }

    /**
     * Gets the exceedence probability values for the foreast.
     * @return the exceedence probability values for the foreast
     */
    public List<Integer> getExceedenceProbabilities() {
    	// Convert the string map keys to integers.
    	List<Integer> probs = new ArrayList<>();
    	for ( Map.Entry<String,Double> entry : this.forecastValues.entrySet() ) {
    		probs.add(Integer.valueOf(entry.getKey()));
    	}
    	// Sort in ascending order.
    	Collections.sort(probs);
    	return probs;
    }

    /**
     * Gets the exceedence values for the forecast.
     * @return the exceedence values for the forecast
     */
	public List<Double> getExceedenceValues() {
    	// First get the keys in sorted order.
    	List<String> keys = new ArrayList<>();
    	for ( Map.Entry<String,Double> entry : this.forecastValues.entrySet() ) {
    		keys.add(entry.getKey());
    	}
    	// Sort in ascending order.
    	Collections.sort(keys);
    	// Get the values corresponding to the keys.
    	List<Double> values = new ArrayList<>();
    	for ( String key : keys ) {
    		values.add ( this.forecastValues.get(key) );
    	}
    	return values;
	}

    /**
     * Gets the forecast period
     * @return the forecast period
     */
    public List<String> getForecastPeriod () {
        return this.forecastPeriod;
    }

    /**
     * Gets the forecast status.
     * @return the forecast status
     */
    public String getForecastStatus () {
        return this.forecastStatus;
    }

    /**
     * Gets the forecast values
     * @return the forecast values
     */
    public Map<String,Double> getForecastValues () {
        return this.forecastValues;
    }

    /**
     * Gets the issue date
     * @return the issue date
     */
    public String getIssueDate () {
        return this.issueDate;
    }

    /**
     * Gets the period normal
     * @return the period normal
     */
    public Double getPeriodNormal () {
        return this.periodNormal;
    }

    /**
     * Gets the publication date
     * @return the publication date
     */
    public String getPublicationDate () {
        return this.publicationDate;
    }

    /**
     * Gets the unit code
     * @return the unit code
     */
    public String getUnitCode () {
        return this.unitCode;
    }

    /**
     * Set the element code
     * @param elementCode element code
     */
    public void setElementCode ( String elementCode ) {
        this.elementCode = elementCode;
    }

    /**
     * Set the forecast period
     * @param forecastPeriod forecast period
     */
    public void setForecastPeriod ( List<String> forecastPeriod ) {
        this.forecastPeriod = forecastPeriod;
    }

    /**
     * Set the forecast status.
     * @param forecastStatus forecast status
     */
    public void setForecastStatus ( String forecastStatus ) {
        this.forecastStatus = forecastStatus;
    }

    /**
     * Set the forecast values.
     * @param forecastValues forecast values
     */
    public void setForecastValues ( Map<String,Double> forecastValues ) {
        this.forecastValues = forecastValues;
    }

    /**
     * Set the issue date
     * @param issueDate issue date
     */
    public void setIssueDate ( String issueDate ) {
        this.issueDate = issueDate;
    }

    /**
     * Set the period normal
     * @param periodNormal period normal
     */
    public void setPeriodNormal ( Double periodNormal ) {
        this.periodNormal = periodNormal;
    }

    /**
     * Set the publication date
     * @param publicationDate publication date
     */
    public void setPublicationDate ( String publicationDate ) {
        this.publicationDate = publicationDate;
    }

    /**
     * Set the unit code
     * @param unitCode unit code
     */
    public void setUnitCode ( String unitCode ) {
        this.unitCode = unitCode;
    }
}