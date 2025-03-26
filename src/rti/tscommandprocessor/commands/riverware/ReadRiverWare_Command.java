// ReadRiverWare_Command - This class initializes, checks, and runs the ReadRiverWare() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.riverware;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.RiverWareTS;
import RTi.TS.TSEnsemble;
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

/**
This class initializes, checks, and runs the ReadRiverWare() command.
*/
public class ReadRiverWare_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{

protected final String _FALSE = "False";
protected final String _TRUE = "True";

protected final String _TimeSeries = "TimeSeries";
protected final String _TimeSeriesAndEnsembles = "TimeSeriesAndEnsembles";

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
List of ensembles read during discovery.  These are TSEnsemble objects but with mainly the
metadata (identifier) filled in.
*/
private List<TSEnsemble> __discoveryEnsembleList = null;

/**
Constructor.
*/
public ReadRiverWare_Command ()
{
	super();
	setCommandName ( "ReadRiverWare" );
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
{
    String routine = getClass().getSimpleName() + ".checkCommandParameters";
	String warning = "";
    String message;
    
    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Get the property values. 
	String InputFile = parameters.getValue("InputFile");
	String Output = parameters.getValue("Output");
	//String Units = parameters.getValue("Units");
	String InputStart = parameters.getValue("InputStart");
	String InputEnd = parameters.getValue("InputEnd");
	String Alias = parameters.getValue("Alias");
    
	// Require the alias to match historical behavior
	if ( (Alias == null) || Alias.equals("") ) {
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
            // TODO SAM 2011-05-23 The below is higher overhead but deals with dynamic folder changes
            // Why doesn't it work?
            //working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, this );
            //Message.printStatus(2, routine, "Working directory = \"" + working_dir + "\".");
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
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an existing input file." ) );
        }
    
        try {
            Message.printStatus(2, routine, "Working directory is \"" + working_dir + "\"" );
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
    
    if ( Output != null && !Output.equals("") && !Output.equalsIgnoreCase(_TimeSeries) &&
        !Output.equalsIgnoreCase(_TimeSeriesAndEnsembles) ) {
        message = "Output (" + Output  + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify Output as " + _TimeSeries + " (default) or " + _TimeSeriesAndEnsembles + ".") );
    }
    
    /*
	if ( Units != null ) {
		// Will check at run time
	}
	*/

	// InputStart
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
	
    if ( (__InputStart != null) && (__InputEnd != null) ) {
        if ( __InputStart.getPrecision() != __InputEnd.getPrecision() ) {
            message = "The input start \"" + InputStart + "\" precision and input end \"" + InputEnd +
            "\" precision are not the same.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify consistent precision for input start and end." ) );
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
    
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>();
    validList.add ( "Alias" );
    validList.add ( "InputFile" );
    validList.add ( "Output" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "Units" );
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
Create the list of ensembles from the list of time series.  The object name and slot name combinations make the unique
ensemble identifiers.
@param tslist list of time series to process
@return list of TSEnsemble determined from the time series
*/
private List<TSEnsemble> createEnsembleList ( List<TS> tslist )
{
    List<TSEnsemble> ensembleList = new ArrayList<TSEnsemble>();
    List<String> objectNames = new ArrayList<String>();
    List<String> slotNames = new ArrayList<String>();
    String objectName, slotName;
    boolean found;
    for ( TS ts : tslist ) {
        objectName = ts.getLocation();
        slotName = ts.getDataType();
        found = false;
        for ( int i = 0; i < objectNames.size(); i++ ) {
            if ( objectNames.get(i).equalsIgnoreCase(objectName) && slotNames.get(i).equalsIgnoreCase(slotName)) {
                found = true;
                break;
            }
        }
        if ( !found ) {
            // Create an ensemble and save the names to check against the other time series
            objectNames.add(objectName);
            slotNames.add(slotName);
        }
    }
    // Now have a unique set of object name and slot name pairs.  Loop and create the ensembles
    List<TS> ensembleTS = null;
    for ( int i = 0; i < objectNames.size(); i++ ) {
        // Get the list of matching time series
        ensembleTS = new ArrayList<TS>();
        objectName = objectNames.get(i);
        slotName = slotNames.get(i);
        for ( TS ts : tslist ) {
            if ( ts.getLocation().equalsIgnoreCase(objectName) && ts.getDataType().equalsIgnoreCase(slotName) ) {
                ensembleTS.add(ts);
            }
        }
        ensembleList.add ( new TSEnsemble(objectName + "_" + slotName, objectName + "_" + slotName, ensembleTS));
    }
    return ensembleList;
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
	return ( new ReadRiverWare_JDialog ( parent, this ) ).ok();
}

/**
Return the list of ensembles read in discovery phase.
*/
private List<TSEnsemble> getDiscoveryEnsembleList ()
{
    return __discoveryEnsembleList;
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
The following classes can be requested:  TS, TSEnsemble
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
	if ( c == TSEnsemble.class ) {
        return (List<T>)getDiscoveryEnsembleList();
    }
	// Time series
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Parse the command string into a PropList of parameters.
@param commandString A string command to parse.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings (recommended is 2).
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String commandString )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "ReadRiverWare_Command.parseCommand", message;
	
    if ( !commandString.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation
        super.parseCommand(commandString);
    }
    else {
        String Alias = null;
    	String InputFile = null;
    	String InputStart = null;
    	String InputEnd = null;
    	String Units = null;
        if (StringUtil.startsWithIgnoreCase(commandString, "TS ")) {
            // There is an alias specified.  Extract the alias from the full command.
            int index = commandString.indexOf("=");
            Alias = StringUtil.getToken ( commandString, " =", StringUtil.DELIM_SKIP_BLANKS, 1);
            if ( (StringUtil.patternCount(commandString,"=") >=2) || commandString.endsWith("()") ) {
                // New syntax, can be blank parameter list.  Extract command name and parameters to parse
                super.parseCommand ( commandString.substring(index + 1).trim() );
            }
            else {
                // Parse the old command...
            	List<String> tokens = StringUtil.breakStringList ( commandString.substring(index + 1),
                    "(,)", StringUtil.DELIM_ALLOW_STRINGS );
                if ( tokens.size() != 5 ) {
                    message = "Invalid syntax for legacy command \"" + commandString +
                    "\".  Expecting TS Alias = ReadRiverWare(InputFile,Units,InputStart,InputEnd).";
                    Message.printWarning ( warning_level, routine, message);
                    CommandStatus status = getCommandStatus();
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Verify command syntax or edit with command editor." ) );
                    throw new InvalidCommandSyntaxException ( message );
                }
                InputFile = (tokens.get(1)).trim();
                Units = (tokens.get(2)).trim();
                InputStart = (tokens.get(3)).trim();
                InputEnd = (tokens.get(4)).trim();
                PropList parameters = getCommandParameters();
                parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
                parameters.set ( "InputFile", InputFile );
                parameters.set ( "InputStart", InputStart );
                parameters.set ( "InputEnd", InputEnd );
                parameters.set ( "Units", Units );
                parameters.setHowSet ( Prop.SET_UNKNOWN );
                setCommandParameters ( parameters );
            }
        }
        
        PropList parameters = getCommandParameters();
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        parameters.set ( "Alias", Alias );
        parameters.setHowSet ( Prop.SET_UNKNOWN );
        setCommandParameters ( parameters );
     
     	// The following is for backwards compatibility with old commands files.
    	if ( (parameters.getValue("InputStart") != null) && parameters.getValue("InputStart").equals("*") ) {
    	    // Reset to more recent blank default
    	    parameters.set("InputStart","");
    	}
        if ( (parameters.getValue("InputEnd") != null) && parameters.getValue("InputEnd").equals("*") ) {
            // Reset to more recent blank default
            parameters.set("InputEnd","");
        }
        if ( (parameters.getValue("Units") != null) && parameters.getValue("Units").equals("*") ) {
            // Reset to more recent blank default
            parameters.set("Units","");
        }
    }
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
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
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_level = 2;
    //int log_level = 3;
	String command_tag = "" + command_number;
	int warning_count = 0;
	    
    // Get and clear the status and clear the run log...
    
    CommandStatus status = getCommandStatus();
    CommandProcessor processor = getCommandProcessor();
    Boolean clearStatus = Boolean.TRUE; // Default.
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
    boolean readData = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ){
        readData = false;
        setDiscoveryTSList ( new ArrayList<TS>() );
        setDiscoveryEnsembleList ( new ArrayList<TSEnsemble>() );
    }

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile");
	String Output = parameters.getValue("Output");
	if ( (Output == null) || Output.equals("") ) {
	    Output = _TimeSeries; // default
	}
	String Units = parameters.getValue("Units");
	String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}";
	}
    String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}";
	}
	String Alias = parameters.getValue("Alias");
    
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

	// Read the file.
    List<TS> tslist = new ArrayList<TS>();
    List<TSEnsemble> ensembleList = new ArrayList<TSEnsemble>();
    String InputFile_full = InputFile;
    boolean isRdf = false;
	try {
        InputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        if ( !IOUtil.fileExists(InputFile_full)) {
            message = "The RiverWare file \"" + InputFile_full + "\" does not exist.";
            status.addToLog(commandPhase,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,"Verify that the filename is correct."));
        }
        else {
            isRdf = RiverWareTS.isRiverWareFile ( InputFile_full, true );
            if ( isRdf ) {
                // Read multiple time series from the file
                Message.printStatus(2, routine, "Reading RiverWare RDF file \"" + InputFile_full + "\"" );
                tslist = RiverWareTS.readTimeSeriesListFromRdf ( InputFile_full, InputStart_DateTime, InputEnd_DateTime, Units, readData );
                if ( Output.equalsIgnoreCase(_TimeSeriesAndEnsembles) ) {
                    // Create a list of ensemble from the time series list
                    ensembleList = createEnsembleList ( tslist );
                }
            }
            else {
                // Read a single time series
                TS ts = RiverWareTS.readTimeSeries ( InputFile_full, InputStart_DateTime, InputEnd_DateTime, Units, readData );
                if ( ts == null ) {
                    message = "Unable to read time series from RiverWare file \"" + InputFile_full + "\".";
                    status.addToLog(commandPhase,
                            new CommandLogRecord(
                                    CommandStatusType.FAILURE, message,"See the log file."));
                }
                else {
                    tslist.add ( ts );
                }
            }
            if ( (Alias != null) && !Alias.equals("") ) {
                for ( TS ts2 : tslist ) {
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts2, Alias, status, commandPhase);
                    ts2.setAlias ( alias );
                }
            }
        }
	} 
	catch ( Exception e ) {
		message = "Unexpected error reading RiverWare file. \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count ),routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(commandPhase,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,"Check the log file for details."));
		throw new CommandException ( message );
	}
    
    int size = 1;
    Message.printStatus ( 2, routine, "Read " + size + " RiverWare time series." );

    if ( commandPhase == CommandPhaseType.RUN ) {
        if ( tslist.size() > 0 ) {
            // Further process the time series...
            // This makes sure the period is at least as long as the output period...
            int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
            if ( wc > 0 ) {
                message = "Error post-processing RiverWare time series after read.";
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
                message = "Error adding RiverWare time series after read.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
        }
        if ( Output.equalsIgnoreCase(_TimeSeriesAndEnsembles) ) {
            for ( TSEnsemble ensemble : ensembleList ) {
                TSCommandProcessorUtil.appendEnsembleToResultsEnsembleList(processor, this, ensemble );
            }
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( tslist );
        if ( Output.equalsIgnoreCase(_TimeSeriesAndEnsembles) ) {
            setDiscoveryEnsembleList ( ensembleList );
        }
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
Set the list of ensembles read in discovery phase.
*/
private void setDiscoveryEnsembleList ( List<TSEnsemble> discoveryEnsembleList )
{
    __discoveryEnsembleList = discoveryEnsembleList;
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList ) {
    __discoveryTSList = discoveryTSList;
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
		"Output",
		"Alias",
		"Units",
		"InputStart",
		"InputEnd"
	};
	return this.toString(parameters, parameterOrder);
}

}