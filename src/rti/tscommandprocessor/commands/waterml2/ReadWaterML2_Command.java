// ReadWaterML2_Command - This class initializes, checks, and runs the ReadWaterML() commands.

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

package rti.tscommandprocessor.commands.waterml2;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.TSUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the ReadWaterML() commands.
*/
public class ReadWaterML2_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Possible values for RequireDataToMatchInterval parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Possible values for ReadMethod parameter.
*/
protected final String _API = "API";
protected final String _ParseDOM = "ParseDOM";

/**
Private data members shared between the checkCommandParameter() and the 
runCommand() methods (prevent code duplication parsing dateTime strings).  
*/
private DateTime __InputStart = null;
private DateTime __InputEnd = null;

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public ReadWaterML2_Command ()
{
	super();
	setCommandName ( "ReadWaterML2" );
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
{
    String routine = getClass().getSimpleName() + ".checkCommandParameters";
	String warning = "";
    String message;
    
    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Get the property values. 
	String InputFile = parameters.getValue("InputFile");
	String ReadMethod = parameters.getValue("ReadMethod");
	//String NewUnits = parameters.getValue("NewUnits");
	String InputStart = parameters.getValue("InputStart");
	String InputEnd = parameters.getValue("InputEnd");
	String Alias = parameters.getValue("Alias");
	String Interval = parameters.getValue( "Interval" );
	String RequireDataToMatchInterval = parameters.getValue( "RequireDataToMatchInterval" );
    
    if (Alias != null && !Alias.equals("")) {
        if (Alias.indexOf(" ") > -1) {
            // do not allow spaces in the alias
            message = "The Alias value cannot contain any spaces.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Remove spaces from the alias." ) );
        }
    }

    if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify an existing input file." ) );
    }
    else if ( InputFile.indexOf("${") < 0 ) {
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
                // Working directory is available so use it...
                if ( o != null ) {
                    working_dir = (String)o;
                }
            }
            catch ( Exception e ) {
                message = "Error requesting WorkingDir from processor.";
                warning += "\n" + message;
                Message.printWarning(3, routine, message );
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an existing input file." ) );
            }
    
        try {
            //String adjusted_path = 
            IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        }
        catch ( Exception e ) {
            message = "The input file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that input file and working directory paths are compatible." ) );
        }
    }
    
    if ( (ReadMethod != null) && !ReadMethod.isEmpty() && !ReadMethod.equalsIgnoreCase(_API) && 
        !ReadMethod.equalsIgnoreCase(_ParseDOM) ) {
        message = "ReadMethod ( " + ReadMethod + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "ReadMethod must be " + _API + " (default) or " + _ParseDOM + "." ) );
    }

	// InputStart
	if ((InputStart != null) && !InputStart.equals("") && (InputStart.indexOf("${") < 0) ) {
		try {
			__InputStart = DateTime.parse(InputStart);
		} 
		catch (Exception e) {
            message = "The input start date/time \"" + InputStart + "\" is not valid.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid input start." ) );
		}
	}

	// InputEnd
	if ((InputEnd != null) && !InputEnd.equals("") && (InputEnd.indexOf("${") < 0) ) {
		try {
			__InputEnd = DateTime.parse(InputEnd);
		} 
		catch (Exception e) {
            message = "The input end date/time \"" + InputEnd + "\" is not valid.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid input end." ) );
		}
	}

	// Make sure __InputStart precedes __InputEnd
	if ( __InputStart != null && __InputEnd != null ) {
		if ( __InputStart.greaterThanOrEqualTo( __InputEnd ) ) {
            message = InputStart + " (" + __InputStart  + ") should be less than InputEnd (" + __InputEnd + ").";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an input start less than the input end." ) );
		}
	}

    if ( Interval == null || (Interval.length() == 0) ) {
        message = "The data interval must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a data interval."));
    }
    else {
        try {
            TimeInterval.parseInterval(Interval);
        }
        catch ( Exception e ) {
            // Should not happen because choices are valid
            message = "The data interval \"" + Interval + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a data interval using the command editor."));
        }
    }

    if ( (RequireDataToMatchInterval != null) && !RequireDataToMatchInterval.isEmpty() &&
    	!RequireDataToMatchInterval.equalsIgnoreCase(_True) && !RequireDataToMatchInterval.equalsIgnoreCase(_False) ) {
        message = "RequireDataToMatchInterval (" + RequireDataToMatchInterval + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "RequireDataToMatchInterval must be " + _False + " or " + _True + " (default)." ) );
    }
    
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(8);
    validList.add ( "Alias" );
    validList.add ( "InputFile" );
    validList.add ( "ReadMethod" );
    validList.add ( "Interval" );
    validList.add ( "RequireDataToMatchInterval" );
    validList.add ( "OutputTimeZoneOffset" );
    validList.add ( "OutputTimeZone" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    //valid_Vector.add ( "NewUnits" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, warning_level ),
			warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	
	// The command will be modified if changed...
	return ( new ReadWaterML2_JDialog ( parent, this ) ).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
	List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    // First time series in list should be of a type that be requested (e.g., MonthTS)
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discoveryTSList;
    }
    else {
        return null;
    }
}

// Use parent class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number The number of the command being run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInteral", message;
	int warning_level = 2;
    int log_level = 3;
	String command_tag = "" + command_number;
	int warning_count = 0;
	    
    // Get and clear the status and clear the run log...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }
    CommandProcessor processor = getCommandProcessor();

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile");
	String ReadMethod = parameters.getValue("ReadMethod");
	boolean readUsingApi = false; // default
	if ( (ReadMethod != null) && ReadMethod.equalsIgnoreCase(_API) ) {
		readUsingApi = true;
	}
	//String NewUnits = parameters.getValue("NewUnits");
	String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}"; // Global default
	}
	String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}"; // Global default
	}
	String Alias = parameters.getValue("Alias");
    // The following is necessary because WaterML 2 does not appear to have a clear indicator of
    // the time series data interval
	String Interval = parameters.getValue("Interval");
    TimeInterval interval = null;
    if ( (Interval != null) && !Interval.equals("") ) {
        try {
            interval = TimeInterval.parseInterval(Interval);
        }
        catch ( Exception e ) {
            // Should not happen because checked previously
        }
    }
    String RequireDataToMatchInterval = parameters.getValue("RequireDataToMatchInterval");
    boolean RequireDataToMatchInterval_boolean = true; // default
    if ( (RequireDataToMatchInterval != null) && RequireDataToMatchInterval.equalsIgnoreCase(_False) ) {
        RequireDataToMatchInterval_boolean = false;
    }
    String OutputTimeZoneOffset = parameters.getValue("OutputTimeZoneOffset");
    if ( (OutputTimeZoneOffset != null) && !OutputTimeZoneOffset.isEmpty() && (commandPhase == CommandPhaseType.RUN) && OutputTimeZoneOffset.indexOf("${") >= 0 ) {
    	OutputTimeZoneOffset = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputTimeZoneOffset);
    }
    String OutputTimeZone = parameters.getValue("OutputTimeZone");
    if ( (OutputTimeZone != null) && !OutputTimeZone.isEmpty() && (commandPhase == CommandPhaseType.RUN) && OutputTimeZone.indexOf("${") >= 0 ) {
    	OutputTimeZone = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputTimeZone);
    }
    
    if ( OutputTimeZone != null ) {
    	if ( OutputTimeZone.equals("\\b") ) {
    		OutputTimeZone = "";
    	}
    	else if ( OutputTimeZone.equals("") ) {
    		OutputTimeZone = null; // Have to use \b parameter value to indicate blank to avoid confusion
    	}
    }
    
	DateTime InputStart_DateTime = null;
	DateTime InputEnd_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			InputStart_DateTime = TSCommandProcessorUtil.getDateTime ( InputStart, "InputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			InputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( InputEnd, "InputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
	}
	if ( (OutputTimeZoneOffset != null) && !OutputTimeZoneOffset.isEmpty() ) {
		if ( InputStart_DateTime != null ) {
			if ( (OutputTimeZoneOffset != null) && !OutputTimeZoneOffset.isEmpty() ) {
				InputStart_DateTime.shiftTimeZone(OutputTimeZoneOffset);
			}
			if ( (OutputTimeZone != null) && !OutputTimeZone.isEmpty() ) {
				InputStart_DateTime.setTimeZone(OutputTimeZone);
			}
		}
		if ( InputEnd_DateTime != null ) {
			if ( (OutputTimeZoneOffset != null) && !OutputTimeZoneOffset.isEmpty() ) {
				InputEnd_DateTime.shiftTimeZone(OutputTimeZoneOffset);
			}
			if ( (OutputTimeZone != null) && !OutputTimeZone.isEmpty() ) {
				InputEnd_DateTime.setTimeZone(OutputTimeZone);
			}
		}
	}

	// Read the file.
    List<TS> tslist = null;   // Keep the list of time series
    String InputFile_full = InputFile;
	try {
        boolean readData = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            readData = false;
        }
        if ( InputFile.toUpperCase().startsWith("HTTP:") ) {
        	// Reading from a URL
        	// TODO smalers 2017-07-01 enable later.  For now file must have been retrieved
        }
        else {
        	// Reading from a file
	        InputFile_full = IOUtil.verifyPathForOS(
	            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
	        if ( !IOUtil.fileExists(InputFile_full) ) {
	            message = "Input file does not exist:  \"" + InputFile_full + "\".";
	            Message.printWarning(log_level,
	                MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                routine, message );
	            status.addToLog ( commandPhase,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Verify that filename is correct and that the file exists." ) );
	        }
	        else {
	            WaterML2Reader watermlReader = new WaterML2Reader ( new File(InputFile_full) );
	            Message.printStatus(2,routine, "Reading WaterML 2 file \"" + InputFile_full + "\"");
	            tslist = watermlReader.readTimeSeriesList( readUsingApi, interval, RequireDataToMatchInterval_boolean,
	            	OutputTimeZoneOffset, OutputTimeZone,
	            	InputStart_DateTime, InputEnd_DateTime, readData );
	            List<String> warningMessages = watermlReader.getWarningMessages();
	            for ( String warningMessage: warningMessages ) {
	                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
	                	warningMessage, "Check WaterML 2 file contents and command parameters." ) );
	            }
	            List<String> failureMessages = watermlReader.getFailureMessages();
	            for ( String failureMessage: failureMessages ) {
	                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	                	failureMessage, "Check WaterML 2 file contents and command parameters." ) );
	            }
	        }
        }
        
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	// Make sure that all the time series that were read have the same interval.
        	// Otherwise handling of some parameters may not be proper (time zone, precision on period, interval, etc.)
        	if ( !TSUtil.areIntervalsSame(tslist) ) {
	            message = "Intervals for time series are not the same - command parameter interpretation may not be appropriate.";
	            Message.printWarning(log_level,
	                MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                routine, message );
	            status.addToLog ( commandPhase,
	                new CommandLogRecord(CommandStatusType.WARNING,
	                    message, "Verify that filename is correct and that the file exists." ) );
        	}
        }
				
		if ( tslist != null ) {
			int tscount = tslist.size();
			message = "Read " + tscount + " time series from \"" + InputFile_full + "\"";
			Message.printStatus ( 2, routine, message );
	        if ( (Alias != null) && !Alias.equals("") ) {
                for ( TS ts : tslist ) {
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, Alias, status, commandPhase);
                    ts.setAlias ( alias );
                }
            }
		}
	}
	catch ( Exception e ) {
		message = "Unexpected error reading WaterML 2 file. \"" + InputFile_full + "\" (" + e + ")";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(commandPhase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,"Check the log file for details."));
		throw new CommandException ( message );
	}
    
    if ( commandPhase == CommandPhaseType.RUN ) {
        if ( tslist != null ) {
            // Further process the time series...
            // This makes sure the period is at least as long as the output period...
            int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
            if ( wc > 0 ) {
                message = "Error post-processing series after read.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
    
            // Now add the list in the processor...
            
            int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
            if ( wc2 > 0 ) {
                message = "Error adding time series after read.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( tslist );
    }

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count ), routine, message );
		throw new CommandWarningException ( message );
	}
    
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props )
{   if ( props == null ) {
        return getCommandName() + "()";
    }

	String Alias = props.getValue("Alias");
	String InputFile = props.getValue("InputFile" );
	String ReadMethod = props.getValue("ReadMethod" );
	//String NewUnits = props.getValue("NewUnits");
	String Interval = props.getValue("Interval");
	String RequireDataToMatchInterval = props.getValue("RequireDataToMatchInterval");
	String OutputTimeZoneOffset = props.getValue("OutputTimeZoneOffset");
	String OutputTimeZone = props.getValue("OutputTimeZone");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");

	StringBuffer b = new StringBuffer ();

	// Input File
	if ((InputFile != null) && (InputFile.length() > 0)) {
		b.append("InputFile=\"" + InputFile + "\"");
	}
	
    if ( (ReadMethod != null) && (ReadMethod.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ReadMethod=" + ReadMethod );
    }

    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }
    
	// New Units
    /*
	if ((NewUnits != null) && (NewUnits.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("NewUnits=\"" + NewUnits + "\"");
	}*/
    if ((Interval != null) && (Interval.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Interval=\"" + Interval + "\"");
    }
    if ((RequireDataToMatchInterval != null) && (RequireDataToMatchInterval.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("RequireDataToMatchInterval=" + RequireDataToMatchInterval );
    }
    if ((OutputTimeZoneOffset != null) && (OutputTimeZoneOffset.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("OutputTimeZoneOffset=\"" + OutputTimeZoneOffset + "\"");
    }
    if ((OutputTimeZone != null) && (OutputTimeZone.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("OutputTimeZone=\"" + OutputTimeZone + "\"");
    }
	if ((InputStart != null) && (InputStart.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("InputStart=\"" + InputStart + "\"");
	}
	if ((InputEnd != null) && (InputEnd.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("InputEnd=\"" + InputEnd + "\"");
	}

    return getCommandName() + "("+ b.toString()+")";
}

}
