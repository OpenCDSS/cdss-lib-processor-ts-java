// DeleteDataStoreTableRows_Command - This class initializes, checks, and runs the RemoveDataStoreTableRows() command.

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

package rti.tscommandprocessor.commands.datastore;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.List;
import java.util.Vector;

import RTi.DMI.DMI;
import RTi.DMI.DMIDeleteStatement;
import RTi.DMI.DatabaseDataStore;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.PropList;

/**
This class initializes, checks, and runs the RemoveDataStoreTableRows() command.
*/
public class DeleteDataStoreTableRows_Command extends AbstractCommand implements Command
{

/**
Possible values for DeleteAllRows parameter.
*/
protected final String _False = "False";
protected final String _True = "True";
protected final String _Truncate = "Truncate";
    
/**
Constructor.
*/
public DeleteDataStoreTableRows_Command ()
{	super();
	setCommandName ( "DeleteDataStoreTableRows" );
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
{   String DataStore = parameters.getValue ( "DataStore" );
    String DataStoreTable = parameters.getValue ( "DataStoreTable" );
    //String TableID = parameters.getValue ( "TableID" );
    String DeleteAllRows = parameters.getValue ( "DeleteAllRows" );

	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (DataStore == null) || (DataStore.length() == 0) ) {
        message = "The datastore must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the datastore." ) );
    }
    if ( (DataStoreTable == null) || (DataStoreTable.length() == 0) ) {
        message = "The datastore table must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the datastore table." ) );
    }
    //if ( (TableID == null) || (TableID.length() == 0) ) {
    //    message = "The output table identifier must be specified.";
    //    warning += "\n" + message;
    //    status.addToLog ( CommandPhaseType.INITIALIZATION,
    //        new CommandLogRecord(CommandStatusType.FAILURE,
    //            message, "Specify the output table identifier." ) );
    //}
    
    if ( (DeleteAllRows != null) && !DeleteAllRows.equals("") &&
        !DeleteAllRows.equalsIgnoreCase(_True) && !DeleteAllRows.equalsIgnoreCase(_False) && !DeleteAllRows.equalsIgnoreCase(_Truncate)) {
        message = "The DeleteAllRows (" + DeleteAllRows + ") parameter value is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as " + _False + " (default), " + _True + ", or" + _Truncate + "." ) );
    }
    
	//  Check for invalid parameters...
	List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "DataStore" );
    valid_Vector.add ( "DataStoreTable" );
    //valid_Vector.add ( "TableID" );
    //valid_Vector.add ( "TableColumns" );
    valid_Vector.add ( "DeleteAllRows" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );    

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
{	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed...
	return (new DeleteDataStoreTableRows_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Parse the command string into a PropList of parameters.
The method is needed here because the legacy RemoveAllRows parameter has been changed to DeleteAllRows.
@param commandString A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String commandString )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{
    super.parseCommand( commandString);
    // Replace the old syntax with the new
    PropList props = getCommandParameters();
    String propValue = props.getValue("RemoveAllRows");
    if ( propValue != null ) {
        props.set("DeleteAllRows",propValue);
        props.unSet("RemoveAllRows");
    }
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    status.clearLog(commandPhase);

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String DataStore = parameters.getValue ( "DataStore" );
    String DataStoreTable = parameters.getValue ( "DataStoreTable" );
    if ( (DataStoreTable != null) && DataStoreTable.equals("") ) {
        DataStoreTable = null; // Simplifies logic below
    }
    String DeleteAllRows = parameters.getValue ( "DeleteAllRows" );
    
    // Find the datastore to use...
    DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
        DataStore, DatabaseDataStore.class );
    DMI dmi = null;
    if ( dataStore == null ) {
        message = "Could not get datastore for name \"" + DataStore + "\" to query data.";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a database connection has been opened with name \"" +
                DataStore + "\"." ) );
    }
    else {
        dmi = ((DatabaseDataStore)dataStore).getDMI();
    }
    
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

    String sqlString = "";
    try {
        if ( (DeleteAllRows != null) && DeleteAllRows.equalsIgnoreCase(_True) ) {
            // Create the delete statement.
            DMIDeleteStatement d = new DMIDeleteStatement(dmi);
            d.addTable(DataStoreTable);
            if ( DataStoreTable != null ) {
                // Execute the delete statement that was built
                sqlString = d.toString();
                int delCount = dmi.dmiDelete(d);
                Message.printStatus(2, routine, "Deleted " + delCount + " rows from table \"" +
                        DataStoreTable + "\" with SQL statement \"" + sqlString + "\".");
            }
        }
        else if ( (DeleteAllRows != null) && DeleteAllRows.equalsIgnoreCase(_Truncate) ) {
            sqlString = "TRUNCATE TABLE " + DataStoreTable;
            int nEffected = dmi.dmiExecute(sqlString);
            Message.printStatus(2, routine, "Truncated " + nEffected + " rows from table \"" +
                DataStoreTable + "\" with SQL statement \"" + sqlString + "\".");
        }
        else {
            // To be implemented in the future...
            message = "Ability to delete specific rows is not implemented.";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Software requires an enhancement.") );
        }
    }
    catch ( Exception e ) {
        message = "Error deleting rows from table \"" + DataStoreTable +
            "\" in datastore \"" + DataStore + "\" (" + e + ").";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the database for datastore \"" + DataStore +
                "\" is appropriate for SQL statement: \"" + sqlString + "\"." ) );
        Message.printWarning ( 3, routine, e );
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
	String DataStore = props.getValue( "DataStore" );
	String DataStoreTable = props.getValue( "DataStoreTable" );
    //String TableID = props.getValue( "TableID" );
	//String DataStoreColumns = props.getValue( "DataStoreColumns" );
	String DeleteAllRows = props.getValue( "DeleteAllRows" );
	StringBuffer b = new StringBuffer ();
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
    if ( (DataStoreTable != null) && (DataStoreTable.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreTable=\"" + DataStoreTable + "\"" );
    }
    /*
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (DataStoreColumns != null) && (DataStoreColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreColumns=\"" + DataStoreColumns + "\"" );
    }
    */
    if ( (DeleteAllRows != null) && (DeleteAllRows.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DeleteAllRows=\"" + DeleteAllRows + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
