// HandleDuplicatesHowType - enumeration to stores values for how to handle duplicate date/time values when converting a table to time series

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

package rti.tscommandprocessor.commands.table;

/**
This enumeration stores values how to handle duplicate date/time values when converting a table to time series
(e.g., with the TableToTimeSeries command).
*/
public enum HandleDuplicatesHowType
{

/**
Add values with the same date/time.
*/
ADD("Add"),

/**
Use the first non-missing value
*/
USE_FIRST_NONMISSING("UseFirstNonmissing"),

/**
Use the last value, even if missing.
*/
USE_LAST("UseLast"),

/**
Use the last non-missing value.
*/
USE_LAST_NONMISSING("UseLastNonmissing");

/**
The name that should be displayed when the type is used in UIs and reports.
*/
private final String displayName;

/**
Construct with a display name.
@param displayName name that should be displayed in choices, etc.
*/
private HandleDuplicatesHowType(String displayName) {
    this.displayName = displayName;
}

/**
Return the display name for the enumeration type.  This is usually the same as the
value but using appropriate mixed case.
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
public static HandleDuplicatesHowType valueOfIgnoreCase(String name)
{
    if ( name == null ) {
        return null;
    }
    HandleDuplicatesHowType [] values = values();
    // Currently supported values
    for ( HandleDuplicatesHowType t : values ) {
        if ( name.equalsIgnoreCase(t.toString()) ) {
            return t;
        }
    } 
    return null;
}

}
