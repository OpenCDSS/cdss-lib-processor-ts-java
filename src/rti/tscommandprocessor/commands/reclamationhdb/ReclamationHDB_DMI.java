package rti.tscommandprocessor.commands.reclamationhdb;

import java.security.InvalidParameterException;
import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import oracle.jdbc.OracleConnection;
import rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib.JavaConnections;
import RTi.DMI.DMI;
import RTi.DMI.DMIUtil;
import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSEnsemble;
import RTi.TS.TSIdent;
import RTi.TS.TSIterator;
import RTi.TS.TSUtil;
import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.StopWatch;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;
import RTi.Util.Time.TimeZoneDefaultType;

// TODO SAM 2010-10-25 Evaluate updating code to be more general (e.g., more completely use DMI base class)
/**
Data Management Interface (DMI) for the Reclamation HDB database.  Low-level code to interact with the
database is mostly provided by code from Reclamation and any new code generally follows the design of that code.
*/
public class ReclamationHDB_DMI extends DMI
{
    
/**
Connection to the database.
*/
private JavaConnections __hdbConnection = null;

/**
Database parameters from REF_DB_PARAMETER.
*/
private Hashtable<String, String> __databaseParameterList = new Hashtable();

/**
Agencies from HDB_AGEN.
*/
private List<ReclamationHDB_Agency> __agencyList = new ArrayList<ReclamationHDB_Agency>();

/**
Data types from HDB_DATATYPE.
*/
private List<ReclamationHDB_DataType> __dataTypeList = new ArrayList<ReclamationHDB_DataType>();

/**
Keep alive SQL string and frequency.  If specified in configuration file and set with
setKeepAlive(), will be used to run a thread and query the database to keep the connection open.
TODO SAM 2015-03-23 This feature is not needed now that database re-connection is enabled in TSTool 11.00.00 - remove once confirmed.
*/
//private String __keepAliveSql = null;
//private String __keepAliveFrequency = null;

/**
The maximum number of insert statements to execute in a batch.
See: http://docs.oracle.com/cd/E11882_01/timesten.112/e21638/tuning.htm
The value is set in the datastore factory - changing here has no effect
*/
private int __writeToHdbInsertStatementMaxDefault = 256; // 10000
private int __writeToHdbInsertStatementMax = __writeToHdbInsertStatementMaxDefault;

/**
The result set fetch size.  Oracle defaults to 10 which results in slow performance.
*/
private int __resultSetFetchSizeDefault = 10000;
private int __resultSetFetchSize = __resultSetFetchSizeDefault;

/**
Timeout for database statements.
*/
private int __readTimeout = -1;

/**
Indicate whether newly created TSIDs (e.g., in the TSTool main GUI) should use common names or SDI and MDI.
*/
private boolean __tsidStyleSDI = true; // Default

/**
Indicate whether when reading NHour data the end date time can be used for the TSTool date/time.
True corresponds to a datastore property ReadNHourEndDateTime=EndDateTime
False corresponds to a datastore property ReadNHourEndDateTime=StartDateTimePlusInterval
*/
private boolean __readNHourEndDateTime = false; // Default, because WRITE_TO_HDB end_date_time does not currently work for NHour

/**
Loading applications from HDB_LOADING_APPLICATION.
*/
private List<ReclamationHDB_LoadingApplication> __loadingApplicationList = new ArrayList<ReclamationHDB_LoadingApplication>();

/**
Models from HDB_MODEL.
*/
private List<ReclamationHDB_Model> __modelList = new ArrayList<ReclamationHDB_Model>();

/**
Object types from HDB_OBJECTTYPE.
*/
private List<ReclamationHDB_ObjectType> __objectTypeList = new ArrayList<ReclamationHDB_ObjectType>();

/**
Overwrite flags from HDB_OVERWRITE_FLAG.
*/
private List<ReclamationHDB_OverwriteFlag> __overwriteFlagList = new ArrayList<ReclamationHDB_OverwriteFlag>();

/**
Time zones supported when writing time series.
*/
private List<String> __timeZoneList = new ArrayList<String>();

/**
Loading applications from HDB_VALIDATION.
*/
private List<ReclamationHDB_Validation> __validationList = new ArrayList<ReclamationHDB_Validation>();

/**
Indicate whether the database has ensemble tables, which will be true if the table REF_ENSEMBLE is present.
*/
private boolean __dbHasEnsembles = false;

/** 
Constructor for a database server and database name, to use an automatically created URL.
Because Dave King's DMI code is used for low-level database work, this DMI is just a wrapper
to his JavaConnections class.
@param databaseEngine The database engine to use (see the DMI constructor), will default to SQLServer.
@param databaseServer The IP address or DSN-resolvable database server machine name.
@param databaseName The database name on the server.
@param port Port number used by the database.  If <= 0, default to that for the database engine.
@param systemLogin If not null, this is used as the system login to make the
connection.  If null, the default system login is used.
@param systemPassword If not null, this is used as the system password to make
the connection.  If null, the default system password is used.
*/
public ReclamationHDB_DMI ( String databaseEngine, String databaseServer,
    String databaseName, int port, String systemLogin, String systemPassword )
throws Exception {
    // The engine is used in the base class so it needs to be non-null in the following call if not specified
    super ( (databaseEngine == null) ? "Oracle" : databaseEngine,
        databaseServer, databaseName, port, systemLogin, systemPassword );
    setEditable(true);
    setSecure(true);
}

/**
Convert the start/end date/time values from an HDB time series table to a single date/time used internally
with time series.
@param startDateTime the START_DATE_TIME value from an HDB time series table
@param endDateTime the END_DATE_TIME value from an HDB time series table
@param intervalBase the TimeInterval interval base for the data
@param intervalMult the TimeInterval multiplier for the data
@param dateTime if null, create a DateTime and return; if not null, reuse the instance
@param timeZone the string time zone (e.g., MST for HDB time series)
*/
private DateTime convertHDBDateTimesToInternal ( DateTime startDateTime, DateTime endDateTime,
        int intervalBase, int intervalMult, DateTime dateTime, String timeZone )
{   if ( dateTime == null ) {
        // Create a new instance with precision that matches the interval and HDB time zone.
        dateTime = new DateTime(intervalBase);
        dateTime.setTimeZone ( timeZone ); // If time zone is not set will default to internal, usually local time
    }
    if ( (intervalBase == TimeInterval.HOUR) && (intervalMult != 1) ) {
        // NHour data - only case where a shift from the HDB start_date_time to the TSTool recording time is needed
        // Need to have the hour shifted by N hour because start date passed as SAMPLE_DATE_TIME is start of interval.
        // Can't rely on end time to be correct because WRITE_TO_HDB does not seem to set the end date time.
        if ( getReadNHourEndDateTime() ) {
            // Just use the end time
            dateTime.setDate(endDateTime);
        }
        else {
            // Calculate the end time as the start time plus the interval
            startDateTime.addHour(intervalMult);
            dateTime.setDate(startDateTime);
        }
    }
    else if ( (intervalBase == TimeInterval.HOUR) || (intervalBase == TimeInterval.IRREGULAR) ) {
        // The ending date/time has the hour of interest
        dateTime.setDate(endDateTime);
    }
    else if ( (intervalBase == TimeInterval.DAY) || (intervalBase == TimeInterval.MONTH) ||
        (intervalBase == TimeInterval.YEAR) ) {
        // The starting date/time has the date of interest
        dateTime.setDate(startDateTime);
    }
    return dateTime;
}

/**
Convert a start date/time for an entire time series to an internal date/time suitable
for the time series period start/end.
@param startDateTime min/max start date/time from time series data records as per HDB conventions
@param intervalBase time series interval base
@param intervalMult time series interval multiplier
@param timeZone time zone abbreviation (e.g., "MST" to use for hourly and instantaneous data)
@return internal date/time that can be used to set the time series start/end, for memory allocation
*/
private DateTime convertHDBStartDateTimeToInternal ( Date startDateTime, int intervalBase, int intervalMult, String timeZone )
{   DateTime dateTime = new DateTime(startDateTime);
    if ( intervalBase == TimeInterval.HOUR ) {
        // The date/time using internal conventions is N-hour later
        // FIXME SAM 2010-11-01 Are there any instantaneous 1hour values?
        dateTime.addHour(intervalMult);
        dateTime.setTimeZone(timeZone);
    }
    else if ( intervalBase == TimeInterval.IRREGULAR ) {
        dateTime.setPrecision(DateTime.PRECISION_MINUTE);
        dateTime.setTimeZone(timeZone);
    }
    // Otherwise for DAY, MONTH, YEAR the starting date/time is correct when precision is considered
    return dateTime;
}

/**
Convert an internal date/time to an HDB start date/time suitable to limit the query of time series records.
Strip off the time zone if specified.
@param startDateTime min/max start date/time from time series data records as per HDB conventions
@param intervalBase time series interval base
@param intervalMult time series interval multiplier
@return internal date/time that can be used to set the time series start/end, for memory allocation
*/
private String convertInternalDateTimeToHDBStartString ( DateTime dateTime, int intervalBase, int intervalMult )
{   // Protect the original value by making a copy...
    // TODO SAM 2013-04-14 for some reason copying loses the time zone.
    DateTime dateTime2 = new DateTime(dateTime);
    // Set time zone to blank because don't want it in the query string
    dateTime2.setTimeZone("");
    if ( intervalBase == TimeInterval.HOUR ) {
        // The date/time using internal conventions is Nhour later
        // FIXME SAM 2010-11-01 Are there any instantaneous 1hour values?
        dateTime2.addHour(-intervalMult);
        return dateTime2.toString();
    }
    else if ( intervalBase == TimeInterval.MONTH ) {
        return dateTime2.toString() + "-01"; // Need extra string as per notes in getOracleDateFormat()
    }
    else if ( intervalBase == TimeInterval.YEAR ) {
        return dateTime2.toString() + "-01-01"; // Need extra string as per notes in getOracleDateFormat()
    }
    else {
        // Otherwise for DAY, MONTH, YEAR the starting date/time is correct when precision is considered
        return dateTime2.toString();
    }
}

/**
Convert a TimeInterval interval to a ReclamationHDB interval for use with read code.
*/
private String convertTimeIntervalToReclamationHDBInterval(String interval)
{
    if ( interval.equalsIgnoreCase("year") ) {
        return "1YEAR";
    }
    else if ( interval.equalsIgnoreCase("month") ) {
        return "1MONTH";
    }
    else if ( interval.equalsIgnoreCase("day") ) {
        return "1DAY";
    }
    else if ( interval.equalsIgnoreCase("hour") ) {
        return "1HOUR";
    }
    else if ( interval.toUpperCase().indexOf("IRR") >= 0 ) {
        return "INSTANT";
    }
    else {
        throw new InvalidParameterException ( "Interval \"" + interval +
            "\" cannot be converted to Reclamation HDB interval." );
    }
}

/**
Determine the database version.
*/
public void determineDatabaseVersion()
{
    // TODO SAM 2010-10-18 Need to enable
}

/**
Find an instance of ReclamationHDB_Ensemble given the ensemble name.
@return the list of matching items (a non-null list is guaranteed)
@param ensembleList a list of ReclamationHDB_Ensemble to search
@param ensembleName the ensemble name to match (case-insensitive)
*/
public List<ReclamationHDB_Ensemble> findEnsemble( List<ReclamationHDB_Ensemble> ensembleList, String ensembleName )
{
    List<ReclamationHDB_Ensemble> foundList = new ArrayList<ReclamationHDB_Ensemble>();
    for ( ReclamationHDB_Ensemble ensemble: ensembleList ) {
        if ( (ensembleName != null) && !ensemble.getEnsembleName().equalsIgnoreCase(ensembleName) ) {
            // Ensemble name to match was specified but did not match
            continue;
        }
        // If here OK to add to the list.
        foundList.add ( ensemble );
    }
    return foundList;
}

/**
Find an instance of ReclamationHDB_LoadingApplication given the application name.
@return the list of matching items (a non-null list is guaranteed)
@param loadingApplicationList a list of ReclamationHDB_Model to search
@param  loadingApplication the model name to match (case-insensitive)
*/
public List<ReclamationHDB_LoadingApplication> findLoadingApplication (
    List<ReclamationHDB_LoadingApplication> loadingApplicationList, String loadingApplication )
{
    List<ReclamationHDB_LoadingApplication> foundList = new ArrayList<ReclamationHDB_LoadingApplication>();
    for ( ReclamationHDB_LoadingApplication la: loadingApplicationList ) {
        if ( (loadingApplication != null) && !la.getLoadingApplicationName().equalsIgnoreCase(loadingApplication) ) {
            // Application name to match was specified but did not match
            continue;
        }
        // If here OK to add to the list.
        foundList.add ( la );
    }
    return foundList;
}

/**
Find an instance of ReclamationHDB_Model given the model name.
@return the list of matching items (a non-null list is guaranteed)
@param modelList a list of ReclamationHDB_Model to search
@param  modelName the model name to match (case-insensitive)
*/
public List<ReclamationHDB_Model> findModel( List<ReclamationHDB_Model> modelList, String modelName )
{
    List<ReclamationHDB_Model> foundList = new ArrayList<ReclamationHDB_Model>();
    for ( ReclamationHDB_Model model: modelList ) {
        if ( (modelName != null) && !model.getModelName().equalsIgnoreCase(modelName) ) {
            // Model name to match was specified but did not match
            continue;
        }
        // If here OK to add to the list.
        foundList.add ( model );
    }
    return foundList;
}

/**
Find an instance of ReclamationHDB_ModelRun given information about the model run.
Any of the search criteria can be null or blank.
@return the list of matching items (a non-null list is guaranteed)
@param modelRunList a list of ReclamationHDB_ModelRun to search
@param modelID the model identifier
@param modelRunName the model run name
@param modelRunDate the model run date in form "YYYY-MM-DD hh:mm"
@param hydrologicIndicator the model run hydrologic indicator
*/
public List<ReclamationHDB_ModelRun> findModelRun( List<ReclamationHDB_ModelRun> modelRunList,
    int modelID, String modelRunName, String modelRunDate, String hydrologicIndicator )
{
    List<ReclamationHDB_ModelRun> foundList = new ArrayList<ReclamationHDB_ModelRun>();
    //Message.printStatus(2, "", "Have " + modelRunList.size() + " model runs to check" );
    for ( ReclamationHDB_ModelRun modelRun: modelRunList ) {
        if ( (modelID >= 0) && (modelRun.getModelID() != modelID) ) {
            // Model name to match was specified but did not match
            continue;
        }
        if ( (modelRunName != null) && !modelRunName.equals("") &&
            !modelRun.getModelRunName().equalsIgnoreCase(modelRunName) ) {
            // Model run name to match was specified but did not match
            continue;
        }
        // Model run date is compared to the minute.
        if ( (modelRunDate != null) && !modelRunDate.equals("") ) {
            DateTime dt = new DateTime(modelRun.getRunDate(),DateTime.PRECISION_MINUTE);
            if ( !dt.toString().equalsIgnoreCase(modelRunDate) ) {
                // Model run date to match was specified but did not match
                continue;
            }
        }
        //Message.printStatus(2, "", "Checking data hydrologic indicator \"" + modelRun.getHydrologicIndicator() +
        //    "\" with filter \"" + hydrologicIndicator + "\" for run date " + new DateTime(modelRun.getRunDate()) + 
        //    " model run ID=" + modelRun.getModelRunID());
        if ( (hydrologicIndicator != null) &&
            !modelRun.getHydrologicIndicator().equalsIgnoreCase(hydrologicIndicator) ) {
            // Allow filter to be an empty string since database can have blanks/nulls
            // Hydrologic indicator (can be a blank string) to match was specified but did not match
            //Message.printStatus(2, "", "Not match" );
            continue;
        }
        // If here OK to add to the list.
        //Message.printStatus(2, "", "Found match for run date " + new DateTime(modelRun.getRunDate()) );
        foundList.add ( modelRun );
    }
    return foundList;
}

/**
Find an instance of ReclamationHDB_SiteDataType given the site common name and data type common name.
@return the list of matching items (a non-null list is guaranteed).
*/
public List<ReclamationHDB_SiteDataType> findSiteDataType( List<ReclamationHDB_SiteDataType> siteDataTypeList,
    String siteCommonName, String dataTypeCommonName )
{
    List<ReclamationHDB_SiteDataType> foundList = new ArrayList<ReclamationHDB_SiteDataType>();
    for ( ReclamationHDB_SiteDataType siteDataType: siteDataTypeList ) {
        if ( (siteCommonName != null) && !siteDataType.getSiteCommonName().equalsIgnoreCase(siteCommonName) ) {
            // Site common name to match was specified but did not match
            continue;
        }
        if ( (dataTypeCommonName != null) && !siteDataType.getDataTypeCommonName().equalsIgnoreCase(dataTypeCommonName) ) {
            // Data type common name to match was specified but did not match
            continue;
        }
        // If here OK to add to the list.
        foundList.add ( siteDataType );
    }
    return foundList;
}

/**
Return the list of agencies (global data initialized when database connection is opened).
@return the list of agencies 
*/
public List<ReclamationHDB_Agency> getAgencyList ()
{
    return __agencyList;
}

/**
Indicate whether the database supports ensembles in the design.
*/
public boolean getDatabaseHasEnsembles ()
{
    return __dbHasEnsembles;
}

/**
Return the global time zone that is used for time series values more precise than daily.
@return the global time zone that is used for time series values more precise than daily.  An empty
string is returned if the time zone is not available.
*/
public String getDatabaseTimeZone ()
{
    String tz = __databaseParameterList.get("TIME_ZONE");
    if ( tz == null ) {
        tz = "";
    }
    return tz;
}

/**
Return the HDB data type list (global data initialized when database connection is opened).
@return the list of data types 
*/
public List<ReclamationHDB_DataType> getDataTypeList ()
{
    return __dataTypeList;
}

/**
Return the list of loading applications.
*/
private List<ReclamationHDB_LoadingApplication> getLoadingApplicationList ()
{
    return __loadingApplicationList;
}

/**
Get the "Object name - data type" strings to use in time series data type selections.
@param includeObjectTypeName if true, include the object type name before the data type; if false, just return the data type.
*/
public List<String> getObjectDataTypes ( boolean includeObjectTypeName )
throws SQLException
{   String routine = getClass().getName() + ".getObjectDataTypes";
    ResultSet rs = null;
    Statement stmt = null;
    String sqlCommand = "select distinct HDB_OBJECTTYPE.OBJECTTYPE_NAME, HDB_DATATYPE.DATATYPE_COMMON_NAME" +
        " from HDB_DATATYPE, HDB_OBJECTTYPE, HDB_SITE, HDB_SITE_DATATYPE" +
        " where HDB_SITE.OBJECTTYPE_ID = HDB_OBJECTTYPE.OBJECTTYPE_ID and" +
        " HDB_SITE.SITE_ID = HDB_SITE_DATATYPE.SITE_ID and" +
        " HDB_DATATYPE.DATATYPE_ID = HDB_SITE_DATATYPE.DATATYPE_ID" +
        " order by HDB_OBJECTTYPE.OBJECTTYPE_NAME, HDB_DATATYPE.DATATYPE_COMMON_NAME";
    List<String> types = new ArrayList<String>();
    
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        if ( rs == null ) {
            Message.printWarning(3, routine, "Resultset is null.");
        }
        String objectType = null, dataType = null;
        while (rs.next()) {
            objectType = rs.getString(1);
            if ( rs.wasNull() ) {
                objectType = "";
            }
            dataType = rs.getString(2);
            if ( rs.wasNull() ) {
                dataType = "";
            }
            if ( includeObjectTypeName ) {
                types.add ( objectType + " - " + dataType );
            }
            else {
                types.add ( dataType );
            }
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting object/data types from HDB (" + e + ")." );
        Message.printWarning(3, routine, "State:" + e.getSQLState() );
        Message.printWarning(3, routine, "ErrorCode:" + e.getErrorCode() );
        Message.printWarning(3, routine, "Message:" + e.getMessage() );
        Message.printWarning(3, routine, e );
    }
    finally {
        rs.close();
        stmt.close();
    }
    
    // Return the object data type list
    return types;
}

// TODO SAM 2010-11-02 Figure out if this can be put in DMIUtil, etc.
/**
Get the Oracle date/time format string given the data interval.
See http://www.techonthenet.com/oracle/functions/to_date.php
Not mentioned, that if the date format does not include enough formatting, unexpected defaults
may be used.  For example, formatting only the year with "YYYY" uses a default month of the
current month, see:  https://forums.oracle.com/forums/thread.jspa?threadID=854498
Consequently, a YYYY string being formatted must have 01-01 already appended.  See
convertInternalDateTimeToHDBStartString().
@param a TimeInterval base interval
@return the Oracle string for the to_date() SQL function (e.g., "YYYY-MM-DD HH24:MI:SS")
*/
private String getOracleDateFormat ( int intervalBase )
{   // Oracle format output by to_date is like: 2000-04-01 00:00:00.0
    if ( intervalBase == TimeInterval.HOUR ) {
        return "YYYY-MM-DD HH24";
    }
    else if ( intervalBase == TimeInterval.DAY ) {
        return "YYYY-MM-DD";
    }
    else if ( intervalBase == TimeInterval.MONTH ) {
        return "YYYY-MM-DD";
    }
    else if ( intervalBase == TimeInterval.YEAR ) {
        return "YYYY-MM-DD";
    }
    else if ( intervalBase == TimeInterval.IRREGULAR ) {
        // Use minute since that seems to be what instantaneous data are
        return "YYYY-MM-DD HH24:MI";
    }
    else {
        throw new InvalidParameterException("Time interval " + intervalBase +
            " is not recognized - can't get Oracle date/time format.");
    }
}

/**
Return the list of global overwrite flags.
*/
public List<ReclamationHDB_OverwriteFlag> getOverwriteFlagList()
{
    return __overwriteFlagList;
}

/**
Indicate whether the date/time for data when reading NHour should just be the *_HOUR.END_DATE_TIME.
*/
public boolean getReadNHourEndDateTime ()
{
    return __readNHourEndDateTime;
}

/**
Get the sample interval for the write_to_hdb stored procedure, given the time series base interval.
@param intervalBase the time series base interval
@return the HDB sample interval string
*/
private String getSampleIntervalFromInterval ( int intervalBase )
{
    String sampleInterval = null;
    if ( intervalBase == TimeInterval.HOUR ) {
        sampleInterval = "hour";
    }
    else if ( intervalBase == TimeInterval.DAY ) {
        sampleInterval = "day";
    }
    else if ( intervalBase == TimeInterval.MONTH ) {
        sampleInterval = "month";
    }
    else if ( intervalBase == TimeInterval.YEAR ) {
        sampleInterval = "year";
    }
    else if ( intervalBase == TimeInterval.IRREGULAR ) {
        sampleInterval = "instant";
    }
    // TODO SAM 2012-03-28 "wy" is not handled
    else {
        throw new InvalidParameterException("Interval \"" + intervalBase + "\" is not supported." );
    }
    return sampleInterval;
}

/**
Get the time series data table name based on the data interval.
@param interval the data interval "hour", etc.
@param isReal true if the data are to be extracted from the real data tables.
@return the table name to use for time series data.
*/
private String getTimeSeriesTableFromInterval ( String interval, boolean isReal )
{
    String prefix = "R_";
    if ( !isReal ) {
        prefix = "M_";
    }
    if ( interval.toUpperCase().indexOf("HOUR") >= 0 ) {
        // Allow NHour time series
        return prefix + "HOUR";
    }
    else if ( interval.equalsIgnoreCase("day") || interval.equalsIgnoreCase("1day")) {
        return prefix + "DAY";
    }
    else if ( interval.equalsIgnoreCase("month") || interval.equalsIgnoreCase("1month")) {
        return prefix + "MONTH";
    }
    else if ( interval.equalsIgnoreCase("year") || interval.equalsIgnoreCase("1year")) {
        return prefix + "YEAR";
    }
    else if ( interval.toUpperCase().indexOf("IRR") >= 0 ) {
        if ( isReal ) {
            return prefix + "INSTANT";
        }
        else {
            throw new InvalidParameterException("Interval \"" + interval + "\" is not supported for model data." );
        }
    }
    else {
        throw new InvalidParameterException("Interval \"" + interval + "\" is not supported." );
    }
}

/**
Create a list of where clauses give an InputFilter_JPanel.  The InputFilter
instances that are managed by the InputFilter_JPanel must have been defined with
the database table and field names in the internal (non-label) data.
@return a list of where clauses, each of which can be added to a DMI statement.
@param dmi The DMI instance being used, which may be checked for specific formatting.
@param panel The InputFilter_JPanel instance to be converted.  If null, an empty list will be returned.
*/
private List<String> getWhereClausesFromInputFilter ( DMI dmi, InputFilter_JPanel panel ) 
{
    // Loop through each filter group.  There will be one where clause per filter group.

    if (panel == null) {
        return new ArrayList<String>();
    }

    int nfg = panel.getNumFilterGroups ();
    InputFilter filter;
    List<String> where_clauses = new ArrayList<String>();
    String where_clause=""; // A where clause that is being formed.
    for ( int ifg = 0; ifg < nfg; ifg++ ) {
        filter = panel.getInputFilter ( ifg );  
        where_clause = DMIUtil.getWhereClauseFromInputFilter(dmi, filter,panel.getOperator(ifg), true);
        if (where_clause != null) {
            where_clauses.add(where_clause);
        }
    }
    return where_clauses;
}

/**
Create a where string given an InputFilter_JPanel.  The InputFilter
instances that are managed by the InputFilter_JPanel must have been defined with
the database table and field names in the internal (non-label) data.
@return a list of where clauses as a string, each of which can be added to a DMI statement.
@param dmi The DMI instance being used, which may be checked for specific formatting.
@param panel The InputFilter_JPanel instance to be converted.  If null, an empty list will be returned.
@param tableName the name of the table for which to get where clauses.  This will be the leading XXXX. of
the matching strings.
@param useAnd if true, then "and" is used instead of "where" in the where strings.  The former can be used
with "join on" SQL syntax.
@param addNewline if true, add a newline if the string is non-blank - this simply helps with formatting of
the big SQL, so that logging has reasonable line breaks
*/
private String getWhereClauseStringFromInputFilter ( DMI dmi, InputFilter_JPanel panel, String tableName,
   boolean addNewline )
{
    List<String> whereClauses = getWhereClausesFromInputFilter ( dmi, panel );
    StringBuffer whereString = new StringBuffer();
    String tableNameDot = (tableName + ".").toUpperCase();
    for ( String whereClause : whereClauses ) {
        // TODO SAM 2011-09-30 If the new code works then remove the following old code
        //if ( !whereClause.toUpperCase().startsWith(tableNameDot) ) {
        //Message.printStatus(2, "", "Checking where clause \"" + whereClause + "\" against \"" + tableNameDot + "\"" );
        if ( whereClause.toUpperCase().indexOf(tableNameDot) < 0 ) {
            // Not for the requested table so don't include the where clause
            continue;
        }
        if ( (whereString.length() > 0)  ) {
            // Need to concatenate
            whereString.append ( " and ");
        }
        whereString.append ( "(" + whereClause + ")");
    }
    if ( addNewline && (whereString.length() > 0) ) {
        whereString.append("\n");
    }
    return whereString.toString();
}

/**
Return the list of global validation flags.
*/
public List<ReclamationHDB_Validation> getHdbValidationList()
{
    return __validationList;
}

/**
Return the list of object types.
*/
public List<ReclamationHDB_ObjectType> getObjectTypeList()
{
    return __objectTypeList;
}

/**
Return the list of supported time zones.
*/
public List<String> getTimeZoneList()
{
    return __timeZoneList;
}

/**
Return whether the TSID format should match SDI syntax or old common name syntax.
*/
public boolean getTSIDStyleSDI ( )
{
    return __tsidStyleSDI;
}

/**
Lookup the ReclamationHDB_Agency given the internal agency ID.
@return the matching agency object, or null if not found
@param agencyList a list of ReclamationHDB_Agency to search
@param agencyID the agency ID to match
*/
public ReclamationHDB_Agency lookupAgency ( List<ReclamationHDB_Agency> agencyList, int agencyID )
{
    for ( ReclamationHDB_Agency a: agencyList ) {
        if ( (a != null) && (a.getAgenID() == agencyID) ) {
            return a;
        }
    }
    return null;
}

/**
Lookup the ReclamationHDB_Agency given the internal agency ID.
@return the matching agency object, or null if not found
@param agencyList a list of ReclamationHDB_Agency to search
@param agenAbbrev the agency abbreviation (case-insensitive)
*/
public ReclamationHDB_Agency lookupAgency ( List<ReclamationHDB_Agency> agencyList, String agenAbbrev )
{
    for ( ReclamationHDB_Agency a: agencyList ) {
        if ( (a != null) && (a.getAgenAbbrev() != null) && a.getAgenAbbrev().equalsIgnoreCase(agenAbbrev) ) {
            return a;
        }
    }
    return null;
}

/**
Lookup the ReclamationHDB_DataType given the data type ID.
@return the matching data type object, or null if not found
@param dataTypeID the data type ID to match
*/
public ReclamationHDB_DataType lookupDataType ( int dataTypeID )
{
    for ( ReclamationHDB_DataType dt: __dataTypeList ) {
        if ( (dt != null) && (dt.getDataTypeID() == dataTypeID) ) {
            return dt;
        }
    }
    return null;
}

/**
Lookup the ReclamationHDB_Model given the data type ID.
@return the matching data type object, or null if not found
@param dataTypeID the data type ID to match
*/
public ReclamationHDB_Model lookupModel ( int modelID )
{
    for ( ReclamationHDB_Model m: __modelList ) {
        if ( (m != null) && (m.getModelID() == modelID) ) {
            return m;
        }
    }
    return null;
}

/**
Lookup the ReclamationHDB_ObjectType given the object type ID.
@return the matching object type object, or null if not found
@param dataTypeID the data type ID to match
*/
public ReclamationHDB_ObjectType lookupObjectType ( int objectTypeID )
{
    for ( ReclamationHDB_ObjectType o: __objectTypeList ) {
        if ( (o != null) && (o.getObjectTypeID() == objectTypeID) ) {
            return o;
        }
    }
    return null;
}

/**
Lookup the ReclamationHDB_Site given the site ID.
@return the matching site object, or null if not found
@param siteList a list of ReclamationHDB_Siteto search
@param siteID the site ID to match
*/
public ReclamationHDB_Site lookupSite ( List<ReclamationHDB_Site> siteList, int siteID )
{
    for ( ReclamationHDB_Site site: siteList ) {
        if ( (site != null) && (site.getSiteID() == siteID) ) {
            return site;
        }
    }
    return null;
}

/**
Open the database connection.
*/
@Override
public void open ()
{   String routine = getClass().getName() + ".open";
    // This will have been set in the constructor
    String databaseServer = getDatabaseServer();
    String databaseName = getDatabaseName();
    String systemLogin = getSystemLogin();
    String systemPassword = getSystemPassword();
    int port = getPort();
    if ( port < 0 ) {
        port = 1521;
    }
    // Use the reclamation connection object
    String sourceDBType = "OracleHDB";
    String sourceUrl = "jdbc:oracle:thin:@" + databaseServer + ":" + port + ":" + databaseName;
    String sourceUserName = systemLogin;
    String sourcePassword = systemPassword;
    __hdbConnection = new JavaConnections(sourceDBType, sourceUrl, sourceUserName, sourcePassword );
    // Set the connection in the base class so it can be used with utility code
    setConnection ( __hdbConnection.ourConn );
    Message.printStatus(2, routine, "Opened the database connection." );
    readGlobalData();
    // Start a "keep alive" thread to make sure the database connection is not lost
    //startKeepAliveThread();
}

/**
Open the database with the specified login information.
@param systemLogin the service account login
@param systemPassword the service account password
*/
@Override
public void open ( String systemLogin, String systemPassword )
{
    setSystemLogin ( systemLogin );
    setSystemPassword ( systemPassword );
    open ();
}

/**
Read an HDB ensemble given the ensemble name.
@param sdi site data type ID corresponding to the ensemble
@param ensembleID unique ensemble ID
@param ensembleName unique ensemble name
@param interval time interval for ensemble
@param readStart starting date/time for read
@param readEnd ending date/time for read
@param nHourIntervalOffset if >= 0 indicates the hour for the first valid value,
to allow for cases where the data do not align with hour zero (needed if a previous data load caused
data to be loaded at more hours than appropriate)
@param readData if true read the data; if false only read time series metadata
*/
public TSEnsemble readEnsemble ( int sdi, String ensembleID, String ensembleName, TimeInterval interval,
    DateTime readStart, DateTime readEnd, int nHourIntervalOffset, boolean readData )
throws Exception
{
    // First read the ensemble object(s) from the HDB database
    List<ReclamationHDB_Ensemble> ensembleList = readRefEnsembleList(ensembleName,null,-1);
    if ( ensembleList.size() != 1 ) {
        throw new RuntimeException ( "Expecting exactly one ensemble from HDB.  Got " + ensembleList.size() );
    }
    ReclamationHDB_Ensemble hensemble = ensembleList.get(0);
    // Get the list of traces that match the ensemble
    List<ReclamationHDB_EnsembleTrace> ensembleTraceList = readRefEnsembleTraceList(hensemble.getEnsembleID(),-1,-1,null);
    // Loop through the traces and read the model time series
    List<TS> tslist = new ArrayList<TS>();
    int itrace = -1;
    int modelIDSave = -1;
    Date runDateSave = null;
    List<ReclamationHDB_EnsembleTrace> missingTraceList = new ArrayList<ReclamationHDB_EnsembleTrace>();
    String hydrologicIndicator = null;
    for ( ReclamationHDB_EnsembleTrace trace: ensembleTraceList ) {
        // Read the model run for the trace and confirm that the model run and run_date are the same for all traces
        ++itrace;
        List<Integer> mriList = new ArrayList<Integer>(1);
        mriList.add ( new Integer(trace.getModelRunID()));
        List<ReclamationHDB_ModelRun> modelRunList = readHdbModelRunList(-1, mriList, null, null, null);
        if ( modelRunList.size() != 1 ) {
            throw new RuntimeException ( "Read " + modelRunList.size() + " model runs for trace[" + itrace +
                "] and model run ID " + trace.getModelRunID() + ".  Expecting 1." );
        }
        if ( itrace == 0 ) {
            // First trace's model run
            modelIDSave = modelRunList.get(0).getModelID();
            runDateSave = modelRunList.get(0).getRunDate();
        }
        else {
            // Subsequent traces model run
            if ( modelRunList.get(0).getModelID() != modelIDSave ) {
                throw new RuntimeException ( "Trace [" + itrace + "] MODEL_ID (" + modelRunList.get(0).getModelID() +
                    ") is different from first trace MODEL_ID (" + modelIDSave + ")." );
            }
            if ( !("" + modelRunList.get(0).getRunDate()).equals("" + runDateSave) ) {
                throw new RuntimeException ( "Trace [" + itrace + "] RUN_DATE (" + modelRunList.get(0).getRunDate() +
                    ") is different from first trace RUN_DATE (" + runDateSave + ")." );
            }
        }
        // If here, OK to read the trace time series
        try {
            TS ts = readTimeSeries(sdi, mriList.get(0), true, interval, readStart, readEnd, nHourIntervalOffset, readData);
            // Set the sequence number for TSTool
            ts.setSequenceID("" + trace.getTraceNumeric() );
            // TODO SAM 2013-10-02 Can set to the trace name if appropriate in the future
            tslist.add(ts);
        }
        catch ( Exception e ) {
            // It is possible that an ensemble had no data for a trace when written.  In this case the metadata will not be
            // read because there are no records in the tables.  Keep track of these years and try to add a missing trace at
            // the end
            missingTraceList.add(trace);
        }
    }
    // If any traces were missing (likely due to completely missing data), find a non-missing trace, copy the time series,
    // and set to missing
    if ( missingTraceList.size() > 0 ) {
        // Find a leap and nonleapyear existing trace
        TS tsLeap = null;
        TS tsNonLeap = null;
        for ( TS ts: tslist ) {
            if ( TimeUtil.isLeapYear(Integer.parseInt(ts.getSequenceID())) ) {
                tsLeap = ts;
                break;
            }
        }
        for ( TS ts: tslist ) {
            if ( !TimeUtil.isLeapYear(Integer.parseInt(ts.getSequenceID())) ) {
                tsNonLeap = ts;
                break;
            }
        }
        TS ts = null;
        for ( ReclamationHDB_EnsembleTrace trace: missingTraceList ) {
            if ( TimeUtil.isLeapYear(trace.getTraceNumeric()) ) {
                ts = tsLeap;
            }
            else {
                ts = tsNonLeap;
            }
            if ( ts != null ) {
                TS tsCopy = (TS)ts.clone();
                // Existing time series will SDI-MRI, for the old MRI.  Replace with the new
                String loc = ts.getLocation().substring(0,ts.getLocation().indexOf("-"));
                tsCopy.getIdentifier().setLocation(loc + "-" + trace.getModelRunID());
                tsCopy.setSequenceID("" + trace.getTraceNumeric());
                if ( readData ) {
                    tsCopy.addToGenesis("Trace is being set as copy of another trace, with data set to missing, because HDB does not store empty time series.");
                    tsCopy.addToGenesis("Some internal properties for the time series may be inaccurate due to the copy.");
                    TSUtil.setConstant(tsCopy,tsCopy.getMissing());
                }
                tslist.add(tsCopy);
            }
        }
    }
    // Create a new ensemble
    TSEnsemble ensemble = new TSEnsemble(ensembleID,ensembleName,tslist);
    // Set ensemble properties
    // DMIUtil.isMissing(tsm.getLatitude()) ? null : new Double(tsm.getLatitude())
    ensemble.setProperty("ENSEMBLE_ID", new Integer(hensemble.getEnsembleID()) );
    ensemble.setProperty("ENSEMBLE_NAME", (hensemble.getEnsembleName() == null) ? "" : hensemble.getEnsembleName());
    return ensemble;
}

/**
Read global data for the database, to keep in memory and improve performance.
*/
@Override
public void readGlobalData()
{   String routine = getClass().getName() + ".readGlobalData";
    // Don't do a lot of caching at this point since database performance seems to be good
    // Do get the global database controlling parameters and other small reference table data

    // Agencies
    try {
        __agencyList = readHdbAgencyList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,e);
        Message.printWarning(3,routine,"Error reading agencies (" + e + ").");
    }
    // Database properties include database timezone for hourly date/times
    try {
        __databaseParameterList = readRefDbParameterList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,e);
        Message.printWarning(3,routine,"Error reading database parameters (" + e + ").");
    }
    // Data types
    try {
        __dataTypeList = readHdbDataTypeList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,e);
        Message.printWarning(3,routine,"Error reading data types (" + e + ").");
    }
    // Loading applications needed to convert "TSTool" to HDB identifier for writing data
    try {
        __loadingApplicationList = readHdbLoadingApplicationList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,e);
        Message.printWarning(3,routine,"Error reading loading applications (" + e + ").");
    }
    // Overwrite flags are used when writing time series
    try {
        __overwriteFlagList = readHdbOverwriteFlagList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,e);
        Message.printWarning(3,routine,"Error reading overwrite flags (" + e + ").");
    }
    // Validation flags are used when writing time series
    try {
        __validationList = readHdbValidationList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,e);
        Message.printWarning(3,routine,"Error reading validation flags (" + e + ").");
    }
    // Models, used when writing time series
    try {
        __modelList = readHdbModelList(null);
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,e);
        Message.printWarning(3,routine,"Error reading models (" + e + ").");
    }
    // Object types, used for location type in time series
    try {
        __objectTypeList = readHdbObjectTypeList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,e);
        Message.printWarning(3,routine,"Error reading object types (" + e + ").");
    }
    // Time zones...
    // As per email from Mark Bogner (2013-03-13):
    // They have to be the standard 3 character time zones (GMT,EST,MST,PST,MDT,CST,EDT...) are all valid
    // For stability, put the supported codes here
    // TODO SAM 2016-03-10 Standard time or GMT is the most robust because time zones won't impact data
    __timeZoneList = new ArrayList<String>();
    __timeZoneList.add ( "CDT" );
    __timeZoneList.add ( "CST" );
    __timeZoneList.add ( "EDT" );
    __timeZoneList.add ( "EST" );
    __timeZoneList.add ( "GMT" );
    __timeZoneList.add ( "MDT" );
    __timeZoneList.add ( "MST" );
    __timeZoneList.add ( "PDT" );
    __timeZoneList.add ( "PST" );
    // Save a flag indicating whether ensembles are in the database
    try {
        __dbHasEnsembles = true;
        /* Getting the metadata is a dog - don't do the following because it is really slow
        __dbHasEnsembles = false;
        DatabaseMetaData meta = getConnection().getMetaData();
        if ( DMIUtil.databaseHasTable(meta, "REF_ENSEMBLE") &&
            DMIUtil.databaseHasTable(meta, "REF_ENSEMBLE_TRACE") ) {
            __dbHasEnsembles = true;
        }
        */
    }
    catch ( Exception e ) {
        // For now nothing to do but assume no ensembles
        Message.printWarning(3, routine, e);
    }
}

/**
Read the HDB_AGEN table.
@return the list of agency data
*/
private List<ReclamationHDB_Agency> readHdbAgencyList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbAgencyList";
    List<ReclamationHDB_Agency> results = new ArrayList<ReclamationHDB_Agency>();
    String sqlCommand = "select HDB_AGEN.AGEN_ID, HDB_AGEN.AGEN_NAME, HDB_AGEN.AGEN_ABBREV from HDB_AGEN " +
        "order by HDB_AGEN.AGEN_NAME";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        String s;
        int record = 0;
        int col;
        ReclamationHDB_Agency data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_Agency();
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setAgenID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setAgenName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setAgenAbbrev(s);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting agency data from HDB \"" +
            getDatabaseName() + "\" (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        if ( stmt != null ) {
            stmt.close();
        }
    }
    
    return results;
}

/**
Read the database data types from the HDB_DATATYPE table.  Some column values will not be unique so output lists may need to be
additionally processed.
@return the list of data types
*/
public List<ReclamationHDB_DataType> readHdbDataTypeList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbDataTypeList";
    List<ReclamationHDB_DataType> results = new ArrayList<ReclamationHDB_DataType>();
    String sqlCommand = "select HDB_DATATYPE.DATATYPE_ID, " +
        "HDB_DATATYPE.DATATYPE_NAME, HDB_DATATYPE.DATATYPE_COMMON_NAME, " +
        "HDB_DATATYPE.PHYSICAL_QUANTITY_NAME, HDB_DATATYPE.UNIT_ID, HDB_DATATYPE.ALLOWABLE_INTERVALS, " +
        "HDB_DATATYPE.AGEN_ID, HDB_DATATYPE.CMMNT from HDB_DATATYPE";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        String s;
        int record = 0;
        int col;
        ReclamationHDB_DataType data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_DataType();
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setDataTypeID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setDataTypeName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setDataTypeCommonName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setPhysicalQuantityName(s);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setUnitID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setAllowableIntervals(s);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setAgenID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setCmmnt(s);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting data types from HDB \"" +
            getDatabaseName() + "\" (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read the database parameters from the HDB_LOADING_APPLICATION table.
@return the list of loading application data
*/
private List<ReclamationHDB_LoadingApplication> readHdbLoadingApplicationList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbLoadingApplication";
    List<ReclamationHDB_LoadingApplication> results = new ArrayList<ReclamationHDB_LoadingApplication>();
    String sqlCommand = "select HDB_LOADING_APPLICATION.LOADING_APPLICATION_ID, " +
    	"HDB_LOADING_APPLICATION.LOADING_APPLICATION_NAME, HDB_LOADING_APPLICATION.MANUAL_EDIT_APP, " +
    	"HDB_LOADING_APPLICATION.CMMNT from HDB_LOADING_APPLICATION";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        String s;
        int record = 0;
        int col;
        ReclamationHDB_LoadingApplication data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_LoadingApplication();
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setLoadingApplicationID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setLoadingApplicationName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setManualEditApp(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setCmmnt(s);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting loading application data from HDB \"" +
            getDatabaseName() + "\" (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read the database models from the HDB_MODEL table.
@param modelName model name to match (null or blank to ignore filter)
@return the list of models
*/
public List<ReclamationHDB_Model> readHdbModelList ( String modelName )
throws SQLException
{   String routine = getClass().getName() + ".readHdbModelList";
    List<ReclamationHDB_Model> results = new ArrayList<ReclamationHDB_Model>();
    StringBuilder sqlCommand = new StringBuilder("select HDB_MODEL.MODEL_ID, " +
        "HDB_MODEL.MODEL_NAME, HDB_MODEL.COORDINATED, " +
        "HDB_MODEL.CMMNT from HDB_MODEL");
    if ( (modelName != null) && !modelName.equals("") ) {
        sqlCommand.append ( " WHERE upper(HDB_MODEL.MODEL_NAME) = '" + modelName.toUpperCase() + "'" );
    }
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand.toString());
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        String s;
        int record = 0;
        int col;
        ReclamationHDB_Model data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_Model();
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setModelID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setModelName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setCoordinated(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setCmmnt(s);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting model data from HDB (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read the database model runs from the HDB_MODEL_RUN table.
@param modelID the model identifier to match, or -1 to ignore.
@param modelRunIDList list of model_run_id to filter the query (for example this may be from the time series tables)
@param modelRunName the model name to match, or null to ignore (blank is treated like null).
@param hydrologicIndicator the hydrologic indicator to match, or null to ignore (null will be matched).
@param runDate the run date to match, or null to ignore.
@return the list of model runs
*/
public List<ReclamationHDB_ModelRun> readHdbModelRunList ( int modelID, List<Integer> modelRunIDList,
    String modelRunName, String hydrologicIndicator, DateTime runDate )
throws SQLException
{   String routine = getClass().getName() + ".readHdbModelRunListForModelID";
    List<ReclamationHDB_ModelRun> results = new ArrayList<ReclamationHDB_ModelRun>();
    StringBuilder sqlCommand = new StringBuilder (
        "select REF_MODEL_RUN.MODEL_ID, REF_MODEL_RUN.MODEL_RUN_ID, REF_MODEL_RUN.MODEL_RUN_NAME," +
        "REF_MODEL_RUN.HYDROLOGIC_INDICATOR, REF_MODEL_RUN.RUN_DATE from REF_MODEL_RUN" );
    StringBuilder where = new StringBuilder();
    if ( modelID >= 0 ) {
        where.append ( " (REF_MODEL_RUN.MODEL_ID = " + modelID + ")");
    }
    if ( (modelRunIDList != null) && (modelRunIDList.size() > 0) ) {
        if ( where.length() > 0 ) {
            where.append ( " AND " );
        }
        where.append ( " (REF_MODEL_RUN.MODEL_RUN_ID IN (" );
        for ( int i = 0; i < modelRunIDList.size(); i++ ) {
            if ( i > 0 ) {
                where.append(",");
            }
            where.append("" + modelRunIDList.get(i));
        }
        where.append ( "))" );
    }
    if ( (modelRunName != null) && !modelRunName.equals("") ) {
        if ( where.length() > 0 ) {
            where.append ( " AND " );
        }
        where.append ( "(upper(REF_MODEL_RUN.MODEL_RUN_NAME) = '" + modelRunName.toUpperCase() + "')" );
    }
    if ( hydrologicIndicator != null ) {
        if ( where.length() > 0 ) {
            where.append ( " AND " );
        }
        if ( hydrologicIndicator.equals("") ) {
            where.append ( "(REF_MODEL_RUN.HYDROLOGIC_INDICATOR = '' OR REF_MODEL_RUN.HYDROLOGIC_INDICATOR is null)" );
        }
        else {
            where.append ( "(upper(REF_MODEL_RUN.HYDROLOGIC_INDICATOR) = '" + hydrologicIndicator.toUpperCase() + "')" );
        }
    }
    String runDateFormatted = null;
    if ( runDate != null ) {
        runDateFormatted = runDate.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm);
        // The run date is not used in the query but is checked below to handle formatting
    }
    if ( where.length() > 0 ) {
        // The keyword was not added above so add here
        where.insert(0, " WHERE ");
    }
    sqlCommand.append(where.toString());
    if ( (hydrologicIndicator != null) && !hydrologicIndicator.isEmpty() ) {
    	sqlCommand.append( " ORDER BY REF_MODEL_RUN.HYDROLOGIC_INDICATOR ASC" );
    }
    Message.printStatus(2,routine,"Reading model run list with SQL:  " + sqlCommand );
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand.toString());
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        String s;
        Date date;
        DateTime dt;
        int record = 0;
        int col;
        ReclamationHDB_ModelRun data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_ModelRun();
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setModelID(i);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setModelRunID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setModelRunName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setHydrologicIndicator(s);
            }
            date = rs.getTimestamp(col++);
            if ( !rs.wasNull() ) {
                data.setRunDate(date);
            }
            // Do the check on the run date here since formatting takes some care
            if ( runDate != null ) {
                if ( date == null ) {
                    continue;
                }
                dt = new DateTime(date);
                if ( !runDateFormatted.equals(dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm)) ) {
                    // Did not match so do not add
                    continue;
                }
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting model run data from HDB using SQL: " + sqlCommand );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read the list of model_run_id from a model data table given a site_datatype_id and data interval.
*/
public List<Integer> readHdbModelRunIDListForModelTable ( int siteDataTypeID, String interval )
throws SQLException
{   String routine = "ReclamationHDB_DMI.readHdbModelRunListForModelTable";
    List<Integer> results = new ArrayList<Integer>();
    String table = getTimeSeriesTableFromInterval(interval, false);
    
    String sqlCommand = "select distinct MODEL_RUN_ID from " + table + " where " + table + ".SITE_DATATYPE_ID = " + siteDataTypeID;
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        while (rs.next()) {
            i = rs.getInt(1);
            if ( !rs.wasNull() ) {
                results.add ( new Integer(i));
            }
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error distinct model_run_id from HDB " + table + " table (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    return results;
}

/**
Read the database parameters from the HDB_OBJECTTYPE table.
@return the list of object type data
*/
private List<ReclamationHDB_ObjectType> readHdbObjectTypeList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbObjectType";
    List<ReclamationHDB_ObjectType> results = new ArrayList<ReclamationHDB_ObjectType>();
    String sqlCommand = "select HDB_OBJECTTYPE.OBJECTTYPE_ID, " +
        "HDB_OBJECTTYPE.OBJECTTYPE_NAME, HDB_OBJECTTYPE.OBJECTTYPE_TAG, " +
        "HDB_OBJECTTYPE.OBJECTTYPE_PARENT_ORDER from HDB_OBJECTTYPE";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        String s;
        int record = 0;
        int col;
        ReclamationHDB_ObjectType data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_ObjectType();
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setObjectTypeID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setObjectTypeName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setObjectTypeTag(s);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setObjectTypeParentOrder(i);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting object type data from HDB \"" +
            getDatabaseName() + "\" (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read the data from the HDB_OVERWRITE_FLAG table.
@return the list of validation data
*/
private List<ReclamationHDB_OverwriteFlag> readHdbOverwriteFlagList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbOverwriteFlagList";
    List<ReclamationHDB_OverwriteFlag> results = new ArrayList<ReclamationHDB_OverwriteFlag>();
    String sqlCommand = "select HDB_OVERWRITE_FLAG.OVERWRITE_FLAG, " +
        "HDB_OVERWRITE_FLAG.OVERWRITE_FLAG_NAME, HDB_OVERWRITE_FLAG.CMMNT from HDB_OVERWRITE_FLAG";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        String s;
        int record = 0;
        int col;
        ReclamationHDB_OverwriteFlag data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_OverwriteFlag();
            col = 1;
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setOverwriteFlag(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setOverwriteFlagName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setCmmnt(s);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting overwrite flag data from HDB (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read the database site data types from the HDB_SITE_DATATYPE table, also joining to HDB_SITE and
HDB_DATATYPE to get the common names.
@return the list of site data types.
*/
public List<ReclamationHDB_SiteDataType> readHdbSiteDataTypeList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbSiteDataTypeList";
    List<ReclamationHDB_SiteDataType> results = new ArrayList<ReclamationHDB_SiteDataType>();
    String sqlCommand = "select HDB_SITE_DATATYPE.SITE_ID, HDB_SITE_DATATYPE.DATATYPE_ID, " +
        "HDB_SITE_DATATYPE.SITE_DATATYPE_ID, HDB_SITE.SITE_COMMON_NAME, HDB_DATATYPE.DATATYPE_COMMON_NAME " +
        "from HDB_SITE_DATATYPE, HDB_SITE, HDB_DATATYPE " +
        "where HDB_SITE_DATATYPE.SITE_ID = HDB_SITE.SITE_ID and " +
        "HDB_SITE_DATATYPE.DATATYPE_ID = HDB_DATATYPE.DATATYPE_ID";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        String s;
        int record = 0;
        int col;
        ReclamationHDB_SiteDataType data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_SiteDataType();
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setSiteID(i);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setDataTypeID(i);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setSiteDataTypeID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setSiteCommonName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setDataTypeCommonName(s);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting loading site data types from HDB \"" +
            getDatabaseName() + "\" (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

// TODO SAM 2010-12-10 Evaluate joins with reference tables - for now get raw data.
/**
Read the database sites from the HDB_SITE table.
Currently the main focus of this is to provide lists to TSTool commands.
@return the list of sites
*/
public List<ReclamationHDB_Site> readHdbSiteList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbSiteList";
    List<ReclamationHDB_Site> results = new ArrayList<ReclamationHDB_Site>();
    String sqlCommand = "select HDB_SITE.SITE_ID," +
    " HDB_SITE.SITE_NAME," +
    " HDB_SITE.SITE_COMMON_NAME,\n" +
    " HDB_SITE.OBJECTTYPE_ID,\n" +
    //" HDB_SITE.STATE_ID," + // Use the reference table string instead of numeric key
    //" HDB_STATE.STATE_CODE,\n" +
    //" HDB_SITE.BASIN_ID," + // Use the reference table string instead of numeric key
    //" HDB_BASIN.BASIN_CODE," +
    //" HDB_SITE.BASIN_ID," + // Change to above later when find basin info
    " HDB_SITE.LAT," +
    " HDB_SITE.LONGI," +
    " HDB_SITE.HYDROLOGIC_UNIT," +
    " HDB_SITE.SEGMENT_NO," +
    " HDB_SITE.RIVER_MILE," +
    " HDB_SITE.ELEVATION,\n" +
    " HDB_SITE.DESCRIPTION," +
    " HDB_SITE.NWS_CODE," +
    " HDB_SITE.SCS_ID," +
    " HDB_SITE.SHEF_CODE," +
    " HDB_SITE.USGS_ID," +
    " HDB_SITE.DB_SITE_CODE from HDB_SITE";
    ResultSet rs = null;
    Statement stmt = null;
    ReclamationHDB_ObjectType objectType;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        String s;
        float f;
        int record = 0;
        int col;
        ReclamationHDB_Site data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_Site();
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setSiteID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setSiteName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setSiteCommonName(s);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                objectType = lookupObjectType(i);
                if ( objectType != null ) {
                    data.setObjectTypeName(objectType.getObjectTypeName());
                }
            }
            // Latitude and longitude are varchars in the DB - convert to numbers if able
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                if ( StringUtil.isDouble(s) ) {
                    data.setLatitude(Double.parseDouble(s));
                }
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                if ( StringUtil.isDouble(s) ) {
                    data.setLongitude(Double.parseDouble(s));
                }
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setHuc(s);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setSegmentNo(i);
            }
            f = rs.getFloat(col++);
            if ( !rs.wasNull() ) {
                data.setRiverMile(f);
            }
            f = rs.getFloat(col++);
            if ( !rs.wasNull() ) {
                data.setElevation(f);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setDescription(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setNwsCode(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setScsID(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setShefCode(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setUsgsID(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setDbSiteCode(s);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting site data from HDB \"" +
            getDatabaseName() + "\" (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read the data from the HDB_VALIDATION table.
@return the list of validation data
*/
private List<ReclamationHDB_Validation> readHdbValidationList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbValidation";
    List<ReclamationHDB_Validation> results = new ArrayList<ReclamationHDB_Validation>();
    String sqlCommand = "select HDB_VALIDATION.VALIDATION, HDB_VALIDATION.CMMNT from HDB_VALIDATION";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        String s;
        int record = 0;
        int col;
        ReclamationHDB_Validation data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_Validation();
            col = 1;
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setValidation(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setCmmnt(s);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting validation data from HDB (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read the model run identifier (MRI) for an ensemble trace,
as per Mark Bogner email of Jan 21, 2013, with a few subsequent changes:
<pre>
PROCEDURE ENSEMBLE.GET_TSTOOL_ENSEMBLE_MRI
  Argument Name                  Type                    In/Out Default?
  ------------------------------ ----------------------- ------ --------
  OP_MODEL_RUN_ID                NUMBER                  OUT
  P_ENSEMBLE_NAME                VARCHAR2                IN
  P_TRACE_NUMBER                 NUMBER                  IN
  P_MODEL_NAME                   VARCHAR2                IN
  P_RUN_DATE                     DATE                    IN     DEFAULT
  P_IS_RUNDATE_KEY               VARCHAR2                IN     DEFAULT
  P_AGEN_ID                      NUMBER                  IN     DEFAULT

This procedure was written exclusively for TsTool use with the following Business rules and specifications

     1. return a model_run_id for the specified TsTool input parameters
     2. apply a business rule: run_date in REF_MODEL_RUN for TsTool is truncated to the hour
     3. apply a business rule: the model_run_name for any new REF_MODEL_RUN records will be a concatenation of the P_ENSEMBLE with the P_TRACE_NUMBER (up to 9999)
     4. throw an exception if P_MODEL_NAME doesn't already exist
     5. create a REF_ENSEMBLE record if the P_ENSEMBLE_NAME doesn't already exist
     6. create a REF_ENSEMBLE_TRACE record if that combination of input parameters to a particular model_run_id record does not already exist
     7. create a REF_MODEL_RUN record if the above business rules and input parameters dictate that necessity
     8. Business rule: P_MODEL_NAME can not be NULL and must match an entry in the database
     9. Business rule: P_ENSEMBLE_NAME can not be NULL
    10. Business rule: P_TRACE_NUMBER can not be NULL
    11. Business rule: P_IS_RUNDATE_KEY must be a "Y" or "N"
    12. Business rule: If using Run_DATE as part of the key, it must be a valid date and not NULL
    13. Any use of P_RUN_DATE utilizes the truncation to the minute
    14. Multiple runs of a single ensemble and trace can be stored if the Run_date is key specified
    15. HYDROLOGIC_INDICATOR will be populated with the P_TRACE_NUMBER (character representation) on creation of a REF_MODEL_RUN record
    16. For REF_ENSEMBLE_TRACE records at a minimum, either column TRACE_NUMERIC or TRACE_NAME must be populated.
    17. For TsTool, creation of REF_ENSEMBLE_TRACE records TRACE_ID, TRACE_NUMERIC and TRACE_NAME will be populated with P_TRACE_NUMBER from the TsTool procedure call
</pre>
@param ensembleName ensemble name (REF_ENSEMBLE.ENSEMBLE_NAME)
@param traceNumber trace number (REF_ENSEMBLE_TRACE.TRACE_NUMERIC)
@param ensembleModelName model name (will be used with the run date to match REF_ENSEMBLE_TRACE.MODEL_RUN_ID)
@param ensembleModelRunDate (will be used with the model name to match REF_ENSEMBLE_TRACE.MODEL_RUN_ID)
@param agenID agencyID number or -1 to use null
@return the model run identifier to use for the ensemble trace, or null if not able to determine
*/
public Long readModelRunIDForEnsembleTrace ( String ensembleName, int traceNumber,
    String ensembleModelName, DateTime ensembleModelRunDate, int agenID )
{   String routine = getClass().getSimpleName() + ".readModelRunIDForEnsembleTrace";
    CallableStatement cs = null;
    try {
        // Argument list includes output
        cs = getConnection().prepareCall("{call ENSEMBLE.GET_TSTOOL_ENSEMBLE_MRI (?,?,?,?,?,?,?)}");
        if ( __readTimeout >= 0 ) {
            cs.setQueryTimeout(__readTimeout);
        }
        int iParam = 1;
        // Have to register the output, in same order as procedure expects
        cs.registerOutParameter(iParam++,java.sql.Types.INTEGER); // 1 - OP_MODEL_RUN_ID
        cs.setString(iParam++,ensembleName); // 2- P_ENSEMBLE_NAME - cannot be null
        cs.setInt(iParam++,traceNumber); // 3 - P_TRACE_NUMBER - cannot be null
        cs.setString(iParam++,ensembleModelName); // 4- P_MODEL_NAME - cannot be null
        if ( ensembleModelRunDate == null ) {
            // Run date is not being used
            cs.setNull(iParam++,java.sql.Types.TIMESTAMP); // 5 - P_RUN_DATE
            cs.setString(iParam++,"N"); // 6 - P_IS_RUNDATE_KEY
        }
        else {
        	// TODO SAM 2016-03-12 Check with Tim Miller whether model run date is MST or local time (MDT)
            cs.setTimestamp(iParam++,new Timestamp(ensembleModelRunDate.getDate(TimeZoneDefaultType.LOCAL).getTime())); // 5 - P_RUN_DATE
            cs.setString(iParam++,"Y"); // 6 - P_IS_RUNDATE_KEY
        }
        if ( agenID < 0 ) {
            cs.setNull(iParam++,java.sql.Types.INTEGER);
            Message.printStatus(2,routine,"Using null agency to get ensemble MRI." );
        }
        else {
            cs.setInt(iParam++,agenID); // 7 - AGEN_ID
            Message.printStatus(2,routine,"Using agency " + agenID + " to get ensemble MRI." );
        }
        cs.executeUpdate();
        int mri = cs.getInt(1);
        cs.close();
        return new Long(mri);
    }
    catch (BatchUpdateException e) {
        // Will happen if any of the batch commands fail.
        Message.printWarning(3,routine,e);
        throw new RuntimeException ( "Error executing  callable statement (" + e + ").", e );
    }
    catch (SQLException e) {
        Message.printWarning(3,routine,e);
        throw new RuntimeException ( "Error executing  callable statement (" + e + ").", e );
    }
}

/**
Read the database parameters from the REF_DB_PARAMETER table.
@return the database parameters as a hashtable
*/
private Hashtable<String,String> readRefDbParameterList ( )
throws SQLException
{   String routine = getClass().getName() + ".readRefDbParameter";

    Hashtable<String,String> results = new Hashtable();
    /* TODO SAM 2010-12-08 This is a real dog - 
    try {
        if ( !DMIUtil.databaseHasTable(this, "REF_DB_PARAMETER") ) {
            return results;
        }
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Error determining whether REF_DB_PARAMETER table exists (" + e + ").");
        return results;
    }
    */
    // Unique combination of many terms (distinct may not be needed).
    // Include newlines to simplify troubleshooting when pasting into other code.
    String sqlCommand = "select REF_DB_PARAMETER.PARAM_NAME, REF_DB_PARAMETER.PARAM_VALUE from " +
        "REF_DB_PARAMETER";
    Message.printStatus(2, routine, "SQL for reading REF_DB_PARAMETER is:\n" + sqlCommand );

    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSizeDefault);
        String propName, propValue;
        while (rs.next()) {
            propName = rs.getString(1);
            propValue = rs.getString(2);
            results.put ( propName, propValue );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting database parameters data from HDB \"" +
            getDatabaseName() + "\" (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        if ( stmt != null ) {
            stmt.close();
        }
    }
    
    return results;
}

/**
Read the distinct list of REF_ENSEMBLE.AGEN_ID, useful for UI choices.
@return the distinct list of ensemble trace domains
*/
public List<Integer> readRefEnsembleAgenIDList ( )
throws SQLException
{   String routine = getClass().getSimpleName() + ".readRefEnsembleAgenIDList";
    List<Integer> results = new ArrayList<Integer>();
    String sqlCommand = "select distinct REF_ENSEMBLE.AGEN_ID from REF_ENSEMBLE order by REF_ENSEMBLE.AGEN_ID";

    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        while (rs.next()) {
            i = rs.getInt(1);
            if ( !rs.wasNull() ) {
                results.add(new Integer(i));
            }
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting ensemble agen_id data from HDB (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read the ensemble key value pairs from the REF_ENSEMBLE_KEYVAL table.
@return the list of ensemble key value pairs
*/
public List<ReclamationHDB_EnsembleKeyVal> readRefEnsembleKeyValList ( int ensembleID )
throws SQLException
{   String routine = getClass().getName() + ".readRefEnsembleKeyValList";
    List<ReclamationHDB_EnsembleKeyVal> results = new ArrayList<ReclamationHDB_EnsembleKeyVal>();
    StringBuilder sqlCommand = new StringBuilder("select REF_ENSEMBLE_KEYVAL.ENSEMBLE_ID, " +
    "REF_ENSEMBLE_KEYVAL.TRACE_ID, REF_ENSEMBLE_KEYVAL.TRACE_NUMERIC from REF_ENSEMBLE_KEYVAL" );
    if ( ensembleID >= 0 ) {
        sqlCommand.append (" WHERE REF_ENSEMBLE_KEYVALUE.ENSEMBLE_ID = " + ensembleID );
    }
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand.toString());
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        String s;
        int record = 0;
        int col;
        ReclamationHDB_EnsembleKeyVal data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_EnsembleKeyVal();
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setEnsembleID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setKeyName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setKeyValue(s);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting ensemble key/value data from HDB (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    return results;
}

/**
Read the ensembles from the REF_ENSEMBLE table.
@param ensembleName the name for the ensemble or null to ignore
@param ensembleIDList list of ensemble identifiers to read, or null to ignore
@param modelRunID model_run_id value to read, or -1 to ignore
@return the list of ensembles
*/
public List<ReclamationHDB_Ensemble> readRefEnsembleList ( String ensembleName, List<Integer> ensembleIDList, int modelRunID )
throws SQLException
{   String routine = getClass().getName() + ".readRefEnsembleList";
    List<ReclamationHDB_Ensemble> results = new ArrayList<ReclamationHDB_Ensemble>();
    StringBuilder sqlCommand = new StringBuilder("select REF_ENSEMBLE.ENSEMBLE_ID, " +
        "REF_ENSEMBLE.ENSEMBLE_NAME, REF_ENSEMBLE.AGEN_ID, REF_ENSEMBLE.TRACE_DOMAIN, " +
        "REF_ENSEMBLE.CMMNT from REF_ENSEMBLE");
    StringBuilder where = new StringBuilder();
    if ( (ensembleName != null) && !ensembleName.equals("") ) {
        where.append ( " (UPPER(REF_ENSEMBLE.ENSEMBLE_NAME) = '" + ensembleName.toUpperCase() + "')");
    }
    if ( (ensembleIDList != null) && (ensembleIDList.size() > 0) ) {
        if ( where.length() > 0 ) {
            where.append ( " AND ");
        }
        where.append ( " (REF_ENSEMBLE.ENSEMBLE_ID IN (" );
        for ( int i = 0; i < ensembleIDList.size(); i++ ) {
            if ( i > 0 ) {
                where.append(",");
            }
            where.append("" + ensembleIDList.get(i));
        }
        where.append ( "))" );
    }
    if ( modelRunID >= 0 ) {
        if ( where.length() > 0 ) {
            where.append ( " AND ");
        }
        where.append (" (REF_ENSEMBLE_TRACE.MODEL_RUN_ID = " + modelRunID + ")");
    }
    if ( where.length() > 0 ) {
        // The keyword was not added above so add here
        where.insert(0, " WHERE ");
    }
    sqlCommand.append(where);
    Message.printStatus(2, routine, "SQL to query ensemble is:  \"" + sqlCommand + "\"" );
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand.toString());
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        String s;
        int record = 0;
        int col;
        ReclamationHDB_Ensemble data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_Ensemble();
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setEnsembleID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setEnsembleName(s);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setAgenID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setTraceDomain(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setCmmnt(s);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting ensemble data from HDB (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read the distinct list of REF_ENSEMBLE.TRACE_DOMAIN, useful for UI choices.
@return the distinct list of ensemble trace domains
*/
public List<String> readRefEnsembleTraceDomainList ( )
throws SQLException
{   String routine = getClass().getSimpleName() + ".readRefEnsembleTraceDomainList";
    List<String> results = new ArrayList<String>();
    String sqlCommand = "select distinct REF_ENSEMBLE.TRACE_DOMAIN from REF_ENSEMBLE order by REF_ENSEMBLE.TRACE_DOMAIN";

    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        String s;
        int record = 0;
        while (rs.next()) {
            ++record;
            s = rs.getString(1);
            if ( !rs.wasNull() ) {
                results.add(s);
            }
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting ensemble trace domain data from HDB (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read the ensemble traces from the REF_ENSEMBLE_TRACE table.
@param ensembleID ensemble ID for which to read data or -1 to read all
@param traceID trace ID for which to read data or -1 to read all
@param modelRunID model run ID for which to read data or -1 to read all
@return the list of ensemble traces
*/
public List<ReclamationHDB_EnsembleTrace> readRefEnsembleTraceList ( int ensembleID, int traceID, int modelRunID, List<Integer> modelRunIDList )
throws SQLException
{   String routine = getClass().getSimpleName() + ".readRefEnsembleTraceList";
    List<ReclamationHDB_EnsembleTrace> results = new ArrayList<ReclamationHDB_EnsembleTrace>();
    if ( !getDatabaseHasEnsembles() ) {
        // Database design does not include ensembles
        return results;
    }
    StringBuilder sqlCommand = new StringBuilder("select REF_ENSEMBLE_TRACE.ENSEMBLE_ID, " +
    "REF_ENSEMBLE_TRACE.TRACE_ID, REF_ENSEMBLE_TRACE.TRACE_NUMERIC, REF_ENSEMBLE_TRACE.TRACE_NAME, " +
    "REF_ENSEMBLE_TRACE.MODEL_RUN_ID from REF_ENSEMBLE_TRACE");
    StringBuilder where = new StringBuilder();
    if ( ensembleID >= 0 ) {
        where.append (" (REF_ENSEMBLE_TRACE.ENSEMBLE_ID = " + ensembleID + ")");
    }
    if ( traceID >= 0 ) {
        if ( where.length() > 0 ) {
            where.append ( " AND ");
        }
        where.append (" (REF_ENSEMBLE_TRACE.TRACE_ID = " + traceID + ")" );
    }
    if ( modelRunID >= 0 ) {
        if ( where.length() > 0 ) {
            where.append ( " AND ");
        }
        where.append (" (REF_ENSEMBLE_TRACE.MODEL_RUN_ID = " + modelRunID + ")");
    }
    if ( (modelRunIDList != null) && (modelRunIDList.size() > 0) ) {
        if ( where.length() > 0 ) {
            where.append ( " AND ");
        }
        where.append ( " (REF_ENSEMBLE_TRACE.MODEL_RUN_ID IN (" );
        for ( int i = 0; i < modelRunIDList.size(); i++ ) {
            if ( i > 0 ) {
                where.append(",");
            }
            where.append("" + modelRunIDList.get(i));
        }
        where.append ( "))" );
    }
    if ( where.length() > 0 ) {
        // The keyword was not added above so add here
        where.insert(0, " WHERE ");
    }
    sqlCommand.append(where);
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        rs = stmt.executeQuery(sqlCommand.toString());
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        int i;
        String s;
        int record = 0;
        int col;
        ReclamationHDB_EnsembleTrace data;
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_EnsembleTrace();
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setEnsembleID(i);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setTraceID(i);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setTraceNumeric(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setTraceName(s);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setModelRunID(i);
            }
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting ensemble trace data from HDB (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        if ( rs != null ) {
            rs.close();
        }
        stmt.close();
    }
    
    return results;
}

/**
Read a list of ReclamationHDB_SiteTimeSeriesMetadata objects given specific input to constrain the query.
@param isReal if true then a real time series is being read; if false a model time series is being read and an attempt to
read matching ensemble trace data also will occur
@param siteCommonName if specified, use to determine the site_datatype_id (SDI)
@param dataTypeCommonName if specified, use to determine the site_datatype_id (SDI)
@param timeStep the interval being read, which indicates which data table to check for matches
@param modelName if specified, use to determine model_run_id (MRI)
@param modelRunName if specified, use to determine model_run_id (MRI)
@param hydrologicIndicator if specified, use to determine model_run_id (MRI)
@param modelRunDate if specified, use to determine model_run_id (MRI)
@param sdi if specified, use this SDI directly
@param mri if specified, use this MRI directly
*/
public List<ReclamationHDB_SiteTimeSeriesMetadata> readSiteTimeSeriesMetadataList( boolean isReal,
    String siteCommonName, String dataTypeCommonName, String timeStep, String modelName, String modelRunName,
    String hydrologicIndicator, String modelRunDate, int sdi, int mri )
throws SQLException
{   
    StringBuffer whereString = new StringBuffer();
    // Replace ? with . in names - ? is a place-holder because . interferes with TSID specification
    if ( (siteCommonName != null) && !siteCommonName.equals("") ) {
        siteCommonName = siteCommonName.replace('?', '.');
        whereString.append( "(upper(HDB_SITE.SITE_COMMON_NAME) = '" + siteCommonName.toUpperCase() + "')" );
    }
    if ( (dataTypeCommonName != null) && !dataTypeCommonName.equals("") ) {
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(upper(HDB_DATATYPE.DATATYPE_COMMON_NAME) = '" + dataTypeCommonName.toUpperCase() + "')" );
    }
    if ( (modelName != null) && !modelName.equals("") ) {
        modelName = modelName.replace('?', '.');
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(upper(HDB_MODEL.MODEL_NAME) = '" + modelName.toUpperCase() + "')" );
    }
    if ( (modelRunName != null) && !modelRunName.equals("") ) {
        modelRunName = modelRunName.replace('?', '.');
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(upper(REF_MODEL_RUN.MODEL_RUN_NAME) = '" + modelRunName.toUpperCase() + "')" );
    }
    if ( (hydrologicIndicator != null) && !hydrologicIndicator.equals("") ) {
        hydrologicIndicator = hydrologicIndicator.replace('?', '.');
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(upper(REF_MODEL_RUN.HYDROLOGIC_INDICATOR) = '" + hydrologicIndicator.toUpperCase() + "')" );
    }
    if ( (modelRunDate != null) && !modelRunDate.equals("") ) {
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(REF_MODEL_RUN.RUN_DATE = to_date('" + modelRunDate + "','YYYY-MM-DD HH24:MI:SS'))" );
    }
    if ( sdi >= 0 ) {
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(HDB_SITE_DATATYPE.SITE_DATATYPE_ID = " + sdi + ")" );
    }
    if ( mri >= 0 ) {
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(REF_MODEL_RUN.MODEL_RUN_ID = " + mri + ")" );
    }
    if ( whereString.length() > 0 ) {
        // The keyword was not added above so add here
        whereString.insert(0, "where ");
    }
    // Don't know from TSID whether it is an ensemble trace so set to false
    boolean isEnsembleTrace = false;
    List<ReclamationHDB_SiteTimeSeriesMetadata> results = readSiteTimeSeriesMetadataListHelper (
        timeStep, whereString.toString(), isReal, isEnsembleTrace );
    for ( ReclamationHDB_SiteTimeSeriesMetadata result: results ) {
        // Try to read matching ensemble trace metadata
        List<ReclamationHDB_EnsembleTrace> traceList = readRefEnsembleTraceList(-1, -1, result.getModelRunID(), null);
        // Should only be one record
        if ( traceList.size() == 1 ) {
            // Assign ensemble trace information
            ReclamationHDB_EnsembleTrace t = traceList.get(0);
            result.setEnsembleTraceID ( t.getTraceID() );
            result.setEnsembleTraceNumeric( t.getTraceNumeric() );
            result.setEnsembleTraceName(t.getTraceName());
            // Also read the ensemble data
            List<Integer> ensembleIDList = new ArrayList<Integer>(1);
            ensembleIDList.add(new Integer(t.getEnsembleID()));
            List<ReclamationHDB_Ensemble> ensembleList = readRefEnsembleList(null, ensembleIDList, -1);
            if ( ensembleList.size() == 1 ) {
                ReclamationHDB_Ensemble e = ensembleList.get(0);
                result.setEnsembleID(e.getEnsembleID());
                result.setEnsembleName(e.getEnsembleName());
                result.setEnsembleAgenID(e.getAgenID());
                ReclamationHDB_Agency a = lookupAgency(getAgencyList(),e.getAgenID());
                if ( a != null ) {
                    result.setEnsembleAgenAbbrev(a.getAgenAbbrev());
                    result.setEnsembleAgenName(a.getAgenName());
                }
                result.setEnsembleTraceDomain(e.getTraceDomain());
                result.setEnsembleCmmnt(e.getCmmnt());
            }
        }
    }
    return results;
}

/**
Read a list of ReclamationHDB_SiteTimeSeriesMetadata objects given specific input to constrain the query.
This version uses as input the SDI amd MRI keys that have been previously determined.
@param siteDataTypeID the SDI for the time series to query
@param timeStep the timestep that allows the table to be determined ("Hour", "6Hour", "Day", etc.)
@param modelRunID the MRI for the model time series to query or -1 if a real time series is being queried
*/
private List<ReclamationHDB_SiteTimeSeriesMetadata> readSiteTimeSeriesMetadataList(
    int siteDataTypeID, String timeStep, int modelRunID )
throws SQLException
{   
    StringBuffer whereString = new StringBuffer();
    whereString.append( "(HDB_SITE_DATATYPE.SITE_DATATYPE_ID = " + siteDataTypeID + ")" );

    boolean isReal = true;
    if ( modelRunID >= 0 ) {
        isReal = false;
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(REF_MODEL_RUN.MODEL_RUN_ID = " + modelRunID + ")" );
    }
    if ( whereString.length() > 0 ) {
        // The keyword was not added above so add here
        whereString.insert(0, "where ");
    }
    // Don't know if an ensemble trace so set that to false
    boolean isEnsembleTrace = false;
    List<ReclamationHDB_SiteTimeSeriesMetadata> results = readSiteTimeSeriesMetadataListHelper (
        timeStep, whereString.toString(), isReal, isEnsembleTrace );
    return results;
}

/**
Read a list of ReclamationHDB_SiteTimeSeriesMetadata objects given an input filter to use for the query.
@param objectTypeDataType a string of the form "ObjectType - DataTypeCommonName" - the object type will
be stripped off before using the data type.
@param timeStep time series timestep ("Hour", "Day", "Month", or "Year")
@param ifp input filter panel with "where" filters, from which specific query criteria are extracted
*/
public List<ReclamationHDB_SiteTimeSeriesMetadata> readSiteTimeSeriesMetadataList ( String dataType, String timeStep,
    InputFilter_JPanel ifp)
throws SQLException
{   String routine = getClass().getName() + ".readSiteTimeSeriesMetadataList";
    List<ReclamationHDB_SiteTimeSeriesMetadata> results = new ArrayList<ReclamationHDB_SiteTimeSeriesMetadata>();
    // Form where clauses based on the data type
    String dataTypeWhereString = "";
    if ( (dataType != null) && !dataType.equals("") && !dataType.equals("*") ) {
        // Have a data type to consider.  Will either be a data type common name or object name " - " and data
        // type common name.  Note that some HDB data types have "-" but do not seem to have surrounding spaces
        String dataTypeCommon = dataType;
        int pos = dataType.indexOf( " - ");
        if ( pos > 0 ) {
            // The common data type is after the object type
            dataTypeCommon = dataType.substring(pos + 3).trim();
        }
        Message.printStatus(2, routine, "dataType=\"" + dataType + "\" + dataTypeCommon=\"" +
            dataTypeCommon + "\"");
        dataTypeWhereString = "upper(HDB_DATATYPE.DATATYPE_COMMON_NAME) = '" + dataTypeCommon.toUpperCase() + "'";
    }

    // Determine whether real and/or model results should be returned.  Get the user value from the
    // "Real or Model Data" input filter choice
    boolean returnReal = false;
    boolean returnModel = false;
    boolean returnEnsembleTrace = false;
    List<String> realModelType = ifp.getInput("Real, Model, Ensemble Data", null, true, null);
    for ( String userInput: realModelType ) {
        if ( userInput.toUpperCase().indexOf("REAL") >= 0 ) {
            returnReal = true;
        }
        if ( userInput.toUpperCase().indexOf("MODEL") >= 0 ) {
            returnModel = true;
        }
        if ( userInput.toUpperCase().indexOf("ENSEMBLETRACE") >= 0 ) {
            returnModel = true;
            returnEnsembleTrace = true;
        }
    }
    if ( !returnReal && !returnModel ) {
        // Default is to return both
        returnReal = true;
        returnModel = true;
    }
    
    // Process the where clauses by extracting input filter where clauses that reference specific tables.
    // Include where clauses for specific tables.  Do this rather than in bulk to make sure that
    // inappropriate filters are not applied (e.g., model filters when only real data are queried)
    List<String> whereClauses = new ArrayList<String>();
    // First include general where clauses
    whereClauses.add ( dataTypeWhereString );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_OBJECTTYPE", true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_SITE", true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_SITE_DATATYPE", true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_DATATYPE", true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_UNIT", true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_STATE", true ) );
    if ( returnModel ) {
        // Model filters
        whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "REF_MODEL_RUN", true ) );
        whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_MODEL", true ) );
        if ( returnEnsembleTrace ) {
            // Ensemble filters
            whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "REF_ENSEMBLE", true ) );
            whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "REF_ENSEMBLE_TRACE", true ) );
        }
    }
    StringBuilder whereString = new StringBuilder();
    for ( String whereClause : whereClauses ) {
        if ( whereClause.length() > 0 ) {
            if ( whereString.length() == 0 ) {
                whereString.append ( "where " );
            }
            else {
                whereString.append ( " and " );
            }
            whereString.append ( "(" + whereClause + ")" );
        }
    }
    
    if ( returnReal ) {
        try {
            // Reading real data, no ensemble
            results.addAll( readSiteTimeSeriesMetadataListHelper ( timeStep, whereString.toString(), true, false ) );
        }
        catch ( Exception e ) {
            Message.printWarning(3,routine,"Error querying Real time series list (" + e + ").");
        }
    }
    if ( returnModel ) {
        try {
            // Reading model data, ensemble if determined above
            results.addAll( readSiteTimeSeriesMetadataListHelper ( timeStep, whereString.toString(), false, returnEnsembleTrace ) );
            /*
             * TODO SAM 2014-04-13 Don't think this is needed now given other changes
            if ( returnEnsembleTrace ) {
                // Remove all the items where the metadata trace ID is missing
                ReclamationHDB_SiteTimeSeriesMetadata r;
                for ( int i = results.size() - 1; i >= 0; i-- ) {
                    r = results.get(i);
                    if ( r.getEnsembleID() < 0 ) {
                        results.remove(i);
                    }
                }
            }
            */
        }
        catch ( Exception e ) {
            Message.printWarning(3,routine,"Error querying Model time series list (" + e + ").");
        }
    }
    
    return results;
}

/**
Read site/time series metadata for a timestep, input filter, and whether real or model time series.
This method may be called multiple times to return the full list of real and model time series.
@param timeStep timestep to query ("Hour", etc.)
@param whereString a "where" string for to apply to the query
@param isReal if true, return the list of real time series; if false return the model time series
@param ifEnsemble if true and isReal=false, add ensemble tables
@return the list of site/time series metadata for real or model time series, matching the input
*/
private List<ReclamationHDB_SiteTimeSeriesMetadata> readSiteTimeSeriesMetadataListHelper (
    String timeStep, String whereString, boolean isReal, boolean isEnsembleTrace )
throws SQLException
{   String routine = getClass().getName() + ".readSiteTimeSeriesMetadataListHelper";
    String tsTableName = getTimeSeriesTableFromInterval ( timeStep, isReal );
    List<ReclamationHDB_SiteTimeSeriesMetadata> results = new ArrayList<ReclamationHDB_SiteTimeSeriesMetadata>();
    String tsType = "Real";
    boolean isModel = false;
    if ( !isReal ) {
        tsType = "Model";
        isModel = true;
    }
    
    // Columns to select (and group by)
    String selectColumns =
    // HDB_OBJECTTYPE
    " HDB_OBJECTTYPE.OBJECTTYPE_ID," +
    " HDB_OBJECTTYPE.OBJECTTYPE_NAME," +
    " HDB_OBJECTTYPE.OBJECTTYPE_TAG,\n" +
    // HDB_SITE
    " HDB_SITE.SITE_ID," +
    " HDB_SITE.SITE_NAME," +
    " HDB_SITE.SITE_COMMON_NAME,\n" +
    //" HDB_SITE.STATE_ID," + // Use the reference table string instead of numeric key
    " HDB_STATE.STATE_CODE,\n" +
    //" HDB_SITE.BASIN_ID," + // Use the reference table string instead of numeric key
    //" HDB_BASIN.BASIN_CODE," +
    " HDB_SITE.BASIN_ID," + // Change to above later when find basin info
    " HDB_SITE.LAT," +
    " HDB_SITE.LONGI," +
    " HDB_SITE.HYDROLOGIC_UNIT," +
    " HDB_SITE.SEGMENT_NO," +
    " HDB_SITE.RIVER_MILE," +
    " HDB_SITE.ELEVATION,\n" +
    " HDB_SITE.DESCRIPTION," +
    " HDB_SITE.NWS_CODE," +
    " HDB_SITE.SCS_ID," +
    " HDB_SITE.SHEF_CODE," +
    " HDB_SITE.USGS_ID," +
    " HDB_SITE.DB_SITE_CODE,\n" +
    // HDB_DATATYPE
    " HDB_DATATYPE.DATATYPE_ID," + // long
    " HDB_DATATYPE.DATATYPE_NAME," + // long
    " HDB_DATATYPE.DATATYPE_COMMON_NAME," + // short
    " HDB_DATATYPE.PHYSICAL_QUANTITY_NAME,\n" + // short
    " HDB_DATATYPE.AGEN_ID,\n" + // short
    //" HDB_DATATYPE.UNIT_ID," + // Use the reference table string instead of numeric key
    " HDB_UNIT.UNIT_COMMON_NAME,\n" +
    // HDB_SITE_DATATYPE
    " HDB_SITE_DATATYPE.SITE_DATATYPE_ID\n";
    
    String selectColumnsModel = "";
    String joinModel = "";
    String selectColumnsEnsembleTrace = "";
    String joinEnsembleTrace = "";
    if ( isModel ) {
        // Add some additional information for the model run time series
        selectColumnsModel =
            ", HDB_MODEL.MODEL_NAME, HDB_MODEL.MODEL_ID," +
            " REF_MODEL_RUN.MODEL_RUN_ID," +
            " REF_MODEL_RUN.MODEL_RUN_NAME," +
            " REF_MODEL_RUN.HYDROLOGIC_INDICATOR," +
            " REF_MODEL_RUN.RUN_DATE\n";
        joinModel =
            "   JOIN REF_MODEL_RUN on " + tsTableName + ".MODEL_RUN_ID = REF_MODEL_RUN.MODEL_RUN_ID\n" +
            "   JOIN HDB_MODEL on REF_MODEL_RUN.MODEL_ID = HDB_MODEL.MODEL_ID\n";
        if ( isEnsembleTrace ) {
            // Join REF_ENSEMBLE, and REF_ENSEMBLE_TRACE
            selectColumnsEnsembleTrace =
                ", REF_ENSEMBLE.ENSEMBLE_ID," +
                " REF_ENSEMBLE.ENSEMBLE_NAME," +
                " REF_ENSEMBLE.AGEN_ID," +
                " REF_ENSEMBLE.TRACE_DOMAIN," +
                " REF_ENSEMBLE.CMMNT,\n" +
                // REF_ENSEMBLE_TRACE.ENSEMBLE_ID -> Foreign key to REF_ENSEMLE.ENSEMBLE_ID
                " REF_ENSEMBLE_TRACE.TRACE_ID," +
                " REF_ENSEMBLE_TRACE.TRACE_NUMERIC," +
                " REF_ENSEMBLE_TRACE.TRACE_NAME"
                // REF_ENSEMBLE_TRACE.MODEL_RUN_ID -> Foreign key to REF_MODEL_RUN.MODEL_RUN_ID
                ;
            joinEnsembleTrace =
                "   JOIN REF_ENSEMBLE_TRACE on REF_ENSEMBLE_TRACE.MODEL_RUN_ID = REF_MODEL_RUN.MODEL_RUN_ID\n" +
                "   JOIN REF_ENSEMBLE on REF_ENSEMBLE.ENSEMBLE_ID = REF_ENSEMBLE_TRACE.ENSEMBLE_ID\n";
        }
    }
    
    // Unique combination of many terms (distinct may not be needed).
    // Include newlines to simplify troubleshooting when pasting into other code.
    String sqlCommand = "select " +
        "distinct " +
        selectColumns +
        selectColumnsModel +
        selectColumnsEnsembleTrace +
        ", min(" + tsTableName + ".START_DATE_TIME), " + // min() and max() require the group by
        "max(" + tsTableName + ".START_DATE_TIME)" +
        " from HDB_OBJECTTYPE \n" +
        "   JOIN HDB_SITE on HDB_SITE.OBJECTTYPE_ID = HDB_OBJECTTYPE.OBJECTTYPE_ID\n" +
        "   JOIN HDB_SITE_DATATYPE on HDB_SITE.SITE_ID = HDB_SITE_DATATYPE.SITE_ID\n" +
        "   JOIN HDB_DATATYPE on HDB_DATATYPE.DATATYPE_ID = HDB_SITE_DATATYPE.DATATYPE_ID\n" +
        "   JOIN HDB_UNIT on HDB_DATATYPE.UNIT_ID = HDB_UNIT.UNIT_ID\n" +
        // The following ensures that returned rows correspond to time series with data
        // TODO SAM 2010-10-29 What about case where time series is "defined" but no data exists?
        "   JOIN " + tsTableName + " on " + tsTableName + ".SITE_DATATYPE_ID = HDB_SITE_DATATYPE.SITE_DATATYPE_ID\n" +
        joinModel +
        joinEnsembleTrace +
        "   LEFT JOIN HDB_STATE on HDB_SITE.STATE_ID = HDB_STATE.STATE_ID\n" +
        whereString +
        " group by " + selectColumns + selectColumnsModel + selectColumnsEnsembleTrace +
        " order by HDB_SITE.SITE_COMMON_NAME, HDB_DATATYPE.DATATYPE_COMMON_NAME";
    Message.printStatus(2, routine, "SQL for reading time series metadata is:\n" + sqlCommand );

    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        if ( __readTimeout >= 0 ) {
            stmt.setQueryTimeout(__readTimeout);
        }
        StopWatch sw = new StopWatch();
        sw.clearAndStart();
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(__resultSetFetchSize);
        sw.stop();
        Message.printStatus(2,routine,"Time to execute query was " + sw.getSeconds() + " seconds." );
        results = toReclamationHDBSiteTimeSeriesMetadataList ( routine, tsType, isEnsembleTrace, timeStep, rs );
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting object/site/datatype data from HDB (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
    	if ( rs != null ) {
    		rs.close();
    	}
    	if ( stmt != null ) {
    		stmt.close();
    	}
    }
    return results;
}

/**
Read a time series using the SDI and MRI keys that match time series metadata, used when reading ensemble traces.
@param siteDataType SDI (must be >= 0)
@param modelRunID MRI, which can be for single model time series or an ensemble trace
(-1 to not use, in which case a real time series will be read)
@param readingEnsemble if true, then the read is for an ensemble trace and the generated TSID will have sequence ID
@param interval interval for time series, required to know what data table to read from and the time series interval
(for example to specify NHour interval)
@param readStart the starting date/time to read, or null to read all available
@param readEnd the ending date/time to read, or null to read all available
@param nHourIntervalOffset if >= 0 indicates the hour for the first valid value,
to allow for cases where the data do not align with hour zero (needed if a previous data load caused
data to be loaded at more hours than appropriate)
@param readData if true, read the data; if false, only read the time series metadata
@return the time series
*/
public TS readTimeSeries ( int siteDataTypeID, int modelRunID, boolean readingEnsemble, TimeInterval interval,
    DateTime readStart, DateTime readEnd, int nHourIntervalOffset, boolean readData )
throws Exception
{
    // Call the helper method that is shared between read methods

    boolean isReal = true;
    String tsType = "Real";
    String seqID = "";
    if ( modelRunID >= 0 ) {
        // Model data since specifying the modelRunID
        isReal = false;
        tsType = "Model";
        if ( readingEnsemble ) {
            // Read the trace information for the MRI in order to get the trace identifier
            List<ReclamationHDB_EnsembleTrace> traceList = readRefEnsembleTraceList(-1, -1, modelRunID, null);
        }
    }
    int intervalBase = interval.getBase();
    int intervalMult = interval.getMultiplier();
    List<ReclamationHDB_SiteTimeSeriesMetadata> tsMetadataList = readSiteTimeSeriesMetadataList(
        siteDataTypeID, interval.toString(), modelRunID );
    if ( tsMetadataList.size() != 1 ) {
        throw new InvalidParameterException ( "Time series SDI " + siteDataTypeID +
            " and MRI " + modelRunID + " matches " + tsMetadataList.size() + " time series - should match exactly one." );
    }
    ReclamationHDB_SiteTimeSeriesMetadata tsMetadata = (ReclamationHDB_SiteTimeSeriesMetadata)tsMetadataList.get(0);
    // Because a TSID string is not used as input for the read, need to construct to use for identification.
    // TODO SAM 2013-09-25 Need to figure out how the unique values for the TSID can be guaranteed
    // The user can always assign an alias to the time series using ${ts:MODEL_RUN_ID}, etc
    // Replace . with ? in strings, but this does not seem to be a problem with the common names
    boolean tsidStyleSDI = getTSIDStyleSDI();
    StringBuilder tsidentString;
    if ( tsidStyleSDI ) {
        // Newer style for TSID
        tsidentString = new StringBuilder( tsMetadata.getObjectTypeName() + TSIdent.LOC_TYPE_SEPARATOR + siteDataTypeID );
        if ( !isReal ) {
            tsidentString.append ( "-" + modelRunID );
        }
        tsidentString.append ( TSIdent.SEPARATOR + "HDB" + TSIdent.SEPARATOR +
            tsMetadata.getDataTypeCommonName().replace("."," ").replace("-"," ") + TSIdent.SEPARATOR +
            interval + TSIdent.SEPARATOR + tsMetadata.getSiteCommonName().replace(".",""));
    }
    else {
        // Put the site common name in the scenario
        tsidentString = new StringBuilder(tsType + TSIdent.LOC_TYPE_SEPARATOR +
            tsMetadata.getSiteCommonName().replace(".", "?") + TSIdent.SEPARATOR +
            "HDB" + TSIdent.SEPARATOR +
            tsMetadata.getDataTypeCommonName().replace(".","?") + TSIdent.SEPARATOR +
            interval );
    }
    if ( !isReal ) {
        // Add the model parts of the TSID
        String modelRunDate = "";
        Date date = tsMetadata.getModelRunDate();
        if ( date != null ) {
            DateTime d = new DateTime(date);
            d.setPrecision(DateTime.PRECISION_MINUTE);
            modelRunDate = d.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm);
        }
        if ( tsidStyleSDI ) {
            // New style already added site common name above and strip out periods
            tsidentString.append ( TSIdent.SEPARATOR +
                tsMetadata.getModelName().replace("."," ") + "-" +
                tsMetadata.getModelRunName().replace("."," ") + "-" +
                tsMetadata.getHydrologicIndicator().replace("."," ") + "-" +
                modelRunDate );
        }
        else {
            // Old style
            tsidentString.append ( TSIdent.SEPARATOR +
                tsMetadata.getModelName() + "-" +
                tsMetadata.getModelRunName() + "-" +
                tsMetadata.getHydrologicIndicator() + "-" +
                modelRunDate );
        }
        if ( seqID.length() > 0 ) {
            tsidentString.append ( TSIdent.SEQUENCE_NUMBER_LEFT + seqID + TSIdent.SEQUENCE_NUMBER_RIGHT );
        }
    }
    TSIdent tsident = TSIdent.parseIdentifier(tsidentString.toString());
    List<String> problems = new ArrayList<String>();
    TS ts = readTimeSeriesHelper ( tsidentString.toString(), tsident, intervalBase, intervalMult,
        siteDataTypeID, modelRunID, tsMetadata, isReal, readingEnsemble, tsType, readStart, readEnd,
        nHourIntervalOffset, readData, problems );
    if ( problems.size() > 0 ) {
    	StringBuilder b = new StringBuilder();
    	for ( int i = 0; i < problems.size(); i++ ) {
    		if ( i > 0 ) {
    			b.append("\n");
    		}
    		b.append(problems.get(i));
    	}
    	throw new RuntimeException ( "Error reading from HDB:\n" + b );
    }
    return ts;
}

/**
Read a time series from the ReclamationHDB database using the string time series identifier.
@param tsidentString time series identifier string.
@param readStart the starting date/time to read.
@param readEnd the ending date/time to read
@param readData if true, read the data; if false, only read the time series metadata
@return the time series
*/
public TS readTimeSeries ( String tsidentString, DateTime readStart, DateTime readEnd, boolean readData )
throws Exception
{
	int nHourIntervalOffset = -1; // Don't use offset
	return readTimeSeries ( tsidentString, readStart, readEnd, nHourIntervalOffset, readData );
}

/**
Read a time series from the ReclamationHDB database using the string time series identifier.
@param tsidentString time series identifier string.
@param readStart the starting date/time to read.
@param readEnd the ending date/time to read
@param nHourIntervalOffset if >= 0 indicates the hour for the first valid value,
to allow for cases where the data do not align with hour zero (needed if a previous data load caused
data to be loaded at more hours than appropriate)
@param readData if true, read the data; if false, only read the time series metadata
@return the time series
*/
public TS readTimeSeries ( String tsidentString, DateTime readStart, DateTime readEnd, int nHourIntervalOffset, boolean readData )
throws Exception
{   String routine = getClass().getSimpleName() + ".readTimeSeries";
    TSIdent tsident = TSIdent.parseIdentifier(tsidentString );

    if ( (tsident.getIntervalBase() != TimeInterval.HOUR) && (tsident.getIntervalBase() != TimeInterval.IRREGULAR) &&
         (tsident.getIntervalMult() != 1) ) {
        // Not able to handle multiples for non-hourly
        throw new IllegalArgumentException("Data interval multiplier must be 1 for intervals other than hour." );
    }
    
    // Read the time series metadata...
    
    boolean isNewStyleTSID = true;
    boolean isReal = true;
    String tsType = "Real";
    if ( tsident.getLocationType().equalsIgnoreCase("Real") ) {
        isNewStyleTSID = false;
        isReal = true;
        tsType = "Real";
    }
    else if ( tsident.getLocationType().equalsIgnoreCase("Model") ) {
        isNewStyleTSID = false;
        isReal = false;
        tsType = "Model";
    }
    // A model time series could be a trace in an ensemble, don't know until read time series metadata
    boolean isEnsembleTrace = false;
    String timeStep = tsident.getInterval();
    List<ReclamationHDB_SiteTimeSeriesMetadata> tsMetadataList = null;
    if ( isNewStyleTSID ) {
        // Read the metadata using the siteDataTypeID and modelRunID
        String [] locParts = tsident.getLocation().split("-");
        int sdi = -1, mri = -1;
        if ( locParts.length == 1 ) {
            // Have SDI only
            isReal = true;
        }
        else if ( locParts.length == 2 ) {
            // Have SDI-MRI
            isReal = false;
            mri = Integer.parseInt(locParts[1]);
        }
        sdi = Integer.parseInt(locParts[0]);
        tsMetadataList = readSiteTimeSeriesMetadataList( isReal,
            null, null, timeStep, null, null, null, null, // None of the common names, etc. used
            sdi, mri );
    }
    else {
        // Read time series metadata using common names as input - problem is these are not unique in current HDB design
        String siteCommonName = tsident.getLocation().substring(tsident.getLocation().indexOf(":") + 1);
        String dataTypeCommonName = tsident.getType();
        String modelName = null;
        String modelRunName = null;
        String modelHydrologicIndicator = null;
        String modelRunDate = null;
        if ( !isReal ) {
            String[] scenarioParts = tsident.getScenario().split("-");
            if ( scenarioParts.length < 4 ) {
                throw new InvalidParameterException ( "Time series identifier \"" + tsidentString +
                "\" is for a model but scenario is not of form " +
                "ModelName-ModelRunName-HydrologicIndicator-ModelRunDate (only have " +
                scenarioParts.length + ")." );
            }
            // Try to do it anyhow - replace question marks from UI with periods to use internally
            if ( scenarioParts.length >= 1 ) {
                modelName = scenarioParts[0].replace('?','.'); // Reverse translation from UI 
            }
            if ( scenarioParts.length >= 2 ) {
                modelRunName = scenarioParts[1].replace('?','.'); // Reverse translation from UI;
            }
            if ( scenarioParts.length >= 3 ) {
                modelHydrologicIndicator = scenarioParts[2].replace('?','.'); // Reverse translation from UI;
            }
            // Run date is whatever is left and the run date includes dashes so need to take the end of the string
            try {
                modelRunDate = tsident.getScenario().substring(modelName.length() + modelRunName.length() +
                    modelHydrologicIndicator.length() + 3); // 3 is for separating periods
            }
            catch ( Exception e ) {
                modelRunDate = null;
            }
        }
        // Scenario for models is ModelName-ModelRunName-ModelRunDate, which translate to a unique model run ID
        tsMetadataList = readSiteTimeSeriesMetadataList( isReal,
            siteCommonName, dataTypeCommonName, timeStep, modelName, modelRunName, modelHydrologicIndicator, modelRunDate,
            -1, -1 ); // SDI and MRI are not used
    }
    if ( tsMetadataList.size() != 1 ) {
        throw new InvalidParameterException ( "Time series identifier \"" + tsidentString +
            "\" matches " + tsMetadataList.size() + " time series - should match exactly one." );
    }
    ReclamationHDB_SiteTimeSeriesMetadata tsMetadata = (ReclamationHDB_SiteTimeSeriesMetadata)tsMetadataList.get(0);
    int siteDataTypeID = tsMetadata.getSiteDataTypeID();
    int refModelRunID = tsMetadata.getModelRunID();
    
    // Call the helper method that is shared between read methods

    TimeInterval tsInterval = TimeInterval.parseInterval(timeStep);
    int intervalBase = tsInterval.getBase();
    int intervalMult = tsInterval.getMultiplier();
    // The above will read ensemble trace metadata if model time series is an ensemble
    if ( !tsMetadata.getEnsembleTraceDomain().equals("") ) {
        isEnsembleTrace = true;
    }
    List<String> problems = new ArrayList<String>();
    TS ts = readTimeSeriesHelper ( tsidentString, tsident, intervalBase, intervalMult,
        siteDataTypeID, refModelRunID, tsMetadata, isReal, isEnsembleTrace, tsType, readStart, readEnd,
        nHourIntervalOffset, readData, problems );
    if ( problems.size() > 0 ) {
    	StringBuilder b = new StringBuilder();
    	for ( int i = 0; i < problems.size(); i++ ) {
    		if ( i > 0 ) {
    			b.append("\n");
    		}
    		b.append(problems.get(i));
    	}
    	throw new RuntimeException ( "Error reading from HDB:\n" + b );
    }
    return ts;
}

/**
Helper method to create and read the time series, once the HDB metadata has been determined.
See the called methods to see how all of the information is determined.
@return the time series that was read
@param tsidentString the time series identifier string, used to create each new time series
@param tsident the time series identifier object
@param intervalBase the time series interval base
@param intervalMult the time series interval multiplier
@param siteDatatypeID the SDI for the time series
@param modelRunID the MRI for the time series, for model time series
@param tsMetadata time series metadata object, used to assign time series header information
@param isReal whether or not the time series is being read from a real table (false=model table)
@param isEnsembleTrace whether or not the time series is being read from an ensemble trace
@param tsType "real" or "model" used for messaging
@param readStart the date/time to start reading, in TSTool conventions
@param readEnd the date/time to end reading, in TSTool conventions
@param nHourIntervalOffset if >= 0 indicates the hour for the first valid value,
to allow for cases where the data do not align with hour zero (needed if a previous data load caused
data to be loaded at more hours than appropriate)
@param readData whether to read data (true) or just the header (false)
@param problems a list of strings indicating problems, to be passed to calling code
*/
private TS readTimeSeriesHelper ( String tsidentString, TSIdent tsident, int intervalBase, int intervalMult,
    int siteDataTypeID, int modelRunID, ReclamationHDB_SiteTimeSeriesMetadata tsMetadata,
    boolean isReal, boolean isTrace, String tsType, DateTime readStart, DateTime readEnd,
    int nHourIntervalOffset, boolean readData, List<String> problems )
throws Exception
{   String routine = getClass().getSimpleName() + ".readTimeSeriesHelper";

    Message.printStatus(2,routine,"Reading time series isTrace=" + isTrace + " nHourIntervalOffset=" + nHourIntervalOffset );
    // Create the time series...
    TS ts = TSUtil.newTimeSeries(tsidentString, true);
    ts.setIdentifier(tsident);   

    // Set the time series metadata in core TSTool data as well as general property list...
    
    String hdbTimeZone = getDatabaseTimeZone(); // From ref_db_parameter TIME_ZONE, for example MST
    ts.setDataUnits(tsMetadata.getUnitCommonName() );
    ts.setDate1Original(convertHDBStartDateTimeToInternal ( tsMetadata.getStartDateTimeMin(),
        intervalBase, intervalMult, hdbTimeZone ) );
    ts.setDate2Original(convertHDBStartDateTimeToInternal ( tsMetadata.getStartDateTimeMax(),
        intervalBase, intervalMult, hdbTimeZone ) );
    // Set the missing value to NaN (HDB missing records typically are not even written to the DB).
    ts.setMissing(Double.NaN);
    setTimeSeriesProperties ( ts, tsMetadata );

    // Now read the data...
    if ( readData ) {
        // Get the table name to read based on the data interval and whether real/model...
        String tsTableName = getTimeSeriesTableFromInterval(tsident.getInterval(), isReal );
        // Handle query dates if specified by calling code...
        String hdbReqStartDateMin = null;
        String hdbReqStartDateMax = null;
        if ( readStart != null ) {
            // The date/time will be internal representation, but need to convert to hdb data record start
            hdbReqStartDateMin = convertInternalDateTimeToHDBStartString ( readStart, intervalBase, intervalMult );
            //Message.printStatus(2,routine,"Setting time series start to requested \"" + readStart + "\"");
            ts.setDate1(readStart);
            if ( (intervalBase == TimeInterval.HOUR) || (intervalBase == TimeInterval.IRREGULAR) ) {
                ts.setDate1(ts.getDate1().setTimeZone(hdbTimeZone) );
            }
            if ( intervalBase == TimeInterval.IRREGULAR ) {
                ts.setDate1(ts.getDate1().setPrecision(DateTime.PRECISION_MINUTE) );
            }
        }
        else {
            Message.printStatus(2, routine, "before date1Original=" + ts.getDate1Original());
            ts.setDate1(ts.getDate1Original());
            Message.printStatus(2, routine, "after date1=" + ts.getDate1() + " date1Original=" + ts.getDate1Original());
        }
        if ( readEnd != null ) {
            // The date/time will be internal representation, but need to convert to hdb data record start
            hdbReqStartDateMax = convertInternalDateTimeToHDBStartString ( readEnd, intervalBase, intervalMult );
            //Message.printStatus(2,routine,"Setting time series end to requested \"" + readEnd + "\"");
            ts.setDate2(readEnd);
            if ( (intervalBase == TimeInterval.HOUR) || (intervalBase == TimeInterval.IRREGULAR) ) {
                ts.setDate2( ts.getDate2().setTimeZone(hdbTimeZone) );
            }
            if ( intervalBase == TimeInterval.IRREGULAR ) {
                ts.setDate2( ts.getDate2().setPrecision(DateTime.PRECISION_MINUTE) );
            }
        }
        else {
            ts.setDate2(ts.getDate2Original());
        }
        // Allocate the time series data space
        ts.allocateDataSpace();
        // Construct the SQL string...
        StringBuffer selectSQL = new StringBuffer (" select " +
            tsTableName + ".START_DATE_TIME, " +
            tsTableName + ".END_DATE_TIME, " +
            tsTableName + ".VALUE " );
        if ( isReal ) {
            // Have flags
            // TODO SAM 2010-11-01 There are also other columns that could be used, but don't for now
            selectSQL.append( ", " +
            tsTableName + ".VALIDATION, " +
            tsTableName + ".OVERWRITE_FLAG, " +
            tsTableName + ".DERIVATION_FLAGS" );
        }
        selectSQL.append (
            " from " + tsTableName +
            " where " + tsTableName + ".SITE_DATATYPE_ID = " + siteDataTypeID );
        if ( !isReal ) {
            // Also select on the model run identifier
            selectSQL.append ( " and " + tsTableName + ".MODEL_RUN_ID = " + modelRunID );
        }
        if ( readStart != null ) {
            selectSQL.append ( " and " + tsTableName + ".START_DATE_TIME >= to_date('" + hdbReqStartDateMin +
            "','" + getOracleDateFormat(intervalBase) + "')" );
        }
        if ( readEnd != null ) {
            selectSQL.append ( " and " + tsTableName + ".START_DATE_TIME <= to_date('" + hdbReqStartDateMax +
            "','" + getOracleDateFormat(intervalBase) + "')" );
        }
        Message.printStatus(2, routine, "SQL:\n" + selectSQL );
        int record = 0;
        ResultSet rs = null;
        Statement stmt = null;
        boolean timeAlignmentChecked = false; // Used with NHour data to make sure requested period times align with available data
        DateTime date1 = ts.getDate1();
        int badAlignmentCount = 0;
        boolean badAlignment = false; // Whether there are issues with HDB date/time and time series
        // Date formats for logging
        DateFormat timeZoneDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        timeZoneDateFormat.setTimeZone(TimeZone.getTimeZone(hdbTimeZone));
        DateFormat gmtDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        gmtDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        DateFormat localDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"); // Don't set the time zone
        try {
            stmt = __hdbConnection.ourConn.createStatement();
            if ( __readTimeout >= 0 ) {
                stmt.setQueryTimeout(__readTimeout);
            }
            StopWatch sw = new StopWatch();
            sw.clearAndStart();
            rs = stmt.executeQuery(selectSQL.toString());
            // Set the fetch size to a relatively big number to try to improve performance.
            // Hopefully this improves performance over VPN and using remote databases
            rs.setFetchSize(__resultSetFetchSize);
            sw.stop();
            Message.printStatus(2,routine,"Query of \"" + tsidentString + "\" data took " + sw.getSeconds() + " seconds.");
            sw.clearAndStart();
            Date hdbSampleStart = null, hdbSampleEnd = null;
            double value = 0.0;
            String validation;
            String overwriteFlag;
            String derivationFlags;
            String dateTimeString;
            DateTime dateTime = null; // Reused to set time series values
            // Create the date/times from the period information to set precision and time zone,
            // in particular to help with irregular data
            DateTime startDateTime = new DateTime(ts.getDate1()); // Reused start for each record
            startDateTime.setTimeZone(hdbTimeZone); // Set here because HDB timestamp does not include time zone
            DateTime endDateTime = new DateTime(ts.getDate1()); // Reused end for each record
            endDateTime.setTimeZone(hdbTimeZone); // Set here because HDB timestamp does not include time zone
            int hour1 = ts.getDate1().getHour();
            // Array to count how many NHour observations fall in a certain hour to allow integrity check
            int [] countHourForNHour = new int[24];
            for ( int i = 0; i < countHourForNHour.length; i++ ) {
            	countHourForNHour[i] = 0;
            }
            int col = 1;
            boolean transferDateTimesAsStrings = true; // Use to evaluate performance of date/time transfer
                                                       // It seems that strings are a bit faster
            boolean doNHour = false;
            if ( (intervalBase == TimeInterval.HOUR) && (intervalMult > 1) ) {
            	doNHour = true;
            }
            String sampleStartString = "", sampleEndString = "";
            while (rs.next()) {
                ++record;
                col = 1;
                // TODO SAM 2010-11-01 Not sure if using getTimestamp() vs. getDate() changes performance
                // In all cases the TimeStamp and Date that come back from the query reflect the database contents,
                // which will have date/time values consistent with MST time (since that is what is in the database)
                // but no time zone set (since HDB uses TIMESTAMP without local time zone)
                if ( transferDateTimesAsStrings ) {
                	// Start
                    dateTimeString = rs.getString(col); // Increment column below
                    sampleStartString = dateTimeString; // This will not contain time zone since Oracle HDB column does not include
                    setDateTimeFromHDBString ( startDateTime, intervalBase, dateTimeString );
                    // Also get Date for debugging
                    if ( (intervalBase == TimeInterval.HOUR) || (intervalBase == TimeInterval.IRREGULAR) ) {
                    	// Will contain nonoseconds and time zone
                        hdbSampleStart = rs.getTimestamp(col);
                    }
                    else {
                    	// Will only contain to date
                        hdbSampleStart = rs.getDate(col);
                    }
                    ++col; // Increment after the above since retrieved for troubleshooting
                    // End 
                    dateTimeString = rs.getString(col++);
                    sampleEndString = dateTimeString;
                    setDateTimeFromHDBString ( endDateTime, intervalBase, dateTimeString );
                    // Get value inside if block so can use in debugging
                    value = rs.getDouble(col++);
                    if ( Message.isDebugOn ) {
	                    Message.printStatus(2,routine,"Sample start string from HDB=" + sampleStartString + " read start=" + startDateTime + 
	                    	" sample end string=" + sampleEndString + " end=" + endDateTime + " value=" + value);
                    }
                    // Make sure the string has the time zone that is expected from the database - NOPE CAN'T DO THIS BECAUSE STRING DOES NOT CONTAIN TIME ZONE
                    // TODO SAM 2016-05-01 could automatically convert to database time zone but should not have to do
                    //if ( !sampleStartString.endsWith(hdbTimeZone) ) {
                    //	String message = "Sample start as text (" + sampleStartString + ") time zone does not match HDB time zone " + hdbTimeZone + " - ignoring value";
                    //	Message.printWarning(3,routine,message);
                    //	problems.add(message);
                    //	continue;
                    //}
                    //if ( !sampleEndString.endsWith(hdbTimeZone) ) {
                    //	String message = "Sample end as text (" + sampleEndString + ") time zone does not match HDB time zone " + hdbTimeZone + " - ignoring value";
                    //	Message.printWarning(3,routine,message);
                    //	problems.add(message);
                    //	continue;
                    //}
                }
                /* Comment out to avoid using - above works and is tested
                else {
                    // Use Date variants to transfer data
                    if ( (intervalBase == TimeInterval.HOUR) || (intervalBase == TimeInterval.IRREGULAR) ) {
                        dt = rs.getTimestamp(col++);
                    }
                    else {
                        dt = rs.getDate(col++);
                    }
                    if ( dt == null ) {
                        // Cannot process record
                        continue;
                    }
                    if ( (intervalBase == TimeInterval.HOUR) || (intervalBase == TimeInterval.IRREGULAR) ) {
                        startDateTime.setDate(dt);
                    }
                    else {
                        // Date object (not timestamp) will throw exception if processing time so set manually
                        startDateTime.setYear(dt.getYear() + 1900);
                        startDateTime.setMonth(dt.getMonth() + 1);
                        startDateTime.setDay(dt.getDate());
                    }
                    if ( (intervalBase == TimeInterval.HOUR) || (intervalBase == TimeInterval.IRREGULAR) ) {
                        dt = rs.getTimestamp(col++);
                    }
                    else {
                        dt = rs.getDate(col++);
                    }
                    if ( dt == null ) {
                        // Cannot process record
                        continue;
                    }
                    if ( (intervalBase == TimeInterval.HOUR) || (intervalBase == TimeInterval.IRREGULAR) ) {
                        endDateTime.setDate(dt);
                    }
                    else {
                        // Date object (not timestamp) will throw exception if processing time so set manually
                        endDateTime.setYear(dt.getYear() + 1900);
                        endDateTime.setMonth(dt.getMonth() + 1);
                        endDateTime.setDay(dt.getDate());
                    }
                    // Put inside so can print debug above
                    value = rs.getDouble(col++);
                }*/
                if ( isReal ) {
                    validation = rs.getString(col++);
                    overwriteFlag = rs.getString(col++);
                    derivationFlags = rs.getString(col++);
                }
                // Set the data in the time series - note that these dates may get modified during the set
                dateTime = convertHDBDateTimesToInternal ( startDateTime, endDateTime, intervalBase, intervalMult, dateTime, hdbTimeZone );
                if ( Message.isDebugOn ) {
                	Message.printStatus(2, routine, "Sample start from query = " + startDateTime + " dateTime after adjusting=" + dateTime + " value=" + value);
                }
                // TODO SAM 2010-10-31 Figure out how to handle flags
                if ( doNHour ) {
                    // TODO SAM 2013-10-14 Need to fix this issue but really need WRITE_TO_HDB to work when setting the end date
                    // It is possible, for example during testing, that an hourly time series is used for 1Hour and NHour
                    // records.  Old records may fill intervening records.  Therefore, only try setting if the hour is
                    // an even multiple of the interval, also considering that the time series may not be on even intervals
                    // (e.g., could be 6hour data at hours 3, 9, 15, 21.  Negatives are OK as long as evenly divisible.
                    // TODO SAM 2013-10-03 Fix this because requested output period may not align with database
                    /*
                    if ( ((dateTime.getHour() - hour1) % intervalMult) != 0 ) {
                        Message.printStatus(2,routine, "Odd interval, skipping HDB startDateTime=\"" + startDateTime +
                            "\" endDateTime=" + endDateTime + " internal dataTime=\"" + dateTime + "\" value=" + value);
                        continue;
                    }
                    */
                }
                // If hourly data, make sure the end hour is not the same as the start hour - this was detected at daylight savings change
                // Actually this may mainly be an issue due to TSTool/Oracle bug
                // and if continue is used below the value after the missing DS interval
                // is often wrong.  Without continue only the DS value being missing in the spring
                // seems to be the issue.
                if ( (intervalBase == TimeInterval.HOUR) && sampleStartString.equals(sampleEndString) ) {
            		Message.printWarning(3,routine,"Sample start and end for hourly data are equal \"" + sampleStartString +
            			"\" - bad data in database due to time change? (using in order returned)." );
                 	//continue;
                }
                // Make sure that the start and end hour are evenly divisible by the hour in the database
                badAlignment = false;
                if ( doNHour ) {
                	// NHour time series
                    if ( !timeAlignmentChecked ) {
                    	// Need to check time alignment for first value
                        boolean datesAdjusted = false;
                        if ( nHourIntervalOffset >= 0 ) {
                        	// Calling code has specified the hour offset to use in data (all other values will be ignored)
                        	date1.setHour(nHourIntervalOffset);
                        	ts.setDate1(date1);
                        	datesAdjusted = true;
                        }
                        else if ( ((dateTime.getHour() - date1.getHour() ) % intervalMult) != 0 ) {
                            // The requested start is offset from the actual data so adjust the time series period to that
                            // of the data.  For example this may be due to:
                            // 1) User does not specify input period for appropriate time zone
                            // 2) Data are being read through "current", which will typically will not match data interval exactly.
                        	// The data could be offset from even intervals, depending on original source.
                            // Set the hour to the smallest in the day that aligns with the data records.
                            date1.setHour(dateTime.getHour()%intervalMult);
                            ts.setDate1(date1);
                            datesAdjusted = true;
                        }
                        DateTime date2 = ts.getDate2();
                        if ( nHourIntervalOffset >= 0 ) {
                        	date2.setHour(24 - intervalMult + nHourIntervalOffset);
                            ts.setDate2(date2);
                            datesAdjusted = true;
                        }
                        else if ( ((dateTime.getHour() - date2.getHour() ) % intervalMult) != 0 ) {
                            // Set the hour to the largest in the day that aligns with the data records
                            date2.setHour(24 - intervalMult + dateTime.getHour()%intervalMult);
                            ts.setDate2(date2);
                            datesAdjusted = true;
                        }
                        timeAlignmentChecked = true;
                        if ( datesAdjusted ) {
                            // Reallocate the data space
                            ts.allocateDataSpace();
                        }
                    }
                    else {
                        // Time alignment was previously checked but to be absolutely sure, check each data record
                        // for alignment.  If nHourIntervalOffset was specified, off-hour data will be ignored below.
                        if ( (dateTime.getHour() - date1.getHour() ) % intervalMult != 0 ) {
                            ++badAlignmentCount;
                            if ( Message.isDebugOn ) {
                            	Message.printDebug(1,routine,"Data date " + dateTime + " does not align with " +
                            		intervalMult + "Hour data with hour offset " + date1.getHour() );
                            }
                            badAlignment = true;
                        }
                    }
                    // Also count the number of occurrences of data values at each hour of the day so a final
                    // check can be done below.
                    ++countHourForNHour[dateTime.getHour()];
                }
                if ( Message.isDebugOn ) {
                    Message.printStatus(2,routine,
                    	"Read HDB sample start string \"" + sampleStartString + "\"" +
                    	" internal start \"" + startDateTime + "\"" +
                    	" HDB sample end string \"" + sampleEndString + "\"" +
                    	" internal end \"" + endDateTime + "\"" +
                    	" startDateTime(GMT)=" + gmtDateFormat.format(hdbSampleStart) +
                    	" startDateTime(" + hdbTimeZone + ")=" + timeZoneDateFormat.format(hdbSampleStart) +
                    	" startDateTime(Locale)=" + localDateFormat.format(hdbSampleStart) +
                        " endDateTime(GMT)=" + (hdbSampleEnd == null ? null : gmtDateFormat.format(hdbSampleEnd)) +
                        " endDateTime(" + hdbTimeZone + ")=" + (hdbSampleEnd == null ? null : timeZoneDateFormat.format(hdbSampleEnd)) +
                        " endDateTime(Locale)=" + (hdbSampleEnd == null ? null : localDateFormat.format(hdbSampleEnd)) +
                        " internal dataTime=\"" + dateTime + "\"" +
                        " value=" + value);
                }
                if ( !badAlignment ) {
                    ts.setDataValue( dateTime, value );
                }
            }
            sw.stop();
            Message.printStatus(2,routine,"Transfer of \"" + tsidentString + "\" data took " + sw.getSeconds() +
                " seconds for " + record + " records.");
            if ( (badAlignmentCount > 0) && (nHourIntervalOffset < 0) ) {
            	// Create a warning unless nHourIntervalOffset was specified - specifying indicates user is purposefully ignoring bad data
            	String message = "There were " + badAlignmentCount +
                    " data values with date/times that did not align as expected with data interval and hour (" +
            			date1.getHour() + ") time zone " + hdbTimeZone +
            			" of the first data value from query (bad data loaded previously?  time zone alignment issue?).  "
            			+ "Use NHourIntervalOffset feature in TSTool ReadReclamationHDB command.";
                Message.printWarning(3, routine, message);
                problems.add(message);
                // Check to see that data values were on expected hours
                for ( int i = 0; i < countHourForNHour.length; i++ ) {
                	problems.add("Count of values at hour " + i + ": " + countHourForNHour[i]);
                }
            }
        }
        catch (SQLException e) {
            Message.printWarning(3, routine, "Error reading " + tsType + " time series data from HDB for TSID \"" +
                tsidentString + "\" (" + e + ") SQL:\n" + selectSQL );
            Message.printWarning(3, routine, e );
            String message = "Error reading " + tsType + " time series data from HDB for TSID \"" +
                    tsidentString + "\" (" + e + ")";
            problems.add(message);
        }
        finally {
        	if ( rs != null ) {
        		rs.close();
        	}
        	if ( stmt != null ) {
        		stmt.close();
        	}
        }
    }
    return ts;
}

/**
Set a DateTime's contents given an HDB (Oracle) date/time string.
This does not do a full parse constructor because a single DateTime instance is reused.
@param dateTime the DateTime instance to set values in (should be at an appropriate precision).  This is reused.
@param intervalBase the base data interval for the date/time being processed - will limit the transfer from the string
@param dateTimeString a string in the format YYYY-MM-DD hh:mm:ss.  The intervalBase is used to determine when
to stop transferring values.  This string will NOT contain a time zone because HDB time series columns are TIMESTAMP without local time zone.
Therefore, set the time zone on dateTime independently (once at start since dateTime is reused).
*/
public void setDateTimeFromHDBString ( DateTime dateTime, int intervalBase, String dateTimeString )
{
    // Transfer the year
    dateTime.setYear ( Integer.parseInt(dateTimeString.substring(0,4)) );
    if ( intervalBase == TimeInterval.YEAR ) {
        return;
    }
    dateTime.setMonth ( Integer.parseInt(dateTimeString.substring(5,7)) );
    if ( intervalBase == TimeInterval.MONTH ) {
        return;
    }
    dateTime.setDay ( Integer.parseInt(dateTimeString.substring(8,10)) );
    if ( intervalBase == TimeInterval.DAY ) {
        return;
    }
    dateTime.setHour ( Integer.parseInt(dateTimeString.substring(11,13)) );
    if ( intervalBase == TimeInterval.HOUR ) {
        return;
    }
    // Instantaneous treat as minute...
    dateTime.setMinute ( Integer.parseInt(dateTimeString.substring(14,16)) );
}

/**
Set the database read timeout.
@param readTimeout read timeout in seconds.
*/
public void setReadTimeout ( int readTimeout )
{
    __readTimeout = readTimeout;
}

/**
Indicate whether the date/time for data when reading NHour should just be the *_HOUR.END_DATE_TIME.
True=EndDateTime, False=StartDateTimePlusInterval
*/
public void setReadNHourEndDateTime ( boolean readNHourEndDateTime )
{
    __readNHourEndDateTime = readNHourEndDateTime;
}

/**
Set the result set fetch size used with JDBC rs.setFetchSize().
@param resultSetFetchSize the maximum number of statements to write when doing a batch insert/update.
*/
public void setResultSetFetchSize(int resultSetFetchSize)
{	String routine = getClass().getSimpleName() + ".setResultSetFetchSize";
	if ( resultSetFetchSize <= 0 ) {
		// Reset to default
		resultSetFetchSize = __resultSetFetchSizeDefault;
	}
	__resultSetFetchSize = resultSetFetchSize;
	// Print a message to make sure the value is being set from config files
	Message.printStatus(2,routine,"Set ResultSetFetchSize="+__resultSetFetchSize);
}

/**
Set properties on the time series, based on HDB information.
@param ts time series to being processed.
*/
private void setTimeSeriesProperties ( TS ts, ReclamationHDB_SiteTimeSeriesMetadata tsm )
{   // Set generally in order of the TSID
    ts.setProperty("REAL_MODEL_TYPE", tsm.getRealModelType());
    
    // Site information...
    ts.setProperty("SITE_ID", DMIUtil.isMissing(tsm.getSiteID()) ? null : new Integer(tsm.getSiteID()) );
    ts.setProperty("SITE_NAME", tsm.getSiteName() );
    ts.setProperty("SITE_COMMON_NAME", tsm.getSiteCommonName() );
    ts.setProperty("STATE_CODE", tsm.getSiteCommonName() );
    ts.setProperty("BASIN_ID", DMIUtil.isMissing(tsm.getBasinID()) ? null : new Integer(tsm.getBasinID()) );
    ts.setProperty("LATITUDE", DMIUtil.isMissing(tsm.getLatitude()) ? null : new Double(tsm.getLatitude()) );
    ts.setProperty("LONGITUDE", DMIUtil.isMissing(tsm.getLongitude()) ? null : new Double(tsm.getLongitude()) );
    ts.setProperty("HUC", tsm.getHuc() );
    ts.setProperty("SEGMENT_NO", DMIUtil.isMissing(tsm.getSegmentNo()) ? null : new Integer(tsm.getSegmentNo()) );
    ts.setProperty("RIVER_MILE", DMIUtil.isMissing(tsm.getRiverMile()) ? null : new Float(tsm.getRiverMile()) );
    ts.setProperty("ELEVATION", DMIUtil.isMissing(tsm.getElevation()) ? null : new Float(tsm.getElevation()) );
    ts.setProperty("DESCRIPTION", tsm.getDescription() );
    ts.setProperty("NWS_CODE", tsm.getNwsCode() );
    ts.setProperty("SCS_ID", tsm.getScsID() );
    ts.setProperty("SHEF_CODE", tsm.getShefCode() );
    ts.setProperty("USGS_ID", tsm.getUsgsID() );
    ts.setProperty("DB_SITE_CODE", tsm.getDbSiteCode() );
    
    ts.setProperty("INTERVAL", tsm.getDataInterval());
    
    ts.setProperty("OBJECT_TYPE_NAME", tsm.getObjectTypeName());
    ts.setProperty("OBJECT_TYPE_ID", tsm.getObjectTypeID());
    ts.setProperty("OBJECT_TYPE_TAG", tsm.getObjectTypeTag());

    // From HDB_DATATYPE
    ts.setProperty("DATA_TYPE_ID", tsm.getDataTypeID());
    ts.setProperty("DATA_TYPE_NAME", tsm.getDataTypeName());
    ts.setProperty("DATA_TYPE_COMMON_NAME", tsm.getDataTypeCommonName());
    ts.setProperty("PHYSICAL_QUANTITY_NAME", tsm.getPhysicalQuantityName());
    ts.setProperty("UNIT_COMMON_NAME", tsm.getUnitCommonName());
    ts.setProperty("AGEN_ID", DMIUtil.isMissing(tsm.getAgenID()) ? null : new Integer(tsm.getAgenID()) );
    ts.setProperty("AGEN_ABBREV", tsm.getAgenAbbrev());

    // From HDB_SITE_DATATYPE
    ts.setProperty("SITE_DATATYPE_ID", DMIUtil.isMissing(tsm.getSiteDataTypeID()) ? null : new Integer(tsm.getSiteDataTypeID()) );

    // From HDB_MODEL
    ts.setProperty("MODEL_NAME", tsm.getModelName());

    // From REF_MODEL_RUN
    if ( !DMIUtil.isMissing(tsm.getModelRunID()) ) {
        ts.setProperty("MODEL_RUN_ID", tsm.getModelRunID() );
        ts.setProperty("MODEL_RUN_NAME", tsm.getModelRunName());
        ts.setProperty("HYDROLOGIC_INDICATOR", tsm.getHydrologicIndicator());
        ts.setProperty("MODEL_RUN_DATE", tsm.getModelRunDate());
        if ( tsm.getHydrologicIndicator().equals("") ) {
            ts.setProperty("TableViewHeaderFormat", "%L, ${ts:SITE_COMMON_NAME}, ${ts:DATA_TYPE_COMMON_NAME}, %U");
        }
        else {
            ts.setProperty("TableViewHeaderFormat", "%L, ${ts:SITE_COMMON_NAME}, ${ts:DATA_TYPE_COMMON_NAME}, ${ts:HYDROLOGIC_INDICATOR}, %U");
        }
    }
    
    if ( !DMIUtil.isMissing(tsm.getEnsembleTraceID()) ) {
        // From REF_ENSEMBLE
        ts.setProperty("ENSEMBLE_ID", tsm.getEnsembleID() );
        ts.setProperty("ENSEMBLE_NAME", tsm.getEnsembleName() );
        ts.setProperty("ENSEMBLE_AGEN_ID", tsm.getEnsembleAgenID());
        ts.setProperty("ENSEMBLE_AGEN_ABBREV", tsm.getEnsembleAgenAbbrev());
        ts.setProperty("ENSEMBLE_AGEN_NAME", tsm.getEnsembleAgenName());
        ts.setProperty("ENSEMBLE_TRACE_DOMAIN", tsm.getEnsembleTraceDomain());
        // From REF_ENSEMBLE_TRACE
        ts.setProperty("ENSEMBLE_TRACE_ID", tsm.getEnsembleTraceID() );
        ts.setProperty("ENSEMBLE_TRACE_NUMERIC", tsm.getEnsembleTraceNumeric() );
        ts.setProperty("ENSEMBLE_TRACE_NAME", tsm.getEnsembleTraceName() );
        // Table heading is not clear so add Hydrologic Indicator
        // TODO SAM 2014-04-13 Evaluate whether this needs to be user-defined
        if ( tsm.getHydrologicIndicator().equals("") ) {
            ts.setProperty("TableViewHeaderFormat", "%L, ${ts:SITE_COMMON_NAME}, ${ts:DATA_TYPE_COMMON_NAME}, %U");
            ts.setProperty("tsp:LegendFormat", "%L, ${ts:SITE_COMMON_NAME}, ${ts:DATA_TYPE_COMMON_NAME}, %U");
        }
        else {
            ts.setProperty("TableViewHeaderFormat", "%L, ${ts:SITE_COMMON_NAME}, ${ts:DATA_TYPE_COMMON_NAME}, ${ts:HYDROLOGIC_INDICATOR}, %U");
            ts.setProperty("tsp:LegendFormat", "%L, ${ts:SITE_COMMON_NAME}, ${ts:DATA_TYPE_COMMON_NAME}, ${ts:HYDROLOGIC_INDICATOR}, %U");
        }
    }
    // Additional properties useful for troubleshooting
    // Use for troubleshooting
    try {
    	OracleConnection conn = (OracleConnection)this.getConnection();
	    ts.setProperty("OracleSessionTimeZone", conn.getSessionTimeZone());
	    ts.setProperty("OracleSessionTimeZoneOffset", conn.getSessionTimeZoneOffset());
    }
    catch ( Exception e ) {
    	// swallow since not critical
    }
}

/**
Indicate whether the TSID format should match SDI syntax or old common name syntax.
 */
public void setTSIDStyleSDI ( boolean tsidStyleSDI )
{
    __tsidStyleSDI = tsidStyleSDI;
}

/**
Set the maximum number of statements to execute in a batch insert.
@param writeToHdbInsertStatementMax the maximum number of statements to write when doing a batch insert/update.
*/
public void setWriteToHdbInsertStatementMax(int writeToHdbInsertStatementMax)
{	String routine = getClass().getSimpleName() + ".setWriteToHdbInsertStatementMax";
	if ( writeToHdbInsertStatementMax <= 0 ) {
		// Reset to default
		writeToHdbInsertStatementMax = __writeToHdbInsertStatementMaxDefault;
	}
	__writeToHdbInsertStatementMax = writeToHdbInsertStatementMax;
	// Print a message to make sure the value is being set from config files
	Message.printStatus(2,routine,"Set WriteToHDBInsertStatementMax="+__writeToHdbInsertStatementMax);
}

/**
Convert result set to site metadata objects.  This method is called after querying time series based on
input filters or by specific criteria, as per TSID.
@param routine routine name for troubleshooting
@param tsType "REAL" or "MODEL".
@param rs result set from query.
*/
private List<ReclamationHDB_SiteTimeSeriesMetadata> toReclamationHDBSiteTimeSeriesMetadataList (
    String routine, String tsType, boolean isEnsembleTrace, String interval, ResultSet rs )
{
    List <ReclamationHDB_SiteTimeSeriesMetadata> results = new ArrayList<ReclamationHDB_SiteTimeSeriesMetadata>();
    ReclamationHDB_SiteTimeSeriesMetadata data = null;
    int i;
    float f;
    String s;
    Date date;
    int col = 1;
    int record = 0;
    boolean isReal = false;
    boolean isModel = false;
    if ( tsType.equalsIgnoreCase("REAL") ) {
        isReal = true;
    }
    if ( !isReal ) {
        isModel = true;
    }
    List<ReclamationHDB_Agency> agencyList = getAgencyList();
    ReclamationHDB_Agency agency;
    try {
        while (rs.next()) {
            ++record;
            data = new ReclamationHDB_SiteTimeSeriesMetadata();
            // Indicate whether data are for real or model time series
            data.setRealModelType ( tsType );
            // Data interval...
            data.setDataInterval ( interval );
            // HDB_OBJECTTYPE
            col = 1;
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setObjectTypeID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setObjectTypeName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setObjectTypeTag(s);
            }
            // HDB_SITE
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setSiteID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setSiteName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setSiteCommonName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setStateCode(s);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setBasinID(i);
            }
            // Latitude and longitude are varchars in the DB - convert to numbers if able
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                if ( StringUtil.isDouble(s) ) {
                    data.setLatitude(Double.parseDouble(s));
                }
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                if ( StringUtil.isDouble(s) ) {
                    data.setLongitude(Double.parseDouble(s));
                }
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setHuc(s);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setSegmentNo(i);
            }
            f = rs.getFloat(col++);
            if ( !rs.wasNull() ) {
                data.setRiverMile(f);
            }
            f = rs.getFloat(col++);
            if ( !rs.wasNull() ) {
                data.setElevation(f);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setDescription(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setNwsCode(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setScsID(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setShefCode(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setUsgsID(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setDbSiteCode(s);
            }
            // HDB_DATATYPE
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setDataTypeID(i);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setDataTypeName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setDataTypeCommonName(s);
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setPhysicalQuantityName(s);
            }
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setAgenID(i);
                // Also set the abbreviation
                agency = lookupAgency ( agencyList, i );
                if ( agency != null ) {
                    if ( agency.getAgenAbbrev() == null ) {
                        data.setAgenAbbrev("");
                    }
                    else {
                        data.setAgenAbbrev(agency.getAgenAbbrev());
                    }
                }
            }
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setUnitCommonName(s);
            }
            // HDB_SITE_DATATYPE
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setSiteDataTypeID(i);
            }
            if ( isModel ) {
                // Also get the model name and model run name, ID, and date...
                s = rs.getString(col++);
                if ( !rs.wasNull() ) {
                    data.setModelName(s);
                }
                i = rs.getInt(col++);
                if ( !rs.wasNull() ) {
                    data.setModelID(i);
                }
                i = rs.getInt(col++);
                if ( !rs.wasNull() ) {
                    data.setModelRunID(i);
                }
                s = rs.getString(col++);
                if ( !rs.wasNull() ) {
                    data.setModelRunName(s);
                }
                s = rs.getString(col++);
                if ( !rs.wasNull() ) {
                    data.setHydrologicIndicator(s);
                }
                date = rs.getTimestamp(col++);
                if ( !rs.wasNull() ) {
                    data.setModelRunDate(date);
                }
                if ( isEnsembleTrace ) {
                    // REF_ENSEMBLE
                    i = rs.getInt(col++);
                    if ( !rs.wasNull() ) {
                        data.setEnsembleID(i);
                    }
                    s = rs.getString(col++);
                    if ( !rs.wasNull() ) {
                        data.setEnsembleName(s);
                    }
                    i = rs.getInt(col++);
                    if ( !rs.wasNull() ) {
                        data.setEnsembleAgenID(i);
                        // Also look up the agency as a string
                        ReclamationHDB_Agency a = lookupAgency(getAgencyList(), i);
                        Message.printStatus(2,routine,"Ensemble agency " + i + " is " + a );
                        if ( a != null ) {
                            data.setEnsembleAgenAbbrev(a.getAgenAbbrev());
                            data.setEnsembleAgenName(a.getAgenName());
                        }
                    }
                    s = rs.getString(col++);
                    if ( !rs.wasNull() ) {
                        data.setEnsembleTraceDomain(s);
                    }
                    s = rs.getString(col++);
                    if ( !rs.wasNull() ) {
                        data.setEnsembleCmmnt(s);
                    }
                    // REF_ENSEMBLE_TRACE
                    i = rs.getInt(col++);
                    if ( !rs.wasNull() ) {
                        data.setEnsembleTraceID(i);
                    }
                    i = rs.getInt(col++);
                    if ( !rs.wasNull() ) {
                        data.setEnsembleTraceNumeric(i);
                    }
                    s = rs.getString(col++);
                    if ( !rs.wasNull() ) {
                        data.setEnsembleTraceName(s);
                    }
                }
            }
            // The min and max for the period
            date = rs.getTimestamp(col++);
            if ( !rs.wasNull() ) {
                data.setStartDateTimeMin(date);
            }
            date = rs.getTimestamp(col++);
            if ( !rs.wasNull() ) {
                data.setStartDateTimeMax(date);
            }
            // Add the object to the return list
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting object/site/datatype data from HDB for record " + record +
            "(" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    return results;
}

//@param intervalOverride if true, then irregular time series will use this hourly interval to write the data
/**
Write a single time series to the database.  A real, model, or single ensemble trace are written depending on input.
@param ts time series to write
@param loadingApp the application name - must match HDB_LOADING_APPLICATION (e.g., "TSTool")
@param siteCommonName site common name, to determine site_datatype_id
@param dataTypeCommonName data type common name, to determine site_datatype_id
@param sideDataTypeID if not null, will be used instead of that determined from above
@param modelName model name, to determine model_run_id
@param modelRunName model run name, to determine model_run_id
@param modelRunDate model run date, to determine model_run_id
@param hydrologicIndicator, to determine model_run_id
@param modelRunID if not null, will be used instead of that determined from above
@param agency agency abbreviation (can be null or blank to use default)
@param validationFlag validation flag for value (can be null or blank to use default)
@param overwriteFlag overwrite flag for value (can be null or blank to use default)
@param dataFlags user-specified data flags (can be null or blank to use default)
@param timeZone time zone to write (can be null or blank to use default)
@param outputStart start of period to write (if null write full period).
@param outputEnd end of period to write (if null write full period).
*/
public void writeTimeSeries ( TS ts, String loadingApp,
    String siteCommonName, String dataTypeCommonName, Long siteDataTypeID,
    String modelName, String modelRunName, String modelRunDate, String hydrologicIndicator, Long modelRunID,
    String agency, String validationFlag, String overwriteFlag, String dataFlags,
    String timeZone, DateTime outputStartReq, DateTime outputEndReq ) //, TimeInterval intervalOverride )
throws SQLException
{   String routine = getClass().getSimpleName() + ".writeTimeSeries";
    if ( ts == null ) {
        return;
    }
    if ( !ts.hasData() ) {
        return;
    }
    // Interval override can only be used with irregular time series
    TimeInterval outputInterval = new TimeInterval(ts.getDataIntervalBase(),ts.getDataIntervalMult());
    if ( (ts.getDataIntervalBase() == TimeInterval.HOUR) && (ts.getDataIntervalMult() == 24) ) {
   	 	throw new IllegalArgumentException("Cannot write 24Hour time series \"" +
   	 		ts.getIdentifierString() + "\" to HDB. Instead, convert to Day interval and then write as day interval time series.");
    }
    // TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
    /*
    if ( intervalOverride == null ) {
        // Use the output interval from the time series
        outputInterval = new TimeInterval(ts.getDataIntervalBase(),ts.getDataIntervalMult());
    }
    else {
        if ( ts.getDataIntervalBase() != TimeInterval.IRREGULAR ) {
            throw new IllegalArgumentException(
                "Interval override can only be used when writing irregular time series." );
        }
        if ( intervalOverride.getMultiplier() != TimeInterval.HOUR ) {
            throw new IllegalArgumentException(
                "Interval override for irregular time series must be hour interval." );
        }
        outputInterval = intervalOverride;
    }
    */
    // Time zone must be specified to avoid issues
    if ( (timeZone == null) || timeZone.isEmpty() ) {
    	throw new IllegalArgumentException("Time zone for data must be specified for data loader to work." );
    }
    // Calendar is used when creating TimeStamp to load data, for example "MST", makes sure to avoid daylight savings shift
    // Have to make sure that timeZone is valid because TimeZone.getTimeZone() will return GMT if it is not recognized
    if ( !TimeUtil.isValidTimeZone(timeZone) ) {
    	throw new IllegalArgumentException("Time zone ID \"" + timeZone + "\" is not recognized." );
    }
    Calendar calendarForTimeZone = Calendar.getInstance(TimeZone.getTimeZone(timeZone));
    Message.printStatus(2,routine,"Using specified time zone \"" + timeZone +
    	"\" for writing time series, Java Calendar used with SQL timestamps is: " + calendarForTimeZone );
    // Determine the loading application
    List<ReclamationHDB_LoadingApplication> loadingApplicationList =
        findLoadingApplication ( getLoadingApplicationList(), loadingApp );
    if ( loadingApplicationList.size() != 1 ) {
        throw new IllegalArgumentException("Unable to match loading application \"" + loadingApp + "\"" );
    }
    int loadingAppID = loadingApplicationList.get(0).getLoadingApplicationID();
    // Determine which HDB table to write...
    String sampleInterval = getSampleIntervalFromInterval ( outputInterval.getBase() );
    // Get the site_datatype_id
    if ( siteDataTypeID == null ) {
        // Try to get from the parts
        // TODO SAM 2012-03-28 Evaluate whether this should be cached
        List<ReclamationHDB_SiteDataType> siteDataTypeList = readHdbSiteDataTypeList();
        List<ReclamationHDB_SiteDataType> matchedList = findSiteDataType(
            siteDataTypeList, siteCommonName, dataTypeCommonName);
        if ( matchedList.size() == 1 ) {
            siteDataTypeID = new Long(matchedList.get(0).getSiteDataTypeID());
        }
        else {
            throw new IllegalArgumentException("Unable to determine site_datatype_id from SiteCommonName=\"" +
                siteCommonName + "\", DataTypeCommonName=\"" + dataTypeCommonName + "\"" );
        }
    }
    if ( modelRunID == null ) {
        modelRunID = new Long(-1);
        if ( (modelName != null) && !modelName.equals("") ) {
            // Try to get from the parts
            List<ReclamationHDB_Model> modelList = readHdbModelList(modelName);
            if ( modelList.size() != 1 ) {
                throw new IllegalArgumentException("Model name \"" + modelName + "\" matches " + modelList.size() +
                    " records in HDB.  Expecting exactly 1.");
            }
            ReclamationHDB_Model model = modelList.get(0);
            if ( (modelRunName != null) && !modelRunName.equals("") ) {
                DateTime runDate = null;
                if ( modelRunDate != null ) {
                    runDate = DateTime.parse(modelRunDate);
                }
                List<ReclamationHDB_ModelRun> modelRunList = readHdbModelRunList(
                    model.getModelID(), null, modelRunName, hydrologicIndicator, runDate) ;
                if ( modelRunList.size() != 1 ) {
                    throw new IllegalArgumentException("Model run name \"" + modelRunName + "\", hydrologic indicator=\"" +
                        hydrologicIndicator + "\", run date=\"" + runDate + "\" matches " + modelRunList.size() +
                        " records in HDB.  Expecting exactly 1.");
                }
                ReclamationHDB_ModelRun modelRun = modelRunList.get(0);
                modelRunID = new Long(modelRun.getModelRunID());
            }
        }
    }
    Integer computeID = null; // Use default
    if ( (agency != null) && agency.equals("") ) {
        // Set to null to use default
        agency = null;
    }
    Integer agenID = null;
    if ( agency != null ) {
        // Lookup the agency from the abbreviation
        agenID = lookupAgency(getAgencyList(), agency).getAgenID();
    }
    if ( (validationFlag != null) && validationFlag.equals("") ) {
        // Set to null to use default
        validationFlag = null;
    }
    if ( (overwriteFlag != null) && overwriteFlag.equals("") ) {
        // Set to null to use default
        overwriteFlag = null;
    }
    if ( (dataFlags != null) && dataFlags.equals("") ) {
        // Set to null to use default
        dataFlags = null;
    }
    DateTime outputStart = new DateTime(ts.getDate1());
    if ( outputStartReq != null ) {
        // Make sure that the requested time aligns with time series period
        if ( outputInterval.isRegularInterval() && !TimeUtil.dateTimeIntervalsAlign(outputStartReq,ts.getDate1(),outputInterval) ) {
        	 throw new IllegalArgumentException("Requested output start \"" + outputStartReq +
        		"\" does not align with time series start \"" + ts.getDate1() + "\" data interval - cannot write.  Change the requested start.");
        }
        outputStart = new DateTime(outputStartReq);
    }
    DateTime outputEnd = new DateTime(ts.getDate2());
    if ( outputEndReq != null ) {
    	// Make sure that the requested time aligns with time series period
        if ( outputInterval.isRegularInterval() && !TimeUtil.dateTimeIntervalsAlign(outputEndReq,ts.getDate2(),outputInterval) ) {
          	 throw new IllegalArgumentException("Requested output end \"" + outputStartReq +
          		"\" does not align with time series end \"" + ts.getDate2() + "\" data interval - cannot write.  Change the requested start.");
          }
        outputEnd = new DateTime(outputEndReq);
    }
    TSIterator tsi = null;
    try {
    	// TODO SAM 2016-05-02 The following checks are redundant with the above (were in place before above)
    	// but use the above for now to force users to understand how they are dealing with offsets.
    	// There is too much potential for issues.
        // Make sure that for NHour data the output start and end align with the time series period
    	// If 1 hour it should not matter because any hour will align
        if ( (outputInterval.getBase() == TimeInterval.HOUR) && (outputInterval.getMultiplier() > 1) ) {
            DateTime date1 = ts.getDate1();
            if ( ((outputStart.getHour() - date1.getHour() ) % outputInterval.getMultiplier()) != 0 ) {
                // The requested start is offset from the actual data so adjust the time series period to that
                // of the data.  For example this may be due to:
                // 1) User does not specify output period for appropriate time zone
                // 2) Data are being output through "current", which will typically will not match data interval exactly
                // Set the hour to the smallest in the day that aligns with the data records
                outputStart = new DateTime(outputStart);
                outputStart.setHour(date1.getHour()%outputInterval.getMultiplier());
            }
            DateTime date2 = ts.getDate2();
            if ( ((outputEnd.getHour() - date2.getHour() ) % outputInterval.getMultiplier()) != 0 ) {
                // Set the hour to the largest in the day that aligns with the data records
                outputEnd = new DateTime(outputEnd);
                outputEnd.setHour(24 - outputInterval.getMultiplier() + date2.getHour()%outputInterval.getMultiplier());
            }
        }
        tsi = ts.iterator(outputStart,outputEnd);
    }
    catch ( Exception e ) {
        throw new RuntimeException("Unable to initialize iterator for period " + outputStart + " to " + outputEnd );
    }
    // Turn off auto-commit to improve performance
    getConnection().setAutoCommit(false);
    CallableStatement cs = getConnection().prepareCall("{call write_to_hdb (?,?,?,?,?,?,?,?,?,?,?,?,?)}");
    TSData tsdata;
    DateTime dateTime;
    int errorCount = 0;
    int writeTryCount = 0;
    double value;
    int iParam;
    int timeOffsetTsToHdbStart = 0;
    int outputIntervalBase = outputInterval.getBase();
    int outputIntervalMult = outputInterval.getMultiplier();
    if ( outputIntervalBase == TimeInterval.HOUR ) {
        // Hourly data - only case where a shift from the TSTool recording time to the HDB start_date_time
        // Need to have the hour shifted by one hour because start date passed as SAMPLE_DATE_TIME
        // is start of interval.  The offset is in milliseconds
        timeOffsetTsToHdbStart = -1000*3600*outputIntervalMult;
        Message.printStatus(2,routine,"For " + outputIntervalMult + "Hour interval offset from end to sample start =" + timeOffsetTsToHdbStart + " ms" );
    }
    else if ( (outputIntervalBase != TimeInterval.IRREGULAR) && (outputIntervalMult != 1) ) {
        // Not able to handle multipliers for non-hourly
        throw new IllegalArgumentException( "Data interval must be 1 for intervals other than hour." );
    }
    // Repeatedly call the stored procedure that writes the data
    if ( modelRunID < 0 ) {
        // Stored procedure wants value of zero if no MRI
        modelRunID = new Long(0);
    }
    Timestamp startTimeStamp, endTimeStamp; // Timestamps for SQL inserts, using timeZone data
    long startTimeStampMsDelta = 0, startTimeStampMsPrev = 0;
    long startTimeStampBeforeShiftMs;
    long startTimeStampMs;
    int batchCount = 0;
    // Maximum batch, 256 as per: http://docs.oracle.com/cd/E11882_01/timesten.112/e21638/tuning.htm
    int batchCountMax = __writeToHdbInsertStatementMax; // Putting a large number here works with new Oracle driver
    int batchCountTotal = 0;
    DateTime batchStart = null;
    DateTime batchEnd = null;
    // Date formats for logging
    DateFormat timeZoneDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    timeZoneDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
    DateFormat gmtDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
    gmtDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    DateFormat localDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z"); // Don't set the time zone
    try {
        while ( true ) {
            tsdata = tsi.next();
            if ( tsdata != null ) {
                // Set the information in the write statement
                dateTime = tsdata.getDate(); // This is TSTool date/time so no Java Date weirdness (yet)
                value = tsdata.getDataValue();
                if ( ts.isDataMissing(value) ) {
                    // TODO SAM 2012-03-27 Evaluate whether should have option to write.
                	// HDB does not have way to write missing because it assumes missing if no records in the database.
                    continue;
                }
                // If an override interval is specified, make sure that the date/time passes the test for
                // writing, as per the TSTool WriteReclamationHDB() documentation
                // TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
                /*
                if ( intervalOverride != null ) {
                    if ( dt.getHour()%intervalOverride.getMultiplier() != 0 ) {
                        // Hour is not evenly divisible by the multiplier so don't allow
                        Message.printWarning(3, routine, "Date/time \"" + dt +
                            "\" hour is not evenly divisible by override interval.  Not writing.");
                        ++errorCount;
                        continue;
                    }
                    // Set the hour and minutes to zero since being written as hourly
                    dt.setMinute(0);
                    dt.setSecond(0);
                    dt.setHSecond(0);
                }
                */
                try {
                    iParam = 1; // JDBC code is 1-based (use argument 1 for return value if used)
                    ++writeTryCount;
                    cs.setInt(iParam++,siteDataTypeID.intValue()); // SAMPLE_SDI
                    // Format the date/time as a string consistent with the database engine
                    //x Old comment leave for now
                    //x sampleDateTimeString = DMIUtil.formatDateTime(this, dt, false);
                    //x writeStatement.setValue(sampleDateTimeString,iParam++); // SAMPLE_DATE_TIME
                    // The offset is negative in order to shift to the start of the interval
                    // Database timestamp is in GMT but corresponds to MST from time series.
                    // In other words, MST time zone will shift times by 7 hours to GMT
                    startTimeStampBeforeShiftMs = dateTime.getDate(timeZone).getTime(); // UNIX GMT time reflecting that date/time is in the specified time zone such as MST
                    startTimeStampMs = startTimeStampBeforeShiftMs + timeOffsetTsToHdbStart; // UNIX GMT, will be non-zero only for hourly data
                    startTimeStampMsDelta = startTimeStampMs - startTimeStampMsPrev; // Delta to see if incrementing evenly over daylight savings
                    startTimeStampMsPrev = startTimeStampMs; // Reset previous value, for log messages
                    // Version to create Timestamp from date/time parts is deprecated so use millisecond version
                    startTimeStamp = new Timestamp(startTimeStampMs);
                    cs.setTimestamp(iParam++,startTimeStamp,calendarForTimeZone); // SAMPLE_DATE_TIME - now back to MST, for example, so JDBC driver sets as MST in database
                    cs.setDouble(iParam++,value); // SAMPLE_VALUE
                    cs.setString(iParam++,sampleInterval); // SAMPLE_INTERVAL
                    cs.setInt(iParam++,loadingAppID); // LOADING_APP_ID
                    if ( computeID == null ) {
                        cs.setNull(iParam++,java.sql.Types.INTEGER);
                    }
                    else {
                        cs.setInt(iParam++,computeID); // COMPUTE_ID
                    }
                    cs.setInt(iParam++,modelRunID.intValue()); // MODEL_RUN_ID - should always be non-null, -1 if not model data
                    if ( validationFlag == null ) { // VALIDATION_FLAG
                        cs.setNull(iParam++,java.sql.Types.CHAR);
                    }
                    else {
                        cs.setString(iParam++,validationFlag);
                    }
                    if ( dataFlags == null ) { // DATA_FLAGS
                        cs.setNull(iParam++,java.sql.Types.VARCHAR);
                    }
                    else {
                        cs.setString(iParam++,dataFlags);
                    }
                    if ( timeZone == null ) { // TIME_ZONE
                        cs.setNull(iParam++,java.sql.Types.VARCHAR);
                    }
                    else {
                        cs.setString(iParam++,timeZone); // For example "MST"
                    }
                    if ( overwriteFlag == null ) { // OVERWRITE_FLAG
                        cs.setNull(iParam++,java.sql.Types.VARCHAR);
                    }
                    else {
                        cs.setString(iParam++,overwriteFlag);
                    }
                    if ( agenID == null ) {
                        cs.setNull(iParam++,java.sql.Types.INTEGER);
                    }
                    else {
                        cs.setInt(iParam++,agenID); // AGEN_ID
                    }
                    // The WRITE_TO_HDB procedure previously only had a SAMPLE_DATE_TIME parameter but as of
                    // 2013-04-16 email from Mark Bogner:
                    // "PER ECAO request:
                    // SAMPLE_END_DATE_TIME has been added as the last parameter of WRITE_TO_HDB in test."
                    // and..
                    // "For the most part, this date/time parameter will be left alone and null.
                    // This parameter was put in place to handle the N hour intervals."
                    //
                    // Consequently, for the most part pass the SAMPLE_END_DATE_TIME as null except in the case
                    // where have NHour data
                    // TODO SAM 2016-04-27 Actually, set for 1 hour also because seems to be issue with daylight savings change otherwise
                    if ( outputIntervalBase == TimeInterval.HOUR ) {
                        endTimeStamp = new Timestamp(dateTime.getDate(timeZone).getTime()); // for timeZone
                        cs.setTimestamp(iParam++,endTimeStamp,calendarForTimeZone); // SAMPLE_END_DATE_TIME, with Calendar indicating MST, so JDBC sets as MST in DB
                        //cs.setTimestamp(iParam++,endTimeStamp);
                        if ( Message.isDebugOn ) {
                            // TODO SAM 2013-10-02 The end date/time always seems to be written as 1 hour offset from start (sample), regardless of
                            // the value that is passed in to WRITE_TO_HDB
                            Message.printStatus(2, routine, "Write date/time=" + dateTime + " (timeZone="+timeZone+") val=" + value +
                                " HDB sampleStartBeforeShift=" + startTimeStampBeforeShiftMs +
                                " HDB sampleStartAfterShift=" + startTimeStampMs +
                                //" msdiff=" + startTimeStampMsDelta +
                                " startTimeStamp(" + timeZone + ")=" +
                                //startTimeStamp +
                                timeZoneDateFormat.format(startTimeStamp) +
                                " HDB end=" + dateTime.getDate(timeZone).getTime() + " endTimeStamp("+timeZone+")=" + timeZoneDateFormat.format(endTimeStamp) +
                                //" endTimeStamp(GMT)=" + endTimeStamp.toGMTString() +
                                " endTimeStamp(GMT)=" + gmtDateFormat.format(endTimeStamp) +
                                //" endTimeStamp(Locale)=" + endTimeStamp.toLocaleString() +
                                " endTimeStamp(Locale)=" + localDateFormat.format(endTimeStamp) +
                                //" start's offset from end=" + timeOffsetTsToHdbStart +
                                " batchCount=" + batchCount);
                        }
                    }
                    else {
                        // Pass a null for SAMPLE_END_DATE_TIME as per functionality before NHour support
                        cs.setNull(iParam++,java.sql.Types.TIMESTAMP);
                        if ( Message.isDebugOn ) {
                            Message.printStatus(2, routine, "Write date/time=" + dateTime + " value=" + value +
                        		" HDB sampleStart=" + startTimeStampMs +
                        		" startTimeStamp(" + timeZone + ")=" +
                                //startTimeStamp +
                                timeZoneDateFormat.format(startTimeStamp) +
                                " HDB date/time ms end=null batchCount=" + batchCount);
                        }
                    }
                    if ( batchCount == 0 ) {
                    	batchStart = new DateTime(dateTime);
                    }
                    batchEnd = dateTime;
                    ++batchCount; 
                    cs.addBatch();
                }
                catch ( Exception e ) {
                    Message.printWarning ( 3, routine, "Error constructing batch write call at " + dateTime + " (" + e + " )" );
                    ++errorCount;
                    if ( errorCount <= 10 ) {
                        // Log the exception, but only for the first 10 errors
                        Message.printWarning(3,routine,e);
                    }
                }
            }
            //if ( writeTryCount > 0 ) {
            //    // TODO SAM 2012-03-28 Only write one value for testing
            //    break;
            //}
            if ( ((tsdata == null) && (batchCount > 0)) || // No more time series data but have a batch to process
            	(batchCount == batchCountMax) ) { // Batch count has reached maximum so process data so far
                // Process the insert, either a group of maximum batch count or the last batch
                try {
                    // TODO SAM 2012-03-28 Figure out how to use to compare values updated with expected number
                    batchCountTotal += batchCount;
                    if ( Message.isDebugOn ) {
	                    Message.printStatus(2, routine, "Writing time series records, this batch count = " + batchCount +
	                        ", batch count max = " + batchCountMax + " batch count total = " + batchCountTotal + " period = " + batchStart + " to " + batchEnd );
                    }
                    int [] updateCounts = cs.executeBatch();
                    if ( updateCounts != null ) {
                        for ( int iu = 0; iu < updateCounts.length; iu++ ) {
                            if ( updateCounts[iu] == Statement.EXECUTE_FAILED ) {
                                Message.printWarning(3,routine,"Error executing batch callable statement." );
                                ++errorCount;
                            }
                        }
                    }
                    // Explicitly commit statements to apply changes
                    cs.getConnection().commit();
                    // Now clear the batch commands for the next group of inserts
                    cs.clearBatch();
                    batchCount = 0;
                }
                catch (Exception e) {
                    // Will happen if any of the batch commands fail.
                    Message.printWarning(3,routine,"Error executing write callable statement (" + e + ")." );
                    Message.printWarning(3,routine,e);
                    ++errorCount;
                }
                finally {
                	batchCount = 0;
                }
            }
            if ( tsdata == null ) {
                // Done with time series
                break;
            }
        }
    }
    catch ( Exception e ) {
        if ( cs != null ) {
            try {
                cs.close();
            }
            catch ( SQLException e2 ) {
                // Should not happen
            }
        }
    }
    finally {
        getConnection().setAutoCommit(true);
    }
    if ( errorCount > 0 ) {
        throw new RuntimeException ( "Had " + errorCount + " errors out of total of " + writeTryCount + " attempts." );
    }
    Message.printStatus(2,routine,"Wrote " + writeTryCount + " values to HDB for SDI=" + siteDataTypeID +
        " MRI=" + modelRunID + ".");
}
    
}