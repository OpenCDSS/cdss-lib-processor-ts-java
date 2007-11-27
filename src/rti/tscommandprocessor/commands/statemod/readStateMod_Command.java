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
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

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
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
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
<p>
This class initializes, checks, and runs the readStateMod() command.
</p>
<p>The CommandProcessor must return the following properties:
TSResultsList, WorkingDir.
</p>
*/
public class readStateMod_Command extends AbstractCommand implements Command
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
Constructor.
*/
public readStateMod_Command ()
{	super();
	setCommandName ( "ReadStateMod" );
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
	else {	String working_dir = null;
        try { Object o = processor.getPropContents ( "WorkingDir" );
				// Working directory is available so use it...
				if ( o != null ) {
					working_dir = (String)o;
				}
			}
			catch ( Exception e ) {
                message = "Error requesting WorkingDir from processor.";
                warning += "\n" + message;
				Message.printWarning(3, routine, message );
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify an existing input file." ) );
			}
	
		try {
            //String adjusted_path = 
                IOUtil.adjustPath (working_dir, InputFile);
		}
		catch ( Exception e ) {
            message = "The output file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
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
                            message, "Specify a valid date/time or InputStart to use the global input start." ) );
		}
	}
	if (	(InputEnd != null) && !InputEnd.equals("") &&
		!InputEnd.equalsIgnoreCase("InputStart") &&
		!InputEnd.equalsIgnoreCase("InputEnd") ) {
		try {	DateTime.parse( InputEnd );
		}
		catch ( Exception e ) {
            message = "The input end date/time \"" + InputEnd + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or InputEnd to use the global input end." ) );

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
    Vector valid_Vector = new Vector();
    valid_Vector.add ( "InputFile" );
    valid_Vector.add ( "InputStart" );
    valid_Vector.add ( "InputEnd" );
    valid_Vector.add ( "Interval" );
    valid_Vector.add ( "SpatialAggregation" );
    valid_Vector.add ( "ParcelYear" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

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
	return (new readStateMod_JDialog ( parent, this )).ok();
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
			Vector tokens = StringUtil.breakStringList (command_string,
				"(,)", StringUtil.DELIM_ALLOW_STRINGS );
			if ( tokens.size() != 2 ) {
				message = "Invalid syntax for command.  Expecting 1 parameter.";
				Message.printWarning ( warning_level, routine, message);
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Use the command editor to correct the command." ) );
				throw new InvalidCommandSyntaxException ( message );
			}
			String InputFile = ((String)tokens.elementAt(1)).trim();
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ReadStateMod_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;	// Log level for non-user warnings
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    CommandProcessor processor = getCommandProcessor();

	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue ( "InputFile" );
	String InputStart = parameters.getValue ( "InputStart" );
	DateTime InputStart_DateTime = null;
	String InputEnd = parameters.getValue ( "InputEnd" );
	String Interval = parameters.getValue ( "Interval" );
	String SpatialAggregation = parameters.getValue ( "SpatialAggregation" );
	String ParcelYear = parameters.getValue ( "ParcelYear" );
	DateTime InputEnd_DateTime = null;

	if ( InputStart != null ) {
		try {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", InputStart );
		CommandProcessorRequestResultsBean bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting InputStart DateTime(DateTime=" + InputStart + "\" from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for InputStart DateTime(DateTime=" +
			InputStart +	"\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report problem to software support." ) );
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
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time for the input start, " +
                        "or InputStart for the global input start." ) );
		throw new InvalidCommandParameterException ( message );
	}
	}
	else {	// Get the global input start from the processor...
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
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report problem to software support." ) );
            throw new InvalidCommandParameterException ( message );
		}
	}
	
		if ( InputEnd != null ) {
			try {
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", InputEnd );
			CommandProcessorRequestResultsBean bean = null;
			try {
                bean = processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting InputEnd DateTime(DateTime=" +
				InputEnd + "\" from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
				throw new InvalidCommandParameterException ( message );
			}

			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for InputEnd DateTime(DateTime=" +
				InputEnd +	"\") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
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
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time for the input end, " +
                            "or InputEnd for the global input start." ) );
			throw new InvalidCommandParameterException ( message );
		}
		}
		else {	// Get from the processor...
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
                status.addToLog ( CommandPhaseType.RUN,
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
	int ParcelYear_int = -1;	// Default - consider all
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

	try {
        String InputFile_full = IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),InputFile);
        Message.printStatus ( 2, routine, "Reading StateMod file \"" + InputFile_full + "\"" );
	
		Vector tslist = null;
		if ( StateMod_DiversionRight.isDiversionRightFile(InputFile_full)) {
			if ( (Interval == null) || Interval.equals("") ) {
				Interval = "Year";
			}
			TimeInterval Interval_TimeInterval = TimeInterval.parseInterval( Interval );
			// Read the diversion rights file and convert to time series
			// (default is to sum time series at a location).
			Vector ddr_Vector = StateMod_WellRight.readStateModFile ( InputFile_full );
			// Convert the rights to time series (one per location)...
			tslist = StateMod_Util.createWaterRightTimeSeriesList (
					ddr_Vector,        // raw water rights
					Interval_TimeInterval.getBase(),  // time series interval
					SpatialAggregation_int,          // Where to summarize time series
					ParcelYear_int,			// Parcel year for filter
					true,				// Create a data set total
					null,              // time series start
					null,              // time series end
					999999.00000,	// No special free water rights
					null,			// ...
					null,			// ...
					true );            // do read data
		}
		else if ( StateMod_InstreamFlowRight.isInstreamFlowRightFile(InputFile_full)) {
			if ( (Interval == null) || Interval.equals("") ) {
				Interval = "Year";
			}
			TimeInterval Interval_TimeInterval = TimeInterval.parseInterval( Interval );
			// Read the instream flow rights file and convert to time series
			// (default is to sum time series at a location).
			Vector ifr_Vector = StateMod_WellRight.readStateModFile ( InputFile_full );
			// Convert the rights to time series (one per location)...
			tslist = StateMod_Util.createWaterRightTimeSeriesList (
					ifr_Vector,        // raw water rights
					Interval_TimeInterval.getBase(),  // time series interval
					SpatialAggregation_int,          // Where to summarize time series
					ParcelYear_int,			// Parcel year for filter
					true,				// Create a data set total
					null,              // time series start
					null,              // time series end
					999999.00000,	// No special free water rights
					null,			// ...
					null,			// ...
					true );            // do read data
		}
		else if ( StateMod_ReservoirRight.isReservoirRightFile(InputFile_full)) {
			if ( (Interval == null) || Interval.equals("") ) {
				Interval = "Year";
			}
			TimeInterval Interval_TimeInterval = TimeInterval.parseInterval( Interval );
			// Read the reservoir rights file and convert to time series
			// (default is to sum time series at a location).
			Vector rer_Vector = StateMod_WellRight.readStateModFile ( InputFile_full );
			// Convert the rights to time series (one per location)...
			tslist = StateMod_Util.createWaterRightTimeSeriesList (
					rer_Vector,        // raw water rights
					Interval_TimeInterval.getBase(),  // time series interval
					SpatialAggregation_int,          // Where to summarize time series
					ParcelYear_int,			// Parcel year for filter
					true,				// Create a data set total
					null,              // time series start
					null,              // time series end
					999999.00000,	// No special free water rights
					null,			// ...
					null,			// ...
					true );            // do read data
		}
		else if ( StateMod_WellRight.isWellRightFile(InputFile_full)) {
			if ( (Interval == null) || Interval.equals("") ) {
				Interval = "Year";
			}
			TimeInterval Interval_TimeInterval = TimeInterval.parseInterval( Interval );
			// Read the well rights file and convert to time series
			// (default is to sum time series at a location).
			Vector wer_Vector = StateMod_WellRight.readStateModFile ( InputFile_full );
			// Convert the rights to time series (one per location)...
			tslist = StateMod_Util.createWaterRightTimeSeriesList (
					wer_Vector,        // raw water rights
					Interval_TimeInterval.getBase(),  // time series interval
					SpatialAggregation_int,          // Where to summarize time series
					ParcelYear_int,			// Parcel year for filter
					true,				// Create a data set total
					null,              // time series start
					null,              // time series end
					999999.00000,	// No special free water rights
					null,			// ...
					null,			// ...
					true );            // do read data
		}
		else {	// Read a traditional time series file
			int interval = StateMod_TS.getFileDataInterval(InputFile_full);

			if ( (interval == TimeInterval.MONTH) || (interval == TimeInterval.DAY) ) {
				tslist = StateMod_TS.readTimeSeriesList (
				InputFile_full, InputStart_DateTime,
				InputEnd_DateTime,
				null,	// Requested units
				true );	// Read all data
			}
			else {
                message = "StateMod file \"" + InputFile_full + "\" is not a recognized interval (bad file format?).";
				Message.printWarning ( warning_level, 
						MessageUtil.formatMessageTag(command_tag,
						++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the file being read is a StateMod file." ) );
				throw new CommandException ( message );
			}
		}

		// Now add the time series to the end of the normal list...

		if ( tslist != null ) {
			Vector TSResultsList_Vector = null;
			try { Object o = processor.getPropContents( "TSResultsList" );
					TSResultsList_Vector = (Vector)o;
			}
			catch ( Exception e ){
				message = "Cannot get time series list to add read time series.  Starting new list.";
				Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(command_tag, ++warning_count),
						routine,message);
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
				TSResultsList_Vector = new Vector();
			}

			// Further process the time series...
			// This makes sure the period is at least as long as the
			// output period...
			int size = tslist.size();
			Message.printStatus ( 2, routine, "Read " + size + " StateMod time series." );
			PropList request_params = new PropList ( "" );
			request_params.setUsingObject ( "TSList", tslist );
			try {
				processor.processRequest( "ReadTimeSeries2", request_params);
			}
			catch ( Exception e ) {
				message =
					"Error post-processing StateMod time series after read.";
					Message.printWarning ( warning_level, 
					MessageUtil.formatMessageTag(command_tag,
					++warning_count), routine, message );
					Message.printWarning(log_level, routine, e);
                    status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report problem to software support." ) );
					throw new CommandException ( message );
			}

			for ( int i = 0; i < size; i++ ) {
				TSResultsList_Vector.addElement ( tslist.elementAt(i) );
			}
			
			// Now reset the list in the processor...
			if ( TSResultsList_Vector != null ) {
				try {	processor.setPropContents ( "TSResultsList", TSResultsList_Vector );
				}
				catch ( Exception e ){
					message = "Cannot set updated time series list.  Results may not be visible.";
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(
						command_tag, ++warning_count),
						routine,message);
                    status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report problem to software support." ) );
				}
			}
		}

		// Free resources from StateMod list...
		tslist = null;
	}
	catch ( Exception e ) {
		Message.printWarning ( log_level, routine, e );
		message = "Unexpected error reading time series from StateMod file.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "See log file for details." ) );
		throw new CommandException ( message );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String InputFile = props.getValue("InputFile");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");
	String Interval = props.getValue("Interval");
	String SpatialAggregation = props.getValue("SpatialAggregation");
	String ParcelYear = props.getValue("ParcelYear");
	StringBuffer b = new StringBuffer ();
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputStart=\"" + InputStart + "\"" );
	}
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputEnd=\"" + InputEnd + "\"" );
	}
	if ( (Interval != null) && (Interval.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Interval=\"" + Interval + "\"" );
	}
	if ( (SpatialAggregation != null) && (SpatialAggregation.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SpatialAggregation=" + SpatialAggregation );
	}
	if ( (ParcelYear != null) && (ParcelYear.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ParcelYear=" + ParcelYear );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

/**
Indicate whether the alias version of the command is being used.  This method
should be called only after the parseCommandParameters() method is called.
*/
protected boolean useAlias ()
{	return _use_alias;
}

}
