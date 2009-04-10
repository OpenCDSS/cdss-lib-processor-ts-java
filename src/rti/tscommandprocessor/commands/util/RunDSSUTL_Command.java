package rti.tscommandprocessor.commands.util;

import java.io.File;
import java.util.List;
import java.util.Vector;
import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ProcessManager;
import RTi.Util.IO.ProcessRunner;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the RunDSSUTL() command.
*/
public class RunDSSUTL_Command extends AbstractCommand
implements Command, ProcessRunner
{
    
/**
Process that is run.
*/
private volatile Process __process = null;
    
/**
Constructor.
*/
public RunDSSUTL_Command ()
{	super();
	setCommandName ( "RunDSSUTL" );
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
{	String routine = getClass().getName() + ".checkCommandParameters";
    String DssFile = parameters.getValue ( "DssFile" );
    String InputFile = parameters.getValue ( "InputFile" );
    String OutputFile = parameters.getValue ( "OutputFile" );
	//String DssutlProgram = parameters.getValue ( "DssutlProgram" );
	String warning = "";
	String message;

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
    if ( (DssFile == null) || (DssFile.length() == 0) ) {
        message = "The HEC-DSS file must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the HEC-DSS file."));
    }
    else {
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
            // Working directory is available so use it...
            if ( o != null ) {
                working_dir = (String)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            Message.printWarning(3, routine, message );
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an existing input file." ) );
        }
    
        try {
            //String adjusted_path = 
            IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                    TSCommandProcessorUtil.expandParameterValue(processor,this,DssFile)));
        }
        catch ( Exception e ) {
            message = "The HEC-DSS file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the HEC-DSS file and working directory paths are compatible." ) );
        }
    }
    
    if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the input file."));
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
                Message.printWarning(3, routine, message );
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
            }
    
        try {
            //String adjusted_path = 
            IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                    TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        }
        catch ( Exception e ) {
            message = "The input file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the input file and working directory paths are compatible." ) );
        }
    }
    
    // Output file is optional
    if ( (OutputFile != null) && (OutputFile.length() != 0) ) {
        String working_dir = null;
        try { Object o = processor.getPropContents ( "WorkingDir" );
            if ( o != null ) {
                working_dir = (String)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Software error - report the problem to support." ) );
        }

        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                    TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The output file parent directory does not exist for: \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Create the output directory." ) );
            }
            f = null;
            f2 = null;
        }
        catch ( Exception e ) {
            message = "The output file:\n" +
            "    \"" + OutputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that output file and working directory paths are compatible." ) );
        }
    }
    
	// Check for invalid parameters...
    List valid_Vector = new Vector();
	valid_Vector.add ( "DssFile" );
	valid_Vector.add ( "InputFile" );
	valid_Vector.add ( "OutputFile" );
	valid_Vector.add ( "DssutlProgram" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new RunDSSUTL_JDialog ( parent, this )).ok();
}

/**
Return the process started by this command.
*/
private Process getProcess()
{
    return __process;
}

/**
Get the list of processes managed by this class.
*/
public List<Process> getProcessList()
{
    List processList = new Vector(1);
    Process process = getProcess();
    if ( process != null ) {
        processList.add ( process );
    }
    return processList;
}

/**
Run the command.
@param command_line Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	
	// Set the process instance to null
	setProcess ( null );
	
	String DssutlProgram = parameters.getValue ( "DssutlProgram" );
	if ( (DssutlProgram == null) || DssutlProgram.equals("") ) {
	    DssutlProgram = "DSSUTL.EXE"; // Default
	}
	String DssFile = parameters.getValue ( "DssFile" );
	String InputFile = parameters.getValue ( "InputFile" );
	int narray = 3; // Number of items in the command array:  DssutlProgram, DssFile, and InputFile
	String OutputFile = parameters.getValue ( "OutputFile" );
	if ( (OutputFile != null) && !OutputFile.equals("") ) {
	    ++narray;
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	// Default command line, which will be expanded below to handle ${WorkingDir}
    String DssutlProgram_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            TSCommandProcessorUtil.expandParameterValue(processor,this,DssutlProgram)));
	String DssFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            TSCommandProcessorUtil.expandParameterValue(processor,this,DssFile)));
    String InputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
    String OutputFile_full = OutputFile;
    if ( (OutputFile != null) && !OutputFile.equals("") ) {
        OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
    }
	String CommandLine_full = DssutlProgram_full + " DSSFILE=" + DssFile_full + " INPUT=" + InputFile_full +
	    " OUTPUT=" + OutputFile_full;
	try {
        // Expand the command line to recognize processor-level properties like WorkingDir
        CommandLine_full = TSCommandProcessorUtil.expandParameterValue(processor,this,CommandLine_full);

        // Run the program using an array.
        ProcessManager pm = null;
        String [] commandArray = new String[narray];
        int iarg = 0;
        commandArray[iarg++] = DssutlProgram_full;
        commandArray[iarg++] = "DSSFILE=" + DssFile_full;
        commandArray[iarg++] = "INPUT=" + InputFile_full;
        if ( (OutputFile != null) && !OutputFile.equals("") ) {
            commandArray[iarg++] = "OUTPUT=" + OutputFile_full;
        }
        // Run the program.  Timeout after an hour.  Use "Status:" in output lines to determine the status.
        pm = new ProcessManager ( commandArray, 3600, "Status:", false );
        pm.saveOutput ( true ); // Save output so it can be used in troubleshooting
        pm.run();
        setProcess ( pm.getProcess() );
        Message.printStatus ( 2, routine, "Exit status from program = " + pm.getExitStatus() );
        if ( pm.getExitStatus() == 996 ) {
            message = "Program \"" + CommandLine_full + "\" timed out.  Full output may not be available.";
            Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify running the program on the command line before running in TSTool."));
        }
        else if ( pm.getExitStatus() != 0 ) {
            message = "Program \"" + CommandLine_full + "\" exited with status " + pm.getExitStatus() +
            ".  Full output may not be available.  Output from program is:\n" +
            StringUtil.toString(pm.getOutputList(),"\n") + "\nStandard error from program is:\n" +
            StringUtil.toString(pm.getErrorList(),"\n");
            Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify running the program on the command line before running in TSTool."));
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
        // Everything finished so set the process instance to null so other code cannot do anything with the
        // completed process.
        setProcess ( null );
	}
	catch ( Exception e ) {
		message = "Unexpected error running DSSUTL \"" + CommandLine_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
		    message, "See the log file for details."));
		throw new CommandException ( message );
	}

	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the process that is being run by the ProcessManager.  This can be retrieved and killed if the process hangs.
@param process process that is being run.
*/
private void setProcess ( Process process )
{
    __process = process;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String DssFile = parameters.getValue("DssFile");
	String InputFile = parameters.getValue("InputFile");
	String OutputFile = parameters.getValue("OutputFile");
	String DssutlProgram = parameters.getValue("DssutlProgram");
	StringBuffer b = new StringBuffer ();
	if ( (DssFile != null) && (DssFile.length() > 0) ) {
		b.append ( "DssFile=\"" + DssFile + "\"" );
	}
    if ( (InputFile != null) && (InputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputFile=\"" + InputFile + "\"");
    }
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"");
    }
    if ( (DssutlProgram != null) && (DssutlProgram.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DssutlProgram=\"" + DssutlProgram + "\"");
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}