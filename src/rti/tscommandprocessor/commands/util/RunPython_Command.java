package rti.tscommandprocessor.commands.util;

import java.io.File;
import java.io.StringWriter;
import java.lang.ClassLoader;
import java.net.URL;
import java.util.Properties;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import org.python.util.PythonInterpreter; 
import org.python.core.PyException;
import org.python.core.PySyntaxError;
import org.python.core.PySystemState;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
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
import RTi.Util.IO.ProcessManager;
import RTi.Util.IO.PropList;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

/**
<p>
This class initializes, checks, and runs the RunPython() command.
</p>
*/
public class RunPython_Command extends AbstractCommand implements Command
{
    
/**
Interpreter parameter value indicating that Python should be called to run script.
*/
protected String _Python = "Python";

/**
Interpreter parameter value indicating that Jython should be called to run script.
*/
protected String _Jython = "Jython";
    
/**
Indicate whether Jython has been initialized.
*/
private static boolean __Jython_initialized = false;

/**
Constructor.
*/
public RunPython_Command ()
{	super();
	setCommandName ( "RunPython" );
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
    String Interpreter = parameters.getValue ( "Interpreter" );
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
	else {
	    String working_dir = null;
	
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
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath ( working_dir, InputFile));
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
            "\" cannot be adjusted to an absolute path using the working directory \"" +
            working_dir + "\".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that command file to run and working directory paths are compatible." ) );
		}
	}
	
    if ( (Interpreter == null) || (Interpreter.length() == 0) ) {
        message = "The interpreter is not specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an interpreter to use, one of " + _Python + " or " + _Jython ) );
    }
    else if ( !Interpreter.equalsIgnoreCase(_Python) && !Interpreter.equalsIgnoreCase(_Jython) ) {
        message = "The interpreter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an interpreter to use, one of " + _Python + " or " + _Jython ) );
    }

	// Check for invalid parameters...
    List valid_Vector = new Vector();
	valid_Vector.add ( "Arguments" );
	valid_Vector.add ( "InputFile" );
	valid_Vector.add ( "Interpreter" );
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
	return (new RunPython_JDialog ( parent, this )).ok();
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
{	String routine = "RunPython_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	PropList parameters = getCommandParameters();

	String InputFile = parameters.getValue ( "InputFile" );
	String Arguments = parameters.getValue ( "Arguments" );
	String Interpreter = parameters.getValue ( "Interpreter" );
	
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
        InputFile_full = IOUtil.verifyPathForOS(IOUtil.adjustPath ( WorkingDir, InputFile) );
        if ( !IOUtil.fileExists(InputFile_full) ) {
            message = "Python script file \"" + InputFile_full + "\" does not exist.";
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify the location of the script file." ) );
        }
		Message.printStatus ( 2, routine,
		"Processing Python script file \"" + InputFile_full + "\" using " + Interpreter + " interpreter.");
        
		if ( Interpreter.equalsIgnoreCase(_Python) ) {
		    warning_count = runPythonScript ( command_tag, warning_count,
		            InputFile_full, Arguments, WorkingDir );
		    Message.printStatus ( 2, routine,"...done processing Python file." );
		}
		else if ( Interpreter.equalsIgnoreCase(_Jython) ) {
		        runJythonScript ( command_tag, InputFile_full,
		            StringUtil.toArray(StringUtil.breakStringList(Arguments, " ", 0)), WorkingDir );
		        Message.printStatus ( 2, routine,"...done processing Python file." );
		}
	}
    catch ( PySyntaxError e ) {
        message = "Syntax error in Python file \"" + InputFile_full + "\" (" + e + ").";
        Message.printWarning ( 3, routine, e );
        Message.printWarning ( warning_level, 
        MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify Python script.  See the log file for Python error and output messages."));
        throw new CommandException ( message );
    }
    catch ( PyException e ) {
        message = "Python error processing Python file \"" + InputFile_full + "\" (" + e + ").";
        Message.printWarning ( 3, routine, e );
        Message.printWarning ( warning_level, 
        MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify Python script.  See the log file for Python error and output messages."));
        throw new CommandException ( message );
    }
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error processing Python file \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),	routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details." ) );
		throw new CommandException ( message );
	}
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Run the Python script using the Jython interpreter.
@param command_tag Command tag for logging.
@param pyfile Python file to run.
@param args List of command line arguments for script.
@param WorkingDir The working directory in which to run the script.
*/
private void runJythonScript ( String command_tag, String pyfile, String [] args, String WorkingDir )
throws PyException, Exception
{   String routine = "RunPython.runJythonScript";
    // Only need to do this once to initialize the default state.
    if ( !__Jython_initialized ) {
        // This passes to the interpreter the environment information from the application
        // Do not set the script name because that may cause confusion.  It will be set below
        // with the command line arguments.
        Properties postProperties = new Properties();
        // TODO SAM 2008-06-25 Change to use a folder in the user's home once TSTool
        // setup is reconfigured to have personal files during install.
        // Use a cachedir in a temporary directory that should be visible and write-able for the
        // user.
        String cachedir = System.getProperty("java.io.tmpdir") +
            System.getProperty("file.separator") + System.getProperty("user.name") + "-jythoncache";
        Message.printStatus( 2, routine, "Initializing Jython interpreter with python.cachdir=\"" +
                cachedir + "\"" );
        postProperties.setProperty ( "python.cachedir", cachedir );
        PythonInterpreter.initialize(System.getProperties(), postProperties, null );
    }
    // Create a new system state for this particular script.
    // The general properties will be inherited from the system properties passed in above.
    // specify new script name and command line parameters based on this specific call.
    PySystemState state = new PySystemState();
    // Add the path to the jar'ed Jython/Lib files, assuming that this file is named
    // jython-lib.jar and is in the same place as jython.jar.
    URL url = ClassLoader.getSystemResource("org/python/util/PythonInterpreter.class");
    String path = url.getPath();
    Message.printStatus( 2, routine, "PythonInterpeter is in: " + path );
    // The path will be something like "File:/C:/path.../jython.jar!...class
    // Replace "/jython.jar" with "/jython-lib.jar" and strip out extra path info
    String libpath = StringUtil.replaceString(path, "/jython.jar", "/jython-lib.jar" );
    libpath = libpath.substring(6,libpath.indexOf("!"));
    Message.printStatus(2, routine, "Adding the following to Python path for state: \"" + libpath + "\"" );
    state.path.add ( libpath );
    
    String [] argv = new String[1 + args.length];
    argv[0] = pyfile;   // Script name
    for ( int i = 1; i <= args.length; i++ ) {
        // Arguments to the script...
        argv[i] = TSCommandProcessorUtil.expandParameterValue(getCommandProcessor(), this, args[i - 1] );
    }
    state.argv.addAll(Arrays.asList(argv));
    PythonInterpreter interp = new PythonInterpreter(null,state);

    // Tell Jython where to send its output so it can be captured and logged.
    // Use the same writer for output and error so that it is intermingled in the correct
    // order in output.
    StringWriter out = new StringWriter();
    interp.setOut( out );
    interp.setErr( out );
    // For troubleshooting
    interp.exec ( "import sys" );
    interp.exec ( "import os" );
    interp.exec ( "print '\"sys.argv\"=' + str(sys.argv)" );
    interp.exec ( "print '\"os.getcwd()\"=\"' + os.getcwd() + '\"'" );
    // Now execute the script.  An error will result in PyException being thrown and
    // caught in the calling code.
    try {
        interp.execfile ( pyfile );
    }
    catch ( Exception e ) {
        // Make sure to capture the Python output...
        Message.printStatus( 2, routine, "Output and error from the Jython script follows:" );
        Message.printStatus(2, routine, out.toString());
        // Rethrow the exception
        throw ( e );
    }
    Message.printStatus( 2, routine, "Output and error from the Jython script follows:" );
    Message.printStatus(2, routine, out.toString());
}

/**
Run the Python script by making a system call to Python.
@param pyfile Python file to run.
@param Arguments Arguments to pass to Python.
@param WorkingDir The working directory in which to run the script.
*/
private int runPythonScript ( String command_tag, int warning_count,
        String pyfile, String Arguments, String WorkingDir )
throws PyException
{   String routine = "RynPython_Command.runPythonScript", message;
    String args = TSCommandProcessorUtil.expandParameterValue(getCommandProcessor(), this, Arguments );
    // Expand ${} parameters...
    String commandLine = "python \"" + pyfile + "\" " + args;
    int warning_level = 2;
    
    Message.printStatus ( 2, routine, "Running:  " + commandLine );
    ProcessManager pm = new ProcessManager ( commandLine );
    pm.saveOutput ( true );
    pm.run();
    CommandStatus status = getCommandStatus();
    if ( pm.getExitStatus() == 996 ) {
        message = "Program \"" + commandLine + "\" timed out.  Full output may not be available.";
        Message.printWarning ( warning_level, 
        MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Check the log file and verify running the program on the command " +
                    		"line before running in TSTool."));
    }
    else if ( pm.getExitStatus() > 0 ) {
        message = "Program \"" + commandLine + "\" exited with status " + pm.getExitStatus() +
        ".  Full output may not be available.";
        Message.printWarning ( warning_level, 
        MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Check the log file and verify running the program on the command " +
                    		"line before running in TSTool."));
    }
    // Echo the output to the log file.
    List output = pm.getOutputList();
    int size = 0;
    if ( output != null ) {
        size = output.size();
    }
    for ( int i = 0; i < size; i++ ) {
        Message.printStatus(2, routine, "Program output:  " + output.get(i));
    }
    return warning_count;
}

}