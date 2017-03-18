package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import RTi.GRTS.TimeSeriesEvent;
import RTi.GRTS.TimeSeriesEventAnnotationCreator;
import RTi.TS.TS;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the CreateTimeSeriesEventTable() command.
*/
public class CreateTimeSeriesEventTable_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
The table that is created.
*/
private DataTable __table = null;

/**
Constructor.
*/
public CreateTimeSeriesEventTable_Command ()
{	super();
	setCommandName ( "CreateTimeSeriesEventTable" );
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
{	String TimeSeriesLocations = parameters.getValue ( "TimeSeriesLocations" );
    String TableID = parameters.getValue ( "TableID" );
    String NewTableID = parameters.getValue ( "NewTableID" );
    String InputTableEventIDColumn = parameters.getValue ( "InputTableEventIDColumn" );
    String InputTableEventTypeColumn = parameters.getValue ( "InputTableEventTypeColumn" );
    String InputTableEventStartColumn = parameters.getValue ( "InputTableEventStartColumn" );
    String InputTableEventEndColumn = parameters.getValue ( "InputTableEventEndColumn" );
    String InputTableEventLocationColumns = parameters.getValue ( "InputTableEventLocationColumns" );
    String InputTableEventLabelColumn = parameters.getValue ( "InputTableEventLabelColumn" );
    String InputTableEventDescriptionColumn = parameters.getValue ( "InputTableEventDescriptionColumn" );
    String OutputTableTSIDColumn = parameters.getValue ( "OutputTableTSIDColumn" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (TableID == null) || (TableID.length() == 0) ) {
        // TODO SAM 2013-09-08 This is required now but will be optional when other options are available
        // for creating the time series events
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }
    
    if ( (TableID != null) && (TableID.length() != 0) && (NewTableID != null) && (NewTableID.length() != 0) &&
        TableID.equalsIgnoreCase(NewTableID) ) {
        message = "The original and new table identifiers are the same.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the new table identifier different from the original table identifier." ) );
    }
    
    if ( (TableID != null) && (TableID.length() > 0) ) {
        // An event table has been specified as input so make sure that all the required table columns are specified
        if ( (TimeSeriesLocations == null) || (TimeSeriesLocations.length() == 0) ) {
            message = "The time series location parameter must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the time series location parameter." ) );
        }
        if ( (InputTableEventIDColumn == null) || (InputTableEventIDColumn.length() == 0) ) {
            message = "The input table event ID column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event ID column." ) );
        }
        if ( (InputTableEventTypeColumn == null) || (InputTableEventTypeColumn.length() == 0) ) {
            message = "The input table event type column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event type column." ) );
        }
        if ( (InputTableEventStartColumn == null) || (InputTableEventStartColumn.length() == 0) ) {
            message = "The input table event start column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event start column." ) );
        }
        if ( (InputTableEventEndColumn == null) || (InputTableEventEndColumn.length() == 0) ) {
            message = "The input table event end column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event end column." ) );
        }
        if ( (InputTableEventLocationColumns == null) || (InputTableEventLocationColumns.length() == 0) ) {
            message = "The input table event location columns must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event location columns." ) );
        }
        if ( (InputTableEventLabelColumn == null) || (InputTableEventLabelColumn.length() == 0) ) {
            message = "The input table event label column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event label column." ) );
        }
        if ( (InputTableEventDescriptionColumn == null) || (InputTableEventDescriptionColumn.length() == 0) ) {
            message = "The input table event description column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event description column." ) );
        }
    }
    
    if ( (NewTableID == null) || (NewTableID.length() == 0) ) {
        message = "The new table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the new table identifier." ) );
    }
    
    if ( (OutputTableTSIDColumn == null) || (OutputTableTSIDColumn.length() == 0) ) {
        message = "The output table TSID column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the output table TSID column." ) );
    }
 
	// Check for invalid parameters...
	List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "TimeSeriesLocations" );
    valid_Vector.add ( "TableID" );
    valid_Vector.add ( "IncludeColumns" );
    valid_Vector.add ( "InputTableEventIDColumn" );
    valid_Vector.add ( "InputTableEventTypeColumn" );
    valid_Vector.add ( "IncludeInputTableEventTypes" );
    valid_Vector.add ( "InputTableEventStartColumn" );
    valid_Vector.add ( "InputTableEventEndColumn" );
    valid_Vector.add ( "InputTableEventLocationColumns" );
    valid_Vector.add ( "InputTableEventLabelColumn" );
    valid_Vector.add ( "InputTableEventDescriptionColumn" );
    valid_Vector.add ( "NewTableID" );
    valid_Vector.add ( "OutputTableTSIDColumn" );
    valid_Vector.add ( "OutputTableTSIDFormat" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );    

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
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
	return (new CreateTimeSeriesEventTable_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable table = getDiscoveryTable();
    List<DataTable> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector<DataTable>();
        v.add ( table );
    }
    return v;
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getName() + ".runCommandInternal",message = "";
	int warning_level = 2;
	int log_level = 3;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
    String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String TimeSeriesLocations = parameters.getValue ( "TimeSeriesLocations" );
    HashMap<String,String> timeSeriesLocations = new HashMap<String,String>();
    if ( (TimeSeriesLocations != null) && (TimeSeriesLocations.length() > 0) &&
        (TimeSeriesLocations.indexOf(":") > 0) ) {
        // First break map pairs by comma
        List<String>pairs = StringUtil.breakStringList(TimeSeriesLocations, ",", 0 );
        // Now break pairs and put in hashmap.  The value may be of form ${TS:property} so need to handle manually
        for ( String pair : pairs ) {
            int colonPos = pair.indexOf(":");
            if ( colonPos < 0 ) {
                // No value
                timeSeriesLocations.put(pair.trim(), "" );
            }
            else {
                if ( colonPos == (pair.length() - 1) ) {
                    // Colon was at end
                    timeSeriesLocations.put(pair.substring(0,colonPos).trim(), "" );
                }
                else {
                    timeSeriesLocations.put(pair.substring(0,colonPos).trim(), pair.substring(colonPos + 1).trim() );
                }
            }
        }
    }
    String TableID = parameters.getValue ( "TableID" );
    String NewTableID = parameters.getValue ( "NewTableID" );
    String IncludeColumns = parameters.getValue ( "IncludeColumns" );
    String [] includeColumns = null;
    if ( (IncludeColumns != null) && !IncludeColumns.equals("") ) {
        includeColumns = IncludeColumns.split(",");
        for ( int i = 0; i < includeColumns.length; i++ ) {
            includeColumns[i] = includeColumns[i].trim();
        }
    }
    String InputTableEventIDColumn = parameters.getValue ( "InputTableEventIDColumn" );
    String InputTableEventTypeColumn = parameters.getValue ( "InputTableEventTypeColumn" );
    String IncludeInputTableEventTypes = parameters.getValue ( "IncludeInputTableEventTypes" );
    List<String> eventTypes = new Vector<String>();
    if ( (IncludeInputTableEventTypes != null) && !IncludeInputTableEventTypes.equals("") ) {
        eventTypes = StringUtil.breakStringList(IncludeInputTableEventTypes, ",", 0);
        for ( int i = 0; i < eventTypes.size(); i++ ) {
            eventTypes.set(i, eventTypes.get(i).trim() );
        }
    }
    String InputTableEventStartColumn = parameters.getValue ( "InputTableEventStartColumn" );
    String InputTableEventEndColumn = parameters.getValue ( "InputTableEventEndColumn" );
    String InputTableEventLocationColumns = parameters.getValue ( "InputTableEventLocationColumns" );
    HashMap<String,String> inputTableEventLocationColumns = new HashMap<String,String>();
    if ( (InputTableEventLocationColumns != null) && (InputTableEventLocationColumns.length() > 0) &&
        (InputTableEventLocationColumns.indexOf(":") > 0) ) {
        // First break map pairs by comma
        List<String>pairs = StringUtil.breakStringList(InputTableEventLocationColumns, ",", 0 );
        // Now break pairs and put in hashmap
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            inputTableEventLocationColumns.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String InputTableEventLabelColumn = parameters.getValue ( "InputTableEventLabelColumn" );
    String InputTableEventDescriptionColumn = parameters.getValue ( "InputTableEventDescriptionColumn" );
    String OutputTableTSIDColumn = parameters.getValue ( "OutputTableTSIDColumn" );
    String OutputTableTSIDFormat = parameters.getValue ( "OutputTableTSIDFormat" );
    
    // Get the time series to process.  Allow TSID to be a pattern or specific time series...

    List<TS> tslist = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, null );
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
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
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
        }
        if ( bean == null ) {
            Message.printStatus ( 2, routine, "Bean is null.");
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
        if ( o_TSList == null ) {
            message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
            Message.printWarning ( log_level, MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
        else {
        	@SuppressWarnings("unchecked")
			List<TS> tslist0 = (List<TS>)o_TSList;
            tslist = tslist0;
            if ( tslist.size() == 0 ) {
                message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
                Message.printWarning ( log_level, MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
            }
        }
    }
    
    int nts = tslist.size();
    if ( nts == 0 ) {
        message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    }
    
    // Get the table to process.

    DataTable table = null;
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

	try {
    	// Create the table...
	    if ( commandPhase == CommandPhaseType.RUN ) {
	        DataTable newTable = null;
	        if ( (TableID != null) && !TableID.equals("") ) {
	            // Have an existing event table as input
    	        // First create the new table similar to the CopyTable() command.
	            // Specify column filters such that no records will actually be copied, since they will only be copied for
	            // matching time series locations.
	            Hashtable<String,String> columnFilters = new Hashtable<String,String> ();
	            columnFilters.put(InputTableEventTypeColumn, "xzyz");
	            // TODO SAM 2013-09-08 For now always default to copying the necessary columns
	            String [] includeColumns2 = {
	                    InputTableEventIDColumn,
	                    InputTableEventTypeColumn,
	                    InputTableEventStartColumn,
	                    InputTableEventEndColumn,
	                    InputTableEventLabelColumn,
	                    InputTableEventDescriptionColumn
	            };
    	        newTable = table.createCopy ( table, NewTableID, /* includeColumns */ includeColumns2, null, null, columnFilters, null );
                // Make sure that the output table includes the TSID and other necessary columns.
    	        // Output table columns should have been created from above copy.
                int outputTableTSIDColumnNumber = -1;
                try {
                    outputTableTSIDColumnNumber = newTable.getFieldIndex(OutputTableTSIDColumn);
                }
                catch ( Exception e2 ) {
                    outputTableTSIDColumnNumber =
                        newTable.addField(new TableField(TableField.DATA_TYPE_STRING, OutputTableTSIDColumn, -1, -1), null);
                    Message.printStatus(2, routine, "Did not match TableTSIDColumn \"" + OutputTableTSIDColumn +
                        "\" as column table so added to table." );
                }
                int outputTableEventIDColumnNumber = -1;
                try {
                    outputTableEventIDColumnNumber = table.getFieldIndex(InputTableEventIDColumn);
                }
                catch ( Exception e2 ) {
                }
                int outputTableEventTypeColumnNumber = -1;
                try {
                    outputTableEventTypeColumnNumber = table.getFieldIndex(InputTableEventTypeColumn);
                }
                catch ( Exception e2 ) {
                }
                int outputTableEventStartColumnNumber = -1;
                try {
                    outputTableEventStartColumnNumber = table.getFieldIndex(InputTableEventStartColumn);
                }
                catch ( Exception e2 ) {
                }
                int outputTableEventEndColumnNumber = -1;
                try {
                    outputTableEventEndColumnNumber = table.getFieldIndex(InputTableEventEndColumn);
                }
                catch ( Exception e2 ) {
                }
                int outputTableEventLabelColumnNumber = -1;
                try {
                    outputTableEventLabelColumnNumber = table.getFieldIndex(InputTableEventLabelColumn);
                }
                catch ( Exception e2 ) {
                }
                int outputTableEventDescriptionColumnNumber = -1;
                try {
                    outputTableEventDescriptionColumnNumber = table.getFieldIndex(InputTableEventDescriptionColumn);
                }
                catch ( Exception e2 ) {
                }
                // Create the events from the existing table and the time series being processed
                List<TimeSeriesEvent> timeSeriesEvents = new Vector<TimeSeriesEvent>();
                for ( TS ts : tslist ) {
                    // Use the original table for matching events, and add the matches to the new table below
                    TimeSeriesEventAnnotationCreator ac = new TimeSeriesEventAnnotationCreator(table, ts);
                    DateTime analysisStart = null; // TODO SAM 2013-09-08 Evaluate whether needed
                    DateTime analysisEnd = null; // TODO SAM 2013-09-08 Evaluate whether needed
                    // Expand the time series location properties for the location types, for the specific time series
                    // It is OK if properties are not matched - time series may not have
                    HashMap<String,String> timeSeriesLocationsExpanded = new HashMap<String,String>();
                    for ( Map.Entry<String,String> pairs: timeSeriesLocations.entrySet() ) {
                        timeSeriesLocationsExpanded.put(pairs.getKey(),
                            TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                                processor, ts, pairs.getValue(), null, null));
                        Message.printStatus(2, "", "Expanded property for \"" + pairs.getValue() + "\" is \"" +
                            timeSeriesLocationsExpanded.get(pairs.getKey()) + "\"");
                    }
                    timeSeriesEvents = ac.createTimeSeriesEvents(eventTypes,
                        InputTableEventIDColumn,
                        InputTableEventTypeColumn,
                        InputTableEventStartColumn,
                        InputTableEventEndColumn,
                        InputTableEventLabelColumn,
                        InputTableEventDescriptionColumn,
                        inputTableEventLocationColumns,
                        timeSeriesLocationsExpanded,
                        analysisStart,
                        analysisEnd );
                    // Transfer the events to the output table
                    int row;
                    String tsid = "";
                    if ( (OutputTableTSIDFormat != null) && !OutputTableTSIDFormat.equals("") ) {
                        tsid = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                            processor, ts, OutputTableTSIDFormat, status, commandPhase);
                    }
                    else {
                        // Use the alias if available and then the TSID
                        tsid = ts.getAlias();
                        if ( (tsid == null) || tsid.equals("") ) {
                            tsid = ts.getIdentifierString();
                        }
                    }
                    for ( TimeSeriesEvent event: timeSeriesEvents ) {
                        row = newTable.getNumberOfRecords(); // Add a new record
                        newTable.setFieldValue(row, outputTableTSIDColumnNumber, tsid, true);
                        newTable.setFieldValue(row, outputTableEventIDColumnNumber, event.getEvent().getEventID(), true);
                        newTable.setFieldValue(row, outputTableEventTypeColumnNumber, event.getEvent().getEventType(), true);
                        newTable.setFieldValue(row, outputTableEventStartColumnNumber, event.getEvent().getEventStart(), true);
                        newTable.setFieldValue(row, outputTableEventEndColumnNumber, event.getEvent().getEventEnd(), true);
                        newTable.setFieldValue(row, outputTableEventLabelColumnNumber, event.getEvent().getLabel(), true);
                        newTable.setFieldValue(row, outputTableEventDescriptionColumnNumber, event.getEvent().getDescription(), true);
                    }
                }
	        }
            // Set the table in the processor...
            if ( newTable != null ) {
                PropList request_params = new PropList ( "" );
                request_params.setUsingObject ( "Table", newTable );
                try {
                    processor.processRequest( "SetTable", request_params);
                }
                catch ( Exception e ) {
                    message = "Error requesting SetTable(Table=...) from processor.";
                    Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
                }
            }
        }
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create an empty table and set the ID
            DataTable newTable = new DataTable();
            newTable.setTableID ( NewTableID );
            setDiscoveryTable ( newTable );
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error creating new table (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
		throw new CommandWarningException ( message );
	}
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		throw new CommandWarningException ( message );
	}

    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __table = table;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TSList = props.getValue( "TSList" );
    String TSID = props.getValue( "TSID" );
    String EnsembleID = props.getValue( "EnsembleID" );
    String TimeSeriesLocations = props.getValue( "TimeSeriesLocations" );
    String TableID = props.getValue( "TableID" );
	String IncludeColumns = props.getValue( "IncludeColumns" );
	String InputTableEventIDColumn = props.getValue( "InputTableEventIDColumn" );
	String InputTableEventTypeColumn = props.getValue( "InputTableEventTypeColumn" );
	String IncludeInputTableEventTypes = props.getValue( "IncludeInputTableEventTypes" );
	String InputTableEventStartColumn = props.getValue( "InputTableEventStartColumn" );
	String InputTableEventEndColumn = props.getValue( "InputTableEventEndColumn" );
	String InputTableEventLocationColumns = props.getValue( "InputTableEventLocationColumns" );
	String InputTableEventLabelColumn = props.getValue( "InputTableEventLabelColumn" );
	String InputTableEventDescriptionColumn = props.getValue( "InputTableEventDescriptionColumn" );
    String NewTableID = props.getValue( "NewTableID" );
    String OutputTableTSIDColumn = props.getValue ( "OutputTableTSIDColumn" );
    String OutputTableTSIDFormat = props.getValue ( "OutputTableTSIDFormat" );
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
    if ( (TimeSeriesLocations != null) && (TimeSeriesLocations.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TimeSeriesLocations=\"" + TimeSeriesLocations + "\"" );
    }
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
	if ( (IncludeColumns != null) && (IncludeColumns.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IncludeColumns=\"" + IncludeColumns + "\"" );
	}
    if ( (InputTableEventIDColumn != null) && (InputTableEventIDColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventIDColumn=\"" + InputTableEventIDColumn + "\"" );
    }
    if ( (InputTableEventTypeColumn != null) && (InputTableEventTypeColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventTypeColumn=\"" + InputTableEventTypeColumn + "\"" );
    }
    if ( (IncludeInputTableEventTypes != null) && (IncludeInputTableEventTypes.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IncludeInputTableEventTypes=\"" + IncludeInputTableEventTypes + "\"" );
    }
    if ( (InputTableEventStartColumn != null) && (InputTableEventStartColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventStartColumn=\"" + InputTableEventStartColumn + "\"" );
    }
    if ( (InputTableEventEndColumn != null) && (InputTableEventEndColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventEndColumn=\"" + InputTableEventEndColumn + "\"" );
    }
    if ( (InputTableEventLocationColumns != null) && (InputTableEventLocationColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventLocationColumns=\"" + InputTableEventLocationColumns + "\"" );
    }
    if ( (InputTableEventLabelColumn != null) && (InputTableEventLabelColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventLabelColumn=\"" + InputTableEventLabelColumn + "\"" );
    }
    if ( (InputTableEventDescriptionColumn != null) && (InputTableEventDescriptionColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventDescriptionColumn=\"" + InputTableEventDescriptionColumn + "\"" );
    }
    if ( (NewTableID != null) && (NewTableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewTableID=\"" + NewTableID + "\"" );
    }
    if ( (OutputTableTSIDColumn != null) && (OutputTableTSIDColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputTableTSIDColumn=\"" + OutputTableTSIDColumn + "\"" );
    }
    if ( (OutputTableTSIDFormat != null) && (OutputTableTSIDFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputTableTSIDFormat=\"" + OutputTableTSIDFormat + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}