package rti.tscommandprocessor.commands.nrcs.awdb;

import gov.usda.nrcs.wcc.ns.awdbwebservice.AwdbWebService;
import gov.usda.nrcs.wcc.ns.awdbwebservice.AwdbWebService_Service;
import gov.usda.nrcs.wcc.ns.awdbwebservice.Data;
import gov.usda.nrcs.wcc.ns.awdbwebservice.Duration;
import gov.usda.nrcs.wcc.ns.awdbwebservice.HeightDepth;
import gov.usda.nrcs.wcc.ns.awdbwebservice.StationMetaData;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import riverside.datastore.AbstractWebServiceDataStore;

import RTi.TS.TS;
import RTi.TS.TSUtil;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
Data store for NRCS AWDB web service.  This class maintains the web service information in a general way.
This data store was created from a previous "input type" convention.  Consequently, this code is mainly a
wrapper for code that previously was developed.
*/
public class NrcsAwdbDataStore extends AbstractWebServiceDataStore
{

/**
ColoradoWaterSMS instance used as SOAP API.
*/
private AwdbWebService __awdbWebService = null;

/**
The list of network codes, listed here:  http://www.wcc.nrcs.usda.gov/web_service/AWDB_Web_Service_Reference.htm
*/
private List<NrcsAwdbNetworkCode> __networkCodeList = new Vector();

/**
The list of element codes, listed here:  http://www.wcc.nrcs.usda.gov/web_service/AWDB_Web_Service_Reference.htm
*/
private List<NrcsAwdbElementCode> __elementCodeList = new Vector();
    
/**
Constructor for web service.
*/
public NrcsAwdbDataStore ( String name, String description, URI serviceRootURI, PropList props )
throws URISyntaxException, IOException
{
    setName ( name );
    setDescription ( description );
    setServiceRootURI ( serviceRootURI );
    setProperties ( props );
    
    // The following as per the AWDB tutorial:
    // http://www.wcc.nrcs.usda.gov/web_service/AWDB_Web_Service_Tutorial.htm
    AwdbWebService_Service lookup = new AwdbWebService_Service(serviceRootURI.toString());
    setAwdbWebService ( lookup.getAwdbWebServiceImplPort() );
    
    // Initialize static data
    // Initialize the network codes - this may be available as a service at some point but for now inline
    __networkCodeList.add ( new NrcsAwdbNetworkCode("BOR","Any Bureau of Reclamation reservoir stations plus other non-BOR reservoir stations"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("CLMIND","Used to store climate indices (such as Southern Oscillation Index or Trans-Nino Index)"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("COOP","National Weather Service COOP stations"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("MPRC","Manual precipitation sites"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("MSNT","Manual SNOTEL non-telemetered, non-real time sites"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("SNOW","NRCS Snow Course Sites"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("SNTL","NWCC SNOTEL and SCAN stations"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("USGS","Any USGS station, but also other non-USGS streamflow stations"));
    // Initialize the element codes - this may be available as a service at some point but for now inline
    Message.printStatus(2, "", "DataStoreConfigFile=" + getProperty("DataStoreConfigFile") + ", ElementListCsv="+getProperty("ElementListCsv"));
    initializeElementList(getProperty("DataStoreConfigFile"),getProperty("ElementListCsv"));
}

/**
Factory method to construct a data store connection from a properties file.
@param filename name of file containing property strings
*/
public static NrcsAwdbDataStore createFromFile ( String filename )
throws IOException, Exception
{
    // Read the properties from the file
    PropList props = new PropList ("");
    props.setPersistentName ( filename );
    props.readPersistent ( false );
    props.set("DataStoreConfigFile",filename);
    String name = IOUtil.expandPropertyForEnvironment("Name",props.getValue("Name"));
    String description = IOUtil.expandPropertyForEnvironment("Description",props.getValue("Description"));
    String serviceRootURI = IOUtil.expandPropertyForEnvironment("ServiceRootURI",props.getValue("ServiceRootURI"));
    
    // Get the properties and create an instance

    NrcsAwdbDataStore ds = new NrcsAwdbDataStore( name, description, new URI(serviceRootURI), props );
    return ds;
}

/**
Format a date/time to the interval precision being queried.
@return the formatted date/time or null if requested period is null.
*/
private String formatDateTime ( DateTime dt, TimeInterval interval )
{
    if ( dt == null ) {
        return null;
    }
    int intervalBase = interval.getBase();
    if ( intervalBase == TimeInterval.YEAR ) {
        return dt.toString(DateTime.FORMAT_YYYY);
    }
    else if ( intervalBase == TimeInterval.MONTH ) {
        return dt.toString(DateTime.FORMAT_YYYY_MM);
    }
    else if ( intervalBase == TimeInterval.DAY ) {
        return dt.toString(DateTime.FORMAT_YYYY_MM_DD);
    }
    else if ( intervalBase == TimeInterval.IRREGULAR ) {
        return dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm);
    }
    else {
        return null;
    }
}

/**
Return the AwdbWebService instance used by the data store.
@return the AwdbWebService instance used by the data store
*/
public AwdbWebService getAwdbWebService ()
{
    return __awdbWebService;
}

/**
Return the list of elements that are available.  This returns the element code and optionally
the name.  Duplicates in the table are ignored.
@param includeName whether to include the description (use " - " as separator).
*/
public List<String> getElementStrings ( boolean includeName )
{   List<String> elementList = new Vector();
    for ( NrcsAwdbElementCode param: __elementCodeList ) {
        if ( includeName ) {
            elementList.add( "" + param.getCode() + " - " + param.getName() );
        }
        else {
            elementList.add( "" + param.getCode() );
        }
    }
    return elementList;
}

/**
Return the list of network that are available.  This returns the network code and optionally
the description.  Duplicates in the table are ignored.
@param includeDesc whether to include the description (use " - " as separator).
*/
public List<String> getNetworkStrings ( boolean includeDesc )
{   List<String> networkList = new Vector();
    for ( NrcsAwdbNetworkCode param: __networkCodeList ) {
        if ( includeDesc ) {
            networkList.add( "" + param.getCode() + " - " + param.getDescription() );
        }
        else {
            networkList.add( "" + param.getCode() );
        }
    }
    return networkList;
}

/**
Initialize the element list from the CSV file.
@param dataStoreFile the full path to the datastore configuration file.
@param elementListFile the relative path to the element list file.
*/
private void initializeElementList ( String dataStoreFile, String elementListFile )
{
    // Get the path from the dataStoreFile
    File dsf = new File(dataStoreFile);
    String elf = IOUtil.verifyPathForOS(IOUtil.toAbsolutePath(dsf.getParent(),elementListFile));
    if ( IOUtil.fileExists(elf) ) {
        // Read the delimited file and set the element codes
        PropList props = new PropList("");
        props.set("CommentLineIndicator","#");
        props.set("TrimInput","True");
        props.set("TrimStrings","True");
        try {
            DataTable table = DataTable.parseFile(elf, props);
            // Loop through the rows in the table and set the elements
            String name, code, units;
            TableRecord rec;
            int nameCol = table.getFieldIndex("Name");
            int codeCol = table.getFieldIndex("Code");
            int unitsCol = table.getFieldIndex("Units");
            for ( int i = 0; i < table.getNumberOfRecords(); i++ ) {
                rec = table.getRecord(i);
                name = (String)rec.getFieldValue(nameCol);
                code = (String)rec.getFieldValue(codeCol);
                units = (String)rec.getFieldValue(unitsCol);
                __elementCodeList.add(new NrcsAwdbElementCode(code,name,units));
            }
        }
        catch ( Exception e ) {
            Message.printWarning(3, "", e);
            // No input file?
        }
    }
}

/**
Lookup the AWDB duration from the internal TS interval.
*/
private Duration lookupDuration ( TimeInterval interval )
{
    if ( interval.getBase() == TimeInterval.YEAR ) {
        return Duration.ANNUAL;
    }
    else if ( interval.getBase() == TimeInterval.MONTH ) {
        return Duration.MONTHLY;
    }
    else if ( interval.getBase() == TimeInterval.DAY ) {
        return Duration.DAILY;
    }
    else if ( interval.getBase() == TimeInterval.IRREGULAR ) {
        return Duration.INSTANTANEOUS;
    }
    else {
        return null;
    }
}

/**
Parse an AWDB date/time string and return a DateTime instance.
*/
private DateTime parseDateTime ( String s )
{
    return DateTime.parse(s);
}

/**
Parse the network code from the station triplet ("StationID:State:Network")
*/
private String parseNetworkCodeFromTriplet(String stationTriplet)
{
    String [] parts = stationTriplet.split(":");
    return parts[2];
}

/**
Parse the state abbreviation from the station triplet ("StationID:State:Network")
*/
private String parseStateFromTriplet(String stationTriplet)
{
    String [] parts = stationTriplet.split(":");
    return parts[1];
}

/**
Parse the station ID from the station triplet ("StationID:State:Network")
*/
private String parseStationIDFromTriplet(String stationTriplet)
{
    String [] parts = stationTriplet.split(":");
    return parts[0];
}

/**
Read a list of time series from the web service, using query parameters that are supported for the web service.
@param element element code to match stations - must be null or a single code
*/
public List<TS> readTimeSeriesList ( List<String> stationIdList, List<String> stateList,
    List<NrcsAwdbNetworkCode>networkList, List<String> hucList, double [] boundingBox,
    List<String> countyList, NrcsAwdbElementCode element, Double elevationMin, Double elevationMax,
    TimeInterval interval, DateTime readStart, DateTime readEnd, boolean readData )
{   String routine = getClass().getName() + ".readTimeSeriesList";
    List<TS> tsList = new Vector();
    // First read the stations that match the basic criteria
    AwdbWebService ws = getAwdbWebService ();
    List<String> stationIds = stationIdList;
    List<String> stateCds = stateList;
    List<String> networkCds = new Vector();
    for ( NrcsAwdbNetworkCode n: networkList ) {
        networkCds.add(n.getCode());
    }
    List<String> hucs = hucList;
    List<String> countyNames = countyList;
    BigDecimal minLatitude = null;
    BigDecimal maxLatitude = null;
    BigDecimal minLongitude = null;
    BigDecimal maxLongitude = null;
    if ( boundingBox != null ) {
        minLatitude = new BigDecimal(boundingBox[1]);
        maxLatitude = new BigDecimal(boundingBox[3]);
        minLongitude = new BigDecimal(boundingBox[0]);
        maxLongitude = new BigDecimal(boundingBox[2]);
    }
    BigDecimal minElevation = null;
    if ( elevationMin != null ) {
        minElevation = new BigDecimal(elevationMin);
    }
    BigDecimal maxElevation = null;
    if ( elevationMax != null ) {
        maxElevation = new BigDecimal(elevationMax);
    }
    List<String> elementCds = new Vector();
    if ( element != null ) {
        elementCds.add ( element.getCode() );
    }
    List<Integer> ordinals = new Vector<Integer>();
    List<HeightDepth> heightDepths = new Vector<HeightDepth>();
    boolean logicalAnd = true;
    List<String> stationTriplets = ws.getStations(stationIds, stateCds, networkCds, hucs, countyNames,
        minLatitude, maxLatitude, minLongitude, maxLongitude, minElevation, maxElevation,
        elementCds, ordinals, heightDepths, logicalAnd );
    int nStations = 0;
    if ( stationTriplets != null ) {
        nStations = stationTriplets.size();
    }
    Message.printStatus(2,routine,"Read " + nStations + " stations from NRCS AWDB getStations(...) request." );
    if ( nStations == 0 ) {
        return tsList;
    }
    // Now get the list of station metadata for the stations
    List<StationMetaData> stationMetaData = ws.getStationMetadataMultiple(stationTriplets);
    TS ts;
    String tsid;
    String stationTriplet;
    String state;
    String stationID;
    String networkCode;
    String elementCode = element.getCode();
    int ordinal = 1;
    HeightDepth heightDepth = null;
    boolean getFlags = true;
    Duration duration = lookupDuration ( interval );
    String beginDate = ""; // Don't use null because that will cause an exception in SOAP API
    String endDate = "";
    if ( readStart != null ) {
        beginDate = formatDateTime(readStart,interval);
    }
    if ( readEnd != null ) {
        endDate = formatDateTime(readEnd,interval);
    }
    int iMeta = -1;
    for ( StationMetaData meta: stationMetaData ) {
        ++iMeta;
        stationTriplet = meta.getStationTriplet();
        state = parseStateFromTriplet(stationTriplet);
        stationID = parseStationIDFromTriplet(stationTriplet);
        networkCode = parseNetworkCodeFromTriplet(stationTriplet);
        String text;
        BigDecimal bd;
        int intervalBase, intervalMult;
        // Stations will only have been returned for requested matching element in first step
        tsid = state + "-" + stationID + "." + networkCode + "." + elementCode + "." + interval;
        try {
            ts = TSUtil.newTimeSeries(tsid,true);
            intervalBase = ts.getDataIntervalBase();
            intervalMult = ts.getDataIntervalMult();
            ts.setIdentifier(tsid);
            ts.setDescription(meta.getName());
            ts.setDate1Original(parseDateTime(meta.getBeginDate()));
            ts.setDate2Original(parseDateTime(meta.getEndDate()));
            ts.setDate1(parseDateTime(meta.getBeginDate()));
            ts.setDate2(parseDateTime(meta.getEndDate()));
            // Also set properties by passing through XML elements
            boolean setPropertiesFromMetadata = true;
            if ( setPropertiesFromMetadata ) {
                // Set time series properties from the timeSeries elements
                ts.setProperty("stationTriplet", (stationTriplet == null) ? "" : stationTriplet );
                text = meta.getName();
                ts.setProperty("name", (text == null) ? "" : text );
                text = meta.getActonId();
                ts.setProperty("actonId", (text == null) ? "" : text );
                text = meta.getShefId();
                ts.setProperty("shefId", (text == null) ? "" : text );
                text = meta.getBeginDate(); // Date station installed
                ts.setProperty("beginDate", (text == null) ? "" : text );
                text = meta.getEndDate(); // Date station discontinued
                ts.setProperty("endDate", (text == null) ? "" : text );
                text = meta.getCountyName();
                ts.setProperty("countyName", (text == null) ? "" : text );
                bd = meta.getElevation();
                ts.setProperty("elevation", (bd == null) ? null : bd.doubleValue() );
                text = meta.getFipsCountyCd();
                ts.setProperty("fipsCountyCd", (text == null) ? "" : text );
                text = meta.getFipsStateNumber();
                ts.setProperty("fipsStateNumber", (text == null) ? "" : text );
                text = meta.getFipsCountryCd();
                ts.setProperty("fipsCountryCd", (text == null) ? "" : text );
                text = meta.getHuc();
                ts.setProperty("huc", (text == null) ? "" : text );
                bd = meta.getLongitude();
                ts.setProperty("longitude", (bd == null) ? null : bd.doubleValue() );
                bd = meta.getLatitude();
                ts.setProperty("latitude", (bd == null) ? null : bd.doubleValue() );
                bd = meta.getStationDataTimeZone();
                ts.setProperty("stationDataTimeZone", (bd == null) ? null : bd.doubleValue() );
                bd = meta.getStationTimeZone();
                ts.setProperty("stationTimeZone", (bd == null) ? null : bd.doubleValue() );
            }
        }
        catch ( Exception e ) {
            continue;
        }
        if ( readData ) {
            Message.printStatus(2, routine, "Getting data values for triplet ("+ iMeta + " of " +
                stationMetaData.size() + ")=\"" + stationTriplet +
                "\" elementCode="+elementCode + " duration=" + duration + " beginDate=" + beginDate +
                " endDate=" + endDate);
            try {
                beginDate = "2011-01-01";
                endDate = "2012-11-01";
                stationTriplet = "471:ID:SNTL";
                //elementCode = "WTEQ";
                // Get the data values for the list of station triplets.
                // Since only one triplet is processed here, the data array will have one element.
                List<Data> dataList = ws.getData(Arrays.asList(stationTriplet), elementCode, ordinal, heightDepth,
                    duration, getFlags, beginDate, endDate);
                if ( dataList.size() == 1 ) {
                    // Have data values for the requested triplet and element code
                    Data data = dataList.get(0);
                    ts.setDate1(parseDateTime(data.getBeginDate()));
                    ts.setDate2(parseDateTime(data.getEndDate()));
                    ts.allocateDataSpace();
                    // Loop through the data values and set the values and the flag
                    List<BigDecimal> values = data.getValues();
                    List<String> flags = data.getFlags();
                    int nValues = values.size();
                    DateTime dt = new DateTime(ts.getDate1());
                    for ( int i = 0; i < nValues; i++, dt.addInterval(intervalBase,intervalMult) ) {
                        ts.setDataValue(dt, values.get(i).doubleValue(),flags.get(i),0);
                    }
                }
            }
            catch ( Exception e ) {
                Message.printWarning(3, routine, "Error getting data values (" + e + ").");
                Message.printWarning(3, routine, e);
            }
        }
        tsList.add(ts);
    }
    return tsList;
}

/**
Set the AwdbWebService instance that is used as the API
*/
private void setAwdbWebService ( AwdbWebService awdbWebService )
{
    __awdbWebService = awdbWebService;
}

}