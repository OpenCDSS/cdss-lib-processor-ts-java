// NewAccessDatabase_Command - This class initializes, checks, and runs the NewAccessDatabase() command.


/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.access;

import javax.swing.JFrame;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.Database.FileFormat;
import com.healthmarketscience.jackcess.DatabaseBuilder;

import riverside.datastore.DataStore;
import riverside.datastore.GenericDatabaseDataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.io.IOException;
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
This class initializes, checks, and runs the NewAccessDatabase() command.
*/
public class NewAccessDatabase_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
 
/**
Datastore from discovery mode.
*/
private DataStore __discoveryDataStore = null;

/**
Constructor.
*/
public NewAccessDatabase_Command ()
{	super();
	setCommandName ( "NewAccessDatabase" );
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
    String DatabaseFile = parameters.getValue ( "DatabaseFile" );
    String Version = parameters.getValue ( "Version" );

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

    if ( (DatabaseFile == null) || (DatabaseFile.length() == 0) ) {
        message = "The database file must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the database directory (folder)."));
    }
    else {
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
                // Working directory is available so use it.
                if ( o != null ) {
                    working_dir = (String)o;
                }
            }
            catch ( Exception e ) {
                message = "Error requesting DatabaseFile from processor.";
                warning += "\n" + message;
                Message.printWarning(3, routine, message );
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
            }
    
        try {
            //String adjusted_path = 
            IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,DatabaseFile)));
        }
        catch ( Exception e ) {
            message = "The database file:\n" +
            "    \"" + DatabaseFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that database directory and working directory paths are compatible." ) );
        }
    }

    if ( (Version != null) && !Version.isEmpty() ) {
    	FileFormat version = FileFormat.valueOf(Version);
    	if ( version == null ) {
    		message = "The version (" + Version + ") is invalid.";
    		warning += "\n" + message;
    		status.addToLog ( CommandPhaseType.INITIALIZATION,
    			new CommandLogRecord(CommandStatusType.FAILURE,
    				message, "Specify a valid version using the command editor." ) );
    	}
    }
    
	//  Check for invalid parameters.
	List<String> validList = new ArrayList<>(3);
    validList.add ( "DataStore" );
    validList.add ( "DatabaseFile" );
    validList.add ( "Version" );

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
{	// The command will be modified if edits are saved.
	return (new NewAccessDatabase_JDialog ( parent, this )).ok();
}

/**
Return the datastore that is read by this class when run in discovery mode.
*/
private DataStore getDiscoveryDataStore() {
    return __discoveryDataStore;
}

/**
Return a list of objects of the requested type, used when running in discovery mode.
This class only keeps a list of DataStore objects.
Classes that can be requested:  DataStore or DatabaseDataStore or GenericDatabaseDataStore
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    DataStore ds = getDiscoveryDataStore();
    List<T> v = null;
    if ( c.isInstance(ds) ) {
        v = new ArrayList<T>();
        v.add ( (T)ds );
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

	PropList parameters = getCommandParameters();
	TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();

    String DataStore = parameters.getValue ( "DataStore" );
    String DatabaseFile = parameters.getValue ( "DatabaseFile" );
    String Version = parameters.getValue ( "Version" );
    FileFormat version = FileFormat.V2016; // Default.
    if ( (Version != null) && !Version.isEmpty() ) {
    	version = FileFormat.valueOf(Version);
    }
    
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}
	
    // Query the table and set in the processor.
    
    if ( commandPhase == CommandPhaseType.RUN ) {
        GenericDatabaseDataStore ds = null;
      	String DatabaseFile_full = DatabaseFile;
        try {
            if ( (DatabaseFile != null) && !DatabaseFile.isEmpty() ) {
                // Use the parts and create the connection string on the fly.
                String systemLogin = null;
                String systemPassword = null;
                GenericDMI dmi = null;
               	// Get file path.
               	DatabaseFile_full = IOUtil.verifyPathForOS(
                   	IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                       	TSCommandProcessorUtil.expandParameterValue(processor,this,DatabaseFile))).replace("\\", "/");
               	// First create the database:
               	// - use try with resources to ensure that it will close properly
               	// - if unable to create an IOException will be thrown
               	boolean createOk = false;
               	try ( Database db = DatabaseBuilder.create(version, new File(DatabaseFile_full))) {
               		Message.printStatus(2, routine, "Created Microsoft Access database: " + DatabaseFile_full);
               		createOk = true;
               	}
               	catch ( IOException e ) {
               		message = "Error creating Access database \"" + DatabaseFile_full + "\" (" + e + ").";
               		Message.printWarning ( 2, routine, message );
               		status.addToLog ( commandPhase,
                   		new CommandLogRecord(CommandStatusType.FAILURE,
                       		message, "Verify that the datastore \"" + DataStore +
                       		"\" and file \"" + DatabaseFile + "\" is appropriate." ) );
               		Message.printWarning ( 3, routine, e );
               	}
               	if ( createOk ) {
               		// The database was successfully created above:
               		// - open the DMI
               		String databaseServer = "";
               		dmi = new GenericDMI( "Access", databaseServer, DatabaseFile_full, -1, systemLogin, systemPassword );
               		/* TODO smalers 2022-07-10 add additional connection properties.
					if ( (ConnectionProperties != null) && !ConnectionProperties.isEmpty() ) {
						// Set additional connection properties if specified:
						// - must get the properties and then append
						PropList props = ds.getProperties();
						props.set("ConnectionProperties",ConnectionProperties);
					}
					*/
               		dmi.open();
               		ds = new GenericDatabaseDataStore( DataStore, DataStore, dmi );
               		// Set the datastore in the processor.
               		// TODO SAM 2014-04-22 How to turn off after the processor runs or reset?
               		processor.setPropContents ( "DataStore", ds );
               	}
            }
        }
        catch ( Exception e ) {
            message = "Error creating Access database \"" + DatabaseFile_full + "\" (" + e + ").";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the datastore \"" + DataStore +
                    "\" and file \"" + DatabaseFile + "\" is appropriate." ) );
            Message.printWarning ( 3, routine, e );
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // TODO SAM 2014-04-22 Need to return new datastore name:
    	// - for now use the identifier for name
    	// - TODO smalers 2021-10-24 need to handle similar to OpenDataStore but needs to be read-only to not corrupt?
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
private void setDiscoveryDataStore ( DataStore dataStore ) {
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
	String DatabaseFile = props.getValue( "DatabaseFile" );
	String Version = props.getValue( "Version" );
	StringBuffer b = new StringBuffer ();
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
    if ( (DatabaseFile != null) && (DatabaseFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DatabaseFile=\"" + DatabaseFile + "\"" );
    }
    if ( (Version != null) && (Version.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Version=\"" + Version + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}