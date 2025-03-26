// WriteWaterML_Command - This class initializes, checks, and runs the WriteWaterML() command.

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

package rti.tscommandprocessor.commands.waterml;

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
This class initializes, checks, and runs the WriteWaterML() command.
*/
public class WriteWaterML_Command extends AbstractCommand implements Command, FileGenerator
{

// TODO SAM 2012-02-28 need to use from an enumerator in WaterML package
/**
WaterML versions.
*/
//protected String _WaterML1_1 = "1.1";
protected String _WaterML1_1_JSON = "WaterML-1.1-JSON";
protected String _WaterML2_0 = "WaterML-2.0";
//protected String _WaterML2_0_JSON = "WaterML-2.0-JSON";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteWaterML_Command ()
{	super();
	setCommandName ( "WriteWaterML" );
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
{	String OutputFile = parameters.getValue ( "OutputFile" );
    String Version = parameters.getValue("Version" );
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
		message = "The output file: \"" + OutputFile + "\" must be specified.";
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
	
	if ( (Version != null) && !Version.isEmpty() && !Version.equals(_WaterML2_0) && !Version.equals(_WaterML1_1_JSON)) {
        message = "The version \"" + Version + "\" is invalid";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the version as " + _WaterML1_1_JSON + " or " + _WaterML2_0 + " (default)." ) );
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
	// Check for invalid parameters...
	List<String> valid_Vector = new ArrayList<String>(9);
	valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
	valid_Vector.add ( "OutputFile" );
	valid_Vector.add ( "Version" );
	valid_Vector.add ( "Precision" );
	valid_Vector.add ( "MissingValue" );
	valid_Vector.add ( "OutputStart" );
	valid_Vector.add ( "OutputEnd" );
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
	return (new WriteWaterML_JDialog ( parent, this )).ok();
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

// parseCommand() inherited from parent class

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
    Boolean clearStatus = Boolean.TRUE; // Default.
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
	String OutputFile = parameters.getValue ( "OutputFile" );
	String Version = parameters.getValue ( "Version" );

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
	if ( (OutputStart == null) || OutputStart.isEmpty() ) {
		OutputStart = "${OutputStart}"; // Default global property
	}
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	if ( (OutputEnd == null) || OutputEnd.isEmpty() ) {
		OutputEnd = "${OutputEnd}"; // Default global property
	}
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

	// Now try to write.
    
    // Write the time series file even if no time series are available.  This is useful for
    // troubleshooting and testing (in cases where no time series are available.
    //if ( (tslist != null) && (tslist.size() > 0) ) {
        String OutputFile_full = OutputFile;
        try {
            // Convert to an absolute path...
            OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                    TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
            Message.printStatus ( 2, routine, "Writing WaterML file \"" + OutputFile_full + "\"" );
            List<String> errors = new ArrayList<String>();
            int precision = 4;
            String MissingValue = "NaN";
            boolean printNice = true;
            writeTimeSeriesList ( tslist, OutputFile_full, Version, precision, MissingValue,
    			OutputStart_DateTime, OutputEnd_DateTime,
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
            setOutputFile ( new File(OutputFile_full));
        }
        catch ( Exception e ) {
            message = "Unexpected error writing time series to WaterML file \"" + OutputFile_full + "\" (" + e + ")";
            Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            Message.printWarning ( 3, routine, e );
            status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check log file for details." ) );
            throw new CommandException ( message );
        }
    //}
	
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
		"OutputFile",
		"Version",
		"Precision",
		"MissingValue",
		"OutputStart",
		"OutputEnd"
	};
	return this.toString(parameters, parameterOrder);
}

/**
Write the time series list to a JSON file.
*/
private void writeTimeSeriesList ( List<TS> tslist, String outputFile, String version, Integer precision, String missingValue,
    DateTime outputStart, DateTime outputEnd,
    boolean printNice,
    CommandProcessor processor, CommandStatus status,
    List<String> errors )
{   Message.printStatus(2,"","Writing WaterML version \"" + version + "\" file \"" + outputFile + "\"" );
    PrintWriter fout = null;
    try {
        FileOutputStream fos = new FileOutputStream ( outputFile );
        fout = new PrintWriter ( fos );
        if ( version.equalsIgnoreCase(_WaterML1_1_JSON) ) {
            writeTimeSeriesList1_1_JSON ( fout, tslist, precision, missingValue, outputStart, outputEnd,
            	printNice,
            	errors );
        }
        else if ( version.equalsIgnoreCase(_WaterML2_0) ) {
        	writeTimeSeriesList2_0 ( fout, tslist, precision, missingValue, outputStart, outputEnd,
            	printNice,
            	errors );
        }
        else {
            errors.add("Unrecognized WaterML file version \"" + version + "\"" );
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
Write the version 1.1 format WaterML as JSON.
@param fout open PrintWriter to write to
@param tslist list of time series to write
@param precision number of digits after decimal point to write
@param missingValue the value to output for missing values
@param outputStart output start or null to write full period
@param outputEnd output end or null to write full period
@param printNice if true, print JSON in pretty format and output nulls
@param errors list of error strings to be propagated to calling code
*/
private void writeTimeSeriesList1_1_JSON ( PrintWriter fout, List<TS> tslist, Integer precision, String missingValue,
    DateTime outputStart, DateTime outputEnd,
    boolean printNice,
    List<String> errors )
{   String routine = getClass().getSimpleName() + "writeTimeSeriesList1_1_JSON";
	Message.printStatus(2,routine,"Writing " + tslist.size() + " time series to WaterML 1.1 JSON file." );
	// Construct the WaterML content from time series objects, manipulating data as necessary
	// First create a top-level WaterML content object
	WaterML11JSON w = new WaterML11JSON();
	// Loop through time series
	for ( TS ts : tslist ) {
		WaterML11JSONTimeSeries wts = new WaterML11JSONTimeSeries();
		w.getValue().getTimeSeries().add ( wts );
		// Set some time series properties
		WaterML11JSONVariable wvar = wts.getVariable();
		wvar.setVariableName(ts.getDataType());
		wvar.getUnit().setUnitName(ts.getDataUnits());
		if ( (missingValue == null) || missingValue.isEmpty() ) {
			missingValue = "" + ts.getMissing();
		}
		wvar.setNoDataValue(missingValue);
		// Set the data array
		List<WaterML11JSONTimeSeriesValue> values = wts.getValues();
		TSIterator tsi = null;
		try {
			tsi = ts.iterator(outputStart,outputEnd);
		}
		catch ( Exception e ) {
			Message.printWarning(3,routine,"Error creating iterator for time series (" + e + ").");
			// Skip data output
			continue;
		}
		TSData tsdata = null;
		double value;
		String flag;
		// Precision for numerical values controls the format
		// TODO SAM 2015-09-13 Evaluate whether to default based on units
		String format = "%.4f";
		if ( precision != null ) {
			format = "%." + precision + "f";
		}
		//WaterML11JSONTimeSeriesValueQualifier wq;
		while ( (tsdata = tsi.next()) != null ) {
			WaterML11JSONTimeSeriesValue wval = new WaterML11JSONTimeSeriesValue();
			// TODO SAM 2015-09-13 Need to handle time zone
			wval.setDateTime("" + tsdata.getDate());
			value = tsdata.getDataValue();
			flag = tsdata.getDataFlag();
			if ( ts.isDataMissing(value) ) {
				wval.setValue(missingValue);
			}
			else {
				wval.setValue(StringUtil.formatString(value,format));
			}
			if ( (flag != null) && !flag.isEmpty() ) {
				// TODO SAM 2015-09-13 Need to add a list of these on the time series if not previously added - metadata for flags
				//wq = new WaterML11JSONTimeSeriesValueQualifier();
				wval.getQualifiers().add(flag);
			}
			values.add(wval);
		}
	}
	// Output the WaterML JSON file
	GsonBuilder builder = new GsonBuilder();
	if ( printNice ) {
		builder.setPrettyPrinting().serializeNulls().serializeSpecialFloatingPointValues();
	}
	Gson gson = builder.create();
	fout.print(gson.toJson(w));
}

/**
Write the version 2.0 format WaterML.
@param fout open PrintWriter to write to
@param tslist list of time series to write
@param precision number of digits after decimal point to write
@param missingValue the value to output for missing values
@param outputStart output start or null to write full period
@param outputEnd output end or null to write full period
@param printNice if true, print JSON in pretty format and output nulls
@param errors list of error strings to be propagated to calling code
*/
private void writeTimeSeriesList2_0 ( PrintWriter fout, List<TS> tslist, Integer precision, String missingValue,
    DateTime outputStart, DateTime outputEnd,
    boolean printNice,
    List<String> errors )
{   String routine = getClass().getSimpleName() + "writeTimeSeriesList2_0";
	Message.printStatus(2,routine,"Writing " + tslist.size() + " time series to WaterML 2.0 file." );
	// TODO brute force output until WaterML package can be generated in a robust way
	String s2 = "  ";
	String s4 = "    ";
	String s6 = "      ";
	String s8 = "        ";
	String s10 = "          ";
	String s12 = "            ";
	String s14 = "              ";
	String s16 = "                ";
	String s18 = "                  ";
	// First write the header
	fout.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
	fout.println("<wml2:Collection xmlns:wml2=\"http://www.opengis.net/waterml/2.0\" "
			+ "xmlns:gml=\"http://www.opengis.net/gml/3.2\" "
			+ "xmlns:om=\"http://www.opengis.net/om/2.0\" "
			+ "xmlns:sa=\"http://www.opengis.net/sampling/2.0\" "
			+ "xmlns:sams=\"http://www.opengis.net/samplingSpatial/2.0\" "
			+ "xmlns:swe=\"http://www.opengis.net/swe/2.0\" "
			+ "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
			+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
			// TODO SAM 2015-09-13 What is the following?
			//+ "gml:id="C.USGS.01646500" "
			+ "xsi:schemaLocation=\"http://www.opengis.net/waterml/2.0 http://schemas.opengis.net/waterml/2.0/waterml2.xsd\">");
	// TODO SAM 2015-09-13 Need to enable something like the following
	//fout.println(s2+"  <gml:identifier codeSpace=\"http://waterservices.usgs.gov/nwis/dv\">USGS.01646500</gml:identifier>");
	//fout.println(s2+"  <gml:name codeSpace=\"http://waterservices.usgs.gov/nwis/dv\">Timeseries collected at POTOMAC RIVER NEAR WASH, DC LITTLE FALLS PUMP STA</gml:name>");
	fout.println(s2+"<wml2:metadata>");
	//fout.println(s4+"<wml2:DocumentMetadata gml:id=\"doc.USGS.MP.USGS.01646500\">");
	//fout.println(s6+"<gml:metaDataProperty about=\"contact\" xlink:href=\"http://waterservices.usgs.gov\" />");
	//fout.println(s6+"<wml2:generationDate>2015-09-03T15:01:39.566-04:00</wml2:generationDate>");
	//fout.println(s6+"<wml2:version xlink:href="http://www.opengis.net/waterml/2.0" xlink:title="WaterML 2.0" />");
	//fout.println(s4+"</wml2:DocumentMetadata>");
	fout.println(s2+"</wml2:metadata>");
	// Loop through time series
	for ( TS ts : tslist ) {
		if ( (missingValue == null) || missingValue.isEmpty() ) {
			missingValue = "" + ts.getMissing();
		}
		fout.println(s2+"<wml2:observationMember>");
		//      <om:OM_Observation gml:id="obs.USGS.01646500.00010.5.00001">
		DateTime now = new DateTime(DateTime.DATE_CURRENT);
		String tsid = ts.getIdentifierString();
		fout.println(s4+"<om:OM_Observation gml:id=\"" + tsid + "\">" );
		fout.println(s6+"<om:phenomenonTime>");
		fout.println(s8+"<gml:TimePeriod gml:id=\"sample_time." + tsid + "\">" );
		if ( outputStart == null ) {
			fout.println(s10+"<gml:beginPosition>" + ts.getDate1() + "</gml:beginPosition>" );
		}
		else {
			fout.println(s10+"<gml:beginPosition>" + outputStart + "</gml:beginPosition>" );
		}
		if ( outputEnd == null ) {
			fout.println(s10+"<gml:endPosition>" + ts.getDate2() + "</gml:endPosition>" );
		}
		else {
			fout.println(s10+"<gml:endPosition>" + outputEnd + "</gml:endPosition>" );
		}
		fout.println(s8+"</gml:TimePeriod>" );
		fout.println(s6+"</om:phenomenonTime>" );
		fout.println(s6+"<om:resultTime>" );
		fout.println(s8+"<gml:TimeInstant gml:id=\"requested_time" + tsid + "\">" );
		fout.println(s10+"<gml:timePosition>" + now + "</gml:timePosition>" );
		fout.println(s8+"</gml:TimeInstant>" );
		fout.println(s6+"</om:resultTime>" );
		fout.println(s6+"<om:procedure>" );
		fout.println(s8+"<wml2:ObservationProcess gml:id=\"process." + tsid + "\">" );
		fout.println(s10+"<wml2:processType xlink:href=\"http://www.opengis.net/def/waterml/2.0/processType/Sensor\" xlink:title=\"Sensor\" />");
		fout.println(s10+"<wml2:parameter xlink:title=\"Statistic\" xlink:href=\"http://waterdata.usgs.gov/nwisweb/rdf?statCd=00001\">" );
		fout.println(s12+"<om:NamedValue>" );
		fout.println(s14+"<om:name xlink:title=\"Maximum\" />" );
		fout.println(s14+"<om:value xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">00001</om:value>" );
		fout.println(s12+"</om:NamedValue>" );
		fout.println(s10+"</wml2:parameter>" );
		fout.println(s8+"</wml2:ObservationProcess>" );
		fout.println(s6+"</om:procedure>" );
		fout.println(s6+"<om:observedProperty xlink:title=\"Temperature, water\" xlink:href=\"http://waterdata.usgs.gov/nwisweb/rdf?parmCd=00010\" />" );
		fout.println(s6+"<om:featureOfInterest xlink:title=\"POTOMAC RIVER NEAR WASH, DC LITTLE FALLS PUMP STA>\"");
		fout.println(s8+"<wml2:MonitoringPoint gml:id=\"" + tsid + "\">" );
		fout.println(s10+"<gml:descriptionReference xlink:href=\"http://waterservices.usgs.gov/nwis/site/?sites=01646500&amp;agencyCd=USGS&amp;format=rdb\" "
				+ "xlink:title=\"POTOMAC RIVER NEAR WASH, DC LITTLE FALLS PUMP STA\" />" );
		fout.println(s10+"<sa:sampledFeature xlink:title=\"POTOMAC RIVER NEAR WASH, DC LITTLE FALLS PUMP STA, 4.1 ft from riverbed (middle)\" />" );
		fout.println(s10+"<sams:shape>" );
		fout.println(s12+"<gml:Point gml:id=\"" + tsid + "\">" );
		fout.println(s14+"<gml:pos srsName=\"urn:ogc:def:crs:EPSG:4326\">38.94977778 -77.12763889</gml:pos>" );
		fout.println(s12+"</gml:Point>" );
		fout.println(s10+"</sams:shape>" );
		fout.println(s8+"</wml2:MonitoringPoint>" );
		fout.println(s6+"</om:featureOfInterest>" );
		fout.println(s6+"<om:result>" );
		fout.println(s8+"<wml2:MeasurementTimeseries gml:id=\"TS." + tsid + "\">" );
		// TODO SAM 2015-09-14 these are flag descriptions - need to handle
		fout.println(s10+"<wml2:defaultPointMetadata>" );
		fout.println(s12+"<wml2:DefaultTVPMeasurementMetadata>" );
		fout.println(s14+"<wml2:qualifier xlink:title=\"Provisional data subject to revision.\">" );
		fout.println(s16+"<swe:Category definition=\"http://waterdata.usgs.gov/nwisweb/rdf?dvQualCd=P\">" );
		fout.println(s18+"<swe:description>Provisional</swe:description>" );
		fout.println(s18+"<swe:value>P</swe:value>" );
		fout.println(s16+"</swe:Category>" );
		fout.println(s14+"</wml2:qualifier>" );
		fout.println(s14+"<wml2:uom xlink:title=\"deg C\" />" );
		fout.println(s14+"<wml2:interpolationType />" );
		fout.println(s12+"</wml2:DefaultTVPMeasurementMetadata>" );
		fout.println(s10+"</wml2:defaultPointMetadata>" );
		TSIterator tsi = null;
		try {
			tsi = ts.iterator(outputStart,outputEnd);
		}
		catch ( Exception e ) {
			Message.printWarning(3,routine,"Error creating iterator for time series (" + e + ").");
			// Skip data output
			continue;
		}
		TSData tsdata = null;
		double value;
		String flag;
		// Precision for numerical values controls the format
		// TODO SAM 2015-09-13 Evaluate whether to default based on units
		String format = "%.4f";
		if ( precision != null ) {
			format = "%." + precision + "f";
		}
		while ( (tsdata = tsi.next()) != null ) {
			fout.println(s10+"<wml2:point>" );
			fout.println(s12+"<wml2:MeasurementTVP>" );
			// TODO SAM 2015-09-13 Need to handle time zone
			fout.println(s14+"<wml2:time>" + tsdata.getDate() + "</wml2:time>" );
			value = tsdata.getDataValue();
			flag = tsdata.getDataFlag();
			if ( ts.isDataMissing(value) ) {
				fout.println(s14+"<wml2:value>" + missingValue + "</wml2:value>" );
			}
			else {
				fout.println(s14+"<wml2:value>" + StringUtil.formatString(value,format) + "</wml2:value>" );
			}
			if ( (flag != null) && !flag.isEmpty() ) {
				// TODO SAM 2015-09-13 Need to add a list of these on the time series if not previously added - metadata for flags
				// TODO SAM 2015-09-14 Need to implement output - need an example
			}
			fout.println(s12+"</wml2:MeasurementTVP>" );
			fout.println(s10+"</wml2:point>" );
		}
		fout.println(s8+"</wml2:MeasurementTimeSeries>" );
		fout.println(s6+"</om:result>" );
		fout.println(s4+"</om:OM_Observation>" );
		fout.println(s2+"</wl2:observationMember>" );
	}
	// Write the footer
	fout.println("</wml2:Collection>");
}

}
