// ReadTimeSeriesList_Command - This class initializes, checks, and runs the ReadTimeSeriesList() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringDictionary;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the ReadTimeSeriesList() command.
*/
public class ReadTimeSeriesList_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Values for ReadData parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

protected final String _Default = "Default";
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";

/**
List of time series read during discovery.
These are TS objects but with mainly the metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public ReadTimeSeriesList_Command () {
	super();
	setCommandName ( "ReadTimeSeriesList" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// Get the property values.
	String TableID = parameters.getValue("TableID");
	String ReadData = parameters.getValue("ReadData");
    String LocationTypeColumn = parameters.getValue("LocationTypeColumn");
    String LocationType = parameters.getValue("LocationType");
	String LocationColumn = parameters.getValue("LocationColumn");
	String DataSourceColumn = parameters.getValue("DataSourceColumn");
	String DataSource = parameters.getValue("DataSource");
	String DataTypeColumn = parameters.getValue("DataTypeColumn");
    String DataType = parameters.getValue("DataType");
    String DataStoreColumn = parameters.getValue("DataStoreColumn");
	String DataStore = parameters.getValue("DataStore");
	String Interval = parameters.getValue("Interval");
	String IfNotFound = parameters.getValue("IfNotFound");

    if ( (TableID == null) || TableID.isEmpty() ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }
    if ( (ReadData != null) && !ReadData.equals("") &&
        !ReadData.equalsIgnoreCase(_False) &&
        !ReadData.equalsIgnoreCase(_True) ) {
        message = "Invalid ReadData flag \"" + ReadData + "\".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the ReadData as " + _False + " or " +
                _True + " (default)." ) );

    }
    if ( (LocationTypeColumn != null) && (LocationTypeColumn.length() > 0) &&
        (LocationType != null) && (LocationType.length() > 0)) {
        message = "LocationTypeColumn and LocationType cannot both be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify only LocationTypeColumn or LocationType, or neither if appropriate." ) );
    }

    if ( (LocationColumn == null) || (LocationColumn.length() == 0) ) {
        message = "The location column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the location column." ) );
    }

    if ( (DataSourceColumn != null) && (DataSourceColumn.length() > 0) &&
        (DataSource != null) && (DataSource.length() > 0)) {
        message = "DataSourceColumn and DataSource cannot both be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify only DataSourceColumn or DataSource, or neither if appropriate." ) );
    }

    if ( (DataTypeColumn != null) && (DataTypeColumn.length() > 0) &&
        (DataType != null) && (DataType.length() > 0)) {
        message = "DataTypeColumn and DataType cannot both be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify only DataTypeColumn or DataType, or neither if appropriate." ) );
    }

	// Interval
	if ((Interval == null) || Interval.equals("")) {
        message = "The interval must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid interval." ) );
	}
	else {
		try {
		    TimeInterval.parseInterval ( Interval );
		}
		catch (Exception e) {
            message = "The data interval \"" + Interval + "\" is not valid.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid interval (e.g., 5Minute, 6Hour, Day, Month, Year)" ) );
		}
	}

	if ( ((DataStoreColumn == null) || DataStoreColumn.equals("")) && ((DataStore == null) || DataStore.equals("")) ) {
        message = "The datastore (column) has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid datastore column or datastore value (e.g., HydroBase)." ) );
    }

    if ( (DataStoreColumn != null) && (DataStoreColumn.length() > 0) && (DataStore != null) && (DataStore.length() > 0)) {
        message = "DataStoreColumn and DataStore cannot both be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify only DataStoreColumn or DataStore, or neither if appropriate." ) );
    }

	if ( (IfNotFound != null) && !IfNotFound.equals("") &&
        !IfNotFound.equalsIgnoreCase(_Ignore) &&
        !IfNotFound.equalsIgnoreCase(_Default) &&
        !IfNotFound.equalsIgnoreCase(_Warn) ) {
        message = "Invalid IfNotFound flag \"" + IfNotFound + "\".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the IfNotFound as " + _Default + ", " +
                _Ignore + ", or (default) " + _Warn + "." ) );
	}

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(25);
	validList.add ( "ReadData" );
    validList.add ( "TableID" );
    validList.add ( "LocationTypeColumn" );
    validList.add ( "LocationType" );
    validList.add ( "LocationColumn" );
    validList.add ( "DataSourceColumn" );
    validList.add ( "DataSource" );
    validList.add ( "DataTypeColumn" );
    validList.add ( "DataType" );
    validList.add ( "Interval" );
    validList.add ( "Scenario" );
    validList.add ( "DataStoreColumn" );
    validList.add ( "DataStore" );
    validList.add ( "InputName" );
    validList.add ( "Alias" );
    validList.add ( "ColumnProperties" );
    validList.add ( "Properties" );
    validList.add ( "IfNotFound" );
    validList.add ( "DefaultUnits" );
    validList.add ( "DefaultOutputStart" );
    validList.add ( "DefaultOutputEnd" );
    validList.add ( "TimeSeriesCountProperty" );
    validList.add ( "TimeSeriesReadCountProperty" );
    validList.add ( "TimeSeriesDefaultCountProperty" );
    validList.add ( "TimeSeriesIndex1Property" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag, warning_level ),
			warning );
		throw new InvalidCommandParameterException ( warning );
	}

    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	// The command will be modified if changed.
	return ( new ReadTimeSeriesList_JDialog ( parent, this, tableIDChoices ) ).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList () {
    return __discoveryTSList;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS).
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class.
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discovery_TS_Vector;
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
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
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
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	int log_level = 3; // Level for non-user messages for log file.
	String command_tag = "" + command_number;
	int warning_count = 0;

    // Get and clear the status and clear the run log.

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // Default.
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen.
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}

	PropList parameters = getCommandParameters();
	// Get the command properties not already stored as members.
	String TableID = parameters.getValue("TableID");
    if ( (TableID != null) && !TableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) ) {
    	// In discovery mode want lists of tables to include ${Property}.
    	if ( TableID.indexOf("${") >= 0 ) {
    		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    	}
    }
    String ReadData = parameters.getValue("ReadData");
    boolean readData = true; // Default for run mode.
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
    	readData = false; // Default - always.
    }
    else {
	    if ( (ReadData != null) && !ReadData.equals("") && ReadData.equalsIgnoreCase(_False) ) {
            readData = false; // OK to ignore reading data in run mode.
        }
    }
    String LocationTypeColumn = parameters.getValue ( "LocationTypeColumn" );
    String LocationType = parameters.getValue ( "LocationType" );
    String LocationColumn = parameters.getValue ( "LocationColumn" );
    String DataSourceColumn = parameters.getValue ( "DataSourceColumn" );
    String DataSource = parameters.getValue ( "DataSource" );
    String [] dataSourceList = new String[0];
    if ( (DataSource != null) && !DataSource.equals("") ) {
        if ( DataSource.indexOf(",") < 0 ) {
            dataSourceList = new String[1];
            dataSourceList[0] = DataSource;
        }
        else {
            dataSourceList = DataSource.split(",");
        }
    }
    String DataTypeColumn = parameters.getValue ( "DataTypeColumn" );
    String DataType = parameters.getValue ( "DataType" );
    if ( DataType == null ) {
        DataType = "";
    }
    String Interval = parameters.getValue ( "Interval" );
    if ( Interval == null ) {
        Interval = "";
    }
    String Scenario = parameters.getValue ( "Scenario" );
    if ( Scenario == null ) {
        Scenario = "";
    }
    String DataStoreColumn = parameters.getValue ( "DataStoreColumn" );
    String DataStore = parameters.getValue ( "DataStore" );
    String InputName = parameters.getValue ( "InputName" );
    if ( InputName == null ) {
        // Set to empty string so check to facilitate processing.
        InputName = "";
    }
    String Alias = parameters.getValue ( "Alias" );
    String ColumnProperties = parameters.getValue ( "ColumnProperties" );
    StringDictionary columnProperties = new StringDictionary(ColumnProperties,":",",");
    String Properties = parameters.getValue ( "Properties" );
    if ( (Properties != null) && (Properties.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	Properties = TSCommandProcessorUtil.expandParameterValue(processor, this, Properties);
	}
    Hashtable<String,String> properties = null;
    if ( (Properties != null) && (Properties.length() > 0) && (Properties.indexOf(":") > 0) ) {
        properties = new Hashtable<String,String>();
        // First break map pairs by comma.
        List<String> pairs = new ArrayList<>();
        if ( Properties.indexOf(",") > 0 ) {
            pairs = StringUtil.breakStringList(Properties, ",", 0 );
        }
        else {
            pairs.add(Properties);
        }
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            properties.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String IfNotFound = parameters.getValue("IfNotFound");
    if ( (IfNotFound == null) || IfNotFound.equals("")) {
        IfNotFound = _Warn; // Default.
    }
    String DefaultUnits = parameters.getValue("DefaultUnits");
    String DefaultOutputStart = parameters.getValue("DefaultOutputStart");
    String DefaultOutputEnd = parameters.getValue("DefaultOutputEnd");
    String TimeSeriesCountProperty = parameters.getValue ( "TimeSeriesCountProperty" );
    if ( (TimeSeriesCountProperty != null) && (TimeSeriesCountProperty.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	TimeSeriesCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, TimeSeriesCountProperty);
	}
    String TimeSeriesReadCountProperty = parameters.getValue ( "TimeSeriesReadCountProperty" );
    if ( (TimeSeriesReadCountProperty != null) && (TimeSeriesReadCountProperty.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	TimeSeriesReadCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, TimeSeriesReadCountProperty);
	}
    String TimeSeriesDefaultCountProperty = parameters.getValue ( "TimeSeriesDefaultCountProperty" );
    if ( (TimeSeriesDefaultCountProperty != null) && (TimeSeriesDefaultCountProperty.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	TimeSeriesDefaultCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, TimeSeriesDefaultCountProperty);
	}
    String TimeSeriesIndex1Property = parameters.getValue ( "TimeSeriesIndex1Property" );
    if ( (TimeSeriesIndex1Property != null) && (TimeSeriesIndex1Property.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    	TimeSeriesIndex1Property = TSCommandProcessorUtil.expandParameterValue(processor, this, TimeSeriesIndex1Property);
	}

    // Assign the default output period, which accepts properties.

    DateTime DefaultOutputStart_DateTime = null;
    DateTime DefaultOutputEnd_DateTime = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			DefaultOutputStart_DateTime = TSCommandProcessorUtil.getDateTime ( DefaultOutputStart, "DefaultOutputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above.
			++warning_count;
		}
	    try {
			DefaultOutputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( DefaultOutputEnd, "DefaultOutputEnd", processor,
				status, warning_level, command_tag );
	    }
	    catch ( InvalidCommandParameterException e ) {
	    	// Warning will have been added above.
	    	++warning_count;
	    }
    }

    // Get the table to process.

    DataTable table = null;
    int locationTypeColumnNum = -1, locationColumnNum = -1, dataSourceColumnNum = -1, dataTypeColumnNum = -1, dataStoreColumnNum = -1;
    if ( commandPhase == CommandPhaseType.RUN ) {
        PropList request_params = null;
        CommandProcessorRequestResultsBean bean = null;
        if ( (TableID != null) && !TableID.equals("") ) {
            // Get the table to be updated.
            request_params = new PropList ( "" );
            request_params.set ( "TableID", TableID );
            try {
                bean = processor.processRequest( "GetTable", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_Table = bean_PropList.getContents ( "Table" );
            if ( o_Table == null ) {
                message = "Unable to find table to process using TableID=\"" + TableID + "\".";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a table exists with the requested ID." ) );
            }
            else {
                table = (DataTable)o_Table;
                if ( (LocationTypeColumn != null) && (LocationTypeColumn.length() > 0) ) {
                    try {
                        locationTypeColumnNum = table.getFieldIndex(LocationTypeColumn);
                    }
                    catch ( Exception e ) {
                        message = "Unable to find location type column \"" + LocationTypeColumn + "\" for TableID=\"" + TableID + "\".";
                        Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a table exists with the column \"" + LocationTypeColumn + "\"." ) );
                    }
                }
                try {
                    locationColumnNum = table.getFieldIndex(LocationColumn);
                }
                catch ( Exception e ) {
                    message = "Unable to find location column \"" + LocationColumn + "\" for TableID=\"" + TableID + "\".";
                    Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that a table exists with the column \"" + LocationColumn + "\"." ) );
                }
                if ( (DataSourceColumn != null) && (DataSourceColumn.length() > 0) ) {
                    try {
                        dataSourceColumnNum = table.getFieldIndex(DataSourceColumn);
                    }
                    catch ( Exception e ) {
                        message = "Unable to find data source column \"" + DataSourceColumn + "\" for TableID=\"" + TableID + "\".";
                        Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a table exists with the column \"" + DataSourceColumn + "\"." ) );
                    }
                }
                if ( (DataTypeColumn != null) && (DataTypeColumn.length() > 0) ) {
                    try {
                        dataTypeColumnNum = table.getFieldIndex(DataTypeColumn);
                    }
                    catch ( Exception e ) {
                        message = "Unable to find data type column \"" + DataTypeColumn + "\" for TableID=\"" + TableID + "\".";
                        Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a table exists with the column \"" + DataTypeColumn + "\"." ) );
                    }
                }
                if ( (DataStoreColumn != null) && (DataStoreColumn.length() > 0) ) {
                    try {
                        dataStoreColumnNum = table.getFieldIndex(DataStoreColumn);
                    }
                    catch ( Exception e ) {
                        message = "Unable to find datastore column \"" + DataStoreColumn + "\" for TableID=\"" + TableID + "\".";
                        Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a table exists with the column \"" + DataStoreColumn + "\"." ) );
                    }
                }
            }
        }
    }

    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings for command parameters.";
        Message.printWarning ( 2,
        MessageUtil.formatMessageTag(command_tag, ++warning_count),
        routine,message);
        throw new InvalidCommandParameterException ( message );
    }

	// Process the rows in the table and read time series.
    List<TS> tslist = new ArrayList<>(); // Keep the list of time series.
    String dataSource;
    int defaultCount = 0; // Count of default time series.
	try {
        if ( commandPhase == CommandPhaseType.DISCOVERY ){
        	// TODO SAM 2015-05-17 Not really doing much in discovery mode - not generating time series - too complex.
        }
        else if ( commandPhase == CommandPhaseType.RUN ){
            // TODO SAM 2013-05-17 Need to determine whether to read table even in discovery mode.
            // Otherwise won't be able to generate time series in discovery mode.

            // Loop through the records in the table and match the identifiers.

            StringBuffer tsidentString = new StringBuffer();
            TableRecord rec = null;
            String locationID;
            TS ts = null;
            String dataType, locationType, dataStore;
            int tsize = table.getNumberOfRecords();
            List<String> problems = new ArrayList<>(); // Use for temporary list of problems - need because multiple data sources.
            List<String> suggestions = new ArrayList<>();
            boolean defaultTSRead = false; // If a default time series was read.
            for ( int i = 0; i < tsize; i++ ) {
                problems.clear();
                suggestions.clear();
                defaultTSRead = false;
                rec = table.getRecord ( i );
                // Location type.
                if ( locationTypeColumnNum >= 0 ) {
                    locationType = rec.getFieldValueString ( locationTypeColumnNum );
                }
                else {
                    locationType = LocationType;
                }
                locationID = rec.getFieldValueString ( locationColumnNum );
                // Skip blank location identifiers.
                if ( (locationID == null) || (locationID.trim().length() == 0) ) {
                    continue;
                }
                // Data type.
                if ( dataTypeColumnNum >= 0 ) {
                    dataType = rec.getFieldValueString ( dataTypeColumnNum );
                }
                else {
                    dataType = DataType;
                }
                // Data store.
                if ( dataStoreColumnNum >= 0 ) {
                    dataStore = rec.getFieldValueString ( dataStoreColumnNum );
                }
                else {
                    dataStore = DataStore;
                }
                // Allow more than one data source to be specified, which is useful when there is mixed ownership of stations.
                int nDataSource = 1;
                if ( dataSourceList.length > 0 ) {
                    nDataSource = dataSourceList.length;
                }
                boolean notFoundLogged = false; // Used to handle read exceptions vs. no time series found.
                for ( int iDataSource = 0; iDataSource < nDataSource; iDataSource++ ) {
                    tsidentString.setLength(0);
                    if ( dataSourceList.length == 0 ) {
                        if ( dataSourceColumnNum >= 0 ) {
                            // Get the data source from the table.
                            dataSource = rec.getFieldValueString ( dataSourceColumnNum );
                        }
                        else {
                            dataSource = "";
                        }
                    }
                    else {
                        dataSource = dataSourceList[iDataSource];
                    }
                    if ( (locationType != null) && (locationType.length() > 0) ) {
                        tsidentString.append ( locationType + TSIdent.LOC_TYPE_SEPARATOR );
                    }
                    tsidentString.append ( locationID + "." + dataSource + "." + dataType + "." + Interval + "~" + dataStore );
                    if ( InputName.length() > 0 ) {
                        tsidentString.append ( "~" + InputName );
                    }
                    String tsid = tsidentString.toString();
                    // Make a request to the processor to read a time series.
                    notifyCommandProgressListeners ( i, tsize, (float)-1.0, "Reading time series " + tsid);
                    PropList request_params = new PropList ( "" );
                    request_params.set ( "TSID", tsidentString.toString() );
                    request_params.setUsingObject ( "WarningLevel", new Integer(warning_level) );
                    request_params.set ( "CommandTag", command_tag );
                    if ( iDataSource == (nDataSource - 1) ) {
                        // Only default on the last data source.
                        request_params.set ( "IfNotFound", IfNotFound );
                        if ( DefaultOutputStart_DateTime != null ) {
                        	request_params.setUsingObject( "DefaultOutputStart", DefaultOutputStart_DateTime );
                        }
                        if ( DefaultOutputEnd_DateTime != null ) {
                        	request_params.setUsingObject( "DefaultOutputEnd", DefaultOutputEnd_DateTime );
                        }
                    }
                    request_params.setUsingObject ( "ReadData", new Boolean(readData) );
                    CommandProcessorRequestResultsBean bean = null;
                    try {
                        bean = processor.processRequest( "ReadTimeSeries", request_params);
                        PropList bean_PropList = bean.getResultsPropList();
                        Object o_TS = bean_PropList.getContents ( "TS" );
                        if ( o_TS != null ) {
                            ts = (TS)o_TS;
                        }
                    }
                    catch ( Exception e ) {
                        if ( iDataSource == (nDataSource - 1) ) {
                            // Last attempt in a list of data sources.
                            problems.add("Time series could not be found using identifier \"" + tsid + "\" (" + e + ")");
                            Message.printWarning(3,routine,e);
                            suggestions.add("Verify that the identifier information is correct.");
                        }
                        else {
                            // Going to try another data source, or defaulting time series.
                            problems.add("Time series could not be found using identifier \"" + tsid + "\" (" + e + ") - will try next data source");
                            suggestions.add("Verify that the identifier information is correct.");
                        }
                        ts = null;
                        notFoundLogged = true;
                    }
                    if ( ts == null ) {
                        if ( !notFoundLogged ) {
                            // Only want to include a warning once so don't duplicate above exception.
                            if ( iDataSource == (nDataSource - 1) ) {
                                problems.add("Time series could not be found using identifier \"" + tsid + "\".");
                                suggestions.add("Verify that the identifier information is correct.");
                            }
                            else {
                                // Non-fatal - ignoring or defaulting time series.
                                problems.add("Time series could not be found using identifier \"" + tsid + "\" - will try next data source.");
                                suggestions.add("Verify that the identifier information is correct.");
                            }
                        }
                    }
                    else {
                        // Found a time series so break out of data source loop.
                        break;
                    }
                }
                // Now have processed all data sources.
                // If no time series was found and a default was assigned, it will have a property set "DefaultTimeSeriesRead=true".
                if ( ts == null ) {
                    if ( IfNotFound.equalsIgnoreCase(_Ignore) ) {
                        // Just continue, but do add warnings to the log.
                        for ( int ip = 0; ip < problems.size(); ++ip ) {
                            Message.printWarning ( warning_level,
                                MessageUtil.formatMessageTag( command_tag, ++warning_count ),
                                routine, problems.get(ip) );
                        }
                        continue;
                    }
                    else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
                        // Warn and continue.
                        for ( int ip = 0; ip < problems.size(); ++ip ) {
                            Message.printWarning ( warning_level,
                                MessageUtil.formatMessageTag( command_tag, ++warning_count ),
                                routine, problems.get(ip) );
                            status.addToLog(commandPhase,
                                new CommandLogRecord( CommandStatusType.FAILURE, problems.get(ip),suggestions.get(ip)));
                        }
                        continue;
                    }
                    else if ( IfNotFound.equalsIgnoreCase(_Default) ) {
                        // A common problem is that the output period was not set so the time series could not be defaulted.
                        for ( int ip = 0; ip < problems.size(); ++ip ) {
                            Message.printWarning ( warning_level,
                                MessageUtil.formatMessageTag( command_tag, ++warning_count ),
                                routine, problems.get(ip) );
                            status.addToLog(commandPhase,
                                new CommandLogRecord( CommandStatusType.FAILURE, problems.get(ip),suggestions.get(ip)));
                        }
                        message = "Attempt to use default time series failed for \"" + tsidentString + "\"";
                        Message.printWarning ( warning_level,
                            MessageUtil.formatMessageTag( command_tag, ++warning_count ),
                            routine, message );
                        status.addToLog(commandPhase,
                            new CommandLogRecord( CommandStatusType.FAILURE, message,
                            	"May need to set input period when reading time series or set default output period."));
                        continue;
                    }
                } // End ts == null
                // If here have a time series to process further and return.
                Object o = ts.getProperty("DefaultTimeSeriesRead");
                if ( (o != null) && o.toString().equalsIgnoreCase("true") ) {
                	defaultTSRead = true;
                	++defaultCount;
                }
                if ( defaultTSRead && (DefaultUnits != null) && ts.getDataUnits().isEmpty() ) {
                    // A default time series was read so assign default units.
                    ts.setDataUnits ( DefaultUnits );
                }
                if ( columnProperties != null ) {
                    // Set time series properties based on column values.
                    LinkedHashMap<String,String> map = columnProperties.getLinkedHashMap();
                    for ( Map.Entry<String,String> entry: map.entrySet() ) {
                        String columnName = entry.getKey();
                        if ( columnName.equals("*") ) {
                            // Set all the table columns as properties, including null values.
                            for ( int icol = 0; icol < table.getNumberOfFields(); icol++ ) {
                                ts.setProperty( table.getFieldName(icol), rec.getFieldValue(icol) );
                            }
                        }
                        else {
                            // Else only set the specified columns.
                            String propertyName = entry.getValue();
                            int columnNum = -1;
                            try {
                                columnNum = table.getFieldIndex(columnName);
                            }
                            catch ( Exception e ) {
                            }
                            if ( propertyName.equals("*") ) {
                                // Get the property name from the table.
                                propertyName = columnName;
                            }
                            if ( columnNum >= 0 ) {
                                // Set even if null.
                                ts.setProperty( propertyName, rec.getFieldValue(columnNum) );
                            }
                            else {
                                ts.setProperty( propertyName, null );
                            }
                        }
                    }
                }
                if ( properties != null ) {
                    // Assign properties.
                    Enumeration<String> keys = properties.keys();
                    String key = null;
                    while ( keys.hasMoreElements() ) {
                        key = keys.nextElement();
                        ts.setProperty( key, TSCommandProcessorUtil.expandTimeSeriesMetadataString (
                            processor, ts, (String)properties.get(key), status, commandPhase) );
                    }
                }
                if ( TimeSeriesIndex1Property != null ) {
                    // Set a property indicating the position in the list.
                    ts.setProperty(TimeSeriesIndex1Property, new Integer((i + 1)) );
                }
                // Set the alias - do this after setting the properties because the alias may use the properties.
                if ( (ts != null) && (Alias != null) && !Alias.equals("") ) {
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, Alias, status, commandPhase);
                    ts.setAlias ( alias );
                }
                tslist.add ( ts );
            }
        }
    }
	catch ( Exception e ) {
		message = "Unexpected error reading time series for table \"" + TableID + "\" (" + e + ")";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count ),
			routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(commandPhase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,"Check the log file for details."));
		throw new CommandException ( message );
	}

    if ( commandPhase == CommandPhaseType.RUN ) {
        if ( tslist != null ) {
            // Now add the list in the processor.

            int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
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
            // Set the property indicating the number of time series read.
            if ( (TimeSeriesCountProperty != null) && !TimeSeriesCountProperty.isEmpty() ) {
                PropList request_params = new PropList ( "" );
                request_params.setUsingObject ( "PropertyName", TimeSeriesCountProperty );
                request_params.setUsingObject ( "PropertyValue", new Integer(tslist.size()) );
                try {
                    processor.processRequest( "SetProperty", request_params);
                }
                catch ( Exception e ) {
                    message = "Error requesting SetProperty(Property=\"" + TimeSeriesCountProperty + "\") from processor.";
                    Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                }
            }
	        // Set the property indicating the number of time series defaulted.
	        if ( (TimeSeriesDefaultCountProperty != null) && !TimeSeriesDefaultCountProperty.isEmpty() ) {
	            PropList request_params = new PropList ( "" );
	            request_params.setUsingObject ( "PropertyName", TimeSeriesDefaultCountProperty );
	            request_params.setUsingObject ( "PropertyValue", new Integer(defaultCount) );
	            try {
	                processor.processRequest( "SetProperty", request_params);
	            }
	            catch ( Exception e ) {
	                message = "Error requesting SetProperty(Property=\"" + TimeSeriesDefaultCountProperty + "\") from processor.";
	                Message.printWarning(log_level,
	                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                    routine, message );
	                status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Report the problem to software support." ) );
	            }
	        }
	        if ( (TimeSeriesReadCountProperty != null) && !TimeSeriesReadCountProperty.isEmpty() ) {
	            PropList request_params = new PropList ( "" );
	            request_params.setUsingObject ( "PropertyName", TimeSeriesReadCountProperty );
	            request_params.setUsingObject ( "PropertyValue", new Integer(tslist.size() - defaultCount) );
	            try {
	                processor.processRequest( "SetProperty", request_params);
	            }
	            catch ( Exception e ) {
	                message = "Error requesting SetProperty(Property=\"" + TimeSeriesReadCountProperty + "\") from processor.";
	                Message.printWarning(log_level,
	                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                    routine, message );
	                status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Report the problem to software support." ) );
	            }
	        }
        }
    }
    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( tslist );
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
private void setDiscoveryTSList ( List<TS> discoveryTSList ) {
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"ReadData",
    	"TableID",
    	"LocationTypeColumn",
    	"LocationType",
    	"LocationColumn",
    	"DataSourceColumn",
    	"DataSource",
    	"DataTypeColumn",
    	"DataType",
    	"Interval",
    	"Scenario",
    	"DataStoreColumn",
    	"DataStore",
    	"InputName",
    	"Alias",
    	"ColumnProperties",
    	"Properties",
    	"IfNotFound",
    	"DefaultUnits",
    	"DefaultOutputStart",
    	"DefaultOutputEnd",
    	"TimeSeriesCountProperty",
    	"TimeSeriesReadCountProperty",
    	"TimeSeriesDefaultCountProperty",
    	"TimeSeriesIndex1Property"
	};
	return this.toString(parameters, parameterOrder);
}

}