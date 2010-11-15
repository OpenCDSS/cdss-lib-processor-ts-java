package rti.tscommandprocessor.commands.reclamationhdb;

import java.security.InvalidParameterException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib.JavaConnections;

import RTi.DMI.DMI;
import RTi.DMI.DMIUtil;
import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
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
    setSecure(false);
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
    if ( intervalBase == TimeInterval.HOUR ) {
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
@return internal date/time that can be used to set the time series start/end, for memory allocation
*/
private DateTime convertHDBStartDateTimeToInternal ( Date startDateTime, int intervalBase )
{   DateTime dateTime = new DateTime(startDateTime);
    if ( intervalBase == TimeInterval.HOUR ) {
        // The date/time using internal conventions is one hour later
        // FIXME SAM 2010-11-01 Are there any instantaneous 1hour values?
        dateTime.addHour(1);
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
    else {
        throw new InvalidParameterException("Time interval " + intervalBase +
            " is not recognized - can't get Oracle date/time format.");
    }
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
    /* TODO SAM 2010-10-28 Support later.
    else if ( interval.toUpperCase().indexOf("IRR") >= 0 ) {
        if ( isReal ) {
            return prefix + "INSTANT";
        }
        else {
            throw new InvalidParameterException("Interval \"" + interval + "\" is not supported for model data." );
        }
    }
    */
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
        where_clause = DMIUtil.getWhereClauseFromInputFilter(dmi, filter,panel.getOperator(ifg));
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
@return a list of where clauses, each of which can be added to a DMI statement.
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
        if ( !whereClause.toUpperCase().startsWith(tableNameDot) ) {
            // Not for the requested table
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
{
    // TODO SAM 2010-10-18 Need to enable
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
        whereString.append( "(HDB_SITE.SITE_COMMON_NAME = '" + siteCommonName + "')" );
    }
    if ( (dataTypeCommonName != null) && !dataTypeCommonName.equals("") ) {
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(HDB_DATATYPE.DATATYPE_COMMON_NAME = '" + dataTypeCommonName + "')" );
    }
    if ( (modelName != null) && !modelName.equals("") ) {
        modelName = modelName.replace('?', '.');
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(HDB_MODEL.MODEL_NAME = '" + modelName + "')" );
    }
    if ( (modelRunName != null) && !modelRunName.equals("") ) {
        modelRunName = modelRunName.replace('?', '.');
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(REF_MODEL_RUN.MODEL_RUN_NAME = '" + modelRunName + "')" );
    }
    if ( (hydrologicIndicator != null) && !hydrologicIndicator.equals("") ) {
        hydrologicIndicator = hydrologicIndicator.replace('?', '.');
        if ( whereString.length() > 0 ) {
            whereString.append ( " and " );
        }
        whereString.append( "(REF_MODEL_RUN.HYDROLOGIC_INDICATOR = '" + hydrologicIndicator + "')" );
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
        dataTypeWhereString = "HDB_DATATYPE.DATATYPE_COMMON_NAME = '" + dataTypeCommon + "'";
    }

    // Determine whether real and/or model results should be returned.  Get the user value from the
    // "Real or Model Data" input filter choice
    boolean returnReal = false;
    boolean returnModel = false;
    List<String> realModelType = ifp.getInput("Real or Model Data", true, null);
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
    
    ts.setDataUnits(tsMetadata.getUnitCommonName() );
    ts.setDate1Original(convertHDBStartDateTimeToInternal ( tsMetadata.getStartDateTimeMin(),
        intervalBase ) );
    ts.setDate2Original(convertHDBStartDateTimeToInternal ( tsMetadata.getStartDateTimeMax(),
        intervalBase ) );
    // Set the missing value to 
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
        }
        else {
            ts.setDate1(ts.getDate1Original());
        }
        if ( readEnd != null ) {
            // The date/time will be internal representation, but need to convert to hdb data record start
            hdbReqStartDateMax = convertInternalDateTimeToHDBStartString ( readEnd, intervalBase );
            ts.setDate2(readEnd);
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
            DateTime startDateTime = new DateTime(); // Reused start for each record
            DateTime endDateTime = new DateTime(); // Reused end for each record
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
                    if ( intervalBase == TimeInterval.HOUR ) {
                        dt = rs.getTimestamp(col++);
                    }
                    else {
                        dt = rs.getDate(col++);
                    }
                    if ( dt == null ) {
                        // Cannot process record
                        continue;
                    }
                    if ( intervalBase == TimeInterval.HOUR ) {
                        startDateTime.setDate(dt);
                    }
                    else {
                        // Date object (not timestamp) will throw exception if processing time so set manually
                        startDateTime.setYear(dt.getYear() + 1900);
                        startDateTime.setMonth(dt.getMonth() + 1);
                        startDateTime.setDay(dt.getDate());
                    }
                    if ( intervalBase == TimeInterval.HOUR ) {
                        dt = rs.getTimestamp(col++);
                    }
                    else {
                        dt = rs.getDate(col++);
                    }
                    if ( dt == null ) {
                        // Cannot process record
                        continue;
                    }
                    if ( intervalBase == TimeInterval.HOUR ) {
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
Set a DateTime's contents given a string.  This does not do a full parse constructor because a single
DateTime instance is reused.
@param dateTime the DateTime instance to set values in (should be at an appropriate precision)
@param intervalBase the base data interval for the date/time being processed - will limit the transfer from
the string
@param dateTimeString a string in the format YYYY-MM-DD hh:mm:ss.  The intervalBase is used to determine when
to stop transferring values.
*/
private void setDateTime ( DateTime dateTime, int intervalBase, String dateTimeString )
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
    // TODO don't handle instantaneous
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
Write a list of time series to the database.
*/
public void writeTimeSeriesList ( List<TS> tslist, DateTime outputStart, DateTime outputEnd )
{
    
}
    
}