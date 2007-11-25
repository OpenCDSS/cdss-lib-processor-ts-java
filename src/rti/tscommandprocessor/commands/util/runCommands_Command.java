package rti.tscommandprocessor.commands.util;

import java.io.File;
import java.util.Vector;

import javax.swing.JFrame;

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
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;

import RTi.TS.TS;
import rti.tscommandprocessor.core.TSCommandFileRunner;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

/**
<p>
This class initializes, checks, and runs the runCommands() command.
</p>
*/
public class runCommands_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public runCommands_Command ()
{	super();
	setCommandName ( "RunCommands" );
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
{	String InputFile = parameters.getValue ( "InputFile" );
	String warning = "";
    String message;
	
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an input file." ) );
	}
	else {	String working_dir = null;
	
			try { Object o = processor.getPropContents ( "WorkingDir" );
					// Working directory is available so use it...
					if ( o != null ) {
						working_dir = (String)o;
					}
			}
			catch ( Exception e ) {
				message = "Error requesting WorkingDir from processor.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Software error - report problem to support." ) );
			}
	
		try {
            String adjusted_path = IOUtil.adjustPath ( working_dir, InputFile);
			File f = new File ( adjusted_path );
			if ( !f.exists() ) {
                message = "The input file does not exist: \"" + adjusted_path + "\".";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the command file to run exists." ) );
            }
			f = null;
		}
		catch ( Exception e ) {
            message = "The input file \"" + InputFile +
            "\" ncannot be adjusted to an absolute path using the working directory \"" +
            working_dir + "\".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that command file to run and working directory paths are compatible." ) );
		}
	}

	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
	valid_Vector.add ( "InputFile" );
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
	return (new runCommands_JDialog ( parent, this )).ok();
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "RunCommands_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	PropList parameters = getCommandParameters();

	String InputFile = parameters.getValue ( "InputFile" );
	String AppendResults = parameters.getValue ( "AppendResults" );
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	// Get the working directory from the processor that is running the commands.

	String WorkingDir = TSCommandProcessorUtil.getWorkingDir(processor);
	String InputFile_full = null;
	try {
        InputFile_full = IOUtil.adjustPath ( WorkingDir, InputFile);
		Message.printStatus ( 2, routine,
		"Processing commands from file \"" + InputFile_full + "\" using command file runner.");
		
		TSCommandFileRunner runner = new TSCommandFileRunner ();
        // This will set the initial working directory of the runner to that of the command file...
		runner.readCommandFile(InputFile_full);
		runner.runCommands();
		
		// Set the CommandStatus for this command to the most severe status of the
		// commands file that was just run.
		CommandStatusType max_severity = TSCommandProcessorUtil.getCommandStatusMaxSeverity((TSCommandProcessor)runner.getProcessor());
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(
						max_severity,
						"Severity is max of commands file that was run (may not be a problem).",
						"See below for more and refer to log file if warning/failure."));
        // FIXME SAM 2007-11-24 Need to figure out how to pass on status log records from failing command files.
        // Copy status logs below...
        //CommandStatusUtil.copyLogRecords ( status, )
        
        // Also add a record to the regression report...
        
        TSCommandProcessorUtil.appendToRegressionTestReport(processor,max_severity,InputFile_full);
				
		// If it was requested to append the results to the calling processor, get
		// the results from the runner and do so...
		
		if ( (AppendResults != null) && AppendResults.equalsIgnoreCase("true")) {
			TSCommandProcessor processor2 = runner.getProcessor();
			Object o_tslist = processor2.getPropContents("TSResultsList");
			PropList request_params = new PropList ( "" );
			if ( o_tslist != null ) {
				Vector tslist = (Vector)o_tslist;
				int size = tslist.size();
				TS ts;
				for ( int i = 0; i < size; i++ ) {
					ts = (TS)tslist.elementAt(i);
					request_params.setUsingObject( "TS", ts );
					processor.processRequest( "AppendTimeSeries", request_params );
				}
			}
		}
		
		Message.printStatus ( 2, routine,"...done processing commands from file." );
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Error processing commands file \"" + InputFile + "\", full path=\"" + InputFile_full + "\".";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new CommandException ( message );
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
	String InputFile = props.getValue("InputFile");
	StringBuffer b = new StringBuffer ();
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
