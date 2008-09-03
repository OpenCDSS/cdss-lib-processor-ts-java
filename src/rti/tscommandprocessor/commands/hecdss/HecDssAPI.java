package rti.tscommandprocessor.commands.hecdss;

import java.util.Vector;

import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDSSFileAccess;
import hec.heclib.dss.HecDataManager;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.util.HecTime;
import hec.heclib.util.intContainer;
import hec.heclib.util.stringContainer;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;

//import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;
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
    
    try {
        // This relies on the java.library.path to locate the javaHeclib.dll,
        // which may be problematic since that configuration is outside the control
        // of this software and may result in a version issue.
        //System.loadLibrary("javaHeclib");
        
        // Instead, load the file explicitly knowing the application home and
        // assuming that the file exists in the bin folder
        //String javaHeclib = IOUtil.getApplicationHomeDir() + "/bin/javaHeclib.dll";
        String javaHeclib = "javaHeclib";
        Message.printStatus(2, routine, "Attempting to load library \"" + javaHeclib + "\"" );
        //System.load( javaHeclib );
        System.loadLibrary("javaHeclib");
        Message.printStatus(2, routine, "Successfully loaded library \"" + javaHeclib + "\"" );
    }
    catch ( UnsatisfiedLinkError e ) {
        Message.printWarning ( 2, routine, "Unable to load javaHeclib.dll using java.library.path \"" +
                System.getProperty("java.library.path") + "\"" );
        Message.printWarning ( 2, routine, "HEC-DSS features will not be functional." );
        Message.printWarning( 3, routine, e);
    }
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
public static TS readTimeSeries ( String tsident, DateTime readStart, DateTime readEnd,
        String unitsReq, boolean readData )
throws Exception
{
    Vector tslist = readTimeSeriesList ( tsident, readStart, readEnd, unitsReq, readData );
    if ( (tslist == null) || (tslist.size() == 0) ) {
        throw new RuntimeException ( "No time series were found matching \"" +
                tsident + "\"" );
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
Read a single time series from a HEC-DSS file.
*/
public static Vector readTimeSeriesList ( String tsidentPattern, DateTime readStart, DateTime readEnd,
        String unitsReq, boolean readData )
throws Exception
{   String routine = "HECDSS.readTimeSeriesList";
    Vector tslist = new Vector();
    // No constructor that takes filename...
    //HecDSSFileDataManager hm = new HecDSSFileDataManager();
    
    TSIdent tsident = new TSIdent ( tsidentPattern );
    String filename = tsident.getInputName();
    HecDSSFileAccess dssFile = new HecDSSFileAccess ( filename );
    int stat = dssFile.open();
    Message.printStatus ( 2, routine, "Status from opening DSS file is " + stat );
    Vector pathnameList = new Vector();
    String aPartReq = "*";  // Requested patterns
    String bPartReq = "*";
    String cPartReq = "*";
    String dPartReq = "*";
    String ePartReq = "*";
    String fPartReq = "*";
    DateTime date1 = null, date2 = null;
    stat = dssFile.searchDSSCatalog(aPartReq, bPartReq, cPartReq, dPartReq, ePartReq, fPartReq, pathnameList);
    dssFile.close();
    // FIXME SAM 2008-09-03 Need to know the best way to close out.
    Message.printStatus ( 2, routine, "Status from searching catalog is " + stat );
    for ( int i = 0; i < pathnameList.size(); i++ ) {
        boolean readData2 = readData;   // Modified below depending on whether know how to read data
        String dssPath = (String)pathnameList.get(i);
        //Message.printStatus( 2, routine, "Pathname[" + i + "] = \"" + dssPath + "\"");
        DSSPathname dssPathName = new DSSPathname ( dssPath );
        String aPart = dssPathName.getAPart();
        String bPart = dssPathName.getBPart();
        String cPart = dssPathName.getCPart();
        String dPart = dssPathName.getDPart();
        if ( !dPart.equals("") ) {
            // Parse out - FIXME SAM 2008-09-02 Might be HECLIB code to do this
            date1 = new DateTime();
            date1.setDay(Integer.parseInt(dPart.substring(0,2)));
            date1.setMonth(TimeUtil.monthFromAbbrev(dPart.substring(2,5)));
            date1.setYear(Integer.parseInt(dPart.substring(5,9)));
        }
        String ePart = dssPathName.getEPart();
        if ( ePart.equals("") ) {
            // If no E part, assume irregular interval
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
        if ( !fPart.trim().equals("") ) {
            dotfPart = "." + fPart;
        }
        String tsidentString = aPart + "-" + bPart + ".HEC-DSS." + cPart + "." +
            ePart + dotfPart + "~HEC-DSS~" + filename;
        TS ts = TSUtil.newTimeSeries(tsidentString, true);
        ts.setIdentifier( tsidentString );
        ts.setDescription ( ts.getLocation() + " " + ts.getDataType() );
        ts.setInputName ( filename );
        // Else read the time series data.
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
            // The precision of the dates will be handled based on the time series interval.
            HecTime hecStart = new HecTime();
            HecTime hecEnd = new HecTime();
            hects.getSeriesTimeRange ( hecStart, hecEnd, 0 );
            Message.printStatus(2, routine, "Hec start = " + hecStart + " end = " + hecEnd );
            date1 = new DateTime();
            date1.setYear( hecStart.year() );
            date1.setMonth( hecStart.month() );
            date1.setDay( hecStart.day() );
            date1.setHour( hecStart.hour() );
            date1.setMinute( hecStart.minute() );
            date1.setSecond( hecStart.second() );
            ts.setDate1 ( date1 );
            ts.setDate1Original ( date1 );
            date2 = new DateTime();
            date2.setYear( hecStart.year() );
            date2.setMonth( hecStart.month() );
            date2.setDay( hecStart.day() );
            date2.setHour( hecStart.hour() );
            date2.setMinute( hecStart.minute() );
            date2.setSecond( hecStart.second() );
            ts.setDate2 ( date2 );
            ts.setDate2Original ( date2 );
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
                    "Can only handle minute and day HEC-DSS data - not reading data for \"" +
                    dssPath + "\"" );
                readData2 = false;
            }
            if ( readData2 ) {
                // Read the data records.  Increment HEC and RTi date/time objects parallel
                // to each other
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
        // Add the time series
        tslist.add ( ts );
    }
    return tslist;
}

/**
 * Taken from wcds.util.DssToPostListTranslator
 * @param unitsCont
 * @param typeCont
 * @param dataManager
 * @param dssPath
 */
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

/**
 * Taken from wcds.util.DssToPostListTranslator
 * @param unitsCont
 * @param typeCont
 * @param dataManager
 * @param dssPath
 */
public static HecTimeSeries getTimeSeries(HecDataManager dataManager, String dssPath)
{
    /* HEC logging
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
    */
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

}