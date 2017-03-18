package rti.tscommandprocessor.commands.shef;

import java.io.File;
import java.util.ArrayList;
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
This class initializes, checks, and runs the WriteSHEF() command.
*/
public class WriteSHEF_Command extends AbstractCommand implements Command, FileGenerator
{
    
/**
Data members used for parameter values.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
The lookup information for data types.
*/
Hashtable<String,String> __DataTypePELookup = new Hashtable<String,String>();

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
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	//String TSList = parameters.getValue ( "TSList" );
	//String TSID = parameters.getValue ( "TSID" );
    String DataTypePELookup = parameters.getValue ( "DataTypePELookup" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String Append = parameters.getValue ( "Append" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String Precision = parameters.getValue ( "Precision" );
	// TODO SAM 2009-04-22 Need to add checks for other parameters but don't want to constrain if don't fully
	// understand SHEF options
	String warning = "";
	String message;
	
	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Parse and check the data type to PE lookup.  Save the results for use later.
	__DataTypePELookup.clear();
	if ( (DataTypePELookup != null) && !DataTypePELookup.equals("") && (DataTypePELookup.indexOf("${") < 0) ) {
		List<String> pairs = StringUtil.breakStringList(DataTypePELookup,";",0);
	    int pairsSize = 0;
	    if ( pairs != null ) {
	        pairsSize = pairs.size();
	    }
	    for ( int i = 0; i < pairsSize; i++ ) {
	        // Further break the pairs..
	        String onePair = pairs.get(i).trim();
	        List<String> pairParts = StringUtil.breakStringList(onePair,",",StringUtil.DELIM_SKIP_BLANKS);
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
	            String dataType = pairParts.get(0).trim();
	            String pe = pairParts.get(1).trim();
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
	else if (OutputFile.indexOf("${") < 0) {
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
	
    if ( (Append != null) && !Append.equals("") ) {
        if ( !Append.equalsIgnoreCase(_False) && !Append.equalsIgnoreCase(_True) ) {
            message = "The Append parameter \"" + Append + "\" must be " + _False + " (default) or " + _True + ".";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the parameter as False or True."));
        }
    }

	if ( (OutputStart != null) && !OutputStart.equals("") &&
		!OutputStart.equalsIgnoreCase("OutputStart") &&
		!OutputStart.equalsIgnoreCase("OutputEnd") && (OutputStart.indexOf("${") < 0) ) {
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
		!OutputEnd.equalsIgnoreCase("OutputEnd") && (OutputEnd.indexOf("${") < 0)) {
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

	if ( (Precision != null) && !Precision.equals("") ) {
		if ( !StringUtil.isInteger(Precision) ) {
			message = "The precision \"" + Precision + "\" is not an integer.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify the precision as an integer." ) );
		}
	}
	
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(13);
	validList.add ( "TSList" );
	validList.add ( "TSID" );
	validList.add ( "LocationID" );
	validList.add ( "DataTypePELookup" );
	validList.add ( "OutputFile" );
	validList.add ( "Append" );
	validList.add ( "OutputStart" );
	validList.add ( "OutputEnd" );
	validList.add ( "TimeZone" );
	validList.add ( "ObservationTime" );
	validList.add ( "CreationDate" );
	validList.add ( "Duration" );
	validList.add ( "Precision" );
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
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
	List<File> list = new Vector<File>();
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
Get the PE code for the data type, for all time series, from the DataTypePELookup parameter.
This defines new PE types if specified in the __DataTypePELookup parameter data, and leaves
the contents as is otherwise.
@param tslist List of time series, from which to extract data types.
@param peList list of PE values (strings) corresponding to the time series data types.
 */
private void getPEForTimeSeries ( List<TS> tslist, List<String> peList )
{   String routine = "WriteSHEF_Command.getPEForTimeSeries";
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    for ( int i = 0; i < size; i++ ) {
        TS ts = tslist.get(i);
        String pe = __DataTypePELookup.get( ts.getDataType() );
        if ( pe != null ) {
            Message.printStatus(2, routine, "Using user-specified PE code \"" + pe +
                "\" for data type \"" + ts.getDataType() + "\"" );
            peList.set(i,pe);
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could nt produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;	// Warning level for non-user log messages

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
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
	
	// Clear the output file
	
	setOutputFile ( null );
	
	// Check whether the processor wants output files to be created...

	if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
			Message.printStatus ( 2, routine,
			"Skipping \"" + toString() + "\" because output is not being created." );
	}

	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) && !TSID.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
	String LocationID = parameters.getValue ( "LocationID" );
	if ( (LocationID != null) && (LocationID.indexOf("${") >= 0) && !LocationID.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		LocationID = TSCommandProcessorUtil.expandParameterValue(processor, this, LocationID);
	}
	String OutputFile = parameters.getValue ( "OutputFile" ); // Expanded below
    String Append = parameters.getValue ( "Append" );
    if ( (Append == null) || Append.equals("") ) {
        Append = _False; // Default
    }
    boolean Append_boolean = false;
    if ( Append.equalsIgnoreCase(_True) ) {
        Append_boolean = true;
    }
    String OutputStart = parameters.getValue ( "OutputStart" ); // Expand below
    if ( (OutputStart == null) || OutputStart.isEmpty() ) {
    	OutputStart = "${OutputStart}"; // Default global property
    }
    String OutputEnd = parameters.getValue ( "OutputEnd" ); // Expand below
    if ( (OutputEnd == null) || OutputEnd.isEmpty() ) {
    	OutputEnd = "${OutputEnd}"; // Default global property
    }
    String TimeZone = parameters.getValue ( "TimeZone" );
	if ( (TimeZone != null) && (TimeZone.indexOf("${") >= 0) && !TimeZone.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		TimeZone = TSCommandProcessorUtil.expandParameterValue(processor, this, TimeZone);
	}
    String ObservationTime = parameters.getValue ( "ObservationTime" );
	if ( (ObservationTime != null) && (ObservationTime.indexOf("${") >= 0) && !ObservationTime.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		ObservationTime = TSCommandProcessorUtil.expandParameterValue(processor, this, ObservationTime);
	}
    String CreationDate = parameters.getValue ( "CreationDate" );
	if ( (CreationDate != null) && (CreationDate.indexOf("${") >= 0) && !CreationDate.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		CreationDate = TSCommandProcessorUtil.expandParameterValue(processor, this, CreationDate);
	}
    String Duration = parameters.getValue ( "Duration" );
	if ( (Duration != null) && (Duration.indexOf("${") >= 0) && !Duration.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		Duration = TSCommandProcessorUtil.expandParameterValue(processor, this, Duration);
	}
    String Precision = parameters.getValue ( "Precision" );
    int Precision_int = -1; // Default
    if ( StringUtil.isInteger(Precision) ) {
        Precision_int = Integer.parseInt(Precision);
    }
    
    // Get the time series to process...
	
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try {
	    bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\") from processor.";
		Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),routine, message );
		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
			message, "Report to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	List<TS> tslist = null;
	if ( o_TSList == null ) {
		message = "Unable to find time series to write using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
		Message.printWarning ( log_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
			message, "Report to software support." ) );
	}
	else {
		@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
	    tslist = tslist0;
		if ( tslist.size() == 0 ) {
			message = "Unable to find time series to write using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
			Message.printWarning ( log_level, MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
			status.addToLog ( CommandPhaseType.RUN,	new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Report to software support." ) );
		}
	}
	
	if ( (tslist == null) || (tslist.size() == 0) ) {
		message = "Unable to find time series to write using TSID \"" + TSID + "\".";
		Message.printWarning ( warning_level,
	        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
			message, "Confirm that time series are available (may be OK for partial run)." ) );
	}

	DateTime OutputStart_DateTime = null;
	DateTime OutputEnd_DateTime = null;
	
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
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
            	TSCommandProcessorUtil.expandParameterValue(processor, this, OutputFile)));
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
			List<String> units_Vector = null;
            // Get PE code from global information...
			List<String> PE_Vector = ShefATS.getPEForTimeSeries ( tslist );
            // Get PE code from parameter
            getPEForTimeSeries ( tslist, PE_Vector );
            for ( int i = 0; i < tslist.size(); i++ ) {
                TS ts = (TS)tslist.get(i);
                if ( ((String)PE_Vector.get(i)).equals("") ) {
                    message = "Unable to determine SHEF PE code for \"" + ts.getIdentifier() +
                    "\" - not writing time series.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                    status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check log file for details." ) );
                }
            }
            List<String> Duration_Vector = null;
            List<String> AltIDList = new ArrayList<String>(tslist.size());
            for ( TS ts : tslist ) {
            	// Add identifier for output, same as time series location ID by default...
            	String locationID = ts.getLocation();
            	// If specified as a property, expand here
            	if ( (LocationID != null) && !LocationID.isEmpty() ) { 
            		locationID = TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, LocationID, status, commandPhase);
            	}
            	AltIDList.add(locationID);
            }
            // TODO SAM 2009-04-22 Why is this needed?
            int hourMax = 24;
            ShefATS.writeTimeSeriesList ( tslist,
                OutputFile_full, Append_boolean, OutputStart_DateTime, OutputEnd_DateTime,
                units_Vector, PE_Vector, Duration_Vector, AltIDList, TimeZone, ObservationTime,
                CreationDate, Duration, hourMax, Precision_int );
			// Save the output file name...
			setOutputFile ( new File(OutputFile_full));
		}
		catch ( Exception e ) {
			Message.printWarning ( 3, routine, e );
			message = "Unexpected error writing SHEF file \"" + OutputFile_full + "\"";
			Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count), routine, message );
			status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Check log file for details." ) );
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error writing time series to SHEF file \"" + OutputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
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
	String LocationID = parameters.getValue("LocationID");
	String DataTypePELookup = parameters.getValue("DataTypePELookup");
	String OutputFile = parameters.getValue("OutputFile");
	String Append = parameters.getValue("Append");
	String OutputStart = parameters.getValue("OutputStart");
	String OutputEnd = parameters.getValue("OutputEnd");
	String TimeZone = parameters.getValue("TimeZone");
    String ObservationTime = parameters.getValue("ObservationTime");
    String CreationDate = parameters.getValue("CreationDate");
    String Duration = parameters.getValue("Duration");
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
	if ( (LocationID != null) && (LocationID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "LocationID=\"" + LocationID + "\"" );
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
    if ( (Append != null) && (Append.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Append=" + Append );
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
	if ( (TimeZone != null) && (TimeZone.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TimeZone=\"" + TimeZone + "\"");
	}
    if ( (ObservationTime != null) && (ObservationTime.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ObservationTime=\"" + ObservationTime + "\"");
    }
    if ( (CreationDate != null) && (CreationDate.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CreationDate=\"" + CreationDate + "\"");
    }
    if ( (Duration != null) && (Duration.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Duration=\"" + Duration + "\"");
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