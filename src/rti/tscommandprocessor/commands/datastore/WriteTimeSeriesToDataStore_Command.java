package rti.tscommandprocessor.commands.datastore;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import riverside.datastore.GenericDatabaseDataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.DMI.DMI;
import RTi.DMI.DMIDeleteStatement;
import RTi.DMI.DMISelectStatement;
import RTi.DMI.DMIUtil;
import RTi.DMI.DMIWriteModeType;
import RTi.DMI.DMIWriteStatement;
import RTi.DMI.DatabaseDataStore;
import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSIterator;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the WriteTimeSeriesToDataStore() command.
*/
public class WriteTimeSeriesToDataStore_Command extends AbstractCommand implements Command
{

/**
Values for MatchLocationType.
*/
protected final String _DeleteAllThenInsert = "DeleteAllThenInsert";
protected final String _DeletePeriodThenInsert = "DeletePeriodThenInsert";

/**
Prepared statement to re-use when writing time series.
*/
private Hashtable<String, PreparedStatement> __preparedStatementHash = null;

/**
Hashtable of database metadata properties/checks to facilitate rapid checks.
*/
private Hashtable<String,String> __dbMetaHashtable = new Hashtable<String,String>();

/**
Constructor.
*/
public WriteTimeSeriesToDataStore_Command ()
{	super();
	setCommandName ( "WriteTimeSeriesToDataStore" );
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
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String DataStore = parameters.getValue ( "DataStore" );
    String DataStoreMissingValue = parameters.getValue("DataStoreMissingValue" );
	String WriteMode = parameters.getValue ( "WriteMode" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	if ( (OutputStart != null) && !OutputStart.isEmpty() && (OutputStart.indexOf("${") < 0) ) {
		try {	DateTime datetime1 = DateTime.parse(OutputStart);
			if ( datetime1 == null ) {
				throw new Exception ("bad date");
			}
		}
		catch (Exception e) {
			message = "Output start date/time \"" + OutputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid output start date/time." ) );
		}
	}
	if ( (OutputEnd != null) && !OutputEnd.isEmpty() && (OutputEnd.indexOf("${") < 0)) {
		try {	DateTime datetime2 = DateTime.parse(OutputEnd);
			if ( datetime2 == null ) {
				throw new Exception ("bad date");
			}
		}
		catch (Exception e) {
			message = "Output end date/time \"" + OutputEnd + "\" is not a valid date/time.";
			warning += "\n" + message;
				status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid output end date/time." ) );
		}
	}
    if ( (DataStore == null) || (DataStore.length() == 0) ) {
        message = "The datastore must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the datastore." ) );
    }
    if ( (DataStoreMissingValue != null) && !DataStoreMissingValue.equals("") && !StringUtil.isDouble(DataStoreMissingValue) &&
        !DataStoreMissingValue.equalsIgnoreCase("null") ) {
        message = "The missing value \"" + DataStoreMissingValue + "\" is not a number, NaN, or null.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the missing value as a number, NaN, or null." ) );
    }
    if ( (WriteMode != null) && !WriteMode.equals("") ) {
        if ( WriteMode.equalsIgnoreCase(_DeleteAllThenInsert) || WriteMode.equalsIgnoreCase(_DeletePeriodThenInsert)) {
            // OK
        }
        else if ( DMIWriteModeType.valueOfIgnoreCase(WriteMode) == null ) {
            message = "The write mode (" + WriteMode + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid write mode." ) );
        }
    }

	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(15);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
	validList.add ( "OutputStart" );
	validList.add ( "OutputEnd" );
	validList.add ( "DataStore" );
	validList.add ( "DataStoreLocationType" );
	validList.add ( "DataStoreLocationID" );
    validList.add ( "DataStoreDataSource" );
    validList.add ( "DataStoreDataType" );
    validList.add ( "DataStoreInterval" );
    validList.add ( "DataStoreScenario" );
    validList.add ( "DataStoreUnits" );
	validList.add ( "DataStoreMissingValue" );
	validList.add ( "WriteMode" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Check the datastore for a property.  Use a hashtable to look up information about the datastore.
This minimizes heavy processing on database metadata.
@param dbMeta database metadata
@param tableName name to check for existence
@param columnNmae column to check for existence
*/
private String checkDataStoreProperty(DatabaseMetaData dbMeta, String tableName, String columnName, String propertyName,
    String tableNote, String columnNote, List<String> problems )
{
    StringBuffer key = new StringBuffer(tableName);
    boolean checkColumn = false;
    if ( columnName != null ) {
        key.append ( "." + columnName );
        checkColumn = true;
    }
    if ( propertyName != null ) {
        key.append ( "." + propertyName );
    }
    // Try to get the property
    Object o = __dbMetaHashtable.get(key);
    /*
    if ( o == null ) {
        // Have not yet checked so do it...
        if ( checkColumn ) {
            if (!DMIUtil.databaseTableHasColumn(dbMeta, tableName, columnName) ) {
                problems.add ( "Database " + tableNote + " table \"" + timeSeriesMetadataTable +
                    "\" does not contain requested " + columnNote + " column \"" + timeSeriesMetadataTableLocationTypeColumn + "\"" );
            }
        }
        else {
            
        }
    }
    else {
        // Return the value, a string...
        return (String)o;
    }
    */
    return null;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new WriteTimeSeriesToDataStore_JDialog ( parent, this )).ok();
}

/**
Return a prepared statement that has been used previously in the command.
*/
private PreparedStatement getPreparedStatement ( String key )
{
    if ( __preparedStatementHash == null ) {
        return null;
    }
    else {
        return __preparedStatementHash.get(key);
    }
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName()+ ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
	
	// Check whether the processor wants output files to be created...

	CommandProcessor processor = getCommandProcessor();
	if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
		Message.printStatus ( 2, routine,
		"Skipping \"" + toString() + "\" because output is not being created." );
	}
	
	CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // default
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}

	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String DataStore = parameters.getValue ( "DataStore" );
    String DataStoreLocationType = parameters.getValue ( "DataStoreLocationType" );
	if ( (commandPhase == CommandPhaseType.RUN) && (DataStoreLocationType != null) && (DataStoreLocationType.indexOf("${") >= 0) ) {
		DataStoreLocationType = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreLocationType);
	}
    String DataStoreLocationID = parameters.getValue ( "DataStoreLocationID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (DataStoreLocationID != null) && (DataStoreLocationID.indexOf("${") >= 0) ) {
		DataStoreLocationID = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreLocationID);
	}
    String DataStoreDataSource = parameters.getValue ( "DataStoreDataSource" );
	if ( (commandPhase == CommandPhaseType.RUN) && (DataStoreDataSource != null) && (DataStoreDataSource.indexOf("${") >= 0) ) {
		DataStoreDataSource = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreDataSource);
	}
    String DataStoreDataType = parameters.getValue ( "DataStoreDataType" );
	if ( (commandPhase == CommandPhaseType.RUN) && (DataStoreDataType != null) && (DataStoreDataType.indexOf("${") >= 0) ) {
		DataStoreDataType = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreDataType);
	}
    String DataStoreInterval = parameters.getValue ( "DataStoreInterval" );
	if ( (commandPhase == CommandPhaseType.RUN) && (DataStoreInterval != null) && (DataStoreInterval.indexOf("${") >= 0) ) {
		DataStoreInterval = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreInterval);
	}
    String DataStoreScenario = parameters.getValue ( "DataStoreScenario" );
	if ( (commandPhase == CommandPhaseType.RUN) && (DataStoreScenario != null) && (DataStoreScenario.indexOf("${") >= 0) ) {
		DataStoreScenario = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreScenario);
	}
    String DataStoreUnits = parameters.getValue ( "DataStoreUnits" );
	if ( (commandPhase == CommandPhaseType.RUN) && (DataStoreUnits != null) && (DataStoreUnits.indexOf("${") >= 0) ) {
		DataStoreUnits = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreUnits);
	}
    String DataStoreMissingValue = parameters.getValue ( "DataStoreMissingValue" );
    String WriteMode = parameters.getValue ( "WriteMode" );
    DMIWriteModeType writeMode = DMIWriteModeType.valueOfIgnoreCase(WriteMode);
    if ( writeMode == null ) {
        writeMode = DMIWriteModeType.UPDATE_INSERT; // default
    }
    
    // Find the datastore to use...
    DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
        DataStore, DatabaseDataStore.class );
    DMI dmi = null;
    DatabaseMetaData dbMeta = null;
    if ( dataStore == null ) {
        message = "Could not get datastore for name \"" + DataStore + "\" to query data.";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a database connection has been opened with name \"" +
                DataStore + "\"." ) );
    }
    else {
        dmi = ((DatabaseDataStore)dataStore).getDMI();
        // Get database metadata once up front to improve performance
        try {
            dbMeta = dmi.getConnection().getMetaData();
        }
        catch ( Exception e ) {
            message = "Could not get database metadata for datastore \"" + DataStore + "\" - cannot work with database.";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a database connection has been opened with name \"" +
                    DataStore + "\"." ) );
        }
    }
    
	String OutputStart = parameters.getValue ( "OutputStart" );
	if ( (OutputStart == null) || OutputStart.isEmpty() ) {
		OutputStart = "${OutputStart}"; // Default global property
	}
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	if ( (OutputEnd == null) || OutputEnd.isEmpty() ) {
		OutputEnd = "${OutputEnd}"; // Default global property
	}
	DateTime OutputStart_DateTime = null;
	DateTime OutputEnd_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			OutputStart_DateTime = TSCommandProcessorUtil.getDateTime ( OutputStart, "OutputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			OutputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( OutputEnd, "OutputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
	}
    
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings for command parameters.";
        Message.printWarning ( 2,
        MessageUtil.formatMessageTag(command_tag, ++warning_count),
        routine,message);
        throw new InvalidCommandParameterException ( message );
    }

	// Get the time series to process...
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
	CommandProcessorRequestResultsBean bean = null;
	try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		status.addToLog ( commandPhase,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	List<TS> tslist = null;
	if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( commandPhase,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report problem to software support." ) );
	}
	else {
    	tslist = (List<TS>)o_TSList;
    	if ( tslist.size() == 0 ) {
            message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
    		Message.printWarning ( warning_level,
    		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
    		status.addToLog ( commandPhase,
    				new CommandLogRecord(CommandStatusType.WARNING,
    						message, "Confirm that time series are available (may be OK for partial run)." ) );
    	}
	}
	
	List<String> problems = new ArrayList<String>();
    if ( (tslist != null) && (tslist.size() > 0) ) {
        int nts = tslist.size();
        int its = -1;
        for ( TS ts : tslist ) {
            ++its;
            problems.clear();
            message = "Writing time series " + its + " of " + nts;
            notifyCommandProgressListeners ( its, nts, (float)-1.0, message );
            try {
                // Convert to an absolute path...
                Message.printStatus ( 2, routine, "Writing time series " +
                    ts.getIdentifier().toStringAliasAndTSID() + " to datastore \"" + DataStore + "\"" );
                writeTimeSeries ( its, ts, OutputStart_DateTime, OutputEnd_DateTime, dataStore, dmi, dbMeta,
                    DataStoreLocationType, DataStoreLocationID, DataStoreDataSource, DataStoreDataType, DataStoreInterval,
                    DataStoreScenario, DataStoreUnits, DataStoreMissingValue, writeMode, WriteMode, problems );
                for ( String problem : problems ) {
                    Message.printWarning ( 3, routine, problem );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            problem, "Check log file for details." ) );
                }
            }
            catch ( Exception e ) {
                message = "Unexpected error writing time series " + ts.getIdentifier().toStringAliasAndTSID() +
                    " to datastore \"" + DataStore + "\""+ "\" (" + e + ")";
                Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
                Message.printWarning ( 3, routine, e );
                status.addToLog ( commandPhase,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    						message, "Check log file for details." ) );
                throw new CommandException ( message );
            }
        }
    }
	
	status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the prepared statement used to write time series, reused for each time series written.
@param ps PreparedStatement used to write time series.
*/
private void setPreparedStatement ( String key, PreparedStatement ps )
{
    if ( __preparedStatementHash == null ) {
        __preparedStatementHash = new Hashtable<String,PreparedStatement>();
    }
    __preparedStatementHash.put(key,ps);
}

/**
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
    String TSList = parameters.getValue ( "TSList" );
    String TSID = parameters.getValue( "TSID" );
    String EnsembleID = parameters.getValue( "EnsembleID" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String DataStore = parameters.getValue ( "DataStore" );
	String DataStoreLocationType = parameters.getValue( "DataStoreLocationType" );
	String DataStoreLocationID = parameters.getValue( "DataStoreLocationID" );
    String DataStoreDataSource = parameters.getValue( "DataStoreDataSource" );
    String DataStoreDataType = parameters.getValue( "DataStoreDataType" );
    String DataStoreInterval = parameters.getValue( "DataStoreInterval" );
    String DataStoreScenario= parameters.getValue( "DataStoreScenario" );
    String DataStoreUnits = parameters.getValue( "DataStoreUnits" );
	String DataStoreMissingValue = parameters.getValue("DataStoreMissingValue");
	String WriteMode = parameters.getValue( "WriteMode" );
	StringBuffer b = new StringBuffer ();
    if ( (TSList != null) && (TSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSList=" + TSList );
    }
    if ( (TSID != null) && (TSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSID=\"" + TSID + "\"" );
    }
    if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
    }
    if ( (OutputStart != null) && (OutputStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputStart=\"" + OutputStart + "\"" );
    }
	if ( (OutputEnd != null) && (OutputEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputEnd=\"" + OutputEnd + "\"" );
	}
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
    if ( (DataStoreLocationType != null) && (DataStoreLocationType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreLocationType=\"" + DataStoreLocationType + "\"" );
    }
    if ( (DataStoreLocationID != null) && (DataStoreLocationID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreLocationID=\"" + DataStoreLocationID + "\"");
    }
    if ( (DataStoreDataSource != null) && (DataStoreDataSource.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreDataSource=\"" + DataStoreDataSource + "\"");
    }
    if ( (DataStoreDataType != null) && (DataStoreDataType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreDataType=\"" + DataStoreDataType + "\"");
    }
    if ( (DataStoreInterval != null) && (DataStoreInterval.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreInterval=\"" + DataStoreInterval + "\"" );
    }
    if ( (DataStoreScenario != null) && (DataStoreScenario.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreScenario=\"" + DataStoreScenario + "\"");
    }
    if ( (DataStoreUnits != null) && (DataStoreUnits.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreUnits=\"" + DataStoreUnits + "\"" );
    }
    if ( (DataStoreMissingValue != null) && (DataStoreMissingValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreMissingValue=" + DataStoreMissingValue );
    }
    if ( (WriteMode != null) && (WriteMode.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WriteMode=" + WriteMode );
    }

	return getCommandName() + "(" + b.toString() + ")";
}

/**
Write a single time series to the datastore.
@param its count of time series being processed, checked to initialize prepared statement.
@param ts time series to write
@param outputStart start of period to write
@param outputEnd end of period to write
@param dataStore DataStore to write to
@param dmi database DMI to write to
@param dbMeta database metadata for the database being written, used to map data
@param dataStoreLocationType if specified, use instead of the time series location type when determining the database time series to write
@param dataStoreLocationID if specified, use instead of the time series location ID when determining the database time series to write
@param dataStoreDataSource if specified, use instead of the time series data source when determining the database time series to write
@param dataStoreDataSource if specified, use instead of the time series data type when determining the database time series to write
@param dataStoreDataSource if specified, use instead of the time series interval when determining the database time series to write
@param dataStoreDataSource if specified, use instead of the time series scenario when determining the database time series to write
@param missingValue missing value for floating point values, as a string to allow "null" and "NaN" to be specified literally
@param writeMode mode to write data records
@param writeModeString write mode string write mode as string, for messaging
@param problems list of strings to be populated with problems if they occur
*/
private void writeTimeSeries ( int its, TS ts, DateTime outputStart, DateTime outputEnd,
    DataStore dataStore, DMI dmi, DatabaseMetaData dbMeta, String dataStoreLocationType, String dataStoreLocationID,
    String dataStoreDataSource, String dataStoreDataType,
    String dataStoreInterval, String dataStoreScenario, String dataStoreUnits, String missingValue,
    DMIWriteModeType writeMode, String writeModeString, List<String> problems )
{   String routine = "WriteTimeSeriesToDataStore.writeTimeSeries";
    // Get the properties necessary to map the time series to the database
    // Expand the properties based on time series internal properties
    CommandProcessor processor = this.getCommandProcessor();
    CommandStatus status = this.getCommandStatus();
    // Booleans to help simplify logic below
    boolean dsMetaHasLocationType = false;
    // location, and metadataID columns are required
    boolean dsMetaHasDataSource = false;
    boolean dsMetaHasDataType = false;
    boolean dsMetaHasInterval = false;
    boolean dsMetaHasScenario = false;
    //  metadataID, date/time, and value columns are required
    boolean dsDataHasFlag = false;
    // Metadata table...
    String timeSeriesMetadataTable = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_META_TABLE_PROP ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableLocationTypeColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCTYPE_COLUMN_PROP ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableLocationIdColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_META_TABLE_LOCATIONID_COLUMN_PROP ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableDataSourceColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATASOURCE_COLUMN_PROP ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableDataTypeColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATATYPE_COLUMN_PROP ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableDataIntervalColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_META_TABLE_DATAINTERVAL_COLUMN_PROP ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableScenarioColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_META_TABLE_SCENARIO_COLUMN_PROP ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableMetadataIDColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_META_TABLE_ID_COLUMN_PROP ),
        status, CommandPhaseType.RUN );
    // Data table...
    String timeSeriesDataTable = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_DATA_TABLE_PROP ),
        status, CommandPhaseType.RUN );
    String timeSeriesDataTableMetadataIDColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_DATA_TABLE_METAID_COLUMN_PROP ),
        status, CommandPhaseType.RUN );
    String timeSeriesDataTableDateTimeColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_DATA_TABLE_DATETIME_COLUMN_PROP ),
        status, CommandPhaseType.RUN );
    String timeSeriesDataTableValueColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_DATA_TABLE_VALUE_COLUMN_PROP ),
        status, CommandPhaseType.RUN );
    String timeSeriesDataTableFlagColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( GenericDatabaseDataStore.TS_DATA_TABLE_FLAG_COLUMN_PROP ),
        status, CommandPhaseType.RUN );
    // Make sure the metadata table and columns exist...
    try {
        if ( !DMIUtil.databaseHasTable(dbMeta, timeSeriesMetadataTable) ) {
            problems.add ( "Database does not contain requested time series metadata table \"" + timeSeriesMetadataTable + "\"" );
        }
        // Location type column is optional
        if ( (timeSeriesMetadataTableLocationTypeColumn != null) && !timeSeriesMetadataTableLocationTypeColumn.equals("") ) {
            //if ( checkDataStoreProperty(timeSeriesMetadataTable,timeSeriesMetadataTableLocationTypeColumn) == null ) {
            if (!DMIUtil.databaseTableHasColumn(dbMeta, timeSeriesMetadataTable, timeSeriesMetadataTableLocationTypeColumn) ) {
                problems.add ( "Database time series metadata table \"" + timeSeriesMetadataTable +
                    "\" does not contain requested location type column \"" + timeSeriesMetadataTableLocationTypeColumn + "\"" );
            }
            else {
                dsMetaHasLocationType = true;
            }
        }
        if ( !DMIUtil.databaseTableHasColumn(dbMeta, timeSeriesMetadataTable, timeSeriesMetadataTableLocationIdColumn) ) {
            problems.add ( "Database time series metadata table \"" + timeSeriesMetadataTable +
                "\" does not contain requested location ID column \"" + timeSeriesMetadataTableLocationIdColumn + "\"" );
        }
        if ( (timeSeriesMetadataTableDataSourceColumn != null) && !timeSeriesMetadataTableDataSourceColumn.equals("") ) {
            if ( !DMIUtil.databaseTableHasColumn(dbMeta, timeSeriesMetadataTable, timeSeriesMetadataTableDataSourceColumn) ) {
                problems.add ( "Database time series metadata table \"" + timeSeriesMetadataTable +
                    "\" does not contain requested data source column \"" + timeSeriesMetadataTableDataSourceColumn + "\"" );
            }
            else {
                dsMetaHasDataSource = true;
            }
        }
        if ( (timeSeriesMetadataTableDataTypeColumn != null) && !timeSeriesMetadataTableDataTypeColumn.equals("") ) {
            if ( !DMIUtil.databaseTableHasColumn(dbMeta, timeSeriesMetadataTable, timeSeriesMetadataTableDataTypeColumn) ) {
                problems.add ( "Database time series metadata table \"" + timeSeriesMetadataTable +
                    "\" does not contain requested data type column \"" + timeSeriesMetadataTableDataTypeColumn + "\"" );
            }
            else {
                dsMetaHasDataType = true;
            }
        }
        if ( (timeSeriesMetadataTableDataIntervalColumn != null) && !timeSeriesMetadataTableDataIntervalColumn.equals("") ) {
            if ( !DMIUtil.databaseTableHasColumn(dbMeta, timeSeriesMetadataTable, timeSeriesMetadataTableDataIntervalColumn) ) {
                problems.add ( "Database time series metadata table \" + timeSeriesMetadataTable +" +
                    "\" does not contain requested data interval column \"" + timeSeriesMetadataTableDataIntervalColumn + "\"" );
            }
            else {
                dsMetaHasInterval = true;
            }
        }
        if ( (timeSeriesMetadataTableScenarioColumn != null) && !timeSeriesMetadataTableScenarioColumn.equals("") ) {
            if ( !DMIUtil.databaseTableHasColumn(dbMeta, timeSeriesMetadataTable, timeSeriesMetadataTableScenarioColumn) ) {
                problems.add ( "Database time series metadata table \"" + timeSeriesMetadataTable +
                    "\" does not contain requested scenario column \"" + timeSeriesMetadataTableScenarioColumn + "\"" );
            }
            else {
                dsMetaHasScenario = true;
            }
        }
    }
    catch ( Exception e ) {
        problems.add ( "Error checking database time series metadata table and columns (" + e + ")." );
    }
    // Make sure the data table and columns exist...
    try {
        if ( !DMIUtil.databaseHasTable(dbMeta, timeSeriesDataTable) ) {
            problems.add ( "Database does not contain requested time series data table \"" + timeSeriesDataTable + "\"" );
        }
        if ( !DMIUtil.databaseTableHasColumn(dbMeta, timeSeriesDataTable, timeSeriesDataTableMetadataIDColumn) ) {
            problems.add ( "Database time series data table \"" + timeSeriesDataTable +
                "\" does not contain requested metadata ID column \"" + timeSeriesDataTableMetadataIDColumn + "\"" );
        }
        // The time series data table can match a table or can be specified as a dictionary with syntax
        // DataTable1:DateColumn1,DataTable2:DateColumn2
        if ( timeSeriesDataTableDateTimeColumn.indexOf(":") > 0 ) {
            // Need to parse out the dictionary
            String [] parts = timeSeriesDataTableDateTimeColumn.split(",");
            for ( int ipart = 0; ipart < parts.length; ipart++ ) {
                String [] parts2 = parts[ipart].split(":");
                if ( timeSeriesDataTable.equalsIgnoreCase(parts2[0]) ) {
                    timeSeriesDataTableDateTimeColumn = parts2[1];
                }
            }
        }
        if ( !DMIUtil.databaseTableHasColumn(dbMeta, timeSeriesDataTable, timeSeriesDataTableDateTimeColumn) ) {

            problems.add ( "Database time series data table \"" + timeSeriesDataTable +
                "\" does not contain requested date/time column \"" + timeSeriesDataTableDateTimeColumn + "\"" );
        }
        if ( !DMIUtil.databaseTableHasColumn(dbMeta, timeSeriesDataTable, timeSeriesDataTableValueColumn) ) {
            problems.add ( "Database time series data table \"" + timeSeriesDataTable +
                "\" does not contain requested value column \"" + timeSeriesDataTableValueColumn + "\"" );
        }
        if ( (timeSeriesDataTableFlagColumn != null) && !timeSeriesDataTableFlagColumn.equals("") ) {
            if ( !DMIUtil.databaseTableHasColumn(dbMeta, timeSeriesDataTable, timeSeriesDataTableFlagColumn) ) {
                problems.add ( "Database time series data table \"" + timeSeriesDataTable +
                    "\" does not contain requested flag column \"" + timeSeriesDataTableFlagColumn + "\"" );
            }
            else {
                dsDataHasFlag = true;
            }
        }
    }
    catch ( Exception e ) {
        problems.add ( "Error checking database time series data table and columns (" + e + ")." );
    }
    // Check for the missing value
    Double missingValueDouble = null;
    boolean missingValueUseNull = false;
    if ( (missingValue != null) && !missingValue.equals("") ) {
        if ( missingValue.equalsIgnoreCase("NaN") ) {
            missingValueDouble = Double.NaN;
        }
        else if ( missingValue.equalsIgnoreCase("null") ) {
            missingValueUseNull = true;
        }
        else {
            missingValueDouble = Double.parseDouble(missingValue);
        }
    }
    if ( problems.size() > 0 ) {
        return;
    }
    // Get the metadata ID value from the metadata parts
    int timeSeriesMetadataTableMetadataID = -1;
    DMISelectStatement ss = new DMISelectStatement(dmi);
    ss.addTable(timeSeriesMetadataTable);
    // Add the values necessary to match time series metadata
    // Metadata is main value of interest so add first and process first in resultset
    ss.addField(timeSeriesMetadataTableMetadataIDColumn);
    try {
        ss.addValue(timeSeriesMetadataTableMetadataID);
    }
    catch ( Exception e ) {
        problems.add ( "Error adding metadata ID " + timeSeriesMetadataTableMetadataID + " to select statement (" + e + ")."); 
    }
    try {
        // Location type is optional and may not have been configured for datastore
        if ( dsMetaHasLocationType ) {
            ss.addField(timeSeriesMetadataTableLocationTypeColumn);
            if ( (dataStoreLocationType != null) && !dataStoreLocationType.equals("") ) {
                // Location type specified by command parameter
                ss.addWhereClause(timeSeriesMetadataTableLocationTypeColumn + "='" +
                    TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts,
                         dataStoreLocationType, status, CommandPhaseType.RUN) + "'");
            }
            else {
                // Use time series location type
                ss.addWhereClause(timeSeriesMetadataTableLocationTypeColumn + "='" + ts.getIdentifier().getLocationType() + "'" );
            }
        }
        // Location ID is required
        ss.addField(timeSeriesMetadataTableLocationIdColumn);
        if ( (dataStoreLocationID != null) && !dataStoreLocationID.equals("") ) {
            // Location ID specified by command parameter
            ss.addWhereClause(timeSeriesMetadataTableLocationIdColumn + "='" +
                TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, dataStoreLocationID, status,
                     CommandPhaseType.RUN) + "'");
        }
        else {
            // Use time series location ID
            ss.addWhereClause(timeSeriesMetadataTableLocationIdColumn + "='" + ts.getLocation() + "'");
        }
        // Data source is optional and may not have been configured for datastore
        if ( dsMetaHasDataSource ) {
            ss.addField(timeSeriesMetadataTableDataSourceColumn);
            if ( (dataStoreDataSource != null) && !dataStoreDataSource.equals("") ) {
                // Data source specified by command parameter
                ss.addWhereClause(timeSeriesMetadataTableDataSourceColumn + "='" +
                    TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, dataStoreDataSource, status,
                         CommandPhaseType.RUN) + "'");
            }
            else {
                // Use time series data source
                ss.addWhereClause(timeSeriesMetadataTableDataSourceColumn + "='" + ts.getIdentifier().getSource() + "'");
            }
        }
        // Data type is optional and may not have been configured for datastore
        if ( dsMetaHasDataType ) {
            ss.addField(timeSeriesMetadataTableDataTypeColumn);
            if ( (dataStoreDataType != null) && !dataStoreDataType.equals("") ) {
                // Data type specified by command parameter
                ss.addWhereClause(timeSeriesMetadataTableDataTypeColumn + "='" +
                    TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, dataStoreDataType, status,
                         CommandPhaseType.RUN) + "'");
            }
            else {
                // use time series data type
                ss.addWhereClause(timeSeriesMetadataTableDataTypeColumn + "='" + ts.getDataType() + "'");
            }
        }
        // Interval is optional and may not have been configured for datastore
        if ( dsMetaHasInterval ) {
            ss.addField(timeSeriesMetadataTableDataIntervalColumn);
            if ( (dataStoreInterval != null) && !dataStoreInterval.equals("") ) {
                // Data interval specified by command parameter
                ss.addWhereClause(timeSeriesMetadataTableDataIntervalColumn + "='" +
                    TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, dataStoreInterval, status,
                         CommandPhaseType.RUN) + "'");
            }
            else {
                // Use time series interval
                ss.addWhereClause(timeSeriesMetadataTableDataIntervalColumn + "='" + ts.getIdentifier().getInterval() + "'");
            }
        }
        // Scenario is optional and may not have been configured for datastore
        if ( dsMetaHasScenario ) {
            ss.addField(timeSeriesMetadataTableScenarioColumn);
            if ( (dataStoreScenario != null) && !dataStoreScenario.equals("") ) {
                // Scenario specified by command parameter
                ss.addWhereClause(timeSeriesMetadataTableScenarioColumn + "='" +
                    TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, dataStoreScenario, status,
                         CommandPhaseType.RUN) + "'");
            }
            else {
                // Use time series scenario
                ss.addWhereClause(timeSeriesMetadataTableScenarioColumn + "='" + ts.getIdentifier().getScenario() + "'");
            }
        }
    }
    catch ( Exception e ) {
        problems.add ( "Error adding where clause to metadata select statement (" + e + ")."); 
    }
    if ( problems.size() > 0 ) {
        return;
    }
    String sqlString = ss.toString();
    ResultSet rs = null;
    try {
        rs = dmi.dmiSelect(ss);
        int rowCount = 0;
        while (rs.next()) {
            ++rowCount;
            timeSeriesMetadataTableMetadataID = rs.getInt(1);
            if (rs.wasNull()) {
                timeSeriesMetadataTableMetadataID = -1;
            }
        }
        if ( rowCount != 1 ) {
            problems.add ( "Was expecting exactly 1 time series metadata record for \"" + ts.getIdentifier().toString() +
                "\", got " + rowCount + " using SQL \"" + sqlString + "\"" ); 
        }
    }
    catch ( Exception e ) {
        problems.add ( "Error reading time series metadata from database for \"" + ts.getIdentifier().toString() +
            "\" with statement \"" + sqlString + "\" (" + e + ")."); 
    }
    finally {
        DMI.closeResultSet(rs);
    }
    if ( problems.size() > 0 ) {
        return;
    }
    // If requested, delete all the records or records in the period.  These options allow writing the data in bulk
    // rather than checking each value
    boolean usePreparedStatement = false;
    if ( (writeModeString != null) && (writeModeString.equalsIgnoreCase(_DeleteAllThenInsert) ||
        writeModeString.equalsIgnoreCase(_DeleteAllThenInsert)) ) {
        usePreparedStatement = true;
        DMIDeleteStatement ds = new DMIDeleteStatement(dmi);
        ds.addTable(timeSeriesDataTable);
        try {
            ds.addWhereClause(timeSeriesDataTableMetadataIDColumn + "=" + timeSeriesMetadataTableMetadataID);
        }
        catch ( Exception e ) {
            problems.add ( "Error adding metadata ID " + timeSeriesMetadataTableMetadataID + " to delete statement (" + e + ")."); 
        }
        if ( writeModeString.equalsIgnoreCase(_DeletePeriodThenInsert) && (outputStart != null) && (outputEnd != null) ) {
            // Constrain the delete to date/time within the specified output period
            Message.printStatus(2,routine,"Deleting time series records " + outputStart + " to " + outputEnd + " for \"" +
                ts.getIdentifier().toStringAliasAndTSID() + "\" before writing data.");
            try {
                ds.addWhereClause(timeSeriesDataTableDateTimeColumn + ">=" + DMIUtil.formatDateTime(dmi, outputStart));
                ds.addWhereClause(timeSeriesDataTableDateTimeColumn + "<=" + DMIUtil.formatDateTime(dmi, outputEnd));
            }
            catch ( Exception e ) {
                problems.add ( "Error adding period to delete statement (" + e + ")."); 
            }
        }
        else {
            Message.printStatus(2,routine,"Deleting all time series records for \"" +
                ts.getIdentifier().toStringAliasAndTSID() + "\" before writing data.");
        }
        sqlString = ds.toString();
        try {
            int rowCount = dmi.dmiDelete(ds);
            //Message.printStatus(2, routine, "Deleted " + rowCount + " rows with statement \"" + dmi.getLastQueryString() + "\".");
        }
        catch ( Exception e ) {
            problems.add ( "Error deleting from database with statement \"" + sqlString + "\" (" + e + ")."); 
        }
        // Reset the write mode for below - OK just to insert since there should be no conflicts
        writeMode = DMIWriteModeType.INSERT;
    }
    if ( problems.size() > 0 ) {
        problems.add ( "Not writing data due to errors." );
        return;
    }
    // Write the time series
    DMIWriteStatement ws;
    TSIterator tsi = null;
    try {
        tsi = ts.iterator(outputStart,outputEnd);
    }
    catch ( Exception e ) {
        problems.add("Unable to get iterator for time series data (" + e + ")." );
        return;
    }
    // Write the time series data records using the metadata ID as the foreign key
    // For now create statements for every time series value.
    // TODO SAM 2013-06-08 Need to optimize by creating a prepared statement
    TSData tsdata;
    double value;
    String flag;
    if ( usePreparedStatement ) {
        // Use a prepared statement to batch all the inserts
        StringBuffer sql = new StringBuffer();
        try {
            sql.append ( "INSERT INTO " + timeSeriesDataTable + "(" + timeSeriesMetadataTableMetadataIDColumn + "," +
                timeSeriesDataTableDateTimeColumn + "," +
                timeSeriesDataTableValueColumn
                );
            if ( dsDataHasFlag ) {
                sql.append ( "," + timeSeriesDataTableFlagColumn );
            }
            sql.append ( ") VALUES (?,?,?" );
            if ( dsDataHasFlag ) {
                sql.append ( ",?" );
            }
            sql.append ( ")" );
            // Check to see if a prepared statement already exists for the table...
            PreparedStatement ps = getPreparedStatement ( sql.toString() );
            if ( ps == null ) {
                // Create and save the prepared statement so it can be used again
                ps = dmi.getConnection().prepareStatement(sql.toString());
                setPreparedStatement(sql.toString(),ps);
            }
            else {
                ps.clearBatch(); // Clear previous load data
            }
            dmi.getConnection().setAutoCommit(false);
            while ( (tsdata = tsi.next()) != null ) {
                ps.setInt(1,timeSeriesMetadataTableMetadataID);
                ps.setInt(2,tsdata.getDate().getYear());
                value = tsdata.getDataValue();
                if ( ts.isDataMissing(value)) {
                    if ( missingValueUseNull ) {
                        ps.setNull(3, java.sql.Types.DOUBLE);
                    }
                    else if ( missingValueDouble != null ) {
                        ps.setDouble(3,missingValueDouble);
                    }
                    else {
                        // Just use the time series value.
                        ps.setDouble(3,value);
                    }
                }
                else {
                    ps.setDouble(3,value);
                }
                if ( dsDataHasFlag ) {
                    flag = tsdata.getDataFlag();
                    ps.setString(4,flag);
                }
                ps.addBatch();
            }
            ps.executeBatch();
            dmi.getConnection().commit();
        }
        catch ( SQLException e ) {
            problems.add ( "Error writing time series \"" + ts.getIdentifier().toStringAliasAndTSID() +
                "\" to database using prepared statement \"" + sql + "\" (" + e + ")."); 
        }
        finally {
            try {
                dmi.getConnection().setAutoCommit(true);
            }
            catch ( SQLException e ) {
                // Swallow.
            }
        }
    }
    else {
        // Use the standard DMI package and process each value
        while ( (tsdata = tsi.next()) != null ) {
            // Create the query.
            sqlString = "Not yet formed";
            ws = new DMIWriteStatement(dmi);
            ws.addTable(timeSeriesDataTable);
            // Add the metadata foreign key value
            ws.addField(timeSeriesMetadataTableMetadataIDColumn);
            try {
                ws.addValue(timeSeriesMetadataTableMetadataID);
            }
            catch ( Exception e ) {
                problems.add ( "Error adding metadata ID " + timeSeriesMetadataTableMetadataID + " to write statement (" + e + ")."); 
            }
            // Add the time series data columns to write to the statement
            ws.addField(timeSeriesDataTableDateTimeColumn);
            try {
                // TODO SAM 2013-06-10 Need some configuration sophistication to know
                // whether writing a date/time, integer (for year), etc.
                // For now hard-code for INSIGHT year as integer
                ws.addValue(tsdata.getDate().getYear());
            }
            catch ( Exception e ) {
                problems.add ( "Error adding date/time \"" + tsdata.getDate() + "\" to write statement (" + e + ")."); 
            }
            ws.addField(timeSeriesDataTableValueColumn);
            try {
                value = tsdata.getDataValue();
                if ( ts.isDataMissing(value) ) {
                    // Handle specifically
                    if ( missingValueUseNull ) {
                        ws.addValue((Double)null);
                    }
                    else if ( missingValueDouble != null ) {
                        ws.addValue(missingValueDouble);
                    }
                    else {
                        // Just use the time series value.
                        ws.addValue(value);
                    }
                }
                else {
                    ws.addValue(value);
                }
            }
            catch ( Exception e ) {
                problems.add ( "Error adding value " + tsdata.getDataValue() + " to write statement (" + e + ")."); 
            }
            if ( dsDataHasFlag ) {
                ws.addField(timeSeriesDataTableFlagColumn);
                try {
                    ws.addValue(tsdata.getDataFlag());
                }
                catch ( Exception e ) {
                    problems.add ( "Error adding flag \"" + tsdata.getDataFlag() + "\" to write statement (" + e + ")."); 
                }
            }
            // Write the data using the statement that was built
            sqlString = ws.toString();
            try {
                int rowCount = dmi.dmiWrite(ws, writeMode.getCode());
                //Message.printStatus(2, routine, "Wrote " + rowCount + " rows with statement \"" + dmi.getLastQueryString() + "\".");
            }
            catch ( Exception e ) {
                problems.add ( "Error writing to database with statement \"" + sqlString + "\" (" + e + ")."); 
            }
        }
    }
}

}