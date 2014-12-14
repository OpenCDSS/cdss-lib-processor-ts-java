package rti.tscommandprocessor.commands.ts;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;
import RTi.TS.TSUtil_SortTimeSeries;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;

/**
This class initializes, checks, and runs the SortTimeSeries() command.
*/
public class SortTimeSeries_Command extends AbstractCommand implements Command
{
/**
Values for TSID property.
*/
protected final String _AliasTSID = "AliasTSID";
protected final String _TSID = "TSID";

/**
Values for SortOrder property.
*/
protected final String _Ascending = "Ascending";
protected final String _Descending = "Descending";

/**
Constructor.
*/
public SortTimeSeries_Command ()
{	super();
	setCommandName ( "SortTimeSeries" );
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
{   String TSIDFormat = parameters.getValue ( "TSIDFormat" );
    String SortOrder = parameters.getValue ( "SortOrder" );
    
    String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    if ( (TSIDFormat != null) && !TSIDFormat.equals("") && !TSIDFormat.equals(_AliasTSID) && !TSIDFormat.equals(_TSID) ) {
        message = "The TSID format is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the TSID format as " + _AliasTSID + ", or " + _TSID + " (default)." ) );
    }
    if ( (SortOrder != null) && !SortOrder.equals("") && !SortOrder.equals(_Ascending) && !SortOrder.equals(_Descending) ) {
        message = "The sort order is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the sort order as " + _Ascending + " (default), or " + _Descending + "." ) );
    }
    
    //  Check for invalid parameters...
    List<String> validList = new ArrayList<String>(4);
    validList.add ( "TSIDFormat" );
    validList.add ( "Property" );
    validList.add ( "PropertyFormat" );
    validList.add ( "SortOrder" );

    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );    

    if ( warning.length() > 0 ) {
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(command_tag,warning_level),warning );
        throw new InvalidCommandParameterException ( warning );
    }
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new SortTimeSeries_JDialog ( parent, this )).ok();
}

// rely on parent parseCommand()

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws CommandWarningException, CommandException
{	String routine = "SortTimeSeries_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	List tslist = null;
	
    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    
    PropList parameters = getCommandParameters();
    String TSIDFormat = parameters.getValue ( "TSIDFormat" );
    String Property = parameters.getValue ( "Property" );
    String PropertyFormat = parameters.getValue ( "PropertyFormat" );
    String SortOrder = parameters.getValue ( "SortOrder" );
    int sortOrder = 1;
    if ( (SortOrder != null) && SortOrder.equalsIgnoreCase(_Descending) ) {
        sortOrder = -1;
    }
	
	try {
	    Object o = processor.getPropContents ( "TSResultsList" );
		tslist = (List)o;
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		message = "Error requesting TSResultsList from processor.";
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report to software support." ) );
		Message.printWarning(3, routine, message );
	}
	int warning_count = 0;
	if ( (tslist == null) || (tslist.size() == 0) ) {
		// Don't do anything...
		Message.printStatus ( 2, routine,"No time series are available.  Not sorting." );
	}
    else {
        try {
            TSUtil_SortTimeSeries tsu = new TSUtil_SortTimeSeries(tslist, TSIDFormat, Property, PropertyFormat, sortOrder );
            List<TS> tslistSorted = tsu.sortTimeSeries();
            // TODO SAM 2014-07-14 Might be dangerous to do the following so replace the existing list
            //processor.setPropContents ( "TSResultsList", tslistSorted );
            tslist.clear();
            tslist.addAll(tslistSorted);
        }
        catch ( Exception e ) {
            message = "Unexpected error sorting time series (" + e + ").";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),
                routine, message );
            Message.printWarning ( 3, routine, e );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report to software support." ) );
            throw new CommandException ( message );
	    }
    }
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{   if ( props == null ) {
        return getCommandName() + "()";
    }
    String TSIDFormat = props.getValue( "TSIDFormat" );
    String Property = props.getValue( "Property" );
    String PropertyFormat = props.getValue( "PropertyFormat" );
    String SortOrder = props.getValue( "SortOrder" );
    StringBuffer b = new StringBuffer ();
    if ( (TSIDFormat != null) && (TSIDFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSIDFormat=\"" + TSIDFormat + "\"" );
    }
    if ( (Property != null) && (Property.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Property=\"" + Property + "\"" );
    }
    if ( (PropertyFormat != null) && (PropertyFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PropertyFormat=\"" + PropertyFormat + "\"" );
    }
    if ( (SortOrder != null) && (SortOrder.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SortOrder=\"" + SortOrder + "\"" );
    }
    return getCommandName() + "(" + b.toString() + ")";
}

}