package rti.tscommandprocessor.commands.datastore;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.sql.ResultSet;
import java.util.List;
import java.util.Vector;

import RTi.DMI.DMI;
import RTi.DMI.DMISelectStatement;
import RTi.DMI.DMIStatement;
import RTi.DMI.DMIStoredProcedureData;
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
    if ( (Sql != null) && !Sql.equals("") && !StringUtil.startsWithIgnoreCase(Sql, "select") ) {
        message = "The SQL statement must start with SELECT.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Update the SQL string to start with SELECT." ) );
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
	List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "DataStore" );
    valid_Vector.add ( "Sql" );
    valid_Vector.add ( "SqlFile" );
    valid_Vector.add ( "DataStoreProcedure" );
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
{	// The command will be modified if changed...
	return (new RunSql_JDialog ( parent, this )).ok();
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
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

    String DataStore = parameters.getValue ( "DataStore" );
    String Sql = parameters.getValue ( "Sql" );
    String SqlFile = parameters.getValue("SqlFile");
    String DataStoreProcedure = parameters.getValue("DataStoreProcedure");
    
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
        dmi = ((DatabaseDataStore)dataStore).getDMI();
    }
    
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}
	
    // Create the query.
    DMIStatement s = new DMIStatement(dmi);
    String sqlString = "";
    // Execute the query as appropriate depending on how the query was specified
    int nEffected = 0;
    ResultSet rs = null;
    try {
        if ( (Sql != null) && !Sql.equals("") ) {
            // Query using the SQL string.  Expand first using ${Property} notation
            sqlString = TSCommandProcessorUtil.expandParameterValue(processor, this, Sql);
            nEffected = dmi.dmiExecute(sqlString);
            Message.printStatus(2, routine, "Executed SQL \"" + dmi.getLastQueryString() + "\".  Rows effected=" + nEffected);
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
            nEffected = dmi.dmiExecute(sqlString);
            Message.printStatus(2, routine, "Executed SQL \"" + dmi.getLastQueryString() + "\".  Rows effected=" + nEffected);
        }
        else if ( (DataStoreProcedure != null) && !DataStoreProcedure.equals("") ) {
            // Run a stored procedure
            // TODO SAM 2013-08-28 Figure out why this is run through the DMISelectStatement
            DMISelectStatement q = new DMISelectStatement(dmi);        
            q.setStoredProcedureData(new DMIStoredProcedureData(dmi,DataStoreProcedure));
            rs = q.executeStoredProcedure();
        }
        Message.printStatus(2, routine, "Executed query \"" + dmi.getLastQueryString() + "\".");
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
	return getCommandName() + "(" + b.toString() + ")";
}

}