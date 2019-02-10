// SetToMin_Command - This class initializes, checks, and runs the SetToMin() command.

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
//import RTi.Util.Time.DateTime;

// TODO SAM 2008-09-23 Leave in some code in case ensembles and other parameters are
// enabled.  For now match the legacy features.
/**
This class initializes, checks, and runs the SetToMin() command.
*/
public class SetToMin_Command extends AbstractCommand implements Command
{
    
/**
Constructor.
*/
public SetToMin_Command ()
{	super();
	setCommandName ( "SetToMin" );
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
	//String SetStart = parameters.getValue ( "SetStart" );
	//String SetEnd = parameters.getValue ( "SetEnd" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series identifier for to modify is not specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series identifier for the time series to modify."));
    }
    
    /*
 	if ( (SetStart != null) && !SetStart.equals("") && !SetStart.equalsIgnoreCase("OutputStart")){
		try {	DateTime.parse(SetStart);
		}
		catch ( Exception e ) {
            message = "The set start date/time \"" + SetStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	if ( (SetEnd != null) && !SetEnd.equals("") && !SetEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse( SetEnd);
		}
		catch ( Exception e ) {
            message = "The set end date/time \"" + SetStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	*/
    
	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(4);
    validList.add ( "TSID" );
    //valid_Vector.add ( "EnsembleID" );
    validList.add ( "IndependentTSList" );
    validList.add ( "IndependentTSID" );
    validList.add ( "IndependentEnsembleID" );
    //valid_Vector.add ( "SetStart" );
    //valid_Vector.add ( "SetEnd" );
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
	return (new SetToMin_JDialog ( parent, this )).ok();
}

/**
Get the time series to process.
@param its Position in time series array to get time series.
@param tspos Positions in time series processor time series array.
*/
private TS getTimeSeriesToProcess ( int its, int[] tspos, String command_tag, int warning_count )
{   String routine = "SetToMin_Command.getTimeSeriesToProcess";
    TS ts = null;
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "Index", new Integer(tspos[its]) );
    CommandProcessorRequestResultsBean bean = null;
    CommandProcessor processor = getCommandProcessor();
    String message;
    CommandStatus status = getCommandStatus();
    //int warning_level = 2;
    int log_level = 3;
    try {
        bean = processor.processRequest( "GetTimeSeries", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeries(Index=" + tspos[its] + "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
        return null;
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object prop_contents = bean_PropList.getContents ( "TS" );
    if ( prop_contents == null ) {
        message = "Null value for GetTimeSeries(Index=" + tspos[its] + "\") returned from processor.";
        Message.printWarning(log_level,
        MessageUtil.formatMessageTag( command_tag, ++warning_count),
            routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
        return null;
    }
    else {
        ts = (TS)prop_contents;
    }
    return ts;
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
	String routine = "SetToMin_Command.parseCommand", message;

	if ( ((command_string.indexOf('=') > 0) || command_string.endsWith("()")) ) {
	    // Current syntax...
        super.parseCommand( command_string);
    }
    else {
		// TODO SAM 2008-09-23 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax
    	List<String> v = StringUtil.breakStringList(command_string,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS |	StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		if ( ntokens < 3 ) {
			message = "Syntax error in \"" + command_string +
			"\".  Expecting SetToMax(TSID,IndependentTSID1,IndependentTSID2,...).";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}

		// Get the individual tokens of the expression...

		String TSID = ((String)v.get(1)).trim();
        StringBuffer IndependentTSID = new StringBuffer();
		//String SetStart = ((String)v.elementAt(3)).trim();
        //String SetEnd = ((String)v.elementAt(4)).trim();

		// Set parameters and new defaults...

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID.length() > 0 ) {
			parameters.set ( "TSID", TSID );
			parameters.setHowSet(Prop.SET_AS_RUNTIME_DEFAULT);
		}
		for ( int i = 2; i < v.size(); i++ ) { 
		    if ( i != 2 ) {
		        IndependentTSID.append( "," );
		    }
		    IndependentTSID.append( ((String)v.get(i)).trim() );
        }
        parameters.set ( "IndependentTSID", IndependentTSID.toString() );
        parameters.setHowSet(Prop.SET_AS_RUNTIME_DEFAULT);
        if ( IndependentTSID.indexOf("*") >= 0 ) {
            parameters.set ( "IndependentTSList", TSListType.ALL_MATCHING_TSID.toString() );
        }
        else {
            parameters.set ( "IndependentTSList", TSListType.LAST_MATCHING_TSID.toString() );
        }
        /*
        if ( !SetStart.equals("*")) {
            parameters.set ( "SetStart", SetStart );
        }
        if ( !SetEnd.equals("*")) {
            parameters.set ( "SetEnd", SetEnd );
        }
        */
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
}

/**
Calls TSCommandProcessor to re-calculate limits for this time series.
@param ts Time Series.
@param TSCmdProc CommandProcessor that is using this command.
@param warningLevel Warning level used for displaying warnings.
@param warning_count Number of warnings found.
@param command_tag Reference or identifier for this command.
*/
/*
private int recalculateLimits( TS ts, CommandProcessor TSCmdProc, 
        int warningLevel, int warning_count, String command_tag )
{
    String routine = "SetToMin_Command.recalculateLimits", message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "TS", ts );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = TSCmdProc.processRequest( "CalculateTSAverageLimits", request_params);
    }
    catch ( Exception e ) {
        message = "Error recalculating original data limits for \"" + ts.getIdentifierString() + "\" (" + e + ")";
        Message.printWarning(warningLevel,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message  );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
        return warning_count;
    }
    // Get the calculated limits and set in the original data limits...
    PropList bean_PropList = bean.getResultsPropList();
    Object prop_contents = bean_PropList.getContents ( "TSLimits" );
    if ( prop_contents == null ) {
        message = "Null value from CalculateTSAverageLimits(" + ts.getIdentifierString() + ")";
        Message.printWarning(warningLevel,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
            routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
        return warning_count;
    }
    // Now set the limits.
    ts.setDataLimitsOriginal ( (TSLimits)prop_contents );
    return warning_count;
}
*/

/**
Run the command.
@param command_number number of command to run.
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
{	String routine = "SetToMin_Command.runCommand", message;
	int warning_count = 0;
	int warningLevel = 2;
	String command_tag = "" + command_number;
	//int log_level = 3;	// Warning message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);

	String TSID = parameters.getValue ( "TSID" );
    //String EnsembleID = parameters.getValue ( "EnsembleID" );
	String EnsembleID = null;
	/*
    String RecalcLimits = parameters.getValue ( "RecalcLimits" );
    boolean RecalcLimits_boolean = false;   // Default
    if ( (RecalcLimits != null) && RecalcLimits.equalsIgnoreCase("true") ) {
        RecalcLimits_boolean = true;
    }
    String HandleMissingHow = parameters.getValue ( "HandleMissingHow" );
    if ( (HandleMissingHow == null) || HandleMissingHow.equals("") ) {
        HandleMissingHow = _SetMissing; // Default
    }
    */

	// Get the time series to process...
	
	PropList request_params = new PropList ( "" );
	String TSList = TSListType.ALL_MATCHING_TSID.toString();
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
		Message.printWarning(warningLevel,
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
		Message.printWarning ( warningLevel,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}
	else {
		@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
        tslist = tslist0;
		if ( tslist.size() == 0 ) {
            message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
			Message.printWarning ( warningLevel,
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
		Message.printWarning ( warningLevel,
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
			Message.printWarning ( warningLevel,
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
		Message.printWarning ( warningLevel,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.WARNING,
                message,
                "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}

	// Independent time series...
    
    String IndependentTSList = parameters.getValue ( "IndependentTSList" );
    String IndependentTSID = parameters.getValue ( "IndependentTSID" );
    String IndependentEnsembleID = parameters.getValue ( "IndependentEnsembleID" );
    request_params = new PropList ( "" );
    request_params.set ( "TSList", IndependentTSList );
    request_params.set ( "TSID", IndependentTSID );
    request_params.set ( "EnsembleID", IndependentEnsembleID );
    try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + IndependentTSList +
        "\", TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\") from processor.";
        Message.printWarning(warningLevel,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    Object o_TSList2 = bean_PropList.getContents ( "TSToProcessList" );
    List<TS> independent_tslist = null;
    if ( o_TSList2 == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + IndependentTSList +
        "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\").";
        Message.printWarning ( warningLevel,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the IndependentTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    else {
    	@SuppressWarnings("unchecked")
		List<TS> independent_tslist0 = (List<TS>)o_TSList2;
        independent_tslist = independent_tslist0;
        if ( independent_tslist.size() == 0 ) {
            message = "No independent time series are available from processor GetTimeSeriesToProcess (TSList=\"" + IndependentTSList +
            "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\").";
            Message.printWarning ( warningLevel,
                MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message,
                    "Verify that the IndependentTSList parameter matches one or more time series - may be OK for partial run." ) );
        }
    }
    Object o_Indices2 = bean_PropList.getContents ( "Indices" );
    int [] independent_tspos = null;
    if ( o_Indices2 == null ) {
        message = "Unable to find indices for independent time series to process using TSList=\"" + IndependentTSList +
        "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\".";
        Message.printWarning ( warningLevel,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
    }
    else {
        independent_tspos = (int [])o_Indices2;
        if ( independent_tspos.length == 0 ) {
            message = "Unable to find indices for independent time series to process using TSList=\"" + IndependentTSList +
            "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\".";
            Message.printWarning ( warningLevel,
                MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
        }
    }
    
    int n_independent_ts = 0;
    if ( independent_tslist != null ) {
        n_independent_ts = independent_tslist.size();
    }
    if ( n_independent_ts == 0 ) {
        message = "Unable to find any independent time series to process using TSList=\"" + IndependentTSList +
        "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\".";
        Message.printWarning ( warningLevel,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.WARNING,
                message,
                "Verify that the IndependentTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    
	// Set period...

    /*
	String SetStart = parameters.getValue("SetStart");
	String SetEnd = parameters.getValue("SetEnd");

	// Figure out the dates to use for the analysis (this only provides values if they ware set)...
	DateTime SetStart_DateTime = null;
	DateTime SetEnd_DateTime = null;

	try {
	if ( SetStart != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", SetStart );
		bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting SetStart DateTime(DateTime=" + SetStart + ") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for SetStart DateTime(DateTime=" + SetStart + "\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	SetStart_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "SetStart \"" + SetStart + "\" is invalid.";
		Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time or OutputStart." ) );
		throw new InvalidCommandParameterException ( message );
	}
	
	try {
	if ( SetEnd != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", SetEnd );
		bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting SetEnd DateTime(DateTime=" + SetEnd + "\") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for SetEnd DateTime(DateTime=" + SetEnd +	"\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	SetEnd_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "SetEnd \"" + SetEnd + "\" is invalid.";
		Message.printWarning(warningLevel,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time or OutputEnd." ) );
		throw new InvalidCommandParameterException ( message );
	}
	*/

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Insufficient data to run command.";
		Message.printWarning ( warningLevel,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}

	// Now process the time series...

	/*
    PropList setprops = new PropList ( "SetToMin" );
	if ( (TransferHow != null) && !TransferHow.equals("") ) {
		setprops.set ( "TransferHow", TransferHow );
	}
    setprops.set ( "HandleMissingHow", HandleMissingHow );
    */

	// Leave as a loop for now but only one time series should be processed (might need the loop
	// if ensembles are allowed).
	TS ts = null;
	for ( int its = 0; its < nts; its++ ) {
		ts = getTimeSeriesToProcess ( its, tspos, command_tag, warning_count );
		if ( ts == null ) {
			// Skip time series.
            message = "Unable to get time series at position " + tspos[its] + " - null time series.";
			Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
			continue;
		}
     
		Message.printStatus ( 2, routine, "Setting \"" + ts.getIdentifier()+ "\" to minimum." );
		try {
            TSUtil.min ( ts, independent_tslist );
		}
		catch ( Exception e ) {
			message = "Unexpected error setting time series \"" + ts.getIdentifier() +
			"\" to minimum (" + e + ").";
            Message.printWarning ( warningLevel,
                MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
			Message.printWarning(3,routine,e);
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
		}
		/*
        if ( RecalcLimits_boolean ) {
            try {
                recalculateLimits( ts, processor, warningLevel, warning_count, command_tag );
            }
            catch ( Exception e ) {
                message = "Unexpected error recalculating limits for time series \"" +
                ts.getIdentifier() + "\" (" + e + ").";
                Message.printWarning ( warningLevel,
                    MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
                Message.printWarning(3,routine,e);
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "See the log file for details - report the problem to software support." ) );
            }
        }
        */
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warningLevel,
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
    String TSID = props.getValue( "TSID" );
    //String EnsembleID = props.getValue( "EnsembleID" );
    String IndependentTSList = props.getValue( "IndependentTSList" );
    String IndependentTSID = props.getValue( "IndependentTSID" );
    String IndependentEnsembleID = props.getValue( "IndependentEnsembleID" );
	//String SetStart = props.getValue("SetStart");
	//String SetEnd = props.getValue("SetEnd");
    //String HandleMissingHow = props.getValue( "HandleMissingHow" );
	//String FillFlag = props.getValue("FillFlag");
    //String RecalcLimits = props.getValue( "RecalcLimits" );
	StringBuffer b = new StringBuffer ();
    if ( (TSID != null) && (TSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSID=\"" + TSID + "\"" );
    }
    /*
    if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
    }
    */
    if ( (IndependentTSList != null) && (IndependentTSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IndependentTSList=" + IndependentTSList );
    }
    if ( (IndependentTSID != null) && (IndependentTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IndependentTSID=\"" + IndependentTSID + "\"" );
    }
    if ( (IndependentEnsembleID != null) && (IndependentEnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IndependentEnsembleID=\"" + IndependentEnsembleID + "\"" );
    }
    /*
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
    if ( (HandleMissingHow != null) && (HandleMissingHow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "HandleMissingHow=\"" + HandleMissingHow + "\"" );
    }
    if ( ( RecalcLimits != null) && (RecalcLimits.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "RecalcLimits=" + RecalcLimits );
    }
    */

	return getCommandName() + "(" + b.toString() + ")";
}

}
