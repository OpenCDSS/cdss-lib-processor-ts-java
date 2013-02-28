package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import RTi.DMI.DMI;
import RTi.DMI.DMIUtil;
import RTi.DMI.DMIWriteModeType;
import RTi.DMI.DMIWriteStatement;
import RTi.DMI.DatabaseDataStore;
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
import RTi.Util.Table.TableColumnType;
import RTi.Util.Table.TableField;

/**
This class initializes, checks, and runs the WriteTableToDataStore() command.
*/
public class WriteTableToDataStore_Command extends AbstractCommand implements Command
{
    
/**
Constructor.
*/
public WriteTableToDataStore_Command ()
{	super();
	setCommandName ( "WriteTableToDataStore" );
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
{   String DataStore = parameters.getValue ( "DataStore" );
    String DataStoreTable = parameters.getValue ( "DataStoreTable" );
    String TableID = parameters.getValue ( "TableID" );
    String WriteMode = parameters.getValue ( "WriteMode" );

	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The output table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the output table identifier." ) );
    }
    if ( (DataStore == null) || (DataStore.length() == 0) ) {
        message = "The datastore must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the datastore." ) );
    }
    if ( (DataStoreTable == null) || (DataStoreTable.length() == 0) ) {
        message = "The datastore table must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the datastore table." ) );
    }
    if ( (WriteMode != null) && (DMIWriteModeType.valueOf(WriteMode) == null) ) {
        message = "The write mode (" + WriteMode + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid write mode." ) );
    }
    
	//  Check for invalid parameters...
	List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "TableID" );
    valid_Vector.add ( "IncludeColumns" );
    valid_Vector.add ( "DataStore" );
    valid_Vector.add ( "DataStoreTable" );
    valid_Vector.add ( "WriteMode" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );    

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
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
{	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed...
	return (new WriteTableToDataStore_JDialog ( parent, this, tableIDChoices )).ok();
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    status.clearLog(commandPhase);

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String TableID = parameters.getValue ( "TableID" );
    String IncludeColumns = parameters.getValue ( "IncludeColumns" );
    String [] includeColumns = null;
    if ( IncludeColumns != null ) {
        if ( IncludeColumns.indexOf(",") > 0 ) {
            includeColumns = IncludeColumns.split(",");
        }
        else {
            includeColumns = new String[1];
            includeColumns[0] = IncludeColumns;
        }
        for ( int i = 0; i < includeColumns.length; i++ ) {
            includeColumns[i] = includeColumns[i].trim();
        }
    }
    String DataStore = parameters.getValue ( "DataStore" );
    String DataStoreTable = parameters.getValue ( "DataStoreTable" );
    if ( (DataStoreTable != null) && DataStoreTable.equals("") ) {
        DataStoreTable = null; // Simplifies logic below
    }
    String WriteMode = parameters.getValue ( "WriteMode" );
    DMIWriteModeType writeMode = DMIWriteModeType.valueOfIgnoreCase(WriteMode);
    if ( writeMode == null ) {
        writeMode = DMIWriteModeType.UPDATE_INSERT; // default
    }
    
    // Find the datastore to use...
    DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
        DataStore, DatabaseDataStore.class );
    DMI dmi = null;
    if ( dataStore == null ) {
        message = "Could not get datastore for name \"" + DataStore + "\" to query data.";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a database connection has been opened with name \"" +
                DataStore + "\"." ) );
    }
    else {
        dmi = ((DatabaseDataStore)dataStore).getDMI();
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
    
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

    String sqlString = "";
    try {
        // Always get the columns from the database to check parameters, to guard against SQL injection
        List<String> datastoreTableColumns = new Vector<String>();
        try {
            datastoreTableColumns = DMIUtil.getTableColumns(dmi,DataStoreTable);
        }
        catch ( Exception e ) {
            message = "Error getting table columns for table \"" + DataStoreTable + "\".";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the database for datastore \"" + DataStore +
                    "\" is accessible.") );
        }
        // Only include columns that are requested to be included
        int numTableColumns = table.getNumberOfFields();
        boolean [] columnOkToWrite = new boolean[numTableColumns];
        String [] tableFieldNames = table.getFieldNames();
        int [] tableColumnTypes = table.getFieldDataTypes();
        for ( int iCol = 0; iCol < numTableColumns; iCol++ ) {
            String tableField = table.getFieldName(iCol);
            columnOkToWrite[iCol] = false;
            if ( includeColumns == null ) {
                // Include all the columns
                columnOkToWrite[iCol] = true;
            }
            else {
                for ( int iInclude = 0; iInclude < includeColumns.length; iInclude++ ) {
                    if ( includeColumns[iInclude].equalsIgnoreCase(tableField) ) {
                        columnOkToWrite[iCol] = true;
                        break;
                    }
                }
            }
            if ( columnOkToWrite[iCol] ) {
                if ( StringUtil.indexOf(datastoreTableColumns,tableField) < 0 ) {
                    message = "Datastore table/view does not contain columnn \"" + tableField + "\".";
                    Message.printWarning ( 2, routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the database table/view contais column \"" + tableField +
                            "\".") );
                }
            }
        }
        // For now create statements for every row.
        // TODO SAM 2013-02-28 Need to optimize by creating a prepared statement
        for ( int iRow = 0; iRow < table.getNumberOfRecords(); iRow++ ) {
            // Create the query.
            DMIWriteStatement ws = new DMIWriteStatement(dmi);
            ws.addTable(DataStoreTable);
            // Add the DataTable columns to write to the statement

            Object o; // Generic table cell values
            for ( int iCol = 0; iCol < numTableColumns; iCol++ ) {
                if ( columnOkToWrite[iCol] ) {
                    // Add the fields to write and the values
                    o = table.getFieldValue(iRow, iCol);
                    // Handle the column types specifically to deal with nulls, dates, etc.
                    // Cast nulls to make sure the correct DMIWriteStatement method is called
                    if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_STRING ) {
                        ws.addField(tableFieldNames[iCol]);
                        if ( o == null ) {
                            ws.addValue((String)null);
                        }
                        else {
                            ws.addValue((String)o);
                        }
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_INT ) {
                        ws.addField(tableFieldNames[iCol]);
                        if ( o == null ) {
                            ws.addValue((Integer)null);
                        }
                        else {
                            ws.addValue((Integer)o);
                        }
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_DOUBLE ) {
                        ws.addField(tableFieldNames[iCol]);
                        if ( o == null ) {
                            ws.addValue((Double)null);
                        }
                        else {
                            ws.addValue((Double)o);
                        }
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_FLOAT ) {
                        ws.addField(tableFieldNames[iCol]);
                        if ( o == null ) {
                            ws.addValue((Float)null);
                        }
                        else {
                            ws.addValue((Float)o);
                        }
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_SHORT ) {
                        ws.addField(tableFieldNames[iCol]);
                        if ( o == null ) {
                            ws.addValue((Short)null);
                        }
                        else {
                            ws.addValue((Short)o);
                        }
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_LONG ) {
                        ws.addField(tableFieldNames[iCol]);
                        if ( o == null ) {
                            ws.addValue((Long)null);
                        }
                        else {
                            ws.addValue((Long)o);
                        }
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_DATE ) {
                        ws.addField(tableFieldNames[iCol]);
                        if ( o == null ) {
                            ws.addValue((Date)null);
                        }
                        else {
                            ws.addValue((Date)o);
                        }
                    }
                    else {
                        message = "Data table column type \"" +
                        TableColumnType.valueOf(tableColumnTypes[iCol]) +
                        "\" handling is not supported.  Unable to write table.";
                        Message.printWarning ( 2, routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Contact software support to enhance software.") );
                    }
                }
            }
            // Query using the statement that was built
            sqlString = ws.toString();
            int rowCount = dmi.dmiWrite(ws, writeMode.getCode());
            Message.printStatus(2, routine, "Wrote " + rowCount + " rows with query \"" + dmi.getLastQueryString() + "\".");
        }
    }
    catch ( Exception e ) {
        message = "Error writing to datastore \"" + DataStore + "\" (" + e + ").";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the database for datastore \"" + DataStore +
                "\" is appropriate for SQL statement: \"" + sqlString + "\"." ) );
        Message.printWarning ( 3, routine, e );
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
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TableID = props.getValue( "TableID" );
    String IncludeColumns = props.getValue( "IncludeColumns" );
	String DataStore = props.getValue( "DataStore" );
	String DataStoreTable = props.getValue( "DataStoreTable" );
	String WriteMode = props.getValue( "WriteMode" );
	StringBuffer b = new StringBuffer ();
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (IncludeColumns != null) && (IncludeColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IncludeColumns=\"" + IncludeColumns + "\"" );
    }
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
    if ( (DataStoreTable != null) && (DataStoreTable.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreTable=\"" + DataStoreTable + "\"" );
    }
    if ( (WriteMode != null) && (WriteMode.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WriteMode=" + WriteMode );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}