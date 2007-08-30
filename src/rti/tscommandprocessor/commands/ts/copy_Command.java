//------------------------------------------------------------------------------
// copy_Command - handle the TS Alias = copy() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-08-25	Steven A. Malers, RTi	Initial version.  Copy and modify
//					scale().
// 2005-08-29	SAM, RTi		Finish enabling runCommand().
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSIdent;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.SkeletonCommand;
import RTi.Util.String.StringUtil;

/**
<p>
This class initializes, checks, and runs the copy() command.
</p>
<p>The CommandProcessor must return the following properties:  TSResultsList.
</p>
*/
public class copy_Command extends SkeletonCommand implements Command
{

/**
Constructor.
*/
public copy_Command ()
{	super();
	setCommandName ( "copy" );
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
{	String Alias = parameters.getValue ( "Alias" );
	String TSID = parameters.getValue ( "TSID" );
	String warning = "";

	if ( (Alias == null) || Alias.equals("") ) {
		warning += "\nThe time series alias must be specified.";
	}
	if ( (TSID == null) || TSID.equals("") ) {
		warning += "\nThe time series identifier must be specified.";
	}
	// REVISIT SAM 2005-08-29
	// Need to decide whether to check NewTSID - it might need to support
	// wildcards.
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
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new copy_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
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
{	int warning_count = 0;
	String routine = "copy_Command.parseCommand", message;

	// Get the part of the command after the TS Alias =...
	int pos = command.indexOf ( "=" );
	if ( pos < 0 ) {
		message = "Syntax error in \"" + command +
			"\".  Expecting:  TS Alias = copy(...)";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	String token0 = command.substring ( 0, pos ).trim();
	String token1 = command.substring ( pos + 1 ).trim();
	if ( (token0 == null) || (token1 == null) ) {
		message = "Syntax error in \"" + command +
			"\".  Expecting:  TS Alias = copy(...)";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	if ( token1.indexOf('=') < 0 ) {
		// No parameters have = in them...
		// REVISIT SAM 2005-08-25 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new
		// syntax.
		//
		// Old syntax without named parameters.
		Vector v = StringUtil.breakStringList ( token0," ",
			StringUtil.DELIM_SKIP_BLANKS );
		if ( (v == null) || (v.size() != 2) ) {
			message = "Syntax error in \"" + command +
				"\".  Expecting:  TS Alias = copy(TSID)";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count), routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
		String Alias = (String)v.elementAt(1);
		v = StringUtil.breakStringList ( token1,"(),",
			StringUtil.DELIM_SKIP_BLANKS );
		if ( (v == null) || (v.size() != 2) ) {
			message = "Syntax error in \"" + command +
				"\".  Expecting:  TS Alias = copy(TSID)";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count), routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
		String TSID = (String)v.elementAt(1);

		// Set parameters and new defaults...

		_parameters = new PropList ( getCommandName() );
		_parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( Alias.length() > 0 ) {
			_parameters.set ( "Alias", Alias );
		}
		if ( TSID.length() > 0 ) {
			_parameters.set ( "TSID", TSID );
		}
		_parameters.setHowSet ( Prop.SET_UNKNOWN );
	}

	else {	// Current syntax...
		Vector v = StringUtil.breakStringList ( token0, " ",
			StringUtil.DELIM_SKIP_BLANKS );
		if ( (v == null) || (v.size() != 2) ) {
			message = "Syntax error in \"" + command +
				"\".  Expecting:  TS Alias = copy(...)";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count), routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
		String Alias = (String)v.elementAt(1);
		Vector tokens = StringUtil.breakStringList ( token1,
			"()", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || tokens.size() < 2 ) {
			// Must have at least the command name and its
			// parameters...
			message = "Syntax error in \"" + command +
				"\".  Not enough tokens.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
		// Get the input needed to process the file...
		try {	_parameters = PropList.parse ( Prop.SET_FROM_PERSISTENT,
				(String)tokens.elementAt(1), routine, "," );
			_parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
			_parameters.set ( "Alias", Alias );
			_parameters.setHowSet ( Prop.SET_UNKNOWN );
		}
		catch ( Exception e ) {
			message = "Syntax error in \"" + command +
				"\".  Not enough tokens.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
	}
}

/**
Run the commands:
<pre>
TS Alias = copy(TSID="X",NewTSID="X")
</pre>
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( String command_tag, int warning_level )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "copy_Command.runCommand", message;
	int warning_count = 0;
	int log_level = 3;	// Level for non-user messages

	// Make sure there are time series available to operate on...

	String Alias = _parameters.getValue ( "Alias" );
	String TSID = _parameters.getValue ( "TSID" );
	String NewTSID = _parameters.getValue ( "NewTSID" );

	// Get the time series to process.  The time series list is searched
	// backwards until the first match...

	TS ts = null;
	try {	PropList request_params = new PropList ( "" );
			request_params.set ( "CommandTag", command_tag );
			request_params.set ( "TSID", TSID );
			CommandProcessorRequestResultsBean bean = null;
			try { bean =
				_processor.processRequest( "GetTimeSeriesForTSID", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSID +
				"\" from processor.";
				Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
				Message.printWarning(log_level, routine, e );
			}
			PropList bean_PropList = bean.getResultsPropList();
			Object o_TS = bean_PropList.getContents ( "TS");
			if ( o_TS == null ) {
				message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID +
				"\" from processor.";
				Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			}
			else {
				ts = (TS)o_TS;
			}
	}
	catch ( Exception e ) {
		ts = null;
	}
	if ( ts == null ) {
		message = "Unable to find time series to copy using TSID \"" +
		TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		throw new CommandWarningException ( message );
	}

	// Now process the time series...

	TS tscopy = null;
	try {	tscopy = (TS)ts.clone();
		tscopy.setAlias ( Alias );	// Do separate because setting
						// the NewTSID might cause the
						// alias set to fail below.
	}
	catch ( Exception e ) {
		message = "Unable to copy time series \""+
			ts.getIdentifier() + "\".";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
	}

	try {	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
			TSIdent tsident = new TSIdent ( NewTSID );
			tscopy.setIdentifier ( tsident );
		}
		tscopy.setAlias ( Alias );
	}
	catch ( Exception e ) {
		message = "Unable to set new time series identifier \""+
			NewTSID + "\".";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
	}

	// Updated the data to the processor so that appropriate actions are
	// taken...
	
	Vector TSResultsList_Vector = null;
	try { Object o = _processor.getPropContents( "TSResultsList" );
			TSResultsList_Vector = (Vector)o;
	}
	catch ( Exception e ){
		message = "Cannot get time series list to add copied time series.  Skipping.";
		Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag, ++warning_count),
				routine,message);
	}
	if ( TSResultsList_Vector != null ) {
		TSResultsList_Vector.addElement ( tscopy );
		try {	_processor.setPropContents ( "TSResultsList", TSResultsList_Vector );
		}
		catch ( Exception e ){
			message = "Cannot set updated time series list.  Copy will not be in results.";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag, ++warning_count),
				routine,message);
		}
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
		throw new CommandWarningException ( message );
	}
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String Alias = props.getValue( "Alias" );
	String TSID = props.getValue( "TSID" );
	String NewTSID = props.getValue( "NewTSID" );
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewTSID=\"" + NewTSID + "\"" );
	}
	return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
}

}
