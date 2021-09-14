// ReadTimeSeries_Command - This class initializes, checks, and runs the ReadTimeSeries() command.

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

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
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
This class initializes, checks, and runs the ReadTimeSeries() command.
*/
public class ReadTimeSeries_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{
	
/**
Values for ReadData parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Values for IfNotFound parameter.
*/
protected final String _Default = "Default";
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public ReadTimeSeries_Command ()
{	super();
	setCommandName ( "ReadTimeSeries" );
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
	String TSID = parameters.getValue ( "TSID" );
	String ReadData = parameters.getValue("ReadData");
	String IfNotFound = parameters.getValue("IfNotFound");
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
	if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a daily time series identifier."));
	}
    if ( (ReadData != null) && !ReadData.equals("") &&
        !ReadData.equalsIgnoreCase(_False) &&
        !ReadData.equalsIgnoreCase(_True) ) {
        message = "Invalid ReadData flag \"" + ReadData + "\".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the ReadData as " + _False + " or " +
                _True + " (default)." ) );
                            
    }
    if ( (IfNotFound != null) && !IfNotFound.equals("") &&
            !IfNotFound.equalsIgnoreCase(_Ignore) &&
            !IfNotFound.equalsIgnoreCase(_Default) &&
            !IfNotFound.equalsIgnoreCase(_Warn) ) {
            message = "Invalid IfNotFound flag \"" + IfNotFound + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the IfNotFound as " + _Default + ", " +
                            _Ignore + ", or (default) " + _Warn + "." ) );
                            
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(5);
    validList.add ( "Alias" );
    validList.add ( "TSID" );
    validList.add ( "ReadData" );
    validList.add ( "IfNotFound" );
    validList.add ( "DefaultUnits" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new ReadTimeSeries_JDialog ( parent, this )).ok();
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
The following classes can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discovery_TS_Vector;
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
	String routine = "ReadTimeSeries_Command.parseCommand", message;

    if ( !command.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation
        super.parseCommand(command);
    }
    else {
    	// Get the part of the command after the TS Alias =...
    	int pos = command.indexOf ( "=" );
    	if ( pos < 0 ) {
    		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = ReadTimeSeries(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	String token0 = command.substring ( 0, pos ).trim();
    	String token1 = command.substring ( pos + 1 ).trim();
    	if ( (token0 == null) || (token1 == null) ) {
    		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = ReadTimeSeries(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	List<String> v = StringUtil.breakStringList ( token0, " ", StringUtil.DELIM_SKIP_BLANKS );
        if ( v == null ) {
            message = "Syntax error in \"" + command +
            "\".  Expecting:  TS Alias = ReadTimeSeries(TSID)";
            Message.printWarning ( warning_level, routine, message);
            throw new InvalidCommandSyntaxException ( message );
        }
        String Alias = (String)v.get(1);
        String TSID = null;
        String IfNotFound = null;
    	if ( (token1.indexOf('=') < 0) && !token1.endsWith("()") ) {
    		// No parameters have = in them...
    		// TODO SAM 2008-09-23 This whole block of code needs to be
    		// removed as soon as commands have been migrated to the new syntax.
    		//
    		// Old syntax without named parameters.
    
    		v = StringUtil.breakStringList ( token1,"(),",StringUtil.DELIM_SKIP_BLANKS|StringUtil.DELIM_ALLOW_STRINGS );
    		if ( (v == null) || (v.size() != 2) ) {
    			message = "Syntax error in \"" + command +
    			"\".  Expecting:  TS Alias = ReadTimeSeries(TSID)";
    			Message.printWarning ( warning_level, routine, message);
    			throw new InvalidCommandSyntaxException ( message );
    		}
            TSID = v.get(1);
            IfNotFound = _Warn; // Default required parameter
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
        if ( (TSID != null) && (TSID.length() > 0) ) {
            parameters.set ( "TSID", TSID );
        }
        if ( (IfNotFound != null) && (IfNotFound.length() > 0) ) {
            parameters.set ( "IfNotFound", IfNotFound );
        }
        parameters.setHowSet ( Prop.SET_UNKNOWN );
    }
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
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
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }
	
	String Alias = parameters.getValue ( "Alias" );
	String TSID = parameters.getValue ( "TSID" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String ReadData = parameters.getValue("ReadData");
    boolean readData = true; // Default for run mode
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
    	readData = false; // Default - always
    }
    else {
	    if ( (ReadData != null) && !ReadData.equals("") && ReadData.equalsIgnoreCase(_False) ) {
            readData = false; // OK to ignore reading data in run mode
        }
    }
    String IfNotFound = parameters.getValue("IfNotFound");
    if ( (IfNotFound == null) || IfNotFound.equals("")) {
        IfNotFound = _Warn; // default
    }
    String DefaultUnits = parameters.getValue("DefaultUnits");

	// Now process the time series...

	TS ts = null;
	try {
	    boolean notFoundLogged = false;
        if ( TSID.indexOf("${") < 0 ) {
	        // Have a valid TSID - make a request to the processor...
	        PropList request_params = new PropList ( "" );
	        request_params.set ( "TSID", TSID );
	        request_params.setUsingObject ( "WarningLevel", new Integer(warning_level) );
	        request_params.set ( "CommandTag", command_tag );
	        request_params.set ( "IfNotFound", IfNotFound );
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
	            message = "Time series could not be read using identifier \"" + TSID + "\".";
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
	            message = "Error reading time series using identifier \"" + TSID + "\" (" + e + ").";
	            if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
	                status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Verify that the identifier information is correct." ) );
	                Message.printWarning ( 3, routine, e );
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
	        if ( ts == null ) {
	            if ( !notFoundLogged ) {
	                // Only want to include a warning once.
	                // This is kind of ugly because currently there is not consistency between all
	                // time series readers in error handling, which is difficult to handle in this
	                // generic command.
	                message = "Time series could not be found using identifier \"" + TSID + "\".";
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
	        if ( (ts != null) && (Alias != null) && !Alias.equals("") ) {
	            String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
	                processor, ts, Alias, status, commandPhase);
	            ts.setAlias ( alias );
	        }
        }
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	// TSID contains a property so create a time series with that identifier for other commands to use as choice
        	ts = new TS();
        	ts.setIdentifier(TSID);
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
	
	List<TS> tslist = new ArrayList<TS>(1);
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
{   if ( props == null ) {
        if ( majorVersion < 10 ) {
            return "TS Alias = " + getCommandName() + "()";
        }
        else {
            return getCommandName() + "()";
        }
    }
	String Alias = props.getValue( "Alias" );
	String TSID = props.getValue( "TSID" );
    String ReadData = props.getValue ( "ReadData" );
    String IfNotFound = props.getValue ( "IfNotFound" );
    String DefaultUnits = props.getValue ( "DefaultUnits" );
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
    if ( majorVersion >= 10 ) {
        // Add as a parameter
        if ( (Alias != null) && (Alias.length() > 0) ) {
            if ( b.length() > 0 ) {
                b.append ( "," );
            }
            b.append ( "Alias=\"" + Alias + "\"" );
        }
    }
    if ((ReadData != null) && (ReadData.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("ReadData=" + ReadData );
    }
    if ((IfNotFound != null) && (IfNotFound.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("IfNotFound=" + IfNotFound );
    }
    if ((DefaultUnits != null) && (DefaultUnits.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DefaultUnits=\"" + DefaultUnits + "\"");
    }
    if ( majorVersion < 10 ) {
        // Old syntax...
        if ( (Alias == null) || Alias.equals("") ) {
            Alias = "Alias";
        }
        return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
    }
    else {
        return getCommandName() + "("+ b.toString()+")";
    }
}

}
