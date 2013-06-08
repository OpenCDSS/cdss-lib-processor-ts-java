package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TimeSeriesNotFoundException;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.IOUtil;
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
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the CreateFromList() command.
*/
public class CreateFromList_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

protected final String _Default = "Default";
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public CreateFromList_Command ()
{
	super();
	setCommandName ( "CreateFromList" );
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
	String ListFile = parameters.getValue("ListFile");
	String IDCol = parameters.getValue("IDCol");
	String InputType = parameters.getValue("InputType");
	String Interval = parameters.getValue("Interval");
	String IfNotFound = parameters.getValue("IfNotFound");
    
    if ( (ListFile == null) || (ListFile.length() == 0) ) {
        message = "The list file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing list file." ) );
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
                                message, "Specify an existing list file." ) );
            }
    
        try {
            //String adjusted_path = 
            IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                    TSCommandProcessorUtil.expandParameterValue(processor,this,ListFile)));
        }
        catch ( Exception e ) {
            message = "The list file:\n" +
            "    \"" + ListFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
        }
    }
    
    // IDCol
    if ((IDCol != null) && !IDCol.equals("") && !StringUtil.isInteger(IDCol)) {
        message = "The ID column is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a the ID column as an integer." ) );
    }

	// Interval
	if ((Interval == null) || Interval.equals("")) {
        message = "The interval has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid interval." ) );
	}
	else {
		try {
		    TimeInterval.parseInterval ( Interval );;
		} 
		catch (Exception e) {
            message = "The data interval \"" + Interval + "\" is not valid.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid interval (e.g., 5Minute, 6Hour, Day, Month, Year)" ) );
		}
	}
	
	// Input type
	if ( (InputType == null) || InputType.equals("")) {
        message = "The input type has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid input type (e.g., HydroBase)." ) );
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

	// Check for invalid parameters...
	List<String> valid_Vector = new Vector();
    valid_Vector.add ( "ListFile" );
    valid_Vector.add ( "IDCol" );
    valid_Vector.add ( "Delim" );
    valid_Vector.add ( "ID" );
    valid_Vector.add ( "DataSource" );
    valid_Vector.add ( "DataType" );
    valid_Vector.add ( "Interval" );
    valid_Vector.add ( "Scenario" );
    valid_Vector.add ( "InputType" );
    valid_Vector.add ( "InputName" );
    valid_Vector.add ( "IfNotFound" );
    valid_Vector.add ( "DefaultUnits" );
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
	return ( new CreateFromList_JDialog ( parent, this ) ).ok();
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
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Parse the command string into a PropList of parameters.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{
    // Call the base class method for basic parsing.
    super.parseCommand( command_string );
    // Update syntax for new parameter name...
    PropList parameters = getCommandParameters();
    String HandleMissingTSHow = parameters.getValue("HandleMissingTSHow");
    String IfNotFound = _Warn; // Default
    if ( HandleMissingTSHow != null ) {
        // Convert to IfNotFound
        parameters.unSet( "HandleMissingTSHow" );
        if ( HandleMissingTSHow.equalsIgnoreCase("DefaultMissingTS")) {
            IfNotFound = _Default;
        }
        else if ( HandleMissingTSHow.equalsIgnoreCase("IgnoreMissingTS")) {
            IfNotFound = _Ignore;
        }
        parameters.set( "IfNotFound", IfNotFound );
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
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
       CommandWarningException,
       CommandException
{	String routine = "CreateFromList_Command.runCommand", message;
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
	String ListFile = parameters.getValue("ListFile");
    String IDCol = parameters.getValue ( "IDCol" );
    if ( (IDCol == null) || IDCol.equals("") ) {
        IDCol = "1";    // Default
    }
    int IDCol_int = StringUtil.atoi ( IDCol ) - 1;  // 0+ internally
    String Delim = parameters.getValue ( "Delim" );
    if ( Delim == null ) {
        Delim = ","; // Default
    }
    String ID = parameters.getValue ( "ID" );
    if ( ID == null ) {
        ID = "*"; // Default
    }
    String idpattern_Java = StringUtil.replaceString(ID,"*",".*");
    String DataSource = parameters.getValue ( "DataSource" );
    if ( DataSource == null ) {
        DataSource = "";
    }
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
    String InputType = parameters.getValue ( "InputType" );
    String InputName = parameters.getValue ( "InputName" );
    if ( InputName == null ) {
        // Set to empty string so check to facilitate processing...
        InputName = "";
    }
    String IfNotFound = parameters.getValue("IfNotFound");
    if ( (IfNotFound == null) || IfNotFound.equals("")) {
        IfNotFound = _Warn; // default
    }
    String DefaultUnits = parameters.getValue("DefaultUnits");
    
	// Read the file.
    List<TS> tslist = new Vector();   // Keep the list of time series
    String ListFile_full = ListFile;
	try {
        boolean read_data = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ){
            read_data = false;
        }
        ListFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                        TSCommandProcessorUtil.expandParameterValue(processor,this,ListFile)));
     
        // Read using the table...
    
        PropList props = new PropList ("");
        props.set ( "Delimiter=" + Delim ); // see existing prototype
        props.set ( "CommentLineIndicator=#" ); // New - skip lines that start with this
        props.set ( "TrimStrings=True" );   // If true, trim strings after reading.
        DataTable table = DataTable.parseFile ( IOUtil.getPathUsingWorkingDir(ListFile_full), props );
        
        int tsize = 0;
        if ( table != null ) {
            tsize = table.getNumberOfRecords();
        }
    
        Message.printStatus ( 2, "", "List file has " + tsize + " records and " + table.getNumberOfFields() + " fields" );
    
        // Loop through the records in the table and match the identifiers...
    
        StringBuffer tsident_string = new StringBuffer();
        TableRecord rec = null;
        String id;
        TS ts = null;
        for ( int i = 0; i < tsize; i++ ) {
            rec = table.getRecord ( i );
            id = (String)rec.getFieldValue ( IDCol_int );
            if ( id.equals("") ) {
                // Probably a blank line in the list file - no need to generate a command warning
                Message.printStatus ( 2, routine, "Missing identifier for time series - " +
                	"probably a blank line in the list file - skipping time series " + (i + 1));
                continue;
            }
            if ( !StringUtil.matchesIgnoreCase(id,idpattern_Java) ) {
                // Does not match...
                continue;
            }
    
            tsident_string.setLength(0);
            tsident_string.append ( id + "." + DataSource + "." + DataType + "." + Interval + "~" + InputType );
            if ( InputName.length() > 0 ) {
                tsident_string.append ( "~" + InputName );
            }
            boolean notFoundLogged = false;
            try {
                // Make a request to the processor...
                String TSID = tsident_string.toString();
                notifyCommandProgressListeners ( i, tsize, (float)-1.0, "Creating time series " + TSID);
                PropList request_params = new PropList ( "" );
                request_params.set ( "TSID", tsident_string.toString() );
                request_params.setUsingObject ( "WarningLevel", new Integer(warning_level) );
                request_params.set ( "CommandTag", command_tag );
                request_params.set ( "IfNotFound", IfNotFound );
                request_params.setUsingObject ( "ReadData", new Boolean(read_data) );
                CommandProcessorRequestResultsBean bean = null;
                try {
                    bean = processor.processRequest( "ReadTimeSeries", request_params);
                    PropList bean_PropList = bean.getResultsPropList();
                    Object o_TS = bean_PropList.getContents ( "TS" );
                    if ( o_TS != null ) {
                        ts = (TS)o_TS;
                    }
                }
                catch ( TimeSeriesNotFoundException e ) {
                    message = "Time series could not be found using identifier \"" + TSID + "\".";
                    if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the identifier information is correct." ) );
                    }
                    else {
                        // Non-fatal - ignoring or defaulting time series.
                        message += "  Non-fatal because IfNotFound=" + IfNotFound;
                        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify that the identifier information is correct." ) );
                    }
                    ts = null;
                    notFoundLogged = true;
                }
                catch ( Exception e ) {
                    message = "Error requesting ReadTimeSeries(TSID=\"" + TSID + "\") from processor + (exception: " +
                    e + ").";
                    //Message.printWarning(3, routine, e );
                    Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify that the identifier information is correct.  Check the log file." +
                            		"  If still a problem, report the problem to software support." ) );
                    ts = null;
                }
                if ( ts == null ) {
                    if ( !notFoundLogged ) {
                        // Only want to include a warning once.
                        // This is kind of ugly because currently there is not consistency between all
                        // time series readers in error handling, which is difficult to handle in this
                        // generic command.
                        message = "Time series could not be found using identifier \"" + TSID + "\".";
                        if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
                            status.addToLog ( commandPhase,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Verify that the identifier information is correct." ) );
                        }
                        else {
                            // Non-fatal - ignoring or defaulting time series.
                            message += "  Non-fatal because IfNotFound=" + IfNotFound;
                            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Verify that the identifier information is correct." ) );
                        }
                    }
                    // Always check for output period because required for default time series.
                    if ( IfNotFound.equalsIgnoreCase(_Default) &&
                        ((processor.getPropContents("OutputStart") == null) ||
                        (processor.getPropContents("OutputEnd") == null)) ) {
                        message = "Time series could not be found using identifier \"" + TSID + "\"." +
                        		"  Requesting default time series but no output period is defined.";
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Set the output period before calling this command." ) );
                    }
                }
                else {
                    if ( (DefaultUnits != null) && (ts.getDataUnits().length() == 0) ) {
                        // Time series has no units so assign default.
                        ts.setDataUnits ( DefaultUnits );
                    }
                    tslist.add ( ts );
                }
            }
            catch ( Exception e1 ) {
                message = "Unexpected error reading time series \"" + tsident_string + "\" (" + e1 + ")";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(
                        command_tag, ++warning_count ),
                    routine, message );
                Message.printWarning ( 3, routine, e1 );
                status.addToLog(commandPhase,
                        new CommandLogRecord( CommandStatusType.FAILURE, message,"Check the log file for details."));
                throw new CommandException ( message );
            }
            /* TODO SAM 2008-09-15 Evaluate how to re-implement
            // Cancel processing if the user has indicated to do so...
            if ( __ts_processor.getCancelProcessingRequested() ) {
                return;
            }
            */
    	}
    }
	catch ( Exception e ) {
		message = "Unexpected error reading time series for list file. \"" + ListFile_full + "\" (" + e + ")";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog(commandPhase,
                new CommandLogRecord( CommandStatusType.FAILURE, message,"Check the log file for details."));
		throw new CommandException ( message );
	}
    
    if ( commandPhase == CommandPhaseType.RUN ) {
        if ( tslist != null ) {
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
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

    String ListFile = props.getValue ( "ListFile" );
    String IDCol = props.getValue ( "IDCol" );
    String Delim = props.getValue ( "Delim" );
    String ID = props.getValue ( "ID" );
    String DataSource = props.getValue ( "DataSource" );
    String DataType = props.getValue ( "DataType" );
    String Interval = props.getValue ( "Interval" );
    String Scenario = props.getValue ( "Scenario" );
    String InputType = props.getValue ( "InputType" );
    String InputName = props.getValue ( "InputName" );
    String IfNotFound = props.getValue ( "IfNotFound" );
    String DefaultUnits = props.getValue ( "DefaultUnits" );

	StringBuffer b = new StringBuffer ();

	if ((ListFile != null) && (ListFile.length() > 0)) {
		b.append("ListFile=\"" + ListFile + "\"");
	}
    if ((IDCol != null) && (IDCol.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("IDCol=" + IDCol );
    }
	if ((Delim != null) && (Delim.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("Delim=\"" + Delim + "\"");
	}
	if ((ID != null) && (ID.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("ID=\"" + ID + "\"");
	}
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
        b.append("Interval=\"" + Interval + "\"");
    }
    if ((Scenario != null) && (Scenario.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Scenario=\"" + Scenario + "\"");
    }
    if ((InputType != null) && (InputType.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("InputType=\"" + InputType + "\"");
    }
    if ((InputName != null) && (InputName.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("InputName=\"" + InputName + "\"");
    }
    if ((IfNotFound != null) && (IfNotFound.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("IfNotFound=" + IfNotFound );
    }
    if ((DefaultUnits != null) && (DefaultUnits.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DefaultUnits=\"" + DefaultUnits + "\"");
    }

	return getCommandName() + "(" + b.toString() + ")";
}

}