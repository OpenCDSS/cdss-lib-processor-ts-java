// TextEdit_Command - This class initializes, checks, and runs the TextEdit() command.

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

package rti.tscommandprocessor.commands.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;

/**
This class initializes, checks, and runs the TextEdit() command.
*/
public class TextEdit_Command extends AbstractCommand
implements Command, FileGenerator
{

/**
Data members used for parameter values.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public TextEdit_Command () {
	super();
	setCommandName ( "TextEdit" );
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
	String InputFile = parameters.getValue ( "InputFile" );
    String SearchFor = parameters.getValue ( "SearchFor" );
    String ReplaceWith = parameters.getValue ( "ReplaceWith" );
    String ReplaceWithFile = parameters.getValue ( "ReplaceWithFile" );
    String OutputFile = parameters.getValue ( "OutputFile" );
	String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	// The existence of the file to append is not checked during initialization
	// because files may be created dynamically at runtime.

	if ( (InputFile == null) || (InputFile.length() == 0) ) {
		message = "The input file must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the input file."));
	}
	if ( (SearchFor == null) || (SearchFor.length() == 0) ) {
		message = "The pattern to search for must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the pattern to search for."));
	}
	// Must specify either ReplaceWith or ReplaceWithFile.
	int replaceCount = 0;
	if ( (ReplaceWith != null) && !ReplaceWith.isEmpty() ) {
		// Do not allow empty string (EMPTY_STRING constant will be specified).
		++replaceCount;
	}
	if ( (ReplaceWithFile != null) && !ReplaceWithFile.isEmpty() ) {
		++replaceCount;
	}
	if ( replaceCount == 0 ) {
		message = "The pattern to replace with is not specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the pattern to replace with using ReplaceWith or ReplaceWithFile."));
		
	}
	else if ( replaceCount == 2 ) {
		message = "The pattern to replace with is specified as text and a file.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the pattern to replace with using ReplaceWith or ReplaceWithFile."));
		
	}
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        message = "The output file must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the output file."));
    }
	if ( (IfInputNotFound != null) && !IfInputNotFound.equals("") ) {
		if ( !IfInputNotFound.equalsIgnoreCase(_Ignore) && !IfInputNotFound.equalsIgnoreCase(_Warn)
		    && !IfInputNotFound.equalsIgnoreCase(_Fail) ) {
			message = "The IfInputNotFound parameter \"" + IfInputNotFound + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Ignore + ", " + _Warn + " (default), or " +
					_Fail + "."));
		}
	}
	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(6);
	validList.add ( "InputFile" );
	validList.add ( "SearchFor" );
	validList.add ( "ReplaceWith" );
	validList.add ( "ReplaceWithFile" );
	validList.add ( "OutputFile" );
	validList.add ( "IfInputNotFound" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new TextEdit_JDialog ( parent, this )).ok();
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
 * Replace one string with another in a StringBuilder.
 * Multiple instances are replaced until all matches have been processed.
 * @param sb StringBuilder to modify.
 * @param searchFor literal string to search for.
 * @param replaceWith literal string to replace with.
 */
private void replaceString ( StringBuilder sb, String searchFor, String replaceWith ) {
	boolean useMatcher = true;
	if ( useMatcher) {
		Pattern pattern = Pattern.compile(searchFor);
		Matcher matcher = pattern.matcher(sb);
		// Replace the substring in the entire string and then replace in the StringBuilder:
		// - this is not very efficient for large strings
		sb.replace(0, sb.length(), matcher.replaceAll(replaceWith));
	}
	else {
		// Use literal strings.
		int start = 0;
		while ( (start = sb.indexOf(searchFor,start)) > -1 ) {
			int end = start + searchFor.length();
			sb.replace(start, end, replaceWith);
			// Increment start to end of filled string for next search.
			start += replaceWith.length();
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
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	PropList parameters = getCommandParameters();

    CommandProcessor processor = getCommandProcessor();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
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
		status.clearLog(commandPhase);
	}

    // Clear the output file.
    setOutputFile ( null );

	String InputFile = parameters.getValue ( "InputFile" ); // Expand below.
	String OutputFile = parameters.getValue ( "OutputFile" ); // Expand below.
	String SearchFor = parameters.getValue ( "SearchFor" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		// Expand properties, will skip over if escaped with \$\{.
		SearchFor = TSCommandProcessorUtil.expandParameterValue(processor, this, SearchFor);
	}
	if ( SearchFor != null ) {
		SearchFor = SearchFor.
			replace("\\n", "\n").
			replace("\\r", "\r").
			replace("\\t", "\t");
	}
	String ReplaceWith = parameters.getValue ( "ReplaceWith" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		// Expand properties, will skip over if escaped with \$\{.
		ReplaceWith = TSCommandProcessorUtil.expandParameterValue(processor, this, ReplaceWith);
	}
	if ( ReplaceWith != null ) {
		if ( ReplaceWith.equals("EMPTY_STRING") ) {
			ReplaceWith = "";
		}
		// Replace literal strings with equivalent internal characters.
		ReplaceWith = ReplaceWith.
			replace("\\n", "\n").
			replace("\\r", "\r").
			replace("\\t", "\t");
	}
	String ReplaceWithFile = parameters.getValue ( "ReplaceWithFile" ); // Expand below.
	// Replace known escaped strings with actual values.
	String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
	if ( (IfInputNotFound == null) || IfInputNotFound.equals("")) {
	    IfInputNotFound = _Warn; // Default.
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Process the files.  Each input file is opened to scan the file.  The output file is opened once in append mode.
    // TODO SAM 2014-02-03 Enable copying a list to a folder, etc. see AppendFile() for example
    String InputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
	String OutputFile_full = IOUtil.verifyPathForOS(
	    IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
    String ReplaceWithFile_full = ReplaceWithFile;
    if ( (ReplaceWithFile != null) && !ReplaceWithFile.isEmpty() ) {
    	ReplaceWithFile_full = IOUtil.verifyPathForOS(
    		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
    			TSCommandProcessorUtil.expandParameterValue(processor,this,ReplaceWithFile)));
    }
	try {
		// Indicate whether an error getting the replacement text from a file.
		boolean hasError = false;
		if ( (ReplaceWithFile != null) && !ReplaceWithFile.isEmpty() ) {
			// Get the replacement text from the file if a file was specified.
			File replaceWithFile = new File(ReplaceWithFile_full);
			if ( replaceWithFile.exists() ) {
				// Open the file.
				Path path = Paths.get(ReplaceWithFile_full);
				ReplaceWith = Files.readString(path);
				// Also expand properties.
				ReplaceWith = TSCommandProcessorUtil.expandParameterValue(processor, this, ReplaceWith);
			}
			else {
	        	message = "Replace with file does not exist ReplaceWithFile=\"" + ReplaceWithFile + "\"";
				if ( IfInputNotFound.equalsIgnoreCase(_Fail) ) {
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
					status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that the replace with file exists at the time the command is run."));
				}
				else if ( IfInputNotFound.equalsIgnoreCase(_Warn) ) {
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
					status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
						message, "Verify that the replace with file exists at the time the command is run."));
				}
				// Indicate that there is an error.
				hasError = true;
			}
		}
		if ( !hasError ) {
			// Can continue with the replacement.
			File in = new File(InputFile_full);
	    	if ( in.exists() ) {
	    		// Read the file into a StringBuilder.
	    		//Message.printStatus(2,routine,"Reading file into StringBuilder.");
    			StringBuilder sb = IOUtil.fileToStringBuilder(InputFile_full);
	    		//Message.printStatus(2,routine,"Replacing string \"" + SearchFor + "\" with \"" + ReplaceWith + "\".");
	    		replaceString ( sb, SearchFor, ReplaceWith );
	    		// Write the output file.
	    		//Message.printStatus(2,routine,"Writing StringBuilder to file.");
	    		IOUtil.writeFile(OutputFile_full, sb.toString());
	    		//Message.printStatus(2,routine,"Back from writing file.");
	        	// Save the output file name.
	        	setOutputFile ( new File(OutputFile_full));
	    	}
	    	else {
	        	// Input file does not exist so generate a warning.
	        	message = "Input file does not exist for InputFile=\"" + InputFile + "\"";
	        	if ( IfInputNotFound.equalsIgnoreCase(_Fail) ) {
	            	Message.printWarning ( warning_level,
	                	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	            	status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                	message, "Verify that the input file exists at the time the command is run."));
	        	}
	        	else if ( IfInputNotFound.equalsIgnoreCase(_Warn) ) {
	            	Message.printWarning ( warning_level,
	                	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	            	status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
	                	message, "Verify that the input file exists at the time the command is run."));
	        	}
	    	}
		}
	}
    catch ( Exception e ) {
		message = "Unexpected error editing text file \"" + InputFile_full + "\" to \"" +
		    OutputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "See the log file for details."));
		throw new CommandException ( message );
	}

    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag, ++warning_count),
            routine,message);
        throw new CommandWarningException ( message );
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
		"InputFile",
		"SearchFor",
		"ReplaceWith",
		"ReplaceWithFile",
		"OutputFile",
		"IfInputNotFound"
	};
	return this.toString(parameters, parameterOrder);
}

}