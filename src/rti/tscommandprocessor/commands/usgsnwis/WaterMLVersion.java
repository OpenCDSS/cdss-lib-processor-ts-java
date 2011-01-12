package rti.tscommandprocessor.commands.usgsnwis;

/**
WaterML versions, reflecting different official WaterML specifications and implementation variations such
as the USGS data.
*/
public enum WaterMLVersion
{
    /**
    Matches specification.
    */
    VANILLA_1_0("1.0"),
    /**
    Matches specification.
    */
    VANILLA_1_1("1.1"),
    /**
    USGS variation on 1.0?
    */
    USGS("USGS"),
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
        WaterMLVersion [] values = values();
        for ( WaterMLVersion t : values ) {
            if ( name.equalsIgnoreCase(t.toString()) ) {
                return t;
            }
        } 
        return null;
    }
}