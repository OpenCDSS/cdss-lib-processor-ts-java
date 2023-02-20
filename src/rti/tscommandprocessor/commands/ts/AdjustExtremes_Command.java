// AdjustExtremes_Command - This class initializes, checks, and runs the AdjustExtremes() command.

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
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

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
This class initializes, checks, and runs the AdjustExtremes() command.
*/
public class AdjustExtremes_Command extends AbstractCommand implements Command
{
    
/**
Possible values for AdjustMethod.
*/
protected final String _Average = "Average";

/**
Possible values for ExtremeToAdjust.
*/
protected final String _AdjustMinimum = "AdjustMinimum";
protected final String _AdjustMaximum = "AdjustMaximum";

/**
Constructor.
*/
public AdjustExtremes_Command ()
{	super();
	setCommandName ( "AdjustExtremes" );
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
{	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
	String AdjustMethod = parameters.getValue ( "AdjustMethod" );
	String ExtremeToAdjust = parameters.getValue ( "ExtremeToAdjust" );
	String ExtremeValue = parameters.getValue ( "ExtremeValue" );
    if ( ExtremeValue == null ) {
        ExtremeValue = ""; // To simplify checks below
    }
    String MaxIntervals = parameters.getValue ( "MaxIntervals" );
    if ( MaxIntervals == null ) {
        MaxIntervals = ""; // To simplify checks below
    }
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    if ( (TSList != null) && !TSListType.ALL_MATCHING_TSID.equals(TSList) &&
            !TSListType.FIRST_MATCHING_TSID.equals(TSList) &&
            !TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        if ( TSID != null ) {
            message = "TSID should only be specified when TSList=" +
            TSListType.ALL_MATCHING_TSID.toString() + " or " +
            TSListType.FIRST_MATCHING_TSID.toString() + " or " +
            TSListType.LAST_MATCHING_TSID.toString() + ".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Do not specify the TSID parameter." ) );
        }
    }
    /*
	if ( TSList == null ) {
		// Probably legacy command...
		// TODO SAM 2005-05-17 Need to require TSList when legacy
		// commands are safely nonexistent...  At that point the
		// following check can occur in any case.
		if ( (TSID == null) || (TSID.length() == 0) ) {
            message = "A TSID must be specified.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a TSList parameter value." ) );
		}
	}
    */
    if ( (AdjustMethod != null) && !AdjustMethod.equals("") && !AdjustMethod.equals(_Average) ) {
        message = "The adjust method is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the adjust method as blank or " + _Average + "." ) );
    }
    if ( (ExtremeToAdjust != null) && !ExtremeToAdjust.equals("") && !ExtremeToAdjust.equals(_AdjustMaximum) &&
            !ExtremeToAdjust.equals(_AdjustMinimum) ) {
        message = "The extreme to adjust is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the extreme to adjust as blank, " + _AdjustMinimum + ", or " +
                _AdjustMaximum + "." ) );
    }
	if ( ExtremeValue.equals("") ) {
        message = "The extreme value is not specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the extreme value as a number." ) );
	}
	else if ( !StringUtil.isDouble(ExtremeValue) ) {
        message = "The extreme value " + ExtremeValue + " is not a number.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the extreme value as a number." ) );
	}
	
    if ( !MaxIntervals.equals("") && !StringUtil.isInteger(MaxIntervals) ) {
        message = "The maximum intervals " + MaxIntervals + " is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the maximum intervals as an integer." ) );
    }

	if ( (AnalysisStart != null) && !AnalysisStart.equals("") && !AnalysisStart.equalsIgnoreCase("OutputStart") && !AnalysisStart.startsWith("${") ){
		try {	DateTime.parse(AnalysisStart);
		}
		catch ( Exception e ) {
            message = "The analysis start date/time \"" + AnalysisStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time." ) );
		}
	}
	if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") && !AnalysisEnd.startsWith("${") ) {
		try {	DateTime.parse( AnalysisEnd);
		}
		catch ( Exception e ) {
            message = "The analysis end date/time \"" + AnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time." ) );
		}
	}
    
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(11);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "AdjustMethod" );
    validList.add ( "ExtremeToAdjust" );
    validList.add ( "ExtremeValue" );
    validList.add ( "MaxIntervals" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
    validList.add ( "SetFlag" );
    validList.add ( "SetFlagDesc" );
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
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new AdjustExtremes_JDialog ( parent, this )).ok();
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
	String routine = "AdjustExtremes_Command.parseCommand", message;

	if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
		// TODO SAM 2005-09-08 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax where the only parameter is a single TSID or * to fill all.
    	List<String> v = StringUtil.breakStringList(command_string,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS | StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		if ( ntokens != 8 ) {
			// Command name, TSID, and constant...
			message = "Syntax error in \"" + command_string +
			"\".  Expecting AdjustExtremes(TSID,AdjustMethod," +
			"ExtremeToAdjust,ExtremeValue,MaxIntervals,AnalysisStart,AnalysisEnd).";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}

		// Get the individual tokens of the expression...

		String TSID = ((String)v.get(1)).trim();
		String AdjustMethod = ((String)v.get(2)).trim();
		String ExtremeToAdjust = ((String)v.get(3)).trim();
		String ExtremeValue = ((String)v.get(4)).trim();
		String MaxIntervals = ((String)v.get(5)).trim();
		String AnalysisStart = ((String)v.get(6)).trim();
		String AnalysisEnd = ((String)v.get(7)).trim();

		// Set parameters and new defaults...

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID.length() > 0 ) {
			parameters.set ( "TSID", TSID );
			parameters.setHowSet(Prop.SET_AS_RUNTIME_DEFAULT);
            // Legacy behavior was to match last matching TSID if no wildcard
            if ( TSID.indexOf("*") >= 0 ) {
                parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
            }
            else {
                parameters.set ( "TSList", TSListType.LAST_MATCHING_TSID.toString() );
            }
		}
		if ( !AdjustMethod.equals("") ) {
		    parameters.set ( "AdjustMethod", AdjustMethod );
		}
	    if ( !ExtremeToAdjust.equals("") ) {
            parameters.set ( "ExtremeToAdjust", ExtremeToAdjust );
        }
	    if ( !ExtremeValue.equals("") ) {
            parameters.set ( "ExtremeValue", ExtremeValue );
        }
	    if ( !MaxIntervals.equals("") ) {
            parameters.set ( "MaxIntervals", MaxIntervals );
        }
	    if ( !AnalysisStart.equals("") ) {
	        if ( AnalysisStart.equals("*") ) {
	            AnalysisStart = "";
	        }
            parameters.set ( "AnalysisStart", AnalysisStart );
        }
	    if ( !AnalysisEnd.equals("") ) {
            if ( AnalysisEnd.equals("*") ) {
                AnalysisEnd = "";
            }
            parameters.set ( "AnalysisEnd", AnalysisEnd );
        }
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
}

/**
Run the command.
@param command_number number of command to run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could  not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Warning message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
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

	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}

	// Get the time series to process...
	
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
	CommandProcessorRequestResultsBean bean = null;
	try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	List<TS> tslist = null;
	if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}
	else {
		@SuppressWarnings("unchecked")
		List<TS> dataList = (List<TS>)o_TSList;
		tslist = dataList;
		if ( tslist.size() == 0 ) {
            message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
					command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message,
                    "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
		}
	}
	Object o_Indices = bean_PropList.getContents ( "Indices" );
	int [] tspos = null;
	if ( o_Indices == null ) {
        message = "Unable to find indices for time series to process using TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
	}
	else {
        tspos = (int [])o_Indices;
		if ( tspos.length == 0 ) {
            message = "Unable to find indices for time series to process using TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
			Message.printWarning ( warning_level,
			    MessageUtil.formatMessageTag(
			        command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
		}
	}
	
	int nts = 0;
	if ( tslist != null ) {
		nts = tslist.size();
	}
	if ( nts == 0 ) {
        message = "Unable to find any time series to process using TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.WARNING,
                message,
                "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}

	// Extreme value...

	String AdjustMethod = parameters.getValue("AdjustMethod");
	if ( (AdjustMethod == null) || AdjustMethod.equals("") ) {
	    AdjustMethod = _Average; // default
	}
	String ExtremeToAdjust = parameters.getValue("ExtremeToAdjust");
    if ( (ExtremeToAdjust == null) || ExtremeToAdjust.equals("") ) {
        ExtremeToAdjust = _AdjustMaximum; // default
    }
	String ExtremeValue = parameters.getValue("ExtremeValue");
	double ExtremeValue_double = StringUtil.atod ( ExtremeValue );
	
	String MaxIntervals = parameters.getValue("MaxIntervals");
	int MaxIntervals_int = 0; // Default
	if ( (MaxIntervals != null) && !MaxIntervals.equals("") ) {
	    MaxIntervals_int = StringUtil.atoi ( MaxIntervals );
	}

	// Analysis period...

	String AnalysisStart = parameters.getValue("AnalysisStart");
	String AnalysisEnd = parameters.getValue("AnalysisEnd");
	String SetFlag = parameters.getValue("SetFlag");
	if ( (SetFlag != null) && (SetFlag.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		SetFlag = TSCommandProcessorUtil.expandParameterValue(processor, this, SetFlag);
	}
	String SetFlagDesc = parameters.getValue("SetFlagDesc");
	if ( (SetFlagDesc != null) && (SetFlagDesc.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		SetFlagDesc = TSCommandProcessorUtil.expandParameterValue(processor, this, SetFlagDesc);
	}

	// Figure out the dates to use for the analysis...
	DateTime AnalysisStart_DateTime = null;
	DateTime AnalysisEnd_DateTime = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			AnalysisStart_DateTime = TSCommandProcessorUtil.getDateTime ( AnalysisStart, "AnalysisStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			AnalysisEnd_DateTime = TSCommandProcessorUtil.getDateTime ( AnalysisEnd, "AnalysisEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
    }

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Insufficient data to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}

	// Now process the time series...

	TS ts = null;
	for ( int its = 0; its < nts; its++ ) {
		ts = null;
		request_params = new PropList ( "" );
		request_params.setUsingObject ( "Index", new Integer(tspos[its]) );
		bean = null;
		try { bean =
			processor.processRequest( "GetTimeSeries", request_params);
		}
		catch ( Exception e ) {
            message = "Error requesting GetTimeSeries(Index=" + tspos[its] + "\") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
		}
		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "TS" );
		if ( prop_contents == null ) {
            message = "Null value for GetTimeSeries(Index=" + tspos[its] + "\") returned from processor.";
			Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
		}
		else {	ts = (TS)prop_contents;
		}
		
		if ( ts == null ) {
			// Skip time series.
            message = "Unable to set time series at position " + tspos[its] + " - null time series.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
			continue;
		}
		
		// Do the setting...
		Message.printStatus ( 2, routine, "Adjusting \"" + ts.getIdentifier()+ "\" extreme " + ExtremeValue + "." );
		try {
		    notifyCommandProgressListeners ( its, nts, (float)-1.0, "Adjusting extremes for " +
		        ts.getIdentifier().toStringAliasAndTSID() );
		    TSUtil.adjustExtremes ( ts, AdjustMethod, ExtremeToAdjust, ExtremeValue_double,
	            MaxIntervals_int, AnalysisStart_DateTime, AnalysisEnd_DateTime, SetFlag, SetFlagDesc );
		}
		catch ( Exception e ) {
			message = "Unexpected error adjusting extremes for time series \"" + ts.getIdentifier() + "\" (" + e + ").";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
			Message.printWarning(3,routine,e);
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
		}
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
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TSList",
    	"TSID",
    	"EnsembleID",
		"AdjustMethod",
    	"ExtremeToAdjust",
    	"ExtremeValue",
    	"MaxIntervals",
		"AnalysisStart",
		"AnalysisEnd",
		"SetFlag",
		"SetFlagDesc"
	};
	return this.toString(parameters, parameterOrder);
}

}