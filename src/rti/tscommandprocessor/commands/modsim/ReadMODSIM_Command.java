package rti.tscommandprocessor.commands.modsim;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
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

import RTi.TS.ModsimTS;

/**
<p>
This class initializes, checks, and runs the TS Alias = ReadMODSIM() and ReadMODSIM() commands.
</p>
*/
public class ReadMODSIM_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
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
private Vector __discovery_TS_Vector = null;

/**
Indicates whether the TS Alias version of the command is being used.
*/
protected boolean _useAlias = false;

/**
Constructor.
*/
public ReadMODSIM_Command ()
{
	super();
	setCommandName ( "ReadMODSIM" );
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
	String NewUnits  = parameters.getValue("NewUnits");
	String InputStart = parameters.getValue("InputStart");
	String InputEnd   = parameters.getValue("InputEnd");
	String Alias = parameters.getValue("Alias");
	String TSID = parameters.getValue("TSID");
    
	if ( _useAlias && ((Alias == null) || Alias.equals("")) ) {
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
    
    if ( _useAlias ) {
        if ( (TSID == null) || TSID.equals("") ) {
            message = "The requested time series identifier must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a node/link name and data type." ) );
        }
        else {
            TSIdent tsident = null;
            try {
                tsident = new TSIdent ( TSID );
                String NodeName = tsident.getLocation();
                String DataType = tsident.getType();
                if ( NodeName.equals("") ) {
                    warning +=
                        "Node/Link name must be set, or press Cancel";
                }
                if ( DataType.equals("") ) {
                    warning +=
                        "Data type must be set, or press Cancel";
                }
            }
            catch ( Exception e ) {
                message = "Unable to determine TSID parts from \"" + TSID + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that node/link name and data type are specified." ) );
            }
        }
    }

    if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
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

	if ( NewUnits != null ) {
		// Will check at run time
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
		/*
		if (__InputStart != null) {
			if (__InputStart.getPrecision() != DateTime.PRECISION_HOUR) {
                message = "The input start date/time \"" + InputStart + "\" precision is not hour.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the input start to hour precision." ) );
			}		
		}
		*/
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
		/*
		if (__InputEnd != null) {
			if (__InputEnd.getPrecision() != DateTime.PRECISION_HOUR) {
                message = "The input end date/time \"" + InputStart + "\" precision is not hour.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the input end to hour precision." ) );
			}
		}     */
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
    Vector valid_Vector = new Vector();
    if ( _useAlias ) {
        valid_Vector.add ( "Alias" );
        valid_Vector.add ( "TSID" );
    }
    valid_Vector.add ( "InputFile" );
    valid_Vector.add ( "InputStart" );
    valid_Vector.add ( "InputEnd" );
    // TODO SAM 2008-09-09 Enable in version 8 MODSIM files?
    //valid_Vector.add ( "NewUnits" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

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
	return ( new ReadMODSIM_JDialog ( parent, this ) ).ok();
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
private Vector getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
    Vector discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    TS datats = (TS)discovery_TS_Vector.elementAt(0);
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
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	
	String routine = "ReadMODSIM_Command.parseCommand", message;
	
    String Alias = null;
	int warning_count = 0;
	_useAlias = false;
    if (StringUtil.startsWithIgnoreCase(command_string, "TS ")) {
        // There is an alias specified.  Extract the alias from the full command.
        _useAlias = true;
        String str = command_string.substring(3); // Alias = command()
        int index = str.indexOf("=");
        int index2 = str.indexOf("(");
        if (index2 < index) {
            // no alias specified -- badly-formed command
            Alias = "Invalid_Alias";
            message = "No alias was specified, although the command started with \"TS ...\"";
            Message.printWarning(warning_level, routine, message);
                ++warning_count;
            throw new InvalidCommandSyntaxException(message);
        }

        Alias = str.substring(0, index).trim();
        // Remainder containing the command name and parameters (old or new style) parse below...
        command_string = str.substring(index+1).trim();
    }
    Message.printStatus(2,routine,"useAlias=" + _useAlias + " command string=\"" + command_string + "\"");
    // In any case parse the command parameters
    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
        // TODO SAM 2008-09-09 This whole block of code needs to be
        // removed as soon as commands have been migrated to the new syntax.
        Vector v = StringUtil.breakStringList(command_string, "(),", StringUtil.DELIM_ALLOW_STRINGS );
        int ntokens = 0;
        if ( v != null ) {
            ntokens = v.size();
        }
        Message.printStatus (2,routine,"numtokens="+ntokens);
        String InputFile = "";
        String TSID = "";
        String NewUnits = "";
        String InputStart = "";
        String InputEnd = "";
        if ( _useAlias ) {
            if ( ntokens >= 2 ) {
                InputFile = ((String)v.elementAt(1)).trim();
            }
            if ( ntokens >= 3 ) {
                TSID = ((String)v.elementAt(2)).trim();
            }
            if ( ntokens >= 4 ) {
                NewUnits = ((String)v.elementAt(3)).trim();
            }
            if ( ntokens >= 5 ) {
                InputStart = ((String)v.elementAt(4)).trim();
            }
            if ( ntokens >= 6 ) {
                InputEnd = ((String)v.elementAt(5)).trim();
            }
        }
        else {
            if ( ntokens >= 2 ) {
                // Only one parameter was supported...
                InputFile = ((String)v.elementAt(1)).trim();
            }
        }
        Message.printStatus (2,routine,"TSID="+TSID);

        // Set parameters and new defaults...

        PropList parameters = new PropList ( getCommandName() );
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        if ( InputFile.length() > 0 ) {
            parameters.set ( "InputFile", InputFile );
        }
        if ( TSID.length() > 0 ) {
            parameters.set ( "TSID", TSID );
        }
        if ( NewUnits.length() > 0 ) {
            parameters.set ( "NewUnits", NewUnits );
        }
        if ( InputStart.length() > 0 ) {
            if ( InputStart.equals("*") ) {
                InputStart = "";
            }
            parameters.set ( "InputStart", InputStart );
        }
        if ( InputEnd.length() > 0 ) {
            if ( InputEnd.equals("*") ) {
                InputEnd = "";
            }
            parameters.set ( "InputEnd", InputEnd );
        }
        parameters.setHowSet ( Prop.SET_UNKNOWN );
        setCommandParameters ( parameters );
    }
    
    // Set alias in addition to parameters set above...

    if ( _useAlias ) {
        PropList parameters = getCommandParameters();
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        if ( (Alias != null) && (Alias.length() > 0) ) {
            parameters.set ( "Alias", Alias );
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
Run the command.
@param command_number The number of the command being run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
       CommandWarningException,
       CommandException
{	String routine = "ReadMODSIM_Command.runCommand", message;
	int warning_level = 2;
    int log_level = 3;
	String command_tag = "" + command_number;
	int warning_count = 0;
	    
    // Get and clear the status and clear the run log...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    CommandProcessor processor = getCommandProcessor();

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile");
	String NewUnits = parameters.getValue("NewUnits");
	String InputStart = parameters.getValue("InputStart");
	String InputEnd = parameters.getValue("InputEnd");
	String Alias = parameters.getValue("Alias");
	String TSID = parameters.getValue("TSID");
    
    DateTime InputStart_DateTime = null;
    DateTime InputEnd_DateTime = null;
    if ( (InputStart != null) && (InputStart.length() != 0) ) {
        try {
        PropList request_params = new PropList ( "" );
        request_params.set ( "DateTime", InputStart );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting InputStart DateTime(DateTime=" + InputStart + ") from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
            throw new InvalidCommandParameterException ( message );
        }

        PropList bean_PropList = bean.getResultsPropList();
        Object prop_contents = bean_PropList.getContents ( "DateTime" );
        if ( prop_contents == null ) {
            message = "Null value for InputStart DateTime(DateTime=" + InputStart + ") returned from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the specified date/time is valid." ) );
            throw new InvalidCommandParameterException ( message );
        }
        else {  InputStart_DateTime = (DateTime)prop_contents;
        }
    }
    catch ( Exception e ) {
        message = "InputStart \"" + InputStart + "\" is invalid.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time for the input start, " +
                        "or InputStart for the global input start." ) );
        throw new InvalidCommandParameterException ( message );
    }
    }
    else {
        // Get the global input start from the processor...
        try {
            Object o = processor.getPropContents ( "InputStart" );
                if ( o != null ) {
                    InputStart_DateTime = (DateTime)o;
                }
        }
        catch ( Exception e ) {
            message = "Error requesting the global InputStart from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
            throw new InvalidCommandParameterException ( message );
        }
    }
    
    if ( (InputEnd != null) && (InputEnd.length() != 0) ) {
        try {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", InputEnd );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting InputEnd DateTime(DateTime=" + InputEnd + ") from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }

            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for InputEnd DateTime(DateTime=" + InputEnd +  ") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the end date/time is valid." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {
                InputEnd_DateTime = (DateTime)prop_contents;
            }
        }
        catch ( Exception e ) {
            message = "InputEnd \"" + InputEnd + "\" is invalid.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time for the input end, " +
                            "or InputEnd for the global input start." ) );
            throw new InvalidCommandParameterException ( message );
        }
        }
        else {
            // Get from the processor...
            try {
                Object o = processor.getPropContents ( "InputEnd" );
                    if ( o != null ) {
                        InputEnd_DateTime = (DateTime)o;
                    }
            }
            catch ( Exception e ) {
                message = "Error requesting the global InputEnd from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
            }
    }

	// Read the MODSIM file.
	TS ts = null; // Single time series
    Vector tslist = null;   // Keep the list of time series
    String InputFile_full = InputFile;
	try {
        boolean read_data = true;
        if ( command_phase == CommandPhaseType.DISCOVERY ){
            read_data = false;
        }
        InputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                        TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        if ( !IOUtil.fileExists(InputFile_full)) {
            message = "The MODSIM file \"" + InputFile_full + "\" does not exist.";
            status.addToLog(command_phase,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,"Verify that the filename is correct."));
        }
        else {
            if ( _useAlias ) {
                ts = ModsimTS.readTimeSeries ( TSID, InputFile_full, InputStart_DateTime, InputEnd_DateTime,
                        NewUnits, read_data );
                if ( ts != null ) {
                    ts.setAlias( Alias );
                    tslist = new Vector ( 1 );
                    tslist.addElement ( ts );
                }
            }
            else {
                tslist = ModsimTS.readTimeSeriesList (
                    InputFile_full, InputStart_DateTime, InputEnd_DateTime, NewUnits, read_data );
            }
        }
	} 
	catch ( Exception e ) {
		message = "Unexpected error reading MODSIM file. \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count ),routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(command_phase,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,"Verify that the file is a valid MODSIM time series file."));
		throw new CommandException ( message );
	}
    
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    message = "Read \"" + size + "\" time series from \"" + InputFile_full + "\"";
    Message.printStatus ( 2, routine, message );
    
    if ( size == 0 ) {
        message = "No MODSIM time series were read.";
        Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag,
            ++warning_count), routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message,
                            "Verify that the file is a valid MODSIM time series file with expected identifiers." ) );
        throw new CommandException ( message );
    }

    if ( command_phase == CommandPhaseType.RUN ) {
        if ( tslist != null ) {
            // Further process the time series...
            // This makes sure the period is at least as long as the output period...
            int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
            if ( wc > 0 ) {
                message = "Error post-processing MODSIM time series after read.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( command_phase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
    
            // Now add the list in the processor...
            
            int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
            if ( wc2 > 0 ) {
                message = "Error adding MODSIM time series after read.";
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
        setDiscoveryTSList ( tslist );
    }

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count ), routine, message );
		throw new CommandWarningException ( message );
	}
    
    status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( Vector discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
	    if ( _useAlias ) {
	        return "TS Alias = " + getCommandName() + "()";
	    }
	    else {
	        return getCommandName() + "()";
	    }
	}

	String Alias = props.getValue("Alias");
	String InputFile = props.getValue("InputFile" );
	String TSID = props.getValue("TSID" );
	//String NewUnits = props.getValue("NewUnits");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");

	StringBuffer b = new StringBuffer ();

	// Input File
	if ((InputFile != null) && (InputFile.length() > 0)) {
		b.append("InputFile=\"" + InputFile + "\"");
	}
	if ( _useAlias ) {
	    if ((TSID != null) && (TSID.length() > 0)) {
	        if (b.length() > 0) {
	            b.append(",");
	        }
	        b.append("TSID=\"" + TSID + "\"");
	    }
	}

	// New Units
	/*
	if ((NewUnits != null) && (NewUnits.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("NewUnits=\"" + NewUnits + "\"");
	}
	*/

	// Input Start
	if ((InputStart != null) && (InputStart.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("InputStart=\"" + InputStart + "\"");
	}

	// Input End
	if ((InputEnd != null) && (InputEnd.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("InputEnd=\"" + InputEnd + "\"");
	}

    String lead = "";
	if ( _useAlias && (Alias != null) && (Alias.length() > 0) ) {
		lead = "TS " + Alias + " = ";
	}

	return lead + getCommandName() + "(" + b.toString() + ")";
}

}
