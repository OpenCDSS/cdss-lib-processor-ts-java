// NewEndOfMonthTSFromDayTS_Command - This class initializes, checks, and runs the NewEndOfMonthTSFromDayTS() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.DayTS;
import RTi.TS.MonthTS;
import RTi.TS.TS;
import RTi.TS.TSUtil_ChangeInterval;
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

/**
This class initializes, checks, and runs the NewEndOfMonthTSFromDayTS() command.
*/
public class NewEndOfMonthTSFromDayTS_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{
    
/**
List of time series read during discovery.  These are TS objects but with mainly the metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public NewEndOfMonthTSFromDayTS_Command ()
{	super();
	setCommandName ( "NewEndOfMonthTSFromDayTS" );
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
	String DayTSID = parameters.getValue ( "DayTSID" );
    String Bracket = parameters.getValue ( "Bracket" );
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
	if ( (DayTSID == null) || DayTSID.equals("") ) {
        message = "The daily time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a daily time series identifier."));
	}
    if ( (Bracket == null) || Bracket.equals("") ) {
        message = "The bracket must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify the bracket (number of days on each side to search)."));
    }
    else if ( !StringUtil.isInteger(Bracket) ) {
        message = "The bracket is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify the bracket as an integer (number of days on each side to search)."));
    }
    else {
        int bracket = Integer.parseInt(Bracket);
        if ( bracket <= 0 ) {
            message = "The bracket cannot be <= 0.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message,
                    "Specify the bracket as a positive integer (number of days on each side to search)."));
        }
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(3);
    validList.add ( "Alias" );
    validList.add ( "DayTSID" );
    validList.add ( "Bracket" );
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
	return (new NewEndOfMonthTSFromDayTS_JDialog ( parent, this )).ok();
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
Classes that can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
    List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discoveryTSList;
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
	String routine = "NewEndOfMonthTSFromDayTS_Command.parseCommand", message;

    if ( !command.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation
        super.parseCommand(command);
    }
    else {
    	// Get the part of the command after the TS Alias =...
    	int pos = command.indexOf ( "=" );
    	if ( pos < 0 ) {
    		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = NewEndOfMonthTSFromDayTS(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	String token0 = command.substring ( 0, pos ).trim();
    	String token1 = command.substring ( pos + 1 ).trim();
    	if ( (token0 == null) || (token1 == null) ) {
    		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = NewEndOfMonthTSFromDayTS(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	List<String> v = StringUtil.breakStringList ( token0, " ", StringUtil.DELIM_SKIP_BLANKS );
        if ( v == null ) {
            message = "Syntax error in \"" + command +
            "\".  Expecting:  TS Alias = NewEndOfMonthTSFromDayTS(DayTSID,Bracket)";
            Message.printWarning ( warning_level, routine, message);
            throw new InvalidCommandSyntaxException ( message );
        }
        String Alias = (String)v.get(1);
        String DayTSID = null;
        String Bracket = null;
    	if ( (token1.indexOf('=') < 0) && !token1.endsWith("()") ) {
    		// No parameters have = in them...
    		// TODO SAM 2008-09-23 This whole block of code needs to be
    		// removed as soon as commands have been migrated to the new syntax.
    		//
    		// Old syntax without named parameters.
    
    		v = StringUtil.breakStringList ( token1,"(),",
    		        StringUtil.DELIM_SKIP_BLANKS|StringUtil.DELIM_ALLOW_STRINGS );
    		if ( (v == null) || (v.size() != 3) ) {
    			message = "Syntax error in \"" + command +
    			"\".  Expecting:  TS Alias = NewEndOfMonthTSFromDayTS(DayTSID,Bracket)";
    			Message.printWarning ( warning_level, routine, message);
    			throw new InvalidCommandSyntaxException ( message );
    		}
            DayTSID = (String)v.get(1);
            Bracket = (String)v.get(2);
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
        if ( (DayTSID != null) && (DayTSID.length() > 0) ) {
            parameters.set ( "DayTSID", DayTSID );
        }
        if ( (Bracket != null) && (Bracket.length() > 0) ) {
            parameters.set ( "Bracket", Bracket );
        }
        parameters.setHowSet ( Prop.SET_UNKNOWN );
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
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Level for non-user messages

	// Make sure there are time series available to operate on...

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = Boolean.TRUE; // Default.
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
        setDiscoveryTSList ( null );
    }
	
	String Alias = parameters.getValue ( "Alias" ); // Will be expanded below in run mode
	String DayTSID = parameters.getValue ( "DayTSID" );
	if ( (DayTSID != null) && (commandPhase == CommandPhaseType.RUN) && (DayTSID.indexOf("${") >= 0) ) {
		DayTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, DayTSID);
	}
	String Bracket = parameters.getValue ( "Bracket" );
	Integer bracket = null;
	if ( (Bracket != null) && !Bracket.equals("") ) {
	    bracket = Integer.valueOf(Bracket);
	}

	// Get the time series to process.  The time series list is searched
	// backwards until the first match...

	TS dayts = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
    	// TODO SAM 2015-05-27 Not sure this is necessary since discovery TSID can be set from alias
        // Get the discovery time series list from all time series above this command
        String TSList = "" + TSListType.LAST_MATCHING_TSID;
        List<TS> tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, DayTSID, null, null );
        if ( (tslist != null) && (tslist.size() > 0) ) {
            dayts = tslist.get(0);
        }
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        try {
            PropList request_params = new PropList ( "" );
			request_params.set ( "CommandTag", command_tag );
			request_params.set ( "TSID", DayTSID );
			CommandProcessorRequestResultsBean bean = null;
			try {
			    bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + DayTSID + "\") from processor.";
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
				message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + DayTSID + "\") from processor.";
				Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
			}
			else {
				dayts = (TS)o_TS;
			}
    	}
    	catch ( Exception e ) {
    		dayts = null;
    	}
    }
	if ( dayts == null ) {
		message = "Unable to find daily time series using TSID \"" + DayTSID +
			"\".  May be OK if time series is created at run time.";
		if ( commandPhase == CommandPhaseType.DISCOVERY ) {
			// Warning
			if ( DayTSID.indexOf("${") < 0 ) {
				// Only warn if properties are not used
				Message.printWarning ( warning_level,
					MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
			}
			status.addToLog ( commandPhase,
	            new CommandLogRecord(CommandStatusType.WARNING,
	                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
		}
		else if ( commandPhase == CommandPhaseType.RUN ) {
			// Failure
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	        status.addToLog ( commandPhase,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
		}
		throw new CommandWarningException ( message );
	}

	// Now process the time series...

	MonthTS monthts = null;
	boolean createData = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        createData = false;
    }
	try {
	    TSUtil_ChangeInterval tsu = new TSUtil_ChangeInterval();
	    monthts = tsu.newEndOfMonthTSFromDayTS ( (DayTS)dayts, 1, bracket, createData );
        if ( (Alias != null) && !Alias.isEmpty() ) {
            String alias = Alias;
            if ( commandPhase == CommandPhaseType.RUN ) {
            	alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
            		processor, monthts, Alias, status, commandPhase);
            }
            monthts.setAlias ( alias );
        }
	}
	catch ( Exception e ) {
		message = "Unexpected error processing daily time series \"" + dayts.getIdentifier() + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Check the log file - report the problem to software support." ) );
	}
	
    // Further process the time series...
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Just want time series headers initialized
        List<TS> discoveryTSList = new ArrayList<TS>(1);
        discoveryTSList.add ( monthts );
        setDiscoveryTSList ( discoveryTSList );
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        // This makes sure the period is at least as long as the output period...
    	List<TS> tslist = new ArrayList<TS>(1);
    	tslist.add ( monthts );
        int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
        if ( wc > 0 ) {
            message = "Error post-processing series after creation.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,
                ++warning_count), routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            throw new CommandException ( message );
        }
    
        // Update the data to the processor so that appropriate actions are taken...
    
        TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, monthts );
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
private void setDiscoveryTSList ( List<TS> discoveryTSList ) {
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props ) {
    return toString ( props, 10 );
}

/**
Return the string representation of the command.
@param parameters to include in the command
@param majorVersion the major version for software - if less than 10, the "TS Alias = " notation is used,
allowing command files to be saved for older software, no longer enabled.
@return the string representation of the command
*/
public String toString ( PropList parameters, int majorVersion ) {
	String [] parameterOrder = {
		"DayTSID",
		"Alias",
		"Bracket"
	};
	return this.toString(parameters, parameterOrder);
}

}