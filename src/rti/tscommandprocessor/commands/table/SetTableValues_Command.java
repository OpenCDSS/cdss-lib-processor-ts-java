// SetTableValues_Command - This class initializes, checks, and runs the SetTableValues() command.

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
import java.util.LinkedHashMap;
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
import RTi.Util.Table.DataTableValueStringProvider;

/**
This class initializes, checks, and runs the SetTableValues() command.
*/
public class SetTableValues_Command extends AbstractCommand implements Command
{
    
/**
Constructor.
*/
public SetTableValues_Command ()
{	super();
	setCommandName ( "SetTableValues" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String TableID = parameters.getValue ( "TableID" );
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
 
	// Check for invalid parameters...
	List<String> validList = new ArrayList<>(5);
    validList.add ( "TableID" );
    validList.add ( "ColumnFilters" );
    validList.add ( "ColumnValues" );
    validList.add ( "Column" );
    validList.add ( "Value" );
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
{	List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed...
	return (new SetTableValues_JDialog ( parent, this, tableIDChoices )).ok();
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;

	PropList parameters = getCommandParameters();
	final CommandProcessor processor = getCommandProcessor();
    Boolean clearStatus = Boolean.TRUE; // Default.
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
		status.clearLog(CommandPhaseType.RUN);
	}

    String TableID = parameters.getValue ( "TableID" );
    if ( (TableID != null) && !TableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) && TableID.indexOf("${") >= 0 ) {
   		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String ColumnFilters = parameters.getValue ( "ColumnFilters" );
    if ( (ColumnFilters != null) && !ColumnFilters.isEmpty() && (commandPhase == CommandPhaseType.RUN) && ColumnFilters.indexOf("${") >= 0 ) {
    	ColumnFilters = TSCommandProcessorUtil.expandParameterValue(processor, this, ColumnFilters);
    }
    Hashtable<String,String> columnFilters = new Hashtable<String,String>();
    Message.printStatus(2, routine, "Processing ColumnFilters \"" + ColumnFilters + "\"");
    if ( (ColumnFilters != null) && (ColumnFilters.length() > 0) && (ColumnFilters.indexOf(":") > 0) ) {
        // First break map pairs by comma
        List<String>pairs = StringUtil.breakStringList(ColumnFilters, ",", 0 );
        // Now break pairs and put in hashtable.  Right side could contain colon if dynamic data so split manually
        for ( String pair : pairs ) {
        	int pos = pair.indexOf(":");
        	if ( pos > 0 ) {
        		columnFilters.put(pair.substring(0,pos).trim(), pair.substring(pos + 1).trim() );
        	}
        }
    }
    String ColumnValues = parameters.getValue ( "ColumnValues" );
    if ( (ColumnValues != null) && !ColumnValues.isEmpty() && (commandPhase == CommandPhaseType.RUN) && ColumnValues.indexOf("${") >= 0 ) {
    	ColumnValues = TSCommandProcessorUtil.expandParameterValue(processor, this, ColumnValues);
    }
    // Used LinkedHashMap because want insert order to be retained in new columns, if columns are created
    LinkedHashMap<String,String> columnValues = new LinkedHashMap<String,String>();
    if ( (ColumnValues != null) && (ColumnValues.length() > 0) && (ColumnValues.indexOf(":") > 0) ) {
        // First break map pairs by comma
        List<String>pairs = StringUtil.breakStringList(ColumnValues, ",", 0 );
        // Now break pairs and put in hashmap
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            columnValues.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String Column = parameters.getValue ( "Column" );
    if ( (Column != null) && !Column.isEmpty() && (commandPhase == CommandPhaseType.RUN) && Column.indexOf("${") >= 0 ) {
    	Column = TSCommandProcessorUtil.expandParameterValue(processor, this, Column);
    }
    String Value = parameters.getValue ( "Value" );
    if ( (Value != null) && !Value.isEmpty() && (commandPhase == CommandPhaseType.RUN) && Value.indexOf("${") >= 0 ) {
    	Value = TSCommandProcessorUtil.expandParameterValue(processor, this, Value);
    }
    
    // Get the table to process.

    DataTable table = null;
    PropList request_params = null;
    CommandProcessorRequestResultsBean bean = null;
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
    	// Set values in the table...
		final Command thisCommand = this; // for anonymous class below
		// TODO SAM 2016-08-25 Why is the DataTableValueStringProvider needed?  The ColumnValues parameter
		// is expanded above before parsing the parameter to allow ${xx:Property} to be expanded
		DataTableValueStringProvider tableValueGetter = new DataTableValueStringProvider () {
			public String getTableCellValueAsString ( String valueFormat ) {
				// The value in the table can actually contain ${Property}
				if ( (valueFormat == null) || valueFormat.isEmpty() || valueFormat.indexOf("${") < 0 ) {
					return valueFormat;
				}
				else {
					return TSCommandProcessorUtil.expandParameterValue(processor, thisCommand, valueFormat);
				}
			}
		};
		// First set using the hash
	    table.setTableValues ( columnFilters, columnValues, tableValueGetter, true );
	    // Next set using the single value
	    if ( (Column != null) && !Column.isEmpty() ) {
	    	LinkedHashMap<String,String> columnValues2 = new LinkedHashMap<String,String>();
	    	columnValues2.put(Column, Value);
	    	table.setTableValues ( columnFilters, columnValues2, tableValueGetter, true );
	    }
 	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error setting values in table (" + e + ").";
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
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TableID",
		"ColumnFilters",
		"ColumnValues",
		"Column",
		"Value"
	};
	return this.toString(parameters, parameterOrder);
}

}