package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.TSUtil_LookupTimeSeriesFromTable;
import RTi.Util.Math.DataTransformationType;
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
import RTi.Util.IO.WarningCount;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.LookupMethodType;
import RTi.Util.Table.OutOfRangeLookupMethodType;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeRange;
import RTi.Util.Time.DateTimeWindow;

/**
This class initializes, checks, and runs the SetTimeSeriesValuesFromLookupTable() command.
*/
public class SetTimeSeriesValuesFromLookupTable_Command extends AbstractCommand
implements Command
{
    
/**
Values for SortInput parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Values for OutOfRangeNotification parameter.
*/
protected final String _Fail = "Fail";
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";

/**
Set window year, since users do not supply this information.
This allows for leap year in case the analysis window start or end is on Feb 29.
*/
private final int __SET_WINDOW_YEAR = 2000;
    
/**
Constructor.
*/
public SetTimeSeriesValuesFromLookupTable_Command ()
{	super();
	setCommandName ( "SetTimeSeriesValuesFromLookupTable" );
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
    String InputTSID = parameters.getValue ( "InputTSID" );
    String OutputTSID = parameters.getValue ( "OutputTSID" );
    String TableID = parameters.getValue ( "TableID" );
    //String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableValue1Column = parameters.getValue ( "TableValue1Column" );
    String SortInput = parameters.getValue ( "SortInput" );
    String TableValue2Column = parameters.getValue ( "TableValue2Column" );
    String LookupMethod = parameters.getValue ( "LookupMethod" );
    String OutOfRangeLookupMethod = parameters.getValue ( "OutOfRangeLookupMethod" );
    String OutOfRangeNotification = parameters.getValue ( "OutOfRangeNotification" );
    String Transformation = parameters.getValue ( "Transformation" );
    String LEZeroLogValue = parameters.getValue ( "LEZeroLogValue" );
    String SetStart = parameters.getValue ( "SetStart" );
    String SetEnd = parameters.getValue ( "SetEnd" );
    String SetWindowStart = parameters.getValue ( "SetWindowStart" );
    String SetWindowEnd = parameters.getValue ( "SetWindowEnd" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (InputTSID == null) || InputTSID.isEmpty() ) {
        message = "The time series identifier for the input time series must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Provide a time series identifier when defining the command."));
	}
    if ( (OutputTSID == null) || OutputTSID.isEmpty() ) {
        message = "The time series identifier for the output time series must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Provide a time series identifier when defining the command."));
    }
    if ( (TableID == null) || TableID.isEmpty() ) {
        message = "The lookup table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the identifier for the lookup table." ) );
    }
    /* Not required
    if ( (TableTSIDColumn == null) || TableTSIDColumn.equals("") ) {
        message = "The TableTSID column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a table column name for the TSID." ) );
    }
    */
    
    if ( (TableValue1Column == null) || TableValue1Column.isEmpty() ) {
        message = "The column for the input values must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Specify a column for the input values."));
    }
    if ( (SortInput != null) && SortInput.isEmpty() &&
        !SortInput.equalsIgnoreCase(_False) &&
        !SortInput.equalsIgnoreCase(_True) ) {
        message = "The value for SortInput (" + SortInput + ") is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Specify SortInput as " + _False + " (default), or " + _True + "."));
    }
    
    if ( (TableValue2Column == null) || TableValue2Column.isEmpty() ) {
        message = "The column for the output values must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Specify a column for the output values."));
    }
    if ( (LookupMethod != null) && !LookupMethod.isEmpty() &&
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
    if ( (OutOfRangeLookupMethod != null) && !OutOfRangeLookupMethod.isEmpty() &&
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
    if ( (OutOfRangeNotification != null) && !OutOfRangeNotification.isEmpty() &&
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
    if ( (LEZeroLogValue != null) && !LEZeroLogValue.isEmpty() && !StringUtil.isDouble( LEZeroLogValue ) ) {
        message = "The <= zero log value (" + LEZeroLogValue + ") is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the <= log value as a number." ) );
    }
    if ( (SetStart != null) && !SetStart.isEmpty() && (SetStart.indexOf("${") < 0) &&
        !SetStart.equalsIgnoreCase("OutputStart") && !SetStart.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse(SetStart);
        }
        catch ( Exception e ) {
            message = "The analysis start \"" + SetStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        }
    }
    if ( (SetEnd != null) && !SetEnd.equals("") && (SetEnd.indexOf("${") < 0) &&
        !SetEnd.equalsIgnoreCase("OutputStart") && !SetEnd.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse( SetEnd );
        }
        catch ( Exception e ) {
            message = "The analysis end \"" + SetEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        }
    }
    if ( (SetWindowStart != null) && !SetWindowStart.isEmpty() && (SetWindowStart.indexOf("${") < 0) ) {
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
    
    if ( (SetWindowEnd != null) && !SetWindowEnd.equals("") && (SetWindowEnd.indexOf("${") < 0) ) {
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
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList(18);
    validList.add ( "InputTSID" );
    validList.add ( "OutputTSID" );
    validList.add ( "TableID" );
    validList.add ( "TableTSIDColumn" );
    validList.add ( "TableTSIDFormat" );
    validList.add ( "TableValue1Column" );
    validList.add ( "SortInput" );
    validList.add ( "TableValue2Column" );
    validList.add ( "EffectiveDateColumn" );
    validList.add ( "LookupMethod" );
    validList.add ( "OutOfRangeLookupMethod" );
    validList.add ( "OutOfRangeNotification" );
    validList.add ( "Transformation" );
    validList.add ( "LEZeroLogValue" );
    validList.add ( "SetStart" );
    validList.add ( "SetEnd" );
    validList.add ( "SetWindowStart" );
    validList.add ( "SetWindowEnd" );
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
	return (new SetTimeSeriesValuesFromLookupTable_JDialog ( parent, this, tableIDChoices )).ok();
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
	int log_level = 3;	// Level for non-user messages

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
	
	String InputTSID = parameters.getValue ( "InputTSID" );
    if ( (InputTSID != null) && !InputTSID.isEmpty() && (commandPhase == CommandPhaseType.RUN) && InputTSID.indexOf("${") >= 0 ) {
    	InputTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, InputTSID);
    }
	String OutputTSID = parameters.getValue ( "OutputTSID" );
    if ( (OutputTSID != null) && !OutputTSID.isEmpty() && (commandPhase == CommandPhaseType.RUN) && OutputTSID.indexOf("${") >= 0 ) {
    	OutputTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputTSID);
    }
    String TableID = parameters.getValue ( "TableID" );
    if ( (TableID != null) && !TableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) && TableID.indexOf("${") >= 0 ) {
    	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    //String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    //String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TableValue1Column = parameters.getValue ( "TableValue1Column" );
    String SortInput = parameters.getValue ( "SortInput" );
    boolean sortInput = false;
    if ( (SortInput != null) && SortInput.equalsIgnoreCase(_True) ) {
        sortInput = true;
    }
    String TableValue2Column = parameters.getValue ( "TableValue2Column" );
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
    String SetStart = parameters.getValue ( "SetStart" );
    String SetEnd = parameters.getValue ( "SetEnd" );
    String SetWindowStart = parameters.getValue ( "SetWindowStart" );
    if ( (SetWindowStart != null) && !SetWindowStart.isEmpty() &&
    	(commandPhase == CommandPhaseType.RUN) && SetWindowStart.indexOf("${") >= 0 ) {
    	SetWindowStart = TSCommandProcessorUtil.expandParameterValue(processor, this, SetWindowStart);
    }
    String SetWindowEnd = parameters.getValue ( "SetWindowEnd" );
    if ( (SetWindowEnd != null) && !SetWindowEnd.isEmpty() &&
    	(commandPhase == CommandPhaseType.RUN) && SetWindowEnd.indexOf("${") >= 0 ) {
    	SetWindowEnd = TSCommandProcessorUtil.expandParameterValue(processor, this, SetWindowEnd);
    }
	
    // Get the analysis period
    DateTime SetStart_DateTime = null;
    DateTime SetEnd_DateTime = null;
    
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

    // Get the table to process.

    DataTable table = null;
    int tableValue1Column = -1;
    int tableValue2Column = -1;
    int effectiveDateColumn = -1;
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
    
	// Get the time series to process.  The time series list is searched
	// backwards until the first match...

	TS ts = null;
	DataTable lookupTable = null;
	try {
	    // Convert the table into the final lookup table, having only records that match the input time series
	    // and sorted in ascending order for value1
	    int tsidCol = -1;
	    String tsidFormatted = "";
	    lookupTable = getLookupTable(table, tsidCol, tsidFormatted, tableValue1Column);
	    
	    // Get the time series to process...
	    PropList requestParams = new PropList ( "" );
	    requestParams.set ( "CommandTag", command_tag );
	    requestParams.set ( "TSID", InputTSID );
		try {
		    bean = processor.processRequest( "GetTimeSeriesForTSID", requestParams);
		}
		catch ( Exception e ) {
			message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + InputTSID + "\") from processor.";
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
			message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + InputTSID + "\") from processor.";
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
	    message = "Error getting input from processor (" + e + ").";
        Message.printWarning(log_level,
        MessageUtil.formatMessageTag( command_tag, ++warning_count),
        routine, message );
        Message.printWarning(log_level, routine, e );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
		ts = null;
	}
    if ( ts == null ) {
		message = "Unable to find input time series using TSID \"" + InputTSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
		throw new CommandWarningException ( message );
	}
	
    TS outts = null;
    try {
        // Get the time series to process...
        PropList requestParams = new PropList ( "" );
        requestParams.set ( "CommandTag", command_tag );
        requestParams.set ( "TSID", OutputTSID );
        try {
            bean = processor.processRequest( "GetTimeSeriesForTSID", requestParams);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + OutputTSID + "\") from processor.";
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
            message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + OutputTSID + "\") from processor.";
            Message.printWarning(log_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
            routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        }
        else {
            outts = (TS)o_TS;
        }
    }
    catch ( Exception e ) {
        message = "Error getting output time series from processor (" + e + ").";
        Message.printWarning(log_level,
        MessageUtil.formatMessageTag( command_tag, ++warning_count),
        routine, message );
        Message.printWarning(log_level, routine, e );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
        outts = null;
    }
    if ( outts == null ) {
        message = "Unable to find output time series using TSID \"" + OutputTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }

	// Now process the time series...

	try {
        // TODO SAM 2012-02-04 Should global output period be used instead?
        // Set the period...
        if ( SetStart_DateTime == null ) {
            SetStart_DateTime = new DateTime(ts.getDate1());
        }
        if ( SetEnd_DateTime == null ) {
            SetEnd_DateTime = new DateTime(ts.getDate2());
        }
        // Lookup time series values...
        DateTimeWindow setWindow = new DateTimeWindow ( SetWindowStart_DateTime, SetWindowEnd_DateTime );
        TSUtil_LookupTimeSeriesFromTable tsu = new TSUtil_LookupTimeSeriesFromTable (
            ts, outts, lookupTable, tableValue1Column, sortInput,
            tableValue2Column, effectiveDateColumn, lookupMethodType,
            outOfRangeLookupMethodType, OutOfRangeNotification,
            transformation, leZeroLogValue,
            SetStart_DateTime, SetEnd_DateTime, setWindow );
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
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props )
{   if ( props == null ) {
        return getCommandName() + "()";
    }
	String InputTSID = props.getValue( "InputTSID" );
	String OutputTSID = props.getValue( "OutputTSID" );
    String TableID = props.getValue( "TableID" );
    String TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
    String SortInput = props.getValue ( "SortInput" );
    String TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
    String TableValue1Column = props.getValue ( "TableValue1Column" );
    String TableValue2Column = props.getValue ( "TableValue2Column" );
    String EffectiveDateColumn = props.getValue ( "EffectiveDateColumn" );
    String LookupMethod = props.getValue ( "LookupMethod" );
    String OutOfRangeLookupMethod = props.getValue ( "OutOfRangeLookupMethod" );
    String OutOfRangeNotification = props.getValue ( "OutOfRangeNotification" );
    String Transformation = props.getValue("Transformation");
    String LEZeroLogValue = props.getValue ( "LEZeroLogValue" );
    String SetStart = props.getValue( "SetStart" );
    String SetEnd = props.getValue( "SetEnd" );
    String SetWindowStart = props.getValue( "SetWindowStart" );
    String SetWindowEnd = props.getValue( "SetWindowEnd" );
	StringBuffer b = new StringBuffer ();
	if ( (InputTSID != null) && (InputTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputTSID=\"" + InputTSID + "\"" );
	}
	if ( (OutputTSID != null) && (OutputTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputTSID=\"" + OutputTSID + "\"" );
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
    if ( (SortInput != null) && (SortInput.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SortInput=" + SortInput );
    }
    if ( (TableValue2Column != null) && (TableValue2Column.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableValue2Column=\"" + TableValue2Column + "\"" );
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
    }
    return getCommandName() + "("+ b.toString()+")";
}

}