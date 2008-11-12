package rti.tscommandprocessor.commands.hecdss;

import java.io.File;
import java.util.List;
import java.util.Vector;

import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDSSFileAccess;
import hec.heclib.dss.HecDataManager;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.util.Heclib;
import hec.heclib.util.HecTime;
import hec.heclib.util.doubleArrayContainer;
import hec.heclib.util.intArrayContainer;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;

import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

/**
 * Preliminary code to read/write HEC-DSS time series files - may refactor later.
 * @author sam
 *
 */
public class HecDssAPI
{

// TODO SAM Evaluate where to put this load call.
/**
Load the javaHeclib.dll library, which is used via JNI to run native code that
reads the DSS files.
*/
static 
{   String routine = "HECDSS.static";
    
    boolean loadUsingJavaLibraryPath = false;
    String javaHeclib = "";
    try {
        // This relies on the java.library.path to locate the javaHeclib.dll,
        // which may be problematic since that configuration is outside the control
        // of this software and may result in a version issue.
        if ( loadUsingJavaLibraryPath ) {
            javaHeclib = "javaHeclib";
            System.loadLibrary(javaHeclib);
        }
        else {
            // Instead, load the file explicitly knowing the application home and
            // assuming that the file exists in the bin folder.  This will probably fail because
            // the HEC package tries to load using the above method, which does not look for the
            // filename in the path, but matches the requested dll by name, which won't match the
            // path below, even though it is already loaded.
            javaHeclib = IOUtil.getApplicationHomeDir() + "/bin/javaHeclib.dll";
            Message.printStatus(2, routine, "Attempting to load library using System.load(" + javaHeclib + ")." );
            System.load( javaHeclib );
            Message.printStatus(2, routine, "Successfully loaded library \"" + javaHeclib + "\"" );
        }
    }
    catch ( UnsatisfiedLinkError e ) {
        if ( loadUsingJavaLibraryPath ) {
            Message.printWarning ( 2, routine, "Unable to load javaHeclib.dll using System.loadLibrary(" +
                javaHeclib + ") and java.library.path \"" + System.getProperty("java.library.path") + "\"." );
        }
        else {
            Message.printWarning ( 2, routine, "Unable to load javaHeclib.dll using System.load(" +
                javaHeclib + ")." );
        }
        Message.printWarning ( 2, routine, "HEC-DSS features will not be functional." );
        Message.printWarning( 3, routine, e);
    }
}

/**
Create a condensed pathname list.  Records that are duplicates except for D parts are removed from the list.
@param pathnameList A list of pathnames (as String).  This will be modified.
*/
private static List createCondensedCatalog ( List pathnameList )
{
    // Sort the pathnames to make sure that duplicates are grouped - seems like this is not needed
    // because the catalog is always sorted.  For now don't sort to increase performance
    
    // Loop through the pathnames.  Compare the parts with the previous item.  If all parts are the same
    // except for the D part, remove the item and change the D part to be inclusive
    
    int size = pathnameList.size();
    String aPart, aPartPrev = null; // Basin
    String bPart, bPartPrev = null; // Location
    String cPart, cPartPrev = null; // Data type
    String dPart, dPartPrev = null; // Period
    String ePart, ePartPrev = null; // Interval
    String fPart, fPartPrev = null; // Scenario
    DSSPathname dssPathName, dssPathNamePrev;
    String pathname;
    for ( int i = 0; i < size; i++ ) {
        pathname = (String)pathnameList.get(i);
        dssPathName = new DSSPathname ( pathname );
        aPart = dssPathName.getAPart();
        bPart = dssPathName.getBPart();
        cPart = dssPathName.getCPart();
        dPart = dssPathName.getDPart();
        ePart = dssPathName.getEPart();
        fPart = dssPathName.getFPart();
        int pos;
        if ( aPart.equals(aPartPrev) && bPart.equals(bPartPrev) && cPart.equals(cPartPrev) &&
                !dPart.equals(dPartPrev) && ePart.equals(ePartPrev) && fPart.equals(fPartPrev)) {
            // The parts match except for D part
            // Throw away the current pathname and update the D part in the kept record to be
            // the previous value to the current value
            if ( (dPart != null) && (dPart.trim().length() > 0) ) {
                pos = dPartPrev.indexOf("-");
                if ( pos < 0 ) {
                    // Previous D part was not a range so use it as is to construct a range
                    dssPathName.setDPart( dPartPrev + " - " + dPart );
                }
                else {
                    // Previous D part was a range so get the first part and then replace the second with
                    // the current D part
                    dssPathName.setDPart( dPartPrev.substring(0,pos).trim() + " - " + dPart );
                }
            }
            pathnameList.set(i - 1,dssPathName.toString() );
            pathnameList.remove(i);
            --i;
            --size;
            continue;
        }
        dssPathNamePrev = dssPathName;
        aPartPrev = aPart;
        bPartPrev = bPart;
        cPartPrev = cPart;
        dPartPrev = dPart;
        ePartPrev = ePart;
        fPartPrev = fPart;
    }
    
    return pathnameList;
}
     
/**
 * Read the available data types from the DSS file.
 */
public static Vector getDataTypes ( String filename )
{
    Vector dataTypes = new Vector();
    return dataTypes;
}

/**
Read a single time series from a HEC-DSS file.
*/
public static TS readTimeSeries ( File file, String tsident, DateTime readStart, DateTime readEnd,
        String unitsReq, boolean readData )
throws Exception
{
    Vector tslist = readTimeSeriesList ( file, tsident, readStart, readEnd, unitsReq, readData );
    if ( (tslist == null) || (tslist.size() == 0) ) {
        throw new RuntimeException ( "No time series were found matching \"" + tsident + "\"" );
    }
    else if ( tslist.size() != 1 ) {
        throw new RuntimeException ( "" + tslist.size() + " time series were found matching \"" +
                tsident + "\" - expecting 1." );
    }
    else {
        TS ts = (TS)tslist.get(0);
        return ts;
    }
}

/**
Read a list of time series from a HEC-DSS file.
@param file The absolute path to the file to read.
@param tsidentPattern A time series identifier string that may contain wild-cards.  The
input name should be the name of the HEC-DSS file to absolute or relative precision, consistent with how
the identifier should appear for data access.
@param readStart the DateTime to start reading data, with precision at least as fine as the time series.
If null, read all available data.
@param readEnd the DateTime to end reading data, with precision at least as fine as the time series.
If null, read all available data.
@param unitsReq requested units (under development, since HEC units may not be consistent with calling software).
@param readData if true, read the time series values.  If false, only read the metadata.
@return a list of time series that were read.
@throws Exception if there is an error reading the time series
*/
public static Vector readTimeSeriesList ( File file, String tsidentPattern,
        DateTime readStart, DateTime readEnd, String unitsReq, boolean readData )
throws Exception
{   String routine = "HecDssAPI.readTimeSeriesList";
    Vector tslist = new Vector();
    TSIdent tsident = new TSIdent ( tsidentPattern );
    String filename = file.getCanonicalPath();
    HecDSSFileAccess dssFile = new HecDSSFileAccess ( filename );
    int stat = dssFile.open();
    Message.printStatus ( 2, routine, "Status from opening DSS file is " + stat );
    // Use the following code to allow wildcards in matching 1+ time series
    // Normally wildcards will only be used when doing a bulk read and a single time series
    // will be matched for a specific time series identifier.
    // FIXME SAM 2008-11-07 What about the multiple D-part (start date)?
    // Is the D-part used to uniquely identify time series (like a save date) or is it used to
    // optimize memory management by saving periods with data and avoiding gaps
    // If there are multiple time series that are the same other than D-part,
    // can they be merged to form one time series?
    Vector pathnameList = new Vector();
    String aPartReq = StringUtil.getToken(tsident.getLocation(),"-",0,0); // From 1st part of Basin-Location in RTi TSID
    if ( aPartReq == null ) {
        aPartReq = "";
    }
    String bPartReq = StringUtil.getToken(tsident.getLocation(),"-",0,1); // From 2nd part of Basin-Location in RTi TSID
    if ( bPartReq == null ) {
        bPartReq = "";
    }
    String cPartReq = tsident.getType();
    String dPartReq = "*"; // Do not limit based on start - get all available data
    String ePartReq = tsident.getInterval();
    String fPartReq = tsident.getScenario();
    DateTime date1FromDPart = null;
    DateTime date2FromDPart = null;
    Message.printStatus ( 2, routine, "Requesting matching pathnames for " +
            "A=\"" + aPartReq + "\" " +
            "B=\"" + bPartReq + "\" " +
            "C=\"" + cPartReq + "\" " +
            "D=\"" + dPartReq + "\" " +
            "E=\"" + ePartReq + "\" " +
            "F=\"" + fPartReq + "\"" );
    // TODO SAM 2008-11-10 Replace the following with HecDSSUtilities.getCondensedCatalog() when moved to Java 6
    stat = dssFile.searchDSSCatalog(aPartReq, bPartReq, cPartReq, dPartReq, ePartReq, fPartReq, pathnameList);
    Message.printStatus ( 2, routine, "Status from searching catalog is " + stat +
            ".  Number of matching path names before condensing is " + pathnameList.size() );
    // Condense the pathnames because the D part might be redundant
    List condensedPathnameList = createCondensedCatalog(pathnameList);
    Message.printStatus ( 2, routine, "Number of matching path names after condensing is " +
            condensedPathnameList.size() );
    // Loop through the pathnames and read each time series
    // Get the period from the D part in case data are not being read
    for ( int i = 0; i < condensedPathnameList.size(); i++ ) {
        String alias = null;  // Set to TSID if TSID contains periods.
        boolean readData2 = readData;   // Modified below depending on whether know how to read data
        //Message.printStatus( 2, routine, "Pathname[" + i + "] = \"" + dssPath + "\"");
        DSSPathname dssPathName = new DSSPathname ( (String)pathnameList.get(i) );
        String aPart = dssPathName.getAPart();
        String bPart = dssPathName.getBPart();
        String cPart = dssPathName.getCPart();
        String dPart = dssPathName.getDPart();
        String dPart1 = null, dPart2 = null;
        if ( !dPart.equals("") ) {
            // Parse out - FIXME SAM 2008-09-02 Might be HECLIB code to do this
            int pos = dPart.indexOf("-");
            if ( pos >= 0 ) {
                // Have date range
                dPart1 = dPart.substring(0,pos).trim();
                dPart2 = dPart.substring(pos + 1).trim();
            }
            else {
                // Only have the first date so make the second one the same as the first
                dPart1 = dPart;
                dPart2 = dPart1;
            }
            date1FromDPart = new DateTime();
            date1FromDPart.setDay(Integer.parseInt(dPart1.substring(0,2)));
            date1FromDPart.setMonth(TimeUtil.monthFromAbbrev(dPart1.substring(2,5)));
            date1FromDPart.setYear(Integer.parseInt(dPart1.substring(5,9)));
            
            date2FromDPart = new DateTime();
            date2FromDPart.setDay(Integer.parseInt(dPart2.substring(0,2)));
            date2FromDPart.setMonth(TimeUtil.monthFromAbbrev(dPart2.substring(2,5)));
            date2FromDPart.setYear(Integer.parseInt(dPart2.substring(5,9)));
        }
        String ePart = dssPathName.getEPart();
        if ( ePart.equals("") ) {
            // If no E part, assume irregular interval
            // FIXME SAM 2008-11-10 Evaluate whether this is a good idea
            ePart = "Irregular";
        }
        String fPart = dssPathName.getFPart();
        Message.printStatus ( 2, routine,
                "A=\"" + aPart + "\" " +
                "B=\"" + bPart + "\" " +
                "C=\"" + cPart + "\" " +
                "D=\"" + dPart + "\" " +
                "E=\"" + ePart + "\" " +
                "F=\"" + fPart + "\"" );
        // Create time series...
        String dotfPart = "";
        String dotfPartNoPeriod = "";
        if ( !fPart.trim().equals("") ) {
            dotfPart = "." + fPart;
            dotfPartNoPeriod = "." + fPart.replace('.',' ');
        }
        // Make sure that the parts do not contain periods, which will mess up the time series.  If the parts
        // do contain periods, set an alias with the first part since the alias can contain parts and replace
        // the periods with dots.
        String tsidMain = aPart + "-" + bPart + ".HEC-DSS." + cPart + "." + ePart + dotfPart;
        String tsidMainNoPeriodsInParts = aPart.replace('.',' ') + "-" + bPart.replace('.',' ') + ".HEC-DSS." +
            cPart.replace('.',' ') + "." + ePart.replace('.',' ') + dotfPartNoPeriod;
        if ( aPart.indexOf(".") >= 0 ) {
            Message.printStatus(2 , routine, "TSID \"" + tsidMain +
                "\" has period(s) in A part.  Replace with spaces and assign TSID to alias." );
            alias = tsidMain;
        }
        if ( bPart.indexOf(".") >= 0 ) {
            Message.printStatus(2 , routine, "TSID \"" + tsidMain +
                "\" has period(s) in B part.  Replace with spaces and assign TSID to alias." );
            alias = tsidMain;
        }
        if ( fPart.indexOf(".") >= 0 ) {
            Message.printStatus(2 , routine, "TSID \"" + tsidMain +
                "\" has period(s) in F part.  Replace with spaces and assign TSID to alias." );
            alias = tsidMain;
        }
        String tsidentString = tsidMainNoPeriodsInParts + "~HEC-DSS~" + filename;
        TS ts = TSUtil.newTimeSeries(tsidentString, true);
        ts.setIdentifier( tsidentString );
        // Set the alias if the TSID had periods above
        if ( alias != null ) {
            ts.setAlias ( alias );
        }
        // Set the description to the location, space, and data type
        ts.setDescription ( ts.getLocation() + " " + ts.getDataType() );
        // Time series input name is the original HEC-DSS file
        ts.setInputName ( filename );
        // Set the start date from the D part but might be reset below from actual data.
        // Only set the end date if it is different from the start.  This will mimic the catalog in that
        // only ranges will be shown.
        ts.setDate1 ( date1FromDPart );
        if ( !date1FromDPart.equals(date2FromDPart) ) {
            ts.setDate2 ( date2FromDPart );
        }
        if ( readData ) {
            // Read the time series data.  This is the only way to get data units so reading
            // no data above will have blank units

            // For now just set the D part to first value because it does not seem that a range
            // is allowed when reading.
            // Setting to blank causes an exception - occurs because of irregular time series?
            if ( (dPart1 != null) && (dPart1.length() > 0) && (dPart.indexOf("-") >= 0) ) {
                dssPathName.setDPart(dPart1);
            }

            // Code snippet provided by Bill Charlie
            //TimeSeriesContainer tsc = new TimeSeriesContainer();  //  Bare bones object to hold data
            // Avoid the container because then we can use the low-level heclib directly, which avoids
            // about 7MB in the distribution
            HecTimeSeries rts = new HecTimeSeries();
            rts.setDSSFileName(filename);
            // If the read period has been requested, use it when reading the time series from HEC-DSS
            if ( (readStart != null) && (readEnd != null) ) {
                // Format of the period to read...
                //rts.setTimeWindow("04Sep1996 1200 05Sep1996 1200");  //  or you can just use correct D part
                String start = StringUtil.formatString(readStart.getDay(),"%02d") +
                    TimeUtil.monthAbbreviation(readStart.getMonth()) +
                    StringUtil.formatString(readStart.getYear(), "%04d");
                String end = StringUtil.formatString(readEnd.getDay(),"%02d") +
                    TimeUtil.monthAbbreviation(readEnd.getMonth()) +
                    StringUtil.formatString(readEnd.getYear(), "%04d");
                if ( (ts.getDataIntervalBase() == TimeInterval.HOUR) || (ts.getDataIntervalBase() == TimeInterval.MINUTE) ) {
                    start = start + " " + StringUtil.formatString(readStart.getHour(),"%02d");
                    end = end + " " + StringUtil.formatString(readEnd.getHour(),"%02d");
                    if ( ts.getDataIntervalBase() == TimeInterval.HOUR ) {
                        start = start + StringUtil.formatString(readStart.getMinute(),"%02d");
                        end = end + StringUtil.formatString(readEnd.getMinute(),"%02d");
                    }
                    else {
                        start = start + "00";
                        end = end + "00";
                    }
                }
                else {
                    // Always add 0000 on times
                    start = start + " 0000";
                    end = end + " 0000";
                }
                // Set the time range using the information from the catalog.
                String timeWindow = start + " " + end;
                Message.printStatus(2, routine, "Setting time window for read to \"" + timeWindow + "\"" );
                rts.setTimeWindow(timeWindow);
            }

            // Clear out the D part so by default all data are read.  If the period is set below
            // it will override D.  This does not seem to work.
            //dssPathName.setDPart("");
            Message.printStatus(2, routine, "Reading data using path \"" + dssPathName + "\"" );
            rts.setPathname(dssPathName.toString());
            // This appears to be equivalent to negative Float.MAX_VALUE
            ts.setMissing( Heclib.UNDEFINED_DOUBLE );

            //int status = rts.read(tsc, false); // false indicates whether to remove missing
            // Is there any way to read time series metadata without reading the records?
            // This would improve performance when data are not requested.
            
            if ( Message.isDebugOn ) {
                Message.printDebug(2, routine, "Before rts.read()" );
            }
            int status = rts.read ();
            // Some time series don't have a period so can't set dates in RTi TS from HecTimeSeries
            if ( Message.isDebugOn ) {
                Message.printDebug(2, routine, "Before setDataPeriod()" );
            }
            readData2 = setDataPeriod ( ts, rts, readStart, readEnd );

            ts.setDataUnits ( rts.units() );
            ts.setDataUnitsOriginal ( rts.units() );
            if (status < -1) {
                Message.printStatus(2,routine, "No Data for " + dssPathName + " in \"" + filename + "\"");
            }
            else if ( readData2 ) {
                // Transfer to the time series.
                if ( Message.isDebugOn ) {
                    Message.printStatus(2, routine, "Start transferring data." );
                }
                ts.allocateDataSpace();
                // Get the data values
                doubleArrayContainer values = new doubleArrayContainer();
                intArrayContainer times = new intArrayContainer();
                rts.getData ( values );
                rts.getTimes ( times );
                HecTime hecTime = new HecTime();
                // DateTime for iteration is copy of time series start to get precision
                DateTime date = new DateTime ( ts.getDate1() );
                for ( int idata = 0; idata < values.length; idata++ ) {
                    // Assume that access is direct on the arrays for performance reasons
                    hecTime.set ( times.array[idata] );
                    if ( Message.isDebugOn ) { 
                        Message.printDebug ( 10, routine, "Setting value " + values.array[idata] + " at " +
                            times.array[idata] + " (" + hecTime + ")");
                    }
                    if ( !hecTime.isDefined() || ts.isDataMissing(values.array[idata])) {
                        // Don't try to set because this may cause exceptions in some cases.
                        continue;
                    }
                    setDateTime ( date, hecTime );
                    ts.setDataValue( date, values.array[idata]);
                }
            }
        }
        /*
        // Code snippet taken from wcds.util.DssToPostListTranslator.getTimeSeries() and main()
        HecDataManager dataManager = new HecDataManager();
        dataManager.setDSSFileName ( filename );
        HecTimeSeries hects = getTimeSeries ( dataManager, dssPath );
        if ( hects != null ) {
            // Get the data units...
            String units = hects.units();
            ts.setDataUnits ( units );
            ts.setDataUnitsOriginal ( units );
            // Get the time range.
            setDataPeriod ( ts, hects );
            int intervalBase = ts.getDataIntervalBase();
            int intervalMult = ts.getDataIntervalMult();
            // FIXME SAM 2008-09-02 How many minutes in a month?
            int minutesInInterval = 1;
            if ( intervalBase == TimeInterval.MINUTE ) {
                minutesInInterval = intervalMult;
            }
            else if ( intervalBase == TimeInterval.DAY ) {
                minutesInInterval = 1440*intervalMult;
            }
            else {
                Message.printWarning ( 3, routine,
                    "Can only handle minute and day HEC-DSS data - not reading data for \"" + dssPath + "\"" );
                readData2 = false;
            }
            if ( readData2 ) {
                // Read the data records.  Increment HEC and RTi date/time objects parallel to each other
                DateTime date = new DateTime ( date1 );
                HecTime hectime = new HecTime ( hecStart );
                intContainer hecStatus = new intContainer();
                for ( ; date.lessThanOrEqualTo( date2);
                    date.addInterval(intervalBase,intervalMult),
                    hectime.increment(1,minutesInInterval) ) {
                    double value = hects.value ( hectime, hecStatus );
                    ts.setDataValue( date, value );
                }
            }
        }
        */
        // Add the time series
        tslist.add ( ts );
    }
    // Close out the file so that reading multiple files does not waste resources
    //rts.done();
    //rts.close();  //  Close the file (usually at the end of the program)
    dssFile.close();
    return tslist;
}

/**
 * Taken from wcds.util.DssToPostListTranslator
 * @param unitsCont
 * @param typeCont
 * @param dataManager
 * @param dssPath
 */
/*
public static void dssPathUnitsAndType(stringContainer unitsCont, stringContainer typeCont, HecDataManager dataManager, String dssPath)
{
    HecTimeSeries ts = getTimeSeries(dataManager, dssPath);
    if(ts == null)
    {
        return;
    } else
    {
        HecTime start = new HecTime();
        HecTime end = new HecTime();
        int num = ts.getTSRecordInfo(start, end, unitsCont, typeCont);
        return;
    }
}
*/

/**
 * Taken from wcds.util.DssToPostListTranslator
 * @param unitsCont
 * @param typeCont
 * @param dataManager
 * @param dssPath
 */
/*
public static HecTimeSeries getTimeSeries(HecDataManager dataManager, String dssPath)
{
    / * HEC logging
    if(setDssMessageLevel && dataManager != null)
    {
        int level = RMAIO.parseInt(System.getProperty("dss.message.level", "4"));
        if(level == 0x80000000)
            level = 4;
        if(WcdsProperties.WCDS_DEBUG >= 1)
            System.out.println("Setting DSS message level to: " + level);
        if(dataManager != null)
        {
            HecDataManager _tmp = dataManager;
            HecDataManager.setMessageLevel(level);
        }
        setDssMessageLevel = false;
    }
    * /
    HecDataManager dssDataSet = dataManager.createObject(dssPath);
    if(!(dssDataSet instanceof HecTimeSeries))
    {
        return null;
    } else
    {
        HecTimeSeries timeSeries = (HecTimeSeries)dssDataSet;
        return timeSeries;
    }
}
*/

/**
Set the period in the TS using the HecTimeSeries.
@param ts RTi time series to set period.
@param hects HEC time series to get period from.
@return true if the period can be set, false if not (undefined).
*/
private static boolean setDataPeriod ( TS ts, HecTimeSeries hects, DateTime readStart, DateTime readEnd )
{   String routine = "HecDSSAPI.setDataPeriod";
    // The precision of the dates will be handled based on the time series interval.
    HecTime hecStart = new HecTime();
    HecTime hecEnd = new HecTime();
    if ( Message.isDebugOn ) {
        Message.printDebug(2, routine, "Before hects.getSeriesTimeRange()" );
    }
    hects.getSeriesTimeRange ( hecStart, hecEnd, 0 );
    if ( Message.isDebugOn ) {
        Message.printStatus(2, routine, "Hec start = " + hecStart + " end = " + hecEnd );
    }
    if ( !hecStart.isDefined() ) {
        Message.printStatus(2, routine, "Hec start is not defined - not setting period for time series." );
        return false;
    }
    if ( !hecEnd.isDefined() ) {
        Message.printStatus(2, routine, "Hec end is not defined - not setting period for time series." );
        return false;
    }
    DateTime date1 = new DateTime();
    setDateTime ( date1, hecStart );
    DateTime date2 = new DateTime();
    setDateTime ( date2, hecEnd );
    // Set the dates in the file...
    ts.setDate1Original ( date1 );
    ts.setDate2Original ( date2 );
    // Set the dates as requested or in the file...
    if ( readStart != null ) {
        ts.setDate1 ( readStart );
    }
    else {
        ts.setDate1 ( date1 );
    }
    if ( readEnd != null ) {
        ts.setDate2 ( readEnd );
    }
    else {
        ts.setDate2 ( date2 );
    }

    return true;
}

/**
Set an RTi DateTime from a HecTime instance.
*/
private static void setDateTime ( DateTime date, HecTime hecTime )
{
    date.setYear( hecTime.year() );
    date.setMonth( hecTime.month() );
    date.setDay( hecTime.day() );
    boolean hour24 = false;
    if ( hecTime.hour() == 24 ) {
        // Set to hour 23 and then increment by one hour after setting everything, to roll to hour "24"
        // This is because RTi DateTime only allows hour 0
        date.setHour( 23 );
        hour24 = true;
    }
    else {
        date.setHour( hecTime.hour() );
    }
    date.setMinute( hecTime.minute() );
    date.setSecond( hecTime.second() );
    if ( hour24 ) {
        date.addHour(1);
    }
}

}