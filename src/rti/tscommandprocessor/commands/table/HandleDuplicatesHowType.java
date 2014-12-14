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
The name that should be displayed when the best fit type is used in UIs and reports.
*/
private final String displayName;

/**
Construct a time series statistic enumeration value.
@param displayName name that should be displayed in choices, etc.
*/
private HandleDuplicatesHowType(String displayName) {
    this.displayName = displayName;
}

/**
Return the display name for the statistic.  This is usually the same as the
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