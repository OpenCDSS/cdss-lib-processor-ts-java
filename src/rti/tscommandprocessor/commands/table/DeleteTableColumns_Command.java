// DeleteTableColumns_Command - This class initializes, checks, and runs the DeleteTableColumns() command.

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

package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
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
import RTi.Util.Table.DataTable;

/**
This class initializes, checks, and runs the DeleteTableColumns() command.
*/
public class DeleteTableColumns_Command extends AbstractCommand implements Command
{
    
/**
Constructor.
*/
public DeleteTableColumns_Command ()
{	super();
	setCommandName ( "DeleteTableColumns" );
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
    String DeleteColumns = parameters.getValue ( "DeleteColumns" );
    //String DeleteCountProperty = parameters.getValue ( "DeleteCountProperty" );
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
    if ( (DeleteColumns == null) || (DeleteColumns.length() == 0) ) {
        message = "The columns to delete must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the columns to delete." ) );
    }
 
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(3);
    validList.add ( "TableID" );
    validList.add ( "KeepColumns" );
    validList.add ( "DeleteColumns" );
    //validList.add ( "DeleteCountProperty" );
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
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed...
	return (new DeleteTableColumns_JDialog ( parent, this, tableIDChoices )).ok();
}

// Use base class parseCommand()

// TODO smalers could put this in a utility class
/**
 * Determine which columns to delete.
 * Delete columns will not be added if in KeepColumns.
 * Comparisons are case-insensitive.
 * @param table DataTable to process, used to confirm that requested columns exist in the table.
 * @param KeepColumns a comma-separated list of columns to keep, * allowed.
 * @param DeleteColumns a comma-separated list of columns to delete, * allowed.
 * @param problems list of problem strings
 */
private String [] getDeleteColumns(DataTable table, String KeepColumns, String DeleteColumns, List<String> problems ) {
	// Get column names from the table
	String [] columnNames = table.getFieldNames();
    // Determine the list of columns to delete by considering the KeepColumns and DeleteColumns parameters
    // The first pass is simply to split the column names, which may contain * wildcard
    String [] keepColumns0 = new String[0];
    if ( (KeepColumns != null) && !KeepColumns.isEmpty() ) {
    	keepColumns0 = KeepColumns.split(",");
	    for ( int i = 0; i < keepColumns0.length; i++ ) {
	    	// Trim and convert to a Java regular expression
	    	keepColumns0[i] = keepColumns0[i].trim().replace("*",".*");
	    }
    }
    // The second pass is to get a list of columns to keep based on wildcard match
    // - do not allow duplicates
    List<String> keepColumns = new ArrayList<>();
    for ( int iField = 0; iField < columnNames.length; iField ++ ) {
    	for ( int iKeepColumn = 0; iKeepColumn < keepColumns0.length; iKeepColumn++ ) {
			// Make sure that the keep column exists in the table
    		if ( keepColumns0[iKeepColumn].indexOf("*") < 0 ) {
    			try {
    				table.getFieldIndex(keepColumns0[iKeepColumn]);
    			}
    			catch ( Exception e ) {
    				problems.add("KeepColumn \"" + keepColumns0[iKeepColumn] + "\" is not found in the table.");
    				continue;
    			}
    		}
    		if ( columnNames[iField].matches(keepColumns0[iKeepColumn]) ) {
    			// Want to add the column to keep list if not already in the list
    			boolean found = false;
    			for ( String keepColumn : keepColumns ) {
    				if ( keepColumn.equalsIgnoreCase(columnNames[iField]) ) {
    					found = true;
    					break;
    				}
    			}
    			if ( !found ) {
    				keepColumns.add(columnNames[iField]);
    				break;
    			}
    		}
    	}
    }
    // The first pass is simply to split the column names, which may contain * wildcard
    String [] deleteColumns0 = new String[0];
    if ( (DeleteColumns != null) && !DeleteColumns.isEmpty() ) {
    	deleteColumns0 = DeleteColumns.split(",");
	    for ( int i = 0; i < deleteColumns0.length; i++ ) {
	    	// Trim and convert to a Java regular expression
	    	deleteColumns0[i] = deleteColumns0[i].trim().replace("*",".*");
	    }
    }
    // The second pass is to get a list of columns to delete based on wildcard match
    // - do not allow duplicates
    List<String> deleteColumns = new ArrayList<>();
    for ( int iField = 0; iField < columnNames.length; iField ++ ) {
    	for ( int iDeleteColumn = 0; iDeleteColumn < deleteColumns0.length; iDeleteColumn++ ) {
			// Make sure that the specific delete column exists in the table
    		if ( deleteColumns0[iDeleteColumn].indexOf("*") < 0 ) {
    			try {
    				table.getFieldIndex(deleteColumns0[iDeleteColumn]);
    			}
    			catch ( Exception e ) {
    			problems.add("DeleteColumn \"" + deleteColumns0[iDeleteColumn] + "\" is not found in the table.");
    				continue;
    			}
    		}
    		if ( columnNames[iField].matches(deleteColumns0[iDeleteColumn]) ) {
    			// Want to add the column to delete list if not already in the list
    			boolean found = false;
    			for ( String deleteColumn : deleteColumns ) {
    				if ( deleteColumn.equalsIgnoreCase(columnNames[iField]) ) {
    					found = true;
    					break;
    				}
    			}
    			if ( !found ) {
    				deleteColumns.add(columnNames[iField]);
    				break;
    			}
    		}
    	}
    }
    // Finally, only include in the delete list if not in the keep list
    List<String> deleteColumns2 = new ArrayList<>();
    for ( String deleteColumn : deleteColumns ) {
    	boolean found = false;
    	for ( String keepColumn : keepColumns ) {
    		if ( deleteColumn.equalsIgnoreCase(keepColumn) ) {
    			found = true;
    			break;
    		}
    	}
    	if ( !found ) {
    		deleteColumns2.add(deleteColumn);
    	}
    }
    /* for debugging...
   	for ( String keepColumn : keepColumns ) {
   		Message.printStatus(2,"","Keep column: \"" + keepColumn + "\"" );
   	}
   	for ( String deleteColumn : deleteColumns ) {
   		Message.printStatus(2,"","Delete column: \"" + deleteColumn + "\"" );
   	}
   	for ( String deleteColumn : deleteColumns2 ) {
   		Message.printStatus(2,"","Final delete column: \"" + deleteColumn + "\"" );
   	}
   	*/
    return StringUtil.toArray(deleteColumns2);
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
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
	CommandProcessor processor = getCommandProcessor();
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
		status.clearLog(CommandPhaseType.RUN);
	}

    String TableID = parameters.getValue ( "TableID" );
    if ( (TableID != null) && !TableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) && TableID.indexOf("${") >= 0 ) {
   		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String KeepColumns = parameters.getValue ( "KeepColumns" );
    if ( (KeepColumns != null) && !KeepColumns.isEmpty() && (commandPhase == CommandPhaseType.RUN) && KeepColumns.indexOf("${") >= 0 ) {
    	KeepColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, KeepColumns);
    }
    String DeleteColumns = parameters.getValue ( "DeleteColumns" );
    if ( (DeleteColumns != null) && !DeleteColumns.isEmpty() && (commandPhase == CommandPhaseType.RUN) && DeleteColumns.indexOf("${") >= 0 ) {
    	DeleteColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, DeleteColumns);
    }
    //String DeleteCountProperty = parameters.getValue ( "DeleteCountProperty" );
    
    // Get the table to process.
    DataTable table = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
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
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	try {
    	// Delete columns...
		List<String> problems = new ArrayList<>();
		String [] deleteColumns = getDeleteColumns(table, KeepColumns, DeleteColumns, problems );
	    for ( String p : problems ) {
	        Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, p );
	        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING, p, "Check input." ) );
	    }
		for ( int i = 0; i < deleteColumns.length; i++ ) {
			// Determine the column to delete, number may change as the columns are deleted
			int columnNum = -1;
			try {
				columnNum = table.getFieldIndex(deleteColumns[i]);
			}
			catch ( Exception e ) {
				message = "Table column \"" + deleteColumns[i] + "\" was not found in table \"" + table.getTableID() + "\"";
				Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
		        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		            message, "Verify that the table contains column \"" + deleteColumns[i] + "\"" ) );
			}
			if ( columnNum >= 0 ) {
				try {
					table.deleteField(columnNum);
				}
				catch ( Exception e ) {
					message = "Exception deleting column \"" + deleteColumns[i] + "\" from table \"" + table.getTableID() + "\" (" + e + ").";
					Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
			        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
			            message, "Check the log file for errors." ) );
				}
			}
        }
 	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error deleting table column (" + e + ").";
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
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TableID = props.getValue( "TableID" );
    String KeepColumns = props.getValue( "KeepColumns" );
    String DeleteColumns = props.getValue( "DeleteColumns" );
    //String DeleteCountProperty = props.getValue( "DeleteCountProperty" );
	StringBuffer b = new StringBuffer ();
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (KeepColumns != null) && (KeepColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "KeepColumns=\"" + KeepColumns + "\"" );
    }
    if ( (DeleteColumns != null) && (DeleteColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DeleteColumns=\"" + DeleteColumns + "\"" );
    }
    /*
    if ( (DeleteCountProperty != null) && (DeleteCountProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DeleteCountProperty=" + DeleteCountProperty );
    }
    */
	return getCommandName() + "(" + b.toString() + ")";
}

}