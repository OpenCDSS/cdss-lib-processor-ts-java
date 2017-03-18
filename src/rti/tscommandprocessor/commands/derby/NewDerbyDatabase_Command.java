package rti.tscommandprocessor.commands.derby;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import riverside.datastore.GenericDatabaseDataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;

import RTi.DMI.GenericDMI;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
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
import RTi.Util.IO.PropList;

/**
This class initializes, checks, and runs the NewDerbyDatabase() command.
*/
public class NewDerbyDatabase_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
 
/**
Indicates that the database is created in memory.
*/
protected final String _InMemory = "InMemory";

/**
Datastore from discovery mode.
*/
private DataStore __discoveryDataStore = null;

/**
Constructor.
*/
public NewDerbyDatabase_Command ()
{	super();
	setCommandName ( "NewDerbyDatabase" );
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
{   String routine = getClass().getSimpleName() + ".checkCommandParameters";
    String DataStore = parameters.getValue ( "DataStore" );
    String DatabaseDir = parameters.getValue ( "DatabaseDir" );

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

    if ( (DatabaseDir == null) || (DatabaseDir.length() == 0) ) {
        message = "The database directory (folder) must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the database directory (folder)."));
    }
    else {
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
                // Working directory is available so use it...
                if ( o != null ) {
                    working_dir = (String)o;
                }
            }
            catch ( Exception e ) {
                message = "Error requesting DatabaseDir from processor.";
                warning += "\n" + message;
                Message.printWarning(3, routine, message );
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
            }
    
        try {
            //String adjusted_path = 
            IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,DatabaseDir)));
        }
        catch ( Exception e ) {
            message = "The database directory (folder):\n" +
            "    \"" + DatabaseDir +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that database directory and working directory paths are compatible." ) );
        }
    }
    
	//  Check for invalid parameters...
	List<String> validList = new ArrayList<String>();
    validList.add ( "DataStore" );
    validList.add ( "DatabaseDir" );

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
	return (new NewDerbyDatabase_JDialog ( parent, this )).ok();
}

/**
Return the datastore that is read by this class when run in discovery mode.
*/
private DataStore getDiscoveryDataStore()
{
    return __discoveryDataStore;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataStore ds = getDiscoveryDataStore();
    List<DataStore> v = null;
    if ( (ds != null) && (c == ds.getClass()) ) {
        v = new ArrayList<DataStore>();
        v.add ( ds );
    }
    return v;
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
{	String routine = getClass().getName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryDataStore ( null );
    }

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();

    String DataStore = parameters.getValue ( "DataStore" );
    String DatabaseDir = parameters.getValue ( "DatabaseDir" );
    
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}
	
    // Query the table and set in the processor...
    
    if ( commandPhase == CommandPhaseType.RUN ) {
        GenericDatabaseDataStore ds = null;
        try {
            if ( (DatabaseDir != null) && DatabaseDir.equalsIgnoreCase(_InMemory) ) {
                // Use the parts and create the connection string on the fly
                String systemLogin = null;
                String systemPassword = null;
                GenericDMI dmi = new GenericDMI( "Derby", "memory", DataStore, -1, systemLogin, systemPassword );
                // TODO SAM 2014-04-22 Define properties before opening
                //ds.setProperties(props);
                dmi.open();
                ds = new GenericDatabaseDataStore( DataStore, DataStore, dmi );
                // Set the datastore in the processor
                // TODO SAM 2014-04-22 How to turn off after the processor runs?
                processor.setPropContents ( "DataStore", ds );
            }
            else {
                // Currently don't support
                message = "Only in-memory Derby databases are currently supported.";
                Message.printWarning ( 2, routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Create an in-memory database." ) );
            }
        }
        catch ( Exception e ) {
            /*
            message = "Error creating Derby database \"" + DataStore + "\" (" + e + ").";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the database for data store \"" + DataStore +
                    "\" is appropriate for SQL statement: \"" + queryString + "\"." ) );
            Message.printWarning ( 3, routine, e );
            */
        }
        finally {
            //DMI.closeResultSet(rs);
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // TODO SAM 2014-04-22 Need to return new datastore name
        DataStore ds = new GenericDatabaseDataStore (DataStore, DataStore, null);
        setDiscoveryDataStore ( ds );
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
Set the data store that is read by this class in discovery mode.
*/
private void setDiscoveryDataStore ( DataStore dataStore )
{
    __discoveryDataStore = dataStore;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String DataStore = props.getValue( "DataStore" );
	String DatabaseDir = props.getValue( "DatabaseDir" );
	StringBuffer b = new StringBuffer ();
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
    if ( (DatabaseDir != null) && (DatabaseDir.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DatabaseDir=\"" + DatabaseDir + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}