package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.Vector;

import RTi.TS.DayTS;
import RTi.TS.MonthTS;
import RTi.TS.TS;
import RTi.TS.TSUtil;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
<p>
This class initializes, checks, and runs the FillDayTSFrom2MonthTSAnd1DayTS() command.
</p>
*/
public class FillDayTSFrom2MonthTSAnd1DayTS_Command extends AbstractCommand implements Command
{
    
/**
Constructor.
*/
public FillDayTSFrom2MonthTSAnd1DayTS_Command ()
{	super();
	setCommandName ( "FillDayTSFrom2MonthTSAnd1DayTS" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor
dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String TSID_D1 = parameters.getValue ( "TSID_D1" );
	String TSID_M1 = parameters.getValue ( "TSID_M1" );
	String TSID_M2 = parameters.getValue ( "TSID_M2" );
	String TSID_D2 = parameters.getValue ( "TSID_D2" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
	if ( (TSID_D1 == null) || (TSID_D1.length() == 0) ) {
        message = "A TSID_D1 value must be specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the TSID_D1 parameter value." ) );
	}
    if ( (TSID_M1 == null) || (TSID_M1.length() == 0) ) {
        message = "A TSID_M1 value must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the TSID_M1 parameter value." ) );
    }
    if ( (TSID_M2 == null) || (TSID_M2.length() == 0) ) {
        message = "A TSID_M2 value must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the TSID_M2 parameter value." ) );
    }
    if ( (TSID_D2 == null) || (TSID_D1.length() == 0) ) {
        message = "A TSID_D2 value must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the TSID_D2 parameter value." ) );
    }

 	if ( (FillStart != null) && !FillStart.equals("") && !FillStart.equalsIgnoreCase("OutputStart")){
		try {	DateTime.parse(FillStart);
		}
		catch ( Exception e ) {
            message = "The fill start date/time \"" + FillStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	if ( (FillEnd != null) && !FillEnd.equals("") && !FillEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse( FillEnd);
		}
		catch ( Exception e ) {
            message = "The fill end date/time \"" + FillStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
   
	// Check for invalid parameters...
    Vector valid_Vector = new Vector();
    valid_Vector.add ( "TSID_D1" );
    valid_Vector.add ( "TSID_M1" );
    valid_Vector.add ( "TSID_M2" );
    valid_Vector.add ( "TSID_D2" );
    valid_Vector.add ( "FillStart" );
    valid_Vector.add ( "FillEnd" );
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
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new FillDayTSFrom2MonthTSAnd1DayTS_JDialog ( parent, this )).ok();
}

/**
Get a time series given the identifier.
*/
private TS getTimeSeries ( String tsid, int warningLevel, String command_tag, int warning_count )
{   String routine = "FillDayTSFrom2MonthTSAnd1DayTS_Command.getTimeSeries";
    CommandProcessor processor = getCommandProcessor();
    PropList request_params = new PropList ( "" );
    request_params.set ( "TSID", tsid );
    request_params.set ( "CommandTag", command_tag );
    CommandProcessorRequestResultsBean bean = null;
    String message;
    CommandStatus status = getCommandStatus();
    try {
        bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + tsid + "\") from processor.";
        Message.printWarning ( 3, routine, e );
        Message.printWarning(warningLevel,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the identifier is correct." ) );
        return null;
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TS = bean_PropList.getContents ( "TS" );
    if ( o_TS == null ) {
        message = "Null time series returned from processor for request GetTimeSeriesForTSID(TSID=\"" +
        tsid + "\").";
        Message.printWarning ( warningLevel,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the identifier is correct." ) );
    }
    else {
        TS ts = (TS)o_TS;
        return ts;
    }
    return null;
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "FillDayTSFrom2MonthTSAnd1DayTS_Command.parseCommand", message;

	if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()")) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
		// TODO SAM 2008-09-19 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax
		Vector v = StringUtil.breakStringList(command_string,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS |	StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		if ( ntokens != 5 ) {
			// Command name, TSID_D1, TSID_M1, TSID_M2, TSID_D2...
			message = "Syntax error in \"" + command_string +
			"\".  Expecting FillDayTSFrom2MonthTSAnd1DayTS(TSID_D1,TSID_M1,TSID_M2,TSID_D2).";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}

		// Get the individual tokens of the expression...

		String TSID_D1 = ((String)v.elementAt(1)).trim();
        String TSID_M1 = ((String)v.elementAt(2)).trim();
        String TSID_M2 = ((String)v.elementAt(3)).trim();
        String TSID_D2 = ((String)v.elementAt(4)).trim();

		// Set parameters and new defaults...

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID_D1.length() > 0 ) {
			parameters.set ( "TSID_D1", TSID_D1 );
		}
        if ( TSID_M1.length() > 0 ) {
            parameters.set ( "TSID_M1", TSID_M1 );
        }
        if ( TSID_M2.length() > 0 ) {
            parameters.set ( "TSID_M2", TSID_M2 );
        }
        if ( TSID_D2.length() > 0 ) {
            parameters.set ( "TSID_D2", TSID_D2 );
        }
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
}

/**
Run the command.
@param command_number number of command to run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "FillDayTSFrom2MonthTSAnd1DayTS_Command.runCommand", message;
	int warning_count = 0;
	int warningLevel = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Warning message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);

	String TSID_D1 = parameters.getValue ( "TSID_D1" );
    String TSID_M1 = parameters.getValue ( "TSID_M1" );
    String TSID_M2 = parameters.getValue ( "TSID_M2" );
    String TSID_D2 = parameters.getValue ( "TSID_D2" );
    String FillStart = parameters.getValue ( "FillStart" );
    String FillEnd = parameters.getValue ( "FillEnd" );

	// Get the time series to process...

	TS tsD1 = getTimeSeries ( TSID_D1, warningLevel, command_tag, warning_count );
	if ( tsD1 == null ) {
        message = "Unable to find time series \"" + TSID_D1 + "\" for processing.";
        Message.printWarning(warningLevel,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the identifier is correct." ) );
	}
    TS tsM1 = getTimeSeries ( TSID_M1, warningLevel, command_tag, warning_count );
    if ( tsD1 == null ) {
        message = "Unable to find time series \"" + TSID_M1 + "\" for processing.";
        Message.printWarning(warningLevel,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the identifier is correct." ) );
    }
    TS tsM2 = getTimeSeries ( TSID_M2, warningLevel, command_tag, warning_count );
    if ( tsD1 == null ) {
        message = "Unable to find time series \"" + TSID_M2 + "\" for processing.";
        Message.printWarning(warningLevel,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the identifier is correct." ) );
    }
    TS tsD2 = getTimeSeries ( TSID_D2, warningLevel, command_tag, warning_count );
    if ( tsD1 == null ) {
        message = "Unable to find time series \"" + TSID_D2 + "\" for processing.";
        Message.printWarning(warningLevel,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the identifier is correct." ) );
    }

	// Fill period...

	// Figure out the dates to use for the analysis...
	DateTime FillStart_DateTime = null;
	DateTime FillEnd_DateTime = null;
    PropList request_params;
    CommandProcessorRequestResultsBean bean = null;
    CommandProcessor processor = getCommandProcessor();
	try {
	if ( FillStart != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", FillStart );
		bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting FillStart DateTime(DateTime=" +	FillStart + ") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for FillStart DateTime(DateTime=" + FillStart + "\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	FillStart_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "FillStart \"" + FillStart + "\" is invalid.";
		Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time or OutputStart." ) );
		throw new InvalidCommandParameterException ( message );
	}
	
	try {
	if ( FillEnd != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", FillEnd );
		bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting FillEnd DateTime(DateTime=" + FillEnd + "\") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for FillEnd DateTime(DateTime=" + FillEnd +	"\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	FillEnd_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "FillEnd \"" + FillEnd + "\" is invalid.";
		Message.printWarning(warningLevel,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time or OutputEnd." ) );
		throw new InvalidCommandParameterException ( message );
	}

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Insufficient data to run command.";
		Message.printWarning ( warningLevel,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}

	// Now process the time series...

	Message.printStatus ( 2, routine, "Filling D1=\"" + TSID_D1+ "\" using M1=\"" +
            TSID_M1 + "\", M2=\"" + TSID_M2 + "\", and D2=\"" + TSID_D2 + "\"" );
	try {
        TSUtil.fillDayTSFrom2MonthTSAnd1DayTS (
                (DayTS)tsD1, (MonthTS)tsM1, (DayTS)tsD2, (MonthTS)tsM2, FillStart_DateTime, FillEnd_DateTime );
	}
	catch ( Exception e ) {
		message = "Unexpected error filling time series \"" + TSID_D1 + "\" (" + e + ").";
        Message.printWarning ( warningLevel,
            MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
		Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "See the log file for details - report the problem to software support." ) );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
		throw new CommandWarningException ( message );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TSID_D1 = props.getValue( "TSID_D1" );
    String TSID_M1 = props.getValue( "TSID_M1" );
    String TSID_M2 = props.getValue( "TSID_M2" );
    String TSID_D2 = props.getValue( "TSID_D2" );
	String FillStart = props.getValue("FillStart");
	String FillEnd = props.getValue("FillEnd");
	//String FillFlag = props.getValue("FillFlag");
	StringBuffer b = new StringBuffer ();
    if ( (TSID_D1 != null) && (TSID_D1.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSID_D1=\"" + TSID_D1 + "\"");
    }
    if ( (TSID_M1 != null) && (TSID_M1.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSID_M1=\"" + TSID_M1 + "\"" );
    }
    if ( (TSID_M2 != null) && (TSID_M2.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSID_M2=\"" + TSID_M2 + "\"" );
    }
    if ( (TSID_D2 != null) && (TSID_D2.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSID_D2=\"" + TSID_D2 + "\"");
    }
	if ( (FillStart != null) && (FillStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillStart=\"" + FillStart + "\"" );
	}
	if ( (FillEnd != null) && (FillEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillEnd=\"" + FillEnd + "\"" );
	}

	return getCommandName() + "(" + b.toString() + ")";
}

}
