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
import java.util.ArrayList;
import java.util.List;

import RTi.Util.IO.Command;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.IO.RequirementCheck;
import RTi.Util.IO.RequirementCheckList;
import RTi.Util.IO.UserRequirementChecker;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreRequirementChecker;
import rti.tscommandprocessor.commands.util.Comment_Command;

/**
This class allows a commands file to be be run.  For example, it can be
used to make a batch run of a commands file.
An instance of TSCommandProcessor is created to process the commands.
*/
public class TSCommandFileRunner
{

/**
The TSCommandProcessor instance that is used to run the commands.
*/
private TSCommandProcessor __processor = null;

/**
 * Constructor.
 * The initial properties should typically be passed throughout nested runners to ensure that
 * processors have initial environment properties.
 * @param initProps initial properties for runner, from application command line properties
 */
@SuppressWarnings("rawtypes")
public TSCommandFileRunner ( PropList initProps, List<Class> pluginCommandClassList ) {
	// Create a new processor that is used to run the commands.
	this.__processor = new TSCommandProcessor(initProps);
	this.__processor.setPluginCommandClasses(pluginCommandClassList, false);
}

/**
Determine whether the command file "@enabledif" requirements are met.
This is used in the TSTool RunCommands() command to determine if a command file meets requirements to be enabled,
typically version compatibility.
This method is static because it is called for single commands and full command file.
Syntax for command files is, for example:
<pre>
#@enabledif application tstool version > x.y.z
#@enabledif datastore HydroBase version >= YYYYMMDD
#@enabledif user someuser == abc
</pre>
@param processor The command processor.
@param commands list of commands to check.
If null or empty, process all the commands for the processor, such as when called from RunCommands command.
If a single command, then checking syntax errors in a comment from the command processor.
@return RequirementCheckList object that contains check criteria, result, and justification of the result.
If ANY comments have "@enabledif" statements that evaluate to false, the overall check result will be false, and otherwise true.
*/
public static RequirementCheckList checkEnabled ( TSCommandProcessor processor, List<Command> commands ) {
	return checkRequirementsForAnnotation ( processor, commands, "@enabledif");
}

/**
Determine whether the command file "@require" requirements are met.
This is used in the TSTool RunCommands() command to determine if a command file meets requirements to run,
typically version compatibility.
This method is static because it is called for single commands and full command file.
Syntax for command files is, for example:
<pre>
#@require application tstool version > x.y.z
#@require datastore HydroBase version >= YYYYMMDD
#@require user someuser == abc
</pre>
@param processor The command processor.
@param commands list of commands to check.
If null or empty, process all the commands for the processor, such as when called from RunCommands command.
If a single command, then checking syntax errors in a comment from the command processor.
@return RequirementCheckList object that contains check criteria, result, and justification of the result.
If ANY comments have "@require" statements that evaluate to false, the overall check result will be false, and otherwise true.
*/
public static RequirementCheckList checkRequirements ( TSCommandProcessor processor, List<Command> commands ) {
	return checkRequirementsForAnnotation ( processor, commands, "@require");
}

/**
Determine whether the command file requirements are met.
This is used in the TSTool RunCommands() command to determine if a command file meets requirements to run,
typically version compatibility.
This method is static because it is called for single commands and full command file.
Syntax for command files is, for example:
<pre>
#@require application tstool version > x.y.z
#@require datastore HydroBase version >= YYYYMMDD
#@require user someuser == abc
</pre>
@param processor The command processor.
@param commands list of commands to check.
If null or empty, process all the commands for the processor, such as when called from RunCommands command.
If a single command, then checking syntax errors in a comment from the command processor.
@param annotation annotation to check, either "@require" or "@enabledif"
@return RequirementCheckList object that contains check criteria, result, and justification of the result.
If any comments have "@require" statements that evaluate to false, the overall check result will be false, and otherwise true.
*/
private static RequirementCheckList checkRequirementsForAnnotation ( TSCommandProcessor processor, List<Command> commands, String annotation ) {
	String routine = TSCommandFileRunner.class.getSimpleName() + ".checkRequirementsForAnnotation";
	RequirementCheckList checkList = new RequirementCheckList();
	String message;
	// Use upper case annotation for string comparisons.
	String annotationUpper = annotation.toUpperCase();
	//boolean requirementsMet = true; // Default until indicated otherwise.
	if ( (commands == null) || (commands.size() == 0) ) {
		commands = processor.getCommands();
	}
    String commandString;
    String commandStringUpper;
    int pos;
    //String appName;
    String reqProperty;
    String datastoreName;
    String operator;
    String reqVersion;
    String checkerName = "TSCommandFileRunner"; // Default value - specific checker will supply scope-specific name.
    for ( Command command : commands ) {
   		if ( command instanceof Comment_Command ) {
   			commandString = command.toString();
   			commandStringUpper = commandString.toUpperCase();
   			// The following handles #@require and # @require (whitespace after comment):
   			// - get the string starting with @require for parsing below
   			pos = commandStringUpper.indexOf(annotationUpper);
   			if ( pos > 0 ) {
   				// Detected a @require or @enabledif annotation based on the passed in 'annotation':
   				// - check the token following @require or @enabledif
   				// - 'reqString' includes @ but not leading #
   				String reqString = commandString.substring(pos - 1).trim();
   				// Create a requirement check object:
   				// - add here to make sure it is added
   				// - further populate below
   				RequirementCheck check = new RequirementCheck (reqString);
   				checkList.add(check);
   				Message.printStatus(2, routine, "Detected " + annotation + ": " + commandString);
   				if ( commandString.length() > (pos + annotationUpper.length()) ) {
   					// Have trailing characters after @require or @enabledif so can continue processing:
   					// - split the comment string
   					// - does not include # or any trailing spaces
   					// - part[0] is @require or @enabledif
   					String [] requireParts = commandString.substring(pos).split(" ");
   					Message.printStatus(2, routine, annotation + " comment has " + requireParts.length + " parts.");
   					if ( requireParts.length > 1 ) {
   						// Have at least the requirement type.
   						if ( requireParts[1].trim().toUpperCase().startsWith("APP") ) {
   							String example = "\n  Example: #" + annotation + " application TSTool version > 1.2.3";
   							Message.printStatus(2, routine, "Detected application requirement.");
   							if ( requireParts.length < 6 ) {
   								message = "Error processing " + annotation + " - expecting 6+ tokens (have " + requireParts.length + "): " + commandString + example;
   								check.setIsRequirementMet(checkerName, false, message);
   								Message.printWarning(3, routine, message);
   							}
   							else {
   								//appName = parts[2].trim(); // For example:  TSTool or StateDMI (used for requirement readability, ignored in check)
   								reqProperty = requireParts[3].trim();
   								Message.printStatus(2, routine, annotation + " requirement property: " + reqProperty);
   								if ( reqProperty.equalsIgnoreCase("version") ) {
   									// Checking the application version.
   									operator = requireParts[4].trim(); // Operator to compare the data.
   									reqVersion = requireParts[5].trim(); // Version criteria to compare.
   									// Get the version for the application, currently there is no more flexibility to check other application properties.
   									//String appVersion = processor.getStateDmiVersionString();
   									String appVersion = IOUtil.getProgramVersion();
   									Message.printStatus(2, routine, annotation + " application version: " + appVersion);
   									if ( (appVersion == null) || appVersion.isEmpty() ) {
   										message = "Can't check application version for " + annotation + " (application version is unknown): " + commandString + example;
   										check.setIsRequirementMet(checkerName, false, message);
   										Message.printWarning(3, routine, message);
   									}
   									else if ( (reqVersion == null) || reqVersion.isEmpty() ) {
   										message = "Don't know how to determine application version for " + annotation + " (no version given): " + commandString + example;
   										check.setIsRequirementMet(checkerName, false, message);
   										Message.printWarning(3, routine, message);
   									}
   									else {
   										// If the application version contains a space such as '(x.x.x (YYYY-MM-DD)', only use the first part.
   										if ( appVersion.indexOf(' ') > 0) {
   											appVersion = appVersion.substring(0,appVersion.indexOf(' '));
   											Message.printStatus(2, routine, annotation + " application version after parsing: " + appVersion);
   										}
   										// Check the application version against the requirement, using semantic version comparison.
   										// - only compare the first 3 parts because modifier can cause issues comparing.
   										if ( !StringUtil.compareSemanticVersions(appVersion, operator, reqVersion, 3) ) {
   											message = "Application version (" + appVersion + ") does not meet requirement." + example;
   											Message.printStatus(2, routine, message );
   											check.setIsRequirementMet(checkerName, false, message);
   										}
   										else {
   											// Must set the requirement as met because the default is false.
   											check.setIsRequirementMet(checkerName, true, "");
   											message = "Application version (" + appVersion + ") does meet requirement.";
   											Message.printStatus(2, routine, message );
   										}
   									}
   								}
   								else {
   									message = annotation + " application property (" + reqProperty + ") is not recognized: " + commandString + example;
   									check.setIsRequirementMet(checkerName, false, message);
   									Message.printWarning(3, routine, message);
   								}
   							}
                    	}
   						else if ( requireParts[1].trim().equalsIgnoreCase("DATASTORE") ) {
   							// For example:
   							// @require datastore HydroBase version >= 20200720
   							String example = "\n  Example: #" + annotation + " datastore HydroBase version > 20200720";
   							Message.printStatus(2, routine, "Detected datastore requirement.");
   							if ( requireParts.length < 6 ) {
   								message = "Error processing " + annotation + " - expecting 6+ tokens (have " + requireParts.length + "): " + commandString + example;
  								// Let the datastore fill in its checker name to override the initial value.
								check.setIsRequirementMet(checkerName, false, message);
   								Message.printWarning(3, routine, message);
   							}
   							else {
								// datastoreName is needed to check whether it implements the DataStoreRequirementChecker interface.
								datastoreName = requireParts[2].trim();
   								reqProperty = requireParts[3].trim();
   								// The following handles datastore name substitution.
								DataStore dataStore = processor.getDataStoreForName ( datastoreName, null );
								if ( dataStore == null ) {
   									message = "Unable to get datastore for name \"" + datastoreName + "\"";
   									// Let the datastore fill in its checker name to override the initial value.
   									check.setIsRequirementMet(checkerName, false, message);
   									Message.printWarning(3, routine, message);
								}
								else {
									// Have found the datastore of interest.
									Message.printStatus(2, routine, "Requested datastore name = '" + datastoreName + "' actual datastore name = '" + dataStore.getName() + "'");
									if ( dataStore instanceof DataStoreRequirementChecker ) {
										// The datastore implements a requirement checker so use it.
   										DataStoreRequirementChecker checker = (DataStoreRequirementChecker)dataStore;
   										// The following will handle reason for failure and will set check to true if condition is met.
   										checker.checkRequirement(check);
   									}
   									else {
   										// The datastore does not implement a requirement checker so fail by default.
   										message = "Datastore code DOES NOT implement requirement checker - can't check requirement.";
   										check.setIsRequirementMet(checkerName, false, message);
   										Message.printWarning(3, routine, message);
   									}
   								}
   							}
   						}
   						else if ( requireParts[1].trim().equalsIgnoreCase("USER") ) {
   							// For example:
   							// @require user == username
   							Message.printStatus(2, routine, "Detected user requirement.");
   							String example = "\n  Example: #" + annotation + " user != root";
   							if ( requireParts.length < 4 ) {
   								message = "Error processing " + annotation + " - expecting 4+ tokens (have " + requireParts.length + "): " + commandString + example;
								check.setIsRequirementMet(checkerName, false, message);
   								Message.printWarning(3, routine, message);
   							}
   							else {
   								// Evaluate the user.
   								UserRequirementChecker userChecker = new UserRequirementChecker();
   								// The following will handle reason for failure.
   								userChecker.checkRequirement(check);
   							}
   						}
   						else {
   							message = "Error processing " + annotation + " - unknown type: " + requireParts[1];
							check.setIsRequirementMet(checkerName, false, message);
							Message.printWarning(3, routine, message);
   						}
   						// If failure here and no message have a coding problem because message needs to be non-empty.
   						if ( ! check.isRequirementMet() && check.getFailReason().isEmpty() ) {
   							message = annotation + " was not met but have empty fail message - need to fix software.";
							check.setIsRequirementMet(checkerName, false, message);
							Message.printWarning(3, routine, message);
   						}
                    }
   					else {
  						message = "Error processing " + annotation + " - expecting 2+ tokens (have " + requireParts.length + ").";
						check.setIsRequirementMet(checkerName, false, message);
   						Message.printWarning(3, routine, message);
   					}
                }
   				else {
  					message = "Error processing " + annotation + " - expecting at least 2+ tokens but line is too short: " + commandString;
					check.setIsRequirementMet(checkerName, false, message);
					Message.printWarning(3, routine, message);
					// Throw an exception because bad syntax.
   					throw new RuntimeException (message);
   				}
            }
   			// Else no @require
        }
    }
    return checkList;
}

/**
Determine whether the command file is enabled.
This is used in the TSTool RunCommands() command to determine if a command file is enabled.
@return false if any comments have "@enabled False" or "@enabledif" conditions that fail,
otherwise return true
*/
public boolean isCommandFileEnabled () {
	String routine = getClass().getSimpleName() + ".isCommandFileEnabled";
    List<Command> commands = __processor.getCommands();
    // Position of @annotation.
    int posEnabled;
    int posEnabledIf;
    // Default is enabled:
    // - will be set to false if requirements are defined that are not met
    boolean enabledOverall = true;
    for ( Command command : commands ) {
        String commandStringUpper = command.toString().toUpperCase();
        posEnabledIf = commandStringUpper.indexOf("@ENABLEDIF");
        posEnabled = commandStringUpper.indexOf("@ENABLED");
        // Check the longer string first because the first part of the annotation is the same.
        if ( posEnabledIf > 0 ) {
           	// Check @enabledif comments for syntax errors:
            // - need to be correct to ensure that tests run properly
            // - when running automated tests with RunCommands, these conditions are checked for the entire command file
            //   before even attempting to run so FAILURE will not occur in test (it won't even be run)
            // - TODO smalers 2021-01-03 evaluate whether this can be put in Comment_Command.runCommand(),
            //   but currently the logic depends on the processor.

        	// Currently only allow the annotations at the front of a comment, otherwise could get confused.
        	String lead = commandStringUpper.substring(0,posEnabledIf);
        	if ( !lead.matches("^[# ]+$") ) {
        		// Don't check the annotation because it is not an expected annotation.
        		continue;
        	}
            try {
            	List<Command> commentList = new ArrayList<>();
                commentList.add(command);
                // Single comment is added to the list to check.
        	    RequirementCheckList checks = TSCommandFileRunner.checkEnabled(__processor, commentList);
        	    if ( !checks.areRequirementsMet() ) {
        	    	// @enabledif requirements were not met, so the command file is NOT enabled.
        	    	enabledOverall = false;
        	    }
            }
            catch ( Exception e ) {
        	    // Syntax error:
                // - mark the command with an error
                // - set the command status information
				Message.printWarning(3, routine, e);
           	}
        }
        else if ( posEnabled > 0 ) {
            //Message.printStatus(2, "", "Detected tag: " + C);
            // Check the token following @enabled

        	// Currently only allow the annotations at the front of a comment, otherwise could get confused.
        	String lead = commandStringUpper.substring(0,posEnabled);
        	if ( !lead.matches("^[# ]+$") ) {
        		// Don't check the annotation because it is not an expected annotation.
        		continue;
        	}
            if ( commandStringUpper.length() > (posEnabled + 8) ) {
                // Have trailing characters so can check for True/False.
                String [] parts = commandStringUpper.substring(posEnabled).split(" ");
                if ( parts.length > 1 ) {
                    if ( parts[1].trim().equals("FALSE") ) {
                        //Message.printStatus(2, "", "Detected false");
                    	enabledOverall = false;
                    }
                }
            }
        }
    }
    //Message.printStatus(2, "", "Did not detect false");
    return enabledOverall;
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
No properties are specified to control the run.
*/
public void runCommands ()
throws Exception {
	__processor.runCommands(
			null, // Subset of Command instances to run - just run all.
			null); // No properties to control run.
}

/**
Run the commands.
@param runProps properties to control the processor execution (these are NOT properties to initialize in the processor).
*/
public void runCommands ( PropList runProps )
throws Exception {
	__processor.runCommands(
			null, // Subset of Command instances to run - just run all commands.
			runProps ); // Properties to control run.
}

/**
Return the command processor used by the runner.
@return the command processor used by the runner
*/
public TSCommandProcessor getProcessor() {
    return __processor;
}

}