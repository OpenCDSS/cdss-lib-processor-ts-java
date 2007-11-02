package rti.tscommandprocessor.core;

import java.util.Vector;

import RTi.TS.TS;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatusProvider;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatusUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
This class contains static utility methods to support TSCommandProcessor.  These methods
are here to prevent the processor from getting to large and in some cases because code is
being migrated.
*/
public class TSCommandProcessorUtil
{
	
/**
Append a time series to the processor time series results list.
@param processor the CommandProcessor to use to get data.
@param command Command for which to get the working directory.
@param ts Time series to append.
*/
public static void appendTimeSeriesToResultsList ( CommandProcessor processor, Command command, TS ts )
{	String routine = "TSCommandProcessorUtil.appendTimeSeriesToResultsList";
	PropList request_params = new PropList ( "" );
	request_params.setUsingObject ( "TS", ts );
	//CommandProcessorRequestResultsBean bean = null;
	try { //bean =
		processor.processRequest( "AppendTimeSeries", request_params );
	}
	catch ( Exception e ) {
		String message = "Error requesting AppendTimeSeries(TS=\"...\" from processor).";
		Message.printWarning(3, routine, e);
		Message.printWarning(3, routine, message );
	}
}
	
/**
Get the commands before the indicated index position.  Only the requested commands
are returned.  Use this, for example, to get the setWorkingDir() commands above
the insert position for a readXXX() command, so the working directory can be
defined and used in the editor dialog.
@return List of commands (as Vector of Command instances) before the index that match the commands in
the needed_commands_Vector.  This will always return a non-null Vector, even if
no commands are in the Vector.
@param index The index in the command list before which to search for other commands.
@param processor A TSCommandProcessor with commands to search.
@param needed_commands_String_Vector Vector of commands (as String) that need to be processed
(e.g., "setWorkingDir").  Only the main command name should be defined.
@param last_only if true, only the last item above the insert point
is returned.  If false, all matching commands above the point are returned in
the order from top to bottom.
*/
public static Vector getCommandsBeforeIndex (
	int index,
	TSCommandProcessor processor,
	Vector needed_commands_String_Vector,
	boolean last_only )
{	// Now search backwards matching commands for each of the requested
	// commands...
	int size = 0;
	if ( needed_commands_String_Vector != null ) {
		size = needed_commands_String_Vector.size();
	}
	String needed_command_string;
	Vector found_commands = new Vector();
	// Get the commands from the processor
	Vector commands = processor.getCommands();
	Command command;
	// Now loop up through the command list...
	for ( int ic = (index - 1); ic >= 0; ic-- ) {
		command = (Command)commands.elementAt(ic);
		for ( int i = 0; i < size; i++ ) {
			needed_command_string = (String)needed_commands_String_Vector.elementAt(i);
			//((String)_command_List.getItem(ic)).trim() );
			if (	needed_command_string.regionMatches(true,0,command.toString().trim(),0,
					needed_command_string.length() ) ) {
					found_commands.addElement ( command );
					if ( last_only ) {
						// Don't need to search any more...
						break;
					}
				}
			}
		}
		// Reverse the commands so they are listed in the order of the list...
		size = found_commands.size();
		if ( size <= 1 ) {
			return found_commands;
		}
		Vector found_commands_sorted = new Vector(size);
		for ( int i = size - 1; i >= 0; i-- ) {
			found_commands_sorted.addElement ( found_commands.elementAt(i));
		}
		return found_commands_sorted;
}
	
/**
Get the commands above an index position.
@param processor The processor that is managing commands.
@param pos Index (0+) before which to get commands.  The command at the indicated
position is NOT included in the search.
*/
private static Vector getCommandsBeforeIndex ( TSCommandProcessor processor, int pos )
{	Vector commands = new Vector();
	int size = processor.size();
	if ( pos > size ) {
		pos = size;
	}
	for ( int i = 0; i < pos; i++ ) {
		commands.addElement ( processor.get(i));
	}
	return commands;
}

/**
Get the maximum command status severity for the processor.  This is used, for example, when
determining an overall status for a runCommands() command.
@param processor Command processor to check status.
@return most severe command status from all commands in a processor.
*/
public static CommandStatusType getCommandStatusMaxSeverity ( TSCommandProcessor processor )
{
	int size = processor.size();
	Command command;
	CommandStatusType most_severe = CommandStatusType.UNKNOWN;
	CommandStatusType from_command;
	for ( int i = 0; i < size; i++ ) {
		command = processor.get(i);
		if ( command instanceof CommandStatusProvider ) {
			from_command = CommandStatusUtil.getHighestSeverity((CommandStatusProvider)command);
			//Message.printStatus (2,"", "Highest severity \"" + command.toString() + "\"=" + from_command.toString());
			most_severe = CommandStatusType.maxSeverity(most_severe,from_command);
		}
	}
	return most_severe;
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
@return a Vector of String containing the time series identifiers, or an empty
Vector.
*/
public static Vector getTSIdentifiersNoInputFromCommandsBeforeCommand( TSCommandProcessor processor, Command command )
{	String routine = "TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand";
	// Get the position of the command in the list...
	int pos = processor.indexOf(command);
	Message.printStatus ( 2, routine,
			"Position in list is " + pos + " for command:" + command );
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
Get the working directory for a command (e.g., for editing).
@param processor the TSCommandProcessor to use to get data.
@param command Command for which to get the working directory.
@return The working directory in effect for a command.
*/
public static String getWorkingDirForCommand ( CommandProcessor processor, Command command )
{	String routine = "TSTool_JFrame.commandProcessor_GetWorkingDirForCommand";
	PropList request_params = new PropList ( "" );
	request_params.setUsingObject ( "Command", command );
	CommandProcessorRequestResultsBean bean = null;
	try { bean =
		processor.processRequest( "GetWorkingDirForCommand", request_params );
		return bean.getResultsPropList().getValue("WorkingDir");
	}
	catch ( Exception e ) {
		String message = "Error requesting GetWorkingDirForCommand(Command=\"" + command +
		"\" from processor).";
		Message.printWarning(3, routine, e);
		Message.printWarning(3, routine, message );
	}
	return null;
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
