package rti.tscommandprocessor.commands.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;
import javax.swing.JFrame;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;

/**
<p>
This class initializes, checks, and runs the CreateRegressionTestCommandFile() command.
</p>
<p>The CommandProcessor must return the following properties:  WorkingDir.
</p>
*/
public class CreateRegressionTestCommandFile_Command extends AbstractCommand
implements Command
{

/**
Data members used for parameter values.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Constructor.
*/
public CreateRegressionTestCommandFile_Command ()
{	super();
	setCommandName ( "CreateRegressionTestCommandFile" );
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
{	String SearchFolder = parameters.getValue ( "SearchFolder" );
	//String FilenamePattern = parameters.getValue ( "FilenamePattern" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String Append = parameters.getValue ( "Append" );
	String warning = "";

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the parent directories or files is not checked
	// because the files may be created dynamically after the command is
	// edited.

	if ( (SearchFolder == null) || (SearchFolder.length() == 0) ) {
		warning += "\nThe search folder must be specified.";
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						"The search folder is not specified.",
						"Specify the search folder."));
	}
	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
		warning += "\nThe output file must be specified.";
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						"The output file is not specified.",
						"Specify the output file name."));
	}
	if ( (Append != null) && !Append.equals("") ) {
		if (	!Append.equalsIgnoreCase(_False) &&
			!Append.equalsIgnoreCase(_True) ) {
			warning += "\nThe Append parameter \"" +
			Append + "\" must be False or True.";
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						"The Append parameter (\"" + Append + ") is invalid.",
						"Specify the parameter as False or True."));
		}
	}

	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
	valid_Vector.add ( "SearchFolder" );
	valid_Vector.add ( "FilenamePattern" );
	valid_Vector.add ( "OutputFile" );
	valid_Vector.add ( "Append" );
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
		StringBuffer b = new StringBuffer();
		for ( int i = 0; i < size; i++ ) {
			warning += "\n" + (String)warning_Vector.elementAt (i);
			b.append ( (String)warning_Vector.elementAt(i));
		}
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.WARNING,
					b.toString(),
					"Specify only valid parameters - see documentation."));
	}
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
	else {
		status.addToLog(CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.SUCCESS,"",""));
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
	return (new CreateRegressionTestCommandFile_JDialog ( parent, this )).ok();
}
    
/**
Visits all files and directories under the given directory and if
the file matches a valid commands file it is added to the test list.
All commands file that end with ".<product_name>" will be added to the list.
@param dir Folder in which to start searching for command files.
@param pattern Pattern to match when searching files, for example "test*.TSTool".
@throws IOException 
 */
private void getMatchingFilenamesInTree ( Vector commands_file_Vector, File path, String pattern ) 
throws IOException
{
    if (path.isDirectory()) {
        String[] children = path.list();
        for (int i = 0; i < children.length; i++) 
        {
        	// Recursively call with full path using the directory and child name.
        	getMatchingFilenamesInTree(commands_file_Vector,new File(path,children[i]), pattern);
        }
    }
    else {
        //add to list if commands file is valid
    	Message.printStatus(2, "", "Checking path \"" + path.getName() + "\" against \"" + pattern + "\"" );
    	// Do comparison on file name without directory.
        if( path.getName().matches( pattern )
        		// FIXME SAM 2007-10-15 Need to enable something like the following to make more robust
        		//&& isValidCommandsFile( dir )
        		) {
        	Message.printStatus(2, "", "File matched." );
           commands_file_Vector.add(path.toString());
        }
    }
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
{	int warning_level = 2;
	String routine = getClass().getName() + ".parseCommand", message;

	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );

	if ( (tokens == null) ) { //|| tokens.size() < 2 ) {}
		message = "Invalid syntax for \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	// Get the input needed to process the command...
	if ( tokens.size() > 1 ) {
		try {	setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
				(String)tokens.elementAt(1), routine,"," ) );
		}
		catch ( Exception e ) {
			message = "Syntax error in \"" + command +
				"\".  Not enough tokens.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
	}
}

/**
Run the command.
@param command_line Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	
	String SearchFolder = parameters.getValue ( "SearchFolder" );
	String FilenamePattern = parameters.getValue ( "FilenamePattern" );
	String FilenamePattern_Java = "";
	if ( FilenamePattern == null ) {
		// The pattern we want is "test*.TSTool" where the . is literal.
		// For Java string matching, need to replace * with .* and . with \...
		FilenamePattern_Java = "^[tT]est.*\\x2eTSTool";
	}
	else {
		//FilenamePattern_Java = StringUtil.replaceString(FilenamePattern,"*",".*");
	}
	String OutputFile = parameters.getValue ( "OutputFile" );
	String Append = parameters.getValue ( "Append" );
	boolean Append_boolean = true;	// Default
	if ( (Append != null) && Append.equalsIgnoreCase(_False)){
		Append_boolean = false;
	}

	String SearchFolder_full = IOUtil.getPathUsingWorkingDir ( SearchFolder );
	String OutputFile_full = IOUtil.getPathUsingWorkingDir ( OutputFile );
	if ( !IOUtil.fileExists(SearchFolder_full) ) {
		message = "The folder to search \"" + SearchFolder_full +
			"\" does not exist.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					"The folder to search \"" + SearchFolder_full + "\" does not exist.",
					"Verify that the folder exists at the time the command is run."));
	}
	/* TODO SAM 2007-10-15 Need to check for parent folder
	if ( !IOUtil.fileExists(InputFile2_full) ) {
		message = "Second input file \"" + InputFile2_full +
			"\" does not exist.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					"Second input file \"" + InputFile2_full + "\" does not exist.",
					"Verify that the file exists at the time the command is run."));
	}
	*/
	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	try {
		// FIXME SAM 2007-10-15 Need to write appropriate header to file.
		// Open the output file...
		PrintWriter out = new PrintWriter(new FileOutputStream(OutputFile_full, Append_boolean));
		File OutputFile_full_File = new File(OutputFile_full);
		// Open a log file for the runner...
		out.println ( "StartLog(LogFile=\"" + OutputFile_full_File.getName() + ".log\")");
		// Find the list of matching files...
		Vector files = new Vector();
		getMatchingFilenamesInTree ( files, new File(SearchFolder_full), FilenamePattern_Java );
		int size = files.size();
		String commands_file_to_run;
		for ( int i = 0; i < size; i++ ) {
			// The commands files to run are relative to the commands file being created.
			commands_file_to_run = IOUtil.toRelativePath ( OutputFile_full_File.getParent(), (String)files.elementAt(i) );
			out.println ( "runCommands(InputFile=\"" + commands_file_to_run + "\")");
		}
		out.close();
	}
	catch ( Exception e ) {
		message = "Error creating regression commands file.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					"Unknown error creating regression test commands file.", "See the log file for details."));
		throw new CommandException ( message );
	}
}

/**
Return the string representation of the command.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String SearchFolder = parameters.getValue("SearchFolder");
	String FilenamePattern = parameters.getValue("FilenamePattern");
	String OutputFile = parameters.getValue("OutputFile");
	String Append = parameters.getValue("Append");
	StringBuffer b = new StringBuffer ();
	if ( (SearchFolder != null) && (SearchFolder.length() > 0) ) {
		b.append ( "SearchFolder=\"" + SearchFolder + "\"" );
	}
	if ( (FilenamePattern != null) && (FilenamePattern.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FilenamePattern=\"" + FilenamePattern + "\"" );
	}
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"");
	}
	if ( (Append != null) && (Append.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Append=" + Append );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
