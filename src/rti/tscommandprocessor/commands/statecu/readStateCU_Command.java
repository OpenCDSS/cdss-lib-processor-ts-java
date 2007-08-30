//------------------------------------------------------------------------------
// readStateCU_Command - handle the readStateCU() and
//				TS Alias = readStateCU() commands
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2007-06-21	Steven A. Malers, RTi	Initial version.  Copy and modify
//					readStateMod_Command().
//------------------------------------------------------------------------------

package rti.tscommandprocessor.commands.statecu;

import java.io.File;
import java.util.Vector;

import javax.swing.JFrame;

import RTi.TS.TS;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.IO.SkeletonCommand;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

import DWR.StateCU.StateCU_CropPatternTS;
import DWR.StateCU.StateCU_IrrigationPracticeTS;
import DWR.StateCU.StateCU_TS;

/**
<p>
This class initializes, checks, and runs the readStateCU() command.
</p>
<p>The CommandProcessor must return the following properties:
TSResultsList, WorkingDir.
</p>
*/
public class readStateCU_Command extends SkeletonCommand implements Command
{

// Values for AutoAdjust
protected final String _False = "False";
protected final String _True = "True";

// Indicates whether the TS Alias version of the command is being used...

protected boolean _use_alias = false;

private String __working_dir = null;	// Application working directory

/**
Constructor.
*/
public readStateCU_Command ()
{	super();
	setCommandName ( "readStateCU" );
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
public void checkCommandParameters (	PropList parameters, String command_tag,
					int warning_level )
throws InvalidCommandParameterException
{	String InputFile = parameters.getValue ( "InputFile" );
	String InputStart = parameters.getValue ( "InputStart" );
	String InputEnd = parameters.getValue ( "InputEnd" );
	//String TSID = parameters.getValue ( "TSID" );
	String NewScenario = parameters.getValue ( "NewScenario" );
	String AutoAdjust = parameters.getValue ( "AutoAdjust" );
	String warning = "";

	if ( (InputFile == null) || (InputFile.length() == 0) ) {
		warning += "\nThe input file must be specified.";
	}
	else {	try { Object o = _processor.getPropContents ( "WorkingDir" );
				// Working directory is available so use it...
				if ( o != null ) {
					__working_dir = (String)o;
				}
			}
			catch ( Exception e ) {
				// Not fatal, but of use to developers.
				String message = "Error requesting WorkingDir from processor - not using.";
				String routine = getCommandName() + ".checkCommandParameters";
				Message.printDebug(10, routine, message );
			}
	
		try {	String adjusted_path = IOUtil.adjustPath (
				__working_dir, InputFile);
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				warning +=
				"\nThe input file parent directory does " +
				"not exist: \"" + adjusted_path + "\".";
			}
			f = null;
			f2 = null;
		}
		catch ( Exception e ) {
			warning +=
				"\nThe working directory:\n" +
				"    \"" + __working_dir +
				"\"\ncannot be used to adjust the file:\n" +
				"    \"" + InputFile + "\".";
		}
	}

	if (	(InputStart != null) && !InputStart.equals("") &&
		!InputStart.equalsIgnoreCase("InputStart") &&
		!InputStart.equalsIgnoreCase("InputEnd") ) {
		try {	DateTime.parse(InputStart);
		}
		catch ( Exception e ) {
			warning += 
				"\nThe input start date/time \"" +InputStart +
				"\" is not a valid date/time.\n"+
				"Specify a date/time or InputStart.";
		}
	}
	if (	(InputEnd != null) && !InputEnd.equals("") &&
		!InputEnd.equalsIgnoreCase("InputStart") &&
		!InputEnd.equalsIgnoreCase("InputEnd") ) {
		try {	DateTime.parse( InputEnd );
		}
		catch ( Exception e ) {
			warning +=
				"\nThe input end date/time \"" + InputEnd +
				"\" is not a valid date/time.\n"+
				"Specify a date/time or InputEnd.";
		}
	}
	// TODO SAM Need to check TSID - see newTimeSeries but support checking wildcards
	if ( (NewScenario != null) && !NewScenario.equals("") &&
		(NewScenario.indexOf(" ") > 0) ) {
			warning +=
				"\nThe NewScenario must not contain spaces.";
	}
	
	if ( (AutoAdjust != null) && !AutoAdjust.equals("") &&
			!AutoAdjust.equalsIgnoreCase("True") &&
			!AutoAdjust.equalsIgnoreCase("False") ) {
				warning +=
					"\nThe AutoAdjust value (" + AutoAdjust + ") must be True or False.";
		}
	
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),
		warning );
		throw new InvalidCommandParameterException ( warning );
	}
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new readStateCU_JDialog ( parent, this )).ok();
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
public void parseCommand (	String command_string, String command_tag,
				int warning_level )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	String message;

	if ( StringUtil.startsWithIgnoreCase(command_string,"TS") ) {
		// Syntax is TS Alias = readStateMod()
		_use_alias = true;
		message = "TS Alias = readStateCU() is not yet supported.";
		throw new InvalidCommandSyntaxException ( message );
	}
	else {	// Syntax is readStateMod()
		_use_alias = false;
		super.parseCommand (command_string, command_tag,warning_level);
	}
}
	
/**
Run the commands:
<pre>
readStateCU(InputFile="X",InputStart="X",InputEnd="X",TSID="X",NewScenario="X",
AutoAdust=X)
</pre>
@param processor The CommandProcessor that is executing the command, which will
provide necessary data inputs and receive output(s).
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( String command_tag, int warning_level )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "readStateCU_Command.runCommand", message;
	int warning_count = 0;
	int log_level = 3;	// Log level for non-user warnings

	String InputFile = _parameters.getValue ( "InputFile" );

	String InputStart = _parameters.getValue ( "InputStart" );
	DateTime InputStart_DateTime = null;
	String InputEnd = _parameters.getValue ( "InputEnd" );
	DateTime InputEnd_DateTime = null;
	String TSID = _parameters.getValue ( "TSID" );
	String NewScenario = _parameters.getValue ( "NewScenario" );
	String AutoAdjust = _parameters.getValue ( "AutoAdjus" );
	
	// REVISIT need to check prop
	boolean IncludeLocationTotal_boolean = true;
	boolean IncludeDataSetTotal_boolean = true;

    // get the path based on the current working directory
    InputFile = IOUtil.getPathUsingWorkingDir( InputFile );
    
	if ( InputStart != null ) {
		try {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", InputStart );
		CommandProcessorRequestResultsBean bean = null;
		try { bean =
			_processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting InputStart DateTime(DateTime=" +
			InputStart + "\" from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			throw new InvalidCommandParameterException ( message );
		}

		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for InputStart DateTime(DateTime=" +
			InputStart +	"\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			throw new InvalidCommandParameterException ( message );
		}
		else {	InputStart_DateTime = (DateTime)prop_contents;
		}
	}
	catch ( Exception e ) {
		message = "InputStart \"" + InputStart + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	}
	else {	// Get from the processor...
		try {	Object o = _processor.getPropContents ( "InputStart" );
				if ( o != null ) {
					InputStart_DateTime = (DateTime)o;
				}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting InputStart from processor - not using.";
			Message.printDebug(10, routine, message );
		}
	}
	
		if ( InputEnd != null ) {
			try {
			PropList request_params = new PropList ( "" );
			request_params.set ( "DateTime", InputEnd );
			CommandProcessorRequestResultsBean bean = null;
			try { bean =
				_processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting InputEnd DateTime(DateTime=" +
				InputEnd + "\" from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
				throw new InvalidCommandParameterException ( message );
			}

			PropList bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for InputEnd DateTime(DateTime=" +
				InputEnd +	"\") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
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
			throw new InvalidCommandParameterException ( message );
		}
		}
		else {	// Get from the processor...
			try {	Object o = _processor.getPropContents ( "InputEnd" );
					if ( o != null ) {
						InputEnd_DateTime = (DateTime)o;
					}
			}
			catch ( Exception e ) {
				// Not fatal, but of use to developers.
				message = "Error requesting InputEnd from processor - not using.";
				Message.printDebug(10, routine, message );
			}
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Now try to read...

	try {	Message.printStatus ( 2, routine, "Reading StateCU file \"" +
			InputFile + "\"" );
		Vector tslist = null;
		String file_type = "unknown file type";
		if ( StateCU_CropPatternTS.isCropPatternTSFile ( InputFile ) ) {
			file_type = "crop pattern";
			PropList read_props = null;
			if ( AutoAdjust != null ) {
				read_props = new PropList ( "CDS" );
				read_props.set ( "AutoAdjust", AutoAdjust );
			}
			tslist = StateCU_CropPatternTS.toTSVector(
				StateCU_CropPatternTS.readStateCUFile (
				InputFile, InputStart_DateTime, InputEnd_DateTime,
				read_props ),
				IncludeLocationTotal_boolean,
				IncludeDataSetTotal_boolean,
				null,
				null );
		}
		else if(StateCU_IrrigationPracticeTS.isIrrigationPracticeTSFile(
				InputFile ) ) {
			file_type = "irrigation practice";
			tslist = StateCU_IrrigationPracticeTS.toTSVector(
				StateCU_IrrigationPracticeTS.readStateCUFile (
					InputFile, InputStart_DateTime, InputEnd_DateTime ),
				IncludeDataSetTotal_boolean, null, null );
		}
		else if ( StateCU_TS.isReportFile ( InputFile ) ) {
			file_type = "model results";
			tslist = StateCU_TS.readTimeSeriesList (
				TSID, InputFile, InputStart_DateTime, InputEnd_DateTime,
				(String)null, true );
		}
		else {	message = "File \"" + InputFile +
			"\" is not a recognized StateCU file type.  Not reading.";
			Message.printWarning ( warning_level, 
					MessageUtil.formatMessageTag(command_tag,
					++warning_count), routine, message );
			throw new CommandException ( message );
		}
		
		// Print out how many time series were actually read...

		int size = 0;
		if ( tslist != null ) {
			size = tslist.size();
		}
		Message.printStatus ( 2, routine,
				"Read " + size + " StateCU " + file_type + " time series." );
		
		// If the scenario was specified set it in all the time series...
		
		if ( (NewScenario != null) && (NewScenario.length() > 0) ) {
			TS ts = null;
			for ( int i = 0; i < size; i++ ) {
				ts = (TS)tslist.elementAt(i);
				ts.getIdentifier().setScenario(NewScenario);
			}
		}

		// Now add the time series to the end of the normal list...

		if ( tslist != null ) {
			Vector TSResultsList_Vector = null;
			try { Object o = _processor.getPropContents( "TSResultsList" );
					TSResultsList_Vector = (Vector)o;
			}
			catch ( Exception e ){
				message = "Cannot get time series list to add read time series.  Starting new list.";
				Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(
						command_tag, ++warning_count),
						routine,message);
				TSResultsList_Vector = new Vector();
			}

			// Further process the time series...
			// This makes sure the period is at least as long as the
			// output period...
			PropList request_params = new PropList ( "" );
			request_params.setUsingObject ( "TSList", tslist );
			try {
				_processor.processRequest( "ReadTimeSeries2", request_params);
			}
			catch ( Exception e ) {
				message =
					"Error post-processing StateCU time series after read.";
					Message.printWarning ( warning_level, 
					MessageUtil.formatMessageTag(command_tag,
					++warning_count), routine, message );
					Message.printWarning(log_level, routine, e);
					throw new CommandException ( message );
			}

			for ( int i = 0; i < size; i++ ) {
				TSResultsList_Vector.addElement ( tslist.elementAt(i) );
			}
			
			// Now reset the list in the processor...
			if ( TSResultsList_Vector != null ) {
				try {	_processor.setPropContents ( "TSResultsList", TSResultsList_Vector );
				}
				catch ( Exception e ){
					message = "Cannot set updated time series list.  Results may not be visible.";
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(
						command_tag, ++warning_count),
						routine,message);
				}
			}
		}

		// Free resources from StateMod list...
		tslist = null;
	}
	catch ( Exception e ) {
		Message.printWarning ( log_level, routine, e );
		message = "Error reading time series from StateCU file.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new CommandException ( message );
	}
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String InputFile = props.getValue("InputFile");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");
	String TSID = props.getValue("TSID");
	String NewScenario = props.getValue("NewScenario");
	String AutoAdjust = props.getValue("AutoAdjust");
	StringBuffer b = new StringBuffer ();
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile=\"" + InputFile + "\"" );
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
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (NewScenario != null) && (NewScenario.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewScenario=\"" + NewScenario + "\"");
	}
	if ( (AutoAdjust != null) && (AutoAdjust.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AutoAdjust=" + AutoAdjust );
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