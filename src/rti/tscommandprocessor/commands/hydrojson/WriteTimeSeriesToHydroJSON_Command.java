// WriteTimeSeriesToHydroJSON_Command - This class initializes, checks, and runs the WriteTimeSeriesToHydroJSON() command.

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

package rti.tscommandprocessor.commands.hydrojson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSIterator;
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
This class initializes, checks, and runs the WriteTimeSeriesToHydroJSON() command.
*/
public class WriteTimeSeriesToHydroJSON_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Possible values for PrintNice parameter.
*/
public final String _False = "False";
public final String _True = "True";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteTimeSeriesToHydroJSON_Command ()
{	super();
	setCommandName ( "WriteTimeSeriesToHydroJSON" );
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
{	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
    String MissingValue = parameters.getValue("MissingValue" );
    String Precision = parameters.getValue ( "Precision" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String PrintNice = parameters.getValue ( "PrintNice" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
		message = "The output file: \"" + OutputFile + "\" must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
			message, "Specify an output file." ) );
	}
	else {
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
		try {	DateTime datetime1 = DateTime.parse(OutputStart);
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
		try {	DateTime datetime2 = DateTime.parse(OutputEnd);
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
	if ( (PrintNice != null) && !PrintNice.isEmpty() ) {
		if ( !PrintNice.equalsIgnoreCase(_False) && !PrintNice.equalsIgnoreCase(_True) ) {
			message = "The PrintNice parameter \"" + PrintNice + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " (default) or " + _True + "."));
		}
	}

	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(31);
	validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
	validList.add ( "OutputStart" );
	validList.add ( "OutputEnd" );
	validList.add ( "MissingValue" );
	validList.add ( "Precision" );
	validList.add ( "TIdentifier" );
	validList.add ( "THash" );
	validList.add ( "TQualityType" );
	validList.add ( "TParameter" );
	validList.add ( "TDuration" );
	validList.add ( "TInterval" );
	validList.add ( "TUnits" );
	validList.add ( "SName" );
	validList.add ( "SResponsibility" );
	validList.add ( "SCoordLatitude" );
	validList.add ( "SCoordLongitude" );
	validList.add ( "SCoordDatum" );
	validList.add ( "SHUC" );
	validList.add ( "SElevValue" );
	validList.add ( "SElevAccuracy" );
	validList.add ( "SElevDatum" );
	validList.add ( "SElevMethod" );
	validList.add ( "STimeZone" );
	validList.add ( "STimeZoneOffset" );
	validList.add ( "STimeFormat" );
	validList.add ( "SActiveFlag" );
	validList.add ( "SLocationType" );
	validList.add ( "OutputFile" );
	validList.add ( "PrintNice" );
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
	return (new WriteTimeSeriesToHydroJSON_Dialog ( parent, this )).ok();
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
	
	// Clear the output file
	
	setOutputFile ( null );
	
	// Check whether the processor wants output files to be created...

	CommandProcessor processor = getCommandProcessor();
	if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
			Message.printStatus ( 2, routine,
			"Skipping \"" + toString() + "\" because output is not being created." );
	}
	
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
		status.clearLog(commandPhase);
	}

	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
	String OutputStart = parameters.getValue ( "OutputStart" );
	if ( (OutputStart == null) || OutputStart.isEmpty() ) {
		OutputStart = "${OutputStart}"; // Default global property
	}
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	if ( (OutputEnd == null) || OutputEnd.isEmpty() ) {
		OutputEnd = "${OutputEnd}"; // Default global property
	}
    String MissingValue = parameters.getValue ( "MissingValue" );
	String Precision = parameters.getValue ( "Precision" );
	Integer precision = new Integer(4);
    if ( (Precision != null) && (Precision.length() > 0) ) {
        precision = Integer.parseInt(Precision);
    }
	String TIdentifier = parameters.getValue ( "TIdentifier" );
    String THash = parameters.getValue("THash");
    String TQualityType = parameters.getValue("TQualityType");
    String TParameter = parameters.getValue("TParameter");
    String TDuration = parameters.getValue("TDuration");
    String TInterval = parameters.getValue("TInterval");
    String TUnits = parameters.getValue("TUnits");
	String SName = parameters.getValue ( "SName" );
	String SResponsibility = parameters.getValue ( "SResponsibility" );
	String SCoordLatitude = parameters.getValue("SCoordLatitude");
    String SCoordLongitude = parameters.getValue("SCoordLongitude");
    String SCoordDatum = parameters.getValue("SCoordDatum");
    String SHUC = parameters.getValue("SHUC");
    String SElevValue = parameters.getValue("SElevValue");
    String SElevAccuracy = parameters.getValue("SElevAccuracy");
    String SElevDatum = parameters.getValue("SElevDatum");
    String SElevMethod = parameters.getValue("SElevMethod");
	String STimeZone = parameters.getValue("STimeZone");
	String STimeZoneOffset = parameters.getValue("STimeZoneOffset");
	String STimeFormat = parameters.getValue("STimeFormat");
	String SActiveFlag = parameters.getValue("SActiveFlag");
	String SLocationType = parameters.getValue("SLocationType");
	String OutputFile = parameters.getValue ( "OutputFile" );
	String PrintNice = parameters.getValue ( "PrintNice" );
	boolean printNice = false;
	if ( (PrintNice != null) && PrintNice.equalsIgnoreCase("true") ) {
		printNice = true;
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
	
	// Get the output period
    
	DateTime OutputStart_DateTime = null;
	DateTime OutputEnd_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			OutputStart_DateTime = TSCommandProcessorUtil.getDateTime ( OutputStart, "OutputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			OutputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( OutputEnd, "OutputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
	}
    
    String OutputFile_full = OutputFile;
    try {
        // Convert to an absolute path...
        OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
        Message.printStatus ( 2, routine, "Writing HydroJSON file \"" + OutputFile_full + "\"" );
        List<String> errors = new ArrayList<String>();
        String version = "01";
        writeTimeSeriesList ( tslist, OutputFile_full, version, precision, MissingValue,
			OutputStart_DateTime, OutputEnd_DateTime,
			TIdentifier, THash, TQualityType, TParameter, TDuration, TInterval, TUnits,
			SName, SResponsibility, SCoordLatitude, SCoordLongitude, SCoordDatum, SHUC,
			SElevValue, SElevAccuracy, SElevDatum, SElevMethod,
			STimeZone, STimeZoneOffset, STimeFormat, SActiveFlag,
			SLocationType,
			printNice,
			processor, status,
			errors );
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
        message = "Unexpected error writing time series to JSON file \"" + OutputFile_full + "\" (" + e + ")";
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
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"TSList",
		"TSID",
		"EnsembleID",
		"OutputStart",
		"OutputEnd",
		"MissingValue",
		"Precision",
		"TIdentifier",
    	"THash",
    	"TQualityType",
    	"TParameter",
    	"TDuration",
    	"TInterval",
    	"TUnits",
    	"SName",
    	"SResponsibility",
    	"SCoordLatitude",
    	"SCoordLongitude",
    	"SCoordDatum",
    	"SHUC",
    	"SElevValue",
    	"SElevAccuracy",
    	"SElevDatum",
    	"SElevMethod",
		"STimeZone",
		"STimeZoneOffset",
		"STimeFormat",
		"SActiveFlag",
		"SLocationType",
		"OutputFile",
		"PrintNice"
	};
	return this.toString(parameters, parameterOrder);
}

// TODO SAM 2015-08-24 Evaluate whether a separate package should be created - for now keep the code here
// until there is time to split out
/**
Write the time series list to a JSON file.
*/
private void writeTimeSeriesList ( List<TS> tslist, String outputFile, String version, Integer precision, String missingValue,
    DateTime outputStart, DateTime outputEnd,
    String tIdentifier, String tHash, String tQualityType, String tParameter, String tDuration, String tInterval, String tUnits,
    String sName, String sResponsibility, String sCoordLatitude, String sCoordLongitude, String sCoordDatum,
    String sHUC,
    String sElevValue, String sElevAccuracy, String sElevDatum, String sElevMethod,
    String sTimeZone, String sTimeZoneOffset, String sTimeFormat, String sActiveFlag,
    String sLocationType,
    boolean printNice,
    CommandProcessor processor, CommandStatus status,
    List<String> errors )
{   Message.printStatus(2,"","Writing HydroJSON version \"" + version + "\" file \"" + outputFile + "\"" );
    PrintWriter fout = null;
    try {
        FileOutputStream fos = new FileOutputStream ( outputFile );
        fout = new PrintWriter ( fos );
        if ( version.endsWith("01") ) {
            writeTimeSeriesList01 ( fout, tslist, precision, missingValue, outputStart, outputEnd,
            	tIdentifier, tHash, tQualityType, tParameter, tDuration, tInterval, tUnits,
            	sName, sResponsibility, sCoordLatitude, sCoordLongitude, sCoordDatum, sHUC,
            	sElevValue, sElevAccuracy, sElevDatum, sElevMethod,
            	sTimeZone, sTimeZoneOffset, sTimeFormat, sActiveFlag,
            	sLocationType,
            	printNice,
            	processor, status,
            	errors );
        }
        else {
            errors.add("Unrecognized JSON time series file version \"" + version + "\"" );
        }
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
Write the version 01 format HydroJSON (currently HydroJSON does not publish a version so this is an internal version).
@param fout open PrintWriter to write to
@param tslist list of time series to write
@param outputStart output start or null to write full period
@param outputEnd output end or null to write full period
@param printNice if true, print JSON in pretty format and output nulls
@param errors list of error strings to be propagated to calling code
*/
private void writeTimeSeriesList01 ( PrintWriter fout, List<TS> tslist, Integer precision, String missingValue,
    DateTime outputStart, DateTime outputEnd,
    String tIdentifier, String tHash, String tQualityType, String tParameter, String tDuration, String tInterval, String tUnits,
    String sName, String sResponsibility,
    String sCoordLatitude, String sCoordLongitude, String sCoordDatum,
    String sHUC,
    String sElevValue, String sElevAccuracy, String sElevDatum, String sElevMethod,
    String sTimeZone, String sTimeZoneOffset, String sTimeFormat, String sActiveFlag,
    String sLocationType,
    boolean printNice,
    CommandProcessor processor, CommandStatus status,
    List<String> errors )
{   Message.printStatus(2,"","Writing " + tslist.size() + " time series to HydroJSON file." ); // TODO SAM add version to message later
	// Construct the HydroJSON content from time series objects, manipulating data as necessary
	// First create a top-level HydroJSON content object
	HydroJSONContent hj = new HydroJSONContent();
	// HydroJSON groups time series by station so first have to get a unique list of stations, using siteid
	List<HydroJSONStation> stationList = new ArrayList<HydroJSONStation>();
	List<String> stationNames = new ArrayList<String>();
	for ( TS ts : tslist ) {
		String stationName = ts.getLocation();
		if ( (sName != null) && !sName.isEmpty() ) {
			stationName = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
				processor, ts, sName, status, CommandPhaseType.RUN);
		}
		boolean match = false;
		for ( String name : stationNames ) {
			if ( stationName.equals(name) ) {
				match = true;
				break;
			}
		}
		if ( !match ) {
			stationNames.add(stationName);
		}
	}
	// Now process the unique list of stations
	for ( String stationName : stationNames ) {
		HydroJSONStation hstation = new HydroJSONStation(stationName);
		stationList.add(hstation);
		// Get the list of time series associated with the station
		List<HydroJSONTimeSeries> stationTslist = new ArrayList<HydroJSONTimeSeries>();
		hstation.setTimeSeriesList(stationTslist);
		int its = 0;
		for ( TS ts : tslist ) {
			String stationName2 = ts.getLocation();
			if ( (sName != null) && !sName.isEmpty() ) {
				stationName2 = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
					processor, ts, sName, status, CommandPhaseType.RUN);
			}
			if ( stationName2.equals(stationName) ) {
				// Have a matching time series for this station - add time series to the station data
				// Also add station properties if the first matching time series
				++its;
				if ( its == 1 ) {
					String responsibility = "";
					if ( (sResponsibility != null) && !sResponsibility.isEmpty() ) {
						responsibility = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
							processor, ts, sResponsibility, status, CommandPhaseType.RUN);
					}
					hstation.setResponsibility(responsibility);
					Double latitude = null;
					if ( sCoordLatitude != null ) {
						String lat = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
							processor, ts, sCoordLatitude, status, CommandPhaseType.RUN);
						try {
							latitude = Double.parseDouble(lat);
						}
						catch ( NumberFormatException e ) {
							// Leave null
						}
					}
					hstation.getCoordinates().setLatitude(latitude);
					Double longitude = null;
					if ( sCoordLongitude != null ) {
						String lon = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
							processor, ts, sCoordLongitude, status, CommandPhaseType.RUN);
						try {
							longitude = Double.parseDouble(lon);
						}
						catch ( NumberFormatException e ) {
							// Leave null
						}
					}
					hstation.getCoordinates().setLongitude(longitude);
					String hdatum = "";
					if ( (sCoordDatum != null) && !sCoordDatum.isEmpty() ) {
						hdatum = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
							processor, ts, sCoordDatum, status, CommandPhaseType.RUN);
					}
					hstation.getCoordinates().setDatum(hdatum);
					String huc = "";
					if ( (sHUC != null) && !sHUC.isEmpty() ) {
						huc = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
							processor, ts, sHUC, status, CommandPhaseType.RUN);
					}
					hstation.setHuc(huc);
					Double elevation = null;
					if ( sElevValue != null ) {
						String elev = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
							processor, ts, sElevValue, status, CommandPhaseType.RUN);
						try {
							elevation = Double.parseDouble(elev);
						}
						catch ( NumberFormatException e ) {
							// Leave null
						}
					}
					hstation.getElevation().setValue(elevation);
					Double accuracy = null;
					if ( sElevAccuracy != null ) {
						String acc = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
							processor, ts, sElevAccuracy, status, CommandPhaseType.RUN);
						try {
							accuracy = Double.parseDouble(acc);
						}
						catch ( NumberFormatException e ) {
							// Leave null
						}
					}
					hstation.getElevation().setAccuracy(accuracy);
					String vdatum = "";
					if ( (sElevDatum != null) && !sElevDatum.isEmpty() ) {
						vdatum = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
							processor, ts, sElevDatum, status, CommandPhaseType.RUN);
					}
					hstation.getElevation().setDatum(vdatum);
					String method = "";
					if ( (sElevMethod != null) && !sElevMethod.isEmpty() ) {
						method = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
							processor, ts, sElevMethod, status, CommandPhaseType.RUN);
					}
					hstation.getElevation().setMethod(method);
					String tz = "";
					if ( (sTimeZone != null) && !sTimeZone.isEmpty() ) {
						tz = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
							processor, ts, sTimeZone, status, CommandPhaseType.RUN);
					}
					hstation.setTimeZone(tz);
					String locationType = "";
					if ( (sLocationType != null) && !sLocationType.isEmpty() ) {
						locationType = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
							processor, ts, sLocationType, status, CommandPhaseType.RUN);
					}
					hstation.setLocationType(locationType);
				}
				HydroJSONTimeSeries hts = new HydroJSONTimeSeries();
				String htsid = ts.getAlias(); // Use alias if available
				if ( (htsid == null) || htsid.isEmpty() ) {
					htsid = ts.getIdentifierString(); // Otherwise use TSID
				}
				if ( (tIdentifier != null) && !tIdentifier.isEmpty() ) {
					htsid = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
						processor, ts, tIdentifier, status, CommandPhaseType.RUN);
				}
				hts.setTsid(htsid);
				stationTslist.add(hts);
				// Set the time series values
				List<HydroJSONTimeSeriesValue> values = hts.getValues();
			    TSIterator tsi = null;
		        try {
		            tsi = ts.iterator(outputStart, outputEnd);
		        }
		        catch ( Exception e ) {
		            errors.add ( "Error creating iterator for time series data (" + e + ")." );
		            continue; 
		        }
		        // Process the data array
		        TSData tsdata;
		        //String valueFormat = "%.4f";
		        if ( precision != null ) {
		            //valueFormat = "%." + precision + "f";
		        }
		        //StringBuffer b = new StringBuffer();
		        int count = 0;
		        int nonMissingCount = 0;
		        while ( (tsdata = tsi.next()) != null ) {
		            ++count;
		            HydroJSONTimeSeriesValue value = new HydroJSONTimeSeriesValue();
		            value.setTimestamp(tsdata.getDate().toString());
		            if ( !ts.isDataMissing(tsdata.getDataValue()) ) {
		            	++nonMissingCount;
		            }
		            value.setValue(tsdata.getDataValue());
		            value.setQuality(tsdata.getDataFlag());
		            values.add(value);
		        }
				// Set the time series properties
		        // Site quality - is this for irregular time series?
		        String hash = "";
				if ( (tHash != null) && !tHash.isEmpty() ) {
					hash = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
						processor, ts, tHash, status, CommandPhaseType.RUN);
				}
				hts.setHash(hash);
		        String qt = "";
				if ( (tQualityType != null) && !tQualityType.isEmpty() ) {
					qt = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
						processor, ts, tQualityType, status, CommandPhaseType.RUN);
				}
				hts.setQualityType(qt);
		        String parameter = ts.getDataType();
				if ( (tParameter != null) && !tParameter.isEmpty() ) {
					parameter = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
						processor, ts, tParameter, status, CommandPhaseType.RUN);
				}
				hts.setParameter(parameter);
		        String duration = "";
				if ( (tDuration != null) && !tDuration.isEmpty() ) {
					duration = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
						processor, ts, tDuration, status, CommandPhaseType.RUN);
				}
				hts.setDuration(duration);
		        String interval = "";
				if ( (tInterval != null) && !tInterval.isEmpty() ) {
					interval = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
						processor, ts, tInterval, status, CommandPhaseType.RUN);
				}
				hts.setInterval(interval);
				hts.setCount(new Integer(count));
				Double minValue = null;
				Double maxValue = null;
				if ( nonMissingCount > 0 ) {
					minValue = ts.getDataLimits().getMinValue();
					maxValue = ts.getDataLimits().getMaxValue();
				}
				if ( minValue != null ) {
					hts.setMinValue(new Double(minValue));
				}
				if ( maxValue != null ) {
					hts.setMaxValue(new Double(maxValue));
				}
				if ( outputStart == null ) {
					if ( ts.getDate1() != null ) {
						hts.setStartTimestamp(ts.getDate1().toString());
					}
				}
				else {
					hts.setStartTimestamp(outputStart.toString());
				}
				if ( outputEnd == null ) {
					if ( ts.getDate1() != null ) {
						hts.setEndTimestamp(ts.getDate2().toString());
					}
				}
				else {
					hts.setEndTimestamp(outputEnd.toString());
				}
		        String units = ts.getDataUnits();
				if ( (tUnits != null) && !tUnits.isEmpty() ) {
					units = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
						processor, ts, tUnits, status, CommandPhaseType.RUN);
				}
				hts.setUnits(units);
			}
		}
	}
	// Finally set the stations, which will cause all other data to be set
	hj.setStationList(stationList);
	// Output the HydroJSON file
	GsonBuilder builder = new GsonBuilder();
	if ( printNice ) {
		builder.setPrettyPrinting().serializeNulls().serializeSpecialFloatingPointValues();
	}
	Gson gson = builder.create();
	fout.print(gson.toJson(hj));
/*
    String nl = System.getProperty("line.separator");
    String i1 = " "; // Indentation levels
    String i2 = "  ";
    String i3 = "   ";
    String i4 = "    ";
    String i5 = "     ";
    fout.write( "{" + nl );
    // Overall properties
    fout.write( i1 + "\"timeSeriesList\": {" + nl );
    fout.write( i2 + "\"numTimeSeries\": " + tslist.size() + "," + nl );
    // Array of time series
    fout.write( i2 + "\"timeSeries\": [" + nl );
    // Represent each time series completely separately
    int its = -1;
    for ( TS ts : tslist ) {
        ++its;
        // Metadata about each time series
        if ( its == 0 ) {
            fout.write( i3+ "{" + nl );
        }
        else {
            fout.write( "," + nl + i3+ "{" + nl );
        }
        // Determie the missing value as a string, to output in metadata and 
        String missingValueString = "" + ts.getMissing();
        if ( (missingValue != null) && !missingValue.equals("") ) {
            missingValueString = missingValue;
        }
        fout.write( i4 + "\"timeSeriesMeta\": {" + nl );
        fout.write( i5 + "\"tsid\": \"" + ts.getIdentifierString() + "\"," + nl );
        fout.write( i5 + "\"alias\": \"" + ts.getAlias() + "\"," + nl );
        fout.write( i5 + "\"description\": \"" + ts.getDescription() + "\"," + nl );
        fout.write( i5 + "\"locationType\": \"" + ts.getIdentifier().getLocationType() + "\"," + nl );
        fout.write( i5 + "\"locationId\": \"" + ts.getIdentifier().getLocation() + "\"," + nl );
        fout.write( i5 + "\"dataSource\": \"" + ts.getIdentifier().getSource() + "\"," + nl );
        fout.write( i5 + "\"dataType\": \"" + ts.getIdentifier().getType() + "\"," + nl );
        fout.write( i5 + "\"scenario\": \"" + ts.getIdentifier().getScenario() + "\"," + nl );
        if ( missingValueString.equalsIgnoreCase("NaN") ) {
            // JSON does not handle NaN as a number so have to quote and parsing code needs to handle
            fout.write( i5 + "\"missingVal\": \"" + missingValueString + "\"," + nl );
        }
        else {
            // No need to quote number
            fout.write( i5 + "\"missingVal\": " + missingValueString + "," + nl );
        }
        fout.write( i5 + "\"units\": \"" + ts.getDataUnits() + "\"," + nl );
        fout.write( i5 + "\"unitsOriginal\": \"" + ts.getDataUnitsOriginal() + "\"," + nl );
        fout.write( i5 + "\"start\": \"" + ts.getDate1() + "\"," + nl );
        fout.write( i5 + "\"end\": \"" + ts.getDate2() + "\"," + nl );
        fout.write( i5 + "\"startOriginal\": \"" + ts.getDate1Original() + "\"," + nl );
        fout.write( i5 + "\"endOriginal\": \"" + ts.getDate2Original() + "\"," + nl );
        boolean hasDataFlags = ts.hasDataFlags();
        fout.write( i5 + "\"hasDataFlags\": " + hasDataFlags + nl );
        fout.write( i4 + "}," + nl );
        // Data for each time series (write arrays separately but have an option to line up
        fout.write( i4 + "\"timeSeriesData\": [" + nl );
        TSIterator tsi = null;
        try {
            tsi = ts.iterator(outputStart, outputEnd);
        }
        catch ( Exception e ) {
            errors.add ( "Error creating iterator for time series data (" + e + ")." );
            continue; 
        }
        // Process the data array
        TSData tsdata;
        double value;
        String valueFormat = "%.4f";
        if ( precision != null ) {
            valueFormat = "%." + precision + "f";
        }
        StringBuffer b = new StringBuffer();
        int iVal = -1;
        while ( (tsdata = tsi.next()) != null ) {
            ++iVal;
            b.setLength(0);
            if ( iVal != 0 ) {
                // Comma and newline after previous value
                b.append ( "," + nl );
            }
            b.append ( i5 );
            b.append ( "{ \"dt\": \"" );
            b.append ( tsdata.getDate().toString() );
            b.append ( "\"" );
            value = tsdata.getDataValue();
            if ( ts.isDataMissing(value) ) {
                b.append ( ", \"value\": " + missingValueString );
            }
            else {
                b.append ( ", \"value\": " + StringUtil.formatString(value,valueFormat) );
            }
            if ( hasDataFlags ) {
                b.append ( ", \"flag\": \"" );
                b.append ( tsdata.getDataFlag() );
                b.append ( "\"" );
            }
            b.append ( " }" );
            fout.write( b.toString() );
        }
        fout.write( nl + i4 + "]" + nl );
        fout.write( i3 + "}" );
    }
    fout.write( nl + i2 + "]" + nl );
    fout.write( i1 + "}" + nl );   
    fout.write( "}" + nl );
    */
}

}
