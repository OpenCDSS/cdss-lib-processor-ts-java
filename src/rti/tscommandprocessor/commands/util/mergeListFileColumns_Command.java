//------------------------------------------------------------------------------
// mergeListFileColumns_Command - handle the mergeListFileColumns() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-11-23	Steven A. Malers, RTi	Initial version.  Copy and modify
//					mergeListFileColumns().
// 2007-02-16	SAM, RTi				Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.util;

import javax.swing.JFrame;

import java.io.File;
import java.util.Vector;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.IOUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;

/**
<p>
This class initializes, checks, and runs the mergeListFileColumns() command.
</p>
<p>The CommandProcessor must return the following properties:  ??.
</p>
*/
public class mergeListFileColumns_Command extends AbstractCommand
implements Command
{

// Columns as integer array, filled in checkCommandParameters() and used in
// runCommand.
private int [] __Columns_intArray = null;

/**
Constructor.
*/
public mergeListFileColumns_Command ()
{	super();
	setCommandName ( "mergeListFileColumns" );
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
{	String ListFile = parameters.getValue ( "ListFile" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String Columns = parameters.getValue ( "Columns" );
	String NewColumnName = parameters.getValue ( "NewColumnName" );
	// TODO SAM 2007-02-16 Need to enable the SimpleMergeFormat
	//String SimpleMergeFormat = parameters.getValue ( "SimpleMergeFormat" );
	String warning = "";

	// If the input file does not exist, warn the user...

	String working_dir = null;
	
	CommandProcessor processor = getCommandProcessor();
	
	try { Object o = processor.getPropContents ( "WorkingDir" );
		// Working directory is available so use it...
		if ( o != null ) {
			working_dir = (String)o;
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		String message = "Error requesting WorkingDir from processor - not using.";
		String routine = getCommandName() + "_checkCommandParameters";
		Message.printDebug(10, routine, message );
	}
	
	if ( ListFile.length() == 0 ) {
		warning += "\nA list file must be specified.";
	}
	else {	try {	String adjusted_path = IOUtil.adjustPath (
				working_dir, ListFile);
			File f = new File ( adjusted_path );
			if ( !f.exists() ) {
				warning +=
				"\nThe list file does not exist:\n" +
				"    " + adjusted_path;
			}
			f = null;
		}
		catch ( Exception e ) {
			warning +=
			"\nThe working directory:\n" + "    \"" + working_dir +
			"\"\ncannot be adjusted using:\n" +
			"    \"" + ListFile + "\".";
		}
	}

	// Adjust the working directory that was passed in by the specified
	// directory.  If the directory for the file does not exist, warn the
	// user...
	if ( OutputFile.length() == 0 ) {
		warning += "\nAn output file must be specified.";
	}
	else {	try {	String adjusted_path = IOUtil.adjustPath (
			working_dir, OutputFile);
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				warning +=
				"\nThe directory does not exist:\n" +
				"    " + f.getParent() + ".";
			}
			f = null;
		}
		catch ( Exception e ) {
			warning +=
			"\nThe working directory:\n" +
			"    \"" + working_dir +
			"\"\ncannot be adjusted using:\n" +
			"    \"" + OutputFile + "\".";
		}
	}

	if ( (Columns == null) || (Columns.length() == 0) ) {
		warning += "\nOne or more columns must be specified";
	}
	else {	// Check for integers...
		Vector v = StringUtil.breakStringList ( Columns, ",", 0 );
		String token;
		if ( v == null ) {
			warning += "\nOne or more columns must be specified";
		}
		else {	int size = v.size();
			__Columns_intArray = new int[size];
			for ( int i = 0; i < size; i++ ) {
				token = (String)v.elementAt(i);
				if ( !StringUtil.isInteger(token) ) {
					warning += "\nColumn \"" + token +
						"\" is not a number";
				}
				else {	// Decrement by one to make
					// zero-referended.
					__Columns_intArray[i] =
						StringUtil.atoi(token) - 1;
				}
			}
		}
	}
 
	if ( NewColumnName.length() == 0 ) {
		warning += "\nA new column name needs to be specified.";
	}

	// Revisit SAM 2005-11-18
	// Check the format.

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
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new mergeListFileColumns_JDialog (
		parent, this,
		false	// Not runnable
		)).ok();
}

/**
Edit and optionally the run command from the editor.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editRunnableCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new mergeListFileColumns_JDialog (
		parent, this,
		true	// Runnable
		)).ok();
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
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
	String routine = "mergeListFileColumns_Command.parseCommand", message;

	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the command name and arguments...
		message = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	// Get the input needed to process the file...
	try {	setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
			(String)tokens.elementAt(1), routine, "," ) );
	}
	catch ( Exception e ) {
		message = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level, routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
}

/**
Run the commands:
<pre>
mergeListFileColumns(ListFile="X",OutputFile="X",Columns="X",NewColumnName="X",
SimpleMergeFormat="X")
</pre>
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "mergeListFileColumns_Command.runCommand",message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String ListFile = parameters.getValue ( "ListFile" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String NewColumnName = parameters.getValue ( "NewColumnName" );
	String SimpleMergeFormat = parameters.getValue ( "SimpleMergeFormat" );

	String ListFile_full = IOUtil.getPathUsingWorkingDir ( ListFile );
	if ( !IOUtil.fileExists(ListFile_full) ) {
		message += "\nThe list file \"" +
			ListFile_full + "\" does not exist.";
		++warning_count;
	}
	String OutputFile_full = IOUtil.getPathUsingWorkingDir ( OutputFile );
	File f = new File(OutputFile_full);
	if ( !IOUtil.fileExists(f.getParent()) ) {
		message += "\nThe output file directory \"" +
			f.getParent() + "\" does not exist.";
		++warning_count;
	}

	if ((__Columns_intArray == null) || (__Columns_intArray.length == 0)) {
		message += "\nOne or more columns must be specified";
		++warning_count;
	}

	String token;
	String SimpleMergeFormat2 = "";	// Initial value.
	if (	(SimpleMergeFormat == null) ||
		(SimpleMergeFormat.length() == 0) ) {
		// Add defaults (treat as strings)...
		for ( int i = 0; i < __Columns_intArray.length; i++ ) {
			SimpleMergeFormat2 += "%s";
		}
	}
	else {	Vector v = StringUtil.breakStringList (
			SimpleMergeFormat, ",", 0 );
		int size = v.size();
		if ( size != __Columns_intArray.length ) {
			message +=
			"\nThe number of specifiers in the merge " +
			"format (" + SimpleMergeFormat +
			") does not match the number of columns (" +
			__Columns_intArray.length;
			++warning_count;
		}
		else {	for ( int i = 0; i < size; i++ ) {
				token = (String)v.elementAt(i);
				if ( !StringUtil.isInteger(token) ) {
					message +=
						"\nFormat specifier \""+
						token +
						"\" is not an integer";
					++warning_count;
				}
				else {	if ( token.startsWith("0") ) {
						// REVISIT SAM 2005-11-18
						// Need to enable 0 for %s in
						// StringUtil.
						// Assume integers...
						SimpleMergeFormat2 +=
						"%" + token + "d";
					}
					else {	SimpleMergeFormat2 +=
						"%" + token+ "." + token + "s";
					}
				}
			}
		}
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	// Now process the file...

	Vector OutputFileList = new Vector();	// List of output files from
						// the processor.

	DataTable table = null;
	PropList props = new PropList ( "DataTable" );
	props.set ( "Delimiter", "," );	// Default
	// REVISIT SAM 2005-11-18 Enable later.
	//if ( MergeDelim != null ) {
		//props.set ( "MergeDelimiters=" + MergeDelim );
	//}
	props.set ( "CommentLineIndicator=#" );	// Skip comment lines
	props.set ( "TrimInput=True" );		// Trim strings after reading.
	props.set ( "TrimStrings=True" );	// Trim strings after parsing
	try {	table = DataTable.parseFile ( ListFile_full, props );
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unable to read list file \"" + ListFile_full + "\".";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message );
		throw new CommandWarningException ( message );
	}
	
	// Add a column to the table.  For now alwasy treat as a string...

	table.addField ( new TableField(
		TableField.DATA_TYPE_STRING,NewColumnName) );

	// Loop through the table records, merge the columns and set in the
	// new column...

	int size = table.getNumberOfRecords();
	String merged;	// Merged column string
	int mergedcol = table.getNumberOfFields() - 1;	// New at end
	Vector v = new Vector ( __Columns_intArray.length );
	TableRecord rec = null;
	String s;
	int j;
	for ( int i = 0; i < size; i++ ) {
		v.removeAllElements();
		try {	rec = table.getRecord(i);
		}
		catch ( Exception e ) {
			message = "Error getting table record [" + i + "]";
			Message.printWarning ( 2,
			MessageUtil.formatMessageTag(command_tag,
			++warning_count),
			routine,message );
			continue;
		}
		for ( j = 0; j < __Columns_intArray.length; j++ ) {
			try {	s = (String)rec.getFieldValue(
					__Columns_intArray[j]);
				v.addElement ( s );
			}
			catch ( Exception e ) {
				message = "Error getting table field [" +
					i + "][" + j + "]";
				Message.printWarning ( 2,
				MessageUtil.formatMessageTag(command_tag,
				++warning_count),
				routine,message );
				continue;
			}
		}
		merged = StringUtil.formatString ( v, SimpleMergeFormat2 );
		try {	rec.setFieldValue ( mergedcol, merged );
		}
		catch ( Exception e ) {
			message = "Error modifying table record [" + i + "]";
			Message.printWarning ( 2,
			MessageUtil.formatMessageTag(command_tag,
			++warning_count),
			routine,message );
			continue;
		}
	}

	// Write the new file...

	Message.printStatus ( 2, routine,
	"Writing list file \"" + OutputFile_full +"\"" );
	try {	table.writeDelimitedFile ( OutputFile_full,
			",",	// Delimiter
			true,	// Write header
			null );	// Comments for header
		// REVISIT SAM 2005-11-18
		// Need a general IOUtil method to format the header strings
		// (and NOT also open the file).

		// Resave the output to the processor so that appropriate
		// actions are taken...

		processor.setPropContents ( "OutputFileList", OutputFileList );
	}
	catch ( Exception e ) {
		message = "Unable to write table file \"" +
			OutputFile_full + "\"";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
		throw new CommandWarningException ( message );
	}
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String ListFile = props.getValue( "ListFile" );
	String OutputFile = props.getValue("OutputFile");
	String Columns = props.getValue("Columns");
	String NewColumnName = props.getValue("NewColumnName");
	String SimpleMergeFormat = props.getValue("SimpleMergeFormat");
	StringBuffer b = new StringBuffer ();
	if ( (ListFile != null) && (ListFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ListFile=\"" + ListFile + "\"" );
	}
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}
	if ( (Columns != null) && (Columns.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Columns=\"" + Columns + "\"" );
	}
	if ( (NewColumnName != null) && (NewColumnName.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewColumnName=\"" + NewColumnName + "\"" );
	}
	if ( (SimpleMergeFormat != null) && (SimpleMergeFormat.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SimpleMergeFormat=\"" + SimpleMergeFormat + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
