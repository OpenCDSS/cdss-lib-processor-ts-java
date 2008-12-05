package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.util.List;
import java.util.Vector;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.IOUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;

/**
<p>
This class initializes, checks, and runs the ReadTableFromDelimitedFile() command.
</p>
*/
public class ReadTableFromDelimitedFile_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
The table that is read.
*/
private DataTable __table = null;

/**
Constructor.
*/
public ReadTableFromDelimitedFile_Command ()
{	super();
	setCommandName ( "ReadTableFromDelimitedFile" );
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
{	String TableID = parameters.getValue ( "TableID" );
    String InputFile = parameters.getValue ( "InputFile" );
	//String SkipRows = parameters.getValue ( "SkipRows" );
	//String SkipColumns = parameters.getValue ( "SkipColumns" );
	//String HeaderRows = parameters.getValue ( "HeaderRows" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// If the input file does not exist, warn the user...

	String working_dir = null;
	
	CommandProcessor processor = getCommandProcessor();
	
    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the table identifier." ) );
    }
	try { Object o = processor.getPropContents ( "WorkingDir" );
		// Working directory is available so use it...
		if ( o != null ) {
			working_dir = (String)o;
		}
	}
	catch ( Exception e ) {
        message = "Error requesting WorkingDir from processor.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	
	if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
	}
	else {
        try {
            String adjusted_path = IOUtil.verifyPathForOS (IOUtil.adjustPath ( working_dir, InputFile) );
			File f = new File ( adjusted_path );
			if ( !f.exists() ) {
                message = "The input file does not exist:  \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the input file exists - may be OK if created at run time." ) );
			}
			f = null;
		}
		catch ( Exception e ) {
            message = "The input file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
		}
	}

    /* FIXME add checks for columns
	if ( (Columns == null) || (Columns.length() == 0) ) {
        message = "One or more columns must be specified";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the columns to merge." ) );
	}
	else {	// Check for integers...
		Vector v = StringUtil.breakStringList ( Columns, ",", 0 );
		String token;
		if ( v == null ) {
            message = "One or more columns must be specified";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the columns to merge." ) );
		}
		else {	int size = v.size();
			__Columns_intArray = new int[size];
			for ( int i = 0; i < size; i++ ) {
				token = (String)v.elementAt(i);
				if ( !StringUtil.isInteger(token) ) {
                    message = "Column \"" + token + "\" is not a number";
					warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the column as an integer >= 1." ) );
				}
				else {
                    // Decrement by one to make zero-referended.
					__Columns_intArray[i] = StringUtil.atoi(token) - 1;
				}
			}
		}
	}
    */
 
	// TODO SAM 2005-11-18 Check the format.
    
	//  Check for invalid parameters...
	List valid_Vector = new Vector();
    valid_Vector.add ( "TableID" );
    valid_Vector.add ( "InputFile" );
    valid_Vector.add ( "SkipRows" );
    valid_Vector.add ( "SkipColumns" );
    valid_Vector.add ( "HeaderRows" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );    

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Convert a properting containing a number sequence like "1,4-5,13" to zero offset like
"0,3-4,12".  This method is used to convert user-based parameters that have values 1+
with internal code values 0+.
*/
private String convertNumberSequenceToZeroOffset ( String sequence_param )
{
    if ( (sequence_param == null) || (sequence_param.length() == 0) ) {
        return sequence_param;
    }
    StringBuffer b = new StringBuffer();
    List v = StringUtil.breakStringList ( sequence_param, ", ", StringUtil.DELIM_SKIP_BLANKS );
    int vsize = 0;
    if ( v != null ) {
        vsize = v.size();
    }
    for ( int i = 0; i < vsize; i++ ) {
        String vi = (String)v.get(i);
        if ( i != 0 ) {
            b.append (",");
        }
        if ( StringUtil.isInteger(vi)) {
            int index = Integer.parseInt(vi);
            b.append ( "" + (index - 1) );
        }
        else {
            int pos = vi.indexOf("-");
            if ( pos >= 0 ) {
                // Specifying a range of values...
                int first_in_range = -1;
                int last_in_range = -1;
                if ( pos == 0 ) {
                    // First index is 1 (will be decremented below)...
                    first_in_range = 1;
                }
                else {
                    // Get first to skip...
                    first_in_range = Integer.parseInt(vi.substring(0,pos).trim());
                }
                last_in_range = Integer.parseInt(vi.substring(pos+1).trim());
                b.append ( "" + (first_in_range - 1) + "-" + (last_in_range - 1));
            }
        }
    }
    return b.toString();
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new ReadTableFromDelimitedFile_JDialog ( parent, this )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable table = getDiscoveryTable();
List v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector();
        v.add ( table );
    }
    return v;
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ReadTableFromDelimitedFile_Command.runCommand",message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String TableID = parameters.getValue ( "TableID" );
	String InputFile = parameters.getValue ( "InputFile" );
	//String SkipColumns = parameters.getValue ( "SkipColumns" );
	String SkipRows = parameters.getValue ( "SkipRows" );
	String HeaderRows = parameters.getValue ( "HeaderRows" );
	Message.printStatus( 2, routine, "parameter SkipRows=\"" + SkipRows + "\"");
	Message.printStatus( 2, routine, "parameter HeaderRows=\"" + HeaderRows + "\"");

	String InputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),InputFile) );
	if ( !IOUtil.fileExists(InputFile_full) ) {
		message += "\nThe delimited table file \"" + InputFile_full + "\" does not exist.";
		++warning_count;
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the delimited table file exists." ) );
	}

    /* FIXME enable the code
 	if ((__Columns_intArray == null) || (__Columns_intArray.length == 0)) {
		message += "\nOne or more columns must be specified";
		++warning_count;
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify columns to merge." ) );
	}

    // FIXME SAM 2007-11-29 Need to move the checks to checkCommandParameters()
	String token;
	String SimpleMergeFormat2 = "";	// Initial value.
	if (	(SimpleMergeFormat == null) ||
		(SimpleMergeFormat.length() == 0) ) {
		// Add defaults (treat as strings)...
		for ( int i = 0; i < __Columns_intArray.length; i++ ) {
			SimpleMergeFormat2 += "%s";
		}
	}
	else {	Vector v = StringUtil.breakStringList (	SimpleMergeFormat, ",", 0 );
		int size = v.size();
		if ( size != __Columns_intArray.length ) {
			message +=
			"\nThe number of specifiers in the merge " +
			"format (" + SimpleMergeFormat +
			") does not match the number of columns (" +
			__Columns_intArray.length;
			++warning_count;
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the number of format specifiers matches the columns to merge." ) );
		}
		else {	for ( int i = 0; i < size; i++ ) {
				token = (String)v.elementAt(i);
				if ( !StringUtil.isInteger(token) ) {
					message += "\nThe format specifier \""+	token +	"\" is not an integer";
					++warning_count;
                    status.addToLog ( command_phase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the format specifier as an integer." ) );
				}
				else {	if ( token.startsWith("0") ) {
						// TODO SAM 2005-11-18 Need to enable 0 for %s in StringUtil. Assume integers...
						SimpleMergeFormat2 += "%" + token + "d";
					}
					else {
                        SimpleMergeFormat2 += "%" + token+ "." + token + "s";
					}
				}
			}
		}
	}
    */

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	// Now process the file...

	DataTable table = null;
	PropList props = new PropList ( "DataTable" );
	props.set ( "Delimiter", "," );	// Default
	props.set ( "CommentLineIndicator=#" );	// Skip comment lines
	props.set ( "TrimInput=True" );		// Trim strings after reading.
	props.set ( "TrimStrings=True" );	// Trim strings after parsing
	//props.set ( "ColumnDataTypes=Auto" );  // Automatically determine column data types
	if ( (SkipRows != null) && (SkipRows.length() > 0) ) {
	    props.set ( "SkipRows=" + convertNumberSequenceToZeroOffset(SkipRows) );
	}
    if ( (HeaderRows != null) && (HeaderRows.length() > 0) ) {
        props.set ( "HeaderRows=" + convertNumberSequenceToZeroOffset(HeaderRows) );
    }
    Message.printStatus( 2, routine, "parameter zero index SkipRows=\"" + props.getValue("SkipRows") + "\"");
    Message.printStatus( 2, routine, "parameter zero index HeaderRows=\"" + props.getValue("HeaderRows") + "\"");
	try {
        table = DataTable.parseFile ( InputFile_full, props );
        
        // Set the table identifier...
        
        table.setTableID ( TableID );
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error read table from delimited file \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the file exists and is readable." ) );
		throw new CommandWarningException ( message );
	}
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
		throw new CommandWarningException ( message );
	}
    
    // Set the table in the processor...
    
    if ( command_phase == CommandPhaseType.RUN ) {
        PropList request_params = new PropList ( "" );
        request_params.setUsingObject ( "Table", table );
        try {
            processor.processRequest( "SetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting SetTable(Table=...) from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
        }
    }
    else if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( table );
    }

    status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __table = table;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TableID = props.getValue( "TableID" );
	String InputFile = props.getValue( "InputFile" );
	String SkipRows = props.getValue("SkipRows");
	String SkipColumns = props.getValue("SkipColumns");
	String HeaderRows = props.getValue("HeaderRows");
	StringBuffer b = new StringBuffer ();
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
	if ( (SkipRows != null) && (SkipRows.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SkipRows=\"" + SkipRows + "\"" );
	}
	if ( (SkipColumns != null) && (SkipColumns.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SkipColumns=\"" + SkipColumns + "\"" );
	}
	if ( (HeaderRows != null) && (HeaderRows.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "HeaderRows=\"" + HeaderRows + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
