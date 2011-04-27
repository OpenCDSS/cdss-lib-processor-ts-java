package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
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
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the CopyTimeSeriesPropertiesToTable() command.
*/
public class CopyTimeSeriesPropertiesToTable_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public CopyTimeSeriesPropertiesToTable_Command ()
{   super();
    setCommandName ( "CopyTimeSeriesPropertiesToTable" );
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
{   String PropertyNames = parameters.getValue ( "PropertyNames" );
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableOutputColumns = parameters.getValue ( "TableOutputColumns" );
    String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
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
    
    if ( (PropertyNames != null) && !PropertyNames.equals("") &&
        (TableOutputColumns != null) && !TableOutputColumns.equals("") ) {
        String[] propertyNames = PropertyNames.split(",");
        String[] tableOutputColumns = TableOutputColumns.split(",");
        if ( propertyNames.length != tableOutputColumns.length ) {
            message = "The number of specified property names (" +
                ") and the number of specified table output columns (" + ") is different.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the same number of property names as output columns." ) );
        }
    }
    
    // Check for invalid parameters...
    List<String> valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "PropertyNames" );
    valid_Vector.add ( "TableID" );
    valid_Vector.add ( "TableTSIDColumn" );
    valid_Vector.add ( "TableTSIDFormat" );
    valid_Vector.add ( "TableOutputColumns" );
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
    return (new CopyTimeSeriesPropertiesToTable_JDialog ( parent, this, tableIDChoices )).ok();
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
    String PropertyNames = parameters.getValue ( "PropertyNames" );
    String [] propertyNames = null;
    if ( (PropertyNames != null) && !PropertyNames.equals("") ) {
        propertyNames = PropertyNames.trim().split(",");
    }
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TableOutputColumns = parameters.getValue ( "TableOutputColumns" );
    String [] tableOutputColumnNames = null;
    if ( (TableOutputColumns != null) && !TableOutputColumns.equals("") ) {
        tableOutputColumnNames = TableOutputColumns.split(",");
    }

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
    List<TS> tslist = null;
    if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
        Message.printWarning ( log_level, MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    }
    else {
        tslist = (List<TS>)o_TSList;
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
            
            // Get the properties to process
            if ( propertyNames == null ) {
                // Get all the properties by forming a list of property names from the hasthtable
                Hashtable<String, Object> propertyHash = ts.getProperties();
                propertyNames = propertyHash.keySet().toArray(new String[0]);
            }
            // Set the column names from the time series properties
            if ( tableOutputColumnNames == null ) {
                tableOutputColumnNames = propertyNames;
            }
            else {
                // Check for wildcards
                for ( int icolumn = 0; icolumn < propertyNames.length; icolumn++ ) {
                    if ( tableOutputColumnNames[icolumn].equals("*") ) {
                        // Output column name is the same as the property name
                        tableOutputColumnNames[icolumn] = propertyNames[icolumn];
                    }
                }
            }
            
            // Get the table column numbers corresponding to the column names...
            
            // Get the columns from the table to be used as output...
            
            int [] tableOutputColumns = new int[tableOutputColumnNames.length];
            for ( int i = 0; i < tableOutputColumns.length; i++ ) {
                try {
                    tableOutputColumns[i] = table.getFieldIndex(tableOutputColumnNames[i]);
                }
                catch ( Exception e2 ) {
                    message = "Table \"" + TableID + "\" does not have column \"" + tableOutputColumnNames[i] + "\".";
                    Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                    status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that a table exists with the requested output column." ) );
                    // Skip the time series...
                    continue;
                }
            }
            
            // Loop through the property names...
            
            for ( int icolumn = 0; icolumn < propertyNames.length; icolumn++ ) {
                String propertyName = propertyNames[icolumn];
                Object propertyValue = ts.getProperty(propertyName);
                // If the property value is null, just skip setting it
                // TODO SAM 2011-04-27 Should this be a warning?
                if ( propertyValue == null ) {
                    continue;
                }
                // Get the matching table column
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
                        message = "Cannot find table \"" + TableID + "\" cell in column \"" + TableTSIDColumn +
                            "\" matching TSID formatted as \"" + tsid + "\" - skipping time series \"" +
                            ts.getIdentifierString() + "\".";
                        Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            routine, message );
                        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
                            "Verify that table \"" + TableID + "\" column TSID matches one or more time series." ) );
                        // Go to next time series.
                        continue;
                    }
                    // Make sure that the table has the specified column...
                    int colNumber = table.getFieldIndex(tableOutputColumnNames[icolumn]);
                    if ( colNumber < 0 ) {
                        message = "Table \"" + TableID +
                        "\" does not have column \"" + tableOutputColumnNames[icolumn] +
                        "\".";
                        Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            routine, message );
                        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify that the proper table output column is specified and has been defined." ) );
                        continue;
                    }
                    // Set the value in the table...
                    try {
                        rec.setFieldValue(tableOutputColumns[icolumn],propertyValue);
                        // TODO SAM 2011-04-27 Evaluate why the column width is necessary in the data table
                        // Reset the column width if necessary
                        if ( propertyValue instanceof String ) {
                            // If the incoming string is longer than the column width, reset the column width
                            int width = table.getFieldWidth(tableOutputColumns[icolumn]);
                            table.setFieldWidth(tableOutputColumns[icolumn],
                                Math.max(width,((String)propertyValue).length()));
                        }
                    }
                    catch ( Exception e ) {
                        // Blank cell values are allowed - just don't set the property
                        message = "Unable to set " + propertyName + "=" + propertyValue + " in table \"" + TableID +
                            "\" column \"" + tableOutputColumnNames[icolumn] +
                            "\" matching TSID \"" + tsid + " (" + ts.getIdentifier().toStringAliasAndTSID() + "\") (" + e + ").";
                        Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            routine, message );
                        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING, message,
                            "Verify that the proper table output column is specified and has been defined." ) );
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
    String PropertyNames = parameters.getValue( "PropertyNames" );
    String TableID = parameters.getValue( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TableOutputColumns = parameters.getValue ( "TableOutputColumns" );
        
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
    if ( (PropertyNames != null) && (PropertyNames.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PropertyNames=\"" + PropertyNames + "\"" );
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
    if ( (TableOutputColumns != null) && (TableOutputColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableOutputColumns=\"" + TableOutputColumns + "\"" );
    }
    
    return getCommandName() + "(" + b.toString() + ")";
}

}