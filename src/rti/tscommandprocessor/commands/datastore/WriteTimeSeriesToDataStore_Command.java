package rti.tscommandprocessor.commands.datastore;

import java.sql.ResultSet;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import RTi.DMI.DMI;
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
protected final String _False = "False";
protected final String _True = "True";

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
    String MissingValue = parameters.getValue("MissingValue" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String DataStore = parameters.getValue ( "DataStore" );
	String MatchLocationType = parameters.getValue ( "MatchLocationType" );
	String WriteMode = parameters.getValue ( "WriteMode" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
    if ( (MissingValue != null) && !MissingValue.equals("") ) {
        if ( !StringUtil.isDouble(MissingValue) ) {
            message = "The missing value \"" + MissingValue + "\" is not a number.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the missing value as a number." ) );
        }
    }

	if ( (OutputStart != null) && !OutputStart.equals("")) {
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
	if ( (OutputEnd != null) && !OutputEnd.equals("")) {
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
    if ( (MatchLocationType != null) && !MatchLocationType.equals("") &&
        !MatchLocationType.equalsIgnoreCase(_False) && !MatchLocationType.equalsIgnoreCase(_True)) {
        message = "The value of MatchLocationType (" + MatchLocationType + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as " + _False + " (default) or " + _True + "." ) );
    }
    if ( (WriteMode != null) && (DMIWriteModeType.valueOf(WriteMode) == null) ) {
        message = "The write mode (" + WriteMode + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid write mode." ) );
    }

	// Check for invalid parameters...
	List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
	valid_Vector.add ( "MissingValue" );
	valid_Vector.add ( "OutputStart" );
	valid_Vector.add ( "OutputEnd" );
	valid_Vector.add ( "DataStore" );
	valid_Vector.add ( "MatchLocationType" );
	valid_Vector.add ( "WriteMode" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
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
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String [] parts = getClass().getName().split(".");
    String routine = parts[parts.length - 1] + ".runCommand", message;
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
	status.clearLog(commandPhase);

	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String MissingValue = parameters.getValue ( "MissingValue" );
    String DataStore = parameters.getValue ( "DataStore" );
    String MatchLocationType = parameters.getValue ( "MatchLocationType" );
    boolean matchLocationType = false;
    if ( (MatchLocationType != null) && MatchLocationType.equalsIgnoreCase("True") ) {
        matchLocationType = true;
    }
    String WriteMode = parameters.getValue ( "WriteMode" );
    DMIWriteModeType writeMode = DMIWriteModeType.valueOfIgnoreCase(WriteMode);
    if ( writeMode == null ) {
        writeMode = DMIWriteModeType.UPDATE_INSERT; // default
    }
    
    // Find the datastore to use...
    DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
        DataStore, DatabaseDataStore.class );
    DMI dmi = null;
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

	String OutputStart = parameters.getValue ( "OutputStart" );
	DateTime OutputStart_DateTime = null;
	if ( OutputStart != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", OutputStart );
		try {
		    bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting DateTime(DateTime=" + OutputStart + ") from processor.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			status.addToLog ( commandPhase,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report problem to software support." ) );
		}
		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for DateTime(DateTime=" + OutputStart +
				"\") returned from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			status.addToLog ( commandPhase,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report problem to software support." ) );
		}
		else {
		    OutputStart_DateTime = (DateTime)prop_contents;
		}
	}
	else {
	    // Get from the processor (can be null)...
		try {
		    Object o_OutputStart = processor.getPropContents ( "OutputStart" );
			if ( o_OutputStart != null ) {
				OutputStart_DateTime = (DateTime)o_OutputStart;
			}
		}
		catch ( Exception e ) {
			message = "Error requesting OutputStart from processor - not using.";
			Message.printDebug(10, routine, message );
			status.addToLog ( commandPhase,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report problem to software support." ) );
		}
	}
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	DateTime OutputEnd_DateTime = null;
	if ( OutputEnd != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", OutputEnd );
		try {
		    bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting DateTime(DateTime=" + OutputEnd + ") from processor.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			status.addToLog ( commandPhase,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report problem to software support." ) );
		}
		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for DateTime(DateTime=" + OutputEnd +
			"\") returned from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			status.addToLog ( commandPhase,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report problem to software support." ) );
		}
		else {
		    OutputEnd_DateTime = (DateTime)prop_contents;
		}
	}
	else {
	    // Get from the processor...
		try {
		    Object o_OutputEnd = processor.getPropContents ( "OutputEnd" );
			if ( o_OutputEnd != null ) {
				OutputEnd_DateTime = (DateTime)o_OutputEnd;
			}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting OutputEnd from processor - not using.";
			Message.printDebug(10, routine, message );
		}
	}

	List<String> problems = new Vector<String>();
    if ( (tslist != null) && (tslist.size() > 0) ) {
        int nts = tslist.size();
        int its = 0;
        for ( TS ts : tslist ) {
            ++its;
            problems.clear();
            message = "Writing time series " + its + " of " + nts;
            notifyCommandProgressListeners ( its, nts, (float)-1.0, message );
            try {
                // Convert to an absolute path...
                Message.printStatus ( 2, routine, "Writing time series " +
                    ts.getIdentifier().toStringAliasAndTSID() + " to datastore \"" + DataStore + "\"" );
                writeTimeSeries ( ts, OutputStart_DateTime, OutputEnd_DateTime, dataStore, dmi,
                    matchLocationType, writeMode, problems );
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
	String MissingValue = parameters.getValue("MissingValue");
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String DataStore = parameters.getValue ( "DataStore" );
	String MatchLocationType = parameters.getValue( "MatchLocationType" );
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
    if ( (MissingValue != null) && (MissingValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MissingValue=" + MissingValue );
    }
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
    if ( (MatchLocationType != null) && (MatchLocationType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MatchLocationType=" + MatchLocationType );
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
@param ts time series to write
@param outputStart start of period to write
@param outputEnd end of period to write
@param dataStore DataStore to write to
@param dmi database DMI to write to
@param matchLocationType indicate whether to match the location type in the prefix - if true then the location ID
in the time series is of the form LocationType:LocationID
@param writeMode mode to write data records
@param list of strings to be populated with problems if they occur
*/
private void writeTimeSeries ( TS ts, DateTime outputStart, DateTime outputEnd,
    DataStore dataStore, DMI dmi, boolean matchLocationType, DMIWriteModeType writeMode, List<String> problems )
{
    // Get the properties necessary to map the time series to the database
    // Expand the properties based on time series internal properties
    CommandProcessor processor = this.getCommandProcessor();
    CommandStatus status = this.getCommandStatus();
    // Metadata table...
    String timeSeriesMetadataTable = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesMetadataTable" ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableLocationTypeColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesMetadataTableLocationTypeColumn" ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableLocationIdColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesMetadataTableLocationIdColumn" ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableDataProviderColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesMetadataTableDataProviderColumn" ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableDataTypeColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesMetadataTableDataTypeColumn" ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableDataIntervalColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesMetadataTableDataIntervalColumn" ),
        status, CommandPhaseType.RUN );
    String timeSeriesMetadataTableMetadataIDColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesMetadataTableMetadataIDColumn" ),
        status, CommandPhaseType.RUN );
    String timeSeriesDataTable = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesDataTable" ),
        status, CommandPhaseType.RUN );
    // Data table...
    String timeSeriesDataTableMetadataIDColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesDataTableMetadataIDColumn" ),
        status, CommandPhaseType.RUN );
    String timeSeriesDataTableDateTimeColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesDataTableDateTimeColumn" ),
        status, CommandPhaseType.RUN );
    String timeSeriesDataTableValueColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesDataTableValueColumn" ),
        status, CommandPhaseType.RUN );
    String timeSeriesDataTableFlagColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
        processor, ts, dataStore.getProperty ( "TimeSeriesDataTableFlagColumn" ),
        status, CommandPhaseType.RUN );
    boolean writeFlag = false;
    if ( (timeSeriesDataTableFlagColumn != null) && !timeSeriesDataTableFlagColumn.equals("") ) {
        writeFlag = true;
    }
    // Make sure the metadata table and columns exist...
    try {
        if ( !DMIUtil.databaseHasTable(dmi, timeSeriesMetadataTable) ) {
            problems.add ( "Database does not contain requested time series metadata table \"" + timeSeriesMetadataTable + "\"" );
        }
        if ( matchLocationType && !DMIUtil.databaseTableHasColumn(dmi, timeSeriesDataTable, timeSeriesMetadataTableLocationTypeColumn) ) {
            problems.add ( "Database time series metadata table \" + timeSeriesmetaDataTable +" +
                "\" does not contain requested location type column \"" + timeSeriesMetadataTableLocationTypeColumn + "\"" );
        }
        if ( !DMIUtil.databaseTableHasColumn(dmi, timeSeriesMetadataTable, timeSeriesMetadataTableLocationIdColumn) ) {
            problems.add ( "Database time series metadata table \" + timeSeriesMetadataTable +" +
                "\" does not contain requested location ID column \"" + timeSeriesMetadataTableLocationIdColumn + "\"" );
        }
        if ( !DMIUtil.databaseTableHasColumn(dmi, timeSeriesMetadataTable, timeSeriesMetadataTableDataProviderColumn) ) {
            problems.add ( "Database time series metadata table \" + timeSeriesMetadataTable +" +
                "\" does not contain requested data provider column \"" + timeSeriesMetadataTableDataProviderColumn + "\"" );
        }
        if ( !DMIUtil.databaseTableHasColumn(dmi, timeSeriesMetadataTable, timeSeriesMetadataTableDataTypeColumn) ) {
            problems.add ( "Database time series metadata table \" + timeSeriesMetadataTable +" +
                "\" does not contain requested data type column \"" + timeSeriesMetadataTableDataTypeColumn + "\"" );
        }
        if ( !DMIUtil.databaseTableHasColumn(dmi, timeSeriesMetadataTable, timeSeriesMetadataTableDataIntervalColumn) ) {
            problems.add ( "Database time series metadata table \" + timeSeriesMetadataTable +" +
                "\" does not contain requested data interval column \"" + timeSeriesMetadataTableDataIntervalColumn + "\"" );
        }
    }
    catch ( Exception e ) {
        problems.add ( "Error checking database time series metadata table and columns (" + e + ")." );
    }
    // Make sure the data table and columns exist...
    try {
        if ( !DMIUtil.databaseHasTable(dmi, timeSeriesDataTable) ) {
            problems.add ( "Database does not contain requested time series data table \"" + timeSeriesDataTable + "\"" );
        }
        if ( !DMIUtil.databaseTableHasColumn(dmi, timeSeriesDataTable, timeSeriesDataTableMetadataIDColumn) ) {
            problems.add ( "Database time series data table \" + timeSeriesDataTable +" +
                "\" does not contain requested metadata ID column \"" + timeSeriesDataTableMetadataIDColumn + "\"" );
        }
        if ( !DMIUtil.databaseTableHasColumn(dmi, timeSeriesDataTable, timeSeriesDataTableDateTimeColumn) ) {
            problems.add ( "Database time series data table \" + timeSeriesDataTable +" +
                "\" does not contain requested date/time column \"" + timeSeriesDataTableDateTimeColumn + "\"" );
        }
        if ( !DMIUtil.databaseTableHasColumn(dmi, timeSeriesDataTable, timeSeriesDataTableValueColumn) ) {
            problems.add ( "Database time series data table \" + timeSeriesDataTable +" +
                "\" does not contain requested value column \"" + timeSeriesDataTableValueColumn + "\"" );
        }
        if ( writeFlag && !DMIUtil.databaseTableHasColumn(dmi, timeSeriesDataTable, timeSeriesDataTableFlagColumn) ) {
            problems.add ( "Database time series data table \" + timeSeriesDataTable +" +
                "\" does not contain requested flag column \"" + timeSeriesDataTableFlagColumn + "\"" );
        }
    }
    catch ( Exception e ) {
        problems.add ( "Error checking database time series data table and columns (" + e + ")." );
    }
    if ( problems.size() > 0 ) {
        return;
    }
    // Get the metadata ID value from the metadata parts
    int timeSeriesMetadataTableMetadataID = -1;
    DMISelectStatement ss = new DMISelectStatement(dmi);
    ss.addTable(timeSeriesMetadataTable);
    // Add the values necessary to match a time series
    ss.addField(timeSeriesMetadataTableMetadataIDColumn);
    try {
        ss.addValue(timeSeriesMetadataTableMetadataID);
    }
    catch ( Exception e ) {
        problems.add ( "Error adding metadata ID " + timeSeriesMetadataTableMetadataID + " to select statement (" + e + ")."); 
    }
    try {
        if ( matchLocationType ) {
            // Location in time series is of form LocationType:LocationID
            int pos = ts.getLocation().indexOf(':');
            if ( pos < 0 ) {
                // There is no location type
                ss.addWhereClause(timeSeriesMetadataTableLocationIdColumn + "='" + ts.getLocation() + "'");
            }
            else {
                String [] parts = ts.getLocation().split(":");
                ss.addWhereClause(timeSeriesMetadataTableLocationTypeColumn + "='" + parts[0] + "'");
                ss.addWhereClause(timeSeriesMetadataTableLocationIdColumn + "='" + parts[1] + "'");
            }
        }
        else {
            // Just use the identifier as is...
            ss.addWhereClause(timeSeriesMetadataTableLocationIdColumn + "='" + ts.getLocation() + "'");
        }
        ss.addWhereClause(timeSeriesMetadataTableDataProviderColumn + "='" + ts.getIdentifier().getSource() + "'");
        ss.addWhereClause(timeSeriesMetadataTableDataTypeColumn + "='" + ts.getDataType() + "'");
        ss.addWhereClause(timeSeriesMetadataTableDataIntervalColumn + "='" + ts.getIdentifier().getInterval() + "'");
    }
    catch ( Exception e ) {
        problems.add ( "Error adding where clause to metadata select statement (" + e + ")."); 
    }
    // TODO SAM 2013-06-08 Evaluate using Scenario for "Irrigation" and "NonIrrigation"
    if ( problems.size() > 0 ) {
        return;
    }
    String sqlString = ss.toString();
    try {
        ResultSet rs = dmi.dmiSelect(ss);
        int rowCount = 0;
        while (rs.next()) {
            ++rowCount;
            timeSeriesMetadataTableMetadataID = rs.getInt(1);
            if (rs.wasNull()) {
                timeSeriesMetadataTableMetadataID = -1;
            }
        }
        rs.close();
        if ( rowCount != 1 ) {
            problems.add ( "Was expecting exactly 1 time series metadata record, got " + rowCount ); 
        }
    }
    catch ( Exception e ) {
        problems.add ( "Error reading time series metadata from database with statement \"" + sqlString + "\" (" + e + ")."); 
    }
    if ( problems.size() > 0 ) {
        return;
    }
    // Write the time series
    // For now create statements for every time series value.
    // TODO SAM 2013-06-08 Need to optimize by creating a prepared statement
    DMIWriteStatement ws;
    TSIterator tsi = null;
    try {
        tsi = ts.iterator(outputStart,outputEnd);
    }
    catch ( Exception e ) {
        problems.add("Unable to get iterator for time series data (" + e + ")." );
        return;
    }
    // Now write the time series data records using the metadata ID
    TSData tsdata;
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
            ws.addValue(tsdata.getDate());
        }
        catch ( Exception e ) {
            problems.add ( "Error adding date/time \"" + tsdata.getDate() + "\" to write statement (" + e + ")."); 
        }
        ws.addField(timeSeriesDataTableValueColumn);
        try {
            ws.addValue(tsdata.getDataValue());
        }
        catch ( Exception e ) {
            problems.add ( "Error adding value " + tsdata.getDataValue() + " to write statement (" + e + ")."); 
        }
        if ( writeFlag ) {
            ws.addField(timeSeriesDataTableFlagColumn);
        }
        try {
            ws.addValue(tsdata.getDataFlag());
        }
        catch ( Exception e ) {
            problems.add ( "Error adding flag \"" + tsdata.getDataFlag() + "\" to write statement (" + e + ")."); 
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