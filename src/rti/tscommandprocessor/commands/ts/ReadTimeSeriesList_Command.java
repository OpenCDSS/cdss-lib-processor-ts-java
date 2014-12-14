package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringDictionary;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the RreadTimeSeriesList() command.
*/
public class ReadTimeSeriesList_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

protected final String _Default = "Default";
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public ReadTimeSeriesList_Command ()
{
	super();
	setCommandName ( "ReadTimeSeriesList" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Get the property values. 
	String TableID = parameters.getValue("TableID");
    String LocationTypeColumn = parameters.getValue("LocationTypeColumn");
    String LocationType = parameters.getValue("LocationType");
	String LocationColumn = parameters.getValue("LocationColumn");
	String DataSourceColumn = parameters.getValue("DataSourceColumn");
	String DataSource = parameters.getValue("DataSource");
	String DataTypeColumn = parameters.getValue("DataTypeColumn");
    String DataType = parameters.getValue("DataType");
    String DataStoreColumn = parameters.getValue("DataStoreColumn");
	String DataStore = parameters.getValue("DataStore");
	String Interval = parameters.getValue("Interval");
	String IfNotFound = parameters.getValue("IfNotFound");
    
    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }
    
    if ( (LocationTypeColumn != null) && (LocationTypeColumn.length() > 0) &&
        (LocationType != null) && (LocationType.length() > 0)) {
        message = "LocationTypeColumn and LocationType cannot both be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify only LocationTypeColumn or LocationType, or neither if appropriate." ) );
    }
    
    if ( (LocationColumn == null) || (LocationColumn.length() == 0) ) {
        message = "The location column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the location column." ) );
    }
    
    if ( (DataSourceColumn != null) && (DataSourceColumn.length() > 0) &&
        (DataSource != null) && (DataSource.length() > 0)) {
        message = "DataSourceColumn and DataSource cannot both be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify only DataSourceColumn or DataSource, or neither if appropriate." ) );
    }
    
    if ( (DataTypeColumn != null) && (DataTypeColumn.length() > 0) &&
        (DataType != null) && (DataType.length() > 0)) {
        message = "DataTypeColumn and DataType cannot both be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify only DataTypeColumn or DataType, or neither if appropriate." ) );
    }
    
	// Interval
	if ((Interval == null) || Interval.equals("")) {
        message = "The interval must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid interval." ) );
	}
	else {
		try {
		    TimeInterval.parseInterval ( Interval );;
		} 
		catch (Exception e) {
            message = "The data interval \"" + Interval + "\" is not valid.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid interval (e.g., 5Minute, 6Hour, Day, Month, Year)" ) );
		}
	}
	
	if ( ((DataStoreColumn == null) || DataStoreColumn.equals("")) && ((DataStore == null) || DataStore.equals("")) ) {
        message = "The datastore (column) has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid datastore column or datastore value (e.g., HydroBase)." ) );
    }
	
    if ( (DataStoreColumn != null) && (DataStoreColumn.length() > 0) && (DataStore != null) && (DataStore.length() > 0)) {
        message = "DataStoreColumn and DataStore cannot both be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify only DataStoreColumn or DataStore, or neither if appropriate." ) );
    }
	
	if ( (IfNotFound != null) && !IfNotFound.equals("") &&
        !IfNotFound.equalsIgnoreCase(_Ignore) &&
        !IfNotFound.equalsIgnoreCase(_Default) &&
        !IfNotFound.equalsIgnoreCase(_Warn) ) {
        message = "Invalid IfNotFound flag \"" + IfNotFound + "\".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the IfNotFound as " + _Default + ", " +
                _Ignore + ", or (default) " + _Warn + "." ) );
                            
	}

	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(20);
    validList.add ( "TableID" );
    validList.add ( "LocationTypeColumn" );
    validList.add ( "LocationType" );
    validList.add ( "LocationColumn" );
    validList.add ( "DataSourceColumn" );
    validList.add ( "DataSource" );
    validList.add ( "DataTypeColumn" );
    validList.add ( "DataType" );
    validList.add ( "Interval" );
    validList.add ( "Scenario" );
    validList.add ( "DataStoreColumn" );
    validList.add ( "DataStore" );
    validList.add ( "InputName" );
    validList.add ( "Alias" );
    validList.add ( "ColumnProperties" );
    validList.add ( "Properties" );
    validList.add ( "IfNotFound" );
    validList.add ( "DefaultUnits" );
    validList.add ( "TimeSeriesCountProperty" );
    validList.add ( "TimeSeriesIndex1Property" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag, warning_level ),
			warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	// The command will be modified if changed...
	return ( new ReadTimeSeriesList_JDialog ( parent, this, tableIDChoices ) ).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number The number of the command being run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getClass() + ".runCommand", message;
	int warning_level = 2;
	int log_level = 3; // Level for non-user messages for log file.
	String command_tag = "" + command_number;
	int warning_count = 0;
	    
    // Get and clear the status and clear the run log...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    CommandProcessor processor = getCommandProcessor();

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String TableID = parameters.getValue("TableID");
    String LocationTypeColumn = parameters.getValue ( "LocationTypeColumn" );
    String LocationType = parameters.getValue ( "LocationType" );
    String LocationColumn = parameters.getValue ( "LocationColumn" );
    String DataSourceColumn = parameters.getValue ( "DataSourceColumn" );
    String DataSource = parameters.getValue ( "DataSource" );
    String [] dataSourceList = new String[0];
    if ( (DataSource != null) && !DataSource.equals("") ) {
        if ( DataSource.indexOf(",") < 0 ) {
            dataSourceList = new String[1];
            dataSourceList[0] = DataSource;
        }
        else {
            dataSourceList = DataSource.split(",");
        }
    }
    String DataTypeColumn = parameters.getValue ( "DataTypeColumn" );
    String DataType = parameters.getValue ( "DataType" );
    if ( DataType == null ) {
        DataType = "";
    }
    String Interval = parameters.getValue ( "Interval" );
    if ( Interval == null ) {
        Interval = "";
    }
    String Scenario = parameters.getValue ( "Scenario" );
    if ( Scenario == null ) {
        Scenario = "";
    }
    String DataStoreColumn = parameters.getValue ( "DataStoreColumn" );
    String DataStore = parameters.getValue ( "DataStore" );
    String InputName = parameters.getValue ( "InputName" );
    if ( InputName == null ) {
        // Set to empty string so check to facilitate processing...
        InputName = "";
    }
    String Alias = parameters.getValue ( "Alias" );
    String ColumnProperties = parameters.getValue ( "ColumnProperties" );
    StringDictionary columnProperties = new StringDictionary(ColumnProperties,":",",");
    String Properties = parameters.getValue ( "Properties" );
    Hashtable properties = null;
    if ( (Properties != null) && (Properties.length() > 0) && (Properties.indexOf(":") > 0) ) {
        properties = new Hashtable();
        // First break map pairs by comma
        List<String> pairs = new ArrayList<String>();
        if ( Properties.indexOf(",") > 0 ) {
            pairs = StringUtil.breakStringList(Properties, ",", 0 );
        }
        else {
            pairs.add(Properties);
        }
        // Now break pairs and put in hashtable
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            properties.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String IfNotFound = parameters.getValue("IfNotFound");
    if ( (IfNotFound == null) || IfNotFound.equals("")) {
        IfNotFound = _Warn; // default
    }
    String DefaultUnits = parameters.getValue("DefaultUnits");
    String TimeSeriesCountProperty = parameters.getValue ( "TimeSeriesCountProperty" );
    String TimeSeriesIndex1Property = parameters.getValue ( "TimeSeriesIndex1Property" );
    
    // Get the table to process.

    DataTable table = null;
    int locationTypeColumnNum = -1, locationColumnNum = -1, dataSourceColumnNum = -1, dataTypeColumnNum = -1, dataStoreColumnNum = -1;
    if ( commandPhase == CommandPhaseType.RUN ) {
        PropList request_params = null;
        CommandProcessorRequestResultsBean bean = null;
        if ( (TableID != null) && !TableID.equals("") ) {
            // Get the table to be updated
            request_params = new PropList ( "" );
            request_params.set ( "TableID", TableID );
            try {
                bean = processor.processRequest( "GetTable", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_Table = bean_PropList.getContents ( "Table" );
            if ( o_Table == null ) {
                message = "Unable to find table to process using TableID=\"" + TableID + "\".";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a table exists with the requested ID." ) );
            }
            else {
                table = (DataTable)o_Table;
                if ( (LocationTypeColumn != null) && (LocationTypeColumn.length() > 0) ) {
                    try {
                        locationTypeColumnNum = table.getFieldIndex(LocationTypeColumn);
                    }
                    catch ( Exception e ) {
                        message = "Unable to find location type column \"" + LocationTypeColumn + "\" for TableID=\"" + TableID + "\".";
                        Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a table exists with the column \"" + LocationTypeColumn + "\"." ) );
                    }
                }
                try {
                    locationColumnNum = table.getFieldIndex(LocationColumn);
                }
                catch ( Exception e ) {
                    message = "Unable to find location column \"" + LocationColumn + "\" for TableID=\"" + TableID + "\".";
                    Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                    status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that a table exists with the column \"" + LocationColumn + "\"." ) );
                }
                if ( (DataSourceColumn != null) && (DataSourceColumn.length() > 0) ) {
                    try {
                        dataSourceColumnNum = table.getFieldIndex(DataSourceColumn);
                    }
                    catch ( Exception e ) {
                        message = "Unable to find data source column \"" + DataSourceColumn + "\" for TableID=\"" + TableID + "\".";
                        Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a table exists with the column \"" + DataSourceColumn + "\"." ) );
                    }
                }
                if ( (DataTypeColumn != null) && (DataTypeColumn.length() > 0) ) {
                    try {
                        dataTypeColumnNum = table.getFieldIndex(DataTypeColumn);
                    }
                    catch ( Exception e ) {
                        message = "Unable to find data type column \"" + DataTypeColumn + "\" for TableID=\"" + TableID + "\".";
                        Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a table exists with the column \"" + DataTypeColumn + "\"." ) );
                    }
                }
                if ( (DataStoreColumn != null) && (DataStoreColumn.length() > 0) ) {
                    try {
                        dataStoreColumnNum = table.getFieldIndex(DataStoreColumn);
                    }
                    catch ( Exception e ) {
                        message = "Unable to find datastore column \"" + DataStoreColumn + "\" for TableID=\"" + TableID + "\".";
                        Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a table exists with the column \"" + DataStoreColumn + "\"." ) );
                    }
                }
            }
        }
    }
    
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings for command parameters.";
        Message.printWarning ( 2,
        MessageUtil.formatMessageTag(command_tag, ++warning_count),
        routine,message);
        throw new InvalidCommandParameterException ( message );
    }
    
	// Process the rows in the table and read time series.
    List<TS> tslist = new ArrayList<TS>(); // Keep the list of time series
    String dataSource;
	try {
        boolean readData = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ){
            readData = false;
        }
        else if ( commandPhase == CommandPhaseType.RUN ){
            // TODO SAM 2013-05-17 Need to determine whether to read table even in discovery mode
            // Otherwise won't be able to generate time series in discovery ode
    
            // Loop through the records in the table and match the identifiers...
        
            StringBuffer tsidentString = new StringBuffer();
            TableRecord rec = null;
            String locationID;
            TS ts = null;
            String dataType, locationType, dataStore;
            int tsize = table.getNumberOfRecords();
            List<String> problems = new ArrayList<String>(); // Use for temporary list of problems - need because multiple data sources
            List<String> suggestions = new ArrayList<String>();
            for ( int i = 0; i < tsize; i++ ) {
                problems.clear();
                suggestions.clear();
                rec = table.getRecord ( i );
                // Location type
                if ( locationTypeColumnNum >= 0 ) {
                    locationType = rec.getFieldValueString ( locationTypeColumnNum );
                }
                else {
                    locationType = LocationType;
                }
                locationID = rec.getFieldValueString ( locationColumnNum );
                // Skip blank location identifiers
                if ( (locationID == null) || (locationID.trim().length() == 0) ) {
                    continue;
                }
                // Data type
                if ( dataTypeColumnNum >= 0 ) {
                    dataType = rec.getFieldValueString ( dataTypeColumnNum );
                }
                else {
                    dataType = DataType;
                }
                // Data store
                if ( dataStoreColumnNum >= 0 ) {
                    dataStore = rec.getFieldValueString ( dataStoreColumnNum );
                }
                else {
                    dataStore = DataStore;
                }
                // Allow more than one data source to be specified, which is useful when there is mixed ownership of stations
                int nDataSource = 1;
                if ( dataSourceList.length > 0 ) {
                    nDataSource = dataSourceList.length;
                }
                boolean notFoundLogged = false; // Used to handle read exceptions vs. no time series found
                for ( int iDataSource = 0; iDataSource < nDataSource; iDataSource++ ) {
                    tsidentString.setLength(0);
                    if ( dataSourceList.length == 0 ) {
                        if ( dataSourceColumnNum >= 0 ) {
                            // Get the data source from the table
                            dataSource = rec.getFieldValueString ( dataSourceColumnNum );
                        }
                        else {
                            dataSource = "";
                        }
                    }
                    else {
                        dataSource = dataSourceList[iDataSource];
                    }
                    if ( (locationType != null) && (locationType.length() > 0) ) {
                        tsidentString.append ( locationType + TSIdent.LOC_TYPE_SEPARATOR );
                    }
                    tsidentString.append ( locationID + "." + dataSource + "." + dataType + "." + Interval + "~" + dataStore );
                    if ( InputName.length() > 0 ) {
                        tsidentString.append ( "~" + InputName );
                    }
                    String tsid = tsidentString.toString();;
                    // Make a request to the processor to read a time series...
                    notifyCommandProgressListeners ( i, tsize, (float)-1.0, "Reading time series " + tsid);
                    PropList request_params = new PropList ( "" );
                    request_params.set ( "TSID", tsidentString.toString() );
                    request_params.setUsingObject ( "WarningLevel", new Integer(warning_level) );
                    request_params.set ( "CommandTag", command_tag );
                    if ( iDataSource == (nDataSource - 1) ) {
                        // Only default on the last data source
                        request_params.set ( "IfNotFound", IfNotFound );
                    }
                    request_params.setUsingObject ( "ReadData", new Boolean(readData) );
                    CommandProcessorRequestResultsBean bean = null;
                    try {
                        bean = processor.processRequest( "ReadTimeSeries", request_params);
                        PropList bean_PropList = bean.getResultsPropList();
                        Object o_TS = bean_PropList.getContents ( "TS" );
                        if ( o_TS != null ) {
                            ts = (TS)o_TS;
                        }
                    }
                    catch ( Exception e ) {
                        if ( iDataSource == (nDataSource - 1) ) {
                            // Last attempt in a list of data sources
                            problems.add("Time series could not be found using identifier \"" + tsid + "\" (" + e + ")");
                            suggestions.add("Verify that the identifier information is correct.");
                        }
                        else {
                            // Going to try another data source, or defaulting time series.
                            problems.add("Time series could not be found using identifier \"" + tsid + "\" (" + e + ") - will try next data source");
                            suggestions.add("Verify that the identifier information is correct.");
                        }
                        ts = null;
                        notFoundLogged = true;
                    }
                    if ( ts == null ) {
                        if ( !notFoundLogged ) {
                            // Only want to include a warning once so don't duplicate above exception
                            if ( iDataSource == (nDataSource - 1) ) {
                                problems.add("Time series could not be found using identifier \"" + tsid + "\".");
                                suggestions.add("Verify that the identifier information is correct.");
                            }
                            else {
                                // Non-fatal - ignoring or defaulting time series.
                                problems.add("Time series could not be found using identifier \"" + tsid + "\" - will try next data source.");
                                suggestions.add("Verify that the identifier information is correct.");
                            }
                        }
                    }
                    else {
                        // Found a time series so break out of data source loop.
                        break;
                    }
                }
                // Now have processed all data sources.  If no time series was found and a default is to be assigned, do it
                if ( ts == null ) {
                    if ( IfNotFound.equalsIgnoreCase(_Ignore) ) {
                        // Just continue, but do add warnings to the log
                        for ( int ip = 0; ip < problems.size(); ++ip ) {
                            Message.printWarning ( warning_level,
                                MessageUtil.formatMessageTag( command_tag, ++warning_count ),
                                routine, problems.get(ip) );
                        }
                        continue;
                    }
                    else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
                        // Warn and continue
                        for ( int ip = 0; ip < problems.size(); ++ip ) {
                            Message.printWarning ( warning_level,
                                MessageUtil.formatMessageTag( command_tag, ++warning_count ),
                                routine, problems.get(ip) );
                            status.addToLog(commandPhase,
                                new CommandLogRecord( CommandStatusType.FAILURE, problems.get(ip),suggestions.get(ip)));
                        }
                        continue;
                    }
                    else if ( IfNotFound.equalsIgnoreCase(_Ignore) ) {
                        // A common problem is that the output period was not set so the time series could not be defaulted
                        message = "Attempt to use default timee series failed.";
                        Message.printWarning ( warning_level,
                            MessageUtil.formatMessageTag( command_tag, ++warning_count ),
                            routine, message );
                        status.addToLog(commandPhase,
                            new CommandLogRecord( CommandStatusType.FAILURE, message, "Make sure that OutputStart and OutputEnd are set."));
                        continue;
                    }
                }
                // If here have a time series to process further and return
                if ( (DefaultUnits != null) && (ts.getDataUnits().length() == 0) ) {
                    // Time series has no units so assign default.
                    ts.setDataUnits ( DefaultUnits );
                }
                if ( (ts != null) && (Alias != null) && !Alias.equals("") ) {
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, Alias, status, commandPhase);
                    ts.setAlias ( alias );
                }
                if ( columnProperties != null ) {
                    // Set time series properties based on column values
                    LinkedHashMap<String,String> map = columnProperties.getLinkedHashMap();
                    for ( Map.Entry<String,String> entry: map.entrySet() ) {
                        String columnName = entry.getKey();
                        if ( columnName.equals("*") ) {
                            // Set all the table columns as properties, including null values
                            for ( int icol = 0; icol < table.getNumberOfFields(); icol++ ) {
                                ts.setProperty( table.getFieldName(icol), rec.getFieldValue(icol) );
                            }
                        }
                        else {
                            // Else only set the specified columns
                            String propertyName = entry.getValue();
                            int columnNum = -1;
                            try {
                                columnNum = table.getFieldIndex(columnName);
                            }
                            catch ( Exception e ) {
                            }
                            if ( propertyName.equals("*") ) {
                                // Get the property name from the table
                                propertyName = columnName;
                            }
                            if ( columnNum >= 0 ) {
                                // Set even if null
                                ts.setProperty( propertyName, rec.getFieldValue(columnNum) );
                            }
                            else {
                                ts.setProperty( propertyName, null );
                            }
                        }
                    }
                }
                if ( properties != null ) {
                    // Assign properties
                    Enumeration keys = properties.keys();
                    String key = null;
                    while ( keys.hasMoreElements() ) {
                        key = (String)keys.nextElement();
                        ts.setProperty( key, TSCommandProcessorUtil.expandTimeSeriesMetadataString (
                            processor, ts, (String)properties.get(key), status, CommandPhaseType.RUN) );
                    }
                }
                if ( TimeSeriesIndex1Property != null ) {
                    // Set a property indicating the position in the list
                    ts.setProperty(TimeSeriesIndex1Property, new Integer((i + 1)) );
                }
                tslist.add ( ts );
            }
        }
    }
	catch ( Exception e ) {
		message = "Unexpected error reading time series for table \"" + TableID + "\" (" + e + ")";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count ),
			routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(commandPhase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,"Check the log file for details."));
		throw new CommandException ( message );
	}
    
    if ( commandPhase == CommandPhaseType.RUN ) {
        if ( tslist != null ) {
            // Now add the list in the processor...
            
            int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
            if ( wc2 > 0 ) {
                message = "Error adding time series after read.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
            // Set the property indicating the number of rows in the table
            if ( (TimeSeriesCountProperty != null) && !TimeSeriesCountProperty.equals("") ) {
                PropList request_params = new PropList ( "" );
                request_params.setUsingObject ( "PropertyName", TimeSeriesCountProperty );
                request_params.setUsingObject ( "PropertyValue", new Integer(tslist.size()) );
                try {
                    processor.processRequest( "SetProperty", request_params);
                }
                catch ( Exception e ) {
                    message = "Error requesting SetProperty(Property=\"" + TimeSeriesCountProperty + "\") from processor.";
                    Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                }
            }
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( tslist );
    }

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count ), routine, message );
		throw new CommandWarningException ( message );
	}
    
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

    String TableID = props.getValue ( "TableID" );
    String LocationTypeColumn = props.getValue ( "LocationTypeColumn" );
    String LocationType = props.getValue ( "LocationType" );
    String LocationColumn = props.getValue ( "LocationColumn" );
    String DataSourceColumn = props.getValue ( "DataSourceColumn" );
    String DataSource = props.getValue ( "DataSource" );
    String DataTypeColumn = props.getValue ( "DataTypeColumn" );
    String DataType = props.getValue ( "DataType" );
    String Interval = props.getValue ( "Interval" );
    String Scenario = props.getValue ( "Scenario" );
    String DataStoreColumn = props.getValue ( "DataStoreColumn" );
    String DataStore = props.getValue ( "DataStore" );
    String InputName = props.getValue ( "InputName" );
    String Alias = props.getValue ( "Alias" );
    String ColumnProperties = props.getValue ( "ColumnProperties" );
    String Properties = props.getValue ( "Properties" );
    String IfNotFound = props.getValue ( "IfNotFound" );
    String DefaultUnits = props.getValue ( "DefaultUnits" );
    String TimeSeriesCountProperty = props.getValue ( "TimeSeriesCountProperty" );
    String TimeSeriesIndex1Property = props.getValue ( "TimeSeriesIndex1Property" );

	StringBuffer b = new StringBuffer ();

	if ((TableID != null) && (TableID.length() > 0)) {
		b.append("TableID=\"" + TableID + "\"");
	}
    if ((LocationTypeColumn != null) && (LocationTypeColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("LocationTypeColumn=\"" + LocationTypeColumn + "\"");
    }
    if ((LocationType != null) && (LocationType.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("LocationType=\"" + LocationType + "\"");
    }
    if ((LocationColumn != null) && (LocationColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("LocationColumn=\"" + LocationColumn + "\"");
    }
    if ((DataSourceColumn != null) && (DataSourceColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DataSourceColumn=\"" + DataSourceColumn + "\"");
    }
	if ((DataSource != null) && (DataSource.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("DataSource=\"" + DataSource + "\"");
	}
    if ((DataTypeColumn != null) && (DataTypeColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DataTypeColumn=\"" + DataTypeColumn + "\"");
    }
    if ((DataType != null) && (DataType.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DataType=\"" + DataType + "\"");
    }
    if ((Interval != null) && (Interval.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Interval=\"" + Interval + "\"");
    }
    if ((Scenario != null) && (Scenario.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Scenario=\"" + Scenario + "\"");
    }
    if ((DataStoreColumn != null) && (DataStoreColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DataStoreColumn=\"" + DataStoreColumn + "\"");
    }
    if ((DataStore != null) && (DataStore.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DataStore=\"" + DataStore + "\"");
    }
    if ((InputName != null) && (InputName.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("InputName=\"" + InputName + "\"");
    }
    if ((Alias != null) && (Alias.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Alias=\"" + Alias + "\"");
    }
    if ((ColumnProperties != null) && (ColumnProperties.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("ColumnProperties=\"" + ColumnProperties + "\"");
    }
    if ((Properties != null) && (Properties.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Properties=\"" + Properties + "\"");
    }
    if ((IfNotFound != null) && (IfNotFound.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("IfNotFound=" + IfNotFound );
    }
    if ((DefaultUnits != null) && (DefaultUnits.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DefaultUnits=\"" + DefaultUnits + "\"");
    }
    if ((TimeSeriesCountProperty != null) && (TimeSeriesCountProperty.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("TimeSeriesCountProperty=\"" + TimeSeriesCountProperty + "\"");
    }
    if ((TimeSeriesIndex1Property != null) && (TimeSeriesIndex1Property.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("TimeSeriesIndex1Property=\"" + TimeSeriesIndex1Property + "\"");
    }

	return getCommandName() + "(" + b.toString() + ")";
}

}