package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import RTi.TS.RunningAverageType;
import RTi.TS.TS;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_RunningStatistic;
import RTi.Util.Math.DistributionType;
import RTi.Util.Math.SortOrderType;
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
import RTi.Util.String.StringDictionary;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

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
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String SampleMethod = parameters.getValue ( "SampleMethod" );
    String Bracket = parameters.getValue ( "Bracket" );
    String BracketByMonth = parameters.getValue ( "BracketByMonth" );
    String CustomBracketByMonth = parameters.getValue ( "CustomBracketByMonth" );
    if ( BracketByMonth == null ) {
        BracketByMonth = ""; // To simplify checks below
    }
    String AllowMissingCount = parameters.getValue ( "AllowMissingCount" );
    String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
    //String Alias = parameters.getValue ( "Alias" );
    String Distribution = parameters.getValue ( "Distribution" );
    String ProbabilityUnits = parameters.getValue ( "ProbabilityUnits" );
    String SortOrder = parameters.getValue ( "SortOrder" );
    String NormalStart = parameters.getValue ( "NormalStart" );
    String NormalEnd = parameters.getValue ( "NormalEnd" );
    String OutputStart = parameters.getValue ( "OutputStart" );
    String OutputEnd = parameters.getValue ( "OutputEnd" );
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
    
    if ( (Distribution != null) && !Distribution.equals("") ) {
        List<String> distChoices = TSUtil_RunningStatistic.getDistributionChoicesAsStrings();
        boolean found = false;
        for ( String d : distChoices ) {
            if ( d.equalsIgnoreCase(Distribution) ) {
                found = true;
                break;
            }
        }
        if ( !found ) {
            message = "The distribution (" + Distribution + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a distribution as one of the choices in the command editor." ) );
        }
    }
    
    if ( (ProbabilityUnits != null) && !ProbabilityUnits.equals("") && !ProbabilityUnits.equalsIgnoreCase("Fraction") &&
        !ProbabilityUnits.equalsIgnoreCase("Percent") && !ProbabilityUnits.equalsIgnoreCase("%")) {
        message = "The probability units (" + ProbabilityUnits + ") are invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a the probability units as Fraction, Percent, or %." ) );
    }
    
    if ( (SortOrder != null) && !SortOrder.equals("") && !SortOrder.equalsIgnoreCase(""+SortOrderType.LOW_TO_HIGH) &&
        !SortOrder.equalsIgnoreCase(""+SortOrderType.HIGH_TO_LOW) ) {
        message = "The sort order (" + SortOrder + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the sort order as " + SortOrderType.LOW_TO_HIGH + " or " + SortOrderType.HIGH_TO_LOW ) );
    }
    
    if ( (AnalysisStart != null) && !AnalysisStart.equals("") &&
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
    if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") &&
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
    
    RunningAverageType sampleMethod = RunningAverageType.valueOfIgnoreCase(SampleMethod);
	if ( sampleMethod == null ) {
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
    if ( (sampleMethod != RunningAverageType.ALL_YEARS) && (sampleMethod != RunningAverageType.N_ALL_YEAR) ) {
        if ( ((Bracket == null) || Bracket.isEmpty()) && ((BracketByMonth == null) || BracketByMonth.isEmpty()) ) {
            message = "The Bracket or BracketByMonth parameter must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the bracket as an integer or bracket by month as 12 integers." ) );
        }
        if ( ((Bracket != null) && !Bracket.isEmpty()) && ((BracketByMonth != null) && !BracketByMonth.isEmpty()) ) {
            message = "Bracket OR BracketByMonth parameter must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the bracket as an integer OR bracket by month as 12 integers." ) );
        }
        if ( (Bracket != null) && !Bracket.isEmpty() && !StringUtil.isInteger(Bracket) ) {
            message = "The Bracket parameter (" + Bracket + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the bracket as an integer." ) );
        }
        if ( (BracketByMonth != null) && !BracketByMonth.isEmpty() ) {
            List<String> v = StringUtil.breakStringList ( BracketByMonth,",", 0 );
        	// breakStringList will not add value if delimiter at end so count commas
            if ( v == null ) {
                message = "12 bracket values must be specified (have " + v.size() + ").";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify 12 bracket values separated by commas." ) );
            }
            else {
            	 if ( v.size() != 12 ) {
            		 // Add empty values at end
            		 for ( int i = v.size(); i < 12; i++ ) {
            			 v.add("");
            		 }
            	 }
            }
            if ( v != null ) {
                String val;
                for ( int i = 0; i < 12; i++ ) {
                	// Values can be missing - will indicate to NOT compute the statistic for the month
                    val = v.get(i).trim();
                    if ( !val.isEmpty() && !StringUtil.isInteger(val) ) {
                        message = "Monthly bracket value for month " + (i + 1) + " \"" + val + "\" is not an integer.";
                        warning += "\n" + message;
                        status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                 message, "Specify 12 bracket values separated by commas." ) );
                    }
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
    
    DateTime NormalStart_DateTime = null;
    if ( (NormalStart != null) && !NormalStart.equals("") &&
        !NormalStart.equalsIgnoreCase("OutputStart") && !NormalStart.equalsIgnoreCase("OutputEnd") ) {
        try {
            NormalStart_DateTime = DateTime.parse(NormalStart);
        }
        catch ( Exception e ) {
            message = "The normal start date/time \"" + NormalStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time." ) );
        }
    }
    DateTime NormalEnd_DateTime = null;
    if ( (NormalEnd != null) && !NormalEnd.equals("") &&
        !NormalEnd.equalsIgnoreCase("OutputStart") && !NormalEnd.equalsIgnoreCase("OutputEnd") ) {
        try {
            NormalEnd_DateTime = DateTime.parse( NormalEnd );
        }
        catch ( Exception e ) {
            message = "The normal end date/time \"" + NormalEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time." ) );
        }
    }
    // Normal period causes results in statistic over all years
    if ( (NormalStart_DateTime != null) && (NormalEnd_DateTime != null) ) {
        if ( sampleMethod != RunningAverageType.ALL_YEARS ) {
            message = "If the normal period is specified, the sample method must be " + RunningAverageType.ALL_YEARS;
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the sample method as " + RunningAverageType.ALL_YEARS ) );
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
    
    if ( (OutputStart != null) && !OutputStart.equals("") &&
        !OutputStart.equalsIgnoreCase("OutputStart") && !OutputStart.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse(OutputStart);
        }
        catch ( Exception e ) {
            message = "The analysis start date/time \"" + OutputStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time, OutputStart, or output end." ) );
        }
    }
    if ( (OutputEnd != null) && !OutputEnd.equals("") &&
        !OutputEnd.equalsIgnoreCase("OutputStart") && !OutputEnd.equalsIgnoreCase("OutputEnd") ) {
        try {
            DateTime.parse( OutputEnd );
        }
        catch ( Exception e ) {
            message = "The analysis end date/time \"" + OutputEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        }
    }
      
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(20);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "Statistic" );
    validList.add ( "Distribution" );
    validList.add ( "DistributionParameters" );
    validList.add ( "ProbabilityUnits" );
    validList.add ( "SortOrder" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
    validList.add ( "SampleMethod" );
    validList.add ( "Bracket" );
    validList.add ( "BracketByMonth" );
    validList.add ( "CustomBracketByMonth" );
    validList.add ( "AllowMissingCount" );
    validList.add ( "MinimumSampleSize" );
    validList.add ( "NormalStart" );
    validList.add ( "NormalEnd" );
    validList.add ( "Alias" );
    validList.add ( "OutputStart" );
    validList.add ( "OutputEnd" );
    validList.add ( "Properties" );
    validList.add ( "CopyProperties" );
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
    String Distribution = parameters.getValue ( "Distribution" );
    if ( (Distribution == null) || Distribution.equals("") ) {
        Distribution = "" + DistributionType.WEIBULL;
    }
    DistributionType distributionType = DistributionType.valueOfIgnoreCase(Distribution);
    String DistributionParameters = parameters.getValue ( "DistributionParameters" );
    Hashtable distParams = null;
    if ( (DistributionParameters != null) && (DistributionParameters.length() > 0) && (DistributionParameters.indexOf(":") > 0) ) {
        distParams = new Hashtable();
        // First break map pairs by comma
        List<String> pairs = new ArrayList<String>();
        if ( DistributionParameters.indexOf(",") > 0 ) {
            pairs = StringUtil.breakStringList(DistributionParameters, ",", 0 );
        }
        else {
            pairs.add(DistributionParameters);
        }
        // Now break pairs and put in hashtable
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            distParams.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String ProbabilityUnits = parameters.getValue ( "ProbabilityUnits" );
    String SortOrder = parameters.getValue ( "SortOrder" );
    SortOrderType sortOrderType = SortOrderType.HIGH_TO_LOW; // default
    if ( (SortOrder != null) && !SortOrder.equals("") ) {
        sortOrderType = SortOrderType.valueOfIgnoreCase(SortOrder);
    }
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String SampleMethod = parameters.getValue ( "SampleMethod" );
	RunningAverageType sampleMethod = RunningAverageType.valueOfIgnoreCase(SampleMethod);
    String Bracket = parameters.getValue ( "Bracket" );
    int Bracket_int = 0;
    if ( (Bracket != null) && Bracket.length() > 0) {
        Bracket_int = Integer.valueOf ( Bracket );
    }
    String BracketByMonth = parameters.getValue("BracketByMonth");
    Integer [] bracketByMonth = null;
    if ( (BracketByMonth != null) && (BracketByMonth.length() > 0) ) {
        bracketByMonth = new Integer[12];
        List<String> v = StringUtil.breakStringList ( BracketByMonth,",", 0 );
        // If less than 12 values are returned, add blanks at end
        String val;
        for ( int i = 0; i < 12; i++ ) {
        	if ( i >= v.size() ) {
        		val = "";
        	}
        	else {
        		val = v.get(i);
        	}
            if ( (val != null) && !val.isEmpty() ) {
            	bracketByMonth[i] = Integer.parseInt ( val.trim() );
            }
            else {
            	bracketByMonth[i] = null;
            }
        }
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
    String NormalStart = parameters.getValue ( "NormalStart" );
    String NormalEnd = parameters.getValue ( "NormalEnd" );
    String Alias = parameters.getValue ( "Alias" );
    String OutputStart = parameters.getValue ( "OutputStart" );
    String OutputEnd = parameters.getValue ( "OutputEnd" );
    String Properties = parameters.getValue ( "Properties" );
    Hashtable properties = null;
    if ( (Properties != null) && (Properties.length() > 0) && (Properties.indexOf(":") > 0) ) {
        properties = new Hashtable();
        // First break map pairs by comma
        List<String> pairs = new ArrayList<String>();
        if ( Properties.indexOf(",") > 0 ) {
            pairs = StringUtil.breakStringList(Properties, ",", 0 );
        }
        else {
            pairs.add(Properties);
        }
        // Now break pairs and put in hashtable
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            properties.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String CopyProperties = parameters.getValue ( "CopyProperties" );
    StringDictionary copyProperties = null;
    if ( (CopyProperties != null) && !CopyProperties.equals("") ) {
        copyProperties = new StringDictionary(CopyProperties,":",",");
    }
    
    // Figure out the dates to use for the analysis.
    // Default of null means to analyze the full period.
    DateTime AnalysisStart_DateTime = null;
    DateTime AnalysisEnd_DateTime = null;
    
    try {
        if ( (AnalysisStart != null) && !AnalysisStart.equals("") ) {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", AnalysisStart );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting AnalysisStart DateTime(DateTime=" + AnalysisStart + ") from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }
    
            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for AnalysisStart DateTime(DateTime=" +
                AnalysisStart + ") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
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
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }
    
    try {
        if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") ) {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", AnalysisEnd );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting AnalysisEnd DateTime(DateTime=" + AnalysisEnd + ") from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }
    
            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for AnalysisStart DateTime(DateTime=" +
                AnalysisStart + "\") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {
                AnalysisEnd_DateTime = (DateTime)prop_contents;
            }
        }
    }
    catch ( Exception e ) {
        message = "AnalysisEnd \"" + AnalysisEnd + "\" is invalid.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }
    
    // Figure out the dates to use for the normal period.
    // Default of null means to use the analysis period.
    DateTime NormalStart_DateTime = null;
    DateTime NormalEnd_DateTime = null;
    
    try {
        if ( (NormalStart != null) && !NormalStart.equals("") ) {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", NormalStart );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting NormalStart DateTime(DateTime=" + NormalStart + ") from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }
    
            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for NormalStart DateTime(DateTime=" +
                NormalStart + ") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {
                NormalStart_DateTime = (DateTime)prop_contents;
            }
        }
    }
    catch ( Exception e ) {
        message = "NormalStart \"" + NormalStart + "\" is invalid.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }
    
    try {
        if ( (NormalEnd != null) && !NormalEnd.equals("") ) {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", NormalEnd );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting NormalEnd DateTime(DateTime=" + NormalEnd + ") from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }
    
            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for NormalStart DateTime(DateTime=" +
                NormalStart + "\") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {
                NormalEnd_DateTime = (DateTime)prop_contents;
            }
        }
    }
    catch ( Exception e ) {
        message = "NormalEnd \"" + NormalEnd + "\" is invalid.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }
    
    // Figure out the dates to use for the Output.
    // Default of null means to analyze the full period.
    DateTime OutputStart_DateTime = null;
    DateTime OutputEnd_DateTime = null;
    
    try {
        if ( (OutputStart != null) && !OutputStart.equals("") ) {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", OutputStart );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting OutputStart DateTime(DateTime=" + OutputStart + ") from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }
    
            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for OutputStart DateTime(DateTime=" +
                OutputStart + ") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {
                OutputStart_DateTime = (DateTime)prop_contents;
            }
        }
    }
    catch ( Exception e ) {
        message = "OutputStart \"" + OutputStart + "\" is invalid.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }
    
    try {
        if ( (OutputEnd != null) && !OutputEnd.equals("") ) {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", OutputEnd );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting OutputEnd DateTime(DateTime=" + OutputEnd + ") from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }
    
            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for OutputStart DateTime(DateTime=" +
                OutputStart + "\") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {
                OutputEnd_DateTime = (DateTime)prop_contents;
            }
        }
    }
    catch ( Exception e ) {
        message = "OutputEnd \"" + OutputEnd + "\" is invalid.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }

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
			    new TSUtil_RunningStatistic(ts, Bracket_int, bracketByMonth, statisticType,
			        AnalysisStart_DateTime, AnalysisEnd_DateTime, sampleMethod, allowMissingCount,
			        minimumSampleSize, distributionType, distParams, ProbabilityUnits, sortOrderType,
			        NormalStart_DateTime, NormalEnd_DateTime, OutputStart_DateTime, OutputEnd_DateTime );
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
                if ( properties != null ) {
                    // Assign properties
                    Enumeration keys = properties.keys();
                    String key = null;
                    while ( keys.hasMoreElements() ) {
                        key = (String)keys.nextElement();
                        newts.setProperty( key, TSCommandProcessorUtil.expandTimeSeriesMetadataString (
                            processor, ts, (String)properties.get(key), status, CommandPhaseType.RUN) );
                    }
                }
                if ( copyProperties != null ) {
                    // Copy properties from input time series to output
                    LinkedHashMap<String, String> map = copyProperties.getLinkedHashMap();
                    String key, newProp;
                    Object o;
                    for ( Map.Entry<String,String> entry : map.entrySet() ) {
                        try {
                            key = entry.getKey();
                            newProp = map.get(key);
                            o = ts.getProperty(key);
                            if ( newProp.equals("*") || newProp.equals("") ) {
                                // Reset to the original
                                newProp = key;
                            }
                            if ( o != null ) {
                                newts.setProperty( newProp, o );
                            }
                        }
                        catch ( Exception e ) {
                            // This should not happen
                        }
                    }
                }
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
    String Distribution = props.getValue("Distribution");
    String DistributionParameters = props.getValue("DistributionParameters");
    String ProbabilityUnits = props.getValue("ProbabilityUnits");
    String SortOrder = props.getValue("SortOrder");
    String AnalysisStart = props.getValue( "AnalysisStart" );
    String AnalysisEnd = props.getValue( "AnalysisEnd" );
	String SampleMethod = props.getValue("SampleMethod");
	String Bracket = props.getValue("Bracket");
	String BracketByMonth = props.getValue("BracketByMonth");
	String CustomBracketByMonth = props.getValue("CustomBracketByMonth");
	String AllowMissingCount = props.getValue("AllowMissingCount");
	String MinimumSampleSize = props.getValue("MinimumSampleSize");
    String NormalStart = props.getValue( "NormalStart" );
    String NormalEnd = props.getValue( "NormalEnd" );
	String Alias = props.getValue("Alias");
	String OutputStart = props.getValue( "OutputStart" );
    String OutputEnd = props.getValue( "OutputEnd" );
    String Properties = props.getValue ( "Properties" );
    String CopyProperties = props.getValue ( "CopyProperties" );
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
    if ( (Distribution != null) && (Distribution.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Distribution=\"" + Distribution + "\"" );
    }
    if ( (DistributionParameters != null) && (DistributionParameters.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DistributionParameters=\"" + DistributionParameters + "\"");
    }
    if ( (ProbabilityUnits != null) && (ProbabilityUnits.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ProbabilityUnits=\"" + ProbabilityUnits + "\"" );
    }
    if ( (SortOrder != null) && (SortOrder.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SortOrder=" + SortOrder );
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
	if ( (BracketByMonth != null) && (BracketByMonth.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "BracketByMonth=\"" + BracketByMonth + "\"" );
	}
	if ( (CustomBracketByMonth != null) && (CustomBracketByMonth.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "CustomBracketByMonth=\"" + CustomBracketByMonth + "\"" );
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
    if ( (NormalStart != null) && (NormalStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NormalStart=\"" + NormalStart + "\"" );
    }
    if ( (NormalEnd != null) && (NormalEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NormalEnd=\"" + NormalEnd + "\"" );
    }
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }
    if ( (OutputStart != null) && (OutputStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputStart=\"" + OutputStart + "\"" );
    }
    if ( (OutputEnd != null) && (OutputEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputEnd=\"" + OutputEnd + "\"" );
    }
    if ((Properties != null) && (Properties.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Properties=\"" + Properties + "\"");
    }
    if ((CopyProperties != null) && (CopyProperties.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("CopyProperties=\"" + CopyProperties + "\"");
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}