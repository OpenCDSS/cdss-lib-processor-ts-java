package rti.tscommandprocessor.commands.ts;

import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;
import RTi.TS.TSUtil;

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
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.YearType;

/**
<p>
This class initializes, checks, and runs the ResequenceTimeSeriesData() command.
</p>
*/
public class ResequenceTimeSeriesData_Command extends AbstractCommand implements Command
{

/**
Protected data members shared with the dialog and other related classes.
*/
protected final String _AllTS = "AllTS";
protected final String _SelectedTS = "SelectedTS";
protected final String _AllMatchingTSID = "AllMatchingTSID";

/**
Constructor.
*/
public ResequenceTimeSeriesData_Command ()
{	super();
	setCommandName ( "ResequenceTimeSeriesData" );
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
{	String TableID = parameters.getValue ( "TableID" );
    String TableColumn = parameters.getValue ( "TableColumn" );
    String TableRowStart = parameters.getValue ( "TableRowStart" );
    String TableRowEnd = parameters.getValue ( "TableRowEnd" );
    String OutputYearType = parameters.getValue ( "OutputYearType" );
    String OutputStart = parameters.getValue ( "OutputStart" );
    String NewScenario = parameters.getValue ( "NewScenario" );
    String Alias = parameters.getValue ( "Alias" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
    
	if ( (TableID == null) || (TableID.length() == 0) ) {
		message = "The table identifier must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
			message, "Specify a table identifier." ) );
	}
    if ( (TableColumn == null) || (TableColumn.length() == 0) ) {
        message = "The table column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a table column name for the year sequence." ) );
    }
    /* TODO SAM Evaluate whether column numbers will be allowed or whether name is required
    else {
        if ( !StringUtil.isInteger(TableColumn) ) {
            message = "The table row (" + TableColumn + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an integer for the table row." ) );
        }
    }
    */
    if ( (TableRowStart == null) || (TableRowStart.length() == 0) ) {
        /*
        message = "The starting table row must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a starting table row (1+) for the year sequence." ) );
                        */
    }
    else {
        if ( !StringUtil.isInteger(TableRowStart) ) {
            message = "The starting table row (" + TableRowStart + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an integer (1+) for the starting table row." ) );
        }
    }
    if ( (TableRowEnd == null) || (TableRowEnd.length() == 0) ) {
        /*
        message = "The ending table row must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an ending table row (1+) for the year sequence." ) );
                        */
    }
    else {
        if ( !StringUtil.isInteger(TableRowEnd) ) {
            message = "The table ending row (" + TableRowEnd + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an integer (1+) for the ending table row." ) );
        }
    }
    if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
        try {
            YearType.valueOfIgnoreCase(OutputYearType);
        }
        catch ( Exception e ) {
            message = "The output year type (" + OutputYearType + ") is invalid.";
            warning += "\n" + message;
            StringBuffer b = new StringBuffer();
            List<YearType> values = YearType.getYearTypeChoices();
            for ( YearType t : values ) {
                if ( b.length() > 0 ) {
                    b.append ( ", " );
                }
                b.append ( t.toString() );
            }
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Valid values are:  " + b.toString() + "."));
        }
    }
    if ( (OutputStart != null) && !OutputStart.equals("") && !StringUtil.isInteger(OutputStart)) {
        message = "The output start \"" + OutputStart + "\" is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                 message, "Specify a valid output start as a 4-digit year." ) );
    }
    /*
    if (    (OutputEnd != null) && !OutputEnd.equals("") &&
        !OutputEnd.equalsIgnoreCase("OutputStart") &&
        !OutputEnd.equalsIgnoreCase("OutputEnd") ) {
        try {   DateTime.parse( OutputEnd );
        }
        catch ( Exception e ) {
                message = "Output end date/time \"" + OutputEnd + "\" is not a valid date/time.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid output end date/time, OutputStart, or OutputEnd." ) );
        }
    }
    */
    if ( ((NewScenario == null) || (NewScenario.length() == 0)) &&
        ((Alias == null) || (Alias.length() == 0))) {
        message = "The new scenario and/or alias must be specified to differentiate output from input time series.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a new scenario and/or alias." ) );
    }

	// Check for invalid parameters...
    List valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
	valid_Vector.add ( "TSID" );
	valid_Vector.add ( "EnsembleID" );
	valid_Vector.add ( "TableID" );
    valid_Vector.add ( "TableColumn" );
    valid_Vector.add ( "TableRowStart" );
    valid_Vector.add ( "TableRowEnd" );
    valid_Vector.add ( "OutputYearType" );
    valid_Vector.add ( "OutputStart" );
    //valid_Vector.add ( "OutputEnd" );
	valid_Vector.add ( "NewScenario" );
    valid_Vector.add ( "Alias" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

// TODO SAM 2009-09-28 Enable reading the sequence from a row rather than a columns
/**
Get the year sequence as a row from the table.
*/
private int [] getTableRow ( DataTable table, int TableRow_int, int TableColumnStart_int, int TableColumnEnd_int,
        String command_tag, int warning_level, CommandStatus status )
throws Exception
{   String routine = "ResequenceTimeSeriesData.getTableRow";
    String message;
    int warning_count = 0;
    int nyears = TableColumnEnd_int - TableColumnStart_int + 1;
    int [] year_sequence = new int[nyears];
    // Remember that indices are zero offset whereas parameters are 1-offset
    TableRecord rec = table.getRecord(TableRow_int - 1);
    Object o;
    int ival = 0;
    String s;
    for ( int icol = (TableColumnStart_int - 1); icol < TableColumnEnd_int; icol++, ival++ ) {
        o = rec.getFieldValue(icol);
        if ( o instanceof Integer ) {
            // Just send back the value...
            year_sequence[ival] = ((Integer)o).intValue();
        }
        else if ( o instanceof String ) {
            s = (String)o;
            if ( StringUtil.isInteger(s) ) {
                year_sequence[ival] = Integer.parseInt(s);
            }
            else {
                message = "Column " + (icol + 1) + " value (" + s + ") is not an integer.";
                Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
            }
        }
        else {
            message = "Column " + (icol + 1) + " value (" + o + ") is not an integer.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report problem to software support." ) );
        }
    }
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " errors getting the years from the table row.";
        throw new Exception ( message );
    }
    return year_sequence;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new ResequenceTimeSeriesData_JDialog ( parent, this )).ok();
}

/**
Get the year sequence as a row from the table.
@param table DataTable from which data are being extracted.
@param TableColumn_int Integer column to extract (0+, from parameter).
@param TableRowStart_int First row to extract  (0+, from parameter).
@param TableRowEnd_int Last row to extract  (0+, from parameter).
@param command_tag Command number (1+).
@param warning_level Warning level for problems.
@param status CommandStatus to which to add messages.
*/
private int [] getTableColumn ( DataTable table, int TableColumn_int, int TableRowStart_int, int TableRowEnd_int,
        String command_tag, int warning_level, CommandStatus status )
throws Exception
{   String routine = "ResequenceTimeSeriesData.getTableColumn";
    String message;
    int warning_count = 0;
    int nyears = 0;
    if ( (TableRowStart_int >= 0) && (TableRowEnd_int >= 0) ) {
        // User has specified so use the information...
        nyears = TableRowEnd_int - TableRowStart_int + 1;
    }
    else if ( (TableRowStart_int >= 0) && (TableRowEnd_int < 0) ) {
        // User has only specified the start so compute from the table size.
        // This works out with the start being zero offset.
        nyears = table.getNumberOfRecords() - TableRowStart_int;
        TableRowEnd_int = table.getNumberOfRecords() - 1;
    }
    else {
        // User has not specified the start or ending row so use all the data in the table.
        nyears = table.getNumberOfRecords();
        TableRowStart_int = 0;
        TableRowEnd_int = table.getNumberOfRecords() - 1;
    }
    int [] year_sequence = new int[nyears];
    Object o;
    int ival = 0;
    String s;
    for ( int irow = TableRowStart_int; irow <= TableRowEnd_int; irow++, ival++ ) {
        TableRecord rec = table.getRecord(irow);
        o = rec.getFieldValue(TableColumn_int);
        if ( o instanceof Integer ) {
            // Just send back the value...
            year_sequence[ival] = ((Integer)o).intValue();
        }
        else if ( o instanceof String ) {
            s = (String)o;
            if ( StringUtil.isInteger(s) ) {
                year_sequence[ival] = Integer.parseInt(s);
            }
            else {
                message = "Row " + (irow + 1) + " value (" + s + ") is not an integer.";
                Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Correct the data to be a 4-digit year.." ) );
            }
        }
        else {
            message = "Row " + (irow + 1) + " value (" + o + ") is not an integer.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Correct the data to be a 4-digit year." ) );
        }
    }
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " errors getting the years from the table column.";
        throw new Exception ( message );
    }
    return year_sequence;
}

// parseCommand from base class

/**
Resequence the data in the old time series, transferring to the new time series, using
the year sequence as the map.
@param oldts Old time series with data to transfer to the new time series.
@param newts New time series receiving the data.
@param year_sequence Sequence of years from the old time series to transfer to the new time series.
@param outputYearType the output year type, indicating the span of months to resequence
*/
private void resequenceData ( TS oldts, TS newts, int [] year_sequence, YearType outputYearType )
{
    //
    int base = newts.getDataIntervalBase();
    int mult = newts.getDataIntervalMult();
    DateTime date2 = newts.getDate2();
    int iyear = 0;
    DateTime dateold = null;    // Date in old data, reset in loop when month = 1
    double oldval;
    int outputYearTypeFirstMonth = outputYearType.getStartMonth();
    // This will iterate in the output year type that was requested, with the month being at the
    // start of an output year.
    for ( DateTime date = new DateTime(newts.getDate1()); date.lessThanOrEqualTo(date2);
        date.addInterval(base,mult), dateold.addInterval(base,mult)) {
        if ( date.getMonth() == outputYearTypeFirstMonth ) {
            // Reset the year to get the specified data...
            dateold = new DateTime(date);
            // Account for calendar and non-calendar years...
            // For calendar, the first month is 1 and the offset is zero.
            // For water year, the first month is 10 and the offset is -1 so the correct 12 months will
            // be shifted.
            dateold.setMonth ( outputYearType.getStartMonth() );
            dateold.setYear ( year_sequence[iyear++] + outputYearType.getStartYearOffset() );
        }
        oldval = oldts.getDataValue ( dateold );
        newts.setDataValue( date, oldval );
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
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ResequenceTimeSeriesData_Command.runCommand", message;
	int warning_level = 2;
    int log_level = 3;  // Warning level for non-user log messages
	String command_tag = "" + command_number;
	int warning_count = 0;

    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);

	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
	if ( TSList == null ) {
		TSList = _AllTS;
	}
	String TSID = parameters.getValue ( "TSID" );
	String EnsembleID = parameters.getValue ( "EnsembleID" );
	String Alias = parameters.getValue("Alias");

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
		"\", TSID=\"" + TSID + "\") from processor.";
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
		message = "Unable to find time series to process using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report problem to software support." ) );
	}
	List tslist = (List)o_TSList;
	if ( tslist.size() == 0 ) {
		message = "Zero time series in list to process using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.WARNING,
						message, "Confirm that time series are available (may be OK for partial run)." ) );
	}
    
	String OutputYearType = parameters.getValue ( "OutputYearType" );
	YearType outputYearType = YearType.CALENDAR;
	if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
	    outputYearType = YearType.valueOfIgnoreCase(OutputYearType);
	}
    String OutputStart = parameters.getValue ( "OutputStart" );
    //String OutputEnd = parameters.getValue ( "OutputEnd" );
    DateTime OutputStart_DateTime = null;
    //DateTime OutputEnd_DateTime = null;
    if ( (OutputStart != null) && !OutputStart.equals("") ) {
        try {
        request_params = new PropList ( "" );
        request_params.set ( "DateTime", OutputStart );
        bean = null;
        try { bean =
            processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting OutputStart DateTime(DateTime=" + OutputStart + ") from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report to software support." ) );
            throw new InvalidCommandParameterException ( message );
        }

        bean_PropList = bean.getResultsPropList();
        Object prop_contents = bean_PropList.getContents ( "DateTime" );
        if ( prop_contents == null ) {
            message = "Null value for OutputStart DateTime(DateTime=" +
            OutputStart +   "\") returned from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report to software support." ) );
            throw new InvalidCommandParameterException ( message );
        }
        else {  OutputStart_DateTime = (DateTime)prop_contents;
        }
    }
    catch ( Exception e ) {
        message = "OutputStart \"" + OutputStart + "\" is invalid.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid output start date/time." ) );
        throw new InvalidCommandParameterException ( message );
    }
    }
    else {  // Get from the processor...
        try {   Object o = processor.getPropContents ( "OutputStart" );
                if ( o != null ) {
                    OutputStart_DateTime = (DateTime)o;
                }
        }
        catch ( Exception e ) {
            // Not fatal, but of use to developers.
            message = "Error requesting OutputStart from processor - not using.";
            Message.printDebug(10, routine, message );
        }
    }/*
    if ( (OutputEnd != null) && !OutputEnd.equals("") ) {
            try {
            request_params = new PropList ( "" );
            request_params.set ( "DateTime", OutputEnd );
            bean = null;
            try { bean =
                processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting OutputEnd DateTime(DateTime=" + OutputEnd + ") from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }

            bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for OutputEnd DateTime(DateTime=" +
                OutputEnd + "\") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {  OutputEnd_DateTime = (DateTime)prop_contents;
            }
        }
        catch ( Exception e ) {
            message = "OutputEnd \"" + OutputEnd + "\" is invalid.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid output start date/time." ) );
            throw new InvalidCommandParameterException ( message );
        }
        }
        else {  // Get from the processor...
            try {   Object o = processor.getPropContents ( "OutputEnd" );
                    if ( o != null ) {
                        OutputEnd_DateTime = (DateTime)o;
                    }
            }
            catch ( Exception e ) {
                // Not fatal, but of use to developers.
                message = "Error requesting OutputEnd from processor - not using.";
                Message.printDebug(10, routine, message );
            }
    }
    */
    
    // Get the table information
    
    String TableID = parameters.getValue ( "TableID" );
    request_params = new PropList ( "" );
    request_params.set ( "TableID", TableID );
    try {
        bean = processor.processRequest( "GetTable", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    Object o_Table = bean_PropList.getContents ( "Table" );
    if ( o_Table == null ) {
        message = "Unable to find table to process using TableID=\"" + TableID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
    }
    DataTable table = (DataTable)o_Table;
    
    int TableColumn_int = -1;
    int TableRowStart_int = -1;
    int TableRowEnd_int = -1;
    String TableColumn = parameters.getValue ( "TableColumn" );
    String TableRowStart = parameters.getValue ( "TableRowStart" );
    String TableRowEnd = parameters.getValue ( "TableRowEnd" );
    if ( TableColumn != null ) {
        // Must be a named column
        try {
            TableColumn_int = table.getFieldIndex(TableColumn);
        }
        catch ( Exception e ) {
            message = "Unable to determine column number from column name \"" + TableColumn + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the table has a column matching \"" + TableColumn + "\"." ) );
            throw new CommandException ( message );
        }
    }
    if ( TableRowStart != null ) {
        TableRowStart_int = Integer.parseInt ( TableRowStart );
    }
    if ( TableRowEnd != null ) {
        TableRowEnd_int = Integer.parseInt ( TableRowEnd );
    }
    int [] year_sequence = null;
    try {
        year_sequence = getTableColumn ( table, TableColumn_int, TableRowStart_int, TableRowEnd_int,
            command_tag, warning_level, status );
    }
    catch ( Exception e ) {
        message = "Error getting year sequence from table.";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check the table format and parameters that specify the cells to use." ) );
        throw new CommandException ( message );
    }

	// Now try to process.

    int nyears = year_sequence.length;
    String NewScenario = parameters.getValue("NewScenario");
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    TS ts = null;
    TS newts = null;
    for ( int i = 0; i < size; i++ ) {
        // Create a copy of the original, but with the new scenario.
        ts = (TS)tslist.get(i);
        if ( ts.getDataIntervalBase() != TimeInterval.MONTH ) {
            message = "Resequencing currently only can be applied to monthly time series.";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify only monthly time series as input to the command." ) );
        }
        newts = (TS)ts.clone();
        if ( (NewScenario != null) && (NewScenario.length() > 0) ) {
            newts.getIdentifier().setScenario(NewScenario);
        }
        // Allocate space for the new time series, for the requested years...
        // For calendar year type, make sure that the start date is Jan of the specified year and go to
        // Dec of the end year.  For other year types, make sure that the period spans full years.
        DateTime OutputStart_new_DateTime = new DateTime(ts.getDate1());
        if ( OutputStart_DateTime != null ) {
            OutputStart_new_DateTime.setYear(OutputStart_DateTime.getYear() + outputYearType.getStartYearOffset());
        }
        // Make sure that the start lines up with the month
        OutputStart_new_DateTime.setMonth(outputYearType.getStartMonth());
        newts.setDate1(OutputStart_new_DateTime);
        // The output end is the end of the year for the number of years...
        DateTime OutputEnd_new_DateTime = new DateTime(OutputStart_new_DateTime);
        // Make sure to account for case where output year type spans two years
        OutputEnd_new_DateTime.addYear ( nyears - 1 + (-1*outputYearType.getStartYearOffset()));
        OutputEnd_new_DateTime.setMonth(outputYearType.getEndMonth());
        newts.setDate2(OutputEnd_new_DateTime);
        newts.allocateDataSpace();
        // Set all data to missing so as to not confuse with old data...
        TSUtil.setConstant ( newts, newts.getMissing() );
        // Reset the description because don't want any extra information like constant note
        newts.setDescription ( ts.getDescription() );
        if ( (Alias != null) && (Alias.length() > 0) ) {
            // Set the alias to the desired string - this is impacted by the Scenario parameter
            String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                processor, ts, Alias, status, CommandPhaseType.RUN);
            newts.setAlias ( alias );
        }

        // Now resequence the data...
        try {
            resequenceData ( ts, newts, year_sequence, outputYearType );
            StringBuffer b = new StringBuffer ();
            for ( int iy = 0; iy < year_sequence.length; iy++ ) {
                if ( iy != 0 ) {
                    b.append (", ");
                }
                b.append ( "" + year_sequence[iy]);
            }
            newts.addToGenesis( "Resequenced data using " + outputYearType + " " + year_sequence.length +
                " years (new period is " + newts.getDate1() + " to " + newts.getDate2() + "): " + b.toString() );
        }
        catch ( Exception e ) {
            message = "Unexpected error resequencing the data in time series \"" + ts.getIdentifier() + "\" (" + e + ").";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            Message.printWarning ( 3, routine, e );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Check log file for details." ) );
            throw new CommandException ( message );
        }
        
        // Append the new time series at the end...
        
        TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, newts);
    }
    
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
        throw new CommandWarningException ( message );
    }
	
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
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
    String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String TableID = parameters.getValue ( "TableID" );
    String TableColumn = parameters.getValue ( "TableColumn" );
    String TableRowStart = parameters.getValue ( "TableRowStart" );
    String TableRowEnd = parameters.getValue ( "TableRowEnd" );
    String OutputYearType = parameters.getValue("OutputYearType");
    String OutputStart = parameters.getValue("OutputStart");
    //String OutputEnd = parameters.getValue("OutputEnd");
    String NewScenario = parameters.getValue ( "NewScenario" );
    String Alias = parameters.getValue("Alias");
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
		b.append ( "TableID=\"" + TableID + "\"" );
	}
    if ( (TableColumn != null) && (TableColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableColumn=\"" + TableColumn + "\"" );
    }
    if ( (TableRowStart != null) && (TableRowStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableRowStart=\"" + TableRowStart + "\"" );
    }
    if ( (TableRowEnd != null) && (TableRowEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableRowEnd=\"" + TableRowEnd + "\"" );
    }
    if ( (OutputYearType != null) && (OutputYearType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputYearType=" + OutputYearType );
    }
    if ( (OutputStart != null) && (OutputStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputStart=\"" + OutputStart + "\"" );
    }
    /*
    if ( (OutputEnd != null) && (OutputEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputEnd=\"" + OutputEnd + "\"" );
    }
    */
    if ( (NewScenario != null) && (NewScenario.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewScenario=\"" + NewScenario + "\"" );
    }
    if ((Alias != null) && (Alias.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Alias=\"" + Alias + "\"");
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
