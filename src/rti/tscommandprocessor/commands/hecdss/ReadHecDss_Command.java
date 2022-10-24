// ReadHecDss_Command - This class initializes, checks, and runs ReadHecDss() command.

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

package rti.tscommandprocessor.commands.hecdss;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import hec.heclib.dss.DSSPathname;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
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

/**
This class initializes, checks, and runs ReadHecDss() command.
*/
public class ReadHecDss_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

protected final String
	_False = "False",
	_True = "True";

// FIXME SAM 2007-12-19 Need to evaluate this - runtime versions may be different.
/**
Private data members shared between the checkCommandParameter() and the
runCommand() methods (prevent code duplication parsing dateTime strings).
*/
private DateTime __InputStart = null;
private DateTime __InputEnd   = null;

/**
List of time series read during discovery.
These are TS objects but with mainly the metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_List = null;

/**
Constructor.
*/
public ReadHecDss_Command () {
	super();
	setCommandName ( "ReadHecDss" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
    String routine = getClass().getSimpleName() + ".checkCommandParameters";
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
	String A = parameters.getValue("A");
	String B = parameters.getValue("B");
	//String Alias = parameters.getValue("Alias");
	String CloseAfterReading = parameters.getValue("CloseAfterReading");

    /*
	if ( _use_alias && ((Alias == null) || Alias.equals("")) ) {
	    message = "The Alias must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an alias." ) );
    }

    if (Alias != null && !Alias.equals("")) {
        if (Alias.indexOf(" ") > -1) {
            // do not allow spaces in the alias
            message = "The Alias value cannot contain any spaces.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Remove spaces from the alias." ) );
        }
    }
    */

    if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
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
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
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
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
        }
    }

    if ( (A != null) && (A.length() != 0) && (A.contains(":") || A.contains("."))) {
        message = "The value for A (" + A + ") is invalid - cannot contain \":.\".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a value for A that does not contain \":.\" characters." ) );
    }
    if ( (B != null) && (B.length() != 0) && (B.contains(":") || B.contains("."))) {
        message = "The value for B (" + B + ") is invalid - cannot contain \":.\".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a value for B that does not contain \":.\" characters." ) );
    }

	if ( NewUnits != null ) {
		// Will check at run time.
	}

	// InputStart.
	if ((InputStart != null) && !InputStart.isEmpty() && (InputStart.indexOf("${") < 0) ) {
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

	// InputEnd.
	if ((InputEnd != null) && !InputEnd.isEmpty() && (InputEnd.indexOf("${") < 0)) {
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

	// Make sure __InputStart precedes __InputEnd.
	if ( __InputStart != null && __InputEnd != null ) {
		if ( __InputStart.greaterThanOrEqualTo( __InputEnd ) ) {
            message = InputStart + " (" + __InputStart  + ") should be less than InputEnd (" + __InputEnd + ").";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an input start less than the input end." ) );
		}
	}

	if ( (CloseAfterReading != null) && !CloseAfterReading.equals("") ) {
		if ( !CloseAfterReading.equalsIgnoreCase(_False) && !CloseAfterReading.equalsIgnoreCase(_True) ) {
			message = "The CloseAfterReading parameter \"" + CloseAfterReading + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _False + " (default), or " + _True + "."));
		}
	}

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(14);
    validList.add ( "InputFile" );
    validList.add ( "A" );
    validList.add ( "B" );
    validList.add ( "C" );
    validList.add ( "D" );
    validList.add ( "E" );
    validList.add ( "F" );
    validList.add ( "Pathname" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "NewUnits" );
    validList.add ( "Location" );
    validList.add ( "Alias" );
    validList.add ( "CloseAfterReading" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag, warning_level ), warning );
		throw new InvalidCommandParameterException ( warning );
	}

    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {	
	// The command will be modified if changed.
	return ( new ReadHecDss_JDialog ( parent, this ) ).ok();
}

/**
Expand the time series identifier location based on user input.
This assumes that the time series identifier has been set to defaults.
@param ts time series to process.
@param location location string to process and set in the time series location.
@return expanded location string, suitable for setting in the time series location.
*/
private String expandLocation ( TS ts, String location ) {
    DSSPathname dssPathname = HecDssAPI.getPathnamePartsFromDefaultTSIdent(ts);
    StringBuffer b = new StringBuffer();
    int length = location.length();
    char c;
    for ( int i = 0; i < length; i++ ) {
        c = location.charAt(i);
        if ( c == '%' ) {
            // Special meaning - need to check next character to know.
            ++i;
            if ( i >= length ) {
                break;
            }
            char c2 = location.charAt(i);
            if ( (c2 == 'a') || (c2 == 'A') ) {
                b.append ( dssPathname.getAPart() );
            }
            else if ( (c2 == 'b') || (c2 == 'B') ) {
                b.append ( dssPathname.getBPart() );
            }
            else if ( (c2 == 'c') || (c2 == 'C') ) {
                b.append ( dssPathname.getCPart() );
            }
            else if ( (c2 == 'e') || (c2 == 'E') ) {
                b.append ( dssPathname.getEPart() );
            }
            else if ( (c2 == 'f') || (c2 == 'F') ) {
                b.append ( dssPathname.getFPart() );
            }
            else {
                // Nothing special - add the literal characters.
                b.append ( "%" + c2 );
            }
        }
        else {
            b.append ( c );
        }
    }
    return b.toString();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList () {
    return __discovery_TS_List;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS).
    TS datats = discovery_TS_Vector.get(0);
    // Also check the base class.
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discovery_TS_Vector;
    }
    else {
        return null;
    }
}

// Use parent for parseCommandParameters().

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
{	String routine = getClass().getSimpleName() + ".runCommand", message;
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

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile");
    String A = parameters.getValue("A");
    if ( (A == null) || A.equals("") ) {
        A = "*"; // Default.
    }
	if ( commandPhase == CommandPhaseType.RUN ) {
		A = TSCommandProcessorUtil.expandParameterValue(processor, this, A);
	}
    String B = parameters.getValue("B");
    if ( (B == null) || B.equals("") ) {
        B = "*"; // Default.
    }
	if ( commandPhase == CommandPhaseType.RUN ) {
		B = TSCommandProcessorUtil.expandParameterValue(processor, this, B);
	}
    String C = parameters.getValue("C");
    if ( (C == null) || C.equals("") ) {
        C = "*"; // Default.
    }
	if ( commandPhase == CommandPhaseType.RUN ) {
		C = TSCommandProcessorUtil.expandParameterValue(processor, this, C);
	}
    String E = parameters.getValue("E");
    if ( (E == null) || E.equals("") ) {
        E = "*"; // Default.
    }
	if ( commandPhase == CommandPhaseType.RUN ) {
		E = TSCommandProcessorUtil.expandParameterValue(processor, this, E);
	}
    String F = parameters.getValue("F");
    if ( (F == null) || F.equals("") ) {
        F = "*"; // Default.
    }
	if ( commandPhase == CommandPhaseType.RUN ) {
		F = TSCommandProcessorUtil.expandParameterValue(processor, this, F);
	}
    String Pathname = parameters.getValue("Pathname"); // Null OK if not available.
	if ( commandPhase == CommandPhaseType.RUN ) {
		Pathname = TSCommandProcessorUtil.expandParameterValue(processor, this, Pathname);
	}
	String NewUnits = parameters.getValue("NewUnits");
	String Location = parameters.getValue("Location");
	if ( commandPhase == CommandPhaseType.RUN ) {
		Location = TSCommandProcessorUtil.expandParameterValue(processor, this, Location);
	}
	String Alias = parameters.getValue("Alias");
	String CloseAfterReading = parameters.getValue("CloseAfterReading");
	boolean closeAfterReading = false;
    if ( (CloseAfterReading != null) && CloseAfterReading.equalsIgnoreCase(_True) ) {
    	closeAfterReading = true;
    }
	
    String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}"; // Global default.
	}
	String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}"; // Global default.
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
	
    if ( IOUtil.isUNIXMachine() ) {
        message = "The command is not enabled for UNIX/Linux.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
        status.addToLog(commandPhase, new CommandLogRecord( CommandStatusType.FAILURE, message,
            "Do not use the command on UNIX/Linux"));
        throw new CommandException ( message );
    }

    /* TODO smalers 2022-08-11 remove when 64-bit tests out.
    int arch = IOUtil.getJreArchBits();
    if ( arch != 32 ) {
    	message = "Running as " + arch + "-bit environment. The command is only supported on 32-bit Java Runtime Environment (and corresponding 32-bit HEC-DSS libraries).";
    	Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
    	status.addToLog(commandPhase, new CommandLogRecord( CommandStatusType.FAILURE, message,
        	"Do not use the command on 64 bit operating systems."));
    	throw new CommandException ( message );
    }
    */
	
    String InputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
    if ( !IOUtil.fileExists(InputFile_full) && (InputFile_full.indexOf("${") < 0) ) {
        message = "Input file \"" + InputFile_full + "\" does not exist.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	status.addToLog(commandPhase, new CommandLogRecord( CommandStatusType.WARNING, message,
            	"Verify that the file exists before using this command - may be OK if file does not yet exist."));
        	throw new CommandException ( message );
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
        	status.addToLog(commandPhase, new CommandLogRecord( CommandStatusType.FAILURE, message,
            	"Verify that the file exists before using this command."));
        	throw new CommandException ( message );
        }
    }

	// Read the file.
    List<TS> tslist = new ArrayList<>();
	try {
        boolean read_data = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            read_data = false;
        }
        if ( InputFile_full.indexOf("${") < 0 ) {
        	if ( (Pathname != null) && !Pathname.equals("") ) {
        		// If the pathname is specified, read it as is.
            	tslist = HecDssAPI.readTimeSeriesListUsingPathname (
                new File(InputFile_full), Pathname,
                  	InputStart_DateTime, InputEnd_DateTime, NewUnits, read_data, closeAfterReading );
        	}
        	else {
            	// Read the matching time series in the file (one time series or traces).
            	tslist = HecDssAPI.readTimeSeriesList (
                	new File(InputFile_full), A + ":" + B + ".HEC-DSS." + C + "." + E + "." + F,
                    	InputStart_DateTime, InputEnd_DateTime, NewUnits, read_data, closeAfterReading );
        	}
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
        	// Expansion of the input file properties at run time has not resolved all properties.
        	message = "Input file contains unexpanded properties.";
        	Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
        	status.addToLog(commandPhase, new CommandLogRecord( CommandStatusType.FAILURE, message,
            	"Verify that properties are defined correctly."));
        }
        // TODO SAM 2007-12-27 - should enable EnsembleID if traces.
		List<String> aliasList = new ArrayList<>();
		if ( tslist != null ) {
			int tscount = tslist.size();
			message = "Read " + tscount + " time series from \"" + InputFile_full + "\"";
			Message.printStatus ( 2, routine, message );
			TS ts = null;
            for (int i = 0; i < tscount; i++) {
                ts = (TS)tslist.get(i);
                if ( (Location != null) && (Location.length() > 0) ) {
                    // Set the location in the TSID.
                    ts.setLocation(expandLocation(ts,Location));
                    // Also reset the description.
                    ts.setDescription(ts.getLocation() + " " + ts.getDataType() );
                }
                if ( (Alias != null) && (Alias.length() > 0) ) {
                    // Set the alias to the desired string - this is impacted by the Location parameter.
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, Alias, status, commandPhase);
                    ts.setAlias ( alias );
                    // Search for duplicate alias and warn.
                    int aliasListSize = aliasList.size();
                    for ( int iAlias = 0; iAlias < aliasListSize; iAlias++ ) {
                        if ( aliasList.get(iAlias).equalsIgnoreCase(alias)) {
                            message = "Alias \"" + alias +
                            "\" was also used for another time series read from the HEC-DSS file.";
                            Message.printWarning(log_level,
                                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Consider using a more specific alias to uniquely identify the time series." ) );
                        }
                    }
                    // Now add the new list to the alias.
                    aliasList.add ( alias );
                }
            }
		}
	}
    catch ( UnsatisfiedLinkError e ) {
        message = "Unexpected error loading HEC-DSS dynamic library (" + e + ").";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
        Message.printWarning ( warning_level, routine, e );
        status.addToLog(commandPhase, new CommandLogRecord( CommandStatusType.FAILURE, message,
            "Contact software support - may be a problem if running on a file server."));
        throw new CommandException ( message );
    }
	catch ( FileNotFoundException e ) {
        message = "HEC-DSS file \"" + InputFile_full + "\" is not found or accessible.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
        status.addToLog(commandPhase, new CommandLogRecord( CommandStatusType.FAILURE, message,
            "Verify that the file exists and is readable."));
        throw new CommandException ( message );
	}
	catch ( Exception e ) {
		message = "Unexpected error reading HEC-DSS file. \"" + InputFile_full + "\" (" + e + ")";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count ),routine, message );
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
                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }

            // Now add the list in the processor.

            int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
            if ( wc2 > 0 ) {
                message = "Error adding time series after read.";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
                status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( tslist );
    }
    
    /*
    try {
    	// TODO smalers 2022-10-23 need to confirm the behavior based on Karl Tarbet input.
    	// If requested, close the file after reading.
    	// The HEC-DSS library typically keeps open and locks the file,
    	// which prevents commands such as CopyFile from working correctly because the first discovery run locks the file.
    	// Currently, only have information about how to close all the files so do that,
    	// but it may not work correctly or be optimized.
    	// Since this may be used mainly only in testing, enable the parameter and see how it goes.
    	if ( closeAfterReading && (InputFile_full.indexOf("${") < 0) ) {
    		// TODO smalers 2022-10-23 really just need to close the one file.
    		//HecDssAPI.closeAllFiles();
    		// Found the following method in 
    		//HecDssAPI.closeFile(InputFile_full);
    	}
    }
    catch ( Exception e ) {
        message = "Error closing DSS file \"" + InputFile_full + "\"";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
        status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report the problem to software support." ) );
        throw new CommandException ( message );
    }
    */

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
private void setDiscoveryTSList ( List<TS> discovery_TS_List ) {
    __discovery_TS_List = discovery_TS_List;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props ) {
	if ( props == null ) {
		return getCommandName() + "()";
	}

	String InputFile = props.getValue("InputFile" );
    String A = props.getValue("A");
    String B = props.getValue("B");
    String C = props.getValue("C");
    String E = props.getValue("E");
    String F = props.getValue("F");
    String Pathname = props.getValue("Pathname");
	String NewUnits = props.getValue("NewUnits");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");
	String Location = props.getValue("Location");
	String Alias = props.getValue("Alias");
	String CloseAfterReading = props.getValue("CloseAfterReading");

	StringBuffer b = new StringBuffer ();

	if ((InputFile != null) && (InputFile.length() > 0)) {
		b.append("InputFile=\"" + InputFile + "\"");
	}

    if ((A != null) && (A.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("A=\"" + A + "\"");
    }
    if ((B != null) && (B.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("B=\"" + B + "\"");
    }
    if ((C != null) && (C.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("C=\"" + C + "\"");
    }
    if ((E != null) && (E.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("E=\"" + E + "\"");
    }
    if ((F != null) && (F.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("F=\"" + F + "\"");
    }
    if ((Pathname != null) && (Pathname.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Pathname=\"" + Pathname + "\"");
    }

	if ((NewUnits != null) && (NewUnits.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("NewUnits=\"" + NewUnits + "\"");
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
	
    if ((Location != null) && (Location.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Location=\"" + Location + "\"");
    }

    if ((Alias != null) && (Alias.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Alias=\"" + Alias + "\"");
    }

    if ((CloseAfterReading != null) && (CloseAfterReading.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("CloseAfterReading=\"" + CloseAfterReading + "\"");
    }

	return getCommandName() + "(" + b.toString() + ")";
}

}