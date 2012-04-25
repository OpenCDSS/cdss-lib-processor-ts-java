package rti.tscommandprocessor.commands.reclamationhdb;

import java.security.InvalidParameterException;
import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib.JavaConnections;

import RTi.DMI.DMI;
import RTi.DMI.DMIStoredProcedureData;
import RTi.DMI.DMIUtil;
import RTi.DMI.DMIWriteStatement;
import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSIdent;
import RTi.TS.TSIterator;
import RTi.TS.TSUtil;
import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.StopWatch;
import RTi.Util.Time.TimeInterval;

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
The hashtable that caches stored procedures.
*/
private Hashtable __storedProcedureHashtable = new Hashtable();

/**
Database parameters from REF_DB_PARAMETER.
*/
private Hashtable<String, String> __databaseParameterList = new Hashtable();

/**
Data types from HDB_DATATYPE.
*/
private List<ReclamationHDB_DataType> __dataTypeList = new Vector();

/**
Loading applications from HDB_LOADING_APPLICATION.
*/
private List<ReclamationHDB_LoadingApplication> __loadingApplicationList = new Vector();

/**
Models from HDB_MODEL.
*/
private List<ReclamationHDB_Model> __modelList = new Vector();

/**
Loading applications from HDB_VALIDATION.
*/
private List<ReclamationHDB_Validation> __validationList = new Vector();

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
    super ( (databaseEngine == null) ? "Oracle":databaseEngine,
        databaseServer, databaseName, port, systemLogin, systemPassword );
    if ( databaseEngine == null ) {
        // Use the default...
        setDatabaseEngine("Oracle");
    }
    setEditable(true);
    setSecure(true);
}

/**
Convert the start/end date/time values from an HDB time series table to a single date/time used internally
with time series.
@param startDateTime the START_DATE_TIME value from an HDB time series table
@param endDateTime the ENDDATE_TIME value from an HDB time series table
@param intervalBase the TimeInterval interval base for the data
@param dateTime if null, create a DateTime and return; if not null, reuse the instance
*/
private DateTime convertHDBDateTimesToInternal ( DateTime startDateTime, DateTime endDateTime,
        int intervalBase, DateTime dateTime )
{   if ( dateTime == null ) {
        // Create a new instance with precision that matches the interval...
        dateTime = new DateTime(intervalBase);
    }
    if ( (intervalBase == TimeInterval.HOUR) || (intervalBase == TimeInterval.IRREGULAR) ) {
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
@param timeZone time zone abbreviation (e.g., "MST" to use for hourly and instantaneous data)
@return internal date/time that can be used to set the time series start/end, for memory allocation
*/
private DateTime convertHDBStartDateTimeToInternal ( Date startDateTime, int intervalBase, String timeZone )
{   DateTime dateTime = new DateTime(startDateTime);
    if ( intervalBase == TimeInterval.HOUR ) {
        // The date/time using internal conventions is one hour later
        // FIXME SAM 2010-11-01 Are there any instantaneous 1hour values?
        dateTime.addHour(1);
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
@param startDateTime min/max start date/time from time series data records as per HDB conventions
@param intervalBase time series interval base
@return internal date/time that can be used to set the time series start/end, for memory allocation
*/
private String convertInternalDateTimeToHDBStartString ( DateTime dateTime, int intervalBase )
{   if ( intervalBase == TimeInterval.HOUR ) {
        // The date/time using internal conventions is one hour later
        // FIXME SAM 2010-11-01 Are there any instantaneous 1hour values?
        dateTime.addHour(-1);
    }
    // Otherwise for DAY, MONTH, YEAR the starting date/time is correct when precision is considered
    return dateTime.toString();
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
Find an instance of ReclamationHDB_LoadingApplication given the application name.
@return the list of matching items (a non-null list is guaranteed)
@param loadingApplicationList a list of ReclamationHDB_Model to search
@param  loadingApplication the model name to match (case-insensitive)
*/
public List<ReclamationHDB_LoadingApplication> findLoadingApplication (
    List<ReclamationHDB_LoadingApplication> loadingApplicationList, String loadingApplication )
{
    List<ReclamationHDB_LoadingApplication> foundList = new Vector();
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
    List<ReclamationHDB_Model> foundList = new Vector();
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
@return the list of matching items (a non-null list is guaranteed)
@param modelRunList a list of ReclamationHDB_ModelRun to search
@param  modelName the model name to match (case-insensitive)
*/
public List<ReclamationHDB_ModelRun> findModelRun( List<ReclamationHDB_ModelRun> modelRunList,
    int modelID, String modelRunName, String modelRunDate, String hydrologicIndicator )
{
    List<ReclamationHDB_ModelRun> foundList = new Vector();
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
        /* TODO SAM 2012-03-26 Need to enable - deal with date/time formatting
        if ( (modelRunDate != null) && !modelRun.getModelRunDate().equalsIgnoreCase(modelRunDate) ) {
            // Model run date to match was specified but did not match
            continue;
        }
        */
        if ( (hydrologicIndicator != null) && !hydrologicIndicator.equals("") &&
            !modelRun.getHydrologicIndicator().equalsIgnoreCase(hydrologicIndicator) ) {
            // Hydrologic indicator to match was specified but did not match
            continue;
        }
        // If here OK to add to the list.
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
    List<ReclamationHDB_SiteDataType> foundList = new Vector();
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
Return the list of loading applications.
*/
private List<ReclamationHDB_LoadingApplication> getLoadingApplicationList ()
{
    return __loadingApplicationList;
}

/**
Get the "Object name - data type" strings to use in time series data type selections.
@param includeObjectType if true, include the object type before the data type; if false, just return
the data type
*/
public List<String> getObjectDataTypes ( boolean includeObjectType )
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
    List<String> types = new Vector();
    
    try
    {
        stmt = __hdbConnection.ourConn.createStatement();
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(10000);
        if ( rs == null ) {
            Message.printWarning(3, routine, "Resultset is null.");
        }
        String objectType = null, dataType = null;
        while (rs.next())
        {
            objectType = rs.getString(1);
            if ( rs.wasNull() ) {
                objectType = "";
            }
            dataType = rs.getString(2);
            if ( rs.wasNull() ) {
                dataType = "";
            }
            if ( includeObjectType ) {
                types.add ( objectType + " - " + dataType );
            }
            else {
                types.add ( dataType );
            }
        }
    }
    catch (SQLException e)
    {
        Message.printWarning(3, routine, "Error getting object/data types from HDB (" + e + ")." );
        Message.printWarning(3, routine, "State:" + e.getSQLState() );
        Message.printWarning(3, routine, "ErrorCode:" + e.getErrorCode() );
        Message.printWarning(3, routine, "Message:" + e.getMessage() );
        Message.printWarning(3, routine, e );
    }
    finally
    {
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
@param a TimeInterval base interval
@return the Oracle string for the to_date() SQL function (e.g., "YYYY-MM-DD HH24:MI:SS")
*/
private String getOracleDateFormat ( int intervalBase )
{
    if ( intervalBase == TimeInterval.HOUR ) {
        return "YYYY-MM-DD HH24";
    }
    else if ( intervalBase == TimeInterval.DAY ) {
        return "YYYY-MM-DD";
    }
    else if ( intervalBase == TimeInterval.MONTH ) {
        return "YYYY-MM";
    }
    else if ( intervalBase == TimeInterval.YEAR ) {
        return "YYYY";
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
    if ( interval.equalsIgnoreCase("hour") || interval.equalsIgnoreCase("1hour")) {
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
        return new Vector();
    }

    int nfg = panel.getNumFilterGroups ();
    InputFilter filter;
    List<String> where_clauses = new Vector();
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
        if ( whereClause.toUpperCase().indexOf(tableNameDot) >= 0 ) {
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
Get the write statement for writing time series.  A stored procedure statement is re-used.
*/
private DMIWriteStatement getWriteTimeSeriesStatement ( DMIWriteStatement writeStatement )
throws Exception
{   String routine = getClass().getName() + ".getWriteTimeSeriesStatement";
    // Look up the definition of the stored procedure (stored in a
    // DMIStoredProcedureData object) in the hashtable.  This allows
    // repeated calls to the same stored procedure to re-used stored
    // procedure meta data without requerying the database.

    String name = "WRITE_TO_HDB"; //"write_to_hdb";
    DMIStoredProcedureData spData = (DMIStoredProcedureData)__storedProcedureHashtable.get(name);
    boolean tryAnyway = true; // Try to use procedure even if following code can't see it defined
    if (spData != null) {
        // If a data object was found, set up the data in the statement below
    }
    else if ( DMIUtil.databaseHasStoredProcedure(this, name) || tryAnyway ) {
        // If no data object was found, but the stored procedure is
        // defined in the database then build the data object for the
        // stored procedure and then store it in the hashtable.
        spData = new DMIStoredProcedureData(this, name);
        __storedProcedureHashtable.put(name, spData);
    }
    else {
        // If no data object was found and the stored procedure is not
        // defined in the database, can't execute.
        Message.printWarning(3, routine,
            "No stored procedure defined in database for procedure \"" + name + "\"" );      
        return null;
    }

    //DMIUtil.dumpProcedureInfo(this, name);

    // Set the data object in the statement.  Doing so will set up the
    // statement as a stored procedure statement.

    writeStatement.setStoredProcedureData(spData);
    return writeStatement;
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
    // Use the reclamation connection object
    String sourceDBType = "OracleHDB";
    String sourceUrl = "jdbc:oracle:thin:@" + databaseServer + ":1521:" + databaseName;
    String sourceUserName = systemLogin;
    String sourcePassword = systemPassword;
    // Default values from the runlastDEV.pl script from Dave King
    // FIXME SAM 2010-10-26 Need to figure out roles for deployment to users
    /*
    String ar = "app_role";
    //String ar = "app_user";
    //String pu = "passwd_user";
    String pu = systemPassword;
    String hl = databaseName; // HDB_LOCAL
    __hdbConnection = new JavaConnections(sourceDBType, sourceUrl, sourceUserName, sourcePassword,
        ar, pu, hl );
        // Below is what the original code had...
        //System.getProperty("ar"), System.getProperty("pu"),
        //System.getProperty("hl"));
         */
    __hdbConnection = new JavaConnections(sourceDBType, sourceUrl, sourceUserName, sourcePassword );
    // Set the connection in the base class so it can be used with utility code
    setConnection ( __hdbConnection.ourConn );
    Message.printStatus(2, routine, "Opened the database connection." );
    readGlobalData();
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
Read global data for the database, to keep in memory and improve performance.
*/
@Override
public void readGlobalData()
{   String routine = getClass().getName() + ".readGlobalData";
    // Don't do a lot of caching at this point since database performance seems to be good
    // Do get the global database controlling parameters and other small reference table data
    // Database properties include database timezone for hourly date/times
    try {
        __databaseParameterList = readRefDbParameterList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,"Error reading database parameters (" + e + ").");
    }
    // Data types
    try {
        __dataTypeList = readHdbDataTypeList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,"Error reading data types (" + e + ").");
    }
    // Loading applications needed to convert "TSTool" to HDB identifier for writing data
    try {
        __loadingApplicationList = readHdbLoadingApplicationList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,"Error reading loading applications (" + e + ").");
    }
    // Validation flags are used when writing time series
    try {
        __validationList = readHdbValidationList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,"Error reading validation flags (" + e + ").");
    }
    // Models, used when writing time series
    try {
        __modelList = readHdbModelList();
    }
    catch ( SQLException e ) {
        Message.printWarning(3,routine,"Error reading models (" + e + ").");
    }
}

/**
Read the database data types from the HDB_DATATYPE table.
@return the list of data types
*/
public List<ReclamationHDB_DataType> readHdbDataTypeList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbDataTypeList";
    List<ReclamationHDB_DataType> results = new Vector();
    String sqlCommand = "select HDB_DATATYPE.DATATYPE_ID, " +
        "HDB_DATATYPE.DATATYPE_NAME, HDB_DATATYPE.DATATYPE_COMMON_NAME, " +
        "HDB_DATATYPE.PHYSICAL_QUANTITY_NAME, HDB_DATATYPE.UNIT_ID, HDB_DATATYPE.ALLOWABLE_INTERVALS, " +
        "HDB_DATATYPE.AGEN_ID, HDB_DATATYPE.CMMNT from HDB_DATATYPE";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(10000);
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
        Message.printWarning(3, routine, "Error getting loading data types from HDB \"" +
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
Read the database parameters from the REF_DB_PARAMETER table.
@return the list of loading application data
*/
private List<ReclamationHDB_LoadingApplication> readHdbLoadingApplicationList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbLoadingApplication";
    List<ReclamationHDB_LoadingApplication> results = new Vector();
    String sqlCommand = "select HDB_LOADING_APPLICATION.LOADING_APPLICATION_ID, " +
    	"HDB_LOADING_APPLICATION.LOADING_APPLICATION_NAME, HDB_LOADING_APPLICATION.MANUAL_EDIT_APP, " +
    	"HDB_LOADING_APPLICATION.CMMNT from HDB_LOADING_APPLICATION";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(10000);
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
@return the list of models
*/
public List<ReclamationHDB_Model> readHdbModelList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbModelList";
    List<ReclamationHDB_Model> results = new Vector();
    String sqlCommand = "select HDB_MODEL.MODEL_ID, " +
        "HDB_MODEL.MODEL_NAME, HDB_MODEL.COORDINATED, " +
        "HDB_MODEL.CMMNT from HDB_MODEL";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(10000);
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
        Message.printWarning(3, routine, "Error getting loading application data from HDB (" + e + ")." );
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
Read the database model runs from the HDB_MODEL_RUN table given the model ID.
@param modelID the model identifier to match, if >= 0
@return the list of model runs
*/
public List<ReclamationHDB_ModelRun> readHdbModelRunListForModelID ( int modelID )
throws SQLException
{   String routine = getClass().getName() + ".readHdbModelList";
    List<ReclamationHDB_ModelRun> results = new Vector();
    StringBuffer sqlCommand = new StringBuffer (
        "select REF_MODEL_RUN.MODEL_ID, REF_MODEL_RUN.MODEL_RUN_ID, REF_MODEL_RUN.MODEL_RUN_NAME," +
        "REF_MODEL_RUN.HYDROLOGIC_INDICATOR, REF_MODEL_RUN.RUN_DATE from REF_MODEL_RUN" );
    if ( modelID >= 0 ) {
        sqlCommand.append ( " WHERE REF_MODEL_RUN.MODEL_ID = " + modelID );
    }
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        rs = stmt.executeQuery(sqlCommand.toString());
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(10000);
        int i;
        String s;
        Date date;
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
            results.add ( data );
        }
    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting loading application data from HDB (" + e + ")." );
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
    List<ReclamationHDB_SiteDataType> results = new Vector();
    String sqlCommand = "select HDB_SITE_DATATYPE.SITE_ID, HDB_SITE_DATATYPE.DATATYPE_ID, " +
        "HDB_SITE_DATATYPE.SITE_DATATYPE_ID, HDB_SITE.SITE_COMMON_NAME, HDB_DATATYPE.DATATYPE_COMMON_NAME " +
        "from HDB_SITE_DATATYPE, HDB_SITE, HDB_DATATYPE " +
        "where HDB_SITE_DATATYPE.SITE_ID = HDB_SITE.SITE_ID and " +
        "HDB_SITE_DATATYPE.DATATYPE_ID = HDB_DATATYPE.DATATYPE_ID";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(10000);
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
    List<ReclamationHDB_Site> results = new Vector();
    String sqlCommand = "select HDB_SITE.SITE_ID," +
    " HDB_SITE.SITE_NAME," +
    " HDB_SITE.SITE_COMMON_NAME,\n" +
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
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(10000);
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
    Message.printStatus(2, routine, "SQL is:\n" + sqlCommand );

    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(10000);
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
Read a list of ReclamationHDB_SiteTimeSeriesMetadata objects given specific input to constrain the query.
*/
public List<ReclamationHDB_SiteTimeSeriesMetadata> readSiteTimeSeriesMetadataList( boolean isReal,
    String siteCommonName, String dataTypeCommonName, String timeStep, String modelName, String modelRunName,
    String hydrologicIndicator, String modelRunDate )
throws SQLException
{   
    StringBuffer whereString = new StringBuffer();
    // Replace ? with . in names - ? is a place-holder because . interferes with TSID specification
    siteCommonName = siteCommonName.replace('?', '.');
    if ( (siteCommonName != null) && !siteCommonName.equals("") ) {
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
        whereString.append( "(REF_MODEL_RUN.RUN_DATE = to_date('" + modelRunDate +
            "','YYYY-MM-DD HH24:MI:SS'))" );
    }
    if ( whereString.length() > 0 ) {
        // The keyword was not added above so add here
        whereString.insert(0, "where ");
    }
    List<ReclamationHDB_SiteTimeSeriesMetadata> results = readSiteTimeSeriesMetadataListHelper (
        timeStep, whereString.toString(), isReal );
    return results;
}

/**
Read a list of ReclamationHDB_SiteTimeSeriesMetadata objects given an input filter to use for the query.
@param objectTypeDataType a string of the form "ObjectType - DataTypeCommonName" - the object type will
be stripped off before using the data type.
@param timeStep time series timestep ("Hour", "Day", "Month", or "Year")
*/
public List<ReclamationHDB_SiteTimeSeriesMetadata> readSiteTimeSeriesMetadataList ( String dataType, String timeStep,
    InputFilter_JPanel ifp)
throws SQLException
{   String routine = getClass().getName() + ".readSiteTimeSeriesMetadataList";
    List<ReclamationHDB_SiteTimeSeriesMetadata> results = new Vector();
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
    List<String> realModelType = ifp.getInput("Real or Model Data", null, true, null);
    for ( String userInput: realModelType ) {
        if ( userInput.toUpperCase().indexOf("REAL") >= 0 ) {
            returnReal = true;
        }
        if ( userInput.toUpperCase().indexOf("MODEL") >= 0 ) {
            returnModel = true;
        }
    }
    if ( !returnReal && !returnModel ) {
        // Default is to return both
        returnReal = true;
        returnModel = true;
    }
    
    // Process the where clauses.
    // Include where clauses for specific tables.  Do this rather than in bulk to make sure that
    // inappropriate filters are not applied (e.g., model filters when only real data are queried)
    List<String> whereClauses = new Vector();
    whereClauses.add ( dataTypeWhereString );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_OBJECTTYPE", true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_SITE", true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_SITE_DATATYPE", true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_DATATYPE", true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_UNIT", true ) );
    whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_STATE", true ) );
    if ( !returnReal ) {
        whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "REF_MODEL_RUN", true ) );
        whereClauses.add ( getWhereClauseStringFromInputFilter ( this, ifp, "HDB_MODEL", true ) );
    }
    StringBuffer whereString = new StringBuffer();
    for ( String whereClause : whereClauses ) {
        if ( whereClause.length() > 0 ) {
            if ( whereString.length() == 0 ) {
                whereString.append ( "where " );
            }
            else if ( whereString.length() > 0 ) {
                whereString.append ( " and " );
            }
            whereString.append ( "(" + whereClause + ")" );
        }
    }
    
    if ( returnReal ) {
        try {
            results.addAll( readSiteTimeSeriesMetadataListHelper ( timeStep, whereString.toString(), true ) );
        }
        catch ( Exception e ) {
            Message.printWarning(3,routine,"Error querying Real time series list (" + e + ").");
        }
    }
    if ( returnModel ) {
        try {
            results.addAll( readSiteTimeSeriesMetadataListHelper ( timeStep, whereString.toString(), false ) );
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
@return the list of site/time series metadata for real or model time series, matching the input
*/
private List<ReclamationHDB_SiteTimeSeriesMetadata> readSiteTimeSeriesMetadataListHelper (
    String timeStep, String whereString, boolean isReal )
throws SQLException
{   String routine = getClass().getName() + ".readSiteTimeSeriesMetadataListHelper";
    String tsTableName = getTimeSeriesTableFromInterval ( timeStep, isReal );
    List<ReclamationHDB_SiteTimeSeriesMetadata> results = new Vector();
    String tsType = "Real";
    if ( !isReal ) {
        tsType = "Model";
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
    " HDB_DATATYPE.DATATYPE_NAME," + // long
    " HDB_DATATYPE.DATATYPE_COMMON_NAME," + // short
    " HDB_DATATYPE.PHYSICAL_QUANTITY_NAME,\n" + // short
    //" HDB_DATATYPE.UNIT_ID," + // Use the reference table string instead of numeric key
    " HDB_UNIT.UNIT_COMMON_NAME,\n" +
    // HDB_SITE_DATATYPE
    " HDB_SITE_DATATYPE.SITE_DATATYPE_ID\n";
    
    String selectColumnsModel = "";
    String joinModel = "";
    if ( !isReal ) {
        // Add some additional information for the model run time series
        selectColumnsModel =
            ", HDB_MODEL.MODEL_NAME," +
            " REF_MODEL_RUN.MODEL_RUN_ID," +
            " REF_MODEL_RUN.MODEL_RUN_NAME," +
            " REF_MODEL_RUN.HYDROLOGIC_INDICATOR," +
            " REF_MODEL_RUN.RUN_DATE\n";
        joinModel =
            "   JOIN REF_MODEL_RUN on " + tsTableName + ".MODEL_RUN_ID = REF_MODEL_RUN.MODEL_RUN_ID\n" +
            "   JOIN HDB_MODEL on REF_MODEL_RUN.MODEL_ID = HDB_MODEL.MODEL_ID\n";
    }
    
    // Unique combination of many terms (distinct may not be needed).
    // Include newlines to simplify troubleshooting when pasting into other code.
    String sqlCommand = "select " +
        "distinct " +
        selectColumns +
        selectColumnsModel +
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
        "   LEFT JOIN HDB_STATE on HDB_SITE.STATE_ID = HDB_STATE.STATE_ID\n" +
        whereString +
        " group by " + selectColumns + selectColumnsModel +
        " order by HDB_SITE.SITE_COMMON_NAME, HDB_DATATYPE.DATATYPE_COMMON_NAME";
    Message.printStatus(2, routine, "SQL is:\n" + sqlCommand );

    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        StopWatch sw = new StopWatch();
        sw.clearAndStart();
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(10000);
        sw.stop();
        Message.printStatus(2,routine,"Time to execute query was " + sw.getSeconds() + " seconds." );
        results = toReclamationHDBSiteTimeSeriesMetadataList ( routine, tsType, timeStep, rs );

    }
    catch (SQLException e) {
        Message.printWarning(3, routine, "Error getting object/site/datatype data from HDB (" + e + ")." );
        Message.printWarning(3, routine, e );
    }
    finally {
        rs.close();
        stmt.close();
    }
    
    return results;
}

/**
Read a time series from the ReclamationHDB database.
@param tsidentString time series identifier string.
@param readStart the starting date/time to read.
@param readEnd the ending date/time to read
@param readData if true, read the data; if false, only read the time series metadata.
*/
public TS readTimeSeries ( String tsidentString, DateTime readStart, DateTime readEnd, boolean readData )
throws Exception
{   String routine = getClass().getName() + "readTimeSeries";
    TSIdent tsident = TSIdent.parseIdentifier(tsidentString );

    // Create the time series...
    
    TS ts = TSUtil.newTimeSeries(tsidentString, true);
    ts.setIdentifier(tsident);
    
    // Read the time series metadata...
    
    boolean isReal = true;
    String tsType = "Real";
    if ( StringUtil.startsWithIgnoreCase(tsident.getLocation(), "Real:") ) {
        isReal = true;
        tsType = "Real";
    }
    else if ( StringUtil.startsWithIgnoreCase(tsident.getLocation(), "Model:") ) {
        isReal = false;
        tsType = "Model";
    }
    else {
        throw new InvalidParameterException ( "Time series identifier \"" + tsidentString +
            "\" does not start with Real: or Model: - cannot determine how to read time series." );
    }
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
            "\" does is for a model but scenario is not of form ModelName-ModelRunName-ModelRunDate" );
        }
        modelName = scenarioParts[0].replace('?','.'); // Reverse translation from UI
        modelRunName = scenarioParts[1].replace('?','.'); // Reverse translation from UI;
        modelHydrologicIndicator = scenarioParts[2].replace('?','.'); // Reverse translation from UI;
        // Run date includes dashes so need to take the end of the string
        modelRunDate = tsident.getScenario().substring(modelName.length() + modelRunName.length() +
            modelHydrologicIndicator.length() + 3); // 3 is for separating periods
    }
    String timeStep = tsident.getInterval();
    // Scenario for models is ModelName-ModelRunName-ModelRunDate, which translate to a unique model run ID
    List<ReclamationHDB_SiteTimeSeriesMetadata> tsMetadataList = readSiteTimeSeriesMetadataList( isReal,
        siteCommonName, dataTypeCommonName, timeStep, modelName, modelRunName, modelHydrologicIndicator,
        modelRunDate );
    TimeInterval tsInterval = TimeInterval.parseInterval(timeStep);
    int intervalBase = tsInterval.getBase();
    if ( tsMetadataList.size() != 1 ) {
        throw new InvalidParameterException ( "Time series identifier \"" + tsidentString +
            "\" matches " + tsMetadataList.size() + " time series - should match exactly one." );
    }
    ReclamationHDB_SiteTimeSeriesMetadata tsMetadata = (ReclamationHDB_SiteTimeSeriesMetadata)tsMetadataList.get(0);
    int siteDataTypeID = tsMetadata.getSiteDataTypeID();
    int refModelRunID = tsMetadata.getModelRunID();
    
    // Set the time series metadata...
    
    String timeZone = getDatabaseTimeZone();
    ts.setDataUnits(tsMetadata.getUnitCommonName() );
    ts.setDate1Original(convertHDBStartDateTimeToInternal ( tsMetadata.getStartDateTimeMin(),
        intervalBase, timeZone ) );
    ts.setDate2Original(convertHDBStartDateTimeToInternal ( tsMetadata.getStartDateTimeMax(),
        intervalBase, timeZone ) );
    // Set the missing value to NaN (HDB missing records typically are not even written to the DB).
    ts.setMissing(Double.NaN);
    
    // Now read the data...
    if ( readData ) {
        // Get the table name to read based on the data interval and whether real/model...
        String tsTableName = getTimeSeriesTableFromInterval(tsident.getInterval(), isReal );
        // Handle query dates if specified by calling code...
        String hdbReqStartDateMin = null;
        String hdbReqStartDateMax = null;
        if ( readStart != null ) {
            // The date/time will be internal representation, but need to convert to hdb data record start
            hdbReqStartDateMin = convertInternalDateTimeToHDBStartString ( readStart, intervalBase );
            ts.setDate1(readStart);
            if ( (intervalBase == TimeInterval.HOUR) || (intervalBase == TimeInterval.IRREGULAR) ) {
                ts.setDate1(ts.getDate1().setTimeZone(timeZone) );
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
            hdbReqStartDateMax = convertInternalDateTimeToHDBStartString ( readEnd, intervalBase );
            ts.setDate2(readEnd);
            if ( (intervalBase == TimeInterval.HOUR) || (intervalBase == TimeInterval.IRREGULAR) ) {
                ts.setDate2( ts.getDate2().setTimeZone(timeZone) );
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
            selectSQL.append ( " and " + tsTableName + ".MODEL_RUN_ID = " + refModelRunID );
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
        try {
            stmt = __hdbConnection.ourConn.createStatement();
            StopWatch sw = new StopWatch();
            sw.clearAndStart();
            rs = stmt.executeQuery(selectSQL.toString());
            // Set the fetch size to a relatively big number to try to improve performance.
            // Hopefully this improves performance over VPN and using remote databases
            rs.setFetchSize(10000);
            sw.stop();
            Message.printStatus(2,routine,"Query of \"" + tsidentString + "\" data took " + sw.getSeconds() + " seconds.");
            sw.clearAndStart();
            Date dt;
            double value;
            String validation;
            String overwriteFlag;
            String derivationFlags;
            String dateTimeString;
            DateTime dateTime = null; // Reused to set time series values
            // Create the date/times from the period information to set precision and time zone,
            // in particular to help with irregular data
            DateTime startDateTime = new DateTime(ts.getDate1()); // Reused start for each record
            DateTime endDateTime = new DateTime(ts.getDate1()); // Reused end for each record
            int col = 1;
            boolean transferDateTimesAsStrings = true; // Use to evaluate performance of date/time transfer
                                                       // It seems that strings are a bit faster
            while (rs.next()) {
                ++record;
                col = 1;
                // TODO SAM 2010-11-01 Not sure if using getTimestamp() vs. getDate() changes performance
                if ( transferDateTimesAsStrings ) {
                    dateTimeString = rs.getString(col++);
                    setDateTime ( startDateTime, intervalBase, dateTimeString );
                    dateTimeString = rs.getString(col++);
                    setDateTime ( endDateTime, intervalBase, dateTimeString );
                }
                else {
                    // Use Date variants to transfer data (seems to be slow)
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
                }
                value = rs.getDouble(col++);
                if ( isReal ) {
                    validation = rs.getString(col++);
                    overwriteFlag = rs.getString(col++);
                    derivationFlags = rs.getString(col++);
                }
                // Set the data in the time series
                dateTime = convertHDBDateTimesToInternal ( startDateTime, endDateTime,
                    intervalBase, dateTime );
                // TODO SAM 2010-10-31 Figure out how to handle flags
                ts.setDataValue( dateTime, value );
            }
            sw.stop();
            Message.printStatus(2,routine,"Transfer of \"" + tsidentString + "\" data took " + sw.getSeconds() +
                " seconds for " + record + " records.");
        }
        catch (SQLException e) {
            Message.printWarning(3, routine, "Error reading " + tsType + " time series data from HDB for TSID \"" +
                tsidentString + "\" (" + e + ") SQL:\n" + selectSQL );
            Message.printWarning(3, routine, e );
        }
        finally {
            rs.close();
            stmt.close();
        }
    }
    return ts;
}

/**
Read the data from the HDB_VALIDATION table.
@return the list of validation data
*/
private List<ReclamationHDB_Validation> readHdbValidationList ( )
throws SQLException
{   String routine = getClass().getName() + ".readHdbValidation";
    List<ReclamationHDB_Validation> results = new Vector();
    String sqlCommand = "select HDB_VALIDATION.VALIDATION, HDB_VALIDATION.CMMNT from HDB_VALIDATION";
    ResultSet rs = null;
    Statement stmt = null;
    try {
        stmt = __hdbConnection.ourConn.createStatement();
        rs = stmt.executeQuery(sqlCommand);
        // Set the fetch size to a relatively big number to try to improve performance.
        // Hopefully this improves performance over VPN and using remote databases
        rs.setFetchSize(10000);
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
Set a DateTime's contents given an HDB (Oracle) date/time string.
This does not do a full parse constructor because a single DateTime instance is reused.
@param dateTime the DateTime instance to set values in (should be at an appropriate precision)
@param intervalBase the base data interval for the date/time being processed - will limit the transfer from
the string
@param dateTimeString a string in the format YYYY-MM-DD hh:mm:ss.  The intervalBase is used to determine when
to stop transferring values.
*/
public void setDateTime ( DateTime dateTime, int intervalBase, String dateTimeString )
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
Convert result set to site metadata objects.  This method is called after querying time series based on
input filters or by specific criteria, as per TSID.
@param rs result set from query.
*/
private List<ReclamationHDB_SiteTimeSeriesMetadata> toReclamationHDBSiteTimeSeriesMetadataList (
    String routine, String tsType, String interval, ResultSet rs )
{
    List <ReclamationHDB_SiteTimeSeriesMetadata> results = new Vector();
    ReclamationHDB_SiteTimeSeriesMetadata data = null;
    int i;
    float f;
    String s;
    Date date;
    int col = 1;
    int record = 0;
    boolean isReal = true;
    if ( !tsType.equalsIgnoreCase("Real") ) {
        isReal = false;
    }
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
            s = rs.getString(col++);
            if ( !rs.wasNull() ) {
                data.setUnitCommonName(s);
            }
            // HDB_SITE_DATATYPE
            i = rs.getInt(col++);
            if ( !rs.wasNull() ) {
                data.setSiteDataTypeID(i);
            }
            if ( !isReal ) {
                // Also get the model name and model run name, ID, and date...
                s = rs.getString(col++);
                if ( !rs.wasNull() ) {
                    data.setModelName(s);
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

/**
Write a time series to the database.
@param ts time series to write
@param loadingApp the application name - must match HDB_LOADING_APPLICATION (e.g., "TSTool")
@param siteCommonName site common name, to determine site_datatype_id
@param dataTypeCommonName data type common name, to determine site_datatype_id
@param sideDataTypeID if specified, will be used instead of that determined from above
@param modelName model name, to determine model_run_id
@param modelRunName model run name, to determine model_run_id
@param modelRunDate model run date, to determine model_run_id
@param hydrologicIndicator, to determine model_run_id
@param modelRunID, will be used instead of that determined from above
@param validationFlag validation flag for value
@param dataFlags user-specified data flags
@param timeZone time zone to write (can be null)
@param outputStart start of period to write (if null write full period).
@param outputEnd end of period to write (if null write full period).
*/
public void writeTimeSeries ( TS ts, String loadingApp,
    String siteCommonName, String dataTypeCommonName, Long siteDataTypeID,
    String modelName, String modelRunName, String modelRunDate, String hydrologicIndicator, Long modelRunID,
    String validationFlag, String dataFlags, String timeZone, DateTime outputStartReq, DateTime outputEndReq )
throws SQLException
{   String routine = getClass().getName() + ".writeTimeSeries";
    if ( ts == null ) {
        return;
    }
    if ( !ts.hasData() ) {
        return;
    }
    // Determine the loading application
    List<ReclamationHDB_LoadingApplication> loadingApplicationList =
        findLoadingApplication ( getLoadingApplicationList(), loadingApp );
    if ( loadingApplicationList.size() != 1 ) {
        throw new IllegalArgumentException("Unable to match loading application \"" + loadingApp + "\"" );
    }
    int loadingAppID = loadingApplicationList.get(0).getLoadingApplicationID();
    String sampleInterval = getSampleIntervalFromInterval ( ts.getDataIntervalBase() );
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
        if ( modelRunName != null ) {
            // Try to get from the parts
            throw new IllegalArgumentException(
                "Determining model_run_id from other parameters currently is not supported." );
        }
        else {
            modelRunID = new Long(0);
        }
    }
    Integer computeID = null; // Use default
    if ( (validationFlag != null) && validationFlag.equals("") ) {
        // Set to null to use default
        validationFlag = null;
    }
    if ( (dataFlags != null) && dataFlags.equals("") ) {
        // Set to null to use default
        dataFlags = null;
    }
    if ( (timeZone != null) && timeZone.equals("") ) {
        // Set to null to use default
        timeZone = null;
    }
    DateTime outputStart = new DateTime(ts.getDate1());
    if ( outputStartReq != null ) {
        outputStart = new DateTime(outputStartReq);
    }
    DateTime outputEnd = new DateTime(ts.getDate2());
    if ( outputEndReq != null ) {
        outputEnd = new DateTime(outputEndReq);
    }
    TSIterator tsi = null;
    try {
        tsi = ts.iterator(outputStart,outputEnd);
    }
    catch ( Exception e ) {
        throw new RuntimeException("Unable to initialize iterator for period " + outputStart + " to " + outputEnd );
    }
    // If true use RiversideDMI, if false use basic callable statement as per some code from Dave King
    boolean writeUsingRiversideDMI = false;
    DMIWriteStatement writeStatement = null;
    CallableStatement cs = null;
    if ( writeUsingRiversideDMI ) {
        writeStatement = new DMIWriteStatement(this);
        // Call the stored procedure that writes the data
        try {
            writeStatement = getWriteTimeSeriesStatement ( writeStatement );
        }
        catch ( Exception e ) {
            throw new RuntimeException("Unable to get write statement (" + e + ")" );
        }
        if ( writeStatement == null ) {
            throw new RuntimeException("Unable to create write statement for stored procedure." );
        }
    }
    else {
        //cs = getConnection().prepareCall("begin write_to_hdb (?,?,?,?,?,?,?,?); end;");
        cs = getConnection().prepareCall("{call write_to_hdb (?,?,?,?,?,?,?,?,?,?)}");
    }
    TSData tsdata;
    DateTime dt;
    String sampleDateTimeString;
    int errorCount = 0;
    int writeTryCount = 0;
    double value;
    int iParam;
    int timeOffset = 0;
    if ( ts.getDataIntervalBase() == TimeInterval.HOUR ) {
        // Hourly data need to have the hour shifted by one hour
        timeOffset = -1000*3600;
    }
    // Repeatedly call the stored procedure that writes the data
    while ( (tsdata = tsi.next()) != null ) {
        // Set the information in the write statement
        dt = tsdata.getDate();
        value = tsdata.getDataValue();
        if ( ts.isDataMissing(value) ) {
            // TODO SAM 2012-03-27 Evaluate whether should have option to write
            continue;
        }
        try {
            iParam = 1; // JDBC code is 1-based (use argument 1 for return value if used)
            ++writeTryCount;
            if ( writeUsingRiversideDMI ) {
                writeStatement.setValue(siteDataTypeID,iParam++); // SAMPLE_SDI
                // Format the date/time as a string consistent with the database engine
                //sampleDateTimeString = DMIUtil.formatDateTime(this, dt, false);
                //writeStatement.setValue(sampleDateTimeString,iParam++); // SAMPLE_DATE_TIME
                writeStatement.setValue(dt,iParam++); // SAMPLE_DATE_TIME
                writeStatement.setValue(value,iParam++); // SAMPLE_VALUE
                writeStatement.setValue(sampleInterval,iParam++); // SAMPLE_INTERVAL
                writeStatement.setValue(loadingAppID,iParam++); // LOADING_APP_ID
                writeStatement.setValue(computeID,iParam++); // COMPUTE_ID
                writeStatement.setValue(modelRunID,iParam++); // MODEL_RUN_ID
                writeStatement.setValue(validationFlag,iParam++); // VALIDATION_FLAG
                writeStatement.setValue(dataFlags,iParam++); // DATA_FLAGS
                writeStatement.setValue(timeZone,iParam++); // TIME_ZONE
                // Execute the statement
                //Message.printStatus(2, routine, "Statement is: " + writeStatement.toString() );
                //dmiWrite(writeStatement, 0);
            }
            else {
                cs.setInt(iParam++,siteDataTypeID.intValue()); // SAMPLE_SDI
                // Format the date/time as a string consistent with the database engine
                //sampleDateTimeString = DMIUtil.formatDateTime(this, dt, false);
                //writeStatement.setValue(sampleDateTimeString,iParam++); // SAMPLE_DATE_TIME
                cs.setTimestamp(iParam++,new Timestamp(dt.getDate().getTime()+timeOffset)); // SAMPLE_DATE_TIME
                cs.setDouble(iParam++,value); // SAMPLE_VALUE
                cs.setString(iParam++,sampleInterval); // SAMPLE_INTERVAL
                cs.setInt(iParam++,loadingAppID); // LOADING_APP_ID
                if ( computeID == null ) {
                    cs.setNull(iParam++,java.sql.Types.INTEGER);
                }
                else {
                    cs.setInt(iParam++,computeID); // COMPUTE_ID
                }
                cs.setInt(iParam++,modelRunID.intValue()); // MODEL_RUN_ID - should always be non-null
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
                    cs.setString(iParam++,timeZone);
                }
                cs.addBatch();
            }
        }
        catch ( Exception e ) {
            if ( writeUsingRiversideDMI ) {
                Message.printWarning ( 3, routine, "Error writing value at " + dt + " (" + e + " )" );
            }
            else {
                Message.printWarning ( 3, routine, "Error constructing batch write call at " + dt + " (" + e + " )" );
            }
            ++errorCount;
            if ( errorCount <= 10 ) {
                // Log the exception, but only for the first 10 errors
                Message.printWarning(3,routine,e);
            }
        }
        //if ( writeTryCount > 0 ) {
        //    // TODO SAM 2012-03-28 Only write one value for testing
        //    break;
        //}
    }
    if ( !writeUsingRiversideDMI ) {
        try {
            // TODO SAM 2012-03-28 Figure out how to use to compare values updated with expected number
            //int [] updateCounts =
                cs.executeBatch();
            cs.close();
        }
        catch (BatchUpdateException e) {
            // Will happen if any of the batch commands fail.
            Message.printWarning(3,routine,e);
            throw new RuntimeException ( "Error executing write callable statement.", e );
        }
        catch (SQLException e) {
            Message.printWarning(3,routine,e);
            throw new RuntimeException ( "Error executing write callable statement.", e );
        }
    }
    if ( errorCount > 0 ) {
        throw new RuntimeException ( "Had " + errorCount + " errors out of total of " + writeTryCount + " attempts." );
    }
    Message.printStatus(2,routine,"Wrote " + writeTryCount + " values to HDB.");
}
    
}