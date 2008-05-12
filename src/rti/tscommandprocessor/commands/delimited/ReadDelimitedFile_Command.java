package rti.tscommandprocessor.commands.delimited;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileReader;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
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
runCommand() methods (prevent code duplication parsing dateTime strings).  
*/
private DateTime __InputStart = null;
private DateTime __InputEnd   = null;

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List __discovery_TS_List = null;

/**
Indicates whether the TS Alias version of the command is being used.
*/
//protected boolean _use_alias = false;
//TODO SAM 2008-01-31 Evaluate whether TS Alias = version is needed. For now do not plan on it.

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
    String SkipRows = parameters.getValue("SkipRows" );
    String Delimiter = parameters.getValue("Delimiter" );
    String TreatConsecutiveDelimitersAsOne = parameters.getValue("TreatConsecutiveDelimitersAsOne" );
    String ColumnNames = parameters.getValue("ColumnNames" );
    String DateColumn = parameters.getValue("DateColumn" );
    String TimeColumn = parameters.getValue("TimeColumn" );
    String DateTimeColumn = parameters.getValue("DateTimeColumn" );
    String ValueColumn = parameters.getValue("ValueColumn" );
    String LocationID = parameters.getValue("LocationID" );
    String DataProviderID = parameters.getValue("DataProviderID" );
    String DataType = parameters.getValue("DataType" );
    String Interval = parameters.getValue("Interval" );
    String Scenario = parameters.getValue("Scenario" );
    String Units = parameters.getValue("Units" );
    String MissingValue = parameters.getValue("MissingValue" );
    String Alias = parameters.getValue("Alias" );
	String InputStart = parameters.getValue("InputStart");
	String InputEnd   = parameters.getValue("InputEnd");
	
	/*
	String Alias = parameters.getValue("Alias");
    
	if ( _use_alias && ((Alias == null) || Alias.equals("")) ) {
	    message = "The Alias must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an alias." ) );
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
    */

    if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
    }
    else {  String working_dir = null;
        try { Object o = processor.getPropContents ( "WorkingDir" );
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
            //String adjusted_path = 
            IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, InputFile));
        }
        catch ( Exception e ) {
            message = "The input file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
        }
    }
    
    List ColumnNames_List = null;
    if ( (ColumnNames == null) || (ColumnNames.length() == 0) ) {
        message = "The column names must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify column names for all columns in the file." ) );
    }
    else {
        // Parse out the column names so that they can be used in checks below.
        ColumnNames_List = StringUtil.breakStringList ( ColumnNames, ",", StringUtil.DELIM_ALLOW_STRINGS );
    }
    
    if ( (DateTimeColumn == null) || (DateTimeColumn.length() == 0) ) {
        message = "The date/time column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a date/time column as one of the values from ColumnNames." ) );
    }
    else {
        if ( ColumnNames_List != null ) {
            boolean found = false;
            for ( int i = 0; i < ColumnNames_List.size(); i++ ) {
                if ( DateTimeColumn.equalsIgnoreCase((String)ColumnNames_List.get(i)) ) {
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                message = "The DateTimeColumn (" + DateTimeColumn + ") is not a recognized column name.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a date/time column as one of the values from ColumnNames." ) );
            }
        }
    }
    
    if ( (ValueColumn == null) || (ValueColumn.length() == 0) ) {
        message = "The data column(s) must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify data column(s) as values from ColumnNames, separated by commas." ) );
    }
    else {
        if ( ColumnNames_List != null ) {
            boolean found = false;
            for ( int i = 0; i < ColumnNames_List.size(); i++ ) {
                if ( ValueColumn.equalsIgnoreCase((String)ColumnNames_List.get(i)) ) {
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                message = "The ValueColumn (" + ValueColumn + ") is not a recognized column name.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify data column(s) as values from ColumnNames, separated by commas." ) );
            }
        }
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
    
	// Check for invalid parameters...
    Vector valid_Vector = new Vector();
    /*
    if ( _use_alias ) {
        valid_Vector.add ( "TSID" );
    }
    */
    valid_Vector.add ( "InputFile" );
    valid_Vector.add ( "SkipRows" );
    valid_Vector.add ( "Delimiter" );
    valid_Vector.add ( "TreatConsecutiveDelimitersAsOne" );
    valid_Vector.add ( "ColumnNames" );
    valid_Vector.add ( "DateColumn" );
    valid_Vector.add ( "TimeColumn" );
    valid_Vector.add ( "DateTimeColumn" );
    valid_Vector.add ( "ValueColumn" );
    valid_Vector.add ( "LocationID" );
    valid_Vector.add ( "DataProviderID" );
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
			MessageUtil.formatMessageTag(
				command_tag, warning_level ),
			warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
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
	__InputStart = null;
	__InputEnd   = null;
	super.finalize();
}

/**
Return the list of time series read in discovery phase.
*/
private List getDiscoveryTSList ()
{
    return __discovery_TS_List;
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

// Use the base class parseCommand() method

/**
Read a list of time series from a delimited file.
@param InputFile_full the full path to the input file.
@param InputStart_DateTime requested start of data (null to return all).
@param InputEnd_DateTime requested end of data (null to return all).
@param NewUnits New data units (null to use units in file).
@param read_data True to read data, false to only read the header information.
@param props Properties to control the read, from command parameters.
*/
private List readTimeSeriesList ( String InputFile_full,
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
    List Scenario_List = null;
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
    Vector tslist = new Vector(ts.length);
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
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
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
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
       CommandWarningException,
       CommandException
{	String routine = "ReadDelimitedFile_Command.runCommand", message;
	int warning_level = 2;
    int log_level = 3;
	String command_tag = "" + command_number;
	int warning_count = 0;
	    
    // Get and clear the status and clear the run log...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    CommandProcessor processor = getCommandProcessor();

	// Get the command properties not already stored as members.
	PropList parameters = getCommandParameters();
	String InputFile = parameters.getValue("InputFile");
	String NewUnits = parameters.getValue("NewUnits");
	//String InputStart = parameters.getValue("InputStart");
	//String InputEnd = parameters.getValue("InputEnd");
	String Alias = parameters.getValue("Alias");   // Alias version
    //String TSID = parameters.getValue("TSID");  // Alias version
    
	/* FIXME SAM 2008-02-01 Evaluate whether supported
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
    List tslist = null; // List of time series that is read
	try {
        boolean read_data = true;
        if ( command_phase == CommandPhaseType.DISCOVERY ){
            read_data = false;
        }
        InputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),InputFile));
        
        // Read everything in the file (one time series or traces).
        tslist = readTimeSeriesList (
                InputFile_full, null, null, null, read_data, parameters );
			
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
	                     processor, ts, Alias, status, command_phase) );
	            }
	        }
		}
	} 
	catch ( Exception e ) {
		message = "Unexpected error reading delimited file. \"" + InputFile_full + "\" (" + e + ")";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(command_phase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,"Check the log file for details."));
		throw new CommandException ( message );
	}
    
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }

    if ( command_phase == CommandPhaseType.RUN ) {
        if ( tslist != null ) {
            // Further process the time series...
            // This makes sure the period is at least as long as the output period...
            int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
            if ( wc > 0 ) {
                message = "Error post-processing series after read.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( command_phase,
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
                    status.addToLog ( command_phase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
        }
    }
    else if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( tslist );
    }

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count ), routine, message );
		throw new CommandWarningException ( message );
	}
    
    status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List discovery_TS_List )
{
    __discovery_TS_List = discovery_TS_List;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

	//String Alias = props.getValue("Alias");
    //String TSID = props.getValue("TSID");
	String InputFile = props.getValue("InputFile" );
    String SkipRows = props.getValue("SkipRows" );
    String Delimiter = props.getValue("Delimiter" );
    String TreatConsecutiveDelimitersAsOne = props.getValue("TreatConsecutiveDelimitersAsOne" );
    String ColumnNames = props.getValue("ColumnNames" );
    String DateColumn = props.getValue("DateColumn" );
    String TimeColumn = props.getValue("TimeColumn" );
    String DateTimeColumn = props.getValue("DateTimeColumn" );
    String ValueColumn = props.getValue("ValueColumn" );
    String LocationID = props.getValue("LocationID" );
    String DataProviderID = props.getValue("DataProviderID" );
    String DataType = props.getValue("DataType" );
    String Interval = props.getValue("Interval" );
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
    
	/*
    if ( _use_alias ) {
        if ((TSID != null) && (TSID.length() > 0)) {
            if (b.length() > 0) {
                b.append(",");
            }
            b.append("TSID=\"" + TSID + "\"");
        }
    }
    */

    if ((SkipRows != null) && (SkipRows.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("SkipRows=\"" + SkipRows + "\"");
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
    if ((DateTimeColumn != null) && (DateTimeColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DateTimeColumn=\"" + DateTimeColumn + "\"");
    }
    if ((ValueColumn != null) && (ValueColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("ValueColumn=\"" + ValueColumn + "\"");
    }
    if ((LocationID != null) && (LocationID.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("LocationID=\"" + LocationID + "\"");
    }
    if ((DataProviderID != null) && (DataProviderID.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DataProviderID=\"" + DataProviderID + "\"");
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

    String lead = "";
    /*
	if ( _use_alias && (Alias != null) && (Alias.length() > 0) ) {
		lead = "TS " + Alias + " = ";
	}
	*/

	return lead + getCommandName() + "(" + b.toString() + ")";
}

}
