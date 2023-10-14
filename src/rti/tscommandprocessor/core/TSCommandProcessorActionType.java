// TSCommandProcessorActionType - actions that are taken on time series

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.core;

/**
Enumeration of actions performed on time series.
*/
public enum TSCommandProcessorActionType {

    /**
     * Insert a time series in results.
     */
    INSERT_TS("Insert"),

    /**
     * Update existing time series in results.
     */
    UPDATE_TS("Update"),

	/**
	 * Exit time series processing.
	 */
	EXIT("Exit"),

    /**
     * No action is performed.
     */
    NONE("None");

    /**
     * The string name that should be displayed.
     */
    private final String displayName;

    /**
     * Construct an enumeration value.
     * @param displayName name that should be displayed in choices, etc.
     */
    private TSCommandProcessorActionType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Equals method to compare enumeration values.
     */
    public boolean equals ( String tsListType ) {
        if ( tsListType.equalsIgnoreCase(this.displayName) ) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Return the display name for the time series list type.
     * This is the same as the value but using appropriate mixed case.
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
	public static TSCommandProcessorActionType valueOfIgnoreCase(String name) {
	    if ( name == null ) {
	        return null;
	    }
	    for ( TSCommandProcessorActionType t : values() ) {
	        if ( name.equalsIgnoreCase(t.toString()) ) {
	            return t;
	        }
	    }
	    return null;
	}
}