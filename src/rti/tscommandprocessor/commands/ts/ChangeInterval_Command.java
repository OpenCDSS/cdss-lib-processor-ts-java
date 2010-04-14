// ----------------------------------------------------------------------------
// changeInterval_Command - editor for TS X = changeInterval()
//
// TODO SAM 2005-02-12
//		In the future may also support changeInterval() to operate on
//		multiple time series.
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 2005-02-16	Steven A. Malers, RTi	Initial version, initialized from
//					normalize_JDialog().
// 2005-02-18	SAM, RTi		Comment out AllowMissingPercent - it
//					is causing problems in some of the
//					computations so re-evaluate later.
// 2005-03-14	SAM, RTi		Add OutputFillMethod and
//					HandleMissingInputHow parameters.
// 2005-05-24	Luiz Teixeira, RTi	Copied the original class 
//					changeInterval_JDialog() from TSTool and
//					started splitting the code into the new
//					changeInterval_JDialog() and
//					changeInterval_Command().
// 2005-05-26	Luiz Teixeira, RTi	Cleanup and documentation.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// ----------------------------------------------------------------------------
package rti.tscommandprocessor.commands.ts;

import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_CalculateTimeSeriesStatistic;
import RTi.TS.TSUtil_ChangeInterval;
import RTi.TS.TSUtil_ChangeInterval_HandleEndpointsHowType;
import RTi.TS.TSUtil_ChangeInterval_HandleMissingInputHowType;
import RTi.TS.TSUtil_ChangeInterval_OutputFillMethodType;

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
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeScaleType;
import RTi.Util.Time.YearType;

public class ChangeInterval_Command extends AbstractCommand implements Command
{

// Defines used by this class and its changeInterval_Dialog counterpart.
protected final String _Interpolate = "Interpolate";
protected final String _KeepMissing = "KeepMissing";
protected final String _Repeat = "Repeat";
protected final String _SetToZero = "SetToZero";
protected final String _IncludeFirstOnly = "IncludeFirstOnly";
protected final String _AverageEndpoints = "AverageEndpoints";

private final boolean __read_one = true;	// For now only enable the TS Alias notation.

/**
Command constructor.
*/
public ChangeInterval_Command ()
{	
	super();
	setCommandName ( "ChangeInterval" );
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
{	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Get the properties from the PropList parameters.
	String Alias = parameters.getValue( "Alias" );
	String TSID = parameters.getValue( "TSID" );
	String NewInterval = parameters.getValue( "NewInterval" );
	String OldTimeScale = parameters.getValue( "OldTimeScale" );
	String NewTimeScale = parameters.getValue( "NewTimeScale" );
    String Statistic = parameters.getValue ( "Statistic" );
	String OutputYearType = parameters.getValue ( "OutputYearType" );
	String Tolerance = parameters.getValue( "Tolerance" );
	String HandleEndpointsHow = parameters.getValue( "HandleEndpointsHow" );
	String AllowMissingCount = parameters.getValue("AllowMissingCount" );
	/* TODO SAM 2005-02-18 may enable later
	String AllowMissingPercent= parameters.getValue("AllowMissingPercent");
	*/
	String OutputFillMethod = parameters.getValue( "OutputFillMethod" );
	String HandleMissingInputHow = parameters.getValue( "HandleMissingInputHow" );

	// Alias must be specified.
	// TODO [LT 2005-05-24] How about the __read_one issue (see parseCommand() method)
	if ( Alias == null || Alias.length() == 0 ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series alias when defining the command."));
	}
	
	// TSID - TSID will always be set from the changeInterval_JDialog when
	// the OK button is pressed, but the user may edit the command without
	// using the changeInterval_JDialog editor and try to run it, so this
	// method should at least make sure the TSID property is given.
	// TODO [LT 2005-05-26] Better test may be put in place here, to make
	// sure the given TSID is actually a valid time series in the system.
	if ( TSID == null || TSID.length() == 0 ) {
        message = "The identifier for the time series to convert must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series identifier when defining the command."));
	}

	// Check if the alias for the new time series is the same as the 
	// alias used by one of the time series in memory.
	// If so print a warning...
	// TODO [LT 2005-05-26] This is used in all other command but it 
	// is not working here.  Why?	Temporarily using the alternative below.
	/*	Vector tsids = (Vector) getCommandProcessor().getPropContents ( "TSIDListNoInput" );
	if ( StringUtil.indexOf( tsids, Alias ) >= 0 ) {
		warning += "\nTime series alias \"" + Alias + "\" is already used above.";
	}
	 */		
	// Check if the alias for the new time series is the same as the 
	// alias used by the original time series.  If so print a warning...
	// TODO [LT 2005-05-26] Would this alternative be more appropriated?
	// Notice: The version above is the one used by the others commands.
	if ( (Alias != null) && (TSID != null) && TSID.equalsIgnoreCase( Alias ) ) {
        message = "The alias \"" + Alias
        + "\" for the new time series is equal to the alias of the original time series.";
		warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify different time series for input and output."));
	}
	
    TimeInterval newInterval = null;
	if ( NewInterval == null || (NewInterval.length() == 0) ) {
		message = "The new interval must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a new interval."));
	}
	else {
	    try {
	        newInterval = TimeInterval.parseInterval(NewInterval);
	    }
	    catch ( Exception e ) {
	        // Should not happen because choices are valid
	        message = "The new interval \"" + NewInterval + "\" is invalid.";
	        warning += "\n" + message;
	        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a new interval using the command editor."));
	    }
	}
	
	// OldTimeScale - OldTimeScale will always be set from the 
	// changeInterval_JDialog when the OK button is pressed, but the user
	// may edit the command without using the changeInterval_JDialog editor
	// and try to run it, so this method should at least make sure the 
	// OldTimeScale property is given.
	if ( OldTimeScale != null && OldTimeScale.length() == 0 ) {
        message = "The old time scale must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify an old time scale."));
	}
	TimeScaleType oldTimeScaleType = null;
	if ( OldTimeScale != null && !OldTimeScale.equals("") ) {
	    try {
	        oldTimeScaleType = TimeScaleType.valueOfIgnoreCase(OldTimeScale);
	    }
	    catch ( Exception e ) {
    		message = "The old time scale (" + OldTimeScale + ") is invalid.";
            warning += "\n" + message;
            StringBuffer b = new StringBuffer();
            List<TimeScaleType> values = TimeScaleType.getTimeScaleChoices();
            for ( TimeScaleType t : values ) {
                if ( b.length() > 0 ) {
                    b.append ( ", " );
                }
                b.append ( t.toString() );
            }
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Valid values are:  " + b.toString() + "."));
	    }
	}
	
	// NewTimeScale - NewTimeScale will always be set from the 
	// changeInterval_JDialog when the OK button is pressed, but the user
	// may edit the command without using the changeInterval_JDialog editor
	// and try to run it, so this method should at least make sure the 
	// NewTimeScale property is given.
	TimeScaleType newTimeScaleType = null;
	if ( (NewTimeScale != null) && NewTimeScale.length() == 0 ) {
        message = "The new time scale must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a new time scale."));
	}
	if ( (NewTimeScale != null) && !NewTimeScale.equals("") ) {
        try {
            newTimeScaleType = TimeScaleType.valueOfIgnoreCase(NewTimeScale);
        }
        catch ( Exception e ) {
            message = "The new time scale (" + NewTimeScale + ") is invalid.";
            warning += "\n" + message;
            StringBuffer b = new StringBuffer();
            List<TimeScaleType> values = TimeScaleType.getTimeScaleChoices();
            for ( TimeScaleType t : values ) {
                if ( b.length() > 0 ) {
                    b.append ( ", " );
                }
                b.append ( t.toString() );
            }
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Valid values are:  " + b.toString() + "."));
        }
	}
	
    if ( (Statistic != null) && !Statistic.equals("") && (oldTimeScaleType != TimeScaleType.INST)&&
        (newTimeScaleType != TimeScaleType.INST) ) {
        message = "The statistic is only valid when converting from time scale " + TimeScaleType.INST +
        " (small interval) to " + TimeScaleType.INST + " (big interval).";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Do not specify the statistic." ) );
    }
    else if ( (Statistic != null) && !Statistic.equals("") ) {
        // Make sure that the statistic is known in general
        boolean supported = false;
        TSStatisticType statisticType = null;
        try {
            statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
            supported = true;
        }
        catch ( Exception e ) {
            message = "The statistic (" + Statistic + ") is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported statistic using the command editor." ) );
        }
        
        // Make sure that it is in the supported list
        
        if ( supported ) {
            supported = false;
            List<TSStatisticType> statistics = TSUtil_CalculateTimeSeriesStatistic.getStatisticChoices();
            for ( TSStatisticType statistic : statistics ) {
                if ( statisticType == statistic ) {
                    supported = true;
                    break;
                }
            }
            if ( !supported ) {
                message = "The statistic (" + Statistic + ") is not supported by this command.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Select a supported statistic using the command editor." ) );
            }
        }
    }
	
    if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
        try {
            YearType.valueOfIgnoreCase(OutputYearType);
        }
        catch ( Exception e ) {
            message = "The output year type (" + OutputYearType + ") is invalid.";
            warning += "\n" + message;
            StringBuffer b = new StringBuffer();
            List<YearType> values = YearType.getYearTypeChoices();
            for ( YearType t : values ) {
                if ( b.length() > 0 ) {
                    b.append ( ", " );
                }
                b.append ( t.toString() );
            }
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Valid values are:  " + b.toString() + "."));
        }
    }
	
	// If the AllowMissingCount is specified, it should be an integer.
	if ( AllowMissingCount!=null && (AllowMissingCount.length()>0) &&
		!StringUtil.isInteger(AllowMissingCount) ) {
        message = "Allow missing count \"" + AllowMissingCount + "\" is not an integer."; 
		warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify the allowed missing count as an interger."));

	}

	// If the Tolerance is specified, it should be a double.
	if ( Tolerance!=null && (Tolerance.length()>0) &&
		( !StringUtil.isDouble(Tolerance) || StringUtil.atod(Tolerance) < 0  ||
        StringUtil.atod(Tolerance) > 1 )) {
        message = "Tolerance \"" + Tolerance + "\" must be a number between 0 and 1 (0.01 = 1 percent).";
		warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify the allowed missing count as an interger."));

	}

    // If the HandleEndpointsHow is specified, make sure it is valid.
	if ( HandleEndpointsHow!=null && HandleEndpointsHow.length()>0 ) {
		if (!HandleEndpointsHow.equalsIgnoreCase(_IncludeFirstOnly)&&
			!HandleEndpointsHow.equalsIgnoreCase(_AverageEndpoints)){
            message = "The HandleEndpointsHow (" + HandleEndpointsHow + ") parameter is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message, "Valid values are \"" + _IncludeFirstOnly
                        + ", and \"" + _AverageEndpoints + "\"."));
		}
		else {
		    // Make sure that it is only specified for INST to MEAN
		    if ( (oldTimeScaleType == TimeScaleType.INST) && (newTimeScaleType == TimeScaleType.MEAN) &&
		        (newInterval.getBase() <= TimeInterval.DAY) ) {
		        // OK
		        // TODO SAM 2010-04-08 Also would like to check that the new interval is < Day but can't do at
		        // initialization
		    }
		    else {
		        // Combination is not allowed.
	            message = "The HandleEndpointsHow (" + HandleEndpointsHow +
	            ") parameter is not supported with the input combination.";
	            warning += "\n" + message;
	            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message, "Only specify the parameter when changing from " +
                    TimeScaleType.INST + " to " + TimeScaleType.MEAN + " small to larger (day or less) interval."));
		    }
		}
	}

	// If the AllowMissingPercent is specified, it should be an number.
	/* TODO SAM 2005-02-18 may enable later
	if ( AllowMissingPercent!=null && (AllowMissingPercent.length()>0) &&
		!StringUtil.isDouble(AllowMissingPercent) ) {
		warning += "\nAllow missing percent \"" + AllowMissingPercent
			+ "\" is not a number.";
	}
	
	// Only one of AllowMissingCount and AllowMissingPercent can be specified
	if ( (AllowMissingCount.length() > 0) &&
	     (AllowMissingPercent.length() > 0) ) {
		warning += "\nOnly one of AllowMissingCount and "
			+ "AllowMissingPercent can be specified.";
	} */
	
	// If the OutputFillMethod is specified, make sure it is valid.
	if ( OutputFillMethod != null && OutputFillMethod.length() > 0 ) {
		if ( !OutputFillMethod.equalsIgnoreCase( _Repeat ) &&
			!OutputFillMethod.equalsIgnoreCase( _Interpolate ) ) {
            message = "The OutputFillMethod (" + OutputFillMethod + ") parameter is invalid.";
			warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message, "Valid values are \"" + _Interpolate
                        + "\" and \"" + _Repeat + "\"."));
		}
	}

	// If the HandleMissingInputHow is specified, make sure it is valid.
	if ( HandleMissingInputHow!=null && HandleMissingInputHow.length()>0 ) {
		if ( !HandleMissingInputHow.equalsIgnoreCase(_KeepMissing)&&
			!HandleMissingInputHow.equalsIgnoreCase(_Repeat)&&
			!HandleMissingInputHow.equalsIgnoreCase(_SetToZero )){
            message = "The HandleMissingInputHow (" + HandleMissingInputHow + ") parameter is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message, "Valid values are \"" + _KeepMissing
                        + "\", " + _Repeat + ", and \"" + _SetToZero + "\"."));
		}
	}
    
    // Check for invalid parameters...
	List valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "NewInterval" );
    valid_Vector.add ( "OldTimeScale" );
    valid_Vector.add ( "NewTimeScale" );
    valid_Vector.add ( "Statistic" );
    valid_Vector.add ( "OutputYearType" );
    valid_Vector.add ( "NewDataType" );
    valid_Vector.add ( "NewUnits" );
    valid_Vector.add ( "Tolerance" );
    valid_Vector.add ( "HandleEndpointsHow" );
    valid_Vector.add ( "AllowMissingCount" );
    valid_Vector.add ( "OutputFillMethod" );
    valid_Vector.add ( "HandleMissingInputHow" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );
    
	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, warning_level ), warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	
	// The command will be modified if changed...
	return ( new ChangeInterval_JDialog ( parent, this ) ).ok();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	
	super.finalize ();
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws 	InvalidCommandSyntaxException, InvalidCommandParameterException
{	String mthd = "changeInterval_Command.parseCommand", mssg;
	int warning_level = 2;

	if ( Message.isDebugOn ) {
		mssg = "Command to parse is: " + command;
		Message.printDebug ( 10, mthd, mssg );
	}
	 
	String Alias = "";
	
    // TODO SAM 2007-11-29 Is this envisioned to process multiple time series?
	// Since this command is of the type TS X = changeInterval (...), we
	// first need to parse the Alias (the X in the command). 
	String substring = "";
	if ( command.indexOf('=') >= 0 ) {
		// Because the parameters contain =, find the first = to break
		// the assignment TS X = changeInterval (...).
		int pos = -1;	// Will be incremented to zero if !__read_one.
		if ( __read_one ) {
			// TS X = changeInterval (...)
			pos = command.indexOf('=');
			substring = command.substring(0,pos).trim();
			List<String> v = StringUtil.breakStringList ( substring, " ", StringUtil.DELIM_SKIP_BLANKS ); 
			// First field has format "TS X"
			Alias = (v.get(1)).trim();		
		}
		
		// Substring, eliminating "TS X =" when __read_one is true.
		// The result substring in any case will contain only the
		// changeInterval (...) part of the command.
		substring = command.substring(pos + 1).trim();	
			
		// Split the substring into two parts: the command name and 
		// the parameters list within the parenthesis.
		List<String> tokens = StringUtil.breakStringList ( substring, "()", 0 );
		if ( tokens == null ) {
			// Must have at least the command name and the parameter list.
			mssg = "Syntax error in \"" + command + "\".";
			Message.printWarning ( warning_level, mthd, mssg);
			throw new InvalidCommandSyntaxException ( mssg );
		}
	
		// Parse the parameters (second token in the tokens vector)
		// needed to process the command.
		try {
			setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT, tokens.get(1), mthd, "," ) );
			// If the Alias was found in the command added it to the parameters propList.	
			if ( Alias != null && Alias.length() > 0 ) {
				setCommandParameter( "Alias", Alias );
				
				if ( Message.isDebugOn ) {
					mssg = "Alias is: " + Alias;
					Message.printDebug ( 10, mthd, mssg );
				}
			} 	
		}
		catch ( Exception e ) {
			mssg = "Syntax error in \"" + command + "\".  Not enough tokens.";
			Message.printWarning ( warning_level, mthd, mssg );
			throw new InvalidCommandSyntaxException ( mssg );
		}
	}
}

/**
Run the command.
@param command_number Number of command being run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
	String routine = getCommandName() + ".runCommand";
	String message = "";
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Warning message level for non-user messages
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	
	PropList parameters = getCommandParameters();
	String Alias = parameters.getValue( "Alias" );
	String TSID = parameters.getValue( "TSID"  );
	String NewInterval = parameters.getValue( "NewInterval"  );
	TimeInterval newInterval = null;
	try {
	    newInterval = TimeInterval.parseInterval(NewInterval);
	}
	catch ( Exception e ) {
        message = "New interval \"" + NewInterval + "\" is not valid.";
        Message.printWarning(log_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
            routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid new interval for the time series - see documentation." ) );
	}
	String OldTimeScale = parameters.getValue( "OldTimeScale" );
	TimeScaleType oldTimeScale = TimeScaleType.valueOfIgnoreCase(OldTimeScale);
	String NewTimeScale = parameters.getValue( "NewTimeScale" );
	TimeScaleType newTimeScale = TimeScaleType.valueOfIgnoreCase(NewTimeScale);
    String Statistic = parameters.getValue ( "Statistic" );
    TSStatisticType statisticType = null;
    if ( (Statistic != null) && (Statistic.length() > 0) ) {
        statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
    }
	String OutputYearType = parameters.getValue( "OutputYearType" );
	YearType outputYearType = YearType.CALENDAR;
	if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
	    outputYearType = YearType.valueOfIgnoreCase(OutputYearType);
	}
	String NewDataType = parameters.getValue( "NewDataType" );
	String NewUnits = parameters.getValue( "NewUnits" );
	String Tolerance = parameters.getValue( "Tolerance" );
	Double tolerance = null;
	if ( StringUtil.isDouble(Tolerance) ) {
	    tolerance = new Double(tolerance);
	}
	String HandleEndpointsHow = parameters.getValue( "HandleEndpointsHow" );
	TSUtil_ChangeInterval_HandleEndpointsHowType handleEndpointsHow = null;
	if ( (HandleEndpointsHow != null) && !HandleEndpointsHow.equals("") ) {
	    handleEndpointsHow =
	        TSUtil_ChangeInterval_HandleEndpointsHowType.valueOfIgnoreCase(HandleEndpointsHow);
	}
	String AllowMissingCount = parameters.getValue("AllowMissingCount" );
	Integer allowMissingCount = null;
	if ( StringUtil.isInteger(AllowMissingCount) ) {
	    allowMissingCount = new Integer(AllowMissingCount);
	}
	/* TODO SAM 2005-02-18 may enable later
	String	AllowMissingPercent= _parameters.getValue("AllowMissingPercent");
	*/
	String OutputFillMethod = parameters.getValue( "OutputFillMethod" );
	TSUtil_ChangeInterval_OutputFillMethodType outputFillMethod = null;
	if ( (OutputFillMethod != null) && !OutputFillMethod.equals("") ) {
	    outputFillMethod = TSUtil_ChangeInterval_OutputFillMethodType.valueOfIgnoreCase(OutputFillMethod);
	}
	String HandleMissingInputHow = parameters.getValue( "HandleMissingInputHow" );
	TSUtil_ChangeInterval_HandleMissingInputHowType handleMissingInputHow = null;
	if ( (HandleMissingInputHow != null) && !HandleMissingInputHow.equals("") ) {
	    handleMissingInputHow =
	        TSUtil_ChangeInterval_HandleMissingInputHowType.valueOfIgnoreCase(HandleMissingInputHow);
	}
	
	// Get the reference (original_ts) to the time series to change interval
	// from.  Currently just one can be processed.
	
	CommandProcessor processor = getCommandProcessor();
	
	PropList request_params = new PropList ( "" );
	request_params.set ( "CommandTag", command_tag );
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try {
	    bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\") from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TS = bean_PropList.getContents ( "TS");
	TS original_ts = null;
	if ( o_TS == null ) {
		message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\") from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
	}
	else {
		original_ts = (TS)o_TS;
	}
	
	if ( original_ts == null ){
		message = "Cannot determine the time series to process for TSID=\"" + TSID + "\".";
		Message.printWarning(warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
		throw new CommandWarningException ( message);
	}
	
    if ( warning_count > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }
	
	// If here, have enough input to attempt the changing the interval
	TS result_ts = null; // Result time series
	try {
		// Process the change of interval
	    TSUtil_ChangeInterval tsu = new TSUtil_ChangeInterval( original_ts, newInterval,
            oldTimeScale, newTimeScale, statisticType, outputYearType, NewDataType, NewUnits, tolerance,
            handleEndpointsHow, outputFillMethod, handleMissingInputHow, allowMissingCount,
            null ); // AllowMissingPercent (not implemented in command)
		result_ts = tsu.changeInterval();	
		
		// Update the newly created time series alias.
		TSIdent tsIdent = result_ts.getIdentifier();
		tsIdent.setAlias ( Alias );
		result_ts.setIdentifier( tsIdent );

		// Add the newly created time series to the software memory.

        TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, result_ts );
	}
    catch ( IllegalArgumentException e ) {
      message = "Error changing the interval for TSID=\"" + TSID + "\" (" + e + ").";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		Message.printWarning ( log_level, routine, e );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Consult documentation for available parameter combinations." ) );
        throw new CommandWarningException ( message );
    }
	catch ( Exception e ) {
		message = "Unexpected error changing the interval for TSID=\"" + TSID + "\" (" + e + ").";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		Message.printWarning ( log_level, routine, e );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
		throw new CommandWarningException ( message );
	}

	// Clean up
	original_ts = null;
	result_ts = null;
	
	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ), routine, message );
		throw new CommandWarningException ( message );
	}
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

	// Get the properties from the command; 
	String Alias = props.getValue( "Alias" );
	String TSID = props.getValue( "TSID" );
	String NewInterval = props.getValue( "NewInterval" );
	String OldTimeScale = props.getValue( "OldTimeScale" );
	String NewTimeScale = props.getValue( "NewTimeScale" );
	String Statistic = props.getValue( "Statistic" );
	String OutputYearType = props.getValue("OutputYearType");
	String NewDataType = props.getValue( "NewDataType" );
	String NewUnits = props.getValue( "NewUnits" );
	String Tolerance = props.getValue( "Tolerance" );
	String HandleEndpointsHow = props.getValue( "HandleEndpointsHow" );
	String AllowMissingCount = props.getValue( "AllowMissingCount" );
	/* TODO SAM 2005-02-18 may enable later
	String AllowMissingPercent = props.getValue( "AllowMissingPercent" );
	*/
	String OutputFillMethod = props.getValue( "OutputFillMethod" );
	String HandleMissingInputHow= props.getValue( "HandleMissingInputHow");
	
	// Creating the command string
	// This StringBuffer will contain all parameters for the command.
	StringBuffer b = new StringBuffer();

	if ( TSID != null && TSID.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "TSID=\"" + TSID + "\"" );
	}

	if ( NewInterval != null && NewInterval.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "NewInterval=" + NewInterval );
	}

	if ( OldTimeScale != null && OldTimeScale.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "OldTimeScale=" + OldTimeScale );
	}

	if ( NewTimeScale != null && NewTimeScale.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "NewTimeScale=" + NewTimeScale  );
	}
	
    if ( Statistic != null && Statistic.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "Statistic=" + Statistic  );
    }
	
    if ( (OutputYearType != null) && (OutputYearType.length() > 0) ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "OutputYearType=" + OutputYearType );
    }

	if ( NewDataType != null && NewDataType.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "NewDataType=" + NewDataType );
	}
	
    if ( NewUnits != null && NewUnits.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "NewUnits=" + NewUnits );
    }

    if ( Tolerance != null && Tolerance.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "Tolerance=" + Tolerance );
    }
	
    if ( HandleEndpointsHow != null && HandleEndpointsHow.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "HandleEndpointsHow=" + HandleEndpointsHow );
    }

	if ( AllowMissingCount != null && AllowMissingCount.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "AllowMissingCount=" + AllowMissingCount );
	}
	
	// Adding the AllowMissingPercent
	/* TODO SAM 2005-02-18 may enable later
	if ( AllowMissingPercent != null && AllowMissingPercent.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "AllowMissingPercent=" + AllowMissingPercent );
	} */
	
	if ( OutputFillMethod != null && OutputFillMethod.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "OutputFillMethod=" + OutputFillMethod );
	}
	
	if ( HandleMissingInputHow != null && HandleMissingInputHow.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "HandleMissingInputHow=" + HandleMissingInputHow );
	}
	
	String commandString = getCommandName() + "(" + b.toString() + ")";
	if ( __read_one ) {
		commandString = "TS " + Alias + " = " + commandString;
	} 
	
	return commandString;
}

}