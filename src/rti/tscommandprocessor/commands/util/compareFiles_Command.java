//------------------------------------------------------------------------------
// compareFiles_Command - handle the compareFiles() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2006-04-19	Steven A. Malers, RTi	Initial version.  Copy and modify
//					compareTimeSeries_Command.
// 2006-05-03	SAM, RTi		Update to include WarnIfSame parameter.
// 2007-03-02	SAM, RTi		Allow booleans to be any case.
// 2007-05-08	SAM, RTi		Cleanup code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.util;

import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Vector;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the compareFiles() command.
*/
public class compareFiles_Command extends AbstractCommand
implements Command
{

/**
Data members used for parameter values.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Constructor.
*/
public compareFiles_Command ()
{	super();
	setCommandName ( "CompareFiles" );
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
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String InputFile1 = parameters.getValue ( "InputFile1" );
	String InputFile2 = parameters.getValue ( "InputFile2" );
	String WarnIfDifferent = parameters.getValue ( "WarnIfDifferent" );
	String WarnIfSame = parameters.getValue ( "WarnIfSame" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the parent directories or files is not checked
	// because the files may be created dynamically after the command is
	// edited.

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
	if ( (WarnIfDifferent != null) && !WarnIfDifferent.equals("") ) {
		if (	!WarnIfDifferent.equalsIgnoreCase(_False) &&
			!WarnIfDifferent.equalsIgnoreCase(_True) ) {
			message = "The WarnIfDifferent parameter \"" + WarnIfDifferent + "\" must be False or True.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as False or True."));
		}
	}
	if ( (WarnIfSame != null) && !WarnIfSame.equals("") ) {
		if (	!WarnIfSame.equalsIgnoreCase(_False) &&
			!WarnIfSame.equalsIgnoreCase(_True) ) {
			message = "The WarnIfSame parameter \"" + WarnIfSame + "\" must be False or True.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message,"Specify the parameter as False or True."));
		}
	}
	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
	valid_Vector.add ( "InputFile1" );
	valid_Vector.add ( "InputFile2" );
	valid_Vector.add ( "WarnIfDifferent" );
	valid_Vector.add ( "WarnIfSame" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

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
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new compareFiles_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "CompareFiles_Command.parseCommand", message;

	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );

	CommandStatus status = getCommandStatus();
	if ( (tokens == null) ) { //|| tokens.size() < 2 ) {}
		message = "Invalid syntax for \"" + command + "\".  Expecting CompareFiles(...).";
		Message.printWarning ( warning_level, routine, message);
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Report the problem to support."));
		throw new InvalidCommandSyntaxException ( message );
	}
	// Get the input needed to process the command...
	if ( tokens.size() > 1 ) {
		try {	setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
				(String)tokens.elementAt(1), routine,"," ) );
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
}

/**
Read a line from a file.  Skip over comments.
@param in BufferedReader for open file to read.
@return the next line from the file, or null if at the end.
*/
private String readLine ( BufferedReader in )
{	String iline;
	while ( true ) {
		try {	iline = in.readLine ();
		}
		catch ( Exception e ) {
			return null;
		}
		if ( iline == null ) {
			return null;
		}
		// check for comments
		else if ( iline.startsWith("#") ) {
			continue;
		}
		else {	return iline;
		}
	}
}

/**
Run the command.
@param command_line Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "CompareFiles_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	
	String InputFile1 = parameters.getValue ( "InputFile1" );
	String InputFile2 = parameters.getValue ( "InputFile2" );
	String WarnIfDifferent = parameters.getValue ( "WarnIfDifferent" );
	String WarnIfSame = parameters.getValue ( "WarnIfSame" );
	boolean WarnIfDifferent_boolean = false;	// Default
	boolean WarnIfSame_boolean = false;		// Default
	int diff_count = 0;				// Number of lines that
							// are different
	if ( (WarnIfDifferent != null) && WarnIfDifferent.equalsIgnoreCase(_True)){
		WarnIfDifferent_boolean = true;
	}
	if ( (WarnIfSame != null) && WarnIfSame.equalsIgnoreCase(_True)){
		WarnIfSame_boolean = true;
	}

	String InputFile1_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),InputFile1) );
	String InputFile2_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),InputFile2) );
	if ( !IOUtil.fileExists(InputFile1_full) ) {
		message = "First input file \"" + InputFile1_full + "\" does not exist.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Verify that the file exists at the time the command is run."));
	}
	if ( !IOUtil.fileExists(InputFile2_full) ) {
		message = "Second input file \"" + InputFile2_full + "\" does not exist.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Verify that the file exists at the time the command is run."));
	}
	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	try {	// Open the files...
		BufferedReader in1 = new BufferedReader(new FileReader(IOUtil.getPathUsingWorkingDir(InputFile1_full)));
		BufferedReader in2 = new BufferedReader(new FileReader(IOUtil.getPathUsingWorkingDir(InputFile2_full)));
		// Loop through the files, comparing non-comment lines...
		String iline1, iline2;
		while ( true ) {
			iline1 = readLine ( in1 );
			iline2 = readLine ( in2 );
			if ( (iline1 == null) && (iline2 == null) ) {
				// both are done at the same time...
				break;
			}
			// TODO SAM 2006-04-20 The following needs to handle comments at the end...
			if ( (iline1 == null) && (iline2 != null) ) {
				// First file is are done so files are different...
				++diff_count;
				break;
			}
			if ( (iline2 == null) && (iline1 != null) ) {
				// Second file is are done so files are different...
				++diff_count;
				break;
			}
			if ( !iline1.equals(iline2) ) {
				++diff_count;
			}
		}
		in1.close();
		in2.close();
		Message.printStatus ( 2, routine, "There are " + diff_count + " lines that are different.");
	}
	catch ( Exception e ) {
		message = "Unexpected error comparing files.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
		throw new CommandException ( message );
	}
	if ( WarnIfDifferent_boolean && (diff_count > 0) ) {
		message = "" + diff_count + " lines were different.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag( command_tag,++warning_count),
		routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.WARNING,
					message, "Check files because difference is not expected.") );
		throw new CommandException ( message );
	}
	if ( WarnIfSame_boolean && (diff_count == 0) ) {
		message = "No lines were different (the files are the same).";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag( command_tag,++warning_count),
		routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.WARNING,
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
	String WarnIfDifferent = parameters.getValue("WarnIfDifferent");
	String WarnIfSame = parameters.getValue("WarnIfSame");
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
	if ( (WarnIfDifferent != null) && (WarnIfDifferent.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "WarnIfDifferent=" + WarnIfDifferent );
	}
	if ( (WarnIfSame != null) && (WarnIfSame.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "WarnIfSame=" + WarnIfSame );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
