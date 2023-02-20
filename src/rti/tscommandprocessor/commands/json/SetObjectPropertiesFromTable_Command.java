// SetObjectPropertiesFromTable_Command - This class initializes, checks, and runs the SetObjectPropertiesFromTable() command.

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
import RTi.Util.String.StringDictionary;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the SetObjectPropertiesFromTable() command.
*/
public class SetObjectPropertiesFromTable_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public SetObjectPropertiesFromTable_Command ()
{   super();
    setCommandName ( "SetObjectPropertiesFromTable" );
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
    String TableID = parameters.getValue ( "TableID" );
    String MatchMap = parameters.getValue ( "MatchMap" );
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

    if ( (TableID == null) || TableID.equals("") ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the identifier for the table to process." ) );
    }

    if ( (MatchMap == null) || MatchMap.equals("") ) {
        message = "The MatchMap is required to match table and object data.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the MatchMap parameter." ) );
    }

    // Check for invalid parameters.
    List<String> validList = new ArrayList<>(5);
    validList.add ( "ObjectID" );
    validList.add ( "TableID" );
    validList.add ( "IncludeColumns" );
    validList.add ( "MatchMap" );
    validList.add ( "PropertyMap" );
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
    List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    return (new SetObjectPropertiesFromTable_JDialog ( parent, this, objectIDChoices, tableIDChoices )).ok();
}

/**
 * Find the object that matches the requested name.
 * This method is called recursively to dig down into the JSON object model.
 * @param map read from a JSON string, can be a top level map the first call or an embedded
 * map from recursive call
 * @param matchMap the commands map of properties to match (e.g., "a.b.c") - the second to last value
 * must be a list or dictionary to match
 * @param pathSearched the JSON hierarchy that has been searched, used to match
 * the leading part of the matchMap key, pass as an empty string for the first call
 * @return the parent object, which will have its data set
 */
private Object findObject ( Map<?,?> map, Map<String,String> matchMap, Object [] matchValues, StringBuilder pathSearched ) {
	String routine = getClass().getSimpleName() + ".findObject";
	
	// Put together a string of values to match for the log message below.
	StringBuilder matchValuesString = new StringBuilder();
	for ( int i = 0; i < matchValues.length; i++ ) {
		if ( i != 0 ) {
			matchValuesString.append(",");
		}
		matchValuesString.append(matchValues[i]);
	}

	// Number of levels processed so far in the search.
	int levelsProcessed = StringUtil.patternCount(pathSearched.toString(), ".");
	Message.printStatus(2, routine, "Searching level=" + levelsProcessed + " pathSearched=\"" + pathSearched +
		"\" for matchValues=\"" + matchValuesString + "\".");

	// Reused below to process maps.
	String name = null;
	Object value = null;

	// Figure out which level of the JSON hierarchy is being processed in the call:
	// - top level is 0
	// - get the first 'matchMap' entry object property since all properties should be at the same hierarchy level
	String matchPropertyPath = null;
	int propertyLevel = -1;
	for (Map.Entry<?, ?> entry : matchMap.entrySet() ) {
		value = entry.getValue();
		// Levels to process for a.b.c would be 0.1.2 so 1 since want the list at 1.
		propertyLevel = StringUtil.patternCount((String)value, ".") - 1;
		matchPropertyPath = (String)value;
		// Break since only need to process one matchMap entry.
		break;
	}
	Message.printStatus(2, routine, "  levelsToProcess=" + propertyLevel + " matchPropertyPath=\"" + matchPropertyPath );

	// Get the corresponding object name to search for at the current level.
	String objectProperty = matchPropertyPath.split("\\.")[levelsProcessed].trim();
	Message.printStatus(2, routine, "  Searching for objecProperty=\"" + objectProperty + "\"" );

	// Loop through the object properties at the current level.
	for (Map.Entry<?, ?> entry : map.entrySet() ) {
		name = (String)entry.getKey();
		value = entry.getValue();
	    Message.printStatus(2, routine, "  Checking name=\"" + name + "\" against \"" + objectProperty + "\"" );
		if ( name.equals(objectProperty) ) {
			// Found the desired name at the current level.
			if ( levelsProcessed == propertyLevel ) {
	            Message.printStatus(2, routine, "  At level to match object - searching for match." );
				// Found the correct parent level so try to match the specific object.
				if ( value instanceof Map ) {
					// Try to match the requested values.
					Map<?,?> objectMap = (Map<?,?>)value;
					int matchCount = 0;
					int iMatch = -1;
					for (Map.Entry<?, ?> entry2 : matchMap.entrySet() ) {
						// Get the last period-delimited string, which is the object name to match.
						++iMatch;
						String objectName = StringUtil.getToken(
							(String)entry2.getValue(),".",0,(StringUtil.patternCount((String)entry2.getValue(), ".")));
	                    Message.printStatus(2, routine, "    Checking object for property \"" + objectName + "\"." );
						Object objectValue = objectMap.get(objectName);
	                    Message.printStatus(2, routine, "      Comparing object value=" + objectValue +
	                    	" against search value=" + matchValues[iMatch] );
						if ( objectValue == null ) {
							if ( matchValues[iMatch] == null ) {
								++matchCount;
							}
						}
						else if ( matchValues[iMatch] == null ) {
							// Won't be a match.
						}
						else if ( objectValue.toString().equals(matchValues[iMatch].toString())) {
							++matchCount;
						}
					}
					if ( matchCount == matchMap.size() ) {
						// Found a matching object in which the table values can be set:
						// - return the map object
		    			Message.printStatus(2, routine, "        Found a match.  Returning the object.");
						return value;
					}
				}
				// If here did not find a matching object:
				// - if called recursively need to check for null and keep searching
				return null;
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
					Object foundObject = findObject ( (Map<?,?>)value, matchMap, matchValues, pathSearched );
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
							Object foundObject = findObject ( (Map<?,?>)o, matchMap, matchValues, pathSearched );
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
	// Done searching and did not find above so return null;
	return null;
}

/**
 * Initialize objects with data to set, to ensure that all have at least null value for consistency.
 * This method is called recursively to dig down into the JSON object model.
 * @param map read from a JSON string, can be a top level map the first call or an embedded
 * map from recursive call
 * @param matchMap the commands map of properties to match (e.g., "a.b.c") - the second to last value
 * must be a list or dictionary to match
 * @param pathSearched the JSON hierarchy that has been searched, used to match
 * the leading part of the matchMap key, pass as an empty string for the first call
 * @return the parent object, which will have its data set
 */
private Object initializeObject ( Map<?,?> map, Map<String,String> matchMap,
	Map<String,String> propertyMap, StringBuilder pathSearched ) {
	String routine = getClass().getSimpleName() + ".initializeObject";
	
	// Number of levels processed so far in the search.
	int levelsProcessed = StringUtil.patternCount(pathSearched.toString(), ".");
	Message.printStatus(2, routine, "Searching level=" + levelsProcessed + " pathSearched=\"" + pathSearched );

	// Reused below to process maps.
	String name = null;
	Object value = null;

	// Figure out which level of the JSON hierarchy is being processed in the call:
	// - top level is 0
	// - get the first 'matchMap' entry object property since all properties should be at the same hierarchy level
	String matchPropertyPath = null;
	int propertyLevel = -1;
	for (Map.Entry<?, ?> entry : matchMap.entrySet() ) {
		value = entry.getValue();
		// Levels to process for a.b.c would be 0.1.2 so 1 since want the list at 1.
		propertyLevel = StringUtil.patternCount((String)value, ".") - 1;
		matchPropertyPath = (String)value;
		// Break since only need to process one matchMap entry.
		break;
	}
	Message.printStatus(2, routine, "  levelsToProcess=" + propertyLevel + " matchPropertyPath=\"" + matchPropertyPath );

	// Get the corresponding object name to search for at the current level.
	String objectProperty = matchPropertyPath.split("\\.")[levelsProcessed].trim();
	Message.printStatus(2, routine, "  Searching for objecProperty=\"" + objectProperty + "\"" );

	// Loop through the object properties at the current level.
	for (Map.Entry<?, ?> entry : map.entrySet() ) {
		name = (String)entry.getKey();
		value = entry.getValue();
	    Message.printStatus(2, routine, "  Checking name=\"" + name + "\" against \"" + objectProperty + "\"" );
		if ( name.equals(objectProperty) ) {
			// Found the desired name at the current level.
			if ( levelsProcessed == propertyLevel ) {
	            Message.printStatus(2, routine, "  At level to match object - initializing." );
				// Found the correct parent level so initialize the value if not set.
				if ( value instanceof Map ) {
					// Try to match the requested values.
					Map<String,?> objectMap = (Map<String,?>)value;
					for (Map.Entry<?, ?> entry2 : propertyMap.entrySet() ) {
						String objectName = StringUtil.getToken(
							(String)entry2.getValue(),".",0,(StringUtil.patternCount((String)entry2.getValue(), ".")));
						Object propertyValue = objectMap.get(objectName);
						if ( propertyValue == null ) {
							// Set it to null again just to make sure the property exists.
							objectMap.put(objectName, null);
						}
					}
				}
				// If called recursively need to process all objects so keep searching.
				return null;
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
					Object foundObject = initializeObject ( (Map<?,?>)value, matchMap, propertyMap, pathSearched );
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
							Object foundObject = initializeObject ( (Map<?,?>)o, matchMap, propertyMap, pathSearched );
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
	// Done searching and did not find above so return null;
	return null;
}

// Parse command is in the base class

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
    	// In discovery mode want lists of tables to include ${Property}.
    	ObjectID = TSCommandProcessorUtil.expandParameterValue(processor, this, ObjectID);
    }
    String TableID = parameters.getValue ( "TableID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	// In discovery mode want lists of tables to include ${Property}.
    	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String IncludeColumns = parameters.getValue ( "IncludeColumns" );
    String [] includeColumns = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
    	if ( (IncludeColumns != null) && !IncludeColumns.equals("") ) {
    		// Have specific columns to include.
    		includeColumns = IncludeColumns.split(",");
    		for ( int i = 0; i < includeColumns.length; i++ ) {
    			includeColumns[i] = includeColumns[i].trim();
    		}
    	}
    	else {
    		// Include all the columns - create array below.
    	}
    }
    String MatchMap = parameters.getValue ( "MatchMap" );
    StringDictionary matchMap = null;
    if ( (MatchMap != null) && (MatchMap.length() > 0) ) {
    	matchMap = new StringDictionary(MatchMap, ":", ",");
    }
    String PropertyMap = parameters.getValue ( "PropertyMap" );
    StringDictionary propertyMap = null;
    if ( (PropertyMap != null) && (PropertyMap.length() > 0) ) {
    	propertyMap = new StringDictionary(PropertyMap, ":", ",");
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

    // Get the table to process.

    DataTable table = null;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be updated
        request_params = new PropList ( "" );
        request_params.set ( "TableID", TableID );
        try {
            bean = processor.processRequest( "GetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table == null ) {
            message = "Unable to find table to process using TableID=\"" + TableID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a table exists with the requested ID." ) );
        }
        else {
            table = (DataTable)o_Table;
            // Check the columns to include.
            includeColumns = new String[table.getNumberOfFields()];
            for ( int i = 0; i < table.getNumberOfFields(); i++ ) {
            	includeColumns[i] = table.getFieldName(i);
            }
        }
    }

    // Get the column numbers from the table to be used as input.

    int [] includeColumnNums = new int[includeColumns.length];
    for ( int i = 0; i < includeColumns.length; i++ ) {
        try {
            includeColumnNums[i] = table.getFieldIndex(includeColumns[i]);
        }
        catch ( Exception e2 ) {
            message = "Table \"" + TableID + "\" does not have column \"" + includeColumns[i] + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a table exists with the requested column." ) );
        }
    }

    // Get the match column numbers from the table to be used as input.
    int [] matchColumnNums = new int[matchMap.size()];
    int iMatch = -1;
	for (Map.Entry<?, ?> entry : matchMap.getLinkedHashMap().entrySet() ) {
		++iMatch;
		String key = (String)entry.getKey();
        try {
            matchColumnNums[iMatch] = table.getFieldIndex(key);
        }
        catch ( Exception e2 ) {
            message = "Table \"" + TableID + "\" does not have column \"" + key + "\" for MatchMap command parameter.";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the requested column exists in the table." ) );
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
		// Find the JSON array of interest, which is actually a Java List:
		// - create the list up front
    	/*
		Map<?,?> map = object.getObjectMap();
		StringBuilder pathSearched = new StringBuilder ();
		List<?> array = findJsonArray(map, matchMap, pathSearched );
		boolean canRead = true;
		if ( arrays.size() == 0 ) {
			message = "Unable to locate array named \"" + arrayName + "\" in the JSON object.";
        	status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            	message, "Verify that the JSON object includes the named array object." ) );
        	Message.printWarning(3, routine, message);
        	// Cannot read any more data.
        	canRead = false;
		}
		*/

    	// Make sure that the object is initialized with null values.
        initializeObject ( object.getObjectMap(),
        	matchMap.getLinkedHashMap(),
        	propertyMap.getLinkedHashMap(),
        	new StringBuilder() );

		// Loop through the table records.
		Object tableObject;
		// For now do not skip nulls in input values.
		boolean skipNulls = false;
		Object [] matchValues = new Object[matchMap.size()];
		for ( int irec = 0; irec < table.getNumberOfRecords(); irec++ ) {
			// Loop through the columns in the table to process.
            for ( int icolumn = 0; icolumn < includeColumns.length; icolumn++ ) {
                try {
                    TableRecord rec = table.getRecord ( irec );
                    // Get the value from the table.
                    tableObject = rec.getFieldValue(includeColumnNums[icolumn]);
                    // Allow the value to be null.
                    if ( (tableObject == null) && skipNulls ) {
                        // Blank cell values are allowed - just don't set the property
                        message = "Table \"" + TableID + "\" value in column \"" + includeColumns[icolumn] +
                        "\" is null - skipping.";
                        Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            routine, message );
                        // Don't add to command log because warnings will result.
                        //status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
                        //    "Verify that the proper table input column is specified and that column values are numbers." ) );
                        // Go to next row.
                        continue;
                    }
                    else {
                    	// Set the value:
                    	// - change the object name if requested
                    	String propertyName = propertyMap.get(includeColumns[icolumn]);
                  		if ( propertyName == null ) {
                  			// Just use the column name.
                   			propertyName = includeColumns[icolumn];
                    	}
                  		// Set the values to use to match the record.
                  		for ( iMatch = 0; iMatch < matchMap.size(); iMatch++ ) {
                  			matchValues[iMatch] = rec.getFieldValue(matchColumnNums[iMatch]);
                  		}
                    	setObjectProperty ( object.getObjectMap(),
                    		matchMap.getLinkedHashMap(), matchValues,
                    		propertyName, tableObject, false );
                    }
                }
                catch ( Exception e ) {
                    message = "Unexpected error processing table record " + (irec + 1) + " (" + e + ").";
                    Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                    Message.printWarning(3,routine,e);
                    status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "See the log file for details - report the problem to software support." ) );
                }
            }
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error setting object properies from table (" + e + ").";
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
 * Set the object's property based on an object from the table.
 * @param map the full object map
 * @param matchMap the map of table to object property names to match records
 * @param matchValues the table column values used to match an object
 * @param propertyName the property name to set in the object
 * @param propertyValue the property value to set in the object
 * @param initializing if true then initializing and only set the value if the property does not exist
 */
private void setObjectProperty ( Map<?,?> map,
	Map<String,String> matchMap, Object [] matchValues,
	String propertyName, Object propertyValue, boolean initializing ) {
	// Find the parent object in the full map.
	StringBuilder objectPath = new StringBuilder();
	Object object = findObject ( map, matchMap, matchValues, objectPath );
	if ( object == null ) {
		// Did not find the object:
		// - cannot set the data
		// - rely on the initial value
	}
	else {
		// Found the object so set the data:
		// - will have been initialized to null or other value so non-matches will have some value
		if ( object instanceof Map ) {
			Map<String,Object> objectMap = (Map<String,Object>)object;
			if ( propertyName.indexOf(".") >= 0 ) {
				// Property name is full a.b.c path so extract the last part.
				propertyName = StringUtil.getToken(
					propertyName,".",0,(StringUtil.patternCount(propertyName, ".")));
			}
			objectMap.put(propertyName, propertyValue);
		}
	}
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"ObjectID",
    	"TableID",
    	"IncludeColumns",
    	"MatchMap",
    	"PropertyMap"
	};
	return this.toString(parameters, parameterOrder);
}

}