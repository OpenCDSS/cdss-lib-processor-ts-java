// For_Command - This class initializes, checks, and runs the For() command.

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

package rti.tscommandprocessor.commands.util;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.TS.TS;
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
See also Continue(), which will be handled in the processor to jump to the end of a loop.
See also Break(), which will be handled in the processor to interrupt a loop.
*/
public class For_Command extends AbstractCommand implements Command
{

/**
Indicate whether breakFor() has been called, indicating that the iteration is complete.
*/
private boolean breakSet = false;

/**
 * Iterator property name, whose value will be set during iteration for use in the processor.
 * This reflects the user's value and default of the property name.
 */
private String iteratorPropertyName = "";

/**
Current iterator property value, a simple value that can be used in iterating.
*/
private Object iteratorObject = null;

/**
Current iterator time series, a time series that 'iteratorObject' can be extracted from.
*/
private TS iteratorTimeSeries = null;

/**
Hashmap for table column to property map.
*/
private Hashtable<String,String> tablePropertyMap = null;

/**
Hashmap for time series property to property map.
*/
private Hashtable<String,String> timeSeriesPropertyMap = null;

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
Indicate that the iterator is using a time series list.
*/
private boolean iteratorIsTsList = false;

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
List of objects to iterate if a list, table, or time series.
TODO smalers 2021-05-09 need to clarify the difference between 'list' and 'iteratorObjectList'.
*/
private List<Object> list = null;

/**
List of objects to iterate through (from list, table, etc.).
TODO smalers 2021-05-09 need to clarify the difference between 'list' and 'iteratorObjectList'.
*/
private List<Object> iteratorObjectList = null;

/**
List of time series objects to iterate.
TODO smalers 2021-05-09 need to clarify the difference between 'list' and 'iteratorObjectList'.
*/
private List<TS> tslist = null;

/**
Position in the iterator object list for processing.
Also used to track time series position.
TODO smalers 2021-05-09 need to clarify the difference between 'list' and 'iteratorObjectList'.
*/
private int iteratorObjectListIndex = -1;

/**
 * List that is reused to pass problems back from next() function.
 */
private List<String> nextProblems = new ArrayList<>();

/**
Constructor.
*/
public For_Command () {
	super();
	setCommandName ( "For" );
}

/**
 * Break he For() loop, meaning that any attempt to iterate will indicate it is complete.
 */
public void breakFor() {
	this.breakSet = true;
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages,
to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	String routine = getCommandName() + "_checkCommandParameters";
	String Name = parameters.getValue ( "Name" );
	String List = parameters.getValue ( "List" );
	String SequenceStart = parameters.getValue ( "SequenceStart" );
	String SequenceEnd = parameters.getValue ( "SequenceEnd" );
	String SequenceIncrement = parameters.getValue ( "SequenceIncrement" );
	String TableID = parameters.getValue ( "TableID" );
	String TableColumn = parameters.getValue ( "TableColumn" );
	String TSList = parameters.getValue ( "TSList" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	this.iteratorIsList = false;
	this.iteratorIsSequence = false;
	this.iteratorIsTable = false;
	this.iteratorIsTsList = false;
    if ( (Name == null) || Name.equals("") ) {
        message = "A name for the for block must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the name." ) );
    }
    // TODO smalers 2021-05-09 remove when code tests out
    //if ( ((TableID == null) || (TableID.isEmpty()) && ((List == null)) || (List.isEmpty()) && ((SequenceStart == null)) || SequenceStart.isEmpty())) {
    //    message = "A list of values, sequence, table, or time series list must be specified";
    //    warning += "\n" + message;
    //    status.addToLog ( CommandPhaseType.INITIALIZATION,
    //        new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the list OR sequence OR table ID/column." ) );
    //}

    int count = 0;
    if ( (List != null) && !List.isEmpty() ) {
    	this.iteratorIsList = true;
    	++count;
    }
    if ( (SequenceStart != null) && !SequenceStart.isEmpty() ) {
    	this.iteratorIsSequence = true;
    	++count;
    	// TODO smalers 2020-11-02 not sure why this is here.  It is repeated in runCommand().
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
    if ( (TSList != null) && !TSList.isEmpty() ) {
    	this.iteratorIsTsList = true;
    	++count;
	    if ( (TSList == null) || TSList.equals("") ) {
	        message = "The TSList parameter value must be specified";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the TSList parameter." ) );
	    }
    }
    if ( count == 0 ) {
        message = "A list, sequence, table, or time series list must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
            	"Specify the list OR sequence OR table ID/column OR time series list." ) );
    }
    else if ( count > 1 ) {
        message = "A list, sequence, table, or time series list must be specified, but not more than one";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the list OR sequence OR table ID/column." ) );
    }

	// Check for invalid parameters.
    List<String> validList = new ArrayList<>(13);
	validList.add ( "Name" );
	validList.add ( "IteratorProperty" );
	validList.add ( "IteratorValueProperty" );
	validList.add ( "List" );
	validList.add ( "SequenceStart" );
	validList.add ( "SequenceEnd" );
	validList.add ( "SequenceIncrement" );
	validList.add ( "TableID" );
	validList.add ( "TableColumn" );
	validList.add ( "TablePropertyMap" );
	validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
	validList.add ( "TimeSeriesPropertyMap" );
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
public boolean editCommand ( JFrame parent ) {
	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	return (new For_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the number of iterations fully completed.
@return the number of iterations fully completed.
*/
public int getIterationsCompleted () {
	return iteratorObjectListIndex + 1;
}

/**
Return the current iterator index property value.
@return the current iterator index property value
*/
public Object getIteratorPropertyValue () {
    return this.iteratorObject;
}

/**
Return the name of the for command.
@return the name of the for command, should not be null.
*/
public String getName () {
    return getCommandParameters().getValue("Name");
}

/**
 * Initialize the iterator for a list.
 */
private void initializeListIterator ( TSCommandProcessor processor ) {
	String List = getCommandParameters().getValue ( "List" );
	if ( List.indexOf("${") >= 0 ) {
		// Can specify with property.
		List = TSCommandProcessorUtil.expandParameterValue(processor, this, List);
 	}
	String [] parts = List.split(",");
	this.list = new ArrayList<Object>();
	for ( int i = 0; i < parts.length; i++ ) {
		this.list.add(parts[i].trim());
	}
}

/**
 * Initialize the iterator for a sequence.
 */
private void initializeSequenceIterator ( TSCommandProcessor processor ) {
	PropList parameters = getCommandParameters();
	String SequenceStart = parameters.getValue ( "SequenceStart" );
	if ( (SequenceStart != null) && (SequenceStart.indexOf("${") >= 0) ) {
		// Can specify with property
		String s0 = SequenceStart;
		SequenceStart = TSCommandProcessorUtil.expandParameterValue(processor, this, SequenceStart);
		/*
		if ( s0.equals(SequenceStart) ) {
            message = "For loop 'SequenceStart' (" + SequenceStart + ") value cannot be determined.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the property has a valid value." ) );
		}
		*/
	}
	Double sequenceStartD = null;
	Integer sequenceStartI = null;
	if ( (SequenceStart != null) && !SequenceStart.isEmpty() ) {
		if ( StringUtil.isInteger(SequenceStart) ) {
			sequenceStartI = Integer.parseInt(SequenceStart);
			this.iteratorSequenceStart = sequenceStartI;
			// Default increment value, may be reset with property below.
			this.iteratorSequenceIncrement = new Integer(1);
		}
		else if ( StringUtil.isDouble(SequenceStart) ) {
			sequenceStartD = Double.parseDouble(SequenceStart);
			this.iteratorSequenceStart = sequenceStartD;
			// Default increment value, may be reset with property below.
			this.iteratorSequenceIncrement = new Double(1.0);
		}
		this.iteratorIsSequence = true;
	}
	String SequenceEnd = parameters.getValue ( "SequenceEnd" );
	if ( (SequenceEnd != null) && (SequenceEnd.indexOf("${") >= 0) ) {
		// Can specify with property.
		String s0 = SequenceEnd;
		SequenceEnd = TSCommandProcessorUtil.expandParameterValue(processor, this, SequenceEnd);
		/*
		if ( s0.equals(SequenceEnd) ) {
            message = "For loop 'SequenceEnd' (" + SequenceEnd + ") value cannot be determined.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the property has a valid value." ) );
		}
		*/
	}
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
}

/**
 * Initialize the iterator for a time series list.
 */
private int initializeTsListIterator ( TSCommandProcessor processor,
	CommandStatus status, int warning_level, String command_tag, int warning_count ) {
	String routine = this.getClass().getSimpleName() + ".initializeTsListIterator";
	// Get the list of time series.
	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}

	// Get the time series to process.
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
	CommandProcessorRequestResultsBean bean = null;
	try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
        String message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	if ( o_TSList == null ) {
        String message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Report problem to software support." ) );
	}
	@SuppressWarnings("unchecked")
	List<TS> tslist = (List<TS>)o_TSList;
	if ( tslist.size() == 0 ) {
        String message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.WARNING,
				message, "Confirm that time series are available (may be OK for partial run)." ) );
	}
	else {
		Message.printStatus(2, routine, "TSList iterator has " + tslist.size() + " time series.");
	}

	this.tslist = tslist;
	return warning_count;
}

/**
Increment the loop counter.
If called the first time, initialize.
This is called before runCommands() when processing commands so initialize some command runtime data here.
@return If the increment will go past the end (for loop is done), return false.
If the loop advanced, return true.
*/
public boolean next () {
    String routine = getClass().getSimpleName() + ".next", message;
	if ( Message.isDebugOn ) {
		Message.printDebug(1, routine, "forInitialized=" + this.forInitialized);
	}
  	if ( !this.forInitialized ) {
    	// Initialize the loop.
  		TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();

  		// Set important properties so don't need to retrieve repetitively.
  		PropList parameters = this.getCommandParameters();
  		String IteratorProperty = parameters.getValue ( "IteratorProperty" );
	 	if ( (IteratorProperty == null) || IteratorProperty.equals("") ) {
	 		String Name = parameters.getValue ( "Name" );
	    	IteratorProperty = Name;
	 	}
    	this.iteratorPropertyName = IteratorProperty;

  		this.breakSet = false;
    	if ( this.iteratorIsList ) {
    	    // Iterate with the list.
	        setIteratorPropertyValue(null);
	        // TODO smalers 2020-11-01 why is this reprocessed here?  In case it is dynamic?
	        initializeListIterator(processor);
	        CommandStatus status = getCommandStatus();
	        status.clearLog(CommandPhaseType.RUN);
	        try {
	            this.iteratorObjectListIndex = 0;
	            this.iteratorObjectList = this.list;
	            setIteratorPropertyValue ( this.iteratorObjectList.get(this.iteratorObjectListIndex) );
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
    		// Iterating on a sequence.
    		// Initialize the loop.
    		setIteratorPropertyValue(null);
	        initializeSequenceIterator(processor);
	        CommandStatus status = getCommandStatus();
	        status.clearLog(CommandPhaseType.RUN);
	        try {
	            this.iteratorObjectListIndex = 0;
	            //this.iteratorObjectList = this.list;
	            setIteratorPropertyValue ( this.iteratorSequenceStart );
	            if ( this.iteratorSequenceIncrement == null ) {
	            	// Defaults:
	            	// - TODO smalers 2020-11-02 should be set in runCommand()
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
	    	// Iterating on table (table must be specified if list is not).
	        // Initialize the loop.
	        setIteratorPropertyValue(null);
	        // Create the list of objects for the iterator.
	        // The list is looked up once because rows cannot be added to the table during processing.
	        String TableID = getCommandParameters().getValue ( "TableID" );
	        if ( (TableID != null) && !TableID.isEmpty() && TableID.indexOf("${") >= 0 ) { // Use to match braces. }
	       		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
	        }
	        String columnName = getCommandParameters().getValue ( "TableColumn" );
	        parseTablePropertyMap ( getCommandParameters().getValue ( "TablePropertyMap" ) );
	        // TODO SAM 2014-06-29 Need to optimize all of this - currently have duplicate code in runCommand().
	        CommandStatus status = getCommandStatus();
	        status.clearLog(CommandPhaseType.RUN);
	        PropList request_params = null;
	        CommandProcessorRequestResultsBean bean = null;
	        int warning_level = 2;
	        String command_tag = "";
	        int warning_count = 0;
	        if ( (TableID != null) && !TableID.equals("") ) {
	            // Get the table providing the list.
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
	            	// No data in list.
	            	return false;
	            }
	            setIteratorPropertyValue ( this.iteratorObjectList.get(this.iteratorObjectListIndex) );
	            this.forInitialized = true;
	            if ( Message.isDebugOn ) {
	            	Message.printDebug(1, routine, "Initialized iterator object to: " + this.iteratorObject );
	            }
	            // Also need to set properties.
	            nextSetPropertiesFromTable(this.nextProblems);
        		if ( this.nextProblems.size() > 0 ) {
        			StringBuilder b = new StringBuilder();
	        		for ( String problem : this.nextProblems ) {
	        			b.append(problem + "\n");
	        		}
	                throw new RuntimeException ( b.toString() );
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
	    else if ( this.iteratorIsTsList ) {
    	    // Iterate with the time series list, not a simple value.
	        setIteratorPropertyValue(null);
	        // TODO smalers 2020-11-01 why is this reprocessed here?  In case it is dynamic?
	        parseTimeSeriesPropertyMap ( getCommandParameters().getValue ( "TimeSeriesPropertyMap" ) );
	        CommandStatus status = getCommandStatus();
	        int warning_level = 2;
	        String command_tag = "";
	        int warning_count = 0;
	        // The following sets this.tslist.
	        warning_count = initializeTsListIterator(processor, status, warning_level, command_tag, warning_count );
	        status.clearLog(CommandPhaseType.RUN);
	        try {
	        	// Position of the first time series in the list.
	            this.iteratorObjectListIndex = 0;
	            //
	            //this.iteratorObjectList = this.list;
	            //this.iteratorObject = this.iteratorObjectList.get(this.iteratorObjectListIndex);
	            // Object is the time series.  Will need to use other methods to get time series data.
	            if ( (tslist == null) || (tslist.size() == 0) ) {
                    message = "No time series are available from the processor - unable to initialize.";
		            Message.printWarning ( warning_level,
		            MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		            status.addToLog ( CommandPhaseType.RUN,
				            new CommandLogRecord(CommandStatusType.WARNING,
						            message, "Confirm that time series are available (may be OK for partial run)." ) );
	            }
	            else {
	            	this.nextSetIteratorObjectFromTimeSeries(routine, (TS)this.tslist.get(this.iteratorObjectListIndex) );
	            }
	            this.forInitialized = true;
	            if ( Message.isDebugOn ) {
	            	Message.printDebug(1, routine, "Initialized iterator time series object to time series having TSID=\"" +
	            		this.iteratorTimeSeries.getIdentifierString() + "\" alias=" +
	            		this.iteratorTimeSeries.getAlias() + "\"");
	            }
	            // Also need to set properties.
	            nextSetPropertiesFromTimeSeries(this.nextProblems);
        		if ( this.nextProblems.size() > 0 ) {
        			StringBuilder b = new StringBuilder();
	        		for ( String problem : this.nextProblems ) {
	        			b.append(problem + "\n");
	        		}
	                throw new RuntimeException ( b.toString() );
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
	    else {
	    	message = "Unknown iteration type (not list, sequence, or table).";
            Message.printWarning(3, routine, message);
            throw new RuntimeException ( message );
	    }
  	}
    else {
    	// For loop was previously initialized and is now being run.
    	if ( this.breakSet ) {
    		// Loop is complete because it was broken out of with Break() command:
    		// - leave iterator as it was before with no further action
    		// - return false indicating that next() has been advanced to the end
    		return false;
    	}
        // Increment the property and optionally set properties from table columns.
    	else if ( this.iteratorIsList || this.iteratorIsTable ) {
	        if ( this.iteratorObjectListIndex >= (this.iteratorObjectList.size() - 1) ) {
	            // Done iterating.
	        	if ( Message.isDebugOn ) {
	        		Message.printDebug(1, routine, "Done iterating on list." );
	        	}
	            return false;
	        }
	        else {
	            ++this.iteratorObjectListIndex;
	            this.setIteratorPropertyValue(this.iteratorObjectList.get(this.iteratorObjectListIndex));
	        	if ( Message.isDebugOn ) {
	        		Message.printDebug(1, routine, "Iterator object set to: " + this.iteratorObject );
	        	}
	        	// If properties were requested, set.
	        	// The column number is looked up each time because columns may be added in the loop.
	        	if ( this.tablePropertyMap != null ) {
	        		nextSetPropertiesFromTable(this.nextProblems);
	        		if ( this.nextProblems.size() > 0 ) {
	        			StringBuilder b = new StringBuilder();
		        		for ( String problem : this.nextProblems ) {
		        			b.append(problem + "\n");
		        		}
		                throw new RuntimeException ( b.toString() );
	        		}
	        	}
	            return true;
	        }
    	}
    	else if ( this.iteratorIsSequence ) {
    		// If the iterator object is already at or will exceed the maximum, then done iterating.
    		Message.printStatus(2, routine, "start=" + this.iteratorSequenceStart +
    			", end=" + this.iteratorSequenceEnd + ", increment=" + this.iteratorSequenceIncrement + ", object=" + this.iteratorObject);
	    	if ( ((this.iteratorSequenceStart instanceof Integer) &&
	    			(((Integer)this.iteratorObject >= (Integer)this.iteratorSequenceEnd) ||
	    			((Integer)this.iteratorObject + (Integer)this.iteratorSequenceIncrement > (Integer)this.iteratorSequenceEnd))
	    			) ||
	    		((this.iteratorSequenceStart instanceof Double) &&
	    			(((Double)this.iteratorObject >= (Double)this.iteratorSequenceEnd) ||
	    			((Double)this.iteratorObject + (Double)this.iteratorSequenceIncrement) >= (Double)this.iteratorSequenceEnd))
	    			) {
	    		// Done iterating
	        	if ( Message.isDebugOn ) {
	        		Message.printDebug(1, routine, "Done iterating on list." );
	        	}
	            return false;
	    	}
	    	else {
	    		// Iterate by adding increment to iterator object.
	    		if ( this.iteratorSequenceStart instanceof Integer ) {
	    			Integer o = (Integer)this.iteratorObject;
	    			Integer oinc = (Integer)this.iteratorSequenceIncrement;
	    			o = o + oinc;
	    			setIteratorPropertyValue(o);
	    		}
	    		else if ( this.iteratorSequenceStart instanceof Double ) {
	    			Double o = (Double)this.iteratorObject;
	    			Double oinc = (Double)this.iteratorSequenceIncrement;
	    			o = o + oinc;
	    			setIteratorPropertyValue(o);
	    		}
	    		return true;
	    	}
    	}
    	else if ( this.iteratorIsTsList ) {
	        if ( this.iteratorObjectListIndex >= (this.tslist.size() - 1) ) {
	            // Done iterating.
	        	if ( Message.isDebugOn ) {
	        		Message.printDebug(1, routine, "Done iterating on time series list." );
	        	}
	            return false;
	        }
	        else {
	        	// Increment the position for the time series.
	            ++this.iteratorObjectListIndex;
	            // Get the next time series from the time series list.
	            this.nextSetIteratorObjectFromTimeSeries(routine, this.tslist.get(this.iteratorObjectListIndex));
	        	// If properties were requested, set.
	        	if ( this.timeSeriesPropertyMap != null ) {
	        		nextSetPropertiesFromTimeSeries(this.nextProblems);
	        		if ( this.nextProblems.size() > 0 ) {
	        			StringBuilder b = new StringBuilder();
		        		for ( String problem : this.nextProblems ) {
		        			b.append(problem + "\n");
		        		}
		                throw new RuntimeException ( b.toString() );
	        		}
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
 * Helper method to set the iterator object from the iterator time series.
 * This is necessary because the time series has built-in and custom properties that
 * can be used for the iterator object, such as alias, identifier, or other value.
 * @param routine calling routine, for logging
 * @param iteratorTimeSeries the time series to set for the current iteration
 */
private void nextSetIteratorObjectFromTimeSeries(String routine, TS iteratorTimeSeries) {
	this.iteratorTimeSeries = iteratorTimeSeries;
   	if ( Message.isDebugOn ) {
   		Message.printDebug(1, routine, "Set iterator time series object to time series having TSID=\"" +
  		  	this.iteratorTimeSeries.getIdentifierString() +
  		  	"\" alias=" + this.iteratorTimeSeries.getAlias() + "\"");
   	}
   	// Set the value of the iterator property.
   	TSCommandProcessor processor = (TSCommandProcessor)this.getCommandProcessor();
	PropList props = this.getCommandParameters();
	CommandStatus status = this.getCommandStatus();
    String IteratorValueProperty = props.getValue( "IteratorValueProperty" );
    Object iteratorValue = null;
    if ( (IteratorValueProperty != null) && !IteratorValueProperty.isEmpty() ) { // A property value is specified so need to use it to set the iterator value.
    	if ( IteratorValueProperty.indexOf("${") >= 0 ) {
    		// Output is an expanded string...
    		iteratorValue = TSCommandProcessorUtil.expandTimeSeriesMetadataString ( processor, iteratorTimeSeries, IteratorValueProperty,
    			status, CommandPhaseType.RUN);
    		if ( Message.isDebugOn ) {
    			// Print with quotes since a string.
    			Message.printDebug(1, routine, "Setting time series iterator property " + this.iteratorPropertyName + "=\"" + iteratorValue + "\"" );
   	 		}
    	}
    	else{
    		// Output can be any type, depending on what type is matched with the property name.
    		iteratorValue = iteratorTimeSeries.getProperty(IteratorValueProperty);
    		if ( Message.isDebugOn ) {
    			// Print with no quotes.
    			Message.printDebug(1, routine, "Setting time series iterator property " + this.iteratorPropertyName + "=" + iteratorValue  );
   	 		}
    	}
    	if ( iteratorValue == null ) {
			Message.printWarning(3, routine, "Iterator property value from expanded time series property \"" + IteratorValueProperty + "\" is null." );
    	}
    }
    else {
    	// Iterator value defaults to time series alias if available, or TSID if not.
    	String alias = iteratorTimeSeries.getAlias();
    	if ( (alias != null) && !alias.isEmpty() ) {
    		iteratorValue = alias;
    	}
    	else {
    		iteratorValue = iteratorTimeSeries.getIdentifierString();
    	}
    	if ( iteratorValue == null ) {
			Message.printWarning(3, routine, "Iterator property value from time serues alias and TSID null."  );
    	}
    }

    // Set the iterator value, can be a string or other object type.
	this.setIteratorPropertyValue(iteratorValue);
}

/**
Set processor properties from the table using the TablePropertyMap command parameter.
This DOES NOT set the iterator property value.
@param problems non-null list of problems to populate.  If any issues occur looking up the table column,
then the problems list will have non-zero length upon return and the list can be populated.
*/
private void nextSetPropertiesFromTable ( List<String> problems) {
	String message, routine = getClass().getSimpleName() + ".nextSetPropertiesFromTable";
	problems.clear();
	TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();
	Hashtable<String,String> map = this.tablePropertyMap;
	int propertyColumnNum = -1;
	String key = null;
	String propertyName = null;
	for ( Map.Entry<String,String> entry : map.entrySet() ) {
	    propertyColumnNum = -1;
	    try {
	    	// 'key' is the column name.
	        key = entry.getKey();
	        propertyName = entry.getValue();
	        propertyColumnNum = table.getFieldIndex(key);
	    }
	    catch ( Exception e ) {
	    	message = "Column \"" + key + "\" not found in table (" + e + ").  Cannot set corresponding property \"" + propertyName + "\".";
	        Message.printWarning(3, routine, message);
	        problems.add(message);
	        continue;
	    }
	    Object o = null;
	    try {
	        // Set the processor property value.
	        // The table record index corresponds to the list values used in iteration.
	    	// Set using a property because processor.setPropContents() only works for built-in properties.
	    	// Want to set the same as the table value even if null, but also need string value.
	    	o = table.getFieldValue(this.iteratorObjectListIndex, propertyColumnNum);
	    }
	    catch ( Exception e ) {
	    	message = "Error getting property value from table for property \"" + propertyName + "\" (" + e + ").";
	    	problems.add(message);
	        Message.printWarning(3, routine, message);
	        continue;
	    }
	    PropList request_params = new PropList ( "" );
		request_params.setUsingObject ( "PropertyName", propertyName );
		request_params.setUsingObject ( "PropertyValue", o );
		try {
	        processor.processRequest( "SetProperty", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting SetProperty(Property=\"" + propertyName + "\") from processor.";
			/* TODO SAM 2016-04-09 Need to do logging for errors.
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
	        status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Report the problem to software support." ) );
	        */
	    	message = "Error setting property \"" + propertyName + " value (" + e + ").";
	        Message.printWarning(3, routine, message);
	        problems.add(message);
	        continue;
		}
	}
}

/**
Set processor properties from the time series using the TimeSeriesPropertyMap command parameter.
This DOES NOT set the iterator property value.
@param problems non-null list of problems to populate.  If any issues occur looking up the table column,
then the problems list will have non-zero length upon return and the list can be populated.
*/
private void nextSetPropertiesFromTimeSeries ( List<String> problems) {
	String message, routine = getClass().getSimpleName() + ".nextSetPropertiesFromTimeSeries";
	problems.clear();
	TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();
	Hashtable<String,String> map = this.timeSeriesPropertyMap;
	String tsPropertyName = null;
	String propertyName = null;
	CommandStatus status = this.getCommandStatus();
	// Get the current time series for the iteration loop.
	if ( (tslist == null) || (tslist.size() == 0) ) {
        message = "No time series are available from the processor - unable to set properties.";
	    Message.printWarning ( 3, routine, message );
	    // Don't log to command since earlier initialization message will have been set.
	    problems.add(message);
	    return;
	}

    TS ts = this.tslist.get(this.iteratorObjectListIndex);
    if ( Message.isDebugOn ) {
      	Message.printDebug(1, routine, "Set iterator time series object to time series having TSID=\"" +
       		ts.getIdentifierString() + "\" alias=" + ts.getAlias() + "\"");
    }
	// Set command processor properties using time series properties:
	// - remapping names is allowed
	for ( Map.Entry<String,String> entry : map.entrySet() ) {
	    try {
	    	// Key is the time series property name.
	        tsPropertyName = entry.getKey();
	        propertyName = entry.getValue();
	    }
	    catch ( Exception e ) {
	    	//message = "Column \"" + key + "\" not found in table (" + e + ").  Cannot set corresponding property \"" + propertyName + "\".";
	        //Message.printWarning(3, routine, message);
	        //problems.add(message);
	        continue;
	    }
	    Object o = null;
	    try {
	        // Set the time series processor property value.
	    	// Set using a property because processor.setPropContents() only works for built-in properties.
	    	// Want to set the same as the table value even if null, but also need string value.
	    	if ( tsPropertyName.indexOf("${") >= 0 ) {
	    		// Output is an expanded string...
	    		o = TSCommandProcessorUtil.expandTimeSeriesMetadataString ( processor, ts, tsPropertyName,
	    			status, CommandPhaseType.RUN);
	    		if ( Message.isDebugOn ) {
	    			Message.printDebug(1, routine, "Setting property " + propertyName + "=\"" + o + "\"" );
     	 		}
	    	}
	    	else{
	    		// Output can be any type, depending on what type is matched with the property name.
	    		o = ts.getProperty(tsPropertyName);
	    	}
	    	if ( Message.isDebugOn ) {
	    		Message.printDebug(1, routine, "Setting property " + propertyName + "=" + o );
     	 	}
	    }
	    catch ( Exception e ) {
	    	message = "Error getting property value from time series for property \"" + tsPropertyName + "\" (" + e + ").";
	    	problems.add(message);
	        Message.printWarning(3, routine, message);
	        continue;
	    }
	    PropList request_params = new PropList ( "" );
		request_params.setUsingObject ( "PropertyName", propertyName );
		request_params.setUsingObject ( "PropertyValue", o );
		try {
	        processor.processRequest( "SetProperty", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting SetProperty(Property=\"" + propertyName + "\") from processor.";
			/* TODO SAM 2016-04-09 Need to do logging for errors.
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
	        status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Report the problem to software support." ) );
	        */
	    	message = "Error setting property \"" + propertyName + " value (" + e + ").";
	        Message.printWarning(3, routine, message);
	        problems.add(message);
	        continue;
		}
	}
}

/**
Parse the TablePropertyMap command parameter and set internal data.
This is necessary because the next() method is called before runCommand().
*/
private void parseTablePropertyMap ( String TablePropertyMap ) {
	//if ( TablePropertyMap != null) && (TablePropertyMap)
    this.tablePropertyMap = new Hashtable<String,String>();
    if ( (TablePropertyMap != null) && (TablePropertyMap.length() > 0) && (TablePropertyMap.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(TablePropertyMap, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            this.tablePropertyMap.put(parts[0].trim(), parts[1].trim() );
        }
    }
}

/**
Parse the TimeSeriesPropertyMap command parameter and set internal data.
This is necessary because the next() method is called before runCommand().
*/
private void parseTimeSeriesPropertyMap ( String TimeSeriesPropertyMap ) {
    this.timeSeriesPropertyMap = new Hashtable<String,String>();
    if ( (TimeSeriesPropertyMap != null) && (TimeSeriesPropertyMap.length() > 0) && (TimeSeriesPropertyMap.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(TimeSeriesPropertyMap, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            this.timeSeriesPropertyMap.put(parts[0].trim(), parts[1].trim() );
        }
    }
}

/**
Reset the command to an uninitialized state.
This is needed to ensure that re-executing commands will restart the loop on the first call to next().
*/
public void resetCommand () {
    this.forInitialized = false;
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws CommandWarningException, CommandException, InvalidCommandParameterException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
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
    this.iteratorPropertyName = IteratorProperty;
	String List = parameters.getValue ( "List" );
	if ( (List != null) && !List.isEmpty() ) {
		if ( List.indexOf("${") >= 0 ) {
			// Can specify with property.
			List = TSCommandProcessorUtil.expandParameterValue(processor, this, List);
		}
		String [] parts = List.split(",");
		this.list = new ArrayList<>();
		for ( int i = 0; i < parts.length; i++ ) {
			this.list.add(parts[i].trim());
		}
		this.iteratorIsList = true;
	}
	String SequenceStart = parameters.getValue ( "SequenceStart" );
	if ( (SequenceStart != null) && (SequenceStart.indexOf("${") >= 0) ) {
		// Can specify with property.
		String s0 = SequenceStart;
		SequenceStart = TSCommandProcessorUtil.expandParameterValue(processor, this, SequenceStart);
		if ( s0.equals(SequenceStart) ) {
            message = "For loop 'SequenceStart' (" + SequenceStart + ") value cannot be determined.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the property has a valid value." ) );
		}
	}
	Double sequenceStartD = null;
	Integer sequenceStartI = null;
	if ( (SequenceStart != null) && !SequenceStart.isEmpty() ) {
		if ( StringUtil.isInteger(SequenceStart) ) {
			sequenceStartI = Integer.parseInt(SequenceStart);
			this.iteratorSequenceStart = sequenceStartI;
			// Default increment value, may be reset with property below.
			this.iteratorSequenceIncrement = new Integer(1);
		}
		else if ( StringUtil.isDouble(SequenceStart) ) {
			sequenceStartD = Double.parseDouble(SequenceStart);
			this.iteratorSequenceStart = sequenceStartD;
			// Default increment value, may be reset with property below.
			this.iteratorSequenceIncrement = new Double(1.0);
		}
		this.iteratorIsSequence = true;
	}
	String SequenceEnd = parameters.getValue ( "SequenceEnd" );
	if ( (SequenceEnd != null) && (SequenceEnd.indexOf("${") >= 0) ) {
		// Can specify with property.
		String s0 = SequenceEnd;
		SequenceEnd = TSCommandProcessorUtil.expandParameterValue(processor, this, SequenceEnd);
		if ( s0.equals(SequenceEnd) ) {
            message = "For loop 'SequenceEnd' (" + SequenceEnd + ") value cannot be determined.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the property has a valid value." ) );
		}
	}
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
	// TableColumn is looked up in next() method because table columns may be added within the loop.
    // TablePropertyMap is handled in next(), which is called before this method.
    // TimeSeriesPropertyMap is handled in next(), which is called before this method.

    // Get the table to process.  This logic is repeated in next() because next() is called first.

    PropList request_params = null;
    CommandProcessorRequestResultsBean bean = null;
    this.table = null;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to provide the list.
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
	    // next() will have been called by the command processor so at this point just set the processor property.
	    // Set the basic property as well as property with 0 and 1 indicating zero and 1 offset list positions.
        request_params = new PropList ( "" );
        request_params.setUsingObject ( "PropertyName", this.iteratorPropertyName );
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
            message = "Error requesting SetProperty(Property=\"" + this.iteratorPropertyName + "\") from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
        }
        // Property with zero on end.
        request_params = new PropList ( "" );
        request_params.setUsingObject ( "PropertyName", this.iteratorPropertyName + "0" );
        request_params.setUsingObject ( "PropertyValue", this.iteratorObjectListIndex );
        try {
            processor.processRequest( "SetProperty", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting SetProperty(Property=\"" + this.iteratorPropertyName + "\") from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
        }
        // Property with 1 on end.
        request_params = new PropList ( "" );
        request_params.setUsingObject ( "PropertyName", this.iteratorPropertyName + "1" );
        request_params.setUsingObject ( "PropertyValue", this.iteratorObjectListIndex + 1 );
        try {
            processor.processRequest( "SetProperty", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting SetProperty(Property=\"" + this.iteratorPropertyName + "\") from processor.";
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
This is the current index value that can be used by commands within the loop to control logic.
@param IteratorPropertyValue value of the index property
*/
private void setIteratorPropertyValue ( Object iteratorPropertyValue ) {
	String routine = getClass().getSimpleName() + ".setIteratorPropertyValue";
	TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();
    this.iteratorObject = iteratorPropertyValue;
    // TODO smalers 2021-05-10 this needs to set in the processor also, right?
    PropList request_params = new PropList ( "" );
	request_params.setUsingObject ( "PropertyName", this.iteratorPropertyName );
	request_params.setUsingObject ( "PropertyValue", iteratorPropertyValue );
	try {
        processor.processRequest( "SetProperty", request_params);
        if ( Message.isDebugOn ) {
	       	Message.printDebug(1, routine, "Set iterator object " + this.iteratorPropertyName +
	       		" to: " + iteratorPropertyValue );
        }
	}
	catch ( Exception e ) {
		String message = "Error requesting SetProperty(Property=\"" + this.iteratorPropertyName + "\") from processor.";
		/* TODO SAM 2016-04-09 Need to do logging for errors.
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
        */
    	message = "Error setting property \"" + this.iteratorPropertyName + " value (" + e + ").";
        Message.printWarning(3, routine, message);
        // TODO smalers 2021-05-10 should not happen.
        //problems.add(message);
        //continue;
	}
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"Name",
    	"IteratorProperty",
    	"IteratorValueProperty",
    	"List",
    	"SequenceStart",
    	"SequenceEnd",
    	"SequenceIncrement",
    	"TableID",
    	"TableColumn",
    	"TablePropertyMap",
		"TSList",
		"TSID",
		"EnsembleID",
    	"TimeSeriesPropertyMap"
	};
	return this.toString(parameters, parameterOrder);
}

}