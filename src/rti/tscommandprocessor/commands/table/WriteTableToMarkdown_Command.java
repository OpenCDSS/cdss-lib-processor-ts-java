// WriteTableToMarkdown_Command - This class initializes, checks, and runs the WriteTableToMarkdown() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
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
import RTi.Util.IO.Markdown.MarkdownWriter;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the WriteTableToMarkdown() command.
*/
public class WriteTableToMarkdown_Command extends AbstractCommand implements FileGenerator
{

/**
Values for use with WriteHeaderComments parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
 * Values for the OutputSchemaFormat parameter.
 */
protected final String _JSONTableSchema = "JSONTableSchema";
protected final String _GoogleBigQuery = "GoogleBigQuery";

/**
Value to use for NaNValue.
*/
protected final String _Blank = "Blank";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Output schema file that is created by this command.
*/
private File __OutputSchemaFile_File = null;

/**
Constructor.
*/
public WriteTableToMarkdown_Command () {
	super();
	setCommandName ( "WriteTableToMarkdown" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	String OutputFile = parameters.getValue ( "OutputFile" );
	String Append = parameters.getValue ( "Append" );
	String TableID = parameters.getValue ( "TableID" );
	//String Delimiter = parameters.getValue ( "Delimiter" );
	String WriteHeaderComments = parameters.getValue ( "WriteHeaderComments" );
	/*
	String WriteColumnNames = parameters.getValue ( "WriteColumnNames" );
	String AlwaysQuoteDateTimes = parameters.getValue ( "AlwaysQuoteDateTimes" );
	String AlwaysQuoteStrings = parameters.getValue ( "AlwaysQuoteStrings" );
	*/
	String OutputSchemaFile = parameters.getValue ( "OutputSchemaFile" );
	String OutputSchemaFormat = parameters.getValue ( "OutputSchemaFormat" );
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

    /*
    if ( (Delimiter != null) && !Delimiter.isEmpty() && (Delimiter.length() != 1) ) {
        message = "The delimiter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "The delimiter, if specified, must be a single character." ) );
    }
    */
	
	if ( (OutputFile == null) || OutputFile.isEmpty() ) {
		message = "The output file must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify an output file." ) );
	}
	else if ( OutputFile.indexOf("${") < 0 ) {
		// Can't check if output file is specified with ${Property}.
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
		// Also check schema file.
		if ( (OutputSchemaFile != null) && (OutputSchemaFile.indexOf("${") < 0) ) {
			try {
	            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, OutputSchemaFile));
				File f = new File ( adjusted_path );
				File f2 = new File ( f.getParent() );
				if ( !f2.exists() ) {
					message = "The output schema file parent directory does not exist: \"" + adjusted_path + "\".";
					warning += "\n" + message;
					status.addToLog ( CommandPhaseType.INITIALIZATION,
						new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Create the output directory." ) );
				}
			}
			catch ( Exception e ) {
				message = "The output schema file:\n" +
				"    \"" + OutputSchemaFile +
				"\"\ncannot be adjusted using the working directory:\n" +
				"    \"" + working_dir + "\".";
				warning += "\n" + message;
				status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that output file and working directory paths are compatible." ) );
			}
		}
	}

	if ( (Append != null) && !Append.equals("") ) {
		if ( !Append.equalsIgnoreCase(_False) && !Append.equalsIgnoreCase(_True) ) {
			message = "The Append parameter \"" + Append + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " (default) or " + _True + "."));
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

    /*
    if ( (WriteColumnNames != null) && !WriteColumnNames.equals("") ) {
        if ( !WriteColumnNames.equalsIgnoreCase(_False) && !WriteColumnNames.equalsIgnoreCase(_True) ) {
            message = "The WriteColumnNames parameter (" + WriteColumnNames + ") must be " + _False +
            " or " + _True + ".";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the parameter as " + _False + " or " + _True + "."));
        }
    }

    if ( (AlwaysQuoteDateTimes != null) && !AlwaysQuoteDateTimes.equals("") ) {
        if ( !AlwaysQuoteDateTimes.equalsIgnoreCase(_False) && !AlwaysQuoteDateTimes.equalsIgnoreCase(_True) ) {
            message = "The AlwaysQuoteDateTimes parameter (" + AlwaysQuoteDateTimes + ") must be " + _False +
            " (default) or " + _True + ".";
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

    if ( (OutputSchemaFormat != null) && !OutputSchemaFormat.equals("")
    	&& !OutputSchemaFormat.equalsIgnoreCase(_JSONTableSchema) && !OutputSchemaFormat.equalsIgnoreCase(_GoogleBigQuery)) {
        message = "The OutputSchemaFormat parameter (" + OutputSchemaFormat + ") must be " + _JSONTableSchema +
        " (default if blank) or " + _GoogleBigQuery + ").";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the parameter as " + _False + " or " + _True + "."));
    }
    */

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(10);
	validList.add ( "OutputFile" );
	validList.add ( "Append" );
	validList.add ( "TableID" );
    validList.add ( "IncludeColumns" );
    validList.add ( "ExcludeColumns" );
	//validList.add ( "Delimiter" );
	validList.add ( "WriteHeaderComments" );
	//validList.add ( "WriteColumnNames" );
	//validList.add ( "AlwaysQuoteDateTimes" );
	//validList.add ( "AlwaysQuoteStrings" );
	validList.add ( "LinkColumns" );
	validList.add ( "NewlineReplacement" );
	validList.add ( "NaNValue" );
	validList.add ( "OutputSchemaFile" );
	validList.add ( "OutputSchemaFormat" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
    List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	return (new WriteTableToMarkdown_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the list of files that were created by this command.
@return the list of files that were created by this command
*/
public List<File> getGeneratedFileList () {
	List<File> list = new ArrayList<>();
	if ( getOutputFile() != null ) {
		list.add ( getOutputFile() );
	}
	if ( getOutputSchemaFile() != null ) {
		list.add ( getOutputSchemaFile() );
	}
	return list;
}

/**
Return the output file generated by this file.  This method is used internally.
@return the output file generated by this file
*/
private File getOutputFile () {
	return __OutputFile_File;
}

/**
Return the output schema file generated by this file.  This method is used internally.
@return the output schema file generated by this file
*/
private File getOutputSchemaFile () {
	return __OutputSchemaFile_File;
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	// Clear the output file.
	
	setOutputFile ( null );
	setOutputSchemaFile ( null );
	
	// Check whether the processor wants output files to be created.

	CommandProcessor processor = getCommandProcessor();
	if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
		Message.printStatus ( 2, routine, "Skipping \"" + toString() + "\" because output is not being created." );
	}

	PropList parameters = getCommandParameters();
	
	CommandStatus status = getCommandStatus();
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
    Boolean clearStatus = new Boolean(true); // Default.
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
		status.clearLog(CommandPhaseType.RUN);
	}
	
	String OutputFile_full = null;

    // Get the table information.
    String TableID = parameters.getValue ( "TableID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	// In discovery mode want lists of tables to include ${Property}.
    	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String OutputFile = parameters.getValue ( "OutputFile" ); // Expanded below.
    OutputFile_full = OutputFile;
	String Append = parameters.getValue ( "Append" );
	boolean append = false;
	if ( (Append != null) && !Append.equals("")) {
	    if ( Append.equalsIgnoreCase(_True) ) {
	        append = true;
	    }
	}
    String IncludeColumns = parameters.getValue ( "IncludeColumns" );
    String [] includeColumns = null;
    if ( IncludeColumns != null ) {
        if ( IncludeColumns.indexOf(",") > 0 ) {
            includeColumns = IncludeColumns.split(",");
        }
        else {
            includeColumns = new String[1];
            includeColumns[0] = IncludeColumns;
        }
        for ( int i = 0; i < includeColumns.length; i++ ) {
            includeColumns[i] = includeColumns[i].trim();
        }
    }
    String ExcludeColumns = parameters.getValue ( "ExcludeColumns" );
    String [] excludeColumns = null;
    if ( ExcludeColumns != null ) {
        if ( ExcludeColumns.indexOf(",") > 0 ) {
            excludeColumns = ExcludeColumns.split(",");
        }
        else {
            excludeColumns = new String[1];
            excludeColumns[0] = ExcludeColumns;
        }
        for ( int i = 0; i < excludeColumns.length; i++ ) {
            excludeColumns[i] = excludeColumns[i].trim();
        }
    }
    String WriteHeaderComments = parameters.getValue ( "WriteHeaderComments" );
    boolean WriteHeaderComments_boolean = false; // Default.
    if ( (WriteHeaderComments != null) && WriteHeaderComments.equalsIgnoreCase(_True) ) {
        WriteHeaderComments_boolean = true;
    }
    /*
    String WriteColumnNames = parameters.getValue ( "WriteColumnNames" );
    boolean WriteColumnNames_boolean = true; // Default.
    if ( (WriteColumnNames != null) && WriteHeaderComments.equalsIgnoreCase(_False) ) {
        WriteColumnNames_boolean = false;
    }
    String Delimiter = parameters.getValue ( "Delimiter" );
    if ( (Delimiter == null) || Delimiter.isEmpty() ) {
    	Delimiter = ","; // Default.
    }
    String AlwaysQuoteDateTimes = parameters.getValue ( "AlwaysQuoteDateTimes" );
    String AlwaysQuoteStrings = parameters.getValue ( "AlwaysQuoteStrings" );
    */
    // Use a LinkedHashMap to preserve order so table column number arrays can align.
    String LinkColumns = parameters.getValue ( "LinkColumns" );
    LinkedHashMap<String,String> linkColumns = new LinkedHashMap<>();
    if ( (LinkColumns != null) && (LinkColumns.length() > 0) && (LinkColumns.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(LinkColumns, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            linkColumns.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String NewlineReplacement = parameters.getValue ( "NewlineReplacement" );
    if ( (NewlineReplacement == null) || NewlineReplacement.isEmpty() ) {
    	NewlineReplacement = " "; // Default so that markdown formatting does not break.
    }
    String NaNValue = parameters.getValue ( "NaNValue" );
    if ( NaNValue != null ) {
        if ( NaNValue.equals("") ) {
            NaNValue = null; // Will result in "NaN" in output.
        }
        else if ( NaNValue.equals(_Blank) ) {
            NaNValue = "";
        }
    }
    String OutputSchemaFile = parameters.getValue ( "OutputSchemaFile" );
    String OutputSchemaFormat = parameters.getValue ( "OutputSchemaFormat" );
    String outputSchemaFormat = OutputSchemaFormat;
    if ( (outputSchemaFormat == null) || outputSchemaFormat.isEmpty() ) {
    	outputSchemaFormat = _JSONTableSchema;
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

    // Check that link columns exist in the table.
    for ( Map.Entry<String,String> entry : linkColumns.entrySet() ) {
       	String linkColumn = entry.getKey();
       	String textColumn = entry.getValue();
       	try {
       		table.getFieldIndex(linkColumn);
       	}
       	catch ( Exception e ) {
       		message = "The link column \"" + linkColumn + "\" is not in the table.";
        	Message.printWarning ( warning_level,
        	MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        	status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
            	message, "Verify that the link column name is correct." ) );
       	}
       	try {
       		table.getFieldIndex(textColumn);
       	}
       	catch ( Exception e ) {
       		message = "The link text column \"" + textColumn + "\" is not in the table.";
        	Message.printWarning ( warning_level,
        	MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        	status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
            	message, "Verify that the link text column name is correct." ) );
       	}
    }

    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings for command parameters.";
        Message.printWarning ( 2,
        MessageUtil.formatMessageTag(command_tag, ++warning_count),
        routine,message);
        throw new InvalidCommandParameterException ( message );
    }

    try {
    	// Now try to write.

        OutputFile_full = OutputFile;

		// Convert to an absolute path.
		OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            	TSCommandProcessorUtil.expandParameterValue(processor, this,OutputFile)) );
		String outputSchemaFile = null;
		if ( (OutputSchemaFile != null) && !OutputSchemaFile.isEmpty() ) {
			outputSchemaFile = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            	TSCommandProcessorUtil.expandParameterValue(processor, this,OutputSchemaFile)) );
		}
		Message.printStatus ( 2, routine, "Writing table to file \"" + OutputFile_full + "\"" );
		HashMap<String,Object> writeProps = new HashMap<>();
		//writeProps.put("AlwaysQuoteDateTimes", AlwaysQuoteDateTimes);
		//writeProps.put("AlwaysQuoteStrings", AlwaysQuoteStrings);
		writeProps.put("IncludeColumns", includeColumns);
		writeProps.put("ExcludeColumns", excludeColumns);
		writeProps.put("LinkColumns", linkColumns );
		writeProps.put("NewlineReplacement", StringUtil.literalToInternal(NewlineReplacement));
		writeProps.put("NaNValue", NaNValue);
		warning_count = writeMarkdown ( table,
			OutputFile_full,
			append,
			//Delimiter,
			WriteHeaderComments_boolean,
			//WriteColumnNames_boolean,
			writeProps,
		    outputSchemaFile, outputSchemaFormat,
		    warning_level, command_tag, warning_count );
		// Save the output file name.
		setOutputFile ( new File(OutputFile_full));
		if ( (outputSchemaFile != null) && !outputSchemaFile.isEmpty() ) {
			setOutputSchemaFile ( new File(outputSchemaFile));
		}
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
@param file output file created by this command
*/
private void setOutputFile ( File file ) {
	__OutputFile_File = file;
}

/**
Set the output schema file that is created by this command.  This is only used internally.
@param file output schema file created by this command
*/
private void setOutputSchemaFile ( File file ) {
	__OutputSchemaFile_File = file;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"TableID",
		"OutputFile",
		"Append",
    	"IncludeColumns",
    	"ExcludeColumns",
		"WriteHeaderComments",
		//"WriteColumnNames",
		//"Delimiter",
		//"AlwaysQuoteDateTimes",
		//"AlwaysQuoteStrings",
		"LinkColumns",
		"NewlineReplacement",
		"NaNValue",
		"OutputSchemaFile",
		"OutputSchemaFormat"
	};
	return this.toString(parameters, parameterOrder);
}

/**
 * Write the table schema file using Google Big Query Schema.
 * See:  https://cloud.google.com/bigquery/docs/reference/rest/v2/tables
 * @table table to write
 * @param outputSchemaFile path/name of schema file to write
 */
private void writeGoogleBigQueryTableSchema ( DataTable table, String outputSchemaFile ) throws IOException {
	PrintWriter out = new PrintWriter( new BufferedWriter(new FileWriter(outputSchemaFile)));
	// Brute force the output.
	// TODO SAM 2017-01-18 move to a general class later.
	String nl = System.getProperty("line.separator");
	String i1 = "  ", i2 = "    ", i3 = "      ", i4 = "        ";
	out.print ( "{" + nl );
	out.print ( i1 + "\"schema\": {" + nl );
	out.print ( i2 + "\"fields\": [" + nl );
	String colName, dataTypeSchema;
	//String colDescription;
	int colType;
	int irow;
	for ( int icol = 0; icol < table.getNumberOfFields(); icol++ ) {
		colName = table.getFieldName(icol);
		// TODO sam 2017-01-18 need to enable.
		//colDescription = table.getFieldDescription(icol);
		colType = table.getFieldDataType(icol);
		dataTypeSchema = "STRING"; // Default.
		if ( colType == TableField.DATA_TYPE_BOOLEAN ) {
			dataTypeSchema = "BOOLEAN";
		}
		else if ( colType == TableField.DATA_TYPE_DATE ) {
			dataTypeSchema = "DATETIME";
		}
		else if ( colType == TableField.DATA_TYPE_DATETIME ) {
			// Figure out the most precise date/time.
			int precMin = DateTime.PRECISION_YEAR;
			Object o;
			DateTime dt;
			for ( irow = 0; irow < table.getNumberOfRecords(); irow++ ) {
				try {
					o = table.getFieldValue(irow, icol);
					if ( o != null ) {
						dt = (DateTime)o;
						if ( dt.getPrecision() < precMin ) {
							precMin = dt.getPrecision();
						}
					}
				}
				catch ( Exception e ) {
					// Ignore.
				}
			}
			if ( precMin >= DateTime.PRECISION_YEAR ) {
				// Only date.
				dataTypeSchema = "DATE";
			}
			else {
				// Date/time.
				dataTypeSchema = "DATETIME";
			}
		}
		else if ( colType == TableField.DATA_TYPE_DOUBLE ) {
			dataTypeSchema = "FLOAT";
		}
		else if ( colType == TableField.DATA_TYPE_FLOAT ) {
			dataTypeSchema = "FLOAT";
		}
		else if ( colType == TableField.DATA_TYPE_INT ) {
			dataTypeSchema = "INTEGER";
		}
		else if ( colType == TableField.DATA_TYPE_LONG ) {
			dataTypeSchema = "INTEGER";
		}
		else if ( colType == TableField.DATA_TYPE_SHORT ) {
			dataTypeSchema = "INTEGER";
		}
		if ( icol > 0 ) {
			out.print("," + nl);
		}
		out.print(i3 + "{" + nl);
		out.print(i4 + "\"name\": \"" + colName + "\"" );
		//out.print(i4 + "\"description\": \"" + colDescription + "\"" );
		out.print("," + nl + i4 + "\"type\": \"" + dataTypeSchema + "\"" );
		out.print(nl + i3 + "}");
	}
	out.print ( nl + i2 + "]" + nl + i1 + "}" + nl + "}");
	out.close();
}

/**
 * Write the table schema file using JSON Table Schema.
 * See:  http://specs.frictionlessdata.io/json-table-schema/
 * @table table to write
 * @param outputSchemaFile path/name of schema file to write
 */
private void writeJSONTableSchema ( DataTable table, String outputSchemaFile ) throws IOException {
	PrintWriter out = new PrintWriter( new BufferedWriter(new FileWriter(outputSchemaFile)));
	// Brute force the output.
	// TODO SAM 2017-01-18 move to a general class later.
	String nl = System.getProperty("line.separator");
	String i1 = "  ", i2 = "    ", i3 = "      ";
	out.print ( "{" + nl );
	out.print ( i1 + "\"fields\": [" + nl );
	String colName, dataTypeSchema;
	//String colDescription;
	int colType;
	int irow;
	//TableField field;
	for ( int icol = 0; icol < table.getNumberOfFields(); icol++ ) {
		colName = table.getFieldName(icol);
		// TODO sam 2017-01-18 need to enable
		//colDescription = table.getFieldDescription(icol);
		colType = table.getFieldDataType(icol);
		dataTypeSchema = "string"; // default
		if ( colType == TableField.DATA_TYPE_BOOLEAN ) {
			dataTypeSchema = "boolean";
		}
		else if ( colType == TableField.DATA_TYPE_DATE ) {
			dataTypeSchema = "datetime";
		}
		else if ( colType == TableField.DATA_TYPE_DATETIME ) {
			// Figure out the most precise date/time.
			int precMin = DateTime.PRECISION_YEAR;
			Object o;
			DateTime dt;
			for ( irow = 0; irow < table.getNumberOfRecords(); irow++ ) {
				try {
					o = table.getFieldValue(irow, icol);
					if ( o != null ) {
						dt = (DateTime)o;
						if ( dt.getPrecision() < precMin ) {
							precMin = dt.getPrecision();
						}
					}
				}
				catch ( Exception e ) {
					// Ignore.
				}
			}
			if ( precMin == DateTime.PRECISION_YEAR ) {
				// Only date.
				dataTypeSchema = "gyear";
			}
			else if ( precMin == DateTime.PRECISION_MONTH ) {
				// Only date.
				dataTypeSchema = "gyearmonth";
			}
			else if ( precMin == DateTime.PRECISION_DAY ) {
				// Only date.
				dataTypeSchema = "date";
			}
			else {
				// Includes time.
				dataTypeSchema = "datetime";
			}
		}
		else if ( colType == TableField.DATA_TYPE_DOUBLE ) {
			dataTypeSchema = "number";
		}
		else if ( colType == TableField.DATA_TYPE_FLOAT ) {
			dataTypeSchema = "number";
		}
		else if ( colType == TableField.DATA_TYPE_INT ) {
			dataTypeSchema = "integer";
		}
		else if ( colType == TableField.DATA_TYPE_LONG ) {
			dataTypeSchema = "integer";
		}
		else if ( colType == TableField.DATA_TYPE_SHORT ) {
			dataTypeSchema = "integer";
		}
		if ( icol > 0 ) {
			out.print("," + nl);
		}
		out.print(i2 + "{" + nl);
		out.print(i3 + "\"name\": \"" + colName + "\"" );
		//out.print(i3 + "\"description\": \"" + colDescription + "\"" );
		out.print("," + nl + i3 + "\"type\": \"" + dataTypeSchema + "\"" );
		out.print(nl + i2 + "}");
	}
	out.print ( nl + i1 + "]" + nl + "}");
	out.close();
}

//@param delimiter delimiter character
//@param writeColumnNames indicates whether column names should be written
//do not handle comments)
/**
Write a table to a markdown file.
@param table Table to write.
@param outputFile path to the file to write.
@param append whether to append to the file
@param writeHeaderComments indicates whether header comments should be written (some software like Esri ArcGIS
@param writeProps properties to control the write, passed to library code
@exception IOException if there is an error writing the file.
*/
private int writeMarkdown ( DataTable table,
	String outputFile,
	boolean append,
	//String delimiter,
	boolean writeHeaderComments,
	//boolean writeColumnNames,
	HashMap<String,Object> writeProps,
	String outputSchemaFile, String outputSchemaFormat,
	int warning_level, String command_tag, int warning_count )
throws IOException {
	String routine = getClass().getSimpleName() + ".writeTable";
	String message;

	// Clear the output file.

	setOutputFile ( null );
	setOutputSchemaFile ( null );

	// Check whether the processor wants output files to be created.

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();

    // Get the comments to add to the top of the file.

    List<String> outputCommentsList = new ArrayList<>();
    if ( writeHeaderComments ) {
        // Get the comments to be written at the top of the file.
        // Put the standard header at the top of the file.
        outputCommentsList = IOUtil.formatCreatorHeader ( "", 80, false );
        // Additional comments to add.
        try {
            Object o = processor.getPropContents ( "OutputComments" );
            // Comments are available so use them.
            if ( o != null ) {
            	@SuppressWarnings("unchecked")
				List<String> o2 = (List<String>)o;
            	outputCommentsList.addAll(o2);
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
		Message.printStatus ( 2, routine, "Writing Markdown table file \"" + outputFile + "\"" );
		MarkdownWriter writer = new MarkdownWriter(outputFile, append);
		//writeTable(writer, table);
		writeMarkdownTable (
			writer,
			table,
			outputCommentsList,
    		writeProps );
		writer.closeFile();
		if ( (outputSchemaFile != null) && !outputSchemaFile.isEmpty() ) {
			if ( outputSchemaFormat.equalsIgnoreCase(_GoogleBigQuery) ) {
				writeGoogleBigQueryTableSchema ( table, outputSchemaFile );
			}
			else {
				writeJSONTableSchema ( table, outputSchemaFile );
			}
		}
	}
	catch ( Exception e ) {
		message = "Unexpected error writing table to file \"" + outputFile + "\" (" + e + ")";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Software error - report problem to support." ) );
	}
	return warning_count;
}

/**
Writes a table to a Markdown file.  If the data items contain the | delimiter, the | will be written as a literal.
@param writer the MarkdownWriter to write
@param comments a list of Strings to put at the top of the file as comments,
@param writeProps additional properties to control writing:
<ul>
<li>IncludeColumns - array of String containing column names to include</li>
<li>ExcludeColumns - array of String containing column names to exclude</li>
<li>LinkColumns - a map of link column and link text column.</li>
<li>NaNValue - value to replace NaN in output (no property or null will result in NaN being written).</li>
<li>NewlineReplacement - if not null, replace newlines in string table values with this replacement string (which can be an empty string).
This is needed to ensure that the delimited file does not include unexpected newlines in mid-row.
Checks are done for \r\n, then \n, then \r to catch all combinations.
This can be a performance hit and mask data issues so the default is to NOT replace newlines.</li>
</ul>
*/
public void writeMarkdownTable (
	MarkdownWriter writer,
	DataTable table,
	List<String> comments,
    HashMap<String,Object> writeProps )
throws Exception {
	String routine = getClass().getSimpleName() + ".writeMarkdownTable";
	
	if (writer == null) {
		return;
	}
	if ( comments == null ) {
	    comments = new ArrayList<>(); // To simplify logic below.
	}

	// Output string to use for NaN values.
	Object propO = writeProps.get("NaNValue");
	String NaNValue = "NaN"; // Default.
	if ( propO != null ) {
	    NaNValue = (String)propO;
	}
	
	// String to use for newlines, can be "", by default don't replace:
	// - default is to not replace newlines
	propO = writeProps.get("NewlineReplacement");
	String NewlineReplacement = null;
	if ( propO != null ) {
		NewlineReplacement = (String)propO;
	}
	String newlineReplacement = null;
	if ( NewlineReplacement != null ) {
		newlineReplacement = NewlineReplacement;
	}
	
	// Check whether include and exclude columns are indicated.
	String [] includeColumns = new String[0];
	String [] excludeColumns = new String[0];
	propO = writeProps.get("IncludeColumns");
	if ( propO != null ) {
		includeColumns = (String [])propO;
	}
	propO = writeProps.get("ExcludeColumns");
	if ( propO != null ) {
		excludeColumns = (String [])propO;
	}

	// Get the link map:
	// - theoretically, use LinkedHashMap to preserve the order so that column positions can be determined
	// - practically, the lookup of table column number from link text column must also use a map
	// - TODO smalers 2022-11-30 may just switch to HashMap if code is working because LinkedHashMap is not needed
	propO = writeProps.get("LinkColumns");
	LinkedHashMap<String,String> linkColumns = null;
	HashMap<String,Integer> linkTextMap = null;
	if ( propO != null ) {
		linkColumns = (LinkedHashMap<String,String>)propO;
		linkTextMap = new HashMap<>();
		
		// Get the associated table columns for the links:
		// - warnings are generated in the main run method
		// - if columns are not found, just format the link as text
        for ( Map.Entry<String,String> entry : linkColumns.entrySet() ) {
        	String textColumn = entry.getValue();
        	try {
        		linkTextMap.put(textColumn,new Integer(table.getFieldIndex(textColumn)));
        	}
        	catch ( Exception e ) {
        		// Don't need to do anything - link won't have text.
        	}
        }
	}
	
	int irow = 0, icol = 0;
	try {

    	// If any comments have been passed in, print them at the top of the file.
    	if ( (comments != null) && (comments.size() > 0) ) {
    		writer.comment(comments);
    	}

    	int cols = table.getNumberOfFields();
    	if (cols == 0) {
    		Message.printWarning(3, routine, "Table has 0 columns!  Nothing will be written.");
    		return;
    	}

    	// Determine which columns should be written:
    	// - default is to write all
       	boolean [] columnOkToWrite = new boolean[cols];
        for ( icol = 0; icol < cols; icol++) {
        	if ( includeColumns.length == 0 ) {
        		// Initialize all columns will be written.
        		columnOkToWrite[icol] = true;
        	}
        	else {
        		// Initialize all to false and only include columns that are requested, checked below.
        		columnOkToWrite[icol] = false;
        	}
        }
        // Loop through the table columns and check whether any are specifically included or excluded.
       	if ( (includeColumns.length != 0) || (excludeColumns.length != 0) ) {
       		for ( icol = 0; icol < cols; icol++) {
       			// First check included names.
       			for ( String includeColumn : includeColumns ) {
       				if ( includeColumn.equals(table.getFieldName(icol)) ) {
       					columnOkToWrite[icol] = true;
       				}
       			}
       			for ( String excludeColumn : excludeColumns ) {
       				if ( excludeColumn.equals(table.getFieldName(icol)) ) {
       					columnOkToWrite[icol] = false;
       				}
       			}
       		}
       	}

    	// Write the table header.

        int nonBlank = 0; // Number of non-blank table headings.
        boolean writeColumnNames = true; // Always true for Markdown.
    	if (writeColumnNames) {
    	    // First determine if any headers are non blank:
    		// - only write headers if requested and have at least one non-blank header
    		// - TODO smalers 2021-10-24 is this left over?  All columns should typically have names for lookups.
            for ( icol = 0; icol < cols; icol++) {
            	if ( columnOkToWrite[icol] ) {
            		if ( table.getFieldName(icol).length() > 0 ) {
            			++nonBlank;
            		}
            	}
            }
            if ( nonBlank > 0 ) {
            	// Write the column headings.

        		// Write the delimiter to start the row.
        		writer.tableRowStart();
        		for ( icol = 0; icol < cols; icol++) {
        			if ( columnOkToWrite[icol] ) {
        				// Count of output columns, so know when to print the delimiter.
        				writer.tableHeader(table.getFieldName(icol));
        			}
        		}
        		writer.tableRowEnd();

        		// Also write the column format indicators.
        		writer.tableRowStart();
        		for ( icol = 0; icol < cols; icol++) {
        			if ( columnOkToWrite[icol] ) {
        				// Count of output columns, so know when to print the delimiter.
        				writer.tableHeaderSeparator();
        			}
        		}
        		writer.tableRowEnd();
            }
    	}

    	// Write the table rows:
    	// - always write the delimiter after the cell
    	
    	String delimiter = "|";
    	int rows = table.getNumberOfRecords();
    	String cell;
    	int tableFieldType;
    	int precision;
    	Object fieldValue;
    	Double fieldValueDouble;
    	Float fieldValueFloat;
    	int icolOut = 0; // Count of columns actually written.
    	String columnName = null;
    	for ( irow = 0; irow < rows; irow++) {
    		icolOut = 0;
    		// Start a row.
    		writer.tableRowStart();
    		for ( icol = 0; icol < cols; icol++) {
       			if ( !columnOkToWrite[icol] ) {
       				continue;
       			}
       			++icolOut;
    		    tableFieldType = table.getFieldDataType(icol);
    		    precision = table.getFieldPrecision(icol);
    		    fieldValue = table.getFieldValue(irow,icol);
    		    if ( fieldValue == null ) {
    		        cell = "";
    		    }
    		    else if ( table.isColumnArray(tableFieldType) ) {
                	// The following formats the array for display in UI table.
                	cell = table.formatArrayColumn(irow,icol);
                }
    		    else if ( tableFieldType == TableField.DATA_TYPE_FLOAT ) {
    		    	// Handle specifically in order to format precision and handle NaN value.
                    fieldValueFloat = (Float)fieldValue;
                    if ( fieldValueFloat.isNaN() ) {
                        cell = NaNValue;
                    }
                    else if ( precision >= 0 ) {
                        // Format according to the precision if floating point.
                        cell = StringUtil.formatString(fieldValue,"%." + precision + "f");
                    }
                    else {
                        // Use default formatting.
                        cell = "" + fieldValue;
                    }
    		    }
    		    else if ( tableFieldType == TableField.DATA_TYPE_DOUBLE ) {
    		    	// Handle specifically in order to format precision and handle NaN value.
    		        fieldValueDouble = (Double)fieldValue;
    		        if ( fieldValueDouble.isNaN() ) {
    		            cell = NaNValue;
    		        }
    		        else if ( precision >= 0 ) {
                        // Format according to the precision if floating point.
                        cell = StringUtil.formatString(fieldValue,"%." + precision + "f");
    		        }
    		        else {
    		            // Use default formatting.
                        cell = "" + fieldValue;
    		        }
                }
                else {
                    // Use default formatting from object toString().
                    cell = "" + fieldValue;
                }
    		    // TODO smalers 2022-02-16 could use ticks here for formatting if enabled.
    		    // Figure out if the initial cell needs to be quoted.
    			// Surround the values with double quotes if:
    		    // 1) the field contains the delimiter
    		    // 2) alwaysQuoteStrings=true
    		    // 3) the field contains a double quote (additionally replace " with "")
    		    if ( tableFieldType == TableField.DATA_TYPE_STRING ) {
    		    	// Determine if the string is a URL:
    		    	// - must start with http: or https:
    		    	// - may not be a full URL because no http://hostname
    		    	// - can be absolute or relative
    		    	if ( cell.startsWith("http:") || cell.startsWith("https:") ) {
    		    		// Determine whether the first part of the URL is a hostname or IP address:
    		    		// https://some.domain/some/path
    		    		// https:some/relative/path
    		    		int colonPos = cell.indexOf(":");
    		    		int colonSlash2Pos = cell.indexOf("://");
    		    		boolean isRelative = false;
    		    		String link = null;
    		    		if ( colonSlash2Pos < 0 ) {
    		    			// No :// so relative path.
    		    			isRelative = true;
    		    			link = cell.substring(colonPos + 1).trim();
    		    		}
    		    		else {
    		    			// Else, has :// so assume a ful URL.
    		    			link = cell;
    		    		}
    		    		Integer linkTextColNum = null;
    		    		Object linkText = link;
    		    		columnName = table.getFieldName(icol);
    		    		String linkTextColumn = linkColumns.get(columnName);
    		    		if ( linkTextColumn != null ) {
    		    			// A link is available for the column.
    		    			linkTextColNum = linkTextMap.get(linkTextColumn);
    		    			if ( linkTextColNum != null ) {
    		    				// Link text column exists in the table.
    		    				linkText = table.getFieldValue(irow, linkTextColNum);
    		    				if ( linkText == null ) {
    		    					// Null string so use the link URL.
    		    					linkText = link;
    		    				}
    		    			}
    		    		}
    		    		// Format the URL as a link.  If a relative path (no periods in first part), format as a relative URL.
    		    		cell = "[" + linkText + "](" + link + ")";
    		    	}
    		    }
    		    else if ( tableFieldType == TableField.DATA_TYPE_DATETIME ) {
    		    	// No special handling.
    		    }
    			if ( (cell.indexOf(delimiter) > -1) ) {
    				// Always have to protect delimiter character in the cell string.
    				// Replace all | instances with `|`.
    				cell = cell.replace("|", "`|`");
    			}
    			if ( (tableFieldType == TableField.DATA_TYPE_STRING) && (newlineReplacement != null) ) {
    			    // Replace newline strings with the specified string.
    			    cell = cell.replace("\r\n", newlineReplacement); // Windows/Mac use 2-characters.
    			    cell = cell.replace("\n", newlineReplacement); // *NIX
    			    cell = cell.replace("\r", newlineReplacement); // To be sure.
    			}
    			writer.tableCell ( cell );
    		}
    		writer.tableRowEnd();
    	}
	}
	catch ( Exception e ) {
	    // Log and rethrow the exception.
	    Message.printWarning(3, routine, "Unexpected error writing table row [" + irow + "][" + icol + "] (" + e + ")." );
	    Message.printWarning(3, routine, e);
	    throw ( e );
	}
}

}