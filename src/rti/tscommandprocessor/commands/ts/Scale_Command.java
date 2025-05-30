// Scale_Command - This class initializes, checks, and runs the Scale() command.

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

/**
This class initializes, checks, and runs the Scale() command.
*/
public class Scale_Command extends AbstractCommand implements Command
{

/**
Possible value for ScaleValue, when not a number.
*/
protected final String _DaysInMonth = "DaysInMonth";
protected final String _DaysInMonthInverse = "DaysInMonthInverse";

/**
Constructor.
*/
public Scale_Command () {
	super();
	setCommandName ( "Scale" );
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
	//String TSID = parameters.getValue ( "TSID" );
	String ScaleValue = parameters.getValue ( "ScaleValue" );
    if ( ScaleValue == null ) {
        ScaleValue = ""; // To simplify checks below.
    }
    String MonthValues = parameters.getValue ( "MonthValues" );
    if ( MonthValues == null ) {
        MonthValues = ""; // To simplify checks below.
    }
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    /* TODO SAM 2008-01-03 Evaluate whether need to check combinations.
	if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a time series identifier." ) );
	}
    */
	if ( !ScaleValue.isEmpty() && !ScaleValue.startsWith("${") && !ScaleValue.equalsIgnoreCase(_DaysInMonth) &&
		!ScaleValue.equalsIgnoreCase(_DaysInMonthInverse) &&
		!StringUtil.isDouble(ScaleValue) ) {
        message = "The scale value (" + ScaleValue + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the scale value as a number, " + _DaysInMonth + ", or " + _DaysInMonthInverse + "." ) );
	}
    if ( !MonthValues.isEmpty() && !MonthValues.startsWith("${") ) {
        List<String> v = StringUtil.breakStringList ( MonthValues,",", 0 );
        if ( (v == null) || (v.size() != 12) ) {
            message = "12 monthly values must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 12 monthly values separated by commas." ) );
        }
        else {
            String val;
            for ( int i = 0; i < 12; i++ ) {
                val = v.get(i).trim();
                if ( !StringUtil.isDouble(val) ) {
                    message = "Monthly value \"" + val + "\" is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                             message, "Specify 12 monthly values separated by commas." ) );
                }
            }
        }
    }
    if ( ScaleValue.isEmpty() && MonthValues.isEmpty() ) {
        message = "Single or monthly scale values must be specified.";;
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Choose a single value or monthly values, but not both." ) );
    }
    if ( !ScaleValue.isEmpty() && !MonthValues.isEmpty() ) {
        message = "Both single and monthly scale values are specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Choose a single value or monthly values, but not both." ) );
    }
	if ( (AnalysisStart != null) && !AnalysisStart.isEmpty() && !AnalysisStart.startsWith("${") &&
		!AnalysisStart.equalsIgnoreCase("OutputStart") && !AnalysisStart.equalsIgnoreCase("OutputEnd") ) {
		try {
		    DateTime.parse(AnalysisStart);
		}
		catch ( Exception e ) {
            message = "The analysis start date/time \"" + AnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time, OutputStart, or output end." ) );
		}
	}
	if ( (AnalysisEnd != null) && !AnalysisEnd.isEmpty() && !AnalysisEnd.startsWith("${") &&
		!AnalysisEnd.equalsIgnoreCase("OutputStart") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
		try {
		    DateTime.parse( AnalysisEnd );
		}
		catch ( Exception e ) {
            message = "The analysis end date/time \"" + AnalysisEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
		}
	}

    // Check for invalid parameters.
	List<String> validList = new ArrayList<>(8);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "ScaleValue" );
    validList.add ( "MonthValues" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
    validList.add ( "NewUnits" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level, MessageUtil.formatMessageTag(command_tag,warning_level), warning );
		throw new InvalidCommandParameterException ( warning );
	}

    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new Scale_JDialog ( parent, this )).ok();
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
    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax.
        super.parseCommand( command_string);
        // Recently added TSList so handle it properly.
        PropList parameters = getCommandParameters();
        String TSList = parameters.getValue ( "TSList");
        String TSID = parameters.getValue ( "TSID");
        if ( ((TSList == null) || (TSList.length() == 0)) && // TSList not specified.
            ((TSID != null) && (TSID.length() != 0)) ) { // but TSID is specified.
            // Assume old-style where TSList was not specified but TSID was.
            if ( (TSID != null) && TSID.indexOf("*") >= 0 ) {
                parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
            }
            else {
                parameters.set ( "TSList", TSListType.LAST_MATCHING_TSID.toString() );
            }
        }
    }
    else {
		// TODO SAM 2005-08-24 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax without named parameters.
    	List<String> v = StringUtil.breakStringList ( command_string,"(),", StringUtil.DELIM_SKIP_BLANKS );
		String TSID = "";
		String ScaleValue = "";
		String AnalysisStart = "";
		String AnalysisEnd = "";
		if ( (v != null) && (v.size() >= 3) ) {
			// Second field is identifier.
			TSID = v.get(1).trim();
			// Third field has scale.
			ScaleValue = v.get(2).trim();
			// Fourth and fifth fields optionally have analysis period.
			if ( v.size() >= 4 ) {
				AnalysisStart = v.get(3).trim();
				if ( AnalysisStart.equals("*") ) {
					// Change to new default.
					AnalysisStart = "";
				}
			}
			if ( v.size() >= 5 ) {
				AnalysisEnd = v.get(4).trim();
				if ( AnalysisEnd.equals("*") ) {
					// Change to new default.
					AnalysisEnd = "";
				}
			}
		}

		// Set parameters and new defaults.

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        if ( TSID.length() > 0 ) {
			parameters.set ( "TSID", TSID );
            if ( TSID.indexOf("*") >= 0 ) {
                parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
            }
            else {
                parameters.set ( "TSList", TSListType.LAST_MATCHING_TSID.toString() );
            }
		}
		if ( ScaleValue.length() > 0 ) {
			parameters.set ( "ScaleValue", ScaleValue );
		}
		if ( AnalysisStart.length() > 0 ) {
			parameters.set ( "AnalysisStart", AnalysisStart );
		}
		if ( AnalysisEnd.length() > 0 ) {
			parameters.set ( "AnalysisEnd", AnalysisEnd );
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
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;  // Level for non-use messages for log file.

	// Make sure there are time series available to operate on.

	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    Boolean clearStatus = Boolean.valueOf(true); // Default.
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
	String ScaleValue = parameters.getValue ( "ScaleValue" );
	if ( (ScaleValue != null) && (ScaleValue.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		ScaleValue = TSCommandProcessorUtil.expandParameterValue(processor, this, ScaleValue);
	}
    String MonthValues = parameters.getValue("MonthValues");
	if ( (MonthValues != null) && (MonthValues.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		MonthValues = TSCommandProcessorUtil.expandParameterValue(processor, this, MonthValues);
	}
    double [] MonthValues_double = null;
    if ( (MonthValues != null) && (MonthValues.length() > 0) ) {
        MonthValues_double = new double[12];
        List<String> v = StringUtil.breakStringList ( MonthValues,",", 0 );
        String val;
        for ( int i = 0; i < 12; i++ ) {
            val = v.get(i).trim();
            try {
            	MonthValues_double[i] = Double.parseDouble ( val );
            }
            catch ( Exception e ) {
    	        message = "Month " + (i + 1) + " scale value (" + val + ") is not a number.";
    	        Message.printWarning(log_level,
    	            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
    	        status.addToLog ( commandPhase,
    	            new CommandLogRecord(CommandStatusType.FAILURE,
    	                message, "Specify the the scale value as a number.") );
            }
        }
    }
	String AnalysisStart = parameters.getValue ( "AnalysisStart" ); // ${Property} is handled below.
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String NewUnits = parameters.getValue ( "NewUnits" );
	if ( (NewUnits != null) && (NewUnits.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		NewUnits = TSCommandProcessorUtil.expandParameterValue(processor, this, NewUnits);
	}

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

	// Get the time series to process.  Allow TSID to be a pattern or specific time series.

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
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
    if ( bean == null ) {
        Message.printStatus ( 2, routine, "Bean is null.");
    }
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	List<TS> tslist = null;
	if ( o_TSList == null ) {
		message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( log_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
	}
	else {
		@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
        tslist = tslist0;
		if ( tslist.size() == 0 ) {
			message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
			Message.printWarning ( log_level,
					MessageUtil.formatMessageTag(
							command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message,
                            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
		}
	}

	int nts = tslist.size();
	if ( nts == 0 ) {
		message = "Unable to find time series to scale using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
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

	TS ts = null;
	Object o_ts = null;
	for ( int its = 0; its < nts; its++ ) {
		// The the time series to process, from the list that was returned above.
		o_ts = tslist.get(its);
		if ( o_ts == null ) {
			message = "Time series to process is null.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message,
                            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
			// Go to next time series.
			continue;
		}
		ts = (TS)o_ts;

		try {
		    // Do the scaling.
		    notifyCommandProgressListeners ( its, nts, (float)-1.0, "Scaling " +
	            ts.getIdentifier().toStringAliasAndTSID() );
			Message.printStatus ( 2, routine, "Scaling \"" + ts.getIdentifier()+ "\" by " + ScaleValue );
			if ( MonthValues_double == null ) {
			    // Scaling by a single value.
			    TSUtil.scale ( ts, AnalysisStart_DateTime, AnalysisEnd_DateTime, -1, ScaleValue );
			}
			else {
			    // Scaling by monthly values.
			    for ( int i = 1; i <= 12; i++ ) {
			        TSUtil.scale ( ts, AnalysisStart_DateTime, AnalysisEnd_DateTime, i, MonthValues_double[i - 1] );
			    }
			}
			// If requested, change the data units.
			if ( (NewUnits != null) && (NewUnits.length() > 0) ) {
				ts.addToGenesis ( "Changed units from \"" + ts.getDataUnits() + "\" to \"" + NewUnits+"\"");
				ts.setDataUnits ( NewUnits );
			}
		}
		catch ( Exception e ) {
			message = "Unexpected error scaling time series \""+ ts.getIdentifier() + "\" by " + ScaleValue +
			    "(" + e + ").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count),routine,message );
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
		"ScaleValue",
		"MonthValues",
		"AnalysisStart",
		"AnalysisEnd",
		"NewUnits"
	};
	return this.toString(parameters, parameterOrder);
}

}