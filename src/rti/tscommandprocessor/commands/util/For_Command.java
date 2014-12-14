package rti.tscommandprocessor.commands.util;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

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
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Table.DataTable;

/**
This class initializes, checks, and runs the For() command.
*/
public class For_Command extends AbstractCommand implements Command
{

/**
Current index property value.
*/
private Object iteratorObject = null;

/**
Indicate whether the loop has been initialized.  This will be done with the first call to next().
*/
private boolean forInitialized = false;

/**
Data table to get list to iterate through.
*/
private DataTable table = null;

/**
List of objects to iterate through (from table, etc.).
*/
private List<Object> iteratorObjectList = null;

/**
Position in the iterator object list for processing.
*/
private int iteratorObjectListIndex = -1;

/**
Constructor.
*/
public For_Command ()
{	super();
	setCommandName ( "For" );
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
{	String routine = getCommandName() + "_checkCommandParameters";
	String Name = parameters.getValue ( "Name" );
	String TableID = parameters.getValue ( "TableID" );
	String TableColumn = parameters.getValue ( "TableColumn" );
	String warning = "";
	String message;
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (Name == null) || Name.equals("") ) {
        message = "A name for the for block must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the name." ) );
    }
    if ( (TableID == null) || TableID.equals("") ) {
        message = "A table identifier must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the table ID." ) );
    }
    if ( (TableColumn == null) || TableColumn.equals("") ) {
        message = "A table column must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the table column." ) );
    }

	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(4);
	validList.add ( "Name" );
	validList.add ( "IteratorProperty" );
	validList.add ( "TableID" );
	validList.add ( "TableColumn" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );
	
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine,
		warning );
		throw new InvalidCommandParameterException ( warning );
	}
	
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	return (new For_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the current index property value.
@return the current index property value
*/
public Object getIteratorPropertyValue ()
{
    return this.iteratorObject;
}

/**
Return the name of the for command.
@return the name of the for command, should not be null.
*/
public String getName ()
{
    return getCommandParameters().getValue("Name");
}

/**
Increment the loop counter.
If called the first time, initialize.
If the increment will go past the end, return false.
*/
public boolean next ()
{   String routine = getClass().getSimpleName() + ".next", message;
    Message.printStatus(2, routine, "forInitialized=" + this.forInitialized);
    if ( !this.forInitialized ) {
        // Initialize the loop
        setIteratorPropertyValue(null);
        // Create the list of objects for the iterator
        // The table may change dynamically so lookup the column number here
        String TableID = getCommandParameters().getValue ( "TableID" );
        String columnName = getCommandParameters().getValue ( "TableColumn" );
        // TODO SAM 2014-06-29 Need to optimize all of this - currently have duplicate code in runCommand()
        CommandProcessor processor = getCommandProcessor();
        CommandStatus status = getCommandStatus();
        status.clearLog(CommandPhaseType.RUN);
        PropList request_params = null;
        CommandProcessorRequestResultsBean bean = null;
        int warning_level = 2;
        String command_tag = "";
        int warning_count = 0;
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
                this.table = (DataTable)o_Table;
            }
        }
        if ( this.table == null ) {
            message = "Table for iteration is null.";
            Message.printWarning(3, routine, message);
            throw new RuntimeException ( message );
        }
        try {
            this.iteratorObjectListIndex = 0;
            this.iteratorObjectList = this.table.getFieldValues(columnName);
            this.iteratorObject = this.iteratorObjectList.get(this.iteratorObjectListIndex);
            this.forInitialized = true;
            Message.printStatus(2, routine, "Initialized iterator object to: " + this.iteratorObject );
            return true;
        }
        catch ( Exception e ) {
            message = "Error getting table column values for column \"" + columnName + "\" for iteration (" + e + ").";
            Message.printWarning(3, routine, message);
            Message.printWarning(3, routine, e);
            throw new RuntimeException ( message, e );
        }
    }
    else {
        // Increment the property
        if ( this.iteratorObjectListIndex >= (this.iteratorObjectList.size() - 1) ) {
            // Done iterating
            Message.printStatus(2, routine, "Done iterating on list." );
            return false;
        }
        else {
            ++this.iteratorObjectListIndex;
            this.iteratorObject = this.iteratorObjectList.get(this.iteratorObjectListIndex);
            Message.printStatus(2, routine, "Iterator object set to: " + this.iteratorObject );
            return true;
        }
    }
}

/**
Reset the command to an uninitialized state.  This is needed to ensure that re-executing commands will
restart the loop on the first call to next().
*/
public void resetCommand ()
{
    this.forInitialized = false;
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws CommandWarningException, CommandException, InvalidCommandParameterException
{	String routine = "For_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3; // Level for non-user messages for log file.
    CommandProcessor processor = getCommandProcessor();
	PropList parameters = getCommandParameters();
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	
	String Name = parameters.getValue ( "Name" );
	String IteratorProperty = parameters.getValue ( "IteratorProperty" );
	if ( (IteratorProperty == null) || IteratorProperty.equals("") ) {
	    IteratorProperty = Name;
	}
	String TableID = parameters.getValue ( "TableID" );
	//String TableColumn = parameters.getValue ( "TableColumn" );
	
    // Get the table to process.  This logic is repeated in next() because next() is called first.

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
            this.table = (DataTable)o_Table;
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
	    // next() will have been called by the command processor so at this point just set the processor property
	    // Set the basic property as well as property with 0 and 1 indicating zero and 1 offset list positions
        request_params = new PropList ( "" );
        request_params.setUsingObject ( "PropertyName", IteratorProperty );
        request_params.setUsingObject ( "PropertyValue", this.iteratorObject );
        try {
            Message.printStatus(2,routine,"For loop \"" + Name + "\" iteration [" + this.iteratorObjectListIndex + "] iterator=" +
                this.iteratorObject );
            processor.processRequest( "SetProperty", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting SetProperty(Property=\"" + IteratorProperty + "\") from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
        }
        // Property with zero on end
        request_params = new PropList ( "" );
        request_params.setUsingObject ( "PropertyName", IteratorProperty + "0" );
        request_params.setUsingObject ( "PropertyValue", this.iteratorObjectListIndex );
        try {
            processor.processRequest( "SetProperty", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting SetProperty(Property=\"" + IteratorProperty + "\") from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
        }
        // Property with 1 on end
        request_params = new PropList ( "" );
        request_params.setUsingObject ( "PropertyName", IteratorProperty + "1" );
        request_params.setUsingObject ( "PropertyValue", this.iteratorObjectListIndex + 1 );
        try {
            processor.processRequest( "SetProperty", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting SetProperty(Property=\"" + IteratorProperty + "\") from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
        }
	}
	catch ( Exception e ) {
		message = "Unexpected error executing For (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Check the log file or command window for details." ) );
		throw new CommandException ( message );
	}
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the value of the index property.
@param IteratorPropertyValue value of the index property
*/
private void setIteratorPropertyValue ( Object IteratorPropertyValue )
{
    this.iteratorObject = IteratorPropertyValue;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{   if ( props == null ) {
        return getCommandName() + "()";
    }
    String Name = props.getValue( "Name" );
    String IteratorProperty = props.getValue( "IteratorProperty" );
    String TableID = props.getValue( "TableID" );
    String TableColumn = props.getValue( "TableColumn" );
    StringBuffer b = new StringBuffer ();
    if ( (Name != null) && (Name.length() > 0) ) {
        b.append ( "Name=\"" + Name + "\"" );
    }
    if ( (IteratorProperty != null) && (IteratorProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IteratorProperty=\"" + IteratorProperty + "\"" );
    }
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (TableColumn != null) && (TableColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableColumn=\"" + TableColumn + "\"" );
    }
    return getCommandName() + "(" + b.toString() + ")";
}

}