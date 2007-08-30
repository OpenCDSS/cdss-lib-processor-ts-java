//------------------------------------------------------------------------------
// fillRegression_Command - handle the fillRegression() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-04-29	Steven A. Malers, RTi	Initial version.
// 2005-05-09	SAM, RTi		* Add full_initialization flag to the
//					  initialize() method.
//					* Add toString(PropList).
// 2005-05-11	SAM, RTi		Update initialize() to not call
//					parseCommand() since the base class
//					method does it.
// 2005-05-12	SAM, RTi		Add FillFlag parameter.
// 2005-05-19	SAM, RTi		Move from TSTool package to TS.
// 2005-05-24	SAM, RTi		Add command_tag to getTimeSeries() call.
// 2005-05-31	SAM, RTi		The parameters for date/times were not
//					being processed correctly if specified.
// 2005-06-30	SAM, RTi		Fix bug where Intercept was not being
//					recognized in runCommand().
// 2006-01-24	SAM, RTi		Fix bug where intercept without a
//					transformation was getting a null
//					pointer.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-03-03	SAM, RTi		Fix bug where parse() was failing on old syntax.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSRegression;
import RTi.TS.TSUtil;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
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
This class initializes, checks, and runs the fillRegression() command.
</p>
<p>The CommandProcessor must return the following properties:  TSResultsList.
</p>
*/
public class fillRegression_Command extends SkeletonCommand implements Command
{

/**
Protected data members shared with the dialog and other related classes.
*/
protected final String _Linear = "Linear";	// obsolete... use None
protected final String _Log = "Log";
protected final String _None = "None";

protected final String _MonthlyEquations = "MonthlyEquations";
protected final String _OneEquation = "OneEquation";

/**
Constructor.
*/
public fillRegression_Command ()
{	super();
	setCommandName ( "fillRegression" );
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
	String IndependentTSID = parameters.getValue ( "IndependentTSID" );
	String NumberOfEquations = parameters.getValue ( "NumberOfEquations" );
	String AnalysisMonth = parameters.getValue ( "AnalysisMonth" );
	String Transformation = parameters.getValue ( "Transformation" );
	String Intercept = parameters.getValue ( "Intercept" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	String FillFlag = parameters.getValue ( "FillFlag" );
	String warning = "";
	if ( (TSID == null) || TSID.length() == 0 ) {
		warning =
		"\nThe dependent time series identifier must be specified.";
	}
	if ( (IndependentTSID == null) || (IndependentTSID.length() == 0) ) {
		warning +=
		"\nThe independent time series identifier must be specified.";
	}
	if (	(TSID != null) && (IndependentTSID != null) &&
		TSID.equalsIgnoreCase(IndependentTSID) ) {
		warning +=
		"\nThe time series to fill \"" + TSID + "\" is the same\n"+
		"as the independent time series \"" + IndependentTSID + "\".";
	}
	if (	(NumberOfEquations != null) &&
		!NumberOfEquations.equalsIgnoreCase(_OneEquation) &&
		!NumberOfEquations.equalsIgnoreCase(_MonthlyEquations)) {
		warning += "\nThe number of equations: \"" + NumberOfEquations +
			"\"\nmust be blank, " + _OneEquation +
			" (default), or " + _MonthlyEquations + ".";
	}
	if ( AnalysisMonth != null ) {
		if ( !StringUtil.isInteger(AnalysisMonth) ) {
			warning += "\nThe analysis month: \"" + AnalysisMonth +
				"\" is not an integer.";
		}
		else if((StringUtil.atoi(AnalysisMonth) < 1) ||
			(StringUtil.atoi(AnalysisMonth) > 12) ) {
			warning += "\nThe analysis month: \"" + AnalysisMonth +
				"\" must be in the range 1 to 12.";
		}
	}
	if ( Transformation != null ) {
		if ( Transformation.equalsIgnoreCase(_Linear) ) {
			// Convert old to new...
			Transformation = _None;
		}
		if (	!Transformation.equalsIgnoreCase(_Log) &&
			!Transformation.equalsIgnoreCase(_None) ) {
			warning += "\nThe transformation: \"" + Transformation +
			"\"\nmust be blank, " + _Log + ", or " + _None +
			" (default).";
		}
	}
	if ( (Intercept != null) && !Intercept.equals("") ) {
		if ( !StringUtil.isDouble(Intercept) ) {
			warning += "\nThe intercept: \"" + Intercept +
				"\" is not a number.";
		}
		else if ( StringUtil.atod(Intercept) != 0.0 ) {
			warning += "\nThe intercept: \"" + Intercept +
				"\" is not zero (only 0 or blank is " +
				"currently supported).";
		}
		if ( (Transformation != null) && Transformation.equals(_Log)){
			warning += "\nThe intercept: \"" + Intercept +
				"\" currently cannot be specified with log " +
				"transformation.\nSpecify blank or change the "+
				"transformation to None.";
		}
	}
	if (	(AnalysisStart != null) && !AnalysisStart.equals("") &&
		!AnalysisStart.equalsIgnoreCase("OutputStart") ) {
		try {	DateTime.parse(AnalysisStart);
		}
		catch ( Exception e ) {
			warning += 
				"\nThe analysis start date/time \"" +
				AnalysisStart +"\" is not a valid date/time.\n"+
				"Specify a date or OutputStart.";
		}
	}
	if (	(AnalysisEnd != null) && !AnalysisEnd.equals("") &&
		!AnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse( AnalysisEnd);
		}
		catch ( Exception e ) {
			warning +=
				"\nThe dependent end date/time \"" +
				AnalysisEnd + "\" is not a valid date/time.\n"+
				"Specify a date or OutputEnd.";
		}
	}
	if (	(FillStart != null) && !FillStart.equals("") &&
		!FillStart.equalsIgnoreCase("OutputStart")){
		try {	DateTime.parse(FillStart);
		}
		catch ( Exception e ) {
			warning += 
				"\nThe fill start date/time \"" + FillStart +
				"\" is not a valid date/time.\n"+
				"Specify a date, OutputStart.";
		}
	}
	if (	(FillEnd != null) && !FillEnd.equals("") &&
		!FillEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse( FillEnd);
		}
		catch ( Exception e ) {
			warning +=
				"\nThe fill end date/time \"" + FillEnd +
				"\" is not a valid date/time.\n"+
				"Specify a date, OutputEnd.";
		}
	}
	if ( (FillFlag != null) && (FillFlag.length() > 1) ) {
		warning +=
			"\nThe fill flag \"" + FillFlag +
			"\" should be a single character.";
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
	return (new fillRegression_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports very-old syntax (separate commands for different combinations of
parameters), newer syntax (one command but fixed-parameter list), and current
syntax (free-format parameters).
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
	String routine = "fillRegression_Command.parseCommand", message;

	if (	(command.indexOf('=') < 0) ||
		(StringUtil.patternCount(command,"=") == 1) ) {
		// REVISIT SAM 2005-04-29 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new
		// syntax.
		//
		// Old syntax (not free-format parameters or with only
		// the Intercept= syntax)...
		// Parse up front.  Don't parse with spaces because a
		// TEMPTS may be present.
		Vector v = StringUtil.breakStringList(command,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS |
			StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		if ( ntokens < 5 ) {
			message = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}

		// Get the individual tokens of the expression...

		String TSID = "";
		String IndependentTSID = "";
		String NumberOfEquations = "";
		String AnalysisMonth = "";
		String Transformation = "";
		String Intercept = "";
		String AnalysisStart = "";
		String AnalysisEnd = "";
		String FillStart = "";
		String FillEnd = "";
		int ic = 1;		// Position 0 is the command name
		TSID = ((String)v.elementAt(ic++)).trim();
		IndependentTSID = ((String)v.elementAt(ic++)).trim();
		NumberOfEquations=((String)v.elementAt(ic++)).trim();
		Transformation = ((String)v.elementAt(ic++)).trim();
		int icmax = ic + 1;
		if ( ntokens >= icmax ) {
			AnalysisStart = ((String)v.elementAt(ic++)).trim();
			if ( AnalysisStart.equals("*") ) {
				AnalysisStart = "";// Current default
			}
			if ( ntokens >= (icmax + 1) ) {
				AnalysisEnd =((String)v.elementAt(ic++)).trim();
			}
			if ( AnalysisEnd.equals("*") ) {
				AnalysisEnd = "";// Current default
			}
		}
		// All others have the fill period...
		if ( ntokens >= icmax ) {
			FillStart = ((String)v.elementAt(ic++)).trim();
		}
		if ( FillStart.equals("*") ) {
			FillStart = "";	// Current default.
		}
		if ( ntokens >= (icmax + 1) ) {
			FillEnd = ((String)v.elementAt(ic++)).trim();
		}
		if ( FillEnd.equals("*") ) {
			FillEnd = "";	// Current default.
		}

		// Check for new-style properties (only Intercept=)...

		String token, token0;
		Vector v2;
		for ( ic = 0; ic < ntokens; ic++ ) {
			// Check for an '=' in the token...
			token = (String)v.elementAt(ic);
			if ( token.indexOf('=') < 0 ) {
				continue;
			}
			v2 = StringUtil.breakStringList ( token, "=", 0 );
			if ( v2.size() < 2 ) {
				continue;
			}
			token0 = ((String)v2.elementAt(0)).trim();
			if ( token0.equalsIgnoreCase("Intercept") ) {
				Intercept = ((String)v2.elementAt(1)).trim();
			}
		}
		v = null;
		_parameters = new PropList ( getCommandName() );
		_parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID.length() > 0 ) {
			_parameters.set ( "TSID", TSID );
		}
		if ( IndependentTSID.length() > 0 ) {
			_parameters.set ( "IndependentTSID", IndependentTSID );
		}
		if ( NumberOfEquations.length() > 0 ) {
			_parameters.set("NumberOfEquations", NumberOfEquations);
		}
		if ( AnalysisMonth.length() > 0 ) {
			_parameters.set ( "AnalysisMonth", AnalysisMonth );
		}
		if ( Transformation.length() > 0 ) {
			_parameters.set ( "Transformation", Transformation );
		}
		if ( Intercept.length() > 0 ) {
			_parameters.set ( "Intercept", Intercept );
		}
		if ( AnalysisStart.length() > 0 ) {
			_parameters.set ( "AnalysisStart", AnalysisStart );
		}
		if ( AnalysisEnd.length() > 0 ) {
			_parameters.set ( "AnalysisEnd", AnalysisEnd );
		}
		if ( FillStart.length() > 0 ) {
			_parameters.set ( "FillStart", FillStart );
		}
		if ( FillEnd.length() > 0 ) {
			_parameters.set ( "FillEnd", FillEnd );
		}
		_parameters.setHowSet ( Prop.SET_UNKNOWN );
	}

	else {	// Current syntax...
		Vector tokens = StringUtil.breakStringList ( command,
			"()", StringUtil.DELIM_SKIP_BLANKS );
		if ( (tokens == null) || tokens.size() < 2 ) {
			// Must have at least the command name, TSID, and
			// IndependentTSID...
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
regress*()
fillRegression()
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
{	String routine = "fillRegression_Command.runCommand", message;
	int warning_count = 0;
	int log_level = 3;	// Warning level for non-user messages

	// Make sure there are time series available to operate on...

	String TSID = _parameters.getValue ( "TSID" );
	
	PropList request_params = new PropList ( "" );
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
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TS = bean_PropList.getContents ( "TS");
	TS ts_to_fill = null;
	if ( o_TS == null ) {
		message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID +
		"\" from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
	}
	else {
		ts_to_fill = (TS)o_TS;
	}
	
	if ( ts_to_fill == null ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count),
			routine, "Unable to find dependent time series \"" +
			TSID+"\".");
	}
	// The independent identifier may or may not have TEMPTS at the front
	// but is handled by getTimeSeries...
	String IndependentTSID = _parameters.getValue ( "IndependentTSID" );
	
	request_params = new PropList ( "" );
	request_params.set ( "CommandTag", command_tag );
	request_params.set ( "TSID", IndependentTSID );
	bean = null;
	try { bean =
		_processor.processRequest( "GetTimeSeriesForTSID", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + IndependentTSID +
		"\" from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
	}
	bean_PropList = bean.getResultsPropList();
	o_TS = bean_PropList.getContents ( "TS");
	TS ts_independent = null;
	if ( o_TS == null ) {
		message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + IndependentTSID +
		"\" from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
	}
	else {
		ts_independent = (TS)o_TS;
	}
	
	if ( ts_independent == null ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count),
			routine, "Unable to find independent time series \"" +
			IndependentTSID + "\"." );
	}

	// Now set the fill properties for TSUtil.fillRegress()...

	PropList props = new PropList ( "fillRegression" );
	String NumberOfEquations =_parameters.getValue ( "NumberOfEquations" );
	if ( NumberOfEquations == null ) {
		NumberOfEquations = _OneEquation;	// default
	}
	props.set ( "NumberOfEquations", NumberOfEquations );

	String AnalysisMonth =_parameters.getValue("AnalysisMonth");
	if ( AnalysisMonth != null ) {
		props.set ( "AnalysisMonth", AnalysisMonth );
	}

	String Transformation =_parameters.getValue("Transformation");
	if (	(Transformation == null) ||
		Transformation.equalsIgnoreCase(_Linear) ) {
		Transformation = _None;	// default (old _Linear is obsolete)
	}
	props.set ( "Transformation", Transformation );

	// Check whether the MOVE2 algorithm should be used...

	props.set ( "AnalysisMethod", "OLSRegression" );

	/* REVISIT SAM 2005-05-05 Need to have a similar MOVE2 command and not
	include here..
	if ( command.equalsIgnoreCase("fillMOVE1") ) {
		props.set ( "AnalysisMethod", "MOVE1" );
	}
	else if ( command.equalsIgnoreCase("fillMOVE2") ) {
		props.set ( "AnalysisMethod", "MOVE2" );
	}
	else {	// Ordinary least squares...
		props.set ( "AnalysisMethod", "OLSRegression" );
	}
	*/

	// Indicate that the analysis is being done for filling (this property
	// is used in the TSRegression class).  The default for TSRegression to
	// compute RMSE to compare time series (e.g., for calibration).  If
	// filling is indicated, then RMSE is computed from the dependent and
	// the estimated dependent...

	props.set ( "AnalyzeForFilling", "true" );

	// Set the analysis/fill periods...

	String AnalysisStart = _parameters.getValue("AnalysisStart");
	if ( AnalysisStart != null ) {
		props.set ( "AnalysisStart=" + AnalysisStart );
	}
	String AnalysisEnd =_parameters.getValue("AnalysisEnd");
	if ( AnalysisEnd != null ) {
		props.set ( "AnalysisEnd="+ AnalysisEnd );
	}

	String FillStart = _parameters.getValue("FillStart");
	if ( FillStart != null ) {
		props.set ( "FillStart="+ FillStart );
	}
	String FillEnd = _parameters.getValue("FillEnd");
	if ( FillEnd != null ) {
		props.set ( "FillEnd="+ FillEnd );
	}

	String FillFlag = _parameters.getValue("FillFlag");
	if ( (FillFlag != null) && !FillFlag.equals("") ) {
		props.set ( "FillFlag="+ FillFlag );
	}

	String Intercept = _parameters.getValue("Intercept");
	if ( (Intercept != null) && !Intercept.equals("") ) {
		props.set ( "Intercept="+ Intercept );
	}

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
	}

	// Call the code that is used by both the old and new version...

	// Figure out the dates to use for the analysis...
	DateTime FillStart_DateTime = null;
	DateTime FillEnd_DateTime = null;

	try {
		if ( FillStart != null ) {
			request_params = new PropList ( "" );
			request_params.set ( "DateTime", FillStart );
			bean = null;
			try { bean =
				_processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting FillStart DateTime(DateTime=" +
				FillStart + "\" from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
				throw new InvalidCommandParameterException ( message );
			}

			bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for FillStart DateTime(DateTime=" +
				FillStart +	"\") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
				throw new InvalidCommandParameterException ( message );
			}
			else {	FillStart_DateTime = (DateTime)prop_contents;
			}
		}
		}
		catch ( Exception e ) {
			message = "FillStart \"" + FillStart + "\" is invalid.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			throw new InvalidCommandParameterException ( message );
		}
		
		try {
		if ( FillEnd != null ) {
			request_params = new PropList ( "" );
			request_params.set ( "DateTime", FillEnd );
			bean = null;
			try { bean =
				_processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting FillEnd DateTime(DateTime=" +
				FillEnd + "\" from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
				throw new InvalidCommandParameterException ( message );
			}

			bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for FillStart DateTime(DateTime=" +
				FillStart +	"\") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
				throw new InvalidCommandParameterException ( message );
			}
			else {	FillEnd_DateTime = (DateTime)prop_contents;
			}
		}
		}
		catch ( Exception e ) {
			message = "FillEnd \"" + FillEnd + "\" is invalid.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			throw new InvalidCommandParameterException ( message );
		}
	
	// Fill the dependent time series...
	// This will result in the time series in the original data
	// being modified...
	try {	TSRegression regress_results = TSUtil.fillRegress ( 
			ts_to_fill, ts_independent, FillStart_DateTime,
			FillEnd_DateTime, props );
		// Print the results to the log file...
		if ( regress_results != null ) {
			Message.printStatus ( 2, routine,
			"Fill results are..." );
			Message.printStatus ( 2, routine,
			regress_results.toString() );
			// REVISIT SAM 2005-05-05 Need to call setPropContents
			// on the TSCommandProcessor?
		}
		else {	message = "Unable to compute regression.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
			throw new CommandException ( message );
		}
	}
	catch ( Exception e ) {
		message = "Error performing regression for \""+toString() +"\"";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
		Message.printWarning ( 3, routine, e );
		throw new CommandException ( message );
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
	String TSID = props.getValue( "TSID" );
	String IndependentTSID = props.getValue("IndependentTSID");
	String NumberOfEquations = props.getValue("NumberOfEquations");
	String AnalysisMonth = props.getValue("AnalysisMonth");
	String Transformation = props.getValue("Transformation");
	String Intercept = props.getValue("Intercept");
	String AnalysisStart = props.getValue("AnalysisStart");
	String AnalysisEnd = props.getValue("AnalysisEnd");
	String FillStart = props.getValue("FillStart");
	String FillEnd = props.getValue("FillEnd");
	String FillFlag = props.getValue("FillFlag");
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (IndependentTSID != null) && (IndependentTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IndependentTSID=\"" + IndependentTSID + "\"" );
	}
	if ( (NumberOfEquations != null) && (NumberOfEquations.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NumberOfEquations=" + NumberOfEquations );
	}
	if ( (AnalysisMonth != null) && (AnalysisMonth.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AnalysisMonth=" + AnalysisMonth );
	}
	if ( (Transformation != null) && (Transformation.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Transformation=" + Transformation );
	}
	if ( (Intercept != null) && (Intercept.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Intercept=" + Intercept );
	}
	if ( (AnalysisStart != null) && (AnalysisStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AnalysisStart=\"" + AnalysisStart + "\"" );
	}
	if ( (AnalysisEnd != null) && (AnalysisEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AnalysisEnd=\"" + AnalysisEnd + "\"" );
	}
	if ( (FillStart != null) && (FillStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillStart=\"" + FillStart + "\"" );
	}
	if ( (FillEnd != null) && (FillEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillEnd=\"" + FillEnd + "\"" );
	}
	if ( (FillFlag != null) && (FillFlag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillFlag=\"" + FillFlag + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
