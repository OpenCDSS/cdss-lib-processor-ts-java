// TimeSeriesToTable_Command - This class initializes, checks, and runs the TimeSeriesToTable() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
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
public TimeSeriesToTable_Command () {
	super();
	setCommandName ( "TimeSeriesToTable" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
	String TableID = parameters.getValue ( "TableID" );
    String DateTimeColumn = parameters.getValue ( "DateTimeColumn" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String IncludeMissingValues = parameters.getValue ( "IncludeMissingValues" );
    String ValueColumn = parameters.getValue ( "ValueColumn" );
    String OutputPrecision = parameters.getValue ( "OutputPrecision" );
    //String FlagColumn = parameters.getValue ( "FlagColumn" );
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

    /* TODO SAM 2008-01-04 Evaluate need.
	if ( TSList == null ) {
		// Probably legacy command.
		// TODO SAM 2005-05-17 Need to require TSList when legacy commands are safely nonexistent.
		// At that point the following check can occur in any case.
		if ( (TSID == null) || (TSID.length() == 0) ) {
            message = "A TSID must be specified.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a TSList parameter value." ) );
		}
	}
    */
	if ( (TableID == null) || TableID.isEmpty() ) {
        message = "The TableID is required but has not been specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the TableID." ) );
	}
    if ( (DateTimeColumn == null) || DateTimeColumn.isEmpty() ) {
        message = "The DateTimeColumn is required but has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify DateTimeColumn as table column name." ) );
    }
    if ( (TableTSIDColumn != null) && !TableTSIDColumn.isEmpty() &&
        (ValueColumn != null) && (ValueColumn.indexOf("%") >= 0) ) {
        message = "The TableTSIDColumn has been specified for single-column output but the ValueColumn " +
            "uses format specifiers (a literal is required).";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a literal string for a column name when TableTSIDColumn is specified." ) );
    }
    if ( (IncludeMissingValues != null) && !IncludeMissingValues.isEmpty() &&
        !IncludeMissingValues.equalsIgnoreCase(_False) && !IncludeMissingValues.equalsIgnoreCase(_True)) {
        message = "The IncludeMissingValues (" + IncludeMissingValues + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as " + _False + " or " + _True + "." ) );
    }
    if ( (ValueColumn == null) || ValueColumn.isEmpty() ) {
        message = "The ValueColumn is required but has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify ValueColumn as table column name." ) );
    }
    if ( (OutputPrecision != null) && !OutputPrecision.isEmpty() ) {
        try {
            Integer.parseInt(OutputPrecision);
        }
        catch ( NumberFormatException e ) {
            message = "The output precision (" + OutputPrecision + ") is invalid .";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify output precision as an integer (0+)." ) );
        }
    }
	if ( (OutputStart != null) && !OutputStart.isEmpty() && !OutputStart.equalsIgnoreCase("OutputStart") && !OutputStart.startsWith("${") ){
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
	if ( (OutputEnd != null) && !OutputEnd.isEmpty() && !OutputEnd.equalsIgnoreCase("OutputEnd") && !OutputEnd.startsWith("${") ) {
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
    if ( (OutputWindowStart != null) && !OutputWindowStart.isEmpty() && !OutputWindowStart.startsWith("${") ) {
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

    if ( (OutputWindowEnd != null) && !OutputWindowEnd.isEmpty() && !OutputWindowEnd.startsWith("${")) {
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
                message, "Specify as blank, " + _Create + " (default), or " + _Warn ) );
	}

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(18);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "TableID" );
    validList.add ( "DateTimeColumn" );
    validList.add ( "TableTSIDColumn" );
    validList.add ( "TableTSIDFormat" );
    validList.add ( "IncludeMissingValues" );
    validList.add ( "ValueColumn" );
    validList.add ( "OutputPrecision" );
    validList.add ( "FlagColumn" );
    validList.add ( "OutputStart" );
    validList.add ( "OutputEnd" );
    validList.add ( "OutputWindowStart" );
    validList.add ( "OutputWindowEnd" );
    validList.add ( "RowCountProperty" );
    validList.add ( "IfTableNotFound" );
    validList.add ( "DataRow" ); // Allow this but don't include in toString() - phase out since default of 1 was always used.
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

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
@param outputPrecision precision for data values transferred to table
@param flagColumns the names of data flag columns
@return A new table with columns set up to receive the time series.
*/
private DataTable createTable ( List<TS> tslist, String tableID, String dateTimeColumn,
    String tableTSIDColumn, List<String> valueColumns, int outputPrecision, List<String> flagColumns ) {
    // Create the table.
    List<TableField> tableFields = new Vector<TableField>(3);
    // TODO SAM 2013-05-12 evaluate whether to use DATA_TYPE_DATETIME.
    // DateTime column (always).
    tableFields.add ( new TableField ( TableField.DATA_TYPE_DATE, dateTimeColumn, 12 ) );
    // TableTSIDColumn (optional) - used for single column output.
    if ( (tableTSIDColumn != null) && !tableTSIDColumn.equals("") ) {
        tableFields.add ( new TableField ( TableField.DATA_TYPE_STRING, tableTSIDColumn, -1 ) );
    }
    int i = -1;
    for ( String valueColumn: valueColumns ) {
        // The data column may include %-specifiers if specifying one column per time series.
        ++i;
        int width = 12; // TODO SAM 2012-08-24 Figure out width from data type or data?
        // TODO SAM 2012-08-24 Figure out precision from data type?
        tableFields.add ( new TableField ( TableField.DATA_TYPE_DOUBLE, valueColumn, width, outputPrecision ) );
        // If flags are to be written, then create a column only if the flag column corresponding to data is not an empty string.
        if ( flagColumns.size() > i ) {
            String flagColumn = flagColumns.get(i);
            if ( !flagColumn.equals("") ) {
                tableFields.add ( new TableField ( TableField.DATA_TYPE_STRING, flagColumn, -1, -1 ) );
            }
        }
    }
    // Now define table with one simple call.
    DataTable table = new DataTable ( tableFields );
    return table;
}

/**
Determine the time series data table column names.
@param tslist list of time series to process
@param tableTSIDColumn if specified, indicates single-column output and therefore only a single data column will be created
@param tableColumn table column specifier (used with values and flags);
if a literal, use for single column output and append sequential number for multi-column output;
if contains format strings, use the format with the time series to determine the column name
*/
private List<String> determineValueColumnNames ( List<TS> tslist, String tableTSIDColumn, String tableColumn ) {
    List<String> tableColumnNames = new ArrayList<>();
    if ( (tableTSIDColumn != null) && !tableTSIDColumn.equals("") ) {
        // Single column output.
        tableColumnNames.add ( tableColumn );
    }
    else {
        // Multi-column output.
        String [] tableColumnParts = null;
        if ( tableColumn.indexOf(",") > 0 ) {
            // Table columns are specified as comma-separated values.
            tableColumnParts = tableColumn.split(",");
        }
        for ( int i = 0; i < tslist.size(); i++ ) {
            TS ts = tslist.get(i);
            // TODO SAM 2009-10-01 Evaluate how to set precision on table columns from time series.
            if ( tableColumn.indexOf(",") > 0 ) {
                // Table column names were split above so use the part.  Also allow expansion.
                tableColumnNames.add ( ts.formatLegend(tableColumnParts[i].trim()) );
            }
            else if ( tableColumn.indexOf("%") >= 0 ) {
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
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new TimeSeriesToTable_JDialog ( parent, this )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable() {
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
The following classes can be requested:  DataTable
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    DataTable table = getDiscoveryTable();
    List<T> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new ArrayList<>();
        v.add ( (T)table );
    }
    return v;
}

/**
Parse command from text.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
   // First parse.
    super.parseCommand(command);
    // Replace legacy "DataColumn" parameter with "ValueColumn".
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number number of command to run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_count = 0;
	int warning_level = 2;
	int logLevel = 3; // Level for non-user messages for log file.
	String command_tag = "" + command_number;
	//int log_level = 3; // Warning message level for non-user messages.

	// Make sure there are time series available to operate on.

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    CommandStatus status = getCommandStatus();
    Boolean clearStatus = Boolean.TRUE; // Default.
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen.
    }
    if ( clearStatus ) {
		status.clearLog(CommandPhaseType.RUN);
	}
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

    // Get the table to insert time series into.  If the table does not exist create it if requested.

    DataTable table = null;
    List<TS> tslist = null;
    DateTime OutputStart_DateTime = null;
    DateTime OutputEnd_DateTime = null;
    DateTime OutputWindowStart_DateTime = null;
    DateTime OutputWindowEnd_DateTime = null;
    boolean createTable = false;
    int DateTimeColumn_int = -1;
    int [] ValueColumn_int = null; // Determined below.  Columns 0+ for each time series data.
    int [] FlagColumn_int = null; // Determined below.  Columns 0+ for each time series data.
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	TableTSIDColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableTSIDColumn);
    }
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    int TableTSIDColumn_int = -1; // Determined below.
    String IncludeMissingValues = parameters.getValue ( "IncludeMissingValues" );
    boolean IncludeMissingValues_boolean = true;
    if ( (IncludeMissingValues != null) && IncludeMissingValues.equalsIgnoreCase(_False) ) {
        IncludeMissingValues_boolean = false;
    }
    String TableID = parameters.getValue("TableID");
    if ( commandPhase == CommandPhaseType.RUN ) {
    	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String RowCountProperty = parameters.getValue ( "RowCountProperty" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	RowCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, RowCountProperty);
    }
    String IfTableNotFound = parameters.getValue("IfTableNotFound");
    if ( (IfTableNotFound == null) || IfTableNotFound.equals("") ) {
        IfTableNotFound = _Warn; // Default.
    }
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // See if an existing table can be found.  If not and creating, create a table and set its identifier.
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
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
    	}
        String EnsembleID = parameters.getValue ( "EnsembleID" );
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
    	}

    	// Get the time series to process.

    	PropList request_params = new PropList ( "" );
    	request_params.set ( "TSList", TSList );
    	request_params.set ( "TSID", TSID );
        request_params.set ( "EnsembleID", EnsembleID );
    	CommandProcessorRequestResultsBean bean = null;
    	Object o_TSList = null;
    	PropList bean_PropList = null;
    	try {
            bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
            bean_PropList = bean.getResultsPropList();
            o_TSList = bean_PropList.getContents ( "TSToProcessList" );
    	}
    	catch ( Exception e ) {
            message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
            "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor (" + e + ").";
    		Message.printWarning(warning_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
    	}
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
    		@SuppressWarnings("unchecked")
			List<TS> tslist0 = (List<TS>)o_TSList;
            tslist = tslist0;
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
    	Object o_Indices = null;
    	if ( bean_PropList != null ) {
    	    o_Indices = bean_PropList.getContents ( "Indices" );
    	}
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

    	// Output period.

    	String OutputStart = parameters.getValue("OutputStart");
    	String OutputEnd = parameters.getValue("OutputEnd");

    	// Figure out the dates to use for the analysis.

		try {
			OutputStart_DateTime = TSCommandProcessorUtil.getDateTime ( OutputStart, "OutputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above.
			++warning_count;
		}
		try {
			OutputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( OutputEnd, "OutputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above.
			++warning_count;
		}

        String OutputWindowStart = parameters.getValue ( "OutputWindowStart" );
        if ( (OutputWindowStart != null) && !OutputWindowStart.isEmpty() &&
        	(commandPhase == CommandPhaseType.RUN) && OutputWindowStart.indexOf("${") >= 0 ) {
        	OutputWindowStart = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputWindowStart);
        }
        String OutputWindowEnd = parameters.getValue ( "OutputWindowEnd" );
        if ( (OutputWindowEnd != null) && !OutputWindowEnd.isEmpty() &&
            (commandPhase == CommandPhaseType.RUN) && OutputWindowEnd.indexOf("${") >= 0 ) {
        	OutputWindowEnd = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputWindowEnd);
        }
        if ( (OutputWindowStart != null) && !OutputWindowStart.isEmpty() && (commandPhase == CommandPhaseType.RUN) ) {
            try {
                // The following works with ISO formats.
                OutputWindowStart_DateTime = DateTime.parse ( "" + DateTimeWindow.WINDOW_YEAR + "-" + OutputWindowStart );
            }
            catch ( Exception e ) {
                message = "OutputWindowStart \"" + OutputWindowStart +
                    "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
                Message.printWarning ( warning_level,
                	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the OutputWindowStart parameter is properly formatted." ) );
            }
        }
        if ( (OutputWindowEnd != null) && !OutputWindowEnd.isEmpty() && (commandPhase == CommandPhaseType.RUN) ) {
            try {
                // The following works with ISO formats.
                OutputWindowEnd_DateTime = DateTime.parse ( "" + DateTimeWindow.WINDOW_YEAR + "-" + OutputWindowEnd );
            }
            catch ( Exception e ) {
                message = "OutputWindowEnd \"" + OutputWindowEnd +
                    "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
                Message.printWarning ( warning_level,
                	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the OutputWindowEnd parameter is properly formatted." ) );
            }
        }

        String DateTimeColumn = parameters.getValue("DateTimeColumn");
        if ( (DateTimeColumn != null) && !DateTimeColumn.isEmpty() && (commandPhase == CommandPhaseType.RUN) && DateTimeColumn.indexOf("${") >= 0 ) {
        	DateTimeColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, DateTimeColumn);
        }
        String ValueColumn = parameters.getValue("ValueColumn");
        if ( (ValueColumn != null) && !ValueColumn.isEmpty() && (commandPhase == CommandPhaseType.RUN) && ValueColumn.indexOf("${") >= 0 ) {
        	ValueColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, ValueColumn);
        }
        String OutputPrecision = parameters.getValue("OutputPrecision");
        int outputPrecision = 2;
        if ( (OutputPrecision != null) && !OutputPrecision.equals("") ) {
            outputPrecision = Integer.parseInt(OutputPrecision);
        }
        String FlagColumn = parameters.getValue("FlagColumn");
        if ( (FlagColumn != null) && !FlagColumn.isEmpty() && (commandPhase == CommandPhaseType.RUN) && FlagColumn.indexOf("${") >= 0 ) {
        	FlagColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, FlagColumn);
        }
        // Determine the column names for values and flags.
        List<String> valueColumnNames = determineValueColumnNames(tslist, TableTSIDColumn, ValueColumn);
        List<String> flagColumnNames = new ArrayList<>();
        if ( (FlagColumn != null) && !FlagColumn.equals("") ) {
            flagColumnNames = determineValueColumnNames(tslist, TableTSIDColumn, FlagColumn);
        }
        // Make sure that the column names are unique.
        List<String> columnNamesCheck = new ArrayList<>();
        StringUtil.addListToStringList(columnNamesCheck, valueColumnNames);
        StringUtil.addListToStringList(columnNamesCheck, flagColumnNames);
        for ( int i = 0; i < columnNamesCheck.size(); i++ ) {
            String columnName = columnNamesCheck.get(i);
            for ( int j = 0; j < columnNamesCheck.size(); j++ ) {
                String columnName2 = columnNamesCheck.get(j);
                if ( columnName != columnName2 ) {
                    if ( columnName.equalsIgnoreCase(columnName2) ) {
                        message = "Time series data (or flag) column " + (i + 1) + " name \"" + columnName + "\" is duplicated in column " + (j + 1) + ".";
                        Message.printWarning(warning_level,
                            MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                               message, "Specify table column names uniquely to avoid ambiguity." ) );
                    }
                }
            }
        }
        if ( createTable ) {
            // No existing table was found and a new table should be created with columns for data values and flags.
            table = createTable(tslist, TableID, DateTimeColumn, TableTSIDColumn,
                valueColumnNames, outputPrecision, flagColumnNames );
            table.setTableID(TableID);
        }
        else {
            // Existing table is being used but need to make sure that the output columns exist.
            for ( int i = 0; i < valueColumnNames.size(); i++ ) {
                String valueColumnName = valueColumnNames.get(i);
                try {
                    table.getFieldIndex(valueColumnName);
                }
                catch ( Exception e ) {
                    // OK, indicates that column is not in table.
                    table.addField(new TableField(TableField.DATA_TYPE_DOUBLE, valueColumnName, -1, outputPrecision), null);
                    // Flag is either not used or is paired with time series.
                    if ( flagColumnNames.size() == valueColumnNames.size() ) {
                        String flagColumnName = flagColumnNames.get(i);
                        try {
                            table.getFieldIndex(flagColumnName);
                        }
                        catch ( Exception e2 ) {
                            // OK, indicates that flag column is not in table.
                            table.addField(new TableField(TableField.DATA_TYPE_STRING, flagColumnName, -1, -1), null);
                        }
                    }
                }
            }
        }
        // Get the column numbers from the table, to ensure proper order.
        try {
            DateTimeColumn_int = table.getFieldIndex(DateTimeColumn);
        }
        catch ( Exception e ) {
            // Should not happen.
            DateTimeColumn_int = -1;
        }
        if ( (TableTSIDColumn != null) && !TableTSIDColumn.equals("") ) {
            try {
                TableTSIDColumn_int = table.getFieldIndex(TableTSIDColumn);
            }
            catch ( Exception e ) {
                // Should not happen.
                TableTSIDColumn_int = -1;
            }
        }
        // Since creating the table, the data columns are 1+ (0 is date/time),
        // alternating data and flag columns if flags are written and not blank.
        ValueColumn_int = new int[valueColumnNames.size()];
        String valueColumnName;
        for ( int i = 0; i < valueColumnNames.size(); i++ ) {
            valueColumnName = valueColumnNames.get(i);
            try {
                ValueColumn_int[i] = table.getFieldIndex(valueColumnName);
            }
            catch ( Exception e ) {
                // Should not happen.
                ValueColumn_int[i] = -1;
            }
        }
        // Number of flag columns must be zero or the same as number of value columns.
        FlagColumn_int = new int[flagColumnNames.size()];
        // Initialize.
        for ( int i = 0; i < flagColumnNames.size(); i++ ) {
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
        // Make sure that the data table does not have duplicate column names since this will break the TSID matching.
        String[] columnNames = table.getFieldNames();
        for ( int i = 0; i < columnNames.length; i++ ) {
            for ( int j = 0; j < columnNames.length; j++ ) {
                if ( i == j ) {
                    continue;
                }
                else if ( columnNames[i].equalsIgnoreCase(columnNames[j]) ) {
                    message = "Table column " + (i + 1) + " name \"" + columnNames[i] + "\" is duplicated column " + (j + 1) + ".";
                    Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Specify table column names uniquely to avoid ambiguity." ) );
                }
            }
        }
    } // Run phase.

    if ( warning_count > 0 ) {
        // Input error.
        message = "Insufficient data to run command.";
        status.addToLog ( commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

	// Now process the time series.

    try {
        if ( commandPhase == CommandPhaseType.RUN ) {
    		// Convert to a table.
            DateTimeWindow outputWindow = new DateTimeWindow ( OutputWindowStart_DateTime, OutputWindowEnd_DateTime );
    		Message.printStatus ( 2, routine, "Copying " + tslist.size() + " time series to table \"" +
    		    TableID + "\"." );
    		TSUtil_TimeSeriesToTable tsu = new TSUtil_TimeSeriesToTable(table, tslist, DateTimeColumn_int,
    		    TableTSIDColumn_int, TableTSIDFormat, IncludeMissingValues_boolean,
    		    ValueColumn_int, FlagColumn_int, OutputStart_DateTime, OutputEnd_DateTime, outputWindow, true );
    		tsu.timeSeriesToTable();
    		List<String> problems = tsu.getProblems();
            for ( int iprob = 0; iprob < problems.size(); iprob++ ) {
                message = problems.get(iprob);
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                // No recommendation since it is a user-defined check.
                // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING, message, "" ) );
            }
            // Add the table to the processor if created.
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

            // Set the property indicating the number of rows in the table.
            if ( (RowCountProperty != null) && !RowCountProperty.equals("") ) {
                int rowCount = 0;
                if ( table != null ) {
                    rowCount = table.getNumberOfRecords();
                }
                PropList request_params = new PropList ( "" );
                request_params.setUsingObject ( "PropertyName", RowCountProperty );
                request_params.setUsingObject ( "PropertyValue", Integer.valueOf(rowCount) );
                try {
                    processor.processRequest( "SetProperty", request_params);
                }
                catch ( Exception e ) {
                    message = "Error requesting SetProperty(Property=\"" + RowCountProperty + "\") from processor.";
                    Message.printWarning(logLevel,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
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
private void setDiscoveryTable ( DataTable table ) {
    __table = table;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"TSList",
		"TSID",
    	"EnsembleID",
		"TableID",
    	"DateTimeColumn",
    	"TableTSIDColumn",
    	"TableTSIDFormat",
    	"IncludeMissingValues",
    	"ValueColumn",
    	"OutputPrecision",
    	"FlagColumn",
		"OutputStart",
		"OutputEnd",
    	"OutputWindowStart",
    	"OutputWindowEnd",
		"RowCountProperty",
		"IfTableNotFound"
	};
	return this.toString(parameters, parameterOrder);
}

}