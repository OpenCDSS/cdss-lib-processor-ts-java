
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB TimingCentralTendency objects, for example from the following.
 * The object may or may not have a 'value' but for now always include.
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
public class TimingCentralTendency {

	// Alphabetize the elements.

	/**
	 * 'day'
	 */
	private Integer day = null;

	/**
	 * 'month'
	 */
	private Integer month = null;

	/**
	 * 'value'
	 */
	private Double value = null;

    /**
     * Constructor.
     */
    public TimingCentralTendency () {
    }

    /**
     * Gets the day.
     * @return the day
     */
    public Integer getDay() {
        return this.day;
    }

    /**
     * Gets the month.
     * @return the month
     */
    public Integer getMonth() {
        return this.month;
    }

    /**
     * Gets the value.
     * @return the value
     */
    public Double getValue() {
        return this.value;
    }

    /**
     * Set the day
     * @param day day
     */
    public void setDay ( Integer day ) {
        this.day = day;
    }

    /**
     * Set the month
     * @param month month
     */
    public void setMonth ( Integer month ) {
        this.month = month;
    }

    /**
     * Set the value
     * @param value value
     */
    public void setValue ( Double value ) {
        this.value = value;
    }
}