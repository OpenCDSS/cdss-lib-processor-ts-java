// PrintTextFile_Command extends - This class initializes, checks, and runs the PrintTextFile() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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

import java.awt.print.PageFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
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
import RTi.Util.IO.PrintUtil;
import RTi.Util.IO.PropList;
import RTi.Util.IO.TextPrinterJob;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the PrintTextFile() command.
*/
public class PrintTextFile_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Possible parameter values.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

protected final String _False = "False";
protected final String _True = "True";

/**
Pages to print.
*/
int [][] __requestedPages = new int[0][0];

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public PrintTextFile_Command ()
{	super();
	setCommandName ( "PrintTextFile" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String InputFile = parameters.getValue ( "InputFile" );
    // TODO SAM 2011-06-25 Might be nice to verify these at load, but it can be slow and don't have time to code now
    // Parameters will be verified when editing and running
    //String PrinterName = parameters.getValue ( "PrinterName" );
    String PaperSize = parameters.getValue ( "PaperSize" );
    //String PaperSource = parameters.getValue ( "PaperSource" );
    String Orientation = parameters.getValue ( "Orientation" );
    String MarginLeft = parameters.getValue ( "MarginLeft" );
    String MarginRight = parameters.getValue ( "MarginRight" );
    String MarginTop = parameters.getValue ( "MarginTop" );
    String MarginBottom = parameters.getValue ( "MarginBottom" );
    String LinesPerPage = parameters.getValue ( "LinesPerPage" );
    String ShowLineCount = parameters.getValue ( "ShowLineCount" );
    String ShowPageCount = parameters.getValue ( "ShowPageCount" );
    String Pages = parameters.getValue ( "Pages" );
    //String DoubleSided = parameters.getValue ( "DoubleSided" );
    String ShowDialog = parameters.getValue ( "ShowDialog" );
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the file to print is not checked during initialization
	// because files may be created dynamically at runtime.

	if ( (InputFile == null) || (InputFile.length() == 0) ) {
		message = "The input file (file to print) must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the file to print."));
	}
	String landscape = PrintUtil.getOrientationAsString(PageFormat.LANDSCAPE);
	String portrait = PrintUtil.getOrientationAsString(PageFormat.PORTRAIT);
    if ( (Orientation != null) && !Orientation.equals("") ) {
        if ( !Orientation.equalsIgnoreCase(landscape) && !Orientation.equalsIgnoreCase(portrait) ) {
            message = "The Orientation parameter \"" + Orientation + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the parameter as " + landscape + " or " + portrait + " (default)."));
        }
    }
    // All margins must be specified, or none at all
    int setCount = 0;
	if ( (MarginLeft != null) && !MarginLeft.equals("") ) {
	    ++setCount;
	    if ( !StringUtil.isDouble(MarginLeft) ) {
            message = "The left margin value (" + MarginLeft + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the left margin as a number." ) );
	    }
    }
    if ( (MarginRight != null) && !MarginRight.equals("") ) {
        ++setCount;
        if ( !StringUtil.isDouble(MarginRight) ) {
            message = "The right margin value (" + MarginRight + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the right margin as a number." ) );
        }
    }
    if ( (MarginTop != null) && !MarginTop.equals("") ) {
        ++setCount;
        if ( !StringUtil.isDouble(MarginTop) ) {
            message = "The top margin value (" + MarginTop + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the top margin as a number." ) );
        }
    }
    if ( (MarginBottom != null) && !MarginBottom.equals("") ) {
        ++setCount;
        if ( !StringUtil.isDouble(MarginBottom) ) {
            message = "The bottom margin value (" + MarginBottom + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the bottom margin as a number." ) );
        }
    }
    if ( (setCount != 0) && (setCount != 4) ) {
        message = "All margins must be set (or none should be set).";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify all margins, or specify none." ) );
    }
    if ( (setCount != 0) && ((PaperSize == null) || (PaperSize.length() == 0)) ) {
        message = "Margins can only be set when the paper is specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the paper size to set margins." ) );
    }
    if ( (LinesPerPage != null) && !LinesPerPage.equals("") && !StringUtil.isInteger(LinesPerPage) ) {
        message = "The lines per page value (" + LinesPerPage + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the lines per page as an integer." ) );
    }
    if ( (ShowLineCount != null) && !ShowLineCount.equals("") &&
        !ShowLineCount.equalsIgnoreCase(_False) && !ShowLineCount.equalsIgnoreCase(_True) ) {
        message = "The ShowLineCount parameter \"" + ShowLineCount + "\" is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the parameter as " + _False + " or " + _True + " (default)."));
    }
    if ( (ShowPageCount != null) && !ShowPageCount.equals("") &&
        !ShowPageCount.equalsIgnoreCase(_False) && !ShowPageCount.equalsIgnoreCase(_True) ) {
        message = "The ShowPageCount parameter \"" + ShowPageCount + "\" is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the parameter as " + _False + " or " + _True + " (default)."));
    }
    if ( (ShowDialog != null) && !ShowDialog.equals("") &&
            !ShowDialog.equalsIgnoreCase(_False) && !ShowDialog.equalsIgnoreCase(_True) ) {
            message = "The ShowDialog parameter \"" + ShowDialog + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the parameter as " + _False + " (default) or " + _True + "."));
        }
    if ( (IfNotFound != null) && !IfNotFound.equals("") ) {
        if ( !IfNotFound.equalsIgnoreCase(_Ignore) && !IfNotFound.equalsIgnoreCase(_Warn) ) {
            message = "The IfNotFound parameter \"" + IfNotFound + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the parameter as " + _Ignore + " or (default) " + _Warn + "."));
        }
    }
    setRequestedPages ( null );
    if ( (Pages != null) && !Pages.equals("") ) {
        try {
            setRequestedPages ( StringUtil.parseIntegerRangeSequence(Pages, ",", 0, -1) );
        }
        catch ( Exception e ) {
            message = "The pages to print (" + Pages + ") is invalid (" + e + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify as comma-separated pages (1+) and/or ranges a-b." ) );
        }
    }
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>();
	validList.add ( "InputFile" );
	validList.add ( "PrinterName" );
	validList.add ( "PaperSize" );
	validList.add ( "PaperSource" );
	validList.add ( "Orientation" );
	validList.add ( "MarginLeft" );
	validList.add ( "MarginRight" );
	validList.add ( "MarginTop" );
	validList.add ( "MarginBottom" );
	validList.add ( "LinesPerPage" );
	validList.add ( "Header" );
	validList.add ( "Footer" );
	validList.add ( "ShowLineCount" );
	validList.add ( "ShowPageCount" );
	validList.add ( "Pages" );
	validList.add ( "DoubleSided" );
	validList.add ( "OutputFile" );
	validList.add ( "ShowDialog" );
	validList.add ( "IfNotFound" );
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
{	// The command will be modified if changed...
	return (new PrintTextFile_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
    List<File> list = new Vector<File>();
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

// Use parent parseCommand()

/**
Run the command.
@param command_line Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "PrintTextFile_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	
    // Clear the output file
    
    setOutputFile ( null );
	
	String InputFile = parameters.getValue ( "InputFile" );
    String PrinterName = parameters.getValue ( "PrinterName" );
    String PaperSize = parameters.getValue ( "PaperSize" );
    String PaperSource = parameters.getValue ( "PaperSource" );
    String Orientation = parameters.getValue ( "Orientation" );
    String MarginLeft = parameters.getValue ( "MarginLeft" );
    double marginLeft = .75; // Default
    if ( (MarginLeft != null) && !MarginLeft.equals("") ) {
        marginLeft = Double.parseDouble(MarginLeft);
    }
    String MarginRight = parameters.getValue ( "MarginRight" );
    double marginRight = .75; // Default
    if ( (MarginRight != null) && !MarginRight.equals("") ) {
        marginRight = Double.parseDouble(MarginRight);
    }
    String MarginTop = parameters.getValue ( "MarginTop" );
    double marginTop = .75; // Default
    if ( (MarginTop != null) && !MarginTop.equals("") ) {
        marginTop = Double.parseDouble(MarginTop);
    }
    String MarginBottom = parameters.getValue ( "MarginBottom" );
    double marginBottom = .75; // Default
    if ( (MarginBottom != null) && !MarginBottom.equals("") ) {
        marginBottom = Double.parseDouble(MarginBottom);
    }
    String LinesPerPage = parameters.getValue ( "LinesPerPage" );
    int linesPerPage = 0; // Default
    if ( (LinesPerPage != null) && !LinesPerPage.equals("") ) {
        linesPerPage = Integer.parseInt(LinesPerPage);
    }
    String Header = parameters.getValue ( "Header" );
    String Footer = parameters.getValue ( "Footer" );
    String ShowLineCount = parameters.getValue ( "ShowLineCount" );
    boolean showLineCount = false; // Default
    if ( (ShowLineCount != null) && ShowLineCount.equalsIgnoreCase("true") ) {
        showLineCount = true;
    }
    String ShowPageCount = parameters.getValue ( "ShowPageCount" );
    boolean showPageCount = true; // Default
    if ( (ShowPageCount != null) && ShowPageCount.equalsIgnoreCase("false") ) {
        showPageCount = false;
    }
    String OutputFile = parameters.getValue ( "OutputFile" );
    String OutputFile_full = null;
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        OutputFile_full = TSCommandProcessorUtil.expandParameterValue(processor,this,
            IOUtil.verifyPathForOS(IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),OutputFile )) );
    }
    String ShowDialog = parameters.getValue ( "ShowDialog" );
    boolean showDialog = false; // Default
    if ( (ShowDialog != null) && ShowDialog.equalsIgnoreCase("true") ) {
        showDialog = true;
    }
    //String DoubleSided = parameters.getValue ( "DoubleSided" );
    boolean doubleSided = false; // TODO SAM 2011-06-25 Need to enable parameter
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	if ( (IfNotFound == null) || IfNotFound.equals("")) {
	    IfNotFound = _Warn; // Default
	}

	String InputFile_full = TSCommandProcessorUtil.expandParameterValue(processor,this,
	    IOUtil.verifyPathForOS(IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),InputFile )) );
    File file = new File ( InputFile_full );
	if ( !file.exists() ) {
        message = "File to print \"" + InputFile_full + "\" does not exist.";
        if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the file exists at the time the command is run."));
        }
        else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
                    message, "Verify that the file exists at the time the command is run."));
        }
        else {
            Message.printStatus( 2, routine, message + "  Ignoring.");
        }
	}
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	if ( file.exists() ) {
	    try {
            // Print the file...
	        List<String> fileAsList = IOUtil.fileToStringList(InputFile_full);
	        // Allow printer name to be a system property
	        String printerName = null;
	        if ( (PrinterName != null) && (PrinterName.length() > 0) ) {
	            printerName = TSCommandProcessorUtil.expandParameterValue(processor,this,PrinterName);
	        }
            new TextPrinterJob ( fileAsList,
                InputFile_full, // Printer job name
                printerName,
                PaperSize,
                PaperSource,
                Orientation,
                marginLeft,
                marginRight,
                marginTop,
                marginBottom,
                linesPerPage,
                Header,
                Footer,
                showLineCount,
                showPageCount,
                __requestedPages,
                doubleSided,
                OutputFile_full,
                showDialog );
            if ( IOUtil.fileExists(OutputFile_full) ) {
                // Save the output file name...
                setOutputFile ( new File(OutputFile_full));
            }
    	}
    	catch ( Exception e ) {
    		message = "Unexpected error printing file \"" + InputFile_full + "\" (" + e + ").";
    		Message.printWarning ( warning_level, 
    		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    		Message.printWarning ( 3, routine, e );
    		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details."));
    		throw new CommandException ( message );
    	}
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
Set the pages to print (integer ranges).
*/
private void setRequestedPages ( int[][] requestedPages )
{
    __requestedPages = requestedPages;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"InputFile",
    	"PrinterName",
    	"PaperSize",
    	"PaperSource",
    	"Orientation",
    	"MarginLeft",
    	"MarginRight",
    	"MarginTop",
    	"MarginBottom",
    	"LinesPerPage",
    	"Header",
    	"Footer",
    	"ShowLineCount",
    	"ShowPageCount",
    	"Pages",
    	"DoubleSided",
    	"ShowDialog",
    	"OutputFile",
		"IfNotFound"
	};
	return this.toString(parameters, parameterOrder);
}

}