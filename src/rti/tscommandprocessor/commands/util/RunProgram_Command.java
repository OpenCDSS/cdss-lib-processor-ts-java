package rti.tscommandprocessor.commands.util;

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
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ProcessManager;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the RunProgram() command.
*/
public class RunProgram_Command extends AbstractCommand
implements Command
{
    
/**
Number of arguments that can be added as ProgramArg# parameters.
*/
protected final int _ProgramArg_SIZE = 8;

/**
Constructor.
*/
public RunProgram_Command ()
{	super();
	setCommandName ( "RunProgram" );
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
{	String CommandLine = parameters.getValue ( "CommandLine" );
    String Program = parameters.getValue ( "Program" );
	String Timeout = parameters.getValue ( "Timeout" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
    if ( (CommandLine == null) || (CommandLine.length() == 0) ) {
        message = "The command line for the program to run must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the program command line to run."));
    }
    // TODO SAM 2009-04-05 For now allow command line and individual arguments to both be specified and
    // use the separate arguments first.
    // Timeout is not required
    if ( (Timeout != null) && (Timeout.length() != 0) && !StringUtil.isDouble(Timeout) ) {
        message = "TThe tmeout value \"" + Timeout + "\" is not a number.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the timeout as a number of seconds or leave blank to default to no timeout."));
    }

	// Check for invalid parameters...
    List valid_Vector = new Vector();
	valid_Vector.add ( "CommandLine" );
	valid_Vector.add ( "Program" );
	for ( int i = 0; i < _ProgramArg_SIZE; i++ ) {
	    valid_Vector.add ( "ProgramArg" + (i + 1) );
	}
	valid_Vector.add ( "Timeout" );
	valid_Vector.add ( "ExitStatusIndicator" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new RunProgram_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "RemoveFile_Command.parseCommand", message;

	if ( (command_string.indexOf("=") > 0) || command_string.endsWith("()") ) {
        // New syntax...
        super.parseCommand(command_string);
    }
    else {
        // Old syntax...
    	List tokens = StringUtil.breakStringList ( command_string,"(,)", StringUtil.DELIM_ALLOW_STRINGS );
        CommandStatus status = getCommandStatus();
        if ( (tokens == null) ) { //|| tokens.size() < 2 ) {}
            message = "Invalid syntax for \"" + command_string + "\".  Expecting RunProgram(...).";
            Message.printWarning ( warning_level, routine, message);
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to support."));
            throw new InvalidCommandSyntaxException ( message );
        }
        // Get the input needed to process the command...
        if ( tokens.size() != 3 ) { // Command name and 2 arguments
            message = "Invalid syntax for \"" + command_string + "\".  Expecting RunProgram(...).";
            Message.printWarning ( warning_level, routine, message);
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to support."));
            throw new InvalidCommandSyntaxException ( message );
        }
        else {
            // Set the parameters
            PropList parameters = getCommandParameters();
            parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
            parameters.set ( "CommandLine", (String)tokens.get(1) );
            parameters.set ( "TimeOut", (String)tokens.get(2) );
            parameters.setHowSet ( Prop.SET_UNKNOWN );
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
{	String routine = "RunProgram_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	
	String CommandLine = parameters.getValue ( "CommandLine" );
	String Program = parameters.getValue ( "Program" );
	String [] ProgramArg = new String[_ProgramArg_SIZE];
	for ( int i = 0; i < _ProgramArg_SIZE; i++ ) {
	    ProgramArg[i] = parameters.getValue ( "ProgramArg" + (i + 1) );
	}
	String Timeout = parameters.getValue ( "Timeout" );
	String ExitStatusIndicator = parameters.getValue ( "ExitStatusIndicator" );
    double Timeout_double = 0.0;
    if ( (Timeout != null) && (Timeout.length() > 0) ) {
        Timeout_double = Double.parseDouble(Timeout);
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	String CommandLine_full = CommandLine;
	String Program_full = Program;
	String [] ProgramArg_full = new String[_ProgramArg_SIZE];
	try {
        // Expand the command line to recognize processor-level properties like WorkingDir
        CommandLine_full = TSCommandProcessorUtil.expandParameterValue(processor,this,CommandLine);
        if ( Program != null ) {
            Program_full = TSCommandProcessorUtil.expandParameterValue(processor,this,Program);
        }
        for ( int i = 0; i < _ProgramArg_SIZE; i++ ) {
            if ( ProgramArg[i] != null ) {
                ProgramArg_full[i] = TSCommandProcessorUtil.expandParameterValue(processor,this,ProgramArg[i]);
            }
        }
        // Do the following if it is hard to track what is going on...
        //PropList props = new PropList ( "PM" );
        //ProcessManagerDialog pmg = new ProcessManagerDialog ( program, props );
        //props = null;
        // pmg = null;
        // Normally can do this, although TSTool may sit for awhile until the
        // process is finished (need to figure out a way to make TSTool wait on the thread without hanging).
        ProcessManager pm = null;
        if ( (Program != null) && !Program.equals("") ) {
            // Specify the program to run using individual strings.
            List programAndArgsList = new Vector();
            programAndArgsList.add ( Program_full );
            for ( int i = 0; i < _ProgramArg_SIZE; i++ ) {
                if ( ProgramArg_full[i] != null ) {
                    programAndArgsList.add (ProgramArg_full[i]);
                }
            }
            pm = new ProcessManager ( StringUtil.toArray(programAndArgsList), (int)(Timeout_double*1000.0), ExitStatusIndicator);
            //CommandLine_full, (int)(Timeout_double*1000.0), ExitStatusIndicator);
        }
        else {
            // Specify the command to run using a full command line
            pm = new ProcessManager (CommandLine_full, (int)(Timeout_double*1000.0), ExitStatusIndicator);
        }
        pm.saveOutput ( true ); // Save output so it can be used in troubleshooting
        pm.run();
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
	}
	catch ( Exception e ) {
		message = "Unexpected error running program \"" + CommandLine + "\" (expanded=" + CommandLine_full + ") (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
		throw new CommandException ( message );
	}

	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String CommandLine = parameters.getValue("CommandLine");
	String Program = parameters.getValue("Program");
	String [] ProgramArg = new String[_ProgramArg_SIZE];
	for ( int i = 0; i < _ProgramArg_SIZE; i++ ) {
	    ProgramArg[i] = parameters.getValue("ProgramArg" + (i + 1));
	}
	String Timeout = parameters.getValue("Timeout");
	String ExitStatusIndicator = parameters.getValue("ExitStatusIndicator");
	StringBuffer b = new StringBuffer ();
	if ( (CommandLine != null) && (CommandLine.length() > 0) ) {
		b.append ( "CommandLine=\"" + CommandLine + "\"" );
	}
    if ( (Program != null) && (Program.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Program=" + Program );
    }
    for ( int i = 0; i < _ProgramArg_SIZE; i++ ) {
        if ( (ProgramArg[i] != null) && (ProgramArg[i].length() > 0) ) {
            if ( b.length() > 0 ) {
                b.append ( "," );
            }
            b.append ( "ProgramArg" + (i + 1) + "=" + ProgramArg[i] );
        } 
    }
	if ( (Timeout != null) && (Timeout.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Timeout=" + Timeout );
	}
    if ( (ExitStatusIndicator != null) && (ExitStatusIndicator.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExitStatusIndicator=\"" + ExitStatusIndicator + "\"");
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
