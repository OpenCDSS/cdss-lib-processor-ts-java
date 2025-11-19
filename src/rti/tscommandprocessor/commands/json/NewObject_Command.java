// NewObject_Command - This class initializes, checks, and runs the NewObject() command.

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

package rti.tscommandprocessor.commands.json;

import javax.swing.JFrame;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.JSON.JSONObject;

/**
This class initializes, checks, and runs the NewObject() command.
*/
public class NewObject_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
The JSON object that is created, which contains identifier and JSON complex object.
*/
private JSONObject jsonObject = null;

/**
Constructor.
*/
public NewObject_Command () {
	super();
	setCommandName ( "NewObject" );
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
	String routine = getClass().getSimpleName() + ".checkCommandParameters";
	String ObjectID = parameters.getValue ( "ObjectID" );
	String InputFile = parameters.getValue("InputFile");
    String JSONText = parameters.getValue ( "JSONText" );
	String warning = "";
    String message;

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (ObjectID == null) || ObjectID.isEmpty() ) {
        message = "The object identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the object identifier." ) );
    }

    if ( ((InputFile == null) || InputFile.isEmpty()) && ((JSONText == null) || JSONText.isEmpty()) ) {
        message = "The input file or JSON text must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify an input file or JSON text." ) );
    }
    if ( ((InputFile != null) && !InputFile.isEmpty()) && (JSONText != null) && !JSONText.isEmpty() ) {
        message = "The input file and JSON text cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify an input file or JSON text." ) );
    }

    if ( (InputFile != null) && !InputFile.isEmpty() && (InputFile.indexOf("${") < 0) ) {
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
                // Working directory is available so use it.
                if ( o != null ) {
                    working_dir = (String)o;
                }
            }
            catch ( Exception e ) {
                message = "Error requesting WorkingDir from processor.";
                warning += "\n" + message;
                Message.printWarning(3, routine, message );
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an existing input file." ) );
            }

        try {
            //String adjusted_path =
            IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        }
        catch ( Exception e ) {
            message = "The input file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that input file and working directory paths are compatible." ) );
        }
    }

	if ( (JSONText != null) && !JSONText.isEmpty() ) {
        // Check the JSON for validity.
		// TODO smalers 2022-10-11 fill out functionality.
	}

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(3);
    validList.add ( "ObjectID" );
    validList.add ( "InputFile" );
    validList.add ( "JSONText" );
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
	return (new NewObject_JDialog ( parent, this )).ok();
}

/**
Return the object that is read by this class when run in discovery mode.
@return the object that is read by this class when run in discovery mode
*/
private JSONObject getDiscoveryJSONObject() {
    return this.jsonObject;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of JSONObject objects.
Classes that can be requested:  JSONObject
@return a list of objects of the requested type
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    JSONObject object = getDiscoveryJSONObject();
    List<T> v = null;
    if ( (object != null) && (c == object.getClass()) ) {
        v = new ArrayList<>();
        v.add ( (T)object );
    }
    return v;
}

// TODO smalers 2022-10-14 see the original code in ReadTableFromJSON, maybe need to put in a JSON utility class.
/**
 * Get a list of JSON array (list) objects corresponding to the requested array name.
 * In some case, a single array will be found.
 * In other cases, an array is embedded in other arrays and multiple arrays are found.
 * The resulting array(s) can be processed into one merged table.
 * If the name is in a list of such objects, the first array (list) is returned.
 * This method is called recursively to dig down into the JSON object model.
 * @param map read from a JSON string, can be a top level map the first call or an embedded * map from recursive call
 * @param arrayName name of the array to find or empty to use the first array, such as a top-level array
 * @param appendArrays whether to append multiple matched arrays (false will return the first instance)
 * @param arrays list of arrays to return.  If null, a new array will be created.
 * If non-null, the list will be modified, such as by recursive calls.
 * @return the object for the requested array (a list), or null if not found
 */
private List<List<?>> getJsonArraysByName ( Map<?,?> map, String arrayName, boolean appendArrays, List<List<?>> arrays ) {
	String routine = getClass().getSimpleName() + ".getJsonArraysByName";
	if ( arrays == null ) {
		arrays = new ArrayList<>();
	}
	// Start by iterating through the top-level map.
	String name;
	Object value;
	for (Map.Entry<?, ?> entry : map.entrySet() ) {
		name = (String)entry.getKey();
		if ( Message.isDebugOn ) {
			Message.printStatus(2, routine, "Map entry has name \"" + name + "\".");
		}
		value = entry.getValue();
		// If the requested array name is empty, use the first array found.
		if ( name.equals(arrayName) || arrayName.isEmpty() ) {
			if ( value instanceof List ) {
				arrays.add((List<?>)value);
				if ( Message.isDebugOn ) {
					Message.printStatus(2, routine, "Found array name \"" + name + "\".");
				}
				if ( arrayName.isEmpty() ) {
					// Return because only want to match the top array.
					return arrays;
				}
			}
		}
	}
	// Recursively iterate deeper if any maps are available.
	for (Map.Entry<?, ?> entry : map.entrySet() ) {
		value = entry.getValue();
		if ( value instanceof Map ) {
		    if ( Message.isDebugOn ) {
		    	Message.printStatus(2, routine, "Recursively looking for array name \"" + arrayName + "\" in map object.");
		    }
			// The following will add to the "arrays" list.
			int sizeBefore = arrays.size();
			List<List<?>> arrays2 = getJsonArraysByName ( (Map<?,?>)value, arrayName, appendArrays, arrays );
			if ( sizeBefore != arrays2.size() ) {
				if ( Message.isDebugOn ) {
					Message.printStatus(2, routine, "Recursively found array name \"" + arrayName + "\".");
				}
				if ( !appendArrays ) {
					// Only want the first match.
					return arrays;
				}
			}
		}
		else if ( value instanceof List ) {
			// Have to dig a bit deeper:
			// - loop through list items
			// - maps and lists are the two collection types so don't need to navigate any other object types
			List<?> objectList = (List<?>) value;
			if ( Message.isDebugOn ) {
				Message.printStatus(2, routine, "Searching list for map objects.");
			}
			for ( Object o : objectList ) {
				if ( o instanceof Map ) {
					if ( Message.isDebugOn ) {
						Message.printStatus(2, routine, "Recursively looking for array name \"" + arrayName + "\" in map object.");
					}
					int sizeBefore = arrays.size();
					List<List<?>> arrays2 = getJsonArraysByName ( (Map<?,?>)o, arrayName, appendArrays, arrays );
					if ( sizeBefore != arrays2.size() ) {
						if ( Message.isDebugOn ) {
							Message.printStatus(2, routine, "Recursively found array name \"" + arrayName + "\".");
						}
						if ( !appendArrays ) {
							// Only want the first match.
							return arrays;
						}
					}
				}
			}
		}
	}
	// Always return the list.
	return arrays;
}

// Use base class parseCommand().

/**
 * Print a map, for troubleshooting.
 * @param obj object (map) to print
 * @param indent indent for each level
 */
private void printMap(Object obj, int indent) {
	String routine = getClass().getSimpleName() + ".printMap";
    String indentStr = new String(new char[indent]).replace("\0", "  "); // spaces
    
    boolean doPrefix = false;
    String mapPrefix = "";
    String listPrefix = "";
    String objectPrefix = "";
    if ( doPrefix ) {
    	mapPrefix = "MapItem: ";
    	listPrefix = "ListItem: ";
    	objectPrefix = "Object: ";
    }

    if (obj instanceof Map) {
        Map<?, ?> map = (Map<?, ?>) obj;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
        	Message.printStatus(2, routine, indentStr + mapPrefix + "\"" + entry.getKey() + "\" :" );
            //System.out.println(indentStr + entry.getKey() + ":");
            printMap(entry.getValue(), indent + 1);
        }
    } else if (obj instanceof List) {
        List<?> list = (List<?>) obj;
        for (int i = 0; i < list.size(); i++) {
            Message.printStatus(2,routine,indentStr + listPrefix + "[" + i + "]:");
            printMap(list.get(i), indent + 1);
        }
    } else {
        Message.printStatus(2,routine, indentStr + objectPrefix + String.valueOf(obj));
    }
}

// See the ReadTableFromJSON code, which was the original.
/**
Read a JSON or XML file into an object.
@param inputFile path to input file to read
@param object existing object to populate
*/
private int readFileUsingJackson ( String inputFile, JSONObject object,
	//String arrayName, boolean appendArrays,
	//String [] excludeNames,
	//String [] arrayColumns, String [] booleanColumns, String [] dateTimeColumns,
	//String [] doubleColumns, String [] integerColumns, String [] textColumns,
	//int top,
	CommandStatus status, int warningCount )
throws FileNotFoundException, IOException {
	String routine = getClass().getSimpleName() + ".readFileUsingJackson";

	boolean useMapper = true;
	boolean readCsv = false;
	boolean readJson = false;
	boolean readXml = false;
	boolean readYaml = false;
	String inputFileUpper = inputFile.toUpperCase();
	if ( inputFileUpper.toUpperCase().endsWith(".CSV") ) {
		// Reading an CSV file.
		readCsv = true;
	}
	else if ( inputFileUpper.endsWith(".XML") ) {
		// Reading an XML file.
		readXml = true;
	}
	else if ( inputFileUpper.endsWith(".YML") || inputFileUpper.endsWith("YAML") ) {
		// Reading an YAML file.
		readYaml = true;
	}
	else {
		// Assume JSON.
		readJson = true;
	}
	if ( useMapper ) {
		// Map the JSON string into an object hierarchy and then pull out what is needed:
		// - may fail on very large input files by running out of memory

		// First read the input file into a string.
		StringBuilder inputFileAsStringBuilder = IOUtil.fileToStringBuilder(inputFile);

		// Parse the string into object hierarchy:
		// - if the JSON string starts with '{', read into a map  // } so editor can match brackets
		// - if the JSON string starts with '[', read into an array and add to a map for further processing
		ObjectMapper mapper = null;
		if ( readCsv ) {
			boolean readMap = false;
			boolean readList = true;
	        CsvMapper csvMapper = new CsvMapper();
	        CsvSchema schema = CsvSchema
	        	.emptySchema()
	        	.withHeader();  // Tells Jackson first row is header.

	        if ( readMap ) {
	        	// Reading a map does not work well because only one object will be in the map
	        	Map<String, Object> csvMap = csvMapper
                	.readerFor(Map.class)
                	.with(schema)
                	.readValue(new File(inputFile));	

				object.setObjectMap(csvMap);
	        }
	        else if ( readList ) {
	        	// Reading a map does not work well because only one object will be in the map
	        	MappingIterator<Map<String, Object>> iterator = csvMapper
                	.readerFor(Map.class)
                	.with(schema)
                	.readValues(new File(inputFile));	
	        	
	        	List<Map<String,Object>>csvList = iterator.readAll();

				object.setObjectArray(csvList);
	        }
			//printMap ( map, 2 );
		}
		else if ( readJson ) {
			mapper = new ObjectMapper();
			Map<?,?> map = null;
			if ( inputFileAsStringBuilder.charAt(0) == '{' ) { // } so editor can match brackets
				map = mapper.readValue(inputFileAsStringBuilder.toString(), Map.class);
				object.setObjectMap ( map );
			}
			else if ( inputFileAsStringBuilder.charAt(0) == '[' ) {
				// Create a top-level map with black name:
				// - use a LinkedHashMap to preserve element order
				//map = new LinkedHashMap<>();
				boolean insertArrayName = false;
				if ( insertArrayName ) {
					if ( Message.isDebugOn ) {
						Message.printStatus(2, routine,
							"JSON top-level array detected.  Adding an object named 'toparray' object at top to facilitate parsing into a map.");
					}
					// The following matched brackets allow an editor to match.
					inputFileAsStringBuilder.insert(0, "{ \"toparray\" : ");
					inputFileAsStringBuilder.append("}");
					// Top level object will be a List of objects.
					map = mapper.readValue(inputFileAsStringBuilder.toString(), Map.class);
					object.setObjectMap ( map );
				}
				else {
					// Top level object will be a List of objects.
					List<?> list = mapper.readValue(inputFileAsStringBuilder.toString(), List.class);
					object.setObjectArray ( list );
				}
			}
			else {
				String message = "JSON does not start with { or [ - cannot parse.";  // } to allow editor to match bracket.
        		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            		message, "Verify that the JSON file content is valid JSON starting with { or [ ." ) ); // } to allow editor to match bracket.
        		Message.printWarning(3, routine, message);
        		// Cannot read any more data.
        		++warningCount;
        		return warningCount;
			}
			if ( Message.isDebugOn ) {
				Message.printStatus(2, routine, "Map from JSON has " + map.size() + " top-level entries.");
			}
		}
		else if ( readXml ) {
			ObjectMapper xmlMapper = new XmlMapper();
			
			//JsonNode root = mapper.readTree(inputFileAsStringBuilder.toString());
			// Because the XML likely has a root element, read as a map.
			//Map<?,?> map = xmlMapper.readValue(inputFileAsStringBuilder.toString(), Map.class);
			JsonNode root = xmlMapper.readTree(inputFileAsStringBuilder.toString());
			// Convert the JsonNode tree to a Map representation.
			//ObjectMapper jsonMapper = new ObjectMapper();
			// Convert JsonNode to a Map (preserves arrays/lists):
			// - does not work, still loses duplicates.
			//Map<String,Object> map = jsonMapper.convertValue(root, Map.class);
			Map<String,JsonNode> map = new HashMap<>();
			map.put("rootJsonNode", root);
			object.setObjectMap(map);
			//printMap ( map, 2 );
		}
		else if ( readYaml ) {
			ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
			
			@SuppressWarnings("unchecked")
			Map<String,Object> yamlMap = yamlMapper.readValue(new File(inputFile),Map.class);
			object.setObjectMap(yamlMap);
			//printMap ( map, 2 );
		}

		// Find the array of interest, which is actually a Java List:
		// - create the list up front
		/*
		List<List<?>> arrays = new ArrayList<>();
		getJsonArraysByName (map, arrayName, appendArrays, arrays );
		if ( arrays.size() == 0 ) {
			String message = "Unable to locate array named \"" + arrayName + "\" in the JSON.";
        	status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            	message, "Verify that the JSON file format includes the named array object." ) );
        	Message.printWarning(3, routine, message);
        	// Cannot read any more data.
        	++warningCount;
        	return warningCount;
		}
		*/

		List<String> problems = new ArrayList<>();

		// Log the problems.
		int maxProblems = 100;
		int iProblem = -1;
		for ( String problem : problems ) {
			++iProblem;
			if ( iProblem >= maxProblems ) {
				String message = "Error limited to " + maxProblems + " errors.";
				++warningCount;
        		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            		message, "Check the JSON file format." ) );
        		Message.printWarning(3, routine, message);
				break;
			}
			String message = "Error reading data [" + (iProblem + 1) + "]: " + problem;
        	status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            	message, "Check the JSON file format." ) );
        	Message.printWarning(3, routine, message);
        	++warningCount;
		}

		/*
				// Load the response into a dictionary.
				// Search for "novastarConfig".
				Object o = map.get("novastarConfig");
				if ( o == null ) {
					Message.printStatus(2, routine,  "Don't have 'novastarConfig' element.");
					return null;
				}
				// Search for novastar.properties.
				Message.printStatus(2, routine,  "Have 'novastarConfig' element.");
				Map map2 = (Map)o;
				Object o2 = map2.get("properties");
				if ( o2 == null ) {
					Message.printStatus(2, routine,  "Don't have 'novastarConfig.properties' element.");
					return null;
				}
				Message.printStatus(2, routine,  "Have 'novastarConfig.properties' element.");
				// Have a properties list.
				// Get the requested property value.
				List<Map> list = (List)o2;
				for ( Object item : list ) {
					Map map3 = (Map)item;
					Object name = map3.get("name");
					Object value = map3.get("value");
					Message.printStatus(2, routine,  "'novastarConfig.properties' name=\"" + name + "\", value=\"" + value + "\".");
					if ( (name != null) && name.equals(propertyName) ) {
						return (String)value;
					}
				}
				// No value available.
				Message.printStatus(2, routine,  "Did not match 'novastarConfig.properties' name=\"" + propertyName + "\".");
				return null;
		*/
	}
	else {
		// Other options.
	}

	return warningCount;
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommandInternal", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

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
		status.clearLog(commandPhase);
	}
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryJSONObject ( null );
    }

	// Make sure there are time series available to operate on.

	PropList parameters = getCommandParameters();

    String ObjectID = parameters.getValue ( "ObjectID" );
    if ( (ObjectID != null) && !ObjectID.isEmpty() && (commandPhase == CommandPhaseType.RUN) && ObjectID.indexOf("${") >= 0 ) {
   		ObjectID = TSCommandProcessorUtil.expandParameterValue(processor, this, ObjectID);
    }
	String InputFile = parameters.getValue("InputFile"); // Expanded below.
    String JSONText = parameters.getValue ( "JSONText" );
    if ( (JSONText != null) && !JSONText.isEmpty() && (commandPhase == CommandPhaseType.RUN) && JSONText.indexOf("${") >= 0 ) {
   		JSONText = TSCommandProcessorUtil.expandParameterValue(processor, this, JSONText);
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	try {
    	// Create the object.

	    JSONObject object = null;
	    String InputFile_full = InputFile;

        if ( commandPhase == CommandPhaseType.RUN ) {
        	if ( (JSONText != null) && !JSONText.isEmpty() ) {
        		// Create the object from the JSON text.
        		object = new JSONObject( ObjectID );
        		//object.setObjectID ( ObjectID );
        	}
        	else if ( commandPhase == CommandPhaseType.RUN ) {
        		// Create the object from JSON file.
        		InputFile_full = IOUtil.verifyPathForOS(
            		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                		TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        		boolean useJackson = true; // Currently the only option.
        		if ( useJackson ) {
   			  		// Use Jackson, preferred because it is used in more code elsewhere.
   			  		object = new JSONObject(ObjectID);
   			  		warning_count = readFileUsingJackson ( InputFile_full,
   			  			object,
   				  		status, warning_count);
   		  		}
        	}

            // Set the object in the processor.

            PropList request_params = new PropList ( "" );
            request_params.setUsingObject ( "Object", object );
            try {
                processor.processRequest( "SetObject", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting SetObject(Object=...) from processor.";
                Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
            }
        }
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create an empty object and set the ID.
            object = new JSONObject(ObjectID);
            setDiscoveryJSONObject ( object );
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error creating new object (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
		throw new CommandWarningException ( message );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		throw new CommandWarningException ( message );
	}

    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the object that is created by this class in discovery mode.
@param objec the object that is created in discovery mode
*/
private void setDiscoveryJSONObject ( JSONObject object ) {
    this.jsonObject = object;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"ObjectID",
		"InputFile",
		"JSONText"
	};
	return this.toString(parameters, parameterOrder);
}

}