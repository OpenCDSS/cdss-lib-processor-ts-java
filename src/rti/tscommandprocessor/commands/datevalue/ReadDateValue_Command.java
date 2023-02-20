// ReadDateValue_Command - This class initializes, checks, and runs the ReadDateValue() commands.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandSavesMultipleVersions;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.TS.DateValueTS;

/**
TODO SAM 2011-03-15 Disabled the TS Alias version - this is really incompatible with DateValue files,
which automatically retain the alias (there is no reason to reassign the alias).  For now leave in both code
but later phase out the alias.
This class initializes, checks, and runs the ReadDateValue() commands.
*/
public class ReadDateValue_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{

/**
Data members used for IfNotFound parameter values.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

/**
List of time series read during discovery.  These are TS objects but with mainly the metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public ReadDateValue_Command () {
	super();
	setCommandName ( "ReadDateValue" );
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
    String routine = getClass().getName() + ".checkCommandParameters";
	String warning = "";
    String message;
    
    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Get the property values. 
	String InputFile = parameters.getValue("InputFile");
	String NewUnits = parameters.getValue("NewUnits");
	String InputStart = parameters.getValue("InputStart");
	String InputEnd = parameters.getValue("InputEnd");
	String Alias = parameters.getValue("Alias");
	String IfNotFound = parameters.getValue ( "IfNotFound" );
    
    if (Alias != null && !Alias.equals("")) {
        if (Alias.indexOf(" ") > -1) {
            // Do not allow spaces in the alias.
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
                // Working directory is available so use it.
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

	if ( NewUnits != null ) {
		// Will check at run time.
	}

	// InputStart
	DateTime inputStart = null, inputEnd = null;
	if ((InputStart != null) && !InputStart.isEmpty() && (InputStart.indexOf("${") < 0)) {
		try {
			inputStart = DateTime.parse(InputStart);
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
	if ((InputEnd != null) && !InputEnd.isEmpty() && (InputEnd.indexOf("${") < 0)) {
		try {
			inputEnd = DateTime.parse(InputEnd);
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
	if ( inputStart != null && inputEnd != null ) {
		if ( inputStart.greaterThan( inputEnd ) ) {
            message = InputStart + " (" + InputStart  + ") should be <= InputEnd (" + InputEnd + ").";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an input start <= the input end." ) );
		}
	}

	if ( (IfNotFound != null) && !IfNotFound.isEmpty() ) {
		if ( !IfNotFound.equalsIgnoreCase(_Ignore) && !IfNotFound.equalsIgnoreCase(_Warn)
		    && !IfNotFound.equalsIgnoreCase(_Fail) ) {
			message = "The IfNotFound parameter \"" + IfNotFound + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Ignore + ", " + _Warn + " (default), or " +
					_Fail + "."));
		}
	}
    
	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(5);
    validList.add ( "Alias" );
    validList.add ( "InputFile" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "NewUnits" );
	validList.add ( "IfNotFound" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, warning_level ), warning );
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
public boolean editCommand ( JFrame parent ) {	
	// The command will be modified if parameters are changed.
	return ( new ReadDateValue_JDialog ( parent, this ) ).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList () {
    return __discoveryTSList;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
	List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    // First time series in list should be of a type that be requested (e.g., MonthTS).
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class.
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discoveryTSList;
    }
    else {
        return null;
    }
}

/**
Parse the command string into a PropList of parameters.
@param command_string A string command to parse.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings (recommended is 2).
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
    if ( !command_string.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation.
        super.parseCommand(command_string);
    }
    else {
    	int index = command_string.indexOf("(");
    	String str = command_string.substring(index);
    	index = str.indexOf("=");
    
        // This is the new format of parsing, where parameters are specified as "InputFilter=", etc.
    	String routine = "ReadDateValue_Command.parseCommand", message;
    	
        String Alias = null;
        if (StringUtil.startsWithIgnoreCase(command_string, "TS ")) {
            // There is an alias specified.  Extract the alias from the full command.
            str = command_string.substring(3); // Alias = ReadDateValue(...)
            index = str.indexOf("=");
            int index2 = str.indexOf("(");
            if (index2 < index) {
                // No alias specified -- badly-formed command.
                Alias = "Invalid_Alias";
                message = "No alias was specified, although the command started with \"TS ...\"";
                Message.printWarning(warning_level, routine, message);
                throw new InvalidCommandSyntaxException(message);
            }
    
            Alias = str.substring(0, index).trim();
            // Parse the command parameters.
            String command_string2 = str.substring(index+1).trim(); // ReadDateValue(...)
            if ( (command_string2.indexOf("=") > 0) || command_string2.endsWith("()") ) {
                // New format.
                Message.printStatus(2, routine, "Parsing new format for " + command_string2);
                super.parseCommand ( command_string2 );
            }
            else {
                // Old format TS Alias = ReadDateValue(InputFile,TSID,NewUnits,InputStart,InputEnd)
                // Where TSID and later arguments are * if defaults.
                Message.printStatus(2, routine, "Parsing old format for " + command_string2);
                PropList parameters = getCommandParameters();
                parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
                parameters.set("InputFile", StringUtil.getToken(command_string2, "(,)", StringUtil.DELIM_ALLOW_STRINGS, 1));
                parameters.set("TSID", StringUtil.getToken(command_string2, "(,)", StringUtil.DELIM_ALLOW_STRINGS, 2));
                parameters.set("NewUnits", StringUtil.getToken(command_string2, "(,)", StringUtil.DELIM_ALLOW_STRINGS, 3));
                parameters.set("InputStart", StringUtil.getToken(command_string2, "(,)", StringUtil.DELIM_ALLOW_STRINGS, 4));
                parameters.set("InputEnd", StringUtil.getToken(command_string2, "(,)", StringUtil.DELIM_ALLOW_STRINGS, 5));
                parameters.setHowSet ( Prop.SET_UNKNOWN );
            }
            // Also set the alias.
            PropList parameters = getCommandParameters();
            parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
            parameters.set ( "Alias", Alias );
            // If the dates are old-style "*", convert to blanks.
            // Note that TSID is not currently used.
            String TSID = parameters.getValue("TSID");
            String InputStart = parameters.getValue("InputStart");
            String InputEnd = parameters.getValue("InputEnd");
            String NewUnits = parameters.getValue("NewUnits");
            if ( TSID != null ) {
                // Unset unused parameter.
                parameters.unSet( "TSID" );
            }
            if ( (InputStart != null) && InputStart.equals("*") ) {
                parameters.set("InputStart","");
            }
            if ( (InputEnd != null) && InputEnd.equals("*") ) {
                parameters.set("InputEnd","");
            }
            if ( (NewUnits != null) && NewUnits.equals("*") ) {
                parameters.set("NewUnits","");
            }
            parameters.setHowSet ( Prop.SET_UNKNOWN );
        }
        else {
            if ( (command_string.indexOf("=") > 0) || command_string.endsWith("()") ) {
                // Named parameters so parse the new way.
                super.parseCommand ( command_string );
            }
            else {
                // Grab the filename from the fixed list of parameters.
                PropList parameters = getCommandParameters();
                parameters.set("InputFile", StringUtil.getToken(command_string, "()", StringUtil.DELIM_ALLOW_STRINGS, 1));
            }
        }
    }
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
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
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_level = 2;
    int log_level = 3;
	String command_tag = "" + command_number;
	int warning_count = 0;

    // Get and clear the status and clear the run log.
    
    CommandStatus status = getCommandStatus();
    CommandProcessor processor = getCommandProcessor();
    Boolean clearStatus = new Boolean(true); // Default.
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen.
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile");
	String NewUnits = parameters.getValue("NewUnits");
	String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}"; // Global default.
	}
	String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}"; // Global default.
	}
	String Alias = parameters.getValue("Alias");
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	if ( (IfNotFound == null) || IfNotFound.equals("")) {
	    IfNotFound = _Warn; // Default.
	}
    
	DateTime InputStart_DateTime = null;
	DateTime InputEnd_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			InputStart_DateTime = TSCommandProcessorUtil.getDateTime ( InputStart, "InputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above.
			++warning_count;
		}
		try {
			InputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( InputEnd, "InputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above.
			++warning_count;
		}
	}
	
	// Read the file.
    List<TS> tslist = null; // Keep the list of time series.
    String InputFile_full = InputFile;
	try {
        boolean readData = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ){
            readData = false;
        }
        InputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        if ( !IOUtil.fileExists(InputFile_full) ) {
            if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            	if ( InputFile.indexOf("${") < 0 ) {
            		// Default for discovery mode is warning.
            		CommandStatusType messageType = CommandStatusType.WARNING;
        	   		if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
        	   			messageType = CommandStatusType.FAILURE;
        	   		}
        	   		if ( !IfNotFound.equalsIgnoreCase(_Ignore) ) {
        	   			// Not ignoring a missing file so generate a message.
        	   			message = "Input file does not exist:  \"" + InputFile_full + "\".";
        	   			Message.printWarning(log_level,
        	   				MessageUtil.formatMessageTag( command_tag, ++warning_count),
        	   				routine, message );
        	   			status.addToLog ( commandPhase,
        	   				new CommandLogRecord(messageType,
        	   					message, "Verify that filename is correct and that the file exists - " +
        	   					"may be OK if file is created during processing." ) );
        	   		}
            	}
            }
            else if ( commandPhase == CommandPhaseType.RUN ) {
            	// Default for run mode is failure.
            	CommandStatusType messageType = CommandStatusType.FAILURE;
        	   	if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
        	   		messageType = CommandStatusType.WARNING;
        	   	}
        	   	if ( !IfNotFound.equalsIgnoreCase(_Ignore) ) {
        	   		// Not ignoring a missing file so generate a message.
        	   		message = "Input file does not exist:  \"" + InputFile_full + "\".";
        	   		Message.printWarning(log_level,
        	   			MessageUtil.formatMessageTag( command_tag, ++warning_count),
        	   			routine, message );
        	   		status.addToLog ( commandPhase,
        	   			new CommandLogRecord(messageType,
        	   				message, "Verify that filename is correct and that the file exists." ) );
        	   	}
            }
        }
        else {
            // Read everything in the file (one time series or traces).
            tslist = DateValueTS.readTimeSeriesList (
                InputFile_full, InputStart_DateTime, InputEnd_DateTime, NewUnits, readData );
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
	catch ( FileNotFoundException e ) {
        message = "DateValue file \"" + InputFile_full + "\" is not found or accessible.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
            status.addToLog(commandPhase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,
                    "Verify that the file exists and is readable."));
        throw new CommandException ( message );
	}
	catch ( Exception e ) {
		message = "Unexpected error reading DateValue file. \"" + InputFile_full + "\" (" + e + ")";
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
            // Further process the time series.
            // This makes sure the period is at least as long as the output period.
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
    
            // Now add the list in the processor.
            
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
@param discovery_TS_List discovery time series list
*/
private void setDiscoveryTSList ( List<TS> discovery_TS_List ) {
    __discoveryTSList = discovery_TS_List;
}

/**
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props ) {
    return toString ( props, 10 );
}

/**
Return the string representation of the command.
@param parameters to include in the command
@param majorVersion the major version for software - if less than 10, the "TS Alias = " notation is used,
allowing command files to be saved for older software, no longer enabled
@return the string representation of the command
*/
public String toString ( PropList parameters, int majorVersion ) {
	String [] parameterOrder = {
		"InputFile",
		"Alias",
		"NewUnits",
		"InputStart",
		"InputEnd",
		"IfNotFound"
	};
	return this.toString(parameters, parameterOrder);
}

}