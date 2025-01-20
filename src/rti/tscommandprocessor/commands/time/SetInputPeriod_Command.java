// SetInputPeriod_Command - This class initializes, checks, and runs the SetInputPeriod() command.

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

package rti.tscommandprocessor.commands.time;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandDiscoverable;
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
The command is discoverable because need to postpone expansion of run-time properties.
*/
public class SetInputPeriod_Command extends AbstractCommand implements CommandDiscoverable
{

/**
Constructor.
*/
public SetInputPeriod_Command () {
	super();
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
throws InvalidCommandParameterException {
	String InputStart = parameters.getValue ( "InputStart" );
	String InputEnd = parameters.getValue ( "InputEnd" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	// When checking InputStart and InputEnd, all we care about is that the syntax is correct.
	// In runCommand() the parameter will be reparsed with runtime data.

	PropList dateprops = new PropList ( "SetInputPeriod" );
	// The instance is not needed when checking syntax but will be checked at runtime.
	DateTime now = new DateTime(DateTime.DATE_CURRENT);
	dateprops.set ( new Prop ("InputStart", now, now.toString()) );
	DateTime InputStart_DateTime = null;
	DateTime InputEnd_DateTime = null;
	if ( (InputStart != null) && !InputStart.isEmpty() && !InputStart.contains("${") ) {
		try {
		    // This handles special syntax like "CurrentToHour" and "CurrentToHour - 6Hour".
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
	if ( (InputEnd != null) && !InputEnd.isEmpty() && !InputEnd.contains("${")) {
		try {
		    // This handles special syntax like "CurrentToHour" and "CurrentToHour - 6Hour".
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

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(2);
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new SetInputPeriod_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
	String routine = getClass().getSimpleName() + ".parseCommand";

	String InputStart = null;
	String InputEnd = null;
	if ( (command.indexOf('=') > 0) || command.endsWith("()") ) {
		// Current syntax.
	    super.parseCommand(command);
	}
	else {
		// TODO SAM 2005-04-29 This whole block of code needs to be removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax where the only parameter is a single TSID or * to fill all.
		List<String> tokens = StringUtil.breakStringList ( command,"(,)", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() != 3) ) {
			throw new InvalidCommandSyntaxException ("Bad command \"" + command + "\"" );
		}
		if ( StringUtil.startsWithIgnoreCase(command,"setQueryPeriod")){
			Message.printStatus ( 3, routine,
			"Automatically converting setQueryPeriod() to SetInputPeriod()" );
		}
		InputStart = ((String)tokens.get(1)).trim();
		if ( InputStart.equals("*") ) {
		    // Phase out old style.
			InputStart = "";
		}
		InputEnd = ((String)tokens.get(2)).trim();
		if ( InputEnd.equals("*") ) {
		    // Phase out old style.
			InputEnd = "";
		}

		// Set parameters and new defaults.

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
@param commandNumber Command number (1+) in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int commandNumber )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( commandNumber, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param commandNumber Command number (1+) in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int commandNumber )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( commandNumber, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param commandNumber number of command (1+) in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int commandNumber, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + commandNumber;

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);

	String InputStart = parameters.getValue ( "InputStart" );
	if ( (commandPhase == CommandPhaseType.RUN) &&
		(InputStart != null) && !InputStart.isEmpty() && InputStart.startsWith("Current") ) {
		// Expand the string:
		// - this allows something like:  CurrentToDay = ${SomeProperty}
		// - doing it always could e an issue if the first part expands to a date with dashes and the dashes confuse the offset
    	InputStart = TSCommandProcessorUtil.expandParameterValue(processor, this, InputStart);
	}
	String InputEnd = parameters.getValue ( "InputEnd" );
	if ( (commandPhase == CommandPhaseType.RUN) &&
		(InputEnd != null) && !InputEnd.isEmpty() && InputEnd.startsWith("Current") ) {
    	InputEnd = TSCommandProcessorUtil.expandParameterValue(processor, this, InputEnd);
	}

    if ( warning_count > 0 ) {
        // Input error.
        message = "Insufficient data to run the command.";
        status.addToLog ( commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to the command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

	// Now process the input period:
    // - can't determine errors until the input is parsed

    try {
        if ( commandPhase == CommandPhaseType.RUN ) {
        	DateTime InputStart_DateTime = null;
	       	DateTime InputEnd_DateTime = null;
	       	PropList dateprops = new PropList ( "SetInputPeriod" );
		    // If the parameters start with ${ get from the utility code. // closing }
		    if ( (InputStart != null) && !InputStart.isEmpty() ) {
			   	if ( InputStart.startsWith("${") ) { // closing }
			       	// Otherwise parse the date/times to take advantage of run-time data values.
			       	try {
				       	InputStart_DateTime = TSCommandProcessorUtil.getDateTime ( InputStart, "InputStart", processor,
					       	status, warning_level, command_tag );
			       	}
			       	catch ( InvalidCommandParameterException e ) {
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
			   	else {
			       	// Otherwise parse the date/times to take advantage of run-time data values.
			       	try {
			           	// This handles special syntax like "CurrentToHour" and "CurrentToHour - 6Hour".
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
			      	// Set the value and contents, which may be used when setting the InputEnd below.
			       	Prop prop = new Prop ( "InputStart",InputStart_DateTime,InputStart_DateTime.toString() );
			       	dateprops.set ( prop );
			   	}
		   	}
		   	if ( (InputEnd != null) && !InputEnd.isEmpty() ) {
			   	if ( InputEnd.startsWith("${") ) { // Closing }.
			       	// Otherwise parse the date/times to take advantage of run-time data values.
			      	try {
				       	InputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( InputEnd, "InputEnd", processor,
					       	status, warning_level, command_tag );
			       	}
			       	catch ( InvalidCommandParameterException e ) {
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

	      	// Set the period in the processor.
	      	if ( InputStart_DateTime != null ) {
	      		processor.setPropContents ( "InputStart", InputStart_DateTime);
           		Message.printStatus(2, routine, "Input period start set to:  " + InputStart_DateTime );
	      	}
	      	if ( InputEnd_DateTime != null ) {
	      		processor.setPropContents ( "InputEnd", InputEnd_DateTime );
           		Message.printStatus(2, routine, "Input period end set to:  " + InputEnd_DateTime );
	      	}
        }
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
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"InputStart",
		"InputEnd"
	};
	return this.toString(parameters, parameterOrder);
}

}