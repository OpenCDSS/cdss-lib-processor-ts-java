
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB Element objects, for example from:
 * <pre>
 *
{
  "forecastPeriods": [
    {
      "code": "09",
      "name": "OCT-SEP",
      "beginMonthDay": "10-01",
      "endMonthDay": "09-30"
    },
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastPeriod {

	// Alphabetize the items.

    /**
     * Beginning month and day (e.g., "10-01").
     */
    private String beginMonthDay = "";

	/**
	 * Forecast period code (e.g., "09").
	 */
	private String code = "";

	/**
	 * Forecast period description.
	 */
	private String description = "";
	
    /**
     * Ending month and day (e.g., "09-30").
     */
    private String endMonthDay = "";

    /**
     * Name (e.g., "OCT-SEP").
     */
    private String name = "";

    /**
     * Constructor.
     */
    public ForecastPeriod () {
    }
    
    /**
     * Find a forecast period given its name.
     * @param forecastPeriods list of ForecastPeriod
     * @param name forecast period name to find
     */
    public static ForecastPeriod findForecastPeriodForName ( List<ForecastPeriod> forecastPeriods, String name ) {
    	if ( (forecastPeriods == null) || (name == null) || name.isEmpty() ) {
    		return null;
    	}
    	for ( ForecastPeriod forecastPeriod : forecastPeriods ) {
    		if ( forecastPeriod.getName().equals(name) ) {
    			return forecastPeriod;
    		}
    	}
    	// Did not find.
    	return null;
    }

    /**
     * Gets the beginning month and day.
     * @return the beginning month and day
     */
    public String getBeginMonthDay() {
        return this.beginMonthDay;
    }

    /**
     * Gets the forecast period code.
     * @return the forecast period code
     */
    public String getCode() {
        return this.code;
    }

    /**
     * Gets the forecast period description.
     * @return the forecast period description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Gets the ending month and day.
     * @return the ending month and day
     */
    public String getEndMonthDay() {
        return this.endMonthDay;
    }

    /**
     * Gets the name.
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set the beginning month and day.
     * @param beginMonthDay beginning month and day
     */
    public void setBeginMonthDay ( String beginMonthDay ) {
        this.beginMonthDay = beginMonthDay;
    }

    /**
     * Set the forecast period code.
     * @param code forecast period code
     */
    public void setCode ( String code ) {
        this.code = code;
    }

    /**
     * Set the forecast period description.
     * @param code forecast period description
     */
    public void setDescription ( String description ) {
        this.description = description;
    }

    /**
     * Set the ending month and day.
     * @param endMonthDay ending month and day
     */
    public void setEndMonthDay ( String endMonthDay ) {
        this.endMonthDay = endMonthDay;
    }

    /**
     * Set the name.
     * @param name element name
     */
    public void setName ( String name ) {
        this.name = name;
    }

}