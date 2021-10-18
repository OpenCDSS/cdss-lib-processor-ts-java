// ReadStateModB_Command - handle the ReadStateModB() and TS Alias = ReadStateModB() commands

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

//------------------------------------------------------------------------------
// readStateModB_Command - handle the readStateModB() and
//				TS Alias = readStateModB() commands
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-09-29	Steven A. Malers, RTi	Initial version.  Copy and modify
//					readStateMod().  Update the original
//					syntax to add the InputStart, InputEnd,
//					and Version parameters.
// 2005-12-21	SAM, RTi		Enable passing the version to the
//					StateMod_BTS constructor.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.statemod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

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
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

import DWR.StateMod.StateMod_BTS;

/**
This class initializes, checks, and runs the ReadStateModB() command.
*/
public class ReadStateModB_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Data members used for parameter values.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

/**
 * Values for OutputVersion parameter.
 */
protected final String _Latest = "Latest";
protected final String _Original = "Original";

/**
 * Indicates whether the TS Alias version of the command is being used.
 */
protected boolean _use_alias = false;

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public ReadStateModB_Command ()
{	super();
	setCommandName ( "ReadStateModB" );
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
{	String InputFile = parameters.getValue ( "InputFile" );
	//String TSID = parameters.getValue ( "TSID" );
	String InputStart = parameters.getValue ( "InputStart" );
	String InputEnd = parameters.getValue ( "InputEnd" );
	String Version = parameters.getValue ( "Version" );
	String OutputVersion = parameters.getValue ( "OutputVersion" );
	String IfFileNotFound = parameters.getValue ( "IfFileNotFound" );
	String warning = "";
    String message;

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
	}
	else if ( InputFile.indexOf("${") < 0 ) { // Does not contain property so can check for existence
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
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)) );
                File f = new File ( adjusted_path );
                if ( !f.exists() ) {
                    message = "The input file does not exist:  \"" + adjusted_path + "\".";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.WARNING,
                                    message, "Verify that the input file exists - may be OK if created at run time." ) );
                }
        }
        catch ( Exception e ) {
            message = "The input file \"" + InputFile +
            "\" cannot be adjusted using the working directory \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
        }
    }
	if ( (InputStart != null) && !InputStart.equals("") &&
		!InputStart.equalsIgnoreCase("InputStart") &&
		!InputStart.equalsIgnoreCase("InputEnd") ) {
		try {	DateTime.parse(InputStart);
		}
		catch ( Exception e ) {
            message = "The input start date/time \"" +InputStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, InputStart, InputEnd, or blank to use the global input start." ) );
		}
	}
	if (	(InputEnd != null) && !InputEnd.equals("") &&
		!InputEnd.equalsIgnoreCase("InputStart") &&
		!InputEnd.equalsIgnoreCase("InputEnd") ) {
		try {	DateTime.parse( InputEnd );
		}
		catch ( Exception e ) {
            message = "The input end date/time \"" +InputStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, InputStart, InputEnd, or blank to use the global input start." ) );
		}
	}
	// Newer versions use NN.NN.NN format but only allow major and minor versions in this parameter
	// to simplify checks.
	if ( Version != null ) {
	    if ( !StringUtil.isDouble(Version) ) {
            message = "The StateMod version must be a number like 09.01 or 10.02 (no third part).";
    		warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a number Major.Minor for the StateMod version." ) );
	    }
	    else {
	        // Make sure that values less than 10. are padded with a leading zero so that string
	        // comparisons of versions work correctly.
	        double version = Double.parseDouble(Version);
	        if ( (version < 10.0) && (Version.charAt(0) != '0') ) {
	            message = "The StateMod version must be a number like 09.01 or 10.02 (no third part).";
	            warning += "\n" + message;
	            status.addToLog ( CommandPhaseType.INITIALIZATION,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Specify a number Major.Minor for the StateMod version, " +
	                        "padded with leading zero if < version 10." ) );
	        }
	    }
	}

	if ( (OutputVersion != null) && !OutputVersion.isEmpty() &&
		!OutputVersion.equalsIgnoreCase(_Original) &&
		!OutputVersion.equalsIgnoreCase(_Latest) ) {
        message = "The OutputVersion \"" + OutputVersion + "\" is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as " + _Original + " (default) or " + _Latest + ".") );
	}

	if ( (IfFileNotFound != null) && !IfFileNotFound.isEmpty() ) {
		if ( !IfFileNotFound.equalsIgnoreCase(_Ignore) && !IfFileNotFound.equalsIgnoreCase(_Warn)
		    && !IfFileNotFound.equalsIgnoreCase(_Fail) ) {
			message = "The IfFileNotFound parameter \"" + IfFileNotFound + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Ignore + ", " + _Warn + " (default), or " +
					_Fail + "."));
		}
	}
    
    // Check for invalid parameters.
	List<String> validList = new ArrayList<>(10);
    validList.add ( "InputFile" );
    validList.add ( "TSID" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "IncludeDataTypes" );
    validList.add ( "ExcludeDataTypes" );
    validList.add ( "Version" );
    validList.add ( "OutputVersion" );
    validList.add ( "Alias" );
    validList.add ( "IfFileNotFound" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),
		warning );
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
	return (new ReadStateModB_JDialog ( parent, this )).ok();
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
The following classes can be returned:  TS
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
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	String message;

	if ( StringUtil.startsWithIgnoreCase(command_string,"TS") ) {
		// Syntax is TS Alias = readStateModB()
		_use_alias = true;
		message = "TS Alias = readStateModB() is not yet supported.";
		throw new InvalidCommandSyntaxException ( message );
	}
	else {	// Syntax is readStateModB()
		_use_alias = false;
		super.parseCommand ( command_string );
	}
}

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
@param commandPhase command phase that is being run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3; // Level for non-user warning messages
	
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String InputFile = parameters.getValue ( "InputFile" );
	String TSID = parameters.getValue ( "TSID" );
    TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	String InputStart = parameters.getValue ( "InputStart" );
	DateTime InputStart_DateTime = null;
	String InputEnd = parameters.getValue ( "InputEnd" );
    String IncludeDataTypes = parameters.getValue ( "IncludeDataTypes" );
    String [] includeDataTypes = null;
    if ( (IncludeDataTypes != null) && !IncludeDataTypes.isEmpty() ) {
        includeDataTypes = IncludeDataTypes.trim().split(",");
        // Remove surrounding whitespace.
        for ( int i = 0; i < includeDataTypes.length; i++ ) {
        	includeDataTypes[i] = includeDataTypes[i].trim();
        }
    }
    String ExcludeDataTypes = parameters.getValue ( "ExcludeDataTypes" );
    String [] excludeDataTypes = null;
    if ( (ExcludeDataTypes != null) && !ExcludeDataTypes.isEmpty() ) {
        excludeDataTypes = ExcludeDataTypes.trim().split(",");
        // Remove surrounding whitespace.
        for ( int i = 0; i < excludeDataTypes.length; i++ ) {
        	excludeDataTypes[i] = excludeDataTypes[i].trim();
        }
    }
	String Version = parameters.getValue ( "Version" );
	String OutputVersion = parameters.getValue ( "OutputVersion" );
	if ( (OutputVersion == null) || OutputVersion.isEmpty() ) {
		OutputVersion = _Original; // Default.
	}
	String Alias = parameters.getValue("Alias");
	String IfFileNotFound = parameters.getValue ( "IfFileNotFound" );
	if ( (IfFileNotFound == null) || IfFileNotFound.equals("")) {
	    IfFileNotFound = _Warn; // Default
	}

	DateTime InputEnd_DateTime = null;
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		try {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", InputStart );
		CommandProcessorRequestResultsBean bean = null;
		try {
		    bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting InputStart DateTime(DateTime=" +
			InputStart + ") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for InputStart DateTime(DateTime=" +
			InputStart + ") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the specified date/time is valid." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {
		    InputStart_DateTime = (DateTime)prop_contents;
		}
	}
	catch ( Exception e ) {
		message = "InputStart \"" + InputStart + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time for the input start, " +
                        "or InputStart for the global input start." ) );
		throw new InvalidCommandParameterException ( message );
	}
	}
	else {	// Get from the processor...
		try {	Object o = processor.getPropContents ( "InputStart" );
				if ( o != null ) {
					InputStart_DateTime = (DateTime)o;
				}
		}
		catch ( Exception e ) {
            message = "Error requesting the global InputStart from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
		}
	}
	
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
			try {
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", InputEnd );
			CommandProcessorRequestResultsBean bean = null;
			try { bean =
				processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
                message = "Error requesting InputEnd DateTime(DateTime=" +
                InputEnd + ") from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
			}

			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
                message = "Null value for InputEnd DateTime(DateTime=" +
                InputEnd +  ") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the end date/time is valid." ) );
                throw new InvalidCommandParameterException ( message );
			}
			else {	InputEnd_DateTime = (DateTime)prop_contents;
			}
		}
		catch ( Exception e ) {
			message = "InputEnd \"" + InputEnd + "\" is invalid.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time for the input end, " +
                            "or InputEnd for the global input start." ) );
			throw new InvalidCommandParameterException ( message );
		}
		}
		else {	// Get from the processor...
			try {	Object o = processor.getPropContents ( "InputEnd" );
					if ( o != null ) {
						InputEnd_DateTime = (DateTime)o;
					}
			}
			catch ( Exception e ) {
                message = "Error requesting the global InputEnd from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
			}
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Try to read.

    String InputFile_full = InputFile;
	try {
	    boolean readData = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            readData = false;
        }
        InputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)) );
        Message.printStatus ( 2, routine, "Reading StateMod binary file \"" + InputFile_full + "\"" );

       	// Can only read if ${Property} is not used:
        // - TODO smalers 2020-06-06 figure out how to run discovery with properties
        // - this limits how time series are listed in discovery
        File f = new File(InputFile_full);
        if ( (commandPhase == CommandPhaseType.RUN) && !f.exists() ) {
        	message = "File to read \"" + InputFile_full + "\" does not exist.";
        	if ( IfFileNotFound.equalsIgnoreCase(_Fail) ) {
            	Message.printWarning ( warning_level,
                	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            	status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                   	message, "Verify that the file exists at the time the command is run."));
        	}
        	else if ( IfFileNotFound.equalsIgnoreCase(_Warn) ) {
            	Message.printWarning ( warning_level,
                	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            	status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
                   	message, "Verify that the file exists at the time the command is run."));
        	}
        	else {
            	Message.printStatus( 2, routine, message + "  Ignoring.");
            	status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.INFO,
                   	message, "Verify that the file exists at the time the command is run."));
        	}
        }
        else if ( f.exists() ) {
        	StateMod_BTS bts = null;
        	// The following will throw IOException if the file does not exist.
		    if ( (Version != null) && StringUtil.isDouble(Version) ) {
			   	bts = new StateMod_BTS ( InputFile_full, Version );
		    }
		    else {
		       	bts = new StateMod_BTS ( InputFile_full );
		    }
		    List<TS> tslist = bts.readTimeSeriesList ( TSID, InputStart_DateTime, InputEnd_DateTime,
			   	includeDataTypes, excludeDataTypes, null, readData, OutputVersion );
		    bts.close();
		    bts = null;

		    List<String> aliasList = new ArrayList<>();
		    if ( tslist != null ) {
		    	int tscount = tslist.size();
		    	message = "Read " + tscount + " time series from \"" + InputFile_full + "\"";
		    	Message.printStatus ( 2, routine, message );
		    	TS ts = null;
		    	for (int i = 0; i < tscount; i++) {
		    		ts = (TS)tslist.get(i);
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
		    						"\" was also used for another time series read from the StateMod output file.";
		    					Message.printWarning(log_level,
		    						MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
		    					status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
		    						message, "Consider using a more specific alias to uniquely identify the time series." ) );
		    				}
		    			}
		    			// Add the new list to the alias.
		    			aliasList.add ( alias );
		    		}
		    	}
		    }
		    if ( commandPhase == CommandPhaseType.RUN ) {
	        	if ( tslist != null ) {
	        		// Further process the time series.
	        		// This makes sure the period is at least as long as the output period.
	        		int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
	        		if ( wc > 0 ) {
	        			message = "Error post-processing time series after read.";
	        			Message.printWarning ( warning_level, 
	        				MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	        			status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	        				message, "Report the problem to software support." ) );
	        			throw new CommandException ( message );
	        		}
	    
	        		// Add the list in the processor.
	            
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
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexepected error reading StateMod binary file \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "See log file for details." ) );
		throw new CommandException ( message );
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
	String InputFile = props.getValue("InputFile");
	String TSID = props.getValue("TSID");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");
	String IncludeDataTypes = props.getValue("IncludeDataTypes");
	String ExcludeDataTypes = props.getValue("ExcludeDataTypes");
	String Version = props.getValue("Version");
	String OutputVersion = props.getValue("OutputVersion");
	String Alias = props.getValue("Alias");
	String IfFileNotFound = props.getValue("IfFileNotFound");
	StringBuffer b = new StringBuffer ();
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputStart=\"" + InputStart + "\"" );
	}
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputEnd=\"" + InputEnd + "\"" );
	}
	if ( (IncludeDataTypes != null) && (IncludeDataTypes.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IncludeDataTypes=\"" + IncludeDataTypes + "\"" );
	}
	if ( (ExcludeDataTypes != null) && (ExcludeDataTypes.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ExcludeDataTypes=\"" + ExcludeDataTypes + "\"" );
	}
	if ( (Version != null) && (Version.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Version=\"" + Version + "\"" );
	}
	if ( (OutputVersion != null) && (OutputVersion.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputVersion=\"" + OutputVersion + "\"" );
	}
    if ((Alias != null) && (Alias.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Alias=\"" + Alias + "\"");
    }
    if ((IfFileNotFound != null) && (IfFileNotFound.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("IfFileNotFound=\"" + IfFileNotFound + "\"");
    }
	return getCommandName() + "(" + b.toString() + ")";
}

/**
Indicate whether the alias version of the command is being used.  This method
should be called only after the parseCommandParameters() method is called.
*/
protected boolean useAlias ()
{	return _use_alias;
}

}