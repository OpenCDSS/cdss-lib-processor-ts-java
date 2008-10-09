//------------------------------------------------------------------------------
// newDataTest_Command - handle the newDataTest() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2006-04-07	J. Thomas Sapienza, RTi	Initial version.
//------------------------------------------------------------------------------

package rti.tscommandprocessor.commands.datatest;

import java.util.Vector;

import javax.swing.JFrame;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSSupplier;

import RTi.DataTest.DataTest;
import RTi.DataTest.DataTestFactory;

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

import RTi.Util.String.StringUtil;

/**
<p>
This class initializes, checks, and runs the newDataTest() command.
</p>
<p>The CommandProcessor must return the following properties:  
TSResultsList and WorkingDir.
</p>
*/
public class NewDataTest_Command 
extends AbstractCommand
implements Command {

protected static final String
	_FALSE = "False",
	_TRUE = "True";

/**
Constructor.
*/
public NewDataTest_Command ()
{
	super();
	setCommandName ( "NewDataTest" );
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
public void checkCommandParameters ( PropList parameters,
				     String command_tag,
				     int warning_level )
throws InvalidCommandParameterException {
	String warning = "";
	
	// Get the property values. 
	String TestExpression = parameters.getValue("TestExpression");
	String DataTest = parameters.getValue("DataTest");

	// Input file
	if ( TestExpression != null && TestExpression.length() != 0 ) {

	} 
	else {
		warning += "\nThe Input File must be specified.";
	}

	if (DataTest != null && !DataTest.equals("")) {
		if (DataTest.indexOf(" ") > -1) {
			// do not allow spaces in the alias
			warning += "\nThe DataTest value (\"" + DataTest
				+ "\") cannot contain any space.";
		}
	}

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, warning_level ),
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
{	
	// The command will be modified if changed...
	return ( new NewDataTest_JDialog ( parent, this ) ).ok();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
	super.finalize();
}

/**
Given a Vector of wildcard TSIDs and a Vector of time series, returns a Vector
of all the unique TSIDs of the time series that match the wildcarded TSIDs.
@param wildcardTSIDs the wildcarded TSIDs to match against.
@param tsVector the Vector of time series to try matching to the wildcarded 
TSIDs.
@return a Vector of all the time series IDs from the tsVector that match any
wildcard in the first Vector.  There will be no repeated TSIDs in this Vector,
even if a TSID from the tsVector matches more than one of the wildcard TSIDs.
This Vector will not be null if no matches can be found -- it will be an empty
Vector.  However, if tsVector is empty or null, a null Vector will be returned.
*/
public Vector findTSMatches(Vector wildcardTSIDs, Vector tsVector) {
	Vector v = new Vector();

	if (tsVector == null || tsVector.size() == 0) {
		// return null so that this case can be detected and an
		// exception thrown in the calling code.
		return null;
	}

	TS ts = null;
	TSIdent tsid = null;

	int size = tsVector.size();
	int size2 = wildcardTSIDs.size();
	String s = null;

	// Loop through the wildcards and find all the tsVector TSIDs that
	// match any of the wildcarded TSIDs.

	for (int i = 0; i < size; i++) {
		ts = (TS)tsVector.elementAt(i);
		tsid = ts.getIdentifier();
		
		for (int j = 0; j < size2; j++) {
			s = (String)wildcardTSIDs.elementAt(j);

			if (tsid.matches(s)) {
				v.add(tsid.toString(true));
			}
		}
	}

	if (v.size() == 0) {
		return null;
	}

	// Remove all duplicates from the matches TSID list.

	java.util.Collections.sort(v);

	Vector single = new Vector();

	size = v.size();
	String val = (String)v.elementAt(0);
	single.add(val);
	for (int i = 1; i < size; i++) {
		s = (String)v.elementAt(i);
		if (s.equals(val)) {
			// next!
		}
		else {
			val = s;
			single.add(val);
		}
	}

	return single;
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
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
	int index = command.indexOf("(");
	String str = command.substring(index);
	String routine = "newDataTest_Command.parseCommand", message;
	
	int warning_level = 2;
	int warning_count = 0;

	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS |
		StringUtil.DELIM_ALLOW_STRINGS);
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the DataTest and TestExpression
		message = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level, routine, message);
		++warning_count;
		throw new InvalidCommandSyntaxException ( message );
	}
	
	try {
		setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
			(String)tokens.elementAt(1), routine, "," ) );
	}
	catch ( Exception e ) {
		message = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level, routine, message);
		++warning_count;
		throw new InvalidCommandSyntaxException ( message );
	}

	if (!StringUtil.startsWithIgnoreCase(command.trim(), "DataTest ")) {
		message = "Command did not start with \"DataTest ...\"";
		Message.printWarning(warning_level, routine, message);
		++warning_count;
		throw new InvalidCommandSyntaxException(message);
	}

	// there is an alias specified.  Extract the alias
	// from the full command.
	str = command.substring(9);	
	index = str.indexOf("=");
	int index2 = str.indexOf("(");
	String DataTest = null;
	if (index2 < index) {
		// no alias specified -- badly-formed command
		DataTest = "Invalid_DataTestID";
		message = "No alias was specified, although "
			+ "the command started with \"TS ...\"";
		Message.printWarning(warning_level, 
			routine, message);
		++warning_count;
		throw new InvalidCommandSyntaxException(
			message);
	}

	DataTest = str.substring(0, index);

	if (DataTest != null) {
		setCommandParameter("DataTest", DataTest.trim());
	}
}

/**
Run the command:
<pre>
newDataTest(TestExpression="x")
</pre>
@param command_number Number of command being processed (0+).
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
       CommandWarningException,
       CommandException
{	String routine = "newDataTest_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	// Get the command properties not already stored as members.
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
	
	String TestExpression = parameters.getValue("TestExpression");
	String DataTestID = parameters.getValue("DataTest");

	DataTest test = null;

	try {
		test = DataTestFactory.buildDataTest(TestExpression, 
			(TSSupplier)processor);
		test.setID(DataTestID);
		// Reset the DataTest list in the processor.
		Vector DataTestList_Vector 
			= (Vector)processor.getPropContents("DataTestList");
		if (DataTestList_Vector == null) {
			DataTestList_Vector = new Vector();
		}
	
		// Clear out any DataTests with the same ID from the internal
		// Vector.
		int size = DataTestList_Vector.size();
		DataTest dt = null;
		for (int i = size - 1; i >= 0; i--) {
			dt = (DataTest)DataTestList_Vector.elementAt(i);
			if (dt.getID().equals(DataTestID)) {
				DataTestList_Vector.removeElementAt(i);	
			}
		}
		
		boolean hasWildcards = test.hasWildcards();

		if (hasWildcards) {
			Vector v = test.getWildcardTSIDs();
			Vector tsList = (Vector)processor.getPropContents( "TSResultsList");
			Vector matches = findTSMatches(v, tsList);

			if (matches == null) {
				message = "No matching time series found.";
				Message.printWarning(warning_level,
					MessageUtil.formatMessageTag(
					command_tag, ++warning_count),
					routine, message);
			}
			else {
				size = matches.size();
				String s = null;

				Vector tests = new Vector();
				
				for (int i = 0; i < size; i++) {
					dt = new DataTest(test);
					s = (String)matches.elementAt(i);
					dt.setWildcards(s);
					dt.setTestData();
					tests.add(dt);
				}

				test.setIsMaster(true);
				test.setDataTests(tests);
			}
		}
		
		DataTestList_Vector.add(test);
		processor.setPropContents("DataTestList", DataTestList_Vector);
	}
	catch (Exception e) {
		message = "Unexpected error creating DataTest for expression \"" + TestExpression + "\" (" + e + ")";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
		Message.printWarning ( warning_level, routine, e);		
	}

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		throw new CommandWarningException ( message );
	}
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

	String DataTest = props.getValue("DataTest");
	String TestExpression = props.getValue("TestExpression");

	StringBuffer b = new StringBuffer ();

	// Input File
	if ((TestExpression != null) && (TestExpression.length() > 0)) {
		b.append("TestExpression=\"" + TestExpression + "\"");
	}

	if (DataTest != null && DataTest.length() > 0) {
		DataTest = "DataTest " + DataTest + " = ";
	}
	else {
		DataTest = "";
	}

	return DataTest + getCommandName() + "(" + b.toString() + ")";
}

}
