package rti.tscommandprocessor.commands.util;

import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
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

/**
<p>
This class initializes, checks, and runs the Free() command.
</p>
</p>
*/
public class Free_Command extends AbstractCommand implements Command
{
	
/**
Constructor.
*/
public Free_Command ()
{	super();
	setCommandName ( "Free" );
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
{	String TSID = parameters.getValue ( "TSID" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	if ( (TSID == null) || (TSID.length() == 0) ) {
		message = "A time series identifier pattern must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify a time series identifier." ) );
	}
	
	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
	valid_Vector.add ( "TSID" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
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
	return (new Free_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{   String routine = "Free_Command.parseCommand", message;
    int warning_level = 2;
    if ( (command_string.indexOf("=") > 0) || command_string.endsWith("()") ) {
        // New syntax, can be blank parameter list for new command...
        super.parseCommand ( command_string );
    }
    else {  // Parse the old command...
        Vector tokens = StringUtil.breakStringList ( command_string,"(,)", StringUtil.DELIM_ALLOW_STRINGS );
        if ( tokens.size() != 2 ) {
            message =
            "Invalid syntax for command.  Expecting Free(TSID).";
            Message.printWarning ( warning_level, routine, message);
            throw new InvalidCommandSyntaxException ( message );
        }
        String TSID = ((String)tokens.elementAt(1)).trim();
        PropList parameters = new PropList ( getCommandName() );
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        if ( TSID.length() > 0 ) {
            parameters.set ( "TSID", TSID );
        }
        parameters.setHowSet ( Prop.SET_UNKNOWN );
        setCommandParameters ( parameters );
    }
}

/**
Remove a time series at the indicated index.
*/
private int removeTimeSeriesAtIndex ( CommandProcessor processor, String TSID,
        Object o_Index, CommandStatus status,
        int warning_count, int warning_level, String command_tag )
{   String message;
    String routine = getClass().getName() + ".removeTimeSeriesAtIndex";
    
    // Have the index of the single time series to free.  Get the time series so a relevant
    // status message can be printed.
    
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "Index", o_Index );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = processor.processRequest( "GetTimeSeries", request_params );
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeries(Index=\"" + o_Index + "\") from processor.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                   message, "Report problem to software support." ) );
        return warning_count;
    }

    // Get the time series out of the results bean...
    
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TS = bean_PropList.getContents ( "TS" );
    if ( o_TS == null ) {
            message = "Unable to find time series \"" + TSID + "\" for Free() command.";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Verify that the TSID pattern matches 1+ time series identifiers - may be OK if a partial run." ) );
            //throw new Exception ( message );
            return warning_count;
    }
    TS ts = (TS)o_TS;

    // Now actually remove the time series...
    
    PropList request_params2 = new PropList ( "" );
    request_params2.set ( "Index", "" + o_Index );
    try {
        processor.processRequest( "RemoveTimeSeriesFromResultsList", request_params );
        if ( ts.getAlias().length() > 0 ) {
            // Print alias and identifier...
            Message.printStatus ( 2, routine,
              "Freed time series resources for \"" + ts.getAlias() + "\" \"" +
              ts.getIdentifierString() + "\" at [" + o_Index +"]");
        }
        else {  
            // Print only the identifier
            Message.printStatus ( 2, routine, "Freed time series resources for \"" +
            ts.getIdentifierString() + "\" at [" + o_Index +"]");
        }
    }
    catch ( Exception e ) {
        message = "Error requesting RemoveTimeSeriesFromResultsList(Index=\"" + o_Index + "\") from processor.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                   message, "Report problem to software support." ) );
        return warning_count;
    }
    
    return warning_count;
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "Free_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	String TSID = parameters.getValue ( "TSID" );
	
    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);

    int count = 0;  // Number of time series removed.
    // Get the original count of time series...
    Object o = null;
    try {
        o = processor.getPropContents ( "TSResultsListSize" );
    }
    catch ( Exception e ) {
        message = "Error requesting TSResultsListSize from processor.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                   message, "Report problem to software support." ) );
    }
    if ( o == null ) {
        message = "TSResultsListSize returned as null from processor.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                   message, "Report problem to software support." ) );
    }
    int tslist_size = ((Integer)o).intValue();
    if ( TSID.equals("*") ) {
        // Free all time series...
        PropList request_params = new PropList ( "" );
        //CommandProcessorRequestResultsBean bean = null;
        try {
            //bean = 
            processor.processRequest( "RemoveAllFromTimeSeriesResultsList", request_params);
            count = tslist_size;    // All will have been removed so no warning below.
        }
        catch ( Exception e ) {
            message = "Error requesting RemoveAllFromTimeSeriesResultsList(TSID=\"" + TSID +
            "\") from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
        }
    }
    else if ( TSID.indexOf('*') < 0 ) {
        // Specific identifier (no wildcard) - find it and be done...
        PropList request_params = new PropList ( "" );
        request_params.set ( "TSID", TSID );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "IndexOf", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting IndexOf(TSID=\"" + TSID + "\") from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
        }
        finally {
            // Should have Index of time series to process...
            PropList bean_PropList = bean.getResultsPropList();
            Object o_Index = bean_PropList.getContents ( "Index" );
            if ( o_Index == null ) {
                message = "Unable to find time series to free using TSID=\"" + TSID + "\".";
                Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag(
                                command_tag,++warning_count), routine, message );
            }
            else {
                int warning_count2 = removeTimeSeriesAtIndex ( processor, TSID, o_Index, status,
                        0, warning_level, command_tag );
                warning_count += warning_count2;
                if ( warning_count2 == 0 ) {
                    // Able to remove so increment the count of removed...
                    ++count;
                }
            } 
        }
    }
    else {
        // Else (wild card) - search through all the time series in memory,
        // freeing those with identifiers that match the pattern...
        try {
            o = processor.getPropContents ( "TSResultsList" );
        }
        catch ( Exception e ) {
            message = "Error requesting TSResultsList from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
        }
        for ( int its = 0; its < tslist_size; its++ ) {
            PropList request_params = new PropList ( "" );
            Integer o_Index = new Integer(its);
            request_params.setUsingObject ( "Index", o_Index );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "GetTimeSeries", request_params );
            }
            catch ( Exception e ) {
                message = "Error requesting GetTimeSeries(Index=\"" + o_Index + "\") from processor.";
                Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
                // Go to the next item in the list
                continue;
            }

            // Get the time series out of the results bean...
            
            PropList bean_PropList = bean.getResultsPropList();
            Object o_TS = bean_PropList.getContents ( "TS" );
            if ( o_TS == null ) {
                    message = "Unable to find time series \"" + TSID + "\" for Free() command.";
                    Message.printWarning ( 2, routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Verify that the TSID pattern matches 1+ time series identifiers - may be OK if a partial run." ) );
                    //throw new Exception ( message );
                    continue;
            }
            
            // Actually do the remove...
            TS ts = (TS)o_TS;
            if ( !ts.getIdentifier().matches(TSID) ) {
                continue;
            }
            else {
                int warning_count2 = removeTimeSeriesAtIndex ( processor, TSID, o_Index, status,
                        0, warning_level, command_tag );
                warning_count += warning_count2;
                if ( warning_count2 == 0 ) {
                    // Able to remove so increment the count of removed and decrement other
                    // loop variables to process the next time series...
                    --tslist_size;
                    --its;
                    ++count;
                }
            }
        }
    }
    if ( count == 0 ) {
        // Maybe an error but could be OK for a partial run.
        message = "No time series were matched for \"" + this + "\"";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( CommandPhaseType.RUN,
           new CommandLogRecord(CommandStatusType.WARNING,
              message, "Verify that the TSID pattern matches 1+ time series identifiers - may be OK if a partial run." ) );
        //throw new Exception ( message );
    }
    else {
        Message.printStatus(2, routine, "Removed (freed) " + count + " time series.");
    }
    
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String TSID = parameters.getValue ( "TSID" );
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
