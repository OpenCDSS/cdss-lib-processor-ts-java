package rti.tscommandprocessor.commands.waterml;

import javax.swing.JFrame;

import rti.tscommandprocessor.commands.wateroneflow.waterml.WaterMLReader;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
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
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the ReadWaterML() commands.
*/
public class ReadWaterML_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{

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
private List<TS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public ReadWaterML_Command ()
{
	super();
	setCommandName ( "ReadWaterML" );
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
	//String NewUnits = parameters.getValue("NewUnits");
	String InputStart = parameters.getValue("InputStart");
	String InputEnd = parameters.getValue("InputEnd");
	String Alias = parameters.getValue("Alias");
	String Interval = parameters.getValue( "Interval" );
    
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
    else {
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
    
	// Check for invalid parameters...
	List<String> valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "InputFile" );
    valid_Vector.add ( "InputStart" );
    valid_Vector.add ( "InputEnd" );
    valid_Vector.add ( "Interval" );
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
	return ( new ReadWaterML_JDialog ( parent, this ) ).ok();
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
*/
public List getObjectList ( Class c )
{
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // First time series in list should be of a type that be requested (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
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
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = "ReadWaterML_Command.runCommand", message;
	int warning_level = 2;
    int log_level = 3;
	String command_tag = "" + command_number;
	int warning_count = 0;
	    
    // Get and clear the status and clear the run log...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }
    CommandProcessor processor = getCommandProcessor();

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile");
	//String NewUnits = parameters.getValue("NewUnits");
	String InputStart = parameters.getValue("InputStart");
	String InputEnd = parameters.getValue("InputEnd");
	String Alias = parameters.getValue("Alias");
    // The following is necessary because WaterML (1.1 at least) does not appear to have a clear indicator of
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
    else {  // Get the global input start from the processor...
        try {   Object o = processor.getPropContents ( "InputStart" );
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
            else {  InputEnd_DateTime = (DateTime)prop_contents;
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
        else {  // Get from the processor...
            try {   Object o = processor.getPropContents ( "InputEnd" );
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

	// Read the file.
    List<TS> tslist = null;   // Keep the list of time series
    String InputFile_full = InputFile;
	try {
        boolean readData = true;
        if ( command_phase == CommandPhaseType.DISCOVERY ){
            readData = false;
        }
        InputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        if ( !IOUtil.fileExists(InputFile_full) ) {
            message = "Input file does not exist:  \"" + InputFile_full + "\".";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that filename is correct and that the file exists." ) );
        }
        else {
            // Read everything in the file.
            StringBuilder fileContents = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(InputFile_full));
            String ls = System.getProperty ( "line.separator" );
            String line;
            int count = 0;
            while ( (line = reader.readLine()) != null ) {
                ++count;
                if ( count > 1 ) {
                    fileContents.append(ls);
                }
                fileContents.append(line);
            }
            reader.close();
            WaterMLReader watermlReader = new WaterMLReader ( fileContents.toString(), null, new File(InputFile_full) );
            tslist = watermlReader.readTimeSeriesList( interval, InputStart_DateTime, InputEnd_DateTime, readData );
        }
			
		if ( tslist != null ) {
			int tscount = tslist.size();
			message = "Read " + tscount + " time series from \"" + InputFile_full + "\"";
			Message.printStatus ( 2, routine, message );
	        if ( (Alias != null) && !Alias.equals("") ) {
                for ( TS ts : tslist ) {
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, Alias, status, command_phase);
                    ts.setAlias ( alias );
                }
            }
		}
	}
	/*
	catch ( FileNotFoundException e ) {
        message = "WaterML file \"" + InputFile_full + "\" is not found or accessible.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
            status.addToLog(command_phase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,
                    "Verify that the file exists and is readable."));
        throw new CommandException ( message );
	}*/
	catch ( Exception e ) {
		message = "Unexpected error reading WaterML file. \"" + InputFile_full + "\" (" + e + ")";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(command_phase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,"Check the log file for details."));
		throw new CommandException ( message );
	}
    
    if ( command_phase == CommandPhaseType.RUN ) {
        if ( tslist != null ) {
            // Further process the time series...
            // This makes sure the period is at least as long as the output period...
            int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
            if ( wc > 0 ) {
                message = "Error post-processing series after read.";
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
                message = "Error adding time series after read.";
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
private void setDiscoveryTSList ( List discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
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
	//String NewUnits = props.getValue("NewUnits");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");
	String Interval = props.getValue("Interval");

	StringBuffer b = new StringBuffer ();

	// Input File
	if ((InputFile != null) && (InputFile.length() > 0)) {
		b.append("InputFile=\"" + InputFile + "\"");
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
    if ((Interval != null) && (Interval.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Interval=\"" + Interval + "\"");
    }

    return getCommandName() + "("+ b.toString()+")";
}

}