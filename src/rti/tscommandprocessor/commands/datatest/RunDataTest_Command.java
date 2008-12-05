//------------------------------------------------------------------------------
// runDataTest_Command - handle the runDataTest() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2006-04-07	J. Thomas Sapienza, RTi	Initial version.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------

package rti.tscommandprocessor.commands.datatest;

import java.util.List;

import javax.swing.JFrame;

import RTi.DataTest.Action;
import RTi.DataTest.DataTest;

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

import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

/**
<p>
This class initializes, checks, and runs the RunDataTest() command.
</p>
*/
public class RunDataTest_Command 
extends AbstractCommand
implements Command {

protected static final String
	_FALSE = "False",
	_TRUE = "True";

/**
Constructor.
*/
public RunDataTest_Command ()
{
	super();
	setCommandName ( "RunDataTest" );
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
	String TestID = parameters.getValue("TestID");
	String TestStart = parameters.getValue("TestStart");
	String TestEnd = parameters.getValue("TestEnd");
	String NumberPositiveResultsBeforeHit 
		= parameters.getValue("NumberPositiveResultsBeforeHit");

	// Input file
	if ( TestID != null && TestID.length() != 0 ) {
		if (TestID.indexOf(" ") > -1) {
			warning += "\nThe DataTest ID (\"" + TestID
				+ "\") cannot contain any spaces.";
		}
	} 
	else {
		warning += "\nThe DataTest ID must be specified.";
	}

	if (TestStart != null && !TestStart.equals("")) {
		if (!TimeUtil.isDateTime(TestStart)) {
			warning += "TestStart (\"" + TestStart 
				+ "\") must be a valid date/time.";
		}
	}

	if (TestEnd != null && !TestEnd.equals("")) {
		if (!TimeUtil.isDateTime(TestEnd)) {
			warning += "TestEnd (\"" + TestEnd 
				+ "\") must be a valid date/time.";
		}
	}

	if (NumberPositiveResultsBeforeHit != null
	    && !NumberPositiveResultsBeforeHit.equals("")) {
	    	if (!StringUtil.isInteger(NumberPositiveResultsBeforeHit)) {
			warning += "NumberPositiveResultsBeforeHit (\""
				+ NumberPositiveResultsBeforeHit + "\")"
				+ " must be a positive integer.";
		}
		int val = (new Integer(NumberPositiveResultsBeforeHit))
			.intValue();
		if (val < 0) {
			warning += "NumberPositiveResultsBeforeHit (\""
				+ NumberPositiveResultsBeforeHit + "\")"
				+ " must be a positive integer.";
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
	return ( new RunDataTest_JDialog ( parent, this ) ).ok();
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
	String routine = "runDataTest_Command.parseCommand", message;
	int warning_level = 2;
	int warning_count = 0;

	List tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS |
		StringUtil.DELIM_ALLOW_STRINGS);
	if ( (tokens == null) || tokens.size() < 1 ) {
		// Must have at least the TestID token.
		message = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level,routine, message);
		++warning_count;
		throw new InvalidCommandSyntaxException ( message );
	}
	
	try {
		setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
			(String)tokens.get(1), routine, "," ) );
	}
	catch ( Exception e ) {
		message = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level,routine, message);
		++warning_count;
		throw new InvalidCommandSyntaxException ( message );
	}
}

/**
Run the command:
<pre>
runDataTest(TestExpression="x")
</pre>
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
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
{	String routine = "runDataTest_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	// Get the command properties not already stored as members.
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
	
	String TestID = parameters.getValue("TestID");
	String TestStart = parameters.getValue("TestStart");
	String TestEnd = parameters.getValue("TestEnd");
	String NumberPositiveResultsBeforeHit 
		= parameters.getValue("NumberPositiveResultsBeforeHit");
	
	List dataTestList = null;
	try { Object o = processor.getPropContents ( "DataTestList" );
		if ( o != null ) {
			dataTestList = (List)o;
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		message = "Error requesting DataTestList from processor - not using.";
		 routine = getCommandName() + ".runCommand";
		Message.printDebug(10, routine, message );
	}
	
	int size = 0;
	if (dataTestList != null) {
		size = dataTestList.size();
	}

	DataTest test = null;
	boolean found = false;
	for (int i = 0; i < size; i++) {
		test = (DataTest)dataTestList.get(i);
		if (test.getID().equals(TestID)) {
			found = true;
			break;
		}
	}

	DateTime start = null;
	DateTime end = null;
	int numBeforeHit = 1;

	if (TestStart != null && TestStart.length() > 0) {
		// already checked in checkParameters()
		try {
			start = DateTime.parse(TestStart);
		}
		catch (Exception e) {
			start = null;
		}
	}
	
	if (TestEnd != null && TestEnd.length() > 0) {
		// already checked in checkParameters()
		try {
			end = DateTime.parse(TestEnd);
		}
		catch (Exception e) {
			end = null;
		}
	}

	if (NumberPositiveResultsBeforeHit != null
	    && NumberPositiveResultsBeforeHit.length() > 0) {
	    	numBeforeHit = (new Integer(NumberPositiveResultsBeforeHit))
			.intValue();
	}

	if (!found) {
		message = "Could not locate DataTest with ID \"" + TestID
			+ "\".";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
	}
	else {
		try {
			if (test.isMaster()) {
				List tests = test.getDataTests();
				size = tests.size();
				DataTest dt = null;
				for (int i = 0; i < size; i++) {
					dt = (DataTest)tests.get(i);
					runDataTest(dt, start, end, 
						numBeforeHit);
				}
			}
			else {
				runDataTest(test, start, end, numBeforeHit);
			}

			Action[] actions = test.getActions();
			for (int i = 0; i < actions.length; i++) {
				if (actions[i].checkForPositiveDataTests()) {
					actions[i].runAction(null, test);
				}
			}

		}
		catch (Exception e) {
			message = "Unexpected error running DataTest. (" + e + ")";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
					command_tag, ++warning_count ),
				routine, message );
			Message.printWarning ( warning_level, routine, e);
		}
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
Runs a single DataTest from start to end.
@param test the test to run.
@param start the starting date from which to run.
@param end the ending date to which to run.
@param numBeforeHit the number of positive results before a hit is considered
to have happened.
@throws Exception if an error occurs.
*/
private void runDataTest(DataTest test, DateTime start, DateTime end,
int numBeforeHit)
throws Exception {
	test.preTestRun();
	TimeInterval baseMult = test.getTimeSeriesIntervalData();
	test.setPositiveCount(numBeforeHit);
	if (start == null) {
		start = test.getStartDate();
	}
	if (end == null) {
		end = test.getEndDate();
	}

	DateTime dt = start;

	while (dt.lessThan(end)) {
		test.run(dt);
		dt.addInterval(baseMult.getBase(), baseMult.getMultiplier());
	}

	test.postTestRun();

/*
	Action[] actions = test.getActions();
	for (int i = 0; i < actions.length; i++) {
		if (actions[i].checkForPositiveDataTests()) {
			actions[i].runAction(test);
		}
	}
*/
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

	String TestID = props.getValue("TestID");
	String TestStart = props.getValue("TestStart");
	String TestEnd = props.getValue("TestEnd");
	String NumberPositiveResultsBeforeHit 
		= props.getValue("NumberPositiveResultsBeforeHit");

	StringBuffer b = new StringBuffer ();

	// Input File
	if ((TestID != null) && (TestID.length() > 0)) {
		b.append("TestID=\"" + TestID + "\"");
	}

	if (TestStart != null && TestStart.length() > 0) {
		b.append("TestStart=\"" + TestStart + "\"");
	}

	if (TestEnd != null && TestEnd.length() > 0) {
		b.append("TestEnd=\"" + TestEnd + "\"");
	}

	if (NumberPositiveResultsBeforeHit != null 
	    && NumberPositiveResultsBeforeHit.length() > 0) {
		b.append("NumberPositiveResultsBeforeHit=\"" 
			+ NumberPositiveResultsBeforeHit + "\"");
	}

	return b.toString();
}

}
