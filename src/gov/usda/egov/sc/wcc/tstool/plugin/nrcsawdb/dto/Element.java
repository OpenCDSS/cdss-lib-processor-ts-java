
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB Element objects, for example from:
 * <pre>
 * {
  "elements": [
    {
      "code": "TAVG",
      "name": "AIR TEMPERATURE AVERAGE",
      "physicalElementName": "air temperature",
      "functionCode": "V",
      "dataPrecision": 1,
      "description": "Average Air Temperature - Sub-Hourly Sampling Frequency",
      "storedUnitCode": "degF",
      "englishUnitCode": "degF",
      "metricUnitCode": "degC"
    },
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Element {

	// Alphabetize the elements.

	/**
	 * Element code (e.g., "TAVG").
	 * Previously "elementCd" in the SOAP API.
	 */
	private String code = "";

	/**
	 * Data precision (e.g., 1).
	 */
	private Integer dataPrecision = null;

	/**
	 * Description (e.g., "Average Air Temperature - Sub-Hourly Sampling Frequency").
	 */
	private String description = "";

    /**
     * English unit code (e.g., "degF").
     */
    private String englishUnitCode = "";
		
	/**
	 * Function code (e.g., "V").
	 */
	private String functionCode = "";

    /**
     * Metric unit code (e.g., "degC").
     */
    private String metricUnitCode = "";

    /**
     * Name (e.g., "AIR TEMPERATURE AVERAGE").
     */
    private String name = "";
    
    /**
     * Physical element name (e.g., "air temperature").
     */
    private String physicalElementName = "";

    /**
     * Stored unit code (e.g., "degF").
     * Previously "storedUnitCd" in the SOAP API.
     */
    private String storedUnitCode = "";
    
    /**
     * Constructor.
     */
    public Element () {
    }

    /**
     * Gets the element code.
     * @return the element code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Gets the data precision.
     * @return the data precision 
     */
    public Integer getDataPrecision() {
        return this.dataPrecision;
    }

    /**
     * Gets the description.
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Get the English unit code.
     * @return the English unit code
     */
    public String getEnglishUnitCode () {
        return this.englishUnitCode;
    }

    /**
     * Gets the function code.
     * @return the function code
     */
    public String getFunctionCode() {
        return this.functionCode;
    }

    /**
     * Get the metric unit code.
     * @return the metric unit code
     */
    public String getMetricUnitCode () {
        return this.metricUnitCode;
    }

    /**
     * Gets the name.
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the physical element name.
     * @return physical element name
     */
    public String gePhysicalElementName() {
        return this.physicalElementName;
    }

    /**
     * Get the stored unit code
     * @return stored unit code
     */
    public String getStoredUnitCode () {
        return this.storedUnitCode;
    }

    /**
     * Set the element code.
     * @param code element code
     */
    public void setCode ( String code ) {
        this.code = code;
    }

    /**
     * Set the description.
     * @param description element description
     */
    public void setDescription ( String description ) {
        this.description = description;
    }

    /**
     * Set the data precision.
     * @param dataPrecision data precision
     */
    public void setDataPrecision ( Integer dataPrecision ) {
        this.dataPrecision = dataPrecision;
    }

    /**
     * Set the English unit code.
     * @param englishUnitCode English unit code
     */
    public void setEnglishUnitCode ( String englishUnitCode ) {
        this.englishUnitCode = englishUnitCode;
    }

    /**
     * Set the function code.
     * @param function code element code
     */
    public void setFunctionCode ( String functionCode ) {
        this.functionCode = functionCode;
    }

    /**
     * Set the metric unit code.
     * @param metricUnitCode metric unit code
     */
    public void setMetricUnitCode ( String metricUnitCode ) {
        this.metricUnitCode = metricUnitCode;
    }

    /**
     * Set the name.
     * @param name element name
     */
    public void setName ( String name ) {
        this.name = name;
    }

    /**
     * Set the physical Element name.
     * @param physicalElementName physical element aame
     */
    public void setPhysicalElementName ( String physicalElementName ) {
        this.physicalElementName = physicalElementName;
    }

    /**
     * Set the stored unit code property.
     * @param storedUnitCode stored unit code
     */
    public void setStoredUnitCode ( String storedUnitCode ) {
        this.storedUnitCode = storedUnitCode;
    }

}