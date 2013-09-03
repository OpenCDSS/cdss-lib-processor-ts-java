package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeParser;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the TableToTimeSeries() command.
*/
public class TableToTimeSeries_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

protected final String _False = "False";
protected final String _True = "True";

/**
String that indicates that column names should be taken from the table.
For example TC[1:] indicates columns 1 through the total number of columns.
*/
protected final String _TC = "TC[";

// FIXME SAM 2007-12-19 Need to evaluate this - runtime versions may be different.
/**
Private data members shared between the checkCommandParameters() and the 
runCommand() methods (prevent code duplication parsing input).  
*/
private DateTime __InputStart = null;
private DateTime __InputEnd = null;

/**
Column names for each time series being processed, from the ColumnNames parameter that
has been expanded to reflect file column names.
See also __columnNamesRuntime.
*/
private List<String> __columnNamesRuntime = new Vector<String>();

/**
Data types for each time series being processed.
*/
private List<String> __dataType = new Vector<String>();

/**
Date column name, expanded for runtime, consistent with the ColumnNames runtime values.
*/
private String __dateColumnRuntime = null;

/**
Time column name, expanded for runtime, consistent with the ColumnNames runtime values.
*/
private String __timeColumnRuntime = null;

/**
Date/time column name, expanded for runtime, consistent with the ColumnNames runtime values.
*/
private String __dateTimeColumnRuntime = null;

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_List = null;

/**
Interval for the time series.
*/
private TimeInterval __interval = null;

/**
Location column for each time series being processed, expanded for runtime.
*/
private String __locationColumnRuntime = null;

/**
Location Type for each time series being processed, expanded for runtime.
*/
private List<String> __locationTypeRuntime = new Vector<String>();

/**
Location type for each time series being processed.
*/
private List<String> __locationType = new Vector<String>();

/**
Location ID for each time series being processed, expanded for runtime.
*/
private List<String> __locationIDRuntime = new Vector<String>();

/**
Missing value strings that may be present in the file.
*/
private List<String> __missingValue = new Vector<String>();

/**
Data source for each time series being processed.
*/
private List<String> __dataSource = new Vector<String>();

/**
Scenario for each time series being processed.
*/
private List<String> __scenario = new Vector<String>();

/**
TODO SAM 2012-11-09 Currently not used
Row ranges to skip (single rows will have same and start value.  Rows are 1+.
*/
private int[][] __skipRows = null; // Allocated when checking parameters

/**
Data units for each time series being processed.
*/
private List<String> __units = new Vector();

/**
Column names for data values, for each time series being processed, expanded for runtime.
*/
private List<String> __valueColumnsRuntime = new Vector();

/**
Column names for data flags, for each time series being processed, expanded for runtime.
*/
private List<String> __flagColumnsRuntime = new Vector();

/**
Constructor.
*/
public TableToTimeSeries_Command ()
{
	super();
	setCommandName ( "TableToTimeSeries" );
}

// TODO SAM 2012-11-15 Change so checks examine column names - after discovery mode is updated to be aware
// of column names.  For now only handle TC[] notation at runtime in the runCommandInternal method.
// Commented-out code below illustrates checks that could be done in discovery mode
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
{
    String routine = getClass().getName() + ".checkCommandParameters";
	String warning = "";
    String message;
    
    //CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Get the property values. 
	String TableID = parameters.getValue("TableID");
    String DateTimeColumn = parameters.getValue("DateTimeColumn" );
    String DateColumn = parameters.getValue("DateColumn" );
    String TimeColumn = parameters.getValue("TimeColumn" );
    String ValueColumn = parameters.getValue("ValueColumn" );
    String FlagColumn = parameters.getValue("FlagColumn" );
    //String SkipRows = parameters.getValue("SkipRows" );
    String LocationType = parameters.getValue("LocationType" );
    String LocationID = parameters.getValue("LocationID" );
    String LocationTypeColumn = parameters.getValue("LocationTypeColumn" );
    String LocationColumn = parameters.getValue("LocationColumn" );
    boolean singleColumn = false;
    if ( (LocationColumn != null) && !LocationColumn.equals("") ) {
        singleColumn = true;
    }
    String DataSourceColumn = parameters.getValue("DataSourceColumn" );
    String DataTypeColumn = parameters.getValue("DataTypeColumn" );
    String ScenarioColumn = parameters.getValue("ScenarioColumn" );
    String UnitsColumn = parameters.getValue("UnitsColumn" );
    String DataSource = parameters.getValue("DataSource" );
    String DataType = parameters.getValue("DataType" );
    String Interval = parameters.getValue("Interval" );
    String Scenario = parameters.getValue("Scenario" );
    String Units = parameters.getValue("Units" );
    String MissingValue = parameters.getValue("MissingValue" );
    String Alias = parameters.getValue("Alias" );
	String InputStart = parameters.getValue("InputStart");
	String InputEnd = parameters.getValue("InputEnd");
	
    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }
    
    /*
    if ( (SkipRows != null) && !SkipRows.equals("") ) {
        try {
            setSkipRows ( StringUtil.parseIntegerRangeSequence(SkipRows, ",", 0, 0) );
        }
        catch ( Exception e ) {
            message = "The rows to skip (" + SkipRows + ") is invalid (" + e + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify as comma-separated rows (1+) and/or ranges a-b." ) );
        }
    }*/

    // Either DateTimeColumn or DateColumn must be specified.
    // If TimeColumn is specified, then DateTimeColumn must not be specified and DateColumn must be specified
    String dateTimeColumnRuntime = null;
    String dateColumnRuntime = null;
    String timeColumnRuntime = null;
    setDateTimeColumnRuntime (dateTimeColumnRuntime);
    setDateColumnRuntime (dateColumnRuntime);
    setTimeColumnRuntime (timeColumnRuntime);
    boolean dateTimeColumnSpecified = false;
    boolean dateColumnSpecified = false;
    boolean timeColumnSpecified = false;
    if ( (DateTimeColumn != null) && (DateTimeColumn.length() > 0) ) {
        dateTimeColumnSpecified = true;
    }
    if ( (DateColumn != null) && (DateColumn.length() > 0) ) {
        dateColumnSpecified = true;
    }
    if ( (TimeColumn != null) && (TimeColumn.length() > 0) ) {
        timeColumnSpecified = true;
    }
    if ( !dateTimeColumnSpecified && !dateColumnSpecified ) {
        message = "The date/time or date column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a date/time or date column as one of the values from ColumnNames." ) );
    }
    if ( dateTimeColumnSpecified && dateColumnSpecified ) {
        message = "The date/time or date column must be specified (but not both).";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a date/time OR date column as one of the values from ColumnNames." ) );
    }
    if ( dateTimeColumnSpecified && timeColumnSpecified ) {
        message = "The time column can only be specified with the date column (not the date/time column).";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the date column as one of the values from ColumnNames when a time column is specified." ) );
    }
    if ( !dateColumnSpecified && timeColumnSpecified ) {
        message = "The time column can only be specified when the date column also is specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the date column as one of the values from ColumnNames when a time column is specified." ) );
    }
    
    if ( dateTimeColumnSpecified ) {
        if ( StringUtil.indexOfIgnoreCase(DateTimeColumn,_TC, 0) >= 0 ) {
            // Original string used slice notation for column name
            try {
                List<String> dateTimeColumnName = new Vector();
                dateTimeColumnName.add ( DateTimeColumn ); // Only one
                //dateTimeColumnName = readColumnNamesFromFile(InputFile_full, dateTimeColumnName,
                //    StringUtil.literalToInternal(Delimiter), Comment, getSkipRows(),
                //    getSkipRowsAfterComments() );
                dateTimeColumnRuntime = dateTimeColumnName.get(0);
            }
            catch ( Exception e ) {
                message = "Error getting the date/time column name to use for runtime processing (" + e + ").";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the date/time column name is specified using valid syntax." ) );
                Message.printWarning( 3, routine, e );
            }
        }
        else {
            // Just use the column names as specified for runtime
            dateTimeColumnRuntime = DateTimeColumn;
        }
        setDateTimeColumnRuntime ( dateTimeColumnRuntime );
        // Now check the value of the date/time column versus the available columns
        /* TODO SAM 2012-11-12 Need to enable something
        if ( getColumnNumberFromName(getDateTimeColumnRuntime(), getColumnNamesRuntime()) < 0 ) {
            message = "The DateTimeColumn (" + getDateTimeColumnRuntime() + ") is not a recognized column name.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a date/time column as one of the values from ColumnNames." ) );
        }
        */
    }
    
    if ( dateColumnSpecified ) {
        if ( StringUtil.indexOfIgnoreCase(DateColumn,_TC, 0) >= 0 ) {
            // Original string used slice notation for column
            try {
                List<String> dateColumnName = new Vector();
                dateColumnName.add ( DateColumn ); // Only one
                //dateColumnName = readColumnNamesFromFile(InputFile_full, dateColumnName,
                //    StringUtil.literalToInternal(Delimiter), Comment, getSkipRows(),
                //    getSkipRowsAfterComments() );
                dateColumnRuntime = dateColumnName.get(0);
            }
            catch ( Exception e ) {
                message = "Error getting the date column name to use for runtime processing (" + e + ").";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the date column name is specified using valid syntax." ) );
                Message.printWarning( 3, routine, e );
            }
        }
        else {
            // Just use the column names as specified for runtime
            dateColumnRuntime = DateColumn;
        }
        setDateColumnRuntime ( dateColumnRuntime );
        // Now check the value of the date column versus the available columns
        /* TODO SAM 2012-11-12 Need to enable something
        if ( getColumnNumberFromName(getDateColumnRuntime(), getColumnNamesRuntime()) < 0 ) {
            message = "The DateColumn (" + getDateColumnRuntime() + ") is not a recognized column name.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a date column as one of the values from ColumnNames." ) );
        }
        */
    }
    
    if ( timeColumnSpecified ) {
        if ( StringUtil.indexOfIgnoreCase(TimeColumn,_TC, 0) >= 0 ) {
            // Original string used slice notation for column name
            try {
                List<String> timeColumnName = new Vector();
                timeColumnName.add ( TimeColumn ); // Only one
                //timeColumnName = readColumnNamesFromFile(InputFile_full, timeColumnName,
                //    StringUtil.literalToInternal(Delimiter), Comment, getSkipRows(),
                //    getSkipRowsAfterComments() );
                timeColumnRuntime = timeColumnName.get(0);
            }
            catch ( Exception e ) {
                message = "Error getting the time column name to use for runtime processing (" + e + ").";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the time column name is specified using valid syntax." ) );
                Message.printWarning( 3, routine, e );
            }
        }
        else {
            // Just use the column names as specified for runtime
            timeColumnRuntime = TimeColumn;
        }
        setTimeColumnRuntime ( timeColumnRuntime );
        // Now check the value of the time column versus the available columns
        /* TODO SAM 2012-11-12 Need to enable something
        if ( getColumnNumberFromName(getTimeColumnRuntime(), getColumnNamesRuntime()) < 0 ) {
            message = "The TimeColumn (" + getTimeColumnRuntime() + ") is not a recognized column name.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a time column as one of the values from ColumnNames." ) );
        }
        */
    }
    
    List<String> valueColumns = new Vector<String>();
    List<String> valueColumnsRuntime = new Vector();
    setValueColumnsRuntime ( valueColumnsRuntime );
    if ( (ValueColumn == null) || (ValueColumn.length() == 0) ) {
        message = "The value column(s) must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify data column(s) from ColumnNames, separated by commas." ) );
    }
    else {
        valueColumns = StringUtil.breakStringList(ValueColumn, ",", StringUtil.DELIM_ALLOW_STRINGS );
        if ( StringUtil.indexOfIgnoreCase(ValueColumn,_TC, 0) >= 0 ) {
            // Original string used slice notation for column name
            try {
                //valueColumnsRuntime = readColumnNamesFromFile(InputFile_full, valueColumns,
                //    StringUtil.literalToInternal(Delimiter), Comment, getSkipRows(),
                //    getSkipRowsAfterComments() );
            }
            catch ( Exception e ) {
                message = "Error getting the value column name(s) to use for runtime processing (" + e + ").";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the value column name(s) are specified using valid syntax." ) );
                Message.printWarning( 3, routine, e );
            }
        }
        else {
            // Just use the column names as specified for runtime
            valueColumnsRuntime = valueColumns;
        }
        if ( singleColumn && (valueColumns.size() != 1) ) {
            message = "Expecting 1 value column but have " + valueColumns.size() + " (" + ValueColumn + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that 1 value column name is specified." ) );
        }
        setValueColumnsRuntime ( valueColumnsRuntime );
        // Now check for valid column names...
        /* TODO SAM 2012-11-12 Need to enable something
        for ( String valueColumnRuntime : valueColumnsRuntime ) {
            if ( getColumnNumberFromName(valueColumnRuntime, getValueColumnsRuntime()) < 0 ) {
                message = "The ValueColumn (" + valueColumnRuntime + ") is not a recognized column name.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify value column(s) matching ColumnNames, separated by commas." ) );
            }
        }
        */
    }
    
    List<String> flagColumns = new Vector<String>();
    List<String> flagColumnsRuntime = new Vector<String>();
    setFlagColumnsRuntime ( flagColumnsRuntime );
    if ( (FlagColumn != null) && (FlagColumn.length() != 0) ) {
        flagColumns = StringUtil.breakStringList(FlagColumn, ",", StringUtil.DELIM_ALLOW_STRINGS );
        if ( StringUtil.indexOfIgnoreCase(FlagColumn,_TC, 0) >= 0 ) {
            // Original string used slice notation for column name
            try {
                //flagColumnsRuntime = readColumnNamesFromFile(InputFile_full, flagColumns,
                //    StringUtil.literalToInternal(Delimiter), Comment, getSkipRows(),
                //    getSkipRowsAfterComments() );
            }
            catch ( Exception e ) {
                message = "Error getting the flag column name(s) to use for runtime processing (" + e + ").";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the flag column name(s) are specified using valid syntax." ) );
                Message.printWarning( 3, routine, e );
            }
        }
        else {
            // Just use the column names as specified for runtime
            flagColumnsRuntime = flagColumns;
        }
        if ( singleColumn && (flagColumns.size() != 1) ) {
            message = "Expecting 1 flag column but have " + flagColumns.size() + " (" + FlagColumn + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that 1 flag column name is specified." ) );
        }
        setFlagColumnsRuntime ( flagColumnsRuntime );
        // Now check for valid column names...
        for ( String flagColumnRuntime : flagColumnsRuntime ) {
            /* TODO SAM 2012-11-12 Need to enable something
            if ( getColumnNumberFromName(flagColumnRuntime, getFlagColumnsRuntime()) < 0 ) {
                message = "The FlagColumn (" + flagColumnRuntime + ") is not a recognized column name.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify flag column(s) matching ColumnNames, separated by commas." ) );
            }
            */
        }
    }
    if ( (valueColumnsRuntime.size() > 0) && (flagColumnsRuntime.size() > 0) &&
        (valueColumnsRuntime.size() != flagColumnsRuntime.size()) ) {
        message = "The number of flag column names (" + flagColumnsRuntime.size() +
            ") does not match the number of value column names (" + valueColumnsRuntime.size() + ").";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify " + valueColumnsRuntime.size() + " flag column names separated by commas." ) );
    }

    List<String> locationIDRuntime = new Vector<String>();
    setLocationIDRuntime( locationIDRuntime );
    if ( ((LocationID == null) || LocationID.equals("")) &&
        ((LocationColumn == null) || LocationColumn.equals("")) ) {
        message = "The location ID column(s) must be specified for multi-column data tables OR " +
            "the location column must be specified for single-column data tables.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify 1+ location ID column(s) values separated by commas or a single location column." ) );
    }
    if ( (LocationID != null) && !LocationID.equals("") ) {
        // Can have one value that is re-used, or LocationID for each time series
        List<String>tokens = StringUtil.breakStringList(LocationID, ",", 0);
        if ( StringUtil.indexOfIgnoreCase(LocationID,_TC, 0) >= 0 ) {
            // Original string used slice notation for column name
            try {
                //tokens = readColumnNamesFromFile(InputFile_full, tokens,
                //    StringUtil.literalToInternal(Delimiter), Comment, getSkipRows(),
                //    getSkipRowsAfterComments() );
            }
            catch ( Exception e ) {
                message = "Error getting the location ID(s) to use for runtime processing (" + e + ").";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the location ID(s) are specified using valid syntax." ) );
                Message.printWarning( 3, routine, e );
            }
        }
        if ( (tokens.size() != 1) && (tokens.size() != getValueColumnsRuntime().size()) ) {
            message = "The number of LocationID strings (" + tokens.size() + ") is invalid - expecting " +
                getValueColumnsRuntime().size();
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + getValueColumnsRuntime().size() +
                    "), separated by commas." ) );
            // Set to empty strings to simplify runtime error handling
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                locationIDRuntime.add ( "" );
            }
        }
        else {
            // Process the LocationID for each time series
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    locationIDRuntime.add(tokens.get(0));
                }
                else {
                    locationIDRuntime.add(tokens.get(i));
                }
            }
        }
    }
    setLocationIDRuntime ( locationIDRuntime );
    
    if ( ((LocationID != null) && !LocationID.equals("")) &&
        ((LocationColumn != null) && !LocationColumn.equals("")) ) {
        // Can only specify location one way
        message = "LocationID and LocationColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify LocationID or LocationColumn but not both." ) );
    }

    if ( ((LocationType != null) && !LocationType.equals("")) &&
        ((LocationTypeColumn != null) && !LocationTypeColumn.equals("")) ) {
        // Can only specify location type one way
        message = "LocationType and LocationTypeColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify LocationType or LocationTypeColumn but not both." ) );
    }
    
    if ( ((DataSource != null) && !DataSource.equals("")) &&
        ((DataSourceColumn != null) && !DataSourceColumn.equals("")) ) {
        // Can only specify data source one way
        message = "DataSource and DataSourceColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify DataSource or DataSourceColumn but not both." ) );
    }
    
    if ( ((DataType != null) && !DataType.equals("")) &&
        ((DataTypeColumn != null) && !DataTypeColumn.equals("")) ) {
        // Can only specify data type one way
        message = "DataType and DataTypeColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify DataType or DataTypeColumn but not both." ) );
    }
    
    if ( ((Scenario != null) && !Scenario.equals("")) &&
        ((ScenarioColumn != null) && !ScenarioColumn.equals("")) ) {
        // Can only specify scenario one way
        message = "Scenario and ScenarioColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify Scenario or ScenarioColumn but not both." ) );
    }
    
    if ( ((Units != null) && !Units.equals("")) &&
        ((UnitsColumn != null) && !UnitsColumn.equals("")) ) {
        // Can only specify units one way
        message = "Units and UnitsColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify Units or UnitsColumn but not both." ) );
    }
    
    List<String> locationType = new Vector<String>();
    setLocationType ( locationType );
    if ( (LocationType != null) && !LocationType.equals("") ) {
        // Can have one value that is re-used, or LocationType for each time series
        List<String>tokens = StringUtil.breakStringList(LocationType, ",", 0);
        setLocationType(tokens);
    }

    List<String> dataSource = new Vector<String>();
    setDataSource ( dataSource );
    if ( (DataSource != null) && !DataSource.equals("") ) {
        // Can have one value that is re-used, or DataSource for each time series
        List<String>tokens = StringUtil.breakStringList(DataSource, ",", 0);
        setDataSource(tokens);
        /*
        if ( (tokens.size() != 1) && (tokens.size() != getValueColumnsRuntime().size()) ) {
            message = "The number of DataSource strings (" + tokens.size() + ") is invalid - expecting " +
                getValueColumnsRuntime().size();
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + getValueColumnsRuntime().size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                dataSource.add ( "" );
            }
        }
        else {
            // Process the DataSource for each time series
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    dataSource.add(tokens.get(0));
                }
                else {
                    dataSource.add(tokens.get(i));
                }
            }
        }*/
    }
    /*
    else {
        for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
            dataSource.add ( "" );
        }
    }
    setDataSource ( dataSource );
    */
    
    List<String> dataType = new Vector<String>();
    setDataType ( dataType );
    if ( (DataType == null) || DataType.equals("") ) {
        // Set to the same as the value columns
        //for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
        //    dataType.add ( getValueColumnsRuntime().get(i) );
        //}
    }
    else {
        // Can have one value that is re-used, or DataType for each time series
        List<String>tokens = StringUtil.breakStringList(DataType, ",", 0);
        setDataType(tokens);
        /*
        if ( (tokens.size() != 1) && (tokens.size() != getValueColumnsRuntime().size()) ) {
            message = "The number of DataType strings (" + tokens.size() + ") is invalid - expecting " +
                getValueColumnsRuntime().size();
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + getValueColumnsRuntime().size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                dataType.add ( "" );
            }
        }
        else {
            // Process the DataType for each time series
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    dataType.add(tokens.get(0));
                }
                else {
                    dataType.add(tokens.get(i));
                }
            }
        }*/
    }
    //setDataType ( dataType );

    TimeInterval interval = null;
    setInterval ( interval );
    if ( (Interval == null) || Interval.equals("") ) {
        message = "The time series data interval must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid data interval using the command editor." ) );
    }
    else {
        try {
            interval = TimeInterval.parseInterval(Interval);
        }
        catch ( Exception e ) {
            message = "The time series data interval is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid data interval using the command editor." ) );
        }
    }
    setInterval ( interval );
    
    List<String> scenario = new Vector<String>();
    setScenario ( scenario );
    if ( (Scenario != null) && !Scenario.equals("") ) {
        // Can have one value that is re-used, or Scenario for each time series
        List<String>tokens = StringUtil.breakStringList(Scenario, ",", 0);
        setScenario ( tokens );
        /*
        if ( (tokens.size() != 1) && (tokens.size() != getValueColumnsRuntime().size()) ) {
            message = "The number of Scenario strings (" + tokens.size() + ") is invalid - expecting " +
                getValueColumnsRuntime().size();
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + getValueColumnsRuntime().size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                scenario.add ( "" );
            }
        }
        else {
            // Process the Scenario for each time series
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    scenario.add(tokens.get(0));
                }
                else {
                    scenario.add(tokens.get(i));
                }
            }
        }*/
    }/*
    else {
        for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
            scenario.add ( "" );
        }
    }
    setScenario ( scenario );
    */
    
    List<String> units = new Vector();
    setUnits ( units );
    if ( (Units != null) && !Units.equals("") ) {
        // Can have one value that is re-used, or units for each time series
        List<String>tokens = StringUtil.breakStringList(Units, ",", 0);
        setUnits ( tokens );
        /*
        if ( (tokens.size() != 1) && (tokens.size() != getValueColumnsRuntime().size()) ) {
            message = "The number of units strings (" + tokens.size() + ") is invalid - expecting " +
                getValueColumnsRuntime().size();
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + getValueColumnsRuntime().size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                units.add ( "" );
            }
        }
        else {
            // Process the units for each time series
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    units.add(tokens.get(0));
                }
                else {
                    units.add(tokens.get(i));
                }
            }
        }*/
    }
    /*
    else {
        for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
            units.add ( "" );
        }
    }
    setUnits ( units );
    */
    
    setMissingValue ( new Vector() );
    if ( (MissingValue != null) && !MissingValue.equals("") ) {
        // Can have one or more values that should be interpreted as missing
        List<String>tokens = StringUtil.breakStringList(MissingValue, ",", 0);
        setMissingValue ( tokens );
    }

	// InputStart
	if ((InputStart != null) && !InputStart.equals("")) {
		try {
			__InputStart = DateTime.parse(InputStart);
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
	if ((InputEnd != null) && !InputEnd.equals("")) {
		try {
			__InputEnd = DateTime.parse(InputEnd);
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
	if ( __InputStart != null && __InputEnd != null ) {
		if ( __InputStart.greaterThanOrEqualTo( __InputEnd ) ) {
            message = InputStart + " (" + __InputStart  + ") should be less than InputEnd (" + __InputEnd + ").";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an input start less than the input end." ) );
		}
	}
    
    if (Alias != null && !Alias.equals("")) {
        if (Alias.indexOf(" ") > -1) {
            // do not allow spaces in the alias
            message = "The Alias value cannot contain any spaces.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Remove spaces from the alias." ) );
        }
    }
    
	// Check for invalid parameters...
    List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "TableID" );
    //valid_Vector.add ( "SkipRows" );
    valid_Vector.add ( "DateTimeColumn" );
    valid_Vector.add ( "DateTimeFormat" );
    valid_Vector.add ( "DateColumn" );
    valid_Vector.add ( "TimeColumn" );
    valid_Vector.add ( "LocationTypeColumn" );
    valid_Vector.add ( "LocationColumn" );
    valid_Vector.add ( "DataSourceColumn" );
    valid_Vector.add ( "DataTypeColumn" );
    valid_Vector.add ( "ScenarioColumn" );
    valid_Vector.add ( "UnitsColumn" );
    valid_Vector.add ( "LocationType" );
    valid_Vector.add ( "LocationID" );
    valid_Vector.add ( "ValueColumn" );
    valid_Vector.add ( "FlagColumn" );
    valid_Vector.add ( "DataSource" );
    valid_Vector.add ( "DataType" );
    valid_Vector.add ( "Interval" );
    valid_Vector.add ( "Scenario" );
    valid_Vector.add ( "Units" );
    valid_Vector.add ( "MissingValue" );
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "InputStart" );
    valid_Vector.add ( "InputEnd" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, warning_level ), warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Create a list of metadata for time series for use at runtime considering all of the command parameter
and dynamic input.  For example, this creates the list of data source strings that should be used for each time series.
@param singleColumn indicates whether single column values are being processed
@param nMetadata the number of time series being processed, and therefore the number of metadata values
@param metadataFromTable the list of metadata values determined from the table (one metadata value per location ID).
@param numTS the number of data value sets (columns in multi-column or subsets of single column) being processed,
which is the number of time series.
@param metadataFromParameter the list of metadata from parameter expansion, for example the data sources.  The list can have
a single value, in which case the single value will be used for each time series, or a list of values can be provided, which will
correspond to the time series
*/
private List<String> createMetadataRuntime ( boolean singleColumn,
    List<String> metadataFromTable, int numTS, List<String> metadataFromParameter )
{   List<String> metadataForTS = new Vector<String>();
    // Initialize...
    int nMetadata = numTS;
    for ( int i = 0; i < nMetadata; i++ ) {
        if ( singleColumn && (metadataFromTable.size() > 0) ) {
            // Single column output and data sources were determined from the table
            metadataForTS.add(metadataFromTable.get(i));
        }
        else {
            metadataForTS.add("");
        }
    }
    // Now reset with available
    if ( metadataFromParameter.size() == 1 ) {
        // Single column or user has specified a constant for the parameter
        for ( int i = 0; i < nMetadata; i++ ) {
            metadataForTS.set(i,metadataFromParameter.get(0));
        }
    }
    else if ( metadataFromParameter.size() > 1 ) {
        // Multiple column, transfer as many specified values as have
        for ( int i = 0; i < metadataFromParameter.size(); i++ ) {
            metadataForTS.set(i,metadataFromParameter.get(i));
        }
    }
    return metadataForTS;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	// The command will be modified if changed...
	return ( new TableToTimeSeries_JDialog ( parent, this, tableIDChoices ) ).ok();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
	super.finalize();
}

/**
Format the TSID from the table records, used as the key for the hash map to look up time series.
*/
private String formatTSIDFromTableRecord ( TableRecord rec, int locationTypePos, int locationPos, int dataSourcePos,
    int dataTypePos, TimeInterval interval, int scenarioPos, String locationTypeFromParam, String dataSourceFromParam,
    String dataTypeFromParam, String scenarioFromParam )
throws Exception
{   String locationType = null, locationId = null, dataSource = null, dataType = null, scenario = null;
    StringBuffer tsidFromTable = new StringBuffer();
    locationId = rec.getFieldValueString(locationPos);
    if ( locationId == null ) {
        // Don't have location ID
        return null;
    }
    // Also save the other metadata if a column was specified...
    if ( locationTypePos >= 0 ) {
        locationType = rec.getFieldValueString(locationTypePos);
    }
    else {
        locationType = locationTypeFromParam;
    }
    if ( (locationType != null) && (locationType.length() != 0) ) {
        tsidFromTable.append(locationType);
        tsidFromTable.append(TSIdent.LOC_TYPE_SEPARATOR);
    }
    // Always append the location ID
    tsidFromTable.append(locationId);
    if ( dataSourcePos >= 0 ) {
        dataSource = rec.getFieldValueString(dataSourcePos);
    }
    else {
        dataSource = dataSourceFromParam;
    }
    if ( dataSource == null ) {
        dataSource = "";
    }
    tsidFromTable.append(TSIdent.SEPARATOR);
    tsidFromTable.append(dataSource);
    if ( dataTypePos >= 0 ) {
        dataType = rec.getFieldValueString(dataTypePos);
    }
    else {
        dataType = dataTypeFromParam;
    }
    if ( dataType == null ) {
        dataType = "";
    }
    tsidFromTable.append(TSIdent.SEPARATOR);
    tsidFromTable.append(dataType);
    tsidFromTable.append(TSIdent.SEPARATOR);
    tsidFromTable.append(interval.toString());
    if ( scenarioPos >= 0 ) {
        scenario = rec.getFieldValueString(scenarioPos);
    }
    else {
        scenario = scenarioFromParam;
    }
    if ( (scenario != null) && (scenario.length() > 0) ) {
        tsidFromTable.append(TSIdent.SEPARATOR);
        tsidFromTable.append(scenario);
    }
    return tsidFromTable.toString();
}

/**
Get the column names by handling the TC[] notation.
@param table the table that is being processed
@param columnName0 the initial parameter value that may include TC[] notation
*/
private List<String> getColumnNamesFromNotation ( DataTable table, String columnName0 )
{   String routine = getClass().getName() + ".getColumnNamesFromNotation";
    List<String> columnNames = new Vector();
    List<String> columnHeadingList = Arrays.asList(table.getFieldNames());
    int nColumnHeadings = columnHeadingList.size();
    // Much of the following matches code in the ReadDelimitedFile() command, but here we are processing a table
    // TODO SAM 2012-11-15 Maybe should put this in the table package as a utility to handle slice notation
    if ( StringUtil.startsWithIgnoreCase(columnName0,_TC) ) {
        // Need to process the column names from the file
        int parenPos1 = columnName0.indexOf(_TC);
        int parenPos2 = columnName0.indexOf("]");
        if ( (parenPos1 >= 0) && (parenPos2 >= 0) ) {
            // Need to interpret slice of field numbers in file
            String slice = columnName0.substring((parenPos1 + _TC.length()),parenPos2);
            int [] tableColPos = StringUtil.parseIntegerSlice( slice, ":", 0, nColumnHeadings );
            Message.printStatus(2, routine, "Got " + tableColPos.length + " columns from slice \"" + slice + "\"" );
            for ( int ipos = 0; ipos <tableColPos.length; ipos++ ) {
                // Positions from parameter parsing are 1+ so need to decrement to get 0+ indices
                Message.printStatus(2, routine, "Adding table column name \"" + columnHeadingList.get(tableColPos[ipos] - 1).trim() + "\"" );
                columnNames.add ( columnHeadingList.get(tableColPos[ipos] - 1).trim() );
            }
        }
        else {
            // Use all the file field names
            for ( int ipos = 0; ipos <nColumnHeadings; ipos++ ) {
                Message.printStatus(2, routine, "Adding table column name \"" + columnHeadingList.get(ipos).trim() + "\"" );
                columnNames.add ( columnHeadingList.get(ipos).trim() );
            }
        }
    }
    else {
        // A literal string that can be used as is
        Message.printStatus(2, routine, "Adding user-specified column name \"" + columnName0 + "\"" );
        columnNames.add ( columnName0 );
    }
    return columnNames;
}

/**
Return the runtime column names list.
*/
private List<String> getColumnNamesRuntime()
{
    return __columnNamesRuntime;
}

/**
Return the data source constant.
*/
private List<String> getDataSource()
{
    return __dataSource;
}

/**
Return the data type constant.
*/
private List<String> getDataType()
{
    return __dataType;
}

/**
Return the date column name expanded for runtime.
*/
private String getDateColumnRuntime ()
{
    return __dateColumnRuntime;
}

/**
Return the date/time column name expanded for runtime.
*/
private String getDateTimeColumnRuntime ()
{
    return __dateTimeColumnRuntime;
}

/**
Return the date/time from the record given the date/time column numbers and format.
@return a DateTime object or null if the value cannot be parsed.
@param rec the DataTable record being processed
@param row the row number (1+) for error messages
@param dateTimePos the column number for the date/time, or -1 if not used
@param datePos the column number for the date, or -1 if not used
@param timePos the column number for the time, or -1 if not used
@param dateTime if non-null, use the instance for results, rather than creating a new object - this can be
more efficient when iterating through raw data
@param dateTimeParser parser based on the specified format (if null use default parser)
@param errorMessages if not null, add parse messages to the list
*/
private DateTime getDateTimeFromRecord ( TableRecord rec, int row, int dateTimePos, int datePos, int timePos,
    DateTime dateTime, DateTimeParser dateTimeParser, List<String> errorMessages )
{
    String dateTimeString = null, dateString, timeString;
    Object dateTimeObject, dateObject, timeObject;
    // Determine the date/time...
    if ( dateTimePos >= 0 ) {
        try {
            dateTimeObject = rec.getFieldValue(dateTimePos);
        }
        catch ( Exception e ) {
            // Should not happen
            dateTimeObject = null;
            Message.printWarning(3,"",e);
        }
        if ( dateTimeObject == null ) {
            return null;
        }
        else if ( dateTimeObject instanceof DateTime ) {
            return (DateTime)dateTimeObject;
        }
        else {
            dateTimeString = "" + dateTimeObject; // Could be integer year
        }
    }
    else if ( datePos >= 0 ) {
        try {
            dateObject = rec.getFieldValue(datePos);
        }
        catch ( Exception e ) {
            // Should not happen
            dateObject = null;
            Message.printWarning(3,"",e);
        }
        if ( dateObject == null ) {
            return null;
        }
        else if ( dateObject instanceof DateTime ) {
            return (DateTime)dateObject;
        }
        else {
            dateString = "" + dateObject; // Could be integer year
        }
        if ( timePos >= 0 ) {
            try {
                timeObject = rec.getFieldValue(timePos);
            }
            catch ( Exception e ) {
                // Should not happen
                timeObject = null;
                Message.printWarning(3,"",e);
            }
            if ( timeObject == null ) {
                return null;
            }
            timeString = (String)timeObject;
            dateTimeString = dateString + ":" + timeString;
        }
        else {
            dateTimeString = dateString;
        }
    }
    try {
        if ( dateTimeParser != null ) {
            // Reuse the date/time for performance
            // This is safe because for regular time series only the parts are used
            // and for irregular a copy is made when setting the value
            return dateTimeParser.parse(dateTime,dateTimeString);
        }
        else {
            // Let the parse() method figure out the format
            return DateTime.parse(dateTimeString);
        }
    }
    catch ( Exception dte ) {
        if ( errorMessages != null ) {
            errorMessages.add ( "Error parsing date/time in row " + row + " column " +
                dateTimePos + " \"" + dateTimeString + "\" (" + dte + ")" );
        }
        //++dtErrorCount;
        //if ( dtErrorCount == 1 ) {
        //    // Print one exception to help with troubleshooting
        //    Message.printWarning(3, routine, dte);
        //}
        return null;
    }
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_List;
}

/**
Return the flag column list, expanded for runtime.
*/
private List<String> getFlagColumnsRuntime()
{
    return __flagColumnsRuntime;
}

/**
Return the data interval.
*/
private TimeInterval getInterval()
{
    return __interval;
}

/**
Return the location ID list, expanded for runtime.
*/
private List<String> getLocationIDRuntime()
{
    return __locationIDRuntime;
}

/**
Return the location type list.
*/
private List<String> getLocationType()
{
    return __locationType;
}

/**
Return the missing value string(s).
*/
private List<String> getMissingValue()
{
    return __missingValue;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
    List<TS> discovery_TS_List = getDiscoveryTSList ();
    if ( (discovery_TS_List == null) || (discovery_TS_List.size() == 0) ) {
        return null;
    }
    // Since all time series in table must have the same interval, check the first time series (e.g., MonthTS)
    TS datats = discovery_TS_List.get(0);
    // Also check the base class
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_List;
    }
    else {
        return null;
    }
}

/**
Return the scenario constant.
*/
private List<String> getScenario()
{
    return __scenario;
}

/**
Return the number of rows to skip (ranges).
*/
private int[][] getSkipRows()
{
    return __skipRows;
}

/**
Return the time column name expanded for runtime.
*/
private String getTimeColumnRuntime ()
{
    return __timeColumnRuntime;
}

/**
Return the data units constant.
*/
private List<String> getUnits()
{
    return __units;
}

/**
Return the value column list, expanded for runtime.
*/
private List<String> getValueColumnsRuntime()
{
    return __valueColumnsRuntime;
}

/**
Parse command from text.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{   // First parse
    super.parseCommand(command);
    PropList parameters = getCommandParameters();
    // Replace legacy "Provider" parameter with "DataSource"
    String provider = parameters.getValue("Provider");
    if ( provider != null ) {
        parameters.set("DataSource",provider);
        parameters.unSet("Provider");
    }
}

/**
Read a list of time series from a multiple column data table.
@param table data table to read
@param dateTimeColumn the date/time column name
@param dateTimeFormat the date/time format, if not determined automatically
@param dateColumn the date column name
@param timeColumn the time column name
@param valueColumns the data value column names
@param flagColumn columns that contain flags (corresponding to valueColumns)
@param skipRows ranges of rows (1+ each) that are to be skipped
@param locationTypeColumn the column to use for location type, if single column data table
@param locationColumn the column to use for locations, if single column data table
@param dataSourceColumn the column to use for data sources, if single column data table
@param dataTypeColumn the column to use for data types, if single column data table
@param scenarioColumn the column to use for scenarios, if single column data table
@param unitsColumn the column to use for units, if single column data table
@param locationTypes location types to use for time series instead of default value column names
@param locationIds identifiers to use for time series instead of default value column names
@param dataSources list of data sources (providers) to use for time series
@param dataTypes list of data types to use for time series
@param interval the data interval
@param scenarios list of scenarios to use for time series
@param units data list of data units to use for time series
@param missing list of missing values to use for time series
@param inputStartReq requested start of data (null to return all).
@param inputEndReq requested end of data (null to return all).
@param readData True to read data, false to only read the header information.
@param errorMessages Error message strings to be propagated back to calling code.
*/
private List<TS> readTimeSeriesListMultiple ( DataTable table,
    String dateTimeColumn, String dateTimeFormat,
    String dateColumn, String timeColumn, List<String> valueColumns, List<String> flagColumns,
    int[][] skipRows, String locationTypeColumn, String locationColumn, String dataSourceColumn,
    String dataTypeColumn, String scenarioColumn, String unitsColumn,
    List<String> locationTypes, List<String> locationIds, List<String> dataSources, List<String> dataTypes, TimeInterval interval,
    List<String> scenarios, List<String> units, List<String> missing,
    DateTime inputStartReq, DateTime inputEndReq,
    boolean readData, CommandPhaseType commandPhase, List<String> errorMessages )
throws IOException
{   String routine = getClass().getName() + ".readTimeSeriesList";
    // Allocate the list
    List<TS> tslist = new Vector<TS>();
    // Translate column names to integer values to speed up processing below - these have been expanded for runtime
    // Any operations on table will fail if in discovery mode
    int dateTimePos = -1;
    if ( (dateTimeColumn != null) && !dateTimeColumn.equals("") ) {
        try {
            dateTimePos = table.getFieldIndex(dateTimeColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for date/time column \"" + dateTimeColumn + "\"" );
            }
        }
    }
    DateTimeParser dateTimeParser = null;
    if ( (dateTimeFormat != null) && !dateTimeFormat.trim().equals("") ) {
        // Set to null to simplify logic below
        dateTimeParser = new DateTimeParser ( dateTimeFormat );
    }
    int datePos = -1;
    if ( (dateColumn != null) && !dateColumn.equals("") ) {
        try {
            datePos = table.getFieldIndex(dateColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for date column \"" + dateColumn + "\"" );
            }
        }
    }
    int timePos = -1;
    if ( (timeColumn != null) && !timeColumn.equals("") ) {
        try {
            timePos = table.getFieldIndex(timeColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for time column \"" + timeColumn + "\"" );
            }
        }
    }
    int locationPos = -1;
    if ( (locationColumn != null) && !locationColumn.equals("") ) {
        try {
            locationPos = table.getFieldIndex(locationColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for location column \"" + locationColumn + "\"" );
            }
        }
    }
    Message.printStatus(2, routine, "Reading multi-column table, location column=" + locationPos +
        ", Date/time column=" + dateTimePos + ", date column=" + datePos + ", time column=" + timePos );
    int [] valuePos = new int[0];
    try {
        valuePos = table.getFieldIndices(valueColumns.toArray(new String[valueColumns.size()]));
    }
    catch ( Exception e ) {
        // Use empty array
    }
    int [] flagPos = new int[0];
    try {
        flagPos = table.getFieldIndices(flagColumns.toArray(new String[flagColumns.size()]));
    }
    catch ( Exception e ) {
        // Use empty array
    }
    int locationTypePos = -1;
    int dataSourcePos = -1;
    int dataTypePos = -1;
    int scenarioPos = -1;
    int unitsPos = -1;
    if ( errorMessages.size() > 0 ) {
        // Don't continue if there are errors
        return tslist;
    }
    // Loop through the data records and get the maximum and minimum date/times, as well as the unique
    // locations, needed to initialize the time series.
    // TODO SAM 2012-11-25 If single column data, the date/times should
    // correspond to the unique list of time series, but a performance hit to figure out.
    //Message.printStatus(2,routine,"Table="+table);
    int nRecords = 0;
    if ( readData ) {
        nRecords = table.getNumberOfRecords();
    }
    DateTime dt = null, dtMaxFromTable = null, dtMinFromTable = null;
    TableRecord rec;
    // Loop through the data records and get the period from the data.
    // If single column, also get the unique list of identifiers and other metadata
    String locationIdFromTable, locationFromTablePrev = "";
    Hashtable<String,String> tsidsFromTable = new Hashtable<String,String>();
    Object o;
    int iLoc, nLoc;
    boolean foundLoc = false;
    // Lists of data extracted from time series in the table, used to initialize the time series
    List<String> locationTypesFromTable = new Vector<String>();
    List<String> locationIdsFromTable = new Vector<String>();
    List<String> dataSourcesFromTable = new Vector<String>();
    List<String> dataTypesFromTable = new Vector<String>();
    List<String> scenariosFromTable = new Vector<String>();
    List<String> unitsFromTable = new Vector<String>();
    for ( int iRec = 0; iRec <= nRecords; iRec++ ) {
        try {
            rec = table.getRecord(iRec);
            // Consider whether single field, etc...
            dt = getDateTimeFromRecord(rec,(iRec + 1),dateTimePos,datePos,timePos,null,dateTimeParser,null);
            if ( dt == null ) {
                continue;
            }
            if ( (dtMaxFromTable == null) || dt.greaterThan(dtMaxFromTable) ) {
                dtMaxFromTable = dt;
            }
            if ( (dtMinFromTable == null) || dt.lessThan(dtMinFromTable) ) {
                dtMinFromTable = dt;
            }
        }
        catch ( Exception e ) {
            continue;
        }
    }
    Message.printStatus(2,routine,"Min date/time from table = " + dtMinFromTable +
        ", max date/time from table = " + dtMaxFromTable );
    // Create lists of metadata to initialize each time series
    // Create location types for each time series
    boolean singleColumnFalse = false;
    List<String>locationTypesForTS = createMetadataRuntime ( singleColumnFalse,
        locationTypesFromTable, valueColumns.size(), locationTypes);
    // Create data sources for each time series
    List<String>dataSourcesForTS = createMetadataRuntime ( singleColumnFalse,
        dataSourcesFromTable, valueColumns.size(), dataSources);
    // Create data types for each time series
    // Data types can be specified as single value for all data columns, list of data type strings (number matches number of values),
    // or if not specified, use the value column names.
    List<String> dataTypesForTS = new Vector<String>();
    if ( dataTypes.size() == 0 ) {
        // Use the data value columns
        dataTypesForTS = valueColumns;
    }
    else {
        dataTypesForTS = createMetadataRuntime ( singleColumnFalse, dataTypesFromTable, valueColumns.size(), dataTypes);
    }
    List<String> locationIdsForTS = createMetadataRuntime ( singleColumnFalse, locationIdsFromTable,
        valueColumns.size(), locationIds );
    // Create scenarios for each time series
    List<String>scenariosForTS = createMetadataRuntime ( singleColumnFalse,
        scenariosFromTable, valueColumns.size(), scenarios);
    // Create units for each time series
    List<String>unitsForTS = createMetadataRuntime ( singleColumnFalse, unitsFromTable,
        valueColumns.size(), units );
    Message.printStatus(2,routine,"Sizes:" +
        " locationTypesForTS=" + locationTypesForTS.size() +
        " locationIdsForTS=" + locationIdsForTS.size() +
        " dataTypesForTS=" + dataTypesForTS.size() +
        " scenariosForTS=" + scenariosForTS.size() +
        " dataSourcesForTS=" + dataSourcesForTS.size() +
        " unitsForTS=" + unitsForTS.size());
    // Create the time series.  If single column, the count corresponds to the individual location identifiers.
    // If multiple column, the count corresponds to the number of value columns
    int nTS = 0;
    nTS = valuePos.length;
    int valuePosForTS;
    String valueColumnForTS;
    for ( int its = 0; its < nTS; its++ ) {
        TSIdent tsident = null;
        TS ts = null;
        String tsidentstr = null;
        String locType = null;
        String scenario = null;
        if ( locationTypesForTS.get(its).equals("") ) {
            locType = "";
        }
        else {
            locType = locationTypesForTS.get(its) + TSIdent.LOC_TYPE_SEPARATOR;
        }
        if ( scenariosForTS.get(its).equals("") ) {
            scenario = "";
        }
        else {
            scenario = TSIdent.SEPARATOR + scenariosForTS.get(its);
        }
        tsidentstr = locType + locationIdsForTS.get(its) + "." + dataSourcesForTS.get(its) + "." +
            dataTypesForTS.get(its) + "." + interval + scenario;
        Message.printStatus(2, routine, "Creating time series for TSID=\"" + tsidentstr + "\", units=\"" +
            unitsForTS.get(its) + "\"" );
        valueColumnForTS = valueColumns.get(its);
        valuePosForTS = valuePos[its];
        if ( valuePosForTS < 0 ) {
            // Was a problem looking up column numbers
            errorMessages.add ( "Value column name \"" +
                valueColumnForTS + "\" does not match known data columns - will not read.");
        }
        else {
            try {
                tsident = new TSIdent( tsidentstr );
            }
            catch ( Exception e ) {
                tsident = null;
                errorMessages.add ( "Error initializing time series \"" + tsidentstr +
                    "\" (" + e + ") - will not read.");
                Message.printWarning(3, routine, e);
            }
            if ( tsident != null ) {
                try {
                    ts = TSUtil.newTimeSeries( tsident.toString(), true );
                    // Set all the information
                    ts.setIdentifier ( tsident );
                    ts.setDescription ( locationIdsForTS.get(its) + " " + dataTypesForTS.get(its) );
                    ts.setDataUnits ( unitsForTS.get(its) );
                    ts.setDataUnitsOriginal ( unitsForTS.get(its) );
                    ts.setMissing ( Double.NaN );
                    ts.setInputName ( table.getTableID() );
                    if ( inputStartReq != null ) {
                        ts.setDate1(inputStartReq);
                    }
                    else {
                        ts.setDate1(dtMinFromTable);
                    }
                    if ( inputEndReq != null ) {
                        ts.setDate2(inputEndReq);
                    }
                    else {
                        ts.setDate2(dtMaxFromTable);
                    }
                    ts.setDate1Original(dtMinFromTable);
                    ts.setDate2Original(dtMaxFromTable);
                }
                catch ( Exception e ) {
                    // Set the TS to null to match the column positions but won't be able to set data below
                    ts = null;
                    errorMessages.add ( "Error initializing time series \"" +
                        tsidentstr + "\" (" + e + ") - will not read.");
                    Message.printWarning(3,routine,e);
                }
            }
        }
        // Don't add if null
        if ( ts != null ) {
            tslist.add ( ts );
        }
    }
    if ( !readData ) {
        return tslist;
    }
    // Process the data records.
    // Date-time is reused for multiple locations
    Double value = null;
    String flag = null;
    int flagColumnPos;
    TS ts;
    // All time series will have the same period since each time series shows up in every row
    for ( int its = 0; its < tslist.size(); its++ ) {
        ts = tslist.get(its);
        ts.allocateDataSpace();
    }
    for ( int iRec = 0; iRec <= nRecords; iRec++ ) {
        try {
            rec = table.getRecord(iRec);
            dt = getDateTimeFromRecord(rec,(iRec + 1),dateTimePos,datePos,timePos,null,dateTimeParser,null);
            if ( dt == null ) {
                continue;
            }
            // Loop through the values, taken from 1+ columns in the row
            for ( int ival = 0; ival < valuePos.length; ival++ ) {
                // Get the value...
                o = rec.getFieldValue(valuePos[ival]);
                if ( o == null ) {
                    continue;
                }
                else if ( o instanceof Double ) {
                    value = (Double)o;
                }
                else if ( o instanceof Float ) {
                    value = ((Float)o).doubleValue();
                }
                else if ( o instanceof Integer ) {
                    value = ((Integer)o).doubleValue();
                }
                else {
                    continue;
                }
                // Get the the data flag
                flag = null;
                flagColumnPos = -1;
                if ( flagPos.length > ival ) {
                    flagColumnPos = flagPos[ival];
                }
                if ( flagColumnPos >= 0 ) {
                    o = rec.getFieldValue(flagColumnPos); 
                    if ( o != null ) {
                        flag = "" + o;
                    }
                }
                // Get the time series, which will be in the order of the values, since the same order
                // was used to initialize the time series above
                ts = tslist.get(ival);
                // Set the value and flag in the time series
                if ( (flag != null) && !flag.equals("") ) {
                    ts.setDataValue(dt, value, flag, -1);
                }
                else {
                    ts.setDataValue(dt, value);
                }
            }
        }
        catch ( Exception e ) {
            // Skip the record
            continue;
        }
    }
    return tslist;
}

/**
Read a list of time series from a single column data table.
@param table data table to read
@param dateTimeColumn the date/time column name
@param dateTimeFormat the date/time format, if not determined automatically
@param dateColumn the date column name
@param timeColumn the time column name
@param valueColumn the data value column name
@param flagColumn column that contains flags (corresponding to valueColumn)
@param skipRows ranges of rows (1+ each) that are to be skipped
@param locationTypeColumn the column to use for location type, if single column data table
@param locationColumn the column to use for locations, if single column data table
@param dataSourceColumn the column to use for data sources, if single column data table
@param dataTypeColumn the column to use for data types, if single column data table
@param scenarioColumn the column to use for scenarios, if single column data table
@param unitsColumn the column to use for units, if single column data table
@param locationType location type to use for time series
@param dataSource data source (provider) to use for time series
@param dataType data type to use for time series
@param interval the data interval
@param scenario scenario to use for time series
@param units data units to use for time series
@param missing list of missing values in table to set to missing in time series (uses NaN in time series)
@param inputStartReq requested start of data (null to return all).
@param inputEndReq requested end of data (null to return all).
@param readData True to read data, false to only read the header information.
@param errorMessages Error message strings to be propagated back to calling code.
*/
private List<TS> readTimeSeriesListSingle ( DataTable table,
    String dateTimeColumn, String dateTimeFormat,
    String dateColumn, String timeColumn, String valueColumn, String flagColumn,
    int[][] skipRows, String locationTypeColumn, String locationColumn, String dataSourceColumn,
    String dataTypeColumn, String scenarioColumn, String unitsColumn, String locationType,
    String dataSource, String dataType, TimeInterval interval,
    String scenario, String units, List<String> missing,
    DateTime inputStartReq, DateTime inputEndReq,
    boolean readData, CommandPhaseType commandPhase, List<String> errorMessages )
throws IOException
{   String routine = getClass().getName() + ".readTimeSeriesList";
    // Allocate the list
    List<TS> tslist = new Vector<TS>();
    // Translate column names to integer values to speed up processing below - these have been expanded for runtime
    // Any operations on table will fail if in discovery mode
    int dateTimePos = -1;
    if ( (dateTimeColumn != null) && !dateTimeColumn.equals("") ) {
        try {
            dateTimePos = table.getFieldIndex(dateTimeColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for date/time column \"" + dateTimeColumn + "\"" );
            }
        }
    }
    DateTimeParser dateTimeParser = null;
    if ( (dateTimeFormat != null) && !dateTimeFormat.trim().equals("") ) {
        // Set to null to simplify logic below
        dateTimeParser = new DateTimeParser ( dateTimeFormat );
    }
    int datePos = -1;
    if ( (dateColumn != null) && !dateColumn.equals("") ) {
        try {
            datePos = table.getFieldIndex(dateColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for date column \"" + dateColumn + "\"" );
            }
        }
    }
    int timePos = -1;
    if ( (timeColumn != null) && !timeColumn.equals("") ) {
        try {
            timePos = table.getFieldIndex(timeColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for time column \"" + timeColumn + "\"" );
            }
        }
    }
    int locationPos = -1;
    if ( (locationColumn != null) && !locationColumn.equals("") ) {
        try {
            locationPos = table.getFieldIndex(locationColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for location column \"" + locationColumn + "\"" );
            }
        }
    }
    Message.printStatus(2, routine, "Single column, location column=" + locationPos + ", Date/time column=" +
        dateTimePos + ", date column=" + datePos + ", time column=" + timePos );
    int valuePos = -1;
    try {
        valuePos = table.getFieldIndex(valueColumn);
    }
    catch ( Exception e ) {
        if ( commandPhase == CommandPhaseType.RUN ) {
            errorMessages.add("Cannot determine column number for value column \"" + valueColumn + "\"" );
        }
    }
    int flagPos = -1;
    if ( (flagColumn != null) && !flagColumn.equals("") ) {
        try {
            flagPos = table.getFieldIndex(flagColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for flag column \"" + flagColumn + "\"" );
            }
        }
    }
    int locationTypePos = -1;
    int dataSourcePos = -1;
    int dataTypePos = -1;
    int scenarioPos = -1;
    int unitsPos = -1;
    // Determine column positions for metadata columns
    if ( (locationTypeColumn != null) && !locationTypeColumn.equals("") ) {
        try {
            locationTypePos = table.getFieldIndex(locationTypeColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for location type column \"" + locationTypeColumn + "\"" );
            }
        }
    }
    if ( (dataSourceColumn != null) && !dataSourceColumn.equals("") ) {
        try {
            dataSourcePos = table.getFieldIndex(dataSourceColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for data source column \"" + dataSourceColumn + "\"" );
            }
        }
    }
    if ( (dataTypeColumn != null) && !dataTypeColumn.equals("") ) {
        try {
            dataTypePos = table.getFieldIndex(dataTypeColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for data type column \"" + dataTypeColumn + "\"" );
            }
        }
    }
    if ( (scenarioColumn != null) && !scenarioColumn.equals("") ) {
        try {
            scenarioPos = table.getFieldIndex(scenarioColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for scenario column \"" + scenarioColumn + "\"" );
            }
        }
    }
    if ( (unitsColumn != null) && !unitsColumn.equals("") ) {
        try {
            unitsPos = table.getFieldIndex(unitsColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for units column \"" + unitsColumn + "\"" );
            }
        }
    }
    if ( errorMessages.size() > 0 ) {
        // Don't continue if there are errors
        return tslist;
    }
    // Loop through the data records and get the maximum and minimum date/times, as well as the unique
    // TSID combinations, needed to initialize the time series.
    int nRecords = 0;
    if ( readData ) {
        nRecords = table.getNumberOfRecords();
    }
    DateTime dt = null, dtMaxFromTable = null, dtMinFromTable = null;
    TableRecord rec;
    // Loop through the data records and get the period from the data and unique TSIDs
    // A hashmap is used to track the TSIDs, where the key is the TSID string and the object is initially the same
    // TSID string but will be set to the TS when initialized
    String unitsFromTable;
    LinkedHashMap<String,String> tsidsFromTable = new LinkedHashMap<String,String>();
    List<String> unitsFromTableList = new Vector<String>();
    Object o;
    String tsidFromTable;
    for ( int iRec = 0; iRec <= nRecords; iRec++ ) {
        try {
            rec = table.getRecord(iRec);
            // Consider whether single field, etc...
            dt = getDateTimeFromRecord(rec,(iRec + 1),dateTimePos,datePos,timePos,null,dateTimeParser,null);
            if ( dt == null ) {
                continue;
            }
            if ( (dtMaxFromTable == null) || dt.greaterThan(dtMaxFromTable) ) {
                dtMaxFromTable = dt;
            }
            if ( (dtMinFromTable == null) || dt.lessThan(dtMinFromTable) ) {
                dtMinFromTable = dt;
            }
            // Determine the location identifier from the data.
            try {
                tsidFromTable = formatTSIDFromTableRecord ( rec, locationTypePos, locationPos, dataSourcePos,
                    dataTypePos, interval, scenarioPos, locationType, dataSource, dataType, scenario );
            }
            catch ( Exception e ) {
                // Should not happen
                continue;
            }
            if ( tsidFromTable == null ) {
                // No location in record
                continue;
            }
            // Add to the hashtable if not found...
            if ( tsidsFromTable.get(tsidFromTable) == null ) {
                tsidsFromTable.put(tsidFromTable.toString(), tsidFromTable.toString());
                // Also save the units in the same order
                if ( unitsPos >= 0 ) {
                    unitsFromTable = rec.getFieldValueString(unitsPos);
                    if ( unitsFromTable == null ) {
                        unitsFromTable = "";
                    }
                    unitsFromTableList.add(unitsFromTable);
                }
                else {
                    unitsFromTableList.add(units);
                }
            }
        }
        catch ( Exception e ) {
            continue;
        }
    }
    Message.printStatus(2,routine,"Min date/time from table = " + dtMinFromTable +
        ", max date/time from table = " + dtMaxFromTable );
    Message.printStatus(2, routine,
        "Number of time series identifiers from single-column data table = " + tsidsFromTable.size() );
    // Create the time series.
    TSIdent tsident = null;
    TS ts = null;
    String tsidentstr = null;
    int its = -1; // Used to iterate through units, parallel to TSID hashmap
    for ( Map.Entry<String,String> tsid: tsidsFromTable.entrySet() ) {
        ++its;
        tsidentstr = tsid.getKey();
        Message.printStatus(2, routine, "Creating time series for TSID=\"" + tsidentstr + "\", units=\"" +
            unitsFromTableList.get(its) + "\"" );
        try {
            tsident = new TSIdent( tsidentstr );
        }
        catch ( Exception e ) {
            tsident = null;
            errorMessages.add ( "Error initializing time series \"" + tsidentstr + "\" (" + e + ") - will not read.");
            Message.printWarning(3, routine, e);
        }
        if ( tsident != null ) {
            try {
                ts = TSUtil.newTimeSeries( tsident.toString(), true );
                // Set all the information
                ts.setIdentifier ( tsident );
                ts.setDescription ( tsident.getLocation() + " " + tsident.getType() );
                ts.setDataUnits ( unitsFromTableList.get(its) );
                ts.setDataUnitsOriginal ( unitsFromTableList.get(its) );
                ts.setMissing ( Double.NaN );
                ts.setInputName ( table.getTableID() );
                if ( inputStartReq != null ) {
                    ts.setDate1(inputStartReq);
                }
                else {
                    ts.setDate1(dtMinFromTable);
                }
                if ( inputEndReq != null ) {
                    ts.setDate2(inputEndReq);
                }
                else {
                    ts.setDate2(dtMaxFromTable);
                }
                ts.setDate1Original(dtMinFromTable);
                ts.setDate2Original(dtMaxFromTable);
            }
            catch ( Exception e ) {
                // Set the TS to null to match the column positions but won't be able to set data below
                ts = null;
                errorMessages.add ( "Error initializing time series \"" +
                    tsidentstr + "\" (" + e + ") - will not read.");
                Message.printWarning(3,routine,e);
            }
        }
        // Don't add if null
        if ( ts != null ) {
            tslist.add ( ts );
        }
    }
    if ( !readData ) {
        return tslist;
    }
    // Process the data records.
    // Create an ordered hash map to simplify lookup of time series based on the TSID information
    LinkedHashMap<String,TS> tsHash = new LinkedHashMap<String,TS>();
    its = -1;
    for ( Map.Entry<String,String> tsid: tsidsFromTable.entrySet() ) {
        ++its;
        tsidentstr = tsid.getKey();
        ts = tslist.get(its);
        ts.allocateDataSpace();
        tsHash.put(tsidentstr,ts);
    }
    String tsidFromTablePrev = "";
    Double value = null;
    String flag;
    for ( int iRec = 0; iRec <= nRecords; iRec++ ) {
        try {
            rec = table.getRecord(iRec);
            dt = getDateTimeFromRecord(rec,(iRec + 1),dateTimePos,datePos,timePos,null,dateTimeParser,null);
            if ( dt == null ) {
                continue;
            }
            // Get the value...
            o = rec.getFieldValue(valuePos);
            if ( o == null ) {
                continue;
            }
            else if ( o instanceof Double ) {
                value = (Double)o;
            }
            else if ( o instanceof Float ) {
                value = ((Float)o).doubleValue();
            }
            else if ( o instanceof Integer ) {
                value = ((Integer)o).doubleValue();
            }
            else {
                continue;
            }
            // Get the TSID string for the table record, used to look up the time series
            try {
                tsidFromTable = formatTSIDFromTableRecord ( rec, locationTypePos, locationPos, dataSourcePos,
                    dataTypePos, interval, scenarioPos, locationType, dataSource, dataType, scenario );
            }
            catch ( Exception e ) {
                // Should not happen - don't process the table record
                continue;
            }
            // Get the time series to process, may be the same as for the previous record, in which
            // case a hash table lookup is not needed.
            if ( !tsidFromTable.equals(tsidFromTablePrev) ) {
                ts = tsHash.get(tsidFromTable);
            }
            tsidFromTablePrev = tsidFromTable;
            // Get the the data flag
            flag = null;
            if ( flagPos >= 0 ) {
                o = rec.getFieldValue(flagPos); 
                if ( o != null ) {
                    flag = "" + o;
                }
            }
            // Set the value and flag in the time series
            if ( (flag != null) && !flag.equals("") ) {
                ts.setDataValue(dt, value, flag, -1);
            }
            else {
                ts.setDataValue(dt, value);
            }
        }
        catch ( Exception e ) {
            // Skip the record
            continue;
        }
    }
    return tslist;
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
{	String routine = "TableToTimeSeries_Command.runCommand", message;
	int warning_level = 2;
    int log_level = 3;
	String command_tag = "" + command_number;
	int warning_count = 0;
	    
    // Get and clear the status and clear the run log...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    CommandProcessor processor = getCommandProcessor();

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String TableID = parameters.getValue("TableID");
	String LocationType = parameters.getValue("LocationType");
	String LocationID = parameters.getValue("LocationID");
	String LocationTypeColumn = parameters.getValue("LocationTypeColumn");
	String LocationColumn = parameters.getValue("LocationColumn");
	String DataSourceColumn = parameters.getValue("DataSourceColumn");
	String DataTypeColumn = parameters.getValue("DataTypeColumn");
	String ScenarioColumn = parameters.getValue("ScenarioColumn");
	String UnitsColumn = parameters.getValue("UnitsColumn");
	String DateTimeColumn = parameters.getValue("DateTimeColumn");
	String DateTimeFormat = parameters.getValue("DateTimeFormat");
	String DateColumn = parameters.getValue("DateColumn");
	String TimeColumn = parameters.getValue("TimeColumn");
	String ValueColumn = parameters.getValue("ValueColumn");
	String FlagColumn = parameters.getValue("FlagColumn");
	String Alias = parameters.getValue("Alias");
	String InputStart = parameters.getValue("InputStart");
    String InputEnd = parameters.getValue("InputEnd");
    
    // Get the table to process.

    DataTable table = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Processor does not have full tables in discovery, only IDs so create a table here
        table = new DataTable();
        table.setTableID(TableID);
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        PropList request_params = null;
        CommandProcessorRequestResultsBean bean = null;
        if ( (TableID != null) && !TableID.equals("") ) {
            // Get the table to be updated
            request_params = new PropList ( "" );
            request_params.set ( "TableID", TableID );
            try {
                bean = processor.processRequest( "GetTable", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_Table = bean_PropList.getContents ( "Table" );
            if ( o_Table == null ) {
                message = "Unable to find table to process using TableID=\"" + TableID + "\".";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a table exists with the requested ID." ) );
            }
            else {
                // Table to be read below
                table = (DataTable)o_Table;
                // TODO SAM 2012-11-15 Evaluate doing this in discovery mode in checkCommandParameters() but
                // for now figure out the information here and set for use below.
                if ( (LocationID != null) && !LocationID.equals("") ) {
                    if ( StringUtil.indexOfIgnoreCase(LocationID,_TC, 0) >= 0 ) {
                        List<String> cols = getColumnNamesFromNotation ( table, LocationID );
                        setLocationIDRuntime(cols);
                    }
                    else if ( LocationID.indexOf(",") > 0 ) {
                        // A list of identifiers has been specified
                        List<String> ids = StringUtil.breakStringList(LocationID, ",", 0);
                        setLocationIDRuntime(ids);
                    }
                    else {
                        // A single identifier has been specified (will be reused)
                        List<String> ids = new Vector<String>();
                        ids.add(LocationID);
                        setLocationIDRuntime(ids);
                    }
                }
                if ( (DateTimeColumn != null) && !DateTimeColumn.equals("") &&
                    StringUtil.indexOfIgnoreCase(DateTimeColumn,_TC, 0) >= 0 ) {
                    List<String> cols = getColumnNamesFromNotation ( table, DateTimeColumn );
                    // Date/time column is single value
                    if ( cols.size() > 0 ) {
                        setDateTimeColumnRuntime(cols.get(0));
                    }
                }
                if ( (DateColumn != null) && !DateColumn.equals("") &&
                    StringUtil.indexOfIgnoreCase(DateColumn,_TC, 0) >= 0 ) {
                    List<String> cols = getColumnNamesFromNotation ( table, DateColumn );
                    // Date column is single value
                    if ( cols.size() > 0 ) {
                        setDateColumnRuntime(cols.get(0));
                    }
                }
                if ( (TimeColumn != null) && !TimeColumn.equals("") &&
                    StringUtil.indexOfIgnoreCase(TimeColumn,_TC, 0) >= 0 ) {
                    List<String> cols = getColumnNamesFromNotation ( table, TimeColumn );
                    // Time column is single value
                    if ( cols.size() > 0 ) {
                        setTimeColumnRuntime(cols.get(0));
                    }
                }
                if ( (ValueColumn != null) && !ValueColumn.equals("") &&
                    StringUtil.indexOfIgnoreCase(ValueColumn,_TC, 0) >= 0 ) {
                    List<String> cols = getColumnNamesFromNotation ( table, ValueColumn );
                    setValueColumnsRuntime(cols);
                }
                if ( (FlagColumn != null) && !FlagColumn.equals("") &&
                    StringUtil.indexOfIgnoreCase(FlagColumn,_TC, 0) >= 0 ) {
                    List<String> cols = getColumnNamesFromNotation ( table, FlagColumn );
                    setFlagColumnsRuntime(cols);
                }
            }
        }
    }
    
    DateTime InputStart_DateTime = null;
    DateTime InputEnd_DateTime = null;
    if ( (InputStart != null) && (InputStart.length() != 0) ) {
        try {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", InputStart );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting InputStart DateTime(DateTime=" + InputStart + ") from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }
    
            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for InputStart DateTime(DateTime=" + InputStart + ") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the specified date/time is valid." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {  InputStart_DateTime = (DateTime)prop_contents;
            }
        }
        catch ( Exception e ) {
            message = "InputStart \"" + InputStart + "\" is invalid.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time for the input start, " +
                            "or InputStart for the global input start." ) );
            throw new InvalidCommandParameterException ( message );
        }
    }
    else {
        // Get the global input start from the processor...
        try {
            Object o = processor.getPropContents ( "InputStart" );
            if ( o != null ) {
                InputStart_DateTime = (DateTime)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting the global InputStart from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
            throw new InvalidCommandParameterException ( message );
        }
    }
    
    if ( (InputEnd != null) && (InputEnd.length() != 0) ) {
        try {
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", InputEnd );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "DateTime", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting InputEnd DateTime(DateTime=" + InputEnd + ") from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
                throw new InvalidCommandParameterException ( message );
            }

            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( prop_contents == null ) {
                message = "Null value for InputEnd DateTime(DateTime=" + InputEnd +  ") returned from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the end date/time is valid." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {  InputEnd_DateTime = (DateTime)prop_contents;
            }
        }
        catch ( Exception e ) {
            message = "InputEnd \"" + InputEnd + "\" is invalid.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time for the input end, " +
                            "or InputEnd for the global input start." ) );
            throw new InvalidCommandParameterException ( message );
        }
    }
    else {
        // Get from the processor...
        try {
            Object o = processor.getPropContents ( "InputEnd" );
            if ( o != null ) {
                InputEnd_DateTime = (DateTime)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting the global InputEnd from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report problem to software support." ) );
        }
    }

    List<TS> tslist = null; // List of time series that is read
	try {
        boolean readData = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            readData = false;
        }
        
        // Read everything in the file (one time series or traces).
        List<String> errorMessages = new Vector();
        // Read the time series
        // If the location column has been specified then the table is single-column data
        boolean singleColumn = false;
        if ( (LocationColumn != null) && !LocationColumn.equals("") ) {
            singleColumn = true;
        }
        if ( singleColumn ) {
            tslist = readTimeSeriesListSingle ( table, getDateTimeColumnRuntime(),
                DateTimeFormat, getDateColumnRuntime(),
                getTimeColumnRuntime(), ValueColumn, FlagColumn, getSkipRows(),
                LocationTypeColumn, LocationColumn, DataSourceColumn, DataTypeColumn, ScenarioColumn, UnitsColumn,
                (getLocationType().size() == 1 ? getLocationType().get(0) : null),
                (getDataSource().size() == 1 ? getDataSource().get(0) : null),
                (getDataType().size() == 1 ? getDataType().get(0) : null), getInterval(),
                (getScenario().size() == 1 ? getScenario().get(0) : null),
                (getUnits().size() == 1 ? getUnits().get(0) : null), getMissingValue(),
                InputStart_DateTime, InputEnd_DateTime, readData, commandPhase, errorMessages );
        }
        else {
            tslist = readTimeSeriesListMultiple ( table, getDateTimeColumnRuntime(),
                DateTimeFormat, getDateColumnRuntime(),
                getTimeColumnRuntime(), getValueColumnsRuntime(), getFlagColumnsRuntime(), getSkipRows(),
                LocationTypeColumn, LocationColumn, DataSourceColumn, DataTypeColumn, ScenarioColumn, UnitsColumn,
                getLocationType(), getLocationIDRuntime(), getDataSource(), getDataType(), getInterval(), getScenario(),
                getUnits(), getMissingValue(),
                InputStart_DateTime, InputEnd_DateTime, readData, commandPhase, errorMessages );
        }
        
		if ( tslist != null ) {
			int tscount = tslist.size();
			message = "Read " + tscount + " time series from table \"" + TableID + "\"";
			Message.printStatus ( 2, routine, message );
	        if ( (Alias != null) && (Alias.length() > 0) ) {
	            for ( int i = 0; i < tscount; i++ ) {
	                TS ts = tslist.get(i);
	                if ( ts == null ) {
	                    continue;
	                }
                    // Set the alias to the desired string.
                    ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString(
	                     processor, ts, Alias, status, commandPhase) );
	            }
	        }
		}
		
		// Add warnings...
		
		for ( String errorMessage: errorMessages ) {
		    Message.printWarning ( warning_level,
	            MessageUtil.formatMessageTag(command_tag, ++warning_count ),
	            routine, errorMessage );
	        status.addToLog(commandPhase,
	            new CommandLogRecord( CommandStatusType.WARNING, errorMessage,
	                "Verify the file format and command parameters."));
		}
	} 
	catch ( Exception e ) {
		message = "Unexpected error reading table \"" + TableID + "\" (" + e + ")";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count ),
			routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(commandPhase,
            new CommandLogRecord( CommandStatusType.FAILURE, message,"Check the log file for details."));
		throw new CommandException ( message );
	}

    if ( commandPhase == CommandPhaseType.RUN ) {
        if ( tslist != null ) {
            // Further process the time series...
            // This makes sure the period is at least as long as the output period...
            int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
            if ( wc > 0 ) {
                message = "Error post-processing series after read.";
                Message.printWarning ( warning_level, MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
    
            // Now add the list in the processor...
            
            int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
            if ( wc2 > 0 ) {
                message = "Error adding time series after read.";
                Message.printWarning ( warning_level, MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
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
Set the data type strings for each time series.
*/
private void setDataType ( List<String> dataType )
{
    __dataType = dataType;
}

/**
Set date column expanded for runtime.
*/
private void setDateColumnRuntime ( String dateColumnRuntime )
{
    __dateColumnRuntime = dateColumnRuntime;
}

/**
Set the data source strings for each time series.
*/
private void setDataSource ( List<String> dataSource )
{
    __dataSource = dataSource;
}

/**
Set date/time column expanded for runtime.
*/
private void setDateTimeColumnRuntime ( String dateTimeColumnRuntime )
{
    __dateTimeColumnRuntime = dateTimeColumnRuntime;
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List discovery_TS_List )
{
    __discovery_TS_List = discovery_TS_List;
}

/**
Set the flag column names for each time series, expanded for runtime.
*/
private void setFlagColumnsRuntime ( List<String> flagColumnsRuntime )
{
    __flagColumnsRuntime = flagColumnsRuntime;
}

/**
Set the data interval for the time series.
*/
private void setInterval ( TimeInterval interval )
{
    __interval = interval;
}

/**
Set the location ID strings for each time series, expanded for runtime.
*/
private void setLocationIDRuntime ( List<String> locationIDRuntime )
{
    __locationIDRuntime = locationIDRuntime;
}

/**
Set the location type strings for each time series.
*/
private void setLocationType ( List<String> locationType )
{
    __locationType = locationType;
}

/**
Set the missing value strings.
*/
private void setMissingValue ( List<String> missingValue )
{
    __missingValue = missingValue;
}

/**
Set the scenario strings for each time series.
*/
private void setScenario ( List<String> scenario )
{
    __scenario = scenario;
}

/**
Set the rows to skip (integer ranges).
*/
private void setSkipRows ( int[][] skipRows )
{
    __skipRows = skipRows;
}

/**
Set time column expanded for runtime.
*/
private void setTimeColumnRuntime ( String timeColumnRuntime )
{
    __timeColumnRuntime = timeColumnRuntime;
}

/**
Set the data units strings for each time series.
*/
private void setUnits ( List<String> units )
{
    __units = units;
}

/**
Set the value column names for each time series, expanded for runtime.
*/
private void setValueColumnsRuntime ( List<String> valueColumnsRuntime )
{
    __valueColumnsRuntime = valueColumnsRuntime;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

	String TableID = props.getValue("TableID" );
    String DateTimeColumn = props.getValue("DateTimeColumn" );
    String DateTimeFormat = props.getValue("DateTimeFormat" );
    String DateColumn = props.getValue("DateColumn" );
    String TimeColumn = props.getValue("TimeColumn" );
    String LocationColumn = props.getValue("LocationColumn" );
    String DataSourceColumn = props.getValue("DataSourceColumn" );
    String DataTypeColumn = props.getValue("DataTypeColumn" );
    String ScenarioColumn = props.getValue("ScenarioColumn" );
    String UnitsColumn = props.getValue("UnitsColumn" );
    String LocationType = props.getValue("LocationType" );
    String LocationID = props.getValue("LocationID" );
    String ValueColumn = props.getValue("ValueColumn" );
    String FlagColumn = props.getValue("FlagColumn" );
    //String SkipRows = props.getValue("SkipRows" );
    String Interval = props.getValue("Interval" );
    String DataSource = props.getValue("DataSource" );
    String DataType = props.getValue("DataType" );
    String Scenario = props.getValue("Scenario" );
    String Units = props.getValue("Units" );
    String MissingValue = props.getValue("MissingValue" );
    String Alias = props.getValue("Alias" );
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");

	StringBuffer b = new StringBuffer ();

	// Input File
	if ((TableID != null) && (TableID.length() > 0)) {
		b.append("TableID=\"" + TableID + "\"");
	}
    if ((DateTimeColumn != null) && (DateTimeColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DateTimeColumn=\"" + DateTimeColumn + "\"");
    }
    if ((DateTimeFormat != null) && (DateTimeFormat.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DateTimeFormat=\"" + DateTimeFormat + "\"");
    }
    if ((DateColumn != null) && (DateColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DateColumn=\"" + DateColumn + "\"");
    }
    if ((TimeColumn != null) && (TimeColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("TimeColumn=\"" + TimeColumn + "\"");
    }
    if ((LocationColumn != null) && (LocationColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("LocationColumn=\"" + LocationColumn + "\"");
    }
    if ((DataSourceColumn != null) && (DataSourceColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DataSourceColumn=\"" + DataSourceColumn + "\"");
    }
    if ((DataTypeColumn != null) && (DataTypeColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DataTypeColumn=\"" + DataTypeColumn + "\"");
    }
    if ((ScenarioColumn != null) && (ScenarioColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("ScenarioColumn=\"" + ScenarioColumn + "\"");
    }
    if ((UnitsColumn != null) && (UnitsColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("UnitsColumn=\"" + UnitsColumn + "\"");
    }
    if ((LocationID != null) && (LocationID.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("LocationID=\"" + LocationID + "\"");
    }
    if ((LocationType != null) && (LocationType.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("LocationType=\"" + LocationType + "\"");
    }
    if ((ValueColumn != null) && (ValueColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("ValueColumn=\"" + ValueColumn + "\"");
    }
    if ((FlagColumn != null) && (FlagColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("FlagColumn=\"" + FlagColumn + "\"");
    }
    /**
    if ((SkipRows != null) && (SkipRows.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("SkipRows=\"" + SkipRows + "\"");
    }*/
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
    if ((Interval != null) && (Interval.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Interval=" + Interval );
    }
    if ((Scenario != null) && (Scenario.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Scenario=\"" + Scenario + "\"");
    }
	if ((Units != null) && (Units.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("Units=\"" + Units + "\"");
	}
    if ((MissingValue != null) && (MissingValue.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("MissingValue=" + MissingValue );
    }
    if ((Alias != null) && (Alias.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Alias=\"" + Alias + "\"");
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

	return getCommandName() + "(" + b.toString() + ")";
}

}