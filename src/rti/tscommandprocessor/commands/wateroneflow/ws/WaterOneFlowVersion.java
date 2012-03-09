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