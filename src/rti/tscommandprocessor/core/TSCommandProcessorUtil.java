package rti.tscommandprocessor.core;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusProvider;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatusUtil;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;

/**
This class contains static utility methods to support TSCommandProcessor.  These methods
are here to prevent the processor from getting to large and in some cases because code is
being migrated.
*/
public abstract class TSCommandProcessorUtil
{

/**
Used to handle regression test results during testing.
*/
private static PrintWriter __regression_test_fp = null;
   
/**
Add a record to the regression test results report.  The report is a simple text file
that indicates whether a test passed.
@param processor CommandProcessor that is being run.
@param max_severity the maximum severity from the command that was run.
@param InputFile_full the full path to the command file that was run. 
*/
public static void appendToRegressionTestReport(CommandProcessor processor, CommandStatusType max_severity,
        String InputFile_full )
{
    if ( __regression_test_fp != null ) {
        __regression_test_fp.println ( StringUtil.formatString(max_severity,"%-10.10s") + "  " + InputFile_full);
    }
}

/**
Append a time series list to the processor time series results list.
Errors should not result and are logged in the log file and command status, indicating a software problem.
@param processor the CommandProcessor to use to get data.
@param command Command for which to get the working directory.
@param tslist List of time series to append.
@param return the number of warnings generated.
*/
public static int appendTimeSeriesListToResultsList ( CommandProcessor processor, Command command, List tslist )
{
    int wc = 0;
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    for ( int i = 0; i < size; i++ ) {
        wc += appendTimeSeriesToResultsList ( processor, command, (TS)tslist.get(i) );
    }
    return wc;
}
	
/**
Append a time series to the processor time series results list.
@param processor the CommandProcessor to use to get data.
@param command Command for which to get the working directory.
@param ts Time series to append.
@param return the number of warnings generated.
*/
public static int appendTimeSeriesToResultsList ( CommandProcessor processor, Command command, TS ts )
{	String routine = "TSCommandProcessorUtil.appendTimeSeriesToResultsList";
	PropList request_params = new PropList ( "" );
	request_params.setUsingObject ( "TS", ts );
    int warning_level = 3;
    int warning_count = 0;
    CommandStatus status = null;
    if ( command instanceof CommandStatusProvider ) {
        status = ((CommandStatusProvider)command).getCommandStatus();
    }
	//CommandProcessorRequestResultsBean bean = null;
	try { //bean =
		processor.processRequest( "AppendTimeSeries", request_params );
	}
	catch ( Exception e ) {
		String message = "Error requesting AppendTimeSeries(TS=\"...\") from processor).";
        // This is a low-level warning that the user should not see.
        // A problem would indicate a software defect so return the warning count as a trigger.
		Message.printWarning(warning_level, routine, e);
		Message.printWarning(warning_level, routine, message );
        if ( status != null ) {
            status.addToLog(CommandPhaseType.RUN,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message,
                    "Check the log file for details - report the problem to software support."));
        }
        ++warning_count;
	}
    return warning_count;
}

/**
Close the regression test report file.
*/
public static void closeRegressionTestReportFile ()
{
    if ( __regression_test_fp != null ) {
        __regression_test_fp.close();
        __regression_test_fp = null;
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
Determine whether commands should create output by checking the CreateOutput parameter.
This is a processor level property.  If there is a problem, return true (create output).
@param processor the CommandProcessor to use to get data.
@return true if output should be created when processing commands, false if not.
*/
public static boolean getCreateOutput ( CommandProcessor processor )
{	String routine = "TSCommandProcessorUtil.getCreateOutput";
	try {
		Object o = processor.getPropContents ( "CreateOutput" );
		if ( o != null ) {
			return ((Boolean)o).booleanValue();
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		String message = "Error requesting CreateOutput from processor - will create output.";
		Message.printWarning(3, routine, message );
		Message.printWarning(3, routine, e );
	}
	return true;
}

/**
Return the list of property names available from the processor.
These properties can be requested using getPropContents().
@return the list of property names available from the processor.
*/
public static Vector getPropertyNameList( CommandProcessor processor )
{
	// This could use reflection.
	if ( processor instanceof TSCommandProcessor ) {
		return ((TSCommandProcessor)processor).getPropertyNameList();
	}
	return new Vector();
}

/**
Get a list of table identifiers from a list of commands.  See documentation for
fully loaded method.  The output list is not sorted and does NOT contain the
input type or name.
@param commands Time series commands to search.
@return list of table identifiers or an empty non-null Vector if nothing
found.
*/
private static Vector getTableIdentifiersFromCommands ( List commands )
{   // Default behavior...
    return getTableIdentifiersFromCommands ( commands, false );
}

/**
Get a list of table identifiers from a list of commands.  The returned strings are suitable for
drop-down lists, etc.  Table identifiers are determined as follows:
Commands that implement ObjectListProvider have their getObjectList(DataTable) method called.
The getID() method on the DataTable is then returned.
@param commands Commands to search.
@param sort Should output be sorted by identifier.
@return list of table identifiers or an empty non-null Vector if nothing found.
*/
protected static Vector getTableIdentifiersFromCommands ( List commands, boolean sort )
{   if ( commands == null ) {
        return new Vector();
    }
    Vector v = new Vector ( 10, 10 );
    int size = commands.size();
    boolean in_comment = false;
    Command command = null;
    String command_string = null;
    for ( int i = 0; i < size; i++ ) {
        command = (Command)commands.get(i);
        command_string = command.toString();
        if ( command_string.startsWith("/*") ) {
            in_comment = true;
            continue;
        }
        else if ( command_string.startsWith("*/") ) {
            in_comment = false;
            continue;
        }
        if ( in_comment ) {
            continue;
        }
        if ( command instanceof ObjectListProvider ) {
            // Try to get the list of identifiers using the interface method.
            // TODO SAM 2007-12-07 Evaluate the automatic use of the alias.
            List list = ((ObjectListProvider)command).getObjectList ( new DataTable().getClass() );
            String id;
            if ( list != null ) {
                int tablesize = list.size();
                DataTable table;
                for ( int its = 0; its < tablesize; its++ ) {
                    table = (DataTable)list.get(its);
                    id = table.getTableID();
                    if ( !id.equals("") ) {
                        v.addElement( id );
                    }
                }
            }
        }
    }
    return v;
}

/**
Return the table identifiers for commands before a specific command
in the TSCommandProcessor.  This is used, for example, to provide a list of
identifiers to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series identifiers are needed.
@return a Vector of String containing the table identifiers, or an empty Vector.
*/
public static Vector getTableIdentifiersFromCommandsBeforeCommand( TSCommandProcessor processor, Command command )
{   String routine = "TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand";
    // Get the position of the command in the list...
    int pos = processor.indexOf(command);
    Message.printStatus ( 2, routine, "Position in list is " + pos + " for command:" + command );
    if ( pos < 0 ) {
        // Just return a blank list...
        return new Vector();
    }
    // Find the commands above the position...
    Vector commands = getCommandsBeforeIndex ( processor, pos );
    // Get the time series identifiers from the commands...
    return getTableIdentifiersFromCommands ( commands );
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
protected static Vector getTSIdentifiersFromCommands (	Vector commands, boolean sort )
{	// Return the identifiers without the input type and name.
	return getTSIdentifiersFromCommands ( commands, false, sort );
}

/**
Get a list of identifiers from a list of commands (as String or Command to allow
for migration to full Command instance processing).  These strings are suitable for drop-down lists, etc.
Time series identifiers are determined as follows:
<ol>
<li>    Commands that implement ObjectListProvider have their getObjectList(TS) method called.
        The time series identifiers from the time series list are examined and those with alias
        will have the alias returned.  Otherwise, the full time series identifier is returned with or
        with input path as requested.</li>
<li>    Command strings that start with "TS ? = " have the alias returned.</li>
<li>    Lines that are time series identifiers are returned, including the full path as requested.</li>
</ol>
@param commands Time series commands to search.
@param include_input If true, include the input type and name in the returned
values.  If false, only include the 5-part information.
@param sort Should output be sorted by identifier.
@return list of time series identifiers or an empty non-null Vector if nothing found.
*/
protected static Vector getTSIdentifiersFromCommands ( Vector commands, boolean include_input, boolean sort )
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
		if ( (command == null) || command.startsWith("#") || (command.length() == 0) ) {
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
        if ( (command_o != null) && (command_o instanceof ObjectListProvider) ) {
            // Try to get the list of identifiers using the interface method.
            // TODO SAM 2007-12-07 Evaluate the automatic use of the alias.
            List list = ((ObjectListProvider)command_o).getObjectList ( new TS().getClass() );
            if ( list != null ) {
                int tssize = list.size();
                TS ts;
                for ( int its = 0; its < tssize; its++ ) {
                    ts = (TS)list.get(its);
                    if ( !ts.getAlias().equals("") ) {
                        // Use the alias if it is avaialble.
                        v.addElement( ts.getAlias() );
                    }
                    else {
                        // Use the identifier.
                        v.addElement ( ts.getIdentifier().toString(include_input) );
                    }
                }
            }
        }
		else if ( StringUtil.startsWithIgnoreCase(command,"TS ") ) {
			// Use the alias...
			tokens = StringUtil.breakStringList( command.substring(3)," =",	StringUtil.DELIM_SKIP_BLANKS);
			if ( (tokens != null) && (tokens.size() > 0) ) {
				v.addElement ( (String)tokens.elementAt(0) );
				//+ " (alias)" );
			}
			tokens = null;
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
	Message.printStatus ( 2, routine, "Position in list is " + pos + " for command:" + command );
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
Get the current working directory for the processor.
@param processor the CommandProcessor to use to get data.
@return The working directory in effect for a command.
*/
public static String getWorkingDir ( CommandProcessor processor )
{	String routine = "TSCommandProcessorUtil.getWorkingDir";
	try {	Object o = processor.getPropContents ( "WorkingDir" );
		if ( o != null ) {
			return (String)o;
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		String message = "Error requesting WorkingDir from processor.";
		Message.printWarning(3, routine, message );
	}
	return null;
}

/**
Get the working directory for a command (e.g., for editing).
@param processor the CommandProcessor to use to get data.
@param command Command for which to get the working directory.
@return The working directory in effect for a command.
*/
public static String getWorkingDirForCommand ( CommandProcessor processor, Command command )
{	String routine = "TSCommandProcessorUtil.commandProcessor_GetWorkingDirForCommand";
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

/**
Open a new regression test report file.
@param OutputFile_full Full path to report file to open.
@param Append_boolean indicates whether the file should be opened in append mode.
*/
public static void openNewRegressionTestReportFile ( String OutputFile_full, boolean Append_boolean )
throws FileNotFoundException
{
    __regression_test_fp = new PrintWriter ( new FileOutputStream ( OutputFile_full, Append_boolean ) );
}

/**
Process a list of time series after reading.  This calls the command processor readTimeSeries2() method.
Command status messages will be added if problems arise but exceptions are not thrown.
*/
public static int processTimeSeriesListAfterRead( CommandProcessor processor, Command command, List tslist )
{   int log_level = 3;
    int warning_count = 0;
    String routine = "TSCommandProcessorUtil.processTimeSeriesListAfterRead";
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "TSList", tslist );
    CommandStatus status = null;
    if ( command instanceof CommandStatusProvider ) {
        status = ((CommandStatusProvider)command).getCommandStatus();
    }
    try {
        processor.processRequest( "ReadTimeSeries2", request_params);
    }
    catch ( Exception e ) {
        String message = "Error post-processing time series after read using ReadTimeSeries2 processor request.";
        Message.printWarning(log_level, routine, e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
        ++warning_count;
    }
    return warning_count;
}

/**
Validate command parameter names and generate standard feedback.
@param valid_Vector List of valid parameter names (others will be flagged as invalid).
@param command The command being checked.
@param warning A warning String that is receiving warning messages, for logging.  It
will be appended to if there are more issues.
@return the warning string, longer if invalid parameters are detected.
*/
public static String validateParameterNames (
		Vector valid_Vector,
		Command command,
		String warning )
{	if ( command == null ) {
		return warning;
	}
	PropList parameters = command.getCommandParameters();
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
		StringBuffer b = new StringBuffer();
		for ( int i = 0; i < size; i++ ) {
			warning += "\n" + (String)warning_Vector.elementAt (i);
			b.append ( (String)warning_Vector.elementAt(i));
		}
		if ( command instanceof CommandStatusProvider ) { 
			CommandStatus status = ((CommandStatusProvider)command).getCommandStatus();
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.WARNING,
					b.toString(),
					"Specify only valid parameters - see documentation."));
		}
	}
	return warning;
}

}
