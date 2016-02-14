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
import RTi.Util.String.StringUtil;
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
Indicate that the iterator is using a list.
*/
private boolean iteratorIsList = false;

/**
Indicate that the iterator is using a sequence.
*/
private boolean iteratorIsSequence = false;

/**
Indicate that the iterator is using a table.
*/
private boolean iteratorIsTable = false;

/**
Indicate whether the loop has been initialized.  This will be done with the first call to next().
*/
private boolean forInitialized = false;

/**
Data table to get list to iterate through.
*/
private DataTable table = null;

/**
Iterator start if sequence.
*/
private Object iteratorSequenceStart = null;

/**
Iterator end if sequence.
*/
private Object iteratorSequenceEnd = null;

/**
Iterator increment if sequence.
*/
private Object iteratorSequenceIncrement = null;

/**
List of objects to iterate if a list or table.
*/
private List<Object> list = null;

/**
List of objects to iterate through (from list, table, etc.).
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
	String List = parameters.getValue ( "List" );
	String SequenceStart = parameters.getValue ( "SequenceStart" );
	String SequenceEnd = parameters.getValue ( "SequenceEnd" );
	String SequenceIncrement = parameters.getValue ( "SequenceIncrement" );
	String TableID = parameters.getValue ( "TableID" );
	String TableColumn = parameters.getValue ( "TableColumn" );
	String warning = "";
	String message;
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	this.iteratorIsList = false;
	this.iteratorIsSequence = false;
	this.iteratorIsTable = false;
    if ( (Name == null) || Name.equals("") ) {
        message = "A name for the for block must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the name." ) );
    }
    if ( ((TableID == null) || TableID.isEmpty()) && ((List == null) || List.isEmpty()) && ((SequenceStart == null) || SequenceStart.isEmpty())) {
        message = "A list, sequence, or table must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the list OR sequence OR table ID/column." ) );
    }

    int count = 0;
    if ( (List != null) && !List.isEmpty() ) {
    	this.iteratorIsList = true;
    	++count;
    }
    if ( (SequenceStart != null) && !SequenceStart.isEmpty() ) {
    	this.iteratorIsSequence = true;
    	++count;
    	Double sequenceStartD = null;
    	Integer sequenceStartI = null;
		if ( StringUtil.isInteger(SequenceStart) ) {
			sequenceStartI = Integer.parseInt(SequenceStart);
			this.iteratorSequenceStart = sequenceStartI;
		}
		else if ( StringUtil.isDouble(SequenceStart) ) {
			sequenceStartD = Double.parseDouble(SequenceStart);
			this.iteratorSequenceStart = sequenceStartD;
		}
		this.iteratorIsSequence = true;
    }
    if ( (SequenceEnd != null) && !SequenceEnd.isEmpty() ) {
    	Double sequenceEndD = null;
    	Integer sequenceEndI = null;
    	if ( (SequenceEnd != null) && !SequenceEnd.isEmpty() ) {
    		if ( StringUtil.isInteger(SequenceEnd) ) {
    			sequenceEndI = Integer.parseInt(SequenceEnd);
    			this.iteratorSequenceEnd = sequenceEndI;
    		}
    		else if ( StringUtil.isDouble(SequenceEnd) ) {
    			sequenceEndD = Double.parseDouble(SequenceEnd);
    			this.iteratorSequenceEnd = sequenceEndD;
    		}
    	}
    }
    if ( (SequenceIncrement != null) && !SequenceIncrement.isEmpty() ) {
    	Double sequenceIncrementD = null;
    	Integer sequenceIncrementI = null;
		if ( StringUtil.isInteger(SequenceIncrement) ) {
			sequenceIncrementI = Integer.parseInt(SequenceIncrement);
			this.iteratorSequenceIncrement = sequenceIncrementI;
		}
		else if ( StringUtil.isDouble(SequenceIncrement) ) {
			sequenceIncrementD = Double.parseDouble(SequenceIncrement);
			this.iteratorSequenceIncrement = sequenceIncrementD;
		}
    }
    if ( (TableID != null) && !TableID.isEmpty() ) {
    	this.iteratorIsTable = true;
    	++count;
	    if ( (TableColumn == null) || TableColumn.equals("") ) {
	        message = "A table column must be specified";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the table column." ) );
	    }
    }
    if ( count > 1 ) {
        message = "A list, sequence, or table must be specified, but not more than one";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the list OR sequence OR table ID/column." ) );
    }

	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(8);
	validList.add ( "Name" );
	validList.add ( "IteratorProperty" );
	validList.add ( "List" );
	validList.add ( "SequenceStart" );
	validList.add ( "SequenceEnd" );
	validList.add ( "SequenceIncrement" );
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
Return the number of iterations fully completed.
@return the number of iterations fully completed.
*/
public int getIterationsCompleted ()
{
	return iteratorObjectListIndex + 1;
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
	if ( Message.isDebugOn ) {
		Message.printDebug(1, routine, "forInitialized=" + this.forInitialized);
	}
  	if ( !this.forInitialized ) {
    	// Initialize the loop
  		TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();
    	if ( this.iteratorIsList ) {
    	    // Iterate with the list
	        setIteratorPropertyValue(null);
	  		String List = getCommandParameters().getValue ( "List" );
    		String [] parts = List.split(",");
    		this.list = new ArrayList<Object>();
    		for ( int i = 0; i < parts.length; i++ ) {
    			this.list.add(parts[i].trim());
    		}
	        CommandStatus status = getCommandStatus();
	        status.clearLog(CommandPhaseType.RUN);
	        try {
	            this.iteratorObjectListIndex = 0;
	            this.iteratorObjectList = this.list;
	            this.iteratorObject = this.iteratorObjectList.get(this.iteratorObjectListIndex);
	            this.forInitialized = true;
	            if ( Message.isDebugOn ) {
	            	Message.printDebug(1, routine, "Initialized iterator object to: " + this.iteratorObject );
	            }
	            return true;
	        }
	        catch ( Exception e ) {
	            message = "Error initializing For() iterator to initial value (" + e + ").";
	            Message.printWarning(3, routine, message);
	            Message.printWarning(3, routine, e);
	            throw new RuntimeException ( message, e );
	        }
	    }
    	else if ( this.iteratorIsSequence ) {
    		// Iterating on a sequence
    		// Initialize the loop
    		setIteratorPropertyValue(null);
	        CommandStatus status = getCommandStatus();
	        status.clearLog(CommandPhaseType.RUN);
	        try {
	            this.iteratorObjectListIndex = 0;
	            //this.iteratorObjectList = this.list;
	            this.iteratorObject = this.iteratorSequenceStart;
	            if ( this.iteratorSequenceIncrement == null ) {
	            	if ( this.iteratorSequenceStart instanceof Integer ) {
	            		this.iteratorSequenceIncrement = new Integer(1);
	            	}
	            	else if ( this.iteratorSequenceStart instanceof Double ) {
	            		this.iteratorSequenceIncrement = new Double(1.0);
	            	}
	            }
	            this.forInitialized = true;
	            if ( Message.isDebugOn ) {
	            	Message.printDebug(1, routine, "Initialized iterator object to: " + this.iteratorObject );
	            }
	            return true;
	        }
	        catch ( Exception e ) {
	            message = "Error initializing For() iterator to initial value (" + e + ").";
	            Message.printWarning(3, routine, message);
	            Message.printWarning(3, routine, e);
	            throw new RuntimeException ( message, e );
	        }
    	}
	    else if ( this.iteratorIsTable ) {
	    	// Iterating on table (table must be specified if list is not)
	        // Initialize the loop
	        setIteratorPropertyValue(null);
	        // Create the list of objects for the iterator
	        // The table may change dynamically so lookup the column number here
	        String TableID = getCommandParameters().getValue ( "TableID" );
	        if ( (TableID != null) && !TableID.isEmpty() && TableID.indexOf("${") >= 0 ) {
	       		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
	        }
	        String columnName = getCommandParameters().getValue ( "TableColumn" );
	        // TODO SAM 2014-06-29 Need to optimize all of this - currently have duplicate code in runCommand()
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
	            if ( (this.iteratorObjectList == null) || (this.iteratorObjectList.size() == 0) ) {
	            	// No data in list
	            	return false;
	            }
	            this.iteratorObject = this.iteratorObjectList.get(this.iteratorObjectListIndex);
	            this.forInitialized = true;
	            if ( Message.isDebugOn ) {
	            	Message.printDebug(1, routine, "Initialized iterator object to: " + this.iteratorObject );
	            }
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
	    	message = "Unknown iteration type (not list, sequence, or table).";
            Message.printWarning(3, routine, message);
            throw new RuntimeException ( message );
	    }
  	}
    else {
        // Increment the property
    	if ( this.iteratorIsList || this.iteratorIsTable ) {
	        if ( this.iteratorObjectListIndex >= (this.iteratorObjectList.size() - 1) ) {
	            // Done iterating
	        	if ( Message.isDebugOn ) {
	        		Message.printDebug(1, routine, "Done iterating on list." );
	        	}
	            return false;
	        }
	        else {
	            ++this.iteratorObjectListIndex;
	            this.iteratorObject = this.iteratorObjectList.get(this.iteratorObjectListIndex);
	        	if ( Message.isDebugOn ) {
	        		Message.printDebug(1, routine, "Iterator object set to: " + this.iteratorObject );
	        	}
	            return true;
	        }
    	}
    	else if ( this.iteratorIsSequence ) {
    		// If the iterator object is already at or will exceed the maximum, then done iterating
	    	if ( ((this.iteratorSequenceStart instanceof Integer) &&
	    			(((Integer)this.iteratorObject >= (Integer)this.iteratorSequenceEnd) ||
	    			((Integer)this.iteratorObject + (Integer)this.iteratorSequenceIncrement > (Integer)this.iteratorSequenceEnd))
	    			) ||
	    		((this.iteratorSequenceStart instanceof Double) &&
	    			(((Double)this.iteratorObject >= (Double)this.iteratorSequenceEnd) ||
	    			((Double)this.iteratorObject + (Double)this.iteratorSequenceIncrement) >= (Double)this.iteratorSequenceEnd))
	    			) {
	        	if ( Message.isDebugOn ) {
	        		Message.printDebug(1, routine, "Done iterating on list." );
	        	}
	            return false;
	    	}
	    	else {
	    		// Iterate by adding increment to iterator object
	    		if ( this.iteratorSequenceStart instanceof Integer ) {
	    			Integer o = (Integer)this.iteratorObject;
	    			Integer oinc = (Integer)this.iteratorSequenceIncrement;
	    			o = o + oinc;
	    			this.iteratorObject = o;
	    		}
	    		else if ( this.iteratorSequenceStart instanceof Double ) {
	    			Double o = (Double)this.iteratorObject;
	    			Double oinc = (Double)this.iteratorSequenceIncrement;
	    			o = o + oinc;
	    			this.iteratorObject = o;
	    		}
	    		return true;
	    	}
    	}
    	else {
    		// Iteration type not recognized so jump out right away to avoid infinite loop.
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
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3; // Level for non-user messages for log file.
    CommandProcessor processor = getCommandProcessor();
	PropList parameters = getCommandParameters();
	
	CommandStatus status = getCommandStatus();
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
	status.clearLog(commandPhase);
	
	this.iteratorIsList = false;
	this.iteratorIsSequence = false;
	this.iteratorIsTable = false;
	
	String Name = parameters.getValue ( "Name" );
	String IteratorProperty = parameters.getValue ( "IteratorProperty" );
	if ( (IteratorProperty == null) || IteratorProperty.equals("") ) {
	    IteratorProperty = Name;
	}
	String List = parameters.getValue ( "List" );
	if ( (List != null) && !List.isEmpty() ) {
		String [] parts = List.split(",");
		this.list = new ArrayList<Object>();
		for ( int i = 0; i < parts.length; i++ ) {
			this.list.add(parts[i].trim());
		}
		this.iteratorIsList = true;
	}
	String SequenceStart = parameters.getValue ( "SequenceStart" );
	Double sequenceStartD = null;
	Integer sequenceStartI = null;
	if ( (SequenceStart != null) && !SequenceStart.isEmpty() ) {
		if ( StringUtil.isInteger(SequenceStart) ) {
			sequenceStartI = Integer.parseInt(SequenceStart);
			this.iteratorSequenceStart = sequenceStartI;
		}
		else if ( StringUtil.isDouble(SequenceStart) ) {
			sequenceStartD = Double.parseDouble(SequenceStart);
			this.iteratorSequenceStart = sequenceStartD;
		}
		this.iteratorIsSequence = true;
	}
	String SequenceEnd = parameters.getValue ( "SequenceEnd" );
	Double sequenceEndD = null;
	Integer sequenceEndI = null;
	if ( (SequenceEnd != null) && !SequenceEnd.isEmpty() ) {
		if ( StringUtil.isInteger(SequenceEnd) ) {
			sequenceEndI = Integer.parseInt(SequenceEnd);
			this.iteratorSequenceEnd = sequenceEndI;
		}
		else if ( StringUtil.isDouble(SequenceEnd) ) {
			sequenceEndD = Double.parseDouble(SequenceEnd);
			this.iteratorSequenceEnd = sequenceEndD;
		}
	}
	String SequenceIncrement = parameters.getValue ( "SequenceIncrement" );
	Double sequenceIncrementD = null;
	Integer sequenceIncrementI = null;
	if ( (SequenceIncrement != null) && !SequenceIncrement.isEmpty() ) {
		if ( StringUtil.isInteger(SequenceIncrement) ) {
			sequenceIncrementI = Integer.parseInt(SequenceIncrement);
			this.iteratorSequenceIncrement = sequenceIncrementI;
		}
		else if ( StringUtil.isDouble(SequenceIncrement) ) {
			sequenceIncrementD = Double.parseDouble(SequenceIncrement);
			this.iteratorSequenceIncrement = sequenceIncrementD;
		}
	}
	String TableID = parameters.getValue ( "TableID" );
    if ( (TableID != null) && !TableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) && TableID.indexOf("${") >= 0 ) {
   		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    if ( (TableID != null) && !TableID.isEmpty() ) {
    	this.iteratorIsTable = true;
    }
	// TableColumn is looked up in next() method because table columns may be added within loop
	
    // Get the table to process.  This logic is repeated in next() because next() is called first.

    PropList request_params = null;
    CommandProcessorRequestResultsBean bean = null;
    this.table = null;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to provide the list
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
        	if ( Message.isDebugOn ) {
        		if ( this.iteratorIsList || this.iteratorIsTable ) {
		            Message.printDebug(1,routine,"For loop \"" + Name + "\" iteration [" + this.iteratorObjectListIndex + "] iterator=" +
		                this.iteratorObject );
        		}
        		else {
        			Message.printDebug(1,routine,"For loop \"" + Name + "\" iterator=" + this.iteratorObject );
        		}
        	}
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
    String List = props.getValue( "List" );
    String SequenceStart = props.getValue( "SequenceStart" );
    String SequenceEnd = props.getValue( "SequenceEnd" );
    String SequenceIncrement = props.getValue( "SequenceIncrement" );
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
    if ( (List != null) && (List.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "List=\"" + List + "\"" );
    }
    if ( (SequenceStart != null) && (SequenceStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SequenceStart=\"" + SequenceStart + "\"" );
    }
    if ( (SequenceEnd != null) && (SequenceEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SequenceEnd=\"" + SequenceEnd + "\"" );
    }
    if ( (SequenceIncrement != null) && (SequenceIncrement.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SequenceIncrement=\"" + SequenceIncrement + "\"" );
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