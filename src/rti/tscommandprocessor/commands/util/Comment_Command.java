// Comment_Command - This class initializes, checks, and runs the * / (comment block end) command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

// If ever add discovery mode, will need to change the command processor to run discovery on comments.
/**
This class initializes, checks, and runs the # single line comment.
An instance is used to uniquely identify this command instead of storing in GenericCommand.
*/
public class Comment_Command extends AbstractCommand
{

/**
Constructor.
*/
public Comment_Command () {
	super();
	setCommandName ( "#" );
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
	// Currently nothing to check.
    String warning = "";
    CommandStatus status = getCommandStatus();
    String commandString = getCommandString();

	// Check for invalid parameters.
	//Vector valid_Vector = new Vector();
	//valid_Vector.add ( "CommandLine" );
	//warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

  	if ( commandString.toUpperCase().contains("@TODO") ) {
   		// Show the notification as a blue rectangle marker:
  		// - do not add to warning string since don't want to show the warning popup dialog
       	status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.NOTIFICATION,
          	"The command file includes a TODO item.",
          	"Update the command file to resolve the issue.  A software update may be required so check release notes." ) );
   	}
   	else if ( commandString.toUpperCase().contains("@FIXME") ) {
   		// Show the notification as a blue rectangle marker:
  		// - do not add to warning string since don't want to show the warning popup dialog
       	status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.NOTIFICATION,
           	"The command file includes a FIXME item.",
           	"Update the command file to resolve the issue.  A software update may be required so check release notes." ) );
   	}

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
	// The command will be modified if changed.
	List<String> v = new ArrayList<>();
    v.add( getCommandString() );
	return (new Comment_JDialog ( parent, v )).ok();
}

/**
Parse the command string.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
    // No parameters.
}

/**
Run the command.
@param commandNumber Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int commandNumber )
throws InvalidCommandParameterException, CommandWarningException, CommandException {   
	String routine = getClass().getSimpleName() + ".runCommand";
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    String commandStringUpper = getCommandString().toUpperCase().trim();
	String commandTag = "" + commandNumber;
	int warningLevel = 2;
	int warningCount = 0;
	String message = null;

    Boolean clearStatus = new Boolean(true); // Default.
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

    Message.printStatus(2, routine, "Checking comment string as upper case: " + commandStringUpper);
    // Check below needs to not match strings like:
    // # - use @sourceUrl and @version
    if ( commandStringUpper.contains("#@SOURCEURL") || commandStringUpper.contains("# @SOURCEURL") ) {
    	// Temporary folder for source file copy and temporary version of TSTool UI commands.
		String tempFolder = System.getProperty("java.io.tmpdir");
		// Full #@sourceUrl annotation, extracted from the local command file.
		String sourceUrl = null;
		// Version of the source command file, extracted after downloading the command file.
		String sourceVersion = null;
		// Version date of the source command file, extracted after downloading the command file.
		String sourceVersionDate = null;
		// Version of the local command file, extracted from in-memory commands.
		String localVersion = null;
		// Version date of the local command file, extracted from in-memory commands.
		String localVersionDate = null;

		// Get the local command file information.
		// File for the source (remote) command file.
		String file1Path = tempFolder + File.separator + "TSTool-commands-source.tstool";
		List<Command> commands = TSCommandProcessorUtil.getAnnotationCommands((TSCommandProcessor)processor,"sourceUrl");
		if ( commands.size() > 1 ) {
			message = "The TSTool commands have multiple #@sourceUrl annotations - can't get command file source.";
			Message.printWarning(3, routine, message);
			++warningCount;
       		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
           		message, "Verify that only one #@sourceUrl annotation comment is used." ) );
		}
		if ( warningCount == 0 ) {
			// Get the URL from the command:
			// - if it is not set, the version comparison will not happen below
			// - source URL is from the first command from above
			sourceUrl = TSCommandProcessorUtil.getAnnotationCommandParameter(commands.get(0), 1);
			commands = TSCommandProcessorUtil.getAnnotationCommands((TSCommandProcessor)processor,"version");
			if ( commands.size() == 0 ) {
				// OK, checked below - must have version and/or versionDate.
			}
			else if ( commands.size() > 1 ) {
				message = "The TSTool commands have multiple #@version annotations - can't check for updates.";
				Message.printWarning(3, routine, message);
				++warningCount;
       			status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
           			message, "Verify that only one #@version annotation comment is used." ) );
			}
			else {
				localVersion = TSCommandProcessorUtil.getAnnotationCommandParameter(commands.get(0), 1);
			}
			commands = TSCommandProcessorUtil.getAnnotationCommands((TSCommandProcessor)processor,"versionDate");
			if ( commands.size() == 0 ) {
				// OK, checked below - must have version and/or versionDate.
			}
			else if ( commands.size() > 1 ) {
				message = "The commands have multiple #@versionDate annotations - can't check for updates.";
				Message.printWarning(3, routine, message);
				++warningCount;
       			status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
           			message, "Verify that only one #@versionDate annotation comment is used." ) );
			}
			else {
				localVersionDate = TSCommandProcessorUtil.getAnnotationCommandParameter(commands.get(0), 1);
			}

			if ( localVersion.isEmpty() && localVersionDate.isEmpty() ) {
				message = "The commands have no #@version or #@versionDate annotation - can't check for updates.";
				Message.printWarning(3, routine, message);
				++warningCount;
       			status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
           			message, "Verify that a #@version and/or #@versionDate annotation comment is used." ) );
			}
		}
		
		if ( warningCount == 0 ) {
			// Get the source command file using the URL and compare the version:
			// - time out is 5 seconds
			if ( IOUtil.getUriContent(sourceUrl, file1Path, null, 5000, 5000) != 200 ) {
				message = "Error retrieving the command file source from: " + sourceUrl;
				Message.printWarning(3, routine, message);
				++warningCount;
       			status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
          				message, "Confirm that the source file can be accessed (may require authentication if a private website)." ) );
			}
		}

		if ( warningCount == 0 ) {
			// Get the version properties out of the source command file:
			// - use a second command processor
			TSCommandProcessor sourceProcessor = new TSCommandProcessor(null);
			try {
				sourceProcessor.readCommandFile ( file1Path, true, false, false );
				commands = TSCommandProcessorUtil.getAnnotationCommands(sourceProcessor,"version");
				if ( commands.size() == 0 ) {
					// OK, checked below - must have version and/or versionDate.
				}
				else if ( commands.size() > 1 ) {
					message = "The source commands have multiple #@version annotations - can't check for updates.";
					Message.printWarning(3, routine, message);
					++warningCount;
       				status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
           				message, "Verify that only one #@version annotation comment is used." ) );
				}
				else {
					sourceVersion = TSCommandProcessorUtil.getAnnotationCommandParameter(commands.get(0), 1);
				}
				commands = TSCommandProcessorUtil.getAnnotationCommands(sourceProcessor,"versionDate");
				if ( commands.size() == 0 ) {
					// OK, checked below - must have version and/or versionDate.
				}
				else if ( commands.size() > 1 ) {
					message = "The source commands have multiple #@versionDate annotations - can't check for updates.";
					Message.printWarning(3, routine, message);
					++warningCount;
       				status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
           				message, "Verify that only one #@versionDate annotation comment is used." ) );
				}
				else {
					sourceVersionDate = TSCommandProcessorUtil.getAnnotationCommandParameter(commands.get(0), 1);
				}
			}
			catch ( Exception e ) {
				message = "Error reading source command file: " + file1Path;
				Message.printWarning(3, routine, message);
				++warningCount;
    			status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
      				message, "Verify that the file exists and is readable." ) );
			}
		}
		if ( warningCount == 0 ) {
			// Do the version comparison:
			// - if 'version' is available for both use it
			// - else if 'versionDate' is available for both use it
			// - if both 'version' and 'versionDate' are available?

			boolean checkDate = false;
			if ( !localVersion.isEmpty() && !sourceVersion.isEmpty() ) {
				// Compare the versions:
				// - both must be either semantic versions or dates
				// - dates are redundant with @versionDate but may be used if semantic versions are not used
				Message.printStatus(2, routine, "Comparing source version \"" + sourceVersion + "\" with local version \"" + localVersion + "\"");
				int localPeriodCount = StringUtil.patternCount(localVersion,".");
				int sourcePeriodCount = StringUtil.patternCount(sourceVersion,".");
				int localDashCount = StringUtil.patternCount(localVersion,"-");
				int sourceDashCount = StringUtil.patternCount(sourceVersion,"-");
				if ( (localPeriodCount > 0) && (sourcePeriodCount > 0) ) {
					// Assume semantic versioning.
					if ( StringUtil.compareSemanticVersions(localVersion, "<", sourceVersion, 3) ) {
						message = "The source commands (" + sourceVersion + ") has a newer @version than local commands (" + localVersion + ").";
						Message.printStatus(2, routine, message);
						status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.NOTIFICATION,
							message, "Need to update the local copy from the original source." ) );
					}
					else if ( StringUtil.compareSemanticVersions(localVersion, ">", sourceVersion, 3) ) {
						message = "The source commands (" + sourceVersion + ") has an older @versionDate than local commands (" + localVersion + ").";
						Message.printStatus(2, routine, message);
						status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.NOTIFICATION,
							message, "Need to update the origional source copy from the local copy." ) );
					}
					else {
						// Versions are the same so no notification.
						// Also check the date as the tie-breaker.
						message = "The source and local versions are the same based on semantic version comparison.";
						Message.printStatus(2, routine, message);
						checkDate = true;
					}
				}
				else if ( (localDashCount > 0) && (sourceDashCount > 0) ) {
					// Compare the versions as date/time strings.
					DateTime localDate = null;
					DateTime sourceDate = null;
					try {
						localDate = DateTime.parse(localVersionDate);
					}
					catch ( Exception e ) {
						message = "The local commands @version seems to be a date/time but is invalid.";
						Message.printWarning(3, routine, message);
						++warningCount;
    					status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
      						message, "Verify that the version is a semantic version (1.2.3.4) or a date YYYY-MM-DD." ) );
					}
					try {
						sourceDate = DateTime.parse(sourceVersionDate);
					}
					catch ( Exception e ) {
						message = "The source commands @version seems to be a date/time but is invalid.";
						Message.printWarning(3, routine, message);
						++warningCount;
    					status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
      						message, "Verify that the version is a semantic version (1.2.3.4) or a date YYYY-MM-DD." ) );
					}
					if ( (localDate != null) && (sourceDate != null) ) {
						if ( sourceDate.greaterThan(localDate) ) {
							// The source is newer than local.
							message = "The source commands (" + sourceVersion + ") has a newer @version than local commands (" + localVersion + ").";
							Message.printStatus(2, routine, message);
							status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.NOTIFICATION,
								message, "Need to update the local copy from the original source." ) );
						}
						else if ( sourceDate.lessThan(localDate) ) {
							// The source is newer than local.
							message = "The source commands (" + sourceVersion + ") has an older @version than local commands (" + localVersion + ").";
							Message.printStatus(2, routine, message);
							status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.NOTIFICATION,
								message, "Need to update the origional source copy from the local copy." ) );
						}
						else {
							// Same so no need to do anything.
							message = "The source and local versions are the same based on date comparison.";
							Message.printStatus(2, routine, message);
						}
					}
				}
				else {
					message = "The source and local @version don't appear to be consistent semantic versions or dates - can't check the version.";
					Message.printWarning(3, routine, message);
					++warningCount;
   					status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
   						message, "Verify that the version in source and local files are both a semantic version (1.2.3.4) or a date YYYY-MM-DD." ) );
				}
			}
			else if ( !localVersionDate.isEmpty() && !sourceVersionDate.isEmpty() ) {
				checkDate = true;
			}
			if ( checkDate ) {
				// Compare the versions.
				DateTime localDate = null;
				DateTime sourceDate = null;
				try {
					localDate = DateTime.parse(localVersionDate);
				}
				catch ( Exception e ) {
					message = "The local commands @versionDate date/time is invalid.";
					Message.printWarning(3, routine, message);
					++warningCount;
   					status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
   						message, "Verify that the local version date is valid (YYYY-MM-DD)." ) );
				}
				try {
					sourceDate = DateTime.parse(sourceVersionDate);
				}
				catch ( Exception e ) {
					message = "The source commands @versionDate date/time is invalid.";
					Message.printWarning(3, routine, message);
					++warningCount;
   					status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
   						message, "Verify that the local version date is valid (YYYY-MM-DD)." ) );
				}
				if ( (localDate != null) && (sourceDate != null) ) {
					if ( sourceDate.greaterThan(localDate) ) {
						// The source is newer than local.
						message = "The source commands (" + sourceVersionDate + ") has a newer @versionDate than local commands (" + localVersionDate + ").";
						Message.printStatus(2, routine, message);
						status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.NOTIFICATION,
							message, "Need to update the local copy from the original source." ) );
					}
					else if ( sourceDate.lessThan(localDate) ) {
						// The source is newer than local.
						message = "The source commands (" + sourceVersionDate + ") has an older @versionDate than local commands (" + localVersionDate + ").";
						Message.printStatus(2, routine, message);
						status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.NOTIFICATION,
							message, "Need to update the origional source copy from the local copy." ) );
					}
					else {
						// Version dates are the same so no need to do anything.
						message = "The source and local versions are the same based on date/time comparison.";
						Message.printStatus(2, routine, message);
					}
				}
			}
		}
   	}

   	if ( warningCount > 0 ) {
   		message = "There were " + warningCount + " warnings processing the command.";
   		Message.printWarning ( warningLevel,
   			MessageUtil.formatMessageTag(commandTag, ++warningCount), routine, message);
   		throw new CommandWarningException ( message );
   	}

   	status.refreshPhaseSeverity ( CommandPhaseType.RUN, CommandStatusType.SUCCESS );
}

/**
Return the string representation of the command.
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	// Return the entire string.
    return getCommandString();
}

}