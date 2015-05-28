package rti.tscommandprocessor.commands.time;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the SetInputPeriod() command.
*/
public class SetInputPeriod_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public SetInputPeriod_Command ()
{	super();
	setCommandName ( "SetInputPeriod" );
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
{	String InputStart = parameters.getValue ( "InputStart" );
	String InputEnd = parameters.getValue ( "InputEnd" );
	String warning = "";
	String message;
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	// When checking InputStart and InputEnd, all we care about is that the
	// syntax is correct.  In runCommand() the parameter will be reparsed with runtime data...

	PropList dateprops = new PropList ( "SetInputPeriod" );
	// The instance is not needed when checking syntax but will be checked at runtime.
	DateTime now = new DateTime(DateTime.DATE_CURRENT);
	dateprops.set ( new Prop ("InputStart", now, now.toString()) );
	DateTime InputStart_DateTime = null;
	DateTime InputEnd_DateTime = null;
	if ( (InputStart != null) && !InputStart.isEmpty() && !InputStart.startsWith("${") ) {
		try {
		    // This handles special syntax like "CurrentToHour" and "CurrentToHour - 6Hour"
			InputStart_DateTime = DateTime.parse(InputStart, dateprops );
		}
		catch ( Exception e ) {
			message = "The input start date/time \"" + InputStart +	"\" is not a valid date/time string.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid date/time for the input start." ) );
			Message.printWarning ( 3, "", e );
		}
	}
	if ( (InputEnd != null) && !InputEnd.isEmpty() && !InputEnd.startsWith("${")) {
		try {
		    // This handles special syntax like "CurrentToHour" and "CurrentToHour - 6Hour"
			InputEnd_DateTime = DateTime.parse(InputEnd, dateprops );
		}
		catch ( Exception e ) {
			message = "The input end date/time \"" + InputEnd +	"\" is not a valid date/time string.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid date/time for the input end." ) );
			Message.printWarning ( 3, "", e );
		}
	}
	if ( (InputStart_DateTime != null) && (InputEnd_DateTime != null) ) {
		if ( InputStart_DateTime.greaterThan(InputEnd_DateTime) ) {
			message = "The start date/time is later than the end date/time.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Correct so that the end time is >= the start time." ) );
		}
		if ( InputStart_DateTime.getPrecision() != InputEnd_DateTime.getPrecision() ) {
			message = "The precision of the start and end date/times are different.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Correct so that the date/times are specified to the same precision." ) );
		}
	}
	
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(2);
	validList.add ( "InputStart" );
	validList.add ( "InputEnd" );
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
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new SetInputPeriod_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	String routine = "SetInputPeriod_Command.parseCommand";

	String InputStart = null;
	String InputEnd = null;
	if ( (command.indexOf('=') > 0) || command.endsWith("()") ) {
		// Current syntax...
	    super.parseCommand(command);
	}
	else {
		// TODO SAM 2005-04-29 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax where the only parameter is a single TSID or * to fill all.
		List tokens = StringUtil.breakStringList ( command,"(,)", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() != 3) ) {
			throw new InvalidCommandSyntaxException ("Bad command \"" + command + "\"" );
		}
		if ( StringUtil.startsWithIgnoreCase(command,"setQueryPeriod")){
			Message.printStatus ( 3, routine,
			"Automatically converting setQueryPeriod() to SetInputPeriod()" );
		}
		InputStart = ((String)tokens.get(1)).trim();
		if ( InputStart.equals("*") ) {
		    // Phase out old style
			InputStart = "";
		}
		InputEnd = ((String)tokens.get(2)).trim();
		if ( InputEnd.equals("*") ) {
		    // Phase out old style
			InputEnd = "";
		}

		// Set parameters and new defaults...

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( InputStart.length() > 0 ) {
			parameters.set ( "InputStart", InputStart );
		}
		if ( InputEnd.length() > 0 ) {
			parameters.set ( "InputEnd", InputEnd );
		}
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	
	String InputStart = parameters.getValue ( "InputStart" );
	String InputEnd = parameters.getValue ( "InputEnd" );
	DateTime InputStart_DateTime = null;
	DateTime InputEnd_DateTime = null;
	PropList dateprops = new PropList ( "SetInputPeriod" );
	try {
		// If the parameters start with ${ get from the utility code
		if ( (InputStart != null) && !InputStart.isEmpty() ) {
			if ( InputStart.startsWith("${") ) {
			    // Otherwise parse the date/times to take advantage of run-time data values...
				try {
					InputStart_DateTime = TSCommandProcessorUtil.getDateTime ( InputStart, "InputStart", processor,
						status, warning_level, command_tag );
				}
				catch ( InvalidCommandParameterException e ) {
					// Warning will have been added above...
				}
			}
			else {
			    // Otherwise parse the date/times to take advantage of run-time data values...
				try {
				    // This handles special syntax like "NowToHour" and "NowToHour - 6Hour"
					InputStart_DateTime = DateTime.parse(InputStart, dateprops );
				}
				catch ( Exception e ) {
					message = "The input start date/time \"" + InputStart +	"\" is not a valid date/time.";
					Message.printWarning ( warning_level, 
					MessageUtil.formatMessageTag(command_tag,
					++warning_count), routine, message );
					status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid date/time for the input start (use command editor)." ) );
					Message.printWarning ( 3, routine, e );
				}
			}
			if ( InputStart_DateTime != null ) {
				// Set the value and contents, which may be used when setting the InputEnd below...
				Prop prop = new Prop ( "InputStart",InputStart_DateTime,InputStart_DateTime.toString() );
				dateprops.set ( prop );
			}
		}
		if ( (InputEnd != null) && !InputEnd.isEmpty() ) {
			if ( InputEnd.startsWith("${") ) {
			    // Otherwise parse the date/times to take advantage of run-time data values...
				try {
					InputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( InputEnd, "InputEnd", processor,
						status, warning_level, command_tag );
				}
				catch ( InvalidCommandParameterException e ) {
					// Warning will have been added above...
				}
			}
			else {
				try {
				    InputEnd_DateTime = DateTime.parse(InputEnd, dateprops );
				}
				catch ( Exception e ) {
					message = "The input end date/time \"" + InputEnd +	"\" is not a valid date/time.";
					Message.printWarning ( warning_level, 
					MessageUtil.formatMessageTag(command_tag,
					++warning_count), routine, message );
					Message.printWarning ( 3, routine, e );
					status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid date/time for the input end (use command editor)." ) );
				}
			}
		}
		if ( warning_count > 0 ) {
			// Input error...
			message = "Cannot process command parameters - invalid date/time.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
			throw new InvalidCommandParameterException ( message );
		}
		processor.setPropContents ( "InputStart", InputStart_DateTime);
        Message.printStatus(2, routine, "Input period start set to:  " + InputStart_DateTime );
		processor.setPropContents ( "InputEnd", InputEnd_DateTime );
        Message.printStatus(2, routine, "Input period end set to:  " + InputEnd_DateTime );
	}
	catch ( Exception e ) {
		message = "Unexpected error setting input period in processor (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Report problem to software support (see log file for details)." ) );
		throw new CommandException ( message );
	}
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{   if ( props == null ) {
        return getCommandName() + "()";
    }
    String InputStart = props.getValue( "InputStart" );
    String InputEnd = props.getValue( "InputEnd" );
    StringBuffer b = new StringBuffer ();
    if ( (InputStart != null) && (InputStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputStart=\"" + InputStart + "\"");
    }
    if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputEnd=\"" + InputEnd + "\"");
    }
    return getCommandName() + "(" + b.toString() + ")";
}

}