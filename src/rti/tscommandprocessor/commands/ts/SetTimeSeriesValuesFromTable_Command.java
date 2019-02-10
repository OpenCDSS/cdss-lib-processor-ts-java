// SetTimeSeriesValuesFromTable_Command - This class initializes, checks, and runs the SetTimeSeriesValuesFromTable() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.TSDataFlagMetadata;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the SetTimeSeriesValuesFromTable() command.
*/
public class SetTimeSeriesValuesFromTable_Command extends AbstractCommand
implements Command
{

/**
Possible values for SortOrder parameter.
*/
protected final String _Ascending = "Ascending";
protected final String _Descending = "Descending";
protected final String _None = "None";
    
/**
Set window year, since users do not supply this information.
This allows for leap year in case the analysis window start or end is on Feb 29.
*/
//private final int __SET_WINDOW_YEAR = 2000;
    
/**
Constructor.
*/
public SetTimeSeriesValuesFromTable_Command ()
{	super();
	setCommandName ( "SetTimeSeriesValuesFromTable" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{
    String SetStart = parameters.getValue ( "SetStart" );
    String SetEnd = parameters.getValue ( "SetEnd" );
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableDateTimeColumn = parameters.getValue ( "TableDateTimeColumn" );
    String TableValueColumn = parameters.getValue ( "TableValueColumn" );
    String SortOrder = parameters.getValue ( "SortOrder" );
    //String SetWindowStart = parameters.getValue ( "SetWindowStart" );
    //String SetWindowEnd = parameters.getValue ( "SetWindowEnd" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (SetStart != null) && !SetStart.isEmpty() &&
        !SetStart.equalsIgnoreCase("OutputStart") && !SetStart.equalsIgnoreCase("OutputEnd") && !SetStart.startsWith("${")) {
        try {
            DateTime.parse(SetStart);
        }
        catch ( Exception e ) {
            message = "The analysis start \"" + SetStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time string or ${Property}." ) );
        }
    }
    if ( (SetEnd != null) && !SetEnd.isEmpty() &&
        !SetEnd.equalsIgnoreCase("OutputStart") && !SetEnd.equalsIgnoreCase("OutputEnd") && !SetEnd.startsWith("${")) {
        try {
            DateTime.parse( SetEnd );
        }
        catch ( Exception e ) {
            message = "The analysis end \"" + SetEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time or ${Property}." ) );
        }
    }
    
    if ( (TableID == null) || TableID.isEmpty() ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the identifier for the table to process." ) );
    }
    if ( (TableTSIDColumn == null) || TableTSIDColumn.isEmpty() ) {
        message = "The table column for TSIDs must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a table column name for TSIDs." ) );
    }
    if ( (TableDateTimeColumn == null) || TableDateTimeColumn.isEmpty() ) {
        message = "The table column for date/time must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Specify a table column for date/time."));
    }
    if ( (TableValueColumn == null) || TableValueColumn.isEmpty() ) {
        message = "The table column for data values must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Specify a table column for data values."));
    }
    if ( (SortOrder != null) && !SortOrder.isEmpty() &&
        !SortOrder.equalsIgnoreCase(_Ascending) &&
        !SortOrder.equalsIgnoreCase(_Descending) &&
        !SortOrder.equalsIgnoreCase(_None) ) {
        message = "The value for SortOrder (" + SortOrder + ") is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Specify SortOrder as " + _Ascending + ", " + _Descending + ", or " + _None + " (default)."));
    }
    
    /*
    if ( (SetWindowStart != null) && !SetWindowStart.equals("") ) {
        String analysisWindowStart = "" + __SET_WINDOW_YEAR + "-" + SetWindowStart;
        try {
            DateTime.parse( analysisWindowStart );
        }
        catch ( Exception e ) {
            message = "The set window start \"" + SetWindowStart + "\" (prepended with " +
            __SET_WINDOW_YEAR + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time using MM, MM-DD, MM-DD hh, or MM-DD hh:mm." ) );
        }
    }
    
    if ( (SetWindowEnd != null) && !SetWindowEnd.equals("") ) {
        String setWindowEnd = "" + __SET_WINDOW_YEAR + "-" + SetWindowEnd;
        try {
            DateTime.parse( setWindowEnd );
        }
        catch ( Exception e ) {
            message = "The set window end \"" + SetWindowEnd + "\" (prepended with " +
            __SET_WINDOW_YEAR + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time using MM, MM-DD, MM-DD hh, or MM-DD hh:mm." ) );
        }
    }*/
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(15);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "TSIDFormat" );
    validList.add ( "SetStart" );
    validList.add ( "SetEnd" );
    validList.add ( "SetFlag" );
    validList.add ( "SetFlagDesc" );
    validList.add ( "TableID" );
    validList.add ( "TableTSIDColumn" );
    validList.add ( "TableDateTimeColumn" );
    validList.add ( "TableValueColumn" );
    validList.add ( "TableSetFlagColumn" );
    validList.add ( "TableSetFlagDescColumn" );
    validList.add ( "SortOrder" );
    //validList.add ( "SetWindowStart" );
    //validList.add ( "SetWindowEnd" );
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
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
	return (new SetTimeSeriesValuesFromTable_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	//int log_level = 3;	// Level for non-user messages

	// Make sure there are time series available to operate on...

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
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
	
	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String TSIDFormat = parameters.getValue ( "TSIDFormat" );
    String SetStart = parameters.getValue ( "SetStart" );
    String SetEnd = parameters.getValue ( "SetEnd" );
    String SetFlag = parameters.getValue ( "SetFlag" );
    String SetFlagDesc = parameters.getValue ( "SetFlagDesc" );
    String TableID = parameters.getValue ( "TableID" );
	if ( (TableID != null) && (TableID.indexOf("${") >= 0) ) {
		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
	}
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableDateTimeColumn = parameters.getValue ( "TableDateTimeColumn" );
    String TableValueColumn = parameters.getValue ( "TableValueColumn" );
    String TableSetFlagColumn = parameters.getValue ( "TableSetFlagColumn" );
    String TableSetFlagDescColumn = parameters.getValue ( "TableSetFlagDescColumn" );
    String SortOrder = parameters.getValue ( "SortOrder" );
    int sortOrder = 0; // None
    if ( SortOrder != null ) {
    	if ( SortOrder.equalsIgnoreCase(_Ascending) ) {
    		sortOrder = 1;
    	}
    	else if ( SortOrder.equalsIgnoreCase(_Descending) ) {
    		sortOrder = -1;
    	}
    }
    //String SetWindowStart = parameters.getValue ( "SetWindowStart" );
    //String SetWindowEnd = parameters.getValue ( "SetWindowEnd" );
    
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
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report problem to software support." ) );
	}
	@SuppressWarnings("unchecked")
	List<TS> tslist = (List<TS>)o_TSList;
	if ( tslist.size() == 0 ) {
        message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.WARNING,
				message, "Confirm that time series are available (may be OK for partial run)." ) );
	}
	
    // Get the set period
	DateTime SetStart_DateTime = null;
	DateTime SetEnd_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			SetStart_DateTime = TSCommandProcessorUtil.getDateTime ( SetStart, "SetStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			SetEnd_DateTime = TSCommandProcessorUtil.getDateTime ( SetEnd, "SetEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
	}

    /*
    DateTime SetWindowStart_DateTime = null;
    if ( (SetWindowStart != null) && (SetWindowStart.length() > 0) ) {
        try {
            // The following works with ISO formats...
            SetWindowStart_DateTime = DateTime.parse ( "" + __SET_WINDOW_YEAR + "-" + SetWindowStart );
        }
        catch ( Exception e ) {
            message = "SetWindowStart \"" + SetWindowStart + "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            throw new InvalidCommandParameterException ( message );
        }
    }
    DateTime SetWindowEnd_DateTime = null;
    if ( (SetWindowEnd != null) && (SetWindowEnd.length() > 0) ) {
        try {
            // The following works with ISO formats...
            SetWindowEnd_DateTime =
                DateTime.parse ( "" + __SET_WINDOW_YEAR + "-" + SetWindowEnd );
        }
        catch ( Exception e ) {
            message = "SetWindowEnd \"" + SetWindowEnd + "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            throw new InvalidCommandParameterException ( message );
        }
    }
    */

    // Get the table to process.

    DataTable table = null;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be used as input
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
        bean_PropList = bean.getResultsPropList();
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
    
    if ( warning_count > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

	// Now process the time series...

    // First get the column numbers from the column names - only need to do this once from the table
	int tableTSIDColumn = -1;
    try {
    	tableTSIDColumn = table.getFieldIndex(TableTSIDColumn);
    }
    catch ( Exception e ) {
        message = "Table \"" + TableID + "\" does not have column \"" + TableTSIDColumn + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that a table exists with column named \"" + TableTSIDColumn + "\"." ) );
    }
	int tableDateTimeColumn = -1;
    try {
    	tableDateTimeColumn = table.getFieldIndex(TableDateTimeColumn);
    }
    catch ( Exception e ) {
        message = "Table \"" + TableID + "\" does not have column \"" + TableDateTimeColumn + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that a table exists with column named \"" + TableDateTimeColumn + "\"." ) );
    }
	int tableValueColumn = -1;
    try {
        tableValueColumn = table.getFieldIndex(TableValueColumn);
    }
    catch ( Exception e ) {
        message = "Table \"" + TableID + "\" does not have column \"" + TableValueColumn + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that a table exists with column named \"" + TableValueColumn + "\"." ) );
    }
    // Optional so no error
	int tableSetFlagColumn = -1;
    try {
    	tableSetFlagColumn = table.getFieldIndex(TableSetFlagColumn);
    }
    catch ( Exception e ) {
    }
	int tableSetFlagDescColumn = -1;
    try {
    	tableSetFlagDescColumn = table.getFieldIndex(TableSetFlagDescColumn);
    }
    catch ( Exception e ) {
    }
	if ( (tableTSIDColumn >= 0) && (tableDateTimeColumn >= 0) && (tableValueColumn >= 0) ) { 
	    // Loop through the time series
	    for ( TS ts : tslist ) {
	    	if ( ts == null ) {
	    		continue;
	    	}
	    	try {
		        // Set time series values...
		        //DateTimeWindow setWindow = new DateTimeWindow ( SetWindowStart_DateTime, SetWindowEnd_DateTime );
		        List<String> problems = new ArrayList<String>();
		        setTimeSeriesFromTable (
		            ts, TSIDFormat, SetStart_DateTime, SetEnd_DateTime, SetFlag, SetFlagDesc,
		            table, tableTSIDColumn, tableDateTimeColumn, tableValueColumn, tableSetFlagColumn, tableSetFlagDescColumn,
		            sortOrder, problems );
		        for ( int iprob = 0; iprob < problems.size(); iprob++ ) {
		            message = problems.get(iprob);
		            Message.printWarning ( warning_level,
		                MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
		            // No recommendation since it is a user-defined check
		            // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
		            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING, message, "" ) );
		        }
		        for ( int iprob = 0; iprob < problems.size(); iprob++ ) {
		            message = problems.get(iprob);
		            Message.printWarning ( warning_level,
		                MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
		            // No recommendation since it is a user-defined check
		            // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
		            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message, "" ) );
		        }
		    }
	    	catch ( Exception e ) {
	    		message = "Unexpected error trying to set time series " +
	    		ts.getIdentifier().toStringAliasAndTSID() + " from table \"" + TableID + "\" (" + e + ").";
	    		Message.printWarning ( warning_level,
	    			MessageUtil.formatMessageTag(
	    			command_tag,++warning_count),routine,message );
	    		Message.printWarning(3,routine,e);
	            status.addToLog ( commandPhase,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Check the log file - report the problem to software support." ) );
	    	}
	    }
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
Set the time series values from the table, for a single time series.
*/
private void setTimeSeriesFromTable (
    TS ts, String TSIDFormat, DateTime SetStart_DateTime, DateTime SetEnd_DateTime, String setFlag, String setFlagDesc,
    DataTable table, int tableTSIDColumn, int tableDateTimeColumn, int tableValueColumn,
    int tableSetFlagColumn, int tableSetFlagDescColumn, int SortOrder, List<String> problems )
{	String routine = getClass().getSimpleName() + ".setTimeSeriesFromTable";
	boolean doSetFlag = false;
	if ( (setFlag != null) && !setFlag.isEmpty() ) {
		doSetFlag = true;
		if ( (setFlagDesc == null) || setFlagDesc.isEmpty() ) {
			setFlagDesc = "Set values from table " + table.getTableID();
		}
	}
    DateTime setStart = null;
    if ( SetStart_DateTime != null ) {
    	setStart = new DateTime(SetStart_DateTime);
    }
    DateTime setEnd = null;
    if ( SetEnd_DateTime != null ) {
    	setEnd = new DateTime(SetEnd_DateTime);
    }
    if ( SetStart_DateTime == null ) {
    	if ( ts.getDate1() == null ) {
    		return;
    	}
        setStart = new DateTime(ts.getDate1());
    }
    if ( SetEnd_DateTime == null ) {
    	if ( ts.getDate2() == null ) {
    		return;
    	}
        setEnd = new DateTime(ts.getDate2());
    }
    // First get the table rows that match the TSID
    int [] columnNumbers = new int[1];
    columnNumbers[0] = tableTSIDColumn;
    List<Object> columnValues = new ArrayList<Object>(1);
    String tsid = ts.formatLegend(TSIDFormat);
    columnValues.add(tsid);
    List<TableRecord> matchedRows = null;
    // Get the rows out of the data data that match the TSID
    try {
    	matchedRows = table.getRecords(columnNumbers, columnValues);
    }
    catch ( Exception e ) {
    	// Should only throw an exception if there was an error - won't get here if no matches
    	problems.add ("Error matching table rows for TSID \"" + tsid + "\" (" + e + ").");
    	return;
    }
    Message.printStatus(2,routine,"Matched " + matchedRows.size() + " rows from data table using TSID \"" + tsid + "\"" );
    // TODO SAM 2015-05-26 Is there a need to sort the rows by date/time?
    int setCount = 0;
    if ( matchedRows.size() > 0 ) {
	    Object o;
	    DateTime tableDateTime = null;
	    double tableValue = ts.getMissing();
	    String tableSetFlagDesc;
	    String setFlag2 = null; // Flag that is actually set, may come from table or parameter
    	// Loop through the table rows set the value.  If a regular time series, the value will set if in the period.
    	// If irregular the time series will match and reset, or be added.
    	for ( TableRecord rec : matchedRows ) {
    		try {
    			o = rec.getFieldValue(tableDateTimeColumn);
    		}
    		catch ( Exception e ) {
    			problems.add ( "Error getting date/time column value - ignoring row (" + e + ")" );
    			continue;
    		}
    		if ( o instanceof DateTime ) {
    			// Use the date/time as is
    			tableDateTime = (DateTime)o;
    		}
    		else if ( o instanceof String ) {
    			// Parse the date/time string
    			try {
    				tableDateTime = DateTime.parse((String)o);
    			}
    			catch ( Exception e ) {
    				problems.add ( "Error parsing string date/time \"" + o + "\" - ignoring row (" + e + ")" );
    			}
    		}
    		// Ignore the value if not in the set period
    		if ( tableDateTime.lessThan(setStart) || tableDateTime.greaterThan(setEnd) ) {
    			continue;
    		}
    		// TODO SAM 2015-06-25 Add SetWindow at some point
			try {
				o = rec.getFieldValue(tableValueColumn);
			}
			catch ( Exception e ) {
				problems.add("Error getting value from table matching TSID \"" + tsid + "\" and date/time " +
					tableDateTime + " - ignoring (" + e + ")" );
				continue;
			}
			// Casts should work OK below
			if ( o == null ) {
				tableValue = ts.getMissing();
			}
			else if ( o instanceof Double ) {
				tableValue = (Double)o;
			}
			else if ( o instanceof Float ) {
				tableValue = (Float)o;
			}
			else if ( o instanceof Integer ) {
				tableValue = (Integer)o;
			}
			else if ( o instanceof Long ) {
				tableValue = (Long)o;
			}
			else if ( o instanceof String ) {
				String s = (String)o;
				if ( s.isEmpty() || s.equalsIgnoreCase("NaN")) {
					tableValue = ts.getMissing();
				}
				else if ( StringUtil.isDouble(s) ) {
					tableValue = Double.parseDouble(s);
				}
				else {
					problems.add("Don't know how to convert table value column type to double precision number.");
				}
			}
			else {
				problems.add("Don't know how to convert table value for TSID=\"" + tsid + "\" date=" +
					tableDateTime + " value=\"" + o + "\" to number for data value.");
				continue;
			}
			// Get the set flag and description
			setFlag2 = setFlag;
			if ( tableSetFlagColumn >= 0 ) {
				// Set the flag based on table data (not flag supplied as command parameter)
				try {
					o = rec.getFieldValue(tableSetFlagColumn);
				}
				catch ( Exception e ) {
					problems.add("Error getting set flag from table (" + e + ")" );
					continue;
				}
				if ( o != null ) {
					setFlag2 = (String)o;
				}
			}
			tableSetFlagDesc = null;
			if ( tableSetFlagDescColumn >= 0 ) {
				try {
					o = rec.getFieldValue(tableSetFlagDescColumn);
				}
				catch ( Exception e ) {
					problems.add("Error getting set flag description from table (" + e + ")" );
					continue;
				}
				if ( o != null ) {
					tableSetFlagDesc = (String)o;
				}
			}
			// Now actually reset the value
			if ( (setFlag2 == null) || setFlag2.isEmpty() ) {
				// Just set the data value
				ts.setDataValue(tableDateTime, tableValue);
				++setCount;
			}
			else {
				// Set the value and flag
				ts.setDataValue(tableDateTime, tableValue, setFlag2, -1);
				++setCount;
			}
			// Set the flag description metadata - if from the table check every value
			if ( (tableSetFlagDesc != null) && !tableSetFlagDesc.isEmpty() ) {
				// Need to set the flag description but if already in the list, reset
				List<TSDataFlagMetadata> metaList = ts.getDataFlagMetadataList();
				boolean found = false;
				int i = -1;
				for ( TSDataFlagMetadata meta : metaList ) {
					++i;
					if ( meta.equals(setFlag) ) {
						metaList.set(i, new TSDataFlagMetadata(setFlag2, tableSetFlagDesc));
						found = true;
						break;
					}
				}
				if ( !found ) {
					ts.addDataFlagMetadata(new TSDataFlagMetadata(setFlag2, tableSetFlagDesc));
				}
			}
		}
	}
	// Only set the flag description globally if something has been set
	if ( doSetFlag && (setCount > 0) ) {
		// Have a flag based on command parameters
		ts.addDataFlagMetadata(new TSDataFlagMetadata(setFlag, setFlagDesc));
	}
}

/**
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props )
{   if ( props == null ) {
        return getCommandName() + "()";
    }
	String TSList = props.getValue ( "TSList" );
	String TSID = props.getValue ( "TSID" );
	String EnsembleID = props.getValue ( "EnsembleID" );
	String TSIDFormat = props.getValue ( "TSIDFormat" );
	String SetStart = props.getValue ( "SetStart" );
	String SetEnd = props.getValue ( "SetEnd" );
	String SetFlag = props.getValue("SetFlag");
	String SetFlagDesc = props.getValue("SetFlagDesc");
	String TableID = props.getValue ( "TableID" );
	String TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
	String TableDateTimeColumn = props.getValue ( "TableDateTimeColumn" );
	String TableValueColumn = props.getValue ( "TableValueColumn" );
	String TableSetFlagColumn = props.getValue ( "TableSetFlagColumn" );
	String TableSetFlagDescColumn = props.getValue ( "TableSetFlagDescColumn" );
	String SortOrder = props.getValue ( "SortOrder" );
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
    if ( (TSIDFormat != null) && (TSIDFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSIDFormat=\"" + TSIDFormat + "\"" );
    }
    if ( (SetStart != null) && (SetStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SetStart=\"" + SetStart + "\"" );
    }
    if ( (SetEnd != null) && (SetEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SetEnd=\"" + SetEnd + "\"" );
    }
	if ( (SetFlag != null) && (SetFlag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SetFlag=\"" + SetFlag + "\"" );
	}
    if ( (SetFlagDesc != null) && (SetFlagDesc.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SetFlagDesc=\"" + SetFlagDesc + "\"" );
    }
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (TableTSIDColumn != null) && (TableTSIDColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableTSIDColumn=\"" + TableTSIDColumn + "\"" );
    }
    if ( (TableDateTimeColumn != null) && (TableDateTimeColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableDateTimeColumn=\"" + TableDateTimeColumn + "\"" );
    }
    if ( (TableValueColumn != null) && (TableValueColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableValueColumn=\"" + TableValueColumn + "\"" );
    }
	if ( (TableSetFlagColumn != null) && (TableSetFlagColumn.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TableSetFlagColumn=\"" + TableSetFlagColumn + "\"" );
	}
    if ( (TableSetFlagDescColumn != null) && (TableSetFlagDescColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableSetFlagDescColumn=\"" + TableSetFlagDescColumn + "\"" );
    }
    if ( (SortOrder != null) && (SortOrder.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SortOrder=" + SortOrder );
    }
    /*
    if ( (SetWindowStart != null) && (SetWindowStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SetWindowStart=\"" + SetWindowStart + "\"" );
    }
    if ( (SetWindowEnd != null) && (SetWindowEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SetWindowEnd=\"" + SetWindowEnd + "\"" );
    }*/
    return getCommandName() + "("+ b.toString()+")";
}

}
