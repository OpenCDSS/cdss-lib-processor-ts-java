// NewTable_Command - This class initializes, checks, and runs the NewTable() command.

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

package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;

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
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;

/**
This class initializes, checks, and runs the NewTable() command.
*/
public class NewTable_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
The table that is created.
*/
private DataTable __table = null;

/**
Constructor.
*/
public NewTable_Command () {
	super();
	setCommandName ( "NewTable" );
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
	String TableID = parameters.getValue ( "TableID" );
    String Columns = parameters.getValue ( "Columns" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (TableID == null) || TableID.isEmpty() ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }

    String columnName = null;
    String columnType = null;
	if ( (Columns != null) && !Columns.isEmpty() ) {
        // Check column definitions (Column name,Type;...).
		List<String> v = StringUtil.breakStringList ( Columns, ",;", 0 );
		if ( v == null ) {
            message = "One or more columns must be specified";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the columns for the table as Name,Type;Name,Type;..." ) );
		}
		else {
			// Get data type choices without note.
		    List<String>dataTypeChoices = TableField.getDataTypeChoices(false);
		    List<String>dataTypeArrayChoices = new ArrayList<>();
		    for ( String dataType : dataTypeChoices ) {
		    	dataTypeArrayChoices.add("[" + dataType + "]");
		    }
		    int size = v.size();
		    if ( (size %2) != 0 ) {
                message = "Column data are not specified in pairs.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the columns for the table as Name,Type;Name,Type;..." ) );
		    }
		    else {
    			for ( int i = 0; i < size; i++ ) {
    				columnName = v.get(i).trim();
    				columnType = v.get(++i).trim(); // Increment i to handle pairs.
    				// Make sure that the data type is recognized:
    				// - check simple type and [type]
    				int posType = StringUtil.indexOfIgnoreCase(dataTypeChoices, columnType);
    				int posArrayType = StringUtil.indexOfIgnoreCase(dataTypeArrayChoices, columnType);
    				if ( (posType < 0) && (posArrayType < 0) ) {
                        message = "Column \"" + columnName + "\" type (" + columnType + ") is invalid";
    					warning += "\n" + message;
                        status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the column type as boolean, datetime, double, float, integer, long, short, or string, or use [ ] around type to indicate array." ) );
    				}
    			}
		    }
		}
	}

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(2);
    validList.add ( "TableID" );
    validList.add ( "Columns" );
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
	return (new NewTable_JDialog ( parent, this )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
@return the table that is read by this class when run in discovery mode
*/
private DataTable getDiscoveryTable() {
    return this.__table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
Classes that can be requested:  DataTable
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    DataTable table = getDiscoveryTable();
    List<T> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new ArrayList<>();
        v.add ( (T)table );
    }
    return v;
}

// Use base class parseCommand().

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
throws InvalidCommandParameterException,
CommandWarningException, CommandException {
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
        setDiscoveryTable ( null );
    }

	// Make sure there are time series available to operate on.

	PropList parameters = getCommandParameters();

    String TableID = parameters.getValue ( "TableID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }

    String [] columnNames = null;
    String [] columnTypes = null;
    String Columns = parameters.getValue ( "Columns" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		Columns = TSCommandProcessorUtil.expandParameterValue(processor, this, Columns);
    }
    // This code is similar to checkCommandParamaters but evaluates at runtime to handle properties.
	if ( (Columns != null) && !Columns.isEmpty() ) {
        // Check column definitions (Column name,Type;...).
		List<String> v = StringUtil.breakStringList ( Columns, ",;", 0 );
		if ( v == null ) {
            message = "One or more columns must be specified";
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the columns for the table as Name,Type;Name,Type;..." ) );
		}
		else {
			// Get data type choices without note.
		    List<String>dataTypeChoices = TableField.getDataTypeChoices(false);
		    List<String>dataTypeArrayChoices = new ArrayList<>();
		    for ( String dataType : dataTypeChoices ) {
		    	dataTypeArrayChoices.add("[" + dataType + "]");
		    }
		    int size = v.size();
		    if ( (size %2) != 0 ) {
                message = "Column data are not specified in pairs.";
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the columns for the table as Name,Type;Name,Type;..." ) );
		    }
		    else {
    			columnNames = new String[size/2]; // Data in pairs.
    			columnTypes = new String[size/2];
    			columnNames = new String[size/2]; // Data in pairs.
    			columnTypes = new String[size/2];
    			int columnCount = -1; // Will increment to zero on first loop below.
    			for ( int i = 0; i < size; i++ ) {
    			    ++columnCount;
    				columnNames[columnCount] = v.get(i).trim();
    				columnTypes[columnCount] = v.get(++i).trim(); // Increment i to handle pairs.
    				// Make sure that the data type is recognized:
    				// - check simple type and [type]
    				int posType = StringUtil.indexOfIgnoreCase(dataTypeChoices, columnTypes[columnCount]);
    				int posArrayType = StringUtil.indexOfIgnoreCase(dataTypeArrayChoices, columnTypes[columnCount]);
    				if ( (posType < 0) && (posArrayType < 0) ) {
                        message = "Column \"" + columnNames[columnCount] + "\" type (" +
                        columnTypes[columnCount] + ") is invalid";
                        status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the column type as boolean, datetime, double, float, integer, long, short, or string, or use [ ] around type to indicate array." ) );
    				}
    			}
		    }
		}
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	try {
    	// Create the table.

	    List<TableField> columnList = new ArrayList<>();
	    DataTable table = null;

        if ( commandPhase == CommandPhaseType.RUN ) {
            // Create the table with column data that was created in checkCommandParameters().
            if ( columnNames != null ) {
                for ( int i = 0; i < columnNames.length; i++ ) {
                	if ( columnTypes[i].charAt(0) == '[' ) {
                		// Column type is an array, which will be the main type:
                		// - also set the precision based on the type in the array
                		String arrayType = columnTypes[i].substring(1,(columnTypes[i].length() - 1));
                		if ( (TableField.lookupDataType(arrayType) == TableField.DATA_TYPE_DOUBLE) ||
                        	(TableField.lookupDataType(arrayType) == TableField.DATA_TYPE_FLOAT) ) {
                        	// Set the precision to 2 (width to 12), which should be reasonable for many data types:
                			// - since an array, the data type includes a base offset
                        	columnList.add ( new TableField((TableField.DATA_TYPE_ARRAY + TableField.lookupDataType(arrayType)), columnNames[i], 12, 2) );
                    	}
                    	else {
                        	// No precision is necessary and specify the field width as -1 meaning it can grow:
                			// - since an array, the data type includes a base offset
                        	columnList.add ( new TableField((TableField.DATA_TYPE_ARRAY + TableField.lookupDataType(arrayType)), columnNames[i], -1) );
                    	}
                	}
                	else if ( (TableField.lookupDataType(columnTypes[i]) == TableField.DATA_TYPE_DOUBLE) ||
                        (TableField.lookupDataType(columnTypes[i]) == TableField.DATA_TYPE_FLOAT) ) {
                        // Set the precision to 2 (width to 12), which should be reasonable for many data types.
                        columnList.add ( new TableField(TableField.lookupDataType(columnTypes[i]), columnNames[i], 12, 2) );
                    }
                    else {
                        // No precision is necessary and specify the field width as -1 meaning it can grow.
                        columnList.add ( new TableField(TableField.lookupDataType(columnTypes[i]), columnNames[i], -1) );
                    }
                }
            }
            table = new DataTable( columnList );
            table.setTableID ( TableID );

            // Set the table in the processor.

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
            // Create an empty table and set the ID.
            table = new DataTable();
            table.setTableID ( TableID );
            setDiscoveryTable ( table );
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error creating new table (" + e + ").";
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
Set the table that is read by this class in discovery mode.
@param table discovery table with TableID set
*/
private void setDiscoveryTable ( DataTable table ) {
    this.__table = table;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TableID",
		"Columns"
	};
	return this.toString(parameters, parameterOrder);
}

}