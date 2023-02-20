// Message_Command - This class initializes, checks, and runs the Message() command.

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

package rti.tscommandprocessor.commands.logging;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.GUI.ResponseJDialog;
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
public class Message_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public Message_Command ()
{	super();
	setCommandName ( "Message" );
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
	String Message0 = parameters.getValue ( "Message" );
	String PromptActions = parameters.getValue ( "PromptActions" );
	String CommandStatus = parameters.getValue ( "CommandStatus" );
	String warning = "";
	String message;
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (Message0 == null) || Message0.isEmpty() ) {
        message = "The message text has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify message text." ) );
    }
    if ( (CommandStatus != null) && !CommandStatus.isEmpty() &&
    	!CommandStatus.equalsIgnoreCase(""+CommandStatusType.SUCCESS) &&
    	!CommandStatus.equalsIgnoreCase(""+CommandStatusType.WARNING) &&
    	!CommandStatus.equalsIgnoreCase(""+CommandStatusType.FAILURE) ) {
        message = "The command status \"" + CommandStatus + "\" is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the command status as " + 
                CommandStatusType.SUCCESS + " (default), " + CommandStatusType.WARNING +
                ", or " + CommandStatusType.FAILURE ) );
    }
    if ( (PromptActions != null) && !PromptActions.isEmpty() ) {
    	String[] parts = PromptActions.split(",");
    	for ( String part : parts ) {
    		part = part.trim();
    		if ( !part.equalsIgnoreCase("CANCEL") && !part.equalsIgnoreCase("CONTINUE") ) {
    			message = "The prompt actions \"" + PromptActions + "\" is invalid.";
    			warning += "\n" + message;
    			if ( !part.equalsIgnoreCase("Cancel") && !part.equalsIgnoreCase("Continue") ) {
    				status.addToLog ( CommandPhaseType.INITIALIZATION,
   						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify one or more prompt actions: Cancel, Continue" ));
    			}
    		}
    	}
    }

	// Check for invalid parameters...
    List<String> validList = new ArrayList<>(3);
	validList.add ( "Message" );
	validList.add ( "PromptActions" );
	validList.add ( "CommandStatus" );
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
	return (new Message_JDialog ( parent, this )).ok();
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
	
	String Message2 = parameters.getValue ( "Message" );
	String PromptActions = parameters.getValue ( "PromptActions" );
    PromptActions = TSCommandProcessorUtil.expandParameterValue(processor,this,PromptActions);
	String CommandStatus = parameters.getValue ( "CommandStatus" );
	CommandStatusType commandStatusType = null;
	if ( (CommandStatus != null) && !CommandStatus.equals("") ) {
	    commandStatusType = CommandStatusType.parse(CommandStatus);
	}

	try {
	    String messageExpanded = TSCommandProcessorUtil.expandParameterValue(processor,this,Message2);
	    Message.printStatus(2, routine, messageExpanded);
	    if ( messageExpanded != null ) {
	    	// Expand escaped newline to actual newline character.
	    	Message.printStatus(2,routine,"Replacing escaped newline with actual newline.");
	    	messageExpanded = messageExpanded.replace("\\n", "\n");
	    }
	    if ( commandStatusType != null ) {
	        // Add a command record message to trigger the status level
	        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(commandStatusType, messageExpanded, "Check the log file or command window for details." ) );
	    }
	    boolean doContinue = true;
		if ( (PromptActions != null) && !PromptActions.isEmpty() ) {
			// Prompt for confirmation:
			// - this will interrupt the workflow
			// - if the UI is not defined, always fail since no way to prompt and confirm
			if ( Message.getTopLevel() == null ) {
				message = "No UI and requesting prompt for message.";
				Message.printWarning ( warning_level,
					MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
				status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Only request prompt if running using the user interface."));
				doContinue = false;
			}
			else {
				int promptButtons = 0;
				if ( PromptActions.toUpperCase().indexOf("CANCEL") >= 0 ) {
					promptButtons = promptButtons|ResponseJDialog.CANCEL;
				}
				if ( PromptActions.toUpperCase().indexOf("CONTINUE") >= 0 ) {
					promptButtons = promptButtons|ResponseJDialog.CONTINUE;
				}
				int x = new ResponseJDialog ( Message.getTopLevel(), "Provide Response",
					messageExpanded,
					ResponseJDialog.CONTINUE|ResponseJDialog.CANCEL).response();
				if ( x == ResponseJDialog.CANCEL ) {
				    Message.printStatus(2,routine,"Canceled.  Command processing will be canceled.");
					doContinue = false;
				}
			}
		}
		if ( !doContinue ) {
			// Answered "Cancel" to prompt:
			// - indicate to processor to cancel processing
			((TSCommandProcessor)processor).setCancelProcessingRequested(true);
		}
	}
	catch ( Exception e ) {
		message = "Unexpected error printing message.";
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

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"Message",
  		"PromptActions",
  		"CommandStatus"
	};
	return this.toString(parameters, parameterOrder);
}

}