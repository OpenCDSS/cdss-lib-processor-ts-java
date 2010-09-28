package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSUtil;
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
import RTi.Util.Table.DataTable;
import RTi.Util.Table.DataTableMathOperatorType;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the TableMath() command.
*/
public class TableTimeSeriesMath_Command extends AbstractCommand implements Command
{
    
/**
Values for NonValue parameter.
*/
protected final String _NaN = "NaN";
protected final String _Null = "Null";

/**
Constructor.
*/
public TableTimeSeriesMath_Command ()
{   super();
    setCommandName ( "TableTimeSeriesMath" );
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
{   String Operator = parameters.getValue ( "Operator" );
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableInputColumn = parameters.getValue ( "TableInputColumn" );
    String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (Operator == null) || Operator.equals("") ) {
        message = "The operator must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the operator to process input." ) );
    }
    else {
        // Make sure that the operator is known in general
        boolean supported = false;
        DataTableMathOperatorType operatorType = null;
        try {
            operatorType = DataTableMathOperatorType.valueOfIgnoreCase(Operator);
            supported = true;
        }
        catch ( Exception e ) {
            message = "The operator (" + Operator + ") is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported operator using the command editor." ) );
        }
        
        // Make sure that it is in the supported list
        
        if ( supported ) {
            supported = false;
            List<DataTableMathOperatorType> operators = getOperatorChoices();
            for ( int i = 0; i < operators.size(); i++ ) {
                if ( operatorType == operators.get(i) ) {
                    supported = true;
                }
            }
            if ( !supported ) {
                message = "The operator (" + Operator + ") is not supported by this command.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Select a supported operator using the command editor." ) );
            }
        }
    }
    
    if ( (TableID == null) || TableID.equals("") ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the identifier for the table to process." ) );
    }
    
    if ( (TableTSIDColumn == null) || TableTSIDColumn.equals("") ) {
        message = "The TableTSID column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a table column name for the TSID." ) );
    }
    
    if ( (TableInputColumn == null) || TableInputColumn.equals("") ) {
        message = "The table input column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a table column name for the input value." ) );
    }
    
    // Check for invalid parameters...
    List<String> valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "Operator" );
    valid_Vector.add ( "TableID" );
    valid_Vector.add ( "TableTSIDColumn" );
    valid_Vector.add ( "TableTSIDFormat" );
    valid_Vector.add ( "TableInputColumn" );
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
{   List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    return (new TableTimeSeriesMath_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Get the list of operators that can be used.
*/
public List<DataTableMathOperatorType> getOperatorChoices()
{
    List<DataTableMathOperatorType> choices = new Vector();
    choices.add ( DataTableMathOperatorType.ADD );
    choices.add ( DataTableMathOperatorType.SUBTRACT );
    choices.add ( DataTableMathOperatorType.MULTIPLY );
    choices.add ( DataTableMathOperatorType.DIVIDE );
    return choices;
}

// Parse command is in the base class

/**
Method to execute the command.
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
    int log_level = 3;
    //int log_level = 3;  // Level for non-use messages for log file.
    
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
    String Operator = parameters.getValue ( "Operator" );
    DataTableMathOperatorType operator = DataTableMathOperatorType.valueOfIgnoreCase(Operator);
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TableInputColumn = parameters.getValue ( "TableInputColumn" );

    // Get the table to process.

    DataTable table = null;
    PropList request_params = null;
    CommandProcessorRequestResultsBean bean = null;
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
        PropList bean_PropList = bean.getResultsPropList();
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
    
    // Get the column from the table to be used as input...
    
    int tableInputColumn = -1;
    try {
        tableInputColumn = table.getFieldIndex(TableInputColumn);
    }
    catch ( Exception e2 ) {
        message = "Table \"" + TableID + "\" does not have column \"" + TableInputColumn + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that a table exists with the requested input column." ) );
    }
    
    // Get the time series to process.  Allow TSID to be a pattern or specific time series...

    request_params = new PropList ( "" );
    request_params.set ( "TSList", TSList );
    request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
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
        Object tableObject; // The table value as a generic object
        Double tableValue; // The value used to perform math
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
                // Get the value from the table
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
                TableRecord rec = table.getRecord ( TableTSIDColumn, tsid );
                if ( rec == null ) {
                    message = "Cannot find table cell in column \"" + TableTSIDColumn +
                        "\" matching TSID formatted as \"" + tsid + "\" - skipping time series \"" +
                        ts.getIdentifierString() + "\".";
                    Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
                        "Verify that the column TSID matches one or more time series." ) );
                    // Go to next time series.
                    continue;
                }
                // Get the value from the table...
                tableObject = rec.getFieldValue(tableInputColumn);
                // Allow the value to be any number
                if ( tableObject == null ) {
                    message = "Table value in column \"" + TableInputColumn +
                    "\" matching TSID \"" + tsid + "\" is null - skipping time series \"" +
                    ts.getIdentifierString() + "\".";
                    Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
                        "Verify that the proper table input column is specified and that column values are numbers." ) );
                    // Go to next time series.
                    continue;
                }
                else if ( !StringUtil.isDouble("" + tableObject) ) {
                    message = "Table value in column \"" + TableInputColumn +
                    "\" matching TSID \"" + tsid + "\" (" + tableObject +
                    ") is not a number - skipping time series \"" + ts.getIdentifierString() + "\".";
                    Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
                        "Verify that the proper table input column is specified and that column values are numbers." ) );
                    // Go to next time series.
                    continue;
                }
                else {
                    // Have a numerical value, but re-parse in case the original is an Integer, etc.
                    tableValue = Double.parseDouble("" + tableObject );
                    if ( tableValue.isNaN() ) {
                        message = "Table value in column \"" + TableInputColumn +
                        "\" matching TSID \"" + tsid + "\" is NaN - skipping time series \"" +
                        ts.getIdentifierString() + "\".";
                        Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            routine, message );
                        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
                            "Verify that the proper table input column is specified and that column values are numbers." ) );
                        // Go to next time series.
                        continue;
                    }
                }
                // Do the calculation...
                if ( operator == DataTableMathOperatorType.ADD ) {
                    ts.addToGenesis("Table \"" + TableID + "\" column \"" + TableInputColumn +
                        "\" value " + tableValue + " used for add." );
                    TSUtil.addConstant(ts, null, null, tableValue);
                }
                else if ( operator == DataTableMathOperatorType.SUBTRACT ) {
                    ts.addToGenesis("Table \"" + TableID + "\" column \"" + TableInputColumn +
                        "\" value " + tableValue + " used for subtract." );
                    TSUtil.addConstant(ts, null, null, -tableValue);
                }
                else if ( operator == DataTableMathOperatorType.MULTIPLY ) {
                    ts.addToGenesis("Table \"" + TableID + "\" column \"" + TableInputColumn +
                        "\" value " + tableValue + " used for multiply." );
                    TSUtil.scale(ts, null, null, tableValue);
                }
                else if ( (operator == DataTableMathOperatorType.DIVIDE) && (tableValue != 0.0) ) {
                    ts.addToGenesis("Table \"" + TableID + "\" column \"" + TableInputColumn +
                        "\" value " + tableValue + " used for divide (multiply 1/" + tableValue + "." );
                    TSUtil.scale(ts, null, null, 1.0/tableValue);
                }
            }
            catch ( Exception e ) {
                message = "Unexpected error processing time series \""+ ts.getIdentifier() + " (" + e + ").";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                Message.printWarning(3,routine,e);
                status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
            }
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error processing time series (" + e + ").";
        Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check log file for details." ) );
        throw new CommandException ( message );
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
    String Operator = parameters.getValue( "Operator" );
    String TableID = parameters.getValue( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TableInputColumn = parameters.getValue ( "TableInputColumn" );
        
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
    if ( (Operator != null) && (Operator.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Operator=\"" + Operator + "\"" );
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
    if ( (TableInputColumn != null) && (TableInputColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableInputColumn=\"" + TableInputColumn + "\"" );
    }
    
    return getCommandName() + "(" + b.toString() + ")";
}

}