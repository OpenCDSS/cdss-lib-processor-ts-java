// CopyFile_Command - This class initializes, checks, and runs the CopyFile() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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
This class initializes, checks, and runs the CopyFile() command.
*/
public class CopyFile_Command extends AbstractCommand
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
public CopyFile_Command ()
{	super();
	setCommandName ( "CopyFile" );
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
    String TempFolder = parameters.getValue ( "TempFolder" );
    String TempFilePrefix = parameters.getValue ( "TempFilePrefix" );
    String TempFileSuffix = parameters.getValue ( "TempFileSuffix" );
    String TempFileProperty = parameters.getValue ( "TempFileProperty" );
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

	int count = 0;
	if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
		++count;
	}
	if ( ((TempFolder != null) && !TempFolder.isEmpty()) ||
	   ((TempFilePrefix != null) && !TempFilePrefix.isEmpty()) ||
	   ((TempFileSuffix != null) && !TempFileSuffix.isEmpty()) ||
	   ((TempFileProperty != null) && !TempFileProperty.isEmpty()) ) {
		++count;
	}
	if ( count == 0 ) {
		message = "The output file OR temporary file parameter(s) must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the output file or at least one temporary file parameter."));
	}
	else if ( count == 2 ) {
		message = "The output file OR temporary file parameter(s) must be specified (but not both).";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the output file or at least one temporary file parameter."));
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
	List<String> validList = new ArrayList<>(7);
	validList.add ( "InputFile" );
	validList.add ( "OutputFile" );
	validList.add ( "TempFolder" );
	validList.add ( "TempFilePrefix" );
	validList.add ( "TempFileSuffix" );
	validList.add ( "TempFileProperty" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed.
	return (new CopyFile_JDialog ( parent, this )).ok();
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
    Boolean clearStatus = new Boolean(true); // Default.
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
	String TempFolder = parameters.getValue ( "TempFolder" ); // Expand below.
	String TempFilePrefix = parameters.getValue ( "TempFilePrefix" );
	TempFilePrefix = TSCommandProcessorUtil.expandParameterValue(processor, this, TempFilePrefix);
	String TempFileSuffix = parameters.getValue ( "TempFileSuffix" );
	TempFileSuffix = TSCommandProcessorUtil.expandParameterValue(processor, this, TempFileSuffix);
	String TempFileProperty = parameters.getValue ( "TempFileProperty" );
	TempFileProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, TempFileProperty);
	String IfInputNotFound = parameters.getValue ( "IfInputNotFound" );
	if ( (IfInputNotFound == null) || IfInputNotFound.equals("")) {
	    IfInputNotFound = _Warn; // Default
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
   	String InputFile_full = InputFile;
   	String TempFolder_full = TempFolder;
	String OutputFile_full = OutputFile;
	try {
		// Process the files.  Each input file is opened to scan the file.
		// The output file is opened once in append mode.
    	// TODO SAM 2014-02-03 Enable copying a list to a folder, etc. see AppendFile() for example.
    	InputFile_full = IOUtil.verifyPathForOS(
        	IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            	TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
    	// Set parameters to null if empty since File method handles null.
		if ( (TempFilePrefix != null) && TempFilePrefix.isEmpty() ) {
			TempFilePrefix = null;
		}
		if ( (TempFileSuffix != null) && TempFileSuffix.isEmpty() ) {
			TempFileSuffix = null;
		}
		if ( ((TempFolder != null) && !TempFolder.isEmpty()) ||
	    	((TempFilePrefix != null) && !TempFilePrefix.isEmpty()) ||
	    	((TempFileSuffix != null) && !TempFileSuffix.isEmpty()) ||
	    	((TempFileProperty != null) && !TempFileProperty.isEmpty()) ) {
			// Have at least one temporary file parameter so use a temporary file:
			// - use the Files class because File method does not allow null prefix
			// - the Java method will create an empty temporary file, which will be overwritten by the copy
			Path tempFile = null;
			if ( (TempFolder != null) && !TempFolder.isEmpty() ) {
				// Use the specified temporary folder.
				TempFolder_full = IOUtil.verifyPathForOS(
					IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
						TSCommandProcessorUtil.expandParameterValue(processor,this,TempFolder)));
				tempFile = Files.createTempFile( Paths.get(TempFolder_full), TempFilePrefix, TempFileSuffix );
			}
			else {
				// Use the default temporary folder.
				tempFile = Files.createTempFile(TempFilePrefix, TempFileSuffix);
			}
			OutputFile_full = tempFile.toAbsolutePath().toString();
    	}
    	else {
    		OutputFile_full = IOUtil.verifyPathForOS(
    			IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
    				TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
    	}

	    File in = new File(InputFile_full);
	    if ( in.exists() ) {
	        IOUtil.copyFile(new File(InputFile_full), new File(OutputFile_full) );
	        // Save the output file name.
	        setOutputFile ( new File(OutputFile_full));

	        if ( (TempFileProperty != null) && !TempFileProperty.isEmpty() ) {
	        	PropList request_params = new PropList ( "" );
	        	request_params.setUsingObject ( "PropertyName", TempFileProperty );
	        	request_params.setUsingObject ( "PropertyValue", OutputFile_full );
	        	try {
	        		processor.processRequest( "SetProperty", request_params);
	        	}
	        	catch ( Exception e ) {
	        		message = "Error requesting SetProperty(Property=\"" + TempFileProperty + "\") from processor.";
	        		Message.printWarning(log_level,
	        			MessageUtil.formatMessageTag( command_tag, ++warning_count),
	        				routine, message );
	        		status.addToLog ( CommandPhaseType.RUN,
	        			new CommandLogRecord(CommandStatusType.FAILURE,
	        				message, "Report the problem to software support." ) );
	        	}
	        }
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
    catch ( Exception e ) {
		message = "Unexpected error copying file \"" + InputFile_full + "\" to \"" +
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
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String InputFile = parameters.getValue("InputFile");
	String OutputFile = parameters.getValue("OutputFile");
	String TempFolder = parameters.getValue("TempFolder");
	String TempFilePrefix = parameters.getValue("TempFilePrefix");
	String TempFileSuffix = parameters.getValue("TempFileSuffix");
	String TempFileProperty = parameters.getValue("TempFileProperty");
	String IfInputNotFound = parameters.getValue("IfInputNotFound");

	StringBuffer b = new StringBuffer ();
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"");
    }
    if ( (TempFolder != null) && (TempFolder.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TempFolder=\"" + TempFolder + "\"");
    }
    if ( (TempFilePrefix != null) && (TempFilePrefix.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TempFilePrefix=\"" + TempFilePrefix + "\"");
    }
    if ( (TempFileSuffix != null) && (TempFileSuffix.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TempFileSuffix=\"" + TempFileSuffix + "\"");
    }
    if ( (TempFileProperty != null) && (TempFileProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TempFileProperty=\"" + TempFileProperty + "\"");
    }
	if ( (IfInputNotFound != null) && (IfInputNotFound.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfInputNotFound=" + IfInputNotFound );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}