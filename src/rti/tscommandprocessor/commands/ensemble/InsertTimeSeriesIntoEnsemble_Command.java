// InsertTimeSeriesIntoEnsemble_Command - This class initializes, checks, and runs the InsertTimeSeriesIntoEnsemble() command.

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

package rti.tscommandprocessor.commands.ensemble;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.TSEnsemble;
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
import RTi.Util.IO.PropList;

/**
This class initializes, checks, and runs the InsertTimeSeriesIntoEnsemble() command.
*/
public class InsertTimeSeriesIntoEnsemble_Command extends AbstractCommand implements Command
{
    
/**
Values for the CopyTimeSeries parameter.
*/
//protected final String _False = "False";
//protected final String _True = "True";

/**
Constructor.
*/
public InsertTimeSeriesIntoEnsemble_Command ()
{	super();
	setCommandName ( "InsertTimeSeriesIntoEnsemble" );
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
{	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
    //String InputStart = parameters.getValue ( "InputStart" );
    //String InputEnd = parameters.getValue ( "InputEnd" );
    String EnsembleID2 = parameters.getValue ( "EnsembleID2" );
    //String CopyTimeSeries = parameters.getValue ( "CopyTimeSeries" );
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
    
    /* TODO SAM 2008-01-04 Evaluate need
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
	if ( (EnsembleID2 == null) || EnsembleID2.equals("") ) {
        message = "The receiving ensemble identifier (EnsembleID2) is required but has not been specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the EnsembleID2." ) );
	}
	/*
	if ( (InputStart != null) && !InputStart.equals("") ){
		try {
            DateTime.parse(InputStart);
		}
		catch ( Exception e ) {
            message = "The input start date/time \"" + InputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time." ) );
		}
	}
	if ( (InputEnd != null) && !InputEnd.equals("") ) {
		try {
		    DateTime.parse( InputEnd);
		}
		catch ( Exception e ) {
            message = "The input end date/time \"" + InputEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time." ) );
		}
	}
	if ( (InputStart != null) && !InputStart.equals("") ) {
	    message = "The input start parameter is currently disabled due to technical issues.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Change the period before using the command or contact software support if this feature is needed." ) );
	}
    if ( (InputEnd != null) && !InputEnd.equals("") ) {
        message = "The input end parameter is currently disabled due to technical issues.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Change the period before using the command orontact software support if this feature is needed." ) );
    }
	
    if ( (CopyTimeSeries != null) && !CopyTimeSeries.equals("") &&
        !CopyTimeSeries.equalsIgnoreCase(_True) &&
        !CopyTimeSeries.equalsIgnoreCase(_False) ) {
        message = "The CopyTimeSeries parameter (" + CopyTimeSeries + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify as " + _True + " or " + _False + " (default)." ) );
    }
    
    if ( (((InputStart != null) && !InputStart.equals("")) ||
        ((InputEnd != null) && !InputEnd.equals(""))) &&
        (CopyTimeSeries != null) && !CopyTimeSeries.equals("") ) {
        message = "The CopyTimeSeries parameter must be true if the input period is set.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify CopyTimeSeries as " + _True + " or don't set the input period." ) );
    }
    
    if ( (CopyTimeSeries != null) && !CopyTimeSeries.equals("") ) {
        message = "The ability to copy time series into the ensemble is currently disabled due to technical issues.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Change the period before using the command or contact software support if this feature is needed." ) );
    }
    */
    
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(4);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "EnsembleID2" );
    //valid_Vector.add ( "InputStart" );
    //valid_Vector.add ( "InputEnd" );
    //valid_Vector.add ( "CopyTimeSeries" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new InsertTimeSeriesIntoEnsemble_JDialog ( parent, this )).ok();
}

// parseCommand() inherited from base class

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = "InsertTimeSeriesIntoEnsemble_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Warning message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    
    List<TS> tslist = null;
    String EnsembleID2 = parameters.getValue("EnsembleID2");

    // Get all the necessary data
	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );

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
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}
	else {
		@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
        tslist = tslist0;
		if ( tslist.size() == 0 ) {
			message = "Unable to find time series to process using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
			Message.printWarning ( warning_level,
			    MessageUtil.formatMessageTag(
			        command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
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
        message = "Unable to find indices for time series to process using TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.WARNING,
                message,
                "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}

    // Get the time series ensemble to process.

    request_params = new PropList ( "" );
    request_params.set ( "CommandTag", command_tag );
    request_params.set ( "EnsembleID", EnsembleID2 );
    try {
        bean = processor.processRequest( "GetEnsemble", request_params );
    }
    catch ( Exception e ) {
        message = "Error requesting GetEnsemble(EnsembleID=\"" + EnsembleID2 + "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    Object o_TSEnsemble = bean_PropList.getContents ( "TSEnsemble");
    TSEnsemble tsensemble = null;
    if ( o_TSEnsemble == null ) {
        message = "Null ensemble requesting GetEnsemble(EnsembleID=\"" + EnsembleID + "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
    }
    else {
        tsensemble = (TSEnsemble)o_TSEnsemble;
    }
    
    if ( tsensemble == null ) {
        message = "Unable to find ensemble to process using EnsembleID \"" + EnsembleID2 + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
    }
        
    if ( warning_count > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog ( commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

	// Now process the time series...
    
    TS ts = null;
    String tsidMessage = "";
    try {
        for ( int i = 0; i < nts; i++ ) {
            ts = tslist.get(i);
            notifyCommandProgressListeners ( i, nts, (float)-1.0, "Inserting time series " +
                ts.getIdentifier().toStringAliasAndTSID() );
            tsidMessage = " \"" + ts.getIdentifierString() + "\" "; // in case exception caught
            // Get list of ensemble time series as new list, to check against this time series
            List<TS>checkList = tsensemble.getTimeSeriesList(true);
            checkList.add ( ts );
            if ( !TSUtil.areUnitsCompatible(checkList) ) {
                message = "Time series units " + ts.getDataUnits() + " are not the same as ensemble time series.";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the time series units are the same as the ensemble time series." ) );
            }
            if ( !TSUtil.areIntervalsSame(checkList) ) {
                message = "Time series data interval " + ts.getIdentifier().getInterval() +
                    " is not the same as ensemble time series.";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the time series data interval is the same as the ensemble time series." ) );
            }
            tsensemble.add ( ts );
        }
	}
	catch ( Exception e ) {
		message = "Unexpected error adding time series" + tsidMessage + "to ensemble \"" +
		    EnsembleID2 + "\" (" + e + ").";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		Message.printWarning(3,routine,e);
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "See the log file for details - report the problem to software support." ) );
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
    	"EnsembleID",
		"EnsembleID2"
		//("InputStart",
		//("InputEnd",
		//("CopyTimeSeries"
	};
	return this.toString(parameters, parameterOrder);
}

}