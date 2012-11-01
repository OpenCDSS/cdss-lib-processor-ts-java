package rti.tscommandprocessor.commands.usgs.nwis.groundwater;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Vector;

import riverside.datastore.AbstractWebServiceDataStore;
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisFormatType;
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisParameterType;
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisSiteStatusType;
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisSiteTimeSeriesMetadata;
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisSiteType;
import rti.tscommandprocessor.commands.wateroneflow.waterml.WaterMLReader;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
Data store for USGS NWIS daily value web services.
<pre>
http://waterservices.usgs.gov/rest/GW-Levels-Service.html
</pre>
@author sam
*/
public class UsgsNwisGroundwaterDataStore extends AbstractWebServiceDataStore
{
    
/**
The records of valid parameters, listed here:  http://help.waterdata.usgs.gov/codes-and-parameters/parameters
*/
private List<UsgsNwisParameterType> __parameterTypeList = new Vector();
    
/**
Constructor for web service.
*/
public UsgsNwisGroundwaterDataStore ( String name, String description, URI serviceRootURI )
throws URISyntaxException, IOException
{
    setName ( name );
    setDescription ( description );
    setServiceRootURI ( serviceRootURI );
    // Initialize the supported parameter types - this may be available as a service at some point but for now inline
    __parameterTypeList.add ( new UsgsNwisParameterType("62610","Physical","Groundwater level above NGVD 1929, feet","","","feet"));
    __parameterTypeList.add ( new UsgsNwisParameterType("62611","Physical","Groundwater level above NAVD 1988, feet","","","feet"));
    __parameterTypeList.add ( new UsgsNwisParameterType("72019","Physical","Depth to water level, feet below land surface","","Depth to water level below land surface","ft"));
    __parameterTypeList.add ( new UsgsNwisParameterType("72020","Physical","Elevation above NGVD 1929, feet","","","feet"));
    // Not sure how to deal with this...
    //__parameterTypeList.add ( new UsgsNwisParameterType("72150","Used to capture local datums for locations remote from the continental United States","","",""));
}

/**
Factory method to construct a data store connection from a properties file.
@param filename name of file containing property strings
*/
public static UsgsNwisGroundwaterDataStore createFromFile ( String filename )
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

    UsgsNwisGroundwaterDataStore ds = new UsgsNwisGroundwaterDataStore( name, description, new URI(serviceRootURI) );
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
are consistent with TSTool ("Day", rather than "daily").  Allow daily and irregular.
*/
public List<String> getDataIntervalStringsForDataType ( String dataType )
{   List<String> dataIntervalStrings = new Vector();
    dataIntervalStrings.add("Day");
    dataIntervalStrings.add("Irregular");
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
    // FIXME SAM 2012-10-17 Need to figure out how to handle groundwater data store
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
    metadataList.add(metadata);
    return metadataList;
}

/**
Read a single time series given the time series identifier (TSID).  The TSID parts are mapped into the REST
query parameters as if a single site has been specified, by calling the readTimeSeriesList() method.
@param tsid time series identifier string of form SiteID..ParameterCode-StatisticCode.Day~DataStoreID
@param readStart the starting date/time to read, or null to read all data.
@param readEnd the ending date/time to read, or null to read all data.
@param readData if true, read the data; if false, construct the time series and populate properties but do
not read the data
@return the time series list read from the USGS NWIS daily web services
*/
public TS readTimeSeries ( String tsid, DateTime readStart, DateTime readEnd, boolean readData )
throws MalformedURLException, IOException, Exception
{   // Initialize empty query parameters.
    List<String> siteList = new Vector();
    List<String> stateList = new Vector();
    List<String> hucList = new Vector();
    double [] boundingBox = null;
    List<String> countyList = new Vector();
    List<UsgsNwisParameterType> parameterList = new Vector<UsgsNwisParameterType>();
    UsgsNwisSiteStatusType siteStatus = UsgsNwisSiteStatusType.ALL;
    List<UsgsNwisSiteType> siteTypeList = new Vector<UsgsNwisSiteType>();
    String agency = null;
    UsgsNwisFormatType format = UsgsNwisFormatType.WATERML;
    String outputFile = null;
    // Parse the TSID string and set in the query parameters
    TSIdent tsident = TSIdent.parseIdentifier(tsid);
    TimeInterval interval = TimeInterval.parseInterval(tsident.getInterval() );
    siteList.add ( tsident.getLocation() );
    parameterList.add ( new UsgsNwisParameterType(tsident.getMainType(), "", "", "", "", "") );
    // The following should return one and only one time series.
    List<TS> tsList = readTimeSeriesList ( siteList, stateList, hucList, boundingBox, countyList,
        parameterList, siteStatus, siteTypeList, agency,
        format, outputFile, interval, readStart, readEnd, readData );
    if ( tsList.size() > 0 ) {
        return tsList.get(0);
    }
    else {
        return null;
    }
}

/**
Read a time series list given the query parameters for the REST interface.  The parameters are used to
form the URL for the query.  The payload that is received is optionally saved as the output file.  The payload
is then parsed into 1+ time series and returned.
@param siteList the list of sites to read
@param stateList the list of state abbreviations to select sites
@param hucList the list of HUCs to select sites
@param boundingBox the bounding box to select sites
@param countyList the list of FIPS county codes to select sites
@param parameterList the list of parameters to read
@param siteStatus the site status to constrain the query
@param siteTypeList the list of site types to constrain the query
@param agency the agency to constrain the query
@param format the output file format
@param outputFile the output file to create
@param interval the time series interval to use for output
@param readStart the starting date/time to read, or null to read all data.
@param readEnd the ending date/time to read, or null to read all data.
@param readData if true, read the data; if false, construct the time series and populate properties but do
not read the data
@return the time series list read from the USGS NWIS groundwater web services
*/
public List<TS> readTimeSeriesList ( List<String> siteList, List<String> stateList,
    List<String> hucList, double[] boundingBox, List<String> countyList,
    List<UsgsNwisParameterType> parameterList,
    UsgsNwisSiteStatusType siteStatus, List<UsgsNwisSiteType> siteTypeList, String agency,
    UsgsNwisFormatType format, String outputFile, TimeInterval interval,
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
            // Pass the input period here because it is used for memory allocation and the time series
            // in the data my have gaps that cause the period to be different
            boolean requireDataToMatchInterval = false; // Output OK as daily, even if more precise date/times
            tslist = watermlReader.readTimeSeriesList( interval, readStart, readEnd, readData, requireDataToMatchInterval );
        }
        else {
            Message.printWarning(3, routine, "USGS NWIS Daily format " + format +
                " is not supported for conversion to time series." );
        }
    }
    return tslist;
}

}