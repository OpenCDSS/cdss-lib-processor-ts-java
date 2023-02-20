// ReadStateCU_Command - handle the ReadStateCU() and TS Alias = ReadStateCU() commands

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
// readStateCU_Command - handle the readStateCU() and
//				TS Alias = readStateCU() commands
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2007-06-21	Steven A. Malers, RTi	Initial version.  Copy and modify
//					readStateMod_Command().
//------------------------------------------------------------------------------

package rti.tscommandprocessor.commands.statecu;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;
import RTi.TS.YearTS;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

import DWR.StateCU.StateCU_CropPatternTS;
import DWR.StateCU.StateCU_IrrigationPracticeTS;
import DWR.StateCU.StateCU_TS;

/**
<p>
This class initializes, checks, and runs the ReadStateCU() command.
</p>
*/
public class ReadStateCU_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Values for AutoAdjust
*/
protected final String _False = "False";
protected final String _True = "True";

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Indicates whether the TS Alias version of the command is being used...
*/
protected boolean _use_alias = false;

private String __working_dir = null;	// Application working directory

/**
Constructor.
*/
public ReadStateCU_Command ()
{	super();
	setCommandName ( "ReadStateCU" );
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
{	String InputFile = parameters.getValue ( "InputFile" );
	String InputStart = parameters.getValue ( "InputStart" );
	String InputEnd = parameters.getValue ( "InputEnd" );
    // TODO SAM Need to add checks to validate
	//String TSID = parameters.getValue ( "TSID" );
	String NewScenario = parameters.getValue ( "NewScenario" );
	String AutoAdjust = parameters.getValue ( "AutoAdjust" );
	String CheckData = parameters.getValue ( "CheckData" );
	String warning = "";
    String message;

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.WARNING,
				"The input file has not been specified.",
				"Specify a StateCU IPY file to read.  " +
				"May be OK if created dynamically during run." ) );
	}
	else if ( InputFile.indexOf("${") < 0 ) { // No properties used so can check path
		try { Object o = processor.getPropContents ( "WorkingDir" );
			// Working directory is available so use it...
			if ( o != null ) {
				__working_dir = (String)o;
			}
		}
		catch ( Exception e ) {
            message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
		}
	
		try {	
			// Adjust the path to the working directory...
			String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath ( __working_dir, InputFile));
			File f = new File ( adjusted_path );
			if ( !f.exists() ) {
                message = "The input file does not exist: \"" + adjusted_path + "\".";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.WARNING,
                        message,
                        "Specify a valid StateCU IPY file to read.  " +
                        "May be OK if created dynamically during run." ) );
			}
		}
		catch ( Exception e ) {
            message = "The input file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + __working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
		}

	}

	if (	(InputStart != null) && !InputStart.equals("") &&
		!InputStart.equalsIgnoreCase("InputStart") &&
		!InputStart.equalsIgnoreCase("InputEnd") ) {
		try {	DateTime.parse(InputStart);
		}
		catch ( Exception e ) {
               message = "The input start date/time \"" +InputStart + "\" is not a valid date/time.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a valid date/time, InputStart, InputEnd, or blank to use the global input start." ) );
		}
	}
	if (	(InputEnd != null) && !InputEnd.equals("") &&
		!InputEnd.equalsIgnoreCase("InputStart") &&
		!InputEnd.equalsIgnoreCase("InputEnd") ) {
		try {	DateTime.parse( InputEnd );
		}
		catch ( Exception e ) {
            message = "The input end date/time \"" +InputStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, InputStart, InputEnd, or blank to use the global input start." ) );
		}
	}
	// TODO SAM Need to check TSID - see newTimeSeries but support checking wildcards
	if ( (NewScenario != null) && !NewScenario.equals("") && (NewScenario.indexOf(" ") > 0) ) {
        message = "The NewScenario cannot contain spaces.";
			warning += "\n" + message;
 			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
					"The NewScenario must not contain spaces.",
					"Remove spaces from the scenario." ) );
	}
	
	if ( (AutoAdjust != null) && !AutoAdjust.equals("") &&
			!AutoAdjust.equalsIgnoreCase("True") &&
			!AutoAdjust.equalsIgnoreCase("False") ) {
                message = "The AutoAdjust value (" + AutoAdjust + ") must be True or False.";
				warning += "\n" + message;
				status.addToLog ( CommandPhaseType.INITIALIZATION,
						new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Change to True or False." ) );
	}
	
	if ( (CheckData != null) && !CheckData.equals("") &&
			!CheckData.equalsIgnoreCase("True") &&
			!CheckData.equalsIgnoreCase("False") ) {
        message = "The CheckData value (" + CheckData + ") must be True or False.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
						new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Change to True or False." ) );
	}
    
    // Check for invalid parameters...
	List<String> validList = new ArrayList<>(8);
    validList.add ( "InputFile" );
    validList.add ( "TSID" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "AutoAdjust" );
    validList.add ( "NewScenario" );
    validList.add ( "Alias" );
    validList.add ( "CheckData" );
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
Check the irrigation practice time series for integrity, mainly to make sure that
the acreage terms add up.  This method may be made public at some point if can
be used by StateDMI commands that read or write the file.
@param tslist List of StateCU_IrrigationPracticeTS to check.
@param status CommandStatus to append check warnings.
*/
private void checkIrrigationPracticeTS ( List<StateCU_IrrigationPracticeTS> tslist, CommandStatus status )
{	
	int size = 0;
	if ( tslist != null ) {
		size = tslist.size();
	}
	StateCU_IrrigationPracticeTS ipy_ts = null;
	YearTS AcresSWFlood_yts = null;
	YearTS AcresSWSprinkler_yts = null;
	YearTS AcresGWFlood_yts = null;
	YearTS AcresGWSprinkler_yts = null;
	YearTS AcresTotal_yts = null;
	double AcresSWFlood_double;
	double AcresSWSprinkler_double;
	double AcresGWFlood_double;
	double AcresGWSprinkler_double;
	double AcresTotal_double;
	double tolerance = 1.0;	// Check to nearest integer since acreage is written as whole number
	int precision = 0;
	// TODO SAM 2007-09-09 Need some utilities to help with checks
	// Need to intelligently compute the precision from the tolerance
	DateTime date_end;		// End of period for looping.
	String id;
	double calculated_total;	// Calculated total of parts.
	for ( int i = 0; i < size; i++ ) {
		ipy_ts = tslist.get(i);
		id = ipy_ts.getID();
		AcresSWFlood_yts = ipy_ts.getAcswflTS();
		AcresSWSprinkler_yts = ipy_ts.getAcswsprTS();
		AcresGWFlood_yts = ipy_ts.getAcgwflTS();
		AcresGWSprinkler_yts = ipy_ts.getAcgwsprTS();
		AcresTotal_yts = ipy_ts.getTacreTS();
		// Loop through the period and check the acreage.
		date_end = ipy_ts.getDate2();
		for ( DateTime date = new DateTime(ipy_ts.getDate1());
			date.lessThanOrEqualTo(date_end); date.addYear(1) ) {
			AcresSWFlood_double = AcresSWFlood_yts.getDataValue(date);
			AcresSWSprinkler_double = AcresSWSprinkler_yts.getDataValue(date);
			AcresGWFlood_double = AcresGWFlood_yts.getDataValue(date);
			AcresGWSprinkler_double = AcresGWSprinkler_yts.getDataValue(date);
			AcresTotal_double = AcresTotal_yts.getDataValue(date);
			calculated_total = AcresSWFlood_double + AcresSWSprinkler_double +
				AcresGWFlood_double + AcresGWSprinkler_double;
			if ( Math.abs(calculated_total - AcresTotal_double) > tolerance ) {
				status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
								"Location \"" + id + "\" acreage terms do not add to total in " +
								date.getYear() + ".  Total of parts = " +
								StringUtil.formatString(calculated_total,"%."+precision+"f") +
								" Total from file = " + StringUtil.formatString(AcresTotal_double,"%."+precision+"f"),
								"Verify data processing." ) );
			}
		}
	}
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new ReadStateCU_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	String message;

	if ( StringUtil.startsWithIgnoreCase(command_string,"TS") ) {
		// Syntax is TS Alias = readStateCU()
		_use_alias = true;
		message = "TS Alias = readStateCU() is not yet supported.";
		throw new InvalidCommandSyntaxException ( message );
	}
	else {	// Syntax is readStateCU()
		_use_alias = false;
		super.parseCommand ( command_string );
	}
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
    List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}
	
/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "readStateCU_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;	// Log level for non-user warnings

	PropList parameters = getCommandParameters();
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    
	String InputFile = parameters.getValue ( "InputFile" );
	String InputStart = parameters.getValue ( "InputStart" );
	DateTime InputStart_DateTime = null;
	String InputEnd = parameters.getValue ( "InputEnd" );
	DateTime InputEnd_DateTime = null;
	String TSID = parameters.getValue ( "TSID" );
	String NewScenario = parameters.getValue ( "NewScenario" );
    String Alias = parameters.getValue("Alias");
	String AutoAdjust = parameters.getValue ( "AutoAdjust" );
	if ( (AutoAdjust == null) || AutoAdjust.equals("") ) {
	    AutoAdjust = _True;
	}
	String CheckData = parameters.getValue ( "CheckData" );
	
	// TODO SAM 2007-11-29 need to check prop
	boolean IncludeLocationTotal_boolean = true;
	boolean IncludeDataSetTotal_boolean = true;
    
    CommandProcessor processor = getCommandProcessor();
    
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		try {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", InputStart );
		CommandProcessorRequestResultsBean bean = null;
		try { bean =
			processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting InputStart DateTime(DateTime=" +
			InputStart + ") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for InputStart DateTime(DateTime=" +
			InputStart + ") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the specified date/time is valid." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	InputStart_DateTime = (DateTime)prop_contents;
		}
	}
	catch ( Exception e ) {
		message = "InputStart \"" + InputStart + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time for the input start, " +
                        "or InputStart for the global input start." ) );
		throw new InvalidCommandParameterException ( message );
	}
	}
	else {	// Get from the processor...
		try {	Object o = processor.getPropContents ( "InputStart" );
				if ( o != null ) {
					InputStart_DateTime = (DateTime)o;
				}
		}
		catch ( Exception e ) {
            message = "Error requesting the global InputStart from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
		}
	}
	
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
			try {
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", InputEnd );
			CommandProcessorRequestResultsBean bean = null;
			try { bean =
				processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting InputEnd DateTime(DateTime=" +
				InputEnd + ") from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
				throw new InvalidCommandParameterException ( message );
			}

			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for InputEnd DateTime(DateTime=" +
				InputEnd +	") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the end date/time is valid." ) );
				throw new InvalidCommandParameterException ( message );
			}
			else {	InputEnd_DateTime = (DateTime)prop_contents;
			}
		}
		catch ( Exception e ) {
			message = "InputEnd \"" + InputEnd + "\" is invalid.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time for the input end, " +
                            "or InputEnd for the global input start." ) );
			throw new InvalidCommandParameterException ( message );
		}
		}
		else {
			// Get from the processor...
			try {	Object o = processor.getPropContents ( "InputEnd" );
					if ( o != null ) {
						InputEnd_DateTime = (DateTime)o;
					}
			}
			catch ( Exception e ) {
                message = "Error requesting the global InputEnd from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
			}
	}

	// Default is to check the data (True).  False can be used if the user
	// knows that the data are incomplete.
	boolean CheckData_boolean = true;
	if ( (CheckData != null) && (CheckData.length() > 0) ) {
		if ( CheckData.equalsIgnoreCase("False")) {
			CheckData_boolean = false;
		}
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Now try to read...

    String InputFile_full = InputFile;
	try {
        // FIXME SAM 2007-12-08 Need to make sure each read is optimized to NOT read all data
        // if in discovery mode.
        boolean read_data = true;
        if ( command_phase == CommandPhaseType.DISCOVERY ){
            read_data = false;
        }
       	// Can only check for file existence and if ${Property} is not used
        // - this limits how time series are listed in discovery
        // - TODO smalers 2020-06-06 figure out how to run discovery with properties
		List<TS> tslist = null;
		String file_type = "unknown file type";
      	InputFile_full = IOUtil.verifyPathForOS(
           	IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
               	TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)) );
        if ( InputFile_full.indexOf("${") < 0 ) {
        	Message.printStatus ( 2, routine, "Reading StateCU file \"" + InputFile_full + "\"" );
			if ( StateCU_CropPatternTS.isCropPatternTSFile ( InputFile_full ) ) {
				file_type = "crop pattern";
				PropList read_props = null;
				if ( AutoAdjust != null ) {
					read_props = new PropList ( "CDS" );
					read_props.set ( "AutoAdjust", AutoAdjust );
				}
				tslist = StateCU_CropPatternTS.toTSList(
					StateCU_CropPatternTS.readStateCUFile (
					InputFile_full, InputStart_DateTime, InputEnd_DateTime,
					read_props ),
					IncludeLocationTotal_boolean,
					IncludeDataSetTotal_boolean,
					null,
					null );
			}
			else if ( StateCU_IrrigationPracticeTS.isIrrigationPracticeTSFile(InputFile_full ) ) {
				file_type = "irrigation practice";
				// Clear the status...
				status.clearLog(command_phase);
				try {
					List<StateCU_IrrigationPracticeTS> ipylist = StateCU_IrrigationPracticeTS.readStateCUFile (
						InputFile_full, InputStart_DateTime, InputEnd_DateTime );
						// Get the individual time series for use by TSTool.
						tslist = StateCU_IrrigationPracticeTS.toTSList(
								ipylist, IncludeDataSetTotal_boolean, null, null );
						if ( CheckData_boolean ) {
							// Check the time series for integrity (acreage adds, etc.)...
							checkIrrigationPracticeTS ( ipylist, status );
						}
				}
				catch ( Exception e ) {
					message = "Unexpected error reading irrigation practice file \"" + InputFile_full + "\".";
					status.addToLog ( command_phase,
						new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check the log file to troubleshoot." ) );
					Message.printWarning ( warning_level, 
						MessageUtil.formatMessageTag(command_tag,
						++warning_count), routine, message );
					Message.printWarning(2,routine,e );
				}
			}
			else if ( StateCU_TS.isReportFile ( InputFile ) ) {
				file_type = "model results";
				tslist = StateCU_TS.readTimeSeriesList (
					TSID, InputFile, InputStart_DateTime, InputEnd_DateTime,
					(String)null, read_data );
			}
			else {
            	message = "File \"" + InputFile + "\" is not a recognized StateCU file type.  Not reading.";
				Message.printWarning ( warning_level, 
						MessageUtil.formatMessageTag(command_tag,
						++warning_count), routine, message );
            	status.addToLog ( command_phase,
                    	new CommandLogRecord(CommandStatusType.FAILURE,
                    	"Unexpected error reading irrigation practice file \"" +
                    	InputFile_full + "\"", "Verify the that the file is a StateCU file as specified." ) );
				throw new CommandException ( message );
			}
        }
		
		// Print out how many time series were actually read...

		int size = 0;
        if ( tslist != null ) {
            size = tslist.size();
            
            List<String> aliasList = new ArrayList<>();
            TS ts = null;
            for (int i = 0; i < size; i++) {
                ts = tslist.get(i);
                if ( (Alias != null) && (Alias.length() > 0) ) {
                    // Set the alias to the desired string - this is impacted by the Location parameter
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, Alias, status, command_phase);
                    ts.setAlias ( alias );
                    // Search for duplicate alias and warn
                    int aliasListSize = aliasList.size();
                    for ( int iAlias = 0; iAlias < aliasListSize; iAlias++ ) {
                        if ( aliasList.get(iAlias).equalsIgnoreCase(alias)) {
                            message = "Alias \"" + alias +
                            "\" was also used for another time series read from the StateMod file.";
                            Message.printWarning(log_level,
                                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                            status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Consider using a more specific alias to uniquely identify the time series." ) );
                        }
                    }
                    // Now add the new list to the alias...
                    aliasList.add ( alias );
                }
            }
        }

		Message.printStatus ( 2, routine, "Read " + size + " StateCU " + file_type + " time series." );
		
		// If the scenario was specified set it in all the time series...
		
		if ( (NewScenario != null) && (NewScenario.length() > 0) ) {
			TS ts = null;
			for ( int i = 0; i < size; i++ ) {
				ts = (TS)tslist.get(i);
				ts.getIdentifier().setScenario(NewScenario);
			}
		}

		// Now add the time series to the end of the normal list...

        if ( command_phase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing StateCU time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
                        status.addToLog ( command_phase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding StateCU time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
                        status.addToLog ( command_phase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
            }
        }
        else if ( command_phase == CommandPhaseType.DISCOVERY ) {
            setDiscoveryTSList ( tslist );
        }

		// Free resources from StateMod list...
		tslist = null;
	}
    catch ( FileNotFoundException e ) {
        message = "StateCU file \"" + InputFile_full + "\" is not found or accessible.";
        Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
                status.addToLog(command_phase,
                    new CommandLogRecord( CommandStatusType.FAILURE, message,
                        "Verify that the file exists and is readable."));
        throw new CommandException ( message );
    }
	catch ( Exception e ) {
		Message.printWarning ( log_level, routine, e );
		message = "Unexpected error reading time series from StateCU file \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "See log file for details." ) );
		throw new CommandException ( message );
	}
    
    status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"InputFile",
		"InputStart",
		"InputEnd",
		"TSID",
		"NewScenario",
		"Alias",
		"AutoAdjust",
		"CheckData"
	};
	return this.toString(parameters, parameterOrder);
}

/**
Indicate whether the alias version of the command is being used.  This method
should be called only after the parseCommandParameters() method is called.
*/
protected boolean useAlias ()
{	return _use_alias;
}

}