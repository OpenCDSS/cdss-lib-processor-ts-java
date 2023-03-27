// ReadDelimitedFile_Command - This class initializes, checks, and runs the ReadDelimitedFile() command.

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

package rti.tscommandprocessor.commands.delimited;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
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
import RTi.Util.Time.DateTimeParser;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the ReadDelimitedFile() command.
*/
public class ReadDelimitedFile_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

protected final String _False = "False";
protected final String _True = "True";

/**
String that indicates that column names should be taken from the file.
For example FC(1:) indicates columns 1 through the total number of columns.
*/
protected final String _FC = "FC[";

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
Row ranges to skip (single rows will have same and start value.  Rows are 1+.
Determined while parsing command and cannot be modified at runtime.
*/
private int[][] __skipRows = null; // Allocated when checking parameters.

/**
Indicate whether to treat consecutive delimiters as one.
*/
private int __skipRowsAfterComments = -1;

/**
Indicate whether to treat consecutive delimiters as one.
*/
private boolean __treatConsecutiveDelimitersAsOne = false;

/**
Constructor.
*/
public ReadDelimitedFile_Command () {
	super();
	setCommandName ( "ReadDelimitedFile" );
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
    String routine = getClass().getSimpleName() + ".checkCommandParameters";
	String warning = "";
    String message;

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// Get the property values.
	String InputFile = parameters.getValue("InputFile");
    String Delimiter = parameters.getValue("Delimiter" );
    String TreatConsecutiveDelimitersAsOne = parameters.getValue("TreatConsecutiveDelimitersAsOne" );
    String ColumnNames = parameters.getValue("ColumnNames" );
    String DateTimeColumn = parameters.getValue("DateTimeColumn" );
    String DateColumn = parameters.getValue("DateColumn" );
    String TimeColumn = parameters.getValue("TimeColumn" );
    String Comment = parameters.getValue("Comment" );  // No checks, but needed to process column names.
    if ( Comment == null ) {
        Comment = "#"; // Need default for checks.
    }
    String SkipRows = parameters.getValue("SkipRows" );
    String SkipRowsAfterComments = parameters.getValue("SkipRowsAfterComments" );
    String Interval = parameters.getValue("Interval" );
    String Alias = parameters.getValue("Alias" );
	String InputStart = parameters.getValue("InputStart");
	String InputEnd = parameters.getValue("InputEnd");

    String InputFile_full = null;
    if ( (InputFile == null) || InputFile.isEmpty() ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify an existing input file." ) );
    }
    else if ( InputFile.indexOf("${") < 0 ){
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
                // Working directory is available so use it.
                if ( o != null ) {
                    working_dir = (String)o;
                }
            }
            catch ( Exception e ) {
                message = "Error requesting WorkingDir from processor.";
                warning += "\n" + message;
                Message.printWarning(3, routine, message );
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
            }

        try {
            InputFile_full = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
            File f = new File ( InputFile_full );
            if ( !f.exists() ) {
                message = "The input file does not exist:  \"" + InputFile_full + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the input file exists - may be OK if created at run time." ) );
            }
        }
        catch ( Exception e ) {
            message = "The input file:\n" + "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" + "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                     message, "Verify that input file and working directory paths are compatible." ) );
        }
    }

    if ( (Delimiter == null) || (Delimiter.length() == 0) ) {
        message = "The delimiter must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the delimiter indicating breaks between columns." ) );
    }

    if ( (TreatConsecutiveDelimitersAsOne != null) && !TreatConsecutiveDelimitersAsOne.equals("") &&
         !TreatConsecutiveDelimitersAsOne.equalsIgnoreCase(_False) &&
         !TreatConsecutiveDelimitersAsOne.equalsIgnoreCase(_True) ) {
        message = "The TreatConsecutiveDelimitersAsOne parameter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the parameter as " + _False + " (default) or " + _True + "." ) );
    }
    else {
        setTreatConsecutiveDelimitersAsOne ( Boolean.parseBoolean(TreatConsecutiveDelimitersAsOne) );
    }

    setSkipRows ( null );
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
    }

    if ( (SkipRowsAfterComments != null) && !SkipRowsAfterComments.equals("") ) {
        if ( !StringUtil.isInteger(SkipRowsAfterComments) || (Integer.parseInt(SkipRowsAfterComments) <= 0) ) {
            message = "The number of rows to skip after comments (" + SkipRowsAfterComments + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an integer > 0." ) );
        }
        else {
            setSkipRowsAfterComments ( Integer.parseInt(SkipRowsAfterComments) );
        }
    }

    // Column names need to be processed after the above have been specified.
    if ( (ColumnNames == null) || (ColumnNames.length() == 0) ) {
        message = "The column names must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify column names for all columns in the file." ) );
    }

    // Either DateTimeColumn or DateColumn must be specified.
    // If TimeColumn is specified, then DateTimeColumn must not be specified and DateColumn must be specified.
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

	// InputStart.
    DateTime InputStart_DateTime = null, InputEnd_DateTime = null;
	if ((InputStart != null) && !InputStart.isEmpty() && !InputStart.startsWith("${")) {
		try {
			InputStart_DateTime = DateTime.parse(InputStart);
		}
		catch (Exception e) {
            message = "The input start date/time \"" + InputStart + "\" is not valid.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid input start." ) );
		}
	}

	// InputEnd.
	if ((InputEnd != null) && !InputEnd.isEmpty() && !InputEnd.startsWith("${")) {
		try {
			InputEnd_DateTime = DateTime.parse(InputEnd);
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
	if ( InputStart_DateTime != null && InputEnd_DateTime != null ) {
		if ( InputStart_DateTime.greaterThanOrEqualTo( InputEnd_DateTime ) ) {
            message = InputStart + " (" + InputStart_DateTime  + ") should be less than InputEnd (" + InputEnd_DateTime + ").";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an input start less than the input end." ) );
		}
	}

    if (Alias != null && !Alias.equals("")) {
        if (Alias.indexOf(" ") > -1) {
            // Do not allow spaces in the alias.
            message = "The Alias value cannot contain any spaces.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Remove spaces from the alias." ) );
        }
    }

	// Check for invalid parameters.
    List<String> validList = new ArrayList<>(23);
    validList.add ( "InputFile" );
    validList.add ( "Delimiter" );
    validList.add ( "TreatConsecutiveDelimitersAsOne" );
    validList.add ( "Comment" );
    validList.add ( "SkipRows" );
    validList.add ( "SkipRowsAfterComments" );
    validList.add ( "ColumnNames" );
    validList.add ( "DateTimeColumn" );
    validList.add ( "DateTimeFormat" );
    validList.add ( "DateColumn" );
    validList.add ( "TimeColumn" );
    validList.add ( "ValueColumn" );
    validList.add ( "FlagColumn" );
    validList.add ( "LocationID" );
    validList.add ( "Provider" );
    validList.add ( "DataType" );
    validList.add ( "Interval" );
    validList.add ( "Scenario" );
    validList.add ( "Units" );
    validList.add ( "MissingValue" );
    validList.add ( "Alias" );
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
Determine the time series end date/time by reading the end of the file to determine the last date.
This opens a temporary connection, reads the end of the file, and parses out data to get the date.
*/
private DateTime determineEndDateTimeFromFile ( String inputFileFull, int dateTimePos, int datePos, int timePos,
    String delim, int parseFlag, DateTimeParser dateTimeParser )
throws FileNotFoundException, IOException
{   String routine = getClass().getName() + ".determineEndDateTimeFromFile";
    int dl = 30;
    if ( Message.isDebugOn ) {
        Message.printDebug( dl, routine, "Getting end date from end of file..." );
    }
    RandomAccessFile ra = new RandomAccessFile ( inputFileFull, "r" );
    long length = ra.length();
    // Skip to "blobLength" bytes from the end.  This should get some actual data lines.
    // Save in a temporary array in memory.
    // TODO SAM 2012-02-04 5000 characters may not be enough for files with very long lines
    //    (might need to put in a loop to make sure that at least one full line is read).
    int blobLength = 5000;
    if ( length >= blobLength ) {
        ra.seek ( length - blobLength );
    }
    else {
        // Otherwise just read from the top of the file to get all content.
        blobLength = (int)length;
    }
    byte[] b = new byte[blobLength];
    ra.read ( b );
    ra.close();
    ra = null;
    // Now break the bytes into records.
    String bs = new String ( b );
    List<String> v = StringUtil.breakStringList ( bs, "\n\r", StringUtil.DELIM_SKIP_BLANKS );
    // Loop through and figure out the last date.
    // Start at the second record because it is likely that a complete record was not found in the first record.
    int size = v.size();
    if ( Message.isDebugOn ) {
        Message.printDebug(1,routine,"Number of lines to check="+ size);
    }
    String string = null;
    List<String> tokens = null;
    // For small files less than the blob that is read, make sure to process single line files.
    // Otherwise, ignore the first line because it may be incomplete.
    int iStart = 1;
    if ( size == 1 ) {
        iStart = 0;
    }
    String dateTimeString = null;
    String dateString = null;
    String timeString = null;
    for ( int i = iStart; i < size; i++ ) {
        string = (v.get(i)).trim();
        if ( Message.isDebugOn ) {
            Message.printDebug(30, routine, "Line to parse=\"" + string + "\"" );
        }
        if ( (string.length() == 0) || (string.charAt(0) == '#') || (string.charAt(0) == '<') ) {
            // Ignore blank lines, comments, and lines with HTML-enclosing tags.
            continue;
        }
        tokens = StringUtil.breakStringList( string, delim, parseFlag | StringUtil.DELIM_ALLOW_STRINGS );
        if ( dateTimePos >= 0 ) {
            // Set the date string - overwrite for each line until all the end lines are processed.
            dateTimeString = tokens.get(dateTimePos);
        }
        else if ( datePos >= 0 ) {
            dateString = tokens.get(datePos);
            if ( timePos >= 0 ) {
                timeString = tokens.get(timePos);
                dateTimeString = dateString + ":" + timeString;
            }
            else {
                dateTimeString = dateString;
            }
        }
        else {
            Message.printWarning(3,routine,"A column for date/time or date has not been specified." );
            return null;
        }
    }
    //Message.printStatus(2,routine,"Date/time string to parse=\""+ dateTimeString + "\"");
    // Whatever came out of the last record will remain as the last record, which is examined.
    DateTime datetime = null;
    try {
        if ( Message.isDebugOn ) {
            Message.printDebug( 2, routine, "Got end date/time string \"" + dateTimeString + "\" from line: " + string );
        }
        if ( dateTimeParser == null ) {
            datetime = DateTime.parse(dateTimeString);
        }
        else {
            datetime = dateTimeParser.parse(null,dateTimeString);
        }
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, "Error parsing end date/time \"" + dateTimeString + "\" from file \"" +
            inputFileFull + "\"." );
        datetime = null;
    }
    return datetime;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return ( new ReadDelimitedFile_JDialog ( parent, this ) ).ok();
}

/**
Return the runtime column names list.
@param ColumnNames the names of the columns in the file, single value or separated by columns.
It is expected that this has already been expanded for ${Property}.
*/
private List<String> getColumnNamesRuntime(String ColumnNames, String inputFileFull,
	String delimiter, String comment, int [][] skipRows, int skipRowsAfterComments,
	String routine, CommandStatus status, List<String> problems )
{	String message;
	List<String> columnNames = new ArrayList<>();
	if ( ColumnNames == null ) {
		return columnNames;
	}
    columnNames = StringUtil.breakStringList ( ColumnNames, ",", StringUtil.DELIM_ALLOW_STRINGS );
    // If the column names include information from the file, get the column names from the file.
    List<String> columnNamesRuntime = new ArrayList<>();
    if ( StringUtil.indexOfIgnoreCase(ColumnNames,_FC, 0) >= 0 ) {
        // Original string used slice notation for column names in file.
        try {
            columnNamesRuntime = readColumnNamesFromFile(inputFileFull, columnNames,
                StringUtil.literalToInternal(delimiter), comment, getSkipRows(),
                getSkipRowsAfterComments() );
        }
        catch ( Exception e ) {
            message = "Error getting the column names to use for runtime processing (" + e + ").";
            problems.add(message);
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the column names are specified using valid syntax." ) );
            Message.printWarning( 3, routine, e );
        }
    }
    else {
        // Just use the column names as specified for runtime.
        columnNamesRuntime = columnNames;
    }
    return columnNamesRuntime;
}

/**
Get the column number (0+) from the column names.
@param columnName column name of interest
@param columnNames names of all the columns in the file
@return the column number (0+) corresponding to the column name, or -1 if not found.
*/
private int getColumnNumberFromName ( String columnName, List<String> columnNames )
{   if ( (columnName == null) || columnName.isEmpty() || (columnNames == null) ) {
        return -1;
    }
    for ( int i = 0; i < columnNames.size(); i++ ) {
        if ( columnName.equalsIgnoreCase(columnNames.get(i)) ) {
            return i;
        }
    }
    String routine = getClass().getSimpleName() + ".getColumnNumberFromName";
    Message.printWarning(3, routine, "Unable to find column name \"" + columnName + "\" in choices." );
    return -1;
}

/**
Get the column numbers (0+) for a list of names.
@param someNames column names of interest.
@param columnNames names of all the columns.
@return the column number (0+) corresponding to the each column name, or -1 if not found, as an array
for each column.
*/
private int[] getColumnNumbersFromNames ( List<String> someNames, List<String> columnNames ) {
    int [] columns = new int[someNames.size()];
    for ( int i = 0; i < columns.length; i++ ) {
        columns[i] = getColumnNumberFromName ( someNames.get(i), columnNames );
    }
    return columns;
}

/**
Return the data type list.
*/
private List<String> getDataTypeRuntime(String DataType,
	List<String> valueColumnsRuntime,String routine, CommandStatus status, List<String> problems)
{	String message;
    List<String> dataType = new ArrayList<>();
    if ( (DataType == null) || DataType.equals("") ) {
        // Set to the same as the value columns.
        for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
            dataType.add ( valueColumnsRuntime.get(i) );
        }
    }
    else {
        // Can have one value that is re-used, or DataType for each time series.
        List<String>tokens = StringUtil.breakStringList(DataType, ",", 0);
        if ( (tokens.size() != 1) && (tokens.size() != valueColumnsRuntime.size()) ) {
            message = "The number of DataType strings (" + tokens.size() + ") is invalid - expecting " +
                valueColumnsRuntime.size();
            problems.add(message);
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + valueColumnsRuntime.size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
                dataType.add ( "" );
            }
        }
        else {
            // Process the DataType for each time series.
            for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    dataType.add(tokens.get(0));
                }
                else {
                    dataType.add(tokens.get(i));
                }
            }
        }
    }
    return dataType;
}

/**
Return the date column name expanded for runtime.
*/
private String getDateColumnRuntime ( String DateColumn, String inputFileFull, List<String> columnNamesRuntime,
	String delimiter, String comment, int [][] skipRows, int skipRowsAfterComments,
	String routine, CommandStatus status, List<String> problems )
{	String message;
    if ( (DateColumn == null) || DateColumn.isEmpty() ) {
    	return "";
    }
    String dateColumnRuntime = "";
    if ( StringUtil.indexOfIgnoreCase(DateColumn,_FC, 0) >= 0 ) {
        // Original string used slice notation for column name in file.
        try {
            List<String> dateColumnName = new ArrayList<>();
            dateColumnName.add ( DateColumn ); // Only one
            dateColumnName = readColumnNamesFromFile(inputFileFull, dateColumnName,
                StringUtil.literalToInternal(delimiter), comment, skipRows,
                skipRowsAfterComments );
            dateColumnRuntime = dateColumnName.get(0);
        }
        catch ( Exception e ) {
            message = "Error getting the date column name to use for runtime processing (" + e + ").";
            problems.add(message);
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
    // Now check the value of the date column versus the available columns.
    if ( getColumnNumberFromName(dateColumnRuntime, columnNamesRuntime) < 0 ) {
        message = "The DateColumn (" + dateColumnRuntime + ") is not a recognized column name.";
        problems.add(message);
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a date column as one of the values from ColumnNames." ) );
    }
    return dateColumnRuntime;
}

/**
Return the date/time column name expanded for runtime.
@param DateTimeColumn date/time column name in file, assumed to already have been expanded from ${Property}.
*/
private String getDateTimeColumnRuntime ( String DateTimeColumn, String inputFileFull, List<String> columnNamesRuntime,
	String delimiter, String comment, int [][] skipRows, int skipRowsAfterComments,
	String routine, CommandStatus status, List<String> problems )
{	String message;
	String dateTimeColumnRuntime = "";
    if ( (DateTimeColumn != null) && !DateTimeColumn.isEmpty() ) {
        if ( StringUtil.indexOfIgnoreCase(DateTimeColumn,_FC, 0) >= 0 ) {
            // Original string used slice notation for column name in file.
            try {
                List<String> dateTimeColumnName = new ArrayList<>();
                dateTimeColumnName.add ( DateTimeColumn ); // Only one
                dateTimeColumnName = readColumnNamesFromFile(inputFileFull, dateTimeColumnName,
                    StringUtil.literalToInternal(delimiter), comment, skipRows,
                    skipRowsAfterComments );
                dateTimeColumnRuntime = dateTimeColumnName.get(0);
            }
            catch ( Exception e ) {
                message = "Error getting the date/time column name to use for runtime processing (" + e + ").";
                problems.add(message);
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the date/time column name is specified using valid syntax." ) );
                Message.printWarning( 3, routine, e );
            }
        }
        else {
            // Just use the column name as specified.
            dateTimeColumnRuntime = DateTimeColumn;
        }
        // Now check the value of the date/time column versus the available columns in the file.
        if ( getColumnNumberFromName(dateTimeColumnRuntime, columnNamesRuntime) < 0 ) {
            message = "The DateTimeColumn (" + dateTimeColumnRuntime + ") is not a recognized column name.";
            problems.add(message);
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a date/time column as one of the values from ColumnNames." ) );
        }
    }
    return dateTimeColumnRuntime;
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList () {
    return __discovery_TS_List;
}

/**
Return the flag column list, expanded for runtime.
*/
private List<String> getFlagColumnsRuntime(String FlagColumn, String inputFileFull, List<String> columnNamesRuntime,
	List<String> valueColumnsRuntime,
	String delimiter, String comment, int [][] skipRows, int skipRowsAfterComments,
	String routine, CommandStatus status, List<String> problems )
{	String message;
    List<String> flagColumnsRuntime = new ArrayList<>();
    if ( (FlagColumn != null) && (FlagColumn.length() != 0) ) {
    	List<String> flagColumns = StringUtil.breakStringList(FlagColumn, ",", StringUtil.DELIM_ALLOW_STRINGS );
        if ( StringUtil.indexOfIgnoreCase(FlagColumn,_FC, 0) >= 0 ) {
            // Original string used slice notation for column name in file.
            try {
                flagColumnsRuntime = readColumnNamesFromFile(inputFileFull, flagColumns,
                    StringUtil.literalToInternal(delimiter), comment, skipRows,
                    skipRowsAfterComments );
            }
            catch ( Exception e ) {
                message = "Error getting the flag column name(s) to use for runtime processing (" + e + ").";
                problems.add(message);
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
        // Now check for valid column names (flag column is allowed to be blank if not used for a value).
        for ( String flagColumnRuntime : flagColumnsRuntime ) {
        	if ( !flagColumnRuntime.isEmpty() ) {
	            if ( getColumnNumberFromName(flagColumnRuntime, columnNamesRuntime) < 0 ) {
	                message = "The FlagColumn (" + flagColumnRuntime + ") is not a recognized column name.";
	                problems.add(message);
	                status.addToLog ( CommandPhaseType.INITIALIZATION,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Specify flag column(s) matching ColumnNames, separated by commas." ) );
	            }
        	}
        }
    }
    if ( (valueColumnsRuntime.size() > 0) && (flagColumnsRuntime.size() > 0) &&
        (valueColumnsRuntime.size() != flagColumnsRuntime.size()) ) {
        message = "The number of flag column names (" + flagColumnsRuntime.size() +
            ") does not match the number of value column names (" + valueColumnsRuntime.size() + ").";
        problems.add(message);
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify " + valueColumnsRuntime.size() + " flag column names separated by commas." ) );
    }
    return flagColumnsRuntime;
}

/**
Return the data interval.
*/
private TimeInterval getInterval() {
    return __interval;
}

/**
Return the location ID list, expanded for runtime.
*/
private List<String> getLocationIDRuntime(String LocationID, String inputFileFull, List<String> columnNamesRuntime,
	List<String> valueColumnsRuntime,
	String delimiter, String comment, int [][] skipRows, int skipRowsAfterComments,
	String routine, CommandStatus status, List<String> problems)
{	String message;
    List<String> locationIDRuntime = new ArrayList<>();
    if ( (LocationID == null) || LocationID.equals("") ) {
        message = "The location ID column(s) must be specified.";
        problems.add(message);
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify location ID column(s) 1+ values from ColumnNames, separated by commas." ) );
    }
    else {
        // Can have one value that is re-used, or LocationID for each time series.
        List<String>tokens = StringUtil.breakStringList(LocationID, ",", 0);
        if ( StringUtil.indexOfIgnoreCase(LocationID,_FC, 0) >= 0 ) {
            // Original string used slice notation for column name in file.
            try {
                tokens = readColumnNamesFromFile(inputFileFull, tokens,
                    StringUtil.literalToInternal(delimiter), comment, getSkipRows(),
                    getSkipRowsAfterComments() );
            }
            catch ( Exception e ) {
                message = "Error getting the location ID(s) to use for runtime processing (" + e + ").";
                problems.add(message);
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the location ID(s) are specified using valid syntax." ) );
                Message.printWarning( 3, routine, e );
            }
        }
        if ( (tokens.size() != 1) && (tokens.size() != valueColumnsRuntime.size()) ) {
            message = "The number of LocationID strings (" + tokens.size() + ") is invalid - expecting " +
                valueColumnsRuntime.size();
            problems.add(message);
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + valueColumnsRuntime.size() +
                    "), separated by commas." ) );
            // Set to empty strings to simplify runtime error handling.
            for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
                locationIDRuntime.add ( "" );
            }
        }
        else {
            // Process the LocationID for each time series.
            for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    locationIDRuntime.add(tokens.get(0));
                }
                else {
                    locationIDRuntime.add(tokens.get(i));
                }
            }
        }
    }
    return locationIDRuntime;
}

/**
Return the list of data objects read by this object in discovery mode.
Classes that can be requested: TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
    List<TS> discovery_TS_List = getDiscoveryTSList ();
    if ( (discovery_TS_List == null) || (discovery_TS_List.size() == 0) ) {
        return null;
    }
    // Since all time series in file must have the same interval, check the first time series (e.g., MonthTS).
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
Return the provider list.
*/
private List<String> getProviderRuntime(String Provider,
	List<String> valueColumnsRuntime,String routine, CommandStatus status, List<String> problems)
{	String message;
    List<String> provider = new ArrayList<>();
    if ( (Provider != null) && !Provider.equals("") ) {
        // Can have one value that is re-used, or Provider for each time series.
        List<String>tokens = StringUtil.breakStringList(Provider, ",", 0);
        if ( (tokens.size() != 1) && (tokens.size() != valueColumnsRuntime.size()) ) {
            message = "The number of Provider strings (" + tokens.size() + ") is invalid - expecting " +
                valueColumnsRuntime.size();
            problems.add(message);
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + valueColumnsRuntime.size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
                provider.add ( "" );
            }
        }
        else {
            // Process the Provider for each time series.
            for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    provider.add(tokens.get(0));
                }
                else {
                    provider.add(tokens.get(i));
                }
            }
        }
    }
    else {
        for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
            provider.add ( "" );
        }
    }
    return provider;
}

/**
Return the scenario list.
*/
private List<String> getScenarioRuntime(String Scenario,
	List<String> valueColumnsRuntime,String routine, CommandStatus status, List<String> problems)
{	String message;
    List<String> scenario = new ArrayList<>();
    if ( (Scenario != null) && !Scenario.equals("") ) {
        // Can have one value that is re-used, or Scenario for each time series.
        List<String>tokens = StringUtil.breakStringList(Scenario, ",", 0);
        if ( (tokens.size() != 1) && (tokens.size() != valueColumnsRuntime.size()) ) {
            message = "The number of Scenario strings (" + tokens.size() + ") is invalid - expecting " +
                valueColumnsRuntime.size();
            problems.add(message);
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + valueColumnsRuntime.size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
                scenario.add ( "" );
            }
        }
        else {
            // Process the Scenario for each time series.
            for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    scenario.add(tokens.get(0));
                }
                else {
                    scenario.add(tokens.get(i));
                }
            }
        }
    }
    else {
        for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
            scenario.add ( "" );
        }
    }
    return scenario;
}

/**
Return the number of rows to skip (ranges).
*/
private int[][] getSkipRows() {
    return __skipRows;
}

/**
Return the number of rows to skip after comments.
*/
private int getSkipRowsAfterComments() {
    return __skipRowsAfterComments;
}

/**
Return the time column name expanded for runtime.
*/
private String getTimeColumnRuntime ( String TimeColumn, String inputFileFull, List<String> columnNamesRuntime,
	String delimiter, String comment, int [][] skipRows, int skipRowsAfterComments,
	String routine, CommandStatus status, List<String> problems )
{	String message;

	if ( (TimeColumn == null) || TimeColumn.isEmpty() ) {
		return "";
	}
	String timeColumnRuntime = "";
    if ( StringUtil.indexOfIgnoreCase(TimeColumn,_FC, 0) >= 0 ) {
        // Original string used slice notation for column name in file.
        try {
            List<String> timeColumnName = new ArrayList<>();
            timeColumnName.add ( TimeColumn ); // Only one.
            timeColumnName = readColumnNamesFromFile(inputFileFull, timeColumnName,
                StringUtil.literalToInternal(delimiter), comment, skipRows,
                skipRowsAfterComments );
            timeColumnRuntime = timeColumnName.get(0);
        }
        catch ( Exception e ) {
            message = "Error getting the time column name to use for runtime processing (" + e + ").";
            problems.add(message);
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
    // Now check the value of the time column versus the available columns.
    if ( getColumnNumberFromName(timeColumnRuntime, columnNamesRuntime) < 0 ) {
        message = "The TimeColumn (" + timeColumnRuntime + ") is not a recognized column name.";
        problems.add(message);
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a time column as one of the values from ColumnNames." ) );
    }
    return timeColumnRuntime;
}

/**
Return whether consecutive delimiters should be treated as one.
*/
private boolean getTreatConsecutiveDelimitersAsOne() {
    return __treatConsecutiveDelimitersAsOne;
}

/**
Return the data units list.
*/
private List<String> getUnitsRuntime(String Units,
		List<String> valueColumnsRuntime,String routine, CommandStatus status, List<String> problems)
{	String message;
    List<String> units = new ArrayList<>();
    if ( (Units != null) && !Units.equals("") ) {
        // Can have one value that is re-used, or units for each time series.
        List<String>tokens = StringUtil.breakStringList(Units, ",", 0);
        if ( (tokens.size() != 1) && (tokens.size() != valueColumnsRuntime.size()) ) {
            message = "The number of units strings (" + tokens.size() + ") is invalid - expecting " +
                valueColumnsRuntime.size();
            problems.add(message);
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + valueColumnsRuntime.size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
                units.add ( "" );
            }
        }
        else {
            // Process the units for each time series.
            for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    units.add(tokens.get(0));
                }
                else {
                    units.add(tokens.get(i));
                }
            }
        }
    }
    else {
        for ( int i = 0; i < valueColumnsRuntime.size(); i++ ) {
            units.add ( "" );
        }
    }
    return units;
}

/**
Return the value column list, expanded for runtime.
*/
private List<String> getValueColumnsRuntime(String ValueColumn, String inputFileFull, List<String> columnNamesRuntime,
	String delimiter, String comment, int [][] skipRows, int skipRowsAfterComments,
	String routine, CommandStatus status, List<String> problems)
{	String message;
    List<String> valueColumnsRuntime = new ArrayList<>();
    if ( (ValueColumn == null) || (ValueColumn.length() == 0) ) {
        message = "The value column(s) must be specified.";
        problems.add(message);
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify data column(s) from ColumnNames, separated by commas." ) );
    }
    else {
    	List<String> valueColumns = StringUtil.breakStringList(ValueColumn, ",", StringUtil.DELIM_ALLOW_STRINGS );
        if ( StringUtil.indexOfIgnoreCase(ValueColumn,_FC, 0) >= 0 ) {
            // Original string used slice notation for column name in file.
            try {
                valueColumnsRuntime = readColumnNamesFromFile(inputFileFull, valueColumns,
                    StringUtil.literalToInternal(delimiter), comment, getSkipRows(),
                    getSkipRowsAfterComments() );
            }
            catch ( Exception e ) {
                message = "Error getting the value column name(s) to use for runtime processing (" + e + ").";
                problems.add(message);
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
        // Now check for valid column names.
        for ( String valueColumnRuntime : valueColumnsRuntime ) {
            if ( getColumnNumberFromName(valueColumnRuntime, columnNamesRuntime) < 0 ) {
                message = "The ValueColumn (" + valueColumnRuntime + ") is not a recognized column name.";
                problems.add(message);
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify value column(s) matching ColumnNames, separated by commas." ) );
            }
        }
    }
    return valueColumnsRuntime;
}

/**
Determine if a row needs to be skipped, based on command parameters.
@param row line from file being processed(1+)
@param firstNonHeaderRow row in the file that is the first after the header comments
@param skipRows ranges of rows (1+ each) that are to be skipped
@param skipRowsAfterComments the number of rows after the header comments to be skipped
*/
private boolean needToSkipRow( int row, int firstNonHeaderRow, int[][] skipRows, int skipRowsAfterComments ) {
    if ( skipRows != null ) {
        // First check the absolute skips.
        for ( int ipair = 0; ipair < skipRows.length; ipair++ ) {
            if ( (row >= skipRows[ipair][0]) && (row <= skipRows[ipair][1])) {
                // Skipping the absolute rows
                //Message.printStatus ( 2, "", "skipping absolute row " + row );
                return true;
            }
        }
    }
    // Check to see if in the rows after the header comments.
    if ( (firstNonHeaderRow > 0) && (skipRowsAfterComments > 0) ) {
        int startSkip = firstNonHeaderRow;
        int endSkip = firstNonHeaderRow + skipRowsAfterComments - 1;
        if ( (row >= startSkip) && (row <= endSkip) ) {
            //Message.printStatus ( 2, "", "Skipping row " + row + " after comments, firstNonHeaderRow=" +
            //    firstNonHeaderRow + ", startSkip=" + startSkip + ", endSkip=" + endSkip + " row=" + row );
            return true;
        }
    }
    return false;
}

// Use the base class parseCommand() method.

// TODO SAM 2010-05-24 Evaluate making code more modular so as to not repeat this code from main read method.
/**
Read the column names from the file.  This code is essentially a copy of some of the code used when
actually processing the time series and should be kept consistent.  It mainly is concerned with handling
initial comments in the file, skipped rows, and reading the first non-comment record as a header.
@param inputFileFull the full path to the input file.
@param columnNames0 the value of the ColumnNames parameter before special handling.  For example, this may contain
FC[] notation.
*/
protected List<String> readColumnNamesFromFile ( String inputFileFull, List<String> columnNames0, String delim,
    String commentChar, int[][] skipRows, int skipRowsAfterComments )
throws IOException
{   String routine = getClass().getSimpleName() + ".readColumnNamesFromFile";
    List<String> columnNames = new ArrayList<>();
    BufferedReader in = null;
    Message.printStatus(2, routine, "Getting the column names from file \"" + inputFileFull + "\"" );
    in = new BufferedReader ( new InputStreamReader(IOUtil.getInputStream ( inputFileFull )) );
    String s, sTrimmed;
    List<String> columnHeadingList = null;
    int nColumnHeadings = 0;
    int row = 0;
    boolean rowIsComment = false;
    int firstNonHeaderRow = -1; // First line that is not a header comment (1+).
    int dl = 10;
    int breakFlag = 0;
    while ( true ) {
        // Read a line and deal with skipping.
        s = in.readLine();
        if ( s == null ) {
            // No more data.
            break;
        }
        // Else handle the line.
        ++row;
        //Message.printStatus(2, routine, "Processing line " + row + ": " + s );
        if ( Message.isDebugOn ) {
            Message.printDebug(dl, routine, "Processing line " + row + ": " + s );
        }
        rowIsComment = false;
        sTrimmed = s.trim();
        // Skip in the range of rows being skipped - this basically throws out rows without evaluating
        // Don't even know if it is a comment.
        if ( needToSkipRow( row, firstNonHeaderRow, skipRows, skipRowsAfterComments ) ) {
            Message.printStatus(2, routine, "Skipping row " + row );
            continue;
        }
        if ( (sTrimmed.length() == 0) || (commentChar.indexOf(s.charAt(0)) >= 0) ) {
            rowIsComment = true;
        }
        // Skip rows first, in particular user-specified skips before evaluating for the first non-comment row.
        if ( rowIsComment || needToSkipRow( row, firstNonHeaderRow, skipRows, skipRowsAfterComments ) ) {
            Message.printStatus(2, routine, "2 Skipping row " + row );
            continue;
        }
        Message.printStatus(2, routine, "Line is not a comment and is not being skipped." );
        if ( !rowIsComment ) {
            if ( firstNonHeaderRow < 0 ) {
                // This is the first non-comment data record.
                firstNonHeaderRow = row;
                if ( Message.isDebugOn ) {
                    Message.printDebug(dl, routine, "Found first non-comment (non-skipped) line at row " +
                        firstNonHeaderRow + ": " + s );
                }
            }
        }
        // Check again in case the first non-header line is detected.
        if ( rowIsComment || needToSkipRow( row, firstNonHeaderRow, skipRows, skipRowsAfterComments ) ) {
            if ( Message.isDebugOn ) {
                Message.printDebug(dl, routine, "Skipping the row because needToSkipRow()=true" );
            }
            continue;
        }
        // Else continue reading data records from the file - this will be the file header with column names.
        // First break the row (allow quoted strings since headers).
        Message.printStatus(2, routine, "Parsing the line to get column names." );
        columnHeadingList = StringUtil.breakStringList ( s, delim, breakFlag | StringUtil.DELIM_ALLOW_STRINGS );
        if ( columnHeadingList == null ) {
            nColumnHeadings = 0;
        }
        else {
            nColumnHeadings = columnHeadingList.size();
        }
        // Loop through original column name tokens and, as requested, expand to what is in the file.
        for ( String columnName0 : columnNames0 ) {
            if ( StringUtil.startsWithIgnoreCase(columnName0,_FC) ) {
                // Need to process the column names from the file.
                int parenPos1 = columnName0.indexOf(_FC);
                int parenPos2 = columnName0.indexOf("]");
                if ( (parenPos1 >= 0) && (parenPos2 >= 0) ) {
                    // Need to interpret slice of field numbers in file.
                    String slice = columnName0.substring((parenPos1 + _FC.length()),parenPos2);
                    int [] fileColPos = StringUtil.parseIntegerSlice( slice, ":", 0, nColumnHeadings );
                    Message.printStatus(2, routine, "Got " + fileColPos.length + " columns from slice \"" + slice + "\"" );
                    for ( int ipos = 0; ipos <fileColPos.length; ipos++ ) {
                        // Positions from parameter parsing are 1+ so need to decrement to get 0+ indices.
                        Message.printStatus(2, routine, "Adding file column name \"" + columnHeadingList.get(fileColPos[ipos] - 1).trim() + "\"" );
                        columnNames.add ( columnHeadingList.get(fileColPos[ipos] - 1).trim() );
                    }
                }
                else {
                    // Use all the file field names.
                    for ( int ipos = 0; ipos <nColumnHeadings; ipos++ ) {
                        Message.printStatus(2, routine, "Adding file column name \"" + columnHeadingList.get(ipos).trim() + "\"" );
                        columnNames.add ( columnHeadingList.get(ipos).trim() );
                    }
                }
            }
            else {
                // A literal string that can be used as is.
                Message.printStatus(2, routine, "Adding user-specified column name \"" + columnName0 + "\"" );
                columnNames.add ( columnName0 );
            }
        }
        // Currently headers in files can only be one row so break out of reading.
        break;
    }
    in.close();
    return columnNames;
}

/**
Read a list of time series from a delimited file.
@param inputFileFull the full path to the input file.
@param delim delimiter character(s).
@param treatConsecutiveDelimitersAsOne indicate whether consecutive delimiter characters should be treated as one.
@param columnNames names of columns to use when mapping columns
@param doReadColumnNamesFromFile if true, then the column names will be read from the file
@param dateTimeFormat the date/time format, if not determined automatically
@param dateTimeColumn the date/time column name
@param dateColumn the date column name
@param timeColumn the time column name
@param valueColumns the data value column names
@param commentChar character(s) that indicates comments lines, if the first character of a line
(or null if not specified).  Only 1-character is checked but more than one special character can be indicated.
@param skipRows ranges of rows (1+ each) that are to be skipped
@param skipRowsAfterComments the number of rows after the header comments to be skipped
@param ids list of location identifiers to use for time series
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
private List<TS> readTimeSeriesList ( String inputFileFull,
    String delim, boolean treatConsecutiveDelimitersAsOne,
    List<String> columnNames, boolean doReadColumnNamesFromFile, String dateTimeColumn, String dateTimeFormat,
    String dateColumn, String timeColumn, List<String> valueColumns, List<String> flagColumns,
    String commentChar, int[][] skipRows, int skipRowsAfterComments,
    List<String> ids, List<String> providers, List<String> datatypes, TimeInterval interval,
    List<String> scenarios, List<String> units, List<String> missing,
    DateTime inputStartReq, DateTime inputEndReq,
    boolean readData, List<String> errorMessages )
throws IOException
{   String routine = getClass().getSimpleName() + ".readTimeSeriesList";
    // Allocate the list.
    List<TS> tslist = new ArrayList<>();
    // Open the file
    BufferedReader in = null;
    in = new BufferedReader ( new InputStreamReader(IOUtil.getInputStream ( inputFileFull )) );
    // Translate column names to integer values to speed up processing below - these have been expanded for runtime.
    int dateTimePos = getColumnNumberFromName(dateTimeColumn,columnNames);
    if ( Message.isDebugOn ) {
    	Message.printDebug(2,routine,"Column (0+) for date/time column \"" + dateTimeColumn + "\" is " + dateTimePos );
    }
    DateTimeParser dateTimeParser = null;
    if ( (dateTimeFormat != null) && !dateTimeFormat.trim().equals("") ) {
        // Set to null to simplify logic below.
        dateTimeParser = new DateTimeParser ( dateTimeFormat );
    }
    int datePos = getColumnNumberFromName(dateColumn,columnNames);
    if ( Message.isDebugOn ) {
    	Message.printDebug(2,routine,"Column (0+) for date column \"" + dateColumn + "\" is " + datePos );
    }
    int timePos = getColumnNumberFromName(timeColumn,columnNames);
    if ( Message.isDebugOn ) {
    	Message.printDebug(2,routine,"Column (0+) for time column \"" + timeColumn + "\" is " + timePos );
    }
    int [] valuePos = getColumnNumbersFromNames(valueColumns,columnNames);
    for ( int i = 0; i < valuePos.length; i++ ) {
    	if ( Message.isDebugOn ) {
    		Message.printDebug(2,routine,"Column (0+) for value \"" + valueColumns.get(i) + "\" is " + valuePos[i] );
    	}
    }
    // For simplification, make flagPos the same length as valuePos and set to -1 if flags not used.
    int [] flagPos = null;
    if ( (flagColumns == null) || (flagColumns.size() == 0) ) {
        flagPos = new int[valuePos.length];
        for ( int i = 0; i < flagPos.length; i++ ) {
            flagPos[i] = -1;
        }
    }
    else {
        flagPos = getColumnNumbersFromNames(flagColumns,columnNames);
        for ( int i = 0; i < flagPos.length; i++ ) {
        	if ( Message.isDebugOn ) {
        		Message.printDebug(2,routine,"Column (0+) for flag \"" + flagColumns.get(i) + "\" is " + flagPos[i] );
        	}
        }
    }
    Message.printStatus(2, routine, "Date/time column=" + dateTimePos + " date column=" +
        datePos + " time column=" + timePos );
    // Create the time series - at this time meta-data are not read from the file.
    // If this changes, then some values may need to be (re)set when the file is read.
    DateTime earliest = null, latest = null; // Make sure these are initialized to null.
    boolean periodRequested = false;
    for ( int its = 0; its < valuePos.length; its++ ) {
        TSIdent tsident = null;
        TS ts = null;
        String tsidentstr = ids.get(its) + "." + providers.get(its) + "." + datatypes.get(its) + "." +
            interval + "." + scenarios.get(its);
        if ( valuePos[its] < 0 ) {
            // Was a problem looking up column numbers.
            errorMessages.add ( "Value column name \"" +
                valueColumns.get(its) + "\" does not match known data columns (" +
                StringUtil.toString(columnNames,",") + ") - will not read.");
        }
        else {
            try {
                tsident = new TSIdent( tsidentstr );
            }
            catch ( Exception e ) {
                tsident = null;
                errorMessages.add ( "Error initializing time series \"" + tsidentstr +
                    "\" (" + e + ") - will not read.");
            }
            if ( tsident != null ) {
                try {
                    ts = TSUtil.newTimeSeries( tsident.toString(), true );
                    // Set all the information.
                    ts.setIdentifier ( tsident );
                    ts.setDescription ( ids.get(its) + " " + datatypes.get(its) );
                    ts.setDataUnits ( units.get(its) );
                    ts.setDataUnitsOriginal ( units.get(its) );
                    ts.setMissing ( Double.NaN );
                    ts.setInputName ( inputFileFull );
                    if ( inputStartReq != null ) {
                        ts.setDate1(inputStartReq);
                        ts.setDate1Original(inputStartReq); // Will get reset.
                    }
                    if ( inputEndReq != null ) {
                        ts.setDate2(inputEndReq);
                        ts.setDate2Original(inputEndReq); // Will get reset.
                    }
                    if ( (inputStartReq != null) && (inputEndReq != null) ) {
                        periodRequested = true;
                    }
                }
                catch ( Exception e ) {
                    // Set the TS to null to match the column positions but won't be able to set data below.
                    ts = null;
                    errorMessages.add ( "Error initializing time series \"" +
                        tsidentstr + "\" (" + e + ") - will not read.");
                }
            }
        }
        // Always add, even if null.
        tslist.add ( ts );
    }
    // Loop through reading the rows from the file, perform actions as needed.
    int row = 0;
    try {
        String s, sTrimmed;
        TS ts = null; // Time series to read.
        boolean rowIsComment;
        List<String> tokens;
        String dateTimeString = null, dateString, timeString, valueString;
        DateTime dateTime;
        int breakFlag = 0;
        if ( treatConsecutiveDelimitersAsOne ) {
            breakFlag = StringUtil.DELIM_SKIP_BLANKS;
        }
        int firstNonHeaderRow = -1; // First line that is not a header comment (1+).
        int ival; // Index for values on line - reused.
        int dataRowCount = 0; // Count of data rows that are processed.
        int dl = 10;
        int ntokens = 0; // Number of tokens parsed from a line.
        double value; // Data value.
        String flag; // Data flag.
        int dtErrorCount = 0; // Count of errors parsing date/times.
        while ( true ) {
            // Read a line and deal with skipping.
            s = in.readLine();
            if ( s == null ) {
                // No more data.
                break;
            }
            // Else handle the line.
            ++row;
            if ( Message.isDebugOn ) {
                Message.printDebug(dl, routine, "Processing line " + row + ": " + s );
            }
            rowIsComment = false;
            sTrimmed = s.trim();
            // Skip in the range of rows being skipped - this basically throws out rows without evaluating
            // Don't even know if it is a comment.
            if ( needToSkipRow( row, firstNonHeaderRow, skipRows, skipRowsAfterComments ) ) {
                continue;
            }
            if ( (sTrimmed.length() == 0) || (commentChar.indexOf(s.charAt(0)) >= 0) ) {
                rowIsComment = true;
            }
            // Skip rows first, in particular user-specified skips before evaluating for the first non-comment row.
            if ( rowIsComment || needToSkipRow( row, firstNonHeaderRow, skipRows, skipRowsAfterComments ) ) {
                continue;
            }
            if ( !rowIsComment ) {
                if ( firstNonHeaderRow < 0 ) {
                    // This is the first non-comment data record.
                    firstNonHeaderRow = row;
                    if ( Message.isDebugOn ) {
                        Message.printDebug(dl, routine, "Found first non-comment (non-skipped) line at row " +
                            firstNonHeaderRow + ": " + s );
                    }
                }
            }
            // Check again in case the first non-header line is detected.
            if ( rowIsComment || needToSkipRow( row, firstNonHeaderRow, skipRows, skipRowsAfterComments ) ) {
                continue;
            }
            // If the ColumnNames contained _FC, then the first non-comment line is the file header.
            // This would have been read during command setup so just read and ignore here - this is not considered a data row.
            if ( doReadColumnNamesFromFile && (dataRowCount == 0) ) {
                Message.printDebug(dl, routine, "Skipping the row since it has file headers." );
                ++dataRowCount;
                continue;
            }
            // Else continue reading data records from the file.
            ++dataRowCount;
            // First break the row.
            tokens = StringUtil.breakStringList ( s, delim, breakFlag | StringUtil.DELIM_ALLOW_STRINGS);
            if ( tokens == null ) {
                ntokens = 0;
            }
            else {
                if ( s.endsWith(delim) ) {
                    // breakStringList() does not count a delimiter at the end as having data after but
                    // this command does some checks that make this required.
                    tokens.add("");
                }
                ntokens = tokens.size();
            }
            if ( tokens.size() < columnNames.size() ) {
                errorMessages.add ( "Read " + ntokens + " tokens for row " + row +
                    " but at least " + columnNames.size() + " are expected - not parsing line: \"" + s + "\"" );
                continue;
            }
            // Determine the date/time.
            if ( dateTimePos >= 0 ) {
                dateTimeString = tokens.get(dateTimePos);
            }
            else if ( datePos >= 0 ) {
                dateString = tokens.get(datePos);
                if ( timePos >= 0 ) {
                    timeString = tokens.get(timePos);
                    dateTimeString = dateString + ":" + timeString;
                }
                else {
                    dateTimeString = dateString;
                }
            }
            dateTime = null;
            try {
                if ( dateTimeParser != null ) {
                    // Reuse the date/time for performance.
                    // This is safe because for regular time series only the parts are used
                    // and for irregular a copy is made when setting the value.
                    dateTime = dateTimeParser.parse(dateTime,dateTimeString);
                }
                else {
                    // Let the parse() method figure out the format.
                    dateTime = DateTime.parse(dateTimeString);
                }
            }
            catch ( Exception dte ) {
                if ( errorMessages != null ) {
                    errorMessages.add ( "Error parsing date/time in row " + row + " column " +
                        dateTimeColumn + " \"" + dateTimeString + "\" (" + dte + ")" );
                }
                ++dtErrorCount;
                if ( dtErrorCount == 1 ) {
                    // Print one exception to help with troubleshooting.
                    Message.printWarning(3, routine, dte);
                }
                continue; // No reason to process the data in the row.
            }
            // Process the time series for the row.
            for ( ival = 0; ival < valuePos.length; ival++ ) {
                if ( valuePos[ival] < 0 ) {
                    // Error matching column in setup so continue to avoid major problem
                    // Will have null time series in result.
                    continue;
                }
                // Time series corresponding to value.
                ts = tslist.get(ival);
                // If the first row being processed, need to allocate the data space for all the time series.
                // This also requires reading from the end of the file to get the end date.
                // Because some files have data in reverse chronological order (newest at top), compare
                // the start and end date/times before setting.
                if ( (doReadColumnNamesFromFile && (dataRowCount == 2)) ||
                     (!doReadColumnNamesFromFile && dataRowCount == 1) ) {
                    // The first date will be set from the first row of data.
                    if ( latest == null ) {
                        // Determine the last date by reading the end of the file - only need to do for first time series.
                        DateTime lastFileDateTime = determineEndDateTimeFromFile (
                            inputFileFull, dateTimePos, datePos, timePos, delim, breakFlag, dateTimeParser );
                        if ( lastFileDateTime == null ) {
                            throw new IOException ( "Unable to determine date/time from last line in file." );
                        }
                        if ( Message.isDebugOn ) {
                            Message.printDebug(dl,routine,"Latest date/time in file is " + lastFileDateTime );
                        }
                        if ( dateTime.greaterThan(lastFileDateTime)) {
                            earliest = lastFileDateTime;
                            latest = dateTime;
                        }
                        else {
                            earliest = dateTime;
                            latest = lastFileDateTime;
                        }
                    }
                    // Always set the original to the file but only set period if not passed in.
                    if ( inputStartReq == null ) {
                        ts.setDate1(earliest);
                    }
                    ts.setDate1Original(earliest);
                    if ( inputEndReq == null ) {
                        ts.setDate2(latest);
                    }
                    ts.setDate2Original(latest);
                    ts.addToGenesis ( "Read time series from file \"" + inputFileFull + "\" for period " +
                        ts.getDate1() + " to " + ts.getDate2() );
                    if ( readData ) {
                        // Allocate the data space.
                        ts.allocateDataSpace();
                    }
                    if ( Message.isDebugOn ) {
                        Message.printDebug(dl, routine, "Set period of time series " + ts.getIdentifierString() +
                            ": " + ts.getDate1() + " to " + ts.getDate2() );
                    }
                }
                if ( !readData ) {
                    // No need to process data for this time series (and will break out of main loop below when
                    // done with all time series).
                    continue;
                }
                valueString = tokens.get(valuePos[ival]).trim();
                // Get the flag if specified (can have flag even if value is missing).
                flag = null;
                if ( (flagPos.length >= ival) && (flagPos[ival] >= 0) ) {
                    flag = tokens.get(flagPos[ival]).trim();
                    flag.replace("\"", "");
                }
                if ( valueString.equals("") || (StringUtil.indexOfIgnoreCase(missing, valueString) >= 0) ) {
                    // Missing so just let it remain missing in the time series, unless flag needs to be set.
                    if ( (flag != null) && !flag.equals("") ) {
                        value = ts.getMissing();
                        ts.setDataValue ( dateTime, value, flag, 0 );
                    }
                }
                else if ( !StringUtil.isDouble(valueString) ) {
                    // Data error.
                    errorMessages.add ( "Data value (" + valueString + ") in row " + row + " column " +
                        valueColumns.get(ival) + " is not a number and is not a recognized missing value." );
                }
                else {
                    // Valid number so set in the time series.
                    value = Double.parseDouble(valueString);
                    if ( Message.isDebugOn ) {
                        Message.printDebug(dl, routine, "Setting value of " + valueColumns.get(ival) + " at " +
                        dateTime + " -> " + value );
                    }
                    if ( periodRequested && dateTime.lessThan(earliest) || dateTime.greaterThan(latest) ) {
                        // Data record is not in period so don't set.
                        // Need to check each one because there is no requirement that records be in order.
                        continue;
                    }
                    if ( (flag != null) && !flag.equals("") ) {
                        ts.setDataValue ( dateTime, value, flag, 0 );
                    }
                    else {
                        ts.setDataValue ( dateTime, value );
                    }
                }
            }
            // If reading the data, can break without allocating data and reading.
            if ( !readData ) {
                // No need to continue.
                break;
            }
        }
        return tslist;
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, e );
        throw new IOException ( "Error reading from file near line " + row + " (" + e + ")." );
    }
    finally {
        if ( in != null ) {
            in.close();
        }
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
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_level = 2;
    int log_level = 3;
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

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile"); // Expanded below.
	String Delimiter = parameters.getValue("Delimiter");
    if ( (Delimiter != null) && !Delimiter.isEmpty() && (Delimiter.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	Delimiter = TSCommandProcessorUtil.expandParameterValue(processor, this, Delimiter);
	}
	String Comment = parameters.getValue("Comment");
	if ( (Comment == null) || Comment.equals("") ) {
	    Comment = "#"; // Default.
	}
    if ( (Comment != null) && !Comment.isEmpty() && (Comment.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	Comment = TSCommandProcessorUtil.expandParameterValue(processor, this, Comment);
	}
	String Alias = parameters.getValue("Alias");
	String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}";
	}
    String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}";
	}
	String ColumnNames = parameters.getValue("ColumnNames");
    if ( (ColumnNames != null) && !ColumnNames.isEmpty() && (ColumnNames.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	ColumnNames = TSCommandProcessorUtil.expandParameterValue(processor, this, ColumnNames);
	}
	String DateTimeColumn = parameters.getValue("DateTimeColumn");
    if ( (DateTimeColumn != null) && !DateTimeColumn.isEmpty() && (DateTimeColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	DateTimeColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, DateTimeColumn);
	}
	String DateTimeFormat = parameters.getValue("DateTimeFormat");
    if ( (DateTimeFormat != null) && !DateTimeFormat.isEmpty() && (DateTimeFormat.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	DateTimeFormat = TSCommandProcessorUtil.expandParameterValue(processor, this, DateTimeFormat);
	}
	String DateColumn = parameters.getValue("DateColumn");
    if ( (DateColumn != null) && !DateColumn.isEmpty() && (DateColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	DateColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, DateColumn);
	}
	String TimeColumn = parameters.getValue("TimeColumn");
    if ( (TimeColumn != null) && !TimeColumn.isEmpty() && (TimeColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	TimeColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TimeColumn);
	}
	String ValueColumn = parameters.getValue("ValueColumn");
    if ( (ValueColumn != null) && !ValueColumn.isEmpty() && (ValueColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	ValueColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, ValueColumn);
	}
	String FlagColumn = parameters.getValue("FlagColumn");
    if ( (FlagColumn != null) && !FlagColumn.isEmpty() && (FlagColumn.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	FlagColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, FlagColumn);
	}
	String LocationID = parameters.getValue("LocationID");
    if ( (LocationID != null) && !LocationID.isEmpty() && (LocationID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	LocationID = TSCommandProcessorUtil.expandParameterValue(processor, this, LocationID);
	}
	String Provider = parameters.getValue("Provider");
    if ( (Provider != null) && !Provider.isEmpty() && (Provider.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	Provider = TSCommandProcessorUtil.expandParameterValue(processor, this, Provider);
	}
	String DataType = parameters.getValue("DataType");
    if ( (DataType != null) && !DataType.isEmpty() && (DataType.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	DataType = TSCommandProcessorUtil.expandParameterValue(processor, this, DataType);
	}
	String Scenario = parameters.getValue("Scenario");
    if ( (Scenario != null) && !Scenario.isEmpty() && (Scenario.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	Scenario = TSCommandProcessorUtil.expandParameterValue(processor, this, Scenario);
	}
	String Units = parameters.getValue("Units");
    if ( (Units != null) && !Units.isEmpty() && (Units.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	Units = TSCommandProcessorUtil.expandParameterValue(processor, this, Units);
	}
    String MissingValue = parameters.getValue("MissingValue");
    if ( (MissingValue != null) && !MissingValue.isEmpty() && (MissingValue.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
    	MissingValue = TSCommandProcessorUtil.expandParameterValue(processor, this, MissingValue);
	}
    List<String> missingValues = new ArrayList<>();
    if ( (MissingValue != null) && !MissingValue.equals("") ) {
        // Can have one or more values that should be interpreted as missing.
    	missingValues = StringUtil.breakStringList(MissingValue, ",", 0);
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

	// Read the file.
    String InputFile_full = InputFile;
    List<TS> tslist = null; // List of time series that is read.
	try {
        boolean readData = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            readData = false;
        }
        InputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));

        if ( !IOUtil.fileReadable(InputFile_full) || !IOUtil.fileExists(InputFile_full)) {
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

        // Read everything in the file (one time series or traces).
        List<String> errorMessages = new ArrayList<>();
        // If any parameters refer to the column names, then the column names are expected to be in the file.
        // doReadColumnNamesFromFile is passed to the readTimeSeriesList() method to indicate that a header line is expected.
        boolean doReadColumnNamesFromFile = false;
        if ( (DateTimeColumn != null) && StringUtil.indexOfIgnoreCase(DateTimeColumn,_FC,0) >= 0 ) {
        	doReadColumnNamesFromFile = true;
        }
        if ( (DateColumn != null) && StringUtil.indexOfIgnoreCase(DateColumn,_FC,0) >= 0 ) {
        	doReadColumnNamesFromFile = true;
        }
        if ( (TimeColumn != null) && StringUtil.indexOfIgnoreCase(TimeColumn,_FC,0) >= 0 ) {
        	doReadColumnNamesFromFile = true;
        }
        if ( (ValueColumn != null) && StringUtil.indexOfIgnoreCase(ValueColumn,_FC,0) >= 0 ) {
        	doReadColumnNamesFromFile = true;
        }
        if ( (FlagColumn != null) && StringUtil.indexOfIgnoreCase(FlagColumn,_FC,0) >= 0 ) {
        	doReadColumnNamesFromFile = true;
        }
        if ( (LocationID != null) && StringUtil.indexOfIgnoreCase(LocationID,_FC,0) >= 0 ) {
        	doReadColumnNamesFromFile = true;
        }
        if ( (ColumnNames != null) && StringUtil.indexOfIgnoreCase(ColumnNames,_FC,0) >= 0 ) {
        	doReadColumnNamesFromFile = true;
        	if ( Message.isDebugOn ) {
        		Message.printDebug(2,routine,"First non-comment line in file will be read as comments because " + _FC + " is used.");
        	}
        }
        else if ( doReadColumnNamesFromFile ) {
        	// ColumnNames does not contain FC therefore do not allow other columns to use because code cannot handle.
            message = "ColumnNames has no " + _FC + " syntax - cannot mix with other columns that use " + _FC;
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Correct the parameter values that specify column names." ) );
        }
        // The column names for the file are either read from a heading line in the file (ColumnNames="[1:]" or are assigned using ColumnNames="name1,name2,...").
        List<String> problemList = new ArrayList<>(); // Cumulative list of warning messages (already added in code below)
        // Translate file column names and other information to runtime values.
        // Print information for troubleshooting.
       	List<String> columnNamesRuntime = getColumnNamesRuntime(ColumnNames, InputFile_full,
      		Delimiter, Comment, getSkipRows(), getSkipRowsAfterComments(), routine, status, problemList );
        if ( Message.isDebugOn ) {
        	for ( String s: columnNamesRuntime ) {
        		Message.printDebug(1, routine, "columnNamesRuntime=\"" + s + "\"");
        	}
        }
        String dateTimeColumnRuntime = getDateTimeColumnRuntime ( DateTimeColumn, InputFile_full, columnNamesRuntime,
        	Delimiter, Comment, getSkipRows(), getSkipRowsAfterComments(), routine, status, problemList );
        if ( Message.isDebugOn ) {
        	Message.printDebug(1, routine, "dateTimeColumnRuntime=\"" + dateTimeColumnRuntime + "\"");
        }
        String dateColumnRuntime = getDateColumnRuntime ( DateColumn, InputFile_full, columnNamesRuntime,
        	Delimiter, Comment, getSkipRows(), getSkipRowsAfterComments(), routine, status, problemList );
        if ( Message.isDebugOn ) {
        	Message.printDebug(1, routine, "dateColumnRuntime=\"" + dateColumnRuntime + "\"");
        }
        String timeColumnRuntime = getTimeColumnRuntime ( TimeColumn, InputFile_full, columnNamesRuntime,
            Delimiter, Comment, getSkipRows(), getSkipRowsAfterComments(), routine, status, problemList );
        if ( Message.isDebugOn ) {
        	Message.printDebug(1, routine, "timeColumnRuntime=\"" + timeColumnRuntime + "\"");
        }
        List<String> valueColumnsRuntime = getValueColumnsRuntime ( ValueColumn, InputFile_full, columnNamesRuntime,
            Delimiter, Comment, getSkipRows(), getSkipRowsAfterComments(), routine, status, problemList );
        if ( Message.isDebugOn ) {
        	for ( String s: valueColumnsRuntime ) {
        		Message.printDebug(1, routine, "valueColumnsRuntime=\"" + s + "\"");
        	}
        }
        List<String> flagColumnsRuntime = getFlagColumnsRuntime ( FlagColumn, InputFile_full, columnNamesRuntime,
            valueColumnsRuntime, Delimiter, Comment, getSkipRows(), getSkipRowsAfterComments(), routine, status, problemList );
        if ( Message.isDebugOn ) {
        	for ( String s: flagColumnsRuntime ) {
        		Message.printDebug(1, routine, "flagColumnsRuntime=\"" + s + "\"");
        	}
        }
        List<String> locationIDRuntime = getLocationIDRuntime ( LocationID, InputFile_full, columnNamesRuntime,
            valueColumnsRuntime, Delimiter, Comment, getSkipRows(), getSkipRowsAfterComments(), routine, status, problemList );
        if ( Message.isDebugOn ) {
        	for ( String s: locationIDRuntime ) {
        		Message.printDebug(1, routine, "locationIDRuntime=\"" + s + "\"");
        	}
        }
        List<String> providerRuntime = getProviderRuntime ( Provider, valueColumnsRuntime, routine, status, problemList );
        if ( Message.isDebugOn ) {
        	for ( String s: providerRuntime ) {
        		Message.printDebug(1, routine, "providerRuntime=\"" + s + "\"");
        	}
        }
        List<String> dataTypeRuntime = getDataTypeRuntime ( DataType, valueColumnsRuntime, routine, status, problemList );
        if ( Message.isDebugOn ) {
        	for ( String s: dataTypeRuntime ) {
        		Message.printDebug(1, routine, "dataTypeRuntime=\"" + s + "\"");
        	}
        }
        List<String> scenarioRuntime = getScenarioRuntime ( Scenario, valueColumnsRuntime, routine, status, problemList );
        if ( Message.isDebugOn ) {
        	for ( String s: scenarioRuntime ) {
        		Message.printDebug(1, routine, "scenarioRuntime=\"" + s + "\"");
        	}
        }
        List<String> unitsRuntime = getUnitsRuntime ( Units, valueColumnsRuntime, routine, status, problemList );
        if ( Message.isDebugOn ) {
        	for ( String s: unitsRuntime ) {
        		Message.printDebug(1, routine, "unitsRuntime=\"" + s + "\"");
        	}
        }
        // Read the time series.
        warning_count += problemList.size();
        tslist = readTimeSeriesList ( InputFile_full, StringUtil.literalToInternal(Delimiter),
            getTreatConsecutiveDelimitersAsOne(),
            columnNamesRuntime, doReadColumnNamesFromFile, dateTimeColumnRuntime,
            DateTimeFormat, dateColumnRuntime,
            timeColumnRuntime, valueColumnsRuntime, flagColumnsRuntime,
            Comment, getSkipRows(), getSkipRowsAfterComments(),
            locationIDRuntime, providerRuntime, dataTypeRuntime, getInterval(), scenarioRuntime,
            unitsRuntime, missingValues,
            InputStart_DateTime, InputEnd_DateTime, readData, errorMessages );

		if ( tslist != null ) {
			int tscount = tslist.size();
			Message.printStatus ( 2, routine, "Read " + tscount + " time series from \"" + InputFile_full + "\"" );
	        if ( (Alias != null) && (Alias.length() > 0) ) {
	            for ( int i = 0; i < tscount; i++ ) {
	                TS ts = (TS)tslist.get(i);
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
		message = "Unexpected error reading delimited file. \"" + InputFile_full + "\" (" + e + ")";
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
            // Further process the time series.
            // This makes sure the period is at least as long as the output period.
            int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
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
private void setDiscoveryTSList ( List<TS> discovery_TS_List ) {
    __discovery_TS_List = discovery_TS_List;
}

/**
Set the data interval for the time series.
*/
private void setInterval ( TimeInterval interval ) {
    __interval = interval;
}

/**
Set the rows to skip (integer ranges).
*/
private void setSkipRows ( int[][] skipRows ) {
    __skipRows = skipRows;
}

/**
Set the number of rows to skip after the header comments.
*/
private void setSkipRowsAfterComments ( int skipRowsAfterComments ) {
    __skipRowsAfterComments = skipRowsAfterComments;
}

/**
Set whether consecutive delimiters should be treated as one.
*/
private void setTreatConsecutiveDelimitersAsOne ( boolean treatConsecutiveDelimitersAsOne ) {
    __treatConsecutiveDelimitersAsOne = treatConsecutiveDelimitersAsOne;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"InputFile",
    	"Delimiter",
    	"TreatConsecutiveDelimitersAsOne",
    	"ColumnNames",
    	"DateTimeColumn",
    	"DateTimeFormat",
    	"DateColumn",
    	"TimeColumn",
    	"ValueColumn",
    	"FlagColumn",
    	"Comment",
    	"SkipRows",
    	"SkipRowsAfterComments",
    	"LocationID",
    	"Provider",
    	"DataType",
    	"Interval",
    	"Scenario",
    	"Units",
    	"MissingValue",
    	"Alias",
		"InputStart",
		"InputEnd"
	};
	return this.toString(parameters, parameterOrder);
}

}