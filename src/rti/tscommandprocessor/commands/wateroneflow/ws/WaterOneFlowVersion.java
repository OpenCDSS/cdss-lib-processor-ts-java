// WaterOneFlowVersion - WaterOneFlow versions, reflecting different official WaterOneFlow specifications (WSDL)
// and implementation variations.

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

package rti.tscommandprocessor.commands.wateroneflow.ws;

/**
WaterOneFlow versions, reflecting different official WaterOneFlow specifications (WSDL)
and implementation variations.  This enumeration is used internally once a version is detected,
and also allows selections in user interfaces.  It is not clear if the WaterOneFlow version is
consistent with the WaterML version, but conceivably they don't need to be the same since the WaterOneFlow
version describes the web services and WaterML is a data/transport format for time series.
*/
public enum WaterOneFlowVersion
{
    /**
    Matches specification.
    */
    STANDARD_1_0("1.0"),
    /**
    Matches specification.
    */
    STANDARD_1_1("1.1"),
    /**
    Data values are not transformed prior to analysis.
    */
    UNKNOWN("Unknown");
    
    private final String displayName;

    /**
     * Name that should be displayed in choices, etc.
     * @param displayName
     */
    private WaterOneFlowVersion(String displayName) {
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
    public static WaterOneFlowVersion valueOfIgnoreCase(String name)
    {
        if ( name == null ) {
            return null;
        }
        WaterOneFlowVersion [] values = values();
        for ( WaterOneFlowVersion t : values ) {
            if ( name.equalsIgnoreCase(t.toString()) ) {
                return t;
            }
        } 
        return null;
    }
}
