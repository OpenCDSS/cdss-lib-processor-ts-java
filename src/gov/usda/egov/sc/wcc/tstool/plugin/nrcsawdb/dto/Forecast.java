
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB Forecast objects, for example from:
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
public class Forecast {

	// Alphabetize the elements.

	/**
	 * Data.
	 */
	private List<ForecastData> data = null;

	/**
	 * Forecast point name.
	 */
	private String forecastPointName = "";

	/**
	 * Station triplet.
	 */
	private String stationTriplet = "";

    /**
     * Constructor.
     */
    public Forecast () {
    }

    /**
     * Gets the forecast data.
     * @return the forecast data
     */
    public List<ForecastData> getData() {
        return this.data;
    }

    /**
     * Gets the forecast point name.
     * @return the forecast point name
     */
    public String getForecastPointName() {
        return this.forecastPointName;
    }

    /**
     * Gets the station triplet.
     * @return the station triple
     */
    public String getStationTriplet() {
        return this.stationTriplet;
    }

    /**
     * Set the data
     * @param data data
     */
    public void setData ( List<ForecastData> data ) {
        this.data = data;
    }

    /**
     * Set the forecast point name
     * @param forecastPointName forecast point name
     */
    public void setForecastPointName ( String forecastPointName ) {
        this.forecastPointName = forecastPointName;
    }

    /**
     * Set the station triplet
     * @param stationTriplet station triplet
     */
    public void setStationTriplet ( String stationTriplet ) {
        this.stationTriplet = stationTriplet;
    }

}