package rti.tscommandprocessor.commands.ts;

import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import riverside.ts.routing.lagk.LagKBuilder;
import riverside.ts.util.Table;
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
import RTi.Util.Time.TimeInterval;

/**
This class initializes, edits, and runs the VariableLagK command.
*/
public class VariableLagK_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
Big flow value used to constrain lookups in the tables.
ResJ had 1000000.0 but this seems like it be exceeded.
*/
private double __BIG_DATA_VALUE = Double.MAX_VALUE;
    
/**
List of time series created during discovery.  This is the routed time series.
*/
private List __discovery_TS_Vector = null;
    
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
	String InflowStates = parameters.getValue( "InflowStates" );
	String OutflowStates = parameters.getValue( "OutflowStates" );
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

	if ( (Lag != null) && !Lag.equals("") ) {
		// If pair values are specified, make sure they are a sequence of numbers.  Save the results
	    // for use when running the command.
		List tokens = StringUtil.breakStringList(Lag, " ,;", StringUtil.DELIM_SKIP_BLANKS);
		int size = 0;
		if ( tokens != null ) {
			size = tokens.size();
		}
		if ( (size%2) != 0 ) {
            message = "The Lag values (" + Lag + ") are not specified as pairs.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Provide a value,Lag as pairs."));
		}
		else {
		    // Verify that each value in the pair is a number and set the pair in the table.
		    // Also verify that the flow values are in increasing order.
		    __Lag_Table = new Table();
		    __Lag_Table.allocateDataSpace(size/2);
    		String token;
            double value = 0.0, lag = 0.0; // "value" needs to stick around because it is saved with lag
            double value_prev = 0.0;
    		for ( int i = 0; i < size; i++ ) {
    			token = ((String)tokens.get(i)).trim();
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
    			        value = Double.parseDouble(token);
    			    }
    			    else {
    			        lag = Double.parseDouble(token);
    			        // Have processed the value and lag so set in the table.
    			        __Lag_Table.set(i/2, value, lag);
                        if ( (i > 1) && (value <= value_prev) ) {
                            // Verify that the flow value is larger than the previous value
                            message = "Flow value (" + StringUtil.formatString(value,"%.4f") +
                                ") is <= previous flow value in the Lag table.";
                            warning += "\n" + message;
                            status.addToLog(CommandPhaseType.INITIALIZATION,
                                new CommandLogRecord(CommandStatusType.FAILURE, message,
                                "Verify that flow values are in increasing order in the Lag table."));
                        }
                        value_prev = value;
    			    }
    			}
    		}
		}
	}
	
	// K is not required - can lag without attenuating

    if ( (K != null) && !K.equals("") ) {
        // If pair values are specified, make sure they are a sequence of numbers.  Save the results
        // for use when running the command.
        List tokens = StringUtil.breakStringList(K, " ,;", StringUtil.DELIM_SKIP_BLANKS);
        int size = 0;
        if ( tokens != null ) {
            size = tokens.size();
        }
        if ( (size%2) != 0 ) {
            message = "The K values (" + K + ") are not specified as pairs.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,"Provide a value,K as pairs."));
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
                token = ((String)tokens.get(i)).trim();
                if ( !StringUtil.isDouble(token) ) {
                    if ( (i%2) == 0 ) {
                        message = "K table flow value (" + token + ") is not a number.";
                        warning += "\n" + message;
                        status.addToLog(CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(
                            CommandStatusType.FAILURE, message,
                            "Provide a valid number."));
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
    }

	// TODO SAM 2008-10-06 Do more checks for the states in the 'runCommand' method. At this time, only
	// the TS Aliases are known, and the data interval (needed to compute
	// the number of required states) cannot be computed from an Alias.
	
	if ( (InflowStates != null) && !InflowStates.equals("") ) {
		List v = StringUtil.breakStringList ( InflowStates, ",", 0 );
	    int size = v.size();
	    for ( int i = 0; i < size; i++ ) {
	        String state = (String)v.get(i);
	        if ( !StringUtil.isDouble(state) ) {
	            message = "The value for InflowStates \"" + state + "\" is invalid.";
	            warning +="\n" + message;
	            status.addToLog ( CommandPhaseType.INITIALIZATION,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Specify the inflow state value as a number." ) );
	        }
	    }
	}
	
    if ( (OutflowStates != null) && !OutflowStates.equals("") ) {
    	List v = StringUtil.breakStringList ( OutflowStates, ",", 0 );
        int size = v.size();
        for ( int i = 0; i < size; i++ ) {
            String state = (String)v.get(i);
            if ( !StringUtil.isDouble(state) ) {
                message = "The value for OutflowStates \"" + state + "\" is invalid.";
                warning +="\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the outflow state value as a number." ) );
            }
        }
    }

	// Throw an InvalidCommandParameterException in case of errors.
    
    // Check for invalid parameters...
    List valid_Vector = new Vector();
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "NewTSID" );
    valid_Vector.add ( "Lag" );
    valid_Vector.add ( "K" );
    valid_Vector.add ( "FlowUnits" );
    valid_Vector.add ( "LagInterval" );
    valid_Vector.add ( "InflowStates" );
    valid_Vector.add ( "OutflowStates" );
    valid_Vector.add ( "Alias" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, warning_level ),warning );
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
{	
	return ( new VariableLagK_JDialog ( parent, this ) ).ok();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	
	super.finalize ();
}

/**
Return the list of time series read in discovery phase.
*/
private List getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
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
{   String routine = getClass().getName() + ".getNormalizedTable";
    // If the original table is null, return an empty table.  For K, need to initialize so that
    // the storage value (K) is zero, with a large flow being used to allow the lookup.
    // This was implemented after SAM talked to Marc Baldo. 
    if ( originalTable == null ) {
        Table newTable = new Table();
        newTable.allocateDataSpace(1);
        newTable.set(0, __BIG_DATA_VALUE, 0.0 );  // Meaning no Lag or no K despite what the input value
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
            "\" are not compatible with the " + tableType + " table flow values - cannot convert to use for routing.";
            Message.printWarning(log_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
            routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the time series to lag has flow units compatible with the " +
                    tableType + " table." ) );
            throw new RuntimeException ( message );
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
                throw new RuntimeException ( message );
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
        double flowAddFactor = unitsConversion.getAddFactor();
        double flowMultFactor = unitsConversion.getMultFactor();
        double timeMultFactor = 1.0; // Convert from original table to time series base interval, 1.0 for no change
        if ( intervalBaseExact ) {
            // No need to do anything
        }
        else if ( (lagInterval.getBase() == TimeInterval.DAY) && (ts.getDataIntervalBase() == TimeInterval.HOUR)) {
           timeMultFactor = lagInterval.getMultiplier()*24.0;
        }
        else if ( (lagInterval.getBase() == TimeInterval.DAY) && (ts.getDataIntervalBase() == TimeInterval.MINUTE)) {
            timeMultFactor = lagInterval.getMultiplier()*24.0*60.0/ts.getDataIntervalMult();
        }
        else if ( (lagInterval.getBase() == TimeInterval.HOUR) && (ts.getDataIntervalBase() == TimeInterval.MINUTE)) {
            timeMultFactor = lagInterval.getMultiplier()*60.0/ts.getDataIntervalMult();
        }
        else if ( (lagInterval.getBase() == TimeInterval.HOUR) && (ts.getDataIntervalBase() == TimeInterval.DAY)) {
            timeMultFactor = lagInterval.getMultiplier()/ts.getDataIntervalMult()*24.0;
        }
        else if ( (lagInterval.getBase() == TimeInterval.MINUTE) && (ts.getDataIntervalBase() == TimeInterval.DAY)) {
            timeMultFactor = lagInterval.getMultiplier()/ts.getDataIntervalMult()*24.0*60.0;
        }
        else if ( (lagInterval.getBase() == TimeInterval.MINUTE) && (ts.getDataIntervalBase() == TimeInterval.HOUR)) {
            timeMultFactor = lagInterval.getMultiplier()/ts.getDataIntervalMult()*60.0;
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
                throw new RuntimeException ( message );
            }
        }
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
public List getObjectList ( Class c )
{
    List discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    TS datats = (TS)discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    TS ts = new TS();
    if ( (c == ts.getClass()) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
    }
    else {
        return null;
    }
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
{	String routine = "VariableLagK_Command.runCommand", message = "";
    //int dl = 10;
	int log_level = 3;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
	
	String TSID = parameters.getValue( "TSID"  );
	String NewTSID = parameters.getValue( "NewTSID"  );
    String FlowUnits = parameters.getValue( "FlowUnits" );
	String Lag = parameters.getValue( "Lag" );
    String K = parameters.getValue( "K" );
	String Alias = parameters.getValue( "Alias" );
	
	TS original_ts = null; // Original time series
	TS result_ts = null; // Result (lagged) time series
	
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
        if ( commandPhase == CommandPhaseType.RUN ){
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
                    result_ts.setDate1 ( original_ts.getDate1() );
                    result_ts.setDate1Original ( original_ts.getDate1() );
                    result_ts.setDate2 ( original_ts.getDate2() );
                    result_ts.setDate2Original ( original_ts.getDate2() );
                    // Allocate memory
                    result_ts.allocateDataSpace();
                }
                
                // Make sure that the Lag table is in the same units as the time series being lagged
                Table Lag_Table = getNormalizedTable ( "Lag", original_ts, FlowUnits, __LagInterval_TimeInterval, __Lag_Table,
                    log_level, command_tag, warning_count, status );
                // Make sure that the K table is in the same base interval as the time series being lagged
                Table K_Table = getNormalizedTable ( "K", original_ts, FlowUnits, __LagInterval_TimeInterval, __K_Table,
                      log_level, command_tag, warning_count, status );
                String units = FlowUnits;
                if ( (units == null) || units.equals("") ) {
                    units = original_ts.getDataUnits();
                }
                Message.printStatus(2, routine, "Lag table used for procesing (" + units + "):\n" + Lag_Table );
                Message.printStatus(2, routine, "K table used for procesing (" + units + "):\n" + K_Table );
                
        	    // TODO SAM 2009-03-24 Paste in the Sentry logic below, but may want to put this in the LagK class
        	    TSIterator tsi = original_ts.iterator();
        	    TSData tsd = null;
        		LagKBuilder lkb = new LagKBuilder(original_ts);
        		lkb.setLagIn ( Lag_Table );
        		lkb.setKOut ( K_Table );
        		riverside.ts.routing.lagk.LagK lagK = lkb.create();
        		
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
                double prev = 0;
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
                while ((tsd = tsi.next()) != null) {
                    double dataIn = tsd.getData();
                    double lagVal = lagK.solveMethod(tsd.getDate(), prev);
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
                    prev = lagVal;
                    // TODO SAM 2009-03-29 Evaluate removing - lagged value from solveMethod() is for current time
                    //DateTime future = new DateTime(tsd.getDate());
                    //future.addInterval(original_ts.getDataIntervalBase(), original_ts.getDataIntervalMult());
                    //result_ts.setDataValue(future, lagVal);
                    result_ts.setDataValue(tsd.getDate(), lagVal);
                    if (original_ts.isDataMissing(dataIn)) {
                        message = "The input time series has missing data at " + tsd.getDate() +
                        " - unable to route time series at and beyond this date/time.";  
                        Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag(
                        command_tag,++warning_count), routine, message );
                        // This is just a warning
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Fill the time series to be routed before trying to route." ) );
                        break;
                    }
                }
        
        		// Update the newly created time series genesis.
        		result_ts.addToGenesis ( "Routed data from " + original_ts.getIdentifierString());
        		result_ts.addToGenesis ( "Lag: " + Lag + " K: "  + K );
        
                // Further process the time series...
                // This makes sure the period is at least as long as the output period, and computes the historical averages.
                List tslist = new Vector();
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
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            List tslist = new Vector();
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
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

/**
Return the string representation of the command.
@param props PropList of Command properties
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

	// Get the properties from the command; 
	String TSID = props.getValue( "TSID" );
	String NewTSID = props.getValue("NewTSID");
    String FlowUnits = props.getValue("FlowUnits");
    String LagInterval = props.getValue("LagInterval");
	String Lag = props.getValue("Lag");
	String K = props.getValue("K");
	String InputStates = props.getValue("InputStates");
	String OutputStates = props.getValue("OutputStates");
	String Alias = props.getValue( "Alias" );
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewTSID=\"" + NewTSID + "\"" );
	}
    if ( (FlowUnits != null) && (FlowUnits.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FlowUnits=" + FlowUnits );
    }
    if ( (LagInterval != null) && (LagInterval.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "LagInterval=" + LagInterval );
    }
	if ( (Lag != null) && (Lag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Lag=\"" + Lag +"\"" );
	}
	if ( (K != null) && (K.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "K=\"" + K + "\"" );
	}
	if ( (InputStates != null) && (InputStates.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputStates=\"" + InputStates + "\"" );
	}
	if ( (OutputStates != null) && (OutputStates.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputStates=\"" + OutputStates + "\"" );
	}
	if ( (Alias != null) && (Alias.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Alias=\"" + Alias + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}