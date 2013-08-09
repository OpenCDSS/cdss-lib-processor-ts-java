package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import RTi.DMI.DMI;
import RTi.DMI.DMISelectStatement;
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
import RTi.Util.Time.DateTime;

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
    if ( (WriteMode != null) && (DMIWriteModeType.valueOfIgnoreCase(WriteMode) == null) ) {
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
    valid_Vector.add ( "ExcludeColumns" );
    valid_Vector.add ( "DataStore" );
    valid_Vector.add ( "DataStoreTable" );
    valid_Vector.add ( "ColumnMap" );
    valid_Vector.add ( "DataStoreRelatedColumnsMap" );
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
    String ExcludeColumns = parameters.getValue ( "ExcludeColumns" );
    String [] excludeColumns = null;
    if ( ExcludeColumns != null ) {
        if ( ExcludeColumns.indexOf(",") > 0 ) {
            excludeColumns = ExcludeColumns.split(",");
        }
        else {
            excludeColumns = new String[1];
            excludeColumns[0] = ExcludeColumns;
        }
        for ( int i = 0; i < excludeColumns.length; i++ ) {
            excludeColumns[i] = excludeColumns[i].trim();
        }
    }
    String DataStore = parameters.getValue ( "DataStore" );
    String DataStoreTable = parameters.getValue ( "DataStoreTable" );
    if ( (DataStoreTable != null) && DataStoreTable.equals("") ) {
        DataStoreTable = null; // Simplifies logic below
    }
    String ColumnMap = parameters.getValue ( "ColumnMap" );
    Hashtable columnMap = new Hashtable();
    if ( (ColumnMap != null) && (ColumnMap.length() > 0) && (ColumnMap.indexOf(":") > 0) ) {
        // First break map pairs by comma
        List<String>pairs = StringUtil.breakStringList(ColumnMap, ",", 0 );
        // Now break pairs and put in hashtable
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            String tableColumn = parts[0].trim();
            if ( columnMap.get(tableColumn) != null ) {
                message = "Column map has duplicate entries for table column \"" + tableColumn + "\".";
                Message.printWarning ( 2, routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Remove the duplicate, for example by adding a column to the table with desired name." ) );
            }
            else {
                columnMap.put(tableColumn, parts[1].trim() );
            }
        }
    }
    String DataStoreRelatedColumnsMap = parameters.getValue ( "DataStoreRelatedColumnsMap" );
    Hashtable dataStoreRelatedColumnsMap = new Hashtable();
    if ( (DataStoreRelatedColumnsMap != null) && (DataStoreRelatedColumnsMap.length() > 0) &&
        (DataStoreRelatedColumnsMap.indexOf(":") > 0) ) {
        // First break map pairs by comma
        List<String>pairs = StringUtil.breakStringList(DataStoreRelatedColumnsMap, ",", 0 );
        // Now break pairs and put in hashtable
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            String tableColumn = parts[0].trim();
            if ( dataStoreRelatedColumnsMap.get(tableColumn) != null ) {
                message = "Related columns map has duplicate entries for table column \"" + tableColumn + "\".";
                Message.printWarning ( 2, routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Remove the duplicate." ) );
            }
            else {
                dataStoreRelatedColumnsMap.put(tableColumn, parts[1].trim() );
            }
        }
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
    int iRow = -1; // Table row being written
    try {
        // Always get the columns from the database to check parameters, to guard against SQL injection
        List<String> datastoreTableColumns = new Vector<String>();
        try {
            datastoreTableColumns = DMIUtil.getTableColumns(dmi,DataStoreTable);
        }
        catch ( Exception e ) {
            message = "Error getting table columns for datastore table \"" + DataStoreTable + "\".";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the database for datastore \"" + DataStore +
                    "\" is accessible.") );
        }
        // Only include columns that are requested to be included
        // Also map the column names to the corresponding datastore names
        int numTableColumns = table.getNumberOfFields();
        boolean [] columnOkToWrite = new boolean[numTableColumns];
        String [] tableFieldNames = table.getFieldNames(); // Names from input table
        String [] tableFieldNamesMapped = new String[tableFieldNames.length]; // Names to use in datastore table
        int [] tableColumnTypes = table.getFieldDataTypes(); // Input table column numbers
        String [] dataStoreRelatedTables = new String[tableFieldNames.length]; // Datastore related tables for foreign keys
        String [] dataStoreRelatedLookupColumns = new String[tableFieldNames.length]; // Datastore related table columns for foreign keys
        String [] dataStoreRelatedPrimaryKeyColumns = new String[tableFieldNames.length]; // Datastore related table primary key column
        //String [] dataStoreRelatedForeignKeyColumns = new String[tableFieldNames.length]; // Datastore related table foreign key column
        Object mappedName = null; // Mapped column name from hashtable map
        Object relatedColumnO; // hastable object if foreign key is used
        for ( int iCol = 0; iCol < numTableColumns; iCol++ ) {
            // Copy the table column name to the datastore mapped name and change if command parameter
            // specified a mapping
            tableFieldNamesMapped[iCol] = tableFieldNames[iCol];
            mappedName = columnMap.get(tableFieldNames[iCol]);
            if ( mappedName != null ) {
                // Have a map
                tableFieldNamesMapped[iCol] = (String)mappedName;
            }
            // Set the boolean for whether the table column should actually be written
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
            // Now check the ExcludeColumns parameter, which may counter the include columns
            if ( excludeColumns != null ) {
                for ( int iExclude = 0; iExclude < excludeColumns.length; iExclude++ ) {
                    if ( excludeColumns[iExclude].equalsIgnoreCase(tableField) ) {
                        columnOkToWrite[iCol] = false;
                        break;
                    }
                }
            }
            if ( columnOkToWrite[iCol] ) {
                // Determine whether the column is associated with a related table column (foreign key)
                // If so, save the related table and column for use in the write code below
                dataStoreRelatedTables[iCol] = null;
                dataStoreRelatedLookupColumns[iCol] = null;
                //dataStoreRelatedForeignKeyColumns[iCol] = null;
                dataStoreRelatedPrimaryKeyColumns[iCol] = null;
                relatedColumnO = dataStoreRelatedColumnsMap.get(tableFieldNamesMapped[iCol]);
                String relatedColumn = null;
                String [] parts = null;
                if ( relatedColumnO != null ) {
                    // Handled related table (foreign key) columns
                    // The value from the hashtable will be a string table.column
                    relatedColumn = (String)relatedColumnO;
                    if ( relatedColumn.indexOf(".") <= 0 ) {
                        // Single part specified
                        // Related table should be determined based on the related foreign key name
                        dataStoreRelatedLookupColumns[iCol] = relatedColumn;
                        String [] foreignKeyTableAndColumn =
                            DMIUtil.getTableForeignKeyTableAndColumn(dmi, DataStoreTable, tableFieldNamesMapped[iCol]);
                        if ( foreignKeyTableAndColumn == null ) {
                            message = "Datastore related column information \"" + (String)relatedColumnO +
                                "\" does not provide a table and no foreign key information is available in the database";
                            Message.printWarning ( 2, routine, message );
                            status.addToLog ( commandPhase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Verify the syntax of the related column specification.") );
                        }
                        else {
                            dataStoreRelatedTables[iCol] = foreignKeyTableAndColumn[0];
                        }
                    }
                    else {
                        parts = relatedColumn.split("\\.");
                        if ( parts.length != 2 ){
                            message = "Datastore related column information \"" + (String)relatedColumnO +
                                "\" does not appear to be in format RelatedColumn or RelatedTable.RelatedColumn";
                            Message.printWarning ( 2, routine, message );
                            status.addToLog ( commandPhase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Verify the syntax of the related column specification.") );
                        }
                        else {
                            dataStoreRelatedTables[iCol] = parts[0];
                            dataStoreRelatedLookupColumns[iCol] = parts[1];
                        }
                    }
                    // Now have the related table name and related table column
                    if ( !DMIUtil.databaseHasTable(dmi, dataStoreRelatedTables[iCol]) ) {
                        message = "Writing datastore table \"" + DataStoreTable +
                            "\" column \"" + tableFieldNamesMapped[iCol] +
                            "\": datastore does not contain related table/view \"" +
                            dataStoreRelatedTables[iCol] + "\".";
                        Message.printWarning ( 2, routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the database contains table \"" +
                                dataStoreRelatedTables[iCol] + "\".") );
                    }
                    else if ( !DMIUtil.databaseTableHasColumn(dmi, dataStoreRelatedTables[iCol],
                        dataStoreRelatedLookupColumns[iCol]) ) {
                        message = "Writing datastore table \"" + DataStoreTable +
                            "\" column \"" + tableFieldNamesMapped[iCol] +
                            "\": related table/view \"" + dataStoreRelatedTables[iCol] +
                            "\" does not contain column \"" + dataStoreRelatedLookupColumns[iCol] + "\".";
                        Message.printWarning ( 2, routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the database related table/view \"" +
                                dataStoreRelatedTables[iCol] + "\" contains column \"" +
                                dataStoreRelatedLookupColumns[iCol] + "\".") );
                    }
                    // Now save the primary key column for the related table, for use below
                    // Must be exactly 1 primary key
                    List<String> primaryKeyColumns = DMIUtil.getTablePrimaryKeyColumns(dmi, dataStoreRelatedTables[iCol]);
                    int np = primaryKeyColumns.size();
                    if ( np == 0 ) {
                        message = "Writing datastore table \"" + DataStoreTable +
                            "\" column \"" + tableFieldNamesMapped[iCol] +
                            "\": related table/view \"" + dataStoreRelatedTables[iCol] +
                            "\" does not have a primary key column.";
                        Message.printWarning ( 2, routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the related database table/view \"" +
                                dataStoreRelatedTables[iCol] + "\" has 1 primary key defined.") );
                    }
                    else if ( np > 1 ) {
                        message = "Writing datastore table \"" + DataStoreTable +
                            "\" column \"" + tableFieldNamesMapped[iCol] +
                            "\": datastore related table/view \"" + dataStoreRelatedTables[iCol] +
                            "\" has " + np + " primary key columns.  Must have only 1.";
                        Message.printWarning ( 2, routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the related database table/view \"" +
                                dataStoreRelatedTables[iCol] + "\" has 1 primary key defined.") );
                    }
                    else {
                        // Single primary key
                        dataStoreRelatedPrimaryKeyColumns[iCol] = primaryKeyColumns.get(0);
                    }
                }
                else if ( StringUtil.indexOf(datastoreTableColumns,tableFieldNamesMapped[iCol]) < 0 ) {
                    message = "Datastore table/view \"" + DataStoreTable + "\" does not contain column \"" +
                        tableFieldNamesMapped[iCol] + "\".";
                    Message.printWarning ( 2, routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the database table/view \"" +
                            DataStoreTable + "\" contains column \"" + tableFieldNamesMapped[iCol] + "\".") );
                }
            }
        }
        // For now create statements for every row.
        // TODO SAM 2013-02-28 Need to optimize by creating a prepared statement
        Object o; // Generic table cell values
        DMIWriteStatement ws; // Write statement
        DMISelectStatement fkSelect; // Used when foreign key select is used
        int nTableRows = table.getNumberOfRecords();
        for ( iRow = 0; iRow < nTableRows; iRow++ ) {
            if ( (iRow == 0) || (iRow == (nTableRows - 1)) || (iRow%5 == 0) ) {
                // Update the progress bar every 5%
                message = "Writing row " + (iRow + 1) + " of " + nTableRows;
                notifyCommandProgressListeners ( iRow, nTableRows, (float)-1.0, message );
            }
            // Create the query.
            sqlString = "Not yet formed";
            ws = new DMIWriteStatement(dmi);
            ws.addTable(DataStoreTable);
            // Add the DataTable columns to write to the statement
            for ( int iCol = 0; iCol < numTableColumns; iCol++ ) {
                if ( columnOkToWrite[iCol] ) {
                    // Add the fields to write and the values
                    o = table.getFieldValue(iRow, iCol);
                    // Handle the column types specifically to deal with nulls, dates, etc.
                    // Cast nulls to make sure the correct DMIWriteStatement method is called
                    //
                    // First check to see if the datastore column is a related table (foreign key)
                    if ( (dataStoreRelatedTables[iCol] != null) && (dataStoreRelatedLookupColumns[iCol] != null) &&
                        (dataStoreRelatedPrimaryKeyColumns[iCol] != null)) {
                        fkSelect = new DMISelectStatement(dmi);
                        fkSelect.addTable(dataStoreRelatedTables[iCol]);
                        // Return only the primary key column value below.
                        // The value in the original table is used as the where
                        if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_STRING ) {
                            fkSelect.addField(dataStoreRelatedPrimaryKeyColumns[iCol]);
                            if ( o == null ) {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " is null" );
                            }
                            else {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " = '" +
                                    (String)o + "'" );
                            }
                        }
                        else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_INT ) {
                            fkSelect.addField(dataStoreRelatedPrimaryKeyColumns[iCol]);
                            if ( o == null ) {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " is null" );
                            }
                            else {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " = " +
                                    (Integer)o );
                            }
                        }
                        else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_SHORT ) {
                            fkSelect.addField(dataStoreRelatedPrimaryKeyColumns[iCol]);
                            if ( o == null ) {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " is null" );
                            }
                            else {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " = " +
                                    (Short)o );
                            }
                        }
                        else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_LONG ) {
                            fkSelect.addField(dataStoreRelatedPrimaryKeyColumns[iCol]);
                            if ( o == null ) {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " is null" );
                            }
                            else {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " = " +
                                    (Long)o );
                            }
                        }
                        // The following types are unlikely to be primary keys, but include for completeness
                        else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_DOUBLE ) {
                            fkSelect.addField(dataStoreRelatedPrimaryKeyColumns[iCol]);
                            if ( o == null ) {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " is null" );
                            }
                            else {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " = " +
                                    (Double)o );
                            }
                        }
                        else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_FLOAT ) {
                            fkSelect.addField(dataStoreRelatedPrimaryKeyColumns[iCol]);
                            if ( o == null ) {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " is null" );
                            }
                            else {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " = " +
                                    (Float)o );
                            }
                        }
                        else if ( (tableColumnTypes[iCol] == TableField.DATA_TYPE_DATE) ||
                            (tableColumnTypes[iCol] == TableField.DATA_TYPE_DATETIME) ) {
                            fkSelect.addField(dataStoreRelatedPrimaryKeyColumns[iCol]);
                            if ( o == null ) {
                                fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " is null" );
                            }
                            else {
                                // TODO SAM 2013-03-02 Get the data formatting working
                                if ( o instanceof Date ) {
                                    fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " = " + (Date)o );
                                }
                                else if ( o instanceof DateTime ) {
                                    fkSelect.addWhereClause(dataStoreRelatedLookupColumns[iCol] + " = " +
                                        ((DateTime)o).getDate() );
                                }
                            }
                        }
                        else {
                            message = "Related datastore table column \"" + tableFieldNamesMapped[iCol] + " type \"" +
                            TableColumnType.valueOf(tableColumnTypes[iCol]) +
                            "\" handling is not supported.  Unable to write table.";
                            Message.printWarning ( 2, routine, message );
                            status.addToLog ( commandPhase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Contact software support to enhance software.") );
                        }
                        // Now add the nested select statement to the main insert
                        ws.addField(tableFieldNamesMapped[iCol]);
                        ws.addValue(fkSelect);
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_STRING ) {
                        ws.addField(tableFieldNamesMapped[iCol]);
                        if ( o == null ) {
                            ws.addValue("");
                        }
                        else {
                            ws.addValue((String)o);
                        }
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_INT ) {
                        ws.addField(tableFieldNamesMapped[iCol]);
                        if ( o == null ) {
                            ws.addValue((Integer)null);
                        }
                        else {
                            ws.addValue((Integer)o);
                        }
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_DOUBLE ) {
                        ws.addField(tableFieldNamesMapped[iCol]);
                        if ( o == null ) {
                            ws.addValue((Double)null);
                        }
                        else {
                            ws.addValue((Double)o);
                        }
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_FLOAT ) {
                        ws.addField(tableFieldNamesMapped[iCol]);
                        if ( o == null ) {
                            ws.addValue((Float)null);
                        }
                        else {
                            ws.addValue((Float)o);
                        }
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_SHORT ) {
                        ws.addField(tableFieldNamesMapped[iCol]);
                        if ( o == null ) {
                            ws.addValue((Short)null);
                        }
                        else {
                            ws.addValue((Short)o);
                        }
                    }
                    else if ( tableColumnTypes[iCol] == TableField.DATA_TYPE_LONG ) {
                        ws.addField(tableFieldNamesMapped[iCol]);
                        if ( o == null ) {
                            ws.addValue((Long)null);
                        }
                        else {
                            ws.addValue((Long)o);
                        }
                    }
                    else if ( (tableColumnTypes[iCol] == TableField.DATA_TYPE_DATE) ||
                        (tableColumnTypes[iCol] == TableField.DATA_TYPE_DATE) ) {
                        ws.addField(tableFieldNamesMapped[iCol]);
                        if ( o == null ) {
                            ws.addValue((Date)null);
                        }
                        else {
                            if ( o instanceof Date ) {
                                ws.addValue((Date)o);
                            }
                            else if ( o instanceof DateTime ) {
                                ws.addValue(((DateTime)o).getDate());
                            }
                        }
                    }
                    else {
                        message = "Datastore table column \"" + tableFieldNamesMapped[iCol] + " type \"" +
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
        message = "Error writing to datastore \"" + DataStore + "\" row " + iRow + " (" + e + ").";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the database for datastore \"" + DataStore +
                "\" is appropriate for SQL statement: \"" + sqlString + "\"." ) );
        Message.printWarning ( 3, routine, "SQL string:  " + sqlString );
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
    String ExcludeColumns = props.getValue( "ExcludeColumns" );
	String DataStore = props.getValue( "DataStore" );
	String DataStoreTable = props.getValue( "DataStoreTable" );
	String ColumnMap = props.getValue( "ColumnMap" );
	String DataStoreRelatedColumnsMap = props.getValue( "DataStoreRelatedColumnsMap" );
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
    if ( (ExcludeColumns != null) && (ExcludeColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExcludeColumns=\"" + ExcludeColumns + "\"" );
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
    if ( (ColumnMap != null) && (ColumnMap.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ColumnMap=\"" + ColumnMap + "\"" );
    }
    if ( (DataStoreRelatedColumnsMap != null) && (DataStoreRelatedColumnsMap.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreRelatedColumnsMap=\"" + DataStoreRelatedColumnsMap + "\"" );
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