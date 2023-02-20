// ChangeTimeZone_Command - This class initializes, checks, and runs the ChangeTimeZone() command.

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
import RTi.TS.TSData;
import RTi.TS.TSIterator;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
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
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the ChangeTimeZone() command.
*/
public class ChangeTimeZone_Command extends AbstractCommand
{

/**
Constructor.
*/
public ChangeTimeZone_Command ()
{	super();
	setCommandName ( "ChangeTimeZone" );
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
	//String NewTimeZone = parameters.getValue ( "NewTimeZone" );
	String warning = "";
    //String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    // TODO sam 2017-04-11 evaluate whether to check time zone.  Empty is OK because will set time zone to empty.
    /*
    if ( (NewTimeZone == null) || NewTimeZone.isEmpty() ) {
        message = "NewTimeZone must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify NewTimeZone." ) );
    }*/
    
	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(4);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "NewTimeZone" );
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
	return (new ChangeTimeZone_JDialog ( parent, this )).ok();
}

// Use parseCommand() from base class

/**
Run the command.
@param command_number number of command to run.
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
	if ( (TSList == null) || TSList.isEmpty() ) {
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
	List<TS> tslist = null;
	int [] tspos = null;
	if ( bean != null ) {
		PropList bean_PropList = bean.getResultsPropList();
		Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
		if ( o_TSList == null ) {
		    message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
	        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
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
		if ( o_Indices == null ) {
			message = "Unable to find indices for time series to fill using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
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
				message = "Unable to find indices for time series to fill using TSList=\"" + TSList +
				"\" TSID=\"" + TSID + "\".";
				Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(
								command_tag,++warning_count), routine, message );
	            status.addToLog ( CommandPhaseType.RUN,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Report the problem to software support." ) );
			}
		}
	}
	
	int nts = 0;
	if ( tslist != null ) {
		nts = tslist.size();
	}
	if ( nts == 0 ) {
	      message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
          "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                        message,
                        "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}

	// New time zone...

	String NewTimeZone = parameters.getValue("NewTimeZone");
	if ( NewTimeZone == null ) {
		NewTimeZone = "";
	}
	if ( (NewTimeZone != null) && (NewTimeZone.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		NewTimeZone = TSCommandProcessorUtil.expandParameterValue(processor, this, NewTimeZone);
	}
	
	// Compare the time zone against valid time zones

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Insufficient data to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}

	// Now process the time series...

	TS ts = null;
	try {
		for ( int its = 0; its < nts; its++ ) {
			ts = null;
			request_params = new PropList ( "" );
			request_params.setUsingObject ( "Index", new Integer(tspos[its]) );
			bean = null;
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
			}
			else {
			    ts = (TS)prop_contents;
			}
			
			if ( ts == null ) {
				// Skip time series.
	            message = "Unable to process time series at position " + tspos[its];
				Message.printWarning(warning_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
							routine, message );
	            status.addToLog ( CommandPhaseType.RUN,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Report the problem to software support." ) );
				continue;
			}
			
		    notifyCommandProgressListeners ( its, nts, (float)-1.0, "Changing time zone to \"" + NewTimeZone + " for " +
		        ts.getIdentifier().toStringAliasAndTSID() );
		    
		    // Reset all date/times to not have time zone
		    DateTime dt = ts.getDate1();
		    dt.setTimeZone(NewTimeZone);
		    ts.setDate1(dt);
		    dt = ts.getDate1Original();
		    dt.setTimeZone(NewTimeZone);
		    ts.setDate1Original(dt);
		    dt = ts.getDate2();
		    dt.setTimeZone(NewTimeZone);
		    ts.setDate2(dt);
		    dt = ts.getDate2Original();
		    dt.setTimeZone(NewTimeZone);
		    ts.setDate2Original(dt);
		    ts.addToGenesis("Changed time zone to \"" + NewTimeZone + "\".");
		    if ( ts.getDataIntervalBase() == TimeInterval.IRREGULAR ) {
		    	// Loop through all the data and set the time zone on the date/time
		    	TSIterator tsi = ts.iterator();
		    	TSData tsdata = null;
		    	while ( (tsdata = tsi.next()) != null ) {
		    		tsdata.setDate(tsdata.getDate().setTimeZone(NewTimeZone));
		    	}
		    }
		    // Set the time series dirty so limits will be recomputed and new dates used
		    ts.setDirty(true);
		    
		    // Otherwise, check the offset on the existing and new time zone.
		    // If no offset change, just change the zone on the dates.
		    
		    // TODO sam 2017-04-11 need to enable time zone shift which will either only change time zone
		    // or will also change the data.
		    // Else, have an offset.  Accomplish the time zone change by changing the period of record to
		    // new date/times that have the new time zone.  This can shift the internal data management
		    // so use the same logic as ChangePeriod() command.
			
			// Change the period...
		    /*
	        if ( NewStart_DateTime == null ) {
	            NewStart_DateTime = new DateTime(ts.getDate1());
	        }
	        if ( NewEnd_DateTime == null ) {
	            NewEnd_DateTime = new DateTime(ts.getDate2());
	        }
			Message.printStatus ( 2, routine, "Changing \"" +
			ts.getIdentifier()+ "\" period to " + NewStart_DateTime + " to " + NewEnd_DateTime + "." );
			*/
			try {
	            //ts.changePeriodOfRecord ( NewStart_DateTime, NewEnd_DateTime );
	            // No need to re-add to results list since the original reference is maintained.
			}
			catch ( Exception e ) {
				message = "Unexpected error changing time zone for \"" + ts.getIdentifier() + "\" (" + e + ").";
	            Message.printWarning ( warning_level,
	                    MessageUtil.formatMessageTag(
	                    command_tag, ++warning_count),
	                    routine,message);
				Message.printWarning(3,routine,e);
	            status.addToLog ( CommandPhaseType.RUN,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "See the log file for details - report the problem to software support." ) );
			}
		}
	}
	catch ( Exception e ) {
		if ( ts != null ) {
			message = "Unexpected error changing time zone for time series \"" + ts.getIdentifier() + "\" (" + e + ").";
		}
		else {
			message = "Unexpected error changing time zone for time series (" + e + ").";
		}
        Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag, ++warning_count),
                routine,message);
		Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
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
		"NewTimeZone"
	};
	return this.toString(parameters, parameterOrder);
}

}