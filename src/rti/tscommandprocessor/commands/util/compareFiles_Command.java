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

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
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
<p>
This class initializes, checks, and runs the compareFiles() command.
</p>
<p>The CommandProcessor must return the following properties:  TSResultsList.
</p>
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
	setCommandName ( "compareFiles" );
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
{	String InputFile1 = parameters.getValue ( "InputFile1" );
	String InputFile2 = parameters.getValue ( "InputFile2" );
	String WarnIfDifferent = parameters.getValue ( "WarnIfDifferent" );
	String WarnIfSame = parameters.getValue ( "WarnIfSame" );
	String warning = "";

	// The existence of the parent directories or files is not checked
	// because the files may be created dynamically after the command is
	// edited.

	if ( (InputFile1 == null) || (InputFile1.length() == 0) ) {
		warning += "\nThe first input file must be specified.";
	}
	if ( (InputFile2 == null) || (InputFile2.length() == 0) ) {
		warning += "\nThe second input file must be specified.";
	}
	if ( (WarnIfDifferent != null) && !WarnIfDifferent.equals("") ) {
		if (	!WarnIfDifferent.equalsIgnoreCase(_False) &&
			!WarnIfDifferent.equalsIgnoreCase(_True) ) {
			warning += "\nThe WarnIfDifferent parameter \"" +
				WarnIfDifferent + "\" must be False or True.";
		}
	}
	if ( (WarnIfSame != null) && !WarnIfSame.equals("") ) {
		if (	!WarnIfSame.equalsIgnoreCase(_False) &&
			!WarnIfSame.equalsIgnoreCase(_True) ) {
			warning += "\nThe WarnIfSame parameter \"" +
				WarnIfSame + "\" must be False or True.";
		}
	}
	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
	valid_Vector.add ( "InputFile1" );
	valid_Vector.add ( "InputFile2" );
	valid_Vector.add ( "WarnIfDifferent" );
	valid_Vector.add ( "WarnIfSame" );
	Vector warning_Vector = null;
	try {	warning_Vector = parameters.validatePropNames (
			valid_Vector, null, null, "parameter" );
	}
	catch ( Exception e ) {
		// Ignore.  Should not happen.
		warning_Vector = null;
	}
	if ( warning_Vector != null ) {
		int size = warning_Vector.size();
		for ( int i = 0; i < size; i++ ) {
			warning += "\n" + (String)warning_Vector.elementAt (i);
		}
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
	return (new compareFiles_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand (	String command, String command_tag,
				int warning_level )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_count = 0;
	String routine = "compareFiles_Command.parseCommand", message;

	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );

	if ( (tokens == null) ) { //|| tokens.size() < 2 ) {}
		message = "Invalid syntax for \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	// Get the input needed to process the command...
	if ( tokens.size() > 1 ) {
		try {	setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
				(String)tokens.elementAt(1), routine,"," ) );
		}
		catch ( Exception e ) {
			message = "Syntax error in \"" + command +
				"\".  Not enough tokens.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message);
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
Run the commands:
<pre>
compareFiles(InputFile1="X",InputFile2="X",WarnIfDifferent=X,WarnIfSame=X)
</pre>
@param processor The CommandProcessor that is executing the command, which will
provide necessary data inputs and receive output(s).
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( String command_tag, int warning_level )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "compareFiles_Command.runCommand", message;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
	String InputFile1 = parameters.getValue ( "InputFile1" );
	String InputFile2 = parameters.getValue ( "InputFile2" );
	String WarnIfDifferent = parameters.getValue ( "WarnIfDifferent" );
	String WarnIfSame = parameters.getValue ( "WarnIfSame" );
	boolean WarnIfDifferent_boolean = false;	// Default
	boolean WarnIfSame_boolean = false;		// Default
	int diff_count = 0;				// Number of lines that
							// are different
	if (	(WarnIfDifferent != null) &&
		WarnIfDifferent.equalsIgnoreCase(_True)){
		WarnIfDifferent_boolean = true;
	}
	if (	(WarnIfSame != null) &&
		WarnIfSame.equalsIgnoreCase(_True)){
		WarnIfSame_boolean = true;
	}

	String InputFile1_full = IOUtil.getPathUsingWorkingDir ( InputFile1 );
	String InputFile2_full = IOUtil.getPathUsingWorkingDir ( InputFile2 );
	if ( !IOUtil.fileExists(InputFile1_full) ) {
		message = "Input file \"" + InputFile1_full +
			"\" does not exist.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
	}
	if ( !IOUtil.fileExists(InputFile2_full) ) {
		message = "Input file \"" + InputFile2_full +
			"\" does not exist.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
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
		BufferedReader in1 = new BufferedReader(new FileReader(
			IOUtil.getPathUsingWorkingDir(InputFile1_full)));
		BufferedReader in2 = new BufferedReader(new FileReader(
			IOUtil.getPathUsingWorkingDir(InputFile2_full)));
		// Loop through the files, comparing non-comment lines...
		String iline1, iline2;
		while ( true ) {
			iline1 = readLine ( in1 );
			iline2 = readLine ( in2 );
			if ( (iline1 == null) && (iline2 == null) ) {
				// both are done at the same time...
				break;
			}
			// REVISIT SAM 2006-04-20
			// The following needs to handle comments at the end...
			if ( (iline1 == null) && (iline2 != null) ) {
				// First file is are done so files are
				// different...
				++diff_count;
				break;
			}
			if ( (iline2 == null) && (iline1 != null) ) {
				// Second file is are done so files are
				// different...
				++diff_count;
				break;
			}
			if ( !iline1.equals(iline2) ) {
				++diff_count;
			}
		}
		in1.close();
		in2.close();
		Message.printStatus ( 2, routine,
		"There are " + diff_count + " lines that are different.");
	}
	catch ( Exception e ) {
		message = "Error comparing files.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		Message.printWarning ( 3, routine, e );
		throw new CommandException ( message );
	}
	if ( WarnIfDifferent_boolean && (diff_count > 0) ) {
		message = "" + diff_count + " lines were different.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag( command_tag,++warning_count),
		routine, message );
		throw new CommandException ( message );
	}
	if ( WarnIfSame_boolean && (diff_count == 0) ) {
		message = "No lines were different (the files are the same).";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag( command_tag,++warning_count),
		routine, message );
		throw new CommandException ( message );
	}
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String InputFile1 = props.getValue("InputFile1");
	String InputFile2 = props.getValue("InputFile2");
	String WarnIfDifferent = props.getValue("WarnIfDifferent");
	String WarnIfSame = props.getValue("WarnIfSame");
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
