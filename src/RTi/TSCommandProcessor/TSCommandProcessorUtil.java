package RTi.TSCommandProcessor;

import java.util.Vector;

import RTi.Util.IO.Command;
import RTi.Util.String.StringUtil;

/**
This class contains static utility methods to support TSCommandProcessor.  These methods
are here to prevent the processor from getting to large and in some cases because code is
being migrated.
*/
public class TSCommandProcessorUtil {
	
/**
Get the commands above an index position.
@param processor The processor that is managing commands.
@param pos Index (0+) above which to get commands.
*/
private static Vector getCommandsBeforeIndex ( TSCommandProcessor processor, int pos )
{	Vector commands = new Vector();
	int size = processor.size();
	for ( int i = 0; i < size; i++ ) {
		commands.addElement ( processor.get(i));
	}
	return commands;
}

/**
Get a list of identifiers from a list of commands.  See documentation for
fully loaded method.  The output list is not sorted and does NOT contain the
input type or name.
@param commands Time series commands to search.
@return list of time series identifiers or an empty non-null Vector if nothing
found.
*/
private static Vector getTSIdentifiersFromCommands ( Vector commands )
{	// Default behavior...
	return getTSIdentifiersFromCommands ( commands, false, false );
}

/**
Get a list of identifiers from a list of commands.  See documentation for
fully loaded method.  The output list does NOT contain the input type or name.
@param commands Time series commands to search.
@param sort Should output be sorted by identifier.
@return list of time series identifiers or an empty non-null Vector if nothing
found.
*/
protected static Vector getTSIdentifiersFromCommands (	Vector commands,
							boolean sort )
{	// Return the identifiers without the input type and name.
	return getTSIdentifiersFromCommands ( commands, false, sort );
}

/**
Get a list of identifiers from a list of commands (as String or Command to allow
for migration to full Command instance processing).  Commands that start
with "TS ? = " or lines that are time series identifiers are returned.
These strings are suitable for drop-down lists, etc.
If a non-empty alias is available, it is used for the identifer.  Otherwise the
full identifier is used.
@param commands Time series commands to search.
@param include_input If true, include the input type and name in the returned
values.  If false, only include the 5-part information.
@param sort Should output be sorted by identifier.
@return list of time series identifiers or an empty non-null Vector if nothing
found.
*/
protected static Vector getTSIdentifiersFromCommands (	Vector commands,
							boolean include_input,
							boolean sort )
{	if ( commands == null ) {
		return new Vector();
	}
	Vector v = new Vector ( 10, 10 );
	int size = commands.size();
	String command = null;
	Vector tokens = null;
	boolean in_comment = false;
	Object command_o = null;	// Command as object
	for ( int i = 0; i < size; i++ ) {
		command_o = commands.elementAt(i);
		if ( command_o instanceof Command ) {
			command = command_o.toString().trim();
		}
		else if ( command_o instanceof String ) {
			command = ((String)command_o).trim();
		}
		if (	(command == null) ||
			command.startsWith("#") ||
			(command.length() == 0) ) {
			// Make sure comments are ignored...
			continue;
		}
		if ( command.startsWith("/*") ) {
			in_comment = true;
			continue;
		}
		else if ( command.startsWith("*/") ) {
			in_comment = false;
			continue;
		}
		if ( in_comment ) {
			continue;
		}
		else if ( StringUtil.startsWithIgnoreCase(command,"TS ") ) {
			// Use the alias...
			tokens = StringUtil.breakStringList(
				command.substring(3)," =",
				StringUtil.DELIM_SKIP_BLANKS);
			if ( (tokens != null) && (tokens.size() > 0) ) {
				v.addElement ( (String)tokens.elementAt(0) );
				//+ " (alias)" );
			}
			tokens = null;	// GC
		}
		else if ( isTSID(command) ) {
			// Reasonably sure it is an identifier.  Only add the
			// 5-part TSID and not the trailing input type and name.
			int pos = command.indexOf("~");
			if ( (pos < 0) || include_input ) {
				// Add the whole thing...
				v.addElement ( command );
			}
			else {	// Add the part before the input fields...
				v.addElement ( command.substring(0,pos) );
			}
		}
	}
	tokens = null;
	return v;
}

/**
Return the time series identifiers for commands before a specific command
in the TSCommandProcessor.  This is used, for example, to provide a list of
identifiers to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series identifiers are needed.
@return a Vector of String containing the time series identifiers.
*/
public static Vector getTSIdentifiersNoInputFromCommandsBeforeCommand( TSCommandProcessor processor, Command command )
{
	// Get the position of the command in the list...
	int pos = processor.indexOf(command);
	if ( pos < 0 ) {
		// Just return a blank list...
		return new Vector();
	}
    // Find the commands above the position...
	Vector commands = getCommandsBeforeIndex ( processor, pos );
	// Get the time series identifiers from the commands...
	return getTSIdentifiersFromCommands ( commands );
}


/**
Evaluate whether a command appears to be a pure time series identifier (not a
command that uses a time series identifier).  The string is checked to see if
it has three "." and that any parentheses are after the first ".".  If this
method is called after checking for TS TSID = command() syntax, it should
correctly evaluate the identifier.  Some of these checks are needed for TSIDs
that have data types with () - this is the case with some input types (e.g.,
HydroBase agricultural statistics that have "(Dry)".
@param command Command to evaluate.
@return true if the command appears to be a pure TSID, false if not.
*/
protected static boolean isTSID ( String command )
{	int left_paren_pos = command.indexOf('(');
	int right_paren_pos = command.indexOf(')');
	int period_pos = command.indexOf('.');

	if ((StringUtil.patternCount(command,".") >= 3) &&
			(((left_paren_pos < 0) &&	// Definitely not a
			(right_paren_pos < 0)) ||	// command.
			((left_paren_pos > 0) &&	// A TSID with ()
			(period_pos > 0) &&
			(left_paren_pos > period_pos))) ) {
		return true;
	}
	else {	return false;
	}
}

}
