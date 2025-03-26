// writeNWSRFSESPTraceEnsemble_Command - handle the writeNWSRFSESPTraceEnsemble() command

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

package rti.tscommandprocessor.commands.nwsrfs;

import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;

import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;

import RTi.Util.String.StringUtil;

import RTi.DMI.NWSRFS_DMI.NWSRFS_ESPTraceEnsemble;
import RTi.TS.TS;

/**
<p>
This class initializes, checks, and runs the WriteNWSRFSESPTraceEnsemble() command.
</p>
*/
public class WriteNWSRFSESPTraceEnsemble_Command extends AbstractCommand implements Command, FileGenerator
{

protected static final String
	_FALSE = "False",
	_TRUE = "True";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteNWSRFSESPTraceEnsemble_Command ()
{
	super();
	setCommandName ( "WriteNwsrfsEspTraceEnsemble" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor
dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    CommandProcessor processor = getCommandProcessor();
	
	// Get the property values. 
	String OutputFile = parameters.getValue("OutputFile");
	// TODO KAT 2007-02-20 Implement checks on all parameters
	//String CarryoverGroup = parameters.getValue("CarryoverGroup");
	//String ForecastGroup = parameters.getValue("ForecastGroup");
	//String Segment = parameters.getValue("Segment");
	//String SegmentDescription = parameters.getValue("SegmentDescription");
	String Latitude = parameters.getValue("Latitude");
	String Longitude = parameters.getValue("Longitude");
	//String RFC = parameters.getValue("RFC");
	//String TSList = parameters.getValue("TSList");
	
	// OutputFile
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        message = "The output file: \"" + OutputFile + "\" must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an output file." ) );
    }
    else {
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
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                    TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
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
    }
	
    if ( (Latitude != null) && (Latitude.length() > 0) && !StringUtil.isDouble(Latitude) ) {
        message = "The latitude is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                 message, "Specify the latitude as a number -180 to 180." ) );
    }
    if ( (Longitude != null) && (Longitude.length() > 0) && !StringUtil.isDouble(Longitude) ) {
        message = "The longitude is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                 message, "Specify the longitude as a number -180 to 180." ) );
    }
    
    // Make sure the filename matches the 5-part ESPADP format standard.
    // Currently, nothing is checked apart from whether there are
    // 4 periods.  Invalid units and intervals can still be entered.
    
    int nperiods = StringUtil.patternCount( OutputFile, "." );
    if ( nperiods != 4) {
        message = "The output file name does not follow the standard.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the output file name similar to: SEGID.TSID.TSTYPE.06.CS" ) );
    }
    
    List<String> validList = new Vector<String>();
    validList.add ( "OutputFile" );
    validList.add ( "CarryoverGroup" );
    validList.add ( "ForecastGroup" );
    validList.add ( "Segment" );
    validList.add ( "SegmentDescription" );
    validList.add ( "Latitude" );
    validList.add ( "Longitude" );
    validList.add ( "RFC" );
    validList.add ( "TSList" );
    validList.add ( "EnsembleID" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, warning_level ),
			warning );
		throw new InvalidCommandParameterException ( warning );
	}
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand(JFrame parent) {
	// The command will be modified if changed...
	return ( new WriteNWSRFSESPTraceEnsemble_JDialog(parent, this)).ok();
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
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
	// This is the new format of parsing, where parameters are
	// specified as "InputFilter=", etc.
	String routine = "writeNWSRFSESPTraceEnsemble_Command.parseCommand", 
	       message = null;
	int warning_level = 2;
	//int warning_count = 0;

	List<String> tokens = StringUtil.breakStringList(command, "()", 0);
	if ((tokens == null) || tokens.size() < 2) {
		// Must have at least the command name and the InputFile
		message = "Syntax error in \"" + command + "\".  Expecting " + getCommandName() + "().";
		Message.printWarning ( warning_level, routine, message);
		//++warning_count;
		throw new InvalidCommandSyntaxException ( message );
	}

	// Get the input needed to process the file...
	try {
		setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
			(String)tokens.get(1), routine, "," ) );
	}
	catch ( Exception e ) {
		message = "Syntax error in \"" + command + "\".  Expecting " + getCommandName() + "().";
		Message.printWarning ( warning_level,routine, message);
		//++warning_count;
		throw new InvalidCommandSyntaxException ( message );
	}
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
       CommandWarningException,
       CommandException
{	String routine = "writeNWSRFSESPTraceEnsemble_Command.runCommand", message = null;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;	// Log message level for non-user warnings
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    
    // Clear the output file
    
    setOutputFile ( null );

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters ();
	String OutputFile = parameters.getValue ( "OutputFile" );
	String CarryoverGroup = parameters.getValue ( "CarryoverGroup" );
	String ForecastGroup = parameters.getValue ( "ForecastGroup" );
	String Segment = parameters.getValue ( "Segment" );
	String SegmentDescription = parameters.getValue("SegmentDescription");
	String Latitude = parameters.getValue ( "Latitude" );
	String Longitude = parameters.getValue ( "Longitude" );
	String RFC = parameters.getValue ( "RFC" );
	String TSList = parameters.getValue ( "TSList" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (TSList == null) || (TSList.length() == 0) ) {
        if ( (EnsembleID != null) && !EnsembleID.equals("") ) {
            TSList = TSListType.ENSEMBLE_ID.toString();
        }
        else {
            // No ensemble so use old default...
            TSList = TSListType.ALL_TS.toString();
        }
	}
		
	List<TS> tslist = null;
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	//request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
	CommandProcessor processor = getCommandProcessor();
	CommandProcessorRequestResultsBean bean = null;
	try { bean =
		processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList + "\", EnsembleID=\"" +
        EnsembleID + "\") from processor.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Report the problem to software support."));
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	if ( o_TSList == null ) {
		message = "Unable to find time series to scale using TSList=\"" + TSList + "\", EnsembleID=\"" +
        EnsembleID + "\".";
		Message.printWarning ( log_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Report the problem to software support."));
	}
	else {
		@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
		tslist = tslist0;
		if ( tslist.size() == 0 ) {
			message = "Unable to find time series to write using TSList=\"" + TSList + "\", EnsembleID=\"" +
			EnsembleID + "\"";
			Message.printWarning ( log_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count), routine, message );
            status.addToLog(CommandPhaseType.RUN,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message,
                    "Verify that a list of time series to output is correctly specified."));
		}
	}
	
	if ((tslist == null) || (tslist.size() == 0)) {
		message = "Unable to find time series to write using TSList=\"" + TSList + "\", EnsembleID=\"" +
        EnsembleID + "\".";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
        status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Verify that a list of time series to output is correctly specified."));
	}
	Message.printStatus( 2, routine, "Will write " + tslist.size() + " time series traces.");

	PropList props = new PropList ( "writeNWSRFSESPTraceEnsemble" );
	// Transfer properties to that needed by ESP...
	if ( CarryoverGroup != null ) {
		props.set ( "CarryoverGroup", CarryoverGroup );
	}
	if ( ForecastGroup != null ) {
		props.set ( "ForecastGroup", ForecastGroup );
	}
	if ( Segment != null ) {
		props.set ( "Segment", Segment );
	}
	if ( SegmentDescription != null ) {
		props.set ( "SegmentDescription", SegmentDescription );
	}
	if ( Latitude != null ) {
		props.set ( "Latitude", Latitude );
	}
	if ( Longitude != null ) {
		props.set ( "Longitude", Longitude );
	}
	if ( RFC != null ) {
		props.set ( "RFC", RFC );
	}
	String OutputFile_full = OutputFile;
	try {
		NWSRFS_ESPTraceEnsemble esp = new NWSRFS_ESPTraceEnsemble( tslist, props );
        OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                        TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)) );
		esp.writeESPTraceEnsembleFile ( OutputFile_full );
        // Save the output file name...
        setOutputFile ( new File(OutputFile_full));
	}
	catch (Exception e) {
		message = "Unexpected error occurred while writing to the file \"" + OutputFile_full + "\" (" + e + ").";
		Message.printWarning(warning_level,
			MessageUtil.formatMessageTag(command_tag, 
				++warning_count), routine, message);
		Message.printWarning ( 3, routine, e );
        status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Check the log file for more details."));
	}
	
	// Throw CommandWarningException in case of problems.
	if (warning_count > 0) {
		String plural_s = "s";
		if (warning_count == 1) {
			plural_s = "";
		}
		String plural_verb = "were";
		if (warning_count == 1) {
			plural_verb = "was";
		}
		
		message = "There " + plural_verb + " " + warning_count + " warning" + plural_s + " processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count ),routine, message );
		throw new CommandWarningException ( message );
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
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

	String OutputFile = props.getValue ( "OutputFile" );
	String CarryoverGroup = props.getValue ( "CarryoverGroup" );
	String ForecastGroup = props.getValue ( "ForecastGroup" );
	String Segment = props.getValue ( "Segment" );
	String SegmentDescription = props.getValue ( "SegmentDescription" );
	String Latitude = props.getValue ( "Latitude" );
	String Longitude = props.getValue ( "Longitude" );
	String RFC = props.getValue ( "RFC" );
	String TSList = props.getValue ( "TSList" );
    String EnsembleID = props.getValue ( "EnsembleID" );

	StringBuffer b = new StringBuffer ();

	// Input File
	if ((OutputFile != null) && (OutputFile.length() > 0)) {
		b.append("OutputFile=\"" + OutputFile + "\"");
	}

	// CarryoverGroup
	if ((CarryoverGroup != null) && (CarryoverGroup.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("CarryoverGroup=\"" + CarryoverGroup + "\"");
	}

	// ForecastGroup
	if ((ForecastGroup != null) && (ForecastGroup.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("ForecastGroup=\"" + ForecastGroup + "\"");
	}

	// Segment
	if ((Segment != null) && (Segment.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("Segment=\"" + Segment + "\"");
	}

	// SegmentDescription
	if ((SegmentDescription != null) && (SegmentDescription.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("SegmentDescription=\"" + SegmentDescription + "\"");
	}

	// Latitude
	if ((Latitude != null) && (Latitude.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("Latitude=\"" + Latitude + "\"");
	}

	// Longitude
	if ((Longitude != null) && (Longitude.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("Longitude=\"" + Longitude + "\"");
	}

	// RFC
	if ((RFC != null) && (RFC.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("RFC=\"" + RFC + "\"");
	}

	// TSList
	if ((TSList != null) && (TSList.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("TSList=\"" + TSList + "\"");
	}
    if ((EnsembleID != null) && (EnsembleID.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("EnsembleID=\"" + EnsembleID + "\"");
    }

	return getCommandName() + "(" + b.toString() + ")";
}

} // end writeNWSRFSESPTraceEnsemble_Command
