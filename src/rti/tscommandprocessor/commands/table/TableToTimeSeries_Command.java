// TableToTimeSeries_Command - This class initializes, checks, and runs the TableToTimeSeries() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import RTi.Util.Time.YearType;

/**
This class initializes, checks, and runs the TableToTimeSeries() command.
*/
public class TableToTimeSeries_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Choices for HandleDuplicatesHow parameter.
*/
protected final String _Add = "Add";
protected final String _UseFirstNonmissing = "UseFirstNonmissing";
protected final String _UseLast = "UseLast";
protected final String _UseLastNonmissing = "UseLastNonmissing";

/**
Values for BlockLayout parameter.
*/
protected final String _Period = "Period";
protected final String _Year = "Year";

/**
String that indicates that column names should be taken from the table.
For example TC[1:] indicates columns 1 through the total number of columns.
*/
protected final String _TC = "TC[";

/**
Column names for each time series being processed,
from the ColumnNames parameter that has been expanded to reflect file column names.
See also __columnNamesRuntime.
*/
private List<String> __columnNamesRuntime = new ArrayList<>();

/**
Data types for each time series being processed.
*/
private List<String> __dataType = new ArrayList<>();

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
List of time series read during discovery.  These are TS objects but with mainly the metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_List = null;

/**
Interval for the time series.
*/
private TimeInterval __interval = null;

/**
Location column for each time series being processed, expanded for runtime.
*/
//private String __locationColumnRuntime = null;

/**
Location Type for each time series being processed, expanded for runtime.
*/
//private List<String> __locationTypeRuntime = new ArrayList<>();

/**
Location type for each time series being processed.
*/
private List<String> __locationType = new ArrayList<>();

/**
Location ID for each time series being processed, expanded for runtime.
*/
private List<String> __locationIDRuntime = new ArrayList<>();

/**
Missing value strings that may be present in the file.
*/
private List<String> __missingValue = new ArrayList<>();

/**
Data source for each time series being processed.
*/
private List<String> __dataSource = new ArrayList<>();

/**
Scenario for each time series being processed.
*/
private List<String> __scenario = new ArrayList<>();

/**
Sequence ID for each time series being processed.
*/
private List<String> __sequenceID = new ArrayList<>();

/**
TODO SAM 2012-11-09 Currently not used
Row ranges to skip (single rows will have same and start value.  Rows are 1+.
*/
private int[][] __skipRows = null; // Allocated when checking parameters.

/**
Data units for each time series being processed.
*/
private List<String> __units = new ArrayList<>();

/**
Precision for each time series being processed.
*/
private List<String> __precision = new ArrayList<>();

/**
Column names for data values, for each time series being processed, expanded for runtime.
*/
private List<String> __valueColumnsRuntime = new ArrayList<>();

/**
Column names for data flags, for each time series being processed, expanded for runtime.
*/
private List<String> __flagColumnsRuntime = new ArrayList<>();

/**
Constructor.
*/
public TableToTimeSeries_Command () {
	super();
	setCommandName ( "TableToTimeSeries" );
}

// TODO SAM 2012-11-15 Change so checks examine column names - after discovery mode is updated to be aware of column names.
// For now only handle TC[] notation at runtime in the runCommandInternal method.
// Commented-out code below illustrates checks that could be done in discovery mode.
/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
    String routine = getClass().getSimpleName() + ".checkCommandParameters";
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
    String SequenceIDColumn = parameters.getValue("SequenceIDColumn" );
    String UnitsColumn = parameters.getValue("UnitsColumn" );
    String DataSource = parameters.getValue("DataSource" );
    String DataType = parameters.getValue("DataType" );
    String Interval = parameters.getValue("Interval" );
    String IrregularIntervalPrecision = parameters.getValue("IrregularIntervalPrecision" );
    String Scenario = parameters.getValue("Scenario" );
    String SequenceID = parameters.getValue("SequenceID" );
    String Units = parameters.getValue("Units" );
    String Precision = parameters.getValue("Precision" );
    String MissingValue = parameters.getValue("MissingValue" );
    String HandleDuplicatesHow = parameters.getValue("HandleDuplicatesHow" );
    String Alias = parameters.getValue("Alias" );
	String BlockOutputYearType = parameters.getValue ( "BlockOutputYearType" );
	String InputStart = parameters.getValue("InputStart");
	String InputEnd = parameters.getValue("InputEnd");

    if ( (TableID == null) || TableID.isEmpty() ) {
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
    // If TimeColumn is specified, then DateTimeColumn must not be specified and DateColumn must be specified.
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
            // Original string used slice notation for column name.
            try {
                List<String> dateTimeColumnName = new ArrayList<>();
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
            // Just use the column names as specified for runtime.
            dateTimeColumnRuntime = DateTimeColumn;
        }
        setDateTimeColumnRuntime ( dateTimeColumnRuntime );
        // Now check the value of the date/time column versus the available columns.
        /* TODO SAM 2012-11-12 Need to enable something.
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
            // Original string used slice notation for column.
            try {
                List<String> dateColumnName = new ArrayList<>();
                dateColumnName.add ( DateColumn ); // Only one.
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
            // Just use the column names as specified for runtime.
            dateColumnRuntime = DateColumn;
        }
        setDateColumnRuntime ( dateColumnRuntime );
        // Now check the value of the date column versus the available columns.
        /* TODO SAM 2012-11-12 Need to enable something.
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
            // Original string used slice notation for column name.
            try {
                List<String> timeColumnName = new ArrayList<>();
                timeColumnName.add ( TimeColumn ); // Only one.
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
            // Just use the column names as specified for runtime.
            timeColumnRuntime = TimeColumn;
        }
        setTimeColumnRuntime ( timeColumnRuntime );
        // Now check the value of the time column versus the available columns.
        /* TODO SAM 2012-11-12 Need to enable something.
        if ( getColumnNumberFromName(getTimeColumnRuntime(), getColumnNamesRuntime()) < 0 ) {
            message = "The TimeColumn (" + getTimeColumnRuntime() + ") is not a recognized column name.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a time column as one of the values from ColumnNames." ) );
        }
        */
    }

    List<String> valueColumns = new ArrayList<>();
    List<String> valueColumnsRuntime = new ArrayList<>();
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
            // Original string used slice notation for column name.
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
            // Just use the column names as specified for runtime.
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
        // Now check for valid column names.
        /* TODO SAM 2012-11-12 Need to enable something.
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

    List<String> flagColumns = new ArrayList<>();
    List<String> flagColumnsRuntime = new ArrayList<>();
    setFlagColumnsRuntime ( flagColumnsRuntime );
    if ( (FlagColumn != null) && (FlagColumn.length() != 0) ) {
        flagColumns = StringUtil.breakStringList(FlagColumn, ",", StringUtil.DELIM_ALLOW_STRINGS );
        if ( StringUtil.indexOfIgnoreCase(FlagColumn,_TC, 0) >= 0 ) {
            // Original string used slice notation for column name.
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
            // Just use the column names as specified for runtime.
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
        // Now check for valid column names.
        //for ( String flagColumnRuntime : flagColumnsRuntime ) {
            /* TODO SAM 2012-11-12 Need to enable something
            if ( getColumnNumberFromName(flagColumnRuntime, getFlagColumnsRuntime()) < 0 ) {
                message = "The FlagColumn (" + flagColumnRuntime + ") is not a recognized column name.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify flag column(s) matching ColumnNames, separated by commas." ) );
            }
            */
        //}
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

    List<String> locationIDRuntime = new ArrayList<>();
    setLocationIDRuntime( locationIDRuntime );
    if ( ((LocationID == null) || LocationID.isEmpty()) &&
        ((LocationColumn == null) || LocationColumn.isEmpty()) ) {
        message = "The location ID column(s) must be specified for multi-column data tables OR " +
            "the location column must be specified for single-column data tables.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify 1+ location ID column(s) values separated by commas or a single location column." ) );
    }
    if ( (LocationID != null) && !LocationID.isEmpty() && (LocationID.indexOf("${") < 0) ) {
        // Can have one value that is re-used, or LocationID for each time series.
        List<String>tokens = StringUtil.breakStringList(LocationID, ",", 0);
        if ( StringUtil.indexOfIgnoreCase(LocationID,_TC, 0) >= 0 ) {
            // Original string used slice notation for column name.
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
            // Set to empty strings to simplify runtime error handling.
            for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
                locationIDRuntime.add ( "" );
            }
        }
        else {
            // Process the LocationID for each time series.
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

    if ( ((LocationID != null) && !LocationID.isEmpty()) &&
        ((LocationColumn != null) && !LocationColumn.isEmpty()) ) {
        // Can only specify location one way.
        message = "LocationID and LocationColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify LocationID or LocationColumn but not both." ) );
    }

    if ( ((LocationType != null) && !LocationType.equals("")) &&
        ((LocationTypeColumn != null) && !LocationTypeColumn.equals("")) ) {
        // Can only specify location type one way.
        message = "LocationType and LocationTypeColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify LocationType or LocationTypeColumn but not both." ) );
    }

    if ( ((DataSource != null) && !DataSource.equals("")) &&
        ((DataSourceColumn != null) && !DataSourceColumn.equals("")) ) {
        // Can only specify data source one way.
        message = "DataSource and DataSourceColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify DataSource or DataSourceColumn but not both." ) );
    }

    if ( ((DataType != null) && !DataType.equals("")) &&
        ((DataTypeColumn != null) && !DataTypeColumn.equals("")) ) {
        // Can only specify data type one way.
        message = "DataType and DataTypeColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify DataType or DataTypeColumn but not both." ) );
    }

    if ( ((Scenario != null) && !Scenario.equals("")) &&
        ((ScenarioColumn != null) && !ScenarioColumn.equals("")) ) {
        // Can only specify scenario one way.
        message = "Scenario and ScenarioColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify Scenario or ScenarioColumn but not both." ) );
    }

    if ( ((SequenceID != null) && !SequenceID.equals("")) &&
        ((SequenceIDColumn != null) && !SequenceIDColumn.equals("")) ) {
        // Can only specify sequence ID one way.
        message = "SequenceID and SequenceIDColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify SequenceID or SequenceIDColumn but not both." ) );
    }

    if ( ((Units != null) && !Units.equals("")) &&
        ((UnitsColumn != null) && !UnitsColumn.equals("")) ) {
        // Can only specify units one way.
        message = "Units and UnitsColumn cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify Units or UnitsColumn but not both." ) );
    }

    List<String> locationType = new ArrayList<>();
    setLocationType ( locationType );
    if ( (LocationType != null) && !LocationType.equals("") ) {
        // Can have one value that is re-used, or LocationType for each time series.
        List<String>tokens = StringUtil.breakStringList(LocationType, ",", 0);
        setLocationType(tokens);
    }

    List<String> dataSource = new ArrayList<>();
    setDataSource ( dataSource );
    if ( (DataSource != null) && !DataSource.equals("") ) {
        // Can have one value that is re-used, or DataSource for each time series.
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
            // Process the DataSource for each time series.
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

    List<String> dataType = new ArrayList<>();
    setDataType ( dataType );
    if ( (DataType == null) || DataType.equals("") ) {
        // Set to the same as the value columns.
        //for ( int i = 0; i < getValueColumnsRuntime().size(); i++ ) {
        //    dataType.add ( getValueColumnsRuntime().get(i) );
        //}
    }
    else {
        // Can have one value that is re-used, or DataType for each time series.
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
            // Process the DataType for each time series.
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
            message = "The time series data interval (" + Interval + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid data interval using the command editor." ) );
        }
    }
    setInterval ( interval );

    if ( (IrregularIntervalPrecision != null) && !IrregularIntervalPrecision.isEmpty() ) {
        try {
            TimeInterval irregPrecision = TimeInterval.parseInterval(IrregularIntervalPrecision);
            // Set the precision on the main interval since the command currently has that split into a separate parameter:
            // - interval was parsed in 'checkCommandParameters'
            if ( (this.__interval != null) && this.__interval.isIrregularInterval() ) {
            	interval.setIrregularIntervalPrecision(irregPrecision.getIrregularIntervalPrecision());
            	Message.printStatus(2, routine, "Set the irregular interval precision to " + this.__interval.getIrregularIntervalPrecision());
            }
        }
        catch ( Exception e ) {
            message = "The irregular interval precision (" + IrregularIntervalPrecision + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid irregular interval precision using the command editor." ) );
        }
    }

    List<String> scenario = new ArrayList<>();
    setScenario ( scenario );
    if ( (Scenario != null) && !Scenario.equals("") ) {
        // Can have one value that is re-used, or Scenario for each time series.
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
            // Process the Scenario for each time series.
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

    List<String> sequenceID = new ArrayList<>();
    setSequenceID ( sequenceID );
    if ( (SequenceID != null) && !SequenceID.equals("") ) {
        // Can have one value that is re-used, or Scenario for each time series.
        List<String>tokens = StringUtil.breakStringList(SequenceID, ",", 0);
        setSequenceID ( tokens );
    }

    List<String> units = new ArrayList<>();
    setUnits ( units );
    if ( (Units != null) && !Units.equals("") ) {
        // Can have one value that is re-used, or units for each time series.
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
            // Process the units for each time series.
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

    setPrecision ( new ArrayList<>() );
    if ( (Precision != null) && !Precision.equals("") ) {
        // Can have one or more values that should be interpreted as the precision.
        List<String> tokens = StringUtil.breakStringList(Precision, ",", 0);
        for ( String token : tokens ) {
        	token = token.trim();
        	if ( token.isEmpty() || !StringUtil.isInteger(token) ) {
        		message = "The Precision value \"" + token + "\" is not valid.";
        		warning += "\n" + message;
        		status.addToLog ( CommandPhaseType.INITIALIZATION,
            		new CommandLogRecord(CommandStatusType.FAILURE,
                		message, "Specify a precision value as an integer." ) );
        	}
        }
        setPrecision ( tokens );
    }

    setMissingValue ( new ArrayList<>() );
    if ( (MissingValue != null) && !MissingValue.equals("") ) {
        // Can have one or more values that should be interpreted as missing.
        List<String> tokens = StringUtil.breakStringList(MissingValue, ",", 0);
        setMissingValue ( tokens );
    }
    if ( (HandleDuplicatesHow != null) && !HandleDuplicatesHow.equals("") &&
        !HandleDuplicatesHow.equalsIgnoreCase(_Add) && !HandleDuplicatesHow.equalsIgnoreCase(_UseFirstNonmissing) &&
        !HandleDuplicatesHow.equalsIgnoreCase(_UseLast) && !HandleDuplicatesHow.equalsIgnoreCase(_UseLastNonmissing)) {
        message = "The HandleDuplicatesHow value \"" + HandleDuplicatesHow + "\" is not valid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify HandleDuplicatesHow as " + _Add + ", " + _UseFirstNonmissing + ", " +
                " or " + _UseLast + " (default), or " + _UseLastNonmissing + "." ) );
    }

	YearType outputYearType = null;
	if ( (BlockOutputYearType != null) && !BlockOutputYearType.isEmpty() ) {
        try {
            outputYearType = YearType.valueOfIgnoreCase(BlockOutputYearType);
        }
        catch ( Exception e ) {
        	outputYearType = null;
        }
        if ( outputYearType == null ) {
            message = "The block output year type (" + BlockOutputYearType + ") is invalid.";
            warning += "\n" + message;
            StringBuffer b = new StringBuffer();
            List<YearType> values = YearType.getYearTypeChoices();
            for ( YearType t : values ) {
                if ( b.length() > 0 ) {
                    b.append ( ", " );
                }
                b.append ( t.toString() );
            }
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Valid values are:  " + b.toString() + "."));
        }
	}

	// InputStart
    DateTime inputStart = null;
    DateTime inputEnd = null;
	if ((InputStart != null) && !InputStart.isEmpty() && !InputStart.startsWith("${") ) {
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

	// Make sure __InputStart precedes __InputEnd.
	if ( inputStart != null && inputEnd != null ) {
		if ( inputStart.greaterThanOrEqualTo( inputEnd ) ) {
            message = InputStart + " (" + InputStart  + ") should be less than InputEnd (" + InputEnd + ").";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an input start less than the input end." ) );
		}
	}

    if (Alias != null && !Alias.equals("")) {
        if (Alias.indexOf(" ") > -1) {
            // do not allow spaces in the alias.
            message = "The Alias value cannot contain any spaces.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Remove spaces from the alias." ) );
        }
    }

	// Check for invalid parameters.
    List<String> validList = new ArrayList<>(28);
    validList.add ( "TableID" );
    //validList.add ( "SkipRows" );
    validList.add ( "DateTimeColumn" );
    validList.add ( "DateTimeFormat" );
    validList.add ( "DateColumn" );
    validList.add ( "TimeColumn" );
    validList.add ( "LocationTypeColumn" );
    validList.add ( "LocationColumn" );
    validList.add ( "DataSourceColumn" );
    validList.add ( "DataTypeColumn" );
    validList.add ( "ScenarioColumn" );
    validList.add ( "SequenceIDColumn" );
    validList.add ( "UnitsColumn" );
    validList.add ( "LocationType" );
    validList.add ( "LocationID" );
    validList.add ( "ValueColumn" );
    validList.add ( "FlagColumn" );
    validList.add ( "DataSource" );
    validList.add ( "DataType" );
    validList.add ( "Interval" );
    validList.add ( "IrregularIntervalPrecision" );
    validList.add ( "Scenario" );
    validList.add ( "SequenceID" );
    validList.add ( "Units" );
    validList.add ( "Precision" );
    validList.add ( "MissingValue" );
    validList.add ( "HandleDuplicatesHow" );
    validList.add ( "Alias" );
	validList.add ( "BlockLayout" );
	validList.add ( "BlockLayoutColumns" );
	validList.add ( "BlockLayoutRows" );
	validList.add ( "BlockOutputYearType" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
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
Create a list of metadata for time series for use at runtime considering all of the command parameter and dynamic input.
For example, this creates the list of data source strings that should be used for each time series.
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
    List<String> metadataFromTable, int numTS, List<String> metadataFromParameter ) {
    List<String> metadataForTS = new ArrayList<>();
    // Initialize.
    int nMetadata = numTS;
    for ( int i = 0; i < nMetadata; i++ ) {
        if ( singleColumn && (metadataFromTable != null) && (metadataFromTable.size() > 0) ) {
            // Single column output and data sources were determined from the table.
            metadataForTS.add(metadataFromTable.get(i));
        }
        else {
            metadataForTS.add("");
        }
    }
    // Now reset with available.
    if ( metadataFromParameter.size() == 1 ) {
        // Single column or user has specified a constant for the parameter.
        for ( int i = 0; i < nMetadata; i++ ) {
            metadataForTS.set(i,metadataFromParameter.get(0));
        }
    }
    else if ( metadataFromParameter.size() > 1 ) {
        // Multiple column, transfer as many specified values as have.
        for ( int i = 0; i < metadataFromParameter.size(); i++ ) {
            metadataForTS.set(i,metadataFromParameter.get(i));
        }
    }
    return metadataForTS;
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
	return ( new TableToTimeSeries_JDialog ( parent, this, tableIDChoices ) ).ok();
}

/**
Format the TSID from the table records, used as the key for the hash map to look up time series.
*/
private String formatTSIDFromTableRecord ( TableRecord rec, int locationTypePos, int locationPos, int dataSourcePos,
    int dataTypePos, TimeInterval interval, int scenarioPos, int sequenceIDPos, String locationTypeFromParam,
    String dataSourceFromParam, String dataTypeFromParam, String scenarioFromParam, String sequenceIDFromParam )
throws Exception {
    String locationType = null, locationId = null, dataSource = null, dataType = null, scenario = null, sequenceID = null;
	String periodReplacement = ""; // TODO SAM 2015-10-12 Evaluate whether to pass as command parameter.
    StringBuffer tsidFromTable = new StringBuffer();
    locationId = rec.getFieldValueString(locationPos);
    if ( locationId == null ) {
        // Don't have location ID.
        return null;
    }
    // Also save the other metadata if a column was specified.
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
    // Always append the location ID.
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
    dataType = dataType.replace(".", periodReplacement);
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
    if ( sequenceIDPos >= 0 ) {
    	sequenceID = rec.getFieldValueString(sequenceIDPos);
    }
    else {
    	sequenceID = sequenceIDFromParam;
    }
    if ( (sequenceID != null) && !sequenceID.isEmpty() ) {
        tsidFromTable.append(TSIdent.SEQUENCE_NUMBER_LEFT);
        tsidFromTable.append(sequenceID);
        tsidFromTable.append(TSIdent.SEQUENCE_NUMBER_RIGHT);
    }
    return tsidFromTable.toString();
}

/**
Get the column names by handling the TC[] notation.
@param table the table that is being processed
@param columnName0 the initial parameter value that may include TC[] notation
*/
private List<String> getColumnNamesFromNotation ( DataTable table, String columnName0 ) {
    String routine = getClass().getSimpleName() + ".getColumnNamesFromNotation";
    List<String> columnNames = new ArrayList<>();
    List<String> columnHeadingList = Arrays.asList(table.getFieldNames());
    int nColumnHeadings = columnHeadingList.size();
    // Much of the following matches code in the ReadDelimitedFile() command, but here we are processing a table.
    // TODO SAM 2012-11-15 Maybe should put this in the table package as a utility to handle slice notation.
    if ( StringUtil.startsWithIgnoreCase(columnName0,_TC) ) {
        // Need to process the column names from the file.
        int parenPos1 = columnName0.indexOf(_TC);
        int parenPos2 = columnName0.indexOf("]");
        if ( (parenPos1 >= 0) && (parenPos2 >= 0) ) {
            // Need to interpret slice of field numbers in file.
            String slice = columnName0.substring((parenPos1 + _TC.length()),parenPos2);
            int [] tableColPos = StringUtil.parseIntegerSlice( slice, ":", 0, nColumnHeadings );
            Message.printStatus(2, routine, "Got " + tableColPos.length + " columns from slice \"" + slice + "\"" );
            for ( int ipos = 0; ipos <tableColPos.length; ipos++ ) {
                // Positions from parameter parsing are 1+ so need to decrement to get 0+ indices.
                Message.printStatus(2, routine, "Adding table column name \"" + columnHeadingList.get(tableColPos[ipos] - 1).trim() + "\"" );
                columnNames.add ( columnHeadingList.get(tableColPos[ipos] - 1).trim() );
            }
        }
        else {
            // Use all the file field names.
            for ( int ipos = 0; ipos <nColumnHeadings; ipos++ ) {
                Message.printStatus(2, routine, "Adding table column name \"" + columnHeadingList.get(ipos).trim() + "\"" );
                columnNames.add ( columnHeadingList.get(ipos).trim() );
            }
        }
    }
    else {
        // A literal string that can be used as is.
        Message.printStatus(2, routine, "Adding user-specified column name \"" + columnName0 + "\"" );
        columnNames.add ( columnName0 );
    }
    return columnNames;
}

/**
Return the runtime column names list.
@return the runtime column names list
*/
@SuppressWarnings("unused")
private List<String> getColumnNamesRuntime() {
    return __columnNamesRuntime;
}

/**
Return the data source constant.
@return the data source constant.
*/
private List<String> getDataSource() {
    return __dataSource;
}

/**
Return the data type constant.
@return the data type constant
*/
private List<String> getDataType() {
    return __dataType;
}

/**
Return the date column name expanded for runtime.
@return the date column name expanded for runtime
*/
private String getDateColumnRuntime () {
    return __dateColumnRuntime;
}

/**
Return the date/time column name expanded for runtime.
@return the date/time column name expanded for runtime
*/
private String getDateTimeColumnRuntime () {
    return __dateTimeColumnRuntime;
}

// TODO SAM 2015-06-11 make more robust - for now just keep simple.
/**
Return the minimum and maximum date/times from a block layout record.
@param rec the DataTable record being processed
@param row the row number (1+) for error messages
@param dateTimePos the column number for the date/time, or -1 if not used
@param dateTime if non-null, use the instance for results, rather than creating a new object - this can be
more efficient when iterating through raw data
@param errorMessages if not null, add parse messages to the list
@return a DateTime[] array with minimum and maximum date/times for a block layout record, or null if can't be determined.
*/
private DateTime [] getDateTimesFromBlockRecord ( TableRecord rec, int row, int dateTimePos,
	int layoutRows, int layoutColumns, YearType yearType, List<String> errorMessages )
throws Exception {
	DateTime [] dts = new DateTime[2];
	Object o;
	if ( layoutRows == TimeInterval.YEAR ) {
		o = rec.getFieldValue(dateTimePos);
		if ( o == null ) {
			return null;
		}
		DateTime dt = new DateTime((DateTime)o);
		// FIXME SAM 2015-06-11 Hard-code water year to calendar year conversion for now.
		dt.addYear(-1);
		if ( layoutColumns == TimeInterval.MONTH ) {
			dts[0] = new DateTime(dt,DateTime.PRECISION_MONTH);
			dts[0].setMonth(10);
			dts[1] = new DateTime(dt,DateTime.PRECISION_MONTH);
			dts[1].addYear(1);
			dts[1].setMonth(9);
		}
	}
	return dts;
}

/**
Return the date/time from the record given the date/time column numbers and format.
@param rec the DataTable record being processed
@param row the row number (1+) for error messages
@param dateTimePos the column number for the date/time, or -1 if not used
@param datePos the column number for the date, or -1 if not used
@param timePos the column number for the time, or -1 if not used
@param dateTime if non-null, use the instance for results, rather than creating a new object - this can be
more efficient when iterating through raw data
@param dateTimeParser parser based on the specified format (if null use default parser)
@param errorMessages if not null, add parse messages to the list
@return a DateTime object or null if the value cannot be parsed.
*/
private DateTime getDateTimeFromRecord ( TableRecord rec, int row, int dateTimePos, int datePos, int timePos,
    DateTime dateTime, DateTimeParser dateTimeParser, List<String> errorMessages ) {
    String dateTimeString = null, dateString, timeString;
    Object dateTimeObject, dateObject, timeObject;
    // Determine the date/time.
    if ( dateTimePos >= 0 ) {
        try {
            dateTimeObject = rec.getFieldValue(dateTimePos);
        }
        catch ( Exception e ) {
            // Should not happen.
            dateTimeObject = null;
            Message.printWarning(3,"",e);
        }
        if ( dateTimeObject == null ) {
            return null;
        }
        else if ( dateTimeObject instanceof DateTime ) {
        	// Can return the DateTime object as is.
            return (DateTime)dateTimeObject;
        }
        else if ( dateTimeObject instanceof Date ) {
        	// Construct a DateTime object from the raw Date.
            return new DateTime((Date)dateTimeObject);
        }
        else {
        	// Parse the string to a DateTime (parsing done below).
            dateTimeString = "" + dateTimeObject; // Could be integer year.
        }
    }
    else if ( datePos >= 0 ) {
        try {
            dateObject = rec.getFieldValue(datePos);
        }
        catch ( Exception e ) {
            // Should not happen.
            dateObject = null;
            Message.printWarning(3,"",e);
        }
        if ( dateObject == null ) {
            return null;
        }
        else if ( dateObject instanceof DateTime ) {
            return (DateTime)dateObject;
        }
        else if ( dateObject instanceof Date ) {
            return new DateTime((Date)dateObject);
        }
        else {
            dateString = "" + dateObject; // Could be integer year.
        }
        if ( timePos >= 0 ) {
            try {
                timeObject = rec.getFieldValue(timePos);
            }
            catch ( Exception e ) {
                // Should not happen.
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
            // Reuse the date/time for performance.
            // This is safe because for regular time series only the parts are used
            // and for irregular a copy is made when setting the value.
            return dateTimeParser.parse(dateTime,dateTimeString);
        }
        else {
            // Let the parse() method figure out the format.
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
        //    // Print one exception to help with troubleshooting.
        //    Message.printWarning(3, routine, dte);
        //}
        return null;
    }
}

/**
Return the list of time series read in discovery phase.
@return the list of time series read in discovery phase
*/
private List<TS> getDiscoveryTSList () {
    return __discovery_TS_List;
}

/**
Return the flag column list, expanded for runtime.
@return the flag column list, expanded for runtime
*/
private List<String> getFlagColumnsRuntime() {
    return __flagColumnsRuntime;
}

/**
Return the data interval.
@return the data interval
*/
private TimeInterval getInterval() {
    return __interval;
}

/**
Return the location ID list, expanded for runtime.
@return the location ID list, expanded for runtime
*/
private List<String> getLocationIDRuntime() {
    return __locationIDRuntime;
}

/**
Return the location type list.
@return the location type list
*/
private List<String> getLocationType() {
    return __locationType;
}

/**
Return the missing value string(s).
@return the missing value string(s)
*/
private List<String> getMissingValue() {
    return __missingValue;
}

/**
Return the list of data objects read by this object in discovery mode.
@return the list of data objects read by this object in discovery mode
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    List<TS> discovery_TS_List = getDiscoveryTSList ();
    if ( (discovery_TS_List == null) || (discovery_TS_List.size() == 0) ) {
        return null;
    }
    // Since all time series in table must have the same interval, check the first time series (e.g., MonthTS).
    TS datats = discovery_TS_List.get(0);
    // Also check the base class.
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discovery_TS_List;
    }
    else {
        return null;
    }
}

/**
Return the data precision.
@return the data precision
*/
private List<String> getPrecision() {
    return __precision;
}

/**
Return the scenario constant.
@return the scenario constant
*/
private List<String> getScenario() {
    return __scenario;
}

/**
Return the sequence ID constant.
@return the sequence ID constant
*/
private List<String> getSequenceID() {
    return __sequenceID;
}

/**
Return the number of rows to skip (ranges).
@return the number of rows to skip (ranges)
*/
private int[][] getSkipRows() {
    return __skipRows;
}

/**
Return the time column name expanded for runtime.
@return the time column name expanded for runtime
*/
private String getTimeColumnRuntime () {
    return __timeColumnRuntime;
}

/**
Return the data units constant.
@return the data units constant
*/
private List<String> getUnits() {
    return __units;
}

/**
Return the value column list, expanded for runtime.
@return the value column list, expanded for runtime
*/
private List<String> getValueColumnsRuntime() {
    return __valueColumnsRuntime;
}

/**
Parse command from text.
@param commmand command string to parse
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
    // First parse.
    super.parseCommand(command);
    PropList parameters = getCommandParameters();
    // Replace legacy "Provider" parameter with "DataSource".
    String provider = parameters.getValue("Provider");
    if ( provider != null ) {
        parameters.set("DataSource",provider);
        parameters.unSet("Provider");
    }
}

/**
Read a list of time series from a table in block format.  Blocks for either a single location ID
can be specified (use locationID), or, multiple locations (use locationColumn).
@param table table to read from
@return a list of time series read from the table
*/
private List<TS> readTimeSeriesListBlock ( DataTable table,
	int layoutColumns, int layoutRows, YearType yearType,
	String dateTimeColumn,
	// The following are when parameters provide the values.
	String locationType, String locationID, String dataSource, String dataType, TimeInterval interval,
	String scenario, String sequenceID, String units, String precision, String missing,
	// The following are when columns provide the values.
	String locationTypeColumn, String locationColumn, String dataSourceColumn,
    String dataTypeColumn, String scenarioColumn, String sequenceIDColumn, String unitsColumn, String valueColumn,
	boolean readData, CommandPhaseType commandPhase, List<String> errorMessages ) {
	String routine = getClass().getSimpleName() + ".readTimeSeriesListBlock";
	List<TS> tslist = new ArrayList<>();
    // Translate column names to integer values to speed up processing below:
	// - these have been expanded for runtime
    // - any operations on table will fail if in discovery mode
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
    int locationTypePos = -1;
    int locationPos = -1;
    int dataSourcePos = -1;
    int dataTypePos = -1;
    int scenarioPos = -1;
    int sequenceIDPos = -1;
    //int unitsPos = -1;
    int valuePos = -1;
    // Determine column positions for metadata columns.
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
    if ( (sequenceIDColumn != null) && !sequenceIDColumn.equals("") ) {
        try {
        	sequenceIDPos = table.getFieldIndex(sequenceIDColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for sequence ID column \"" + sequenceIDColumn + "\"" );
            }
        }
    }
    if ( (unitsColumn != null) && !unitsColumn.equals("") ) {
        try {
            //unitsPos = table.getFieldIndex(unitsColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for units column \"" + unitsColumn + "\"" );
            }
        }
    }
    if ( (valueColumn != null) && !valueColumn.isEmpty() ) {
        try {
            valuePos = table.getFieldIndex(valueColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for value column \"" + valueColumn + "\"" );
            }
        }
    }
    if ( errorMessages.size() > 0 ) {
        // Don't continue if there are errors.
        return tslist;
    }
    // Loop through the table once and get the period from the data and if necessary the time series identifiers.
    Object o;
    int nRecords = table.getNumberOfRecords();
    TableRecord rec = null;
    DateTime [] dts = null;
    DateTime dtMinFromTable = null;
    DateTime dtMaxFromTable = null;
	// Use a hashmap for a unique list of time series identifiers from the table to facilitate lookups.
    String tsidFromTable = null;
    LinkedHashMap<String,String> tsidsFromTable = new LinkedHashMap<>();
    // Need to determine the TSID to allocate the time series whether reading data or not.
    // Only need to loop through the table if reading data or TSIDs come from table columns.
    if ( locationPos < 0 ) {
    	// Only reading a single time series.
    	String tsidentstr = locationID + "." + dataSource + "." + dataType + "." + interval;
    	if ( (locationType != null) && !locationType.isEmpty() ) {
    		tsidentstr = locationType + ":" + tsidentstr;
    	}
    	tsidsFromTable.put(tsidentstr, tsidentstr);
    }
	// Need to get TSIDs from the table and also date/time range for metadata.
    for ( int iRec = 0; iRec < nRecords; iRec++ ) {
        try {
            rec = table.getRecord(iRec);
            if ( readData ) {
	            // Consider whether single field, etc.
	            dts = getDateTimesFromBlockRecord(rec,(iRec + 1),dateTimePos,layoutRows,layoutColumns,yearType,null);
	            if ( dts == null ) {
	                continue;
	            }
	            if ( (dtMaxFromTable == null) || dts[1].greaterThan(dtMaxFromTable) ) {
	                dtMaxFromTable = dts[1];
	            }
	            if ( (dtMinFromTable == null) || dts[0].lessThan(dtMinFromTable) ) {
	                dtMinFromTable = dts[0];
	            }
            }
            if ( locationPos >= 0 ) {
	            // Determine the location identifier from the data.
	            try {
	                tsidFromTable = formatTSIDFromTableRecord ( rec, locationTypePos, locationPos, dataSourcePos,
	                    dataTypePos, interval, scenarioPos, sequenceIDPos, locationType, dataSource, dataType, scenario, sequenceID );
	            }
	            catch ( Exception e ) {
	                // Should not happen.
	                continue;
	            }
	            if ( tsidFromTable == null ) {
	                // No location in record.
	                continue;
	            }
	            // Add to the hashtable if not found.
	            if ( tsidsFromTable.get(tsidFromTable) == null ) {
	                tsidsFromTable.put(tsidFromTable.toString(), tsidFromTable.toString());
	            }
	        }
        }
        catch ( Exception e ) {
            continue;
        }
    }
    Message.printStatus(2,routine,"Min date/time from table = " + dtMinFromTable +
        ", max date/time from table = " + dtMaxFromTable );
    // Create time series.
    TS ts = null;
    int its = -1;
    Message.printStatus(2,routine,"Have " + tsidsFromTable.size() + " location IDs to read from block.");
    String tsidentstr = null;
    its = -1; // Used to iterate through units, parallel to TSID hashmap.
    TSIdent tsident = null;
    for ( Map.Entry<String,String> tsid: tsidsFromTable.entrySet() ) {
        tsidentstr = tsid.getKey();
        //Message.printStatus(2, routine, "Creating time series for TSID=\"" + tsidentstr + "\", units=\"" +
        //    unitsFromTableList.get(its) + "\"" );
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
		        // Set all the information.
		        ts.setIdentifier ( tsident );
		        ts.setDescription ( tsident.getLocation() + " " + tsident.getType() );
		        ts.setDataUnits ( "" );//unitsFromTableList.get(its) );
		        //ts.setDataUnitsOriginal ( unitsFromTableList.get(its) );
                if ( (precision != null) && !precision.isEmpty() ) {
                	ts.setDataPrecision ( Short.parseShort(precision) );
                }
		        if ( (missing != null) && !missing.isEmpty() ) {
		        	if ( missing.equalsIgnoreCase("nan") ) {
		        		ts.setMissing ( Double.NaN );
		        	}
		        	else {
		        		ts.setMissing ( Double.parseDouble(missing) );
		        	}
		        }
		        else {
		        	ts.setMissing ( Double.NaN );
		        }
		        ts.setInputName ( table.getTableID() );
		        //if ( inputStartReq != null ) {
		        //    ts.setDate1(inputStartReq);
		        // }
		        //else {
		            ts.setDate1(dtMinFromTable);
		        //}
		        //if ( inputEndReq != null ) {
		        //    ts.setDate2(inputEndReq);
		        //}
		        //else {
		            ts.setDate2(dtMaxFromTable);
		        //}
		        ts.setDate1Original(dtMinFromTable);
		        ts.setDate2Original(dtMaxFromTable);
		        if ( readData ) {
		        	ts.allocateDataSpace();
		        }
		        tslist.add(ts);
		    }
		    catch ( Exception e ) {
		        // Set the TS to null to match the column positions but won't be able to set data below.
		        ts = null;
		        errorMessages.add ( "Error initializing time series \"" +
		            tsidentstr + "\" (" + e + ") - will not read.");
		        Message.printWarning(3,routine,e);
		    }
    	}
    }
    // Now read through the table and process records into the time series.
    // This is similar to single column reading but with optimization where possible to read a single time series.
    if ( readData ) {
        // Process the data records.
        // Create an ordered hash map to simplify lookup of time series based on the TSID information.
        LinkedHashMap<String,TS> tsHash = new LinkedHashMap<>();
        its = -1;
        for ( Map.Entry<String,String> tsid: tsidsFromTable.entrySet() ) {
            ++its;
            tsidentstr = tsid.getKey();
            ts = tslist.get(its);
            tsHash.put(tsidentstr,ts);
        }
	    DateTime dt = null;
	    DateTime tableDate = null;
	    if ( layoutColumns == TimeInterval.MONTH ) {
	    	dt = new DateTime(DateTime.PRECISION_MONTH);
	    }
	    String tsidFromTablePrev = "";
	    for ( int iRec = 0; iRec < nRecords; iRec++ ) {
	    	try {
	    		rec = table.getRecord(iRec);
	    	}
	    	catch ( Exception e ) {
	    		continue;
	    	}
	    	// Figure out the time series that is being processed.
	    	if ( locationPos < 0 ) {
	    		// Use the single time series.
	    		ts = tslist.get(0);
	    	}
	    	else {
	    		// Need to figure out the time series identifier based on the record.
	            // Get the TSID string for the table record, used to look up the time series.
	            try {
	                tsidFromTable = formatTSIDFromTableRecord ( rec, locationTypePos, locationPos, dataSourcePos,
	                    dataTypePos, interval, scenarioPos, sequenceIDPos, locationType, dataSource, dataType, scenario, sequenceID );
	            }
	            catch ( Exception e ) {
	                // Should not happen - don't process the table record.
	                continue;
	            }
	            // Get the time series to process, may be the same as for the previous record,'
	            // in which case a hash table lookup is not needed.
	            if ( !tsidFromTable.equals(tsidFromTablePrev) ) {
	                ts = tsHash.get(tsidFromTable);
	            }
	            // Else, record is for the same time series that is was modified for previous record.
	            tsidFromTablePrev = tsidFromTable;
	    	}
	        if ( layoutRows == TimeInterval.YEAR ) {
	        	if ( layoutColumns == TimeInterval.MONTH ) {
	        		try {
	        			o = rec.getFieldValue(dateTimePos);
	        		}
	        		catch ( Exception e ) {
	        			continue;
	        		}
	        		if ( o == null ) {
	        			continue;
	        		}
	        		tableDate = (DateTime)o;
	        		// Transfer the date values to the working date (only year).
	        		dt.setDate(tableDate); // Also sets precision but need to override below since date is only year.
	        		dt.setPrecision(DateTime.PRECISION_MONTH);
	        		//Message.printStatus(2, routine, "Row (" + (iRec + 1) + ") tableDate=" + tableDate + " dt=" + dt );
	        		for ( int imon = 0; imon < 12; imon++ ) {
	        			if ( imon == 0 ) {
	        				// Convert water year to previous calendar year.
	        				dt.addYear(yearType.getStartYearOffset());
	        				dt.setMonth(yearType.getStartMonth());
	    	        		//Message.printStatus(2, routine, "Row (" + (iRec + 1) + " Starting new year " + dt );
	        			}
	        			else {
	        				dt.addMonth(1);
	        			}
	        			// Get the value, starting with the column for the data values.
	        			try {
	        				// Date is column 0, Year 1, YearAsDate 2.
	        				o = rec.getFieldValue(valuePos + imon);
	        			}
	        			catch ( Exception e ) {
	        				continue;
	        			}
	        			if ( (o != null) && (o instanceof Double) ) {
	        				//Message.printStatus(2, routine, "Setting " + dt + " to " + o);
	        				ts.setDataValue(dt, (Double)o );
	        			}
	        		}
	        	}
	        }
	    }
    }
	return tslist;
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
@param precision data list of data precision to use for time series
@param missing list of missing values to use for time series
@param handleDuplicatesHow indicate how to handle duplicate date/times
@param inputStartReq requested start of data (null to return all).
@param inputEndReq requested end of data (null to return all).
@param readData True to read data, false to only read the header information.
@param errorMessages Error message strings to be propagated back to calling code.
@return the list of time series that were read
*/
private List<TS> readTimeSeriesListMultiple ( DataTable table,
    String dateTimeColumn, String dateTimeFormat,
    String dateColumn, String timeColumn, List<String> valueColumns, List<String> flagColumns,
    int[][] skipRows, String locationTypeColumn, String locationColumn, String dataSourceColumn,
    String dataTypeColumn, String scenarioColumn, String unitsColumn,
    List<String> locationTypes, List<String> locationIds, List<String> dataSources, List<String> dataTypes, TimeInterval interval,
    TimeInterval irregularIntervalPrecision,
    List<String> scenarios, List<String> units, List<String> precision, List<String> missing, HandleDuplicatesHowType handleDuplicatesHow,
    DateTime inputStartReq, DateTime inputEndReq,
    boolean readData, CommandPhaseType commandPhase, List<String> errorMessages )
throws IOException
{   String routine = getClass().getSimpleName() + ".readTimeSeriesListMultiple";
    // Allocate the list
    List<TS> tslist = new ArrayList<>();
    if ( handleDuplicatesHow == null ) {
        handleDuplicatesHow = HandleDuplicatesHowType.USE_LAST; // Default.
    }
    // Translate column names to integer values to speed up processing below - these have been expanded for runtime.
    // Any operations on table will fail if in discovery mode.
    int dateTimePos = -1;
    if ( (dateTimeColumn != null) && !dateTimeColumn.isEmpty() ) {
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
    if ( (dateTimeFormat != null) && !dateTimeFormat.trim().isEmpty() ) {
        // Set to null to simplify logic below.
        dateTimeParser = new DateTimeParser ( dateTimeFormat );
    }
    int datePos = -1;
    if ( (dateColumn != null) && !dateColumn.isEmpty() ) {
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
    if ( (timeColumn != null) && !timeColumn.isEmpty() ) {
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
    if ( (locationColumn != null) && !locationColumn.isEmpty() ) {
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
   	int [] flagPos = new int[0];
    if ( commandPhase == CommandPhaseType.RUN ) {
    	try {
        	valuePos = table.getFieldIndices(valueColumns.toArray(new String[valueColumns.size()]));
    	}
    	catch ( Exception e ) {
        	// Use empty array from above.
    		errorMessages.add(e.toString());
    		Message.printWarning(3, routine, e);
    	}
    	try {
        	flagPos = table.getFieldIndices(flagColumns.toArray(new String[flagColumns.size()]));
    	}
    	catch ( Exception e ) {
    		Message.printWarning(3, routine, e);
    		errorMessages.add(e.toString());
        	// Use empty array from above.
    	}
    }
    if ( errorMessages.size() > 0 ) {
        // Don't continue if there are errors.
        return tslist;
    }
    // Loop through the data records and get the maximum and minimum date/times, as well as the unique locations,
    // needed to initialize the time series.
    // TODO SAM 2012-11-25 If single column data, the date/times should correspond to the unique list of time series,
    // but a performance hit to figure out.
    //Message.printStatus(2,routine,"Table="+table);
    int nRecords = 0;
    if ( readData ) {
        nRecords = table.getNumberOfRecords();
    }
    DateTime dt = null, dtMaxFromTable = null, dtMinFromTable = null;
    TableRecord rec;
    // Loop through the data records and get the period from the data.
    // If single column, also get the unique list of identifiers and other metadata.
    Object o;
    // Lists of data extracted from time series in the table, used to initialize the time series.
    List<String> locationTypesFromTable = new ArrayList<>();
    List<String> locationIdsFromTable = new ArrayList<>();
    List<String> dataSourcesFromTable = new ArrayList<>();
    List<String> dataTypesFromTable = new ArrayList<>();
    List<String> scenariosFromTable = new ArrayList<>();
    List<String> unitsFromTable = new ArrayList<>();
    int errorMax = 50;
    int errorCount = 0;
    for ( int iRec = 0; iRec < nRecords; iRec++ ) {
        try {
            rec = table.getRecord(iRec);
            // Consider whether single field, etc.
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
        	++errorCount;
        	if ( errorCount < errorMax ) {
        		// Print a few errors to help with troubleshooting.
                errorMessages.add("Error getting date/time from record " + (iRec + 1) + ": " + e );
                Message.printWarning(3, routine, e);
        	}
            continue;
        }
    }
    Message.printStatus(2,routine,"Min date/time from table = " + dtMinFromTable +
        ", max date/time from table = " + dtMaxFromTable );
    // Create lists of metadata to initialize each time series.
    // Create location types for each time series.
    boolean singleColumnFalse = false;
    List<String>locationTypesForTS = createMetadataRuntime ( singleColumnFalse,
        locationTypesFromTable, valueColumns.size(), locationTypes);
    // Create data sources for each time series.
    List<String>dataSourcesForTS = createMetadataRuntime ( singleColumnFalse,
        dataSourcesFromTable, valueColumns.size(), dataSources);
    // Create data types for each time series.
    // Data types can be specified as single value for all data columns, list of data type strings (number matches number of values),
    // or if not specified, use the value column names.
    List<String> dataTypesForTS = new ArrayList<>();
    if ( dataTypes.size() == 0 ) {
        // Use the data value columns.
        dataTypesForTS = valueColumns;
    }
    else {
        dataTypesForTS = createMetadataRuntime ( singleColumnFalse, dataTypesFromTable, valueColumns.size(), dataTypes);
    }
    List<String> locationIdsForTS = createMetadataRuntime ( singleColumnFalse, locationIdsFromTable, valueColumns.size(), locationIds );
    // Create scenarios for each time series.
    List<String>scenariosForTS = createMetadataRuntime ( singleColumnFalse, scenariosFromTable, valueColumns.size(), scenarios);
    // Create units for each time series.
    List<String>unitsForTS = createMetadataRuntime ( singleColumnFalse, unitsFromTable, valueColumns.size(), units );
    // Create precision for each time series:
    // - don't have precision from the table, only the command parameter
    List<String>precisionForTS = createMetadataRuntime ( singleColumnFalse, null, valueColumns.size(), precision );
    Message.printStatus(2,routine,"Sizes:" +
        " valueColumns=" + valueColumns.size() +
        " valuePos=" + valuePos.length +
        " locationTypesForTS=" + locationTypesForTS.size() +
        " locationIdsForTS=" + locationIdsForTS.size() +
        " dataTypesForTS=" + dataTypesForTS.size() +
        " scenariosForTS=" + scenariosForTS.size() +
        " dataSourcesForTS=" + dataSourcesForTS.size() +
        " unitsForTS=" + unitsForTS.size() +
        " precisionForTS=" + precisionForTS.size() );
    // Create the time series.  If single column, the count corresponds to the individual location identifiers.
    // If multiple column, the count corresponds to the number of value columns.
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
        if ( locationTypesForTS.get(its).isEmpty() ) {
            locType = "";
        }
        else {
            locType = locationTypesForTS.get(its) + TSIdent.LOC_TYPE_SEPARATOR;
        }
        if ( scenariosForTS.get(its).isEmpty() ) {
            scenario = "";
        }
        else {
            scenario = TSIdent.SEPARATOR + scenariosForTS.get(its);
        }
        tsidentstr = locType + locationIdsForTS.get(its) + "." + dataSourcesForTS.get(its) + "." +
            dataTypesForTS.get(its) + "." + interval + scenario;
        Message.printStatus(2, routine, "Creating time series for TSID=\"" + tsidentstr + "\", units=\"" +
            unitsForTS.get(its) + "\", precision=" + precisionForTS.get(its) );
        valueColumnForTS = valueColumns.get(its);
        valuePosForTS = valuePos[its];
        if ( valuePosForTS < 0 ) {
            // Was a problem looking up column numbers.
            errorMessages.add ( "Value column name \"" +
                valueColumnForTS + "\" does not match known data columns - will not read.");
        }
        else {
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
                    // Set all the information.
                    ts.setIdentifier ( tsident );
                    ts.setDescription ( locationIdsForTS.get(its) + " " + dataTypesForTS.get(its) );
                    ts.setDataUnits ( unitsForTS.get(its) );
                    if ( !precisionForTS.get(its).isEmpty() ) {
                    	ts.setDataPrecision ( Short.parseShort(precisionForTS.get(its)) );
                    }
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
                    if ( irregularIntervalPrecision != null ) {
                    	Message.printStatus(2, routine, "Irregular interval precision = " + irregularIntervalPrecision.getBase());
                    	if ( ts.getDataIntervalBase() == TimeInterval.IRREGULAR ) {
                    		ts.setDate1(ts.getDate1().setPrecision(irregularIntervalPrecision.getBase()));
                    		ts.setDate2(ts.getDate2().setPrecision(irregularIntervalPrecision.getBase()));
                    		ts.setDate1Original(ts.getDate1Original().setPrecision(irregularIntervalPrecision.getBase()));
                    		ts.setDate2Original(ts.getDate2Original().setPrecision(irregularIntervalPrecision.getBase()));
                    	}
                    }
                    if ( Message.isDebugOn ) {
                    	Message.printStatus(2, routine, "Allocated time series with period " + ts.getDate1() + " to " + ts.getDate2() );
                    }
                }
                catch ( Exception e ) {
                    // Set the TS to null to match the column positions but won't be able to set data below.
                    ts = null;
                    errorMessages.add ( "Error initializing time series \"" +
                        tsidentstr + "\" (" + e + ") - will not read.");
                    Message.printWarning(3,routine,e);
                }
            }
        }
        // Don't add if null.
        if ( ts != null ) {
            tslist.add ( ts );
        }
    }
    if ( !readData ) {
        return tslist;
    }
    // Process the data records.
    // Date-time is reused for multiple locations.
    Double value = null;
    String flag = null;
    int flagColumnPos;
    TS ts = null;
    // All time series will have the same period since each time series shows up in every row.
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
            // Create a copy so the original in the table is not changed.
            dt = new DateTime (dt);
            if ( (ts.getDataIntervalBase() == TimeInterval.IRREGULAR) && (irregularIntervalPrecision != null) ) {
            	// Set the precision on the date/time.
            	dt.setPrecision(irregularIntervalPrecision.getBase());
            }
            // If the requested period was set for irregular, only set if in the period:
            // - this is needed because the IrregularTS does not restrict setting data outside of the period
            if ( ts.getDataIntervalBase() == TimeInterval.IRREGULAR ) {
            	if ( (inputStartReq != null) && dt.lessThan(inputStartReq) ) {
            		continue;
            	}
            	if ( (inputEndReq != null) && dt.greaterThan(inputEndReq) ) {
            		continue;
            	}
            }
            // Loop through the values, taken from 1+ columns in the row.
            for ( int ival = 0; ival < valuePos.length; ival++ ) {
                // Get the time series, which will be in the order of the values,
            	// since the same order was used to initialize the time series above.
                ts = tslist.get(ival);
                // Get the value.
                o = rec.getFieldValue(valuePos[ival]);
                if ( o == null ) {
                    // Do not continue here because HandleDuplicatesHow may actually set to missing value.
                    value = ts.getMissing();
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
                // Get the the data flag.
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
                // Set the value and flag in the time series.
                if ( (flag != null) && !flag.equals("") ) {
                    if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_LAST ) {
                        // Set to the last value even if missing.
                        ts.setDataValue(dt, value, flag, -1);
                    }
                    else if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_LAST_NONMISSING ) {
                        // Set to the last value only if not missing.
                        if ( !ts.isDataMissing(value) ) {
                            ts.setDataValue(dt, value, flag, -1);
                        }
                    }
                    else if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_FIRST_NONMISSING ) {
                        // Only set if value in time series is missing (has not been set).
                        if ( ts.isDataMissing(ts.getDataValue(dt)) && !ts.isDataMissing(value) ) {
                            ts.setDataValue(dt, value, flag, -1);
                        }
                    }
                    else if ( handleDuplicatesHow == HandleDuplicatesHowType.ADD ) {
                        if ( !ts.isDataMissing(value) ) {
                            // If the existing value is missing, just set.
                            double oldValue = ts.getDataValue(dt);
                            if ( ts.isDataMissing(oldValue) ) {
                                ts.setDataValue(dt, value, flag, -1);
                            }
                            else {
                                // Add to the previous value.
                                ts.setDataValue(dt, (oldValue + value), flag, -1);
                            }
                        }
                    }
                }
                else {
                    if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_LAST ) {
                        // Set to the last value even if missing.
                        ts.setDataValue(dt, value);
                    }
                    else if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_LAST_NONMISSING ) {
                        // Set to the last value only if not missing.
                        if ( !ts.isDataMissing(value) ) {
                            ts.setDataValue(dt, value);
                        }
                    }
                    else if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_FIRST_NONMISSING ) {
                        // Only set if value in time series is missing (has not been set).
                        if ( ts.isDataMissing(ts.getDataValue(dt)) && !ts.isDataMissing(value) ) {
                            ts.setDataValue(dt, value);
                        }
                    }
                    else if ( handleDuplicatesHow == HandleDuplicatesHowType.ADD ) {
                        if ( !ts.isDataMissing(value) ) {
                            // If the existing value is missing, just set.
                            double oldValue = ts.getDataValue(dt);
                            if ( ts.isDataMissing(oldValue) ) {
                                ts.setDataValue(dt, value);
                            }
                            else {
                                // Add to the previous value.
                                ts.setDataValue(dt, (oldValue + value));
                            }
                        }
                    }
                }
            }
        }
        catch ( Exception e ) {
            // Skip the record.
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
@param sequenceIDColumn the column to use for sequence ID, if single column data table
@param unitsColumn the column to use for units, if single column data table
@param locationType location type to use for time series
@param dataSource data source (provider) to use for time series
@param dataType data type to use for time series
@param interval the data interval
@param scenario scenario to use for time series
@param sequenceID sequence ID to use for time series (constant)
@param units data units to use for time series
@param missing list of missing values in table to set to missing in time series (uses NaN in time series)
@param handleDuplicatesHow indicate how to handle duplicate date/times
@param inputStartReq requested start of data (null to return all).
@param inputEndReq requested end of data (null to return all).
@param readData True to read data, false to only read the header information.
@param errorMessages Error message strings to be propagated back to calling code.
*/
private List<TS> readTimeSeriesListSingle ( DataTable table,
    String dateTimeColumn, String dateTimeFormat,
    String dateColumn, String timeColumn, String valueColumn, String flagColumn,
    int[][] skipRows, String locationTypeColumn, String locationColumn, String dataSourceColumn,
    String dataTypeColumn, String scenarioColumn, String sequenceIDColumn, String unitsColumn, String locationType,
    String dataSource, String dataType, TimeInterval interval, TimeInterval irregularIntervalPrecision,
    String scenario, String sequenceID, String units, String precision, List<String> missing, HandleDuplicatesHowType handleDuplicatesHow,
    DateTime inputStartReq, DateTime inputEndReq,
    boolean readData, CommandPhaseType commandPhase, List<String> errorMessages )
throws IOException {
    String routine = getClass().getSimpleName() + ".readTimeSeriesListSingle";
    // Allocate the list.
    List<TS> tslist = new ArrayList<>();
    if ( handleDuplicatesHow == null ) {
        handleDuplicatesHow = HandleDuplicatesHowType.USE_LAST; // Default.
    }
    // Translate column names to integer values to speed up processing below - these have been expanded for runtime.
    // Any operations on table will fail if in discovery mode.
    int dateTimePos = -1;
    if ( (dateTimeColumn != null) && !dateTimeColumn.isEmpty() ) {
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
    if ( (dateTimeFormat != null) && !dateTimeFormat.trim().isEmpty() ) {
        // Set to null to simplify logic below.
        dateTimeParser = new DateTimeParser ( dateTimeFormat );
    }
    int datePos = -1;
    if ( (dateColumn != null) && !dateColumn.isEmpty() ) {
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
    if ( (timeColumn != null) && !timeColumn.isEmpty() ) {
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
    if ( (locationColumn != null) && !locationColumn.isEmpty() ) {
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
    if ( (flagColumn != null) && !flagColumn.isEmpty() ) {
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
    int sequenceIDPos = -1;
    int unitsPos = -1;
    // Determine column positions for metadata columns.
    if ( (locationTypeColumn != null) && !locationTypeColumn.isEmpty() ) {
        try {
            locationTypePos = table.getFieldIndex(locationTypeColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for location type column \"" + locationTypeColumn + "\"" );
            }
        }
    }
    if ( (dataSourceColumn != null) && !dataSourceColumn.isEmpty() ) {
        try {
            dataSourcePos = table.getFieldIndex(dataSourceColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for data source column \"" + dataSourceColumn + "\"" );
            }
        }
    }
    if ( (dataTypeColumn != null) && !dataTypeColumn.isEmpty() ) {
        try {
            dataTypePos = table.getFieldIndex(dataTypeColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for data type column \"" + dataTypeColumn + "\"" );
            }
        }
    }
    if ( (scenarioColumn != null) && !scenarioColumn.isEmpty() ) {
        try {
            scenarioPos = table.getFieldIndex(scenarioColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for scenario column \"" + scenarioColumn + "\"" );
            }
        }
    }
    if ( (sequenceIDColumn != null) && !sequenceIDColumn.isEmpty() ) {
        try {
        	sequenceIDPos = table.getFieldIndex(sequenceIDColumn);
        }
        catch ( Exception e ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                errorMessages.add("Cannot determine column number for sequence ID column \"" + sequenceIDColumn + "\"" );
            }
        }
    }
    if ( (unitsColumn != null) && !unitsColumn.isEmpty() ) {
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
        // Don't continue if there are errors.
        return tslist;
    }
    // Loop through the data records and get the maximum and minimum date/times
    // and the unique TSID combinations needed to initialize the time series.
    int nRecords = 0;
    if ( readData ) {
        nRecords = table.getNumberOfRecords();
    }
    DateTime dt = null, dtMaxFromTable = null, dtMinFromTable = null;
    TableRecord rec;
    // Loop through the data records and get the period from the data and unique TSIDs.
    // A hashmap is used to track the TSIDs, where the key is the TSID string and the object is initially the same
    // TSID string but will be set to the TS when initialized.
    String unitsFromTable;
    LinkedHashMap<String,String> tsidsFromTable = new LinkedHashMap<>(); // Will retain insert order.
    List<String> unitsFromTableList = new ArrayList<>();
    Object o;
    String tsidFromTable;
    for ( int iRec = 0; iRec <= nRecords; iRec++ ) {
        try {
            rec = table.getRecord(iRec);
            // Consider whether single field, etc.
            dt = getDateTimeFromRecord(rec,(iRec + 1),dateTimePos,datePos,timePos,null,dateTimeParser,null);
            // TODO smalers 2022-03-06 make a copy if the 'dt' object is going to be changed in any way.
            if ( dt == null ) {
                continue;
            }
            if ( (dtMaxFromTable == null) || dt.greaterThan(dtMaxFromTable) ) {
                dtMaxFromTable = dt;
            }
            if ( (dtMinFromTable == null) || dt.lessThan(dtMinFromTable) ) {
                dtMinFromTable = dt;
            }
            // Determine the TSID from the data.
            try {
                tsidFromTable = formatTSIDFromTableRecord ( rec, locationTypePos, locationPos, dataSourcePos,
                    dataTypePos, interval, scenarioPos, sequenceIDPos, locationType, dataSource, dataType, scenario, sequenceID );
            }
            catch ( Exception e ) {
                // Should not happen.
                continue;
            }
            if ( tsidFromTable == null ) {
                // No location in record.
                continue;
            }
            // Add to the hashtable if not found.
            if ( tsidsFromTable.get(tsidFromTable) == null ) {
                tsidsFromTable.put(tsidFromTable.toString(), tsidFromTable.toString());
                // Also save the units in the same order.
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
    int its = -1; // Used to iterate through units, parallel to TSID hashmap.
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
                // Set all the information.
                ts.setIdentifier ( tsident );
                ts.setDescription ( tsident.getLocation() + " " + tsident.getType() );
                ts.setDataUnits ( unitsFromTableList.get(its) );
                ts.setDataUnitsOriginal ( unitsFromTableList.get(its) );
                if ( (precision != null) && !precision.isEmpty() ) {
                	ts.setDataPrecision ( Short.parseShort(precision) );
                }
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
                if ( (ts.getDataIntervalBase() == TimeInterval.IRREGULAR) && (irregularIntervalPrecision != null) ) {
                	ts.setDate1(ts.getDate1().setPrecision(irregularIntervalPrecision.getBase()));
                	ts.setDate2(ts.getDate2().setPrecision(irregularIntervalPrecision.getBase()));
                	ts.setDate1Original(ts.getDate1Original().setPrecision(irregularIntervalPrecision.getBase()));
                	ts.setDate2Original(ts.getDate2Original().setPrecision(irregularIntervalPrecision.getBase()));
                }
            }
            catch ( Exception e ) {
                // Set the TS to null to match the original list positions but won't be able to set data below.
                ts = null;
                errorMessages.add ( "Error initializing time series \"" +
                    tsidentstr + "\" (" + e + ") - will not read.");
                Message.printWarning(3,routine,e);
            }
        }
        // Add even if null to keep the list size the same.
        tslist.add ( ts );
    }
    if ( !readData ) {
    	// Remove null time series.
    	for ( int i = tslist.size() - 1; i >= 0; i-- ) {
    		if ( tslist.get(i) == null ) {
    			tslist.remove(i);
    		}
    	}
        return tslist;
    }
    Message.printStatus(2, routine, "Number of time series including null =" + tslist.size());
    // Create an ordered hash map to simplify lookup of time series based on the TSID information.
    LinkedHashMap<String,TS> tsHash = new LinkedHashMap<>();
    its = -1;
    for ( Map.Entry<String,String> tsid: tsidsFromTable.entrySet() ) {
        ++its;
        tsidentstr = tsid.getKey();
        ts = tslist.get(its);
        if ( ts != null ) {
	        ts.allocateDataSpace();
	        tsHash.put(tsidentstr,ts);
        }
    }
	// Now remove null time series since not needed.
	for ( int i = tslist.size() - 1; i >= 0; i-- ) {
		if ( tslist.get(i) == null ) {
			tslist.remove(i);
		}
	}
	Message.printStatus(2, routine, "Number of time series excluding null =" + tslist.size());
    // Process the data records.
    String tsidFromTablePrev = "";
    Double value = null;
    String sValue;
    String flag;
    for ( int iRec = 0; iRec < nRecords; iRec++ ) {
        try {
            rec = table.getRecord(iRec);
            dt = getDateTimeFromRecord(rec,(iRec + 1),dateTimePos,datePos,timePos,null,dateTimeParser,null);
            if ( dt == null ) {
                continue;
            }
            if ( (ts.getDataIntervalBase() == TimeInterval.IRREGULAR) && (irregularIntervalPrecision != null) ) {
            	// Set the precision on the date/time.
            	dt.setPrecision(irregularIntervalPrecision.getBase());
            }
            // Get the value.
            o = rec.getFieldValue(valuePos);
            if ( o == null ) {
                // Do not continue here because HandleDuplicatesHow may actually set to missing value.
                value = ts.getMissing();
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
            else if ( o instanceof String ) {
            	// Try to convert to a number.
            	sValue = (String)o;
            	sValue = sValue.trim();
            	try {
            		value = Double.parseDouble(sValue);
            	}
            	catch ( NumberFormatException e ) {
            		continue;
            	}
            }
        	else {
        		continue;
        	}
            // Get the TSID string for the table record, used to look up the time series.
            try {
                tsidFromTable = formatTSIDFromTableRecord ( rec, locationTypePos, locationPos, dataSourcePos,
                    dataTypePos, interval, scenarioPos, sequenceIDPos, locationType, dataSource, dataType,
                    scenario, sequenceID );
            }
            catch ( Exception e ) {
                // Should not happen - don't process the table record.
            	if ( Message.isDebugOn ) {
            		Message.printWarning(3, routine, e);
            	}
                continue;
            }
            // Get the time series to process, may be the same as for the previous record,
            // in which case a hash table lookup is not needed.
            if ( !tsidFromTable.equals(tsidFromTablePrev) ) {
                ts = tsHash.get(tsidFromTable);
                if ( ts == null ) {
                	Message.printStatus(2, routine, "Null time series from hashtable using \"" + tsidFromTable + "\"");
                	continue;
                }
            }
            tsidFromTablePrev = tsidFromTable;
            // Get the the data flag.
            flag = null;
            if ( flagPos >= 0 ) {
                o = rec.getFieldValue(flagPos);
                if ( o != null ) {
                    flag = "" + o;
                }
            }
            // Set the value and flag in the time series.
            if ( Message.isDebugOn ) {
            	Message.printDebug(1, routine, "Setting time series from table for date/time=" + dt + " val=" + value + " flag=\"" + flag + "\"");
            }
            if ( (flag != null) && !flag.isEmpty() ) {
                if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_LAST ) {
                    // Set to the last value even if missing.
                    ts.setDataValue(dt, value, flag, -1);
                }
                else if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_LAST_NONMISSING ) {
                    // Set to the last value only if not missing.
                    if ( !ts.isDataMissing(value) ) {
                        ts.setDataValue(dt, value, flag, -1);
                    }
                }
                else if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_FIRST_NONMISSING ) {
                    // Only set if value in time series is missing (has not been set).
                    if ( ts.isDataMissing(ts.getDataValue(dt)) && !ts.isDataMissing(value) ) {
                        ts.setDataValue(dt, value, flag, -1);
                    }
                }
                else if ( handleDuplicatesHow == HandleDuplicatesHowType.ADD ) {
                    if ( !ts.isDataMissing(value) ) {
                        // If the existing value is missing, just set.
                        double oldValue = ts.getDataValue(dt);
                        if ( ts.isDataMissing(oldValue) ) {
                            ts.setDataValue(dt, value, flag, -1);
                        }
                        else {
                            // Add to the previous value.
                            ts.setDataValue(dt, (oldValue + value), flag, -1);
                        }
                    }
                }
            }
            else {
                if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_LAST ) {
                    // Set to the last value even if missing.
                    ts.setDataValue(dt, value);
                }
                else if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_LAST_NONMISSING ) {
                    // Set to the last value only if not missing.
                    if ( !ts.isDataMissing(value) ) {
                        ts.setDataValue(dt, value);
                    }
                }
                else if ( handleDuplicatesHow == HandleDuplicatesHowType.USE_FIRST_NONMISSING ) {
                    // Only set if value in time series is missing (has not been set).
                    if ( ts.isDataMissing(ts.getDataValue(dt)) && !ts.isDataMissing(value) ) {
                        ts.setDataValue(dt, value);
                    }
                }
                else if ( handleDuplicatesHow == HandleDuplicatesHowType.ADD ) {
                    if ( !ts.isDataMissing(value) ) {
                        // If the existing value is missing, just set.
                        double oldValue = ts.getDataValue(dt);
                        if ( ts.isDataMissing(oldValue) ) {
                            ts.setDataValue(dt, value);
                        }
                        else {
                            // Add to the previous value.
                            ts.setDataValue(dt, (oldValue + value));
                        }
                    }
                }
            }
        }
        catch ( Exception e ) {
            // Skip the record.
        	if ( Message.isDebugOn ) {
        		Message.printWarning(1,routine,e);
        	}
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
	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_level = 2;
    //int log_level = 3;
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
		status.clearLog(CommandPhaseType.RUN);
	}

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String TableID = parameters.getValue("TableID");
    if ( commandPhase == CommandPhaseType.RUN ) {
    	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
	//String LocationType = parameters.getValue("LocationType");
	String LocationID = parameters.getValue("LocationID");
	if ( commandPhase == CommandPhaseType.RUN ) {
		LocationID = TSCommandProcessorUtil.expandParameterValue(processor, this, LocationID);
	}
	// Because the irregular interval precision is a separate parameter, the "base" is used for precision.
	// TODO smalers 2022-03-04 enable "IrregSecond", etc. as the "Interval".
	String IrregularIntervalPrecision = parameters.getValue("IrregularIntervalPrecision");
	TimeInterval irregularIntervalPrecision = null;
	if ( (IrregularIntervalPrecision != null) && !IrregularIntervalPrecision.isEmpty() ) {
		irregularIntervalPrecision = TimeInterval.parseInterval(IrregularIntervalPrecision);
		if ( Message.isDebugOn ) {
			Message.printStatus(2,routine,"IrregularIntervalPrecision after parsing \"" +
				IrregularIntervalPrecision + "\" = " +
				irregularIntervalPrecision.getBase() + " (" +
				TimeInterval.getName(irregularIntervalPrecision.getBase(),0) + ")");
		}
	}
	String LocationTypeColumn = parameters.getValue("LocationTypeColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		LocationTypeColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, LocationTypeColumn);
	}
	String LocationColumn = parameters.getValue("LocationColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		LocationColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, LocationColumn);
	}
	String DataSourceColumn = parameters.getValue("DataSourceColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		DataSourceColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, DataSourceColumn);
	}
	String DataTypeColumn = parameters.getValue("DataTypeColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		DataTypeColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, DataTypeColumn);
	}
	String ScenarioColumn = parameters.getValue("ScenarioColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		ScenarioColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, ScenarioColumn);
	}
	String SequenceIDColumn = parameters.getValue("SequenceIDColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		SequenceIDColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, SequenceIDColumn);
	}
	String UnitsColumn = parameters.getValue("UnitsColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		UnitsColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, UnitsColumn);
	}
	String DateTimeColumn = parameters.getValue("DateTimeColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		DateTimeColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, DateTimeColumn);
	}
	String DateTimeFormat = parameters.getValue("DateTimeFormat");
	String DateColumn = parameters.getValue("DateColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		DateColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, DateColumn);
	}
	String TimeColumn = parameters.getValue("TimeColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		TimeColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TimeColumn);
	}
	String ValueColumn = parameters.getValue("ValueColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		ValueColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, ValueColumn);
	}
	String FlagColumn = parameters.getValue("FlagColumn");
	if ( commandPhase == CommandPhaseType.RUN ) {
		FlagColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, FlagColumn);
	}
	String Alias = parameters.getValue("Alias"); // Expanded below.
	String HandleDuplicatesHow = parameters.getValue("HandleDuplicatesHow");
	HandleDuplicatesHowType handleDuplicatesHow = HandleDuplicatesHowType.valueOfIgnoreCase(HandleDuplicatesHow);
	String BlockLayout = parameters.getValue("BlockLayout");
	String BlockLayoutColumns = parameters.getValue("BlockLayoutColumns");
	String BlockLayoutRows = parameters.getValue("BlockLayoutRows");
	String BlockOutputYearType = parameters.getValue("BlockOutputYearType");
	String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}"; // Global default.
	}
    String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}"; // Global default.
	}

    // Get the table to process.

    DataTable table = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Processor does not have full tables in discovery, only IDs so create a table here.
        table = new DataTable();
        table.setTableID(TableID);
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        PropList request_params = null;
        CommandProcessorRequestResultsBean bean = null;
        if ( (TableID != null) && !TableID.isEmpty() ) {
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
                // Table to be read below.
                table = (DataTable)o_Table;
                // TODO SAM 2012-11-15 Evaluate doing this in discovery mode in checkCommandParameters() but
                // for now figure out the information here and set for use below.
                if ( (LocationID != null) && !LocationID.equals("") ) {
                    if ( StringUtil.indexOfIgnoreCase(LocationID,_TC, 0) >= 0 ) {
                        List<String> cols = getColumnNamesFromNotation ( table, LocationID );
                        setLocationIDRuntime(cols);
                    }
                    else if ( LocationID.indexOf(",") > 0 ) {
                        // A list of identifiers has been specified.
                        List<String> ids = StringUtil.breakStringList(LocationID, ",", 0);
                        setLocationIDRuntime(ids);
                    }
                    else {
                        // A single identifier has been specified (will be reused).
                        List<String> ids = new ArrayList<>();
                        ids.add(LocationID);
                        setLocationIDRuntime(ids);
                    }
                }
                if ( (DateTimeColumn != null) && !DateTimeColumn.equals("") &&
                    StringUtil.indexOfIgnoreCase(DateTimeColumn,_TC, 0) >= 0 ) {
                    List<String> cols = getColumnNamesFromNotation ( table, DateTimeColumn );
                    // Date/time column is single value.
                    if ( cols.size() > 0 ) {
                        setDateTimeColumnRuntime(cols.get(0));
                    }
                }
                if ( (DateColumn != null) && !DateColumn.equals("") &&
                    StringUtil.indexOfIgnoreCase(DateColumn,_TC, 0) >= 0 ) {
                    List<String> cols = getColumnNamesFromNotation ( table, DateColumn );
                    // Date column is single value.
                    if ( cols.size() > 0 ) {
                        setDateColumnRuntime(cols.get(0));
                    }
                }
                if ( (TimeColumn != null) && !TimeColumn.equals("") &&
                    StringUtil.indexOfIgnoreCase(TimeColumn,_TC, 0) >= 0 ) {
                    List<String> cols = getColumnNamesFromNotation ( table, TimeColumn );
                    // Time column is single value.
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
    if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			InputStart_DateTime = TSCommandProcessorUtil.getDateTime ( InputStart, "InputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above.
			++warning_count;
		}
		try {
			InputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( InputEnd, "InputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above.
			++warning_count;
		}
    }

    List<TS> tslist = null; // List of time series that is read.
	try {
        boolean readData = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            readData = false;
        }

        // Read everything in the file (one time series or traces).
        List<String> errorMessages = new ArrayList<>();
        // Read the time series:
        // - if the location column has been specified then the table is single-column data
        boolean singleColumn = false;
        boolean blockData = false;
        if ( (LocationColumn != null) && !LocationColumn.isEmpty() ) {
            singleColumn = true;
        }
        if ( (BlockLayout != null) && !BlockLayout.isEmpty() ) {
        	blockData = true;
        }
        if ( blockData ) {
        	TimeInterval layoutColumns = TimeInterval.parseInterval(BlockLayoutColumns);
        	TimeInterval layoutRows = TimeInterval.parseInterval(BlockLayoutRows);
        	YearType yearType = YearType.valueOfIgnoreCase(BlockOutputYearType);
    		String dataType = (getDataType().size() == 1 ? getDataType().get(0) : null);
    		if ( (dataType != null) && (dataType.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
    			dataType = TSCommandProcessorUtil.expandParameterValue(processor, this, dataType);
    		}
        	tslist = readTimeSeriesListBlock ( table,
        		layoutColumns.getBase(), layoutRows.getBase(), yearType,
        		DateTimeColumn,
        		// Metadata provided as values.
        		(getLocationType().size() == 1 ? getLocationType().get(0) : null),
        		(getLocationIDRuntime().size() == 1 ? getLocationIDRuntime().get(0) : null),
                (getDataSource().size() == 1 ? getDataSource().get(0) : null),
                dataType,
                getInterval(),
                (getScenario().size() == 1 ? getScenario().get(0) : null),
                (getSequenceID().size() == 1 ? getSequenceID().get(0) : null),
                (getUnits().size() == 1 ? getUnits().get(0) : null),
                (getPrecision().size() == 1 ? getPrecision().get(0) : null),
                (getMissingValue().size() == 1 ? getMissingValue().get(0) : null),
        		// Metadata provided in columns.
        		LocationTypeColumn, LocationColumn, DataSourceColumn, DataTypeColumn, ScenarioColumn, SequenceIDColumn, UnitsColumn,
        		(getValueColumnsRuntime().size() == 1 ? getValueColumnsRuntime().get(0) : null),
        		readData, commandPhase, errorMessages );
        }
        else {
        	if ( singleColumn ) {
        		String dataType = (getDataType().size() == 1 ? getDataType().get(0) : null);
        		if ( (dataType != null) && (dataType.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
        			dataType = TSCommandProcessorUtil.expandParameterValue(processor, this, dataType);
        		}
	            tslist = readTimeSeriesListSingle ( table, getDateTimeColumnRuntime(),
	                DateTimeFormat, getDateColumnRuntime(),
	                getTimeColumnRuntime(), ValueColumn, FlagColumn, getSkipRows(),
	                LocationTypeColumn, LocationColumn, DataSourceColumn, DataTypeColumn, ScenarioColumn, SequenceIDColumn, UnitsColumn,
	                (getLocationType().size() == 1 ? getLocationType().get(0) : null),
	                (getDataSource().size() == 1 ? getDataSource().get(0) : null),
	                dataType, getInterval(), irregularIntervalPrecision,
	                (getScenario().size() == 1 ? getScenario().get(0) : null),
	                (getSequenceID().size() == 1 ? getSequenceID().get(0) : null),
	                (getUnits().size() == 1 ? getUnits().get(0) : null),
	                (getPrecision().size() == 1 ? getPrecision().get(0) : null),
	                getMissingValue(), handleDuplicatesHow,
	                InputStart_DateTime, InputEnd_DateTime, readData, commandPhase, errorMessages );
	        }
	        else {
	        	List<String> dataType = getDataType();
	        	int i = -1;
	        	for ( String d : dataType ) {
	        		++i;
	        		if ( (d != null) && (d.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
	        			d = TSCommandProcessorUtil.expandParameterValue(processor, this, d);
	        			dataType.set(i,d);
	        		}
	        	}
	            tslist = readTimeSeriesListMultiple ( table, getDateTimeColumnRuntime(),
	                DateTimeFormat, getDateColumnRuntime(),
	                getTimeColumnRuntime(), getValueColumnsRuntime(), getFlagColumnsRuntime(), getSkipRows(),
	                LocationTypeColumn, LocationColumn, DataSourceColumn, DataTypeColumn, ScenarioColumn, UnitsColumn,
	                getLocationType(), getLocationIDRuntime(), getDataSource(), dataType, getInterval(), irregularIntervalPrecision, getScenario(),
	                getUnits(), getPrecision(), getMissingValue(), handleDuplicatesHow,
	                InputStart_DateTime, InputEnd_DateTime, readData, commandPhase, errorMessages );
	        }
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

		// Add warnings.

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
            // Further process the time series:
            // - this makes sure the period is at least as long as the output period
            int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
            if ( wc > 0 ) {
                message = "Error post-processing series after read.";
                Message.printWarning ( warning_level, MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }

            // Now add the list in the processor.

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
    	if ( (tslist != null) && (tslist.size() > 0) ) {
    		// Have time series to add.
    		setDiscoveryTSList ( tslist );
    	}
    	else if ( (Alias != null) && !Alias.isEmpty() && (Alias.indexOf("%") < 0) ) {
    		// Add the alias:
    		// - ${property} is OK
    		// - however, cannot handle % specifiers because they are not the same in other commands
    		TS ts = new TS();
    		ts.setAlias(Alias);
    		List<TS> discoveryList = new ArrayList<>();
    		discoveryList.add(ts);
    		setDiscoveryTSList ( discoveryList );
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
Set the data type strings for each time series.
@param dataType data type for the time series
*/
private void setDataType ( List<String> dataType ) {
    __dataType = dataType;
}

/**
Set date column expanded for runtime.
@param dateColumnRuntime date column at run time
*/
private void setDateColumnRuntime ( String dateColumnRuntime ) {
    __dateColumnRuntime = dateColumnRuntime;
}

/**
Set the data source strings for each time series.
@param dataSource time series data source
*/
private void setDataSource ( List<String> dataSource ) {
    __dataSource = dataSource;
}

/**
Set date/time column expanded for runtime.
@param dateTimeColumnRuntime the date/time column at run time
*/
private void setDateTimeColumnRuntime ( String dateTimeColumnRuntime ) {
    __dateTimeColumnRuntime = dateTimeColumnRuntime;
}

/**
Set the list of time series read in discovery phase.
@param discovery_TS_List discovery time series list
*/
private void setDiscoveryTSList ( List<TS> discovery_TS_List ) {
    __discovery_TS_List = discovery_TS_List;
}

/**
Set the flag column names for each time series, expanded for runtime.
@param flagColumnsRuntime flag columns at run time
*/
private void setFlagColumnsRuntime ( List<String> flagColumnsRuntime ) {
    __flagColumnsRuntime = flagColumnsRuntime;
}

/**
Set the data interval for the time series.
@param interval time series data interval
*/
private void setInterval ( TimeInterval interval ) {
    __interval = interval;
}

/**
Set the location ID strings for each time series, expanded for runtime.
@param locationIdRuntime time series location identifier at run time
*/
private void setLocationIDRuntime ( List<String> locationIDRuntime ) {
    __locationIDRuntime = locationIDRuntime;
}

/**
Set the location type strings for each time series.
@param locationType time series location type
*/
private void setLocationType ( List<String> locationType ) {
    __locationType = locationType;
}

/**
Set the missing value strings.
@param missingValue time series missing value strings
*/
private void setMissingValue ( List<String> missingValue ) {
    __missingValue = missingValue;
}

/**
Set the precision strings for each time series.
@param precision time series data precision
*/
private void setPrecision ( List<String> precision ) {
    __precision = precision;
}

/**
Set the scenario strings for each time series.
@param scenario time series identifier scenario
*/
private void setScenario ( List<String> scenario ) {
    __scenario = scenario;
}

/**
Set the sequence ID strings for each time series.
@param sequenceID time series sequence identifier
*/
private void setSequenceID ( List<String> sequenceID ) {
    __sequenceID = sequenceID;
}

/**
Set the rows to skip (integer ranges).
@param skipRows the table rows to skip
*/
@SuppressWarnings("unused")
private void setSkipRows ( int[][] skipRows ) {
    __skipRows = skipRows;
}

/**
Set time column expanded for runtime.
@param timeColumnRuntime time column at run time
*/
private void setTimeColumnRuntime ( String timeColumnRuntime ) {
    __timeColumnRuntime = timeColumnRuntime;
}

/**
Set the data units strings for each time series.
@param units time series units
*/
private void setUnits ( List<String> units ) {
    __units = units;
}

/**
Set the value column names for each time series, expanded for runtime.
@param valueColumnsRunTime value columns at run time
*/
private void setValueColumnsRuntime ( List<String> valueColumnsRuntime ) {
    __valueColumnsRuntime = valueColumnsRuntime;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"TableID",
    	"DateTimeColumn",
    	"DateTimeFormat",
    	"DateColumn",
    	"TimeColumn",
    	"LocationTypeColumn",
    	"LocationColumn",
    	"DataSourceColumn",
    	"DataTypeColumn",
    	"ScenarioColumn",
    	"SequenceIDColumn",
    	"UnitsColumn",
    	"LocationType",
    	"LocationID",
    	"ValueColumn",
    	"FlagColumn",
    	//"SkipRows",
    	"DataSource",
    	"DataType",
    	"Interval",
    	"IrregularIntervalPrecision",
    	"Scenario",
    	"SequenceID",
    	"Units",
    	"Precision",
    	"MissingValue",
    	"HandleDuplicatesHow",
    	"Alias",
		"BlockLayout",
		"BlockLayoutColumns",
		"BlockLayoutRows",
		"BlockOutputYearType",
		"InputStart",
		"InputEnd"
	};
	return this.toString(parameters, parameterOrder);
}

}