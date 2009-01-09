package rti.tscommandprocessor.commands.hecdss;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import hec.dssgui.CondensedReference;
import hec.heclib.dss.DSSPathname;
import hec.heclib.dss.HecDSSFileAccess;
import hec.heclib.dss.HecDSSUtilities;
import hec.heclib.dss.HecTimeSeries;
import hec.heclib.util.Heclib;
import hec.heclib.util.HecTime;
import hec.heclib.util.stringContainer;
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
    
    boolean loadUsingJavaLibraryPath = false;
    String dll = "";
    boolean loadRmaUtil = true; // Quick way to turn on/off rmaUtil.dll load, for testing dependencies
    try {
        // This relies on the java.library.path to locate the javaHeclib.dll,
        // which may be problematic since that configuration is outside the control
        // of this software and may result in a version issue.
        if ( loadUsingJavaLibraryPath ) {
            dll = "javaHeclib";
            System.loadLibrary(dll);
            if ( loadRmaUtil ) {
                dll = "rmaUtil";
                System.loadLibrary(dll);
            }
        }
        else {
            // Instead, load the file explicitly knowing the application home and
            // assuming that the file exists in the bin folder.  This is safer in that a specific DLL version
            // can be loaded.  However, it may result in a duplicate load because the HEC software itself may try
            // to load the DLL using the above method, which does not look for the filename in the path,
            // but matches the requested dll by name, which won't match the path below, even though it is
            // already loaded.  Either approach may be OK as long as the application install controls the
            // Java run-time environment.
            dll = IOUtil.getApplicationHomeDir() + "/bin/javaHeclib.dll";
            Message.printStatus(2, routine, "Attempting to load library using System.load(" + dll + ")." );
            System.load( dll );
            Message.printStatus(2, routine, "Successfully loaded library \"" + dll + "\"" );
            if ( loadRmaUtil ) {
                dll = IOUtil.getApplicationHomeDir() + "/bin/rmaUtil.dll";
                Message.printStatus(2, routine, "Attempting to load library using System.load(" + dll + ")." );
                System.load( dll );
                Message.printStatus(2, routine, "Successfully loaded library \"" + dll + "\"" );
            }
        }
    }
    // Exceptions should only be thrown if the test environment or build process is incorrect and should be
    // corrected on the developer side - users should never see an issue if the build process is correct.
    catch ( UnsatisfiedLinkError e ) {
        if ( loadUsingJavaLibraryPath ) {
            Message.printWarning ( 2, routine, "Unable to load " + dll + " using System.loadLibrary(" +
                dll + ") and java.library.path \"" + System.getProperty("java.library.path") + "\"." );
        }
        else {
            Message.printWarning ( 2, routine, "Unable to load " + dll + " using System.load(" + dll + ")." );
        }
        Message.printWarning ( 2, routine, "HEC-DSS features will not be functional." );
        Message.printWarning( 3, routine, e);
        // Rethrow the error so it can be indicated as a command error
        throw ( e );
    }
}

/**
TODO SAM 2008-01-08 This method may be unnecessary if the condensed catalog code in the HEC library can be
figured out for all requirements.

Create a condensed pathname list.  Records that are duplicates except for D parts are removed from the list.
This is necessary because the normal catalog method returns path names that show data blocks (e.g., one block
per year), resulting in multiple pathnames for the same time series.
@param pathnameList A list of pathnames (as String).  This will be modified and returned.
*/
private static List createCondensedCatalog ( List pathnameList )
{   String routine = "HecDssAPI.createCondensedCatalog";
    // Sort the pathnames to make sure that duplicates are grouped - seems like this is not needed
    // because the catalog is always sorted.  For now don't sort to increase performance
    
    // Loop through the pathnames.  Compare the parts with the previous item.  If all parts are the same
    // except for the D part, remove the item and change the D part to be inclusive
    
    int size = pathnameList.size();
    String aPart, aPartPrev = null; // Basin
    String bPart, bPartPrev = null; // Location
    String cPart, cPartPrev = null; // Data type
    String dPart, dPartPrev = null; // Period
    String dPartMerged, dPartPrevMerged = null; // Period, merged
    String ePart, ePartPrev = null; // Interval
    String fPart, fPartPrev = null; // Scenario
    DSSPathname dssPathName;
    String pathname;
    int nMerged = 0; // Number of records merged to make one record
    int nRemoved = 0; // Number of records removed, total
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
            // The parts match except for D part so need to merge
            ++nMerged;
            // Throw away the current pathname and update the D part in the kept (first) record to be
            // the previous value to the current value
            if ( dPartPrevMerged == null ) {
                // This is the first record so just set the merged to the single date
                dPartMerged = dPart;
            }
            else {
                // 2nd+ records...
                pos = dPartPrevMerged.indexOf("-");
                if ( pos < 0 ) {
                    // Previous D part was not a range so use it as is to construct a range
                    dPartMerged = dPartPrev + " - " + dPart;
                    dssPathName.setDPart( dPartMerged );
                }
                else {
                    // Previous D part was a range so get the first part and then replace the second with
                    // the current D part
                    dPartMerged = dPartPrev.substring(0,pos).trim() + " - " + dPart;
                    dssPathName.setDPart( dPartMerged );
                }
            }
            // Comment once the code is verified.
            Message.printStatus( 2, routine, "Condensed A=" + aPart +
                " B=" + bPart + " C=" + cPart + " E=" + ePart + " F=" + fPart + " D now = " + dPartMerged +
                " #records merged=" + nMerged );
            // Remove the redundant pathname record since it has been processed (no need to reset "prev" since
            // it is the same as the current record.
            pathnameList.set(i - 1,dssPathName.toString() );
            pathnameList.remove(i);
            ++nRemoved;
            --i;
            --size;
            dPartPrev = dPart; // The only thing that is different is related to D
            dPartPrevMerged = dPartMerged;
            continue;
        }
        // Else the record remains in the list as either a single-record path or the first record in a
        // multi-record path.
        // Save the parts of the current record as "prev" to allow check of the next record
        aPartPrev = aPart;
        bPartPrev = bPart;
        cPartPrev = cPart;
        dPartPrev = dPart;
        ePartPrev = ePart;
        fPartPrev = fPart;
        // No merged record exists, just a single record
        dPartPrevMerged = null;
        nMerged = 1;
    }
    Message.printStatus( 2, routine, "Removed " + nRemoved + " path records during condensing." );
    return pathnameList;
}

/**
Convert an RTi DateTime to HecTime instance.
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
    
    ht.setYearMonthDay(dt.getYear(), dt.getMonth(), dt.getDay(), (dt.getHour()*60 + dt.getMinute()) );
    int precision = dt.getPrecision();
    //if ( precision == DateTime.PRECISION_YEAR ) {
    //    ht.setTimeGranularity(ht.??);
    //}
    
    return ht;
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
@param readStart the DateTime to start reading data, with precision at least as fine as the time series.
If null, read all available data.
@param readEnd the DateTime to end reading data, with precision at least as fine as the time series.
If null, read all available data.
@param unitsReq requested units (under development, since HEC units may not be consistent with calling software).
@param readData if true, read the time series values.  If false, only read the metadata.
@return a list of time series that were read.
@throws Exception if there is an error reading the time series
*/
public static List readTimeSeriesList ( File file, String tsidentPattern,
        DateTime readStart, DateTime readEnd, String unitsReq, boolean readData )
throws Exception
{   String routine = "HecDssAPI.readTimeSeriesList";
	List tslist = new Vector();
    TSIdent tsident = new TSIdent ( tsidentPattern );
    String dssFilename = file.getCanonicalPath();
    HecDSSFileAccess dssFile = new HecDSSFileAccess ( dssFilename );
    // FIXME SAM 2009-01-08 Need to implement a cache so that the file is not repeatedly opened
    // Do this similar to other binary file databases like StateMod and StateCU
    int stat = dssFile.open();
    Message.printStatus ( 2, routine, "Status from opening DSS file is " + stat );
    // Use the following code to allow wildcards in matching 1+ time series
    // Normally wildcards will only be used when doing a bulk read and a single time series
    // will be matched for a specific time series identifier.
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
        // Handle the D part similarly regardless of whether using RTi or HEC code above.
        if ( !dPart.equals("") ) {
            // Parse out the D part into DateTime objects, but only to day precision since that is all that is in D.
            // Note that the D part might be one date if no condensing was necessary, or two parts if
            // condensing occurred.
            
            // FIXME SAM 2008-09-02 Might be HECLIB code to do this
            
            // TODO QUESTION FOR BILL CHARLEY - is there code to convert the D part to one or two date/times?
            // Then I would not need to deal with the parsing here.
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
        }
        Message.printStatus ( 2, routine,
                "A=\"" + aPart + "\" " +
                "B=\"" + bPart + "\" " +
                "C=\"" + cPart + "\" " +
                "D=\"" + dPart + "\" " +
                "E=\"" + ePart + "\" " +
                "F=\"" + fPart + "\"" );
        // Create time series.
        // TODO SAM 2009-01-08 Need to evaluate how to handle use of reserved characters (periods and dashes) in
        // HEC parts since these conflict with RTi time series identifier conventions.  For now, replace the
        // offending characters with spaces in the identifier and utilize the time
        // series alias to retain the original identifier.
        String dotfPart = "";
        String dotfPartNoPeriod = ""; // Dot F part without a period
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
        // based on the D part.
        ts.setDate1 ( date1FromDPart );
        if ( !date1FromDPart.equals(date2FromDPart) ) {
            ts.setDate2 ( date2FromDPart );
        }
        if ( !readData ) {
            // Not reading the data (generally for performance reasons).  To get complete metadata including
            // the units, need to read something out of the time series.

            // TODO QUESTION FOR BILL CHARLEY - is the following the correct way to get the data units
            // without reading the time series, or does it actually read ALL the data (which would be undesirable
            // because it is slow)?
            
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
            
            // TODO SAM 2009-01-08 If the above does not work, maybe can read the time series records in limited
            // format (e.g., first day in period) to get the units when not reading data).
        }
        else {
            // Read the time series data.  This is the only way to get data units so reading
            // no data above will have blank units.

            // For now just set the D part to first value because it does not seem that a range
            // is allowed when reading.
            // Setting to blank causes an exception - occurs because of irregular time series?
            if ( (dPart1 != null) && (dPart1.length() > 0) && (dPart.indexOf("-") >= 0) ) {
                dssPathName.setDPart(dPart1);
            }

            HecTimeSeries rts = new HecTimeSeries();
            rts.setDSSFileName(dssFilename);
            // If the read period has been requested, use it when reading the time series from HEC-DSS
            if ( (readStart != null) && (readEnd != null) ) {
                // Format of the period to read...
                //rts.setTimeWindow("04Sep1996 1200 05Sep1996 1200");
                String start = StringUtil.formatString(readStart.getDay(),"%02d") +
                    TimeUtil.monthAbbreviation(readStart.getMonth()) +
                    StringUtil.formatString(readStart.getYear(), "%04d");
                String end = StringUtil.formatString(readEnd.getDay(),"%02d") +
                    TimeUtil.monthAbbreviation(readEnd.getMonth()) +
                    StringUtil.formatString(readEnd.getYear(), "%04d");
                if ( (ts.getDataIntervalBase() == TimeInterval.HOUR) ||
                    (ts.getDataIntervalBase() == TimeInterval.MINUTE) ) {
                    // Add the hour and minute for higher precision interval
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
                    // Always add 0000 on times specified to day precision
                    start = start + " 0000";
                    end = end + " 0000";
                }
                // Set the time range using the information from the catalog.
                String timeWindow = start + " " + end;
                Message.printStatus(2, routine, "Setting time window for read to \"" + timeWindow + "\"" );
                rts.setTimeWindow(timeWindow);
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
            // Some time series don't have a period so can't set dates in RTi TS from HecTimeSeries
            // TODO QUESTION FOR BILL CHARLEY - why do some time series not have dates for their period?  Is it
            // because a time series is defined but no data records are saved?
            if ( Message.isDebugOn ) {
                Message.printDebug(2, routine, "Before setDataPeriod()" );
            }
            // readData2 below indicates that a period was determined so it is OK to continue reading the data.
            // If OK, it also sets the period for the RTi time series.
            readData2 = setDataPeriod ( ts, rts, readStart, readEnd );
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
            else if ( readData2 ) {
                // Have data - transfer to the RTi time series instance.
                if ( Message.isDebugOn ) {
                    Message.printStatus(2, routine, "Start transferring data." );
                }
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
                    setDateTime ( date, hecTime );
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
private static boolean setDataPeriod ( TS ts, HecTimeSeries hects, DateTime readStart, DateTime readEnd )
{   String routine = "HecDssAPI.setDataPeriod";
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
    // Have period for the time series.
    return true;
}

/**
Set an RTi DateTime from a HecTime instance.
@param date RTi DateTime instance to modify.
@param hecTime HecTime instance from which to retrieve data.
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

/**
Convert an RTi time series identifier interval to a HEC-DSS time series interval.
*/
private static String tsidIntervalToHecInterval ( TSIdent tsid )
{
    String hecInterval = null;
    int intervalBase = tsid.getIntervalBase();
    // FIXME SAM 2009-01-08 Need to handle irregular time series
    if ( intervalBase == TimeInterval.IRREGULAR ) {
        // Need special attention - not yet addressed
        throw new RuntimeException ( "Irregular time series are not yet handled for conversion to HEC-DSS.");
    }
    else {
        // Just return the interval string
        hecInterval = tsid.getInterval();
    }
    return hecInterval;
}

/**
Convert an RTi time series identifier to a HEC-DSS pathname.
This is designed for writing time series, not general use, especially the handling of the D part.
@param tsid Time series identifier that follows RTi conventions.
@return HEC-DSS pathname suitable for use with HEC-DSS files.
*/
private static DSSPathname tsidToHecPathName ( TSIdent tsid )
{
    DSSPathname path = new DSSPathname();
    // A part from first part of location
    path.setAPart(tsid.getMainLocation());
    path.setBPart(tsid.getSubLocation());
    path.setCPart(tsid.getType());
    // TODO SAM 2009-01-08 See if it is OK to leave blank to use the whole time series in writing
    // Don't set D part
    path.setEPart(tsidIntervalToHecInterval(tsid));
    return path;
}

/**
Write a list of time series to a HEC-DSS file.  
@param outputFile the output file to write.  TThe file will be created if it does not already exist.
The parent folder must exist.
@param tslist list of time series to write.  The HEC-DSS pathname for each time series will be created from the
RTi time series ID convention (see readTimeSeries() for documentation).
@param writeStartReq the requested start for output, or null to write all data.
@param writeEndReq the requested end for output, or null to write all data.
@param unitsReq the requested units for output, currently not used.
@see #readTimeSeries(File, String, DateTime, DateTime, String, boolean)
@exception IOException if the HEC-DSS file cannot be written.
@exception Exception if other errors occur.
*/
public static void writeTimeSeriesList ( File outputFile, List tslist, DateTime writeStartReq, DateTime writeEndReq,
        String unitsReq )
throws IOException, Exception
{
    if ( (tslist == null) || (tslist.size() == 0) ) {
        // Nothing in the list so return
        return;
    }
    // TODO QUESTION FOR BILL CHARLEY - is it bad to loop like this, or should some of the file manipulation occur
    // outside the loop?
    
    // TODO QUESTION FOR BILL CHARLEY - I looked at the plugin example that you previously provided
    // to see how to transfer from external time series to a HEC-DSS time series and write to a file.
    // It had very little information transferred.  Our code is pretty rigorous about units, missing data value,
    // etc. and I tried to handle below.  However, I wonder if there are more container or HEC time series
    // data members that I should be setting before writing.
    
    // Loop through the time series and write each to the file
    int tslistSize = tslist.size();
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
        // Get the number of values in the time series for the write interval.  This works
        // for regular or irregular interval time series.
        int numValues = TSUtil.calculateDataSize(ts, writeStart, writeEnd);
        // Allocate the container arrays.
        tsc.values = new double[numValues];
        tsc.times = new int[numValues];
        DateTime date = new DateTime(writeStart);
        TSIterator tsi = ts.iterator(); // Iterator for time series data
        TSData dataPoint; // Single data point in time series
        HecTime hectime = new HecTime(); // Used for transfer of date/times to HEC time series
        double value;
        // Iterate through the time series and transfer the date and corresponding values from
        // the RTi time series to the HEC time series container.
        for ( int ival = 0; (dataPoint = tsi.next()) != null; ival++ ) {
            value = dataPoint.getData();
            date = dataPoint.getDate();
            // Check for missing values in the RTi time series and set the the HEC missing value for output
            if( !ts.isDataMissing( value ) ) {
                // Translate to the missing value used by HEC
                value = Heclib.UNDEFINED_DOUBLE;
            }
            // Set in the arrays
            tsc.values[ival] = value;
            // TODO QUESTION FOR BILL CHARLEY - I cannot figure out how to get from a HecTime to the time that
            // is needed in the container.  Because the data values coming from the RTi time series code MUST be
            // consistent with the values, what do I do?  The following is a guess based on the methods that
            // are available for the HecTime class.
            tsc.times[ival] = dateTimeToHecTime(date, hectime).value();
        }
        // Set the precision of output
        // FIXME SAM 2009-01-08 Not currently a parameter to this method but need to add if supported by HEC code
        
        //TODO QUESTION FOR BILL CHARLEY - is there a way to set the precision on output?  In the AddPlugin.java
        // example there is a method in HecDoubleArray but I am trying to use TimeSeriesContainer.  Is the following
        // correct, where precisionReq would be 2 to output hundredths?
        //tsc.precision = precisionReq;
        // Create HEC time series and write the container to the specified file.
        HecTimeSeries hts = new HecTimeSeries();
        // Set the data units
        // TODO QUESTION FOR BILL CHARLEY - is it required to use HEC-DSS units or can any string be used?
        hts.setUnits(ts.getDataUnits());
        // Set the data type
        // TODO QUESTION FOR BILL CHARLEY - what is the purpose of "type"?  In the HecDssTimeSeriesExample.java code
        // the type is different from the data type used in the path.  How is the string from setType() ever
        // used?  Please correct me if using these time series methods is correct given that you have instructed
        // me to use the TimeSeriesContainer for other things.
        hts.setType(ts.getDataType());
        hts.setPathname(tsidToHecPathName(ts.getIdentifier()).toString());
        hts.setDSSFileName(outputFile.getCanonicalPath());
        hts.write(tsc);
    }
}

}