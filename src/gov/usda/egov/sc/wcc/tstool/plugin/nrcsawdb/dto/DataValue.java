
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB DataValue objects, for example from 'values' below:
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
public class DataValue {

	// Alphabetize the elements.

	/**
	 * 'average' - for example, when daily data is requested, the long-term average for the day
	 */
	private Double average = null;

	/**
	 * 'collectionDate'.
	 */
	private String collectionDate = "";

	/**
	 * 'date'.
	 */
	private String date = "";

	/**
	 * 'median' - for example, when daily data is requested, the long-term median for the day
	 */
	private Double median = null;

	/**
	 * 'month'.
	 */
	private Integer month = null;

	/**
	 * 'monthPart'.
	 */
	private String monthPart = "";

	/**
	 * 'origQcFlag'.
	 */
	private String origQcFlag = "";

	/**
	 * 'origValue'.
	 */
	private Double origValue = null;

	/**
	 * 'qaFlag'.
	 */
	private String qaFlag = "";

	/**
	 * 'qcFlag'.
	 */
	private String qcFlag = "";

	/**
	 * 'value'.
	 */
	private Double value = null;

	/**
	 * 'year'.
	 */
	private Integer year = null;

    /**
     * Constructor.
     */
    public DataValue () {
    }

    /**
     * Gets the average
     * @return the average
     */
    public Double getAverage() {
        return this.average;
    }

    /**
     * Gets the collection date.
     * @return the collection date
     */
    public String getCollectionDate() {
        return this.collectionDate;
    }

    /**
     * Gets the date.
     * @return the date
     */
    public String getDate() {
        return this.date;
    }

    /**
     * Gets the median
     * @return the median
     */
    public Double getMedian() {
        return this.median;
    }

    /**
     * Gets the month
     * @return the month
     */
    public Integer getMonth() {
        return this.month;
    }

    /**
     * Gets the monthPart
     * @return the monthPart
     */
    public String getMonthPart() {
        return this.monthPart;
    }

    /**
     * Gets the origQcFlag.
     * @return the origQcFlag
     */
    public String getOrigQcFlag() {
        return this.origQcFlag;
    }

    /**
     * Gets the origValue
     * @return the origValue
     */
    public Double getOrigValue() {
        return this.origValue;
    }

    /**
     * Gets the qaFlag.
     * @return the qaFlag
     */
    public String getQaFlag() {
        return this.qaFlag;
    }

    /**
     * Gets the qcFlag.
     * @return the qcFlag
     */
    public String getQcFlag() {
        return this.qcFlag;
    }

    /**
     * Gets the Value
     * @return the Value
     */
    public Double getValue() {
        return this.value;
    }

    /**
     * Gets the year
     * @return the year
     */
    public Integer getYear() {
        return this.year;
    }

    /**
     * Set the average.
     * @param average average
     */
    public void setAverage ( Double average ) {
        this.average = average;
    }

    /**
     * Set the collectionDate.
     * @param date collectionDate
     */
    public void setCollectionDate ( String collectionDate ) {
        this.collectionDate = collectionDate;
    }

    /**
     * Set the date.
     * @param date date
     */
    public void setDate ( String date ) {
        this.date = date;
    }

    /**
     * Set the median.
     * @param median median
     */
    public void setMedian ( Double median ) {
        this.median = median;
    }

    /**
     * Set the month.
     * @param month month
     */
    public void setMonth ( Integer month ) {
        this.month = month;
    }

    /**
     * Set the month part.
     * @param month month part
     */
    public void setMonthPart ( String monthPart ) {
        this.monthPart = monthPart;
    }

    /**
     * Set the origQcFlag
     * @param origQcFlag origQcFlag
     */
    public void setOrigQcFlag ( String origQcFlag ) {
        this.origQcFlag = origQcFlag;
    }

    /**
     * Set the origValue.
     * @param origValue origValue
     */
    public void setOrigValue ( Double origValue ) {
        this.origValue = origValue;
    }

    /**
     * Set the qaFlag
     * @param qaFlag qaFlag
     */
    public void setQaFlag ( String qaFlag ) {
        this.qaFlag = qaFlag;
    }

    /**
     * Set the qcFlag
     * @param qcFlag qcFlag
     */
    public void setQcFlag ( String qcFlag ) {
        this.qcFlag = qcFlag;
    }

    /**
     * Set the value.
     * @param value value
     */
    public void setValue ( Double value ) {
        this.value = value;
    }

    /**
     * Set the year.
     * @param year year
     */
    public void setYear ( Integer year ) {
        this.year = year;
    }

}