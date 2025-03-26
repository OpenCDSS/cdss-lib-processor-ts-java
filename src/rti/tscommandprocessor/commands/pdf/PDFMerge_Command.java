// PDFMerge_Command - This class initializes, checks, and runs the PDFMerge() command.

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

package rti.tscommandprocessor.commands.pdf;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

//import org.apache.pdfbox.multipdf.PDFMergerUtility;

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
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the PDFMerge() command.
*/
public class PDFMerge_Command extends AbstractCommand
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
public PDFMerge_Command () {
	super();
	setCommandName ( "PDFMerge" );
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
	String InputFiles = parameters.getValue ( "InputFiles" );
    String OutputFile = parameters.getValue ( "OutputFile" );
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	// The existence of the file to append is not checked during initialization
	// because files may be created dynamically at runtime.

	if ( (InputFiles == null) || InputFiles.isEmpty() ) {
		message = "The input file must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the input file."));
	}
    if ( (OutputFile == null) || OutputFile.isEmpty() ) {
        message = "The output file must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the output file."));
    }
	if ( (IfNotFound != null) && !IfNotFound.isEmpty() ) {
		if ( !IfNotFound.equalsIgnoreCase(_Ignore) && !IfNotFound.equalsIgnoreCase(_Warn)
		    && !IfNotFound.equalsIgnoreCase(_Fail) ) {
			message = "The IfNotFound parameter \"" + IfNotFound + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Ignore + ", " + _Warn + " (default), or " +
					_Fail + "."));
		}
	}
	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(3);
	validList.add ( "InputFiles" );
	validList.add ( "OutputFile" );
	validList.add ( "IfNotFound" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
 * Do the merge.
 */
private int doMerge ( String OutputFile_full, String IfNotFound, List<File> fileList,
	CommandStatus status, String commandTag, int warningLevel, int warningCount )
	throws Exception {
	//String routine = getClass().getSimpleName() + ".doMerge";
	//String message;

	/*
	// Instantiate the PDFMergerUtility class.
	PDFMergerUtility pdfMerger = new PDFMergerUtility();
		
	//Setting the destination file
	pdfMerger.setDestinationFileName(OutputFile_full);
		
	// Add the source files to merge.
	for ( File file : fileList ) {
		if ( file.exists() ) {
			message = "Merging file \"" + file + "\".";
			Message.printStatus( 2, routine, message );
			pdfMerger.addSource(file);
		}
		else {
			message = "Input file to merge \"" + file + "\" does not exist.";
			if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
				Message.printWarning ( warningLevel,
					MessageUtil.formatMessageTag(commandTag,++warningCount), routine, message );
				status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Verify that the file exists at the time the command is run."));
			}
			else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
				Message.printWarning ( warningLevel,
					MessageUtil.formatMessageTag(commandTag,++warningCount), routine, message );
				status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
					message, "Verify that the file exists at the time the command is run."));
			}
			else {
				Message.printStatus( 2, routine, message + "  Ignoring file.");
			}
		}
	}
		
	// Merge the added documents.
	//MemoryUsageSetting memoryUsageSetting = MemoryUsageSetting.setupMainMemoryOnly();
	pdfMerger.mergeDocuments(null);
	*/
	
	return warningCount;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new PDFMerge_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList () {
    List<File> list = new ArrayList<>(1);
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
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException {
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

    // Clear the output file.
    setOutputFile ( null );

	String InputFiles = parameters.getValue ( "InputFiles" );
	String OutputFile = parameters.getValue ( "OutputFile" ); // Expanded below.
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	if ( (IfNotFound == null) || IfNotFound.equals("")) {
	    IfNotFound = _Warn; // Default.
	}

	// Expand to a list of files.
	// First handle list of file patterns separated by commas.
	String [] parts = new String[0];
	if ( InputFiles.contains(",") ) {
		// Matching a list of files or patterns in a folder.
		parts = InputFiles.split(",");
	}
	else {
		// Single file pattern.
		parts = new String[1];
		parts[0] = InputFiles;
	}

	// Loop through the parts to create the final list of files.
	List<File> fileList = new ArrayList<>();
	for ( String part : parts ) {
		part = part.trim();
		// Always use Linux path separator because it simplifies glob evaluation.
		String partFull = IOUtil.verifyPathForOS(
       		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
       			TSCommandProcessorUtil.expandParameterValue(processor,this,part) ) ).replace("\\", "/");
		if ( !StringUtil.containsAny(partFull, "*{?[", false) ) {
	    	// Processing a single file so don't need to deal with glob wildcards.
			Message.printStatus(2,routine,"Adding single file: " + partFull);
	    	fileList.add(Paths.get(partFull).toFile());
		}
		else {
	    	// TODO SAM 2016-02-08 Need to enable parameter for ignore case.
			String pattern = "glob:" + partFull.replace("\\", "/");
			Message.printStatus(2,routine,"Getting files for pattern: " + pattern);
			try {
				List<File> partFileList = IOUtil.getFilesMatchingPattern(pattern);
				fileList.addAll(partFileList);
			}
			catch ( IOException e ) {
				message = "Error getting list of files for pattern: " + pattern;
				if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
					status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Contact software support."));
				}
				else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
					status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
						message, "Contact software support."));
				}
			}
		}
	}

	if ( fileList.size() == 0 ) {
		// No text and no files.
	    message = "Unable to match any files using InputFiles=\"" + InputFiles + "\"";
	    if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the input file(s) exist at the time the command is run."));
        }
        else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
                message, "Verify that the input file(s) exist at the time the command is run."));
        }
	}
	else {
	    message = "Matched " + fileList.size() + " files.";
	    Message.printStatus(2, routine, message);
	}
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Process the files and append text.  Each input file is opened to scan the file.  The output file is opened once in append mode.

	String OutputFile_full = OutputFile;
	try {
		OutputFile_full = IOUtil.verifyPathForOS(
	    	IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        	TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile) ) );

		// The PDFBox code might mess with Swing fonts, for example odd errors loading the command.
		// ChatGPT suggested running in a thread but this does not seem to solve the problem.
		// Leave in for now and deal with it later.
		// It seems that loading a different command file first and then the PDFMerge command always works?
		boolean doSeparateThread = false;
		if ( doSeparateThread ) {
			Thread pdfThread = new Thread (() -> {
				try {
					final String OutputFile_full_final = IOUtil.verifyPathForOS(
						IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
							TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile) ) );
					final String IfNotFound_final = "";
					final int warning_count_final = 0;
					doMerge ( OutputFile_full_final, IfNotFound_final, fileList, status, command_tag, warning_level, warning_count_final );
				}
				catch ( Exception e ) {
				}
			});
			pdfThread.start();
			try {
				pdfThread.join();
			}
			catch ( InterruptedException e ) {
			}
		}
		else {
			// Run in the same thread.
			doMerge ( OutputFile_full, IfNotFound, fileList, status, command_tag, warning_level, warning_count );
		}

    	// Save the output file name.
    	setOutputFile ( new File(OutputFile_full));
	}
	catch ( Exception e ) {
		message = "Unexpected error merging PDF file(s) as \"" + OutputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level,
  			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "See the log file for details."));
	}
	finally {
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
		"InputFiles",
		"OutputFile",
		"IfNotFound"
	};
	return this.toString(parameters, parameterOrder);
}

}