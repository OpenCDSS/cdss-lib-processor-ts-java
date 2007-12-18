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
    Vector valid_Vector = new Vector();
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
    Vector v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector();
        v.addElement ( table );
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
	//String SkipRows = parameters.getValue ( "SkipRows" );
	//String HeaderRows = parameters.getValue ( "HeaderRows" );

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
	try {
        table = DataTable.parseFile ( InputFile_full, props );
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unable to read delimited table file \"" + InputFile_full + "\".";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the file exists and is readable." ) );
		throw new CommandWarningException ( message );
	}
    
    // Set the table identifier...
    
    table.setTableID ( TableID );
    
    // Set the table in the processor...
	
	// Loop through the table records, merge the columns and set in the new column...

    /*
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
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify the format of the list file." ) );
			continue;
		}
		for ( j = 0; j < __Columns_intArray.length; j++ ) {
			try {
                s = (String)rec.getFieldValue(__Columns_intArray[j]);
				v.addElement ( s );
			}
			catch ( Exception e ) {
				message = "Error getting table field [" + i + "][" + j + "]";
				Message.printWarning ( 2,
				MessageUtil.formatMessageTag(command_tag,
				++warning_count),
				routine,message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify the format of the list file - report the problem to software support." ) );
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
              status.addToLog ( command_phase,
                      new CommandLogRecord(CommandStatusType.FAILURE,
                              message, "Report the problem to software support." ) );
			continue;
		}
	}
    */

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
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "SetTable", request_params);
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
