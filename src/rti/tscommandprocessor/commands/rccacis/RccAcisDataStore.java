package rti.tscommandprocessor.commands.rccacis;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

import riverside.datastore.AbstractWebServiceDataStore;

import RTi.TS.TS;
import RTi.TS.TSDataFlagMetadata;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.IO.ReaderInputStream;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
Data store for Regional Climate Center Applied Climate Information System (RCC ACIS) web services.
This class provides a general interface to the web service, consistent with TSTool conventions.
@author sam
*/
public class RccAcisDataStore extends AbstractWebServiceDataStore
{

/**
The web service API version, critical for forming the request URL and parsing the results.
*/
private int __apiVersion = 2; // Default
    
/**
The records of table variables, for version 1 read from:  http://data.rcc-acis.org/doc/VariableTable.html
*/
private List<RccAcisVariableTableRecord> __variableTableRecordList = new Vector();

/**
The station codes for provider agency station identifiers.
*/
private List<RccAcisStationType> __stationTypeList = new Vector();

/**
Indicates whether global data store properties have been initialized, set by initialize().
*/
private boolean __initialized = false;

/**
Constructor for web service.
Important, properties other than the default values passed as parameters may be set with a subsequent
call to setProperties().  Consequently, initialization should occur from public called methods to ensure
that information is available for initialization.
*/
public RccAcisDataStore ( String name, String description, URI serviceRootURI )
throws URISyntaxException, IOException
{
    setName ( name );
    setDescription ( description );
    setServiceRootURI ( serviceRootURI );
    // Determine the web service version
    determineAPIVersion();
    // OK to initialize since no properties other than the main properties impact anything
    initialize();
}

/**
Factory method to construct a data store connection from a properties file.
@param filename name of file containing property strings
*/
public static RccAcisDataStore createFromFile ( String filename )
throws IOException, Exception
{
    // Read the properties from the file
    PropList props = new PropList ("");
    props.setPersistentName ( filename );
    props.readPersistent ( false );
    String name = IOUtil.expandPropertyForEnvironment("Name",props.getValue("Name"));
    String description = IOUtil.expandPropertyForEnvironment("Description",props.getValue("Description"));
    String serviceRootURI = IOUtil.expandPropertyForEnvironment("ServiceRootURI",props.getValue("ServiceRootURI"));
    
    // Get the properties and create an instance

    RccAcisDataStore ds = new RccAcisDataStore( name, description, new URI(serviceRootURI) );
    ds.setProperties(props);
    return ds;
}

/**
Determine the web service API version.
*/
private void determineAPIVersion()
{   String routine = "RccAcisDataStore.determineAPIVersion";
    __apiVersion = 2; // Default is most current
    String urlString = "" + getServiceRootURI() + "/version";
    try {
        String resultString = IOUtil.readFromURL(urlString);
        // Format of result is JSON like:  "2.0.0-beta.1"
        // Therefore check the 2nd character
        if ( resultString.length() >= 2 ) {
            if ( resultString.charAt(1) == '1' ) {
                __apiVersion = 1;
            }
            else if ( resultString.charAt(1) != '2' ) {
                String message = "ACIS API version is not supported:  " + resultString;
                Message.printWarning ( 2, routine, message );
                throw new RuntimeException ( message );
            }
        }
    }
    catch ( Exception e ) {
        // Might be disconnected from the internet - usually safe to default to latest version
        Message.printWarning ( 2, routine,
            "Error reading version for web service API using \"" + urlString +
            "\" - possibly due to not being connected to internet.  Assuming version is " + __apiVersion );
    }
}

/**
Return the service API version as determined from determineAPIVersion().
@return the API version
*/
public int getAPIVersion ()
{
    return __apiVersion;
}

/**
Return the unique list of data interval strings available for a data type, returning values that
are consistent with TSTool ("Day", rather than "daily").
@param dataType data type string of form "N" or "N - name" or "name", where N is the major number.
*/
public List<String> getDataIntervalStringsForDataType ( String dataType )
{   List<String> dataIntervalStrings = new Vector();
    // For now a data type should only have one interval because of the uniqueness of the data type.
    RccAcisVariableTableRecord variable = lookupVariable(dataType);
    String interval = translateAcisIntervalToInternal(variable.getReportInterval());
    if ( interval != null ) {
        dataIntervalStrings.add(interval);
    }
    return dataIntervalStrings;
}

/**
Return the list of data types that are available.  Currently this returns the major number and optionally
the name.  Duplicates in the table are ignored.
TODO SAM 2011-01-07 It would be good to have the option of using data type abbreviations - work with Bill Noon
on this.
@param includeName whether to include the name
@param includeInterval whether to include the interval as (daily), etc.
*/
public List<String> getDataTypeStrings ( boolean includeName, boolean includeInterval )
{   try {
        initialize();
    }
    catch ( Exception e ) {
        ; // Ignore.
    }
    int version = getAPIVersion();
    List<String> typeList = new Vector();
    RccAcisVariableTableRecord recPrev = null;
    String nameString = "";
    String intervalString = "";
    for ( RccAcisVariableTableRecord rec: __variableTableRecordList ) {
        if ( (typeList.size() > 0) && (rec.getMajor() == recPrev.getMajor()) ) {
            // Same information as previous record so don't add again
            continue;
        }
        nameString = "";
        intervalString = "";
        if ( includeName ) {
            nameString = " - " + rec.getName();
        }
        if ( includeInterval ) {
            intervalString = " (" + rec.getReportInterval() + ")";
        }
        if ( version == 1 ) {
            // Perhaps mistakenly was using the var major for user-facing choices but the
            // abbreviation is used in the REST API so it should be OK to use (do so in version 2+)
            typeList.add( "" + rec.getMajor() + nameString + intervalString );
        }
        else {
            typeList.add( rec.getElem() + nameString + intervalString );
        }
        recPrev = rec;
    }
    return typeList;
}

/**
Initialize internal data store data.
This method should be called from all methods that are likely to be called from external code.
*/
private void initialize ()
throws URISyntaxException, IOException
{
    if ( __initialized ) {
        // Already initialized
        return;
    }
    // Otherwise initialize the global data for the data store
    __initialized = true;
    // Determine the API version from a web request
    if ( getAPIVersion() == 1 ) {
        // Read variables from the HTML file on the website
        readVariableTableVersion1();
    }
    else {
        // TODO SAM 2012-06-25 where does the list of variables come from for the version 2 API?
        // TODO SAM 2012-07-25 Version 2 documentation lists a few more variables so add below
        // 2012-06-26 Bill Noon pointed to test site (http://scacis.rcc-acis.org/ACIS_Builder.html) but this
        // only shows "Common Element Names" - an API call will be made available
        // Hard-code in order of major variable
        // Elem, major, minor, name, method, measInterval, reportInterval, units, source
        // Version 2 uses the element abbreviation and if VarMajor is not known, use a  negative number
        // All VarMajor need to be unique because duplicates are ignored
        //
        // Precipitation...
        __variableTableRecordList.add(new RccAcisVariableTableRecord("pcpn", 4, 1, "Precipitation", "sum",
            "daily", "daily", "Inch", ""));
        // Snow...
        __variableTableRecordList.add(new RccAcisVariableTableRecord("snwd", 11, 1, "Snow depth, at obs time", "inst",
            "inst", "daily", "Inch", ""));
        __variableTableRecordList.add(new RccAcisVariableTableRecord("snow", 10, 1, "Snowfall", "sum",
            "daily", "daily", "Inch", ""));
        // Temperature...
        __variableTableRecordList.add(new RccAcisVariableTableRecord("avgt", 43, 1, "Temperature, average", "ave",
            "daily", "daily", "DegF", ""));
        __variableTableRecordList.add(new RccAcisVariableTableRecord("maxt", 1, 1, "Temperature, maximum", "max",
            "daily", "daily", "DegF", ""));
        __variableTableRecordList.add(new RccAcisVariableTableRecord("mint", 2, 1, "Temperature, minimum", "min",
            "daily", "daily", "DegF", ""));
        __variableTableRecordList.add(new RccAcisVariableTableRecord("obst", 3, 1, "Temperature, at obs time", "inst",
            "inst", "daily", "DegF", ""));
        // Degree days based on temperature...
        __variableTableRecordList.add(new RccAcisVariableTableRecord("hdd", 45, 1, "Heating degree days (base 65)", "sum",
            "daily", "daily", "Day", ""));
        __variableTableRecordList.add(new RccAcisVariableTableRecord("cdd", 44, 1, "Cooling degree days (base 65)", "sum",
            "daily", "daily", "Day", ""));
        __variableTableRecordList.add(new RccAcisVariableTableRecord("gdd50", 9990, 1, "Growing degree days (base 50)", "sum",
            "daily", "daily", "Day", ""));
        __variableTableRecordList.add(new RccAcisVariableTableRecord("gdd40", 9991, 1, "Growing degree days (base 40)", "sum",
            "daily", "daily", "Day", ""));
    }
    // Initialize the station types - this may be available as a service at some point but for now inline
    __stationTypeList.add ( new RccAcisStationType(0,"ACIS","ACIS internal id"));
    __stationTypeList.add ( new RccAcisStationType(1,"WBAN","5-digit WBAN id"));
    __stationTypeList.add ( new RccAcisStationType(2,"COOP","6-digit COOP id"));
    __stationTypeList.add ( new RccAcisStationType(3,"FAA","3-character FAA id"));
    __stationTypeList.add ( new RccAcisStationType(4,"WMO","5-digit WMO id"));
    __stationTypeList.add ( new RccAcisStationType(5,"ICAO","4-character ICAO id"));
    // Note that ACIS documentation calls it GHCN-Daily but daily is the interval so leave out of the station type here
    __stationTypeList.add ( new RccAcisStationType(6,"GHCN","?-character GHCN id"));
    __stationTypeList.add ( new RccAcisStationType(7,"NWSLI","5-character NWSLI"));
    __stationTypeList.add ( new RccAcisStationType(9,"ThreadEx","6-character ThreadEx id"));
    __stationTypeList.add ( new RccAcisStationType(10,"CoCoRaHS","5+ character CoCoRaHS identifier"));
    // AWDN still not officially supported in version 2 (not included in version 2 changelog)?
    __stationTypeList.add ( new RccAcisStationType(16,"AWDN","7-character HPRCC AWDN id"));
}

/**
Look up the station type given the station code (return null if not found).
@param code station code (e.g., 2 for COOP).
*/
public RccAcisStationType lookupStationTypeFromCode ( int code )
{
    for ( RccAcisStationType stationType: __stationTypeList ) {
        if ( stationType.getCode() == code ) {
            return stationType;
        }
    }
    return null;
}

/**
Look up the station type given the station type string (return null if not found).
@param type station type (e.g., "COOP").
*/
public RccAcisStationType lookupStationTypeFromType ( String type )
{   try {
        initialize();
    }
    catch ( Exception e ) {
        // Should not happen
    }
    for ( RccAcisStationType stationType: __stationTypeList ) {
        if ( stationType.getType().equalsIgnoreCase(type) ) {
            return stationType;
        }
    }
    return null;
}

/**
Look up the variable information given the data type, which is a string of the form
"N" or "N - Name" or "Name" where N is the "major" (if an integer, for version 1 API in TSTool) or "elem" if a string
(version 2+ API in TSTool).
@param dataType data type to match in variable data.
@return the ACIS variable that matches the data type.
*/
public RccAcisVariableTableRecord lookupVariable ( String dataType )
{   String elemString = null;
    String name = null;
    int pos = dataType.indexOf("-");
    if ( pos > 0) {
        // Assume that the elem or major variable is specified and also the name
        elemString = dataType.substring(0,pos).trim();
        name = dataType.substring(pos + 1).trim();
    }
    else {
        // Only one piece of information specified, such as the data type from a TSID.
        if ( StringUtil.isInteger(dataType) ) {
            // Actually the varMajor
            elemString = dataType.trim();
        }
        else {
            elemString = dataType.trim();
            name = elemString;
        }
    }
    if ( elemString != null ) {
        if ( StringUtil.isInteger(elemString) ) {
            // Use the major number to look up the variable
            int majorInt = -1;
            try {
                majorInt = Integer.parseInt(elemString);
            }
            catch ( NumberFormatException e ) {
                throw new InvalidParameterException("Data type \"" + dataType + "\" major number is invalid." );
            }
            for ( RccAcisVariableTableRecord variable: __variableTableRecordList ) {
                if ( variable.getMajor() == majorInt ) {
                    return variable;
                }
            }
        }
        else {
            // Use the element to look up the variable
            for ( RccAcisVariableTableRecord variable: __variableTableRecordList ) {
                if ( variable.getElem().equalsIgnoreCase(elemString) ) {
                    return variable;
                }
            }
        }
    }
    else if ( name != null ){
        // Use the variable name to look up the variable
        for ( RccAcisVariableTableRecord variable: __variableTableRecordList ) {
            if ( variable.getName().equalsIgnoreCase(name) ) {
                return variable;
            }
        }
    }
    // No match...
    return null;
}

/**
Read a list of MultiStn data records.
*/
public List<RccAcisStationTimeSeriesMetadata> readStationTimeSeriesMetadataList(
    String dataType, String timeStep, InputFilter_JPanel ifp )
throws IOException, MalformedURLException, URISyntaxException
{   String routine = getClass().getName() + ".readStationTimeSeriesMetadataList";
    // Make sure data store is initialized
    initialize();
    // Look up the metadata for the data types
    RccAcisVariableTableRecord variable = lookupVariable ( dataType );
    int apiVersion = getAPIVersion();
    if ( apiVersion == 1 ) {
        return readStationTimeSeriesMetadataListVersion1 ( dataType, timeStep, ifp );
    }
    // Else, use the current version 2 API, which uses the StnMeta call instead of version 1 MultiStnData call
    // Form the URL - ask for as much metadata as possible
    StringBuffer urlString = new StringBuffer("" + getServiceRootURI() +
        "/StnMeta?meta=sIds,uid,name,state,county,basin,climdiv,cwa,ll,elev,valid_daterange" );
    // Specify constraints from input filter
    // Element being read (currently only one data type per call)...
    urlString.append("&elems="+variable.getElem());
    // Bounding box...
    List<String> bboxList = ifp.getInput(null, "bbox", true, null);
    if ( bboxList.size() > 1 ) {
        throw new IOException ( "<= 1 bounding box filters can be specified." );
    }
    else if ( bboxList.size() == 1 ) {
        String bbox = bboxList.get(0).trim();
        urlString.append("&bbox="+URLEncoder.encode(bbox,"UTF-8"));
    }
    // Climate division (always use newest syntax in filter)...
    List<String> climdivList = ifp.getInput(null, "climdiv", true, null);
    if ( climdivList.size() > 1 ) {
        throw new IOException ( "<= 1 climate division filters can be specified." );
    }
    else if ( climdivList.size() == 1 ) {
        String climdiv = climdivList.get(0).split("-")[0].trim();
        urlString.append("&climdiv="+URLEncoder.encode(climdiv,"UTF-8"));
    }
    // FIPS county...
    List<String> countyList = ifp.getInput(null, "county", true, null);
    if ( countyList.size() > 1 ) {
        throw new IOException ( "<= 1 FIPS county filters can be specified." );
    }
    else if ( countyList.size() == 1 ) {
        String county = countyList.get(0).split("-")[0].trim();
        urlString.append("&county="+URLEncoder.encode(county,"UTF-8"));
    }
    // NWS CWA...
    List<String> cwaList = ifp.getInput(null, "cwa", true, null);
    if ( cwaList.size() > 1 ) {
        throw new IOException ( "<= 1 NWS CWA county filters can be specified." );
    }
    else if ( cwaList.size() == 1 ) {
        String cwa = cwaList.get(0).trim();
        urlString.append("&cwa="+URLEncoder.encode(cwa,"UTF-8"));
    }
    // Drainage basin (HUC)...
    List<String> basinList = ifp.getInput(null, "basin", true, null);
    if ( basinList.size() > 1 ) {
        throw new IOException ( "<= 1 basin filters can be specified." );
    }
    else if ( basinList.size() == 1 ) {
        String basin = basinList.get(0).trim();
        urlString.append("&basin="+URLEncoder.encode(basin,"UTF-8"));
    }
    // State code...
    List<String> stateList = ifp.getInput(null, "state", true, null);
    if ( stateList.size() > 1 ) {
        throw new IOException ( "<= 1 state code filters can be specified." );
    }
    else if ( stateList.size() == 1 ) {
        String state = stateList.get(0).split("-")[0].trim();
        urlString.append("&state="+URLEncoder.encode(state,"UTF-8"));
    }
    // Always want JSON results...
    urlString.append("&output=json");
    Message.printStatus(2, routine, "Performing the following request:  " + urlString );
    String resultString = IOUtil.readFromURL(urlString.toString() );
    if ( Message.isDebugOn ) {
        Message.printDebug(1,routine,"Returned data="+resultString);
    }
    // Check for error in response string...
    if ( resultString.indexOf("error") >= 0 ) {
        throw new IOException ( "Error retrieving data for URL \"" + urlString + "\":  " + resultString );
    }
    else {
        // Parse the JSON
        Gson gson = new Gson();
        RccAcisStationTimeSeriesMetadataList metadataListObject =
            gson.fromJson(resultString, RccAcisStationTimeSeriesMetadataList.class);
        if ( metadataListObject != null ) {
            for ( RccAcisStationTimeSeriesMetadata metadata: metadataListObject.getMeta() ) {
                //Message.printStatus(2,routine,metadata.getName());
                metadata.setVariable(variable);
                metadata.setDataStore ( this );
                // TODO SAM 2011-01-07 Some metadata like HUC do not return so may need to set based
                // on whether the information was entered in the filter
            }
            // Remove records that have no period
            try {
                metadataListObject.cleanupData();
            }
            catch ( Exception e ) {
                Message.printWarning(3, routine, e);
            }
            return metadataListObject.getMeta();
        }
        else {
            // Return an empty list
            List<RccAcisStationTimeSeriesMetadata> data = new Vector();
            return data;
        }
    }
}

/**
Read a list of RccAcisStationTimeSeriesMetadata data records for the Version 1 RCS ACIS API (uses MultiStn).
*/
public List<RccAcisStationTimeSeriesMetadata> readStationTimeSeriesMetadataListVersion1(
    String dataType, String timeStep, InputFilter_JPanel ifp )
throws IOException, MalformedURLException
{   String routine = getClass().getName() + ".readStationTimeSeriesMetadataListVersion1";
    // Look up the metadata for the data types
    RccAcisVariableTableRecord variable = lookupVariable ( dataType );
    // Form the URL - ask for as much metadata as possible
    StringBuffer urlString = new StringBuffer("" + getServiceRootURI() +
        "/MultiStnData?meta=sIds,uid,name,postal,county,ll,elev,valid_daterange" );
    // To deal with slow performance, the following was done (only needed with version 1):
    //    <pre>
    //    Date:   Thu, 25 Aug 2011 11:00:57 -0400
    //    From:   Keith Eggleston <kle1@cornell.edu>
    //    If you don't need to get the AWDN sites, Bill has instituted a patch to ignore them, thus speeding up access to the other stations. Here's the note I received:
    //
    //    "As a temporary work-around for the AWDN problems I have added a parameter to the MultiStnData call that will ignore any AWDN stations.
    //    Just add a parameter 'no_awdn' to the parameter list:
    //    curl "http://data.rcc-acis.org/MultiStnData?postal=NE&elems=1,2&meta=sIds,name&no_awdn=1&date=20110801&output=json"
    //    --Bill"
    //    </pre>
    urlString.append("&no_awdn=1");
    // Specify constraints from input filter
    // Element being read...
    urlString.append("&elems="+variable.getMajor());
    // Bounding box...
    List<String> bboxList = ifp.getInput(null, "bbox", true, null);
    if ( bboxList.size() > 1 ) {
        throw new IOException ( "<= 1 bounding box filters can be specified." );
    }
    else if ( bboxList.size() == 1 ) {
        String bbox = bboxList.get(0).trim();
        urlString.append("&bbox="+URLEncoder.encode(bbox,"UTF-8"));
    }
    // Climate division (always use newest syntax in filter)...
    List<String> climdivList = ifp.getInput(null, "climdiv", true, null);
    if ( climdivList.size() > 1 ) {
        throw new IOException ( "<= 1 climate division filters can be specified." );
    }
    else if ( climdivList.size() == 1 ) {
        String climdiv = climdivList.get(0).split("-")[0].trim();
        urlString.append("&clim_div="+URLEncoder.encode(climdiv,"UTF-8"));
    }
    // FIPS county...
    List<String> countyList = ifp.getInput(null, "county", true, null);
    if ( countyList.size() > 1 ) {
        throw new IOException ( "<= 1 FIPS county filters can be specified." );
    }
    else if ( countyList.size() == 1 ) {
        String county = countyList.get(0).split("-")[0].trim();
        urlString.append("&county="+URLEncoder.encode(county,"UTF-8"));
    }
    // NWS CWA...
    List<String> cwaList = ifp.getInput(null, "cwa", true, null);
    if ( cwaList.size() > 1 ) {
        throw new IOException ( "<= 1 NWS CWA county filters can be specified." );
    }
    else if ( cwaList.size() == 1 ) {
        String cwa = cwaList.get(0).trim();
        urlString.append("&cwa="+URLEncoder.encode(cwa,"UTF-8"));
    }
    // Drainage basin (HUC)...
    List<String> basinList = ifp.getInput(null, "basin", true, null);
    if ( basinList.size() > 1 ) {
        throw new IOException ( "<= 1 basin filters can be specified." );
    }
    else if ( basinList.size() == 1 ) {
        String basin = basinList.get(0).trim();
        urlString.append("&basin="+URLEncoder.encode(basin,"UTF-8"));
    }
    // State code...
    List<String> stateList = ifp.getInput(null, "state", true, null);
    if ( stateList.size() > 1 ) {
        throw new IOException ( "<= 1 state code filters can be specified." );
    }
    else if ( stateList.size() == 1 ) {
        String state = stateList.get(0).split("-")[0].trim();
        urlString.append("&postal="+URLEncoder.encode(state,"UTF-8"));
    }
    // Always want JSON results...
    urlString.append("&output=json");
    Message.printStatus(2, routine, "Performing the following request:  " + urlString );
    String resultString = IOUtil.readFromURL(urlString.toString());
    if ( Message.isDebugOn ) {
        Message.printDebug(1,routine,"Returned data="+resultString);
    }
    if ( resultString.indexOf("error") >= 0 ) {
        throw new IOException ( "Error retrieving data for URL \"" + urlString + "\":  " + resultString );
    }
    else {
        // Parse the JSON
        Gson gson = new Gson();
        RccAcisStationTimeSeriesMetadataList metadataListObject =
            gson.fromJson(resultString, RccAcisStationTimeSeriesMetadataList.class);
        if ( metadataListObject != null ) {
            if ( metadataListObject.getMeta() == null ) {
                Message.printStatus(2,routine,"data is null");
            }
            for ( RccAcisStationTimeSeriesMetadata metadata: metadataListObject.getMeta() ) {
                //Message.printStatus(2,routine,metadata.getName());
                metadata.setVariable(variable);
                metadata.setDataStore ( this );
                // TODO SAM 2011-01-07 Some metadata like HUC do not return so may need to set based
                // on whether the information was entered in the filter
            }
            // Remove records that have no period
            try {
                metadataListObject.cleanupData();
            }
            catch ( Exception e ) {
                Message.printWarning(3, routine, e);
            }
            return metadataListObject.getMeta();
        }
        else {
            // Return an empty list
            List<RccAcisStationTimeSeriesMetadata> data = new Vector();
            return data;
        }
    }
}

/**
Read a time series.  Only one element type is read.
@param tsidentString the time series identifier string as per TSTool conventions.  The location should be
LocationType:ID (e.g., COOP:1234).  The data type should be the variable name (might implement abbreviation later but
trying to stay away from opaque variable major).
@param readStart the starting date/time to read, or null to read all data.
@param readEnd the ending date/time to read, or null to read all data.
@param readData if true, read the data; if false, construct the time series and populate properties but do
not read the data
@return the time series read from the ACIS web services
*/
public TS readTimeSeries ( String tsidentString, DateTime readStart, DateTime readEnd, boolean readData )
throws MalformedURLException, Exception
{   // Make sure data store is initialized
    initialize();
    TS ts = null;
    String routine = getClass().getName() + ".readTimeSeries";
    TSIdent tsident = TSIdent.parseIdentifier(tsidentString);
    int intervalBase = tsident.getIntervalBase();
    // Look up the metadata for the data name
    RccAcisVariableTableRecord variable = lookupVariable ( tsident.getType() );
    if ( variable == null ) {
        throw new IllegalArgumentException("Data type is not recognized:  " + tsident.getType() );
    }
    int apiVersion = getAPIVersion();
    // The station ID needs to specify the location type...
    String stationIDAndStationType = readTimeSeries_FormHttpRequestStationID ( tsident.getLocationType(), tsident.getLocation() );
    // The start and end date are required.
    String readStartString = "por";
    String readEndString = "por";
    if ( !readData ) {
        // Specify a minimal period to try a query and make sure that the time series is defined.
        // If this period is not valid for the time series, a missing value will come back.
        // TODO SAM 2012-06-28 Evaluate whether this impacts the number of stations returned since not all
        // will be active on the requested day
        readStart = DateTime.parse("2011-01-01");
        readEnd = DateTime.parse("2011-01-01");
    }
    // TODO SAM 2011-01-21 ACIS always seems to want precision to day on request
    if ( readStart != null ) {
        //if ( intervalBase == TimeInterval.DAY ) {
            readStartString = readStart.toString(DateTime.FORMAT_YYYY_MM_DD);
        //}
        //else if ( intervalBase == TimeInterval.MONTH ) {
        //    readStartString = readStart.toString(DateTime.FORMAT_YYYY_MM);
        //}
        //else if ( intervalBase == TimeInterval.HOUR ) {
        //    readStartString = readStart.toString(DateTime.FORMAT_YYYY_MM_DD_HH);
        //} 
        //else {
        //    readStartString = readStart.toString();
        //}
    }
    if ( readEnd != null ) {
        //if ( intervalBase == TimeInterval.DAY ) {
            readEndString = readEnd.toString(DateTime.FORMAT_YYYY_MM_DD);
        //}
        //else if ( intervalBase == TimeInterval.MONTH ) {
        //    readEndString = readEnd.toString(DateTime.FORMAT_YYYY_MM);
        //}
        //else if ( intervalBase == TimeInterval.HOUR ) {
        //    readEndString = readEnd.toString(DateTime.FORMAT_YYYY_MM_DD_HH);
        //}
        //else {
        //    readEndString = readEnd.toString();
        //}
    }
    // Only one data type is requested as per readTimeSeries() conventions
    String elems = "";
    if ( apiVersion == 1 ) {
        // Version 1 uses var major number
        // Note this may cause an error if VarMajor is not known, but since the Version 1 API is obsolete
        // this hopefully should not be an issue.
        elems = "" + variable.getMajor();
    }
    else {
        // Version 2 uses element name or var major (but use element name because some data types don't
        // seem to have documented var major)
        elems = "" + variable.getElem();
    }
    // Form the URL - no need to ask for metadata?
    // Always specify the station id type to avoid ambiguity
    boolean requestJSON = true; // JSON more work to parse, CSV is verified to work
    if ( apiVersion == 1 ) {
        // Version 1 worked with CSV so leave it as is
        // For version 2 can focus on new features in JSON
        requestJSON = false;
    }
    StringBuffer urlString = new StringBuffer("" + getServiceRootURI() + "/StnData" );
    if ( requestJSON ) {
        urlString.append("?output=json");
    }
    else {
        // Request CSV
        urlString.append("?output=csv");
    }
    // Only JSON format allows metadata to be requested
    // Version 1 requires sId... whereas version 2 is case-independent
    if ( requestJSON ) {
        urlString.append( "&meta=sIds,uid,name,state,county,basin,climdiv,cwa,ll,elev,valid_daterange" );
    }
    urlString.append( "&sId=" +
         URLEncoder.encode(stationIDAndStationType,"UTF-8") +
         "&elems=" + elems + "&sDate=" + readStartString + "&eDate=" + readEndString );
    String urlStringEncoded = urlString.toString(); //URLEncoder.encode(urlString.toString(),"UTF-8");
    Message.printStatus(2, routine, "Performing the following request:  " + urlStringEncoded );
    String resultString = IOUtil.readFromURL(urlStringEncoded);
    //Message.printStatus(2,routine,"Returned string="+resultString);
    if ( Message.isDebugOn ) {
        Message.printDebug(1,routine,"Returned string="+resultString);
    }
    if ( resultString.indexOf("error") >= 0 ) {
        throw new IOException ( "Error retrieving data for URL \"" + urlStringEncoded +
            "\":  " + resultString );
    }
    else {
        RccAcisStationTimeSeriesMetaAndData metaAndData = null;
        if ( requestJSON ) {
            // Parse the JSON for time series ("meta" and "data)
            Gson gson = new Gson();
            metaAndData = gson.fromJson(resultString, RccAcisStationTimeSeriesMetaAndData.class);
            // Should only be one record since a specific time series is requested
            if ( metaAndData == null ) {
                throw new IOException ( "Expecting metadata for 1 time series, no metadata returned." );
            }
            else {
                metaAndData.getMeta().setVariable(variable);
                metaAndData.getMeta().setDataStore ( this );
            }
        }
        else {
            // No metadata for CSV.  Get the station name from the first line of data
        }
        // Create the time series.
        ts = TSUtil.newTimeSeries(tsidentString, true);
        ts.setIdentifier(tsidentString);
        ts.setMissing(Double.NaN);// Use this instead of legacy default -999
        // Parse the data into short strings
        String [] dataStringsArray = new String[0];
        DateTime dataStart = null; // Determined from data records
        DateTime dataEnd = null;
        DateTime validDataStart = null; // Determined from valid_daterange metadata (only if JSON)
        DateTime validDataEnd = null;
        String stationName = "";
        int mCount = 0;
        int tCount = 0;
        int commaPos = 0; // Position of comma
        // Set the time series properties from returned data
        String [][] data = null;
        int nData = 0; // Number of records to process
        int iFirstData = 0; // Index of first data record to process
        if ( requestJSON ) {
            // Set time series properties from the metadata
            stationName = metaAndData.getMeta().getName();
            data = metaAndData.getData();
            nData = data.length; // Number of rows in 2D array
            iFirstData = 0;
            Message.printStatus(2, routine, "Have " + nData + " data records." );
            if ( nData > 0 ) {
                dataStart = DateTime.parse(data[iFirstData][0]);
                dataEnd = DateTime.parse(data[nData - 1][0]);
            }
            // Also get the valid data start and end from the metadata
            validDataStart = DateTime.parse(metaAndData.getMeta().getValid_daterange()[0][0]);
            validDataEnd = DateTime.parse(metaAndData.getMeta().getValid_daterange()[0][1]);
        }
        else {
            // Used by default with version 1 API...
            // CSV, each newline delimited row has YYYY-MM-DD,valueFlag
            // (Flag character is optional) with the first line being the station name
            dataStringsArray = resultString.split("\n");
            Message.printStatus(2, routine, "Have " + dataStringsArray.length + " data records (first is station name)." );
            nData = dataStringsArray.length;
            iFirstData = 1; // Station name is record 1
            if ( nData > 1 ) {
                stationName = dataStringsArray[0];
                commaPos = dataStringsArray[1].indexOf(",");
                dataStart = DateTime.parse(dataStringsArray[1].substring(0,commaPos));
                commaPos = dataStringsArray[dataStringsArray.length - 1].indexOf(",");
                dataEnd = DateTime.parse(dataStringsArray[dataStringsArray.length - 1].substring(0,commaPos));
                // Since CSV does not have metadata and not doing a round-trip would be a hit, use the same dates
                validDataStart = new DateTime(dataStart);
                validDataEnd = new DateTime(dataEnd);
            }
        }
        ts.setDataUnits(variable.getUnits());
        ts.setDataUnitsOriginal(variable.getUnits());
        ts.setDescription(stationName);
        boolean setPropertiesFromMetadata = true;
        if ( setPropertiesFromMetadata ) {
            // Set time series properties from the station metadata
            if ( metaAndData != null ) {
                // Get metadata from the object and set as properties, using the JSON property names
                RccAcisStationTimeSeriesMetadata meta = metaAndData.getMeta();
                ts.setProperty("uid", (meta.getUid() == null) ? "" : meta.getUid() );
                ts.setProperty("name", (meta.getName() == null) ? "" : meta.getName() );
                ts.setProperty("county", (meta.getCounty() == null) ? "" : meta.getCounty() );
                ts.setProperty("basin", (meta.getBasin() == null) ? "" : meta.getBasin() );
                ts.setProperty("climdiv", (meta.getClimdiv() == null) ? "" : meta.getClimdiv() );
                ts.setProperty("cwa", (meta.getCwa() == null) ? "" : meta.getCwa() );
                ts.setProperty("state", (meta.getState() == null) ? "" : meta.getState() );
                ts.setProperty("elev", new Double(meta.getElev()) );
                double [] ll = meta.getLl();
                if ( (ll != null) && (ll.length == 2) ) {
                    ts.setProperty("longitude", new Double(ll[0]) );
                    ts.setProperty("latitude", new Double(ll[1]) );
                }
                String [] sids = meta.getSids();
                if ( sids != null ) {
                    // Set each identifier for the different station types
                    for ( int i = 0; i < sids.length; i++ ) {
                        // Each string is "ID type"
                        String [] parts = sids[i].split(" ");
                        if ( (parts != null) && (parts.length == 2) ) {
                            // Set a property "ID-type"
                            RccAcisStationType stype = lookupStationTypeFromCode(Integer.parseInt(parts[1]));
                            if ( stype != null ) {
                                ts.setProperty("ID-" + stype.getType(), parts[0] );
                            }
                        }
                    }
                }
                // Also set the preferred identifier with location type so it can be used to read the time series
                ts.setProperty("IDWithLocType", meta.getIDPreferred(true));
                // Valid dates will only have two dates since one element was requested
                String [][] validDates = meta.getValid_daterange();
                if ( validDates.length > 0 ) {
                    if ( validDates[0].length == 2 ) {
                        ts.setProperty("valid_daterange1", validDates[0][0] );
                        ts.setProperty("valid_daterange2", validDates[0][1] );
                    }
                }
            }
        }
        // Since there is no way currently to retrieve the separate periods, set both to what was retrieved.
        ts.setDate1(dataStart);
        ts.setDate2(dataEnd);
        ts.setDate1Original(validDataStart);
        ts.setDate2Original(validDataEnd);
        DateTime date = null;
        String [] dataStringParts = null;
        String dateString = ""; // string containing the date
        String valueString = ""; // string containing value and optionally flag part of data
        String flagString; // string containing flag part of data (split out of "valueString")
        int valueStringLength;
        // Nolan Doesken and Bill Noon indicate that 0 is what people use for trace
        double traceValue = 0.0;
        double missing = ts.getMissing(); // Will be Double.NaN, based on initialization
        if ( readData ) {
            ts.allocateDataSpace();
            // Process each data string.  Trace values result in setting the data flag.
            for ( int i = iFirstData; i < nData; i++ ) {
                try {
                    if ( requestJSON ) {
                        dateString = data[i][0];
                        valueString = data[i][1];
                    }
                    else {
                        // CSV
                        dataStringParts = dataStringsArray[i].split(",");
                        dateString = dataStringParts[0];
                        valueString = dataStringParts[1];
                    }
                    //Message.printStatus(2,routine,"Date="+dateString+", value="+valueString);
                    date = DateTime.parse(dateString);
                    valueStringLength = valueString.length();
                    if ( valueString.equals("M") ) {
                        // No value and missing flag.  Do set a flag since ACIS specific sets a flag
                        ts.setDataValue(date, missing, "M", 0 );
                        ++mCount;
                    }
                    else if ( valueString.equals("T") ) {
                        // No value and trace flag.  Do set a flag since ACIS specific sets a flag
                        ts.setDataValue(date, traceValue, "T", 0 );
                        ++tCount;
                    }
                    // Check for data string form ##F or ##F1 (two one-character flags may occur, with
                    // the second flag possibly being a character or digit)
                    else if ( (valueString.length() > 0) &&
                        Character.isLetter(valueString.charAt(valueStringLength - 1)) ) {
                        flagString = valueString.substring(valueStringLength - 1);
                        valueString = valueString.substring(0,valueStringLength - 1);
                        if ( valueString.length() > 0 ) {
                            ts.setDataValue(date, Double.parseDouble(valueString), flagString, 0 );
                        }
                        else {
                            // Only flag was available
                            ts.setDataValue(date, missing, flagString, 0 );
                        }
                    }
                    else if ( (valueString.length() > 1) &&
                        Character.isLetter(valueString.charAt(valueStringLength - 2)) ) {
                        flagString = valueString.substring(valueStringLength - 2);
                        valueString = valueString.substring(0,valueStringLength - 2);
                        ts.setDataValue(date, Double.parseDouble(valueString), flagString, 0 );
                        if ( valueString.length() > 0 ) {
                            ts.setDataValue(date, Double.parseDouble(valueString), flagString, 0 );
                        }
                        else {
                            // Only flag was available
                            ts.setDataValue(date, missing, flagString, 0 );
                        }
                    }
                    else {
                        // Just the data value
                        ts.setDataValue(date,Double.parseDouble(valueString));
                    }
                }
                catch ( NumberFormatException e ) {
                    Message.printWarning(3,routine,"Error parsing data point date=" + dateString + " valueString=\"" +
                        valueString + "\" (" + e + ") - treating as flagged data.");
                    // TODO SAM 2011-04-04 Have seen data values like "S", "0.20A".  Should these be
                    // considered valid data points or treated as missing because they failed some test?
                    // Submitted an email request to the ACIS contact page to see if I can get an answer.
                    // For now, strip the characters off the end and treat as the flag and use the numerical
                    // part (if present) for the value.
                    int lastDigitPos = -1;
                    for ( int iChar = valueString.length() - 1; iChar >= 0; iChar-- ) {
                        if ( Character.isDigit(valueString.charAt(iChar)) ) {
                            lastDigitPos = iChar;
                            break;
                        }
                    }
                    if ( lastDigitPos >= 0 ) {
                        String number = valueString.substring(0,lastDigitPos);
                        if ( StringUtil.isDouble(number) ) {
                            ts.setDataValue(date,Double.parseDouble(number),
                                valueString.substring(lastDigitPos + 1),0);
                        }
                        else {
                            // Set the entire string as the flag
                            ts.setDataValue(date,ts.getMissing(),valueString,0);
                        }
                    }
                    else {
                        ts.setDataValue(date,ts.getMissing(),valueString,0);
                    }
                }
                catch ( Exception e ) {
                    Message.printWarning(3,routine,"Error parsing data point date=" + dateString + ", valueString=\"" +
                        valueString + "\" (" + e + ").");
                }
            }
        }
        if ( tCount > 0 ) {
            // Add a specific data flag type 
            ts.addDataFlagMetadata(new TSDataFlagMetadata("T", "Trace - value of " + traceValue + " is used."));
        }
        if ( mCount > 0 ) {
            // Add a specific data flag type 
            ts.addDataFlagMetadata(new TSDataFlagMetadata("M", "Missing value."));
        }
    }
    return ts;
}

/**
Form the station ID string part of the time series request, something like "Type:ID" (e.g., "GHCN:USC00016643"),
where ID is the station.
@param tsidLocationType the location type part of a time series identifier (station ID network abbreviation).
@param tsidLocation the location part of a time series identifier (station ID).
*/
private String readTimeSeries_FormHttpRequestStationID ( String tsidLocationType, String tsidLocation )
{
    if ( tsidLocationType.length() == 0 ) {
        throw new InvalidParameterException ( "Station location type abbreviation is not specified." );
    }
    try {
        RccAcisStationType stationType = lookupStationTypeFromType(tsidLocationType.trim());
        if ( stationType == null ) {
            throw new InvalidParameterException ( "Station code from \"" + tsidLocation +
                "\" cannot be determined." );
        }
        else if ( stationType.getCode() == 0 ) {
            // No station type code is expected since the ACIS type, just pass ACIS ID
            return tsidLocation.trim();
        }
        else {
            // Station ID followed by the station type code
            return tsidLocation.trim() + " " + stationType.getCode();
        }
    }
    catch ( NumberFormatException e ) {
        throw new InvalidParameterException ( "Station location \"" + tsidLocation +
        "\" is invalid (should be Type:ID)" );
    }
}

/**
Make a request to get the variable table, which is used to map ACIS data to time series available in TSTool.
This approach is used with the version 1 API.
The table comes back as HTML so need to use a DOM and extract cells.
TODO SAM 2011-01-08 Need Bill Noon to implement an API to get the variable table.
*/
private void readVariableTableVersion1 ()
throws URISyntaxException, IOException
{   String routine = getClass().getName() + ".readVariableTableVersion1";
    // Get the variable table file
    String urlString = "" + getServiceRootURI() + "/doc/VariableTable.html";
    StringBuffer b = new StringBuffer ( IOUtil.readFromURL(urlString) );
    // TODO SAM 2011-01-11 Need to perhaps use standalone="yes" rather than using a DTD when parsing the HTML
    // Prepend the DTD information so document parsing/verification will work properly
    // And content does not have html tag at start?
    // The commented versions did not work (asking for > at end of HTML.Version, etc.)
    // In the end probably could have just defined the entity for nbsp, etc. and not specified a DTD
    boolean useDTD = false;
    if ( useDTD ) {
        b.insert(0, //"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            //"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"\n"
            //+ "\"http://www.w3.org/TR/html4/strict.dtd\">\n" );
            //"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n"
            //+ "\"http://www.w3.org/TR/html4/loose.dtd\">\n" );
            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML Basic 1.1//EN\" \n" +
                "\"http://www.w3.org/TR/xhtml-basic/xhtml-basic11.dtd\">\n" );
    }
    else {
        // Just handle the specific elements that cause trouble
        b.insert(0, "<?xml version=\"1.0\"?>\n" +
                    "<!DOCTYPE some_name [\n" +
                    "<!ENTITY nbsp \"&#160;\">\n" +
                    "]>" ); 
    }
   
    // Page seems to have extra <table> at start
    // Units does not have leading <td>
    String tableString = b.toString().
        replace("<body>", "<head></head><body>"). // Extra <table> at front
        replace("<table>", ""). // Extra <table> at front
        replace("height=\"30\"",""). // height not supported by DTD
        replace("Units</td>", "Units</th>").
        replace("Source</td><tr>", "Source</th></tr>").
        replace("<br>","<br/>");
    if ( Message.isDebugOn ) {
        Message.printStatus(2, routine, "Variable table:" + tableString );
        System.setProperty ( "jaxp.debug", "1" );
    }
    // Not sure why this does not get found since Java runtime jar should be before Xerces.jar?
    // Anyhow, specify here so that the older xerces.jar used with StateMod network files is not used.
    // Need to phase out the xerces.jar use.
    System.setProperty ( "javax.xml.parsers.DocumentBuilderFactory",
        "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    Message.printStatus(2, routine, "DOM factory is \"" + domFactory.getClass().getName() + "\"" );
    //domFactory.setNamespaceAware(true);
    domFactory.setValidating(false);
    /*
    try {
        // Was getting: org.xml.sax.SAXParseException: The declaration for the entity "HTML.Version" must end with '>'.
        // See:  http://stackoverflow.com/questions/2226819/error-accessing-w3-org-when-applying-a-xslt
        domFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }
    catch ( ParserConfigurationException e ) {
        throw new IOException ( e );
    }
    */
    //domFactory.setIgnoringElementContentWhitespace(true);
    DocumentBuilder builder = null;
    try {
        builder = domFactory.newDocumentBuilder();
        Message.printStatus(2, routine, "DOM builder is \"" + builder.getClass().getName() + "\"" );
    }
    catch ( ParserConfigurationException e ) {
        throw new IOException ( e );
    }
    Document doc = null;
    try {
        doc = builder.parse(new ReaderInputStream(new StringReader(tableString)));
        // Spit it out..
        // Code from:  http://stackoverflow.com/questions/139076/how-to-pretty-print-xml-from-java
        /*
        OutputFormat format = new OutputFormat(doc);
        format.setLineWidth(65);
        format.setIndenting(true);
        format.setIndent(2);
        Writer out = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(out, format);
        serializer.serialize(doc);
        Message.printStatus(2, routine, "Document after parse: " + out.toString() );
        */
    }
    catch ( SAXException e ) {
        throw new IOException ( e );
    }
    XPath xpath = XPathFactory.newInstance().newXPath();
    boolean doXpath = false;
    try {
        // Extract the rows of the table
        NodeList nodes = null;
        if ( doXpath ) {
            XPathExpression expr = xpath.compile("//table//td");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            nodes = (NodeList)result;
        }
        else {
            // Just use the document
            nodes = doc.getElementsByTagName("td");
        }
        //Message.printStatus(2,routine, "Have " + nodes.getLength() + " <td> elements");
        int major = -1, minor = -1;
        // Note "elem" corresponding to web service API is not in the HTML file so use blank
        // "elem" is initialized for the version 2 API
        String elem = "", name = "", method = "", measureInterval = "", reportInterval = "", units = "", source = "";
        int imod9;
        String nodeContent;
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            nodeContent = node.getTextContent().trim();
            //Message.printStatus(2, routine, "Node text content=\"" + nodeContent + "\"" );
            imod9 = i%9;
            if ( imod9 == 0 ) {
                major = -1;
                try {
                    major = Integer.parseInt(nodeContent);
                }
                catch ( NumberFormatException e ) {
                    // Just don't set
                }
            }
            else if ( imod9 == 1 ) {
                minor = -1;
                try {
                    minor = Integer.parseInt(nodeContent);
                }
                catch ( NumberFormatException e ) {
                    // Just don't set
                }
            }
            else if ( imod9 == 2 ) {
                name = nodeContent;
            }
            else if ( imod9 == 3 ) {
                method = nodeContent;
            }
            else if ( imod9 == 4 ) {
                measureInterval = nodeContent;
            }
            else if ( imod9 == 5 ) {
                reportInterval = nodeContent;
            }
            else if ( imod9 == 6 ) {
                units = nodeContent;
            }
            else if ( imod9 == 7 ) {
                source = nodeContent;
            }
            else if ( imod9 == 8 ) {
                // Last column in the table is blank
                // TODO SAM 2011-03-31 Only include daily data as per Bill Noon - need to figure out aggregations
                if ( reportInterval.equalsIgnoreCase("DAILY") ) {
                    __variableTableRecordList.add(new RccAcisVariableTableRecord(elem, major, minor, name, method,
                           measureInterval, reportInterval, units, source));
                }
            }
        }
    }
    catch ( Exception e ) { //XPathExpressionException e ) {
        throw new IOException ( e );
    }
}

/**
Translate RCS ACIS time intervals to internal (e.g., "daily" becomes "Day").
@param acisInterval ACIS interval to translate
@return internal interval equivalent, or null if not matched
*/
public String translateAcisIntervalToInternal ( String acisInterval )
{
    if ( acisInterval.equalsIgnoreCase("sub-hrly")) {
        return "Irregular";
    }
    else if ( acisInterval.equalsIgnoreCase("hrly") || acisInterval.equalsIgnoreCase("hourly") ) {
        return "Hour";
    }
    else if ( acisInterval.equalsIgnoreCase("3 hrly")) {
        return "3Hour";
    }
    else if ( acisInterval.equalsIgnoreCase("6 hrly")) {
        return "6Hour";
    }
    else if ( acisInterval.equalsIgnoreCase("daily")) {
        return "Day";
    }
    else if ( acisInterval.equalsIgnoreCase("weekly")) {
        // TODO SAM 2011-01-08 Evaluate whether/how to implement weekly time series
        return "Irregular";
    }
    else if ( acisInterval.equalsIgnoreCase("monthly")) {
        return "Month";
    }
    else {
        return null;
    }
}

}