package rti.tscommandprocessor.commands.usgs.nwis.instantaneous;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisFormatType;
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisParameterType;
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisSiteStatusType;
import rti.tscommandprocessor.commands.usgs.nwis.daily.UsgsNwisSiteType;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.TS.TS;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the ReadUsgsNwisInstantaneous() command.
*/
public class ReadUsgsNwisInstantaneous_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, FileGenerator
{
	
	/**
	Possible values for WaterMLRequireDataToMatchInterval parameter.
	*/
	protected final String _False = "False";
	protected final String _True = "True";

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Bounding box coordinate WestLon, SouthLat, EastLon, NorthLat
*/
double [] __boundingBox = null; // Use null to indicate no bounding box specified

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public ReadUsgsNwisInstantaneous_Command ()
{	super();
	setCommandName ( "ReadUsgsNwisInstantaneous" );
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
{	String warning = "";
    String message;
    
    String DataStore = parameters.getValue ( "DataStore" );
    String Sites = parameters.getValue ( "Sites" );
    String States = parameters.getValue ( "States" );
    String HUCs = parameters.getValue ( "HUCs" );
    String BoundingBox = parameters.getValue ( "BoundingBox" );
    String Counties = parameters.getValue ( "Counties" );
    String InputStart = parameters.getValue ( "InputStart" );
    String InputEnd = parameters.getValue ( "InputEnd" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String OutputFormat = parameters.getValue ( "OutputFormat" );
    String WaterMLInterval = parameters.getValue ( "WaterMLInterval" );
    String WaterMLRequireDataToMatchInterval = parameters.getValue ( "WaterMLRequireDataToMatchInterval" );

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (DataStore == null) || DataStore.equals("") ) {
        message = "The data store must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store." ) );
    }

    int locCount = 0; // Count of location parameters (only 1 allowed)
    if ( (Sites != null) && !Sites.equals("") ) {
        ++locCount;
    }
    if ( (States != null) && !States.equals("") ) {
        ++locCount;
    }
    if ( (HUCs != null) && !HUCs.equals("") ) {
        ++locCount;
    }
    __boundingBox = null;
    if ( (BoundingBox != null) && !BoundingBox.equals("") && (BoundingBox.indexOf("${") < 0) ) {
        // Make sure that 4 numbers are specified
        ++locCount;
        String [] parts = BoundingBox.split(",");
        if ( parts == null ) {
            message = "The bounding box (" + BoundingBox + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the bounding box as WestLonDeg,SouthLatDeg,EastLonDeg,NorthLatDeg." ) );
        }
        else {
            if ( parts.length != 4 ) {
                message = "The bounding box (" + BoundingBox + ") is invalid.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the bounding box as WestLonDeg,SouthLatDeg,EastLonDeg,NorthLatDeg." ) );
            }
            __boundingBox = new double[4];
            for ( int i = 0; i < parts.length; i++ ) {
                try {
                    __boundingBox[i] = Double.parseDouble(parts[i].trim());
                }
                catch ( NumberFormatException e ) {
                    message = "The bounding box (" + BoundingBox + ") part " + (i + 1) + " is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the bounding box as WestLonDeg,SouthLatDeg,EastLonDeg,NorthLatDeg." ) );
                }
                if ( ((i == 0) || (i == 2)) && ((__boundingBox[i] < -180.0) || (__boundingBox[i] > 180.0)) ) {
                    message = "The bounding box longitude (" + __boundingBox[i] + ") is not in range -180 to 180.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the longitude in range -180 to 180." ) );
                }
                if ( ((i == 1) || (i == 3)) && ((__boundingBox[i] < -90.0) || (__boundingBox[i] > 90.0)) ) {
                    message = "The bounding box latitude (" + __boundingBox[i] + ") is not in range -90 to 90.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the latitude in range -90 to 90." ) );
                }
            }
        }
    }
    if ( (Counties != null) && !Counties.equals("") ) {
        ++locCount;
    }
    if ( locCount != 1 ) {
        message = "Only one location constraint can be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify one location constraint." ) );
    }

	// TODO SAM 2006-04-24 Need to check the WhereN parameters.

	if ( (InputStart != null) && !InputStart.equals("") && (InputStart.indexOf("${") < 0) &&
		!InputStart.equalsIgnoreCase("InputStart") && !InputStart.equalsIgnoreCase("InputEnd") ) {
		try {
		    DateTime.parse(InputStart);
		}
		catch ( Exception e ) {
            message = "The input start date/time \"" + InputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a date/time or InputStart." ) );
		}
	}
	if ( (InputEnd != null) && !InputEnd.equals("") && (InputEnd.indexOf("${") < 0) &&
		!InputEnd.equalsIgnoreCase("InputStart") && !InputEnd.equalsIgnoreCase("InputEnd") ) {
		try {
		    DateTime.parse( InputEnd );
		}
		catch ( Exception e ) {
            message = "The input end date/time \"" + InputEnd + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a date/time or InputEnd." ) );
		}
	}
	
    if ( (OutputFile != null) && (OutputFile.length() != 0) && (OutputFile.indexOf("${") < 0) ) {
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
            if ( o != null ) {
                working_dir = (String)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Software error - report the problem to support." ) );
        }

        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                    TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The output file parent directory does not exist for: \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
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
            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that output file and working directory paths are compatible." ) );
        }
    }

    if ( (OutputFormat == null) || OutputFormat.isEmpty() || OutputFormat.equalsIgnoreCase("WaterML") ) {
	    if ( WaterMLInterval == null || (WaterMLInterval.length() == 0) ) {
	        message = "The WaterML data interval must be specified.";
	        warning += "\n" + message;
	        status.addToLog(CommandPhaseType.INITIALIZATION,
	                new CommandLogRecord(
	                CommandStatusType.FAILURE, message, "Specify a WaterML data interval."));
	    }
	    else {
	        try {
	            TimeInterval.parseInterval(WaterMLInterval);
	        }
	        catch ( Exception e ) {
	            // Should not happen because choices are valid
	            message = "The WaterML data interval \"" + WaterMLInterval + "\" is invalid.";
	            warning += "\n" + message;
	            status.addToLog(CommandPhaseType.INITIALIZATION,
	                new CommandLogRecord(
	                CommandStatusType.FAILURE, message, "Specify a WaterML data interval using the command editor."));
	        }
	    }
	
	    if ( WaterMLRequireDataToMatchInterval != null && !(WaterMLRequireDataToMatchInterval.equalsIgnoreCase(_True)) && 
	        !(WaterMLRequireDataToMatchInterval.equalsIgnoreCase(_False)) && !(WaterMLRequireDataToMatchInterval.equalsIgnoreCase(""))) {
	        message = "WaterMLRequireDataToMatchInterval is invalid.";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "WaterMLRequireDataToMatchInterval must " + _False + " or " + _True + " (default)." ) );
	    }
    }

    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(17);
    validList.add ( "DataStore" );
    validList.add ( "Sites" );
    validList.add ( "States" );
    validList.add ( "HUCs" );
    validList.add ( "BoundingBox" );
    validList.add ( "Counties" );
    validList.add ( "Parameters" );
    validList.add ( "SiteStatus" );
    validList.add ( "SiteTypes" );
    validList.add ( "Agency" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "Alias" );
    validList.add ( "Format" );
    validList.add ( "OutputFile" );
    validList.add ( "WaterMLInterval" );
    validList.add ( "WaterMLRequireDataToMatchInterval" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), warning );
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
{   // The command will be modified if changed...
    return (new ReadUsgsNwisInstantaneous_JDialog ( parent, this )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
    List<File> list = new ArrayList<File>();
    if ( getOutputFile() != null ) {
        list.add ( getOutputFile() );
    }
    return list;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Also check the base class
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
    return __OutputFile_File;
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
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
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
    
    boolean readData = true;
    boolean propertiesUsedInParameters = false; // Don't call the read method if any ${Property} are used because it will cause issues
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
        readData = false;
    }
    
    String dataStoreName = parameters.getValue("DataStore");
    String Sites = parameters.getValue("Sites");
    if ( (Sites != null) && !Sites.isEmpty() && (Sites.indexOf("${") >= 0) ) {
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		Sites = TSCommandProcessorUtil.expandParameterValue(processor, this, Sites);
    	}
    	else {
    		propertiesUsedInParameters = true;
    	}
	}
    List<String> siteList = new ArrayList<String>();
    if ( (Sites != null) && !Sites.equals("") ) {
        if ( Sites.indexOf(",") < 0 ) {
            siteList.add(Sites.trim());
        }
        else {
            String [] siteArray = Sites.split(",");
            for ( int i = 0; i < siteArray.length; i++ ) {
                siteList.add(siteArray[i].trim());
            }
        }
    }
    String States = parameters.getValue("States");
    if ( (States != null) && !States.isEmpty() && (States.indexOf("${") >= 0) ) {
    	if ( commandPhase == CommandPhaseType.RUN) {
    		States = TSCommandProcessorUtil.expandParameterValue(processor, this, States);
    	}
    	else {
    		propertiesUsedInParameters = true;
    	}
	}
    List<String> stateList = new ArrayList<String>();
    if ( (States != null) && !States.equals("") ) {
        if ( States.indexOf(",") < 0 ) {
            stateList.add(States.trim());
        }
        else {
            String [] stateArray = States.split(",");
            for ( int i = 0; i < stateArray.length; i++ ) {
                stateList.add(stateArray[i].trim());
            }
        }
    }
    String HUCs = parameters.getValue("HUCs");
    if ( (HUCs != null) && !HUCs.isEmpty() && (HUCs.indexOf("${") >= 0) ) {
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		HUCs = TSCommandProcessorUtil.expandParameterValue(processor, this, HUCs);
    	}
    	else {
    		propertiesUsedInParameters = true;
    	}
	}
    List<String> hucList = new ArrayList<String>();
    if ( (HUCs != null) && !HUCs.equals("") ) {
        if ( HUCs.indexOf(",") < 0 ) {
            hucList.add(HUCs.trim());
        }
        else {
            String [] hucArray = HUCs.split(",");
            for ( int i = 0; i < hucArray.length; i++ ) {
                hucList.add(hucArray[i].trim());
            }
        }
    }
    String Counties = parameters.getValue("Counties");
    if ( (Counties != null) && !Counties.isEmpty() && (Counties.indexOf("${") >= 0) ) {
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		Counties = TSCommandProcessorUtil.expandParameterValue(processor, this, Counties);
    	}
    	else {
    		propertiesUsedInParameters = true;
    	}
	}
    List<String> countyList = new ArrayList<String>();
    if ( (Counties != null) && !Counties.equals("") ) {
        if ( Counties.indexOf(",") < 0 ) {
            countyList.add(Counties.trim());
        }
        else {
            String [] countyArray = Counties.split(",");
            for ( int i = 0; i < countyArray.length; i++ ) {
                countyList.add(countyArray[i].trim());
            }
        }
    }
    String Parameters = parameters.getValue("Parameters");
    if ( (Parameters != null) && !Parameters.isEmpty() && (Parameters.indexOf("${") >= 0) ) {
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		Parameters = TSCommandProcessorUtil.expandParameterValue(processor, this, Parameters);
    	}
    	else {
    		propertiesUsedInParameters = true;
    	}
	}
    List<UsgsNwisParameterType> parameterList = new ArrayList<UsgsNwisParameterType>();
    if ( (Parameters != null) && !Parameters.equals("") ) {
        if ( Parameters.indexOf(",") < 0 ) {
            parameterList.add(new UsgsNwisParameterType(Parameters.trim(), "", "", "", "", ""));
        }
        else {
            String [] parameterArray = Parameters.split(",");
            for ( int i = 0; i < parameterArray.length; i++ ) {
                parameterList.add(new UsgsNwisParameterType(parameterArray[i].trim(), "", "", "", "", ""));
            }
        }
    }
    String SiteStatus = parameters.getValue("SiteStatus");
    UsgsNwisSiteStatusType siteStatus = UsgsNwisSiteStatusType.valueOfIgnoreCase(SiteStatus);
    if ( siteStatus == null ) {
        siteStatus = UsgsNwisSiteStatusType.ALL;
    }
    String SiteTypes = parameters.getValue("SiteTypes");
    if ( (SiteTypes != null) && !SiteTypes.isEmpty() && (SiteTypes.indexOf("${") >= 0) ) {
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		SiteTypes = TSCommandProcessorUtil.expandParameterValue(processor, this, SiteTypes);
    	}
    	else {
    		propertiesUsedInParameters = true;
    	}
    }
	
    List<UsgsNwisSiteType> siteTypeList = new ArrayList<UsgsNwisSiteType>();
    if ( (SiteTypes != null) && !SiteTypes.equals("") ) {
        if ( SiteTypes.indexOf(",") < 0 ) {
            siteTypeList.add(new UsgsNwisSiteType(SiteTypes.trim(), "", ""));
        }
        else {
            String [] siteTypeArray = SiteTypes.split(",");
            for ( int i = 0; i < siteTypeArray.length; i++ ) {
                siteTypeList.add(new UsgsNwisSiteType(siteTypeArray[i].trim(), "", ""));
            }
        }
    }
    String Agency = parameters.getValue("Agency");
    if ( (Agency != null) && !Agency.isEmpty() && (Agency.indexOf("${") >= 0) ) {
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		Agency = TSCommandProcessorUtil.expandParameterValue(processor, this, Agency);
    	}
    	else {
    		propertiesUsedInParameters = true;
    	}
	}
    String Alias = parameters.getValue("Alias");
    String Format = parameters.getValue("Format");
    UsgsNwisFormatType format = UsgsNwisFormatType.valueOfIgnoreCase(Format);
    if ( format == null ) {
        format = UsgsNwisFormatType.WATERML;
    }
    String OutputFile = parameters.getValue("OutputFile");
    // The following is necessary because WaterML (1.1 at least) does not appear to have a clear indicator of
    // the time series data interval
	String WaterMLInterval = parameters.getValue("WaterMLInterval");
	TimeInterval watermlInterval = null;
    if ( (WaterMLInterval != null) && !WaterMLInterval.equals("") ) {
        try {
        	watermlInterval = TimeInterval.parseInterval(WaterMLInterval);
        }
        catch ( Exception e ) {
            // Should not happen because checked previously
        }
    }
    String WaterMLRequireDataToMatchInterval = parameters.getValue("WaterMLRequireDataToMatchInterval");
    boolean WaterRequireDataToMatchInterval_boolean = true; // default
    if ( (WaterMLRequireDataToMatchInterval != null) && WaterMLRequireDataToMatchInterval.equalsIgnoreCase(_False) ) {
        WaterRequireDataToMatchInterval_boolean = false;
    }
	String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		if ( commandPhase == CommandPhaseType.RUN ) {
			InputStart = "${InputStart}"; // Global default
		}
		else {
			propertiesUsedInParameters = true;
		}
	}
	String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		if ( commandPhase == CommandPhaseType.RUN ) {
			InputEnd = "${InputEnd}"; // Global default
		}
		else {
			propertiesUsedInParameters = true;
		}
	}
    
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

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Now try to read...

	List<TS> tslist = new ArrayList<TS>();	// List for time series results.
					// Will be added to for one time series
					// read or replaced if a list is read.
	try {
		// Find the data store to use...
		DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
		    dataStoreName, UsgsNwisInstantaneousDataStore.class );
		if ( dataStore == null ) {
			message = "Could not get data store for name \"" + dataStoreName + "\" to query data.";
			Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the USGS NWIS instantaneous web service has been configured with name \"" +
                    dataStoreName + "\" and is available." ) );
			throw new Exception ( message );
		}
		UsgsNwisInstantaneousDataStore usgsNwisInstantaneousDataStore = (UsgsNwisInstantaneousDataStore)dataStore;

		String OutputFile_full = OutputFile;
		if ( (OutputFile != null) && !OutputFile.equals("") ) {
		    OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
		}
		if ( !propertiesUsedInParameters ) {
	        tslist = usgsNwisInstantaneousDataStore.readTimeSeriesList ( siteList, stateList,
	            hucList, __boundingBox, countyList,
	            parameterList,
	            siteStatus, siteTypeList, Agency,
	            format, OutputFile_full,
	            InputStart_DateTime, InputEnd_DateTime,
	            watermlInterval, WaterRequireDataToMatchInterval_boolean, readData );
		}
		// Make sure that size is set...
		int size = 0;
		if ( tslist != null ) {
			size = tslist.size();
		}
	
   		if ( ((tslist == null) || (size == 0)) && !propertiesUsedInParameters ) {
			Message.printStatus ( 2, routine,"No USGS NWIS instantaneous time series were found." );
	        // Warn if nothing was retrieved (can be overridden to ignore).
            message = "No time series were read from the USGS NWIS instantaneous value web service.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database." +
                    	"  Previous messages may provide more information." ) );
   		}
   		else {
			// Else, further process each time series...
			for ( TS ts: tslist ) {
			    // Set the alias to the desired string - this is impacted by the Location parameter
                String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                    processor, ts, Alias, status, commandPhase);
                ts.setAlias ( alias );
			}
			
            // Save the output file name...
			if ( (OutputFile != null) && !OutputFile.equals("") ) {
			    setOutputFile ( new File(OutputFile_full));
			}
		}
    
        Message.printStatus ( 2, routine, "Read " + size + " USGS NWIS instantaneous time series." );

        if ( commandPhase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing USGS NWIS instantaneous time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
                    // Don't throw an exception - probably due to missing data.
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding USGS NWIS instantaneous time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
            }
        }
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            setDiscoveryTSList ( tslist );
        }
        // Warn if nothing was retrieved (can be overridden to ignore).
        if ( ((tslist == null) || (size == 0)) && !propertiesUsedInParameters ) {
            message = "No time series were read from the USGS NWIS instantaneous value web service.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database.  See previous messages." ) );
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error reading time series from USGS NWIS instantaneous web service (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
               message, "Report the problem to software support - also see the log file." ) );
		throw new CommandException ( message );
	}

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
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
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file )
{
    __OutputFile_File = file;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	StringBuffer b = new StringBuffer ();
	if ( props == null ) {
	    return getCommandName() + "()";
	}
    String DataStore = props.getValue("DataStore");
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
    String Sites = props.getValue("Sites");
    if ( (Sites != null) && (Sites.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Sites=\"" + Sites + "\"" );
    }
	String States = props.getValue("States");
	if ( (States != null) && (States.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "States=\"" + States + "\"" );
	}
    String HUCs = props.getValue("HUCs");
    if ( (HUCs != null) && (HUCs.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "HUCs=\"" + HUCs + "\"" );
    }
    String BoundingBox = props.getValue("BoundingBox");
    if ( (BoundingBox != null) && (BoundingBox.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "BoundingBox=\"" + BoundingBox + "\"" );
    }
    String Counties = props.getValue("Counties");
    if ( (Counties != null) && (Counties.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Counties=\"" + Counties + "\"" );
    }
    String Parameters = props.getValue("Parameters");
    if ( (Parameters != null) && (Parameters.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Parameters=\"" + Parameters + "\"" );
    }
    String SiteStatus = props.getValue("SiteStatus");
    if ( (SiteStatus != null) && (SiteStatus.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SiteStatus=\"" + SiteStatus + "\"" );
    }
    String SiteTypes = props.getValue("SiteTypes");
    if ( (SiteTypes != null) && (SiteTypes.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SiteTypes=\"" + SiteTypes + "\"" );
    }
    String Agency = props.getValue("Agency");
    if ( (Agency != null) && (Agency.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Agency=\"" + Agency + "\"" );
    }
	String InputStart = props.getValue("InputStart");
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputStart=\"" + InputStart + "\"" );
	}
	String InputEnd = props.getValue("InputEnd");
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputEnd=\"" + InputEnd + "\"" );
	}
    String Alias = props.getValue("Alias");
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }
    String Format = props.getValue("Format");
    if ( (Format != null) && (Format.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Format=\"" + Format + "\"" );
    }
    String OutputFile = props.getValue("OutputFile");
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"" );
    }
    String WaterMLInterval = props.getValue("WaterMLInterval");
    if ((WaterMLInterval != null) && (WaterMLInterval.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("WaterMLInterval=\"" + WaterMLInterval + "\"");
    }
	String WaterMLRequireDataToMatchInterval = props.getValue("WaterMLRequireDataToMatchInterval");
    if ((WaterMLRequireDataToMatchInterval != null) && (WaterMLRequireDataToMatchInterval.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("WaterMLRequireDataToMatchInterval=" + WaterMLRequireDataToMatchInterval );
    }

    return getCommandName() + "(" + b.toString() + ")";
}

}