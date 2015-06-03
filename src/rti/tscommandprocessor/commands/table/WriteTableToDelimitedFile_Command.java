package rti.tscommandprocessor.commands.table;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;

/**
This class initializes, checks, and runs the WriteTableToDelimitedFile() command.
*/
public class WriteTableToDelimitedFile_Command extends AbstractCommand implements Command, FileGenerator
{

/** 
Values for use with WriteHeaderComments parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Value to use for NaNValue.
*/
protected final String _Blank = "Blank";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteTableToDelimitedFile_Command ()
{	super();
	setCommandName ( "WriteTableToDelimitedFile" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String OutputFile = parameters.getValue ( "OutputFile" );
	String TableID = parameters.getValue ( "TableID" );
	String WriteHeaderComments = parameters.getValue ( "WriteHeaderComments" );
	String AlwaysQuoteStrings = parameters.getValue ( "AlwaysQuoteStrings" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
    if ( (TableID == null) || TableID.isEmpty()) {
        message = "The table identifier for the table to write has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid table identifier." ) );
    }
	
	if ( (OutputFile == null) || OutputFile.isEmpty() ) {
		message = "The output file must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify an output file." ) );
	}
	else if ( !OutputFile.startsWith("${") ) {
		// Can't check if output file is specified with ${Property}
	    String working_dir = null;
		try { Object o = processor.getPropContents ( "WorkingDir" );
			if ( o != null ) {
				working_dir = (String)o;
			}
		}
		catch ( Exception e ) {
			message = "Error requesting WorkingDir from processor.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Software error - report problem to support." ) );
		}

		try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, OutputFile));
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				message = "The output file parent directory does " +
				"not exist: \"" + adjusted_path + "\".";
				warning += "\n" + message;
				status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Create the output directory." ) );
			}
			f = null;
			f2 = null;
		}
		catch ( Exception e ) {
			message = "The output file:\n" +
			"    \"" + OutputFile +
			"\"\ncannot be adjusted using the working directory:\n" +
			"    \"" + working_dir + "\".";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that output file and working directory paths are compatible." ) );
		}
	}
	
    if ( (WriteHeaderComments != null) && !WriteHeaderComments.equals("") ) {
        if ( !WriteHeaderComments.equalsIgnoreCase(_False) && !WriteHeaderComments.equalsIgnoreCase(_True) ) {
            message = "The WriteHeaderComments parameter (" + WriteHeaderComments + ") must be " + _False +
            " or " + _True + ".";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the parameter as " + _False + " or " + _True + "."));
        }
    }
    
    if ( (AlwaysQuoteStrings != null) && !AlwaysQuoteStrings.equals("") ) {
        if ( !AlwaysQuoteStrings.equalsIgnoreCase(_False) && !AlwaysQuoteStrings.equalsIgnoreCase(_True) ) {
            message = "The AlwaysQuoteStrings parameter (" + AlwaysQuoteStrings + ") must be " + _False +
            " (default) or " + _True + ".";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the parameter as " + _False + " or " + _True + "."));
        }
    }

	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(6);
	validList.add ( "OutputFile" );
	validList.add ( "TableID" );
	validList.add ( "WriteHeaderComments" );
	validList.add ( "AlwaysQuoteStrings" );
	validList.add ( "NewlineReplacement" );
	validList.add ( "NaNValue" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
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
    List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	return (new WriteTableToDelimitedFile_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
	List<File> list = new Vector();
	if ( getOutputFile() != null ) {
		list.add ( getOutputFile() );
	}
	return list;
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
	return __OutputFile_File;
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	// Clear the output file
	
	setOutputFile ( null );
	
	// Check whether the processor wants output files to be created...

	CommandProcessor processor = getCommandProcessor();
	if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
		Message.printStatus ( 2, routine, "Skipping \"" + toString() + "\" because output is not being created." );
	}

	PropList parameters = getCommandParameters();
	
	CommandStatus status = getCommandStatus();
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
	status.clearLog(commandPhase);
	
	String OutputFile_full = null;

    // Get the table information  
    String OutputFile = parameters.getValue ( "OutputFile" );
    OutputFile_full = OutputFile;
    String TableID = parameters.getValue ( "TableID" );
    if ( (TableID != null) && !TableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) ) {
    	// In discovery mode want lists of tables to include ${Property}
    	if ( TableID.indexOf("${") >= 0 ) {
    		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    	}
    }
    String WriteHeaderComments = parameters.getValue ( "WriteHeaderComments" );
    boolean WriteHeaderComments_boolean = true;
    if ( (WriteHeaderComments != null) && WriteHeaderComments.equalsIgnoreCase(_False) ) {
        WriteHeaderComments_boolean = false;
    }
    String AlwaysQuoteStrings = parameters.getValue ( "AlwaysQuoteStrings" );
    boolean AlwaysQuoteStrings_boolean = false; // Default
    if ( (AlwaysQuoteStrings != null) && AlwaysQuoteStrings.equalsIgnoreCase(_True) ) {
        AlwaysQuoteStrings_boolean = true;
    }
    String NewlineReplacement = parameters.getValue ( "NewlineReplacement" );
    String newlineReplacement = NewlineReplacement;
    if ( (NewlineReplacement != null) && NewlineReplacement.equals("") ) {
        newlineReplacement = null; // User must use \s to indicate space
    }
    String NaNValue = parameters.getValue ( "NaNValue" );
    if ( NaNValue != null ) {
        if ( NaNValue.equals("") ) {
            NaNValue = null; // Will result in "NaN" in output
        }
        else if ( NaNValue.equals(_Blank) ) {
            NaNValue = "";
        }
    }

    PropList request_params = new PropList ( "" );
    request_params.set ( "TableID", TableID );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = processor.processRequest( "GetTable", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_Table = bean_PropList.getContents ( "Table" );
    DataTable table = null;
    if ( o_Table == null ) {
        message = "Unable to find table to process using TableID=\"" + TableID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that a table exists with the requested ID." ) );
    }
    else {
        table = (DataTable)o_Table;
    }
        
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings for command parameters.";
        Message.printWarning ( 2,
        MessageUtil.formatMessageTag(command_tag, ++warning_count),
        routine,message);
        throw new InvalidCommandParameterException ( message );
    }
        
    try {
    	// Now try to write...
    
        OutputFile_full = OutputFile;
    
		// Convert to an absolute path...
		OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            	TSCommandProcessorUtil.expandParameterValue(processor, this,OutputFile)) );
		Message.printStatus ( 2, routine, "Writing table to file \"" + OutputFile_full + "\"" );
		warning_count = writeTable ( table, OutputFile_full, WriteHeaderComments_boolean,
		    AlwaysQuoteStrings_boolean, StringUtil.literalToInternal(newlineReplacement), NaNValue,
		        warning_level, command_tag, warning_count );
		// Save the output file name...
		setOutputFile ( new File(OutputFile_full));
	}
	catch ( Exception e ) {
		message = "Unexpected error writing table to file \"" + OutputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
			message, "Check log file for details." ) );
		throw new CommandException ( message );
	}
	
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
        throw new CommandWarningException ( message );
    }
	
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file )
{
	__OutputFile_File = file;
}

/**
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String OutputFile = parameters.getValue ( "OutputFile" );
	String TableID = parameters.getValue ( "TableID" );
	String WriteHeaderComments = parameters.getValue ( "WriteHeaderComments" );
	String AlwaysQuoteStrings = parameters.getValue ( "AlwaysQuoteStrings" );
	String NewlineReplacement = parameters.getValue ( "NewlineReplacement" );
	String NaNValue = parameters.getValue ( "NaNValue" );
	StringBuffer b = new StringBuffer ();
	if ( (TableID != null) && (TableID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TableID=\"" + TableID + "\"" );
	}
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}
    if ( (WriteHeaderComments != null) && (WriteHeaderComments.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WriteHeaderComments=" + WriteHeaderComments );
    }
    if ( (AlwaysQuoteStrings != null) && (AlwaysQuoteStrings.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AlwaysQuoteStrings=" + AlwaysQuoteStrings );
    }
    if ( (NewlineReplacement != null) && (NewlineReplacement.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewlineReplacement=\"" + NewlineReplacement + "\"" );
    }
    if ( (NaNValue != null) && (NaNValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NaNValue=\"" + NaNValue + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

/**
Write a table to a delimited file.
@param table Table to write.
@param OutputFile name of file to write.
@param writeHeaderComments indicates whether header comments should be written (some software like Esri ArcGIS
do not handle comments)
@param alwaysQuoteStrings if true, then always surround strings with double quotes; if false strings will only
be quoted when they include the delimiter
@param newlineReplacement if non-null, string to replace newlines in strings when writing the file
@param NaNValue value to write for NaN (null will result in "NaN" being output).
@exception IOException if there is an error writing the file.
*/
private int writeTable ( DataTable table, String OutputFile, boolean writeHeaderComments,
	boolean alwaysQuoteStrings, String newlineReplacement, String NaNValue,
	int warning_level, String command_tag, int warning_count )
throws IOException
{	String routine = getClass().getName() + ".writeTable";
	String message;

	// Clear the output file

	setOutputFile ( null );

	// Check whether the processor wants output files to be created...

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();

    // Get the comments to add to the top of the file.

    List<String> outputCommentsList = new Vector();
    if ( writeHeaderComments ) {
        // Get the comments to be written at the top of the file
        // Put the standard header at the top of the file
        outputCommentsList = IOUtil.formatCreatorHeader ( "", 80, false );
        // Additional comments to add
        try {
            Object o = processor.getPropContents ( "OutputComments" );
            // Comments are available so use them...
            if ( o != null ) {
                outputCommentsList.addAll((List<String>)o);
            }
            // Also add internal comments specific to the table.
            outputCommentsList.addAll ( table.getComments() );
        }
        catch ( Exception e ) {
            // Not fatal, but of use to developers.
            message = "Error requesting OutputComments from processor - not using.";
            Message.printDebug(10, routine, message );
        }
    }
	
	try {
		Message.printStatus ( 2, routine, "Writing table file \"" + OutputFile + "\"" );
		table.writeDelimitedFile(OutputFile, ",", true, outputCommentsList, "#", alwaysQuoteStrings,
		    newlineReplacement, NaNValue );
	}
	catch ( Exception e ) {
		message = "Unexpected error writing table to file \"" + OutputFile + "\" (" + e + ")";
		Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Software error - report problem to support." ) );
	}
	return warning_count;
}

}