// SetObjectProperty_Command - This class initializes, checks, and runs the SetObjectProperty() command.

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

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.JSON.JSONObject;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the SetObjectProperty() command.
*/
public class SetObjectProperty_Command extends AbstractCommand implements Command
{

	/**
	 * Values for AllowNull.
	 */
	protected final String _False = "False";
	protected final String _True = "True";

/**
Constructor.
*/
public SetObjectProperty_Command () {
   super();
    setCommandName ( "SetObjectProperty" );
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
    String ObjectID = parameters.getValue ( "ObjectID" );
    String ObjectProperty = parameters.getValue ( "ObjectProperty" );
    String Property = parameters.getValue ( "Property" );
    String SetAsString = parameters.getValue ( "SetAsString" );
    String AllowNull = parameters.getValue ( "AllowNull" );
    String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (ObjectID == null) || ObjectID.equals("") ) {
        message = "The object identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the identifier for the object to process." ) );
    }

    if ( (ObjectProperty == null) || ObjectProperty.equals("") ) {
        message = "The object property must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the object property to set." ) );
    }

    if ( (Property == null) || Property.equals("") ) {
        message = "The Property must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the Property parameter." ) );
    }

	if ( (SetAsString != null) && !SetAsString.equals("") ) {
		if ( !SetAsString.equalsIgnoreCase(_False) && !SetAsString.equalsIgnoreCase(_True) ) {
			message = "The SetAsString parameter \"" + SetAsString + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " (default) or " + _True + "."));
		}
	}

	if ( (AllowNull != null) && !AllowNull.equals("") ) {
		if ( !AllowNull.equalsIgnoreCase(_False) && !AllowNull.equalsIgnoreCase(_True) ) {
			message = "The AllowNull parameter \"" + AllowNull + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " (default) or " + _True + "."));
		}
	}

    // Check for invalid parameters.
    List<String> validList = new ArrayList<>(5);
    validList.add ( "ObjectID" );
    validList.add ( "ObjectProperty" );
    validList.add ( "Property" );
    validList.add ( "SetAsString" );
    validList.add ( "AllowNull" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

    if ( warning.length() > 0 ) {
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(command_tag,warning_level),
        warning );
        throw new InvalidCommandParameterException ( warning );
    }

    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{   List<String> objectIDChoices =
        TSCommandProcessorUtil.getObjectIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    return (new SetObjectProperty_JDialog ( parent, this, objectIDChoices)).ok();
}

/**
 * Find the parent object that matches the requested name.
 * This allows the parent's dictionary in calling code to set the object value.
 * This method is called recursively to dig down into the JSON object model.
 * @param map read from a JSON string, can be a top level map for the first call or an embedded map from recursive call
 * @param objectPropertyName the object property name to match (e.g., "a.b.c") - the second to last value
 * must be a list or dictionary to match as the parent of the requested property
 * @param pathSearched the JSON hierarchy that has been searched prior to the current call,
 * used to match the leading part of the matchMap key, pass as an empty string for the first call
 * (e.g., "" on first call and "a" on second and "a.b" on third)
 * @return the parent object, which will have its data set
 */
private Object findParentObject ( Map<?,?> map, String objectPropertyName, StringBuilder pathSearched ) {
	String routine = getClass().getSimpleName() + ".findObject";
	
	// Reused below to process maps.
	String name = null;
	Object value = null;

	// Number of levels processed so far in the search:
	// - will be zero the first call
	int levelsProcessed = StringUtil.patternCount(pathSearched.toString(), ".");
	Message.printStatus(2, routine, "Searching level=" + levelsProcessed + " pathSearched=\"" + pathSearched +
		"\" for property name =\"" + objectPropertyName + "\"");

	// Figure out which level of the JSON hierarchy is being processed in the call:
	// - top level is 0
	String matchPropertyPath = null;
	int parentPropertyLevel = -1;
	// Levels to process for a.b.c:
	// - would be 0.1.2
	// - since want to return the parent object at level 1 so subtract 1
	// - if root-level objects are requested, the result will be -1
	parentPropertyLevel = StringUtil.patternCount(objectPropertyName, ".") - 1;
	matchPropertyPath = objectPropertyName;
	if ( parentPropertyLevel < 0 ) {
		Message.printStatus(2, routine, "  parentPropertyLevel=" + parentPropertyLevel + " (root) matchPropertyPath=\"" + matchPropertyPath + "\"");
	}
	else {
		Message.printStatus(2, routine, "  parentPropertyLevel=" + parentPropertyLevel + " matchPropertyPath=\"" + matchPropertyPath + "\"");
	}

	// Get the corresponding object name to search for at the current level.
	String objectProperty = matchPropertyPath.split("\\.")[levelsProcessed].trim();
	Message.printStatus(2, routine, "  Searching for objectProperty=\"" + objectProperty + "\"" );

	String matchPattern = null; // Use if a wildcard is used to match property names.
	if ( objectProperty.indexOf("*") >= 0) {
		// Convert glob-style wildcard to Java regular expression.
		matchPattern = objectProperty.replace("*", ".*");
	}

	// Loop through the object properties at the current level.
	for (Map.Entry<?, ?> entry : map.entrySet() ) {
		name = (String)entry.getKey();
		value = entry.getValue();
	    Message.printStatus(2, routine, "  Checking name=\"" + name + "\" against \"" + objectProperty + "\"" );
		if ( name.equals(objectProperty) || ((matchPattern != null) && objectProperty.matches(matchPattern)) ) {
			// Found the desired name at the current level.
            Message.printStatus(2, routine, "  Found a matching object levelProcessed=" + levelsProcessed + " parentPropertyLevel=" + parentPropertyLevel );
			if ( parentPropertyLevel < 0 ) {
	            Message.printStatus(2, routine, "  Root level  matches object - returning the root map as the parent map." );
				return map;
			}
			else if ( levelsProcessed == parentPropertyLevel ) {
	            Message.printStatus(2, routine, "  At level to match object - returning the parent map." );
				return value;
			}
			else {
				// Need to process another deeper level so call recursively.
				// Append the current level to the path that has already been searched.
				pathSearched.append(objectProperty + ".");
				// Get the next property level name to look for.
				objectProperty = matchPropertyPath.split("\\.")[levelsProcessed + 1].trim();
	            Message.printStatus(2, routine, "  Not yet at level to match object." );
				if ( value instanceof Map ) {
		    		if ( Message.isDebugOn ) {
		    			Message.printStatus(2, routine, "  In map.  Recursively looking for object name \"" + objectProperty + "\" in map object.");
		    		}
					// The following will add to the "arrays" list.
					Object foundObject = findParentObject ( (Map<?,?>)value, objectPropertyName, pathSearched );
					if ( foundObject != null ) {
						return foundObject;
					}
					else {
						// May need to traverse more array elements to find a match.
					}
				}
				else if ( value instanceof List ) {
					// Have to dig a bit deeper:
					// - loop through list items
					// - maps and lists are the two collection types so don't need to navigate any other object types
					List<?> objectList = (List<?>) value;
					if ( Message.isDebugOn ) {
						Message.printStatus(2, routine, "  In list.  Searching list for map objects.");
					}
					for ( Object o : objectList ) {
						if ( o instanceof Map ) {
							if ( Message.isDebugOn ) {
								Message.printStatus(2, routine, "  Recursively looking for object name \"" + objectProperty + "\" in map object.");
							}
							Object foundObject = findParentObject ( (Map<?,?>)o, objectPropertyName, pathSearched );
							if ( foundObject != null ) {
								return foundObject;
							}
							else {
								// May need to traverse more array elements to find a match.
							}
						}
					}
				}
			}
		}
	}
	// Done searching and did not find above so return null.
	return null;
}

/**
 * Get the last part of a property name.
 * @param objectPropertyName object property name to set (e.g., "level1.level2.level3")
 * @return the last part of the property name (e.g., "level3")
 */
private String getPropertyNameLastPart ( String objectPropertyName ) {
	if ( objectPropertyName.indexOf(".") < 0 ) {
		// Only a single part.
		return objectPropertyName.trim();
	}
	else {
		String [] parts = objectPropertyName.split("\\.");
		return parts[parts.length - 1].trim();
	}
}

// parseCommand method is in the base class.

/**
Method to execute the command.
@param command_number Number of command in sequence.
@exception Exception if there is an error processing the command.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   String message, routine = getCommandName() + "_Command.runCommand";
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    //int log_level = 3;  // Level for non-use messages for log file.

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    PropList parameters = getCommandParameters();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;

    // Get the input parameters.

    String ObjectID = parameters.getValue ( "ObjectID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	// In discovery mode want lists of objects to include ${Property}.
    	ObjectID = TSCommandProcessorUtil.expandParameterValue(processor, this, ObjectID);
    }
    String ObjectProperty = parameters.getValue ( "ObjectProperty" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	// In discovery mode want lists of objects to include ${Property}.
    	ObjectProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, ObjectProperty);
    }
    String Property = parameters.getValue ( "Property" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	// In discovery mode want lists of objects to include ${Property}.
    	Property = TSCommandProcessorUtil.expandParameterValue(processor, this, Property);
    }
    String SetAsString = parameters.getValue ( "SetAsString" );
    boolean setAsString = false; // Default.
    if ( (SetAsString != null) && SetAsString.equalsIgnoreCase(_True)  ) {
    	setAsString = true;
    }
    String AllowNull = parameters.getValue ( "AllowNull" );
    boolean allowNull = false; // Default.
    if ( (AllowNull != null) && AllowNull.equalsIgnoreCase(_True)  ) {
    	allowNull = true;
    }

    // Get the object to process.

    JSONObject object = null;
    PropList request_params = null;
    CommandProcessorRequestResultsBean bean = null;
    if ( (ObjectID != null) && !ObjectID.equals("") ) {
        // Get the object to be updated.
        request_params = new PropList ( "" );
        request_params.set ( "ObjectID", ObjectID );
        try {
            bean = processor.processRequest( "GetObject", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetObject(ObjectID=\"" + ObjectID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_object = bean_PropList.getContents ( "Object" );
        if ( o_object == null ) {
            message = "Unable to find object to process using ObjectID=\"" + ObjectID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that an object exists with the requested ID." ) );
        }
        else {
            object = (JSONObject)o_object;
        }
    }

    if ( warning_count > 0 ) {
        // Input error.
        message = "Insufficient data to run command.";
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

    // Now process.

    try {
    	// Get the property value:
    	// - allow null value
    	Object propertyValue = processor.getPropContents(Property);
    	if ( (propertyValue == null) && !allowNull ) {
        	message = "Property \"" + Property + "\" has null value.  Not allowed to set null value.";
        	Message.printWarning ( warning_level,
           		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        	status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
           		message, "Confirm that the property name is correct and has a non-null value, or specify AllowNull=True." ) );
    	}
    	else {
    		// Make sure that the object is initialized with null values.
        	//initializeObject ( object.getObjectMap(),
        	//	matchMap.getLinkedHashMap(),
        	//	propertyMap.getLinkedHashMap(),
        	//	new StringBuilder() );

        	// Set the value.
    		if ( (propertyValue != null) && setAsString ) {
    			// Convert the property value to a string.
    			propertyValue = propertyValue.toString();
    		}
        	if ( !setObjectProperty ( object.getObjectMap(), ObjectProperty, propertyValue, false ) ) {
        		message = "Unable to set property, usually because the property could not be matched.";
        		Message.printWarning ( warning_level,
            		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            		message, "Confirm that the object property name with periods matches an existing property to the parent's (second from last) level." ) );
        	}
    	}
    }
    catch ( Exception e ) {
        message = "Unexpected error setting object property (" + e + ").";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check log file for details." ) );
        throw new CommandException ( message );
    }

    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
 * Set the object's property.
 * @param map the full object map
 * @param propertyName the property name to set in the object
 * @param propertyValue the property value to set in the object
 * @param initializing if true then initializing and only set the value if the property does not exist
 * @return true if able to set, false if not
 */
private boolean setObjectProperty ( Map<?,?> map,
	String objectPropertyName, Object propertyValue, boolean initializing ) {
	// Find the parent object in the full map.
	StringBuilder objectPath = new StringBuilder();
	Object parentObject = findParentObject ( map, objectPropertyName, objectPath );
	if ( parentObject == null ) {
		// Did not find the object:
		// - cannot set the data
		// - a command error will be generated in the calling code
		return false;
	}
	else {
		if ( propertyValue instanceof DateTime ) {
			// Handle DateTime specifically:
			// - clone because don't want interaction with an instance that may be changing, such as in an iterator.
			// - don't convert to string yet because may want to access the DateTime object during processing
			// - DateTime are converted to string when writing the object
			propertyValue = ((DateTime)propertyValue).clone();
		}

		// Found the object so set the property value in the object:
		// - will have been initialized to null or other value so non-matches will have some value
		// - this works with reset or new object
		// - get the last part of the name to check for a name at the right hierarchy level
		if ( objectPropertyName.indexOf(".") >= 0 ) {
			// Property name is full a.b.c path so extract the last part.
			objectPropertyName = getPropertyNameLastPart(objectPropertyName);
		}
		if ( parentObject instanceof Map ) {
			Map<String,Object> parentObjectMap = (Map<String,Object>)parentObject;
			// TODO smalers 2022-11-22 is there a way to do a deep clone on unknown object type?
			// Otherwise data may change if original objects are not immutable.
			parentObjectMap.put(objectPropertyName, propertyValue);
		}
		else if ( parentObject instanceof List ) {
			// Want to process all the objects in the list, each of which will be a simple property or a map.
			// Name in the list.
			String listObjectName;
			int listIndex = -1;
			String matchPattern = null; // Use if a wildcard is used to match property names.
			if ( objectPropertyName.indexOf("*") >= 0) {
				// Convert glob-style wildcard to Java regular expression.
				matchPattern = objectPropertyName.replace("*", ".*");
			}
			List<Object> parentObjectList = (List<Object>)parentObject;
			for ( Object listObject : parentObjectList ) {
				++listIndex;
				if (listObject instanceof Map ) {
					Map<String,Object> listObjectMap = (Map<String,Object>)listObject;
					// Array element is a map.  Search for the matching object.
					boolean found = false;
					for (Map.Entry<?, ?> entry : listObjectMap.entrySet() ) {
						String listObjectPropertyName = (String)entry.getKey();
						if ( listObjectPropertyName.equals(objectPropertyName) ||
							((matchPattern != null) && listObjectPropertyName.matches(matchPattern)) ) {
							found = true;
							if ( matchPattern != null ) {
								// Use the original property name.
								listObjectMap.put(listObjectPropertyName, propertyValue);
							}
							else {
								listObjectMap.put(objectPropertyName, propertyValue);
							}
						}
					}
					if ( !found ) {
						if ( propertyValue instanceof DateTime ) {
							// Handle DateTime specifically:
							// - clone because don't want interaction with an instance that may be changing, such as in an iterator.
							// - don't convert to string yet because may want to access the DateTime object during processing
							// - DateTime are converted to string when writing the object
							listObjectMap.put(objectPropertyName, ((DateTime)propertyValue).clone());
						}
						else {
							// Just set.  Will be OK for primitives that are immutable, such as String, Integer.
							listObjectMap.put(objectPropertyName, propertyValue);
						}
					}
				}
				else {
					// Simple primitive value to reset:
					// - since values are not named, reset all, which limits the utility
					// - TODO smalers 2022-11-23 need way to delete or add?
					parentObjectList.set(listIndex, propertyValue);
				}
			}
		}
		return true;
	}
}

/**
Return the string representation of the command.
@param parameters parameters for the command.
*/
public String toString ( PropList parameters ) {
    if ( parameters == null ) {
        return getCommandName() + "()";
    }

    String ObjectID = parameters.getValue( "ObjectID" );
    String ObjectProperty = parameters.getValue ( "ObjectProperty" );
    String Property = parameters.getValue ( "Property" );
    String SetAsString = parameters.getValue ( "SetAsString" );
    String AllowNull = parameters.getValue ( "AllowNull" );

    StringBuffer b = new StringBuffer ();

    if ( (ObjectID != null) && (ObjectID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ObjectID=\"" + ObjectID + "\"" );
    }
    if ( (ObjectProperty != null) && (ObjectProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ObjectProperty=\"" + ObjectProperty + "\"" );
    }
    if ( (Property != null) && (Property.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Property=\"" + Property + "\"" );
    }
    if ( (SetAsString != null) && (SetAsString.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SetAsString=" + SetAsString );
    }
    if ( (AllowNull != null) && (AllowNull.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AllowNull=" + AllowNull );
    }

    return getCommandName() + "(" + b.toString() + ")";
}

}