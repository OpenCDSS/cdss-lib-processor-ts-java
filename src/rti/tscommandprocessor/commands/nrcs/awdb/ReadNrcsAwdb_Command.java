// ReadNrcsAwdb_Command - This class initializes, checks, and runs the ReadNrcsAwdb() command.

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

package rti.tscommandprocessor.commands.nrcs.awdb;

import gov.usda.nrcs.wcc.ns.awdbwebservice.Element;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
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
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.InvalidTimeIntervalException;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the ReadNrcsAwdb() command.
*/
public class ReadNrcsAwdb_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Possible values for ReadForecast.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
The table that is created in discovery mode (for forecast data).
*/
private DataTable __discoveryTable = null;

/**
List of time series read during discovery.  These are TS objects but with mainly the metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Bounding box coordinate WestLon, SouthLat, EastLon, NorthLat
*/
double [] __boundingBox = null; // Use null to indicate no bounding box specified

/**
Constructor.
*/
public ReadNrcsAwdb_Command ()
{	super();
	setCommandName ( "ReadNrcsAwdb" );
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
    String Interval = parameters.getValue ( "Interval" );
    String Stations = parameters.getValue ( "Stations" );
    String States = parameters.getValue ( "States" );
    String Networks = parameters.getValue ( "Networks" );
    String HUCs = parameters.getValue ( "HUCs" );
    String BoundingBox = parameters.getValue ( "BoundingBox" );
    String Counties = parameters.getValue ( "Counties" );
    String ReadForecast = parameters.getValue ( "ReadForecast" );
    String ForecastPeriod = parameters.getValue ( "ForecastPeriod" );
    String ElevationMin = parameters.getValue ( "ElevationMin" );
    String ElevationMax = parameters.getValue ( "ElevationMax" );
    String InputStart = parameters.getValue ( "InputStart" );
    String InputEnd = parameters.getValue ( "InputEnd" );

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (DataStore == null) || DataStore.equals("") ) {
        message = "The data store must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store." ) );
    }
    
    if ( Interval == null || (Interval.length() == 0) ) {
        message = "The data interval must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a data interval."));
    }
    else {
        try {
            TimeInterval.parseInterval(Interval);
        }
        catch ( Exception e ) {
            // Should not happen because choices are valid
            message = "The data interval \"" + Interval + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a data interval using the command editor."));
        }
    }

    __boundingBox = null;
    if ( (BoundingBox != null) && !BoundingBox.equals("") ) {
        // Make sure that 4 numbers are specified
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
        if ( (States == null) || States.equals("") ) {
            message = "The county can only be specified if state is also specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid state abbreviation when specifying county." ) );
        }
    }

    // Make sure some filter is specified.  Otherwise all the data in the NRCS database will be read, which
    // will be extremely slow
    
    if ( ((Stations == null) || Stations.equals("")) &&
        ((States == null) || States.equals("")) &&
        ((Networks == null) || Networks.equals("")) &&
        ((HUCs == null) || HUCs.equals("")) &&
        ((BoundingBox == null) || BoundingBox.equals("")) &&
        ((Counties == null) || Counties.equals("")) ) {
        message = "At least one location constraint must be specified (otherwise query is very large and slow).";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify at least one location constraint." ) );
    }
    
    if ( (ElevationMin != null) && !StringUtil.isDouble(ElevationMin) ) {
        message = "The elevation minimum (" + ElevationMin + ") is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the elevation minimum as a number." ) );
    }
    
    if ( (ElevationMax != null) && !StringUtil.isDouble(ElevationMax) ) {
        message = "The elevation maximum (" + ElevationMax + ") is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the elevation maximum as a number." ) );
    }
    
    if ( (ReadForecast != null) && ReadForecast.equalsIgnoreCase(_True) ) {
        // Must specify a ForecastPeriod
        if ( (ForecastPeriod == null) || ForecastPeriod.equalsIgnoreCase("") ) {
            message = "The forecast period must be specified when forecasts are being read.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the forecast period." ) );
        }
    }

	if ( (InputStart != null) && !InputStart.equals("") &&
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
	if ( (InputEnd != null) && !InputEnd.equals("") &&
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
	
    // Check for invalid parameters...
    List<String> validList = new ArrayList<>(21);
    validList.add ( "DataStore" );
    validList.add ( "Interval" );
    validList.add ( "Stations" );
    validList.add ( "Networks" );
    validList.add ( "States" );
    validList.add ( "HUCs" );
    validList.add ( "BoundingBox" );
    validList.add ( "Counties" );
    validList.add ( "ReadForecast" );
    validList.add ( "ForecastTableID" );
    validList.add ( "ForecastPeriod" );
    validList.add ( "ForecastPublicationDateStart" );
    validList.add ( "ForecastPublicationDateEnd" );
    validList.add ( "ForecastExceedanceProbabilities" );
    validList.add ( "Elements" );
    validList.add ( "ElevationMin" );
    validList.add ( "ElevationMax" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "TimeZoneMap" );
    validList.add ( "Alias" );
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
    return (new ReadNrcsAwdb_JDialog ( parent, this )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __discoveryTable;
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
The following classes can be requested:  DataTable, TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
	List<TS> discoveryTSList = getDiscoveryTSList ();
    DataTable table = getDiscoveryTable();
    if ( (table != null) && (c == table.getClass()) ) {
        // Asking for tables
        List<T> list = null;
        if ( table != null ) {
            list = new ArrayList<>();
            list.add ( (T)table );
        }
        return list;
    }
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discoveryTSList.get(0);
    // Also check the base class
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discoveryTSList;
    }
    else {
        return null;
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
{	String routine = getClass().getSimpleName() + ".runCommand", message;
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
    
    boolean readData = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable(null);
        setDiscoveryTSList ( null );
        readData = false;
    }
    
    String dataStoreName = parameters.getValue("DataStore");
    String Stations = parameters.getValue("Stations");
    List<String> stationList = new ArrayList<>();
    if ( (Stations != null) && !Stations.equals("") ) {
        // Station list is allowed to use a processor property
        Stations = TSCommandProcessorUtil.expandParameterValue(processor,this,Stations);
        if ( Stations.indexOf(",") < 0 ) {
            stationList.add(Stations.trim());
        }
        else {
            String [] siteArray = Stations.split(",");
            for ( int i = 0; i < siteArray.length; i++ ) {
                stationList.add(siteArray[i].trim());
            }
        }
    }
    String States = parameters.getValue("States");
    List<String> stateList = new ArrayList<>();
    if ( (States != null) && !States.equals("") ) {
        // State list is allowed to use a processor property
        States = TSCommandProcessorUtil.expandParameterValue(processor,this,States);
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
    String Networks = parameters.getValue("Networks");
    List<NrcsAwdbNetworkCode> networkList = new ArrayList<>();
    if ( (Networks != null) && !Networks.equals("") ) {
        // Network list is allowed to use a processor property
        Networks = TSCommandProcessorUtil.expandParameterValue(processor,this,Networks);
        if ( Networks.indexOf(",") < 0 ) {
            networkList.add(new NrcsAwdbNetworkCode(Networks, ""));
        }
        else {
            String [] networkArray = Networks.split(",");
            for ( int i = 0; i < networkArray.length; i++ ) {
                networkList.add(new NrcsAwdbNetworkCode(networkArray[i].trim(),""));
            }
        }
    }
    String HUCs = parameters.getValue("HUCs");
    List<String> hucList = new ArrayList<>();
    if ( (HUCs != null) && !HUCs.equals("") ) {
        // HUC list is allowed to use a processor property
        HUCs = TSCommandProcessorUtil.expandParameterValue(processor,this,HUCs);
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
    List<String> countyList = new ArrayList<>();
    if ( (Counties != null) && !Counties.equals("") ) {
        // County list is allowed to use a processor property
        Counties = TSCommandProcessorUtil.expandParameterValue(processor,this,Counties);
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
    String ReadForecast = parameters.getValue("ReadForecast");
    boolean readForecast = false;
    if ( (ReadForecast != null) && ReadForecast.equalsIgnoreCase(_True) ) {
        readForecast = true;
    }
    String ForecastTableID = parameters.getValue("ForecastTableID");
    if ( (ForecastTableID == null) || ForecastTableID.equals("") ) {
        ForecastTableID = "NRCS_Forecasts"; // Default
    }
    String ForecastPeriod = parameters.getValue("ForecastPeriod");
    ForecastPeriod = TSCommandProcessorUtil.expandParameterValue(processor,this,ForecastPeriod);
    String ForecastPublicationDateStart = parameters.getValue("ForecastPublicationDateStart");
    ForecastPublicationDateStart = TSCommandProcessorUtil.expandParameterValue(processor,this,ForecastPublicationDateStart);
    String ForecastPublicationDateEnd = parameters.getValue("ForecastPublicationDateEnd");
    ForecastPublicationDateEnd = TSCommandProcessorUtil.expandParameterValue(processor,this,ForecastPublicationDateEnd);
    String ForecastExceedanceProbabilities = parameters.getValue("ForecastExceedanceProbabilities");
    int [] forecastExceedanceProbabilities = null;
    if ( (ForecastExceedanceProbabilities != null) && !ForecastExceedanceProbabilities.equals("") ) {
        String [] parts = ForecastExceedanceProbabilities.split(",");
        forecastExceedanceProbabilities = new int[parts.length];
        for ( int i = 0; i < parts.length; i++ ) {
            forecastExceedanceProbabilities[i] = Integer.parseInt(parts[i].trim());
        }
    }
   
    String Elements = parameters.getValue("Elements");
    List<Element> elementList = new ArrayList<>();
    Element el;
    if ( (Elements != null) && !Elements.equals("") ) {
        if ( Elements.indexOf(",") < 0 ) {
            el = new Element();
            el.setElementCd(Elements.trim());
            elementList.add(el);
        }
        else {
            String [] elementArray = Elements.split(",");
            for ( int i = 0; i < elementArray.length; i++ ) {
                el = new Element();
                el.setElementCd(elementArray[i].trim());
            }
        }
    }
    String ElevationMin = parameters.getValue("ElevationMin");
    Double elevationMin = null;
    if ( (ElevationMin != null) && !ElevationMin.equals("") ) {
        elevationMin = Double.parseDouble(ElevationMin);
    }
    String ElevationMax = parameters.getValue("ElevationMax");
    Double elevationMax = null;
    if ( (ElevationMax != null) && !ElevationMax.equals("") ) {
        elevationMax = Double.parseDouble(ElevationMax);
    }
    String Interval = parameters.getValue("Interval");
    TimeInterval interval = null;
    if ( (Interval != null) && (Interval.length() > 0) ) {
        try {
            interval = TimeInterval.parseInterval(Interval);
        }
        catch ( InvalidTimeIntervalException e ) {
            // Should not happen since previously checked
        }
    }
	String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}"; // Global default
	}
	String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}"; // Global default
	}
    String TimeZoneMap = parameters.getValue ( "TimeZoneMap" );
    Hashtable<String,String> timeZoneMap = new Hashtable<String,String>();
    if ( (TimeZoneMap != null) && (TimeZoneMap.length() > 0) && (TimeZoneMap.indexOf(":") > 0) ) {
        // First break map pairs by comma
        List<String>pairs = StringUtil.breakStringList(TimeZoneMap, ",", 0 );
        // Now break pairs and put in hashtable
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            timeZoneMap.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String Alias = parameters.getValue("Alias");
    
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

	List<TS> tslist = new ArrayList<>();
	try {
		// Find the data store to use...
		DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
		    dataStoreName, NrcsAwdbDataStore.class );
		if ( dataStore == null ) {
			message = "Could not get data store for name \"" + dataStoreName + "\" to query data.";
			Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the NRCS AWDB web service has been configured with name \"" +
                    dataStoreName + "\" and is available." ) );
			throw new Exception ( message );
		}
		NrcsAwdbDataStore nrcsAwdbDataStore = (NrcsAwdbDataStore)dataStore;

		if ( readForecast ) {
		    // Reading the forecast table
		    if ( commandPhase == CommandPhaseType.RUN ) {
		        DataTable table = nrcsAwdbDataStore.readForecastTable( stationList, stateList,
		                networkList, hucList, elementList, ForecastPeriod, ForecastPublicationDateStart,
		                ForecastPublicationDateEnd, forecastExceedanceProbabilities, ForecastTableID );
	            // Set the table in the processor...
		        PropList request_params = new PropList ( "" );
	            request_params.setUsingObject ( "Table", table );
	            try {
	                processor.processRequest( "SetTable", request_params);
	            }
	            catch ( Exception e ) {
	                message = "Error requesting SetTable(Table=...) from processor.";
	                Message.printWarning(warning_level,
	                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                    routine, message );
	                status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                       message, "Report problem to software support." ) );
	            }
		    }
		    else {
		        // Create an empty table for discovery mode
	            DataTable table = new DataTable();
	            table.setTableID ( ForecastTableID );
	            setDiscoveryTable ( table );
		    }
		}
		else {
		    // Reading time series.
			if ( commandPhase == CommandPhaseType.DISCOVERY ) {
				if ( stateList.indexOf("${") < 0 ) {
					// OK to do discovery read.
					tslist = nrcsAwdbDataStore.readTimeSeriesList ( stationList, stateList, networkList,
						hucList, __boundingBox, countyList, elementList, elevationMin, elevationMax, interval,
						InputStart_DateTime, InputEnd_DateTime, timeZoneMap, readData );
				}
			}
			else {
				// Full read.
				tslist = nrcsAwdbDataStore.readTimeSeriesList ( stationList, stateList, networkList,
					hucList, __boundingBox, countyList, elementList, elevationMin, elevationMax, interval,
					InputStart_DateTime, InputEnd_DateTime, timeZoneMap, readData );
			}
    		// Make sure that size is set...
    		int size = 0;
    		if ( tslist != null ) {
    			size = tslist.size();
    		}
    	
       		if ( (tslist == null) || (size == 0) ) {
       			// Only warn if in run mode because may be using properties so no time series are read up front.
       			if ( commandPhase == CommandPhaseType.RUN ) {
       				Message.printStatus ( 2, routine,"No NRCS AWDB time series were found." );
    	        	// Warn if nothing was retrieved (can be overridden to ignore).
                	message = "No time series were read from the NRCS AWDB web service.";
                	Message.printWarning ( warning_level, 
                    	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                	status.addToLog ( commandPhase,
                    	new CommandLogRecord(CommandStatusType.WARNING,
                    		message, "Data may not be in database." +
                    		"  Previous messages may provide more information.  OK if " ) );
       			}
       		}
       		else {
    			// Else, further process each time series...
    			for ( TS ts: tslist ) {
    			    // Set the alias to the desired string - this is impacted by the Location parameter
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, Alias, status, commandPhase);
                    ts.setAlias ( alias );
    			}
    		}
        
            Message.printStatus ( 2, routine, "Read " + size + " NRCS AWDB time series." );
    
            if ( commandPhase == CommandPhaseType.RUN ) {
                if ( tslist != null ) {
                    // Further process the time series...
                    // This makes sure the period is at least as long as the output period...
    
                    int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                    if ( wc > 0 ) {
                        message = "Error post-processing NRCS AWDB time series after read.";
                        Message.printWarning ( warning_level, 
                            MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                        // Don't throw an exception - probably due to missing data.
                    }
        
                    // Now add the list in the processor...
                    
                    int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                    if ( wc2 > 0 ) {
                        message = "Error adding NRCS AWDB time series after read.";
                        Message.printWarning ( warning_level, 
                            MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
                        throw new CommandException ( message );
                    }
                }
                // Warn if nothing was retrieved (can be overridden to ignore).
                if ( (tslist == null) || (size == 0) ) {
                    message = "No time series were read from the NRCS AWDB value web service.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Data may not be in database.  See previous messages." ) );
                }
            }
            else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            	// Save the time series for discovery mode, may be empty if properties are used.
                setDiscoveryTSList ( tslist );
            }
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error reading time series from NRCS AWDB web service (" + e + ").";
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
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __discoveryTable = table;
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"DataStore",
		"Interval",
		"Stations",
		"States",
		"Networks",
		"HUCs",
		"BoundingBox",
		"Counties",
		"ReadForecast",
		"ForecastTableID",
		"ForecastPeriod",
		"ForecastPublicationDateStart",
		"ForecastPublicationDateEnd",
		"ForecastExceedanceProbabilities",
		"Elements",
    	"ElevationMin",
    	"ElevationMax",
    	"InputStart",
    	"InputEnd",
    	"TimeZoneMap",
    	"Alias"
	};
	return this.toString(parameters, parameterOrder);
}

}