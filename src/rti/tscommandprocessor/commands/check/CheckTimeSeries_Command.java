package rti.tscommandprocessor.commands.check;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

import RTi.TS.CheckType;
import RTi.TS.TS;
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
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the CheckTimeSeries() command.
*/
public class CheckTimeSeries_Command extends AbstractCommand implements Command
{
    
/**
Values for Action parameter.
*/
protected final String _Remove = "Remove";
protected final String _SetMissing = "SetMissing";

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
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{   //String TSID = parameters.getValue ( "TSID" );
    String CheckCriteria = parameters.getValue ( "CheckCriteria" );
    String AnalysisStart = parameters.getValue ( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
    String Value1 = parameters.getValue ( "Value1" );
    String Value2 = parameters.getValue ( "Value2" );
    String MaxWarnings = parameters.getValue ( "MaxWarnings" );
    String Action = parameters.getValue ( "Action" );
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
    
    if ( (MaxWarnings != null) && !StringUtil.isInteger(MaxWarnings) ) {
        message = "MaxWarnings (" + MaxWarnings + ") is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify MaxWarnings as an integer." ) );
    }
    
    if ( (Action != null) && !Action.equals("") &&
        !Action.equalsIgnoreCase(_Remove) && !Action.equalsIgnoreCase(_SetMissing) ) {
            message = "The action \"" + Action + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the action as " + _Remove + " or " + _SetMissing + ".") );
    }
    
    // Check for invalid parameters...
    List<String> valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "CheckCriteria" );
    valid_Vector.add ( "Value1" );
    valid_Vector.add ( "Value2" );
    valid_Vector.add ( "AnalysisStart" );
    valid_Vector.add ( "AnalysisEnd" );
    valid_Vector.add ( "ProblemType" );
    valid_Vector.add ( "MaxWarnings" );
    valid_Vector.add ( "Flag" );
    valid_Vector.add ( "FlagDesc" );
    valid_Vector.add ( "Action" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{   return (new CheckTimeSeries_JDialog ( parent, this )).ok();
}

// Parse command is in the base class

/**
Method to execute the setIrrigationPracticeTSPumpingMaxUsingWellRights() command.
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
    int log_level = 3;  // Level for non-use messages for log file.
    
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

    // Figure out the dates to use for the analysis.
    // Default of null means to analyze the full period.
    DateTime AnalysisStart_DateTime = null;
    DateTime AnalysisEnd_DateTime = null;
    
    try {
        if ( AnalysisStart != null ) {
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
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
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
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
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
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }
    
    try {
        if ( AnalysisEnd != null ) {
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
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
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
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
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
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }

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
            notifyCommandProgressListeners ( its, nts, (float)-1.0, "Checking time series " +
                ts.getIdentifier().toStringAliasAndTSID() );
            
            try {
                // Do the check...
                TSUtil_CheckTimeSeries check = new TSUtil_CheckTimeSeries(ts, checkCriteria,
                    AnalysisStart_DateTime, AnalysisEnd_DateTime, Value1_Double, Value2_Double, ProblemType,
                    Flag, FlagDesc, Action );
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
    String CheckCriteria = parameters.getValue( "CheckCriteria" );
    String Value1 = parameters.getValue( "Value1" );
    String Value2 = parameters.getValue( "Value2" );
    String AnalysisStart = parameters.getValue( "AnalysisStart" );
    String AnalysisEnd = parameters.getValue( "AnalysisEnd" );
    String ProblemType = parameters.getValue( "ProblemType" );
    String MaxWarnings = parameters.getValue( "MaxWarnings" );
    String Flag = parameters.getValue( "Flag" );
    String FlagDesc = parameters.getValue( "FlagDesc" );
    String Action = parameters.getValue ( "Action" );
        
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
    
    return getCommandName() + "(" + b.toString() + ")";
}

}