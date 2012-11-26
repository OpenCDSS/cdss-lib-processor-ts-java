package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSUtil_TimeSeriesToTable;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeWindow;

/**
This class initializes, checks, and runs the TimeSeriesToTable() command.
*/
public class TimeSeriesToTable_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Values for the Transformation parameter.
*/
protected final String _Create = "Create";
protected final String _Warn = "Warn";

/**
Values for the IncludeMissingValues parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
The table that is created, if creation is requested, for use in discovery mode.
*/
private DataTable __table = null;

/**
Constructor.
*/
public TimeSeriesToTable_Command ()
{	super();
	setCommandName ( "TimeSeriesToTable" );
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
{	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
	String TableID = parameters.getValue ( "TableID" );
    String DateTimeColumn = parameters.getValue ( "DateTimeColumn" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String IncludeMissingValues = parameters.getValue ( "IncludeMissingValues" );
    String ValueColumn = parameters.getValue ( "ValueColumn" );
    String FlagColumn = parameters.getValue ( "FlagColumn" );
    String DataRow = parameters.getValue ( "DataRow" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
    String OutputWindowStart = parameters.getValue ( "OutputWindowStart" );
    String OutputWindowEnd = parameters.getValue ( "OutputWindowEnd" );
    String IfTableNotFound = parameters.getValue ( "IfTableNotFound" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    if ( (TSList != null) && !TSListType.ALL_MATCHING_TSID.equals(TSList) &&
        !TSListType.FIRST_MATCHING_TSID.equals(TSList) &&
        !TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        if ( TSID != null ) {
            message = "TSID should only be specified when TSList=" +
            TSListType.ALL_MATCHING_TSID.toString() + " or " +
            TSListType.FIRST_MATCHING_TSID.toString() + " or " +
            TSListType.LAST_MATCHING_TSID.toString() + ".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Do not specify the TSID parameter." ) );
        }
    }
    
    /* TODO SAM 2008-01-04 Evaluate need
	if ( TSList == null ) {
		// Probably legacy command...
		// TODO SAM 2005-05-17 Need to require TSList when legacy
		// commands are safely nonexistent...  At that point the
		// following check can occur in any case.
		if ( (TSID == null) || (TSID.length() == 0) ) {
            message = "A TSID must be specified.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a TSList parameter value." ) );
		}
	}
    */
	if ( (TableID == null) || TableID.equals("") ) {
        message = "The TableID is required but has not been specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the TableID." ) );
	}
    if ( (DateTimeColumn == null) || (DateTimeColumn.length() == 0) ) {
        message = "The DateTimeColumn is required but has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify DateTimeColumn as table column name." ) );
    }
    if ( (TableTSIDColumn != null) && (TableTSIDColumn.length() != 0) &&
        (ValueColumn != null) && (ValueColumn.indexOf("%") >= 0) ) {
        message = "The TableTSIDColumn has been specified for single-column output but the ValueColumn " +
            "uses format specifiers (a literal is required).";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a literal string for a column name when TableTSIDColumn is specified." ) );
    }
    if ( (IncludeMissingValues != null) && !IncludeMissingValues.equals("") &&
        !IncludeMissingValues.equalsIgnoreCase(_False) && !IncludeMissingValues.equalsIgnoreCase(_True)) {
        message = "The IncludeMissingValues (" + IncludeMissingValues + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as " + _False + " or " + _True + "." ) );   
    }
    if ( (ValueColumn == null) || (ValueColumn.length() == 0) ) {
        message = "The ValueColumn is required but has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify ValueColumn as table column name." ) );
    }
    if ( (DataRow == null) || (DataRow.length() == 0) ) {
        message = "The DataRow is required but has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify DataRow as table row number (1+)." ) );
    }
    else if ( !StringUtil.isInteger(DataRow) ) {
        message = "The DataRow (" + DataRow + ") is not a valid integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify DataRow as table row number (1+)." ) );
    }
	if ( (OutputStart != null) && !OutputStart.equals("") && !OutputStart.equalsIgnoreCase("OutputStart")){
		try {
            DateTime.parse(OutputStart);
		}
		catch ( Exception e ) {
            message = "The output start date/time \"" + OutputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	if ( (OutputEnd != null) && !OutputEnd.equals("") && !OutputEnd.equalsIgnoreCase("OutputEnd") ) {
		try {
		    DateTime.parse( OutputEnd);
		}
		catch ( Exception e ) {
            message = "The output end date/time \"" + OutputStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
		}
	}
    if ( (OutputWindowStart != null) && !OutputWindowStart.equals("") ) {
        String outputWindowStart = "" + DateTimeWindow.WINDOW_YEAR + "-" + OutputWindowStart;
        try {
            DateTime.parse( outputWindowStart );
        }
        catch ( Exception e ) {
            message = "The Output window start \"" + OutputWindowStart + "\" (prepended with " +
            DateTimeWindow.WINDOW_YEAR + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time using MM, MM-DD, MM-DD hh, or MM-DD hh:mm." ) );
        }
    }
    
    if ( (OutputWindowEnd != null) && !OutputWindowEnd.equals("") ) {
        String outputWindowEnd = "" + DateTimeWindow.WINDOW_YEAR + "-" + OutputWindowEnd;
        try {
            DateTime.parse( outputWindowEnd );
        }
        catch ( Exception e ) {
            message = "The Output window end \"" + OutputWindowEnd + "\" (prepended with " +
            DateTimeWindow.WINDOW_YEAR + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time using MM, MM-DD, MM-DD hh, or MM-DD hh:mm." ) );
        }
    }
	if ( (IfTableNotFound != null) && !IfTableNotFound.equals("") &&
	    !IfTableNotFound.equalsIgnoreCase(_Create) && !IfTableNotFound.equalsIgnoreCase(_Warn)) {
        message = "The IfTableNotFound parameter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as blank, " + _Create + ", or " + _Warn + " (default)." ) );
	}
	// Put this in for now since don't know how to modify an existing table
	if ( (IfTableNotFound == null) || !IfTableNotFound.equalsIgnoreCase(_Create) ) {
        message = "The IfTableNotFound currently must be " + _Create;
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as " + _Create + "." ) );   
	}
    
	// Check for invalid parameters...
	List<String> valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "TableID" );
    valid_Vector.add ( "DateTimeColumn" );
    valid_Vector.add ( "TableTSIDColumn" );
    valid_Vector.add ( "TableTSIDFormat" );
    valid_Vector.add ( "IncludeMissingValues" );
    valid_Vector.add ( "ValueColumn" );
    valid_Vector.add ( "FlagColumn" );
    valid_Vector.add ( "DataRow" );
    valid_Vector.add ( "OutputStart" );
    valid_Vector.add ( "OutputEnd" );
    valid_Vector.add ( "OutputWindowStart" );
    valid_Vector.add ( "OutputWindowEnd" );
    valid_Vector.add ( "IfTableNotFound" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );
    
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),
		warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Create a blank table to contain the time series.
@param tslist List of time series to put into table.
@param tableID identifier for the table
@param dateTimeColumn name for date/time column
@param tableTSIDColumn name for TSID column (can be missing)
@param valueColumns the names of data value columns
@param flagColumns the names of data flag columns
@param dataRow the first row for data (0+, command parameter is 1+ for users).
@return A new table with columns set up to receive the time series.
*/
private DataTable createTable ( List<TS> tslist, String tableID, String dateTimeColumn,
    String tableTSIDColumn, List<String> valueColumns, List<String> flagColumns, int dataRow )
{   // Create the table
    List<TableField> tableFields = new Vector(3);
    // DateTime column (always)
    tableFields.add ( new TableField ( TableField.DATA_TYPE_DATE, dateTimeColumn, 12 ) );
    // TableTSIDColumn (optional) - used for single column output
    if ( (tableTSIDColumn != null) && !tableTSIDColumn.equals("") ) {
        tableFields.add ( new TableField ( TableField.DATA_TYPE_STRING, tableTSIDColumn, -1 ) );
    }
    int i = -1;
    for ( String valueColumn: valueColumns ) {
        // The data column may include %-specifiers if specifying one column per time series
        ++i;
        int width = 12; // TODO SAM 2012-08-24 Figure out width from data type or data?
        int precision = 2; // TODO SAM 2012-08-24 Figure out precision from data type?
        tableFields.add ( new TableField ( TableField.DATA_TYPE_DOUBLE, valueColumn,
            width, precision ) );
        // If flags are to be written, then create a column only if the flag column corresponding to data
        // is not an empty string
        if ( flagColumns.size() > i ) {
            String flagColumn = flagColumns.get(i);
            if ( !flagColumn.equals("") ) {
                tableFields.add ( new TableField ( TableField.DATA_TYPE_STRING, flagColumn, -1, -1 ) );
            }
        }
    }
    // Now define table with one simple call...
    DataTable table = new DataTable ( tableFields );
    return table;
}

/**
Determine the time series data table column names.
@param tslist list of time series to process
@param tableTSIDColumn if specified, indicates single-column output and therefore only a single data column will be
created
@param tableColumn table column specifier (used with values and flags);
if a literal, use for single column output and append sequential number for
multi-column output; if contains format strings, use the format with the time series to
determine the column name
*/
private List<String> determineValueColumnNames ( List<TS> tslist, String tableTSIDColumn, String tableColumn )
{
    List<String> tableColumnNames = new Vector();
    if ( (tableTSIDColumn != null) && !tableTSIDColumn.equals("") ) {
        // Single column output
        tableColumnNames.add ( tableColumn );
    }
    else {
        // Multi-column output
        for ( int i = 0; i < tslist.size(); i++ ) {
            TS ts = tslist.get(i);
            // TODO SAM 2009-10-01 Evaluate how to set precision on table columns from time series.
            if ( tableColumn.indexOf("%") >= 0 ) {
                // The data column includes format specifiers and will be expanded
                tableColumnNames.add ( ts.formatLegend(tableColumn) );
            }
            else {
                // No ID specifiers so use the same column name +1 from the first
                if ( i == 0 ) {
                    tableColumnNames.add ( tableColumn );
                }
                else {
                    tableColumnNames.add ( tableColumn + i );
                }
            }
        }
    }
    return tableColumnNames;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new TimeSeriesToTable_JDialog ( parent, this )).ok();
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
    List v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector();
        v.add ( table );
    }
    return v;
}

/**
Parse command from text.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{   // First parse
    super.parseCommand(command);
    // Replace legacy "DataColumn" parameter with "ValueColumn"
    PropList parameters = getCommandParameters();
    String valueColumn = parameters.getValue("DataColumn");
    if ( valueColumn != null ) {
        parameters.set("ValueColumn",valueColumn);
        parameters.unSet("DataColumn");
    }
}

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
@param command_number number of command to run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "TimeSeriesToTable_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Warning message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

    // Get the table to insert time series into.  If the table does not exist create it if requested
    
    DataTable table = null;
    List<TS> tslist = null;
    DateTime OutputStart_DateTime = null;
    DateTime OutputEnd_DateTime = null;
    DateTime OutputWindowStart_DateTime = null;
    DateTime OutputWindowEnd_DateTime = null;
    boolean createTable = false;
    int DateTimeColumn_int = -1;
    int [] ValueColumn_int = null; // Determined below.  Columns 0+ for each time series data
    int [] FlagColumn_int = null; // Determined below.  Columns 0+ for each time series data
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    int TableTSIDColumn_int = -1; // Determined below.
    String IncludeMissingValues = parameters.getValue ( "IncludeMissingValues" );
    boolean IncludeMissingValues_boolean = true;
    if ( (IncludeMissingValues != null) && IncludeMissingValues.equalsIgnoreCase(_False) ) {
        IncludeMissingValues_boolean = false;
    }
    int DataRow_int = -1; // Determined below.  Row 0+ for first data value.
    String TableID = parameters.getValue("TableID");
    String IfTableNotFound = parameters.getValue("IfTableNotFound");
    if ( (IfTableNotFound == null) || IfTableNotFound.equals("") ) {
        IfTableNotFound = _Warn; // default
    }
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // See if an existing table can be found.  If not and creating, create a table and
        // set its identifier
        List<String> TableIDs = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
                (TSCommandProcessor)processor, this );
        if ( StringUtil.indexOfIgnoreCase(TableIDs, TableID) < 0 ) {
            table = new DataTable();
            table.setTableID(TableID);
            setDiscoveryTable ( table );
        }
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        PropList request_params = new PropList ( "" );
        request_params.set ( "TableID", TableID );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "GetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table == null ) {
            if ( IfTableNotFound.equalsIgnoreCase(_Warn)) {
                message = "Unable to find table to process using TableID=\"" + TableID + "\".";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a table exists with the requested ID or indicate " +
                        "that the table should be created if not found." ) );
            }
            else if ( IfTableNotFound.equalsIgnoreCase(_Create) ) {
                createTable = true;
            }
        }
        else {
            table = (DataTable)o_Table;
        }
    }
    
    if ( commandPhase == CommandPhaseType.RUN ) {
    	String TSList = parameters.getValue ( "TSList" );
        if ( (TSList == null) || TSList.equals("") ) {
            TSList = TSListType.ALL_TS.toString();
        }
    	String TSID = parameters.getValue ( "TSID" );
        String EnsembleID = parameters.getValue ( "EnsembleID" );
    
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
                            message, "Report the problem to software support." ) );
    	}
    	PropList bean_PropList = bean.getResultsPropList();
    	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
    	if ( o_TSList == null ) {
            message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
    		Message.printWarning ( warning_level,
    		MessageUtil.formatMessageTag(
    		command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
    	}
    	else {
            tslist = (List)o_TSList;
    		if ( tslist.size() == 0 ) {
    			message = "Unable to find time series to process using TSList=\"" + TSList +
    			"\" TSID=\"" + TSID + "\".";
    			Message.printWarning ( warning_level,
    			    MessageUtil.formatMessageTag(
    			        command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                        message,
                        "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
    		}
    	}
    	Object o_Indices = bean_PropList.getContents ( "Indices" );
    	int [] tspos = null;
    	if ( o_Indices == null ) {
    		message = "Unable to find indices for time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
    		Message.printWarning ( warning_level,
    		MessageUtil.formatMessageTag(
    		command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
    	}
    	else {
            tspos = (int [])o_Indices;
    		if ( tspos.length == 0 ) {
                message = "Unable to find indices for time series to process using TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
    			Message.printWarning ( warning_level,
    				MessageUtil.formatMessageTag(
    					command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
    		}
    	}
    	
    	int nts = 0;
    	if ( tslist != null ) {
    		nts = tslist.size();
    	}
    	if ( nts == 0 ) {
            message = "Unable to find indices for time series to process using TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
    		Message.printWarning ( warning_level,
    		MessageUtil.formatMessageTag(
    		command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message,
                    "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
    	}
    
    	// Output period...
    
    	String OutputStart = parameters.getValue("OutputStart");
    	String OutputEnd = parameters.getValue("OutputEnd");
    
    	// Figure out the dates to use for the analysis...
    
    	try {
    	if ( OutputStart != null ) {
    		request_params = new PropList ( "" );
    		request_params.set ( "DateTime", OutputStart );
    		bean = null;
    		try {
                bean = processor.processRequest( "DateTime", request_params);
    		}
    		catch ( Exception e ) {
    			message = "Error requesting OutputStart DateTime(DateTime=" +	OutputStart + ") from processor.";
    			Message.printWarning(log_level,
    			    MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    			throw new InvalidCommandParameterException ( message );
    		}
    
    		bean_PropList = bean.getResultsPropList();
    		Object prop_contents = bean_PropList.getContents ( "DateTime" );
    		if ( prop_contents == null ) {
    			message = "Null value for OutputStart DateTime(DateTime=" + OutputStart + "\") returned from processor.";
    			Message.printWarning(log_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    			throw new InvalidCommandParameterException ( message );
    		}
    		else {	OutputStart_DateTime = (DateTime)prop_contents;
    		}
    	}
    	}
    	catch ( Exception e ) {
    		message = "OutputStart \"" + OutputStart + "\" is invalid.";
    		Message.printWarning(warning_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
    		throw new InvalidCommandParameterException ( message );
    	}
    	
    	try {
    	if ( OutputEnd != null ) {
    		request_params = new PropList ( "" );
    		request_params.set ( "DateTime", OutputEnd );
    		bean = null;
    		try {
                bean = processor.processRequest( "DateTime", request_params);
    		}
    		catch ( Exception e ) {
    			message = "Error requesting OutputEnd DateTime(DateTime=" + OutputEnd + "\") from processor.";
    			Message.printWarning(log_level,
    					MessageUtil.formatMessageTag( command_tag, ++warning_count),
    					routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
    			throw new InvalidCommandParameterException ( message );
    		}
    
    		bean_PropList = bean.getResultsPropList();
    		Object prop_contents = bean_PropList.getContents ( "DateTime" );
    		if ( prop_contents == null ) {
    			message = "Null value for OutputEnd DateTime(DateTime=" + OutputEnd +	"\") returned from processor.";
    			Message.printWarning(log_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a valid date/time or OutputEnd." ) );
    			throw new InvalidCommandParameterException ( message );
    		}
    		else {	OutputEnd_DateTime = (DateTime)prop_contents;
    		}
    	}
    	}
    	catch ( Exception e ) {
    		message = "OutputEnd \"" + OutputEnd + "\" is invalid.";
    		Message.printWarning(warning_level,
    			MessageUtil.formatMessageTag( command_tag, ++warning_count),
    			routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
    		throw new InvalidCommandParameterException ( message );
    	}
    	
        String OutputWindowStart = parameters.getValue ( "OutputWindowStart" );
        String OutputWindowEnd = parameters.getValue ( "OutputWindowEnd" );
        if ( (OutputWindowStart != null) && (OutputWindowStart.length() > 0) ) {
            try {
                // The following works with ISO formats...
                OutputWindowStart_DateTime =
                    DateTime.parse ( "" + DateTimeWindow.WINDOW_YEAR + "-" + OutputWindowStart );
            }
            catch ( Exception e ) {
                message = "OutputWindowStart \"" + OutputWindowStart +
                    "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(
                command_tag,++warning_count), routine, message );
                throw new InvalidCommandParameterException ( message );
            }
        }
        if ( (OutputWindowEnd != null) && (OutputWindowEnd.length() > 0) ) {
            try {
                // The following works with ISO formats...
                OutputWindowEnd_DateTime =
                    DateTime.parse ( "" + DateTimeWindow.WINDOW_YEAR + "-" + OutputWindowEnd );
            }
            catch ( Exception e ) {
                message = "OutputWindowEnd \"" + OutputWindowEnd +
                    "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(
                command_tag,++warning_count), routine, message );
                throw new InvalidCommandParameterException ( message );
            }
        }
    	
        String DateTimeColumn = parameters.getValue("DateTimeColumn");
        String ValueColumn = parameters.getValue("ValueColumn");
        String FlagColumn = parameters.getValue("FlagColumn");
        String DataRow = parameters.getValue("DataRow");
        DataRow_int = Integer.parseInt(DataRow) - 1; // Zero offset
        if ( createTable ) {
            List<String> valueColumnNames = determineValueColumnNames(tslist, TableTSIDColumn, ValueColumn);
            List<String> flagColumnNames = new Vector(); 
            if ( (FlagColumn != null) && !FlagColumn.equals("") ) {
                flagColumnNames = determineValueColumnNames(tslist, TableTSIDColumn, FlagColumn);
            }
            // No existing table was found and a new table should be created with columns for data values and
            // flags
            table = createTable(tslist, TableID, DateTimeColumn, TableTSIDColumn,
                valueColumnNames, flagColumnNames, DataRow_int );
            // Get the column numbers from the table that was created, to ensure proper order
            try {
                DateTimeColumn_int = table.getFieldIndex(DateTimeColumn);
            }
            catch ( Exception e ) {
                // Should not happen
                DateTimeColumn_int = -1;
            }
            if ( (TableTSIDColumn != null) && !TableTSIDColumn.equals("") ) {
                try {
                    TableTSIDColumn_int = table.getFieldIndex(TableTSIDColumn);
                }
                catch ( Exception e ) {
                    // Should not happen
                    TableTSIDColumn_int = -1;
                }
            }
            // Since creating the table, the data columns are 1+ (0 is date/time), alternating data and
            // flag columns if flags are written and not blank
            // TODO SAM 2012-08-24 This will break if an existing table is allowed to be written
            ValueColumn_int = new int[valueColumnNames.size()];
            String valueColumnName;
            for ( int i = 0; i < valueColumnNames.size(); i++ ) {
                valueColumnName = valueColumnNames.get(i);
                try {
                    ValueColumn_int[i] = table.getFieldIndex(valueColumnName);
                }
                catch ( Exception e ) {
                    // Should not happen
                    ValueColumn_int[i] = -1;
                }
            }
            // Number of flag columns must be zero or the same as number of value columns
            FlagColumn_int = new int[valueColumnNames.size()];
            // Initialize...
            for ( int i = 0; i < valueColumnNames.size(); i++ ) {
                FlagColumn_int[i] = -1;
            }
            String flagColumnName;
            for ( int i = 0; i < flagColumnNames.size(); i++ ) {
                flagColumnName = flagColumnNames.get(i);
                try {
                    FlagColumn_int[i] = table.getFieldIndex(flagColumnName);
                }
                catch ( Exception e ) {
                    // Should not happen but flag column may be blank indicating not wanting to write
                    FlagColumn_int[i] = -1;
                }
            }
            table.setTableID(TableID);
            setDiscoveryTable(table);
        }
        
        if ( warning_count > 0 ) {
            // Input error...
            message = "Insufficient data to run command.";
            status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
            Message.printWarning(3, routine, message );
            throw new CommandException ( message );
        }
    }

	// Now process the time series...
    
    try {
        if ( commandPhase == CommandPhaseType.RUN ) {
    		// Convert to a table...
            DateTimeWindow outputWindow = new DateTimeWindow ( OutputWindowStart_DateTime, OutputWindowEnd_DateTime );
    		Message.printStatus ( 2, routine, "Copying " + tslist.size() + " time series to table \"" +
    		    TableID + "\"." );
    		TSUtil_TimeSeriesToTable tsu = new TSUtil_TimeSeriesToTable(table, tslist, DateTimeColumn_int,
    		    TableTSIDColumn_int, TableTSIDFormat, IncludeMissingValues_boolean,
    		    ValueColumn_int, FlagColumn_int, DataRow_int,
    		    OutputStart_DateTime, OutputEnd_DateTime, outputWindow, true );
    		tsu.timeSeriesToTable();
    		List<String> problems = tsu.getProblems();
            for ( int iprob = 0; iprob < problems.size(); iprob++ ) {
                message = problems.get(iprob);
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                // No recommendation since it is a user-defined check
                // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING, message, "" ) );
            }
            // Add the table to the processor if created
            if ( createTable ) {
                PropList request_params = new PropList ( "" );
                request_params.setUsingObject ( "Table", table );
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
	}
	catch ( Exception e ) {
		message = "Unexpected error copying time series to table \"" + TableID + "\" (" + e + ").";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		Message.printWarning(3,routine,e);
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "See the log file for details - report the problem to software support." ) );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
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
	String TableID = props.getValue( "TableID" );
    String DateTimeColumn = props.getValue( "DateTimeColumn" );
    String TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
    String IncludeMissingValues = props.getValue ( "IncludeMissingValues" );
    String ValueColumn = props.getValue( "ValueColumn" );
    String FlagColumn = props.getValue( "FlagColumn" );
    String DataRow = props.getValue( "DataRow" );
	String OutputStart = props.getValue("OutputStart");
	String OutputEnd = props.getValue("OutputEnd");
    String OutputWindowStart = props.getValue( "OutputWindowStart" );
    String OutputWindowEnd = props.getValue( "OutputWindowEnd" );
	String IfTableNotFound = props.getValue("IfTableNotFound");
	StringBuffer b = new StringBuffer ();
	if ( (TSList != null) && (TSList.length() > 0) ) {
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
	if ( (TableID != null) && (TableID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TableID=\"" + TableID + "\"");
	}
    if ( (DateTimeColumn != null) && (DateTimeColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DateTimeColumn=\"" + DateTimeColumn + "\"" );
    }
    if ( (TableTSIDColumn != null) && (TableTSIDColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableTSIDColumn=\"" + TableTSIDColumn + "\"" );
    }
    if ( (TableTSIDFormat != null) && (TableTSIDFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableTSIDFormat=\"" + TableTSIDFormat + "\"" );
    }
    if ( (IncludeMissingValues != null) && (IncludeMissingValues.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IncludeMissingValues=" + IncludeMissingValues );
    }
    if ( (ValueColumn != null) && (ValueColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ValueColumn=\"" + ValueColumn + "\"");
    }
    if ( (FlagColumn != null) && (FlagColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FlagColumn=\"" + FlagColumn + "\"");
    }
    if ( (DataRow != null) && (DataRow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataRow=" + DataRow );
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
    if ( (OutputWindowStart != null) && (OutputWindowStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputWindowStart=\"" + OutputWindowStart + "\"" );
    }
    if ( (OutputWindowEnd != null) && (OutputWindowEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputWindowEnd=\"" + OutputWindowEnd + "\"" );
    }
	if ( (IfTableNotFound != null) && (IfTableNotFound.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfTableNotFound=\"" + IfTableNotFound + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}