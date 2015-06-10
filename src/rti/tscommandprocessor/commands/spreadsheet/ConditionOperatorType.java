package rti.tscommandprocessor.commands.spreadsheet;

/**
Condition operator types, for example used in condition statements.
All apply to numbers, and additionally CONTAINS applies to strings.
*/
public enum ConditionOperatorType
{
/**
Is value equal to.
*/
EQUAL_TO("=="),
/**
Is value not equal to.
*/
NOT_EQUAL_TO("!="),
/**
Is value less than.
*/
LESS_THAN("<"),
/**
Is value less than or equal to.
*/
LESS_THAN_OR_EQUAL_TO("<="),
/**
Is value greater than.
*/
GREATER_THAN(">"),
/**
Is value greater than or equal to.
*/
GREATER_THAN_OR_EQUAL_TO(">="),
/**
Does the value contain.
*/
CONTAINS("contains");

/**
The name that should be displayed when the best fit type is used in UIs and reports.
*/
private final String displayName;

/**
Construct a time series statistic enumeration value.
@param displayName name that should be displayed in choices, etc.
*/
private ConditionOperatorType(String displayName) {
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
public static ConditionOperatorType valueOfIgnoreCase(String name)
{
    ConditionOperatorType [] values = values();
    for ( ConditionOperatorType t : values ) {
        if ( name.equalsIgnoreCase(t.toString()) ) {
            return t;
        }
    } 
    return null;
}

}