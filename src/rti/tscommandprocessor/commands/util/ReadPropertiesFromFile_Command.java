// ReadPropertiesFromFile_Command - This class initializes, checks, and runs the ReadPropertiesFromFile() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
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
	 * Possible values for ExpandProperties parameter.
	 */
	protected final String _False = "False";
	protected final String _True = "True";

/**
List of properties in discovery mode.
*/
List<Prop> __discoveryProps = new ArrayList<>();

/**
Constructor.
*/
public ReadPropertiesFromFile_Command () {
	super();
	setCommandName ( "ReadPropertiesFromFile" );
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
    String FileFormat = parameters.getValue ( "FileFormat" );
    String IncludeProperties = parameters.getValue ( "IncludeProperties" );
	String IgnoreCase = parameters.getValue ( "IgnoreCase" );
	String ExpandProperties = parameters.getValue ( "ExpandProperties" );
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
			new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify an input file." ) );
	}
	/* TODO SAM 2017-03-19 need to enable in some form.
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

    if ( (FileFormat != null) && !FileFormat.isEmpty() ) {
    	if ( !FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.INI) &&
    		!FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.NAME_VALUE) &&
    		!FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.NAME_TYPE_VALUE) &&
    		!FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON) &&
    		!FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.VALUE) ) {
    		message="FileFormat (" + FileFormat + ") is not a valid value.";
    		warning += "\n" + message;
    		status.addToLog ( CommandPhaseType.INITIALIZATION,
   				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify as " +
					PropertyFileFormatType.INI + ", " +
					PropertyFileFormatType.NAME_VALUE + ", " +
					PropertyFileFormatType.NAME_TYPE_VALUE + " (default), " +
					PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON + ", " +
					PropertyFileFormatType.VALUE + "." ) );
    	}
   		if ( FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.VALUE) &&
   			((IncludeProperties == null) || IncludeProperties.isEmpty()) ) {
    		message="IncludeProperties is not specified.";
    		warning += "\n" + message;
    		status.addToLog ( CommandPhaseType.INITIALIZATION,
   				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify IncludeProperties when FileFormat=" + PropertyFileFormatType.VALUE + "." ) );
   		}
    }

	if ( (IgnoreCase != null) && !IgnoreCase.equals("") ) {
		if ( !IgnoreCase.equalsIgnoreCase(_False) && !IgnoreCase.equalsIgnoreCase(_True) ) {
			message = "The IgnoreCase parameter \"" + IgnoreCase + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " (default) or " + _True + "."));
		}
	}

	if ( (ExpandProperties != null) && !ExpandProperties.equals("") ) {
		if ( !ExpandProperties.equalsIgnoreCase(_False) && !ExpandProperties.equalsIgnoreCase(_True) ) {
			message = "The ExpandProperties parameter \"" + ExpandProperties + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " or " + _True + " (default)."));
		}
	}

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(5);
	validList.add ( "InputFile" );
	validList.add ( "FileFormat" );
	validList.add ( "IncludeProperties" ); // Prior to TSTool 14.8.0 this was IncludeProperty.
	validList.add ( "ExcludeProperties" );
	validList.add ( "IgnoreCase" );
	validList.add ( "ExpandProperties" );
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
	return (new ReadPropertiesFromFile_JDialog ( parent, this )).ok();
}

/**
Return the list of discovery properties.
*/
private List<Prop> getDiscoveryProps () {
    return __discoveryProps;
}

/**
Return the list of supported FileFormat parameter choices.
@return the list of supported FileFormat parameter choices.
*/
protected List<PropertyFileFormatType> getFileFormatChoices () {
    List<PropertyFileFormatType> fileFormatChoices = new ArrayList<>();
    fileFormatChoices.add ( PropertyFileFormatType.INI );
    fileFormatChoices.add ( PropertyFileFormatType.NAME_TYPE_VALUE );
    fileFormatChoices.add ( PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON );
    fileFormatChoices.add ( PropertyFileFormatType.NAME_VALUE );
    fileFormatChoices.add ( PropertyFileFormatType.VALUE );
    return fileFormatChoices;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  Prop
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    List<Prop> discoveryProps = getDiscoveryProps ();
    if ( discoveryProps == null ) {
        return null;
    }
    Prop prop = new Prop();
    // Check for Prop request.
    if ( c == prop.getClass() ) {
        return (List<T>)discoveryProps;
    }
    else {
        return null;
    }
}

/**
 * Parse the command.
 * Do here because TSTool 14.8.0 changed IncludeProperty (singular) to IncludeProperties (plural) to be more correct.
 */
public void parseCommand ( String commandString )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
	// First parse in the base class.
	super.parseCommand(commandString);

    // If IncludeProperty is set, convert to IncludeProperties.
    String IncludeProperty = getCommandParameters().getValue("IncludeProperty");
    if ( (IncludeProperty != null) && !IncludeProperty.isEmpty() ) {
    	getCommandParameters().set("IncludeProperties",IncludeProperty);
    }
}

/**
Read the property file for PropertyFileFormatType.INI format.
Property names have form section.propertyName.
@param processor the command processor
@param commandPhase the command phase
@param inputFileFull full path to the property file being read
@param includeProperties the array of properties to read, or if empty read all
@param excludeProperties the array of properties to ignore, or if empty read all
@param ignoreCase whether to ignore case when including/excluding property names
@param expandProperties whether to expand properties that have ${property} notation in the value
@return the list of problems, guaranteed to be non-null
*/
private List<String> readPropertyFileINI ( CommandProcessor processor, CommandPhaseType commandPhase,
    String inputFileFull, String [] includeProperties, String [] excludeProperties, boolean ignoreCase, boolean expandProperties )
throws IOException {
	String routine = getClass().getSimpleName() + ".readPropertyFileINI";
    BufferedReader in = null;
    List<String> problems = new ArrayList<>();

    try {
        // Open the file.
        in = new BufferedReader ( new InputStreamReader(IOUtil.getInputStream ( inputFileFull )) );
        // Currently all formats use Name=Value syntax, with some variations (but no XML, JSON, etc.).
        String lineString, lineStringTrimmed;
        String propName, propValue;
        int pos1;
        Object objectValue; // Generic object type to set as property.
        String section = null;
        HashMap<String,Object> sectionPropMap = null;
        while ( (lineString = in.readLine()) != null ) {
            lineStringTrimmed = lineString.trim();
            if ( (lineStringTrimmed.length() == 0) || (lineStringTrimmed.charAt(0) == '#') ) {
            	// Blank line or comment so ignore.
                continue;
            }
            if ( lineStringTrimmed.startsWith("[") && lineStringTrimmed.endsWith("]") ) {
            	// Section from [section] will be used below to set the property name.
            	section = lineStringTrimmed.substring(1,lineStringTrimmed.length() - 1).trim();
            	Message.printStatus(2, routine, "Found section \"" + section + "\"");
            	// Start a local list of properties without section in the name.
            	sectionPropMap = new HashMap<>();
            	continue;
            }
            pos1 = lineStringTrimmed.indexOf("=");
            if ( pos1 <= 1 ) {
                // Not a valid property assignment of form: Property = ValueExpression
                continue;
            }
            propName = lineStringTrimmed.substring(0,pos1).trim();
            propValue = lineStringTrimmed.substring(pos1 + 1).trim();
            // Remove surrounding quotes if present.
            if ( propValue.charAt(0) == '"') {
                propValue = propValue.substring(1);
            }
            if ( propValue.charAt(propValue.length() - 1) == '"') {
                propValue = propValue.substring(0,propValue.length() - 1);
            }
            // Handle special values to allow files to be more portable:
            if ( propValue.equals("PARENT_FOLDER") ) {
            	File f = new File(inputFileFull);
            	propValue = f.getParentFile().getAbsolutePath();
            }
            else if ( propValue.equals("PARENT_PARENT_FOLDER") ) {
            	File f = new File(inputFileFull);
            	propValue = f.getParentFile().getParentFile().getAbsolutePath();
            }
            Message.printStatus(2,"","Property name \"" + propName + "\" has value \"" + propValue + "\".");
           	if ( (commandPhase == CommandPhaseType.RUN) && expandProperties ) {
           		// Expand the property:
           		// - first try expanding using properties local to the section (have the local property name)
           		// - then expand using processor properties (have Section.PropertyName for the property name)
           		propValue = StringUtil.expandForProperties ( propValue, sectionPropMap );
          		propValue = TSCommandProcessorUtil.expandParameterValue(processor, this, propValue);
          		Message.printStatus(2,"","Property name \"" + propName + "\" has value (after expansion) \"" + propValue + "\".");
          	}
            // Treat as a string.
            objectValue = propValue;
            Message.printStatus(2,"","Setting " + propName + "=" + objectValue );
            try {
                if ( commandPhase == CommandPhaseType.DISCOVERY ) {
                    getDiscoveryProps().add ( new Prop(propName,objectValue,"" + objectValue ) );
                }
                else if ( commandPhase == CommandPhaseType.RUN ) {
                	if ( section != null ) {
                		// Save a local property without section:
                		// - do this before modifying the name with the section
                		// - this can be used to expand properties in this function prior to checking processor properties
                		sectionPropMap.put(propName, propValue);
                		// Have a section so prepend to the property name.
                		propName = section + "." + propName;
                	}
                	if ( !shouldIncludeProperty ( propName, includeProperties, excludeProperties, ignoreCase ) ) {
                		// Property is not to be included:
                		// - check here because need to consider the section
            	    	continue;
                	}
                    PropList requestParams = new PropList ( "" );
                    requestParams.setUsingObject ( "PropertyName", propName );
                    requestParams.setUsingObject ( "PropertyValue", objectValue );
                    processor.processRequest( "SetProperty", requestParams);
                    Message.printStatus(2,routine,"Setting property " + propName + "=" + objectValue );
                }
            }
            catch ( Exception e ) {
                problems.add("Error requesting SetProperty(Property=\"" + propName + "\") from processor (" + e + ").");
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

//TODO SAM 2012-07-27 Evaluate putting this in more generic code, perhaps IOUtil.PropList.readPersistent()?
/**
Read the property file for PropertyFileFormatType.NAME_VALUE format.
@param processor the command processor
@param commandPhase the command phase
@param inputFileFull full path to the property file being read
@param includeProperties the array of properties to read, or if empty read all
@param excludeProperties the array of properties to ignore, or if empty read all
@param ignoreCase whether to ignore case when including/excluding property names
@param expandProperties whether to expand properties that have ${property} notation in the value
@return the list of problems, guaranteed to be non-null
*/
private List<String> readPropertyFileNameValue ( CommandProcessor processor, CommandPhaseType commandPhase,
    String inputFileFull, String [] includeProperties, String [] excludeProperties, boolean ignoreCase, boolean expandProperties )
throws IOException {
	String routine = getClass().getSimpleName() + ".readPropertyFileNameValue";
    BufferedReader in = null;
    List<String> problems = new ArrayList<>();

    try {
        // Open the file.
        in = new BufferedReader ( new InputStreamReader(IOUtil.getInputStream ( inputFileFull )) );
        // Currently all formats use Name=Value syntax, with some variations (but no XML, JSON, etc.).
        String lineString, lineStringTrimmed;
        String propName, propValue;
        int pos1, pos2;
        Object objectValue; // Generic object type to set as property.
        while ( (lineString = in.readLine()) != null ) {
            lineStringTrimmed = lineString.trim();
            if ( (lineStringTrimmed.length() == 0) || (lineStringTrimmed.charAt(0) == '#') ) {
                continue;
            }
            pos1 = lineStringTrimmed.indexOf("=");
            if ( pos1 <= 1 ) {
                // Not a valid property assignment of form: Property = ValueExpression
                continue;
            }
            propName = lineStringTrimmed.substring(0,pos1).trim();
           	if ( !shouldIncludeProperty ( propName, includeProperties, excludeProperties, ignoreCase ) ) {
           		// Property is not to be included.
       	    	continue;
           	}
            propValue = lineStringTrimmed.substring(pos1 + 1).trim();
            // Remove surrounding quotes if present.
            if ( propValue.charAt(0) == '"') {
                propValue = propValue.substring(1);
            }
            if ( propValue.charAt(propValue.length() - 1) == '"') {
                propValue = propValue.substring(0,propValue.length() - 1);
            }
            if ( (commandPhase == CommandPhaseType.RUN) && expandProperties ) {
           		// Expand the property.
          		propValue = TSCommandProcessorUtil.expandParameterValue(processor, this, propValue);
          		//Message.printStatus(2,"","Property name \"" + propName + "\" has value (after expansion) \"" + propValue + "\".");
          	}
            //Message.printStatus(2,"","Property name \"" + propName + "\" has value \"" + propValue + "\".");
            // Default just treat as string.
            objectValue = propValue;
            // Handle special values.
            if ( propValue.equals("PARENT_FOLDER") ) {
            	File f = new File(inputFileFull);
            	propValue = f.getParentFile().getAbsolutePath();
            }
            else if ( propValue.equals("PARENT_PARENT_FOLDER") ) {
            	File f = new File(inputFileFull);
            	propValue = f.getParentFile().getParentFile().getAbsolutePath();
            }
            // Handle special types.
            if ( propValue.toUpperCase().startsWith("DATETIME") ) {
                // Parse out the argument inside the ().
                pos1 = propValue.indexOf("(");
                pos2 = propValue.indexOf(")");
                DateTime dt = null;
                if ( (pos1 >= 0) && (pos2 > 0) ) {
                    String dtString = propValue.substring(pos1 + 1, pos2);
                    //Message.printStatus(2,"","Date/time string is \"" + dtString + "\".");
                    if ( dtString.indexOf(",") > 0 ) {
                        // Assume Pythonic notation DateTime(YYYY,MM,DD,...).
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
                        // Assume DateTime("date/time string"), quotes are optional.
                        dtString = dtString.replace("\"", "");
                        //Message.printStatus(2,"","Date/time string is \"" + dtString + "\".");
                        dt = DateTime.parse(dtString);
                    }
                    objectValue = dt;
                }
            }
            // TODO SAM 2012-07-30 Need to add support for Integer() and Double() for read and write.
            else if ( TimeUtil.isDateTime(propValue) ) {
            	// Set the object in the processor.
                objectValue = DateTime.parse(propValue);
            }
           	// The order of the following is important because an integer also passes test for double.
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
@param includeProperties the array of properties to read, or if empty read all
@param excludeProperties the array of properties to ignore, or if empty read all
@param ignoreCase whether to ignore case when including/excluding property names
@param expandProperties whether to expand properties that have ${property} notation in the value
@return the list of problems, guaranteed to be non-null
*/
private List<String> readPropertyFileNameTypeValue ( CommandProcessor processor, CommandPhaseType commandPhase,
    String inputFileFull, String [] includeProperties, String [] excludeProperties, boolean ignoreCase, boolean expandProperties )
throws IOException {
    // Currently the same code.
    return readPropertyFileNameValue ( processor, commandPhase, inputFileFull, includeProperties, excludeProperties, ignoreCase, expandProperties );
}

/**
Read the property file for PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON format.
@param processor the command processor
@param commandPhase the command phase
@param inputFileFull full path to the property file being read
@param includeProperties the array of properties to read, or if empty read all
@param excludeProperties the array of properties to ignore, or if empty read all
@param ignoreCase whether to ignore case when including/excluding property names
@param expandProperties whether to expand properties that have ${property} notation in the value
@return the list of problems, guaranteed to be non-null
*/
private List<String> readPropertyFileNameTypeValuePython ( CommandProcessor processor, CommandPhaseType commandPhase,
    String inputFileFull, String [] includeProperties, String [] excludeProperties, boolean ignoreCase, boolean expandProperties )
throws IOException {
    // Currently the same code.
    return readPropertyFileNameValue ( processor, commandPhase, inputFileFull, includeProperties, excludeProperties, ignoreCase, expandProperties );
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@param command_phase The command phase that is being run (RUN or DISCOVERY).
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	CommandProcessor processor = getCommandProcessor();
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

	if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	    getDiscoveryProps().clear();
	}

	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue ( "InputFile" ); // Expanded below.
    String FileFormat = parameters.getValue ( "FileFormat" );
    PropertyFileFormatType fileFormat = PropertyFileFormatType.valueOfIgnoreCase(FileFormat);
    if ( fileFormat == null ) {
        fileFormat = PropertyFileFormatType.NAME_TYPE_VALUE; // Default.
    }
	String IncludeProperties = parameters.getValue ( "IncludeProperties" );
	if ( commandPhase == CommandPhaseType.RUN )  {
		IncludeProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, IncludeProperties);
	}
	// Wildcards are checked below.
	String [] includeProperties = new String[0];
	if ( (IncludeProperties != null) && !IncludeProperties.equals("") ) {
	    if ( IncludeProperties.indexOf(",") > 0 ) {
	        includeProperties = IncludeProperties.split(",");
	    }
	    else {
	        includeProperties = new String[1];
	        includeProperties[0] = IncludeProperties.trim();
	    }
	}
	// Wildcards are checked below.
	String ExcludeProperties = parameters.getValue ( "ExcludeProperties" );
	if ( commandPhase == CommandPhaseType.RUN )  {
		ExcludeProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, ExcludeProperties);
	}
	String [] excludeProperties = new String[0];
	if ( (ExcludeProperties != null) && !ExcludeProperties.equals("") ) {
	    if ( ExcludeProperties.indexOf(",") > 0 ) {
	        excludeProperties = ExcludeProperties.split(",");
	    }
	    else {
	        excludeProperties = new String[1];
	        excludeProperties[0] = ExcludeProperties.trim();
	    }
	}
    String IgnoreCase = parameters.getValue ( "IgnoreCase" );
	boolean ignoreCase = false; // Default for discovery mode.
	if ( commandPhase == CommandPhaseType.RUN )  {
		ignoreCase = false; // Default is to not ignore case.
		if ( (IgnoreCase != null) && IgnoreCase.equalsIgnoreCase(this._True) ) {
			ignoreCase = true;
		}
	}
    String ExpandProperties = parameters.getValue ( "ExpandProperties" );
	boolean expandProperties = false; // Default for discovery mode.
	if ( commandPhase == CommandPhaseType.RUN )  {
		expandProperties = true; // Default is to expand when running.
		if ( (ExpandProperties != null) && ExpandProperties.equalsIgnoreCase(this._False) ) {
			expandProperties = false;
		}
	}

    // Convert the include and exclude properties:
	// - use upper case so that string 'match' can be used with wildcard
	// - escape . with \. since period has meaning to "match"
	// - convert glob-style wildcard * to Java-style .*
    if ( includeProperties != null ) {
    	int i = -1;
    	for ( String includeProperty : includeProperties ) {
    		++i;
    		includeProperties[i] = includeProperty.replace(".","\\.").replace("*", ".*");
    		if ( ignoreCase ) {
    			includeProperties[i] = includeProperties[i].toUpperCase();
    		}

    	}
    }
    if ( excludeProperties != null ) {
    	int i = -1;
    	for ( String excludeProperty : excludeProperties ) {
    		++i;
    		excludeProperties[i] = excludeProperty.replace(".","\\.").replace("*", ".*");
    		if ( ignoreCase ) {
    			includeProperties[i] = includeProperties[i].toUpperCase();
    		}
    	}
    }

	// Now try to read.

	String InputFile_full = InputFile;
	try {
		if ( commandPhase == CommandPhaseType.RUN ) {
			// Convert to an absolute path.
			InputFile_full = IOUtil.verifyPathForOS(
	            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	                TSCommandProcessorUtil.expandParameterValue(processor, this, InputFile) ) );
			List<String> problems = new ArrayList<>();
			// Call the parsing method based on the file format.
			if ( fileFormat == PropertyFileFormatType.INI ) {
			    problems = readPropertyFileINI ( processor, commandPhase,
		    		InputFile_full, includeProperties, excludeProperties, ignoreCase, expandProperties );
			}
			else if ( fileFormat == PropertyFileFormatType.NAME_VALUE ) {
			    problems = readPropertyFileNameValue ( processor, commandPhase, InputFile_full,
			    	includeProperties, excludeProperties, ignoreCase, expandProperties );
			}
			else if ( fileFormat == PropertyFileFormatType.NAME_TYPE_VALUE ) {
	            problems = readPropertyFileNameTypeValue ( processor, commandPhase, InputFile_full,
	            	includeProperties, excludeProperties, ignoreCase, expandProperties );
	        }
			else if ( fileFormat == PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON ) {
	            problems = readPropertyFileNameTypeValuePython ( processor, commandPhase, InputFile_full,
	            	includeProperties, excludeProperties, ignoreCase, expandProperties );
	        }
			else if ( fileFormat == PropertyFileFormatType.VALUE ) {
            	String propName = includeProperties[0];
	            try {
	            	StringBuilder b = IOUtil.fileToStringBuilder(InputFile_full);
	            	Object objectValue = b.toString();
	            	if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	            		getDiscoveryProps().add ( new Prop(propName,objectValue,"" + objectValue ) );
	            	}
	            	else if ( commandPhase == CommandPhaseType.RUN ) {
	            		PropList requestParams = new PropList ( "" );
	            		requestParams.setUsingObject ( "PropertyName", propName );
	            		requestParams.setUsingObject ( "PropertyValue", objectValue );
	            		try {
	            			processor.processRequest( "SetProperty", requestParams);
	            			Message.printStatus(2,routine,"Setting property " + propName + "=" + objectValue );
	            		}
	            		catch ( Exception e ) {
	            			problems.add("Error requesting SetProperty(Property=\"" + propName + "\") from processor (" + e + ").");
	            		}
	            	}
	            }
	           	catch ( Exception e ) {
            		problems.add ( "Error reading property value from input file: " + InputFile_full );
	            }
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
 * Check whether a property should be included.
 * @param propName the property name to check
 * @param includeProperties the array of properties to read (or null/empty to read all)
 * @param excludeProperties the array of properties to ignore (or null/empty to read all)
 * @param ignoreCase whether to ignore case when including/excluding property names
 */
private boolean shouldIncludeProperty ( String propName, String [] includeProperties, String [] excludeProperties, boolean ignoreCase ) {
	String routine = getClass().getSimpleName() + ".shouldIncludeProperty";

	if ( ignoreCase) {
		// The arrays will already have been converted to upper case.
		propName = propName.toUpperCase();
	}
	// Default is to include all properties.
	boolean doInclude = true;
	if ( Message.isDebugOn ) {
		Message.printStatus(2, routine, "Checking whether \"" + propName + "\" should be included.");
	}
    if ( (includeProperties != null) && (includeProperties.length > 0) ) {
        // See if the requested property matches what is in the file.
    	// Assume not to include unless property name is matched.
        boolean found = false;
        for ( String includeProperty : includeProperties ) {
        	if ( Message.isDebugOn ) {
        		Message.printStatus(2, routine, "  Checking include properity pattern \"" + includeProperty + "\".");
        	}
            if ( propName.matches(includeProperty)) {
            	// Property name was matched:
            	// - include unless it is also in the exclude array
                found = true;
                break;
            }
        }
        if ( !found ) {
        	// Did not find in the include list so don't include.
        	doInclude = false;
        }
       	if ( Message.isDebugOn ) {
       		Message.printStatus(2, routine, "  After include checks, doInclude=" + doInclude);
       	}
    }

    // If include checks result in include=true, also check the exclude.
    if ( doInclude ) {
       	if ( (excludeProperties != null) && excludeProperties.length > 0 ) {
       		// Ignore if it is in the exclude list.
       		boolean found = false;
       		for ( String excludeProperty : excludeProperties ) {
       			if ( Message.isDebugOn ) {
       				Message.printStatus(2, routine, "  Checking exclude properity pattern \"" + excludeProperty + "\".");
       			}
       			if ( propName.matches(excludeProperty)) {
       				found = true;
       				break;
       			}
       		}
       		if ( found ) {
       			// Found in the exclude list so don't include as output.
       			doInclude = false;
       		}
       	}
		if ( Message.isDebugOn ) {
			Message.printStatus(2, routine, "  After exclude checks, doInclude=" + doInclude);
		}
	}

    // Return the overall result.
    return doInclude;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"InputFile",
		"FileFormat",
		"IncludeProperties",
		"ExcludeProperties",
		"IgnoreCase",
		"ExpandProperties"
	};
	return this.toString(parameters, parameterOrder);
}

}