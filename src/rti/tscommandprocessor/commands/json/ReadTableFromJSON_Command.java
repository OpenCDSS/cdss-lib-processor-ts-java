// ReadTableFromJSON_Command - This class initializes, checks, and runs the ReadTableFromJSON() command.

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

package rti.tscommandprocessor.commands.json;

import javax.swing.JFrame;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.IOUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ReadTableFromJSON() command.
*/
public class ReadTableFromJSON_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Data members used for AppendArrays parameter values.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
 * Default precision for floating point numbers.
 */
protected final int defaultPrecision = 6;
    
/**
The table that is read.
*/
private DataTable __table = null;

/**
Constructor.
*/
public ReadTableFromJSON_Command ()
{	super();
	setCommandName ( "ReadTableFromJSON" );
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
{	String TableID = parameters.getValue ( "TableID" );
    String InputFile = parameters.getValue ( "InputFile" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// If the input file does not exist, warn the user...

	//String working_dir = null;
	
	CommandProcessor processor = getCommandProcessor();
	
    if ( (TableID == null) || TableID.isEmpty() ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the table identifier." ) );
    }
	try {
	    Object o = processor.getPropContents ( "WorkingDir" );
		// Working directory is available so use it...
		if ( o != null ) {
			//working_dir = (String)o;
		}
	}
	catch ( Exception e ) {
        message = "Error requesting WorkingDir from processor.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	
	if ( (InputFile == null) || InputFile.isEmpty() ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
	}
	else if ( InputFile.indexOf("${") < 0 ) {
		// Can only check if no property in path
		/* Allow since might be dynamically created - TODO SAM 2015-12-06 Add warning in run code
        try {
            String adjusted_path = IOUtil.verifyPathForOS (IOUtil.adjustPath ( working_dir, InputFile) );
			File f = new File ( adjusted_path );
			if ( !f.exists() ) {
                message = "The input file does not exist:  \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the input file exists - may be OK if created at run time." ) );
			}
			f = null;
		}
		catch ( Exception e ) {
            message = "The input file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
		}
		*/
	}
    
	//  Check for invalid parameters...
	List<String> validList = new ArrayList<>(12);
    validList.add ( "TableID" );
    validList.add ( "InputFile" );
    validList.add ( "ArrayName" );
    validList.add ( "AppendArrays" );
    validList.add ( "ExcludeNames" );
    validList.add ( "ArrayColumns" );
    validList.add ( "BooleanColumns" );
    validList.add ( "DateTimeColumns" );
    validList.add ( "DoubleColumns" );
    validList.add ( "IntegerColumns" );
    validList.add ( "TextColumns" );
    validList.add ( "Top" );
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
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new ReadTableFromJSON_JDialog ( parent, this )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __table;
}

/**
 * Get a list of JSON array (list) objects corresponding to the requested array name.
 * In some case, a single array will be found.
 * In other cases, an array is embedded in other arrays and multiple arrays are found.
 * The resulting array(s) can be processed into one merged table.
 * If the name is in a list of such objects, the first array (list) is returned.
 * This method is called recursively to dig down into the JSON object model.
 * @param map read from a JSON string, can be a top level map the first call or an embedded
 * map from recursive call
 * @param arrayName name of the array to find or empty to use the first array,
 * such as a top-level array
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
			// - loop through list items.
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

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
The following classes can be requested:  DataTable

*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{   DataTable table = getDiscoveryTable();
    List<T> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector<T>();
        v.add ( (T)table );
    }
    return v;
}

/**
 * Initialize the table columns based on JSON data.
 * @param table data table being read
 * @param objectList list of object lists to process, from JSON map
 * @excludeNames the object names to exclude
 * @arrayColumns column names for primitive array data (otherwise arrays are ignored)
 * @booleanColumns column names for boolean data (overrides auto-determined type)
 * @dateTimeColumns column names for date/time data (overrides auto-determined type)
 * @doubleColumns column names for double (float) data (overrides auto-determined type)
 * @integerColumns column names for integer data (overrides auto-determined type)
 * @textColumns column names for text data (overrides auto-determined type)
 */
private void initializeTableColumns ( DataTable table, List<List<?>> objectList,
	String[] excludeNames,
	String [] arrayColumns, String[] booleanColumns, String [] dateTimeColumns,
	String [] doubleColumns, String [] integerColumns, String [] textColumns ) {
	String routine = getClass().getSimpleName() + ".initializeTableColumns";
	
	// First loop through the list of Map to get the column names:
	// - it is not required that all objects have the same element names (although this is usually the case)
	
	Map<?,?> listMap = null;
	List<String> columnNames = new ArrayList<>();
	String name;
	Object value;
	boolean nameFound = false;
	boolean excludeNameFound = false;
	// Loop through the list of lists.
	for ( List<?> listItem : objectList ) {
	// Loop through the objects in each list.
	for ( Object o : listItem ) {
		if ( o instanceof Map ) {
			// As expected each list item is a Map of individual name/value objects.
			listMap = (Map<?,?>)o;
			// Loop through the items in the map and add columns for simple object types,
			// but ignore List and Map objects.
			for (Map.Entry<?, ?> entry : listMap.entrySet() ) {
				value = entry.getValue();
				name = (String)entry.getKey();
				if ( value instanceof Map ) {
					// Value is a list or map so don't add as a table column:
					// - TODO smalers 2021-07-31 may add a way to do it later
					continue;
				}
				else if ( (value instanceof List) && isArrayColumn(name, arrayColumns) ) {
					// Only process into a column if indicated as an array column:
					// - otherwise will ignore
					// Check whether a new column name to add.
					nameFound = false;
					for ( String columnName : columnNames ) {
						if ( columnName.equals(name) ) {
							nameFound = true;
							break;
						}
					}
					if ( !nameFound ) {
						// Object name was not found so add as a column name.
						columnNames.add(name);
					}
				}
				else if ( value instanceof List ) {
					// List but not a requested array so ignore.
					continue;
				}
				else {
					// Exclude names.
					excludeNameFound = false;
					for ( String excludeName : excludeNames ) {
						if ( name.equals(excludeName) ) {
							excludeNameFound = true;
							break;
						}
					}
					if ( excludeNameFound ) {
						continue;
					}
					// Check whether a new column name to add.
					nameFound = false;
					for ( String columnName : columnNames ) {
						if ( columnName.equals(name) ) {
							nameFound = true;
							break;
						}
					}
					if ( !nameFound ) {
						// Object name was not found so add as a column name.
						columnNames.add(name);
					}
				}
			}
		}
	}
	}
	
	int numColumns = columnNames.size();
	Message.printStatus(2, routine, "Number of columns = " + numColumns );

	// Allocate arrays to count how many object types are found for each column:
	// - column types can typically be determined without an issue and command parameters can define if an issue
    int [] countBoolean = new int[numColumns];
    int [] countDouble = new int[numColumns];
	int [] countInt = new int[numColumns];
    int [] countNull = new int[numColumns];
    int [] countString = new int[numColumns];
    int [] lenmaxString = new int[numColumns];
    int [] precision = new int[numColumns];
    int [] precisionString = new int[numColumns]; // If a string but contains period, the precision based on the string

    // Additional granularity for array columns.
    boolean [] isArray = new boolean[numColumns];
    int [] countArrayBoolean = new int[numColumns];
    int [] countArrayDouble = new int[numColumns];
	int [] countArrayInt = new int[numColumns];
    int [] countArrayNull = new int[numColumns];
    int [] countArrayString = new int[numColumns];

    for ( int icol = 0; icol < numColumns; icol++ ) {
        countBoolean[icol] = 0;
        countDouble[icol] = 0;
        countInt[icol] = 0;
        countNull[icol] = 0;
        countString[icol] = 0;
        lenmaxString[icol] = 0;
        precisionString[icol] = 0;
        precision[icol] = 0;

        isArray[icol] = false;
        countArrayBoolean[icol] = 0;
        countArrayDouble[icol] = 0;
	    countArrayInt[icol] = 0;
        countArrayNull[icol] = 0;
        countArrayString[icol] = 0;
    }

    // Empty list as a fall through:
    // - should be created below for actual columns
    List<TableField> tableFields = new ArrayList<>();
	// Loop through the list of lists.
	for ( List<?> listItem : objectList ) {
	// Loop through the objects in each list.
	for ( Object o : listItem ) {
		if ( o instanceof Map ) {
			// As expected each list item is a Map of individual name/value objects.
			listMap = (Map<?,?>)o;
			// Loop through the items in the map and add columns for simple object types,
			// but ignore List and Map objects.
			String s;
			int i;
			int len;
			double d;
			float f;
			int periodPos;
			for (Map.Entry<?, ?> entry : listMap.entrySet() ) {
				value = entry.getValue();
				name = (String)entry.getKey();
				if ( value instanceof Map ) {
					// Value is a list or map so don't add as a table column:
					// - TODO smalers 2021-07-31 may add a way to do it later
				}
				else if ( (value instanceof List) && isArrayColumn(name, arrayColumns) ) {
					// Need to indicate that the column is an array.
					int iColumn = -1;
					for ( String columnName : columnNames ) {
						++iColumn;
						if ( columnName.equals(name) ) {
							break;
						}
					}
					if ( iColumn < 0 ) {
						// Should not happen.
						continue;
					}
					isArray[iColumn] = true;
					// Determine the data type in the array values.
					List<?> objectList2 = (List<?>)value;
					for ( Object value2 : objectList2 ) {
						if ( value2 == null ) {
							++countArrayNull[iColumn];
						}
						else if ( value2 instanceof Boolean ) {
							// Length accommodates "False" with commas and surrounding brackets.
							lenmaxString[iColumn] = 5*objectList2.size() + 2 + (objectList2.size() - 1);
							++countArrayBoolean[iColumn];
						}
						else if ( value2 instanceof Double ) {
							d = ((Double)value2).doubleValue();
							// Estimate based on how table will format (6 digits precision).
							if ( d > 0.0 ) {
								len = (int)(Math.log10(d) + 1) + 1 + 6;
								len = len*objectList2.size() + 2 + (objectList2.size() - 1);
								if ( len > lenmaxString[iColumn] ) {
									lenmaxString[iColumn] = len;
								}
							}
							++countArrayDouble[iColumn];
						}
						else if ( value2 instanceof Float ) {
							// Treat the same as double but handle float for length check.
							f = ((Float)value2).floatValue();
							// Estimate based on how table will format (6 digits precision).
							if ( f > 0.0 ) {
								len = (int)(Math.log10(f) + 1) + 1 + 6;
								len = len*objectList2.size() + 2 + (objectList2.size() - 1);
								if ( len > lenmaxString[iColumn] ) {
									lenmaxString[iColumn] = len;
								}
							}
							++countArrayDouble[iColumn];
						}
						else if ( value2 instanceof Integer ) {
							i = ((Integer)value2).intValue();
							if ( i > 0 ) {
								len = (int)(Math.log10(i) + 1);
								len = len*objectList2.size() + 2 + (objectList2.size() - 1);
								if ( len > lenmaxString[iColumn] ) {
									lenmaxString[iColumn] = len;
								}
							}
							++countArrayInt[iColumn];
						}
						else if ( value2 instanceof String ) {
							++countArrayString[iColumn];
							s = (String)value2;
							len = s.length();
							// Assume same length for all strings.
							len = len*objectList2.size() + 2 + (objectList.size() - 1);
							if ( len > lenmaxString[iColumn]) {
								lenmaxString[iColumn] = len;
							}
						}
					}
				}
				else if ( value instanceof List ) {
					// List but not a requested array so ignore.
				}
				else {
					// Lookup the column number from the name:
					// - typically always be in the same order but not required
					int iColumn = -1;
					for ( String columnName : columnNames ) {
						++iColumn;
						if ( columnName.equals(name) ) {
							break;
						}
					}
					if ( iColumn < 0 ) {
						// Should not happen.
						continue;
					}
					if ( value == null ) {
						++countNull[iColumn];
					}
					else if ( value instanceof Boolean ) {
						lenmaxString[iColumn] = 5; // Accommodates "False".
						++countBoolean[iColumn];
					}
					else if ( value instanceof Double ) {
						d = ((Double)value).doubleValue();
						// TODO smalers 2021-08-02 need to handle precision:
						// - for now default to 4
						// - does "toString" return a reasonable number of digits (but could be a problem if exponential notation)
						precision[iColumn] = this.defaultPrecision;
						if ( d > 0.0 ) {
							len = (int)(Math.log10(d) + 1) + 1 + precision[iColumn];
							if ( len > lenmaxString[iColumn] ) {
								lenmaxString[iColumn] = (int)(Math.log10(d) + 1) + 1 + precision[iColumn];
							}
						}
						++countDouble[iColumn];
					}
					else if ( value instanceof Float ) {
						// Treat the same as double but handle float for length check.
						f = ((Float)value).floatValue();
						// TODO smalers 2021-08-02 need to handle precision:
						// - for now default to 4
						precision[iColumn] = this.defaultPrecision;
						if ( f > 0.0 ) {
							len = (int)(Math.log10(f) + 1) + 1 + precision[iColumn];
							if ( len > lenmaxString[iColumn] ) {
								lenmaxString[iColumn] = len;
							}
						}
						++countDouble[iColumn];
					}
					else if ( value instanceof Integer ) {
						i = ((Integer)value).intValue();
						if ( i > 0 ) {
							len = (int)(Math.log10(i) + 1);
							if ( len > lenmaxString[iColumn] ) {
								lenmaxString[iColumn] = len;
							}
						}
						++countInt[iColumn];
					}
					else if ( value instanceof String ) {
						++countString[iColumn];
						s = (String)value;
						len = s.length();
						// The following also helps with floating point numbers as the total width.
						if ( len > lenmaxString[iColumn]) {
							lenmaxString[iColumn] = len;
						}
						// If the string contains a period, get the number of characters after the last period.
						// This is used later if the type is specified by the command as a double.
						periodPos = s.lastIndexOf('.');
						if ( periodPos > 0 ) {
							precisionString[iColumn] = Math.max(precisionString[iColumn], (len - periodPos - 1));
							//Message.printStatus(2, routine, "precisonString for " + name + " is " + precisionString[iColumn]);
						}
						else {
							// Rely on precision from other strings.
						}
					}
				}
			}
		}
	}
	}
		
	// Set the initial column types from the data.

	// Loop through the table fields and based on the examination of data above,
	// set the table field type and if a string, max width.
	
	// Initialize all columns to string and reset below.
    tableFields = new ArrayList<TableField> ( numColumns );
    TableField newTableField;
   	for ( int i=0; i<numColumns; i++ ) {
   		newTableField = new TableField ( );
   		newTableField.setName (	columnNames.get(i).trim());
   		newTableField.setDataType(TableField.DATA_TYPE_STRING);
   		tableFields.add ( newTableField );
   	}
	int [] tableFieldType = new int[tableFields.size()];
	boolean isArray2 = false;
	boolean isBoolean = false;
	boolean isString = false;
	boolean isDateTime = false;
	boolean isInteger = false;
	boolean isDouble = false;
	TableField tableField = null;
	String columnName;
   	for ( int icol = 0; icol < numColumns; icol++ ) {
   	   	tableField = tableFields.get(icol);
   		columnName = tableField.getName();
   	   	isArray2 = isArray[icol];
   		isBoolean = false;
   		if ( booleanColumns != null ) {
   			for ( int i = 0; i < booleanColumns.length; i++ ) {
   				if ( booleanColumns[i].equalsIgnoreCase(tableField.getName()) ) {
   					isBoolean = true;
   				}
   			}
   		}
   		isDateTime = false;
   		if ( dateTimeColumns != null ) {
   			for ( int i = 0; i < dateTimeColumns.length; i++ ) {
   				if ( dateTimeColumns[i].equalsIgnoreCase(tableField.getName()) ) {
   					isDateTime = true;
   				}
   			}
   		}
   		isDouble = false;
   		if ( doubleColumns != null ) {
   			for ( int i = 0; i < doubleColumns.length; i++ ) {
   				if ( doubleColumns[i].equalsIgnoreCase(tableField.getName()) ) {
   					isDouble = true;
   				}
   			}
   		}
   		isInteger = false;
   		if ( integerColumns != null ) {
   			for ( int i = 0; i < integerColumns.length; i++ ) {
   				if ( integerColumns[i].equalsIgnoreCase(tableField.getName()) ) {
   					isInteger = true;
   				}
   			}
   		}
   		isString = false;
   		if ( textColumns != null ) {
   			for ( int i = 0; i < textColumns.length; i++ ) {
   				if ( textColumns[i].equalsIgnoreCase(tableField.getName()) ) {
   					isString = true;
   				}
   			}
   		}
   		// Set column type based on calling code specified type and then discovery from data.
   	   	if ( isArray2 ) {
   	   		// Handle array types.
   	   		tableField.setDataType(TableField.DATA_TYPE_ARRAY);
   	       	tableFieldType[icol] = TableField.DATA_TYPE_ARRAY;
            if ( (countArrayBoolean[icol] > 0) && (countArrayString[icol] == 0) &&
                (countArrayDouble[icol] == 0) && (countArrayInt[icol] == 0) ) {
               	// All data are boolean so assume column type is boolean.
               	tableField.setDataType(TableField.DATA_TYPE_ARRAY + TableField.DATA_TYPE_BOOLEAN);
               	tableFieldType[icol] = TableField.DATA_TYPE_ARRAY + TableField.DATA_TYPE_BOOLEAN;
               	//tableField.setWidth (lenmaxString[icol] );
               	Message.printStatus ( 2, routine, "Column [" + icol +
                   	"] \"" + columnName + "\" type is boolean array as determined from examining array data (" + countArrayBoolean[icol] +
                   	" booleans, " + countArrayString[icol] + " strings, " +
                      	countArrayNull[icol] + " nulls).");
           	}
           	else if ( (countArrayInt[icol] > 0) && (countArrayString[icol] == 0) &&
               	((countArrayDouble[icol] == 0) || (countArrayInt[icol] == countArrayDouble[icol])) ) {
               	// All data are integers so assume column type is integer.
               	// Note that integers also meet the criteria of double, hence the extra check above
               	// TODO SAM 2013-02-17 Need to handle DATA_TYPE_LONG
               	tableField.setDataType(TableField.DATA_TYPE_ARRAY + TableField.DATA_TYPE_INT);
               	tableFieldType[icol] = TableField.DATA_TYPE_ARRAY + TableField.DATA_TYPE_INT;
               	tableField.setWidth (lenmaxString[icol] );
               	Message.printStatus ( 2, routine, "Column [" + icol +
                   	"] \"" + columnName + "\" type is integer as determined from examining array data (" + countArrayInt[icol] +
                   	" integers, " + countArrayDouble[icol] + " doubles, " + countArrayString[icol] + " strings, " +
                      	countArrayNull[icol] + " nulls).");
           	}
           	else if ( (countArrayDouble[icol] > 0) && (countArrayString[icol] == 0) ) {
               	// All data are double (integers will also count as double) so assume column type is double.
               	tableField.setDataType(TableField.DATA_TYPE_ARRAY + TableField.DATA_TYPE_DOUBLE);
               	tableFieldType[icol] = TableField.DATA_TYPE_ARRAY + TableField.DATA_TYPE_DOUBLE;
              	tableField.setWidth (lenmaxString[icol] );
               	tableField.setPrecision ( precision[icol] );
               	Message.printStatus ( 2, routine, "Column [" + icol +
                   	"] \"" + columnName + "\" type is double as determined from examining array data (" + countArrayInt[icol] +
                   	" integers, " + countArrayDouble[icol] + " doubles, " + countArrayString[icol] + " strings, " +
                   	countArrayNull[icol] + " blanks, width=" + lenmaxString[icol] + ", precision=" + precision[icol] + ".");
          	}
           	else {
               	// Based on what is known, can only treat column as containing strings.
               	tableField.setDataType(TableField.DATA_TYPE_ARRAY + TableField.DATA_TYPE_STRING);
               	tableFieldType[icol] = TableField.DATA_TYPE_ARRAY + TableField.DATA_TYPE_STRING;
               	Message.printStatus ( 2, routine, "Column [" + icol +
                   	"] \"" + columnName + "\" type is string as determined from examining array data (" + countArrayBoolean[icol] +
                   	" booleans, " + countArrayInt[icol] +
                   	" integers, " + countArrayDouble[icol] + " doubles, " + countArrayString[icol] + " strings), " +
                   	countArrayNull[icol] + " blanks.");
  	       	}
     	}
   	   	// Below here are not array columns.
     	else if ( isBoolean ) {
    		tableField.setDataType(TableField.DATA_TYPE_BOOLEAN);
        	tableFieldType[icol] = TableField.DATA_TYPE_BOOLEAN;
        	Message.printStatus ( 2, routine, "Column [" + icol +
            	"] \"" + columnName + "\" type is boolean as determined from specified column type." );
    	}
    	else if ( isDateTime ) {
    		tableField.setDataType(TableField.DATA_TYPE_DATETIME);
        	tableFieldType[icol] = TableField.DATA_TYPE_DATETIME;
        	Message.printStatus ( 2, routine, "Column [" + icol +
            	"] \"" + columnName + "\" type is date/time as determined from specified column type." );
    	}
    	else if ( isDouble ) {
    		tableField.setDataType(TableField.DATA_TYPE_DOUBLE);
        	tableFieldType[icol] = TableField.DATA_TYPE_DOUBLE;
        	Message.printStatus ( 2, routine, "Column [" + icol +
            	"] \"" + columnName + "\" type is double as determined from specified column type." );
           	tableField.setWidth (lenmaxString[icol] );
           	// Precision is not provided in command parameters so try to determine from data, likely was a string.
           	if ( (precisionString[icol] > 0) && (precisionString[icol] <= 6) ) {
           		// Original type was a string and precision was determined from decimal point position:
           		// - but only use if <= 6 (otherwise may be a mistake)
           		tableField.setPrecision ( precisionString[icol] );
           	}
           	else {
           		// Default to value determined above.
           		if ( precision[icol] > 0 ) {
           			tableField.setPrecision ( Math.min(this.defaultPrecision,precision[icol]) );
           		}
           		else {
           			tableField.setPrecision ( this.defaultPrecision );
           		}
           	}
           	// Set the number of digits to the original string length.
           	tableField.setWidth (lenmaxString[icol] );
    	}
    	else if ( isInteger ) {
    		tableField.setDataType(TableField.DATA_TYPE_INT);
        	tableFieldType[icol] = TableField.DATA_TYPE_INT;
        	Message.printStatus ( 2, routine, "Column [" + icol +
            	"] \"" + columnName + "\" type is integer as determined from specified column type." );
    	}
    	else if ( isString ) {
    		tableField.setDataType(TableField.DATA_TYPE_STRING);
        	tableFieldType[icol] = TableField.DATA_TYPE_STRING;
        	if ( lenmaxString[icol] <= 0 ) {
            	// Likely that the entire column of numbers is empty so set the width to the field name
            	// width if available)
            	tableField.setWidth (tableFields.get(icol).getName().length() );
        	}
        	else {
            	tableField.setWidth (lenmaxString[icol] );
        	}
        	Message.printStatus ( 2, routine, "Column [" + icol +
            	"] \"" + columnName + "\" type is string as determined from specified column type." );
    	}
    	// Below here determine the type from the data values.
    	else if ( (countBoolean[icol] > 0) && (countString[icol] == 0) &&
    		(countDouble[icol] == 0) && (countInt[icol] == 0) ) {
    	    // All data are boolean so assume column type is boolean.
    	    tableField.setDataType(TableField.DATA_TYPE_BOOLEAN);
    	    tableFieldType[icol] = TableField.DATA_TYPE_BOOLEAN;
    	    tableField.setWidth (lenmaxString[icol] );
    	    Message.printStatus ( 2, routine, "Column [" + icol +
    	        "] \"" + columnName + "\" type is boolean as determined from examining data (" + countBoolean[icol] +
    	        " booleans, " + countString[icol] + " strings, " +
                countNull[icol] + " nulls).");
    	}
    	else if ( (countInt[icol] > 0) && (countString[icol] == 0) &&
    	    ((countDouble[icol] == 0) || (countInt[icol] == countDouble[icol])) ) {
    	    // All data are integers so assume column type is integer.
    	    // Note that integers also meet the criteria of double, hence the extra check above
    	    // TODO SAM 2013-02-17 Need to handle DATA_TYPE_LONG
    	    tableField.setDataType(TableField.DATA_TYPE_INT);
    	    tableFieldType[icol] = TableField.DATA_TYPE_INT;
    	    tableField.setWidth (lenmaxString[icol] );
    	    Message.printStatus ( 2, routine, "Column [" + icol +
    	        "] \"" + columnName + "\" type is integer as determined from examining data (" + countInt[icol] +
    	        " integers, " + countDouble[icol] + " doubles, " + countString[icol] + " strings, " +
                countNull[icol] + " nulls).");
    	}
    	else if ( (countDouble[icol] > 0) && (countString[icol] == 0) ) {
    	    // All data are double (integers will also count as double) so assume column type is double.
            tableField.setDataType(TableField.DATA_TYPE_DOUBLE);
            tableFieldType[icol] = TableField.DATA_TYPE_DOUBLE;
            tableField.setWidth (lenmaxString[icol] );
           	// Precision is not provided in command parameters so try to determine from data, likely was a string.
           	if ( (precisionString[icol] > 0) && (precisionString[icol] <= 6) ) {
           		// Original type was a string and precision was determined from decimal point position:
           		// - but only use if <= 6 (otherwise may be a mistake)
           		tableField.setPrecision ( precisionString[icol] );
           	}
           	else {
           		// Default to value determined above.
           		if ( precision[icol] > 0 ) {
           			tableField.setPrecision ( Math.min(this.defaultPrecision,precision[icol]) );
           		}
           		else {
           			tableField.setPrecision ( this.defaultPrecision );
           		}
           	}
            Message.printStatus ( 2, routine, "Column [" + icol +
                "] \"" + columnName + "\" type is double as determined from examining data (" + countInt[icol] +
                " integers, " + countDouble[icol] + " doubles, " + countString[icol] + " strings, " +
                countNull[icol] + " blanks, width=" + lenmaxString[icol] + ", precision=" + precision[icol] + ".");
        }
        else {
            // Based on what is known, can only treat column as containing strings.
            tableField.setDataType(TableField.DATA_TYPE_STRING);
            tableFieldType[icol] = TableField.DATA_TYPE_STRING;
            if ( lenmaxString[icol] <= 0 ) {
                // Likely that the entire column of numbers is empty so set the width to the field name
                // width if available)
                tableField.setWidth (tableFields.get(icol).getName().length() );
            }
            else {
                tableField.setWidth (lenmaxString[icol] );
            }
            Message.printStatus ( 2, routine, "Column [" + icol +
                   "] \"" + columnName + "\" type is string as determined from examining data (" + countInt[icol] +
                   " integers, " + countDouble[icol] + " doubles, " + countString[icol] + " strings), " +
                   countNull[icol] + " blanks.");
           // Message.printStatus ( 2, routine, "length max=" + lenmax_string[icol] );
        }
    }
		
	// Set the table fields.
	table.setTableFields(tableFields);
}

/**
 * Determine whether a column is an array column.
 * This is used to handle primitive array data.
 * @param name JSON name to check
 * @param arrayColumns the list of JSON names that should be translated to table array columns
 */
private boolean isArrayColumn ( String name, String [] arrayColumns ) {
	for ( String arrayColumn : arrayColumns ) {
		if ( name.equals(arrayColumn) ) {
			return true;
		}
	}
	return false;
}

// Use base class parseCommand()

// TODO SAM 2015-12-05 Need to evaluate how best to flatten arrays embedded in objects
/**
Read a JSON file into a table object.
*/
private DataTable readJSONUsingGson ( String inputFile, String [] excludeNames,
	String [] dateTimeColumns, String [] doubleColumns,
	String [] integerColumns, String [] textColumns, int top )
throws FileNotFoundException, IOException
{	String routine = getClass().getSimpleName() + ".readJSON";
	DataTable table = new DataTable();
    JsonReader jsonReader = new JsonReader(new FileReader(inputFile));
    // Refer to Gson API:  http://google.github.io/gson/apidocs/
    // Consume the opening "["
    if ( Message.isDebugOn ) {
    	Message.printDebug(2, routine, "Begin main array");
    }
    jsonReader.beginArray();
    // Read until there are no more objects in the array
    // If the first object, set up the table columns
    boolean tableInitialized = false;
    int tableColumnAdded = 0; // Used to handle table initialization
    int row = -1; // Row in table
    int col = 0; // Column in table
    int colType = 0; // Column type when adding/using table
    int objectCount = 0; // Count of objects processed, used with "top"
    double dvalue = 0; // Number value for token
    String svalue = ""; // String value for token
    boolean bvalue = false; // Boolean value for token
    int arrayRowStart = -1, arrayRowEnd = -1, arrayColStart = -1; // Used to repeat array rows for columns not in embedded array
    int objectArrayCount = 0; // Count of arrays embedded in top-level object - only allow 1 active for flattening
    boolean excludeName = false; // Whether or not to exclude a name
    // Loop through top-level objects in array
    // - may have simple name/value pairs but some names may correspond to arrays
    // - currently only allow one array value and handle flattening
    while (jsonReader.hasNext()) {
    	// Table row is the object count, which is zero-indexed and only incremented at end of this loop
    	// Process the objects in the top-level array
    	arrayRowStart = -1;
    	arrayRowEnd = -1;
    	arrayColStart = -1;
    	objectArrayCount = 0; // Within the object
    	++row; // Goes to 0 on first iteration
    	// In the array so get the next object
        if ( Message.isDebugOn ) {
        	Message.printDebug(2, routine, "Begin object");
        }
        jsonReader.beginObject();
        while (jsonReader.hasNext()) {
	    	// Process name/value pairs until exhausted
        	// Always get the name
	    	String name = jsonReader.nextName();
	    	excludeName = readJSON_ExcludeName(name,excludeNames);
	    	// Peek ahead to determine the value type of the next value so that it can be handled below
	    	JsonToken token = jsonReader.peek();
	    	// List token types in likely order of occurrence to improve performance
	    	if ( token == JsonToken.STRING ) {
	    		svalue = jsonReader.nextString();
	    		if ( !tableInitialized ) {
	    			colType = TableField.DATA_TYPE_STRING; // Default
	    			// Check for column type override
	    			colType = readJSON_DetermineColumnType ( name, colType, dateTimeColumns, doubleColumns, integerColumns, textColumns );
	    			if ( !excludeName && readJSON_AddTableColumn(table,name,colType) >= 0 ) {
	    				++tableColumnAdded;
	    			}
	    		}
	    		// Set the table cell value
	    		if ( !excludeName ) {
		    		try {
		    			col = table.getFieldIndex(name);
		    			colType = table.getFieldDataType(col);
		    		}
		    		catch ( Exception e ) {
		    			col = -1;
		    		}
		    		if ( col >= 0 ) {
		    			try {
			    			if ( colType == TableField.DATA_TYPE_STRING ) {
			    				// Simple set
			    				table.setFieldValue(row, col, svalue, true);
			    			}
			    			else if ( colType == TableField.DATA_TYPE_DOUBLE ) {
		    					double d = Double.parseDouble(svalue);
		    					table.setFieldValue(row, col, d, true);
			    			}
			    			else if ( colType == TableField.DATA_TYPE_INT ) {
		    					int i = Integer.parseInt(svalue);
		    					table.setFieldValue(row, col, i, true);
			    			}
			    			else if ( colType == TableField.DATA_TYPE_DATETIME ) {
			    				// Parse the date/time
			    				table.setFieldValue(row,col,DateTime.parse(svalue));
			    			}
		    			}
		    			catch ( Exception e ) {
		    				// TODO SAM 2015-12-05 Should generally not happen - evaluate error messages
		    				// Leave as initial null value in table
		    			}
		    		}
	    		}
	    	}
	    	else if ( token == JsonToken.NUMBER ) {
	    		dvalue = jsonReader.nextDouble();
	    		svalue = "" + dvalue;
	    		if ( !tableInitialized ) {
	    			// Check for column type override, may be double or integer
	    			colType = TableField.DATA_TYPE_DOUBLE; // Default
	    			colType = readJSON_DetermineColumnType ( name, colType, dateTimeColumns, doubleColumns, integerColumns, textColumns );
	    			if ( !excludeName && readJSON_AddTableColumn(table,name,colType) >= 0 ) {
	    				++tableColumnAdded;
	    			}
	    		}
	    		if ( !excludeName ) {
		    		try {
		    			col = table.getFieldIndex(name);
		    			colType = table.getFieldDataType(col);
		    		}
		    		catch ( Exception e ) {
		    			col = -1;
		    		}
		    		if ( col >= 0 ) {
		    			try {
			    			if ( colType == TableField.DATA_TYPE_DOUBLE ) {
			    				table.setFieldValue(row, col, dvalue, true);
			    			}
			    			else if ( colType == TableField.DATA_TYPE_STRING ) {
			    				table.setFieldValue(row, col, svalue, true);
			    			}
			    			else if ( colType == TableField.DATA_TYPE_INT ) {
			    				table.setFieldValue(row, col, (int)dvalue, true);
			    			}
		    			}
		    			catch ( Exception e ) {
		    				// TODO SAM 2015-12-05 Should generally not happen - evaluate error messages
		    			}
		    		}
	    		}
	    	}
	    	else if ( token == JsonToken.BOOLEAN ) {
	    		bvalue = jsonReader.nextBoolean();
	    		svalue = "" + bvalue;
    			colType = TableField.DATA_TYPE_BOOLEAN; // Default
    			colType = readJSON_DetermineColumnType ( name, colType, dateTimeColumns, doubleColumns, integerColumns, textColumns );
    			if ( !excludeName && readJSON_AddTableColumn(table,name,colType) >= 0 ) {
    				++tableColumnAdded;
    			}
    			if ( !excludeName ) {
		    		try {
		    			col = table.getFieldIndex(name);
		    			colType = table.getFieldDataType(col);
		    		}
		    		catch ( Exception e ) {
		    			col = -1;
		    		}
		    		if ( col >= 0 ) {
		    			try {
		    				table.setFieldValue(row, col, bvalue, true);
		    			}
		    			catch ( Exception e ) {
		    				// TODO SAM 2015-12-05 Should generally not happen - evaluate error messages
		    			}
		    		}
    			}
	    	}
	    	else if ( token == JsonToken.NULL ) {
	    		// TODO SAM 2015-12-05 need to handle in initialization, but table is initialized to nulls.
	    		svalue = null;
	    	}
	    	else if ( token == JsonToken.BEGIN_ARRAY ) {
	    		if ( (objectArrayCount >= 1) || excludeName ) {
	    			// Currently only know how to flatten one array in object (that is not excluded).
	    			jsonReader.skipValue();
	    		}
	    		else {
		    		// Expand the array and add columns to the table.
		    		// For array position [0] add to current row.
		    		// For array position [1+] repeat the other column values
		    		// This could be made recursive with some additional effort
	    			++objectArrayCount;
		    		jsonReader.beginArray();
		    		arrayRowStart = row; // Used to flatten other columns by repeating values
		    		arrayRowEnd = arrayRowStart;
		    		arrayColStart = -1;
		    		int arrayIndex = -1;
		    		int arrayObjectCount = 0; // Number of objects in the array
		    		while (jsonReader.hasNext()) {
		    			// Iterate through objects in the array.
		    			++arrayIndex;
		    			jsonReader.beginObject();
		    			++arrayObjectCount;
		    	        while (jsonReader.hasNext()) {
		    		    	// Process name/value pairs until exhausted
		    	        	// Always get the name
		    		    	name = jsonReader.nextName();
		    		    	token = jsonReader.peek();
		    		    	// List token types in likely order of occurrence to improve performance
		    		    	if ( token == JsonToken.STRING ) {
		    		    		svalue = jsonReader.nextString();
		    		    		if ( !tableInitialized && (arrayIndex == 0) ) {
			    		    		// Only add columns if the first array index.
		    		    			colType = TableField.DATA_TYPE_STRING; // Default
		    		    			// Check for column type override
		    		    			colType = readJSON_DetermineColumnType ( name, colType, dateTimeColumns, doubleColumns, integerColumns, textColumns );
		    		    			if ( readJSON_AddTableColumn(table,name,colType) >= 0 ) {
		    		    				++tableColumnAdded;
		    		    			}
		    		    		}
		    		    		// Set the table cell value
		    		    		try {
		    		    			col = table.getFieldIndex(name);
		    		    			if ( arrayColStart < 0 ) {
		    		    				arrayColStart = col;
		    		    			}
		    		    			colType = table.getFieldDataType(col);
		    		    		}
		    		    		catch ( Exception e ) {
		    		    			col = -1;
		    		    		}
		    		    		if ( col >= 0 ) {
		    		    			try {
		    			    			if ( colType == TableField.DATA_TYPE_STRING ) {
		    			    				// Simple set
		    			    				table.setFieldValue(row, col, svalue, true);
		    			    			}
		    			    			else if ( colType == TableField.DATA_TYPE_DOUBLE ) {
		    		    					double d = Double.parseDouble(svalue);
		    		    					table.setFieldValue(row, col, d, true);
		    			    			}
		    			    			else if ( colType == TableField.DATA_TYPE_INT ) {
		    		    					int i = Integer.parseInt(svalue);
		    		    					table.setFieldValue(row, col, i, true);
		    			    			}
		    			    			else if ( colType == TableField.DATA_TYPE_DATETIME ) {
		    			    				// Parse the date/time
		    			    				table.setFieldValue(row,col,DateTime.parse(svalue));
		    			    			}
		    		    			}
		    		    			catch ( Exception e ) {
		    		    				// TODO SAM 2015-12-05 Should generally not happen - evaluate error messages
		    		    				// Leave as initial null value in table
		    		    			}
		    		    		}
		    		    	}
			    	    	else if ( token == JsonToken.BEGIN_ARRAY ) {
			    	    		// Only go one level deep on hierarchy until figure out recursion
			    	    		jsonReader.skipValue();
			    	    	}
		    	        }
		    	        jsonReader.endObject();
		    			// Increment the row because each object in the array will be in a different row
		    			++row;
		    			++arrayRowEnd;
		    		}
		    		// Decrement the row because want the next name/value in the object to occur on the same row as the last array object
		    		// Columns will be filled in as necessary to fill the row
		    		// Only do this if the array actually had elements
		    		if ( arrayObjectCount > 0 ) {
		    			--row;
		    		}
		    		jsonReader.endArray();
		    		// Fill out the columns prior to those added by the array by repeating rows
		    		//Message.printStatus(2, routine, "Filling in content before array columns arrayRowStart="+arrayRowStart+", arrayRowEnd="+arrayRowEnd+", arrayColStart="+arrayColStart);
		    		for ( int iArrayRow = (arrayRowStart + 1); iArrayRow <= arrayRowEnd; iArrayRow++ ) {
		    			for ( int iArrayCol = 0; iArrayCol < arrayColStart; iArrayCol++ ) {
		    				// Duplicate each columns values from the first row of the array to other rows introduced by array
		    				try {
		    					table.setFieldValue(iArrayRow, iArrayCol, table.getFieldValue(arrayRowStart, iArrayCol));
		    				}
		    				catch ( Exception e ) {
		    					// Just leave the value null - should not happen
		    				}
		    			}
		    		}
	    		}
	    	}
	    	else if ( token == JsonToken.END_ARRAY ) {
	    		// TODO SAM 2015-12-05 need to consume array
	    		// Should not need to do anything if skipValue() or above code handles?
	    	}
	    	// For debugging
	        if ( Message.isDebugOn ) {
	        	Message.printDebug(2, routine, "Read name=\"" + name + "\" value=\"" + svalue + "\", set at row="+row+ ", col="+col);
	        }
	        //Message.printStatus(2, routine, "objectArrayCount="+objectArrayCount+", col="+col);
			if ( (objectArrayCount > 0) && (col >= 0) ) {
				if ( Message.isDebugOn ) {
					Message.printDebug(2, routine, "Filling in content before array columns arrayRowStart="+arrayRowStart+", arrayRowEnd="+arrayRowEnd+", arrayColStart="+arrayColStart);
				}
				// Fill out the rows for current column prior to those added by the array by repeating rows
				// Handle generically without caring about the column type
	    		for ( int iArrayRow = arrayRowStart; iArrayRow < row; iArrayRow++ ) {
    				// Duplicate column' value to other rows introduced by array (start of array to previous row)
    				try {
    					table.setFieldValue(iArrayRow, col, table.getFieldValue(row, col));
    				}
    				catch ( Exception e ) {
    					// Just leave the value null - should not happen
    				}
	    		}
			}
        }
        if ( Message.isDebugOn ) {
        	Message.printDebug(2, routine, "End object");
        }
        jsonReader.endObject();
        ++objectCount;
        // If any columns have been added indicate that the table has been initialized
        if ( tableColumnAdded > 0 ) {
        	tableInitialized = true;
        }
        // Exit reading if top has been exceeded
        if ( objectCount == top ) {
        	break;
        }
    }
    if ( Message.isDebugOn ) {
    	Message.printDebug(2, routine, "End main array");
    }
    // Don't need the following and it may throw exception if "top" is used
    //jsonReader.endArray();
    jsonReader.close();
	return table;
}

/**
Read a JSON file into a table object.
This code is similar to reading a CSV file, with automatic determination of column types.
*/
private int readJSONUsingJackson ( String inputFile, DataTable table, String arrayName, boolean appendArrays,
	String [] excludeNames,
	String [] arrayColumns, String [] booleanColumns, String [] dateTimeColumns,
	String [] doubleColumns, String [] integerColumns, String [] textColumns,
	int top,
	CommandStatus status, int warningCount )
throws FileNotFoundException, IOException
{	String routine = getClass().getSimpleName() + ".readJSON";

	boolean useMapper = true;
	if ( useMapper ) {
		// Map the JSON string into an object hierarchy and then pull out what is needed:
		// - may fail on very large input files by running out of memory
		
		// First read the input file into a string.
		StringBuilder responseJson = IOUtil.fileToStringBuilder(inputFile);
		
		// Parse the string into object hierarchy:
		// - if the JSON string starts with '{', read into a map
		// - if the JSON string starts with '[', read into an array and add to a map for further processing
		ObjectMapper mapper = new ObjectMapper();
		Map<?,?> map = null;
		if ( responseJson.charAt(0) == '{' ) {
			map = mapper.readValue(responseJson.toString(), Map.class);
		}
		else if ( responseJson.charAt(0) == '[' ) {
			// Create a top-level map with black name:
			// - use a LinkedHashMap to preserve element order
			//map = new LinkedHashMap<>();
			if ( Message.isDebugOn ) {
				Message.printStatus(2, routine,
					"JSON array detected.  Adding an object nameed 'toparray' object at top to facilitate parsing into a map.");
			}
			responseJson.insert(0, "{ \"toparray\" : ");
			responseJson.append("}");
			map = mapper.readValue(responseJson.toString(), Map.class);
		}
		else {
			String message = "JSON does not start with { or [ - cannot parse.";
        	status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            	message, "Verify that the JSON file content is valid JSON starting with { or [ ." ) );
        	Message.printWarning(3, routine, message);
        	// Cannot read any more data.
        	++warningCount;
        	return warningCount;
		}
		if ( Message.isDebugOn ) {
			Message.printStatus(2, routine, "Map from JSON has " + map.size() + " top-level entries.");
		}

		// Find the array of interest, which is actually a Java List:
		// - create the list up front
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
		
		// Process the objects in the list.
		
		initializeTableColumns ( table, arrays,
			excludeNames,
			arrayColumns, booleanColumns, dateTimeColumns, doubleColumns, integerColumns, textColumns );
		
		// Read the data.
		
		List<String> problems = new ArrayList<>();
		readTableData ( table, arrays, arrayColumns, excludeNames, top, problems );
		
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
		// Other options
		
	}
	
	return warningCount;
}

/**
Add a column to the table for the column type.
*/
private int readJSON_AddTableColumn (DataTable table, String name, int colType)
{
	if ( colType == TableField.DATA_TYPE_STRING ) {
		return table.addField(new TableField(colType, name, -1, -1), null);
	}
	else if ( colType == TableField.DATA_TYPE_DATETIME ) {
		return table.addField(new TableField(colType, name), null);
	}
	else if ( colType == TableField.DATA_TYPE_DOUBLE ) {
		return table.addField(new TableField(TableField.DATA_TYPE_DOUBLE, name, -1, 6), null);
	}
	else if ( colType == TableField.DATA_TYPE_INT ) {
		return table.addField(new TableField(TableField.DATA_TYPE_INT, name), null);
	}
	else if ( colType == TableField.DATA_TYPE_BOOLEAN ) {
		return table.addField(new TableField(TableField.DATA_TYPE_BOOLEAN, name), null);
	}
	else {
		return -1;
	}
}

// TODO SAM 2015-12-05 Maybe move this to DataTable to allow reuse.
/**
Determine the column type considering overrides.
@param colType0 initial column type if override is not specified.
*/
private int readJSON_DetermineColumnType ( String colName, int colType0, String [] dateTimeColumns,
	String [] doubleColumns, String [] integerColumns, String [] textColumns )
{	int colType = colType0;
	
	if ( dateTimeColumns != null) {
		for ( int i = 0; i < dateTimeColumns.length; i++ ) {
			if ( dateTimeColumns[i].equalsIgnoreCase(colName) ) {
				return TableField.DATA_TYPE_DATETIME;
			}
		}
	}
	if ( doubleColumns != null) {
		for ( int i = 0; i < doubleColumns.length; i++ ) {
			if ( doubleColumns[i].equalsIgnoreCase(colName) ) {
				return TableField.DATA_TYPE_DOUBLE;
			}
		}
	}
	if ( integerColumns != null) {
		for ( int i = 0; i < integerColumns.length; i++ ) {
			if ( integerColumns[i].equalsIgnoreCase(colName) ) {
				return TableField.DATA_TYPE_INT;
			}
		}
	}
	if ( textColumns != null) {
		for ( int i = 0; i < textColumns.length; i++ ) {
			if ( textColumns[i].equalsIgnoreCase(colName) ) {
				return TableField.DATA_TYPE_STRING;
			}
		}
	}
	return colType;
}

/**
Determine whether the name should be excluded from output.
*/
private boolean readJSON_ExcludeName(String name, String [] excludeNames) {
	if ( excludeNames != null ) {
		for ( int i = 0; i < excludeNames.length; i++ ) {
			if ( name.equalsIgnoreCase(excludeNames[i]) ) {
				return true;
			}
		}
	}
	return false;
}

/**
 * Read the data into the table.
 * @param table data table to read
 * @param objectList list of objects lists to process 
 * @param arrayColumns array of columns that are arrays
 * @param excludeNames array of data array object names to exclude
 * @param top if non-zero indicates the number of first records to read
 * @param problems list of strings indicating parse problems
 */
private void readTableData ( DataTable table, List<List<?>> objectList, String [] arrayColumns,
	String[] excludeNames, int top, List<String> problems ) {
	String routine = getClass().getSimpleName() + ".readTableData";
	Map<?,?> listMap;
	Object value;
	String name;
	TableRecord rec = null;
	int iObject = -1;
	String s;
	int iColumn = 0;
	int columnType = 0;
	boolean excludeNameFound = false;
	// Loop through the list of lists.
	for ( List<?> listItem : objectList ) {
	// Loop through the objects in each list.
	for ( Object o : listItem ) {
		++iObject;
		// Check 'top'.
		if ( (top != 0) && (iObject >= top) ) {
			// Done reading.
			break;
		}
		if ( o instanceof Map ) {
			// As expected each list item is a Map of individual name/value objects.
			listMap = (Map<?,?>)o;
			// Create a new table record, which should include all defined columns from the previous step.
            try {
            	rec = table.addRecord(table.emptyRecord());
            }
            catch ( Exception e ) {
            	// Should not happen.
            	problems.add("Error adding new record for object [" + iObject + "].");
            	continue;
            }
			// Loop through the items in the map and add rows for primitive data types.
			for (Map.Entry<?, ?> entry : listMap.entrySet() ) {
				value = entry.getValue();
				name = (String)entry.getKey();
				if ( value instanceof Map ) {
					// Value is a map so don't add as a table column.
					continue;
				}
				else if ( (value instanceof List) && isArrayColumn(name, arrayColumns) ) {
					// Simple data array that should be included as an array in the table column:
					// - value is a List of primitive types but DataTable wants an array

					// Lookup the column number from the name:
					// - typically always be in the same order but not required
					try {
						iColumn = table.getFieldIndex(name);
					}
					catch ( Exception e ) {
						problems.add("Error getting table column for object [" + iObject + "] name: " + name );
						continue;
					}

					// The following is brute force but dealing with abstraction is a pain.
					int baseType = table.getFieldDataType(iColumn) - TableField.DATA_TYPE_ARRAY_BASE;
					if ( baseType == TableField.DATA_TYPE_BOOLEAN ) {
						@SuppressWarnings("unchecked")
						List<Boolean> BooleanList = (List<Boolean>)value;
						value = BooleanList.toArray(new Boolean[0]);
					}
					else if ( baseType == TableField.DATA_TYPE_DOUBLE ) {
						@SuppressWarnings("unchecked")
						List<Double> DoubleList = (List<Double>)value;
						value = DoubleList.toArray(new Double[0]);
					}
					else if ( baseType == TableField.DATA_TYPE_INT ) {
						@SuppressWarnings("unchecked")
						List<Integer> IntegerList = (List<Integer>)value;
						value = IntegerList.toArray(new Integer[0]);
					}
					else if ( baseType == TableField.DATA_TYPE_STRING ) {
						@SuppressWarnings("unchecked")
						List<String> StringList = (List<String>)value;
						value = StringList.toArray(new String[0]);
					}
					else {
						// Don't know how to handle.
						problems.add("Don't know how to handle array conversion for object [" + iObject + "] name: " + name );
					}
				}
				else if ( value instanceof List ) {
					// List that is ignored.
					continue;
				}
				else {
					// Exclude names.
					excludeNameFound = false;
					for ( String excludeName : excludeNames ) {
						if ( name.equals(excludeName) ) {
							excludeNameFound = true;
							break;
						}
					}
					if ( excludeNameFound ) {
						// Ignore the data.
						continue;
					}
					// Lookup the column number from the name:
					// - typically always be in the same order but not required
					try {
						iColumn = table.getFieldIndex(name);
					}
					catch ( Exception e ) {
						problems.add("Error getting table column for object [" + iObject + "] name: " + name );
						continue;
					}
					if ( value == null ) {
						// Just pass through and set null.
					}
					else if ( value instanceof String ) {
						// If a string, check the column type and convert if necessary:
						// - this can be used, for example, to convert boolean string to boolean
						// - DateTime must be in standard formats
						s = (String)value;
						if ( s.equalsIgnoreCase("NULL") ) {
							value = null;
						}
						else {
							// Problems use index 1+.
							columnType = table.getFieldDataType(iColumn);
							if ( columnType == TableField.DATA_TYPE_BOOLEAN) {
								// Try to parse.
								try {
									value = Boolean.parseBoolean(s);
								}
								catch ( Exception e ) {
									problems.add("Invalid boolean for \"" + name + "\" object (" + (iObject + 1) + "): " + s );
									value = null;
								}
							}
							else if ( columnType == TableField.DATA_TYPE_DATETIME) {
								// Try to parse.
								try {
									value = DateTime.parse(s);
								}
								catch ( Exception e ) {
									problems.add("Invalid date/time for \"" + name + "\" object (" + (iObject + 1) + ") (" + e + "): " + s );
								    Message.printWarning(3,routine,e);
									value = null;
								}
							}
							else if ( columnType == TableField.DATA_TYPE_DOUBLE) {
								// Try to parse.
								try {
									value = Double.parseDouble(s);
								}
								catch ( Exception e ) {
									problems.add("Invalid double for \"" + name + "\" object (" + (iObject + 1) + "): " + s );
									value = null;
								}
							}
							else if ( columnType == TableField.DATA_TYPE_INT) {
								// Try to parse.
								try {
									value = Integer.parseInt(s);
								}
								catch ( Exception e ) {
									problems.add("Invalid integer for \"" + name + "\" object (" + (iObject + 1) + "): " + s );
									value = null;
								}
							}
							else if ( columnType == TableField.DATA_TYPE_STRING) {
								// Allow conversion from non-text to parse.
								if ( value instanceof String ) {
									// No action needed.
								}
								else {
									// Convert to a string.
									value = value.toString();
								}
							}
							else {
								// Null value is OK.
								problems.add("Unknown column type for \"" + name + "\" object (" + (iObject + 1) + "): " + columnType);
							}
						}
					}
				}
				// Below here whatever value was determined is set.
				try {
					rec.setFieldValue(iColumn, value);
				}
				catch ( Exception e ) {
					problems.add("Error setting table record value for \"" + name + "\" object [" + (iObject + 1) + "]." );
				}
			}
		}
	}
	}
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message = "";
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
        setDiscoveryTable ( null );
    }

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();

    String TableID = parameters.getValue ( "TableID" );
	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
	String InputFile = parameters.getValue ( "InputFile" ); // Expanded below.
	String ArrayName = parameters.getValue ( "ArrayName" );
	ArrayName = TSCommandProcessorUtil.expandParameterValue(processor, this, ArrayName);
	if ( ArrayName == null ) {
		// Set to empty to avoid null pointer exceptions.
		ArrayName = "";
	}
	boolean appendArrays = false;
	String AppendArrays = parameters.getValue ( "AppendArrays" );
	if ( (AppendArrays != null) && AppendArrays.equalsIgnoreCase(this._True) ) {
		appendArrays = true;
	}

	String ExcludeNames = parameters.getValue ( "ExcludeNames" );
	ExcludeNames = TSCommandProcessorUtil.expandParameterValue(processor, this, ExcludeNames);
	String [] excludeNames = null;
    if ( (ExcludeNames != null) && !ExcludeNames.isEmpty() ) {
    	excludeNames = ExcludeNames.split(",");
        for ( int i = 0; i < excludeNames.length; i++ ) {
        	excludeNames[i] = excludeNames[i].trim();
        }
    }
    else {
    	// Initialize to empty array to simplify error handling.
    	excludeNames = new String[0];
    }
	String ArrayColumns = parameters.getValue ( "ArrayColumns" );
	ArrayColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, ArrayColumns);
	String [] arrayColumns = null;
    if ( (ArrayColumns != null) && !ArrayColumns.isEmpty() ) {
        arrayColumns = ArrayColumns.split(",");
        for ( int i = 0; i < arrayColumns.length; i++ ) {
            arrayColumns[i] = arrayColumns[i].trim();
        }
    }
    else {
    	arrayColumns = new String[0];
    }
	String BooleanColumns = parameters.getValue ( "BooleanColumns" );
	BooleanColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, BooleanColumns);
	String [] booleanColumns = null;
    if ( (BooleanColumns != null) && !BooleanColumns.isEmpty() ) {
        booleanColumns = BooleanColumns.split(",");
        for ( int i = 0; i < booleanColumns.length; i++ ) {
            booleanColumns[i] = booleanColumns[i].trim();
        }
    }
    else {
    	booleanColumns = new String[0];
    }
	String DateTimeColumns = parameters.getValue ( "DateTimeColumns" );
	DateTimeColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, DateTimeColumns);
	String [] dateTimeColumns = null;
    if ( (DateTimeColumns != null) && !DateTimeColumns.isEmpty() ) {
        dateTimeColumns = DateTimeColumns.split(",");
        for ( int i = 0; i < dateTimeColumns.length; i++ ) {
            dateTimeColumns[i] = dateTimeColumns[i].trim();
        }
    }
    else {
    	dateTimeColumns = new String[0];
    }
	String DoubleColumns = parameters.getValue ( "DoubleColumns" );
	DoubleColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, DoubleColumns);
	String [] doubleColumns = null;
    if ( (DoubleColumns != null) && !DoubleColumns.isEmpty() ) {
    	doubleColumns = DoubleColumns.split(",");
        for ( int i = 0; i < doubleColumns.length; i++ ) {
        	doubleColumns[i] = doubleColumns[i].trim();
        }
    }
    else {
    	doubleColumns = new String[0];
    }
	String IntegerColumns = parameters.getValue ( "IntegerColumns" );
	IntegerColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, IntegerColumns);
	String [] integerColumns = null;
    if ( (IntegerColumns != null) && !IntegerColumns.isEmpty() ) {
    	integerColumns = IntegerColumns.split(",");
        for ( int i = 0; i < integerColumns.length; i++ ) {
        	integerColumns[i] = integerColumns[i].trim();
        }
    }
    else {
    	integerColumns = new String[0];
    }
	String TextColumns = parameters.getValue ( "TextColumns" );
	TextColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, TextColumns);
	String [] textColumns = null;
    if ( (TextColumns != null) && !TextColumns.isEmpty() ) {
    	textColumns = TextColumns.split(",");
        for ( int i = 0; i < textColumns.length; i++ ) {
        	textColumns[i] = textColumns[i].trim();
        }
    }
    else {
    	textColumns = new String[0];
    }
	String Top = parameters.getValue ( "Top" );
	Top = TSCommandProcessorUtil.expandParameterValue(processor, this, Top);
	int top = 0;
	if ( (Top != null) && !Top.isEmpty() ) {
		top = Integer.parseInt(Top);
	}

	String InputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
        	TSCommandProcessorUtil.expandParameterValue(processor, this,InputFile)) );
	if ( commandPhase == CommandPhaseType.RUN ) {
		if ( !IOUtil.fileExists(InputFile_full) ) {
			message += "\nThe JSON file \"" + InputFile_full + "\" does not exist.";
			++warning_count;
        	status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            	message, "Verify that the JSON file exists." ) );
		}
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	// Now process the file.

    if ( commandPhase == CommandPhaseType.RUN ) {
    	DataTable table = null;
    	PropList props = new PropList ( "DataTable" );

        if ( (DateTimeColumns != null) && !DateTimeColumns.isEmpty() ) {
            props.set ( "DateTimeColumns=" + DateTimeColumns);
        }
        if ( (TextColumns != null) && !TextColumns.isEmpty() ) {
            props.set ( "TextColumns=" + TextColumns);
        }
        if ( (Top != null) && !Top.isEmpty() ) {
            props.set ( "Top=" + Top);
        }
        boolean useJackson = true;
    	try {
    		if ( useJackson ) {
    			// Use Jackson, preferred because it is used in more code elsewhere.
    			table = new DataTable();
	   			table.setTableID(TableID);
    			warning_count = readJSONUsingJackson ( InputFile_full, table,
    				ArrayName, appendArrays, excludeNames,
    				arrayColumns, booleanColumns, dateTimeColumns, doubleColumns, integerColumns, textColumns,
    				top, status, warning_count);
    		}
    		else {
    			// Use GSON.
    			table = readJSONUsingGson ( InputFile_full, excludeNames, dateTimeColumns, doubleColumns, integerColumns, textColumns, top );
    		}
            // Set the table identifier...
            table.setTableID ( TableID );
    	}
    	catch ( Exception e ) {
    		Message.printWarning ( 3, routine, e );
    		message = "Unexpected error reading table from JSON file \"" + InputFile_full + "\" (" + e + ").";
    		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the file exists and is readable." ) );
    		throw new CommandWarningException ( message );
    	}
    	
    	if ( warning_count > 0 ) {
    		message = "There were " + warning_count + " warnings processing the command.";
    		Message.printWarning ( warning_level,
    			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
    		throw new CommandWarningException ( message );
    	}
        
        // Set the table in the processor...

        PropList request_params = new PropList ( "" );
        request_params.setUsingObject ( "Table", table );
        try {
            processor.processRequest( "SetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting SetTable(Table=...) from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Create an empty table and set the ID
        DataTable table = new DataTable();
        table.setTableID ( TableID );
        setDiscoveryTable ( table );
    }

    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __table = table;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TableID = props.getValue( "TableID" );
	String InputFile = props.getValue( "InputFile" );
	String ArrayName = props.getValue( "ArrayName" );
	String AppendArrays = props.getValue( "AppendArrays" );
	String ExcludeNames = props.getValue("ExcludeNames");
	String ArrayColumns = props.getValue("ArrayColumns");
	String BooleanColumns = props.getValue("BooleanColumns");
	String DateTimeColumns = props.getValue("DateTimeColumns");
	String DoubleColumns = props.getValue("DoubleColumns");
	String IntegerColumns = props.getValue("IntegerColumns");
	String TextColumns = props.getValue("TextColumns");
	String Top = props.getValue("Top");
	StringBuffer b = new StringBuffer ();
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
	if ( (ArrayName != null) && (ArrayName.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ArrayName=\"" + ArrayName + "\"" );
	}
	if ( (AppendArrays != null) && (AppendArrays.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AppendArrays=" + AppendArrays );
	}
	if ( (ExcludeNames != null) && (ExcludeNames.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ExcludeNames=\"" + ExcludeNames + "\"" );
	}
	if ( (ArrayColumns != null) && (ArrayColumns.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ArrayColumns=\"" + ArrayColumns + "\"" );
	}
	if ( (BooleanColumns != null) && (BooleanColumns.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "BooleanColumns=\"" + BooleanColumns + "\"" );
	}
	if ( (DateTimeColumns != null) && (DateTimeColumns.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DateTimeColumns=\"" + DateTimeColumns + "\"" );
	}
	if ( (DoubleColumns != null) && (DoubleColumns.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DoubleColumns=\"" + DoubleColumns + "\"" );
	}
	if ( (IntegerColumns != null) && (IntegerColumns.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IntegerColumns=\"" + IntegerColumns + "\"" );
	}
	if ( (TextColumns != null) && (TextColumns.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TextColumns=\"" + TextColumns + "\"" );
	}
	if ( (Top != null) && (Top.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Top=" + Top );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
