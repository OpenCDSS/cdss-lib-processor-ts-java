// NetworkGainMethodType - Gain methods for network.

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

package rti.tscommandprocessor.commands.network;

/**
Gain methods for network.
*/
public enum NetworkGainMethodType
{
/**
Distance (stream mile, etc.), used when rate of gain/loss is constant through reach.
*/
DISTANCE("Distance"),
/**
Product of distance and weight, used when rate of gain/loss is constant along distance.
*/
DISTANCE_WEIGHT("DistanceWeight"),
/**
Weight (relative gain/loss for node in reach), used when fraction of gain/loss for node is constant.
*/
WEIGHT("Weight"),
/**
No gain analysis (carry forward).
*/
NONE("None");
    
/**
The name that should be displayed when the best fit type is used in UIs and reports.
*/
private final String displayName;

/**
Construct an enumeration value.
@param displayName name that should be displayed in choices, etc.
*/
private NetworkGainMethodType(String displayName) {
    this.displayName = displayName;
}

/**
Return the display name.
@return the display name.
*/
@Override
public String toString() {
    return displayName;
}

/**
Return the enumeration value given a string name (case-independent).
@return the enumeration value given a string name (case-independent), or null if not matched.
*/
public static NetworkGainMethodType valueOfIgnoreCase(String name)
{
    if ( name == null ) {
        return null;
    }
    NetworkGainMethodType [] values = values();
    for ( NetworkGainMethodType t : values ) {
        if ( name.equalsIgnoreCase(t.toString()) ) {
            return t;
        }
    } 
    return null;
}

}
