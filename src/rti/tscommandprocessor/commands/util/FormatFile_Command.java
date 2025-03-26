// FormatFile_Command - This class initializes, checks, and runs the FormatFile() command.

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

package rti.tscommandprocessor.commands.util;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;

/**
This class initializes, checks, and runs the FormatFile() command.
*/
public class FormatFile_Command extends AbstractCommand
implements FileGenerator
{

/**
Data members used for parameter values (these have been replaced with _Warn, etc. instead).
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Data members used for ContentType parameter.
- these are general types to inform auto-formatting
- may add mime-types for specific formatting
- TODO smalers 2019-10=19 need to spend more time packaging images
*/
protected final String _Csv = "Csv";
protected final String _Image = "Image";
protected final String _Json = "Json";
protected final String _Text = "Text";

/**
Data members used for OutputType parameter.
- these are general types to inform formatting
*/
protected final String _Cgi = "Cgi";
protected final String _Html = "Html";
//protected final String _Text = "Text";  // Defined above.

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public FormatFile_Command ()
{	super();
	setCommandName ( "FormatFile" );
}

/**
 * Add lines after main content.
 * @param fout PrintWriter for output
 * @param contentType general content type from ContentType parameter
 * @param outputType output format type from OutputType
 */
private void addAutoAppendLines(PrintWriter fout, String contentType, String outputType ) {
	if ( outputType.equalsIgnoreCase(_Cgi) ) {
		// Format for CGI:
		// - don't need to do anything
	}
	else if ( outputType.equalsIgnoreCase(_Html) ) {
		if ( contentType.equalsIgnoreCase(_Image) ) {
		}
		else {
			// Text
			fout.println("</pre>");
		}
		fout.println("</body>");
		fout.println("</html>");
	}
	else {
		// Default is text, nothing to be done.
	}
}

/**
 * Add lines before main content.
 * @param fout PrintWriter for output
 * @param inputFile the input file being processed, needed to check file extension
 * @param contentType general content type from ContentType parameter
 * @param outputType output format type from OutputType
 */
private void addAutoPrependLines(PrintWriter fout, String inputFile, String contentType, String outputType ) {
	if ( outputType.equalsIgnoreCase(_Cgi) ) {
		// Format for CGI.
		fout.println("Content-type: " + lookupMimeType(contentType, inputFile) );
		fout.println("");
	}
	else if ( outputType.equalsIgnoreCase(_Html) ) {
		// The HTML format is very basic, just enough to view the content:
		// - better formatting requires using prepend and append files
		fout.println("<!DOCTYPE html>");
		fout.println("<html>");
		fout.println("<head>");
		fout.println("</head>");
		fout.println("<body>");
		if ( contentType.equalsIgnoreCase(_Image) ) {
			// TODO smalers 2019-10-19 Need to figure out how to reference another file:
			// - for now use the path but it probably won't work
			fout.println("<img src=" + inputFile + ">");
		}
		else {
			// Text - format as is without line wrapping.
			fout.println("<pre>");
		}
	}
	else {
		// Default is text, nothing to be done.
	}
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
{	String InputFile = parameters.getValue ( "InputFile" );
	String ContentType = parameters.getValue ( "ContentType" );
	String AutoFormat = parameters.getValue ( "AutoFormat" );
	String OutputType = parameters.getValue ( "OutputType" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the parent directories or files is not checked
	// because the files may be created dynamically after the command is edited.

	if ( (InputFile == null) || (InputFile.length() == 0) ) {
		message = "The input file to format must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the first file name."));
	}
	if ( (ContentType != null) && !ContentType.equals("") &&
		!ContentType.equalsIgnoreCase(_Csv) &&
		!ContentType.equalsIgnoreCase(_Image) &&
		!ContentType.equalsIgnoreCase(_Json) &&
		!ContentType.equalsIgnoreCase(_Text) ) {
		message = "The ContentType parameter \"" + ContentType + "\" is not a valid value.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the parameter as " +
				_Csv + ", " +
				_Image + ", " +
				_Json + ", " +
				_Text + " (default)."));
	}
    if ( (AutoFormat != null) && !AutoFormat.equals("") && !AutoFormat.equalsIgnoreCase(_False) &&
        !AutoFormat.equalsIgnoreCase(_True) ) {
        message = "The AutoFormat parameter \"" + AutoFormat + "\" is not a valid value.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the parameter as " + _False + " or " + _True + " (default)"));
    }
	if ( (OutputType != null) && !OutputType.equals("") &&
		!OutputType.equalsIgnoreCase(_Cgi) &&
		!OutputType.equalsIgnoreCase(_Html) &&
		!OutputType.equalsIgnoreCase(_Text) ) {
		message = "The OutputType parameter \"" + OutputType + "\" is not a valid value.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the parameter as " + _Cgi + ", " + _Html + ", " + _Text + " (default)."));
	}
	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
		message = "The output file be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the output file name."));
	}
	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(7);
	validList.add ( "InputFile" );
	validList.add ( "PrependFile" );
	validList.add ( "AppendFile" );
	validList.add ( "ContentType" );
	validList.add ( "AutoFormat" );
	validList.add ( "OutputType" );
	validList.add ( "OutputFile" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed.
	Prop prop = IOUtil.getProp("DiffProgram");
	String diffProgram = null;
	if ( prop != null ) {
		diffProgram = prop.getValue();
	}
	return (new FormatFile_JDialog ( parent, this, diffProgram )).ok();
}

/**
Return the list of files that were created by this command.
@return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList () {
    List<File> list = new ArrayList<>(1);
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

// Use parent parseCommmand.

/**
 * Lookup the mime type to use for the header.
 * @param contentType the general content type ("Text", etc.).
 * @param inputFile input file being processed, used to check image extensions
 * @return the mime type 
 */
private String lookupMimeType(String contentType, String inputFile) {
	if ( contentType.equalsIgnoreCase("csv") ) {
		return "text/csv";
	}
	else if ( contentType.equalsIgnoreCase("image") ) {
		String ext = IOUtil.getFileExtension(inputFile);
		if ( ext.equalsIgnoreCase("gif") ) {
			ext = "gif";
		}
		else if ( ext.equalsIgnoreCase("png") ) {
			ext = "png";
		}
		else if ( ext.equalsIgnoreCase("jpg") || ext.equalsIgnoreCase("jpeg") ) {
			ext = "jpeg";
		}
		return "image/" + ext;
	}
	else if ( contentType.equalsIgnoreCase("json") ) {
		return "application/json";
	}
	else if ( contentType.equalsIgnoreCase("text") ) {
		return "text/plain";
	}
	else {
		return null;
	}
}

/**
Run the command.
@param command_line Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
    CommandProcessor processor = getCommandProcessor();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
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

    // Clear the output file.
    setOutputFile ( null );
	
	String InputFile = parameters.getValue ( "InputFile" );
	String PrependFile = parameters.getValue ( "PrependFile" );
	String AppendFile = parameters.getValue ( "AppendFile" );
    String ContentType = parameters.getValue ( "ContentType" );
    if ( (ContentType == null) || ContentType.isEmpty() ) {
    	ContentType = _Text;
    }
	String AutoFormat = parameters.getValue ( "AutoFormat" );
    boolean AutoFormat_boolean = false; // Default
    if ( (AutoFormat != null) && AutoFormat.equalsIgnoreCase(_True)) {
        AutoFormat_boolean = true;
    }
	String OutputType = parameters.getValue ( "OutputType" );
    if ( (OutputType == null) || OutputType.isEmpty() ) {
    	OutputType = _Text;
    }
	String OutputFile = parameters.getValue ( "OutputFile" );

	String InputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            TSCommandProcessorUtil.expandParameterValue(processor, this, InputFile) ) );
	String PrependFile_full = PrependFile;
	String AppendFile_full = AppendFile;
	String OutputFile_full = OutputFile;
	if ( !IOUtil.fileExists(InputFile_full) ) {
		message = "Input file \"" + InputFile_full + "\" does not exist.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Verify that the file exists at the time the command is run."));
	}
	if ( (PrependFile != null) && !PrependFile.isEmpty() ) {
		PrependFile_full = IOUtil.verifyPathForOS(
       		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
           		TSCommandProcessorUtil.expandParameterValue(processor, this, PrependFile) ) );
		if ( !IOUtil.fileExists(PrependFile_full) ) {
			message = "Prepend file \"" + PrependFile_full + "\" does not exist.";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count), routine, message );
			status.addToLog(CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that the file exists at the time the command is run."));
		}
	}
	if ( (AppendFile != null) && !AppendFile.isEmpty() ) {
		AppendFile_full = IOUtil.verifyPathForOS(
       		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
           		TSCommandProcessorUtil.expandParameterValue(processor, this, AppendFile) ) );
		if ( !IOUtil.fileExists(AppendFile_full) ) {
			message = "Append file \"" + AppendFile_full + "\" does not exist.";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count), routine, message );
			status.addToLog(CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that the file exists at the time the command is run."));
		}
	}
	if ( (OutputFile != null) && !OutputFile.isEmpty() && !OutputFile.equalsIgnoreCase("stdout") ) {
		// Only expand the filename if not "stdout".
		OutputFile_full = IOUtil.verifyPathForOS(
       		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
           		TSCommandProcessorUtil.expandParameterValue(processor, this, OutputFile) ) );
	}
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Output file.
	PrintWriter fout = null;
	try {
		// Open the output file.
		String temporaryFile = null;
		try {
			if ( OutputFile.equalsIgnoreCase("stdout") ) {
				fout = new PrintWriter ( System.out );
			}
			else {
				if ( OutputFile_full.equals(InputFile_full) ) {
					// Output file is the same as the input file so use a temporary output file and rename later. 
					temporaryFile = IOUtil.tempFileName();
					fout = new PrintWriter ( new FileOutputStream( temporaryFile, false ) );
				}
				else {
					// Output file is different from input file so just open.
					fout = new PrintWriter ( new FileOutputStream( OutputFile_full, false ) );
				}
			}
		}
		catch ( Exception e ) {
	    	message = "Error opening the output file (" + e + ").";
        	Message.printWarning ( warning_level,
            	MessageUtil.formatMessageTag(
            	command_tag, ++warning_count),
            	routine,message);
        	throw new CommandException ( message );
		}
		// Process the content before the body of the file:
		// - the following indicates whether the content needs including below,
		//   some formats must handle with the prepend content
		if ( AutoFormat_boolean ) {
			// Auto-formatting the prepend content
			addAutoPrependLines ( fout, InputFile, ContentType, OutputType );
		}
		else if ( (PrependFile != null) && !PrependFile.isEmpty() ) {
			// Prepend file is provided.
            List<String> prependFileLines = IOUtil.fileToStringList(PrependFile_full);
            for ( String s : prependFileLines ) {
            	s = TSCommandProcessorUtil.expandParameterValue(processor,this,s);
            	fout.println(s);
            }
		}
		// Process the content of the body of the file:
		// - currently just transfer without processing
		// - may also expand properties
		if ( ContentType.equalsIgnoreCase(_Image) ) {
			// Image will have been handled in prepend content if HTML,
			// and otherwise needs to be output in binary form for CGI.
			if ( OutputType.equalsIgnoreCase(_Cgi) ) {
				// Print the contents of the image file:
				// - TODO smalers 2019-10-18 not sure if this is correct, need to test
				//   probably need to use FileOutputStream and convert everything to bytes
				InputStream bis = new BufferedInputStream(new FileInputStream(InputFile_full));
				int bsize = 20480; // 20K
				byte [] buffer = new byte[bsize];
				while ((bis.read(buffer)) != -1 ) {
					fout.print(buffer);
				}
				bis.close();
			}
		}
		else {
			// All text content is printed:
			// - TODO smalers 2019-10-19 evaluate if some formats should rendered
			//   nicely by default, for example JSON using a JavaScript package to pretty print,
			//   CSV displaying nicely in a table,
			//   and GeoJSON displaying in a simple map, similar to GitHub basic renderers
			List<String> fileLines = IOUtil.fileToStringList(InputFile_full);
			for ( String s : fileLines) {
				s = TSCommandProcessorUtil.expandParameterValue(processor,this,s);
				fout.println(s);
			}
		}
		// Process the content after the body of the file.
		if ( AutoFormat_boolean ) {
			// Auto-formatting the append content.
			addAutoAppendLines ( fout, ContentType, OutputType );
		}
		else if ( (AppendFile != null) && !AppendFile.isEmpty() ) {
			// Append file is provided.
            List<String> appendFileLines = IOUtil.fileToStringList(AppendFile_full);
            for ( String s : appendFileLines ) {
            	s = TSCommandProcessorUtil.expandParameterValue(processor,this,s);
            	fout.println(s);
            }
		}
		// If a temporary file was used, rename the temporary file to output file.
		if ( (temporaryFile != null) && IOUtil.fileExists(temporaryFile) ) {
			fout.close();
			fout = null; // Will be checked in 'finally'.
			Path source = Paths.get(temporaryFile);
			Path target = Paths.get(OutputFile_full);
			// Remove the target first because move expects it to not exist.
			if ( Files.exists(target) ) {
				Files.delete(target);
			}
			// Move the file.
			Files.move(source, target);
		}
    	// Save the output file name.
    	setOutputFile ( new File(OutputFile_full));
	}
	catch ( Exception e ) {
		message = "Unexpected error formatting output file (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
		throw new CommandException ( message );
	}
	finally {
		if ( fout != null ) {
			fout.close();
		}
	}
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file ) {
    __OutputFile_File = file;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"InputFile",
		"PrependFile",
		"AppendFile",
		"ContentType",
		"AutoFormat",
		"OutputType",
		"OutputFile"
	};
	return this.toString(parameters, parameterOrder);
}

}