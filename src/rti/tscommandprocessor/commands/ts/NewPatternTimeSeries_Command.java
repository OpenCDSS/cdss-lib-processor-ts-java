package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import java.util.List;
import java.util.Vector;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.IrregularTS;
import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.TS.TSUtil_SetDataValuesUsingPattern;

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
import RTi.Util.Time.TimeUtil;

/**
<p>
This class initializes, checks, and runs the NewPatternTimeSeries() command.
</p>
*/
public class NewPatternTimeSeries_Command extends AbstractCommand implements Command
{
	
/**
Pattern values as doubles.  These are created during initialization and used
during the run.
*/
private double[] __PatternValues_double = null;

// Other data are either primitives that don't need conversion or must be
// resolved at run time.

/**
Constructor.
*/
public NewPatternTimeSeries_Command ()
{	super();
	setCommandName ( "NewPatternTimeSeries" );
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
public void checkCommandParameters (	PropList parameters, String command_tag,
					int warning_level )
throws InvalidCommandParameterException
{	String routine = getClass().getName() + ".checkCommandParameters";
    String Alias = parameters.getValue ( "Alias" );
	//String NewTSID = parameters.getValue ( "NewTSID" );
	String IrregularInterval = parameters.getValue ( "IrregularInterval" );
	String SetStart = parameters.getValue ( "SetStart" );
	String SetEnd = parameters.getValue ( "SetEnd" );
	String PatternValues = parameters.getValue ( "PatternValues" );
	String warning = "";
    String message;
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
    
    TSIdent tsident = null; // Checked below to verify interval against time

	if ( (Alias == null) || Alias.equals("") ) {
        message = "The time series alias must be specified.";
		warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(
				CommandStatusType.FAILURE, message,
				"Provide a time series alias when defining the command."));
	}
    /* TODO SAM 2007-11-29 Need to evaluate whether this is a required check
	if ( (NewTSID == null) || NewTSID.equals("") ) {
        message = "The new time series identifier must be specified.";
		warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(
				CommandStatusType.FAILURE, message,
				"Provide a new time series identifier when defining the command."));
	}
	else {
		try {
            tsident = TSIdent.parseIdentifier( NewTSID );
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
    */
	
	if ( (IrregularInterval != null) && !IrregularInterval.equals("") ) {
    	try {
    	    TimeInterval.parseInterval ( IrregularInterval );
    	}
    	catch ( Exception e ) {
            message = "The irregular time series interval is not valid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message,
                    "Specify a standard interval (e.g., 6Hour, Day, Month)."));
    	}
	}
    
    if ( (PatternValues == null) || PatternValues.equals("") ) {
        message = "The pattern values must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a list of values to define the pattern."));
    }

	if ( (PatternValues != null) && !PatternValues.equals("") ) {
		// If pattern values are specified, make sure they are a sequence of numbers...
		// Allow blanks if they want to allow missing to remain
		List tokens = StringUtil.breakStringList(PatternValues, " ,", 0);
		int size = 0;
		if ( tokens != null ) {
			size = tokens.size();
		}
		String token;
		if ( size > 0 ) {
			__PatternValues_double = new double[size];
		}
		for ( int i = 0; i < size; i++ ) {
			token = (String)tokens.get(i);
			if ( !StringUtil.isDouble(token) ) {
                message = "Pattern value (" + (i + 1) + "): " + token + " is not a number.";
				warning += "\n" + message;
                status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(
					CommandStatusType.FAILURE, message,
					"Provide a valid number."));
			}
			else {
				__PatternValues_double[i] = StringUtil.atod ( token );
			}
		}
	}
	if (	(SetStart != null) && !SetStart.equals("") &&
		!SetStart.equalsIgnoreCase("OutputStart") &&
		!SetStart.equalsIgnoreCase("OutputEnd") ) {
		try {
		    DateTime dt = DateTime.parse(SetStart);
            if ( tsident!= null ) {
                Integer c = TimeUtil.compareDateTimePrecisionToTimeInterval(dt,tsident.getInterval());
                if ( (c == null) || (c.intValue() != 0) ) {
                    message = "Set start precision does not match the time series data interval \"" +
                    tsident.getInterval() + "\" interval.";
                    warning += "\n" + message;
                    status.addToLog(CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(
                                    CommandStatusType.FAILURE, message,
                            "Specify the set start date/time to a precision that matches the time series data."));
                }
            }
		}
		catch ( Exception e ) {
            message = "The set start date \"" + SetStart + "\" is not a valid date.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(
					CommandStatusType.FAILURE, message,
					"Specify a date, OutputStart, or OutputEnd."));
		}
	}
	if (	(SetEnd != null) && !SetEnd.equals("") &&
		!SetEnd.equalsIgnoreCase("OutputStart") &&
		!SetEnd.equalsIgnoreCase("OutputEnd") ) {
		try {
            DateTime dt = DateTime.parse( SetEnd );
            if ( tsident!= null ) {
                Integer c = TimeUtil.compareDateTimePrecisionToTimeInterval(dt,tsident.getInterval());
                if ( (c == null) || (c.intValue() != 0) ) {
                    message = "Set end precision does not match the time series data interval \"" +
                    tsident.getInterval() + "\" interval.";
                    warning += "\n" + message;
                    status.addToLog(CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(
                                CommandStatusType.FAILURE, message,
                        "Specify the set start date/time to a precision that matches the time series data."));
                }
            }
		}
		catch ( Exception e ) {
            message = "The set end date \"" + SetEnd + "\" is not a valid date.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(
					CommandStatusType.FAILURE, message,
					"Specify a date, OutputStart, or OutputEnd."));
		}
	}
	
	// Check for invalid parameters...
	List valid_Vector = new Vector();
	valid_Vector.add ( "Alias" );
	valid_Vector.add ( "NewTSID" );
	valid_Vector.add ( "IrregularInterval" );
	valid_Vector.add ( "Description" );
	valid_Vector.add ( "SetStart" );
	valid_Vector.add ( "SetEnd" );
	valid_Vector.add ( "Units" );
	valid_Vector.add ( "PatternValues" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );
	
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new NewPatternTimeSeries_JDialog ( parent, this )).ok();
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
{	int warning_level = 2;
	String routine = "NewPatternTimeSeries.parseCommand", message;

	// Get the part of the command after the TS Alias =...
	int pos = command.indexOf ( "=" );
	if ( pos < 0 ) {
		message = "Syntax error in \"" + command +
			"\".  Expecting:  TS Alias = NewPatternTimeSeries(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	String token0 = command.substring ( 0, pos ).trim();	// TS Alias
	String token1 = command.substring ( pos + 1 ).trim();	// command(...)
	if ( (token0 == null) || (token1 == null) ) {
		message = "Syntax error in \"" + command +
			"\".  Expecting:  TS Alias = NewPatternTimeSeries(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}

	// Get the alias from the first token before the equal sign...
	
	List v = StringUtil.breakStringList ( token0, " ", StringUtil.DELIM_SKIP_BLANKS );
	if ( (v == null) || (v.size() != 2) ) {
		message = "Syntax error in \"" + command +
			"\".  Expecting:  TS Alias = NewPatternTimeSeries(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	String Alias = (String)v.get(1);

	// Get the command parameters from the token on the right of the =...

	List tokens = StringUtil.breakStringList ( token1, "()", 0 );
	if ( (tokens == null) || (tokens.size() < 2) ) {
		// Must have at least the command name and its parameters...
		message = "Syntax error in \"" + command + "\". Expecting:  TS Alias = NewPatternTimeSeries(...)";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}

	try {
        PropList parameters = PropList.parse ( Prop.SET_FROM_PERSISTENT,
			(String)tokens.get(1), routine, "," );
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
{	String routine = "NewPatternTimeSeries.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Level for non-user warnings to go to log file.
	
	// Get and clear the status and clear the run log...
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters ();
	CommandProcessor processor = getCommandProcessor();

	String Alias = parameters.getValue ( "Alias" );
	String NewTSID = parameters.getValue ( "NewTSID" );
	String IrregularInterval = parameters.getValue ( "IrregularInterval" );
	String Description = parameters.getValue ( "Description" );
	String SetStart = parameters.getValue ( "SetStart" );
	String SetEnd = parameters.getValue ( "SetEnd" );
	String Units = parameters.getValue ( "Units" );

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
			bean =
			processor.processRequest( "DateTime", request_params);
			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for SetStart DateTime(DateTime=" +
				"OutputStart" +	"\") returned from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
				status.addToLog(CommandPhaseType.RUN,
						new CommandLogRecord(
						CommandStatusType.FAILURE,message,
						"Specify SetStart or make sure that a setOutputPeriod() command has been specified prior to this command."));
				throw new InvalidCommandParameterException ( message );
			}
			else {	SetStart_DateTime = (DateTime)prop_contents;
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
				message = "Null value for SetStart DateTime(DateTime=" +
				SetStart +	"\") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
				throw new InvalidCommandParameterException ( message );
			}
			else {	SetStart_DateTime = (DateTime)prop_contents;
			}
		}
	}
	catch ( Exception e ) {
		message = "SetStart \"" + SetStart + "\" is invalid." +
		"  Specify a valid SetStart or global OutputStart.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		Message.printWarning(2, routine, e);
		throw new InvalidCommandParameterException ( message );
	}
	
	try {
		if ( (SetEnd == null) || SetEnd.equals("") ) {
			// Try to set SetEnd from global OutputEnd...
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", "OutputEnd" );
			CommandProcessorRequestResultsBean bean = null;
			bean =
			processor.processRequest( "DateTime", request_params);
			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for SetEnd DateTime(DateTime=OutputEnd) returned from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
				status.addToLog(CommandPhaseType.RUN,
						new CommandLogRecord(
						CommandStatusType.FAILURE, message,
						"Specify SetEnd or make sure that a setOutputPeriod() command has been specified prior to this command."));
				throw new InvalidCommandParameterException ( message );
			}
			else {	SetEnd_DateTime = (DateTime)prop_contents;
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
				SetEnd + ") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog(CommandPhaseType.RUN,
                        new CommandLogRecord(
                        CommandStatusType.FAILURE, message,
                        "Report the problem to software support."));
				throw new InvalidCommandParameterException ( message );
			}
			else {	SetEnd_DateTime = (DateTime)prop_contents;
			}
		}
	}
	catch ( Exception e ) {
		message = "SetEnd \"" + SetEnd + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify a valid SetEnd or global OutputStart."));
		throw new InvalidCommandParameterException ( message );
	}
	
	// Now process the time series...

	TS ts = null;
	try {	// Create the time series...
		ts = TSUtil.newTimeSeries ( NewTSID, true );
		if ( ts == null ) {
			throw new Exception ( "Null time series." );
		}
	}
	catch ( Exception e ) {
		message = "Unable to create an empty new time series using NewTSID=\""+ NewTSID + "\".";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(
				CommandStatusType.FAILURE, message,
				"Verify the TSID format using the command editor."));
		throw new CommandException ( message );
	}
	try {
        // Try to fill out the time series. Allocate memory and set other information...
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
			status.addToLog(CommandPhaseType.RUN,
					new CommandLogRecord(
					CommandStatusType.FAILURE, message,
					"Verify that the output period is not huge and check computer memory."));
		}
		if ( (__PatternValues_double != null) && (__PatternValues_double.length > 0) ) {
		    if ( ts instanceof IrregularTS ) {
		        // Irregular time series don't have a regular data space so need to fill in some
		        // missing values first.  Use the period to iterate and define missing values.
		        DateTime end = new DateTime ( ts.getDate2() );
		        TimeInterval tsinterval = null;
		        try {
		            tsinterval = TimeInterval.parseInterval ( IrregularInterval );
		        }
		        catch ( Exception e ) {
		            message = "Unable to allocate memory for time series.";
		            Message.printWarning ( warning_level,
		            MessageUtil.formatMessageTag(
		            command_tag,++warning_count),routine,message );
		            status.addToLog(CommandPhaseType.RUN,
		                    new CommandLogRecord(
		                    CommandStatusType.FAILURE, message,
		                    "Verify that the output period is not huge and check computer memory."));
		        }
		        double missing = ts.getMissing();
		        for ( DateTime date = new DateTime(ts.getDate1()); date.lessThanOrEqualTo(end);
		            date.addInterval(tsinterval.getBase(),tsinterval.getMultiplier())) {
		            ts.setDataValue(date, missing );
		        }
		    }
		    // Set the data.  This will reset the initial missing values in the time series.
			TSUtil_SetDataValuesUsingPattern tsworker = new TSUtil_SetDataValuesUsingPattern ();
			tsworker.setDataValuesUsingPattern ( ts, SetStart_DateTime, SetEnd_DateTime, __PatternValues_double );
		}
        
        ts.setAlias ( Alias );

		// Further process the time series...
        // This makes sure the period is at least as long as the output period, and computes the historical averages.
        List tslist = new Vector();
        tslist.add ( ts );
        PropList request_params = new PropList ( "" );
        request_params.setUsingObject ( "TSList", tslist );
        try {
            processor.processRequest( "ReadTimeSeries2", request_params);
        }
        catch ( Exception e ) {
            message = "Error post-processing new pattern time series.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,
                ++warning_count), routine, message );
            Message.printWarning(log_level, routine, e);
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
            throw new CommandException ( message );
        }
        
        // Update the data to the processor...
        
        try {
            TSCommandProcessorUtil.appendTimeSeriesToResultsList ( processor, this, ts );
        }
        catch ( Exception e ){
                message = "Cannot append new time series to results list.  Skipping.";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(
                    command_tag, ++warning_count),
                    routine,message);
                status.addToLog(CommandPhaseType.RUN,
                        new CommandLogRecord(
                        CommandStatusType.FAILURE, message,
                        "Unable to provide recommendation - check log file for details."));
        }
	}
	catch ( Exception e ) {
		message = "Unexpected error creating a new pattern time series for \""+ NewTSID + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(
				CommandStatusType.FAILURE, message,
				"Report the problem to software support - check log file for details."));
	}

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
	String IrregularInterval = props.getValue( "IrregularInterval" );
	String Description = props.getValue( "Description" );
	String SetStart = props.getValue( "SetStart" );
	String SetEnd = props.getValue( "SetEnd" );
	String Units = props.getValue( "Units" );
	String PatternValues = props.getValue( "PatternValues" );
	StringBuffer b = new StringBuffer ();
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewTSID=\"" + NewTSID + "\"" );
	}
   if ( (IrregularInterval != null) && (IrregularInterval.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IrregularInterval=" + IrregularInterval );
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
	if ( (PatternValues != null) && (PatternValues.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "PatternValues=\"" + PatternValues + "\"" );
	}
	return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
}

}
