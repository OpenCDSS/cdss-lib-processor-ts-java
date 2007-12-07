//------------------------------------------------------------------------------
// readNwsCard_Command - handle the readNwsCard() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-05-11	Luiz Teixeira, RTi	Initial version.
// 2005-05-16	Luiz Teixeira, RTi	Clean up and documentation.
// 2005-05-19	SAM, RTi		Move from TSTool package to TS.
// 2005-12-06	J. Thomas Sapienza, RTi	Added Read24HourAsDay parameter.
// 2005-12-12	JTS, RTi		readTimeSeries() call now passes in
//					a control PropList.
// 2006-01-04	JTS, RTi		Corrected many problems after review by
//					SAM.
// 2006-01-18	JTS, RTi		Moved from RTi.TS package.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------

package rti.tscommandprocessor.commands.nwsrfs;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.Vector;
import java.io.File;

import RTi.TS.TS;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

import RTi.DMI.NWSRFS_DMI.NWSCardTS;

/**
<p>
This class initializes, checks, and runs the TS Alias and non-TS Alias 
readNwsCard() commands.
</p>
*/
public class readNwsCard_Command 
extends AbstractCommand
implements Command {

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
Indicates whether the TS Alias version of the command is being used.
*/
protected boolean _use_alias = false;

/**
Constructor.
*/
public readNwsCard_Command ()
{
	super();
	setCommandName ( "ReadNwsCard" );
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
public void checkCommandParameters ( PropList parameters,
				     String command_tag,
				     int warning_level )
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
	String Read24HourAsDay = parameters.getValue("Read24HourAsDay");
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
            IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, InputFile));
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
			/*
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
			*/
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
/*		
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
*/			
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
    Vector valid_Vector = new Vector();
    if ( _use_alias ) {
        valid_Vector.add ( "Alias" );
    }
    valid_Vector.add ( "InputFile" );
    valid_Vector.add ( "InputStart" );
    valid_Vector.add ( "InputEnd" );
    valid_Vector.add ( "NewUnits" );
    valid_Vector.add ( "Read24HourAsDay" );
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
	return ( new readNwsCard_JDialog ( parent, this ) ).ok();
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

    // This is the new format of parsing, where parameters are
	// specified as "InputFilter=", etc.
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
 
 	// The following is for backwards compatability with old commands files.
    PropList parameters = getCommandParameters ();
	if (parameters.getValue("InputStart") == null) {
		parameters.set("InputStart", parameters.getValue("ReadStart"));
	}
	if ( parameters.getValue("InputEnd") == null) {
		parameters.set("InputEnd", parameters.getValue(	"ReadEnd"));
	}
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
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
       CommandWarningException,
       CommandException
{	String routine = "ReadNwsCard_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	Vector TSList = null;	// Keep the list of time series
    
    // Get and clear the status and clear the run log...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    CommandProcessor processor = getCommandProcessor();

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile");
	String NewUnits = parameters.getValue("NewUnits");
	// TODO SAM 2007-02-18 Need to enable InputStart and InputEnd handling.
	//String InputStart = _parameters.getValue("InputStart");
	//String InputEnd = _parameters.getValue("InputEnd");
	String Read24HourAsDay = parameters.getValue("Read24HourAsDay");
	String Alias = parameters.getValue("Alias");

	// Set the properties for NWSCardTS.readTimeSeries().
	PropList props = new PropList("NWSCardTS.readTimeSeries");
	props.set("Read24HourAsDay=" + Read24HourAsDay);

	// Read the NWS Card file.
	int TSCount = 0;
    String InputFile_full = InputFile;
	try {
        InputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),InputFile));
		TSList = NWSCardTS.readTimeSeriesList (
			// REVISIT [LT 2005-05-17] May add the TSID parameter 
			//	(1st parameter here) in the future.
			(TS) null,    		// Currently not used.
			InputFile_full, 		// String 	fname
			__InputStart,		// DateTime 	date1
			__InputEnd, 		// DateTime 	date2
			NewUnits,		// String 	units
			true,			// boolean 	read_data
			props);			// whether to read 24 hour 
						// as day.
			
		if ( TSList != null ) {
			TSCount = TSList.size();
			message = "Read \"" + TSCount + "\" time series from \"" + InputFile_full + "\"";
			Message.printStatus ( 2, routine, message );
			TS ts = null;
			for (int i = 0; i < TSCount; i++) {
				ts = (TS)TSList.elementAt(i);
				ts.setAlias(Alias);
			}
		}
	} 
	catch ( Exception e ) {
		message = "Error reading NWS Card File. \"" + InputFile_full + "\"";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Check the log file for details."));
		throw new CommandException ( message );
	}

	// Add the new time series to the TSResultsList.
	if ( TSList != null && TSCount > 0 ) {
		// Get the list of time series currently in the command processor.
		Vector TSResultsList_Vector = null;
		try { Object o = processor.getPropContents( "TSResultsList" );
				TSResultsList_Vector = (Vector)o;
		}
		catch ( Exception e ){
			message = "Cannot get time series list to add new time series.  Creating new list.";
            Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(
                        command_tag, ++warning_count ),
                    routine, message );
            status.addToLog(CommandPhaseType.RUN,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message,
                    "Report the problem to software support."));
		}
		if ( TSResultsList_Vector == null ) {
			// Not a warning because this can be the first command.
			TSResultsList_Vector = new Vector();
		}
		// Add to the time series...
		for ( int i=0; i<TSCount; i++ ) {
			TSResultsList_Vector.addElement ( TSList.elementAt(i) );
		}
		// Now set back in the processor...
		try {	processor.setPropContents ( "TSResultsList", TSResultsList_Vector );
		}
		catch ( Exception e ){
			message = "Cannot set updated time series list.  Skipping.";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag, ++warning_count),
				routine,message);
             status.addToLog(CommandPhaseType.RUN,
                        new CommandLogRecord(
                        CommandStatusType.FAILURE, message,
                        "Report the problem to software support."));
		}
	} 
	else {
		message = "Read zero time series from the NWS Card file \"" + InputFile_full + "\"";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
         status.addToLog(CommandPhaseType.RUN,
                 new CommandLogRecord(
                 CommandStatusType.FAILURE, message,
                 "Verify the format of the input file."));
	}

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count +" warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		throw new CommandWarningException ( message );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
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
	String NewUnits = props.getValue("NewUnits");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");
	String Read24HourAsDay = props.getValue("Read24HourAsDay");

	StringBuffer b = new StringBuffer ();

	// Input File
	if ((InputFile != null) && (InputFile.length() > 0)) {
		b.append("InputFile=\"" + InputFile + "\"");
	}

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

    String lead = "";
	if ( _use_alias && (Alias != null) && (Alias.length() > 0) ) {
		lead = "TS " + Alias + " = ";
	}

	return lead + getCommandName() + "(" + b.toString() + ")";
}

} // end readNwsCard_Command
