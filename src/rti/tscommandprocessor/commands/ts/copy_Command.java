//------------------------------------------------------------------------------
// copy_Command - handle the TS Alias = copy() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-08-25	Steven A. Malers, RTi	Initial version.  Copy and modify
//					scale().
// 2005-08-29	SAM, RTi		Finish enabling runCommand().
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSIdent;

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
import RTi.Util.Time.TimeInterval;

/**
<p>
This class initializes, checks, and runs the copy() command.
</p>
*/
public class copy_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public copy_Command ()
{	super();
	setCommandName ( "Copy" );
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
    String NewTSID = parameters.getValue ( "NewTSID" );
	String TSID = parameters.getValue ( "TSID" );
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
        message = "The time series identifier for the time series to copy must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series identifier when defining the command."));
	}
    if ( (NewTSID == null) || NewTSID.equals("") ) {
        message = "The new time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a new time series identifier when defining the command.  " +
                "Previously was optional but is now required."));
    }
    else {
        try {
            TSIdent tsident = TSIdent.parseIdentifier( NewTSID );
            try { TimeInterval.parseInterval(tsident.getInterval());
            }
            catch ( Exception e2 ) {
                message = "NewTSID interval \"" + tsident.getInterval() + "\" is not a valid interval.";
                warning += "\n" + message;
                status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a valid interval when defining the command."));
            }
        }
        catch ( Exception e ) {
            // TODO SAM 2007-03-12 Need to catch a specific exception like
            // InvalidIntervalException so that more intelligent messages can be
            // generated.
            message = "NewTSID \"" + NewTSID + "\" is not a valid identifier." +
            "Use the command editor to enter required fields.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Use the command editor to enter required fields."));
        }
    }
    
    // Check for invalid parameters...
    Vector valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "NewTSID" );
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
	return (new copy_JDialog ( parent, this )).ok();
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
	String routine = "copy_Command.parseCommand", message;

	// Get the part of the command after the TS Alias =...
	int pos = command.indexOf ( "=" );
	if ( pos < 0 ) {
		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = Copy(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	String token0 = command.substring ( 0, pos ).trim();
	String token1 = command.substring ( pos + 1 ).trim();
	if ( (token0 == null) || (token1 == null) ) {
		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = Copy(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
    Vector v = StringUtil.breakStringList ( token0, " ", StringUtil.DELIM_SKIP_BLANKS );
    if ( v == null ) {
        message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = copy(TSID)";
        Message.printWarning ( warning_level, routine, message);
        throw new InvalidCommandSyntaxException ( message );
    }
    String Alias = (String)v.elementAt(1);
    String TSID = null;
	if ( (token1.indexOf('=') < 0) && !token1.endsWith("()") ) {
		// No parameters have = in them...
		// TODO SAM 2005-08-25 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax without named parameters.

		v = StringUtil.breakStringList ( token1,"(),",	StringUtil.DELIM_SKIP_BLANKS );
		if ( v == null ) {
			message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = copy(TSID)";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
        // TSID is the only parameter
        TSID = (String)v.elementAt(1);
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
    // Get from the parameters...
    TSID = parameters.getValue( "TSID");
    String NewTSID = parameters.getValue( "NewTSID");
    if ( (NewTSID == null) || (NewTSID.length() == 0) ) {
        // NewTSID is not specified.  The requirement that this be specified was added to
        // avoid confusion between copies and the original.  However, this has caused a lot
        // of migration issues.  Therefore, if TSID is specified, copy it to NewTSID and use
        // "copy" for the scenario.  This can't be done with aliases.
        if ( (TSID != null) && (TSID.length() > 0) ) {
            // Try to evaluate whether it is an alias..
            if ( StringUtil.patternCount(TSID, ".") >= 3 ) {
                // Probably not an alias so try to process
                try {
                    TSIdent ident = TSIdent.parseIdentifier ( TSID );
                    ident.setScenario ( "copy" );
                    // Set the new identifier
                    parameters.set ( "NewTSID", ident.toString(false) );
                }
                catch ( Exception e ) {
                    // Don't set the NewTSID and force the user to set it when command
                    // validation occurs.
                    // FIXME SAM 2008-05-29 Need to evaluate how serious this is.
                    Message.printWarning( 3, routine, "Unable to parse the TSID to use for NewTSID.");
                    Message.printWarning( 3, routine, e);
                }
            } 
        }
    }
    else {
        // Have NewTSID parameter but the interval may be invalid.  Copy from TSID if that is the case.
        try {
            TSIdent newident = TSIdent.parseIdentifier ( NewTSID );
            try {
                TimeInterval.parseInterval(newident.getInterval());
            }
            catch ( Exception e ) {
                // Bad interval in NewTSID so try to use the one from TSID.  First have to parse out TSID
                try {
                    TSIdent ident = TSIdent.parseIdentifier ( TSID );
                    // Make sure the interval is valid from the original (won't be able to get if Alias).
                    try {
                        TimeInterval.parseInterval(ident.getInterval());
                        newident.setInterval(ident.getInterval());
                        // Set the new identifier
                        parameters.set ( "NewTSID", newident.toString(false) );
                    }
                    catch ( Exception e3 ) {
                        // FIXME SAM 2008-05-29 Need to evaluate how serious this is.
                        Message.printWarning ( 3, routine, "Invalid TSID interval \"" + ident.getInterval() +
                                "\" to fill in NewTSID interval.");
                        Message.printWarning( 3, routine, e3 );
                    }
                }
                catch ( Exception e2 ) {
                    // Not able to parse the TSID so user will need to fix manually.
                    // FIXME SAM 2008-05-29 Need to evaluate how serious this is.
                    Message.printWarning ( 3, routine, "Unable to parse TSID to fill in NewTSID interval.");
                    Message.printWarning( 3, routine, e2 );
                }
            }
        }
        catch ( Exception e ) {
            // Don't set the NewTSID and force the user to set it when command
            // validation occurs.
            // FIXME SAM 2008-05-29 Need to evaluate how serious this is.
            Message.printWarning ( 3, routine, "Unable to parse NewTSID to check its interval." );
            Message.printWarning( 3, routine, e);
        }
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
{	String routine = "copy_Command.runCommand", message;
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
	String NewTSID = parameters.getValue ( "NewTSID" );

	// Get the time series to process.  The time series list is searched
	// backwards until the first match...

	TS ts = null;
	try {	PropList request_params = new PropList ( "" );
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
		message = "Unable to find time series to copy using TSID \"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
		throw new CommandWarningException ( message );
	}

	// Now process the time series...

	TS tscopy = null;
	try {
        tscopy = (TS)ts.clone();
		tscopy.setAlias ( Alias );	// Do separate because setting
						// the NewTSID might cause the
						// alias set to fail below.
	}
	catch ( Exception e ) {
		message = "Unexpected error trying to copy time series \""+ ts.getIdentifier() + "\".";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the log file - report the problem to software support." ) );
	}

	try {
        if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
			TSIdent tsident = new TSIdent ( NewTSID );
			tscopy.setIdentifier ( tsident );
		}
		tscopy.setAlias ( Alias );
	}
	catch ( Exception e ) {
		message = "Unexpected error setting the new time series identifier \"" + NewTSID + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the log file - report the problem to software support." ) );
	}

    // Update the data to the processor so that appropriate actions are taken...

    TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, tscopy );

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
	String Alias = props.getValue( "Alias" );
	String TSID = props.getValue( "TSID" );
	String NewTSID = props.getValue( "NewTSID" );
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewTSID=\"" + NewTSID + "\"" );
	}
	return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
}

}
