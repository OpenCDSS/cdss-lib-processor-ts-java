// WriteObjectToJSON_Command - This class initializes, checks, and runs the WriteObjectToJSON() command.

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

package rti.tscommandprocessor.commands.json;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.JSON.JSONObject;

/**
This class initializes, checks, and runs the WritePropertiesToFile() command.
*/
public class WriteObjectToJSON_Command extends AbstractCommand implements FileGenerator
{

/**
Values for PrettyPrint property.
*/
protected final String _False = "False";
protected final String _True = "True";
	
/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteObjectToJSON_Command ()
{	super();
	setCommandName ( "WriteObjectToJSON" );
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
{	String ObjectID = parameters.getValue ( "ObjectID" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String PrettyPrint = parameters.getValue ( "PrettyPrint" );
	String Indent = parameters.getValue ( "Indent" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	//CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (ObjectID == null) || ObjectID.isEmpty()) {
        message = "The object identifier for the object to write has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid object identifier." ) );
    }

	CommandProcessor processor = getCommandProcessor();
	if ( (OutputFile == null) || OutputFile.isEmpty() ) {
		message = "The output file: \"" + OutputFile + "\" must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify an output file." ) );
	}
	else if ( OutputFile.indexOf("${") < 0 ) {
	    String working_dir = null;
		try {
		    Object o = processor.getPropContents ( "WorkingDir" );
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
		// Expand for ${} properties...
		OutputFile = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputFile);
		try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, OutputFile));
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				message = "The output parent directory does " +
				"not exist for the output file: \"" + adjusted_path + "\".";
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
			"\"\ncannot be adjusted to an absolute path using the working directory:\n" +
			"    \"" + working_dir + "\".";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that output file and working directory paths are compatible." ) );
		}
	}
	
   if ( (PrettyPrint != null) && !PrettyPrint.isEmpty() &&
	   !PrettyPrint.equals(_False) && !PrettyPrint.equals(_True) ) {
       message = "The PrettyPrint value (" + PrettyPrint + ") is invalid.";
       warning += "\n" + message;
       status.addToLog ( CommandPhaseType.INITIALIZATION,
           new CommandLogRecord(CommandStatusType.FAILURE,
               message, "Specify PrettyPrint as " + _False + " (default), or " + _True + "." ) );
   }

   if ( (Indent != null) && !Indent.isEmpty() && !StringUtil.isInteger(Indent) ) {
       message="The indent (" + Indent + ") is not a valid value.";
       warning += "\n" + message;
       status.addToLog ( CommandPhaseType.INITIALIZATION,
           new CommandLogRecord(CommandStatusType.FAILURE,
               message, "Specify the indent as an integer (default=2 if not specified).") );
   }
   
	
	// Check for invalid parameters...
	List<String> validList = new ArrayList<>(4);
	validList.add ( "ObjectID" );
	validList.add ( "OutputFile" );
	validList.add ( "PrettyPrint" );
	validList.add ( "Indent" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed.
    List<String> objectIDChoices =
        TSCommandProcessorUtil.getObjectIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	return (new WriteObjectToJSON_JDialog ( parent, this, objectIDChoices )).ok();
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
@param command_number Command number in sequence.
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
	
	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
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
		status.clearLog(CommandPhaseType.RUN);
	}
	
	// Clear the output file
	
	setOutputFile ( null );
	
	// Check whether the processor wants output files to be created...

	if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
		Message.printStatus ( 2, routine,
		"Skipping \"" + toString() + "\" because output is not being created." );
	}

	PropList parameters = getCommandParameters();
    String ObjectID = parameters.getValue ( "ObjectID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	// In discovery mode want lists of objects to include ${Property}.
    	ObjectID = TSCommandProcessorUtil.expandParameterValue(processor, this, ObjectID);
    }
	String OutputFile = parameters.getValue ( "OutputFile" ); // Expanded below.
	String PrettyPrint = parameters.getValue ( "PrettyPrint" );
    boolean prettyPrint = true;
    if ( PrettyPrint != null ) {
    	if ( PrettyPrint.equalsIgnoreCase(_False) ) {
    		prettyPrint = false;
    	}
    }
	String Indent = parameters.getValue ( "Indent" );
	int indent = 2;
	if ( (Indent != null) && StringUtil.isInteger(Indent) ) {
		indent = Integer.valueOf(Indent);
	}

	// Now try to write.

	String OutputFile_full = OutputFile;
	try {
		// Convert to an absolute path.
		OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor, this, OutputFile) ) );

		// Get the object.
		PropList request_params = new PropList ( "" );
		request_params.set ( "ObjectID", ObjectID );
		CommandProcessorRequestResultsBean bean = null;
		try {
			bean =  processor.processRequest( "GetObject", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting GetObject(ObjectID=\"" + ObjectID + "\") from processor.";
			Message.printWarning(2, routine, message );
			Message.printWarning ( 3, routine, e );
		}
		PropList bean_PropList = bean.getResultsPropList();
		Object o_object = bean_PropList.getContents ( "Object" );
		JSONObject object = null;
		if ( o_object == null ) {
			message = "Null object returned from processor for GetObject(ObjectID=\"" + ObjectID + "\").";
			Message.printWarning ( 2, routine, message );
		}
		else {
			object = (JSONObject)o_object;
		}

    	// Write the object.
       	if ( prettyPrint ) {
       		// Write using pretty printing with indent.
       		//mapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(OutputFile_full).toFile(), object);
       		ObjectMapper mapper = new ObjectMapper();
       		mapper.registerModule(new JavaTimeModule());
       		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
       		mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
       		String indentSpaces = "";
       		for ( int i = 0; i < indent; i++ ) {
       			indentSpaces += " ";
       		}
       		DefaultPrettyPrinter printer = new DefaultPrettyPrinter().withObjectIndenter(
       			new DefaultIndenter(indentSpaces, "\n"));
       		if ( object.getObjectMap() != null ) {
       			mapper.writer(printer).writeValue(Paths.get(OutputFile_full).toFile(), object.getObjectMap());
       		}
       		else if ( object.getObjectArray() != null ) {
       			mapper.writer(printer).writeValue(Paths.get(OutputFile_full).toFile(), object.getObjectArray());
       		}
       	}
       	else {
       		// Write with default formatting (not pretty).
       		ObjectMapper mapper = new ObjectMapper();
       		mapper.registerModule(new JavaTimeModule());
       		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
       		mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
       		if ( object.getObjectMap() != null ) {
       			mapper.writeValue(Paths.get(OutputFile_full).toFile(), object.getObjectMap());
       		}
       		else if ( object.getObjectArray() != null ) {
       			mapper.writeValue(Paths.get(OutputFile_full).toFile(), object.getObjectArray());
       		}
       	}
		// Save the output file name.
		setOutputFile ( new File(OutputFile_full));
	}
	catch ( Exception e ) {
		message = "Unexpected error writing object to file \"" + OutputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE, message, "Check log file for details." ) );
		throw new CommandException ( message );
	}
	
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file )
{
	__OutputFile_File = file;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"ObjectID",
		"OutputFile",
		"PrettyPrint",
		"Indent"
	};
	return this.toString(parameters, parameterOrder);
}

}