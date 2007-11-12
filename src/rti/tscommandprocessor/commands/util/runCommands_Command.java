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
<p>The CommandProcessor must return the following properties:
WorkingDir.
</p>
*/
public class runCommands_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public runCommands_Command ()
{	super();
	setCommandName ( "runCommands" );
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
public void checkCommandParameters (	PropList parameters, String command_tag,
					int warning_level )
throws InvalidCommandParameterException
{	String InputFile = parameters.getValue ( "InputFile" );
	String warning = "";
	
	CommandProcessor processor = getCommandProcessor();
	
	if ( (InputFile == null) || (InputFile.length() == 0) ) {
		warning += "\nThe input file must be specified.";
	}
	else {	String working_dir = null;
	
			try { Object o = processor.getPropContents ( "WorkingDir" );
					// Working directory is available so use it...
					if ( o != null ) {
						working_dir = (String)o;
					}
			}
			catch ( Exception e ) {
				// Not fatal, but of use to developers.
				String message = "Error requesting WorkingDir from processor - not using.";
				String routine = getCommandName() + ".checkCommandParameters";
				Message.printDebug(10, routine, message );
			}
	
		try {	String adjusted_path = IOUtil.adjustPath ( working_dir, InputFile);
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				warning +=
				"\nThe input file parent directory does " +
				"not exist: \"" + adjusted_path + "\".";
			}
			f = null;
			f2 = null;
		}
		catch ( Exception e ) {
			warning +=
				"\nThe working directory:\n" +
				"    \"" + working_dir +
				"\"\ncannot be adjusted using:\n" +
				"    \"" + InputFile + "\".";
		}
	}

	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
	valid_Vector.add ( "InputFile" );
	Vector warning_Vector = null;
	try {	warning_Vector = parameters.validatePropNames (
			valid_Vector, null, null, "parameter" );
	}
	catch ( Exception e ) {
		// Ignore.  Should not happen.
		warning_Vector = null;
	}
	if ( warning_Vector != null ) {
		int size = warning_Vector.size();
		for ( int i = 0; i < size; i++ ) {
			warning += "\n" + (String)warning_Vector.elementAt (i);
		}
	}

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),
		warning );
		throw new InvalidCommandParameterException ( warning );
	}
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
Run the commands:
<pre>
runCommands(InputFile="X")
</pre>
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "runCommands_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	
	CommandProcessor processor = getCommandProcessor();
	PropList parameters = getCommandParameters();

	String InputFile = parameters.getValue ( "InputFile" );
	String AppendResults = parameters.getValue ( "AppendResults" );
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	// Get the working directory from the processor that is running the commands.

	String WorkingDir = null;
	try { Object o = processor.getPropContents ( "WorkingDir" );
		// Working directory is available so use it...
		if ( o != null ) {
			WorkingDir = (String)o;
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		message = "Error requesting WorkingDir from processor - not using.";
		Message.printDebug(10, routine, message );
	}
	if ( WorkingDir == null ) {
		// Use the aplication working directory...
		WorkingDir = IOUtil.getProgramWorkingDir ();
	}

	String fullname = null;
	String WorkingDir_save = null;
	try {
		fullname = IOUtil.adjustPath ( WorkingDir, InputFile);
	
		//String fullname = InputFile;
		//fullname = IOUtil.getPathUsingWorkingDir ( InputFile );
	
		//try {	// Read the commands file as a Vector of String...


		//Vector commands = IOUtil.fileToStringList ( fullname );

		// Process the commands using the current commands processor

		//PropList props2 = new PropList ( "" );
		//props2.set ( "Recursive=True" );
		
		// Set the global working directory to the location of the commands
		// file.  This is used by some low-level code to complete file paths.
		// Since this code is not getting the dynamic working directory, it is
		// necessary to reset the global value temporarily (this is not thread-safe!).
		
		WorkingDir_save = IOUtil.getProgramWorkingDir();
		File fullname_File = new File ( fullname );
		IOUtil.setProgramWorkingDir ( fullname_File.getParent() );
		
		Message.printStatus ( 2, routine,
		"Processing commands from file \"" + fullname.toString() + "\" using command file runner.");
		
		TSCommandFileRunner runner = new TSCommandFileRunner ();
		runner.readCommandFile(fullname);
		runner.runCommands();
		
		// Set the CommandStatus for this command to the most severe status of the
		// commands file that was just run.
		
		CommandStatus status = getCommandStatus();
		status.clearLog( CommandPhaseType.RUN );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(
						TSCommandProcessorUtil.getCommandStatusMaxSeverity((TSCommandProcessor)runner.getProcessor()),
						"Severity is max of commands file that was run (may not be a problem).",
						"Refer to log file if warning/failure."));
		
		/* TODO SAM 2007-10-11 Remove if code tests out
		PropList request_params = new PropList ( "" );
		request_params.setUsingObject ( "Commands", commands );
		request_params.setUsingObject ( "Properties", props2 );
		try { processor.processRequest( "ProcessCommands", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting ProcessCommands() from processor.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
		}
		*/
		
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
		
		Message.printStatus ( 2, routine,
		"...done processing commands from file." );
		
		// Reset the working directory...
		
		IOUtil.setProgramWorkingDir ( WorkingDir_save );
	}
	catch ( Exception e ) {
		// Reset the working directory...
		//IOUtil.setProgramWorkingDir ( workingdir_save );
		Message.printWarning ( 3, routine, e );
		message = "Error processing commands file \"" + InputFile +
		"\", full path=\"" + fullname + "\".";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		// Also execute this to make sure working directory gets reset OK.
		if ( WorkingDir_save != null ) {
			IOUtil.setProgramWorkingDir ( WorkingDir_save );
		}
		throw new CommandException ( message );
	}
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
