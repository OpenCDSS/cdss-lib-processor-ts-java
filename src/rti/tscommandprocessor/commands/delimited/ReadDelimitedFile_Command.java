package rti.tscommandprocessor.commands.delimited;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Vector;

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
import RTi.Util.Time.TimeInterval;

/**
<p>
This class initializes, checks, and runs the ReadDelimitedFile() command.
</p>
*/
public class ReadDelimitedFile_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

protected final String _False = "False";
protected final String _True = "True";

// FIXME SAM 2007-12-19 Need to evaluate this - runtime versions may be different.
/**
Private data members shared between the checkCommandParameter() and the 
runCommand() methods (prevent code duplication parsing input).  
*/
//private DateTime __InputStart = null;
//private DateTime __InputEnd   = null;

/**
Column names for each time series being processed.
*/
private List<String> __columnNames = new Vector();

/**
Data types for each time series being processed.
*/
private List<String> __dataType = new Vector();

/**
Date/time column name.
*/
private String __dateTimeColumn = null;

/**
Delimiter for parsing file data.
*/
private String __delimiter = null;

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List __discovery_TS_List = null;

/**
Interval for the time series.
*/
private TimeInterval __interval = null;

/**
Location ID for each time series being processed.
*/
private List<String> __locationID = new Vector();

/**
Missing value strings that may be present in the file.
*/
private List<String> __missingValue = new Vector();

/**
Provider for each time series being processed.
*/
private List<String> __provider = new Vector();

/**
Scenario for each time series being processed.
*/
private List<String> __scenario = new Vector();

/**
Row ranges to skip (single rows will have same and start value.  Rows are 1+.
*/
private int[][] __skipRows = null; // Allocated when checking parameters

/**
Indicate whether to treat consecutive delimiters as one.
*/
private int __skipRowsAfterComments = -1;

/**
Indicate whether to treat consecutive delimiters as one.
*/
private boolean __treatConsecutiveDelimitersAsOne = false;

/**
Data units for each time series being processed.
*/
private List<String> __units = new Vector();

/**
Column names for data values, for each time series being processed.
*/
private List<String> __valueColumns = new Vector();

/**
Constructor.
*/
public ReadDelimitedFile_Command ()
{
	super();
	setCommandName ( "ReadDelimitedFile" );
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
{
    String routine = getClass().getName() + ".checkCommandParameters";
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
    // String DateColumn = parameters.getValue("DateColumn" ); // Not implemented
    // String TimeColumn = parameters.getValue("TimeColumn" ); // Not implemented
    String ValueColumn = parameters.getValue("ValueColumn" );
    //String Comment = parameters.getValue("Comment" );  // No checks
    String SkipRows = parameters.getValue("SkipRows" );
    String SkipRowsAfterComments = parameters.getValue("SkipRowsAfterComments" );
    String LocationID = parameters.getValue("LocationID" );
    String Provider = parameters.getValue("Provider" );
    String DataType = parameters.getValue("DataType" );
    String Interval = parameters.getValue("Interval" );
    String Scenario = parameters.getValue("Scenario" );
    String Units = parameters.getValue("Units" );
    String MissingValue = parameters.getValue("MissingValue" );
    String Alias = parameters.getValue("Alias" );
	//String InputStart = parameters.getValue("InputStart");
	//String InputEnd   = parameters.getValue("InputEnd");
	
    if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify an existing input file." ) );
    }
    else {
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
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
            }
    
        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
            File f = new File ( adjusted_path );
            if ( !f.exists() ) {
                message = "The input file does not exist:  \"" + adjusted_path + "\".";
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
    
    setDelimiter ( null );
    if ( (Delimiter == null) || (Delimiter.length() == 0) ) {
        message = "The delimiter must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the delimiter indicating breaks between columns." ) );
    }
    else {
        // Convert \t into internal characters
        String delimiter = Delimiter.replace("\\t", "\t");
        setDelimiter ( delimiter );
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
    
    List<String> columnNames = new Vector();
    setColumnNames ( columnNames );
    if ( (ColumnNames == null) || (ColumnNames.length() == 0) ) {
        message = "The column names must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify column names for all columns in the file." ) );
    }
    else {
        // Parse out the column names so that they can be used in checks below.
        columnNames = StringUtil.breakStringList ( ColumnNames, ",", StringUtil.DELIM_ALLOW_STRINGS );
        setColumnNames ( columnNames );
    }
    
    setDateTimeColumn (null);
    if ( (DateTimeColumn == null) || (DateTimeColumn.length() == 0) ) {
        message = "The date/time column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a date/time column as one of the values from ColumnNames." ) );
    }
    else {
        if ( getColumnNumberFromName(DateTimeColumn, columnNames) < 0 ) {
            message = "The DateTimeColumn (" + DateTimeColumn + ") is not a recognized column name.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a date/time column as one of the values from ColumnNames." ) );
        }
        else {
            setDateTimeColumn ( DateTimeColumn );
        }
    }
    
    List<String> valueColumns = new Vector();
    setValueColumns ( valueColumns );
    if ( (ValueColumn == null) || (ValueColumn.length() == 0) ) {
        message = "The value column(s) must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify data column(s) as values from ColumnNames, separated by commas." ) );
    }
    else {
        valueColumns = StringUtil.breakStringList ( ValueColumn, ",", StringUtil.DELIM_ALLOW_STRINGS );
        for ( String valueColumn : valueColumns ) {
            if ( getColumnNumberFromName(valueColumn, columnNames) < 0 ) {
                message = "The ValueColumn (" + valueColumn + ") is not a recognized column name.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify value column(s) matching ColumnNames, separated by commas." ) );
            }
        }
    }
    setValueColumns(valueColumns);

    setSkipRows ( null );
    if ( (SkipRows != null) && !SkipRows.equals("") ) {
        try {
            setSkipRows ( StringUtil.parseIntegerRangeSequence(SkipRows, ",", 0) );
        }
        catch ( Exception e ) {
            message = "The rows to skip (" + SkipRows + ") is invalid (" + e + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify as comma-separated rows (1+) or ranges a-b." ) );
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

    List<String> locationID = new Vector();
    setLocationID( locationID );
    if ( (LocationID != null) && !LocationID.equals("") ) {
        // Can have one value that is re-used, or LocationID for each time series
        List<String>tokens = StringUtil.breakStringList(LocationID, ",", 0);
        if ( (tokens.size() != 1) && (tokens.size() != valueColumns.size()) ) {
            message = "The number of LocationID strings (" + tokens.size() + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + valueColumns.size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < valueColumns.size(); i++ ) {
                locationID.add ( "" );
            }
        }
        else {
            // Process the LocationID for each time series
            for ( int i = 0; i < valueColumns.size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    locationID.add(tokens.get(0));
                }
                else {
                    locationID.add(tokens.get(i));
                }
            }
        }
    }
    setLocationID ( locationID );

    List<String> provider = new Vector();
    setProvider ( provider );
    if ( (Provider != null) && !Provider.equals("") ) {
        // Can have one value that is re-used, or Provider for each time series
        List<String>tokens = StringUtil.breakStringList(Provider, ",", 0);
        if ( (tokens.size() != 1) && (tokens.size() != valueColumns.size()) ) {
            message = "The number of Provider strings (" + tokens.size() + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + valueColumns.size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < valueColumns.size(); i++ ) {
                provider.add ( "" );
            }
        }
        else {
            // Process the Provider for each time series
            for ( int i = 0; i < valueColumns.size(); i++ ) {
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
        for ( int i = 0; i < valueColumns.size(); i++ ) {
            provider.add ( "" );
        }
    }
    setProvider ( provider );
    
    List<String> dataType = new Vector();
    setDataType ( dataType );
    if ( (DataType == null) || DataType.equals("") ) {
        // Set to the same as the value columns
        for ( int i = 0; i < valueColumns.size(); i++ ) {
            dataType.add ( valueColumns.get(i) );
        }
    }
    else {
        // Can have one value that is re-used, or DataType for each time series
        List<String>tokens = StringUtil.breakStringList(DataType, ",", 0);
        if ( (tokens.size() != 1) && (tokens.size() != valueColumns.size()) ) {
            message = "The number of DataType strings (" + tokens.size() + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + valueColumns.size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < valueColumns.size(); i++ ) {
                dataType.add ( "" );
            }
        }
        else {
            // Process the DataType for each time series
            for ( int i = 0; i < valueColumns.size(); i++ ) {
                if ( tokens.size() == 1 ) {
                    dataType.add(tokens.get(0));
                }
                else {
                    dataType.add(tokens.get(i));
                }
            }
        }
    }
    setDataType ( dataType );

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
        if ( (tokens.size() != 1) && (tokens.size() != valueColumns.size()) ) {
            message = "The number of Scenario strings (" + tokens.size() + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + valueColumns.size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < valueColumns.size(); i++ ) {
                scenario.add ( "" );
            }
        }
        else {
            // Process the Scenario for each time series
            for ( int i = 0; i < valueColumns.size(); i++ ) {
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
        for ( int i = 0; i < valueColumns.size(); i++ ) {
            scenario.add ( "" );
        }
    }
    setScenario ( scenario );
    
    List<String> units = new Vector();
    setUnits ( units );
    if ( (Units != null) && !Units.equals("") ) {
        // Can have one value that is re-used, or units for each time series
        List<String>tokens = StringUtil.breakStringList(Units, ",", 0);
        if ( (tokens.size() != 1) && (tokens.size() != valueColumns.size()) ) {
            message = "The number of units strings (" + tokens.size() + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify 1 value or the same number as the value columns (" + valueColumns.size() +
                    "), separated by commas." ) );
            for ( int i = 0; i < valueColumns.size(); i++ ) {
                units.add ( "" );
            }
        }
        else {
            // Process the units for each time series
            for ( int i = 0; i < valueColumns.size(); i++ ) {
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
        for ( int i = 0; i < valueColumns.size(); i++ ) {
            units.add ( "" );
        }
    }
    setUnits ( units );
    
    setMissingValue ( new Vector() );
    if ( (MissingValue != null) && !MissingValue.equals("") ) {
        // Can have one or more values that should be interpreted as missing
        List<String>tokens = StringUtil.breakStringList(MissingValue, ",", 0);
        setMissingValue ( tokens );
    }

    /*
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
	*/
    
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
    List valid_Vector = new Vector();
    valid_Vector.add ( "InputFile" );
    valid_Vector.add ( "Delimiter" );
    valid_Vector.add ( "TreatConsecutiveDelimitersAsOne" );
    valid_Vector.add ( "ColumnNames" );
    valid_Vector.add ( "DateTimeColumn" );
    valid_Vector.add ( "DateTimeFormat" );
    valid_Vector.add ( "DateColumn" );
    valid_Vector.add ( "TimeColumn" );
    valid_Vector.add ( "ValueColumn" );
    valid_Vector.add ( "Comment" );
    valid_Vector.add ( "SkipRows" );
    valid_Vector.add ( "SkipRowsAfterComments" );
    valid_Vector.add ( "LocationID" );
    valid_Vector.add ( "Provider" );
    valid_Vector.add ( "DataType" );
    valid_Vector.add ( "Interval" );
    valid_Vector.add ( "Scenario" );
    valid_Vector.add ( "Units" );
    valid_Vector.add ( "MissingValue" );
    valid_Vector.add ( "Alias" );
    //valid_Vector.add ( "InputStart" );
    //valid_Vector.add ( "InputEnd" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, warning_level ),
			warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Determine the time series end date/time by reading the end of the file to determine the last date.
This opens a temporary connection, reads the end of the file, and parses out data to get the date.
*/
private DateTime determineEndDateTimeFromFile ( String inputFileFull, int dateTimePos, String delim, int parseFlag )
throws FileNotFoundException, IOException
{   String routine = getClass().getName() + ".determineEndDateTimeFromFile";
    int dl = 30;
    if ( Message.isDebugOn ) {
        Message.printDebug( dl, routine, "Getting end date from end of file..." );
    }
    RandomAccessFile ra = new RandomAccessFile ( inputFileFull, "r" );
    long length = ra.length();
    // Skip to 5000 bytes from the end.  This should get some actual data
    // lines.  Save in a temporary array in memory.
    if ( length >= 5000 ) {
        ra.seek ( length - 5000 );
    } // Otherwise just read from the top of the file to get all content
    byte[] b = new byte[5000];
    ra.read ( b );
    ra.close();
    ra = null;
    // Now break the bytes into records...
    String bs = new String ( b );
    List<String> v = StringUtil.breakStringList ( bs, "\n\r", StringUtil.DELIM_SKIP_BLANKS );
    // Loop through and figure out the last date.  Start at the second
    // record because it is likely that a complete record was not found in the first record.
    int size = v.size();
    String string = null;
    String date2_string = null;
    List<String> tokens = null;
    for ( int i = 1; i < size; i++ ) {
        string = (v.get(i)).trim();
        if ( (string.length() == 0) || (string.charAt(0) == '#') || (string.charAt(0) == '<') ) {
            // Ignore blank lines, comments, and HTML-enclosing tags.
            continue;
        }
        tokens = StringUtil.breakStringList( string, delim, parseFlag );
        // Set the date string - overwrite for each line until all the end lines are processed
        date2_string = tokens.get(dateTimePos);
    }
    // Whatever came out of the last record will remain.
    DateTime datetime = null;
    try {
        if ( Message.isDebugOn ) {
            Message.printDebug( 2, routine, "Got end date/time string \"" + date2_string + "\" from line \"" + string + "\"" );
        }
        datetime = DateTime.parse(date2_string);
    }
    catch ( Exception e ) {
        Message.printWarning ( 3, routine, "Error parsing end date/time \"" + date2_string + "\" from file \"" +
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
public boolean editCommand ( JFrame parent )
{	
	// The command will be modified if changed...
	return ( new ReadDelimitedFile_JDialog ( parent, this ) ).ok();
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
Return the column names list.
*/
private List<String> getColumnNames()
{
    return __columnNames;
}

/**
Get the column number (0+) from the column names.
@param columnName column name of interest
@param columnName names of all the columns
@return the column number (0+) corresponding to the column name, or -1 if not found.
*/
private int getColumnNumberFromName ( String columnName, List<String> columnNames )
{
    for ( int i = 0; i < columnNames.size(); i++ ) {
        if ( columnName.equalsIgnoreCase(columnNames.get(i)) ) {
            return i;
        }
    }
    return -1;
}

/**
Get the column numbers (0+) for a list of names.
@param someNames column names of interest.
@param columnNames names of all the columns.
@return the column number (0+) corresponding to the each column name, or -1 if not found, as an array
for each column.
*/
private int[] getColumnNumbersFromNames ( List<String> someNames, List<String> columnNames )
{
    int [] columns = new int[someNames.size()];
    for ( int i = 0; i < columns.length; i++ ) {
        columns[i] = getColumnNumberFromName ( someNames.get(i), columnNames );
    }
    return columns;
}

/**
Return the data type list.
*/
private List<String> getDataType()
{
    return __dataType;
}

/**
Return the date/time column name.
*/
private String getDateTimeColumn ()
{
    return __dateTimeColumn;
}

/**
Return the delimiter that separates columns.
*/
private String getDelimiter ()
{
    return __delimiter;
}

/**
Return the list of time series read in discovery phase.
*/
private List getDiscoveryTSList ()
{
    return __discovery_TS_List;
}

/**
Return the data interval.
*/
private TimeInterval getInterval()
{
    return __interval;
}

/**
Return the location ID list.
*/
private List<String> getLocationID()
{
    return __locationID;
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
    List discovery_TS_List = getDiscoveryTSList ();
    if ( (discovery_TS_List == null) || (discovery_TS_List.size() == 0) ) {
        return null;
    }
    TS datats = (TS)discovery_TS_List.get(0);
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
Return the number of rows to skip after comments.
*/
private int getSkipRowsAfterComments()
{
    return __skipRowsAfterComments;
}

/**
Return whether consecutive delimiters should be treated as one.
*/
private boolean getTreatConsecutiveDelimitersAsOne()
{
    return __treatConsecutiveDelimitersAsOne;
}

/**
Return the data units list.
*/
private List<String> getUnits()
{
    return __units;
}

/**
Return the value columns list.
*/
private List<String> getValueColumns()
{
    return __valueColumns;
}

/**
Determine if a row needs to be skipped, based on command parameters.
@param row line from file being processed(1+)
@param firstNonHeaderRow row in the file that is the first after the header comments
@param skipRows ranges of rows (1+ each) that are to be skipped
@param skipRowsAfterComments the number of rows after the header comments to be skipped
*/
private boolean needToSkipRow( int row, int firstNonHeaderRow, int[][] skipRows, int skipRowsAfterComments )
{
    if ( skipRows != null ) {
        // First check the absolute skips
        for ( int ipair = 0; ipair < skipRows.length; ipair++ ) {
            if ( (row >= skipRows[ipair][0]) && (row <= skipRows[ipair][1])) {
                // Skipping the absolute rows
                return true;
            }
        }
    }
    // Check to see if in the rows after the header comments
    if ( (firstNonHeaderRow > 0) && (skipRowsAfterComments > 0) ) {
        int startSkip = firstNonHeaderRow;
        int endSkip = firstNonHeaderRow + skipRowsAfterComments - 1;
        //Message.printStatus ( 2, "", "startSkip="+startSkip+" endSkip="+endSkip+" row="+row);
        if ( (row >= startSkip) && (row <= endSkip) ) {
            return true;
        }
    }
    return false;
}

// Use the base class parseCommand() method

/**
Read a list of time series from a delimited file.
@param inputFileFull the full path to the input file.
@param delim delimiter character(s).
@param treatConsecutiveDelimitersAsOne indicate whether consecutive delimiter characters should be treated as one.
@param inputStart requested start of data (null to return all).
@param inputEnd requested end of data (null to return all).
@param commentChar character(s) that indicates comments lines, if the first character of a line
(or null if not specified).  Only 1-character is checked but more than one special character can be indicated.
@param skipRows ranges of rows (1+ each) that are to be skipped
@param skipRowsAfterComments the number of rows after the header comments to be skipped
@param units data units for each time series.
@param readData True to read data, false to only read the header information.
@param props Properties to control the read, from command parameters.
*/
private List<TS> readTimeSeriesList ( String inputFileFull,
    String delim,  boolean treatConsecutiveDelimitersAsOne,
    List<String> columnNames, String dateTimeColumn, List<String> valueColumns,
    String commentChar, int[][] skipRows, int skipRowsAfterComments,
    List<String> ids, List<String> providers, List<String> datatypes, TimeInterval interval,
    List<String> scenarios, List<String> units, List<String> missing,
    DateTime inputStart, DateTime inputEnd,
    boolean readData, List<String> errorMessages )
throws IOException
{   String routine = getClass().getName() + ".readTimeSeriesList", message;
    // Allocate the list
    List<TS> tslist = new Vector();
    // Open the file
    BufferedReader in = null;
    in = new BufferedReader ( new InputStreamReader(IOUtil.getInputStream ( inputFileFull )) );
    // Translate column names to integer values to speed up processing below
    int dateTimePos = getColumnNumberFromName(dateTimeColumn,columnNames);
    int [] valuePos = getColumnNumbersFromNames(valueColumns,columnNames);
    // Create the time series - at this time meta-data are not read from the file
    // If this changes, then some values may need to be (re)set when the file is read
    for ( int its = 0; its < valuePos.length; its++ ) {
        TSIdent tsident = null;
        TS ts = null;
        String tsidentstr = ids.get(its) + "." + providers.get(its) + "." + datatypes.get(its) + "." +
            interval + "." + scenarios.get(its);
        try {
            tsident = new TSIdent( tsidentstr );
        }
        catch ( Exception e ) {
            tsident = null;
            errorMessages.add ( "Error initializing time series \"" + tsidentstr + "\" (" + e +
                ") - will not read.");
        }
        if ( tsident != null ) {
            try {
                ts = TSUtil.newTimeSeries( tsident.toString(), true );
                // Set all the information
                ts.setIdentifier ( tsident );
                ts.setDescription ( ids.get(its) + " " + datatypes.get(its) );
                ts.setDataUnits ( units.get(its) );
                ts.setDataUnitsOriginal ( units.get(its) );
                ts.setMissing ( Double.NaN );
            }
            catch ( Exception e ) {
                // Set the TS to null to match the column positions but won't be able to set data below
                ts = null;
                errorMessages.add ( "Error initializing time series \"" + tsidentstr + "\" (" + e +
                ") - will not read.");
            }
        }
        // Always add, even if null
        tslist.add ( ts );
    }
    // Loop through reading the rows from the file, perform actions as needed
    int row = 0;
    try {
        String s, sTrimmed;
        TS ts = null; // Time series to read
        boolean rowIsComment;
        List<String> tokens;
        String dateTimeString, valueString;
        DateTime dateTime;
        int breakFlag = 0;
        if ( treatConsecutiveDelimitersAsOne ) {
            breakFlag = StringUtil.DELIM_SKIP_BLANKS;
        }
        int firstNonHeaderRow = -1; // first line that is not a header comment (1+)
        int ival; // index for values on line - reused
        int dataRowCount = 0; // Count of data rows that are processed
        int dl = 10;
        int ntokens = 0; // Number of tokens parsed from a line
        double value; // Data value
        while ( true ) {
            // Read a line and deal with skipping
            s = in.readLine();
            if ( s == null ) {
                // No more data
                break;
            }
            // Else handle the line
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
            // Skip rows first, in particular user-specified skips before evaluating for the first non-comment row
            if ( rowIsComment || needToSkipRow( row, firstNonHeaderRow, skipRows, skipRowsAfterComments ) ) {
                continue;
            }
            if ( !rowIsComment ) {
                if ( firstNonHeaderRow < 0 ) {
                    // This is the first non-comment data record
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
            // Else continue reading data records from the file...
            // First break the row...
            tokens = StringUtil.breakStringList ( s, delim, breakFlag );
            if ( tokens == null ) {
                ntokens = 0;
            }
            else {
                ntokens = tokens.size();
            }
            if ( tokens.size() < columnNames.size() ) {
                errorMessages.add ( "Read " + ntokens + " tokens for row " + row +
                    " but at least " + columnNames.size() + " are expected - not parsing line: \"" + s + "\"" );
                continue;
            }
            // Determine the date/time...
            dateTimeString = tokens.get(dateTimePos);
            dateTime = null;
            try {
                dateTime = DateTime.parse(dateTimeString);
            }
            catch ( Exception dte ) {
                if ( errorMessages != null ) {
                    errorMessages.add ( "Error parsing date/time in row " + row + " column " +
                        dateTimeColumn + " \"" + dateTimeString + "\" (" + dte + ")" );
                    continue; // No reason to process the data in the row
                }
            }
            // Process the time series for the row
            for ( ival = 0; ival < valuePos.length; ival++ ) {
                valueString = tokens.get(valuePos[ival] );
                if ( valueString.equals("") || (StringUtil.indexOfIgnoreCase(missing, valueString) >= 0) ) {
                    // Missing so just let it remain missing in the time series.
                }
                else if ( !StringUtil.isDouble(valueString) ) {
                    // Data error
                    errorMessages.add ( "Data value (" + valueString + ") in row " + row + " column " +
                        valueColumns.get(ival) + " is not a number and is not a recognized missing value." );
                }
                else {
                    // Valid number so set in the time series
                    ts = tslist.get(ival);
                    // If the first value, need to allocate the data space for all the time series
                    // This also requires reading from the end of the file to get the end date
                    if ( dataRowCount == 0 ) {
                        // The first date will be set from the first row of data
                        for ( TS its : tslist ) {
                            its.setDate1(dateTime);
                            its.setDate1Original(dateTime);
                            its.setDate2 ( determineEndDateTimeFromFile ( inputFileFull, dateTimePos, delim, breakFlag ) );
                            its.setDate2Original ( its.getDate2() );
                            its.addToGenesis ( "Read time series from file \"" + inputFileFull + "\" for period " +
                                its.getDate1() + " to " + its.getDate2() );
                            if ( readData ) {
                                // Allocate the data space
                                its.allocateDataSpace();
                            }
                        }
                        if ( Message.isDebugOn ) {
                            Message.printDebug(dl, routine, "Set period of time series: " + ts.getDate1() +
                                " to " + ts.getDate2() );
                        }
                        // If reading the data, can break without allocating data and reading
                        if ( !readData ) {
                            // No need to continue...
                            break;
                        }
                    }
                    ++dataRowCount;
                    value = Double.parseDouble(valueString);
                    if ( Message.isDebugOn ) {
                        Message.printDebug(dl, routine, "Setting value of " + valueColumns.get(ival) + " at " +
                        dateTime + " -> " + value );
                    }
                    ts.setDataValue ( dateTime, value );
                }
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

// TODO SAM 2010-04-14 Need to get something working - don't have time to figure out
// and enhance Ian's code so do something else for now, consistent with other TSTool commands.
/**
Read a list of time series from a delimited file.
@param InputFile_full the full path to the input file.
@param InputStart_DateTime requested start of data (null to return all).
@param InputEnd_DateTime requested end of data (null to return all).
@param NewUnits New data units (null to use units in file).
@param read_data True to read data, false to only read the header information.
@param props Properties to control the read, from command parameters.
*/
private List readTimeSeriesListOld ( String InputFile_full,
        DateTime InputStart_DateTime, DateTime InputEnd_DateTime, String NewUnits,
        boolean read_data, PropList props )
throws IOException, FileNotFoundException
{   String routine = "ReadDelimitedFile.readTimeSeriesList";
    String message; // Used for errors.
    
    // Process parameters into form usable by this code
    
    String SkipRows = props.getValue ( "SkipRows" );
    int SkipRows_int = -1;
    if ( SkipRows != null ) {
        SkipRows_int = StringUtil.atoi (SkipRows);
    }
    
    String Delimiter = props.getValue ( "Delimiter" );
    if ( Delimiter == null ) {
        Delimiter = ",";    // Default
    }
    else {
        // Replace \t literal with tab character...
        Delimiter = Delimiter.replaceAll("\\\\t", "\t" );
    }
    
    List ColumnNames_List = null;
    String ColumnNames = props.getValue ( "ColumnNames" );
    if ( ColumnNames == null ) {
        message = "No ColumnNames parameter has been specified.";
        Message.printWarning( 3, routine, message);
        throw new RuntimeException ( message );
    }
    else {
        // Parse for other code to use...
        ColumnNames_List = StringUtil.breakStringList ( ColumnNames, ",", StringUtil.DELIM_ALLOW_STRINGS );
        if ( (ColumnNames_List == null) || (ColumnNames_List.size() == 0) ) {
            message = "No ColumnNames parameter has been specified.";
            Message.printWarning( 3, routine, message);
            throw new RuntimeException ( message );
        }
    }
    
    String DateTimeColumn = props.getValue ( "DateTimeColumn" );
    int DateTimeColumn_int = -1;
    if ( DateTimeColumn == null ) {
        message = "No DateTimeColumn has been specified.";
        Message.printWarning( 3, routine, message);
        throw new RuntimeException ( message );
    }
    else {
        // Get the column out of the list...
        for ( int i = 0; i < ColumnNames_List.size(); i++ ) {
            if ( DateTimeColumn.equalsIgnoreCase((String)ColumnNames_List.get(i))) {
                DateTimeColumn_int = i;
                break;
            }
        }
    }
    if ( DateTimeColumn_int < 0 ) {
        message = "DateTimeColumn \"" + DateTimeColumn +
        "\" does not match columns in ColumnNames \"" + ColumnNames + "\".";
        Message.printWarning( 3, routine, message);
        throw new RuntimeException ( message );
    }
    
    int [] ValueColumn_int = null;
    String ValueColumn = props.getValue ( "ValueColumn" );
    if ( ValueColumn == null ) {
        message = "No ValueColumn parameter has been specified.";
        Message.printWarning( 3, routine, message);
        throw new RuntimeException ( message );
    }
    else {
        // Parse for other code to use...
        List ValueColumn_List = StringUtil.breakStringList ( ValueColumn, ",", StringUtil.DELIM_ALLOW_STRINGS );
        if ( (ValueColumn_List == null) || (ValueColumn_List.size() == 0) ) {
            message = "No ValueColumn parameter has been specified.";
            Message.printWarning( 3, routine, message);
            throw new RuntimeException ( message );
        }
        // Convert to integers...
        ValueColumn_int = new int[ValueColumn_List.size()];
        for ( int iv = 0; iv < ValueColumn_List.size(); iv++ ) {
            String ValueColumn_String = (String)ValueColumn_List.get(iv);
            for ( int i = 0; i < ColumnNames_List.size(); i++ ) {
                if ( ValueColumn_String.equalsIgnoreCase((String)ColumnNames_List.get(i))) {
                    ValueColumn_int[iv] = i;
                    break;
                }
                if ( ValueColumn_int[iv] < 0 ) {
                    message = "ValueColumn parameter \"" + ValueColumn_String + "\" does not match a known column.";
                    Message.printWarning( 3, routine, message);
                    throw new RuntimeException ( message );
                }
            }
        }
    }
    
    String LocationID = props.getValue ( "LocationID" );
    List LocationID_List = null;
    if ( LocationID == null ) {
        message = "No LocationID parameter has been specified.";
        Message.printWarning( 3, routine, message);
        throw new RuntimeException ( message );
    }
    else {
        // Parse for other code to use...
        LocationID_List = StringUtil.breakStringList ( LocationID, ",", StringUtil.DELIM_ALLOW_STRINGS );
        if ( (LocationID_List == null) || (LocationID_List.size() == 0) ) {
            message = "No LocationID parameter has been specified.";
            Message.printWarning( 3, routine, message);
            throw new RuntimeException ( message );
        }
    }
    
    String DataProviderID = props.getValue ( "DataProviderID" );
    if ( DataProviderID == null ) {
        DataProviderID = "";
    }
    
    String DataType = props.getValue ( "DataType" );
    List DataType_List = null;
    if ( DataType == null ) {
        message = "No DataType parameter has been specified.";
        Message.printWarning( 3, routine, message);
        throw new RuntimeException ( message );
    }
    else {
        // Parse for other code to use...
        DataType_List = StringUtil.breakStringList ( DataType, ",", StringUtil.DELIM_ALLOW_STRINGS );
        if ( (DataType_List == null) || (DataType_List.size() == 0) ) {
            message = "No DataType parameter has been specified.";
            Message.printWarning( 3, routine, message);
            throw new RuntimeException ( message );
        }
    }
    
    String Interval = props.getValue ( "Interval" );
    if ( Interval == null ) {
        Interval = "";
    }
    
    String Scenario = props.getValue ( "Scenario" );
    //List Scenario_List = null;
    if ( Scenario == null ) {
        Scenario = "";
    }
    /* FIXME SAM 2008-02-01 Make scenario be 1 or match data values.
    else {
        // Parse for other code to use...
        Scenario_List = StringUtil.breakStringList ( Scenario, ",", StringUtil.DELIM_ALLOW_STRINGS );
        if ( Scenario_List.size() == 1)
    }
    */
    
    String Units = props.getValue ( "Units" );
    List Units_List = null;
    if ( Units == null ) {
        message = "No Units parameter has been specified.";
        Message.printWarning( 3, routine, message);
        throw new RuntimeException ( message );
    }
    else {
        // Parse for other code to use...
        Units_List = StringUtil.breakStringList ( Units, ",", StringUtil.DELIM_ALLOW_STRINGS );
        if ( (Units_List == null) || (Units_List.size() == 0) ) {
            message = "No Units parameter has been specified.";
            Message.printWarning( 3, routine, message);
            throw new RuntimeException ( message );
        }
    }
    
    // Create a row cursor for the data using the specified delimiter
    // FIXME SAM 2008-02-01 Delimiter should allow more than just a single character
    // FIXME SAM 2008-02-01 TreatConsecutiveDelimitersAsOne needs handled
    CSVCursor row_cursor = new CSVCursor ( new BufferedReader(new FileReader(InputFile_full)), Delimiter, null, ColumnNames_List.size() );
    // Skip over the requested number of rows.
    // FIXME SAM 2008-02-01 SkipRows is actually designed for more than just the lines at
    // the top of the file, but only do that for now
    if ( SkipRows_int > 0 ) {
        for ( int i = 0; i < SkipRows_int; i++ ) {
            row_cursor.next();
        }
    }
    // Create a time series assembler and set the date to the specified column
    // FIXME SAM 2008-02-01 Need to handle separate date and time columns and revisit formats.
    TimeSeriesAssembler assembler = new TimeSeriesAssembler ( row_cursor ).setDateColumn ( DateTimeColumn_int );
    assembler.setDateTimeConverter(Converters.getDateFormatConverter("M/d/yyyy hh:mm:ss a"));
    // Add time series columns based on the data columns
    for ( int i = 0; i < ValueColumn_int.length; i++ ) {
        String tsid_string = LocationID_List.get(i) + "." + DataProviderID + "." +
            DataType_List.get(i) + "." + Interval + "." + Scenario;
        assembler.addTimeSeriesColumn ( ValueColumn_int[i], tsid_string );
    }
    // Now assemble the time series
    // FIXME SAM 2008-02-01 Need a way to assemble time series without reading the data, for discovery mode.
    TS[] ts = assembler.assemble();
    // Transfer to a Vector that adheres to the List interface...
    List tslist = new Vector(ts.length);
    for ( int i = 0; i < ts.length; i++ ) {
        ts[i].setDataUnitsOriginal ( (String)Units_List.get(i) );
        ts[i].setDataUnits ( (String)Units_List.get(i) );
        tslist.add ( ts[i] );
    }
    return tslist;
}

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
@param command_number The number of the command being run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
       CommandWarningException,
       CommandException
{	String routine = "ReadDelimitedFile_Command.runCommand", message;
	int warning_level = 2;
    //int log_level = 3;
	String command_tag = "" + command_number;
	int warning_count = 0;
	    
    // Get and clear the status and clear the run log...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    CommandProcessor processor = getCommandProcessor();

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile");
	String Comment = parameters.getValue("Comment");
	if ( (Comment == null) || Comment.equals("") ) {
	    Comment = "#"; // default
	}
	String Alias = parameters.getValue("Alias");
	//String InputStart = parameters.getValue("InputStart");
    //String InputEnd = parameters.getValue("InputEnd");
    
    DateTime InputStart_DateTime = null;
    DateTime InputEnd_DateTime = null;
    /* FIXME SAM 2008-02-01 Evaluate whether supported
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
            status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time for the input start, " +
                        "or InputStart for the global input start." ) );
        throw new InvalidCommandParameterException ( message );
    }
    }
    else {  // Get the global input start from the processor...
        try {   Object o = processor.getPropContents ( "InputStart" );
                if ( o != null ) {
                    InputStart_DateTime = (DateTime)o;
                }
        }
        catch ( Exception e ) {
            message = "Error requesting the global InputStart from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
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
                status.addToLog ( command_phase,
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
                status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time for the input end, " +
                            "or InputEnd for the global input start." ) );
            throw new InvalidCommandParameterException ( message );
        }
        }
        else {  // Get from the processor...
            try {   Object o = processor.getPropContents ( "InputEnd" );
                    if ( o != null ) {
                        InputEnd_DateTime = (DateTime)o;
                    }
            }
            catch ( Exception e ) {
                message = "Error requesting the global InputEnd from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report problem to software support." ) );
            }
    }
    */

	// Read the file.
    String InputFile_full = InputFile;
    List<TS> tslist = null; // List of time series that is read
	try {
        boolean readData = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            readData = false;
        }
        InputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        
        if ( !IOUtil.fileReadable(InputFile_full) || !IOUtil.fileExists(InputFile_full)) {
            message = "Delimited file \"" + InputFile_full + "\" is not found or accessible.";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
            status.addToLog(commandPhase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,
                    "Verify that the file exists and is readable."));
            throw new CommandException ( message );
        }
        
        // Read everything in the file (one time series or traces).
        List<String> errorMessages = new Vector();
        tslist = readTimeSeriesList ( InputFile_full, getDelimiter(), getTreatConsecutiveDelimitersAsOne(),
            getColumnNames(), getDateTimeColumn(), getValueColumns(),
            Comment, getSkipRows(), getSkipRowsAfterComments(),
            getLocationID(), getProvider(), getDataType(), getInterval(), getScenario(),
            getUnits(), getMissingValue(),
            InputStart_DateTime, InputEnd_DateTime, readData, errorMessages );
        
		if ( tslist != null ) {
			int tscount = tslist.size();
			message = "Read " + tscount + " time series from \"" + InputFile_full + "\"";
			Message.printStatus ( 2, routine, message );
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
            // Further process the time series...
            // This makes sure the period is at least as long as the output period...
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
    
            // Now add the list in the processor...
            
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
Set the column names in the file.
*/
private void setColumnNames ( List<String> columnNames )
{
    __columnNames = columnNames;
}

/**
Set the data type strings for each time series.
*/
private void setDataType ( List<String> dataType )
{
    __dataType = dataType;
}

/**
Set date/time column.
*/
private void setDateTimeColumn ( String dateTimeColumn )
{
    __dateTimeColumn = dateTimeColumn;
}

/**
Set the delimiter that separates columns.
*/
private void setDelimiter ( String delimiter )
{
    __delimiter = delimiter;
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List discovery_TS_List )
{
    __discovery_TS_List = discovery_TS_List;
}

/**
Set the data interval for the time series.
*/
private void setInterval ( TimeInterval interval )
{
    __interval = interval;
}

/**
Set the location ID strings for each time series.
*/
private void setLocationID ( List<String> locationID )
{
    __locationID = locationID;
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
Set the number of rows to skip after the header comments.
*/
private void setSkipRowsAfterComments ( int skipRowsAfterComments )
{
    __skipRowsAfterComments = skipRowsAfterComments;
}

/**
Set whether consecutive delimiters should be treated as one.
*/
private void setTreatConsecutiveDelimitersAsOne ( boolean treatConsecutiveDelimitersAsOne )
{
    __treatConsecutiveDelimitersAsOne = treatConsecutiveDelimitersAsOne;
}

/**
Set the data units strings for each time series.
*/
private void setUnits ( List<String> units )
{
    __units = units;
}

/**
Set the value column names for each time series.
*/
private void setValueColumns ( List<String> valueColumns )
{
    __valueColumns = valueColumns;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

	String InputFile = props.getValue("InputFile" );
    String Delimiter = props.getValue("Delimiter" );
    String ColumnNames = props.getValue("ColumnNames" );
    String DateTimeColumn = props.getValue("DateTimeColumn" );
    String DateTimeFormat = props.getValue("DateTimeFormat" );
    String DateColumn = props.getValue("DateColumn" );
    String TimeColumn = props.getValue("TimeColumn" );
    String ValueColumn = props.getValue("ValueColumn" );
    String Comment = props.getValue("Comment" );
    String SkipRows = props.getValue("SkipRows" );
    String SkipRowsAfterComments = props.getValue("SkipRowsAfterComments" );
    String TreatConsecutiveDelimitersAsOne = props.getValue("TreatConsecutiveDelimitersAsOne" );
    String LocationID = props.getValue("LocationID" );
    String Interval = props.getValue("Interval" );
    String Provider = props.getValue("Provider" );
    String DataType = props.getValue("DataType" );
    String Scenario = props.getValue("Scenario" );
    String Units = props.getValue("Units" );
    String MissingValue = props.getValue("MissingValue" );
    String Alias = props.getValue("Alias" );

	//String InputStart = props.getValue("InputStart");
	//String InputEnd = props.getValue("InputEnd");

	StringBuffer b = new StringBuffer ();

	// Input File
	if ((InputFile != null) && (InputFile.length() > 0)) {
		b.append("InputFile=\"" + InputFile + "\"");
	}
    if ((Delimiter != null) && (Delimiter.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Delimiter=\"" + Delimiter + "\"");
    }
    if ((TreatConsecutiveDelimitersAsOne != null) && (TreatConsecutiveDelimitersAsOne.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("TreatConsecutiveDelimitersAsOne=" + TreatConsecutiveDelimitersAsOne );
    }
    if ((ColumnNames != null) && (ColumnNames.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("ColumnNames=\"" + ColumnNames + "\"");
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
    if ((ValueColumn != null) && (ValueColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("ValueColumn=\"" + ValueColumn + "\"");
    }
    if ((Comment != null) && (Comment.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Comment=\"" + Comment + "\"");
    }
    if ((SkipRows != null) && (SkipRows.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("SkipRows=\"" + SkipRows + "\"");
    }
    if ((SkipRowsAfterComments != null) && (SkipRowsAfterComments.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("SkipRowsAfterComments=\"" + SkipRowsAfterComments + "\"");
    }
    if ((LocationID != null) && (LocationID.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("LocationID=\"" + LocationID + "\"");
    }
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

	/*
	// Input Start
	if ((InputStart != null) && (InputStart.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("InputStart=\"" + InputStart + "\"");
	}

	// Input End
	if ((InputEnd != null) && (InputEnd.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("InputEnd=\"" + InputEnd + "\"");
	}
	*/

	return getCommandName() + "(" + b.toString() + ")";
}

}