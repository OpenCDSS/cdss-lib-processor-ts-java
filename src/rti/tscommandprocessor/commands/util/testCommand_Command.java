
package rti.tscommandprocessor.commands.util;

import java.util.Vector;
import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;

/**
<p>
This class initializes, checks, and runs the testCommand() command.
*/
public class testCommand_Command extends AbstractCommand
implements Command
{

/**
Constructor.
*/
public testCommand_Command ()
{	super();
	setCommandName ( "TestCommand" );
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
public void checkCommandParameters (	PropList parameters, String command_tag,
					int warning_level )
throws InvalidCommandParameterException
{	String InitializeStatus = parameters.getValue ( "InitializeStatus" );
	String DiscoveryStatus = parameters.getValue ( "DiscoveryStatus" );
	String RunStatus = parameters.getValue ( "RunStatus" );
	String warning = "";
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// TODO SAM 2007-09-09 Need to use Enum when Java 1.5+

    String istatus = null;
	for ( int i = 0; i < 3; i++ ) {
		if ( i == 0 ) {
			istatus = InitializeStatus;
		}
		else if ( i == 1 ) {
			istatus = DiscoveryStatus;
		}
		else if ( i == 2 ) {
			istatus = RunStatus;
		}
		if ( (istatus != null) && (istatus.length() > 0) &&
			(!InitializeStatus.equalsIgnoreCase(CommandStatusType.UNKNOWN.toString()) &&
			!InitializeStatus.equalsIgnoreCase(CommandStatusType.SUCCESS.toString()) &&
			!InitializeStatus.equalsIgnoreCase(CommandStatusType.WARNING.toString()) &&
			!InitializeStatus.equalsIgnoreCase(CommandStatusType.FAILURE.toString())) ) {
			warning += "\nI" + status + " must be " + CommandStatusType.UNKNOWN +
			", " + CommandStatusType.UNKNOWN +
			", " + CommandStatusType.SUCCESS +
			", " + CommandStatusType.WARNING +
			", or " + CommandStatusType.FAILURE;
		}
	}
	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
	valid_Vector.add ( "InitializeStatus" );
	valid_Vector.add ( "DiscoveryStatus" );
	valid_Vector.add ( "RunStatus" );
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
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new testCommand_JDialog ( parent, this )).ok();
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	PropList parameters = getCommandParameters();

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	
	String InitializeStatus = parameters.getValue ( "InitializeStatus" );
	String DiscoveryStatus = parameters.getValue ( "DiscoveryStatus" );
	String RunStatus = parameters.getValue ( "RunStatus" );
	
	String routine = getClass().getName() + ".runCommand";
	Message.printStatus ( 2, routine,
			"InitializeStatus=\"" + InitializeStatus + "\" " +
			"DiscoveryStatus=\"" + DiscoveryStatus + "\" " +
			"RunStatus=\"" + RunStatus + "\"" );
	
	if ( InitializeStatus.equalsIgnoreCase(CommandStatusType.UNKNOWN.toString())) {
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.UNKNOWN,
						"Command status is unknown.", ""));
	}
	else if ( InitializeStatus.equalsIgnoreCase(CommandStatusType.SUCCESS.toString())) {
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.SUCCESS,
						"Success.", ""));
	}
	else if ( InitializeStatus.equalsIgnoreCase(CommandStatusType.WARNING.toString())) {
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.WARNING,
						"There is a warning.", "Don't have a recommendation."));
	}
	else if ( InitializeStatus.equalsIgnoreCase(CommandStatusType.FAILURE.toString())) {
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						"There is a failure.", "Don't have a recommendation."));
	}
	
	if ( DiscoveryStatus.equalsIgnoreCase(CommandStatusType.UNKNOWN.toString())) {
		status.addToLog ( CommandPhaseType.DISCOVERY,
				new CommandLogRecord(CommandStatusType.UNKNOWN,
						"Command status is unknown.", ""));
	}
	else if ( DiscoveryStatus.equalsIgnoreCase(CommandStatusType.SUCCESS.toString())) {
		status.addToLog ( CommandPhaseType.DISCOVERY,
				new CommandLogRecord(CommandStatusType.SUCCESS,
						"Success.", ""));
	}
	else if ( DiscoveryStatus.equalsIgnoreCase(CommandStatusType.WARNING.toString())) {
		status.addToLog ( CommandPhaseType.DISCOVERY,
				new CommandLogRecord(CommandStatusType.WARNING,
						"There is a warning.", "Don't have a recommendation."));
	}
	else if ( DiscoveryStatus.equalsIgnoreCase(CommandStatusType.FAILURE.toString())) {
		status.addToLog ( CommandPhaseType.DISCOVERY,
				new CommandLogRecord(CommandStatusType.FAILURE,
						"There is a failure.", "Don't have a recommendation."));
	}
	
	if ( RunStatus.equalsIgnoreCase(CommandStatusType.UNKNOWN.toString())) {
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.UNKNOWN,
						"Command status is unknown.", ""));
	}
	else if ( RunStatus.equalsIgnoreCase(CommandStatusType.SUCCESS.toString())) {
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.SUCCESS,
						"Success.", ""));
	}
	else if ( RunStatus.equalsIgnoreCase(CommandStatusType.WARNING.toString())) {
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.WARNING,
						"There is a warning.", "Don't have a recommendation."));
	}
	else if ( RunStatus.equalsIgnoreCase(CommandStatusType.FAILURE.toString())) {
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						"There is a failure.", "Don't have a recommendation."));
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String InitializeStatus = props.getValue ( "InitializeStatus" );
	String DiscoveryStatus = props.getValue ( "DiscoveryStatus" );
	String RunStatus = props.getValue ( "RunStatus" );
	StringBuffer b = new StringBuffer();
	if ( (InitializeStatus != null) && (InitializeStatus.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InitializeStatus=" + InitializeStatus );
	}
	if ( (DiscoveryStatus != null) && (DiscoveryStatus.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DiscoveryStatus=" + DiscoveryStatus );
	}
	if ( (RunStatus != null) && (RunStatus.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "RunStatus=" + RunStatus );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}