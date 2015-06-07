package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSFunctionType;
import RTi.TS.TSIdent;
import RTi.TS.TSIterator;
import RTi.TS.TSUtil;
import RTi.TS.TSUtil_SetDataValuesUsingFunction;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandSavesMultipleVersions;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the NewTimeSeries() command.
*/
public class NewTimeSeries_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public NewTimeSeries_Command ()
{	super();
	setCommandName ( "NewTimeSeries" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String Alias = parameters.getValue ( "Alias" );
	String NewTSID = parameters.getValue ( "NewTSID" );
	String SetStart = parameters.getValue ( "SetStart" );
	String SetEnd = parameters.getValue ( "SetEnd" );
	String MissingValue = parameters.getValue ( "MissingValue" );
	String InitialValue = parameters.getValue ( "InitialValue" );
	String InitialFunction = parameters.getValue ( "InitialFunction" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (Alias == null) || Alias.isEmpty() ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an alias." ) );
	}
	if ( (NewTSID == null) || NewTSID.isEmpty() ) {
        message = "The new time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a new time series identifier." ) );
	}
	else { // if ( !NewTSID.startsWith("${") ) {
		// TODO SAM 2015-06-03 ?Can only check if parameter does not use ${Property}
		try { TSIdent tsident = TSIdent.parseIdentifier( NewTSID );
			try { TimeInterval.parseInterval(tsident.getInterval());
			}
			catch ( Exception e2 ) {
                message = "NewTSID interval \"" + tsident.getInterval() + "\" is not a valid interval.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a valid time series interval." ) );
			}
		}
		catch ( Exception e ) {
			// TODO SAM 2007-03-12 Need to catch a specific exception like
			// InvalidIntervalException so that more intelligent messages can be generated.
            message = "NewTSID is not a valid identifier.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Use the command editor to enter required fields." ) );
		}
	}
	
	// TODO SAM 2012-04-01 Evaluate whether range should be supported
    if ( (MissingValue != null) && !MissingValue.isEmpty() && !MissingValue.startsWith("${") &&
        !StringUtil.isDouble(MissingValue) && !MissingValue.equalsIgnoreCase("NaN")) {
        message = "The missing value (" + MissingValue+ ") must be a number or NaN.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Specify the missing value as a number or NaN."));
    }

	if ( (InitialValue != null) && !InitialValue.isEmpty() && !InitialValue.startsWith("${")) {
		// If an initial value is specified, make sure it is a number...
		if ( !StringUtil.isDouble(InitialValue) ) {
            message = "The initial value (" + InitialValue + ") is not a number.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the initial value as a number." ) ); 
		}
		if ( (InitialFunction != null) && !InitialFunction.isEmpty() ) {
		    message = "The initial value and function cannot both be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the initial value OR function." ) ); 
		}
	}
    if ( (InitialFunction != null) && !InitialFunction.isEmpty() ) {
        // Make sure that the statistic is known in general
        boolean supported = false;
        TSFunctionType functionType = null;
        try {
            functionType = TSFunctionType.valueOfIgnoreCase(InitialFunction);
            supported = true;
        }
        catch ( Exception e ) {
            message = "The function (" + InitialFunction + ") is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported function using the command editor." ) );
        }
        
        // Make sure that it is in the supported list for this command
        
        if ( supported ) {
            supported = false;
            List<TSFunctionType> functionTypes = getFunctionChoices();
            for ( int i = 0; i < functionTypes.size(); i++ ) {
                if ( functionType == functionTypes.get(i) ) {
                    supported = true;
                }
            }
            if ( !supported ) {
                message = "The function (" + InitialFunction + ") is not supported by this command.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Select a supported function using the command editor." ) );
            }
        }
    }
    if ( (SetStart != null) && !SetStart.isEmpty() && !SetStart.startsWith("${") &&
    	!SetStart.equalsIgnoreCase("OutputStart") && !SetStart.equalsIgnoreCase("OutputEnd") ) {
            try {
                DateTime.parse(SetStart);
            }
            catch ( Exception e ) {
                message = "The set start \"" + SetStart + "\" is not a valid date/time.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
            }
        }
	if ( (SetEnd != null) && !SetEnd.isEmpty() && !SetEnd.startsWith("${") &&
		!SetEnd.equalsIgnoreCase("OutputStart") && !SetEnd.equalsIgnoreCase("OutputEnd") ) {
		try {
		    DateTime.parse( SetEnd );
		}
		catch ( Exception e ) {
            message = "The set end \"" + SetEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
		}
	}
    
    // Check for invalid parameters...
	List<String> validList = new ArrayList<String>(10);
    validList.add ( "Alias" );
    validList.add ( "NewTSID" );
    validList.add ( "Description" );
    validList.add ( "SetStart" );
    validList.add ( "SetEnd" );
    validList.add ( "Units" );
    validList.add ( "MissingValue" );
    validList.add ( "InitialValue" );
    validList.add ( "InitialFlag" );
    validList.add ( "InitialFunction" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );
    
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),
		warning );
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
{	// The command will be modified if changed...
	return (new NewTimeSeries_JDialog ( parent, this )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Return the list of supported functions for the InitialFunction parameter.
*/
protected List<TSFunctionType> getFunctionChoices()
{
    List<TSFunctionType> functionTypes = new Vector();
    functionTypes.add ( TSFunctionType.DATE_YYYY );
    functionTypes.add ( TSFunctionType.DATE_YYYYMM );
    functionTypes.add ( TSFunctionType.DATE_YYYYMMDD );
    functionTypes.add ( TSFunctionType.DATETIME_YYYYMMDD_HH );
    functionTypes.add ( TSFunctionType.DATETIME_YYYYMMDD_HHMM );
    functionTypes.add ( TSFunctionType.RANDOM_0_1);
    functionTypes.add ( TSFunctionType.RANDOM_0_1000);
    return functionTypes;
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
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "NewTimeSeries.parseCommand", message;

    if ( !command.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation
        super.parseCommand(command);
    }
    else {
    	// Get the part of the command after the TS Alias =...
    	int pos = command.indexOf ( "=" );
    	if ( pos < 0 ) {
    		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = newTimeSeries(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	String token0 = command.substring ( 0, pos ).trim();	// TS Alias
    	String token1 = command.substring ( pos + 1 ).trim();	// command(...)
    	if ( (token0 == null) || (token1 == null) ) {
    		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = NewTimeSeries(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    
    	// Alias is everything after "TS " (can include space in alias name)
    	String Alias = token0.trim().substring(3).trim();
    
    	// Get the command parameters from the token on the right of the =...
    
    	List<String> tokens = StringUtil.breakStringList ( token1, "()", 0 );
    	if ( (tokens == null) || (tokens.size() < 2) ) {
    		// Must have at least the command name and its parameters...
    		message = "Syntax error in \"" + command + "\". Expecting:  TS Alias = NewTimeSeries(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    
    	try {
    	    PropList parameters = PropList.parse ( Prop.SET_FROM_PERSISTENT, tokens.get(1), routine, "," );
    		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
    		parameters.set ( "Alias", Alias );
    		parameters.setHowSet ( Prop.SET_UNKNOWN );
    		setCommandParameters ( parameters );
    	}
    	catch ( Exception e ) {
    		message = "Syntax error in \"" + command + "\".";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
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
throws InvalidCommandParameterException,
CommandWarningException, CommandException
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warningCount = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int logLevel = 3; // Level for non-user warnings to go to log file.

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters ();
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
		status.clearLog(commandPhase);
	}
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Initialize the list
        setDiscoveryTSList ( null );
    }

	String Alias = parameters.getValue ( "Alias" ); // Expanded below after creating time series
	String NewTSID = parameters.getValue ( "NewTSID" );
	if ( (NewTSID != null) && (NewTSID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		NewTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, NewTSID);
	}
	String Description = parameters.getValue ( "Description" ); // Expanded below
	if ( (Description != null) && (Description.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		Description = TSCommandProcessorUtil.expandParameterValue(processor, this, Description);
	}
	String SetStart = parameters.getValue ( "SetStart" );
	if ( (SetStart == null) || SetStart.isEmpty() ) {
		SetStart = "${OutputStart}";
	}
	String SetEnd = parameters.getValue ( "SetEnd" );
	if ( (SetEnd == null) || SetEnd.isEmpty() ) {
		SetEnd = "${OutputEnd}";
	}
	String Units = parameters.getValue ( "Units" );
	if ( (Units != null) && (Units.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		Units = TSCommandProcessorUtil.expandParameterValue(processor, this, Units);
	}
    String MissingValue = parameters.getValue ( "MissingValue" );
    Double missingValue = null;
    if ( (MissingValue != null) && !MissingValue.isEmpty() ) {
    	if ( commandPhase == CommandPhaseType.RUN ) {
			if ( MissingValue.indexOf("${") >= 0 ) {
				MissingValue = TSCommandProcessorUtil.expandParameterValue(processor, this, MissingValue);
			}
			try {
				// Handles numbers and NaN
				missingValue = Double.parseDouble(MissingValue);
			}
			catch ( Exception e ) {
				message = "MissingValue (" + MissingValue + ") is invalid.";
		        Message.printWarning(logLevel,
		            MessageUtil.formatMessageTag( command_tag, ++warningCount), routine, message );
		        status.addToLog ( commandPhase,
		            new CommandLogRecord(CommandStatusType.FAILURE,
		                message, "Specify the missing value as a number or NaN.") );
			}
		}
    }
	String InitialValue = parameters.getValue ( "InitialValue" );
	double InitialValue_double = 0.0;
	if ( (InitialValue != null) && !InitialValue.isEmpty() ) {
		if ( commandPhase == CommandPhaseType.RUN ) {
			if ( InitialValue.indexOf("${") >= 0 ) {
				InitialValue = TSCommandProcessorUtil.expandParameterValue(processor, this, InitialValue);
			}
			try {
				InitialValue_double = Double.parseDouble ( InitialValue );
			}
			catch ( Exception e ) {
				message = "InitialValue (" + InitialValue + ") is invalid.";
		        Message.printWarning(logLevel,
		            MessageUtil.formatMessageTag( command_tag, ++warningCount), routine, message );
		        status.addToLog ( commandPhase,
		            new CommandLogRecord(CommandStatusType.FAILURE,
		                message, "Specify the initial value as a number or NaN.") );
			}
		}
	}
	String InitialFlag = parameters.getValue ( "InitialFlag" );
	if ( (InitialFlag != null) && (InitialFlag.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		InitialFlag = TSCommandProcessorUtil.expandParameterValue(processor, this, InitialFlag);
	}
	String InitialFunction = parameters.getValue ( "InitialFunction" );
	TSFunctionType initialFunction = null;
	if ( InitialFunction != null ) {
	    initialFunction = TSFunctionType.valueOfIgnoreCase(InitialFunction);
	}

	// Determine the dates to use for the Set...
	DateTime SetStart_DateTime = null;
	DateTime SetEnd_DateTime = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			SetStart_DateTime = TSCommandProcessorUtil.getDateTime ( SetStart, "SetStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warningCount;
		}
		try {
			SetEnd_DateTime = TSCommandProcessorUtil.getDateTime ( SetEnd, "SetEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warningCount;
		}
		// Make sure that dates are not null 
	    if ( SetStart_DateTime == null ) {
	        message = "SetStart is not set - cannot allocate time series data array.";
	        Message.printWarning(logLevel,
	            MessageUtil.formatMessageTag( command_tag, ++warningCount), routine, message );
	        status.addToLog ( commandPhase,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Specify the SetStart parameter or use a SetOutputPeriod() command.") );
	    }
	    if ( SetEnd_DateTime == null ) {
	        message = "SetEnd is not set - cannot allocate time series data array.";
	        Message.printWarning(logLevel,
	            MessageUtil.formatMessageTag( command_tag, ++warningCount), routine, message );
	        status.addToLog ( commandPhase,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Specify the SetEnd parameter or use a SetOutputPeriod() command.") );
	    }
    }
    
    if ( warningCount > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog (commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }
	
	// Now process the time series...

	TS ts = null;
	try {
	    // Create the time series...
		ts = TSUtil.newTimeSeries ( NewTSID, true );
		if ( ts == null ) {
            message = "Null time series returned when trying to create with NewTSID=\"" + NewTSID + "\"";
            Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(
                    command_tag,++warningCount),routine,message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify the NewTSID - contact software support if necessary." ) );
			throw new Exception ( "Null time series." );
		}
	}
	catch ( Exception e ) {
		message = "Unexpected error creating the new time series using NewTSID=\""+	NewTSID + "\".";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warningCount),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the NewTSID - contact software support if necessary." ) );
		throw new CommandException ( message );
	}
	try {
        // Try to fill out the time series.  Allocate memory and set other information...
		ts.setIdentifier ( NewTSID );
		if ( (Description != null) && !Description.isEmpty() ) {
			ts.setDescription ( Description );
		}
		if ( (Units != null) && !Units.isEmpty() ) {
			ts.setDataUnits ( Units );
			ts.setDataUnitsOriginal ( Units );
		}
		ts.setDate1 ( SetStart_DateTime );
		ts.setDate1Original ( SetStart_DateTime );
		ts.setDate2 ( SetEnd_DateTime );
		ts.setDate2Original ( SetEnd_DateTime );
        if ( missingValue != null ) {
            ts.setMissing(missingValue);
        }
		if ( commandPhase == CommandPhaseType.RUN ) {
    		if ( ts.allocateDataSpace() != 0 ) {
    			message = "Unable to allocate memory for time series.";
    			Message.printWarning ( warning_level,
    			MessageUtil.formatMessageTag(
    			command_tag,++warningCount),routine,message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the period for the time series is not huge." ) );
    		}
    		if ( (InitialValue != null) && !InitialValue.isEmpty() ) {
    		    // Assign values to the constant
    			TSUtil.setConstant ( ts, InitialValue_double );
    		}
    		else if ( initialFunction != null ) {
    		    // Assign values using the function
    		    TSUtil_SetDataValuesUsingFunction tsu = new TSUtil_SetDataValuesUsingFunction ( ts, initialFunction );
    		    tsu.setDataValuesUsingFunction ();
    		}
    		if ( (InitialFlag != null) && !InitialFlag.isEmpty() ) {
    		    // Iterate through the data and set the flag
    		    TSIterator it = ts.iterator();
    		    TSData tsdata = null;
    		    while ( (tsdata = it.next()) != null ) {
    		        // Reset the same values and additional the initial flag.
    		        ts.setDataValue(it.getDate(), it.getDataValue(), InitialFlag, tsdata.getDuration() );
    		    }
    		}
		}
        if ( (Alias != null) && !Alias.isEmpty() ) {
            String alias = Alias;
            if ( commandPhase == CommandPhaseType.RUN ) {
            	alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, Alias, status, commandPhase);
            }
            ts.setAlias ( alias );
        }
	}
	catch ( Exception e ) {
        message = "Unexpected error creating new time series \"" + ts.getIdentifier() + "\" (" + e + ").";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warningCount),routine,message );
        Message.printWarning(3,routine,e);
        status.addToLog ( commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE,
            message, "See the log file for details." ) );
	}

	if ( commandPhase == CommandPhaseType.RUN ) {
    	// Update the data to the processor so that appropriate actions are taken...
        TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, ts);
	}
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Set in the discovery list
        if ( ts != null ) {
            List<TS> tslist = new ArrayList<TS>(1);
            tslist.add(ts);
            setDiscoveryTSList(tslist);
        }
    }

	if ( warningCount > 0 ) {
		message = "There were " + warningCount + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag, ++warningCount),
			routine,message);
		throw new CommandWarningException ( message );
	}
    
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
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
{	if ( props == null ) {
        if ( majorVersion < 10 ) {
            return "TS Alias = " + getCommandName() + "()";
        }
        else {
            return getCommandName() + "()";
        }
    }
	String Alias = props.getValue( "Alias" );
	String NewTSID = props.getValue( "NewTSID" );
	String Description = props.getValue( "Description" );
	String SetStart = props.getValue( "SetStart" );
	String SetEnd = props.getValue( "SetEnd" );
	String Units = props.getValue( "Units" );
	String MissingValue = props.getValue( "MissingValue" );
	String InitialValue = props.getValue( "InitialValue" );
	String InitialFlag = props.getValue( "InitialFlag" );
	String InitialFunction = props.getValue( "InitialFunction" );
	StringBuffer b = new StringBuffer ();
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewTSID=\"" + NewTSID + "\"" );
	}
	if ( (Description != null) && (Description.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Description=\"" + Description + "\"" );
	}
	if ( (SetStart != null) && (SetStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SetStart=\"" + SetStart + "\"" );
	}
	if ( (SetEnd != null) && (SetEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SetEnd=\"" + SetEnd + "\"" );
	}
	if ( (Units != null) && (Units.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Units=\"" + Units + "\"" );
	}
    if ( (MissingValue != null) && (MissingValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MissingValue=" + MissingValue );
    }
	if ( (InitialValue != null) && (InitialValue.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InitialValue=" + InitialValue );
	}
    if ( (InitialFlag != null) && (InitialFlag.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InitialFlag=\"" + InitialFlag + "\"" );
    }
    if ( (InitialFunction != null) && (InitialFunction.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InitialFunction=\"" + InitialFunction + "\"" );
    }
    if ( majorVersion < 10 ) {
        if ( (Alias == null) || Alias.equals("") ) {
            Alias = "Alias";
        }
        return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
    }
    else {
        if ( (Alias != null) && (Alias.length() > 0) ) {
            if ( b.length() > 0 ) {
                b.insert(0, "Alias=\"" + Alias + "\",");
            }
            else {
                b.append ( "Alias=\"" + Alias + "\"" );
            }
        }
        return getCommandName() + "("+ b.toString()+")";
    }
}

}