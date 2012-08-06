package rti.tscommandprocessor.commands.usgs.nwis.daily;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisParameterType;
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisSiteTimeSeriesMetadata;
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisStatisticType;
import rti.tscommandprocessor.commands.wateroneflow.waterml.WaterMLReader;

import RTi.TS.TS;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

// TODO SAM 2012-02-28 Need to enable
/**
Data store for USGS NWIS daily value web services.
<pre>
http://waterservices.usgs.gov/rest/Site-Service.html
</pre>
@author sam
*/
public class UsgsNwisDailyDataStore extends AbstractWebServiceDataStore
{
    
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
public UsgsNwisDailyDataStore ( String name, String description, URI serviceRootURI )
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
    //__statisticTypeList.add ( new UsgsNwisStatisticType("00009","STD","Standard deviation values"));
    //__statisticTypeList.add ( new UsgsNwisStatisticType("00010","Variance","Variance values"));
    //__statisticTypeList.add ( new UsgsNwisStatisticType("00021","Tidal high","High high-tide values"));
    //__statisticTypeList.add ( new UsgsNwisStatisticType("00022","Tidal high","High high-tide values"));
    //__statisticTypeList.add ( new UsgsNwisStatisticType("00023","Tidal high","High high-tide values"));
    //__statisticTypeList.add ( new UsgsNwisStatisticType("00024","Tidal high","High high-tide values"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("01001","0.1 Percentile","0.1 Percentile"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("01002","0.2 Percentile","0.2 Percentile"));
    __statisticTypeList.add ( new UsgsNwisStatisticType("01983","98.3 Percentile","98.3 Percentile"));
    //__statisticTypeList.add ( new UsgsNwisStatisticType("32359","Observation at 23:59",
    //    "Instantaneous observation at time hhmm where hhmm runs from 00001 to 2400"));
}

/**
Factory method to construct a data store connection from a properties file.
@param filename name of file containing property strings
*/
public static UsgsNwisDailyDataStore createFromFile ( String filename )
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

    UsgsNwisDailyDataStore ds = new UsgsNwisDailyDataStore( name, description, new URI(serviceRootURI) );
    ds.setProperties(props);
    return ds;
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
are consistent with TSTool ("Day", rather than "daily").  There is only one choice since using the daily
value web service.
*/
public List<String> getDataIntervalStringsForDataType ( String dataType )
{   List<String> dataIntervalStrings = new Vector();
    dataIntervalStrings.add("Day");
    return dataIntervalStrings;
}

/**
Return the list of parameters that are available.  Currently this returns the parameter code and optionally
the name.  Duplicates in the table are ignored.
TODO SAM 2011-01-07 It would be good to have the option of using data type abbreviations instead of
numeric codes, but this may not be an option.
@param includeName whether to include the name.
*/
public List<String> getParameterStrings ( boolean includeName )
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
Read a time series list given the query parameters for the REST interface.  The parameters are used to
form the URL for the query.  The payload that is received is optionally saved as the output file.  The payload
is then parsed into 1+ time series and returned.
@param readStart the starting date/time to read, or null to read all data.
@param readEnd the ending date/time to read, or null to read all data.
@param readData if true, read the data; if false, construct the time series and populate properties but do
not read the data
@return the time series list read from the USGS NWIS daily web services
*/
public List<TS> readTimeSeriesList ( List<String> siteList, List<String> stateList,
    List<String> hucList, double[] boundingBox, List<String> countyList,
    List<UsgsNwisParameterType> parameterList, List<UsgsNwisStatisticType> statisticTypeList,
    UsgsNwisSiteStatusType siteStatus, List<UsgsNwisSiteType> siteTypeList, String agency,
    UsgsNwisFormatType format, String outputFile,
    DateTime readStart, DateTime readEnd, boolean readData )
throws MalformedURLException, IOException, Exception
{
    String routine = getClass().getName() + ".readTimeSeriesList";
    List<TS> tslist = new Vector();

    // Form the URL, starting with the root
    StringBuffer urlString = new StringBuffer("" + getServiceRootURI() );
    // Specify these in the order of the web service API documentation
    // Major filter - location, pick the first one specified
    List<String> queryParameters = new Vector(); // Correspond to each query argument - ? and & handled later
    // Site list
    if ( siteList.size() > 0 ) {
        StringBuffer b = new StringBuffer("sites=");
        for ( int i = 0; i < siteList.size(); i++ ) {
            if ( i > 0 ) {
                b.append(",");
            }
            b.append(siteList.get(i));
        }
        queryParameters.add(b.toString());
    }
    // State list
    else if ( stateList.size() > 0 ) {
        StringBuffer b = new StringBuffer("stateCd=");
        for ( int i = 0; i < stateList.size(); i++ ) {
            if ( i > 0 ) {
                b.append(",");
            }
            b.append(stateList.get(i));
        }
        queryParameters.add(b.toString());
    }
    // HUC list
    else if ( hucList.size() > 0 ) {
        StringBuffer b = new StringBuffer("huc=");
        for ( int i = 0; i < hucList.size(); i++ ) {
            if ( i > 0 ) {
                b.append(",");
            }
            b.append(hucList.get(i));
        }
        queryParameters.add(b.toString());
    }
    // Bounding box
    else if ( (boundingBox != null) && (boundingBox.length == 4) ) {
        StringBuffer b = new StringBuffer("bBox=");
        for ( int i = 0; i < boundingBox.length; i++ ) {
            if ( i > 0 ) {
                b.append(",");
            }
            b.append(StringUtil.formatString(boundingBox[i],"%.6f"));
        }
        queryParameters.add(b.toString());
    }
    // County list
    else if ( countyList.size() > 0 ) {
        StringBuffer b = new StringBuffer("countyCd=");
        for ( int i = 0; i < countyList.size(); i++ ) {
            if ( i > 0 ) {
                b.append(",");
            }
            b.append(countyList.get(i));
        }
        queryParameters.add(b.toString());
    }
    // The start and end date.  If not reading data, don't specify dates and the last value will
    // be returned.  If reading data, there is no way to request "all"
    // TODO SAM 2012-02-29 Figure if there is a way to request all
    if ( !readData ) {
        // Specify a minimal period to try a query and make sure that the time series is defined.
        // If no period is specified the latest value will be returned.
        readStart = null;
        readEnd = null;
    }
    if ( readStart != null ) {
        queryParameters.add("startDT=" + readStart.toString(DateTime.FORMAT_YYYY_MM_DD));
    }
    if ( readEnd != null ) {
        queryParameters.add("endDT=" + readEnd.toString(DateTime.FORMAT_YYYY_MM_DD));
    }
    // Format
    if ( format != null ) {
        queryParameters.add("format=" + format);
    }
    // Parameter list
    if ( parameterList.size() > 0 ) {
        StringBuffer b = new StringBuffer("parameterCd=");
        for ( int i = 0; i < parameterList.size(); i++ ) {
            String code = parameterList.get(i).getCode();
            if ( (code != null) && !code.equals("") ) {
                if ( i > 0 ) {
                    b.append(",");
                }
                b.append(code);
            }
        }
        queryParameters.add(b.toString());
    }
    // Statistic list
    if ( statisticTypeList.size() > 0 ) {
        StringBuffer b = new StringBuffer("statCd=");
        for ( int i = 0; i < statisticTypeList.size(); i++ ) {
            String code = statisticTypeList.get(i).getCode();
            if ( (code != null) && !code.equals("") ) {
                if ( i > 0 ) {
                    b.append(",");
                }
                b.append(code);
            }
        }
        queryParameters.add(b.toString());
    }
    // Site status
    if ( siteStatus != null ) {
        queryParameters.add("siteStatus=" + siteStatus);
    }
    // Site types
    if ( siteTypeList.size() > 0 ) {
        StringBuffer b = new StringBuffer("siteType=");
        for ( int i = 0; i < siteTypeList.size(); i++ ) {
            String code = siteTypeList.get(i).getCode();
            if ( (code != null) && !code.equals("") ) {
                if ( i > 0 ) {
                    b.append(",");
                }
                b.append(code);
            }
        }
        queryParameters.add(b.toString());
    }
    // Site was modified (not currently supported)
    // TODO SAM 2012-02-29 Evaluate whether useful
    // Agency code
    if ( (agency != null) && !agency.equals("") ) {
        queryParameters.add("agencyCd=" + agency);
    }
    // Altitude (not currently supported)
    // TODO SAM 2012-02-29 Evaluate whether useful
    // Surface water arguments (not currently supported)
    // TODO SAM 2012-02-29 Evaluate whether useful
    // Groundwater arguments (not currently supported)
    // TODO SAM 2012-02-29 Evaluate whether useful
    // Hole depth (not currently supported)
    // TODO SAM 2012-02-29 Evaluate whether useful
    // Now process the query parameters and handle ? and &
    if ( queryParameters.size() > 0 ) {
        urlString.append ( "?");
    }
    for ( int i = 0; i < queryParameters.size(); i++ ) {
        if ( i > 0 ) {
            urlString.append ( "&" );
        }
        urlString.append ( queryParameters.get(i) );
    }
    Message.printStatus(2, routine, "Performing the following request:  " + urlString.toString() );
    String resultString = IOUtil.readFromURL(urlString.toString());
    // TODO SAM 2012-02-29 Might want to constrain this more based on error codes
    // so it does not bloat the log, especially since the response can be written to the output file
    if ( Message.isDebugOn ) {
        Message.printStatus(10,routine,"Returned data="+resultString);
    }
    if ( resultString.indexOf("error") >= 0 ) {
        throw new IOException ( "Error retrieving data:  " + resultString + " (" + resultString + ")." );
    }
    else {
        // Save the output to a file if requested
        if ( (outputFile != null) && !outputFile.equals("") ) {
            try {
                IOUtil.writeFile(outputFile, resultString);
                Message.printStatus ( 2, routine, "Wrote output to file \"" + outputFile + "\"." );
            }
            catch ( Exception e ) {
                Message.printWarning(3,routine,"Error writing output file \"" + outputFile + "\" (" + e + ")." );
            }
        }
        if ( format == UsgsNwisFormatType.WATERML ) {
            // Create the time series from the WaterML...
            WaterMLReader watermlReader = new WaterMLReader ( resultString, urlString.toString(), null );
            // This is necessary because WaterML (1.1 at least) does not appear to have a clear indicator of
            // the time series data interval
            TimeInterval interval = TimeInterval.parseInterval("Day");
            // Pass the input period here because it is used for memory allocation and the time series
            // in the data my have gaps that cause the period to be different
            tslist = watermlReader.readTimeSeriesList( interval, readStart, readEnd, readData );
        }
        else {
            Message.printWarning(3, routine, "USGS NWIS Daily format " + format +
                " is not supported for conversion to time series." );
        }
    }
    return tslist;
}

}