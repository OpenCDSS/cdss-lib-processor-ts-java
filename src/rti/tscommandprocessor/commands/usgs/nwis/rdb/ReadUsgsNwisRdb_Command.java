package rti.tscommandprocessor.commands.usgs.nwis.rdb;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.UsgsNwisRdbTS;
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
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the ReadUsgsNwisRdb() command.
*/
public class ReadUsgsNwisRdb_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{

protected static final String
	_FALSE = "False",
	_TRUE = "True";

/**
Private data members shared between the checkCommandParameter() and the 
runCommand() methods (prevent code duplication parsing dateTime strings).  
*/
private DateTime __InputStart = null;
private DateTime __InputEnd   = null;

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public ReadUsgsNwisRdb_Command () {
	super();
	setCommandName ( "ReadUsgsNwisRdb" );
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
	//String NewUnits  = parameters.getValue("NewUnits");
	String Interval = parameters.getValue("Interval");
	String InputStart = parameters.getValue("InputStart");
	String InputEnd   = parameters.getValue("InputEnd");
	String Alias = parameters.getValue("Alias");
    
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

    /*
	if ( NewUnits != null ) {
		// Will check at run time
	}
	*/
    
    if ( (Interval != null) && !Interval.isEmpty() ) {
        try {
            TimeInterval.parseInterval(Interval);
        }
        catch ( Exception e ) {
            message = "The time series data interval is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid data interval using the command editor." ) );
        }
    }

	// InputStart
	if ((InputStart != null) && !InputStart.equals("")) {
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
		
		if (__InputStart != null) {
			if (__InputStart.getPrecision() != DateTime.PRECISION_DAY) {
                message = "The input start date/time \"" + InputStart + "\" precision is not day.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the input start to day precision." ) );
			}		
		}
	}

	// InputEnd
	if ((InputEnd != null) && !InputEnd.equals("")) {
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
		
		if (__InputEnd != null) {
			if (__InputEnd.getPrecision() != DateTime.PRECISION_DAY) {
                message = "The input end date/time \"" + InputStart + "\" precision is not day.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the input end to day precision." ) );
			}		
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
	List<String> validList = new ArrayList<String>(7);
    validList.add ( "Alias" );
    validList.add ( "InputFile" );
    validList.add ( "DataType" );
    validList.add ( "Interval" );
    validList.add ( "Units" );
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
	return ( new ReadUsgsNwisRdb_JDialog ( parent, this ) ).ok();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
	__InputStart = null;
	__InputEnd   = null;
	super.finalize();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
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
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String commandString )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "ReadUsgsNwisRdb_Command.parseCommand", message;
	
    if ( !commandString.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation
        super.parseCommand(commandString);
    }
    else {
        String Alias = null;
    	String InputFile = null;
    	String InputStart = null;
    	String InputEnd = null;
        if (StringUtil.startsWithIgnoreCase(commandString, "TS ")) {
            // There is an alias specified.  Extract the alias from the full command.
            Alias = StringUtil.getToken ( commandString, " =", StringUtil.DELIM_SKIP_BLANKS, 1);
            if ( (StringUtil.patternCount(commandString,"=") >=2) || commandString.endsWith("()") ) {
                // New syntax, can be blank parameter list.  Extract command name and parameters to parse
                int index = commandString.indexOf("=");
                super.parseCommand ( commandString.substring(index + 1) );
            }
            else {
                // Parse the old command...
            	List<String> tokens = StringUtil.breakStringList ( commandString,
                    "(,)", StringUtil.DELIM_ALLOW_STRINGS );
                if ( tokens.size() != 4 ) {
                    message =
                    "Invalid syntax for legacy command \"" + commandString +
                    "\".  Expecting TS Alias = ReadUsgsNwisRdb(InputFile,InputStart,InputEnd).";
                    Message.printWarning ( warning_level, routine, message);
                    CommandStatus status = getCommandStatus();
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Verify command syntax or edit with command editor." ) );
                    throw new InvalidCommandSyntaxException ( message );
                }
                InputFile = tokens.get(1).trim();
                InputStart = tokens.get(2).trim();
                InputEnd = tokens.get(3).trim();
                PropList parameters = getCommandParameters();
                parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
                parameters.set ( "InputFile", InputFile );
                parameters.set ( "InputStart", InputStart );
                parameters.set ( "InputEnd", InputEnd );
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
    	if (parameters.getValue("InputStart") == null) {
    		parameters.set("InputStart", parameters.getValue("QueryStart"));
    	}
    	if ( (parameters.getValue("InputStart") != null) &&
    	        parameters.getValue("InputStart").equals("*") ) {
    	    // Reset to more recent blank default
    	    parameters.set("InputStart","");
    	}
    	if ( parameters.getValue("InputEnd") == null) {
    		parameters.set("InputEnd", parameters.getValue(	"QueryEnd"));
    	}
        if ( (parameters.getValue("InputEnd") != null) &&
            parameters.getValue("InputEnd").equals("*") ) {
            // Reset to more recent blank default
            parameters.set("InputEnd","");
        }
    }
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
    
	CommandProcessor processor = getCommandProcessor();
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
    boolean read_data = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ){
        read_data = false;
        setDiscoveryTSList ( null );
    }

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile"); // Expand below
	//String NewUnits = parameters.getValue("NewUnits");
	String Interval = parameters.getValue("Interval");
	if ( (Interval == null) || Interval.isEmpty() ) {
		Interval = "Day"; // Default
	}
	String DataType = parameters.getValue("DataType");
	TimeInterval interval = TimeInterval.parseInterval(Interval);
	String Units = parameters.getValue("Units");
	String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}"; // Global default
	}
	String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}"; // Global default
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
    TS ts = null;
    String InputFile_full = InputFile;
	try {
        InputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                        TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        if ( !IOUtil.fileExists(InputFile_full)) {
            message = "The USGS NWIS file \"" + InputFile_full + "\" does not exist.";
            status.addToLog(commandPhase,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,"Verify that the filename is correct."));
        }
        else {
            // No requested units...
            ts = UsgsNwisRdbTS.readTimeSeries ( InputFile_full, InputStart_DateTime, InputEnd_DateTime, DataType, interval, Units, null, read_data );
            if ( ts == null ) {
                message = "Unable to read time series from USGS RDB file \"" + InputFile_full + "\".";
                status.addToLog(commandPhase,
                        new CommandLogRecord(
                                CommandStatusType.FAILURE, message,"See the log file."));
            }
            else {
	            if ( (Alias != null) && !Alias.equals("") ) {
	                String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
	                    processor, ts, Alias, status, commandPhase);
	                ts.setAlias ( alias );
	            }
            }
        }
	} 
	catch ( Exception e ) {
		message = "Unexpected error reading USGS NWIS RDB file. \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count ),routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(commandPhase,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,"Check the log file for details."));
		throw new CommandException ( message );
	}
    
    int size = 1;
    Message.printStatus ( 2, routine, "Read " + size + " USGS NWIS RDB time series." );

    if ( commandPhase == CommandPhaseType.RUN ) {
        if ( ts != null ) {
            // Further process the time series...
            // This makes sure the period is at least as long as the output period...
            int wc = TSCommandProcessorUtil.processTimeSeriesAfterRead( processor, this, ts );
            if ( wc > 0 ) {
                message = "Error post-processing USGS NWIS time series after read.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
    
            // Now add the list in the processor...
            
            int wc2 = TSCommandProcessorUtil.appendTimeSeriesToResultsList ( processor, this, ts );
            if ( wc2 > 0 ) {
                message = "Error adding USGS NWIS time series after read.";
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
    	List<TS> tslist = new Vector<TS>(1);
        tslist.add ( ts );
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
private void setDiscoveryTSList ( List<TS> discovery_TS_Vector ) {
    __discovery_TS_Vector = discovery_TS_Vector;
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
		"DataType",
		"Interval",
		"Units",
		//"NewUnits",
		"InputStart",
		"InputEnd"
	};
	return this.toString(parameters, parameterOrder);
}

}