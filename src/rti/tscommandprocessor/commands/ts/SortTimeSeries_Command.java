//------------------------------------------------------------------------------
// sortTimeSeries_Command - handle the sortTimeSeries() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-05-03	Steven A. Malers, RTi	Initial version.
// 2005-05-11	SAM, RTi		Update initialize() to not call
//					parseCommand() since the base class
//					method does it.
// 2005-05-19	SAM, RTi		Move from TSTool package to TS.
// 2007-02-11	SAM, RTi		Verify consistency with new TSCommandProcessor.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import java.util.Vector;

import javax.swing.JFrame;

import RTi.TS.TSUtil;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.PropList;


/**
<p>
This class initializes, checks, and runs the SortTimeSeries() command.
</p>
*/
public class SortTimeSeries_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public SortTimeSeries_Command ()
{	super();
	setCommandName ( "SortTimeSeries" );
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
{	// Currently no parameters for this command
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
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
	return (new SortTimeSeries_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	
    setCommandParameters ( new PropList ( getCommandName() ) );
}

/**
Run the commands:
<pre>
sortTimeSeries()
</pre>
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws CommandWarningException, CommandException
{	String routine = "SortTimeSeries_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	Vector tslist = null;
	
    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	
	try { Object o = processor.getPropContents ( "TSResultsList" );
			tslist = (Vector)o;
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		message = "Error requesting TSResultsList from processor.";
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report to software support." ) );
		Message.printWarning(3, routine, message );
	}
	int warning_count = 0;
	if ( (tslist == null) || (tslist.size() == 0) ) {
		// Don't do anything...
		Message.printStatus ( 2, routine,"No time series are available.  Not sorting." );
	}
    else {
        try {	Vector tslist_sorted = TSUtil.sort ( tslist );
                processor.setPropContents ( "TSResultsList", tslist_sorted );
        }
        catch ( Exception e ) {
            message = "Unexpected error sorting time series (" + e + ").";
            Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag, ++warning_count),
                    routine, message );
            Message.printWarning ( 3, routine, e );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report to software support." ) );
            throw new CommandException ( message );
	   }
    }
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

// Can rely on base class for toString().

}
