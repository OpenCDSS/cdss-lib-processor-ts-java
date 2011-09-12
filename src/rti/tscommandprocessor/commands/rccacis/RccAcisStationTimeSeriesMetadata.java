package rti.tscommandprocessor.commands.rccacis;

/**
Metadata for station time series joined data.  The data correspond to the MultiStn request metadata.
*/
public class RccAcisStationTimeSeriesMetadata
{
    
/**
Default constructor, required by GSON.
*/
public RccAcisStationTimeSeriesMetadata ()
{
}

/**
Data store associated with this record.
*/
private RccAcisDataStore __dataStore = null;

/**
Variable information.
*/
private RccAcisVariableTableRecord __variable = null;

/**
FIPS county.
*/
private String county = "";

/**
Elevation (feet).
*/
private double elev = Double.NaN;

/**
Longitude and latitude.
*/
private double[] ll;

/**
Location name.
*/
private String name = "";

/**
Postal code.
*/
private String postal = "";

/**
Agency station identifiers and internal agency code (e.g., "SOMEID 2".
Use the RccAcisDataStore.lookupStationIDType() method.
*/
private String [] sIds = { "" };

/**
ACIS station identifier.
*/
private String uid = "";

/**
Valid date range.
*/
private String[] valid_daterange = { "", "" };

/**
Return the FIPS county.
*/
public String getCounty()
{
    return county;
}

/**
Return the elevation (feet).
*/
public double getElev()
{
    return elev;
}

/**
Return the longitude and latitude.
If not available an array of NaN is returned.
*/
public double [] getLl()
{
    if ( ll == null ) {
        ll = new double[2];
        ll[0] = Double.NaN;
        ll[1] = Double.NaN;
    }
    return ll;
}

/**
Return the name.
*/
public String getName()
{
    return name;
}

/**
Return the postal code.
*/
public String getPostal()
{
    return postal;
}

/**
Return the station identifiers and agency types.
*/
public String [] getSIds()
{
    if ( sIds == null ) {
        return new String[0];
    }
    else {
        return sIds;
    }
}

/**
Get the preferred station ID for use in time series identifiers.  This includes the station ID type as a
"domain:" prefix.  The preferred identifier may at some point be based on a numbered order of agencies.
Always return the ACIS identifier as the final option (in this case it may be indicative of ACIS adding station types
and this code not knowing about it).
@param prefixDomain if true, prefix the returned string (if not empty) with the station ID type (e.g., "ACIS:").
@return the preferred ID, or a blank string if no ID is determined.
*/
public String getIDPreferred ( boolean prefix )
{   // TODO SAM 2011-01-08 This preferred order could be specified in a configuration file, but need to
    // be careful because don't want user experience to be too varied.
    // List order from Bill Noon (2011-01-13).
    String [] preferredIDOrder = { "COOP", "ICAO", "NWSLI", "FAA", "WMO", "WBAN",
        "ThreadEx", "AWDN", "GHCN", "CoCoRaHS", "ACIS" };
    for ( int i = 0; i < preferredIDOrder.length; i++ ) {
        String id = getIDSpecific(preferredIDOrder[i]);
        if ( !id.equals("") ) {
            if ( prefix ) {
                return preferredIDOrder[i] + ":" + id;
            }
            else {
                return id;
            }
        }
    }
    // Hopefully should not get here...
    return "";
}

/**
Return the specific station identifier, or a blank string if not a of the specific type station.
@param stationIDType a station identifier type (e.g., "WBAN") from the RccAcisStationType instances.
@return station identifier for the specific station type.
*/
public String getIDSpecific(String stationIDType)
{
    if ( stationIDType.equalsIgnoreCase("ACIS") ) {
        // The sids won't contain this since it is in uid
        return getUid();
    }
    String [] sids = getSIds();
    for ( int i = 0; i < sids.length; i++ ) {
        String [] parts = sids[i].split(" ");
        int stationTypeInt;
        try {
            stationTypeInt = Integer.parseInt(parts[1].trim());
        }
        catch ( NumberFormatException e ) {
            return "";
        }
        if ( __dataStore != null ) {
            RccAcisStationType stationType = __dataStore.lookupStationTypeFromCode ( stationTypeInt );
            if ( (stationType != null) && stationType.getType().equalsIgnoreCase(stationIDType) ) {
                return parts[0].trim();
            }
        }
    }
    return "";
}

/**
Return the time series identifier corresponding to the metadata.
@param dataStoreName the name of the data store to include at the end of the TSID, or null to ignore.
@return the TSID string of the form ID.ACIS.MajorVariableNumber.Day[~DataStoreName]
*/
public String getTSID ( String dataStoreName )
{
    StringBuffer tsid = new StringBuffer();
    tsid.append ( getIDPreferred(true) + "." );
    tsid.append ( "ACIS." ); // Providing organization might be better?
    tsid.append ( getVariable().getMajor());
    tsid.append ( ".Day" );
    if ( (dataStoreName != null) && !dataStoreName.equals("") ) {
        tsid.append ( "~" + dataStoreName );
    }
    return tsid.toString();
}

/**
Return the ACIS station identifier.
*/
public String getUid()
{
    return uid;
}

/**
Return the valid date range.
*/
public String [] getValid_daterange()
{
    return valid_daterange;
}

/**
Return the variable information for the data.
*/
public RccAcisVariableTableRecord getVariable()
{
    return __variable;
}

/**
Set the data store.
*/
public void setDataStore ( RccAcisDataStore dataStore )
{
    __dataStore = dataStore;
}

/**
Set the name.
*/
public void setName ( String name2 )
{
    name = name2;
}

/**
Set the name.
*/
public void setPostal ( String postal2 )
{
    postal = postal2;
}

/**
Set the valid date range.
*/
public void setValid_daterange ( String [] valid_daterange2 )
{
    valid_daterange = valid_daterange2;
}

/**
Set the variable for the data.
*/
public void setVariable ( RccAcisVariableTableRecord variable )
{
    __variable = variable;
}

}