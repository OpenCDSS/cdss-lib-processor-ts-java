// VariableLagK_Command - This class initializes, edits, and runs the VariableLagK command.

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

package rti.tscommandprocessor.commands.ts;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import riverside.ts.routing.lagk.LagKBuilder;
import riverside.ts.util.Table;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSIdent;
import RTi.TS.TSIterator;
import RTi.TS.TSUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.DataUnitsConversion;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.DataUnits;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, edits, and runs the VariableLagK command.
*/
public class VariableLagK_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Possible values for InitializeStatesFromTable parameter.
*/
protected final String _False = "False";
protected final String _True = "True";
    
/**
Big flow value used to constrain lookups in the tables.
ResJ had 1000000.0 but this seems like it be exceeded.
*/
//private double __BIG_DATA_VALUE = 1000000.0; // Legacy ResJ
//private double __BIG_DATA_VALUE = Double.MAX_VALUE; // Up to TSTool 12.05.00, seems to cause issue with LagKBuilder use of 1000000.0 for table large bounding value
private double BIG_DATA_VALUE = LagKBuilder.BIG_DATA_VALUE; // Make sure this agrees with use in LagKBuilder.__BIG_DATA_VALUE
    
/**
List of time series created during discovery.  This is the routed time series.
*/
private List<TS> __discoveryTSList = null;
    
/**
LagInterval as a TimeInterval.
*/
private TimeInterval __LagInterval_TimeInterval = null;

/**
Table of flow,lag values.
*/
private Table __Lag_Table = null;

/**
Table of flow,K values.
*/
private Table __K_Table = null;

/**
Constructor.
*/
public VariableLagK_Command ()
{	super();
	setCommandName ( "VariableLagK" );
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
{	
	String warning = "";
    String message;
        
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Get the properties from the PropList parameters.
	String TSID = parameters.getValue( "TSID" );
    String NewTSID = parameters.getValue ( "NewTSID" );
    String FlowUnits = parameters.getValue( "FlowUnits" );
    String LagInterval = parameters.getValue( "LagInterval" );
    String Lag = parameters.getValue( "Lag" );
	String K = parameters.getValue( "K" );
    String OutputStart = parameters.getValue ( "OutputStart" );
    String OutputEnd = parameters.getValue ( "OutputEnd" );
    String InitializeStatesFromTable = parameters.getValue( "InitializeStatesFromTable" );
	String InitialLaggedInflow = parameters.getValue( "InitialLaggedInflow" );
	String InitialOutflow = parameters.getValue( "InitialOutflow" );
	String InitialStorage = parameters.getValue( "InitialStorage" );
	String InitialQTLag = parameters.getValue( "InitialQTLag" );
	String StateTableID = parameters.getValue( "StateTableID" );
	String StateTableObjectIDColumn = parameters.getValue( "StateTableObjectIDColumn" );
	String StateTableObjectID = parameters.getValue( "StateTableObjectID" );
	String StateTableDateTimeColumn = parameters.getValue( "StateTableDateTimeColumn" );
	//String StateTableNameColumn = parameters.getValue( "StateTableNameColumn" );
	String StateTableValueColumn = parameters.getValue( "StateTableValueColumn" );
	String StateSaveDateTime = parameters.getValue( "StateSaveDateTime" );
	String StateSaveInterval = parameters.getValue( "StateSaveInterval" );
	//String Alias = parameters.getValue( "Alias" );
	
	// TSID - TSID will always be set from the lagK_JDialog when
	// the OK button is pressed, but the user may edit the command without
	// using the lagK_JDialog editor and try to run it, so this
	// method should at least make sure the TSID property is given.

	if ( (TSID == null) || (TSID.length() == 0) ) {
        message = "The time series to process must be specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the identifier for the time series to process." ) );
	}
	else if ( TSID.equalsIgnoreCase( NewTSID ) ) {
		// The new TS must be different from in input TS
        message = "The lagged time series must be different from the input time series.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify different identifiers for the input and lagged time series." ) );
	}

    if ( (NewTSID == null) || NewTSID.equals("") ) {
        message = "The new time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Provide a new time series identifier when defining the command."));
    }
    else {
        try {
            TSIdent tsident = TSIdent.parseIdentifier( NewTSID );
            try {
                TimeInterval.parseInterval(tsident.getInterval());
            }
            catch ( Exception e2 ) {
                message = "NewTSID interval \"" + tsident.getInterval() + "\" is not a valid interval.";
                warning += "\n" + message;
                status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a valid interval when defining the command."));
            }
        }
        catch ( Exception e ) {
            // TODO SAM 2007-03-12 Need to catch a specific exception like
            // InvalidIntervalException so that more intelligent messages can be generated.
            message = "NewTSID \"" + NewTSID + "\" is not a valid identifier." +
            "Use the command editor to enter required fields.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord( CommandStatusType.FAILURE, message,
            "Use the command editor to enter required fields."));
        }
    }
    
    if ( (FlowUnits == null) || FlowUnits.equals("") ) {
        message = "The value for FlowUnits must be specified.";
        warning +="\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "FlowUnits must be specified." ) );
    }
    
    if ( (LagInterval == null) || LagInterval.equals("") ) {
        message = "The value for LagInterval must be specified.";
        warning +="\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "LagInterval must be specified." ) );
    }
    else {
        __LagInterval_TimeInterval = null;
        try {
            __LagInterval_TimeInterval = TimeInterval.parseInterval ( LagInterval );
            if ( __LagInterval_TimeInterval.getMultiplier() != 1 ) {
                message = "The LagInterval (" + LagInterval + ") must be 1.";
                warning +="\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE, message,
                        "Specify a the LagInterval as Minute, Hour, Day (with no multiplier)." ) );
            }
        }
        catch ( Exception e ) {
            message = "The LagInterval is not a valid interval.";
            warning +="\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Specify a valid LagInterval (e.g., Minute, Hour, Day." ) );
        }
    }
	
    // Lag is not required - can attenuate without

    __Lag_Table = null;
	if ( (Lag != null) && !Lag.isEmpty() && (Lag.indexOf("${") < 0) ) {
		warning = parseLagParameter(Lag,warning,status);
	}
	
	// K is not required - can lag without attenuating

	__K_Table = null;
    if ( (K != null) && !K.equals("") && (K.indexOf("${") < 0) ) {
    	warning = parseKParameter(K,warning,status);
    }
    
    if ( (OutputStart != null) && !OutputStart.isEmpty() && !OutputStart.startsWith("${") ) {
        try {
            DateTime.parse(OutputStart);
        }
        catch ( Exception e ) {
            message = "The analysis start date/time \"" + OutputStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time or use ${Property}." ) );
        }
    }
    if ( (OutputEnd != null) && !OutputEnd.isEmpty() && !OutputEnd.startsWith("${") ) {
        try {
            DateTime.parse( OutputEnd );
        }
        catch ( Exception e ) {
            message = "The analysis end date/time \"" + OutputEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time or use ${Property}." ) );
        }
    }

    if ( (InitializeStatesFromTable != null) && !InitializeStatesFromTable.isEmpty() && (InitializeStatesFromTable.indexOf("${")<0) &&
    	!InitializeStatesFromTable.equalsIgnoreCase(_False) && !InitializeStatesFromTable.equalsIgnoreCase(_True)) {
    	if ( !StringUtil.isDouble(InitialLaggedInflow) ) {
            message = "The value for InitializeStatesFromTable (" + InitializeStatesFromTable + ") is invalid.";
            warning +="\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify as " + _False + " (default) or " + _True + "." ) );
    	}
    }
	
    if ( (InitialLaggedInflow != null) && !InitialLaggedInflow.isEmpty() && (InitialLaggedInflow.indexOf("${") < 0) ) {
    	if ( !StringUtil.isDouble(InitialLaggedInflow) ) {
            message = "The value for InitialLaggedInflow (" + InitialLaggedInflow + ") is invalid.";
            warning +="\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the lagged inflow state value default as a number." ) );
    	}
    }
    
    if ( (InitialOutflow != null) && !InitialOutflow.isEmpty() && (InitialOutflow.indexOf("${") < 0) ) {
    	if ( !StringUtil.isDouble(InitialOutflow) ) {
            message = "The value for InitialOutflow (" + InitialOutflow + ") is invalid.";
            warning +="\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the outflow state value default as a number." ) );
    	}
    }
    
    if ( (InitialStorage != null) && !InitialStorage.isEmpty() && (InitialStorage.indexOf("${") < 0) ) {
    	if ( !StringUtil.isDouble(InitialOutflow) ) {
            message = "The value for InitialStorage (" + InitialStorage + ") is invalid.";
            warning +="\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the storage state value default as a number." ) );
    	}
    }
    
	if ( (InitialQTLag != null) && !InitialQTLag.equals("") && (InitialQTLag.indexOf("${") < 0) ) {
		List<String> v = StringUtil.breakStringList ( InitialQTLag, ",", 0 );
	    int size = v.size();
	    for ( int i = 0; i < size; i++ ) {
	        String state = v.get(i);
	        if ( !StringUtil.isDouble(state) ) {
	            message = "The value, position " + (i + 1) + " for InitialQTLag (" + state + ") is not a valid number.";
	            warning +="\n" + message;
	            status.addToLog ( CommandPhaseType.INITIALIZATION,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Specify the inflow state value as a number." ) );
	        }
	    }
	}
	
    // If a state table is specified, make sure all the other necessary parameters are also specified
    
    if ( (StateTableID != null) && !StateTableID.isEmpty() ) {
    	if ( ((StateTableObjectIDColumn != null) && !StateTableObjectIDColumn.isEmpty()) &&
    		((StateTableObjectID == null) || StateTableObjectID.isEmpty()) ) {
	        message = "The StateTableObjectIDColumn is specified but StateTableObjectID is not.";
	        warning +="\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify both StateTableObjectIDColumn and StateTableObjectID (or neither)." ) );
    	}
    	if ( ((StateTableObjectIDColumn == null) || StateTableObjectIDColumn.isEmpty()) &&
    		((StateTableObjectID != null) && !StateTableObjectID.isEmpty()) ) {
	        message = "The StateTableObjectID is specified but StateTableObjectIDColumn is not.";
	        warning +="\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify both StateTableObjectIDColumn and StateTableObjectID (or neither)." ) );
    	}
    	if ( (StateTableDateTimeColumn == null) || StateTableDateTimeColumn.isEmpty() ) {
	        message = "The StateTableDateTimeColumn must be specified.";
	        warning +="\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE, message, "StateTableDateTimeColumn must be specified." ) );
    	}
    	/*
    	if ( (StateTableNameColumn == null) || StateTableNameColumn.isEmpty() ) {
	        message = "The StateTableNameColumn must be specified.";
	        warning +="\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE, message, "StateTableNameColumn must be specified." ) );
    	}
    	*/
    	if ( (StateTableValueColumn == null) || StateTableValueColumn.isEmpty() ) {
	        message = "The StateTableValueColumn must be specified.";
	        warning +="\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE, message, "StateTableValueColumn must be specified." ) );
    	}
    }

    if ( (StateSaveDateTime != null) && !StateSaveDateTime.isEmpty() && (StateSaveDateTime.indexOf("${") < 0) ) {
    	try {
    		DateTime.parse(StateSaveDateTime);
    	}
    	catch ( Exception e ) {
            message = "The value for StateSaveDateTime (" + StateSaveDateTime + ") is invalid.";
            warning +="\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the state save date/time as a valid date/time." ) );
    	}
    }
    
    if ( (StateSaveInterval != null) && !StateSaveInterval.isEmpty() && (StateSaveInterval.indexOf("${") < 0) ) {
    	try {
    		TimeInterval.parseInterval(StateSaveInterval);
    	}
    	catch ( Exception e ) {
            message = "The value for StateSaveInterval (" + StateSaveInterval + ") is invalid.";
            warning +="\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the state save interval as a valid time interval (use choices in editor)." ) );
    	}
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(23);
    validList.add ( "TSID" );
    validList.add ( "FlowUnits" );
    validList.add ( "LagInterval" );
    validList.add ( "Lag" );
    validList.add ( "K" );
    validList.add ( "OutputStart" );
    validList.add ( "OutputEnd" );
    validList.add ( "InitializeStatesFromTable" );
    validList.add ( "InitialLaggedInflow" );
    validList.add ( "InitialOutflow" );
    validList.add ( "InitialStorage" );
    validList.add ( "InitialQTLag" );
    validList.add ( "StateTableID" );
    validList.add ( "StateTableObjectIDColumn" );
    validList.add ( "StateTableObjectID" );
    validList.add ( "StateTableDateTimeColumn" );
    validList.add ( "StateTableNameColumn" );
    validList.add ( "StateTableValueColumn" );
    validList.add ( "StateTableStateName" );
    validList.add ( "StateSaveDateTime" );
    validList.add ( "StateSaveInterval" );
    validList.add ( "NewTSID" );
    validList.add ( "Alias" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, warning_level ),warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
 * Deserialize a states JSON string using Google GSON.
 */
private VariableLagK_States deserializeStatesJSON ( String statesJSON ) {
	Gson gson = new GsonBuilder().create();
	VariableLagK_States states = gson.fromJson(statesJSON, VariableLagK_States.class);
	return states;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	List<String> StateTableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	return ( new VariableLagK_JDialog ( parent, this, StateTableIDChoices ) ).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Get a table (Lag or K) that contains flow values that are in the same units as the time series and
time interval that are the same base unit as the time series.  If the original table is null, return a new
empty table (no Lag or no K).
@param tableType table type being checked, for logging ( "Lag" or "K").
@param ts Time series that will be routed.
@param flowUnits data units for the table (values in column [0]).
@param lagInterval base interval for the table (values in column [1]).
@param originalTable original table provided by the user.
*/
private Table getNormalizedTable ( String tableType, TS ts, String flowUnits, TimeInterval lagInterval, Table originalTable,
    int log_level, String command_tag, int warning_count, CommandStatus status )
{   String routine = getClass().getSimpleName() + ".getNormalizedTable";
    // If the original table is null, return an empty table.  For K, need to initialize so that
    // the storage value (K) is zero, with a large flow being used to allow the lookup.
    // This was implemented after SAM talked to Marc Baldo. 
    if ( originalTable == null ) {
        Table newTable = new Table();
        newTable.allocateDataSpace(1);
        newTable.set(0, BIG_DATA_VALUE, 0.0 );  // Meaning no Lag or no K despite what the input value
        return newTable;
    }
    DataUnitsConversion unitsConversion = null;
    // The units must exactly match to use the existing table - otherwise values need to be converted
    boolean unitsExact = DataUnits.areUnitsStringsCompatible(ts.getDataUnits(), flowUnits, true );
    if ( !unitsExact ) {
        // Verify that conversion factors can be obtained.
        boolean unitsCompatible = DataUnits.areUnitsStringsCompatible(ts.getDataUnits(), flowUnits, false );
        if ( !unitsExact && !unitsCompatible ) {
            // This is a problem
            String message = "The input time series units \"" + ts.getDataUnits() +
            "\" are not compatible with the " + tableType + " table flow value units \"" + flowUnits +
            "\" - cannot convert to use for routing.";
            Message.printWarning(log_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
            routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the time series to lag has flow units compatible with the " +
                    tableType + " table." ) );
            throw new IllegalArgumentException ( message );
        }
        else if ( !unitsExact && unitsCompatible ) {
            // Get the conversion factor on the units.
            try {
                unitsConversion = DataUnits.getConversion(flowUnits, ts.getDataUnits());
            }
            catch ( Exception e ) {
                String message = "Cannot get conversion factor to convert " + tableType + " flow value units \"" +
                flowUnits + " to time series units \"" + ts.getDataUnits() +
                "\" - cannot convert to use for routing.";
                Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the time series to lag has flow units compatible with the " +
                        tableType + " table." ) );
                throw new IllegalArgumentException ( message );
            }
        }
    }
    // The base interval must exactly match to use the existing table - otherwise the interval needs to be converted.
    boolean intervalBaseExact = false;
    if ( lagInterval.getBase() == ts.getDataIntervalBase() ) {
        intervalBaseExact = true;
    }
    // Now either return the original table or do the conversion.
    if ( unitsExact && intervalBaseExact || (originalTable.getNRows() == 0) ) {
        // OK to use the original parameter table
        return originalTable;
    }
    else {
        // Need to create a new table with values that are normalized to the time series.
        Table newTable = new Table();
        int nRows = originalTable.getNRows();
        newTable.allocateDataSpace(nRows);
        double newFlowValue;
        double newTimeValue;
        double flowAddFactor = 0.0;
        double flowMultFactor = 1.0;
        if ( unitsConversion != null ) {
            flowAddFactor = unitsConversion.getAddFactor();
            flowMultFactor = unitsConversion.getMultFactor();
        }
        double timeMultFactor = 1.0; // Convert from original table to time series base interval, 1.0 for no change
        if ( intervalBaseExact ) {
            // No need to do anything
        }
        else if ( (lagInterval.getBase() == TimeInterval.DAY) && (ts.getDataIntervalBase() == TimeInterval.HOUR)) {
            timeMultFactor = lagInterval.getMultiplier()*24.0;
        }
        else if ( (lagInterval.getBase() == TimeInterval.DAY) && (ts.getDataIntervalBase() == TimeInterval.MINUTE)) {
            timeMultFactor = (lagInterval.getMultiplier()*24.0*60.0)/ts.getDataIntervalMult();
        }
        else if ( (lagInterval.getBase() == TimeInterval.HOUR) && (ts.getDataIntervalBase() == TimeInterval.MINUTE)) {
            timeMultFactor = (lagInterval.getMultiplier()*60.0)/ts.getDataIntervalMult();
        }
        else if ( (lagInterval.getBase() == TimeInterval.HOUR) && (ts.getDataIntervalBase() == TimeInterval.DAY)) {
            timeMultFactor = lagInterval.getMultiplier()/(ts.getDataIntervalMult()*24.0);
        }
        else if ( (lagInterval.getBase() == TimeInterval.MINUTE) && (ts.getDataIntervalBase() == TimeInterval.DAY)) {
            timeMultFactor = lagInterval.getMultiplier()/(ts.getDataIntervalMult()*24.0*60.0);
        }
        else if ( (lagInterval.getBase() == TimeInterval.MINUTE) && (ts.getDataIntervalBase() == TimeInterval.HOUR)) {
            timeMultFactor = lagInterval.getMultiplier()/(ts.getDataIntervalMult()*60.0);
        }
        else {
            // If the interval base is not an exact match or a supported ratio of intervals, print a warning.
            if ( !intervalBaseExact ) {
                // This is a problem
                String message = "The input time series interval \"" + ts.getIdentifier().getInterval() +
                "\" cannot be converted to the " + tableType + " table interval values - cannot route.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the time series to lag has interval base that can be converted to the " +
                    tableType + " table (conversion is only implemented for minute/hour/day)." ) );
                throw new IllegalArgumentException ( message );
            }
        }
        Message.printStatus(2, routine, "Normalizing " + tableType + " values using flowAddFactor="+flowAddFactor +
            " flowMultFactor=" + flowMultFactor + " timeMultFactor=" + timeMultFactor );
        for ( int i = 0; i < nRows; i++ ) {
            newFlowValue = flowAddFactor + flowMultFactor*originalTable.get(i,0);
            newTimeValue = timeMultFactor*originalTable.get(i,1);
            newTable.set(i, newFlowValue, newTimeValue );
        }
        return newTable;
    }
}

/**
Return the list of data objects read by this object in discovery mode.
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
    List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discoveryTSList;
    }
    else {
        return null;
    }
}

/**
 * Get the states from the state table.
 * @param stateTable data table with states
 * @param stateTableObjectIDColumnNum table column number (0+) with object ID - if < 0 don't used to lookup states
 * @param stateTableDateTimeColumnNum table column number (0+) with date/time - required
 * @param stateTableNameColumnNum table column number (0+) with state name - if < 0 don't use to lookup states
 * @param stateTableValueColumnNum table column number (0+) with state value - required
 * @param objectID object ID to match if using to look up states (not used if object ID column was not specified)
 * @param dt date/time to look up state - required
 * @param stateName name of state to look up (not used of state name column was not specified)
 * @return the JSON states string.
 */
private VariableLagK_States getStatesFromTable(DataTable stateTable, int stateTableObjectIDColumnNum, int stateTableDateTimeColumnNum,
	int stateTableNameColumnNum, int stateTableValueColumnNum,
	String objectID, DateTime dt, String stateName )
throws Exception {
	String routine = getClass().getSimpleName() + ".getStatesFromTable";
	// See if an existing table record exists
	int [] columnNumbers = new int[3];
	List<Object> columnValues = new ArrayList<Object>();
	int columnCount = 0;
	if ( stateTableObjectIDColumnNum >= 0 ) {
		// Lookup using object ID if specified
		columnNumbers[columnCount++] = stateTableObjectIDColumnNum;
		columnValues.add(objectID);
	}
	// Date/time always used
	columnNumbers[columnCount++] = stateTableDateTimeColumnNum;
	columnValues.add(dt);
	if ( stateTableNameColumnNum >= 0 ) {
		// Lookup using state name if specified
		columnNumbers[columnCount++] = stateTableNameColumnNum;
		columnValues.add(stateName);
	}
	// Resize array if necessary because some columns may not have been used for lookup...
	if ( columnCount != 3 ) {
		int [] columnNumbers2 = new int[columnCount];
		System.arraycopy(columnNumbers, 0, columnNumbers2, 0, columnCount);
		columnNumbers = columnNumbers2;
	}
	List<TableRecord> records = stateTable.getRecords(columnNumbers, columnValues);
	if ( records.size() != 1 ) {
		// If more than one record it is a problem
		throw new RuntimeException("State table contains " + records.size() + " records (1 expected) for ObjectID=\""
			+ objectID + "\" date/time=\"" + dt + "\" state name=\"" + stateName + "\"");
	}
	TableRecord rec = records.get(0);
	String statesJSON = rec.getFieldValueString(stateTableValueColumnNum);
	if ( Message.isDebugOn ) {
		Message.printStatus(2,routine,"Read states: " + statesJSON );
	}
	// Convert the states JSON string into in-memory objects that can be queried
	return deserializeStatesJSON ( statesJSON );
}

/**
Determine whether states should be saved by checking the current processing date/time against requested state save date/time.
@param dt date/time to check, corresponding to processing iterator
@param stateSaveDateTime a requested date/time to save the states, typically a single end of run date/time
@param stateSaveInterval interval on which to save states, for example if Day, save at midnight each day
@return true if the states need to be saved at the specified date/time, false if no need to save
*/
private boolean needToSaveStates(DateTime dt,DateTime stateSaveDateTime,TimeInterval stateSaveInterval,DateTime intervalDateTime) {
	if ( stateSaveDateTime != null ) {
		// Check whether the current date/time matches the requested save date/time
		if ( dt.equals(stateSaveDateTime) ) {
			return true;
		}
	}
	if ( stateSaveInterval != null ) {
		// Check whether the current date/time matches the requested save interval.
		// For example if the requested save interval is Day then the full precision save date/time would be YYYY-MM-DD 00:00:00
		// The intervalDateTime object is re-used by setting the start 
		if ( (stateSaveInterval.getBase() == TimeInterval.DAY) && (stateSaveInterval.getMultiplier() == 1) ) {
			if ( (dt.getHour() == intervalDateTime.getHour()) &&
				(dt.getMinute() == intervalDateTime.getMinute()) &&
				(dt.getSecond() == intervalDateTime.getSecond()) ) {
				return true;
			}
		}
		else if ( (stateSaveInterval.getBase() == TimeInterval.HOUR) && (stateSaveInterval.getMultiplier() == 1) ) {
			if ( (dt.getMinute() == intervalDateTime.getMinute()) &&
				(dt.getMinute() == intervalDateTime.getSecond()) ) {
				return true;
			}
		}
		else {
			// Don't know how to handle.  Throw an exception
			throw new RuntimeException ( "Don't know how to handle StateSaveInterval " + stateSaveInterval);
		}
	}
	return false;
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{   // This is only needed to translate older properties that have been renamed.
    // Parse with parent
    super.parseCommand( command_string);
    // Now convert DataUnits to FlowUnits.
    PropList parameters = getCommandParameters();
    String DataUnits = parameters.getValue ( "DataUnits");
    if ( DataUnits != null ) {
        parameters.set("FlowUnits", DataUnits);
        parameters.unSet(DataUnits);
    }
}

private String parseKParameter ( String K, String warning, CommandStatus status ) {
	String message;
    // If pair values are specified, make sure they are a sequence of numbers.  Save the results
    // for use when running the command.
    List<String> tokens = StringUtil.breakStringList(K, " ,;", StringUtil.DELIM_SKIP_BLANKS);
    int size = 0;
    if ( tokens != null ) {
        size = tokens.size();
    }
    if ( (size%2) != 0 ) {
        message = "The K values (" + K + ") are not specified as pairs.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
            	"Provide lag pairs as:  flow1,k1;flow2,k2;flow3,k3 as pairs"));
    }
    else {
        // Verify that each value in the pair is a number and set the pair in the table
        // Also verify that the flow values are in increasing order.
        __K_Table = new Table();
        __K_Table.allocateDataSpace(size/2);
        String token;
        double value = 0.0, k = 0.0; // "value" needs to stick around because it is saved with lag
        double value_prev = 0.0;
        for ( int i = 0; i < size; i++ ) {
            token = tokens.get(i).trim();
            if ( !StringUtil.isDouble(token) ) {
                if ( (i%2) == 0 ) {
                    message = "K table flow value (" + token + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog(CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE, message,"Provide a valid number."));
                }
                else {
                    message = "K value (" + token + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog(CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(
                        CommandStatusType.FAILURE, message,
                        "Provide a valid number."));
                }
            }
            else {
                if ( (i%2) == 0 ) {
                    value = Double.parseDouble(token);
                }
                else {
                    k = Double.parseDouble(token);
                    // Have processed the value and k so set in the table.
                    __K_Table.set(i/2, value, k);
                    if ( (i > 1) && (value <= value_prev) ) {
                        // Verify that the flow value is larger than the previous value
                        message = "Flow value (" + StringUtil.formatString(value,"%.4f") +
                            ") is <= previous flow value in the K table.";
                        warning += "\n" + message;
                        status.addToLog(CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE, message,
                            "Verify that flow values are in increasing order in the K table."));
                    }
                    value_prev = value;
                }
            }
        }
    }
    return warning;
}

/**
 * Parse the lag parameter.
 * @param Lag the Lag parameter value
 * @param warning the cumulative warning string when initializing
 * @param status command status
 */
private String parseLagParameter ( String Lag, String warning, CommandStatus status ) {
	String message;
    int countLagPositive = 0;
    int countLagNegative = 0;
	// If pair values are specified, make sure they are a sequence of numbers.  Save the results
    // for use when running the command.
	List<String> tokens = StringUtil.breakStringList(Lag, " ,;", StringUtil.DELIM_SKIP_BLANKS);
	int size = 0;
	if ( tokens != null ) {
		size = tokens.size();
	}
	if ( (size%2) != 0 ) {
        message = "The Lag values (" + Lag + ") are not specified as pairs.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Provide lag pairs as:  flow1,lag1;flow2,lag2;flow3,lag3 as pairs."));
	}
	else {
	    // Verify that each value in the pair is a number and set the pair in the table.
	    // Also verify that the flow values are in increasing order.
	    __Lag_Table = new Table();
	    __Lag_Table.allocateDataSpace(size/2);
		String token;
        double flowValue = 0.0, lag = 0.0; // "value" needs to stick around because it is saved with lag
        double value_prev = 0.0;
		for ( int i = 0; i < size; i++ ) {
			token = tokens.get(i).trim();
			if ( !StringUtil.isDouble(token) ) {
			    if ( (i%2) == 0 ) {
                    message = "Lag table flow value (" + token + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog(CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE, message,"Provide a valid number."));
                }
			    else {
                    message = "Lag value (" + token + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog(CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE, message,"Provide a valid number."));
                }
            }
			else {
			    if ( (i%2) == 0 ) {
			        flowValue = Double.parseDouble(token);
			    }
			    else {
			        lag = Double.parseDouble(token);
                    if ( lag > 0.0 ) {
                        ++countLagPositive;
                    }
                    else if ( lag < 0.0 ) {
                        ++countLagNegative;
                    }
			        // Have processed the value and lag so set in the table.
			        __Lag_Table.set(i/2, flowValue, lag);
                    if ( (i > 1) && (flowValue <= value_prev) ) {
                        // Verify that the flow value is larger than the previous value
                        message = "Flow value (" + StringUtil.formatString(flowValue,"%.4f") +
                            ") is <= previous flow value in the Lag table.";
                        warning += "\n" + message;
                        status.addToLog(CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE, message,
                            "Verify that flow values are in increasing order in the Lag table."));
                    }
                    value_prev = flowValue;
			    }
			}
		}
	}
	// Make sure that lag values are either all positive or all negative
	if ( (countLagNegative > 0) && (countLagPositive > 0) ) {
        message = "Negative and positive lag values cannot be used together.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Specify lag values as all <= 0 or all >= 0."));
	}
	return warning;
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message = "";
    //int dl = 10;
	int log_level = 3;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
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
	
	String TSID = parameters.getValue( "TSID"  );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) && !TSID.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
	String NewTSID = parameters.getValue( "NewTSID"  );
	if ( (NewTSID != null) && (NewTSID.indexOf("${") >= 0) && !NewTSID.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		NewTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, NewTSID);
	}
    String LagInterval = parameters.getValue( "LagInterval" );
	if ( (LagInterval != null) && (LagInterval.indexOf("${") >= 0) && !LagInterval.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		LagInterval = TSCommandProcessorUtil.expandParameterValue(processor, this, LagInterval);
	}
    String FlowUnits = parameters.getValue( "FlowUnits" );
	if ( (FlowUnits != null) && (FlowUnits.indexOf("${") >= 0) && !FlowUnits.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		FlowUnits = TSCommandProcessorUtil.expandParameterValue(processor, this, FlowUnits);
	}
	String Lag = parameters.getValue( "Lag" );
	if ( (Lag != null) && (Lag.indexOf("${") >= 0) && !Lag.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		Lag = TSCommandProcessorUtil.expandParameterValue(processor, this, Lag);
	}
	if ( (Lag != null) && !Lag.isEmpty() && (commandPhase == CommandPhaseType.RUN) ) {
		// Have to reparse the array at runtime
		String warning = "";
		parseLagParameter(Lag,warning,status);
	}
    String K = parameters.getValue( "K" );
	if ( (K != null) && (K.indexOf("${") >= 0) && !K.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		K = TSCommandProcessorUtil.expandParameterValue(processor, this, K);
	}
	if ( (K != null) && !K.isEmpty() && (commandPhase == CommandPhaseType.RUN) ) {
		// Have to reparse the array at runtime
		String warning = "";
		parseKParameter(K,warning,status);
	}
    String OutputStart = parameters.getValue ( "OutputStart" ); // Expand below
    if ( (OutputStart == null) || OutputStart.isEmpty() ) {
    	OutputStart = "${OutputStart}"; // Default global property
    }
    String OutputEnd = parameters.getValue ( "OutputEnd" ); // Expand below
    if ( (OutputEnd == null) || OutputEnd.isEmpty() ) {
    	OutputEnd = "${OutputEnd}"; // Default global property
    }
    String InitializeStatesFromTable = parameters.getValue ( "InitializeStatesFromTable" );
	if ( (InitializeStatesFromTable != null) && (InitializeStatesFromTable.indexOf("${") >= 0) &&
		!InitializeStatesFromTable.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		InitializeStatesFromTable = TSCommandProcessorUtil.expandParameterValue(processor, this, InitializeStatesFromTable);
	}
    boolean initializeStatesFromTable = false;
    if ( (InitializeStatesFromTable != null) && !InitializeStatesFromTable.isEmpty() ) {
    	if ( InitializeStatesFromTable.equalsIgnoreCase(_True) ) {
    		initializeStatesFromTable = true;
    	}
    }
    String InitialLaggedInflow = parameters.getValue ( "InitialLaggedInflow" );
    Double initialLaggedInflow = null;
	if ( (InitialLaggedInflow != null) && (InitialLaggedInflow.indexOf("${") >= 0) && !InitialLaggedInflow.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		InitialLaggedInflow = TSCommandProcessorUtil.expandParameterValue(processor, this, InitialLaggedInflow);
	}
    if ( (InitialLaggedInflow != null) && !InitialLaggedInflow.isEmpty() ) {
    	initialLaggedInflow = Double.valueOf(InitialLaggedInflow);
    }
    String InitialOutflow = parameters.getValue ( "InitialOutflow" );
    Double initialOutflow = null;
	if ( (InitialOutflow != null) && (InitialOutflow.indexOf("${") >= 0) && !InitialOutflow.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		InitialOutflow = TSCommandProcessorUtil.expandParameterValue(processor, this, InitialOutflow);
	}
    if ( (InitialOutflow != null) && !InitialOutflow.isEmpty() ) {
    	initialOutflow = Double.valueOf(InitialOutflow);
    }
    String InitialStorage = parameters.getValue ( "InitialStorage" );
	if ( (InitialStorage != null) && (InitialStorage.indexOf("${") >= 0) && !InitialStorage.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		InitialStorage = TSCommandProcessorUtil.expandParameterValue(processor, this, InitialStorage);
	}
    Double initialStorage = null;
    if ( (InitialStorage != null) && !InitialOutflow.isEmpty() ) {
    	initialStorage = Double.valueOf(InitialStorage);
    }
    String InitialQTLag = parameters.getValue ( "InitialQTLag" );
    double [] initialQTLag = null;
	if ( (InitialQTLag != null) && (InitialQTLag.indexOf("${") >= 0) && !InitialQTLag.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		InitialQTLag = TSCommandProcessorUtil.expandParameterValue(processor, this, InitialQTLag);
	}
    if ( (InitialQTLag != null) && !InitialQTLag.isEmpty() ) {
    	String [] parts = InitialQTLag.split(",");
    	initialQTLag = new double[parts.length];
    	for ( int i = 0; i < parts.length; i++ ) {
    		initialQTLag[i] = Double.valueOf(parts[i].trim());
    	}
    }
    String StateSaveDateTime = parameters.getValue ( "StateSaveDateTime" );
    DateTime stateSaveDateTime = null;
	if ( (StateSaveDateTime != null) && (StateSaveDateTime.indexOf("${") >= 0) &&
		!StateSaveDateTime.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		StateSaveDateTime = TSCommandProcessorUtil.expandParameterValue(processor, this, StateSaveDateTime);
	}
    if ( (StateSaveDateTime != null) && !StateSaveDateTime.isEmpty() &&
    	(StateSaveDateTime.indexOf("${") < 0) && (commandPhase == CommandPhaseType.RUN)) {
    	stateSaveDateTime = DateTime.parse(StateSaveDateTime);
    }
    String StateSaveInterval = parameters.getValue ( "StateSaveInterval" );
	if ( (StateSaveInterval != null) && (StateSaveInterval.indexOf("${") >= 0) &&
		!StateSaveInterval.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		StateSaveInterval = TSCommandProcessorUtil.expandParameterValue(processor, this, StateSaveInterval);
	}
    TimeInterval stateSaveInterval = null;
    DateTime stateSaveIntervalDateTime = null;
    if ( (StateSaveInterval != null) && !StateSaveInterval.isEmpty() &&
    	(StateSaveInterval.indexOf("${") < 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	stateSaveInterval = TimeInterval.parseInterval(StateSaveInterval);
    }
    String StateTableID = parameters.getValue ( "StateTableID" );
    boolean haveStateTable = false; // Whether a state table is available, meaning the state table name was given and therefore should be used
    if ( (StateTableID != null) && !StateTableID.isEmpty() ) {
    	haveStateTable = true;
    	if ( (commandPhase == CommandPhaseType.RUN) && (StateTableID.indexOf("${") >= 0) ) {
    		// In discovery mode want lists of tables to include ${Property}
    		StateTableID = TSCommandProcessorUtil.expandParameterValue(processor, this, StateTableID);
    	}
    }
    String StateTableObjectIDColumn = parameters.getValue ( "StateTableObjectIDColumn" );
    if ( (StateTableObjectIDColumn != null) && (StateTableObjectIDColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	StateTableObjectIDColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, StateTableObjectIDColumn);
	}
    String StateTableObjectID = parameters.getValue ( "StateTableObjectID" );
    if ( (StateTableObjectID != null) && (StateTableObjectID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	StateTableObjectID = TSCommandProcessorUtil.expandParameterValue(processor, this, StateTableObjectID);
	}
    String StateTableDateTimeColumn = parameters.getValue ( "StateTableDateTimeColumn" );
    if ( (StateTableDateTimeColumn != null) && (StateTableDateTimeColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	StateTableDateTimeColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, StateTableDateTimeColumn);
	}
    String StateTableNameColumn = parameters.getValue ( "StateTableNameColumn" );
    if ( (StateTableNameColumn != null) && (StateTableNameColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	StateTableNameColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, StateTableNameColumn);
	}
    String StateTableValueColumn = parameters.getValue ( "StateTableValueColumn" );
    if ( (StateTableValueColumn != null) && (StateTableValueColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	StateTableValueColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, StateTableValueColumn);
	}
    String StateTableStateName = parameters.getValue ( "StateTableStateName" );
    if ( (StateTableStateName != null) && (StateTableStateName.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	StateTableStateName = TSCommandProcessorUtil.expandParameterValue(processor, this, StateTableStateName);
	}
	String Alias = parameters.getValue( "Alias" ); // Expanded below
	
	TS original_ts = null; // Original time series
	TS result_ts = null; // Result (lagged) time series
	
    // Figure out the dates to use for the analysis.
    // Default of null means to analyze the full period.
    DateTime OutputStart_DateTime = null;
    DateTime OutputEnd_DateTime = null;
    
    if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			OutputStart_DateTime = TSCommandProcessorUtil.getDateTime ( OutputStart, "OutputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			OutputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( OutputEnd, "OutputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
    }
	
    // Get the state table to process.

    DataTable stateTable = null;
    if ( haveStateTable ) {
	    PropList request_params = null;
	    CommandProcessorRequestResultsBean bean = null;
	    if ( commandPhase == CommandPhaseType.RUN ) {
		    if ( (StateTableID != null) && !StateTableID.isEmpty() ) {
		        // Get the table to be updated/created
		        request_params = new PropList ( "" );
		        request_params.set ( "TableID", StateTableID );
		        try {
		            bean = processor.processRequest( "GetTable", request_params);
		            PropList bean_PropList = bean.getResultsPropList();
		            Object o_Table = bean_PropList.getContents ( "Table" );
		            if ( o_Table != null ) {
		                // Found the table
		                stateTable = (DataTable)o_Table;
		            }
		        }
		        catch ( Exception e ) {
		            message = "Error requesting GetTable(StateTableID=\"" + StateTableID + "\") from processor.";
		            Message.printWarning(warning_level,
		                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
		            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
		                message, "Report problem to software support." ) );
		        }
		    }
	        if ( stateTable == null ) {
	            message = "Specified state table \"" + StateTableID + "\" was not found.";
	            Message.printWarning(warning_level,
	                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
	            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Verify that the state table \"" + StateTableID + "\" is available." ) );
	        }
	    }
    }
	
    // The final check on input errors.

    if ( warning_count > 0 ) {
        // Input error (e.g., missing time series)...
        message = "There were " + warning_count + " errors initializing data to run the command.";  
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Correct command parameters/input." ) );
        throw new InvalidCommandParameterException ( message );
    }
    
	// Lag the time series...
	try {
		// Create the output time series. It should have the same units and interval as the input time series.
		// Do this here so that discovery mode will have access to the time series, but set other properties
	    // of the time series in the run block below.
		result_ts = TSUtil.newTimeSeries ( NewTSID, true );
		result_ts.setIdentifier(NewTSID);
		if ( (Alias != null) && !Alias.equals("") ) {
		    result_ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                processor, result_ts, Alias, status, commandPhase) );
		}
        if ( commandPhase == CommandPhaseType.RUN ) {
            boolean canRun = true; // Use to help with graceful error handling
            int StateTableObjectIDColumnNum = -1;
        	int StateTableDateTimeColumnNum = -1;
        	int StateTableNameColumnNum = -1;
        	int StateTableValueColumnNum = -1;
	    	if ( haveStateTable ) {
	    	    // Get the table columns and other needed data
	    		if ( (StateTableObjectIDColumn != null) && !StateTableObjectIDColumn.isEmpty() ) {
	                try {
	                    StateTableObjectIDColumnNum = stateTable.getFieldIndex(StateTableObjectIDColumn);
	                }
	                catch ( Exception e2 ) {
	                    message = "Did not match StateTableObjectIDColumn \"" + StateTableObjectIDColumn +
	                        "\" as table column - cannot run."; 
	                    Message.printWarning ( warning_level,
		                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	                    status.addToLog ( commandPhase,
	                        new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Correct command parameters/input." ) );
	                    canRun = false;
	                }
	    		}
                try {
                    StateTableDateTimeColumnNum = stateTable.getFieldIndex(StateTableDateTimeColumn);
                    if ( stateTable.getFieldDataType(StateTableDateTimeColumnNum) != TableField.DATA_TYPE_DATETIME ) {
                    	 message = "State table date/time column \"" + StateTableDateTimeColumn +
                             "\" is not of type date/time and may not allow state lookups.";
                         Message.printWarning ( warning_level,
     	                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                         status.addToLog ( commandPhase,
                             new CommandLogRecord(CommandStatusType.WARNING,
                                 message, "Verify that the column is read as a date/time." ) );
                    }
                }
                catch ( Exception e2 ) {
                    message = "Did not match StateTableDateTimeColumn \"" + StateTableDateTimeColumn +
                        "\" as table column - cannot run.";
                    Message.printWarning ( warning_level,
	                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Correct command parameters/input." ) );
                    canRun = false;
                }
                if ( (StateTableNameColumn != null) && !StateTableNameColumn.isEmpty() ) {
	                try {
	                    StateTableNameColumnNum = stateTable.getFieldIndex(StateTableNameColumn);
	                }
	                catch ( Exception e2 ) {
	                    message = "Did not match StateTableNameColumn \"" + StateTableNameColumn +
	                        "\" as table column - cannot run.";
	                    Message.printWarning ( warning_level,
    	                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Correct command parameters/input." ) );
	                    canRun = false;
	                }
                }
                try {
                    StateTableValueColumnNum = stateTable.getFieldIndex(StateTableValueColumn);
                }
                catch ( Exception e2 ) {
                    message = "Did not match StateTableValueColumn \"" + StateTableValueColumn +
                        "\" as table column - cannot run.";
                    Message.printWarning ( warning_level,
	                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Correct command parameters/input." ) );
                    canRun = false;
                }
	    	}
            //Get the reference (original_ts) to the time series to route
            
            PropList request_params = new PropList ( "" );
            request_params.set ( "TSID", TSID );
            CommandProcessorRequestResultsBean bean = null;
            int ts_pos = -1;    // No time series found
            try {
                bean = processor.processRequest( "IndexOf", request_params);
                PropList bean_PropList = bean.getResultsPropList();
                Object o_Index = bean_PropList.getContents ( "Index" );
                if ( o_Index == null ) {
                    Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, "Null value for IndexOf(TSID=" + TSID + ") returned from processor." );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the time series to lag matches an available time series." ) );
                }
                else {
                    ts_pos = ((Integer)o_Index).intValue();
                }
            }
            catch ( Exception e ) {
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, "Error requesting IndexOf(TSID=" + TSID + ") from processor." );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
            
            try {
                if ( ts_pos < 0 ) {
                    message = "The time series \"" + TSID + "\" was not defined in a previous command.";
                    Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the time series to lag matches an available time series." ) );
                }
                else {      
                    request_params = new PropList ( "" );
                    request_params.setUsingObject ( "Index", new Integer(ts_pos) );
                    bean = null;
                    try {
                        bean = processor.processRequest( "GetTimeSeries", request_params);
                    }
                    catch ( Exception e ) {
                        Message.printWarning(log_level,
                            MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            routine, "Error requesting GetTimeSeries(Index=" + ts_pos + "\") from processor." );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
                    }
                    PropList bean_PropList = bean.getResultsPropList();
                    Object prop_contents = bean_PropList.getContents ( "TS" );
                    if ( prop_contents == null ) {
                        message = "Null value for GetTimeSeries(Index=" + ts_pos + ") returned from processor.";
                        Message.printWarning(warning_level,
                            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the time series to lag matches an available time series." ) );
                    }
                    else {
                        original_ts = (TS)prop_contents;
                    }
                }
                
            } catch ( Exception e ) {
                message = "Unexpected error getting the time series to route.";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(
                command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the time series to lag matches an available time series." ) );
            }
                    
            // Verify that the interval of the input time series is the same as the output time series.
            
            if ( (original_ts.getDataIntervalBase() != result_ts.getDataIntervalBase()) ||
                (original_ts.getDataIntervalMult() != result_ts.getDataIntervalMult())) {
                message = "The input time series interval \"" + original_ts.getIdentifier().getInterval() +
                "\" is not the same as the output time series interval \"" + result_ts.getIdentifier().getInterval() +
                "\" - cannot route.";  
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                // This is just a warning
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the NewTSID interval is the same as the input time series." ) );
            }
            else if ( original_ts.getDataIntervalBase() == TimeInterval.IRREGULAR ) {
                message = "The input time series has irregular interval - cannot route.";  
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                // This is just a warning
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Convert the time series to a regular interval before routing." ) );
            }
            else {
                // Set additional properties on the output time series
                if ( original_ts != null ) {
                    result_ts.setDataUnits ( original_ts.getDataUnits() );
                    result_ts.setDataUnitsOriginal ( original_ts.getDataUnits() );
                    if ( OutputStart_DateTime == null ) {
                    	OutputStart_DateTime = new DateTime(original_ts.getDate1());
                    }
                    result_ts.setDate1 ( OutputStart_DateTime );
                    result_ts.setDate1Original ( OutputStart_DateTime );
                    if ( OutputEnd_DateTime == null ) {
                    	OutputEnd_DateTime = new DateTime(original_ts.getDate2());
                    }
                    result_ts.setDate2 ( OutputEnd_DateTime );
                    result_ts.setDate2Original ( OutputEnd_DateTime );
                    // Allocate memory
                    result_ts.allocateDataSpace();
                }
                
                // Make sure that the Lag table is in the same units as the time series being lagged
                Table Lag_Table = null;
                try {
                    Lag_Table = getNormalizedTable ( "Lag", original_ts, FlowUnits, __LagInterval_TimeInterval, __Lag_Table,
                    	log_level, command_tag, warning_count, status );
                }
                catch ( Exception e ) {
                    canRun = false;
                }
                // Make sure that the K table is in the same base interval as the time series being lagged
                Table K_Table = null;
                try {
                    K_Table = getNormalizedTable ( "K", original_ts, FlowUnits, __LagInterval_TimeInterval, __K_Table,
                      log_level, command_tag, warning_count, status );
                }
                catch ( Exception e ) {
                    canRun = false;
                }
                
                if ( canRun ) {
                    // OK to continue processing...
                    String units = FlowUnits;
                    if ( (units == null) || units.equals("") ) {
                        units = original_ts.getDataUnits();
                    }
                    Message.printStatus(2, routine, "Lag table used for procesing (" + units + "):\n" + Lag_Table );
                    Message.printStatus(2, routine, "K table used for procesing (" + units + "):\n" + K_Table );
                    
            	    // TODO SAM 2009-03-24 Paste in the Sentry logic below, but may want to put this in the LagK class
            	    TSIterator tsi = original_ts.iterator(); // Iterate through entire input time series
            	    TSData tsd = null;
            		LagKBuilder lkb = new LagKBuilder(original_ts);
            		lkb.setLagIn ( Lag_Table );
            		lkb.setKOut ( K_Table );
            		double [] coInitial = null; // Will cause states to initialize to zero
            		Double coLaggedInflow = null, coOutflow = null, coStorage = null;
            	    boolean doInitialStates = true; // Whether the initial state parameters should be used (modified depending on state table)
            		if ( haveStateTable && initializeStatesFromTable ) {
                		doInitialStates = false; // Use what is in the table unless there is an issue below
            			// New approach to getting states is to read from the state table
                		// States are encoded string with JSON text, in particular the qt array:
                		// { "currentLaggedInflow": 123.46,
                		//   "currentOutflow": 123.45,
                		//   "currentStorage": 123.45,
                		//   "qt": [
            			//      { "q": 123.45, "t": "0" },
            			//      { "q": 123.45, "t": "1" },
            			//      { ... }
                		//   ]
                		// }
            			VariableLagK_States states = null;
            			try {
            				states = getStatesFromTable(stateTable, StateTableObjectIDColumnNum, StateTableDateTimeColumnNum,
	        					StateTableNameColumnNum, StateTableValueColumnNum,
	        					StateTableObjectID, OutputStart_DateTime, StateTableStateName );
            				// The states will be null if not read from the JSON
                			coLaggedInflow = states.getCurrentLaggedInflow();
                			coOutflow = states.getCurrentOutflow();
                			coStorage = states.getCurrentStorage();
                			coInitial = states.getQtLag();
                			// TODO SAM 2016-08-29 Check the states against the input time series to make sure they are compatible
                			// Make sure that lag interval is the same as the input time series
                			if ( (states.getLagInterval() != null) && !states.getLagInterval().isEmpty() ) {
                				TimeInterval stateLagInterval = TimeInterval.parseInterval(states.getLagInterval());
                				if ( (stateLagInterval.getBase() != original_ts.getDataIntervalBase()) ||
                					(stateLagInterval.getMultiplier() != original_ts.getDataIntervalMult()) ) {
                					// Major problem that indicates data management issue
                                    message = "States retrieved from table have lag interval \"" + states.getLagInterval()
                                    	+ "\" but input time series interval is \"" + original_ts.getIdentifier().getInterval() + "\" - ignoring.";  
                                    Message.printWarning ( warning_level,
                                        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                                    status.addToLog ( commandPhase,
                                        new CommandLogRecord(CommandStatusType.FAILURE,
                                            message, "Verify that states were saved for inflow time series with proper interval." ) );
                                    doInitialStates = true;
                				}
                			}
            			}
            			catch ( Exception e ) {
                            message = "Unable to retrieve states at " + OutputStart_DateTime + ".  Will use initial states if provided.";  
                            Message.printWarning ( warning_level,
                                MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                            Message.printWarning(3,routine,e);
                            status.addToLog ( commandPhase,
                                new CommandLogRecord(CommandStatusType.WARNING,
                                    message, "Verify that states were saved at " + OutputStart_DateTime ) );
                            doInitialStates = true;
            			}
            		}
            		if ( doInitialStates ) {
            			// Try to use the initial states if they were provided (OK if values are null because handled in "create" method below)
            			coLaggedInflow = initialLaggedInflow;
            			coOutflow = initialOutflow;
            			coStorage = initialStorage;
            			coInitial = initialQTLag;
            		}
            		riverside.ts.routing.lagk.LagK lagK = lkb.create(coLaggedInflow,coOutflow,coStorage,coInitial);
                    double previousOutflow = 0.0;
            		StringBuffer b = new StringBuffer();
                    double [] co0 = lagK.getCarryoverValues();
                    for ( int i = 0; i < co0.length; i++ ) {
                        b.append ( StringUtil.formatString(co0[i],"%.4f") + " " );
                    }
                    if ( Message.isDebugOn ) {
                        Message.printDebug(1, routine, "Initial input carryover values co=" + b.toString());
                    }
            		
                    // TODO SAM 2009-03-24 evaluate input states - for now do not use and accept defaults (0)
                    //reinstate(lagK);

                    //final boolean verbal = verbal();
                    // TODO SAM 2009-03-24 What does the following do?  Position the arrays?
                    /*
                    while ((tsd = tsi.next()) != null) {
                        if (!original_ts.isDataMissing(tsd.getData())) {
                            tsi.previous();
                            break;
                        }
                    }
                    */
                    b = new StringBuffer();
                    DateTime dt = null;
                    // The following is necessary to initialize for negative lag.
                    // It repositions the initial time of the loop and initializes carryover because of the negative lag.
                    // If there is no negative lag, it does not do anything to the Date/time
                    lagK.initializeCarryoverForNegativeLag (tsi);
                    TSIterator tso = original_ts.iterator(); // Same as tsi when positive lag, different when negative lag
                    if ( stateSaveInterval != null ) {
	                	// Create a DateTime instance that will be reused when checking
	                	// processing DateTime for whether to save.
	                	// Create to precision of the time series
	                	stateSaveIntervalDateTime = new DateTime(original_ts.getDate1());
	                	// Set the end of the date/time based on the save interval
	                	// - currently can only save at midnight for Day and minute 0 for Hour interval
	                	if ( stateSaveInterval.getBase() == TimeInterval.DAY ) {
	                		// Will check that hour, minute, and second are 0 later so set here
	                		stateSaveIntervalDateTime.setSecond(0);
	                		stateSaveIntervalDateTime.setMinute(0);
	                		stateSaveIntervalDateTime.setHour(0);
	                	}
	                	if ( stateSaveInterval.getBase() == TimeInterval.HOUR ) {
	                		// Will check that minute, and second are 0 later so set here
	                		stateSaveIntervalDateTime.setSecond(0);
	                		stateSaveIntervalDateTime.setMinute(0);
	                	}
                    }
                    // Process the input to create the routed time series
                    while ((tsd = tsi.next()) != null) {
                        tso.next();
                        dt = tsd.getDate();
                        double dataIn = tsd.getDataValue();
                        double lagVal = lagK.solveMethod(tsd.getDate(), previousOutflow);
                        lagK.doCarryOver(tsd.getDate());
                        b.setLength(0);
                        // Only using for debugging...
                        //double [] co = lagK.getCarryOverValues(tsd.getDate());
                        double [] co = lagK.getCarryoverValues();
                        for ( int i = 0; i < co.length; i++ ) {
                            b.append ( StringUtil.formatString(co[i],"%.4f") + " " );
                        }
                        if ( Message.isDebugOn ) {
                            Message.printDebug(1, routine, "Time: " + tsd.getDate() + " in=" + dataIn + " routed=" + lagVal
                                + " co=" + b.toString());
                        }
                        // TODO SAM 2009-03-29 Evaluate removing - we're trying to make it work
                        //prev = dataIn;
                        previousOutflow = lagVal;
                        // TODO SAM 2009-03-29 Evaluate removing - lagged value from solveMethod() is for current time
                        //DateTime future = new DateTime(tsd.getDate());
                        //future.addInterval(original_ts.getDataIntervalBase(), original_ts.getDataIntervalMult());
                        //result_ts.setDataValue(future, lagVal);
                        result_ts.setDataValue(tso.getDate(), lagVal);
                        if (original_ts.isDataMissing(dataIn)) {
                            message = "The input time series has missing data at " + dt +
                            " - unable to route time series at and beyond this date/time.";  
                            Message.printWarning ( warning_level,
                                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                            // This is just a warning
                            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Fill the time series to be routed before trying to route." ) );
                            break;
                        }
                        if ( dataIn < 0.0 ) {
                            message = "The input time series has a negative data value at " + dt +
                            " - unable to route time series at and beyond this date/time.";  
                            Message.printWarning ( warning_level,
                                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                            // This is just a warning
                            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Adjust negative values before trying to route." ) );
                            break;
                        }
                        // If the date/time matches requested save date/time, write to the state table.
                        // Make sure to overwrite if the record is matched
                        if ( needToSaveStates(dt,stateSaveDateTime,stateSaveInterval,stateSaveIntervalDateTime) ) {
                        	Message.printStatus(2,routine,"Saving states at " + dt );
	                        try {
	                        	saveStatesToTable(stateTable,StateTableObjectIDColumnNum,StateTableDateTimeColumnNum,
	                        		StateTableNameColumnNum,StateTableValueColumnNum,
	                        		StateTableObjectID,dt,
	                        		StateTableStateName, FlowUnits, original_ts.getIdentifier().getInterval(),
	                        		lagK.getLaggedInflow(), lagVal, lagK.getStorageCarryOver(), co );
	                        }
	                        catch ( Exception e ) {
	                            message = "Unable to save states at " + dt + " (" + e + ") - will not be able to restart on date/time with states.";  
                                Message.printWarning ( warning_level,
                                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                                // This is just a warning
                                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                                    message, "Contact software support to troubleshoot." ) );
	                        }
                        }
                    } // End of time loop
            
            		// Update the newly created time series genesis.
            		result_ts.addToGenesis ( "Routed data from " + original_ts.getIdentifierString());
            		result_ts.addToGenesis ( "Lag: " + Lag + " K: "  + K );
            
                    // Further process the time series...
                    // This makes sure the period is at least as long as the output period, and computes the historical averages.
                    List<TS> tslist = new ArrayList<TS>();
                    tslist.add ( result_ts );
                    request_params = new PropList ( "" );
                    request_params.setUsingObject ( "TSList", tslist );
                    try {
                        processor.processRequest( "ReadTimeSeries2", request_params);
                    }
                    catch ( Exception e ) {
                        message = "Error post-processing routed time series.";
                        Message.printWarning ( warning_level, 
                            MessageUtil.formatMessageTag(command_tag,
                            ++warning_count), routine, message );
                        Message.printWarning(log_level, routine, e);
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
                        throw new CommandException ( message );
                    }
                    
                    // Update the data to the processor...
                    
                    try {
                        TSCommandProcessorUtil.appendTimeSeriesToResultsList ( processor, this, result_ts );
                    }
                    catch ( Exception e ){
                        message = "Cannot append new time series to results list.  Skipping.";
                        Message.printWarning ( warning_level,
                            MessageUtil.formatMessageTag(
                            command_tag, ++warning_count),
                            routine,message);
                        status.addToLog(commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE, message,
                            "Unable to provide recommendation - check log file for details."));
                    }
                }
            }
        }
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            List<TS> tslist = new ArrayList<TS>();
            tslist.add ( result_ts );
            setDiscoveryTSList ( tslist );
        }
	} 
	catch ( Exception e ) {
        message ="Unexpected error lagging the time series (" + e + ").";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count),routine,message );
        Message.printWarning(3,routine,e);
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "See the log file for details." ) );
		throw new CommandException ( message );
	}
	
	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		throw new CommandWarningException ( message );
	}
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Save the states to the table, in JSON format.
*/
private void saveStatesToTable(DataTable stateTable, int stateTableObjectIDColumnNum, int stateTableDateTimeColumnNum,
		int stateTableNameColumnNum, int stateTableValueColumnNum,
		String stateTableObjectID, DateTime dt,
		String stateName, String flowUnits, String lagInterval,
		double currentLaggedInflow, double currentOutflow, double currentStorage, double [] co )
throws Exception
{
	// Format the states as JSON
	Gson gson = null;
	boolean prettyJson = false; // Should only be used for debug because it introduces newlines
	if ( prettyJson ) {
		gson = new GsonBuilder().setPrettyPrinting().create();
	}
	else {
		gson = new GsonBuilder().create();
	}
	VariableLagK_States states = new VariableLagK_States();
	states.setLagInterval(lagInterval);
	states.setUnits(flowUnits);
	states.setCurrentLaggedInflow(currentLaggedInflow);
	states.setCurrentOutflow(currentOutflow);
	states.setCurrentStorage(currentStorage);
	states.setQtLag(co);
	// See if an existing table record exists
	int [] columnNumbers = new int[3];
	List<Object> columnValues = new ArrayList<Object>();
	int columnCount = 0;
	if ( stateTableObjectIDColumnNum >= 0 ) {
		// Lookup using object ID if specified
		columnNumbers[columnCount++] = stateTableObjectIDColumnNum;
		columnValues.add(stateTableObjectID);
	}
	// Date/time always used
	columnNumbers[columnCount++] = stateTableDateTimeColumnNum;
	columnValues.add(dt);
	if ( stateTableNameColumnNum >= 0 ) {
		// Lookup using state name if specified
		columnNumbers[columnCount++] = stateTableNameColumnNum;
		columnValues.add(stateName);
	}
	// Resize array if necessary because some columns may not have been used for lookup...
	if ( columnCount != 3 ) {
		int [] columnNumbers2 = new int[columnCount];
		System.arraycopy(columnNumbers, 0, columnNumbers2, 0, columnCount);
		columnNumbers = columnNumbers2;
	}
	List<TableRecord> records = stateTable.getRecords(columnNumbers, columnValues);
	if ( records.size() > 1 ) {
		// If more than one record it is a problem
		throw new RuntimeException("State table contains " + records.size() + " records (0 or 1 expected) for ObjectID=\""
			+ stateTableObjectID + "\" date/time=\"" + dt + "\" state name=\"" + stateName + "\"");
	}
	else if ( records.size() == 1 ) {
		// If an existing record, update
		TableRecord record = records.get(0);
		if ( stateTableObjectIDColumnNum >= 0 ) {
			record.setFieldValue(stateTableObjectIDColumnNum, stateTableObjectID);
		}
		record.setFieldValue(stateTableDateTimeColumnNum, dt);
		if ( stateTableNameColumnNum >= 0 ) {
			record.setFieldValue(stateTableNameColumnNum, stateName);
		}
		record.setFieldValue(stateTableValueColumnNum, gson.toJson(states).replace("\n"," "));
	}
	else {
		// Else, add a new record at the end
		TableRecord newRecord = stateTable.emptyRecord();
		if ( stateTableObjectIDColumnNum >= 0 ) {
			newRecord.setFieldValue(stateTableObjectIDColumnNum, stateTableObjectID);
		}
		newRecord.setFieldValue(stateTableDateTimeColumnNum, dt);
		if ( stateTableNameColumnNum >= 0 ) {
			newRecord.setFieldValue(stateTableNameColumnNum, stateName);
		}
		newRecord.setFieldValue(stateTableValueColumnNum, gson.toJson(states).replace("\n"," "));
		stateTable.addRecord(newRecord);
	}
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"TSID",
    	"FlowUnits",
    	"LagInterval",
		"Lag",
		"K",
    	"OutputStart",
    	"OutputEnd",
		"InitializeStatesFromTable",
		"InitialLaggedInflow",
		"InitialOutflow",
		"InitialStorage",
		"InitialQTLag",
		"StateTableID",
		"StateTableObjectIDColumn",
		"StateTableObjectID",
		"StateTableDateTimeColumn",
		"StateTableNameColumn",
		"StateTableValueColumn",
		"StateTableStateName",
		"StateSaveDateTime",
		"StateSaveInterval",
		"NewTSID",
		"Alias"
	};
	return this.toString(parameters, parameterOrder);
}

}