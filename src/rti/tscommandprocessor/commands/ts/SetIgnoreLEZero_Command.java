package rti.tscommandprocessor.commands.ts;

import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the SetIgnoreLEZero() command.
*/
public class SetIgnoreLEZero_Command extends AbstractCommand
implements Command
{
    
protected final String _True = "True";
protected final String _False = "False";

/**
Constructor.
*/
public SetIgnoreLEZero_Command ()
{	super();
	setCommandName ( "SetIgnoreLEZero" );
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
{	String IgnoreLEZero = parameters.getValue ( "IgnoreLEZero" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the file to remove is not checked during initialization
	// because files may be created dynamically at runtime.

	if ( (IgnoreLEZero != null) && !IgnoreLEZero.equals("") ) {
		if (	!IgnoreLEZero.equalsIgnoreCase(_False) &&
			!IgnoreLEZero.equalsIgnoreCase(_True) ) {
			message = "The IgnoreLEZero parameter \"" + IgnoreLEZero + "\" must be " + _False +
			" or " + _True + ".";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _False + " or " + _True + "."));
		}
	}
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>();
	validList.add ( "IgnoreLEZero" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
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
	return (new SetIgnoreLEZero_JDialog ( parent, this )).ok();
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
{
    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
        // TODO SAM 2008-07-08 This whole block of code needs to be
        // removed as soon as commands have been migrated to the new syntax.
    	List<String> v = StringUtil.breakStringList(command_string, "(),", StringUtil.DELIM_ALLOW_STRINGS );
        int ntokens = 0;
        if ( v != null ) {
            ntokens = v.size();
        }
        String IgnoreLEZero = "";
        if ( ntokens >= 2 ) {
            // Output year type...
            IgnoreLEZero = ((String)v.get(1)).trim();
        }

        // Set parameters and new defaults...

        PropList parameters = new PropList ( getCommandName() );
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        if ( IgnoreLEZero.length() > 0 ) {
            parameters.set ( "IgnoreLEZero", IgnoreLEZero );
        }
        parameters.setHowSet ( Prop.SET_UNKNOWN );
        setCommandParameters ( parameters );
    }
}

/**
Run the command.
@param command_line Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "SetIgnoreLEZero_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	
	String IgnoreLEZero = parameters.getValue ( "IgnoreLEZero" );
	
	try {
        // Set the output year type...
		Message.printStatus ( 2, routine, "Set IgnoreLEZero to \"" + IgnoreLEZero + "\".");
		if ( IgnoreLEZero.equalsIgnoreCase(_True) ) {
		    Message.printStatus ( 2, routine,
		    "Values <= 0 will be treated as missing when computing historical averages." );
		    processor.setPropContents ( "IgnoreLEZero", new Boolean(true) );
		}
		else if ( IgnoreLEZero.equalsIgnoreCase(_False) ){
		    Message.printStatus ( 2, routine,
            "Values <= 0 WILL NOT be treated as missing when computing historic averages." );
		    processor.setPropContents ( "IgnoreLEZero", new Boolean(false) );
		}
	}
	catch ( Exception e ) {
		message = "Unexpected error setting IgnoreLEZero to \"" + IgnoreLEZero + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
		throw new CommandException ( message );
	}

	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String IgnoreLEZero = parameters.getValue("IgnoreLEZero");
	StringBuffer b = new StringBuffer ();
	if ( (IgnoreLEZero != null) && (IgnoreLEZero.length() > 0) ) {
		b.append ( "IgnoreLEZero=" + IgnoreLEZero );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
