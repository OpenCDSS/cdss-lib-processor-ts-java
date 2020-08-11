// NewStatisticYearTS_Command - This class initializes, checks, and runs the NewStatisticYearTS() command.

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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_NewStatisticYearTS;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandSavesMultipleVersions;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.YearType;

// TODO SAM 2016-02-27 Need to phase out TestValue in favor of Value1 after some period, introduced in TSTool 11.09.00
/**
This class initializes, checks, and runs the NewStatisticYearTS() command.
*/
public class NewStatisticYearTS_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
TSEnsemble created in discovery mode (basically to get the identifier for other commands).
*/
private TSEnsemble __discoveryEnsemble = null;
    
/**
Analysis window year, since users do not supply this information.
This allows for leap year in case the analysis window start or end is on Feb 29.
*/
private final int __ANALYSIS_WINDOW_YEAR = 2000;

/**
Constructor.
*/
public NewStatisticYearTS_Command ()
{	super();
	setCommandName ( "NewStatisticYearTS" );
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
{	String Alias = parameters.getValue ( "Alias" );
	String TSID = parameters.getValue ( "TSID" );
	String EnsembleID = parameters.getValue ( "EnsembleID" );
	String Statistic = parameters.getValue ( "Statistic" );
	String TestValue = parameters.getValue ( "TestValue" );
	String Value1 = parameters.getValue ( "Value1" );
	String AllowMissingCount = parameters.getValue ( "AllowMissingCount" );
	String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
	String OutputYearType = parameters.getValue ( "OutputYearType" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String AnalysisWindowStart = parameters.getValue ( "AnalysisWindowStart" );
	String AnalysisWindowEnd = parameters.getValue ( "AnalysisWindowEnd" );
	String SearchStart = parameters.getValue ( "SearchStart" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (Alias == null) || Alias.isEmpty() ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an alias." ) );
	}
	/* Default is all available time series
	 * TODO smalers 2020-08-04 remove once used for awhile
	if ( ((TSID == null) || TSID.isEmpty()) && ((EnsembleID == null) || EnsembleID.isEmpty()) ) {
        message = "The time series identifier or ensemble identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series or ensemble identifier." ) );
	}
	*/
	if ( ((TSID != null) && !TSID.isEmpty()) && ((EnsembleID != null) && !EnsembleID.isEmpty()) ) {
        message = "The time series identifier and ensemble identifier cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series or ensemble identifier." ) );
	}
	// TODO SAM 2005-08-29
	// Need to decide whether to check NewTSID - it might need to support wildcards.
	
    TSStatisticType statisticType = null;
	if ( (Statistic == null) || Statistic.equals("") ) {
        message = "The statistic must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a statistic." ) );
	}
	else {
        // Make sure that the statistic is known in general
        boolean supported = false;
        try {
            statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
            supported = true;
        }
        catch ( Exception e ) {
            message = "The statistic (" + Statistic + ") is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported statistic using the command editor." ) );
        }
        
        // Make sure that it is in the supported list
        
        if ( supported ) {
            supported = false;
            List<TSStatisticType> statistics =
               TSUtil_NewStatisticYearTS.getStatisticChoicesForInterval(TimeInterval.UNKNOWN, null);
            for ( int i = 0; i < statistics.size(); i++ ) {
                if ( statisticType == statistics.get(i) ) {
                    supported = true;
                }
            }
            if ( !supported ) {
                message = "The statistic (" + Statistic + ") is not supported by this command.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Select a supported statistic using the command editor." ) );
            }
        }
	}
	// TODO SAM 2016-02-27 phase out TestValue in favor of Value1 at some point, introduced in TSTool 11.09.00
	if ( (TestValue != null) && !TestValue.equals("") ) {
		// If a test value is specified, for now make sure it is a
		// number.  It is possible that in the future it could be a
		// special value (date, etc.) but for now focus on numbers.
		if ( !StringUtil.isDouble(TestValue) ) {
            message = "The test value (" + TestValue + ") is not a number.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the test value as a number." ) );
		}
	}
	else {
	    // Test value not specified...
        if ( (statisticType != null) && TSUtil_NewStatisticYearTS.isTestValueNeeded(statisticType)) {
            message = "The test value is required for the " + statisticType + " statistic.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the test value as a number." ) ); 
        }
	}
	
	if ( (Value1 != null) && !Value1.equals("") ) {
		// If a test value is specified, for now make sure it is a
		// number.  It is possible that in the future it could be a
		// special value (date, etc.) but for now focus on numbers.
		if ( !StringUtil.isDouble(Value1) ) {
            message = "Value1 (" + Value1 + ") is not a number.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify Value1 as a number." ) );
		}
	}
	else {
	    // Value1 not specified...
        if ( (statisticType != null) && TSUtil_NewStatisticYearTS.isTestValueNeeded(statisticType)) {
            message = "Value1 is required for the " + statisticType + " statistic.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the Value1 as a number." ) ); 
        }
	}
	
	if ( (AllowMissingCount != null) && !AllowMissingCount.equals("") ) {
		if ( !StringUtil.isInteger(AllowMissingCount) ) {
            message = "The AllowMissingCount value (" + AllowMissingCount + ") is not an integer.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the AllowMissingCount value as an integer." ) );
		}
		else {
            // Make sure it is an allowable value >= 0...
			if ( StringUtil.atoi(AllowMissingCount) < 0 ) {
                message = "The AllowMissingCount value (" + AllowMissingCount + ") must be >= 0.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the AllowMissingCount value as an integer >= 0." ) );
			}
		}
	}
	
    if ( (MinimumSampleSize != null) && !MinimumSampleSize.equals("") ) {
        if ( !StringUtil.isInteger(MinimumSampleSize) ) {
            message = "The MinimumSampleSize value (" + MinimumSampleSize + ") is not an integer.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an integer for MinimumSampleSize." ) );
        }
        else {
            // Make sure it is an allowable value >= 0...
            int i = Integer.parseInt(MinimumSampleSize);
            if ( i <= 0 ) {
                message = "The MinimumSampleSize value (" + MinimumSampleSize + ") must be >= 1.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a value >= 1." ) );
            }
        }
    }

    if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
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
    
    if ( (AnalysisStart != null) && !AnalysisStart.isEmpty() && !AnalysisStart.startsWith("${") &&
        !AnalysisStart.equalsIgnoreCase("OutputStart") && !AnalysisStart.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse(AnalysisStart);
        }
        catch ( Exception e ) {
            message = "The analysis start date \"" + AnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        }
    }
    if ( (AnalysisEnd != null) && !AnalysisEnd.isEmpty() && !AnalysisEnd.startsWith("${") &&
        !AnalysisEnd.equalsIgnoreCase("OutputStart") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse( AnalysisEnd );
        }
        catch ( Exception e ) {
            message = "The analysis end date \"" + AnalysisEnd + "\" is not a valid date.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        }
    }

	if ( (AnalysisWindowStart != null) && !AnalysisWindowStart.equals("") ) {
	    String analysisWindowStart = "" + __ANALYSIS_WINDOW_YEAR + "-" + AnalysisWindowStart;
		try {
		    DateTime.parse( analysisWindowStart );
		}
		catch ( Exception e ) {
            message = "The analysis window start \"" + AnalysisWindowStart + "\" (prepended with " +
            __ANALYSIS_WINDOW_YEAR + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time using MM, MM-DD, MM-DD hh, or MM-DD hh:mm." ) );
		}
	}
	
    if ( (AnalysisWindowEnd != null) && !AnalysisWindowEnd.equals("") ) {
        String analysisWindowEnd = "" + __ANALYSIS_WINDOW_YEAR + "-" + AnalysisWindowEnd;
        try {
            DateTime.parse( analysisWindowEnd );
        }
        catch ( Exception e ) {
            message = "The analysis window end \"" + AnalysisWindowEnd + "\" (prepended with " +
            __ANALYSIS_WINDOW_YEAR + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time using MM, MM-DD, MM-DD hh, or MM-DD hh:mm." ) );
        }
    }
    
    if ( (SearchStart != null) && !SearchStart.equals("") ) {
        String searchStart = "" + __ANALYSIS_WINDOW_YEAR + "-" + SearchStart;
        try {
            DateTime.parse( searchStart );
        }
        catch ( Exception e ) {
            message = "The search start date \"" + SearchStart + "\" (prepended with " +
            __ANALYSIS_WINDOW_YEAR + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time using MM, MM-DD, MM-DD hh, or MM-DD hh:mm." ) );
        }
    }
    
    // Check for invalid parameters...
	List<String> validList = new ArrayList<String>(18);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "Statistic" );
    validList.add ( "TestValue" );
    validList.add ( "Value1" );
    validList.add ( "AllowMissingCount" );
    validList.add ( "MinimumSampleSize" );
    validList.add ( "OutputYearType" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
    validList.add ( "AnalysisWindowStart" );
    validList.add ( "AnalysisWindowEnd" );
    validList.add ( "SearchStart" );
    validList.add ( "Alias" );
    validList.add ( "NewTSID" );
    validList.add ( "NewEnsembleID" );
    validList.add ( "NewEnsembleName" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );
    
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new NewStatisticYearTS_JDialog ( parent, this )).ok();
}

/**
Return the ensemble that is read by this class when run in discovery mode.
*/
private TSEnsemble getDiscoveryEnsemble()
{
    return __discoveryEnsemble;
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Return the list of data objects created by this object in discovery mode.
Classes that can be requested:  TS, TSEnsemble
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
    TSEnsemble tsensemble = getDiscoveryEnsemble();
    List<TS> discoveryTSList = getDiscoveryTSList ();

    // TODO SAM 2011-03-31 Does the following work as intended?
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
            return null;
        }
        else {
            return (List<T>)discoveryTSList;
        }
    }
    else if ( (tsensemble != null) && (c == tsensemble.getClass()) ) {
        List<T> v = new ArrayList<T>();
        v.add ( (T)tsensemble );
        return v;
    }
    else {
        return null;
    }
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = getClass().getSimpleName() + ".parseCommand", message;

    if ( !command.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation
        super.parseCommand(command);
        // If TestValue is set but Value1 is not, set Value1 to same value as TestValue for transition to new syntax
        // Also leave TestValue because it may be needed by old versions of the software
        String Value1 = getCommandParameters().getValue("Value1");
        String TestValue = getCommandParameters().getValue("TestValue");
        if ( ((TestValue != null) && !TestValue.isEmpty()) && ((Value1 == null) || Value1.isEmpty()) ) {
        	getCommandParameters().set("Value1",TestValue);
        }
        // If TSID is specified but not TSList, it is legacy behavior
        String TSList = getCommandParameters().getValue("TSList");
        String TSID = getCommandParameters().getValue("TSID");
        if ( ((TSID != null) && !TSID.isEmpty()) && ((TSList == null) || TSList.isEmpty()) ) {
        	getCommandParameters().set("TSList","" + TSListType.LAST_MATCHING_TSID);
        }
    }
    else {
    	// Get the part of the command after the TS Alias =...
    	int pos = command.indexOf ( "=" );
    	if ( pos < 0 ) {
    		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = NewStatisticYearTS(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	String token0 = command.substring ( 0, pos ).trim();
    	String token1 = command.substring ( pos + 1 ).trim();
    	if ( (token0 == null) || (token1 == null) ) {
    		message = "Syntax error in \"" + command + "\".  Expecting:  TS Alias = NewStatisticYearTS(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    
    	List<String> v = StringUtil.breakStringList ( token0, " ", StringUtil.DELIM_SKIP_BLANKS );
    	if ( (v == null) || (v.size() != 2) ) {
    		message = "Syntax error in \"" + command +
    			"\".  Expecting:  TS Alias = NewStatisticYearTS(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	String Alias = v.get(1);
    	List<String> tokens = StringUtil.breakStringList ( token1, "()", 0 );
    	if ( (tokens == null) || tokens.size() < 2 ) {
    		// Must have at least the command name and its parameters...
    		message = "Syntax error in \"" + command + "\".  Not enough tokens.";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	// Get the input needed to process the file...
    	try {
    	    PropList parameters = PropList.parse ( Prop.SET_FROM_PERSISTENT, (String)tokens.get(1), routine, "," );
    		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
    		parameters.set ( "Alias", Alias );
    		parameters.setHowSet ( Prop.SET_UNKNOWN );
    		setCommandParameters ( parameters );
    	}
    	catch ( Exception e ) {
    		message = "Syntax error in \"" + command + "\".  Not enough tokens.";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
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
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Non-user warning level

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters ();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
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
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
    	setDiscoveryEnsemble ( null );
        setDiscoveryTSList ( null );
    }

	String Alias = parameters.getValue ( "Alias" ); // Expansion handled below
	String TSList = parameters.getValue ( "TSList" );
	if ( (TSList == null) || TSList.isEmpty() ) {
		TSList = "" + TSListType.ALL_TS; // Default
	}
	String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
	String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
	String Statistic = parameters.getValue ( "Statistic" );
	TSStatisticType statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
	// Legacy is TestValue but Value1 is being phased in
	Double value1 = null; // Default
	String TestValue = parameters.getValue ( "TestValue" );
    if ( StringUtil.isDouble(TestValue) ) {
        value1 = new Double(TestValue);
    }
	String Value1 = parameters.getValue ( "Value1" );
    if ( StringUtil.isDouble(Value1) ) {
        value1 = new Double(Value1);
    }
	String AllowMissingCount = parameters.getValue ( "AllowMissingCount" );
	Integer AllowMissingCount_Integer = new Integer(-1); // Default
	if ( StringUtil.isInteger(AllowMissingCount) ) {
	    AllowMissingCount_Integer = new Integer(AllowMissingCount);
	}
	Integer MinimumSampleSize_Integer = null; // Default - no minimum
	String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
    if ( StringUtil.isInteger(MinimumSampleSize) ) {
        MinimumSampleSize_Integer = new Integer(MinimumSampleSize);
    }
    String OutputYearType = parameters.getValue ( "OutputYearType" );
    YearType outputYearType = YearType.CALENDAR;
    if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
        outputYearType = YearType.valueOfIgnoreCase(OutputYearType);
    }
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String AnalysisWindowStart = parameters.getValue ( "AnalysisWindowStart" );
	String AnalysisWindowEnd = parameters.getValue ( "AnalysisWindowEnd" );
	String SearchStart = parameters.getValue ( "SearchStart" );
	String NewTSID = parameters.getValue ( "NewTSID" );
	if ( (NewTSID != null) && (NewTSID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		NewTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, NewTSID); // Also expand for time series properties
	}
	String NewEnsembleID = parameters.getValue ( "NewEnsembleID" );
	if ( (NewEnsembleID != null) && (NewEnsembleID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		NewEnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, NewEnsembleID);
	}
	String NewEnsembleName = parameters.getValue ( "NewEnsembleName" );
	if ( (NewEnsembleName != null) && (NewEnsembleName.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		NewEnsembleName = TSCommandProcessorUtil.expandParameterValue(processor, this, NewEnsembleName);
	}

	// Figure out the dates to use for the analysis...

	DateTime AnalysisStart_DateTime = null;
	DateTime AnalysisEnd_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			AnalysisStart_DateTime = TSCommandProcessorUtil.getDateTime ( AnalysisStart, "AnalysisStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
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
		
	DateTime AnalysisWindowStart_DateTime = null;
	if ( (AnalysisWindowStart != null) && (AnalysisWindowStart.length() > 0) ) {
		try {
		    // The following works with ISO formats...
		    AnalysisWindowStart_DateTime =
		        DateTime.parse ( "" + __ANALYSIS_WINDOW_YEAR + "-" + AnalysisWindowStart );
		}
		catch ( Exception e ) {
			message = "AnalysisWindowStart \"" + AnalysisWindowStart +
			    "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
			throw new InvalidCommandParameterException ( message );
		}
	}
    DateTime AnalysisWindowEnd_DateTime = null;
    if ( (AnalysisWindowEnd != null) && (AnalysisWindowEnd.length() > 0) ) {
        try {
            // The following works with ISO formats...
            AnalysisWindowEnd_DateTime =
                DateTime.parse ( "" + __ANALYSIS_WINDOW_YEAR + "-" + AnalysisWindowEnd );
        }
        catch ( Exception e ) {
            message = "AnalysisWindowEnd \"" + AnalysisWindowEnd +
                "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            throw new InvalidCommandParameterException ( message );
        }
    }
    DateTime SearchStart_DateTime = null;
    if ( (SearchStart != null) && (SearchStart.length() > 0) ) {
        try {
            // The following works with ISO formats...
            SearchStart_DateTime = DateTime.parse ( "" + __ANALYSIS_WINDOW_YEAR + "-" + SearchStart );
        }
        catch ( Exception e ) {
            message = "SearchStart \"" + SearchStart + "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            throw new InvalidCommandParameterException ( message );
        }
    }

	// Get the time series or ensemble to process.  The time series list is searched backwards until the first match...
    List<TS> tsList = new ArrayList<TS>();
    boolean createData = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        tsList = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
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
            Message.printWarning(log_level, routine, e);
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
        }
        Object o_TSList = null;
        if ( bean == null ) {
            Message.printStatus ( 2, routine, "Bean is null.");
        }
        else {
        	PropList bean_PropList = bean.getResultsPropList();
        	o_TSList = bean_PropList.getContents ( "TSToProcessList" );
        }
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
			List<TS> tsList0 = (List<TS>)o_TSList;
            tsList = tsList0;
            if ( tsList.size() == 0 ) {
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
	
	if ( tsList == null ) {
		message = "Unable to find time series to analyze using TSID \"" + TSID + "\" and EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
		throw new CommandWarningException ( message );
	}
	
    if ( warning_count > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog (commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

	// Now process the time series...
    List<TS> resultList = new ArrayList<TS>();
	for ( TS ts : tsList ) {
		try {
		    // Make sure that the statistic is allowed for the time series interval.
		    if ( !TSUtil_NewStatisticYearTS.isStatisticSupported(statisticType, ts.getDataIntervalBase(), null) ) {
		        message = "Statistic \"" + statisticType + "\" is not supported the interval for \"" +
	            ts.getIdentifier() + "\".";
	            Message.printWarning ( warning_level,
	                MessageUtil.formatMessageTag(
	                command_tag,++warning_count),routine,message );
	            status.addToLog ( commandPhase,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Refer to documentation to determine supported statistics." ) );
		    }
		    else {
	    	    TSUtil_NewStatisticYearTS tsu = new TSUtil_NewStatisticYearTS ( ts, NewTSID, statisticType,
	    	        value1, AllowMissingCount_Integer, MinimumSampleSize_Integer,
	    	        outputYearType, AnalysisStart_DateTime, AnalysisEnd_DateTime,
	                AnalysisWindowStart_DateTime, AnalysisWindowEnd_DateTime, SearchStart_DateTime );
	    		TS stats_ts = tsu.newStatisticYearTS ( createData );
	    		// Set alias separately because setting the NewTSID might cause the alias set to fail below.
	            if ( (Alias != null) && !Alias.isEmpty() ) {
	                String alias = Alias;
	                if ( commandPhase == CommandPhaseType.RUN ) {
	                	alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, stats_ts, Alias, status, commandPhase);
	                }
	                stats_ts.setAlias ( alias );
	            }
	            // Add the output time series to an array to add to the ensemble output
	            resultList.add(stats_ts);
	    
	    		// Update the data to the processor so that appropriate actions are taken...
	    	    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	    	        // Just want time series headers initialized
	    	        List<TS> discoveryTSList = new ArrayList<TS>(1);
	    	        discoveryTSList.add ( stats_ts );
	    	        setDiscoveryTSList ( discoveryTSList );
	    	    }
	    	    else if ( commandPhase == CommandPhaseType.RUN ) {
	    	        TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, stats_ts);
	    	    }
		    }
		}
		catch ( Exception e ) {
			message ="Unexpected error generating the statistic time series from \""+
				ts.getIdentifier() + "\" (" + e + ").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count),routine,message );
			Message.printWarning(3,routine,e);
	        status.addToLog ( commandPhase,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "See the log file for details." ) );
		}
	}
	
    // If processing an ensemble, create the new ensemble
    
    if ( (NewEnsembleID != null) && !NewEnsembleID.isEmpty() ) {
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create a discovery ensemble with ID and name
            TSEnsemble ensemble = new TSEnsemble ( NewEnsembleID, NewEnsembleName, resultList );
            setDiscoveryEnsemble ( ensemble );
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
            // Add the ensemble to the processor if created
            TSEnsemble ensemble = new TSEnsemble ( NewEnsembleID, NewEnsembleName, resultList );
            TSCommandProcessorUtil.appendEnsembleToResultsEnsembleList(processor, this, ensemble);
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
Set the ensemble that is processed by this class in discovery mode.
*/
private void setDiscoveryEnsemble ( TSEnsemble tsensemble )
{
    __discoveryEnsemble = tsensemble;
}

/**
Set the list of time series read in discovery phase.
@param discoveryTSList list of time series created during discovery phase
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props )
{
    return toString ( props, 10 );
}

/**
Return the string representation of the command.
@param props parameters for the command
@param majorVersion the major version for software - if less than 10, the "TS Alias = " notation is used,
allowing command files to be saved for older software.
*/
public String toString ( PropList props, int majorVersion )
{   if ( props == null ) {
        if ( majorVersion < 10 ) {
            return "TS Alias = " + getCommandName() + "()";
        }
        else {
            return getCommandName() + "()";
        }
    }
	String TSList = props.getValue( "TSList" );
	String TSID = props.getValue( "TSID" );
	String EnsembleID = props.getValue( "EnsembleID" );
	String Statistic = props.getValue( "Statistic" );
	String TestValue = props.getValue( "TestValue" );
	String Value1 = props.getValue( "Value1" );
	String AllowMissingCount = props.getValue( "AllowMissingCount" );
	String MinimumSampleSize = props.getValue( "MinimumSampleSize" );
	String OutputYearType = props.getValue( "OutputYearType" );
	String AnalysisStart = props.getValue( "AnalysisStart" );
	String AnalysisEnd = props.getValue( "AnalysisEnd" );
	String AnalysisWindowStart = props.getValue( "AnalysisWindowStart" );
	String AnalysisWindowEnd = props.getValue( "AnalysisWindowEnd" );
	String SearchStart = props.getValue( "SearchStart" );
	String Alias = props.getValue( "Alias" );
	String NewTSID = props.getValue( "NewTSID" );
	String NewEnsembleID = props.getValue( "NewEnsembleID" );
	String NewEnsembleName = props.getValue( "NewEnsembleName" );
	StringBuffer b = new StringBuffer ();
	if ( (TSList != null) && (TSList.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSList=" + TSList );
	}
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
	}
	if ( (Statistic != null) && (Statistic.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Statistic=" + Statistic );
	}
	if ( (TestValue != null) && (TestValue.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TestValue=" + TestValue );
	}
	if ( (Value1 != null) && (Value1.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Value1=" + Value1 );
	}
	if ( (AllowMissingCount != null) && (AllowMissingCount.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AllowMissingCount=" + AllowMissingCount );
	}
    if ( (MinimumSampleSize != null) && (MinimumSampleSize.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MinimumSampleSize=" + MinimumSampleSize );
    }
    if ( (OutputYearType != null) && (OutputYearType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputYearType=" + OutputYearType );
    }
	if ( (AnalysisStart != null) && (AnalysisStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AnalysisStart=\"" + AnalysisStart + "\"" );
	}
	if ( (AnalysisEnd != null) && (AnalysisEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AnalysisEnd=\"" + AnalysisEnd + "\"" );
	}
    if ( (AnalysisWindowStart != null) && (AnalysisWindowStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AnalysisWindowStart=\"" + AnalysisWindowStart + "\"" );
    }
    if ( (AnalysisWindowEnd != null) && (AnalysisWindowEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AnalysisWindowEnd=\"" + AnalysisWindowEnd + "\"" );
    }
    if ( (SearchStart != null) && (SearchStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SearchStart=\"" + SearchStart + "\"" );
    }
    if ( majorVersion >= 10 ) {
        // Add as a parameter
        if ( (Alias != null) && (Alias.length() > 0) ) {
            if ( b.length() > 0 ) {
                b.append ( "," );
            }
            b.append ( "Alias=\"" + Alias + "\"" );
        }
    }
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewTSID=\"" + NewTSID + "\"" );
	}
	if ( (NewEnsembleID != null) && (NewEnsembleID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewEnsembleID=\"" + NewEnsembleID + "\"" );
	}
	if ( (NewEnsembleName != null) && (NewEnsembleName.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewEnsembleName=\"" + NewEnsembleName + "\"" );
	}
    if ( majorVersion < 10 ) {
        // Old syntax...
        if ( (Alias == null) || Alias.equals("") ) {
            Alias = "Alias";
        }
        return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
    }
    else {
        return getCommandName() + "("+ b.toString()+")";
    }
}

}
