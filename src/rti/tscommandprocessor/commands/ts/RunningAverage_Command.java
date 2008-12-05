package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSUtil;

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

/**
<p>
This class initializes, checks, and runs the RunningAverage() command.
</p>
*/
public class RunningAverage_Command extends AbstractCommand implements Command
{

/**
Values for the AverageMethod parameter.
*/
protected final String _Centered = "Centered";
protected final String _Future = "Future";
protected final String _FutureInclusive = "FutureInclusive";
protected final String _NYear = "NYear";
protected final String _Previous = "Previous";
protected final String _PreviousInclusive = "PreviousInclusive";

/**
Constructor.
*/
public RunningAverage_Command ()
{	super();
	setCommandName ( "RunningAverage" );
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
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	//String TSID = parameters.getValue ( "TSID" );
	String AverageMethod = parameters.getValue ( "AverageMethod" );
    String Bracket = parameters.getValue ( "Bracket" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    /* TODO SAM 2008-01-03 Evaluate combination with TSList
	if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide a time series identifier." ) );
	}
    */
	if ( (AverageMethod != null) && !AverageMethod.equals("") &&
		!AverageMethod.equalsIgnoreCase(_Centered) &&
        !AverageMethod.equalsIgnoreCase(_Future) &&
        !AverageMethod.equalsIgnoreCase(_FutureInclusive) &&
		!AverageMethod.equalsIgnoreCase(_NYear) &&
        !AverageMethod.equalsIgnoreCase(_Previous) &&
        !AverageMethod.equalsIgnoreCase(_PreviousInclusive) ) {
        message = "The AverageMethod parameter (" + AverageMethod + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "AverageMethod must be " + _Centered + " or " + _NYear ) );
	}
    if ( (Bracket != null) && !Bracket.equals("") && !StringUtil.isInteger(Bracket) ) {
        message = "The Bracket parameter (" + Bracket + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the bracket as an integer." ) );
    }
      
    // Check for invalid parameters...
    List valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "AverageMethod" );
    valid_Vector.add ( "Bracket" );
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
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new RunningAverage_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
        // Recently added TSList so handle it properly
        PropList parameters = getCommandParameters();
        String TSList = parameters.getValue ( "TSList");
        String TSID = parameters.getValue ( "TSID");
        if ( ((TSList == null) || (TSList.length() == 0)) && // TSList not specified
                ((TSID != null) && (TSID.length() != 0)) ) { // but TSID is specified
            // Assume old-style where TSList was not specified but TSID was...
            if ( (TSID != null) && TSID.indexOf("*") >= 0 ) {
                parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
            }
            else {
                parameters.set ( "TSList", TSListType.LAST_MATCHING_TSID.toString() );
            }
        }
    }
    else {
		//TODO SAM 2005-08-24 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax without named parameters.
    	List v = StringUtil.breakStringList ( command_string,"(),",StringUtil.DELIM_SKIP_BLANKS );
		String TSID = "";
		String AverageMethod = "";
        String Bracket = "";
		if ( (v != null) && (v.size() == 4) ) {
			// Second field is identifier...
			TSID = ((String)v.get(1)).trim();
			// Third field has average method...
            AverageMethod = ((String)v.get(2)).trim();
            // Transition to newer convention...
            if ( AverageMethod.equalsIgnoreCase("N-Year")) {
                AverageMethod = _NYear;
            }
            // Fourth field has bracket...
            Bracket = ((String)v.get(3)).trim();
		}

		// Set parameters and new defaults...

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID.length() > 0 ) {
			parameters.set ( "TSID", TSID );
            // Legacy behavior was to match last matching TSID if no wildcard
            if ( TSID.indexOf("*") >= 0 ) {
                parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
            }
            else {
                parameters.set ( "TSList", TSListType.LAST_MATCHING_TSID.toString() );
            }
		}
		if ( AverageMethod.length() > 0 ) {
			parameters.set ( "AverageMethod", AverageMethod);
		}
        if ( Bracket.length() > 0 ) {
            parameters.set ( "Bracket", Bracket );
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
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "RunningAverage_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;	// Warning level for non-user messages.

	// Make sure there are time series available to operate on...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
    String TSList = parameters.getValue ( "TSList" );
    String TSID = parameters.getValue ( "TSID" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_MATCHING_TSID.toString();
        if ( (TSID == null) || TSID.equals("") ) {
            TSID = "*";
        }
    }
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	String AverageMethod = parameters.getValue ( "AverageMethod" );
    String Bracket = parameters.getValue ( "Bracket" );

	// Get the time series to process.  Allow TSID to be a pattern or specific time series...

	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
	CommandProcessorRequestResultsBean bean = null;
	try {
	    bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	List tslist = null;
	if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}
	else {	tslist = (List)o_TSList;
		if ( tslist.size() == 0 ) {
            message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
					command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message,
                    "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
		}
	}
	Object o_Indices = bean_PropList.getContents ( "Indices" );
	int [] tspos = null;
	if ( o_Indices == null ) {
		message = "Unable to find indices for time series to process using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
	}
	else {	tspos = (int [])o_Indices;
		if ( tspos.length == 0 ) {
			message = "Unable to find indices for time series to process using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
			Message.printWarning ( warning_level,
			    MessageUtil.formatMessageTag(
				    command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
		}
	}
    
    // The ensemble is needed because its time series will be replaced...
    
    TSEnsemble tsensemble = null;
    if ( TSListType.ENSEMBLE_ID.equals(TSList) ) {
        request_params = new PropList ( "" );
        request_params.set ( "CommandTag", command_tag );
        request_params.set ( "EnsembleID", EnsembleID );
        try {
            bean = processor.processRequest( "GetEnsemble", request_params );
        }
        catch ( Exception e ) {
            message = "Error requesting GetEnsemble(EnsembleID=\"" + EnsembleID + "\") from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object o_TSEnsemble = bean_PropList.getContents ( "TSEnsemble");
        if ( o_TSEnsemble == null ) {
            message = "Null TS requesting GetEnsemble(EnsembleID=\"" + EnsembleID + "\") from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
        }
        else {
            tsensemble = (TSEnsemble)o_TSEnsemble;
        }
        
        if ( tsensemble == null ) {
            message = "Unable to find ensemble to process using EnsembleID \"" + EnsembleID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
            throw new CommandWarningException ( message );
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

	int Bracket_int = StringUtil.atoi ( Bracket );
	TS ts = null;  // Time series to process
    TS newts = null;    // Running average time series
	for ( int its = 0; its < nts; its++ ) {
		request_params = new PropList ( "" );
		request_params.setUsingObject ( "Index", new Integer(tspos[its]) );
		bean = null;
		try {
            bean = processor.processRequest( "GetTimeSeries", request_params);
		}
		catch ( Exception e ) {
            message = "Error requesting GetTimeSeries(Index=" + tspos[its] + ") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
		}
		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "TS" );
		if ( prop_contents == null ) {
            message = "Null value for GetTimeSeries(Index=" + tspos[its] + ") returned from processor.";
			Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
		}
		else {
            ts = (TS)prop_contents;
		}
		
		// TODO SAM 2007-02-17 Evaluate whether to print warning if null TS
		
		try {
            // Do the processing...
			Message.printStatus ( 2, routine, "Converterting to running average: \"" + ts.getIdentifier() + "\"." );
            if ( AverageMethod.equalsIgnoreCase( _Centered) ) {
                newts = TSUtil.createRunningAverageTS ( ts, Bracket_int, TSUtil.RUNNING_AVERAGE_CENTER );
            }
            else if ( AverageMethod.equalsIgnoreCase( _Future) ) {
                newts = TSUtil.createRunningAverageTS ( ts, Bracket_int, TSUtil.RUNNING_AVERAGE_FUTURE );
            }
            else if ( AverageMethod.equalsIgnoreCase( _FutureInclusive) ) {
                newts = TSUtil.createRunningAverageTS ( ts, Bracket_int, TSUtil.RUNNING_AVERAGE_FUTURE_INCLUSIVE );
            }
            else if ( AverageMethod.equalsIgnoreCase( _NYear) ) {
                newts = TSUtil.createRunningAverageTS ( ts, Bracket_int, TSUtil.RUNNING_AVERAGE_NYEAR );
            }
            else if ( AverageMethod.equalsIgnoreCase( _Previous) ) {
                newts = TSUtil.createRunningAverageTS ( ts, Bracket_int, TSUtil.RUNNING_AVERAGE_PREVIOUS );
            }
            else if ( AverageMethod.equalsIgnoreCase( _PreviousInclusive) ) {
                newts = TSUtil.createRunningAverageTS ( ts, Bracket_int, TSUtil.RUNNING_AVERAGE_PREVIOUS_INCLUSIVE );
            }
            // Because the time series is a new instance, replace in the processor (and ensemble if appropriate)...
            request_params = new PropList ( "" );
            request_params.setUsingObject ( "TS", newts );
            request_params.setUsingObject ( "Index", new Integer(tspos[its]) );
            bean = null;
            try {
                bean = processor.processRequest( "SetTimeSeries", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting SetTimeSeries(Index=" + tspos[its] + ") from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
            }
            if ( TSListType.ENSEMBLE_ID.equals(TSList) ) {
                // TODO SAM 2008-01-07 Evaluate whether low-level code should be updated to now cause a new
                // reference to be needed
                tsensemble.set ( its, newts );
            }
		}
		catch ( Exception e ) {
			message = "Unexpected error converting time series \""+	ts.getIdentifier() + "\" to running average (" + e + ").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count),routine,message );
			Message.printWarning(3,routine,e);
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
		}
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
    String TSList = props.getValue( "TSList" );
    String TSID = props.getValue( "TSID" );
    String EnsembleID = props.getValue( "EnsembleID" );
	String AverageMethod = props.getValue("AverageMethod");
	String Bracket = props.getValue("Bracket");
	StringBuffer b = new StringBuffer ();
    if ( (TSList != null) && (TSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSList=" + TSList );
    }
    if ( (TSID != null) && (TSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSID=\"" + TSID + "\"" );
    }
    if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
    }
	if ( (AverageMethod != null) && (AverageMethod.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AverageMethod=" + AverageMethod );
	}
	if ( (Bracket != null) && (Bracket.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Bracket=" + Bracket );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
