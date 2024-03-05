// CopyTimeSeriesPropertiesToTable_Command - This class initializes, checks, and runs the CopyTimeSeriesPropertiesToTable() command.

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
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import RTi.TS.TS;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the CopyTimeSeriesPropertiesToTable() command.
*/
public class CopyTimeSeriesPropertiesToTable_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Used with AllowDuplicates.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
The table that is created (when not operating on an existing table).
*/
private DataTable __table = null;

/**
Constructor.
*/
public CopyTimeSeriesPropertiesToTable_Command () {
    super();
    setCommandName ( "CopyTimeSeriesPropertiesToTable" );
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
    String IncludeProperties = parameters.getValue ( "IncludeProperties" );
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String AllowDuplicates = parameters.getValue ( "AllowDuplicates" );
    String TableOutputColumns = parameters.getValue ( "TableOutputColumns" );
    String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (TableID == null) || TableID.isEmpty() ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the identifier for the table to process." ) );
    }

    if ( (TableTSIDColumn == null) || TableTSIDColumn.equals("") ) {
        message = "The TableTSID column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a table column name for the TSID." ) );
    }

    if ( (IncludeProperties != null) && !IncludeProperties.equals("") &&
        (TableOutputColumns != null) && !TableOutputColumns.equals("") ) {
        String[] includeProperties = IncludeProperties.split(",");
        String[] tableOutputColumns = TableOutputColumns.split(",");
        if ( includeProperties.length != tableOutputColumns.length ) {
            message = "The number of include properties (" +
                ") and the number of specified table output columns (" + ") is different.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the same number of include properties names as output columns." ) );
        }
    }

    if ( (AllowDuplicates != null) && !AllowDuplicates.equals("") && !AllowDuplicates.equalsIgnoreCase(_False) &&
        !AllowDuplicates.equalsIgnoreCase(_True) ) {
        message = "The AllowDuplicates value (" + AllowDuplicates + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specity the value as " + _False + " or " + _True + " (default)." ) );
    }

    // Check for invalid parameters.
    List<String> validList = new ArrayList<>(9);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "IncludeProperties" );
    validList.add ( "TableID" );
    validList.add ( "TableTSIDColumn" );
    validList.add ( "TableTSIDFormat" );
    validList.add ( "AllowDuplicates" );
    validList.add ( "TableOutputColumns" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

    if ( warning.length() > 0 ) {
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(command_tag,warning_level),
        warning );
        throw new InvalidCommandParameterException ( warning );
    }

    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent ) {
    List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    return (new CopyTimeSeriesPropertiesToTable_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable() {
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
@return a list of objects of the requested type
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    DataTable table = getDiscoveryTable();
    List<T> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new ArrayList<>();
        v.add ( (T)table );
    }
    return v;
}

/**
Override parent parseCommand() so that parameter name can be changed.
@param commandString command string to parse
*/
public void parseCommand ( String commandString )
throws InvalidCommandParameterException, InvalidCommandSyntaxException {
	super.parseCommand(commandString);
	// Now replace "PropertyNames" with "IncludeProperties".
	PropList params = getCommandParameters();
	String propVal = params.getValue("PropertyNames");
	if ( (propVal != null) && !propVal.isEmpty() ) {
		params.set("IncludeProperties=" + propVal );
		params.unSet("PropertyNames");
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
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException {
    String message, routine = getCommandName() + "_Command.runCommand";
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    int log_level = 3;
    //int log_level = 3;  // Level for non-use messages for log file.

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

    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

    // Get the input parameters.

    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
    String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String IncludeProperties = parameters.getValue ( "IncludeProperties" );
    String [] includeProperties = null;
    if ( (IncludeProperties != null) && !IncludeProperties.equals("") ) {
        includeProperties = IncludeProperties.trim().split(",");
        // Remove surrounding whitespace.
        for ( int i = 0; i < includeProperties.length; i++ ) {
        	includeProperties[i] = includeProperties[i].trim();
        }
    }
    String TableID = parameters.getValue ( "TableID" );
    if ( (TableID != null) && !TableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) ) {
    	// In discovery mode want lists of tables to include ${Property}.
    	if ( TableID.indexOf("${") >= 0 ) {
    		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    	}
    }
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String AllowDuplicates = parameters.getValue ( "AllowDuplicates" );
    boolean allowDuplicates = false; // Default
    if ( (AllowDuplicates != null) && AllowDuplicates.equalsIgnoreCase(_True) ) {
        allowDuplicates = true;
    }
    String TableOutputColumns = parameters.getValue ( "TableOutputColumns" );
    String [] tableOutputColumnNames0 = null;
    if ( (TableOutputColumns != null) && !TableOutputColumns.equals("") ) {
        tableOutputColumnNames0 = TableOutputColumns.split(",");
        // These are expanded below based on dynamic time series properties.
    }

    // Get the table to process.

    DataTable table = null;
    PropList request_params = null;
    CommandProcessorRequestResultsBean bean = null;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be updated/created.
        request_params = new PropList ( "" );
        request_params.set ( "TableID", TableID );
        try {
            bean = processor.processRequest( "GetTable", request_params);
            PropList bean_PropList = bean.getResultsPropList();
            Object o_Table = bean_PropList.getContents ( "Table" );
            if ( o_Table != null ) {
                // Found the table so no need to create it.
                table = (DataTable)o_Table;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
    }

    // Get the time series to process.  Allow TSID to be a pattern or specific time series.
    List<TS> tslist = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command.
        // FIXME - SAM 2011-02-02 This gets all the time series, not just the ones matching the request.
        tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, EnsembleID );
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        request_params = new PropList ( "" );
        request_params.set ( "TSList", TSList );
        request_params.set ( "TSID", TSID );
        request_params.set ( "EnsembleID", EnsembleID );
        try {
            bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
            "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
        }
        if ( bean == null ) {
            Message.printStatus ( 2, routine, "Bean is null.");
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
        if ( o_TSList == null ) {
            message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
            Message.printWarning ( log_level, MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
        else {
        	@SuppressWarnings("unchecked")
			List<TS> tslist0 = (List<TS>)o_TSList;
            tslist = tslist0;
            if ( tslist.size() == 0 ) {
                message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
                Message.printWarning ( log_level, MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
            }
        }

        int nts = tslist.size();
        if ( nts == 0 ) {
            message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
                "\", EnsembleID=\"" + EnsembleID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
    }

    if ( warning_count > 0 ) {
        // Input error.
        message = "Insufficient data to run command.";
        status.addToLog ( commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

    // Now process.

    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        if ( table == null ) {
            // Did not find table so is being created in this command.
            // Create an empty table and set the ID.
            table = new DataTable();
            table.setTableID ( TableID );
            setDiscoveryTable ( table );
        }
        else {
        	// The table was created in a previous command so don't need to add here.
        }
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        if ( table == null ) {
            // Did not find the table above so create it.
            table = new DataTable( /*columnList*/ );
            table.setTableID ( TableID );
            Message.printStatus(2, routine, "Was not able to match existing table \"" + TableID + "\" so created new table.");

            // Set the table in the processor.

            request_params = new PropList ( "" );
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
        int nts = 0;
        if ( tslist != null ) {
            nts = tslist.size();
        }
        try {
            TS ts = null;
            Object o_ts = null;
            int TableTSIDColumnNumber = -1;
            Message.printStatus(2, routine, "Copying properties to table \"" + TableID + "\" for " + nts + " time series");
            for ( int its = 0; its < nts; its++ ) {
                // The the time series to process, from the list that was returned above.
                message = "Copying properties for time series " + (its + 1) + " of " + nts;
                notifyCommandProgressListeners ( its, nts, (float)-1.0, message );
                // Reset the tableColumnNames for each time series.
                String [] tableOutputColumnNames = null;
                if ( tableOutputColumnNames0 != null ) {
                	tableOutputColumnNames = new String[tableOutputColumnNames0.length];
                	for ( int i = 0; i < tableOutputColumnNames0.length; i++ ) {
                		tableOutputColumnNames[i] = tableOutputColumnNames0[i];
                	}
                }
                o_ts = tslist.get(its);
                if ( o_ts == null ) {
                    message = "Time series " + (its + 1) + " to process is null - skipping.";
                    Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
                    // Go to next time series.
                    continue;
                }
                ts = (TS)o_ts;

                // Get the properties to process:
                // - this must be done for each time series because the properties may be different between time series
                if ( IncludeProperties == null ) {
                    // Get all the properties by forming a list of property names from the hashtable.
                    HashMap<String, Object> propertyHash = ts.getProperties();
                    ArrayList<String> keyList = new ArrayList<>(propertyHash.keySet());
                    // Don't sort because original order has meaning.
                    //Collections.sort(keyList);
                    includeProperties = StringUtil.toArray(keyList);
                }
                // Set the column names from the time series properties.
                if ( tableOutputColumnNames == null ) {
                	// Default output column names to input.
                    tableOutputColumnNames = includeProperties;
                }
                else {
                    // Table output columns were set to an array above.  Check for wildcards.
                	// TODO SAM 2016-02-17 What does this do?  Need dictionary to map similar to other commands.
                    for ( int icolumn = 0; icolumn < tableOutputColumnNames.length; icolumn++ ) {
                        if ( tableOutputColumnNames[icolumn].equals("*") ) {
                            // Output column name gets reset to the output name.
                            tableOutputColumnNames[icolumn] = includeProperties[icolumn];
                        }
                    }
                }

                // Make sure that the output table includes the columns to receive property values, including the TSID column.
                // TSID is always a string.
                try {
                    TableTSIDColumnNumber = table.getFieldIndex(TableTSIDColumn);
                }
                catch ( Exception e2 ) {
                    TableTSIDColumnNumber =
                        table.addField(new TableField(TableField.DATA_TYPE_STRING, TableTSIDColumn, -1, -1), null);
                    Message.printStatus(2, routine, "Did not match TableTSIDColumn \"" + TableTSIDColumn +
                        "\" as column table so added to table." );
                }
                for ( int icolumn = 0; icolumn < includeProperties.length; icolumn++ ) {
                	Message.printStatus(2,routine,"includeProperties["+icolumn+"]="+includeProperties[icolumn]);
                }
                for ( int icolumn = 0; icolumn < tableOutputColumnNames.length; icolumn++ ) {
                	Message.printStatus(2,routine,"tableOutputColumnNames["+icolumn+"]="+tableOutputColumnNames[icolumn]);
                }
                // Other output column types depend on the time series properties.
                for ( int i = 0; i < tableOutputColumnNames.length; i++ ) {
                    String tableOutputColumnName = tableOutputColumnNames[i];
                    try {
                        // Column names are allowed to use time series properties.
                        tableOutputColumnName = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                            processor, ts, tableOutputColumnName, status, commandPhase);
                        table.getFieldIndex(tableOutputColumnName);
                    }
                    catch ( Exception e2 ) {
                        message = "Table \"" + TableID + "\" does not have column \"" + tableOutputColumnName + "\" - creating.";
                        Message.printStatus(2,routine,message);
                        //Message.printWarning ( warning_level,
                        //MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        //status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                        //    message, "Verify that a table exists with the requested output column." ) );
                        // Skip the time series...
                        //continue;
                        //
                        // Create the column in the table.
                        // Do this before any attempt to match the record based on TSID below.
                        // For now don't set any width or precision on the column.
                        // First find the matching property in the time series to determine the property type.
                        // The order of IncludeProperties is the same as tableOutputColumnNames.
                        Object propertyValue = ts.getProperty(includeProperties[i] );
                        if ( propertyValue == null ) {
                            // If null just let the property be set by a later record where a non-null value is found.
                            // TODO SAM 2012-09-30 Is it possible to check the type even if null?
                            continue;
                        }
                        else if ( propertyValue instanceof String ) {
                            table.addField(new TableField(TableField.DATA_TYPE_STRING, tableOutputColumnName, -1, -1), null);
                        }
                        else if ( propertyValue instanceof Boolean ) {
                            table.addField(new TableField(TableField.DATA_TYPE_BOOLEAN, tableOutputColumnName, -1, -1), null);
                        }
                        else if ( propertyValue instanceof Integer ) {
                            table.addField(new TableField(TableField.DATA_TYPE_INT, tableOutputColumnName, -1, -1), null);
                        }
                        else if ( propertyValue instanceof Long ) {
                            table.addField(new TableField(TableField.DATA_TYPE_LONG, tableOutputColumnName, -1, -1), null);
                        }
                        else if ( propertyValue instanceof Short ) {
                            table.addField(new TableField(TableField.DATA_TYPE_SHORT, tableOutputColumnName, -1, -1), null);
                        }
                        else if ( propertyValue instanceof Double ) {
                            table.addField(new TableField(TableField.DATA_TYPE_DOUBLE, tableOutputColumnName,15, 6), null);
                        }
                        else if ( propertyValue instanceof Float ) {
                            table.addField(new TableField(TableField.DATA_TYPE_FLOAT, tableOutputColumnName,15, 6), null);
                        }
                        else if ( propertyValue instanceof Date ) {
                            table.addField(new TableField(TableField.DATA_TYPE_DATE, tableOutputColumnName, -1, -1), null);
                        }
                        else if ( propertyValue instanceof DateTime ) {
                            table.addField(new TableField(TableField.DATA_TYPE_DATETIME, tableOutputColumnName, -1, -1), null);
                        }
                        else {
                            message = "Time series property type for \"" + tableOutputColumnNames[i] +
                                "\" (" + propertyValue + ") is not handled - cannot add column to table.";
                            Message.printWarning ( warning_level,
                            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Contact software support." ) );
                            // Skip the time series.
                            continue;
                        }
                        Message.printStatus(2, routine, "Did not match property name \"" + tableOutputColumnNames[i] +
                            "\" as column table so added to table." );
                    }
                }

                // Get the table column numbers corresponding to the column names.

                // Get the columns from the table to be used as output.
                // TODO SAM 2014-06-09 Why is this done here and not above?
                int [] tableOutputColumns = new int[tableOutputColumnNames.length];
                String [] tableOutputColumnNamesExpanded = new String[tableOutputColumnNames.length];
                for ( int i = 0; i < tableOutputColumns.length; i++ ) {
                    tableOutputColumnNamesExpanded[i] = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, tableOutputColumnNames[i], status, commandPhase);
                    try {
                        tableOutputColumns[i] = table.getFieldIndex(tableOutputColumnNamesExpanded[i]);
                    }
                    catch ( Exception e2 ) {
                        // This should not happen since columns created above, but possible that a value had all nulls
                        // above and therefore column was not added because type was unknown.
                        // FIXME SAM 2012-09-30 Need to add column as string if all values were null?
                        //message = "Table \"" + TableID + "\" does not have column \"" + tableOutputColumnNames[i] + "\".";
                        //Message.printWarning ( warning_level,
                        //MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                        //status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                        //    message, "Verify that a table exists with the requested output column." ) );
                        // Skip the time series.
                        //continue;
                    }
                }

                // See if a matching row exists using the specified TSID column.
                String tsid = null;
                if ( (TableTSIDFormat != null) && !TableTSIDFormat.equals("") ) {
                    // Format the TSID using the specified format.
                    tsid = ts.formatLegend ( TableTSIDFormat );
                }
                else {
                    // Use the alias if available and then the TSID.
                    tsid = ts.getAlias();
                    if ( (tsid == null) || tsid.equals("") ) {
                        tsid = ts.getIdentifierString();
                    }
                }
                TableRecord rec = null;
                if ( !allowDuplicates ) {
                    // Try to match the TSID.
                    rec = table.getRecord ( TableTSIDColumn, tsid );
                }
                if ( rec == null ) {
                    //message = "Cannot find table \"" + TableID + "\" cell in column \"" + TableTSIDColumn +
                    //    "\" matching TSID formatted as \"" + tsid + "\" - skipping time series \"" +
                    //    ts.getIdentifierString() + "\".";
                    //Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    //    routine, message );
                    //status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                    //    "Verify that table \"" + TableID + "\" column TSID matches one or more time series." ) );
                    // Go to next time series.
                    //continue;

                    // Add a new record to the table that matches the formatted TSID.
                    int recNum = table.getTableRecords().size();
                    table.setFieldValue(recNum, TableTSIDColumnNumber, tsid, true);
                    // Get the new record for use below.
                    rec = table.getRecord(recNum);
                }
                else {
                    Message.printStatus(2, routine, "Matched table \"" + TableID + "\" row for TSID \"" + tsid );
                }

                // Loop through the property names.

                //for ( int icolumn = 0; icolumn < IncludeProperties.length; icolumn++ ) {
                //    String propertyName = IncludeProperties[icolumn];
                //    Object propertyValue = ts.getProperty(propertyName);
                for ( int icolumn = 0; icolumn < tableOutputColumnNames.length; icolumn++ ) {
                	// Get property name that matches the table output column.
                    String propertyName = includeProperties[icolumn]; // This should align with a corresponding output column.
                    Object propertyValue = ts.getProperty(propertyName);
                    // If the property value is null, just skip setting it - default value for columns is null.
                    // TODO SAM 2011-04-27 Should this be a warning?
                    if ( propertyValue == null ) {
                        Message.printStatus(2,routine,"Time series \"" + ts.getIdentifierString() + "\" property \"" +
                        	propertyName + "\" is null, not copying (column value will be null).");
                        continue;
                    }
                    // Get the matching table column.
                    try {
                        // Get the value from the table.
                        // Make sure that the table has the specified column.
                        int colNumber = tableOutputColumns[icolumn];
                        if ( colNumber < 0 ) {
                            // TODO SAM 2012-09-30 Should not happen?
                            message = "Table \"" + TableID +
                            "\" does not have column \"" + tableOutputColumnNamesExpanded[icolumn] + "\".";
                            Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                                routine, message );
                            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Verify that the proper table output column is specified and has been defined." ) );
                            continue;
                        }
                        // Set the value in the table.
                        try {
                            rec.setFieldValue(colNumber,propertyValue);
                            if ( Message.isDebugOn ) {
                                Message.printDebug(1, routine, "Setting table column [" + icolumn + "]" + tableOutputColumnNamesExpanded[icolumn] + "=\"" +
                                    propertyValue + "\"" );
                            }
                            Message.printStatus(2, routine, "Setting table column [" + icolumn + "] " + tableOutputColumnNamesExpanded[icolumn] + "=\"" +
                                 propertyValue + "\"" );
                            // TODO SAM 2011-04-27 Evaluate why the column width is necessary in the data table.
                            // Reset the column width if necessary.
                            if ( propertyValue instanceof String ) {
                                // If the incoming string is longer than the column width, reset the column width.
                                int width = table.getFieldWidth(tableOutputColumns[icolumn]);
                                if ( width > 0 ) {
                                    table.setFieldWidth(tableOutputColumns[icolumn],
                                        Math.max(width,((String)propertyValue).length()));
                                }
                            }
                        }
                        catch ( Exception e ) {
                            // Blank cell values are allowed - just don't set the property.
                            message = "Unable to set " + propertyName + "=" + propertyValue + " in table \"" + TableID +
                                "\" column \"" + tableOutputColumnNamesExpanded[icolumn] +
                                "\" matching TSID \"" + tsid + " (" + ts.getIdentifier().toStringAliasAndTSID() + "\") (" + e + ").";
                            Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                                routine, message );
                            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING, message,
                                "Verify that the proper table output column is specified and has been defined." ) );
                        }
                    }
                    catch ( Exception e ) {
                        message = "Unexpected error processing time series \""+ ts.getIdentifier() + " (" + e + ").";
                        Message.printWarning ( warning_level,
                            MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                        Message.printWarning(3,routine,e);
                        status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "See the log file for details - report the problem to software support." ) );
                    }
                }
            }
            Message.printStatus(2, routine, "Table \"" + TableID +
                "\" after copying properties has " + table.getNumberOfRecords() + " records." );
        }
        catch ( Exception e ) {
            message = "Unexpected error processing time series (" + e + ").";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            Message.printWarning ( 3, routine, e );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Check log file for details." ) );
            throw new CommandException ( message );
        }
    }

    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
@param table the DataTable to receive output
*/
private void setDiscoveryTable ( DataTable table ) {
    __table = table;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TSList",
    	"TSID",
    	"EnsembleID",
    	"IncludeProperties",
    	"TableID",
    	"TableTSIDColumn",
    	"TableTSIDFormat",
    	"AllowDuplicates",
    	"TableOutputColumns"
	};
	return this.toString(parameters, parameterOrder);
}

}