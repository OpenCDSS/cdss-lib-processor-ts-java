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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSRegression;
import RTi.TS.TSUtil;

import RTi.Util.Math.DataTransformationType;
import RTi.Util.Math.NumberOfEquationsType;
import RTi.Util.Math.RegressionType;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeUtil;

/**
This class initializes, checks, and runs the FillRegression() command.
*/
public class FillRegression_Command extends AbstractCommand implements Command
{

/**
Protected data members shared with the dialog and other related classes.
*/
protected final String _Linear = "Linear";	// obsolete... use None

/**
Constructor.
*/
public FillRegression_Command ()
{	super();
	setCommandName ( "FillRegression" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String TSID = parameters.getValue ( "TSID" );
	String IndependentTSID = parameters.getValue ( "IndependentTSID" );
	String NumberOfEquations = parameters.getValue ( "NumberOfEquations" );
	String AnalysisMonth = parameters.getValue ( "AnalysisMonth" );
	String Transformation = parameters.getValue ( "Transformation" );
	String LEZeroLogValue = parameters.getValue ( "LEZeroLogValue" );
	String Intercept = parameters.getValue ( "Intercept" );
    String MinimumDataCount = parameters.getValue ( "MinimumDataCount" );
    String MinimumR = parameters.getValue ( "MinimumR" );
    String ConfidenceInterval = parameters.getValue ( "ConfidenceInterval" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	String FillFlag = parameters.getValue ( "FillFlag" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
	if ( (TSID == null) || TSID.length() == 0 ) {
        message = "The dependent time series identifier must be specified.";
		warning = "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the dependent time series identifier." ) );
	}
	if ( (IndependentTSID == null) || (IndependentTSID.length() == 0) ) {
		message = "The independent time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the independent time series identifier." ) );
	}
	if ( (TSID != null) && (IndependentTSID != null) &&	TSID.equalsIgnoreCase(IndependentTSID) ) {
        message = "The time series to fill \"" + TSID + "\" is the same\n"+
		"as the independent time series \"" + IndependentTSID + "\".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the independent time series identifier." ) );
	}
	if ( (NumberOfEquations != null) &&
		!NumberOfEquations.equalsIgnoreCase(""+NumberOfEquationsType.ONE_EQUATION) &&
		!NumberOfEquations.equalsIgnoreCase(""+NumberOfEquationsType.MONTHLY_EQUATIONS)) {
        message = "The number of equations (" + NumberOfEquations +
        ") must be " + NumberOfEquationsType.ONE_EQUATION + " (default) or " +
        NumberOfEquationsType.MONTHLY_EQUATIONS + ".";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the number of equations as " + NumberOfEquationsType.ONE_EQUATION +
                " (default) or " + NumberOfEquationsType.MONTHLY_EQUATIONS + ".") );
	}
	if ( AnalysisMonth != null ) {
		if ( !StringUtil.isInteger(AnalysisMonth) ) {
            message = "The analysis month: \"" + AnalysisMonth + "\" is not an integer.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an integer 1-12 for the analysis month.") );
		}
		else if((StringUtil.atoi(AnalysisMonth) < 1) ||	(StringUtil.atoi(AnalysisMonth) > 12) ) {
            message = "The analysis month: \"" + AnalysisMonth + "\" must be in the range 1 to 12.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an integer 1-12 for the analysis month.") );
		}
	}
	if ( Transformation != null ) {
		if ( Transformation.equalsIgnoreCase(_Linear) ) {
			// Convert old to new...
			Transformation = "" + DataTransformationType.NONE;
		}
		if ( !Transformation.equalsIgnoreCase(""+DataTransformationType.LOG) &&
		    !Transformation.equalsIgnoreCase(""+DataTransformationType.NONE) ) {
            message = "The transformation (" + Transformation +
            ") must be  " + DataTransformationType.LOG + " or " + DataTransformationType.NONE + " (default).";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the transformation as " + DataTransformationType.LOG + " or " +
                    DataTransformationType.NONE + " (default).") );
		}
	}
	if ( (Intercept != null) && !Intercept.equals("") ) {
		if ( !StringUtil.isDouble(Intercept) ) {
            message = "The intercept: \"" + Intercept + "\" is not a number.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the intercept as a zero or blank.") );
		}
		else if ( StringUtil.atod(Intercept) != 0.0 ) {
            message = "The intercept: \"" + Intercept + "\" is not zero (only 0 or blank is " +
            "currently supported).";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the intercept as a zero or blank.") );
		}
		if ( (Transformation != null) && Transformation.equals(""+DataTransformationType.LOG)){
            message = "The intercept (" + Intercept + ") currently cannot be specified with log transformation.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the intercept as blank or change the transformation to None.") );
		}
	}
    // Make sure LEZeroLogValue, if given is a valid double.
    if ( (LEZeroLogValue != null) && !LEZeroLogValue.equals("") && !StringUtil.isDouble( LEZeroLogValue ) ) {
        message = "The <= zero log value (" + LEZeroLogValue + ") is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the <= log value as a number." ) );
    }
	if ( (AnalysisStart != null) && !AnalysisStart.equals("") &&
		!AnalysisStart.equalsIgnoreCase("OutputStart") ) {
		try {	DateTime.parse(AnalysisStart);
		}
		catch ( Exception e ) {
            message = "The analysis start date/time \"" + AnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	
	// Make sure MinimumDataCount was given and is a valid integer
    if ( (MinimumDataCount != null) && !MinimumDataCount.equals("") && !StringUtil.isInteger(MinimumDataCount)) {
        message = "Minimum data count (" + MinimumDataCount + ") is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum data count as an integer." ) );
    }
        
    // Make sure MinimumR, if given is a valid double. If not given set to the default 0.5.
    if ( (MinimumR != null) && !MinimumR.equals("") && !StringUtil.isDouble( MinimumR ) ) {
        message = "The minimum R value (" + MinimumR + ") is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum R value as a number." ) );
    }
    
    // Make sure confidence level, if given is a valid number
    if ( (ConfidenceInterval != null) && !ConfidenceInterval.equals("") ) {
        if ( !StringUtil.isDouble(ConfidenceInterval) ) { 
            message = "The confidence level (" + ConfidenceInterval + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the confidence interval as a percent > 0 and < 100 (e.g., 95)." ) );
        }
        else {
            double cl = Double.parseDouble(ConfidenceInterval);
            if ( (cl <= 0.0) || (cl >= 100.0) ) { 
                message = "The confidence level (" + ConfidenceInterval + ") is invalid.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the confidence interval as a percent > 0 and < 100 (e.g., 95)." ) );
            }
        }
    }
    
	if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse( AnalysisEnd);
		}
		catch ( Exception e ) {
            message = "The analysis end date/time \"" + AnalysisEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
		}
	}
	if ( (FillStart != null) && !FillStart.equals("") && !FillStart.equalsIgnoreCase("OutputStart")){
		try {	DateTime.parse(FillStart);
		}
		catch ( Exception e ) {
            message = "The fill start date/time \"" + FillStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	if ( (FillEnd != null) && !FillEnd.equals("") && !FillEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse( FillEnd);
		}
		catch ( Exception e ) {
            message = "The fill end date/time \"" + FillStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
		}
	}
	if ( (FillFlag != null) && (FillFlag.length() > 1) ) {
        message = "The fill flag must be 1 character long.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a 1-character fill flag or blank to not use a flag." ) );
	}
    
    // Check for invalid parameters...
	List valid_Vector = new Vector();
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "IndependentTSID" );
    valid_Vector.add ( "NumberOfEquations" );
    valid_Vector.add ( "AnalysisMonth" );
    valid_Vector.add ( "Transformation" );
    valid_Vector.add ( "Intercept" );
    valid_Vector.add ( "LEZeroLogValue" );
    valid_Vector.add ( "MinimumDataCount" );
    valid_Vector.add ( "MinimumR" );
    valid_Vector.add ( "ConfidenceInterval" );
    valid_Vector.add ( "AnalysisStart" );
    valid_Vector.add ( "AnalysisEnd" );
    valid_Vector.add ( "FillStart" );
    valid_Vector.add ( "FillEnd" );
    valid_Vector.add ( "FillFlag" );
    valid_Vector.add ( "TableID" );
    valid_Vector.add ( "TableTSIDColumn" );
    valid_Vector.add ( "TableTSIDFormat" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );
    
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
    List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	return (new FillRegression_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports very-old syntax (separate commands for different combinations of
parameters), newer syntax (one command but fixed-parameter list), and current
syntax (free-format parameters).
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "FillRegression_Command.parseCommand", message;

    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
		// TODO SAM 2005-04-29 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new
		// syntax.
		//
		// Old syntax (not free-format parameters or with only
		// the Intercept= syntax)...
		// Parse up front.  Don't parse with spaces because a
		// TEMPTS may be present.
    	List v = StringUtil.breakStringList(command_string,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS |
			StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		if ( ntokens < 5 ) {
			message = "Syntax error in \"" + command_string +
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
		TSID = ((String)v.get(ic++)).trim();
		IndependentTSID = ((String)v.get(ic++)).trim();
		NumberOfEquations=((String)v.get(ic++)).trim();
		Transformation = ((String)v.get(ic++)).trim();
		int icmax = ic + 1;
		if ( ntokens >= icmax ) {
			AnalysisStart = ((String)v.get(ic++)).trim();
			if ( AnalysisStart.equals("*") ) {
				AnalysisStart = "";// Current default
			}
			if ( ntokens >= (icmax + 1) ) {
				AnalysisEnd =((String)v.get(ic++)).trim();
			}
			if ( AnalysisEnd.equals("*") ) {
				AnalysisEnd = "";// Current default
			}
		}
		// All others have the fill period...
		if ( ntokens >= icmax ) {
			FillStart = ((String)v.get(ic++)).trim();
		}
		if ( FillStart.equals("*") ) {
			FillStart = "";	// Current default.
		}
		if ( ntokens >= (icmax + 1) ) {
			FillEnd = ((String)v.get(ic++)).trim();
		}
		if ( FillEnd.equals("*") ) {
			FillEnd = "";	// Current default.
		}

		// Check for new-style properties (only Intercept=)...

		String token, token0;
		List v2;
		for ( ic = 0; ic < ntokens; ic++ ) {
			// Check for an '=' in the token...
			token = (String)v.get(ic);
			if ( token.indexOf('=') < 0 ) {
				continue;
			}
			v2 = StringUtil.breakStringList ( token, "=", 0 );
			if ( v2.size() < 2 ) {
				continue;
			}
			token0 = ((String)v2.get(0)).trim();
			if ( token0.equalsIgnoreCase("Intercept") ) {
				Intercept = ((String)v2.get(1)).trim();
			}
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
		if ( AnalysisMonth.length() > 0 ) {
			parameters.set ( "AnalysisMonth", AnalysisMonth );
		}
		if ( Transformation.length() > 0 ) {
			parameters.set ( "Transformation", Transformation );
		}
		if ( Intercept.length() > 0 ) {
			parameters.set ( "Intercept", Intercept );
		}
		if ( AnalysisStart.length() > 0 ) {
			parameters.set ( "AnalysisStart", AnalysisStart );
		}
		if ( AnalysisEnd.length() > 0 ) {
			parameters.set ( "AnalysisEnd", AnalysisEnd );
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
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "FillRegression_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Warning level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters ();
	CommandProcessor processor = getCommandProcessor();
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);

	String TSID = parameters.getValue ( "TSID" );
	
	PropList request_params = new PropList ( "" );
	request_params.set ( "CommandTag", command_tag );
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try { bean =
		processor.processRequest( "GetTimeSeriesForTSID", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\") from processor.";
		Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TS = bean_PropList.getContents ( "TS");
	TS tsToFill = null;
	if ( o_TS == null ) {
		message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\") from processor.";
		Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the dependent TSID matches a time series." ) );
	}
	else {
		tsToFill = (TS)o_TS;
	}
	
	if ( tsToFill == null ) {
        message = "Unable to find dependent time series \"" + TSID+"\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message);
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the dependent TSID matches a time series." ) );
	}
	// The independent identifier may or may not have TEMPTS at the front
	// but is handled by getTimeSeries...
	String IndependentTSID = parameters.getValue ( "IndependentTSID" );
	
	request_params = new PropList ( "" );
	request_params.set ( "CommandTag", command_tag );
	request_params.set ( "TSID", IndependentTSID );
	bean = null;
	try {
	    bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + IndependentTSID +
		"\") from processor.";
		Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
	}
	bean_PropList = bean.getResultsPropList();
	o_TS = bean_PropList.getContents ( "TS");
	TS tsIndependent = null;
	if ( o_TS == null ) {
		message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + IndependentTSID + "\") from processor.";
		Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the independent TSID matches a time series." ) );
	}
	else {
		tsIndependent = (TS)o_TS;
	}
	
	if ( tsIndependent == null ) {
        message = "Unable to find independent time series \"" + IndependentTSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the independent TSID matches a time series." ) );
	}

	// Determine the fill properties for TSUtil.fillRegress()...

	String NumberOfEquations = parameters.getValue ( "NumberOfEquations" );
	NumberOfEquationsType numberOfEquations = NumberOfEquationsType.ONE_EQUATION; // Default
    if ( (NumberOfEquations != null) && !NumberOfEquations.equals("") ) {
        numberOfEquations = NumberOfEquationsType.valueOfIgnoreCase(NumberOfEquations);
    }

    // FIXME SAM 2009-08-30 Treat as list but dialog only allows single number
	String AnalysisMonth = parameters.getValue("AnalysisMonth");
	int [] analysisMonths = null;
	if ( (AnalysisMonth != null) && !AnalysisMonth.equals("") ) {
		analysisMonths = StringUtil.parseIntegerSequenceArray(AnalysisMonth, ", ", StringUtil.DELIM_SKIP_BLANKS);
	}

    String Transformation = parameters.getValue("Transformation");
    DataTransformationType transformation = DataTransformationType.NONE; // Default
    if ( (Transformation != null) && !Transformation.equals("") ) {
        if ( Transformation.equalsIgnoreCase(_Linear)) { // Linear is obsolete
            Transformation = "" + DataTransformationType.NONE;
        }
        transformation = DataTransformationType.valueOfIgnoreCase(Transformation);
    }

	// Set the analysis/fill periods...

	String AnalysisStart = parameters.getValue("AnalysisStart");
    DateTime dependentAnalysisStart = null;
    if ( TimeUtil.isDateTime(AnalysisStart) ) {
        try {
            dependentAnalysisStart = DateTime.parse(AnalysisStart);
        }
        catch ( Exception e ) {
            // Should not happen
        }
    }
	String AnalysisEnd = parameters.getValue("AnalysisEnd");
    DateTime dependentAnalysisEnd = null;
    if ( TimeUtil.isDateTime(AnalysisEnd) ) {
        try {
            dependentAnalysisEnd = DateTime.parse(AnalysisEnd);
        }
        catch ( Exception e ) {
            // Should not happen
        }
    }

	String FillStart = parameters.getValue("FillStart");
	String FillEnd = parameters.getValue("FillEnd");
	String FillFlag = parameters.getValue("FillFlag");

	String Intercept = parameters.getValue("Intercept");
	Double intercept = null;
	if ( (Intercept != null) && !Intercept.equals("") ) {
		intercept = Double.parseDouble(Intercept);
	}
	
    String LEZeroLogValue = parameters.getValue("LEZeroLogValue");
    Double leZeroLogValue = null;
    if ( (LEZeroLogValue != null) && !LEZeroLogValue.equals("") ) {
        leZeroLogValue = Double.parseDouble(LEZeroLogValue);
    }
    String MinimumDataCount = parameters.getValue("MinimumDataCount");
    Integer minimumDataCount = null;
    if ( (MinimumDataCount != null) && !MinimumDataCount.equals("") ) {
        minimumDataCount = Integer.parseInt(MinimumDataCount);
    }
    String MinimumR = parameters.getValue("MinimumR");
    Double minimumR = null;
    if ( (MinimumR != null) && !MinimumR.equals("") ) {
        minimumR = Double.parseDouble(MinimumR);
    }
    String ConfidenceInterval = parameters.getValue("ConfidenceInterval");
    Double confidenceInterval = null;
    if ( (ConfidenceInterval != null) && !ConfidenceInterval.equals("") ) {
        confidenceInterval = Double.parseDouble(ConfidenceInterval);
    }
	
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    
    DataTable table = null;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be updated
        request_params = new PropList ( "" );
        request_params.set ( "TableID", TableID );
        try {
            bean = processor.processRequest( "GetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table == null ) {
            message = "Unable to find table to process using TableID=\"" + TableID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a table exists with the requested ID." ) );
        }
        else {
            table = (DataTable)o_Table;
        }
    }

	if ( warning_count > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
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
			try {
			    bean = processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting FillStart DateTime(DateTime=" +	FillStart + ") from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
				throw new InvalidCommandParameterException ( message );
			}

			bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for FillStart DateTime(DateTime=" +
				FillStart +	") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
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
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
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
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
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
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
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
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
			throw new InvalidCommandParameterException ( message );
		}
	
	// Fill the dependent time series...
	// This will result in the time series in the original data being modified...
	try {
	    TSRegression regressionResults = TSUtil.fillRegress ( 
			tsToFill, tsIndependent,
			null, // No previously computed TSRegression object
			RegressionType.OLS_REGRESSION, numberOfEquations,
            intercept,
            analysisMonths,
            transformation,
            leZeroLogValue,
            minimumDataCount,
            minimumR,
            confidenceInterval,
            dependentAnalysisStart, dependentAnalysisEnd,
            null, //independentAnalysisStart - used with MOVE2
            null, //independentAnalysisEnd - used with MOVE2
            FillStart_DateTime, FillEnd_DateTime,
            FillFlag,
            null );// use default descriptionString
	    if ( numberOfEquations == NumberOfEquationsType.ONE_EQUATION ) {
	        if ( regressionResults.getN1() == 0 ) {
	            message = "Number of overlapping points is 0.";
	            Message.printWarning ( warning_level,
	            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
	            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Verify that time series have overlapping periods." ) );
	        }
	    }
	    else {
	        for ( int i = 1; i <= 12; i++ ) {
	            if ( regressionResults.getN1(i) == 0 ) {
	                message = "Number of overlapping points in month " + i + " (" +
                    TimeUtil.monthAbbreviation(i) + ") is 0.";
	                Message.printWarning ( warning_level,
	                MessageUtil.formatMessageTag(
	                command_tag,++warning_count), routine, message );
	                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify that time series have overlapping periods." ) );
	            }
	        }
	    }
	    // Print the results to the log file and optionally an output table...
	    if ( regressionResults != null ) {
			Message.printStatus ( 2, routine, "Fill results are..." );
			Message.printStatus ( 2, routine, regressionResults.toString() );
            // Now set in the table
		    if ( (TableID != null) && !TableID.equals("") ) {
    			saveStatisticsToTable ( tsToFill, regressionResults, table, TableID,
    			    TableTSIDColumn, TableTSIDFormat, numberOfEquations );
		    }
		}
		else {
            message = "Unable to compute regression.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that time series have overlapping periods." ) );
			throw new CommandException ( message );
		}
	}
	catch ( Exception e ) {
		message = "Unexpected error performing regression for \""+toString() +"\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that time series are of same interval and overlap - " +
                        " also check the log file for details." ) );
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
Save the statistics to the table.
*/
private void saveStatisticsToTable ( TS ts, TSRegression regressionResults, DataTable table,
    String TableID, String tableTSIDColumnName, String TableTSIDFormat, NumberOfEquationsType numberOfEquations )
throws Exception
{   String routine = getClass().getName() + ".saveStatisticsToTable";
    // Verify that the TSID table columns are available for dependent and independent time series
    String tableTSIDColumnNameIndependent = tableTSIDColumnName + "_Independent";
    int tableTSIDColumnNumber = -1;
    int tableTSIDColumnNumberIndependent = -1;
    // If the column name does not exist, add it to the table
    try {
        tableTSIDColumnNumber = table.getFieldIndex(tableTSIDColumnName);
    }
    catch ( Exception e2 ) {
        // Automatically add to the table, initialize with null (not nonValue)
        table.addField(new TableField(TableField.DATA_TYPE_STRING,tableTSIDColumnName,-1,-1), null );
        // Get the corresponding column number for row-edits below
        tableTSIDColumnNumber = table.getFieldIndex(tableTSIDColumnName);
    }
    try {
        tableTSIDColumnNumberIndependent = table.getFieldIndex(tableTSIDColumnNameIndependent);
    }
    catch ( Exception e2 ) {
        // Automatically add to the table, initialize with null (not nonValue)
        table.addField(new TableField(TableField.DATA_TYPE_STRING,tableTSIDColumnNameIndependent,-1,-1), null );
        // Get the corresponding column number for row-edits below
        tableTSIDColumnNumberIndependent = table.getFieldIndex(tableTSIDColumnNameIndependent);
    }
    // Loop through the statistics, creating table column names if necessary
    // Do this first so that all columns are fully defined.  Then process the row values below.
    int numEquations = 1;
    if ( numberOfEquations == NumberOfEquationsType.MONTHLY_EQUATIONS ) {
        numEquations = 12;
    }
    // List in a reasonable order
    String [] statistics =
        { "NX", "MeanX", "SX", "N1", "MeanX1", "SX1", "N2", "MeanX2", "SX2",
            "MeanY1", "SY1", "MeanY", "SY", "a", "b", "R", "MeanY1est", "SY1est" };
    int countStatisticTotal = statistics.length*numEquations; // The total number of statistics columns to add
    String [] statisticColumnNames = new String[countStatisticTotal]; // names in table
    int [] statisticColumnNumbers = new int[countStatisticTotal]; // columns in table
    Double [] statisticValueDouble = new Double[countStatisticTotal];
    Integer [] statisticValueInteger = new Integer[countStatisticTotal];
    int countStatistic = -1; // The count of statistics added (0-index) for array access
    for ( int iEquation = 1; iEquation <= numEquations; iEquation++ ) {
        for ( int iStatistic = 0; iStatistic < statistics.length; iStatistic++ ) {
            // Set statistics to null (one will be set below).
            ++countStatistic;
            statisticValueDouble[countStatistic] = null;
            statisticValueInteger[countStatistic] = null;
            // Column name for the statistic...
            if ( numEquations == 1 ) {
                statisticColumnNames[countStatistic] = statistics[iStatistic];
                if ( statistics[iStatistic].equals("a") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getA());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("b") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getB());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("MeanX") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getMeanX());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("MeanY") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getMeanY());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("MeanY1") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getMeanY1());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("MeanY1est") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getMeanY1Estimated());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("NX") ) {
                    statisticValueInteger[countStatistic] = new Integer(regressionResults.getN1() +
                        regressionResults.getN2());
                }
                else if ( statistics[iStatistic].equals("N1") ) {
                    statisticValueInteger[countStatistic] = new Integer(regressionResults.getN1());
                }
                else if ( statistics[iStatistic].equals("N2") ) {
                    statisticValueInteger[countStatistic] = new Integer(regressionResults.getN2());
                }
                else if ( statistics[iStatistic].equals("R") ) {
                    try {
                        statisticValueDouble[countStatistic] =
                            new Double(regressionResults.getCorrelationCoefficient());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SX") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationX());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SX1") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationX1());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SX2") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationX2());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SY") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationY());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SY1") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationY1());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SY1est") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationY1Estimated());
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
            }
            else {
                // Monthly so add subscript
                statisticColumnNames[countStatistic] = statistics[iStatistic] + "_" + iEquation;
                if ( statistics[iStatistic].equals("a") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getA(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("b") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getB(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("MeanX") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getMeanX(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("MeanX1") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getMeanX1(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("MeanX2") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getMeanX2(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("MeanY") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getMeanY(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("MeanY1") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getMeanY1(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("MeanY1est") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getMeanY1Estimated(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("NX") ) {
                    statisticValueInteger[countStatistic] = new Integer(regressionResults.getN1(iEquation) +
                        regressionResults.getN2(iEquation));
                }
                else if ( statistics[iStatistic].equals("N1") ) {
                    statisticValueInteger[countStatistic] = new Integer(regressionResults.getN1(iEquation));
                }
                else if ( statistics[iStatistic].equals("N2") ) {
                    statisticValueInteger[countStatistic] = new Integer(regressionResults.getN2(iEquation));
                }
                else if ( statistics[iStatistic].equals("R") ) {
                    try {
                        statisticValueDouble[countStatistic] =
                            new Double(regressionResults.getCorrelationCoefficient(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SX") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationX(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SX1") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationX1(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SX2") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationX2(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SY") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationY(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SY1") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationY1(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
                else if ( statistics[iStatistic].equals("SY1est") ) {
                    try {
                        statisticValueDouble[countStatistic] = new Double(regressionResults.getStandardDeviationY1Estimated(iEquation));
                    }
                    catch ( Exception e ) {
                        // No value computed.  Leave as null for output.
                    }
                }
            }
            // If the column name does not exist, add it to the table
            try {
                statisticColumnNumbers[countStatistic] = table.getFieldIndex(statisticColumnNames[countStatistic]);
            }
            catch ( Exception e2 ) {
                // Automatically add to the table, initialize with null (not nonValue)
                // TODO SAM 2010-12-16 Evaluate field width and precision
                if ( statisticValueInteger != null ) {
                    table.addField(new TableField(
                        TableField.DATA_TYPE_INT,statisticColumnNames[countStatistic],-1,-1), null );
                }
                else if ( statisticValueDouble != null ) {
                    table.addField(new TableField(
                         TableField.DATA_TYPE_DOUBLE,statisticColumnNames[countStatistic],10,4), null );
                }
                // Get the corresponding column number for row-edits below
                statisticColumnNumbers[countStatistic] =
                    table.getFieldIndex(statisticColumnNames[countStatistic]);
            }
        }
    }
    // By here the table should have the proper columns
    // Now loop through again and process the row for the dependent and independent time series
    // First format the dependent and independent time series identifiers for to match the table...
    String tableTSIDDependent = null;
    if ( (TableTSIDFormat != null) && !TableTSIDFormat.equals("") ) {
        // Format the TSID using the specified format
        tableTSIDDependent = ts.formatLegend ( TableTSIDFormat );
    }
    else {
        // Use the alias if available and then the TSID
        tableTSIDDependent = ts.getAlias();
        if ( (tableTSIDDependent == null) || tableTSIDDependent.equals("") ) {
            tableTSIDDependent = ts.getIdentifierString();
        }
    }
    String tableTSIDIndependent = null;
    if ( (TableTSIDFormat != null) && !TableTSIDFormat.equals("") ) {
        // Format the TSID using the specified format
        tableTSIDIndependent = regressionResults.getIndependentTS().formatLegend ( TableTSIDFormat );
    }
    else {
        // Use the alias if available and then the TSID
        tableTSIDDependent = regressionResults.getIndependentTS().getAlias();
        if ( (tableTSIDDependent == null) || tableTSIDDependent.equals("") ) {
            tableTSIDIndependent = regressionResults.getIndependentTS().getIdentifierString();
        }
    }
    // Next, find the record that has the dependent and independent identifiers...
    // Find the record that matches the dependent and independent identifiers (should only be one)
    List<String> tableColumnNames = new Vector(); // The dependent and independent TSID column names
    tableColumnNames.add ( tableTSIDColumnName );
    tableColumnNames.add ( tableTSIDColumnNameIndependent );
    List<String> tableColumnValues = new Vector(); // The dependent and independent TSID values
    tableColumnValues.add ( tableTSIDDependent );
    tableColumnValues.add ( tableTSIDIndependent );
    List<TableRecord> recList = table.getRecords ( tableColumnNames, tableColumnValues );
    TableRecord rec = null;
    //Message.printStatus(2,routine,"Searched column\"" + TableTSIDColumn + "\" for \"" +
    //    tableTSIDDependent + "\" ... found " + rec );
    if ( recList.size() == 0 ) {
        // No record in the table so add one with TSID column values and blank statistic values...
        table.addRecord(rec=table.emptyRecord());
        rec.setFieldValue(tableTSIDColumnNumber, tableTSIDDependent);
        rec.setFieldValue(tableTSIDColumnNumberIndependent, tableTSIDIndependent);
    }
    else if ( recList.size() == 1 ) {
        // Record already exists so use it
        rec = recList.get(0);
    }
    else {
        // For some reason more than one record was matched.  Insert into the first row
        rec = recList.get(0);
        Message.printWarning(3,routine, "Columns " + tableTSIDColumnName + "=" + tableTSIDDependent + ", " +
            tableTSIDColumnNameIndependent + "=" + tableTSIDIndependent +
            " has > 1 matching records - inserting into first matching row." );
    }
    // Finally loop through the statistics and insert into the row matched above
    countStatistic = -1;
    for ( int iEquation = 0; iEquation < numEquations; iEquation++ ) {
        for ( int iStatistic = 0; iStatistic < statistics.length; iStatistic++ ) {
            // Set the value...
            ++countStatistic;
            if ( statisticValueDouble[countStatistic] != null ) {
                rec.setFieldValue(statisticColumnNumbers[countStatistic],
                     statisticValueDouble[countStatistic]);
            }
            if ( statisticValueInteger[countStatistic] != null ) {
                rec.setFieldValue(statisticColumnNumbers[countStatistic],
                     statisticValueInteger[countStatistic]);
            }
        }
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
    String LEZeroLogValue = props.getValue ( "LEZeroLogValue" );
    String MinimumDataCount = props.getValue ( "MinimumDataCount" );
    String MinimumR = props.getValue ( "MinimumR" );
    String ConfidenceInterval = props.getValue ( "ConfidenceInterval" );
	String AnalysisStart = props.getValue("AnalysisStart");
	String AnalysisEnd = props.getValue("AnalysisEnd");
	String FillStart = props.getValue("FillStart");
	String FillEnd = props.getValue("FillEnd");
	String FillFlag = props.getValue("FillFlag");
    String TableID = props.getValue ( "TableID" );
    String TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = props.getValue ( "TableTSIDFormat" );
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
    if ( LEZeroLogValue != null && LEZeroLogValue.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "LEZeroLogValue=" + LEZeroLogValue);
    }
	if ( (Intercept != null) && (Intercept.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Intercept=" + Intercept );
	}
    if ( MinimumDataCount != null && MinimumDataCount.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "MinimumDataCount=" + MinimumDataCount);
    }
    if ( MinimumR != null && MinimumR.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "MinimumR="+ MinimumR );
    }
    if ( ConfidenceInterval != null && ConfidenceInterval.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "ConfidenceInterval=" + ConfidenceInterval );
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
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (TableTSIDColumn != null) && (TableTSIDColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableTSIDColumn=\"" + TableTSIDColumn + "\"" );
    }
    if ( (TableTSIDFormat != null) && (TableTSIDFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableTSIDFormat=\"" + TableTSIDFormat + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}