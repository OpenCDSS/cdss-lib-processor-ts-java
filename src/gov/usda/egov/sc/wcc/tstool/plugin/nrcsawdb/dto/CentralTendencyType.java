// CentralTendencyType - enumeration to store central tendency values

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
This enumeration stores central tendency values to query NRCS AWDB web services.
It is NOT equivalent to the TimingCentralTendency data transfer object.
*/
public enum CentralTendencyType {
	
    /**
     * NONE - no data.
     */
    NONE("NONE"),

    /**
     * ALL- all daily.
     */
    ALL("ALL"),

	/**
	 * MEDIAN - median values.
	 */
	MEDIAN("MEDIAN"),

	/**
	 * AVERAGE - average values.
	 */
	AVERAGE("AVERAGE");

    /**
     * The name that should be displayed.
     */
    private final String displayName;

    /**
     * Construct an enumeration value.
     * @param displayName name that should be displayed in choices, etc.
     */
    private CentralTendencyType ( String displayName ) {
        this.displayName = displayName;
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
    public static CentralTendencyType valueOfIgnoreCase ( String name ) {
    	if ( name == null ) {
    		return null;
    	}

    	CentralTendencyType [] values = values();
    	for ( CentralTendencyType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) ) {
            	return t;
        	}
    	}
    	return null;
	}

}