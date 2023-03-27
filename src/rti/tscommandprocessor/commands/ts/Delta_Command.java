// Delta_Command - this class initializes, checks, and runs the Delta() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.ResetType;
import RTi.TS.TS;
import RTi.TS.TSUtil_Delta;
import RTi.TS.TrendType;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the Delta() command.
*/
public class Delta_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

	/**
	 * Possible values for ResetType.
	 */
	protected final String _Auto = "" + ResetType.AUTO;
	// TODO smalers 2023-03-31 implement in the future to only allow increasing or decreasing without change in direction
	//protected final String _None = "" + ResetType.NONE;
	protected final String _ResetValue = "" + ResetType.ROLLOVER;
	// TODO smalers 2023-03-21 could add date/time such as every day.

	/**
	Values for Action parameter.
	*/
	protected final String _Keep = "Keep";
	protected final String _SetMissing = "SetMissing";

	/**
	The table that is created for discovery mode.
	*/
	private DataTable __discoveryTable = null;

	/**
	List of time series read during discovery.
	These are TS objects but with mainly the metadata (TSIdent) filled in.
	*/
	private List<TS> __discoveryTSList = null;

	/**
	Constructor.
	*/
	public Delta_Command () {
		super();
		setCommandName ( "Delta" );
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
	// General.
    String ExpectedTrend = parameters.getValue ( "ExpectedTrend" );
    String ResetType = parameters.getValue ( "ResetType" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String Alias = parameters.getValue ( "Alias" );
	// Delta limit.
	String DeltaLimit = parameters.getValue ( "DeltaLimit" );
	String DeltaLimitAction = parameters.getValue ( "DeltaLimitAction" );
	//String DeltaLimitFlag = parameters.getValue ( "DeltaLimitFlag" );
	String IntervalLimit = parameters.getValue ( "IntervalLimit" );
	String IntervalLimitAction = parameters.getValue ( "IntervalLimitAction" );
	// ResetType=Auto
    String AutoResetDatum = parameters.getValue ( "AutoResetDatum" );
	// ResetType=Rollover
	String ResetMin = parameters.getValue ( "ResetMin" );
	String ResetMax = parameters.getValue ( "ResetMax" );
	String ResetProximityLimit = parameters.getValue ( "ResetProximityLimit" );
	String ResetProximityInterval = parameters.getValue ( "ResetProximityInterval" );
	// Output table.
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
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

    // Verify that the ExpectedTrend is a value that can be handled.
    if ( (ExpectedTrend != null) && !ExpectedTrend.isEmpty() && !ExpectedTrend.equalsIgnoreCase("" + TrendType.DECREASING) &&
        !ExpectedTrend.equalsIgnoreCase("" + TrendType.INCREASING) ) {
        message = "The ExpectedTrend (" + ExpectedTrend + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the trend as blank, " + TrendType.DECREASING + ", or " + TrendType.INCREASING + "." ) );
    }

    if ( (DeltaLimit != null) && !DeltaLimit.isEmpty() ) {
        if ( !StringUtil.isDouble(DeltaLimit) ) {
            message = "The DeltaLimit value (" + DeltaLimit + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the delta limit as a number." ) );
        }
    }

    // Verify that the DataLimitAction is a value that can be handled.
    if ( (DeltaLimitAction != null) && !DeltaLimitAction.isEmpty() &&
    	!DeltaLimitAction.equalsIgnoreCase("" + this._Keep) &&
    	!DeltaLimitAction.equalsIgnoreCase("" + this._SetMissing) ) {
        message = "The DeltaLimitAction (" + DeltaLimitAction + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the delta limit action as blank, " + this._Keep + " or "
            + this._SetMissing + " (default)." ) );
    }

    if ( (IntervalLimit != null) && !IntervalLimit.isEmpty() ) {
        if ( !TimeInterval.isInterval(IntervalLimit) ) {
            message = "The IntervalLimit value (" + IntervalLimit + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the interval limit as 1Hour, 7Day, etc." ) );
        }
    }

    // Verify that the DataLimitAction is a value that can be handled.
    if ( (IntervalLimitAction != null) && !IntervalLimitAction.isEmpty() &&
    	!IntervalLimitAction.equalsIgnoreCase("" + this._Keep) &&
    	!IntervalLimitAction.equalsIgnoreCase("" + this._SetMissing) ) {
        message = "The IntervalLimitAction (" + IntervalLimitAction + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the delta limit action as blank, " + this._Keep + " or "
            + this._SetMissing + " (default)." ) );
    }

    // Verify that the ResetType is a value that can be handled.
    if ( (ResetType != null) && !ResetType.isEmpty() &&
    	!ResetType.equalsIgnoreCase("" + RTi.TS.ResetType.AUTO) &&
    	!ResetType.equalsIgnoreCase("" + RTi.TS.ResetType.ROLLOVER) &&
    	!ResetType.equalsIgnoreCase("" + RTi.TS.ResetType.UNKNOWN) ) {
        message = "The ResetType (" + ResetType + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the reset type as blank, " + RTi.TS.ResetType.AUTO + ", "
            + RTi.TS.ResetType.ROLLOVER + ", or " + RTi.TS.ResetType.UNKNOWN + " (default)." ) );
    }
    
    // If the reset type is AUTO the trend direction must be known.
    if ( (ResetType != null) && (ResetType.equalsIgnoreCase("" + RTi.TS.ResetType.AUTO)) ) {
    	if ( ExpectedTrend == null ) {
    		message = "The ExpectedTrend must be specified when ResetType=" + RTi.TS.ResetType.AUTO;
    		warning += "\n" + message;
    		status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
    			message, "Specify the reset type."));
    	}

    }

  	// Verify that the AutoResetDatum is empty, Auto, or a number.
   	if ( (AutoResetDatum != null) && !AutoResetDatum.isEmpty() &&
   		!AutoResetDatum.equalsIgnoreCase(this._Auto) && !StringUtil.isDouble(AutoResetDatum) ) {
       	message = "The AutoResetDatum (" + AutoResetDatum + ") is invalid.";
       	warning += "\n" + message;
       	status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
           	message, "Specify the auto reset datum as blank, " + this._Auto + ", or a number (default).") );
   	}

    int resetCount = 0;
    Double resetMin = null;
	if ( (ResetMin != null) && !ResetMin.equals("") ) {
        ++resetCount;
	    if  ( !StringUtil.isDouble(ResetMin) ) {
            message = "The ResetMin value (" + ResetMin + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the reset minimum as a number." ) );
	    }
	    else {
	        resetMin = Double.parseDouble(ResetMin);
	    }
	}
    if ( (ResetMax != null) && !ResetMax.equals("") ) {
        ++resetCount;
        if ( !StringUtil.isDouble(ResetMax) ) {
            message = "The ResetMax value (" + ResetMax + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the reset maximum as a number." ) );
        }
        else if ( (resetMin != null) && Double.parseDouble(ResetMax) <= resetMin.doubleValue() ) {
            message = "The ResetMax value (" + ResetMax + ") is <= ResetMin (" + ResetMin + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the reset maximum > the reset minimum." ) );
        }
    }
    if ( resetCount == 1 ) {
        message = "ResetMin and ResetMax must both be specified or specify neither.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify both or neither ResetMax and ResetMin." ) );
    }
    // Make sure that the trend is specified if the reset values are specified.
    if ( (resetCount == 2) && ((ExpectedTrend == null) || ExpectedTrend.equals("")) ) {
        message = "ResetMin and ResetMax are specified but Trend is not.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify ExpectedTrend when ResetMax and ResetMin are specified." ) );
    }
    
    // Make sure that if ResetMin or ResetMax are set that ResetType=Rollover.
    if ( resetCount > 0 ) {
    	if ( (ResetType == null) || ResetType.isEmpty() ) {
        	message = "ResetMin or ResetMax are specified but ResetType is not Rollover.";
        	warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            	message, "Specify ResetType=Rollover when ResetMax and ResetMin are specified." ) );
    	}
    }

    if ( (ResetProximityLimit != null) && !ResetProximityLimit.isEmpty() ) {
        if ( !StringUtil.isDouble(ResetProximityLimit) ) {
            message = "The ResetProximityLimit value (" + ResetProximityLimit + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the reset proximity limit as a number." ) );
        }
    }

    if ( (ResetProximityInterval != null) && !ResetProximityInterval.isEmpty() ) {
        if ( !StringUtil.isDouble(ResetProximityInterval) ) {
            message = "The ResetProximityInterval (" + ResetProximityInterval + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the reset proximity interval as an interval (e.g., 1Hour)." ) );
        }
    }

	if ( (AnalysisStart != null) && !AnalysisStart.equals("") &&
		!AnalysisStart.equalsIgnoreCase("OutputStart") && !AnalysisStart.equalsIgnoreCase("OutputEnd") &&
		(AnalysisStart.indexOf("${") < 0) ) {
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
	if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") &&
		!AnalysisEnd.equalsIgnoreCase("OutputStart") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") &&
		(AnalysisEnd.indexOf("${") < 0)) {
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

    if ( (Alias == null) || Alias.equals("") ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a time series alias." ) );
    }

    if ( (TableID != null) && !TableID.isEmpty() ) {
        if ( (TableTSIDColumn == null) || TableTSIDColumn.equals("") ) {
            message = "The Table TSID column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid TSID column." ) );
        }
        if ( (TableTSIDFormat == null) || TableTSIDFormat.equals("") ) {
            message = "The TSID format must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid TSID format." ) );
        }
    }

    // Check for invalid parameters.
	List<String> validList = new ArrayList<>(28);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    // General.
    validList.add ( "ExpectedTrend" );
    validList.add ( "ResetMax" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
    validList.add ( "Flag" );
    validList.add ( "Alias" );
    // Delta limit.
    validList.add ( "DeltaLimit" );
    validList.add ( "DeltaLimitAction" );
    validList.add ( "DeltaLimitFlag" );
    validList.add ( "IntervalLimit" );
    validList.add ( "IntervalLimitAction" );
    validList.add ( "IntervalLimitFlag" );
    // ResetType=Auto.
    validList.add ( "AutoResetDatum" );
    // ResetType=Rollover.
    validList.add ( "ResetType" );
    validList.add ( "ResetMin" );
    validList.add ( "ResetProximityLimit" );
    validList.add ( "ResetProximityLimitInterval" );
    validList.add ( "ResetProximityLimitFlag" );
    // Output table.
    validList.add ( "TableID" );
    validList.add ( "TableTSIDColumn" );
    validList.add ( "TableTSIDFormat" );
    validList.add ( "TableDateTimeColumn" );
    validList.add ( "TableValuePreviousColumn" );
    validList.add ( "TableValueColumn" );
    validList.add ( "TableDeltaCalculatedColumn" );
    validList.add ( "TableActionColumn" );
    validList.add ( "TableDeltaColumn" );
    validList.add ( "TableFlagColumn" );
    validList.add ( "TableProblemColumn" );
    // Output properties.
    validList.add ( "ProblemCountProperty" );
    validList.add ( "ProblemCountTimeSeriesProperty" );
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
    List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
	// The command will be modified if changed.
	return (new Delta_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
@return the discovery mode table
*/
private DataTable getDiscoveryTable() {
    return this.__discoveryTable;
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList () {
    return __discoveryTSList;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
	// First check for table request.
    DataTable table = getDiscoveryTable();
    List<T> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new ArrayList<>();
        v.add ( (T)table );
        return v;
    }

    // Check for time series request.
    List<T> matchingDiscoveryTS = new ArrayList<>();
    List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return matchingDiscoveryTS;
    }
    for ( TS datats : discoveryTSList ) {
        // Use the most generic for the base class.
        if ( (c == TS.class) || (c == datats.getClass()) ) {
            matchingDiscoveryTS.add((T)datats);
        }
    }
    return matchingDiscoveryTS;
}

// parseCommand() from the base class is used.

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;  // Level for non-use messages for log file.

	// Make sure there are time series available to operate on.
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // Default.
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
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
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
	
	// General.
    String ExpectedTrend = parameters.getValue ( "ExpectedTrend" );
    TrendType trendType = null;
    if ( (ExpectedTrend != null) && !ExpectedTrend.isEmpty() ) {
        trendType = TrendType.valueOfIgnoreCase(ExpectedTrend);
    }
    String ResetType = parameters.getValue ( "ResetType" );
    ResetType resetType = null;
    if ( (ResetType != null) && !ResetType.isEmpty() ) {
        resetType = RTi.TS.ResetType.valueOfIgnoreCase(ResetType);
    }
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String Flag = parameters.getValue ( "Flag" );
	if ( (Flag != null) && (Flag.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		Flag = TSCommandProcessorUtil.expandParameterValue(processor, this, Flag);
	}
	String Alias = parameters.getValue ( "Alias" );

	// Delta limit.
	String DeltaLimit = parameters.getValue ( "DeltaLimit" );
    Double deltaLimit = null;
    if ( (DeltaLimit != null) && !DeltaLimit.equals("") ) {
        deltaLimit = Double.parseDouble(DeltaLimit);
    }
	String DeltaLimitAction = parameters.getValue ( "DeltaLimitAction" );
	String DeltaLimitFlag = parameters.getValue ( "DeltaLimitFlag" );
	String IntervalLimit = parameters.getValue ( "IntervalLimit" );
    TimeInterval intervalLimit = null;
    if ( (IntervalLimit != null) && !IntervalLimit.equals("") ) {
        intervalLimit = TimeInterval.parseInterval(IntervalLimit);
    }
	String IntervalLimitAction = parameters.getValue ( "IntervalLimitAction" );
	String IntervalLimitFlag = parameters.getValue ( "IntervalLimitFlag" );

	// ResetType=Auto.
	// Treat as a string because can be a number or "Auto".
	String AutoResetDatum = parameters.getValue ( "AutoResetDatum" );

	// ResetType=Rollover.
	String ResetMin = parameters.getValue ( "ResetMin" );
	Double resetMin = null;
	if ( (ResetMin != null) && !ResetMin.equals("") ) {
	    resetMin = Double.parseDouble(ResetMin);
	}
	String ResetMax = parameters.getValue ( "ResetMax" );
    Double resetMax = null;
    if ( (ResetMax != null) && !ResetMax.equals("") ) {
        resetMax = Double.parseDouble(ResetMax);
    }
	String ResetProximityLimit = parameters.getValue ( "ResetProximityLimit" );
    Double resetProximityLimit = null;
    if ( (ResetProximityLimit != null) && !ResetProximityLimit.equals("") ) {
        resetProximityLimit = Double.parseDouble(ResetProximityLimit);
    }
	String ResetProximityLimitInterval = parameters.getValue ( "ResetProximityLimitInterval" );
	TimeInterval resetProximityLimitInterval = null;
	if ( (ResetProximityLimitInterval != null) && !ResetProximityLimitInterval.isEmpty() ) {
		resetProximityLimitInterval = TimeInterval.parseInterval(ResetProximityLimitInterval);
	}
	String ResetProximityLimitFlag = parameters.getValue ( "ResetProximityLimitFlag" );

	// Output table.
    String TableID = parameters.getValue ( "TableID" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
	}
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
	TableTSIDColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableTSIDColumn);
	if ( (TableTSIDColumn == null) || TableTSIDColumn.isEmpty() ) {
		TableTSIDColumn = "TSID";
	}
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TableDateTimeColumn = parameters.getValue ( "TableDateTimeColumn" );
	TableDateTimeColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableDateTimeColumn);
	if ( (TableDateTimeColumn == null) || TableDateTimeColumn.isEmpty() ) {
		TableDateTimeColumn = "DateTime";
	}
    String TableValuePreviousColumn = parameters.getValue ( "TableValuePreviousColumn" );
	TableValuePreviousColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableValuePreviousColumn);
	if ( (TableValuePreviousColumn == null) || TableValuePreviousColumn.isEmpty() ) {
		TableValuePreviousColumn = "ValuePrevious";
	}
    String TableValueColumn = parameters.getValue ( "TableValueColumn" );
	TableValueColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableValueColumn);
	if ( (TableValueColumn == null) || TableValueColumn.isEmpty() ) {
		TableValueColumn = "Value";
	}
    String TableDeltaCalculatedColumn = parameters.getValue ( "TableDeltaCalculatedColumn" );
	TableDeltaCalculatedColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableDeltaCalculatedColumn);
	if ( (TableDeltaCalculatedColumn == null) || TableDeltaCalculatedColumn.isEmpty() ) {
		TableDeltaCalculatedColumn = "DeltaCalculated";
	}
    String TableActionColumn = parameters.getValue ( "TableActionColumn" );
	TableActionColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableActionColumn);
	if ( (TableActionColumn == null) || TableActionColumn.isEmpty() ) {
		TableActionColumn = "Action";
	}
    String TableDeltaColumn = parameters.getValue ( "TableDeltaColumn" );
	TableDeltaColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableDeltaColumn);
	if ( (TableDeltaColumn == null) || TableDeltaColumn.isEmpty() ) {
		TableDeltaColumn = "Delta";
	}
    String TableFlagColumn = parameters.getValue ( "TableFlagColumn" );
	TableFlagColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableFlagColumn);
	if ( (TableFlagColumn == null) || TableFlagColumn.isEmpty() ) {
		TableFlagColumn = "Flag";
	}
    String TableProblemColumn = parameters.getValue ( "TableProblemColumn" );
	TableProblemColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableProblemColumn);
	if ( (TableProblemColumn == null) || TableProblemColumn.isEmpty() ) {
		TableProblemColumn = "Problem";
	}
    
    // Output properties.
    String ProblemCountProperty = parameters.getValue ( "ProblemCountProperty" );
    String ProblemCountTimeSeriesProperty = parameters.getValue ( "ProblemCountTimeSeriesProperty" );
    
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

    // Figure out the dates to use for the analysis.
    // Default of null means to analyze the full period.
    DateTime AnalysisStart_DateTime = null;
    DateTime AnalysisEnd_DateTime = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
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
			// Warning will have been added above...
			++warning_count;
		}
    }

	// Get the time series to process.  Allow TSID to be a pattern or specific time series.

    List<TS> tslist = null;
	if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	    // Get the discovery time series list from all time series above this command.
	    tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
	        (TSCommandProcessor)processor, this, TSList, TSID, null, EnsembleID );
	}
	else if ( commandPhase == CommandPhaseType.RUN ) {
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
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
    	}
        if ( bean == null ) {
            Message.printStatus ( 2, routine, "Bean is null.");
        }
    	PropList bean_PropList = bean.getResultsPropList();
    	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
    	if ( o_TSList == null ) {
    		message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
    		"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
    		Message.printWarning ( log_level,
    		MessageUtil.formatMessageTag(
    		command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
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
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message,
                                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    		}
    	}
	}

	int nts = tslist.size();
	if ( nts == 0 ) {
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	if ( TSID.indexOf("${") < 0 ) {
        		// Only show if properties are not used
	            message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
	                "\", EnsembleID=\"" + EnsembleID + "\".  May be OK if time series are created at run time.";
	            Message.printWarning ( warning_level, MessageUtil.formatMessageTag(
	                command_tag,++warning_count), routine, message );
	            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING, message,
	                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        	}
        }
        else {
			message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
	            "\", EnsembleID=\"" + EnsembleID + "\".";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
	        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
	            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
	}

    // Get the table to process:
	// - may be null if it does not exist

    DataTable table = null;
    boolean doTable = false;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be used as input
    	doTable = true;
    	PropList request_params = new PropList ( "" );
        request_params.set ( "TableID", TableID );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "GetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table != null ) {
            table = (DataTable)o_Table;
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

	TS ts = null;
	Object o_ts = null;
	List<TS> discoverTSList = new ArrayList<>();
    boolean createData = true; // Run the full command.
    try {
    	if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	// Just want time series headers initialized, but not actually do the processing.
        	createData = false;
	
       		if ( doTable ) {
	        	if ( table == null ) {
	            	// Did not find table so is being created in this command.
	            	// Create an empty table and set the ID.
	            	table = new DataTable();
	            	table.setTableID ( TableID );
	            	setDiscoveryTable ( table );
	        	}
       		}
    	}
    	else if ( commandPhase == CommandPhaseType.RUN ) {
        	if ( doTable ) {
	            if ( table == null ) {
	                // Did not find the table above so create it.
	                table = new DataTable( /*columnList*/ );
	                table.setTableID ( TableID );
	                Message.printStatus(2, routine, "Was not able to match existing table \"" + TableID + "\" so created new table.");
	                
	                // Set the table in the processor.
	                PropList request_params = null;
	                request_params = new PropList ( "" );
	                request_params.setUsingObject ( "Table", table );
	                try {
	                    processor.processRequest( "SetTable", request_params);
	                }
	                catch ( Exception e ) {
	                    message = "Error requesting SetTable(Table=...) from processor.";
	                    Message.printWarning(warning_level,
	                            MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                            routine, message );
	                    status.addToLog ( commandPhase,
	                            new CommandLogRecord(CommandStatusType.FAILURE,
	                               message, "Report problem to software support." ) );
	                }
	            }
        	}
    	}
		for ( int its = 0; its < nts; its++ ) {
			// The time series to process, from the list that was returned above.
			o_ts = tslist.get(its);
			if ( o_ts == null ) {
				message = "Time series to process is null.";
				Message.printWarning(warning_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
            	status.addToLog ( commandPhase,
                	new CommandLogRecord(CommandStatusType.FAILURE, message,
                    	"Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
				// Go to next time series.
				continue;
			}
			ts = (TS)o_ts;

		    // Create the delta time series using the utility class.
		    notifyCommandProgressListeners ( its, nts, (float)-1.0, "Creating delta for " +
	            ts.getIdentifier().toStringAliasAndTSID() );
			Message.printStatus ( 2, routine, "Creating delta time series from \"" + ts.getIdentifier()+ "\"." );
			TSUtil_Delta tsu = new TSUtil_Delta ( ts, AnalysisStart_DateTime, AnalysisEnd_DateTime,
			    trendType, resetType,
			    AutoResetDatum,
			    resetMin, resetMax, resetProximityLimit, resetProximityLimitInterval, ResetProximityLimitFlag,
			    Flag, createData,
			    deltaLimit, DeltaLimitAction, DeltaLimitFlag,
			    intervalLimit, IntervalLimitAction, IntervalLimitFlag,
                table, TableTSIDColumn, TableTSIDFormat, TableDateTimeColumn,
                TableValuePreviousColumn, TableValueColumn,
                TableDeltaCalculatedColumn, TableActionColumn, TableDeltaColumn,
                TableFlagColumn, TableProblemColumn );
			TS newts = tsu.delta();
			if ( (Alias != null) && !Alias.equals("") ) {
			    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                    processor, newts, Alias, status, commandPhase);
                newts.setAlias ( alias );
			}

			// Append problems in the low-level code to command status log.
            for ( String problem : tsu.getProblems() ) {
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,problem );
                // No recommendation since it is a user-defined check.
                // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
                status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.WARNING, problem, "" ) );
            }

            // Add the table and time series to the processor.
 
            if ( commandPhase == CommandPhaseType.DISCOVERY ) {
                discoverTSList.add ( newts );
            }
            else if ( commandPhase == CommandPhaseType.RUN ) {
                TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, newts );

                // Set the processor and time series properties.

                int problemCount = 0;
                if ( table != null ) {
            	    problemCount = table.getNumberOfRecords();
                }
	            if ( (ProblemCountProperty != null) && !ProblemCountProperty.isEmpty() ) {
	        	    String propName = TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, ProblemCountProperty, status, commandPhase);
	        	    PropList request_params = new PropList ( "" );
	                request_params.setUsingObject ( "PropertyName", propName );
	                request_params.setUsingObject ( "PropertyValue", new Integer(problemCount) );
	                try {
	                    processor.processRequest( "SetProperty", request_params);
	                }
	                catch ( Exception e ) {
	                    message = "Error requesting SetProperty(Property=\"" + ProblemCountProperty + "\") from processor.";
	                    Message.printWarning(log_level,
	                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                        routine, message );
	                    status.addToLog ( CommandPhaseType.RUN,
	                        new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Report the problem to software support." ) );
	                }
	            }
	            if ( (ProblemCountTimeSeriesProperty != null) && !ProblemCountTimeSeriesProperty.isEmpty() ) {
	        	    String propName = TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, ProblemCountTimeSeriesProperty, status, commandPhase);
	        	    ts.setProperty(propName, new Integer(problemCount));
	            }
            }
		}

		// Set the discovery time series list.

    	if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	// Just want time series headers initialized.
        	setDiscoveryTSList ( discoverTSList );
        	// Discovery table was set at the start of the method.
    	}
    }
	catch ( Exception e ) {
		message = "Unexpected error creating delta time series for \""+ ts.getIdentifier() + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
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
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table ) {
    __discoveryTable = table;
}

/**
Set the list of time series read in discovery phase.
@param discoveryTSList list of time series created in discovery mode
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList ) {
    __discoveryTSList = discoveryTSList;
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
    	// General.
    	"ExpectedTrend",
    	"ResetType",
		"AnalysisStart",
		"AnalysisEnd",
		"Flag",
		"Alias",
    	// Delta Limit.
		"DeltaLimit",
		"DeltaLimitAction",
		"DeltaLimitFlag",
		"IntervalLimit",
		"IntervalLimitAction",
		"IntervalLimitFlag",
		// ResetType=Auto.
		"AutoResetDatum",
		// ResetType=Rollover.
		"ResetMin",
		"ResetMax",
		"ResetProximityLimit",
		"ResetMaxProximityLimitInterval",
		"ResetMaxProximityLimitFlag",
		// Output table.
		"TableID",
		"TableTSIDColumn",
		"TableTSIDFormat",
		"TableDateTimeColumn",
		"TablePreviousValueColumn",
		"TableValueColumn",
		"TableDeltaCalculatedColumn",
		"TableActionColumn",
		"TableDeltaColumn",
		"TableFlagColumn",
		"TableProblemColumn",
		// Output properties.
    	"ProblemCountProperty",
    	"ProblemCountTimeSeriesProperty"
	};
	return this.toString(parameters, parameterOrder);
}

}