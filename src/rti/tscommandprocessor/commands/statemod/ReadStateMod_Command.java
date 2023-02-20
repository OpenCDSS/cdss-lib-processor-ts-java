// ReadStateMod_Command - handle the ReadStateMod() and TS Alias = ReadStateMod() commands

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
// readStateMod_Command - handle the readStateMod() and
//				TS Alias = readStateMod() commands
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-09-02	Steven A. Malers, RTi	Initial version.  Copy and modify
//					writeStateMod().
// 2007-05-16	SAM, RTi                Add ability to read well rights file.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.statemod;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
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
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

import DWR.StateMod.StateMod_DiversionRight;
import DWR.StateMod.StateMod_InstreamFlowRight;
import DWR.StateMod.StateMod_ReservoirRight;
import DWR.StateMod.StateMod_TS;
import DWR.StateMod.StateMod_Util;
import DWR.StateMod.StateMod_WellRight;

/**
This class initializes, checks, and runs the ReadStateMod() command.
*/
public class ReadStateMod_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
/**
Flags used when setting the interval.
*/
protected String _Day = "Day";
protected String _Month = "Month";
protected String _Year = "Year";
//protected String _Irregular = "Irregular";

/**
Flags used when setting the spatial aggregation.
*/
protected String _Location = "Location";
protected String _Parcel = "Parcel";
protected String _None = "None";

/**
Indicates whether the TS Alias version of the command is being used.
*/
protected boolean _use_alias = false;

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public ReadStateMod_Command ()
{	super();
	setCommandName ( "ReadStateMod" );
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
{	String routine = getCommandName() + ".checkCommandParameters";
    String InputFile = parameters.getValue ( "InputFile" );
	String InputStart = parameters.getValue ( "InputStart" );
	String InputEnd = parameters.getValue ( "InputEnd" );
	String Interval = parameters.getValue ( "Interval" );
	String SpatialAggregation = parameters.getValue ( "SpatialAggregation" );
	String ParcelYear = parameters.getValue ( "ParcelYear" );
	String warning = "";
    String message;

	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
	if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
	}
	else if ( InputFile.indexOf("${") < 0 ) { // No properties used so can check path
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
                                message, "Report the problem to software support." ) );
			}
	
		try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
                File f = new File ( adjusted_path );
                if ( !f.exists() ) {
                    message = "The input file does not exist:  \"" + adjusted_path + "\".";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.WARNING,
                                    message, "Verify that the input file exists - may be OK if created at run time." ) );
                }
		}
		catch ( Exception e ) {
            message = "The input file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
		}
	}

	if ( (InputStart != null) && !InputStart.equals("") &&
		!InputStart.equalsIgnoreCase("InputStart") &&
		!InputStart.equalsIgnoreCase("InputEnd") && (InputStart.indexOf("${") < 0) ) {
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
	if ( (InputEnd != null) && !InputEnd.equals("") &&
		!InputEnd.equalsIgnoreCase("InputStart") &&
		!InputEnd.equalsIgnoreCase("InputEnd") && (InputEnd.indexOf("${") < 0) ) {
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
	if ( (Interval != null) && !Interval.equals("") &&
		!Interval.equalsIgnoreCase(_Day) &&
		!Interval.equalsIgnoreCase(_Month) &&
		!Interval.equalsIgnoreCase(_Year) ) {
	        message = "The interval for StateMod data (" + Interval + ") is invalid.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the interval as Day, Month, or Year, consistent with the data file." ) );
	}
	
	if ( (SpatialAggregation != null)&&
			!SpatialAggregation.equals("") &&
			!SpatialAggregation.equalsIgnoreCase(_Location) &&
			!SpatialAggregation.equalsIgnoreCase(_Parcel) &&
			!SpatialAggregation.equalsIgnoreCase(_None) ) {
            message = "The spatial aggregation (" + SpatialAggregation + ") is invalid.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the spatial aggregation as " +
                                _Location + ", " + _Parcel + ", or " + _None + "." ) );
	}
	
	if ( (ParcelYear != null) && (ParcelYear.length() > 0) ) {
		if ( !StringUtil.isInteger(ParcelYear)) {
            message = "\nThe parcel year (" + ParcelYear + ") must be an integer.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the parcel year as an integer YYYY." ) );
				
		}
	}
    
    // Check for invalid parameters...
	List<String> validList = new ArrayList<>(7);
    validList.add ( "InputFile" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "Alias" );
    validList.add ( "Interval" );
    validList.add ( "SpatialAggregation" );
    validList.add ( "ParcelYear" );
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
{	// The command will be modified if changed...
	return (new ReadStateMod_JDialog ( parent, this )).ok();
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
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
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
{	String routine = "readStateMod_Command.parseCommand", message;
	int warning_level = 2;
    CommandStatus status = getCommandStatus();
	if ( StringUtil.startsWithIgnoreCase(command_string,"TS") ) {
		// Syntax is TS Alias = readStateMod()
		_use_alias = true;
		message = "TS Alias = ReadStateMod() is not yet supported.";
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Use the TSID as a command to read a single time series." ) );
		throw new InvalidCommandSyntaxException ( message );
	}
	else {	// Syntax is readStateMod()
		_use_alias = false;
        if ( (command_string.indexOf("=") > 0) || command_string.endsWith("()") ) {
			// New syntax...
			super.parseCommand ( command_string );
		}
		else {	// Parse the old command...
			List<String> tokens = StringUtil.breakStringList (command_string,
				"(,)", StringUtil.DELIM_ALLOW_STRINGS );
			if ( tokens.size() != 2 ) {
				message = "Invalid syntax for command.  Expecting 1 parameter.";
				Message.printWarning ( warning_level, routine, message);
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Use the command editor to correct the command." ) );
				throw new InvalidCommandSyntaxException ( message );
			}
			String InputFile = ((String)tokens.get(1)).trim();
			PropList parameters = new PropList ( getCommandName() );
			parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
			if ( InputFile.length() > 0 ) {
				parameters.set ( "InputFile", InputFile );
			}
			parameters.setHowSet ( Prop.SET_UNKNOWN );
			setCommandParameters ( parameters );
		}
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
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run method internal to this class, to handle running in discovery and run mode.
@param command_number Command number 1+ from processor.
@param commandPhase Command phase being executed (RUN or DISCOVERY).
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    String routine = "ReadStateMod_Command.runCommandInternal", message;
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    int log_level = 3;  // Log level for non-user warnings
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    CommandProcessor processor = getCommandProcessor();

    PropList parameters = getCommandParameters();
    String InputFile = parameters.getValue ( "InputFile" );
    String InputStart = parameters.getValue ( "InputStart" );
    DateTime InputStart_DateTime = null;
    String InputEnd = parameters.getValue ( "InputEnd" );
    String Alias = parameters.getValue("Alias");
    String Interval = parameters.getValue ( "Interval" );
    if ( (Interval == null) || Interval.equals("") ) {
        Interval = "Year"; // Default
    }
    String SpatialAggregation = parameters.getValue ( "SpatialAggregation" );
    String ParcelYear = parameters.getValue ( "ParcelYear" );
    DateTime InputEnd_DateTime = null;

    if ( (InputStart != null) && (InputStart.length() != 0) ) {
	    if ( InputStart.indexOf("${") >= 0) {
	    	InputStart = TSCommandProcessorUtil.expandParameterValue(
	            this.getCommandProcessor(),this,InputStart);
	    }
        try {
        PropList request_params = new PropList ( "" );
        request_params.set ( "DateTime", InputStart );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting InputStart DateTime(DateTime=" + InputStart + ") from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
            throw new InvalidCommandParameterException ( message );
        }

        PropList bean_PropList = bean.getResultsPropList();
        Object prop_contents = bean_PropList.getContents ( "DateTime" );
        if ( prop_contents == null ) {
            message = "Null value for InputStart DateTime(DateTime=" + InputStart + ") returned from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the specified date/time is valid." ) );
            throw new InvalidCommandParameterException ( message );
        }
        else {  InputStart_DateTime = (DateTime)prop_contents;
        }
    }
    catch ( Exception e ) {
        message = "InputStart \"" + InputStart + "\" is invalid.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time for the input start, " +
                        "or InputStart for the global input start." ) );
        throw new InvalidCommandParameterException ( message );
    }
    }
    else {  // Get the global input start from the processor...
        try {   Object o = processor.getPropContents ( "InputStart" );
                if ( o != null ) {
                    InputStart_DateTime = (DateTime)o;
                }
        }
        catch ( Exception e ) {
            message = "Error requesting the global InputStart from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
            throw new InvalidCommandParameterException ( message );
        }
    }
    
    if ( (InputEnd != null) && (InputEnd.length() != 0) ) {
	    if ( InputEnd.indexOf("${") >= 0) {
	    	InputEnd = TSCommandProcessorUtil.expandParameterValue(
	            this.getCommandProcessor(),this,InputEnd);
	    }
        try {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", InputEnd );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting InputEnd DateTime(DateTime=" +
                InputEnd + ") from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }

            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for InputEnd DateTime(DateTime=" +
                InputEnd +  ") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the end date/time is valid." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {  InputEnd_DateTime = (DateTime)prop_contents;
            }
        }
        catch ( Exception e ) {
            message = "InputEnd \"" + InputEnd + "\" is invalid.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time for the input end, " +
                            "or InputEnd for the global input start." ) );
            throw new InvalidCommandParameterException ( message );
        }
        }
        else {  // Get from the processor...
            try {   Object o = processor.getPropContents ( "InputEnd" );
                    if ( o != null ) {
                        InputEnd_DateTime = (DateTime)o;
                    }
            }
            catch ( Exception e ) {
                message = "Error requesting the global InputEnd from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
            }
    }

    int SpatialAggregation_int = 0;  // Default
    if ( SpatialAggregation == null ) {
        SpatialAggregation = _Location;
        SpatialAggregation_int = 0;
    }
    else if ( SpatialAggregation.equalsIgnoreCase(_Parcel) ) {
        SpatialAggregation_int = 1;
    }
    else if ( SpatialAggregation.equalsIgnoreCase(_None) ) {
        SpatialAggregation_int = 2;
    }
    int ParcelYear_int = -1;    // Default - consider all
    if ( ParcelYear != null ) {
        ParcelYear_int = StringUtil.atoi ( ParcelYear );
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
        boolean read_data = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ){
            read_data = false;
        }
        InputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)) );
        Message.printStatus ( 2, routine, "Reading StateMod file \"" + InputFile_full + "\"" );
    
        List<TS> tslist = null;
       	// Can only check for file existence and if ${Property} is not used
        // - TODO smalers 2020-06-06 figure out how to run discovery with properties
        // - this limits how time series are listed in discovery
        if ( InputFile_full.indexOf("${") < 0 ) {
       	    if ( !IOUtil.fileReadable(InputFile_full) || !IOUtil.fileExists(InputFile_full) ) {
       	    	message = "StateMod file \"" + InputFile_full + "\" is not found or accessible.";
           		Message.printWarning ( warning_level,
                   		MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
                   		status.addToLog(commandPhase,
                       		new CommandLogRecord( CommandStatusType.FAILURE, message,
                           		"Verify that the file exists and is readable."));
           		throw new CommandException ( message );
        	}
        	if ( StateMod_DiversionRight.isDiversionRightFile(InputFile_full)) {
            	TimeInterval Interval_TimeInterval = TimeInterval.parseInterval( Interval );
            	// Read the diversion rights file and convert to time series
            	// (default is to sum time series at a location).
            	List<StateMod_DiversionRight> ddrList = StateMod_DiversionRight.readStateModFile ( InputFile_full );
            	// Convert the rights to time series (one per location)...
            	tslist = StateMod_Util.createWaterRightTimeSeriesList (
                    	ddrList, // raw water rights
                    	Interval_TimeInterval.getBase(), // time series interval
                    	SpatialAggregation_int, // Where to summarize time series
                    	ParcelYear_int, // Parcel year for filter
                    	true, // Create a data set total
                    	InputStart_DateTime, // time series start
                    	InputEnd_DateTime, // time series end
                    	999999.00000, // No special free water rights
                    	null, // ...
                    	null, // ...
                    	read_data ); // do read data
        	}
        	else if ( StateMod_InstreamFlowRight.isInstreamFlowRightFile(InputFile_full)) {
            	TimeInterval Interval_TimeInterval = TimeInterval.parseInterval( Interval );
            	// Read the instream flow rights file and convert to time series
            	// (default is to sum time series at a location).
            	List<StateMod_InstreamFlowRight> ifrList = StateMod_InstreamFlowRight.readStateModFile ( InputFile_full );
            	// Convert the rights to time series (one per location)...
            	tslist = StateMod_Util.createWaterRightTimeSeriesList (
                    	ifrList, // raw water rights
                    	Interval_TimeInterval.getBase(), // time series interval
                    	SpatialAggregation_int, // Where to summarize time series
                    	ParcelYear_int, // Parcel year for filter
                    	true, // Create a data set total
                    	InputStart_DateTime, // time series start
                    	InputEnd_DateTime, // time series end
                    	999999.00000, // No special free water rights
                    	null, // ...
                    	null, // ...
                    read_data ); // do read data
        	}
        	else if ( StateMod_ReservoirRight.isReservoirRightFile(InputFile_full)) {
            	TimeInterval Interval_TimeInterval = TimeInterval.parseInterval( Interval );
            	// Read the reservoir rights file and convert to time series
            	// (default is to sum time series at a location).
            	List<StateMod_ReservoirRight> rer_Vector = StateMod_ReservoirRight.readStateModFile ( InputFile_full );
            	// Convert the rights to time series (one per location)...
            	tslist = StateMod_Util.createWaterRightTimeSeriesList (
                    	rer_Vector, // raw water rights
                    	Interval_TimeInterval.getBase(), // time series interval
                    	SpatialAggregation_int, // Where to summarize time series
                    	ParcelYear_int, // Parcel year for filter
                    	true, // Create a data set total
                    	InputStart_DateTime, // time series start
                    	InputEnd_DateTime, // time series end
                    	999999.00000, // No special free water rights
                    	null, // ...
                    	null, // ...
                    	read_data ); // do read data
        	}
        	else if ( StateMod_WellRight.isWellRightFile(InputFile_full)) {
            	TimeInterval Interval_TimeInterval = TimeInterval.parseInterval( Interval );
            	// Read the well rights file and convert to time series
            	// (default is to sum time series at a location).
            	List<StateMod_WellRight> wer_Vector = StateMod_WellRight.readStateModFile ( InputFile_full );
            	// Convert the rights to time series (one per location)...
            	tslist = StateMod_Util.createWaterRightTimeSeriesList (
                    	wer_Vector, // raw water rights
                    	Interval_TimeInterval.getBase(), // time series interval
                    	SpatialAggregation_int, // Where to summarize time series
                    	ParcelYear_int, // Parcel year for filter
                    	true, // Create a data set total
                    	InputStart_DateTime, // time series start
                    	InputEnd_DateTime, // time series end
                    	999999.00000, // No special free water rights
                    	null, // ...
                    	null, // ...
                    	read_data ); // do read data
        	}
        	else {
            	// Read a time series file that follows typical column-oriented formatting
            	//   *.stm monthly (including average monthly) and daily format
            	//   *.xop monthly format
            	int interval = StateMod_TS.getFileDataInterval(InputFile_full);

            	if ( (interval == TimeInterval.MONTH) || (interval == TimeInterval.DAY) ) {
                	tslist = StateMod_TS.readTimeSeriesList (
                	InputFile_full, InputStart_DateTime,
                	InputEnd_DateTime,
                	null,   // Requested units
                	read_data ); // Read all data
            	}
            	else {
                	message = "StateMod file \"" + InputFile_full + "\" is not a recognized daily or monthly file (bad file format?).";
                	Message.printWarning ( warning_level, 
                        	MessageUtil.formatMessageTag(command_tag,
                        	++warning_count), routine, message );
                	status.addToLog ( commandPhase,
                        	new CommandLogRecord(CommandStatusType.FAILURE,
                                	message, "Verify that the file being read is a StateMod file." ) );
                	throw new CommandException ( message );
            	}
        	}
        }
        
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
                        processor, ts, Alias, status, commandPhase);
                    ts.setAlias ( alias );
                    // Search for duplicate alias and warn
                    int aliasListSize = aliasList.size();
                    for ( int iAlias = 0; iAlias < aliasListSize; iAlias++ ) {
                        if ( aliasList.get(iAlias).equalsIgnoreCase(alias)) {
                            message = "Alias \"" + alias +
                            "\" was also used for another time series read from the StateMod file.";
                            Message.printWarning(log_level,
                                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Consider using a more specific alias to uniquely identify the time series." ) );
                        }
                    }
                    // Now add the new list to the alias...
                    aliasList.add ( alias );
                }
            }
        }
        Message.printStatus ( 2, routine, "Read " + size + " StateMod time series." );

        // Now add the time series to the end of the normal list...
        if ( commandPhase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing StateMod time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
                        status.addToLog ( commandPhase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding StateMod time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
                        status.addToLog ( commandPhase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
            }
        }
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            setDiscoveryTSList ( tslist );
        }

        // Free resources from StateMod list...
        tslist = null;
    }
    catch ( FileNotFoundException e ) {
        message = "StateMod file \"" + InputFile_full + "\" is not found or accessible.";
        Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
                Message.printWarning ( 3, routine, e );
                status.addToLog(commandPhase,
                    new CommandLogRecord( CommandStatusType.FAILURE, message,
                        "Verify that the file exists and is readable."));
        throw new CommandException ( message );
    }
    catch ( Exception e ) {
        Message.printWarning ( log_level, routine, e );
        message = "Unexpected error reading time series from StateMod file \"" + InputFile_full + "\" (" + e + ").";
        Message.printWarning ( warning_level, 
        MessageUtil.formatMessageTag(command_tag, ++warning_count),
        routine, message );
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "See log file for details." ) );
        throw new CommandException ( message );
    }
    
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discovery_TS_Vector ) {
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
		"Alias",
		"Interval",
		"SpatialAggregation",
		"ParcelYear"
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