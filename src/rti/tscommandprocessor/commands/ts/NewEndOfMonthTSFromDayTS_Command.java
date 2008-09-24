package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.Vector;

import RTi.TS.DayTS;
import RTi.TS.MonthTS;
import RTi.TS.TS;
import RTi.TS.TSUtil_ChangeInterval;

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
This class initializes, checks, and runs the NewEndOfMonthTSFromDayTS() command.
</p>
*/
public class NewEndOfMonthTSFromDayTS_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public NewEndOfMonthTSFromDayTS_Command ()
{	super();
	setCommandName ( "NewEndOfMonthTSFromDayTS" );
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
{	String Alias = parameters.getValue ( "Alias" );
	String DayTSID = parameters.getValue ( "DayTSID" );
    String Bracket = parameters.getValue ( "Bracket" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (Alias == null) || Alias.equals("") ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series alias when defining the command."));
	}
	if ( (DayTSID == null) || DayTSID.equals("") ) {
        message = "The daily time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a daily time series identifier."));
	}
    if ( (Bracket == null) || Bracket.equals("") ) {
        message = "The bracket must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify the bracket (number of days on each side to search)."));
    }
    else if ( !StringUtil.isInteger(Bracket) ) {
        message = "The bracket is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify the bracket as an integer (number of days on each side to search)."));
    }
    else {
        int bracket = Integer.parseInt(Bracket);
        if ( bracket <= 0 ) {
            message = "The bracket cannot be <= 0.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message,
                    "Specify the bracket as a positive integer (number of days on each side to search)."));
        }
    }
    
    // Check for invalid parameters...
    Vector valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "DayTSID" );
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
	return (new NewEndOfMonthTSFromDayTS_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "NewEndOfMonthTSFromDayTS_Command.parseCommand", message;

	// Get the part of the command after the TS Alias =...
	int pos = command.indexOf ( "=" );
	if ( pos < 0 ) {
		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = NewEndOfMonthTSFromDayTS(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	String token0 = command.substring ( 0, pos ).trim();
	String token1 = command.substring ( pos + 1 ).trim();
	if ( (token0 == null) || (token1 == null) ) {
		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = NewEndOfMonthTSFromDayTS(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
    Vector v = StringUtil.breakStringList ( token0, " ", StringUtil.DELIM_SKIP_BLANKS );
    if ( v == null ) {
        message = "Syntax error in \"" + command +
        "\".  Expecting:  TS Alias = NewEndOfMonthTSFromDayTS(DayTSID,Bracket)";
        Message.printWarning ( warning_level, routine, message);
        throw new InvalidCommandSyntaxException ( message );
    }
    String Alias = (String)v.elementAt(1);
    String DayTSID = null;
    String Bracket = null;
	if ( (token1.indexOf('=') < 0) && !token1.endsWith("()") ) {
		// No parameters have = in them...
		// TODO SAM 2008-09-23 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax without named parameters.

		v = StringUtil.breakStringList ( token1,"(),",
		        StringUtil.DELIM_SKIP_BLANKS|StringUtil.DELIM_ALLOW_STRINGS );
		if ( (v == null) || (v.size() != 3) ) {
			message = "Syntax error in \"" + command +
			"\".  Expecting:  TS Alias = NewEndOfMonthTSFromDayTS(DayTSID,Bracket)";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
        DayTSID = (String)v.elementAt(1);
        Bracket = (String)v.elementAt(2);
	}
	else {
        // Current syntax...
        super.parseCommand( token1 );
	}
    
    // Set parameters and new defaults...

    PropList parameters = getCommandParameters();
    parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
    if ( Alias.length() > 0 ) {
        parameters.set ( "Alias", Alias );
    }
    // Reset using above information
    if ( (DayTSID != null) && (DayTSID.length() > 0) ) {
        parameters.set ( "DayTSID", DayTSID );
    }
    if ( (Bracket != null) && (Bracket.length() > 0) ) {
        parameters.set ( "Bracket", Bracket );
    }
    parameters.setHowSet ( Prop.SET_UNKNOWN );
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
{	String routine = "NewEndOfMonthTSFromDayTS_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Level for non-user messages

	// Make sure there are time series available to operate on...

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	
	String Alias = parameters.getValue ( "Alias" );
	String DayTSID = parameters.getValue ( "DayTSID" );
	String Bracket = parameters.getValue ( "Bracket" );

	// Get the time series to process.  The time series list is searched
	// backwards until the first match...

	TS ts = null;
	try {	PropList request_params = new PropList ( "" );
			request_params.set ( "CommandTag", command_tag );
			request_params.set ( "TSID", DayTSID );
			CommandProcessorRequestResultsBean bean = null;
			try { bean =
				processor.processRequest( "GetTimeSeriesForTSID", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + DayTSID +
				"\") from processor.";
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
				message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + DayTSID +
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
		message = "Unable to find daily time series using TSID \"" + DayTSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
		throw new CommandWarningException ( message );
	}

	// Now process the time series...

	MonthTS monthts = null;
	try {
	    PropList changeprops = new PropList ( "newEndOfMonthTSFromDayTS" );
	    changeprops.set ( "UseNearestToEnd", Bracket );
	    Message.printStatus ( 1, routine, "Changing interval..." );
	    TSUtil_ChangeInterval tsu = new TSUtil_ChangeInterval();
	    monthts = tsu.OLDchangeToMonthTS ( (DayTS)ts, 1, changeprops );
		monthts.setAlias ( Alias );
	}
	catch ( Exception e ) {
		message = "Unexpected processing daily time series \"" + ts.getIdentifier() + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the log file - report the problem to software support." ) );
	}
	
    // Further process the time series...
    // This makes sure the period is at least as long as the output period...
	Vector tslist = new Vector(1);
	tslist.add ( monthts );
    int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
    if ( wc > 0 ) {
        message = "Error post-processing series after creation.";
        Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag,
            ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
        throw new CommandException ( message );
    }

    // Update the data to the processor so that appropriate actions are taken...

    TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, monthts );

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
		return "TS Alias = " + getCommandName() + "()";
	}
	String Alias = props.getValue( "Alias" );
	String DayTSID = props.getValue( "DayTSID" );
	String Bracket = props.getValue( "Bracket" );
	StringBuffer b = new StringBuffer ();
	if ( (DayTSID != null) && (DayTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DayTSID=\"" + DayTSID + "\"" );
	}
	if ( (Bracket != null) && (Bracket.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Bracket=" + Bracket );
	}
	return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
}

}
