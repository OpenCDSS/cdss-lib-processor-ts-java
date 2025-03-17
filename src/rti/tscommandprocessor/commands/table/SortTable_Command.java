// SortTable_Command - This class initializes, checks, and runs the SortTable() command.

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringDictionary;
import RTi.Util.Table.DataTable;

/**
This class initializes, checks, and runs the SortTable() command.
*/
public class SortTable_Command extends AbstractCommand implements Command
{

/**
Values for SortOrder parameter.
*/
protected final String _Ascending = "Ascending";
protected final String _Descending = "Descending";

/**
Columns to sort, initialized in checkCommandParameters().
*/
private String [] sortColumns = new String[0];

/**
Constructor.
*/
public SortTable_Command () {
	super();
	setCommandName ( "SortTable" );
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
    String SortColumns = parameters.getValue ( "SortColumns" );
    String SortOrder = parameters.getValue ( "SortOrder" );
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

    if ( (SortColumns != null) && !SortColumns.isEmpty() ) {
    	this.sortColumns = SortColumns.split(",");
    	for ( int i = 0; i < this.sortColumns.length; i++ ) {
    		this.sortColumns[i] = this.sortColumns[i].trim();
    	}
    	StringDictionary sortOrder = new StringDictionary(SortOrder,":",",");
    	LinkedHashMap<String,String> map = sortOrder.getLinkedHashMap();
    	Set<String> set = map.keySet();
    	for ( String s : set ) {
    		// Look for column in the sort columns list.
    		boolean found = false;
    		for ( int i = 0; i < this.sortColumns.length; i++ ) {
    			if ( s.equalsIgnoreCase(sortColumns[i]) ) {
    				found = true;
    				break;
    			}
    		}
    		if ( !found ) {
    			message = "Column \"" + s + "\" specified with sort order is not included in SortColumns.";
    	        warning += "\n" + message;
    	        status.addToLog ( CommandPhaseType.INITIALIZATION,
    	            new CommandLogRecord(CommandStatusType.FAILURE,
    	                message, "Only specify sort order for columns that are being sorted." ) );
    		}
    		String order = map.get(s);
    		if ( !order.equalsIgnoreCase(_Ascending) && !order.equalsIgnoreCase(_Descending)) {
    	        message = "The sort order (" + order + ") for sort column (" + s + ") is invalid.";
    	        warning += "\n" + message;
    	        status.addToLog ( CommandPhaseType.INITIALIZATION,
    	            new CommandLogRecord(CommandStatusType.FAILURE,
    	                message, "Specify the sort order as " + _Ascending + " or " + _Descending + ".") );
    		}
    	}
    }

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(4);
    validList.add ( "TableID" );
    validList.add ( "SortColumns" );
    validList.add ( "SortOrder" );
    validList.add ( "OrderColumns" );
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
	return (new SortTable_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Parse the command.  Need to handle legacy SortOrder that does not use a dictionary.
@param commandString the string representation of the command
*/
public void parseCommand ( String commandString )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
	super.parseCommand(commandString);
	// Check for SortOrder that does not have the dictionary ":" delimiter.
	// If found, replace with the new syntax on the single column to be sorted.
	PropList props = getCommandParameters();
	String propValue = props.getValue("SortOrder");
	if ( propValue != null ) {
		if ( propValue.indexOf(":") < 0 ) {
			// Does not use the dictionary notation so set to new syntax.
			String col = props.getValue("SortColumns");
			props.set("SortOrder",col + ":" + propValue);
		}
	}
}

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
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // Default.
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

	// Make sure there are time series available to operate on.

	PropList parameters = getCommandParameters();

    String TableID = parameters.getValue ( "TableID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	// In discovery mode want lists of tables to include ${Property}.
    	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String SortOrder = parameters.getValue ( "SortOrder" );
    StringDictionary sortOrder = new StringDictionary(SortOrder,":",",");
    // Array to hold sort order for the sorted columns:
    // 1 = ascending (default)
    // -1 = descending
	int [] sortOrderArray = new int[sortColumns.length];
	for ( int i = 0; i < sortColumns.length; i++ ) {
		sortOrderArray[i] = 1; // Default (1=Ascending).
		Object o = sortOrder.get(sortColumns[i]);
		if ( o != null ) {
			String s = (String)o;
			if ( s.equalsIgnoreCase(_Descending) ) {
				sortOrderArray[i] = -1;
			}
		}
	}

    String OrderColumns = parameters.getValue ( "OrderColumns" );
    String orderColumns[] = new String[0];
    if ( commandPhase == CommandPhaseType.RUN ) {
    	// In discovery mode want lists of tables to include ${Property}.
    	OrderColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, OrderColumns);
    }
    if ( (OrderColumns != null) && !OrderColumns.isEmpty() ) {
    	if ( OrderColumns.contains(",") ) {
    		String [] parts = OrderColumns.split(",");
   			orderColumns = new String[parts.length];
    		for ( int i = 0; i < parts.length; i++ ) {
    			orderColumns[i] = parts[i].trim();
    		}
    	}
    	else {
    		orderColumns = new String[1];
    		orderColumns[0] = OrderColumns.trim();
    	}
    }

    // Get the table to process.

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

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	try {
		// Sort the table rows.
		if ( sortColumns.length > 0 ) {
			table.sortTable ( sortColumns, sortOrderArray );
		}

		// Order the table columns.
		if ( orderColumns.length > 0 ) {
			// Check that columns exist.
			for ( String columnName : orderColumns ) {
				int columnNumber = -1;
				try {
					columnNumber = table.getFieldIndex(columnName);
				}
				catch ( Exception e ) {
					columnNumber = -1;
				}
				if ( columnNumber < 0 ) {
					message = "The table does not have column \"" + columnName + "\" - will ignore when ordering columns.";
					Message.printWarning ( warning_level,
							MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
					status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
						message, "Verify that column \"" + columnName + "\" exists in the table." ) );
				}
			}
			List<String> problems = new ArrayList<>();
			table.reorderFields ( orderColumns, problems );
			int problemCount = 0;
			for ( String problem : problems ) {
				++problemCount;
				message = problem;
				Message.printWarning ( warning_level,
					MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
				status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
					message, "See the log file." ) );
				if ( problemCount == 500 ) {
					message = "Limiting output to 50 problems.";
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
					status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
					problem, "See the log file." ) );
				}
			}
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error sorting the table (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
		throw new CommandWarningException ( message );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		throw new CommandWarningException ( message );
	}

    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TableID",
		"SortColumns",
		"SortOrder",
		"OrderColumns"
	};
	return this.toString(parameters, parameterOrder);
}

}