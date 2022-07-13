// OpenDataStore_Command - This class initializes, checks, and runs the OpenDataStore() command.

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

package rti.tscommandprocessor.commands.datastore;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import riverside.datastore.GenericDatabaseDataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;

import RTi.DMI.DMIDatabaseType;
import RTi.DMI.DatabaseDataStore;
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
This class initializes, checks, and runs the OpenDataStore() command.
*/
public class OpenDataStore_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Data members used for parameter values.
*/
protected final String _Close = "Close";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

/**
Datastore from discovery mode.
*/
private DataStore __discoveryDataStore = null;

/**
Constructor.
*/
public OpenDataStore_Command ()
{	super();
	setCommandName ( "OpenDataStore" );
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
{   String DataStoreName = parameters.getValue ( "DataStoreName" );
    String ServerName = parameters.getValue ( "ServerName" );
    String DatabaseEngine = parameters.getValue ( "DatabaseEngine" );
    if ( (DatabaseEngine == null) || DatabaseEngine.isEmpty() ) {
    	// Default for checks.
    	DatabaseEngine = "" + DMIDatabaseType.SQLITE;
    }
    String DatabaseName = parameters.getValue ( "DatabaseName" );
	String IfFound = parameters.getValue ( "IfFound" );

	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (DataStoreName == null) || DataStoreName.isEmpty() ) {
        message = "The datastore name must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the datastore name." ) );
    }

    DMIDatabaseType dbEngine = DMIDatabaseType.valueOfIgnoreCase(DatabaseEngine);
    if ( dbEngine == null ) {
        message = "The database engine (" + DatabaseEngine + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the datasbase engine using the command editor." ) );
    }
    else {
    	if ( dbEngine == DMIDatabaseType.ACCESS ) {
    		if ( (DatabaseName == null) || DatabaseName.isEmpty() ) {
    			message = "The database name must be specified as the path to the database.";
    			warning += "\n" + message;
    			status.addToLog ( CommandPhaseType.INITIALIZATION,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					message, "Specify the datasbase name as the path to the database file." ) );
    		}
    	}
    	else if ( dbEngine == DMIDatabaseType.SQLITE ) {
    		if ( (ServerName == null) || ServerName.isEmpty() ) {
    			message = "The database server must be specified as the path to the database file or MEMORY for in-memory database";
    			warning += "\n" + message;
    			status.addToLog ( CommandPhaseType.INITIALIZATION,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					message, "Specify the datasbase server as the path to the database file or MEMORY for in-memory database." ) );
    		}
    	}
    }

	if ( (IfFound != null) && !IfFound.isEmpty() ) {
		if ( !IfFound.equalsIgnoreCase(_Close) && !IfFound.equalsIgnoreCase(_Warn)
		    && !IfFound.equalsIgnoreCase(_Fail) ) {
			message = "The IfNotFound parameter \"" + IfFound + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Close + " (default), " + _Warn + ", or " +
					_Fail + "."));
		}
	}
    
	//  Check for invalid parameters.
	List<String> validList = new ArrayList<>(10);
    validList.add ( "DataStoreName" );
    validList.add ( "DataStoreDescription" );
    validList.add ( "DataStoreType" );
    validList.add ( "DatabaseEngine" );
    validList.add ( "ServerName" );
    validList.add ( "DatabaseName" );
    validList.add ( "Login" );
    validList.add ( "Password" );
    validList.add ( "ConnectionProperties" );
	validList.add ( "IfFound" );
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
{	// The command will be modified if changed.
	return (new OpenDataStore_JDialog ( parent, this )).ok();
}

// Use base class parseCommand().

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
{	String routine = getClass().getSimpleName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryDataStore ( null );
    }

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String DataStoreName = parameters.getValue ( "DataStoreName" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		DataStoreName = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreName);
    }
    String DataStoreDescription = parameters.getValue ( "DataStoreDescription" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		DataStoreDescription = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreDescription);
    }
    if ( (DataStoreDescription == null) || DataStoreDescription.isEmpty() ) {
    	// Default.
    	DataStoreDescription = DataStoreName;
    }
    // TODO smalers 2020-10-18 it should be possible to list other than default datastore type.
    //String DataStoreType = parameters.getValue ( "DataStoreType" );
    String DatabaseEngine = parameters.getValue ( "DatabaseEngine" );
    String ServerName = parameters.getValue ( "ServerName" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		ServerName = TSCommandProcessorUtil.expandParameterValue(processor, this, ServerName);
    }
    String DatabaseName = parameters.getValue ( "DatabaseName" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		DatabaseName = TSCommandProcessorUtil.expandParameterValue(processor, this, DatabaseName);
    }
    String Login = parameters.getValue ( "Login" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		Login = TSCommandProcessorUtil.expandParameterValue(processor, this, Login);
    }
    String Password = parameters.getValue ( "Password" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		Password = TSCommandProcessorUtil.expandParameterValue(processor, this, Password);
    }
    String ConnectionProperties = parameters.getValue ( "ConnectionProperties" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		ConnectionProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, ConnectionProperties);
    }
	String IfFound = parameters.getValue ( "IfFound" );
	if ( (IfFound == null) || IfFound.equals("")) {
	    IfFound = _Close; // Default
	}
    
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}
	
    // Open the datastore.
	
	try {
		if ( commandPhase == CommandPhaseType.RUN ) {
			// First see if a matching datastore name exists.
			// Find the data store to use.
			DataStore ds = ((TSCommandProcessor)processor).getDataStoreForName ( DataStoreName, DatabaseDataStore.class );
			if ( ds != null ) {
				// Have an existing datastore.
				message = "Existing datastore \"" + DataStoreName + "\" was found.";
				if ( IfFound.equalsIgnoreCase(_Fail) ) {
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
					status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Closing before reopening may prevent access to data."));
				}
				else if ( IfFound.equalsIgnoreCase(_Warn) ) {
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
					status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
						message, "Closing before reopening may prevent access to data."));
				}
				else {
					// Default is to close the existing datastore:
					// - this is similar to the CloseDataStore command
					/* TODO smalers 2020-10-18 not sure this is needed since it will automatically close in lower code
					Message.printStatus( 2, routine, message + "  Closing the existing datastore before creating new connection.");
					// Close the connection.
					DMI dmi = ((DatabaseDataStore)ds).getDMI();
					dmi.close();
					// Set the status message on the datastore.
					// - if the re-open is successful below, then a new message will be shown
					ds.setStatus(2);
					ds.setStatusMessage("Closed by OpenDataStore command before re-opening.");
					*/
				}
			}

			// Open the new datastore.

			GenericDatabaseDataStore gds = null;
			try {
				// Use the parts and create the connection string on the fly.
				String systemLogin = Login;
				String systemPassword = Password;
				String serverName = null;
				GenericDMI dmi = null;
				if ( DatabaseEngine.equalsIgnoreCase("Access") ) {
					// Get file path:
					// - use the database name as the path to the file
					String DatabaseFile_full = IOUtil.verifyPathForOS(
						IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
						TSCommandProcessorUtil.expandParameterValue(processor,this,DatabaseName)));
					dmi = new GenericDMI( "Access", serverName, DatabaseFile_full, -1, systemLogin, systemPassword );
				}
				else if ( DatabaseEngine.equalsIgnoreCase("SQLite") ) {
					if ( ServerName.equalsIgnoreCase("Memory") ) {
						// SQLite documentation shows lower case.
						dmi = new GenericDMI( "SQLite", "memory", DatabaseName, -1, systemLogin, systemPassword );
					}
					else {
						// Get file path.
						String DatabaseFile_full = IOUtil.verifyPathForOS(
							IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
								TSCommandProcessorUtil.expandParameterValue(processor,this,ServerName)));
						dmi = new GenericDMI( "SQLite", DatabaseFile_full, DatabaseName, -1, systemLogin, systemPassword );
					}
				}
				else {
					// Database that does not use a file.
					dmi = new GenericDMI( DatabaseEngine, ServerName, DatabaseName, -1, systemLogin, systemPassword );
				}
				if ( (ConnectionProperties != null) && !ConnectionProperties.isEmpty() ) {
					// Set additional connection properties if specified:
					// - must get the properties and then append
					PropList props = ds.getProperties();
					props.set("ConnectionProperties",ConnectionProperties);
				}
				dmi.open();
				gds = new GenericDatabaseDataStore( DataStoreName, DataStoreDescription, dmi );
				// TODO smalers 2020-10-18 could add more information to the status message such as command file name.
				gds.setStatusMessage("Opened by OpenDataStore command.");
				// Set the datastore in the processor.
				// TODO SAM 2014-04-22 How to turn off after the processor runs or reset?
				processor.setPropContents ( "DataStore", gds );
			}
			catch ( Exception e ) {
				message = "Error creating datastore \"" + DataStoreName + "\" (" + e + ").";
				Message.printWarning ( 2, routine, message );
				status.addToLog ( commandPhase,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that the datastore \"" + DataStoreName +
						"\" properties are appropriate." ) );
				Message.printWarning ( 3, routine, e );
			}
		}
		else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
			// TODO SAM 2014-04-22 Need to return new datastore name:
			// - for now use the identifier for name
			// - TODO smalers 2021-10-23 problem is if in discovery, then other commands like ReadTableFromDataStore
			//   do not have a working datastore/DMI to list tables - should it open the DMI so that
			//   basic metadata queries can occur?
			boolean simple = false;
			if ( simple ) {
				// This is s simple operation so do every time called.
				DataStore ds = new GenericDatabaseDataStore (DataStoreName, DataStoreDescription, null);
				setDiscoveryDataStore ( ds );
			}
			else {
				// Open the datastore similar to run phase:
				// - set an additional property Discovery=true
				// - DO NOT add to the processor because it is used only for editing
				// - close the resources before (re)opening.
				// - might not need to reopen but could be changing configuration during troubleshooting without restarting TSTool
				// - if resources are not closed beforehand, it is a resource leak
				GenericDatabaseDataStore gds = null;
				try {
					String systemLogin = Login;
					String systemPassword = Password;
					String serverName = null;
					GenericDMI dmi = null;
					if ( DatabaseEngine.equalsIgnoreCase("Access") ) {
						// Get file path:
						// - use the database name as the path to the file
						String DatabaseFile_full = IOUtil.verifyPathForOS(
							IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
							TSCommandProcessorUtil.expandParameterValue(processor,this,DatabaseName)));
						dmi = new GenericDMI( "Access", serverName, DatabaseFile_full, -1, systemLogin, systemPassword );
					}
					else if ( DatabaseEngine.equalsIgnoreCase("SQLite") ) {
						// Special handling for SQLite because it can be an in-memory or on-disk database:
						// - use the server name for the path to the database file
						if ( ServerName.equalsIgnoreCase("Memory") ) {
							// SQLite documentation shows lower case.
							dmi = new GenericDMI( "SQLite", "memory", DatabaseName, -1, systemLogin, systemPassword );
						}
						else {
							// Get file path.
							String DatabaseFile_full = IOUtil.verifyPathForOS(
								IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
								TSCommandProcessorUtil.expandParameterValue(processor,this,ServerName)));
							dmi = new GenericDMI( "SQLite", DatabaseFile_full, DatabaseName, -1, systemLogin, systemPassword );
						}
					}
					else {
						// Database that does not use a file.
						dmi = new GenericDMI( DatabaseEngine, ServerName, DatabaseName, -1, systemLogin, systemPassword );
					}
					if ( (ConnectionProperties != null) && !ConnectionProperties.isEmpty() ) {
						dmi.setAdditionalConnectionProperties(ConnectionProperties);
					}
					//ds.setProperties(props);
					dmi.open();
					gds = new GenericDatabaseDataStore( DataStoreName, DataStoreDescription, dmi );
					// TODO smalers 2020-10-18 could add more information to the status message such as command file name.
					gds.setStatusMessage("Opened by OpenDataStore command to facilitate command editing in discovery mode.");
					// Set the datastore in the processor.
					// TODO SAM 2014-04-22 How to turn off after the processor runs or reset?
					gds.getProperties().set("Discovery", "true");
					setDiscoveryDataStore ( gds );
				}
				catch ( Exception e ) {
					message = "Error creating discovery datastore \"" + DataStoreName + "\" (" + e + ").";
					Message.printWarning ( 2, routine, message );
					status.addToLog ( commandPhase,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Verify that the datastore \"" + DataStoreName +
							"\" properties are appropriate." ) );
					Message.printWarning ( 3, routine, e );
					// Create a datastore instance without opening anything.
					DataStore ds = new GenericDatabaseDataStore (DataStoreName, DataStoreDescription, null);
					setDiscoveryDataStore ( ds );
				}
			}
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Error opening datastore \"" + DataStoreName + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),	routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report to software developers." ) );
		throw new CommandException ( message );
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
    String DataStoreName = props.getValue("DataStoreName");
    String DataStoreDescription = props.getValue("DataStoreDescription");
    String DataStoreType = props.getValue("DataStoreType");
    String DatabaseEngine = props.getValue("DatabaseEngine");
    String ServerName = props.getValue("ServerName");
    String DatabaseName = props.getValue("DatabaseName");
    String Login = props.getValue("Login");
    String Password = props.getValue("Password");
    String ConnectionProperties = props.getValue("ConnectionProperties");
	String IfFound = props.getValue("IfFound");
	StringBuffer b = new StringBuffer ();
    if ( (DataStoreName != null) && (DataStoreName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreName=\"" + DataStoreName + "\"" );
    }
    if ( (DataStoreDescription != null) && (DataStoreDescription.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreDescription=\"" + DataStoreDescription + "\"" );
    }
    if ( (DataStoreType != null) && (DataStoreType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreType=\"" + DataStoreType + "\"" );
    }
    if ( (DatabaseEngine != null) && (DatabaseEngine.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DatabaseEngine=\"" + DatabaseEngine + "\"" );
    }
    if ( (ServerName != null) && (ServerName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ServerName=\"" + ServerName + "\"" );
    }
    if ( (DatabaseName != null) && (DatabaseName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DatabaseName=\"" + DatabaseName + "\"" );
    }
    if ( (Login != null) && (Login.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Login=\"" + Login + "\"" );
    }
    if ( (Password != null) && (Password.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Password=\"" + Password + "\"" );
    }
    if ( (ConnectionProperties != null) && (ConnectionProperties.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ConnectionProperties=\"" + ConnectionProperties + "\"" );
    }
	if ( (IfFound != null) && (IfFound.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfFound=" + IfFound );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}