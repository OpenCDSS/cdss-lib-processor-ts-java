package rti.tscommandprocessor.commands.spreadsheet;

import javax.swing.JFrame;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.IO.IOUtil;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the SetExcelWorksheetViewProperties() command, using Apache POI.
A useful link is:  http://poi.apache.org/spreadsheet/quick-guide.html
*/
public class SetExcelWorksheetViewProperties_Command extends AbstractCommand implements Command
{
	
/**
Possible values for KeepOpen parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Constructor.
*/
public SetExcelWorksheetViewProperties_Command ()
{	super();
	setCommandName ( "SetExcelWorksheetViewProperties" );
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
{	String FreezePaneRowBelowSplit = parameters.getValue ( "FreezePaneRowBelowSplit" );
    String KeepOpen = parameters.getValue ( "KeepOpen" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( FreezePaneRowBelowSplit != null && !FreezePaneRowBelowSplit.equalsIgnoreCase("") && 
        !StringUtil.isInteger(FreezePaneRowBelowSplit)) {
        message = "FreezePaneRowBelowSplit (" + FreezePaneRowBelowSplit + ") is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify FreezePaneRowBelowSplit as an integer" ) );
    }
  
    if ( KeepOpen != null && !KeepOpen.equalsIgnoreCase(_True) && 
        !KeepOpen.equalsIgnoreCase(_False) && !KeepOpen.equalsIgnoreCase("") ) {
        message = "KeepOpen is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "KeepOpen must be specified as " + _False + " (default) or " + _True ) );
    }

	//  Check for invalid parameters...
	List<String> validList = new ArrayList<String>();
    validList.add ( "OutputFile" );
    validList.add ( "Worksheet" );
    validList.add ( "FreezePaneColumnRightOfSplit" );
    validList.add ( "FreezePaneRowBelowSplit" );
    validList.add ( "KeepOpen" );
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
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new SetExcelWorksheetViewProperties_JDialog ( parent, this )).ok();
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ReadTableFromExcelFile_Command.runCommand",message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    status.clearLog(commandPhase);

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String OutputFile = parameters.getValue ( "OutputFile" );
	String Worksheet = parameters.getValue ( "Worksheet" );
	String FreezePaneColumnRightOfSplit = parameters.getValue ( "FreezePaneColumnRightOfSplit" );
	if ( (FreezePaneColumnRightOfSplit != null) && FreezePaneColumnRightOfSplit.equals("") ) {
		FreezePaneColumnRightOfSplit = null; // Easier to check below
	}
	String FreezePaneRowBelowSplit = parameters.getValue ( "FreezePaneRowBelowSplit" );
	int freezePaneRowBelowSplit = 0;
	if ( (FreezePaneRowBelowSplit != null) && !FreezePaneRowBelowSplit.equals("") ) {
		freezePaneRowBelowSplit = Integer.parseInt(FreezePaneRowBelowSplit);
	}
	else {
		FreezePaneRowBelowSplit = null; // Easier to check below
	}
    String KeepOpen = parameters.getValue ( "KeepOpen" );
    boolean keepOpen = false; // default
    if ( (KeepOpen != null) && KeepOpen.equalsIgnoreCase(_True) ) {
        keepOpen = true;
    }

    // TODO SAM 2015-02-04 Not sure that it needs to exist because may be in memory before final write
	String OutputFile_full = //IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),OutputFile);// );
	/*
	if ( !IOUtil.fileExists(OutputFile_full) ) {
		message += "\nThe Excel workbook file \"" + OutputFile_full + "\" does not exist.";
		++warning_count;
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
            message, "Verify that the Excel workbook file exists." ) );
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

	try {
	    Workbook wb = ExcelUtil.getOpenWorkbook(OutputFile_full);
        if ( wb == null ) {
        	// This is an error because this command operates on an open workbook
            message = "The Excel workbook \"" + OutputFile_full + "\" is not open - need to keep open in previous command.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        else {
        	// Get the worksheet to be modified
            Sheet ws = null;
            if ( (Worksheet == null) || (Worksheet.length() == 0) ) {
                // Default is to use the first sheet
                ws = wb.getSheetAt(0);
                if ( ws == null ) {
                    message = "The Excel workbook \"" + OutputFile_full + "\" does not include any worksheets.";
                    Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                    status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Add worksheets to the workbook." ) );
                }
            }
            else {
                ws = wb.getSheet(Worksheet);
                if ( ws == null ) {
                    message = "The Excel workbook \"" + OutputFile_full + "\" does not include worksheet named \"" + Worksheet + "\"";
                    Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                    status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the specified worksheet exists in the workbook." ) );
                }
            }
            if ( ws != null ) {
            	if ( (FreezePaneColumnRightOfSplit != null) || (FreezePaneRowBelowSplit != null) ) {
	            	// Create the freeze pane.  Need to convert the command parameters to the internal values.
            		int freezePaneColumnRightOfSplit = 0;
            		if ( (FreezePaneColumnRightOfSplit != null) && !FreezePaneColumnRightOfSplit.equals("") ) {
            			 // Convert the Excel column letter to 0+ column number
            			 CellReference ref = new CellReference(FreezePaneColumnRightOfSplit + "1");
            			 freezePaneColumnRightOfSplit = ref.getCol();
            		}
            		// Row needs to be converted from 1+ to 0+ (initial 0 means no freeze)
            		if ( freezePaneRowBelowSplit > 0 ) {
            			--freezePaneRowBelowSplit;
            		}
	            	ws.createFreezePane(freezePaneColumnRightOfSplit, freezePaneRowBelowSplit);
            	}
            }
            // If keeping open skip because it will be written by a later command.
            if ( !keepOpen ) {
                // Close the workbook and remove from the cache
                wb.setForceFormulaRecalculation(true); // Will cause Excel to recalculate formulas when it opens
                FileOutputStream fout = new FileOutputStream(OutputFile_full);
                wb.write(fout);
                fout.close();
                ExcelUtil.removeOpenWorkbook(OutputFile_full);
            }
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error setting view properties for Excel workbook file \"" + OutputFile_full + "\" (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that the file exists and is readable." ) );
		throw new CommandWarningException ( message );
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
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String OutputFile = props.getValue( "OutputFile" );
	String Worksheet = props.getValue( "Worksheet" );
	String FreezePaneColumnRightOfSplit = props.getValue("FreezePaneColumnRightOfSplit");
	String FreezePaneRowBelowSplit = props.getValue("FreezePaneRowBelowSplit");
	String KeepOpen = props.getValue("KeepOpen");
	StringBuffer b = new StringBuffer ();
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}
    if ( (Worksheet != null) && (Worksheet.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Worksheet=\"" + Worksheet + "\"" );
    }
    if ( (FreezePaneColumnRightOfSplit != null) && (FreezePaneColumnRightOfSplit.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FreezePaneColumnRightOfSplit=" + FreezePaneColumnRightOfSplit );
    }
    if ( (FreezePaneRowBelowSplit != null) && (FreezePaneRowBelowSplit.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FreezePaneRowBelowSplit=" + FreezePaneRowBelowSplit );
    }
    if ( (KeepOpen != null) && (KeepOpen.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "KeepOpen=" + KeepOpen );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}