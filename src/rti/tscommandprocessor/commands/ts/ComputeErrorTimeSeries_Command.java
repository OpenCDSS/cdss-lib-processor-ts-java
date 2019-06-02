// ComputeErrorTimeSeries_Command - This class initializes, checks, and runs the ComputeErrorTimeSeries() command.

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
import RTi.TS.TSUtil_ComputeErrorTimeSeries;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.AbstractCommand;
//import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
//import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;

/**
This class initializes, checks, and runs the ComputeErrorTimeSeries() command.
*/
public class ComputeErrorTimeSeries_Command extends AbstractCommand
implements Command
// FIXME SAM 2008-02-04 Evaluate whether this is possible given that "real" time series
// would need to be found as input, CommandDiscoverable, ObjectListProvider
{
    
/**
Values of the ErrorMeasure parameter.
*/
protected final String _PercentError = "PercentError";

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_List = null;

/**
Constructor.
*/
public ComputeErrorTimeSeries_Command ()
{	super();
	setCommandName ( "ComputeErrorTimeSeries" );
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
{	String ObservedTSList = parameters.getValue ( "ObservedTSList" );
	String ObservedTSID = parameters.getValue ( "ObservedTSID" );
	//String SetStart = parameters.getValue ( "SetStart" );
	//String SetEnd = parameters.getValue ( "SetEnd" );
	String ErrorMeasure = parameters.getValue ( "ErrorMeasure" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
	if ( (ObservedTSList != null) && !TSListType.ALL_MATCHING_TSID.equals(ObservedTSList) ) {
		if ( ObservedTSID != null ) {
            message = "TSID should only be specified when TSList=" + TSListType.ALL_MATCHING_TSID.toString() + ".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Do not specify the TSID parameter when TList=" + TSListType.ALL_MATCHING_TSID.toString() ) );
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
    /*
    if ( alias.equalsIgnoreCase(independent) ) {
        warning += "\nThe time series to fill \"" + alias +
            "\" is the same.\n"+
            "as the independent time series \"" +
            independent + "\".\n" + "Correct or Cancel.";
    }
    */
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
    if ( (ErrorMeasure != null) && !ErrorMeasure.equals("") &&
            !ErrorMeasure.equalsIgnoreCase(_PercentError) ) {
        message = "The ErrorMeasure parameter (" + ErrorMeasure + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify ErrorMeasure as " + _PercentError ) );
    }
    
	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>();
    validList.add ( "ObservedTSList" );
    validList.add ( "ObservedTSID" );
    validList.add ( "ObservedEnsembleID" );
    validList.add ( "SimulatedTSList" );
    validList.add ( "SimulatedTSID" );
    validList.add ( "SimulatedEnsembleID" );
    //valid_Vector.add ( "SetStart" );
    //valid_Vector.add ( "SetEnd" );
    validList.add ( "ErrorMeasure" );
    validList.add ( "Alias" );
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
	return (new ComputeErrorTimeSeries_JDialog ( parent, this )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_List;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
    List<TS> discovery_TS_List = getDiscoveryTSList ();
    if ( (discovery_TS_List == null) || (discovery_TS_List.size() == 0) ) {
        return null;
    }
    TS datats = discovery_TS_List.get(0);
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discovery_TS_List;
    }
    else {
        return null;
    }
}

/**
Get the time series to process.
@param its Position in time series array to get time series.
@param tspos Positions in time series processor time series array.
*/
private TS getTimeSeriesToProcess ( int its, int[] tspos, String command_tag, int warning_count )
{   String routine = "ComputeErrorTimeSeries_Command.getTimeSeriesToProcess";
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

// Use base class parseCommand()

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
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ComputeErrorTimeSeries_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);

	String ObservedTSList = parameters.getValue ( "ObservedTSList" );
    if ( (ObservedTSList == null) || ObservedTSList.equals("") ) {
        ObservedTSList = TSListType.ALL_TS.toString();
    }
	String ObservedTSID = parameters.getValue ( "ObservedTSID" );
    String ObservedEnsembleID = parameters.getValue ( "ObservedEnsembleID" );
    String Alias = parameters.getValue ( "Alias" );

	// Get the time series to process...
	
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", ObservedTSList );
	request_params.set ( "TSID", ObservedTSID );
    request_params.set ( "EnsembleID", ObservedEnsembleID );
	CommandProcessorRequestResultsBean bean = null;
	try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + ObservedTSList +
        "\", TSID=\"" + ObservedTSID + "\", EnsembleID=\"" + ObservedEnsembleID + "\") from processor.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	List<TS> tslist = null;
	if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + ObservedTSList +
        "\" TSID=\"" + ObservedTSID + "\", EnsembleID=\"" + ObservedEnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the ObservedTSList parameter matches one or more time series - may be OK for partial run." ) );
	}
	else {
        @SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
        tslist = tslist0;
		if ( tslist.size() == 0 ) {
            message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + ObservedTSList +
            "\" TSID=\"" + ObservedTSID + "\", EnsembleID=\"" + ObservedEnsembleID + "\").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
					command_tag,++warning_count), routine, message );
            status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message,
                    "Verify that the ObservedTSList parameter matches one or more time series - may be OK for partial run." ) );
		}
	}
	Object o_Indices = bean_PropList.getContents ( "Indices" );
	int [] tspos = null;
	if ( o_Indices == null ) {
        message = "Unable to find indices for time series to process using TSList=\"" + ObservedTSList +
        "\" TSID=\"" + ObservedTSID + "\", EnsembleID=\"" + ObservedEnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
	}
	else {
        tspos = (int [])o_Indices;
		if ( tspos.length == 0 ) {
            message = "Unable to find indices for time series to process using TSList=\"" + ObservedTSList +
            "\" TSID=\"" + ObservedTSID + "\", EnsembleID=\"" + ObservedEnsembleID + "\".";
			Message.printWarning ( warning_level,
			    MessageUtil.formatMessageTag(
			        command_tag,++warning_count), routine, message );
            status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
		}
	}
	
	int nts = 0;
	if ( tslist != null ) {
		nts = tslist.size();
	}
	if ( nts == 0 ) {
        message = "Unable to find any time series to process using TSList=\"" + ObservedTSList +
        "\" TSID=\"" + ObservedTSID + "\", EnsembleID=\"" + ObservedEnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
            new CommandLogRecord(CommandStatusType.WARNING,
                message,
                "Verify that the ObservedTSList parameter matches one or more time series - may be OK for partial run." ) );
	}
	// FIXME SAM 2008-02-04 Evaluate how to handle more than one observed time series.
    if ( nts != 1 ) {
        message = "Currently only allow one observed time series, but have " + nts + " using TSList=\"" + ObservedTSList +
        "\" TSID=\"" + ObservedTSID + "\", EnsembleID=\"" + ObservedEnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
            new CommandLogRecord(CommandStatusType.WARNING,
                message,
                "Verify that the ObservedTSList parameter matches one time series only." ) );
        throw new InvalidCommandParameterException ( message );
    }

	// Simulated time series...
    
    String SimulatedTSList = parameters.getValue ( "SimulatedTSList" );
    if ( (SimulatedTSList == null) || SimulatedTSList.equals("") ) {
        SimulatedTSList = TSListType.ALL_TS.toString();
    }
    String SimulatedTSID = parameters.getValue ( "SimulatedTSID" );
    String SimulatedEnsembleID = parameters.getValue ( "SimulatedEnsembleID" );
    request_params = new PropList ( "" );
    request_params.set ( "TSList", SimulatedTSList );
    request_params.set ( "TSID", SimulatedTSID );
    request_params.set ( "EnsembleID", SimulatedEnsembleID );
    try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + SimulatedTSList +
        "\", TSID=\"" + SimulatedTSID + "\", EnsembleID=\"" + SimulatedEnsembleID + "\") from processor.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    Object o_TSList2 = bean_PropList.getContents ( "TSToProcessList" );
    List<TS> independent_tslist = null;
    if ( o_TSList2 == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + SimulatedTSList +
        "\" TSID=\"" + SimulatedTSID + "\", EnsembleID=\"" + SimulatedEnsembleID + "\").";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the SimulatedTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    else {
    	@SuppressWarnings("unchecked")
		List<TS> independent_tslist0 = (List<TS>)o_TSList2;
        independent_tslist = independent_tslist0;
        if ( independent_tslist.size() == 0 ) {
            message = "No simulated time series are available from processor GetTimeSeriesToProcess (TSList=\"" + SimulatedTSList +
            "\" TSID=\"" + SimulatedTSID + "\", EnsembleID=\"" + SimulatedEnsembleID + "\").";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
            status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message,
                    "Verify that the SimulatedTSList parameter matches one or more time series - may be OK for partial run." ) );
        }
    }
    Object o_Indices2 = bean_PropList.getContents ( "Indices" );
    int [] simulated_tspos = null;
    if ( o_Indices2 == null ) {
        message = "Unable to find indices for simulated time series to process using TSList=\"" + SimulatedTSList +
        "\" TSID=\"" + SimulatedTSID + "\", EnsembleID=\"" + SimulatedEnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
    }
    else {
        simulated_tspos = (int [])o_Indices2;
        if ( simulated_tspos.length == 0 ) {
            message = "Unable to find indices for simulated time series to process using TSList=\"" + SimulatedTSList +
            "\" TSID=\"" + SimulatedTSID + "\", EnsembleID=\"" + SimulatedEnsembleID + "\".";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
            status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
        }
    }
    
    int n_simulated_ts = 0;
    if ( independent_tslist != null ) {
        n_simulated_ts = independent_tslist.size();
    }
    if ( n_simulated_ts == 0 ) {
        message = "Unable to find any simulated time series to process using TSList=\"" + SimulatedTSList +
        "\" TSID=\"" + SimulatedTSID + "\", EnsembleID=\"" + SimulatedEnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
            new CommandLogRecord(CommandStatusType.WARNING,
                message,
                "Verify that the SimulatedTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    
    // Make sure that the number of dependent and independent time series is consistent
    
    /* TODO SAM Evaluate how to handle multiple observed - for now check is not needed.
    if ( (n_simulated_ts > 1) && (n_simulated_ts != nts) ) {
        message = "The number of simulated time series (" + n_simulated_ts +
            ") is > 1 but does not agree with the number of observed time series (" + nts + ").";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the SimulatedTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    */

    /*
	// Set period...

	String SetStart = parameters.getValue("SetStart");
	String SetEnd = parameters.getValue("SetEnd");

	// Figure out the dates to use for the analysis...
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
			message = "Error requesting SetStart DateTime(DateTime=" +	SetStart + ") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
		Message.printWarning(warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time or OutputEnd." ) );
		throw new InvalidCommandParameterException ( message );
	}
	*/

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Insufficient data to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		throw new CommandException ( message );
	}

	// Now process the time series...

    String ErrorMeasure = parameters.getValue("ErrorMeasure");
	PropList setprops = new PropList ( "ErrorMeasure" );
	if ( (ErrorMeasure != null) && !ErrorMeasure.equals("") ) {
		setprops.set ( "ErrorMeasure", ErrorMeasure );
	}

	TS ts = null;
    TS simulated_ts = null;
	for ( int its = 0; its < n_simulated_ts; its++ ) {
	    // For now always get the same observed...
		//ts = getTimeSeriesToProcess ( its, tspos, command_tag, warning_count );
		ts = getTimeSeriesToProcess ( 0, tspos, command_tag, warning_count );
		if ( ts == null ) {
			// Skip time series.
            message = "Unable to get time series at position " + tspos[its] + " - null time series.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
			continue;
		}
		
		// Do the setting...
        if ( n_simulated_ts == 1 ) {
            // Reuse the same independent time series for all transfers...
            simulated_ts = getTimeSeriesToProcess ( 0, simulated_tspos, command_tag, warning_count );
        }
        else {
            // Get the time series matching the loop index...
            simulated_ts = getTimeSeriesToProcess ( its, simulated_tspos, command_tag, warning_count );
        }
        
        if ( simulated_ts == null ) {
            // Skip time series.
            message = "Unable to get simulated time series at position " + tspos[its] + " - null time series.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
            continue;
        }
        
		Message.printStatus ( 2, routine, "Computing error between simulated time series \"" +
		        simulated_ts.getIdentifier()+ "\" and observed time series \"" +
                ts.getIdentifier() + "\"." );
		try {
		    TSUtil_ComputeErrorTimeSeries tsu = new TSUtil_ComputeErrorTimeSeries();
            TS error_ts = tsu.computeErrorTimeSeries ( ts, simulated_ts, ErrorMeasure );
            
            if ( (Alias != null) && (Alias.length() > 0) ) {
                // Set the alias to the desired string.
                error_ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                         processor, error_ts, Alias, status, command_phase) );
            }
            
            if ( command_phase == CommandPhaseType.RUN ) {
                if ( tslist != null ) {
                    // Further process the time series...
                    // This makes sure the period is at least as long as the output period...
                    int wc = TSCommandProcessorUtil.processTimeSeriesAfterRead( processor, this, error_ts );
                    if ( wc > 0 ) {
                        message = "Error post-processing series after read.";
                        Message.printWarning ( warning_level, 
                            MessageUtil.formatMessageTag(command_tag,
                            ++warning_count), routine, message );
                            status.addToLog ( command_phase,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                            message, "Report the problem to software support." ) );
                        throw new CommandException ( message );
                    }
            
                    // Now add the list in the processor...
                    
                    int wc2 = TSCommandProcessorUtil.appendTimeSeriesToResultsList ( processor, this, error_ts );
                    if ( wc2 > 0 ) {
                        message = "Error adding time series after read.";
                        Message.printWarning ( warning_level, 
                            MessageUtil.formatMessageTag(command_tag,
                            ++warning_count), routine, message );
                            status.addToLog ( command_phase,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                            message, "Report the problem to software support." ) );
                        throw new CommandException ( message );
                    }
                }
            }
            else if ( command_phase == CommandPhaseType.DISCOVERY ) {
                setDiscoveryTSList ( tslist );
            }
		}
		catch ( Exception e ) {
			message = "Unexpected error setting time series \"" + ts.getIdentifier() + "\" from \"" +
                simulated_ts.getIdentifier() + "\" (" + e + ").";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
			Message.printWarning(3,routine,e);
            status.addToLog ( command_phase,
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
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discovery_TS_List )
{
    __discovery_TS_List = discovery_TS_List;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String ObservedTSList = props.getValue( "ObservedTSList" );
    String ObservedTSID = props.getValue( "ObservedTSID" );
    String ObservedEnsembleID = props.getValue( "ObservedEnsembleID" );
    String SimulatedTSList = props.getValue( "SimulatedTSList" );
    String SimulatedTSID = props.getValue( "SimulatedTSID" );
    String SimulatedEnsembleID = props.getValue( "SimulatedEnsembleID" );
	//String SetStart = props.getValue("SetStart");
	//String SetEnd = props.getValue("SetEnd");
    String ErrorMeasure = props.getValue( "ErrorMeasure" );
    String Alias = props.getValue( "Alias" );
	StringBuffer b = new StringBuffer ();
    if ( (ObservedTSList != null) && (ObservedTSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ObservedTSList=" + ObservedTSList );
    }
    if ( (ObservedTSID != null) && (ObservedTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ObservedTSID=\"" + ObservedTSID + "\"" );
    }
    if ( (ObservedEnsembleID != null) && (ObservedEnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ObservedEnsembleID=\"" + ObservedEnsembleID + "\"" );
    }
    if ( (SimulatedTSList != null) && (SimulatedTSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SimulatedTSList=" + SimulatedTSList );
    }
    if ( (SimulatedTSID != null) && (SimulatedTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SimulatedTSID=\"" + SimulatedTSID + "\"" );
    }
    if ( (SimulatedEnsembleID != null) && (SimulatedEnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SimulatedEnsembleID=\"" + SimulatedEnsembleID + "\"" );
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
	*/
    if ( (ErrorMeasure != null) && (ErrorMeasure.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ErrorMeasure=" + ErrorMeasure );
    }
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }

	return getCommandName() + "(" + b.toString() + ")";
}

}
