// LookupTimeSeriesFromTable_Command - This class initializes, checks, and runs the LookupTimeSeriesFromTable() command.

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
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.TS.TSUtil_LookupTimeSeriesFromTable;

import RTi.Util.Math.DataTransformationType;
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
import RTi.Util.IO.WarningCount;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.LookupMethodType;
import RTi.Util.Table.OutOfRangeLookupMethodType;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeRange;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the LookupTimeSeriesFromTable() command.
*/
public class LookupTimeSeriesFromTable_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
Values for OutOfRangeNotification parameter.
*/
protected final String _Fail = "Fail";
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public LookupTimeSeriesFromTable_Command ()
{	super();
	setCommandName ( "LookupTimeSeriesFromTable" );
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
{	String Alias = parameters.getValue ( "Alias" );
    String NewTSID = parameters.getValue ( "NewTSID" );
	String TSID = parameters.getValue ( "TSID" );
    String TableID = parameters.getValue ( "TableID" );
    //String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableValue1Column = parameters.getValue ( "TableValue1Column" );
    String TableValue2Column = parameters.getValue ( "TableValue2Column" );
    String LookupMethod = parameters.getValue ( "LookupMethod" );
    String OutOfRangeLookupMethod = parameters.getValue ( "OutOfRangeLookupMethod" );
    String OutOfRangeNotification = parameters.getValue ( "OutOfRangeNotification" );
    String Transformation = parameters.getValue ( "Transformation" );
    String LEZeroLogValue = parameters.getValue ( "LEZeroLogValue" );
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (Alias == null) || Alias.equals("") ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series alias when defining the command."));
	}
	if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series identifier for the input time series must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series identifier when defining the command."));
	}
    if ( (NewTSID == null) || NewTSID.equals("") ) {
        message = "The new time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a new time series identifier when defining the command.  " +
                "Previously was optional but is now required."));
    }
    else {
        try {
            TSIdent tsident = TSIdent.parseIdentifier( NewTSID );
            try { TimeInterval.parseInterval(tsident.getInterval());
            }
            catch ( Exception e2 ) {
                message = "NewTSID interval \"" + tsident.getInterval() + "\" is not a valid interval.";
                warning += "\n" + message;
                status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a valid interval when defining the command."));
            }
        }
        catch ( Exception e ) {
            // TODO SAM 2007-03-12 Need to catch a specific exception like
            // InvalidIntervalException so that more intelligent messages can be
            // generated.
            message = "NewTSID \"" + NewTSID + "\" is not a valid identifier." +
            "Use the command editor to enter required fields.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Use the command editor to enter required fields."));
        }
    }
    
    if ( (TableID == null) || TableID.equals("") ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the identifier for the table to process." ) );
    }
    /* Not required
    if ( (TableTSIDColumn == null) || TableTSIDColumn.equals("") ) {
        message = "The TableTSID column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a table column name for the TSID." ) );
    }
    */
    
    if ( (TableValue1Column == null) || TableValue1Column.equals("") ) {
        message = "The column for the input values must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Specify a column for the input values."));
    }
    
    if ( (TableValue2Column == null) || TableValue2Column.equals("") ) {
        message = "The column for the output values must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Specify a column for the output values."));
    }
    if ( (LookupMethod != null) && LookupMethod.equals("") &&
        !LookupMethod.equalsIgnoreCase(""+LookupMethodType.INTERPOLATE) &&
        !LookupMethod.equalsIgnoreCase(""+LookupMethodType.NEXT_VALUE) &&
        !LookupMethod.equalsIgnoreCase(""+LookupMethodType.PREVIOUS_VALUE)) {
        message = "The value for LookupMethod (" + LookupMethod + ") is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Specify LookupMethod as " + LookupMethodType.INTERPOLATE + " (default), " +
            LookupMethodType.NEXT_VALUE + ", or " + LookupMethodType.PREVIOUS_VALUE + "."));
    }
    if ( (OutOfRangeLookupMethod != null) && OutOfRangeLookupMethod.equals("") &&
        !OutOfRangeLookupMethod.equalsIgnoreCase(""+OutOfRangeLookupMethodType.EXTRAPOLATE) &&
        !OutOfRangeLookupMethod.equalsIgnoreCase(""+OutOfRangeLookupMethodType.SET_MISSING) &&
        !OutOfRangeLookupMethod.equalsIgnoreCase(""+OutOfRangeLookupMethodType.USE_END_VALUE)) {
        message = "The value for OutOfRangeLookupMethod (" + OutOfRangeLookupMethod + ") is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Specify OutOfRangeLookupMethod as " + OutOfRangeLookupMethodType.EXTRAPOLATE + ", " +
            OutOfRangeLookupMethodType.SET_MISSING + "(default), or " +
            OutOfRangeLookupMethodType.USE_END_VALUE + "."));
    }
    if ( (OutOfRangeNotification != null) && OutOfRangeNotification.equals("") &&
        !OutOfRangeNotification.equalsIgnoreCase(_Ignore) &&
        !OutOfRangeNotification.equalsIgnoreCase(_Warn) &&
        !OutOfRangeNotification.equalsIgnoreCase(_Fail)) {
        message = "The value for OutOfRangeNotification (" + OutOfRangeNotification + ") is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Specify OutOfRangeNotification as " + _Ignore + " (default), " +
            _Warn + ", or " + _Fail + "."));
    }
    if ( Transformation != null ) {
        if ( !Transformation.equalsIgnoreCase(""+DataTransformationType.LOG) &&
            !Transformation.equalsIgnoreCase(""+DataTransformationType.NONE) ) {
            message = "The transformation (" + Transformation +
            ") must be  " + DataTransformationType.LOG + " or " + DataTransformationType.NONE + " (default).";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the transformation as " + DataTransformationType.LOG + " or " +
                    DataTransformationType.NONE + " (default).") );
        }
    }
    // Make sure LEZeroLogValue, if given is a valid double.
    if ( (LEZeroLogValue != null) && !LEZeroLogValue.equals("") && !StringUtil.isDouble( LEZeroLogValue ) ) {
        message = "The <= zero log value (" + LEZeroLogValue + ") is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the <= log value as a number." ) );
    }
    if ( (AnalysisStart != null) && !AnalysisStart.equals("") &&
        !AnalysisStart.equalsIgnoreCase("OutputStart") && !AnalysisStart.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse(AnalysisStart);
        }
        catch ( Exception e ) {
            message = "The analysis start \"" + AnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        }
    }
    if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") &&
        !AnalysisEnd.equalsIgnoreCase("OutputStart") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse( AnalysisEnd );
        }
        catch ( Exception e ) {
            message = "The analysis end \"" + AnalysisEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        }
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>();
    validList.add ( "Alias" );
    validList.add ( "TSID" );
    validList.add ( "NewTSID" );
    validList.add ( "TableID" );
    validList.add ( "TableTSIDColumn" );
    validList.add ( "TableTSIDFormat" );
    validList.add ( "TableValue1Column" );
    validList.add ( "TableValue2Column" );
    validList.add ( "Units" );
    validList.add ( "EffectiveDateColumn" );
    validList.add ( "LookupMethod" );
    validList.add ( "OutOfRangeLookupMethod" );
    validList.add ( "OutOfRangeNotification" );
    validList.add ( "Transformation" );
    validList.add ( "LEZeroLogValue" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
	return (new LookupTimeSeriesFromTable_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Return the lookup table that contains only the records for the requested input time series.
@param table the full datatable, which may contain extra records.
@param tsidCol the column number that contains the formatted TSID
@param tsidFormatted the formatted TSID to match for records to retain
@param value1Col the column number of the lookup value, which will be used to sort into ascending order
*/
private DataTable getLookupTable( DataTable table, int tsidCol, String tsidFormatted, int value1Col )
{
    DataTable lookupTable = null;
    // TODO SAM 2012-02-04 Need to finish the logic
    if ( tsidCol < 0 ) {
        // Just use the full table
        lookupTable = table;
    }
    else {
        // Need to remove records that do not match the TSID
        lookupTable = table;
    }
    // Now sort based on the value1 column
    // TODO SAM 2012-02-11 Add later, the analysis code checks for sort and throws an exception if not sorted
    //lookupTable.sort ( value1Col ):
    return lookupTable;
}

/**
Return the list of data objects created by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
    List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discoveryTSList;
    }
    else {
        return null;
    }
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
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
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "LookupTimeSeriesFromTable_Command.runCommandInternal", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Level for non-user messages

	// Make sure there are time series available to operate on...

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
	
	String Alias = parameters.getValue ( "Alias" );
	String TSID = parameters.getValue ( "TSID" );
	String NewTSID = parameters.getValue ( "NewTSID" );
    String TableID = parameters.getValue ( "TableID" );
    //String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    //String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TableValue1Column = parameters.getValue ( "TableValue1Column" );
    String TableValue2Column = parameters.getValue ( "TableValue2Column" );
    String Units = parameters.getValue ( "Units" );
    String EffectiveDateColumn = parameters.getValue ( "EffectiveDateColumn" );
    String LookupMethod = parameters.getValue ( "LookupMethod" );
    LookupMethodType lookupMethodType = LookupMethodType.INTERPOLATE; // Default
    if ( (LookupMethod != null) && !LookupMethod.equals("") ) {
        lookupMethodType = LookupMethodType.valueOfIgnoreCase(LookupMethod);
    }
    String OutOfRangeLookupMethod = parameters.getValue ( "OutOfRangeLookupMethod" );
    OutOfRangeLookupMethodType outOfRangeLookupMethodType = OutOfRangeLookupMethodType.SET_MISSING; // Default
    if ( (OutOfRangeLookupMethod != null) && !OutOfRangeLookupMethod.equals("") ) {
        outOfRangeLookupMethodType = OutOfRangeLookupMethodType.valueOfIgnoreCase(OutOfRangeLookupMethod);
    }
    String OutOfRangeNotification = parameters.getValue ( "OutOfRangeNotification" );
    if ( (OutOfRangeNotification == null) || OutOfRangeNotification.equals("") ) {
        OutOfRangeNotification = _Ignore; // default
    }
    String Transformation = parameters.getValue("Transformation");
    DataTransformationType transformation = DataTransformationType.NONE; // Default
    if ( (Transformation != null) && !Transformation.equals("") ) {
        transformation = DataTransformationType.valueOfIgnoreCase(Transformation);
    }
    if ( transformation == null ) {
        transformation = DataTransformationType.NONE;
    }
    String LEZeroLogValue = parameters.getValue("LEZeroLogValue");
    double leZeroLogValue = .001; // Default
    if ( (LEZeroLogValue != null) && !LEZeroLogValue.equals("") ) {
        leZeroLogValue = Double.parseDouble(LEZeroLogValue);
    }
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }
    
    // Get the analysis period
    WarningCount warningCount = new WarningCount();
    DateTimeRange analysisStartAndEnd = TSCommandProcessorUtil.getOutputPeriodForCommand (
        this, commandPhase, "AnalysisStart", AnalysisStart,  "AnalysisEnd", AnalysisEnd,
        false,
        log_level, command_tag, warning_level, warningCount );
    warning_count += warningCount.getCount();
    DateTime AnalysisStart_DateTime = analysisStartAndEnd.getStart();
    DateTime AnalysisEnd_DateTime = analysisStartAndEnd.getEnd();

    // Get the table to process.

    DataTable table = null;
    int tableValue1Column = -1;
    int tableValue2Column = -1;
    int effectiveDateColumn = -1;
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
        
        // Get the column numbers from the column names...

        try {
            tableValue1Column = table.getFieldIndex(TableValue1Column);
        }
        catch ( Exception e ) {
            message = "Table \"" + TableID + "\" does not have column \"" + TableValue1Column + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a table exists with the requested column." ) );
        }
        try {
            tableValue2Column = table.getFieldIndex(TableValue2Column);
        }
        catch ( Exception e ) {
            message = "Table \"" + TableID + "\" does not have column \"" + TableValue2Column + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a table exists with the requested column." ) );
        }
        if ( (EffectiveDateColumn != null) && !EffectiveDateColumn.equals("") ) {
            try {
                effectiveDateColumn = table.getFieldIndex(EffectiveDateColumn);
            }
            catch ( Exception e ) {
                message = "Table \"" + TableID + "\" does not have column \"" + EffectiveDateColumn + "\".";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a table exists with the requested column." ) );
            }
        }
    }
    
	// Get the time series to process.  The time series list is searched
	// backwards until the first match...

	TS ts = null;
	DataTable lookupTable = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        String TSList = "" + TSListType.LAST_MATCHING_TSID;
        List<TS> tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, null );
        if ( (tslist != null) && (tslist.size() > 0) ) {
            ts = tslist.get(0);
        }
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
    	try {
    	    // Convert the table into the final lookup table, having only records that match the input time series
    	    // and sorted in ascending order for value1
    	    int tsidCol = -1;
    	    String tsidFormatted = "";
    	    lookupTable = getLookupTable(table, tsidCol, tsidFormatted, tableValue1Column);
    	    
    	    // Get the time series to process...
    	    PropList requestParams = new PropList ( "" );
    	    requestParams.set ( "CommandTag", command_tag );
    	    requestParams.set ( "TSID", TSID );
    	    CommandProcessorRequestResultsBean bean = null;
    		try {
    		    bean = processor.processRequest( "GetTimeSeriesForTSID", requestParams);
    		}
    		catch ( Exception e ) {
    			message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\") from processor.";
    			Message.printWarning(log_level,
    			MessageUtil.formatMessageTag( command_tag, ++warning_count),
    			routine, message );
    			Message.printWarning(log_level, routine, e );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    		}
    		PropList bean_PropList = bean.getResultsPropList();
    		Object o_TS = bean_PropList.getContents ( "TS");
    		if ( o_TS == null ) {
    			message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\") from processor.";
    			Message.printWarning(log_level,
    			MessageUtil.formatMessageTag( command_tag, ++warning_count),
    			routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
    		}
    		else {
    			ts = (TS)o_TS;
    		}
    	}
    	catch ( Exception e ) {
    		ts = null;
    	}
    }
	if ( ts == null ) {
		message = "Unable to find input time series using TSID \"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
		throw new CommandWarningException ( message );
	}

	// Now process the time series...

	TS tsnew = null;
	try {
	    // Create the new time series
        tsnew = TSUtil.newTimeSeries ( NewTSID, true );
        if ( tsnew == null ) {
            message = "Null time series returned when trying to create with NewTSID=\"" + NewTSID + "\"";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify the NewTSID - contact software support if necessary." ) );
            throw new Exception ( "Null time series." );
        }
        // Always set the basic information but don't allocate data space
        tsnew.setIdentifier ( NewTSID );
        if ( Units != null ) {
            tsnew.setDataUnits ( Units );
            tsnew.setDataUnitsOriginal ( Units );
        }
        if ( (Alias != null) && !Alias.equals("") ) {
            String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                processor, tsnew, Alias, status, commandPhase);
            tsnew.setAlias ( alias );
        }
        // TODO SAM 2012-02-04 Should global output period be used instead?
        if ( commandPhase == CommandPhaseType.RUN ) {
            // Set the period...
            if ( AnalysisStart_DateTime == null ) {
                AnalysisStart_DateTime = new DateTime(ts.getDate1());
            }
            if ( AnalysisEnd_DateTime == null ) {
                AnalysisEnd_DateTime = new DateTime(ts.getDate2());
            }
            tsnew.setDate1(AnalysisStart_DateTime);
            tsnew.setDate1Original(AnalysisStart_DateTime);
            tsnew.setDate2(AnalysisEnd_DateTime);
            tsnew.setDate2Original(AnalysisEnd_DateTime);
            tsnew.allocateDataSpace();
            // Lookup time series values...
            TSUtil_LookupTimeSeriesFromTable tsu = new TSUtil_LookupTimeSeriesFromTable (
                ts, tsnew, lookupTable, tableValue1Column, false,
                tableValue2Column, effectiveDateColumn, lookupMethodType,
                outOfRangeLookupMethodType, OutOfRangeNotification,
                transformation, leZeroLogValue,
                AnalysisStart_DateTime, AnalysisEnd_DateTime, null );
            tsu.lookupTimeSeriesValuesFromTable();
            List<String> problems = tsu.getProblemsWarning();
            for ( int iprob = 0; iprob < problems.size(); iprob++ ) {
                message = problems.get(iprob);
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                // No recommendation since it is a user-defined check
                // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING, message, "" ) );
            }
            problems = tsu.getProblemsFailure();
            for ( int iprob = 0; iprob < problems.size(); iprob++ ) {
                message = problems.get(iprob);
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                // No recommendation since it is a user-defined check
                // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message, "" ) );
            }
        }
	}
	catch ( Exception e ) {
		message = "Unexpected error trying to lookup time series from time series " +
		ts.getIdentifier().toStringAliasAndTSID() +  " and table \"" + TableID + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Check the log file - report the problem to software support." ) );
	}

    // Update the data to the processor so that appropriate actions are taken...

    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Just want time series headers initialized
        List<TS> discoveryTSList = new Vector<TS>();
        discoveryTSList.add ( tsnew );
        setDiscoveryTSList ( discoveryTSList );
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, tsnew );
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
Set the list of time series read in discovery phase.
@param discoveryTSList list of time series created during discovery phase
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props )
{   if ( props == null ) {
        return getCommandName() + "()";
    }
	String Alias = props.getValue( "Alias" );
	String TSID = props.getValue( "TSID" );
	String NewTSID = props.getValue( "NewTSID" );
    String TableID = props.getValue( "TableID" );
    String TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
    String TableValue1Column = props.getValue ( "TableValue1Column" );
    String TableValue2Column = props.getValue ( "TableValue2Column" );
    String Units = props.getValue( "Units" );
    String EffectiveDateColumn = props.getValue ( "EffectiveDateColumn" );
    String LookupMethod = props.getValue ( "LookupMethod" );
    String OutOfRangeLookupMethod = props.getValue ( "OutOfRangeLookupMethod" );
    String OutOfRangeNotification = props.getValue ( "OutOfRangeNotification" );
    String Transformation = props.getValue("Transformation");
    String LEZeroLogValue = props.getValue ( "LEZeroLogValue" );
    String AnalysisStart = props.getValue( "AnalysisStart" );
    String AnalysisEnd = props.getValue( "AnalysisEnd" );
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewTSID=\"" + NewTSID + "\"" );
	}
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
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
    if ( (TableTSIDFormat != null) && (TableTSIDFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableTSIDFormat=\"" + TableTSIDFormat + "\"" );
    }
    if ( (TableValue1Column != null) && (TableValue1Column.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableValue1Column=\"" + TableValue1Column + "\"" );
    }
    if ( (TableValue2Column != null) && (TableValue2Column.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableValue2Column=\"" + TableValue2Column + "\"" );
    }
    if ( (Units != null) && (Units.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Units=\"" + Units + "\"" );
    }
    if ( (EffectiveDateColumn != null) && (EffectiveDateColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EffectiveDateColumn=\"" + EffectiveDateColumn + "\"" );
    }
    if ( (LookupMethod != null) && (LookupMethod.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "LookupMethod=" + LookupMethod );
    }
    if ( (OutOfRangeLookupMethod != null) && (OutOfRangeLookupMethod.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutOfRangeLookupMethod=" + OutOfRangeLookupMethod );
    }
    if ( (OutOfRangeNotification != null) && (OutOfRangeNotification.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutOfRangeNotification=" + OutOfRangeNotification );
    }
    if ( (Transformation != null) && (Transformation.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Transformation=" + Transformation );
    }
    if ( LEZeroLogValue != null && LEZeroLogValue.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "LEZeroLogValue=" + LEZeroLogValue);
    }
    if ( (AnalysisStart != null) && (AnalysisStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AnalysisStart=\"" + AnalysisStart + "\"" );
    }
    if ( (AnalysisEnd != null) && (AnalysisEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AnalysisEnd=\"" + AnalysisEnd + "\"" );
    }
    return getCommandName() + "("+ b.toString()+")";
}

}
