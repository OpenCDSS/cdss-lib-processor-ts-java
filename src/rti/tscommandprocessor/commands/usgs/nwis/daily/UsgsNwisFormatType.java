package rti.tscommandprocessor.commands.usgs.nwis.daily;

/**
USGS NWIS data formats.
*/
public enum UsgsNwisFormatType
{
    /**
    JSON.
    */
    JSON("JSON"),
    /**
    RDB.
    */
    RDB("RDB"),
    /**
    WaterML
    */
    WATERML("WaterML");
    
    private final String displayName;

    /**
     * Name that should be displayed in choices, etc.
     * @param displayName
     */
    private UsgsNwisFormatType(String displayName) {
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
    public static UsgsNwisFormatType valueOfIgnoreCase(String name)
    {
        if ( name == null ) {
            return null;
        }
        UsgsNwisFormatType [] values = values();
        for ( UsgsNwisFormatType t : values ) {
            if ( name.equalsIgnoreCase(t.toString()) ) {
                return t;
            }
        } 
        return null;
    }
}