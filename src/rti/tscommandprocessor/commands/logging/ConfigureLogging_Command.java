package rti.tscommandprocessor.commands.logging;

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
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;

/**
This class initializes, checks, and runs the Message() command.
*/
public class ConfigureLogging_Command extends AbstractCommand implements Command
{
	
/**
 * Values for StartLogEnabled.
 */
protected final String __FALSE = "False";
protected final String __TRUE = "True";

/**
Constructor.
*/
public ConfigureLogging_Command ()
{	super();
	setCommandName ( "ConfigureLogging" );
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
	String StartLogEnabled = parameters.getValue ( "StartLogEnabled" );
	String warning = "";
	String message;
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (StartLogEnabled != null) && !StartLogEnabled.equalsIgnoreCase(__TRUE) && !StartLogEnabled.equalsIgnoreCase(__FALSE) ) {
        message = "The value of the StartLogEnabled parameter must be " + __TRUE + " (default) or " + __FALSE + ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as " + __TRUE + " (default if not specified) or " + __FALSE + "." ) );
    }

	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(1);
	validList.add ( "StartLogEnabled" );
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
	return (new ConfigureLogging_JDialog ( parent, this )).ok();
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
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3; // Level for non-user messages for log file.
	
	PropList parameters = getCommandParameters();
	
	CommandStatus status = getCommandStatus();
	CommandProcessor processor = getCommandProcessor();
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
		status.clearLog(CommandPhaseType.RUN);
	}
	
	String StartLogEnabled = parameters.getValue ( "StartLogEnabled" );

	try {
	    // Set the processor property (rather than, for example, static setting in StartLog command class)
		// so that the scope of the configuration is on the processor and not global
    	// Set the property in the processor
	    
		if ( (StartLogEnabled != null) && !StartLogEnabled.isEmpty() ) {
	        if ( StartLogEnabled.equalsIgnoreCase("true") ) {
	        	Message.printStatus(2,routine,"StartLogEnabled=" + StartLogEnabled + " - logging will be split according to StartLog commands.");
	        }
	        else if ( StartLogEnabled.equalsIgnoreCase("false") ) {
	        	Message.printStatus(2,routine,"StartLogEnabled=" + StartLogEnabled + " - logging will always go to main log file \"" + Message.getLogFile() + "\".");
	        }
	        try {
	        	processor.setPropContents("StartLogEnabled",new Boolean(StartLogEnabled));
	    	}
	    	catch ( Exception e ) {
	    		message = "Error setting StartLogEnabled property in processor (" + e + ").";
	    		Message.printWarning(log_level,
	    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
	    				routine, message );
	            Message.printWarning(log_level,routine,e);
	            status.addToLog ( CommandPhaseType.RUN,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Report the problem to software support." ) );
	    	}
		}
	}
	catch ( Exception e ) {
		message = "Unexpected error setting logging configuration.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Check the log file or command window for details." ) );
		throw new CommandException ( message );
	}
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

// OK to use parent toString()

}