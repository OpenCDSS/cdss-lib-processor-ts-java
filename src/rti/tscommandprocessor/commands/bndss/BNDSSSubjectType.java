package rti.tscommandprocessor.commands.ipp;

/**
Database subject types, to which are attached metadata and time series data.
*/
public enum IPPSubjectType
{
    COUNTY("County"),
    //BASIN("Basin"),
    //STATE("State"),
    PROVIDER("Provider"),
    PROJECT("Project");
    
    private final String displayName;

    /**
     * Name that should be displayed in choices, etc.
     * @param displayName
     */
    private IPPSubjectType(String displayName) {
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
    public static IPPSubjectType valueOfIgnoreCase(String name)
    {
        IPPSubjectType [] values = values();
        for ( IPPSubjectType t : values ) {
            if ( name.equalsIgnoreCase(t.toString()) ) {
                return t;
            }
        } 
        return null;
    }
}