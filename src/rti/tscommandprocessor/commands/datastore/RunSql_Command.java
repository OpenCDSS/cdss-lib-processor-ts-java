// RunSql_Command - This class initializes, checks, and runs the RunSql() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import RTi.DMI.DMI;
import RTi.DMI.DMISelectStatement;
import RTi.DMI.DMIStoredProcedureData;
import RTi.DMI.DMIUtil;
import RTi.DMI.DatabaseDataStore;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the RunSql() command.
*/
public class RunSql_Command extends AbstractCommand implements Command
{
    
/**
Constructor.
*/
public RunSql_Command ()
{	super();
	setCommandName ( "RunSql" );
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
    String Sql = parameters.getValue ( "Sql" );
    String SqlFile = parameters.getValue ( "SqlFile" );
    String DataStoreProcedure = parameters.getValue ( "DataStoreProcedure" );

	String warning = "";
    String message;

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (DataStore == null) || (DataStore.length() == 0) ) {
        message = "The data store must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store." ) );
    }
    int specCount = 0;
    if ( (Sql != null) && !Sql.equals("") ) {
        ++specCount;
    }
    if ( (SqlFile != null) && (SqlFile.length() != 0) ) {
        ++specCount;
    }
    if ( ((DataStoreProcedure != null) && (DataStoreProcedure.length() != 0)) ) {
        ++specCount;
    }
    if ( specCount == 0 ) {
        message = "The data store table, SQL statement, SQL file, or procedure must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store table, SQL statement, SQL file, or procedure." ) );
    }
    if ( specCount > 1 ) {
        message = "Onely one of the data store table, SQL statement, SQL file, or procedure can be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store table, SQL statement, or SQL file." ) );
    }
    String SqlFile_full = null;
    if ( (SqlFile != null) && (SqlFile.length() != 0) ) {
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
                // Working directory is available so use it...
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
    
	//  Check for invalid parameters...
	List<String> validList = new ArrayList<>(6);
    validList.add ( "DataStore" );
    validList.add ( "Sql" );
    validList.add ( "SqlFile" );
    validList.add ( "DataStoreProcedure" );
    validList.add ( "ProcedureParameters" );
    validList.add ( "ProcedureReturnProperty" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new RunSql_JDialog ( parent, this )).ok();
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message = "";
	int warning_level = 2;
	int log_level = 3; // Level for non-user messages for log file.
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    status.clearLog(commandPhase);

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String DataStore = parameters.getValue ( "DataStore" );
    String Sql = parameters.getValue ( "Sql" );
    String SqlFile = parameters.getValue("SqlFile");
    String DataStoreProcedure = parameters.getValue("DataStoreProcedure");
    String ProcedureParameters = parameters.getValue ( "ProcedureParameters" );
    // Use a LinkedHashMap to retain the parameter order
    HashMap<String,String> procedureParameters = new LinkedHashMap<String,String>();
    if ( (ProcedureParameters != null) && (ProcedureParameters.length() > 0) && (ProcedureParameters.indexOf(":") > 0) ) {
        // First break map pairs by comma
        List<String>pairs = StringUtil.breakStringList(ProcedureParameters, ",", 0 );
        // Now break pairs and put in hashtable
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            procedureParameters.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String ProcedureReturnProperty = parameters.getValue ( "ProcedureReturnProperty" );
    
    // Find the data store to use...
    DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
        DataStore, DatabaseDataStore.class );
    DMI dmi = null;
    if ( dataStore == null ) {
        message = "Could not get data store for name \"" + DataStore + "\" to query data.";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a database connection has been opened with name \"" +
                DataStore + "\"." ) );
    }
    else {
        DatabaseDataStore dbds = (DatabaseDataStore)dataStore;
    	// Make sure database connection is open - may have timed out
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
	
    String sqlString = "";
    // Execute the query as appropriate depending on how the query was specified
    int nEffected = 0;
    ResultSet rs = null;
   	DMIStoredProcedureData procedureData = null; // Used below if stored procedure
   	DMISelectStatement q = null; // Used if stored procedure
    try {
        if ( (Sql != null) && !Sql.equals("") ) {
            // Query using the SQL string.  Expand first using ${Property} notation
            sqlString = TSCommandProcessorUtil.expandParameterValue(processor, this, Sql);
            // Remove comments if Microsoft Access.  Otherwise leave because troubleshooting might be easier
            if ( dmi.getDatabaseEngineType() == DMI.DBENGINE_ACCESS ) {
                sqlString = DMIUtil.removeCommentsFromSql(sqlString);
            }
            nEffected = dmi.dmiExecute(sqlString);
            Message.printStatus(2, routine, "Executed SQL \"" + sqlString + "\".  Rows effected=" + nEffected);
        }
        else if ( (SqlFile != null) && !SqlFile.equals("") ) {
            // Query using the contents of the SQL file
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
            sqlString = TSCommandProcessorUtil.expandParameterValue(processor, this,
                StringUtil.toString(IOUtil.fileToStringList(SqlFile_full), " "));
            // Remove comments if Microsoft Access.  Otherwise leave because troubleshooting might be easier
            if ( dmi.getDatabaseEngineType() == DMI.DBENGINE_ACCESS ) {
                sqlString = DMIUtil.removeCommentsFromSql(sqlString);
            }
            nEffected = dmi.dmiExecute(sqlString);
            Message.printStatus(2, routine, "Executed SQL \"" + sqlString + "\".  Rows effected=" + nEffected);
        }
        else if ( (DataStoreProcedure != null) && !DataStoreProcedure.equals("") ) {
            // Run a stored procedure
            // TODO SAM 2013-08-28 Figure out why this is run through the DMISelectStatement
            //x DMISelectStatement q = new DMISelectStatement(dmi);        
            //x q.setStoredProcedureData(new DMIStoredProcedureData(dmi,DataStoreProcedure));
            //x rs = q.executeStoredProcedure();
           	// - declaring the procedure will fill its internal metadata
         	int errorCount = 0; // Count of errors that will prevent further processing
           	Message.printStatus(2, routine, "Executing stored procedure \"" + DataStoreProcedure + "\"");
           	procedureData = new DMIStoredProcedureData(dmi,DataStoreProcedure);
           	q = new DMISelectStatement(dmi);
           		// The following code is the same as ReadTableFromDataStore, with matching indentation
                q.setStoredProcedureData(procedureData);
                // Iterate through the parameters
                // - it is OK that the number of parameters is 0
                // - parameter position in statement is 1+, 2+ if the procedure has a return code
                int parameterNum = 0;
                if (procedureData.hasReturnValue()) {
                	// If the procedure has a return value, offset parameters by one
                	// - will have values 2+ below
                	parameterNum = 1;
                }
                int parameterNum0 = -1; // 0-offset index 
                for ( Map.Entry<String,String> entry : procedureParameters.entrySet() ) {
                	++parameterNum;
                	++parameterNum0;
                	// For the following only a few common core types are enabled in the q.setValue() methods.
                	// Therefore, convert the SQL types into common types depending on data type precision.
                	// Issues that arise will have to be addressed by adding additional data types and overloaded methods.
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
                		(parameterType == java.sql.Types.INTEGER) ) {
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
                		// Use DateTime to add a layer of parsing and error handling
                		String s = entry.getValue();
                		DateTime dt = DateTime.parse(s);
                		q.setValue(dt,parameterNum);
                	}
                	else if ( parameterType == java.sql.Types.TIMESTAMP ) {
                		// Use DateTime to add a layer of parsing and error handling
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
                }
                if ( errorCount == 0 ) {
                	rs = q.executeStoredProcedure();
                	// Query string is formatted as procedure call:  procedureName(param1,param2,...)
                	String queryString = q.toString();
                	Message.printStatus(2, routine, "Executed SQL \"" + queryString + "\".");
                }
                // End code from ReadTableToDataStore
        }

        // This code is the same as ReadTableFromDataStore, same indent
        // - the resultset is not processes
            	// Process the return status after processing the resultset as per JDBC documentation:
            	// https://docs.oracle.com/javase/8/docs/api/java/sql/CallableStatement.html
            	// - "a call's ResultSet objects and update counts should be processed prior to getting the values of output parameters"
            	// - if the following code is run before processing the ResultSet, exceptions occur about closed resultset
               	if ( (procedureData != null) && procedureData.hasReturnValue()) {
               		// The return value was registered with when the callable statement was set up.
               		// It could be any type and does not necessarily indicate an error code.
               		// Log the return value and then set as a property if requested.
               		// The return value type is not needed here so use generic Object.
   	                Object returnObject = q.getReturnValue();
   	                Message.printStatus(2, routine, "Return value from stored procedure \"" + procedureData.getProcedureName() + "\" is:  " + returnObject);
   	                // The above gets the return value out of the statement but need to also to get the resultset to continue  processing.
               	    if ( (ProcedureReturnProperty != null) && !ProcedureReturnProperty.isEmpty() ) {
               	    	// Want to set the return value to property, either to use as data or check the error status.
       	                String returnProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, ProcedureReturnProperty);
       	                // Return value can be of any type so get it as an object
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
    catch ( Exception e ) {
        message = "Error querying data store \"" + DataStore + "\" using SQL \"" + sqlString + " (" + e + ").";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the database for data store \"" + DataStore +
                "\" is appropriate for SQL statement: \"" + sqlString + "\"." ) );
        Message.printWarning ( 3, routine, e );
    }
    finally {
        DMI.closeResultSet(rs);
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
	String DataStore = props.getValue( "DataStore" );
	String Sql = props.getValue( "Sql" );
	String SqlFile = props.getValue( "SqlFile" );
	String DataStoreProcedure = props.getValue( "DataStoreProcedure" );
	String ProcedureParameters = props.getValue( "ProcedureParameters" );
	String ProcedureReturnProperty = props.getValue( "ProcedureReturnProperty" );
	StringBuffer b = new StringBuffer ();
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
    if ( (Sql != null) && (Sql.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Sql=\"" + Sql + "\"" );
    }
    if ( (SqlFile != null) && (SqlFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SqlFile=\"" + SqlFile + "\"" );
    }
    if ( (DataStoreProcedure != null) && (DataStoreProcedure.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreProcedure=\"" + DataStoreProcedure + "\"" );
    }
    if ( (ProcedureParameters != null) && (ProcedureParameters.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ProcedureParameters=\"" + ProcedureParameters + "\"" );
    }
    if ( (ProcedureReturnProperty != null) && (ProcedureReturnProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ProcedureReturnProperty=\"" + ProcedureReturnProperty + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}