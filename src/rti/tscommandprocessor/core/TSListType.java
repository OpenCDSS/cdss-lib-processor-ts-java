package rti.tscommandprocessor.core;

/**
This class provides an enumeration of possible TSList command parameter values.
An enum could be used when Java 1.5 is utilized.     
*/
public class TSListType {
	
    /**
     * AllMatchingTS indicates that all time series matching a pattern should be in the list.
     */
    public static TSListType ALL_MATCHING_TSID = new TSListType(0, "AllMatchingTSID");
    
    /**
     * AllTS indicates that all time series should be in the list. 
     */
    public static TSListType ALL_TS = new TSListType(1, "AllTS");
	
	/**
	 * EnsembleID indicates that all time series in the ensemble should be in the list.
	 */
	public static TSListType ENSEMBLE_ID = new TSListType(2, "EnsembleID");
	
    /**
     * FirstMatchingTSID indicates that only the first matching time series should be in the list. 
     */
    public static TSListType FIRST_MATCHING_TSID = new TSListType(3, "FirstMatchingTSID");
    
    /**
     * LastMatchingTSID indicates that only the last matching time series should be in the list. 
     */
    public static TSListType LAST_MATCHING_TSID = new TSListType(3, "LastMatchingTSID");
	
	/**
	 * SelectedTS indicates that all selected time series should be in the list. 
	 */
	public static TSListType SELECTED_TS = new TSListType(4, "SelectedTS");
    
    /**
     * SpecifiedTSID indicates that all specified time series should be in the list.
     * Specified time series are those that are explicitly included in a list.
     */
    public static TSListType SPECIFIED_TSID = new TSListType(5, "SpecifiedTSID");
    
    /**
     * SpecifiedTSID indicates that all specified time series should be in the list. 
     */
    public static TSListType TSPOSITION = new TSListType(6, "TSPosition");
    
    /**
	 * Integer value of the type.
	 * @uml.property  name="__type"
	 */
	private int __type;
	/**
	 * Type name, e.g., "AllTS", "EnsembleID".
	 * @uml.property  name="__typename"
	 */
	private String __typename;
	
	/**
	 * Construct the TSList type using the type number and name.  It is
	 * private because other code should use the predefined instances.
	 * @param type
	 * @param typename
	 */
	private TSListType ( int type, String typename ){
		__type = type;
		__typename = typename;
	}
	
	/**
	 * Determine if two types are equal.
     * @param type An instance of TSListType.
	 */
	public boolean equals ( TSListType type )
	{
		if ( __type == type.__type ) {
			return true;
		}
		else {
			return false;
		}
	}
    
    /**
     * Determine if two types are equal, based on the string name.  Case is ignored.
     * @param typename Type name (e.g., "AllTS").
     */
    public boolean equals ( String typename )
    {
        if ( __typename.equalsIgnoreCase( typename) ) {
            return true;
        }
        else {
            return false;
        }
    }

	/**
	 * Return a String representation of the command status (return the type name).
	 */
	public String toString () {
		return __typename;
	}
}
