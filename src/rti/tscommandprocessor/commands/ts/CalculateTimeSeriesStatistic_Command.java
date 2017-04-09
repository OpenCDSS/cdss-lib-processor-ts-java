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
import RTi.TS.TSUtil_CalculateTimeSeriesStatistic;
import RTi.Util.Math.Regression;
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
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the CalculateTimeSeriesStatistic() command.
*/
public class CalculateTimeSeriesStatistic_Command extends AbstractCommand
implements CommandDiscoverable, ObjectListProvider
{
    
/**
Values for IfNotFound parameter.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

/**
Analysis window year, since users do not supply this information.
This allows for leap year in case the analysis window start or end is on Feb 29.
*/
private final int __ANALYSIS_WINDOW_YEAR = 2000;

/**
Values for ProblemType parameter.
*/
protected final String _PROBLEM_TYPE_Check = "Check";

/**
The table that is created (when not operating on an existing table).
*/
private DataTable __table = null;

/**
Constructor.
*/
public CalculateTimeSeriesStatistic_Command ()
{   super();
    setCommandName ( "CalculateTimeSeriesStatistic" );
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
    String AnalysisWindowStart = parameters.getValue ( "AnalysisWindowStart" );
    String AnalysisWindowEnd = parameters.getValue ( "AnalysisWindowEnd" );
    String Value1 = parameters.getValue ( "Value1" );
    String Value2 = parameters.getValue ( "Value2" );
    String Value3 = parameters.getValue ( "Value3" );
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
        TSStatisticType statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
        if ( statisticType == null ) {
            message = "The statistic (" + Statistic + ") is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported statistic using the command editor." ) );
        }
        else {
            // Make sure that it is in the supported list
            supported = false;
            List<TSStatisticType> statistics = TSUtil_CalculateTimeSeriesStatistic.getStatisticChoices();
            for ( TSStatisticType statistic : statistics ) {
                if ( statisticType == statistic ) {
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
                nRequiredValues = TSUtil_CalculateTimeSeriesStatistic.getRequiredNumberOfValuesForStatistic ( statisticType );
            }
            catch ( Exception e ) {
                message = "Statistic \"" + statisticType + "\" is not recognized.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Contact software support." ) );
            }
            
            if ( nRequiredValues >= 1 ) {
                if ( (Value1 == null) || Value1.equals("") ) {
                    message = "Value1 must be specified for the statistic.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide Value1." ) );
                }
                else if ( !StringUtil.isDouble(Value1) ) {
                    message = "Value1 (" + Value1 + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify Value1 as a number." ) );
                }
            }
            
            if ( nRequiredValues >= 2 ) {
                if ( (Value2 == null) || Value2.equals("") ) {
                    message = "Value2 must be specified for the statistic.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide Value2." ) );
                }
                else if ( !StringUtil.isDouble(Value2) ) {
                    message = "Value2 (" + Value2 + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify Value2 as a number." ) );
                }
            }
            
            if ( nRequiredValues == 3 ) {
                if ( (Value3 == null) || Value3.equals("") ) {
                    message = "Value3 must be specified for the statistic.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide Value3." ) );
                }
                else if ( !StringUtil.isDouble(Value2) ) {
                    message = "Value3 (" + Value3 + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify Value3 as a number." ) );
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

    if ( (AnalysisStart != null) && !AnalysisStart.equals("") && !AnalysisStart.startsWith("${") &&
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
    if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") && !AnalysisEnd.startsWith("${") &&
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
    if ( (AnalysisWindowStart != null) && !AnalysisWindowStart.isEmpty() ) {
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
    
    if ( (AnalysisWindowEnd != null) && !AnalysisWindowEnd.isEmpty() ) {
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
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(18);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "Statistic" );
    validList.add ( "Value1" );
    validList.add ( "Value2" );
    validList.add ( "Value3" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
    validList.add ( "AnalysisWindowStart" );
    validList.add ( "AnalysisWindowEnd" );
    validList.add ( "TableID" );
    validList.add ( "TableTSIDColumn" );
    validList.add ( "TableTSIDFormat" );
    validList.add ( "TableStatisticColumn" );
    validList.add ( "TableStatisticDateTimeColumn" );
    validList.add ( "TimeSeriesProperty" );
    validList.add ( "StatisticValueProperty" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{   List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    return (new CalculateTimeSeriesStatistic_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable table = getDiscoveryTable();
    List<DataTable> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector<DataTable>();
        v.add ( table );
    }
    // TODO sam 2017-04-01 need to evaluate whether to return Prop with processor property name
    return v;
}

// Parse command is in the base class

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
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
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   String message, routine = getClass().getSimpleName() + ".runCommandInternal";
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    int log_level = 3;  // Level for non-use messages for log file.
    
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
		status.clearLog(CommandPhaseType.RUN);
	}
    PropList parameters = getCommandParameters();
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }
    
    // Get the input parameters...
    
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
    String Statistic = parameters.getValue ( "Statistic" );
    String Value1 = parameters.getValue ( "Value1" );
    Double Value1_Double = null;
    if ( (Value1 != null) && !Value1.equals("") ) {
        Value1_Double = new Double(Value1);
    }
    String Value2 = parameters.getValue ( "Value2" );
    Double Value2_Double = null;
    if ( (Value2 != null) && !Value2.equals("") ) {
        Value2_Double = new Double(Value2);
    }
    String Value3 = parameters.getValue ( "Value3" );
    Double Value3_Double = null;
    if ( (Value3 != null) && !Value3.equals("") ) {
        Value3_Double = new Double(Value3);
    }
    String AnalysisStart = parameters.getValue ( "AnalysisStart" ); // Property expansion is handled below
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
    String AnalysisWindowStart = parameters.getValue ( "AnalysisWindowStart" );
    String AnalysisWindowEnd = parameters.getValue ( "AnalysisWindowEnd" );
    boolean doTable = false;
    String TableID = parameters.getValue ( "TableID" );
    if ( (TableID != null) && !TableID.isEmpty() ) {
    	doTable = true;
    	if ( (commandPhase == CommandPhaseType.RUN) && (TableID.indexOf("${") >= 0) ) {
    		// In discovery mode want lists of tables to include ${Property}
    		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    	}
    }
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    if ( (TableTSIDColumn != null) && (TableTSIDColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	TableTSIDColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableTSIDColumn);
	}
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    if ( (TableTSIDFormat != null) && (TableTSIDFormat.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	TableTSIDFormat = TSCommandProcessorUtil.expandParameterValue(processor, this, TableTSIDFormat);
	}
    // TODO SAM 2015-07-16 it is not clear how multiple statistic columns are used.
    // Seems like only the first is used and 2nd, 3rd, etc. are not needed?
    // TODO SAM 2015-07-16 This is expanded here and then below for each time series - maybe only expand below?
    String TableStatisticColumn = parameters.getValue ( "TableStatisticColumn" );
    if ( (TableStatisticColumn != null) && (TableStatisticColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	TableStatisticColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableStatisticColumn);
	}
    String [] tableStatisticResultsColumn = new String[0];
    if ( (TableStatisticColumn != null) && !TableStatisticColumn.equals("") ) {
        String [] tableStatisticColumnParts = TableStatisticColumn.split(",");
        if ( Statistic.equalsIgnoreCase("" + TSStatisticType.TREND_OLS) ) {
            // Output will consist of multiple statistics corresponding to parameter-assigned column + suffix
            tableStatisticResultsColumn = new String[3];
            tableStatisticResultsColumn[0] = "" + tableStatisticColumnParts[0] + "_Intercept";
            tableStatisticResultsColumn[1] = "" + tableStatisticColumnParts[0] + "_Slope";
            tableStatisticResultsColumn[2] = "" + tableStatisticColumnParts[0] + "_R2";
        }
        else {
            // Output will consist of single statistic corresponding to parameter-assigned column
            // Some statistics like "Last" also have the date
            tableStatisticResultsColumn = new String[tableStatisticColumnParts.length];
            for ( int i = 0; i < tableStatisticColumnParts.length; i++ ) {
                tableStatisticResultsColumn[i] = tableStatisticColumnParts[i];
            }
        }
    }
    int [] statisticColumnNum = new int[tableStatisticResultsColumn.length]; // Integer columns for performance
    String tableStatisticDateTimeColumn = parameters.getValue ( "TableStatisticDateTimeColumn" );
    // TODO SAM 2015-07-16 This is expanded here and then below for each time series - maybe only expand below?
    if ( (tableStatisticDateTimeColumn != null) && (tableStatisticDateTimeColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	tableStatisticDateTimeColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, tableStatisticDateTimeColumn);
	}
    int statisticDateTimeColumnNum = -1;
    String TimeSeriesProperty = parameters.getValue ( "TimeSeriesProperty" );
    String StatisticValueProperty = parameters.getValue ( "StatisticValueProperty" );
    if ( (StatisticValueProperty != null) && (StatisticValueProperty.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	StatisticValueProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, StatisticValueProperty);
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
    
    // Get the time series to process.  Allow TSID to be a pattern or specific time series...

    List<TS> tslist = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, null );
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
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
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
            Message.printWarning ( log_level, MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
        else {
            @SuppressWarnings("unchecked")
			List<TS> tslist0 = (List <TS>)o_TSList;
            tslist = tslist0;
            if ( tslist.size() == 0 ) {
                message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
                Message.printWarning ( log_level, MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
            }
        }
    }
    
    int nts = tslist.size();
    if ( nts == 0 ) {
        message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	// Error is OK, especially with dynamic content
        }
        else {
	        Message.printWarning ( warning_level,
	        MessageUtil.formatMessageTag(
	        command_tag,++warning_count), routine, message );
	        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
	            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
    }

    // Get the table to process.

    DataTable table = null;
    if ( doTable ) {
	    PropList request_params = null;
	    CommandProcessorRequestResultsBean bean = null;
	    if ( (TableID != null) && !TableID.isEmpty() ) {
	        // Get the table to be updated/created
	        request_params = new PropList ( "" );
	        request_params.set ( "TableID", TableID );
	        try {
	            bean = processor.processRequest( "GetTable", request_params);
	            PropList bean_PropList = bean.getResultsPropList();
	            Object o_Table = bean_PropList.getContents ( "Table" );
	            if ( o_Table != null ) {
	                // Found the table so no need to create it
	                table = (DataTable)o_Table;
	            }
	        }
	        catch ( Exception e ) {
	            message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
	            Message.printWarning(warning_level,
	                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
	            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Report problem to software support." ) );
	        }
	    }
    }
    
    if ( warning_count > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }
    
    // Now process...
    
    try {
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	if ( doTable ) {
	            if ( table == null ) {
	                // Did not find table so is being created in this command
	                // Create an empty table and set the ID
	                table = new DataTable();
	                table.setTableID ( TableID );
	                // Only set discovery if creating here because if table was found a previous
	                // command supplies in discovery mode
	            }
                setDiscoveryTable ( table );
        	}
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
        	if ( doTable ) {
	            if ( table == null ) {
	                // Did not find the table above so create it
	                table = new DataTable( /*columnList*/ );
	                table.setTableID ( TableID );
	                Message.printStatus(2, routine, "Was not able to match existing table \"" + TableID + "\" so created new table.");
	                
	                // Set the table in the processor...
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
            // Process the time series and add statistics columns to the table if not found...
            TS ts = null;
            Object o_ts = null;
            for ( int its = 0; its < nts; its++ ) {
                // Get the time series to process, from the list that was returned above.
                o_ts = tslist.get(its);
                if ( o_ts == null ) {
                    message = "Time series to process is null.";
                    Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
                    // Go to next time series.
                    continue;
                }
                ts = (TS)o_ts;
                notifyCommandProgressListeners ( its, nts, (float)-1.0, "Calculating statistic for " +
                    ts.getIdentifier().toStringAliasAndTSID() );
                int tableTSIDColumnNumber = -1;
                if ( doTable ) {
	                // Make sure that the output table includes the TSID column.
	                try {
	                    tableTSIDColumnNumber = table.getFieldIndex(TSCommandProcessorUtil.expandTimeSeriesMetadataString(
	                        processor, ts, TableTSIDColumn, status, commandPhase));
	                }
	                catch ( Exception e2 ) {
	                    tableTSIDColumnNumber =
	                        table.addField(new TableField(TableField.DATA_TYPE_STRING, TableTSIDColumn, -1, -1), null);
	                    Message.printStatus(2, routine, "Did not match TableTSIDColumn \"" + TableTSIDColumn +
	                        "\" as column table so added to table." );
	                }
	                
	                // Expand the statistic output column based on runtime information
	                // Also reset the statistic columns each time because can vary based on time series property
	                // TODO SAM 2014-06-09 Optimize this to only reset when the statistic column is dynamically determined
	                for ( int i = 0; i < tableStatisticResultsColumn.length; i++ ) {
	                    statisticColumnNum[i] = -1;
	                }
	                if ( (TableStatisticColumn != null) && !TableStatisticColumn.isEmpty() ) {
	                    String [] tableStatisticColumnParts = TableStatisticColumn.split(",");
	                    if ( Statistic.equalsIgnoreCase("" + TSStatisticType.TREND_OLS) ) {
	                        // Output will consist of multiple statistics corresponding to parameter-assigned column + suffix
	                        tableStatisticResultsColumn = new String[3];
	                        tableStatisticResultsColumn[0] = "" + tableStatisticColumnParts[0] + "_Intercept";
	                        tableStatisticResultsColumn[0] = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
	                            processor, ts, tableStatisticResultsColumn[0], status, commandPhase);
	                        tableStatisticResultsColumn[1] = "" + tableStatisticColumnParts[0] + "_Slope";
	                        tableStatisticResultsColumn[1] = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
	                            processor, ts, tableStatisticResultsColumn[1], status, commandPhase);
	                        tableStatisticResultsColumn[2] = "" + tableStatisticColumnParts[0] + "_R2";
	                        tableStatisticResultsColumn[2] = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
	                            processor, ts, tableStatisticResultsColumn[2], status, commandPhase);
	                    }
	                    else {
	                        // Output will consist of single statistic corresponding to parameter-assigned column
	                        tableStatisticResultsColumn = new String[tableStatisticColumnParts.length];
	                        for ( int i = 0; i < tableStatisticColumnParts.length; i++ ) {
	                            tableStatisticResultsColumn[i] = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
	                                processor, ts, tableStatisticColumnParts[i], status, commandPhase);
	                        }
	                        // Some statistics like "Last" and "Max" also have the date
	                        if ( (tableStatisticDateTimeColumn != null) && !tableStatisticDateTimeColumn.isEmpty() ) {
	                        	tableStatisticDateTimeColumn = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
		                            processor, ts, tableStatisticDateTimeColumn, status, commandPhase);
	                        }
	                    }
	                }
                }
                
                try {
                    // Do the calculation...
                    TSStatisticType statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
                    TSUtil_CalculateTimeSeriesStatistic tsu = new TSUtil_CalculateTimeSeriesStatistic(ts, statisticType,
                        AnalysisStart_DateTime, AnalysisEnd_DateTime,
                        AnalysisWindowStart_DateTime, AnalysisWindowEnd_DateTime,
                        Value1_Double, Value2_Double, Value3_Double );
                    tsu.calculateTimeSeriesStatistic();
                    // Set the statistic as a property on the time series
                    if ( (TimeSeriesProperty != null) && !TimeSeriesProperty.isEmpty() ) {
                    	// TODO sam 2017-03-25 why is the following not using tsu.getStatisticResult()?
                    	setProperty ( ts, TimeSeriesProperty, tsu );
                    }
	                if ( (StatisticValueProperty != null) && !StatisticValueProperty.isEmpty() ) {
	                	String propName = TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, StatisticValueProperty, status, commandPhase);
	                	PropList request_params = new PropList ( "" );
	                    request_params.setUsingObject ( "PropertyName", propName );
	                    request_params.setUsingObject ( "PropertyValue", tsu.getStatisticResult() );
	                    try {
	                        processor.processRequest( "SetProperty", request_params);
	                    }
	                    catch ( Exception e ) {
	                        message = "Error requesting SetProperty(Property=\"" + StatisticValueProperty + "\") from processor.";
	                        Message.printWarning(log_level,
	                            MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                            routine, message );
	                        status.addToLog ( CommandPhaseType.RUN,
	                            new CommandLogRecord(CommandStatusType.FAILURE,
	                                message, "Report the problem to software support." ) );
	                    }
	                }
                    // Now set the statistic value(s) in the table by matching the row (via TSID) and column
                    // (via statistic column name)
                    if ( doTable ) {
                    	// Table will have been found or created above
                        if ( (TableStatisticColumn != null) && !TableStatisticColumn.isEmpty() ) {
                            // See if a matching row exists using the specified TSID column...
                            String tsid = null;
                            if ( (TableTSIDFormat != null) && !TableTSIDFormat.isEmpty() ) {
                                // Format the TSID using the specified format
                                tsid = ts.formatLegend ( TableTSIDFormat );
                            }
                            else {
                                // Use the alias if available and then the TSID
                                tsid = ts.getAlias();
                                if ( (tsid == null) || tsid.isEmpty() ) {
                                    tsid = ts.getIdentifierString();
                                }
                            }
                            Message.printStatus(2,routine, "Searching column \"" + TableTSIDColumn + "\" for TSID \"" +
                                tsid + "\"" );
                            TableRecord rec = table.getRecord ( TableTSIDColumn, tsid );
                            Message.printStatus(2,routine, "Searched column \"" + TableTSIDColumn + "\" for TSID \"" +
                                tsid + "\" ... found " + rec );
                            if ( rec == null ) {
                                // Add a blank record.
                                rec = table.addRecord(table.emptyRecord().setFieldValue(tableTSIDColumnNumber, tsid));
                            }
                            Class c = tsu.getStatisticDataClass();
                            for ( int iStat = 0; iStat < statisticColumnNum.length; iStat++ ) {
                                if ( statisticColumnNum[iStat] < 0 ) {
                                    // Have not previously checked for or added the column for the statistic
                                    try {
                                        statisticColumnNum[iStat] = table.getFieldIndex(tableStatisticResultsColumn[iStat]);
                                        // Have a column in the table.  Make sure that it matches in type the statistic
                                        if ( (c == Integer.class) &&
                                            (table.getFieldDataType(statisticColumnNum[iStat]) != TableField.DATA_TYPE_INT) ) {
                                            message = "Existing table column \"" + tableStatisticResultsColumn[iStat] +
                                                "\" is not of correct integer type for statistic";
                                            Message.printWarning ( warning_level,
                                                MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                                            status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.FAILURE,
                                                message, "Change the table column type or let this command create the column." ) );
                                            continue;
                                        }
                                        // TODO SAM 2013-02-10 Handle datetime and other types
                                        if ( ((c == Double.class) || (c == Regression.class)) &&
                                            (table.getFieldDataType(statisticColumnNum[iStat]) != TableField.DATA_TYPE_DOUBLE) ) {
                                            message = "Existing table column \"" + tableStatisticResultsColumn[iStat] +
                                                "\" is not of correct double type for statistic";
                                            Message.printWarning ( warning_level,
                                                MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                                            status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.FAILURE,
                                                message, "Change the table column type or let this command create the column." ) );
                                            continue;
                                        }
                                    }
                                    catch ( Exception e2 ) {
                                        // Column was not found.
                                        // Automatically add the statistic column to the table, initialize with null (not nonValue)
                                        // Create the column using an appropriate type for the statistic
                                        // This call is needed because the statistic could be null or NaN
                                        if ( c == Integer.class ) {
                                            statisticColumnNum[iStat] = table.addField(new TableField(TableField.DATA_TYPE_INT,tableStatisticResultsColumn[iStat],-1,-1), null );
                                        }
                                        else if ( c == DateTime.class ) {
                                            statisticColumnNum[iStat] = table.addField(new TableField(TableField.DATA_TYPE_DATETIME,tableStatisticResultsColumn[iStat],-1,-1), null );
                                        }
                                        else if ( c == Double.class ) {
                                            statisticColumnNum[iStat] = table.addField(new TableField(TableField.DATA_TYPE_DOUBLE,tableStatisticResultsColumn[iStat],10,4), null );
                                        }
                                        else if ( c == Regression.class ) {
                                            // Intercept, slope, and R2 all are doubles, column names were set up previously
                                            statisticColumnNum[iStat] = table.addField(new TableField(TableField.DATA_TYPE_DOUBLE,tableStatisticResultsColumn[iStat],10,4), null );
                                        }
                                        else {
                                            // Put this in to help software developers
                                            message = "Don't know how to handle statistic result class \"" + c +
                                                "\" for \"" + tableStatisticResultsColumn[iStat] + "\"";
                                            Message.printWarning ( warning_level,
                                                MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                                            status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.FAILURE,
                                                message, "See the log file for details - report the problem to software support." ) );
                                            continue;
                                        }
                                    }
                                    //Message.printStatus(2, routine, "Added column \"" + tableStatisticResultsColumn[iStat] +
                                    //    "\" to table in position " + statisticColumnNum[iStat] + " statistic count [" + iStat + "]" );
                                    // Also add a column for date/time if necessary
                                    if ( (tableStatisticDateTimeColumn != null) && !tableStatisticDateTimeColumn.isEmpty() &&
                                    	(statisticDateTimeColumnNum < 0) && tsu.getStatisticResultHasDateTime() ) {
                                    	statisticDateTimeColumnNum = table.addField(new TableField(TableField.DATA_TYPE_DATETIME,tableStatisticDateTimeColumn,-1,-1), null );
                                    	Message.printStatus(2, routine, "Added column \"" + tableStatisticDateTimeColumn +
                                            "\" to table in position " + statisticDateTimeColumnNum );
                                    }
                                }
                                // Set the value in the table column...
                                if ( statisticColumnNum.length == 1 ) {
                                    rec.setFieldValue(statisticColumnNum[iStat], tsu.getStatisticResult());
                                    if ( (statisticDateTimeColumnNum >= 0) && tsu.getStatisticResultHasDateTime() ) {
                                    	rec.setFieldValue(statisticDateTimeColumnNum, tsu.getStatisticResultDateTime());
                                    }
                                }
                                else {
                                    // A statistic with multiple results
                                    if ( statisticType == TSStatisticType.TREND_OLS ) {
                                        // Have 3 statistic values to set - an error computing regression will never
                                        // get to this point
                                        Regression r = (Regression)tsu.getStatisticResult();
                                        if ( iStat == 0 ) {
                                            rec.setFieldValue(statisticColumnNum[iStat], r.getA());
                                        }
                                        else if ( iStat == 1 ) {
                                            rec.setFieldValue(statisticColumnNum[iStat], r.getB());
                                        }
                                        else if ( iStat == 2 ) {
                                            rec.setFieldValue(statisticColumnNum[iStat],
                                                r.getCorrelationCoefficient()*r.getCorrelationCoefficient() );
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                catch ( Exception e ) {
                    message = "Unexpected error calculating time series statistic for \""+ ts.getIdentifier() + " (" + e + ").";
                    Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                    Message.printWarning(3,routine,e);
                    status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "See the log file for details - report the problem to software support." ) );
                }
            }
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error computing statistic for time series (" + e + ").";
        Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check log file for details." ) );
        throw new CommandException ( message );
    }
    
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
        throw new CommandWarningException ( message );
    }
    
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __table = table;
}

/**
Set the property on the time series.
*/
private void setProperty ( TS ts, String propertyName, TSUtil_CalculateTimeSeriesStatistic tsu )
{
	ts.setProperty(propertyName,tsu.getStatisticResult());
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
    String Value1 = parameters.getValue( "Value1" );
    String Value2 = parameters.getValue( "Value2" );
    String Value3 = parameters.getValue( "Value3" );
    String AnalysisStart = parameters.getValue( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue( "AnalysisEnd" );
    String AnalysisWindowStart = parameters.getValue( "AnalysisWindowStart" );
    String AnalysisWindowEnd = parameters.getValue( "AnalysisWindowEnd" );
    String IfNotFound = parameters.getValue ( "IfNotFound" );
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TableStatisticColumn = parameters.getValue ( "TableStatisticColumn" );
    String TableStatisticDateTimeColumn = parameters.getValue ( "TableStatisticDateTimeColumn" );
    String TimeSeriesProperty = parameters.getValue ( "TimeSeriesProperty" );
    String StatisticValueProperty = parameters.getValue ( "StatisticValueProperty" );
        
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
    if ( (Value1 != null) && (Value1.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Value1=" + Value1 );
    }
    if ( (Value2 != null) && (Value2.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Value2=" + Value2 );
    }
    if ( (Value3 != null) && (Value3.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Value3=" + Value3 );
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
    if ( (TableStatisticDateTimeColumn != null) && !TableStatisticDateTimeColumn.isEmpty() ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableStatisticDateTimeColumn=\"" + TableStatisticDateTimeColumn + "\"" );
    }
    if ( (TimeSeriesProperty != null) && (TimeSeriesProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TimeSeriesProperty=\"" + TimeSeriesProperty + "\"" );
    }
    if ( (StatisticValueProperty != null) && (StatisticValueProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "StatisticValueProperty=\"" + StatisticValueProperty + "\"" );
    }
    
    return getCommandName() + "(" + b.toString() + ")";
}

}