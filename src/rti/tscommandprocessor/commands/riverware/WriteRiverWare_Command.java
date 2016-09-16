//------------------------------------------------------------------------------
// writeRiverWare_Command - handle the writeRiverWare() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-05-31	Steven A. Malers, RTi	Initial version.  Copy and modify
//					sortTimeSeries().
// 2005-06-01	SAM, RTi		Add Precision parameter.
// 2005-08-30	SAM, RTi		Check for CreateOutput property in the
//					processor when running.
// 2005-05-24	SAM, RTi		Check for OutputStart and OutputEnd
//					when running - was not recognizing the
//					global output period.
// 2007-02-11	SAM, RTi		Remove direct dependency on TSCommandProcessor.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.riverware;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
// FIXME SAM 2007-08-30 Need to move RiverWare to its own DAO package
import RTi.TS.RiverWareTS;
import RTi.TS.TS;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the WriteRiverWare() command.
*/
public class WriteRiverWare_Command extends AbstractCommand implements Command, FileGenerator
{

/** 
Values for use with WriteHeaderComments parameter.
*/
protected final String _False = "False";
protected final String _True = "True";
	
/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteRiverWare_Command ()
{	super();
	setCommandName ( "WriteRiverWare" );
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
{	String OutputFile = parameters.getValue ( "OutputFile" );
	String WriteHeaderComments = parameters.getValue ( "WriteHeaderComments" );
	//String Units = parameters.getValue ( "Units" );
	String Scale = parameters.getValue ( "Scale" );
	//String SetUnits = parameters.getValue ( "SetUnits" );
	String SetScale = parameters.getValue ( "SetScale" );
	String Precision = parameters.getValue ( "Precision" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	String message;
	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
		message = "The output file: \"" + OutputFile + "\" must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify an output file." ) );
	}
	else if ( OutputFile.indexOf("${") < 0 ){
        String working_dir = null;
		try { Object o = processor.getPropContents ( "WorkingDir" );
			if ( o != null ) {
				working_dir = (String)o;
			}
		}
		catch ( Exception e ) {
			message = "Error requesting WorkingDir from processor.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Software error - report problem to support." ) );
		}

		try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, OutputFile));
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				message = "The output file parent directory does " +
					"not exist: \"" + adjusted_path + "\".";
				warning += "\n" + message;
				status.addToLog ( CommandPhaseType.INITIALIZATION,
						new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Create output directory." ) );
			}
			f = null;
			f2 = null;
		}
		catch ( Exception e ) {
			message = "The output file:\n" +
				"    \"" + OutputFile +
				"\"\ncannot be adjusted using the working directory:\n" +
				"    \"" + working_dir + "\".";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Verify that output file and working directory paths are compatible." ) );
		}
	}

	if ( (WriteHeaderComments != null) && !WriteHeaderComments.equals("") ) {
        if ( !WriteHeaderComments.equalsIgnoreCase(_False) && !WriteHeaderComments.equalsIgnoreCase(_True) ) {
            message = "The WriteHeaderComments parameter (" + WriteHeaderComments + ") must be " + _False +
            " or " + _True + ".";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the parameter as " + _False + " or " + _True + "."));
        }
    }

	/* TODO SAM 2005-05-31 Might need to check units against global
	list - right now units are not universally choosable.
	if ( (Units != null) && !Units.equals("") ) {
	}
	*/
	if ( (Scale != null) && !Scale.isEmpty() && (Scale.indexOf("${") < 0) ) {
		if ( !StringUtil.isDouble(Scale) ) {
			message = "The scale: \"" + Scale + "\" is not a number.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a number > 0 for the scale." ) );

		}
		else if ( StringUtil.atod(Scale) <= 0 ) {
			message = "The scale: \"" + Scale +	"\" must be > 0.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a scale > 0." ) );
			
		}
	}
	/* TODO SAM 2005-05-31 Might need to check units against global
	list - right now units are not universally choosable.
	if ( (SetUnits != null) && !SetUnits.equals("") ) {
	}
	*/
	if ( (SetScale != null) && !SetScale.isEmpty() && (SetScale.indexOf("${") < 0) ) {
		if ( !StringUtil.isDouble(SetScale) ) {
			message = "The set_scale: \"" + SetScale + "\" is not a number.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a set scale as a number > 0." ) );
		}
		else if ( StringUtil.atod(SetScale) <= 0 ) {
			message = "The set_scale: \"" + SetScale + "\" must be > 0.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a set scale > 0." ) );
		}
	}
	if ( (Precision != null) && !Precision.equals("") ) {
		if ( !StringUtil.isInteger(Precision) ) {
			message = "The precision: \"" + Precision +	"\" is not an integer.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify the precision as an integer." ) );
		}
		else if ( StringUtil.atoi(Precision) < 0 ) {
			message = "The precision: \"" + Precision + "\" must be >= 0.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a precision >= 0." ) );
		}
	}
	// Check for invalid parameters...
	List validList = new ArrayList<String>(9);
	validList.add ( "TSList" );
	validList.add ( "TSID" );
	validList.add ( "OutputFile" );
	validList.add ( "WriteHeaderComments" );
	validList.add ( "Units" );
	validList.add ( "Scale" );
	validList.add ( "SetScale" );
	validList.add ( "Precision" );
	validList.add ( "SetUnits" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine,
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
	return (new WriteRiverWare_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List getGeneratedFileList ()
{
	List list = new Vector();
	if ( getOutputFile() != null ) {
		list.add ( getOutputFile() );
	}
	return list;
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
	return __OutputFile_File;
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
{	String routine = "writeRiverWare_Command.parseCommand", message;
	int warning_level = 2;
	if ( command_string.indexOf("=") > 0 ) {
		// New syntax...
		super.parseCommand ( command_string );
	}
	else {	// Parse the old command...
		List tokens = StringUtil.breakStringList ( command_string,
			"(,)", StringUtil.DELIM_ALLOW_STRINGS );
		if ( tokens.size() != 6 ) {
			message =
			"Invalid syntax for command.  Expecting 5 parameters.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
		String OutputFile = ((String)tokens.get(1)).trim();
		String Units = ((String)tokens.get(2)).trim();
		if ( Units.equals("*") ) {
			Units = "";
		}
		String Scale = ((String)tokens.get(3)).trim();
		String SetUnits = ((String)tokens.get(4)).trim();
		if ( SetUnits.equals("*") ) {
			SetUnits = "";
		}
		String SetScale = ((String)tokens.get(5)).trim();
		// Defaults because not in the old command...
		String TSList = "AllTS";
		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		parameters.set ( "TSList", TSList );
		if ( OutputFile.length() > 0 ) {
			parameters.set ( "OutputFile", OutputFile );
		}
		if ( Units.length() > 0 ) {
			parameters.set("Units", Units);
		}
		if ( Scale.length() > 0 ) {
			parameters.set ( "Scale", Scale );
		}
		if ( SetUnits.length() > 0 ) {
			parameters.set ( "SetUnits", SetUnits );
		}
		if ( SetScale.length() > 0 ) {
			parameters.set ( "SetScale", SetScale );
		}
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
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
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	// Clear the output file
	
	setOutputFile ( null );

	// Check whether the processor wants output files to be created...

	CommandStatus status = getCommandStatus();
	CommandProcessor processor = getCommandProcessor();
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
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
	if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
			Message.printStatus ( 2, routine,
			"Skipping \"" + toString() + "\" because output is not being created." );
	}

	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
    if ( (TSID != null) && !TSID.isEmpty() && (TSID.indexOf("${") >= 0) ) {
    	TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
	String OutputFile = parameters.getValue ( "OutputFile" ); // Expanded below
    String WriteHeaderComments = parameters.getValue ( "WriteHeaderComments" );
    boolean writeHeaderComments = true;
    if ( (WriteHeaderComments != null) && WriteHeaderComments.equalsIgnoreCase(_False) ) {
        writeHeaderComments = false;
    }
	String Units = parameters.getValue ( "Units" );
    if ( (Units != null) && !Units.isEmpty() && (Units.indexOf("${") >= 0) ) {
    	Units = TSCommandProcessorUtil.expandParameterValue(processor, this, Units);
	}
	String Scale = parameters.getValue ( "Scale" );
    if ( (Scale != null) && !Scale.isEmpty() && (Scale.indexOf("${") >= 0) ) {
    	Scale = TSCommandProcessorUtil.expandParameterValue(processor, this, Scale);
	}
	String SetUnits = parameters.getValue ( "SetUnits" );
    if ( (SetUnits != null) && !SetUnits.isEmpty() && (SetUnits.indexOf("${") >= 0) ) {
    	SetUnits = TSCommandProcessorUtil.expandParameterValue(processor, this, SetUnits);
	}
	String SetScale = parameters.getValue ( "SetScale" );
    if ( (SetScale != null) && !SetScale.isEmpty() && (SetScale.indexOf("${") >= 0) ) {
    	SetScale = TSCommandProcessorUtil.expandParameterValue(processor, this, SetScale);
	}
	String Precision = parameters.getValue ( "Precision" );
    if ( (Precision != null) && !Precision.isEmpty() && (Precision.indexOf("${") >= 0) ) {
    	Precision = TSCommandProcessorUtil.expandParameterValue(processor, this, Precision);
	}
	String OutputStart = parameters.getValue ( "OutputStart" );
	if ( (OutputStart == null) || OutputStart.isEmpty() ) {
		OutputStart = "${OutputStart}"; // Default
	}
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	if ( (OutputEnd == null) || OutputEnd.isEmpty() ) {
		OutputEnd = "${OutputEnd}"; // Default
	}

	// Get the time series to process...
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	if ( TSList == null ) {
		TSList = "" + TSListType.ALL_TS;
	}
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try { bean =
		processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\") from processor.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	if ( o_TSList == null ) {
		message = "Unable to find time series to write using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report problem to software support." ) );
	}
	List tslist = (List)o_TSList;
	if ( tslist.size() == 0 ) {
		message = "Unable to find time series to write using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.WARNING,
						message, "Confirm that time series are available (may be OK for partial run)." ) );
	}

	// TODO SAM 2007-02-12 Need to enable OutputStart, OutputEnd in command
	DateTime OutputStart_DateTime = null;
	DateTime OutputEnd_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			OutputStart_DateTime = TSCommandProcessorUtil.getDateTime ( OutputStart, "OutputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			OutputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( OutputEnd, "OutputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
    }

	if ( warning_count > 0 ) {
		message = "Error preparing data to write RiverWare file.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new CommandException ( message );
	}
	
    // Get the comments to add to the top of the file.

	PropList outputProps = new PropList("");
	List outputCommentsList = null;
	if ( writeHeaderComments ) {
	    try { Object o = processor.getPropContents ( "OutputComments" );
	        // Comments are available so use them...
	        if ( o != null ) {
	            outputCommentsList = (List)o;
	            outputProps.setUsingObject("OutputComments",outputCommentsList);
	        }
	    }
	    catch ( Exception e ) {
	        // Not fatal, but of use to developers.
	        message = "Error requesting OutputComments from processor - not using.";
	        Message.printDebug(10, routine, message );
	    }
	}
	// Set output header parameters
	if ( (Units != null) && !Units.isEmpty() ) {
		outputProps.set("Units",Units);
	}
	if ( (Scale != null) && !Scale.isEmpty() ) {
		outputProps.set("Scale",Scale);
	}
	// Set output header parameters
	if ( (SetUnits != null) && !SetUnits.isEmpty() ) {
		outputProps.set("SetUnits",SetUnits);
	}
	if ( (SetScale != null) && !SetScale.isEmpty() ) {
		outputProps.set("SetScale",SetScale);
	}
	if ( (Precision != null) && !Precision.isEmpty() ) {
		outputProps.set("Precision",Precision);
	}

	// Now try to write...

	String OutputFile_full = OutputFile;
	try {
		// Convert to an absolute path...
		OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
		Message.printStatus ( 2, routine, "Writing RiverWare file \"" + OutputFile_full + "\"" );
		// Only write the first time series...
		TS tsout = (TS)tslist.get(0);
		// Don't pass units to below...
		RiverWareTS.writeTimeSeries ( tsout, OutputFile_full,
			OutputStart_DateTime,
			OutputEnd_DateTime, outputProps, true );
		// Save the output file name...
		setOutputFile ( new File(OutputFile_full));
	}
	catch ( Exception e ) {
		message = "Unexpected error writing time series to RiverWare file \"" + OutputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "See log file for details." ) );
		throw new CommandException ( message );
	}
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file )
{
	__OutputFile_File = file;
}

/**
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String TSList = parameters.getValue("TSList");
	String TSID = parameters.getValue("TSID");
	String OutputFile = parameters.getValue("OutputFile");
	String WriteHeaderComments = parameters.getValue ( "WriteHeaderComments" );
	String Units = parameters.getValue("Units");
	String Scale = parameters.getValue("Scale");
	String SetUnits = parameters.getValue("SetUnits");
	String SetScale = parameters.getValue("SetScale");
	String Precision = parameters.getValue("Precision");
	StringBuffer b = new StringBuffer ();
	if ( (TSList != null) && (TSList.length() > 0) ) {
		b.append ( "TSList=" + TSList );
	}
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}
    if ( (WriteHeaderComments != null) && (WriteHeaderComments.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WriteHeaderComments=" + WriteHeaderComments );
    }
	if ( (Units != null) && (Units.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Units=\"" + Units + "\"" );
	}
	if ( (Scale != null) && (Scale.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Scale=" + Scale );
	}
	if ( (SetUnits != null) && (SetUnits.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SetUnits=\"" + SetUnits + "\"" );
	}
	if ( (SetScale != null) && (SetScale.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SetScale=" + SetScale );
	}
	if ( (Precision != null) && (Precision.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Precision=" + Precision );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
