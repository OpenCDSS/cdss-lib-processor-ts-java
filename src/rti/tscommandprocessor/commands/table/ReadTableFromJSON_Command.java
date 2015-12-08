package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ReadTableFromJSON() command.
*/
public class ReadTableFromJSON_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
    
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

	String working_dir = null;
	
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
			working_dir = (String)o;
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
	List validList = new ArrayList<String>(8);
    validList.add ( "TableID" );
    validList.add ( "InputFile" );
    validList.add ( "ExcludeNames" );
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
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable table = getDiscoveryTable();
    List v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector();
        v.add ( table );
    }
    return v;
}

// Use base class parseCommand()

// TODO SAM 2015-12-05 Need to evaluate how best to flatten arrays embedded in objects
/**
Read a JSON file into a table object.
*/
private DataTable readJSON ( String inputFile, String [] excludeNames,
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
private boolean readJSON_ExcludeName(String name, String [] excludeNames)
{
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
	if ( (TableID != null) && (TableID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
	}
	String InputFile = parameters.getValue ( "InputFile" );
	String ExcludeNames = parameters.getValue ( "ExcludeNames" );
	String [] excludeNames = null;
    if ( (ExcludeNames != null) && !ExcludeNames.isEmpty() ) {
    	excludeNames = ExcludeNames.split(",");
        for ( int i = 0; i < excludeNames.length; i++ ) {
        	excludeNames[i] = excludeNames[i].trim();
        }
    }
	String DateTimeColumns = parameters.getValue ( "DateTimeColumns" );
	String [] dateTimeColumns = null;
    if ( (DateTimeColumns != null) && !DateTimeColumns.isEmpty() ) {
        dateTimeColumns = DateTimeColumns.split(",");
        for ( int i = 0; i < dateTimeColumns.length; i++ ) {
            dateTimeColumns[i] = dateTimeColumns[i].trim();
        }
    }
	String DoubleColumns = parameters.getValue ( "DoubleColumns" );
	String [] doubleColumns = null;
    if ( (DoubleColumns != null) && !DoubleColumns.isEmpty() ) {
    	doubleColumns = DoubleColumns.split(",");
        for ( int i = 0; i < doubleColumns.length; i++ ) {
        	doubleColumns[i] = doubleColumns[i].trim();
        }
    }
	String IntegerColumns = parameters.getValue ( "IntegerColumns" );
	String [] integerColumns = null;
    if ( (IntegerColumns != null) && !IntegerColumns.isEmpty() ) {
    	integerColumns = IntegerColumns.split(",");
        for ( int i = 0; i < integerColumns.length; i++ ) {
        	integerColumns[i] = integerColumns[i].trim();
        }
    }
	String TextColumns = parameters.getValue ( "TextColumns" );
	String [] textColumns = null;
    if ( (TextColumns != null) && !TextColumns.isEmpty() ) {
    	textColumns = TextColumns.split(",");
        for ( int i = 0; i < textColumns.length; i++ ) {
        	textColumns[i] = textColumns[i].trim();
        }
    }
	String Top = parameters.getValue ( "Top" );
	int top = 0;
	if ( (Top != null) && !Top.isEmpty() ) {
		top = Integer.parseInt(Top);
	}

	String InputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
        	TSCommandProcessorUtil.expandParameterValue(processor, this,InputFile)) );
	if ( !IOUtil.fileExists(InputFile_full) ) {
		message += "\nThe JSON file \"" + InputFile_full + "\" does not exist.";
		++warning_count;
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that the JSON file exists." ) );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	// Now process the file...

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
    	try {
            table = readJSON ( InputFile_full, excludeNames, dateTimeColumns, doubleColumns, integerColumns, textColumns, top );
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
	String ExcludeNames = props.getValue("ExcludeNames");
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
	if ( (ExcludeNames != null) && (ExcludeNames.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ExcludeNames=\"" + ExcludeNames + "\"" );
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