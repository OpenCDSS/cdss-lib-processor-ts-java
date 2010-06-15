//------------------------------------------------------------------------------
// newTimeSeries - handle the TS Alias = newTimeSeries() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-09-20	Steven A. Malers, RTi	Initial version.  Copy and modify
//					newStatisticYearTS().
// 2007-02-12	SAM, RTi				Remove direct dependence on
//					TSCommandProcessor.
//					Clean up code based on Eclipse feedback.
// 2007-03-12	SAM, RTi				Add check to make sure NewTSID has a
//							valid interval.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the NewTimeSeries() command.
*/
public class NewTimeSeries_Command extends AbstractCommand
implements Command
{

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
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String Alias = parameters.getValue ( "Alias" );
	String NewTSID = parameters.getValue ( "NewTSID" );
	String SetStart = parameters.getValue ( "SetStart" );
	String SetEnd = parameters.getValue ( "SetEnd" );
	String InitialValue = parameters.getValue ( "InitialValue" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (Alias == null) || Alias.equals("") ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an alias." ) );
	}
	if ( (NewTSID == null) || NewTSID.equals("") ) {
        message = "The new time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a new time series identifier." ) );
	}
	else {
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
			// InvalidIntervalException so that more intelligent messages can be
			// generated.
            message = "NewTSID is not a valid identifier.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Use the command editor to enter required fields." ) );
		}
	}

	if ( (InitialValue != null) && !InitialValue.equals("") ) {
		// If an initial value is specified, make sure it is a number...
		if ( !StringUtil.isDouble(InitialValue) ) {
            message = "The initial value (" + InitialValue + ") is not a number.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the initial value as a number." ) ); 
		}
	}
	if (	(SetStart != null) && !SetStart.equals("") &&
		!SetStart.equalsIgnoreCase("OutputStart") &&
		!SetStart.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse(SetStart);
		}
		catch ( Exception e ) {
            message = "The set start \"" + SetStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
		}
	}
	if (	(SetEnd != null) && !SetEnd.equals("") &&
		!SetEnd.equalsIgnoreCase("OutputStart") &&
		!SetEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse( SetEnd );
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
	List valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "NewTSID" );
    valid_Vector.add ( "Description" );
    valid_Vector.add ( "SetStart" );
    valid_Vector.add ( "SetEnd" );
    valid_Vector.add ( "Units" );
    valid_Vector.add ( "InitialValue" );
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
	return (new NewTimeSeries_JDialog ( parent, this )).ok();
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
	String routine = "NewTimeSeries.parseCommand", message;

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

	// Get the alias from the first token before the equal sign...
	
	List<String> v = StringUtil.breakStringList ( token0, " ", StringUtil.DELIM_SKIP_BLANKS );
	if ( (v == null) || (v.size() != 2) ) {
		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = NewTimeSeries(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	String Alias = v.get(1);

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

/**
Run the command.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "newTimeSeries.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Level for non-user warnings to go to log file.

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters ();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);

	String Alias = parameters.getValue ( "Alias" );
	String NewTSID = parameters.getValue ( "NewTSID" );
	String Description = parameters.getValue ( "Description" );
	String SetStart = parameters.getValue ( "SetStart" );
	String SetEnd = parameters.getValue ( "SetEnd" );
	String Units = parameters.getValue ( "Units" );
	String InitialValue = parameters.getValue ( "InitialValue" );
	double InitialValue_double = 0.0;
	if ( (InitialValue != null) && (InitialValue.length() > 0) ) {
		InitialValue_double = Double.parseDouble ( InitialValue );
	}
	if ( SetStart == null ) {
		SetStart = "";	// Makes for better messages
	}
	if ( SetEnd == null ) {
		SetEnd = "";	// Better messages
	}

	// Figure out the dates to use for the Set...
	DateTime SetStart_DateTime = null;
	DateTime SetEnd_DateTime = null;
	
	try {
		if ( (SetStart == null) || SetStart.equals("") ) {
			// Try to set SetStart from global OutputStart...
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", "OutputStart" );
			CommandProcessorRequestResultsBean bean = null;
			bean = processor.processRequest( "DateTime", request_params);
			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for SetStart DateTime(DateTime=OutputStart) returned from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Use a SetOutputPeriod() command or specify the set start." ) );
				throw new InvalidCommandParameterException ( message );
			}
			else {
			    SetStart_DateTime = (DateTime)prop_contents;
			}
		}
		else {
			// Try to set from what user specified...
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", SetStart );
			CommandProcessorRequestResultsBean bean = null;
			bean = processor.processRequest( "DateTime", request_params);
			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for SetStart DateTime(DateTime=" +SetStart +	") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Use a SetOutputPeriod() command or specify the set start." ) );
				throw new InvalidCommandParameterException ( message );
			}
			else {	SetStart_DateTime = (DateTime)prop_contents;
			}
		}
	}
	catch ( Exception e ) {
		message = "SetStart \"" + SetStart + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		Message.printWarning(2, routine, e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid set start date/time, or as OutputStart or OutputEnd." ) );
		throw new InvalidCommandParameterException ( message );
	}
	
	try {
		if ( (SetEnd == null) || SetEnd.equals("") ) {
			// Try to set SetEnd from global OutputEnd...
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", "OutputEnd" );
			CommandProcessorRequestResultsBean bean = null;
			bean = processor.processRequest( "DateTime", request_params);
			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for SetEnd DateTime(DateTime=" +
				"OutputEnd" +	"\") returned from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Use a SetOutputPeriod() command or specify the set end." ) );
				throw new InvalidCommandParameterException ( message );
			}
			else {
			    SetEnd_DateTime = (DateTime)prop_contents;
			}
		}
		else {
			// Try to set from what user specified...
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", SetEnd );
			CommandProcessorRequestResultsBean bean = null;
			bean = processor.processRequest( "DateTime", request_params);
			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for SetStart DateTime(DateTime=" +
				SetEnd +	"\") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Use a SetOutputPeriod() command or specify the set end." ) );
				throw new InvalidCommandParameterException ( message );
			}
			else {
			    SetEnd_DateTime = (DateTime)prop_contents;
			}
		}
	}
	catch ( Exception e ) {
		message = "SetEnd \"" + SetEnd + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid set start date/time, or as OutputStart or OutputEnd." ) );
		throw new InvalidCommandParameterException ( message );
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
                    command_tag,++warning_count),routine,message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify the NewTSID - contact software support if necessary." ) );
			throw new Exception ( "Null time series." );
		}
	}
	catch ( Exception e ) {
		message = "Unexpected error creating the new time series using NewTSID=\""+	NewTSID + "\".";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the NewTSID - contact software support if necessary." ) );
		throw new CommandException ( message );
	}
	try {
        // Try to fill out the time series.  Allocate memory and set other information...
		ts.setIdentifier ( NewTSID );
		if ( (Description != null) && (Description.length() > 0) ) {
			ts.setDescription ( Description );
		}
		if ( (Units != null) && (Units.length() > 0) ) {
			ts.setDataUnits ( Units );
			ts.setDataUnitsOriginal ( Units );
		}
		ts.setDate1 ( SetStart_DateTime );
		ts.setDate1Original ( SetStart_DateTime );
		ts.setDate2 ( SetEnd_DateTime );
		ts.setDate2Original ( SetEnd_DateTime );
		if ( ts.allocateDataSpace() != 0 ) {
			message = "Unable to allocate memory for time series.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the period for the time series is not huge." ) );
		}
		if ( (InitialValue != null) && (InitialValue.length() > 0) ) {
			TSUtil.setConstant ( ts, InitialValue_double );
		}
		ts.setAlias ( Alias );
	}
	catch ( Exception e ) {
        message ="Unexpected error generating the statistic time series from \""+
        ts.getIdentifier() + "\" (" + e + ").";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count),routine,message );
        Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE,
            message, "See the log file for details." ) );
	}

	// Update the data to the processor so that appropriate actions are taken...

    TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, ts);

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
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
	String Alias = props.getValue( "Alias" );
	String NewTSID = props.getValue( "NewTSID" );
	String Description = props.getValue( "Description" );
	String SetStart = props.getValue( "SetStart" );
	String SetEnd = props.getValue( "SetEnd" );
	String Units = props.getValue( "Units" );
	String InitialValue = props.getValue( "InitialValue" );
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
	if ( (InitialValue != null) && (InitialValue.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InitialValue=" + InitialValue );
	}
	return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
}

}
