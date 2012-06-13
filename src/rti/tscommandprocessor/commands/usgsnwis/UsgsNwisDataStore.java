package rti.tscommandprocessor.commands.usgsnwis;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import riverside.datastore.AbstractWebServiceDataStore;

import RTi.TS.TS;
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
Data store for USGS NWSI web services.  This class maintains the web service information in a general way.
*/
public class UsgsNwisDataStore extends AbstractWebServiceDataStore
{
    
/**
WaterML name space.
*/
private static final String WATERML_1_0_NS = "http://www.cuahsi.org/waterML/1.0/";
    
/**
The records of valid parameters, listed here:  http://nwis.waterdata.usgs.gov/usa/nwis/pmcodes?radio_pm_search=param_group&pm_group=All+--+include+all+parameter+groups&pm_search=&casrn_search=&srsname_search=&format=html_table&show=parameter_group_nm&show=parameter_nm&show=casrn&show=srsname&show=parameter_units
Examples here:  http://waterservices.usgs.gov/rest/USGS-DV-Service.html
*/
private List<UsgsNwisParameterType> __parameterTypeList = new Vector();
    
/**
The records of valid statistics, listed here:  http://waterservices.usgs.gov/rest/USGS-DV-Service.html
*/
private List<UsgsNwisStatisticType> __statisticTypeList = new Vector();
   
/**
Constructor for web service.
*/
public UsgsNwisDataStore ( String name, String description, URI serviceRootURI )
throws URISyntaxException, IOException
{
    setName ( name );
    setDescription ( description );
    setServiceRootURI ( serviceRootURI );
    // Initialize the parameter types - this may be available as a service at some point but for now inline
    __parameterTypeList.add ( new UsgsNwisParameterType("00054","Physical","Reservoir storage, acre feet","","Reservoir storage","ac-ft"));
    __parameterTypeList.add ( new UsgsNwisParameterType("00060","Physical","Discharge, cubic feet per second","","Stream flow, mean. daily","cfs"));
    __parameterTypeList.add ( new UsgsNwisParameterType("00065","Physical","Gage height, feet","","Height, gage","ft"));
    __parameterTypeList.add ( new UsgsNwisParameterType("74207","Physical","Moisture content, soil, volumetric, percent of total volume","","Moisture content","%"));
    __parameterTypeList.add ( new UsgsNwisParameterType("63160","Physical","Stream water level elevation above NAVD 1988, in feet","","","ft"));
    __parameterTypeList.add ( new UsgsNwisParameterType("72020","Physical","Reservoir storage, total pool, percent of capacity","","","%"));
    __parameterTypeList.add ( new UsgsNwisParameterType("81026","Physical","Water content of snow, inches","","Water content of snow","in"));
    __parameterTypeList.add ( new UsgsNwisParameterType("82300","Physical","Snow depth, inches","","Depth, snow cover","in"));
    // Initialize the statistic types - this may be available as a service at some point but for now inline
    __statisticTypeList.add ( new UsgsNwisStatisticType("00001","Maximum","Maximum values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("00002","Minimum","Minimum values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("00003","Mean","Mean values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("00006","Sum","Sum of values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("00007","Mode","Modal values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("00008","Median","Median values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("00009","STD","Standard deviation values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("00010","Variance","Variance values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("00021","Tidal high","High high-tide values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("00022","Tidal high","High high-tide values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("00023","Tidal high","High high-tide values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("00024","Tidal high","High high-tide values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("01002","?","?"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("31200","Observation at 12:00",
         "Instantaneous observation at time hhmm where hhmm runs from 00001 to 2400"));
    //__statisticTypeList.add ( new UsgsNwisStatisticType("32359","Observation at 23:59",
    //    "Instantaneous observation at time hhmm where hhmm runs from 00001 to 2400"));
}

/**
Factory method to construct a data store connection from a properties file.
@param filename name of file containing property strings
*/
public static UsgsNwisDataStore createFromFile ( String filename )
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

    UsgsNwisDataStore ds = new UsgsNwisDataStore( name, description, new URI(serviceRootURI) );
    return ds;
}

/**
Determine the WaterML version.
@param dom DOM from WaterML
@throws IOException
*/
private WaterMLVersion determineWaterMLVersion(String resourcePath, Document dom)
throws IOException {
    String xmlns = dom.getDocumentElement().getAttribute("xmlns");
    WaterMLVersion version = WaterMLVersion.VANILLA_1_0; // default

    // @todo handle (possibly detect) other flavors
    if ( resourcePath.indexOf("NWISQuery") > 0 ) {
        // TODO SAM 2011-01-11 Need a cleaner way to determine USGS WaterML variant
        version = WaterMLVersion.USGS;
    }
    else if (!WATERML_1_0_NS.equals(xmlns)) {
        throw new IOException("Unable to process document based on xmlns " + xmlns);
    }
    Message.printStatus(2,"","WaterML version is " + version);
    return version;
}

/**
Return the list of agencies that are available.  Currently this returns an empty list.
@param includeName whether to include the name.
*/
public List<String> getAgencyStrings ( boolean includeName )
{   List<String> agencyList = new Vector();
    /*
    for ( UsgsNwisAgencyType agency: __agencyTypeList ) {
        if ( includeName ) {
            agencyList.add( "" + agency.getAbbreviation() + " - " + agency.getName() );
        }
        else {
            agencyList.add( "" + agency.getAbbreviation() );
        }
    }
    */
    return agencyList;
}

/**
Return the unique list of data interval strings available for a data type, returning values that
are consistent with TSTool ("Day", rather than "daily").
@param dataType data type string of form "N" or "N - name" or "name", where N is the major number.
*/
public List<String> getDataIntervalStringsForDataType ( String dataType )
{   List<String> dataIntervalStrings = new Vector();
    // For now a data type should only have one interval because of the uniqueness of the data type.
    //UsgsNwisParameterType param = lookupParameterFromDataType(dataType);
    // TODO SAM 2011-01-10 For now always return daily but need to evaluate
    dataIntervalStrings.add("Day");
    return dataIntervalStrings;
}

/**
Return the list of data types that are available.  Currently this returns the parameter code and optionally
the name.  Duplicates in the table are ignored.
TODO SAM 2011-01-07 It would be good to have the option of using data type abbreviations, but this may
not be an option.
@param includeName whether to include the name.
*/
public List<String> getDataTypeStrings ( boolean includeName )
{   List<String> dataTypeList = new Vector();
    for ( UsgsNwisParameterType param: __parameterTypeList ) {
        if ( includeName ) {
            dataTypeList.add( "" + param.getCode() + " - " + param.getName() );
        }
        else {
            dataTypeList.add( "" + param.getCode() );
        }
    }
    return dataTypeList;
}

/**
Return the list of statistics that are available.  Currently this returns the statistic code and optionally
the name.
@param includeName whether to include the name.
*/
public List<String> getStatisticStrings ( boolean includeName )
{   List<String> statisticList = new Vector();
    for ( UsgsNwisStatisticType statistic: __statisticTypeList ) {
        if ( includeName ) {
            statisticList.add( "" + statistic.getCode() + " - " + statistic.getName() );
        }
        else {
            statisticList.add( "" + statistic.getCode() );
        }
    }
    return statisticList;
}

/**
Look up the parameter type given the parameter string "Code" or "Code - name".
@return the parameter or null if not found.
*/
public UsgsNwisParameterType lookupParameterType ( String parameter )
{   int pos = parameter.indexOf("-");
    String parameterCode = null;
    if ( pos > 0 ) {
        parameterCode = parameter.substring(0,pos).trim();
    }
    else {
        parameterCode = parameter.trim();
    }
    for ( UsgsNwisParameterType parameterType: __parameterTypeList ) {
        if ( parameterType.getCode().equalsIgnoreCase(parameterCode) ) {
            return parameterType;
        }
    }
    return null;
}

/**
Look up the statistic type given the statistic string "Code" or "Code - name".
@return the statistic or null if not found.
*/
public UsgsNwisStatisticType lookupStatisticType ( String statistic )
{   int pos = statistic.indexOf("-");
    String statisticCode = null;
    if ( pos > 0 ) {
        statisticCode = statistic.substring(0,pos).trim();
    }
    else {
        statisticCode = statistic.trim();
    }
    for ( UsgsNwisStatisticType statisticType: __statisticTypeList ) {
        if ( statisticType.getCode().equalsIgnoreCase(statisticCode) ) {
            return statisticType;
        }
    }
    return null;
}

/**
Look up the statistic type given the statistic string name.
@return the statistic or null if not found.
*/
public UsgsNwisStatisticType lookupStatisticTypeFromName ( String statisticName )
{   for ( UsgsNwisStatisticType statisticType: __statisticTypeList ) {
        if ( statisticType.getName().equalsIgnoreCase(statisticName) ) {
            return statisticType;
        }
        else if ( StringUtil.isInteger(statisticName) &&
            statisticType.getCode().equalsIgnoreCase(statisticName)) {
            // Assume that the statistic code was provided
            return statisticType;
        }
    }
    return null;
}

/**
Read a list of site/time series data records.  Currently the CUAHSI cataloging service is not enabled and
the USGS service does not seem to provide a catalog list either so just construct a single metadata instance
from the query parameters and return.
*/
public List<UsgsNwisSiteTimeSeriesMetadata> readSiteTimeSeriesMetadataList(
    String dataType, String timeStep, InputFilter_JPanel ifp )
throws IOException, MalformedURLException
{   //String routine = getClass().getName() + ".readSiteTimeSeriesMetadataList";
    List<UsgsNwisSiteTimeSeriesMetadata> metadataList = new Vector();
    UsgsNwisSiteTimeSeriesMetadata metadata = new UsgsNwisSiteTimeSeriesMetadata();
    metadata.setDataStore(this);
    metadata.setInterval("1Day");
    // Parameter is from the data type
    metadata.setParameter(lookupParameterType(dataType));
    // Get the information from the input filter
    // Site number...
    List<String> siteNumber = ifp.getInput(null, "SiteNum", true, null);
    if ( siteNumber.size() > 1 ) {
        throw new IOException ( "<= 1 site number can be specified." );
    }
    else if ( siteNumber.size() == 1 ) {
        metadata.setSiteNum ( siteNumber.get(0).trim() );
    }
    // Agency...
    List<String> agency = ifp.getInput(null, "AgencyCode", true, null);
    if ( agency.size() > 1 ) {
        throw new IOException ( "<= 1 agency can be specified." );
    }
    else if ( agency.size() == 1 ) {
        metadata.setAgencyCode( agency.get(0).trim() );
    }
    // Statistic...
    List<String> statistic = ifp.getInput(null, "StatisticCode", true, null);
    if ( statistic.size() > 1 ) {
        throw new IOException ( "<= 1 statistic can be specified." );
    }
    else if ( statistic.size() == 1 ) {
        String statisticCode = statistic.get(0).trim();
        metadata.setStatistic(lookupStatisticType(statisticCode));
    }
    metadataList.add(metadata);
    return metadataList;
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
throws MalformedURLException, IOException, Exception
{
    String routine = getClass().getName() + ".readTimeSeries";
    TS ts = null;
    TSIdent tsident = TSIdent.parseIdentifier(tsidentString);
    // The data type part of the identifier has the parameter code and the statistic
    String dataType = tsident.getType();
    String parameterCode = null;
    String statisticName = null;
    UsgsNwisStatisticType statistic = null;
    int pos = dataType.indexOf("-");
    if ( pos > 0 ) {
        parameterCode = dataType.substring(0,pos).trim();
        statisticName = dataType.substring(pos+1).trim();
        statistic = lookupStatisticTypeFromName ( statisticName );
    }
    else {
        parameterCode = dataType.trim();
    }
    // The start and end date are required.  If not specified, request ten years...
    String readStartString = null;
    String readEndString = null;
    if ( !readData ) {
        // Specify a minimal period to try a query and make sure that the time series is defined.
        readStart = DateTime.parse("2011-01-01");
        readEnd = DateTime.parse("2011-01-01");
    }
    else {
        if ( readEnd == null ) {
            readEnd = new DateTime(DateTime.DATE_CURRENT|DateTime.PRECISION_DAY);
        }
        if ( readStart == null ) {
            // Default to ten years before start
            readStart = new DateTime(readEnd);
            readStart.addYear(-10);
        }
    }
    readStartString = readStart.toString(DateTime.FORMAT_YYYY_MM_DD);
    readEndString = readEnd.toString(DateTime.FORMAT_YYYY_MM_DD);
    // Form the URL
    StringBuffer urlString = new StringBuffer("" + getServiceRootURI() +
         "/GetDV1?SiteNum=" + tsident.getLocation() +
         "&ParameterCode=" + parameterCode );
    if ( statistic == null ) {
        if ( (statisticName != null) && StringUtil.isInteger(statisticName) ) {
            urlString.append("&StatisticCode=" + statisticName );
        }
    }
    else {
        if ( statistic.getCode() != null ) {
            urlString.append("&StatisticCode=" + statistic.getCode() );
        }
    }
    String agencyCode = tsident.getSource();
    if ( (agencyCode != null) && !agencyCode.equals("") ) {
        urlString.append("&AgencyCode=" + agencyCode );
    }
    urlString.append("&StartDate=" + readStartString + "&EndDate=" + readEndString );
    Message.printStatus(2, routine, "Performing the following request:  " + urlString.toString() );
    URL url = new URL ( urlString.toString() );
    // Open the input stream...
    HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
    InputStream in = null;
    Message.printStatus(2, routine, "Response code=" + urlConnection.getResponseCode() +
        " Response message = \"" + urlConnection.getResponseMessage() + "\"" );
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
    //if ( Message.isDebugOn ) {
        Message.printStatus(1,routine,"Returned data="+resultString);
    //}
    if ( b.indexOf("error") >= 0 ) {
        throw new IOException ( "Error retrieving data:  " + resultString + " (" + b + ")." );
    }
    else {
        // Create the time series from the WaterML...
        Document dom = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            new ReaderInputStream(new StringReader(resultString)));
        List<TS> tsList = readTimeSeriesList(urlString.toString(), dom, readData );
        // Expect one and only one time series based on how the request was originally made
        if ( tsList.size() == 0 ) {
            throw new IOException ( "Read 0 time series matching TSID \"" + tsidentString + ".\"" );
        }
        else if ( tsList.size() > 1 ) {
            throw new IOException ( "Read " + tsList.size() + " time series matching TSID \"" +
                tsidentString + " - expecting exactly 1 time series.\"" );
        }
        else {
            ts = tsList.get(0); 
        }
    }
    return ts;
}

/**
Read a single time series from the DOM given an element for the "timeseries" tag.
@param readData whether to read data values (if false initialize the period but do not allocate
memory or process the data values)
*/
private TS readTimeSeries( WaterMLVersion watermlVersion, Element element, boolean readData)
throws IOException
{

    Element sourceInfo = getSingleElement(element, "sourceInfo");
    Element variable = getSingleElement(element, "variable");
    Element values = getSingleElement(element, "values");

    TSIdent ident = readTimeSeries_ParseIdent(watermlVersion, sourceInfo, variable, values);

    TS ts;
    try {
        ts = TSUtil.newTimeSeries(ident.toString(), true);
        ts.setIdentifier(ident);
        ts.setDataUnits(findSingleElementValue(variable, "units"));
    }
    catch (Exception ex) {
        throw new IOException("Error setting identifier ", ex);
    }
    
    // Get the main time series properties...
    Element siteName = getSingleElement(element, "siteName");
    if ( siteName != null ) {
        ts.setDescription(siteName.getTextContent());
    }

    readTimeSeries_ParseValues(ts, values, readData);

    return ts;
}

private TSIdent readTimeSeries_ParseIdent(WaterMLVersion watermlVersion,
    Element sourceInfo, Element variable, Element values)
throws IOException
{
    TSIdent ident = new TSIdent();

    ident.setLocation(getSingleElementValue(sourceInfo, "siteCode"));

    String type = getSingleElementValue(variable, "variableName");
    String subtype = getSingleElementValue(variable, "dataType");
    if (subtype != null) {
        type = type + "-" + subtype;
    }
    ident.setType(type);

    try {
        ident.setInterval(readTimeSeries_ParseInterval(variable));
    }
    catch (Exception ex) {
        throw new IOException("Error parsing interval", ex);
    }

    if (watermlVersion == WaterMLVersion.USGS) {
        // Get the data source from a note...
        String src = grepValue(getElementValues(sourceInfo, "note"), "Agency:([^)])");
        ident.setSource(src == null ? "USGS" : src); // it's probably USGS anyway
    }
    else if (watermlVersion == WaterMLVersion.VANILLA_1_0) {
        try {
            Element source = getSingleElement(values, "source");
            ident.setSource(getSingleElementValue(source, "Organization"));
        }
        catch ( Exception e ) {
            // No data source
            Message.printWarning ( 3, "", "Unable to determine data source - not setting (" + e + ").");
        }
    }
    else {
        throw new RuntimeException("WaterML version " + watermlVersion + " is not implemented");
    }
    return ident;
}

private String readTimeSeries_ParseInterval(Element variable) throws IOException {
    Element timeSupport = getSingleElement(variable, "timeSupport");
    boolean regular = timeSupport.getAttribute("isRegular").equalsIgnoreCase("true");
    String interval = "IRREGULAR";
    if (regular) {
        Element unit = getSingleElement(timeSupport, "unit");
        String type = findSingleElementValue(unit, "UnitName");
        if (type == null) {
            type = findSingleElementValue(unit, "UnitDescription");
        }
        String amt = getSingleElementValue(timeSupport, "timeInterval");
        interval = amt + type;
    }
    return interval;
}

/**
Parse time series values from the DOM, and also set the period.
@param ts the time series that has been previously created and initialized
@param values
@param readData whether to read data values (if false initialize the period but do not allocate
memory or process the data values)
*/
private void readTimeSeries_ParseValues(TS ts, Element values, boolean readData )
throws IOException
{
    DateTime start = null;
    // TODO SAM 2011-01-11 Why not just used beginDateTime and endDateTime for the period?
    NodeList valuelist = values.getElementsByTagName("value");
    if ( (valuelist == null) || (valuelist.getLength() == 0) ) {
        // No data to process.  This may occur, for example, if a date range is requested that has no data
        return;
    }
    String dateTimeString = "";
    try {
        dateTimeString = ((Element)valuelist.item(0)).getAttribute("dateTime").replace("T", " ");
        start = DateTime.parse( dateTimeString, DateTime.FORMAT_YYYY_MM_DD_HH_mm_SS);
    }
    catch (IOException ex) {
        throw ex;
    }
    catch (Exception ex) {
        throw new IOException("Error parsing start date/time \"" + dateTimeString + "\"", ex);
    }
    // DO NOT use count data in the WaterML to determine the end date.  Apparently missing values in the original
    // data result in no XML record.  Therefore it is necessary to parse all DateTime strings from the data.
    DateTime end = null;
    dateTimeString = "";
    try {
        dateTimeString = ((Element)valuelist.item(valuelist.getLength() - 1)).getAttribute("dateTime").replace("T", " ");
        end = DateTime.parse(dateTimeString,DateTime.FORMAT_YYYY_MM_DD_HH_mm_SS);
    }
    catch (IOException ex) {
        throw ex;
    }
    catch (Exception ex) {
        throw new IOException("Error parsing date/time \"" + dateTimeString + "\"", ex);
    }
    ts.setDate1(start);
    ts.setDate1Original(start);
    ts.setDate2(end);
    ts.setDate2Original(end);
    String dataFlag;
    DateTime dateTime;
    if ( readData ) {
        ts.allocateDataSpace();
        for (int i = 0; i < valuelist.getLength(); i++) {
            // Must parse the dates because missing results in gaps
            try {
                Element el = (Element) valuelist.item(i);
                dateTimeString = el.getAttribute("dateTime").replace("T", " ");
                dataFlag = el.getAttribute("qualifiers");
                dateTime = DateTime.parse(dateTimeString);
                if ( (dataFlag != null) && !dataFlag.equals("") ) {
                    ts.setDataValue(dateTime, Double.parseDouble(el.getTextContent()), dataFlag, 0);
                }
                else {
                    ts.setDataValue(dateTime, Double.parseDouble(el.getTextContent()));
                }
            }
            catch ( Exception e ) {
                // Bad record.
                Message.printWarning(3,"","Bad data record (" + e + ") - skipping..." );
            }
        }
    }
}

/**
Read a time series list from the DOM.
@param resourcePath the URI as a string to the time series, used to check the WaterML version
@param readData whether to read data values (if false initialize the period but do not allocate
memory or process the data values)
*/
private List<TS> readTimeSeriesList ( String resourcePath, Document dom, boolean readData )
throws IOException
{
    WaterMLVersion watermlVersion = determineWaterMLVersion(resourcePath, dom);
   
    List<TS> tsList = new ArrayList<TS>();
    NodeList timeSeries = dom.getElementsByTagName("timeSeries");
    for (int i = 0; i < timeSeries.getLength(); i++) {
        tsList.add(readTimeSeries(watermlVersion, (Element)timeSeries.item(i), readData ));
    }
    return tsList;
}

// The following methods might bet better in a DOMUtil class or similar.  They are called by the ReadTimeSeries
// method that processes DOM elements
// TODO SAM 2011-01-11 Clean up this code, javadoc, etc.

private List<String> getElementValues(Element parent, String name) throws IOException {
    NodeList nodes = parent.getElementsByTagName(name);
    ArrayList<String> vals = new ArrayList<String>(nodes.getLength());
    for (int i = 0; i < nodes.getLength(); i++) {
        vals.add(((Element) nodes.item(i)).getTextContent().trim());
    }
    return vals;
}

private Element findSingleElement(Element parent, String name) throws IOException {
    NodeList nodes = parent.getElementsByTagName(name);
    return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
}

private String findSingleElementValue(Element parent, String name) throws IOException {
    Element el = findSingleElement(parent, name);
    return el == null ? null : el.getTextContent().trim();
}

private String getSingleElementValue(Element parent, String name)
throws IOException {
    return getSingleElement(parent, name).getTextContent().trim();
}

private Element getSingleElement(Element parent, String name) throws IOException {
    NodeList nodes = parent.getElementsByTagName(name);
    if (nodes.getLength() != 1) {
        throw new IOException("Expected to find child \"" + name + "\" in \"" + parent.getTagName() + "\"");
    }
    return (Element) nodes.item(0);
}

static String grepValue(List<String> items, String regex) {
    Pattern p = Pattern.compile(regex);
    String match = null;
    for (String s : items) {
        Matcher m = p.matcher(s);
        if (m.find()) {
            match = m.group(m.groupCount());
            break;
        }
    }
    return match;
}

}