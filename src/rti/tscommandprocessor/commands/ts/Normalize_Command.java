package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.Vector;

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
<p>
This class initializes, checks, and runs the Normalize() command.
</p>
*/
public class Normalize_Command extends AbstractCommand implements Command
{
    
/**
Values for the MinValueMethod parameter.
*/
protected final String _MinFromTS = "MinFromTS";
protected final String _MinZero = "MinZero";

/**
Constructor.
*/
public Normalize_Command ()
{	super();
	setCommandName ( "Normalize" );
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
{	String Alias = parameters.getValue ( "Alias" );
	String TSID = parameters.getValue ( "TSID" );
	String MinValue = parameters.getValue ( "MinValue" );
	String MaxValue = parameters.getValue ( "MaxValue" );
	String MinValueMethod = parameters.getValue ( "MinValueMethod" );
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
	if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series identifier for the time series to normalize must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series identifier when defining the command."));
	}
	if ( (Alias != null) && !Alias.equals("") && (TSID != null) && !TSID.equals("") &&
	        Alias.equalsIgnoreCase(TSID) ) {
        message = "The alias and original time series are the same.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify a different alias."));
	}
    if ( (MinValue == null) || MinValue.equals("") ) {
        message = "The minimum value must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum value as a number." ) );
    }
    else if ( !StringUtil.isDouble(MinValue) ) {
        message = "The minimum value " + MinValue + " is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum value as a number." ) );
    }
    
    if ( (MaxValue == null) || MaxValue.equals("") ) {
        message = "The maximum value must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the maximum value as a number." ) );
    }
    else if ( !StringUtil.isDouble(MaxValue) ) {
        message = "The maximum value " + MaxValue + " is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the maximum value as a number." ) );
    }

    if ( (MinValueMethod == null) || MinValueMethod.equals("") ) {
        message = "The minimum value method must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum value method as " + _MinFromTS + " or " + _MinZero ) );
    }
    else if ( !MinValueMethod.equalsIgnoreCase(_MinFromTS) && !MinValueMethod.equalsIgnoreCase(_MinZero) ) {
        message = "The minimum value method \"" + MinValueMethod + "\" is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify a method of " + _MinFromTS + " or " + _MinZero + "."));
    }
    
    // Check for invalid parameters...
    Vector valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "MinValue" );
    valid_Vector.add ( "MaxValue" );
    valid_Vector.add ( "MinValueMethod" );
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
	return (new Normalize_JDialog ( parent, this )).ok();
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
	String routine = "Normalize_Command.parseCommand", message;

	// Get the part of the command after the TS Alias =...
	int pos = command.indexOf ( "=" );
	if ( pos < 0 ) {
		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = Normalize(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	String token0 = command.substring ( 0, pos ).trim();
	String token1 = command.substring ( pos + 1 ).trim();
	if ( (token0 == null) || (token1 == null) ) {
		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = Normalize(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
    Vector v = StringUtil.breakStringList ( token0, " ", StringUtil.DELIM_SKIP_BLANKS );
    if ( v == null ) {
        message = "Syntax error in \"" + command +
        "\".  Expecting:  TS Alias = Normalize(TSID,MinValueMethod,MinValue,MaxValue)";
        Message.printWarning ( warning_level, routine, message);
        throw new InvalidCommandSyntaxException ( message );
    }
    String Alias = (String)v.elementAt(1);
    String TSID = null;
    String MinValue = null;
    String MaxValue = null;
    String MinValueMethod = null;
	if ( (token1.indexOf('=') < 0) && !token1.endsWith("()") ) {
		// No parameters have = in them...
		// TODO SAM 2009-09-22 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax without named parameters.

		v = StringUtil.breakStringList ( token1,"(),",
		        StringUtil.DELIM_SKIP_BLANKS|StringUtil.DELIM_ALLOW_STRINGS );
		if ( (v == null) || v.size() != 5 ) {
			message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = " +
					"Normalize(TSID,MinValueMethod,MinValue,MaxValue";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
        // TSID is the only parameter
        TSID = (String)v.elementAt(1);
        MinValueMethod = (String)v.elementAt(2);
        MinValue = (String)v.elementAt(3);
        MaxValue = (String)v.elementAt(4);
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
    if ( (TSID != null) && (TSID.length() > 0) ) {
        parameters.set ( "TSID", TSID );
    }
    if ( (MinValue != null) && (MinValue.length() > 0) ) {
        parameters.set ( "MinValue", MinValue );
    }
    if ( (MaxValue != null) && (MaxValue.length() > 0) ) {
        parameters.set ( "MaxValue", MaxValue );
    }
    if ( (MinValueMethod != null) && (MinValueMethod.length() > 0) ) {
        parameters.set ( "MinValueMethod", MinValueMethod );
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
{	String routine = "Normalize_Command.runCommand", message;
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
	String TSID = parameters.getValue ( "TSID" );
	String MinValue = parameters.getValue ( "MinValue" );
	double MinValue_double = Double.parseDouble(MinValue);
	String MaxValue = parameters.getValue ( "MaxValue" );
	double MaxValue_double = Double.parseDouble(MaxValue);
	String MinValueMethod = parameters.getValue ( "MinValueMethod" );
	boolean MinValueMethod_MinFromTS_boolean = true;
	if ( MinValueMethod.equalsIgnoreCase(_MinZero) ) {
	    MinValueMethod_MinFromTS_boolean = false;
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
		message = "Unable to find time series to normalize using TSID \"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
		throw new CommandWarningException ( message );
	}
	
	// Now process the time series...

	TS tsnew = null;
	try {
	    // Make a copy of the found time series...
        tsnew = (TS)ts.clone();
	    TSUtil.normalize ( tsnew, MinValueMethod_MinFromTS_boolean, MinValue_double, MaxValue_double );
		tsnew.setAlias ( Alias );
	}
	catch ( Exception e ) {
		message = "Unexpected error trying to normalize time series \""+ ts.getIdentifier() + "\".";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the log file - report the problem to software support." ) );
	}

    // Update the data to the processor so that appropriate actions are taken...

    TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, tsnew );

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
	String TSID = props.getValue( "TSID" );
	String MinValue = props.getValue( "MinValue" );
	String MaxValue = props.getValue( "MaxValue" );
	String MinValueMethod = props.getValue( "MinValueMethod" );
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
    if ( (MinValueMethod != null) && (MinValueMethod.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MinValueMethod=" + MinValueMethod );
    }
	if ( (MinValue != null) && (MinValue.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "MinValue=" + MinValue );
	}
    if ( (MaxValue != null) && (MaxValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MaxValue=" + MaxValue );
    }
	return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
}

}
