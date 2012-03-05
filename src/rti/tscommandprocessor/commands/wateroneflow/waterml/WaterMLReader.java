package rti.tscommandprocessor.commands.wateroneflow.waterml;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import rti.tscommandprocessor.commands.wateroneflow.waterml.WaterMLVersion;

import RTi.TS.TS;
import RTi.TS.TSDataFlagMetadata;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.Util.IO.ReaderInputStream;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
Class to read a WaterML file and return time series.
*/
public class WaterMLReader
{
    
/**
TODO SAM 2012-03-05 Evaluate whether to support WaterML 1.0
WaterML 1.0 name space.
*/
//private static final String WATERML_1_0_NS = "http://www.cuahsi.org/waterML/1.0/";

/**
WaterML 1.1 name space.
*/
private static final String WATERML_1_1_NS = "http://www.cuahsi.org/waterML/1.1/";
    
/**
WaterML content as string.
*/
private String __waterMLString = "";

/**
URL used to read the WaterML.
*/
private String __url = "";

/**
File path from which WaterML is being read.
*/
private File __file = null;
    
/**
Constructor.
@param waterMLString the string from which to parse the time series (may be result of in-memory
web service call, or text read from WaterML file)
@param url the full URL query used to make the query (specify as null or empty if read from a file)
*/
public WaterMLReader ( String waterMLString, String url, File file )
{
    __waterMLString = waterMLString;
    __url = url;
    __file = file;
    
}

/**
Determine the WaterML version.
@param dom DOM from WaterML
@throws IOException
*/
private WaterMLVersion determineWaterMLVersion(Document dom)
throws IOException
{   String routine = getClass().getName() + "determineWaterMLVersion";
    String xmlns = dom.getDocumentElement().getAttribute("xmlns");
    Message.printStatus ( 2, routine, "DOM xmlns=\"" + xmlns + "\"" );
    String xmlns_ns1 = dom.getDocumentElement().getAttribute("xmlns:ns1");
    Message.printStatus ( 2, routine, "DOM xmlns:ns1=\"" + xmlns_ns1 + "\"" );
    WaterMLVersion version = null;

    /* TODO SAM 2012-03-05 Evaluate whether old version should be supported
    if ( WATERML_1_0_NS.equals(xmlns) || WATERML_1_0_NS.equals(xmlns_ns1) ) {
        // TODO SAM 2012-03-02 Not even sure if version 1.0 should be supported
        version = WaterMLVersion.STANDARD_1_0;
    }
    else*/
    if ( WATERML_1_1_NS.equals(xmlns) || WATERML_1_1_NS.equals(xmlns_ns1) ) {
        version = WaterMLVersion.STANDARD_1_1;
    }
    else {
        throw new IOException("Unable to determine WaterML version based on xmlns \"" + xmlns +
            "\" WaterML version not supported.");
    }
    Message.printStatus(2,"", "WaterML version is " + version);
    return version;
}

//The following methods might bet better in a DOMUtil class or similar.  They are called by the ReadTimeSeries
//method that processes DOM elements
//TODO SAM 2011-01-11 Clean up this code, javadoc, etc.

/**
Get a list of element text values.
Return the list of element text values.
@param parentElement parent element
@param name name of element to match
*/
private List<String> getElementValues(Element parentElement, String name) throws IOException {
    NodeList nodes = parentElement.getElementsByTagNameNS("*",name);
    ArrayList<String> vals = new ArrayList<String>(nodes.getLength());
    for (int i = 0; i < nodes.getLength(); i++) {
        vals.add(((Element) nodes.item(i)).getTextContent().trim());
    }
    return vals;
}

/**
Find an element (given a parent element) that matches the given element name.
@param parentElement parent element to process
@param elementName the element name to match
@param attributeName the attribute name to match (if null, don't try to match the attribute name)
@return the first matched element, or null if none are matched
@throws IOException
*/
private Element findSingleElement(Element parentElement, String elementName ) throws IOException {
    return findSingleElement ( parentElement, elementName, null );
}

/**
Find an element (given a parent element) that matches the given element name.
@param parentElement parent element to process
@param elementName the element name to match
@return the first matched element, or null if none are matched
@throws IOException
*/
private Element findSingleElement(Element parentElement, String elementName,
    String attributeName ) throws IOException {
    NodeList nodes = parentElement.getElementsByTagNameNS("*",elementName);
    if ( nodes.getLength() == 0 ) {
        return null;
    }
    else {
        if ( (attributeName != null) && !attributeName.equals("") ) {
            // Want to search to see if the node has a matching attribute name
            for ( int i = 0; i < nodes.getLength(); i++ ) {
                Node node = nodes.item(i);
                NamedNodeMap nodeMap = node.getAttributes();
                if ( nodeMap.getNamedItem(attributeName) != null ) {
                    // Found the node of interest
                    return (Element)node;
                }
            }
            // No node had the requested attribute
            return null;
        }
        else {
            return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
        }
    }
}

/**
Find an element (given a parent element) that matches the given element name and return its string content.
@param parentElement parent element to process
@param elementName the element name to match
@return the first string text of the first matched element, or null if none are matched
@throws IOException
*/
private String findSingleElementValue(Element parentElement, String elementName) throws IOException {
    Element el = findSingleElement(parentElement, elementName);
    return el == null ? null : el.getTextContent().trim();
}

private String getSingleElementValue(Element parent, String name)
throws IOException {
    return getSingleElement(parent, name).getTextContent().trim();
}

/**
Get the single element matching the given name from a parent element
@param parentElement parent element to process
@param elementName the element name to match
@return the element matching elementName, or null if not matched
@throws IOException
*/
private Element getSingleElement(Element parentElement, String name) throws IOException {
    NodeList nodes = parentElement.getElementsByTagNameNS("*",name);
    if (nodes.getLength() != 1) {
        throw new IOException("Expected to find child \"" + name + "\" in \"" + parentElement.getTagName() + "\"");
    }
    return (Element) nodes.item(0);
}

private String grepValue(List<String> items, String regex) {
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

/**
Determine whether the WaterML is for USGS NWIS, in which case some additional information is present
beyond the WaterML namespace.
*/
private boolean isUsgsNwis ( Document dom, WaterMLVersion watermlVersion )
{
    // Further check to see if USGS version, something like:
    // <ns2:queryURL>http://waterservices.usgs.gov/nwis/dv</ns2:queryURL>
    if ( watermlVersion == WaterMLVersion.STANDARD_1_1) {
        NodeList nodes = dom.getDocumentElement().getElementsByTagNameNS("*","queryURL");
        Node node;
        for ( int i = 0; i < nodes.getLength(); i++ ) {
            node = nodes.item(i);
            if ( node.getTextContent().toUpperCase().indexOf("USGS") >= 0 ) {
                return true;
            }
        }
    }
    return false;
}

/**
Read a single time series from the DOM given an element for the "timeseries" tag.
@param watermlVersion WaterML version being read (element names are different)
@param domElement the top level DOM element
@param timeSeriesElement the "timeSeries" element that is being processed, containing one time series
@param interval indicates the interval for data in the file, needed as a hint because there is nothing
in the file that indicates that data are daily values, etc.
@param url the original URL used to read the WaterML (can be null or "" if read from a file), used to
populate the history comments in the time series
@param file the original File corresponding to the WaterML file (can be null or "" if read from a file), used to
populate the history comments in the time series
@param readData whether to read data values (if false initialize the period but do not allocate
memory or process the data values)
*/
private TS readTimeSeries( WaterMLVersion watermlVersion, Element domElement, Element timeSeriesElement,
    TimeInterval interval, String url, File file, boolean readData)
throws IOException
{
    String sourceInfoTag = null;
    String variableTag = null;
    String valuesTag = null;
    if ( watermlVersion == WaterMLVersion.STANDARD_1_1 ) {
        sourceInfoTag = "sourceInfo"; // Information about ?
        variableTag = "variable"; // Information about data type
        valuesTag = "values"; // Data values and information about flags
    }
    Element sourceInfoElement = getSingleElement(timeSeriesElement, sourceInfoTag);
    Element variableElement = getSingleElement(timeSeriesElement, variableTag);
    Element valuesElement = getSingleElement(timeSeriesElement, valuesTag);

    // Get the time series identifier for the time series in order to initialize the time series
    TSIdent ident = readTimeSeries_ParseIdent(watermlVersion, interval,
        sourceInfoElement, variableElement, valuesElement);

    TS ts;
    try {
        // Create the time series and set basic time series properties
        ts = TSUtil.newTimeSeries(ident.toString(), true);
        ts.setIdentifier(ident);
    }
    catch (Exception ex) {
        throw new IOException("Error setting time series identifier ", ex);
    }
    
    // Set the main time series properties
    
    try {
        // Missing data value
        ts.setMissing ( Double.NaN );
        // Data units
        ts.setDataUnits(readTimeSeries_ParseUnits(watermlVersion,variableElement));
        // Description - set to site name
        Element siteName = getSingleElement(timeSeriesElement, "siteName");
        if ( siteName != null ) {
            ts.setDescription(siteName.getTextContent());
        }
        // History
        if ( (url != null) && !url.equals("") ) {
            // Set creation information from URL
            ts.addToGenesis("Create time series from WaterML queried with URL:  " + url );
        }
        else {
            if ( file != null ) {
                // Set creation information from file
                ts.addToGenesis("Create time series from contents of file:  " + file.getAbsolutePath() );
            }
            // Also extract creation information from the WaterML (probably a file).
            ts.addToGenesis("Query information extracted from WaterML (as XML elements):  " );
            Element queryInfoElement = getSingleElement(domElement, "queryInfo");
            if ( queryInfoElement != null ) {
                // Just pass through the information
                ts.addToGenesis ( queryInfoElement.toString() );
            }
        }
        // TODO SAM 2012-03-05 Need to enable more sourceInfo as time series properties, perhaps
        // finish when the interactive browsing is in place and it is more obvious how to handle
        // all the properties.
        
        // Qualifiers on flags, as per:
        // <ns1:qualifier qualifierID="0" ns1:network="NWIS" ns1:vocabulary="uv_rmk_cd">
        //     <ns1:qualifierCode>P</ns1:qualifierCode>
        //     <ns1:qualifierDescription>Provisional data subject to revision.</ns1:qualifierDescription>
        // </ns1:qualifier>
        NodeList nodes = valuesElement.getElementsByTagNameNS("*","qualifier");
        for ( int i = 0; i < nodes.getLength(); i++ ) {
            Element qualifierElement = (Element)nodes.item(i);
            String qualifierCode = getSingleElementValue(qualifierElement,"qualifierCode");
            String qualifierDescription = getSingleElementValue(qualifierElement,"qualifierDescription");
            if ( (qualifierCode != null) && !qualifierCode.equals("") ) {
                ts.addDataFlagMetadata(new TSDataFlagMetadata(qualifierCode,qualifierDescription));
            }
        }
    }
    catch (Exception ex) {
        throw new IOException("Error setting time series properties ", ex);
    }
    
    // Set the time series period and optionally read the data

    String noDataValue = getSingleElementValue(variableElement, "noDataValue" );
    readTimeSeries_ParseValues ( watermlVersion, ts, valuesElement, noDataValue, readData );

    return ts;
}

/**
Parse the WaterML information necessary to create a unique TSIdent, which will be used to create
a time series.
@param watermlVersion the WaterML version being used
@param interval indicates the interval for data in the file, needed as a hint because there is nothing
in the file that indicates that data are daily values, etc.
@param sourceInfoElement
@param variableElement
@param valuesElement
@return
@throws IOException
*/
private TSIdent readTimeSeries_ParseIdent(WaterMLVersion watermlVersion, TimeInterval interval,
    Element sourceInfoElement, Element variableElement, Element valuesElement )
throws IOException
{   String routine = getClass().getName() + ".readTimeSeries_ParseIdent";
    TSIdent ident = new TSIdent();
    String siteCodeTag = null;
    String variableCodeTag = null;
    if ( watermlVersion == WaterMLVersion.STANDARD_1_1 ) {
        siteCodeTag = "siteCode";
        variableCodeTag = "variableCode";
    }
    ident.setLocation(getSingleElementValue(sourceInfoElement, siteCodeTag));

    boolean useDataTypePrefix = false; // TODO SAM 2012-03-04 evaluate whether need this for clarity
    String dataType = getSingleElementValue(variableElement, variableCodeTag);
    if ( useDataTypePrefix ) {
        dataType = "VariableCode:" + dataType;
    }
    String subtype = null;
    if ( watermlVersion == WaterMLVersion.STANDARD_1_1 ) {
        // For WaterML 1.1 the subtype is the statistic code, which is taken from the following:
        // <ns1:options>
        //     <ns1:option name="Statistic" optionCode="00006">Sum</ns1:option>
        //</ns1:options>
        // To be bulletproof and avoid periods that would cause a problem in the TSID, use the code
        Element optionsElement = findSingleElement(variableElement,"options");
        if ( optionsElement != null ) {
            NodeList optionElements = optionsElement.getElementsByTagNameNS("*", "option");
            if ( optionElements != null ) {
                for ( int i = 0; i < optionElements.getLength(); i++ ) {
                    Element optionElement = (Element)optionElements.item(i);
                    subtype = optionElement.getAttribute("optionCode"); // DO NOT use getAttributeNS("*"
                    if ( (subtype != null) && useDataTypePrefix ) {
                        subtype = "StatisticCode:" + subtype;
                    }
                }
            }
            else {
                Message.printStatus(2, routine,
                    "option element is null for options element - cannot determine statistic for data subtype" );
            }
        }
        else {
            Message.printStatus(2, routine,
                "options element is null for variable element - cannot determine statistic for data subtype" );
        }
    }
    //else {
        // USGS 1.0?
        //subtype = getSingleElementValue(variable, "dataType");
    //}
    if (subtype != null) {
        dataType = dataType + "-" + subtype;
    }
    ident.setType(dataType);

    try {
        // TODO SAM 2012-03-05 This seemed to work with WatermML 1.0 but not 1.1...
        if ( interval != null ) {
            ident.setInterval("" + interval);
        }
        else {
            if ( watermlVersion == WaterMLVersion.STANDARD_1_0 ) {
                ident.setInterval(readTimeSeries_ParseInterval(variableElement));
            }
        }
    }
    catch (Exception ex) {
        throw new IOException("Error parsing interval (" + ex + ").", ex);
    }

    /* TODO SAM 2012-03-01 Figure out if this code still needs to be be handled
    if (watermlVersion == WaterMLVersion.USGS) {
        // Get the data source from a note...
        String src = grepValue(getElementValues(sourceInfo, "note"), "Agency:([^)])");
        ident.setSource(src == null ? "USGS" : src); // it's probably USGS anyway
    }
    else 
        */
    if (watermlVersion == WaterMLVersion.STANDARD_1_1) {
        try {
            Element siteCodeElement = getSingleElement(sourceInfoElement, "siteCode");
            if ( siteCodeElement != null ) {
                String agencyCode = siteCodeElement.getAttribute("agencyCode");
                if ( agencyCode != null ) {
                    Message.printStatus(2,routine,"agencyCode=\"" + agencyCode + "\"" );
                    ident.setSource(agencyCode);
                }
                else {
                    Message.printStatus(2, routine, "agencyCode attribute for siteCode element is null - cannot determine agency" );
                }
            }
            else {
                Message.printStatus(2, routine, "siteCode element is null for siteInfo element - cannot determine agency" );
            }
        }
        catch ( Exception e ) {
            // No data source
            Message.printWarning ( 3, "", "Unable to determine data source - not setting (" + e + ").");
        }
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
Parse data units from the DOM.
@param variableElement the variable element that contains unit information
@return the units as a string
*/
private String readTimeSeries_ParseUnits ( WaterMLVersion watermlVersion, Element variableElement )
throws IOException
{
    String unitsTag = null;
    if ( watermlVersion == WaterMLVersion.STANDARD_1_1 ) {
        unitsTag = "unitCode";
    }
    String units = findSingleElementValue(variableElement, unitsTag);
    if ( units == null ) {
        units = "";
    }
    return units;
}

/**
Parse time series values from the DOM, and also set the period.
@param ts the time series that has been previously created and initialized
@param valuesElement element containing a list of data values elements
@param noDataValue text that indicates no data value
@param readData whether to read data values (if false initialize the period but do not allocate
memory or process the data values)
*/
private void readTimeSeries_ParseValues(WaterMLVersion watermlVersion, TS ts, Element valuesElement,
    String noDataValue, boolean readData )
throws IOException
{
    DateTime start = null;
    // TODO SAM 2011-01-11 Why not just used beginDateTime and endDateTime for the period?
    NodeList valuelist = valuesElement.getElementsByTagNameNS("*","value");
    if ( (valuelist == null) || (valuelist.getLength() == 0) ) {
        // No data to process.  This may occur, for example, if a date range is requested that has no data
        return;
    }
    String dateTimeString = "";
    try {
        dateTimeString = ((Element)valuelist.item(0)).getAttribute("dateTime");
        // Strip off the hundredths
        int pos = dateTimeString.indexOf(".");
        if ( pos > 0 ) {
            dateTimeString = dateTimeString.substring(0,pos);
        }
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
        dateTimeString = ((Element)valuelist.item(valuelist.getLength() - 1)).getAttribute("dateTime");
        // Strip off the hundredths
        int pos = dateTimeString.indexOf(".");
        if ( pos > 0 ) {
            dateTimeString = dateTimeString.substring(0,pos);
        }
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
    String dataValueString;
    double dataValue;
    String dataFlag;
    DateTime dateTime;
    if ( readData ) {
        ts.allocateDataSpace();
        for (int i = 0; i < valuelist.getLength(); i++) {
            // Must parse the dates because missing results in gaps
            try {
                Element el = (Element) valuelist.item(i);
                dateTimeString = el.getAttribute("dateTime");
                // Strip off the hundredths
                int pos = dateTimeString.indexOf(".");
                if ( pos > 0 ) {
                    dateTimeString = dateTimeString.substring(0,pos);
                }
                dateTime = DateTime.parse(dateTimeString);
                dataFlag = el.getAttribute("qualifiers");
                dataValueString = el.getTextContent();
                if ( dataValueString.equals(noDataValue) ) {
                    dataValue = Double.NaN;
                }
                else {
                    dataValue = Double.parseDouble(el.getTextContent());
                }
                if ( (dataFlag != null) && !dataFlag.equals("") ) {
                    ts.setDataValue(dateTime, dataValue, dataFlag, 0);
                }
                else {
                    ts.setDataValue(dateTime, dataValue);
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
Read the time series list from the string WaterML.
@param interval indicates the interval for data in the file, needed as a hint because there is nothing
in the file that indicates that data are daily values, etc.
@param readData if true, read all the data values; if false, only initialize the time series header information
*/
public List<TS> readTimeSeriesList ( TimeInterval interval, boolean readData )
throws MalformedURLException, IOException, Exception
{   String routine = getClass().getName() + ".readTimeSeriesList";
    // Create the time series from the WaterML...
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    // Be aware of namespace to transparently handle ns1: in front of element names, but do not
    // do full validation with setValidating(true)
    dbf.setNamespaceAware(true);
    Document dom = dbf.newDocumentBuilder().parse(
        new ReaderInputStream(new StringReader(__waterMLString)));
    WaterMLVersion watermlVersion= determineWaterMLVersion(dom);
    List<TS> tsList = new ArrayList<TS>();
    String timeSeriesTag = "";
    if ( watermlVersion == WaterMLVersion.STANDARD_1_1 ) {
        timeSeriesTag = "timeSeries";
    }
    else {
        throw new IOException ( "WaterML version " + watermlVersion + " is not supported.");
    }
    NodeList timeSeries = dom.getDocumentElement().getElementsByTagNameNS("*",timeSeriesTag);
    Message.printStatus(2, routine, "Have " + timeSeries.getLength() +
        " WaterML timeSeries elements to process." );
    //Node node;
    for (int i = 0; i < timeSeries.getLength(); i++) {
        //node = timeSeries.item(i);
        //Message.printStatus(2, routine, "NodeLocalName=" + node.getLocalName() +
        //    ", namespace=" + node.getNamespaceURI() + ", name=" + node.getNodeName());
        tsList.add(readTimeSeries(watermlVersion, dom.getDocumentElement(), (Element)timeSeries.item(i),
            interval, __url, __file, readData ));
    }
    return tsList;
}

}