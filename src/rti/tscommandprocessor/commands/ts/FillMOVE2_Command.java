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

import javax.swing.JFrame;

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
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeUtil;

/**
<p>
This class initializes, checks, and runs the FillMOVE2() command.
</p>
*/
public class FillMOVE2_Command extends AbstractCommand implements Command
{

/**
Protected data members shared with the dialog and other related classes.
*/
protected final String _Linear = "Linear";	// obsolete... use None

/**
Constructor.
*/
public FillMOVE2_Command ()
{	super();
	setCommandName ( "FillMOVE2" );
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
	// TODO SAM 2006-04-13 can this be enabled?
	//String AnalysisMonth = parameters.getValue ( "AnalysisMonth" );
	String Transformation = parameters.getValue ( "Transformation" );
	// TODO SAM 2006-04-13 can this be enabled?
	//String Intercept = parameters.getValue ( "Intercept" );
	String DependentAnalysisStart = parameters.getValue ("DependentAnalysisStart" );
	String DependentAnalysisEnd = parameters.getValue ("DependentAnalysisEnd" );
	String IndependentAnalysisStart = parameters.getValue ("IndependentAnalysisStart" );
	String IndependentAnalysisEnd = parameters.getValue ("IndependentAnalysisEnd" );
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
        ") must be blank, " + NumberOfEquationsType.ONE_EQUATION +
        " (default), or " + NumberOfEquationsType.MONTHLY_EQUATIONS + ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the number of equations as blank, " + NumberOfEquationsType.ONE_EQUATION +
                " (default), or " + NumberOfEquationsType.MONTHLY_EQUATIONS + ".") );
	}
	/* TODO SAM 2006-04-13 Can this be enabled?
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
			Transformation = "" + DataTransformationType.NONE;
		}
		if ( !Transformation.equalsIgnoreCase("" + DataTransformationType.LOG) &&
		    !Transformation.equalsIgnoreCase("" + DataTransformationType.NONE) ) {
            message = "The transformation (" + Transformation +
            ") must be " + DataTransformationType.LOG + " or " +
            DataTransformationType.NONE + " (default).";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the transformation as " + DataTransformationType.LOG + " or " +
                    DataTransformationType.NONE + " (default).") );
		}
	}
	/* TODO SAM 2006-04-13 Can this be enabled?
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
            message = "The dependent analysis start date/time \"" + DependentAnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	if (	(DependentAnalysisEnd != null) &&
		!DependentAnalysisEnd.equals("") &&
		!DependentAnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse(DependentAnalysisEnd);
		}
		catch ( Exception e ) {
            message = "The dependent analysis end date/time \"" + DependentAnalysisEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
		}
	}
	if (	(IndependentAnalysisStart != null) &&
		!IndependentAnalysisStart.equals("") &&
		!IndependentAnalysisStart.equalsIgnoreCase("OutputStart") ) {
		try {	DateTime.parse(IndependentAnalysisStart);
		}
		catch ( Exception e ) {
            message = "The independent analysis start date/time \"" + DependentAnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	if (	(IndependentAnalysisEnd != null) &&
		!IndependentAnalysisEnd.equals("") &&
		!IndependentAnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse(IndependentAnalysisEnd);
		}
		catch ( Exception e ) {
            message = "The independent analysis end date/time \"" + DependentAnalysisEnd + "\" is not a valid date/time.";
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
    //valid_Vector.add ( "AnalysisMonth" );
    //valid_Vector.add ( "Transformation" );
    //valid_Vector.add ( "Intercept" );
    valid_Vector.add ( "DependentAnalysisStart" );
    valid_Vector.add ( "DependentAnalysisEnd" );
    valid_Vector.add ( "IndependentAnalysisStart" );
    valid_Vector.add ( "IndependentAnalysisEnd" );
    valid_Vector.add ( "FillStart" );
    valid_Vector.add ( "FillEnd" );
    valid_Vector.add ( "FillFlag" );
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
	return (new FillMOVE2_JDialog ( parent, this )).ok();
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
	String routine = "fillMOVE2_Command.parseCommand", message;

    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
		// TODO SAM 2006-04-16 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax (not free-format parameters)...
    	List v = StringUtil.breakStringList(command_string,
			"(),\t", StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		// v[0] is the command name
		if ( ntokens < 11 ) {
			message = "Syntax error in \"" + command_string + "\".  Not enough parameters.";
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
		int ic = 1;   // Skip command name
		TSID = ((String)v.get(ic++)).trim();
		IndependentTSID = ((String)v.get(ic++)).trim();
		NumberOfEquations=((String)v.get(ic++)).trim();
		Transformation = ((String)v.get(ic++)).trim();
		DependentAnalysisStart = ((String)v.get(ic++)).trim();
		if ( DependentAnalysisStart.equals("*") ) {
			DependentAnalysisStart = "";// Current default
		}
		DependentAnalysisEnd =((String)v.get(ic++)).trim();
		if ( DependentAnalysisEnd.equals("*") ) {
			DependentAnalysisEnd = "";// Current default
		}
		IndependentAnalysisStart = ((String)v.get(ic++)).trim();
		if ( IndependentAnalysisStart.equals("*") ) {
			IndependentAnalysisStart = "";// Current default
		}
		IndependentAnalysisEnd =((String)v.get(ic++)).trim();
		if ( IndependentAnalysisEnd.equals("*") ) {
			IndependentAnalysisEnd = "";// Current default
		}
		FillStart = ((String)v.get(ic++)).trim();
		if ( FillStart.equals("*") ) {
			FillStart = "";	// Current default.
		}
		FillEnd = ((String)v.get(ic++)).trim();
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
		/* TODO SAM 2006-04-16
			Evaluate whether this can be enabled
		if ( AnalysisMonth.length() > 0 ) {
			_parameters.set ( "AnalysisMonth", AnalysisMonth );
		}
		*/
		if ( Transformation.length() > 0 ) {
			parameters.set ( "Transformation", Transformation );
		}
		/* TODO SAM 2006-04-16
			Evaluate whether this can be enabled
		if ( Intercept.length() > 0 ) {
			_parameters.set ( "Intercept", Intercept );
		}
		*/
		if ( DependentAnalysisStart.length() > 0 ) {
			parameters.set ( "DependentAnalysisStart",DependentAnalysisStart );
		}
		if ( DependentAnalysisEnd.length() > 0 ) {
			parameters.set ( "DependentAnalysisEnd",DependentAnalysisEnd );
		}
		if ( IndependentAnalysisStart.length() > 0 ) {
			parameters.set ( "IndependentAnalysisStart",IndependentAnalysisStart );
		}
		if ( IndependentAnalysisEnd.length() > 0 ) {
			parameters.set ( "IndependentAnalysisEnd",IndependentAnalysisEnd );
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
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
		message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSID +
		"\") from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
         status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TS = bean_PropList.getContents ( "TS");
	TS ts_to_fill = null;
	if ( o_TS == null ) {
		message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\") from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the dependent TSID matches a time series." ) );
	}
	else {
		ts_to_fill = (TS)o_TS;
	}
	
	if ( ts_to_fill == null ) {
        message = "Unable to find dependent time series \"" + TSID+"\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count),
			routine, message);
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
	try { bean =
		processor.processRequest( "GetTimeSeriesForTSID", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + IndependentTSID +
		"\") from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	bean_PropList = bean.getResultsPropList();
	o_TS = bean_PropList.getContents ( "TS");
	TS ts_independent = null;
	if ( o_TS == null ) {
		message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + IndependentTSID +
		"\") from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the independent TSID matches a time series." ) );
	}
	else {
		ts_independent = (TS)o_TS;
	}
	
	if ( ts_independent == null ) {
        message = "Unable to find independent time series \"" + IndependentTSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count),
			routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the independent TSID matches a time series." ) );
	}

	// Determine parameters for TSUtil.fillRegress()...

	String NumberOfEquations = parameters.getValue ( "NumberOfEquations" );
	NumberOfEquationsType numberOfEquations = NumberOfEquationsType.ONE_EQUATION; // Default
	if ( (NumberOfEquations != null) && !NumberOfEquations.equals("") ) {
		numberOfEquations = NumberOfEquationsType.valueOfIgnoreCase(NumberOfEquations);
	}

	/* TODO SAM 2006-04-16 Evaluate whether this can be enabled.
	String AnalysisMonth =_parameters.getValue("AnalysisMonth");
	if ( AnalysisMonth != null ) {
		props.set ( "AnalysisMonth", AnalysisMonth );
	}
	*/

	String Transformation = parameters.getValue("Transformation");
	DataTransformationType transformation = DataTransformationType.NONE; // Default
	if ( (Transformation != null) && !Transformation.equals("") ) {
	    if ( Transformation.equalsIgnoreCase(_Linear)) { // Linear is obsolete
	        Transformation = "" + DataTransformationType.NONE;
	    }
		transformation = DataTransformationType.valueOfIgnoreCase(Transformation);
	}

	// Set the analysis/fill periods...

	String DependentAnalysisStart =	parameters.getValue("DependentAnalysisStart");
	DateTime dependentAnalysisStart = null;
	if ( TimeUtil.isDateTime(DependentAnalysisStart) ) {
		try {
		    dependentAnalysisStart = DateTime.parse(DependentAnalysisStart);
		}
		catch ( Exception e ) {
		    // Should not happen
		}
	}
	String DependentAnalysisEnd = parameters.getValue("DependentAnalysisEnd");
	DateTime dependentAnalysisEnd = null;
	if ( TimeUtil.isDateTime(DependentAnalysisEnd) ) {
	    try {
	        dependentAnalysisEnd = DateTime.parse(DependentAnalysisEnd);
	    }
        catch ( Exception e ) {
            // Should not happen
        }
	}

	String IndependentAnalysisStart = parameters.getValue("IndependentAnalysisStart");
	DateTime independentAnalysisStart = null;
	if ( TimeUtil.isDateTime(IndependentAnalysisStart) ) {
	    try {
	        independentAnalysisStart = DateTime.parse(IndependentAnalysisStart);
	    }
        catch ( Exception e ) {
            // Should not happen
        }
	}
	String IndependentAnalysisEnd = parameters.getValue("IndependentAnalysisEnd");
	DateTime independentAnalysisEnd = null;
	if ( TimeUtil.isDateTime(IndependentAnalysisEnd) ) {
	    try {
	        independentAnalysisEnd = DateTime.parse(IndependentAnalysisEnd);
	    }
        catch ( Exception e ) {
            // Should not happen
        }
	}

	String FillStart = parameters.getValue("FillStart");
	String FillEnd = parameters.getValue("FillEnd");
	String FillFlag = parameters.getValue("FillFlag");

	/* TODO SAM 2006-04-16 Evaluate whether this can be enabled
	String Intercept = _parameters.getValue("Intercept");
	if ( (Intercept != null) && !Intercept.equals("") ) {
		props.set ( "Intercept="+ Intercept );
	}
	*/

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
			try { bean =
				processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting FillStart DateTime(DateTime=" +
				FillStart + ") from processor.";
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
				FillEnd + ") from processor.";
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
	    TSRegression regress_results = TSUtil.fillRegress ( ts_to_fill, ts_independent,
            RegressionType.MOVE2, numberOfEquations,
            null, //Double intercept,
            null, //int [] analysisMonths,
            transformation,
            dependentAnalysisStart, dependentAnalysisEnd,
            independentAnalysisStart, independentAnalysisEnd,
            FillStart_DateTime, FillEnd_DateTime,
            FillFlag,
            null );// descriptionString
        if ( numberOfEquations == NumberOfEquationsType.ONE_EQUATION ) {
            if ( regress_results.getN1() == 0 ) {
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
                if ( regress_results.getN1(i) == 0 ) {
                    message = "Number of overlapping points in month " + i + "(" +
                    TimeUtil.monthAbbreviation(i) + ") is 0.";
                    Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify that time series have overlapping periods." ) );
                }
            }
        }
		// Print the results to the log file...
		if ( regress_results != null ) {
			Message.printStatus ( 2, routine,"Analysis results are..." );
			Message.printStatus ( 2, routine,regress_results.toString() );
			// TODO SAM 2005-05-05 Need to call setPropContents on the TSCommandProcessor?
		}
		else {
            message = "Unable to compute regression.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that time series have overlapping periods." ) );
			throw new CommandException ( message );
		}
	}
	catch ( Exception e ) {
        message = "Unexpected error filling with regression for \""+toString() +"\" (" + e + ").";
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
	String IndependentAnalysisStart = props.getValue("IndependentAnalysisStart");
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
