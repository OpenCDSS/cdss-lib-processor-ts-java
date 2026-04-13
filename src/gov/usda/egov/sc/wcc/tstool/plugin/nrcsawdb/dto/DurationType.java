// DurationType - enumeration to store duration values

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 2026 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

/**
This enumeration stores values of duration used to query NRCS AWDB web services.
It is NOT equivalent to the Duration data transfer object.
*/
public enum DurationType {
	// List in the order from small duration to large.
	
    /**
     * Hourly data.
     */
    HOURLY("HOURLY"),

    /**
     * Daily data.
     */
    DAILY("DAILY"),

	/**
	 * Semi-monthly data.
	 */
	SEMIMONTHLY("SEMIMONTHLY"),

	/**
	 * Monthly data.
	 */
	MONTHLY("MONTHLY"),

	/**
	 * Calendar year.
	 */
	CALENDAR_YEAR("CALENDAR_YEAR"),

	/**
	 * Water year.
	 */
	WATER_YEAR("WATER_YEAR");

    /**
     * The name that should be displayed.
     */
    private final String displayName;

    /**
     * Construct an enumeration value.
     * @param displayName name that should be displayed in choices, etc.
     */
    private DurationType ( String displayName ) {
        this.displayName = displayName;
    }
    
    /**
     * Check whether a DataType instance matches another.
     * The display name is compared.
     * @param name the data type name to check
     */
    public boolean equals ( String name ) {
    	if ( this.displayName.equalsIgnoreCase(name) ) {
    		return true;
    	}
    	else {
    		return false;
    	}
    }

    /**
     * Return the display name for the enumeration.
     * This is usually the same as the value but using appropriate mixed case.
     * @return the display name.
     */
    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Return the enumeration value given a string name (case-independent).
     * @return the enumeration value given a string name (case-independent), or null if not matched.
     */
    public static DurationType valueOfIgnoreCase ( String name ) {
    	if ( name == null ) {
    		return null;
    	}

    	DurationType [] values = values();
    	for ( DurationType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) ) {
            	return t;
        	}
    	}
    	return null;
	}

}