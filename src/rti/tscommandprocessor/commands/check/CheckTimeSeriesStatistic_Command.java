package rti.tscommandprocessor.commands.check;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

import RTi.TS.CheckType;
import RTi.TS.TS;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_CalculateTimeSeriesStatistic;
import RTi.TS.TSUtil_CheckTimeSeries;
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
import RTi.Util.IO.PropList;
import RTi.Util.IO.WarningCount;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeRange;

/**
This class initializes, checks, and runs the CheckTimeSeriesStatistic() command.
*/
public class CheckTimeSeriesStatistic_Command extends AbstractCommand implements Command
{

/**
Values for IfCriteriaMet parameter.
*/
protected final String _Fail = "Fail";
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";

/**
Constructor.
*/
public CheckTimeSeriesStatistic_Command ()
{   super();
    setCommandName ( "CheckTimeSeriesStatistic" );
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
{   //String TSID = parameters.getValue ( "TSID" );
    String Statistic = parameters.getValue ( "Statistic" );
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
    String StatisticValue1 = parameters.getValue ( "StatisticValue1" );
    String StatisticValue2 = parameters.getValue ( "StatisticValue2" );
    String StatisticValue3 = parameters.getValue ( "StatisticValue3" );
    String CheckCriteria = parameters.getValue ( "CheckCriteria" );
    String CheckValue1 = parameters.getValue ( "CheckValue1" );
    String CheckValue2 = parameters.getValue ( "CheckValue2" );
    String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    if ( (Statistic == null) || Statistic.equals("") ) {
        message = "The statistic must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the statistic to calculate." ) );
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
            List<TSStatisticType> statistics = TSUtil_CalculateTimeSeriesStatistic.getStatisticChoices();
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
       
        // Additional checks that depend on the statistic
        
        if ( supported ) {
            int nRequiredValues = -1;
            try {
                TSUtil_CalculateTimeSeriesStatistic.getRequiredNumberOfValuesForStatistic ( statisticType );
            }
            catch ( Exception e ) {
                message = "Statistic \"" + statisticType + "\" is not recognized.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Contact software support." ) );
            }
            
            if ( nRequiredValues >= 1 ) {
                if ( (StatisticValue1 == null) || StatisticValue1.equals("") ) {
                    message = "StatisticValue1 must be specified for the statistic.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide StatisticValue1." ) );
                }
                else if ( !StringUtil.isDouble(StatisticValue1) ) {
                    message = "StatisticValue1 (" + StatisticValue1 + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify StatisticValue1 as a number." ) );
                }
            }
            
            if ( nRequiredValues >= 2 ) {
                if ( (StatisticValue2 == null) || StatisticValue2.equals("") ) {
                    message = "StatisticValue2 must be specified for the statistic.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide Value2." ) );
                }
                else if ( !StringUtil.isDouble(StatisticValue2) ) {
                    message = "StatisticValue2 (" + StatisticValue2 + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify StatisticValue2 as a number." ) );
                }
            }
            
            if ( nRequiredValues == 3 ) {
                if ( (StatisticValue3 == null) || StatisticValue3.equals("") ) {
                    message = "StatisticValue3 must be specified for the statistic.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide StatisticValue3." ) );
                }
                else if ( !StringUtil.isDouble(StatisticValue2) ) {
                    message = "StatisticValue3 (" + StatisticValue3 + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify StatisticValue3 as a number." ) );
                }
            }
    
            if ( nRequiredValues > 3 ) {
                message = "A maximum of 3 values are supported as input to statistic computation.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Refer to documentation for statistic.  Contact software support if necessary." ) ); 
            }
        }
    }
    
    if ( (CheckCriteria == null) || CheckCriteria.equals("") ) {
        message = "The check criteria must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the check criteria to evaluate." ) );
    }
    else {
        // Make sure that it is in the supported list
        CheckType checkType = CheckType.valueOfIgnoreCase(CheckCriteria);
        if ( checkType == null ) {
            message = "The check criteria (" + CheckCriteria + ") is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported criteria using the command editor." ) );
        }

        // Additional checks that depend on the criteria
        
        int nRequiredValues = 0;
        if ( checkType != null ) {
            nRequiredValues = TSUtil_CheckTimeSeries.getRequiredNumberOfValuesForCheckCriteria ( checkType );
        }
        
        if ( nRequiredValues >= 1 ) {
            if ( (CheckValue1 == null) || CheckValue1.equals("") ) {
                message = "CheckValue1 must be specified for the criteria.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Provide CheckValue1." ) );
            }
            else if ( !StringUtil.isDouble(CheckValue1) ) {
                message = "CheckValue1 (" + CheckValue1 + ") is not a number.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify CheckValue1 as a number." ) );
            }
        }
        
        if ( nRequiredValues == 2 ) {
            if ( (CheckValue2 == null) || CheckValue2.equals("") ) {
                message = "CheckValue2 must be specified for the criteria.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Provide CheckValue2." ) );
            }
            else if ( !StringUtil.isDouble(CheckValue2) ) {
                message = "CheckValue2 (" + CheckValue2 + ") is not a number.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify CheckValue2 as a number." ) );
            }
        }
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
    
    // Check for invalid parameters...
    List<String> valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "Statistic" );
    valid_Vector.add ( "StatisticValue1" );
    valid_Vector.add ( "StatisticValue2" );
    valid_Vector.add ( "StatisticValue3" );
    valid_Vector.add ( "AnalysisStart" );
    valid_Vector.add ( "AnalysisEnd" );
    valid_Vector.add ( "TableID" );
    valid_Vector.add ( "TableTSIDColumn" );
    valid_Vector.add ( "TableTSIDFormat" );
    valid_Vector.add ( "TableStatisticColumn" );
    valid_Vector.add ( "CheckCriteria" );
    valid_Vector.add ( "CheckValue1" );
    valid_Vector.add ( "CheckValue2" );
    valid_Vector.add ( "IfCriteriaMet" );
    valid_Vector.add ( "ProblemType" );
    valid_Vector.add ( "PropertyName" );
    valid_Vector.add ( "PropertyValue" );
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
Check the time series statistic.
@param ts the time series to check
@param tsStatistic the statistic that was computed (and supporting information)
@param checkCriteria the check criteria compared to the statistic
@param checkValue1 the value of the check criteria
@param checkValue2 the second value of the check criteria (if needed)
@param ifCriteriaMet action if the criteria is matched (for messaging)
@param problemType the string to use for problem messages, when criteria is met
@param propertyName the property name to assign, when the criteria is met
@param propertyValue the property value to assign to the time series, when criteria is met
@param problems a list of problems encountered during processing, to add to command status in calling code
@return true if the check matches the criteria, false if not (or could not be evaluated).
For example, true will be returned if the statistic
is 100 and the check criteria is for values > 90.
*/
public boolean checkTimeSeriesStatistic ( TS ts, TSUtil_CalculateTimeSeriesStatistic tsStatistic,
    CheckType checkCriteria, Double checkValue1, Double checkValue2,
    String ifCriteriaMet, String problemType, String propertyName, String propertyValue,
    List<String> problems )
{
    Object statisticValue = tsStatistic.getStatisticResult();
    if ( statisticValue == null ) {
        // Statistic was not computed so this is definitely an error
        problems.add ( "Statistic was not computed - unable to check its value." );
        return false;
    }
    boolean meetsCriteria = false;
    if ( statisticValue instanceof Double ) {
        // Do comparisons on doubles
        Double statisticDouble = (Double)statisticValue;
        if ( statisticDouble.isNaN() ) {
            problems.add( "Statistic value is not a number - unable to check its value." );
        }
        else if ( checkCriteria == CheckType.IN_RANGE ) {
            if ( (statisticDouble >= checkValue1) && (statisticDouble <= checkValue2) ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.OUT_OF_RANGE ) {
            if ( (statisticDouble < checkValue1) || (statisticDouble > checkValue2) ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.LESS_THAN ) {
            if ( statisticDouble < checkValue1 ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.LESS_THAN_OR_EQUAL_TO ) {
            if ( statisticDouble <= checkValue1 ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.GREATER_THAN ) {
            if ( statisticDouble > checkValue1 ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.GREATER_THAN_OR_EQUAL_TO ) {
            if ( statisticDouble >= checkValue1 ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.EQUAL_TO ) {
            if ( statisticDouble == checkValue1 ) {
                meetsCriteria = true;
            }
        }
        else {
            // Criteria is not supported
            problems.add ( "Check criteria " + checkCriteria + " is not supported." );
        }
    }
    else if ( statisticValue instanceof Integer ) {
        // Do comparisons on integers
        Integer statisticInteger = (Integer)statisticValue;
        if ( checkCriteria == CheckType.IN_RANGE ) {
            if ( (statisticInteger >= checkValue1) && (statisticInteger <= checkValue2) ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.OUT_OF_RANGE ) {
            if ( (statisticInteger < checkValue1) || (statisticInteger > checkValue2) ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.LESS_THAN ) {
            if ( statisticInteger < checkValue1 ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.LESS_THAN_OR_EQUAL_TO ) {
            if ( statisticInteger <= checkValue1 ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.GREATER_THAN ) {
            if ( statisticInteger > checkValue1 ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.GREATER_THAN_OR_EQUAL_TO ) {
            if ( statisticInteger >= checkValue1 ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.EQUAL_TO ) {
            if ( statisticInteger == (int)checkValue1.doubleValue() ) {
                meetsCriteria = true;
            }
        }
    }
    else {
        // Don't know how to handle
        problems.add ( "Statistic is not a floating point number or integer - unable to check its value." );
    }
    if ( meetsCriteria ) {
        if ( (propertyName != null) && !propertyName.equals("") &&
            (propertyValue != null) && !propertyValue.equals("") ) {
            ts.setProperty(propertyName, propertyValue);
        }
    }
    return meetsCriteria;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{   List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    return (new CheckTimeSeriesStatistic_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Get the list of check types that can be performed.
*/
public List<CheckType> getCheckCriteriaChoices()
{
    List<CheckType> choices = new Vector();
    choices.add ( CheckType.IN_RANGE );
    choices.add ( CheckType.OUT_OF_RANGE );
    //choices.add ( CheckType.MISSING );
    choices.add ( CheckType.LESS_THAN );
    choices.add ( CheckType.LESS_THAN_OR_EQUAL_TO );
    choices.add ( CheckType.GREATER_THAN );
    choices.add ( CheckType.GREATER_THAN_OR_EQUAL_TO );
    choices.add ( CheckType.EQUAL_TO );
    return choices;
}

/**
Get the list of statistics that can be performed.
@return the statistic display names as strings.
*/
public List<String> getCheckCriteriaChoicesAsStrings()
{
    List<CheckType> choices = getCheckCriteriaChoices();
    List<String> stringChoices = new Vector();
    for ( int i = 0; i < choices.size(); i++ ) {
        stringChoices.add ( "" + choices.get(i) );
    }
    return stringChoices;
}

// Parse command is in the base class

/**
Run the command.
@param command_number Number of command in sequence.
@exception Exception if there is an error processing the command.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{   String message, routine = getCommandName() + "_Command.runCommand";
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    int log_level = 3; // Level for non-user messages for log file.
    
    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    PropList parameters = getCommandParameters();
    
    // Get the input parameters...
    
    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
    String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String Statistic = parameters.getValue ( "Statistic" );
    String StatisticValue1 = parameters.getValue ( "StatisticValue1" );
    Double StatisticValue1_Double = null;
    if ( (StatisticValue1 != null) && !StatisticValue1.equals("") ) {
        StatisticValue1_Double = new Double(StatisticValue1);
    }
    String StatisticValue2 = parameters.getValue ( "StatisticValue2" );
    Double StatisticValue2_Double = null;
    if ( (StatisticValue2 != null) && !StatisticValue2.equals("") ) {
        StatisticValue2_Double = new Double(StatisticValue2);
    }
    String StatisticValue3 = parameters.getValue ( "StatisticValue3" );
    Double StatisticValue3_Double = null;
    if ( (StatisticValue3 != null) && !StatisticValue3.equals("") ) {
        StatisticValue3_Double = new Double(StatisticValue3);
    }
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TableStatisticColumn = parameters.getValue ( "TableStatisticColumn" );
    String CheckCriteria = parameters.getValue ( "CheckCriteria" );
    CheckType checkCriteria = CheckType.valueOfIgnoreCase(CheckCriteria);
    String CheckValue1 = parameters.getValue ( "CheckValue1" );
    Double CheckValue1_Double = null;
    if ( (CheckValue1 != null) && !CheckValue1.equals("") ) {
        CheckValue1_Double = new Double(CheckValue1);
    }
    String CheckValue2 = parameters.getValue ( "CheckValue2" );
    Double CheckValue2_Double = null;
    if ( (CheckValue2 != null) && !CheckValue2.equals("") ) {
        CheckValue2_Double = new Double(CheckValue2);
    }
    String IfCriteriaMet = parameters.getValue ( "IfCriteriaMet" );
    if ( (IfCriteriaMet == null) || IfCriteriaMet.equals("") ) {
        IfCriteriaMet = _Warn; // Default
    }
    String ProblemType = parameters.getValue ( "ProblemType" );
    if ( (ProblemType == null) || ProblemType.equals("") ) {
        ProblemType = Statistic + "-" + CheckCriteria; // Default
    }
    String PropertyName = parameters.getValue ( "PropertyName" );
    String PropertyValue = parameters.getValue ( "PropertyValue" );

    // Determine the dates to use for the analysis.
    // Default of null means to analyze the full period.
    WarningCount warningCount = new WarningCount();
    DateTimeRange setStartAndEnd = TSCommandProcessorUtil.getOutputPeriodForCommand (
        this, CommandPhaseType.RUN, "AnalysisStart", AnalysisStart, "AnalysisEnd", AnalysisEnd,
        false, // Do not want global output period as default - default is full time series period
        log_level, command_tag, warning_level, warningCount );
    warning_count += warningCount.getCount();
    // OK if these are null...
    DateTime AnalysisStart_DateTime = setStartAndEnd.getStart();
    DateTime AnalysisEnd_DateTime = setStartAndEnd.getEnd();

    // Get the time series to process.  Allow TSID to be a pattern or specific time series...

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
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report the problem to software support." ) );
    }
    if ( bean == null ) {
        Message.printStatus ( 2, routine, "Bean is null.");
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
    List tslist = null;
    if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
        Message.printWarning ( log_level, MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    }
    else {
        tslist = (List)o_TSList;
        if ( tslist.size() == 0 ) {
            message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
            Message.printWarning ( log_level, MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
    }
    
    int nts = tslist.size();
    if ( nts == 0 ) {
        message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    }
    
    DataTable table = null;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be updated
        request_params = new PropList ( "" );
        request_params.set ( "TableID", TableID );
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
        bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table == null ) {
            message = "Unable to find table to process using TableID=\"" + TableID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a table exists with the requested ID." ) );
        }
        else {
            table = (DataTable)o_Table;
        }
    }
    
    if ( warning_count > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }
    
    // Now process...
    
    try {
        TS ts = null;
        Object o_ts = null;
        for ( int its = 0; its < nts; its++ ) {
            // The the time series to process, from the list that was returned above.
            o_ts = tslist.get(its);
            if ( o_ts == null ) {
                message = "Time series to process is null.";
                Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
                // Go to next time series.
                continue;
            }
            ts = (TS)o_ts;
            
            try {
                // Do the statistic calculation...
                notifyCommandProgressListeners ( its, nts, (float)-1.0, "Checking statistic for " +
                    ts.getIdentifier().toStringAliasAndTSID() );
                TSStatisticType statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
                TSUtil_CalculateTimeSeriesStatistic tsStatistic = new TSUtil_CalculateTimeSeriesStatistic(ts, statisticType,
                    AnalysisStart_DateTime, AnalysisEnd_DateTime, null, null,
                    StatisticValue1_Double, StatisticValue2_Double, StatisticValue3_Double );
                tsStatistic.calculateTimeSeriesStatistic();
                Message.printStatus(2,routine,"Statistic " + statisticType + "=" + tsStatistic.getStatisticResult() +
                    " for " + ts.getIdentifier().toStringAliasAndTSID() );
                // Now set in the table
                if ( (TableID != null) && !TableID.equals("") ) {
                    if ( (TableStatisticColumn != null) && !TableStatisticColumn.equals("") ) {
                        // See if a matching row exists using the specified TSID column...
                        String tsid = null;
                        if ( (TableTSIDFormat != null) && !TableTSIDFormat.equals("") ) {
                            // Format the TSID using the specified format
                            tsid = ts.formatLegend ( TableTSIDFormat );
                        }
                        else {
                            // Use the alias if available and then the TSID
                            tsid = ts.getAlias();
                            if ( (tsid == null) || tsid.equals("") ) {
                                tsid = ts.getIdentifierString();
                            }
                        }
                        Message.printStatus(2,routine, "Searching column \"" + TableTSIDColumn + "\" for \"" +
                            tsid + "\"" );
                        TableRecord rec = table.getRecord ( TableTSIDColumn, tsid );
                        Message.printStatus(2,routine, "Searched column \"" + TableTSIDColumn + "\" for \"" +
                            tsid + "\" ... found " + rec );
                        int statisticColumn = -1;
                        try {
                            statisticColumn = table.getFieldIndex(TableStatisticColumn);
                        }
                        catch ( Exception e2 ) {
                            // Automatically add to the table, initialize with null (not nonValue)
                            table.addField(new TableField(TableField.DATA_TYPE_DOUBLE,TableStatisticColumn,10,4), null );
                            statisticColumn = table.getFieldIndex(TableStatisticColumn);
                        }
                        if ( rec != null ) {
                            // There is already a row for the TSID so just set the value in the table column...
                            rec.setFieldValue(statisticColumn, tsStatistic.getStatisticResult());
                        }
                        else {
                            // There is no row in the table for the time series so add a row to the table...
                            int tsidColumn = table.getFieldIndex(TableTSIDColumn);
                            table.addRecord(table.emptyRecord().setFieldValue(tsidColumn, tsid).
                                setFieldValue(statisticColumn, tsStatistic.getStatisticResult()));
                        }
                    }
                }
                
                // Do the check by comparing to the statistic...
                List<String> problems = new Vector();
                // This is similar to TSUtil_CheckTimeSeries but it only needs to check the one statistic
                // value and therefore is much simpler... so include the code in this class for now
                boolean ifCriteriaMet = checkTimeSeriesStatistic ( ts, tsStatistic, checkCriteria,
                    CheckValue1_Double, CheckValue2_Double,
                    IfCriteriaMet, ProblemType, PropertyName, PropertyValue, problems );
                if ( ifCriteriaMet ) {
                    // Generate a warning
                    CommandStatusType commandStatusType = CommandStatusType.WARNING;
                    if ( IfCriteriaMet.equals(_Fail) ) {
                        commandStatusType = CommandStatusType.FAILURE;
                    }
                    StringBuffer b = new StringBuffer();
                    b.append ( "Statistic " + Statistic + " (" + tsStatistic.getStatisticResult() +
                        ") meets criteria " + CheckCriteria );
                    if ( (CheckValue1 != null) && !CheckValue1.equals("") ) {
                        b.append ( " " + CheckValue1 );
                    }
                    if ( (CheckValue2 != null) && !CheckValue2.equals("") ) {
                        b.append ( ", " + CheckValue2 );
                    }
                    b.append ( " for time series " + ts.getIdentifier().toStringAliasAndTSID() );
                    Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag(command_tag,++warning_count),routine,b.toString() );
                    if ( !IfCriteriaMet.equalsIgnoreCase(_Ignore) ) {
                        status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(commandStatusType,
                            ProblemType, b.toString(), "Time series should be treated accordingly." ) );
                    }
                }
                int problemsSize = problems.size();
                for ( int iprob = 0; iprob < problemsSize; iprob++ ) {
                    message = problems.get(iprob);
                    Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                    // No recommendation since it is a user-defined check
                    // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
                    status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.WARNING, ProblemType, message, "" ) );
                }
            }
            catch ( Exception e ) {
                message = "Unexpected error checking time series \""+ ts.getIdentifier() + " (" + e + ").";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                Message.printWarning(3,routine,e);
                status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
            }
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error checking time series (" + e + ").";
        Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check log file for details." ) );
        throw new CommandException ( message );
    }
    
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
        throw new CommandWarningException ( message );
    }
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
@param parameters parameters for the command.
*/
public String toString ( PropList parameters )
{   
    if ( parameters == null ) {
        return getCommandName() + "()";
    }
    
    String TSList = parameters.getValue( "TSList" );
    String TSID = parameters.getValue( "TSID" );
    String EnsembleID = parameters.getValue( "EnsembleID" );
    String Statistic = parameters.getValue( "Statistic" );
    String StatisticValue1 = parameters.getValue( "StatisticValue1" );
    String StatisticValue2 = parameters.getValue( "StatisticValue2" );
    String StatisticValue3 = parameters.getValue( "StatisticValue3" );
    String AnalysisStart = parameters.getValue( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue( "AnalysisEnd" );
    String IfNotFound = parameters.getValue ( "IfNotFound" );
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TableStatisticColumn = parameters.getValue ( "TableStatisticColumn" );
    String CheckCriteria = parameters.getValue( "CheckCriteria" );
    String CheckValue1 = parameters.getValue( "CheckValue1" );
    String CheckValue2 = parameters.getValue( "CheckValue2" );
    String IfCriteriaMet = parameters.getValue( "IfCriteriaMet" );
    String ProblemType = parameters.getValue( "ProblemType" );
    String PropertyName = parameters.getValue( "PropertyName" );
    String PropertyValue = parameters.getValue( "PropertyValue" );
        
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
        b.append ( "Statistic=\"" + Statistic + "\"" );
    }
    if ( (StatisticValue1 != null) && (StatisticValue1.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "StatisticValue1=" + StatisticValue1 );
    }
    if ( (StatisticValue2 != null) && (StatisticValue2.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "StatisticValue2=" + StatisticValue2 );
    }
    if ( (StatisticValue3 != null) && (StatisticValue3.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "StatisticValue3=" + StatisticValue3 );
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
    if ( IfNotFound != null && IfNotFound.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IfNotFound=" + IfNotFound );
    }
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (TableTSIDColumn != null) && (TableTSIDColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableTSIDColumn=\"" + TableTSIDColumn + "\"" );
    }
    if ( (TableTSIDFormat != null) && (TableTSIDFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableTSIDFormat=\"" + TableTSIDFormat + "\"" );
    }
    if ( (TableStatisticColumn != null) && (TableStatisticColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableStatisticColumn=\"" + TableStatisticColumn + "\"" );
    }
    if ( (CheckCriteria != null) && (CheckCriteria.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CheckCriteria=\"" + CheckCriteria + "\"" );
    }
    if ( (CheckValue1 != null) && (CheckValue1.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CheckValue1=" + CheckValue1 );
    }
    if ( (CheckValue2 != null) && (CheckValue2.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CheckValue2=" + CheckValue2 );
    }
    if ( (IfCriteriaMet != null) && (IfCriteriaMet.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IfCriteriaMet=" + IfCriteriaMet );
    }
    if ( (ProblemType != null) && (ProblemType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ProblemType=\"" + ProblemType + "\"" );
    }
    if ( (PropertyName != null) && (PropertyName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PropertyName=\"" + PropertyName + "\"" );
    }
    if ( (PropertyValue != null) && (PropertyValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PropertyValue=\"" + PropertyValue + "\"" );
    }
    
    return getCommandName() + "(" + b.toString() + ")";
}

}