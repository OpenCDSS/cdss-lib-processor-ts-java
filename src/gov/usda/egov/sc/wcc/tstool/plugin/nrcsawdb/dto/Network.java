
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB Network objects, for example from:
 * <pre>
 * {
  "networks": [
    {
      "code": "CLMIND",
      "name": "CLIMATE INDEX",
      "description": "CLIMATE INDICES NETWORK"
    },
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Network {

	// Alphabetize the items.

	/**
	 * Network code (e.g., "CLMIND").
	 */
	private String code = "";

	/**
	 * Description (e.g., "CLIMATE INDICES NETWORK").
	 */
	private String description = "";

    /**
     * Name (e.g., "CLIMATE INDEX").
     */
    private String name = "";

    /**
     * Constructor.
     */
    public Network () {
    }

    /**
     * Constructor.
     * @param code network code
     */
    public Network ( String code ) {
    	this.code = code;
    }

    /**
     * Gets the network code.
     * @return the network code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Gets the description.
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Gets the name.
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the network code.
     * @param code network code
     */
    public void setCode ( String code ) {
        this.code = code;
    }

    /**
     * Set the description.
     * @param description network description
     */
    public void setDescription ( String description ) {
        this.description = description;
    }

    /**
     * Set the name.
     * @param name network name
     */
    public void setName ( String name ) {
        this.name = name;
    }

}