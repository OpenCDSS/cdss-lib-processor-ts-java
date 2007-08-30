//------------------------------------------------------------------------------
// runCommands_Command - handle the runCommands() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2006-05-02	Steven A. Malers, RTi	Initial version.  Copy and modify
//					readStateMod().
// 2007-02-12	SAM, RTi				Remove dependence on TSCommandProcessor.
//					Update code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import java.io.File;
import java.util.Vector;

import javax.swing.JFrame;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.IO.SkeletonCommand;

/**
<p>
This class initializes, checks, and runs the runCommands() command.
</p>
<p>The CommandProcessor must return the following properties:
WorkingDir.
</p>
*/
public class runCommands_Command extends SkeletonCommand implements Command
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
	
	if ( (InputFile == null) || (InputFile.length() == 0) ) {
		warning += "\nThe input file must be specified.";
	}
	else {	String working_dir = null;
	
			try { Object o = _processor.getPropContents ( "WorkingDir" );
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
	
		try {	String adjusted_path = IOUtil.adjustPath (
				working_dir, InputFile);
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
@param processor The CommandProcessor that is executing the command, which will
provide necessary data inputs and receive output(s).
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( String command_tag, int warning_level )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "runCommands_Command.runCommand", message;
	int warning_count = 0;

	String InputFile = _parameters.getValue ( "InputFile" );

	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Save the working directory because each commands file needs to run
	// with the working directory correspondig to the commands directory...
	String workingdir_save = IOUtil.getProgramWorkingDir();
	try {	// Read the commands file as a Vector of String...

		Message.printStatus ( 2, routine,
		"Reading commands file \"" + InputFile + "\"" );

		String fullname = InputFile;
	
		fullname = IOUtil.getPathUsingWorkingDir ( InputFile );
		Vector commands = IOUtil.fileToStringList ( fullname );

		// Process the commands using the current commands processor

		PropList props2 = new PropList ( "" );
		props2.set ( "Recursive=True" );
		// Set the working directory to the location of the commands
		// file...
		File fullname_File = new File ( fullname );
		IOUtil.setProgramWorkingDir ( fullname_File.getParent() );
		Message.printStatus ( 2, routine,
		"Processing commands from file " + fullname.toString() );
		
		PropList request_params = new PropList ( "" );
		request_params.setUsingObject ( "Commands", commands );
		request_params.setUsingObject ( "Properties", props2 );
		try { _processor.processRequest( "ProcessCommands", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting ProcessCommands() from processor.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
		}
		
		Message.printStatus ( 2, routine,
		"...done processing commands from file." );
		// Reset the working directory...
		IOUtil.setProgramWorkingDir ( workingdir_save );
	}
	catch ( Exception e ) {
		// Reset the working directory...
		IOUtil.setProgramWorkingDir ( workingdir_save );
		Message.printWarning ( 3, routine, e );
		message = "Error processing commands file.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
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
