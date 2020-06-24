// WriteStateMod_Command - WriteStateMod() command

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

//------------------------------------------------------------------------------
// writeStateMod_Command - handle the writeStateMod() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-08-30	Steven A. Malers, RTi	Initial version.  Copy and modify
//					writeRiverWare().
// 2005-11-22	SAM, RTi		Fix so that a negative precision is
//					allowed, since it is used for special
//					formatting.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.statemod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import RTi.TS.TS;
import RTi.TS.TSUtil;

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
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.YearType;

import DWR.StateMod.StateMod_TS;

/**
This class initializes, checks, and runs the WriteStateMod() command.
*/
public class WriteStateMod_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteStateMod_Command ()
{	super();
	setCommandName ( "WriteStateMod" );
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
{	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String MissingValue = parameters.getValue ( "MissingValue" );
	String Precision = parameters.getValue ( "Precision" );
	String warning = "";
	String message;
	
	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (TSList != null) && (TSList.length() > 0) ) {
		if ( TSList.equalsIgnoreCase("" + TSListType.ALL_MATCHING_TSID) && ((TSID == null) || (TSID.length() == 0)) ) {
			message = "TSList=" + TSListType.ALL_MATCHING_TSID + " requires a TSID value.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a TSID (pattern) to match." ) );
		}
	}

	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
		message = "The output file must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify an output file." ) );
	}
	else if ( OutputFile.indexOf("${") < 0 ) {
		// Does not contain property notation so can check folder
		String working_dir = null;		
		try { Object o = processor.getPropContents ( "WorkingDir" );
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
							message, "Software error - report to support." ) );
		}
	
		try {
			String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, OutputFile));
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				message = "The output file parent directory does " +
				"not exist: \"" + f2 + "\".";
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

	if (	(OutputStart != null) && !OutputStart.equals("") &&
		!OutputStart.equalsIgnoreCase("OutputStart") &&
		!OutputStart.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse(OutputStart);
		}
		catch ( Exception e ) {
			message = "Output start date/time \"" + OutputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid output start date/time, OutputStart, or OutputEnd." ) );
		}
	}
	if (	(OutputEnd != null) && !OutputEnd.equals("") &&
		!OutputEnd.equalsIgnoreCase("OutputStart") &&
		!OutputEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse( OutputEnd );
		}
		catch ( Exception e ) {
				message = "Output end date/time \"" + OutputEnd + "\" is not a valid date/time.";
				warning += "\n" + message;
				status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid output end date/time, OutputStart, or OutputEnd." ) );
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

	if ( (Precision != null) && !Precision.equals("") ) {
		if ( !StringUtil.isInteger(Precision) ) {
			message = "The precision \"" + Precision + "\" is not an integer.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify the precision as an integer." ) );
		}
	}
	
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(7);
	validList.add ( "TSList" );
	validList.add ( "TSID" );
	validList.add ( "OutputFile" );
	validList.add ( "OutputStart" );
	validList.add ( "OutputEnd" );
	validList.add ( "MissingValue" );
	validList.add ( "Precision" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),
		warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
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
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new WriteStateMod_JDialog ( parent, this )).ok();
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
{	String routine = "writeStateMod_Command.parseCommand", message;
	int warning_level = 2;
	if ( (command_string.indexOf("=") > 0) || command_string.endsWith("()") ) {
		// New syntax, can be blank parameter list...
		super.parseCommand ( command_string );
	}
	else {
	    // Parse the old command...
		List<String> tokens = StringUtil.breakStringList ( command_string,
			"(,)", StringUtil.DELIM_ALLOW_STRINGS );
		if ( tokens.size() != 3 ) {
			message = "Invalid syntax for command \"" + command_string + "\".  Expecting 2 parameters.";
			Message.printWarning ( warning_level, routine, message);
			CommandStatus status = getCommandStatus();
			status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Verify command syntax or edit with command editor." ) );
			throw new InvalidCommandSyntaxException ( message );
		}
		String OutputFile = tokens.get(1).trim();
		String Precision = tokens.get(2).trim();
		if ( Precision.equals("*") ) {
			Precision = "";
		}
		// Defaults because not in the old command...
		String TSList = "AllTS";
		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		parameters.set ( "TSList", TSList );
		if ( OutputFile.length() > 0 ) {
			parameters.set ( "OutputFile", OutputFile );
		}
		if ( Precision.length() > 0 ) {
			parameters.set ( "Precision", Precision );
		}
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;	// Warning level for non-user log messages
	
	// Clear the output file
	
	setOutputFile ( null );
	
	// Check whether the processor wants output files to be created...

	CommandProcessor processor = getCommandProcessor();
	if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
			Message.printStatus ( 2, routine,
			"Skipping \"" + toString() + "\" because output is not being created." );
	}

	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && TSID.indexOf("${") >= 0 ) {
        TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
	String OutputFile = parameters.getValue ( "OutputFile" ); // Expanded for property below
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);

	// Get the time series to process...
	
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try {
	    bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\" from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	List<TS> tslist = null;
	if ( o_TSList == null ) {
		message = "Unable to find time series to write using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( log_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report to software support." ) );
	}
	else {
		@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
	    tslist = tslist0;
		if ( tslist.size() == 0 ) {
			message = "Unable to find time series to write using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
			Message.printWarning ( log_level,
					MessageUtil.formatMessageTag(
							command_tag,++warning_count), routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report to software support." ) );
		}
	}
	
	if ( (tslist == null) || (tslist.size() == 0) ) {
		message = "Unable to find time series to write using TSID \"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.WARNING,
						message, "Confirm that time series are available (may be OK for partial run)." ) );
	}

	String OutputStart = parameters.getValue ( "OutputStart" );
	DateTime OutputStart_DateTime = null;
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	DateTime OutputEnd_DateTime = null;

	if ( (OutputStart != null) && !OutputStart.equals("") ) {
		try {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", OutputStart );
		bean = null;
		try {
		    bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting OutputStart DateTime(DateTime=" + OutputStart + ") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for OutputStart DateTime(DateTime=" +
			OutputStart +	"\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	OutputStart_DateTime = (DateTime)prop_contents;
		}
	}
	catch ( Exception e ) {
		message = "OutputStart \"" + OutputStart + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify a valid output start date/time." ) );
		throw new InvalidCommandParameterException ( message );
	}
	}
	else {	// Get from the processor...
		try {	Object o = processor.getPropContents ( "OutputStart" );
				if ( o != null ) {
					OutputStart_DateTime = (DateTime)o;
				}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting OutputStart from processor - not using.";
			Message.printDebug(10, routine, message );
		}
	}
	if ( (OutputEnd != null) && !OutputEnd.equals("") ) {
			try {
			request_params = new PropList ( "" );
			request_params.set ( "DateTime", OutputEnd );
			bean = null;
			try { bean =
				processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting OutputEnd DateTime(DateTime=" + OutputEnd + ") from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
				status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Report problem to software support." ) );
				throw new InvalidCommandParameterException ( message );
			}

			bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for OutputEnd DateTime(DateTime=" +
				OutputEnd +	"\") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
				status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Report problem to software support." ) );
				throw new InvalidCommandParameterException ( message );
			}
			else {	OutputEnd_DateTime = (DateTime)prop_contents;
			}
		}
		catch ( Exception e ) {
			message = "OutputEnd \"" + OutputEnd + "\" is invalid.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid output start date/time." ) );
			throw new InvalidCommandParameterException ( message );
		}
		}
		else {	// Get from the processor...
			try {	Object o = processor.getPropContents ( "OutputEnd" );
					if ( o != null ) {
						OutputEnd_DateTime = (DateTime)o;
					}
			}
			catch ( Exception e ) {
				// Not fatal, but of use to developers.
				message = "Error requesting OutputEnd from processor - not using.";
				Message.printDebug(10, routine, message );
			}
	}

	String Precision = parameters.getValue ( "Precision" );
	String MissingValue = parameters.getValue ( "MissingValue" );

	String OutputYearType = "Calendar";	// Default
	YearType outputYearType = YearType.CALENDAR;
	try {
	    Object o = processor.getPropContents ( "OutputYearType" );
		// Output year type is available so use it...
		if ( o != null ) {
			outputYearType = (YearType)o;
		}
	}
	catch ( Exception e ) {
		message = "Error requesting OutputYearType from processor - defaulting to calendar.";
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Report problem to software support." ) );
		Message.printDebug(10, routine, message );
	}
	// Check OutputYearType at runtime because it is specified in earlier commands.
	
    if ( (outputYearType != YearType.CALENDAR) && (outputYearType != YearType.WATER)
    	&& (outputYearType != YearType.NOV_TO_OCT) ) {
        message = "\nThe output year type (" + OutputYearType +
            ") must be \"" + YearType.CALENDAR + "\", \"" + YearType.WATER + "\", or \"" +
            YearType.NOV_TO_OCT + "\".  Defaulting to Calendar.";
        outputYearType = YearType.CALENDAR;
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.WARNING,
				message, "Check the SetOutputYearType() command." ) );
    }
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Now try to write...

    String OutputFile_full = OutputFile;
	try {
		OutputFile_full = IOUtil.verifyPathForOS(IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            TSCommandProcessorUtil.expandParameterValue(processor, this,OutputFile)));
		Message.printStatus ( 2, routine,"Writing StateMod file \"" + OutputFile_full + "\"" );
		
		// Make sure the parent folder exists
		File f = new File(OutputFile_full);
		File parent = f.getParentFile();
		if ( !parent.exists() ) {
			message = "Parent folder (" + parent.getAbsolutePath() + ") of of output file does not exist. Can't write output.";
			Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(command_tag,
			++warning_count), routine, message );
			status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Verify that all time series being written have interval of either Day or Month." ) );
		}

		// Get the comments to add to the top of the file.

		List<String> OutputComments_Vector = null;
		try {
			Object o = processor.getPropContents ( "OutputComments" );
			// Comments are available so use them...
			if ( o != null ) {
				@SuppressWarnings("unchecked")
				List<String> OutputComments_Vector0 = (List<String>)o;
				OutputComments_Vector = OutputComments_Vector0;
			}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting OutputComments from processor - not using.";
			Message.printDebug(10, routine, message );
		}

		TS ts = null;	// Time series being checked.
		if ( TSUtil.intervalsMatch ( tslist ) ) {
			// The time series to write have the same interval so
			// write using the first interval....
			int interval = TimeInterval.UNKNOWN;
			int size = 0;
			if ( tslist != null ) {
				size = tslist.size();
			}
			for ( int i = 0; i < size; i++ ) {
				ts = tslist.get(i);
				if ( ts == null ) {
					continue;
				}
				interval = ts.getDataIntervalBase();
				break;
			}
			if ( interval == TimeInterval.UNKNOWN ) {
				message = "Unable to determine time interval from time series.  Can't write output.";
				Message.printWarning ( warning_level, 
				MessageUtil.formatMessageTag(command_tag,
				++warning_count), routine, message );
				status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that all time series being written have interval of either Day or Month." ) );
				throw new CommandException ( message );
			}
			else if ((interval == TimeInterval.DAY) || (interval == TimeInterval.MONTH) ) {
				PropList smprops = new PropList ( "StateMod" );
				// Don't set output file since it is null...
				smprops.set ( "OutputFile", OutputFile_full );
				if ( OutputComments_Vector != null ) {
					smprops.setUsingObject ( "NewComments",	StringUtil.toArray(	OutputComments_Vector) );
				}
				if ( OutputStart_DateTime != null ) {
					smprops.set("OutputStart=" + OutputStart_DateTime.toString());
				}
				if ( OutputEnd_DateTime != null ) {
					smprops.set ( "OutputEnd=" + OutputEnd_DateTime.toString());
				}
				if ( OutputYearType != null ) {
					smprops.set ( "CalendarType", "" + outputYearType );
				}
				if ( MissingValue != null ) {
					smprops.set ( "MissingDataValue",MissingValue );
				}
				if ( Precision != null ) {
					smprops.set ( "OutputPrecision",Precision );
				}
				/* TODO SAM 2005-09-01 Need to handle from processor...
				if ( _detailedheader ) {
					smprops.set ( "PrintGenesis", "true" );
				}
				else {	smprops.set ( "PrintGenesis", "false" );
				}
				*/
				try {	
					StateMod_TS.writeTimeSeriesList ( tslist, smprops );
					// Save the output file name...
					setOutputFile ( new File(OutputFile_full));
				}
				catch ( Exception e ) {
					Message.printWarning ( 3, routine, e );
					message = "Unable to write StateMod file \"" + OutputFile_full + "\"";
					Message.printWarning ( warning_level, 
					MessageUtil.formatMessageTag(
					command_tag, ++warning_count), routine, message );
					status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Check log file for details." ) );
				}
			}
			else {
			    message = "Don't know how to write StateMod output for interval \"" +
			        ts.getIdentifier().getInterval() + "\": \"" + ts.getIdentifierString() + "\".";
				Message.printWarning ( warning_level, 
				MessageUtil.formatMessageTag(command_tag,
					++warning_count), routine, message );
				status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that all time series being written have interval of either Day or Month." ) );
				throw new CommandException ( message );
			}
		}
		else {
		    message = "Unable to write StateMod time series of different intervals.";
			Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(command_tag,
				++warning_count), routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Verify that all time series being written have interval of either Day or Month." ) );
			throw new CommandException ( message );
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Error writing time series to StateMod file \"" + OutputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check the log file for details." ) );
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
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String TSList = parameters.getValue("TSList");
	String TSID = parameters.getValue("TSID");
	String OutputFile = parameters.getValue("OutputFile");
	String OutputStart = parameters.getValue("OutputStart");
	String OutputEnd = parameters.getValue("OutputEnd");
	String MissingValue = parameters.getValue("MissingValue");
	String Precision = parameters.getValue("Precision");
	StringBuffer b = new StringBuffer ();
	if ( (TSList != null) && (TSList.length() > 0) ) {
		b.append ( "TSList=" + TSList );
	}
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
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
	if ( (MissingValue != null) && (MissingValue.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "MissingValue=" + MissingValue );
	}
	if ( (Precision != null) && (Precision.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Precision=" + Precision );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
