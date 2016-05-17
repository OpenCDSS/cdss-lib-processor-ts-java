package rti.tscommandprocessor.commands.delftfews;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

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
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ReadDelftFewsPiXml() command.
*/
public class ReadDelftFewsPiXml_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Indicate output of command.
*/
protected final String _TimeSeries = "TimeSeries";
protected final String _TimeSeriesAndEnsembles = "TimeSeriesAndEnsembles";

/**
Indicate output of command.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
TSEnsemble created in discovery mode (basically to get the identifier for other commands).
*/
private TSEnsemble __tsensemble = null;

/**
List of time series read during discovery if not reading ensemble.  These are TS objects but with mainly the metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public ReadDelftFewsPiXml_Command ()
{
	super();
	setCommandName ( "ReadDelftFewsPiXml" );
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
{
    String routine = getClass().getSimpleName() + ".checkCommandParameters";
	String warning = "";
    String message;
    
    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Get the property values. 
	String InputFile = parameters.getValue("InputFile");
	String NewUnits = parameters.getValue("NewUnits");
	String InputStart = parameters.getValue("InputStart");
	String InputEnd = parameters.getValue("InputEnd");
	String TimeZoneOffset = parameters.getValue("TimeZoneOffset");
	String Read24HourAsDay = parameters.getValue("Read24HourAsDay");
	String Read24HourAsDayCutoff = parameters.getValue("Read24HourAsDayCutoff");
    
    if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify an existing input file." ) );
    }
    else if ( InputFile.indexOf("${") < 0 ){
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

	if ( NewUnits != null ) {
		// Will check at run time
	}

	// InputStart
	DateTime inputStart = null, inputEnd = null;
	if ((InputStart != null) && !InputStart.isEmpty() && !InputStart.startsWith("${")) {
		try {
			inputStart = DateTime.parse(InputStart);
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
	if ((InputEnd != null) && !InputEnd.isEmpty() && !InputEnd.startsWith("${")) {
		try {
			inputEnd = DateTime.parse(InputEnd);
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
	if ( inputStart != null && inputEnd != null ) {
		if ( inputStart.greaterThan( inputEnd ) ) {
            message = InputStart + " (" + InputStart  + ") should be <= InputEnd (" + InputEnd + ").";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an input start <= the input end." ) );
		}
	}
	
    if ( (TimeZoneOffset != null) && !TimeZoneOffset.isEmpty() && !StringUtil.isInteger(TimeZoneOffset) ) {
        message = "The TimeZoneOffset parameter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify TimeZoneOffset as an integer offset from GMT.") );
    }
	
    if ( (Read24HourAsDay != null) && !Read24HourAsDay.isEmpty() && !Read24HourAsDay.equalsIgnoreCase(_False) &&
    	!Read24HourAsDay.equalsIgnoreCase(_True) ) {
        message = "The Read24HourAsDay parameter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify Read24HourAsDay as " + _False + " (default) or " + _True + ".") );
    }
    
    if ( (Read24HourAsDayCutoff != null) && !Read24HourAsDayCutoff.isEmpty() ) {
    	if ( !StringUtil.isInteger(Read24HourAsDayCutoff) ) {
	        message = "The Read24HourAsDayCutoff parameter is invalid.";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
	            message, "Specify Read24HourAsDayCutoff as an integer >= 0 and <= 23.") );
    	}
    	else {
    		int cutoff = Integer.parseInt(Read24HourAsDayCutoff);
        	if ( (cutoff < 0) || (cutoff > 23) ) {
    	        message = "The Read24HourAsDayCutoff parameter is invalid.";
    	        warning += "\n" + message;
    	        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
    	            message, "Specify Read24HourAsDayCutoff as an integer >= 0 and <= 23.") );
        	}
    	}
    }
    
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(14);
    validList.add ( "InputFile" );
    validList.add ( "Output" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "TimeZoneOffset" );
    validList.add ( "TimeZone" );
    validList.add ( "DataSource" );
    validList.add ( "DataType" );
    validList.add ( "Description" );
    validList.add ( "Read24HourAsDay" );
    validList.add ( "Read24HourAsDayCutoff" );
    //validList.add ( "NewUnits" );
    validList.add ( "Alias" );
    validList.add ( "EnsembleID" );
    validList.add ( "EnsembleName" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, warning_level ), warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	
	// The command will be modified if changed...
	return ( new ReadDelftFewsPiXml_JDialog ( parent, this ) ).ok();
}

/**
Return the ensemble that is read by this class when run in discovery mode.
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
    return __discoveryTSList;
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
    // Since all time series must be the same interval, check the class for the first one (e.g., HourTS)
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
    }
    else if ( c == TSEnsemble.class ) {
        TSEnsemble ensemble = getDiscoveryEnsemble();
        if ( ensemble == null ) {
            return null;
        }
        else {
        	List<TSEnsemble> v = new ArrayList<TSEnsemble>();
            v.add ( ensemble );
            return v;
        }
    }
    else {
        return null;
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_level = 2;
    int log_level = 3;
	String command_tag = "" + command_number;
	int warning_count = 0;
	    
    // Get and clear the status and clear the run log...
    
    CommandStatus status = getCommandStatus();
    CommandProcessor processor = getCommandProcessor();
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
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
    	setDiscoveryEnsemble ( null );
        setDiscoveryTSList ( null );
    }

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile");
	String Output = parameters.getValue("Output");
	if ( (Output == null) || Output.isEmpty() ) {
		Output = _TimeSeriesAndEnsembles; // Default
	}
	String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}"; // Global default
	}
	String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}"; // Global default
	}
	String TimeZoneOffset = parameters.getValue("TimeZoneOffset");
	Integer timeZoneOffset = null;
	if ( (TimeZoneOffset != null) && !TimeZoneOffset.isEmpty() ) {
		timeZoneOffset = Integer.parseInt(TimeZoneOffset);
	}
	String TimeZone = parameters.getValue("TimeZone");
	if ( (TimeZone != null) && TimeZone.indexOf("${") >= 0 ) {
		TimeZone = TSCommandProcessorUtil.expandParameterValue(processor,this,TimeZone);
	}
	String DataSource = parameters.getValue("DataSource");
	if ( (DataSource != null) && DataSource.indexOf("${") >= 0 ) {
		DataSource = TSCommandProcessorUtil.expandParameterValue(processor,this,DataSource);
	}
	String DataType = parameters.getValue("DataType");
	if ( (DataType != null) && DataType.indexOf("${") >= 0 ) {
		DataType = TSCommandProcessorUtil.expandParameterValue(processor,this,DataType);
	}
	String Description = parameters.getValue("Description");
	if ( (Description != null) && Description.indexOf("${") >= 0 ) {
		Description = TSCommandProcessorUtil.expandParameterValue(processor,this,Description);
	}
	String Read24HourAsDay = parameters.getValue("Read24HourAsDay");
	boolean read24HourAsDay = false;
	if ( (Read24HourAsDay != null) && Read24HourAsDay.equalsIgnoreCase(_True) ) {
		read24HourAsDay = true;
	}
	String Read24HourAsDayCutoff = parameters.getValue("Read24HourAsDayCutoff");
	int read24HourAsDayCutoff = 0; // default
	if ( (Read24HourAsDayCutoff != null) && Read24HourAsDayCutoff.equalsIgnoreCase(_True) ) {
		read24HourAsDayCutoff = Integer.parseInt(Read24HourAsDayCutoff);
	}
	//String NewUnits = parameters.getValue("NewUnits");
	String Alias = parameters.getValue("Alias");
	String EnsembleID = parameters.getValue("EnsembleID");
	if ( (EnsembleID != null) && EnsembleID.indexOf("${") >= 0 ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor,this,EnsembleID);
	}
	String EnsembleName = parameters.getValue("EnsembleName");
	if ( (EnsembleName != null) && EnsembleName.indexOf("${") >= 0 ) {
		EnsembleName = TSCommandProcessorUtil.expandParameterValue(processor,this,EnsembleName);
	}
    
	// InputStart
	DateTime InputStart_DateTime = null;
	DateTime InputEnd_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			InputStart_DateTime = TSCommandProcessorUtil.getDateTime ( InputStart, "InputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			InputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( InputEnd, "InputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
	}
	if ( (TimeZone != null) && !TimeZone.isEmpty() ) {
		if ( InputStart_DateTime != null ) {
			InputStart_DateTime.setTimeZone(TimeZone);
		}
		if ( InputEnd_DateTime != null ) {
			InputEnd_DateTime.setTimeZone(TimeZone);
		}
	}
	
	// Read the file.
    List<TS> tsList = new ArrayList<TS>(); // Keep the list of time series
    List<TSEnsemble> ensembleList = new ArrayList<TSEnsemble>();
    List<String> problems = new ArrayList<String>();
    String InputFile_full = InputFile;
	try {
        boolean readData = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ){
            readData = false;
        }
        InputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        if ( !IOUtil.fileExists(InputFile_full) ) {
            if ( commandPhase == CommandPhaseType.DISCOVERY ) {
                message = "Input file does not exist:  \"" + InputFile_full + "\".";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Verify that filename is correct and that the file exists - " +
                        	"may be OK if file is created during processing." ) );
            }
            else if ( commandPhase == CommandPhaseType.RUN ) {
                message = "Input file does not exist:  \"" + InputFile_full + "\".";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that filename is correct and that the file exists." ) );
            }
        }
        else {
            // Read everything in the file (one time series or traces).
        	DelftFewsPiXmlReader reader = new DelftFewsPiXmlReader ( InputFile_full );
            reader.readTimeSeriesList (
                InputStart_DateTime, InputEnd_DateTime, timeZoneOffset, TimeZone, DataSource, DataType,
                Description, read24HourAsDay, read24HourAsDayCutoff, null,
                Output, EnsembleID, EnsembleName, readData, problems );
            tsList = reader.getTimeSeriesList();
            ensembleList = reader.getEnsembleList();
            int count = 0;
            for ( String problem : problems ) {
            	++count;
            	if ( count == 500 ) {
            		break;
            	}
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, problem );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        problem, "Check command input." ) );
            }
        }
			
		if ( tsList != null ) {
			int tscount = tsList.size();
			message = "Read " + tscount + " time series from \"" + InputFile_full + "\"";
			Message.printStatus ( 2, routine, message );
	        if ( (Alias != null) && !Alias.equals("") ) {
                for ( TS ts : tsList ) {
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, Alias, status, commandPhase);
                    ts.setAlias ( alias );
                }
            }
		}
	}
	catch ( FileNotFoundException e ) {
        message = "PI XML file \"" + InputFile_full + "\" is not found or accessible.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
            status.addToLog(commandPhase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,
                    "Verify that the file exists and is readable."));
        throw new CommandException ( message );
	}
	catch ( Exception e ) {
		message = "Unexpected error reading PI XML file. \"" + InputFile_full + "\" (" + e + ")";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(commandPhase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,"Check the log file for details."));
		throw new CommandException ( message );
	}
    
    if ( commandPhase == CommandPhaseType.RUN ) {
        if ( tsList != null ) {
            // Further process the time series...
            // This makes sure the period is at least as long as the output period...
            int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tsList );
            if ( wc > 0 ) {
                message = "Error post-processing series after read.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
    
            // Now add the list in the processor...
            
            int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tsList );
            if ( wc2 > 0 ) {
                message = "Error adding time series after read.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
        }
        
        if ( ensembleList.size() > 0 ) {
        	// Transfer some properties from the time series to the ensemble
    		TSEnsemble ensemble = ensembleList.get(0);
    		List<TS>tslist = ensemble.getTimeSeriesList(false);
        	if ( tslist.size() > 0 ) {
        		TS ts = tslist.get(0);
        		// Set properties that are relevant to an ensemble - mostly at station level since time series hold time series properties
        		//
        		// Allow the ensemble ID and name to be reset using time series properties
        		// - this is OK because it has not yet been added to the processor for management
        		/* TODO SAM 2016-05-16 Don't seem to need this because ${ts:locationId} is already recognized for EnsembleID
        		if ( ensemble.getEnsembleID().indexOf("${") >= 0 ) {
        			String ensembleID = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, ensemble.getEnsembleID(), status, commandPhase);
        			
        			ensemble.setEnsembleID(ensembleID);
        		}
        		if ( ensemble.getEnsembleName().indexOf("${") >= 0 ) {
        			String ensembleName = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, ensemble.getEnsembleName(), status, commandPhase);
        			ensemble.setEnsembleName(ensembleName);
        		}*/
        		Object o = ts.getProperty("ensembleId");
        		if ( o != null) {
        			ensemble.setProperty("ensembleId",ts.getProperty("ensembleId")); 
        		}
        		o = ts.getProperty("forecastDate");
        		if ( o != null) {
        			ensemble.setProperty("forecastDate",ts.getProperty("forecastDate")); 
        		}
        		o = ts.getProperty("lat");
        		if ( o != null) {
        			ensemble.setProperty("lat",ts.getProperty("lat")); 
        		}
        		o = ts.getProperty("lon");
        		if ( o != null) {
        			ensemble.setProperty("lon",ts.getProperty("lon")); 
        		}
        		o = ts.getProperty("locationId");
        		if ( o != null) {
        			ensemble.setProperty("locationId",ts.getProperty("locationId")); 
        		}
        		o = ts.getProperty("stationName");
        		if ( o != null) {
        			ensemble.setProperty("stationName",ts.getProperty("stationName")); 
        		}
        		o = ts.getProperty("x");
        		if ( o != null) {
        			ensemble.setProperty("x",ts.getProperty("x"));
        		}
        		o = ts.getProperty("y");
        		if ( o != null) {
        			ensemble.setProperty("y",ts.getProperty("y"));
        		}
        		o = ts.getProperty("z");
        		if ( o != null) {
        			ensemble.setProperty("z",ts.getProperty("z")); 
        		}
        	}
            // Add first ensemble in ensemble list to the processor...
            TSCommandProcessorUtil.appendEnsembleToResultsEnsembleList(processor, this, ensemble);
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( tsList );
        if ( ensembleList.size() > 0 ) {
        	setDiscoveryEnsemble ( ensembleList.get(0) );
        }
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
    __discoveryTSList = discovery_TS_Vector;
}

/**
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props )
{
    if ( props == null ) {
        return getCommandName() + "()";
    }

	String InputFile = props.getValue("InputFile" );
	String Output = props.getValue("Output");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");
	String TimeZoneOffset = props.getValue("TimeZoneOffset");
	String TimeZone = props.getValue("TimeZone");
	String DataSource = props.getValue("DataSource");
	String DataType = props.getValue("DataType");
	String Description = props.getValue("Description");
	String Read24HourAsDay = props.getValue("Read24HourAsDay");
	String Read24HourAsDayCutoff = props.getValue("Read24HourAsDayCutoff");
	//String NewUnits = props.getValue("NewUnits");
	String Alias = props.getValue("Alias");
	String EnsembleID = props.getValue("EnsembleID");
	String EnsembleName = props.getValue("EnsembleName");

	StringBuilder b = new StringBuilder ();

	if ((InputFile != null) && (InputFile.length() > 0)) {
		b.append("InputFile=\"" + InputFile + "\"");
	}
	if ((Output != null) && (Output.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("Output=" + Output );
	}
	if ((TimeZoneOffset != null) && (TimeZoneOffset.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("TimeZoneOffset=" + TimeZoneOffset );
	}
	if ((TimeZone != null) && (TimeZone.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("TimeZone=\"" + TimeZone + "\"");
	}
	if ((InputStart != null) && (InputStart.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("InputStart=\"" + InputStart + "\"");
	}
	if ((InputEnd != null) && (InputEnd.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("InputEnd=\"" + InputEnd + "\"");
	}
	if ((DataSource != null) && (DataSource.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("DataSource=\"" + DataSource + "\"");
	}
	if ((DataType != null) && (DataType.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("DataType=\"" + DataType + "\"");
	}
	if ((Description != null) && (Description.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("Description=\"" + Description + "\"");
	}
	if ((Read24HourAsDay != null) && (Read24HourAsDay.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("Read24HourAsDay=" + Read24HourAsDay );
	}
	if ((Read24HourAsDayCutoff != null) && (Read24HourAsDayCutoff.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("Read24HourAsDayCutoff=" + Read24HourAsDayCutoff );
	}
	
	// New Units
	/*
	if ((NewUnits != null) && (NewUnits.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("NewUnits=\"" + NewUnits + "\"");
	}
	*/
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }
    if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
    }
    if ( (EnsembleName != null) && (EnsembleName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleName=\"" + EnsembleName + "\"" );
    }

    return getCommandName() + "("+ b.toString()+")";
}

}