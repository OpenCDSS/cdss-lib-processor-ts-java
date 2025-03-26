// DeleteTableRows_Command - This class initializes, checks, and runs the DeleteTableRows() command.

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
import java.util.List;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
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
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRowConditionEvaluator;

/**
This class initializes, checks, and runs the DeleteTableRows() command.
*/
public class DeleteTableRows_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

/**
Property set during discovery.
*/
private Prop discoveryDeleteCountProp = null;

/**
Property set during discovery.
*/
private Prop discoveryRowCountProp = null;

/**
Constructor.
*/
public DeleteTableRows_Command () {
	super();
	setCommandName ( "DeleteTableRows" );
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
    String Condition = parameters.getValue ( "Condition" );
    String DeleteRowNumbers = parameters.getValue ( "DeleteRowNumbers" );
    String First = parameters.getValue ( "First" );
    String Last = parameters.getValue ( "Last" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    int count = 0;
    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }
    if ( (DeleteRowNumbers != null) && !DeleteRowNumbers.isEmpty() ) {
    	++count;
    }
    if ( (Condition != null) && !Condition.isEmpty() ) {
    	++count;
    }
    if ( (First != null) && !First.isEmpty() ) {
    	++count;
    }
    if ( (Last != null) && !Last.isEmpty() ) {
    	++count;
    }
    if ( count > 1 ) {
        message = "Only one of condition, row numbers to delete, First, or Last parameters can be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify condition, row numbers to delete, First, or Last." ) );
    }

    if ( (First != null) && !First.isEmpty() && !StringUtil.isInteger(First) ) {
        message = "The First parameter value (" + First + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the First parameter as an integer." ) );
    }
    if ( (Last != null) && !Last.isEmpty() && !StringUtil.isInteger(Last) ) {
        message = "The Last parameter value (" + Last + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the Last parameter as an integer." ) );
    }
    if ( (First != null) && !First.isEmpty() && (Last != null) && !Last.isEmpty() ) {
        message = "The First and Last parameter are both specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify either First or Last parameters." ) );
    }

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(7);
    validList.add ( "TableID" );
    validList.add ( "Condition" );
    validList.add ( "DeleteRowNumbers" );
    validList.add ( "First" );
    validList.add ( "Last" );
    validList.add ( "DeleteCountProperty" );
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
	List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed.
	return (new DeleteTableRows_JDialog ( parent, this, tableIDChoices )).ok();
}

// Use base class parseCommand().

/**
Return the delete count property defined in discovery phase.
@return the delete count property defined in discovery phase.
*/
private Prop getDiscoveryDeleteCountProp () {
    return this.discoveryDeleteCountProp;
}

/**
Return the table row count property defined in discovery phase.
@return the table row count property defined in discovery phase
*/
private Prop getDiscoveryRowCountProp () {
    return this.discoveryRowCountProp;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  Prop
@return the list of data objects read by this object in discovery mode
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    Prop rowCountProp = getDiscoveryRowCountProp ();
    Prop deleteCountProp = getDiscoveryDeleteCountProp ();
    if ( (rowCountProp == null) && (deleteCountProp == null) ) {
        return null;
    }
    Prop prop = new Prop();
    // Check for TS request or class that matches the data.
    if ( c == prop.getClass() ) {
        List<T> v = new ArrayList<>(1);
        if ( rowCountProp != null ) {
        	v.add ( (T)rowCountProp );
        }
        if ( deleteCountProp != null ) {
        	v.add ( (T)deleteCountProp );
        }
        return v;
    }
    else {
        return null;
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
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_level = 2;
	int log_level = 3; // Level for non-user messages for log file.
	String command_tag = "" + command_number;
	int warning_count = 0;

    CommandStatus status = getCommandStatus();

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
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

    String TableID = parameters.getValue ( "TableID" );
   	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    String Condition = parameters.getValue ( "Condition" );
   	Condition = TSCommandProcessorUtil.expandParameterValue(processor, this, Condition);
    String DeleteRowNumbers = parameters.getValue ( "DeleteRowNumbers" );
   	DeleteRowNumbers = TSCommandProcessorUtil.expandParameterValue(processor, this, DeleteRowNumbers);
    String [] deleteRowNumbers = new String[0];
    boolean deleteAllRows = false;
    if ( (DeleteRowNumbers != null) && !DeleteRowNumbers.isEmpty() ) {
    	deleteRowNumbers = DeleteRowNumbers.split(",");
	    for ( int i = 0; i < deleteRowNumbers.length; i++ ) {
	    	deleteRowNumbers[i] = deleteRowNumbers[i].trim();
	    	if ( deleteRowNumbers[i].equals("*") ) {
	    		deleteAllRows = true;
	    		break;
	    	}
	    }
    }
    String First = parameters.getValue ( "First" );
   	First = TSCommandProcessorUtil.expandParameterValue(processor, this, First);
   	Integer first = null;
   	if ( First != null ) {
   		first = Integer.parseInt(First);
   	}
    String Last = parameters.getValue ( "Last" );
   	Last = TSCommandProcessorUtil.expandParameterValue(processor, this, Last);
   	Integer last = null;
   	if ( Last != null ) {
   		last = Integer.parseInt(Last);
   	}
    String DeleteCountProperty = parameters.getValue ( "DeleteCountProperty" );
    String RowCountProperty = parameters.getValue ( "RowCountProperty" );

    // Get the table to process.
    DataTable table = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
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
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	int rowCount0 = table.getNumberOfRecords();
	try {
		if ( commandPhase == CommandPhaseType.RUN ) {
			if ( deleteRowNumbers.length > 0 ) {
    	    	// Delete rows based on numbers.
				if ( deleteAllRows ) {
					// Special case - delete all the rows.
			    	try {
				    	table.deleteAllRecords();
			    	}
			    	catch ( Exception e ) {
				    	message = "Exception deleting all rows from table \"" + table.getTableID() + "\" (" + e + ").";
				    	Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
		            	status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		                message, "Check the log file for errors." ) );
			    	}
				}
				else {
		        	for ( int i = 0; i < deleteRowNumbers.length; i++ ) {
			        	// Determine the row(s) to delete, number may change as the rows are deleted so user must be aware.
			        	int numRowsInTable = table.getNumberOfRecords();
			        	String rowToDeleteString = deleteRowNumbers[i];
			        	int rowToDelete1 = -1; // 1-index
			        	if ( rowToDeleteString.equalsIgnoreCase("last") ) {
				        	rowToDelete1 = numRowsInTable;
			        	}
			        	else {
				        	rowToDelete1 = Integer.parseInt(rowToDeleteString);
			        	}
			        	if ( rowToDelete1 > numRowsInTable ) {
				        	message = "Row to delete (" + rowToDelete1 + ") is > number of rows in table.  Not deleting.";
				        	Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
		                	status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		                    	message, "Verify that the table contains row " + rowToDelete1 ) );
				        	continue;
			        	}
			        	if ( rowToDelete1 < 1 ) {
				        	message = "Row to delete (" + rowToDelete1 + ") is < 1.  Not deleting.";
				        	Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
		                	status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		                    	message, "Specify a row > 0" ) );
				        	continue;
			        	}
			        	int rowToDelete0 = rowToDelete1 - 1; // 0-index.
			        	try {
				        	table.deleteRecord(rowToDelete0);
			        	}
			        	catch ( Exception e ) {
				        	message = "Exception deleting row \"" + rowToDelete1 + "\" from table \"" + table.getTableID() + "\" (" + e + ").";
				        	Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
		                	status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		                    	message, "Check the log file for errors." ) );
			        	}
		        	}
            	}
 	    	}
			else if ( (Condition != null) && !Condition.isEmpty() ) {
				// Delete by matching rows that adhere to condition:
				// - currently this is simple logic
				// - process in reverse order so that the shift does not cause an issue
				TableRowConditionEvaluator evaluator = new TableRowConditionEvaluator(table, Condition);
				for ( int row = (table.getNumberOfRecords() - 1); row >= 0; row-- ) {
					if ( evaluator.evaluate(table, row) ) {
						// Condition was met so delete the row:
						// - decrement the row since same row needs to be reprocessed
						if ( Message.isDebugOn ) {
							message = "Condition evaluated to true for row [" + row + "].";
							Message.printDebug ( 1, routine, message );
						}
						try {
							table.deleteRecord(row);
						}
						catch ( Exception e ) {
							message = "Exception deleting row \"" + row + "\" from table \"" + table.getTableID() + "\" (" + e + ").";
							Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
							status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
								message, "Check the log file for errors." ) );
						}
					}
					else {
						if ( Message.isDebugOn ) {
							message = "Condition evaluated to false for row [" + row + "].";
							Message.printDebug ( 1, routine, message );
						}
					}
				}
			}
			else if ( first != null ) {
				if ( first > 0 ) {
					// Delete the first number of rows:
					// - loop backwards
					for ( int i = (first - 1); i >= 0; i-- ) {
						table.deleteRecord(i);
					}
				}
				else {
					// Delete all but the first rows:
					// - loop backwards
					first = -first;
					for ( int i = (table.getNumberOfRecords() - 1); i > (first - 1); i-- ) {
						table.deleteRecord(i);
					}
				}
			}
			else if ( last != null ) {
				if ( last > 0 ) {
					// Delete the last number of rows:
					// - loop backwards
					int iend = table.getNumberOfRecords() - last;
					for ( int i = (table.getNumberOfRecords() - 1); i >= iend; i-- ) {
						table.deleteRecord(i);
					}
				}
				else {
					// Delete all but the last rows:
					// - loop backwards
					last = -last;
					for ( int i = (last - 1); i >= 0; i-- ) {
						table.deleteRecord(i);
					}
				}
			}

			int rowCount = table.getNumberOfRecords();
			// Compute the delete count as the change from original.
			int deleteCount = rowCount0 - rowCount;

			// Set the processor properties.

			// Set the property indicating the number of deleted rows.
			if ( (DeleteCountProperty != null) && !DeleteCountProperty.equals("") ) {
				PropList request_params = new PropList ( "" );
				request_params.setUsingObject ( "PropertyName", DeleteCountProperty );
				request_params.setUsingObject ( "PropertyValue", Integer.valueOf(deleteCount) );
				try {
					processor.processRequest( "SetProperty", request_params);
				}
				catch ( Exception e ) {
					message = "Error requesting SetProperty(Property=\"" + DeleteCountProperty + "\") from processor.";
					Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
					status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report the problem to software support." ) );
				}
			}

			// Set the property indicating the number of rows in the table after rows are deleted.
			if ( (RowCountProperty != null) && !RowCountProperty.equals("") ) {
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
		else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create an empty table and set the ID.
			if ( (DeleteCountProperty != null) && !DeleteCountProperty.isEmpty() ) {
				Prop prop = new Prop(DeleteCountProperty, "", "");
				prop.setHowSet(Prop.SET_UNKNOWN);
				setDiscoveryDeleteCountProp ( prop );
			}
			if ( (RowCountProperty != null) && !RowCountProperty.isEmpty() ) {
				Prop prop = new Prop(RowCountProperty, "", "");
				prop.setHowSet(Prop.SET_UNKNOWN);
				setDiscoveryRowCountProp ( prop );
			}
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error deleting table row (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check the log file for errors." ) );
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
Set the property defined in discovery phase.
@param prop Property set during discovery phase.
*/
private void setDiscoveryDeleteCountProp ( Prop prop ) {
    this.discoveryDeleteCountProp = prop;
}

/**
Set the property defined in discovery phase.
@param prop Property set during discovery phase.
*/
private void setDiscoveryRowCountProp ( Prop prop ) {
    this.discoveryRowCountProp = prop;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TableID",
    	"Condition",
    	"DeleteRowNumbers",
    	"First",
    	"Last",
    	"DeleteCountProperty",
    	"RowCountProperty"
	};
	return this.toString(parameters, parameterOrder);
}

}