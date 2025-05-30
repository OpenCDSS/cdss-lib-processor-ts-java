// StartLog_Command - This class initializes, checks, and runs the StartLog() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.logging;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the StartLog() command.
*/
public class StartLog_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Strings used for parameter values, here and in the editor dialog.
*/
protected final String _Date = "Date";
protected final String _DateTime = "DateTime";

/**
Output (log) file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public StartLog_Command () {
	super();
	setCommandName ( "StartLog" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	String routine = getCommandName() + ".checkCommandParameters";
	String LogFile = parameters.getValue ( "LogFile" );
	String MaxSize = parameters.getValue ( "MaxSize" );
	String Suffix = parameters.getValue ( "Suffix" );
	String working_dir = null;
	String warning = "";
	String message;

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	try { Object o = processor.getPropContents ( "WorkingDir" );
		// Working directory is available so use it.
		if ( o != null ) {
			working_dir = (String)o;
			if ( working_dir.equals("") ) {
				working_dir = null;	// Not available.
			}
		}
	}
	catch ( Exception e ) {
		message = "Error requesting WorkingDir from processor.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
			message, "Software error - report to support." ) );
	}

	try {
		// A null logfile means that the current log file should be re-opened.
		if ( (LogFile != null) && (working_dir != null) && (LogFile.indexOf("${") < 0) ) {
			String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath(working_dir, LogFile));
			File f = new File(adjusted_path);
			File f2 = new File(f.getParent());
			if (!f2.exists()) {
				message = "The log file parent folder \"" + f.getParent() +
				"\" does not exist for: \"" + adjusted_path + "\".";
				warning += "\n" + message;
				status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Verify that the log file parent folder exists." ) );
			}
			f = null;
			f2 = null;
		}
	}
	catch ( Exception e ) {
		// Print a stack trace so the output shows up somewhere because the log file may not be used.
		if ( Message.isDebugOn ) {
			e.printStackTrace();
		}
		message = "\nThe log file \"" + LogFile +
		"\" cannot be adjusted to an absolute path using the working directory \"" +
		working_dir + "\".";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
			message, "Verify that the path information is consistent." ) );
	}

    if ( (MaxSize != null) && !MaxSize.isEmpty() && !StringUtil.isInteger(MaxSize) ) {
        message = "The screen warning level \"" + MaxSize + "\" is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Change the screen level to an integer." ) );
    }

	if ( (Suffix != null) && (Suffix.length() != 0) &&
		!Suffix.equalsIgnoreCase(_Date) &&
		!Suffix.equalsIgnoreCase(_DateTime) ) {
		message = "The suffix must be blank, \"" + _Date + "\", or \"" + _DateTime + "\".";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
			message, "Change the suffix to an allowable value." ) );
	}

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(3);
	validList.add ( "Logfile" );
	validList.add ( "MaxSize" );
	validList.add ( "Suffix" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
	        MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
		throw new InvalidCommandParameterException ( warning );
	}

	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new StartLog_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList () {
	List<File> list = new ArrayList<>();
	if ( getOutputFile() != null ) {
		list.add ( getOutputFile() );
	}
	return list;
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile () {
	return __OutputFile_File;
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	PropList parameters = getCommandParameters();

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
    Boolean clearStatus = Boolean.TRUE; // Default.
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen.
    }
    if ( clearStatus ) {
		status.clearLog(CommandPhaseType.RUN);
	}

    // Check whether this command should be executed:
    // - the ConfigureLogging() command may turn off so all logging goes to the main log file
    Boolean enabled = true; // Default.
    try {
    	Object o = processor.getPropContents("StartLogEnabled");
    	if ( o != null ) {
    		enabled = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen.
    }

    if ( enabled ) {
	String LogFile_full = null;	// File with path, used below and in final catch.
	try {
		String LogFile = parameters.getValue ( "LogFile" ); // Expanded below.
		String MaxSize = parameters.getValue ( "MaxSize" );
		int maxSize = -1;
		if ( (MaxSize != null) && !MaxSize.isEmpty() ) {
			maxSize = Integer.parseInt(MaxSize);
		}
		String Suffix = parameters.getValue ( "Suffix" );
		if ( (LogFile == null) || (LogFile.length() == 0) ) {
			// Restart the current log file.
			Message.restartLogFile();
		}
		else {
			// Open a new log file.  Append the suffix if it has been specified.
			if ( (Suffix == null) || (Suffix.length() == 0) ) {
				// Make sure to do nothing below.
				Suffix = "";
			}
			else if ( Suffix.equalsIgnoreCase(_Date) ) {
				DateTime d = new DateTime (	DateTime.DATE_CURRENT );
				Suffix = "." +
					StringUtil.formatString(d.getYear(),"%04d") +
					StringUtil.formatString(d.getMonth(),"%02d") +
					StringUtil.formatString(d.getDay(),"%02d");
			}
			else if ( Suffix.equalsIgnoreCase(_DateTime) ) {
				DateTime d = new DateTime (	DateTime.DATE_CURRENT );
				// Make sure there is no space and can't use colons here because Windows uses that for drive letters.
				Suffix = "." +
					StringUtil.formatString(d.getYear(),"%04d") +
					StringUtil.formatString(d.getMonth(),"%02d") +
					StringUtil.formatString(d.getDay(),"%02d") + "_" +
					StringUtil.formatString(d.getHour(),"%02d") +
					StringUtil.formatString(d.getMinute(),"%02d") +
					StringUtil.formatString(d.getSecond(),"%02d");
			}
			if ( Suffix.length() > 0 ) {
				String ext = IOUtil.getFileExtension (LogFile );
				if ( ext == null ) {
					// Just append.
					LogFile = LogFile + Suffix;
				}
				else {
				    // Insert before the last extension.
					LogFile = LogFile.substring(0,LogFile.length()-ext.length()-1)+ Suffix + "." + ext;
				}
			}
			LogFile_full = IOUtil.verifyPathForOS(
				IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
					TSCommandProcessorUtil.expandParameterValue(processor,this,LogFile)));
			// Write a message to the old log file.
			Message.printStatus(2, routine, "Logfile full path for next log file to open is \"" + LogFile_full + "\"");
			// Close the old log file.
			String previousLogFile = Message.getLogFile();
			Message.closeLogFile();
			// Open the new log file.
			Message.openNewLogFile ( LogFile_full );
			Message.setLogFileMaxSize ( maxSize );
			Message.printStatus(2, routine, "Opened log file \"" + LogFile_full + "\"");
			Message.printStatus(2, routine, "Previous log file was \"" + previousLogFile + "\"");
			setOutputFile ( new File(LogFile_full));
		}
	}
	catch ( Exception e ) {
		message = "Unexpected error (re)starting the log file \"" + LogFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
			message, "Check the old log file or command window for details." ) );
		throw new CommandException ( message );
	}
    } // if enabled.
    else {
    	Message.printStatus(2, routine, "StartLog command is disabled globally.  Log file is \"" + Message.getLogFile() + "\"");
    }

	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file ) {
	__OutputFile_File = file;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"LogFile",
		"MaxSize",
    	"Suffix"
	};
	return this.toString(parameters, parameterOrder);
}

}