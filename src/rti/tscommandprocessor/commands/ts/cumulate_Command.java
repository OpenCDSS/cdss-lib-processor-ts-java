//------------------------------------------------------------------------------
// cumulate_Command - handle the cumulate() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-09-29	Steven A. Malers, RTi	Initial version.  Copy and modify
//					scale().
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSUtil;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;

/**
<p>
This class initializes, checks, and runs the cumulate() command.
</p>
<p>The CommandProcessor must return the following properties:  TSResultsList.
</p>
*/
public class cumulate_Command extends AbstractCommand
implements Command
{

/**
Strings used with the command.
*/
protected final String _CarryForwardIfMissing = "CarryForwardIfMissing";
protected final String _SetMissingIfMissing = "SetMissingIfMissing";

/**
Constructor.
*/
public cumulate_Command ()
{	super();
	setCommandName ( "cumulate" );
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
{	String TSID = parameters.getValue ( "TSID" );
	String HandleMissingHow = parameters.getValue ( "HandleMissingHow" );
	String warning = "";

	if (	(TSID == null) || TSID.equals("") ) {
		warning +=
			"\nThe time series identifier must be specified.";
	}
	if (	(HandleMissingHow != null) &&
		!HandleMissingHow.equals("") &&
		!HandleMissingHow.equalsIgnoreCase(_CarryForwardIfMissing) &&
		!HandleMissingHow.equalsIgnoreCase(_SetMissingIfMissing) ) {
		warning +=
			"\nHandleMissingHow, if specified, must be " +
			_CarryForwardIfMissing + " or " +
			_SetMissingIfMissing;
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
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new cumulate_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "cumulate_Command.parseCommand", message;

	if ( command.indexOf('=') < 0 ) {
		// REVISIT SAM 2005-08-24 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new
		// syntax.
		//
		// Old syntax without named parameters.
		Vector v = StringUtil.breakStringList ( command,"(),",
			StringUtil.DELIM_SKIP_BLANKS );
		String TSID = "";
		String HandleMissingHow = "";
		if ( (v != null) && (v.size() == 3) ) {
			// Second field is identifier...
			TSID = ((String)v.elementAt(1)).trim();
			// Third field has missing data type...
			HandleMissingHow = ((String)v.elementAt(2)).trim();
		}

		// Set parameters and new defaults...

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID.length() > 0 ) {
			parameters.set ( "TSID", TSID );
		}
		if ( HandleMissingHow.length() > 0 ) {
			parameters.set ( "HandleMissingHow", HandleMissingHow);
		}
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}

	else {	// Current syntax...
		Vector tokens = StringUtil.breakStringList ( command,
			"()", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || tokens.size() < 2 ) {
			// Must have at least the command name, TSID, and
			// IndependentTSID...
			message = "Syntax error in \"" + command +
				"\".  Not enough tokens.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
		// Get the input needed to process the file...
		try {	setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
				(String)tokens.elementAt(1), routine, "," ) );
		}
		catch ( Exception e ) {
			message = "Syntax error in \"" + command +
				"\".  Not enough tokens.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
	}
}

/**
Run the commands:
<pre>
cumulate(TSID="X",HandleMissingHow=X)
</pre>
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "cumulate_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;	// Warning level for non-user messages.

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String TSList = "AllMatchingTSID";
	String TSID = parameters.getValue ( "TSID" );
	String HandleMissingHow = parameters.getValue ( "HandleMissingHow" );

	// Get the time series to process.  Allow TSID to be a pattern or
	// specific time series...

	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try { bean =
		processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\" from processor.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	Vector tslist = null;
	if ( o_TSList == null ) {
		message = "Unable to find time series to cumulate using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}
	else {	tslist = (Vector)o_TSList;
		if ( tslist.size() == 0 ) {
			message = "Unable to find time series to cumulate using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
			Message.printWarning ( warning_level,
					MessageUtil.formatMessageTag(
							command_tag,++warning_count), routine, message );
		}
	}
	Object o_Indices = bean_PropList.getContents ( "Indices" );
	int [] tspos = null;
	if ( o_Indices == null ) {
		message = "Unable to find indices for time series to cumulate using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}
	else {	tspos = (int [])o_Indices;
		if ( tspos.length == 0 ) {
			message = "Unable to find indices for time series to cumulate using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
			Message.printWarning ( warning_level,
					MessageUtil.formatMessageTag(
							command_tag,++warning_count), routine, message );
		}
	}
	
	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Insufficient data to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		// It is OK if no time series.
	}

	// Now process the time series...

	int nts = 0;
	if ( tslist != null ) {
		nts = tslist.size();
	}
	PropList cumulate_props = new PropList ( "cumulate" );
	if ( HandleMissingHow != null ) {
		cumulate_props.add ( "HandleMissingHow=" + HandleMissingHow );
	}
	TS ts = null;
	for ( int its = 0; its < nts; its++ ) {
		request_params = new PropList ( "" );
		request_params.setUsingObject ( "Index", new Integer(tspos[its]) );
		bean = null;
		try { bean =
			processor.processRequest( "GetTimeSeries", request_params);
		}
		catch ( Exception e ) {
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, "Error requesting GetTimeSeries(Index=" + tspos[its] +
					"\" from processor." );
		}
		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "TS" );
		if ( prop_contents == null ) {
			Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, "Null value for GetTimeSeries(Index=" + tspos[its] +
				"\") returned from processor." );
		}
		else {	ts = (TS)prop_contents;
		}
		
		// TODO SAM 2007-02-17 Evaluate whether to print warning if null TS
		
		try {	// Do the processing...
			Message.printStatus ( 2, routine, "Cumulating \"" +
				ts.getIdentifier() + "\"." );
			TSUtil.cumulate ( ts, null, null, cumulate_props );
		}
		catch ( Exception e ) {
			message = "Unable to cumulate time series \""+
				ts.getIdentifier() + "\".";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count),routine,message );
			Message.printWarning(3,routine,e);
		}
	}

	// Resave the data to the processor so that appropriate actions are
	// taken...
	// REVISIT SAM 2005-08-25
	// Is this needed?
	//_processor.setPropContents ( "TSResultsList", TSResultsList );

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
	String TSID = props.getValue( "TSID" );
	String HandleMissingHow = props.getValue("HandleMissingHow");
	String Reset = props.getValue("Reset");
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (HandleMissingHow != null) && (HandleMissingHow.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "HandleMissingHow=" + HandleMissingHow );
	}
	if ( (Reset != null) && (Reset.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Reset=\"" + Reset + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
