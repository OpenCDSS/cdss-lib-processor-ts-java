// ReadStateCUB_Command - This class initializes, checks, and runs the ReadStateCUB() command.

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

package rti.tscommandprocessor.commands.statecu;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.TS.TS;
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
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Time.DateTime;

import DWR.StateCU.StateCU_BTS;

/**
This class initializes, checks, and runs the ReadStateCUB() command.
*/
public class ReadStateCUB_Command extends AbstractCommand implements Command
{

/**
 * Values for OutputVersion parameter.
 */
protected final String _Latest = "Latest";
protected final String _Original = "Original";
protected final String _Version14 = "14";

/**
Constructor.
*/
public ReadStateCUB_Command ()
{	super();
	setCommandName ( "ReadStateCUB" );
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
{	String InputFile = parameters.getValue ( "InputFile" );
	//String TSID = parameters.getValue ( "TSID" );
	String InputStart = parameters.getValue ( "InputStart" );
	String InputEnd = parameters.getValue ( "InputEnd" );
	String OutputVersion = parameters.getValue ( "OutputVersion" );
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
	else if ( InputFile.indexOf("${") < 0  ) {
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
                String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, InputFile) );
                    File f = new File ( adjusted_path );
                    if ( !f.exists() ) {
                        message = "The input file does not exist:  \"" + adjusted_path + "\".";
                        warning += "\n" + message;
                        status.addToLog ( CommandPhaseType.INITIALIZATION,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Verify that the input file exists - may be OK if created at run time." ) );
                    }
            }
            catch ( Exception e ) {
                message = "The input file \"" + InputFile +
                "\" cannot be adjusted using the working directory \"" + working_dir + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that input file and working directory paths are compatible." ) );
            }
    }
	if ( (InputStart != null) && !InputStart.isEmpty() &&
		!InputStart.equalsIgnoreCase("InputStart") &&
		!InputStart.equalsIgnoreCase("InputEnd") && (InputStart.indexOf("${") < 0) ) {
		try {
			DateTime.parse(InputStart);
		}
		catch ( Exception e ) {
            message = "The input start date/time \"" +InputStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, InputStart, InputEnd, or blank to use the global input start." ) );
		}
	}
	if ( (InputEnd != null) && !InputEnd.isEmpty() &&
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

	if ( (OutputVersion != null) && !OutputVersion.isEmpty() &&
		!OutputVersion.equalsIgnoreCase(_Original) &&
		!OutputVersion.equalsIgnoreCase(_Latest) &&
		!OutputVersion.equalsIgnoreCase(_Version14) ) {
        message = "The OutputVersion \"" + OutputVersion + "\" is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as " + _Original + " (default), " + _Latest + ", or " + _Version14 ) );
	}
    
    // Check for invalid parameters...
	List<String> validList = new ArrayList<>(5);
    validList.add ( "InputFile" );
    validList.add ( "TSID" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "OutputVersion" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),
		warning );
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
	return (new ReadStateCUB_JDialog ( parent, this )).ok();
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
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3; // Level for non-user warning messages
	
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String InputFile = parameters.getValue ( "InputFile" ); // Expanded below
	String TSID = parameters.getValue ( "TSID" );
    TSID = TSCommandProcessorUtil.expandParameterValue(processor,this,TSID);
	String InputStart = parameters.getValue ( "InputStart" );
	DateTime InputStart_DateTime = null;
	String InputEnd = parameters.getValue ( "InputEnd" );
	DateTime InputEnd_DateTime = null;
	String OutputVersion = parameters.getValue ( "OutputVersion" );
	if ( (OutputVersion == null) || OutputVersion.isEmpty() ) {
		OutputVersion = _Original; // Default.
	}
	
	// Get the period.
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		try {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", InputStart );
		CommandProcessorRequestResultsBean bean = null;
		try { bean =
			processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting InputStart DateTime(DateTime=" + InputStart + ") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for InputStart DateTime(DateTime=" +
			InputStart + ") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the specified date/time is valid." ) );
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
	else {	// Get from the processor...
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
                            message, "Report the problem to software support." ) );
		}
	}
	
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
			try {
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", InputEnd );
			CommandProcessorRequestResultsBean bean = null;
			try { bean =
				processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
                message = "Error requesting InputEnd DateTime(DateTime=" +
                InputEnd + ") from processor.";
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
                InputEnd +  ") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the end date/time is valid." ) );
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
                                message, "Report the problem to software support." ) );
			}
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
        InputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        Message.printStatus ( 2, routine, "Reading StateCU binary file \"" + InputFile_full + "\"" );

		StateCU_BTS bts = null;
		bts = new StateCU_BTS ( InputFile_full );
		String reqUnits = null;
		boolean readData = true;
		List<TS> tslist = bts.readTimeSeriesList ( TSID, InputStart_DateTime, InputEnd_DateTime, reqUnits, readData, OutputVersion );
		bts.close();
		bts = null;

		// Now add the time series to the end of the normal list...

		if ( tslist != null ) {
			List<TS> TSResultsList_Vector = null;
			try { Object o = processor.getPropContents( "TSResultsList" );
				@SuppressWarnings("unchecked")
				List<TS> TSResultsList_Vector0 = (List<TS>)o;
					TSResultsList_Vector = TSResultsList_Vector0;
			}
			catch ( Exception e ){
				message = "Cannot get time series list to add read time series.  Starting new list.";
				Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(
						command_tag, ++warning_count),
						routine,message);
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
				TSResultsList_Vector = new Vector<TS>();
			}

			// Further process the time series...
			// This makes sure the period is at least as long as the output period...
			int size = tslist.size();
			Message.printStatus ( 2, routine, "Read " + size + " StateCU binary time series." );
			PropList request_params = new PropList ( "" );
			request_params.setUsingObject ( "TSList", tslist );
			try {
				processor.processRequest( "ReadTimeSeries2", request_params);
			}
			catch ( Exception e ) {
				message =
					"Error post-processing StateCU binary time series after read.";
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
				TSResultsList_Vector.add ( tslist.get(i) );
			}
			
			// Now reset the list in the processor...
			if ( TSResultsList_Vector != null ) {
				try {
                    processor.setPropContents ( "TSResultsList", TSResultsList_Vector );
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

		// Free resources from StateCU list...
		tslist = null;
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexepected error reading StateCU binary file \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "See log file for details." ) );
		throw new CommandException ( message );
	}
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String InputFile = props.getValue("InputFile");
	String TSID = props.getValue("TSID");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");
	String OutputVersion = props.getValue("OutputVersion");
	StringBuffer b = new StringBuffer ();
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
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
	if ( (OutputVersion != null) && (OutputVersion.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputVersion=\"" + OutputVersion + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}