// ResequenceTimeSeriesData_Command - This class initializes, checks, and runs the ResequenceTimeSeriesData() command.

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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import RTi.TS.TS;
import RTi.TS.TSUtil;

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
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.YearType;

/**
This class initializes, checks, and runs the ResequenceTimeSeriesData() command.
*/
public class ResequenceTimeSeriesData_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

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
    List<String> validList = new ArrayList<String>();
    validList.add ( "TSList" );
	validList.add ( "TSID" );
	validList.add ( "EnsembleID" );
	validList.add ( "TableID" );
    validList.add ( "TableColumn" );
    validList.add ( "TableRowStart" );
    validList.add ( "TableRowEnd" );
    validList.add ( "OutputYearType" );
    validList.add ( "OutputStart" );
    //valid_Vector.add ( "OutputEnd" );
	validList.add ( "NewScenario" );
    validList.add ( "Alias" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

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
	return (new ResequenceTimeSeriesData_JDialog ( parent, this )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Return the list of data objects created by this object in discovery mode.
The following classed can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
    List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discoveryTSList;
    }
    else {
        return null;
    }
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

//TODO SAM 2009-09-28 Enable reading the sequence from a row rather than a columns
/**
Get the year sequence as a row from the table.
*/
@SuppressWarnings("unused")
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
{	String routine = "ResequenceTimeSeriesData_Command.runCommand", message;
	int warning_level = 2;
    int log_level = 3;  // Warning level for non-user log messages
	String command_tag = "" + command_number;
	int warning_count = 0;

    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }

	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
	if ( TSList == null ) {
		TSList = "" + TSListType.ALL_TS;
	}
	String TSID = parameters.getValue ( "TSID" );
	String EnsembleID = parameters.getValue ( "EnsembleID" );
	String Alias = parameters.getValue("Alias");

	// Get the time series to process...
	List<TS> tslist = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        // FIXME - SAM 2011-02-02 This gets all the time series, not just the ones matching the request!
        tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, EnsembleID );
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
    		"\", TSID=\"" + TSID + "\") from processor.";
    		Message.printWarning(warning_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
    		status.addToLog ( commandPhase,
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
    		status.addToLog ( commandPhase,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    						message, "Report problem to software support." ) );
    	}
    	@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
    	tslist = tslist0;
    }
	if ( (tslist == null) || (tslist.size() == 0) ) {
		message = "No time series in list to process using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( commandPhase,
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
    PropList request_params = new PropList ( "" );
    CommandProcessorRequestResultsBean bean = null;
    PropList bean_PropList;
    if ( (OutputStart != null) && !OutputStart.equals("") ) {
        try {
        request_params = new PropList ( "" );
        request_params.set ( "DateTime", OutputStart );
        bean = null;
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting OutputStart DateTime(DateTime=" + OutputStart + ") from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( commandPhase,
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
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report to software support." ) );
            throw new InvalidCommandParameterException ( message );
        }
        else {
            OutputStart_DateTime = (DateTime)prop_contents;
        }
    }
    catch ( Exception e ) {
        message = "OutputStart \"" + OutputStart + "\" is invalid.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( commandPhase,
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
                status.addToLog ( commandPhase,
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
                status.addToLog ( commandPhase,
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
            status.addToLog ( commandPhase,
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
    
    DataTable table = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        // TODO SAM 2011-02-17 Evaluate whether this is needed
        //table = TSCommandProcessorUtil.getDiscoveryTablesFromCommandsBeforeCommand(
        //    (TSCommandProcessor)processor, this );
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
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
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table == null ) {
            message = "Unable to find table to process using TableID=\"" + TableID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        table = (DataTable)o_Table;
    }
    
    int TableColumn_int = -1;
    int TableRowStart_int = -1;
    int TableRowEnd_int = -1;
    String TableColumn = parameters.getValue ( "TableColumn" );
    String TableRowStart = parameters.getValue ( "TableRowStart" );
    String TableRowEnd = parameters.getValue ( "TableRowEnd" );
    if ( TableColumn != null ) {
        // Must be a named column
        if ( commandPhase == CommandPhaseType.RUN ) {
            try {
                TableColumn_int = table.getFieldIndex(TableColumn);
            }
            catch ( Exception e ) {
                message = "Unable to determine column number from column name \"" + TableColumn + "\".";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the table has a column matching \"" + TableColumn + "\"." ) );
                throw new CommandException ( message );
            }
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
        if ( commandPhase == CommandPhaseType.RUN ) {
            year_sequence = getTableColumn ( table, TableColumn_int, TableRowStart_int, TableRowEnd_int,
                command_tag, warning_level, status );
        }
    }
    catch ( Exception e ) {
        message = "Error getting year sequence from table.";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check the table format and parameters that specify the cells to use." ) );
        throw new CommandException ( message );
    }

	// Now try to process.

    int nyears = 0;
    if ( year_sequence != null ) {
        nyears = year_sequence.length;
    }
    String NewScenario = parameters.getValue("NewScenario");
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    TS ts = null;
    TS newts = null;
    List<TS> discoveryTSList = new Vector<TS>();
    for ( int i = 0; i < size; i++ ) {
        // Create a copy of the original, but with the new scenario.
        ts = (TS)tslist.get(i);
        notifyCommandProgressListeners ( i, size, (float)-1.0, "Resequencing " +
            ts.getIdentifier().toStringAliasAndTSID() );
        if ( ts.getDataIntervalBase() != TimeInterval.MONTH ) {
            message = "Resequencing currently only can be applied to monthly time series.";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
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
        if ( commandPhase == CommandPhaseType.RUN ) {
            newts.allocateDataSpace();
            // Set all data to missing so as to not confuse with old data...
            TSUtil.setConstant ( newts, newts.getMissing() );
        }
        // Reset the description because don't want any extra information like constant note
        newts.setDescription ( ts.getDescription() );
        if ( (Alias != null) && (Alias.length() > 0) ) {
            // Set the alias to the desired string - this is impacted by the Scenario parameter
            String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                processor, ts, Alias, status, commandPhase);
            newts.setAlias ( alias );
        }

        // Now resequence the data...
        try {
            if ( commandPhase == CommandPhaseType.RUN ) {
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
        }
        catch ( Exception e ) {
            message = "Unexpected error resequencing the data in time series \"" + ts.getIdentifier() + "\" (" + e + ").";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            Message.printWarning ( 3, routine, e );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Check log file for details." ) );
            throw new CommandException ( message );
        }
        
        // Append the new time series at the end...
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Just want time series headers initialized
            discoveryTSList.add ( newts );
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
            TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, newts);
        }
    }
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Just want time series headers initialized
        setDiscoveryTSList ( discoveryTSList );
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
Set the list of time series read in discovery phase.
@param discoveryTSList list of time series created during discovery phase
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList ) {
    __discoveryTSList = discoveryTSList;
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
    	"TableColumn",
    	"TableRowStart",
    	"TableRowEnd",
    	"OutputYearType",
    	"OutputStart",
    	//"OutputEnd",
    	"NewScenario",
    	"Alias"
	};
	return this.toString(parameters, parameterOrder);
}

}