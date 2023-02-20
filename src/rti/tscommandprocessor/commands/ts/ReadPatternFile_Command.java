// ReadPatternFile_Command - This class initializes, checks, and runs the ReadPatternFile() command

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

package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.util.List;
import java.util.Vector;

import DWR.StateMod.StateMod_TS;
import RTi.TS.StringMonthTS;
import RTi.TS.TS;
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
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.IOUtil;
import RTi.Util.String.StringUtil;

/**
<p>
This class initializes, checks, and runs the ReadPatternFile() command, which is used with the
FillPattern() command.  This command replaces the SetPatternFile() command.
</p>
*/
public class ReadPatternFile_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<StringMonthTS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public ReadPatternFile_Command ()
{	super();
	setCommandName ( "ReadPatternFile" );
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
{	String PatternFile = parameters.getValue ( "PatternFile" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// If the input file does not exist, warn the user...

	String working_dir = null;
	
	CommandProcessor processor = getCommandProcessor();
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
	
	if ( (PatternFile == null) || (PatternFile.length() == 0) ) {
        message = "The pattern file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing pattern file." ) );
	}
	else {
        try {
            String adjusted_path = IOUtil.verifyPathForOS (IOUtil.adjustPath ( working_dir, PatternFile) );
			File f = new File ( adjusted_path );
			if ( !f.exists() ) {
                message = "The pattern file does not exist:  \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the pattern file exists - may be OK if created at run time." ) );
			}
			f = null;
		}
		catch ( Exception e ) {
            message = "The pattern file:\n" +
            "    \"" + PatternFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that pattern file and working directory paths are compatible." ) );
		}
	}
 
	// TODO SAM 2008-09-17 Check the format.
    
	//  Check for invalid parameters...
	List<String> valid_Vector = new Vector<String>(1);
    valid_Vector.add ( "PatternFile" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );    

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
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
{	// The command will be modified if changed...
	return (new ReadPatternFile_JDialog ( parent, this )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<StringMonthTS> getDiscoveryTSList ()
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
	List<StringMonthTS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., StringMonthTS)
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
Parse the command string into a PropList of parameters.  This method
supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{   int warning_level = 2;
    String routine = "ReadPatternFile_Command.parseCommand", message;

    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
        // TODO SAM 2008-09-17 This whole block of code needs to be
        // removed as soon as commands have been migrated to the new syntax.
        //
        // Old syntax where the only parameter is a single filename to read.
    	List<String> v = StringUtil.breakStringList(command_string,
            "(),\t", StringUtil.DELIM_SKIP_BLANKS | StringUtil.DELIM_ALLOW_STRINGS );
        int ntokens = 0;
        if ( v != null ) {
            ntokens = v.size();
        }
        if ( ntokens != 2 ) {
            // Command name, TSID, and constant...
            message = "Syntax error in \"" + command_string +
            "\".  Expecting ReadPatternFile(PatternFile).";
            Message.printWarning ( warning_level, routine, message);
            throw new InvalidCommandSyntaxException ( message );
        }

        // Get the individual tokens of the expression...

        String PatternFile = ((String)v.get(1)).trim();

        // Set parameters and new defaults...

        PropList parameters = new PropList ( getCommandName() );
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        if ( PatternFile.length() > 0 ) {
            parameters.set ( "PatternFile", PatternFile );
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
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ReadPatternFile_Command.runCommand",message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String PatternFile = parameters.getValue ( "PatternFile" );

	String PatternFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),PatternFile) );
	if ( !IOUtil.fileExists(PatternFile_full) ) {
		message += "\nThe pattern file \"" + PatternFile_full + "\" does not exist.";
		++warning_count;
        status.addToLog ( command_phase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the pattern file exists." ) );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	// Now process the file...

	List<StringMonthTS> tslist = null;
	try {
	    Message.printStatus ( 2, routine, "Using \"" + PatternFile_full + "\" for fill pattern file." );
	    // Read the fill pattern file.  Since multiple files may be read with multiple commands,
	    // create a temporary Vector and then append to the main vector...
	    tslist = StateMod_TS.readPatternTimeSeriesList( PatternFile_full, true );
	    if ( tslist == null ) {
	        message = "Read zero time series from pattern file \"" + PatternFile_full + "\".";
	        status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that file format is correct." ) );
	    }
	    else {
	        int listsize = tslist.size();
	        Message.printStatus ( 2, routine,
	        "Read "+listsize+" pattern time series from \""+ PatternFile_full + "\".  Pattern identifiers are:" );
	        for ( int i = 0; i < listsize; i++ ) {
	            Message.printStatus ( 2, routine, ((TS)tslist.get(i)).getLocation() );
	        }
	    }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error reading pattern file \"" + PatternFile_full + "\" (" + e + ").";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the file exists and is readable." ) );
		throw new CommandWarningException ( message );
	}
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
		throw new CommandWarningException ( message );
	}
    
    // Set the time series in the processor...
    
    if ( (tslist != null) && (command_phase == CommandPhaseType.RUN) ) {
        PropList request_params = new PropList ( "" );
        request_params.setUsingObject ( "TSList", tslist );
        //CommandProcessorRequestResultsBean bean = null;
        try {
            processor.processRequest( "SetPatternTSList", request_params);
        }
        catch ( Exception e ) {
            message = "Error processing request SetPatternTSList(TSList=...).";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
        }
    }
    else if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( tslist );
    }

    status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<StringMonthTS> discovery_TS_Vector ) {
    __discovery_TS_Vector = discovery_TS_Vector;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"PatternFile"
	};
	return this.toString(parameters, parameterOrder);
}

}