// SetAveragePeriod_Command - This class initializes, checks, and runs the SetAveragePeriod() command.

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
This class initializes, checks, and runs the SetAveragePeriod() command.
*/
public class SetAveragePeriod_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public SetAveragePeriod_Command ()
{	super();
	setCommandName ( "SetAveragePeriod" );
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
{	String AverageStart = parameters.getValue ( "AverageStart" );
	String AverageEnd = parameters.getValue ( "AverageEnd" );
	String warning = "";
	String message;
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	// When checking AverageStart and AverageEnd, all we care about is that the
	// syntax is correct.  In runCommand() the parameter will be reparsed with runtime data...

	PropList dateprops = new PropList ( "SetAveragePeriod" );
	// The instance is not needed when checking syntax but will be checked at runtime.
	DateTime now = new DateTime(DateTime.DATE_CURRENT);
	dateprops.set ( new Prop ("AverageStart", now, now.toString()) );
	DateTime AverageStart_DateTime = null;
	DateTime AverageEnd_DateTime = null;
	if ( (AverageStart != null) && !AverageStart.equals("") ) {
		try {
		    // This handles special syntax like "NowToHour" and "NowToHour - 6Hour"
			AverageStart_DateTime = DateTime.parse(AverageStart, dateprops );
		}
		catch ( Exception e ) {
			message = "The average start date/time \"" + AverageStart +	"\" is not a valid date/time string.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid date/time for the average start." ) );
			Message.printWarning ( 3, "", e );
		}
	}
	if ( (AverageEnd != null) && !AverageEnd.equals("") ) {
		try {
		    // This handles special syntax like "NowToHour" and "NowToHour - 6Hour"
			AverageEnd_DateTime = DateTime.parse(AverageEnd, dateprops );
		}
		catch ( Exception e ) {
			message = "The average end date/time \"" + AverageEnd +	"\" is not a valid date/time string.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid date/time for the average end." ) );
			Message.printWarning ( 3, "", e );
		}
	}
	if ( (AverageStart_DateTime != null) && (AverageStart_DateTime != null) ) {
		if ( AverageStart_DateTime.greaterThan(AverageEnd_DateTime) ) {
			message = "The start date/time is later than the end date/time.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Correct so that the end time is >= the start time." ) );
		}
		if ( AverageStart_DateTime.getPrecision() != AverageEnd_DateTime.getPrecision() ) {
			message = "The precision of the start and end date/times are different.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Correct so that the date/times are specified to the same precision." ) );
		}
	}
	
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>();
	validList.add ( "AverageStart" );
	validList.add ( "AverageEnd" );
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
	return (new SetAveragePeriod_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	String AverageStart = null;
	String AverageEnd = null;
	if ( (command.indexOf('=') > 0) || command.endsWith("()") ) {
		// Current syntax...
	    super.parseCommand(command);
	}
	else {
		// TODO SAM 2005-04-29 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		List<String> tokens = StringUtil.breakStringList ( command,"(,)", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() != 3) ) {
			throw new InvalidCommandSyntaxException ("Bad command \"" + command + "\"" );
		}
		AverageStart = ((String)tokens.get(1)).trim();
		if ( AverageStart.equals("*") ) {	// Phase out old style
			AverageStart = "";
		}
		AverageEnd = ((String)tokens.get(2)).trim();
		if ( AverageEnd.equals("*") ) {	// Phase out old style
			AverageEnd = "";
		}

		// Set parameters and new defaults...

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( AverageStart.length() > 0 ) {
			parameters.set ( "AverageStart", AverageStart );
		}
		if ( AverageEnd.length() > 0 ) {
			parameters.set ( "AverageEnd", AverageEnd );
		}
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws CommandWarningException, CommandException
{	String routine = "SetAveragePeriod_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	
	String AverageStart = parameters.getValue ( "AverageStart" );
	String AverageEnd = parameters.getValue ( "AverageEnd" );
	DateTime AverageStart_DateTime = null;
	DateTime AverageEnd_DateTime = null;
	PropList dateprops = new PropList ( "SetAveragePeriod" );
	try {
	    // Reparse the date/times to take advantage of run-time data values...
		if ( (AverageStart != null) && !AverageStart.equals("") ) {
			try {
			    // This handles special syntax like "NowToHour" and "NowToHour - 6Hour"
				AverageStart_DateTime = DateTime.parse(AverageStart, dateprops );
			}
			catch ( Exception e ) {
				message = "The average start date/time \"" + AverageStart + "\" is not a valid date/time.";
				Message.printWarning ( warning_level, 
				MessageUtil.formatMessageTag(command_tag,
				++warning_count), routine, message );
				status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Specify a valid date/time for the average start (use command editor)." ) );
				Message.printWarning ( 3, routine, e );
			}
		}
		if ( AverageStart_DateTime != null ) {
			// Set the value and contents...
			Prop prop = new Prop ( "AverageStart",AverageStart_DateTime,AverageStart_DateTime.toString() );
			dateprops.set ( prop );
		}
		if ( (AverageEnd != null) && !AverageEnd.equals("") ) {
			try {
			    AverageEnd_DateTime = DateTime.parse(AverageEnd, dateprops );
			}
			catch ( Exception e ) {
				message = "The average end date/time \"" + AverageEnd +	"\" is not a valid date/time.";
				Message.printWarning ( warning_level, 
				MessageUtil.formatMessageTag(command_tag,
				++warning_count), routine, message );
				Message.printWarning ( 3, routine, e );
				status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Specify a valid date/time for the avergage end (use command editor)." ) );
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
		processor.setPropContents ( "AverageStart", AverageStart_DateTime);
		processor.setPropContents ( "AverageEnd", AverageEnd_DateTime );
	}
	catch ( Exception e ) {
		message = "Unexpected error setting average period in processor (" + e + ").";
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
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"AverageStart",
		"AverageEnd"
	};
	return this.toString(parameters, parameterOrder);
}

}