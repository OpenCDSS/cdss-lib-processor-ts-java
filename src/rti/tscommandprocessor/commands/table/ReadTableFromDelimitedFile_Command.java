// ReadTableFromDelimitedFile_Command - This class initializes, checks, and runs the ReadTableFromDelimitedFile() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.util.ArrayList;
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
This class initializes, checks, and runs the ReadTableFromDelimitedFile() command.
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
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String TableID = parameters.getValue ( "TableID" );
    String InputFile = parameters.getValue ( "InputFile" );
	//String SkipLines = parameters.getValue ( "SkipLines" );
	//String SkipColumns = parameters.getValue ( "SkipColumns" );
	String HeaderLines = parameters.getValue ( "HeaderLines" );
	String ColumnNames = parameters.getValue ( "ColumnNames" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	CommandProcessor processor = getCommandProcessor();
	
    if ( (TableID == null) || TableID.isEmpty() ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the table identifier." ) );
    }
    // Don't check WorkingDir here because it may be created dynamically but make sure property is available.
	try {
	    processor.getPropContents ( "WorkingDir" );
	}
	catch ( Exception e ) {
        message = "Error requesting WorkingDir from processor.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	
	if ( (InputFile == null) || InputFile.isEmpty() ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
	}

    /* FIXME add checks for columns.
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
                    // Decrement by one to make zero-referenced.
					__Columns_intArray[i] = StringUtil.atoi(token) - 1;
				}
			}
		}
	}
    */

	int paramCount = 0;
    if ( (HeaderLines != null) && !HeaderLines.isEmpty() ) {
    	++paramCount;
    	/* TODO smalers 2020-09-12 need to check that header lines are valid.
        message = ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
                */
    }
    if ( (ColumnNames != null) && !ColumnNames.isEmpty() ) {
    	++paramCount;
    }
    if ( paramCount == 2 ) {
        message = "Only one of HeaderLines and ColumnNames parameter can be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify HeaderLines or ColumnNames, but not both." ) );
    }
 
	//  Check for invalid parameters.
	List<String> validList = new ArrayList<>(13);
    validList.add ( "TableID" );
    validList.add ( "InputFile" );
    validList.add ( "Delimiter" );
    validList.add ( "SkipLines" );
    validList.add ( "SkipColumns" );
    validList.add ( "HeaderLines" );
    validList.add ( "ColumnNames" );
    validList.add ( "DateTimeColumns" );
    validList.add ( "DoubleColumns" );
    validList.add ( "IntegerColumns" );
    validList.add ( "TextColumns" );
    validList.add ( "Top" );
    validList.add ( "RowCountProperty" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );    

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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed.
	return (new ReadTableFromDelimitedFile_JDialog ( parent, this )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable() {
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
The following classes can be requested:  DataTable
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{   DataTable table = getDiscoveryTable();
    List<T> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector<T>();
        v.add ( (T)table );
    }
    return v;
}

// Use base class parseCommand().

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;

	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = Boolean.TRUE; // Default.
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen.
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

	// Make sure there are time series available to operate on.
	
	PropList parameters = getCommandParameters();

    String TableID = parameters.getValue ( "TableID" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
	}
	String InputFile = parameters.getValue ( "InputFile" ); // Expanded below.
	String Delimiter = parameters.getValue ( "Delimiter" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		Delimiter = TSCommandProcessorUtil.expandParameterValue(processor, this, Delimiter);
	}
	String delimiter = ","; // Default.
	if ( (Delimiter != null) && !Delimiter.isEmpty() ) {
		delimiter = Delimiter;
		delimiter = delimiter.replace("\\t", "\t");
		delimiter = delimiter.replace("\\s", " ");
	}
	//String SkipColumns = parameters.getValue ( "SkipColumns" );
	String SkipLines = parameters.getValue ( "SkipLines" );
	String HeaderLines = parameters.getValue ( "HeaderLines" );
	Message.printStatus( 2, routine, "parameter SkipLines=\"" + SkipLines + "\"");
	Message.printStatus( 2, routine, "parameter HeaderLines=\"" + HeaderLines + "\"");
	String ColumnNames = parameters.getValue ( "ColumnNames" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		ColumnNames = TSCommandProcessorUtil.expandParameterValue(processor, this, ColumnNames);
	}
	String DateTimeColumns = parameters.getValue ( "DateTimeColumns" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		DateTimeColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, DateTimeColumns);
	}
	String DoubleColumns = parameters.getValue ( "DoubleColumns" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		DoubleColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, DoubleColumns);
	}
	String IntegerColumns = parameters.getValue ( "IntegerColumns" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		IntegerColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, IntegerColumns);
	}
	String TextColumns = parameters.getValue ( "TextColumns" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TextColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, TextColumns);
	}
	String Top = parameters.getValue ( "Top" );
    String RowCountProperty = parameters.getValue ( "RowCountProperty" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	RowCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, RowCountProperty);
    }

    /* FIXME enable the code.
 	if ((__Columns_intArray == null) || (__Columns_intArray.length == 0)) {
		message += "\nOne or more columns must be specified";
		++warning_count;
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify columns to merge." ) );
	}

    // FIXME SAM 2007-11-29 Need to move the checks to checkCommandParameters().
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

	// Now process the file.

    if ( commandPhase == CommandPhaseType.RUN ) {
    	String InputFile_full = InputFile;
    	boolean canRead = true;
    	// Match a file if * wildcard is used:
    	// - can only use the wildcard in the filename part
    	// - must match a single file
    	if ( InputFile_full.indexOf("*") > 0 ) {
    		InputFile_full = IOUtil.verifyPathForOS(
	        	IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        		TSCommandProcessorUtil.expandParameterValue(processor, this,InputFile)) );
    	    File inputFile = new File( InputFile_full );
    	    String parent = inputFile.getParent();
    	    // The pattern to match is glob-style, so turn into Java pattern.
    	    String filePattern = inputFile.getName().replace("*", ".*");
    	    if ( parent.indexOf("*") >= 0 ) {
    	    	message += "The delimited file can only contain * in the filename part.";
			 	++warning_count;
	          	status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	               	message, "Verify that the delimited file contains * only in the filename (not folder)." ) );
	         	canRead = false;
    	    }
    	    else {
    	    	File [] filePathList = new File(parent).listFiles();
    	    	List<String> matchingFiles = new ArrayList<>();
    	    	for ( File filePath : filePathList ) {
    	    		String file = filePath.getName();
    	    		//Message.printStatus(2, routine, "Checking file: \"" + file + "\" against pattern \"" + filePattern + "\"");
    	    		if ( file.matches(filePattern) ) {
    	    			matchingFiles.add(filePath.getPath());
    	    		}
    	    	}
    	    	if ( matchingFiles.size() == 0 ) {
    	    		++warning_count;
	          		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	               		"No files matched: " + InputFile_full, "Check whether the wildcard pattern is too specific or in error.") );
	         		canRead = false;
    	    	}
    	    	else if ( matchingFiles.size() > 1 ) {
    	    		++warning_count;
	          		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	               		"" + matchingFiles.size() + " files matched, expecting 1 match for: " + InputFile_full,
	          			"Check whether the wildcard pattern needs to be more specific.") );
	         		canRead = false;
    	    	}
    	    	else {
    	    		// Matched a single file so can read.
    	    		InputFile_full = matchingFiles.get(0);
    	    		canRead = true;
    	    	}
    	    }
    	}
    	else {
    		// Filename does not include * so just expand as usual.
    		InputFile_full = IOUtil.verifyPathForOS(
	        	IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        		TSCommandProcessorUtil.expandParameterValue(processor, this,InputFile)) );
    		if ( !IOUtil.fileExists(InputFile_full) ) {
			  		message = "The delimited file \"" + InputFile_full + "\" does not exist.";
			  		++warning_count;
	          		status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	              		message, "Verify that the delimited table file exists." ) );
	          		canRead = false;
    		}
		}
		if ( canRead ) {
			DataTable table = null;
			PropList props = new PropList ( "DataTable" );
			props.set ( "Delimiter", delimiter );
			props.set ( "CommentLineIndicator=#" );	// Skip comment lines.
			props.set ( "TrimInput=True" ); // Trim strings before parsing.
			props.set ( "TrimStrings=True" ); // Trim strings after parsing.
			//props.set ( "ColumnDataTypes=Auto" ); // Automatically determine column data types.
			if ( (SkipLines != null) && !SkipLines.isEmpty() ) {
				props.set ( "SkipLines=" + StringUtil.convertNumberSequenceToZeroOffset(SkipLines) );
			}
			if ( (HeaderLines != null) && !HeaderLines.isEmpty() ) {
				props.set ( "HeaderLines=" + StringUtil.convertNumberSequenceToZeroOffset(HeaderLines) );
			}
			if ( (ColumnNames != null) && !ColumnNames.isEmpty() ) {
				props.set ( "ColumnNames=" + ColumnNames);
			}
			if ( (DateTimeColumns != null) && !DateTimeColumns.isEmpty() ) {
				props.set ( "DateTimeColumns=" + DateTimeColumns);
			}
			if ( (DoubleColumns != null) && !DoubleColumns.isEmpty() ) {
				props.set ( "DoubleColumns=" + DoubleColumns);
			}
			if ( (IntegerColumns != null) && !IntegerColumns.isEmpty() ) {
				props.set ( "IntegerColumns=" + IntegerColumns);
			}
			if ( (TextColumns != null) && !TextColumns.isEmpty() ) {
				props.set ( "TextColumns=" + TextColumns);
			}
			if ( (Top != null) && !Top.isEmpty() ) {
				props.set ( "Top=" + Top);
			}
			Message.printStatus( 2, routine, "Parameter zero index SkipLines=\"" + props.getValue("SkipLines") + "\"");
			Message.printStatus( 2, routine, "Parameter zero index HeaderLines=\"" + props.getValue("HeaderLines") + "\"");
			try {
				// Always try to read object data types (default if not specified is read as all strings).
				props.set("ColumnDataTypes=Auto");
				table = DataTable.parseFile ( InputFile_full, props );

				// Set the table identifier.
				table.setTableID ( TableID );
			}
			catch ( Exception e ) {
				Message.printWarning ( 3, routine, e );
				message = "Unexpected error reading table from delimited file \"" + InputFile_full + "\" (" + e + ").";
				Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that the file exists and is properly formatted - see the log file." ) );
				throw new CommandWarningException ( message );
			}
    	
			// Set the table in the processor.

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
				status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
			}
        
			// Set the property indicating the number of rows in the table.
			if ( (RowCountProperty != null) && !RowCountProperty.isEmpty() ) {
				int rowCount = table.getNumberOfRecords();
				request_params = new PropList ( "" );
				request_params.setUsingObject ( "PropertyName", RowCountProperty );
				request_params.setUsingObject ( "PropertyValue", Integer.valueOf(rowCount) );
				try {
					processor.processRequest( "SetProperty", request_params);
				}
				catch ( Exception e ) {
					message = "Error requesting SetProperty(Property=\"" + RowCountProperty + "\") from processor.";
					Message.printWarning(warning_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
					status.addToLog ( commandPhase,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report the problem to software support." ) );
				}
			}
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Create an empty table and set the ID.
        DataTable table = new DataTable();
        table.setTableID ( TableID );
        setDiscoveryTable ( table );
    }
    
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		throw new CommandWarningException ( message );
	}

    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table ) {
    __table = table;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TableID",
		"InputFile",
		"Delimiter",
		"SkipLines",
		"SkipColumns",
		"HeaderLines",
		"ColumnNames",
		"DateTimeColumns",
		"DoubleColumns",
		"IntegerColumns",
		"TextColumns",
		"Top",
		"RowCountProperty"
	};
	return this.toString(parameters, parameterOrder);
}

}