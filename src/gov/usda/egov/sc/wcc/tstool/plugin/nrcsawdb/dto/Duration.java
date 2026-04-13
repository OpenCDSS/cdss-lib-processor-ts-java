
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB Duration objects, for example from:
 * <pre>
{
  "durations": [
    {
      "code": "1",
      "name": "1 Calendar Year",
      "durationMinutes": "525600"
    },
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Duration {

	// Alphabetize the items.

	/**
	 * Duration code (e.g., "1").
	 */
	private String code = "";

	/**
	 * Duration minutes (e.g., "525600").
	 */
	private String durationMinutes = "";

    /**
     * Name (e.g., "1 Calendar Year").
     */
    private String name = "";

    /**
     * Constructor.
     */
    public Duration () {
    }

    /**
     * Gets the duration code.
     * @return the duration code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Gets the duration minutes.
     * @return the duration minutes
     */
    public String getDurationMinutes() {
        return this.durationMinutes;
    }

    /**
     * Gets the name.
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the duration code.
     * @param code duration code
     */
    public void setCode ( String code ) {
        this.code = code;
    }

    /**
     * Set the duration minutes.
     * @param durationMinutes duration minutes
     */
    public void setDurationMinutes ( String durationMinutes ) {
        this.durationMinutes = durationMinutes;
    }

    /**
     * Set the name.
     * @param name duration name
     */
    public void setName ( String name ) {
        this.name = name;
    }

}