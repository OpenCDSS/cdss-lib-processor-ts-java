package rti.tscommandprocessor.commands.statemod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;
import RTi.TS.TSUtil;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

import DWR.StateMod.StateMod_TS;

/**
This class initializes, checks, and runs the StateModMax() command.
*/
public class StateModMax_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public StateModMax_Command ()
{	super();
	setCommandName ( "StateModMax" );
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
{	String routine = getCommandName() + ".checkCommandParameters";
    String InputFile1 = parameters.getValue ( "InputFile1" );
    String InputFile2 = parameters.getValue ( "InputFile2" );
	String warning = "";
    String message;

	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
	if ( (InputFile1 == null) || (InputFile1.length() == 0) ) {
        message = "The first input file must be specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
	}
	else {
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
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, InputFile1));
                File f = new File ( adjusted_path );
                if ( !f.exists() ) {
                    message = "The first input file does not exist:  \"" + adjusted_path + "\".";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Verify that the first input file exists - may be OK if created at run time." ) );
                }
		}
		catch ( Exception e ) {
            message = "The first input file:\n" +
            "    \"" + InputFile1 +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
		}
	}
	
    if ( (InputFile2 == null) || (InputFile2.length() == 0) ) {
        message = "The second input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
    }
    else {
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
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, InputFile2));
                File f = new File ( adjusted_path );
                if ( !f.exists() ) {
                    message = "The second input file does not exist:  \"" + adjusted_path + "\".";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Verify that the second input file exists - may be OK if created at run time." ) );
                }
        }
        catch ( Exception e ) {
            message = "The second input file:\n" +
            "    \"" + InputFile2 +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
        }
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(2);
    validList.add ( "InputFile1" );
    validList.add ( "InputFile2" );
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
	return (new StateModMax_JDialog ( parent, this )).ok();
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
*/
public List getObjectList ( Class c )
{
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Parse the command string into a PropList of parameters.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	String routine = "StateModMax_Command.parseCommand", message;
	int warning_level = 2;
    CommandStatus status = getCommandStatus();
    if ( (command_string.indexOf("=") > 0) || command_string.endsWith("()") ) {
		// New syntax...
		super.parseCommand ( command_string );
	}
	else {
	    // Parse the old command...
		List<String> tokens = StringUtil.breakStringList (command_string,
			"(,)", StringUtil.DELIM_ALLOW_STRINGS );
		if ( tokens.size() != 3 ) {
			message = "Invalid syntax for command.  Expecting StateModMax(InputFile1,InputFile2).";
			Message.printWarning ( warning_level, routine, message);
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Use the command editor to correct the command." ) );
			throw new InvalidCommandSyntaxException ( message );
		}
		String InputFile1 = ((String)tokens.get(1)).trim();
		String InputFile2 = ((String)tokens.get(2)).trim();
		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( InputFile1.length() > 0 ) {
			parameters.set ( "InputFile1", InputFile1 );
		}
        if ( InputFile2.length() > 0 ) {
            parameters.set ( "InputFile2", InputFile2 );
        }
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
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
Run method internal to this class, to handle running in discovery and run mode.
@param command_number Command number 1+ from processor.
@param command_phase Command phase being executed (RUN or DISCOVERY).
*/
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    String routine = "StateModMax_Command.runCommandInternal", message;
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    int log_level = 3;  // Log level for non-user warnings
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    CommandProcessor processor = getCommandProcessor();

    PropList parameters = getCommandParameters();
    String InputFile1 = parameters.getValue ( "InputFile1" );
    String InputFile2 = parameters.getValue ( "InputFile2" );
    
    // Get global input start and end...
    
    DateTime InputStart_DateTime = null;
    DateTime InputEnd_DateTime = null;
    Object o = null;
    try {
        o = processor.getPropContents( "InputStart" );
        if ( o != null ) {
            InputStart_DateTime = (DateTime)o;
        }
        o = processor.getPropContents( "InputEnd" );
        if ( o != null ) {
            InputEnd_DateTime = (DateTime)o;
        }
    }
    catch ( Exception e ) {
        message = "Error getting InputStart and InputEnd from processor.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
            status.addToLog(command_phase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,
                    "Report the problem to software support."));
    }

    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings about command parameters.";
        Message.printWarning ( warning_level, 
        MessageUtil.formatMessageTag(command_tag, ++warning_count),
        routine, message );
        throw new InvalidCommandParameterException ( message );
    }

    // Now try to read...

    String InputFile1_full = InputFile1;
    String InputFile2_full = InputFile2;
    List<TS> tslist1 = null;  // First file and results
    List<TS> tslist2 = null;  // Second file
    try {
        boolean readData = true;
        if ( command_phase == CommandPhaseType.DISCOVERY ){
            readData = false;
        }
        InputFile1_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),InputFile1) );
        Message.printStatus ( 2, routine, "Reading StateMod file 1 \"" + InputFile1_full + "\"" );
    
        if ( !IOUtil.fileReadable(InputFile1_full) || !IOUtil.fileExists(InputFile1_full)) {
            message = "StateMod file \"" + InputFile1_full + "\" is not found or accessible.";
            Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
                    status.addToLog(command_phase,
                        new CommandLogRecord( CommandStatusType.FAILURE, message,
                            "Verify that the file exists and is readable."));
            throw new CommandException ( message );
        }
        
        InputFile2_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),InputFile2) );
        Message.printStatus ( 2, routine, "Reading StateMod file 2 \"" + InputFile2_full + "\"" );
    
        if ( !IOUtil.fileReadable(InputFile2_full) || !IOUtil.fileExists(InputFile2_full)) {
            message = "StateMod file \"" + InputFile2_full + "\" is not found or accessible.";
            Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
                    status.addToLog(command_phase,
                        new CommandLogRecord( CommandStatusType.FAILURE, message,
                            "Verify that the file exists and is readable."));
            throw new CommandException ( message );
        }
        
        // Make sure that the intervals are the same
        
        int interval1 = StateMod_TS.getFileDataInterval ( InputFile1_full );
        int interval2 = StateMod_TS.getFileDataInterval ( InputFile2_full );

        // Intervals must be the same...
        if ( interval1 != interval2 ) {
            message = "Data intervals for the StateMod files are not the same";
            Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the StateMod data files have the same data interval." ) );
            throw new CommandException ( message );
        }
        
        // Read the StateMod files
        
        try {
            tslist1 = StateMod_TS.readTimeSeriesList ( InputFile1_full,
                InputStart_DateTime, InputEnd_DateTime, null, readData );
        }
        catch ( Exception e ) {
            message = "Error reading StateMod file \"" + InputFile1_full + "\".";
            Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
            Message.printWarning(3, routine, e);
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the file is a valid StateMod time series file." ) );
            throw new CommandException ( message );
        }
        try {
            tslist2 = StateMod_TS.readTimeSeriesList ( InputFile2_full,
                InputStart_DateTime, InputEnd_DateTime, null, readData );
        }
        catch ( Exception e ) {
            message = "Error reading StateMod file \"" + InputFile2_full + "\".";
            Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
            Message.printWarning(3, routine, e);
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the file is a valid StateMod time series file." ) );
            throw new CommandException ( message );
        }
        
        int size1 = 0;
        if ( tslist1 != null ) {
            size1 = tslist1.size();
        }
        int size2 = 0;
        if ( tslist2 != null ) {
            size2 = tslist2.size();
        }
        Message.printStatus ( 2, routine, "Read " + size1 + " StateMod time series from first file and " +
                size2 + " time series from second file." );
        
        if ( command_phase == CommandPhaseType.RUN ) {
            if ( size1 > 0 ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist1 );
                if ( wc > 0 ) {
                    message = "Error post-processing StateMod time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
                        status.addToLog ( command_phase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
                
                // Now perform the "max" processing...
                
                // Now loop through the time series in the first list and compare with
                // the matching time series in the second list, saving the maximum at
                // each time step...
                int vsize = 0;
                if ( tslist1 != null ) {
                    vsize = tslist1.size();
                }
                TS ts1 = null;
                int pos = 0;
                for ( int iv = 0; iv < vsize; iv++ ) {
                    // Get a time series...
                    ts1 = (TS)tslist1.get(iv);
                    if ( ts1 == null ) {
                        continue;
                    }
                    // Find the same time series in the second list...
                    pos = TSUtil.indexOf ( tslist2, ts1.getLocation(), "Location", 1 );
                    if ( pos < 0 ) {
                        message = "Cannot find matching 2nd time series for \"" +
                        ts1.getLocation() + "\" in \"" + InputFile2_full + "\"";
                        Message.printWarning ( warning_level,
                                MessageUtil.formatMessageTag(command_tag,
                                ++warning_count), routine, message );
                        status.addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Verify that the files contain matching time series identifiers." ) );
                    }
                    else {
                        // The "ts1" instance will be modified..
                        TSUtil.max ( ts1, (TS)tslist2.get(pos) );
                    }
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist1 );
                if ( wc2 > 0 ) {
                    message = "Error adding StateModMax to results time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
                        status.addToLog ( command_phase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
            }
        }
        else if ( command_phase == CommandPhaseType.DISCOVERY ) {
            // OK to use the identifiers from the first list.
            setDiscoveryTSList ( tslist1 );
        }
    }
    catch ( Exception e ) {
        Message.printWarning ( log_level, routine, e );
        message = "Unexpected error processing time series from StateMod files \"" + InputFile1_full + "\" and \"" +
        InputFile2_full + "\" (" + e + ").";
        Message.printWarning ( warning_level, 
        MessageUtil.formatMessageTag(command_tag, ++warning_count),
        routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "See log file for details." ) );
        throw new CommandException ( message );
    }
    
    status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String InputFile1 = props.getValue("InputFile1");
	String InputFile2 = props.getValue("InputFile2");
	StringBuffer b = new StringBuffer ();
	if ( (InputFile1 != null) && (InputFile1.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile1=\"" + InputFile1 + "\"" );
	}
	if ( (InputFile2 != null) && (InputFile2.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile2=\"" + InputFile2 + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
