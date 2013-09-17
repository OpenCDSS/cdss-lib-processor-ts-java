package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

import RTi.TS.RunningAverageType;
import RTi.TS.TS;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_RunningStatistic;

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

/**
This class initializes, checks, and runs the RunningStatisticTimeSeries() command.
*/
public class RunningStatisticTimeSeries_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public RunningStatisticTimeSeries_Command ()
{	super();
	setCommandName ( "RunningStatisticTimeSeries" );
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
{	//String TSID = parameters.getValue ( "TSID" );
    String Statistic = parameters.getValue ( "Statistic" );
	String SampleMethod = parameters.getValue ( "SampleMethod" );
    String Bracket = parameters.getValue ( "Bracket" );
    String AllowMissingCount = parameters.getValue ( "AllowMissingCount" );
    String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
    //String Alias = parameters.getValue ( "Alias" );
    String ProbabilityUnits = parameters.getValue ( "ProbabilityUnits" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    /* TODO SAM 2008-01-03 Evaluate combination with TSList
	if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide a time series identifier." ) );
	}
    */
    
    if ( (Statistic == null) || Statistic.equals("") ) {
        message = "The statistic must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a Statistic." ) );
    }
    else {
        // Make sure that the statistic is known in general
        boolean supported = false;
        TSStatisticType statisticType = null;
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
            List<TSStatisticType> statistics = TSUtil_RunningStatistic.getStatisticChoices();
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
    
    RunningAverageType averageType = RunningAverageType.valueOfIgnoreCase(SampleMethod);
	if ( averageType == null ) {
        message = "The SampleMethod parameter (" + SampleMethod + ") is invalid.";
        warning += "\n" + message;
        StringBuffer b = new StringBuffer();
        for ( RunningAverageType type : TSUtil_RunningStatistic.getRunningAverageTypeChoices() ) {
            b.append ( "" + type + ", ");
        }
        // Remove the extra comma at end
        b.delete(b.length() - 2, b.length());
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "SampleMethod must be one of: " + b ) );
	}
    if ( (averageType != RunningAverageType.ALL_YEARS) && (averageType != RunningAverageType.N_ALL_YEAR) ) {
        if ( (Bracket == null) || Bracket.equals("") ) {
            message = "The Bracket parameter must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the bracket as an integer." ) );
        }
        else if ( !StringUtil.isInteger(Bracket) ) {
            message = "The Bracket parameter (" + Bracket + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the bracket as an integer." ) );
        }
    }
    
    if ( (AllowMissingCount != null) && !AllowMissingCount.equals("") ) {
        if ( !StringUtil.isInteger(AllowMissingCount) ) {
            message = "The AllowMissingCount value (" + AllowMissingCount + ") is not an integer.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an integer for AllowMissingCount." ) );
        }
        else {
            // Make sure it is an allowable value >= 0...
            int i = Integer.parseInt(AllowMissingCount);
            if ( i < 0 ) {
                message = "The AllowMissingCount value (" + AllowMissingCount + ") must be >= 0.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a value >= 0." ) );
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
                message = "The MinimumSampleSize value (" + MinimumSampleSize + ") must be > 0.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a value > 0." ) );
            }
        }
    }
    
    /*
    if ( (Alias == null) || Alias.equals("") ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a time series alias." ) );
    }
    */
    
    if ( (ProbabilityUnits != null) && !ProbabilityUnits.equals("") && !ProbabilityUnits.equalsIgnoreCase("Fraction") &&
        !ProbabilityUnits.equalsIgnoreCase("Percent") && !ProbabilityUnits.equalsIgnoreCase("%")) {
        message = "The probability units are invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a the probability units as Fraction, Percent, or %." ) );
    }
      
    // Check for invalid parameters...
    List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "Statistic" );
    valid_Vector.add ( "SampleMethod" );
    valid_Vector.add ( "Bracket" );
    valid_Vector.add ( "AllowMissingCount" );
    valid_Vector.add ( "MinimumSampleSize" );
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "ProbabilityUnits" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );
    
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
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new RunningStatisticTimeSeries_JDialog ( parent, this )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
    List<TS> matchingDiscoveryTS = new Vector();
    List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return matchingDiscoveryTS;
    }
    for ( TS datats : discovery_TS_Vector ) {
        // Use the most generic for the base class...
        if ( (c == TS.class) || (c == datats.getClass()) ) {
            matchingDiscoveryTS.add(datats);
        }
    }
    return matchingDiscoveryTS;
}

// parseCommand() in parent class

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
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "RunningStatistic_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;	// Warning level for non-user messages.

	// Make sure there are time series available to operate on...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
    String TSList = parameters.getValue ( "TSList" );
    String TSID = parameters.getValue ( "TSID" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = "" + TSListType.ALL_TS;
    }
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String Statistic = parameters.getValue ( "Statistic" );
    TSStatisticType statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
	String SampleMethod = parameters.getValue ( "SampleMethod" );
	RunningAverageType sampleMethod = RunningAverageType.valueOfIgnoreCase(SampleMethod);
    String Bracket = parameters.getValue ( "Bracket" );
    int Bracket_int = 0;
    if ( (Bracket != null) && Bracket.length() > 0) {
        Bracket_int = Integer.valueOf ( Bracket );
    }
    String AllowMissingCount = parameters.getValue ( "AllowMissingCount" );
    int allowMissingCount = 0;
    if ( (AllowMissingCount != null) && AllowMissingCount.length() > 0) {
        allowMissingCount = Integer.valueOf ( AllowMissingCount );
    }
    String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
    int minimumSampleSize = 0;
    if ( (MinimumSampleSize != null) && MinimumSampleSize.length() > 0) {
        minimumSampleSize = Integer.valueOf ( MinimumSampleSize );
    }
    String Alias = parameters.getValue ( "Alias" );
    String ProbabilityUnits = parameters.getValue ( "ProbabilityUnits" );

    // Get the time series to process.

    List<TS> tslist = null;
    boolean createData = true; // Whether to fill in the data array
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        // FIXME - SAM 2011-02-02 This gets all the time series, not just the ones matching the request!
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
            tslist = (List)o_TSList;
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
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
                "\", EnsembleID=\"" + EnsembleID + "\".  May be OK if time series are not yet created.";
            Message.printWarning ( warning_level, MessageUtil.formatMessageTag(
                command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING, message,
                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
        else {
            message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
            Message.printWarning ( warning_level, MessageUtil.formatMessageTag(
                command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
    }
    
    if ( warning_count > 0 ) {
        // Input error (e.g., missing time series)...
        message = "Insufficient data to run command.";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        // It is OK if no time series.
    }

	// Now process the time series...

	TS ts = null; // Time series to process
    TS newts = null; // Running statistic time series
    List<TS> discoveryTSList = new Vector();
	for ( int its = 0; its < nts; its++ ) {
	    ts = tslist.get(its);
		try {
            // Do the processing...
		    notifyCommandProgressListeners ( its, nts, (float)-1.0, "Running statistic for " +
	            ts.getIdentifier().toStringAliasAndTSID() );
			Message.printStatus ( 2, routine, "Calculating running statistic for: \"" + ts.getIdentifier() + "\"." );
			TSUtil_RunningStatistic tsu =
			    new TSUtil_RunningStatistic(ts, Bracket_int, statisticType, sampleMethod, allowMissingCount,
			        minimumSampleSize, ProbabilityUnits );
			newts = tsu.runningStatistic(createData);
			if ( (Alias != null) && !Alias.equals("") ) {
                String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                    processor, newts, Alias, status, commandPhase);
                newts.setAlias ( alias );
            }
	        // Append problems in the low-level code to command status log
            for ( String problem : tsu.getProblems() ) {
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,problem );
                // No recommendation since it is a user-defined check
                // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
                status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.WARNING,
                    problem, "" ) );
            }
            if ( commandPhase == CommandPhaseType.DISCOVERY ) {
                discoveryTSList.add ( newts );
            }
            else if ( commandPhase == CommandPhaseType.RUN ) {
                TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, newts );
            }  
		}
		catch ( Exception e ) {
			message = "Unexpected error calculating running statistic for time series \"" + ts.getIdentifier() +
			    "\" (" + e + ").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count),routine,message );
			Message.printWarning(3,routine,e);
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
		}
	}
		
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Just want time series headers initialized
        setDiscoveryTSList ( discoveryTSList );
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
*/
private void setDiscoveryTSList ( List<TS> discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TSList = props.getValue( "TSList" );
    String TSID = props.getValue( "TSID" );
    String EnsembleID = props.getValue( "EnsembleID" );
    String Statistic = props.getValue("Statistic");
	String SampleMethod = props.getValue("SampleMethod");
	String Bracket = props.getValue("Bracket");
	String AllowMissingCount = props.getValue("AllowMissingCount");
	String MinimumSampleSize = props.getValue("MinimumSampleSize");
	String Alias = props.getValue("Alias");
	String ProbabilityUnits = props.getValue("ProbabilityUnits");
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
	if ( (SampleMethod != null) && (SampleMethod.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SampleMethod=" + SampleMethod );
	}
	if ( (Bracket != null) && (Bracket.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Bracket=" + Bracket );
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
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }
    if ( (ProbabilityUnits != null) && (ProbabilityUnits.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ProbabilityUnits=\"" + ProbabilityUnits + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}