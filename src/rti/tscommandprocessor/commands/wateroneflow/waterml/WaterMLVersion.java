// WaterMLVersion - WaterML versions, reflecting different official WaterML specifications
// and implementation variations such as the USGS data.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.wateroneflow.waterml;

/**
WaterML versions, reflecting different official WaterML specifications and implementation variations such
as the USGS data.  This enumeration is used internally once a version is detected, and also allows
selections in user interfaces.
*/
public enum WaterMLVersion
{
    /**
    Matches specification (CUAHSI specification).
    */
    STANDARD_1_0("1.0"),
    /**
    Matches specification (CUAHSI specification).
    */
    STANDARD_1_1("1.1"),
    /**
    Matches specification (OGC specification).
    */
    STANDARD_2_0("2.0"),
    /**
    USGS and other variations are handled with specific checks.
    */
    /**
    Data values are not transformed prior to analysis.
    */
    UNKNOWN("Unknown");
    
    private final String displayName;

    /**
     * Name that should be displayed in choices, etc.
     * @param displayName
     */
    private WaterMLVersion(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Return the display name.
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
    public static WaterMLVersion valueOfIgnoreCase(String name)
    {
        if ( name == null ) {
            return null;
        }
        WaterMLVersion [] values = values();
        for ( WaterMLVersion t : values ) {
            if ( name.equalsIgnoreCase(t.toString()) ) {
                return t;
            }
        } 
        return null;
    }
}
