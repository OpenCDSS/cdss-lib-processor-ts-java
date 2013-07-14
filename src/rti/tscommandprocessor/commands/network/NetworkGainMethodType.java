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