// PDF_Command - This class initializes, checks, and runs the PDF() command.

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
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;

import org.apache.pdfbox.multipdf.PDFMergerUtility;

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
This class initializes, checks, and runs the PDF() command.
*/
public class PDF_Command extends AbstractCommand
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
private File __MergeOutputFile_File = null;

/**
Constructor.
*/
public PDF_Command () {
	super();
	setCommandName ( "PDF" );
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
	String PDFCommand = parameters.getValue ( "PDFCommand" );
	String MergeInputFiles = parameters.getValue ( "MergeInputFiles" );
    String MergeOutputFile = parameters.getValue ( "MergeOutputFile" );
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	PDFCommandType s3Command = null;
	if ( (PDFCommand == null) || PDFCommand.isEmpty() ) {
		message = "The PDF command must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the PDF command."));
	}
	else {
		s3Command = PDFCommandType.valueOfIgnoreCase(PDFCommand);
		if ( s3Command == null ) {
			message = "The PDF command (" + PDFCommand + ") is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify a valid PDF command."));
		}
	}

	// The existence of the file to append is not checked during initialization
	// because files may be created dynamically at runtime.

	if ( (MergeInputFiles == null) || MergeInputFiles.isEmpty() ) {
		message = "The input file must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the input file."));
	}
    if ( (MergeOutputFile == null) || MergeOutputFile.isEmpty() ) {
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
	List<String> validList = new ArrayList<>(4);
	validList.add ( "PDFCommand" );
	validList.add ( "MergeInputFiles" );
	validList.add ( "MergeOutputFile" );
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
private int doMerge ( String MergeOutputFile_full, String IfNotFound, List<File> fileList,
	CommandStatus status, String commandTag, int warningLevel, int warningCount )
	throws Exception {
	String routine = getClass().getSimpleName() + ".doMerge";
	String message;

	// Instantiate the PDFMergerUtility class.
	PDFMergerUtility pdfMerger = new PDFMergerUtility();

	// Set the destination file.
	pdfMerger.setDestinationFileName(MergeOutputFile_full);

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

	return warningCount;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new PDF_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList () {
    List<File> list = new ArrayList<>(1);
    if ( getMergeOutputFile() != null ) {
        list.add ( getMergeOutputFile() );
    }
    return list;
}

/**
Return the output file generated by this command for the Merge.  This method is used internally.
Return the output file generated by this command for the Merge.  This method is used internally.
*/
private File getMergeOutputFile () {
    return __MergeOutputFile_File;
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
    setMergeOutputFile ( null );

	String PDFCommand = parameters.getValue ( "PDFCommand" );
	PDFCommandType pdfCommand = PDFCommandType.valueOfIgnoreCase(PDFCommand);

	// Merge.
	String MergeInputFiles = parameters.getValue ( "MergeInputFiles" );
	String MergeOutputFile = parameters.getValue ( "MergeOutputFile" ); // Expanded below.
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	if ( (IfNotFound == null) || IfNotFound.equals("")) {
	    IfNotFound = _Warn; // Default.
	}

	// Expand to a list of files.
	// First handle list of file patterns separated by commas.
	String [] parts = new String[0];
	if ( MergeInputFiles.contains(",") ) {
		// Matching a list of files or patterns in a folder.
		parts = MergeInputFiles.split(",");
	}
	else {
		// Single file pattern.
		parts = new String[1];
		parts[0] = MergeInputFiles;
	}

	// Loop through the parts to create the final list of files.
	List<File> fileList = new ArrayList<>();
	for ( String part : parts ) {
		part = part.trim();
		// Always use Linux path separator because it simplifies glob evaluation.
		String partFull = IOUtil.verifyPathForOS(
       		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
       			TSCommandProcessorUtil.expandParameterValue(processor,this,part) ) ).replace("\\", "/");
		if ( !StringUtil.containsAny(partFull, "*{?[", false) ) { // } to allow editor bracket matching
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
				// Convert to strings to sort.
				List<String> partFileStringList = new ArrayList<>();
				for ( File partFile : partFileList ) {
					partFileStringList.add(partFile.getAbsolutePath());
				}
				Collections.sort(partFileStringList, String.CASE_INSENSITIVE_ORDER);
				for ( String partFileString : partFileStringList ) {
					fileList.add(new File(partFileString));
				}
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

	// Remove files that are not PDF.
	if ( fileList.size() > 0 ) {
		for ( int i = fileList.size() - 1; i >= 0; i-- ) {
			File file = fileList.get(i);
			if ( ! file.getName().toUpperCase().endsWith("PDF") ) {
				// File does not end in PDF so remove from the merge list to avoid errors.
				fileList.remove(i);
			}
		}
	}

	if ( fileList.size() == 0 ) {
		// No text and no files.
	    message = "Unable to match any PDF files using MergeInputFiles=\"" + MergeInputFiles + "\"";
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

	// Merge.
	String MergeOutputFile_full = MergeOutputFile;

	try {
		if ( pdfCommand == PDFCommandType.MERGE_FILES ) {
			MergeOutputFile_full = IOUtil.verifyPathForOS(
	    		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        		TSCommandProcessorUtil.expandParameterValue(processor,this,MergeOutputFile) ) );

			// The PDFBox code might mess with Swing fonts, for example odd errors loading the command.
			// ChatGPT suggested running in a thread but this does not seem to solve the problem.
			// Leave in for now and deal with it later.
			// It seems that loading a different command file first and then the PDF Merge command always works?
			boolean doSeparateThread = false;
			if ( doSeparateThread ) {
				Thread pdfThread = new Thread (() -> {
					try {
						final String MergeOutputFile_full_final = IOUtil.verifyPathForOS(
							IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
								TSCommandProcessorUtil.expandParameterValue(processor,this,MergeOutputFile) ) );
						final String IfNotFound_final = "";
						final int warning_count_final = 0;
						doMerge ( MergeOutputFile_full_final, IfNotFound_final, fileList, status, command_tag, warning_level, warning_count_final );
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
				doMerge ( MergeOutputFile_full, IfNotFound, fileList, status, command_tag, warning_level, warning_count );
			}

    		// Save the output file name.
    		setMergeOutputFile ( new File(MergeOutputFile_full));
		}
	}
	catch ( Exception e ) {
		if ( pdfCommand == PDFCommandType.MERGE_FILES ) {
			message = "Unexpected error merging PDF file(s) as \"" + MergeOutputFile_full + "\" (" + e + ").";
		}
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
@param file PDF merge output file to create
*/
private void setMergeOutputFile ( File file ) {
    __MergeOutputFile_File = file;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"PDFCommand",
		"MergeInputFiles",
		"MergeOutputFile",
		"IfNotFound"
	};
	return this.toString(parameters, parameterOrder);
}

}