package rti.tscommandprocessor.commands.rccacis;

import RTi.Util.Message.Message;

/**
<p>
Metadata for station time series joined data.  The data correspond to the MultiStn or new version 2 StnMeta request.
GSON instantiates instances of this class when a JSON string is parsed and then assigns data by matching the JSON
elements with data member names, which must exactly match.
</p>
<p>
The version 1 API uses "data" for the list of metadata while version 2 uses "meta"
(handled in RccAcisStationTimeSeriesMetadataList).  For each item in the list, version 1 uses "postal" and "SIds"
whereas version 2 uses "state" and "sids".  To address these differences there are duplicate data members to allow
GSON to set the values.  However, the get methods are expected to be called only on the version 2 values
(e.g., getState() instead of getPostal()) and the get methods will check for the version 1 data first. This allows the
version 1 API to be supported during the transition and eventually the version 2 API will be phased in as the only
option.
</p>
<p>
An alternative would be to replace "data:" with "meta:" before calling the GSON parser.  However, this may be a
performance hit if the full response string is manipulated.  Try the approach handled in this class for now.
</p>
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
Postal code (version 1 API, replaced by "state" in version 2+).
*/
private String postal = "";

/**
State code (previously "postal" in version 1 API).
*/
private String state = "";

/**
Version 2+ API.
Agency station identifiers and internal agency code (e.g., "SOMEID 2".
Use the RccAcisDataStore.lookupStationIDType() method to convert the number to a string.
*/
private String [] sids = null;

/**
Version 1 API.
Agency station identifiers and internal agency code (e.g., "SOMEID 2".
Use the RccAcisDataStore.lookupStationIDType() method to convert the number to a string.
*/
private String [] sIds = null;

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
    return this.county;
}

/**
Return the elevation (feet).
*/
public double getElev()
{
    return this.elev;
}

/**
Get the preferred station ID for use in time series identifiers.  This includes the station ID type as a
"domain:" prefix.  The preferred identifier may at some point be based on a numbered order of agencies.
Always return the ACIS identifier as the final option (in this case it may be indicative of ACIS adding station types
and this code not knowing about it).
@param prefixDomain if true, prefix the returned string (if not empty) with the station ID type (e.g., "ACIS:").
@return the preferred ID, or a blank string if no ID is determined.
*/
public String getIDPreferred ( boolean prefixDomain )
{   // TODO SAM 2011-01-08 This preferred order could be specified in a configuration file, but need to
    // be careful because don't want user experience to be too varied.
    // List order from Bill Noon (2011-01-13).
    String [] preferredIDOrder = { "COOP", "ICAO", "NWSLI", "FAA", "WMO", "WBAN",
        "ThreadEx", "AWDN", "GHCN", "CoCoRaHS", "ACIS" };
    for ( int i = 0; i < preferredIDOrder.length; i++ ) {
        String id = getIDSpecific(preferredIDOrder[i]);
        if ( !id.equals("") ) {
            if ( prefixDomain ) {
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
Return the specific station identifier, or a blank string if not a specific type station.
@param stationIDType a station identifier type (e.g., "WBAN" or "BUFthr 9") from the RccAcisStationType instances.
@return station identifier for the specific station type.
*/
public String getIDSpecific(String stationIDType)
{
    if ( stationIDType.equalsIgnoreCase("ACIS") ) {
        // The sids won't contain this since it is in uid
        return getUid();
    }
    String [] sids = getSids();
    //Message.printStatus(2,"","Number of sids=" + sids.length );
    for ( int i = 0; i < sids.length; i++ ) {
        //Message.printStatus(2,"","Splitting sid \"" + sids[i] + "\"" );
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
Return the longitude and latitude.
If not available an array of NaN is returned.
*/
public double [] getLl()
{
    if ( this.ll == null ) {
        ll = new double[2];
        ll[0] = Double.NaN;
        ll[1] = Double.NaN;
    }
    return this.ll;
}

/**
Return the name.
*/
public String getName()
{
    return this.name;
}

/**
Return the state code.
*/
public String getState()
{
    String version1Postal = this.postal;
    if ( (version1Postal != null) && !version1Postal.equals("") ) {
        // Phasing in version 1 API to version 2
        return version1Postal;
    }
    return this.state;
}

/**
Return the station identifiers and agency types, for version 2+ API.
*/
public String [] getSids()
{
    String [] sidsVersion1 = sIds;
    if ( (sidsVersion1 != null) ) {
        // Phasing version1 input to version2 code...
        return sidsVersion1;
    }
    else {
        // Version 2 data...
        if ( this.sids == null ) {
            return new String[0];
        }
        else {
            return this.sids;
        }
    }
}

/**
Return the time series identifier corresponding to the metadata.
@param dataStore the data store that is being used to process data, or null to ignore (if null, the current
API version is assumed).
@return the TSID string of the form ID.ACIS.MajorVariableNumber.Day[~DataStoreName]
*/
public String getTSID ( RccAcisDataStore dataStore )
{
    int version = 2;
    String dataStoreName = null;
    if ( dataStore != null ) {
        version = dataStore.getAPIVersion();
        dataStoreName = dataStore.getName();
    }
    StringBuffer tsid = new StringBuffer();
    tsid.append ( getIDPreferred(true) + "." );
    tsid.append ( "ACIS." ); // Providing originating organization might be better?
    if ( version == 1 ) {
        // Version 1 variable list did not cross-correlate with element name so var-major was unique
        tsid.append ( getVariable().getMajor());
    }
    else {
        // Version 2 uses element name
        tsid.append ( getVariable().getElem());
    }
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
public void setName ( String name )
{
    this.name = name;
}

/**
Set the postal code (state abbreviation), used with version 1 API.
*/
public void setPostal ( String postal )
{
    this.postal = postal;
}

/**
Set the state.
*/
public void setState ( String state )
{
    this.state = state;
}

/**
Set the site identifiers.
@param sids array of site identifiers of form ("COOP 2").
*/
public void setSids ( String [] sids )
{
    this.sids = sids;
}

/**
Set the site identifiers, for version 1 API.
@param sids array of site identifiers of form ("COOP 2").
*/
public void setSIds ( String [] SIds )
{
    this.sIds = SIds;
}

/**
Set the valid date range.
*/
public void setValid_daterange ( String [] valid_daterange )
{
    this.valid_daterange = valid_daterange;
}

/**
Set the variable for the data.
*/
public void setVariable ( RccAcisVariableTableRecord variable )
{
    __variable = variable;
}

}