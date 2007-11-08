//------------------------------------------------------------------------------
// fillMOVE2_Command - handle the fillMOVE2() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-04-29	Steven A. Malers, RTi	Initial version.  Copy and modify the
//					fillRegression_Command code.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import java.util.Vector;

import javax.swing.JFrame;

import RTi.TS.TS;
import RTi.TS.TSRegression;
import RTi.TS.TSUtil;
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
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
<p>
This class initializes, checks, and runs the fillMOVE2() command.
</p>
<p>The CommandProcessor must return the following properties:  TSResultsList.
</p>
*/
public class fillMOVE2_Command extends AbstractCommand implements Command
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
public fillMOVE2_Command ()
{	super();
	setCommandName ( "fillMOVE2" );
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
	// REVISIT SAM 2006-04-13
	// can this be enabled?
	//String AnalysisMonth = parameters.getValue ( "AnalysisMonth" );
	String Transformation = parameters.getValue ( "Transformation" );
	// REVISIT SAM 2006-04-13
	// can this be enabled?
	//String Intercept = parameters.getValue ( "Intercept" );
	String DependentAnalysisStart = parameters.getValue (
		"DependentAnalysisStart" );
	String DependentAnalysisEnd = parameters.getValue (
		"DependentAnalysisEnd" );
	String IndependentAnalysisStart = parameters.getValue (
		"IndependentAnalysisStart" );
	String IndependentAnalysisEnd = parameters.getValue (
		"IndependentAnalysisEnd" );
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
	/* REVISIT SAM 2006-04-13 Can this be enabled?
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
	*/
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
	/* REVISIT SAM 2006-04-13 Can this be enabled?
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
	*/
	if (	(DependentAnalysisStart != null) &&
		!DependentAnalysisStart.equals("") &&
		!DependentAnalysisStart.equalsIgnoreCase("OutputStart") ) {
		try {	DateTime.parse(DependentAnalysisStart);
		}
		catch ( Exception e ) {
			warning += 
				"\nThe dependent analysis start date/time \"" +
				DependentAnalysisStart +
				"\" is not a valid date/time.\n"+
				"Specify a date/time or OutputStart.";
		}
	}
	if (	(DependentAnalysisEnd != null) &&
		!DependentAnalysisEnd.equals("") &&
		!DependentAnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse(DependentAnalysisEnd);
		}
		catch ( Exception e ) {
			warning +=
				"\nThe dependent analysis end date/time \"" +
				DependentAnalysisEnd +
				"\" is not a valid date/time.\n"+
				"Specify a date/time or OutputEnd.";
		}
	}
	if (	(IndependentAnalysisStart != null) &&
		!IndependentAnalysisStart.equals("") &&
		!IndependentAnalysisStart.equalsIgnoreCase("OutputStart") ) {
		try {	DateTime.parse(IndependentAnalysisStart);
		}
		catch ( Exception e ) {
			warning += 
				"\nThe Independent analysis start date/time \""
				+ IndependentAnalysisStart +
				"\" is not a valid date/time.\n"+
				"Specify a date/time or OutputStart.";
		}
	}
	if (	(IndependentAnalysisEnd != null) &&
		!IndependentAnalysisEnd.equals("") &&
		!IndependentAnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse(IndependentAnalysisEnd);
		}
		catch ( Exception e ) {
			warning +=
				"\nThe Independent analysis end date/time \"" +
				IndependentAnalysisEnd +
				"\" is not a valid date/time.\n"+
				"Specify a date/time or OutputEnd.";
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
				"Specify a date/time or OutputStart.";
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
				"Specify a date/time or OutputEnd.";
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
	return (new fillMOVE2_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports very-old syntax (separate commands for different combinations of
parameters), newer syntax (one command but fixed-parameter list), and current
syntax (free-format parameters).
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
	String routine = "fillMOVE2_Command.parseCommand", message;

	if ( command.indexOf('=') < 0 ) {
		// REVISIT SAM 2006-04-16 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new
		// syntax.
		//
		// Old syntax (not free-format parameters)...
		// Parse up front.  Don't parse with spaces because a
		// TEMPTS may be present.
		Vector v = StringUtil.breakStringList(command,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS |
			StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		if ( ntokens < 10 ) {
			message = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}

		// Get the individual tokens of the expression...

		String TSID = "";
		String IndependentTSID = "";
		String NumberOfEquations = "";
		//String AnalysisMonth = "";
		String Transformation = "";
		//String Intercept = "";
		String DependentAnalysisStart = "";
		String DependentAnalysisEnd = "";
		String IndependentAnalysisStart = "";
		String IndependentAnalysisEnd = "";
		String FillStart = "";
		String FillEnd = "";
		int ic = 0;
		TSID = ((String)v.elementAt(ic++)).trim();
		IndependentTSID = ((String)v.elementAt(ic++)).trim();
		NumberOfEquations=((String)v.elementAt(ic++)).trim();
		Transformation = ((String)v.elementAt(ic++)).trim();
		DependentAnalysisStart = ((String)v.elementAt(ic++)).trim();
		if ( DependentAnalysisStart.equals("*") ) {
			DependentAnalysisStart = "";// Current default
		}
		DependentAnalysisEnd =((String)v.elementAt(ic++)).trim();
		if ( DependentAnalysisEnd.equals("*") ) {
			DependentAnalysisEnd = "";// Current default
		}
		IndependentAnalysisStart = ((String)v.elementAt(ic++)).trim();
		if ( IndependentAnalysisStart.equals("*") ) {
			IndependentAnalysisStart = "";// Current default
		}
		IndependentAnalysisEnd =((String)v.elementAt(ic++)).trim();
		if ( IndependentAnalysisEnd.equals("*") ) {
			IndependentAnalysisEnd = "";// Current default
		}
		FillStart = ((String)v.elementAt(ic++)).trim();
		if ( FillStart.equals("*") ) {
			FillStart = "";	// Current default.
		}
		FillEnd = ((String)v.elementAt(ic++)).trim();
		if ( FillEnd.equals("*") ) {
			FillEnd = "";	// Current default.
		}

		v = null;
		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID.length() > 0 ) {
			parameters.set ( "TSID", TSID );
		}
		if ( IndependentTSID.length() > 0 ) {
			parameters.set ( "IndependentTSID", IndependentTSID );
		}
		if ( NumberOfEquations.length() > 0 ) {
			parameters.set("NumberOfEquations", NumberOfEquations);
		}
		/* REVISIT SAM 2006-04-16
			Evaluate whether this can be enabled
		if ( AnalysisMonth.length() > 0 ) {
			_parameters.set ( "AnalysisMonth", AnalysisMonth );
		}
		*/
		if ( Transformation.length() > 0 ) {
			parameters.set ( "Transformation", Transformation );
		}
		/* REVISIT SAM 2006-04-16
			Evaluate whether this can be enabled
		if ( Intercept.length() > 0 ) {
			_parameters.set ( "Intercept", Intercept );
		}
		*/
		if ( DependentAnalysisStart.length() > 0 ) {
			parameters.set ( "DependentAnalysisStart",
				DependentAnalysisStart );
		}
		if ( DependentAnalysisEnd.length() > 0 ) {
			parameters.set ( "DependentAnalysisEnd",
			DependentAnalysisEnd );
		}
		if ( IndependentAnalysisStart.length() > 0 ) {
			parameters.set ( "IndependentAnalysisStart",
				IndependentAnalysisStart );
		}
		if ( IndependentAnalysisEnd.length() > 0 ) {
			parameters.set ( "IndependentAnalysisEnd",
			IndependentAnalysisEnd );
		}
		if ( FillStart.length() > 0 ) {
			parameters.set ( "FillStart", FillStart );
		}
		if ( FillEnd.length() > 0 ) {
			parameters.set ( "FillEnd", FillEnd );
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
fillMOVE2()
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
{	String routine = "fillMOVE2_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String TSID = parameters.getValue ( "TSID" );
	
	PropList request_params = new PropList ( "" );
	request_params.set ( "CommandTag", command_tag );
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try { bean =
		processor.processRequest( "GetTimeSeriesForTSID", request_params);
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
	String IndependentTSID = parameters.getValue ( "IndependentTSID" );
	request_params = new PropList ( "" );
	request_params.set ( "CommandTag", command_tag );
	request_params.set ( "TSID", IndependentTSID );
	bean = null;
	try { bean =
		processor.processRequest( "GetTimeSeriesForTSID", request_params);
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

	PropList props = new PropList ( "fillMOVE2" );
	props.set ( "AnalysisMethod", "MOVE2" );
	String NumberOfEquations = parameters.getValue ( "NumberOfEquations" );
	if ( NumberOfEquations == null ) {
		NumberOfEquations = _OneEquation;	// default
	}
	props.set ( "NumberOfEquations", NumberOfEquations );

	/* REVISIT SAM 2006-04-16
		Evaluate whether this can be enabled.
	String AnalysisMonth =_parameters.getValue("AnalysisMonth");
	if ( AnalysisMonth != null ) {
		props.set ( "AnalysisMonth", AnalysisMonth );
	}
	*/

	String Transformation = parameters.getValue("Transformation");
	if (	(Transformation == null) ||
		Transformation.equalsIgnoreCase(_Linear) ) {
		Transformation = _None;	// default (old _Linear is obsolete)
	}
	props.set ( "Transformation", Transformation );

	// Indicate that the analysis is being done for filling (this property
	// is used in the TSRegression class).  The default for TSRegression to
	// compute RMSE to compare time series (e.g., for calibration).  If
	// filling is indicated, then RMSE is computed from the dependent and
	// the estimated dependent...

	props.set ( "AnalyzeForFilling", "true" );

	// Set the analysis/fill periods...

	String DependentAnalysisStart =
		parameters.getValue("DependentAnalysisStart");
	if ( DependentAnalysisStart != null ) {
		props.set ( "DependentAnalysisStart=" + DependentAnalysisStart);
	}
	String DependentAnalysisEnd =
		parameters.getValue("DependentAnalysisEnd");
	if ( DependentAnalysisEnd != null ) {
		props.set ( "DependentAnalysisEnd=" + DependentAnalysisEnd );
	}

	String IndependentAnalysisStart =
		parameters.getValue("IndependentAnalysisStart");
	if ( IndependentAnalysisStart != null ) {
		props.set ( "IndependentAnalysisStart=" +
			IndependentAnalysisStart);
	}
	String IndependentAnalysisEnd =
		parameters.getValue("IndependentAnalysisEnd");
	if ( IndependentAnalysisEnd != null ) {
		props.set ( "IndependentAnalysisEnd=" + IndependentAnalysisEnd);
	}

	String FillStart = parameters.getValue("FillStart");
	if ( FillStart != null ) {
		props.set ( "FillStart="+ FillStart );
	}
	String FillEnd = parameters.getValue("FillEnd");
	if ( FillEnd != null ) {
		props.set ( "FillEnd="+ FillEnd );
	}

	String FillFlag = parameters.getValue("FillFlag");
	if ( (FillFlag != null) && !FillFlag.equals("") ) {
		props.set ( "FillFlag="+ FillFlag );
	}

	/* REVISIT SAM 2006-04-16
		Evaluate whether this can be enabled
	String Intercept = _parameters.getValue("Intercept");
	if ( (Intercept != null) && !Intercept.equals("") ) {
		props.set ( "Intercept="+ Intercept );
	}
	*/

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
				processor.processRequest( "DateTime", request_params);
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
				processor.processRequest( "DateTime", request_params);
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
			"Analysis results are..." );
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
	//String AnalysisMonth = props.getValue("AnalysisMonth");
	String Transformation = props.getValue("Transformation");
	//String Intercept = props.getValue("Intercept");
	String DependentAnalysisStart =props.getValue("DependentAnalysisStart");
	String DependentAnalysisEnd = props.getValue("DependentAnalysisEnd");
	String IndependentAnalysisStart =
		props.getValue("IndependentAnalysisStart");
	String IndependentAnalysisEnd =props.getValue("IndependentAnalysisEnd");
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
	/*
	if ( (AnalysisMonth != null) && (AnalysisMonth.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AnalysisMonth=" + AnalysisMonth );
	}
	*/
	if ( (Transformation != null) && (Transformation.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Transformation=" + Transformation );
	}
	/*
	if ( (Intercept != null) && (Intercept.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Intercept=" + Intercept );
	}
	*/
	if (	(DependentAnalysisStart != null) &&
		(DependentAnalysisStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DependentAnalysisStart=\"" +
			DependentAnalysisStart + "\"" );
	}
	if (	(DependentAnalysisEnd != null) &&
		(DependentAnalysisEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DependentAnalysisEnd=\"" +
			DependentAnalysisEnd + "\"" );
	}
	if (	(IndependentAnalysisStart != null) &&
		(IndependentAnalysisStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IndependentAnalysisStart=\"" +
			IndependentAnalysisStart + "\"" );
	}
	if (	(IndependentAnalysisEnd != null) &&
		(IndependentAnalysisEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IndependentAnalysisEnd=\"" +
			IndependentAnalysisEnd + "\"" );
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
