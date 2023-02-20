// SortTimeSeries_Command - This class initializes, checks, and runs the SortTimeSeries() command.

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
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	List<TS> tslist = null;
	
    CommandProcessor processor = getCommandProcessor();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
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
	    @SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o;
		tslist = tslist0;
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
            TSUtil_SortTimeSeries<TS> tsu = new TSUtil_SortTimeSeries(tslist, TSIDFormat, Property, PropertyFormat, sortOrder );
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
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TSIDFormat",
    	"Property",
    	"PropertyFormat",
    	"SortOrder"
	};
	return this.toString(parameters, parameterOrder);
}

}