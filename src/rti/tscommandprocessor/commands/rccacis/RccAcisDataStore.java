package rti.tscommandprocessor.commands.rccacis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
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
Data store for NCDC ACIS web services.  This class maintains the web service information in a general way.
To deal with slow performance, the following was done:
<pre>
Date:   Thu, 25 Aug 2011 11:00:57 -0400
From:   Keith Eggleston <kle1@cornell.edu>
If you don't need to get the AWDN sites, Bill has instituted a patch to ignore them, thus speeding up access to the other stations. Here's the note I received:

"As a temporary work-around for the AWDN problems I have added a parameter to the MultiStnData call that will ignore any AWDN stations.
Just add a parameter 'no_awdn' to the parameter list:
curl "http://data.rcc-acis.org/MultiStnData?postal=NE&elems=1,2&meta=sIds,name&no_awdn=1&date=20110801&output=json"
--Bill"
</pre>
@author sam
*/
public class RccAcisDataStore extends AbstractWebServiceDataStore
{
    
/**
The records of table variables, read from:  http://data.rcc-acis.org/doc/VariableTable.html
*/
private List<RccAcisVariableTableRecord> __variableTableRecordList = new Vector();

/**
The station codes for provider agency station identifiers.
*/
private List<RccAcisStationType> __stationTypeList = new Vector();
    
/**
Constructor for web service.
*/
public RccAcisDataStore ( String name, String description, URI serviceRootURI )
throws URISyntaxException, IOException
{
    setName ( name );
    setDescription ( description );
    setServiceRootURI ( serviceRootURI );
    // Read the variable table.
    readVariableTable();
    // Initialize the station types - this may be available as a service at some point but for now inline
    __stationTypeList.add ( new RccAcisStationType(0,"ACIS","ACIS internal id"));
    __stationTypeList.add ( new RccAcisStationType(1,"WBAN","5-digit WBAN id"));
    __stationTypeList.add ( new RccAcisStationType(2,"COOP","6-digit COOP id"));
    __stationTypeList.add ( new RccAcisStationType(3,"FAA","3-character FAA id"));
    __stationTypeList.add ( new RccAcisStationType(4,"WMO","5-digit WMO id"));
    __stationTypeList.add ( new RccAcisStationType(5,"ICAO","4-character ICAO id"));
    __stationTypeList.add ( new RccAcisStationType(6,"GHCN","?-character GHCN id"));
    __stationTypeList.add ( new RccAcisStationType(7,"NWSLI","5-character NWSLI"));
    __stationTypeList.add ( new RccAcisStationType(9,"ThreadEx","6-character ThreadEx id"));
    __stationTypeList.add ( new RccAcisStationType(10,"CoCoRaHS","5+ character CoCoRaHS identifier"));
    __stationTypeList.add ( new RccAcisStationType(16,"AWDN","7-character HPRCC AWDN id"));
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
    String name = IOUtil.expandPropertyForEnvironment(props.getValue("Name"));
    String description = IOUtil.expandPropertyForEnvironment(props.getValue("Description"));
    String serviceRootURI = IOUtil.expandPropertyForEnvironment(props.getValue("ServiceRootURI"));
    
    // Get the properties and create an instance

    RccAcisDataStore ds = new RccAcisDataStore( name, description, new URI(serviceRootURI) );
    return ds;
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
{   List<String> typeList = new Vector();
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
        typeList.add( "" + rec.getMajor() + nameString + intervalString );
        recPrev = rec;
    }
    return typeList;
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
{
    for ( RccAcisStationType stationType: __stationTypeList ) {
        if ( stationType.getType().equalsIgnoreCase(type) ) {
            return stationType;
        }
    }
    return null;
}

/**
Look up the variable information given the data type, which is a string of the form
"N" or "N - Name" or "Name" where N is the "major".
@param dataType data type to match in variable data.
@return the variable that matches the data type.
*/
public RccAcisVariableTableRecord lookupVariable ( String dataType )
{   String majorString = null;
    String name = null;
    int pos = dataType.indexOf("-");
    if ( pos > 0) {
        // Assume that the major variable is specified and also the name
        majorString = dataType.substring(0,pos).trim();
        name = dataType.substring(pos + 1).trim();
    }
    else {
        // Only one piece of information specified.
        if ( StringUtil.isInteger(dataType) ) {
            majorString = dataType.trim();
        }
        else {
            name = dataType.trim();
        }
    }
    if ( majorString != null ) {
        // Use the major number to look up the variable
        int majorInt = -1;
        try {
            majorInt = Integer.parseInt(majorString);
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
    else if ( name != null ){
        // Use the variable name to look up the variable
        for ( RccAcisVariableTableRecord variable: __variableTableRecordList ) {
            if ( variable.getName().equalsIgnoreCase(name) ) {
                return variable;
            }
        }
    }
    return null;
}

/**
Read a list of MultiStn data records.
*/
public List<RccAcisStationTimeSeriesMetadata> readStationTimeSeriesMetadataList(
    String dataType, String timeStep, InputFilter_JPanel ifp )
throws IOException, MalformedURLException
{   String routine = getClass().getName() + ".readStationTimeSeriesMetadataList";
    // Look up the metadata for the data types
    RccAcisVariableTableRecord variable = lookupVariable ( dataType );
    // Form the URL - ask for as much metadata as possible
    StringBuffer urlString = new StringBuffer("" + getServiceRootURI() +
        "/MultiStnData?meta=sIds,uid,name,postal,county,ll,elev,valid_daterange" );
    // The following helps with performance
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
    // Climate division...
    List<String> clim_divList = ifp.getInput(null, "clim_div", true, null);
    if ( clim_divList.size() > 1 ) {
        throw new IOException ( "<= 1 climate division filters can be specified." );
    }
    else if ( clim_divList.size() == 1 ) {
        String clim_div = clim_divList.get(0).split("-")[0].trim();
        urlString.append("&clim_div="+URLEncoder.encode(clim_div,"UTF-8"));
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
    // Postal code...
    List<String> postalList = ifp.getInput(null, "postal", true, null);
    if ( postalList.size() > 1 ) {
        throw new IOException ( "<= 1 postal code filters can be specified." );
    }
    else if ( postalList.size() == 1 ) {
        String postal = postalList.get(0).split("-")[0].trim();
        urlString.append("&postal="+URLEncoder.encode(postal,"UTF-8"));
    }
    // Always want JSON results...
    urlString.append("&output=json");
    Message.printStatus(2, routine, "Performing the following request:  " + urlString );
    URL url = new URL ( urlString.toString() );
    // Open the input stream...
    HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
    InputStream in = null;
    if ( urlConnection.getResponseCode() >= 400 ) {
        in = urlConnection.getErrorStream();
    }
    else {
        in = urlConnection.getInputStream();
    }
    InputStreamReader inp = new InputStreamReader(in);
    BufferedReader reader = new BufferedReader(inp);
    char[] buffer = new char[8192];
    int len1 = 0;
    StringBuffer b = new StringBuffer();
    while ( (len1 = reader.read(buffer)) != -1 ) {
        b.append(buffer,0,len1);
    }
    in.close();
    urlConnection.disconnect();
    String resultString = b.toString();
    if ( Message.isDebugOn ) {
        Message.printDebug(1,routine,"Returned data="+resultString);
    }
    if ( b.indexOf("error") >= 0 ) {
        throw new IOException ( "Error retrieving data for URL \"" + urlString + "\":  " +
            resultString + " (" + b + ")." );
    }
    else {
        // Parse the JSON
        // First fix a known issue with the parsing by replacing offending empty valid date range with something
        // that GSON can handle
        // Extra bracket around date range...
        //resultString = resultString.replace("valid_daterange\":[[", "valid_daterange\":[");
        //resultString = resultString.replace("]],\"postal","],\"postal");
        //resultString = resultString.replace("valid_daterange\":[[]]", "valid_daterange\":[\"\",\"\"]");
        resultString = resultString.replace("[[", "[");
        resultString = resultString.replace("]]", "]");
        if ( Message.isDebugOn ) {
            Message.printDebug(1,routine,"Returned data after cleanup="+resultString);
        }
        Gson gson = new Gson();
        RccAcisStationTimeSeriesMetadataList metadataListObject =
            gson.fromJson(resultString, RccAcisStationTimeSeriesMetadataList.class);
        if ( metadataListObject != null ) {
            for ( RccAcisStationTimeSeriesMetadata metadata: metadataListObject.getData() ) {
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
            return metadataListObject.getData();
        }
        else {
            // Return an empty list
            List<RccAcisStationTimeSeriesMetadata> data = new Vector();
            return data;
        }
    }
}

/**
Read a time series.
@param tsidentString the time series identifier string as per TSTool conventions.  The location should be
Type:ID (e.g., COOP:1234).  The data type should be the variable name (might implement abbreviation later but
trying to stay away from opaque variable major).
@param readStart the starting date/time to read, or null to read all data.
@param readEnd the ending date/time to read, or null to read all data.
@param readData if true, read the data; if false, construct the time series and populate properties but do
not read the data
@return the time series read from the ACIS web services
*/
public TS readTimeSeries ( String tsidentString, DateTime readStart, DateTime readEnd, boolean readData )
throws MalformedURLException, Exception
{
    TS ts = null;
    String routine = getClass().getName() + ".readTimeSeries";
    TSIdent tsident = TSIdent.parseIdentifier(tsidentString);
    int intervalBase = tsident.getIntervalBase();
    // Look up the metadata for the data name
    RccAcisVariableTableRecord variable = lookupVariable ( tsident.getType() );
    // The station ID needs to specify the location type...
    String stationIDAndStationType = readTimeSeries_FormHttpRequestStationID ( tsident.getLocation() );
    // The start and end date are required.
    String readStartString = "por";
    String readEndString = "por";
    if ( !readData ) {
        // Specify a minimal period to try a query and make sure that the time series is defined.
        // If this period is not valid for the time series, a missing value will come back.
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
    String elems = "" + variable.getMajor();
    // Form the URL - no need to ask for metadata?
    // Always specify the station id type to avoid ambiguity
    StringBuffer urlString = new StringBuffer("" + getServiceRootURI() +
         "/StnData?meta=sIds,uid,name,postal,county,ll,elev&sId=" + URLEncoder.encode(stationIDAndStationType,"UTF-8") +
         "&elems=" + elems + "&sDate=" + readStartString + "&eDate=" + readEndString );
    // Always want JSON results...
    boolean requestJSON = false;
    if ( requestJSON ) {
        urlString.append("&output=json");
    }
    else {
        // Request CSV
        urlString.append("&output=csv");
    }
    String urlStringEncoded = urlString.toString(); //URLEncoder.encode(urlString.toString(),"UTF-8");
    Message.printStatus(2, routine, "Performing the following request:  " + urlStringEncoded );
    URL url = new URL ( urlStringEncoded );
    // Open the input stream...
    HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
    // TODO SAM 2011-06-21 Set the timeouts if a property in the datastore file.
    // This may be necessary if network performance
    // is very slow.  However, the default of 0 means no timeout... so why does this sometimes throw
    // a timeout exception?  Is there a timeout of a sub-process on the other end that gets enforced?
    //urlConnection.setConnectTimeout(5000);
    //urlConnection.setReadTimeout(5000);
    //Message.printStatus(2, routine, "Default connect timeout=" + urlConnection.getConnectTimeout() );
    //Message.printStatus(2, routine, "Default read timeout=" + urlConnection.getReadTimeout() );
    InputStream in = null;
    Message.printStatus(2, routine, "Response code=" + urlConnection.getResponseCode() +
        " Response message = \"" + urlConnection.getResponseMessage() + "\"" );
    if ( urlConnection.getResponseCode() >= 400 ) {
        in = urlConnection.getErrorStream();
    }
    else {
        in = urlConnection.getInputStream();
    }
    /*
    try {

    }
    catch ( Exception e ) {
        Message.printWarning(3,routine,"Error in RCC ACIS request (" + e + ")" );
        Message.printWarning(3,routine,e);
        // Rethrow...
        throw e;
    }
    */
    InputStreamReader inp = new InputStreamReader(in);
    BufferedReader reader = new BufferedReader(inp);
    char[] buffer = new char[8192];
    int len1 = 0;
    StringBuffer b = new StringBuffer();
    while ( (len1 = reader.read(buffer)) != -1 ) {
        b.append(buffer,0,len1);
    }
    in.close();
    urlConnection.disconnect();
    String resultString = b.toString();
    if ( Message.isDebugOn ) {
        Message.printDebug(1,routine,"Returned data="+resultString);
    }
    if ( b.indexOf("error") >= 0 ) {
        throw new IOException ( "Error retrieving data for URL \"" + urlStringEncoded +
            "\":  " + resultString + " (" + b + ")." );
    }
    else {
        if ( requestJSON ) {
            // Parse the JSON for time series attributes
            //Gson gson = new Gson();
            /*
            RccAcisStationTimeSeriesMetadataList metadataListObject =
                gson.fromJson(resultString, RccAcisStationTimeSeriesMetadataList.class);
            if ( metadataListObject != null ) {
                for ( RccAcisStationTimeSeriesMetadata metadata: metadataListObject.getData() ) {
                    //Message.printStatus(2,routine,metadata.getName());
                    metadata.setVariable(variable);
                    metadata.setDataStore ( this );
                    // TODO SAM 2011-01-07 Some metadata like HUC do not return so may need to set based
                    // on whether the information was entered in the filter
                }
            }
            return metadataListObject.getData();
            */
        }
        else {
            
        }
        // Create the time series.
        ts = TSUtil.newTimeSeries(tsidentString, true);
        ts.setIdentifier(tsidentString);
        ts.setMissing(Double.NaN);// Use this instead of legacy default -999
        // Parse the data into short strings
        String [] dataStringsArray = new String[0];
        DateTime dataStart = null;
        DateTime dataEnd = null;
        String stationName = "";
        int mCount = 0;
        int tCount = 0;
        int commaPos = 0; // Position of comma
        if ( requestJSON ) {
            // Process the JSON by brute force.  Data are after "data":[ and are a sequence of
            // ["yyyy-mm-dd","value"],... where value can be a number, "M" for missing, or "T" for trace.
            // Station name is "name":"xxxx"
            int pos = resultString.indexOf("name\":");
            if ( pos > 0 ) {
                // Find the closing quote on the name
                int pos2 = resultString.indexOf("\"",pos + 8);
                if ( pos2 >= 0 ) {
                    stationName = resultString.substring(pos+5,pos2);
                }
            }
            pos = resultString.indexOf("data\":[");
            if ( pos > 0 ) {
                // Found the data section
                dataStringsArray = resultString.substring(pos + 7).split(",");
                Message.printStatus(2, routine, "Have " + dataStringsArray.length + " data values." );
                if ( dataStringsArray.length > 0 ) {
                    // Each string should be ["YYYY-MM-DD","Val"]
                    dataStart = DateTime.parse(dataStringsArray[0].substring(2,12));
                    dataEnd = DateTime.parse(dataStringsArray[dataStringsArray.length - 1].substring(2,12));
                }
            }
        }
        else {
            // CSV, each newline delimited row has YYYY-MM-DD,valueFlag
            // (Flag character is optional) with the first line being the station name
            dataStringsArray = resultString.split("\n");
            Message.printStatus(2, routine, "Have " + dataStringsArray.length + " data records (first is station name)." );
            if ( dataStringsArray.length > 1 ) {
                stationName = dataStringsArray[0];
                commaPos = dataStringsArray[1].indexOf(",");
                dataStart = DateTime.parse(dataStringsArray[1].substring(0,commaPos));
                commaPos = dataStringsArray[dataStringsArray.length - 1].indexOf(",");
                dataEnd = DateTime.parse(dataStringsArray[dataStringsArray.length - 1].substring(0,commaPos));
            }
        }
        ts.setDataUnits(variable.getUnits());
        ts.setDataUnitsOriginal(variable.getUnits());
        ts.setDescription(stationName);
        ts.setDate1(dataStart);
        ts.setDate2(dataEnd);
        ts.setDate1Original(dataStart);
        ts.setDate2Original(dataEnd);
        DateTime date = null;
        String [] dataStringParts = null;
        String valueString; // string containing value and optionally flag part of data
        String flagString; // string containing flag part of data
        int valueStringLength;
        // Nolan Doesken and Bill Noon indicate that 0 is what people use for trace
        double traceValue = 0.0;
        double missing = ts.getMissing(); // Should be Double.NaN
        if ( readData ) {
            ts.allocateDataSpace();
            // Process each data string.  Trace values result in setting the data flag.
            if ( requestJSON ) {
                for ( int i = 0; i < dataStringsArray.length; i++ ) {
                    try {
                        dataStringParts = dataStringsArray[i].split(",");
                        dataStringParts[0] = dataStringParts[0].substring(2,14);
                        dataStringParts[1] = dataStringParts[1].substring(1,dataStringParts[1].length() - 2);
                        valueString = dataStringParts[1];
                        valueStringLength = valueString.length();
                        date = DateTime.parse(dataStringsArray[i]); // TODO SAM is this correct?
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
                            ts.setDataValue(date,Double.parseDouble(valueString));
                        }
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3,routine,"Error parsing data point \"" +
                            dataStringsArray[i] + "\" (" + e + ").");
                        continue;
                    }
                }
            }
            else {
                // CSV data - start in second row since first is the station name
                for ( int i = 1; i < dataStringsArray.length; i++ ) {
                    try {
                        dataStringParts = dataStringsArray[i].split(",");
                        date = DateTime.parse(dataStringParts[0]);
                        valueString = dataStringParts[1];
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
                        Message.printWarning(3,routine,"Error parsing data point \"" +
                            dataStringsArray[i] + "\" (" + e + ") - treating as flagged data.");
                        // TODO SAM 2011-04-04 Have seen data values like "S", "0.20A".  Should these be
                        // considered valid data points or treated as missing because they failed some test?
                        // Submitted an email request to the ACIS contact page to see if I can get an answer.
                        // For now, strip the characters off the end and treat as the flag and use the numerical
                        // part (if present) for the value.
                        int lastDigitPos = -1;
                        String dataStringPart = dataStringParts[1];
                        for ( int iChar = dataStringPart.length() - 1; iChar >= 0; iChar-- ) {
                            if ( Character.isDigit(dataStringPart.charAt(iChar)) ) {
                                lastDigitPos = iChar;
                                break;
                            }
                        }
                        if ( lastDigitPos >= 0 ) {
                            String number = dataStringPart.substring(0,lastDigitPos);
                            if ( StringUtil.isDouble(number) ) {
                                ts.setDataValue(date,Double.parseDouble(number),
                                    dataStringPart.substring(lastDigitPos + 1),0);
                            }
                            else {
                                // Set the entire string as the flag
                                ts.setDataValue(date,ts.getMissing(),dataStringPart,0);
                            }
                        }
                        else {
                            ts.setDataValue(date,ts.getMissing(),dataStringPart,0);
                        }
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3,routine,"Error parsing data point \"" +
                            dataStringsArray[i] + "\" (" + e + ").");
                    }
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
Form the station ID string part of the time series request, something like "ID code", where code is the station
ID code.  If an ACIS ID, no code is necessary.
@param tsidLocation the location part of a time series identifier.
 */
String readTimeSeries_FormHttpRequestStationID ( String tsidLocation )
{
    int colonPos = tsidLocation.indexOf(":");
    if ( colonPos <= 0 ) {
        throw new InvalidParameterException ( "Station location \"" + tsidLocation +
            "\" is invalid (should be Type:ID)" );
    }
    try {
        RccAcisStationType stationType = lookupStationTypeFromType(tsidLocation.substring(0,colonPos).trim());
        if ( stationType == null ) {
            throw new InvalidParameterException ( "Station code from \"" + tsidLocation +
                "\" cannot be determined." );
        }
        else if ( stationType.getCode() == 0 ) {
            // No station type code is expected since the ACIS type, just pass ACIS ID
            return tsidLocation.substring(colonPos + 1).trim();
        }
        else {
            // Station ID followed by the station type code
            return tsidLocation.substring(colonPos + 1).trim() + " " + stationType.getCode();
        }
    }
    catch ( NumberFormatException e ) {
        throw new InvalidParameterException ( "Station location \"" + tsidLocation +
        "\" is invalid (should be Type:ID)" );
    }
}

/**
Make a request to get the variable table, which is used to map ACIS data to time series available in TSTool.
The table comes back as HTML so need to use a DOM and extract cells.
TODO SAM 2011-01-08 Need Bill Noon to implement an API to get the variable table.
*/
private void readVariableTable ()
throws URISyntaxException, IOException
{   String routine = getClass().getName() + ".readVariableTable";
    // Get the file
    URL url = new URL ( "" + getServiceRootURI() + "/doc/VariableTable.html" );
    // Open the input stream...
    URLConnection urlConnection = url.openConnection();
    InputStream in = urlConnection.getInputStream();
    InputStreamReader inp = new InputStreamReader(in);
    BufferedReader reader = new BufferedReader(inp);
    char[] buffer = new char[8192];
    int len1 = 0;
    StringBuffer b = new StringBuffer();
    while ( (len1 = reader.read(buffer)) != -1 ) {
        b.append(buffer,0,len1);
    }
    in.close();
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
        String name = "", method = "", measureInterval = "", reportInterval = "", units = "", source = "";
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
                    __variableTableRecordList.add(new RccAcisVariableTableRecord(major, minor, name, method,
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