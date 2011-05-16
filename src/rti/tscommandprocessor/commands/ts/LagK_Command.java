// ----------------------------------------------------------------------------
// lagK_Command - editor for TS X = LagK()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 2005-07-11	Steven A. Malers, RTi	Initial version, initialized from
//					changeInterval_Command().
// 2005-07-14	Michael Thiemann, RTi	Ported LagK operation from C++. Changed
//                                      fillNearest maximum search window to
//                                      24 intervals.
// 2005-08-04	SAM, RTi		* Update the toString() method to be
//					  consistent with other commands.
//					* Change the InTSID parameter to TSID,
//					  consistent with other commands.
// 2005-08-07	SAM, RTi		* Change so that the InflowStates and
//					  OutflowStates parameters are listed
//					  earliest to latest in time (left to
//					  right).  The parameters are reversed
//					  when parsed and put into the internal
//					  arrays, which are still ordered latest
//					  to earliest in time.
// 2005-08-17	SAM, RTi		* Fix bug where missing TSID was causing
//					  an exception checking the parameters.
//					* Format code to fit 80-column width.
//					* Update all warning checks to be
//					  consistent with other code and be more
//					  useful to users.
//					* Remove valueOf() calls.
//					* Time series lookup was not being
//					  checked properly, resulting in
//					  exceptions if the time series was
//					  not found.
// 2007-02-13	SAM, RTi		Remove direct dependence on TSCommandProcessor.
//					Clean up code based on Eclipse feedback.
// ----------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandSavesMultipleVersions;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

public class LagK_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{

/**
Possible values for FillNearest parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Flag indicating that missing data points are filled with nearby non-missing data.
*/
private boolean __param_FillNearest = false;
/**
Flow used in routing when missing data cannot be replaced by other means.
*/
private double __param_DefaultFlow = 0.0;
/**
Time in input data intervals that the flow data will be delayed when there is no attenuation (__param_k=0).
*/
private double __param_lag;
/**
Attenuation time in input data intervals. 
K can be thought of as the ratio of storage to discharge.
*/
private double __param_k;
/**
Number of initial values (states) required for the computation during the first calculation time steps
*/
private int __param_numStates;
/**
Inflow data values prior to the first calculation time step (__param_numStates values).
__param_InflowStates[0] is the last value prior to the first calculation time step
*/
private double[] __param_InflowStates;
/**
Outflow data values prior to the first calculation time step
*/
private double[] __param_OutflowStates;
/**
Fraction of a time interval to be lagged if __param_lag is not evenly divisible by the input data interval
 */
private double __lag_fraction = 0.0;
/**
Number of intervals to search for non-missing data in both directions when __param_FillNearest is true
*/
private int	 __searchWindowIntervals = 0;

/**
Constructor.
*/
public LagK_Command ()
{	super();
	setCommandName ( "LagK" );
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
    String Alias = parameters.getValue( "Alias" );
	String TSID = parameters.getValue( "TSID" );
	String ObsTSID = parameters.getValue( "ObsTSID" );
	String DefaultFlow = parameters.getValue( "DefaultFlow" );
	String FillNearest = parameters.getValue( "FillNearest" );
	String K = parameters.getValue( "K" );
	String Lag = parameters.getValue( "Lag" );
	String InflowStates = parameters.getValue( "InflowStates" );
	String OutflowStates = parameters.getValue( "OutflowStates" );
	
    if ( (Alias == null) || (Alias.length() == 0) ) {
        message = "The alias must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the alias for the time series to create." ) );
    }
	
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
	else if ( TSID.equalsIgnoreCase( ObsTSID) ) {
		// The observed TS must be different from in input TS
        message = "The observed time series must be different from the input time series.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify different identifiers for the observed and lagged time series." ) );
	}
	
	// If specified, the DefaultFlow must be a double value

	if ( DefaultFlow != null && DefaultFlow.length() != 0 ) {
		if( ! StringUtil.isDouble( DefaultFlow ) ) {
            message = "The value for DefaultFlow \"" + DefaultFlow + "\" must be a number.";
			warning += "\n" + message;
             status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the default flow as a number." ) );
		} else {
			__param_DefaultFlow = StringUtil.atod( DefaultFlow );
		}
	}
	
	// Set the FillNearest boolean
	if( FillNearest != null && FillNearest.equalsIgnoreCase(_True) ) {
		__param_FillNearest = true;
	}
	else if ( FillNearest != null && FillNearest.equalsIgnoreCase(_False)){
		__param_FillNearest = false;
	}
	else if( FillNearest != null ) {
        message = "The value for FillNearest \"" + FillNearest + "\" must be either \"" + _True +
        "\" or \"" + _False + "\".";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify as " + _True + " or " + _False + "." ) );
	}
	
	// The Lag must be specified and a double precision value
	if ( Lag == null || Lag.length() == 0) {
        message = "Lag must be specified.";
		warning +="\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the lag." ) );
	}
	else if ( !StringUtil.isDouble( Lag ) ) {
        message = "The value for Lag \"" + Lag + "\" must be a number.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the lag value as a number." ) );
	}
	else {
		__param_lag = StringUtil.atod( Lag );
	}
	
	// K must be specified as a number
	if ( K == null || K.length() == 0) {
        message = "K must be specified.";
		warning +="\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the K value as a number." ) );
	}
	else if ( ! StringUtil.isDouble( K ) ) {
        message = "The value for K \"" + K + "\" must be a number.";
		warning +="\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the K value as a number." ) );
	}
	else {
		__param_k = StringUtil.atod( K );
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
    List<String> valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "ObsTSID" );
    valid_Vector.add ( "FillNearest" );
    valid_Vector.add ( "DefaultFlow" );
    valid_Vector.add ( "Lag" );
    valid_Vector.add ( "K" );
    valid_Vector.add ( "InflowStates" );
    valid_Vector.add ( "OutflowStates" );
    //valid_Vector.add ( "SearchStart" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, warning_level ),
			warning );
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
	return ( new LagK_JDialog ( parent, this ) ).ok();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	
	__param_InflowStates = null;
	__param_OutflowStates = null;
	
	super.finalize ();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
    List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	String routine = "lagK_Command.parseCommand", message;
	int warning_level = 2;

	if ( Message.isDebugOn ) {
		Message.printDebug ( 10, routine, "Command to parse is: " + command );
	}
	
    if ( !command.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation
        super.parseCommand(command);
    }
    else {
    	String Alias = "";
    	     
    	// Since this command is of the type TS X = lagK (...), first parse the
    	// Alias (the X in the command)...
    
    	String substring = command;
    	if ( command.indexOf('=') >= 0 ) {
    		// Because the parameters contain =, find the first = to break
    		// the assignment TS X = lagK (...).
    		int pos = command.indexOf('=');
    		substring = command.substring(0,pos).trim();
    		List<String> v = StringUtil.breakStringList ( substring, " ", StringUtil.DELIM_SKIP_BLANKS ); 
    		// First field has format "TS Alias"
    		Alias = (v.get(1)).trim();		
    		// Get the main part of the command...
    		substring = command.substring(pos + 1).trim();	
    	}
    	else {
    	    // New blank command may not have alias so assign a default...
    		Alias = "Alias";
    	}
    		
    	// Split the substring into two parts: the command name and 
    	// the parameters list within the parenthesis.
    	List<String> tokens = StringUtil.breakStringList ( substring, "()", 0 );
    	if ( (tokens == null) || tokens.size() < 2 ) {
    		// Must have at least the command name and the parameter list.
    		message = "Syntax error in \"" + command + "\".  Not enough tokens.";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    
    	// Parse the parameters (second token in the tokens vector) needed to process the command.
    	try {
    		setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT, tokens.get(1), routine, "," ) );
    		// If the Alias was found in the command added it to the parameters propList.	
    		if ( (Alias != null) && (Alias.length() > 0) ) {
    			setCommandParameter( "Alias", Alias );
    			if ( Message.isDebugOn ) {
    				message = "Alias is: " + Alias;
    				Message.printDebug ( 10, routine, message );
    			}
    		} 	
    	}
    	catch ( Exception e ) {
    		message = "Syntax error in \"" + command + "\".  Error parsing parameters.";
    		Message.printWarning ( warning_level, routine, message );
    		throw new InvalidCommandSyntaxException ( message );
    	}
    }
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
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
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = "lagK_Command.runCommand", message = "";
    int dl = 10;
	int log_level = 3;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Initialize the list
        setDiscoveryTSList ( null );
    }
	
	String Alias = parameters.getValue( "Alias" );
	String TSID = parameters.getValue( "TSID"  );
	String ObsTSID = parameters.getValue( "ObsTSID"  );
	
	TS original_ts = null;		// Original time series
	TS result_ts = null;		// Result time series
	TS obs_ts = null;               // Optional observed ts
		
	// Get the reference (original_ts) to the time series to route
	
	PropList request_params = new PropList ( "" );
    int ts_pos = -1; // No time series found
    CommandProcessorRequestResultsBean bean = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        String TSList = "" + TSListType.LAST_MATCHING_TSID;
        List<TS> tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, null );
        if ( (tslist != null) && (tslist.size() > 0) ) {
            original_ts = tslist.get(0);
        }
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
    	request_params.set ( "TSID", TSID );
    	try { bean =
    		processor.processRequest( "IndexOf", request_params);
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
    	}
    	catch ( Exception e ) {
    		message = "Unexpected error getting the time series to route.";
    		Message.printWarning ( warning_level,
    		MessageUtil.formatMessageTag(
    		command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the time series to lag matches an available time series." ) );
    	}
    }
			
	//Get the optional obs_ts
	if( ObsTSID != null && ObsTSID.length() > 0 ) {
		try {
			request_params = new PropList ( "" );
			request_params.set ( "TSID", ObsTSID );
			bean = null;
			ts_pos = -1;	// No time series found
			try {
			    bean = processor.processRequest( "IndexOf", request_params);
				PropList bean_PropList = bean.getResultsPropList();
				Object o_Index = bean_PropList.getContents ( "Index" );
				if ( o_Index == null ) {
					Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, "Null value for IndexOf(TSID=" + ObsTSID + ") returned from processor." );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the time series to lag matches an available time series." ) );
				}
				else {
				    ts_pos = ((Integer)o_Index).intValue();
				}
			}
			catch ( Exception e ) {
                message = "Error requesting IndexOf(TSID=" + ObsTSID + "\" from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
			}
						
			if ( ts_pos < 0 ) {
				message = "The time series \"" + ObsTSID + "\" was not defined in a previous command.";
				Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count), routine, message);
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
						routine, "Error requesting GetTimeSeries(Index=" + ts_pos + ") from processor." );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the time series to lag matches an available time series." ) );
				}
				PropList bean_PropList = bean.getResultsPropList();
				Object prop_contents = bean_PropList.getContents ( "TS" );
				if ( prop_contents == null ) {
					Message.printWarning(warning_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, "Null value for GetTimeSeries(Index=" + ts_pos + ") returned from processor." );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the time series to lag matches an available time series." ) );
				}
				else {
				    obs_ts = (TS)prop_contents;
				}
			}
		} catch ( Exception e ) {
            message = "Unexpected error getting the observed time series \"" + ObsTSID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the time series to lag matches an available time series." ) );
		}
	}
	
	// Get the base multiplier of the input time series.
	// Initialize to -1 will result in __param_numStates to be negative,
	// which will allow for a check in initializeStates().

	int mult = -1, base = -1;
	if ( original_ts != null ) {
	    mult = original_ts.getDataIntervalMult();
		base = original_ts.getDataIntervalBase();
		
    	if( base == TimeInterval.IRREGULAR ) {
    		message = "The input time series is irregular, cannot route!";	
    		Message.printWarning ( warning_level,
    		MessageUtil.formatMessageTag(
    		command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Use ChangeInterval() to convert to a regular time series before routing." ) );
    	}
    	
    	//Check the default flow value
    	if( __param_DefaultFlow == original_ts.getMissing() ) {
    		message = "The default flow cannot be the missing value of the input TS.";
    		Message.printWarning ( warning_level,
    		MessageUtil.formatMessageTag(
    		command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Change the default flow value." ) );
    	}
	} // end if ( original_ts != null )
	
	//Define the search window if data are filled
	if( __param_FillNearest ) {
		if( base <= TimeInterval.SECOND )  { // 1000 intervals
			__searchWindowIntervals = 100;
		}
		else if( base == TimeInterval.MINUTE )  { // one day
			__searchWindowIntervals = 60 * 24 / mult;
		}
		else if ( base == TimeInterval.HOUR ) { // one day
			__searchWindowIntervals = 24 / mult;
		}
		else if ( base == TimeInterval.DAY ) { // one week
			__searchWindowIntervals = 7 / mult;
		}
		else { //one week, month, year
			__searchWindowIntervals = 1;
		}
	}
	//Compute the number of required states
	__param_numStates = ((int) __param_lag/mult ) + 1;

	// Get the processing period

	DateTime date1 = null;
	DateTime date2 = null;
	
	if ( original_ts != null ) {
    	date1 = original_ts.getDate1();
    	date2 = original_ts.getDate2();
    	if ( original_ts.getDate1() == null ) {
    		message = "The starting date/time for the input time series s not defined.";	
    		Message.printWarning ( warning_level,
    		MessageUtil.formatMessageTag(
    		command_tag,++warning_count), routine, message );
               status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that data exist for the time series." ) );
    	}
    	if ( original_ts.getDate2() == null ) {
    		message = "The ending date/time for the input time series is not defined.";	
    		Message.printWarning ( warning_level,
    		MessageUtil.formatMessageTag(
    		command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that data exist for the time series." ) );
    	}
	}
	
	if ( (__param_k != 0.0) && (Math.abs(__param_k) < .5*original_ts.getDataIntervalMult() ) ) {
        message = "The K value (" + __param_k + ") is < .5 the time series interval (" +
        original_ts.getIdentifier().getInterval() + ") - results may not be accurate.";
        Message.printWarning(log_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.WARNING,
                message, "Specify the K value as 0 or > .5 the time series interval." ) );
    }

	// Lag the time series...
	try {
		// Create the output time series. It will have the same units
		// and interval as the input time series
		if ( original_ts != null ) {
			result_ts = TSUtil.newTimeSeries (original_ts.getIdentifierString(), true );
			result_ts.copyHeader ( original_ts );
			result_ts.setDate1 ( original_ts.getDate1() );
			result_ts.setDate2 ( original_ts.getDate2() );
			if ( commandPhase == CommandPhaseType.RUN ) {
    			// Allocate memory
    			result_ts.allocateDataSpace(); 
    
        		// Compute the lag:
        		// 1. Number of time steps
        		
        		int lag_timesteps = (int) (__param_lag/mult);
        	
        		// 2. Fraction as remainder, this is used for interpolation
        		__lag_fraction = ( (double) __param_lag / (double ) mult ) - lag_timesteps;
        					
        		if ( Message.isDebugOn ) {
        			Message.printDebug ( dl, routine,
        				"Lagged timesteps: " + lag_timesteps +
        				" Lagged fraction of timestep: "+__lag_fraction+
        				" Delay in timesteps: " + __param_numStates );
        		}
        	
        		// Read the states -- NOTE: These are user input or defaults. 
        		// TSTool does curRently not read states from a file or database.
        		// TODO SAM 2009-03-10 Evaluate code
        
        		warning_count = initializeStates ( command_tag, warning_level,warning_count );
        
        		// The final check on input errors.  It is done here because
        		// initializing the states are the last use of input parameters.
        
        		if ( warning_count > 0 ) {
        			// Input error (e.g., missing time series)...
        			message = "The input data must be corrected before running the command.";
        			Message.printWarning ( warning_level,
        			MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
        			throw new InvalidCommandParameterException ( message );
        		}
        
        		// Now loop through the data and fill in from the data array...
        		double I1, I2, O1, O2;
        		DateTime O2_date = new DateTime ( DateTime.DATE_FAST );
        		int i;
        		String inflow_string2 = "Inflow2";
        		String outflow_string2 = "Outflow2";
        		String inflow_string1 = "Inflow1";
        		String outflow_string1 = "Outflow1";
        		// Initialize the dates...
        		// O2_date is date being processed...
        		O2_date = new DateTime(date1);
        		// O1_date is one interval less than the date being processed...
        		DateTime O1_date = new DateTime(O2_date);
        		O1_date.addInterval( base, -1*(int)mult );
        		
        		// I2_date is lag_timesteps before O2...
        		// Note: lag_timesteps is an integer, fractional lags are dealt
        		// with in the getDataValue routine
        		DateTime I2_date = new DateTime(O2_date);
        		I2_date.addInterval( base, (int) (-1*lag_timesteps*(int)mult) );
        		// I1_date is one time step before I2...
        		DateTime I1_date = new DateTime(I2_date);
        		I1_date.addInterval( base, -1*(int)mult );
        
        		if ( Message.isDebugOn ) {
        			Message.printDebug ( dl, routine,
        				"INITIAL O2_date: " + O2_date.toString() +
        				"  I1_date: " + I1_date.toString() +
        				"  I2_date: " + I2_date.toString() +
        				"  O1_date: " + O1_date.toString()
        			);
        		}
        				
        		for (	i = 0; O2_date.lessThanOrEqualTo(date2);
        			I1_date.addInterval( base, mult ), 
        			I2_date.addInterval( base, mult ), 
        			O1_date.addInterval( base, mult ), 
        			O2_date.addInterval( base, mult ), 
        			i++ ) {
        	
        			if ( Message.isDebugOn ) {
        				Message.printDebug ( dl, routine,
        					"COMPUTING FOR Current O2_date: " +
        					O2_date.toString() +
        					"  I1_date: " + I1_date.toString() +
        					"  I2_date: " + I2_date.toString() +
        					"  O1_date: " + O1_date.toString()
        				);
        			}
        	
        			// Missing values should not occur.
        			
        			// Get the input flow values, relying on the input time
        			// series and observed...
        	
        			// For the input values, get the data as follows:
        			//
        			// 1) Check the original_ts for a value, allow
        			//    interpolation
        			//
        			// If missing....
        			//
        			// 2) Check for observed value.
        			// 3) Check inflow time series for nearest value.
        			// 4) Check observed time series for nearest value
        			//    (should never happen but returns more reasonable
        			//    value than 5).
        			// 5) Use default flow.
        
        			I1 = getDataValue ( i,  true, inflow_string1,
        					original_ts, I1_date,
        					obs_ts, I1_date,
        					obs_ts, I1_date, 
        					__param_InflowStates, date1);
        			I2 = getDataValue ( i,  true, inflow_string2,
        					original_ts, I2_date,
        					obs_ts, I2_date,
        					obs_ts, I2_date, 
        					__param_InflowStates, date1);
        	
        			// For the routed values, get the data as follows:
        			//
        			// 1) Check the result_ts for a value; do not allow
        			//    interpolation because its values were computed
        			//    using interpolated inflow data (if indicated by
        			//    __param_lag)
        			//
        			// If missing....
        			//
        			// 2) Check for observed value if in first part of
        			//    period.
        			// 3) Check output time series for nearest value.
        			// 4) Check inflow time series for nearest value (should
        			//    never happen but returns more reasonable value
        			//    than 5).
        			// 5) Use default flow.
        	
        			O1 = getDataValue ( i,  false, outflow_string1,
        					result_ts, O1_date,
        					obs_ts, I1_date,
        					original_ts, O1_date,
        					__param_OutflowStates, date1);
        	
        			// If anything is missing, try to get the value similar
        			// to other values...
        	
        			if (	original_ts.isDataMissing(I1) ||
        				original_ts.isDataMissing(I2) ||
        				result_ts.isDataMissing(O1) ) {
        				// Don't have data to calculate so try to get
        				// from other time series...
        				//
        				// 1) Check the result_ts for a value.
        				//
        				// If missing....
        				//
        				// 2) Check for observed value if in first part
        				//    of period.
        				// 3) Check output time series for nearest
        				//    value.
        				// 4) Use default flow.
        				O2 = getDataValue ( i,  false, outflow_string2,
        					result_ts, O2_date,
        					obs_ts, I2_date,
        					null, O2_date, 
        					__param_OutflowStates, date1 );
        			}
        			else {
        			    // Calculate the routed outflow...
        				// This reduces to the following if no K (pass
        				// through with no change in storage)
        				//
        				// O2 = I1 + I2 - O1
        	
        				O2 = (I1 + I2 - O1*(1.0 - 2.0*__param_k/mult))/(1.0 + 2.0*__param_k/mult);
        			}
        			
        			if ( Message.isDebugOn ) {
        				Message.printDebug ( 10, routine,
        				"Current date: " + O2_date.toString() +
        				"  I1: " + I1 +
        				"  I2: " + I2 +
        				"  O1: " + O1 +
        				"  O2: " + O2 );
        			}
        	
        			// This will then be used as O1 in next iteration...
        			result_ts.setDataValue ( O2_date, O2 );
        	
        			// Do not save states at this time!
        			// REVISIT
        		}
        
        		// done	
        		
        		// Update the scenario
        		TSIdent tsIdent = result_ts.getIdentifier();
                if ( (Alias != null) && !Alias.equals("") ) {
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, result_ts, Alias, status, commandPhase);
                    result_ts.setAlias ( alias );
                }
        		if ( tsIdent.getScenario().equals("") ) {
        		    tsIdent.setScenario( "routed" );
        		}
        		else {
        		    tsIdent.setScenario( tsIdent.getScenario() + "-routed" );
        		}
        		result_ts.setIdentifier( tsIdent );
        		// Update the newly created time series genesis.
        		result_ts.addToGenesis ( "Routed data from " + original_ts.getIdentifierString());
        		result_ts.addToGenesis ( "Lag: " + __param_lag + " K: "  + __param_k );
        
        		// Add the newly created time series to the software memory.
        		List TSResultsList = (List) processor.getPropContents ( "TSResultsList" );
        		TSResultsList.add( result_ts );
        		processor.setPropContents ( "TSResultsList", TSResultsList );
            }
		    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
		        // Set in the discovery list
		        if ( result_ts != null ) {
		            if ( (Alias != null) && !Alias.equals("") ) {
		                String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
		                    processor, result_ts, Alias, status, commandPhase);
		                result_ts.setAlias ( alias );
		            }
		            List<TS> tslist = new Vector();
		            tslist.add(result_ts);
		            setDiscoveryTSList(tslist);
		        }
		    }
		}
	} 
	catch ( Exception e ) {
        message = "Unexpected error lagging the time series (" + e + ").";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count),routine,message );
        Message.printWarning(3,routine,e);
           status.addToLog ( commandPhase,
             new CommandLogRecord(CommandStatusType.FAILURE,
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
Return the index in the States array
*/
private int getStatesIndex( TS ts, DateTime ts_date, DateTime sim_startdate) {

	int dl = 30;

	if ( ts_date.lessThan(sim_startdate) ) {

		int intervalsInStates = TimeUtil.getNumIntervals( ts_date, sim_startdate,
				ts.getDataIntervalBase(),
				ts.getDataIntervalMult() );

		if ( Message.isDebugOn ) {
			Message.printDebug ( dl,
				"LagK_Command.getStatesIndex",
				"Intervals between " + ts_date +
				" and startdate " + sim_startdate + " : " +
				intervalsInStates );
		}

		if( intervalsInStates > 0 &&
			intervalsInStates <= __param_numStates ) {
			return intervalsInStates - 1;
		}
	}
	
	return -1;
}

/**
@return A data value to use for the given parameter.  If the nearest non-missing
value is search for, the search is performed by looking back first and then
into the future, expanding the search up to 1000 time steps for HSECOND od SECOND intervals,
one day for MINUTE or HOUR intervals ans 1 time steps for all other intervals.
@param i Iteration number (used to check against num-delay).
@param interpolate Flag allowing interpolation if true
@param ts_type_label Used for messages to indicate which time series is being operated on.
@param ts Time series to get data from.
@param ts_date Date to get data from ts.
@param inflowts Observed time series.  
@param inflowts_date Date to get data from inflowts.
@param backupts If not null it will be used to fill a missing data value by
finding the nearest non-missing value.
@param backupts_date Date to get data from backupts.
*/
private double getDataValue (	int i, boolean interpolate,
				String ts_type_label,
				TS ts, DateTime ts_date,
				TS inflowts, DateTime inflowts_date,
				TS backupts, DateTime backupts_date,
				double[] state_ts_data, DateTime sim_startdate )
{	String routine = "LagK_Command.getDataValue";
	int dl = 10;

	// 0.
	// Check if inflow time series exists and has data
	// Do not use '&& ( ts.hasNonMissingData() )'
	// because the limits of the output TS will be constantly recomputed 
	if( ( ts != null )  ) {
		
		// 1.
		//
		// See if a value exists...
		double value;
		if( interpolate && __lag_fraction != 0.0 ) { //interpolation

			value = ts.getMissing();

			//data should be available in the inflow TS and should be interpolated
			if( ts_date.greaterThan(sim_startdate) ) {
				value = TSUtil.getInterpolatedDataValue( ts, ts_date, __lag_fraction );
				if ( Message.isDebugOn ) {
					Message.printDebug ( dl, routine, "Interpolated value " + __lag_fraction +
					" intervals later than " + ts_date + " is " + value );
				}	
			}
			else {
			    // no need to interpolate, because these values
				// are already interpolated to the output times!
				int index = getStatesIndex( ts, ts_date, sim_startdate );

				//this will return > -1 only if ts_date < sim_startdate !
				if( index > -1 ) {
					if ( Message.isDebugOn ) {
						Message.printDebug ( dl, routine,
							"Missing data at " +
							ts_date + " in " +
							ts_type_label +
							". Set to States [ " +
							index  + " ] : " +
							state_ts_data[ index ]);
					}
					value = state_ts_data[ index ]; 
				}
				else {
				    // Could not interpolate because the current time == sim_startdate and
					// there is no data the time step before!

					// In this case interpolate between the last value in the States
					// and the first value in the TS ???
					value = ts.getDataValue (sim_startdate);
					if( !ts.isDataMissing(value) ) {
						double lastInStates = state_ts_data[ 0 ];
						
						if(	!ts.isDataMissing(lastInStates) ) {

							value = lastInStates + ( value - lastInStates ) * ( 1 - __lag_fraction);
						}
					}
				}
			}

			if ( !ts.isDataMissing(value) ) { //use data from ts
				if ( Message.isDebugOn ) {
					Message.printDebug ( dl, routine, "Value of " +
						ts.getIdentifierString()+ " at " + ts_date + " is " +  value );
				}
				return value;
			}
		}
		else {
		    //no interpolation
			value = ts.getDataValue ( ts_date );
			if ( !ts.isDataMissing(value) ) { //use data from ts
				if ( Message.isDebugOn ) {
					Message.printDebug ( dl, routine, "Value of " +
						ts.getIdentifierString() + " at " + ts_date + " is " +  value );
				}
				return value;
			}
			else {
			    //get data from States
				int index = getStatesIndex( ts, ts_date, sim_startdate );
				if( index > -1 ) {
					if ( Message.isDebugOn ) {
						Message.printDebug ( dl,
							routine,
							"Missing data at " +
							ts_date.toString() +
							" in " + ts_type_label +
							". Set to States [ " +
							index + " ] : " +
							state_ts_data[ index ]);
					}
					return state_ts_data[ index ];
				}
			}
		}
	}

	// 2. proceed with other time series
	//
	// Check to see if an observed time series has been specified.
	// If so, see if a value is given for the requested date...

	double obs_value = 0.0;
	if ( inflowts != null ) {
		obs_value = inflowts.getDataValue ( inflowts_date );
		if ( !inflowts.isDataMissing(obs_value) ) {
			if ( Message.isDebugOn ) {
				Message.printDebug ( dl, routine,
				"Missing data in " + ts_type_label + ". Set to observed " + obs_value);
			}
			return obs_value;
		}
	}

	// 3.
	//
	// Next check to see if the fill nearest parameter has been set...
	// If so, try to find the nearest in the output time series...

	try {
	    if ( __param_FillNearest == true ) {
			double nearest_value = TSUtil.findNearestDataValue (
						ts, ts_date, -1, __searchWindowIntervals, __searchWindowIntervals );
			if ( Message.isDebugOn ) {
				Message.printDebug ( dl, routine, "Nearest value in output is " + nearest_value);
			}
			if ( !ts.isDataMissing(nearest_value) ) {
				if ( Message.isDebugOn ) {
					Message.printDebug ( dl, routine,
					"Missing data in " + ts_type_label + ". Set to nearest " + nearest_value);
				}
				return nearest_value;
			}
		}
	}
	catch ( Exception e ) {
		; // Ignore and go to next criteria...
	}

	// 4.
	//
	// If not found in the output, try to find the nearest in the backup time series...

	try {	if ( (__param_FillNearest == true ) && (backupts != null) ) {
			double nearest_value = TSUtil.findNearestDataValue (
				backupts, backupts_date, -1, __searchWindowIntervals, __searchWindowIntervals );
			if ( Message.isDebugOn ) {
				Message.printDebug ( dl, routine, "Nearest value in backup TS is " + nearest_value );
			}
			if ( !backupts.isDataMissing(nearest_value) ) {
				if ( Message.isDebugOn ) {
					Message.printDebug ( dl, routine,
					"Missing data in " + ts_type_label + ". Set to nearest backup " + nearest_value);
				}
				return nearest_value;
			}
		}
	}
	catch ( Exception e ) {
		; // Ignore and go to next criteria...
	}


	// 5.
	//
	// If still missing, use the default value...

	if ( Message.isDebugOn ) {
		Message.printDebug ( dl, routine, "Missing data in " + ts_type_label + ". Set to default " + __param_DefaultFlow );
	}
	return __param_DefaultFlow;
}

/**
Initialize the states.  This should be called from the runCommand() method.
This method was significantly changed from the original C++ version, because
TSTool does currently not know how to read sattes from a file or database.
This function can only be called after __param_numStates was set, that is
during command execution when __param_numStates can be derived from the
TSID (during the 'CheckInput' only an Alias is supplied!)
An exception is not thrown if user error is detected because it is expected that
the warning_count will be checked by the calling code.
REVISIT
@param command_tag Used with messages.
@param warning_level Used with messages.
@param warning_count the count of warnings generated during start-up prior to
calling this method.
@return the warning count after this method has incremented the count that was
passed in.
*/
private int initializeStates ( String command_tag, int warning_level, int warning_count )
throws CommandWarningException
{	String message;
	String routine = "lagK_Command.initializeStates";
    CommandStatus status = getCommandStatus();

	// If specified, the States data must be an array 
	// of (Lag/TSID-Multiplier) + 1  = __param_numStates double values
	PropList parameters = getCommandParameters ();
	String InflowStates = parameters.getValue( "InflowStates"  );
	String OutflowStates = parameters.getValue( "OutflowStates"  );
		
	if ( __param_numStates < 0 ) {
		// Must have had some warnings in previous could and will have
		// confusing warnings below so just return...
		return warning_count;
	}
	__param_InflowStates = new double[ __param_numStates ];	
	//Check DefaultinflowStatesStr 
	if (	(InflowStates != null) && InflowStates.length() != 0 ) {
		List v1 = StringUtil.breakStringList (
				InflowStates, ",",
				StringUtil.DELIM_SKIP_BLANKS ); 
		int num_co_supplied1 = v1.size();
		
		if( num_co_supplied1 != __param_numStates ) {
			message =
			"Wrong number of supplied inflow states (" +
			num_co_supplied1 + ").  " +
			"The expected number of values is " +
				__param_numStates;
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the indicated number of state values." ) );
		}
		else {
			for( int i = 0; i < __param_numStates; i++ ) {
				if ( !StringUtil.isDouble( (String) v1.get(i))){
					message =
						"\nInflow state " +
						(String)v1.get(i) +
						" is not a number.";
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(
						command_tag,++warning_count),
						routine, message );
				}

			   	else {	// Transfer the value to the internal
					// array.  The internal array stores
					// values in the order latest to
					// earliest.  However, the parameter is
					// specified as earliest to latest.
					// REVISIT SAM 2005-08-07
					// Earliest to latest is more
					// consistent with other command
					// parameters.  The analysis code could
					// be made consistent or left as is, as
					// long as it is documented internally.
					__param_InflowStates[__param_numStates - i - 1] = StringUtil.atod( (String) v1.get(i) );
				}
			}
		}
	}
	else {
		for( int i = 0; i < __param_numStates; i++ ) {
			__param_InflowStates[i] = 0.0;
		}
	}
	
	//Check DefaultoutflowStatesStr 
	__param_OutflowStates = new double[ __param_numStates ];
	if( OutflowStates != null && OutflowStates.length() != 0 ) {
		List v2 = StringUtil.breakStringList (
				OutflowStates, ",",
				StringUtil.DELIM_SKIP_BLANKS ); 
		int num_co_supplied2 = v2.size();
		
		if( num_co_supplied2 != __param_numStates ) {
			message = "Wrong number of supplied outflow states (" +
				num_co_supplied2 + ").  " +
				"The expected number of values is " +
				__param_numStates;
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the indicated number of outflow state values." ) );
		}
		else {
			for( int i = 0; i < __param_numStates; i++ ) {
				if( ! StringUtil.isDouble( (String) v2.get(i))){
					message =
						"\nOutflow state " +
						(String)v2.get(i) +
						" is not a number.";
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(
						command_tag,++warning_count),
						routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Provide states as numbers." ) );
				}
			   	else {	// Transfer the value to the internal
					// array.  The internal array stores
					// values in the order latest to
					// earliest.  However, the parameter is
					// specified as earliest to latest.
					// REVISIT SAM 2005-08-07
					// Earliest to latest is more
					// consistent with other command
					// parameters.  The analysis code could
					// be made consistent or left as is, as
					// long as it is documented internally.
					__param_OutflowStates[__param_numStates - i - 1] = Double.parseDouble( (String) v2.get(i) );
				}
			}
		}
	}
	else {
		for( int i = 0; i < __param_numStates; i++ ) {
			__param_OutflowStates[i] = 0.0;
		}
	}
	return warning_count;
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
@param props parameters for the command
*/
public String toString ( PropList props )
{
    return toString ( props, 10 );
}

/**
Return the string representation of the command.
@param props parameters for the command
@param majorVersion the major version for software - if less than 10, the "TS Alias = " notation is used,
allowing command files to be saved for older software.
*/
public String toString ( PropList props, int majorVersion )
{   if ( props == null ) {
        if ( majorVersion < 10 ) {
            return "TS Alias = " + getCommandName() + "()";
        }
        else {
            return getCommandName() + "()";
        }
    }

	// Get the properties from the command; 
	String Alias = props.getValue( "Alias" );
	String TSID = props.getValue( "TSID" );
	String ObsTSID = props.getValue("ObsTSID");
	String DefaultFlow = props.getValue("DefaultFlow");
	String FillNearest = props.getValue("FillNearest");
	String K = props.getValue("K");
	String Lag = props.getValue("Lag");
	String InflowStates = props.getValue("InflowStates");
	String OutflowStates = props.getValue("OutflowStates");
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (ObsTSID != null) && (ObsTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ObsTSID=\"" + ObsTSID + "\"" );
	}
    if ( majorVersion >= 10 ) {
        // Add as a parameter
        if ( (Alias != null) && (Alias.length() > 0) ) {
            if ( b.length() > 0 ) {
                b.append ( "," );
            }
            b.append ( "Alias=\"" + Alias + "\"" );
        }
    }
	if ( (FillNearest != null) && (FillNearest.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillNearest=" + FillNearest );
	}
	if ( (DefaultFlow != null) && (DefaultFlow.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DefaultFlow=" + DefaultFlow );
	}
	if ( (Lag != null) && (Lag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Lag=" + Lag );
	}
	if ( (K != null) && (K.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "K=" + K );
	}
	if ( (InflowStates != null) && (InflowStates.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InflowStates=\"" + InflowStates + "\"" );
	}
	if ( (OutflowStates != null) && (OutflowStates.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutflowStates=\"" + OutflowStates + "\"" );
	}
    if ( majorVersion < 10 ) {
        // Old syntax...
        if ( (Alias == null) || Alias.equals("") ) {
            Alias = "Alias";
        }
        return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
    }
    else {
        return getCommandName() + "("+ b.toString()+")";
    }
}

}