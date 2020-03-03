// WriteDelimitedFile_Command - This class initializes, checks, and runs the WriteDelimitedFile() command.

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

package rti.tscommandprocessor.commands.delimited;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.TS.IrregularTS;
import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSIterator;
import RTi.TS.TSLimits;
import RTi.TS.TSUtil;
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
import RTi.Util.Time.InvalidTimeIntervalException;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;

/**
This class initializes, checks, and runs the WriteDelimitedFile() command.
*/
public class WriteDelimitedFile_Command extends AbstractCommand implements Command, FileGenerator
{
    
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
public WriteDelimitedFile_Command ()
{	super();
	setCommandName ( "WriteDelimitedFile" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String OutputFile = parameters.getValue ( "OutputFile" );
    String dateTimeFormatterType = parameters.getValue ( "DateTimeFormatterType" );
    String HeadingSurround = parameters.getValue("HeadingSurround" );
    String Precision = parameters.getValue ( "Precision" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String warning = "";
	String routine = getClass().getSimpleName() + ".checkCommandParameters";
	String message;

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	if ( (OutputFile == null) || OutputFile.isEmpty() ) {
		message = "The output file: \"" + OutputFile + "\" must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
			message, "Specify an output file." ) );
	}
	else if ( OutputFile.indexOf("${") < 0 ) {
		// Can only check when ${Property} is not used
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

    if ( (HeadingSurround != null) && !HeadingSurround.equals("") && HeadingSurround.equals("\"")) {
        message = "The heading surround character (" + HeadingSurround + ") must be specified as \\\".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the double quote as \\\"." ) );
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
    
	if ( (OutputStart != null) && !OutputStart.isEmpty() && !OutputStart.startsWith("${") ) {
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
	if ( (OutputEnd != null) && !OutputEnd.isEmpty() && !OutputEnd.startsWith("${") ) {
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
	ArrayList<String> validList = new ArrayList<>(15);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
	validList.add ( "OutputFile" );
	validList.add ( "DateTimeColumn" );
	validList.add ( "DateTimeFormatterType" );
	validList.add ( "DateTimeFormat" );
	validList.add ( "ValueColumns" );
	validList.add ( "HeadingSurround" );
	validList.add ( "Delimiter" );
	validList.add ( "Precision" );
	validList.add ( "MissingValue" );
	validList.add ( "OutputStart" );
	validList.add ( "OutputEnd" );
	validList.add ( "HeaderComments" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new WriteDelimitedFile_JDialog ( parent, this )).ok();
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
		status.clearLog(CommandPhaseType.RUN);
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
	String DateTimeColumn = parameters.getValue ( "DateTimeColumn" );
    String DateTimeFormatterType0 = parameters.getValue ( "DateTimeFormatterType" );
    if ( (DateTimeFormatterType0 == null) || DateTimeFormatterType0.equals("") ) {
        DateTimeFormatterType0 = "" + DateTimeFormatterType.C;
    }
    DateTimeFormatterType dateTimeFormatterType = DateTimeFormatterType.valueOfIgnoreCase(DateTimeFormatterType0);
    String DateTimeFormat = parameters.getValue ( "DateTimeFormat" );
    String ValueColumns = parameters.getValue ( "ValueColumns" );
    String HeadingSurround = parameters.getValue ( "HeadingSurround" );
    if ( HeadingSurround == null ) {
        HeadingSurround = "";
    }
    else {
        // Swap special strings for internal characters
        HeadingSurround = HeadingSurround.replace ( "\\\"", "\"" );
    }
    String Delimiter = parameters.getValue ( "Delimiter" );
    if ( (Delimiter == null) || Delimiter.equals("") ) {
        Delimiter = ","; // default
    }
    else {
        // Swap special strings for internal characters
        Delimiter = Delimiter.replace ( "\\s", " " );
        Delimiter = Delimiter.replace ( "\\t", "\t" );
    }
    String Precision = parameters.getValue ( "Precision" );
    Integer precision = 4; // default
    if ( (Precision != null) && !Precision.equals("") ) {
        precision = Integer.parseInt(Precision);
    }
    String MissingValue = parameters.getValue ( "MissingValue" );
    if ( (MissingValue != null) && MissingValue.equals("") ) {
        // Set to null to indicate default internal value should be used
        MissingValue = null;
    }
    String HeaderComments = parameters.getValue ( "HeaderComments" );
    List<String> headerComments = new ArrayList<>();
    if ( HeaderComments != null ) {
    	if ( (HeaderComments != null) && (HeaderComments.indexOf("${") >= 0) ) {
		   	HeaderComments = TSCommandProcessorUtil.expandParameterValue(processor, this, HeaderComments);
	   	}
    	// Expand \\n to actual newlines and \\" to quote
    	HeaderComments = HeaderComments.replace("\\n", "\n").replace("\\\"", "\"");
    	headerComments = StringUtil.breakStringList(HeaderComments, "\n", 0);
    	// Make sure that comments have # at front
    	String comment;
    	for ( int i = 0; i < headerComments.size(); i++ ) {
    		comment = headerComments.get(i);
    		if ( ! comment.startsWith("#") ) {
    			comment = "# " + comment;
    			headerComments.set(i,comment);
    		}
    	}
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

	String OutputStart = parameters.getValue ( "OutputStart" );
	if ( (OutputStart == null) || OutputStart.isEmpty() ) {
		OutputStart = "${OutputStart}"; // Default
	}
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	if ( (OutputEnd == null) || OutputEnd.isEmpty() ) {
		OutputEnd = "${OutputEnd}"; // Default
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

    // TODO SAM 2013-10-22 Get the comments to add to the top of the file.
	/*
    List<String> OutputComments_Vector = null;
    try {
        Object o = processor.getPropContents ( "OutputComments" );
    }
    catch ( Exception e ) {
        // Not fatal, but of use to developers.
        message = "Error requesting OutputComments from processor - not using.";
        Message.printDebug(10, routine, message );
    }
    */
    
    // Write the time series file even if no time series are available.  This is useful for
    // troubleshooting and testing (in cases where no time series are available.
    //if ( (tslist != null) && (tslist.size() > 0) ) {
        String OutputFile_full = OutputFile;
        try {
            // Convert to an absolute path...
            OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                    TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
            Message.printStatus ( 2, routine, "Writing DelimitedFile file \"" + OutputFile_full + "\"" );
            List<String> problems = new ArrayList<String>();
            writeTimeSeries ( tslist, OutputFile_full, DateTimeColumn, dateTimeFormatterType, DateTimeFormat, ValueColumns,
                HeadingSurround, Delimiter, precision, MissingValue, OutputStart_DateTime, OutputEnd_DateTime,
                headerComments,
                problems, processor, status, CommandPhaseType.RUN );
            // Save the output file name...
            setOutputFile ( new File(OutputFile_full));
        }
        catch ( Exception e ) {
            message = "Unexpected error writing time series to delimited file \"" + OutputFile_full + "\" (" + e + ")";
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
	String DateTimeColumn = parameters.getValue ( "DateTimeColumn" );
    String DateTimeFormatterType = parameters.getValue ( "DateTimeFormatterType" );
    String DateTimeFormat = parameters.getValue ( "DateTimeFormat" );
	String ValueColumns = parameters.getValue ( "ValueColumns" );
	String HeadingSurround = parameters.getValue ( "HeadingSurround" );
	String Delimiter = parameters.getValue ( "Delimiter" );
	String Precision = parameters.getValue("Precision");
	String MissingValue = parameters.getValue("MissingValue");
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String HeaderComments = parameters.getValue ( "HeaderComments" );
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
    if ( (DateTimeColumn != null) && (DateTimeColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DateTimeColumn=\"" + DateTimeColumn + "\"" );
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
    if ( (ValueColumns != null) && (ValueColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ValueColumns=\"" + ValueColumns + "\"" );
    }
    if ( (HeadingSurround != null) && (HeadingSurround.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "HeadingSurround=\"" + HeadingSurround + "\"" );
    }
    if ( (Delimiter != null) && (Delimiter.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Delimiter=\"" + Delimiter + "\"" );
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
	if ( (HeaderComments != null) && (HeaderComments.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "HeaderComments=\"" + HeaderComments + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

/**
Write a time series to the output file.
@param tslist list of time series to write
@param outputFile full path to output file
@param dateTimeColumn name of column for date/time
@param dateTimeFormatterType formatter type for date/times
@param dateTimeFormat the format to use for date/times, when processed by the date/time formatter
@param valueColumns name(s) of column(s) for time series values using %L, ${ts:property}, ${property}
@param headingSurround character to surround column headings
@param delim delimiter between columns
@param precision precision for output value (default is from data units, or 4)
@param missingValue requested missing value to output, or null to output time series missing value
@param outputStart start for output values
@param output End end for output values
@param headerComments list of strings for file header, should already have '#' at front.
*/
private void writeTimeSeries ( List<TS> tslist, String outputFile, String dateTimeColumn,
    DateTimeFormatterType dateTimeFormatterType, String dateTimeFormat, String valueColumns,
    String headingSurround, String delim,
    Integer precision, String missingValue, DateTime outputStart, DateTime outputEnd,
    List<String> headerComments,
    List<String> problems, CommandProcessor processor, CommandStatus cs, CommandPhaseType commandPhase )
{   String message;
	String routine = getClass().getSimpleName() + ".writeTimeSeries";
    PrintWriter fout = null;
    // Make sure the time series have the same interval
    try {
        // Open the file...
        fout = new PrintWriter ( new FileOutputStream ( outputFile ) );
        if ( (tslist == null) || (tslist.size() == 0) ) {
            return;
        }
        // Set up for writing time series data
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
        // Loop through the specified period or if not specified the full overlapping period
        if ( (outputStart == null) || (outputEnd == null) ) {
            TSLimits limits = TSUtil.getPeriodFromTS(tslist, TSUtil.MAX_POR);
            if ( outputStart == null ) {
                outputStart = limits.getDate1();
            }
            if ( outputEnd == null ) {
                outputEnd = limits.getDate2();
            }
        }
        if ( !TSUtil.areIntervalsSame(tslist) ) {
            throw new InvalidTimeIntervalException("Time series time intervals are not the same.  Cannot write file.");
        }
        boolean isRegular = true; // All time series are matching regular interval
        // TODO SAM 2013-10-22 For now only support writing irregular data for a single time series
        if ( !TimeInterval.isRegularInterval(tslist.get(0).getDataIntervalBase()) ) {
        	// This will be the case if 1+ time series all have irregular interval
        	if ( tslist.size() > 1 ) {
        		throw new InvalidTimeIntervalException("Can only write a single irregular time series.  Cannot write file.");
        	}
        	else {
        		// Have one time series so allow it to be written below as irregular
        		isRegular = false;
        	}
        }
        int intervalBase = -1;
        int intervalMult = -1;
        String [] missingValueStrings = new String[tslist.size()];
        int its = -1;
        for ( TS ts : tslist ) {
            ++its;
            if ( ts != null ) {
                intervalBase = ts.getDataIntervalBase();
                intervalMult = ts.getDataIntervalMult();
            }
            // Missing value can be output as a string so check
            if ( (missingValue == null) || missingValue.equals("") ) {
                // Use the time series value
                if ( Double.isNaN(ts.getMissing()) ) {
                    missingValueStrings[its] = "NaN";
                }
                else {
                    missingValueStrings[its] = StringUtil.formatString(ts.getMissing(),valueFormat);
                }
            }
            else {
                if ( missingValue.equalsIgnoreCase(_Blank) ) {
                    missingValueStrings[its] = "";
                }
                else {
                    missingValueStrings[its] = missingValue;
                }
            }
        }
        // Output the file header
        // - currently does not output the standard header like DateValue but may add this
        if ( headerComments.size() > 0 ) {
        	for ( String headerComment : headerComments ) {
        		fout.println(headerComment);
        	}
        }
        // Output the column headings
        if ( headingSurround == null ) {
            headingSurround = "";
        }
        fout.print(headingSurround);
        if ( (dateTimeColumn == null) || dateTimeColumn.equals("") ) {
            if ( intervalBase >= TimeInterval.DAY ) {
                dateTimeColumn = "Date";
            }
            else {
                dateTimeColumn = "DateTime";
            }
        }
        fout.print(dateTimeColumn);
        fout.print(headingSurround);
        if ( (valueColumns == null) || valueColumns.equals("") ) {
            valueColumns = "%L_%T";
        }
        for ( TS ts : tslist ) {
            fout.print(delim + headingSurround);
            if ( ts != null ) {
                String heading = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
                    processor, ts, valueColumns, cs, commandPhase );
                // Make sure the heading does not include the surround character
                if ( headingSurround.length() != 0 ) {
                    heading = heading.replace(headingSurround,"");
                }
                fout.print(heading);
            }
            fout.print(headingSurround);
        }
        fout.println();
        // Loop through date/time corresponding to each row in the output file
        double value;
        String valueString, dateTimeString = "";
        if ( isRegular ) {
        	// Have regular interval data with matching intervals
        	// Iterate using DateTime increment and the request data from time series
	        for ( DateTime date = new DateTime(outputStart); date.lessThanOrEqualTo(outputEnd); date.addInterval(intervalBase, intervalMult)) {
	            // Output the date/time as per the format
	            if ( dateTimeFormatterType == DateTimeFormatterType.C ) {
	                if ( dateTimeFormat == null ) {
	                    // Just use the default
	                    dateTimeString = date.toString();
	                }
	                else {
	                    // Format according to the requested
	                    dateTimeString = TimeUtil.formatDateTime(date, dateTimeFormat);
	                }
	                if ( delim.equals(" ") ) {
	                    // The dateTimeString might contain a space between date and time so replace
	                    dateTimeString.replace(" ","T");
	                }
	            }
	            fout.print(dateTimeString);
	            // Loop through the time series list and output each value
	            its = -1;
	            for ( TS ts : tslist ) {
	                // Iterate through data in the time series and output each value according to the format.
	                ++its;
	                TSData tsdata = new TSData();
	                tsdata = ts.getDataPoint(date, tsdata);
	                // First expand the line to replace time series properties
	                value = tsdata.getDataValue();
	                if ( ts.isDataMissing(value) ) {
	                    valueString = missingValueString;
	                }
	                else {
	                    valueString = StringUtil.formatString(value, valueFormat);
	                }
	                fout.print(delim + valueString);
	            }
	            fout.println();
	        }
        }
        else {
        	// Single irregular interval time series
        	IrregularTS ts = (IrregularTS)tslist.get(0);
        	// Find the nearest date
        	DateTime iteratorStart = null, iteratorEnd = null;
        	if ( outputStart == null ) {
        		iteratorStart = new DateTime(ts.getDate1());
        	}
        	else {
            	TSData tsdata = ts.findNearestNext(outputStart, null, null, true);
            	if ( tsdata == null ) {
            		iteratorStart = new DateTime(ts.getDate1());
            	}
            	else {
            		iteratorStart = new DateTime(tsdata.getDate());
            	}
        	}
        	if ( outputEnd == null ) {
        		iteratorEnd = new DateTime(ts.getDate2());
        	}
        	else {
            	TSData tsdata = ts.findNearestNext(outputEnd, null, null, true);
            	if ( tsdata == null ) {
            		iteratorEnd = new DateTime(ts.getDate2());
            	}
            	else {
            		iteratorEnd = new DateTime(tsdata.getDate());
            	}
        	}
        	TSIterator tsi = ts.iterator(iteratorStart, iteratorEnd);
        	TSData tsdata = null;
        	DateTime date;
	        while ( (tsdata = tsi.next()) != null ) {
	            // Output the date/time as per the format
	        	date = tsdata.getDate();
	            if ( dateTimeFormatterType == DateTimeFormatterType.C ) {
	                if ( dateTimeFormat == null ) {
	                    // Just use the default
	                    dateTimeString = date.toString();
	                }
	                else {
	                    // Format according to the requested
	                    dateTimeString = TimeUtil.formatDateTime(date, dateTimeFormat);
	                }
	                if ( delim.equals(" ") ) {
	                    // The dateTimeString might contain a space between date and time so replace
	                    dateTimeString.replace(" ","T");
	                }
	            }
	            fout.print(dateTimeString);
                // First expand the line to replace time series properties
                value = tsdata.getDataValue();
                if ( ts.isDataMissing(value) ) {
                    valueString = missingValueString;
                }
                else {
                    valueString = StringUtil.formatString(value, valueFormat);
                }
                fout.print(delim + valueString);
	            fout.println();
	        }
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error writing time series to file \"" + outputFile + "\" (" + e + ").";
        Message.printWarning(3, routine, e);
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