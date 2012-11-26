package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
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
private List<String> __columnNamesRuntime = new Vector();

/**
Data types for each time series being processed.
*/
private List<String> __dataType = new Vector();

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
Location ID for each time series being processed, expanded for runtime.
*/
private List<String> __locationIDRuntime = new Vector<String>();

/**
Missing value strings that may be present in the file.
*/
private List<String> __missingValue = new Vector<String>();

/**
Provider for each time series being processed.
*/
private List<String> __provider = new Vector<String>();

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
    String LocationColumn = parameters.getValue("LocationColumn" );
    String LocationID = parameters.getValue("LocationID" );
    String Provider = parameters.getValue("Provider" );
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
    
    List<String> valueColumns = new Vector();
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
    
    List<String> flagColumns = new Vector();
    List<String> flagColumnsRuntime = new Vector();
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

    List<String> locationIDRuntime = new Vector();
    setLocationIDRuntime( locationIDRuntime );
    setLocationColumnRuntime( "" );
    if ( ((LocationID == null) || LocationID.equals("")) &&
        ((LocationColumn == null) || LocationColumn.equals("")) ) {
        message = "The location ID column(s) must be specified for multi-column data tables OR " +
            "the location column must be specified for single-column data tables.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify 1+ location ID column(s) values separated by commas or a single location column." ) );
    }
    if ( (LocationColumn != null) && !LocationColumn.equals("") ) {
        // TODO SAM 2012-11-12 Need to check whether column exists
        setLocationColumnRuntime( LocationColumn );
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

    List<String> provider = new Vector();
    setProvider ( provider );
    if ( (Provider != null) && !Provider.equals("") ) {
        // Can have one value that is re-used, or Provider for each time series
        List<String>tokens = StringUtil.breakStringList(Provider, ",", 0);
        setProvider(tokens);
        /*
        if ( (tokens.size() != 1) && (tokens.size() != getValueColumnsRuntime().size()) ) {
            message = "The number of Provider strings (" + tokens.size() + ") is invalid - expecting " +
                getValueColumnsRuntime().size();
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + getValueColumnsRuntime().size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                provider.add ( "" );
            }
        }
        else {
            // Process the Provider for each time series
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    provider.add(tokens.get(0));
                }
                else {
                    provider.add(tokens.get(i));
                }
            }
        }*/
    }
    /*
    else {
        for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
            provider.add ( "" );
        }
    }
    setProvider ( provider );
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
    
    List<String> scenario = new Vector();
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
    List<String> valid_Vector = new Vector();
    valid_Vector.add ( "TableID" );
    //valid_Vector.add ( "SkipRows" );
    valid_Vector.add ( "DateTimeColumn" );
    valid_Vector.add ( "DateTimeFormat" );
    valid_Vector.add ( "DateColumn" );
    valid_Vector.add ( "TimeColumn" );
    valid_Vector.add ( "LocationColumn" );
    valid_Vector.add ( "LocationID" );
    valid_Vector.add ( "ValueColumn" );
    valid_Vector.add ( "FlagColumn" );
    valid_Vector.add ( "Provider" );
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
Create a list of data types for each time series for use at runtime.
*/
private List<String> createDataTypesRuntime ( boolean singleColumn, List<String> locationIdsFromTable,
    List<String>valueColumns, List<String> dataTypes )
{   List<String> dataTypesForTS = new Vector<String>();
    int nDataTypes = 0;
    // If user has specified data types, use.  If not, use value columns
    if ( dataTypes.size() == 0 ) {
        dataTypes = valueColumns;
    }
    if ( singleColumn ) {
        nDataTypes = locationIdsFromTable.size();
    }
    else {
        nDataTypes = valueColumns.size();
    }
    // Initialize blank...
    for ( int i = 0; i < nDataTypes; i++ ) {
        dataTypesForTS.add("");
    }
    // Now reset with available
    if ( dataTypes.size() == 1 ) {
        // Single column or user has specified only one data type
        for ( int i = 0; i < nDataTypes; i++ ) {
            dataTypesForTS.set(i,dataTypes.get(0));
        }
    }
    else if ( dataTypes.size() > 1 ) {
        // Multiple column, transfer as many specified values as have
        for ( int i = 0; i < nDataTypes; i++ ) {
            dataTypesForTS.set(i,dataTypes.get(i));
        }
    }
    return dataTypesForTS;
}

/**
Create a list of time series location identifiers based on command parameters.
This method takes into account whether a single data column or multiple data column table is used.
@param locationPos location column number, when using single column data
@param locationIdsFromTable location identifiers determined from single column data
@param locationIds user-specified location identifiers, when using multiple column data
*/
private List<String> createLocationIdsRuntime ( boolean singleColumn, List<String> locationIdsFromTable,
    List<String>locationIds )
{
    if ( singleColumn ) {
        // Using single column data table, so return the list of identifiers read from the table
        return locationIdsFromTable;
    }
    else {
        // Location identifiers were provided by user
        return locationIds;
    }
}

/**
Create a list of providers for time series for use at runtime.
*/
private List<String> createProvidersRuntime ( boolean singleColumn, List<String> locationIdsFromTable,
    int nDataTypes, List<String> providers )
{   List<String> providersForTS = new Vector();
    int nProviders = 0;
    if ( singleColumn ) {
        nProviders = locationIdsFromTable.size();
    }
    else {
        nProviders = nDataTypes;
    }
    // Initialize blank...
    for ( int i = 0; i < nProviders; i++ ) {
        providersForTS.add("");
    }
    // Now reset with available
    if ( providers.size() == 1 ) {
        // Single column or user has specified only one provider
        for ( int i = 0; i < nProviders; i++ ) {
            providersForTS.set(i,providers.get(0));
        }
    }
    else if ( providers.size() > 1 ) {
        // Multiple column, transfer as many specified values as have
        for ( int i = 0; i < providers.size(); i++ ) {
            providersForTS.set(i,providers.get(i));
        }
    }
    return providersForTS;
}

/**
Create a list of scenarios for time series for use at runtime.
*/
private List<String> createScenariosRuntime ( boolean singleColumn, List<String> locationIdsFromTable,
    int nDataTypes, List<String> scenarios )
{   List<String> scenariosForTS = new Vector();
    int nScenarios = 0;
    if ( singleColumn ) {
        nScenarios = locationIdsFromTable.size();
    }
    else {
        nScenarios = nDataTypes;
    }
    // Initialize blank...
    for ( int i = 0; i < nScenarios; i++ ) {
        scenariosForTS.add("");
    }
    // Now reset with available
    if ( scenarios.size() == 1 ) {
        // Single column or user has specified only one provider
        for ( int i = 0; i < nScenarios; i++ ) {
            scenariosForTS.set(i,scenarios.get(0));
        }
    }
    else if ( scenarios.size() > 1 ) {
        // Multiple column, transfer as many specified values as have
        for ( int i = 0; i < scenarios.size(); i++ ) {
            scenariosForTS.set(i,scenarios.get(i));
        }
    }
    return scenariosForTS;
}

/**
Create a list of units for time series for use at runtime.
*/
private List<String> createUnitsRuntime ( boolean singleColumn, List<String> locationIdsFromTable,
    int nDataTypes, List<String> units )
{   List<String> unitsForTS = new Vector();
    int nUnits = 0;
    if ( singleColumn ) {
        nUnits = locationIdsFromTable.size();
    }
    else {
        nUnits = nDataTypes;
    }
    // Initialize blank...
    for ( int i = 0; i < nUnits; i++ ) {
        unitsForTS.add("");
    }
    // Now reset with available
    if ( units.size() == 1 ) {
        // Single column or user has specified only one provider
        for ( int i = 0; i < nUnits; i++ ) {
            unitsForTS.set(i,units.get(0));
        }
    }
    else if ( units.size() > 1 ) {
        // Multiple column, transfer as many specified units as have
        for ( int i = 0; i < units.size(); i++ ) {
            unitsForTS.set(i,units.get(i));
        }
    }
    return unitsForTS;
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
                Message.printStatus(2, routine, "Adding file column name \"" + columnHeadingList.get(tableColPos[ipos] - 1).trim() + "\"" );
                columnNames.add ( columnHeadingList.get(tableColPos[ipos] - 1).trim() );
            }
        }
        else {
            // Use all the file field names
            for ( int ipos = 0; ipos <nColumnHeadings; ipos++ ) {
                Message.printStatus(2, routine, "Adding file column name \"" + columnHeadingList.get(ipos).trim() + "\"" );
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
Return the data type list.
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
        else if ( dateTimeObject instanceof String ) {
            dateTimeString = (String)dateTimeObject;
        }
        else if ( dateTimeObject instanceof DateTime ) {
            return (DateTime)dateTimeObject;
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
        dateString = (String)dateObject;
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
Return the location column, expanded for runtime.
*/
private String getLocationColumnRuntime()
{
    return __locationColumnRuntime;
}

/**
Return the location ID list, expanded for runtime.
*/
private List<String> getLocationIDRuntime()
{
    return __locationIDRuntime;
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
Return the provider list.
*/
private List<String> getProvider()
{
    return __provider;
}

/**
Return the scenario list.
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
Return the data units list.
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

// Use the base class parseCommand() method

/**
Read a list of time series from a table.
@param table data table to read
@param dateTimeColumn the date/time column name
@param dateTimeFormat the date/time format, if not determined automatically
@param dateColumn the date column name
@param timeColumn the time column name
@param valueColumns the data value column names
@param flagColumn columns that contain flags (corresponding to valueColumns)
@param skipRows ranges of rows (1+ each) that are to be skipped
@param locationColumn the column to use for locations, if single column data table
@param locationIds identifiers to use for time series instead of default value column names
@param providers list of providers (data sources) to use for time series
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
private List<TS> readTimeSeriesList ( DataTable table,
    String dateTimeColumn, String dateTimeFormat,
    String dateColumn, String timeColumn, List<String> valueColumns, List<String> flagColumns,
    int[][] skipRows, String locationColumn,
    List<String> locationIds, List<String> providers, List<String> datatypes, TimeInterval interval,
    List<String> scenarios, List<String> units, List<String> missing,
    DateTime inputStartReq, DateTime inputEndReq,
    boolean readData, List<String> errorMessages )
throws IOException
{   String routine = getClass().getName() + ".readTimeSeriesList";
    // Allocate the list
    List<TS> tslist = new Vector<TS>();
    // Translate column names to integer values to speed up processing below - these have been expanded for runtime
    // Any operations on table will fail if in discovery mode
    int dateTimePos = -1;
    try {
        dateTimePos = table.getFieldIndex(dateTimeColumn);
    }
    catch ( Exception e ) {
        // Use -1 from above
    }
    DateTimeParser dateTimeParser = null;
    if ( (dateTimeFormat != null) && !dateTimeFormat.trim().equals("") ) {
        // Set to null to simplify logic below
        dateTimeParser = new DateTimeParser ( dateTimeFormat );
    }
    int datePos = -1;
    try {
        datePos = table.getFieldIndex(dateColumn);
    }
    catch ( Exception e ) {
        // Use -1 from above
    }
    int timePos = -1;
    try {
        timePos = table.getFieldIndex(timeColumn);
    }
    catch ( Exception e ) {
        // Use -1 from above
    }
    // If the location column has been specified then the table is single-column data
    boolean singleColumn = false;
    int locationPos = -1;
    if ( (locationColumn != null) && !locationColumn.equals("") ) {
        singleColumn = true;
        try {
            locationPos = table.getFieldIndex(locationColumn);
        }
        catch ( Exception e ) {
            // Use -1 from above
        }
    }
    Message.printStatus(2, routine, "Single column=" + singleColumn + ", location column=" +
        locationPos + ", Date/time column=" + dateTimePos +
        ", date column=" + datePos + ", time column=" + timePos );
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
    // Loop through the data records and get the maximum and minimum date/times, as well as the unique
    // locations, needed to initialize the time series
    Message.printStatus(2,routine,"Table="+table);
    int nRecords = 0;
    if ( readData ) {
        nRecords = table.getNumberOfRecords();
    }
    DateTime dt = null, dtMaxFromTable = null, dtMinFromTable = null;
    TableRecord rec;
    // Loop through the data records and get the period from the data.
    // If single column, also get the unique list of identifiers
    String locationIdFromTable, locationFromTablePrev = "";
    Object o;
    int iLoc, nLoc;
    boolean foundLoc = false;
    List<String> locationIdsFromTable = new Vector<String>();
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
            if ( singleColumn ) {
                // Determine the location identifiers from the data.
                o = rec.getFieldValue(locationPos);
                if ( o != null ) {
                    locationIdFromTable = "" + o; // Sometimes data tables with numeric identifiers parse as numbers
                    if ( !locationIdFromTable.equals(locationFromTablePrev) ) {
                        // Different from previous row so candidate for addition.  This will only help with
                        // performance if a sequential block of records is for one time series.
                        nLoc = locationIdsFromTable.size();
                        foundLoc = false;
                        for ( iLoc = 0; iLoc < nLoc; iLoc++ ) {
                            if ( locationIdsFromTable.get(iLoc).equalsIgnoreCase(locationIdFromTable) ) {
                                foundLoc = true;
                                break;
                            }
                        }
                        if ( !foundLoc ) {
                            locationIdsFromTable.add(locationIdFromTable);
                        }
                    }
                    locationFromTablePrev = locationIdFromTable;
                }
            }
        }
        catch ( Exception e ) {
            continue;
        }
    }
    Message.printStatus(2,routine,"Min date/time from table = " + dtMinFromTable +
        ", max date/time from table = " + dtMaxFromTable );
    if ( singleColumn ) {
        Message.printStatus(2, routine,
            "Number of location identifiers from single-column data table = " + locationIdsFromTable.size() );
    }
    // Create lists of metadata to initialize each time series
    // Create data types for each time series
    List<String>dataTypesForTS = createDataTypesRuntime ( singleColumn, locationIdsFromTable,
        valueColumns, datatypes);
    List<String> locationIdsForTS = createLocationIdsRuntime ( singleColumn, locationIdsFromTable, locationIds );
    // Create providers for each time series
    List<String>providersForTS = createProvidersRuntime ( singleColumn, locationIdsFromTable,
        valueColumns.size(), providers);
    // Create scenarios for each time series
    List<String>scenariosForTS = createScenariosRuntime ( singleColumn, locationIdsFromTable,
        valueColumns.size(), scenarios);
    // Create units for each time series
    for ( int i = 0; i < units.size(); i++ ) {
        Message.printStatus(2, routine, "Units[" + i + "] = " + units.get(i) );
    }
    List<String>unitsForTS = createUnitsRuntime ( singleColumn, locationIdsFromTable,
        valueColumns.size(), units );
    Message.printStatus(2,routine,"Sizes: locationIdsForTS=" + locationIdsForTS.size() +
        " dataTypesForTS=" + dataTypesForTS.size() +
        " scenariosForTS=" + scenariosForTS.size() +
        " providersForTS=" + providersForTS.size() +
        " unitsForTS=" + unitsForTS.size());
    // Create the time series.  If single column, the count corresponds to the individual location identifiers.
    // If multiple column, the count corresponds to the number of value columns
    int nTS = 0;
    if ( singleColumn ) {
        nTS = locationIdsForTS.size();
    }
    else {
        nTS = valuePos.length;
    }
    int valuePosForTS;
    String valueColumnForTS;
    for ( int its = 0; its < nTS; its++ ) {
        TSIdent tsident = null;
        TS ts = null;
        String tsidentstr = locationIdsForTS.get(its) + "." + providersForTS.get(its) + "." +
            dataTypesForTS.get(its) + "." + interval + "." + scenariosForTS.get(its);
        Message.printStatus(2, routine, "Creating time series for TSID=\"" + tsidentstr + "\", units=\"" +
            unitsForTS.get(its) + "\"" );
        if ( singleColumn ) {
            valueColumnForTS = valueColumns.get(0);
            valuePosForTS = valuePos[0];
        }
        else {
            valueColumnForTS = valueColumns.get(its);
            valuePosForTS = valuePos[its];
        }
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
    if ( singleColumn ) {
        // Create a hashtable to simplify lookup of time series based on the location ID
        Hashtable<String,TS> hash = new Hashtable();
        TS ts = null;
        for ( int its = 0; its < tslist.size(); its++ ) {
            ts = tslist.get(its);
            ts.allocateDataSpace();
            hash.put(ts.getIdentifier().getLocation(),ts);
        }
        String locationIdFromTablePrev = "";
        Double value = null;
        for ( int iRec = 0; iRec <= nRecords; iRec++ ) {
            try {
                rec = table.getRecord(iRec);
                dt = getDateTimeFromRecord(rec,(iRec + 1),dateTimePos,datePos,timePos,null,dateTimeParser,null);
                if ( dt == null ) {
                    continue;
                }
                // Get the value...
                o = rec.getFieldValue(valuePos[0]);
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
                // Get the location, which indicates the time series in which to set data
                o = rec.getFieldValue(locationPos);
                if ( o == null ) {
                    continue;
                }
                locationIdFromTable = "" + o;
                // Get the time series to process, may be the same as for the previous record, in which
                // case a hash table lookup is not needed.
                if ( !locationIdFromTable.equals(locationIdFromTablePrev) ) {
                    ts = hash.get(locationIdFromTable);
                }
                ts.setDataValue(dt, value);
                // TODO SAM 2012-11-11 Need to handle data flag
            }
            catch ( Exception e ) {
                // Skip the record
                continue;
            }
        }
    }
    else {
        // Multi-column data
        // Date-time is reused for multiple locations
        Double value = null;
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
                    // Get the time series, which will be in the order of the values, since the same order
                    // was used to initialize the time series above
                    ts = tslist.get(ival);
                    ts.setDataValue(dt, value);
                    // TODO SAM 2012-11-11 Need to handle data flag
                }
            }
            catch ( Exception e ) {
                // Skip the record
                continue;
            }
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
	String LocationID = parameters.getValue("LocationID");
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
                if ( (LocationID != null) && !LocationID.equals("") &&
                    StringUtil.indexOfIgnoreCase(LocationID,_TC, 0) >= 0 ) {
                    List<String> cols = getColumnNamesFromNotation ( table, LocationID );
                    setLocationIDRuntime(cols);
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
        tslist = readTimeSeriesList ( table, getDateTimeColumnRuntime(),
            DateTimeFormat, getDateColumnRuntime(),
            getTimeColumnRuntime(), getValueColumnsRuntime(), getFlagColumnsRuntime(),
            getSkipRows(), getLocationColumnRuntime(), getLocationIDRuntime(), getProvider(),
            getDataType(), getInterval(), getScenario(),
            getUnits(), getMissingValue(),
            InputStart_DateTime, InputEnd_DateTime, readData, errorMessages );
        
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
Set the location column name, expanded for runtime.
*/
private void setLocationColumnRuntime ( String locationColumnRuntime )
{
    __locationColumnRuntime = locationColumnRuntime;
}

/**
Set the location ID strings for each time series, expanded for runtime.
*/
private void setLocationIDRuntime ( List<String> locationIDRuntime )
{
    __locationIDRuntime = locationIDRuntime;
}

/**
Set the missing value strings.
*/
private void setMissingValue ( List<String> missingValue )
{
    __missingValue = missingValue;
}

/**
Set the provider strings for each time series.
*/
private void setProvider ( List<String> provider )
{
    __provider = provider;
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
    String LocationID = props.getValue("LocationID" );
    String ValueColumn = props.getValue("ValueColumn" );
    String FlagColumn = props.getValue("FlagColumn" );
    //String SkipRows = props.getValue("SkipRows" );
    String Interval = props.getValue("Interval" );
    String Provider = props.getValue("Provider" );
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
    if ((LocationID != null) && (LocationID.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("LocationID=\"" + LocationID + "\"");
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
    if ((Provider != null) && (Provider.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Provider=\"" + Provider + "\"");
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