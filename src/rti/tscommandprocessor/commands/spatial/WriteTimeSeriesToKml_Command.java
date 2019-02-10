// WriteTimeSeriesToKml_Command - This class initializes, checks, and runs the WriteTimeSeriesToKml() command.

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

package rti.tscommandprocessor.commands.spatial;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.GIS.GeoView.WKTGeometryParser;
import RTi.GR.GRPoint;
import RTi.GR.GRPointZM;
import RTi.GR.GRPolygon;
import RTi.GR.GRShape;
import RTi.TS.TS;
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
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the WriteTimeSeriesToKml() command.
*/
public class WriteTimeSeriesToKml_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteTimeSeriesToKml_Command ()
{   super();
    setCommandName ( "WriteTimeSeriesToKml" );
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
{   String OutputFile = parameters.getValue ( "OutputFile" );
    String LongitudeProperty = parameters.getValue ( "LongitudeProperty" );
    String LatitudeProperty = parameters.getValue ( "LatitudeProperty" );
    String WKTGeometryProperty = parameters.getValue ( "WKTGeometryProperty" );
    String StyleFile = parameters.getValue ( "StyleFile" );
    String MissingValue = parameters.getValue("MissingValue" );
    String Precision = parameters.getValue ( "Precision" );
    String OutputStart = parameters.getValue ( "OutputStart" );
    String OutputEnd = parameters.getValue ( "OutputEnd" );
    String warning = "";
    String routine = getCommandName() + ".checkCommandParameters";
    String message;

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        message = "The output file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify an output file." ) );
    }
    else if ( OutputFile.indexOf("${") < 0 ){
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
            if ( o != null ) {
                working_dir = (String)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Software error - report the problem to support." ) );
        }

        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                    TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The output file parent directory does not exist for: \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
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
            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that output file and working directory paths are compatible." ) );
        }
    }
    
    if ( ((LongitudeProperty == null) || (LongitudeProperty.length() == 0)) &&
        ((WKTGeometryProperty == null) || (WKTGeometryProperty.length() == 0)) ) {
        message = "The longitude property OR WKT geometry property must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the longitude OR WKT geometry property." ) );
    }
    
    if ( ((LongitudeProperty != null) && (LongitudeProperty.length() != 0)) &&
        ((WKTGeometryProperty != null) && (WKTGeometryProperty.length() != 0)) ) {
        message = "The longitude property OR WKT geometry property must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the longitude OR WKT geometry property." ) );
    }
    
    if ( ((LatitudeProperty == null) || (LatitudeProperty.length() == 0)) &&
        ((WKTGeometryProperty == null) || (WKTGeometryProperty.length() == 0)) ) {
        message = "The latitude property OR WKT geometry property must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the latitude column OR WKT geometry property." ) );
    }
    
    if ( ((LatitudeProperty != null) && (LatitudeProperty.length() != 0)) &&
        ((WKTGeometryProperty != null) && (WKTGeometryProperty.length() != 0)) ) {
        message = "The latitude property OR WKT geometry property must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the latitude column OR WKT geometry property." ) );
    }
    
    String StyleFile_full = null;
    if ( (StyleFile != null) && (StyleFile.length() != 0) && (StyleFile.indexOf("${") < 0) ) {
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
                // Working directory is available so use it...
                if ( o != null ) {
                    working_dir = (String)o;
                }
            }
            catch ( Exception e ) {
                message = "Error requesting WorkingDir from processor.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing style file." ) );
            }
    
        try {
            StyleFile_full = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,StyleFile)));
            File f = new File ( StyleFile_full );
            if ( !f.exists() ) {
                message = "The style file does not exist:  \"" + StyleFile_full + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the style file exists - may be OK if created at run time." ) );
            }
        }
        catch ( Exception e ) {
            message = "The style file:\n" + "    \"" + StyleFile +
            "\"\ncannot be adjusted using the working directory:\n" + "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                     message, "Verify that style file and working directory paths are compatible." ) );
        }
    }
    
    if ( (Precision != null) && !Precision.equals("") ) {
        if ( !StringUtil.isInteger(Precision) ) {
            message = "The precision \"" + Precision + "\" is not an integer.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the precision as an integer." ) );
        }
    }
    
    if ( (MissingValue != null) && !MissingValue.equals("") ) {
        if ( !StringUtil.isDouble(MissingValue) ) {
            message = "The missing value \"" + MissingValue + "\" is not a number.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the missing value as a number." ) );
        }
    }

    if ( (OutputStart != null) && !OutputStart.equals("")) {
        try {   DateTime datetime1 = DateTime.parse(OutputStart);
            if ( datetime1 == null ) {
                throw new Exception ("bad date");
            }
        }
        catch (Exception e) {
            message = "Output start date/time \"" + OutputStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid output start date/time." ) );
        }
    }
    if ( (OutputEnd != null) && !OutputEnd.equals("")) {
        try {   DateTime datetime2 = DateTime.parse(OutputEnd);
            if ( datetime2 == null ) {
                throw new Exception ("bad date");
            }
        }
        catch (Exception e) {
            message = "Output end date/time \"" + OutputEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid output end date/time." ) );
        }
    }

    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(20);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "OutputFile" );
    validList.add ( "Name" );
    validList.add ( "Description" );
    validList.add ( "LongitudeProperty" );
    validList.add ( "LatitudeProperty" );
    validList.add ( "ElevationProperty" );
    validList.add ( "WKTGeometryProperty" );
    validList.add ( "GeometryInsert" );
    validList.add ( "PlacemarkName" );
    validList.add ( "PlacemarkDescription" );
    validList.add ( "StyleInsert" );
    validList.add ( "StyleFile" );
    validList.add ( "StyleUrl" );
    validList.add ( "Precision" );
    validList.add ( "MissingValue" );
    validList.add ( "OutputStart" );
    validList.add ( "OutputEnd" );
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
{   // The command will be modified if changed...
    return (new WriteTimeSeriesToKml_JDialog ( parent, this )).ok();
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

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   String routine = getClass().getSimpleName() + ".runCommandInternal", message;
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    
    // Clear the output file
    
    setOutputFile ( null );
    
    // Check whether the processor wants output files to be created...

    CommandProcessor processor = getCommandProcessor();
    if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
            Message.printStatus ( 2, routine,
            "Skipping \"" + toString() + "\" because output is not being created." );
    }
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    CommandStatus status = getCommandStatus();
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
		status.clearLog(commandPhase);
	}

    PropList parameters = getCommandParameters();
    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
    String TSID = parameters.getValue ( "TSID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String OutputFile = parameters.getValue ( "OutputFile" ); // Expanded below
    String Name = parameters.getValue ( "Name" );
	if ( (commandPhase == CommandPhaseType.RUN) && (Name != null) && (Name.indexOf("${") >= 0) ) {
		Name = TSCommandProcessorUtil.expandParameterValue(processor, this, Name);
	}
    String Description = parameters.getValue ( "Description" );
	if ( (commandPhase == CommandPhaseType.RUN) && (Description != null) && (Description.indexOf("${") >= 0) ) {
		Description = TSCommandProcessorUtil.expandParameterValue(processor, this, Description);
	}
    String LongitudeProperty = parameters.getValue ( "LongitudeProperty" );
	if ( (commandPhase == CommandPhaseType.RUN) && (LongitudeProperty != null) && (LongitudeProperty.indexOf("${") >= 0) ) {
		LongitudeProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, LongitudeProperty);
	}
    String LatitudeProperty = parameters.getValue ( "LatitudeProperty" );
	if ( (commandPhase == CommandPhaseType.RUN) && (LatitudeProperty != null) && (LatitudeProperty.indexOf("${") >= 0) ) {
		LatitudeProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, LatitudeProperty);
	}
    String ElevationProperty = parameters.getValue ( "ElevationProperty" );
	if ( (commandPhase == CommandPhaseType.RUN) && (ElevationProperty != null) && (ElevationProperty.indexOf("${") >= 0) ) {
		ElevationProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, ElevationProperty);
	}
    String WKTGeometryProperty = parameters.getValue ( "WKTGeometryProperty" );
	if ( (commandPhase == CommandPhaseType.RUN) && (WKTGeometryProperty != null) && (WKTGeometryProperty.indexOf("${") >= 0) ) {
		WKTGeometryProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, WKTGeometryProperty);
	}
    String GeometryInsert = parameters.getValue ( "GeometryInsert" );
	if ( (commandPhase == CommandPhaseType.RUN) && (GeometryInsert != null) && (GeometryInsert.indexOf("${") >= 0) ) {
		GeometryInsert = TSCommandProcessorUtil.expandParameterValue(processor, this, GeometryInsert);
	}
    String PlacemarkName = parameters.getValue ( "PlacemarkName" ); // Expanded in TS scope
    String PlacemarkDescription = parameters.getValue ( "PlacemarkDescription" ); // Expanded in TS scope
    String StyleInsert = parameters.getValue ( "StyleInsert" );
	if ( (commandPhase == CommandPhaseType.RUN) && (StyleInsert != null) && (StyleInsert.indexOf("${") >= 0) ) {
		StyleInsert = TSCommandProcessorUtil.expandParameterValue(processor, this, StyleInsert);
	}
    String StyleFile = parameters.getValue ( "StyleFile" ); // Expanded below
    String StyleUrl = parameters.getValue ( "StyleUrl" );
	if ( (commandPhase == CommandPhaseType.RUN) && (StyleUrl != null) && (StyleUrl.indexOf("${") >= 0) ) {
		StyleUrl = TSCommandProcessorUtil.expandParameterValue(processor, this, StyleUrl);
	}

    // Get the time series to process...
    PropList request_params = new PropList ( "" );
    request_params.set ( "TSList", TSList );
    request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
    if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
    }
    @SuppressWarnings("unchecked")
	List<TS> tslist = (List<TS>)o_TSList;
    if ( tslist.size() == 0 ) {
        message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Confirm that time series are available (may be OK for partial run)." ) );
    }

    String OutputStart = parameters.getValue ( "OutputStart" );
    DateTime OutputStart_DateTime = null;
    if ( OutputStart != null ) {
        request_params = new PropList ( "" );
        request_params.set ( "DateTime", OutputStart );
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting DateTime(DateTime=" + OutputStart + ") from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object prop_contents = bean_PropList.getContents ( "DateTime" );
        if ( prop_contents == null ) {
            message = "Null value for DateTime(DateTime=" + OutputStart +
                "\") returned from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report problem to software support." ) );
        }
        else {
            OutputStart_DateTime = (DateTime)prop_contents;
        }
    }
    else {
        // Get from the processor (can be null)...
        try {
            Object o_OutputStart = processor.getPropContents ( "OutputStart" );
            if ( o_OutputStart != null ) {
                OutputStart_DateTime = (DateTime)o_OutputStart;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting OutputStart from processor - not using.";
            Message.printDebug(10, routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report problem to software support." ) );
        }
    }
    String OutputEnd = parameters.getValue ( "OutputEnd" );
    DateTime OutputEnd_DateTime = null;
    if ( OutputEnd != null ) {
        request_params = new PropList ( "" );
        request_params.set ( "DateTime", OutputEnd );
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting DateTime(DateTime=" + OutputEnd + ") from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object prop_contents = bean_PropList.getContents ( "DateTime" );
        if ( prop_contents == null ) {
            message = "Null value for DateTime(DateTime=" + OutputEnd +
            "\") returned from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report problem to software support." ) );
        }
        else {
            OutputEnd_DateTime = (DateTime)prop_contents;
        }
    }
    else {
        // Get from the processor...
        try {
            Object o_OutputEnd = processor.getPropContents ( "OutputEnd" );
            if ( o_OutputEnd != null ) {
                OutputEnd_DateTime = (DateTime)o_OutputEnd;
            }
        }
        catch ( Exception e ) {
            // Not fatal, but of use to developers.
            message = "Error requesting OutputEnd from processor - not using.";
            Message.printDebug(10, routine, message );
        }
    }

    String Precision = parameters.getValue ( "Precision" );
    Integer precision = new Integer(4);
    if ( (Precision != null) && (Precision.length() > 0) ) {
        precision = Integer.parseInt(Precision);
    }
    String MissingValue = parameters.getValue ( "MissingValue" ); 
    
    String OutputFile_full = OutputFile;
    try {
        // Convert to an absolute path...
        OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
        Message.printStatus ( 2, routine, "Writing KML file \"" + OutputFile_full + "\"" );
        List<String> errors = new Vector<String>();
        String kmlVersion = "2";
        String StyleFile_full = null;
        if ( (StyleFile != null) && !StyleFile.equals("") ) {
            StyleFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                    TSCommandProcessorUtil.expandParameterValue(processor,this,StyleFile)));
            if ( !IOUtil.fileReadable(StyleFile_full) || !IOUtil.fileExists(StyleFile_full)) {
                message = "Style file \"" + StyleFile_full + "\" is not found or accessible.";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
                status.addToLog(CommandPhaseType.RUN,
                    new CommandLogRecord( CommandStatusType.FAILURE, message,
                        "Verify that the file exists and is readable."));
                throw new CommandException ( message );
            }
        }
        writeTimeSeriesList ( tslist, OutputFile_full, kmlVersion,
            (TSCommandProcessor)processor, status,
            Name, Description,
            LongitudeProperty, LatitudeProperty, ElevationProperty, WKTGeometryProperty,
            GeometryInsert,
            PlacemarkName, PlacemarkDescription, StyleInsert, StyleFile_full, StyleUrl,
            precision, MissingValue, OutputStart_DateTime, OutputEnd_DateTime, errors );
        for ( String error : errors ) {
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, error );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    error, "Check log file for details." ) );
        }
        // Save the output file name...
        setOutputFile ( new File(OutputFile_full));
    }
    catch ( Exception e ) {
        message = "Unexpected error writing time series to KML file \"" + OutputFile_full + "\" (" + e + ")";
        Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Check log file for details." ) );
        throw new CommandException ( message );
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
{   if ( parameters == null ) {
        return getCommandName() + "()";
    }
    String TSList = parameters.getValue ( "TSList" );
    String TSID = parameters.getValue( "TSID" );
    String EnsembleID = parameters.getValue( "EnsembleID" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String Name = parameters.getValue ( "Name" );
    String Description = parameters.getValue ( "Description" );
    String LongitudeProperty = parameters.getValue ( "LongitudeProperty" );
    String LatitudeProperty = parameters.getValue ( "LatitudeProperty" );
    String ElevationProperty = parameters.getValue ( "ElevationProperty" );
    String WKTGeometryProperty = parameters.getValue ( "WKTGeometryProperty" );
    String GeometryInsert = parameters.getValue ( "GeometryInsert" );
    String PlacemarkName = parameters.getValue ( "PlacemarkName" );
    String PlacemarkDescription = parameters.getValue ( "PlacemarkDescription" );
    String StyleInsert = parameters.getValue ( "StyleInsert" );
    String StyleFile = parameters.getValue ( "StyleFile" );
    String StyleUrl = parameters.getValue ( "StyleUrl" );
    String Precision = parameters.getValue("Precision");
    String MissingValue = parameters.getValue("MissingValue");
    String OutputStart = parameters.getValue ( "OutputStart" );
    String OutputEnd = parameters.getValue ( "OutputEnd" );
    StringBuffer b = new StringBuffer ();
    if ( (TSList != null) && (TSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSList=" + TSList );
    }
    if ( (TSID != null) && (TSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSID=\"" + TSID + "\"" );
    }
    if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
    }
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"" );
    }
    if ( (Name != null) && (Name.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Name=\"" + Name + "\"" );
    }
    if ( (Description != null) && (Description.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Description=\"" + Description + "\"" );
    }
    if ( (LongitudeProperty != null) && (LongitudeProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "LongitudeProperty=\"" + LongitudeProperty + "\"" );
    }
    if ( (LatitudeProperty != null) && (LatitudeProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "LatitudeProperty=\"" + LatitudeProperty + "\"" );
    }
    if ( (ElevationProperty != null) && (ElevationProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ElevationProperty=\"" + ElevationProperty + "\"" );
    }
    if ( (WKTGeometryProperty != null) && (WKTGeometryProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WKTGeometryProperty=\"" + WKTGeometryProperty + "\"" );
    }
    if ( (GeometryInsert != null) && (GeometryInsert.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "GeometryInsert=\"" + GeometryInsert + "\"" );
    }
    if ( (PlacemarkName != null) && (PlacemarkName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PlacemarkName=\"" + PlacemarkName + "\"" );
    }
    if ( (PlacemarkDescription != null) && (PlacemarkDescription.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PlacemarkDescription=\"" + PlacemarkDescription + "\"" );
    }
    if ( (StyleInsert != null) && (StyleInsert.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "StyleInsert=\"" + StyleInsert + "\"" );
    }
    if ( (StyleFile != null) && (StyleFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "StyleFile=\"" + StyleFile + "\"" );
    }
    if ( (StyleUrl != null) && (StyleUrl.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "StyleUrl=\"" + StyleUrl + "\"" );
    }
    if ( (Precision != null) && (Precision.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Precision=" + Precision );
    }
    if ( (MissingValue != null) && (MissingValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MissingValue=" + MissingValue );
    }
    if ( (OutputStart != null) && (OutputStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputStart=\"" + OutputStart + "\"" );
    }
    if ( (OutputEnd != null) && (OutputEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputEnd=\"" + OutputEnd + "\"" );
    }
    return getCommandName() + "(" + b.toString() + ")";
}

// TODO SAM 2013-7-01 Evaluate whether a separate package should be created - for now keep the code here
// until there is time to split out
/**
Write the time series list to a KML file.
*/
private void writeTimeSeriesList ( List<TS> tslist, String outputFile, String kmlVersion,
    TSCommandProcessor processor, CommandStatus status,
    String name, String description,
    String longitudeProperty, String latitudeProperty, String elevationProperty, String wktGeometryProperty,
    String geometryInsert,
    String placemarkName, String placemarkDescription, String styleInsert, String styleFile, String styleUrl,
    Integer precision, String missingValue, DateTime outputStart, DateTime outputEnd,
    List<String> errors )
{   PrintWriter fout = null;
    try {
        FileOutputStream fos = new FileOutputStream ( outputFile );
        fout = new PrintWriter ( fos );
        // For now just support KML 2
        writeTimeSeriesList02 ( fout, tslist,
            processor, status,
            name, description,
            longitudeProperty, latitudeProperty, elevationProperty, wktGeometryProperty,
            geometryInsert,
            placemarkName, placemarkDescription, styleInsert, styleFile, styleUrl,
            precision, missingValue, outputStart, outputEnd, errors );
    }
    catch ( FileNotFoundException e ) {
        errors.add ( "Output file \"" + outputFile + "\" could not be created (" + e + ")." );
    }
    finally {
        try {
            fout.close();
        }
        catch ( Exception e ) {
        }
    } 
}

/**
Write the version 02 format KML.
@param fout open PrintWriter to write to
@param tslist list of time series to write
@param outputStart output start or null to write full period
@param outputEnd output end or null to write full period
@param overlap if true, write the data in overlapping fashion (date/time shared between time series)
@param errors list of error strings to be propagated to calling code
*/
private void writeTimeSeriesList02 ( PrintWriter fout, List<TS> tslist,
    TSCommandProcessor processor, CommandStatus status,
    String name, String description,
    String longitudeProperty, String latitudeProperty, String elevationProp, String wktGeometryProperty,
    String geometryInsert,
    String placemarkName, String placemarkDescription, String styleInsert, String styleFile, String styleUrl,
    Integer precision, String missingValue, DateTime outputStart, DateTime outputEnd,
    List<String> errors )
{   // Write the header
    String i1 = " ";
    String i2 = "  ";
    String i3 = "   ";
    String i4 = "    ";
    String i5 = "     ";
    String i6 = "      ";
    String i7 = "       ";
    boolean doPlacemarkName = false, doPlacemarkDescription = false, doStyleUrl = false, doPoint = false, doWkt = false;;
    if ( (placemarkName != null) && !placemarkName.equals("") ) {
        doPlacemarkName = true;
    }
    if ( (placemarkDescription != null) && !placemarkDescription.equals("") ) {
        doPlacemarkDescription = true;
    }
    // WKT trumps point data
    if ( (wktGeometryProperty != null) && !wktGeometryProperty.equals("") ) {
        doWkt = true;
    }
    else {
        if ( (latitudeProperty != null) && !latitudeProperty.equals("") &&
            (longitudeProperty != null) && !longitudeProperty.equals("")) {
            doPoint = true;
        }
    }
    if ( (styleUrl != null) && !styleUrl.equals("") ) {
        doStyleUrl = true;
    }
    if ( name == null ) {
        name = "";
    }
    if ( name.startsWith("<") ) {
        name = "<![CDATA[\n" + name + "]]>";
    }
    if ( description == null ) {
        description = "";
    }
    if ( description.startsWith("<") ) {
        description = "<![CDATA[\n" + description + "]]>";
    }
    fout.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
        i1 + "<Document>\n" +
        i2 + "<name>" + name + "</name>\n" +
        i2 + "<description>" + description + "</description>\n");
    // TODO evaluate whether to default styles based on data type
    // Write the styles based on supplied information
    if ( (styleInsert != null) && !styleInsert.equals("") ) {
        fout.write(i2 + styleInsert + "\n");
    }
    else if ( (styleFile != null) && !styleFile.equals("") ) {
        // Insert styles from the contents of the SQL file
        try {
            List<String> list = IOUtil.fileToStringList(styleFile);
            for ( String line : list ) {
                fout.write(i2 + line + "\n" );
            }
        }
        catch ( Exception e ) {
            errors.add("Error adding style file to KML (" + e + ")." ); 
        }
    }
    Object longitude, latitude, elevation; // Can be double, int, or string
    String placemarkNameOut, placemarkDescriptionOut;
    WKTGeometryParser wktParser = null;
    Object wktO;
    GRShape shape;
    if ( doWkt ) {
        wktParser = new WKTGeometryParser();
    }
    for ( TS ts : tslist ) {
        longitude = null;
        latitude = null;
        elevation = null;
        shape = null;
        if ( doPoint ) {
            longitude = ts.getProperty(longitudeProperty);
            latitude = ts.getProperty(latitudeProperty);
            if ( (longitude == null) || (latitude == null) ) {
                continue;
            }
            if ( elevationProp != null ) {
                elevation = ts.getProperty(elevationProp);
            }
        }
        placemarkNameOut = ts.getLocation(); // Default
        placemarkDescriptionOut = ts.getDescription(); // Default
        if ( doPlacemarkName ) {
            placemarkNameOut = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
                processor, ts, placemarkName, status, CommandPhaseType.RUN);
        }
        if ( doPlacemarkDescription ) {
            placemarkDescriptionOut = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
                processor, ts, placemarkDescription, status, CommandPhaseType.RUN);
        }
        fout.write(i2 + "<Placemark>\n");
        // TODO SAM 2013-07-01 use formatting strings for name
        fout.write(i3 + "<name>" + placemarkNameOut + "</name>\n");
        if ( placemarkDescriptionOut.startsWith("<") ) {
            placemarkDescriptionOut = "<![CDATA[\n" + placemarkDescriptionOut + "]]>";
        }
        fout.write(i3 + "<description>" + placemarkDescriptionOut + "</description>\n");
        if ( doStyleUrl ) {
            fout.write(i3 + "<styleUrl>" + styleUrl + "</styleUrl>\n");
        }
        if ( doPoint ) {
            fout.write(i3 + "<Point>\n");
            if ( (geometryInsert != null) && !geometryInsert.equals("") ) {
                fout.write(i4 +geometryInsert + "\n");
            }
            if ( elevation == null ) {
                fout.write(i4 + "<coordinates>" + longitude + "," + latitude + "</coordinates>\n");
            }
            else {
                fout.write(i4 + "<coordinates>" + longitude + "," + latitude + "," + elevation + "</coordinates>\n");
            }
            fout.write(i3 + "</Point>\n");
        }
        else if ( doWkt ) {
            wktO = ts.getProperty(wktGeometryProperty);;
            shape = wktParser.parseWKT((String)wktO);
            if ( shape != null ) {
                if ( shape instanceof GRPoint ) {
                    GRPoint pt = (GRPoint)shape;
                    longitude = new Double(pt.x);
                    latitude = new Double(pt.y);
                    if ( shape instanceof GRPointZM ) {
                        elevation = new Double(((GRPointZM)pt).z);
                    }
                    fout.write(i3 + "<Point>\n");
                    if ( (geometryInsert != null) && !geometryInsert.equals("") ) {
                        fout.write(i4 +geometryInsert + "\n");
                    }
                    if ( elevation == null ) {
                        fout.write(i4 + "<coordinates>" + longitude + "," + latitude + "</coordinates>\n");
                    }
                    else {
                        fout.write(i4 + "<coordinates>" + longitude + "," + latitude + "," + elevation + "</coordinates>\n");
                    }
                    fout.write(i3 + "</Point>\n");
                }
                else if ( shape instanceof GRPolygon ) {
                    GRPolygon p = (GRPolygon)shape;
                    fout.write(i3 + "<Polygon>\n");
                    if ( (geometryInsert != null) && !geometryInsert.equals("") ) {
                        fout.write(i4 +geometryInsert + "\n");
                    }
                    fout.write(i4 + "<outerBoundaryIs>\n" );
                    fout.write(i5 + "<LinearRing>\n");
                    fout.write(i6 + "<coordinates>\n");
                    Double longitudeD = null, latitudeD = null, elevationD = null;
                    Double longitudeD0 = null, latitudeD0 = null, elevationD0 = null;
                    for ( int i = 0; i < p.npts; i++ ) {
                        longitudeD = new Double(p.pts[i].x);
                        latitudeD = new Double(p.pts[i].y);
                        // TODO SAM 2014-02-23 Need to enable ZM
                        //if ( shape instanceof GRPolygonZM ) {
                        //    elevationD = new Double(((GRPointZM)p.pts[i]).z);
                        //}
                        if ( i == 0 ) {
                            // Save first point and output at end if needed to close ring
                            longitudeD0 = longitudeD;
                            latitudeD0 = latitudeD;
                            elevationD0 = elevationD;
                        }
                        if ( elevationD == null ) {
                            fout.write(i7 + longitudeD + "," + latitudeD + "\n");
                        }
                        else {
                            fout.write(i7 + longitudeD + "," + latitudeD + "," + elevationD + "\n");
                        }
                    }
                    // Write the first point again if it was not written at the end
                    if ( elevationD == null ) {
                        if ( !longitudeD.equals(longitudeD0) || !latitudeD.equals(latitudeD0) ) {
                            fout.write(i7 + longitudeD0 + "," + latitudeD0 + "\n");
                        }
                    }
                    else {
                        if ( !longitudeD.equals(longitudeD0) || !latitudeD.equals(latitudeD0)
                            || !elevationD.equals(elevationD0) ) {
                            fout.write(i7 + longitudeD0 + "," + latitudeD0 + "," + elevationD0 + "\n");
                        }
                    }
                      
                    fout.write(i6 + "</coordinates>\n");
                    fout.write(i5 + "</LinearRing>\n");
                    fout.write(i4 + "</outerBoundaryIs>\n" );
                    fout.write(i3 + "</Polygon>\n");
                }
            }
        }
        fout.write(i2 + "</Placemark>\n");
    }
    fout.write( i1 + "</Document>\n" );   
    fout.write( "</kml>\n" );
}

}
