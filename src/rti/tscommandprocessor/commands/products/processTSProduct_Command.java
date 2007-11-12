//------------------------------------------------------------------------------
// processTSProduct_Command - handle the processTSProduct() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-10-18	Steven A. Malers, RTi	Initial version.  Copy and modify
//					writeStateMod().
// 2005-11-04	SAM, RTi		Add annotation provider hooks.
// 2005-11-07	J. Thomas Sapienza, RTi	Completed code for adding annotations 
//					via annotation providers.
// 2007-02-09	SAM, RTi		Remove direct use of TSCommandProcessor.
//					Cast to a TSSupplier instead of TSCommandProcessor.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.products;

import java.io.File;
import java.util.Vector;

import java.awt.event.WindowListener;
import javax.swing.JFrame;

import RTi.GRTS.TSProcessor;
import RTi.GRTS.TSProduct;
import RTi.GRTS.TSProductAnnotationProvider;

import RTi.TS.TSSupplier;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
<p>
This class initializes, checks, and runs the processTSProduct() command.
</p>
<p>The CommandProcessor must return the following properties:  CreateOutput,
TSResultsList, WorkingDir.
</p>
*/
public class processTSProduct_Command extends AbstractCommand implements Command
{

/**
Protected data members shared with the dialog and other related classes.
*/
protected final String _BatchOnly = "BatchOnly";
protected final String _GUIOnly = "GUIOnly";
protected final String _GUIAndBatch = "GUIAndBatch";

protected final String _False = "False";
protected final String _True = "True";

/**
Constructor.
*/
public processTSProduct_Command ()
{	super();
	setCommandName ( "processTSProduct" );
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
{	String TSProductFile = parameters.getValue ( "TSProductFile" );
	String RunMode = parameters.getValue ( "RunMode" );
	String View = parameters.getValue ( "View" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String warning = "";
	
	CommandProcessor processor = getCommandProcessor();

	if ( (TSProductFile == null) || (TSProductFile.length() == 0) ) {
		warning += "\nThe TSProduct file must be specified.";
	}
	else {	String working_dir = null;
			try { Object o = processor.getPropContents ( "WorkingDir" );
				// Working directory is available so use it...
				if ( o != null ) {
					working_dir = (String)o;
				}
			}
			catch ( Exception e ) {
				// Not fatal, but of use to developers.
				String message = "Error requesting WorkingDir from processor - not using.";
				String routine = getCommandName() + ".checkCommandParameters";
				Message.printDebug(10, routine, message );
			}
		try {	String adjusted_path = IOUtil.adjustPath (
				working_dir, TSProductFile);
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				warning +=
				"\nThe TSProduct file parent directory does " +
				"not exist: \"" + adjusted_path + "\".";
			}
			f = null;
			f2 = null;
		}
		catch ( Exception e ) {
			warning +=
				"\nThe working directory:\n" +
				"    \"" + working_dir +
				"\"\ncannot be adjusted using:\n" +
				"    \"" + TSProductFile + "\".";
		}
	}

	if (	(RunMode != null) && !RunMode.equals("") &&
		!RunMode.equalsIgnoreCase(_BatchOnly) &&
		!RunMode.equalsIgnoreCase(_GUIOnly) &&
		!RunMode.equalsIgnoreCase(_GUIAndBatch) ) {
		warning += 
			"\nThe run mode \"" +RunMode +
			"\" is not valid.";
	}
	if (	(View != null) && !View.equals("") &&
		!View.equalsIgnoreCase(_True) &&
		!View.equalsIgnoreCase(_False) ) {
		warning += 
			"\nThe View parameter \"" +View +
			"\" must be " + _True + " or " + _False + ".";
	}
	if (	(View != null) && View.equalsIgnoreCase(_False) &&
		((OutputFile == null) || (OutputFile.length() == 0)) ) {
		warning+="\nThe output file must be specified when View=False.";
	}
	else if ( (OutputFile != null) && !OutputFile.equals("") ) {
		String working_dir = null;
		try { Object o = processor.getPropContents ( "WorkingDir" );
			// Working directory is available so use it...
			if ( o != null ) {
				working_dir = (String)o;
			}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			String message = "Error requesting WorkingDir from processor - not using.";
			String routine = getCommandName() + ".checkCommandParameters";
			Message.printDebug(10, routine, message );
		}
				
		try {	String adjusted_path = IOUtil.adjustPath (
				working_dir, OutputFile);
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				warning +=
				"\nThe output file parent directory does " +
				"not exist: \"" + adjusted_path + "\".";
			}
			f = null;
			f2 = null;
		}
		catch ( Exception e ) {
			warning +=
				"\nThe working directory:\n" +
				"    \"" + working_dir +
				"\"\ncannot be adjusted using:\n" +
				"    \"" + OutputFile + "\".";
		}
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
	return (new processTSProduct_JDialog ( parent, this )).ok();
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
{	//int warning_count = 0;
	String routine = "processTSProduct_Command.parseCommand", message;
	int warning_level = 2;
	if ( command_string.indexOf("=") > 0 ) {
		// New syntax...
		super.parseCommand ( command_string );
	}
	else {	// Parse the old command...
		Vector tokens = StringUtil.breakStringList ( command_string,
			"(,)", StringUtil.DELIM_ALLOW_STRINGS );
		if ( (tokens.size() != 4) && (tokens.size() != 5) ) {
			message =
			"Invalid syntax for command.  Expecting 4 parameters.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}
		String TSProductFile = ((String)tokens.elementAt(1)).trim();
		String RunMode = ((String)tokens.elementAt(2)).trim();
		String View = ((String)tokens.elementAt(3)).trim();
		String OutputFile = ((String)tokens.elementAt(4)).trim();
		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSProductFile.length() > 0 ) {
			parameters.set ( "TSProductFile", TSProductFile );
		}
		if ( RunMode.length() > 0 ) {
			parameters.set ( "RunMode", RunMode );
		}
		if ( View.length() > 0 ) {
			if ( View.equalsIgnoreCase("Preview") ) {
				View = _True;
			}
			else if ( View.equalsIgnoreCase("NoPreview") ) {
				View = _False;
			}
			parameters.set ( "View", View );
		}
		if ( OutputFile.length() > 0 ) {
			parameters.set ( "OutputFile", OutputFile );
		}
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
}

/**
Run the commands:
<pre>
processTSProduct(TSProductFile="X",RunMode=X,View=X,OutputFile="X")
</pre>
Time series are taken from the
available list in memory, if available.  Otherwise the time series are re-read.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "processTSProduct_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	// Check whether the application wants output files to be created...
	
	CommandProcessor processor = getCommandProcessor();
	try {	Object o = processor.getPropContents ( "CreateOutput" );
		if ( o != null ) {
			boolean CreateOutput_boolean = ((Boolean)o).booleanValue();
			if  ( !CreateOutput_boolean ) {
				Message.printStatus ( 2, routine,
					"Skipping \"" + toString() +
				"\" because output is to be ignored." );
				return;
			}
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		message = "Error requesting CreateOutput from processor - not using.";
		Message.printWarning(10, routine, message );
	}

	PropList parameters = getCommandParameters();
	String TSProductFile = parameters.getValue ( "TSProductFile" );
	String RunMode = parameters.getValue ( "RunMode" );
	if ( (RunMode == null) || RunMode.equals("") ) {
		RunMode = _GUIAndBatch;
	}
	String View = parameters.getValue ( "View" );
	if ( (View == null) || View.equals("") ) {
		View = _True;
	}
	String OutputFile = parameters.getValue ( "OutputFile" );
	if (	(View == null) || View.equals("") &&
		((OutputFile == null) || OutputFile.equals("")) ) {
		// No output file so view is true by default...
		View = _True;
	}

	// Get from the processor...

	WindowListener tsview_window_listener = null;
	
	try { Object o = processor.getPropContents ( "TSViewWindowListener" );
		// TSViewWindowListener is available so use it...
		if ( o != null ) {
			tsview_window_listener = (WindowListener)o;
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		message = "Error requesting TSViewWindowListener from processor - not using.";
		Message.printDebug(10, routine, message );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Now try to process...

	try {	PropList override_props = new PropList ("TSTool");
		DateTime now = new DateTime ( DateTime.DATE_CURRENT );
		if ( (OutputFile != null) && !OutputFile.equals("") ) {
			override_props.set ( "OutputFile",
			IOUtil.getPathUsingWorkingDir(OutputFile) );
		}
		override_props.set ( "CurrentDateTime=", now.toString() );
		if ( View.equalsIgnoreCase(_True) ) {
			override_props.set ( "InitialView", "Graph" );
			override_props.set ( "PreviewOutput", "True" );
		}
		if (	(IOUtil.isBatch() &&
			(RunMode.equalsIgnoreCase("GUIAndBatch") ||
			(RunMode.equalsIgnoreCase("Batch")))) ||
			(!IOUtil.isBatch() &&
			(RunMode.equalsIgnoreCase("GUIAndBatch") ||
			(RunMode.equalsIgnoreCase("GUIOnly")))) ) {
			// Only run the command for the requested run mode...
			TSProcessor p = new TSProcessor();
			if ( tsview_window_listener != null ) {
				p.addTSViewWindowListener (
					tsview_window_listener );
			}
			p.addTSSupplier ( (TSSupplier)processor );
			TSProduct tsp = new TSProduct (
						IOUtil.getPathUsingWorkingDir(
						TSProductFile),
						override_props );
			// Specify annotation providers if available...
			Vector ap_Vector = null;			
			try { Object o = processor.getPropContents (
					"TSProductAnnotationProviderList" );
					if ( o != null ) {
							ap_Vector = (Vector)o;
					}
			}
			catch ( Exception e ) {
				// Not fatal, but of use to developers.
				message =
					"Error requesting TSProductAnnotationProviderList from processor - not using.";
				Message.printDebug(10, routine, message );
			}
			
			if ( ap_Vector != null ) {
				int size = ap_Vector.size();
				TSProductAnnotationProvider ap;
				for ( int i = 0; i < size; i++ ) {
					ap = (TSProductAnnotationProvider)
						ap_Vector.elementAt(i);
					tsp.addTSProductAnnotationProvider(
						ap, null);
				}
			}
			// Now process the product...
			p.processProduct ( tsp );
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Error processing TSProduct file \"" + TSProductFile + "\".";
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
	String TSProductFile = props.getValue("TSProductFile");
	String RunMode = props.getValue("RunMode");
	String View = props.getValue("View");
	String OutputFile = props.getValue("OutputFile");
	StringBuffer b = new StringBuffer ();
	if ( (TSProductFile != null) && (TSProductFile.length() > 0) ) {
		b.append ( "TSProductFile=\"" + TSProductFile + "\"" );
	}
	if ( (RunMode != null) && (RunMode.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "RunMode=" + RunMode );
	}
	if ( (View != null) && (View.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "View=" + View );
	}
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}
