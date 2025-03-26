// FillProrate_Command - This class initializes, checks, and runs the FillProrate() command.

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
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the FillProrate() command.
*/
public class FillProrate_Command extends AbstractCommand implements Command
{
    
protected String _Forward = "Forward";
protected String _Backward = "Backward";

protected String _AnalyzeAverage = "AnalyzeAverage";
protected String _NearestPoint = "NearestPoint";

protected String _NearestBackward = "NearestBackward";
protected String _NearestForward = "NearestForward";
    
/**
Constructor.
*/
public FillProrate_Command ()
{	super();
	setCommandName ( "FillProrate" );
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
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String FillFlag = parameters.getValue ( "FillFlag" );
	String FillDirection = parameters.getValue ( "FillDirection" );
	String FactorMethod = parameters.getValue ( "FactorMethod" );
	String InitialValue = parameters.getValue ( "InitialValue" );
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
    
	if ( (TSID == null) || (TSID.length() == 0) ) {
        message = "An independent TSID has not been specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Specify a IndependentTSID parameter value." ) );
	}
 	if ( (FillStart != null) && !FillStart.equals("") && !FillStart.equalsIgnoreCase("OutputStart")){
		try {
		    DateTime.parse(FillStart);
		}
		catch ( Exception e ) {
            message = "The fill start date/time \"" + FillStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	if ( (FillEnd != null) && !FillEnd.equals("") && !FillEnd.equalsIgnoreCase("OutputEnd") ) {
		try {
		    DateTime.parse( FillEnd);
		}
		catch ( Exception e ) {
            message = "The fill end date/time \"" + FillStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
    if ( (FillFlag != null) && !FillFlag.equals("") && (FillFlag.length() != 1) ) {
        message = "The fill flag must be 1 character long.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a 1-character fill flag or blank to not use a flag." ) );
    }
    if ( (FillDirection != null) && !FillDirection.equals("") &&
            !FillDirection.equalsIgnoreCase(_Backward) &&
            !FillDirection.equalsIgnoreCase(_Forward) ) {
        message = "The FillDirection parameter (" + FillDirection + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify FillDirection as " + _Backward + " or " +
                _Forward + " (default).") );
    }
    if ( (FactorMethod != null) && !FactorMethod.equals("") &&
            !FactorMethod.equalsIgnoreCase(_AnalyzeAverage) &&
            !FactorMethod.equalsIgnoreCase(_NearestPoint) ) {
        message = "The FactorMethod parameter (" + FactorMethod + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify FactorMethod as " + _AnalyzeAverage + " or " +
                _NearestPoint + " (default).") );
    }
    if ( (AnalysisStart != null) && !AnalysisStart.equals("") && !AnalysisStart.equalsIgnoreCase("OutputStart")){
        try {
            DateTime.parse(AnalysisStart);
        }
        catch ( Exception e ) {
            message = "The analysis start date/time \"" + AnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
        }
    }
    if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse( AnalysisEnd);
        }
        catch ( Exception e ) {
            message = "The analysis end date/time \"" + AnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
        }
    }
    if ( (InitialValue != null) && !InitialValue.equals("") &&
            !InitialValue.equalsIgnoreCase(_Backward) &&
            !InitialValue.equalsIgnoreCase(_Forward) &&
            !StringUtil.isDouble(InitialValue) ) {
        message = "The InitialValue parameter (" + InitialValue + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify InitialValue as " + _NearestBackward + " or " +
                _NearestForward + ", or a specified number.") );
    }
    
	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>();
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "IndependentTSID" );
    validList.add ( "FillStart" );
    validList.add ( "FillEnd" );
    validList.add ( "FillFlag" );
    validList.add ( "FillDirection" );
    validList.add ( "FactorMethod" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
    validList.add ( "InitialValue" );
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
	return (new FillProrate_JDialog ( parent, this )).ok();
}

/**
Get the time series to process.
@param its Position in time series array to get time series.
@param tspos Positions in time series processor time series array.
*/
private TS getTimeSeriesToProcess ( int its, int[] tspos, String command_tag, int warning_count )
{   String routine = "FillProrate_Command.getTimeSeriesToProcess";
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
        message = "Error requesting GetTimeSeries(Index=" + tspos[its] + ") from processor.";
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
        message = "Null value for GetTimeSeries(Index=" + tspos[its] + ") returned from processor.";
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
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	// Parse using base class method...
    super.parseCommand( command_string);
    PropList parameters = getCommandParameters();
    // Legacy is either one ID or a pattern.
    if ( parameters.getValue("TSList") == null ) {
        parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
    }
    // Reset some values that have changed...
    String CalculateFactorHow = parameters.getValue ( "CalculateFactorHow" );
    if ( CalculateFactorHow != null ) {
        // Reset to new value...
        parameters.unSet ( "CalculateFactorHow" );
        parameters.set ( "FactorMethod", CalculateFactorHow );
    }
}

/**
Run the command.
@param command_number number of command to run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "FillProrate_Command.runCommand", message;
	int warning_count = 0;
	int warningLevel = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Warning message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);

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
    
    String IndependentTSID = parameters.getValue ( "IndependentTSID" );
    TS independent_ts = null;
    try {   request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "TSID", IndependentTSID );
            try {
                bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + IndependentTSID + "\") from processor.";
                Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
                Message.printWarning(log_level, routine, e );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
            }
            bean_PropList = bean.getResultsPropList();
            Object o_TS = bean_PropList.getContents ( "TS");
            if ( o_TS == null ) {
                message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + IndependentTSID +
                "\") from processor.";
                Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
            }
            else {
                independent_ts = (TS)o_TS;
            }
    }
    catch ( Exception e ) {
        independent_ts = null;
    }
    if ( independent_ts == null ) {
        message = "Unable to find time series to process using TSID \"" + IndependentTSID + "\".";
        Message.printWarning ( log_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }

	// Fill period...

	String FillStart = parameters.getValue("FillStart");
	String FillEnd = parameters.getValue("FillEnd");

	// Figure out the dates to use for the analysis...
	DateTime FillStart_DateTime = null;
	DateTime FillEnd_DateTime = null;

	try {
	if ( FillStart != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", FillStart );
		bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting FillStart DateTime(DateTime=" +	FillStart + ") from processor.";
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
			message = "Null value for FillStart DateTime(DateTime=" + FillStart + "\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	FillStart_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "FillStart \"" + FillStart + "\" is invalid.";
		Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time or OutputStart." ) );
		throw new InvalidCommandParameterException ( message );
	}
	
	try {
	if ( FillEnd != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", FillEnd );
		bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting FillEnd DateTime(DateTime=" + FillEnd + "\") from processor.";
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
			message = "Null value for FillEnd DateTime(DateTime=" + FillEnd +	"\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	FillEnd_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "FillEnd \"" + FillEnd + "\" is invalid.";
		Message.printWarning(warningLevel,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time or OutputEnd." ) );
		throw new InvalidCommandParameterException ( message );
	}
	
	// Analysis period...

    String AnalysisStart = parameters.getValue("AnalysisStart");
    String AnalysisEnd = parameters.getValue("AnalysisEnd");
    DateTime AnalysisStart_DateTime = null;
    DateTime AnalysisEnd_DateTime = null;

    try {
    if ( AnalysisStart != null ) {
        request_params = new PropList ( "" );
        request_params.set ( "DateTime", AnalysisStart );
        bean = null;
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting AnalysisStart DateTime(DateTime=" + AnalysisStart + ") from processor.";
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
            message = "Null value for AnalysisStart DateTime(DateTime=" + AnalysisStart + "\") returned from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
            throw new InvalidCommandParameterException ( message );
        }
        else {
            AnalysisStart_DateTime = (DateTime)prop_contents;
        }
    }
    }
    catch ( Exception e ) {
        message = "AnalysisStart \"" + AnalysisStart + "\" is invalid.";
        Message.printWarning(warningLevel,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time or OutputStart." ) );
        throw new InvalidCommandParameterException ( message );
    }
    
    try {
    if ( AnalysisEnd != null ) {
        request_params = new PropList ( "" );
        request_params.set ( "DateTime", AnalysisEnd );
        bean = null;
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting AnalysisEnd DateTime(DateTime=" + AnalysisEnd + "\") from processor.";
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
            message = "Null value for AnalysisEnd DateTime(DateTime=" + AnalysisEnd +   "\") returned from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
            throw new InvalidCommandParameterException ( message );
        }
        else {  AnalysisEnd_DateTime = (DateTime)prop_contents;
        }
    }
    }
    catch ( Exception e ) {
        message = "AnalysisEnd \"" + AnalysisEnd + "\" is invalid.";
        Message.printWarning(warningLevel,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
            routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Insufficient data to run command.";
		Message.printWarning ( warningLevel,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}

	// Now process the time series...

	// Define properties for utility fill method.
	PropList fillProps = new PropList ( "FillProrate" );
	String FillFlag = parameters.getValue ( "FillFlag" );
    if ( (FillFlag != null) && !FillFlag.equals("") ) {
        fillProps.set ( "FillFlag", FillFlag );
    }
    String FillDirection = parameters.getValue ( "FillDirection" );
    if ( (FillDirection == null) || FillDirection.equals("") ) {
        FillDirection = _Forward; // Default
    }
    fillProps.set ( "FillDirection", FillDirection );
    String FactorMethod = parameters.getValue ( "FactorMethod" );
    if ( (FactorMethod == null) || FactorMethod.equals("")) {
        FactorMethod = _NearestPoint; // Default
    }
    fillProps.set ( "FactorMethod", FactorMethod );
    // For analysis start, convert back to string because the above code interprets
    // special date/time strings but low-level code needs basic date/time
    if ( AnalysisStart_DateTime != null ) {
        fillProps.set ( "AnalysisStart", AnalysisStart_DateTime.toString() );
    }
    if ( AnalysisEnd_DateTime != null ) {
        fillProps.set ( "AnalysisEnd", AnalysisEnd_DateTime.toString() );
    }
    String InitialValue = parameters.getValue ( "InitialValue" );
    if ( (InitialValue != null) && !InitialValue.equals("") ) {
        fillProps.set ( "InitialValue", InitialValue );
    }

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
		if ( ts == independent_ts ) {
		    // No need to fill...
		    continue;
		}
		notifyCommandProgressListeners ( its, nts, (float)-1.0, "Filling time series " +
            ts.getIdentifier().toStringAliasAndTSID() );
		Message.printStatus ( 2, routine, "Filling \"" + ts.getIdentifier()+ "\" from \"" +
            independent_ts.getIdentifier() + "\"." );
		try {
		    TSUtil.fillProrate ( ts, independent_ts, FillStart_DateTime, FillEnd_DateTime, fillProps );
		}
		catch ( Exception e ) {
			message = "Unexpected error filling time series \"" + ts.getIdentifier() + "\" from \"" +
                independent_ts.getIdentifier() + "\" (" + e + ").";
            Message.printWarning ( warningLevel,
                MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
			Message.printWarning(3,routine,e);
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
		}
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
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TSList",
    	"TSID",
    	"EnsembleID",
    	"IndependentTSID",
		"FillStart",
		"FillEnd",
		"FillFlag",
    	"FillDirection",
    	"FactorMethod",
    	"AnalysisStart",
    	"AnalysisEnd",
    	"InitialValue"
	};
	return this.toString(parameters, parameterOrder);
}

}