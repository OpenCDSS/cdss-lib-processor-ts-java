package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

import RTi.TS.DayTS;
import RTi.TS.MonthTS;
import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
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
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

/**
This class initializes, checks, and runs the NewDayTSFromMonthAndDayTS() command.
*/
public class NewDayTSFromMonthAndDayTS_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public NewDayTSFromMonthAndDayTS_Command ()
{	super();
	setCommandName ( "NewDayTSFromMonthAndDayTS" );
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
{	String Alias = parameters.getValue ( "Alias" );
    String NewTSID = parameters.getValue ( "NewTSID" );
	String MonthTSID = parameters.getValue ( "MonthTSID" );
	String DayTSID = parameters.getValue ( "DayTSID" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (Alias == null) || Alias.equals("") ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series alias when defining the command."));
	}
    if ( (NewTSID == null) || NewTSID.equals("") ) {
        message = "The new time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a new time series identifier when defining the command.  " +
                "Previously was optional but is now required."));
    }
    else {
        try {
            TSIdent tsident = TSIdent.parseIdentifier( NewTSID );
            try { TimeInterval.parseInterval(tsident.getInterval());
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
            // InvalidIntervalException so that more intelligent messages can be
            // generated.
            message = "NewTSID \"" + NewTSID + "\" is not a valid identifier." +
            "Use the command editor to enter required fields.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Use the command editor to enter required fields."));
        }
    }
	if ( (MonthTSID == null) || MonthTSID.equals("") ) {
        message = "The monthly time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a monthly time series identifier."));
	}
	if ( (DayTSID == null) || DayTSID.equals("") ) {
        message = "The daily time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a daily time series identifier."));
	}
    
    // Check for invalid parameters...
	List<String> valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "NewTSID" );
    valid_Vector.add ( "MonthTSID" );
    valid_Vector.add ( "DayTSID" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );
    
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new NewDayTSFromMonthAndDayTS_JDialog ( parent, this )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Return the list of data objects created by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
    List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discoveryTSList;
    }
    else {
        return null;
    }
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "NewDayTSFromMonthAndDayTS_Command.parseCommand", message;

	// Get the part of the command after the TS Alias =...
	int pos = command.indexOf ( "=" );
	if ( pos < 0 ) {
		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = NewDayTSFromMonthAndDayTS(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	String token0 = command.substring ( 0, pos ).trim();
	String token1 = command.substring ( pos + 1 ).trim();
	if ( (token0 == null) || (token1 == null) ) {
		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = NewDayTSFromMonthAndDayTS(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	List v = StringUtil.breakStringList ( token0, " ", StringUtil.DELIM_SKIP_BLANKS );
    if ( v == null ) {
        message = "Syntax error in \"" + command +
        "\".  Expecting:  TS Alias = NewDayTSFromMonthAndDayTS(NewTSID,MonthTSID,DayTSID)";
        Message.printWarning ( warning_level, routine, message);
        throw new InvalidCommandSyntaxException ( message );
    }
    String Alias = (String)v.get(1);
    String NewTSID = null;
    String MonthTSID = null;
    String DayTSID = null;
	if ( (token1.indexOf('=') < 0) && !token1.endsWith("()") ) {
		// No parameters have = in them...
		// TODO SAM 2008-09-23 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax without named parameters.

		v = StringUtil.breakStringList ( token1,"(),",
		        StringUtil.DELIM_SKIP_BLANKS|StringUtil.DELIM_ALLOW_STRINGS );
		if ( (v == null) || (v.size() != 4) ) {
			message = "Syntax error in \"" + command +
			"\".  Expecting:  TS Alias = NewDayTSFromMonthAndDayTS(NewTSID,MonthTSID,DayTSID)";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
        NewTSID = (String)v.get(1);
        MonthTSID = (String)v.get(2);
        DayTSID = (String)v.get(3);
	}
	else {
        // Current syntax...
        super.parseCommand( token1 );
	}
    
    // Set parameters and new defaults...

    PropList parameters = getCommandParameters();
    parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
    if ( Alias.length() > 0 ) {
        parameters.set ( "Alias", Alias );
    }
    // Reset using above information
    if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
        parameters.set ( "NewTSID", NewTSID );
    }
    if ( (MonthTSID != null) && (MonthTSID.length() > 0) ) {
        parameters.set ( "MonthTSID", MonthTSID );
    }
    if ( (DayTSID != null) && (DayTSID.length() > 0) ) {
        parameters.set ( "DayTSID", DayTSID );
    }
    parameters.setHowSet ( Prop.SET_UNKNOWN );
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "NewDayTSFromMonthAndDayTS_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Level for non-user messages

	// Make sure there are time series available to operate on...

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }
	
	String Alias = parameters.getValue ( "Alias" );
	String NewTSID = parameters.getValue ( "NewTSID" );
	String MonthTSID = parameters.getValue ( "MonthTSID" );
	String DayTSID = parameters.getValue ( "DayTSID" );

	// Get the time series to process.  The time series list is searched
	// backwards until the first match...

	TS monthTS = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        String TSList = "" + TSListType.LAST_MATCHING_TSID;
        List<TS> tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, MonthTSID, null, null );
        if ( (tslist != null) && (tslist.size() > 0) ) {
            monthTS = tslist.get(0);
        }
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        try {
            PropList request_params = new PropList ( "" );
			request_params.set ( "CommandTag", command_tag );
			request_params.set ( "TSID", MonthTSID );
			CommandProcessorRequestResultsBean bean = null;
			try {
			    bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + MonthTSID +
				"\") from processor.";
				Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
				Message.printWarning(log_level, routine, e );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
			}
			PropList bean_PropList = bean.getResultsPropList();
			Object o_TS = bean_PropList.getContents ( "TS");
			if ( o_TS == null ) {
				message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + MonthTSID +
				"\") from processor.";
				Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
			}
			else {
				monthTS = (TS)o_TS;
			}
        }
    	catch ( Exception e ) {
    	    monthTS = null;
    	}
    }
	if ( monthTS == null ) {
		message = "Unable to find monthly time series using TSID \"" + MonthTSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
		throw new CommandWarningException ( message );
	}
	
    TS dayTS = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        // FIXME - SAM 2011-02-02 This gets all the time series, not just the ones matching the request!
        String TSList = "" + TSListType.LAST_MATCHING_TSID;
        List<TS> tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, MonthTSID, null, null );
        if ( (tslist != null) && (tslist.size() > 0) ) {
            dayTS = tslist.get(0);
        }
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        try {
            PropList request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "TSID", DayTSID );
            CommandProcessorRequestResultsBean bean = null;
            try { bean =
                processor.processRequest( "GetTimeSeriesForTSID", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + DayTSID +
                "\") from processor.";
                Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
                Message.printWarning(log_level, routine, e );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_TS = bean_PropList.getContents ( "TS");
            if ( o_TS == null ) {
                message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + DayTSID +
                "\") from processor.";
                Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
            }
            else {
                dayTS = (TS)o_TS;
            }
        }
        catch ( Exception e ) {
            dayTS = null;
        }
    }
    if ( dayTS == null ) {
        message = "Unable to find daily time series using TSID \"" + DayTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }

	// Now process the time series...

	TS ts = null; // New time series to be created
	try {
	    DateTime OutputStart_DateTime = null;
	    Object o = processor.getPropContents("OutputStart");
	    if ( o != null ) {
	        OutputStart_DateTime = (DateTime)o;
	        // Make sure the date has a day precision...
	        if ( OutputStart_DateTime.getPrecision() != DateTime.PRECISION_DAY ) {
    	        OutputStart_DateTime.setPrecision(DateTime.PRECISION_DAY);
    	        OutputStart_DateTime.setDay ( 1 );
	        }
	    }
        DateTime OutputEnd_DateTime = null;
        o = processor.getPropContents("OutputEnd");
        if ( o != null ) {
            OutputEnd_DateTime = (DateTime)o;
            // Make sure the date has a day precision...
            if ( OutputEnd_DateTime.getPrecision() != DateTime.PRECISION_DAY ) {
                OutputEnd_DateTime.setPrecision(DateTime.PRECISION_DAY);
                OutputEnd_DateTime.setDay ( TimeUtil.numDaysInMonth(
                        OutputEnd_DateTime.getMonth(), OutputEnd_DateTime.getYear()) );
            }
        }
        // Get date limits from the data if not globally specified
        if ( OutputStart_DateTime == null ) {
            // Use the daily data
            OutputStart_DateTime = new DateTime(dayTS.getDate1());
        }
        if ( OutputEnd_DateTime == null ) {
            // Use the daily data
            OutputEnd_DateTime = new DateTime(dayTS.getDate2());
        }
        ts = TSUtil.newTimeSeries(NewTSID,true);
        DateTime date1 = new DateTime ( OutputStart_DateTime );
        ts.setDate1 ( date1 );
        DateTime date2 = new DateTime ( OutputEnd_DateTime);
        ts.setDate2 ( date2 );
        ts.allocateDataSpace ();
        ts.setIdentifier ( NewTSID );
        if ( commandPhase == CommandPhaseType.RUN ) {
            setUsingMonthAndDay ( (DayTS)ts, (MonthTS)monthTS, (DayTS)dayTS );
        }
		ts.setAlias ( Alias );
	}
	catch ( Exception e ) {
		message = "Unexpected error creating the new time series \"" + NewTSID + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the log file - report the problem to software support." ) );
	}

    // Update the data to the processor so that appropriate actions are taken...
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Just want time series headers initialized
        List<TS> discoveryTSList = new Vector();
        discoveryTSList.add ( ts );
        setDiscoveryTSList ( discoveryTSList );
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, ts );
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
		throw new CommandWarningException ( message );
	}
    
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
@param discoveryTSList list of time series created during discovery phase
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
}

/**
Set the contents of a daily time series using a monthly time series for the
total and a daily time series for the pattern.  This method was extracted from
TSUtil when the command was moved to a class.
*/
private void setUsingMonthAndDay ( DayTS ts, MonthTS monthts, DayTS dayts )
{   if ( (ts == null) || (monthts == null) || (dayts == null) ) {
        return;
    }
    if ( !((ts.getDataIntervalBase() == TimeInterval.DAY) && (ts.getDataIntervalMult() == 1)) ) {
        return;
    }
    if ( !((dayts.getDataIntervalBase() == TimeInterval.DAY) && (dayts.getDataIntervalMult() == 1)) ) {
        return;
    }
    if ( !((monthts.getDataIntervalBase() == TimeInterval.MONTH) && (monthts.getDataIntervalMult() == 1)) ) {
        return;
    }

    // Loop through the time series to set...

    DateTime daydate = new DateTime ( ts.getDate1() );
    // Make sure the day is 1...
    daydate.setDay(1);
    DateTime monthdate = new DateTime ( daydate );
    monthdate.setPrecision ( DateTime.PRECISION_MONTH );
    DateTime monthend = new DateTime ( ts.getDate2() );
    monthend.setPrecision ( DateTime.PRECISION_MONTH );
    double dayvalue = 0.0;
    double daytotal = 0.0;
    double monthvalue = 0.0;
    int num_days_in_month = 0;
    int i = 0;
    boolean found_missing_day = false;
    
    // Loop on the months for the time series being filled...
    for ( ; monthdate.lessThanOrEqualTo ( monthend ); monthdate.addInterval(TimeInterval.MONTH, 1) ) {
        // Get the monthly value...
        monthvalue = monthts.getDataValue(monthdate);
        if ( monthts.isDataMissing(monthvalue) ) {
            // Don't do anything for the month...
            continue;
        }
        num_days_in_month = TimeUtil.numDaysInMonth( monthdate.getMonth(), monthdate.getYear() );
        // Set the starting date for the daily date to the first of the month...
        daydate.setYear(monthdate.getYear());
        daydate.setMonth(monthdate.getMonth());
        daydate.setDay(1);
        // Get the total of the values in daily time series being used for the distribution...
        daytotal = 0.0;
        
        // reset the found missing data flag
        found_missing_day = false;
        
        for ( i = 1; i <= num_days_in_month; i++, daydate.addInterval(TimeInterval.DAY,1) ) {
            dayvalue = dayts.getDataValue ( daydate );
           
            if ( dayts.isDataMissing( dayvalue )) {    
               found_missing_day = true;
               break;
            }
            daytotal += dayvalue;
        }
        
        // If data is missing for the day, skip to next month
        if ( found_missing_day ) {
           continue;
        }
        //Message.printStatus ( 1, "", "Day total for " + monthdate.toString() + " is " + daytotal );
        // Now loop through again and fill in the time series to be
        // created by taking the monthly total value and multiplying it
        // by the ratio of the specific day to the total of the days...
        if ( daytotal > 0.0 ) {
            daydate.setYear(monthdate.getYear());
            daydate.setMonth(monthdate.getMonth());
            daydate.setDay(1);
            for ( i = 1; i <= num_days_in_month; i++, daydate.addInterval(TimeInterval.DAY,1) ) {
                dayvalue = dayts.getDataValue ( daydate );
                  
                // For now hard-code the conversion factor from ACFT to CFS...
                if ( !dayts.isDataMissing(dayvalue) ) {
                    dayvalue = (monthvalue * (1 / 1.9835)) * (dayvalue/daytotal);
                    // Message.printStatus ( 1, "", "Setting " + daydate.toString() + " to " + daytotal );
                    ts.setDataValue(daydate,dayvalue);
                }
            }
        }
    }
    ts.setDataUnits ( "CFS" );
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return "TS Alias = " + getCommandName() + "()";
	}
	String Alias = props.getValue( "Alias" );
	String NewTSID = props.getValue( "NewTSID" );
	String MonthTSID = props.getValue( "MonthTSID" );
	String DayTSID = props.getValue( "DayTSID" );
	StringBuffer b = new StringBuffer ();
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewTSID=\"" + NewTSID + "\"" );
	}
    if ( (MonthTSID != null) && (MonthTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MonthTSID=\"" + MonthTSID + "\"" );
    }
    if ( (DayTSID != null) && (DayTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DayTSID=\"" + DayTSID + "\"" );
    }
	return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
}

}
