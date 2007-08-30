//------------------------------------------------------------------------------
// setInputPeriod_Command - handle the setInputPeriod() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-12-14	Steven A. Malers, RTi	Initial version.  Copy and modify
//					sortTimeSeries().
// 2007-02-11	SAM, RTi				Check consistency with new
//					TSCommandProcessor definition.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import java.util.Vector;

import javax.swing.JFrame;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.SkeletonCommand;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
<p>
This class initializes, checks, and runs the setInputPeriod() command.
</p>
<p>The CommandProcessor must return the following properties:
InputStart, InputEnd.
</p>
*/
public class setInputPeriod_Command extends SkeletonCommand implements Command
{

/**
Constructor.
*/
public setInputPeriod_Command ()
{	super();
	setCommandName ( "setInputPeriod" );
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
{	String InputStart = parameters.getValue ( "InputStart" );
	String InputEnd = parameters.getValue ( "InputEnd" );
	String warning = "";

	// When checking InputStart and InputEnd, all we care about is that the
	// syntax is correct.  In runCommand() the parameter will be reparsed
	// with runtime data...

	PropList dateprops = new PropList ( "setInputPeriod" );
	// The instance is not needed when checking syntax but will be checked
	// at runtime.
	DateTime now = new DateTime(DateTime.DATE_CURRENT);
	dateprops.set ( new Prop ("InputStart", now, now.toString()) );
	DateTime InputStart_DateTime = null;
	DateTime InputEnd_DateTime = null;
	if ( (InputStart != null) && !InputStart.equals("") ) {
		try {	// This handles special syntax like "NowToHour" and
			// "NowToHour - 6Hour"
			DateTime.parse(InputStart, dateprops );
		}
		catch ( Exception e ) {
			warning += 
				"\nThe input start date/time \"" +
				InputStart +
				"\" is not a valid date/time string.\n"+
				"Specify a date/time or recognized values.";
			Message.printWarning ( 3, "", e );
		}
	}
	if ( (InputEnd != null) && !InputEnd.equals("") ) {
		try {	// This handles special syntax like "NowToHour" and
			// "NowToHour - 6Hour"
			DateTime.parse(InputEnd, dateprops );
		}
		catch ( Exception e ) {
			warning += 
				"\nThe input end date/time \"" +
				InputEnd +
				"\" is not a valid date/time string.\n"+
				"Specify a date/time or recognized values.";
			Message.printWarning ( 3, "", e );
		}
	}
	if ( (InputStart_DateTime != null) && (InputStart_DateTime != null) ) {
		if ( InputStart_DateTime.greaterThan(InputEnd_DateTime) ) {
			warning +=
			"\nThe start date/time is later than " +
			"the end date/time.";
		}
		if (	InputStart_DateTime.getPrecision() !=
			InputEnd_DateTime.getPrecision() ) {
			warning +=
			"\nThe precision of the start and end are different.";
		}
	}
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),
		warning );
		throw new InvalidCommandParameterException ( warning );
	}
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new setInputPeriod_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand (	String command, String command_tag,
				int warning_level )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	String routine = "setInputPeriod_Command.parseCommand", message;

	String InputStart = null;
	String InputEnd = null;
	if ( command.indexOf('=') < 0 ) {
		// REVISIT SAM 2005-04-29 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new
		// syntax.
		//
		// Old syntax where the only parameter is a single TSID or *
		// to fill all.
		Vector tokens = StringUtil.breakStringList ( command,
				"(,)", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || (tokens.size() != 3) ) {
			throw new InvalidCommandSyntaxException (
			"Bad command \"" + command + "\"" );
		}
		if ( StringUtil.startsWithIgnoreCase(command,"setQueryPeriod")){
			Message.printStatus ( 3, routine,
			"Automatically converting setQueryPeriod() to " +
			"setInputPeriod()" );
		}
		InputStart = ((String)tokens.elementAt(1)).trim();
		if ( InputStart.equals("*") ) {	// Phase out old style
			InputStart = "";
		}
		InputEnd = ((String)tokens.elementAt(2)).trim();
		if ( InputEnd.equals("*") ) {	// Phase out old style
			InputEnd = "";
		}

		// Set parameters and new defaults...

		_parameters = new PropList ( getCommandName() );
		_parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( InputStart.length() > 0 ) {
			_parameters.set ( "InputStart", InputStart );
		}
		if ( InputEnd.length() > 0 ) {
			_parameters.set ( "InputEnd", InputEnd );
		}
		_parameters.setHowSet ( Prop.SET_UNKNOWN );
	}
	else {	// Current syntax...
		Vector tokens = StringUtil.breakStringList ( command,
			"()", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || tokens.size() < 2 ) {
			message = "Invalid syntax for \"" + command +
				"\".  Not enough tokens.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
		// Get the input needed to process the file...
		_parameters = PropList.parse ( Prop.SET_FROM_PERSISTENT,
			(String)tokens.elementAt(1), routine,"," );
	}
}

/**
Run the commands:
<pre>
setInputPeriod(InputStart="X",InputEnd="X")
</pre>
@param processor The CommandProcessor that is executing the command, which will
provide necessary data inputs and receive output(s).
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( String command_tag, int warning_level )
throws CommandWarningException, CommandException
{	String routine = "setInputPeriod_Command.runCommand", message;
	int warning_count = 0;
	String InputStart = _parameters.getValue ( "InputStart" );
	String InputEnd = _parameters.getValue ( "InputEnd" );
	DateTime InputStart_DateTime = null;
	DateTime InputEnd_DateTime = null;
	PropList dateprops = new PropList ( "setInputPeriod" );
	try {	// Reparse the date/times to take advantage of run-time data
		// values...
		if ( (InputStart != null) && !InputStart.equals("") ) {
			try {	// This handles special syntax like "NowToHour"
				// and "NowToHour - 6Hour"
				InputStart_DateTime =
				DateTime.parse(InputStart, dateprops );
			}
			catch ( Exception e ) {
				message =
					"\nThe input start date/time \"" +
					InputStart +
					"\" is not a valid date/time.\n"+
					"Specify a date/time or recognized " +
					"values.";
				Message.printWarning ( warning_level, 
				MessageUtil.formatMessageTag(command_tag,
				++warning_count), routine, message );
				Message.printWarning ( 3, routine, e );
			}
		}
		if ( InputStart_DateTime != null ) {
			// Set the value and contents...
			Prop prop = new Prop ( "InputStart",
				InputStart_DateTime,
				InputStart_DateTime.toString() );
			dateprops.set ( prop );
		}
		if ( (InputEnd != null) && !InputEnd.equals("") ) {
			try {	InputEnd_DateTime =
				DateTime.parse(InputEnd, dateprops );
			}
			catch ( Exception e ) {
				message =
					"\nThe input end date/time \"" +
					InputEnd +
					"\" is not a valid date/time.\n"+
					"Specify a date/time or recognized " +
					"values.";
				Message.printWarning ( warning_level, 
				MessageUtil.formatMessageTag(command_tag,
				++warning_count), routine, message );
				Message.printWarning ( 3, routine, e );
			}
		}
		if ( warning_count > 0 ) {
			// Input error...
			message = "Cannot process command parameters.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
			throw new InvalidCommandParameterException ( message );
		}
		_processor.setPropContents ( "InputStart", InputStart_DateTime);
		_processor.setPropContents ( "InputEnd", InputEnd_DateTime );
	}
	catch ( Exception e ) {
		message = "Error setting input period.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		Message.printWarning ( 3, routine, e );
		throw new CommandException ( message );
	}
}

// Can rely on base class for toString().

}
