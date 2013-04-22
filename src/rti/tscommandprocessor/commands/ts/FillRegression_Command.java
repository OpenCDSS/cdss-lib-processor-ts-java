package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSRegression;
import RTi.TS.TSRegressionAnalysis;
import RTi.TS.TSUtil;
import RTi.TS.TSUtil_FillRegression;

import RTi.Util.Math.DataTransformationType;
import RTi.Util.Math.NumberOfEquationsType;
import RTi.Util.Math.RegressionType;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
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
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeUtil;

/**
This class initializes, checks, and runs the FillRegression() command.
*/
public class FillRegression_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Protected data members shared with the dialog and other related classes.
*/
protected final String _Linear = "Linear";	// obsolete... use DataTransformationType.NONE

/**
Possible data values for Fill parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
The table that is (optionally) created with statistics information.
*/
private DataTable __table = null;

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
    String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
    String MinimumR = parameters.getValue ( "MinimumR" );
    String ConfidenceInterval = parameters.getValue ( "ConfidenceInterval" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String Fill = parameters.getValue ( "Fill" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
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
    // Make sure LEZeroLogValue, if given is a valid double.
    if ( (LEZeroLogValue != null) && !LEZeroLogValue.equals("") && !StringUtil.isDouble( LEZeroLogValue ) ) {
        message = "The <= zero log value (" + LEZeroLogValue + ") is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the <= log value as a number." ) );
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
	if ( (AnalysisStart != null) && !AnalysisStart.equals("") &&
		!AnalysisStart.equalsIgnoreCase("OutputStart") ) {
		try {
		    DateTime.parse(AnalysisStart);
		}
		catch ( Exception e ) {
            message = "The analysis start date/time \"" + AnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
    if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse( AnalysisEnd);
        }
        catch ( Exception e ) {
            message = "The analysis end date/time \"" + AnalysisEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time or OutputEnd." ) );
        }
    }
	
	// Make sure MinimumSampleSize was given and is a valid integer
    if ( (MinimumSampleSize != null) && !MinimumSampleSize.equals("") && !StringUtil.isInteger(MinimumSampleSize)) {
        message = "The minimum sample size (" + MinimumSampleSize + ") is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum sample size as an integer." ) );
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
    
    if ( (Fill != null) && !Fill.equals("") ) {
        if ( !Fill.equalsIgnoreCase(_False) && !Fill.equalsIgnoreCase(_True) ) {
            message = "The Fill (" + Fill +
            ") parameter must be  " + _False + " or " + _True + " (default).";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the Fill parameter as " + _False + " or " + _True + " (default).") );
        }
    }
	if ( (FillStart != null) && !FillStart.equals("") && !FillStart.equalsIgnoreCase("OutputStart")){
		try {
		    DateTime.parse(FillStart);
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
		try {
		    DateTime.parse( FillEnd);
		}
		catch ( Exception e ) {
            message = "The fill end date/time \"" + FillStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time or OutputEnd." ) );
		}
	}
    
    // Check for invalid parameters...
	List<String> valid_Vector = new Vector();
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "IndependentTSID" );
    valid_Vector.add ( "NumberOfEquations" );
    valid_Vector.add ( "AnalysisMonth" );
    valid_Vector.add ( "Transformation" );
    valid_Vector.add ( "LEZeroLogValue" );
    valid_Vector.add ( "Intercept" );
    valid_Vector.add ( "MinimumSampleSize" );
    valid_Vector.add ( "MinimumR" );
    valid_Vector.add ( "ConfidenceInterval" );
    valid_Vector.add ( "AnalysisStart" );
    valid_Vector.add ( "AnalysisEnd" );
    valid_Vector.add ( "Fill" );
    valid_Vector.add ( "FillStart" );
    valid_Vector.add ( "FillEnd" );
    valid_Vector.add ( "FillFlag" );
    valid_Vector.add ( "FillFlagDesc" );
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
    List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
	return (new FillRegression_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable table = getDiscoveryTable();
    List v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector();
        v.add ( table );
    }
    return v;
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
    	List<String> v = StringUtil.breakStringList(command_string,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS | StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		if ( ntokens < 5 ) {
			message = "Syntax error in \"" + command_string + "\".  Not enough tokens.";
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
		TSID = v.get(ic++).trim();
		IndependentTSID = v.get(ic++).trim();
		NumberOfEquations = v.get(ic++).trim();
		Transformation = v.get(ic++).trim();
		int icmax = ic + 1;
		if ( ntokens >= icmax ) {
			AnalysisStart = v.get(ic++).trim();
			if ( AnalysisStart.equals("*") ) {
				AnalysisStart = "";// Current default
			}
			if ( ntokens >= (icmax + 1) ) {
				AnalysisEnd = v.get(ic++).trim();
			}
			if ( AnalysisEnd.equals("*") ) {
				AnalysisEnd = "";// Current default
			}
		}
		// All others have the fill period...
		if ( ntokens >= icmax ) {
			FillStart = v.get(ic++).trim();
		}
		if ( FillStart.equals("*") ) {
			FillStart = "";	// Current default.
		}
		if ( ntokens >= (icmax + 1) ) {
			FillEnd = v.get(ic++).trim();
		}
		if ( FillEnd.equals("*") ) {
			FillEnd = ""; // Current default.
		}

		// Check for new-style properties (only Intercept=)...

		String token, token0;
		List<String> v2;
		for ( ic = 0; ic < ntokens; ic++ ) {
			// Check for an '=' in the token...
			token = v.get(ic);
			if ( token.indexOf('=') < 0 ) {
				continue;
			}
			v2 = StringUtil.breakStringList ( token, "=", 0 );
			if ( v2.size() < 2 ) {
				continue;
			}
			token0 = v2.get(0).trim();
			if ( token0.equalsIgnoreCase("Intercept") ) {
				Intercept = v2.get(1).trim();
			}
		}
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
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
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
    status.clearLog(commandPhase);

	String TSID = parameters.getValue ( "TSID" );
	
	PropList request_params = new PropList ( "" );
    TS tsToFill = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        String TSList = "" + TSListType.LAST_MATCHING_TSID;
        List<TS> tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, null );
        if ( (tslist != null) && (tslist.size() > 0) ) {
            tsToFill = tslist.get(0);
        }
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
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
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
    	}
    	PropList bean_PropList = bean.getResultsPropList();
    	Object o_TS = bean_PropList.getContents ( "TS");
    	if ( o_TS == null ) {
    		message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\") from processor.";
    		Message.printWarning(log_level,
    			MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the dependent TSID matches a time series." ) );
    	}
    	else {
    		tsToFill = (TS)o_TS;
    	}
    }
	
	if ( tsToFill == null ) {
        message = "Unable to find dependent time series \"" + TSID+"\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message);
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the dependent TSID matches a time series." ) );
	}
	// Get the independent time series
	String IndependentTSID = parameters.getValue ( "IndependentTSID" );
    TS tsIndependent = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        String TSList = "" + TSListType.LAST_MATCHING_TSID;
        List<TS> tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, null );
        if ( (tslist != null) && (tslist.size() > 0) ) {
            tsIndependent = tslist.get(0);
        }
    }
    if ( commandPhase == CommandPhaseType.RUN ) {
    	request_params = new PropList ( "" );
    	request_params.set ( "CommandTag", command_tag );
    	request_params.set ( "TSID", IndependentTSID );
        CommandProcessorRequestResultsBean bean = null;
    	try {
    	    bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
    	}
    	catch ( Exception e ) {
    		message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + IndependentTSID +
    		"\") from processor.";
    		Message.printWarning(log_level,
    			MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
    	}
    	PropList bean_PropList = bean.getResultsPropList();
    	Object o_TS = bean_PropList.getContents ( "TS");
    	if ( o_TS == null ) {
    		message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + IndependentTSID + "\") from processor.";
    		Message.printWarning(log_level,
    			MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the independent TSID matches a time series." ) );
    	}
    	else {
    		tsIndependent = (TS)o_TS;
    	}
    }
	
	if ( tsIndependent == null ) {
        message = "Unable to find independent time series \"" + IndependentTSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase,
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
            Message.printWarning(3,routine,e);
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
            Message.printWarning(3,routine,e);
        }
    }

    String Fill = parameters.getValue ( "Fill" );
    boolean Fill_boolean = true;
    if ( (Fill != null) && Fill.equalsIgnoreCase("False") ) {
        Fill_boolean = false;
    }
	String FillStart = parameters.getValue("FillStart");
	String FillEnd = parameters.getValue("FillEnd");
	String FillFlag = parameters.getValue("FillFlag");
	String FillFlagDesc = parameters.getValue("FillFlagDesc");

	String Intercept = parameters.getValue("Intercept");
	Double forcedIntercept = null;
	if ( (Intercept != null) && !Intercept.equals("") ) {
		forcedIntercept = Double.parseDouble(Intercept);
	}
	
    String LEZeroLogValue = parameters.getValue("LEZeroLogValue");
    Double leZeroLogValue = null;
    if ( (LEZeroLogValue != null) && !LEZeroLogValue.equals("") ) {
        leZeroLogValue = Double.parseDouble(LEZeroLogValue);
    }
    String MinimumSampleSize = parameters.getValue("MinimumSampleSize");
    Integer minimumSampleSize = null;
    if ( (MinimumSampleSize != null) && !MinimumSampleSize.equals("") ) {
        minimumSampleSize = Integer.parseInt(MinimumSampleSize);
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
    boolean newTable = false; // true if a new table had to be created
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be updated
        request_params = new PropList ( "" );
        request_params.set ( "TableID", TableID );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "GetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table == null ) {
            Message.printStatus ( 2, routine, "Unable to find table to process using TableID=\"" + TableID +
                "\" - creating empty table." );
            // Create an empty table matching the identifier
            table = new DataTable( new Vector() );
            table.setTableID ( TableID );
            newTable = true;
        }
        else {
            newTable = false;
            table = (DataTable)o_Table;
        }
    }
    if ( newTable ) {
        if ( commandPhase == CommandPhaseType.RUN ) {
            // Set the table in the processor (its contents will be modified below)...
            
            request_params = new PropList ( "" );
            request_params.setUsingObject ( "Table", table );
            try {
                processor.processRequest( "SetTable", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting SetTable(Table=...) from processor.";
                Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
            }
        }
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create an empty table and set the ID
            table = new DataTable();
            table.setTableID ( TableID );
            setDiscoveryTable ( table );
        }
    }

	if ( warning_count > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog ( commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
	}

	// Call the code that is used by both the old and new version...

	// Figure out the dates to use for the analysis...
	DateTime FillStart_DateTime = null;
	DateTime FillEnd_DateTime = null;
    
	try {
		if ( (FillStart != null) && !FillStart.equals("")  ) {
			request_params = new PropList ( "" );
			request_params.set ( "DateTime", FillStart );
			CommandProcessorRequestResultsBean bean = null;
			try {
			    bean = processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting FillStart DateTime(DateTime=" +	FillStart + ") from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
				throw new InvalidCommandParameterException ( message );
			}

			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for FillStart DateTime(DateTime=" +
				FillStart +	") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog ( commandPhase,
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
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
			throw new InvalidCommandParameterException ( message );
		}
		
		try {
		if ( (FillEnd != null) && !FillEnd.equals("") ) {
			request_params = new PropList ( "" );
			request_params.set ( "DateTime", FillEnd );
			CommandProcessorRequestResultsBean bean = null;
			try { bean =
				processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting FillEnd DateTime(DateTime=" +
				FillEnd + "\" from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
				throw new InvalidCommandParameterException ( message );
			}

			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for FillStart DateTime(DateTime=" +
				FillStart +	"\") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog ( commandPhase,
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
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
			throw new InvalidCommandParameterException ( message );
		}
	
	// Fill the dependent time series...
	// This will result in the time series in the original data being modified...
	try {
	    if ( commandPhase == CommandPhaseType.RUN ) {
	        boolean doLegacy = false;
	        if ( doLegacy ) {
        	    TSRegression regressionResults = TSUtil.fillRegress ( 
        			tsToFill, tsIndependent,
        			null, // No previously computed TSRegression object
        			RegressionType.OLS_REGRESSION,
        			numberOfEquations,
                    forcedIntercept,
                    analysisMonths,
                    transformation,
                    leZeroLogValue,
                    minimumSampleSize,
                    minimumR,
                    confidenceInterval,
                    dependentAnalysisStart, dependentAnalysisEnd,
                    null, //independentAnalysisStart - used with MOVE2 but not OLS_REGRESSION
                    null, //independentAnalysisEnd - used with MOVE2 but not OLS_REGRESSION
                    FillStart_DateTime, FillEnd_DateTime,
                    FillFlag,
                    FillFlagDesc,
                    Fill_boolean );
                if ( numberOfEquations == NumberOfEquationsType.ONE_EQUATION ) {
                    if ( regressionResults.getN1() == 0 ) {
                        message = "Number of overlapping points is 0.";
                        Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        status.addToLog ( commandPhase,
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
                            status.addToLog ( commandPhase,
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
                    // TODO SAM 2012-01-15 Not enabled for legacy code
                    //if ( (TableID != null) && !TableID.equals("") ) {
                    //    tsufr.saveStatisticsToTable ( tsToFill, regressionResults, table,
                    //        TableTSIDColumn, TableTSIDFormat, numberOfEquations );
                    //}
                }
                else {
                    message = "Unable to compute regression.";
                    Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that time series have overlapping periods." ) );
                    throw new CommandException ( message );
                }
	        }
	        else {
	            // New code that is more modular and consistent with FillMixedStation().
	            // First analyze the time series
	            TSUtil_FillRegression tsufr = new TSUtil_FillRegression (
                    tsToFill, tsIndependent,
                    RegressionType.OLS_REGRESSION,
                    numberOfEquations,
                    analysisMonths,
                    transformation,
                    leZeroLogValue,
                    forcedIntercept,
                    dependentAnalysisStart, dependentAnalysisEnd,
                    null, //independentAnalysisStart - used with MOVE2 but not OLS_REGRESSION
                    null, //independentAnalysisEnd - used with MOVE2 but not OLS_REGRESSION
                    minimumSampleSize,
                    minimumR,
                    confidenceInterval,
                    FillStart_DateTime, FillEnd_DateTime,
                    FillFlag,
                    FillFlagDesc,
                    Fill_boolean, // Whether to fill or just analyze for filling (but don't fill) 
                    null ); // No user-specified description
	            // Fill the missing values in the dependent
	            tsufr.fillRegression ();
	            TSRegressionAnalysis ra = tsufr.getTSRegressionAnalysis();
	            List<String> problems = tsufr.getProblems();
	            for ( int iprob = 0; iprob < problems.size(); iprob++ ) {
	                message = problems.get(iprob);
	                Message.printWarning ( warning_level,
	                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
	                // No recommendation since it is a user-defined check
	                // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
	                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING, message, "" ) );
	            }
                if ( numberOfEquations == NumberOfEquationsType.ONE_EQUATION ) {
                    if ( ra.getTSRegressionData().getSingleEquationRegressionData().getN1() == 0 ) {
                        message = "Number of overlapping points is 0.";
                        Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Verify that time series have overlapping periods." ) );
                    }
                }
                else {
                    for ( int iMonth = 1; iMonth <= 12; iMonth++ ) {
                        if ( ra.getTSRegressionData().getMonthlyEquationRegressionData(iMonth).getN1() == 0 ) {
                            message = "Number of overlapping points in month " + iMonth + " (" +
                            TimeUtil.monthAbbreviation(iMonth) + ") is 0.";
                            Message.printWarning ( warning_level,
                            MessageUtil.formatMessageTag(
                            command_tag,++warning_count), routine, message );
                            status.addToLog ( commandPhase,
                                new CommandLogRecord(CommandStatusType.WARNING,
                                    message, "Verify that time series have overlapping periods." ) );
                        }
                    }
                }
                // Print the results to the log file and optionally an output table...
                if ( ra != null ) {
                    // Now set in the table
                    if ( (TableID != null) && !TableID.equals("") ) {
                        tsufr.saveStatisticsToTable ( tsToFill, table,
                            TableTSIDColumn, TableTSIDFormat, RegressionType.OLS_REGRESSION,
                            numberOfEquations, transformation );
                    }
                }
                else {
                    message = "Unable to compute regression.";
                    Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that time series have overlapping periods." ) );
                    throw new CommandException ( message );
                }
	        }
	    }
	}
	catch ( Exception e ) {
		message = "Unexpected error performing regression for \""+toString() +"\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog ( commandPhase,
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
    
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __table = table;
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
    String LEZeroLogValue = props.getValue ( "LEZeroLogValue" );
	String Intercept = props.getValue("Intercept");
    String MinimumSampleSize = props.getValue ( "MinimumSampleSize" );
    String MinimumR = props.getValue ( "MinimumR" );
    String ConfidenceInterval = props.getValue ( "ConfidenceInterval" );
	String AnalysisStart = props.getValue("AnalysisStart");
	String AnalysisEnd = props.getValue("AnalysisEnd");
    String Fill = props.getValue ( "Fill" );
	String FillStart = props.getValue("FillStart");
	String FillEnd = props.getValue("FillEnd");
	String FillFlag = props.getValue("FillFlag");
	String FillFlagDesc = props.getValue("FillFlagDesc");
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
    if ( MinimumSampleSize != null && MinimumSampleSize.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "MinimumSampleSize=" + MinimumSampleSize);
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
    if ( (Fill != null) && (Fill.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Fill=" + Fill );
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
    if ( (FillFlagDesc != null) && (FillFlagDesc.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FillFlagDesc=\"" + FillFlagDesc + "\"" );
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