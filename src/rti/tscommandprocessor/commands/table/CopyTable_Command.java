// CopyTable_Command - This class initializes, checks, and runs the CopyTable() command.

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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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
import RTi.Util.IO.PropList;
import RTi.Util.String.StringDictionary;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;

/**
This class initializes, checks, and runs the CopyTable() command.
*/
public class CopyTable_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

/**
The table that is created in discovery mode.
*/
private DataTable __discoveryTable = null;

/**
Constructor.
*/
public CopyTable_Command () {
	super();
	setCommandName ( "CopyTable" );
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
    String NewTableID = parameters.getValue ( "NewTableID" );
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

    if ( (NewTableID == null) || NewTableID.isEmpty() ) {
        message = "The new table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the new table identifier." ) );
    }

    if ( (TableID != null) && !TableID.isEmpty() && (NewTableID != null) && !NewTableID.isEmpty() &&
        TableID.equalsIgnoreCase(NewTableID) ) {
        message = "The original and new table identifiers are the same.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the new table identifier different from the original table identifier." ) );
    }

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(8);
    validList.add ( "TableID" );
    validList.add ( "NewTableID" );
    validList.add ( "DistinctColumns" );
    validList.add ( "IncludeColumns" );
    validList.add ( "ColumnMap" );
    validList.add ( "ColumnFilters" );
    validList.add ( "ColumnExcludeFilters" );
    validList.add ( "RowCountProperty" );
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
	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed.
	return (new CopyTable_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable() {
    return __discoveryTable;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
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

// Use base class parseCommand()

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
	int log_level = 3; // Level for non-user messages for log file.
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
		status.clearLog(CommandPhaseType.RUN);
	}
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

	PropList parameters = getCommandParameters();

    String TableID = parameters.getValue ( "TableID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String NewTableID = parameters.getValue ( "NewTableID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	NewTableID = TSCommandProcessorUtil.expandParameterValue(processor, this, NewTableID);
    }
    String IncludeColumns = parameters.getValue ( "IncludeColumns" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	IncludeColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, IncludeColumns);
    }
    String [] includeColumns = null;
    if ( (commandPhase == CommandPhaseType.RUN) && (IncludeColumns != null) && !IncludeColumns.isEmpty() ) {
        includeColumns = IncludeColumns.split(",");
        for ( int i = 0; i < includeColumns.length; i++ ) {
            includeColumns[i] = includeColumns[i].trim();
        }
    }
    String DistinctColumns = parameters.getValue ( "DistinctColumns" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	DistinctColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, DistinctColumns);
    }
    String [] distinctColumns = null;
    if ( (commandPhase == CommandPhaseType.RUN) && (DistinctColumns != null) && !DistinctColumns.isEmpty() ) {
        distinctColumns = DistinctColumns.split(",");
        for ( int i = 0; i < distinctColumns.length; i++ ) {
            distinctColumns[i] = distinctColumns[i].trim();
        }
    }
    String ColumnMap = parameters.getValue ( "ColumnMap" );
    Hashtable<String,String> columnMap = new Hashtable<>();
    if ( (ColumnMap != null) && (ColumnMap.length() > 0) && (ColumnMap.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(ColumnMap, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            columnMap.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String ColumnFilters = parameters.getValue ( "ColumnFilters" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	ColumnFilters = TSCommandProcessorUtil.expandParameterValue(processor, this, ColumnFilters);
    }
    Hashtable<String,String> columnFilters = new Hashtable<>();
    if ( (ColumnFilters != null) && (ColumnFilters.length() > 0) && (ColumnFilters.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(ColumnFilters, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            columnFilters.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String ColumnExcludeFilters = parameters.getValue ( "ColumnExcludeFilters" );
    StringDictionary columnExcludeFilters = new StringDictionary(ColumnExcludeFilters,":",",");
    String RowCountProperty = parameters.getValue ( "RowCountProperty" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	RowCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, RowCountProperty);
    }

    // Get the table to process.

    DataTable table = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
        PropList request_params = null;
        CommandProcessorRequestResultsBean bean = null;
        if ( (TableID != null) && !TableID.isEmpty() ) {
            // Get the table to be updated.
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
    	// Copy the table.

	    DataTable newTable = null;
	    if ( commandPhase == CommandPhaseType.RUN ) {
	        newTable = table.createCopy ( table, NewTableID, includeColumns,
	            distinctColumns, columnMap, columnFilters, columnExcludeFilters );

            // Set the table in the processor.

            PropList request_params = new PropList ( "" );
            request_params.setUsingObject ( "Table", newTable );
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
            newTable = new DataTable();
            newTable.setTableID ( NewTableID );
            setDiscoveryTable ( newTable );
        }
	    // Set the property indicating the number of rows in the table.
        if ( (RowCountProperty != null) && !RowCountProperty.isEmpty() ) {
            int rowCount = 0;
            if ( newTable != null ) {
                rowCount = newTable.getNumberOfRecords();
            }
            PropList request_params = new PropList ( "" );
            request_params.setUsingObject ( "PropertyName", RowCountProperty );
            request_params.setUsingObject ( "PropertyValue", Integer.valueOf(rowCount) );
            try {
                processor.processRequest( "SetProperty", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting SetProperty(Property=\"" + RowCountProperty + "\") from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error copying table (" + e + ").";
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
*/
private void setDiscoveryTable ( DataTable table ) {
    __discoveryTable = table;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TableID",
    	"NewTableID",
		"IncludeColumns",
    	"DistinctColumns",
		"ColumnMap",
		"ColumnFilters",
		"ColumnExcludeFilters",
		"RowCountProperty"
	};
	return this.toString(parameters, parameterOrder);
}

}