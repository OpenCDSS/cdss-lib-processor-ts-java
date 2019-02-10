// AbstractCommand - This class initializes, checks, and runs the RunProgram() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package rti.tscommandprocessor.commands.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ProcessManager;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the RunProgram() command.
*/
public class RunProgram_Command extends AbstractCommand
implements Command
{
    
/**
Possible values for UseCommandShell parameter.
*/
protected final String _False = "False";
protected final String _True = "True";
    
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
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String CommandLine = parameters.getValue ( "CommandLine" );
    String Program = parameters.getValue ( "Program" );
    String UseCommandShell = parameters.getValue ( "UseCommandShell" );
	String Timeout = parameters.getValue ( "Timeout" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
    if ( ((CommandLine == null) || (CommandLine.length() == 0)) &&
        ((Program == null) || (Program.length() == 0)) ) {
        message = "The command line or the program name for the program to run must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the program command line to run."));
    }
    
    if ( UseCommandShell != null && !UseCommandShell.equalsIgnoreCase("") &&
        !UseCommandShell.equalsIgnoreCase("True") && !UseCommandShell.equalsIgnoreCase("False")) {
        message = "The UseCommandShell parameter value (" + UseCommandShell + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify UseCommandShell as " + _False + " or " + _True + " (default)." ) );
    }
    
    // TODO SAM 2009-04-05 For now allow command line and individual arguments to both be specified and
    // use the separate arguments first.
    // Timeout is not required
    if ( (Timeout != null) && (Timeout.length() != 0) && !StringUtil.isDouble(Timeout) ) {
        message = "The timeout value \"" + Timeout + "\" is not a number.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the timeout as a number of seconds or leave blank to default to no timeout."));
    }

	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(10+_ProgramArg_SIZE);
	validList.add ( "CommandLine" );
	validList.add ( "Program" );
	for ( int i = 0; i < _ProgramArg_SIZE; i++ ) {
	    validList.add ( "ProgramArg" + (i + 1) );
	}
	validList.add ( "UseCommandShell" );
	validList.add ( "CommandShell" );
	validList.add ( "Timeout" );
	validList.add ( "ExitStatusIndicator" );
	validList.add ( "ExitCodeProperty" );
	validList.add ( "OutputCheckTableID" );
	validList.add ( "OutputCheckWarningCountProperty" );
	validList.add ( "OutputCheckFailureCountProperty" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

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
{	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	// The command will be modified if changed...
	return (new RunProgram_JDialog ( parent, this, tableIDChoices )).ok();
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
    	List<String> tokens = StringUtil.breakStringList ( command_string,"(,)", StringUtil.DELIM_ALLOW_STRINGS );
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	int log_level = 3; // Level for non-user messages for log file.
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
    CommandProcessor processor = getCommandProcessor();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
	CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // default
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}
	
	String CommandLine = parameters.getValue ( "CommandLine" );
	if ( (commandPhase == CommandPhaseType.RUN) && (CommandLine != null) && (CommandLine.indexOf("${") >= 0) ) {
		CommandLine = TSCommandProcessorUtil.expandParameterValue(processor, this, CommandLine);
	}
	String Program = parameters.getValue ( "Program" );
	if ( (commandPhase == CommandPhaseType.RUN) && (Program != null) && (Program.indexOf("${") >= 0) ) {
		Program = TSCommandProcessorUtil.expandParameterValue(processor, this, Program);
	}
	String [] ProgramArg = new String[_ProgramArg_SIZE];
	for ( int i = 0; i < _ProgramArg_SIZE; i++ ) {
	    ProgramArg[i] = parameters.getValue ( "ProgramArg" + (i + 1) );
		if ( (commandPhase == CommandPhaseType.RUN) && (ProgramArg[i] != null) && (ProgramArg[i].indexOf("${") >= 0) ) {
			ProgramArg[i] = TSCommandProcessorUtil.expandParameterValue(processor, this, ProgramArg[i]);
		}
	}
    String UseCommandShell = parameters.getValue ( "UseCommandShell" );
    boolean UseCommandShell_boolean = true; // default
    if ( (UseCommandShell != null) && UseCommandShell.equalsIgnoreCase(_False) ) {
        UseCommandShell_boolean = false;
    }
    String CommandShell = parameters.getValue ( "CommandShell" );
	if ( (commandPhase == CommandPhaseType.RUN) && (CommandShell != null) && (CommandShell.indexOf("${") >= 0) ) {
		CommandShell = TSCommandProcessorUtil.expandParameterValue(processor, this, CommandShell);
	}
	// The ProcessManager wants to see the command shell as its parts so split by spaces
	String [] commandInterpreter = null;
	if ( (CommandShell != null) && !CommandShell.isEmpty() ) {
		commandInterpreter = CommandShell.split(" ");
		for ( int i = 0; i < commandInterpreter.length; i++ ) {
			commandInterpreter[i] = commandInterpreter[i].trim();
		}
	}
	String Timeout = parameters.getValue ( "Timeout" );
    double Timeout_double = 0.0;
    if ( (Timeout != null) && (Timeout.length() > 0) ) {
        Timeout_double = Double.parseDouble(Timeout);
    }
    String ExitStatusIndicator = parameters.getValue ( "ExitStatusIndicator" );
    String ExitCodeProperty = parameters.getValue ( "ExitCodeProperty" );
	if ( (commandPhase == CommandPhaseType.RUN) && (ExitCodeProperty != null) && (ExitCodeProperty.indexOf("${") >= 0) ) {
		ExitCodeProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, ExitCodeProperty);
	}
    String OutputCheckTableID = parameters.getValue ( "OutputCheckTableID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (OutputCheckTableID != null) && (OutputCheckTableID.indexOf("${") >= 0) ) {
		OutputCheckTableID = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputCheckTableID);
	}
    String OutputCheckWarningCountProperty = parameters.getValue ( "OutputCheckWarningCountProperty" );
	if ( (commandPhase == CommandPhaseType.RUN) && (OutputCheckWarningCountProperty != null) && (OutputCheckWarningCountProperty.indexOf("${") >= 0) ) {
		OutputCheckWarningCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputCheckWarningCountProperty);
	}
    String OutputCheckFailureCountProperty = parameters.getValue ( "OutputCheckFailureCountProperty" );
	if ( (commandPhase == CommandPhaseType.RUN) && (OutputCheckFailureCountProperty != null) && (OutputCheckFailureCountProperty.indexOf("${") >= 0) ) {
		OutputCheckFailureCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputCheckFailureCountProperty);
	}
	
	// Get the output check table.

    DataTable outputCheckTable = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
        PropList request_params = null;
        CommandProcessorRequestResultsBean bean = null;
        if ( (OutputCheckTableID != null) && !OutputCheckTableID.equals("") ) {
            // Get the table to be updated
            request_params = new PropList ( "" );
            request_params.set ( "TableID", OutputCheckTableID );
            try {
                bean = processor.processRequest( "GetTable", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTable(TableID=\"" + OutputCheckTableID + "\") from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_Table = bean_PropList.getContents ( "Table" );
            if ( o_Table == null ) {
                message = "Unable to find table to process using OutputCheckTableID=\"" + OutputCheckTableID + "\".";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a table exists with the requested ID." ) );
            }
            else {
                outputCheckTable = (DataTable)o_Table;
            }
        }
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	// If the Program has been specified but CommandLine has not, construct the command line from the
	// program for logging.  The Program will take precedence below so this will not change the logic
	if ( (CommandLine == null) || CommandLine.equals("") ) {
	    StringBuffer b = new StringBuffer();
	    b.append ( Program );
	    for ( int i = 0; i < _ProgramArg_SIZE; i++ ) {
            if ( ProgramArg[i] != null ) {
                b.append ( " " + ProgramArg[i] );
            }
        }
	    CommandLine = b.toString();
	}

	try {
        // Do the following if it is hard to track what is going on...
        //PropList props = new PropList ( "PM" );
        //ProcessManagerDialog pmg = new ProcessManagerDialog ( program, props );
        //props = null;
        // pmg = null;
        // Normally can do this, although TSTool may sit for awhile until the
        // process is finished (need to figure out a way to make TSTool wait on the thread without hanging).
        ProcessManager pm = null;
        if ( (Program != null) && !Program.isEmpty() ) {
            // Specify the program to run using individual strings - this takes precedence over the full
            // command line.
            List<String> programAndArgsList = new ArrayList<String>();
            programAndArgsList.add ( Program );
            for ( int i = 0; i < _ProgramArg_SIZE; i++ ) {
                if ( (ProgramArg[i] != null) && !ProgramArg[i].isEmpty() ) {
                    programAndArgsList.add (ProgramArg[i]);
                }
            }
            pm = new ProcessManager ( StringUtil.toArray(programAndArgsList), (int)(Timeout_double*1000.0),
                ExitStatusIndicator, UseCommandShell_boolean,
                new File((String)processor.getPropContents("WorkingDir")));
            if ( commandInterpreter != null ) {
            	pm.setCommandInterpreter(commandInterpreter);
            }
            //CommandLine_full, (int)(Timeout_double*1000.0), ExitStatusIndicator);
        }
        else {
            // Specify the command to run using a full command line
            pm = new ProcessManager (CommandLine, (int)(Timeout_double*1000.0), ExitStatusIndicator,
                UseCommandShell_boolean, new File((String)processor.getPropContents("WorkingDir")));
            if ( commandInterpreter != null ) {
            	pm.setCommandInterpreter(commandInterpreter);
            }
        }
        pm.saveOutput ( true ); // Save output so it can be used in troubleshooting
        pm.run();
        Message.printStatus ( 2, routine, "Exit status from program = " + pm.getExitStatus() );
        if ( pm.getExitStatus() == 996 ) {
            message = "Program \"" + CommandLine + "\" timed out.  Full output may not be available.";
            Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify running the program on the command line before running in TSTool."));
        }
        else if ( pm.getExitStatus() != 0 ) {
            message = "Program \"" + CommandLine + "\" exited with status " + pm.getExitStatus() +
            ".  Full output may not be available.  Output from program is:\n" +
            StringUtil.toString(pm.getOutputList(),"\n") + "\nStandard error from program is:\n" +
            StringUtil.toString(pm.getErrorList(),"\n");
            Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify running the program on the command line before running in TSTool."));
        }
        if ( (ExitCodeProperty != null) && !ExitCodeProperty.isEmpty() ) {
            PropList request_params = new PropList ( "" );
            request_params.setUsingObject ( "PropertyName", ExitCodeProperty );
            request_params.setUsingObject ( "PropertyValue", new Integer(pm.getExitStatus()) );
            try {
                processor.processRequest( "SetProperty", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting SetProperty(Property=\"" + ExitCodeProperty + "\") from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
        }
        // Echo the output to the log file.
        List<String> output = pm.getOutputList();
        int size = 0;
        if ( output != null ) {
            size = output.size();
        }
        for ( int i = 0; i < size; i++ ) {
            Message.printStatus(2, routine, "Program output:  " + output.get(i));
        }
        // Process the check table
        if ( outputCheckTable != null ) {
        	// Loop through the lines of the table and for each process the patterns in the check file.
        	// This will make it easier to review the output results without jumping around
        	int outputWarningCount = 0;
        	int outputFailureCount = 0;
        	// Get the column numbers for the check table
        	int fileColumnNum = -1;
        	try {
                fileColumnNum = outputCheckTable.getFieldIndex("File");
            }
            catch ( Exception e ) {
            	message = "Output check table \"" + OutputCheckTableID + "\" does not have \"File\" column - unable to check output.";
        		Message.printWarning ( warning_level, 
	        		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
	        			Message.printWarning ( 3, routine, e );
        		status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
        		    message, "Verify that output check table includes \"File\" column."));
            }
        	int patternColumnNum = -1;
        	try {
        		patternColumnNum = outputCheckTable.getFieldIndex("Pattern");
            }
            catch ( Exception e ) {
            	message = "Output check table \"" + OutputCheckTableID + "\" does not have \"Pattern\" column - unable to check output.";
        		Message.printWarning ( warning_level, 
	        		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
	        			Message.printWarning ( 3, routine, e );
        		status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
        		    message, "Verify that output check table includes \"Pattern\" column."));
            }
        	int levelColumnNum = -1;
        	try {
        		levelColumnNum = outputCheckTable.getFieldIndex("Level");
            }
            catch ( Exception e ) {
            	message = "Output check table \"" + OutputCheckTableID + "\" does not have \"Level\" column - unable to check output.";
        		Message.printWarning ( warning_level, 
	        		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
	        			Message.printWarning ( 3, routine, e );
        		status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
        		    message, "Verify that output check table includes \"Level\" column."));
            }
        	int messageColumnNum = -1;
        	try {
        		messageColumnNum = outputCheckTable.getFieldIndex("Message");
            }
            catch ( Exception e ) {
            	message = "Output check table \"" + OutputCheckTableID + "\" does not have \"Message\" column - unable to check output.";
        		Message.printWarning ( warning_level, 
	        		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
	        			Message.printWarning ( 3, routine, e );
        		status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
        		    message, "Verify that output check table includes \"Message\" column."));
            }
        	int recommendationColumnNum = -1;
        	try {
        		recommendationColumnNum = outputCheckTable.getFieldIndex("Recommendation");
            }
            catch ( Exception e ) {
            	message = "Output check table \"" + OutputCheckTableID + "\" does not have \"Recommendation\" column - unable to check output.";
        		Message.printWarning ( warning_level, 
	        		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
	        			Message.printWarning ( 3, routine, e );
        		status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
        		    message, "Verify that output check table includes \"Recommendation\" column."));
            }
        	if ( (fileColumnNum >= 0) && (patternColumnNum >= 0) && (levelColumnNum >= 0) && (messageColumnNum >= 0) && (recommendationColumnNum >= 0) ) {
	        	// First loop through the table and get a list of the distinct output files, ignoring "stdout" since that is processed separately
	        	List<String>outputFiles = new ArrayList<String>();
	        	for ( int irow = 0; irow < outputCheckTable.getNumberOfRecords(); irow++ ) {
	        		TableRecord rec = outputCheckTable.getRecord(irow);
	        		String file = rec.getFieldValueString(fileColumnNum);
	        		if ( file == null ) {
	        			continue;
	        		}
	        		else if ( (commandPhase == CommandPhaseType.RUN) && (file.indexOf("${") >= 0) ) {
	        			file = TSCommandProcessorUtil.expandParameterValue(processor, this, file);
	        		}
	        		boolean found = false;
	        		for ( String outputFile : outputFiles ) {
	        			if ( file.equals(outputFile) ) {
	        				found = true;
	        				break;
	        			}
	        		}
	        		if ( !found ) {
	        			outputFiles.add(file);
	        		}
	        	}
	        	// Get the list of patterns and other data for each output file
	        	List<String> [] patternList = new ArrayList[outputFiles.size()];
	        	List<String> [] levelList = new ArrayList[outputFiles.size()];
	        	List<String> [] messageList = new ArrayList[outputFiles.size()];
	        	List<String> [] recommendationList = new ArrayList[outputFiles.size()];
	        	// Construct the lists of patterns and store in arrays of lists for fast access during processing
	        	for ( int iFile = 0; iFile < outputFiles.size(); iFile++ ) {
	        		String outputFile = outputFiles.get(iFile);
	        		patternList[iFile] = new ArrayList<String>();
	        		levelList[iFile] = new ArrayList<String>();
	        		messageList[iFile] = new ArrayList<String>();
	        		recommendationList[iFile] = new ArrayList<String>();
	        		for ( int irow = 0; irow < outputCheckTable.getNumberOfRecords(); irow++ ) {
		        		TableRecord rec = outputCheckTable.getRecord(irow);
		        		String file = rec.getFieldValueString(fileColumnNum);
		        		if ( (commandPhase == CommandPhaseType.RUN) && (file != null) && (file.indexOf("${") >= 0) ) {
		        			file = TSCommandProcessorUtil.expandParameterValue(processor, this, file);
		        		}
		        		String pattern = rec.getFieldValueString(patternColumnNum);
		        		if ( (commandPhase == CommandPhaseType.RUN) && (pattern != null) && (pattern.indexOf("${") >= 0) ) {
		        			pattern = TSCommandProcessorUtil.expandParameterValue(processor, this, pattern);
		        		}
	        			// Replace * with .* for Java regular expressions
	        			pattern = pattern.replace("*",".*");
		        		String level = rec.getFieldValueString(levelColumnNum);
		        		if ( (commandPhase == CommandPhaseType.RUN) && (level != null) && (level.indexOf("${") >= 0) ) {
		        			level = TSCommandProcessorUtil.expandParameterValue(processor, this, level);
		        		}
		        		String message2 = rec.getFieldValueString(messageColumnNum);
		        		if ( (commandPhase == CommandPhaseType.RUN) && (message2 != null) && (message2.indexOf("${") >= 0) ) {
		        			message2 = TSCommandProcessorUtil.expandParameterValue(processor, this, message2);
		        		}
		        		String recommendation = rec.getFieldValueString(recommendationColumnNum);
		        		if ( (commandPhase == CommandPhaseType.RUN) && (recommendation != null) && (recommendation.indexOf("${") >= 0) ) {
		        			recommendation = TSCommandProcessorUtil.expandParameterValue(processor, this, recommendation);
		        		}
		        		if ( file.equals(outputFile) ) {
		        			// Found a row that matches the output file
		        			patternList[iFile].add(pattern);
		        			levelList[iFile].add(level);
		        			messageList[iFile].add(message2);
		        			recommendationList[iFile].add(recommendation);
		        		}
	        		}
	        	}
	        	// Loop through the files, open each one, and process the patterns
	        	for ( int iFile = 0; iFile < outputFiles.size(); iFile++ ) {
	        		String outputFile = outputFiles.get(iFile);
	        		boolean doStdout = false;
	        		String outputFileFull = null;
	        		if ( outputFile.equalsIgnoreCase("stdout") ) {
	        			doStdout = true;
	        		}
	        		else {
		        		outputFileFull = IOUtil.verifyPathForOS(
	        	            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        	                TSCommandProcessorUtil.expandParameterValue(processor,this,outputFile)));
	        		}
	        		if ( Message.isDebugOn ) {
	        			Message.printDebug(1,routine,"Checking output file \"" + outputFileFull + "\"");
	        		}
	        		BufferedReader fp = null;
	        		try {
	        			if ( !doStdout ) {
	        				fp = new BufferedReader ( new InputStreamReader(IOUtil.getInputStream(outputFileFull) ));
	        			}
	        		    String line = null;
	        		    int lineCount = -1;
	        		    int lineCountMax = output.size() - 1;
		        		while ( true ) {
		        			++lineCount;
		        			if ( doStdout ) {
		        				// Get a line from the standard output
		        				if ( lineCount > lineCountMax ) {
		        					break;
		        				}
		        				line = output.get(lineCount);
		        			}
		        			else {
		        				// Get a line from the file
			        			line = fp.readLine();
			        			if ( line == null ) {
			        				break;
			        			}
		        			}
		        			for ( int iPattern = 0; iPattern < patternList[iFile].size(); iPattern++ ) {
		        				if ( Message.isDebugOn ) {
		        					Message.printDebug(1,routine,"Checking pattern \"" +patternList[iFile].get(iPattern) + "\"");
		        				}
		        				if ( line.matches(patternList[iFile].get(iPattern)) ) {
		        					// Line matches so generate a message
		    	        			message = messageList[iFile].get(iPattern);
		    	        			if ( message.indexOf("${file.line:text}") >= 0 ) {
		    	        				// Replace the place-holder with the output line
		    	        				message = message.replace("${file.line:text}",line);
		    	        			}
		    	        			if ( message.indexOf("${file.line:number}") >= 0 ) {
		    	        				// Replace the place-holder with the output line
		    	        				message = message.replace("${file.line:number}",(""+lineCount));
		    	        			}
		    	        			if ( message.indexOf("${file:path}") >= 0 ) {
		    	        				// Replace the place-holder with the output line
		    	        				message = message.replace("${file:path}",(""+outputFile));
		    	        			}
		    	        			String level = levelList[iFile].get(iPattern);
		    	        			if ( level.equalsIgnoreCase("warning") ) {
			    	            		Message.printWarning ( warning_level, 
			            	        		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
			                    		status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
			                    		    message, recommendationList[iFile].get(iPattern)));
			                    		++outputWarningCount;
		        					}
		    	        			else if ( level.equalsIgnoreCase("failure") || level.equalsIgnoreCase("error")) {
			    	            		Message.printWarning ( warning_level, 
			            	        		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
			                    		status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
			                    		    message, recommendationList[iFile].get(iPattern)));
			                    		++outputFailureCount;
		        					}
		    	        			else if ( level.equalsIgnoreCase("success") ) {
			    	            		Message.printWarning ( warning_level, 
			            	        		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
			                    		status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.SUCCESS,
			                    		    message, recommendationList[iFile].get(iPattern)));
		        					}
		        				}
		        			}
		        		}
	        		}
	        		catch ( Exception e ) {
	        			message = "Unable to read file \"" + outputFile + "\" (" + e + ").";
	            		Message.printWarning ( warning_level, 
        	        		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        	        			Message.printWarning ( 3, routine, e );
                		status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                		    message, "Verify that output file \"" + outputFileFull + "\" exists."));
	        			continue;
	        		}
	        		finally {
	        			if ( fp != null ) {
	        				fp.close ();
	        			}
	        		}
	        	}
        	}
        	// Set properties for the output warning and failure counts
            if ( (OutputCheckWarningCountProperty != null) && !OutputCheckWarningCountProperty.isEmpty() ) {
                PropList request_params = new PropList ( "" );
                request_params.setUsingObject ( "PropertyName", OutputCheckWarningCountProperty );
                request_params.setUsingObject ( "PropertyValue", new Integer(outputWarningCount) );
                try {
                    processor.processRequest( "SetProperty", request_params);
                }
                catch ( Exception e ) {
                    message = "Error requesting SetProperty(Property=\"" + OutputCheckWarningCountProperty + "\") from processor.";
                    Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                }
            }
            if ( (OutputCheckFailureCountProperty != null) && !OutputCheckFailureCountProperty.isEmpty() ) {
                PropList request_params = new PropList ( "" );
                request_params.setUsingObject ( "PropertyName", OutputCheckFailureCountProperty );
                request_params.setUsingObject ( "PropertyValue", new Integer(outputFailureCount) );
                try {
                    processor.processRequest( "SetProperty", request_params);
                }
                catch ( Exception e ) {
                    message = "Error requesting SetProperty(Property=\"" + OutputCheckFailureCountProperty + "\") from processor.";
                    Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                }
            }
        }
	}
	catch ( Exception e ) {
		message = "Unexpected error running program \"" + CommandLine + "\" (expanded=" + CommandLine + ") (" + e + ").";
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
	String UseCommandShell = parameters.getValue("UseCommandShell");
	String CommandShell = parameters.getValue("CommandShell");
	String Timeout = parameters.getValue("Timeout");
	String ExitStatusIndicator = parameters.getValue("ExitStatusIndicator");
	String ExitCodeProperty = parameters.getValue("ExitCodeProperty");
	String OutputCheckTableID = parameters.getValue("OutputCheckTableID");
	String OutputCheckWarningCountProperty = parameters.getValue("OutputCheckWarningCountProperty");
	String OutputCheckFailureCountProperty = parameters.getValue("OutputCheckFailureCountProperty");
	StringBuffer b = new StringBuffer ();
	if ( (CommandLine != null) && (CommandLine.length() > 0) ) {
		b.append ( "CommandLine=\"" + CommandLine + "\"" );
	}
    if ( (Program != null) && (Program.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Program=\"" + Program + "\"");
    }
    for ( int i = 0; i < _ProgramArg_SIZE; i++ ) {
        if ( (ProgramArg[i] != null) && (ProgramArg[i].length() > 0) ) {
            if ( b.length() > 0 ) {
                b.append ( "," );
            }
            b.append ( "ProgramArg" + (i + 1) + "=\"" + ProgramArg[i] + "\"");
        } 
    }
    if ( (UseCommandShell != null) && (UseCommandShell.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "UseCommandShell=" + UseCommandShell );
    }
    if ( (CommandShell != null) && (CommandShell.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CommandShell=\"" + CommandShell + "\"" );
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
    if ( (ExitCodeProperty != null) && (ExitCodeProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExitCodeProperty=\"" + ExitCodeProperty + "\"");
    }
    if ( (OutputCheckTableID != null) && (OutputCheckTableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputCheckTableID=\"" + OutputCheckTableID +"\"" );
    }
    if ( (OutputCheckWarningCountProperty != null) && (OutputCheckWarningCountProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputCheckWarningCountProperty=\"" + OutputCheckWarningCountProperty +"\"" );
    }
    if ( (OutputCheckFailureCountProperty != null) && (OutputCheckFailureCountProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputCheckFailureCountProperty=\"" + OutputCheckFailureCountProperty +"\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
