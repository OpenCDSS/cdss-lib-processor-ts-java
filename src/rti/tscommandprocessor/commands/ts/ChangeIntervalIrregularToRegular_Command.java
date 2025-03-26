// ChangeIntervalIrregularToRegular_Command - class for ChangeIntervalIrregularToRegular command

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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.TS.IrregularTS;
import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSLimits;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_ChangeInterval;
import RTi.TS.TSUtil_ChangeIntervalIrregularToRegular;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
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
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.YearType;

/**
This command initializes and runs the ChangeIntervalIrregularToRegular() command.
*/
public class ChangeIntervalIrregularToRegular_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{

	/**
	 * RecalcLimits values.
	 */
	//protected final String _False = "False";
	//protected final String _True = "True";


/**
TSEnsemble created in discovery mode (basically to get the identifier for other commands).
*/
private TSEnsemble discoveryEnsemble = null;

/**
List of time series read during discovery.  These are TS objects but with mainly the metadata (TSIdent) filled in.
*/
private List<TS> discoveryTSList = null;

/**
Command constructor.
*/
public ChangeIntervalIrregularToRegular_Command () {
	super();
	setCommandName ( "ChangeIntervalIrregularToRegular" );
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
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// Get the properties from the PropList parameters.
	String Alias = parameters.getValue( "Alias" );
	String TSID = parameters.getValue( "TSID" );
	String EnsembleID = parameters.getValue( "EnsembleID" );
	String NewEnsembleID = parameters.getValue( "NewEnsembleID" );
	String NewInterval = parameters.getValue( "NewInterval" );
    String Statistic = parameters.getValue ( "Statistic" );
    String PersistInterval = parameters.getValue ( "PersistInterval" );
    String PersistValue = parameters.getValue ( "PersistValue" );
    String SequenceValues = parameters.getValue ( "SequenceValues" );
	String OutputYearType = parameters.getValue ( "OutputYearType" );
	String ScaleValue = parameters.getValue ( "ScaleValue" );
	//String RecalcLimits = parameters.getValue ( "RecalcLimits" );

	// Alias must be specified - for historical command syntax (and generally a good idea).
	if ( (Alias == null) || Alias.isEmpty() ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series alias when defining the command."));
	}

	// Check if the alias for the new time series is the same as the alias used by one of the time series in memory.
	// If so print a warning.
	// TODO [LT 2005-05-26] This is used in all other command but it is not working here.  Why?	Temporarily using the alternative below.
	/*	Vector tsids = (Vector) getCommandProcessor().getPropContents ( "TSIDListNoInput" );
	if ( StringUtil.indexOf( tsids, Alias ) >= 0 ) {
		warning += "\nTime series alias \"" + Alias + "\" is already used above.";
	}
	 */
	// Check if the alias for the new time series is the same as the alias used by the original time series.
	// If so print a warning.
	// TODO [LT 2005-05-26] Would this alternative be more appropriated?
	// Notice: The version above is the one used by the others commands.
	if ( (Alias != null) && (TSID != null) && TSID.equalsIgnoreCase( Alias ) ) {
        message = "The alias \"" + Alias
        + "\" for the new time series is equal to the alias of the original time series.";
		warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify different time series for input and output."));
	}

	// Verify that a NewEnsembleID is specified only if an input EnsembleID is specified.
	if ( (NewEnsembleID != null) && !NewEnsembleID.isEmpty() && ((EnsembleID == null) || EnsembleID.isEmpty())) {
        message = "The NewEnsembleID can only be specified when the input time series are specified using EnsembleID.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Specify input as an ensemble or clear the NewEnsembleID."));
	}

	if ( (NewInterval == null) || NewInterval.isEmpty() ) {
		message = "The new interval must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a new interval."));
	}
	else {
	    try {
	        TimeInterval.parseInterval(NewInterval);
	    }
	    catch ( Exception e ) {
	        // Should not happen because choices are valid.
	        message = "The new interval \"" + NewInterval + "\" is invalid.";
	        warning += "\n" + message;
	        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a new interval using the command editor."));
	    }
	}

    if ( (Statistic != null) && !Statistic.isEmpty() ) {
        // Make sure that the statistic is known in general.
        if ( TSStatisticType.valueOfIgnoreCase(Statistic) == null ) {
            message = "The statistic (" + Statistic + ") is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported statistic using the command editor." ) );
        }

        // Make sure that the statistic is in the supported list for the command.

        if ( TSStatisticType.valueOfIgnoreCase(TSUtil_ChangeInterval.getStatisticChoices(), Statistic) == null ) {
            message = "The statistic (" + Statistic + ") is not supported by this command.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported statistic using the command editor." ) );
        }
    }

    if ( (PersistInterval != null) && !PersistInterval.isEmpty() ) {
    	try {
    		TimeInterval.parseInterval(PersistInterval);
    	}
    	catch (Exception e) {
    		message = "The PersistInterval (" + PersistInterval + ") is invalid.";
    		warning += "\n" + message;
    		status.addToLog ( CommandPhaseType.INITIALIZATION,
    			new CommandLogRecord(CommandStatusType.FAILURE,
    				message, "Specify a valid interval (e.g., 1Hour, Day).") );
    	}
    }

    if ( (PersistValue != null) && !PersistValue.isEmpty() ) {
    	try {
    		Double.parseDouble(PersistValue);
    	}
    	catch (Exception e) {
    		message = "The PersistValue (" + PersistValue + ") is not a number.";
    		warning += "\n" + message;
    		status.addToLog ( CommandPhaseType.INITIALIZATION,
    			new CommandLogRecord(CommandStatusType.FAILURE,
    				message, "Specify a number or leave blank.") );
    	}
    }

    if ( (SequenceValues != null) && !SequenceValues.isEmpty() ) {
    	// All values must be numbers.
    	List<String> values = StringUtil.breakStringList(SequenceValues, ",", StringUtil.DELIM_TRIM_STRINGS);
    	for ( String value : values ) {
    		if ( !StringUtil.isDouble(value) ) {
    			message = "The SequenceValue (" + value + ") is not a number.";
    			warning += "\n" + message;
    			status.addToLog ( CommandPhaseType.INITIALIZATION,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					message, "Specify a number or leave blank.") );
    		}
    	}
    }

    if ( (OutputYearType != null) && !OutputYearType.isEmpty() ) {
        try {
            YearType.valueOfIgnoreCase(OutputYearType);
        }
        catch ( Exception e ) {
            message = "The output year type (" + OutputYearType + ") is invalid.";
            warning += "\n" + message;
            StringBuffer b = new StringBuffer();
            List<YearType> values = YearType.getYearTypeChoices();
            for ( YearType t : values ) {
                if ( b.length() > 0 ) {
                    b.append ( ", " );
                }
                b.append ( t.toString() );
            }
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Valid values are:  " + b.toString() + "."));
        }
    }

    if ( (ScaleValue != null) && !ScaleValue.isEmpty() ) {
    	try {
    		Double.parseDouble(ScaleValue);
    	}
    	catch (Exception e) {
    		message = "The ScaleValue (" + ScaleValue + ") is invalid.";
    		warning += "\n" + message;
    		status.addToLog ( CommandPhaseType.INITIALIZATION,
    			new CommandLogRecord(CommandStatusType.FAILURE,
    				message, "Specify a number or leave blank.") );
    	}
    }

    /*
    if ( (RecalcLimits != null) && !RecalcLimits.isEmpty() &&
        !RecalcLimits.equalsIgnoreCase( _True ) && !RecalcLimits.equalsIgnoreCase(_False) ) {
        message = "The RecalcLimits parameter must be blank, " + _False + " (default), or " + _True + ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a RecalcLimits as " + _False + " or " + _True + ".") );
    }
    */

    // Check for invalid parameters.
	List<String> validList = new ArrayList<>(18);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "Statistic" );
    validList.add ( "Flag" );
    validList.add ( "FlagDescription" );
    // Analysis - persist
    validList.add ( "PersistInterval" );
    validList.add ( "PersistValue" );
    validList.add ( "PersistFlag" );
    validList.add ( "PersistFlagDescription" );
    // Analysis - sequence
    validList.add ( "SequenceValues" );
    validList.add ( "Alias" );
    validList.add ( "NewInterval" );
    validList.add ( "OutputYearType" );
    validList.add ( "NewDataType" );
    validList.add ( "NewUnits" );
    validList.add ( "ScaleValue" );
    //validList.add ( "RecalcLimits" );
    validList.add ( "NewEnsembleID" );
    validList.add ( "NewEnsembleName" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, warning_level ), warning );
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
	return ( new ChangeIntervalIrregularToRegular_JDialog ( parent, this ) ).ok();
}

/**
Return the ensemble that is read by this class when run in discovery mode.
@return the ensemble that is read by this class when run in discovery mode.
*/
private TSEnsemble getDiscoveryEnsemble() {
    return this.discoveryEnsemble;
}

/**
Return the list of time series read in discovery phase.
@return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList () {
    return this.discoveryTSList;
}

/**
Return the list of data objects created by this object in discovery mode.
@return the list of data objects created by this object in discovery mode.
*/
@SuppressWarnings("unchecked")
public <T> List<T>  getObjectList ( Class<T>  c ) {
    TSEnsemble tsensemble = getDiscoveryEnsemble();
    List<TS> discoveryTSList = getDiscoveryTSList ();

    // TODO SAM 2011-03-31 Does the following work as intended?
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS).
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class.
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
            return null;
        }
        else {
            return (List<T>)discoveryTSList;
        }
    }
    else if ( (tsensemble != null) && (c == tsensemble.getClass()) ) {
        List<T> v = new ArrayList<>();
        v.add ( (T)tsensemble );
        return v;
    }
    else {
        return null;
    }
}

// Use the parseCommand method in parent class.

/**
Calls TSCommandProcessor to re-calculate limits for this time series.
@param ts Time Series.
@param TSCmdProc CommandProcessor that is using this command.
@param warningLevel Warning level used for displaying warnings.
@param warning_count Number of warnings found.
@param command_tag Reference or identifier for this command.
 */
private int recalculateLimits( TS ts, CommandProcessor TSCmdProc,
    int warningLevel, int warning_count, String command_tag ) {
    String routine = getClass().getSimpleName() + ".recalculateLimits", message;
    CommandStatus status = getCommandStatus();
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
    // Get the calculated limits and set in the original data limits.
    PropList bean_PropList = bean.getResultsPropList();
    Object prop_contents = bean_PropList.getContents ( "TSLimits" );
    if ( prop_contents == null ) {
        message = "Null value for TSLimits from CalculateTSAverageLimits(" + ts.getIdentifierString() + ")";
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommandInternal";
	String message = "";
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Warning message level for non-user messages.

    CommandStatus status = getCommandStatus();
    TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();
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
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
        setDiscoveryEnsemble ( null );
    }

	PropList parameters = getCommandParameters();
    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = "" + TSListType.ALL_TS;
    }
    String TSID = parameters.getValue ( "TSID" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}

    String Statistic = parameters.getValue ( "Statistic" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		Statistic = TSCommandProcessorUtil.expandParameterValue(processor, this, Statistic);
	}
    TSStatisticType statisticType = null;
    if ( (Statistic != null) && (Statistic.length() > 0) ) {
        statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
    }
    String Flag = parameters.getValue ( "Flag" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		Flag = TSCommandProcessorUtil.expandParameterValue(processor, this, Flag);
	}
    String FlagDescription = parameters.getValue ( "FlagDescription" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		FlagDescription = TSCommandProcessorUtil.expandParameterValue(processor, this, FlagDescription);
	}
    String PersistInterval = parameters.getValue ( "PersistInterval" );
    TimeInterval persistInterval = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		PersistInterval = TSCommandProcessorUtil.expandParameterValue(processor, this, PersistInterval);
	}
    if ( (PersistInterval != null) && !PersistInterval.isEmpty() ) {
        persistInterval = TimeInterval.parseInterval(PersistInterval);
    }
    String PersistValue = parameters.getValue ( "PersistValue" );
    Double persistValue = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		PersistValue = TSCommandProcessorUtil.expandParameterValue(processor, this, PersistValue);
	}
    if ( (PersistValue != null) && !PersistValue.isEmpty() ) {
        persistValue = Double.valueOf(PersistValue);
    }
    String PersistFlag = parameters.getValue ( "PersistFlag" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		PersistFlag = TSCommandProcessorUtil.expandParameterValue(processor, this, PersistFlag);
	}
    String PersistFlagDescription = parameters.getValue ( "PersistFlagDescription" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		PersistFlagDescription = TSCommandProcessorUtil.expandParameterValue(processor, this, PersistFlagDescription);
	}
    String SequenceValues = parameters.getValue ( "SequenceValues" );
    double [] sequenceValues = new double[0];
	if ( commandPhase == CommandPhaseType.RUN ) {
		SequenceValues = TSCommandProcessorUtil.expandParameterValue(processor, this, SequenceValues);
		int parseFlag = 0;
		StringUtil.parseDoubleSequenceArray(SequenceValues, ",", parseFlag);
	}

    String Alias = parameters.getValue ( "Alias" );
	String NewInterval = parameters.getValue( "NewInterval"  );
	TimeInterval newInterval = TimeInterval.parseInterval(NewInterval);
	String OutputYearType = parameters.getValue( "OutputYearType" );
	YearType outputYearType = YearType.CALENDAR;
	if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
	    outputYearType = YearType.valueOfIgnoreCase(OutputYearType);
	}
	String NewDataType = parameters.getValue( "NewDataType" );
	String NewUnits = parameters.getValue( "NewUnits" );
    String ScaleValue = parameters.getValue ( "ScaleValue" );
    Double scaleValue = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		ScaleValue = TSCommandProcessorUtil.expandParameterValue(processor, this, ScaleValue);
	}
    if ( (ScaleValue != null) && !ScaleValue.isEmpty() ) {
        scaleValue = Double.valueOf(ScaleValue);
    }
	/*
	String RecalcLimits = parameters.getValue ( "RecalcLimits" );
    boolean recalcLimits = false; // Default.
    if ( (RecalcLimits != null) && RecalcLimits.equalsIgnoreCase("true") ) {
        recalcLimits = true;
    }
    */
    String NewEnsembleID = parameters.getValue( "NewEnsembleID" );
    String NewEnsembleName = parameters.getValue( "NewEnsembleName" );

    // Get the time series to process.

    List<TS> tslist = null;
    boolean createData = true; // Whether to fill in the data array.
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command.
        // FIXME - SAM 2011-02-02 This gets all the time series, not just the ones matching the request.
        tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, EnsembleID );
        createData = false;
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
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message,
                            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
        else {
            @SuppressWarnings("unchecked")
			List<TS> dataList = (List<TS>)o_TSList;
            tslist = dataList;
            if ( tslist.size() == 0 ) {
                message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
                Message.printWarning ( log_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE, message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
            }
        }
    }

    int nts = tslist.size();
    if ( nts == 0 ) {
        message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	// Error is OK, especially with dynamic content.
        }
        else {
	        Message.printWarning ( warning_level,
	        MessageUtil.formatMessageTag(
	        command_tag,++warning_count), routine, message );
	        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
	            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
    }

    if ( warning_count > 0 ) {
        // Input error.
        message = "Insufficient data to run command.";
        status.addToLog ( commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

	// If here, have enough input to attempt the changing the interval.
    IrregularTS originalTS = null; // Original (input) time series.
	TS resultTS = null; // Result time series.
	List<TS> resultList = new ArrayList<TS>();
    for ( int its = 0; its < nts; its++ ) {
        TS ts = tslist.get(its);
        if ( !(ts instanceof IrregularTS) ) {
        	message = "Input time series \"" + ts.getIdentifierString() + "\" does not have irregular interval.";
        	status.addToLog ( commandPhase,
        	new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        	Message.printWarning(3, routine, message );
        	continue;
        }
        originalTS = (IrregularTS)ts;
        notifyCommandProgressListeners ( its, nts, (float)-1.0, "Changing interval for " +
            originalTS.getIdentifier().toStringAliasAndTSID() );
    	try {
    		// Process the change of interval.
    	    TSUtil_ChangeIntervalIrregularToRegular tsu = new TSUtil_ChangeIntervalIrregularToRegular (
    	    	originalTS,
                statisticType,
                Flag,
                FlagDescription,
                persistInterval,
                persistValue,
                PersistFlag,
                PersistFlagDescription,
                sequenceValues,
                // Output.
                newInterval,
                outputYearType,
                NewDataType,
                NewUnits,
                scaleValue );
    		resultTS = tsu.changeInterval ( createData );
    		//if ( (commandPhase == CommandPhaseType.RUN) && recalcLimits ) {
    		if ( commandPhase == CommandPhaseType.RUN ) {
    		    warning_count = recalculateLimits( resultTS, processor, warning_level, warning_count, command_tag );
    		}
    		resultList.add(resultTS);

    		// Update the newly created time series alias (alias is required).
            if ( (Alias != null) && !Alias.equals("") ) {
                String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString ( processor, resultTS, Alias, status, commandPhase );
                resultTS.setAlias ( alias );
            }

    		// Add the newly created time series to the software memory.
    	    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
    	        // Just want time series headers initialized
    	        setDiscoveryTSList ( resultList ); // OK to reset each time.
    	    }
    	    if ( commandPhase == CommandPhaseType.RUN ) {
    	        // Add single time series.
    	        TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, resultTS );
    	    }
    	}
        catch ( IllegalArgumentException e ) {
            message = "Error changing the interval for TSID=\"" + TSID + "\" (" + e + ").";
    		Message.printWarning(warning_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
    		Message.printWarning ( log_level, routine, e );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Consult documentation for available parameter combinations." ) );
            throw new CommandWarningException ( message );
        }
    	catch ( Exception e ) {
    		message = "Unexpected error changing the interval for TSID=\"" + TSID + "\" (" + e + ").";
    		Message.printWarning(warning_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
    		Message.printWarning ( log_level, routine, e );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
    		throw new CommandWarningException ( message );
    	}
    }

    // If processing an ensemble, create the new ensemble.

    if ( (NewEnsembleID != null) && !NewEnsembleID.equals("") ) {
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create a discovery ensemble with ID and name.
            TSEnsemble ensemble = new TSEnsemble ( NewEnsembleID, NewEnsembleName, resultList );
            setDiscoveryEnsemble ( ensemble );
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
            // Add the ensemble to the processor if created.
            TSEnsemble ensemble = new TSEnsemble ( NewEnsembleID, NewEnsembleName, resultList );
            TSCommandProcessorUtil.appendEnsembleToResultsEnsembleList(processor, this, ensemble);
        }
    }

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ), routine, message );
		throw new CommandWarningException ( message );
	}
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the ensemble that is processed by this class in discovery mode.
*/
private void setDiscoveryEnsemble ( TSEnsemble tsensemble ) {
    this.discoveryEnsemble = tsensemble;
}

/**
Set the list of time series read in discovery phase.
@param discoveryTSList list of time series created during discovery phase
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList ) {
    this.discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		// Input.
    	"TSList",
    	"TSID",
    	"EnsembleID",
    	// Analysis (Larger).
		"Statistic",
		"Flag",
		"FlagDescription",
		"PersistInterval",
		"PersistValue",
		"PersistFlag",
		"PersistFlagDescription",
		"SequenceValues",
		// Output.
		"NewTSID",
		"Alias",
		"NewInterval",
		"OutputYearType",
		//"NewDataType",
		"NewUnits",
		"ScaleValue",
		//"RecalcLimits",
		// Output (Ensemble).
		"NewEnsembleID",
		"NewEnsembleName"
	};
	return this.toString(parameters, parameterOrder);
}

}