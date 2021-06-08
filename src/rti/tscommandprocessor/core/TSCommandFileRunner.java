// TSCommandFileRunner - This class runs a commands file

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

package rti.tscommandprocessor.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import RTi.Util.IO.Command;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreVersionChecker;
import rti.tscommandprocessor.commands.util.Comment_Command;

/**
This class allows a commands file to be be run.  For example, it can be
used to make a batch run of a commands file.  An instance of TSCommandProcessor
is created to process the commands.
*/
public class TSCommandFileRunner
{

/**
The TSCommandProcessor instance that is used to run the commands.
*/
private TSCommandProcessor __processor = new TSCommandProcessor();

/**
Determine whether the command file requirements are met.
This is used in the TSTool RunCommands() command to determine if a command file meets requirements
to run, typically version compatibility.
This method is static because it is called for single commands and full command file.
Syntax for command files is, for example:
<pre>
#@require app tstool > x.y.z
#@reqiure datastore HydroBase >= YYYYMMDD
</pre>
@param processor The command processor.
@param commands list of commands to check.
If null or empty, process all the commands for the processor, such as when called from RunCommands command.
If a single command, then checking syntax errors in a comment from the command processor.
@return false if any comments have "@require" statements that evaluate to false, otherwise true
*/
public static boolean areRequirementsMet ( TSCommandProcessor processor, List<Command> commands ) {
	String routine = TSCommandFileRunner.class.getSimpleName() + ".areRequirementsMet";
	String message;
	boolean requirementsMet = true; // Default until indicated otherwise
	if ( (commands == null) || (commands.size() == 0) ) {
		commands = processor.getCommands();
	}
    String commandString;
    String commandStringUpper;
    int pos;
    String appName;
    String datastoreName;
    String operator;
    String version;
    for ( Command command : commands ) {
   		if ( command instanceof Comment_Command ) {
   			commandString = command.toString();
   			commandStringUpper = commandString.toUpperCase();
   			// The following handles #@require and # @require (whitespace after comment)
   			pos = commandStringUpper.indexOf("@REQUIRE");
   			if ( pos > 0 ) {
   				// Detected a @require annotation.
   				// - check the token following @require
   				Message.printStatus(2, routine, "Detected @REQUIRE: " + commandString);
   				if ( commandString.length() > (pos + 8) ) {
   					// Have trailing characters.
   					// Split the comment starting with @.
   					String [] parts = commandString.substring(pos).split(" ");
   					Message.printStatus(2, routine, "@REQUIRE comment has " + parts.length + " parts.");
   					if ( parts.length > 1 ) {
   						if ( parts[1].trim().toUpperCase().startsWith("APP") ) {
   							Message.printStatus(2, routine, "Detected APP");
   							if ( parts.length < 5 ) {
   								message = "Error processing @require - expecting 5+ tokens (have " + parts.length + "): " + commandString;
   								Message.printWarning(3, routine, message);
   								throw new RuntimeException(message);
   							}
   							else {
   								// appName must be StateDMI
   								appName = parts[2].trim();
   								operator = parts[3].trim();
   								version = parts[4].trim();
   								// Get the version for the app
   								//String appVersion = processor.getStateDmiVersionString();
   								String appVersion = IOUtil.getProgramVersion();
   								if ( (appVersion == null) || appVersion.isEmpty() ) {
   									message = "Don't know how to determine application version for @require: " + commandString;
   									Message.printWarning(3, routine, message);
   									throw new RuntimeException(message);
   								}
   								// If the version contains a space such as '(x.x.x (YYYY-MM-DD)', only use the first part.
   								if ( appVersion.indexOf(' ') > 0) {
   									appVersion = appVersion.substring(0,appVersion.indexOf(' '));
   								}
   								// Check the app version against the requirement, using semantic version comparison.
   								// - only compare the first 3 parts because modifier can cause issues comparing.
   								if ( !StringUtil.compareSemanticVersions(appVersion, operator, version, 3) ) {
   									requirementsMet = false;
   								}
   							}
                    	}
   						else if ( parts[1].trim().equalsIgnoreCase("DATASTORE") ) {
   							// For example:
   							// @require datastore HydroBase >= 20200720
   							Message.printStatus(2, routine, "Detected DATASTORE");
   							if ( parts.length < 5 ) {
   								message = "Error processing @require - expecting 5+ tokens (have " + parts.length + "): " + commandString;
   								Message.printWarning(3, routine, message);
   								throw new RuntimeException (message);
   							}
   							else {
   								// datastoreName should always be 'HydroBase' for general tests
   								datastoreName = parts[2].trim();
   								operator = parts[3].trim();
   								version = parts[4].trim();
   								// Get the datastore from the processor
   								if ( !datastoreName.equalsIgnoreCase("HydroBase") ) {
   									message = "Don't know how to handle datastore name in @require (only handle HydroBase): " + commandString;
   									Message.printWarning(3, routine, message);
   									throw new RuntimeException(message);
   								}
   								DataStore dataStore = processor.getDataStoreForName ( datastoreName, null );
   								if ( dataStore == null ) {
   									// This is currently a major issue since StateDMI depends on HydroBase datastore
   									message = "Unable to get datastore for name \"" + datastoreName + "\"";
   									Message.printWarning(3, routine, message);
   									throw new RuntimeException(message);
   								}
   								else {
   									// Get the version for the processor
   									//String dbVersion = dataStore.getVersion();
   									//HydroBaseDMI dmi = (HydroBaseDMI)dataStore.getDMI();
   									//String dbVersion = dmi.getDatabaseVersionFromName();
   									// Check the datastore version against the requirement, using string comparison since no delimiters.
   									//if ( !StringUtil.compareUsingOperator(versionType, dbVersion, operator, version) ) {
   									if ( dataStore instanceof DataStoreVersionChecker ) {
   										DataStoreVersionChecker checker = (DataStoreVersionChecker)dataStore;
   										if ( !checker.checkVersion(operator, version) ) {
   											Message.printStatus(2, routine, "Database version \"" + checker.getVersionForCheck() + "\" DOES NOT meet required " + operator + " \"" + version + "\"");
   											// Set the return value.
   											// - any false value from list of comments will set the overall return value to false
   											requirementsMet = false;
   										}
   										else {
   											Message.printStatus(2, routine, "Database version \"" + checker.getVersionForCheck() + "\" DOES meet required " + operator + " \"" + version + "\"");
   										}
   									}
   									else {
   										Message.printStatus(2, routine, "Database DOES NOT implement version check.  Cannot check that version " + operator + " \"" + version + "\"");
   										// Set the return value.
   										// - any false value from list of comments will set the overall return value to false
   										requirementsMet = false;
   									}
   								}
   							}
   						}
                    }
   					else {
  						message = "Error processing @require - expecting 5+ tokens (have " + parts.length + "): " + commandString;
   						Message.printWarning(3, routine, message);
   						throw new RuntimeException (message);
   					}
                }
   				else {
  					message = "Error processing @require - expecting 5+ tokens but line is too short: " + commandString;
   					Message.printWarning(3, routine, message);
   					throw new RuntimeException (message);
   				}
            }
   			else {
   				// OK - since comments may not contain any @require
   			}
        }
    }
    return requirementsMet;
}

/**
Determine whether the command file is enabled.
This is used in the TSTool RunCommands() command to determine if a command file is enabled.
@return false if any comments have "@enabled False", otherwise true
*/
public boolean isCommandFileEnabled () {
    List<Command> commands = __processor.getCommands();
    String C;
    int pos;
    for ( Command command : commands ) {
        C = command.toString().toUpperCase();
        pos = C.indexOf("@ENABLED");
        if ( pos >= 0 ) {
            //Message.printStatus(2, "", "Detected tag: " + C);
            // Check the token following @enabled
            if ( C.length() > (pos + 8) ) {
                // Have trailing characters
                String [] parts = C.substring(pos).split(" ");
                if ( parts.length > 1 ) {
                    if ( parts[1].trim().equals("FALSE") ) {
                        //Message.printStatus(2, "", "Detected false");
                        return false;
                    }
                }
            }
        }
    }
    //Message.printStatus(2, "", "Did not detect false");
    return true;
}

/**
Read the commands from a file.
@param filename name of command file to run, should be absolute.
@param runDiscoveryOnLoad indicates whether to run discovery mode on commands when loading (this is
a noticeable performance hit for large command files)
*/
public void readCommandFile ( String path, boolean runDiscoveryOnLoad )
throws FileNotFoundException, IOException {
	__processor.readCommandFile (
		path, // InitialWorkingDir will be set to commands file location
		true, // Create GenericCommand instances for unknown commands
		false, // Do not append the commands.
		runDiscoveryOnLoad );
}


/**
Run the commands.
*/
public void runCommands ()
throws Exception {
	__processor.runCommands(
			null, // Subset of Command instances to run - just run all
			null ); // Properties to control run
}

/**
Run the commands.
@param runProps properties to pass to the processor
*/
public void runCommands ( PropList runProps )
throws Exception {
	__processor.runCommands(
			null, // Subset of Command instances to run - just run all commands
			runProps ); // Properties to control run
}

/**
Return the command processor used by the runner.
@return the command processor used by the runner
*/
public TSCommandProcessor getProcessor() {
    return __processor;
}

}