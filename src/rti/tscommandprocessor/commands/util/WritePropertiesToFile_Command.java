package rti.tscommandprocessor.commands.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.FileWriteModeType;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.PropertyFileFormatType;

/**
This class initializes, checks, and runs the WritePropertiesToFile() command.
*/
public class WritePropertiesToFile_Command extends AbstractCommand implements Command, FileGenerator
{
	
/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WritePropertiesToFile_Command ()
{	super();
	setCommandName ( "WritePropertiesToFile" );
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
{	String OutputFile = parameters.getValue ( "OutputFile" );
	//String IncludeProperty = parameters.getValue ( "IncludeProperty" );
	String WriteMode = parameters.getValue ( "WriteMode" );
	String FileFormat = parameters.getValue ( "FileFormat" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	//CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
		message = "The output file: \"" + OutputFile + "\" must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify an output file." ) );
	}
	/* Don't check because ${} properties may be used
	 * TODO SAM 2008-07-31 Need to enable check in some form.
	else {
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
		working_dir = TSCommandProcessorUtil.expandParameterValue(processor, this, working_dir);
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
	*/

	/* TODO Check to make sure properties are valid (but may be dynamic)
	if ( (PropertyName == null) || (PropertyName.length() == 0) ) {
		message = "A property name must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify a property name." ) );
	}
	else {
		List valid_properties = new Vector(TSCommandProcessorUtil.getPropertyNameList(processor));
		int size = 0;
		if ( valid_properties != null ) {
			size = valid_properties.size();
		}
		boolean found = false;
		for ( int i = 0; i < size; i++ ) {
			if ( PropertyName.equalsIgnoreCase((String)valid_properties.get(i))) {
				found = true;
				break;
			}
		}
		if ( !found ) {
			message = "The property name \"" + PropertyName + "\" is not valid.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid property name (use command editor choice)." ) );
		}
	}
	*/
	
	if ( (WriteMode != null) && (WriteMode.length() != 0) &&
	    !WriteMode.equalsIgnoreCase("" + FileWriteModeType.APPEND) &&
	    !WriteMode.equalsIgnoreCase("" + FileWriteModeType.OVERWRITE)) {
		message="WriteMode (" + WriteMode + ") is not a valid value.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify as " + FileWriteModeType.APPEND + " or " +
				FileWriteModeType.OVERWRITE + " (default)." ) );
	}
	
   if ( (FileFormat != null) && (FileFormat.length() != 0) &&
       !FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.NAME_VALUE) &&
       !FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.NAME_TYPE_VALUE) &&
       !FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON)) {
       message="FileFormat (" + FileFormat + ") is not a valid value.";
       warning += "\n" + message;
       status.addToLog ( CommandPhaseType.INITIALIZATION,
           new CommandLogRecord(CommandStatusType.FAILURE,
               message, "Specify as " + PropertyFileFormatType.NAME_VALUE + ", " +
               PropertyFileFormatType.NAME_TYPE_VALUE + " (default), " +
               PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON + "." ) );
   }
	
	// Check for invalid parameters...
	List<String> valid_Vector = new Vector();
	valid_Vector.add ( "OutputFile" );
	valid_Vector.add ( "IncludeProperty" );
	valid_Vector.add ( "WriteMode" );
	valid_Vector.add ( "FileFormat" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

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
{	// The command will be modified if changed...
	return (new WritePropertiesToFile_JDialog ( parent, this )).ok();
}

/**
Return the list of supported FileFormat parameter choices.
@return the list of supported FileFormat parameter choices.
*/
protected List<PropertyFileFormatType> getFileFormatChoices ()
{
    List<PropertyFileFormatType> fileFormatChoices = new Vector();
    fileFormatChoices.add ( PropertyFileFormatType.NAME_TYPE_VALUE );
    fileFormatChoices.add ( PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON );
    fileFormatChoices.add ( PropertyFileFormatType.NAME_VALUE );
    return fileFormatChoices;
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
	List<File> list = new Vector();
	if ( getOutputFile() != null ) {
		list.add ( getOutputFile() );
	}
	return list;
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
	return __OutputFile_File;
}

/**
Return the list of supported WriteMode parameter choices.
@return the list of supported WriteMode parameter choices.
*/
protected List<FileWriteModeType> getWriteModeChoices ()
{
    List<FileWriteModeType> writeModeChoices = new Vector();
    writeModeChoices.add ( FileWriteModeType.APPEND );
    writeModeChoices.add ( FileWriteModeType.OVERWRITE );
    // TODO SAM 2012-07-27 Need to update
    //writeModeChoices.add ( FileWriteModeType.UPDATE );
    return writeModeChoices;
}

// parseCommand is base class

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
	
	// Clear the output file
	
	setOutputFile ( null );
	
	// Check whether the processor wants output files to be created...

	CommandProcessor processor = getCommandProcessor();
	if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
		Message.printStatus ( 2, routine,
		"Skipping \"" + toString() + "\" because output is not being created." );
	}

	PropList parameters = getCommandParameters();
	String OutputFile = parameters.getValue ( "OutputFile" );
	String IncludeProperty = parameters.getValue ( "IncludeProperty" );
	String [] includeProperty = new String[0];
	if ( (IncludeProperty != null) && !IncludeProperty.equals("") ) {
	    if ( IncludeProperty.indexOf(",") > 0 ) {
	        includeProperty = IncludeProperty.split(",");
	    }
	    else {
	        includeProperty = new String[1];
	        includeProperty[0] = IncludeProperty.trim();
	    }
        // Also convert glob-style wildcard * to internal Java wildcard
	    if ( IncludeProperty.indexOf('*') >= 0 ) {
	    	for ( int i = 0; i < includeProperty.length; i++ ) {
	    		includeProperty[i] = includeProperty[i].replace("*", ".*");
	    	}
	    }
	}
	String WriteMode = parameters.getValue ( "WriteMode" );
    FileWriteModeType writeMode = FileWriteModeType.valueOfIgnoreCase(WriteMode);
    if ( writeMode == null ) {
        writeMode = FileWriteModeType.OVERWRITE; // Default
    }
    String FileFormat = parameters.getValue ( "FileFormat" );
    PropertyFileFormatType fileFormat = PropertyFileFormatType.valueOfIgnoreCase(FileFormat);
    if ( fileFormat == null ) {
        fileFormat = PropertyFileFormatType.NAME_TYPE_VALUE; // Default
    }
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);

	// Now try to write...

	String OutputFile_full = OutputFile;
	try {
		// Convert to an absolute path...
		OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor, this, OutputFile) ) );
	    List<String> problems = new ArrayList<String>();
		writePropertyFile ( processor, OutputFile_full, includeProperty, writeMode, fileFormat, problems );
		for ( String problem : problems ) {
			Message.printWarning ( warning_level, 
				MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, problem );
			status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE, problem, "Check log file for details." ) );
		}
		// Save the output file name...
		setOutputFile ( new File(OutputFile_full));
	}
	catch ( Exception e ) {
		message = "Unexpected error writing property to file \"" + OutputFile_full + "\" (" + e + ").";
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
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String OutputFile = parameters.getValue ( "OutputFile" );
	String IncludeProperty = parameters.getValue ( "IncludeProperty" );
	String WriteMode = parameters.getValue ( "WriteMode" );
	String FileFormat = parameters.getValue ( "FileFormat" );
	StringBuffer b = new StringBuffer ();
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}
	if ( (IncludeProperty != null) && (IncludeProperty.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IncludeProperty=\"" + IncludeProperty + "\"" );
	}
	if ( (WriteMode != null) && (WriteMode.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "WriteMode=" + WriteMode );
	}
    if ( (FileFormat != null) && (FileFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FileFormat=" + FileFormat );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

/**
Write a single property to the output file.
*/
private void writeProperty ( PrintWriter fout, String propertyName, Object propertyObject, PropertyFileFormatType formatType )
{
    // Write the output...
    String quote = "";
    boolean doDateTime = false;
    // Only use double quotes around String and DateTime objects
    if ( propertyObject instanceof DateTime ) {
        doDateTime = true;
    }
    if ( (propertyObject instanceof String) || doDateTime ) {
        quote = "\"";
    }
    if ( formatType == PropertyFileFormatType.NAME_VALUE ) {
        fout.println ( propertyName + "=" + quote + propertyObject + quote );
    }
    else if ( formatType == PropertyFileFormatType.NAME_TYPE_VALUE ) {
        if ( doDateTime ) {
            fout.println ( propertyName + "=DateTime(" + quote + propertyObject + quote + ")" );
        }
        else {
            // Same as NAME_VALUE
            fout.println ( propertyName + "=" + quote + propertyObject + quote );
        }
    }
    else if ( formatType == PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON ) {
        if ( doDateTime ) {
            DateTime dt = (DateTime)propertyObject;
            StringBuffer dtBuffer = new StringBuffer();
            dtBuffer.append("" + dt.getYear() );
            if ( dt.getPrecision() <= DateTime.PRECISION_MONTH ) {
                dtBuffer.append("," + dt.getMonth() );
            }
            if ( dt.getPrecision() <= DateTime.PRECISION_DAY ) {
                dtBuffer.append("," + dt.getDay() );
            }
            if ( dt.getPrecision() <= DateTime.PRECISION_HOUR ) {
                dtBuffer.append("," + dt.getHour() );
            }
            if ( dt.getPrecision() <= DateTime.PRECISION_MINUTE ) {
                dtBuffer.append("," + dt.getMinute() );
            }
            if ( dt.getPrecision() <= DateTime.PRECISION_SECOND ) {
                dtBuffer.append("," + dt.getSecond() );
            }
            // TODO SAM 2012-07-30 Evaluate time zone
            fout.println ( propertyName + "=DateTime(" + dtBuffer + ")" );
        }
        else {
            // Same as NAME_VALUE
            fout.println ( propertyName + "=" + quote + propertyObject + quote );
        }
    }
}

// TODO SAM 2012-07-27 Evaluate putting this in more generic code, perhaps IOUtil.PropList.writePersistent()?
/**
Write the property file.
*/
private List<String> writePropertyFile ( CommandProcessor processor, String outputFileFull,
    String [] includeProperty, FileWriteModeType writeMode, PropertyFileFormatType formatType, List<String> problems )
{
    PrintWriter fout = null;
    try {
        // Open the file...
        boolean doAppend = false; // Default is overwrite
        if ( writeMode == FileWriteModeType.APPEND ) {
            doAppend = true;
        }
        fout = new PrintWriter ( new FileOutputStream ( outputFileFull, doAppend ) );
        // Get all the user-specified properties
        Hashtable<String,Object> userProps = null;
        try {
        	PropList requestProps = new PropList("");
        	requestProps.set("GetUserProperties=True");
        	CommandProcessorRequestResultsBean r = processor.processRequest("GetPropertyHashtable", requestProps);
        	PropList resultsProps = r.getResultsPropList();
        	Prop prop = resultsProps.getProp("PropertyHashtable");
        	if ( prop != null ) {
        		userProps = (Hashtable<String,Object>)prop.getContents();
        	}
        }
        catch ( Exception e ) {
            problems.add("Error requesting user-specified property hashtable from processor (" + e + ")." );
        }
        // Loop through property names and retrieve from the processor
        for ( int i = 0; i < includeProperty.length; i++ ) {
            //Message.printStatus(2, "", "Writing property \"" + includeProperty[i] + "\"" );
            if ( includeProperty[i].indexOf("*") >= 0 ) {
            	// Includes wildcards.  Check the user-specified properties
            	Set<String> keys = userProps.keySet();
            	for ( String key : keys ) {
            		if ( key.matches(includeProperty[i]) ) {
            			// Have a match so output
            			writeProperty(fout,key,userProps.get(key),formatType);
            		}
            	}
            }
            else {
	            // Get the property to output...
	            Object propertyObject = null;
	            try {
	                propertyObject = processor.getPropContents ( includeProperty[i] );
	            }
	            catch ( Exception e ) {
	                problems.add("Error requesting property named \"" + includeProperty[i] + "\" from processor (" +
	                    e + ")." );
	                continue;
	            }
	            writeProperty(fout,includeProperty[i],propertyObject,formatType);
            }
        }
    }
    catch ( FileNotFoundException e ) {
        problems.add("Error opening output file \"" + outputFileFull + "\" (" + e + ")." );
    }
    finally {
        if ( fout != null ) {
            fout.close();
        }
    }
    return problems;
}

}