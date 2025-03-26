// NrcsAwdbDataStore - Data store for NRCS AWDB web service.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package rti.tscommandprocessor.commands.nrcs.awdb;

import gov.usda.nrcs.wcc.ns.awdbwebservice.AwdbWebService;
import gov.usda.nrcs.wcc.ns.awdbwebservice.AwdbWebService_Service;
import gov.usda.nrcs.wcc.ns.awdbwebservice.Data;
import gov.usda.nrcs.wcc.ns.awdbwebservice.DataSource;
import gov.usda.nrcs.wcc.ns.awdbwebservice.Duration;
import gov.usda.nrcs.wcc.ns.awdbwebservice.Element;
import gov.usda.nrcs.wcc.ns.awdbwebservice.Forecast;
import gov.usda.nrcs.wcc.ns.awdbwebservice.ForecastPeriod;
import gov.usda.nrcs.wcc.ns.awdbwebservice.ForecastPoint;
import gov.usda.nrcs.wcc.ns.awdbwebservice.HeightDepth;
import gov.usda.nrcs.wcc.ns.awdbwebservice.HourlyData;
import gov.usda.nrcs.wcc.ns.awdbwebservice.HourlyDataValue;
import gov.usda.nrcs.wcc.ns.awdbwebservice.InstantaneousData;
import gov.usda.nrcs.wcc.ns.awdbwebservice.InstantaneousDataFilter;
import gov.usda.nrcs.wcc.ns.awdbwebservice.InstantaneousDataValue;
import gov.usda.nrcs.wcc.ns.awdbwebservice.ReservoirMetadata;
import gov.usda.nrcs.wcc.ns.awdbwebservice.StationElement;
import gov.usda.nrcs.wcc.ns.awdbwebservice.StationMetaData;
import gov.usda.nrcs.wcc.ns.awdbwebservice.UnitSystem;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import riverside.datastore.AbstractWebServiceDataStore;
import RTi.TS.TS;
import RTi.TS.TSDataFlagMetadata;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

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
If null will be loaded in first get call.
*/
private List<NrcsAwdbNetworkCode> __networkCodeList = null;

/**
The list of element codes, listed here:  http://www.wcc.nrcs.usda.gov/web_service/AWDB_Web_Service_Reference.htm
If null will be loaded in first get call.
*/
private List<Element> __elementList = null;

/**
The list of forecast periods.  If null will be loaded in first get call.
*/
private List<ForecastPeriod> __forecastPeriodList = null;
    
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
    int connectionTimeout = -1;
    int receiptTimeout = -1;
    String propVal = props.getValue("ConnectTimeout");
    if ( (propVal != null) && !propVal.trim().equals("") ) {
    	int factor = 1000;
    	try {
    		// If property ends with "ms", then value is milliseconds, otherwise it is seconds
    		if ( propVal.toUpperCase().endsWith("MS") ) {
    			propVal = propVal.substring(0,propVal.length() - 2).trim();
    			factor = 1;
    		}
    		connectionTimeout = Integer.parseInt(propVal)*factor;
    	}
    	catch ( NumberFormatException e ) {
    		// For now ignore
    	}
    }
    propVal = props.getValue("ReadTimeout");
    if ( (propVal != null) && !propVal.trim().equals("") ) {
    	int factor = 1000;
    	try {
    		// If property ends with "ms", then value is milliseconds, otherwise it is seconds
    		if ( propVal.toUpperCase().endsWith("MS") ) {
    			propVal = propVal.substring(0,propVal.length() - 2).trim();
    			factor = 1;
    		}
    		receiptTimeout = Integer.parseInt(propVal)*factor;
    	}
    	catch ( NumberFormatException e ) {
    		// For now ignore
    	}
    }
    AwdbWebService_Service lookup = new AwdbWebService_Service(serviceRootURI.toString(),connectionTimeout,receiptTimeout);
    setAwdbWebService ( lookup.getAwdbWebServiceImplPort() );
    
    // Initialize cached data
    // Commonly used data are loaded in lazy fashion when first "get" call occurs.
    // - Element
    // - ForecastPeriod
    // - Network
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
    // Properties read and stored in the PropList
    // ConnectTimeout
    // ReadTimeout
    
    // Get the properties and create an instance

    NrcsAwdbDataStore ds = new NrcsAwdbDataStore( name, description, new URI(serviceRootURI), props );
    return ds;
}

/**
Format a date/time string for use with the web service calls.
@return the formatted date/time or null if requested period is null.
@param dt DateTime object to format
@param interval interval of time series, which by default controls date/time formatting
@param formatToSecond if true always format to the second
@param endPos if 0 the date/time is at the start date/time and if necessary additional information will be added
to match the start of the month; if 1, add to match the end of the month
*/
private String formatDateTime ( DateTime dt, TimeInterval interval, boolean formatToSecond, int endPos )
{
    if ( dt == null ) {
        return null;
    }
    int intervalBase = interval.getBase();
    if ( formatToSecond ) {
        if ( endPos == 1 ) {
            // Make sure day is set to number of days in month
            dt = (DateTime)dt.clone();
            if ( intervalBase == TimeInterval.YEAR ) {
                dt.setMonth(12);
            }
            if ( (intervalBase == TimeInterval.YEAR) || (intervalBase == TimeInterval.MONTH) ) {
                dt.setDay(TimeUtil.numDaysInMonth(dt));
            }
        }
        return dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm_SS);
    }
    else if ( intervalBase == TimeInterval.YEAR ) {
        return dt.toString(DateTime.FORMAT_YYYY);
    }
    else if ( intervalBase == TimeInterval.MONTH ) {
        return dt.toString(DateTime.FORMAT_YYYY_MM);
    }
    else if ( intervalBase == TimeInterval.DAY ) {
        return dt.toString(DateTime.FORMAT_YYYY_MM_DD);
    }
    else if ( intervalBase == TimeInterval.HOUR ) {
        return dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH);
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
{   List<String> elementList = new ArrayList<String>();
    if ( __elementList == null ) {
        // Using lazy loading so read the first time requested
        readElements();
    }
    for ( Element el: __elementList ) {
        if ( includeName ) {
            elementList.add( "" + el.getElementCd() + " - " + el.getName() );
        }
        else {
            elementList.add( "" + el.getElementCd() );
        }
    }
    return elementList;
}

/**
Return the list of forecast periods that are available.  Duplicates in the table are ignored.
@return forecast period strings
*/
public List<String> getForecastPeriodStrings ()
{   List<String> fpList = new ArrayList<String>();
    if ( __forecastPeriodList == null ) {
        // Lazy loading so need to read it now
        readForecastPeriods();
    }
    for ( ForecastPeriod fp: __forecastPeriodList ) {
        fpList.add( "" + fp.getForecastPeriod() );
    }
    Collections.sort(fpList,String.CASE_INSENSITIVE_ORDER);
    return fpList;
}

/**
Return the list of network that are available.  This returns the network code and optionally
the description.  Duplicates in the table are ignored.
@param includeDesc whether to include the description (use " - " as separator).
*/
public List<String> getNetworkStrings ( boolean includeDesc )
{   List<String> networkList = new ArrayList<String>();
    if ( __networkCodeList == null ) {
        // Use lazy loading so read on first request
        readNetworks();
    }
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
Lookup the AWDB duration from the internal TS interval.
*/
private Duration lookupDurationFromInterval ( TimeInterval interval )
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
Lookup the TimeInterval from the AWDB duration.
*/
private int lookupIntervalFromDuration ( Duration duration )
{
    if ( duration == Duration.ANNUAL ) {
        return TimeInterval.YEAR;
    }
    else if ( duration == Duration.MONTHLY ) {
        return TimeInterval.MONTH;
    }
    else if ( duration == Duration.DAILY ) {
        return TimeInterval.DAY;
    }
    else if ( // (duration == null) ||
        (duration == Duration.INSTANTANEOUS) ) {
        // TODO SAM 2012-11-08 confirm whether null duration means instantaneous in getStationElements results
        return TimeInterval.IRREGULAR;
    }
    else {
        return TimeInterval.UNKNOWN;
    }
}

/**
Parse an AWDB date/time string and return a DateTime instance.  If the date/time starts with 2100-01-01 (to indicate
active station), replace with the current date
@param s date/time string in format YYYY-MM-DD hh:mm
*/
private DateTime parseDateTime ( String s )
{
    if ( s.startsWith("2100-01-01") ) {
        // Value of 2100-01-01 00:00:00 is returned for end date for some web service methods
        // Since this value does not make sense for historical data, replace with the current date
        DateTime d = new DateTime(DateTime.DATE_CURRENT); // Any issues with time zone here?
        s = d.toString(DateTime.FORMAT_YYYY_MM_DD) + s.substring(10);
    }
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
Read the available elements from the web service and cache for further use.
*/
private void readElements ()
{
    AwdbWebService ws = getAwdbWebService ();
    __elementList = ws.getElements();
}

/**
Read the available forecast periods from the web service and cache for further use.
*/
private void readForecastPeriods ()
{
    AwdbWebService ws = getAwdbWebService ();
    __forecastPeriodList = ws.getForecastPeriods();
    //for ( ForecastPeriod fp : __forecastPeriodList ) {
    //    Message.printStatus(2,"","Forecast period = \"" + fp.getForecastPeriod() + "\"");
    //}
}

/**
Read a forecast table.
@param stationIdList the list of station identifiers to match (can be null or empty)
@param stateList the list of state abbreviations to match (can be null or empty)
@param netorkList the list of networks to match (can be null or empty)
@param hucList the list of HUC basin identifiers to match (can be null or empty)
@param elementList the list of elements to match (can be null or empty)
@param forecastPeriod the forecast period to match (can be null or empty)
@param forecastPublicationDateStart publication date start, YYYY-MM-DD hh:mm:ss, or null to read all
@param forecastPublicationDateEnd publication date end, YYYY-MM-DD hh:mm:ss, or null to read all
@param forecastExceedanceProbabilities exceedance probabilities to return, or null to read all
@param forecastTableID the name of the forecast table to create
*/
public DataTable readForecastTable ( List<String> stationIdList, List<String> stateList,
    List<NrcsAwdbNetworkCode>networkList, List<String> hucList, List<Element> elementList, String forecastPeriod,
    String forecastPublicationDateStart, String forecastPublicationDateEnd,
    int [] forecastExceedanceProbabilities, String forecastTableID )
{   String routine = "NrcsAwdbDataStore.readForecastTable";
    DataTable table = null;
    AwdbWebService ws = getAwdbWebService ();
    if ( (forecastExceedanceProbabilities != null) && (forecastExceedanceProbabilities.length == 0) ) {
        // Just set to null to simplify logic below
        forecastExceedanceProbabilities = null;
    }
    // First translate method parameters into types consistent with web service
    List<String> stationIds = stationIdList;
    List<String> stateCds = stateList;
    List<String> networkCds = new ArrayList<String>();
    for ( NrcsAwdbNetworkCode n: networkList ) {
        networkCds.add(n.getCode());
    }
    List<String> hucs = hucList;
    // TODO SAM 2013-11-04 Enable forecast point names and forecasters
    List<String> forecastPointNames = new ArrayList<String>();
    List<String> forecasters = new ArrayList<String>();
    boolean logicalAnd = true;
    // Create an output table with some standard columns containing the forecast information
    // Multiple forecasts can be retrieved and will need to be split out later with table manipulation commands
    List<TableField> columnList = new ArrayList<TableField>();
    table = new DataTable( columnList );
    int stationTripletCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "StationTriplet", -1, -1), null);
    int stateCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "State", -1, -1), null);
    int stationIDCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "StationID", -1, -1), null);
    int networkCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Network", -1, -1), null);
    int elementCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Element", -1, -1), null);
    int forecastPeriodCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "ForecastPeriod", -1, -1), null);
    int publicationDateCol = table.addField(new TableField(TableField.DATA_TYPE_DATETIME, "PublicationDate", -1, -1), null);
    int calculationDateCol = table.addField(new TableField(TableField.DATA_TYPE_DATETIME, "CalculationDate", -1, -1), null);
    int exceedanceProbabilityCol = table.addField(new TableField(TableField.DATA_TYPE_INT, "ExceedanceProbability", -1, 2), null);
    int valueCol = table.addField(new TableField(TableField.DATA_TYPE_DOUBLE, "Value", -1, 2), null);
    int unitCdCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "UnitCd", -1, -1), null);
    int periodAverageCol = table.addField(new TableField(TableField.DATA_TYPE_DOUBLE, "PeriodAverage", -1, 2), null);
    table.setTableID ( forecastTableID );
    // First get the forecast points that correspond to the location information
    List<ForecastPoint> forecastPoints = ws.getForecastPoints(stationIds, stateCds, networkCds,
        forecastPointNames, hucs, forecasters, logicalAnd );
    if ( (stationIds != null) && (stationIds.size() != forecastPoints.size()) ) {
        // Did not get data for all the forecast points
        // Generate warnings for each forecast point that did not have anything returned
        for ( String stationID : stationIds ) {
            boolean found = false;
            for ( ForecastPoint fp : forecastPoints ) {
                if ( stationID.equalsIgnoreCase(fp.getStationTriplet().split(":")[0])) {
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                Message.printWarning(3, routine, "Did not find forecast point for station \"" + stationID + "\"" );
            }
        } 
    }
    String [] tripletParts;
    int row = -1;
    String beginPublicationDate = null, endPublicationDate = null;
    if ( (forecastPublicationDateStart != null) && !forecastPublicationDateStart.equals("") ) {
        beginPublicationDate = forecastPublicationDateStart;
        if ( (forecastPublicationDateEnd == null) || forecastPublicationDateEnd.equals("") ) {
            // Set end to the same as the begin since both are required
            endPublicationDate = beginPublicationDate;
        }
    }
    if ( (forecastPublicationDateEnd != null) && !forecastPublicationDateEnd.equals("") ) {
        endPublicationDate = forecastPublicationDateEnd;
        if ( (forecastPublicationDateStart == null) || forecastPublicationDateStart.equals("") ) {
            // Set begin to the same as the end since both are required
            beginPublicationDate = endPublicationDate;
        }
    }
    for ( Element element: elementList ) {
        // Loop through the element codes of interest.  The API only takes one value.
        for ( ForecastPoint fp : forecastPoints ) {
            // Loop through the forecast points that matched the initial query
            Message.printStatus(2, routine, "Calling getForecasts for triplet=\"" + fp.getStationTriplet() +
                "\" elementCd=\"" + element.getElementCd() + "\" forecastPeriod=" + forecastPeriod );
            List<Forecast> forecasts;
            if ( (beginPublicationDate == null) && (endPublicationDate == null)) {
                forecasts = ws.getForecasts(fp.getStationTriplet(), element.getElementCd(), forecastPeriod);
            }
            else {
                forecasts = ws.getForecastsByPubDate(fp.getStationTriplet(), element.getElementCd(), forecastPeriod,
                    beginPublicationDate, endPublicationDate);
            }
            tripletParts = fp.getStationTriplet().split(":"); // StationID:State:Network
            for ( Forecast f : forecasts ) {
                if ( Message.isDebugOn ) {
                    Message.printDebug(1, routine, "Forecast calculationDate=" + f.getCalculationDate() +
                        ", elementCd=" + f.getElementCd() + ", forecastPeriod=" + f.getForecastPeriod() +
                        ", publicationDate=" + f.getPublicationDate() + ", stationTriplet=" + f.getStationTriplet() +
                        ", unitCd=" + f.getUnitCd() + ", periodAverage=" + f.getPeriodAverage());
                }
                List<Integer> eprob = f.getExceedenceProbabilities();
                List<BigDecimal> eval = f.getExceedenceValues();
                boolean includeProb = false;
                for ( int i = 0; i < eprob.size(); i++ ) {
                    if ( Message.isDebugOn ) {
                        Message.printDebug(1,routine,"Probability=" + eprob.get(i) + " value=" + eval.get(i) );
                    }
                    // Skip the probability if not requested
                    includeProb = true;
                    if ( forecastExceedanceProbabilities != null ) {
                        includeProb = false;
                        for ( int ip = 0; ip < forecastExceedanceProbabilities.length; ip++ ) {
                            if ( eprob.get(i) == forecastExceedanceProbabilities[ip] ) {
                                includeProb = true;
                                break;
                            }
                        }
                    }
                    if ( !includeProb ) {
                        continue;
                    }
                    // Set the values in the table.
                    ++row;
                    try {
                        table.setFieldValue(row, stationTripletCol, fp.getStationTriplet(), true);
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting station triplet for row " + row );
                    }
                    try {
                        table.setFieldValue(row, stateCol, tripletParts[1], true);
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting state for row " + row );
                    }
                    try {
                        table.setFieldValue(row, stationIDCol, tripletParts[0], true);
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting station ID for row " + row );
                    }
                    try {
                        table.setFieldValue(row, networkCol, tripletParts[2], true);
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting network for row " + row );
                    }
                    try {
                        table.setFieldValue(row, elementCol, element.getElementCd(), true);
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting element for row " + row );
                    }
                    try {
                        table.setFieldValue(row, forecastPeriodCol, forecastPeriod, true);
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting forecast period for row " + row );
                    }
                    try {
                        table.setFieldValue(row, publicationDateCol, DateTime.parse(f.getPublicationDate()), true);
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting publication date for row " + row );
                    }
                    try {
                        table.setFieldValue(row, calculationDateCol, DateTime.parse(f.getCalculationDate()), true);
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting calculation date for row " + row );
                    }
                    try {
                        BigDecimal v = eval.get(i);
                        if ( v == null ) {
                            table.setFieldValue(row, valueCol, null, true);
                        }
                        else {
                            table.setFieldValue(row, valueCol, v.doubleValue(), true);
                        }
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting value for row " + row );
                    }
                    try {
                        table.setFieldValue(row, unitCdCol, f.getUnitCd(), true);
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting units for row " + row );
                    }
                    try {
                        // Probability is an integer
                        table.setFieldValue(row, exceedanceProbabilityCol, eprob.get(i), true);
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting exceedance probability for row " + row );
                    }
                    try {
                        // Average is a BigDecimal so convert to Double
                        BigDecimal ave = f.getPeriodAverage();
                        if ( ave == null ) {
                            table.setFieldValue(row, periodAverageCol, ave, true);
                        }
                        else {
                            table.setFieldValue(row, periodAverageCol, ave.doubleValue(), true);
                        }
                    }
                    catch ( Exception e ) {
                        Message.printWarning(3, routine, "Error setting period average for row " + row );
                    }
                }
            } 
        }
    }
    return table;
}

/**
Read the network list
*/
private void readNetworks ()
{
    __networkCodeList = new ArrayList<NrcsAwdbNetworkCode>();
    // Initialize the network codes - this may be available as a service at some point but for now inline
    __networkCodeList.add ( new NrcsAwdbNetworkCode("ACIS","Applied Climate Information System (ACIS) stations"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("BOR","Any Bureau of Reclamation reservoir stations plus other non-BOR reservoir stations"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("CLMIND","Used to store climate indices (such as Southern Oscillation Index or Trans-Nino Index)"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("COOP","National Weather Service COOP stations"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("MPRC","Manual Precipitation Network stations"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("MSNT","Manual SNOTEL non-telemetered, non-real time sites"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("NRCSXP","NRCS experimental and test sites"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("OTHER","Telemetered NRCS stations that do not meet criteria for SNOTEL, SNOLITE, SCAN, or experimental"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("SCAN","Soil Climate Analysis Network (SCAN) stations"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("SNOW","NRCS Snow Course and areal marker stations"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("SNTL","Snow Telemetry Network stations"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("SNTLT","Snow Telemetry Network stations, limited sensors (SNOLITE)"));
    __networkCodeList.add ( new NrcsAwdbNetworkCode("USGS","Any USGS station, but also other non-USGS streamflow stations"));
}

/**
Read a single time series given the time series identifier (TSID).  The TSID parts are mapped into the SOAP
query parameters as if a single site has been specified, by calling the readTimeSeriesList() method.
@param tsid time series identifier string of form State-StationID.NetworkCode.ElementCode.Interval~DataStoreID
@param readStart the starting date/time to read, or null to read all data.
@param readEnd the ending date/time to read, or null to read all data.
@param readData if true, read the data; if false, construct the time series and populate properties but do
not read the data
@return the time series list read from the NRCS AWDB daily web services
*/
public TS readTimeSeries ( String tsid, DateTime readStart, DateTime readEnd, boolean readData )
throws MalformedURLException, IOException, Exception
{   // Initialize empty query parameters.
    List<String> stationIdList = new ArrayList<String>();
    List<String> stateList = new ArrayList<String>();
    List<String> hucList = new ArrayList<String>();
    double [] boundingBox = null;
    List<String> countyList = new ArrayList<String>();
    List<NrcsAwdbNetworkCode> networkList = new ArrayList<NrcsAwdbNetworkCode>();
    List<Element> elementList = new ArrayList<Element>();
    // Parse the TSID string and set in the query parameters
    TSIdent tsident = TSIdent.parseIdentifier(tsid);
    TimeInterval interval = TimeInterval.parseInterval(tsident.getInterval() );
    String loc = tsident.getLocation();
    String [] locParts = loc.split("-");
    stateList.add ( locParts[0] );
    stationIdList.add ( locParts[1] );
    networkList.add(new NrcsAwdbNetworkCode(tsident.getSource(),""));
    Element element = new Element();
    element.setElementCd(tsident.getType());
    elementList.add ( element );
    Hashtable<String,String> timeZoneMap = new Hashtable<String,String>();
    // The following should return one and only one time series.
    List<TS> tsList = readTimeSeriesList ( stationIdList, stateList, networkList, hucList, boundingBox,
        countyList, elementList, null, null, interval, readStart, readEnd, timeZoneMap, readData );
    if ( tsList.size() > 0 ) {
        return tsList.get(0);
    }
    else {
        return null;
    }
}

/**
Read a list of time series from the web service, using query parameters that are supported for the web service.
@param boundingBox bounding box as WestLong, SouthLat, EastLong, NorthLat (negatives for western hemisphere longitudes).
@param elementListReq list of requested element codes to match stations -
if null then the element list is queried for each station as processed
@param timeZoneMap a map that allows resetting the time zone ID from the NRCS value
(e.g., "-8.0" to a more standard representation such as "PST").
*/
public List<TS> readTimeSeriesList ( List<String> stationIdList, List<String> stateList,
    List<NrcsAwdbNetworkCode>networkList, List<String> hucList, double [] boundingBox, List<String> countyList,
    List<Element> elementListReq, Double elevationMin, Double elevationMax,
    TimeInterval interval, DateTime readStartReq, DateTime readEndReq, Hashtable<String,String> timeZoneMap, boolean readData )
{   String routine = getClass().getSimpleName() + ".readTimeSeriesList";
    List<TS> tsList = new ArrayList<TS>();
    // First translate method parameters into types consistent with web service
    AwdbWebService ws = getAwdbWebService ();
    List<String> stationIds = stationIdList;
    List<String> stateCds = stateList;
    List<String> networkCds = new ArrayList<String>();
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
    List<String> elementCds = new ArrayList<String>();
    if ( elementListReq != null ) {
        for ( Element el: elementListReq ) {
            elementCds.add ( el.getElementCd() );
        }
    }
    List<Integer> ordinals = new ArrayList<Integer>();
    List<HeightDepth> heightDepths = new ArrayList<HeightDepth>();
    boolean logicalAnd = true;
    List<String> stationTriplets = ws.getStations(stationIds, stateCds, networkCds, hucs, countyNames,
        minLatitude, maxLatitude, minLongitude, maxLongitude, minElevation, maxElevation,
        elementCds, ordinals, heightDepths, logicalAnd );
    // If nothing returned above, try to get reservoirs
    int nStations = 0;
    if ( stationTriplets != null ) {
        nStations = stationTriplets.size();
    }
    Message.printStatus(2,routine,"Read " + nStations + " stations from NRCS AWDB getStations(...) request." );
    if ( nStations == 0 ) {
        return tsList;
    }
    // TODO SAM 2012-11-08 Would be nice to know which of these to call from the element code,
    // without hard-coding
    // Now get the list of station metadata for the stations
    List<StationMetaData> stationMetaData = ws.getStationMetadataMultiple(stationTriplets);
    List<ReservoirMetadata> reservoirMetaDataList = null;
    // Check whether triplets have reservoirs, which will be the case if the network is BOR (NRCS email)
    boolean doReservoirs = false;
    for ( String st: stationTriplets ) {
        if ( st.toUpperCase().endsWith(":BOR") ) {
            doReservoirs = true;
            break;
        }
    }
    if ( doReservoirs ) {
        // Try getting reservoir metadata, which is in addition to station metadata and will be set below
        reservoirMetaDataList = ws.getReservoirMetadataMultiple(stationTriplets);
    }
    StringBuilder errorText = new StringBuilder();
    TS ts;
    String tsid;
    String stationTriplet;
    String state;
    String stationID;
    String networkCode;
    String elementCode;
    int ordinal = 1;
    HeightDepth heightDepth = null;
    Boolean alwaysReturnDailyFeb29 = false; // Want correct calendar to be used
    boolean getFlags = true;
    // Duration is only used with the getData() web service method (hour interval uses getHourlyData())
    Duration duration = lookupDurationFromInterval ( interval );
    String beginDateString = null; // Null means no requested period so read all
    String endDateString = null;
    // Date to read data and also to allocate time series.
    // May be reset below if no requested period.
    DateTime readStart = null, readEnd = null; // Will be set below to non-null to set time series period
    if ( readStartReq != null ) {
        readStart = new DateTime(readStartReq);
        beginDateString = formatDateTime(readStartReq,interval,true,0);
    }
    if ( readEndReq != null ) {
        readEnd = new DateTime(readEndReq);
        endDateString = formatDateTime(readEndReq,interval,true,1);
    }
    // Loop through the stations and then the elements for each station
    ReservoirMetadata metaRes;
    for ( StationMetaData meta: stationMetaData ) {
        stationTriplet = meta.getStationTriplet();
        state = parseStateFromTriplet(stationTriplet);
        stationID = parseStationIDFromTriplet(stationTriplet);
        networkCode = parseNetworkCodeFromTriplet(stationTriplet);
        // Find the matching reservoir metadata (may not have any)
        metaRes = null;
        if ( reservoirMetaDataList != null ) {
            for ( ReservoirMetadata res : reservoirMetaDataList ) {
                if ( res.getStationTriplet().equals(stationTriplet) ) {
                    metaRes = res;
                    break;
                }
            }
        }
        String text;
        BigDecimal bd;
        int intervalBase, intervalMult;
        // Get the elements that are valid for the station, specifying null for dates to ensure information
        List<StationElement> stationElementList = ws.getStationElements(stationTriplet, null, null);
        Message.printStatus(2,routine,"Read " + stationElementList.size() +
            " StationElements from NRCS AWDB getStationElements(" + stationTriplet + ") request." );
        // Now cut back the list to elements and intervals that were originally requested
        // See below for special handling of instantaneous/irregular data
        // The following is used to deal with instantaneous and hourly data because station elements do not
        // contain hourly for getHourlyData() or instantaneous for getInstantaneousData().  It is assumed that if a daily
        // time series is available that instantaneous and hourly may also be available.
        List<StationElement> dailyElementList = new ArrayList<StationElement>();
        for ( int iEl = 0; iEl < stationElementList.size(); iEl++ ) {
            boolean elementFound = true; // Default is all elements
            boolean intervalFound = false; // Must match interval
            StationElement sel = stationElementList.get(iEl);
            Message.printStatus(2, routine, "Checking station element=\"" + sel.getElementCd() + "\" duration=" +
                sel.getDuration() + " interval=" + lookupIntervalFromDuration(sel.getDuration()));
            if ( (elementListReq != null) && (elementListReq.size() > 0) ) {
                // Specific element codes were requested so check to see if the station element codes match...
                elementFound = false; // Only add to list if element matched
                for ( Element el : elementListReq ) {
                    if ( sel.getElementCd().equalsIgnoreCase(el.getElementCd())) {
                        // Matched the element
                        elementFound = true;
                        break;
                    }
                }
            }
            // Check for requested interval
            if ( lookupIntervalFromDuration(sel.getDuration()) == interval.getBase() ) {
                intervalFound = true;
            }
            if ( elementFound && lookupIntervalFromDuration(sel.getDuration()) == TimeInterval.DAY ) {
                // Save the daily record because may need to add an instantaneous/irregular/hour record
                dailyElementList.add(sel);
            }
            // If data from web service does not match the requested values, remove from the list so
            // the StationElement does not get processed below.
            if ( !elementFound || !intervalFound ) {
                // Available element/interval was not requested so remove from list
                // See special handling of instantaneous/irregular/hour data below
                if ( Message.isDebugOn ) {
                    Message.printDebug(1,routine,"Tossing out unrequested StationElement elementCode=" +
                        sel.getElementCd() + " duration=" + sel.getDuration());
                }
                stationElementList.remove(iEl);
                --iEl;
            }
            else {
                if ( Message.isDebugOn ) {
                    Message.printDebug(1,routine,"Keeping requested StationElement elementCode=" +
                        sel.getElementCd() + " duration=" + sel.getDuration());
                }
            }
        }
        if ( interval.getBase() == TimeInterval.IRREGULAR ) {
            // Request was for instantaneous/irregular data but the getStationElements method appears
            // to not indicate when instantaneous data are available so always add an entry.  If the
            // element was matched for daily interval, add an instantaneous object to continue processing
            boolean found = false;
            for ( StationElement selDaily: dailyElementList ) {
                // See if an instantaneous object was added - this would mean getData() can be called with duration Instantaneous?
                for ( StationElement sel: stationElementList ) {
                    if ( (sel.getDuration() == Duration.INSTANTANEOUS) && selDaily.getElementCd().equals(selDaily.getElementCd()) ) {
                        // Instantaneous StationElement was found above so no need to continue processing below
                        found = true;
                        break;
                    }
                }
                if ( !found ) {
                    // Instantaneous StationElement was not found above so add it
                    StationElement elAdd = new StationElement();
                    elAdd.setElementCd(selDaily.getElementCd());
                    elAdd.setDuration(Duration.INSTANTANEOUS);
                    elAdd.setBeginDate(selDaily.getBeginDate());
                    elAdd.setEndDate(selDaily.getEndDate());
                    elAdd.setStoredUnitCd(selDaily.getStoredUnitCd());
                    elAdd.setOriginalUnitCd(selDaily.getOriginalUnitCd());
                    if ( Message.isDebugOn ) {
                        Message.printDebug(2,routine,"Adding elementCode=" +
                            elAdd.getElementCd() + " duration=" + elAdd.getDuration());
                    }
                    stationElementList.add(elAdd);
                }
            }
        }
        else if ( interval.getBase() == TimeInterval.HOUR ) {
            // Request was for hour data but the getStationElements method appears
            // to not indicate when hour data are available because no Duration.HOUR so always add an entry if day data were available.
            // If the element was matched for daily data, add an hourly object to continue processing below
            for ( StationElement selDaily: dailyElementList ) {
                // Hourly StationElement was not found above because no Duration.HOUR so add it
                StationElement elAdd = new StationElement();
                elAdd.setElementCd(selDaily.getElementCd());
                elAdd.setDuration(null);
                elAdd.setBeginDate(selDaily.getBeginDate());
                elAdd.setEndDate(selDaily.getEndDate());
                elAdd.setStoredUnitCd(selDaily.getStoredUnitCd());
                elAdd.setOriginalUnitCd(selDaily.getOriginalUnitCd());
                if ( Message.isDebugOn ) {
                    Message.printDebug(2,routine,"Adding elementCode=" +
                        elAdd.getElementCd() + " duration=" + elAdd.getDuration() + " for hourly data because DAILY was found");
                }
                stationElementList.add(elAdd);
            }
        }
        // Process the remaining StationElement items...
        Message.printStatus(2,routine,"After filtering to match duration (interval) and element code, have " +
            stationElementList.size() + " StationElements from NRCS AWDB getStationElements(" + stationTriplet +
            ") request remaining." );
        int tscount = 0;
        for ( StationElement sel: stationElementList ) {
            // Process each element code that applies to the station
            elementCode = sel.getElementCd();
            tsid = state + "-" + stationID + "." + networkCode + "." + elementCode + "." + interval;
            String tzString = "";
            ++tscount;
            try {
                ts = TSUtil.newTimeSeries(tsid,true);
                ts.setIdentifier(tsid);
                intervalBase = ts.getDataIntervalBase();
                intervalMult = ts.getDataIntervalMult();
                ts.setMissing(Double.NaN);
                ts.setDescription(meta.getName());
                // Time zone is set to the stationDataTimeZone value
                // If the timeZoneMap is specified and has a key that matches stationDataTimeZone, use the value
                BigDecimal stationDataTimeZone = meta.getStationDataTimeZone();
                String tzString0 = "";
                if ( stationDataTimeZone != null ) {
                	tzString0 = tzString = "" + stationDataTimeZone; // Default, will be something like "-8.0" for PST
                	if ( tzString.startsWith("-") ) {
                		// GMT in front because something like -8.0 may mess up date/time parser
                		tzString = "GMT" + tzString;
                	}
                }
                // Always try the lookup because allowing blank key to reset to default value
                if ( timeZoneMap != null ) {
                	// Allow lookup on original -8.0 or modified GMT-8.0
                	String tzString2 = timeZoneMap.get(tzString0);
                	if ( tzString2 == null ) {
                		tzString2 = timeZoneMap.get(tzString);
                	}
                	if ( tzString2 != null ) {
                		// Use the requested time zone instead of internal
                		tzString = tzString2;
                	}
                } 
                ts.setDate1Original(parseDateTime(sel.getBeginDate())); // Sensor install date
                ts.setDate1Original(ts.getDate1Original().setTimeZone(tzString));
                // Sensor end, or 2100-01-01 00:00 if active (switched to current because don't want 2100)
                ts.setDate2Original(parseDateTime(sel.getEndDate()));
                ts.setDate2Original(ts.getDate2Original().setTimeZone(tzString));
                // The following will be reset below if reading data but are OK for discovery mode...
                if ( readStartReq != null ) {
                    ts.setDate1(readStartReq);
                    ts.setDate1(ts.getDate1().setTimeZone(tzString));
                }
                else {
                    // Set the period to read from the data
                    ts.setDate1(ts.getDate1Original());
                    ts.setDate1(ts.getDate1().setTimeZone(tzString));
                }
                if ( readEndReq != null ) {
                    ts.setDate2(readEndReq);
                    ts.setDate2(ts.getDate2().setTimeZone(tzString));
                }
                else {
                    // Set the period to read from the data
                    ts.setDate2(ts.getDate2Original());
                    ts.setDate2(ts.getDate2().setTimeZone(tzString));
                }
                // Set the data units
                ts.setDataUnits(sel.getStoredUnitCd());
                ts.setDataUnitsOriginal(sel.getOriginalUnitCd());
                // Set the flag descriptions
                ts.addDataFlagMetadata(new TSDataFlagMetadata("V", "Valid"));
                ts.addDataFlagMetadata(new TSDataFlagMetadata("S", "Suspect"));
                ts.addDataFlagMetadata(new TSDataFlagMetadata("E", "Edited"));
                // Also set properties by passing through XML elements
                boolean setPropertiesFromMetadata = true;
                if ( setPropertiesFromMetadata ) {
                    // Set time series properties from the timeSeries elements
                    ts.setProperty("stationTriplet", (stationTriplet == null) ? "" : stationTriplet );
                    ts.setProperty("state", (state == null) ? "" : state );
                    ts.setProperty("stationId", (stationID == null) ? "" : stationID );
                    ts.setProperty("networkCode", (networkCode == null) ? "" : networkCode );
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
                    text = meta.getHud();
                    ts.setProperty("hud", (text == null) ? "" : text );
                    bd = meta.getLongitude();
                    ts.setProperty("longitude", (bd == null) ? null : bd.doubleValue() );
                    bd = meta.getLatitude();
                    ts.setProperty("latitude", (bd == null) ? null : bd.doubleValue() );
                    Integer i = sel.getDataPrecision();
                    ts.setProperty("dataPrecision", (i == null) ? null : i );
                    DataSource s = sel.getDataSource();
                    ts.setProperty("dataSource", (i == null) ? null : "" + s );
                    int i2 = sel.getOrdinal();
                    ts.setProperty("ordinal", Integer.valueOf(i2) );
                    HeightDepth hd = sel.getHeightDepth();
                    if ( hd == null ) {
                        ts.setProperty("heightDepthValue", null );
                        ts.setProperty("heightDepthUnitCd", null );
                    }
                    else {
                        ts.setProperty("heightDepthValue", (hd.getValue() == null) ? null : Double.valueOf(hd.getValue().doubleValue()));
                        ts.setProperty("heightDepthUnitCd", (hd.getUnitCd() == null) ? "" : hd.getUnitCd() );
                    }
                    ts.setProperty("stationDataTimeZone", (stationDataTimeZone == null) ? null : stationDataTimeZone.doubleValue() );
                    bd = meta.getStationTimeZone();
                    ts.setProperty("stationTimeZone", (bd == null) ? null : bd.doubleValue() );
                    if ( metaRes != null ) {
                        // Set reservoir properties...
                        bd = metaRes.getElevationAtCapacity();
                        ts.setProperty("elevationAtCapacity", (bd == null) ? null : bd.doubleValue() );
                        bd = metaRes.getReservoirCapacity();
                        ts.setProperty("reservoirCapacity", (bd == null) ? null : bd.doubleValue() );
                        bd = metaRes.getUsableCapacity();
                        ts.setProperty("usableCapacity", (bd == null) ? null : bd.doubleValue() );
                    }
                }
            }
            catch ( Exception e ) {
                Message.printWarning(3,routine,e);
                errorText.append ( "\n" + e );
                continue;
            }
            if ( readData ) {
                // Reset the date to read based on the full period available from the StationElement
                double missing = ts.getMissing();
                if ( readStartReq == null ) {
                    beginDateString = sel.getBeginDate();
                }
                if ( readEndReq == null ) {
                    endDateString = sel.getEndDate();
                    if ( endDateString.startsWith("2100-01-01") ) {
                        // Value of 2100-01-01 00:00:00 is returned for end date for some web service methods
                        // Since this value does not make sense for historical data, replace with the current date
                        DateTime d = new DateTime(DateTime.DATE_CURRENT); // Any issues with time zone here?
                        endDateString = d.toString(DateTime.FORMAT_YYYY_MM_DD) +
                            (endDateString.length() > 10 ? endDateString.substring(10) : "") ;
                    }
                }
                Message.printStatus(2, routine, "Getting data values for triplet ("+ tscount + " of " +
                    stationElementList.size() + ")=\"" + stationTriplet +
                    "\" elementCode="+elementCode + " duration=" + duration + " beginDate=" + beginDateString +
                    " endDate=" + endDateString);
                if ( duration == Duration.INSTANTANEOUS ) {
                	// Instantaneous data so use getInstantaneousData web service call
                    try {
                        // Get the data values for the list of station triplets.
                        // Since only one triplet is processed here, the data array will have one element.
                        List<InstantaneousData> dataList = ws.getInstantaneousData(Arrays.asList(stationTriplet),
                            elementCode, ordinal, heightDepth,
                            beginDateString, endDateString, InstantaneousDataFilter.ALL, UnitSystem.ENGLISH );
                        if ( dataList.size() == 1 ) {
                            // Have data values for the requested station triplet and element code
                            InstantaneousData data = dataList.get(0);
                            List<InstantaneousDataValue> values = data.getValues();
                            int nValues = values.size();
                            Message.printStatus(2, routine, "Have " + nValues + " data values for triplet " + stationTriplet );
                            // If a period is not requested, set to the available StationElement data period
                            if ( readStartReq == null ) {
                                readStart = DateTime.parse(beginDateString);
                                ts.setDate1(readStart);
                                ts.setDate1(ts.getDate1().setTimeZone(tzString));
                            }
                            if ( readEndReq == null ) {
                                readEnd = new DateTime(ts.getDate1());
                                readEnd = TimeUtil.addIntervals(readEnd,intervalBase,intervalMult,(nValues - 1));
                                ts.setDate2(readEnd);
                                ts.setDate2(ts.getDate2().setTimeZone(tzString));
                            }
                            // Loop through the data values and set the values and the flag
                            // The date/time is provided with each value so there should be no issues parsing
                            DateTime dt;
                            String dtString;
                            BigDecimal value;
                            String flag;
                            for ( int i = 0; i < nValues; i++ ) {
                                dtString = values.get(i).getTime();
                                try {
                                    dt = DateTime.parse(dtString);
                                }
                                catch ( Exception e ) {
                                    // Should not happen
                                    String message = "Error parsing date/time \"" + dtString + "\" - skipping data value.";
                                    Message.printWarning(3, routine, message );
                                    errorText.append ( "\n" + message );
                                    continue;
                                }
                                value = values.get(i).getValue();
                                flag = values.get(i).getFlag();
                                if ( value == null ) {
                                    // Might still have a flag
                                    if ( (flag != null) && !flag.equals("") ) {
                                        ts.setDataValue(dt,ts.getMissing(),flag,0);
                                    }
                                }
                                else {
                                    // Value is not missing but flag may be null
                                    if ( flag == null ) {
                                        ts.setDataValue(dt, value.doubleValue());
                                    }
                                    else {
                                        ts.setDataValue(dt, value.doubleValue(),flag,0);
                                    }
                                }
                            }
                        }
                    }
                    catch ( Exception e ) {
                        String message = "Error getting instantaneous data values (" + e + ").";
                        Message.printWarning(3, routine, message );
                        Message.printWarning(3, routine, e);
                        errorText.append ( message );
                    }
                }
                else if ( interval.getBase() == TimeInterval.HOUR ) {
                	// Hourly data so use getHourlyData web service call
                    try {
                        // Get the hourly data values for the list of station triplets.
                        // Since only one triplet is processed here, the data array will have one element.
                        // Make sure the begin and end date strings are YYYY-MM-DD and that the hour is extracted from
                        // the strings.
                        int beginHour = 0, endHour = 23;
                        // beginHour and endHour apply to each day!  These should not be needed if the request period contains the hour but
                        // to be certain, specify getting all the hours.  The following code might set begin and end hour to 0 and only return
                        // even-hour measurements.
                        //if ( readStart != null ) {
                            //beginHour = readStart.getHour();
                        	//beginHour = -1;
                        //}
                        //if ( readEnd != null ) {
                            //endHour = readEnd.getHour();
                        	//beginHour = -1;
                        //}
                        Message.printStatus(2, routine, "Calling getHourlyData with stationTriplet=\"" + stationTriplet +
                            "\" elementCode=\"" + elementCode + "\" ordinal=" + ordinal + " heightDepth=" + heightDepth +
                            " beginDateString=" + beginDateString + " endDateString=" + endDateString + " beginHour=" +
                            beginHour + " endHour=" + endHour );
                        List<String> stationTriplets1 = new ArrayList<String>(1);
                        stationTriplets1.add(stationTriplet);
                        List<HourlyData> dataList = ws.getHourlyData(stationTriplets1, elementCode, ordinal, heightDepth,
                            beginDateString, endDateString, beginHour, endHour );
                        Message.printStatus(2, routine,"getHourlyData returned " + dataList.size() + " objects.");
                        if ( dataList.size() != 1 ) {
                            Message.printStatus(2, routine,"Was expecting 1 record from getHourlyData but got " + dataList.size() );
                            for ( HourlyData data: dataList ) {
                            	Message.printStatus(2,routine,"" + data.getStationTriplet() + " " + data.getBeginDate() + " " + data.getEndDate() );
                            }
                        }
                        else {
                            // Have data values for the single requested station triplet and element code so OK to continue
                            HourlyData data = dataList.get(0);
                            List<HourlyDataValue> values = data.getValues();
                            int nValues = values.size();
                            // If a period is requested, the time series period will have been set above.
                            // If a period is not requested, set to the available StationElement data period
                            if ( readStartReq == null ) {
                                readStart = DateTime.parse(data.getBeginDate());
                                ts.setDate1(readStart);
                                ts.setDate1(ts.getDate1().setTimeZone(tzString));
                            }
                            if ( readEndReq == null ) {
                                readEnd = DateTime.parse(data.getEndDate());
                                ts.setDate2(readEnd);
                                ts.setDate2(ts.getDate2().setTimeZone(tzString));
                            }
                            ts.allocateDataSpace();
                            Message.printStatus(2, routine, "Have " + nValues + " data values for triplet " + stationTriplet +
                                " starting on " + data.getBeginDate() + " ending on " + data.getEndDate() + " expecting " +
                                ts.getDataSize() + " if values are recorded every hour." );
                            // Loop through the data values and set the values and the flag
                            // Use the dates returned in the data list to set the period because time series
                            // requested period may differ (although should be the same)
                            DateTime dt = null;
                            BigDecimal value;
                            String flag;
                            HourlyDataValue hourlyValue;
                            String dateTime; // format YYYY-MM-dd
                            for ( int i = 0; i < nValues; i++ ) {
                            	// Get values, which will correspond to XML:
                                // <values>
                                //   <dateTime>2000-01-01 00:00</dateTime>
                                //   <flag>V</flag>
                                //   <value>7.00</value>
                                // </values>
                                hourlyValue = values.get(i);
                                if ( hourlyValue == null ) {
                                    continue;
                                }
                                else {
                                    value = hourlyValue.getValue();
                                    flag = hourlyValue.getFlag();
                                    dateTime = hourlyValue.getDateTime();
                                    // Use the specified date/time to set data rather than rely on aligning with requested period
                                    // because there may be gaps in the data.
                                    dt = DateTime.parse(dateTime);
                                    if ( value == null ) {
                                        // Value is missing but flag may be non-null
                                        if ( flag != null ) {
                                            ts.setDataValue(dt, missing,flag,0);
                                        }
                                    }
                                    else {
                                        // Value is not missing
                                        if ( flag == null ) {
                                            ts.setDataValue(dt, value.doubleValue());
                                        }
                                        else {
                                            ts.setDataValue(dt, value.doubleValue(),flag,0);
                                        }
                                    }
                                    if ( Message.isDebugOn ) {
                                    	Message.printDebug(1, routine, "Date " + dateTime + " value=" + value + " flag=" + flag);
                                    }
                                }
                            }
                        }
                    }
                    catch ( Exception e ) {
                        String message = "Error getting hourly data values (" + e + ").";
                        Message.printWarning(3, routine, message );
                        Message.printWarning(3, routine, e);
                        errorText.append ( message );
                    }
                }
                else {
                	// Not instantaneous or hour interval so use getData() web service call
                    try {
                        // Get the data values for the list of station triplets.
                        // Since only one triplet is processed here, the data array will have one element.
                        Message.printStatus(2, routine, "Calling getData with stationTriplet=\"" + stationTriplet +
                            "\" elementCode=\"" + elementCode + "\" ordinal=" + ordinal + " heightDepth=" + heightDepth +
                            " duration=" + sel.getDuration() + " getFlags=" + getFlags + " beginDateString=" +
                            beginDateString + " endDateString=" + endDateString + " alwaysReturnDailyFeb29=" +
                            alwaysReturnDailyFeb29 );
                        // FIXME SAM 2013-11-10 There is a disconnect where requesting the longer period from the station
                        // data returns that period, but values are relative to shorter non-missing data period in time series
                        List<Data> dataList = ws.getData(Arrays.asList(stationTriplet), elementCode, ordinal, heightDepth,
                            sel.getDuration(), getFlags, beginDateString, endDateString, alwaysReturnDailyFeb29);
                        if ( dataList.size() == 1 ) {
                            // Have data values for the requested station triplet and element code
                            Data data = dataList.get(0);
                            List<BigDecimal> values = data.getValues();
                            List<String> flags = data.getFlags();
                            int nValues = values.size();
                            boolean nrcsBug = true; // Issue that getData() begin and end dates don't seem to agree with the data
                            if ( nrcsBug ) {
                                // Dates returned from web service are wrong. Use what was specified for the read
                                DateTime dt1 = DateTime.parse(beginDateString);
                                ts.setDate1(dt1);
                                DateTime dt2 = DateTime.parse(endDateString);
                                ts.setDate2(dt2);
                                ts.setDate1(ts.getDate1().setTimeZone(tzString));
                                ts.setDate2(ts.getDate2().setTimeZone(tzString));
                            }
                            else {
                                // If the period was not requested, reset to what was returned here
                                if ( readStartReq == null ) {
                                    readStart = DateTime.parse(data.getBeginDate());
                                    ts.setDate1(readStart);
                                    ts.setDate1(ts.getDate1().setTimeZone(tzString));
                                }
                                if ( readEndReq == null ) {
                                    readEnd = DateTime.parse(data.getEndDate());
                                    ts.setDate2(readEnd);
                                    ts.setDate2(ts.getDate2().setTimeZone(tzString));
                                }
                            }
                            if ( nrcsBug ) {
                                // FIXME SAM 2013-12-18 Need this check because of web service bug
                                if ( data.getBeginDate() == null ) {
                                    Message.printStatus(2, routine, "Begin date returned from getData() is null - unable to read time series.");
                                    continue;
                                }
                                else if ( !beginDateString.equals(data.getBeginDate())) {
                                    String message = "Requested begin date " + beginDateString +
                                    " does not match getData() returned begin date string " + data.getBeginDate() +
                                    " - retrying query using start date of " + data.getBeginDate() + ".";
                                    Message.printWarning(3,routine,message);
                                    // FIXME SAM 2013-11-10 There is a disconnect where requesting the longer period from the station
                                    // data returns that period, but values are relative to shorter non-missing data period in time series
                                    beginDateString = data.getBeginDate();
                                    dataList = ws.getData(Arrays.asList(stationTriplet), elementCode, ordinal, heightDepth,
                                        sel.getDuration(), getFlags, beginDateString, endDateString, alwaysReturnDailyFeb29);
                                    if ( dataList.size() == 1 ) {
                                        // Have data values for the requested station triplet and element code
                                        data = dataList.get(0);
                                        values = data.getValues();
                                        flags = data.getFlags();
                                        nValues = values.size();
                                        if ( nrcsBug ) {
                                            // Dates returned from web service are wrong. Use what was specified for the read
                                            DateTime dt1 = DateTime.parse(beginDateString);
                                            ts.setDate1(dt1);
                                            ts.setDate1(ts.getDate1().setTimeZone(tzString));
                                            DateTime dt2 = DateTime.parse(endDateString);
                                            ts.setDate2(dt2);
                                            ts.setDate2(ts.getDate2().setTimeZone(tzString));
                                        }
                                        else {
                                            // If the period was not requested, reset to what was returned here
                                            if ( readStartReq == null ) {
                                                readStart = DateTime.parse(data.getBeginDate());
                                                ts.setDate1(readStart);
                                                ts.setDate1(ts.getDate1().setTimeZone(tzString));
                                            }
                                            if ( readEndReq == null ) {
                                                readEnd = DateTime.parse(data.getEndDate());
                                                ts.setDate2(readEnd);
                                                ts.setDate2(ts.getDate2().setTimeZone(tzString));
                                            }
                                        }
                                    }
                                }
                                // Check it again...
                                if ( !beginDateString.equals(data.getBeginDate())) {
                                    String message = "Requested begin date " + beginDateString +
                                    " does not match getData() returned begin date string " + data.getBeginDate() +
                                    " - AWDB getData() web service cannot be worked around.";
                                    Message.printWarning(3,routine,message);
                                    errorText.append("\n"+ message);
                                    throw new RuntimeException(message);
                                }
                                /* TODO SAM 2014-02-01 this is limiting the software.  If the begin date is OK then
                                   hopefully things are OK
                                if ( !endDateString.equals(data.getEndDate())) {
                                    String message = "Requested end date " + endDateString +
                                    " does not match getData() returned end date string " + data.getEndDate() +
                                    " - AWDB getData() web service cannot be worked around.";
                                    Message.printWarning(3,routine,message);
                                    errorText.append("\n"+ message);
                                    throw new RuntimeException(message);
                                }
                                */
                            }
                            ts.allocateDataSpace();
                            Message.printStatus(2, routine, "Have " + nValues + " data values for triplet " + stationTriplet +
                                " starting on " + data.getBeginDate() + " ending on " + data.getEndDate() + " expecting " +
                                ts.getDataSize() + " based on data begin/end" );
                            // Loop through the data values and set the values and the flag
                            // Use the dates returned in the data list because time series requested period may differ
                            // FIXME SAM 2013-11-10 Despite the fact that begin date is returned, have to use the date
                            // that was requested in the query - this seems like a bug and has been reported
                            //DateTime dt = DateTime.parse(data.getBeginDate());
                            DateTime dt = DateTime.parse(beginDateString);
                            dt.setPrecision(ts.getDate1().getPrecision());
                            BigDecimal value;
                            String flag;
                            for ( int i = 0; i < nValues; i++, dt.addInterval(intervalBase,intervalMult) ) {
                                value = values.get(i);
                                flag = flags.get(i);
                                if ( value == null ) {
                                    // Might still have a flag
                                    if ( (flag != null) && !flag.equals("") ) {
                                        ts.setDataValue(dt,missing,flag,0);
                                    }
                                    //Message.printStatus(2, routine, "Date " + dt + " value=" + value);
                                }
                                else {
                                    // Value is not missing but flag may be null
                                    if ( flag == null ) {
                                        ts.setDataValue(dt, value.doubleValue());
                                    }
                                    else {
                                        ts.setDataValue(dt, value.doubleValue(),flag,0);
                                    }
                                    //Message.printStatus(2, routine, "Date " + dt + " value=" + value);
                                }
                            }
                        }
                    }
                    catch ( Exception e ) {
                        String message = "Error getting data values (" + e + ").";
                        Message.printWarning(3, routine, message);
                        Message.printWarning(3, routine, e);
                        errorText.append("\n"+ message);
                    }
                }
            }
            tsList.add(ts);
        }
    }
    if ( errorText.length() > 0 ) {
        throw new RuntimeException ( errorText.toString() );
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
