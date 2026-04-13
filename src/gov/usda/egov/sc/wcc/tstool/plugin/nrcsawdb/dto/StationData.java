
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB Station Data objects, for example the top-level object from:
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
public class StationData {

	// Alphabetize the elements.

	/**
	 * Station triplet.
	 */
	private String stationTriplet = "";

	/**
	 * 'data'
	 */
	private List<Data> data = null;

    /**
     * Constructor.
     */
    public StationData () {
    }

    /**
     * Gets the data list
     * @return the data list
     */
    public List<Data> getData () {
        return this.data;
    }

    /**
     * Gets the station triplet
     * @return the station triplet
     */
    public String getStationTriplet() {
        return this.stationTriplet;
    }

    /**
     * Set the data
     * @param data data
     */
    public void setData ( List<Data> data ) {
        this.data = data;
    }

    /**
     * Set the station triplet
     * @param stationTriplet station triplet
     */
    public void setStationTriplet ( String stationTriplet ) {
        this.stationTriplet = stationTriplet;
    }

}