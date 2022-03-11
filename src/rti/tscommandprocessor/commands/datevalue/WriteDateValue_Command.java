// WriteDateValue_Command - This class initializes, checks, and runs the WriteDateValue() command.

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

package rti.tscommandprocessor.commands.datevalue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.TS.DateValueTS;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the WriteDateValue() command.
*/
public class WriteDateValue_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Possible values for WriteDataFlagDescriptions parameter.
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
public WriteDateValue_Command ()
{	super();
	setCommandName ( "WriteDateValue" );
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
    String Delimiter = parameters.getValue("Delimiter" );
    String MissingValue = parameters.getValue("MissingValue" );
    String Precision = parameters.getValue ( "Precision" );
    String WriteDataFlagDescriptions = parameters.getValue ( "WriteDataFlagDescriptions" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String IrregularInterval = parameters.getValue ( "IrregularInterval" );
	String Version = parameters.getValue ( "Version" );
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
	
	if ( (Delimiter != null) && !Delimiter.equals("") && !Delimiter.equals(",")) {
        message = "The delimiter \"" + Delimiter + "\" currently must be blank (to indicate space) or a comma.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the delimiter as blank or a comma." ) );
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
    
    if ( (WriteDataFlagDescriptions != null) && !WriteDataFlagDescriptions.isEmpty() &&
    	!WriteDataFlagDescriptions.equals(_False) && !WriteDataFlagDescriptions.equals(_True) ) {
        message = "The WriteDataFlagDescriptions \"" + WriteDataFlagDescriptions + "\" parameter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the whether to write data flag descriptions using " + _False + " (default) or " + _True + "." ) );
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

    if ( (IrregularInterval != null) && !IrregularInterval.equals("") ) {
        try {
            TimeInterval.parseInterval ( IrregularInterval );
        }
        catch ( Exception e ) {
            message = "The irregular time series interval is not valid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
            	new CommandLogRecord(
                    CommandStatusType.FAILURE, message,
                    "Specify a standard interval (e.g., 6Hour, Day, Month)."));
        }
    }
    
    if ( (Version != null) && !Version.equals("1.4") && !Version.equals("1.5") && !Version.equals("1.6") ) {
        message = "The version \"" + Version + "\" is not recognized.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the version as 1.4, 1.5, or 1.6 (default)." ) );
    }

	// Check for invalid parameters...
	List<String> validList = new ArrayList<>(13);
	validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
	validList.add ( "OutputFile" );
	validList.add ( "Delimiter" );
	validList.add ( "Precision" );
	validList.add ( "MissingValue" );
	validList.add ( "IncludeProperties" );
	validList.add ( "WriteDataFlagDescriptions" );
	validList.add ( "OutputStart" );
	validList.add ( "OutputEnd" );
    validList.add ( "IrregularInterval" );
    validList.add ( "Version" );
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
	return (new WriteDateValue_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
	List<File> list = new ArrayList<>();
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
Parse the command string into a PropList of parameters.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	String routine = "WriteDateValue_Command.parseCommand", message;
	int warning_level = 2;
	if ( (command_string.indexOf("=") > 0) || command_string.endsWith("()") ) {
		// New syntax, can be blank parameter list for new command...
		super.parseCommand ( command_string );
	}
	else {	// Parse the old command...
		List<String> tokens = StringUtil.breakStringList ( command_string,"(,)", StringUtil.DELIM_ALLOW_STRINGS );
		if ( tokens.size() != 2 ) {
			message =
			"Invalid syntax for command.  Expecting WriteDateValue(OutputFile).";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
		String OutputFile = ((String)tokens.get(1)).trim();
		// Defaults because not in the old command...
		String TSList = "AllTS";
		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		parameters.set ( "TSList", TSList );
		if ( OutputFile.length() > 0 ) {
			parameters.set ( "OutputFile", OutputFile );
		}
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
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
	String OutputFile = parameters.getValue ( "OutputFile" );
	String IrregularInterval = parameters.getValue ( "IrregularInterval" );
    TimeInterval irregularInterval = null;
    try {
        irregularInterval = TimeInterval.parseInterval ( IrregularInterval );
    }
    catch ( Exception e ) {
        // Will have been checked previously
    }
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

	// Now try to write.  Only do so if the number of time series is 1+.  Otherwise an exception will occur.
    // TODO SAM 2007-11-19 Evaluate whether DateValueTS.writeTimeSeriesList() should allow empty list,
    // resulting in just a header in the output.  This might be useful during testing

	PropList props = new PropList ( "WriteDateValue" );
	String Delimiter = parameters.getValue( "Delimiter" );
	if ( (Delimiter != null) && (Delimiter.length() > 0) ) {
	    props.set("Delimiter=" + Delimiter);
	}
	String Precision = parameters.getValue ( "Precision" );
    if ( (Precision != null) && (Precision.length() > 0) ) {
        props.set("Precision=" + Precision);
    }
    String MissingValue = parameters.getValue ( "MissingValue" );
    if ( (MissingValue != null) && (MissingValue.length() > 0) ) {
        props.set("MissingValue=" + MissingValue);
    }
    String IncludeProperties = parameters.getValue ( "IncludeProperties" );
    String [] includeProperties = new String[0];
    if ( (IncludeProperties != null) && !IncludeProperties.isEmpty() ) {
    	includeProperties = IncludeProperties.split(",");
    	for ( int i = 0; i < includeProperties.length; i++ ) {
    		includeProperties[i] = includeProperties[i].trim();
    	}
    	props.setUsingObject("IncludeProperties",includeProperties);
    }
    String WriteDataFlagDescriptions = parameters.getValue ( "WriteDataFlagDescriptions" );
    boolean writeDataFlagDescriptions = false; // default
    if ( WriteDataFlagDescriptions != null ) {
    	if ( WriteDataFlagDescriptions.equalsIgnoreCase(_True)) {
    		writeDataFlagDescriptions = true;
    	}
    }
    // Always set the property so default for command is enforced
	props.set("WriteDataFlagDescriptions="+writeDataFlagDescriptions);
    if ( (Version != null) && (Version.length() > 0) ) {
        props.set("Version=" + Version);
    }
    
    // Get the comments to add to the top of the file.

    List<String> OutputComments_List = null;
    try {
        Object o = processor.getPropContents ( "OutputComments" );
        // Comments are available so use them...
        if ( o != null ) {
        	@SuppressWarnings("unchecked")
			List<String> OutputComments_List0 = (List<String>)o;
            OutputComments_List = OutputComments_List0;
            props.setUsingObject("OutputComments",OutputComments_List);
        }
    }
    catch ( Exception e ) {
        // Not fatal, but of use to developers.
        message = "Error requesting OutputComments from processor - not using.";
        Message.printDebug(10, routine, message );
    }
    
    if ( irregularInterval != null ) {
        props.setUsingObject("IrregularInterval",irregularInterval);
    }
    
    // Write the time series file even if no time series are available.  This is useful for
    // troubleshooting and testing (in cases where no time series are available.
    //if ( (tslist != null) && (tslist.size() > 0) ) {
        String OutputFile_full = OutputFile;
        try {
            // Convert to an absolute path...
            OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                    TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
            Message.printStatus ( 2, routine, "Writing DateValue file \"" + OutputFile_full + "\"" );
            DateValueTS.writeTimeSeriesList ( tslist, OutputFile_full,
				OutputStart_DateTime, OutputEnd_DateTime, "", true, props );
            // Save the output file name...
            setOutputFile ( new File(OutputFile_full));
        }
        catch ( Exception e ) {
            message = "Unexpected error writing time series to DateValue file \"" + OutputFile_full + "\" (" + e + ")";
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
	String Delimiter = parameters.getValue ( "Delimiter" );
	String Precision = parameters.getValue("Precision");
	String MissingValue = parameters.getValue("MissingValue");
	String IncludeProperties = parameters.getValue("IncludeProperties");
	String WriteDataFlagDescriptions = parameters.getValue("WriteDataFlagDescriptions");
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
    String IrregularInterval = parameters.getValue( "IrregularInterval" );
    String Version = parameters.getValue( "Version" );
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
    if ( (IncludeProperties != null) && (IncludeProperties.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IncludeProperties=\"" + IncludeProperties + "\"");
    }
    if ( (WriteDataFlagDescriptions != null) && (WriteDataFlagDescriptions.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WriteDataFlagDescriptions=" + WriteDataFlagDescriptions);
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
    if ( (IrregularInterval != null) && (IrregularInterval.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IrregularInterval=" + IrregularInterval );
    }
    if ( (Version != null) && (Version.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Version=" + Version );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
