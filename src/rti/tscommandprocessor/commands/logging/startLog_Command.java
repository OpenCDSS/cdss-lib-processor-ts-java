//------------------------------------------------------------------------------
// startLog_Command - handle the startLog() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-05-13	Steven A. Malers, RTi	Initial version (copy and modify
//					sortTimeSeries).
// 2005-05-19	SAM, RTi		Move from TSTool package.
// 2005-05-20	SAM, RTi		Add Suffix parameter.
// 2005-12-12	J. Thomas Sapienza, RTi Added check for null log file names in
//					the check parameter method so that by
//					not specifying a name, the command will
//					re-open the current log file.
// 2007-02-16	SAM, RTi		Update for new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.logging;

import java.io.File;

import javax.swing.JFrame;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
<p>
This class initializes, checks, and runs the startLog() command.
</p>
<p>The CommandProcessor must return the following properties:  WorkingDir.
</p>
*/
public class startLog_Command extends AbstractCommand implements Command
{

/**
Strings used for parameter values, here and in the editor dialog.
*/
protected final String _Date = "Date";
protected final String _DateTime = "DateTime";

/**
Constructor.
*/
public startLog_Command ()
{	super();
	setCommandName ( "startLog" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor
dialogs).
*/
public void checkCommandParameters (	PropList parameters, String command_tag,
					int warning_level )
throws InvalidCommandParameterException
{	String LogFile = parameters.getValue ( "LogFile" );
	String Suffix = parameters.getValue ( "Suffix" );
	String working_dir = null;
	
	CommandProcessor processor = getCommandProcessor();
	
	try { Object o = processor.getPropContents ( "WorkingDir" );
		// Working directory is available so use it...
		if ( o != null ) {
			working_dir = (String)o;
			if ( working_dir.equals("") ) {
				working_dir = null;	// Not available.
			}
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		String message = "Error requesting WorkingDir from processor - not using.";
		String routine = getCommandName() + "_checkCommandParameters";
		Message.printDebug(10, routine, message );
	}
	String warning = "";

	try {	
		// A null logfile means that the current log file should
		// be re-opened.
		if ( (LogFile != null) && (working_dir != null) ) {
			String adjusted_path = IOUtil.adjustPath(working_dir, 
				LogFile);
			File f = new File(adjusted_path);
			File f2 = new File(f.getParent());
			if (!f2.exists()) {
				warning += "\nThe log file parent directory "
					+ "does not exist:\n    \"" 
					+ adjusted_path + "\".";
			}
			f = null;
			f2 = null;
		}
	}
	catch ( Exception e ) {
		e.printStackTrace();
		warning +=
			"\nThe working directory:\n" +
			"    \"" + working_dir +
			"\"\ncannot be adjusted using:\n" +
			"    \"" + LogFile + "\".";
	}
	if (	(Suffix != null) && (Suffix.length() != 0) &&
		!Suffix.equalsIgnoreCase(_Date) &&
		!Suffix.equalsIgnoreCase(_DateTime) ) {
		warning +=
		"\nThe suffix must be blank, \"" + _Date + "\", or \"" +
		_DateTime + "\".";
	}
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),
		warning );
		throw new InvalidCommandParameterException ( warning );
	}
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new startLog_JDialog ( parent, this )).ok();
}

/**
Run the commands:
<pre>
startLog()
</pre>
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws CommandWarningException, CommandException
{	String routine = "startLog_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
	try {	String LogFile = parameters.getValue ( "LogFile" );
		String Suffix = parameters.getValue ( "Suffix" );
		if ( (LogFile == null) || (LogFile.length() == 0) ) {
			// Restart the current log file...
			Message.restartLogFile();
		}
		else {	// Open a new log file.  Append the suffix if it has
			// been specified.
			if ( (Suffix == null) || (Suffix.length() == 0) ) {
				// Make sure to do nothing below...
				Suffix = "";
			}
			else if ( Suffix.equalsIgnoreCase(_Date) ) {
				DateTime d = new DateTime (
					DateTime.DATE_CURRENT );
				Suffix = "." +
					StringUtil.formatString(
					d.getYear(),"%04d") +
					StringUtil.formatString(
					d.getMonth(),"%02d") +
					StringUtil.formatString(
					d.getDay(),"%02d");
			}
			else if ( Suffix.equalsIgnoreCase(_DateTime) ) {
				DateTime d = new DateTime (
					DateTime.DATE_CURRENT );
				// Make sure there is no space and can't use
				// colons here because Windows uses that for
				// drive letters...
				Suffix = "." + 
					StringUtil.formatString(
					d.getYear(),"%04d") +
					StringUtil.formatString(
					d.getMonth(),"%02d") +
					StringUtil.formatString(
					d.getDay(),"%02d") + "_" +
					StringUtil.formatString(
					d.getHour(),"%02d") +
					StringUtil.formatString(
					d.getMinute(),"%02d") +
					StringUtil.formatString(
					d.getSecond(),"%02d");
			}
			if ( Suffix.length() > 0 ) {
				String ext = IOUtil.getFileExtension (LogFile );
				if ( ext == null ) {
					// Just append...
					LogFile = LogFile + Suffix;
				}
				else {	// Insert before the last extension...
					LogFile = LogFile.substring(0,
						LogFile.length()-ext.length()-1)
						+ Suffix + "." + ext;
				}
			}
			Message.openNewLogFile (
				IOUtil.getPathUsingWorkingDir(LogFile) );
		}
	}
	catch ( Exception e ) {
		message = "Error restarting the log file.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		Message.printWarning ( 3, routine, e );
		throw new CommandException ( message );
	}
}

// Can rely on base class for toString().

}
