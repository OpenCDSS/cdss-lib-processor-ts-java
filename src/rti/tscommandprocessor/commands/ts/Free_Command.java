// Free_Command - This class initializes, checks, and runs the Free() command.

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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import RTi.TS.TS;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
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
This class initializes, checks, and runs the Free() command.
*/
public class Free_Command extends AbstractCommand
{
    
/**
 * Values for FreeEnsembleIfEmpty.
 */
protected final String _False = "False";
protected final String _True = "True";

/**
 * Values for IfNotFound.
 */
protected final String _Fail = "Fail";
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";

/**
TSPosition data, zero offset indices
*/
private int [] __TSPositionStart = new int[0];
private int [] __TSPositionEnd = new int[0];

/**
Constructor.
*/
public Free_Command () {
	super();
	setCommandName ( "Free" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	String TSPosition = parameters.getValue ( "TSPosition" );
	String IfNotFound = parameters.getValue("IfNotFound");
    String FreeEnsembleIfEmpty = parameters.getValue ( "FreeEnsembleIfEmpty" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
    if ( (TSPosition != null) && !TSPosition.equals("") ) {
    	List<String> tokens = StringUtil.breakStringList ( TSPosition,",", StringUtil.DELIM_SKIP_BLANKS );
        int npos = 0;
        if ( tokens != null ) {
            npos = tokens.size();
        }
        __TSPositionStart = new int[npos];
        __TSPositionEnd = new int[npos];
        for ( int i = 0; i < npos; i++ ) {
            String token = tokens.get(i);
            if ( token.indexOf("-") >= 0 ) {
                // Range.
                String posString = StringUtil.getToken(token, "-",0,0).trim();
                if ( !StringUtil.isInteger(posString) ) {
                    message = "The TSPosition range (" + token + ") contains an invalid position.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the range using integers with value 1+." ) );
                }
                else {
                    __TSPositionStart[i] = StringUtil.atoi( posString ) - 1;
                }
                posString = StringUtil.getToken(token, "-",0,1).trim();
                if ( !StringUtil.isInteger(posString) ) {
                    message = "The TSPosition range (" + token + ") contains an invalid position.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the range using integer with value 1+." ) );
                }
                else {
                    __TSPositionEnd[i] = StringUtil.atoi( posString ) - 1;
                }
            }
            else {
                // Single value.  Treat as a range of 1.
                if ( !StringUtil.isInteger(token) ) {
                    message = "The TSPosition (" + token + ") is invalid.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the position as an integer 1+." ) );
                }
                __TSPositionStart[i] = StringUtil.atoi(token) - 1;
                __TSPositionEnd[i] = __TSPositionStart[i];
            }
            Message.printStatus ( 1, "", "Range " + i + " from " + token + " is " +
                __TSPositionStart[i] + "," + __TSPositionEnd[i] );
        }
    }
    
	if ( (IfNotFound != null) && !IfNotFound.equals("") &&
        !IfNotFound.equalsIgnoreCase(_Ignore) &&
        !IfNotFound.equalsIgnoreCase(_Fail) &&
        !IfNotFound.equalsIgnoreCase(_Warn) ) {
        message = "Invalid IfNotFound flag \"" + IfNotFound + "\".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the IfNotFound as " + _Ignore + ", " +
                _Warn + ", (default), or " + _Fail + "." ) );
                            
	}
    
    if ( (FreeEnsembleIfEmpty != null) && !FreeEnsembleIfEmpty.equals("") ) {
        if ( !FreeEnsembleIfEmpty.equals(_False) && !FreeEnsembleIfEmpty.equals(_True) ) {
            message = "The FreeEnsembleIfEmpty parameter \"" + FreeEnsembleIfEmpty + "\" must be " +
            _False + " or " + _True + ".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify " + _False + " or " + _True + " (or use blank to use default of " + _True + ")." ) );
        }
    }
	
	// Check for invalid parameters.
    List<String> validList = new ArrayList<>(5);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "TSPosition" );
    validList.add ( "IfNotFound" );
    validList.add ( "FreeEnsembleIfEmpty" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new Free_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
    String routine = "Free_Command.parseCommand", message;
    int warning_level = 2;
    if ( (command_string.indexOf("=") > 0) || command_string.endsWith("()") ) {
        // New syntax, can be blank parameter list for new command.
        super.parseCommand ( command_string );
        // Support legacy (but newer than oldest version below) where only TSID= was specified (no TSList).
        PropList parameters = getCommandParameters();
        String TSList = parameters.getValue ( "TSList" );
        String TSID = parameters.getValue ( "TSID" );
        if ( TSList == null ) {
            if ( TSID != null ) {
                if ( TSID.indexOf("*") >= 0 ) {
                    parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
                }
                else {
                    parameters.set ( "TSList", TSListType.LAST_MATCHING_TSID.toString() );
                }
            }
        }
    }
    else {
        // Parse the old command.
    	List<String> tokens = StringUtil.breakStringList ( command_string,"(,)", StringUtil.DELIM_ALLOW_STRINGS );
        if ( tokens.size() != 2 ) {
            message = "Invalid syntax for command.  Expecting Free(TSID).";
            Message.printWarning ( warning_level, routine, message);
            throw new InvalidCommandSyntaxException ( message );
        }
        String TSID = ((String)tokens.get(1)).trim();
        PropList parameters = new PropList ( getCommandName() );
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        if ( TSID.length() > 0 ) {
            // Legacy behavior was to match last matching TSID if no wildcard.
            if ( TSID.indexOf("*") >= 0 ) {
                parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
            }
            else {
                parameters.set ( "TSList", TSListType.LAST_MATCHING_TSID.toString() );
            }
            parameters.set ( "TSID", TSID );
        }
        parameters.setHowSet ( Prop.SET_UNKNOWN );
        setCommandParameters ( parameters );
    }
}

/**
Remove a time series at the indicated index.
@param FreeEnsembleIfEmpty Indicate whether ensembles should be freed if empty (True or False).
*/
private int removeTimeSeriesAtIndex ( CommandProcessor processor, String TSID,
        Object o_Index, String FreeEnsembleIfEmpty, CommandStatus status,
        int warning_count, int warning_level, String command_tag ) {
    String message;
    String routine = getClass().getName() + ".removeTimeSeriesAtIndex";
    
    // Have the index of the single time series to free.
    // Get the time series so a relevant status message can be printed.
    
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "Index", o_Index );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = processor.processRequest( "GetTimeSeries", request_params );
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeries(Index=\"" + o_Index + "\") from processor.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
        return warning_count;
    }

    // Get the time series out of the results bean.
    
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TS = bean_PropList.getContents ( "TS" );
    if ( o_TS == null ) {
        message = "Unable to find time series \"" + TSID + "\" for Free() command.";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
            message, "Verify that the TSID pattern matches 1+ time series identifiers - may be OK if a partial run." ) );
        //throw new Exception ( message );
        return warning_count;
    }
    TS ts = (TS)o_TS;

    // Now actually remove the time series.
    
    PropList request_params2 = new PropList ( "" );
    request_params2.setUsingObject ( "Index", o_Index );
    // By here should be True or False.
    Boolean FreeEnsembleIfEmpty_Boolean = Boolean.valueOf(FreeEnsembleIfEmpty);
    request_params2.setUsingObject ( "FreeEnsembleIfEmpty", FreeEnsembleIfEmpty_Boolean );
    try {
        processor.processRequest( "RemoveTimeSeriesFromResultsList", request_params2 );
        if ( ts.getAlias().length() > 0 ) {
            // Print alias and identifier.
            Message.printStatus ( 2, routine,
            "Freed time series resources for \"" + ts.getAlias() + "\" \"" +
            ts.getIdentifierString() + "\" at [" + o_Index +"]");
        }
        else {  
            // Print only the identifier.
            Message.printStatus ( 2, routine, "Freed time series resources for \"" +
            ts.getIdentifierString() + "\" at [" + o_Index +"]");
        }
    }
    catch ( Exception e ) {
        message = "Error requesting RemoveTimeSeriesFromResultsList(Index=\"" + o_Index + "\") from processor.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
        return warning_count;
    }
    
    return warning_count;
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;  // Level for non-use messages for log file.

    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
    Boolean clearStatus = Boolean.TRUE; // Default.
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen.
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}
	
	PropList parameters = getCommandParameters();
    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = "" + TSListType.ALL_TS;
    }
    String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String TSPosition = parameters.getValue ( "TSPosition" );
    String IfNotFound = parameters.getValue("IfNotFound");
    if ( (IfNotFound == null) || IfNotFound.equals("")) {
        IfNotFound = _Warn; // Default.
    }
    String FreeEnsembleIfEmpty = parameters.getValue ( "FreeEnsembleIfEmpty" );
    if ( (FreeEnsembleIfEmpty == null) || FreeEnsembleIfEmpty.equals("")) {
        FreeEnsembleIfEmpty = _True;    // Default.
    }
	
    int countRemoved = 0;  // Number of time series removed.
    // Get the original count of time series.
    Object o = null;
    try {
        o = processor.getPropContents ( "TSResultsListSize" );
    }
    catch ( Exception e ) {
        message = "Error requesting TSResultsListSize from processor.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
    }
    if ( o == null ) {
        message = "TSResultsListSize returned as null from processor.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
    }
    
    // Get the time series to process:
    // - allow TSID to be a pattern or specific time series
    // - if time series are not found, handle using the IfNotFound preference
    CommandStatusType notFoundStatus = CommandStatusType.FAILURE;
    if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
    	notFoundStatus = CommandStatusType.WARNING;
    }
    else if ( IfNotFound.equalsIgnoreCase(_Ignore) ) {
    	notFoundStatus = null;
    }

    PropList request_params = new PropList ( "" );
    request_params.set ( "TSList", TSList );
    request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
    request_params.set ( "TSPosition", TSPosition );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
    }
    catch ( Exception e ) {
	        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
	        "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
	        Message.printWarning(log_level,
	            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
	        Message.printWarning(log_level, routine, e );
	        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	            message, "Report the problem to software support." ) );
    }

    PropList bean_PropList = bean.getResultsPropList();
    Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
    List<TS> tslist = null;
    int [] tsposArray = null;
    if ( o_TSList == null ) {
    	if ( notFoundStatus != null ) {
	        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
	        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\" TSPosition=\"" + TSPosition + "\").";
	        Message.printWarning ( log_level,
	            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
	        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(notFoundStatus, message,
	            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    	}
    }
    else {
    	@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
        tslist = tslist0;
        if ( tslist.size() == 0 ) {
        	if ( notFoundStatus != null ) {
	            message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
	            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\" TSPosition=\"" + TSPosition + "\").";
	            Message.printWarning ( log_level,
	                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(notFoundStatus, message,
	                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        	}
        }
        // Also get the array positions.
        tsposArray = (int [])bean_PropList.getContents ( "Indices" );
    }
    
    int nts = tslist.size();
    if ( nts == 0 ) {
    	if ( notFoundStatus != null ) {
	        message = "Unable to find time series to free using TSList=\"" + TSList + "\" TSID=\"" + TSID +
	            "\", EnsembleID=\"" + EnsembleID + "\" TSPosition=\"" + TSPosition + "\".";
	        Message.printWarning ( warning_level,
	            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
	        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(notFoundStatus, message,
	            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    	}
    }

    if ( warning_count > 0 ) {
        // Input error (e.g., missing time series).
        message = "Command parameter data has errors.  Unable to run command.";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        throw new CommandException ( message );
    }

    // Now process the time series.
    // Remove them in the order of maximum index to minimum so that the indices don't change in the list to be removed.
    // Can't count on the TSList results to be in any specific order so search for the maximum index each time.
    // Loop through the number of matched time series.
    for ( int its = 0; its < nts; its++ ) {
        // Loop through the time series position array and find the largest one.
    	// After detecting, set the array content to -1 to know that it has been processed.
        // This is brute force but hopefully pretty fast under normal circumstances.
        int tspos = -1;
        int iposMax = -1;
        for ( int ipos = 0; ipos < nts; ipos++ ) {
            if ( tsposArray[ipos] < 0 ) {
                continue;
            }
            else if ( tsposArray[ipos] > tspos ) {
                tspos = tsposArray[ipos];
                iposMax = ipos;
            }
        }
        if ( tspos == -1 ) {
            // Done processing.
            break;
        }
        // Indicate that a position has been processed (assuming no error below,
        // but need to do this otherwise an infinite loop).
        tsposArray[iposMax] = -1;
        try {
            notifyCommandProgressListeners ( its, nts, (float)-1.0, "Freeing time series [" + tspos + "]" );
            int warning_count2 = removeTimeSeriesAtIndex ( processor, TSID, Integer.valueOf(tspos),
                    FreeEnsembleIfEmpty, status, 0, warning_level, command_tag );
            warning_count += warning_count2;
            if ( warning_count2 == 0 ) {
                // Able to remove so increment the count of removed and decrement other
                // loop variables to process the next time series.
                ++countRemoved;
            }
        }
        catch ( Exception e ) {
            message = "Unexpected error freeing (removing) time series (" + e + ").";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag,++warning_count),routine,message );
            Message.printWarning(3,routine,e);
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
        }
    }
    if ( (nts > 0) && (countRemoved == 0) ) {
        // Maybe an error but could be OK for a partial run.
        message = "No time series were matched for \"" + this + "\"";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
            message, "Verify that the TSID pattern matches 1+ time series identifiers - may be OK if a partial run." ) );
        //throw new Exception ( message );
    }
    else {
        Message.printStatus(2, routine, "Removed (freed) " + countRemoved + " time series.");
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
		"TSList",
		"TSID",
		"EnsembleID",
		"TSPosition",
		"IfNotFound",
		"FreeEnsembleIfEmpty"
	};
	return this.toString(parameters, parameterOrder);
}

}