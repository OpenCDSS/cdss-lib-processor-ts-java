
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB Forecast Data objects, for example the 'data' array from:
 * <pre>
[
  {
    "stationTriplet": "302:OR:SNTL",
    "data": [
      {
        "stationElement": {
          "elementCode": "WTEQ",
          "ordinal": 1,
          "heightDepth": null,
          "durationName": "DAILY",
          "dataPrecision": 2,
          "storedUnitCode": "in",
          "originalUnitCode": "in",
          "beginDate": "1980-10-30 15:53",
          "endDate": "2100-01-01",
          "derivedData": false
        },
        "timingCentralTendencies": {
          "medianPeak": {
            "month": 1,
            "day": 1,
            "value": 23.2
          },
          "medianOnset": {
            "month": 1,
            "day": 1
          },
          "medianMeltout": {
            "month": 1,
            "day": 1
          },
          "averagePeak": {
            "month": 1,
            "day": 1,
            "value": 23.2
          },
          "averageOnset": {
            "month": 1,
            "day": 1
          },
          "averageMeltout": {
            "month": 1,
            "day": 1
          }
        },
        "values": [
          {
            "date": "2022-01-01",
            "month": 1,
            "monthPart": "1",
            "year": 2022,
            "collectionDate": "2021-12-31",
            "value": 1.2,
            "qcFlag": "E",
            "qaFlag": "A",
            "origValue": 1.3,
            "origQcFlag": "V",
            "average": 1.4,
            "median": 1
          }
        ],
        "error": "Unsupported operation - the insertOrUpdateBeginDate parameter is not supported for derived data (NOTE: Only included when there is an error)."
      }
    ]
  }
]
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Data {

	// Alphabetize the elements.

	/**
	 * 'values
	 */
	private List<DataValue> values = null;

	/**
	 * Error string.
	 */
	private String error = "";

	/**
	 * 'stationElement'.
	 */
	private StationElement stationElement = null;

	/**
	 * 'timingCentralTentencies'
	 */
	private Map<String,TimingCentralTendency> timingCentralTendencies = null;

    /**
     * Constructor.
     */
    public Data () {
    }

    /**
     * Gets the error
     * @return the error
     */
    public String getError() {
        return this.error;
    }

    /**
     * Gets station element
     * @return the station element
     */
    public StationElement getStationElement() {
        return this.stationElement;
    }

    /**
     * Gets the timing central tendencies map
     * @return the timing central tendencies
     */
    public Map<String,TimingCentralTendency> getTimingCentralTendencies () {
        return this.timingCentralTendencies;
    }

    /**
     * Gets the values 
     * @return the values
     */
    public List<DataValue> getValues () {
        return this.values;
    }

    /**
     * Set the error
     * @param error error
     */
    public void setError ( String error ) {
        this.error = error;
    }

    /**
     * Set the station element
     * @param stationElement station element
     */
    public void setStationElement ( StationElement stationElement ) {
        this.stationElement = stationElement;
    }

    /**
     * Set the timing central tendencies
     * @param stationTriplet timing central tendencies
     */
    public void setTimingCentralTendencies ( Map<String,TimingCentralTendency> timingCentralTendencies ) {
        this.timingCentralTendencies = timingCentralTendencies;
    }

    /**
     * Set the values
     * @param values values
     */
    public void setValues ( List<DataValue> values ) {
        this.values = values;
    }

}