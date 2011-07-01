package rti.tscommandprocessor.commands.util;

import java.awt.print.PageFormat;
import java.io.File;
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
public class PrintTextFile_Command extends AbstractCommand
implements Command
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
	List<String> valid_Vector = new Vector();
	valid_Vector.add ( "InputFile" );
	valid_Vector.add ( "PrinterName" );
	valid_Vector.add ( "PaperSize" );
	valid_Vector.add ( "PaperSource" );
	valid_Vector.add ( "Orientation" );
	valid_Vector.add ( "MarginLeft" );
	valid_Vector.add ( "MarginRight" );
	valid_Vector.add ( "MarginTop" );
	valid_Vector.add ( "MarginBottom" );
	valid_Vector.add ( "LinesPerPage" );
	valid_Vector.add ( "Header" );
	valid_Vector.add ( "Footer" );
	valid_Vector.add ( "ShowLineCount" );
	valid_Vector.add ( "ShowPageCount" );
	valid_Vector.add ( "Pages" );
	valid_Vector.add ( "DoubleSided" );
	valid_Vector.add ( "OutputFile" );
	valid_Vector.add ( "ShowDialog" );
	valid_Vector.add ( "IfNotFound" );
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
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new PrintTextFile_JDialog ( parent, this )).ok();
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
Set the pages to print (integer ranges).
*/
private void setRequestedPages ( int[][] requestedPages )
{
    __requestedPages = requestedPages;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String InputFile = parameters.getValue("InputFile");
    String PrinterName = parameters.getValue ( "PrinterName" );
    String PaperSize = parameters.getValue ( "PaperSize" );
    String PaperSource = parameters.getValue ( "PaperSource" );
    String Orientation = parameters.getValue ( "Orientation" );
    String MarginLeft = parameters.getValue ( "MarginLeft" );
    String MarginRight = parameters.getValue ( "MarginRight" );
    String MarginTop = parameters.getValue ( "MarginTop" );
    String MarginBottom = parameters.getValue ( "MarginBottom" );
    String LinesPerPage = parameters.getValue ( "LinesPerPage" );
    String Header = parameters.getValue ( "Header" );
    String Footer = parameters.getValue ( "Footer" );
    String ShowLineCount = parameters.getValue ( "ShowLineCount" );
    String ShowPageCount = parameters.getValue ( "ShowPageCount" );
    String Pages = parameters.getValue ( "Pages" );
    String DoubleSided = parameters.getValue ( "DoubleSided" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String ShowDialog = parameters.getValue ( "ShowDialog" );
	String IfNotFound = parameters.getValue("IfNotFound");
	StringBuffer b = new StringBuffer ();
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
    if ( (PrinterName != null) && (PrinterName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PrinterName=\"" + PrinterName + "\"" );
    }
    if ( (PaperSize != null) && (PaperSize.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PaperSize=\"" + PaperSize + "\"");
    }
    if ( (PaperSource != null) && (PaperSource.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PaperSource=\"" + PaperSource + "\"");
    }
    if ( (Orientation != null) && (Orientation.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Orientation=" + Orientation );
    }
    if ( (MarginLeft != null) && (MarginLeft.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MarginLeft=" + MarginLeft );
    }
    if ( (MarginRight != null) && (MarginRight.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MarginRight=" + MarginRight );
    }
    if ( (MarginTop != null) && (MarginTop.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MarginTop=" + MarginTop );
    }
    if ( (MarginBottom != null) && (MarginBottom.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MarginBottom=" + MarginBottom );
    }
    if ( (LinesPerPage != null) && (LinesPerPage.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "LinesPerPage=" + LinesPerPage );
    }
    if ( (Header != null) && (Header.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Header=\"" + Header + "\"" );
    }
    if ( (Footer != null) && (Footer.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Footer=\"" + Footer + "\"" );
    }
    if ( (ShowLineCount != null) && (ShowLineCount.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ShowLineCount=" + ShowLineCount );
    }
    if ( (ShowPageCount != null) && (ShowPageCount.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ShowPageCount=" + ShowPageCount );
    }
    if ( (Pages != null) && (Pages.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Pages=\"" + Pages + "\"");
    }
    if ( (DoubleSided != null) && (DoubleSided.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DoubleSided=" + DoubleSided );
    }
    if ( (ShowDialog != null) && (ShowDialog.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ShowDialog=" + ShowDialog );
    }
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"" );
    }
	if ( (IfNotFound != null) && (IfNotFound.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfNotFound=" + IfNotFound );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}