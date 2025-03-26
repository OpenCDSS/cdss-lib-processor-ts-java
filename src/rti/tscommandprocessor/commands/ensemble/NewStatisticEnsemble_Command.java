// NewStatisticEnsemble_Command - This class initializes, checks, and runs the NewStatisticYearTS() command.

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

package rti.tscommandprocessor.commands.ensemble;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_NewStatisticEnsemble;
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
This class initializes, checks, and runs the NewStatisticYearTS() command.
*/
public class NewStatisticEnsemble_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
Ensemble created in discovery mode (basically to get the identifier for other commands).
*/
private TSEnsemble __discoveryTSEnsemble = null;
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Test values as doubles, parsed when command is checked.
*/
private double [] __testValueArray;
    
/**
Constructor.
*/
public NewStatisticEnsemble_Command ()
{	super();
	setCommandName ( "NewStatisticEnsemble" );
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
	String NewTSID = parameters.getValue ( "NewTSID" );
	String Statistic = parameters.getValue ( "Statistic" );
	String TestValue = parameters.getValue ( "TestValue" );
	String AllowMissingCount = parameters.getValue ( "AllowMissingCount" );
	String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
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
	if ( (NewTSID == null) || NewTSID.equals("") ) {
        message = "The new time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a new time series identifier." ) );
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
	if ( (TestValue != null) && !TestValue.equals("") ) {
		// If a test value is specified, for now make sure that each value is a number.
	    // It is possible that in the future it could be a
		// special value (date, etc.) but for now focus on numbers.
	    String [] testValues = TestValue.split(",");
	    __testValueArray = new double[testValues.length];
	    for ( int i = 0; i < testValues.length; i++ ) {
    		if ( !StringUtil.isDouble(testValues[i]) ) {
                message = "The test value (" + testValues[i] + ") is not a number.";
    			warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the test value(s) as number(s) separated by commas." ) );
    		}
    		else {
    		    __testValueArray[i] = Double.parseDouble(testValues[i]);
    		}
	    }
	}
	else {
	    // Test value not specified...
        if ( (statisticType != null) && TSUtil_NewStatisticEnsemble.isTestValueNeeded(statisticType)) {
            message = "The test value is required for the " + statisticType + " statistic.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the test value(s) as number(s) separated by commas." ) ); 
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
   
    // Check for invalid parameters...
	List<String> validList = new ArrayList<String>();
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "NewEnsembleID" );
    validList.add ( "NewEnsembleName" );
    validList.add ( "Alias" );
    validList.add ( "NewTSID" );
    validList.add ( "Statistic" );
    validList.add ( "TestValue" );
    validList.add ( "AllowMissingCount" );
    validList.add ( "MinimumSampleSize" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
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
	return (new NewStatisticEnsemble_JDialog ( parent, this )).ok();
}

// TODO SAM 2012-07-17 Evaluate whether this can be generalized for all commands
// Issue, how to know when a command parameter is a comma-separated value list or a single value, etc.
/**
Expand a string to recognize processor and command property ${Property} strings.
If a property string is not found, it will remain as originally specified without being replaced.
@param processor The processor that is being used.
@param ts Time series to be used for metadata string.
@param s String to expand.  The string can contain % format specifiers used with TS.
@param status CommandStatus to add messages to if problems occur.
@param commandPhase command phase (for logging)
*/
private String expandMetadataString ( CommandProcessor processor, Command command, TS ts, int its, String s,
        CommandStatus status, CommandPhaseType commandPhase )
{   String routine = getClass().getName() + ".expandMetadataString";
    if ( s == null ) {
        return "";
    }
    // Replace ${Property} strings with properties from the processor
    int start = 0;
    int pos2 = 0;
    while ( pos2 < s.length() ) {
        int pos1 = s.indexOf( "${", start );
        if ( pos1 >= 0 ) {
            // Find the end of the property
            pos2 = s.indexOf( "}", pos1 );
            if ( pos2 > 0 ) {
                // Try getting the property from the command...
                // TODO SAM 2012-07-17 Evaluate making this more generic so as to not hard-code in this command
                String propname = s.substring(pos1+2,pos2);
                String propvalString = "";
                if ( propname.equalsIgnoreCase("c:Statistic") ) {
                    propvalString = command.getCommandParameters().getValue("Statistic");
                }
                else if ( propname.equalsIgnoreCase("c:TestValue") ) {
                    propvalString = command.getCommandParameters().getValue("TestValue").split(",")[its].trim();
                }
                if ( propvalString == null ) {
                    // Property not yet matched so try getting the property from the processor...
                    PropList request_params = new PropList ( "" );
                    request_params.setUsingObject ( "PropertyName", propname );
                    CommandProcessorRequestResultsBean bean = null;
                    try {
                        bean = processor.processRequest( "GetProperty", request_params);
                    }
                    catch ( Exception e ) {
                        String message = "Error requesting GetProperty(Property=\"" + propname + "\") from processor.";
                        Message.printWarning ( 3,routine, message );
                        if ( status != null ) {
                            status.addToLog ( commandPhase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                        }
                        start = pos2;
                        continue;
                    }
                    if ( bean == null ) {
                        String message =
                            "Unable to find property from processor using GetProperty(Property=\"" + propname + "\").";
                        Message.printWarning ( 3,routine, message );
                        if ( status != null ) {
                            status.addToLog ( commandPhase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                    message,
                                    "Verify that the property name is valid - must match case." ) );
                        }
                        start = pos2;
                        continue;
                    }
                    PropList bean_PropList = bean.getResultsPropList();
                    Object o_PropertyValue = bean_PropList.getContents ( "PropertyValue" );
                    if ( o_PropertyValue == null ) {
                        String message =
                            "Null PropertyValue returned from processor for GetProperty(PropertyName=\"" + propname + "\").";
                        Message.printWarning ( 3, routine, message );
                        if ( status != null ) {
                            status.addToLog ( commandPhase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                    message,
                                    "Verify that the property name is valid - must match case." ) );
                        }
                        start = pos2;
                        continue;
                    }
                    else {
                        // Matched the property so replace the value
                        propvalString = o_PropertyValue.toString();
                        start = pos2;
                    }
                }
                // Replace the string and continue to evaluate s2
                s = s.substring ( 0, pos1 ) + propvalString + s.substring (pos2 + 1);
                // No need to reposition the start because ${ is searched for and above logic will march on
            }
            else {
                // No closing character so march on...
                start = pos1 + 2;
                if ( start > s.length() ) {
                    break;
                }
            }
        }
        else {
            // Done processing properties.
            break;
        }
    }
    return s;
}

/**
Return the ensemble that is read by this class when run in discovery mode.
*/
private TSEnsemble getDiscoveryTSEnsemble()
{
    return __discoveryTSEnsemble;
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Return a list of objects of the requested type.
Classes that can be requested:  TS, TSEnsenble
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{   TSEnsemble tsensemble = getDiscoveryTSEnsemble();
    List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (tsensemble != null) && (c == tsensemble.getClass()) ) {
    	List<T> v = new Vector<T>();
        v.add ( (T)tsensemble );
    }
    else if ( (discoveryTSList != null) && (discoveryTSList.size() != 0) ) {
        // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
        TS datats = discoveryTSList.get(0);
        if ((c == TS.class) || (c == datats.getClass()) ) {
            return (List<T>)discoveryTSList;
        }
    }
    return null;
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "NewStatisticEnsemble.runCommand", message;
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

    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = "" + TSListType.ALL_TS; // Default is process all time series
    }
    String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String NewEnsembleID = parameters.getValue("NewEnsembleID");
    String NewEnsembleName = parameters.getValue("NewEnsembleName");
    if ( NewEnsembleName == null ) {
        NewEnsembleName = ""; // default
    }
	String Alias = parameters.getValue ( "Alias" );
	String NewTSID = parameters.getValue ( "NewTSID" );
	String Statistic = parameters.getValue ( "Statistic" );
	TSStatisticType statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
	String AllowMissingCount = parameters.getValue ( "AllowMissingCount" );
	Integer AllowMissingCount_Integer = Integer.valueOf(-1); // Default
	if ( StringUtil.isInteger(AllowMissingCount) ) {
	    AllowMissingCount_Integer = Integer.valueOf(AllowMissingCount);
	}
	Integer MinimumSampleSize_Integer = null; // Default - no minimum
	String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
    if ( StringUtil.isInteger(MinimumSampleSize) ) {
        MinimumSampleSize_Integer = Integer.valueOf(MinimumSampleSize);
    }
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	
    boolean createData = true;
    List<TS> tslist = null; // Time series to add to the new ensemble
    int nts = 0;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        // FIXME - SAM 2011-02-02 This gets all the time series, not just the ones matching the request!
        tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, EnsembleID );
        createData = false;
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        // Get the time series to process...
        
        if ( (TSList != null) && !TSList.equals("") ) {
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
            if ( tslist != null ) {
                nts = tslist.size();
            }
            if ( nts == 0 ) {
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
    }

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
	
    // Make sure that the statistic is allowed for the time series interval.
    if ( !TSUtil_NewStatisticEnsemble.isStatisticSupported(statisticType) ) {
        message = "Statistic \"" + statisticType + "\" is not supported.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count),routine,message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Refer to documentation to determine supported statistics." ) );
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

	try {
	    TSUtil_NewStatisticEnsemble tsu = new TSUtil_NewStatisticEnsemble ( tslist, NewEnsembleID, NewEnsembleName,
	        Alias, NewTSID, statisticType,
	        __testValueArray, AllowMissingCount_Integer, MinimumSampleSize_Integer,
	        AnalysisStart_DateTime, AnalysisEnd_DateTime );
		TSEnsemble statsEnsemble = tsu.newStatisticEnsemble ( createData );
		// Set the alias for each time series...
		List<TS> statsEnsembleList = statsEnsemble.getTimeSeriesList(false);
		for ( int its = 0; its < statsEnsembleList.size(); its++ ) {
		    TS ts = statsEnsembleList.get(its);
    		String alias = expandMetadataString(processor, this, ts, its, Alias, status, commandPhase);
    		ts.setAlias ( alias );
		}

		// Update the data to the processor so that appropriate actions are taken...
	    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	        if ( (NewEnsembleID != null) && !NewEnsembleID.equals("") ) {
	            setDiscoveryTSEnsemble ( statsEnsemble );
	        }
	        // Just want time series headers initialized
	        setDiscoveryTSList ( statsEnsemble.getTimeSeriesList(false) );
	    }
	    else if ( commandPhase == CommandPhaseType.RUN ) {
	        if ( (NewEnsembleID != null) && !NewEnsembleID.equals("") ) {
	            TSCommandProcessorUtil.appendEnsembleToResultsEnsembleList(processor, this, statsEnsemble);
	        }
	        TSCommandProcessorUtil.appendTimeSeriesListToResultsList(
	            processor, this, statsEnsemble.getTimeSeriesList(false));
	    }
	}
	catch ( Exception e ) {
		message ="Unexpected error generating the statistic ensemble (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,message, "See the log file for details." ) );
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
private void setDiscoveryTSEnsemble ( TSEnsemble tsensemble )
{
    __discoveryTSEnsemble = tsensemble;
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
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TSList",
    	"TSID",
    	"EnsembleID",
    	"NewEnsembleID",
    	"NewEnsembleName",
		"Alias",
		"NewTSID",
		"Statistic",
		"TestValue",
		"AllowMissingCount",
		"MinimumSampleSize",
		"AnalysisStart",
		"AnalysisEnd",
	};
	return this.toString(parameters, parameterOrder);
}

}