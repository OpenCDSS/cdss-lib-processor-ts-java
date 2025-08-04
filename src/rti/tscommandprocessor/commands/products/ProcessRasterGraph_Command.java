// ProcessRasterGraph_Command - This class initializes, checks, and runs the ProcessRasterGraph() command.

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
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ProcessRasterGraph() command.
*/
public class ProcessRasterGraph_Command extends AbstractCommand implements FileGenerator
{

/**
 * Values for RunMode parameter.
 */
protected final String _BatchOnly = "BatchOnly";
protected final String _GUIOnly = "GUIOnly";
protected final String _GUIAndBatch = "GUIAndBatch";

/**
 * Values for the View parameter.
 */
protected final String _False = "False";
protected final String _True = "True";

/**
 * Values to use with ImageMapDataArea parameter.
 */
protected final String _Cell = "Cell";
protected final String _TimeSeries = "TimeSeries";

/**
 * Values to use with ImageMap*Target parameters.
 */
protected final String _Blank = "Blank";
protected final String _Parent = "Parent";
protected final String _Self = "Self";
protected final String _Top = "Top";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Image map output file that is created by this command.
*/
private File __ImageMapFile_File = null;

/**
Constructor.
*/
public ProcessRasterGraph_Command () {
	super();
	setCommandName ( "ProcessRasterGraph" );
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
	String TSProductFile = parameters.getValue ( "TSProductFile" );
	String RunMode = parameters.getValue ( "RunMode" );
	String View = parameters.getValue ( "View" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String ImageMapFile = parameters.getValue ( "ImageMapFile" );
	String ImageMapUrl = parameters.getValue ( "ImageMapUrl" );
	String ImageMapDataArea = parameters.getValue ( "ImageMapDataArea" );
	String ImageMapDataHref = parameters.getValue ( "ImageMapDataHref" );
	String ImageMapDataTarget = parameters.getValue ( "ImageMapDataTarget" );
	String ImageMapDataTitle = parameters.getValue ( "ImageMapDataTitle" );
	String ImageMapLegendHref = parameters.getValue ( "ImageMapLegendHref" );
	String ImageMapLegendTarget = parameters.getValue ( "ImageMapLegendTarget" );
	String ImageMapLegendTitle = parameters.getValue ( "ImageMapLegendTitle" );
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
	else if ( !TSProductFile.contains("${") ) {
	    String working_dir = null;
		try {
			Object o = processor.getPropContents ( "WorkingDir" );
			// Working directory is available so use it.
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
				/* Don't warn because want to allow the command to be saved if the file does not exist.
                message = "The TSProduct file does not exist for: \"" + adjusted_path + "\".";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Verify that the time series product file to process exists - OK if the file is created at run time." ) );
                        */
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
	if ( (View != null) && View.equalsIgnoreCase(_False) && ((OutputFile == null) || (OutputFile.length() == 0)) ) {
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
			// Working directory is available so use it.
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

	if ( (ImageMapFile != null) && !ImageMapFile.isEmpty() ) {
		if ( ImageMapFile.indexOf("${") < 0 ) {
			String working_dir = null;
			try {
				Object o = processor.getPropContents ( "WorkingDir" );
				// Working directory is available so use it.
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
            	String adjusted_path = IOUtil.verifyPathForOS( IOUtil.adjustPath (working_dir, ImageMapFile) );
				File f = new File ( adjusted_path );
				File f2 = new File ( f.getParent() );
				if ( !f2.exists() ) {
                	message = "The image map file parent directory does not exist for: \"" + adjusted_path + "\".";
  					warning += "\n" + message;
                	status.addToLog ( CommandPhaseType.INITIALIZATION,
                        	new CommandLogRecord(CommandStatusType.FAILURE,
                                	message, "Create the folder for the output file." ) );
            	}
			}
			catch ( Exception e ) {
            	message = "The image map file \"" + ImageMapFile + "\" cannot be adjusted by the working directory: \"" +
            	working_dir + "\".";
				warning += "\n" + message;
            	status.addToLog ( CommandPhaseType.INITIALIZATION,
                    	new CommandLogRecord(CommandStatusType.FAILURE,
                            	message, "Verify that the image map file path and working directory are compatible." ) );
			}
		}
		
		if ( (ImageMapUrl == null) || ImageMapUrl.isEmpty() ) {
			message = "The image map URL must be specified when ImageMapFile=True.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message,
                    "Specify an image map URL." ) );
		}

		if ( (ImageMapDataArea != null) && !ImageMapDataArea.isEmpty() &&
			!ImageMapDataArea.equalsIgnoreCase(_Cell) &&
			!ImageMapDataArea.equalsIgnoreCase(_TimeSeries)  ) {
        	message = "The image map data area \"" + ImageMapDataArea + "\" is not valid.";
			warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
               	new CommandLogRecord(CommandStatusType.FAILURE,
                   	message,
               		"Correct the image map data area to be " + _Cell + " or " + _TimeSeries + " (default)." ) );
		}

		if ( (ImageMapDataTarget != null) && !ImageMapDataTarget.isEmpty() &&
			!ImageMapDataTarget.equalsIgnoreCase(_Blank) &&
			!ImageMapDataTarget.equalsIgnoreCase(_Parent) &&
			!ImageMapDataTarget.equalsIgnoreCase(_Self) &&
			!ImageMapDataTarget.equalsIgnoreCase(_Top)  ) {
        	message = "The image map data target \"" + ImageMapDataTarget + "\" is not valid.";
			warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
               	new CommandLogRecord(CommandStatusType.FAILURE,
                   	message,
               		"Correct the image map data 'target' to be " + _Blank + ", " + _Parent + ", " + _Self + " (default), or " + _Top + ".") );
		}

		if ( (ImageMapLegendTarget != null) && !ImageMapLegendTarget.isEmpty() &&
			!ImageMapLegendTarget.equalsIgnoreCase(_Blank) &&
			!ImageMapLegendTarget.equalsIgnoreCase(_Parent) &&
			!ImageMapLegendTarget.equalsIgnoreCase(_Self) &&
			!ImageMapLegendTarget.equalsIgnoreCase(_Top)  ) {
        	message = "The image map legend target \"" + ImageMapLegendTarget + "\" is not valid.";
			warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
               	new CommandLogRecord(CommandStatusType.FAILURE,
                   	message,
               		"Correct the image map legend 'target' to be " + _Blank + ", " + _Parent + ", " + _Self + " (default), or " + _Top + ".") );
		}

		int elementCount = 0;
		if ( (ImageMapDataHref != null) && !ImageMapDataHref.isEmpty() ) {
			++elementCount;
		}
		if ( (ImageMapDataTitle != null) && !ImageMapDataTitle.isEmpty() ) {
			++elementCount;
		}
		if ( (ImageMapLegendHref != null) && !ImageMapLegendHref.isEmpty() ) {
			++elementCount;
		}
		if ( (ImageMapLegendTitle != null) && !ImageMapLegendTitle.isEmpty() ) {
			++elementCount;
		}
		if ( elementCount == 0 ) {
			message = "At least one data/legend 'href' or 'title' parameters must be specified when ImageMapFile=True.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message,
                    "Specify at least one of ImageMapDataHref, ImageMapDataTitle, ImageMapLegendHref, or ImageMapLegendTitle." ) );
		}
	}

    // Check for invalid parameters.
	List<String> validList = new ArrayList<>(18);
    validList.add ( "TSProductFile" );
    validList.add ( "RunMode" );
    validList.add ( "View" );
    validList.add ( "OutputFile" );
    validList.add ( "VisibleStart" );
    validList.add ( "VisibleEnd" );
    validList.add ( "CommandStatusProperty" );
    validList.add ( "DefaultSaveFile" );
    validList.add ( "ImageMapFile" );
    validList.add ( "ImageMapUrl" );
    validList.add ( "ImageMapName" );
    validList.add ( "ImageMapDataArea" );
    validList.add ( "ImageMapDataHref" );
    validList.add ( "ImageMapDataTarget" );
    validList.add ( "ImageMapDataTitle" );
    validList.add ( "ImageMapLegendHref" );
    validList.add ( "ImageMapLegendTarget" );
    validList.add ( "ImageMapLegendTitle" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new ProcessRasterGraph_JDialog ( parent, this )).ok();
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
    if ( getImageMapFile() != null ) {
        list.add ( getImageMapFile() );
    }
    return list;
}

/**
Return the image map output file generated by this command.  This method is used internally.
@return the image map output file generated by this command
*/
private File getImageMapFile () {
    return this.__ImageMapFile_File;
}

/**
Return the output file generated by this command.  This method is used internally.
@return the output file generated by this command
*/
private File getOutputFile () {
    return this.__OutputFile_File;
}

/**
Run the command.
Time series are taken from the available list in memory, if available.  Otherwise the time series are re-read.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warningLevel = 2;
	String commandTag = "" + command_number;
	int warningCount = 0;

    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    status.clearLog(commandPhase);

	// Check whether the application wants output files to be created.

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
	if ( (View == null) || View.isEmpty() ) {
		View = _True;
	}
	String OutputFile = parameters.getValue ( "OutputFile" );
	if ( (View == null) || View.equals("") && ((OutputFile == null) || OutputFile.equals("")) ) {
		// No output file so view is true by default.
		View = _True;
	}
    String CommandStatusProperty = parameters.getValue ( "CommandStatusProperty" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	CommandStatusProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, CommandStatusProperty);
    }
    // TODO smalers 2025--07-28 Will this be used?
	String DefaultSaveFile = parameters.getValue ( "DefaultSaveFile" );

	String ImageMapFile = parameters.getValue ( "ImageMapFile" );
	if ( (ImageMapFile != null) && !ImageMapFile.isEmpty() ) {
		// ImageMapFile has been specified:
		// - expand below
	}
	String ImageMapUrl = parameters.getValue ( "ImageMapUrl" );
	if ( (ImageMapUrl != null) && ! ImageMapUrl.isEmpty() ) {
    	ImageMapUrl = TSCommandProcessorUtil.expandParameterValue(processor, this, ImageMapUrl);
	}
	String ImageMapName = parameters.getValue ( "ImageMapName" );
	if ( (ImageMapName != null) && ! ImageMapName.isEmpty() ) {
		// Expand here for processor properties and also expand in called code for time series properties.
    	ImageMapName = TSCommandProcessorUtil.expandParameterValue(processor, this, ImageMapName);
	}
	else {
		// Default.
		ImageMapName = "imagemap";
	}
	String ImageMapDataArea = parameters.getValue ( "ImageMapDataArea" );
	if ( (ImageMapDataArea != null) && ! ImageMapDataArea.isEmpty() ) {
    	ImageMapDataArea = TSCommandProcessorUtil.expandParameterValue(processor, this, ImageMapDataArea);
	}
	String ImageMapDataHref = parameters.getValue ( "ImageMapDataHref" );
	if ( (ImageMapDataHref != null) && ! ImageMapDataHref.isEmpty() ) {
		// Expand here for processor properties and also expand in called code for time series properties.
    	ImageMapDataHref = TSCommandProcessorUtil.expandParameterValue(processor, this, ImageMapDataHref);
	}
	String ImageMapDataTarget = parameters.getValue ( "ImageMapDataTarget" );
	if ( (ImageMapDataTarget != null) && ! ImageMapDataTarget.isEmpty() ) {
    	ImageMapDataTarget = TSCommandProcessorUtil.expandParameterValue(processor, this, ImageMapDataTarget);
	}
	String ImageMapDataTitle = parameters.getValue ( "ImageMapDataTitle" );
	if ( (ImageMapDataTitle != null) && ! ImageMapDataTitle.isEmpty() ) {
		// Expand here for processor properties and also expand in called code for time series properties.
    	ImageMapDataTitle = TSCommandProcessorUtil.expandParameterValue(processor, this, ImageMapDataTitle);
	}
	String ImageMapLegendHref = parameters.getValue ( "ImageMapLegendHref" );
	if ( (ImageMapLegendHref != null) && ! ImageMapLegendHref.isEmpty() ) {
		// Expand here for processor properties and also expand in called code for time series properties.
    	ImageMapLegendHref = TSCommandProcessorUtil.expandParameterValue(processor, this, ImageMapLegendHref);
	}
	String ImageMapLegendTarget = parameters.getValue ( "ImageMapLegendTarget" );
	if ( (ImageMapLegendTarget != null) && ! ImageMapLegendTarget.isEmpty() ) {
    	ImageMapLegendTarget = TSCommandProcessorUtil.expandParameterValue(processor, this, ImageMapLegendTarget);
	}
	String ImageMapLegendTitle = parameters.getValue ( "ImageMapLegendTitle" );
	if ( (ImageMapLegendTitle != null) && ! ImageMapLegendTitle.isEmpty() ) {
		// Expand here for processor properties and also expand in called code for time series properties.
    	ImageMapLegendTitle = TSCommandProcessorUtil.expandParameterValue(processor, this, ImageMapLegendTitle);
	}

	// Get from the processor.

	WindowListener tsview_window_listener = null;

	try { Object o = processor.getPropContents ( "TSViewWindowListener" );
		// TSViewWindowListener is available so use it.
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

	// Now try to process.

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
			// Warning will have been added above.
			++warningCount;
		}
		try {
			VisibleEnd_DateTime = TSCommandProcessorUtil.getDateTime ( VisibleEnd, "VisibleEnd", processor,
				status, warningLevel, commandTag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above.
			++warningCount;
		}
    }

    String TSProductFile_full = TSProductFile;
    String OutputFile_full = OutputFile;
    String ImageMapFile_full = ImageMapFile;
	try {
    	// Determine whether the command file is a template.
    	// - although searches for ${Property} and <# Freemarker content> could be done, only search for @template
		boolean isProductTemplate = false;
        PropList overrideProps = new PropList ("TSTool");
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
            OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                	TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)) );
			overrideProps.set ( "OutputFile", OutputFile_full );
		}

		if ( (ImageMapFile != null) && !ImageMapFile.isEmpty() ) {
            ImageMapFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                	TSCommandProcessorUtil.expandParameterValue(processor,this,ImageMapFile)) );
			overrideProps.set ( "ImageMapFile", ImageMapFile_full );
		}

		if ( (TSProductFile != null) && !TSProductFile.isEmpty() ) {
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
    		// Is a template so automatically expand to a temporary file and then pass that file to the graphing code:
    		// - this requires passing processor properties to the
    		TSProductFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                	TSCommandProcessorUtil.expandParameterValue(processor,this,TSProductFile)) );
    		boolean useTables = true;
    		String tempFile = IOUtil.tempFileName();
    		TSCommandProcessorUtil.expandTemplateFile(processor, TSProductFile_full, tempFile, useTables,
    			status, commandTag, warningLevel, warningCount );
    		// Reset the template filename to the temporary filename for processing below.
    		TSProductFile_full = tempFile;
    	}
		// TODO SAM 2013-01-28 Seems like the misplaced equals sign will cause a problem but
		// need to evaluate whether fix will ALWAYS turn on current date/time vertical line.
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
		if ( (ImageMapFile != null) && !ImageMapFile.isEmpty() ) {
			overrideProps.set ( "ImageMapFile", ImageMapFile_full );
			if ( (ImageMapUrl != null) && !ImageMapUrl.isEmpty() ) {
				overrideProps.set ( "ImageMapUrl", ImageMapUrl );
			}
			if ( (ImageMapName != null) && !ImageMapName.isEmpty() ) {
				overrideProps.set ( "ImageMapName", ImageMapName );
			}
			if ( (ImageMapDataArea != null) && !ImageMapDataArea.isEmpty() ) {
				overrideProps.set ( "ImageMapDataArea", ImageMapDataArea );
			}
			if ( (ImageMapDataHref != null) && !ImageMapDataHref.isEmpty() ) {
				overrideProps.set ( "ImageMapDataHref", ImageMapDataHref );
			}
			if ( (ImageMapDataTarget != null) && !ImageMapDataTarget.isEmpty() ) {
				overrideProps.set ( "ImageMapDataTarget", ImageMapDataTarget );
			}
			if ( (ImageMapDataTitle != null) && !ImageMapDataTitle.isEmpty() ) {
				overrideProps.set ( "ImageMapDataTitle", ImageMapDataTitle );
			}
			if ( (ImageMapLegendHref != null) && !ImageMapLegendHref.isEmpty() ) {
				overrideProps.set ( "ImageMapLegendHref", ImageMapLegendHref );
			}
			if ( (ImageMapLegendTarget != null) && !ImageMapLegendTarget.isEmpty() ) {
				overrideProps.set ( "ImageMapLegendTarget", ImageMapLegendTarget );
			}
			if ( (ImageMapLegendTitle != null) && !ImageMapLegendTitle.isEmpty() ) {
				overrideProps.set ( "ImageMapLegendTitle", ImageMapLegendTitle );
			}
		}
		if ( (IOUtil.isBatch() && (RunMode.equalsIgnoreCase("GUIAndBatch") ||(RunMode.equalsIgnoreCase("Batch")))) ||
			(!IOUtil.isBatch() && (RunMode.equalsIgnoreCase("GUIAndBatch") ||(RunMode.equalsIgnoreCase("GUIOnly")))) ) {
			// Only run the command for the requested run mode.
			TSProcessor p = new TSProcessor();
			if ( tsview_window_listener != null ) {
				p.addTSViewWindowListener (	tsview_window_listener );
			}
			p.addTSSupplier ( (TSSupplier)processor );
            //TSProductFile_full = IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),TSProductFile);
			TSProduct tsp = new TSProduct ( TSProductFile_full, overrideProps );
			// Specify annotation providers if available.
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
			// Now process the product and create the output.
			p.processProduct ( tsp );

            // Save the output file name.
            if ( (OutputFile_full != null) && !OutputFile_full.equals("") ) {
                setOutputFile ( new File(OutputFile_full));
            }

            // Save the image map output file name.
            if ( (ImageMapFile_full != null) && !ImageMapFile_full.equals("") ) {
                setImageMapFile ( new File(ImageMapFile_full));
            }
		}

	    // Set the property indicating the command status:
		// - use nice text "Success", "Warning", "Failure"
		// - set to success here because if a serious error occurred an exception would have been thrown and is caught below
		// - do not use the cumulative status from the command because it will hold loop results
        if ( (CommandStatusProperty != null) && !CommandStatusProperty.equals("") ) {
            PropList request_params = new PropList ( "" );
            request_params.setUsingObject ( "PropertyName", CommandStatusProperty );
            request_params.setUsingObject ( "PropertyValue", CommandStatusType.SUCCESS.toMixedCaseString() );
            try {
                processor.processRequest( "SetProperty", request_params);
            }
            catch ( Exception e ) {
                message = "Error processing request SetProperty(Property=\"" + CommandStatusProperty + "\").";
                Message.printWarning(warningLevel,
                    MessageUtil.formatMessageTag( commandTag, ++warningCount),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
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

	    // Set the property indicating the command status:
		// - use nice text "Success", "Warning", "Failure"
		// - set to success here because if a serious error occurred an exception would have been thrown and is caught below
		// - do not use the cumulative status from the command because it will hold loop results
        if ( (CommandStatusProperty != null) && !CommandStatusProperty.equals("") ) {
            PropList request_params = new PropList ( "" );
            request_params.setUsingObject ( "PropertyName", CommandStatusProperty );
            request_params.setUsingObject ( "PropertyValue", CommandStatusType.FAILURE.toMixedCaseString() );
            try {
                processor.processRequest( "SetProperty", request_params);
            }
            catch ( Exception e2 ) {
                message = "Error processing request SetProperty(Property=\"" + CommandStatusProperty + "\").";
                Message.printWarning(warningLevel,
                    MessageUtil.formatMessageTag( commandTag, ++warningCount),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
        }

        // Rethrow the exception.
		throw new CommandException ( message );
	}
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the image map output file that is created by this command.  This is only used internally.
@param file image map output file created by this command
*/
private void setImageMapFile ( File file ) {
    this.__ImageMapFile_File = file;
}

/**
Set the output file that is created by this command.  This is only used internally.
@param file output file created by this command
*/
private void setOutputFile ( File file ) {
    this.__OutputFile_File = file;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		// Input.
		"TSProductFile",
		"RunMode",
		// Output.
		"View",
		"OutputFile",
    	"VisibleStart",
    	"VisibleEnd",
    	"CommandStatusProperty",
		"DefaultSaveFile",
		// Image map.
		"ImageMapFile",
		"ImageMapUrl",
		"ImageMapName",
		"ImageMapDataArea",
		"ImageMapDataHref",
		"ImageMapDataTarget",
		"ImageMapDataTitle",
		"ImageMapLegendHref",
		"ImageMapLegendTarget",
		"ImageMapLegendTitle"
	};
	return this.toString(parameters, parameterOrder);
}

}