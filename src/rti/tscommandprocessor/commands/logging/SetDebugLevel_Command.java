// SetDebugLevel_Command - This class initializes, checks, and runs the SetDebugLevel() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the SetDebugLevel() command.
*/
public class SetDebugLevel_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public SetDebugLevel_Command ()
{	super();
	setCommandName ( "SetDebugLevel" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	String routine = getCommandName() + "_checkCommandParameters";
	String ScreenLevel = parameters.getValue ( "ScreenLevel" );
	String LogFileLevel = parameters.getValue ( "LogFileLevel" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (ScreenLevel != null) && (ScreenLevel.length() != 0) &&
            !StringUtil.isInteger(ScreenLevel) ) {
        message = "The screen warning level \"" + ScreenLevel + "\" is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Change the screen level to an integer." ) );
    }
    if ( (LogFileLevel != null) && (LogFileLevel.length() != 0) &&
            !StringUtil.isInteger(LogFileLevel) ) {
        message = "The log file warning level \"" + LogFileLevel + "\" is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Change the log file level to an integer." ) );
    }

	// Check for invalid parameters.
    List<String> validList = new ArrayList<>(2);
	validList.add ( "ScreenLevel" );
	validList.add ( "LogFileLevel" );
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
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new SetDebugLevel_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
This method currently supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax.
        super.parseCommand( command_string);
    }
    else {
        // TODO smalers 2008-06-23 This whole block of code needs to be
        // removed as soon as commands have been migrated to the new syntax.
    	List<String> v = StringUtil.breakStringList(command_string, "(),", StringUtil.DELIM_ALLOW_STRINGS );
        int ntokens = 0;
        if ( v != null ) {
            ntokens = v.size();
        }
        String ScreenLevel = "";
        String LogFileLevel = "";
        if ( ntokens >= 2 ) {
            // Command name and screen level.
            ScreenLevel = ((String)v.get(1)).trim();
        }
        if ( ntokens == 3 ) {
            // Command name and screen level.
            LogFileLevel = ((String)v.get(2)).trim();
        }

        // Set parameters and new defaults.

        PropList parameters = new PropList ( getCommandName() );
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        if ( ScreenLevel.length() > 0 ) {
            parameters.set ( "ScreenLevel", ScreenLevel );
        }
        if ( LogFileLevel.length() > 0 ) {
            parameters.set ( "LogFileLevel", LogFileLevel );
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
throws CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
	CommandStatus status = getCommandStatus();
    Boolean clearStatus = Boolean.TRUE; // Default.
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen.
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}

	try {
		String ScreenLevel = parameters.getValue ( "ScreenLevel" );
		String LogFileLevel = parameters.getValue ( "LogFileLevel" );
		Message.printStatus( 2, routine, "levels are " + ScreenLevel + ", " + LogFileLevel );
		int screenLevelInt = -1;
		int logLevelInt = -1;
		if ( ScreenLevel != null ) {
		    screenLevelInt = Integer.parseInt(ScreenLevel);
		    Message.setDebugLevel( Message.TERM_OUTPUT, screenLevelInt );
		}
	    if ( LogFileLevel != null ) {
	         logLevelInt = Integer.parseInt(LogFileLevel);
	         Message.setDebugLevel( Message.LOG_OUTPUT, logLevelInt );
	    }
	    if ( (screenLevelInt > 0) || (logLevelInt > 0) ) {
	        Message.setDebug ( true );
	    }
	    else if ( (screenLevelInt == 0) && (logLevelInt == 0) ) {
	        // Since both are specified as zero, turn debug off.
	        Message.setDebug ( false );
	    }
	}
	catch ( Exception e ) {
		message = "Unexpected error setting debug level.";
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
		"ScreenLevel",
    	"LogFileLevel"
	};
	return this.toString(parameters, parameterOrder);
}

}