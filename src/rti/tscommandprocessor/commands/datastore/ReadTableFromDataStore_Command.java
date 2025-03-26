// ReadTableFromDataStore_Command - This class initializes, checks, and runs the ReadTableFromDataStore() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package rti.tscommandprocessor.commands.datastore;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import RTi.DMI.DMI;
import RTi.DMI.DMIDatabaseType;
import RTi.DMI.DMISelectStatement;
import RTi.DMI.DMIStoredProcedureData;
import RTi.DMI.DMIUtil;
import RTi.DMI.DatabaseDataStore;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.ResultSetToDataTableFactory;
import RTi.Util.Table.TableField;
//import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ReadTableFromDataStore() command.
*/
public class ReadTableFromDataStore_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

/**
The table that is read.
*/
private DataTable __table = null;

/**
Constructor.
*/
public ReadTableFromDataStore_Command () {
	super();
	setCommandName ( "ReadTableFromDataStore" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	DatabaseDataStore datastore = null;
	checkCommandParameters ( datastore, parameters, command_tag, warning_level );
}

/**
Check the command parameter for valid values, combination, etc.
@param dataStore datastore to use for data checks, used when called from the editor
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( DatabaseDataStore dataStore, PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
    String DataStore = parameters.getValue ( "DataStore" );
    String DataStoreTable = parameters.getValue ( "DataStoreTable" );
    String Top = parameters.getValue ( "Top" );
    String Sql = parameters.getValue ( "Sql" );
    String SqlFile = parameters.getValue ( "SqlFile" );
    String DataStoreFunction = parameters.getValue ( "DataStoreFunction" );
    String DataStoreProcedure = parameters.getValue ( "DataStoreProcedure" );
    String TableID = parameters.getValue ( "TableID" );

	String warning = "";
    String message;

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (DataStore == null) || (DataStore.length() == 0) ) {
        message = "The datastore must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the datastore." ) );
    }
	else {
		// Have DataStore parameter.
        if ( dataStore != null ) {
        	// Could put datastore data checks here.
        }
	}

    int specCount = 0;
    if ( (Sql != null) && !Sql.equals("") ) {
    	// Convert command file placeholder for newline into actual newline.
    	Sql = Sql.replace("\\n", "\n");
        ++specCount;
    }
    if ( ((DataStoreTable != null) && (DataStoreTable.length() != 0)) ) {
        ++specCount;
    }
    if ( (SqlFile != null) && (SqlFile.length() != 0) ) {
        ++specCount;
    }
    if ( ((DataStoreFunction != null) && (DataStoreFunction.length() != 0)) ) {
        ++specCount;
    }
    if ( ((DataStoreProcedure != null) && (DataStoreProcedure.length() != 0)) ) {
        ++specCount;
    }
    if ( specCount == 0 ) {
        message = "The data store table, SQL statement, SQL file, function, or procedure must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store table, SQL statement, SQL file, function, or procedure." ) );
    }
    if ( specCount > 1 ) {
        message = "Only one of the data store table, SQL statement, SQL file, function, or procedure can be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store table, SQL statement, SQL file, function, or procedure." ) );
    }
    // Remove comments:
    // - in general /* */ are the main comments supported because they are used with SQL Server and Oracle and generally easy to deal with
    // - do not remove other comments such as double dashes
    String sqlNoComments = null;
    if ( (Sql != null) && !Sql.equals("") ) {
        sqlNoComments = DMIUtil.removeCommentsFromSql ( Sql ).trim();
        if ( !StringUtil.startsWithIgnoreCase(sqlNoComments, "SELECT") ) {
            message = "The SQL statement (with comments removed) must start with SELECT: " + sqlNoComments;
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Update the SQL string to start with SELECT.  Comments using /* */ and -- are OK." ) );
        }
    }
    if ( (Top != null) && (Top.length() != 0) && !StringUtil.isInteger(Top)) {
        message = "The Top value (" + Top +") is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the Top parameter as an integer." ) );
    }
    String SqlFile_full = null;
    if ( (SqlFile != null) && !SqlFile.isEmpty() && (SqlFile.indexOf("${") < 0) ) {
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
                // Working directory is available so use it.
                if ( o != null ) {
                    working_dir = (String)o;
                }
            }
            catch ( Exception e ) {
                message = "Error requesting WorkingDir from processor.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing SQL file." ) );
            }

        try {
            SqlFile_full = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,SqlFile)));
            File f = new File ( SqlFile_full );
            if ( !f.exists() ) {
                message = "The SQL file does not exist:  \"" + SqlFile_full + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the SQL file exists - may be OK if created at run time." ) );
            }
        }
        catch ( Exception e ) {
            message = "The SQL file:\n" + "    \"" + SqlFile +
            "\"\ncannot be adjusted using the working directory:\n" + "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                     message, "Verify that SQL file and working directory paths are compatible." ) );
        }
    }
    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The output table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the output table identifier." ) );
    }

	//  Check for invalid parameters.
	List<String> validList = new ArrayList<>(18);
    validList.add ( "DataStore" );
    validList.add ( "EditorDataStore" ); // Only used in the editor.
    validList.add ( "DataStoreCatalog" );
    validList.add ( "DataStoreSchema" );
    validList.add ( "DataStoreTable" );
    validList.add ( "DataStoreColumns" );
    validList.add ( "OrderBy" );
    validList.add ( "Top" );
    validList.add ( "Sql" );
    validList.add ( "SqlFile" );
    validList.add ( "DataStoreFunction" );
    validList.add ( "FunctionParameters" );
    validList.add ( "DataStoreProcedure" );
    validList.add ( "ProcedureParameters" );
    validList.add ( "ProcedureReturnProperty" );
    validList.add ( "OutputProperties" );
    validList.add ( "TableID" );
    validList.add ( "RowCountProperty" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	String routine = getClass().getSimpleName() + ".editCommand";
	if ( Message.isDebugOn ) {
		Message.printDebug(1,routine,"Editing the command...getting active and discovery database datastores.");
	}
	List<DatabaseDataStore> dataStoreList =
		TSCommandProcessorUtil.getDatabaseDataStoresForEditors ( (TSCommandProcessor)this.getCommandProcessor(), this );
	List<Prop> propList =
        TSCommandProcessorUtil.getDiscoveryPropFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	return (new ReadTableFromDataStore_JDialog ( parent, this, dataStoreList, propList )).ok();
}

/**
 * Format the function parameters into the dictionary string.
 * @param parameterNames list of parameter names
 */
protected String formatFunctionParameters ( List<String> parameterNames ) {
	StringBuilder b = new StringBuilder();
	int nparam = 0;
	for ( String parameter : parameterNames ) {
		++nparam;
		if ( nparam > 1 ) {
			b.append(",");
		}
		b.append ( parameter + ":");
	}
	return b.toString();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable() {
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
The following classes can be requested:  DataTable
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    DataTable table = getDiscoveryTable();
    List<T> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new ArrayList<>();
        v.add ( (T)table );
    }
    return v;
}

// Use base class parseCommand().

/**
 * Parse the parameter names from the full function or procedure signature:
 *   function(param1 type1, param2 type2) -> return
 * This will return 'param1', 'param2' in a list.
 * @param function or procedure signature string
 * @return list of parameter names
 */
protected List<String> parseFunctionParameterNames ( String routineSignature ) {
	List<String> params = new ArrayList<>();
	int pos1 = routineSignature.indexOf("(");
	int pos2 = routineSignature.indexOf(")");
	if ( routineSignature.length() > 0 ) {
		List<String> parts = StringUtil.breakStringList(routineSignature.substring((pos1 + 1),pos2), ",", 0);
		for ( String part : parts ) {
			pos1 = part.indexOf(" ");
			params.add(part.substring(0,pos1).trim());
		}
	}
	return params;
}

/**
 * Parse the parameter types from the full function or procedure signature:
 *   function(param1 type1, param2 type2) -> return
 * This will return 'type1', 'type2' in a list.
 * @param function or procedure signature string
 * @return LinkedHashMap with parameter name as key and type as value
 */
protected HashMap<String,String> parseFunctionParameterTypes ( String routineSignature ) {
	HashMap<String,String> typeMap = new LinkedHashMap<>();
	int pos1 = routineSignature.indexOf("(");
	int pos2 = routineSignature.indexOf(")");
	String name;
	String value;
	if ( routineSignature.length() > 0 ) {
		List<String> parts = StringUtil.breakStringList(routineSignature.substring((pos1 + 1),pos2), ",", 0);
		for ( String part : parts ) {
			pos1 = part.indexOf(" ");
			name = part.substring(0,pos1).trim();
			value = part.substring((pos1+1)).trim();
			typeMap.put(name, value);
		}
	}
	return typeMap;
}

/**
 * Remove surrounding single quotes, needed if a string parameter value contains characters that need protection.
 * @param paramValue a parameter string
 * @return string without surrounding single quotes
 */
private String removeSurroundingQuotes ( String paramValue ) {
	// Do simple checks to avoid constructing a StringBuilder.
	if ( (paramValue.charAt(0) == '\'') && (paramValue.charAt(paramValue.length() - 1) == '\'') ) {
		return paramValue.substring(1,paramValue.length() - 1);
	}
	else if ( (paramValue.charAt(0) == '\'') && (paramValue.charAt(paramValue.length() - 1) != '\'') ) {
		return paramValue.substring(1);
	}
	else if ( (paramValue.charAt(0) != '\'') && (paramValue.charAt(paramValue.length() - 1) == '\'') ) {
		return paramValue.substring(0,paramValue.length() - 1);
	}
	else {
		// Just return the original string.
		return paramValue;
	}
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
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
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message = "";
	int log_level = 3; // Level for non-user messages for log file.
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

    CommandStatus status = getCommandStatus();
	CommandProcessor processor = getCommandProcessor();
    Boolean clearStatus = Boolean.TRUE; // Default.
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen.
    }
    if ( clearStatus ) {
		status.clearLog(CommandPhaseType.RUN);
	}
	List<Prop> propList = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
		// Get all discovery properties, used to handle ${Property} expansion in discovery mode.
		propList = TSCommandProcessorUtil.getDiscoveryPropFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
        setDiscoveryTable ( null );
    }

	// Make sure there are time series available to operate on.

	PropList parameters = getCommandParameters();

    String DataStore = parameters.getValue ( "DataStore" );
	if ( commandPhase == CommandPhaseType.RUN ) {
	    DataStore = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStore);
	}
	else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	    DataStore = TSCommandProcessorUtil.expandParameterDiscoveryValue(propList, this, DataStore);
	}
    String DataStoreCatalog = parameters.getValue ( "DataStoreCatalog" );
    if ( (DataStoreCatalog != null) && DataStoreCatalog.equals("") ) {
        DataStoreCatalog = null; // Simplifies logic below.
    }
    String DataStoreSchema = parameters.getValue ( "DataStoreSchema" );
    if ( (DataStoreSchema != null) && DataStoreSchema.equals("") ) {
        DataStoreSchema = null; // Simplifies logic below.
    }
    String DataStoreTable = parameters.getValue ( "DataStoreTable" );
    if ( (DataStoreTable != null) && DataStoreTable.equals("") ) {
        DataStoreTable = null; // Simplifies logic below.
    }
    String DataStoreColumns = parameters.getValue ( "DataStoreColumns" );
    String OrderBy = parameters.getValue ( "OrderBy" );
    String Top = parameters.getValue ( "Top" );
    Integer top = 0;
    if ( (Top != null) && !Top.equals("") ) {
        top = Integer.parseInt(Top);
    }
    String Sql = parameters.getValue ( "Sql" );
    if ( Sql != null ) {
    	// Expand escaped newline to actual newline character.
    	Sql = Sql.replace("\\n", "\n");
    }
    String SqlFile = parameters.getValue("SqlFile");
    // Use a LinkedHashMap to retain the parameter order.
    HashMap<String,String> functionParameters = new LinkedHashMap<>();
    HashMap<String,String> functionParameterTypes = new LinkedHashMap<>();
    String DataStoreFunction = parameters.getValue("DataStoreFunction");
    if ( (DataStoreFunction != null) && !DataStoreFunction.isEmpty() ) {
    	// Get the parameter types from the function name, which has format:
    	//   func(param1 type1, param2 type2, ...)
    	functionParameterTypes = parseFunctionParameterTypes(DataStoreFunction);
    }
    String FunctionParameters = parameters.getValue ( "FunctionParameters" );
    if ( (FunctionParameters != null) && (FunctionParameters.length() > 0) && (FunctionParameters.indexOf(":") > 0) ) {
        // Parse the parameter string into a dictionary.
    	functionParameters = StringUtil.parseDictionary(FunctionParameters);
    }

    // Use a LinkedHashMap to retain the parameter order.
    HashMap<String,String> procedureParameters = new LinkedHashMap<>();
    HashMap<String,String> procedureParameterTypes = new LinkedHashMap<>();
    String DataStoreProcedure = parameters.getValue("DataStoreProcedure");
    if ( (DataStoreProcedure != null) && !DataStoreProcedure.isEmpty() ) {
    	// Get the parameter types from the procedure name, which has format:
    	//   proc(param1 type1, param2 type2, ...)
    	procedureParameterTypes = parseFunctionParameterTypes(DataStoreProcedure);
    }
    String ProcedureParameters = parameters.getValue ( "ProcedureParameters" );
    if ( (ProcedureParameters != null) && (ProcedureParameters.length() > 0) && (ProcedureParameters.indexOf(":") > 0) ) {
        // Parse the parameter string into a dictionary.
     	procedureParameters = StringUtil.parseDictionary(ProcedureParameters);
    }
    String ProcedureReturnProperty = parameters.getValue ( "ProcedureReturnProperty" );
    Hashtable<String,String> outputPropertiesMap = new Hashtable<>();
    String OutputProperties = parameters.getValue ( "OutputProperties" );
    if ( (OutputProperties != null) && (OutputProperties.length() > 0) && (OutputProperties.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(OutputProperties, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            outputPropertiesMap.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String TableID = parameters.getValue ( "TableID" );
    if ( (TableID != null) && !TableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) && TableID.indexOf("${") >= 0 ) {
   		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String RowCountProperty = parameters.getValue ( "RowCountProperty" );

    // Find the data store to use.
    DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
        DataStore, DatabaseDataStore.class );
    DMI dmi = null;
    if ( dataStore == null ) {
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		message = "Could not get datastore for name \"" + DataStore + "\" to query data.";
    		Message.printWarning ( 2, routine, message );
    		status.addToLog ( commandPhase,
    			new CommandLogRecord(CommandStatusType.FAILURE,
    				message, "Verify that a database connection has been opened with name \"" +
    				DataStore + "\"." ) );
    	}
    }
    else {
    	DatabaseDataStore dbds = (DatabaseDataStore)dataStore;
    	// Make sure database connection is open - may have timed out.
    	dbds.checkDatabaseConnection();
        dmi = ((DatabaseDataStore)dataStore).getDMI();
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

    // Query the table and set in the processor.

    DataTable table = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
        // Create the query.
        DMISelectStatement q = new DMISelectStatement(dmi);
        if ( DataStoreTable != null ) {
            StringBuffer dataStoreTable = new StringBuffer();
            if ( DataStoreCatalog != null ) {
                // Prepend the database to the table.
                dataStoreTable.append(DMIUtil.escapeField(dmi,DataStoreCatalog));
            }
            if ( DataStoreSchema != null ) {
                // Prepend the database to the table.
                if ( dataStoreTable.length() > 0 ) {
                    dataStoreTable.append(".");
                }
                dataStoreTable.append(DMIUtil.escapeField(dmi,DataStoreSchema));
            }
            if ( dataStoreTable.length() > 0 ) {
                dataStoreTable.append(".");
            }
            if ( DataStoreTable.indexOf('.') > 0 ) {
                // Table already has the parts so just add.
                dataStoreTable.append(DMIUtil.escapeField(dmi,DataStoreTable));
            }
            else {
                // Assume it is a simple table name so escape.
                dataStoreTable.append(DMIUtil.escapeField(dmi,DataStoreTable));
            }
            q.addTable(dataStoreTable.toString());
            // Always get the columns from the database to check parameters, to guard against SQL injection.
            List<String> columns = null;
            try {
                columns = DMIUtil.getTableColumns(dmi,DataStoreTable);
            }
            catch ( Exception e ) {
                message = "Error getting table columns for table \"" + DataStoreTable + "\".";
                Message.printWarning ( 2, routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the database for data store \"" + DataStore + "\" is accessible.") );
            }
            // Get the columns to query.
            if ( (DataStoreColumns != null) && !DataStoreColumns.equals("") ) {
                // Use the columns from the parameter.
                String [] columnsReq = DataStoreColumns.split(",");
                for ( int i = 0; i < columnsReq.length; i++ ) {
                	columnsReq[i] = columnsReq[i].trim();
                    if ( StringUtil.indexOf(columns,columnsReq[i]) < 0 ) {
                        message = "Database table/view does not contain columnn \"" + columnsReq[i] + "\".";
                        Message.printWarning ( 2, routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the database table/view contains column \"" + columnsReq[i] + "\".") );
                    }
                    else {
                        q.addField(columnsReq[i]);
                    }
                }
            }
            else {
                // Use all the columns from the database.
                //for ( String column: columns ) {
                //    q.addField(column);
                //}
                // This is simpler than adding the long list of columns.
                q.addField("*");
            }
            // Set the order by information to query.
            if ( (OrderBy != null) && !OrderBy.equals("") ) {
                String [] columnsReq = OrderBy.split(",");
                for ( int i = 0; i < columnsReq.length; i++ ) {
                    // Check for table to guard against SQL injection:
                	// - it may be necessary to add modifiers to the column, such as "columnname COLLATE NOCASE"
                	//   in SQLite to ignore case
                	// - therefore, split if a space
                	columnsReq[i] = columnsReq[i].trim();
                	String columnName = columnsReq[i];
                	if ( columnsReq[i].indexOf(" ") > 0 ) {
                		columnName = StringUtil.getToken(columnsReq[i], " ", 0, 0);
                	}
                    if ( StringUtil.indexOfIgnoreCase(columns,columnName) < 0 ) {
                        message = "Database table/view does not contain columnn \"" + columnName + "\".";
                        Message.printWarning ( 2, routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the database table/view contains column \"" + columnName +
                                "\".") );
                    }
                    else {
                    	// Include everything provided, including extra keywords for a specific database.
                        q.addOrderByClause(columnsReq[i]);
                    }
                }
            }
            if ( (Top != null) && !Top.equals("") ) {
                q.setTop ( top );
            }
        }
        String queryString = "";
        // Execute the query as appropriate depending on how the query was specified.
        ResultSet rs = null;
        DMIStoredProcedureData procedureData = null; // Used below if stored procedure.
        int errorCount = 0; // Count of errors that will prevent further processing.
       	String messageType = ""; // Used with messaging to indicate function or procedure.
        try {
            if ( DataStoreTable != null ) {
                // Query using the statement that was built above.
                queryString = q.toString();
                rs = dmi.dmiSelect(q);
                Message.printStatus(2, routine, "Executed query \"" + queryString + "\".");
            }
            else if ( (Sql != null) && !Sql.equals("") ) {
                // Query using the SQL string.  Expand first using ${Property} notation.
                queryString = TSCommandProcessorUtil.expandParameterValue(processor, this, Sql);
                // Remove comments if Microsoft Access.  Otherwise leave because troubleshooting might be easier.
                if ( dmi.getDatabaseEngineType() == DMIDatabaseType.ACCESS ) {
                    queryString = DMIUtil.removeCommentsFromSql(queryString);
                }
                rs = dmi.dmiSelect(queryString);
                Message.printStatus(2, routine, "Executed query \"" + queryString + "\".");
            }
            else if ( (SqlFile != null) && !SqlFile.equals("") ) {
                // Query using the contents of the SQL file.
                String SqlFile_full = SqlFile;
                SqlFile_full = IOUtil.verifyPathForOS(
                    IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                        TSCommandProcessorUtil.expandParameterValue(processor,this,SqlFile)));

                if ( !IOUtil.fileReadable(SqlFile_full) || !IOUtil.fileExists(SqlFile_full)) {
                    message = "SQL file \"" + SqlFile_full + "\" is not found or accessible.";
                    Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
                    status.addToLog(commandPhase,
                        new CommandLogRecord( CommandStatusType.FAILURE, message,
                            "Verify that the file exists and is readable."));
                    throw new CommandException ( message );
                }
                queryString = TSCommandProcessorUtil.expandParameterValue(processor, this,
                    StringUtil.toString(IOUtil.fileToStringList(SqlFile_full), " "));
                // Remove comments if Microsoft Access.  Otherwise leave because troubleshooting might be easier.
                if ( dmi.getDatabaseEngineType() == DMIDatabaseType.ACCESS ) {
                    queryString = DMIUtil.removeCommentsFromSql(queryString);
                }
                rs = dmi.dmiSelect(queryString);
                Message.printStatus(2, routine, "Executed query \"" + queryString + "\".");
            }
            else if ( ((DataStoreProcedure != null) && !DataStoreProcedure.equals("")) ||
            		((DataStoreProcedure != null) && !DataStoreProcedure.equals("")) ) {
                // Run a function or stored procedure:
            	// - declaring the procedure will fill its internal metadata
            	HashMap<String,String> params = null;
            	HashMap<String,String> paramTypes = null;
            	String signature = null;
            	if ( (DataStoreFunction != null) && !DataStoreFunction.equals("") ) {
            		Message.printStatus(2, routine, "Executing function: " + DataStoreFunction );
            		params = functionParameters;
            		paramTypes = functionParameterTypes;
            		signature = DataStoreFunction;
            		messageType = "function";
            	}
            	else if ( (DataStoreProcedure != null) && !DataStoreProcedure.equals("") ) {
            		Message.printStatus(2, routine, "Executing stored procedure: " + DataStoreProcedure );
            		params = procedureParameters;
            		paramTypes = procedureParameterTypes;
            		signature = DataStoreProcedure;
            		messageType = "procedure";
            	}
            	int pos = signature.indexOf("(");
            	String callName = signature.substring(0,pos);
            	procedureData = new DMIStoredProcedureData(dmi,callName);
                q.setStoredProcedureData(procedureData);
                // Iterate through the parameters:
                // - it is OK that the number of parameters is 0
                int parameterNum = 0;
                String paramName;
                String paramType;
                String paramTypeUpper;
                String paramValue;
                int i = 0;
                float f = (float)0.0;
                String s = "";
                for ( Map.Entry<String,String> entry : params.entrySet() ) {
                	// For the following only a few common core types are enabled in the q.setValue() methods.
                	// Therefore, convert the SQL types into common types depending on data type precision.
                	// SQL types are from getFunctionColumns() and getProcedureColumns() "TYPE_NAME",
                	// which are user type names to ensure that types work with database features.
                	// Issues that arise will have to be addressed by adding additional data types and overloaded methods.
                	// In most cases, simple types will be used.
                	++parameterNum;
                	paramName = entry.getKey();
                	paramValue = entry.getValue();
                	paramType = paramTypes.get(paramName);
                	paramTypeUpper = paramType.toUpperCase();
                	if ( paramTypeUpper.equals("DECIMAL") ||
                		paramTypeUpper.equals("FLOAT") ||
                		paramTypeUpper.equals("FLOAT4") ||
                		paramTypeUpper.equals("FLOAT8") ||
                		paramTypeUpper.equals("REAL")
                		) {
                		// Float type.
                		try {
                			f = Float.parseFloat(paramValue);
                		}
                		catch ( NumberFormatException e ) {
                			++errorCount;
                			message = "Invalid " + messageType + " parameter value (" + paramValue + ") for parameter " + paramName +
                				" type " + paramType + ", for " + messageType + ": " + signature;
                			Message.printWarning ( 2, routine, message );
                			status.addToLog ( commandPhase,
                    			new CommandLogRecord(CommandStatusType.FAILURE,
                        		message, "Check that the parameter value is correct.") );
                			continue;
                		}
               			q.setValue(f,parameterNum);
                	}
                	else if ( paramTypeUpper.equals("INT") ||
                		paramTypeUpper.equals("INTEGER") ||
                		paramTypeUpper.equals("INT2") ||
                		paramTypeUpper.equals("INT4")
                		) {
                		// Integer type.
                		try {
                			i = Integer.parseInt(paramValue);
                		}
                		catch ( NumberFormatException e ) {
                			++errorCount;
                			message = "Invalid " + messageType + " parameter value (" + paramValue + ") for parameter " + paramName +
                				" type " + paramType + ", for " + messageType + ": " + signature;
                			Message.printWarning ( 2, routine, message );
                			status.addToLog ( commandPhase,
                    			new CommandLogRecord(CommandStatusType.FAILURE,
                        		message, "Check that the parameter value is correct.") );
                			continue;
                		}
                		q.setValue(i,parameterNum);
                	}
                	else if ( paramTypeUpper.equals("LONGVARCHAR") ||
                		paramTypeUpper.equals("TEXT") ||
                		paramTypeUpper.equals("VARCHAR")
                		) {
                		// String type.
                		q.setValue(removeSurroundingQuotes(s),parameterNum);
                	}
                	else if ( paramTypeUpper.equals("TIMESTAMP") ) {
                		// PostgreSQL expects strings in format 'YYYY-MM-DD hh:mm:ss'
                		q.setValue(removeSurroundingQuotes(s),parameterNum);
                	}
                	else {
                		++errorCount;
                		message = "Don't know how to handle parameter type " + paramType + ", for " + messageType + ": " + signature;
                		Message.printWarning ( 2, routine, message );
                		status.addToLog ( commandPhase,
                    		new CommandLogRecord(CommandStatusType.FAILURE,
                        		message, "Need to update the software to handle the type.") );
                	}
                	// TODO smalers 2021-11-07 evaluate host best to handle UDT from signatures:
                	// - the UDT may or may not be standardized
                	// - how to handle custom UDT?
                	// - for now try using with common databases and their types so can at least implement support for primitives
                	/*
                	int parameterType = procedureData.getParameterType(parameterNum0);
                	if ( (parameterType == java.sql.Types.BOOLEAN) ) {
                		boolean b = Boolean.parseBoolean(entry.getValue());
                		q.setValue(b,parameterNum);
                	}
                	else if ( (parameterType == java.sql.Types.BIGINT) ) {
                		long l = Long.parseLong(entry.getValue());
                		q.setValue(l,parameterNum);
                	}
                	else if (
                		(parameterType == java.sql.Types.INTEGER) ||
                		(parameterType == java.sql.Types.SMALLINT) ||
                		(parameterType == java.sql.Types.TINYINT) ) {
                		int i = Integer.parseInt(entry.getValue());
                		q.setValue(i,parameterNum);
                	}
                	else if (
                		(parameterType == java.sql.Types.DECIMAL) ||
                		(parameterType == java.sql.Types.FLOAT) ||
                		(parameterType == java.sql.Types.REAL) ) {
                		float f = Float.parseFloat(entry.getValue());
                		q.setValue(f,parameterNum);
                	}
                	else if ( (parameterType == java.sql.Types.DOUBLE) ) {
                		double d = Double.parseDouble(entry.getValue());
                		q.setValue(d,parameterNum);
                	}
                	else if (
                		(parameterType == java.sql.Types.LONGVARCHAR) ||
                		(parameterType == java.sql.Types.VARCHAR) ) {
                		String s = entry.getValue();
                		q.setValue(s,parameterNum);
                	}
                	else if ( parameterType == java.sql.Types.DATE ) {
                		// Use DateTime to add a layer of parsing and error handling.
                		String s = entry.getValue();
                		DateTime dt = DateTime.parse(s);
                		q.setValue(dt,parameterNum);
                	}
                	else if ( parameterType == java.sql.Types.TIMESTAMP ) {
                		// Use DateTime to add a layer of parsing and error handling.
                		String s = entry.getValue();
                		DateTime dt = DateTime.parse(s);
                		q.setValue(dt,parameterNum);
                	}
                	else {
                		++errorCount;
                		message = "Don't know how to handle procedure parameter type " + parameterType + " (from java.sql.Types)";
                		Message.printWarning ( 2, routine, message );
                		status.addToLog ( commandPhase,
                    		new CommandLogRecord(CommandStatusType.FAILURE,
                        		message, "Need to update the software.") );
                	}
                	*/
                }
                if ( errorCount == 0 ) {
                	rs = q.executeStoredProcedureQuery();
                	// Query string is formatted as procedure call:  procedureName(param1,param2,...)
                	queryString = q.toString();
                	Message.printStatus(2, routine, "Ran " + messageType + ": " + queryString);
                }
            }
            if ( errorCount == 0 ) {
            	// Continue processing the table (otherwise errors above will likely cause issues).
            	if ( rs == null ) {
   	                Message.printStatus(2, routine, "ResultSet is null, not adding table.");
            	}
            	else {
            		ResultSetToDataTableFactory factory = new ResultSetToDataTableFactory();
            		String tableID = TSCommandProcessorUtil.expandParameterValue(processor,this,TableID);
            		table = factory.createDataTable(dmi.getDatabaseEngineType(), rs, tableID);

            		// Set the table in the processor.

            		PropList request_params = new PropList ( "" );
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

            	// Process the return status after processing the result set as per JDBC documentation:
            	// https://docs.oracle.com/javase/8/docs/api/java/sql/CallableStatement.html
            	// - "a call's ResultSet objects and update counts should be processed prior to getting the values of output parameters"
            	// - if the following code is run before processing the ResultSet, exceptions occur about closed result set
               	if ( (procedureData != null) && procedureData.hasReturnValue()) {
               		// The return value was registered with when the callable statement was set up.
               		// It could be any type and does not necessarily indicate an error code.
               		// Log the return value and then set as a property if requested.
               		// The return value type is not needed here so use generic Object.
   	                Object returnObject = q.getReturnValue();
   	                Message.printStatus(2, routine, "Return value from " + messageType + "\"" + procedureData.getProcedureName() + "\" is:  " + returnObject);
   	                // The above gets the return value out of the statement but need to also to get the resultset to continue  processing.
   	                // - TODO this is not needed
   	                //rs = q.getCallableStatement().getResultSet();
               	    if ( (ProcedureReturnProperty != null) && !ProcedureReturnProperty.isEmpty() ) {
               	    	// Want to set the return value to property, either to use as data or check the error status.
       	                String returnProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, ProcedureReturnProperty);
       	                // Return value can be of any type so get it as an object.
                        PropList request_params = new PropList ( "" );
                        request_params.setUsingObject ( "PropertyName", returnProperty );
                        request_params.setUsingObject ( "PropertyValue", returnObject );
                        try {
                            processor.processRequest( "SetProperty", request_params);
                        }
                        catch ( Exception e ) {
                            message = "Error requesting SetProperty(Property=\"" + returnProperty + "\") from processor.";
                            Message.printWarning(log_level,
                                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                            status.addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                        }
                    }
               	}
        	}
        }
        catch ( Exception e ) {
            message = "Error querying datastore \"" + DataStore + "\" using SQL:\n" + queryString + "\nException:\n" + e + "";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the database for data store \"" + DataStore +
                    "\" is appropriate for SQL statement: \"" + queryString + "\"." ) );
            Message.printWarning ( 3, routine, e );
        }
        finally {
            DMI.closeResultSet(rs);
        }
        if ( table != null ) {
        	// Do some final checks, which may be indicative of database not being fully supported.
        	int numStringColsWithZeroWidth = 0;
        	int numStringCols = 0;
        	for ( int icol = 0; icol < table.getNumberOfFields(); icol++ ) {
        		TableField col = table.getTableField(icol);
        		if ( col.getDataType() == TableField.DATA_TYPE_STRING ) {
        			++numStringCols;
        			if ( col.getWidth() == 0 ) {
        				++numStringColsWithZeroWidth;
        			}
        		}
        	}
        	if ( (table.getNumberOfRecords() > 0) && (numStringCols > 0) && (numStringCols == numStringColsWithZeroWidth) ) {
            	message = "All string columns have zero width in table - columns will not display properly.";
            	Message.printWarning(log_level,
                	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                	routine, message );
            	status.addToLog ( CommandPhaseType.RUN,
                	new CommandLogRecord(CommandStatusType.WARNING,
                    	message, "Report the problem to software support - database metadata should return the width." ) );
        	}
        }
	    // Set the property indicating the number of rows in the table.
        if ( (RowCountProperty != null) && !RowCountProperty.equals("") ) {
        	String rowCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, RowCountProperty);
           	int rowCount = 0;
           	if ( table != null ) {
               	rowCount = table.getNumberOfRecords();
           	}
           	PropList request_params = new PropList ( "" );
           	request_params.setUsingObject ( "PropertyName", rowCountProperty );
           	request_params.setUsingObject ( "PropertyValue", Integer.valueOf(rowCount) );
           	try {
               	processor.processRequest( "SetProperty", request_params);
           	}
           	catch ( Exception e ) {
               	message = "Error requesting SetProperty(Property=\"" + RowCountProperty + "\") from processor.";
               	Message.printWarning(log_level,
                   	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                   	routine, message );
               	status.addToLog ( CommandPhaseType.RUN,
                   	new CommandLogRecord(CommandStatusType.FAILURE,
                       	message, "Report the problem to software support." ) );
           	}
        }
        // Set the properties if a one-row result.
        if ( (outputPropertiesMap.size() > 0) && (table != null) && (table.getNumberOfRecords() == 1) ) {
        	// Loop through the map and look up matching table column name:
        	// - warn if column name is not matched
        	for ( Map.Entry<String,String> mapElement : outputPropertiesMap.entrySet() ) {
        		String columnName = mapElement.getKey();
        		String propertyName = mapElement.getValue();

        		try {
        			// Make sure the column name is found in the table.
        			int columnNum = table.getFieldIndex(columnName);
        			// Get the column value and set as a property.
        			PropList request_params = new PropList ( "" );
        			request_params.setUsingObject ( "PropertyName", propertyName );
        			request_params.setUsingObject ( "PropertyValue", table.getFieldValue(0, columnNum) );
        			try {
        				processor.processRequest( "SetProperty", request_params);
        			}
        			catch ( Exception e ) {
        				message = "Error requesting SetProperty(Property=\"" + propertyName + "\") from processor.";
        				Message.printWarning(log_level,
        					MessageUtil.formatMessageTag( command_tag, ++warning_count),
        					routine, message );
        				status.addToLog ( CommandPhaseType.RUN,
        					new CommandLogRecord(CommandStatusType.FAILURE,
        						message, "Report the problem to software support." ) );
        			}
        		}
        		catch ( Exception e ) {
        			message = "Column \"" + columnName + "\") does not exist in the table.";
        			Message.printWarning(log_level,
        				MessageUtil.formatMessageTag( command_tag, ++warning_count),
        				routine, message );
        			status.addToLog ( CommandPhaseType.RUN,
        				new CommandLogRecord(CommandStatusType.FAILURE,
        					message, "Confirm the table column name." ) );
        		}
        	}
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // TODO SAM 2012-01-31 Evaluate whether discover should create table with proper column names.
        table = new DataTable ();
        table.setTableID ( TableID );
        setDiscoveryTable ( table );
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
@param table data table used for discovery mode
*/
private void setDiscoveryTable ( DataTable table ) {
    __table = table;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"DataStore",
		"EditorDataStore",
		"DataStoreCatalog",
		"DataStoreSchema",
		"DataStoreTable",
		"DataStoreColumns",
		"OrderBy",
		"Top",
		"Sql",
		"SqlFile",
		"DataStoreFunction",
		"FunctionParameters",
		"DataStoreProcedure",
		"ProcedureParameters",
		"ProcedureReturnProperty",
		"OutputProperties",
    	"TableID",
    	"RowCountProperty"
	};
	return this.toString(parameters, parameterOrder);
}

}