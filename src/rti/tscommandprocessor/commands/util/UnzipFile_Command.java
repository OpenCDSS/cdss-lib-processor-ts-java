// UnzipFile_Command - This class initializes, checks, and runs the UnzipFile() command.

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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.GUI.ResponseJDialog;
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
import RTi.Util.IO.GzipToolkit;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.IO.ZipToolkit;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;

/**
This class initializes, checks, and runs the UnzipFile() command.
*/
public class UnzipFile_Command extends AbstractCommand
implements Command, FileGenerator
{

/**
Data members used for parameter values.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

protected final String _False = "False";
protected final String _True = "True";

protected final String _TrueWithPrompt = "TrueWithPrompt";

/**
Output file that is created by this command.
*/
private List<File> __outputFileList = null;

/**
Constructor.
*/
public UnzipFile_Command ()
{	super();
	setCommandName ( "UnzipFile" );
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
{	String InputFile = parameters.getValue ( "InputFile" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String OutputFolder = parameters.getValue ( "OutputFolder" );
	String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
	String RemoveOutputFolder = parameters.getValue ( "RemoveOutputFolder" );
	String ListInResults = parameters.getValue ( "ListInResults" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the file to append is not checked during initialization
	// because files may be created dynamically at runtime.

	if ( (InputFile == null) || InputFile.isEmpty() ) {
		message = "The input file must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the input file."));
	}
	if ( ((OutputFile == null) || OutputFile.isEmpty()) && ((OutputFolder == null) || OutputFolder.isEmpty()) ) {
		message = "The output file or output folder must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the input file."));
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
    if ( (RemoveOutputFolder != null) && !RemoveOutputFolder.isEmpty() &&
        !RemoveOutputFolder.equalsIgnoreCase(_True) &&
        !RemoveOutputFolder.equalsIgnoreCase(_TrueWithPrompt) &&
        !RemoveOutputFolder.equalsIgnoreCase(_False) ) {
        message = "The RemoveOutputFolder parameter \"" + RemoveOutputFolder + "\" must be " + _False + ", " +
        	_True + ", or " + _TrueWithPrompt + ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Correct the RemoveOutputFolder parameter " + _False + " (default) or " +
                _True + ", or " + _TrueWithPrompt + "." ) );
    }
    if ( (ListInResults != null) && !ListInResults.isEmpty() &&
        !ListInResults.equalsIgnoreCase(_True) &&
        !ListInResults.equalsIgnoreCase(_False) ) {
        message = "The ListInResults parameter \"" + ListInResults + "\" must be " + _True + " or " + _False + ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Correct the ListInResults parameter " + _True + " (default) or " + _False + "." ) );
    }
	// Check for invalid parameters...
	List<String> validList = new ArrayList<>(6);
	validList.add ( "InputFile" );
	validList.add ( "OutputFile" );
	validList.add ( "OutputFolder" );
	validList.add ( "IfInputNotFound" );
	validList.add ( "RemoveOutputFolder" );
	validList.add ( "ListInResults" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new UnzipFile_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
    if ( getOutputFileList() != null ) {
        return getOutputFileList();
    }
    else {
    	return new ArrayList<File>();
    }
}

/**
Return the output files generated by this file.  This method is used internally.
*/
private List<File> getOutputFileList ()
{
    return __outputFileList;
}

/**
Run the command.
@param command_line Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
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
    	// Should not happen
    }
    if ( clearStatus ) {
		status.clearLog(CommandPhaseType.RUN);
	}
	
    // Clear the output file list
    setOutputFileList ( null );
	
	String InputFile = parameters.getValue ( "InputFile" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String OutputFolder = parameters.getValue ( "OutputFolder" );
	String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
	if ( (IfInputNotFound == null) || IfInputNotFound.equals("")) {
	    IfInputNotFound = _Warn; // Default
	}
	String RemoveOutputFolder = parameters.getValue ( "RemoveOutputFolder" );
	if ( (RemoveOutputFolder == null) || RemoveOutputFolder.isEmpty() ) {
		RemoveOutputFolder = _False; // Default
	}
	String ListInResults = parameters.getValue ( "ListInResults" );
	boolean listInResults = true;
	if ( (ListInResults != null) && ListInResults.equalsIgnoreCase(_False) ) {
	    listInResults = false;
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	// Process the zip file.
    String InputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
	String OutputFile_full = null;
	if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
		OutputFile_full = IOUtil.verifyPathForOS(
		    IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
		        TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
	}
	String OutputFolder_full = null;
	if ( (OutputFolder != null) && !OutputFolder.isEmpty() ) {
		OutputFolder_full = IOUtil.verifyPathForOS(
			IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
				TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFolder)));
	}
	else {
		// Default to the folder for the input file
		File f = new File(InputFile_full);
		OutputFolder_full = f.getParent();
	}
	try {
		// Remove the output folder if requested.
		File outputFolder = new File(OutputFolder_full);
		boolean okToUnzip = true;
		if ( outputFolder.exists() ) {
			boolean doRemove = false;
			if ( RemoveOutputFolder.equalsIgnoreCase(_True) ) {
				doRemove = true;
			}
			else if ( RemoveOutputFolder.equalsIgnoreCase(_TrueWithPrompt) ) {
				// Prompt for confirmation:
				// - this will interrupt the workflow
				// - if the UI is not defined, always fail since no way to prompt and confirm
				if ( Message.getTopLevel() == null ) {
					message = "No UI and requesting prompt for output folder delete.";
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
					status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Only request prompt if running using the user interface."));
					okToUnzip = false;
				}
				else {
					int x = new ResponseJDialog ( Message.getTopLevel(), "Delete Output Folder?",
						"Are you sure you want to delete the output folder before unzipping?\n   "
						+ OutputFolder_full,
						ResponseJDialog.YES|ResponseJDialog.NO|ResponseJDialog.CANCEL).response();
					if ( x == ResponseJDialog.YES ) {
						doRemove = true;
					}
					else if ( x == ResponseJDialog.CANCEL ) {
						doRemove = false;
						okToUnzip = false;
					}
					else {
						doRemove = false;
					}
				}
			}
			if ( doRemove ) {
				if ( ! IOUtil.deleteDirectory(outputFolder) ) {
					message = "Output folder could not be deleted.";
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
					status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that no other program is using files in the folder."));
					okToUnzip = false;
				}
			}
		}
		if ( okToUnzip ) {
			File in = new File(InputFile_full);
	    	if ( !in.exists() ) {
	        	// Input file does not exist so generate a warning
	        	message = "Input file \"" + InputFile + "\" does not exist.";
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
	    	else if ( in.exists() && InputFile_full.toUpperCase().endsWith("ZIP") ) {
				ZipToolkit zt = new ZipToolkit();
				List<String> outputFileList0 = zt.unzipFileToFolder(InputFile_full,OutputFolder_full,true);
	        	// TODO SAM 2015-10-13 Evaluate this to make sure it works with single zipped file and maybe directory.
	        	// Save the output file name...
				if ( listInResults ) {
					List<File> outputFileList = new ArrayList<>();
					for ( String outputFile: outputFileList0 ) {
						outputFileList.add(new File(outputFile));
					}
					setOutputFileList ( outputFileList );
				}
	    	}
	    	else if ( in.exists() && InputFile_full.toUpperCase().endsWith("GZ") ) {
				GzipToolkit zt = new GzipToolkit();
	        	String unzippedFile = zt.unzipFileToFolder(InputFile_full,OutputFolder_full,null);
	        	if ( listInResults ) {
		        	// Save the output file name...
		        	List<File> outputFileList = new ArrayList<>();
		        	outputFileList.add(new File(unzippedFile));
		        	setOutputFileList ( outputFileList );
	        	}
	    	}
	    	else {
	    		message = "Do not know how to unzip with '" +
	    			IOUtil.getFileExtension(InputFile_full) + "' file extension.";
            	Message.printWarning ( warning_level,
            		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            	status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            		message, "Can handle .zip and .gz extension."));
	    	}
		}
		else {
			// Something was wrong so show another warning.
			message = "There was a problem and could not unzip the file.";
           	Message.printWarning ( warning_level,
           		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
           	status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
           		message, "Verify that zip file is available and can be unzipped to the output location."));
		}
	}
    catch ( Exception e ) {
		message = "Unexpected error unzipping file \"" + InputFile_full + "\" to \"" +
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
Set the output files that are created by this command.  This is only used internally.
*/
private void setOutputFileList ( List<File> fileList )
{
    __outputFileList = fileList;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"InputFile",
		"OutputFile",
		"OutputFolder",
		"IfInputNotFound",
		"RemoveOutputFolder",
		"ListInResults"
	};
	return this.toString(parameters, parameterOrder);
}

}