package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_NewStatisticMonthTimeSeries;

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
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;

import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the NewStatisticMonthTimeSeries() command.
*/
public class NewStatisticMonthTimeSeries_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;
    
/**
Analysis window year, since users do not supply this information.
This allows for leap year in case the analysis window start or end is on Feb 29.
*/
private final int __ANALYSIS_WINDOW_YEAR = 2000;

/**
Constructor.
*/
public NewStatisticMonthTimeSeries_Command ()
{	super();
	setCommandName ( "NewStatisticMonthTimeSeries" );
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
{	String Alias = parameters.getValue ( "Alias" );
	String TSID = parameters.getValue ( "TSID" );
	String Statistic = parameters.getValue ( "Statistic" );
	String TestValue = parameters.getValue ( "TestValue" );
	String MonthTestValues = parameters.getValue ( "MonthTestValues" );
	String AllowMissingCount = parameters.getValue ( "AllowMissingCount" );
	String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String AnalysisWindowStart = parameters.getValue ( "AnalysisWindowStart" );
	String AnalysisWindowEnd = parameters.getValue ( "AnalysisWindowEnd" );
	String SearchStart = parameters.getValue ( "SearchStart" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (Alias == null) || Alias.equals("") ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an alias." ) );
	}
	if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
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
               TSUtil_NewStatisticMonthTimeSeries.getStatisticChoicesForInterval(TimeInterval.UNKNOWN, null);
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
	// Either TestValue or MonthTestValues are required for some statistics
	if ( TSUtil_NewStatisticMonthTimeSeries.isTestValueNeeded(statisticType) ) {
    	if ( ((TestValue == null) || TestValue.equals("")) && ((MonthTestValues == null) || MonthTestValues.equals("")) ) {
    	    message = "Single or monthly test values must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify single or monthly test value(s)." ) );
    	}
    	else if ( ((TestValue != null) && !TestValue.equals("")) && ((MonthTestValues != null) && !MonthTestValues.equals("")) ) {
            message = "Single or monthly test values must be specified (but not both).";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify single or monthly test value(s)." ) );
        }
    	else {
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
            if ( (MonthTestValues != null) && !MonthTestValues.equals("") ) {
                String [] parts = MonthTestValues.split(",");
                if ( parts.length == 12 ) {
                    for ( int i = 0; i < parts.length; i++ ) {
                        if ( !StringUtil.isDouble(parts[i].trim()) ) {
                            message = "The test value (" + parts[i] + ") is not a number.";
                            warning += "\n" + message;
                            status.addToLog ( CommandPhaseType.INITIALIZATION,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the test value as a number." ) );
                        }
                    }
                }
                else {
                    message = "" + parts.length + " monthly test values (" + MonthTestValues + ") are provided - 12 expected.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the 12 monthly test values." ) );
                }
            }
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

    if ( (AnalysisStart != null) && !AnalysisStart.equals("") &&
        !AnalysisStart.equalsIgnoreCase("OutputStart") &&
        !AnalysisStart.equalsIgnoreCase("OutputEnd") ) {
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
    if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") &&
        !AnalysisEnd.equalsIgnoreCase("OutputStart") &&
        !AnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
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
	List<String> validList = new ArrayList<String>(13);
    validList.add ( "Alias" );
    validList.add ( "TSID" );
    validList.add ( "NewTSID" );
    validList.add ( "Statistic" );
    validList.add ( "TestValue" );
    validList.add ( "MonthTestValues" );
    validList.add ( "AllowMissingCount" );
    validList.add ( "MinimumSampleSize" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
    validList.add ( "AnalysisWindowStart" );
    validList.add ( "AnalysisWindowEnd" );
    validList.add ( "SearchStart" );
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
	return (new NewStatisticMonthTimeSeries_JDialog ( parent, this )).ok();
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
*/
public List getObjectList ( Class c )
{
    List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discoveryTSList;
    }
    else {
        return null;
    }
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
Run the command.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "NewStatisticMonthTimeSeries.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Non-user warning level

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters ();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }

	String Alias = parameters.getValue ( "Alias" );
	String TSID = parameters.getValue ( "TSID" );
	String NewTSID = parameters.getValue ( "NewTSID" );
	String Statistic = parameters.getValue ( "Statistic" );
	TSStatisticType statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
	Double testValue = null; // Default
	String TestValue = parameters.getValue ( "TestValue" );
    if ( (TestValue != null) && !TestValue.equals("") ) {
        testValue = new Double(TestValue);
    }
    Double [] monthTestValues = null;
    String MonthTestValues = parameters.getValue ( "MonthTestValues" );
    if ( (MonthTestValues != null) && !MonthTestValues.equals("") ) {
        String [] parts = MonthTestValues.split(",");
        monthTestValues = new Double[parts.length];
        for ( int i = 0; i < parts.length; i++ ) {
            monthTestValues[i] = Double.parseDouble(parts[i].trim());
        }
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
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String AnalysisWindowStart = parameters.getValue ( "AnalysisWindowStart" );
	String AnalysisWindowEnd = parameters.getValue ( "AnalysisWindowEnd" );
	String SearchStart = parameters.getValue ( "SearchStart" );

	// Figure out the dates to use for the analysis...

	DateTime AnalysisStart_DateTime = null;
	DateTime AnalysisEnd_DateTime = null;
	try {
		if ( (AnalysisStart != null) && !AnalysisStart.equals("") ) {
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", AnalysisStart );
			CommandProcessorRequestResultsBean bean = null;
			try { bean =
				processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting AnalysisStart DateTime(DateTime=" +
				AnalysisStart + ") from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
				throw new InvalidCommandParameterException ( message );
			}

			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for AnalysisStart DateTime(DateTime=" +
				AnalysisStart +	") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify the AnalysisStart information." ) );
				throw new InvalidCommandParameterException ( message );
			}
			else {
			    AnalysisStart_DateTime = (DateTime)prop_contents;
			}
		}
	}
	catch ( Exception e ) {
		message = "AnalysisStart \"" + AnalysisStart + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the AnalysisStart information." ) );
		throw new InvalidCommandParameterException ( message );
	}
	
	try {
		if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") ) {
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", AnalysisEnd );
			CommandProcessorRequestResultsBean bean = null;
			try { bean =
				processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting AnalysisEnd DateTime(DateTime=" +
				AnalysisEnd + ") from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
				throw new InvalidCommandParameterException ( message );
			}

			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for AnalysisEnd DateTime(DateTime=" +
				AnalysisStart +	") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify the AnalysisEnd information." ) );
				throw new InvalidCommandParameterException ( message );
			}
			else {	AnalysisEnd_DateTime = (DateTime)prop_contents;
			}
		}
	}
	catch ( Exception e ) {
		message = "AnalysisEnd \"" + AnalysisEnd + "\" is invalid.";
		Message.printWarning(warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, message );
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the AnalysisEnd information." ) );
		throw new InvalidCommandParameterException ( message );
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
            message = "SearchStart \"" + SearchStart +
                "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            throw new InvalidCommandParameterException ( message );
        }
    }

	// Get the time series to process.  The time series list is searched backwards until the first match...
    TS ts = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        String TSList = "" + TSListType.LAST_MATCHING_TSID;
        List<TS> tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, null );
        if ( (tslist != null) && (tslist.size() > 0) ) {
            ts = tslist.get(0);
        }
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
    	PropList request_params = new PropList ( "" );
    	request_params.set ( "CommandTag", command_tag );
    	request_params.set ( "TSID", TSID );
    	CommandProcessorRequestResultsBean bean = null;
    	try { bean =
    		processor.processRequest( "GetTimeSeriesForTSID", request_params);
    	}
    	catch ( Exception e ) {
    		message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSID +
    		"\") from processor.";
    		Message.printWarning(log_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
    	}
    	PropList bean_PropList = bean.getResultsPropList();
    	Object o_TS = bean_PropList.getContents ( "TS");
    	if ( o_TS == null ) {
    		message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID +
    		"\") from processor.";
    		Message.printWarning(log_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
    	}
    	else {
    		ts = (TS)o_TS;
    	}
    }
	
	if ( ts == null ) {
		message = "Unable to find time series to analyze using TSID \""+TSID + "\".";
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
    boolean createData = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        createData = false;
    }
    else if ( commandPhase == CommandPhaseType.RUN ){
        // Currently only support daily time series as input
        if ( ts.getDataIntervalBase() != TimeInterval.DAY ) {
            message = "Only daily interval for input time series is currently supported (time series is " +
                ts.getIdentifier().getInterval() + ").";
            status.addToLog (commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
            Message.printWarning(3, routine, message );
            throw new CommandException ( message );
        }
    }
	try {
	    // Make sure that the statistic is allowed for the time series interval.
	    if ( !TSUtil_NewStatisticMonthTimeSeries.isStatisticSupported(statisticType, ts.getDataIntervalBase(), null) ) {
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
    	    TSUtil_NewStatisticMonthTimeSeries tsu = new TSUtil_NewStatisticMonthTimeSeries ( ts, NewTSID, statisticType,
    	        testValue, monthTestValues, AllowMissingCount_Integer, MinimumSampleSize_Integer,
    	        AnalysisStart_DateTime, AnalysisEnd_DateTime,
                AnalysisWindowStart_DateTime, AnalysisWindowEnd_DateTime, SearchStart_DateTime );
    		TS stats_ts = tsu.newStatisticMonthTS ( createData );
    		String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                processor, stats_ts, Alias, status, commandPhase);
    		stats_ts.setAlias ( alias ); // Do separate because setting the NewTSID might cause the alias set to fail below.
    
    		// Update the data to the processor so that appropriate actions are taken...
    	    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
    	        // Just want time series headers initialized
    	        List<TS> discoveryTSList = new Vector();
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
{   if ( props == null ) {
        return getCommandName() + "()";
    }
	String Alias = props.getValue( "Alias" );
	String TSID = props.getValue( "TSID" );
	String NewTSID = props.getValue( "NewTSID" );
	String Statistic = props.getValue( "Statistic" );
	String TestValue = props.getValue( "TestValue" );
	String MonthTestValues = props.getValue( "MonthTestValues" );
	String AllowMissingCount = props.getValue( "AllowMissingCount" );
	String MinimumSampleSize = props.getValue( "MinimumSampleSize" );
	String AnalysisStart = props.getValue( "AnalysisStart" );
	String AnalysisEnd = props.getValue( "AnalysisEnd" );
	String AnalysisWindowStart = props.getValue( "AnalysisWindowStart" );
	String AnalysisWindowEnd = props.getValue( "AnalysisWindowEnd" );
	String SearchStart = props.getValue( "SearchStart" );
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewTSID=\"" + NewTSID + "\"" );
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
	if ( (MonthTestValues != null) && (MonthTestValues.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MonthTestValues=\"" + MonthTestValues + "\"");
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
    return getCommandName() + "("+ b.toString()+")";
}

}