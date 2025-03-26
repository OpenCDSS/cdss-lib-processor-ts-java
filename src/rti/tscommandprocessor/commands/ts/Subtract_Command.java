// Subtract_Command - This class initializes, checks, and runs the Subtract() command.

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
This class initializes, checks, and runs the Subtract() command.
*/
public class Subtract_Command extends AbstractCommand implements Command
{

/**
Values for the HandleMissingHow parameter.
*/
protected final String _IgnoreMissing = "IgnoreMissing";
protected final String _SetMissingIfOtherMissing = "SetMissingIfOtherMissing";
protected final String _SetMissingIfAnyMissing = "SetMissingIfAnyMissing";

/**
Constructor.
*/
public Subtract_Command () {
	super();
	setCommandName ( "Subtract" );
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
	String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    //String SubtractTSList = parameters.getValue ( "SubtractTSList" );
	String HandleMissingHow = parameters.getValue ( "HandleMissingHow" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    /*
	if ( (TSList != null) && !TSListType.ALL_MATCHING_TSID.equals(TSList) ) {
		if ( TSID != null ) {
            message = "TSID should only be specified when TSList=" + TSListType.ALL_MATCHING_TSID.toString() + ".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Do not specify the TSID parameter when TList=" + TSListType.ALL_MATCHING_TSID.toString() ) );
		}
	}
    */
    /*
	if ( TSList == null ) {
		// Probably legacy command.
		// TODO SAM 2005-05-17 Need to require TSList when legacy commands are safely nonexistent.
		// At that point the following check can occur in any case.
		if ( (TSID == null) || (TSID.length() == 0) ) {
            message = "A TSID must be specified.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a TSList parameter value." ) );
		}
	}
    */

    if ( ((TSID == null) || TSID.equals("")) && ((EnsembleID == null) || EnsembleID.equals("")) ) {
        message = "Neither TSID or EnsembleID have been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the TSID or EnsembleID to process." ) );
    }
    if ( (TSID != null) && !TSID.equals("") && (EnsembleID != null) && !EnsembleID.equals("") ) {
        message = "Only one of the TSID and EnsembleID should be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the TSID or EnsembleID to process." ) );
    }

    if ( (HandleMissingHow != null) && !HandleMissingHow.equals("") &&
            !HandleMissingHow.equalsIgnoreCase(_IgnoreMissing) &&
            !HandleMissingHow.equalsIgnoreCase(_SetMissingIfOtherMissing) &&
            !HandleMissingHow.equalsIgnoreCase(_SetMissingIfAnyMissing) ) {
        message = "The HandleMissingHow parameter (" + HandleMissingHow + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify HandleMissingHow as " + _IgnoreMissing + ", " +
                _SetMissingIfOtherMissing + ", or " + _SetMissingIfAnyMissing) );
    }

    if ( (AnalysisStart != null) && !AnalysisStart.isEmpty() && (AnalysisStart.indexOf("${") < 0) ) {
		try {
		    DateTime.parse(AnalysisStart);
		}
		catch ( Exception e ) {
            message = "The analysis start date/time \"" + AnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time." ) );
		}
	}
	if ( (AnalysisEnd != null) && !AnalysisEnd.isEmpty() && (AnalysisEnd.indexOf("${") < 0) ) {
		try {
		    DateTime.parse( AnalysisEnd );
		}
		catch ( Exception e ) {
            message = "The analysis end date/time \"" + AnalysisEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time." ) );
		}
	}

	// Check for invalid parameters.
    List<String> validList = new ArrayList<>(8);
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "SubtractTSList" );
    validList.add ( "SubtractTSID" );
    validList.add ( "SubtractEnsembleID" );
    validList.add ( "HandleMissingHow" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
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
@return true if the command was edited (e.g., "OK" was pressed),
and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new Subtract_JDialog ( parent, this )).ok();
}

/**
Get the time series to process.
@param its Position in time series array to get time series.
@param tspos Positions in time series processor time series array.
*/
private TS getTimeSeriesToProcess ( int its, int[] tspos, String command_tag, int warning_count ) {
    String routine = getClass().getSimpleName() + ".getTimeSeriesToProcess";
    TS ts = null;
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "Index", Integer.valueOf(tspos[its]) );
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
Parse the command string into a PropList of parameters.
This method currently supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
	int warning_level = 2;
	String routine = getClass().getSimpleName() + ".parseCommand", message;

	if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()")) {
        // Current syntax.
        super.parseCommand( command_string);
        // Recently added TSList so handle it properly.
        PropList parameters = getCommandParameters();
        String TSList = parameters.getValue ( "TSList");
        String SubtractTSList = parameters.getValue ( "SubtractTSList");
        String SubtractTSID = parameters.getValue ( "SubtractTSID");
        if ( ((SubtractTSList == null) || (SubtractTSList.length() == 0)) && ((SubtractTSID != null) && (SubtractTSID.length() > 0)) ) {
            // Old command where SubtractTSID= is specified but SubtractTSList is not.
            // TSList may be used instead of SubtractTSList and if so use it.
            if ( (TSList != null) && (TSList.length() > 0) ) {
                SubtractTSList = TSList;
                // Convert to newer syntax:
                // - SpecifiedTS replaced with SpecifiedTSID
                if ( SubtractTSList.equalsIgnoreCase("SpecifiedTS") ) {
                    SubtractTSList = TSListType.SPECIFIED_TSID.toString();
                }
                parameters.set ( "SubtractTSList", SubtractTSList );
            }
            else {
                // Examine SubtractTSID to figure out what to do.
                if ( SubtractTSID.indexOf("*") >= 0 ) {
                    SubtractTSList = TSListType.ALL_TS.toString();
                }
                else {
                    SubtractTSList = TSListType.SPECIFIED_TSID.toString();
                }
            }
            parameters.set ( "SubtractTSList", SubtractTSList );
        }
        else if ( (TSList != null) && (TSList.length() > 0) ) {
            // Convert to newer syntax:
        	// - SpecifiedTS replaced with SpecifiedTSID
            SubtractTSList = TSList;
            if ( SubtractTSList.equalsIgnoreCase("SpecifiedTS") ) {
                SubtractTSList = TSListType.SPECIFIED_TSID.toString();
            }
            parameters.set ( "SubtractTSList", SubtractTSList );
        }
    }
    else {
        // TODO SAM 2008-10-23 This whole block of code needs to be
        // removed as soon as commands have been migrated to the new syntax.
        //
        // Old syntax without named parameters.
    	List<String> v = StringUtil.breakStringList ( command_string,"(),", StringUtil.DELIM_ALLOW_STRINGS );
        if ( (v == null) || (v.size() < 4) ) {
            message = "Syntax error in legacy command \"" + command_string +
                "Expecting Subract(TSID,HandleMissingHow,SubtractTSID,...";
            Message.printWarning ( warning_level, routine, message);
            throw new InvalidCommandSyntaxException ( message );
        }
        String TSID = ((String)v.get(1)).trim();
        String HandleMissingHow = ((String)v.get(2)).trim();
        StringBuffer SubtractTSID = new StringBuffer();
        for ( int i = 3; i < v.size(); i++ ) {
            // Fourth and fifth fields optionally have analysis period.
            if ( i > 3 ) {
                SubtractTSID.append(",");
            }
            SubtractTSID.append(((String)v.get(i)).trim());
        }

        // Set parameters and new defaults.

        PropList parameters = new PropList ( getCommandName() );
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        parameters.set ( "TSID", TSID );
        parameters.set ( "TSList", TSListType.SPECIFIED_TSID.toString() );
        if ( HandleMissingHow.length() > 0 ) {
            parameters.set ( "HandleMissingHow", HandleMissingHow );
        }
        parameters.set ( "SubtractTSID", SubtractTSID.toString() );
        parameters.setHowSet ( Prop.SET_UNKNOWN );
        setCommandParameters ( parameters );

        message = "Automatically updated to current syntax from old command \"" + command_string + "\".";
        CommandStatus status = getCommandStatus();
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.INFO, message, "" ) );
    }
}

/**
Run the command.
@param command_number number of command to run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	//int log_level = 3;	// Warning message level for non-user messages.

	// Make sure there are time series available to operate on.

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
    	// Should not happen.
    }
    if ( clearStatus ) {
		status.clearLog(CommandPhaseType.RUN);
	}

	// Get the time series to process.  This should be a single matching time series or ensemble time series.

	PropList request_params = new PropList ( "" );
	// Only one of these will be specified.
    String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String TSList = null;
    if ( (TSID != null) && (TSID.length() > 0) ) {
        TSList = TSListType.ALL_MATCHING_TSID.toString();   // Should only match one?
        request_params.set ( "TSList", TSList );
        request_params.set ( "TSID", TSID );
    }
    else if ( (EnsembleID != null) && (EnsembleID.length() > 0)) {
        TSList = TSListType.ENSEMBLE_ID.toString();
        request_params.set ( "TSList", TSList );
        request_params.set ( "EnsembleID", EnsembleID );
    }
	String AnalysisStart = parameters.getValue ( "AnalysisStart" ); // ${Property} is handled below.
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" ); // ${Property} is handled below.

	// Figure out the dates to use for the analysis.
	// Default of null means to analyze the full period.
	DateTime AnalysisStart_DateTime = null;
	DateTime AnalysisEnd_DateTime = null;

	try {
		AnalysisStart_DateTime = TSCommandProcessorUtil.getDateTime ( AnalysisStart, "AnalysisStart", processor,
			status, warning_level, command_tag );
	}
	catch ( InvalidCommandParameterException e ) {
		// Warning will have been added above.
		++warning_count;
	}
	try {
		AnalysisEnd_DateTime = TSCommandProcessorUtil.getDateTime ( AnalysisEnd, "AnalysisEnd", processor,
			status, warning_level, command_tag );
	}
	catch ( InvalidCommandParameterException e ) {
		// Warning will have been added above.
		++warning_count;
	}

	CommandProcessorRequestResultsBean bean = null;
	try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
		Message.printWarning(warning_level,
		    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
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
    else {
        if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ) {
            // Expecting only a single time series.
            if ( nts != 1 ) {
                message = "Expecting to find one time series to process (have " + nts + ") using TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(
                command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.WARNING,
                        message,
                        "Verify that the TSList parameter matches one time series." ) );
            }
        }
    }

	// Time series to subtract.

    String SubtractTSList = parameters.getValue ( "SubtractTSList" );
    if ( (SubtractTSList == null) || SubtractTSList.equals("") ) {
        SubtractTSList = TSListType.ALL_TS.toString();
    }
    String SubtractTSID = parameters.getValue ( "SubtractTSID" );
	if ( (SubtractTSID != null) && (SubtractTSID.indexOf("${") >= 0) ) {
		SubtractTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, SubtractTSID);
	}
    String SubtractEnsembleID = parameters.getValue ( "SubtractEnsembleID" );
	if ( (SubtractEnsembleID != null) && (SubtractEnsembleID.indexOf("${") >= 0) ) {
		SubtractEnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, SubtractEnsembleID);
	}
    request_params = new PropList ( "" );
    request_params.set ( "TSList", SubtractTSList );
    request_params.set ( "TSID", SubtractTSID );
    request_params.set ( "EnsembleID", SubtractEnsembleID );
    try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + SubtractTSList +
        "\", TSID=\"" + SubtractTSID + "\", EnsembleID=\"" + SubtractEnsembleID + "\") from processor.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    Object o_TSList2 = bean_PropList.getContents ( "TSToProcessList" );
    List<TS> subtract_tslist = null;
    if ( o_TSList2 == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + SubtractTSList +
        "\" TSID=\"" + SubtractTSID + "\", EnsembleID=\"" + SubtractEnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the SubtractTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    else {
    	@SuppressWarnings("unchecked")
		List<TS> subtract_tslist0 = (List<TS>)o_TSList2;
        subtract_tslist = subtract_tslist0;
        if ( subtract_tslist.size() == 0 ) {
            message = "No time series to subtract are available from processor GetTimeSeriesToProcess (TSList=\"" + SubtractTSList +
            "\" TSID=\"" + SubtractTSID + "\", EnsembleID=\"" + SubtractEnsembleID + "\".";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message,
                    "Verify that the SubtractTSList parameter matches one or more time series - may be OK for partial run." ) );
        }
    }
    Object o_Indices2 = bean_PropList.getContents ( "Indices" );
    int [] subtract_tspos = null;
    if ( o_Indices2 == null ) {
        message = "Unable to find indices for time series to subtract using TSList=\"" + SubtractTSList +
        "\" TSID=\"" + SubtractTSID + "\", EnsembleID=\"" + SubtractEnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
    }
    else {
        subtract_tspos = (int [])o_Indices2;
        if ( subtract_tspos.length == 0 ) {
            message = "Unable to find indices for time series to subtract using TSList=\"" + SubtractTSList +
            "\" TSID=\"" + SubtractTSID + "\", EnsembleID=\"" + SubtractEnsembleID + "\".";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
        }
    }

    int n_subtract_ts = 0;
    if ( subtract_tslist != null ) {
        n_subtract_ts = subtract_tslist.size();
    }
    if ( n_subtract_ts == 0 ) {
        message = "Unable to find any time series to subtract using TSList=\"" + SubtractTSList +
        "\" TSID=\"" + SubtractTSID + "\", EnsembleID=\"" + SubtractEnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.WARNING,
                message,
                "Verify that the SubtractTSList parameter matches one or more time series - may be OK for partial run." ) );
    }

    // Make sure that the number of dependent and independent time series is consistent.
    // Already checked to make sure there is a single time series above if NOT processing ensembles so
    // just check ensembles here.

    if ( TSListType.ENSEMBLE_ID.equals(SubtractTSList) ) {
        if ( (n_subtract_ts != 1) && (n_subtract_ts != nts) ) {
            message = "The number if time series to subtract to the ensemble (" + n_subtract_ts +
                ") must be 1 or the same number in the ensemble (" + nts + ").";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message,
                    "Verify that the number of time series to subtract is 1 or the same as the ensemble." ) );
        }
    }

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series).
		message = "Insufficient data to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}

	// Now process the time series.

    /*
    String TransferHow = parameters.getValue("TransferHow");
	PropList setprops = new PropList ( "Subtract" );
	if ( (TransferHow != null) && !TransferHow.equals("") ) {
		setprops.set ( "TransferHow", TransferHow );
	}
    */

    String HandleMissingHow = parameters.getValue("HandleMissingHow");
    int HandleMissingHow_int = TSUtil.IGNORE_MISSING;
    if ( (HandleMissingHow == null) || HandleMissingHow.isEmpty() ) {
        HandleMissingHow = "IgnoreMissing"; // Default.
    }
    if ( HandleMissingHow.equalsIgnoreCase( _IgnoreMissing ) ) {
        HandleMissingHow_int = TSUtil.IGNORE_MISSING;
    }
    else if ( HandleMissingHow.equalsIgnoreCase( _SetMissingIfOtherMissing ) ){
        HandleMissingHow_int = TSUtil.SET_MISSING_IF_OTHER_MISSING;
    }
    else if ( HandleMissingHow.equalsIgnoreCase( _SetMissingIfAnyMissing ) ) {
        HandleMissingHow_int=TSUtil.SET_MISSING_IF_ANY_MISSING;
    }

	TS ts = null;  // Time series to be subtracted from.
    // Loop through the time series being modified.
	for ( int its = 0; its < nts; its++ ) {
		ts = getTimeSeriesToProcess ( its, tspos, command_tag, warning_count );
		if ( ts == null ) {
			// Skip time series.
            message = "Unable to get time series at position " + tspos[its] + " - null time series.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
			continue;
		}

        // TODO SAM 2008-01-06 Phase out if customer does not need or if a more robust way to check
        // for dates can be implemented.
        // Special check inspired by CDSS where people tried to subtract FrostDate time series.
        if ( StringUtil.indexOfIgnoreCase(ts.getDataType(), "FrostDate", 0) >= 0 ) {
            // TODO - SAM 2005-05-20
            // This is a special check because the subtract() command used to
            // be used in TSTool to subtract frost date time series.
        	// Now the Subtract() command is not suitable.
            message = "The " + getCommandName() + "() command is not suitable for frost dates - skipping processing.";
            Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Use Blend(), SetFromTS(), or similar commands." ) );
        }

		// Get the specific time series to subtract depending on the input parameters.

        TS tstosubtract = null;  // Single time series to subtract.
        List<TS> tstosubtract_list = new ArrayList<TS>(); // List of time series to subtract.
        if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ) {
            // Processing a single time series.  Subtract all the time series from it.
            // Reuse the same independent time series for all transfers.
            tstosubtract_list = subtract_tslist;
            Message.printStatus(2, routine, "Subtracting " + tstosubtract_list.size() +
                    " time series from single time series \"" + ts.getIdentifier() + "\"" );
        }
        else if ( TSListType.ENSEMBLE_ID.equals(TSList) ) {
            // Processing an ensemble.  Need to loop through each time series in the ensemble and subtract a single
            // time series (either from a single TS or another ensemble).
            // Get the time series matching the loop index.
            if ( TSListType.ENSEMBLE_ID.equals(SubtractTSList) ) {
                // Subtracting an ensemble from an ensemble so get the ensemble time series at the position.
                tstosubtract = getTimeSeriesToProcess ( its, subtract_tspos, command_tag, warning_count );
                tstosubtract_list.add( tstosubtract );
                Message.printStatus(2, routine, "Subtracting ensemble time series \"" + tstosubtract.getIdentifier() +
                        "\" from ensemble time series \"" + ts.getIdentifier() + "\".");
            }
            else {
                // Subtracting another time series from an ensemble
            	// (checks above verified that only one time series is subtracted).
                Message.printStatus(2, routine, "Subtracting " + tstosubtract_list.size() +
                        " time series from ensemble time series \"" + ts.getIdentifier() + "\".");
                tstosubtract_list = subtract_tslist;
            }
        }

        int tstosubtract_list_size = tstosubtract_list.size();
        if ( tstosubtract_list_size == 0 ) {
            // Skip time series.
            message = "Zero time series to subtract for " + ts.getIdentifier();
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message, "May be be OK for partial run." ) );
            continue;
        }

        // Remove from the time series list the time series being subtracted to (don't subtract from itself).

        TS subtract_ts;
        for ( int icheck = 0; icheck < tstosubtract_list_size; icheck++ ) {
            subtract_ts = (TS)subtract_tslist.get(icheck);
            if ( subtract_ts == null ) {
                continue;
            }
            else if ( subtract_ts == ts ) {
                Message.printStatus(2, routine, "Removing \"" + subtract_ts.getIdentifier() +
                        "\" from subtract since it is same as the receiving time series." );
                subtract_tslist.remove(icheck);
                --icheck;
                --tstosubtract_list_size;
            }
        }

        // Finally do the subtract.

        try {
            TSUtil.subtract ( ts, tstosubtract_list, HandleMissingHow_int, AnalysisStart_DateTime, AnalysisEnd_DateTime );
        }
        catch ( Exception e ) {
            message = "Unexpected error subtracting from time series \"" + ts.getIdentifier() + "\" (" + e + ").";
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
    	"TSID",
    	"EnsembleID",
    	"SubtractTSList",
    	"SubtractTSID",
    	"SubtractEnsembleID",
    	"HandleMissingHow",
    	//"TransferHow",
		"AnalysisStart",
		"AnalysisEnd"
	};
	return this.toString(parameters, parameterOrder);
}

}