package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
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
This class initializes, checks, and runs the Multiply() command.
*/
public class Multiply_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public Multiply_Command ()
{	super();
	setCommandName ( "Multiply" );
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
	String MultiplierTSID = parameters.getValue ( "MultiplierTSID" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series to be modified is not specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series identifier for the time series to be modified."));
	}
    if ( (MultiplierTSID == null) || MultiplierTSID.equals("") ) {
        message = "The time series identifier for the divisor time series is not specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series identifier for the divisor time series."));
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>();
    validList.add ( "TSID" );
    validList.add ( "MultiplierTSID" );
    validList.add ( "NewUnits" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );
    
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
	return (new Multiply_JDialog ( parent, this )).ok();
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
{   int warning_level = 2;
    String routine = "Multiply_Command.parseCommand", message;

    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
        // TODO SAM 2008-09-24 This whole block of code needs to be
        // removed as soon as commands have been migrated to the new syntax.
        //
        // Old syntax where the only parameter is a single TSID or * to fill all.
    	List v = StringUtil.breakStringList(command_string,
            "(),\t", StringUtil.DELIM_SKIP_BLANKS | StringUtil.DELIM_ALLOW_STRINGS );
        int ntokens = 0;
        if ( v != null ) {
            ntokens = v.size();
        }
        if ( ntokens != 3 ) {
            message = "Syntax error in \"" + command_string +
            "\".  Expecting Multiply(TSID,MultiplierTSID).";
            Message.printWarning ( warning_level, routine, message);
            throw new InvalidCommandSyntaxException ( message );
        }

        // Get the individual tokens of the expression...

        String TSID = ((String)v.get(1)).trim();
        String MultiplierTSID = ((String)v.get(2)).trim();

        // Set parameters and new defaults...

        PropList parameters = new PropList ( getCommandName() );
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        if ( TSID.length() > 0 ) {
            parameters.set ( "TSID", TSID );
        }
        if ( MultiplierTSID.length() > 0 ) {
            parameters.set ( "MultiplierTSID", MultiplierTSID );
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
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Level for non-user messages

	// Make sure there are time series available to operate on...

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    Boolean clearStatus = new Boolean(true); // default
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}
	
	String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
	String MultiplierTSID = parameters.getValue ( "MultiplierTSID" );
	if ( (MultiplierTSID != null) && (MultiplierTSID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		MultiplierTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, MultiplierTSID);
	}
	String NewUnits = parameters.getValue ( "NewUnits" );
	if ( (NewUnits != null) && (NewUnits.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		NewUnits = TSCommandProcessorUtil.expandParameterValue(processor, this, NewUnits);
	}

	// Get the time series to process.  The time series list is searched
	// backwards until the first match...

	TS ts = null;
	try {	PropList request_params = new PropList ( "" );
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
				Message.printWarning(log_level, routine, e );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
			}
			PropList bean_PropList = bean.getResultsPropList();
			Object o_TS = bean_PropList.getContents ( "TS");
			if ( o_TS == null ) {
				message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID +
				"\") from processor.";
				Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
			}
			else {
				ts = (TS)o_TS;
			}
	}
	catch ( Exception e ) {
		ts = null;
	}
	if ( ts == null ) {
		message = "Unable to find time series to process using TSID \"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
		throw new CommandWarningException ( message );
	}
 	
    TS tsMultiplier = null;
    try {   PropList request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "TSID", MultiplierTSID );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + MultiplierTSID + "\") from processor.";
                Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
                Message.printWarning(log_level, routine, e );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_TS = bean_PropList.getContents ( "TS");
            if ( o_TS == null ) {
                message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + MultiplierTSID +
                "\") from processor.";
                Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
            }
            else {
                tsMultiplier = (TS)o_TS;
            }
    }
    catch ( Exception e ) {
        tsMultiplier = null;
    }
    if ( tsMultiplier == null ) {
        message = "Unable to find time series to process using TSID \"" + MultiplierTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }
	
	// Now process the time series...

	try {
	    TSUtil.multiply ( ts, tsMultiplier );
        // If requested, change the data units...
        if ( (NewUnits != null) && (NewUnits.length() > 0) ) {
            ts.addToGenesis ( "Changed units from \"" + ts.getDataUnits() + "\" to \"" + NewUnits+"\"");
            ts.setDataUnits ( NewUnits );
        }
	}
	catch ( Exception e ) {
		message = "Unexpected error trying to multiply \""+
		ts.getIdentifier() + "\" by \"" + tsMultiplier.getIdentifier() + "\".";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the log file - report the problem to software support." ) );
	}

    // No need to update the data since the original ts reference is used.

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
	String MultiplierTSID = props.getValue( "MultiplierTSID" );
	String NewUnits = props.getValue( "NewUnits" );
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
    if ( (MultiplierTSID != null) && (MultiplierTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MultiplierTSID=\"" + MultiplierTSID + "\"" );
    }
    if ( (NewUnits != null) && (NewUnits.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewUnits=\"" + NewUnits + "\"" );
    }
	return getCommandName() + "("+ b.toString()+")";
}

}
