package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.Vector;

import RTi.TS.TS;

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
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;

/**
<p>
This class initializes, checks, and runs the SelectTimeSeries() command.
</p>
*/
public class SelectTimeSeries_Command extends AbstractCommand implements Command
{
    
/**
Values for DeselectAllFirst.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
TSPosition data, zero offset indices
*/
int [] TSPositionStart = new int[0];
int [] TSPositionEnd = new int[0];

/**
Constructor.
*/
public SelectTimeSeries_Command ()
{	super();
	setCommandName ( "SelectTimeSeries" );
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
{	//String TSList = parameters.getValue ( "TSList" );
    //String TSID = parameters.getValue ( "TSID" );
    String TSPosition = parameters.getValue ( "TSPosition" );
	String DeselectAllFirst = parameters.getValue ( "DeselectAllFirst" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (TSPosition != null) && !TSPosition.equals("") ) {
        Vector tokens = StringUtil.breakStringList ( TSPosition,",", StringUtil.DELIM_SKIP_BLANKS );
        int npos = 0;
        if ( tokens != null ) {
            npos = tokens.size();
        }
        TSPositionStart = new int[npos];
        TSPositionEnd = new int[npos];
        for ( int i = 0; i < npos; i++ ) {
            String token = (String)tokens.elementAt(i);
            if ( token.indexOf("-") >= 0 ) {
                // Range...
                String posString = StringUtil.getToken(token, "-",0,0).trim();
                if ( !StringUtil.isInteger(posString) ) {
                    message = "The TSPosition range (" + token + ") contains an invalid position.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the range using integers with value 1+." ) );
                }
                else {
                    TSPositionStart[i] = StringUtil.atoi( posString ) - 1;
                }
                posString = StringUtil.getToken(token, "-",0,1).trim();
                if ( !StringUtil.isInteger(posString) ) {
                    message = "The TSPosition range (" + token + ") contains an invalid position.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the range using integer with value 1+." ) );
                }
                else {
                    TSPositionEnd[i] = StringUtil.atoi( posString ) - 1;
                }
            }
            else {
                // Single value.  Treat as a range of 1.
                if ( !StringUtil.isInteger(token) ) {
                    message = "The TSPosition (" + token + ") is invalid.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the position as an integer 1+." ) );
                }
                TSPositionStart[i] = StringUtil.atoi(token) - 1;
                TSPositionEnd[i] = TSPositionStart[i];
            }
            Message.printStatus ( 1, "", "Range " + i + " from " + token + " is " +
                    TSPositionStart[i] + "," + TSPositionEnd[i] );
        }
	}
	
	if ( (DeselectAllFirst != null) && !DeselectAllFirst.equals("") &&
	        !DeselectAllFirst.equalsIgnoreCase(_True) && !DeselectAllFirst.equalsIgnoreCase(_False) ) {
        message = "The DeselectAllFirst (" + DeselectAllFirst + ") parameter value is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify as " + _False + " or " + _True + "." ) );
	}
    
    // Check for invalid parameters...
    Vector valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "TSPosition" );
    valid_Vector.add ( "DeselectAllFirst" );
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
	return (new SelectTimeSeries_JDialog ( parent, this )).ok();
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
{
    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
        // Recently added TSList so handle it properly
        PropList parameters = getCommandParameters();
        String TSList = parameters.getValue ( "TSList");
        String TSID = parameters.getValue ( "TSID");
        String Pos = parameters.getValue ( "Pos");

        if ( (Pos != null) && (Pos.length() != 0) ) {
            // Legacy Pos property is specified.  Switch to TSPosition
            parameters.set ( "TSList", TSListType.TSPOSITION.toString() );
            parameters.set ( "TSPosition", Pos );
        }
        else if ( ((TSList == null) || (TSList.length() == 0)) && // TSList not specified
                ((TSID != null) && (TSID.length() != 0)) ) { // but TSID is specified
            // Assume old-style where TSList was not specified but TSID was...
            parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
        }
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
{	String routine = "SelectTimeSeries_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;  // Level for non-use messages for log file.

	// Make sure there are time series available to operate on...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	String TSPosition = parameters.getValue ( "TSPosition" );
	String DeselectAllFirst = parameters.getValue ( "DeselectAllFirst" );
	boolean DeselectAllFirst_boolean = false;  // Default
	if ( (DeselectAllFirst != null) && DeselectAllFirst.equalsIgnoreCase("true") ) {
	    DeselectAllFirst_boolean = true;
	}
	
	// If necessary, get the list of all time series...
	Vector tslistAll = new Vector();
	if ( DeselectAllFirst_boolean ) {
	    // Deselect all first
	    try {
	        tslistAll = (Vector)processor.getPropContents("TSResultsList");
            if ( DeselectAllFirst_boolean ) {
                Message.printStatus ( 2, routine, "Deselecting all time series first." );
                int ntsAll = 0;
                if ( tslistAll != null ) {
                    ntsAll = tslistAll.size();
                }
                for ( int its = 0; its < ntsAll; its++ ) {
                    TS ts = (TS)tslistAll.get(its);    // Will throw Exception
                    ts.setSelected ( false );
                }
            }
	    }
	    catch ( Exception e ) {
	        // Should not happen.
	        message = "Unexpected error deselecting all time series first.";
	        Message.printWarning(log_level,
	                MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                routine, message );
	        status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Report the problem to software support." ) );
	    }
	}
	
	// Get the time series to process.  Allow TSID to be a pattern or specific time series...

	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
    request_params.set ( "TSPosition", TSPosition );
	CommandProcessorRequestResultsBean bean = null;
	try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
    if ( bean == null ) {
        Message.printStatus ( 2, routine, "Bean is null.");
    }
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	Vector tslist = null;
	if ( o_TSList == null ) {
		message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( log_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
	}
	else {
        tslist = (Vector)o_TSList;
		if ( tslist.size() == 0 ) {
			message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
			Message.printWarning ( log_level,
					MessageUtil.formatMessageTag(
							command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message,
                            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
		}
	}
	
	int nts = tslist.size();
	if ( nts == 0 ) {
		message = "Unable to find time series to select using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
	}

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Command parameter data has errors.  Unable to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		throw new CommandException ( message );
	}

	// Now process the time series...

	TS ts = null;
	Object o_ts = null;
	for ( int its = 0; its < nts; its++ ) {
		// The the time series to process, from the list that was returned above.
		o_ts = tslist.elementAt(its);
		if ( o_ts == null ) {
			message = "Time series to process is null.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message,
                            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
			// Go to next time series.
			continue;
		}
		ts = (TS)o_ts;
		
		try {

		    
		    // Do the selection...
			Message.printStatus ( 2, routine, "Selecting \"" + ts.getIdentifier()+ "\"" );
			ts.setSelected ( true );
		}
		catch ( Exception e ) {
			message = "Unexpected error selecting time series (" + e + ").";
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
	String TSPosition = props.getValue("TSPosition");
	String DeselectAllFirst = props.getValue("DeselectAllFirst");
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
	if ( (TSPosition != null) && (TSPosition.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSPosition=\"" + TSPosition + "\"" );
	}
	if ( (DeselectAllFirst != null) && (DeselectAllFirst.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DeselectAllFirst=" + DeselectAllFirst );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
