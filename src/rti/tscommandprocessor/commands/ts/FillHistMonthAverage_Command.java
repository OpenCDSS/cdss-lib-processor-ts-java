// FillHistMonthAverage_Command - This class initializes, checks, and runs the FillHistMonthAverage() command.

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

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.MonthTSLimits;
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
This class initializes, checks, and runs the FillHistMonthAverage() command.
*/
public class FillHistMonthAverage_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public FillHistMonthAverage_Command ()
{	super();
	setCommandName ( "FillHistMonthAverage" );
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
{	//String TSList = parameters.getValue ( "TSList" );
	//String TSID = parameters.getValue ( "TSID" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	//String FillFlag = parameters.getValue ( "FillFlag" );
	//String FillFlagDesc = parameters.getValue ( "FillFlagDesc" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
	if ( (FillStart != null) && !FillStart.equals("") && !FillStart.equalsIgnoreCase("OutputStart") && !FillStart.startsWith("${")){
		try {	DateTime.parse(FillStart);
		}
		catch ( Exception e ) {
            message = "The fill start date/time \"" + FillStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	if ( (FillEnd != null) && !FillEnd.equals("") && !FillEnd.equalsIgnoreCase("OutputEnd") && !FillEnd.startsWith("${")) {
		try {	DateTime.parse( FillEnd);
		}
		catch ( Exception e ) {
            message = "The fill end date/time \"" + FillEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
		}
	}
    
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(6);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "FillStart" );
    validList.add ( "FillEnd" );
    validList.add ( "FillFlag" );
    validList.add ( "FillFlagDesc" );
    
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
	return (new FillHistMonthAverage_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "fillHistMonthAverage_Command.parseCommand", message;

    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
		// TODO SAM 2005-04-29 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax where the only parameter is a single TSID or * to fill all.
    	List<String> v = StringUtil.breakStringList(command_string,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS |
			StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		if ( ntokens != 2 ) {
			message = "Syntax error in \"" + command_string + "\".  Two tokens expected.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}

		// Get the individual tokens of the expression...

		String TSID = ((String)v.get(1)).trim();

		// Set parameters and new defaults...

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID.length() > 0 ) {
			parameters.set ( "TSID", TSID );
			parameters.setHowSet(Prop.SET_AS_RUNTIME_DEFAULT);
			parameters.set ( "TSList", "" + TSListType.ALL_MATCHING_TSID );
		}
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
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
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Warning level for non-user messages.

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    Boolean clearStatus = Boolean.TRUE; // Default.
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

	String TSList = parameters.getValue ( "TSList" );
	if ( (TSList == null) || TSList.equals("") ) {
	    TSList = "" + TSListType.ALL_TS;
	}
	String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}

	// Get the time series to process...
	
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try {
	    bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\") from processor.";
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
		message = "Unable to find time series to fill using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	else {
		@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
        tslist = tslist0;
		if ( tslist.size() == 0 ) {
			message = "Unable to find time series to fill using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
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
		message = "Unable to find indices for time series to fill using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
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
			message = "Unable to find indices for time series to fill using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
			Message.printWarning ( warning_level,
					MessageUtil.formatMessageTag(
							command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message,
                            "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
		}
	}
	
	int nts = 0;
	if ( tslist != null ) {
			nts = tslist.size();
	}
	if ( nts == 0 ) {
		message = "Unable to find time series to fill using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                        message,
                        "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}

	// Fill period...

	String FillStart = parameters.getValue("FillStart");
	String FillEnd = parameters.getValue("FillEnd");
	String FillFlag = parameters.getValue("FillFlag");
	if ( (FillFlag != null) && (FillFlag.indexOf("${") >= 0) ) {
		FillFlag = TSCommandProcessorUtil.expandParameterValue(processor, this, FillFlag);
	}
	String FillFlagDesc = parameters.getValue("FillFlagDesc");
	if ( (FillFlagDesc != null) && (FillFlagDesc.indexOf("${") >= 0) ) {
		FillFlagDesc = TSCommandProcessorUtil.expandParameterValue(processor, this, FillFlagDesc);
	}

	// Figure out the dates to use for the analysis...
	DateTime FillStart_DateTime = null;
	DateTime FillEnd_DateTime = null;

	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			FillStart_DateTime = TSCommandProcessorUtil.getDateTime ( FillStart, "FillStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			FillEnd_DateTime = TSCommandProcessorUtil.getDateTime ( FillEnd, "FillEnd", processor,
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
	String nl = System.getProperty ( "line.separator" );
	for ( int its = 0; its < nts; its++ ) {
		ts = null;
		request_params = new PropList ( "" );
		request_params.setUsingObject ( "Index", Integer.valueOf(tspos[its]) );
		bean = null;
		try { bean =
			processor.processRequest( "GetTimeSeries", request_params);
		}
		catch ( Exception e ) {
            message = "Error requesting GetTimeSeries(Index=" + tspos[its] + "\" from processor.";
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
			Message.printWarning(warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
				continue;
		}
		else {	ts = (TS)prop_contents;
		}

		notifyCommandProgressListeners ( its, nts, (float)-1.0, "Filling time series " +
            ts.getIdentifier().toStringAliasAndTSID() );
		if ( ts.getDataIntervalBase() != TimeInterval.MONTH) {
			message = "Filling with historic month average is only implemented for monthly time series.  Skipping:\n" +
			ts.getIdentifier();
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify that the TSList parameter specifies only monthly time series." ) );
		}
		// Do the filling...
		Message.printStatus ( 2, routine, "Filling \"" + ts.getIdentifier()+"\" with monthly average." );
		MonthTSLimits average_limits = null;
		try {
            average_limits = (MonthTSLimits)ts.getDataLimitsOriginal();
			// For log file...
			if ( average_limits == null ) {
                message = "Historical averages for time series " + ts.getIdentifierString() + " are not available (no data).";
				Message.printStatus ( 2, routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Verify that time series has data." ) );
			}
			else {
                Message.printStatus ( 2, routine, "Historical averages for time series follow..."+ nl + average_limits );
				// Now fill the time series...
				TSUtil.fillConstantByMonth ( ts,
						FillStart_DateTime, FillEnd_DateTime,
						average_limits.getMeanArray(),
						", fill w/ hist mon avg", FillFlag, FillFlagDesc );
			}
			// TODO need a PropList to pass the fill flag.
		}
		catch ( Exception e ) {
			if ( average_limits == null ) {
				message = "Unable to fill time series \""+ ts.getIdentifierString() +
					"\" with historical averages (no data limits are available - no data?).";
                Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag(
                        command_tag, ++warning_count),
                        routine,message);
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Verify that time series has data." ) );
			}
			else {
                // Not sure why there is an error so print the trace...
				message = "Unexpected error filling time series \""+
					ts.getIdentifierString() + "\" with historical averages (" + e + ").";
                Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag(
                        command_tag, ++warning_count),
                        routine,message);
				Message.printWarning(3,routine,e);
                status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Report the problem to software support - see the log file for details." ) );
			}
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
    
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
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
		"FillStart",
		"FillEnd",
		"FillFlag",
		"FillFlagDesc"
	};
	return this.toString(parameters, parameterOrder);
}

}