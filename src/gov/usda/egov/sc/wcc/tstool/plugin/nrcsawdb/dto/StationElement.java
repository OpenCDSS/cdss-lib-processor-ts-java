
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB station element objects, for example:
 * <pre>
       {
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
      }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class StationElement {

	// Alphabetize the elements.

	/**
	 * Begin date for data (e.g., "1980-10-30 15:53").
	 */
	private String beginDate = "";

	/**
	 * Data precision (e.g., 1).
	 */
	private Integer dataPrecision = null;

	/**
	 * Whether derived data.
	 */
	private Boolean derivedData = null;

	/**
	 * Description (e.g., "Average Air Temperature - Sub-Hourly Sampling Frequency").
	 */
	//private String description = "";

	/**
	 * Duration name (e.g., "DAILY").
	 */
	private String durationName = "";

	/**
	 * Element code (e.g., "WTEQ").
	 */
	private String elementCode = "";

	/**
	 * End date for data (e.g., "2100:01-01").
	 */
	private String endDate = "";

    /**
     * English unit code (e.g., "degF").
     */
    //private String englishUnitCode = "";
		
	/**
	 * Function code (e.g., "V").
	 */
	//private String functionCode = "";

	/**
	 * Height/depth.
	 */
	private Integer heightDepth = null;

    /**
     * Metric unit code (e.g., "degC").
     */
    //private String metricUnitCode = "";

    /**
     * Name (e.g., "AIR TEMPERATURE AVERAGE").
     */
    //private String name = "";

	/**
	 * Ordinal (e.g., 1).
	 */
	private Integer ordinal = null;
    
    /**
     * Original unit code (e.g., "in").
     */
    private String originalUnitCode = "";
    
    /**
     * Physical element name (e.g., "air temperature").
     */
    //private String physicalElementName = "";

    /**
     * Stored unit code (e.g., "degF").
     * Previously "storedUnitCd" in the SOAP API.
     */
    private String storedUnitCode = "";

    /**
     * Constructor.
     */
    public StationElement () {
    }

    /**
     * Gets the begin date as a string.
     * @return the begin date
     */
    public String getBeginDate() {
        return this.beginDate;
    }

    /**
     * Gets the data precision.
     * @return the data precision 
     */
    public Integer getDataPrecision() {
        return this.dataPrecision;
    }

    /**
     * Gets whether derived data
     * @return whether derived data
     */
    public Boolean getDerivedData () {
        return this.derivedData;
    }

    /**
     * Gets the description.
     * @return the description
     */
    /*
    public String getDescription() {
        return this.description;
    }
    */

    /**
     * Gets the duration name
     * @return the duration name
     */
    public String getDurationName() {
        return this.durationName;
    }

    /**
     * Gets the element code.
     * @return the element code
     */
    public String getElementCode() {
        return this.elementCode;
    }

    /**
     * Gets the end date as a string.
     * @return the end date
     */
    public String getEndDate() {
        return this.endDate;
    }

    /**
     * Get the English unit code.
     * @return the English unit code
     */
    /*
    public String getEnglishUnitCode () {
        return this.englishUnitCode;
    }
    */

    /**
     * Gets the function code.
     * @return the function code
     */
    /*
    public String functionCode() {
        return this.functionCode;
    }
    */

    /**
     * Gets the height/depth.
     * @return the height/depth
     */
    public Integer getHeightDepth() {
        return this.heightDepth;
    }

    /**
     * Get the metric unit code.
     * @return the metric unit code
     */
    /*
    public String getMetricUnitCode () {
        return this.metricUnitCode;
    }
    */

    /**
     * Gets the name.
     * @return name
     */
    /*
    public String getName() {
        return this.name;
    }
    */

    /**
     * Gets the ordinal.
     * @return ordinal
     */
    public Integer getOrdinal() {
        return this.ordinal;
    }

    /**
     * Get the original unit code
     * @return original unit code
     */
    public String getOriginalUnitCode () {
        return this.originalUnitCode;
    }

    /**
     * Gets the physical element name.
     * @return physical element name
     */
    /*
    public String gePhysicalElementName() {
        return this.physicalElementName;
    }
    */

    /**
     * Get the stored unit code
     * @return stored unit code
     */
    public String getStoredUnitCode () {
        return this.storedUnitCode;
    }

    /**
     * Set the begin date
     * @param beginDate begin date
     */
    public void setBeginDate ( String beginDate ) {
        this.beginDate = beginDate;
    }

    /**
     * Set the description.
     * @param description element description
     */
    /*
    public void setDescription ( String description ) {
        this.description = description;
    }
    */

    /**
     * Set the data precision.
     * @param dataPrecision data precision
     */
    public void setDataPrecision ( Integer dataPrecision ) {
        this.dataPrecision = dataPrecision;
    }

    /**
     * Set whether derived data
     * @param derivedData whether derived data
     */
    public void setDerivedData ( Boolean derivedData ) {
        this.derivedData = derivedData;
    }

    /**
     * Set the duration name.
     * @param durationName duration name
     */
    public void setDurationName ( String durationName ) {
        this.durationName = durationName;
    }

    /**
     * Set the element code.
     * @param elemdntCode element code
     */
    public void setEleeentCode ( String elementCode ) {
        this.elementCode = elementCode;
    }

    /**
     * Set the end date
     * @param endDate end date
     */
    public void setEndDate ( String endDate ) {
        this.endDate = endDate;
    }

    /**
     * Set the English unit code.
     * @param englishUnitCode English unit code
     */
    /*
    public void setEnglishUnitCode ( String englishUnitCode ) {
        this.englishUnitCode = englishUnitCode;
    }
    */

    /**
     * Set the function code.
     * @param function code element code
     */
    /*
    public void setFunctionCode ( String functionCode ) {
        this.functionCode = functionCode;
    }
    */

    /**
     * Set the height/depth.
     * @param heightDepth height/depth
     */
    public void setHeightDepth ( Integer heightDepth ) {
        this.heightDepth = heightDepth;
    }

    /**
     * Set the metric unit code.
     * @param metricUnitCode metric unit code
     */
    /*
    public void setMetricUnitCode ( String metricUnitCode ) {
        this.metricUnitCode = metricUnitCode;
    }
    */

    /**
     * Set the name.
     * @param name element name
     */
    /*
    public void setName ( String name ) {
        this.name = name;
    }
    */

    /**
     * Set the ordinal.
     * @param ordinal ordinal
     */
    public void setOrdinal ( Integer ordinal ) {
        this.ordinal = ordinal;
    }

    /**
     * Set the original unit code
     * @param orighnalUnitCode original unit code
     */
    public void setOriginalUnitCode ( String originalUnitCode ) {
        this.originalUnitCode = originalUnitCode;
    }

    /**
     * Set the physical Element name.
     * @param physicalElementName physical element aame
     */
    /*
    public void setPhysicalElementName ( String physicalElementName ) {
        this.physicalElementName = physicalElementName;
    }
    */

    /**
     * Set the stored unit code property.
     * @param storedUnitCode stored unit code
     */
    public void setStoredUnitCode ( String storedUnitCode ) {
        this.storedUnitCode = storedUnitCode;
    }

}