// TSID_Command - This class initializes, checks, and runs the TSID command.

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

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TimeSeriesNotFoundException;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.IO.PropList;

/**
<p>
This class initializes, checks, and runs the TSID command.
</p>
*/
public class TSID_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{
    
/**
Values for internal IfNotFound parameter.
*/
private final String _Default = "Default";
//protected final String _Ignore = "Ignore";
private final String _Warn = "Warn";
    
/**
List of time series read during discovery.  These are TS objects but with only the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public TSID_Command ()
{	super();
	setCommandName ( "TSID" );
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
{	String TSID = parameters.getValue ( "TSID" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a daily time series identifier."));
	}
    
    // Check for invalid parameters...
	List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "TSID" );
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
	return (new TSID_JDialog ( parent, this )).ok();
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
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	// The entire command string is the time series identifier
	String TSID = command;
	PropList parameters = getCommandParameters();
	// Since the a command name is not used in output, an internal parameter matching the TSID is tracked and used by toString()
	parameters.set ( "TSID", TSID );
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
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number The number of the command being run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	//int log_level = 3;	// Level for non-user messages

	// Make sure there are time series available to operate on...

	PropList parameters = getCommandParameters();
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
    boolean readData = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        readData = false;
        setDiscoveryTSList(null);
    }
	
	String TSID = parameters.getValue ( "TSID" );
	String IfNotFound = _Warn;
	String DefaultUnits = null; // Not used

	// Now process the time series...

	TS ts = null;
	try {
	    boolean notFoundLogged = false;
        // Make a request to the processor to read the time series...
        PropList request_params = new PropList ( "" );
        request_params.set ( "TSID", TSID );
        request_params.setUsingObject ( "WarningLevel", new Integer(warning_level) );
        request_params.set ( "CommandTag", command_tag );
        request_params.set ( "IfNotFound", IfNotFound );
        // Indicates discovery mode...
        request_params.setUsingObject ( "ReadData", new Boolean(readData) );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "ReadTimeSeries", request_params);
            PropList bean_PropList = bean.getResultsPropList();
            Object o_TS = bean_PropList.getContents ( "TS" );
            if ( o_TS != null ) {
                ts = (TS)o_TS;
            }
        }
        catch ( TimeSeriesNotFoundException e ) {
            message = "Time series could not be found using identifier \"" + TSID + "\" (" + e + ").";
            if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the identifier information is correct." ) );
            }
            else {
                // Non-fatal - ignoring or defaulting time series.
                message += "  Non-fatal because IfNotFound=" + IfNotFound;
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Verify that the identifier information is correct." ) );
            }
            ts = null;
            notFoundLogged = true;
        }
        catch ( Exception e ) {
            message = "Error requesting ReadTimeSeries(TSID=\"" + TSID + "\") from processor + (exception: " +
            e + ").";
            //Message.printWarning(3, routine, e );
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message, "Verify that the identifier information is correct.  Check the log file." +
                            "  If still a problem, report the problem to software support." ) );
            ts = null;
        }
        if ( ts == null ) {
            if ( !notFoundLogged ) {
                // Only want to include a warning once.
                // This is kind of ugly because currently there is not consistency between all
                // time series readers in error handling, which is difficult to handle in this
                // generic command.
                message = "Time series could not be found using identifier \"" + TSID + "\".";
                if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify that the identifier information is correct." ) );
                }
                else {
                    // Non-fatal - ignoring or defaulting time series.
                    message += "  Non-fatal because IfNotFound=" + IfNotFound;
                    status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.WARNING,
                                    message, "Verify that the identifier information is correct." ) );
                }
            }
            // Always check for output period because required for default time series.
            if ( IfNotFound.equalsIgnoreCase(_Default) &&
                    ((processor.getPropContents("OutputStart") == null) ||
                    (processor.getPropContents("OutputEnd") == null)) ) {
                message = "Time series could not be found using identifier \"" + TSID + "\"." +
                        "  Requesting default time series but no output period is defined.";
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Set the output period before calling this command." ) );
            }
        }
        else if ( readData && (ts.getDate1() == null) && (ts.getDate2() == null) ) {
            // Have time series but no data records
            if ( !notFoundLogged ) {
                // Only want to include a warning once.
                // This is kind of ugly because currently there is not consistency between all
                // time series readers in error handling, which is difficult to handle in this
                // generic command.
                message = "Time series \"" + TSID + "\" has no data.";
                if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify that the identifier information is correct and that data exist." ) );
                }
                else {
                    // Non-fatal - ignoring or defaulting time series.
                    message += "  Non-fatal because IfNotFound=" + IfNotFound;
                    status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.WARNING,
                                    message, "Verify that the identifier information is correct and that data exist." ) );
                }
            }
            // Always check for output period because required for default time series.
            if ( IfNotFound.equalsIgnoreCase(_Default) &&
                    ((processor.getPropContents("OutputStart") == null) ||
                    (processor.getPropContents("OutputEnd") == null)) ) {
                message = "Time series could not be found using identifier \"" + TSID + "\"." +
                        "  Requesting default time series but no output period is defined.";
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Set the output period before calling this command." ) );
            }
        }
        else {
            if ( (DefaultUnits != null) && (ts.getDataUnits().length() == 0) ) {
                // Time series has no units so assign default.
                ts.setDataUnits ( DefaultUnits );
            }
        }
	}
	catch ( Exception e ) {
		message = "Unexpected error reading time series \"" + TSID + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the log file - report the problem to software support." ) );
	}
	
	List<TS> tslist = new Vector<TS>(1);
    if ( ts != null ) {
        tslist.add ( ts );
    }
    if ( commandPhase == CommandPhaseType.RUN ) {
        // Now add the list in the processor...
        
        int wc3 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
        if ( wc3 > 0 ) {
            message = "Error adding time series after read.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,
                ++warning_count), routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
            throw new CommandException ( message );
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        //Message.printStatus(2,routine,"TS in discovery mode is " + ts );
        if ( ts == null ) {
            try {
                // Create a time series and set the identifier - other metadata will not be set
                TS tsd = new TS ();
                tsd.setIdentifier(TSID);
                tslist.add ( tsd );
            }
            catch ( Exception e ) {
                message = "Error adding time series for discovery " +
                	"(time series identifier will not be visible to other commands during discovery/editing).";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Confirm that the TSID is a valid format." ) );
                Message.printWarning(3,routine,e);   
            }
        }
        //else {
        //    Message.printStatus(2,routine,"TSID in discovery mode is " + ts.getIdentifierString() );
        //}
        setDiscoveryTSList ( tslist );
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
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return "";
	}
	String TSID = props.getValue( "TSID" );
	return TSID;
}

}
