package rti.tscommandprocessor.commands.ts;

import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;
import RTi.TS.TSUtil_CreateTracesFromTimeSeries;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;

/**
<p>
This class initializes, checks, and runs the CreateTraces() command.
</p>
*/
public class CreateTraces_Command extends AbstractCommand implements Command
{

/**
Protected data members shared with the dialog and other related classes.
*/
protected final String _NoShift = "NoShift";
protected final String _ShiftToReference = "ShiftToReference";

/**
Constructor.
*/
public CreateTraces_Command ()
{	super();
	setCommandName ( "CreateTraces" );
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
{	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
    
    String TSID = parameters.getValue ( "TSID" );
    String ReferenceDate = parameters.getValue ( "ReferenceDate" );
    String ShiftDataHow = parameters.getValue ( "ShiftDataHow" );
    String TraceLength = parameters.getValue ( "TraceLength" );
    
    if ( (TSID == null) || (TSID.length() == 0) ) {
        message = "A time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
    }
    if ( (ReferenceDate != null) && !ReferenceDate.equals("") ) {
        try {
            DateTime.parse ( ReferenceDate );
        }
        catch ( Exception e ) {
            message = "The reference date \"" + ReferenceDate + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date (or blank) for the reference date." ) );
        }
    }
    
    // Check the trace length...
    try {
        TimeInterval.parseInterval ( TraceLength );
    }
    catch ( Exception e ) {
        message = "Trace length \"" + TraceLength +"\" is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid interval (e.g., 1Year)." ) );
    }
    

    if ( (ShiftDataHow != null) && (ShiftDataHow.length() != 0) &&
            !ShiftDataHow.equalsIgnoreCase(_ShiftToReference) &&
            !ShiftDataHow.equalsIgnoreCase(_NoShift)) {
        message = "The ShiftDataHow parameter value is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify as blank, " + _NoShift + " (default), or " +
                        _ShiftToReference + "." ) );
    }

	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
    valid_Vector.add ( "TSID" );
	valid_Vector.add ( "TraceLength" );
	valid_Vector.add ( "ReferenceDate" );
    valid_Vector.add ( "ShiftDataHow" );
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
	return (new CreateTraces_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{   if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
        //TODO SAM 2007-12-12 This whole block of code needs to be
        // removed as soon as commands have been migrated to the new syntax.
        //
        // Old syntax without named parameters.
        Vector v = StringUtil.breakStringList ( command_string,"(),",0 );
        String TSID = "";
        String TraceLength = "";
        String ReferenceDate = "";
        String ShiftDataHow = "";
        if ( v != null ) {
            if ( v.size() >= 2 ) {
                TSID = ((String)v.elementAt(1)).trim();
            }
            if ( v.size() >= 3 ) {
                TraceLength = ((String)v.elementAt(2)).trim();
            }
            if ( v.size() >= 4 ) {
                ReferenceDate = ((String)v.elementAt(3)).trim();
            }
            if ( v.size() >= 5 ) {
                ShiftDataHow = ((String)v.elementAt(4)).trim();
            }
        }

        // Set parameters and new defaults...

        PropList parameters = new PropList ( getCommandName() );
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        if ( TSID.length() > 0 ) {
            parameters.set ( "TSID", TSID );
        }
        if ( TraceLength.length() > 0 ) {
            parameters.set ( "TraceLength", TraceLength );
        }
        if ( ReferenceDate.length() > 0 ) {
            parameters.set ( "ReferenceDate", ReferenceDate );
        }
        if ( ShiftDataHow.length() > 0 ) {
            parameters.set ( "ShiftDataHow", ShiftDataHow );
        }
        parameters.setHowSet ( Prop.SET_UNKNOWN );
        setCommandParameters ( parameters );
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
{	String routine = "CreateTraces_Command.runCommand", message;
	int warning_level = 2;
    int log_level = 3;  // Non-user warning level
	String command_tag = "" + command_number;
	int warning_count = 0;

    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
    CommandPhaseType command_phase = CommandPhaseType.RUN;

	PropList parameters = getCommandParameters();
	String TSID = parameters.getValue ( "TSID" );
    String TraceLength = parameters.getValue ( "TraceLength" );
    String ReferenceDate = parameters.getValue ( "ReferenceDate" );
    String ShiftDataHow = parameters.getValue ( "ShiftDataHow" );

    // Get the time series to process.  The time series list is searched backwards until the first match...

    PropList request_params = new PropList ( "" );
    request_params.set ( "CommandTag", command_tag );
    request_params.set ( "TSID", TSID );
    CommandProcessorRequestResultsBean bean = null;
    try { bean =
        processor.processRequest( "GetTimeSeriesForTSID", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TS = bean_PropList.getContents ( "TS");
    TS ts = null;
    if ( o_TS == null ) {
        message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\" from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
    }
    else {
        ts = (TS)o_TS;
    }
     
    if ( ts == null ) {
        message = "Unable to find time series to analyze using TSID \"" + TSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }
    
    DateTime ReferenceDate_DateTime = null;
    if ( (ReferenceDate != null) && !ReferenceDate.equals("") ) {
        try {
            ReferenceDate_DateTime = DateTime.parse(ReferenceDate);
        }
        catch ( Exception e ) {
            message="Reference date \"" + ReferenceDate + "\" is invalid.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the reference date is valid." ) );
        }
    }
    
    String InputStart = parameters.getValue ( "InputStart" );
    DateTime InputStart_DateTime = null;
    if ( (InputStart != null) && (InputStart.length() > 0) ) {
        request_params = new PropList ( "" );
        request_params.set ( "DateTime", InputStart );
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting DateTime(DateTime=" + InputStart + ") from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object prop_contents = bean_PropList.getContents ( "DateTime" );
        if ( prop_contents == null ) {
            message = "Null value for DateTime(DateTime=" + InputStart + "\") returned from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Verify that a valid InputStart string has been specified." ) );
        }
        else {  InputStart_DateTime = (DateTime)prop_contents;
        }
    }
    else {  // Get from the processor...
        try {
            Object o = processor.getPropContents ( "InputStart" );
            if ( o != null ) {
                InputStart_DateTime = (DateTime)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting InputStart from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
        }
    }
    String InputEnd = parameters.getValue ( "InputEnd" );
    DateTime InputEnd_DateTime = null;
    if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
        request_params = new PropList ( "" );
        request_params.set ( "DateTime", InputEnd );
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting DateTime(DateTime=" + InputEnd + ") from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object prop_contents = bean_PropList.getContents ( "DateTime" );
        if ( prop_contents == null ) {
            message = "Null value for DateTime(DateTime=" + InputEnd + ") returned from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Verify that a valid InputEnd has been specified." ) );
        }
        else {  InputEnd_DateTime = (DateTime)prop_contents;
        }
    }
    else {  // Get from the processor...
        try { Object o = processor.getPropContents ( "InputEnd" );
            if ( o != null ) {
                InputEnd_DateTime = (DateTime)o;
            }
        }
        catch ( Exception e ) {
            // Not fatal, but of use to developers.
            message = "Error requesting InputEnd from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
        }
    }
	
	// Now try to process.
    
    Vector tslist = null;
    try {
        TSUtil_CreateTracesFromTimeSeries util = new TSUtil_CreateTracesFromTimeSeries();
        tslist = util.getTracesFromTS ( ts, TraceLength,
                ReferenceDate_DateTime, ShiftDataHow,
                InputStart_DateTime,
                InputEnd_DateTime );
    }
    catch ( Exception e ) {
        message = "Unexpected error creating traces from time series \"" + ts.getIdentifier() + "\"";
        Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Check log file for details." ) );
        throw new CommandException ( message );
    }
    
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    Message.printStatus ( 2, routine, "Created " + size + " traces from time series \"" + ts.getIdentifier() + "\"" );
    
    // Update the data to the processor so that appropriate actions are taken...

    if ( tslist != null ) {
        TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
        TSCommandProcessorUtil.appendTimeSeriesListToResultsList(processor, this, tslist);
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
    String TSID = parameters.getValue ( "TSID" );
    String TraceLength = parameters.getValue ( "TraceLength" );
    String ReferenceDate = parameters.getValue ( "ReferenceDate" );
    String ShiftDataHow = parameters.getValue ( "ShiftDataHow" );
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (TraceLength != null) && (TraceLength.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TraceLength=" + TraceLength );
	}
    if ( (ReferenceDate != null) && (ReferenceDate.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ReferenceDate=\"" + ReferenceDate + "\"");
    }
    if ( (ShiftDataHow != null) && (ShiftDataHow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ShiftDataHow=" + ShiftDataHow );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
