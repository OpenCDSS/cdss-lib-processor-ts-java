// ProcessRasterGraph_Command - This class initializes, checks, and runs the ProcessRasterGraph() command.

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

package rti.tscommandprocessor.commands.products;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.WindowListener;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.GRTS.TSProcessor;
import RTi.GRTS.TSProduct;
import RTi.GRTS.TSProductAnnotationProvider;
import RTi.TS.TSSupplier;
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
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ProcessRasterGraph() command.
*/
public class ProcessRasterGraph_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Protected data members shared with the dialog and other related classes.
*/
protected final String _BatchOnly = "BatchOnly";
protected final String _GUIOnly = "GUIOnly";
protected final String _GUIAndBatch = "GUIAndBatch";

protected final String _False = "False";
protected final String _True = "True";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public ProcessRasterGraph_Command ()
{	super();
	setCommandName ( "ProcessRasterGraph" );
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
{	String TSProductFile = parameters.getValue ( "TSProductFile" );
	String RunMode = parameters.getValue ( "RunMode" );
	String View = parameters.getValue ( "View" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String VisibleStart = parameters.getValue ( "VisibleStart" );
    String VisibleEnd = parameters.getValue ( "VisibleEnd" );
	String warning = "";
    String message;
	
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (TSProductFile == null) || (TSProductFile.length() == 0) ) {
        message = "The TSProduct file must be specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series product file." ) );
	}
	else if ( TSProductFile.indexOf("${") < 0) {
	    String working_dir = null;
		try { Object o = processor.getPropContents ( "WorkingDir" );
			// Working directory is available so use it...
			if ( o != null ) {
				working_dir = (String)o;
			}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Software error - report problem to support." ) );
		}
		try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath ( working_dir, TSProductFile) );
			File f = new File ( adjusted_path );
			if ( !f.exists() ) {
                message = "The TSProduct file does not exist for: \"" + adjusted_path + "\".";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the time series product file to process exists." ) );
			}
		}
		catch ( Exception e ) {
            message = "The product file \"" + TSProductFile + "\" cannot be adjusted by the working directory \""
            + working_dir + "\".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Verify that the path to the time series product file and working directory are compatible." ) );
		}
	}

	if ( (RunMode != null) && !RunMode.equals("") &&
		!RunMode.equalsIgnoreCase(_BatchOnly) &&
		!RunMode.equalsIgnoreCase(_GUIOnly) &&
		!RunMode.equalsIgnoreCase(_GUIAndBatch) ) {
        message = "The run mode \"" + RunMode + "\" is not valid.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Correct the run mode to be blank, " + _BatchOnly + ", " + _GUIOnly + ", or " +
                        _GUIAndBatch + "." ) );
	}
	if ( (View != null) && !View.equals("") &&
		!View.equalsIgnoreCase(_True) &&
		!View.equalsIgnoreCase(_False) ) {
        message = "The View parameter \"" + View + "\" must be " + _True + " or " + _False + ".";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Correct the view parameter to be blank, " + _True + ", or " + _False + "." ) );
	}
	if (	(View != null) && View.equalsIgnoreCase(_False) &&
		((OutputFile == null) || (OutputFile.length() == 0)) ) {
        message = "The output file must be specified when View=False.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Specify an output file for the product." ) );
	}
	else if ( (OutputFile != null) && !OutputFile.equals("") && (OutputFile.indexOf("${") < 0) ) {
		String working_dir = null;
		try { Object o = processor.getPropContents ( "WorkingDir" );
			// Working directory is available so use it...
			if ( o != null ) {
				working_dir = (String)o;
			}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting WorkingDir from processor.";
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Software error - report problem to support." ) );
		}
				
		try {
            String adjusted_path = IOUtil.verifyPathForOS( IOUtil.adjustPath (working_dir, OutputFile) );
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
                message = "The output file parent directory does not exist for: \"" + adjusted_path + "\".";
  				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Create the folder for the output file." ) );
            }
			f = null;
			f2 = null;
		}
		catch ( Exception e ) {
            message = "The output file \"" + OutputFile + "\" cannot be adjusted by the working directory: \"" +
            working_dir + "\".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the output file path and working directory are compatible." ) );
		}
	}
	
    if ( (VisibleStart != null) && !VisibleStart.equals("")) {
        try {
            DateTime datetime1 = DateTime.parse(VisibleStart);
            if ( datetime1 == null ) {
                throw new Exception ("bad date");
            }
        }
        catch (Exception e) {
            message = "Visible start date/time \"" + VisibleStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid Visible start date/time." ) );
        }
    }
    if ( (VisibleEnd != null) && !VisibleEnd.equals("")) {
        try {
            DateTime datetime2 = DateTime.parse(VisibleEnd);
            if ( datetime2 == null ) {
                throw new Exception ("bad date");
            }
        }
        catch (Exception e) {
            message = "Visible end date/time \"" + VisibleEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid Visible end date/time." ) );
        }
    }
    // Check for invalid parameters...
	List<String> validList = new ArrayList<>(7);
    validList.add ( "TSProductFile" );
    validList.add ( "RunMode" );
    validList.add ( "View" );
    validList.add ( "OutputFile" );
    validList.add ( "DefaultSaveFile" );
    validList.add ( "VisibleStart" );
    validList.add ( "VisibleEnd" );
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
	return (new ProcessRasterGraph_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
	List<File> list = new ArrayList<File>();
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
Time series are taken from the available list in memory, if available.  Otherwise the time series are re-read.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warningLevel = 2;
	String commandTag = "" + command_number;
	int warningCount = 0;
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    status.clearLog(commandPhase);

	// Check whether the application wants output files to be created...
	
	CommandProcessor processor = getCommandProcessor();
    if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
            Message.printStatus ( 2, routine,
            "Skipping \"" + toString() + "\" because output is not being created." );
    }

	PropList parameters = getCommandParameters();
	String TSProductFile = parameters.getValue ( "TSProductFile" );
	String RunMode = parameters.getValue ( "RunMode" );
	if ( (RunMode == null) || RunMode.equals("") ) {
		RunMode = _GUIAndBatch;
	}
	String View = parameters.getValue ( "View" );
	if ( (View == null) || View.equals("") ) {
		View = _True;
	}
	String OutputFile = parameters.getValue ( "OutputFile" );
	if ( (View == null) || View.equals("") && ((OutputFile == null) || OutputFile.equals("")) ) {
		// No output file so view is true by default...
		View = _True;
	}
	String DefaultSaveFile = parameters.getValue ( "DefaultSaveFile" );

	// Get from the processor...

	WindowListener tsview_window_listener = null;
	
	try { Object o = processor.getPropContents ( "TSViewWindowListener" );
		// TSViewWindowListener is available so use it...
		if ( o != null ) {
			tsview_window_listener = (WindowListener)o;
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		message = "Error requesting TSViewWindowListener from processor - not using.";
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Report problem to software support." ) );
	}

	if ( warningCount > 0 ) {
		message = "There were " + warningCount + " warnings about command parameters.";
		Message.printWarning ( warningLevel, 
		MessageUtil.formatMessageTag(commandTag, ++warningCount),	routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Now try to process...
	
    String VisibleStart = parameters.getValue ( "VisibleStart" );
    String VisibleEnd = parameters.getValue ( "VisibleEnd" );
    DateTime VisibleStart_DateTime = null;
    DateTime VisibleEnd_DateTime = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			VisibleStart_DateTime = TSCommandProcessorUtil.getDateTime ( VisibleStart, "VisibleStart", processor,
				status, warningLevel, commandTag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warningCount;
		}
		try {
			VisibleEnd_DateTime = TSCommandProcessorUtil.getDateTime ( VisibleEnd, "VisibleEnd", processor,
				status, warningLevel, commandTag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warningCount;
		}
    }

    String TSProductFile_full = TSProductFile;
    String OutputFile_full = OutputFile;
	try {
    	// Determine whether the command file is a template.
    	// - although searches for ${Property} and <# Freemarker content> could be done, only search for @template
		boolean isProductTemplate = false;
        PropList overrideProps = new PropList ("TSTool");
		if ( (OutputFile != null) && !OutputFile.equals("") ) {
            OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                	TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)) );
			overrideProps.set ( "OutputFile", OutputFile_full );
		}
		if ( (TSProductFile != null) && !TSProductFile.equals("") ) {
            TSProductFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                	TSCommandProcessorUtil.expandParameterValue(processor,this,TSProductFile)) );
		}
		List<String> contents = IOUtil.fileToStringList(TSProductFile_full);
		for ( String s : contents ) {
			if ( s.trim().startsWith("#") && (s.toUpperCase().indexOf("@TEMPLATE") > 0) ) {
				isProductTemplate = true;
				break;
			}
		}
    	if ( isProductTemplate ) {
    		// Is a template so automatically expand to a temporary file and then pass that file to the graphing code
    		// - this requires passing processor properties to the
    		TSProductFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                	TSCommandProcessorUtil.expandParameterValue(processor,this,TSProductFile)) );
    		boolean useTables = true;
    		String tempFile = IOUtil.tempFileName();
    		TSCommandProcessorUtil.expandTemplateFile(processor, TSProductFile_full, tempFile, useTables,
    			status, commandTag, warningLevel, warningCount );
    		// Reset the template filename to the temporary filename for processing below
    		TSProductFile_full = tempFile;
    	}
		// TODO SAM 2013-01-28 Seems like the misplaced equals sign will cause a problem but
		// need to evaluate whether fix will ALWAYS turn on current date/time vertical line
		DateTime now = new DateTime ( DateTime.DATE_CURRENT );
		overrideProps.set ( "CurrentDateTime=", now.toString() );
		if ( VisibleStart_DateTime != null ) {
		    overrideProps.set ( "VisibleStart", "" + VisibleStart_DateTime );
		}
        if ( VisibleEnd_DateTime != null ) {
            overrideProps.set ( "VisibleEnd", "" + VisibleEnd_DateTime );
        }
		if ( View.equalsIgnoreCase(_True) ) {
			overrideProps.set ( "InitialView", "Graph" );
			overrideProps.set ( "PreviewOutput", "True" );
			if ( (DefaultSaveFile != null) && (DefaultSaveFile.length() > 0) ) {
			    String DefaultSaveFile_full =
			        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),DefaultSaveFile);
			    overrideProps.set ( "DefaultSaveFile", DefaultSaveFile_full );
			}
		}
		if ( (IOUtil.isBatch() && (RunMode.equalsIgnoreCase("GUIAndBatch") ||(RunMode.equalsIgnoreCase("Batch")))) ||
			(!IOUtil.isBatch() && (RunMode.equalsIgnoreCase("GUIAndBatch") ||(RunMode.equalsIgnoreCase("GUIOnly")))) ) {
			// Only run the command for the requested run mode...
			TSProcessor p = new TSProcessor();
			if ( tsview_window_listener != null ) {
				p.addTSViewWindowListener (	tsview_window_listener );
			}
			p.addTSSupplier ( (TSSupplier)processor );
            //TSProductFile_full = IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),TSProductFile);
			TSProduct tsp = new TSProduct ( TSProductFile_full, overrideProps );
			// Specify annotation providers if available...
			List<TSProductAnnotationProvider> ap_Vector = null;			
			try {
                Object o = processor.getPropContents ("TSProductAnnotationProviderList" );
					if ( o != null ) {
						@SuppressWarnings("unchecked")
						List<TSProductAnnotationProvider> ap_Vector0 = (List<TSProductAnnotationProvider>)o;
						ap_Vector = ap_Vector0;
					}
			}
			catch ( Exception e ) {
				// Not fatal, but of use to developers.
				message = "Error requesting TSProductAnnotationProviderList from processor.";
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Report problem to software support." ) );
			}
			
			if ( ap_Vector != null ) {
				int size = ap_Vector.size();
				TSProductAnnotationProvider ap;
				for ( int i = 0; i < size; i++ ) {
					ap = (TSProductAnnotationProvider)ap_Vector.get(i);
					tsp.addTSProductAnnotationProvider(	ap, null);
				}
			}
			// Now process the product...
			p.processProduct ( tsp );
            // Save the output file name...
            if ( (OutputFile_full != null) && !OutputFile_full.equals("") ) {
                setOutputFile ( new File(OutputFile_full));
            }
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error processing TSProduct file \"" + TSProductFile_full + "\" (" + e + ").";
		Message.printWarning ( warningLevel, 
		MessageUtil.formatMessageTag(commandTag, ++warningCount), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the log file." ) );
		throw new CommandException ( message );
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
		"TSProductFile",
		"RunMode",
		"View",
		"OutputFile",
		"DefaultSaveFile",
    	"VisibleStart",
    	"VisibleEnd"
	};
	return this.toString(parameters, parameterOrder);
}

}