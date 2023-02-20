// SetPropertyFromObject_Command - This class initializes, checks, and runs the SetPropertyFromObject() command.

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
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.JSON.JSONObject;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the SetPropertyFromObject() command.
*/
public class SetPropertyFromObject_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

	/**
	Possible value for PropertyType:
	Leave out 'Date' since Java-style date is not handled as cleanly as DateTime.
	*/
	protected final String _Boolean = "Boolean";
	protected final String _DateTime = "DateTime";
	protected final String _Double = "Double";
	protected final String _Integer = "Integer";
	protected final String _String = "String";

	/**
	 * Values for AllowNull.
	 */
	protected final String _False = "False";
	protected final String _True = "True";

	/**
	Property set during discovery.
	*/
	private Prop discoveryProp = null;

/**
Constructor.
*/
public SetPropertyFromObject_Command () {
   super();
    setCommandName ( "SetPropertyFromObject" );
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
    String PropertyName = parameters.getValue ( "PropertyName" );
    String PropertyType = parameters.getValue ( "PropertyType" );
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

    if ( (PropertyName == null) || PropertyName.equals("") ) {
        message = "The property name must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the PropertyName parameter." ) );
    }

    if ( (PropertyType != null) && !PropertyType.isEmpty() ) {
		if ( !PropertyType.equalsIgnoreCase(_Boolean) &&
			!PropertyType.equalsIgnoreCase(_DateTime) &&
			!PropertyType.equalsIgnoreCase(_Double) &&
			!PropertyType.equalsIgnoreCase(_Integer) &&
			!PropertyType.equalsIgnoreCase(_String) &&
			!PropertyType.equalsIgnoreCase(_String)
		) {
			message = "The property type (" + PropertyType + ") is invalid";
	    	warning += "\n" + message;
	    	status.addToLog ( CommandPhaseType.INITIALIZATION,
            	new CommandLogRecord(CommandStatusType.FAILURE,
                	message, "Provide a valid property type." ) );
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
    validList.add ( "PropertyName" );
    validList.add ( "PropertyType" );
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
    return (new SetPropertyFromObject_JDialog ( parent, this, objectIDChoices)).ok();
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
Return the property defined in discovery phase.
*/
private Prop getDiscoveryProp () {
    return this.discoveryProp;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  Prop
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{   Prop discovery_Prop = getDiscoveryProp ();
    if ( discovery_Prop == null ) {
        return null;
    }
    Prop prop = new Prop();
    // Check for TS request or class that matches the data.
    if ( c == prop.getClass() ) {
        List<T> v = new ArrayList<>(1);
        v.add ( (T)discovery_Prop );
        return v;
    }
    else {
        return null;
    }
}

/**
 * Get the object's property based on an object name.
 * @param map the full object map
 * @param propertyName the property name to set in the object
 * @param problems list of problems
 * @return the requested object or null if not found or null
 */
private Object getObjectProperty ( Map<?,?> map, String objectPropertyName, List<String> problems ) {
	// Find the parent object in the full map.
	StringBuilder objectPath = new StringBuilder();
	Object parentObject = findParentObject ( map, objectPropertyName, objectPath );
	Object object = null;
	if ( parentObject == null ) {
		// Did not find the object:
		// - this is a problem
		problems.add("The requested object property name \"" + objectPropertyName + "\".");
	}
	else {
		// Found the object so set the property value in the object:
		// - will have been initialized to null or other value so non-matches will have some value
		// - this works with reset or new object
		// - get the last part of the name to check for a name at the right hierarchy level
		if ( objectPropertyName.indexOf(".") >= 0 ) {
			// Property name is full a.b.c path so extract the last part.
			objectPropertyName = getPropertyNameLastPart(objectPropertyName);
		}
		if ( parentObject instanceof Map ) {
			// Can handle.
			Map<String,Object> parentObjectMap = (Map<String,Object>)parentObject;
			// Otherwise data may change if original objects are not immutable.
			object = parentObjectMap.get(objectPropertyName);
		}
		else if ( parentObject instanceof List ) {
			// Can't currently handle:
			// - TODO smalers 2022-12-05 could handle array if add command parmaeters to match an array object.
			object = null;
		}

		if ( object != null ) {
			// Post-process objects if necessary.
			if ( object instanceof DateTime ) {
				// Handle DateTime specifically:
				// - clone because don't want interaction with an instance that may be changing, such as in an iterator.
				// - don't convert to string yet because may want to access the DateTime object during processing
				// - DateTime are converted to string when writing the object
				object = ((DateTime)object).clone();
			}
		}

	}
	return object;
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
@param commandPhase The command phase that is being run (RUN or DISCOVERY).
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    int log_level = 3;  // Level for non-use messages for log file.

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    PropList parameters = getCommandParameters();

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
    String PropertyType = parameters.getValue ( "PropertyType" );
	boolean doPropertyType = false;
	if ( (PropertyType != null) && !PropertyType.isEmpty() ) {
		doPropertyType = true;
	}
    String PropertyName = parameters.getValue ( "PropertyName" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	// In discovery mode want lists of objects to include ${PropertyName}.
    	PropertyName = TSCommandProcessorUtil.expandParameterValue(processor, this, PropertyName);
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
    if ( commandPhase == CommandPhaseType.RUN ) {
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
    	// - allow null value if the command parameter indicates
    	if ( commandPhase == CommandPhaseType.RUN ) {

    		boolean canSet = true; // Can set the property.
    		List<String> problems = new ArrayList<>();
    		Object propertyObject = getObjectProperty( object.getObjectMap(), ObjectProperty, problems );
    		if ( problems.size() > 0 ) {
    			for ( String problem : problems ) {
    				Message.printWarning ( warning_level,
    					MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, problem );
    				status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
    					problem, "Check input." ) );
    			}
    			canSet = false;
    		}
    		else if ( (propertyObject == null) && !allowNull ) {
        		message = "Property \"" + PropertyName + "\" has null value.  Not allowed to set null value.";
        		Message.printWarning ( warning_level,
           			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
           			message, "Confirm that the property name is correct and has a non-null value, or specify AllowNull=True." ) );
    			canSet = false;
    		}
    		else if ( (propertyObject instanceof String) && ((String)propertyObject).isEmpty() &&
    			((PropertyType != null) && !PropertyType.isEmpty() && !PropertyType.equalsIgnoreCase(_String) ) && !allowNull ) {
    			// Input is an empty string and type other than string is requested so equivalent to null input.
        		message = "Property \"" + PropertyName + "\" is an empty string and converting to non-string.  Not allowed to set null value.";
        		Message.printWarning ( warning_level,
           			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
           			message, "Confirm that the property name is correct and has a value that is not an empty string, or specify AllowNull=True." ) );
    			canSet = false;
    		}
    		if ( canSet ) {
    			// If here can set the value whether null or not but it may be null and may need to be converted to a different type.
    			if ( propertyObject == null ) {
    				// Use the null value as is.
    			}
   				else if ( doPropertyType ) {
   					if ( propertyObject instanceof Boolean ) {
   						Boolean propertyBoolean = (Boolean)propertyObject;
    					if ( PropertyType.equalsIgnoreCase(_Boolean) ) {
    						// Nothing needs to be done.
    					}
    					else if ( PropertyType.equalsIgnoreCase(_String) ) {
    						// Convert boolean to string.
   							propertyObject = propertyBoolean.toString();
    					}
    					else if ( PropertyType.equalsIgnoreCase(_Integer) ) {
    						// Convert boolean to integer.
    						if ( propertyBoolean ) {
    							propertyObject = Integer.valueOf(1);
    						}
    						else {
    							propertyObject = Integer.valueOf(0);
    						}
    					}
    					else if ( PropertyType.equalsIgnoreCase(_Double) ) {
    						// Convert boolean to double.
    						if ( propertyBoolean ) {
    							propertyObject = Double.valueOf(1.0);
    						}
    						else {
    							propertyObject = Double.valueOf(0.0);
    						}
    					}
    					else {
    						message = "Property \"" + PropertyName + "\" - unhandled type conversion to " + PropertyType + ".";
    						Message.printWarning ( warning_level,
    							MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    						status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Can only convert boolean to " + _Double + ", " + _Integer + ", or " + _String +"." ) );
    					}
   					}
   					if ( propertyObject instanceof DateTime ) {
   						// Can only convert to string.
    					if ( PropertyType.equalsIgnoreCase(_DateTime) ) {
    						// Nothing needs to be done.
    					}
    					else if ( PropertyType.equalsIgnoreCase(_String) ) {
    						propertyObject = propertyObject.toString();
    					}
    					else {
    						message = "Property \"" + PropertyName + "\" - unhandled type conversion to " + PropertyType + ".";
    						Message.printWarning ( warning_level,
    							MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    						status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Can only convert date/time to " + _String +"." ) );
    					}
   					}
   					else if ( propertyObject instanceof Double ) {
   						Double propertyDouble = (Double)propertyObject;
    					if ( PropertyType.equalsIgnoreCase(_Double) ) {
    						// Nothing needs to be done.
    					}
    					else if ( PropertyType.equalsIgnoreCase(_String) ) {
    						// Convert double to string.
   							propertyObject = propertyDouble.toString();
    					}
    					else if ( PropertyType.equalsIgnoreCase(_Integer) ) {
    						// Convert double to integer.
    						propertyObject = Integer.valueOf(propertyDouble.intValue());
    					}
    					else if ( PropertyType.equalsIgnoreCase(_Boolean) ) {
    						// Convert double to boolean.
    						if ( propertyDouble == 0.0 ) {
    							propertyObject = Boolean.FALSE;
    						}
    						else {
    							propertyObject = Boolean.TRUE;
    						}
    					}
    					else {
    						message = "Property \"" + PropertyName + "\" - unhandled type conversion to " + PropertyType + ".";
    						Message.printWarning ( warning_level,
    							MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    						status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Can only convert double (float) to " + _Boolean + ", " + _Double + ", or " + _String +"." ) );
    					}
   					}
   					else if ( propertyObject instanceof Integer ) {
   						Integer propertyInteger = (Integer)propertyObject;
    					if ( PropertyType.equalsIgnoreCase(_Integer) ) {
    						// Nothing needs to be done.
    					}
    					else if ( PropertyType.equalsIgnoreCase(_String) ) {
    						// Convert integer to string.
   							propertyObject = propertyInteger.toString();
    					}
    					else if ( PropertyType.equalsIgnoreCase(_Double) ) {
    						// Convert integer to double.
    						propertyObject = Integer.valueOf(propertyInteger.intValue());
    					}
    					else if ( PropertyType.equalsIgnoreCase(_Boolean) ) {
    						// Convert integer to boolean.
    						if ( propertyInteger == 0 ) {
    							propertyObject = Boolean.FALSE;
    						}
    						else {
    							propertyObject = Boolean.TRUE;
    						}
    					}
    					else {
    						message = "Property \"" + PropertyName + "\" - unhandled type conversion to " + PropertyType + ".";
    						Message.printWarning ( warning_level,
    							MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    						status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Can only convert integer to " + _Boolean + ", " + _Double + ", or " + _String +"." ) );
    					}
   					}
   					else if ( propertyObject instanceof String ) {
   						String propertyString = (String)propertyObject;
    					if (propertyString.isEmpty() ) {
    						if ( doPropertyType && !PropertyType.equalsIgnoreCase(_String) ) {
    							// Converting an empty string to another type, null is OK.
    							propertyObject = null;
    						}
    					}
    					// Non-empty string.
    					if ( PropertyType.equalsIgnoreCase(_String) ) {
    						// No conversion is necessary.
    					}
    					else if ( PropertyType.equalsIgnoreCase(_Boolean) ) {
    						try {
    							propertyObject = Boolean.valueOf(propertyString);
    						}
    						catch ( Exception e ) {
    							message = "Property \"" + PropertyName + "\" - error converting to boolean from \"" + propertyString + "\".";
    							Message.printWarning ( warning_level,
    								MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    							status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
    								message, "Check that the property value is a valid boolean." ) );
    							canSet = false;
    						}
			    		}
    					else if ( PropertyType.equalsIgnoreCase(_DateTime) ) {
			        		// This handles special strings like CurrentToHour.
			        		// Have to specify a PropList to ensure the special syntax is handled.
    						// TODO SAM 2016-09-18 consider whether parsing should recognize in-memory DateTime properties.
    						try {
    							propertyObject = DateTime.parse(propertyString,(PropList)null);
    						}
    						catch ( Exception e ) {
    							message = "Property \"" + PropertyName + "\" - error converting to date/time from \"" + propertyString + "\".";
    							Message.printWarning ( warning_level,
    								MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    							status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
    								message, "Check that the property value is a valid date/time." ) );
    							canSet = false;
    						}
			    		}
    					else if ( PropertyType.equalsIgnoreCase(_Double) ) {
			    			try {
			    				propertyObject = new Double(propertyString);
			    			}
    						catch ( Exception e ) {
    							message = "Property \"" + PropertyName + "\" - error converting to double from \"" + propertyString + "\".";
    							Message.printWarning ( warning_level,
    								MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    							status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
    								message, "Check that the property value is a valid double." ) );
    							canSet = false;
    						}
			    		}
    					else if ( PropertyType.equalsIgnoreCase(_Integer) ) {
    						try {
    							propertyObject = Integer.valueOf(propertyString);
    						}
    						catch ( Exception e ) {
    							message = "Property \"" + PropertyName + "\" - error converting to integer from \"" + propertyString + "\".";
    							Message.printWarning ( warning_level,
    								MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    							status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
    								message, "Check that the property value is a valid integer." ) );
    							canSet = false;
    						}
    					}
    					else {
    						message = "Property \"" + PropertyName + "\" - unhandled type conversion to " + PropertyType + ".";
    						Message.printWarning ( warning_level,
    							MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    						status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
    							message, "Software needs to be updated.  Contact software support." ) );
    						canSet = false;
    					}
			    	}
    			}
    			if ( canSet ) {
    				// If here have a property value to set in the processor.
		   			request_params = new PropList ( "" );
		   			request_params.setUsingObject ( "PropertyName", PropertyName );
		   			request_params.setUsingObject ( "PropertyValue", propertyObject );
		   			try {
		        		processor.processRequest( "SetProperty", request_params);
		   			}
		   			catch ( Exception e ) {
		   				message = "Error requesting SetProperty(Property=\"" + PropertyName + "\") from processor (" + e + ").";
		   				Message.printWarning(log_level,
		   					MessageUtil.formatMessageTag( command_tag, ++warning_count),
		   					routine, message );
		           		Message.printWarning(log_level,routine,e);
		           		status.addToLog ( CommandPhaseType.RUN,
		                	new CommandLogRecord(CommandStatusType.FAILURE,
		                   		message, "Report the problem to software support." ) );
		   			}
    			}
    		}
    	}
    	else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	    	// Set the property type according to the request so that code that asks for discovery.
	    	// objects and then filters on type will have the correct type.
    		Object propertyObject = null;
	    	if ( (PropertyType == null) || PropertyType.isEmpty() ) {
	    		propertyObject = "";
	    	}
	    	else if ( PropertyType.equalsIgnoreCase(_Boolean) ) {
	    		propertyObject = new Boolean(true);
	    	}
	    	else if ( PropertyType.equalsIgnoreCase(_DateTime) ) {
	    		propertyObject = new DateTime(DateTime.DATE_CURRENT);
	    	}
	    	else if ( PropertyType.equalsIgnoreCase(_Double) ) {
	    		propertyObject = new Double(1.0);
	    	}
	    	else if ( PropertyType.equalsIgnoreCase(_Integer) ) {
	    		propertyObject = new Integer(1);
	    	}
	    	else {
	    		propertyObject = "";
	    	}
	    	Prop prop = new Prop(PropertyName, propertyObject, propertyObject.toString());
	        prop.setHowSet(Prop.SET_UNKNOWN);
	    	setDiscoveryProp ( prop );
    	}
    }
    catch ( Exception e ) {
        message = "Unexpected error setting property from object (" + e + ").";
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
Set the property defined in discovery phase.
@param prop Property set during discovery phase.
*/
private void setDiscoveryProp ( Prop prop ) {
    this.discoveryProp = prop;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"ObjectID",
    	"ObjectProperty",
    	"PropertyName",
    	"PropertyType",
    	"AllowNull"
	};
	return this.toString(parameters, parameterOrder);
}

}