package rti.tscommandprocessor.commands.check;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.TS.CheckType;
import RTi.TS.TS;
import RTi.TS.TSUtil_CheckTimeSeries;
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
import RTi.Util.Table.DataTable;
import RTi.Util.Time.DateTime;

// TODO SAM 2015-06-06 Add Tolerance parameter to add buffer for == and Repeat.
// Add parameter to format message that accepts ${ts:value}, etc.
/**
This class initializes, checks, and runs the CheckTimeSeries() command.
*/
public class CheckTimeSeries_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Analysis window year, since users do not supply this information.
This allows for leap year in case the analysis window start or end is on Feb 29.
*/
private final int __ANALYSIS_WINDOW_YEAR = 2000;
    
/**
Values for Action parameter.
*/
protected final String _Remove = "Remove";
protected final String _SetMissing = "SetMissing";

/**
The table that is created for discovery mode.
*/
private DataTable __discoveryTable = null;

/**
Constructor.
*/
public CheckTimeSeries_Command ()
{   super();
    setCommandName ( "CheckTimeSeries" );
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
{   //String TSID = parameters.getValue ( "TSID" );
    String CheckCriteria = parameters.getValue ( "CheckCriteria" );
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
    String AnalysisWindowStart = parameters.getValue ( "AnalysisWindowStart" );
    String AnalysisWindowEnd = parameters.getValue ( "AnalysisWindowEnd" );
    String Value1 = parameters.getValue ( "Value1" );
    String Value2 = parameters.getValue ( "Value2" );
    String MaxWarnings = parameters.getValue ( "MaxWarnings" );
    String Action = parameters.getValue ( "Action" );
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TableValuePrecision = parameters.getValue ( "TableValuePrecision" );
    String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

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
            if ( (Value1 == null) || Value1.equals("") ) {
                message = "Value1 must be specified for the criteria.";
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
        
        if ( nRequiredValues == 2 ) {
            if ( (Value2 == null) || Value2.equals("") ) {
                message = "Value2 must be specified for the criteria.";
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
    }

    if ( (MaxWarnings != null) && !StringUtil.isInteger(MaxWarnings) ) {
        message = "MaxWarnings (" + MaxWarnings + ") is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify MaxWarnings as an integer." ) );
    }
    
    if ( (Action != null) && !Action.isEmpty() &&
        !Action.equalsIgnoreCase(_Remove) && !Action.equalsIgnoreCase(_SetMissing) ) {
            message = "The action \"" + Action + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the action as " + _Remove + " or " + _SetMissing + ".") );
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
        if ( (TableValuePrecision != null) && !TableValuePrecision.equals("") && !StringUtil.isInteger(TableValuePrecision) ) {
            message = "The table value precision (" + TableValuePrecision + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the precision as an integer." ) );
        }
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(26);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "CheckCriteria" );
    validList.add ( "Value1" );
    validList.add ( "Value2" );
    validList.add ( "ProblemType" );
    validList.add ( "MaxWarnings" );
    validList.add ( "Flag" );
    validList.add ( "FlagDesc" );
    validList.add ( "Action" );
    validList.add ( "AnalysisStart" );
    validList.add ( "AnalysisEnd" );
    validList.add ( "AnalysisWindowStart" );
    validList.add ( "AnalysisWindowEnd" );
    validList.add ( "TableID" );
    validList.add ( "TableTSIDColumn" );
    validList.add ( "TableTSIDFormat" );
    validList.add ( "TableDateTimeColumn" );
    validList.add ( "TableValueColumn" );
    validList.add ( "TableValuePrecision" );
    validList.add ( "TableFlagColumn" );
    validList.add ( "TableCheckTypeColumn" );
    validList.add ( "TableCheckMessageColumn" );
    validList.add ( "CheckCountProperty" );
    validList.add ( "CheckCountTimeSeriesProperty" );
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
{   List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
	return (new CheckTimeSeries_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __discoveryTable;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable table = getDiscoveryTable();
    List v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector();
        v.add ( table );
    }
    return v;
}

// Parse command is in the base class

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
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
throws InvalidCommandParameterException,
CommandWarningException, CommandException
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
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String CheckCriteria = parameters.getValue ( "CheckCriteria" );
    CheckType checkCriteria = CheckType.valueOfIgnoreCase(CheckCriteria);
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
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
    String AnalysisWindowStart = parameters.getValue ( "AnalysisWindowStart" );
    String AnalysisWindowEnd = parameters.getValue ( "AnalysisWindowEnd" );
    String ProblemType = parameters.getValue ( "ProblemType" );
    if ( (ProblemType ==null) || ProblemType.equals("") ) {
        ProblemType = CheckCriteria; // Default
    }
    String MaxWarnings = parameters.getValue ( "MaxWarnings" );
    int MaxWarnings_int = -1;
    if ( (MaxWarnings != null) && !MaxWarnings.equals("") ) {
        MaxWarnings_int = Integer.parseInt(MaxWarnings);
    }
    String Flag = parameters.getValue ( "Flag" );
    String FlagDesc = parameters.getValue ( "FlagDesc" );
    String Action = parameters.getValue ( "Action" );
    String TableID = parameters.getValue ( "TableID" );
	if ( (TableID != null) && (TableID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
	}
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableDateTimeColumn = parameters.getValue ( "TableDateTimeColumn" );
    String TableValueColumn = parameters.getValue ( "TableValueColumn" );
    String TableValuePrecision = parameters.getValue ( "TableValuePrecision" );
    int tableValuePrecision = 4; // Default
    if ( (TableValuePrecision != null) && !TableValuePrecision.isEmpty() ) {
    	tableValuePrecision = Integer.parseInt(TableValuePrecision);
    }
    String TableFlagColumn = parameters.getValue ( "TableFlagColumn" );
    String TableCheckTypeColumn = parameters.getValue ( "TableCheckTypeColumn" );
    String TableCheckMessageColumn = parameters.getValue ( "TableCheckMessageColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String CheckCountProperty = parameters.getValue ( "CheckCountProperty" );
    String CheckCountTimeSeriesProperty = parameters.getValue ( "CheckCountTimeSeriesProperty" );
    
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

    // Get the time series to process.  Allow TSID to be a pattern or specific time series...
    
    List tslist = null;
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
	        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
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
    }

    if ( tslist == null ) {
    	tslist = new ArrayList<TS>();
    }
    int nts = tslist.size();
    if ( nts == 0 ) {
    	if ( commandPhase == CommandPhaseType.RUN ) {
	        message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
	            "\", EnsembleID=\"" + EnsembleID + "\".";
	        Message.printWarning ( warning_level,
	        MessageUtil.formatMessageTag(
	        command_tag,++warning_count), routine, message );
	        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
	            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    	}
    }
    
    // Get the table to process.

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
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }
    
    // Now process...
    
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
    
    try {
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	if ( doTable ) {
	            if ( table == null ) {
	                // Did not find table so is being created in this command
	                // Create an empty table and set the ID
	                table = new DataTable();
	                table.setTableID ( TableID );
	                setDiscoveryTable ( table );
	            }
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
	            notifyCommandProgressListeners ( its, nts, (float)-1.0, "Checking time series " +
	                ts.getIdentifier().toStringAliasAndTSID() );
	            
	            try {
	                // Do the check...
	                TSUtil_CheckTimeSeries check = new TSUtil_CheckTimeSeries(ts, checkCriteria,
	                    AnalysisStart_DateTime, AnalysisEnd_DateTime,
	                    AnalysisWindowStart_DateTime, AnalysisWindowEnd_DateTime,
	                    Value1_Double, Value2_Double, ProblemType,
	                    Flag, FlagDesc, Action, table, TableTSIDColumn, TableTSIDFormat, TableDateTimeColumn,
	                    TableValueColumn, tableValuePrecision, TableFlagColumn, TableCheckTypeColumn, TableCheckMessageColumn );
	                check.checkTimeSeries();
	                List<String> problems = check.getProblems();
	                int problemsSize = problems.size();
	                int problemsSizeOutput = problemsSize;
	                if ( (MaxWarnings_int > 0) && (problemsSize > MaxWarnings_int) ) {
	                    // Limit the warnings to the maximum
	                    problemsSizeOutput = MaxWarnings_int;
	                }
	                if ( problemsSizeOutput < problemsSize ) {
	                    message = "Time series had " + problemsSize + " check warnings - only " + problemsSizeOutput + " are listed.";
	                    Message.printWarning ( warning_level,
	                        MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
	                    // No recommendation since it is a user-defined check
	                    // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
	                    status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.WARNING, ProblemType, message, "" ) );
	                }
	                for ( int iprob = 0; iprob < problemsSizeOutput; iprob++ ) {
	                    message = problems.get(iprob);
	                    Message.printWarning ( warning_level,
	                        MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
	                    // No recommendation since it is a user-defined check
	                    // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
	                    status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.WARNING, ProblemType, message, "" ) );
	                }
	                int checkCriteriaMetCount = check.getCheckCriteriaMetCount();
	                if ( (CheckCountProperty != null) && !CheckCountProperty.isEmpty() ) {
	                	String propName = TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, CheckCountProperty, status, commandPhase);
	                	PropList request_params = new PropList ( "" );
	                    request_params.setUsingObject ( "PropertyName", propName );
	                    request_params.setUsingObject ( "PropertyValue", new Integer(checkCriteriaMetCount) );
	                    try {
	                        processor.processRequest( "SetProperty", request_params);
	                    }
	                    catch ( Exception e ) {
	                        message = "Error requesting SetProperty(Property=\"" + CheckCountProperty + "\") from processor.";
	                        Message.printWarning(log_level,
	                            MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                            routine, message );
	                        status.addToLog ( CommandPhaseType.RUN,
	                            new CommandLogRecord(CommandStatusType.FAILURE,
	                                message, "Report the problem to software support." ) );
	                    }
	                }
	                if ( (CheckCountTimeSeriesProperty != null) && !CheckCountTimeSeriesProperty.isEmpty() ) {
	                	String propName = TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, CheckCountTimeSeriesProperty, status, commandPhase);
	                	ts.setProperty(propName, new Integer(checkCriteriaMetCount));
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
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __discoveryTable = table;
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
    String CheckCriteria = parameters.getValue( "CheckCriteria" );
    String Value1 = parameters.getValue( "Value1" );
    String Value2 = parameters.getValue( "Value2" );
    String ProblemType = parameters.getValue( "ProblemType" );
    String MaxWarnings = parameters.getValue( "MaxWarnings" );
    String Flag = parameters.getValue( "Flag" );
    String FlagDesc = parameters.getValue( "FlagDesc" );
    String Action = parameters.getValue ( "Action" );
    String AnalysisStart = parameters.getValue( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue( "AnalysisEnd" );
    String AnalysisWindowStart = parameters.getValue( "AnalysisWindowStart" );
    String AnalysisWindowEnd = parameters.getValue( "AnalysisWindowEnd" );
	String TableID = parameters.getValue ( "TableID" );
	String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
	String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
	String TableDateTimeColumn = parameters.getValue ( "TableDateTimeColumn" );
	String TableValueColumn = parameters.getValue ( "TableValueColumn" );
	String TableValuePrecision = parameters.getValue ( "TableValuePrecision" );
	String TableFlagColumn = parameters.getValue ( "TableFlagColumn" );
	String TableCheckTypeColumn = parameters.getValue ( "TableCheckTypeColumn" );
	String TableCheckMessageColumn = parameters.getValue ( "TableCheckMessageColumn" );
    String CheckCountProperty = parameters.getValue ( "CheckCountProperty" );
    String CheckCountTimeSeriesProperty = parameters.getValue ( "CheckCountTimeSeriesProperty" );
        
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
    if ( (CheckCriteria != null) && (CheckCriteria.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CheckCriteria=\"" + CheckCriteria + "\"" );
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
    if ( (ProblemType != null) && (ProblemType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ProblemType=\"" + ProblemType + "\"" );
    }
    if ( (MaxWarnings != null) && (MaxWarnings.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MaxWarnings=" + MaxWarnings  );
    }
    if ( (Flag != null) && (Flag.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Flag=\"" + Flag + "\"" );
    }
    if ( (FlagDesc != null) && (FlagDesc.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FlagDesc=\"" + FlagDesc + "\"" );
    }
    if ( Action != null && Action.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Action=" + Action );
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
    if ( (TableDateTimeColumn != null) && (TableDateTimeColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableDateTimeColumn=\"" + TableDateTimeColumn + "\"" );
    }
    if ( (TableValueColumn != null) && (TableValueColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableValueColumn=\"" + TableValueColumn + "\"" );
    }
    if ( (TableValuePrecision != null) && (TableValuePrecision.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableValuePrecision=" + TableValuePrecision );
    }
    if ( (TableFlagColumn != null) && (TableFlagColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableFlagColumn=\"" + TableFlagColumn + "\"" );
    }
    if ( (TableCheckTypeColumn != null) && (TableCheckTypeColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableCheckTypeColumn=\"" + TableCheckTypeColumn + "\"" );
    }
    if ( (TableCheckMessageColumn != null) && (TableCheckMessageColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableCheckMessageColumn=\"" + TableCheckMessageColumn + "\"" );
    }
    if ( CheckCountProperty != null && CheckCountProperty.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CheckCountProperty=\"" + CheckCountProperty + "\"");
    }
    if ( CheckCountTimeSeriesProperty != null && CheckCountTimeSeriesProperty.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CheckCountTimeSeriesProperty=\"" + CheckCountTimeSeriesProperty + "\"");
    }
    
    return getCommandName() + "(" + b.toString() + ")";
}

}