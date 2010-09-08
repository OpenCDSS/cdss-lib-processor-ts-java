package rti.tscommandprocessor.commands.nwsrfs;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSEnsemble;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
//import RTi.Util.Time.DateTime;

import RTi.DMI.NWSRFS_DMI.NWSRFS_ESPTraceEnsemble;

/**
<p>
This class initializes, checks, and runs the TS Alias and non-TS Alias 
ReadNwsrfsEspTraceEnsemble() commands.  Only the non-alias version is enabled.
</p>
*/
public class ReadNwsrfsEspTraceEnsemble_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

protected static final String
	_FALSE = "False",
	_TRUE = "True";

/**
Private data members shared between the checkCommandParameters() and the 
runCommand() methods (prevent code duplication parsing DateTime strings).  
*/
//private DateTime __InputStart = null;
//private DateTime __InputEnd   = null;

/**
TSEnsemble created in discovery mode (to provide the identifier for other commands).
*/
private TSEnsemble __tsensemble = null;

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Indicates whether the TS Alias version of the command is being used.
*/
protected boolean _use_alias = false;

/**
Constructor.
*/
public ReadNwsrfsEspTraceEnsemble_Command ()
{
	super();
	setCommandName ( "ReadNwsrfsEspTraceEnsemble" );
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
{
    String routine = getClass().getName() + ".checkCommandParameters";
	String warning = "";
    String message;
    
    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Get the property values. 
	String InputFile = parameters.getValue("InputFile");
    String EnsembleID = parameters.getValue("EnsembleID");
    /*
	String NewUnits  = parameters.getValue("NewUnits");
	String InputStart = parameters.getValue("InputStart");
	String InputEnd   = parameters.getValue("InputEnd");
	String Read24HourAsDay = parameters.getValue("Read24HourAsDay");
    */
	String Alias = parameters.getValue("Alias");
    
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

    if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
    }
    else {  String working_dir = null;
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
            message = "The output file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
        }
    }
    
    if ( (EnsembleID == null) || (EnsembleID.length() == 0) ) {
        message = "An ensemble identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an ensemble identifier." ) );
    }

    /*
	if ( NewUnits != null ) {
		// Will check at run time
	}

	boolean read24HourAsDay = false;

	// Read24HourAsDay
	if ((Read24HourAsDay != null) && !Read24HourAsDay.equals("")) {
		if (Read24HourAsDay.equalsIgnoreCase("true")) {
			// valid entry
			read24HourAsDay = true;
		}
		else if (Read24HourAsDay.equalsIgnoreCase("false") || Read24HourAsDay.trim().equals("")) {
		    	// valid entry
		    	read24HourAsDay = false;
		}
		else {
			// invalid value -- will default to false, but report a warning.
            message = "The value to specify whether to convert "
                + "24 Hour data to Daily should be blank, or "
                + "one of \"True\" or \"False\", not \""
                + Read24HourAsDay + "\"";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify True, False, or blank." ) );

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
			if (__InputStart.getPrecision() != DateTime.PRECISION_HOUR) {
                message = "The input start date/time \"" + InputStart + "\" precision is not hour.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the input start to hour precision." ) );
			}		

			// When reading 24 hour as day, 24 is a valid hour
			// for the current day (because of how the NWS does
			// things), but RTi's DateTime class automatically 
			// parses hour 24 as being hour 0 of the next day, so
			// decrement the __InputStart day by 1 to account for
			// this.			
			if (read24HourAsDay) {
				if (InputStart.endsWith(" 24")) {
					__InputStart.addDay(-1);
				}
			}
			/ *
			if (!read24HourAsDay && __InputStart.getPrecision() 
			    != DateTime.PRECISION_HOUR) {
				warning += "\nThe input start date/time \""
					+ InputStart
					+ "\" precision is not hour.";
			}
			else if (read24HourAsDay && __InputStart.getPrecision() 
			    != DateTime.PRECISION_DAY) {
				warning += "\nThe input start date/time \""
					+ InputStart
					+ "\" precision is not day.";
			}
			* /
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
			if (__InputEnd.getPrecision() != DateTime.PRECISION_HOUR) {
                message = "The input end date/time \"" + InputStart + "\" precision is not hour.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the input end to hour precision." ) );
			}		

			// When reading 24 hour as day, 24 is a valid hour
			// for the current day (because of how the NWS does
			// things), but RTi's DateTime class automatically 
			// parses hour 24 as being hour 0 of the next day, so
			// decrement the __InputEnd day by 1 to account for
			// this.
			if (read24HourAsDay) {
				if (InputEnd.endsWith(" 24")) {
					__InputEnd.addDay(-1);
				}
			}			
/ *		
			if (!read24HourAsDay && __InputEnd.getPrecision() 
			    != DateTime.PRECISION_HOUR) {
				warning += "\nThe input end date/time \""
					+ InputEnd
					+ "\" precision is not hour.";
			}
			else if (read24HourAsDay && __InputEnd.getPrecision() 
			    != DateTime.PRECISION_DAY) {
				warning += "\nThe input end date/time \""
					+ InputEnd
					+ "\" precision is not day.";
			}			
* /			
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
        */
    
	// Check for invalid parameters...
    List valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "InputFile" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "EnsembleName" );
    //valid_Vector.add ( "InputStart" );
    //valid_Vector.add ( "InputEnd" );
    //valid_Vector.add ( "NewUnits" );
    //valid_Vector.add ( "Read24HourAsDay" );
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
	return ( new ReadNwsrfsEspTraceEnsemble_JDialog ( parent, this ) ).ok();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
	//__InputStart = null;
	//__InputEnd   = null;
	super.finalize();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private TSEnsemble getDiscoveryEnsemble()
{
    return __tsensemble;
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
{   List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    TS datats = null;
    // Since all time series must be the same interval, check the class for the first one (e.g., HourTS)
    if ( (discovery_TS_Vector != null) && (discovery_TS_Vector.size() > 0) ) {
        datats = discovery_TS_Vector.get(0);
    }
    // Use the most generic for the base class...
    if ( (c == TS.class) || ((datats != null) && (c == datats.getClass())) ) {
        // Get the list of time series...
        if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
            return null;
        }
        else {
            return discovery_TS_Vector;
        }
    }
    else if ( c == TSEnsemble.class ) {
        TSEnsemble ensemble = getDiscoveryEnsemble();
        if ( ensemble == null ) {
            return null;
        }
        else {
        	List<TSEnsemble> v = new Vector();
            v.add ( ensemble );
            return v;
        }
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
	
	int index = command_string.indexOf("(");
	String str = command_string.substring(index);
	index = str.indexOf("=");

    // This is the new format of parsing, where parameters are specified as "InputFilter=", etc.
	String routine = "ReadNwsCard_Command.parseCommand", message;
	
    String Alias = null;
	int warning_count = 0;
    if (StringUtil.startsWithIgnoreCase(command_string, "TS ")) {
        // There is an alias specified.  Extract the alias from the full command.
        _use_alias = true;
        str = command_string.substring(3); 
        index = str.indexOf("=");
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
        // Parse the command parameters...
        super.parseCommand ( command_string.substring(index+1).trim() );
        PropList parameters = getCommandParameters();
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        parameters.set ( "Alias", Alias );
        parameters.setHowSet ( Prop.SET_UNKNOWN );
        setCommandParameters ( parameters );
    }
    else {
        _use_alias = false;
        super.parseCommand ( command_string );
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
{	String routine = "ReadNwsrfsEspTraceEnsemble_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	    
    // Get and clear the status and clear the run log...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    CommandProcessor processor = getCommandProcessor();

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile");
    String EnsembleID = parameters.getValue("EnsembleID");  // Get from file?
    String EnsembleName = parameters.getValue("EnsembleName");
    if ( EnsembleName == null ) {
        EnsembleName = "";
    }
    String Alias = parameters.getValue("Alias");
	//String NewUnits = parameters.getValue("NewUnits");
	// TODO SAM 2007-02-18 Need to enable InputStart and InputEnd handling.
	//String InputStart = _parameters.getValue("InputStart");
	//String InputEnd = _parameters.getValue("InputEnd");
	//String Read24HourAsDay = parameters.getValue("Read24HourAsDay");
	
	//props.set("Read24HourAsDay=" + Read24HourAsDay);

	// Read the ensemble file.
    List tslist = null;   // Keep the list of time series
    String InputFile_full = InputFile;
	try {
        boolean read_data = true;
        if ( command_phase == CommandPhaseType.DISCOVERY ){
            read_data = false;
        }
        InputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                        TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        NWSRFS_ESPTraceEnsemble ensemble = new NWSRFS_ESPTraceEnsemble ( InputFile_full, read_data );
        tslist = ensemble.getTimeSeriesVector ();
		int tscount = 0;
		if ( tslist != null ) {
			tscount = tslist.size();
			message = "Read \"" + tscount + "\" time series from \"" + InputFile_full + "\"";
			Message.printStatus ( 2, routine, message );
		}
		if ( (Alias != null) && (Alias.length() > 0) ) {
    		for ( int i = 0; i < tscount; i++ ) {
    		    TS ts = (TS)tslist.get(i);
    		    if ( ts == null ) {
    		        continue;
    		    }
    		    if ( _use_alias ) {
    		        // Reading a single time series so set the alias
    		        ts.setAlias ( Alias );
    		    }
    		    else {
    		        // Set the alias to the desired string.
    		        ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString(
    		                processor, ts, Alias, status, command_phase) );
    		    }
    		}
		}
	}
    catch ( FileNotFoundException e ) {
        message = "File does not exist for NWSRFS ensemble file: \"" + InputFile_full + "\"";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count ),routine, message );
        status.addToLog(command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE, message,"Verify that the file exists."));
        throw new CommandException ( message );
    }
	catch ( Exception e ) {
		message = "Unexpected error reading NWSRFS ensemble file. \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count ),routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE, message,"Check the log file for details."));
		throw new CommandException ( message );
	}
    
    if ( command_phase == CommandPhaseType.RUN ) {
        if ( tslist != null ) {
            // Further process the time series...
            // This makes sure the period is at least as long as the output period...
            int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
            if ( wc > 0 ) {
                message = "Error post-processing NWS ensemble time series after read.";
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
                message = "Error adding ensemble time series after read.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( command_phase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
        }
        // Create an ensemble and add to the processor...
        
        TSEnsemble ensemble = new TSEnsemble ( EnsembleID, EnsembleName, tslist );
        TSCommandProcessorUtil.appendEnsembleToResultsEnsembleList(processor, this, ensemble);
    }
    else if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( tslist );
        // Just want the identifier...
        TSEnsemble ensemble = new TSEnsemble ( EnsembleID, EnsembleName, null );
        setDiscoveryEnsemble ( ensemble );
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
Set the ensemble that is processed by this class in discovery mode.
*/
private void setDiscoveryEnsemble ( TSEnsemble tsensemble )
{
    __tsensemble = tsensemble;
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
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

	String Alias = props.getValue("Alias");
	String InputFile = props.getValue("InputFile" );
    String EnsembleID = props.getValue("EnsembleID" );
    String EnsembleName = props.getValue("EnsembleName" );
    /*
	String NewUnits = props.getValue("NewUnits");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");
	String Read24HourAsDay = props.getValue("Read24HourAsDay");
    */

	StringBuffer b = new StringBuffer ();

	// Input File
	if ((InputFile != null) && (InputFile.length() > 0)) {
		b.append("InputFile=\"" + InputFile + "\"");
	}
    if ((EnsembleID != null) && (EnsembleID.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("EnsembleID=\"" + EnsembleID + "\"");
    }
    if ((EnsembleName != null) && (EnsembleName.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("EnsembleName=\"" + EnsembleName + "\"");
    }

    /*
	// New Units
	if ((NewUnits != null) && (NewUnits.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("NewUnits=\"" + NewUnits + "\"");
	}

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

	if (Read24HourAsDay != null && Read24HourAsDay.length() > 0) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("Read24HourAsDay=" + Read24HourAsDay + "");
	}
    */

    String lead = "";
	if ( _use_alias && (Alias != null) && (Alias.length() > 0) ) {
		lead = "TS " + Alias + " = ";
	}
	else {
	    // Add alias like any other parameter
	    if ((Alias != null) && (Alias.length() > 0)) {
	        if (b.length() > 0) {
	            b.append(",");
	        }
	        b.append("Alias=\"" + Alias + "\"");
	    }
	}

	return lead + getCommandName() + "(" + b.toString() + ")";
}

}
