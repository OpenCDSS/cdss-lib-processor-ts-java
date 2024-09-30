// TSCommandProcessor - a class to process time series commands and manage relevant data

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.openwaterfoundation.network.NodeNetwork;

import java.awt.event.WindowListener; // To know when graph window closes to close entire application.

import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandListListener;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorEvent;
import RTi.Util.IO.CommandProcessorEventListener;
import RTi.Util.IO.CommandProcessorEventProvider;
import RTi.Util.IO.CommandProcessorListener;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusProvider;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatusUtil;
import RTi.Util.IO.GenericCommand;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.RequestParameterInvalidException;
import RTi.Util.IO.RequestParameterNotFoundException;
import RTi.Util.IO.UnknownCommandException;
import RTi.Util.IO.UnrecognizedRequestException;
import RTi.Util.JSON.JSONObject;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.YearType;
import RTi.TS.StringMonthTS;
import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSIdent;
import RTi.TS.TSLimits;
import RTi.TS.TSSupplier;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreSubstitute;
import riverside.datastore.WebServiceDataStore;
import rti.tscommandprocessor.core.TimeSeriesView;

// Check commands.

import rti.tscommandprocessor.commands.check.CheckFileCommandProcessorEventListener;

// HEC-DSS I/O.

import rti.tscommandprocessor.commands.hecdss.HecDssAPI;
import rti.tscommandprocessor.commands.util.SetWorkingDir_Command;

// HydroBase commands.

import DWR.DMI.HydroBaseDMI.HydroBaseDMI;

// NWSRFS_DMI commands.

import RTi.DMI.DatabaseDataStore;
import RTi.DMI.NWSRFS_DMI.NWSRFS_DMI;
import RTi.GRTS.TSProductAnnotationProvider;

// TS general commands.

// TODO SAM 2005-05-19 When TSEngine is sufficiently clean,
// merge its code into this class and rework the connection to the TSTool application.
/**
This class processes time series commands and manages the relevant data.
*/
public class TSCommandProcessor implements CommandProcessor, TSSupplier, CommandProcessorEventListener
{

/**
The legacy TSEngine class did all of the processing in TSTool.
It is now wrapped by this TSCommandProcessor class and code will continue to be moved to Command classes and this class.
*/
private TSEngine __tsengine = null;

/**
The list of commands managed by this command processor, guaranteed to be non-null.
*/
private List<Command> __CommandList = new Vector<>(); // Use Vector for thread-safe.

/**
List of plugin command classes, which allow third-party commands to be recognized and run.
These are created in the TSTool main program.
There is no need to have data in TSEngine because by the time TSEngine is used, commands are already created.
*/
private List<Class> pluginCommandClassList = new Vector<>(); // Use Vector for thread-safe.

/**
The name of the file to which the commands are saved, or null if not saved.
Note that the in-memory commands should always be used for printing file headers.
*/
private String __commandFilename = null;

/**
The array of CommandListListeners to be called when the command list changes.
*/
private CommandListListener [] __CommandListListener_array = null;

/**
The array of CommandProcessorListeners to be called when the commands are running, to indicate progress.
*/
private CommandProcessorListener [] __CommandProcessorListener_array = null;

/**
The list of CommandProcessorEventListener managed by this command processor,
which is currently used only by the check file.
See the OpenCheckFile command for creation of the instances.
*/
private CommandProcessorEventListener[] __CommandProcessorEventListener_array = null;

/**
Indicate whether output should be created.  If true, then output files will be created.
If false, commands will run but output files will not be created.
The latter may be used during troubleshooting to increase performance.
*/
private Boolean __CreateOutput_Boolean = new Boolean(true);

/**
Indicate whether commands should clear their log before running. Normally this is true.
However, when using For() commands it is helpful to accumulate logging messages in commands within the For() loop.
The command processor sets this value and each command must call getShouldCommandsClearLog() to check whether to clear.
*/
private Boolean __commandsShouldClearRunStatus = new Boolean(true);

/**
Indicate whether StartLog commands should be enabled.
If true, then new log files will be opened by this command (the default).
If false, the main log file will be used throughout, useful for centralizing output for troubleshooting.
*/
private Boolean __StartLogEnabled_Boolean = new Boolean(true);

/**
The list of StringMonthTS, currently only read by ReadPatternFile() and used with FillPattern().
*/
private List<StringMonthTS> __patternTSList = new Vector<>();

/**
The list of TSEnsemble managed by this command processor, guaranteed to be non-null.
*/
private List<TSEnsemble> __TSEnsembleList = new Vector<>();

/**
The initial working directory for processing, typically the location of the command file from read/write.
This is used to adjust the working directory with
SetWorkingDir() commands and is used as the starting location with RunCommands().
*/
private String __InitialWorkingDir_String = null;

/**
The current working directory for processing,
which was initialized to InitialWorkingDir and modified by SetWorkingDir() commands.
*/
private String __WorkingDir_String = null;

/**
HashMap of properties used by the processor.
HashMap allows null keys and values, although here keys should be non-null.
*/
private HashMap<String,Object> __propertyHashmap = new HashMap<String,Object>();

/**
 * Properties from the command line to be assigned in the processor before each run.
 * These properties can be specified on TSTool command line with Property==Value.
 * These properties should not be modified once set and are independent of built-in and dynamic workflow properties.
 */
private PropList initialProps = new PropList("AppProperties");

/**
Indicates whether the processor is currently running (false if has not started or has completed).
The running occurs in TSEngine.processCommands().
*/
private volatile boolean __is_running = false;

/**
Indicates whether the processing loop should be canceled.
This is a request (e.g., from a GUI) that needs to be handled as soon as possible during command processing.
It is envisioned that cancel can always occur between commands and as time allows it will also be enabled within a command.
*/
private volatile boolean __cancel_processing_requested = false;

// TODO SAM 2007-12-06 Evaluate how to make the DataTable object more generic.
/**
List of DataTable objects maintained by the processor.
*/
List<DataTable> __TableList = new Vector<>();

/**
List of NodeNetwork objects maintained by the processor.
*/
List<NodeNetwork> __NodeNetworkList = new Vector<>();

/**
List of JSON objects maintained by the processor.
*/
List<JSONObject> __jsonObjectList = new Vector<>();

/**
List of TimeSeriesView objects maintained by the processor.
*/
List<TimeSeriesView> __TimeSeriesViewList = new Vector<>();

/**
List of output files generated by processing commands.
*/
List<File> __outputFileList = new Vector<>();

/**
Construct a command processor with no commands.
@initialProps initial properties, typically specified on the application command line,
which are important to make available globally
*/
public TSCommandProcessor ( PropList initialProps ) {
	super();

	// Create a TSEngine that works parallel to this class.
	__tsengine = new TSEngine ( this );
    // Define some standard properties, even before the command processor has run.
	// TODO SAM 2010-05-26 Need to evaluate how to set important global properties up front but also
	// dynamically in resetWorkflowProperties.
    __propertyHashmap.put ( "InstallDir", IOUtil.getApplicationHomeDir() );
    // This is used to locate the HTML documentation for command editor dialogs, etc.
    __propertyHashmap.put ( "InstallDirURL", "file:///" + IOUtil.getApplicationHomeDir().replace("\\", "/") );
    // FIXME SAM 2016-04-03 This is hard-coded for TSTool - need to make more generic to work outside of TSTool?
    // FIXME smalers 2022-12-06 Prior to this date used the following:
    // - not sure why .tstool is included
    // - as of TSTool 14.5.1 add UserTstoolDir
    //String homeDir = System.getProperty("user.home") + File.separator + ".tstool";
    String homeDir = System.getProperty("user.home");
    __propertyHashmap.put ( "UserHomeDir", homeDir );
    __propertyHashmap.put ( "UserHomeDirURL", "file:///" + homeDir.replace("\\", "/") );
    __propertyHashmap.put ( "UserTstoolDir", homeDir + File.separator + ".tstool");
    __propertyHashmap.put ( "UserTstoolDirURL", "file:///" + homeDir.replace("\\", "/") + "/.tstool");
    // TODO smalers set based on the version from ProgramVersionString property.
    //__propertyHashmap.put ( "UserTstoolVersionDir", homeDir + File.separator + ".tstool" + File.separator + majorVersion);
    //__propertyHashmap.put ( "UserTstoolVersionDirURL", "file:///" + homeDir.replace("\\", "/") );

    // Save the initial properties.
    if ( initialProps == null ) {
    	initialProps = new PropList("AppProperties");
    }
    this.initialProps = initialProps;
}

/**
Add a command at the end of the list and notify command list listeners of the add.
@param command Command to add.
*/
public void addCommand ( Command command ) {
	addCommand ( command, true );
}

/**
Add a command at the end of the list.
@param command Command to add.
@param notifyCommandListListeners Indicate whether registered CommandListListeners should be notified.
*/
public void addCommand ( Command command, boolean notifyCommandListListeners ) {
	String routine = getClass().getSimpleName() + ".addCommand";
	__CommandList.add( command );
	// Also add this processor as a listener for events.
	if ( command instanceof CommandProcessorEventProvider ) {
	    CommandProcessorEventProvider ep = (CommandProcessorEventProvider)command;
	    ep.addCommandProcessorEventListener(this);
	}
	if ( notifyCommandListListeners ) {
		notifyCommandListListenersOfAdd ( __CommandList.size() - 1, __CommandList.size() - 1 );
	}
	if ( Message.isDebugOn ) {
	    Message.printDebug(1, routine, "Added command object \"" + command + "\"." );
	}
}

/**
Add a command at the end of the list using the string text.
This should currently only be used for commands that do not have command classes, which perform
additional validation on the commands.
A GenericCommand instance will be instantiated to maintain the string and allow command status to be set.
@param command_string Command string for command.
@param index Index (0+) at which to insert the command.
*/
public void addCommand ( String command_string ) {
	String routine = getClass().getSimpleName() + ".addCommand";
	Command command = new GenericCommand ();
	command.setCommandString ( command_string );
	addCommand ( command );
	Message.printStatus(2, routine, "Creating generic command from string \"" + command_string + "\"." );
}

/**
Add a CommandListListener, to be notified when commands are added, removed,
or change (are edited or execution status is updated).
If the listener has already been added, the listener will remain in the list in the original order.
*/
public void addCommandListListener ( CommandListListener listener ) {
	// Use arrays to make a little simpler than Vectors to use later.
	if ( listener == null ) {
		return;
	}
	// See if the listener has already been added.
	// Resize the listener array.
	int size = 0;
	if ( __CommandListListener_array != null ) {
		size = __CommandListListener_array.length;
	}
	for ( int i = 0; i < size; i++ ) {
		if ( __CommandListListener_array[i] == listener ) {
			return;
		}
	}
	if ( __CommandListListener_array == null ) {
		__CommandListListener_array = new CommandListListener[1];
		__CommandListListener_array[0] = listener;
	}
	else {
	    // Need to resize and transfer the list.
		size = __CommandListListener_array.length;
		CommandListListener [] newlisteners = new CommandListListener[size + 1];
		for ( int i = 0; i < size; i++ ) {
				newlisteners[i] = __CommandListListener_array[i];
		}
		__CommandListListener_array = newlisteners;
		__CommandListListener_array[size] = listener;
		newlisteners = null;
	}
}

/**
Add a CommandProcessorEventListener, to be notified when commands generate CommandProcessorEvents.
This is currently utilized by the check file capability, which queues events and generates a report file.
If the listener has already been added, the listener will remain in the list in the original order.
TODO SAM 2008-08-21 Make this private for now but may need to rethink if other than the check file use the events.
*/
private void addCommandProcessorEventListener ( CommandProcessorEventListener listener ) {
    // Use arrays to make a little simpler than Vectors to use later.
    if ( listener == null ) {
        return;
    }
    // See if the listener has already been added.
    // Resize the listener array.
    int size = 0;
    if ( __CommandProcessorEventListener_array != null ) {
        size = __CommandProcessorEventListener_array.length;
    }
    for ( int i = 0; i < size; i++ ) {
        if ( __CommandProcessorEventListener_array[i] == listener ) {
            return;
        }
    }
    if ( __CommandProcessorEventListener_array == null ) {
        __CommandProcessorEventListener_array = new CommandProcessorEventListener[1];
        __CommandProcessorEventListener_array[0] = listener;
    }
    else {
        // Need to resize and transfer the list.
        size = __CommandProcessorEventListener_array.length;
        CommandProcessorEventListener [] newlisteners = new CommandProcessorEventListener[size + 1];
        for ( int i = 0; i < size; i++ ) {
            newlisteners[i] = __CommandProcessorEventListener_array[i];
        }
        __CommandProcessorEventListener_array = newlisteners;
        __CommandProcessorEventListener_array[size] = listener;
        newlisteners = null;
    }
}

/**
Add a CommandProcessorListener, to be notified when commands are started,
progress made, and completed.  This is useful to allow calling software to report progress.
If the listener has already been added, the listener will remain in the list in the original order.
*/
public void addCommandProcessorListener ( CommandProcessorListener listener ) {
	// Use arrays to make a little simpler than lists to use later.
	if ( listener == null ) {
		return;
	}
	// See if the listener has already been added.
	// Resize the listener array.
	int size = 0;
	if ( __CommandProcessorListener_array != null ) {
		size = __CommandProcessorListener_array.length;
	}
	for ( int i = 0; i < size; i++ ) {
		if ( __CommandProcessorListener_array[i] == listener ) {
			return;
		}
	}
	if ( __CommandProcessorListener_array == null ) {
		__CommandProcessorListener_array = new CommandProcessorListener[1];
		__CommandProcessorListener_array[0] = listener;
	}
	else {
	    // Need to resize and transfer the list.
		size = __CommandProcessorListener_array.length;
		CommandProcessorListener [] newlisteners = new CommandProcessorListener[size + 1];
		for ( int i = 0; i < size; i++ ) {
				newlisteners[i] = __CommandProcessorListener_array[i];
		}
		__CommandProcessorListener_array = newlisteners;
		__CommandProcessorListener_array[size] = listener;
		newlisteners = null;
	}
}

// TODO SAM 2009-01-20 Could name the method better if working with generics.
// For now use the longer method name to clue people in that the list contains strings.
/**
Initialize and add new commands to the processor given a list of command strings.
This is an alternative to reading the command file with the readCommandFile()
method and allows in-memory command processing without the command file.
A disadvantage of this approach is that the command file is not
an artifact of processing and therefore troubleshooting may be more difficult
so verify that the log file contains the commands in the header comments.
Registered CommandListListener instances are notified about each add
(this could be used to let a GUI update its appearance when a command file is loaded).
@param commandStrings a list of command strings, as if read from a command file.
@param createUnknownCommandIfNotRecognized If true, create a GenericCommand
if the command is not recognized or has a syntax problem.
This is being used during transition of old string commands to full Command classes and
may be needed in any case to preserve commands that were manually edited.
Commands with problems will in any case be flagged at run-time as unrecognized or problematic.
@param append If true, the commands will be appended to the existing commands.
@param runDiscoveryOnLoad if true, run discovery mode on the commands at load; if false, do not run discovery
@param initialWorkingDir the initial working directory to use to run the commands (if not null),
which is typically specified because it would be set to the location of the command file if the command file were read from disk.
@exception IOException if initialWorkingDir is not valid.
*/
public void addCommandsFromStringList ( List<String> commandStrings, boolean createUnknownCommandIfNotRecognized,
    boolean append, boolean runDiscoveryOnLoad, File initialWorkingDir )
throws IOException {
    String routine = getClass().getSimpleName() + ".initializeCommandsFromStringList";
    // Set the working directory because this may be used by other commands.
    if ( initialWorkingDir != null ) {
        setInitialWorkingDir ( initialWorkingDir.getCanonicalPath() );
    }
    String line;
    Command command = null;
    // TODO smalers 2020-08-04 does it make sense to pass in a command factory instance to this method?
    TSCommandFactory cf = new TSCommandFactory(this.pluginCommandClassList);
    // Use this to control whether listeners should be notified for each insert.
    // Why would this be done?
    // If, for example, a GUI should display the progress in reading/initializing the commands.
    //
    // Why would this not be done?  Because of performance issues.
    boolean notifyListenersForEachAdd = true;
    // If not appending, remove all.
    if ( !append ) {
        removeAllCommands();
    }
    // Now process each line in the file and turn into a command.
    int numAdded = 0;
    int numCommandStrings = commandStrings.size();
    for ( int i = 0; i < numCommandStrings; i++ ) {
        line = commandStrings.get(i);
        // Trim spaces from the end of the line.  These can really cause problems with time series identifiers
        // FIXME SAM 2009-01-20 Is desirable to trim later so the original representation of commands is not changed.
        // In particular people may want to indent the commands.
        // Need to make sure that trim() is included when the command strings are interpreted.
        // For now this is more trouble than it is worth.
        //
        // TODO smalers 2022-02-23 as of TSTool 14.6.0 indentation is allowed so don't trim when loading commands.
        //line = line.trim();
        // Create a command from the line.
        // Normally will create the command even if not recognized.
        if ( Message.isDebugOn ) {
            Message.printDebug( 10, routine, "Creating command using untrimmed string \"" + line + "\"" );
        }
        if ( createUnknownCommandIfNotRecognized ) {
            try {
                command = cf.newCommand ( line, createUnknownCommandIfNotRecognized );
            }
            catch ( UnknownCommandException e ) {
                Message.printWarning( 3, routine, "Unexpected error creating command (" + e + ")." );
                // Should not happen because of parameter passed above.
            }
        }
        else {
            try {
                command = cf.newCommand ( line, createUnknownCommandIfNotRecognized );
            }
            catch ( UnknownCommandException e ) {
                // TODO SAM 2007-09-08 Evaluate how to handle unknown commands at load without stopping the load.
                // In this case skip the command, although the above case may always be needed?
            }
        }
        // Have a command instance.  Initialize the command (parse the command string) and check its arguments.
        String fixme = "@FIXME! ";  // String for inserted messages.
        try {
            command.initializeCommand(
                line, // Command string, needed to do full parse on parameters.
                this, // Processor, needed to make requests.
                true); // Do full initialization (parse).
        }
        catch ( InvalidCommandSyntaxException e ) {
            // Can't use cf.newCommand() because it will recognized the command and generate yet another exception.
        	// So, treat as a generic command with a problem.
            Message.printWarning (2, routine, "Invalid command syntax.  Adding command with problems:  " + line );
            Message.printWarning(3, routine, e);
            // CommandStatus will be set while initializing so no need to set here.
            // Do it anyway to make sure something does not fall through the cracks.
            if ( (command != null) && (command instanceof CommandStatusProvider) ) {
                CommandStatus status = ((CommandStatusProvider)command).getCommandStatus();
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        "Invalid command syntax (" + e + ").",
                        "Correct the command.  See log file for details." ) );
            }
            // Add generic commands as comments prior to this command to show the original.
            Command command2 = new GenericCommand ();
            command2.setCommandString ( "#" + fixme +
                "The following command had errors and needs to be corrected below and this comment removed.");
            CommandStatus status = ((CommandStatusProvider)command2).getCommandStatus();
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    "There was an error loading the following command.",
                    "Correct the command below (typically a parameter error due to manual edit)." ) );
            addCommand ( command2, notifyListenersForEachAdd );
            ++numAdded;
            command2 = new GenericCommand ();
            command2.setCommandString ( "#" + fixme + line );
            status = ((CommandStatusProvider)command2).getCommandStatus();
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    "There was an error loading this command.",
                    "Correct the command below (typically a parameter error due to manual edit)." ) );
            addCommand ( command2, notifyListenersForEachAdd );
            ++numAdded;
            // Allow the bad command to be loaded below.
            // It may have no arguments or partial parameters that need corrected.
        }
        catch ( InvalidCommandParameterException e) {
            // Can't use cf.newCommand() because it will recognized the command and generate yet another exception.
        	// So, treat as a generic command with a problem.
            Message.printWarning (2, routine, "Invalid command parameter.  Adding command with problems:  " + line );
            Message.printWarning(3, routine, e);
            // CommandStatus will be set while initializing so no need to set here.
            // Do it anyway to make sure something does not fall through the cracks.
            if ( (command != null) && (command instanceof CommandStatusProvider) ) {
                CommandStatus status = ((CommandStatusProvider)command).getCommandStatus();
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        "Invalid command parameter." + e + ").",
                        "Correct the command.  See log file for details." ) );
            }
            // Add generic commands as comments prior to this command to show the original.
            Command command2 = new GenericCommand ();
            command2.setCommandString ( "# " + fixme +
                "The following command had errors and needs to be corrected below and this comment removed.");
            CommandStatus status = ((CommandStatusProvider)command2).getCommandStatus();
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    "There was an error loading the following command.",
                    "Correct the command below (typically an error due to manual edit)." ) );
            addCommand ( command2, notifyListenersForEachAdd );
            ++numAdded;
            command2 = new GenericCommand ();
            command2.setCommandString ( "#" + line );
            status = ((CommandStatusProvider)command2).getCommandStatus();
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    "There was an error loading this command.",
                    "Correct the command below (typically an error due to manual edit)." ) );
            addCommand ( command2, notifyListenersForEachAdd );
            ++numAdded;
            // Allow the bad command to be loaded below.
            // It may have no arguments or partial parameters that need corrected.
        }
        catch ( Exception e ) {
            // TODO SAM 2007-11-29 Need to decide whether to handle here or in command with CommandStatus.
            // It is important that the command get added, even if it is invalid, so the user can edit the command file.
        	// They will likely need to replace the command, not edit it.
            Message.printWarning( 1, routine, "Unexpected error creating command \"" + line + "\" - report to software support." );
            Message.printWarning ( 3, routine, e );
            // CommandStatus likely not set while initializing so need to set here to alert user.
            if ( (command != null) && (command instanceof CommandStatusProvider) ) {
                CommandStatus status = ((CommandStatusProvider)command).getCommandStatus();
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        "Unexpected error creating the command.",
                        "Check the command syntax.  See log file for details." ) );
            }
            // Add generic commands as comments prior to this command to show the original.
            Command command2 = new GenericCommand ();
            command2.setCommandString ( "#" + fixme +
                " The following command had errors and needs to be corrected below and this comment removed.");
            CommandStatus status = ((CommandStatusProvider)command2).getCommandStatus();
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    "There was an error loading the following command.",
                    "Correct the command below (typically an error due to manual edit)." ) );
            addCommand ( command2, notifyListenersForEachAdd );
            ++numAdded;
            command2 = new GenericCommand ();
            command2.setCommandString ( "#" + fixme + line );
            status = ((CommandStatusProvider)command2).getCommandStatus();
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    "There was an error loading this command.",
                    "Correct the command below (typically an error due to manual edit)." ) );
            addCommand ( command2, notifyListenersForEachAdd );
            ++numAdded;
            // Allow the bad command to be loaded below.
            // It may have no arguments or partial parameters that need corrected.
        }
        // TODO SAM 2007-10-09 Evaluate whether to call listeners each time a command is added.
        // Could be good to indicate progress of load in the GUI.
        // For now, add the command, without notifying listeners of changes.
        if ( command != null ) {
            // Check the command parameters.
            String command_tag = "" + numAdded + 1;  // Command number, for messaging.
            int error_count = 0;
            try {
                command.checkCommandParameters(command.getCommandParameters(), command_tag, 2 );
            }
            catch ( InvalidCommandParameterException e ) {
                /* TODO SAM 2008-05-14 Evaluate whether this can work.
                 * Don't want a bunch of extra comments for commands that are already being flagged with status.
                // Add generic commands as comments prior to this command to show the original,
                Command command2 = new GenericCommand ();
                command2.setCommandString ( "#" + fixme +
                "The following command had errors and needs to be corrected below and this comment removed.");
                CommandStatus status = ((CommandStatusProvider)command2).getCommandStatus();
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                "There was an error loading the following command.",
                                "Correct the command below (typically an error due to manual edit)." ) );
                addCommand ( command2, notify_listeners_for_each_add );
                ++num_added;
                command2 = new GenericCommand ();
                command2.setCommandString ( "#" + fixme + line );
                status = ((CommandStatusProvider)command2).getCommandStatus();
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                "There was an error loading this command.",
                                "Correct the command below (typically an error due to manual edit)." ) );
                addCommand ( command2, notify_listeners_for_each_add );
                ++num_added;
                */
                // Add command status to the command itself,
            	// handling whether a recognized command or a generic command (string command).
                String message = "Error loading command - invalid syntax (" + e + ").";
                if ( command instanceof CommandStatusProvider ) {
                   if ( CommandStatusUtil.getHighestSeverity((CommandStatusProvider)command).
                       greaterThan(CommandStatusType.UNKNOWN) ) {
                       // No need to print a message to the screen because a visual marker will be shown, but log.
                       Message.printWarning ( 2,
                           MessageUtil.formatMessageTag(command_tag,
                               ++error_count), routine, message );
                   }
                   if ( command instanceof GenericCommand ) {
                        // The command class will not have added a log record so do it here.
                        ((CommandStatusProvider)command).getCommandStatus().addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Check the log for more details." ) );
                   }
                }
                else {
                    // Command has not been updated to set warning/failure in status so show here.
                    Message.printWarning ( 2,
                        MessageUtil.formatMessageTag(command_tag,
                        ++error_count), routine, message );
                }
                // Log the exception.
                if (Message.isDebugOn) {
                    Message.printDebug(3, routine, e);
                }
            }
            // Now finally add the command to the list.
            addCommand ( command, notifyListenersForEachAdd );
            ++numAdded;
            // Run discovery on the command so that the identifiers are available to other commands.
            // Do up front and then only when commands are edited.
            if ( runDiscoveryOnLoad && (command instanceof CommandDiscoverable) ) {
                readCommandFile_RunDiscoveryOnCommand ( command );
            }
        }
    } // Looping over command strings.
    // Now notify listeners about the add one time (only need to do if it was not getting done for each add).
    if ( !notifyListenersForEachAdd ) {
        notifyCommandListListenersOfAdd ( 0, (numAdded - 1) );
    }
    if ( Message.isDebugOn ) {
        Message.printDebug(10, routine, "Added " + numAdded + " commands." );
    }
}

/**
Determine if the commands are a template.
In this case, applications may disable save features.
The special comment "#@template" indicates that the commands are a template.
@return true if commands are marked as a template, false if not.
*/
public boolean areCommandsTemplate () {
    // String that indicates a template.
    String templateString = "@TEMPLATE";
    // Loop through the commands and check comments for the special string.
    int size = size();
    Command c;
    String cst, csu;
    for ( int i = 0; i < size; i++ ) {
        c = __CommandList.get(i);
        cst = c.toString().trim();
        if ( cst.startsWith("#") ) {
	        csu = cst.toUpperCase();
	        if ( csu.indexOf(templateString) > 0 ) {
	            return true;
	        }
        }
    }
    return false;
}

/**
Clear the results of processing.  This resets the list of time series, tables, and ensembles to empty.
Other data still closely coupled with __tsengine are cleared in its processCommands_ResetDataForRunStart() method,
which calls this method.
*/
public void clearResults() {
	this.__tsengine.clearTimeSeriesResults();
    if ( this.__NodeNetworkList != null ) {
        this.__NodeNetworkList.clear();
    }
    if ( this.__jsonObjectList != null ) {
        this.__jsonObjectList.clear();
    }
    if ( this.__TableList != null ) {
        this.__TableList.clear();
    }
    removeAllEnsembles();
    removeAllPatternTS();
}

//TODO SAM 2009-01-15 Evaluate whether something is needed for databases, etc.
//To date things have worked fine as is but HEC-DSS open connections may linger and cause resources issues.
/**
Close data connections, in particular any HEC-DSS connections that may be open.
@param closeAll if true, close all connections in use by the processor,
even those that by reference may be used in an external application and which were passed by reference
(currently not implemented).  If false,
close only the connections that are likely to be used locally in the processor.
*/
public void closeDataConnections ( boolean closeAll ) {
     // Regardless, close HEC-DSS connections.
     HecDssAPI.closeAllFiles();
}

/**
Return the Command instance at the requested position.
@return The number of commands being managed by the processor
*/
public Command get( int pos ) {
	return __CommandList.get(pos);
}

/**
Indicate whether canceling processing has been requested.
@return true if canceling has been requested.
*/
public boolean getCancelProcessingRequested () {
	return __cancel_processing_requested;
}

/**
Get the name of the command file associated with the processor.
@return the name of the command file associated with the processor.
*/
public String getCommandFileName () {
	return __commandFilename;
}

/**
Return the list of commands.
@return the list of commands.
*/
public List<Command> getCommands () {
	return __CommandList;
}

/**
Indicate whether output files should be created when processing.
@return true if files should be created, false if not.
*/
public Boolean getCreateOutput () {
	return __CreateOutput_Boolean;
}

/**
Return the datastore for the requested name, or null if not found.
The matching datastore is returned regardless of whether active (open) or not.
If the requested name matches a substitute, the substitute is returned.
@param name the data store name to match (case is ignored in the comparison)
@param dataStoreClass the class of the data store to match, useful when ensuring that the data store
is compatible with intended use - specify as null to not match class
@return the data store for the requested name, or null if not found.
*/
public DataStore getDataStoreForName ( String name, Class<?> dataStoreClass ) {
	return getDataStoreForName ( name, dataStoreClass, false );
}

/**
Return the datastore for the requested name, or null if not found.
@param name the data store name to match (case is ignored in the comparison)
@param dataStoreClass the class of the data store to match,
useful when ensuring that the data store is compatible with intended use - specify as null to not match class
@param activeOnly if true, only active datastores are returned (status = 0)
@return the data store for the requested name, or null if not found.
*/
public DataStore getDataStoreForName ( String name, Class<?> dataStoreClass, boolean activeOnly ) {
	String routine = null;
	if ( Message.isDebugOn ) {
		routine = getClass().getSimpleName() + ".getDataStoreForName";
		Message.printDebug(1,routine,"Trying to match datastore \"" + name + "\".");
	}
	// First see if there is a substitute for the datastore:
	// - the requested name could be a substitute name
	// - need to get the original datastore name, which is what is actually stored in the processor
	List<DataStoreSubstitute> datastoreSubstituteList = this.__tsengine.getDataStoreSubstituteList();
    for ( DataStoreSubstitute dssub : datastoreSubstituteList ) {
    	String nameInCommands = dssub.getDatastoreNameInCommands();
    	if ( nameInCommands.equals(name) ) {
    		// Matched the "name in commands" substitute name.
    		String nameToUse = dssub.getDatastoreNameToUse();
    		if ( Message.isDebugOn ) {
    			Message.printDebug(1, routine, "  Requested datastore name matches substitute name used in commands \"" + nameInCommands +
    				"\", will use datastore name \"" + nameToUse + "\"");
    		}
    		name = nameToUse;
    		break;
    	}
    }
	// Search the datastores for the requested name.
   	//Message.printStatus(2,routine, "  Checking " + getDataStores().size() + " datastores." );
	for ( DataStore dataStore : getDataStores() ) {
        if ( dataStore.getName().equalsIgnoreCase(name) ) {
        	// Have a matching datastore.  Perform other checks.
    		if ( Message.isDebugOn ) {
    			Message.printDebug(1,routine,"  Found matching datastore name \"" + name + "\"." );
    		}
            if ( dataStoreClass != null ) {
            	// Also must match the requested class.
                if (dataStore.getClass() == dataStoreClass ) {
                    ; // Match is OK.
                }
                // Also check for common base classes.
                // TODO SAM 2012-01-31 Why not just use instanceof all the time?
                else if ( (dataStoreClass == DatabaseDataStore.class) && dataStore instanceof DatabaseDataStore ) {
                    ; // Match is OK
                }
                else {
                    // Does not match class.
                	if ( Message.isDebugOn ) {
                		Message.printDebug(1,routine,"  Datastore name \"" + name + "\" does not match requested class." );
                	}
                    dataStore = null;
                }
            }
            if ( activeOnly ) {
            	if ( dataStore.getStatus() == 0 ) {
            		// Only want active datastores and it is active.
            		return dataStore;
            	}
            	else {
            		// Datastore is not active.
                	if ( Message.isDebugOn ) {
                		Message.printDebug(1,routine,"  Datastore name \"" + name + "\" is not active." );
                	}
            		dataStore = null;
            	}
            }
           	// Return the matched datastore.
           	return dataStore;
        }
    }
	// No datastore was matched.
    return null;
}

/**
Return the list of all DataStore instances known to the processor.
These are named database connections that correspond to input type/name for time series.
Active and inactive datastores are returned.
*/
public List<DataStore> getDataStores() {
    return __tsengine.getDataStoreList();
}

/**
Return the list of all DataStore instances known to the processor.
These are named database connections that correspond to input type/name for time series.
*/
public List<DataStore> getDataStores ( boolean activeOnly ) {
	// Get the list of all datastores.
	List<DataStore> datastoreList = __tsengine.getDataStoreList();
	if ( activeOnly ) {
		// Loop through and remove datastores where status != 0 (no error).
		for ( int i = datastoreList.size() - 1; i >= 0; i-- ) {
			DataStore ds = datastoreList.get(i);
			if ( ds.getStatus() != 0 ) {
				datastoreList.remove(i);
			}
		}
	}
	return datastoreList;
}

/**
Return the list of data stores for the requested type (e.g., HydroBaseDataStore).
A non-null list is guaranteed, but the list may be empty.
Only active datastores are returned, those that are enabled and status is 0 (Ok - no error).
@param dataStoreClass the data store class to match (required).
@return the list of data stores matching the requested type
*/
public List<DataStore> getDataStoresByType ( Class<?> dataStoreClass ) {
	return getDataStoresByType ( dataStoreClass, true );
}

/**
Return the list of data stores for the requested type (e.g., HydroBaseDataStore).
A non-null list is guaranteed, but the list may be empty.
@param dataStoreClass the data store class to match (required).
@param activeOnly if true, only return datastores that are active (open)
@return the list of data stores matching the requested type
*/
public List<DataStore> getDataStoresByType ( Class<?> dataStoreClass, boolean activeOnly ) {
	String routine = getClass().getSimpleName() + ".getDataStoresByType";
	List<DataStore> dataStoreList = new ArrayList<>();
	boolean debug = Message.isDebugOn;
	if ( debug ) {
		Message.printDebug(1, routine,
			"Getting datastores for class by checking " + getDataStores().size() + " datastores for: " + dataStoreClass);
	}
    for ( DataStore dataStore : getDataStores() ) {
   		if ( debug ) {
   			Message.printDebug(1, routine, "Checking datastore " + dataStore.getName() + " class " + dataStore.getClass() );
   		}
    	if ( activeOnly && (dataStore.getStatus() != 0) ) {
    		// If only active are requested, then status must be 0 (OK).
    		if ( debug ) {
    			Message.printDebug(1, routine, "  Datastore " + dataStore.getName() +
    				" is not active (status is " + dataStore.getStatus() + ") - not a match since ignoring.");
    		}
    		continue;
    	}
        // Check for exact match on class.
        if ( dataStore.getClass() == dataStoreClass ) {
        	if ( debug ) {
        		Message.printDebug(1, routine, "  Found a match because datastore " + dataStore.getClass() +
        			" is the same as requested class " + dataStoreClass + " and status=" + dataStore.getStatus());
        	}
            dataStoreList.add(dataStore);
        }
        // The following will work if the objects were loaded by two different class loaders, but problems will arise later:
        // - TODO smalers 2020-07-26 figuring this out during adding a new plugin that is causing ClassCastExceptions
        // - it is generally best to make sure a single class loader is used to load the code
        /*
        else if ( ("" + dataStoreClass).equals("" + dataStore.getClass()) ) {
        	if ( debug ) {
        		Message.printStatus(2, routine, "Found a match comparing class strings for datastore:  " + dataStore.getClass() );
        	}
            dataStoreList.add(dataStore);
        }
        */
        // Also check for common base classes.
        // TODO SAM 2012-01-31 Why not just use instanceof all the time?
        else if ( (dataStoreClass == DatabaseDataStore.class) && dataStore instanceof DatabaseDataStore ) {
        	if ( debug ) {
        		Message.printDebug(1, routine, "  Found a match because " + dataStoreClass + " == DatabaseDataStore class and " +
        			dataStore.getClass() + " instanceof DatabaseDataStore and status=" + dataStore.getStatus());
        	}
            dataStoreList.add(dataStore);
        }
        else if ( (dataStoreClass == WebServiceDataStore.class) && dataStore instanceof WebServiceDataStore ) {
        	if ( debug ) {
        		Message.printDebug(1, routine, "  Found a match because " + dataStoreClass + " == WebServiceDataStore class and " +
        			dataStore.getClass() + " instanceof WebServiceDataStore and status=" + dataStore.getStatus());
        	}
            dataStoreList.add(dataStore);
        }
        else {
        	if ( debug ) {
        		Message.printDebug(1, routine, "  Datastore " + dataStore.getName() + " (" + dataStore.getClass() + ") is not a match.");
        	}
        }
    }
    return dataStoreList;
}

/**
Return the list of DataStore substitutions.
@return the list of DataStore substitutions.
*/
public List<DataStoreSubstitute> getDataStoreSubstituteList() {
    return __tsengine.getDataStoreSubstituteList();
}

/**
Return an Ensemble matching the requested identifier, or null if not found.
This method is meant to be used internally without going through the request mechanism.
@param EnsembleID Ensemble ID to match, * wildcard is special value to match first ensemble,
*pattern will match the first matching TSID.
*/
protected TSEnsemble getEnsemble ( String EnsembleID ) {
    if ( (EnsembleID == null) || EnsembleID.equals("") ) {
        return null;
    }
    int size = __TSEnsembleList.size();
    TSEnsemble tsensemble = null, tsensemble2;
    String ensembleIDPattern = null;
    if ( EnsembleID.indexOf("*") >= 0 ) {
    	ensembleIDPattern = EnsembleID.replace("*", ".*").toUpperCase();
    }
    for ( int i = 0; i < size; i++ ) {
        tsensemble2 = __TSEnsembleList.get(i);
        if ( tsensemble2 == null ) {
            continue;
        }
        // See if the ensemble ID matches the requested expression.
        if ( ensembleIDPattern == null ) {
        	// Do a simple comparison as per legacy code.
        	// This guards against someone having perhaps used special characters for ensemble ID.
	        if ( tsensemble2.getEnsembleID().equalsIgnoreCase(EnsembleID) ) {
	            tsensemble = tsensemble2;
	            break;
	        }
        }
        else {
        	// Do a regular expression match.
	        if ( tsensemble2.getEnsembleID().toUpperCase().matches(ensembleIDPattern) ) {
	            tsensemble = tsensemble2;
	            break;
	        }
        }
    }
    return tsensemble;
}

/**
 * Return the initial properties, which are those from the application command line.
 * These properties should be chained from one processor to another
 * (such as used by RunCommands command) to ensure that the original environment is available for all processing.
 * @return the initial PropList, which holds properties from the command line
 */
public PropList getInitialPropList () {
	return this.initialProps;
}

/**
Return the initial working directory for the processor.
@return the initial working directory for the processor.
*/
public String getInitialWorkingDir () {
	return __InitialWorkingDir_String;
}

/**
Indicate whether the processing is running.
@return true if the command processor is running, false if not.
*/
public boolean getIsRunning () {
	return __is_running;
}

/**
 * Get the plugin class list.
 * This is necessary to hand off the list to RunCommands command.
 * @return the plugin class list.
 */
public List<Class> getPluginCommandClasses () {
	return this.pluginCommandClassList;
}

/**
Return data for a named property, required by the CommandProcessor interface.
See getPropcontents() for a list of properties that are handled.
@param prop Property to set.
@return the named property, or null if a value is not found.
@exception Exception if the property cannot be found or there is an error determining the property.
*/
public Prop getProp ( String prop ) throws Exception {
	Object o = getPropContents ( prop );
	if ( o == null ) {
		return null;
	}
	else {
	    // Contents will be a Vector, etc., so convert to a full property.
		// TODO SAM 2005-05-13 This will work seamlessly for strings but may have a side-effects (conversions) for non-strings.
		Prop p = new Prop ( prop, o, o.toString() );
		return p;
	}
}

// TODO SAM 2007-02-18 Need to enable NDFD Adapter.
/**
Return the contents for a named property, required by the CommandProcessor interface.
Internal properties are checked first and if not matched the list of user-defined properties is searched.
Currently the following internal properties are handled:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>
</tr>

<td><b>CreateOutput</b></td>
<td>Indicate if output should be created, as Boolean.
If True, commands that create output should do so.  If False, the commands should be skipped.
This is used to speed performance during initial testing.
</td>
</tr>

<tr>
<td><b>DataTestList</b></td>
<td>The List of DataTests.</td>
</tr>

<tr>
<td><b>DebugLevelLogFile</b></td>
<td>The debug level for log file messages.</td>
</tr>

<tr>
<td><b>DebugLevelScreen</b></td>
<td>The debug level for screen messages.</td>
</tr>

<tr>
<td><b>HaveOutputPeriod</b></td>
<td>Indicate whether the output period has been specified, as a Boolean.
</td>
</tr>

<tr>
<td><b>HydroBaseDMIList</b></td>
<td>A List of open HydroBaseDMI, available for reading.
</td>
</tr>

<tr>
<td><b>InitialWorkingDir</b></td>
<td>The initial working directory for the processor, typically the directory where the commands file lives.
</td>
</tr>

<tr>
<td><b>InputEnd</b></td>
<td>The input end from the setInputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>InputStart</b></td>
<td>The input start from the setInputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>OutputComments</b></td>
<td>A List of String with comments suitable for output.
The comments DO NOT contain the leading comment character
(specific code that writes the output should add the comment characters).
Currently the comments contain open HydroBase connection information, if available.
</td>
</tr>

<tr>
<td><b>OutputEnd</b></td>
<td>The output end from the setOutputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>OutputFileList</b></td>
<td>The list of output files from processing commands as List<File>.
</td>
</tr>

<tr>
<td><b>OutputStart</b></td>
<td>The output start from the setOutputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>OutputYearType</b></td>
<td>The output year type, as a String ("Calendar", "Water" (Oct-Nov), or "NovToOct" (Nov-Oct)).
</td>
</tr>

<tr>
<td><b>NDFDAdapterList</b></td>
<td>A list of available NDFDAdapter, available for reading.  THIS HAS NOT BEEN IMPLEMENTED.
</td>
</tr>

<tr>
<td><b>PatternTSList</b></td>
<td>The pattern time series as a List.</td>
</tr>

<tr>
<td><b>StartLogEnabled</b></td>
<td>Indicate whether StartLog commands are enabled, as Boolean.</td>
</tr>

<tr>
<td><b>TableResultsList</b></td>
<td>The table results list as a List.</td>
</tr>

<tr>
<td><b>TSProductAnnotationProviderList</b></td>
<td>A List of TSProductAnnotationProvider (for example, this is requested by the processTSProduct() command).
</td>
</tr>

<tr>
<td><b>TSResultsList</b></td>
<td>The List of time series results.</td>
</tr>

<tr>
<td><b>TSResultsListSize</b></td>
<td>The size of the TSResultsList as an Integer</td>
</tr>

<tr>
<td><b>TSViewWindowListener</b></td>
<td>The WindowListener that is interested in listing to TSView window events.
This is used when processing a TSProduct in batch mode so that the main
application can close when the TSView window is closed.</td>
</tr>

<tr>
<td><b>WarningLevelLogFile</b></td>
<td>The warning level for log file messages.</td>
</tr>

<tr>
<td><b>WarningLevelScreen</b></td>
<td>The warning level for screen messages.</td>
</tr>

<tr>
<td><b>WorkingDir</b></td>
<td>The current working directory for the processor,
reflecting the initial working directory and changes from setWorkingDir() commands.
</td>
</tr>

</table>
@return the contents for a named property, or null if a value is not found.
@exception UnrecognizedRequestException if an unknown property is requested.
*/
public Object getPropContents ( String propName ) throws Exception {
	if ( propName.equalsIgnoreCase("AutoExtendPeriod") ) {
		return getPropContents_AutoExtendPeriod();
	}
    else if ( propName.equalsIgnoreCase("AverageEnd") ) {
        return getPropContents_AverageEnd();
    }
    else if ( propName.equalsIgnoreCase("AverageStart") ) {
        return getPropContents_AverageStart();
    }
    else if ( propName.equalsIgnoreCase("CommandFileName") ) {
        return getCommandFileName();
    }
    else if ( propName.equalsIgnoreCase("CommandsShouldClearRunStatus") ) {
        return getPropContents_CommandsShouldClearRunStatus();
    }
	else if ( propName.equalsIgnoreCase("CreateOutput") ) {
		return getPropContents_CreateOutput();
	}
    else if ( propName.equalsIgnoreCase("DebugLevelLogFile") ) {
        return new Integer(Message.getDebugLevel(Message.LOG_OUTPUT));
    }
    else if ( propName.equalsIgnoreCase("DebugLevelScreen") ) {
        return new Integer(Message.getDebugLevel(Message.TERM_OUTPUT));
    }
    else if ( propName.equalsIgnoreCase("EnsembleResultsList") ) {
        return getPropContents_EnsembleResultsList();
    }
	else if ( propName.equalsIgnoreCase("HaveOutputPeriod") ) {
		return getPropContents_HaveOutputPeriod();
	}
    else if ( propName.equalsIgnoreCase("HydroBaseDMIList") ) {
        return getPropContents_HydroBaseDMIList();
    }
    else if ( propName.equalsIgnoreCase("HydroBaseDMIListSize") ) {
        return getPropContents_HydroBaseDMIListSize();
    }
    else if ( propName.equalsIgnoreCase("IgnoreLEZero") ) {
        return getPropContents_IgnoreLEZero();
    }
	else if ( propName.equalsIgnoreCase("IncludeMissingTS") ) {
		return getPropContents_IncludeMissingTS();
	}
	else if ( propName.equalsIgnoreCase("InitialWorkingDir") ) {
		return getPropContents_InitialWorkingDir();
	}
	else if ( propName.equalsIgnoreCase("InputEnd") ) {
		return getPropContents_InputEnd();
	}
	else if ( propName.equalsIgnoreCase("InputStart") ) {
		return getPropContents_InputStart();
	}
    else if ( propName.equalsIgnoreCase("NetworkResultsList") ) {
        return getPropContents_NetworkResultsList();
    }
    else if ( propName.equalsIgnoreCase("ObjectResultsList") ) {
        return getPropContents_ObjectResultsList();
    }
	else if ( propName.equalsIgnoreCase("OutputComments") ) {
		return getPropContents_OutputComments();
	}
	else if ( propName.equalsIgnoreCase("OutputEnd") ) {
		return getPropContents_OutputEnd();
	}
	else if ( propName.equalsIgnoreCase("OutputFileList") ) {
		return getPropContents_OutputFileList();
	}
	else if ( propName.equalsIgnoreCase("OutputStart") ) {
		return getPropContents_OutputStart();
	}
	else if ( propName.equalsIgnoreCase("OutputYearType") ) {
		return getPropContents_OutputYearType();
	}
    else if ( propName.equalsIgnoreCase("PatternTSList") ) {
        return getPropContents_PatternTSList();
    }
    else if ( propName.equalsIgnoreCase("StartLogEnabled") ) {
        return getPropContents_StartLogEnabled();
    }
    else if ( propName.equalsIgnoreCase("TableResultsList") ) {
        return getPropContents_TableResultsList();
    }
    else if ( propName.equalsIgnoreCase("TimeSeriesViewResultsList") ) {
        return getPropContents_TimeSeriesViewResultsList();
    }
    else if ( propName.equalsIgnoreCase("TSEnsembleResultsListSize") ) {
        return getPropContents_TSEnsembleResultsListSize();
    }
	else if ( propName.equalsIgnoreCase("TSProductAnnotationProviderList") ) {
		return getPropContents_TSProductAnnotationProviderList();
	}
	else if ( propName.equalsIgnoreCase("TSResultsList") ) {
		return getPropContents_TSResultsList();
	}
	else if ( propName.equalsIgnoreCase("TSResultsListSize") ) {
		return getPropContents_TSResultsListSize();
	}
	else if ( propName.equalsIgnoreCase("TSViewWindowListener") ) {
		return getPropContents_TSViewWindowListener();
	}
    else if ( propName.equalsIgnoreCase("WarningLevelLogFile") ) {
        return new Integer(Message.getWarningLevel(Message.LOG_OUTPUT));
    }
    else if ( propName.equalsIgnoreCase("WarningLevelScreen") ) {
        return new Integer(Message.getWarningLevel(Message.TERM_OUTPUT));
    }
	else if ( propName.equalsIgnoreCase("WorkingDir") ) {
		return getPropContents_WorkingDir();
	}
	else if ( propName.equalsIgnoreCase("WorkingDirPortable") ) {
		return IOUtil.toPortablePath(getPropContents_WorkingDir());
	}
	else if ( propName.equalsIgnoreCase("WorkingDirPosix") ) {
		return IOUtil.toPosixPath(getPropContents_WorkingDir());
	}
	else {
	    // Property is not one of the individual objects that have been historically maintained,
		// but it may be a user-supplied property in the hashtable.
	    Object o = __propertyHashmap.get ( propName );
	    if ( o == null ) {
	    	// Changed on 2016-09-18 to allow null to be returned,
	    	// generally indicating that user-supplied property is being processed.
    	    //String warning = "Unknown GetPropContents request \"" + propName + "\"";
    		// TODO SAM 2007-02-07 Need to figure out a way to indicate an error and pass back useful information.
    		//throw new UnrecognizedRequestException ( warning );
	    	return null;
	    }
	    else {
	        // Return the object from the hashtable.
	        return o;
	    }
	}
}

/**
Handle the AutoExtendPeriod property request.
@return Boolean depending on whether time series periods should automatically be extended during reads.
*/
private Boolean getPropContents_AutoExtendPeriod() {
	boolean b = __tsengine.getAutoExtendPeriod();
	return new Boolean ( b );
}

/**
Handle the AverageEnd property request.
@return DateTime for AverageEnd, or null if not set.
*/
private DateTime getPropContents_AverageEnd() {
    return __tsengine.getAverageEnd();
}

/**
Handle the AverageStart property request.
@return DateTime for AverageStart, or null if not set.
*/
private DateTime getPropContents_AverageStart() {
    return __tsengine.getAverageStart();
}

/**
Handle the CommandsShouldClearRunStatus property request.
@return Boolean indicating whether run status should be cleared.
*/
private Boolean getPropContents_CommandsShouldClearRunStatus() {
	return __commandsShouldClearRunStatus;
}

/**
Handle the CreateOutput property request.
@return Boolean indicating whether output should be created.
*/
private Boolean getPropContents_CreateOutput() {
	return __CreateOutput_Boolean;
}

/**
Handle the EnsembleResultsList property request.
@return The ensemble results list, as a List of TSEnsemble.
*/
private List<TSEnsemble> getPropContents_EnsembleResultsList() {
    return __TSEnsembleList;
}

/**
Handle the HaveOutputPeriod property request.
@return Boolean depending on whether the output period has been set.
*/
private Boolean getPropContents_HaveOutputPeriod() {
	boolean b = __tsengine.haveOutputPeriod();
	return new Boolean ( b );
}

/**
Handle the HydroBaseDMIList property request.
@return list of open HydroBaseDMI instances.
*/
private List<HydroBaseDMI> getPropContents_HydroBaseDMIList() {
	return __tsengine.getHydroBaseDMIList();
}

/**
Handle the HydroBaseDMIListSize property request.
@return Number of open HydroBaseDMI instances.
*/
private Integer getPropContents_HydroBaseDMIListSize() {
    List<HydroBaseDMI> v = __tsengine.getHydroBaseDMIList();
    if ( v == null ) {
        return new Integer(0);
    }
    else {
        return new Integer(v.size());
    }
}

/**
Handle the IgnoreLEZero property request.
@return Boolean indicating whether values <= 0 should be included in historical averages.
*/
private Boolean getPropContents_IgnoreLEZero() {
    boolean b = __tsengine.getIgnoreLEZero();
    return new Boolean ( b );
}

/**
Handle the IncludeMissingTS property request.
@return Boolean indicating whether missing time series should be created when no data found.
*/
private Boolean getPropContents_IncludeMissingTS() {
	boolean b = __tsengine.getIncludeMissingTS();
	return new Boolean ( b );
}

/**
Handle the InitialWorkingDir property request.
The initial working directory is the home of the commands file if read/saved and should be
passed by the calling code when running commands.
Use getPropContents_WorkingDir to get the working directory after processing.
@return The working directory, as a String.
*/
private String getPropContents_InitialWorkingDir() {
	return getInitialWorkingDir();
}

/**
Handle the InputEnd property request.
@return DateTime for InputEnd, or null if not set.
*/
private DateTime getPropContents_InputEnd() {
	return __tsengine.getInputEnd();
}

/**
Handle the InputStart property request.
@return DateTime for InputStart, or null if not set.
*/
private DateTime getPropContents_InputStart() {
	return __tsengine.getInputStart();
}

/**
Handle the NetworkResultsList property request.
@return The network results list, as a List of NodeNetwork.
*/
private List<NodeNetwork> getPropContents_NetworkResultsList() {
    return this.__NodeNetworkList;
}

/**
Handle the ObjectResultsList property request.
@return The object results list, as a List of JSONObject.
*/
private List<JSONObject> getPropContents_ObjectResultsList() {
    return this.__jsonObjectList;
}

/**
Handle the OutputComments property request.  This includes, for example,
the commands that are active and HydroBase version information that documents data available for a command.
The leading comment character is NOT included since it will be added before final output by calling code.
@return list of String containing comments for output.
*/
private List<String> getPropContents_OutputComments() {
	String [] array = __tsengine.formatOutputHeaderComments(getCommands());
	List<String> v = new ArrayList<>();
	if ( array != null ) {
		for ( int i = 0; i < array.length; i++ ) {
			v.add( array[i]);
		}
	}
	return v;
}

/**
Handle the OutputEnd property request.
@return DateTime for OutputEnd, or null if not set.
*/
private DateTime getPropContents_OutputEnd() {
	return __tsengine.getOutputEnd();
}

/**
Handle the OutputFileList property request.
@return DateTime for OutputFileList, or null if not set.
*/
private List<File> getPropContents_OutputFileList() {
	return __outputFileList;
}

/**
Handle the OutputStart property request.
@return DateTime for OutputStart, or null if not set.
*/
private DateTime getPropContents_OutputStart() {
	return __tsengine.getOutputStart();
}

/**
Handle the OutputYearType property request.
@return YearType for OutputYearType, should always be non-null (default is calendar).
*/
private YearType getPropContents_OutputYearType() {
	return __tsengine.getOutputYearType();
}

/**
Handle the PatternTSList property request.
@return The pattern time series results list, as a List of StringMonthTS.
*/
private List<StringMonthTS> getPropContents_PatternTSList() {
    return __patternTSList;
}

/**
Handle the StartLogEnabled property request.
@return whether the StartLog command is enabled
*/
private Boolean getPropContents_StartLogEnabled() {
    return __StartLogEnabled_Boolean;
}

/**
Handle the TableResultsList property request.
@return The table results list, as a List of DataTable.
*/
private List<DataTable> getPropContents_TableResultsList() {
    return __TableList;
}

/**
Handle the TimeSeriesViewResultsList property request.
@return The TimeSeriesView results list.
*/
private List<TimeSeriesView> getPropContents_TimeSeriesViewResultsList() {
    return __TimeSeriesViewList;
}

/**
Handle the TSEnsembleResultsListSize property request.
@return Size of the time series ensemble results list, as an Integer.
*/
private Integer getPropContents_TSEnsembleResultsListSize() {
    return new Integer( __TSEnsembleList.size());
}

/**
Handle the TSProductAnnotationProviderList property request.
@return The time series product annotation provider list, as a list of TSProductAnnotationProvider.
*/
private List<TSProductAnnotationProvider> getPropContents_TSProductAnnotationProviderList() {
	return __tsengine.getTSProductAnnotationProviders();
}

/**
Handle the TSResultsList property request.
@return The time series results list, as a list of TS.
*/
private List<TS> getPropContents_TSResultsList() {
	return __tsengine.getTimeSeriesList(null);
}

/**
Handle the TSResultsListSize property request.
@return Size of the time series results list, as an Integer.
*/
private Integer getPropContents_TSResultsListSize() {
	return new Integer( __tsengine.getTimeSeriesList(null).size());
}

/**
Handle the TSViewWindowListener property request.
@return TSViewWindowListener that listens for plot windows closing.
*/
private WindowListener getPropContents_TSViewWindowListener() {
	return __tsengine.getTSViewWindowListener();
}

/**
Handle the WorkingDir property request.
The working directory is set based on the initial working directory and subsequent setWorkingDir() commands.
@return The working directory, as a String.
*/
private String getPropContents_WorkingDir() {
	return getWorkingDir();
}

/**
Return the list of property names available from the processor.
These properties can be requested using getPropContents().
@param includeBuiltinProperties if true, include the list of built-in property names.
@param includeDynamicPoperties if true, include the list of dynamically-defined property names.
*/
public Collection<String> getPropertyNameList ( boolean includeBuiltInProperties, boolean includeDynamicProperties ) {
    // Create a set that includes the above.
    TreeSet<String> set = new TreeSet<>();
	// FIXME SAM 2008-02-15 Evaluate whether these should be in the property hashmap.
    // Should properties be available before ever being defined (in case they are used later)
    // or should only defined properties be available (and rely on discovery to pass to other commands)?
    // For now important properties are represented as data members in this class.
    //
	// Add properties that are accessible with getPropContents_XXXX methods.
	if ( includeBuiltInProperties ) {
	    List<String> v = new ArrayList<>();
        v.add ( "AutoExtendPeriod" );
        v.add ( "AverageStart" );
        v.add ( "AverageEnd" );
        v.add ( "CreateOutput" ); // Useful?
        v.add ( "DebugLevelLogFile" );
        v.add ( "DebugLevelScreen" );
        v.add ( "HaveOutputPeriod" ); // Useful?
        v.add ( "HydroBaseDMIListSize" );
        v.add ( "IgnoreLEZero" );
        v.add ( "IncludeMissingTS" );
        v.add ( "InitialWorkingDir" );
    	v.add ( "InputStart" );
    	v.add ( "InputEnd" );
    	//v.add ( "OutputComments" ); // Not sure this needs to be visible.
    	v.add ( "OutputStart" );
    	v.add ( "OutputEnd" );
        v.add ( "OutputYearType" );
        v.add ( "StartLogEnabled" );
        v.add ( "TSEnsembleResultsListSize" );   // Useful for testing when zero time series are expected.
        v.add ( "TSResultsListSize" );   // Useful for testing when zero time series are expected.
        v.add ( "WarningLevelLogFile" );
        v.add ( "WarningLevelScreen" );
        v.add ( "WorkingDir" );
        v.add ( "WorkingDirPortable" );
        v.add ( "WorkingDirPosix" );
        set.addAll ( v );
	}
    if ( includeDynamicProperties ) {
        // Add the hashtable keys and make a unique list.
        set.addAll ( __propertyHashmap.keySet() );
    }
	return set;
}

/**
Determine if the commands are read-only.
In this case, applications may disable save features.
The special comment "#@readOnly" indicates that the commands are read-only.
@return true if read-only, false if can be written.
*/
public boolean getReadOnly () {
    // String that indicates readOnly
    String readOnlyString = "@readOnly";
    // Loop through the commands and check comments for the special string.
    int size = size();
    Command c;
    for ( int i = 0; i < size; i++ ) {
        c = __CommandList.get(i);
        String commandString = c.toString();
        if ( commandString.trim().startsWith("#") &&
                (StringUtil.indexOfIgnoreCase(commandString,readOnlyString,0) > 0) ) {
            return true;
        }
    }
    return false;
}

/**
Return the TSSupplier name.
@return the TSSupplier name ("TSEngine").
*/
public String getTSSupplierName() {
	return __tsengine.getTSSupplierName();
}

/**
Return the current working directory for the processor.
@return the current working directory for the processor.
*/
protected String getWorkingDir () {
	return __WorkingDir_String;
}

/**
Handle the CommandProcessorEvent events generated during processing and format for output.
Currently this method passes on the events to listeners registered on this processor.
@param event CommandProcessorEvent to handle.
*/
public void handleCommandProcessorEvent ( CommandProcessorEvent event ) {
    if ( __CommandProcessorEventListener_array != null ) {
        for ( int i = 0; i < __CommandProcessorEventListener_array.length; i++ ) {
            __CommandProcessorEventListener_array[i].handleCommandProcessorEvent(event);
        }
    }
}

/**
Determine the index of a command in the processor.  A reference comparison occurs.
@param command A command to search for in the processor.
@return the index (0+) of the matching command, or -1 if not found.
*/
public int indexOf ( Command command ) {
	return TSCommandProcessorUtil.indexOf(this,command,0);
}

/**
Add a command using the Command instance.
@param command Command to insert.
@param index Index (0+) at which to insert the command.
*/
public void insertCommandAt ( Command command, int index ) {
	String routine = getClass().getSimpleName() + ".insertCommandAt";
	__CommandList.add( index, command);
	// Also add this processor as a listener for events.
    if ( command instanceof CommandProcessorEventProvider ) {
        CommandProcessorEventProvider ep = (CommandProcessorEventProvider)command;
        ep.addCommandProcessorEventListener(this);
    }
	notifyCommandListListenersOfAdd ( index, index );
	Message.printStatus(2, routine, "Inserted command object \"" + command + "\" at [" + index + "]" );
}

/**
Add a command using the string text.
This should currently only be used for commands that do not have command classes,
which perform additional validation on the commands.
A GenericCommand instance will be instantiated to maintain the string and allow command status to be set.
@param command_string Command string for command.
@param index Index (0+) at which to insert the command.
*/
public void insertCommandAt ( String command_string, int index ) {
	String routine = getClass().getSimpleName() + ".insertCommandAt";
	Command command = new GenericCommand ();
	command.setCommandString ( command_string );
	insertCommandAt ( command, index );
	// Also add this processor as a listener for events.
    if ( command instanceof CommandProcessorEventProvider ) {
        CommandProcessorEventProvider ep = (CommandProcessorEventProvider)command;
        ep.addCommandProcessorEventListener(this);
    }
	Message.printStatus(2, routine, "Creating generic command from string \"" + command_string + "\"." );
}

/**
Return a SetWorkingDir(xxx) command where xxx is the initial working directory.
This command should be prepended to the list of SetWorkingDir() commands that
are processed when determining the working directory for an edit dialog or other command-context action.
*/
private Command newInitialSetWorkingDirCommand()
throws InvalidCommandParameterException, InvalidCommandSyntaxException {
	boolean doGeneric = false;
	String commandString = "SetWorkingDir(WorkingDir=\"" + getInitialWorkingDir() + "\")";
	if ( doGeneric ) {
		// Older code used this.
		// For now put in a generic command since no specific Command class is available.
		GenericCommand c = new GenericCommand ();
		c.setCommandName("SetWorkingDir");
		c.setCommandString( commandString );
		// TODO SAM 2007-08-22 Need to implement the command class.
		return c;
	}
	else {
		// Create a new command.
		SetWorkingDir_Command c = new SetWorkingDir_Command();
		c.initializeCommand ( commandString, this, true );	// Full initialization.
		return c;
	}
}

/**
Notify registered CommandListListeners about one or more commands being added.
@param index0 The index (0+) of the first command that is added.
@param index1 The index (0+) of the last command that is added.
*/
private void notifyCommandListListenersOfAdd ( int index0, int index1 ) {
	if ( __CommandListListener_array != null ) {
		for ( int i = 0; i < __CommandListListener_array.length; i++ ) {
			__CommandListListener_array[i].commandAdded(index0, index1);
		}
	}
}

/**
Notify registered CommandListListeners about one or more commands being changed.
@param index0 The index (0+) of the first command that is added.
@param index1 The index (0+) of the last command that is added.
*/
@SuppressWarnings("unused")
private void notifyCommandListListenersOfChange ( int index0, int index1 ) {
	if ( __CommandListListener_array != null ) {
		for ( int i = 0; i < __CommandListListener_array.length; i++ ) {
			__CommandListListener_array[i].commandChanged(index0, index1);
		}
	}
}

/**
Notify registered CommandListListeners about one or more commands being removed.
@param index0 The index (0+) of the first command that is added.
@param index1 The index (0+) of the last command that is added.
*/
private void notifyCommandListListenersOfRemove ( int index0, int index1 ) {
	if ( __CommandListListener_array != null ) {
		for ( int i = 0; i < __CommandListListener_array.length; i++ ) {
			__CommandListListener_array[i].commandRemoved(index0, index1);
		}
	}
}

/**
Notify registered CommandProcessorListeners about a command being canceled.
@param icommand The index (0+) of the command that is canceled.
@param ncommand The number of commands being processed.
This will often be the total number of commands but calling code may process a subset.
@param command The instance of the nearest command that is being canceled.
*/
protected void notifyCommandProcessorListenersOfCommandCancelled ( int icommand, int ncommand, Command command ) {
	// This method is protected to allow TSEngine to call.
	if ( __CommandProcessorListener_array != null ) {
		for ( int i = 0; i < __CommandProcessorListener_array.length; i++ ) {
			__CommandProcessorListener_array[i].commandCanceled(icommand,ncommand,command,-1.0F,"Command cancelled.");
		}
	}
}

/**
Notify registered CommandProcessorListeners about a command completing.
@param icommand The index (0+) of the command that is completing.
@param ncommand The number of commands being processed.
This will often be the total number of commands but calling code may process a subset.
@param command The instance of the command that is completing.
*/
protected void notifyCommandProcessorListenersOfCommandCompleted ( int icommand, int ncommand, Command command ) {
	// This method is protected to allow TSEngine to call.
	if ( __CommandProcessorListener_array != null ) {
		for ( int i = 0; i < __CommandProcessorListener_array.length; i++ ) {
			__CommandProcessorListener_array[i].commandCompleted(icommand,ncommand,command,-1.0F,"Command completed.");
		}
	}
}

/**
Notify registered CommandProcessorListeners about a command starting.
@param icommand The index (0+) of the command that is starting.
@param ncommand The number of commands being processed.
This will often be the total number of commands but calling code may process a subset.
@param command The instance of the command that is starting.
*/
protected void notifyCommandProcessorListenersOfCommandStarted ( int icommand, int ncommand, Command command ) {
	// This method is protected to allow TSEngine to call.
	if ( __CommandProcessorListener_array != null ) {
		for ( int i = 0; i < __CommandProcessorListener_array.length; i++ ) {
			__CommandProcessorListener_array[i].commandStarted(icommand,ncommand,command,-1.0F,"Command started.");
		}
	}
}

/**
 * Process a request with no initial properties for the processor.
 * @param request the request being processed
 * @param requestParams request parameters - see overloaded method for documentation
 */
public CommandProcessorRequestResultsBean processRequest ( String request, PropList requestParams )
throws Exception {
	return processRequest ( request, requestParams, null );
}

/**
Process a request, required by the CommandProcessor interface.
This is a generalized way to allow commands to call specialized functionality
through the interface without directly naming a processor.
For example, the request may involve data that only the TSCommandProcessor has access to and that a command does not.
Currently the following requests are handled:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Request</b></td>	<td><b>Description</b></td>
</tr>

<tr>
<td><b>AddCommandProcessorEventListener</b></td>
<td>Add a CommandProcessorEventListener to the processor,
which will pass on events from commands to these listeners.
It is expected that the listener will be added before each run (via commands) and will be removed at the end of the run.
This design may need to change as testing occurs.  Parameters to this request are:
<ol>
<li>    <b>TS</b> Monthly time series to process, as TS (MonthTS) object.</li>
<li>    <b>Index</b> The index (0+) of the time series identifier being processed, as an Integer.</li>
</ol>
Returned values from this request are:
<ol>
<li>    <b>CommandProcessorEventListener</b>the listener to add.</li>
</ol>
</td>
</tr>

<tr>
<td><b>CalculateTSAverageLimits</b></td>
<td>Calculate the average data limits for a time series using the averaging period if specified (otherwise use the available period).
	Currently only limits for monthly time series are supported.
	Parameters to this request are:
<ol>
<li>	<b>TS</b> Monthly time series to process, as TS (MonthTS) object.</li>
<li>	<b>Index</b> The index (0+) of the time series identifier being processed, as an Integer.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TSLimits</b>the average data for a time series.
	If a monthly time series, a MonthTSLimits will be returned.</li>
</ol>
</td>
</tr>

<tr>
<td><b>DateTime</b></td>
<td>Get a date/time from a string.  This is done using the following rules:
<ol>
<li>	If the string is "*" or "", return null.</li>
<li>	If the string uses a standard name OutputStart, OutputEnd,
		InputStart (previously QueryStart),
		InputEnd (previously QueryEnd), return the corresponding DateTime.</li>
<li>	Check the date/time hash table for user-defined date/times.
<li>	Parse the string.
</ol>
	Parameters to this request are:
<ol>
<li>	<b>DateTime</b> The date/time to parse, as a String.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>DateTime</b>  The resulting date/time, as a DateTime object.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetHydroBaseDMI</b></td>
<td>	Return the HydroBaseDMI that is being used.
	Parameters for this request are:
	<ol>
	<li><b>InputName</b> Input name for the DMI as a String, can be blank.</li>
	</ol>
	Returned values from this request are:
	<ol>
	<li><b>HydroBaseDMI</b> The HydroBaseDMI instance matching the input name (may return null).</li>
	</ol>
</td>
</tr>

<tr>
<td><b>GetObject</b></td>
<td>Get a JSONObject instance managed by the processor.  Parameters to this request are:
<ol>
<li>    <b>ObjectID</b> An object identifier.</li>
</ol>
Returned values from this request are:
<ol>
<li>    <b>Object</b> A JSONObject object.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetProperty</b></td>
<td>Get a processor property.  Parameters to this request are:
<ol>
<li>    <b>PropertyName</b> The property name.</li>
</ol>
Returned values from this request are:
<ol>
<li>    <b>PropertyValue</b> The property value, as an object (e.g., DateTime, Double, Integer, or String.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetPropertyHashtable</b></td>
<td>Get the hash table of processor properties.  Parameters to this request are:
<ol>
<li>    <b>GetUserProperties</b> Set to "true" if the list of user-supplied properties is to be returned.</li>
</ol>
Returned values from this request are:
<ol>
<li>    <b>PropertyHashtable</b> Hashtable of properties including internal and user-defined properties.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetTable</b></td>
<td>Get a DataTable instance managed by the processor.  Parameters to this request are:
<ol>
<li>    <b>TableID</b> A table identifier.</li>
</ol>
Returned values from this request are:
<ol>
<li>    <b>Table</b> A DataTable object.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetTimeSeries</b></td>
<td>Return a time series from either the __tslist vector or the BinaryTS file, as appropriate.
If a BinaryTS is returned, it is a new instance from the file and should be set to null when done.
Parameters to this request are:
<ol>
<li>	<b>Index</b> The index (0+) of the time series identifier being requested, as an Integer.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TS</b> The requested time series or null if none is available, as a TS instance.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetTimeSeriesForTSID</b></td>
<td>Return a time series from either the __tslist vector or the BinaryTS file, as appropriate.
If a BinaryTS is returned, it is a new instance from the file and should be set to null when done.
The search is performed backwards in the list,
assuming that the commands are being processed sequentially and therefore
any reference to a duplicate ID would intuitively be referring to the latest instance in the list.
For this version of the method, the trace (sequence number) is ignored.  Parameters to this request are:
<ol>
<li>	<b>TSID</b> The time series identifier of the time series being requested
		(either an alias or TSIdent string), as a String.</li>
<li>	</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TS</b> The requested time series or null if none is available, as a TS instance.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetTimeSeriesToProcess</b></td>
<td>Return the list of time series to process, based on information that indicates how the list can be determined.
Parameters to this request are:
<ol>
<li>	<b>TSList</b> Indicates how the list of time series for processing is to be determined,
		with one of the following values:</li>
	<ol>
	<li>	"AllTS" will result in true being returned.</li>
	<li>	"AllMatchingTSID" will use the TSID value to match time series.</li>
	<li>	"LastMatchingTSID" will use the TSID value to match time series, returning the last match.
		This is necessary for backward compatibility.
		</li>
	<li>	"SelectedTS" will result in true being returned only if the time series is selected.</li>
	</ol>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TSToProcessList</b> The requested time series to process or null if none are available, as a list of TS.
		Use the size of the list (in the first element) to determine the number of time series to process.
		The order of the time series will be from first to last.</li>
<li>	<b>Indices</b> An int[] indicating the positions in the time series list,
		to be used to update the time series.</li>
</ol>
</td>
</tr>

<tr>
<td><b>IndexOf</b></td>
<td>Return the position of a time series from either the __tslist list or the BinaryTS file, as appropriate.
	See the similar method in TSEngine for full documentation.
	This version assumes that no sequence number is used in the TSID.
	The search is performed backwards in order to find the time series from the most recent processed command.
	Parameters to this request are:
<ol>
<li>	<b>TSID</b> The time series identifier or alias being requested, as a String.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>Index</b> The index (0+) of the time series identifier being requested, as an Integer.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ProcessCommands</b></td>
<td>Process a list of commands (recursively), for example when called by the RunCommands() command.
	Parameters to this request are:
<ol>
<li>	<b>Commands</b> A list of commands to run, as a list of String.</li>
<li>	<b>Properties</b> Properties to control the commands, as a PropList.
		Note that all properties should be String, as per the TSEngine properties.
		Valid properties are InitialWorkingDir=PathToWorkingDir, CreateOutput=True|False.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ProcessTimeSeriesAction</b></td>
<td>Process a time series action, meaning insert or update in the list.
	Parameters to this request are:
<ol>
<li>	<b>Action</b> "INSERT" to insert at the position,
		"UPDATE" to update at the position,
		"NONE" to do to nothing, as a String.</li>
<li>	<b>TS</b> The time series to act on, as TS object.</li>
<li>	<b>Index</b> The index (0+) of the time series identifier being processed, as an Integer.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ProcessTimeSeriesResultsList</b></td>
<td>Process the processor's time series results into a graph, file, etc.
	Parameters to this request are:
<ol>
<li>	<b>Indices</b> Indices (0+) indicating the time series to process.</li>
<li>	<b>Properties</b> Properties indicating the type of output.
		These are currently documented internally in TSEngine.
		</ol>
Returned values from this request are:
<ol>
<li>	None - results display or are created as files.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ReadTimeSeries</b></td>
<td>Read a time series given its identifier.
    Parameters to this request are:
<ol>
<li>    <b>TSID</b> Full time series identifier with input type and name (if needed).</li>
<li>    <b>HandleMissingTSHow</b> Indicate how to handle missing time series.
		Use <code>IgnoreMissingTS</code> to ignore the time series (return null) or
		<code>DefaultMissingTS</code> to return an empty time series.</li>
        </ol>
Returned values from this request are:
<ol>
<li>    <b>TS</b>  The time series that was read.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ReadTimeSeries2</b></td>
<td> Process a list of time series after the initial read.
This does NOT add the time series to the list (use setTimeSeries() or other commands to do this).
This method should be called after time series are read.
This method does the following:
<ol>
<li>	Sets the legend to "" (this unsets legend information that was
	previously required with the old graph package).</li>
<li>	If the description is not set, sets it to the location.</li>
<li>	If a missing data range has been set, indicate it to the time series.
	This may be phased out.</li>
<li>	If the time series identifier needs to be reset to something known to the read code,
		reset it (using the non-null tsident_string parameter that is passed in).</li>
<li>	Compute the historic averages for the raw data so that it is available later for filling.</li>
<li>	If the output period is specified, make sure that the time series period includes the output period.
		For important time series, the available period may already include the output period.
		For time series that are being filled,
		it is likely that the available period will need to be extended to include the output period.</li>
</ol>
Parameters to this request are:
<ol>
<li>	<b>TSList</b> List of time series to process, as a Vector of TS.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>RemoveProperty</b></td>
<td>Remove (unset) a processor property.  Parameters to this request are:
<ol>
<li>    <b>PropertyName</b> The property name.</li>
</ol>
Returned values from this request are:
<ol>
<li>    None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>RunCommands</b></td>
<td>Run commands to create the results:
<ol>
<li>	<b>CommandList</b> A Vector of Command instances to run.</li>
<li>	<b>InitialWorkingDir</b> The initial working directory as a String, to initialize paths.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None - time series results will contain the results.</li>
</ol>
</td>
</tr>

<tr>
<td><b>SetHydroBaseDMI</b></td>
<td>Set a HydroBaseDMI instance for use in database queries.
	Parameters to this request are:
<ol>
<li>	<b>HydroBaseDMI</b> An open HydroBaseDMI instance,
		where the input name can be used to uniquely identify the instance.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>SetObject</b></td>
<td>Set an object.  Parameters to this request are:
<ol>
<li>    <b>Object</b> The JSONObject instance to set.</li>
</ol>
Returned values from this request are:
<ol>
<li>    None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>SetPatternTSList</b></td>
<td>Set the list of pattern time series, to be used with FillPattern() commands.
    Parameters to this request are:
<ol>
<li>    <b>TSList</b> A list of pattern time series to save for later use.
        Matching identifiers are overwritten.</li>
</ol>
Returned values from this request are:
<ol>
<li>    None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>SetProperty</b></td>
<td>Set a processor property.  Parameters to this request are:
<ol>
<li>    <b>PropertyName</b> The property name.</li>
<li>    <b>PropertyValue</b> The property value, as an object (e.g., DateTime, Double, Integer, or String.</li>
<li>	<b>SetNull</b> If True, set the property value to null.</li>
</ol>
Returned values from this request are:
<ol>
<li>    None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>SetTable</b></td>
<td>Set a DataTable instance.  Parameters to this request are:
<ol>
<li>    <b>Table</b> A DataTable, where getTableID() is used to get a unique identifier for matching in the list of tables.</li>
</ol>
Returned values from this request are:
<ol>
<li>    None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>TSIDListNoInputAboveCommand</b></td>
<td>The list of time series identifiers that are available (as String), without the input type and name.
The time series identifiers from commands above the selected command are returned.
This property will normally only be used with command editor dialogs.  Parameters for this request are:</td>
<ol>
<li>	<b>Command</b> A Command instance, typically being edited, above which time series identifiers are determined.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TSIDList</b> The list of time series identifiers as a list of String.</li>
</ol>
</td>
</tr>

</table>
@param request_params An optional list of parameters to be used in the request.
@param processorProps processor properties to initialize the processor before running
@exception Exception if the request cannot be processed.
@return the results of a request, or null if a value is not found.
*/
public CommandProcessorRequestResultsBean processRequest ( String request, PropList request_params, PropList processorProps )
throws Exception {
	//return __tsengine.getPropContents ( prop );
    if ( request.equalsIgnoreCase("AddCommandProcessorEventListener") ) {
        return processRequest_AddCommandProcessorEventListener ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("AppendTimeSeries") ) {
		return processRequest_AppendTimeSeries ( request, request_params );
	}
    else if ( request.equalsIgnoreCase("AppendEnsemble") ) {
        return processRequest_AppendEnsemble( request, request_params );
    }
	else if ( request.equalsIgnoreCase("CalculateTSAverageLimits") ) {
		return processRequest_CalculateTSAverageLimits ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("DateTime") ) {
		return processRequest_DateTime ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetEnsemble") ) {
        return processRequest_GetEnsemble ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("GetEnsembleAt") ) {
        return processRequest_GetEnsembleAt ( request, request_params );
    }
	else if ( request.equalsIgnoreCase("GetHydroBaseDMI") ) {
		return processRequest_GetHydroBaseDMI ( request, request_params );
	}
    else if ( request.equalsIgnoreCase("GetNetwork") ) {
        return processRequest_GetNetwork ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("GetNwsrfsDMI") ) {
        return processRequest_GetNwsrfsDMI ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("GetObject") ) {
        return processRequest_GetObject ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("GetOutputPeriodForCommand") ) {
        return processRequest_GetOutputPeriodForCommand ( request, request_params );
    }
    // Put before shorter GetProperty.
    else if ( request.equalsIgnoreCase("GetPropertyHashtable") ) {
        return processRequest_GetPropertyHashtable ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("GetProperty") ) {
        return processRequest_GetProperty ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("GetTable") ) {
        return processRequest_GetTable ( request, request_params );
    }
	else if ( request.equalsIgnoreCase("GetTimeSeries") ) {
		return processRequest_GetTimeSeries ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetTimeSeriesForTSID") ) {
		return processRequest_GetTimeSeriesForTSID ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetTimeSeriesToProcess") ) {
		return processRequest_GetTimeSeriesToProcess ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetTSIDListNoInputAboveCommand") ) {
		return processRequest_GetTSIDListNoInputAboveCommand ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetWorkingDirForCommand") ) {
		return processRequest_GetWorkingDirForCommand ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("IndexOf") ) {
		return processRequest_IndexOf ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("ProcessCommands") ) {
		return processRequest_ProcessCommands ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("ProcessTimeSeriesAction") ) {
		return processRequest_ProcessTimeSeriesAction ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("ProcessTimeSeriesResultsList") ) {
		return processRequest_ProcessTimeSeriesResultsList ( request, request_params );
	}
    else if ( request.equalsIgnoreCase("ReadTimeSeries") ) {
        return processRequest_ReadTimeSeries ( request, request_params );
    }
	else if ( request.equalsIgnoreCase("ReadTimeSeries2") ) {
		return processRequest_ReadTimeSeries2 ( request, request_params );
	}
    else if ( request.equalsIgnoreCase("RemoveAllFromEnsembleResultsList") ) {
        return processRequest_RemoveAllFromEnsembleResultsList ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("RemoveAllFromTimeSeriesResultsList") ) {
        return processRequest_RemoveAllFromTimeSeriesResultsList ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("RemoveObjectFromResultsList") ) {
        return processRequest_RemoveObjectFromResultsList ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("RemoveProperty") ) {
        return processRequest_RemoveProperty ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("RemoveTableFromResultsList") ) {
        return processRequest_RemoveTableFromResultsList ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("RemoveTimeSeriesFromResultsList") ) {
        return processRequest_RemoveTimeSeriesFromResultsList ( request, request_params );
    }
	else if ( request.equalsIgnoreCase("RunCommands") ) {
		// This requires the initial properties.
		return processRequest_RunCommands ( request, request_params, processorProps );
	}
    else if ( request.equalsIgnoreCase("DataStore") ) {
        return processRequest_SetDataStore ( request, request_params );
    }
	else if ( request.equalsIgnoreCase("SetHydroBaseDMI") ) {
		return processRequest_SetHydroBaseDMI ( request, request_params );
	}
    else if ( request.equalsIgnoreCase("SetNetwork") ) {
        return processRequest_SetNetwork ( request, request_params );
    }
	else if ( request.equalsIgnoreCase("SetNWSRFSFS5FilesDMI") ) {
		return processRequest_SetNWSRFSFS5FilesDMI ( request, request_params );
	}
    else if ( request.equalsIgnoreCase("SetObject") ) {
        return processRequest_SetObject ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("SetPatternTSList") ) {
        return processRequest_SetPatternTSList ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("SetProperty") ) {
        return processRequest_SetProperty ( request, request_params );
    }
    else if ( request.equalsIgnoreCase("SetTable") ) {
        return processRequest_SetTable ( request, request_params );
    }
	else if ( request.equalsIgnoreCase("SetTimeSeries") ) {
		return processRequest_SetTimeSeries ( request, request_params );
	}
    else if ( request.equalsIgnoreCase("SetTimeSeriesView") ) {
        return processRequest_SetTimeSeriesView ( request, request_params );
    }
	else {
		TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
		String warning = "Unknown TSCommandProcessor request \"" + request + "\"";
		bean.setWarningText( warning );
		Message.printWarning(3, "TSCommandProcessor.processRequest", warning);
		// TODO SAM 2007-02-07 Need to figure out a way to indicate an error and pass back useful information.
		throw new UnrecognizedRequestException ( warning );
	}
}

/**
Process the AddCommandProcessorEventListener request.
*/
private CommandProcessorRequestResultsBean processRequest_AddCommandProcessorEventListener (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "CommandProcessorEventListener" );
    if ( o == null ) {
            String warning = "Request AddCommandProcessorEventListener() does not " +
            		"provide a CommandProcessorEventListener parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    CommandProcessorEventListener listener = (CommandProcessorEventListener)o;
    addCommandProcessorEventListener ( listener );
    // No data are returned in the bean.
    return bean;
}

/**
Process the AppendEnsemble request.
*/
private CommandProcessorRequestResultsBean processRequest_AppendEnsemble (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "TSEnsemble" );
    if ( o == null ) {
            String warning = "Request AppendEnsemble() does not provide a TSEnsemble parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    TSEnsemble tsensemble = (TSEnsemble)o;
    __TSEnsembleList.add ( tsensemble );
    // No data are returned in the bean.
    return bean;
}

/**
Process the AppendTimeSeries request.
*/
private CommandProcessorRequestResultsBean processRequest_AppendTimeSeries (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "TS" );
	if ( o == null ) {
		String warning = "Request AppendTimeSeries() does not provide a TS parameter.";
		bean.setWarningText ( warning );
		bean.setWarningRecommendationText ( "This is likely a software code error.");
		throw new RequestParameterNotFoundException ( warning );
	}
	TS ts = (TS)o;
	__tsengine.appendTimeSeries ( ts );
	// No data are returned in the bean.
	return bean;
}

/**
Process the CalculateTSAverageLimits request.
*/
private CommandProcessorRequestResultsBean processRequest_CalculateTSAverageLimits (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	// Time series.
	Object o_TS = request_params.getContents ( "TS" );
	if ( o_TS == null ) {
		String warning = "Request CalculateTSAverageLimits() does not provide a TS parameter.";
		bean.setWarningText ( warning );
		bean.setWarningRecommendationText ( "This is likely a software code error.");
		throw new RequestParameterNotFoundException ( warning );
	}
	TS ts = (TS)o_TS;
	TSLimits tslimits = __tsengine.calculateTSAverageLimits(ts);
	// Return the limits.
	PropList results = bean.getResultsPropList();
	results.setUsingObject ( "TSLimits", tslimits );
	return bean;
}

/**
Get a date/time property (DateTime instances) from a string.
The string is first expanded to fill ${Property} strings and then the
matching property name is used to determine the date/time using the following rules:
<ol>
<li> If the string is null, "*" or "", return null.</li>
<li> If the string uses a standard name InputStart (QueryStart), InputEnd (QueryEnd), OutputStart, OutputEnd, return the corresponding DateTime.</li>
<li> Check the processor date/time hash table for user-defined date/time properties.</li>
<li> Parse the string using DateTime.parse().
</ol>
@param request the processor request "DateTime" for logging
@param request_params request parameters:
<ol>
<li> DateTime - date/time string to process into a DateTime object
</ol>
*/
private CommandProcessorRequestResultsBean processRequest_DateTime ( String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the DateTime parameters from the request.
	Object o = request_params.getContents ( "DateTime" );
	if ( o == null ) {
		String warning = "Request DateTime() does not provide a DateTime parameter.";
		bean.setWarningText ( warning );
		bean.setWarningRecommendationText ( "This is likely a software code error.");
		throw new RequestParameterNotFoundException ( warning );
	}
	// Have a "DateTime" parameter in the request:
	// - get the string value
	String DateTime = (String)o;
	// Call the TSEngine class method to expand the DateTime:
	// - handles the variants in the method documentation above
	DateTime dt = __tsengine.getDateTime ( DateTime );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference.
	results.setUsingObject("DateTime", dt );
	return bean;
}

/**
Process the GetEnsemble request.
*/
private CommandProcessorRequestResultsBean processRequest_GetEnsemble (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "EnsembleID" );
    if ( o == null ) {
            String warning = "Request GetEnsemble() does not provide an EnsembleID parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    String EnsembleID = (String)o;
    TSEnsemble tsensemble = getEnsemble ( EnsembleID );
    PropList results = bean.getResultsPropList();
    // This will be set in the bean because the PropList is a reference.
    results.setUsingObject("TSEnsemble", tsensemble );
    return bean;
}

/**
Process the GetEnsembleAt request.
*/
private CommandProcessorRequestResultsBean processRequest_GetEnsembleAt (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "Index" );
    if ( o == null ) {
            String warning = "Request GetEnsembleAt() does not provide an Index parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    Integer Index = (Integer)o;
    int size = __TSEnsembleList.size();
    int i = Index.intValue();
    TSEnsemble tsensemble = null;
    if ( i > (size - 1) ) {
        tsensemble = null;
    }
    else {
        tsensemble = __TSEnsembleList.get ( i );
    }
    PropList results = bean.getResultsPropList();
    // This will be set in the bean because the PropList is a reference.
    results.setUsingObject("TSEnsemble", tsensemble );
    return bean;
}

/**
Process the GetHydroBaseDMI request.
*/
private CommandProcessorRequestResultsBean processRequest_GetHydroBaseDMI (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "InputName" );
	if ( o == null ) {
			String warning = "Request GetHydroBaseDMI() does not provide an InputName parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText ( "This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String InputName = (String)o;
	HydroBaseDMI dmi = __tsengine.getHydroBaseDMI( InputName );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference.
	results.setUsingObject("HydroBaseDMI", dmi );
	return bean;
}

/**
Process the GetNetwork request.
*/
private CommandProcessorRequestResultsBean processRequest_GetNetwork (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "NetworkID" );
    if ( o == null ) {
            String warning = "Request GetNetwork() does not provide a NetworkID parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    String NetworkID = (String)o;
    int size = 0;
    if ( __NodeNetworkList != null ) {
        size = __NodeNetworkList.size();
    }
    NodeNetwork network = null;
    boolean found = false;
    for ( int i = 0; i < size; i++ ) {
        network = (NodeNetwork)__NodeNetworkList.get(i);
        if ( network.getNetworkId().equalsIgnoreCase(NetworkID) ) {
            found = true;
            break;
        }
    }
    if ( !found ) {
        network = null;
    }
    PropList results = bean.getResultsPropList();
    // This will be set in the bean because the PropList is a reference.
    results.setUsingObject("Network", network );
    return bean;
}

/**
Process the GetNwsrfsDMI request.
*/
private CommandProcessorRequestResultsBean processRequest_GetNwsrfsDMI (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "InputName" );
    if ( o == null ) {
        String warning = "Request GetNwsrfsDMI() does not provide an InputName parameter.";
        bean.setWarningText ( warning );
        bean.setWarningRecommendationText ( "This is likely a software code error.");
        throw new RequestParameterNotFoundException ( warning );
    }
    String InputName = (String)o;
    NWSRFS_DMI dmi = __tsengine.getNWSRFSFS5FilesDMI ( InputName, true );
    PropList results = bean.getResultsPropList();
    // This will be set in the bean because the PropList is a reference.
    results.setUsingObject("NwsrfsDMI", dmi );
    return bean;
}

/**
Process the GetObject request.
*/
private CommandProcessorRequestResultsBean processRequest_GetObject (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "ObjectID" );
    if ( o == null ) {
            String warning = "Request GetObject() does not provide a ObjectID parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    String ObjectID = (String)o;
    int size = 0;
    if ( this.__jsonObjectList != null ) {
        size = this.__jsonObjectList.size();
    }
    JSONObject object = null;
    boolean found = false;
    for ( int i = 0; i < size; i++ ) {
        object = (JSONObject)this.__jsonObjectList.get(i);
        if ( object.getObjectID().equalsIgnoreCase(ObjectID) ) {
            found = true;
            break;
        }
    }
    if ( !found ) {
        object = null;
    }
    PropList results = bean.getResultsPropList();
    // This will be set in the bean because the PropList is a reference.
    results.setUsingObject("Object", object );
    return bean;
}

/**
Process the GetOutputPeriodForCommand request.
*/
private CommandProcessorRequestResultsBean processRequest_GetOutputPeriodForCommand (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "Command" );
    if ( o == null ) {
        String warning = "Request GetOutputPeriodForCommand() does not provide a Command parameter.";
        bean.setWarningText ( warning );
        bean.setWarningRecommendationText ( "This is likely a software code error.");
        throw new RequestParameterNotFoundException ( warning );
    }
    Command command = (Command)o;
    // Get the index of the requested command.
    int index = indexOf ( command );
    // Get the setWorkingDir() commands.
    List<String> neededCommandsStringList = new Vector<String>();
    neededCommandsStringList.add ( "SetOutputPeriod" );
    List<Command> setOutputPeriodCommandList = TSCommandProcessorUtil.getCommandsBeforeIndex (
        index,
        this,
        neededCommandsStringList,
        false );    // Get all, not just last.
    // Create a local command processor:
    // - pass along the initial properties
    TSCommandProcessor tsProcessor = new TSCommandProcessor( this.initialProps);
    // Add all the commands (currently no method to add all because this is normally not done).
    for ( Command setOutputPeriodCommand : setOutputPeriodCommandList ) {
        tsProcessor.addCommand ( setOutputPeriodCommand );
    }
    // Run the commands to set the working directory in the temporary processor.
    try {
        tsProcessor.runCommands(
            null, // Process all commands in this processor.
            null ); // No need for controlling properties since controlled by commands.
    }
    catch ( Exception e ) {
        // This is a software problem.
        String routine = getClass().getSimpleName() + ".processRequest_GetOutputPeriodForCommand";
        Message.printWarning(2, routine, "Error getting output period for command (" + e + ")." );
        Message.printWarning(3, routine, e);
    }
    // Return the output period as DateTime instances.  This can then be used in editors, for example.
    PropList results = bean.getResultsPropList();
    results.setUsingObject( "OutputStart", (DateTime)tsProcessor.getPropContents ( "OutputStart") );
    results.setUsingObject( "OutputEnd", (DateTime)tsProcessor.getPropContents ( "OutputEnd") );
    return bean;
}

/**
Process the GetProperty request.
User-specified properties are checked first and if not found the built-in properties are requested.
*/
private CommandProcessorRequestResultsBean processRequest_GetProperty (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "PropertyName" );
    if ( o == null ) {
        String warning = "Request GetProperty() does not provide a PropertyName parameter.";
        bean.setWarningText ( warning );
        bean.setWarningRecommendationText ( "This is likely a software code error.");
        throw new RequestParameterNotFoundException ( warning );
    }
    String PropertyName = (String)o;
    Object PropertyValue = __propertyHashmap.get ( PropertyName );
    if ( PropertyValue == null ) {
        // Try the built-in properties.
        PropertyValue = getPropContents(PropertyName);
    }
    // Return the property value in the bean.
    PropList results = bean.getResultsPropList();
    // This will be set in the bean because the PropList is a reference.
    results.setUsingObject("PropertyValue", PropertyValue );
    return bean;
}

/**
Process the GetPropertyHashtable request.
Currently only user-specified properties are returned and only if the request parameter "UserProperties=True".
@return Hashtable of properties, not in sorted order.
This is a new Hashtable instance whose contents generally should not be modified.
*/
private CommandProcessorRequestResultsBean processRequest_GetPropertyHashtable (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// New Hashtable to return.
	Hashtable<String,Object> ph = new Hashtable<>();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "GetUserProperties" );
	if ( o != null ) {
		String propval = (String)o;
		if ( (propval != null) && propval.equalsIgnoreCase("true") ) {
			// Transfer the user-specified properties.
			Set<String> keys = __propertyHashmap.keySet();
			for ( String key : keys ) {
				o = __propertyHashmap.get ( key );
				ph.put(key,o);
			}
		}
	}
	// TODO SAM 2015-04-26 Transfer the internal properties.
    // Return the property value in the bean.
    PropList results = bean.getResultsPropList();
    // This will be set in the bean because the PropList is a reference.
    results.setUsingObject("PropertyHashtable", ph );
    return bean;
}

/**
Process the GetTable request.
*/
private CommandProcessorRequestResultsBean processRequest_GetTable ( String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "TableID" );
    if ( o == null ) {
            String warning = "Request GetTable() does not provide a TableID parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    String TableID = (String)o;
    int size = 0;
    if ( __TableList != null ) {
        size = __TableList.size();
    }
    DataTable table = null;
    boolean found = false;
    for ( int i = 0; i < size; i++ ) {
        table = (DataTable)__TableList.get(i);
        if ( table.getTableID().equalsIgnoreCase(TableID) ) {
            found = true;
            break;
        }
    }
    if ( !found ) {
        table = null;
    }
    PropList results = bean.getResultsPropList();
    // This will be set in the bean because the PropList is a reference.
    results.setUsingObject("Table", table );
    return bean;
}

/**
Process the GetTimeSeries request.
*/
private CommandProcessorRequestResultsBean processRequest_GetTimeSeries ( String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "Index" );
	if ( o == null ) {
			String warning = "Request GetTimeSeries() does not provide an Index parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Integer Index = (Integer)o;
	TS ts = __tsengine.getTimeSeries ( Index.intValue() );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference.
	results.setUsingObject("TS", ts );
	return bean;
}

/**
Process the GetTimeSeriesForTSID request.
The time series is located by processing backwards in the list.
@param request the 'processRequest' request that was called (for messaging)
@param request_paramts request parameters:  "TSID" as the TSID or alias and "CommandTag" for the command tag (position)
*/
private CommandProcessorRequestResultsBean processRequest_GetTimeSeriesForTSID (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "TSID" );
	if ( o == null ) {
			String warning = "Request GetTimeSeriesForTSID() does not provide a TSID parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (	"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String TSID = (String)o;
	o = request_params.getContents ( "CommandTag" );
	String CommandTag = "";
	if ( o != null ) {
		CommandTag = (String)o;
	}
	TS ts = __tsengine.getTimeSeries ( CommandTag, TSID );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference.
	results.setUsingObject("TS", ts );
	return bean;
}

/**
Process the GetTimeSeriesToProcess request.
*/
private CommandProcessorRequestResultsBean processRequest_GetTimeSeriesToProcess (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "TSList" );
	if ( o == null ) {
		String warning = "Request GetTimeSeriesToProcess() does not provide a TSList parameter.";
		bean.setWarningText ( warning );
		bean.setWarningRecommendationText (	"This is likely a software code error.");
		throw new RequestParameterNotFoundException ( warning );
	}
	// Else continue.
	String TSList = (String)o;
	// The following can be null.  Let the called code handle it.
	Object o_TSID = request_params.getContents ( "TSID" );
	String TSID = null;
	if ( o_TSID != null ) {
		TSID = (String)o_TSID;
	}
    // The following can be null.  Let the called code handle it.
    Object o_EnsembleID = request_params.getContents ( "EnsembleID" );
    String EnsembleID = null;
    if ( o_EnsembleID != null ) {
        EnsembleID = (String)o_EnsembleID;
    }
    // The following can be null.  Let the called code handle it.
    Object o_TSPosition = request_params.getContents ( "TSPosition" );
    String TSPosition = null;
    if ( o_TSPosition != null ) {
        TSPosition = (String)o_TSPosition;
    }
	// Get the information from TSEngine, which is returned as a list
	// with the first element being the matching time series list and the second
	// being the indices of those time series in the time series results list.
    TimeSeriesToProcess tsToProcess = __tsengine.getTimeSeriesToProcess ( TSList, TSID, EnsembleID, TSPosition );
	List<TS> tsList = tsToProcess.getTimeSeriesList();
	int [] tsPos = tsToProcess.getTimeSeriesPositions();
	List<String> errorList = tsToProcess.getErrors();
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference.
	//Message.printStatus(2,"From TSEngine",((Vector)(tslist.elementAt(0))).toString() );
	results.setUsingObject("TSToProcessList", tsList );
	results.setUsingObject("Indices", tsPos );
	results.setUsingObject("Errors", errorList );
	return bean;
}

/**
Process the GetTimeSeriesForTSID request.
*/
private CommandProcessorRequestResultsBean processRequest_GetTSIDListNoInputAboveCommand (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "Command" );
	if ( o == null ) {
			String warning = "Request GetTSIDListNoInputAboveCommand() does not provide a TSID parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Command command = (Command)o;
	List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand( this, command );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference.
	results.setUsingObject("TSIDList", tsids );
	return bean;
}

/**
Process the GetWorkingDirForCommand request.
Do not set any internal "Posix" variations - retrieve those as needed by reformatting the other versions.
*/
private CommandProcessorRequestResultsBean processRequest_GetWorkingDirForCommand (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "Command" );
	if ( o == null ) {
			String warning = "Request GetWorkingDirForCommand() does not provide a Command parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText ( "This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Command command = (Command)o;
	// Get the index of the requested command.
	int index = indexOf ( command );
	// Get the setWorkingDir() commands.
	List<String> needed_commands_String_List = new ArrayList<>();
	needed_commands_String_List.add ( "SetWorkingDir" );
	List<Command> setWorkingDir_CommandList = TSCommandProcessorUtil.getCommandsBeforeIndex (
			index,
			this,
			needed_commands_String_List,
			false ); // Get all, not just last.
	// Always add the starting working directory to the top to make sure an initial condition is set.
	setWorkingDir_CommandList.add ( 0, newInitialSetWorkingDirCommand() );
	// Create a local command processor:
    // - pass along the initial properties
	TSCommandProcessor ts_processor = new TSCommandProcessor(this.initialProps);
	Object workingDir = getPropContents("InitialWorkingDir");
	ts_processor.setPropContents("InitialWorkingDir", workingDir);
	int size = setWorkingDir_CommandList.size();
	// Add all the commands (currently no method to add all because this is normally not done).
	for ( int i = 0; i < size; i++ ) {
		ts_processor.addCommand ( (Command)setWorkingDir_CommandList.get(i));
	}
	// Run the commands to set the working directory in the temporary processor.
	try {
	    ts_processor.runCommands(
			null,	// Process all commands in this processor.
			null);	// No need for controlling properties since controlled by commands.
	}
	catch ( Exception e ) {
		// This is a software problem.
		String routine = getClass().getSimpleName() + ".processRequest_GetWorkingDirForCommand";
		Message.printWarning(2, routine, "Error getting working directory for command." );
		Message.printWarning(2, routine, e);
	}
	// Return the working directory as a String:
	// - this can then be used in editors, for example
	// - the WorkingDir property will have been set in the temporary processor
	// - also set WorkingDirPosix and WorkingDirPortable and phase in POSIX paths
	PropList results = bean.getResultsPropList();
	results.set( "WorkingDir", (String)ts_processor.getPropContents ( "WorkingDir") );
	results.set( "WorkingDirPortable", IOUtil.toPortablePath((String)ts_processor.getPropContents ( "WorkingDir")) );
	results.set( "WorkingDirPosix", IOUtil.toPosixPath((String)ts_processor.getPropContents ( "WorkingDir")) );
	return bean;
}

/**
Process the IndexOf request.
*/
private CommandProcessorRequestResultsBean processRequest_IndexOf (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "TSID" );
	if ( o == null ) {
			String warning = "Request IndexOf() does not provide a TSID parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText ( "This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String TSID = (String)o;
	int index = __tsengine.indexOf ( TSID );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference.
	results.setUsingObject("Index", new Integer(index));
	return bean;
}

/**
Process the ProcessTimeSeriesAction request.
*/
private CommandProcessorRequestResultsBean processRequest_ProcessCommands (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	// Time series.
	Object o_Commands = request_params.getContents ( "Commands" );
	if ( o_Commands == null ) {
			String warning = "Request ProcessCommands() does not provide a Commands parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText ( "This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	@SuppressWarnings("unchecked")
	List<Command> commands = (List<Command>)o_Commands;
	Object o_Properties = request_params.getContents ( "Properties" );
	if ( o_Properties == null ) {
			String warning = "Request ProcessCommands() does not provide a Properties parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText ( "This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	__tsengine.processCommands( commands, (PropList)o_Properties );
	// No results need to be set in the bean.
	return bean;
}

/**
Process the ProcessTimeSeriesAction request.
*/
private CommandProcessorRequestResultsBean processRequest_ProcessTimeSeriesAction ( String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean =	new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	// Time series.
	Object o_TS = request_params.getContents ( "TS" );
	if ( o_TS == null ) {
			String warning = "Request ProcessTimeSeriesAction() does not provide a TS parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText ( "This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	TS ts = (TS)o_TS;
	// Action.
	Object o_Action = request_params.getContents ( "Action" );
	if ( o_Action == null ) {
			String warning = "Request ProcessTimeSeriesAction() does not provide an Action parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText ( "This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String Action = (String)o_Action;
	// TODO SAM 2007-02-11 Need to handle actions as strings cleaner.
	TSCommandProcessorActionType action = TSCommandProcessorActionType.valueOfIgnoreCase(Action);
	if ( action == null ) {
	    String warning = "Request ProcessTimeSeriesAction() Action value \"" + Action + "\" is invalid.";
		throw new RequestParameterInvalidException ( warning );
	}

	// Index.
	Object o_Index = request_params.getContents ( "Index" );
	if ( o_Index == null ) {
			String warning = "Request ProcessTimeSeriesAction() does not provide an Index parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText ( "This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Integer Index = (Integer)o_Index;
	int ts_pos = Index.intValue();
    if ( action == TSCommandProcessorActionType.INSERT_TS ) {
        // Add new time series to the list.
        __tsengine.setTimeSeries ( ts, ts_pos );
    }
    else if ( action == TSCommandProcessorActionType.UPDATE_TS ) {
        // Update in the time series list.
        __tsengine.setTimeSeries ( ts, ts_pos );
    }
	// No results need to be set in the bean.
	return bean;
}

/**
Process the ProcessTimeSeriesResultsList request.
This processes the time series into a product such as a graph or report.
@param request will be the original request "ProcessTimeSeriesResultsList", used in error-handling
@param request_params request parameters, as follows:
<ol>
<li>Indices - an int[] containing the processor time series list indices to process</li>
<li>TSList - a List<TS> containing the time series list to process
<li>Properties - a PropList containing properties to control output processing, passed to TSEngine.processTimeSeries()</li>
</ol>
*/
@SuppressWarnings("unchecked")
private CommandProcessorRequestResultsBean processRequest_ProcessTimeSeriesResultsList (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean =	new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	// Indices for time series.
	Object o_Indices = request_params.getContents ( "Indices" );
	int [] Indices_array = null;
	if ( o_Indices != null ) {
		Indices_array = (int[])o_Indices;
	}
	Object o_TSList = request_params.getContents ( "TSList" );
	List<TS> tslist = null;
	if ( (o_TSList != null) ) {
		tslist = (List<TS>)o_TSList;
	}
	// Properties for processing, as per legacy code.
	Object o_Properties = request_params.getContents ( "Properties" );
	PropList Properties = null;
	if ( o_Properties == null ) {
		/* OK for now - use defaults in called code.
		String warning = "Request ProcessTimeSeriesResultsList() does not provide a Properties parameter.";
		bean.setWarningText ( warning );
		bean.setWarningRecommendationText ("This is likely a software code error.");
		throw new RequestParameterNotFoundException ( warning );
		*/
	}
	else {
		Properties = (PropList)o_Properties;
	}
	// Take action based on what was passed in.
	if ( (tslist != null) && (tslist.size() > 0) ) {
		// Have a list of time series to process.
		__tsengine.processTimeSeries( tslist, Properties );
	}
	else {
		// Treat as if indices were passed (legacy approach before TSList option was added).
		// OK if o_Indices is null because it means to process all time series in the results list.
		__tsengine.processTimeSeries( Indices_array, Properties );
	}
	// No results need to be set in the bean.
	return bean;
}

/**
Process the ReadTimeSeries request.
This request is used with the CreateFromList() and ReadTimeSeries() commands.
Because this method performs fundamental tasks, some of the error handling is different than other requests,
in particular passing in the WarningLevel and CommandTag request parameters.
This reads the time series and post-processes.
To only post-process time series, use the "ReadTimeSeries2" request.
The following properties are expected:
<ol>
<li> CommandTag - string to tag warnings for logging</li>
<li> DefaultOutputStart - DateTime with output start date if IfNotFound=Default</li>
<li> DefaultOutputEnd - DateTime with output date if IfNotFound=Default</li>
<li> IfNotFound - Default, Ignore, or Warn (default)</li>
<li> ReadData - true (default) to read data, false to only initialize time series</li>
<li> TSID - time series identifier to read</li>
<li> WarningLevel - warning level for warning messages</li>
</ol>
@return a result bean with the following property contents:
<ol>
<li> TS - time series object</li>
</ol>
*/
private CommandProcessorRequestResultsBean processRequest_ReadTimeSeries ( String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    // Identifier for time series to read.
    Object o = request_params.getContents ( "TSID" );
    if ( o == null ) {
            String warning = "Request ReadTimeSeries() does not provide a TSID parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    String TSID = "";
    if ( o instanceof TSIdent ) {
        TSID = ((TSIdent)o).toString(true);
    }
    else {
        TSID = (String)o;
    }
    // Warning level to use for command logging.
    Object o_WarningLevel = request_params.getContents ( "WarningLevel" );
    if ( o_WarningLevel == null ) {
            String warning = "Request ReadTimeSeries() does not provide a WarningLevel parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    int warningLevel = 2;
    Integer WarningLevel = (Integer)o_WarningLevel;
    warningLevel = WarningLevel.intValue();
    // Command tag to use for command logging.
    Object o_CommandTag = request_params.getValue ( "CommandTag" );
    if ( o_CommandTag == null ) {
            String warning = "Request ReadTimeSeries() does not provide a CommandTag parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    String commandTag = (String)o_CommandTag;
    // IfMissing parameter to indicate how to handle missing time series.
    // The Warn value will generate warnings in the main command.
    //
    // For the low level code, Warn and Ignore result in no default time series (includeMissingTS=false in low level code).
    // Default results in default time series being added (includeMissingTS=true in low level code).
    Object o_IfNotFound = request_params.getValue ( "IfNotFound" );
    if ( o_IfNotFound == null ) {
            o_IfNotFound = "Warn";  // Default.
    }
    String IfNotFound = (String)o_IfNotFound;
    boolean includeMissingTS = false; // Default.
    Object o_DefaultOutputStart = request_params.getContents ( "DefaultOutputStart" );
    Object o_DefaultOutputEnd = request_params.getContents ( "DefaultOutputEnd" );
    DateTime defaultOutputStart = null;
    DateTime defaultOutputEnd = null;
    if ( o_DefaultOutputStart != null ) {
    	defaultOutputStart = (DateTime)o_DefaultOutputStart;
    }
    if ( o_DefaultOutputEnd != null ) {
    	defaultOutputEnd = (DateTime)o_DefaultOutputEnd;
    }
    if ( IfNotFound.equalsIgnoreCase("Default") ) {
        includeMissingTS = true;
    }
    Object o_ReadData = request_params.getContents ( "ReadData" );
    if ( o_ReadData == null ) {
           o_ReadData = new Boolean(true); // Default.
    }
    boolean readData = ((Boolean)o_ReadData).booleanValue();
    // If values have been specified for default time series,
    // save the current IgnoreMissingTS global flag, set to the value for this command, and then reset to the global value.
    TS ts = null;
    boolean includeMissingTsOld = __tsengine.getIncludeMissingTS();
    DateTime outputStartOld = __tsengine.getIncludeMissingTSOutputStart();
    DateTime outputEndOld = __tsengine.getIncludeMissingTSOutputEnd();
    try {
        __tsengine.setIncludeMissingTS ( includeMissingTS );
        if ( defaultOutputStart != null ) {
        	__tsengine.setIncludeMissingTSOutputStart(defaultOutputStart);
        }
        if ( defaultOutputEnd != null ) {
        	__tsengine.setIncludeMissingTSOutputEnd(defaultOutputEnd);
        }
        ts = __tsengine.readTimeSeries ( warningLevel, commandTag, TSID, readData );
    }
    finally {
    	// Reset the global property to include missing time series.
        __tsengine.setIncludeMissingTS ( includeMissingTsOld );
        if ( defaultOutputStart != null ) {
        	__tsengine.setIncludeMissingTSOutputStart(outputStartOld);
        }
        if ( defaultOutputEnd != null ) {
        	__tsengine.setIncludeMissingTSOutputEnd(outputEndOld);
        }
    }
    PropList results = bean.getResultsPropList();
    // Return the time series.
    results.setUsingObject ( "TS", ts );
    return bean;
}

/**
Process the ReadTimeSeries2 request.
*/
private CommandProcessorRequestResultsBean processRequest_ReadTimeSeries2 (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "TSList" );
	if ( o == null ) {
		String warning = "Request ReadTimeSeries2() does not provide a TSList parameter.";
		bean.setWarningText ( warning );
		bean.setWarningRecommendationText ( "This is likely a software code error.");
		throw new RequestParameterNotFoundException ( warning );
	}
	@SuppressWarnings("unchecked")
	List<TS> TSList = (List<TS>)o;
	__tsengine.readTimeSeries2 ( TSList );
	//PropList results = bean.getResultsPropList();
	// No data are returned in the bean.
	return bean;
}

/**
Process the RemoveAllFromEnsembleResultsList request.
*/
private CommandProcessorRequestResultsBean processRequest_RemoveAllFromEnsembleResultsList (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    removeAllEnsembles ();
    //PropList results = bean.getResultsPropList();
    // No data are returned in the bean.
    return bean;
}

/**
Process the RemoveAllFromTimeSeriesResultsList request.
*/
private CommandProcessorRequestResultsBean processRequest_RemoveAllFromTimeSeriesResultsList (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    __tsengine.removeAllTimeSeries ();
    //PropList results = bean.getResultsPropList();
    // No data are returned in the bean.
    return bean;
}

/**
Process the RemoveObjectFromResultsList request.
*/
private CommandProcessorRequestResultsBean processRequest_RemoveObjectFromResultsList (
    String request, PropList request_params )
throws Exception {
    //String routine = "TSCommandProcessor.processRequest_RemoveObjectFromResultsList";
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "ObjectID" );
    if ( o == null ) {
        String warning = "Request RemoveObjectFromResultsList() does not provide an ObjectID parameter.";
        bean.setWarningText ( warning );
        bean.setWarningRecommendationText ( "This is likely a software code error.");
        throw new RequestParameterNotFoundException ( warning );
    }
    String ObjectID = (String)o;
    // Remove all objects having the same identifier.
    JSONObject object;
    for ( int i = 0; i < this.__jsonObjectList.size(); i++ ) {
        object = this.__jsonObjectList.get(i);
        // Remove and decrement the counter so that the next table is checked.
        if ( object.getObjectID().equalsIgnoreCase(ObjectID) ) {
            this.__jsonObjectList.remove(i--);
        }
    }
    return bean;
}

/**
Process the SetProperty request.  Null property values are NOT allowed.
*/
private CommandProcessorRequestResultsBean processRequest_RemoveProperty (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "PropertyName" );
    if ( o == null ) {
            String warning = "Request SetProperty() does not provide a PropertyName parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    String PropertyName = (String)o;
    // Do not allow removing official property like InputStart as this would likely cause problems.
    // First see if it is a known user-defined property.
    Object o2 = __propertyHashmap.get ( PropertyName );
    if ( o2 != null ) {
    	// Found it so remove (for some reason can't pass in o2 and have it work).
    	__propertyHashmap.remove(PropertyName);
    }
    // No data are returned in the bean.
    return bean;
}

/**
Process the RemoveTableFromResultsList request.
*/
private CommandProcessorRequestResultsBean processRequest_RemoveTableFromResultsList (
    String request, PropList request_params )
throws Exception {
    //String routine = "TSCommandProcessor.processRequest_RemoveTableFromResultsList";
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "TableID" );
    if ( o == null ) {
        String warning = "Request RemoveTableFromResultsList() does not provide a TableID parameter.";
        bean.setWarningText ( warning );
        bean.setWarningRecommendationText ( "This is likely a software code error.");
        throw new RequestParameterNotFoundException ( warning );
    }
    String TableID = (String)o;
    // Remove all tables having the same identifier.
    DataTable table;
    for ( int i = 0; i < __TableList.size(); i++ ) {
        table = __TableList.get(i);
        // Remove and decrement the counter so that the next table is checked.
        if ( table.getTableID().equalsIgnoreCase(TableID) ) {
            __TableList.remove(i--);
        }
    }
    return bean;
}

/**
Process the RemoveTimeSeriesFromResultsList request.
*/
private CommandProcessorRequestResultsBean processRequest_RemoveTimeSeriesFromResultsList (
        String request, PropList request_params )
throws Exception {
    String routine = getClass().getSimpleName() + ".processRequest_RemoveTimeSeriesFromResultsList";
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "Index" );
    if ( o == null ) {
            String warning = "Request RemoveTimeSeriesFromResultsList() does not provide an Index parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    Integer Index = (Integer)o;
    o = request_params.getContents ( "FreeEnsembleIfEmpty" );
    Boolean FreeEnsembleIfEmpty_Boolean = null;
    if ( o != null ) {
        FreeEnsembleIfEmpty_Boolean = (Boolean)o;
    }
    // Get the time series that will be removed.
    TS ts = __tsengine.getTimeSeries(Index.intValue());
    // Remove the time series.
    __tsengine.removeTimeSeries ( Index.intValue() );
    // Remove the time series from ensembles and remove ensembles if empty.
    // Time series are only removed if the time series reference in the ensemble matches the removed time series
    // (identifiers are not checked).
    for ( int i = 0; i < __TSEnsembleList.size(); i++ ) {
        TSEnsemble ensemble = __TSEnsembleList.get(i);
        if ( ensemble.remove ( ts ) ) {
            // Time series was in the ensemble.
            // Also remove empty ensembles.
            if ( FreeEnsembleIfEmpty_Boolean.booleanValue() && (ensemble.size() == 0) ) {
                Message.printStatus(2, routine, "Ensemble is empty, removing ensemble." );
                __TSEnsembleList.remove(i);
            }
        }
    }
    return bean;
}

/**
Process the RunCommands request.
*/
private CommandProcessorRequestResultsBean processRequest_RunCommands (
		String request, PropList request_params, PropList initialProps )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	// Command list.
	Object o = request_params.getContents ( "CommandList" );
	if ( o == null ) {
			String warning = "Request RunCommands() does not provide a CommandList parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText ( "This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	@SuppressWarnings("unchecked")
	List<Command> commands = (List<Command>)o;
	// Whether commands should create output.
	Object o3 = request_params.getContents ( "CreateOutput" );
	if ( o3 == null ) {
			String warning = "Request RunCommands() does not provide a CreateOutput parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText ( "This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Boolean CreateOutput_Boolean = (Boolean)o3;
	// Set properties as per the legacy application.
	PropList props = new PropList ( "TSEngine");
	props.set ( "CreateOutput", "" + CreateOutput_Boolean );
	// TODO SAM 2007-08-22 Need to evaluate recursion for complex workflow and testing.
	// Call the TSEngine method.
	runCommands ( commands, props );
	// No results need to be returned.
	return bean;
}

/**
Process the SetDataStore request.
*/
private CommandProcessorRequestResultsBean processRequest_SetDataStore (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "DataStore" );
    if ( o == null ) {
        String warning = "Request SetDataStore() does not provide a DataStore parameter.";
        bean.setWarningText ( warning );
        bean.setWarningRecommendationText ( "This is likely a software code error.");
        throw new RequestParameterNotFoundException ( warning );
    }
    DataStore dataStore = (DataStore)o;
    // Add an open DataStore instance, closing and discarding a previous data store of the same id if it exists.
    //
    __tsengine.setDataStore( dataStore, true );
    // No results need to be returned.
    return bean;
}

/**
Process the SetHydroBaseDMI request.
*/
private CommandProcessorRequestResultsBean processRequest_SetHydroBaseDMI (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "HydroBaseDMI" );
	if ( o == null ) {
			String warning = "Request SetHydroBaseDMI() does not provide a HydroBaseDMI parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (	"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	HydroBaseDMI dmi = (HydroBaseDMI)o;
	// Add an open HydroBaseDMI instance, closing a previous connection of the same name if it exists.
	__tsengine.setHydroBaseDMI( dmi, true );
	// No results need to be returned.
	return bean;
}

/**
Process the SetNetwork request.
*/
private CommandProcessorRequestResultsBean processRequest_SetNetwork (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "Network" );
    if ( o == null ) {
            String warning = "Request SetNetwork() does not provide a Network parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ("This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    NodeNetwork o_Network = (NodeNetwork)o;
    // Loop through the networks in memory.  If a matching network ID is found, reset.  Otherwise, add at the end.
    int size = __NodeNetworkList.size();
    NodeNetwork nodeNetwork;
    boolean found = false;
    for ( int i = 0; i < size; i++ ) {
        nodeNetwork = (NodeNetwork)__NodeNetworkList.get(i);
        if ( nodeNetwork.getNetworkId().equalsIgnoreCase(o_Network.getNetworkId())) {
        	__NodeNetworkList.set(i,o_Network);
            found = true;
        }
    }
    if ( !found ) {
    	__NodeNetworkList.add ( o_Network );
    }
    // No data are returned in the bean.
    return bean;
}

/**
Process the SetNWSRFSFS5FilesDMI request.
*/
private CommandProcessorRequestResultsBean processRequest_SetNWSRFSFS5FilesDMI (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "NWSRFSFS5FilesDMI" );
	if ( o == null ) {
			String warning = "Request SetNWSRFSFS5FilesDMI() does not provide a NWSRFSFS5FilesDMI parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (	"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	NWSRFS_DMI dmi = (NWSRFS_DMI)o;
	// Add an open NWSRFS_DMI instance, closing a previous connection of the same name if it exists.
	__tsengine.setNWSRFSFS5FilesDMI( dmi, true );
	// No results need to be returned.
	return bean;
}

/**
Process the SetObject request.
*/
private CommandProcessorRequestResultsBean processRequest_SetObject (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "Object" );
    if ( o == null ) {
            String warning = "Request SetObject() does not provide an Object parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ("This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    JSONObject o_JSONObject = (JSONObject)o;
    // Loop through the objects.  If a matching object ID is found, reset.  Otherwise, add at the end.
    int size = this.__jsonObjectList.size();
    JSONObject object;
    boolean found = false;
    for ( int i = 0; i < size; i++ ) {
        object = (JSONObject)this.__jsonObjectList.get(i);
        if ( object.getObjectID().equalsIgnoreCase(o_JSONObject.getObjectID())) {
            this.__jsonObjectList.set(i,o_JSONObject);
            found = true;
        }
    }
    if ( !found ) {
        this.__jsonObjectList.add ( o_JSONObject );
    }
    // No data are returned in the bean.
    return bean;
}

/**
Process the SetPatternTSList request.
*/
private CommandProcessorRequestResultsBean processRequest_SetPatternTSList (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "TSList" );
    if ( o == null ) {
            String warning = "Request SetPatternTSList() does not provide a TSList parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    @SuppressWarnings("unchecked")
	List<StringMonthTS> tslist = (List<StringMonthTS>)o;
    int size = tslist.size();
    StringMonthTS tsi, tsj;
    for ( int i = 0; i < size; i++ ) {
        // See if the item is already in the list.  If so, replace it.  If not, add at the end.
        boolean found = false;
        tsi = tslist.get(i);
        for ( int j = 0; j < __patternTSList.size(); j++ ) {
            tsj = tslist.get(j);
            if ( tsi.getIdentifier().toString().equalsIgnoreCase(tsj.getIdentifier().toString()) ) {
                __patternTSList.set(j, tsi);
                found = true;
                break;
            }
        }
        if ( !found ) {
            __patternTSList.add ( tsi );
        }
    }
    // No data are returned in the bean.
    return bean;
}

/**
Process the SetProperty request.
Nulls are allowed, but typically only with a special request.
Otherwise, it is difficult to check input for errors.
*/
private CommandProcessorRequestResultsBean processRequest_SetProperty (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "PropertyName" );
    if ( o == null ) {
            String warning = "Request SetProperty() does not provide a PropertyName parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ( "This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    String PropertyName = (String)o;
    Object o2 = request_params.getContents ( "PropertyValue" );
    if ( o2 == null ) {
    	Object o3 = request_params.getValue ( "SetNull" );
    	if ( (o3 != null) && o3.toString().equalsIgnoreCase("true") ) {
	        String warning = "Request SetProperty() does not provide a PropertyValue parameter.";
	        bean.setWarningText ( warning );
	        bean.setWarningRecommendationText ( "This is likely a software code error.");
	        throw new RequestParameterNotFoundException ( warning );
    	}
    	// Else OK to set a null property.
    }
    // Try to set official property.
    Collection<String> internalProperties = getPropertyNameList(true,false);
    if ( internalProperties.contains(PropertyName) ) {
	    try {
	    	// Null is OK here for o2.
	        setPropContents(PropertyName, o2);
	    }
	    catch ( UnrecognizedRequestException e ) {
	        // Not recognized as a core internal so will set below as a user property.
	    }
    }
    else {
	    // Otherwise it is a user-defined property.
	    __propertyHashmap.put ( PropertyName, o2 );
    }
    // No data are returned in the bean.
    return bean;
}

/**
Process the SetTable request.
*/
private CommandProcessorRequestResultsBean processRequest_SetTable (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "Table" );
    if ( o == null ) {
            String warning = "Request SetTable() does not provide a Table parameter.";
            bean.setWarningText ( warning );
            bean.setWarningRecommendationText ("This is likely a software code error.");
            throw new RequestParameterNotFoundException ( warning );
    }
    DataTable o_DataTable = (DataTable)o;
    // Loop through the tables.  If a matching table ID is found, reset.  Otherwise, add at the end.
    int size = __TableList.size();
    DataTable table;
    boolean found = false;
    for ( int i = 0; i < size; i++ ) {
        table = (DataTable)__TableList.get(i);
        if ( table.getTableID().equalsIgnoreCase(o_DataTable.getTableID())) {
            __TableList.set(i,o_DataTable);
            found = true;
        }
    }
    if ( !found ) {
        __TableList.add ( o_DataTable );
    }
    // No data are returned in the bean.
    return bean;
}

/**
Process the SetTimeSeries request.
*/
private CommandProcessorRequestResultsBean processRequest_SetTimeSeries (
		String request, PropList request_params )
throws Exception {
	TSCommandProcessorRequestResultsBean bean =	new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters.
	Object o = request_params.getContents ( "TS" );
	if ( o == null ) {
		String warning = "Request SetTimeSeries() does not provide a TS parameter.";
		bean.setWarningText ( warning );
		bean.setWarningRecommendationText ( "This is likely a software code error.");
		throw new RequestParameterNotFoundException ( warning );
	}
	TS ts = (TS)o;
	Object o2 = request_params.getContents ( "Index" );
	if ( o2 == null ) {
		String warning = "Request SetTimeSeries() does not provide an Index parameter.";
		bean.setWarningText ( warning );
		bean.setWarningRecommendationText ("This is likely a software code error.");
		throw new RequestParameterNotFoundException ( warning );
	}
	Integer Index = (Integer)o2;
	__tsengine.setTimeSeries ( ts, Index.intValue() );
	//PropList results = bean.getResultsPropList();
	// No data are returned in the bean.
	return bean;
}

/**
Process the SetTimeSeriesView request.
*/
private CommandProcessorRequestResultsBean processRequest_SetTimeSeriesView (
        String request, PropList request_params )
throws Exception {
    TSCommandProcessorRequestResultsBean bean = new TSCommandProcessorRequestResultsBean();
    // Get the necessary parameters.
    Object o = request_params.getContents ( "TimeSeriesView" );
    if ( o == null ) {
        String warning = "Request SetTimeSeriesView() does not provide a TimeSeriesView parameter.";
        bean.setWarningText ( warning );
        bean.setWarningRecommendationText ("This is likely a software code error.");
        throw new RequestParameterNotFoundException ( warning );
    }
    TimeSeriesView o_TimeSeriesView = (TimeSeriesView)o;
    // Loop through the views.  If a matching table ID is found, reset.  Otherwise, add at the end.
    int size = __TimeSeriesViewList.size();
    TimeSeriesView view;
    boolean found = false;
    for ( int i = 0; i < size; i++ ) {
        view = (TimeSeriesView)__TimeSeriesViewList.get(i);
        if ( view.getViewID().equalsIgnoreCase(o_TimeSeriesView.getViewID())) {
            __TimeSeriesViewList.set(i,o_TimeSeriesView);
            found = true;
        }
    }
    if ( !found ) {
        __TimeSeriesViewList.add ( o_TimeSeriesView );
    }
    // No data are returned in the bean.
    return bean;
}

/**
Read the command file and initialize new commands.  The initial working directory for the processor will be set
to the directory of the command file.  The addCommandsFromStringList() method is called on the strings from the file.
@param path Path to the command file - this should be an absolute path.
@param createUnknownCommandIfNotRecognized If true, create a GenericCommand
if the command is not recognized or has a syntax problem.
This is being used during transition of old string commands to full Command classes and
may be needed in any case to preserve commands that were manually edited.
Commands with problems will in any case be flagged at run-time as unrecognized or problematic.
@param append If true, the commands will be appended to the existing commands.
@param runDiscoveryOnLoad if true, run discovery mode on the commands at load; if false, do not run discovery
@exception IOException if there is a problem reading the file.
@exception FileNotFoundException if the specified commands file does not exist.
*/
public void readCommandFile ( String path, boolean createUnknownCommandIfNotRecognized, boolean append, boolean runDiscoveryOnLoad )
throws IOException, FileNotFoundException {
	BufferedReader br = null;
	br = new BufferedReader( new FileReader(path) );
	try {
        setCommandFileName ( path ); // This is used in headers, etc.
        // Read all the lines in the file.
        String line;
        List<String> commandStrings = new ArrayList<>();
        while ( true ) {
        	line = br.readLine();
        	if ( line == null ) {
        		break;
        	}
        	// The line may contain spaces at the front due to indentation.
        	commandStrings.add ( line );
        } // Looping over commands in file.
        // Search for comment annotations in the file and set global settings.
        // Search for "@runDiscoveryOnLoad" at the start of the comment and if found change runDiscoveryOnLoad to false.
    	boolean inBlockComment = false;
        for ( String commandString: commandStrings ) {
        	// Trim because the command may contain spaces at the front due to indentation.
        	String commandStringTrimmed = commandString.trim();
        	if ( commandString.startsWith("#") ) {
        		// No discovery mode.
        		continue;
        	}
        	// Put the following after the above because #/* comments out a block start.
        	else if ( commandStringTrimmed.startsWith("/*") ) {
        		inBlockComment = true;
        	}
        	else if ( commandStringTrimmed.startsWith("*/") ) {
        		inBlockComment = false;
        	}
        	if ( inBlockComment ) {
        		continue;
        	}
        	String commandStringTrimmedUpper = commandStringTrimmed.toUpperCase();
        	int pos = commandStringTrimmedUpper.indexOf("@RUNDISCOVERYONLOAD");
        	if ( pos > 0 ) {
        		// Check to see if true or false after the annotation.
        		if ( commandStringTrimmedUpper.indexOf("FALSE",(pos+19)) >= 0 ) {
        			runDiscoveryOnLoad = false;
        		}
        	}
        }
        // Now add the commands from the string list that was read.
        File path_File = new File(path);
        String initialWorkingDir = path_File.getParent();
        addCommandsFromStringList ( commandStrings, createUnknownCommandIfNotRecognized,
            append, runDiscoveryOnLoad, new File(initialWorkingDir) );
	}
	finally {
        // Close the file.
	    if ( br != null ) {
	        br.close();
	    }
	}
}

/**
Run discovery on the command.
This will, for example, make available a list of time series to be requested with the
ObjectListProvider.getObjectList() method.
@param command command that needs to be run in discovery mode
*/
private void readCommandFile_RunDiscoveryOnCommand ( Command command ) {
    String routine = getClass().getSimpleName() + ".readCommandFile_RunDiscoveryOnCommand";
    // Run the discovery.
    if ( Message.isDebugOn ) {
        Message.printStatus(2, routine, "Running discovery mode on command:  \"" + command + "\"" );
    }
    try {
        ((CommandDiscoverable)command).runCommandDiscovery(indexOf(command));
    }
    catch ( Exception e ) {
        // TODO SAM 2011-02-17 Need to show warning to user?  With current design, code should have complete input.
        // For now ignore because edit-time input may not be complete.
        String message = "Unable to make discover run - may be OK if partial data.";
        Message.printStatus(2, routine, message);
        Message.printWarning(3, routine, e);
    }
}

/**
Method for TSSupplier interface.
Read a time series given a time series identifier string.
The string may be a file name if the time series are stored in files,
or may be a true identifier string if the time series is stored in a database.
The specified period is read.  The data are converted to the requested units.
@param tsident_string Time series identifier or file name to read.
@param req_date1 First date to query.  If specified as null the entire period will be read.
@param req_date2 Last date to query.  If specified as null the entire period will be read.
@param req_units Requested units to return data.
If specified as null or an empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series header will be read.
@return Time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public TS readTimeSeries (	String tsident_string, DateTime req_date1, DateTime req_date2,
				String req_units, boolean read_data )
throws Exception {
	return __tsengine.readTimeSeries ( tsident_string, req_date1, req_date2, req_units, read_data );
}

/**
Method for TSSupplier interface.
Read a time series given an existing time series and a file name.  The specified period is read.
The data are converted to the requested units.
@param req_ts Requested time series to fill.  If null, return a new time series.
If not null, all data are reset, except for the identifier,
which is assumed to have been set in the calling code.
This can be used to query a single time series from a file that contains multiple time series.
@param fname File name to read.
@param date1 First date to query.  If specified as null the entire period will be read.
@param date2 Last date to query.  If specified as null the entire period will be read.
@param req_units Requested units to return data.
If specified as null or an empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series header will be read.
@return Time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public TS readTimeSeries ( TS req_ts, String fname, DateTime date1, DateTime date2, String req_units, boolean read_data )
throws Exception {
	return __tsengine.readTimeSeries ( req_ts, fname, date1, date2, req_units, read_data );
}

/**
Method for TSSupplier interface.
Read a time series list from a file (this is typically used used where a time
series file can contain one or more time series).  The specified period is read.
The data are converted to the requested units.
@param fname File to read.
@param date1 First date to query.  If specified as null the entire period will be read.
@param date2 Last date to query.  If specified as null the entire period will be read.
@param req_units Requested units to return data.
If specified as null or an empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series header will be read.
@return list of time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public List<TS> readTimeSeriesList ( String fname, DateTime date1, DateTime date2, String req_units, boolean read_data )
throws Exception {
	return __tsengine.readTimeSeriesList ( fname, date1, date2, req_units, read_data );
}

/**
Method for TSSupplier interface.
Read a time series list from a file or database using the time series identifier information as a query pattern.
The specified period is read.  The data are converted to the requested units.
@param tsident A TSIdent instance that indicates which time series to query.
If the identifier parts are empty, they will be ignored in the selection.
If set to "*", then any time series identifier matching the field will be selected.
If set to a literal string, the identifier field must match exactly to be selected.
@param fname File to read.
@param date1 First date to query.  If specified as null the entire period will be read.
@param date2 Last date to query.  If specified as null the entire period will be read.
@param req_units Requested units to return data.
If specified as null or an empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series header will be read.
@return list of time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public List<TS> readTimeSeriesList ( TSIdent tsident, String fname, DateTime date1, DateTime date2,
					String req_units, boolean read_data )
throws Exception {
	return __tsengine.readTimeSeriesList ( tsident, fname, date1, date2, req_units, read_data );
}

/**
Remove all CommandProcessorEventListener.
*/
public void removeAllCommandProcessorEventListeners ( ) {
    // Just reset the array to null.
    __CommandProcessorEventListener_array = null;
}

/**
Remove all commands.
*/
public void removeAllCommands () {
	int size = __CommandList.size();
	if ( size > 0 ) {
		__CommandList.clear ();
		notifyCommandListListenersOfRemove ( 0, size - 1 );
	}
}

/**
Remove all ensembles.
*/
private void removeAllEnsembles () {
    if ( __TSEnsembleList != null ) {
        __TSEnsembleList.clear();
    }
}

/**
Remove all pattern time series, for example at the start of a run.
*/
private void removeAllPatternTS () {
    if ( __patternTSList != null ) {
        __patternTSList.clear();
    }
}

/**
Remove a command at a position.
@param index Position (0+) at which to remove command.
*/
public void removeCommandAt ( int index ) {
	String routine = getClass().getSimpleName() + ".removeCommandAt";
	__CommandList.remove ( index );
	notifyCommandListListenersOfRemove ( index, index );
	Message.printStatus(2, routine, "Remove command object at [" + index + "]" );
}

/**
Remove a CommandListListener.
@param listener CommandListListener to remove.
*/
public void removeCommandListListener ( CommandListListener listener ) {
	if ( listener == null ) {
		return;
	}
	if ( __CommandListListener_array != null ) {
		// Loop through and set to null any listeners that match the requested listener.
		int size = __CommandListListener_array.length;
		int count = 0;
		for ( int i = 0; i < size; i++ ) {
			if ( (__CommandListListener_array[i] != null) && (__CommandListListener_array[i] == listener) ) {
				__CommandListListener_array[i] = null;
			}
			else {
			    ++count;
			}
		}
		// Now resize the listener array.
		CommandListListener [] newlisteners = new CommandListListener[count];
		count = 0;
		for ( int i = 0; i < size; i++ ) {
			if ( __CommandListListener_array[i] != null ) {
				newlisteners[count++] = __CommandListListener_array[i];
			}
		}
		__CommandListListener_array = newlisteners;
		newlisteners = null;
	}
}

/**
Reset the workflow global properties to defaults, necessary when a command processor is rerun.
For example, initial properties include user name, home folder, etc.
This is the original code - newer overloaded allows the initial properties.
@param intialProps initial property list to use to initialize the processor properties,
such as specified on the TSTool command line
*/
/*
private void resetWorkflowProperties ()
throws Exception {
	// Reset the workflow with no initial properties.
	resetWorkflowProperties ( null );
}
*/

/**
Reset the workflow global properties to defaults, necessary when a command processor is rerun.
For example, initial properties include user name, home folder, etc.
*/
private void resetWorkflowProperties ()
throws Exception {
    String routine = getClass().getSimpleName() + ".resetWorkflowProperties";
    Message.printStatus(2, routine, "Resetting workflow properties." );

    // First clear user-defined properties.
    __propertyHashmap.clear();

    // Define some standard properties.
    __propertyHashmap.put ( "ComputerName", InetAddress.getLocalHost().getHostName() ); // Useful for messages.
    // Use new time zone class.
    ZonedDateTime now = ZonedDateTime.now();
    __propertyHashmap.put ( "ComputerTimezone", now.getZone().getId() ); // America/Denver, etc.
    __propertyHashmap.put ( "InstallDir", IOUtil.getApplicationHomeDir() );
    __propertyHashmap.put ( "InstallDirPortable", IOUtil.toPortablePath(IOUtil.getApplicationHomeDir()) );
    __propertyHashmap.put ( "InstallDirPosix", IOUtil.toPosixPath(IOUtil.getApplicationHomeDir()) );
    __propertyHashmap.put ( "InstallDirURL", "file:///" + IOUtil.getApplicationHomeDir().replace("\\", "/") );
    // Temporary directory useful in some cases.
    __propertyHashmap.put ( "TempDir", System.getProperty("java.io.tmpdir") );
    __propertyHashmap.put ( "TempDirPosix", IOUtil.toPosixPath(System.getProperty("java.io.tmpdir")) );
    // FIXME smalers 2016-04-03 This is hard-coded for TSTool - need to make more generic to work outside of TSTool?
    // FIXME smalers 2022-12-06 before this date used the following.
    //String homeDir = System.getProperty("user.home") + File.separator + ".tstool";
    String homeDir = System.getProperty("user.home");
    __propertyHashmap.put ( "UserHomeDir", homeDir );
    __propertyHashmap.put ( "UserHomeDirPosix", IOUtil.toPosixPath(homeDir) );
    __propertyHashmap.put ( "UserHomeDirURL", "file:///" + homeDir.replace("\\", "/") );
    __propertyHashmap.put ( "UserTstoolDir", homeDir + File.separator + ".tstool");
    __propertyHashmap.put ( "UserTstoolDirPosix", IOUtil.toPosixPath(homeDir) + "/.tstool" );
    __propertyHashmap.put ( "UserTstoolDirURL", "file:///" + homeDir.replace("\\", "/") + "/.tstool");
    __propertyHashmap.put ( "UserName", System.getProperty("user.name") );
    // Set the program version as a property, useful for version-dependent command logic.
    // Assume the version is xxx.xxx.xxx beta (date), with at least one period.
    // Save the program version as a string.
    String programVersion = IOUtil.getProgramVersion();
    int pos = programVersion.indexOf(" ");
    if ( pos > 0 ) {
    	programVersion = programVersion.substring(0,pos);
    }
    __propertyHashmap.put ( "ProgramVersionString", programVersion );
    // Also save the numerical version.
    double programVersionNumber = -1.0;
    pos = programVersion.indexOf(".");
    StringBuilder b = new StringBuilder();
    if ( pos < 0 ) {
    	// Just a number.
    	b.append(programVersion);
    }
    else {
    	// Transfer the characters including the first period but no other periods:
    	// - only include the first 3 parts since the additional parts may be 'beta', 'dev2', etc.
    	b.append(programVersion.substring(0,pos) + ".");
    	int partCount = 1;
    	for ( int i = pos + 1; i < programVersion.length(); i++ ) {
    		if ( programVersion.charAt(i) == '.' ) {
    			++partCount;
    			if ( partCount == 3 ) {
    				break;
    			}
    			else {
    				continue;
    			}
    		}
    		else {
    			b.append ( programVersion.charAt(i) );
    		}
    	}
    }
    // Also remove any non-digits like would occur in "beta", etc.
    for ( int i = pos + 1; i < b.length(); i++ ) {
    	if ( !Character.isDigit(b.charAt(i)) ) {
    		b.deleteCharAt(i--);
    	}
    }
    try {
    	programVersionNumber = Double.parseDouble(b.toString());
    }
    catch ( NumberFormatException e ) {
    	programVersionNumber = -1.0;
    }
    __propertyHashmap.put ( "ProgramVersionNumber", new Double(programVersionNumber) );

    // Set initial properties, such as from the command line:
    // - all are strings, so are immutable
	Message.printStatus(2, routine, "Setting initial (command line) application properties in processor." );
    if ( this.initialProps == null ) {
	    Message.printStatus(2, routine, "There are no initial application properties (null list)." );
    }
    else {
    	if ( this.initialProps.getList().size() == 0 ) {
    		Message.printStatus(2, routine, "There are 0 initial application properties." );
    	}
    	for ( Prop prop : this.initialProps.getList() ) {
			Message.printStatus(2, routine, "  Setting initial (command line) application property in processor: " +
				prop.getKey() + "=" + prop.getValue() );
    		__propertyHashmap.put(prop.getKey(), prop.getValue());
    	}
    }

    // TODO smalers 2021-07-30 Remove the following when the above checks out.
    /*
    // Handle the special case of processor properties that are set on the command line when the application was started.
    // These need to be shared because they are often used to share important environment configurations.
	PropList appProps = IOUtil.getPropListManager().getPropList("TSTool.CommandLine");
	if ( appProps == null ) {
		Message.printWarning(3,routine,"Application properties are not available - software code error.");
	}
	else {
		for ( Prop prop : appProps.getList() ) {
			// Make a copy so as to not corrupt the application properties:
			// - there is no copy constructor so pass the parts
			Message.printStatus(2, routine, "Setting application property in processor: " +
				prop.getKey() + "=" + prop.getValue() );
			PropList requestParams = new PropList("");
			requestParams.set("PropertyName=" + prop.getKey());
			requestParams.set("PropertyValue=" + prop.getValue());
			runnerProcessor.processRequest ("SetProperty", requestParams );
		}
    }
	*/

    // Now make sure that specific controlling properties that may be set with commands are cleared out.
    // FIXME SAM 2008-07-15 Move data members from TSEngine to this class.
    __tsengine.setIgnoreLEZero ( false );
    __tsengine.setIncludeMissingTS ( false );
    __tsengine.setInputEnd ( null );
    __tsengine.setInputStart ( null );
    __tsengine.setOutputEnd ( null );
    __tsengine.setOutputStart ( null );
    __tsengine.setOutputYearType ( YearType.CALENDAR );
}

/**
Run the specified commands.  If no commands are specified, run all that are being managed.
@param commands list of Command to process.
@param runProps Properties to control run.  See full list in TSEngine.processCommands.
This method only acts on the properties shown below.
<td><b>Property</b></td>    <td><b>Description</b></td>
</tr>

<tr>
<td><b>ResetWorkflowProperties</b></td>
<td>If set to true (default), indicates that global processor properties (output period, etc.)
should be reset before running.
</td>
<td>False</td>
</tr>

</table>
*/
public void runCommands ( List<Command> commands, PropList runProps )
throws Exception {
    // Reset the global workflow properties if requested.
    boolean resetWorkflowProperties = true;   // Default.
    if ( runProps != null ) {
        String prop = runProps.getValue ( "ResetWorkflowProperties" );
        if ( (prop != null) && prop.equalsIgnoreCase("False") ) {
            resetWorkflowProperties = false;
        }
    }
    if ( resetWorkflowProperties ) {
    	// Clear all processor properties before running:
    	// - some standard properties will be initialized for each run
        resetWorkflowProperties();
    }

    // Currently, always set the properties defined for the initial application command line.

    // Remove all registered CommandProcessorEventListener,
    // so that listeners don't get added more than once if the processor is rerun.
    // Currently this will require that an OpenCheckFile()
    // command is always run since it is the only thing that handles events at this time.
    removeAllCommandProcessorEventListeners();

    // Now call the TSEngine method to do the processing.
    // FIXME SAM 2008-07-15 Need to merge TSEngine into TSCommandProcess when all commands have been converted to classes,
    // then code size should be more manageable and can remove redundant code in the two classes.
	__tsengine.processCommands ( commands, runProps );

	// Now finalize the results by processing the check files, if any.

	if ( __CommandProcessorEventListener_array != null ) {
    	for ( int i = 0; i < __CommandProcessorEventListener_array.length; i++ ) {
    	    CommandProcessorEventListener listener = __CommandProcessorEventListener_array[i];
    	    if ( listener instanceof CheckFileCommandProcessorEventListener ) {
    	        CheckFileCommandProcessorEventListener cflistener = (CheckFileCommandProcessorEventListener)listener;
    	        cflistener.finalizeOutput();
    	    }
    	}
	}

    // Remove all registered CommandProcessorEventListener again so that if by chance editing,
	// etc. generates events don't want to deal with.
    removeAllCommandProcessorEventListeners();
}

/**
Request that processing be canceled.
This sets a flag that is detected in the TSEngine.processCommands() method.
Processing will be canceled as soon as the current command completes its processing.
@param cancel_processing_requested Set to true to cancel processing.
*/
public void setCancelProcessingRequested ( boolean cancel_processing_requested ) {
	__cancel_processing_requested = cancel_processing_requested;
}

/**
Set the name of the commands file where the commands are saved.
@param filename Name of commands file (should be absolute since it will be used in output headers).
*/
public void setCommandFileName ( String filename ) {
	__commandFilename = filename;
}

/**
Set whether commands should clear their run status before running, used to accumulate status with For() commands.
@param clear true if commands should clear their status before running, false if status should be accumulated.
*/
private void setCommandsShouldClearRunStatus ( Boolean clear ) {
	__commandsShouldClearRunStatus = clear;
}

/**
Indicate output files should be created.
@param CreateOutput_boolean true if output should be created, false if not.
*/
protected void setCreateOutput ( Boolean CreateOutput_Boolean ) {
	__CreateOutput_Boolean = CreateOutput_Boolean;
}

/**
Set the list of all DataStore instances known to the processor.
These are named database connections that correspond to input type/name for time series.
This method is normally only called in special cases.
For example, the RunCommands() command sets the data stores from the main processor into the called commands.
Note that each data store in the list is set using setDataStore().
The instance of the list that manages the data stores is not reset.
@param dataStoreList list of DataStore to use in the processor
@param closeOld if true, then any matching data stores are first closed before being set to the
new value (normally this should be false if,
for example, a list of data stores from one processor is passed to another)
*/
public void setDataStores ( List<DataStore> dataStoreList, boolean closeOld ) {
    for ( DataStore dataStore : dataStoreList ) {
        __tsengine.setDataStore(dataStore, closeOld );
    }
}

/**
 * Set the datastore substitute list.
 * Requests for a specific datastore will return a different datastore if matched.
 * @param dssubList datastore substitute list
 */
public void setDatastoreSubstituteList ( List<DataStoreSubstitute> dssubList ) {
	__tsengine.setDatastoreSubstituteList(dssubList);
}

/**
Set the initial working directory for the processor.
This is typically the location of the command file, or a temporary directory if the commands have not been saved.
Also set the current working directory by calling setWorkingDir() with the same information.
@param InitialWorkingDir The current working directory.
*/
public void setInitialWorkingDir ( String InitialWorkingDir ) {
    String routine = getClass().getSimpleName() + ".setInitialWorkingDir";
    Message.printStatus(2, routine, "Setting the initial working directory to \"" + InitialWorkingDir + "\"" );
	__InitialWorkingDir_String = InitialWorkingDir;
	// Also set the working directory.
	setWorkingDir ( __InitialWorkingDir_String );
}

/**
Indicate whether the processor is running.
This should be set in processCommands() and can be monitored by code (e.g., GUI)
that has behavior that depends on whether the processor is running.
The method is protected to allow it to be called from TSEngine (for example when an interrupt occurs)
but would normally not be called from other code.
@param is_running indicates whether the processor is running (processing commands).
*/
protected void setIsRunning ( boolean is_running ) {
	__is_running = is_running;
}

/**
Set the output file list in the command processor object.
The output files can then be retrieved, for example, in the TSTool results area.
@param outputFileList List of output files generated by commands.
*/
private void setOutputFileList ( List<File> outputFileList )
throws Exception {
	__outputFileList = outputFileList;
}

/**
Set the list of all plugin command classes instances known to the processor.
These are needed by any instance of TSCommandFactory to use the plugin commands.
The plugin commands are typically loaded by TSTool at startup (for example)
because it knows how to deal with application folders where plugins are loaded.
@param pluginCommandClasses list of DataStore to use in the processor
@param append if True, append to the internal list.  Otherwise create a new list.
*/
public void setPluginCommandClasses ( List<Class> pluginCommandClasses, boolean append ) {
	// Add to an internal list to protect from manipulation.
	if ( !append ) {
		this.pluginCommandClassList = new Vector<>();
	}
    for ( Class c : pluginCommandClasses ) {
		this.pluginCommandClassList.add(c);
    }
}

/**
Set the data for a built-in named property, required by the CommandProcessor interface.
See the getPropContents method for a list of properties that are handled.
This method simply calls setPropContents () using the information in the Prop instance.
@param prop Property to set.
@return the named property, or null if a value is not found.
@exception Exception if there is an error setting the property.
*/
public void setProp ( Prop prop ) throws Exception {
	setPropContents ( prop.getKey(), prop.getContents() );
}

/**
Set the contents for a built-in named property, required by the CommandProcessor interface.
The following properties are handled.
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>
</tr>

<tr>
<td><b>AutoExtendPeriod</b></td>
<td>A Boolean indicating whether time series period should automatically be extended to the output period at read.
</td>
</tr>

<tr>
<td><b>AverageEnd</b></td>
<td>The date/time for the end of the averaging period, as a DateTime.
</td>
</tr>

<tr>
<td><b>AverageStart</b></td>
<td>The date/time for the start of averaging period, as a DateTime.
</td>
</tr>

<tr>
<td><b>CommandsShouldClearRunStatus</b></td>
<td>Boolean indicating whether CommandStatus.clearLog(CommandPhaseType.RUN) should be called before running.
This was added to handle For() loops in commands since status clear setting is set in the processor.
</td>
</tr>

<tr>
<td><b>DataStore</b></td>
<td>A DataStore instance, such as RiversideDBDataStore.
</td>
</tr>

<tr>
<td><b>DataTestList</b></td>
<td>A list of DataTest, to be processed when evaluating data.
</td>
</tr>

<tr>
<td><b>HydroBaseDMIList</b></td>
<td>A list of open HydroBaseDMI, to be used by other code for reading data.
</td>
</tr>

<tr>
<td><b>IgnoreLEZero</b></td>
<td>A Boolean indicating whether values <= zero should be included in historical averages.
</td>
</tr>

<tr>
<td><b>IncludeMissingTS</b></td>
<td>A Boolean indicating whether reading missing time series should be return empty time series.
</td>
</tr>

<tr>
<td><b>InitialWorkingDir</b></td>
<td>A String containing the path to the initial working directory, from which all paths are determined.
This is usually the directory to the commands file, or the startup directory.
</td>
</tr>

<tr>
<td><b>InputEnd</b></td>
<td>The date/time for the end of reading data, as a DateTime.
</td>
</tr>

<tr>
<td><b>InputStart</b></td>
<td>The date/time for the start of reading data, as a DateTime.
</td>
</tr>

<tr>
<td><b>OutputFileList</b></td>
<td>The list of output files from processing commands as List<File>.
</td>
</tr>

<tr>
<td><b>StartLogEnabled></b></td>
<td>a Boolean indicating whether or not the StartLog command is enabled,
useful to disable when troubleshooting so that all logging is in the main log file.
</td>
</tr>

<tr>
<td><b>TSResultsList</b></td>
<td>The list of time series results, as a Vector of TS.
</td>
</tr>

<tr>
<td><b>TSViewWindowListener</b></td>
<td>The WindowListener that is interested in listing to TSView window events.
This is used when processing a TSProduct in batch mode so that the main
application can close when the TSView window is closed.</td>
</tr>

<tr>
<td><b>WorkingDir</b></td>
<td>The working directory for the processor (should be an absolute path).
</td>
</tr>

</table>
@exception Exception if there is an error setting the properties.
*/
public void setPropContents ( String propName, Object contents ) throws Exception {
	if ( propName.equalsIgnoreCase("AutoExtendPeriod" ) ) {
        __tsengine.setAutoExtendPeriod ( ((Boolean)contents).booleanValue() );
    }
    else if ( propName.equalsIgnoreCase("AverageEnd") ) {
        __tsengine.setAverageEnd ( (DateTime)contents );
    }
    else if ( propName.equalsIgnoreCase("AverageStart") ) {
        __tsengine.setAverageStart ( (DateTime)contents );
    }
    else if ( propName.equalsIgnoreCase("CommandsShouldClearRunStatus") ) {
        setCommandsShouldClearRunStatus((Boolean)contents);
    }
    else if ( propName.equalsIgnoreCase("DataStore" ) ) {
    	// Open the datastore, closing the old datastore if the name is matched.
        __tsengine.setDataStore ( (DataStore)contents, true );
    }
	else if ( propName.equalsIgnoreCase("HydroBaseDMIList" ) ) {
		@SuppressWarnings("unchecked")
		List<HydroBaseDMI> contents2 = (List<HydroBaseDMI>)contents;
		__tsengine.setHydroBaseDMIList ( contents2 );
	}
    else if ( propName.equalsIgnoreCase("IgnoreLEZero" ) ) {
        __tsengine.setIgnoreLEZero ( ((Boolean)contents).booleanValue() );
    }
    else if ( propName.equalsIgnoreCase("IncludeMissingTS" ) ) {
        __tsengine.setIncludeMissingTS ( ((Boolean)contents).booleanValue() );
    }
	else if ( propName.equalsIgnoreCase("InitialWorkingDir" ) ) {
		setInitialWorkingDir ( (String)contents );
	}
	else if ( propName.equalsIgnoreCase("InputEnd") ) {
		__tsengine.setInputEnd ( (DateTime)contents );
	}
	else if ( propName.equalsIgnoreCase("InputStart") ) {
		__tsengine.setInputStart ( (DateTime)contents );
	}
	else if ( propName.equalsIgnoreCase("OutputEnd") ) {
		__tsengine.setOutputEnd ( (DateTime)contents );
	}
	else if ( propName.equalsIgnoreCase("OutputFileList") ) {
		@SuppressWarnings("unchecked")
		List<File> contents2 = (List<File>)contents;
		setOutputFileList ( contents2 );
	}
	else if ( propName.equalsIgnoreCase("OutputStart") ) {
		__tsengine.setOutputStart ( (DateTime)contents );
	}
    else if ( propName.equalsIgnoreCase("OutputYearType") ) {
        YearType outputYearType = (YearType)contents;
        __tsengine.setOutputYearType ( outputYearType );
    }
    else if ( propName.equalsIgnoreCase("StartLogEnabled") ) {
    	// Use the built-in properties.
        setStartLogEnabled((Boolean)contents);
    }
	else if ( propName.equalsIgnoreCase("TSResultsList") ) {
		@SuppressWarnings("unchecked")
		List<TS> contents2 = (List<TS>)contents;
		__tsengine.setTimeSeriesList ( contents2 );
	}
    else if ( propName.equalsIgnoreCase("TSViewWindowListener") ) {
        __tsengine.addTSViewWindowListener((WindowListener)contents );
    }
    else if ( propName.equalsIgnoreCase("WorkingDir") ) {
        setWorkingDir ( (String)contents );
    }
	else {
	    // Not recognized.
		String message = null;
		if ( contents instanceof String ) {
			// Common case so provide more information.
			message = "Unable to set TSCommandProcessor data for unknown property \"" + propName + "\"=\"" + contents + "\".";
		}
		else {
			// Could be a complex object so don't output the value.
			message = "Unable to set TSCommandProcessor data for unknown property \"" + propName + "\".";
		}
		throw new UnrecognizedRequestException ( message );
	}
}

/**
 * Set a processor property.
 * This will set a built-in property if found and otherwise will set a user-defined property.
 * This is similar to the processRequest_SetProperty() method,
 * but does not require creating a bean object to pass the request.
 */
public void setProperty ( String propName, Object contents ) {
    // Try to set official property.
    Collection<String> internalProperties = getPropertyNameList(true,false);
    if ( internalProperties.contains(propName) ) {
	    try {
	    	// Null is OK here for o2.
	        setPropContents(propName, contents);
	    }
	    catch ( Exception e ) {
	        // Not recognized as a core internal so will set below as a user property.
	    	// TODO smalers 2022-06-21 this does not actually do anything here or in processRequest_SetProperty?
	    }
    }
    else {
	    // Otherwise it is a user-defined property.
	    __propertyHashmap.put ( propName, contents );
    }
}

/**
Indicate whether StartLog commands should be enabled.
@param StartLogEnabled_Boolean true if StartLog commands are enabled, false if not.
*/
protected void setStartLogEnabled ( Boolean StartLogEnabled_Boolean ) {
	__StartLogEnabled_Boolean = StartLogEnabled_Boolean;
}

/**
Set the working directory for the processor.
This is typically set by SetInitialWorkingDir() method when initializing the processor and SetWorkingDir() commands.
@param WorkingDir The current working directory.
*/
protected void setWorkingDir ( String WorkingDir ) {
	__WorkingDir_String = WorkingDir;
}

/**
Return the number of commands being managed by this processor.
This matches the Collection interface, although that is not yet fully implemented.
@return The number of commands being managed by the processor
*/
public int size() {
	return __CommandList.size();
}

}