// AppendTable_Command - This class initializes, checks, and runs the AppendTable() command.

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
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;

/**
This class initializes, checks, and runs the AppendTable() command.
*/
public class AppendTable_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public AppendTable_Command () {
	super();
	setCommandName ( "AppendTable" );
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
    String AppendTableID = parameters.getValue ( "AppendTableID" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }
    
    if ( (AppendTableID == null) || (AppendTableID.length() == 0) ) {
        message = "The table identifier from which to append must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier from which to append." ) );
    }
    
	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(7);
    validList.add ( "TableID" );
    validList.add ( "AppendTableID" );
    validList.add ( "IncludeColumns" );
    validList.add ( "ColumnMap" );
    validList.add ( "ColumnData" );
    validList.add ( "ColumnFilters" );
    validList.add ( "AppendRowNumbers" );
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
	return (new AppendTable_JDialog ( parent, this, tableIDChoices )).ok();
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
	String routine = getClass().getSimpleName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;

	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
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

	// Make sure there are time series available to operate on.
	
	PropList parameters = getCommandParameters();

    String TableID = parameters.getValue ( "TableID" );
    if ( (TableID != null) && !TableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) && TableID.indexOf("${") >= 0 ) {
   		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String AppendTableID = parameters.getValue ( "AppendTableID" );
    if ( (AppendTableID != null) && !AppendTableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) && AppendTableID.indexOf("${") >= 0 ) {
    	AppendTableID = TSCommandProcessorUtil.expandParameterValue(processor, this, AppendTableID);
    }
    String IncludeColumns = parameters.getValue ( "IncludeColumns" );
    if ( (IncludeColumns != null) && !IncludeColumns.isEmpty() && (commandPhase == CommandPhaseType.RUN) && IncludeColumns.indexOf("${") >= 0 ) {
   		IncludeColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, IncludeColumns);
    }
    String [] includeColumns = null;
    if ( (IncludeColumns != null) && !IncludeColumns.equals("") ) {
        includeColumns = IncludeColumns.split(",");
        for ( int i = 0; i < includeColumns.length; i++ ) {
            includeColumns[i] = includeColumns[i].trim();
        }
    }
    String ColumnMap = parameters.getValue ( "ColumnMap" );
    if ( (ColumnMap != null) && !ColumnMap.isEmpty() && (commandPhase == CommandPhaseType.RUN) && ColumnMap.indexOf("${") >= 0 ) {
   		ColumnMap = TSCommandProcessorUtil.expandParameterValue(processor, this, ColumnMap);
    }
    Hashtable<String,String> columnMap = new Hashtable<String,String>();
    if ( (ColumnMap != null) && (ColumnMap.length() > 0) && (ColumnMap.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(ColumnMap, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            columnMap.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String ColumnData = parameters.getValue ( "ColumnData" );
    if ( (ColumnData != null) && !ColumnData.isEmpty() && (commandPhase == CommandPhaseType.RUN) && ColumnData.indexOf("${") >= 0 ) {
   		ColumnData = TSCommandProcessorUtil.expandParameterValue(processor, this, ColumnData);
    }
    Hashtable<String,String> columnData = new Hashtable<String,String>();
    if ( (ColumnData != null) && (ColumnData.length() > 0) && (ColumnData.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(ColumnData, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            columnData.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String ColumnFilters = parameters.getValue ( "ColumnFilters" );
    if ( (ColumnFilters != null) && !ColumnFilters.isEmpty() && (commandPhase == CommandPhaseType.RUN) && ColumnFilters.indexOf("${") >= 0 ) {
   		ColumnFilters = TSCommandProcessorUtil.expandParameterValue(processor, this, ColumnFilters);
    }
    Hashtable<String,String> columnFilters = new Hashtable<String,String>();
    if ( (ColumnFilters != null) && (ColumnFilters.length() > 0) && (ColumnFilters.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(ColumnFilters, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            columnFilters.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String AppendRowNumbers = parameters.getValue ( "AppendRowNumbers" );
   	AppendRowNumbers = TSCommandProcessorUtil.expandParameterValue(processor, this, AppendRowNumbers);
    String [] appendRowNumbers = new String[0];
    if ( (AppendRowNumbers != null) && !AppendRowNumbers.isEmpty() ) {
    	appendRowNumbers = AppendRowNumbers.split(",");
	    for ( int i = 0; i < appendRowNumbers.length; i++ ) {
	    	appendRowNumbers[i] = appendRowNumbers[i].trim();
	    }
    }
    
    // Get the tables to process.

    DataTable table = null;
    PropList request_params = null;
    CommandProcessorRequestResultsBean bean = null;
    if ( (TableID != null) && !TableID.equals("") ) {
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
    
    DataTable appendTable = null;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be updated.
        request_params = new PropList ( "" );
        request_params.set ( "TableID", AppendTableID );
        try {
            bean = processor.processRequest( "GetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTable(TableID=\"" + AppendTableID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table == null ) {
            message = "Unable to find table to process using AppendTableID=\"" + AppendTableID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a table exists with the requested ID." ) );
        }
        else {
            appendTable = (DataTable)o_Table;
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
    	// Append to the table

		// Convert the row numbers from strings to integers.
		int [] appendRowNumbersArray = null;
		if ( appendRowNumbers.length > 0 ) {
			appendRowNumbersArray = new int[appendRowNumbers.length];
			int i = -1;
			for ( String s : appendRowNumbers ) {
				++i;
				// Expand the individual row number as a string.
				s = TSCommandProcessorUtil.expandParameterValue(processor, this, s);
				if ( s.equalsIgnoreCase("last") ) {
					// Set the row number to the table row count.
					s = "" + appendTable.getNumberOfRecords();
				}
				if ( StringUtil.isInteger(s) ) {
					appendRowNumbersArray[i] = Integer.parseInt(s);
				}
				else {
					appendRowNumbersArray[i] = -1;
				}
			}
		}
		
		// Append the table.
        int rowsAppended = table.appendTable ( table, appendTable, includeColumns, columnMap, columnData, columnFilters,
        	appendRowNumbersArray );
        Message.printStatus(2, routine, "Appended " + rowsAppended + " rows.");
    }
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error appending table (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "See the log file for details." ) );
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
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TableID",
    	"AppendTableID",
		"IncludeColumns",
		"ColumnMap",
		"ColumnData",
		"ColumnFilters",
		"AppendRowNumbers"
	};
	return this.toString(parameters, parameterOrder);
}

}