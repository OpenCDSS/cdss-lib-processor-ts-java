package rti.tscommandprocessor.commands.hecdss;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import hec.dssgui.CondensedReference;
import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDSSDataAttributes;
import hec.heclib.dss.HecDSSFileAccess;
import hec.heclib.dss.HecDSSFileDataManager;
import hec.heclib.dss.HecDSSUtilities;
import hec.heclib.dss.HecDataManager;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.util.Heclib;
import hec.heclib.util.HecTime;
//import hec.heclib.util.stringContainer;
import hec.io.TimeSeriesContainer;

import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSIdent;
import RTi.TS.TSIterator;
import RTi.TS.TSUtil;

import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

/**
API to read/write HEC-DSS time series files by interfacing the RTi time series packages with the HEC API.

TODO SAM 2009-01-08 It is likely that some performance optimization may still need to occur.
TODO SAM 2009-01-08 Comments are included below to indicate issues to resolve and to help Bill Charley answer
questions.

TODO QUESTION FOR BILL CHARLEY - are the path strings case-sensitive?  In other words, if someone accidentally
put in a mixed-case string, will it match an upper-case string in the HEC-DSS file?
*/
public class HecDssAPI
{

/**
TODO QUESTION FOR BILL CHARLEY - we really don't want a special install where you have copied code into heclib.
This will cause you to do more work and will possibly result in things falling through the cracks in future
releases.  Instead, comments are included below to identify where the API could be improved.  SAM added hec.jar,
rma.jar (and correspondingly rmaUtil.dll) during various trials of code but these may not all be needed and
more time will be spent later evaluating dependencies.

TODO SAM 2009-01-08 Need to determine the minimum of the indicated files needed to access to HEC-DSS files

Load the javaHeclib.dll library in a static block so it gets done once.  These DLLs are used
via JNI to run native code (C? and FORTRAN) that reads the DSS files.  The following jars and DLLs need to be
in the classpath for the test environment and installed software:
<ol>
<li> hec.jar - contains many newer utility classes/methods (to support VUE?) (could be optional?)</li>
<li> heclib.jar - the low-level API to the HEC-DSS files, which call native methods (required)</li>
<li> javaHeclib.dll - the DLL that patches the Java code to native code (required)</li>
<li> MSVCRTD.DLL - the Microsoft Visual Studio DLL that contains FORTRAN debug routines (required).  Normally on a
computer only the optimized/release version of the DLL is distributed.  Although it is not ideal
to distribute OS level DLLs, this copy of the file is isolated and is probably not going to be found by
anything else on the system.</li>
<li> rma.jar - apparently some low-level IO code (optional, but required when hec.jar is used?)<li>
<li> rmaUtil.dll - DLL corresponding to rma.jar (optional, but required with rma.jar)</li>
</ol>
*/
static 
{   String routine = "HecDssAPI.static";
    
    boolean loadUsingJavaLibraryPath = true; // Don't use full paths to libraries - rely on runtime load path
    String dll = "";
    boolean loadRmaUtil = true; // Quick way to turn on/off rmaUtil.dll load, for testing dependencies
    // TODO SAM 2009-04-15 This may not be needed - load problems were probably due to changing the "start in" folder
    // for the app on Windows - keep for now.
    int maxTries = 5; // Number of times to try loading a library - have seen some exceptions on servers due to locking?
    int sleepMilliSeconds = 10;
    
    // Do each library separately to be able to better handle errors, but loop to avoid redundant code
    String [] libnames = { "javaHeclib", "rmaUtil" };
    UnsatisfiedLinkError [] libException = new UnsatisfiedLinkError[libnames.length]; // Default to null for each
    for ( int ilib = 0; ilib < libnames.length; ilib++ ) {
        // If not loading the rmaUtil, skip the second iteration
        if ( (ilib == 1) && !loadRmaUtil ) {
            continue;
        }
        for ( int i = 0; i < maxTries; i++ ) {
            try {
                // This relies on the java.library.path to locate the javaHeclib.dll,
                // which may be problematic since that configuration is outside the control
                // of this software and may result in a version issue.
                if ( loadUsingJavaLibraryPath ) {
                    dll = libnames[ilib];
                    System.loadLibrary(dll);
                    // If successful, make sure the exception object is null and break out of the retries
                    libException[ilib] = null;
                    break;
                }
                else {
                    // Instead, load the file explicitly knowing the application home and
                    // assuming that the file exists in the bin folder.  This is safer in that a specific DLL version
                    // can be loaded.  However, it may result in a duplicate load because the HEC software itself may try
                    // to load the DLL using the above method, which does not look for the filename in the path,
                    // but matches the requested dll by name, which won't match the path below, even though it is
                    // already loaded.  Either approach may be OK as long as the application install controls the
                    // Java run-time environment.
                    dll = IOUtil.getApplicationHomeDir() + "/bin/" + libnames[ilib] + ".dll";
                    Message.printStatus(2, routine, "Attempting to load library using System.load(" + dll + ")." );
                    System.load( dll );
                    Message.printStatus(2, routine, "Successfully loaded library \"" + dll + "\"" );
                }
                // Sleep for the next try...
                TimeUtil.sleep(sleepMilliSeconds);
            }
            // Exceptions should only be thrown if the test environment or build process is incorrect and should be
            // corrected on the developer side - users should never see an issue if the build process is correct.
            catch ( UnsatisfiedLinkError e2 ) {
                libException[ilib] = e2;
                if ( loadUsingJavaLibraryPath ) {
                    Message.printWarning ( 2, routine, "[Try " + (i + 1) +
                        "] Unable to load " + dll + " using System.loadLibrary(" +
                        dll + ") and java.library.path \"" + System.getProperty("java.library.path") + "\" (" + e2 + ")." );
                }
                else {
                    Message.printWarning ( 2, routine, "[Try " + (i + 1) +
                        "] Unable to load " + dll + " using System.load(" + dll + ") (" + e2 + ")." );
                }
                Message.printWarning ( 3, routine, e2 );
            }
        }
    }
    // Now check for exceptions that still remain, indicating that the load could not occur even after retries.
    // Individual failures will have been logged above.
    StringBuffer b = new StringBuffer();
    for ( int ilib = 0; ilib < libnames.length; ilib++ ) {
        if ( libException[ilib] != null ) {
            b.append ( "Unable to load library \"" + libnames[ilib] + "\" after " + maxTries + " tries.");
        }
    }
    if ( b.length() > 0 ) {
        b.append ( "HEC-DSS features may not be functional." );
        Message.printWarning ( 2, routine, b.toString() );
        // Rethrow the error so it can be indicated as a command error
        throw new RuntimeException ( b.toString() );
    }
   
    // Set the message level to the maximum to track down issues.
    
    //HecDataManager.setMessageLevel ( 15 );
}

/**
Adjust an end date from a D part (period) to the end of the month.  This is because the end D part date shows
the first of the month.
@param ts time series associated with date, to retrieve the data interval.
@param blockStartDateTime a DateTime that was determined from the end date/time in the D part,
with no previous adjustments.
@return the adjusted DateTime that corresponds to the end of the month
*/
private static DateTime adjustEndBlockDateToLastDateTime ( TS ts, DateTime blockStartDateTime )
{
    int intervalBase = ts.getDataIntervalBase();
    int intervalMult = ts.getDataIntervalMult();
    // Create a new DateTime from the block start
    DateTime adjusted = new DateTime ( blockStartDateTime );
    // Adjust DateTime to the end of the block, based on the time series interval.
    if ( intervalBase == TimeInterval.YEAR ) {
        // Annual blocks are century.  Add 100 to make sure roundoff results in the end being offset by
        // a century and then subtract a year to make sure the year is at the end of the starting century
        adjusted.setYear(((blockStartDateTime.getYear() + 100)/100)*100 - 1);
    }
    else if ( intervalBase == TimeInterval.MONTH ) {
        // Blocks are decades.  Add 10 to make sure roundoff results in the end being offset by a decade
        // and then subtract a year to make sure the year is at the end of the starting decade.
        adjusted.setYear ( ((blockStartDateTime.getYear() + 10)/10)*10 - 1);
        adjusted.setMonth ( 12 );
    }
    else if ( intervalBase == TimeInterval.DAY ) {
        // Blocks are years - set the month and day to the end of the year
        adjusted.setMonth ( 12 );
        adjusted.setDay ( 31 );
    }
    else if ( (intervalBase == TimeInterval.HOUR) ||
        ((intervalBase == TimeInterval.MINUTE) && (intervalMult == 15) || (intervalMult == 20) ||
        (intervalMult == 30))) {
        // Blocks are month - set the day and hour to the end of the month
        adjusted.setDay ( TimeUtil.numDaysInMonth(blockStartDateTime) );
        // FIXME SAM 2009-01-14 Below, ...may need to roll over to next day, hour zero
        if (intervalBase == TimeInterval.HOUR) {
            // HEC-DSS does not allow 24 hour so the following will give the last value in the day
            adjusted.setHour ( 24 - intervalMult );
        }
        else {
            // Set the hour to 23 since minute interval will go until the last hour of the day
            adjusted.setHour ( 23 );
            // HEC-DSS does not allow 60 minute so the following will give the last value in the day
            adjusted.setMinute ( 60 - intervalMult );
        }
    }
    else if ( intervalBase == TimeInterval.MINUTE ) {
        // Blocks are day - set the hour to 23 since minute interval will go until the last hour of the day
        adjusted.setHour ( 23 );
        // HEC-DSS does not allow 60 minute so the following will give the last value in the day
        adjusted.setMinute ( 60 - intervalMult );
    }
    return adjusted;
}

/**
Adjust a requested date/time precision to that needed by HEC-DSS (to minute).
*/
private static DateTime adjustRequestedDateTimePrecision ( DateTime dt, boolean start )
{
    DateTime adjusted = new DateTime();
    if ( dt.getPrecision() == DateTime.PRECISION_YEAR ) {
        adjusted.setYear(dt.getYear());
        if ( start ) {
            adjusted.setMonth(1);
            adjusted.setDay(1);
            adjusted.setHour(0);
            adjusted.setMinute(0);
        }
        else {
            adjusted.setMonth(12);
            adjusted.setDay(31);
            adjusted.setHour(0);
            adjusted.addHour(24); // OK to roll over
            adjusted.setMinute(0);
        }
    }
    else if ( dt.getPrecision() == DateTime.PRECISION_MONTH ) {
        adjusted.setYear(dt.getYear());
        adjusted.setMonth(dt.getMonth());
        if ( start ) {
            adjusted.setDay(1);
            adjusted.setHour(0);
            adjusted.setMinute(0);
        }
        else {
            adjusted.setDay(TimeUtil.numDaysInMonth(dt));
            adjusted.setHour(0);
            adjusted.addHour(24); // OK to roll over
            adjusted.setMinute(0);
        }
    }
    else if ( dt.getPrecision() == DateTime.PRECISION_DAY ) {
        adjusted.setYear(dt.getYear());
        adjusted.setMonth(dt.getMonth());
        adjusted.setDay(dt.getDay());
        if ( start ) {
            adjusted.setHour(0);
            adjusted.setMinute(0);
        }
        else {
            adjusted.setHour(0);
            adjusted.addHour(24); // OK to roll over
            adjusted.setMinute(0);
        }
    }
    else if ( dt.getPrecision() == DateTime.PRECISION_HOUR ) {
        adjusted.setYear(dt.getYear());
        adjusted.setMonth(dt.getMonth());
        adjusted.setDay(dt.getDay());
        adjusted.setHour(dt.getHour());
        adjusted.setMinute(0);
    }
    return adjusted;
}

/**
Run Bill Charley's example write code.  Keep this around for awhile for a reference.
*/
private static void billsWriteCode ()
{
    HecTimeSeries.setMessageLevel(9);
    HecTimeSeries.setDefaultDSSFileName("C:/Temp_X/mydb.dss");
    
    TimeSeriesContainer tsc = new TimeSeriesContainer();
    HecTime startTime = new HecTime("12Jan2009", "2400");
    HecTime time = new HecTime();
    tsc.interval = 1440;
    tsc.numberValues = 20;
    //  Set values and times
    tsc.values = new double[tsc.numberValues];
    tsc.times = new int[tsc.numberValues];
    for (int i=0; i<tsc.times.length; i++) {
              //  Make up data
        tsc.values[i] = Math.pow((double)i, 2.0);
        time.set(startTime);
        time.increment(i, tsc.interval);
        tsc.times[i] = time.value();
    }
    tsc.startTime = tsc.times[0];
    tsc.endTime = tsc.times[tsc.times.length-1];
    
    //  Set pathname
    DSSPathname path = new DSSPathname();
    path.setAPart("River");
    path.setBPart("Loc");
    path.setCPart("Flow");
    String ePart =
HecTimeSeries.getEPartFromInterval(tsc.interval);
    path.setEPart(ePart);
    path.setFPart("Ver");
    
    tsc.fullName = path.pathname();
    tsc.units = "CFS";
    tsc.type = "PER_AVER";
    
    HecTimeSeries hecTimeSeries = new HecTimeSeries();
    hecTimeSeries.write(tsc);
    hecTimeSeries.done();

    // Force close at end of execution
    HecTimeSeries.closeAllFiles();
    System.out.println("done!");
}

/**
Close the data files that may be open.
*/
public static void closeAllFiles ()
{
    HecTimeSeries.closeAllFiles();
    // As per Bill Charley, this will print the status of all files...
    HecDSSFileDataManager.status();
}

/**
TODO SAM 2008-01-08 This method may be unnecessary if the condensed catalog code in the HEC library can be
figured out for all requirements.

Create a condensed pathname list.  Records that are duplicates except for D parts are removed from the list.
This is necessary because the normal catalog method returns path names that show data blocks (e.g., one block
per year), resulting in multiple pathnames for the same time series.
Condensing works for time series records and paired data pass through.
@param pathnameList A list of pathnames (as String).  This will be modified and returned.
@return a condensed pathname list, as a new list.
*/
private static List createCondensedCatalog ( List pathnameList )
{   String routine = "HecDssAPI.createCondensedCatalog";
    // Sort the pathnames to make sure that duplicates are grouped - seems like this is not needed
    // because the catalog is always sorted.  For now don't sort to increase performance
    
    // Loop through the pathnames.  Compare the parts with the previous item.  If all parts are the same
    // except for the D part, remove the item and change the D part to be inclusive
    
    int size = pathnameList.size();
    List condensedPathnameList = new Vector();
    String aPart, aPart2 = null; // Basin
    String bPart, bPart2 = null; // Location
    String cPart, cPart2 = null; // Data type
    String dPart, dPart2 = null; // Period
    String ePart, ePart2 = null; // Interval
    String fPart, fPart2 = null; // Scenario
    DSSPathname dssPathName, dssPathName2;
    String pathname; // One record pathname
    String pathname2;
    String condensedPathname; // With D part that has two dates
    int numAdditionalRecords = 0; // Number of records to merge with the first
    int numAdditionalRecordsTotal = 0; // Number of total addtional/merged records
    for ( int i = 0; i < size; i++ ) {
        pathname = (String)pathnameList.get(i);
        dssPathName = new DSSPathname ( pathname );
        aPart = dssPathName.getAPart();
        bPart = dssPathName.getBPart();
        cPart = dssPathName.getCPart();
        ePart = dssPathName.getEPart();
        fPart = dssPathName.getFPart();
        // Look ahead to see if any duplicates exist.  If on the last record, numAdditionalRecords
        // will be zero and the current record will be added below.
        numAdditionalRecords = 0;
        for ( int j = (i + 1); j < size; j++ ) {
            pathname2 = (String)pathnameList.get(j);
            dssPathName2 = new DSSPathname ( pathname2 );
            aPart2 = dssPathName2.getAPart();
            bPart2 = dssPathName2.getBPart();
            cPart2 = dssPathName2.getCPart();
            ePart2 = dssPathName2.getEPart();
            fPart2 = dssPathName2.getFPart();
            // TODO SAM 2009-04-16 Does this always work correctly for paired data?
            if ( !aPart.equals(aPart2) || !bPart.equals(bPart2) || !cPart.equals(cPart2) ||
                !ePart.equals(ePart2) || !fPart.equals(fPart2)) {
                // Not a continuation of the same time series
                break;
            }
            else {
                // A continuation of the record
                ++numAdditionalRecords;
                ++numAdditionalRecordsTotal;
                // Save the D part so by the end will have the maximum
                dPart2 = dssPathName2.getDPart();
            }
        }
        // Now process the D parts in the first and last record to give a new pathname list
        if ( numAdditionalRecords == 0 ) {
            // Just add the original record to the condensed list
            condensedPathnameList.add(pathname);
        }
        else {
            // Modify the D part of the original record and add the merged record
            dPart = dssPathName.getDPart();
            dssPathName.setDPart( dPart + " - " + dPart2 );
            condensedPathname = dssPathName.toString();
            //Message.printStatus( 2, routine, "Condensed pathname = \"" + condensedPathname + "\"" );
            condensedPathnameList.add(condensedPathname);
            // Increment the counter to start a new set of records
            i += numAdditionalRecords;
        }
    }
    for ( int i = 0; i < condensedPathnameList.size(); i++ ) {
        Message.printStatus( 2, routine, "Condensed pathname " + i + " = \"" + condensedPathnameList.get(i) + "\"" );
    }
    Message.printStatus( 2, routine, "Removed " + numAdditionalRecordsTotal + " path records during condensing." );
    return condensedPathnameList;
}

/**
Create the time window string to limit the time series read.  The format will be compatible with
the D part if includeHour=false and compatible with the time window string in any case.
@param startDateTime the start date/time for the read.
@param endDateTime the end date/time for the read, null if not included.
@param ts the RTi time series being processed, used to determine the data interval.
@param includeHour indicates whether the hour should be included
*/
private static String createTimeWindowString ( DateTime startDateTime, DateTime endDateTime, TS ts, boolean includeHour )
{
    // Format of the period to read...
    //rts.setTimeWindow("04Sep1996 1200 05Sep1996 1200");
    String startString = StringUtil.formatString(startDateTime.getDay(),"%02d") +
        TimeUtil.monthAbbreviation(startDateTime.getMonth()).toUpperCase() +
        StringUtil.formatString(startDateTime.getYear(), "%04d");
    String endString = "";
    if ( endDateTime != null ) {
        endString = StringUtil.formatString(endDateTime.getDay(),"%02d") +
        TimeUtil.monthAbbreviation(endDateTime.getMonth()).toUpperCase() +
        StringUtil.formatString(endDateTime.getYear(), "%04d");
    }
    if ( includeHour ) {
        if ( (ts.getDataIntervalBase() == TimeInterval.HOUR) ||
            (ts.getDataIntervalBase() == TimeInterval.MINUTE) ) {
            // Add the hour and minute for higher precision interval
            startString = startString + " " + StringUtil.formatString(startDateTime.getHour(),"%02d");
            endString = endString + " " + StringUtil.formatString(endDateTime.getHour(),"%02d");
            if ( ts.getDataIntervalBase() == TimeInterval.HOUR ) {
                startString = startString + StringUtil.formatString(startDateTime.getMinute(),"%02d");
                endString = endString + StringUtil.formatString(endDateTime.getMinute(),"%02d");
            }
            else {
                startString = startString + "00";
                endString = endString + "00";
            }
        }
        else {
            // Ending at 2400 seems to be the standard
            startString = startString + " 0000";
            endString = endString + " 2400";
        }
    }
    // Set the time range using the information from the catalog.
    String timeWindow = startString + " " + endString;
    return timeWindow;
}

/**
Convert an RTi DateTime to HecTime instance.  The precision of the DateTime is used to set the date values.
For example, for daily and monthly values the hour is set to 24.
@param dt RTi DateTime instance.
@param ht Existing HecTime instance to reuse (for optimization).  If null, create a new
instance to return.
@return HecTime instance.
*/
private static HecTime dateTimeToHecTime ( DateTime dt, HecTime ht )
{
    if ( ht == null ) {
        ht = new HecTime();
    }

    // Transfer the values.
    
    // TODO QUESTION FOR BILL CHARLEY - why doesn't HecTime have setYear(), etc.?  If the precision of
    // the date/time is year, why force the other values to set?  The RTi DateTime class has a precision data
    // member that seems to be similar to the HecTime granularity so I'm trying to keep them consistent.
    // Does the HEC code now how to deal with granularity from the interval on the time series?  See the commented
    // lines below.  Doesn't HecTime allow setting the "granularity" to "year", "month", or even "period"?
    int month = dt.getMonth();
    int day = dt.getDay();
    int hour = dt.getHour();
    int minute = dt.getMinute();
    if ( dt.getPrecision() == DateTime.PRECISION_YEAR ) {
        // Value is recorded for last day of the year
        month = 12;
        day = 31;
    }
    if ( dt.getPrecision() == DateTime.PRECISION_MONTH ) {
        // Value is recorded for the last day of the month
        day = TimeUtil.numDaysInMonth(dt);
    }
    if ( (dt.getPrecision() == DateTime.PRECISION_YEAR) ||
        (dt.getPrecision() == DateTime.PRECISION_MONTH) ||
        (dt.getPrecision() == DateTime.PRECISION_DAY) ) {
        // Value is recorded at the end of the day
        hour = 24;
        minute = 0;
    }
    // TODO SAM 2009-03-30 Does the hour depend on the time series "Type".  For example does instantaneous vs. mean
    // change the hour that a daily value is recorded?
    ht.setYearMonthDay(dt.getYear(), month, day, (hour*60 + minute) );
    //int precision = dt.getPrecision();
    //if ( precision == DateTime.PRECISION_YEAR ) {
    //    ht.setTimeGranularity(ht.??);
    //}
    
    return ht;
}

/**
Get a DSSPathname from a time series, assuming that the TSID follows the default convention:
A:B.HEC-DSS.Cpart.Epart.Fpart.
@param ts Time series to examine.
@return a new DSS pathname object with parts set according to the above.
*/
public static DSSPathname getPathnamePartsFromDefaultTSIdent( TS ts )
{
    TSIdent tsident = ts.getIdentifier();
    DSSPathname p = new DSSPathname();
    String loc = tsident.getLocation();
    int pos = loc.indexOf(":");
    if ( pos < 0 ) {
        // No basin
        p.setBPart(loc);
    }
    else {
        // Have basin and site
        p.setAPart(loc.substring(0,pos));
        p.setBPart(loc.substring(pos + 1));
    }
    p.setCPart(tsident.getType());
    p.setEPart(tsident.getInterval());
    p.setFPart(tsident.getScenario());
    return p;
}

/**
Get the list of A parts that are available in the file.
@return the list of unique A parts from the file, using a condensed catalog.
*/
public static List getUniqueAPartList ( File hecDssFile, String bPartReq, String cPartReq, String ePartReq, String fPartReq )
{
    // Internally use the time series code because it handles all of the wildcarding
    List tsList = null;
    List aPartList = new Vector();
    String bPart = "*";
    String cPart = "*";
    String ePart = "*";
    String fPart = "*";
    if ( (bPartReq != null) && !bPartReq.equals("") ) {
        bPart = bPartReq;
    }
    if ( (cPartReq != null) && !cPartReq.equals("") ) {
        cPart = cPartReq;
    }
    if ( (ePartReq != null) && !ePartReq.equals("") ) {
        ePart = ePartReq;
    }
    if ( (fPartReq != null) && !fPartReq.equals("") ) {
        fPart = fPartReq;
    }
    try {
        String tsidPattern = "*-" + bPart + "*." + cPart + "." + ePart + "." + fPart;
        tsList = readTimeSeriesList ( hecDssFile, tsidPattern, null, null, null, false );
        int tsListSize = tsList.size();
        TS ts = null;
        String aPart = null;
        int aPartListSize = 0;
        boolean found = false;
        for ( int i = 0; i < tsListSize; i++ ) {
            // Search the list to see if the A part is already in the list.  If not, add it.
            ts = (TS)tsList.get(i);
            aPart = ts.getIdentifier().getMainLocation();
            aPartListSize = aPartList.size();
            found = false;
            for ( int j = 0; j < aPartListSize; j++ ) {
                if ( aPartList.get(j).equals(aPart)) {
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                aPartList.add ( aPart );
            }
        }
    }
    catch ( Exception e ) {
        // Log it and return an empty list
        String routine = "HecDssAPI.getUniqueAPartList";
        Message.printWarning(3, routine, e);
    }
    return aPartList;
}
     
/**
TODO SAM 2008-01-08 Need to enable this capability to support GUI filters.
Read the available data types from the DSS file.  Unless there is an easy way to do this, will probably implement
by calling readTimeSeriesList(readData=false) and find unique data types from the pathnames.
*/
public static List getDataTypes ( String filename )
{   // For now return a blank list since the GUI functionality is not enabled.
	List dataTypes = new Vector();
    return dataTypes;
}

/**
Determine whether the time series interval is supported by the HEC-DSS API.
Some intervals (e.g., irregular) are not supported because the software has not
been written.  Others are not supported because the interval is not supported by
HEC-DSS (e.g., 24-hour).
@return false if the interval is not supported.
*/
public static boolean isTimeSeriesIntervalSupportedByAPI ( TS ts )
{
    boolean supported = false;
    int intervalBase = ts.getDataIntervalBase();
    int intervalMult = ts.getDataIntervalMult();
    if ( (intervalBase == TimeInterval.MINUTE) && (
        (intervalMult == 1) || (intervalMult == 2) || (intervalMult == 3) || (intervalMult == 4)
        || (intervalMult == 5) || (intervalMult == 10) || (intervalMult == 15) || (intervalMult == 20)
        || (intervalMult == 30)) ) {
        supported = true;
    }
    else if ( (intervalBase == TimeInterval.HOUR) && ( (intervalMult == 1) || (intervalMult == 2)
        || (intervalMult == 3) || (intervalMult == 4) || (intervalMult == 6) || (intervalMult == 8)
        || (intervalMult == 12)) ) {
        supported = true;
    }
    else if ( (intervalBase == TimeInterval.DAY) && ( (intervalMult == 1) ) ) {
        supported = true;
    }
    else if ( intervalBase == TimeInterval.WEEK ) {
        // Not currently handled by RTi code
        supported = false;
    }
    else if ( (intervalBase == TimeInterval.MONTH) && ( (intervalMult == 1) ) ) {
        supported = true;
    }
    else if ( (intervalBase == TimeInterval.YEAR) && ( (intervalMult == 1) ) ) {
        supported = true;
    }
    return supported;
}

/**
Read a single time series from a HEC-DSS file.  This method is typically called when a specific time series is
read for processing.
@param file HEC-DSS file (full path).
@param tsident Time series identifier string of the form where only the parts before "~" are required since
this API equates to HEC-DSS input type and the filename is in the "file" parameter:
<pre>
RTi convention:
Location.DataSource.DataType.Interval.Scenario~HEC-DSS~Filename

HEC parts inserted:
A-B.HEC-DSS.C.E.F
</pre>
The main issue is that this convention utilizes periods and dashes and these characters in the original
data may need substitution for internal handling.  The input name should be the name of the HEC-DSS file to absolute or
relative precision, consistent with how the identifier should appear for data access.
@param readStartReq the DateTime to start reading data, with precision at least as fine as the time series.
If null, read all available data.
@param readEndReq the DateTime to end reading data, with precision at least as fine as the time series.
If null, read all available data.
@param unitsReq requested units (under development, since HEC units may not be consistent with calling software).
@param readData if true, read the time series values.  If false, only read the metadata.
@return a single time series matching the requesting time series identifier.
@exception RuntimeException if there is an error reading the time series.
*/
public static TS readTimeSeries ( File file, String tsident, DateTime readStartReq, DateTime readEndReq,
        String unitsReq, boolean readData )
throws Exception
{
    // Expecting that a single time series will be matched because the time series identifier should not
    // contain wildcards.
	List tslist = readTimeSeriesList ( file, tsident, readStartReq, readEndReq, unitsReq, readData );
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

// TODO QUESTION FOR BILL CHARLEY - using the following code on the sample.dss file shipped with DSS-VUE, there
// appear to be 32 pathnames in the file, 17 after condensing.  DSS-VUE only shows 3.  Is there an attribute on the
// time series that indicates that it is deleted (but still listed in the catalog) or something like that which
// would cause the pathname to show up in my code but not DSS-VUE?

/**
Read a list of one or more time series from a HEC-DSS file.  This method is typically called when the entire file
or a subset filtered by the parts is read.  For example, all time series in an area or of a data type may be
processed for data filling or quality control.  Another use is to call this method from a GUI with readData=false
in order to display the time series in the file without actually reading all the data - this allows time selections
in GUIs to be fast.
@param file HEC-DSS file (full path).
@param tsident Time series identifier string of the form where only the parts before "~" are required since
this API equates to HEC-DSS input type and the filename is in the "file" parameter:
<pre>
RTi convention:
Location.DataSource.DataType.Interval.Scenario~HEC-DSS~Filename

HEC parts inserted:
A-B.HEC-DSS.C.E.F
</pre>
The main issue is that this convention utilizes periods and dashes and these characters in the original
data may need substitution for internal handling.  The input name should be the name of the HEC-DSS file to absolute or
relative precision, consistent with how the identifier should appear for data access.  The "*" character can be used
in the time series identifier parts to filter the query.
@param readStartReq the DateTime to start reading data, with precision at least as fine as the time series.
If null, read all available data.
@param readEndReq the DateTime to end reading data, with precision at least as fine as the time series.
If null, read all available data.
@param unitsReq requested units (under development, since HEC units may not be consistent with calling software).
@param readData if true, read the time series values.  If false, only read the metadata.
@return a list of time series that were read.
@throws Exception if there is an error reading the time series
*/
public static List readTimeSeriesList ( File file, String tsidentPattern,
        DateTime readStartReq, DateTime readEndReq, String unitsReq, boolean readData )
throws Exception
{   String routine = "HecDssAPI.readTimeSeriesList";
    TSIdent tsident = new TSIdent ( tsidentPattern );
    String dssFilename = file.getCanonicalPath();
    HecDSSFileAccess dssFile = new HecDSSFileAccess ( dssFilename );
    // FIXME SAM 2009-01-08 Need to implement a cache so that the file is not repeatedly opened
    // Do this similar to other binary file databases like StateMod and StateCU
    int stat = dssFile.open();
    Message.printStatus ( 2, routine, "Status from opening DSS file \"" + dssFilename + "\" is " + stat );
    // Use the following code to allow wildcards in matching 1+ time series
    // Normally wildcards will only be used when doing a bulk read and a single time series
    // will be matched for a specific time series identifier.
    Vector pathnameList = new Vector();
    String location = tsident.getLocation();
    int posColon = location.indexOf(':');
    String aPartReq = "";
    String bPartReq = "";
    if ( posColon >= 0 ) {
        // Get separate A parts
        aPartReq = location.substring(0,posColon);
        bPartReq = location.substring(posColon + 1);
    }
    else {
        aPartReq = "";
        bPartReq = location;
    }
    String cPartReq = tsident.getType();
    String dPartReq = "*"; // Do not limit based on start - get all available data
    String ePartReq = tsident.getInterval();
    String fPartReq = tsident.getScenario();
    Message.printStatus ( 2, routine, "Requesting matching pathnames for " +
            "A=\"" + aPartReq + "\" " +
            "B=\"" + bPartReq + "\" " +
            "C=\"" + cPartReq + "\" " +
            "D=\"" + dPartReq + "\" " +
            "E=\"" + ePartReq + "\" " +
            "F=\"" + fPartReq + "\"" );
    List condensedPathnameList = null;
    // FIXME SAM 2009-01-08 Cannot get the following HEC code to work because it does not seem to allow filtering
    // on the pathname parts.  Therefore use the RTi code below but use boolean to allow HEC code to be turned on.

    // TODO QUESTION FOR BILL CHARLEY - I can't get this to work - see questions below
    boolean useHECCondensedPathnameListCode = false;
    if ( useHECCondensedPathnameListCode ) {
        // Get condensed catalog
        HecDSSUtilities u = new HecDSSUtilities();
        // TODO QUESTION FOR BILL CHARLEY - why not permanently put this method in the heclib jar since it is useful
        // with the low-level API?
        // TODO SAM 2009-01-07 Is this global filename a performance hit when jumping around files?
        HecDSSUtilities.setDefaultDSSFileName ( dssFilename );
        // TODO QUESTION FOR BILL CHARLEY - how does this work with the ability to filter the parts?
        // We need a method like the following that includes searchDSSCatalog() functionality with wildcards
        condensedPathnameList = u.getCondensedCatalog();
    }
    else {
        // Use the code written by SAM at RTi, which condenses catalog records and allows wildcards on parts
        stat = dssFile.searchDSSCatalog(aPartReq, bPartReq, cPartReq, dPartReq, ePartReq, fPartReq, pathnameList);
        Message.printStatus ( 2, routine, "Status from searching catalog is " + stat +
            ".  Number of matching path names before condensing is " + pathnameList.size() );
        // Condense the pathnames because the D part might be redundant
        condensedPathnameList = createCondensedCatalog(pathnameList);
        Message.printStatus ( 2, routine, "Number of matching path names after condensing is " +
            condensedPathnameList.size() );
    }
    return readTimeSeriesListUsingPathnameList ( dssFilename, condensedPathnameList, readStartReq, readEndReq,
        unitsReq, readData );
}

/**
Read a list of one or more time series from a HEC-DSS file.  This method is typically called when a specific
pathname is given.  It is envisioned that * could be included in the pathname but currently the pathname must be
a literal string, as if taken from the condensed catalog.
@param file HEC-DSS file (full path).
@param pathname HEC-DSS condensed pathname for time series to read (D-part must contain period, not just one date).
@param readStartReq the DateTime to start reading data, with precision at least as fine as the time series.
If null, read all available data.
@param readEndReq the DateTime to end reading data, with precision at least as fine as the time series.
If null, read all available data.
@param unitsReq requested units (under development, since HEC units may not be consistent with calling software).
@param readData if true, read the time series values.  If false, only read the metadata.
@return a list of time series that were read.
@throws Exception if there is an error reading the time series
*/
public static List readTimeSeriesListUsingPathname ( File file, String pathname,
        DateTime readStartReq, DateTime readEndReq, String unitsReq, boolean readData )
throws Exception
{   String routine = "HecDssAPI.readTimeSeriesListUsingPathname";
    String dssFilename = file.getCanonicalPath();
    HecDSSFileAccess dssFile = new HecDSSFileAccess ( dssFilename );
    // FIXME SAM 2009-01-08 Need to implement a cache so that the file is not repeatedly opened
    // Do this similar to other binary file databases like StateMod and StateCU
    int stat = dssFile.open();
    Message.printStatus ( 2, routine, "Status from opening DSS file \"" + dssFilename + "\" is " + stat );
    Vector pathnameList = new Vector();
    pathnameList.add ( pathname );
    return readTimeSeriesListUsingPathnameList ( dssFilename, pathnameList, readStartReq, readEndReq, unitsReq, readData );
}

/**
Internal method to help with reading time series, called from multiple methods.
*/
private static List readTimeSeriesListUsingPathnameList ( String dssFilename, List condensedPathnameList,
    DateTime readStartReq, DateTime readEndReq, String unitsReq, boolean readData )
throws Exception
{   String routine = "HecDSSAPI.readTimeSeriesListUsingPathnameList";
    List tslist = new Vector();
    DateTime date1FromDPart = null;
    DateTime date2FromDPart = null;
    // Create a file manager so that the utility code for sure knows the filename
    //System.out.println("Creating HecDataManager");
    HecDataManager hmgr = new HecDataManager(dssFilename);
    //System.out.println("Created HecDataManager");
    // Loop through the pathnames and read each time series
    // Get the period from the D part in case data are not being read
    for ( int i = 0; i < condensedPathnameList.size(); i++ ) {
        String alias = null;  // Set to TSID if TSID contains periods.
        boolean readData2 = readData;   // Modified below depending on whether know how to read data
        //Message.printStatus( 2, routine, "Pathname[" + i + "] = \"" + dssPath + "\"");
        Object condensedPathname = condensedPathnameList.get(i);
        DSSPathname dssPathName = null;
        String aPart = null;
        String bPart = null;
        String cPart = null;
        String dPart = null;
        String dPart1 = null, dPart2 = null;
        String ePart = null;
        String fPart = null;
        // The following use of instanceof allows the HEC and RTi code from above to be used.
        if ( condensedPathname instanceof String ) {
            // From RTi code above.
            String p = (String)condensedPathname;
            dssPathName = new DSSPathname ( p );
            Message.printStatus ( 2, routine, "Reading using condensed pathname \"" + dssPathName + "\"." );
            aPart = dssPathName.getAPart();
            bPart = dssPathName.getBPart();
            cPart = dssPathName.getCPart();
            dPart = dssPathName.getDPart();
            ePart = dssPathName.getEPart();
            fPart = dssPathName.getFPart();
        }
        else if ( condensedPathname instanceof CondensedReference ) {
            // TODO QUESTION FOR BILL CHARLEY I think that I have this figured out.  However, the inability to
            // filter on the parts makes it so I can't use this right now without doing the filtering myself.  Is
            // there a way to create a CondensedReference that filters?  There must be since VUE seems to do it.
            CondensedReference cr = (CondensedReference)condensedPathname;
            DSSPathname cp1 = new DSSPathname(cr.getFirstPathname()); // For first D part
            DSSPathname cp2 = new DSSPathname(cr.getLastPathname()); // For last D part
            aPart = cp1.aPart();
            bPart = cp1.bPart();
            cPart = cp1.cPart();
            dPart1 = cp1.dPart();
            dPart2 = cp2.dPart();
            if ( cr.size() == 1 ) {
                dPart = dPart1;
            }
            else {
                dPart = dPart1 + " - " + dPart2;
            }
            ePart = cp1.ePart();
            fPart = cp1.fPart();
        }
        // Check here whether the pathname corresponds to a time series or not.
        // This can't be called unless the HecDSSUtilities code is told what the DSS file is
        //HecDSSUtilities hutil = new HecDSSUtilities.pathnameDataType(dssPathName.toString());
        //System.out.println("Trying to get attributes");
        String dssPathNameNotCondensed = uncondensedPathName(dssPathName).toString();
        int t = hmgr.recordType(dssPathNameNotCondensed);
        // TODO SAM 2009-04-16 Emailed Bill Charley to ask how to interpret the type and for now just skip types
        // that obviously look inappropriate
        if ( (t == HecDSSDataAttributes.PAIRED) || (t == HecDSSDataAttributes.PAIRED_DOUBLES) ||
            (t == HecDSSDataAttributes.TEXT) ) {
            Message.printStatus(2, routine, "Skipping path \"" + dssPathName + "\" because it is record type " + t +
                " (not time series).");
            continue;
        }
        else {
            if ( Message.isDebugOn ) {
                Message.printStatus(2, routine, "Path \"" + dssPathName + "\" has record type " + t + ".");
            }
        }
        // Handle the D part similarly regardless of whether using RTi or HEC code above.
        if ( !dPart.equals("") ) {
            // Parse out the D part into DateTime objects, but only to day precision since that is all that is in D.
            // Note that the D part might be one date if no condensing was necessary, or two parts if
            // condensing occurred.
            
            // FIXME SAM 2008-09-02 Might be HECLIB code to do this
            
            // TODO QUESTION FOR BILL CHARLEY - is there code to convert the D part to one or two HecTimes?
            // Then I would not need to deal with the parsing here.  What about automatically getting a date/time
            // that is for the end of the month, not the start of month that is shown in the D part.?
            // Is a full month of data always available?
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
        if ( ePart.equals("") ) {
            // If no E part, assume irregular interval for RTi time series - otherwise HEC intervals are
            // already interpreted correctly for RTi time series
            
            // FIXME SAM 2008-11-10 Evaluate whether this is a good idea - need to understand better the
            // irregular intervals used by HEC software - might need to adopt a more granular irregular
            // interval convention in RTi code than the current generic irregular interval.  For example use
            // Irr6Hour.
            ePart = "Irregular";
            Message.printStatus ( 2, routine, "Reading irregular time series and paired data are not yet supported - " +
        		"not reading data for " + "A=\"" + aPart + "\" " + "B=\"" + bPart + "\" " + "C=\"" + cPart + "\" " +
                "D=\"" + dPart + "\" " + "E=\"" + ePart + "\" " + "F=\"" + fPart + "\"" );
            continue;
        }
        Message.printStatus ( 2, routine, "DSS pathname for time series: " +
            "A=\"" + aPart + "\" " + "B=\"" + bPart + "\" " + "C=\"" + cPart + "\" " +
            "D=\"" + dPart + "\" " + "E=\"" + ePart + "\" " + "F=\"" + fPart + "\"" );
        // Create time series.
        // TODO SAM 2009-01-08 Need to evaluate how to handle use of reserved characters (periods and dashes) in
        // HEC parts since these conflict with RTi time series identifier conventions.  For now, replace the
        // periods with spaces in the identifier and utilize the time
        // series alias to retain the original identifier.
        String dotfPart = "";
        String dotfPartNoPeriod = ""; // Dot F part without a period
        if ( !fPart.trim().equals("") ) {
            dotfPart = "." + fPart;
            dotfPartNoPeriod = "." + fPart.replace('.',' ');
        }
        // Make sure that the parts do not contain periods, which will mess up the time series.  If the parts
        // do contain periods, set an alias with the first part since the alias can contain parts and replace
        // the periods with dots.  Separate the A and B parts with a colon because dashes are often used in HEC
        // and have special meaning in TSID for main-sub location.
        String tsidMain = aPart + ":" + bPart + ".HEC-DSS." + cPart + "." + ePart + dotfPart;
        String tsidMainNoPeriodsInParts = aPart.replace('.',' ') + ":" + bPart.replace('.',' ') + ".HEC-DSS." +
            cPart.replace('.',' ') + "." + ePart.replace('.',' ') + dotfPartNoPeriod;
        if ( aPart.indexOf(".") >= 0 ) {
            Message.printStatus(2 , routine, "TSID \"" + tsidMain +
                "\" has period(s) in A part.  Replace with spaces and assign full TSID to alias." );
            alias = tsidMain;
        }
        if ( bPart.indexOf(".") >= 0 ) {
            Message.printStatus(2 , routine, "TSID \"" + tsidMain +
                "\" has period(s) in B part.  Replace with spaces and assign full TSID to alias." );
            alias = tsidMain;
        }
        if ( fPart.indexOf(".") >= 0 ) {
            Message.printStatus(2 , routine, "TSID \"" + tsidMain +
                "\" has period(s) in F part.  Replace with spaces and assign full TSID to alias." );
            alias = tsidMain;
        }
        String tsidentString = tsidMainNoPeriodsInParts + "~HEC-DSS~" + dssFilename;
        // Create a new time series with appropriate internal data management for the interval
        TS ts = TSUtil.newTimeSeries(tsidentString, true);
        int tsIntervalBase = ts.getDataIntervalBase(); // Used to help with date precisions
        ts.setIdentifier( tsidentString );
        // Set the alias if the TSID had periods above
        if ( alias != null ) {
            ts.setAlias ( alias );
        }
        // Set the description to the location, space, and data type
        
        // TODO QUESTION FOR BILL CHARLEY - do DSS files have any narrative description or comments for each time
        // series other than the parts?  RTi time series have a description (typically like
        // "South Fork of ABC below XYZ") and also comments, which can be any number of strings.
        ts.setDescription ( ts.getLocation() + " " + ts.getDataType() );
        // Time series input name is the original HEC-DSS file
        ts.setInputName ( dssFilename );
        // Set the start date from the D part but might be reset below from actual data.
        // Only set the end date if it is different from the start.  This will mimic the catalog in that
        // only ranges will be shown.
        
        // FIXME SAM 2009-01-08 Need to set the precision of dates including hour.  Currently only set to day
        // based on the D part.  This will be reset below.
        ts.setDate1 ( date1FromDPart );
        if ( !date1FromDPart.equals(date2FromDPart) ) {
            ts.setDate2 ( date2FromDPart );
        }
        if ( !readData ) {
            // Not reading the data (generally for performance reasons).  To get complete metadata including
            // the units, need to read something out of the time series.

            // TODO QUESTION FOR BILL CHARLEY - is the following the correct way to get the data units
            // without reading the time series, or does it actually read ALL the data (which would be undesirable
            // because it is slow)?  Or does it do nothing if the time series has not been read?
            // It does not seem to result in units coming back so I have commented it out
            /*
            HecTimeSeries rts = new HecTimeSeries();
            rts.setDSSFileName(dssFilename);
            HecTime start = new HecTime();
            HecTime end = new HecTime();
            stringContainer unitsCont = new stringContainer();
            stringContainer typeCont = new stringContainer();

            //int num =
                rts.getTSRecordInfo(start, end, unitsCont, typeCont);
            ts.setDataUnits ( unitsCont.toString() );
            ts.setDataUnitsOriginal ( unitsCont.toString() );
            */
            
            // TODO SAM 2009-01-08 If the above does not work, maybe can read the time series records in limited
            // format (e.g., first day in period) to get the units when not reading data).
        }
        else {
            // Read the time series data.  This is the only way to get data units so reading
            // no data above will have blank units.

            // For now just set the D part to first value because it does not seem that a range
            // is allowed when reading.
            // Setting to blank causes an exception - occurs because of irregular time series?
            // Latest... don't set because the time window is always required for proper read and will be set
            // to requested or based on the D part from above.
            //if ( (dPart1 != null) && (dPart1.length() > 0) && (dPart.indexOf("-") >= 0) ) {
            //    dssPathName.setDPart(dPart1);
            //}

            HecTimeSeries rts = new HecTimeSeries();
            rts.setDSSFileName(dssFilename);
            // If the read period has been requested, use it when reading the time series from HEC-DSS
            if ( (readStartReq != null) && (readEndReq != null) ) {
                String timeWindow = createTimeWindowString ( adjustRequestedDateTimePrecision(readStartReq,true),
                    adjustRequestedDateTimePrecision(readEndReq,false), ts, true );
                Message.printStatus(2, routine,
                    "Setting time window for read (requested by calling code) to \"" + timeWindow + "\"" );
                rts.setTimeWindow(timeWindow);
            }
            else {
                // No specific period has been requested so read all available data.  Not sure if there is a
                // simple API to do this so do it brute force by coming up with temporary dates based on the
                // condensed pathname D parts.  Request that hour be added because it seems to be required.
                String timeWindow = createTimeWindowString ( date1FromDPart,
                    adjustEndBlockDateToLastDateTime(ts, date2FromDPart), ts, true );
                Message.printStatus(2, routine,
                    "Setting time window for read (based on catalog) to \"" + timeWindow + "\"" );
                rts.setTimeWindow(timeWindow);
 
                // TODO QUESTION FOR BILL CHARLEY - reading the entire period without the user having to specify
                // that information is a very common use case.  It would be great if there were a way to read
                // the entire time series without specifying the window.  I'm still seeing that by default if the
                // window is not specified it only reads the first "block".  Is there something easier than what
                // I have done above?  DSS-VUE seems to do something because you can view the entire time series
                // without specifying the time window.
            }

            Message.printStatus(2, routine, "Reading data using path \"" + dssPathName + "\"" );
            rts.setPathname(dssPathName.toString());
            // This appears to be equivalent to negative Float.MAX_VALUE
            ts.setMissing( Heclib.UNDEFINED_DOUBLE );

            if ( Message.isDebugOn ) {
                Message.printDebug(2, routine, "Before rts.read()" );
            }
            TimeSeriesContainer tsc = new TimeSeriesContainer();
            // False below means don't remove missing
            
            // TODO SAM 2009-01-08 Evaluate whether true or false should be specified.  If true, then there is
            // a performance hit in the HEC code to remove the missing values.  If false, there is more overhead
            // to process more records.  Not sure what is best.
            
            // TODO QUESTION FOR BILL CHARLEY - The RTi time series package handles missing values.  Regular
            // time series are filled with missing at initialization.  I'm not sure what the performance impacts
            // are for the boolean (see also above TODO comment).
            
            int status = rts.read (tsc,false);
            Message.printStatus(2, routine, "Status from read = " + status + " number of values=" + tsc.values.length );
            // Some time series don't have a period so can't set dates in RTi TS from HecTimeSeries
            // TODO QUESTION FOR BILL CHARLEY - why do some time series not have dates for their period?  Is it
            // because a time series is defined but no data records are saved?
            if ( Message.isDebugOn ) {
                Message.printDebug(2, routine, "Before setDataPeriod()" );
            }
            // readData2 below indicates that a period was determined so it is OK to continue reading the data.
            // If OK, it also sets the period for the RTi time series.  However, it is possible that data were
            // read but the output of getTimeRange() is not valid so rely on the data values below to set the dates.
            readData2 = setDataPeriodFromData ( ts, tsc, readStartReq, readEndReq );
            // Remember units are not available until data records are read so handle units below.
            String units = rts.units();
            ts.setDataUnits ( units );
            ts.setDataUnitsOriginal ( units );
            // FIXME SAM 2009-01-09 Need to evaluate mapping of HEC units with standard units supported by
            // RTi code base
            if ( (unitsReq != null) && !unitsReq.equalsIgnoreCase(units) ) {
                throw new RuntimeException ( "Requested units \"" + unitsReq +
                     "\" do not equal time series units and don't know how to convert." );
            }
            if (status < -1) {
                // No data - OK if time series was defined but no data saved
                Message.printStatus(2,routine, "No Data for " + dssPathName + " in \"" + dssFilename + "\"");
            }
            else if ( readData2 || (tsc.values.length > 0) ) {
                // Have data - transfer to the RTi time series instance.
                if ( Message.isDebugOn ) {
                    Message.printStatus(2, routine, "Start transferring data." );
                }
                // Reset the dates in the time series to either the requested or the range of
                // dates in the returned data.  This is easier than trying to interpret the path dates for
                // returned data.
                if ( (readStartReq == null) && (readEndReq == null) ) {
                    setTimeSeriesDatesToData ( ts, tsc.times );
                }
                // Now allocate the data space for the time series before actually transferring the data.
                ts.allocateDataSpace();
                // FIXME SAM 2009-01-08 Evaluate transfer of quality flag to RTi time series
                
                // Transfer the data values from the HEC time series container - the container arrays are
                // apparently public to increase performance.
                HecTime hecTime = new HecTime();
                // DateTime for iteration is copy of time series start to get precision
                DateTime date = new DateTime ( ts.getDate1() );
                for ( int idata = 0; idata < tsc.values.length; idata++ ) {
                    // Assume that access is direct on the arrays for performance reasons
                    hecTime.set ( tsc.times[idata] );
                    if ( Message.isDebugOn ) { 
                        Message.printDebug ( 10, routine, "Setting value " + tsc.values[idata] + " at " +
                            tsc.times[idata] + " (" + hecTime + ")");
                    }
                    if ( !hecTime.isDefined() || ts.isDataMissing(tsc.values[idata])) {
                        // Don't try to set because this may cause exceptions in some cases.
                        continue;
                    }
                    // Set RTi DateTime class instances to HecTime data
                    // Monthly and daily values are set using hecTime like 31 January 2000 24:00
                    // ... so be picky about how dates roll over hour 24
                    if ( (tsIntervalBase == TimeInterval.YEAR) || (tsIntervalBase == TimeInterval.MONTH) ||
                        (tsIntervalBase == TimeInterval.DAY) ) {
                        setDateTime ( date, hecTime, tsIntervalBase, false );
                    }
                    else {
                        setDateTime ( date, hecTime, tsIntervalBase, true );
                    }
                    // Set the value in the RTi time series
                    ts.setDataValue( date, tsc.values[idata]);
                    // FIXME SAM 2009-01-08 Here is where units would be converted, or do it on the entire time
                    // series once read.
                }
            }
        }
        // Add the time series to the list
        tslist.add ( ts );
    }
    // FIXME SAM 2009-01-08 Closing the file is a performance hit so don't probably don't want to do it
    // for each call.  Need to evaluate the cache of opened DSS files and making sure they do get closed out
    // so that they don't interfere with other file resources in the session.  DSS files seem to have a timeout
    // for non-use so this can be good in that files would automatically close, but if we rely on this, we could
    // end up with too many open files that don't close in a timely manner and free up resources.
    
    // TODO QUESTION FOR BILL CHARLEY - I don't know how many DSS files we might want to open at once but for some
    // other formats we open hundreds or thousands of files so managing open files may be an issue.  I'm going to
    // look at caching, something like:
    //
    // List openHecDSS...
    // openHecDSS ( filename )
    //      if ( not already opened )
    //          open it...
    //          add to the list of open files
    //      else
    //          open so return the object to use
    // Comments?  Is something like this already done internally in the HECl library for optimization?
    
    // Close out the file so that reading multiple files does not waste resources
    //rts.done();
    //rts.close();  //  Close the file (usually at the end of the program)
    //dssFile.close();
    return tslist;
}

/**
Set the period in the TS using the HecTimeSeries.
@param ts RTi time series to set period.
@param hects HEC time series to get period from.
@return true if the period can be set, false if not (undefined period in the time series).
*/
private static boolean setDataPeriodFromData ( TS ts, TimeSeriesContainer tsc, DateTime readStart, DateTime readEnd )
{   String routine = "HecDssAPI.setDataPeriod";
    // The precision of the dates will be handled based on the time series interval.
    HecTime hecStart = new HecTime();
    HecTime hecEnd = new HecTime();
    if ( Message.isDebugOn ) {
        Message.printDebug(2, routine, "Before hects.getSeriesTimeRange()" );
    }
    // This did not work so just look at the data values themselves
    //hects.getSeriesTimeRange ( hecStart, hecEnd, 0 );
    if ( (tsc != null) && (tsc.times != null) && (tsc.times.length > 0) ) {
        hecStart.set ( tsc.times[0] );
        hecEnd.set ( tsc.times[tsc.times.length - 1] );
    }
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
    // Allow rollover because for monthly data need 1999-12-31 24 to show up as 2000-01 and
    // daily to show up as 2000-01-01
    setDateTime ( date1, hecStart, ts.getDataIntervalBase(), true );
    DateTime date2 = new DateTime();
    // TODO SAM 2009-03-30 Not sure about roll-over here?
    setDateTime ( date2, hecEnd, ts.getDataIntervalBase(), true );
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
    // Have period for the time series.
    return true;
}

/**
Set an RTi DateTime from a HecTime instance.
@param date RTi DateTime instance to modify.
@param hecTime HecTime instance from which to retrieve data.
@param tsIntervalBase time series interval base.  Monthly dates in hecTime will have an hour of 24 and therefore
need to ignore the hour when setting the date.
@param rollOver if True allow the day to roll over if hour is 24.  If false, do not roll over the day for year,
month, and day interval.  This flag is needed because some of the period-related dates are at hour 24 of a prior
day and data is typically on the same day.
*/
private static void setDateTime ( DateTime date, HecTime hecTime, int tsIntervalBase, boolean rollOver )
{
    date.setYear( hecTime.year() );
    if ( tsIntervalBase == TimeInterval.YEAR ) {
        return;
    }
    date.setMonth( hecTime.month() );
    if ( (tsIntervalBase == TimeInterval.MONTH) && !rollOver ) {
        return;
    }
    date.setDay( hecTime.day() );
    if ( (tsIntervalBase == TimeInterval.DAY) && !rollOver ) {
        return;
    }
    boolean hour24 = false;
    if ( (hecTime.hour() == 24) ) {
        // Set to hour 23 and then increment by one hour after setting everything, to roll to hour "24"
        // This is because RTi DateTime only allows hour 0
        date.setHour( 23 );
        hour24 = true;
    }
    else {
        date.setHour( hecTime.hour() );
    }
    if ( hour24 ) {
        date.addHour(1);
    }
    if ( tsIntervalBase == TimeInterval.HOUR ) {
        return;
    }
    date.setMinute( hecTime.minute() );
    if ( tsIntervalBase == TimeInterval.MINUTE ) {
        return;
    }
    date.setSecond( hecTime.second() );
}

/**
Set the time series period based on data read from the HEC-DSS file.  The precisions of the dates will
be set automatically in the TS.set*() methods.  The dates should be OK (no extra 24 hour rollover for monthly, etc.)
because the setDateTime() method only sets what is appropriate for the precision of the time series.
@param ts Time series being filled.
@param times list of times read from the HEC-DSS time series, corresponding to data.
*/
private static void setTimeSeriesDatesToData ( TS ts, int [] times )
{   String routine = "HecDssAPI.setTimeSeriesDatesToData";
    // Set the period to those of the data.
    HecTime hdataStart = new HecTime();
    hdataStart.set(times[0]);
    HecTime hdataEnd = new HecTime();
    hdataEnd.set (times[times.length - 1]);
    DateTime dataStart = new DateTime();
    // Allow the roll-over to the next day if hour 24 because for monthly data 1999-12-31 24 should actually
    // be 2000-01 and daily should be 2000-01-01
    setDateTime(dataStart,hdataStart,ts.getDataIntervalBase(),true);
    DateTime dataEnd = new DateTime();
    // Do not allow roll-over to the next day
    setDateTime(dataEnd,hdataEnd,ts.getDataIntervalBase(),false);
    // The following will properly adjust the precision to match the time series interval
    ts.setDate1(dataStart);
    ts.setDate2(dataEnd);
    Message.printStatus(2, routine, "Setting time series period to dates from data records:  " + dataStart +
        " to " + dataEnd );
}

/**
Convert an RTi time series identifier interval to a HEC-DSS time series interval.
*/
private static String tsidIntervalToHecInterval ( TSIdent tsid )
{
    String hecInterval = null;
    int intervalBase = tsid.getIntervalBase();
    int intervalMult = tsid.getIntervalMult();
    // FIXME SAM 2009-01-08 Need to handle irregular time series
    if ( intervalBase == TimeInterval.IRREGULAR ) {
        // Need special attention - not yet addressed
        throw new RuntimeException ( "Irregular time series are not yet handled for conversion to HEC-DSS.");
    }
    else if ( intervalBase == TimeInterval.YEAR ) {
        if ( intervalMult != 1 ) {
            throw new RuntimeException ( "Only 1YEAR interval is allowed, " + intervalMult +
                " year is not supported." );
        }
        hecInterval = "1YEAR";
    }
    else if ( intervalBase == TimeInterval.MONTH) {
        if ( intervalMult != 1 ) {
            throw new RuntimeException ( "Only 1MON interval is allowed, " + intervalMult +
                " month is not supported." );
        }
        hecInterval = "1MON";
    }
    else if ( intervalBase == TimeInterval.DAY) {
        if ( intervalMult != 1 ) {
            throw new RuntimeException ( "Only 1DAY interval is allowed, " + intervalMult +
                " month is not supported." );
        }
        hecInterval = "1DAY";
    }
    else if ( intervalBase == TimeInterval.HOUR) {
        if ( (intervalMult != 1) && (intervalMult != 2) && (intervalMult != 3) && (intervalMult != 4) &&
            (intervalMult != 6) && (intervalMult != 8) && (intervalMult != 12) ) {
            throw new RuntimeException ( "Only hours intervals 1, 2, 3, 4, 6, 8, and 12 are allowed, " +
                intervalMult + " hour is not supported." );
        }
        hecInterval = "" + intervalMult + "HOUR";
    }
    else if ( intervalBase == TimeInterval.HOUR) {
        if ( (intervalMult != 1) && (intervalMult != 2) && (intervalMult != 3) && (intervalMult != 4) &&
            (intervalMult != 5) && (intervalMult != 10) && (intervalMult != 15) && (intervalMult != 20) &&
            (intervalMult != 30) ) {
            throw new RuntimeException ( "Only hours intervals 1, 2, 3, 4, 6, 8, and 12 are allowed, " +
                intervalMult + " hour is not supported." );
        }
        hecInterval = "" + intervalMult + "MIN";
    }
    else {
        throw new RuntimeException ( "Interval " + tsid.getInterval() +
            " is not supported with HEC-DSS time series." );
    }
    return hecInterval;
}

/**
Convert an RTi time series identifier to a HEC-DSS pathname.
This is designed for writing time series, not general use, especially the handling of the D part.
@param ts Time series that follows RTi conventions.
@param A A-part to override default from TSID, or null to use TSID.
@param B B-part to override default from TSID, or null to use TSID.
@param C C-part to override default from TSID, or null to use TSID.
@param E E-part to override default from TSID, or null to use TSID.
@param F F-part to override default from TSID, or null to use TSID.
@param writeStart output start date/time, or null if no D part should be included (currently not used).
@param writeEnd output end date/time, or null if no D part should be included (currently not used).
@return HEC-DSS pathname suitable for use with HEC-DSS files.
*/
private static DSSPathname getHecPathNameForTimeSeries ( TS ts, String A, String B, String C, String E, String F,
    DateTime writeStart, DateTime writeEnd )
{
    DSSPathname path = new DSSPathname();
    TSIdent tsid = ts.getIdentifier();
    // A part from first part of location, separated by ':'
    String location = tsid.getLocation();
    int pos = location.indexOf(':');
    int intervalBase = ts.getDataIntervalBase();
    int intervalMult = ts.getDataIntervalMult();
    if ( pos >= 0 ) {
        // The location includes a : - assume that it is defined to be compatible with the HEC-DSS
        // conventions and has a location Apart:Bpart
        path.setAPart(location.substring(0,pos));
        path.setBPart(location.substring(pos+1));
    }
    else {
        // No colon so assume no basin
        path.setAPart ( "" );
        path.setBPart ( location );
    }
    // Reset with what was passed in.
    if ( (A != null) && !A.equals("") ) {
        path.setAPart( A );
    }
    if ( (B != null) && !B.equals("") ) {
        path.setBPart( B );
    }
    if ( (C != null) && !C.equals("") ) {
        path.setCPart(C);
    }
    else {
        path.setCPart(tsid.getType());
    }
    // D part depends on the interval
    if ( writeStart != null ) {
        if ( intervalBase == TimeInterval.YEAR ) {
            // Annual blocks are century
            path.setDPart( "01JAN" + (writeStart.getYear()/100)*100 );
        }
        else if ( intervalBase == TimeInterval.MONTH ) {
            // Blocks are decades
            path.setDPart( "01JAN" + (writeStart.getYear()/10)*10 );
        }
        else if ( intervalBase == TimeInterval.DAY ) {
            // Blocks are years
            path.setDPart( "01JAN" + writeStart.getYear() );
        }
        else if ( (intervalBase == TimeInterval.HOUR) ||
            ((intervalBase == TimeInterval.MINUTE) && (intervalMult == 15) || (intervalMult == 20) ||
            (intervalMult == 30))) {
            // Blocks are month
            path.setDPart( "01" + TimeUtil.monthAbbreviation(writeStart.getMonth()).toUpperCase() +
                writeStart.getYear() );
        }
        else if ( intervalBase == TimeInterval.MINUTE ) {
            // The rest of the minutes - blocks are day
            path.setDPart( StringUtil.formatString(writeStart.getDay(), "%02d") +
                    TimeUtil.monthAbbreviation(writeStart.getMonth()).toUpperCase() +
                    writeStart.getYear() );
        }
    }
    if ( (E != null) && !E.equals("") ) {
        path.setEPart(E);
    }
    else {
        path.setEPart(tsidIntervalToHecInterval(tsid));
    }
    if ( (F != null) && !F.equals("") ) {
        path.setFPart(F);
    }
    else {
        path.setFPart(tsid.getScenario()); 
    }
    return path;
}

/**
Get the uncondensed pathname string given a condensed path name.  Examine the D part for a dash and two dates
and return the first date only.
@param DSS pathhname to evaluate.
@return the uncondensed pathname.  A new instance is always returned.
*/
private static DSSPathname uncondensedPathName ( DSSPathname dssPathName )
{
    DSSPathname uncondensed = new DSSPathname(dssPathName.toString());
    String dPart = dssPathName.getDPart();
    if ( (dPart.length() == 21) && Character.isDigit(dPart.charAt(0)) && dPart.regionMatches(9, " - ", 0, 3)) {
        // Assume that the part might be a normal path of form DDMonYYYY - DDMonYYYY
        dPart = dPart.substring(0,9);
        uncondensed.setDPart(dPart);
    }
    return uncondensed;
}

/**
Write a list of time series to a HEC-DSS file.  
@param outputFile the output file to write.  The file will be created if it does not already exist.
The parent folder must exist.
@param tslist list of time series to write.  The HEC-DSS pathname for each time series will be created from the
RTi time series ID convention (see readTimeSeries() for documentation).
@param writeStartReq the requested start for output, or null to write all data.
@param writeEndReq the requested end for output, or null to write all data.
@param unitsReq the requested units for output, currently not used.
@param precisionReq the requested precision for output, digits after the period (specify as negative to use the
default).
@param hecType the "type" in HEC-DSS convention (e.g., "PER-AVER").
@param A A-part to override default from TSID, or null to use TSID.
@param B B-part to override default from TSID, or null to use TSID.
@param C C-part to override default from TSID, or null to use TSID.
@param E E-part to override default from TSID, or null to use TSID.
@param F F-part to override default from TSID, or null to use TSID.
@param replaceTimeSeries if true, delete the existing time series before writing.
@param closeFileAfterWrite if true, close the HEC-DSS file after write.  This may slow overall performance but
will ensure that the process does not lock the file for removal, etc.
@see #readTimeSeries(File, String, DateTime, DateTime, String, boolean)
@exception IOException if the HEC-DSS file cannot be written.
@exception Exception if other errors occur.
*/
public static void writeTimeSeriesList ( File outputFile, List tslist, DateTime writeStartReq, DateTime writeEndReq,
        String unitsReq, int precisionReq, String hecType, String A, String B, String C, String E, String F,
        boolean replaceTimeSeries, boolean closeFileAfterWrite )
throws IOException, Exception
{   String routine = "HecDssAPE.writeTimeSeriesList";

    if ( (tslist == null) || (tslist.size() == 0) ) {
        // Nothing in the list so return
        return;
    }
    
    // Turn messaging to high to troubleshoot
    if ( Message.isDebugOn ) {
        HecDataManager.setMessageLevel ( 9 );
        //HecDataManager.setMessageLevel ( 12 );
    }

    // Loop through the time series and write each to the file
    int tslistSize = tslist.size();
    List<String> pathsNotWritten80List = new Vector(); // HEC-DSS time series that could not be written (path > 80 char)
    List<String> pathsNotWrittenBadIntervalList = new Vector(); // HEC-DSS time series that could not be written (interval not supported)
    List<String> errorDuplicatePathsList = new Vector(); // Writing multiple time series with the same path
    List<String> pathsWrittenNoDPartList = new Vector(); // Unique list of paths (no D parts) that are written (duplicates removed) - for error check
    HecTimeSeries hts = null; // Put here because handle on time series is used to close HEC-DSS files after loop
    for ( int i = 0; i < tslistSize; i++ ) {
        TS ts = (TS)tslist.get(i);
        // Get the date/times for output, either the entire period or the requested date/times
        DateTime writeStart = ts.getDate1();
        DateTime writeEnd = ts.getDate2();
        if ( writeStartReq != null ) {
            writeStart = new DateTime(writeStartReq);
        }
        if ( writeEndReq != null ) {
            writeEnd = new DateTime(writeEndReq);
        }
        // Create a container to receive the data from the RTi time series
        TimeSeriesContainer tsc = new TimeSeriesContainer();
        int numValues = TSUtil.calculateDataSize(ts, writeStart, writeEnd);
        Message.printStatus(2, routine, "Writing " + numValues + " values for period " + writeStart + " to " + writeEnd );
        // Allocate the container arrays.
        tsc.numberValues = numValues;
        tsc.values = new double[numValues];
        tsc.times = new int[numValues];
        DateTime date = new DateTime(writeStart);
        TSIterator tsi = ts.iterator(); // Iterator for time series data
        TSData dataPoint; // Single data point in time series
        HecTime hectime = new HecTime(); // Used for transfer of date/times to HEC time series
        double value;
        // Iterate through the time series and transfer the date and corresponding values from
        // the RTi time series to the HEC time series container.
        for ( int ival = 0; ((dataPoint = tsi.next()) != null) && (ival < numValues); ival++ ) {
            value = dataPoint.getData();
            date = dataPoint.getDate();
            // Check for missing values in the RTi time series and set the the HEC missing value for output
            if( ts.isDataMissing( value ) ) {
                // Translate to the missing value used by HEC
                value = Heclib.UNDEFINED_DOUBLE;
            }
            // Set in the arrays
            tsc.values[ival] = value;
            tsc.times[ival] = dateTimeToHecTime(date, hectime).value();
            if ( Message.isDebugOn ) {
                Message.printDebug(1,routine, "[" + ival + "] TS container has value " + value + " at DateTime=" + date +
                    " HecTime=" + hectime + " HecTime-int=" + tsc.times[ival] );
            }
        }
        tsc.startTime = tsc.times[0];
        tsc.endTime = tsc.times[numValues - 1];
        HecTime hstart = new HecTime();
        hstart.set(tsc.startTime);
        HecTime hend = new HecTime();
        hend.set(tsc.endTime);
        Message.printStatus(2, routine, "HEC-DSS time series container startTime=" + hstart + " endTime=" + hend );
        // Set the precision of output, if requested by calling code
        if ( precisionReq >= 0 ) {
            Message.printStatus(2, routine, "HEC-DSS time series container precision=" + precisionReq );
            tsc.precision = precisionReq;
        }
        // Set the pathname and use the E part to get the interval.
        DSSPathname dssPathName = getHecPathNameForTimeSeries(ts, A, B, C, E, F, null, null );
        String pathname = dssPathName.toString(); // TODO SAM 2009-04-15 Evaluate use .toUpperCase();
        // Check the path name for <= 80 characters.
        if ( pathname.length() > 80 ) {
            pathsNotWritten80List.add ( pathname );
        }
        // Verify that the interval is ok.
        if ( !isTimeSeriesIntervalSupportedByAPI(ts) ) {
            pathsNotWrittenBadIntervalList.add ( pathname );
        }
        // Verify that the path is not already written (ignore the D part)
        DSSPathname pathnameNoD = new DSSPathname();
        pathnameNoD.setAPart(dssPathName.getAPart());
        pathnameNoD.setBPart(dssPathName.getBPart());
        pathnameNoD.setCPart(dssPathName.getCPart());
        pathnameNoD.setEPart(dssPathName.getEPart());
        pathnameNoD.setFPart(dssPathName.getFPart());
        String pathnameNoDString = pathnameNoD.toString();
        boolean duplicateFound = false;
        int pathsWrittenNoDPartListSize = pathsWrittenNoDPartList.size();
        Message.printStatus(2, routine, "Checking " + pathsWrittenNoDPartListSize + " paths for duplicates.");
        for ( int ipath = 0; ipath < pathsWrittenNoDPartListSize; ipath++ ) {
            Message.printStatus(2, routine, "Checking \"" + pathnameNoDString + "\" vs \"" +
                    pathsWrittenNoDPartList.get(ipath) + "\"" );
            if ( pathnameNoDString.equalsIgnoreCase(pathsWrittenNoDPartList.get(ipath))) {
                duplicateFound = true;
                errorDuplicatePathsList.add(pathname); // Include D so user can match
                break;
            }
        }
        if ( !duplicateFound ) {
            // Add to the list of unique paths (no D part) that are written, to allow check
            pathsWrittenNoDPartList.add ( pathnameNoDString );
        }
        // End of duplicate path check
        tsc.fullName = pathname;
        tsc.interval = HecTimeSeries.getIntervalFromEPart(dssPathName.getEPart());
        // Set the data units - any string can be used but may want to standardize to allow units conversion
        tsc.units = ts.getDataUnits();
        if ( (tsc.units == null) || (tsc.units.length() == 0) ) {
            throw new RuntimeException ( "The HEC-DSS data units are not defined for \"" +
                ts.getIdentifierString() + "\"" );
        }
        Message.printStatus(2, routine, "HEC-DSS time series container units=\"" + tsc.units + "\"" );
        // Set the data type.  This is equivalent to the RTi TimeScale.  Allowable values are:
        //   PER-AVER (period average, is this interval average?)
        //   PER-CUM (period cumulative, is this interval cumulative?)
        //   INST-VAL (instantaneous)
        //   INST-CUM (instantaneous cumulative)
        // FIXME SAM 2008-12-01 Try this to see if it solves the -5 status on write (it does not)
        if ( hecType == null ) {
            throw new RuntimeException ( "The HEC-DSS type is not defined for \"" +
                ts.getIdentifierString() + "\"" );
        }
        else {
            tsc.type = hecType;
            Message.printStatus(2, routine, "HEC-DSS time series container type=\"" + tsc.type + "\"" );
        }
        // The pathname will NOT contain a D part.  It does not seem that a D part is necessary based on the
        // HecDssTimeSeriesExample.java code.  Do convert it to upper-case though because it seems that HEC strings
        // are all upper-case.
        String outputFileCanonical = outputFile.getCanonicalPath();
        Message.printStatus( 2, routine, "HEC-DSS time series output file is \"" + outputFileCanonical + "\"" );
        Message.printStatus( 2, routine, "HEC-DSS time series pathname is \"" + pathname + "\"" );
        // Create HEC time series and write the container to the specified file.
        hts = new HecTimeSeries();
        hts.setDSSFileName(outputFileCanonical);
        // Delete the old records if replacing
        if ( replaceTimeSeries ) {
            // FIXME SAM 2009-04-05 See Bill Charley email about ZDELET?
            //Heclib.zdelet(ifltab, pathname);
        }
        // Now try to write...
        int status = hts.write(tsc);
        Message.printStatus( 2, routine, "Status from writing time series is " + status );
        if ( status != 0 ) {
            // FIXME SAM 2009-01-13 Evaluate code.
            // For now throw an exception whenever an error occurs, until we figure out status.
            // May want to accumulate errors to put in one exception message.
            throw new RuntimeException ( "Error writing time series to HEC-DSS file, return status=" + status);
        }
    }
    // Close the file if requested - not the default because it may be a performance hit
    if ( closeFileAfterWrite ) {
        if ( hts != null ) {
            // TODO SAM 2009-04-14 does not seem to work
            hts.done();
            hts.close();
            hts.closeDSSFile();
        }
        //Heclib.zclose(ifltab);
        // The following is code from Bill Charley
        closeAllFiles();
    }
    // Throw an exception if there were any other problems in the loop.  Could put this in the loop but want
    // as much writing to occur as possible.  For now a single message is passed back to the calling code.
    int pathsNotWritten80ListSize = pathsNotWritten80List.size();
    int pathsNotWrittenBadIntervalListSize = pathsNotWritten80List.size();
    StringBuffer b = new StringBuffer();
    if ( (pathsNotWritten80ListSize > 0) || (pathsNotWrittenBadIntervalListSize > 0) ) {
        b.append ( "\nError writing the following time series because the pathname is > 80 characters:\n" );
        for ( int i = 0; i < pathsNotWritten80ListSize; i++ ) {
            if ( i > 0 ) {
                b.append ( "\n" );
            }
            b.append ( (String)pathsNotWritten80List.get(i) );
        }
        b.append ( "\nError writing the following time series because the interval is not supported:\n" );
        for ( int i = 0; i < pathsNotWrittenBadIntervalListSize; i++ ) {
            if ( i > 0 ) {
                b.append ( "\n" );
            }
            b.append ( (String)pathsNotWrittenBadIntervalList.get(i) );
        }
    }
    int errorDuplicatePathsListSize = errorDuplicatePathsList.size();
    if ( errorDuplicatePathsListSize > 0 ) {
        b.append ( "\nDuplicate HEC-DSS paths (A-C,E-F) for the following time series (may result in merged/overwritten output):\n" );
        for ( int i = 0; i < errorDuplicatePathsListSize; i++ ) {
            if ( i > 0 ) {
                b.append ( "\n" );
            }
            b.append ( (String)errorDuplicatePathsList.get(i) );
        }
    }
    if ( b.length() > 0 ) {
        throw new RuntimeException ( b.toString() );
    }
}

}