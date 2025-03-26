// RemoveFolder_Command - This class initializes, checks, and runs the RemoveFolder() command.

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

package rti.tscommandprocessor.commands.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
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
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the RemoveFolder() command.
*/
public class RemoveFolder_Command extends AbstractCommand
implements Command {

	/**
	Data members used for IfNotFound parameter values.
	*/
	protected final String _Ignore = "Ignore";
	protected final String _Warn = "Warn";
	protected final String _Fail = "Fail";

	/**
	 * Default delete folder minimum depth.
	 */
    protected final int _MinDepth = 3;

	/**
	Constructor.
	*/
	public RemoveFolder_Command () {
		super();
		setCommandName ( "RemoveFolder" );
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
		String Folder = parameters.getValue ( "Folder" );
		String MinDepth = parameters.getValue ( "MinDepth" );
		String IfNotFound = parameters.getValue ( "IfNotFound" );
		String warning = "";
		String message;

		CommandStatus status = getCommandStatus();
		status.clearLog(CommandPhaseType.INITIALIZATION);

		// The existence of the file to remove is not checked during initialization
		// because files may be created dynamically at runtime.

		if ( (Folder == null) || Folder.isEmpty() ) {
			message = "The folder to remove must be specified.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the folder to remove."));
		}
		if ( (MinDepth != null) && !MinDepth.equals("") && !StringUtil.isInteger(MinDepth)) {
			message = "The MinDepth parameter (" + MinDepth + ") is not an integer.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as an integer."));
		}
		if ( (IfNotFound != null) && !IfNotFound.isEmpty() ) {
			if ( !IfNotFound.equalsIgnoreCase(_Ignore) && !IfNotFound.equalsIgnoreCase(_Warn)
		    	&& !IfNotFound.equalsIgnoreCase(_Fail) ) {
				message = "The IfNotFound parameter \"" + IfNotFound + "\" is invalid.";
				warning += "\n" + message;
				status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _Ignore + ", " + _Warn + " (default), or " +
						_Fail + "."));
			}
		}
		// Check for invalid parameters.
		List<String> validList = new ArrayList<>(3);
		validList.add ( "Folder" );
		validList.add ( "MinDepth" );
		validList.add ( "IfNotFound" );
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
	@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
	*/
	public boolean editCommand ( JFrame parent ) {
		// The command will be modified if parameters are changed.
		return (new RemoveFolder_JDialog ( parent, this )).ok();
	}

	/**
	 * Determine whether a folder path has a depth at least the requested value.
	 * For example:
	 * <pre>
	 *    file.txt - folder depth 0
	 *    /file.txt - folder depth 0
	 *
	 *    folder1/file.txt - folder depth 1
	 *    /folder1/file.txt - folder depth 1
	 *
	 *    /folder1/folder2/file.txt - folder depth 2
	 *    folder1/folder2/file.txt - folder depth 2
	 *
	 *    folder1/folder2/folder3/file.txt - folder depth 3
	 *    /folder1/folder2/folder3/file.txt - folder depth 3
	 * </pre>
	 * @param key the key to evaluate
	 * @param minDepth minimum required folder depth
	 */
	public boolean folderDepthIsAtLeast ( String path, int minDepth ) {
		//String routine = getClass().getSimpleName() + ".keyFolderDepthIsAtLeast";
		if ( path == null ) {
			return false;
		}
		// Count the number of / and \ to handle mixed paths.
		// Folder delimiter character.
		String delim1 = "/";
		String delim2 = "\\";
		// Count the number of /.
		int delimCount = StringUtil.patternCount(path, delim1) + StringUtil.patternCount(path, delim2);
		// If the key did not start with /, add one to the count as if it did.
		if ( !path.startsWith(delim1) && !path.startsWith(delim2)) {
			++delimCount;
		}
		//Message.printStatus(2, routine, "Key \"" + key + "\" has delimCount=" + delimCount);
		if ( delimCount >= minDepth ) {
			return true;
		}
		else {
			return false;
		}
	}

	/**
	Run the command.
	@param command_line Command number in sequence.
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	*/
	public void runCommand ( int command_number )
	throws InvalidCommandParameterException, CommandWarningException, CommandException {
		String routine = getClass().getSimpleName() + ".runCommand", message;
		int warning_level = 2;
		String command_tag = "" + command_number;
		int warning_count = 0;

		PropList parameters = getCommandParameters();

    	CommandProcessor processor = getCommandProcessor();
		CommandStatus status = getCommandStatus();
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

		String Folder = parameters.getValue ( "Folder" ); // Expanded below.
		String MinDepth = parameters.getValue ( "MinDepth" );
		int minDepth = 3;
		if ( (MinDepth != null) && !MinDepth.isEmpty() ) {
			try {
				minDepth = Integer.parseInt(MinDepth);
			}
			catch ( NumberFormatException e ) {
				minDepth = this._MinDepth;
			}
		}
		String IfNotFound = parameters.getValue ( "IfNotFound" );
		if ( (IfNotFound == null) || IfNotFound.equals("")) {
	    	IfNotFound = _Warn; // Default.
		}

		String Folder_full = IOUtil.verifyPathForOS(
        	IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
        		TSCommandProcessorUtil.expandParameterValue(processor, this,Folder) ) );
    	File folder = new File ( Folder_full );
		if ( !folder.exists() ) {
        	message = "Folder to remove \"" + Folder_full + "\" does not exist.";
        	if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
            	Message.printWarning ( warning_level,
                	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            	status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    	message, "Verify that the folder exists at the time the command is run."));
        	}
        	else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
            	Message.printWarning ( warning_level,
                	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            	status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
                    	message, "Verify that the folder exists at the time the command is run."));
        	}
        	else {
            	Message.printStatus( 2, routine, message + "  Ignoring.");
        	}
		}
		if ( warning_count > 0 ) {
			message = "There were " + warning_count + " warnings about command parameters.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
			throw new InvalidCommandParameterException ( message );
		}
		
		// Make sure that the folder path has the required minimum number of folders.
		if ( !folderDepthIsAtLeast(Folder_full, minDepth) ) {
			// Folder does not satisfy minimum depth requirement.
           	message = "The folder \"" + Folder_full + "\" does not meet the MinDepth requirement of >= " + this._MinDepth
           		+ " folder levels - not deleting.";
           	Message.printWarning ( warning_level,
           	MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
           	status.addToLog(CommandPhaseType.RUN,
               	new CommandLogRecord(CommandStatusType.FAILURE,
                   	message, "Set the MinDepth command parameter to allow deleting."));
		}
		else if ( folder.exists() ) {
	    	try {
            	// Remove the folder.
            	IOUtil.deleteDirectory(folder);
    		}
	    	catch ( SecurityException e ) {
            	message = "Security (permissions) do not allow removing folder \"" + Folder_full + "\" (" + e + ").";
            	Message.printWarning ( warning_level,
            	MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            	Message.printWarning ( 3, routine, e );
            	status.addToLog(CommandPhaseType.RUN,
                	new CommandLogRecord(CommandStatusType.FAILURE,
                    	message, "See the log file for details."));
        	}
    		catch ( Exception e ) {
    			message = "Unexpected error removing folder \"" + Folder_full + "\" (" + e + ").";
    			Message.printWarning ( warning_level,
    			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    			Message.printWarning ( 3, routine, e );
    			status.addToLog(CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
    		}

    		// Sometimes folders and files are locked because another process is using it.
	    	// Make sure that the folder was deleted.
    		// If not, generate a failure - user will need to do something.
    		if ( folder.exists() ) {
    	    	// The folder was not removed.
            	message = "Unable to remove folder \"" + Folder_full + "\".";
            	Message.printWarning ( warning_level,
            	MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            	status.addToLog(CommandPhaseType.RUN,
                	new CommandLogRecord(CommandStatusType.FAILURE,
                    	message, "Files may be locked by another process/program."));
    		}
    		else {
    	    	Message.printStatus ( 2, routine, "Removed folder \"" + Folder_full + "\".");
    		}
		}

		// Throw CommandWarningException in case of problems.
		if ( warning_count > 0 ) {
			message = "There were " + warning_count + " warnings processing the command.";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(command_tag, ++warning_count ), routine, message );
			throw new CommandWarningException ( message );
		}
    
		status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
	}

	/**
	Return the string representation of the command.
	@param parameters to include in the command
	@return the string representation of the command
	*/
	public String toString ( PropList parameters ) {
		String [] parameterOrder = {
			"Folder",
			"MinDepth",
			"IfNotFound"
		};
		return this.toString(parameters, parameterOrder);
	}

}