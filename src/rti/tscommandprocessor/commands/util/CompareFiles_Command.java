// CompareFiles_Command - This class initializes, checks, and runs the CompareFiles() command.

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

package rti.tscommandprocessor.commands.util;

import java.io.FileReader;
import java.time.Instant;
import java.io.BufferedReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the CompareFiles() command.
*/
public class CompareFiles_Command extends AbstractCommand
implements Command
{

/**
Possible values for FileProperty parameter.
*/
protected final String _ModificationTime = "ModificationTime";
protected final String _Size = "Size";

/**
Possible values for boolean parameters.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Data members used for IfDifferent and IfSame parameters.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

/**
Data members used for WaitUntil.
*/
protected final String _FilesAreDifferent = "FilesAreDifferent";
protected final String _FilesAreSame = "FilesAreSame";
protected final String _NoWait = "NoWait";

/**
 * Temporary files used when comparing URLs.
 */
protected String tmpInputFile1 = null;
protected String tmpInputFile2 = null;

/**
Constructor.
*/
public CompareFiles_Command ()
{	super();
	setCommandName ( "CompareFiles" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String InputFile1 = parameters.getValue ( "InputFile1" );
	String InputFile2 = parameters.getValue ( "InputFile2" );
	String MatchCase = parameters.getValue ( "MatchCase" );
	String IgnoreWhitespace = parameters.getValue ( "IgnoreWhitespace" );
	String AllowedDiff = parameters.getValue ( "AllowedDiff" );
	String IfDifferent = parameters.getValue ( "IfDifferent" );
	String IfSame = parameters.getValue ( "IfSame" );
	String FileProperty = parameters.getValue ( "FileProperty" );
	String FilePropertyOperator = parameters.getValue ( "FilePropertyOperator" );
	String FilePropertyAction = parameters.getValue ( "FilePropertyAction" );
	String WaitUntil = parameters.getValue ( "WaitUntil" );
	String WaitTimeout = parameters.getValue ( "WaitTimeout" );
	String WaitInterval = parameters.getValue ( "WaitInterval" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the parent directories or files is not checked
	// because the files may be created dynamically after the command is edited.

	if ( (InputFile1 == null) || (InputFile1.length() == 0) ) {
		message = "The first input file to compare must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the first file name."));
	}
	if ( (InputFile2 == null) || (InputFile2.length() == 0) ) {
		message = "The second input file to compare must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the second file name."));
	}
    if ( (MatchCase != null) && !MatchCase.equals("") && !MatchCase.equalsIgnoreCase(_False) &&
        !MatchCase.equalsIgnoreCase(_True) ) {
        message = "The MatchCase parameter \"" + MatchCase + "\" is not a valid value.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the parameter as " + _False + " or " + _True + " (default)"));
    }
	if ( (IgnoreWhitespace != null) && !IgnoreWhitespace.equals("") && !IgnoreWhitespace.equalsIgnoreCase(_False) &&
		!IgnoreWhitespace.equalsIgnoreCase(_True) ) {
		message = "The IgnoreWhitespace parameter \"" + IgnoreWhitespace + "\" is not a valid value.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the parameter as " + _False + " (default) or " + _True ));
	}
    if ( (AllowedDiff != null) && !AllowedDiff.equals("") && !StringUtil.isInteger(AllowedDiff) ) {
            message = "The number of allowed differences \"" + AllowedDiff + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                     message, "Specify the parameter as an integer."));
    }
	if ( (IfDifferent != null) && !IfDifferent.equals("") && !IfDifferent.equalsIgnoreCase(_Ignore) &&
		!IfDifferent.equalsIgnoreCase(_Warn) && !IfDifferent.equalsIgnoreCase(_Fail) ) {
			message = "The IfDifferent parameter \"" + IfDifferent + "\" is not a valid value.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _Ignore + " (default), " +
						_Warn + ", or " + _Fail + "."));
	}
	if ( (IfSame != null) && !IfSame.equals("") && !IfSame.equalsIgnoreCase(_Ignore) &&
		!IfSame.equalsIgnoreCase(_Warn) && !IfSame.equalsIgnoreCase(_Fail) ) {
		message = "The IfSame parameter \"" + IfSame + "\" is not a valid value.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Ignore + " (default), " +
					_Warn + ", or " + _Fail + "."));
	}
	int sameCount = 0;
	int diffCount = 0;
	if ( (IfSame != null) && (IfSame.equalsIgnoreCase(_Warn) || IfSame.equalsIgnoreCase(_Fail)) ) {
		++sameCount;
	}
	if ( (IfDifferent != null) && (IfDifferent.equalsIgnoreCase(_Warn) || IfDifferent.equalsIgnoreCase(_Fail)) ) {
		++diffCount;
	}
	if ( ((FileProperty == null) || FileProperty.isEmpty()) && (sameCount + diffCount) == 0 ) {
        message = "At least one of IfDifferent or IfSame must be " + _Warn + " or " + _Fail + ".";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Check the values of IfDifferent and IfSame." ) );
	}
	if ( ((FileProperty == null) || FileProperty.isEmpty()) && (sameCount + diffCount) > 1 ) {
        message = "Only one of IfDifferent or IfSame can be " + _Warn + " or + " + _Fail + ".";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Check the values of IfDifferent and IfSame." ) );
	}
	if ( (FileProperty != null) && !FileProperty.equals("") ) {
		if ( !FileProperty.equalsIgnoreCase(_ModificationTime) && !FileProperty.equalsIgnoreCase(_Size) ) {
			message = "The FileProperty parameter \"" + FileProperty + "\" is not a valid value.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _ModificationTime + 
						", or " + _Size + "."));
		}
		// Also check the operator.
		if ( (FilePropertyOperator != null) && !FilePropertyOperator.equals("") &&
			!FilePropertyOperator.equalsIgnoreCase("<") &&
			!FilePropertyOperator.equalsIgnoreCase("<=") &&
			!FilePropertyOperator.equalsIgnoreCase("=") &&
			!FilePropertyOperator.equalsIgnoreCase(">") &&
			!FilePropertyOperator.equalsIgnoreCase(">=") &&
			!FilePropertyOperator.equalsIgnoreCase("!=") ) {
			message = "The FilePropertyOperator parameter \"" + FilePropertyOperator + "\" is not a valid value.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as >, >=, =, <, <=, or !="));
		}
		if ( (FilePropertyAction != null) && !FilePropertyAction.equals("") &&
			!FilePropertyAction.equalsIgnoreCase(_Warn) && !FilePropertyAction.equalsIgnoreCase(_Fail) ) {
			message = "The FilePropertyAction parameter \"" + FilePropertyAction + "\" is not a valid value.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Warn + " (default) " +
					" or " + _Fail + "."));
		}
	}

	if ( (WaitUntil != null) && !WaitUntil.equals("") && !WaitUntil.equalsIgnoreCase(_FilesAreDifferent) &&
		!WaitUntil.equalsIgnoreCase(_FilesAreSame) && !WaitUntil.equalsIgnoreCase(_NoWait) ) {
			message = "The WaitUntil parameter \"" + WaitUntil + "\" is not a valid value.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _FilesAreDifferent + ", " +
					_FilesAreSame + ", or " + _NoWait + " (default)."));
	}
    if ( (WaitTimeout != null) && !WaitTimeout.equals("") && !StringUtil.isInteger(WaitTimeout) ) {
            message = "The wait timeout \"" + WaitTimeout + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                     message, "Specify the parameter as an integer number of milliseconds."));
    }
    if ( (WaitInterval != null) && !WaitInterval.equals("") && !StringUtil.isInteger(WaitInterval) ) {
            message = "The wait interval \"" + WaitInterval + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                     message, "Specify the parameter as an integer number of milliseconds."));
    }

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(15);
	validList.add ( "InputFile1" );
	validList.add ( "InputFile2" );
	validList.add ( "CommentLineChar" );
	validList.add ( "MatchCase" );
	validList.add ( "IgnoreWhitespace" );
	validList.add ( "ExcludeText" );
	validList.add ( "AllowedDiff" );
	validList.add ( "IfDifferent" );
	validList.add ( "IfSame" );
	validList.add ( "FileProperty" );
	validList.add ( "FilePropertyOperator" );
	validList.add ( "FilePropertyAction" );
	validList.add ( "WaitUntil" );
	validList.add ( "WaitTimeout" );
	validList.add ( "WaitInterval" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
 * Compare a file property.
 * @param inputFile1 path to first file to compare
 * @param inputFile1Full full path to first file to compare
 * @param inputFile2 path to second file to compare
 * @param inputFile2Full full path to second file to compare
 * @param fileProperty property to compare
 * @param filePropertyOperator operator to use when comparing property
 * @return the number of warnings generated in the method
 */
private int compareFileProperty(String inputFile1, String inputFile1Full,
	String inputFile2, String inputFile2Full, String fileProperty, String filePropertyOperator,
	CommandStatusType FilePropertyAction_CommandStatusType,
	CommandStatus status, int warningLevel, String commandTag ) {
	String routine = getClass().getSimpleName() + ".compareFileProperty";
	File file1 = new File(inputFile1Full);
	File file2 = new File(inputFile2Full);
	long value1 = 0, value2 = 0;
	String message;
	int warningCount = 0;
	// Get the values to compare.
	if ( fileProperty.equalsIgnoreCase(this._ModificationTime) ) {
		value1 = file1.lastModified();
		value2 = file2.lastModified();
		//Message.printStatus(2, routine, inputFile1 + " modification time is " + value1);
		//Message.printStatus(2, routine, inputFile2 + " modification time is " + value2);
	}
	else if ( fileProperty.equalsIgnoreCase(this._Size) ) {
		value1 = file1.length();
		value2 = file2.length();
		//Message.printStatus(2, routine, inputFile1 + " size is " + value1);
		//Message.printStatus(2, routine, inputFile2 + " size is " + value2);
	}
	else {
		// Error - property unknown.
		message = "Property property " + fileProperty + " is not recognized.";
		Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag(
			commandTag,++warningCount), routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Verify that the property being compared is valid."));
	}
	// Do the comparison based on the operator.
	boolean conditionMet = false;
	if ( filePropertyOperator.equals("<") ) {
		if ( value1 < value2 ) {
			conditionMet = true;
		}
	}
	else if ( filePropertyOperator.equals("<=") ) {
		if ( value1 <= value2 ) {
			conditionMet = true;
		}
	}
	else if ( filePropertyOperator.equals("=") || filePropertyOperator.equals("==") ) {
		if ( value1 == value2 ) {
			conditionMet = true;
		}
	}
	else if ( filePropertyOperator.equals(">") ) {
		if ( value1 > value2 ) {
			conditionMet = true;
		}
	}
	else if ( filePropertyOperator.equals(">=") ) {
		if ( value1 >= value2 ) {
			conditionMet = true;
		}
	}
	else if ( filePropertyOperator.equals("!=") ) {
		if ( value1 >= value2 ) {
			conditionMet = true;
		}
	}
	else {
		// Error - operator unknown.
		message = "Property operator " + filePropertyOperator + " is not recognized.";
		Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag(
			commandTag,++warningCount), routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Verify that the operator is valid."));
	}
	if ( conditionMet && ((FilePropertyAction_CommandStatusType == CommandStatusType.WARNING) ||
		(FilePropertyAction_CommandStatusType == CommandStatusType.FAILURE))) {
		message = inputFile1 + " property " + fileProperty + " " + filePropertyOperator + " " + inputFile2;
		Message.printWarning ( warningLevel,
		MessageUtil.formatMessageTag( commandTag,++warningCount),
		routine, message );
		status.addToLog(CommandPhaseType.RUN,
			new CommandLogRecord(FilePropertyAction_CommandStatusType,
				message, "") );
	}
	return warningCount;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if edits are committed.
	String routine = getClass().getSimpleName() + ".editCommand";
	String diffProgram = null;
	Prop prop = IOUtil.getProp("DiffProgram");
	// Get the initial program to use for visual difference comparison.
	if ( prop != null ) {
		diffProgram = prop.getValue();
		if ( Message.isDebugOn ) {
			Message.printStatus(2, routine, "Difference program from DiffProgram=\"" + diffProgram + "\"");
		}
	}
	// If defined, use the property for the operating system.
	if ( IOUtil.isUNIXMachine() ) {
		if ( Message.isDebugOn ) {
			Message.printStatus(2, routine, "Is UNIX machine." );
		}
		prop = IOUtil.getProp("DiffProgram.Linux");
		if ( prop != null ) {
			diffProgram = prop.getValue();
			if ( Message.isDebugOn ) {
				Message.printStatus(2, routine, "Difference program from DiffProgram.Linux=\"" + diffProgram + "\"");
			}
		}
		else {
			if ( Message.isDebugOn ) {
				Message.printStatus(2, routine, "No program DiffProgram.Linux=\"" + diffProgram + "\"");
			}
		}
	}
	else {
		if ( Message.isDebugOn ) {
			Message.printStatus(2, routine, "Is Windows machine." );
		}
		prop = IOUtil.getProp("DiffProgram.Windows");
		if ( prop != null ) {
			diffProgram = prop.getValue();
			if ( prop != null ) {
				diffProgram = prop.getValue();
				if ( Message.isDebugOn ) {
					Message.printStatus(2, routine, "Difference program from DiffProgram.Windows=\"" + diffProgram + "\"");
				}
			}
		}
	}
	return (new CompareFiles_JDialog ( parent, this, diffProgram )).ok();
}

/**
 * Return the temporary input file if the first file is a URL.
 */
public String getTmpInputFile1 () {
	return this.tmpInputFile1;
}

/**
 * Return the temporary input file if the second file is a URL.
 */
public String getTmpInputFile2 () {
	return this.tmpInputFile2;
}

/**
Parse the command string into a PropList of parameters.
Can't use base class method because of change in parameter names.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "CompareFiles_Command.parseCommand", message;

	List<String> tokens = StringUtil.breakStringList ( command, "()", StringUtil.DELIM_SKIP_BLANKS );

	CommandStatus status = getCommandStatus();
	if ( (tokens == null) ) { //|| tokens.size() < 2 ) {}
		message = "Invalid syntax for \"" + command + "\".  Expecting CompareFiles(...).";
		Message.printWarning ( warning_level, routine, message);
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Report the problem to support."));
		throw new InvalidCommandSyntaxException ( message );
	}
	// Get the input needed to process the command.
	if ( tokens.size() > 1 ) {
		try {
		    setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT, tokens.get(1), routine,"," ) );
		}
		catch ( Exception e ) {
			message = "Invalid syntax for \"" + command + "\".  Expecting CompareFiles(...).";
			Message.printWarning ( warning_level, routine, message);
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report the problem to support."));
			throw new InvalidCommandSyntaxException ( message );
		}
	}
	// Update old parameter names to new:
	// - change WarnIfDifferent=True to IfDifferent=Warn
	// - change WarnIfSame=True to IfSame=Warn
	PropList props = getCommandParameters();
	String propValue = props.getValue ( "WarnIfDifferent" );
	if ( propValue != null ) {
		if ( propValue.equalsIgnoreCase(_True) ) {
			props.set("IfDifferent",_Warn);
		}
		props.unSet("WarnIfDifferent");
	}
	propValue = props.getValue ( "WarnIfSame" );
	if ( propValue != null ) {
		if ( propValue.equalsIgnoreCase(_True) ) {
			props.set("IfSame",_Warn);
		}
		props.unSet("WarnIfSame");
	}
}

/**
Read a line from a file.  Skip over comments until the next non-comment line is found.
Also skip lines that contain matching excludeText.
@param in BufferedReader for open file to read.
@param CommentLineChar character at start of line that indicates comment line.
@param ignoreWhitespace if true, trim the lines.
@param excludeText array of Java regular expressions - if matched, skip the line.
@return the next line from the file, or null if at the end.
*/
private String readLine ( BufferedReader in, String CommentLineChar, boolean ignoreWhitespace, String [] excludeText )
{	String iline;
	int commentCount = 0;
	int excludeCount = 0;
	while ( true ) {
		// Read until a non-comment line is found.
		try {
		    iline = in.readLine ();
		}
		catch ( Exception e ) {
			return null;
		}
		if ( iline == null ) {
			return null;
		}
		// Check for comments.
		else if ( (iline.length() > 0) && (CommentLineChar.indexOf(iline.charAt(0)) >= 0) ) {
			++commentCount;
			continue;
		}
		else {
			// Loop through to see if any excluded text matches.
			boolean exclude = false;
			for ( int iExclude = 0; iExclude < excludeText.length; iExclude++ ) {
				if ( iline.matches(excludeText[iExclude]) ) {
					++excludeCount;
					exclude = true;
					break;
				}
			}
			if ( exclude ) {
				continue;
			}
			if ( Message.isDebugOn ) {
				Message.printDebug (1, "", "Skipped " + commentCount + " comments and " +
					excludeCount + " excluded patterns before getting to data line" );
			}
			if ( ignoreWhitespace ) {
				return iline.trim();
			}
			else {
				return iline;
			}
		}
	}
}

/**
Run the command.
@param command_line Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int dl = 1;
	
	PropList parameters = getCommandParameters();
	
    CommandProcessor processor = getCommandProcessor();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
	CommandStatus status = getCommandStatus();
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
		status.clearLog(commandPhase);
	}

    // Reset temporary files to null so can check for null in the editor.
	this.tmpInputFile1 = null;
	this.tmpInputFile2 = null;
	
	String InputFile1 = parameters.getValue ( "InputFile1" );
	String InputFile2 = parameters.getValue ( "InputFile2" );
	String CommentLineChar = parameters.getValue ( "CommentLineChar" );
	String MatchCase = parameters.getValue ( "MatchCase" );
    boolean MatchCase_boolean = true; // Default.
    if ( (MatchCase != null) && MatchCase.equalsIgnoreCase(_False)) {
        MatchCase_boolean = false;
    }
    String IgnoreWhitespace = parameters.getValue ( "IgnoreWhitespace" );
	boolean IgnoreWhitespace_boolean = false; // Default.
	if ( (IgnoreWhitespace != null) && IgnoreWhitespace.equalsIgnoreCase(_True)) {
		IgnoreWhitespace_boolean = true;
	}
    String ExcludeText = parameters.getValue ( "ExcludeText" );
    String [] excludeText = {}; // List of regular expressions, glob-style.
	if ( (ExcludeText != null) && !ExcludeText.isEmpty()) {
		// Split by comma first.
		excludeText = ExcludeText.split(",");
		// Convert to Java regular expressions.
		for ( int i = 0; i < excludeText.length; i++ ) {
			excludeText[i] = excludeText[i].replace("*", ".*");
		}
	}
	String AllowedDiff = parameters.getValue ( "AllowedDiff" );
	int AllowedDiff_int = 0;
	if ( StringUtil.isInteger(AllowedDiff) ) {
	    AllowedDiff_int = Integer.parseInt(AllowedDiff);
	}
	if ( (CommentLineChar == null) || CommentLineChar.equals("") ) {
	    CommentLineChar = "#";
	}
	boolean doCompareContent = false; // Whether to compare file content.
	String IfDifferent = parameters.getValue ( "IfDifferent" );
	CommandStatusType IfDifferent_CommandStatusType = CommandStatusType.UNKNOWN;
	if ( IfDifferent == null ) {
		IfDifferent = _Ignore; // Default.
	}
	else {
		if ( !IfDifferent.equalsIgnoreCase(_Ignore) ) {
			IfDifferent_CommandStatusType = CommandStatusType.parse(IfDifferent);
			doCompareContent = true;
		}
	}
	String IfSame = parameters.getValue ( "IfSame" );
	CommandStatusType IfSame_CommandStatusType = CommandStatusType.UNKNOWN;
	if ( IfSame == null ) {
		IfSame = _Ignore; // Default.
	}
	else {
		if ( !IfSame.equalsIgnoreCase(_Ignore) ) {
			IfSame_CommandStatusType = CommandStatusType.parse(IfSame);
			doCompareContent = true;
		}
	}
	String FileProperty = parameters.getValue ( "FileProperty" );
	String FilePropertyOperator = parameters.getValue ( "FilePropertyOperator" );
	String FilePropertyAction = parameters.getValue ( "FilePropertyAction" );
	CommandStatusType FilePropertyAction_CommandStatusType = CommandStatusType.UNKNOWN;
	if ( FilePropertyAction == null ) {
		IfSame = _Warn; // Default.
	}
	else {
		FilePropertyAction_CommandStatusType = CommandStatusType.parse(FilePropertyAction);
	}
	String WaitUntil = parameters.getValue ( "WaitUntil" );
	boolean doWaitUntil = false; // Used to simplify logic below.
	if ( (WaitUntil == null) || WaitUntil.isEmpty() ) {
		WaitUntil = this._NoWait;
	}
	if ( !WaitUntil.equals(this._NoWait) ) {
		doWaitUntil = true;
	}
	String WaitTimeout = parameters.getValue ( "WaitTimeout" );
	int waitTimeout = 1000;
	if ( StringUtil.isInteger(WaitTimeout) ) {
	    waitTimeout = Integer.parseInt(WaitTimeout);
	}
	String WaitInterval = parameters.getValue ( "WaitInterval" );
	int waitInterval = 1000;
	if ( StringUtil.isInteger(WaitInterval) ) {
	    waitInterval = Integer.parseInt(WaitInterval);
	}

	String InputFile1_full = InputFile1;
	boolean inputFile1IsUrl = StringUtil.isUrl(InputFile1);
	if ( inputFile1IsUrl ) {
		// Expand for properties but don't adjust the path.
		InputFile1_full = TSCommandProcessorUtil.expandParameterValue(processor, this, InputFile1);
	}
	else {
		// Local file so check the path.
		InputFile1_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor, this, InputFile1) ) );
	}
	String InputFile2_full = InputFile2;
	boolean inputFile2IsUrl = StringUtil.isUrl(InputFile2);
	if ( inputFile2IsUrl ) {
		// Expand for properties but don't adjust the path.
		InputFile2_full = TSCommandProcessorUtil.expandParameterValue(processor, this, InputFile2);
	}
	else {
		// Local file so check the path.
		InputFile2_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor, this, InputFile2) ) );
	}
	if ( !inputFile1IsUrl && !IOUtil.fileExists(InputFile1_full) ) {
		// Not a URL and file does not exist.
		message = "First input file \"" + InputFile1_full + "\" does not exist.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Verify that the file exists at the time the command is run."));
	}
	if ( !inputFile2IsUrl && !IOUtil.fileExists(InputFile2_full) ) {
		// Not a URL and file does not exist.
		message = "Second input file \"" + InputFile2_full + "\" does not exist.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Verify that the file exists at the time the command is run."));
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	// If input is a URL, download to a temporary file and use that for the comparison:
	// - reuse the filename so that only one or two files are created
	// - use the command number and time to uniquely identify the files
	Instant now = Instant.now();
	String tmpFolder = System.getProperty("java.io.tmpdir");
	if ( !(tmpFolder.charAt(tmpFolder.length() - 1) == File.separatorChar) ) {
		tmpFolder = tmpFolder + File.separatorChar;
	}
	if ( inputFile1IsUrl ) {
		this.tmpInputFile1 = tmpFolder + "CompareFiles-InputFile1-" +
			command_number + "." + now.getEpochSecond() + "." + now.getNano();
		Message.printStatus(2, routine, "Temporary file for InputFile1 URL is: " + this.tmpInputFile1);
	}
	if ( inputFile2IsUrl ) {
		this.tmpInputFile2 = tmpFolder + "CompareFiles-InputFile2-" +
			command_number + "." + now.getEpochSecond() + "." + now.getNano();
		Message.printStatus(2, routine, "Temporary file for InputFile2 URL is: " + this.tmpInputFile2);
	}

	// Used in messages so have to define outside the loop:
	// - reset to zero below
	int lineCountCompared = 0;
	int diffCount = 0; // Number of lines that are different.

	try {
		// Loop once if no wait is occurring or until the wait timeout has been exceeded:
		// - only allow a single exception, not each loop
		// - a check is done at the end to break out if wait is not used
		int waitTotal = 0;
		while ( waitTotal < waitTimeout ) {
			// Initialize for the loop.
			lineCountCompared = 0;
			diffCount = 0;
			// If URLs are used as input, download the files to temporary names.
			if ( inputFile1IsUrl ) {
				int code = IOUtil.getUriContent(InputFile1_full, this.tmpInputFile1, null);
				if ( code != 200 ) {
					message = "Error downloading: " + InputFile1_full;
					Message.printWarning ( warning_level, 
						MessageUtil.formatMessageTag(command_tag, ++warning_count),
						routine, message );
					status.addToLog(CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Verify that the URL is valid and the resource exists."
								+ "  Note that a web browser may cache the file and therefore may not be an accurate reflection of whether the file exists on a server."));
					// Break out of loop since can't compare.
					break;
				}
			}
			if ( inputFile2IsUrl ) {
				int code = IOUtil.getUriContent(InputFile2_full, this.tmpInputFile2, null);
				if ( code != 200 ) {
					message = "Error downloading: " + InputFile2_full;
					Message.printWarning ( warning_level, 
						MessageUtil.formatMessageTag(command_tag, ++warning_count),
						routine, message );
					status.addToLog(CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Verify that the URL is valid and the resource exists."
								+ "  Note that a web browser may cache the file and therefore may not be an accurate reflection of whether the file exists on a server."));
					// Break out of loop since can't compare.
					break;
				}
			}
			if ( doCompareContent ) {
				// Compare the file content.

				// Open the files:
				// - allow missing files if using WaitUntil
				BufferedReader in1 = null;
				BufferedReader in2 = null;
				// Use the following to indicate end of file reached and also no file.
				boolean file1EndReached = false;
				boolean file2EndReached = false;
				if ( doWaitUntil ) {
					// Allow missing file:
					// - for example upload or other processing needs to complete
					// - for missing file, set as if end of file has been reached
					if ( inputFile1IsUrl ) {
						try {
							in1 = new BufferedReader(new FileReader(IOUtil.getPathUsingWorkingDir(this.tmpInputFile1)));
						}
						catch ( Exception e ) {
							file1EndReached = true;
						}
					}
					else {
						try {
							in1 = new BufferedReader(new FileReader(IOUtil.getPathUsingWorkingDir(InputFile1_full)));
						}
						catch ( Exception e ) {
							file1EndReached = true;
						}
					}
					if ( inputFile2IsUrl ) {
						try {
							in2 = new BufferedReader(new FileReader(IOUtil.getPathUsingWorkingDir(this.tmpInputFile2)));
						}
						catch ( Exception e ) {
							file2EndReached = true;
						}
					}
					else {
						try {
							in2 = new BufferedReader(new FileReader(IOUtil.getPathUsingWorkingDir(InputFile2_full)));
						}
						catch ( Exception e ) {
							file2EndReached = true;
						}
					}
				}
				else {
					// No missing files allowed so allow exception to stop processing.
					if ( inputFile1IsUrl ) {
						in1 = new BufferedReader(new FileReader(IOUtil.getPathUsingWorkingDir(this.tmpInputFile1)));
					}
					else {
						in1 = new BufferedReader(new FileReader(IOUtil.getPathUsingWorkingDir(InputFile1_full)));
					}
					if ( inputFile2IsUrl ) {
						in2 = new BufferedReader(new FileReader(IOUtil.getPathUsingWorkingDir(this.tmpInputFile2)));
					}
					else {
						in2 = new BufferedReader(new FileReader(IOUtil.getPathUsingWorkingDir(InputFile2_full)));
					}
				}
				// Loop through the files, comparing non-comment lines.
				String iline1 = null, iline2 = null;
				while ( true ) {
					// The following will discard comments and only return non-comment lines.
					// Therefore comparisons are made on chunks of non-comment lines.
					if ( !file1EndReached ) {
						iline1 = readLine ( in1, CommentLineChar, IgnoreWhitespace_boolean, excludeText );
					}
					if ( !file2EndReached ) {
						iline2 = readLine ( in2, CommentLineChar, IgnoreWhitespace_boolean, excludeText );
					}
					if ( (iline1 == null) && (iline2 == null) ) {
						// Reached the end of both files at the same time.
						break;
					}
					// Increment the line counter now.
					++lineCountCompared;
					// TODO SAM 2006-04-20 The following needs to handle comments at the end.
					if ( (iline1 == null) && (iline2 != null) ) {
						// First file is done (second is not) so files are different:
						// - increment the count because a line exists in one file but not the other
						file1EndReached = true;
						++diffCount;
					}
					else if ( (iline2 == null) && (iline1 != null) ) {
						// Second file is done (first is not) so files are different:
						// - increment the count because a line exists in one file but not the other
						file2EndReached = true;
						++diffCount;
					}
					else if ( (iline1 != null) && (iline2 != null) ) {
						// Have lines from each file to compare.
						if ( MatchCase_boolean ) {
    						if ( !iline1.equals(iline2) ) {
    							++diffCount;
    						}
						}
						else {
			    			if ( !iline1.equalsIgnoreCase(iline2) ) {
                    			++diffCount;
                			}
						}
						if ( Message.isDebugOn ) {
							Message.printDebug (dl,routine,"Compared:\n\"" + iline1 + "\"\n\"" + iline2 + "\"\nDiffCount=" +
									diffCount );
						}
					}
				}
				// Comparison is done so close the files.
				in1.close();
				in2.close();
				if ( lineCountCompared == 0 ) {
					// Likely because both files are empty.
					double diffPercent = 0.0;
					Message.printStatus ( 2, routine, "There are " + diffCount + " lines that are different, " +
						StringUtil.formatString(diffPercent, "%.2f") + "% (compared " + lineCountCompared + " lines).  Files are both empty or all comments?");
				}
				else {
					Message.printStatus ( 2, routine, "There are " + diffCount + " lines that are different, " +
						StringUtil.formatString(100.0*(double)diffCount/(double)lineCountCompared, "%.2f") +
						"% (compared " + lineCountCompared + " lines).");
				}
			}
			else if ( (FileProperty != null) && !FileProperty.isEmpty() ) {
				warning_level += compareFileProperty(InputFile1, InputFile1_full,
					InputFile2, InputFile2_full, FileProperty, FilePropertyOperator,
					FilePropertyAction_CommandStatusType, status, warning_level, command_tag);
			}
			
			// Check for conditions to exit the "WaitUntil" while loop.
			
			if ( !doWaitUntil ) {
				// Not waiting so exit the loop:
				// - this is the default if no WaitUntil parameter is specified
				break;
			}
			else {
				// Check to see if the wait condition is satisfied.
				if ( WaitUntil.equalsIgnoreCase(this._FilesAreDifferent) && (diffCount > 0) ) {
					// Detected difference.
					Message.printStatus ( 2, routine, "WaitUntil=" + WaitUntil + ", detected different files.  Stop waiting after " + waitTotal + " ms.");
					break;
				}
				else if ( WaitUntil.equalsIgnoreCase(this._FilesAreSame) && (diffCount == 0) ) {
					// Detected same.
					Message.printStatus ( 2, routine, "WaitUntil=" + WaitUntil + ", detected same files.  Stop waiting after " + waitTotal + " ms.");
					break;
				}
				else {
					// Check to see if the processor needs to break.
					if ( processor instanceof TSCommandProcessor ) {
						TSCommandProcessor tp = (TSCommandProcessor)processor;
						if ( tp.getCancelProcessingRequested() ) {
							// Request from the main interface to stop processing.
							Message.printStatus ( 2, routine, "WaitUntil=" + WaitUntil + ", cancel processing was requested at time " + waitTotal + "ms.");
							break;
						}
					}
					// Increment the wait total and compare again.
					Message.printStatus ( 2, routine, "WaitUntil=" + WaitUntil + ", waiting " + waitInterval + " ms.");
					Thread.sleep(waitInterval);
					waitTotal += waitInterval;
					Message.printStatus ( 2, routine, "WaitUntil=" + WaitUntil + ", waited " + waitInterval + " ms, now at " + waitTotal + " total wait of " + waitTimeout + " timeout.");
				}
			}
		} // End of wait loop.

		// Done looping so report on the last iteration's result below.
	}
	catch ( Exception e ) {
		message = "Unexpected error comparing files (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
		throw new CommandException ( message );
	}

	if ( lineCountCompared == 0 ) {
		// Likely because both files are empty.
		double diffPercent = 0.0;
		message = "" + diffCount + " lines were different, " +
			StringUtil.formatString(diffPercent, "%.2f") + "% (compared " + lineCountCompared + " lines).  Files are both empty or all comments?";
		Message.printStatus ( 2, routine, message );
	}
	if ( (diffCount > AllowedDiff_int) && ((IfDifferent_CommandStatusType == CommandStatusType.WARNING) ||
		(IfDifferent_CommandStatusType == CommandStatusType.FAILURE)) ) {
		double diffPercent = 100.0*(double)diffCount/(double)lineCountCompared;
		message = "" + diffCount + " lines were different, " +
			StringUtil.formatString(diffPercent, "%.2f") + "% (compared " + lineCountCompared + " lines).";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag( command_tag,++warning_count),
		routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(IfDifferent_CommandStatusType,
					message, "Check files because difference is not expected.") );
		throw new CommandException ( message );
	}
	if ( (diffCount == 0) && ((IfSame_CommandStatusType == CommandStatusType.WARNING) ||
			(IfSame_CommandStatusType == CommandStatusType.FAILURE))) {
		message = "No lines were different (the files are the same).";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag( command_tag,++warning_count),
		routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(IfSame_CommandStatusType,
					message, "Check files because match is not expected.") );
		throw new CommandException ( message );
	}
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String InputFile1 = parameters.getValue("InputFile1");
	String InputFile2 = parameters.getValue("InputFile2");
	String CommentLineChar = parameters.getValue("CommentLineChar");
	String MatchCase = parameters.getValue("MatchCase");
	String IgnoreWhitespace = parameters.getValue("IgnoreWhitespace");
	String ExcludeText = parameters.getValue("ExcludeText");
	String AllowedDiff = parameters.getValue("AllowedDiff");
	String IfDifferent = parameters.getValue("IfDifferent");
	String IfSame = parameters.getValue("IfSame");
	String FileProperty = parameters.getValue("FileProperty");
	String FilePropertyOperator = parameters.getValue("FilePropertyOperator");
	String FilePropertyAction = parameters.getValue("FilePropertyAction");
	String WaitUntil = parameters.getValue("WaitUntil");
	String WaitTimeout = parameters.getValue("WaitTimeout");
	String WaitInterval = parameters.getValue("WaitInterval");
	StringBuffer b = new StringBuffer ();
	if ( (InputFile1 != null) && (InputFile1.length() > 0) ) {
		b.append ( "InputFile1=\"" + InputFile1 + "\"" );
	}
	if ( (InputFile2 != null) && (InputFile2.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile2=\"" + InputFile2 + "\"" );
	}
    if ( (CommentLineChar != null) && (CommentLineChar.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CommentLineChar=\"" + CommentLineChar + "\"" );
    }
    if ( (MatchCase != null) && (MatchCase.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MatchCase=" + MatchCase );
    }
    if ( (IgnoreWhitespace != null) && (IgnoreWhitespace.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IgnoreWhitespace=" + IgnoreWhitespace );
    }
    if ( (ExcludeText != null) && (ExcludeText.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExcludeText=\"" + ExcludeText + "\"" );
    }
    if ( (AllowedDiff != null) && (AllowedDiff.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AllowedDiff=\"" + AllowedDiff + "\"" );
    }
	if ( (IfDifferent != null) && (IfDifferent.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfDifferent=" + IfDifferent );
	}
	if ( (IfSame != null) && (IfSame.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfSame=" + IfSame );
	}
	if ( (FileProperty != null) && (FileProperty.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FileProperty=" + FileProperty );
	}
	if ( (FilePropertyOperator != null) && (FilePropertyOperator.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FilePropertyOperator=\"" + FilePropertyOperator + "\"" );
	}
	if ( (FilePropertyAction != null) && (FilePropertyAction.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FilePropertyAction=" + FilePropertyAction );
	}
	if ( (WaitUntil != null) && (WaitUntil.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "WaitUntil=" + WaitUntil );
	}
	if ( (WaitTimeout != null) && (WaitTimeout.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "WaitTimeout=" + WaitTimeout );
	}
	if ( (WaitInterval != null) && (WaitInterval.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "WaitInterval=" + WaitInterval );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}