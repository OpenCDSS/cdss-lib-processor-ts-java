package rti.tscommandprocessor.commands.json;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

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
This class initializes, checks, and runs the WriteTimeSeriesToJson() command.
*/
public class WriteTimeSeriesToJson_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteTimeSeriesToJson_Command ()
{	super();
	setCommandName ( "WriteTimeSeriesToJson" );
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
	List<String> validList = new ArrayList<String>(8);
	validList.add ( "OutputFile" );
	validList.add ( "Precision" );
	validList.add ( "MissingValue" );
	validList.add ( "OutputStart" );
	validList.add ( "OutputEnd" );
	validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
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
	return (new WriteTimeSeriesToJson_Dialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
	List<File> list = new Vector();
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
	if ( (commandPhase == CommandPhaseType.RUN) && (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
	String OutputFile = parameters.getValue ( "OutputFile" );

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
	List tslist = (List)o_TSList;
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
        Message.printStatus ( 2, routine, "Writing JSON file \"" + OutputFile_full + "\"" );
        List<String> errors = new Vector<String>();
        String version = "01";
        boolean overlap = false;
        writeTimeSeriesList ( tslist, OutputFile_full, version, precision, MissingValue,
			OutputStart_DateTime, OutputEnd_DateTime, overlap, errors );
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
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String OutputFile = parameters.getValue ( "OutputFile" );
	String Precision = parameters.getValue("Precision");
	String MissingValue = parameters.getValue("MissingValue");
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
    String TSList = parameters.getValue ( "TSList" );
    String TSID = parameters.getValue( "TSID" );
    String EnsembleID = parameters.getValue( "EnsembleID" );
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
Write the time series list to a JSON file.
*/
private void writeTimeSeriesList ( List<TS> tslist, String outputFile, String version, Integer precision, String missingValue,
    DateTime outputStart, DateTime outputEnd, boolean overlap, List<String> errors )
{   Message.printStatus(2,"","Writing JSON version \"" + version + "\" file \"" + outputFile + "\"" );
    PrintWriter fout = null;
    try {
        FileOutputStream fos = new FileOutputStream ( outputFile );
        fout = new PrintWriter ( fos );
        if ( version.endsWith("01") ) {
            writeTimeSeriesList01 ( fout, tslist, precision, missingValue, outputStart, outputEnd, overlap, errors );
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
Write the version 01 format JSON.
@param fout open PrintWriter to write to
@param tslist list of time series to write
@param outputStart output start or null to write full period
@param outputEnd output end or null to write full period
@param overlap if true, write the data in overlapping fashion (date/time shared between time series)
@param errors list of error strings to be propagated to calling code
*/
private void writeTimeSeriesList01 ( PrintWriter fout, List<TS> tslist, Integer precision, String missingValue,
    DateTime outputStart, DateTime outputEnd,
    boolean overlap, List<String> errors )
{   Message.printStatus(2,"","Writing " + tslist.size() + " time series to JSON 01 version file." );
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
    fout.write( i2 + "\"overlap\": " + overlap + "," + nl );
    // Array of time series
    fout.write( i2 + "\"timeSeries\": [" + nl );
    if ( overlap ) {
        // Overlap the time series in output, similar to DateValue file
        // TODO SAM 2013-07-01 Need to enable
    }
    else {
        // Represent each time series completely separately
        int its = -1;
        for ( TS ts : tslist ) {
            if ( ts == null ) {
            	continue;
            }
            ++its;
            boolean canWriteValues = true; // Whether data array can be written
            // Metadata about each time series
            if ( its == 0 ) {
                fout.write( i3+ "{" + nl );
            }
            else {
                fout.write( "," + nl + i3+ "{" + nl );
            }
            // Determine the missing value as a string, to output in metadata and 
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
            fout.write( i4 + "}," + nl ); // End of starting "timeSeriesMeta": {
            // Data for each time series (write arrays separately but have an option to line up
            fout.write( i4 + "\"timeSeriesData\": [" + nl );
            TSIterator tsi = null;
            try {
                tsi = ts.iterator(outputStart, outputEnd);
            }
            catch ( Exception e ) {
                errors.add ( "Error creating iterator for time series data (" + e + ")." );
                canWriteValues = false;
            }
            if ( canWriteValues ) {
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
            }
            // Always close the data value array, whether or not values were written
            fout.write( nl + i4 + "]" + nl ); // Close bracket that was opened with "timeSeriesData": [
            // Close the time 
            fout.write( i3 + "}" ); // Close bracket for the time series in the timeSeries array
        }
    }
    fout.write( nl + i2 + "]" + nl ); // Close time series array started with:  "timeSeries": [
    fout.write( i1 + "}" + nl ); // Close "timeSeriesList": {
    fout.write( "}" + nl ); // Close top level JSON
}

}