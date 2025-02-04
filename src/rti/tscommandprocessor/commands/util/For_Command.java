// For_Command - This class initializes, checks, and runs the For() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

/**
This class initializes, checks, and runs the For() command.
See also Continue(), which will be handled in the processor to jump to the end of a loop.
See also Break(), which will be handled in the processor to interrupt a loop.

The command processor (TSEngine) first calls 'next'.
If not at the end of the loop, true will be returned, indicating that the command can be run.
The first time that next() is called, the loop is positioned at the starting value.
*/
public class For_Command extends AbstractCommand
{

/**
Data values for boolean parameters.
*/
protected String _False = "False";
protected String _True = "True";

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
 * Index property name, whose value will be set during iteration for use in the processor as
 * 0 for initial value 1 for first iteration, and increment by 1.
 * This is useful for checking if at the start of the loop.
 */
private String indexPropertyName = null; // Set to null to indicate not set in the processor.

/**
 * Index property value.
 * This is useful for checking if at the start of the loop.
 * <ul>
 * <li> 0 for initial value</li>
 * <li> 1 for first iteration</li>
 * <li> increment by 1</li>
 * </ul>
 */
private int indexPropertyValue = 0;

/**
 * Maximum number of iterations, used for showing progress,
 * where 1 is the first iteration and 'indexPropertyMax' is the maximum expected value.
 */
private int indexPropertyMax = -1;

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
Indicate that the iterator is using a time period.
*/
private boolean iteratorIsPeriod = false;

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
Iterator start if period.
*/
private DateTime iteratorPeriodStart = null;

/**
Iterator end if period.
*/
private DateTime iteratorPeriodEnd = null;

/**
Iterator increment if period.
*/
private TimeInterval iteratorPeriodIncrement = null;

/**
Iterator increment sign if period.
*/
private int iteratorPeriodIncrementSign = 1;

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
 * Whether or not to show progress in the UI:
 * - default is false
 * - true will update the command progress indicator in the TSTool UI
 */
private boolean showProgress;

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
	// General.
	String Name = parameters.getValue ( "Name" );
	String IteratorProperty = parameters.getValue ( "IteratorProperty" );
	String IndexProperty = parameters.getValue ( "IndexProperty" );
	String ShowProgress = parameters.getValue ( "ShowProgress" );
	// List.
	String List = parameters.getValue ( "List" );
	// Sequence.
	String SequenceStart = parameters.getValue ( "SequenceStart" );
	String SequenceEnd = parameters.getValue ( "SequenceEnd" );
	String SequenceIncrement = parameters.getValue ( "SequenceIncrement" );
	// Table.
	String TableID = parameters.getValue ( "TableID" );
	String TableColumn = parameters.getValue ( "TableColumn" );
	// Time Period.
	String PeriodStart = parameters.getValue ( "PeriodStart" );
	String PeriodEnd = parameters.getValue ( "PeriodEnd" );
	String PeriodIncrement = parameters.getValue ( "PeriodIncrement" );
	// Time series list.
	String TSList = parameters.getValue ( "TSList" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	this.iteratorIsList = false;
	this.iteratorIsPeriod = false;
	this.iteratorIsSequence = false;
	this.iteratorIsTable = false;
	this.iteratorIsTsList = false;
    if ( (Name == null) || Name.equals("") ) {
        message = "A name for the 'For' block must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the name." ) );
    }

    if ( (IteratorProperty != null) && !IteratorProperty.equals("") && (IndexProperty != null) && !IndexProperty.isEmpty() ) {
    	if ( IteratorProperty.equals(IndexProperty) ) {
    		message = "The IteratorProperty and IndexProperty cannot be the same.";
    		warning += "\n" + message;
    		status.addToLog ( CommandPhaseType.INITIALIZATION,
    			new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify different values for IteratorProperty and IndexProperty." ) );
    	}
    }
    // TODO smalers 2021-05-09 remove when code tests out.
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

    DateTime periodStart = null;
    if ( (PeriodStart != null) && !PeriodStart.isEmpty() ) {
    	this.iteratorIsPeriod = true;
    	++count;
    	if ( ! PeriodStart.contains("${") ) {
    		try {
    			periodStart = DateTime.parse(PeriodStart);
    		}
    		catch ( Exception e ) {
    			message = "The period start (" + PeriodStart + ") is invalid.";
    			warning += "\n" + message;
    			status.addToLog ( CommandPhaseType.INITIALIZATION,
    				new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify a valid date/time." ) );
    		}
    	}
    }
    DateTime periodEnd = null;
    if ( (PeriodEnd != null) && !PeriodEnd.isEmpty() ) {
    	if ( ! PeriodEnd.contains("${") ) {
    		try {
    			periodEnd = DateTime.parse(PeriodEnd);
    		}
    		catch ( Exception e ) {
    			message = "The period end (" + PeriodEnd + ") is invalid.";
    			warning += "\n" + message;
    			status.addToLog ( CommandPhaseType.INITIALIZATION,
    				new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify a valid date/time." ) );
    		}
    	}
    }
    if ( (periodStart != null) && (periodEnd != null) && (periodStart.getPrecision() != periodEnd.getPrecision()) ) {
    	message = "Period start (" + PeriodStart + ") and period end (" + PeriodEnd + ") have different precision.";
    	warning += "\n" + message;
    	status.addToLog ( CommandPhaseType.INITIALIZATION,
    		new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify start and end with the same precision." ) );
    }
    if ( (PeriodIncrement != null) && !PeriodIncrement.isEmpty() ) {
    	if ( ! PeriodIncrement.contains("${") ) {
    		try {
    			if ( PeriodIncrement.startsWith("-") ) {
    				TimeInterval.parseInterval(PeriodIncrement.substring(1));
    			}
    			else {
    				TimeInterval.parseInterval(PeriodIncrement);
    			}
    		}
    		catch ( Exception e ) {
    			message = "The period increment (" + PeriodIncrement + ") is invalid.";
    			warning += "\n" + message;
    			status.addToLog ( CommandPhaseType.INITIALIZATION,
    				new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify a valid increment (interval)." ) );
    		}
    	}
    }

    if ( (TSList != null) && !TSList.isEmpty() ) {
    	this.iteratorIsTsList = true;
    	++count;
	    if ( (TSList == null) || TSList.equals("") ) {
	        message = "The TSList parameter value must be specified.";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the TSList parameter." ) );
	    }
    }

	if ( (ShowProgress != null) && !ShowProgress.isEmpty() ) {
		if ( !ShowProgress.equalsIgnoreCase(_True) && !ShowProgress.equalsIgnoreCase(_False) ) {
            message = "Invalid ShowProgress parameter \"" + ShowProgress + "\"";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify " + _False + " (default) or " + _True + "." ) );
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
        message = "A list, sequence, table, or time series list must be specified, but not more than one.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the list OR sequence OR table ID/column." ) );
    }

	// Check for invalid parameters.
    List<String> validList = new ArrayList<>(19);
    // General.
	validList.add ( "Name" );
	validList.add ( "IteratorProperty" );
	validList.add ( "IndexProperty" );
	validList.add ( "ShowProgress" );
	// List.
	validList.add ( "IteratorValueProperty" );
	validList.add ( "List" );
	// Sequence.
	validList.add ( "SequenceStart" );
	validList.add ( "SequenceEnd" );
	validList.add ( "SequenceIncrement" );
	// Table.
	validList.add ( "TableID" );
	validList.add ( "TableColumn" );
	validList.add ( "TablePropertyMap" );
	// Time period.
	validList.add ( "PeriodStart" );
	validList.add ( "PeriodEnd" );
	validList.add ( "PeriodIncrement" );
	// Time series.
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
Set the value of the index property.
This is the current index value that can be used by commands within the loop to control logic.
The property is only set if IndexProperty parameter is specified.
@param initialValue initial value of the index property, >= 0 set the value to 1,
if < 0 increment the existing value
*/
private void incrementIndexPropertyValue ( int initialValue ) {
	if ( initialValue >= 0 ) {
		this.indexPropertyValue = 1;
	}
	else {
		// Increment the property value.
		++this.indexPropertyValue;
	}
	if ( this.indexPropertyName != null ) {
		// The 'IndexProperty' command parameter was provided so set the property in the processor.
		String routine = getClass().getSimpleName() + ".setIndexPropertyValue";
		TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();
    	Integer indexPropertyValueO = Integer.valueOf(this.indexPropertyValue);
    	PropList request_params = new PropList ( "" );
		request_params.setUsingObject ( "PropertyName", this.indexPropertyName );
		request_params.setUsingObject ( "PropertyValue", indexPropertyValueO );
		try {
        	processor.processRequest( "SetProperty", request_params);
        	if ( Message.isDebugOn ) {
	       		Message.printDebug(1, routine, "Set index object " + this.indexPropertyName + " to: " + indexPropertyValue );
        	}
		}
		catch ( Exception e ) {
			String message = "Error processing request SetProperty(Property=\"" + this.indexPropertyName + "\").";
    		message = "Error setting property \"" + this.indexPropertyName + " value (" + e + ").";
        	Message.printWarning(3, routine, message);
		}
	}
}

/**
 * Initialize the iterator for a list.
 * @param processor the time series processor
 */
private void initializeListIterator ( TSCommandProcessor processor ) {
	String List = getCommandParameters().getValue ( "List" );
	if ( List.indexOf("${") >= 0 ) {
		// Can specify with property.
		List = TSCommandProcessorUtil.expandParameterValue(processor, this, List);
 	}
	String [] parts = List.split(",");
	this.list = new ArrayList<>();
	for ( int i = 0; i < parts.length; i++ ) {
		this.list.add(parts[i].trim());
	}

	if ( this.showProgress ) {
		// Initialize notify progress listeners.
		this.indexPropertyMax = this.list.size();
		String message = "Initializing 'For' list iterator to value " + parts[0];
		notifyCommandProgressListeners ( 0, this.indexPropertyMax, (float)(0.0), message );
	}
}

/**
 * Initialize the iterator for a period.
 * @param processor the command processor instance
 */
private void initializePeriodIterator ( TSCommandProcessor processor ) {
	PropList parameters = getCommandParameters();
	String PeriodStart = parameters.getValue ( "PeriodStart" );
	if ( (PeriodStart != null) && PeriodStart.contains("${") ) { // }
		// Can specify with a property.
		PeriodStart = TSCommandProcessorUtil.expandParameterValue(processor, this, PeriodStart);
		/*
		if ( s0.equals(PeriodStart) ) {
            message = "For loop 'PeriodStart' (" + PeriodStart + ") value cannot be determined.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the property has a valid value." ) );
		}
		*/
	}
	DateTime periodStart = null;
	if ( (PeriodStart != null) && !PeriodStart.isEmpty() ) {
		periodStart = DateTime.parse(PeriodStart);
		this.iteratorIsPeriod = true;
		this.iteratorPeriodStart = periodStart;
	}
	String PeriodEnd = parameters.getValue ( "PeriodEnd" );
	if ( (PeriodEnd != null) && PeriodEnd.contains("${") ) { // }
		// Can specify with a property.
		PeriodEnd = TSCommandProcessorUtil.expandParameterValue(processor, this, PeriodEnd);
		/*
		if ( s0.equals(PeriodEnd) ) {
            message = "For loop 'PeriodEnd' (" + PeriodEnd + ") value cannot be determined.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the property has a valid value." ) );
		}
		*/
	}
	DateTime periodEnd = null;
	if ( (PeriodEnd != null) && !PeriodEnd.isEmpty() ) {
		periodEnd = DateTime.parse(PeriodEnd);
		this.iteratorPeriodEnd = periodEnd;
	}
	String PeriodIncrement = parameters.getValue ( "PeriodIncrement" );
	TimeInterval periodIncrement = null;
	if ( (PeriodIncrement != null) && !PeriodIncrement.isEmpty() ) {
		if ( PeriodIncrement.startsWith("-") ) {
			// Negative increment.
			this.iteratorPeriodIncrementSign = -1;
			periodIncrement = TimeInterval.parseInterval(PeriodIncrement.substring(1));
		}
		else {
			this.iteratorPeriodIncrementSign = 1;
			periodIncrement = TimeInterval.parseInterval(PeriodIncrement);
		}
		this.iteratorPeriodIncrement = periodIncrement;
	}

	if ( this.showProgress ) {
		// Notify progress listeners with initial value.
		if ( (this.iteratorPeriodStart != null) && (this.iteratorPeriodEnd != null) && (this.iteratorPeriodIncrement != null) ) {
			String message = "Initializing 'For' period iterator to value " + PeriodStart;
			this.indexPropertyMax = TimeUtil.getNumIntervals(this.iteratorPeriodStart, this.iteratorPeriodEnd,
			this.iteratorPeriodIncrement.getBase(), this.iteratorPeriodIncrement.getMultiplier() );
			notifyCommandProgressListeners ( 0, this.indexPropertyMax, (float)(0.0), message );
		}
	}
}

/**
 * Initialize the iterator for a sequence.
 * @param processor the command processor instance
 */
private void initializeSequenceIterator ( TSCommandProcessor processor ) {
	PropList parameters = getCommandParameters();
	String SequenceStart = parameters.getValue ( "SequenceStart" );
	if ( (SequenceStart != null) && (SequenceStart.indexOf("${") >= 0) ) { // }
		// Can specify with property.
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
	if ( (SequenceEnd != null) && (SequenceEnd.indexOf("${") >= 0) ) { // }
		// Can specify with property.
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
			// Set number of steps for progress notification.
			this.indexPropertyMax = (sequenceEndI - sequenceStartI)/sequenceIncrementI + 1;
		}
		else if ( StringUtil.isDouble(SequenceIncrement) ) {
			sequenceIncrementD = Double.parseDouble(SequenceIncrement);
			this.iteratorSequenceIncrement = sequenceIncrementD;
			// Set number of steps for progress notification.
			this.indexPropertyMax = (int)((sequenceEndD - sequenceStartD)/sequenceIncrementD + 1);
		}
	}

	if ( this.showProgress ) {
		// Notify progress listeners with initial value.
		String message = "Initializing 'For' loop value iterator to " + SequenceStart;
		notifyCommandProgressListeners ( sequenceStartI, sequenceEndI, (float)(0.0), message );
	}
}

/**
 * Initialize the iterator for a time series list.
 * @param processor the command processor instance
 * @param status command status, for logging
 * @param warning_level warning level for warning messages
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

	if ( this.showProgress ) {
		// Notify progress listeners with initial value.
		String message = "Initializing 'For' time series list iterator to value " + 0;
		this.indexPropertyMax = tslist.size();
		notifyCommandProgressListeners ( 0, tslist.size(), (float)(0.0), message );
	}

	this.tslist = tslist;
	return warning_count;
}

/**
Increment the loop counter.
If called the first time, initialize to the first value to be iterated.
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
    	// Initialize the loop:
  		// - set the value to the initial value
  		// - if a list or table, set the object list index to 0
  		// - subsequent calls to 'next' will increment
  		TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();
  		
  		// Set important properties so don't need to retrieve repetitively.
  		PropList parameters = this.getCommandParameters();
  		String IteratorProperty = parameters.getValue ( "IteratorProperty" );
	 	if ( (IteratorProperty == null) || IteratorProperty.equals("") ) {
	 		// Default the iterator property to the same as the For name.
	 		String Name = parameters.getValue ( "Name" );
	    	IteratorProperty = Name;
	 	}
    	this.iteratorPropertyName = IteratorProperty;

  		String IndexProperty = parameters.getValue ( "IndexProperty" );
	 	if ( (IndexProperty != null) && IndexProperty.isEmpty() ) {
	 		// Empty property name.  Use null to indicate that the property will not be set in the processor.
	    	this.indexPropertyName = null;
	 	}
	 	else {
	 		// Have a property name to set in the processor.
	 		this.indexPropertyName = IndexProperty;
	 	}
	 	// Always set the index to 1, even if 'IndexProperty' is not used.
    	incrementIndexPropertyValue(1);

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
        		if ( this.showProgress ) {
    				// Notify of the initial value.
    				message = "Initialize 'For' iteration.";
    				notifyCommandProgressListeners ( 0, this.indexPropertyMax, (float)(0.0), message );
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
    	else if ( this.iteratorIsPeriod ) {
    		// Iterating on a time period.
    		// Initialize the loop.
    		setIteratorPropertyValue(null);
	        initializePeriodIterator(processor);
	        CommandStatus status = getCommandStatus();
	        status.clearLog(CommandPhaseType.RUN);
	        try {
	            this.iteratorObjectListIndex = 0;
	            //this.iteratorObjectList = this.list;
	            setIteratorPropertyValue ( this.iteratorPeriodStart );
	            if ( this.iteratorPeriodIncrement == null ) {
	            	// Defaults:
	            	// - currently Day but could make more intelligent
	            	this.iteratorPeriodIncrement = TimeInterval.parseInterval("Day");
	            	this.iteratorPeriodIncrementSign = 1;
	            }
	            this.forInitialized = true;
	            if ( Message.isDebugOn ) {
	            	Message.printDebug(1, routine, "Initialized iterator object to: " + this.iteratorObject );
	            }
        		if ( this.showProgress ) {
    				// Notify of the initial value.
    				message = "Initialize 'For' iteration.";
    				notifyCommandProgressListeners ( 0, this.indexPropertyMax, (float)(0.0), message );
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
        		if ( this.showProgress ) {
    				// Notify of the initial value.
    				message = "Initialize 'For' iteration.";
    				notifyCommandProgressListeners ( 0, this.indexPropertyMax, (float)(0.0), message );
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
        		if ( this.showProgress ) {
    				// Notify of the initial value.
    				message = "Initialize 'For' iteration.";
    				notifyCommandProgressListeners ( 0, this.indexPropertyMax, (float)(0.0), message );
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
        		if ( this.showProgress ) {
    				// Notify of the initial value.
    				message = "Initialize 'For' iteration.";
    				notifyCommandProgressListeners ( 0, this.indexPropertyMax, (float)(0.0), message );
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
	    	message = "Unknown iteration type (not list, sequence, table, time period, or time series list).";
            Message.printWarning(3, routine, message);
            throw new RuntimeException ( message );
	    }
  	}
    else {
    	// The For loop was previously initialized and is now being run:
    	// - the first call to next will initialize 'this.indexPropertyValue' to 1 (above)
    	// - subsequent calls will increment to 2, 3, etc. (below)
    	if ( this.breakSet ) {
    		// Loop is complete because it was broken out of with Break() command:
    		// - leave iterator as it was before with no further action
    		// - return false indicating that next() has been advanced to the end
    		return false;
    	}
        // Increment the property and optionally set properties from table columns.
    	else if ( this.iteratorIsList || this.iteratorIsTable ) {
    		// Notify progress listeners with initial value.
	        if ( this.iteratorObjectListIndex >= (this.iteratorObjectList.size() - 1) ) {
	            // Done iterating.
	        	if ( Message.isDebugOn ) {
	        		Message.printDebug(1, routine, "Done iterating on list." );
	        	}
	        	if ( this.showProgress ) {
	        		// Notify of the last value.
	        		message = "Completed 'For' iteration.";
	        		notifyCommandProgressListeners ( this.indexPropertyMax, this.indexPropertyMax, (float)(100.0), message );
	        	}
	            return false;
	        }
	        else {
	        	// Increment the object list index.
	            ++this.iteratorObjectListIndex;
	            // Increment the general loop index:
	            // - will only be set in the processor if the index property was specified
	            incrementIndexPropertyValue(-1);
	            this.setIteratorPropertyValue(this.iteratorObjectList.get(this.iteratorObjectListIndex));
	        	if ( Message.isDebugOn ) {
	        		Message.printDebug(1, routine, "Iterator object set to: " + this.iteratorObject );
	        	}
	        	if ( this.showProgress ) {
	        		// Notify every 5%.
        			if ( (this.indexPropertyMax > 0) && ((this.indexPropertyValue*100/this.indexPropertyMax)%5) == 0 ) {
        				float progressPercent = (float)(this.iteratorObjectListIndex*100/this.indexPropertyMax);
        				if ( progressPercent > 100.0 ) {
        					progressPercent = 100.0F;
        				}
        				if ( this.iteratorIsList ) {
	        				message = String.format("Processing 'For' list item at %.2f%%.", progressPercent );
        				}
        				else {
	        				message = String.format("Processing 'For' table row at %.2f%%.", progressPercent );
	        			}
        				notifyCommandProgressListeners ( 0, this.list.size(), (float)(progressPercent), message );
	        		}
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
    	else if ( this.iteratorIsPeriod ) {
    		// If the iterator object is already at or will exceed the maximum, then done iterating.
    		if ( Message.isDebugOn ) {
    			Message.printStatus(2, routine, "start=" + this.iteratorPeriodStart +
    				", end=" + this.iteratorPeriodEnd + ", increment=" + this.iteratorPeriodIncrementSign + this.iteratorPeriodIncrement +
    				", object=" + this.iteratorObject);
    		}
    		if ( this.iteratorPeriodIncrementSign > 0 ) {
    			// Incrementing forward in time.
    			if ( ((DateTime)this.iteratorObject).greaterThanOrEqualTo((DateTime)this.iteratorPeriodEnd) ) {
    				// Done iterating.
    				if ( Message.isDebugOn ) {
    					Message.printDebug(1, routine, "Done iterating on time period." );
    				}
    				if ( this.showProgress ) {
    					// Notify of the last value.
    					message = "Completed 'For' iteration.";
    					notifyCommandProgressListeners ( this.indexPropertyMax, this.indexPropertyMax, (float)(100.0), message );
    				}
    				return false;
    			}
    			else {
    				// Increment the general index:
    				// - will only be set in the processor if the index property was specified
    				incrementIndexPropertyValue(-1);
    				// Iterate by adding increment to iterator object:
    				// - no need to reset the iterator object since it is mutable
    				// - multiply by the sign that was detected when the command was parsed
    				if ( Message.isDebugOn ) {
    					Message.printStatus(2,routine, "Incrementing date/time by " + this.iteratorPeriodIncrementSign + this.iteratorPeriodIncrement );
    				}
    				((DateTime)this.iteratorObject).addInterval (
    						this.iteratorPeriodIncrement.getBase(), this.iteratorPeriodIncrementSign*this.iteratorPeriodIncrement.getMultiplier() );
    				if ( this.showProgress ) {
    					// Notify every 5%.
    					if ( (this.indexPropertyMax > 0) && ((this.indexPropertyValue*100/this.indexPropertyMax)%5) == 0 ) {
	        				float progressPercent = (float)(this.indexPropertyValue*100/this.indexPropertyMax);
	        				if ( progressPercent > 100.0 ) {
	        					progressPercent = 100.0F;
	        				}
    						message = String.format("Processing 'For' date/time at %.2f%%.", progressPercent );
    						notifyCommandProgressListeners ( 0, this.indexPropertyMax, (float)(progressPercent), message );
    					}
    				}
    				return true;
    			}
	    	}
    		else {
    			// Incrementing backward in time.
    			if ( ((DateTime)this.iteratorObject).lessThanOrEqualTo((DateTime)this.iteratorPeriodEnd) ) {
    				// Done iterating.
    				if ( Message.isDebugOn ) {
    					Message.printDebug(1, routine, "Done iterating on time period." );
    				}
    				if ( this.showProgress ) {
    					// Notify of the last value.
    					message = "Completed 'For' iteration.";
    					notifyCommandProgressListeners ( this.indexPropertyMax, this.indexPropertyMax, (float)(100.0), message );
    				}
    				return false;
    			}
    			else {
    				// Increment the index:
    				// - will only be set in the processor if the index property was specified
    				incrementIndexPropertyValue(-1);
    				// Iterate by adding increment to iterator object:
    				// - no need to reset the iterator object since it is mutable
    				// - multiply by the sign that was detected when the command was parsed
    				if ( Message.isDebugOn ) {
    					Message.printStatus(2,routine, "Incrementing date/time by " + this.iteratorPeriodIncrementSign + this.iteratorPeriodIncrement );
    				}
    				((DateTime)this.iteratorObject).addInterval (
    						this.iteratorPeriodIncrement.getBase(), this.iteratorPeriodIncrementSign*this.iteratorPeriodIncrement.getMultiplier() );
    				if ( this.showProgress ) {
    					// Notify every 5%.
    					if ( (this.indexPropertyMax > 0) && ((this.indexPropertyValue*100/this.indexPropertyMax)%5) == 0 ) {
	        				float progressPercent = (float)(this.indexPropertyValue*100/this.indexPropertyMax);
	        				if ( progressPercent > 100.0 ) {
	        					progressPercent = 100.0F;
	        				}
    						message = String.format("Processing 'For' date/time at %.2f%%.", progressPercent );
    						notifyCommandProgressListeners ( 0, this.indexPropertyMax, (float)(progressPercent), message );
    					}
    				}
    				return true;
    			}
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
	    		// Done iterating.
	        	if ( Message.isDebugOn ) {
	        		Message.printDebug(1, routine, "Done iterating on sequence." );
	        	}
   				if ( this.showProgress ) {
   					// Notify of the last value.
   					message = "Completed 'For' iteration.";
   					notifyCommandProgressListeners ( this.indexPropertyMax, this.indexPropertyMax, (float)(100.0), message );
   				}
	            return false;
	    	}
	    	else {
	            // Increment the index.
	            incrementIndexPropertyValue(-1);
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
   				if ( this.showProgress ) {
   					// Notify every 5%.
   					if ( (this.indexPropertyMax > 0) && ((this.indexPropertyValue*100/this.indexPropertyMax)%5) == 0 ) {
	       				float progressPercent = (float)(this.indexPropertyValue*100/this.indexPropertyMax);
	        			if ( progressPercent > 100.0 ) {
	        				progressPercent = 100.0F;
	        			}
   						message = String.format("Processing 'For' sequence at %.2f%%.", progressPercent );
   						notifyCommandProgressListeners ( 0, this.indexPropertyMax, (float)(progressPercent), message );
   					}
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
   				if ( this.showProgress ) {
   					// Notify of the last value.
   					message = "Completed 'For' iteration.";
   					notifyCommandProgressListeners ( this.indexPropertyMax, this.indexPropertyMax, (float)(100.0), message );
   				}
	            return false;
	        }
	        else {
	            // Increment the index.
   				// - will only be set in the processor if the index property was specified
	            incrementIndexPropertyValue(-1);
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
   				if ( this.showProgress ) {
   					// Notify every 5%.
   					if ( (this.indexPropertyMax > 0) && ((this.indexPropertyMax*100/this.indexPropertyMax)%5) == 0 ) {
	       				float progressPercent = (float)(this.indexPropertyMax*100/this.indexPropertyMax);
	        			if ( progressPercent > 100.0 ) {
	        				progressPercent = 100.0F;
	        			}
   						message = String.format("Processing 'For' sequence at %.2f%%.", progressPercent );
   						notifyCommandProgressListeners ( 0, this.indexPropertyMax, (float)(progressPercent), message );
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
@param TimeSeriesPropertyMap command parameter to parse
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
This method is called from TSEngine when processing commands and the end of a loop is found.
Resetting allows the For loop to be rerun in the next iteration of nested loops.
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
	this.iteratorIsPeriod = false;
	this.iteratorIsSequence = false;
	this.iteratorIsTable = false;

	this.showProgress = false;

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

	String PeriodStart = parameters.getValue ( "PeriodStart" );
	if ( (PeriodStart != null) && (PeriodStart.indexOf("${") >= 0) ) {
		// Can specify with property.
		String s0 = PeriodStart;
		PeriodStart = TSCommandProcessorUtil.expandParameterValue(processor, this, PeriodStart);
		if ( s0.equals(PeriodStart) ) {
            message = "For loop 'PeriodStart' (" + PeriodStart + ") value cannot be determined.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the property has a valid value." ) );
		}
	}
	DateTime periodStart = null;
	if ( (PeriodStart != null) && !PeriodStart.isEmpty() ) {
		periodStart = DateTime.parse(PeriodStart);
		this.iteratorPeriodStart = periodStart;
		this.iteratorPeriodIncrement = TimeInterval.parseInterval("Day");
		this.iteratorPeriodIncrementSign = 1;
		this.iteratorIsPeriod = true;
	}
	String PeriodEnd = parameters.getValue ( "PeriodEnd" );
	if ( (PeriodEnd != null) && (PeriodEnd.indexOf("${") >= 0) ) {
		// Can specify with property.
		String s0 = PeriodEnd;
		PeriodEnd = TSCommandProcessorUtil.expandParameterValue(processor, this, PeriodEnd);
		if ( s0.equals(PeriodEnd) ) {
            message = "For loop 'PeriodEnd' (" + PeriodEnd + ") value cannot be determined.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the property has a valid value." ) );
		}
	}
	DateTime periodEnd = null;
	if ( (PeriodEnd != null) && !PeriodEnd.isEmpty() ) {
		periodEnd = DateTime.parse(PeriodEnd);
		this.iteratorPeriodEnd = periodEnd;
	}
	String PeriodIncrement = parameters.getValue ( "PeriodIncrement" );
	PeriodIncrement = TSCommandProcessorUtil.expandParameterValue(processor, this, PeriodIncrement);
	if ( (PeriodIncrement != null) && !PeriodIncrement.isEmpty() ) {
		TimeInterval periodIncrement = null;
		// Negative in front of the increment is allowed but should not occur because choices don't provide negatives.
		if ( PeriodIncrement.startsWith("-") ) {
			periodIncrement = TimeInterval.parseInterval(PeriodIncrement.substring(1));
		    this.iteratorPeriodIncrementSign = -1;
		}
		else {
			periodIncrement = TimeInterval.parseInterval(PeriodIncrement);
		    this.iteratorPeriodIncrementSign = 1;
			// If the Period start is after the period end then use a negative increment.
		    if ( periodStart.greaterThan(periodEnd) ) {
		    	this.iteratorPeriodIncrementSign = -1;
		    }
		}
		this.iteratorPeriodIncrement = periodIncrement;
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
    if ( (TableID != null) && !TableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) && TableID.indexOf("${") >= 0 ) { // {
   		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    if ( (TableID != null) && !TableID.isEmpty() ) {
    	this.iteratorIsTable = true;
    }
	// TableColumn is looked up in next() method because table columns may be added within the loop.
    // TablePropertyMap is handled in next(), which is called before this method.
    // TimeSeriesPropertyMap is handled in next(), which is called before this method.

	String ShowProgress = parameters.getValue ( "ShowProgress" );
	this.showProgress = false;
	if ( (ShowProgress != null) && !ShowProgress.isEmpty() && ShowProgress.equalsIgnoreCase(this._True) ) {
		this.showProgress = true;
	}

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
Set the value of the iterator property.
This is the current iterator value that can be used by commands within the loop to control logic.
@param iteratorPropertyValue value of the iterator property
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
		// General.
    	"Name",
    	"IteratorProperty",
    	"IndexProperty",
    	"ShowProgress",
    	// List.
    	"IteratorValueProperty",
    	"List",
    	// Sequence.
    	"SequenceStart",
    	"SequenceEnd",
    	"SequenceIncrement",
    	// Table.
    	"TableID",
    	"TableColumn",
    	"TablePropertyMap",
    	// Time period.
    	"PeriodStart",
    	"PeriodEnd",
    	"PeriodIncrement",
    	// Time series.
		"TSList",
		"TSID",
		"EnsembleID",
    	"TimeSeriesPropertyMap"
	};
	return this.toString(parameters, parameterOrder);
}

}