package rti.tscommandprocessor.commands.datastream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
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
   
    if ( (OutputLineFormat == null) || (OutputLineFormat.length() == 0) ) {
        message = "The output line format must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify an output line format." ) );
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
	List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
	valid_Vector.add ( "OutputFile" );
	valid_Vector.add ( "Append" );
    valid_Vector.add ( "OutputFileHeader" );
    valid_Vector.add ( "OutputLineFormat" );
	valid_Vector.add ( "OutputFileFooter" );
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
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "WriteDateValue_Command.runCommand", message;
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
	boolean append = true;
	if ( (Append != null) && Append.equalsIgnoreCase("False") ) {
	    append = false;
	}
	String OutputFileHeader = parameters.getValue ( "OutputFileHeader" );
	String OutputLineFormat = parameters.getValue ( "OutputLineFormat" );
	if ( (OutputLineFormat == null) || (OutputLineFormat.length() == 0) ) {
	    // FIXME SAM 2013-08-10 Need to figure out how to merge TS and label formats or use ${ts:...}
	    OutputLineFormat = "%v";
	}
	String OutputFileFooter = parameters.getValue ( "OutputFileFooter" );
    String Precision = parameters.getValue ( "Precision" );
    Integer precision = null;
    if ( (Precision != null) && (Precision.length() > 0) ) {
        precision = Integer.parseInt(Precision);
    }
    String MissingValue = parameters.getValue ( "MissingValue" );
    Double missingValue = null;
    if ( (MissingValue != null) && (MissingValue.length() > 0) ) {
        missingValue = Double.parseDouble(MissingValue);
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
        Message.printStatus ( 2, routine, "Writing DateValue file \"" + OutputFile_full + "\"" );
        List<String> problems = new Vector<String>();
        writeTimeSeries ( tslist, OutputFile_full, append, OutputFileHeader, OutputLineFormat, OutputFileFooter,
			precision, missingValue, OutputStart_DateTime, OutputEnd_DateTime, problems );
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
	String OutputFileFooter = parameters.getValue ( "OutputFileFooter" );
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
@param outputFileFooter the footer content to add at the bottom of the output file
@param precision precision for output value (default is from data units, or 4)
@param missingValue missing value to output
@param outputStart start for output values
@param output End end for output values
*/
private void writeTimeSeries ( List<TS> tslist, String outputFile, boolean append,
    String outputFileHeader, String outputLineFormat, String outputFileFooter,
    Integer precision, Double missingValue, DateTime outputStart, DateTime outputEnd, List<String> problems )
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
        for ( TS ts : tslist ) {
            // Iterate through data in the time series and output each value according to the format.
            TSIterator it = ts.iterator(outputStart, outputEnd);
            TSData tsdata = null;
            String units = ts.getDataUnits();
            String valueFormat = "%.4f";
            double value;
            while ( (tsdata = it.next()) != null ) {
                value = tsdata.getDataValue();
                fout.println(TSData.toString(outputLineFormat,valueFormat, tsdata.getDate(), value, 0.0,
                    tsdata.getDataFlag().trim(),units));
            }
        }
        if ( (outputFileFooter != null) && (outputFileFooter.length() > 0) ) {
            fout.println ( outputFileFooter );
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error writing property to file \"" + outputFile + "\" (" + e + ").";
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