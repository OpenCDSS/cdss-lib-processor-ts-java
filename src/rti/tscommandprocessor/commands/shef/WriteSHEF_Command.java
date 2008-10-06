package rti.tscommandprocessor.commands.shef;

import java.io.File;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.ShefATS;
import RTi.TS.TS;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
<p>
This class initializes, checks, and runs the WriteSHEF() command.
</p>
*/
public class WriteSHEF_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
The lookup information for data types.
*/
Hashtable __DataTypePELookup = new Hashtable();

/**
Constructor.
*/
public WriteSHEF_Command ()
{	super();
	setCommandName ( "WriteSHEF" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters (	PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	//String TSList = parameters.getValue ( "TSList" );
	//String TSID = parameters.getValue ( "TSID" );
    String DataTypePELookup = parameters.getValue ( "DataTypePELookup" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
    // TODO SAM Evaluate whether other parameters are needed
	//String MissingValue = parameters.getValue ( "MissingValue" );
	//String Precision = parameters.getValue ( "Precision" );
	String warning = "";
	String message;
	
	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Parse and check the data type to PE lookup.  Save the results for use later.
	__DataTypePELookup.clear();
	if ( (DataTypePELookup != null) && !DataTypePELookup.equals("") ) {
	    Vector pairs = StringUtil.breakStringList(DataTypePELookup,";",0);
	    int pairsSize = 0;
	    if ( pairs != null ) {
	        pairsSize = pairs.size();
	    }
	    for ( int i = 0; i < pairsSize; i++ ) {
	        // Further break the pairs..
	        String onePair = ((String)pairs.get(i)).trim();
	        Vector pairParts = StringUtil.breakStringList(onePair,",",StringUtil.DELIM_SKIP_BLANKS);
	        int pairPartsSize = 0;
	        if ( pairParts != null ) {
	            pairPartsSize = pairParts.size();
	        }
	        if ( pairPartsSize != 2 ) {
	            message = "DataType/PE lookup pair \"" + onePair + "\" is not a comma-separated pair.";
	            warning += "\n" + message;
	            status.addToLog ( CommandPhaseType.INITIALIZATION,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Specify data type/PE lookup pairs as DataType,PE." ) );
	        }
	        else {
	            String dataType = ((String)pairParts.get(0)).trim();
	            String pe = ((String)pairParts.get(1)).trim();
	            __DataTypePELookup.put(dataType, pe);
	        }
	    } 
	}

	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
		message = "The output file must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify an output file." ) );
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
								message, "Software error - report to support." ) );
			}
	
		try {
			String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, OutputFile));
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				message = "The output file parent directory does not exist: \"" + f2 + "\".";
				warning += "\n" + message;
				status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Create the output directory." ) );
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

	if ( (OutputStart != null) && !OutputStart.equals("") &&
		!OutputStart.equalsIgnoreCase("OutputStart") &&
		!OutputStart.equalsIgnoreCase("OutputEnd") ) {
		try {
		    DateTime.parse(OutputStart);
		}
		catch ( Exception e ) {
			message = "Output start date/time \"" + OutputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid output start date/time, OutputStart, or OutputEnd." ) );
		}
	}
	if ( (OutputEnd != null) && !OutputEnd.equals("") &&
		!OutputEnd.equalsIgnoreCase("OutputStart") &&
		!OutputEnd.equalsIgnoreCase("OutputEnd") ) {
		try {
		    DateTime.parse( OutputEnd );
		}
		catch ( Exception e ) {
				message = "Output end date/time \"" + OutputEnd + "\" is not a valid date/time.";
				warning += "\n" + message;
				status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid output end date/time, OutputStart, or OutputEnd." ) );
		}
	}

    /*
	if ( (MissingValue != null) && !MissingValue.equals("") ) {
		if ( !StringUtil.isDouble(MissingValue) ) {
			message = "The missing value \"" + MissingValue + "\" is not a number.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify the missing value as a number." ) );
		}
	}

	if ( (Precision != null) && !Precision.equals("") ) {
		if ( !StringUtil.isInteger(Precision) ) {
			message = "The precision \"" + Precision + "\" is not an integer.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify the precision as an integer." ) );
		}
	}
    */
	
	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
	valid_Vector.add ( "TSList" );
	valid_Vector.add ( "TSID" );
	valid_Vector.add ( "DataTypePELookup" );
	valid_Vector.add ( "OutputFile" );
	valid_Vector.add ( "OutputStart" );
	valid_Vector.add ( "OutputEnd" );
	//valid_Vector.add ( "MissingValue" );
	//valid_Vector.add ( "Precision" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),
		warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Return the list of files that were created by this command.
*/
public List getGeneratedFileList ()
{
	Vector list = new Vector();
	if ( getOutputFile() != null ) {
		list.addElement ( getOutputFile() );
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
Get the PE code for the data type, for all time series, from the DataTypePELookup parameter.
This defines new PE types if specified in the __DataTypePELookup parameter data, and leaves
the contents as is otherwise.
@param tslist List of time series, from which to extract data types.
@param peList list of PE values (strings) corresponding to the time series data types.
 */
private void getPEForTimeSeries ( Vector tslist, Vector peList )
{   String routine = "WriteSHEF_Command.getPEForTimeSeries";
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    for ( int i = 0; i < size; i++ ) {
        TS ts = (TS)tslist.get(i);
        Object pe = __DataTypePELookup.get( ts.getDataType() );
        if ( pe != null ) {
            Message.printStatus(2, routine, "Using user-specified PE code \"" + pe +
                    "\" for data type \"" + ts.getDataType() + "\"" );
            peList.setElementAt(pe, i);
        }
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
	return (new WriteSHEF_JDialog ( parent, this )).ok();
}

// Use base class parseCommand()

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
{	String routine = "WriteSHEF_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;	// Warning level for non-user log messages
	
	// Clear the output file
	
	setOutputFile ( null );
	
	// Check whether the processor wants output files to be created...

	CommandProcessor processor = getCommandProcessor();
	if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
			Message.printStatus ( 2, routine,
			"Skipping \"" + toString() + "\" because output is not being created." );
	}

	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);

	// Get the time series to process...
	
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try { bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\" from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	Vector tslist = null;
	if ( o_TSList == null ) {
		message = "Unable to find time series to write using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( log_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report to software support." ) );
	}
	else {	tslist = (Vector)o_TSList;
		if ( tslist.size() == 0 ) {
			message = "Unable to find time series to write using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
			Message.printWarning ( log_level,
					MessageUtil.formatMessageTag(
							command_tag,++warning_count), routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report to software support." ) );
		}
	}
	
	if ( (tslist == null) || (tslist.size() == 0) ) {
		message = "Unable to find time series to write using TSID \"" + TSID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.WARNING,
						message, "Confirm that time series are available (may be OK for partial run)." ) );
	}

	String OutputStart = parameters.getValue ( "OutputStart" );
	DateTime OutputStart_DateTime = null;
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	DateTime OutputEnd_DateTime = null;

	if ( (OutputStart != null) && !OutputStart.equals("") ) {
		try {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", OutputStart );
		bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting OutputStart DateTime(DateTime=" + OutputStart + ") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for OutputStart DateTime(DateTime=" +
			OutputStart +	"\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	OutputStart_DateTime = (DateTime)prop_contents;
		}
	}
	catch ( Exception e ) {
		message = "OutputStart \"" + OutputStart + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify a valid output start date/time." ) );
		throw new InvalidCommandParameterException ( message );
	}
	}
	else {	// Get from the processor...
		try {	Object o = processor.getPropContents ( "OutputStart" );
				if ( o != null ) {
					OutputStart_DateTime = (DateTime)o;
				}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting OutputStart from processor - not using.";
			Message.printDebug(10, routine, message );
		}
	}
	if ( (OutputEnd != null) && !OutputEnd.equals("") ) {
			try {
			request_params = new PropList ( "" );
			request_params.set ( "DateTime", OutputEnd );
			bean = null;
			try { bean =
				processor.processRequest( "DateTime", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting OutputEnd DateTime(DateTime=" +	OutputEnd + ") from processor.";
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
				status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Report problem to software support." ) );
				throw new InvalidCommandParameterException ( message );
			}

			bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "DateTime" );
			if ( prop_contents == null ) {
				message = "Null value for OutputEnd DateTime(DateTime=" + OutputEnd +") returned from processor.";
				Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
				status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Report problem to software support." ) );
				throw new InvalidCommandParameterException ( message );
			}
			else {	OutputEnd_DateTime = (DateTime)prop_contents;
			}
		}
		catch ( Exception e ) {
			message = "OutputEnd \"" + OutputEnd + "\" is invalid.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid output start date/time." ) );
			throw new InvalidCommandParameterException ( message );
		}
		}
		else {	// Get from the processor...
			try {	Object o = processor.getPropContents ( "OutputEnd" );
					if ( o != null ) {
						OutputEnd_DateTime = (DateTime)o;
					}
			}
			catch ( Exception e ) {
				// Not fatal, but of use to developers.
				message = "Error requesting OutputEnd from processor - not using.";
				Message.printDebug(10, routine, message );
			}
	}

	//String Precision = parameters.getValue ( "Precision" );
	//String MissingValue = parameters.getValue ( "MissingValue" );
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Now try to write...

    String OutputFile_full = OutputFile;
	try {
		OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),OutputFile));
		Message.printStatus ( 2, routine,"Writing SHEF file \"" + OutputFile_full + "\"" );
		
		// Get the comments to add to the top of the file.

        /*
		Vector OutputComments_Vector = null;
		try { Object o = processor.getPropContents ( "OutputComments" );
			// Comments are available so use them...
			if ( o != null ) {
				OutputComments_Vector = (Vector)o;
			}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting OutputComments from processor - not using.";
			Message.printDebug(10, routine, message );
		}
        */

		try {	
            Vector units_Vector = null;
            // Get PE code from global information...
            Vector PE_Vector = ShefATS.getPEForTimeSeries ( tslist );
            // Get PE code from parameter
            getPEForTimeSeries ( tslist, PE_Vector );
            for ( int i = 0; i < tslist.size(); i++ ) {
                TS ts = (TS)tslist.get(i);
                if ( ((String)PE_Vector.get(i)).equals("") ) {
                    message = "Unable to determine SHEF PE code for \"" + ts.getIdentifier() +
                    "\" - not writing time series.";
                    Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(
                    command_tag, ++warning_count), routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Check log file for details." ) );
                }
            }
            Vector Duration_Vector = null;
            Vector AltID_Vector = null;
            PropList shef_props = new PropList ( "SHEF" );
            shef_props.set ( "HourMax=24" );
            ShefATS.writeTimeSeriesList ( tslist,
                OutputFile_full, OutputStart_DateTime, OutputEnd_DateTime,
                units_Vector, PE_Vector, Duration_Vector, AltID_Vector, shef_props );
			// Save the output file name...
			setOutputFile ( new File(OutputFile_full));
		}
		catch ( Exception e ) {
			Message.printWarning ( 3, routine, e );
			message = "Unexpected error writing SHEF file \"" + OutputFile_full + "\"";
			Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count), routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Check log file for details." ) );
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error writing time series to SHEF file \"" + OutputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check the log file for details." ) );
		throw new CommandException ( message );
	}
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
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
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String TSList = parameters.getValue("TSList");
	String TSID = parameters.getValue("TSID");
	String DataTypePELookup = parameters.getValue("DataTypePELookup");
	String OutputFile = parameters.getValue("OutputFile");
	String OutputStart = parameters.getValue("OutputStart");
	String OutputEnd = parameters.getValue("OutputEnd");
	//String MissingValue = parameters.getValue("MissingValue");
	//String Precision = parameters.getValue("Precision");
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
    if ( (DataTypePELookup != null) && (DataTypePELookup.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataTypePELookup=\"" + DataTypePELookup + "\"" );
    }
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}
	if ( (OutputStart != null) && (OutputStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputStart=\"" + OutputStart + "\"" );
	}
	if ( (OutputEnd != null) && (OutputEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputEnd=\"" + OutputEnd + "\"" );
	}
    /*
	if ( (MissingValue != null) && (MissingValue.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "MissingValue=" + MissingValue );
	}
	if ( (Precision != null) && (Precision.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Precision=" + Precision );
	}
    */
	return getCommandName() + "(" + b.toString() + ")";
}

}
