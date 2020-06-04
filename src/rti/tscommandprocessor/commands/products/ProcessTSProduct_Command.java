// ProcessTSProduct_Command - This class initializes, checks, and runs the ProcessTSProduct() command.

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
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ProcessTSProduct() command.
*/
public class ProcessTSProduct_Command extends AbstractCommand implements FileGenerator
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
 * Properties for OutputProductFormat
 */
protected final String _Properties = "Properties";
protected final String _JSON = "JSON";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Output product file that is created by this command.
*/
private File __OutputProductFile_File = null;

/**
Constructor.
*/
public ProcessTSProduct_Command ()
{	super();
	setCommandName ( "ProcessTSProduct" );
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
{	String TSProductFile = parameters.getValue ( "TSProductFile" );
	String RunMode = parameters.getValue ( "RunMode" );
	String View = parameters.getValue ( "View" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String VisibleStart = parameters.getValue ( "VisibleStart" );
    String VisibleEnd = parameters.getValue ( "VisibleEnd" );
	String OutputProductFile = parameters.getValue ( "OutputProductFile" );
	String OutputProductFormat = parameters.getValue ( "OutputProductFormat" );
	String warning = "";
    String message;
	
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (TSProductFile == null) || TSProductFile.isEmpty() ) {
        message = "The TSProduct file must be specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series product file." ) );
	}
	else if ( TSProductFile.indexOf("${") < 0 ) {
		// Can only check when ${Property} is not used
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

	if ( (RunMode != null) && !RunMode.isEmpty() &&
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
	if ( (View != null) && !View.isEmpty() &&
		!View.equalsIgnoreCase(_True) &&
		!View.equalsIgnoreCase(_False) ) {
        message = "The View parameter \"" + View + "\" must be " + _True + " or " + _False + ".";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Correct the view parameter to be blank, " + _True + ", or " + _False + "." ) );
	}
	if ( (View != null) && View.equalsIgnoreCase(_False) &&
		((OutputFile == null) || OutputFile.isEmpty()) ) {
        message = "The output file must be specified when View=False.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Specify an output file for the product." ) );
	}
	else if ( (OutputFile != null) && !OutputFile.isEmpty() && (OutputFile.indexOf("${") < 0) ) {
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
                        new CommandLogRecord(CommandStatusType.WARNING,
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
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify that the output file path and working directory are compatible." ) );
		}
	}
	
    if ( (VisibleStart != null) && !VisibleStart.isEmpty() && !VisibleStart.startsWith("${")) {
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
    if ( (VisibleEnd != null) && !VisibleEnd.isEmpty() && !VisibleEnd.startsWith("${") ) {
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

	if ( (OutputProductFile != null) && !OutputProductFile.isEmpty() && (OutputProductFile.indexOf("${") < 0) ) {
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
            String adjusted_path = IOUtil.verifyPathForOS( IOUtil.adjustPath (working_dir, OutputProductFile) );
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
                message = "The output file parent directory does not exist for: \"" + adjusted_path + "\".";
  				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Create the folder for the output product file." ) );
            }
			f = null;
			f2 = null;
		}
		catch ( Exception e ) {
            message = "The output product file \"" + OutputProductFile + "\" cannot be adjusted by the working directory: \"" +
            working_dir + "\".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify that the output product file path and working directory are compatible." ) );
		}
	}
	if ( (OutputProductFormat != null) && !OutputProductFormat.isEmpty() &&
		!OutputProductFormat.equalsIgnoreCase(_JSON) &&
		!OutputProductFormat.equalsIgnoreCase(_Properties) ) {
        message = "The OutputProductFormat parameter \"" + OutputProductFormat + "\" must be " + _JSON + " or " + _Properties + ".";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Correct the output product format parameter to be blank, " + _JSON + ", or " + _Properties + "." ) );
	}

    // Check for invalid parameters...
	List<String> validList = new ArrayList<>(9);
    validList.add ( "TSProductFile" );
    validList.add ( "RunMode" );
    validList.add ( "View" );
    validList.add ( "OutputFile" );
    validList.add ( "VisibleStart" );
    validList.add ( "VisibleEnd" );
    validList.add ( "OutputProductFile" );
    validList.add ( "OutputProductFormat" );
    validList.add ( "DefaultSaveFile" );
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
	return (new ProcessTSProduct_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList () {
	List<File> list = new ArrayList<File>();
    if ( getOutputFile() != null ) {
        list.add ( getOutputFile() );
    }
    if ( getOutputProductFile() != null ) {
        list.add ( getOutputProductFile() );
    }
    return list;
}

/**
Return the output image file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
    return __OutputFile_File;
}

/**
Return the output product file generated by this file.  This method is used internally.
*/
private File getOutputProductFile ()
{
    return __OutputProductFile_File;
}

/**
Parse the command string into a PropList of parameters.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	//int warning_count = 0;
	String routine = "processTSProduct_Command.parseCommand", message;
	int warning_level = 2;
	if ( (command_string.indexOf("=") > 0) || command_string.endsWith("()") ) {
		// New syntax...
		super.parseCommand ( command_string );
	}
	else {	// Parse the old command...
		List<String> tokens = StringUtil.breakStringList ( command_string,
			"(,)", StringUtil.DELIM_ALLOW_STRINGS );
		if ( (tokens.size() != 4) && (tokens.size() != 5) ) {
			message = "Invalid syntax for command.  Expecting 4 parameters.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
		String TSProductFile = ((String)tokens.get(1)).trim();
		String RunMode = ((String)tokens.get(2)).trim();
		String View = ((String)tokens.get(3)).trim();
		String OutputFile = ((String)tokens.get(4)).trim();
		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSProductFile.length() > 0 ) {
			parameters.set ( "TSProductFile", TSProductFile );
		}
		if ( RunMode.length() > 0 ) {
			parameters.set ( "RunMode", RunMode );
		}
		if ( View.length() > 0 ) {
			if ( View.equalsIgnoreCase("Preview") ) {
				View = _True;
			}
			else if ( View.equalsIgnoreCase("NoPreview") ) {
				View = _False;
			}
			parameters.set ( "View", View );
		}
		if ( OutputFile.length() > 0 ) {
			parameters.set ( "OutputFile", OutputFile );
		}
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
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

	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    Boolean clearStatus = new Boolean(true); // default
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen
    }
    if ( clearStatus ) {
		status.clearLog(CommandPhaseType.RUN);
	}

	// Check whether the application wants output files to be created...
	
    if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
            Message.printStatus ( 2, routine,
            "Skipping \"" + toString() + "\" because output is not being created." );
    }

	PropList parameters = getCommandParameters();
	String TSProductFile = parameters.getValue ( "TSProductFile" );  // Property expansion below
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
	String OutputProductFile = parameters.getValue ( "OutputProductFile" );  // Property expansion below
	String OutputProductFormat = parameters.getValue ( "OutputProductFormat" );
	String DefaultSaveFile = parameters.getValue ( "DefaultSaveFile" );  // Property expansion below

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
		TSProductFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            	TSCommandProcessorUtil.expandParameterValue(processor,this,TSProductFile)) );
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
        PropList overrideProps = new PropList ("TSTool");
		DateTime now = new DateTime ( DateTime.DATE_CURRENT );
		if ( (OutputFile != null) && !OutputFile.equals("") ) {
            OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                	TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)) );
			overrideProps.set ( "OutputFile", OutputFile_full );
		}
		// TODO SAM 2013-01-28 Seems like the misplaced equals sign will cause a problem but
		// need to evaluate whether fix will ALWAYS turn on current date/time vertical line
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
			if ( (DefaultSaveFile != null) && !DefaultSaveFile.isEmpty() ) {
			    String DefaultSaveFile_full =
			        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
			        	TSCommandProcessorUtil.expandParameterValue(processor,this,DefaultSaveFile));
			    overrideProps.set ( "DefaultSaveFile", DefaultSaveFile_full );
			}
		}
	    String OutputProductFile_full = null;
		if ( (OutputProductFile != null) && !OutputProductFile.isEmpty() ) {
			// Add the output product file
		    OutputProductFile_full = IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
		        	TSCommandProcessorUtil.expandParameterValue(processor,this,OutputProductFile));
		    overrideProps.set ( "OutputProductFile", OutputProductFile_full );
		    // OK if null as default will be handled in called code
		    overrideProps.set ( "OutputProductFomat", OutputProductFormat );
		}
		if ( (IOUtil.isBatch() && (RunMode.equalsIgnoreCase("GUIAndBatch") ||(RunMode.equalsIgnoreCase("Batch")))) ||
			(!IOUtil.isBatch() && (RunMode.equalsIgnoreCase("GUIAndBatch") ||(RunMode.equalsIgnoreCase("GUIOnly")))) ) {
			// Only run the command for the requested run mode...
			TSProcessor p = new TSProcessor();
			if ( tsview_window_listener != null ) {
				p.addTSViewWindowListener (	tsview_window_listener );
			}
			p.addTSSupplier ( (TSSupplier)processor );
			if ( !isProductTemplate ) {
				// If template will have a temporary filename in TSProductFile
				TSProductFile_full = IOUtil.verifyPathForOS(
                    IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                    	TSCommandProcessorUtil.expandParameterValue(processor,this,TSProductFile)) );
			}
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
					ap = ap_Vector.get(i);
					tsp.addTSProductAnnotationProvider(	ap, null);
				}
			}
			// Now process the product...
			p.processProduct ( tsp );
            // Save the output file name...
            if ( (OutputFile_full != null) && !OutputFile_full.equals("") ) {
                setOutputFile ( new File(OutputFile_full));
            }
            // Save the output product file name...
            if ( (OutputProductFile_full != null) && !OutputProductFile_full.equals("") ) {
                setOutputProductFile ( new File(OutputProductFile_full));
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
Set the output image file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file )
{
    __OutputFile_File = file;
}

/**
Set the output product file that is created by this command.  This is only used internally.
*/
private void setOutputProductFile ( File file )
{
    __OutputProductFile_File = file;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String TSProductFile = props.getValue("TSProductFile");
	String RunMode = props.getValue("RunMode");
	String View = props.getValue("View");
	String OutputFile = props.getValue("OutputFile");
    String VisibleStart = props.getValue ( "VisibleStart" );
    String VisibleEnd = props.getValue ( "VisibleEnd" );
	String OutputProductFile = props.getValue("OutputProductFile");
	String OutputProductFormat = props.getValue("OutputProductFormat");
	String DefaultSaveFile = props.getValue("DefaultSaveFile");
	StringBuffer b = new StringBuffer ();
	if ( (TSProductFile != null) && (TSProductFile.length() > 0) ) {
		b.append ( "TSProductFile=\"" + TSProductFile + "\"" );
	}
	if ( (RunMode != null) && (RunMode.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "RunMode=" + RunMode );
	}
	if ( (View != null) && (View.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "View=" + View );
	}
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}
    if ( (VisibleStart != null) && (VisibleStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "VisibleStart=\"" + VisibleStart + "\"" );
    }
    if ( (VisibleEnd != null) && (VisibleEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "VisibleEnd=\"" + VisibleEnd + "\"" );
    }
	if ( (OutputProductFile != null) && (OutputProductFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputProductFile=\"" + OutputProductFile + "\"" );
	}
	if ( (OutputProductFormat != null) && (OutputProductFormat.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputProductFormat=\"" + OutputProductFormat + "\"" );
	}
    if ( (DefaultSaveFile != null) && (DefaultSaveFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DefaultSaveFile=\"" + DefaultSaveFile + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
