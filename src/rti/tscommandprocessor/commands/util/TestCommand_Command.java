// TestCommand_Command - This class initializes, checks, and runs the TestCommand() command.

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
<p>
This class initializes, checks, and runs the TestCommand() command.
*/
public class TestCommand_Command extends AbstractCommand
implements Command
{

/**
Constructor.
*/
public TestCommand_Command ()
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
	List<String> validList = new ArrayList<String>();
	validList.add ( "InitializeStatus" );
	validList.add ( "DiscoveryStatus" );
	validList.add ( "RunStatus" );
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
	return (new TestCommand_JDialog ( parent, this )).ok();
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
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"InitializeStatus",
		"DiscoveryStatus",
		"RunStatus"
	};
	return this.toString(parameters, parameterOrder);
}

}