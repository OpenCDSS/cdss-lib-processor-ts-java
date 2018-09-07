package cdss.dmi.hydrobase.rest.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.Util.GUI.InputFilter_JPanel;
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
import RTi.Util.IO.MissingObjectEvent;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.InvalidTimeIntervalException;
import RTi.Util.Time.TimeInterval;
import cdss.dmi.hydrobase.rest.ColoradoHydroBaseRestDataStore;
import cdss.dmi.hydrobase.rest.dao.DiversionWaterClass;
import cdss.dmi.hydrobase.rest.dao.Station;
import cdss.dmi.hydrobase.rest.dao.TelemetryStationDataTypes;
import cdss.dmi.hydrobase.rest.dao.WaterLevelsWell;
import cdss.dmi.hydrobase.rest.ui.ColoradoHydroBaseRest_Station_InputFilter_JPanel;
import cdss.dmi.hydrobase.rest.ui.ColoradoHydroBaseRest_Structure_InputFilter_JPanel;
import cdss.dmi.hydrobase.rest.ui.ColoradoHydroBaseRest_TelemetryStation_InputFilter_JPanel;
import cdss.dmi.hydrobase.rest.ui.ColoradoHydroBaseRest_Well_InputFilter_JPanel;

/**
This class initializes, checks, and runs the ReadHydroBase() command.
*/
public class ReadColoradoHydroBaseRest_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Number of where clauses shown in the editor and available as parameters.
*/
private int __numWhere = 6;

/**
Data values for boolean parameters.
*/
protected String _False = "False";
protected String _True = "True";

/**
Data values for IfMissing parameter.
*/
protected String _Ignore = "Ignore";
protected String _Warn = "Warn";

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public ReadColoradoHydroBaseRest_Command ()
{	super();
	setCommandName ( "ReadColoradoHydroBaseRest" );
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

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    String DataStore = parameters.getValue ( "DataStore" );
    String TSID = parameters.getValue ( "TSID" );
    String InputStart = parameters.getValue ( "InputStart" );
    String InputEnd = parameters.getValue ( "InputEnd" );
    String InputFiltersCheck = parameters.getValue ( "InputFiltersCheck" ); // Passed in from the editor, not an actual parameter
    
	if ( (DataStore == null) || DataStore.isEmpty() ) {
        message = "The datastore must be specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the datastore." ) );
	}

	if ( (TSID != null) && !TSID.equals("") ) {
	    // Check the parts of the TSID...
		TSIdent tsident = null;
		try {
            tsident = new TSIdent ( TSID );
			String Location = tsident.getLocation();
			String DataType = tsident.getType();
			String Interval = tsident.getInterval();
			if ( Location.length() == 0 ) {
                message = "The location part of the TSID must be specified.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a location for the time series identifier." ) );
			}
			if ( DataType.length() == 0 ) {
                message = "The data type must be specified.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a data type for the time series identifier." ) );
			}
			if ( Interval.length() == 0 ) {
                message = "The TSID interval must be specified.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an interval for the time series identifier." ) );
			}
			else {
                // TODO SAM 2006-04-25
				// Most likely the following will not be executed because parsing the TSID will generate an
				// InvalidTimeIntervalException (caught below).  This may cause the user to
				// do two checks to catch all input errors.
				try { TimeInterval.parseInterval (Interval);
				}
				catch ( Exception e ) {
                    message = "The TSID data interval \"" + Interval + "\" is invalid";
					warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid data interval." ) );
				}
			}
		}
		catch ( InvalidTimeIntervalException e ) {
			// Will not even be able to print it...
            message = "The data interval in the time series identifier \"" + TSID + "\" is invalid";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid data interval." ) );
		}
		catch ( Exception e ) {
            message = "Unable to parse TSID \"" + TSID + "\" to check its parts.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify the time series identifier." ) );
		}
	}
	if ( (TSID == null) || TSID.equals("") ) {
    	String DataType = parameters.getValue ( "DataType" );
    	if ( (DataType == null) || (DataType.length() == 0) ) {
            message = "The data type must be specified.";
    		warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the data type." ) );
    	}
    	String Interval = parameters.getValue ( "Interval" );
    	if ( (Interval == null) || (Interval.length() == 0) ) {
            message = "The data interval must be specified.";
    		warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the data interval." ) );
    	}
    	else {
    	    try { TimeInterval.parseInterval (Interval);
    		}
    		catch ( Exception e ) {
                message = "The data interval \"" + Interval + "\" is invalid";
    			warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid data interval." ) );
    		}
    	}
	}
	// TODO SAM 2006-04-24 Need to check the WhereN parameters.

	if ( (InputStart != null) && !InputStart.equals("") &&
		!InputStart.equalsIgnoreCase("InputStart") &&
		!InputStart.equalsIgnoreCase("InputEnd") && (InputStart.indexOf("${") < 0)) {
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
	if ( (InputEnd != null) && !InputEnd.equals("") &&
		!InputEnd.equalsIgnoreCase("InputStart") &&
		!InputEnd.equalsIgnoreCase("InputEnd") && (InputEnd.indexOf("${") < 0)) {
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

	/*
	String FillUsingDivComments = parameters.getValue ( "FillUsingDivComments" );
	if ((FillUsingDivComments != null) && !FillUsingDivComments.equals("")){
		if (	!FillUsingDivComments.equalsIgnoreCase(_True) &&
			!FillUsingDivComments.equalsIgnoreCase(_False) ) {
            message = "The FillUsingDivComments parameter \"" + FillUsingDivComments +
            "\" must be True (blank) or False";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify False or True, or blank for default of False." ) );
		}
	}
	String FillUsingDivCommentsFlag = parameters.getValue ( "FillUsingDivCommentsFlag" );
	if ( (FillUsingDivCommentsFlag != null) && (FillUsingDivCommentsFlag.length() != 1) ) {
        message = "The FillUsingDivCommentsFlag must be 1 character long.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                  new CommandLogRecord(CommandStatusType.FAILURE,
                          message, "Specify a 1-character flag, or blank to not use flag." ) );
	}
	*/
	String IfMissing = parameters.getValue ( "IfMissing" );
    if ( (IfMissing != null) && !IfMissing.equals("") &&
        !IfMissing.equalsIgnoreCase(_Warn) && !IfMissing.equalsIgnoreCase(_Ignore) ) {
        message = "The IfMissing parameter \"" + IfMissing +
        "\" must be blank, " + _Ignore + ", or " + _Warn;
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify " + _Ignore + ", " + _Warn +
                        ", or blank for default of " + _Warn + "." ) );
    }
    
    // If any issues were detected in the input filter add to the message string
    if ( (InputFiltersCheck != null) && !InputFiltersCheck.isEmpty() ) {
    	warning += InputFiltersCheck;
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>();
    validList.add ( "DataStore" );
    validList.add ( "Alias" );
    validList.add ( "TSID" );
    validList.add ( "DataType" );
    validList.add ( "Interval" );
    int numFilters = 25; // Make a big number so all are allowed
    for ( int i = 1; i <= numFilters; i++ ) { 
        validList.add ( "Where" + i );
    }
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "FillUsingDivComments" );
    validList.add ( "FillUsingDivCommentsFlag" );
    validList.add ( "IfMissing" );
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
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
@SuppressWarnings("rawtypes")
public List getObjectList ( Class c )
{
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Also check the base class
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
    }
    else {
        return null;
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
	return (new ReadColoradoHydroBaseRest_JDialog ( parent, this )).ok();
}

// parseCommand is in parent class

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
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
	TSCommandProcessor tsprocessor = (TSCommandProcessor)processor;
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    
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
    
    boolean readData = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
        readData = false;
    }
    
	String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}";
	}
    String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}";
	}
	String FillUsingDivComments = parameters.getValue ("FillUsingDivComments" );
	if ( (FillUsingDivComments == null) ||FillUsingDivComments.equals("") ) {
		FillUsingDivComments = _False;	// Default is NOT to fill
	}
	String FillUsingDivCommentsFlag = parameters.getValue ( "FillUsingDivCommentsFlag" );
	
    String IfMissing = parameters.getValue ("IfMissing" );
    boolean IfMissingWarn = true;  // Default
    if ( (IfMissing != null) && IfMissing.equalsIgnoreCase(_Ignore) ) {
        IfMissingWarn = false;  // Ignore when time series are not found
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

	// Set up properties for the read...

	PropList HydroBase_props = new PropList ( "HydroBase" );
	/* TODO SAM 2006-04-27 Code cleanup
	As per Ray Bennett always do this.
	HydroBase_props.set ( "FillDailyDiv=" + FillDailyDiv );
	if ( (FillDailyDivFlag != null) && !FillDailyDivFlag.equals("") ) {
		HydroBase_props.set ( "FillDailyDivFlag=" + FillDailyDivFlag );
	}
	*/
	HydroBase_props.set ( "FillUsingDivComments=" + FillUsingDivComments );
	if ( (FillUsingDivCommentsFlag != null) &&!FillUsingDivCommentsFlag.equals("")){
		HydroBase_props.set ( "FillUsingDivCommentsFlag=" + FillUsingDivCommentsFlag );
	}

	// Now try to read...

	List<TS> tslist = new Vector<TS>();	// List for time series results.
					// Will be added to for one time series
					// read or replaced if a list is read.
	try {
        String Alias = parameters.getValue ( "Alias" );
        String TSID = parameters.getValue ( "TSID" );
        String DataStore = parameters.getValue ( "DataStore" );
        ColoradoHydroBaseRestDataStore dataStore = null;
		if ( (DataStore != null) && !DataStore.equals("") ) {
		    // User has indicated that a datastore should be used...
		    DataStore dataStore0 = ((TSCommandProcessor)getCommandProcessor()).getDataStoreForName( DataStore, ColoradoHydroBaseRestDataStore.class );
	        if ( dataStore0 != null ) {
	            Message.printStatus(2, routine, "Selected data store is \"" + dataStore0.getName() + "\"." );
				dataStore = (ColoradoHydroBaseRestDataStore)dataStore0;
	        }
	    }
		if ( dataStore == null ) {
            message = "Cannot get ColoradoHydroBaseRestDataStore for \"" + DataStore + "\".";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a ColoradoHydroBaseRestDataStore datastore is properly configured." ) );
            throw new RuntimeException ( message );
        }
        else { 
	        if ( (TSID != null) && !TSID.equals("") ) {
				// Version that reads a single time series using the TSID
				Message.printStatus ( 2, routine,"Reading HydroBase REST web service time series \"" + TSID + "\"" );
				TS ts = null;
				try {
	                ts = dataStore.readTimeSeries ( TSID, InputStart_DateTime, InputEnd_DateTime, readData );
				}
				catch ( Exception e ) {
				    ts = null;
					message = "Unexpected error reading HydroBase REST time series \"" + TSID + "\" (" + e + ").";
					Message.printWarning ( 2, routine, message );
					Message.printWarning ( 2, routine, e );
					if ( IfMissingWarn ) {
	                    status.addToLog ( commandPhase,
	                        new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Verify the time series identifier." ) );
	                   throw new RuntimeException ( message );
					}
					else {
					    // Just show for info purposes
	                    status.addToLog ( commandPhase,
	                        new CommandLogRecord(CommandStatusType.INFO,
	                            message, "Verify the time series identifier." ) );
					}
				}
				finally {
				    if ( ts == null ) {
				        // Generate an event for listeners
				        notifyCommandProcessorEventListeners(
					        new MissingObjectEvent(TSID,Class.forName("RTi.TS.TS"),"Time Series", this));
				    }
				}
				if ( ts != null ) {
					// Set the alias...
				    if ( Alias != null ) {
				        ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString(
			                    processor, ts, Alias, status, commandPhase) );
				    }
					tslist.add ( ts );
				}
	        }
			else {
	            // Read 1+ time series using the input filters
				// Get the input needed to process the file...
				String DataType = parameters.getValue ( "DataType" );
				String Interval = parameters.getValue ( "Interval" );
				String InputName = parameters.getValue ( "InputName" );
				if ( InputName == null ) {
					InputName = "";
				}
				List<String> whereNList = new ArrayList<String>();
				String WhereN;
				int nfg = 0; // Used below.
				// User may have skipped where and left a blank so loop over a sufficiently large number of where parameters
				for ( int ifg = 0; ifg < 25; ifg++ ) {
					WhereN = parameters.getValue ( "Where" + (ifg + 1) );
					if ( WhereN != null ) {
						++nfg;
						whereNList.add ( WhereN );
					}
				}
	
				// Initialize an input filter based on the data type...
	
				InputFilter_JPanel filterPanel = null;
				boolean isStation = false;
				boolean isStructure = false;
				boolean isWaterClass = false;
				boolean isTelemetryStation = false;
				boolean isWell = false;
	
				// Create the input filter panel...
				// Data type is the part after the dash (e.g., "Structure - DivTotal")
				String dataType = "";
			    if ( dataType.indexOf("-") > 0 ) {
			        dataType = StringUtil.getToken(DataType,"-",0,1).trim();
			    }
			    else {
			        dataType = DataType.trim();
			    }
	
				if ( dataStore.isStationTimeSeriesDataType ( dataType ) ){
					// Stations...
					isStation = true;
					filterPanel = new ColoradoHydroBaseRest_Station_InputFilter_JPanel ( dataStore );
					Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for station." );
				}
				else if ( dataStore.isStructureTimeSeriesDataType ( dataType ) ){
					// Structures...
					isStructure = true;
					boolean includeSFUT = true;
					filterPanel = new ColoradoHydroBaseRest_Structure_InputFilter_JPanel ( dataStore, includeSFUT );
					Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for structure." );
					if( dataStore.isWaterClassStructure(dataType)){
						isWaterClass = true;
					}
				}
				else if ( dataStore.isTelemetryStationTimeSeriesDataType ( dataType ) ){
					// Telemetry stations...
					isTelemetryStation = true;
					filterPanel = new ColoradoHydroBaseRest_TelemetryStation_InputFilter_JPanel ( dataStore );
					Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for telemetry station." );
				}
				else if ( dataStore.isWellTimeSeriesDataType ( dataType ) ){
					// Wells...
					isWell = true;
					filterPanel = new ColoradoHydroBaseRest_Well_InputFilter_JPanel ( dataStore );
					Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for well." );
				}
				else {
	                message = "Data type \"" + dataType + "\" is not recognized as a ColoradoHydroBaseRest web services data type.";
					Message.printWarning ( 2, routine, message );
	                status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Verify that the datatype can be used with ColoradoHydroBaseRest web services (see documentation)." ) );
					throw new Exception ( message );
				}
	
				// Populate with the where information from the command...
	
				String filter_delim = ";";
				for ( int ifg = 0; ifg < nfg; ifg ++ ) {
					WhereN = whereNList.get(ifg);
	                if ( WhereN.length() == 0 ) {
	                    continue;
	                }
					// Set the filter...
					try {
	                    filterPanel.setInputFilter( ifg, WhereN, filter_delim );
					}
					catch ( Exception e ) {
	                    message = "Error setting where information using \""+WhereN+"\"";
						Message.printWarning ( 2, routine,message);
						Message.printWarning ( 3, routine, e );
						++warning_count;
	                    status.addToLog ( commandPhase,
	                        new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Report the problem to software support - also see the log file." ) );
					}
				}
			
				// Read the list of objects from which identifiers can be obtained.  This code is similar to that in
				// TSTool_JFrame.readHydroBaseHeaders...
			
				Message.printStatus ( 2, routine, "Getting the list of time series..." );
			
				// Create empty lists for catalogs from each major data category
				List<Station> stationCatalog = new ArrayList<Station>();
				List<DiversionWaterClass> structureCatalog = new ArrayList<DiversionWaterClass>();
				List<TelemetryStationDataTypes> telemetryStationCatalog = new ArrayList<TelemetryStationDataTypes>();
				List<WaterLevelsWell> wellCatalog = new ArrayList<WaterLevelsWell>();
				
				// Read the catalog
				int size = 0;
				if ( isStation ) {
					try {
						stationCatalog = dataStore.getStationTimeSeriesCatalog ( dataType, Interval, (ColoradoHydroBaseRest_Station_InputFilter_JPanel)filterPanel );
						size = stationCatalog.size();
					}
					catch ( Exception e ) {
						// Probably no data
					}
				}
				if ( isStructure ) {
					try {
						structureCatalog = dataStore.getWaterClassesTimeSeriesCatalog ( dataType, Interval, (ColoradoHydroBaseRest_Structure_InputFilter_JPanel)filterPanel );
						size = structureCatalog.size();
					}
					catch ( Exception e ) {
						// Probably no data
					}
				}
				if ( isTelemetryStation ) {
					try {
						telemetryStationCatalog = dataStore.getTelemetryStationTimeSeriesCatalog ( dataType, Interval, (ColoradoHydroBaseRest_TelemetryStation_InputFilter_JPanel)filterPanel );
						size = telemetryStationCatalog.size();
					}
					catch ( Exception e ) {
						// Probably no data
					}
				}
				if ( isWell ) {
					try {
						wellCatalog = dataStore.getWellTimeSeriesCatalog ( dataType, Interval, (ColoradoHydroBaseRest_Well_InputFilter_JPanel)filterPanel );
						size = wellCatalog.size();
					}
					catch ( Exception e ) {
						// Probably no data
					}
				}
				
				// Make sure that size is set...
	       		if ( size == 0 ) {
					Message.printStatus ( 2, routine,"No HydroBase REST web service time series were found." );
			        // Warn if nothing was retrieved (can be overridden to ignore).
		            if ( IfMissingWarn ) {
		                message = "No time series were read from HydroBase REST web service.";
		                Message.printWarning ( warning_level, 
		                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	                    status.addToLog ( commandPhase,
	                        new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Data may not be in database." +
	                            	"  Previous messages may provide more information." ) );
		            }
		            else {
		                // Ignore the problem.  Call it a success if no other problems occurred.
		                status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
		            }
		            // Generate an event for listeners
		            // FIXME SAM 2008-08-20 Need to put together a more readable id for reporting
	                notifyCommandProcessorEventListeners(
	                    new MissingObjectEvent(DataType + ", " + Interval + ", see command for user-specified criteria",
	                        Class.forName("RTi.TS.TS"),"Time Series", this));
					return;
	       		}
			
				// Else, convert each header object to a TSID string and read the time series...
	
				Message.printStatus ( 2, "", "Reading " + size + " time series..." );
	
				String tsidentString = null; // TSIdent string
				TS ts; // Time series to read.
		        Station sta;
	            DiversionWaterClass str;
	            TelemetryStationDataTypes tsta;
	            WaterLevelsWell well;
				String inputName = "";	// Input name to add to identifiers, if it has been requested (this includes the tilde character).
				if ( (DataStore != null) && !DataStore.equals("") ) {
				    // Use the datastore for the time series input name (will not include "HydroBase" since
				    // datastore has the connection information)
				    inputName = "~" + DataStore;
				}
				for ( int i = 0; i < size; i++ ) {
					// Check to see if reading time series should be canceled because the command has been canceled.
					if ( tsprocessor.getCancelProcessingRequested() ) {
						// The user has requested that command processing should be canceled.
						// Check here in this command because a very large query could take a long time before a single command finishes.
						Message.printStatus(2, routine, "Cancel processing based on user request.");
						break;
					}
					// List in order of likelihood to improve performance...
					tsidentString = null; // Do this in case there is no active match
					// TODO smalers 2019-06-30 the follow is redundant with the table model getTimeSeriesIdentifier()
					// - need to figure out if code can can be moved to datastore
					if ( isStation ) {
						// Station time series...
						//sta = stationCatalog.get(i);
						//tsidentString = sta.getStation_id()
						//	+ "." + sta.getData_source()
						//	+ "." + DataType
						//	+ "." + Interval
						//	+ inputName;
					}
					else if ( isStructure ) {
						// Structure time series, but time series list uses generalized water classes
						str = (DiversionWaterClass)structureCatalog.get(i);
						if( isWaterClass){
							String wcIdentifier = str.getWcIdentifier();
							tsidentString = str.getWdid()
								+ "." + "DWR"
								+ "." + DataType 
								+ "-" + wcIdentifier
								+ "." + Interval
								+ inputName;
						}else{
							tsidentString = str.getWdid()
								+ "." + "DWR"
								+ "." + DataType 
								+ "." + Interval
								+ inputName;
						}
						
					}
					else if ( isTelemetryStation ) {
						// Telemetry station time series...
						tsta = (TelemetryStationDataTypes)telemetryStationCatalog.get(i);
						tsidentString = "abbrev:"+tsta.getAbbrev()
							+ "." + tsta.getDataSourceAbbrev()
							+ "." + DataType
							+ "." + Interval
							+ inputName;
					}
	                else if ( isWell ) {
	                    // Well time series...
	                    well = (WaterLevelsWell)wellCatalog.get(i);
	                    tsidentString = "wellid:" + well.getWellId() 
	                        + "." + well.getDataSource() 
	                        + "." + DataType
	                        + "." + Interval
	                        + inputName;
	                }
		            // Update the progress
					message = "Reading HydroBase REST web service time series " + (i + 1) + " of " + size + " \"" + tsidentString + "\"";
	                notifyCommandProgressListeners ( i, size, (float)-1.0, message );
					try {
					    ts = dataStore.readTimeSeries (
							tsidentString,
							InputStart_DateTime,
							InputEnd_DateTime, readData );
						// Add the time series to the temporary list.  It will be further processed below...
		                if ( (ts != null) && (Alias != null) && !Alias.equals("") ) {
		                    ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString(
		                        processor, ts, Alias, status, commandPhase) );
		                }
		                // Allow null to be added here.
						tslist.add ( ts );
					}
					catch ( Exception e ) {
						message = "Unexpected error reading HydroBase REST web service time series \"" + tsidentString + "\" (" + e + ").";
						Message.printWarning ( 2, routine, message );
						Message.printWarning ( 2, routine, e );
						++warning_count;
	                    status.addToLog ( commandPhase,
	                        new CommandLogRecord(CommandStatusType.FAILURE,
	                           message, "Report the problem to software support - also see the log file." ) );
					}
				}
			}
		}
    
        int size = 0;
        if ( tslist != null ) {
            size = tslist.size();
        }
        Message.printStatus ( 2, routine, "Read " + size + " HydroBase REST web service time series." );

        if ( commandPhase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing HydroBase REST web service time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding HydroBase REST web service time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
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
        if ( (tslist == null) || (size == 0) ) {
            if ( IfMissingWarn ) {
                message = "No time series were read from HydroBase REST web service.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Data may not be in database.  See previous messages." ) );
            }
            // Generate an event for listeners
            // TOD SAM 2008-08-20 Evaluate whether need here
            //notifyCommandProcessorEventListeners(new MissingObjectEvent(DataType + ", " + Interval + filter_panel,this));
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error reading time series from HydroBase REST web service (" + e + ").";
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
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props )
{   if ( props == null ) {
        return getCommandName() + "()";
    }
	StringBuffer b = new StringBuffer ();
	String Alias = props.getValue("Alias"); // Alias added at end
    String DataStore = props.getValue("DataStore");
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
	String TSID = props.getValue("TSID");
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (TSID == null) || TSID.equals("") ) {
	    // The following need to be explicitly specified when not using the TSID
        String DataType = props.getValue("DataType");
    	if ( (DataType != null) && (DataType.length() > 0) ) {
    		if ( b.length() > 0 ) {
    			b.append ( "," );
    		}
    		b.append ( "DataType=\"" + DataType + "\"" );
    	}
    	String Interval = props.getValue("Interval");
    	if ( (Interval != null) && (Interval.length() > 0) ) {
    		if ( b.length() > 0 ) {
    			b.append ( "," );
    		}
    		b.append ( "Interval=\"" + Interval + "\"" );
    	}
	}
	String delim = ";";
    for ( int i = 1; i <= __numWhere; i++ ) {
    	String where = props.getValue("Where" + i);
    	if ( (where != null) && (where.length() > 0) && !where.startsWith(delim) ) {
    		if ( b.length() > 0 ) {
    			b.append ( "," );
    		}
    		b.append ( "Where" + i + "=\"" + where + "\"" );
    	}
    }
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
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
	String FillUsingDivComments = props.getValue("FillUsingDivComments");
	if ( (FillUsingDivComments != null) && (FillUsingDivComments.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillUsingDivComments=" + FillUsingDivComments );
	}
	String FillUsingDivCommentsFlag = props.getValue("FillUsingDivCommentsFlag");
	if ( (FillUsingDivCommentsFlag != null) && (FillUsingDivCommentsFlag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillUsingDivCommentsFlag=\"" + FillUsingDivCommentsFlag + "\"" );
	}
    String IfMissing = props.getValue("IfMissing");
    if ( (IfMissing != null) && (IfMissing.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IfMissing=" + IfMissing );
    }
    return getCommandName() + "("+ b.toString()+")";
}

}