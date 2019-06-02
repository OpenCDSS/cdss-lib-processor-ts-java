// ReadPropertiesFromFile_Command - This class initializes, checks, and runs the ReadPropertiesFromFile() command.

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeUtil;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.PropertyFileFormatType;

/**
This class initializes, checks, and runs the ReadPropertiesFromFile() command.
*/
public class ReadPropertiesFromFile_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

/**
List of properties in discovery mode.
*/
List<Prop> __discoveryProps = new ArrayList<Prop>();
	
/**
Constructor.
*/
public ReadPropertiesFromFile_Command ()
{	super();
	setCommandName ( "ReadPropertiesFromFile" );
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
{	String InputFile = parameters.getValue ( "InputFile" );
    String FileFormat = parameters.getValue ( "FileFormat" );
	//String IncludeProperty = parameters.getValue ( "IncludeProperty" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	//CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	if ( (InputFile == null) || (InputFile.length() == 0) ) {
		message = "The input file: \"" + InputFile + "\" must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify an input file." ) );
	}
	/* TODO SAM 2017-03-19 need to enable in some form
	else if ( InputFile.indexOf("${") < 0 ) {
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
	List<String> validList = new ArrayList<String>(3);
	validList.add ( "InputFile" );
	validList.add ( "FileFormat" );
	validList.add ( "IncludeProperty" );
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
{	// The command will be modified if changed...
	return (new ReadPropertiesFromFile_JDialog ( parent, this )).ok();
}

/**
Return the list of discovery properties.
*/
private List<Prop> getDiscoveryProps ()
{
    return __discoveryProps;
}

/**
Return the list of supported FileFormat parameter choices.
@return the list of supported FileFormat parameter choices.
*/
protected List<PropertyFileFormatType> getFileFormatChoices ()
{
    List<PropertyFileFormatType> fileFormatChoices = new ArrayList<PropertyFileFormatType>();
    fileFormatChoices.add ( PropertyFileFormatType.NAME_TYPE_VALUE );
    fileFormatChoices.add ( PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON );
    fileFormatChoices.add ( PropertyFileFormatType.NAME_VALUE );
    return fileFormatChoices;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  Prop
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
    List<Prop> discoveryProps = getDiscoveryProps ();
    if ( discoveryProps == null ) {
        return null;
    }
    Prop prop = new Prop();
    // Check for Prop request...
    if ( c == prop.getClass() ) {
        return (List<T>)discoveryProps;
    }
    else {
        return null;
    }
}

// parseCommand is base class

//TODO SAM 2012-07-27 Evaluate putting this in more generic code, perhaps IOUtil.PropList.readPersistent()?
/**
Read the property file for PropertyFileFormatType.NAME_VALUE format.
@param processor the command processor
@param commandPhase the command phase
@param inputFileFull full path to the property file being read
@param includeProperty the list of properties to read, or if empty read all
@return the list of problems, guaranteed to be non-null
*/
private List<String> readPropertyFileNameValue ( CommandProcessor processor, CommandPhaseType commandPhase,
    String inputFileFull, String [] includeProperty )
throws IOException
{	String routine = getClass().getSimpleName() + ".readPropertyFileNameValue";
    BufferedReader in = null;
    List<String> problems = new ArrayList<String>();
    try {
        // Open the file...
        in = new BufferedReader ( new InputStreamReader(IOUtil.getInputStream ( inputFileFull )) );
        // Currently all formats use Name=Value syntax, with some variations (but no XML, JSON, etc.)
        String lineString, lineStringTrimmed;
        String propName, propValue;
        int pos1, pos2;
        Object objectValue; // Generic object type to set as property
        while ( (lineString = in.readLine()) != null ) {
            lineStringTrimmed = lineString.trim();
            if ( (lineStringTrimmed.length() == 0) || (lineStringTrimmed.charAt(0) == '#') ) {
                continue;
            }
            pos1 = lineStringTrimmed.indexOf("=");
            if ( pos1 <= 1 ) {
                // Not a valid property assignment of form Property = ValueExpression
                continue;
            }
            propName = lineStringTrimmed.substring(0,pos1).trim();
            if ( includeProperty.length > 0 ) {
                // See if the requested property matches what is in the file
                boolean found = false;
                for ( int i = 0; i < includeProperty.length; i++ ) {
                    if ( propName.equalsIgnoreCase(includeProperty[i])) {
                        found = true;
                        break;
                    }
                }
                if ( !found ) {
                    continue;
                }
            }
            propValue = lineStringTrimmed.substring(pos1 + 1).trim();
            // Remove surrounding quotes if present
            if ( propValue.charAt(0) == '"') {
                propValue = propValue.substring(1);
            }
            if ( propValue.charAt(propValue.length() - 1) == '"') {
                propValue = propValue.substring(0,propValue.length() - 1);
            }
            //Message.printStatus(2,"","Property name \"" + propName + "\" has value \"" + propValue + "\".");
            // Default just treat as string
            objectValue = propValue;
            // Handle special types
            if ( propValue.toUpperCase().startsWith("DATETIME") ) {
                // Parse out the argument inside the ()
                pos1 = propValue.indexOf("(");
                pos2 = propValue.indexOf(")");
                DateTime dt = null;
                if ( (pos1 >= 0) && (pos2 > 0) ) {
                    String dtString = propValue.substring(pos1 + 1, pos2);
                    //Message.printStatus(2,"","Date/time string is \"" + dtString + "\".");
                    if ( dtString.indexOf(",") > 0 ) {
                        // Assume Pythonic notation DateTime(YYYY,MM,DD,...)
                        dt = new DateTime();
                        String [] parts = dtString.split(",");
                        if ( parts.length >= 1 ) {
                            dt.setPrecision(DateTime.PRECISION_YEAR);
                            dt.setYear(Integer.parseInt(parts[0].trim()));
                        }
                        if ( parts.length >= 2 ) {
                            dt.setPrecision(DateTime.PRECISION_MONTH);
                            dt.setMonth(Integer.parseInt(parts[1].trim()));
                        }
                        if ( parts.length >= 3 ) {
                            dt.setPrecision(DateTime.PRECISION_DAY);
                            dt.setDay(Integer.parseInt(parts[2].trim()));
                        }
                        if ( parts.length >= 4 ) {
                            dt.setPrecision(DateTime.PRECISION_HOUR);
                            dt.setHour(Integer.parseInt(parts[3].trim()));
                        }
                        if ( parts.length >= 5 ) {
                            dt.setPrecision(DateTime.PRECISION_MINUTE);
                            dt.setMinute(Integer.parseInt(parts[4].trim()));
                        }
                        if ( parts.length >= 6 ) {
                            dt.setPrecision(DateTime.PRECISION_SECOND);
                            dt.setSecond(Integer.parseInt(parts[5].trim()));
                        }
                    }
                    else {
                        // Assume DateTime("date/time string"), quotes are optional
                        dtString = dtString.replace("\"", "");
                        //Message.printStatus(2,"","Date/time string is \"" + dtString + "\".");
                        dt = DateTime.parse(dtString);
                    }
                    objectValue = dt;
                }
            }
            // TODO SAM 2012-07-30 Need to add support for Integer() and Double() for read and write
            // Now set the object in the processor
            else if ( TimeUtil.isDateTime(propValue) ) {
                objectValue = DateTime.parse(propValue);
            }
            // The order of the following is important because an integer also passes test for double
            else if ( StringUtil.isInteger(propValue) ) {
                objectValue = new Integer(propValue);
            }
            else if ( StringUtil.isDouble(propValue) ) {
                objectValue = new Double(propValue);
            }
            //Message.printStatus(2,"","Setting " + propName + "=" + objectValue );
            try {
                if ( commandPhase == CommandPhaseType.DISCOVERY ) {
                    getDiscoveryProps().add ( new Prop(propName,objectValue,"" + objectValue ) );
                }
                else if ( commandPhase == CommandPhaseType.RUN ) {
                    PropList requestParams = new PropList ( "" );
                    requestParams.setUsingObject ( "PropertyName", propName );
                    requestParams.setUsingObject ( "PropertyValue", objectValue );
                    processor.processRequest( "SetProperty", requestParams);
                    Message.printStatus(2,routine,"Setting property " + propName + "=" + objectValue );
                }
            }
            catch ( Exception e ) {
                problems.add("Error requesting SetProperty(Property=\"" + propName +
                    "\") from processor (" + e + ").");
            }
        }
    }
    catch ( FileNotFoundException e ) {
        problems.add("Error opening input file \"" + inputFileFull + "\" (" + e + ")." );
    }
    finally {
        if ( in != null ) {
            in.close();
        }
    }
    return problems;
}

/**
Read the property file for PropertyFileFormatType.NAME_TYPE_VALUE format.
@param processor the command processor
@param commandPhase the command phase
@param inputFileFull full path to the property file being read
@param includeProperty the list of properties to read, or if empty read all
@return the list of problems, guaranteed to be non-null
*/
private List<String> readPropertyFileNameTypeValue ( CommandProcessor processor, CommandPhaseType commandPhase,
    String inputFileFull, String [] includeProperty )
throws IOException
{
    // Currently the same code...
    return readPropertyFileNameValue ( processor, commandPhase, inputFileFull, includeProperty );
}

/**
Read the property file for PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON format.
@param processor the command processor
@param commandPhase the command phase
@param inputFileFull full path to the property file being read
@param includeProperty the list of properties to read, or if empty read all
@return the list of problems, guaranteed to be non-null
*/
private List<String> readPropertyFileNameTypeValuePython ( CommandProcessor processor, CommandPhaseType commandPhase,
    String inputFileFull, String [] includeProperty )
throws IOException
{
    // Currently the same code...
    return readPropertyFileNameValue ( processor, commandPhase, inputFileFull, includeProperty );
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@param command_phase The command phase that is being run (RUN or DISCOVERY).
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	CommandProcessor processor = getCommandProcessor();
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
	
	if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	    getDiscoveryProps().clear();
	}
	
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue ( "InputFile" );
    String FileFormat = parameters.getValue ( "FileFormat" );
    PropertyFileFormatType fileFormat = PropertyFileFormatType.valueOfIgnoreCase(FileFormat);
    if ( fileFormat == null ) {
        fileFormat = PropertyFileFormatType.NAME_TYPE_VALUE; // Default
    }
	String IncludeProperty = parameters.getValue ( "IncludeProperty" );
	if ( (commandPhase == CommandPhaseType.RUN) && (IncludeProperty != null) && (IncludeProperty.indexOf("${") >= 0) ) {
		IncludeProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, IncludeProperty);
	}
	String [] includeProperty = new String[0];
	if ( (IncludeProperty != null) && !IncludeProperty.equals("") ) {
	    if ( IncludeProperty.indexOf(",") > 0 ) {
	        includeProperty = IncludeProperty.split(",");
	    }
	    else {
	        includeProperty = new String[1];
	        includeProperty[0] = IncludeProperty.trim();
	    }
	}

	// Now try to read...

	String InputFile_full = InputFile;
	try {
		if ( commandPhase == CommandPhaseType.RUN ) {
			// Convert to an absolute path...
			InputFile_full = IOUtil.verifyPathForOS(
	            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	                TSCommandProcessorUtil.expandParameterValue(processor, this, InputFile) ) );
			List<String> problems = new ArrayList<String>();
			// Call the parsing method based on the file format
			if ( fileFormat == PropertyFileFormatType.NAME_VALUE ) {
			    problems = readPropertyFileNameValue ( processor, commandPhase, InputFile_full, includeProperty );
			}
			else if ( fileFormat == PropertyFileFormatType.NAME_TYPE_VALUE ) {
	            problems = readPropertyFileNameTypeValue ( processor, commandPhase, InputFile_full, includeProperty );
	        }
			else if ( fileFormat == PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON ) {
	            problems = readPropertyFileNameTypeValuePython ( processor, commandPhase, InputFile_full, includeProperty );
	        }
			for ( String problem : problems ) {
			    Message.printWarning ( warning_level, 
		            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, problem );
	            status.addToLog ( commandPhase,
	                new CommandLogRecord(CommandStatusType.FAILURE, problem, "" ) );
			}
		}
	}
	catch ( Exception e ) {
		message = "Unexpected error reading property to file \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( commandPhase,
			new CommandLogRecord(CommandStatusType.FAILURE, message, "Check log file for details." ) );
		throw new CommandException ( message );
	}
	
	status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String InputFile = parameters.getValue ( "InputFile" );
	String FileFormat = parameters.getValue ( "FileFormat" );
	String IncludeProperty = parameters.getValue ( "IncludeProperty" );
	StringBuilder b = new StringBuilder ();
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
    if ( (FileFormat != null) && (FileFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FileFormat=" + FileFormat );
    }
	if ( (IncludeProperty != null) && (IncludeProperty.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IncludeProperty=\"" + IncludeProperty + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
