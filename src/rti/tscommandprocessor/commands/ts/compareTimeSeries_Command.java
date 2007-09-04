//------------------------------------------------------------------------------
// compareTimeSeries_Command - handle the compareTimeSeries() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-05-10	Steven A. Malers, RTi	Initial version.
// 2005-05-16	SAM, RTi		Add the ability to flag the values that
//					are different.
// 2005-05-18	SAM, RTi		* Add AnalysisStart and AnalysisEnd
//					  parameters.
//					* Add an average error, allowing
//					  positive and negative to cancel.
//					* Print the data flag for values that
//					  are different, to facilitate finding
//					  filled values that are different.
//					* Add MatchLocation, MatchDataType
//					  parameters.
// 2005-05-19	SAM, RTi		* Change so printed values use the
//					  given precision, or a default of 6.
//					* Change to print out the maximum
//					  difference as an actual value (not
//					  absolute value).
//					* Fix problem with matching if only the
//					  location is being used to match time
//					  series.
//					* Move from TSTool package to TS.
// 2005-05-21	SAM, RTi		* Minor changes to printed results.
//					* Add the WarnIfDifferent parameter to
//					  help automate testing.
// 2005-05-25	SAM, RTi		* Make sure that time series being
//					  compared have the same interval.
//					* Change to using a TSIterator to loop
//					  through data, to handle irregular
//					  data.  The logic is OK since all data
//					  access is relative to the first time
//					  series.
// 2005-06-01	SAM, RTi		* Change so that missing in one time
//					  series but not another is considered
//					  a difference.
//					* Change so that by default the longest
//					  overlapping period is used for the
//					  comparison, if the analysis period
//					  is not specified.
// 2005-06-07	SAM, RTi		* Fix bug where "no differences" was
//					  being indicated in some cases - the
//					  check was being zeroed out for each
//					  tolerance.
// 2005-06-10	SAM, RTi		* Format a summary of the results at the
//					  end, to use in reports.
// 2005-07-08	SAM, RTi		* Clarify in messages that differences
//					  are computed as TS2 - TS1.
// 2006-05-02	SAM, RTi		* Add WarnIfSame parameter.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-04-08	SAM, RTi		* Add CreateDiffTS to help checking CDSS old
//					and new data sets.
// 2007-04-13	SAM, RTi		* Add summary of only differences, to facilitate
//					documentation preparation.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import java.util.Vector;
import javax.swing.JFrame;

import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSIterator;
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
This class initializes, checks, and runs the compareTimeSeries() command.
</p>
<p>The CommandProcessor must return the following properties:  TSResultsList.
</p>
*/
public class compareTimeSeries_Command extends AbstractCommand
implements Command
{

/**
Data members used for parameter values.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Constructor.
*/
public compareTimeSeries_Command ()
{	super();
	setCommandName ( "compareTimeSeries" );
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
{	String MatchLocation = parameters.getValue ( "MatchLocation" );
	String MatchDataType = parameters.getValue ( "MatchDataType" );
	String Precision = parameters.getValue ( "Precision" );
	String Tolerance = parameters.getValue ( "Tolerance" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String DiffFlag = parameters.getValue ( "DiffFlag" );
	String CreateDiffTS = parameters.getValue ( "CreateDiffTS" );
	String WarnIfDifferent = parameters.getValue ( "WarnIfDifferent" );
	String WarnIfSame = parameters.getValue ( "WarnIfSame" );
	String warning = "";

	if ( (MatchLocation != null) && !MatchLocation.equals("") ) {
		if (	!MatchLocation.equals(_False) &&
			!MatchLocation.equals(_True) ) {
			warning += "\nThe MatchLocation parameter \"" +
				MatchLocation + "\" must be False or True.";
		}
	}
	if ( (MatchDataType != null) && !MatchDataType.equals("") ) {
		if (	!MatchDataType.equals(_False) &&
			!MatchDataType.equals(_True) ) {
			warning += "\nThe MatchDataType parameter \"" +
				MatchDataType + "\" must be False or True.";
		}
	}
	if ( (Precision != null) && !Precision.equals("") ) {
		if ( !StringUtil.isInteger(Precision) ) {
			warning += "\nThe precision: \"" + Precision +
				"\" is not an integer.";
		}
		if ( StringUtil.atoi(Precision) < 0 ) {
			warning += "\nThe precision: \"" + Precision +
				"\" must be >= 0.";
		}
	}
	if ( (Tolerance != null) && !Tolerance.equals("") ) {
		Vector v = StringUtil.breakStringList(Tolerance,", ",0);
		int size = 0;
		if ( v != null ) {
			size = v.size();
		}
		// Make sure that each tolerance is a number...
		String string;
		for ( int i = 0; i < size; i++ ) {
			string = (String)v.elementAt(i);
			if ( !StringUtil.isDouble(string) ) {
				warning += "\nThe tolerance: \"" + string +
					"\" is not a number.";
			}
			if ( StringUtil.atod(string) < 0.0 ) {
				warning += "\nThe tolerance: \"" + Tolerance +
					"\" must be >= 0.0.";
			}
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
	if (	(DiffFlag != null) &&
		(DiffFlag.length() != 1) ) {
		warning += "\nThe difference flag \"" + DiffFlag +
				"\" must be 1 character long.";
	}
	if ( (CreateDiffTS != null) && !CreateDiffTS.equals("") ) {
		if (	!CreateDiffTS.equals(_False) &&
			!CreateDiffTS.equals(_True) ) {
			warning += "\nThe CreateDiffTS parameter \"" +
			CreateDiffTS + "\" must be False or True.";
		}
	}
	if ( (WarnIfDifferent != null) && !WarnIfDifferent.equals("") ) {
		if (	!WarnIfDifferent.equals(_False) &&
			!WarnIfDifferent.equals(_True) ) {
			warning += "\nThe WarnIfDifferent parameter \"" +
				WarnIfDifferent + "\" must be False or True.";
		}
	}
	if ( (WarnIfSame != null) && !WarnIfSame.equals("") ) {
		if (	!WarnIfSame.equals(_False) &&
			!WarnIfSame.equals(_True) ) {
			warning += "\nThe WarnIfSame parameter \"" +
				WarnIfSame + "\" must be False or True.";
		}
	}
	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
	valid_Vector.add ( "MatchLocation" );
	valid_Vector.add ( "MatchDataType" );
	valid_Vector.add ( "Precision" );
	valid_Vector.add ( "Tolerance" );
	valid_Vector.add ( "AnalysisStart" );
	valid_Vector.add ( "AnalysisEnd" );
	valid_Vector.add ( "DiffFlag" );
	valid_Vector.add ( "CreateDiffTS" );
	valid_Vector.add ( "WarnIfDifferent" );
	valid_Vector.add ( "WarnIfSame" );
	Vector warning_Vector = null;
	try {	warning_Vector = parameters.validatePropNames (
			valid_Vector, null, null, "parameter" );
	}
	catch ( Exception e ) {
		// Ignore.  Should not happen.
		warning_Vector = null;
	}
	if ( warning_Vector != null ) {
		int size = warning_Vector.size();
		for ( int i = 0; i < size; i++ ) {
			warning += "\n" + (String)warning_Vector.elementAt (i);
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
	return (new compareTimeSeries_JDialog ( parent, this )).ok();
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
{	int warning_count = 0;
	String routine = "compareTimeSeries_Command.parseCommand", message;

	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );

	if ( (tokens == null) ) { //|| tokens.size() < 2 ) {}
		message = "Invalid syntax for \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	// Get the input needed to process the command...
	if ( tokens.size() > 1 ) {
		try {	setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
				(String)tokens.elementAt(1), routine,"," ) );
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
compareTimeSeries(MatchLocation=X,MatchDataType=X,Precision=X,
Tolerance="X,X,X,...",DiffFlag="X",CreateDiffTS=X,WarnIfDifferent=X,WarnIfSame=X)
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
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "compareTimeSeries_Command.runCommand", message;
	int warning_count = 0;
	int log_level = 3;	// Level for non-user messages
	int size = 0;
	
	CommandProcessor processor = getCommandProcessor();
	PropList parameters = getCommandParameters();
	
	Vector tslist = null;
	try { Object o = processor.getPropContents( "TSResultsList" );
			tslist = (Vector)o;
	}
	catch ( Exception e ){
		message = "Cannot get time series list to process.";
		Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag, ++warning_count),
				routine,message);
	}
	
	if ( (tslist == null) || (tslist.size() == 0) ) {
		// Don't do anything...
		Message.printStatus ( 2, routine,
		"No time series are available.  Not comparing." );
		return;
	}
	// Check to make sure that the intervals are the same...
	if ( !TSUtil.areIntervalsSame(tslist) ) {
		message = "Time series intervals are not consistent.  Not able"+
		" to compare time series.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new CommandException ( message );
	}
	String MatchLocation = parameters.getValue ( "MatchLocation" );
	String MatchDataType = parameters.getValue ( "MatchDataType" );
	String Precision = parameters.getValue ( "Precision" );
						// What to round data to before
						// comparing.
	String Tolerance = parameters.getValue ( "Tolerance" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String DiffFlag = parameters.getValue ( "DiffFlag" );
	if ( (DiffFlag != null) && (DiffFlag.length() == 0) ) {
		// Set to null to indicate no flag in checks below...
		DiffFlag = null;
	}
	String CreateDiffTS = parameters.getValue ( "CreateDiffTS" );
	String WarnIfDifferent = parameters.getValue ( "WarnIfDifferent" );
	String WarnIfSame = parameters.getValue ( "WarnIfSame" );
	int Precision_int = 0;			// The number of digits after
						// the decimal to use for
						// comparisons.  If Precision is
						// null, no rounding occurs.

	boolean MatchLocation_boolean = true;	// Default
	if ( (MatchLocation != null) && MatchLocation.equalsIgnoreCase(_False)){
		MatchLocation_boolean = false;
	}
	boolean MatchDataType_boolean = false;	// Default
	if ( (MatchDataType != null) && MatchDataType.equalsIgnoreCase(_True)){
		MatchDataType_boolean = true;
	}
	boolean CreateDiffTS_boolean = false;	// Default
	if (	(CreateDiffTS != null) &&
			CreateDiffTS.equalsIgnoreCase(_True)){
		CreateDiffTS_boolean = true;
	}
	boolean WarnIfDifferent_boolean = false;	// Default
	if (	(WarnIfDifferent != null) &&
		WarnIfDifferent.equalsIgnoreCase(_True)){
		WarnIfDifferent_boolean = true;
	}
	boolean WarnIfSame_boolean = false;	// Default
	if (	(WarnIfSame != null) &&
		WarnIfSame.equalsIgnoreCase(_True)){
		WarnIfSame_boolean = true;
	}
	double [] Tolerance_double = null;
	String value_format = "%.6f";	// Default
	if ( Precision != null ) {
		Precision_int = StringUtil.atoi ( Precision );
		value_format = "%." + Precision_int + "f";
	}
	int Tolerance_count = 0;
	Vector Tolerance_tokens = null;
	if ( Tolerance != null ) {
		// The parameter has been specified as a list of one or more
		// numbers...
		Tolerance_tokens = StringUtil.breakStringList(Tolerance,", ",0);
		if ( Tolerance_tokens != null ) {
			Tolerance_count = Tolerance_tokens.size();
		}
		if ( Tolerance_count > 0 ) {
			Tolerance_double = new double[Tolerance_count];
		}
		// Get each tolerance as a number...
		for ( int it = 0; it < Tolerance_count; it++ ) {
			Tolerance_double[it] = StringUtil.atod(
				(String)Tolerance_tokens.elementAt(it) );
		}
	}
	else {	// Default is tolerance of 0.0...
		Tolerance_count = 1;
		Tolerance_double = new double[1];
		Tolerance_double[0] = 0.0;
		Tolerance_tokens = new Vector (1);
		Tolerance_tokens.addElement ("0");
	}

	DateTime AnalysisStart_DateTime = null;
	DateTime AnalysisEnd_DateTime = null;
	
	try {
	if ( AnalysisStart != null ) {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", AnalysisStart );
		CommandProcessorRequestResultsBean bean = null;
		try { bean =
			processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting AnalysisStart DateTime(DateTime=" +
			AnalysisStart + "\" from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			throw new InvalidCommandParameterException ( message );
		}

		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for AnalysisStart DateTime(DateTime=" +
			AnalysisStart +	"\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			throw new InvalidCommandParameterException ( message );
		}
		else {	AnalysisStart_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "AnalysisStart \"" + AnalysisStart + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	try {
	if ( AnalysisEnd != null ) {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", AnalysisEnd );
		CommandProcessorRequestResultsBean bean = null;
		try { bean =
			processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting AnalysisEnd DateTime(DateTime=" +
			AnalysisEnd + "\" from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			throw new InvalidCommandParameterException ( message );
		}

		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for AnalysisStart DateTime(DateTime=" +
			AnalysisStart +	"\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			throw new InvalidCommandParameterException ( message );
		}
		else {	AnalysisEnd_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "AnalysisEnd \"" + AnalysisEnd + "\" is invalid.";
		Message.printWarning(warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	int [] diffcount;	// Count of differences
							// for each tolerance.
	double [] difftotal = new double[Tolerance_count];
							// Total difference for
							// each tolerance.
	double [] difftotalabs = new double[Tolerance_count];
							// Total difference for
							// each tolerance,
							// absolute values.
	double [] diffabsavg = null;
	double [] diffavg = null;
	int DiffFlag_length = 1;			// Longest DiffFlag
							// length.
	if ( DiffFlag != null ) {
		DiffFlag_length = DiffFlag.length();
	}
	boolean is_diff = false;			// Indicates whether a
							// differences has been
							// determined.
	TSData tsdata = null;				// Data point from time
							// series.
	boolean found_loc1 = false;			// Has the time series
	boolean found_datatype1 = false;		// already been
	boolean found_match = false;			// processed?
	int tsdiff_count = 0;				// The number of time
							// series that are
							// different.
	Vector compare_tslist = new Vector();		// Lists of data
	Vector compare_numvalues = new Vector();	// that were processed,
	Vector compare_diffmax = new Vector();		// that were processed,
	Vector compare_diffabsavg = new Vector();	// for the report.
	Vector compare_diffavg = new Vector();
	Vector compare_diffcount = new Vector();
	Vector compare_diffmaxdate = new Vector();
	Vector diffts_Vector = null;			// Vector if CreateDiffTS=True
	try {	// Loop through the time series.  For each time series, find its
		// matching time series, by location, ignoring itself.  When a
		// match is found, do the comparison.
		TS ts1, ts2;
		TS diffts = null;	// Difference time series
		TSIterator tsi;
		size = tslist.size();
		double	diff,				// differnce
			diffabs,			// abs(diff)
			value1, value1_orig,		// Data values from each
			value2, value2_orig;		// time series, rounded
							// and original.
		TSData tsdata1, tsdata2;		// Data points for each
							// time series.
		String flag1, flag2;			// Data flags for values
							// in each time series.
		double diffmax = 0.0, diffmaxabs;
		DateTime diffmax_DateTime = null;	// Date for max diff.
		DateTime date, date1 = null, date2 = null;
							// Dates for iteration.
		int j;
		int it;					// Counter for
							// tolerances.
		int totalcount = 0;			// Total non-missing
							// data values.
		String loc1, datatype1;			// Location and data
							// type for the first
							// time series. 
		Vector loc_Vector = new Vector();	// Location/datatype
		Vector datatype_Vector = new Vector();	// pairs that have
							// already been
							// processed.
		if ( CreateDiffTS_boolean ) {
			diffts_Vector = new Vector ( size );
		}
		for ( int i = 0; i < size; i++ ) {
			ts1 = (TS)tslist.elementAt(i);
			loc1 = ts1.getLocation();
			datatype1 = ts1.getDataType();
			// Make sure not to analyze the same combination
			// more than once, based on the location ID and data
			// type...
			// REVISIT SAM 2005-05-25 Also need to check interval
			// always.
			found_match = false;
			for ( j = 0; j < loc_Vector.size(); j++ ) {
				// Check the parts individually...
				found_loc1 = false;
				found_datatype1 = false;
				if (	loc1.equalsIgnoreCase(
					(String)loc_Vector.elementAt(j)) ) {
					found_loc1 = true;
				}
				if (	datatype1.equalsIgnoreCase(
					(String)datatype_Vector.elementAt(j))) {
					found_datatype1 = true;
				}
				// Now reset found_loc1 depending on whether
				// the location and datatype (one or both) are
				// being matched...
				if (	MatchLocation_boolean &&
					MatchDataType_boolean ) {
					// Need to check both...
					if ( found_loc1 && found_datatype1 ) {
						found_match = true;
						break;
					}
				}
				else if ( MatchLocation_boolean && found_loc1 ){
					found_match = true;
					break;
				}
				else if ( MatchDataType_boolean &&
					found_datatype1 ) {
					found_match = true;
					break;
				}
			}
			if ( found_match ) {
				// Have already processed this time series...
				continue;
			}
			// Try to find a matching location, skipping the same
			// instance and incompatible intervals...
			for ( j = 0; j < size; j++ ) {
				if ( i == j ) {
					continue;
				}
				ts2 = (TS)tslist.elementAt(j);
				// Make sure that the interval is the same...
				if (	!((ts1.getDataIntervalBase() ==
					ts2.getDataIntervalBase()) &&
					(ts1.getDataIntervalMult() ==
					ts2.getDataIntervalMult())) ) {
					// Intervals do not match...
					continue;
				}
				// Check the requested information...
				if (	MatchLocation_boolean &&
					!loc1.equalsIgnoreCase(
					ts2.getLocation())){
					// Not a match...
					continue;
				}
				if (	MatchDataType_boolean &&
					!datatype1.equalsIgnoreCase(
					ts2.getDataType())){
					// Not a match...
					continue;
				}
				// Have a match so do the comparison.  Currently
				// only compare the data values, not the header
				// information.
				loc_Vector.addElement ( loc1 );
				datatype_Vector.addElement ( datatype1 );
								// save for check
				// If the differences will be flagged, allocate
				// the data flag space in both time series.
				if ( DiffFlag != null ) {
					ts1.allocateDataFlagSpace (
						DiffFlag_length, null,
						true );	// Retain previous.
					ts2.allocateDataFlagSpace (
						DiffFlag_length, null,
						true );	// Retain previous.
				}
				// If a difference time series is to be created, do it...
				if ( CreateDiffTS_boolean ) {
					String diffid = "Diff_" +
						ts2.getIdentifier().toString();
					diffts = TSUtil.newTimeSeries ( diffid, true );
					diffts.copyHeader ( ts2 );
					diffts.setIdentifier ( diffid );
					diffts.allocateDataSpace();
					diffts_Vector.addElement ( diffts );
				}
				// Initialize for the comparison...
				totalcount = 0;
				diffmaxabs = -1.0e10;
				// Reallocate in order to have unique references
				// to save for the report...
				diffcount = new int[Tolerance_count];
				diffabsavg = new double[Tolerance_count];
				diffavg = new double[Tolerance_count];
				for ( it = 0; it < Tolerance_count; it++ ) {
					diffcount[it] = 0;
					difftotal[it] = (double)0.0;
					difftotalabs[it] = (double)0.0;
				}
				if ( AnalysisStart_DateTime == null ) {
					date1 = new DateTime (ts1.getDate1());
					if ( ts2.getDate1().lessThan(date1) ) {
						date1 = new DateTime(
							ts2.getDate1());
					}
				}
				else {	date1 = new DateTime (
						AnalysisStart_DateTime);
				}
				if ( AnalysisEnd_DateTime == null ) {
					date2 = new DateTime (ts1.getDate2());
					if ( ts2.getDate2().greaterThan(date2)){
						date2 = new DateTime(
							ts2.getDate2());
					}
				}
				else {	date2 = new DateTime (
						AnalysisEnd_DateTime);
				}
				Message.printStatus ( 2, routine,
				"TS1 = " + ts1.getIdentifier().toString(true) );
				Message.printStatus ( 2, routine,
				"TS2 = " + ts2.getIdentifier().toString(true) );
				Message.printStatus ( 2, routine,
					"Data differences TS2 - TS1 follow " +
					"(Tolerance=" + Tolerance + ", Period="+
					date1 + " to " + date2 + "):" );
				// Iterate using the first time series...
				tsi = ts1.iterator(date1,date2);
				for ( ; tsi.next() != null; ) {
					date = tsi.getDate();
					// This is not overly efficient but
					// currently the iterator does not have
					// a way to set a data point...
					tsdata1 = ts1.getDataPoint ( date );
					value1_orig = tsdata1.getData ();
					flag1 = tsdata1.getDataFlag().trim();
					tsdata2 = ts2.getDataPoint ( date );
					value2_orig = tsdata2.getData ();
					flag2 = tsdata2.getDataFlag().trim();
					if ( Precision != null ) {
						// Need to round.  For now do
						// with strings, which handles
						// the rounding...
						value1 =StringUtil.atod(
							StringUtil.formatString(
							value1_orig,"%."+
							Precision_int +"f"));
						value2 =StringUtil.atod(
							StringUtil.formatString(
							value2_orig,"%."+
							Precision_int +"f"));
					}
					else {	value1 = value1_orig;
						value2 = value2_orig;
					}
					// Count of data points that are
					// checked...
					++totalcount;
					is_diff = false;	// Initialize
					if (	ts1.isDataMissing(value1_orig)&&
						!ts2.isDataMissing(
						value2_orig)) {
						Message.printStatus ( 2,
							routine,
							loc1 + " has different"+
							" data on " + date +
							" TS1 = missing " +
							flag1 +
							" TS2 = " +
							StringUtil.formatString(
							value2,value_format) +
							" " + flag2 );
						// Indicate as missing at all
						// levels...
						is_diff = true;
						for (	it = 0;
							it < Tolerance_count;
							it++ ) {
							++diffcount[it];
						}
						if ( CreateDiffTS_boolean ) {
							// ts2 has value so make that
							// the difference...
							diffts.setDataValue ( date, value2 );
						}
					}
					else if(!ts1.isDataMissing(
						value1_orig) &&
						ts2.isDataMissing(value2_orig)){
						Message.printStatus ( 2,
							routine,
							loc1 + " has different"+
							" data on " + date +
							" TS1 = " +
							StringUtil.formatString(
							value1,value_format) +
							" " + flag1 +
							" TS2 = missing " +
							flag2);
						// Indicate as missing at all
						// levels...
						is_diff = true;
						for (	it = 0;
							it < Tolerance_count;
							it++ ) {
							++diffcount[it];
						}
						if ( CreateDiffTS_boolean ) {
							// ts1 has value so make that
							// the difference (negative)...
							diffts.setDataValue ( date, -value2 );
						}
					}
					else if(ts1.isDataMissing(value1_orig)&&
						ts2.isDataMissing(value2_orig)){
						// Both missing so continue...
						continue;
					}
					else {	// Analyze differences for each
						// tolerance...
						diff = value2 - value1;
						diffabs = Math.abs(diff);
						if ( CreateDiffTS_boolean ) {
							// Set difference (regardless of
							// tolerance).
							diffts.setDataValue ( date, diff );
						}
						if ( diffabs > diffmaxabs ) {
							diffmaxabs = diffabs;
							diffmax = diff;
							diffmax_DateTime =
							new DateTime ( date );
						}
						for (	it = 0;
							it < Tolerance_count;
							it++ ) {
							if (	diffabs >
							Tolerance_double[it]){
							// Report the
							// difference...
							Message.printStatus ( 2,
							routine,
							loc1 + " (tolerance=" +
							Tolerance_double[it] +
							") has difference " +
							"TS2-TS1 of " +
							StringUtil.formatString(
							diff,value_format) +
							" on " + date +
							" TS2 = " +
							StringUtil.formatString(
							value2,value_format) +
							" " + flag1 +
							" TS1 = " +
							StringUtil.formatString(
							value1,value_format) +
							" " + flag2 );
							difftotal[it] += diff;
							difftotalabs[it] +=
								diffabs;
							++diffcount[it];
							is_diff = true;
							}
						}
					}
					if ( is_diff && (DiffFlag != null) ) {
						// Append to the data flag...
						tsdata=ts1.getDataPoint (date);
						ts1.setDataValue ( date,
							value1_orig,
							tsdata.getDataFlag().
							trim() + DiffFlag, 1 );
						tsdata=ts2.getDataPoint (date);
						ts2.setDataValue ( date,
							value2_orig,
							tsdata.getDataFlag().
							trim() + DiffFlag, 1 );
					}
				}
				is_diff = false;
				for ( it = 0; it < Tolerance_count; it++ ) {
					if ( diffcount[it] > 0 ) {
						is_diff = true;
						diffabsavg[it] =
						difftotalabs[it]/diffcount[it];
						diffavg[it] =
						difftotal[it]/diffcount[it];
						Message.printStatus ( 2,
						routine, loc1 + " (tolerance=" +
						Tolerance_double[it] +
						") has " + diffcount[it] +
						" differences out of " +
						totalcount +
						" values." );
						Message.printStatus ( 2,
						routine, loc1 +
						" Average " +
						"difference (tolerance=" +
						Tolerance_double[it] + ")= " +
						StringUtil.formatString(
						diffavg[it],"%.6f") );
						Message.printStatus ( 2,
						routine, loc1 +
						" Average absolute " +
						"difference (tolerance=" +
						Tolerance_double[it] + ")= " +
						StringUtil.formatString(
						diffabsavg[it],"%.6f") );
					}
				}
				if ( is_diff ) {
					Message.printStatus ( 2, routine, loc1 +
					" maximum difference = " +
					StringUtil.formatString(
					diffmax, value_format) +" earliest on "+
					diffmax_DateTime );
					if ( WarnIfDifferent_boolean ) {
						message = "Time series for " +
						ts1.getIdentifier() +
						" have differences.";
						Message.printWarning (
						warning_level,
						MessageUtil.formatMessageTag(
						command_tag,++warning_count),
						routine, message );
					}
					++tsdiff_count;
				}
				else {	Message.printStatus ( 2, routine, loc1 +
					" had no differences." );
				}
				// Save information for the report...
				compare_tslist.addElement ( ts1 );
				compare_numvalues.addElement (
					new Integer ( totalcount ) );
				compare_diffcount.addElement ( diffcount );
				compare_diffabsavg.addElement ( diffabsavg );
				compare_diffavg.addElement ( diffavg );
				if ( is_diff ) {
					compare_diffmax.addElement (
					new Double(diffmax) );
					compare_diffmaxdate.addElement (
					new DateTime(diffmax_DateTime) );
				}
				else {	compare_diffmax.addElement (
					new Double(0.0) );
					compare_diffmaxdate.addElement (
					null );
				}
			}
		}
		Message.printStatus ( 2, routine,
			"" + tsdiff_count + " of " + size +
			" time series had differences." );
		// else print a warning and throw an exception below.

		// Print a summary of the comparison...

		size = compare_tslist.size();
		StringBuffer b = new StringBuffer();  // Full report
		StringBuffer b2 = new StringBuffer();	// Only differences
		int location_length = 8;	// Enough for heading
		int datatype_length = 9;	// Enough for heading
		TS ts = null;
		// Figure out the length for some of the string columns...
		for ( int i = 0; i < size; i++ ) {
			ts = (TS)compare_tslist.elementAt(i);
			if (	ts.getIdentifier().getLocation().length() >
				location_length ) {
				location_length =
				ts.getIdentifier().getLocation().length();
			}
			if ( MatchDataType_boolean ) {
				if (	ts.getIdentifier().getType().length() >
					datatype_length ) {
					datatype_length =
					ts.getIdentifier().getType().length();
				}
			}
		}
		String location_format = "%-14.14s";	// Default max
		if ( location_length < 14 ) {
			location_format = "%-" + location_length + "." +
				location_length + "s";
		}
		String datatype_format = "%-14.14s";	// Default max
		if ( datatype_length < 14 ) {
			datatype_format = "%-" + datatype_length + "." +
				datatype_length + "s";
		}
		String int_format = "%7d";
		String double_format = "%10.2f";
		String double_blank = "          ";
		String date_format = "%10.10s";
		// Print the headings...
		String nl = System.getProperty ( "line.separator" );
		b.append ( "|" +
			StringUtil.formatString( "Location", location_format) );
		b2.append ( "|" +
			StringUtil.formatString( "Location", location_format) );
		if ( MatchDataType_boolean ) {
			b.append ( "|" +
			StringUtil.formatString( "Data Type", datatype_format));
			b2.append ( "|" +
			StringUtil.formatString( "Data Type", datatype_format));
		}
		b.append ( "| #Val  " );
		b2.append ( "| #Val  " );
		for ( j = 0; j < Tolerance_count; j++ ) {
			b.append ( "|#D>" + StringUtil.formatString(
				Tolerance_tokens.elementAt(j),"%-4.4s") );
			b2.append ( "|#D>" + StringUtil.formatString(
					Tolerance_tokens.elementAt(j),"%-4.4s") );
			b.append ( "|AbsAvgDiff" );
			b2.append ( "|AbsAvgDiff" );
			b.append ( "| AvgDiff  " );
			b2.append ( "| AvgDiff  " );
		}
		b.append ( "| MaxDiff  " );
		b2.append ( "| MaxDiff  " );
		b.append ( "| MaxDate  " );
		b2.append ( "| MaxDate  " );
		b.append ( "|" + nl );
		b2.append ( "|" + nl );
		int [] diffcount_array;
		double [] diffabsavg_array;
		double [] diffavg_array;
		for ( int i = 0; i < size; i++ ) {
			ts = (TS)compare_tslist.elementAt(i);
			is_diff = false;
			// Check for difference here so that differency-only
			// summary report can be completed...
			diffcount_array = (int[])compare_diffcount.elementAt(i);
			for ( j = 0; j < Tolerance_count; j++ ) {
				if ( diffcount_array[j] > 0 ) {
					is_diff = true;
				}
			}
			// Always put in the location...
			b.append ( "|" +
				StringUtil.formatString(
				ts.getIdentifier().getLocation(),
				location_format) );
			if ( is_diff ) {
				b2.append ( "|" +
					StringUtil.formatString(
					ts.getIdentifier().getLocation(),
					location_format) );
			}
			if ( MatchDataType_boolean ) {
				b.append ( "|" +
					StringUtil.formatString(
					ts.getIdentifier().getType(),
					datatype_format) );
				if ( is_diff ) {
					b2.append ( "|" +
						StringUtil.formatString(
						ts.getIdentifier().getType(),
						datatype_format) );
				}
			}
			b.append ( "|" +
				StringUtil.formatString(
				((Integer)compare_numvalues.elementAt(i)).
				intValue(),
				int_format) );
			if ( is_diff ) {
				b2.append ( "|" +
					StringUtil.formatString(
					((Integer)compare_numvalues.elementAt(i)).
					intValue(),
					int_format) );
			}
			diffabsavg_array=
				(double[])compare_diffabsavg.elementAt(i);
			diffavg_array=(double[])compare_diffavg.elementAt(i);
			for ( j = 0; j < Tolerance_count; j++ ) {
				b.append ( "|" +
					StringUtil.formatString(
					diffcount_array[j], int_format) );
				if ( is_diff ) {
					b2.append ( "|" +
							StringUtil.formatString(
							diffcount_array[j], int_format) );
				}
				if ( diffcount_array[j] > 0 ) {
					b.append ( "|" +
					StringUtil.formatString(
					diffabsavg_array[j], double_format) );
					b.append ( "|" +
					StringUtil.formatString(
					diffavg_array[j], double_format) );
					if ( is_diff ) {
						b2.append ( "|" +
								StringUtil.formatString(
								diffabsavg_array[j], double_format) );
						b2.append ( "|" +
								StringUtil.formatString(
								diffavg_array[j], double_format) );
					}
				}
				else {	b.append ( "|" + double_blank );
					b.append ( "|" + double_blank );
					if ( is_diff ) {
						b2.append ( "|" + double_blank );
						b2.append ( "|" + double_blank );
					}
				}
			}
			if ( is_diff ) {
				b.append ( "|" +
					StringUtil.formatString(
					((Double)compare_diffmax.elementAt(i)).
					doubleValue(),
					double_format) );
				b2.append ( "|" +
						StringUtil.formatString(
						((Double)compare_diffmax.elementAt(i)).
						doubleValue(),
						double_format) );
				date = (DateTime)
					compare_diffmaxdate.elementAt(i);
				if ( date == null ) {
					b.append ( "|" +
						StringUtil.formatString( "",
							date_format) );
					b2.append ( "|" +
							StringUtil.formatString( "",
								date_format) );
				}
				else {	b.append ( "|" +
						StringUtil.formatString(
						((DateTime)compare_diffmaxdate.
						elementAt(i)).
						toString(),
						date_format) );
						b2.append ( "|" +
						StringUtil.formatString(
						((DateTime)compare_diffmaxdate.
						elementAt(i)).
						toString(),
						date_format) );
				}
			}
			else {	b.append ( "|" + double_blank );
				b.append ( "|" +
					StringUtil.formatString( "",
					date_format) );
			}
			b.append ( "|" + nl );	// Last border
			if ( is_diff ) {
				b2.append ( "|" + nl );	// Last border
			}
		}
		Message.printStatus ( 2, "",
				"Summary of differences only (differences are second time " +
				"series minus first time series):" + nl +
				b2.toString() );
		Message.printStatus ( 2, "", "" );
		Message.printStatus ( 2, "",
			"Summary of differences for all time series (differences are second time " +
			"series minus first time series):" + nl +
			b.toString() );

		if ( CreateDiffTS_boolean ) {
			try { Object o = processor.getPropContents ( "TSResultsList" );
				tslist = (Vector)o;
			}
			catch ( Exception e ) {
				// Not fatal, but of use to developers.
				message = "Error requesting TSResultsList from processor - no data.";
				Message.printWarning(3, routine, message );
			}
			// Add the difference time series...
			int diffsize = diffts_Vector.size();
			for ( int i = 0; i < diffsize; i++ ) {
				tslist.addElement ( (TS)diffts_Vector.elementAt(i) );
			}
			try {	processor.setPropContents ( "TSResultsList", tslist );
			}
			catch ( Exception e ) {
				message = "Error setting time series list appended with difference time series.";
				Message.printWarning ( warning_level, 
						MessageUtil.formatMessageTag(command_tag, ++warning_count),
						routine, message );
				Message.printWarning ( 3, routine, e );
				throw new CommandException ( message );
			}
			Message.printStatus ( 2, "",
				"Difference time series (second time series minus first) have been appended to results." );
		}
	}
	catch ( Exception e ) {
		message = "Error comparing time series.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		Message.printWarning ( 3, routine, e );
		throw new CommandException ( message );
	}
	if ( WarnIfDifferent_boolean && (tsdiff_count > 0) ) {
		message = "" + tsdiff_count + " of " + size +
		" time series had differences.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag( command_tag,++warning_count),
		routine, message );
		throw new CommandException ( message );
	}
	if ( WarnIfSame_boolean && (tsdiff_count == 0) ) {
		message = "All " + size + " time series are the same.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag( command_tag,++warning_count),
		routine, message );
		throw new CommandException ( message );
	}
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String MatchLocation = props.getValue("MatchLocation");
	String MatchDataType = props.getValue("MatchDataType");
	String Precision = props.getValue("Precision");
	String Tolerance = props.getValue("Tolerance");
	String AnalysisStart = props.getValue("AnalysisStart");
	String AnalysisEnd = props.getValue("AnalysisEnd");
	String DiffFlag = props.getValue("DiffFlag");
	String CreateDiffTS = props.getValue("CreateDiffTS");
	String WarnIfDifferent = props.getValue("WarnIfDifferent");
	String WarnIfSame = props.getValue("WarnIfSame");
	StringBuffer b = new StringBuffer ();
	if ( (MatchLocation != null) && (MatchLocation.length() > 0) ) {
		b.append ( "MatchLocation=" + MatchLocation );
	}
	if ( (MatchDataType != null) && (MatchDataType.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "MatchDataType=" + MatchDataType );
	}
	if ( (Precision != null) && (Precision.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Precision=" + Precision );
	}
	if ( (Tolerance != null) && (Tolerance.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Tolerance=\"" + Tolerance + "\"" );
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
	if ( (DiffFlag != null) && (DiffFlag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DiffFlag=\"" + DiffFlag + "\"" );
	}
	if ( (CreateDiffTS != null) && (CreateDiffTS.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "CreateDiffTS=" + CreateDiffTS );
	}
	if ( (WarnIfDifferent != null) && (WarnIfDifferent.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "WarnIfDifferent=" + WarnIfDifferent );
	}
	if ( (WarnIfSame != null) && (WarnIfSame.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "WarnIfSame=" + WarnIfSame );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
