package rti.tscommandprocessor.commands.datastream;

import java.io.File;
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
import RTi.Util.Time.DateTimeFormatterType;
import RTi.Util.Time.TimeUtil;

/**
This class initializes, checks, and runs the WriteTimeSeriesToDataStream() command.
*/
public class WriteTimeSeriesToDataStream_Command extends AbstractCommand implements Command, FileGenerator
{
    
/**
Values for Append parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Values for MissingValue parameter.
*/
protected final String _Blank = "Blank";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteTimeSeriesToDataStream_Command ()
{	super();
	setCommandName ( "WriteTimeSeriesToDataStream" );
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
    String Append = parameters.getValue ( "Append" );
    String OutputLineFormat = parameters.getValue ( "OutputLineFormat" );
    String OutputLineFormatFile = parameters.getValue ( "OutputLineFormatFile" );
    String dateTimeFormatterType = parameters.getValue ( "DateTimeFormatterType" );
    String DateTimeFormat = parameters.getValue ( "DateTimeFormat" );
    String Precision = parameters.getValue ( "Precision" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String NonMissingOutputCount = parameters.getValue ( "NonMissingOutputCount" );
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
	
   if ( (Append != null) && (Append.length() != 0) && !Append.equalsIgnoreCase(_True) &&
        !Append.equalsIgnoreCase(_False)) {
        message="Append (" + Append + ") is not a valid value (True or False).";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as True or false." ) );
    }
   
    if ( ((OutputLineFormat == null) || (OutputLineFormat.length() == 0)) &&
        ((OutputLineFormatFile == null) || (OutputLineFormatFile.length() == 0)) ) {
        message = "The output line format or format file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify an output line format OR format file." ) );
    }
    if ( ((OutputLineFormat != null) && (OutputLineFormat.length() == 0)) &&
        ((OutputLineFormatFile != null) && (OutputLineFormatFile.length() == 0)) ) {
        message = "The output line format or format file cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify an output line format OR format file." ) );
    }
    
    if ( (OutputLineFormatFile != null) && (OutputLineFormatFile.length() > 0) ) {
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
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputLineFormatFile)));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The output line format file parent directory does not exist for: \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Create the input directory and file." ) );
            }
            f = null;
            f2 = null;
        }
        catch ( Exception e ) {
            message = "The format file:\n" +
            "    \"" + OutputLineFormatFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that format file and working directory paths are compatible." ) );
        }
    }
    
    if ( (dateTimeFormatterType != null) && !dateTimeFormatterType.equals("") ) {
        // Check the value given the type - only support types that are enabled in this command.
        if ( !dateTimeFormatterType.equalsIgnoreCase(""+DateTimeFormatterType.C) ) {
            message = "The date/time formatter \"" + dateTimeFormatterType + "\" is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the date/time formatter type as " + DateTimeFormatterType.C ));
        }
    }
    if ( (DateTimeFormat == null) || DateTimeFormat.equals("") ) {
        message = "The date/time format must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Provide a date/time format." ) );
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
	
    if ( (NonMissingOutputCount != null) && !NonMissingOutputCount.equals("") ) {
        if ( !StringUtil.isInteger(Precision) ) {
            message = "The non-missing output count \"" + NonMissingOutputCount + "\" is not an integer.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the non-missing output count as an integer." ) );
        }
    }

	// Check for invalid parameters...
	List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
	valid_Vector.add ( "OutputFile" );
	valid_Vector.add ( "Append" );
    valid_Vector.add ( "OutputFileHeader" );
    valid_Vector.add ( "OutputLineFormat" );
    valid_Vector.add ( "OutputLineFormatFile" );
    valid_Vector.add ( "DateTimeFormatterType" );
    valid_Vector.add ( "DateTimeFormat" );
	valid_Vector.add ( "OutputFileFooter" );
	valid_Vector.add ( "Precision" );
	valid_Vector.add ( "MissingValue" );
	valid_Vector.add ( "OutputStart" );
	valid_Vector.add ( "OutputEnd" );
	valid_Vector.add ( "NonMissingOutputCount" );

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
	return (new WriteTimeSeriesToDataStream_JDialog ( parent, this )).ok();
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
{	String routine = "WriteDataStream_Command.runCommand", message;
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
	status.clearLog(CommandPhaseType.RUN);

	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String Append = parameters.getValue ( "Append" );
	boolean append = false; // Default
	if ( (Append != null) && Append.equalsIgnoreCase("True") ) {
	    append = true;
	}
	String OutputFileHeader = parameters.getValue ( "OutputFileHeader" );
	String OutputLineFormat = parameters.getValue ( "OutputLineFormat" );
	if ( (OutputLineFormat == null) || (OutputLineFormat.length() == 0) ) {
	    // FIXME SAM 2013-08-10 Need to figure out how to merge TS and label formats or use ${ts:...}
	    OutputLineFormat = "${tsdata:datetime} %{tsdata:value}";
	}
	String OutputLineFormatFile = parameters.getValue ( "OutputLineFormatFile" );
    String DateTimeFormatterType0 = parameters.getValue ( "DateTimeFormatterType" );
    if ( (DateTimeFormatterType0 == null) || DateTimeFormatterType0.equals("") ) {
        DateTimeFormatterType0 = "" + DateTimeFormatterType.C;
    }
    DateTimeFormatterType dateTimeFormatterType = DateTimeFormatterType.valueOfIgnoreCase(DateTimeFormatterType0);
    String DateTimeFormat = parameters.getValue ( "DateTimeFormat" );
	String OutputFileFooter = parameters.getValue ( "OutputFileFooter" );
    String Precision = parameters.getValue ( "Precision" );
    Integer precision = null;
    if ( (Precision != null) && (Precision.length() > 0) ) {
        precision = Integer.parseInt(Precision);
    }
    String MissingValue = parameters.getValue ( "MissingValue" );
    String NonMissingOutputCount = parameters.getValue ( "NonMissingOutputCount" );
    Integer nonMissingOutputCount = null;
    if ( (NonMissingOutputCount != null) && (NonMissingOutputCount.length() > 0) ) {
        nonMissingOutputCount = Integer.parseInt(NonMissingOutputCount);
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
						message, "Check TSList, TSID, EnsembleID parameters." ) );
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
						message, "Check TSList, TSID, EnsembleID parameters." ) );
	}
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

	// Now try to write.

    // Get the comments to add to the top of the file.
    /*
    List<String> OutputComments_Vector = null;
    try {
        Object o = processor.getPropContents ( "OutputComments" );
        // Comments are available so use them...
        if ( o != null ) {
            OutputComments_Vector = (List)o;
            props.setUsingObject("OutputComments",OutputComments_Vector);
        }
    }
    catch ( Exception e ) {
        // Not fatal, but of use to developers.
        message = "Error requesting OutputComments from processor - not using.";
        Message.printDebug(10, routine, message );
    }
    */
    
    String OutputFile_full = OutputFile;
    try {
        // Convert to an absolute path...
        OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
        Message.printStatus ( 2, routine, "Writing DataStream file \"" + OutputFile_full + "\"" );
        List<String> problems = new Vector<String>();
        String outputLineFormat = OutputLineFormat;
        if ( (OutputLineFormatFile != null) && !OutputLineFormatFile.equals("") ) {
            // Read the format from the file
            String formatFileFull = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                    TSCommandProcessorUtil.expandParameterValue(processor,this,OutputLineFormatFile)));
            if ( !IOUtil.fileReadable(formatFileFull) || !IOUtil.fileExists(formatFileFull)) {
                message = "Format file \"" + formatFileFull + "\" is not found or accessible.";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
                status.addToLog(CommandPhaseType.RUN,
                    new CommandLogRecord( CommandStatusType.FAILURE, message,
                        "Verify that the file exists and is readable."));
                throw new CommandException ( message );
            }
            // Expand the format here to deal with processor properties, leaving only time series properties
            outputLineFormat = StringUtil.toString(IOUtil.fileToStringList(formatFileFull), " ");
        }
        // Expand the format here to deal with processor properties, leaving only time series properties
        // This improves performance a bit
        outputLineFormat = TSCommandProcessorUtil.expandParameterValue(processor,this,outputLineFormat);
        // Now write the data records and expand the output line format dynamically for each time series and data value
        writeTimeSeries ( tslist, OutputFile_full, append, OutputFileHeader, outputLineFormat, dateTimeFormatterType, DateTimeFormat,
            OutputFileFooter, precision, MissingValue, OutputStart_DateTime, OutputEnd_DateTime, nonMissingOutputCount,
            problems, processor, status, CommandPhaseType.RUN );
        // Save the output file name...
        setOutputFile ( new File(OutputFile_full));
    }
    catch ( Exception e ) {
        message = "Unexpected error writing time series to data stream file \"" + OutputFile_full + "\" (" + e + ")";
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
    String TSList = parameters.getValue ( "TSList" );
    String TSID = parameters.getValue( "TSID" );
    String EnsembleID = parameters.getValue( "EnsembleID" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String Append = parameters.getValue ( "Append" );
	String OutputFileHeader = parameters.getValue ( "OutputFileHeader" );
	String OutputLineFormat = parameters.getValue ( "OutputLineFormat" );
	String OutputLineFormatFile = parameters.getValue ( "OutputLineFormatFile" );
	String DateTimeFormatterType = parameters.getValue ( "DateTimeFormatterType" );
	String DateTimeFormat = parameters.getValue ( "DateTimeFormat" );
	String OutputFileFooter = parameters.getValue ( "OutputFileFooter" );
	String Precision = parameters.getValue("Precision");
	String MissingValue = parameters.getValue("MissingValue");
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String NonMissingOutputCount = parameters.getValue ( "NonMissingOutputCount" );
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
    if ( (Append != null) && (Append.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Append=" + Append );
    }
    if ( (OutputFileHeader != null) && (OutputFileHeader.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFileHeader=\"" + OutputFileHeader + "\"" );
    }
    if ( (OutputLineFormat != null) && (OutputLineFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputLineFormat=\"" + OutputLineFormat + "\"" );
    }
    if ( (OutputLineFormatFile != null) && (OutputLineFormatFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputLineFormatFile=\"" + OutputLineFormatFile + "\"" );
    }
    if ( (DateTimeFormatterType != null) && (DateTimeFormatterType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DateTimeFormatterType=\"" + DateTimeFormatterType + "\"" );
    }
    if ( (DateTimeFormat != null) && (DateTimeFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DateTimeFormat=\"" + DateTimeFormat + "\"" );
    }
    if ( (OutputFileFooter != null) && (OutputFileFooter.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFileFooter=\"" + OutputFileFooter + "\"" );
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
    if ( (NonMissingOutputCount != null) && (NonMissingOutputCount.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NonMissingOutputCount=" + NonMissingOutputCount );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

/**
Write a time series to the output file.
@param tslist list of time series to write
@param outputFile full path to output file
@param append whether or not to append to the output file
@param outputFileHeader the header content to add at the top of the output file
@param outputLineFormat output line format to be used for each value, using TS % specifiers, similar to graph
data point labeling
@param dateTimeFormatterType formatter type for date/times
@param dateTimeFormat the format to use for date/times, when processed by the date/time formatter
@param outputFileFooter the footer content to add at the bottom of the output file
@param precision precision for output value (default is from data units, or 4)
@param missingValue requested missing value to output, or null to output time series missing value
@param outputStart start for output values
@param output End end for output values
@param nonMissingOutputCount number of values to output.  If negative, count the values from the end.
*/
private void writeTimeSeries ( List<TS> tslist, String outputFile, boolean append,
    String outputFileHeader, String outputLineFormat, DateTimeFormatterType dateTimeFormatterType, String dateTimeFormat,
    String outputFileFooter, Integer precision, String missingValue, DateTime outputStart, DateTime outputEnd,
    Integer nonMissingOutputCount,
    List<String> problems, CommandProcessor processor, CommandStatus status, CommandPhaseType commandPhase )
{   String message;
    PrintWriter fout = null;
    try {
        // Open the file...
        fout = new PrintWriter ( new FileOutputStream ( outputFile, append ) );
        if ( (outputFileHeader != null) && (outputFileHeader.length() > 0) ) {
            fout.println ( outputFileHeader );
        }
        if ( tslist == null ) {
            return;
        }
        String dataLineExpanded;
        if ( precision == null ) {
            precision = 4;
        }
        String valueFormat = "%." + precision + "f";
        String missingValueString = "";
        // Create a DateTimeFormatter to format the data values
        if ( dateTimeFormatterType == null ) {
            dateTimeFormatterType = DateTimeFormatterType.C;
        }
        if ( (dateTimeFormat != null) && dateTimeFormat.equals("") ) {
            // Set to null to simplify checks below
            dateTimeFormat = null;
        }
        for ( TS ts : tslist ) {
            List<String> outputLines = new ArrayList<String>(); // Used when nonMissingOutputCount is used
            // Missing value can be output as a string so check
            if ( (missingValue == null) || missingValue.equals("") ) {
                // Use the time series value
                if ( Double.isNaN(ts.getMissing()) ) {
                    missingValueString = "NaN";
                }
                else {
                    missingValueString = StringUtil.formatString(ts.getMissing(),valueFormat);
                }
            }
            else {
                if ( missingValue.equalsIgnoreCase(_Blank) ) {
                    missingValueString = "";
                }
                else {
                    missingValueString = missingValue;
                }
            }
            // Iterate through data in the time series and output each value according to the format.
            TSIterator it;
            try {
                it = ts.iterator(outputStart, outputEnd);
            }
            catch ( Exception e ) {
                // Most likely a missing time series
                problems.add("Error setting up data iterator (no data?) - skipping time series (" + e + ").");
                continue;
            }
            TSData tsdata = null;
            //String units = ts.getDataUnits();
            double value;
            String valueString, dateTimeString = "";
            CommandStatus cs = null;//status; // Set to null once debugged
            while ( (tsdata = it.next()) != null ) {
                // First expand the line to replace time series properties
                value = tsdata.getDataValue();
                //Message.printStatus(2, "", "Processing " + tsdata.getDate() + " " + value );
                dataLineExpanded = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
                    processor, ts, outputLineFormat, cs, commandPhase ); // Comment status to avoid many messages
                if ( ts.isDataMissing(value) ) {
                    if ( nonMissingOutputCount != null ) {
                        // Skip missing
                        continue;
                    }
                    valueString = missingValueString;
                }
                else {
                    valueString = StringUtil.formatString(value, valueFormat);
                }
                // Do a brute force replace of the ${tsdata:value} and ${tsdata:datetime} properties with the value
                dataLineExpanded = dataLineExpanded.replace("${tsdata:value}", valueString);
                if ( dateTimeFormatterType == DateTimeFormatterType.C ) {
                    if ( dateTimeFormat == null ) {
                        // Just use the default
                        dateTimeString = tsdata.getDate().toString();
                    }
                    else {
                        // Format according to the requested
                        dateTimeString = TimeUtil.formatDateTime(tsdata.getDate(), dateTimeFormat);
                    }
                }
                dataLineExpanded = dataLineExpanded.replace("${tsdata:datetime}", dateTimeString );
                dataLineExpanded = dataLineExpanded.replace("${tsdata:flag}", "" + tsdata.getDataFlag());
                //TSData.toString(outputLineFormat,valueFormat, tsdata.getDate(), value, 0.0, tsdata.getDataFlag().trim(),units);
                if ( nonMissingOutputCount != null ) {
                    outputLines.add(dataLineExpanded);
                }
                else {
                    fout.println(dataLineExpanded);
                }
            }
            if ( (nonMissingOutputCount != null) && (outputLines.size() > 0) ) {
                // Write the output as requested
                int istart = 0;
                if ( nonMissingOutputCount < 0 ) {
                    istart = outputLines.size() + nonMissingOutputCount;
                }
                for ( int i = istart; i < outputLines.size(); i++ ) {
                    fout.println(outputLines.get(i));
                }
            }
        }
        if ( (outputFileFooter != null) && (outputFileFooter.length() > 0) ) {
            fout.println ( outputFileFooter );
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error writing time series to file \"" + outputFile + "\" (" + e + ").";
        problems.add(message);
        throw new RuntimeException ( message );
    }
    finally {
        if ( fout != null ) {
            fout.close();
        }
    }
}

}