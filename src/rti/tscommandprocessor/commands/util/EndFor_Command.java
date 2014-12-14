package rti.tscommandprocessor.commands.util;

import java.util.ArrayList;
import java.util.List;

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
This class initializes, checks, and runs the EndFor() command.
*/
public class EndFor_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public EndFor_Command ()
{	super();
	setCommandName ( "EndFor" );
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
{	String routine = getCommandName() + "_checkCommandParameters";
	String Name = parameters.getValue ( "Name" );
	String warning = "";
	String message;
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (Name == null) || Name.equals("") ) {
        message = "A name for the for loop must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the name." ) );
    }

	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(1);
	validList.add ( "Name" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );
	
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine,
		warning );
		throw new InvalidCommandParameterException ( warning );
	}
	
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new EndFor_JDialog ( parent, this )).ok();
}

/**
Return the name of the if command.
@return the name of the if command, should not be null.
*/
public String getName ()
{
    return getCommandParameters().getValue("Name");
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws CommandWarningException, CommandException
{	//String routine = "EndFor_Command.runCommand", message;
	//int warning_level = 2;
	//String command_tag = "" + command_number;
	//int warning_count = 0;
	
	//PropList parameters = getCommandParameters();
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	
	//String Name = parameters.getValue ( "Name" );

	// Command does not do anything but is check by the processor

	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{   if ( props == null ) {
        return getCommandName() + "()";
    }
    String Name = props.getValue( "Name" );
    StringBuffer b = new StringBuffer ();
    if ( (Name != null) && (Name.length() > 0) ) {
        b.append ( "Name=\"" + Name + "\"" );
    }
    return getCommandName() + "(" + b.toString() + ")";
}

}