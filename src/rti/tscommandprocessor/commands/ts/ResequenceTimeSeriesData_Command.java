package rti.tscommandprocessor.commands.ts;

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
(recommended is 2 for initialization, and 1 for interactive command editor
dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String TableID = parameters.getValue ( "TableID" );
    String TableRow = parameters.getValue ( "TableRow" );
    String TableColumnStart = parameters.getValue ( "TableColumnStart" );
    String TableColumnEnd = parameters.getValue ( "TableColumnEnd" );
    String OutputStart = parameters.getValue ( "OutputStart" );
    String OutputEnd = parameters.getValue ( "OutputEnd" );
    String NewScenario = parameters.getValue ( "NewScenario" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
    
	if ( (TableID == null) || (TableID.length() == 0) ) {
		message = "The table identifier must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify a table identifier." ) );
	}
    if ( (TableRow == null) || (TableRow.length() == 0) ) {
        message = "The table row must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a table row for the year sequence." ) );
    }
    else {
        if ( !StringUtil.isInteger(TableRow) ) {
            message = "The table row (" + TableRow + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an integer for the table row." ) );
        }
    }
    if ( (TableColumnStart == null) || (TableColumnStart.length() == 0) ) {
        message = "The starting table column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a starting table column for the year sequence." ) );
    }
    else {
        if ( !StringUtil.isInteger(TableColumnStart) ) {
            message = "The starting table column (" + TableColumnStart + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an integer for the starting table column." ) );
        }
    }
    if ( (TableColumnEnd == null) || (TableColumnEnd.length() == 0) ) {
        message = "The ending table column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an ending table column for the year sequence." ) );
    }
    else {
        if ( !StringUtil.isInteger(TableColumnEnd) ) {
            message = "The table ending column (" + TableColumnEnd + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an integer for the ending table column." ) );
        }
    }
    if (    (OutputStart != null) && !OutputStart.equals("") &&
            !OutputStart.equalsIgnoreCase("OutputStart") &&
            !OutputStart.equalsIgnoreCase("OutputEnd") ) {
        try {   DateTime.parse(OutputStart);
        }
        catch ( Exception e ) {
            message = "Output start date/time \"" + OutputStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid output start date/time, OutputStart, or OutputEnd." ) );
        }
    }
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
    if ( (NewScenario == null) || (NewScenario.length() == 0) ) {
        message = "The new scenario must be specified to differentiate output from input time series.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a new scenario." ) );
    }

	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
	valid_Vector.add ( "TSID" );
	valid_Vector.add ( "TableID" );
    valid_Vector.add ( "TableRow" );
    valid_Vector.add ( "TableColumnStart" );
    valid_Vector.add ( "TableColumnEnd" );
    valid_Vector.add ( "OutputStart" );
    valid_Vector.add ( "OutputEnd" );
	valid_Vector.add ( "NewScenario" );
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
	return (new ResequenceTimeSeriesData_JDialog ( parent, this )).ok();
}

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
                year_sequence[ival] = StringUtil.atoi(s);
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
    /*
    int [] year_sequence = new int[] //[nyears];
                                   {1986,1942,1952,1987,1958,1995,1977,1988,
                                  1940,1972,1991,1937,1979,1956,1986,
                                  1943,1991,1973,1958,1992,1962,1976,
                                  1963,1950,1970,1947,1947,1966,1984,
                                  1942,1965,1996,1974,1963,1955,1994,
                                  1984,1941,1944,1987,1997,1964,1939,
                                  1992,1957,1968,1940,1954,1958,1972,
                                  1961,1974,1991,1973,1972,1983,1987,
                                  1992,1951,1951,1993,1975,1944,1949,
                                  1976,1955,1986,1954,1955,1942,1970,
                                  1966,1955,1974,1969,1986,1951,1966,
                                  1989,1945,1960,1937,1972,1988,1994,
                                  1972,1952,1962,1972,1995,1991,1968,
                                  1993,1974,1957,1997,1989,1937,1950,1938 };
                                  */
    return year_sequence;
}

// parseCommand from base class

/**
Resequence the data in the old time series, transferring to the new time series, using
the year sequence as the map.
*/
private void resequenceData ( TS oldts, TS newts, int [] year_sequence )
{
    //
    int base = newts.getDataIntervalBase();
    int mult = newts.getDataIntervalMult();
    DateTime date2 = newts.getDate2();
    int iyear = 0;
    DateTime dateold = null;
    double oldval;
    for ( DateTime date = new DateTime(newts.getDate1()); date.lessThanOrEqualTo(date2);
        date.addInterval(base,mult), dateold.addInterval(base,mult)) {
        if ( date.getMonth() == 1 ) {
            // Reset the year to get the specified data...
            dateold = new DateTime(date);
            dateold.setYear ( year_sequence[iyear++] );
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
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
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

	// Get the time series to process...
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
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
	Vector tslist = (Vector)o_TSList;
	if ( tslist.size() == 0 ) {
		message = "Zero time series in list to process using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.WARNING,
						message, "Confirm that time series are available (may be OK for partial run)." ) );
	}
    
    String OutputStart = parameters.getValue ( "OutputStart" );
    String OutputEnd = parameters.getValue ( "OutputEnd" );
    DateTime OutputStart_DateTime = null;
    DateTime OutputEnd_DateTime = null;
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
    }
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
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    Object o_Table = bean_PropList.getContents ( "Table" );
    if ( o_Table == null ) {
        message = "Unable to find table to process using TableID=\"" + TableID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
    }
    DataTable table = (DataTable)o_Table;
    
    String TableRow = parameters.getValue ( "TableRow" );
    String TableColumnStart = parameters.getValue ( "TableColumnStart" );
    String TableColumnEnd = parameters.getValue ( "TableColumnEnd" );
    int TableRow_int = StringUtil.atoi ( TableRow );
    int TableColumnStart_int = StringUtil.atoi ( TableColumnStart );
    int TableColumnEnd_int = StringUtil.atoi ( TableColumnEnd );
    int [] year_sequence = null;
    try {
        year_sequence = getTableRow ( table, TableRow_int, TableColumnStart_int, TableColumnEnd_int,
                command_tag, warning_level, status );
    }
    catch ( Exception e ) {
        message = "Error getting year sequence from table.";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the table format and parameters that specify the cells to use." ) );
        throw new InvalidCommandParameterException ( message );
    }

	// Now try to process.

    String NewScenario = parameters.getValue("NewScenario");
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    TS ts = null;
    TS newts = null;
    for ( int i = 0; i < size; i++ ) {
        // Create a copy of the original, but with the new scenario.
        ts = (TS)tslist.elementAt(i);
        newts = (TS)ts.clone();
        newts.getIdentifier().setScenario(NewScenario);
        // Allocate space for the new time series, for the requested years...
        newts.setDate1(OutputStart_DateTime);
        newts.setDate2(OutputEnd_DateTime);
        newts.allocateDataSpace();
        // Set all data to missing so as to not confuse with old data...
        TSUtil.setConstant ( newts, newts.getMissing() );

        // Now resequence the data...
        try {
            resequenceData ( ts, newts, year_sequence );
        }
        catch ( Exception e ) {
            message = "Unexpected error resequencing the data in time series \"" + ts.getIdentifier() + "\"";
            Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            Message.printWarning ( 3, routine, e );
            status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check log file for details." ) );
            throw new CommandException ( message );
        }
        
        // Append the new time series at the end...
        
        TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, newts);
    }
    
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag, ++warning_count),
            routine,message);
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
    String TableID = parameters.getValue ( "TableID" );
    String TableRow = parameters.getValue ( "TableRow" );
    String TableColumnStart = parameters.getValue ( "TableColumnStart" );
    String TableColumnEnd = parameters.getValue ( "TableColumnEnd" );
    String OutputStart = parameters.getValue("OutputStart");
    String OutputEnd = parameters.getValue("OutputEnd");
    String NewScenario = parameters.getValue ( "NewScenario" );
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
	if ( (TableID != null) && (TableID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TableID=\"" + TableID + "\"" );
	}
    if ( (TableRow != null) && (TableRow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableRow=\"" + TableRow + "\"" );
    }
    if ( (TableColumnStart != null) && (TableColumnStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableColumnStart=\"" + TableColumnStart + "\"" );
    }
    if ( (TableColumnEnd != null) && (TableColumnEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableColumnEnd=\"" + TableColumnEnd + "\"" );
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
    if ( (NewScenario != null) && (NewScenario.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewScenario=\"" + NewScenario + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
