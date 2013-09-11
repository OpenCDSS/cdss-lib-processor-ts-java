package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
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
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the CreateFromList() command.
*/
public class ReadTimeSeriesList_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
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
public ReadTimeSeriesList_Command ()
{
	super();
	setCommandName ( "ReadTimeSeriesList" );
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
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// Get the property values. 
	String TableID = parameters.getValue("TableID");
	String LocationColumn = parameters.getValue("LocationColumn");
	String DataSourceColumn = parameters.getValue("DataSourceColumn");
	String DataSource = parameters.getValue("DataSource");
	String DataTypeColumn = parameters.getValue("DataTypeColumn");
    String DataType = parameters.getValue("DataType");
	String DataStore = parameters.getValue("DataStore");
	String Interval = parameters.getValue("Interval");
	String IfNotFound = parameters.getValue("IfNotFound");
    
    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
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
	
	if ( (DataStore == null) || DataStore.equals("")) {
        message = "The datastore has not been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid datastore (e.g., HydroBase)." ) );
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
	List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "TableID" );
    valid_Vector.add ( "LocationColumn" );
    valid_Vector.add ( "DataSourceColumn" );
    valid_Vector.add ( "DataSource" );
    valid_Vector.add ( "DataTypeColumn" );
    valid_Vector.add ( "DataType" );
    valid_Vector.add ( "Interval" );
    valid_Vector.add ( "Scenario" );
    valid_Vector.add ( "DataStore" );
    valid_Vector.add ( "InputName" );
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "IfNotFound" );
    valid_Vector.add ( "DefaultUnits" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	// The command will be modified if changed...
	return ( new ReadTimeSeriesList_JDialog ( parent, this, tableIDChoices ) ).ok();
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
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getClass() + ".runCommand", message;
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
	String TableID = parameters.getValue("TableID");
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
    String DataStore = parameters.getValue ( "DataStore" );
    String InputName = parameters.getValue ( "InputName" );
    if ( InputName == null ) {
        // Set to empty string so check to facilitate processing...
        InputName = "";
    }
    String Alias = parameters.getValue ( "Alias" );
    String IfNotFound = parameters.getValue("IfNotFound");
    if ( (IfNotFound == null) || IfNotFound.equals("")) {
        IfNotFound = _Warn; // default
    }
    String DefaultUnits = parameters.getValue("DefaultUnits");
    
    // Get the table to process.

    DataTable table = null;
    int locationColumnNum = -1, dataSourceColumnNum = -1, dataTypeColumnNum = -1;
    if ( commandPhase == CommandPhaseType.RUN ) {
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
                table = (DataTable)o_Table;
                try {
                    locationColumnNum = table.getFieldIndex(LocationColumn);
                }
                catch ( Exception e ) {
                    message = "Unable to find location column \"" + LocationColumn + "\" for TableID=\"" + TableID + "\".";
                    Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                    status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
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
                        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
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
                        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a table exists with the column \"" + DataTypeColumn + "\"." ) );
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
    List<TS> tslist = new Vector<TS>();   // Keep the list of time series
    String dataSource;
	try {
        boolean readData = true;
        if ( commandPhase == CommandPhaseType.DISCOVERY ){
            readData = false;
        }
        else if ( commandPhase == CommandPhaseType.RUN ){
            // TODO SAM 2013-05-17 Need to determine whether to read table even in discovery mode
            // Otherwise won't be able to generate time series in discovery ode
    
            // Loop through the records in the table and match the identifiers...
        
            StringBuffer tsidentString = new StringBuffer();
            TableRecord rec = null;
            String locationID;
            TS ts = null;
            String dataType;
            String tsid1 = null; // The TSID corresponding to the first data source, used for default time series
            int tsize = table.getNumberOfRecords();
            for ( int i = 0; i < tsize; i++ ) {
                rec = table.getRecord ( i );
                locationID = rec.getFieldValueString ( locationColumnNum );
                // Skip blank location identifiers
                if ( (locationID == null) || (locationID.trim().length() == 0) ) {
                    continue;
                }
                // Data type
                if ( dataTypeColumnNum >= 0 ) {
                    dataType = rec.getFieldValueString ( dataTypeColumnNum );
                }
                else {
                    dataType = DataType;
                }
                // Allow more than one data source to be specified, which is useful when there is mixed ownership of stations
                int nDataSource = 1;
                if ( dataSourceList.length > 0 ) {
                    nDataSource = dataSourceList.length;
                }
                boolean notFoundLogged = false;
                for ( int iDataSource = 0; iDataSource < nDataSource; iDataSource++ ) {
                    tsidentString.setLength(0);
                    if ( dataSourceList.length == 0 ) {
                        if ( dataSourceColumnNum >= 0 ) {
                            // Get the data source from the table
                            dataSource = rec.getFieldValueString ( dataSourceColumnNum );
                        }
                        else {
                            dataSource = "";
                        }
                    }
                    else {
                        dataSource = dataSourceList[iDataSource];
                    }
                    tsidentString.append ( locationID + "." + dataSource + "." + dataType + "." + Interval + "~" + DataStore );
                    if ( InputName.length() > 0 ) {
                        tsidentString.append ( "~" + InputName );
                    }
                    String tsid = tsidentString.toString();;
                    if ( iDataSource == 0 ) {
                        // Keep the first TSID if a default is needed
                        tsid1 = tsid;
                    }
                    try {
                        // Make a request to the processor to read a time series...
                        notifyCommandProgressListeners ( i, tsize, (float)-1.0, "Creating time series " + tsid);
                        PropList request_params = new PropList ( "" );
                        request_params.set ( "TSID", tsidentString.toString() );
                        request_params.setUsingObject ( "WarningLevel", new Integer(warning_level) );
                        request_params.set ( "CommandTag", command_tag );
                        request_params.set ( "IfNotFound", IfNotFound );
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
                        catch ( TimeSeriesNotFoundException e ) {
                            message = "Time series could not be found using identifier \"" + tsid + "\" (" + e + ").";
                            if ( IfNotFound.equalsIgnoreCase(_Warn) && (iDataSource == (nDataSource - 1)) ) {
                                status.addToLog ( commandPhase,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Verify that the identifier information is correct." ) );
                            }
                            else {
                                // Non-fatal - ignoring, going to try another data source, or defaulting time series.
                                if ( (iDataSource == (nDataSource - 1)) ) {
                                    message += "  Non-fatal because IfNotFound=" + IfNotFound;
                                    status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                                        message, "Verify that the identifier information is correct." ) );
                                }
                                else {
                                    // First data source(s) in list OK to skip over
                                    message += "  Non-fatal because multipe data sources are being tried";
                                    status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.INFO,
                                        message, "Expect subsequent data source to match time series." ) );
                                }
                            }
                            ts = null;
                            notFoundLogged = true;
                        }
                        catch ( Exception e ) {
                            message = "Error requesting ReadTimeSeries(TSID=\"" + tsid + "\") from processor + (" + e + ").";
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
                                message = "Time series could not be found using identifier \"" + tsid + "\".";
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
                                message = "Time series could not be found using identifier \"" + tsid1 + "\"." +
                                		"  Requesting default time series but no output period is defined.";
                                status.addToLog ( commandPhase,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Set the output period before calling this command." ) );
                            }
                        }
                        else {
                            // Found a time series so break out of data source loop.
                            if ( (DefaultUnits != null) && (ts.getDataUnits().length() == 0) ) {
                                // Time series has no units so assign default.
                                ts.setDataUnits ( DefaultUnits );
                            }
                            if ( (ts != null) && (Alias != null) && !Alias.equals("") ) {
                                String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                                    processor, ts, Alias, status, commandPhase);
                                ts.setAlias ( alias );
                            }
                            tslist.add ( ts );
                            break;
                        }
                    }
                    catch ( Exception e1 ) {
                        message = "Unexpected error reading time series \"" + tsid + "\" (" + e1 + ")";
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

    String TableID = props.getValue ( "TableID" );
    String LocationColumn = props.getValue ( "LocationColumn" );
    String DataSourceColumn = props.getValue ( "DataSourceColumn" );
    String DataSource = props.getValue ( "DataSource" );
    String DataTypeColumn = props.getValue ( "DataTypeColumn" );
    String DataType = props.getValue ( "DataType" );
    String Interval = props.getValue ( "Interval" );
    String Scenario = props.getValue ( "Scenario" );
    String DataStore = props.getValue ( "DataStore" );
    String InputName = props.getValue ( "InputName" );
    String Alias = props.getValue ( "Alias" );
    String IfNotFound = props.getValue ( "IfNotFound" );
    String DefaultUnits = props.getValue ( "DefaultUnits" );

	StringBuffer b = new StringBuffer ();

	if ((TableID != null) && (TableID.length() > 0)) {
		b.append("TableID=\"" + TableID + "\"");
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
	if ((DataSource != null) && (DataSource.length() > 0)) {
		if (b.length() > 0) {
			b.append(",");
		}
		b.append("DataSource=\"" + DataSource + "\"");
	}
    if ((DataTypeColumn != null) && (DataTypeColumn.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DataTypeColumn=\"" + DataTypeColumn + "\"");
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
    if ((DataStore != null) && (DataStore.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("DataStore=\"" + DataStore + "\"");
    }
    if ((InputName != null) && (InputName.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("InputName=\"" + InputName + "\"");
    }
    if ((Alias != null) && (Alias.length() > 0)) {
        if (b.length() > 0) {
            b.append(",");
        }
        b.append("Alias=\"" + Alias + "\"");
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