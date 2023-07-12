// SetTimeSeriesPropertiesFromTable_Command - This class initializes, checks, and runs the SetTimeSeriesPropertiesFromTable() command.

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

package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import RTi.TS.TS;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the SetTimeSeriesPropertiesFromTable() command.
*/
public class SetTimeSeriesPropertiesFromTable_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public SetTimeSeriesPropertiesFromTable_Command () {
    super();
    setCommandName ( "SetTimeSeriesPropertiesFromTable" );
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
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableInputColumns = parameters.getValue ( "TableInputColumns" );
    String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (TableID == null) || TableID.equals("") ) {
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

    if ( (TableInputColumns == null) || TableInputColumns.equals("") ) {
        message = "The table input column(s) must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide one or more table column names for properties." ) );
    }

    // Check for invalid parameters.
    List<String> validList = new ArrayList<>();
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "TableID" );
    validList.add ( "TableTSIDColumn" );
    validList.add ( "TableTSIDFormat" );
    validList.add ( "TableInputColumns" );
    validList.add ( "TSPropertyNames" );
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
    return (new SetTimeSeriesPropertiesFromTable_JDialog ( parent, this, tableIDChoices )).ok();
}

// Parse command is in the base class.

/**
Method to execute the command.
@param command_number Number of command in sequence.
@exception Exception if there is an error processing the command.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    String message, routine = getCommandName() + "_Command.runCommand";
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    int log_level = 3;
    //int log_level = 3;  // Level for non-use messages for log file.

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    PropList parameters = getCommandParameters();

    // Get the input parameters.

    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
    String TSID = parameters.getValue ( "TSID" );
	TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
    String TableID = parameters.getValue ( "TableID" );
	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    String TableInputColumns = parameters.getValue ( "TableInputColumns" );
	TableInputColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, TableInputColumns);
    String [] tableInputColumns = new String[0];
    if ( (TableInputColumns != null) && (TableInputColumns.length() > 0) ) {
        tableInputColumns = TableInputColumns.split(",");
    }
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
	TableTSIDColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableTSIDColumn);
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String TSPropertyNames = parameters.getValue ( "TSPropertyNames" );
	TSPropertyNames = TSCommandProcessorUtil.expandParameterValue(processor, this, TSPropertyNames);
    Hashtable<String,String> tsPropertyNamesMap = new Hashtable<>();
    if ( (TSPropertyNames != null) && (TSPropertyNames.length() > 0) ) {
        if ( TSPropertyNames.indexOf(":") > 0 ) {
            // Newer style property that is a dictionary.
            // First break map pairs by comma.
            List<String>pairs = StringUtil.breakStringList(TSPropertyNames, ",", 0 );
            // Have to handle manually because right side can be ${TS:Description}, for example.
            for ( String pair : pairs ) {
                int colonPos = pair.indexOf(":");
                if ( colonPos < 0 ) {
                    // Should not happen - invalid input.
                    tsPropertyNamesMap.put(pair, "" );
                }
                else {
                    String key = pair.substring(0,colonPos).trim();
                    if ( colonPos == (pair.length() - 1) ) {
                        // Colon is at the end of the string.
                        tsPropertyNamesMap.put(key, "" );
                    }
                    else {
                        tsPropertyNamesMap.put(key, pair.substring(colonPos + 1) );
                    }
                }
            }
        }
        else {
            // Older syntax that expects property name to be matched with table column name.
            // Don't error check that array lengths are the same because older TSTool should have enforced this.
            String [] tsPropertyNames = TSPropertyNames.split(",");
            for ( int i = 0; i < tsPropertyNames.length; i++ ) {
                tsPropertyNamesMap.put(tableInputColumns[i],tsPropertyNames[i]);
            }
        }
    }

    // Get the table to process.

    DataTable table = null;
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
        }
    }

    // Get the column from the table to be used as input.

    int [] tableInputColumnNums = new int[tableInputColumns.length];
    for ( int i = 0; i < tableInputColumns.length; i++ ) {
        try {
            tableInputColumnNums[i] = table.getFieldIndex(tableInputColumns[i]);
        }
        catch ( Exception e2 ) {
            message = "Table \"" + TableID + "\" does not have column \"" + tableInputColumns[i] + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a table exists with the requested input column." ) );
        }
    }

    // Get the time series to process.  Allow TSID to be a pattern or specific time series.

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
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report the problem to software support." ) );
    }
    if ( bean == null ) {
        Message.printStatus ( 2, routine, "Bean is null.");
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
    List<TS> tslist = null;
    if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
        Message.printWarning ( log_level, MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
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
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
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
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    }

    if ( warning_count > 0 ) {
        // Input error.
        message = "Insufficient data to run command.";
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

    // Now process.

    try {
        TS ts = null;
        Object o_ts = null;
        Object tableObject; // The table value as a generic object.
        // Get the table column number for the TSID.
        int tableTSIDColumnNum = -1;
        try {
            tableTSIDColumnNum = table.getFieldIndex(TableTSIDColumn);
        }
        catch ( Exception e ) {
            message = "Table column for TSID \"" + TableTSIDColumn + "\" is not found in table \"" + TableID +
                "\".  Cannot match time series.";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid TSID column." ) );
        }
        if ( tableTSIDColumnNum >= 0 ) {
            for ( int its = 0; its < nts; its++ ) {
                // The the time series to process, from the list that was returned above.
                o_ts = tslist.get(its);
                if ( o_ts == null ) {
                    message = "Time series to process is null.";
                    Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                    status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
                    // Go to next time series.
                    continue;
                }
                ts = (TS)o_ts;

                for ( int icolumn = 0; icolumn < tableInputColumns.length; icolumn++ ) {
                    try {
                        // Get the value from the table.
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
                        TableRecord rec = table.getRecord ( tableTSIDColumnNum, tsid );
                        if ( rec == null ) {
                            message = "Cannot find table \"" + TableID + "\" cell in column \"" + TableTSIDColumn +
                                "\" matching TSID formatted as \"" + tsid + "\" - skipping time series \"" +
                                ts.getIdentifierString() + "\".";
                            Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                                routine, message );
                            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
                                "Verify that table \"" + TableID + "\" column TSID matches one or more time series." ) );
                            // Go to next time series.
                            continue;
                        }
                        // Get the value from the table.
                        tableObject = rec.getFieldValue(tableInputColumnNums[icolumn]);
                        // Allow the value to be any number.
                        if ( tableObject == null ) {
                            // Blank cell values are allowed - just don't set the property.
                            message = "Table \"" + TableID + "\" value in column \"" + tableInputColumns[icolumn] +
                            "\" matching TSID \"" + tsid + "\" is null - skipping time series \"" +
                            ts.getIdentifierString() + "\".";
                            Message.printWarning(warning_level, MessageUtil.formatMessageTag( command_tag, ++warning_count),
                                routine, message );
                            // Don't add to command log because warnings will result.
                            //status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE, message,
                            //    "Verify that the proper table input column is specified and that column values are numbers." ) );
                            // Go to next time series.
                            continue;
                        }
                        else {
                            // Treat all time series properties as strings.
                            // TODO SAM 2013-09-10 Why treat all as strings?
                            String tableObjectAsString = "" + tableObject;
                            tableObjectAsString = tableObjectAsString.trim();
                            String tsPropertyName = tsPropertyNamesMap.get(tableInputColumns[icolumn]);
                            if ( tsPropertyName == null ) {
                                // Use the table column name for the property.
                                ts.setProperty(tableInputColumns[icolumn], tableObject );
                                ts.addToGenesis( "Set table property from table \"" + TableID + "\", column \"" +
                                    tableInputColumns[icolumn] + "\", \"" +
                                    tableInputColumns[icolumn] + "\" = " + tableObject );
                            }
                            else {
                                // Else, use the specified property name for the time series property
                                // If the property name matches a special name, set specifically
                                if ( tsPropertyName.equalsIgnoreCase("${TS:Description}") ) {
                                    ts.setDescription(tableObjectAsString);
                                    ts.addToGenesis( "Set table description from table \"" + TableID + "\", column \"" +
                                        tsPropertyName + "\", description = " + tableObjectAsString );
                                }
                                else {
                                    ts.setProperty(tsPropertyName, tableObject );
                                    ts.addToGenesis( "Set table property from table \"" + TableID + "\", column \"" +
                                        tsPropertyName + "\", \"" + tsPropertyName + "\" = " + tableObject );
                                }
                            }
                        }
                    }
                    catch ( Exception e ) {
                        message = "Unexpected error processing time series \""+ ts.getIdentifier() + " (" + e + ").";
                        Message.printWarning ( warning_level,
                            MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                        Message.printWarning(3,routine,e);
                        status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "See the log file for details - report the problem to software support." ) );
                    }
                }
            }
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error processing time series (" + e + ").";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check log file for details." ) );
        throw new CommandException ( message );
    }

    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
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
    	"TableID",
    	"TableTSIDColumn",
    	"TableTSIDFormat",
    	"TableInputColumns",
    	"TSPropertyNames"
	};
	return this.toString(parameters, parameterOrder);
}

}