package rti.tscommandprocessor.commands.usgs.nwis.daily;

/**
USGS NWIS site status choices.
*/
public enum UsgsNwisSiteStatusType
{
    /**
    Return all sites.
    */
    ALL("All"),
    /**
    Return active sites.
    */
    ACTIVE("Active"),
    /**
    Return inactive sites.
    */
    INACTIVE("Inactive");
    
    private final String displayName;

    /**
     * Name that should be displayed in choices, etc.
     * @param displayName
     */
    private UsgsNwisSiteStatusType(String displayName) {
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
    public static UsgsNwisSiteStatusType valueOfIgnoreCase(String name)
    {
        if ( name == null ) {
            return null;
        }
        UsgsNwisSiteStatusType [] values = values();
        for ( UsgsNwisSiteStatusType t : values ) {
            if ( name.equalsIgnoreCase(t.toString()) ) {
                return t;
            }
        } 
        return null;
    }
}