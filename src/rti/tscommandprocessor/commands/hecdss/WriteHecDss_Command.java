// WriteHecDss_Command - This class initializes, checks, and runs the WriteHecDss() command.

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

package rti.tscommandprocessor.commands.hecdss;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.TS.TS;
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
This class initializes, checks, and runs the WriteHecDss() command.
*/
public class WriteHecDss_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Values for Type parameter.
*/
protected final String _PerAver = "PER-AVER";
protected final String _PerCum = "PER-CUM";
protected final String _InstVal = "INST-VAL";
protected final String _InstCum = "INST-CUM";

/**
Values for Close parameter.
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
public WriteHecDss_Command ()
{	super();
	setCommandName ( "WriteHecDss" );
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
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String Precision = parameters.getValue ( "Precision" );
	String Type = parameters.getValue ( "Type" );
	String Replace = parameters.getValue ( "Replace" );
	String Close = parameters.getValue ( "Close" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
		message = "The output file must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
			    message, "Specify an output file." ) );
	}
	else if ( OutputFile.indexOf("${") < 0 ) {
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
			status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
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

	if ( (OutputStart != null) && !OutputStart.isEmpty() && (OutputStart.indexOf("${") < 0) ) {
		try {
		    DateTime datetime1 = DateTime.parse(OutputStart);
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
	if ( (OutputEnd != null) && !OutputEnd.equals("") && (OutputEnd.indexOf("${") < 0) ) {
		try {
		    DateTime datetime2 = DateTime.parse(OutputEnd);
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
	if ( (Precision != null) && !Precision.equals("") && !StringUtil.isInteger(Precision) ) {
	    message = "The Precision \"" + Precision + "\" is not a valid integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the precision as an integer." ) );
	}
    if ( (Type == null) || (Type.length() == 0) ) {
        message = "The time series type must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the time series type" ) );
    }
    else if ( !Type.equalsIgnoreCase(_InstCum) && !Type.equalsIgnoreCase(_InstVal) &&
        !Type.equalsIgnoreCase(_PerAver) && !Type.equalsIgnoreCase(_PerCum) ) {
        message = "The time series type is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the time series type as " + _InstCum + ", " + _InstVal + ", " +
                _PerAver + ", or " + _PerCum + "." ) );
    }

    if ( (Replace != null) && !Replace.equals("") && !Type.equalsIgnoreCase(_False) && !Type.equalsIgnoreCase(_True) ) {
        message = "The value of Replace (" + Replace + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the value of Replace as " + _False + " (default) or " + _True + "." ) );
    }

    if ( (Close != null) && !Close.equals("") && !Close.equalsIgnoreCase(_False) && !Close.equalsIgnoreCase(_True) ) {
        message = "The value of Close (" + Close + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the value of Close as " + _False + " (default) or " + _True + "." ) );
    }
    
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(15);
	validList.add ( "OutputFile" );
	validList.add ( "OutputStart" );
	validList.add ( "OutputEnd" );
	validList.add ( "Precision" );
	validList.add ( "Type" );
	validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "A" );
    validList.add ( "B" );
    validList.add ( "C" );
    validList.add ( "E" );
    validList.add ( "F" );
    validList.add ( "Replace" );
    validList.add ( "Close" );
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
	return (new WriteHecDss_JDialog ( parent, this )).ok();
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

// Use the base class parseCommand() method.

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
		Message.printStatus ( 2, routine, "Skipping \"" + toString() + "\" because output is not being created." );
	}
	
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
	CommandStatus status = getCommandStatus();
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
	
    if ( IOUtil.isUNIXMachine() ) {
        message = "The command is not enabled for UNIX/Linux.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
        status.addToLog(CommandPhaseType.RUN, new CommandLogRecord( CommandStatusType.FAILURE, message,
            "Do not use the command on UNIX/Linux"));
        throw new CommandException ( message );
    }
    
    /* TODO smalers 2022-08-11 remove when 64-bit tests out.
    int arch = IOUtil.getJreArchBits();
    if ( arch != 32 ) {
    	message = "Running as " + arch + "-bit environment. The command is only supported on 32-bit Java Runtime Environment (and corresponding 32-bit HEC-DSS libraries).";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
        status.addToLog(CommandPhaseType.RUN, new CommandLogRecord( CommandStatusType.FAILURE, message,
            "Do not use the command on 64 bit systems."));
        throw new CommandException ( message );
    }
    */

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
			MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
		    message, "Report problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
			message, "Report problem to software support." ) );
	}
	@SuppressWarnings("unchecked")
	List<TS> tslist = (List<TS>)o_TSList;
	if ( tslist.size() == 0 ) {
        message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
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
	Message.printStatus ( 2, routine, "Will write HEC-DSS time series for period " + OutputStart_DateTime +
	    " to " + OutputEnd_DateTime );

	// Now try to write.

	PropList props = new PropList ( "WriteHecDss" );
	String Delimiter = parameters.getValue( "Delimiter" );
	if ( (Delimiter != null) && (Delimiter.length() > 0) ) {
	    props.set("Delimiter=" + Delimiter);
	}
	String Precision = parameters.getValue ( "Precision" );
	int Precision_int = -1;    // Default
    if ( (Precision != null) && (Precision.length() > 0) ) {
        props.set("Precision=" + Precision);
        Precision_int = Integer.parseInt(Precision);
    }
    String Type = parameters.getValue ( "Type" );
    String A = parameters.getValue ( "A" );
	if ( (commandPhase == CommandPhaseType.RUN) && (A != null) && (A.indexOf("${") >= 0) ) {
		A = TSCommandProcessorUtil.expandParameterValue(processor, this, A);
	}
    String B = parameters.getValue ( "B" );
	if ( (commandPhase == CommandPhaseType.RUN) && (B != null) && (B.indexOf("${") >= 0) ) {
		B = TSCommandProcessorUtil.expandParameterValue(processor, this, B);
	}
    String C = parameters.getValue ( "C" );
	if ( (commandPhase == CommandPhaseType.RUN) && (C != null) && (C.indexOf("${") >= 0) ) {
		C = TSCommandProcessorUtil.expandParameterValue(processor, this, C);
	}
    String E = parameters.getValue ( "E" );
	if ( (commandPhase == CommandPhaseType.RUN) && (E != null) && (E.indexOf("${") >= 0) ) {
		E = TSCommandProcessorUtil.expandParameterValue(processor, this, E);
	}
    String F = parameters.getValue ( "F" );
	if ( (commandPhase == CommandPhaseType.RUN) && (F != null) && (F.indexOf("${") >= 0) ) {
		F = TSCommandProcessorUtil.expandParameterValue(processor, this, F);
	}
    String Replace = parameters.getValue ( "Replace" );
    boolean Replace_boolean = false; // Default
    if ( (Replace != null) && Replace.equalsIgnoreCase(_True)) {
        Replace_boolean = true;
    }
    String Close = parameters.getValue ( "Close" );
    boolean Close_boolean = false; // Default
    if ( (Close != null) && Close.equalsIgnoreCase(_True)) {
        Close_boolean = true;
    }
    
    // Get the comments to add to the top of the file.

    List<String> OutputComments_Vector = null;
    try { Object o = processor.getPropContents ( "OutputComments" );
        // Comments are available so use them...
        if ( o != null ) {
        	@SuppressWarnings("unchecked")
			List<String> OutputComments_Vector0 = (List<String>)o;
            OutputComments_Vector = OutputComments_Vector0;
            props.setUsingObject("OutputComments",OutputComments_Vector);
        }
    }
    catch ( Exception e ) {
        // Not fatal, but of use to developers.
        message = "Error requesting OutputComments from processor - not using.";
        Message.printDebug(10, routine, message );
    }
    
    if ( (tslist != null) && (tslist.size() > 0) ) {
        String OutputFile_full = OutputFile;
        try {
            // Convert to an absolute path...
            OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                    TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
            Message.printStatus ( 2, routine, "Writing HEC-DSS file \"" + OutputFile_full + "\"" );
            HecDssAPI.writeTimeSeriesList ( new File(OutputFile_full), tslist,
				OutputStart_DateTime, OutputEnd_DateTime, "", Precision_int, Type, A, B, C, E, F, Replace_boolean,
				Close_boolean );
            // Save the output file name...
            setOutputFile ( new File(OutputFile_full));
        }
        catch ( RuntimeException e ) {
            message = "Error writing time series to HEC-DSS file \"" + OutputFile_full + "\" (" + e + ")";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            Message.printWarning ( 3, routine, e );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE, message, "Check log file for details." ) );
            throw new CommandException ( message );
        }
        catch ( Exception e ) {
            message = "Unexpected error writing time series to HEC-DSS file \"" + OutputFile_full + "\" (" + e + ")";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            Message.printWarning ( 3, routine, e );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE, message, "Check log file for details." ) );
            throw new CommandException ( message );
        }
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
		"OutputFile",
		"OutputStart",
		"OutputEnd",
    	"TSList",
    	"TSID",
    	"EnsembleID",
    	"Precision",
    	"Type",
    	"A",
    	"B",
    	"C",
    	"E",
    	"F",
    	"Replace",
    	"Close"
	};
	return this.toString(parameters, parameterOrder);
}

}