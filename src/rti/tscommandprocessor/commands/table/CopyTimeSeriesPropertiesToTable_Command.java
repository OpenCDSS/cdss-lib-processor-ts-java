// CopyTimeSeriesPropertiesToTable_Command - This class initializes, checks, and runs the CopyTimeSeriesPropertiesToTable() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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
import java.util.Hashtable;
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
    //String IncludeProperties = parameters.getValue ( "IncludeProperties" );
    String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String AllowDuplicates = parameters.getValue ( "AllowDuplicates" );
    //String NameMap = parameters.getValue ( "NameMap" );
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

    /*
    if ( (IncludeProperties != null) && !IncludeProperties.isEmpty() &&
        (TableOutputColumns != null) && !TableOutputColumns.isEmpty() ) {
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
    */

    if ( (TableOutputColumns != null) && !TableOutputColumns.isEmpty() ) {
        message = "The TableOutputColumns parameter is not supported as of TSTool 14.10.0.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify only the NameMap parameter (TableOutputColumns is being phased out)." ) );
    }

    if ( (AllowDuplicates != null) && !AllowDuplicates.equals("") && !AllowDuplicates.equalsIgnoreCase(_False) &&
        !AllowDuplicates.equalsIgnoreCase(_True) ) {
        message = "The AllowDuplicates value (" + AllowDuplicates + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Use the NameMap parameter." ) );
    }

    // Check for invalid parameters.
    List<String> validList = new ArrayList<>(10);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "IncludeProperties" );
    validList.add ( "ExcludeProperties" );
    validList.add ( "IncludeBuiltInProperties" );
    validList.add ( "ExcludeBuiltInProperties" );
    validList.add ( "TableID" );
    validList.add ( "TableTSIDColumn" );
    validList.add ( "TableTSIDFormat" );
    validList.add ( "AllowDuplicates" );
    validList.add ( "NameMap" );
    //validList.add ( "TableOutputColumns" );
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
    String message, routine = getClass().getSimpleName() + ".runCommand";
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    int log_level = 3;
    //int log_level = 3;  // Level for non-use messages for log file.

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = Boolean.TRUE; // Default.
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
    if ( (TSList == null) || TSList.isEmpty() ) {
        TSList = TSListType.ALL_TS.toString(); // Default.
    }
    String TSID = parameters.getValue ( "TSID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String IncludeProperties = parameters.getValue ( "IncludeProperties" );
    if ( commandPhase == CommandPhaseType.RUN ) {
		IncludeProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, IncludeProperties);
	}
    // These may contain * wildcards.
    String [] includePropertiesReq = null;
    if ( (IncludeProperties != null) && !IncludeProperties.isEmpty() ) {
        includePropertiesReq = IncludeProperties.trim().split(",");
        // Remove surrounding whitespace.
        for ( int i = 0; i < includePropertiesReq.length; i++ ) {
        	includePropertiesReq[i] = includePropertiesReq[i].trim();
        }
    }
    // These may contain * wildcards.
    String ExcludeProperties = parameters.getValue ( "ExcludeProperties" );
    if ( commandPhase == CommandPhaseType.RUN ) {
		ExcludeProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, ExcludeProperties);
	}
    String [] excludePropertiesReq = null;
    if ( (ExcludeProperties != null) && !ExcludeProperties.equals("") ) {
        excludePropertiesReq = ExcludeProperties.trim().split(",");
        // Remove surrounding whitespace.
        for ( int i = 0; i < excludePropertiesReq.length; i++ ) {
        	excludePropertiesReq[i] = excludePropertiesReq[i].trim();
        }
    }
    // These may contain * wildcards.
    String IncludeBuiltInProperties = parameters.getValue ( "IncludeBuiltInProperties" );
    boolean doBuiltInProperties = false;
    if ( commandPhase == CommandPhaseType.RUN ) {
		IncludeBuiltInProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, IncludeBuiltInProperties);
	}
    String [] includeBuiltInPropertiesReq = null;
    if ( (IncludeBuiltInProperties != null) && !IncludeBuiltInProperties.equals("") ) {
    	doBuiltInProperties = true;
        includeBuiltInPropertiesReq = IncludeBuiltInProperties.trim().split(",");
        // Remove surrounding whitespace.
        for ( int i = 0; i < includeBuiltInPropertiesReq.length; i++ ) {
        	includeBuiltInPropertiesReq[i] = includeBuiltInPropertiesReq[i].trim();
        }
    }
    // These may contain * wildcards.
    String ExcludeBuiltInProperties = parameters.getValue ( "ExcludeBuiltInProperties" );
    if ( commandPhase == CommandPhaseType.RUN ) {
		ExcludeBuiltInProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, ExcludeBuiltInProperties);
	}
    String [] excludeBuiltInPropertiesReq = null;
    if ( (ExcludeBuiltInProperties != null) && !ExcludeBuiltInProperties.equals("") ) {
        excludeBuiltInPropertiesReq = ExcludeBuiltInProperties.trim().split(",");
        // Remove surrounding whitespace.
        for ( int i = 0; i < includeBuiltInPropertiesReq.length; i++ ) {
        	excludeBuiltInPropertiesReq[i] = excludeBuiltInPropertiesReq[i].trim();
        }
    }
    String TableID = parameters.getValue ( "TableID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	// In discovery mode want lists of tables to include ${Property} so only expand if in run mode.
   		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    if ( commandPhase == CommandPhaseType.RUN ) {
		TableTSIDColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableTSIDColumn);
	}
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    String AllowDuplicates = parameters.getValue ( "AllowDuplicates" );
    boolean allowDuplicates = false; // Default
    if ( (AllowDuplicates != null) && AllowDuplicates.equalsIgnoreCase(_True) ) {
        allowDuplicates = true;
    }
    String NameMap = parameters.getValue ( "NameMap" );
    if ( commandPhase == CommandPhaseType.RUN ) {
		NameMap = TSCommandProcessorUtil.expandParameterValue(processor, this, NameMap);
	}
    Hashtable<String,String> nameMap = new Hashtable<>();
    if ( (NameMap != null) && !NameMap.isEmpty() && (NameMap.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(NameMap, ",", 0 );
        // Now break pairs and put in hashtable.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            nameMap.put(parts[0].trim(), parts[1].trim() );
        }
    }
    /*
    String TableOutputColumns = parameters.getValue ( "TableOutputColumns" );
    String [] tableOutputColumnNames0 = null;
    if ( (TableOutputColumns != null) && !TableOutputColumns.equals("") ) {
        tableOutputColumnNames0 = TableOutputColumns.split(",");
        // These are expanded below based on dynamic time series properties.
    }
    */

    // Get the table to process.

    DataTable table = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
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
    	CommandProcessorRequestResultsBean bean = null;
        PropList request_params = new PropList ( "" );
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
    	// OK to set the table ID no matter what.
        //if ( table == null ) {
            // Did not find table so is being created in this command.
            // Create an empty table and set the ID, which is used for discovery.
            table = new DataTable();
            table.setTableID ( TableID );
            setDiscoveryTable ( table );
        //}
        //else {
        	// The table was created in a previous command so don't need to add for discovery here.
        //}
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        if ( table == null ) {
            // Did not find the table above so create it.
            table = new DataTable( /*columnList*/ );
            table.setTableID ( TableID );
            Message.printStatus(2, routine, "Was not able to match existing table \"" + TableID + "\" so created new table.");

            // Set the table in the processor.

            PropList request_params = new PropList ( "" );
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
        
        // Set the build-in properties to copy:
        // - built-in property names are the same for all time series
        // - can initialize here and reuse below
        String [] includeBuiltInProperties = null;
        if ( doBuiltInProperties ) {
            // Get all the built-in properties:
            // - these are the known built-in properties from TS.getProperty()
            // - see TSUtil.getTSFormatSpecifiers ()
            String [] includeBuiltInProperties0 = new String[8];
            includeBuiltInProperties0[0] = "alias";
            includeBuiltInProperties0[1] = "datatype";
            includeBuiltInProperties0[2] = "description";
            includeBuiltInProperties0[3] = "interval";
            includeBuiltInProperties0[4] = "periodstart";
            includeBuiltInProperties0[5] = "periodend";
            includeBuiltInProperties0[6] = "tsid";
            includeBuiltInProperties0[7] = "units";
            if ( IncludeBuiltInProperties.equals("*") && ((ExcludeBuiltInProperties == null) || ExcludeBuiltInProperties.isEmpty()) ) {
            	// Simplest case of include all built-in properties.
            	includeBuiltInProperties = new String[includeBuiltInProperties0.length];
            	for ( int i = 0; i < includeBuiltInProperties0.length; i++ ) {
            		includeBuiltInProperties[i] = includeBuiltInProperties0[i];
            	}
            }
            else {
            	// Want to include one or more built-in properties:
            	// - create Java regular expression patterns for the includes and excludes
            	boolean includeFirst = true;
            	boolean ignoreCase = false;
            	boolean checkRegex = true;
            	// Check to see if any of the include or exclude have regular expressions:
            	// - if any do, then code below needs to check regular expressions
            	// - if there are no regular expressions, simple comparisons can be done, which is faster
            	includeBuiltInProperties = StringUtil.toArray(StringUtil.includeExcludeStrings (
            		StringUtil.toList(includeBuiltInProperties0),
            		StringUtil.toRegExList(StringUtil.toList(includeBuiltInPropertiesReq)),
            		StringUtil.toRegExList(StringUtil.toList(excludeBuiltInPropertiesReq)),
            		includeFirst, ignoreCase, checkRegex) );
           	}
            for ( String includeBuiltInProperty: includeBuiltInProperties ) {
            	Message.printStatus(2, routine, "Built-in property for all time series: \"" + includeBuiltInProperty + "\".");
            }
        }

        // Loop through the time series twice, once for dynamic properties and once for built-in properties:
        // - iout=0 is for dynamic properties
        // - iout=1 is for built-in properties
        // - in the following code the column names and numbers are specific to whether built-in or dynamic properties
        // - the end result is that column names from both dynamic and built-in properties will be added

        for ( int iout = 0; iout < 2; iout++ ) {
        	// Array of include properties to copy from the time series:
        	// - dynamic properties will vary by time series
        	// - built-in properties will be constant for all time series
        	String [] includeProperties = null;
        	try {
            	TS ts = null;
            	Object o_ts = null;
            	int TableTSIDColumnNumber = -1;
            	String [] tableOutputColumnNames = null;
            
            	// Set the 'includeProperties':
            	// - indicates the the dynamic or built-in properties to copy
            	// - dynamic is handled in the first 'iout' loop and the list may differ by time series
            	// - built-in resets 'includeProperties' but should be OK since built-in properties are the same for all time series
            	if ( iout == 0 ) {
            		// User-defined properties:
            		// - the 'includeProperties' and 'tableOutputColumnNames' are determined below in iout=0 code
            		// - properties may vary by time series
            		Message.printStatus(2, routine, "Copying user-defined (dynamic) properties to table \"" + TableID + "\" for " + nts + " time series.");
            	}
            	else {
            		// Built-in properties:
            		// - set the 'tableOutputColumnNames' here based on built-in properties determined above
            		// - the properties are the same for all time series
            		if ( doBuiltInProperties ) {
            			// Have built-in properties to copy.
            			Message.printStatus(2, routine, "Copying built-in properties to table \"" + TableID + "\" for " + nts + " time series.");

               			// Set the include properties for generic handling.
               			includeProperties = includeBuiltInProperties;

            			// Default output column names to the same as the included properties:
           				// - need to copy because may change values below
            			tableOutputColumnNames = new String[includeBuiltInProperties.length];
              			for ( int icolumn = 0; icolumn < tableOutputColumnNames.length; icolumn++ ) {
               				tableOutputColumnNames[icolumn] = includeBuiltInProperties[icolumn];
               			}

               			// If the NameMap was specified, reset the output column names using the map.

               			if ( nameMap != null ) {
               				for ( int iprop = 0; iprop < includeBuiltInProperties.length; iprop++ ) {
               					String columnName = nameMap.get(includeBuiltInProperties[iprop]);
               					if ( columnName != null ) {
               						// A mapped name has been specified:
               						// - a bad map property name will be ignored since never requested
               						tableOutputColumnNames[iprop] = columnName;
               					}
               				}
               			}
            		}
            		else {
            			// No need to process built-in properties:
            			// - skip major loop to avoid unnecessary checks and messages
            			continue;
            		}
            	}

            	// Loop through the time series:
            	// - dynamic properties are added first (iout=0)
            	// - built-in properties are added second (iout=1)

            	for ( int its = 0; its < nts; its++ ) {
                	// The time series to process, from the list that was returned above.
                	message = "Copying properties for time series " + (its + 1) + " of " + nts;
                	notifyCommandProgressListeners ( its, nts, (float)-1.0, message );
                	// Reset the tableColumnNames for each time series:
                	// - this code is no longer used
                	if ( iout == 0 ) {
                		/*
                		if ( tableOutputColumnNames0 != null ) {
                			tableOutputColumnNames = new String[tableOutputColumnNames0.length];
                			for ( int i = 0; i < tableOutputColumnNames0.length; i++ ) {
                				tableOutputColumnNames[i] = tableOutputColumnNames0[i];
                			}
                		}
                		*/
                	}
                	else {
                		// Built-in property names are constant across time series and were set at the top.
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
                	
                	// Else, have a time series to process.
                	ts = (TS)o_ts;

                	// Get the property names to process:
                	// - the values are not determined until later because
                	//   need to also make sure the table columns are defined first

                	if ( iout == 0 ) {
                		// Dynamic properties:
                		// - this must be done for each time series because the properties may be different between time series
           	   			String [] includePropertiesReq2 = null;
           	   			String [] excludePropertiesReq2 = null;
                		if ( (IncludeProperties == null) || IncludeProperties.isEmpty() || IncludeProperties.equals("*") ) {
                    		// Get all the dynamic (not built-in) properties by forming a list of property names from the hashtable.
                    		HashMap<String, Object> propertyHash = ts.getProperties();
                    		ArrayList<String> propertyKeyList = new ArrayList<>(propertyHash.keySet());
                    		// Don't sort because original order has meaning.
                    		//Collections.sort(keyList);
                    		includePropertiesReq2 = StringUtil.toArray(propertyKeyList);
                		}
                		else {
                			// Want to include one or more dynamic properties:
            	   			// - create Java regular expression patterns for the includes and excludes
            	   			if ( includePropertiesReq != null ) {
            	   				includePropertiesReq2 = new String[includePropertiesReq.length];
            	   				for ( int i = 0; i < includePropertiesReq.length; i++ ) {
            	   					String includePropertyReq = includePropertiesReq[i];
            	   					includePropertiesReq2[i] = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
            	   						processor, ts, includePropertyReq, status, commandPhase);
            	   				}
            	   			}
                		}

           	   			if ( excludePropertiesReq != null ) {
           	   				excludePropertiesReq2 = new String[excludePropertiesReq.length];
           	   				for ( int i = 0; i < excludePropertiesReq.length; i++ ) {
           	   					String excludePropertyReq = excludePropertiesReq[i];
           	   					excludePropertiesReq2[i] = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
           	   						processor, ts, excludePropertyReq, status, commandPhase);
           	   				}
           	   			}

                		// Form the final list of properties to include:
           	   			// - consider the include and exclude lists
           	   			boolean includeFirst = true;
           	   			boolean ignoreCase = false;
           	   			boolean checkRegex = true;
                   		HashMap<String, Object> propertyHash = ts.getProperties();
                   		ArrayList<String> propertyKeyList = new ArrayList<>(propertyHash.keySet());
           	   			includeProperties = StringUtil.toArray(StringUtil.includeExcludeStrings (
           		   			propertyKeyList,
           		   			StringUtil.toRegExList(StringUtil.toList(includePropertiesReq2)),
           		   			StringUtil.toRegExList(StringUtil.toList(excludePropertiesReq2)),
           		   			includeFirst, ignoreCase, checkRegex) );

                		// Set the output column names from the time series properties:
                		// - do this before remapping the names below
                		//if ( tableOutputColumnNames == null ) {
                			// Default output column names to input:
                			// - need to copy because may change values below
                    		tableOutputColumnNames = new String[includeProperties.length];
                    		for ( int icolumn = 0; icolumn < tableOutputColumnNames.length; icolumn++ ) {
                    			tableOutputColumnNames[icolumn] = includeProperties[icolumn];
                    		}
                    		/*
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
                		*/
                	}
                	else {
                		// Built-in properties:
                		// - the 'includeParameters' array and 'tableOutputColumnNames' arrays were set before the time series loop
                	}

                	// If the NameMap was specified, reset the output column names:
                	// - this is what is created in the output table
                	// - do this after setting the column names to defaults that match the input properties

                	if ( nameMap != null ) {
                		for ( int iprop = 0; iprop < includeProperties.length; iprop++ ) {
                			String columnName = nameMap.get(includeProperties[iprop]);
                			if ( columnName != null ) {
                				// A mapped name has been specified:
               					// - a bad map property name will be ignored since never requested
                				tableOutputColumnNames[iprop] = columnName;
                			}
                			else {
                				// Did not find a property to rename:
                				// - ignore this because not all time series may have the same properties
                			}
                		}
                	}
                	
                	// Make sure that the output table includes the columns to receive property values:
                	// - TSID is always handled separately from the property columns and is a string data type

                	try {
                    	TableTSIDColumnNumber = table.getFieldIndex(TableTSIDColumn);
                	}
                	catch ( Exception e2 ) {
                    	TableTSIDColumnNumber =
                        	table.addField(new TableField(TableField.DATA_TYPE_STRING, TableTSIDColumn, -1, -1), null);
                    	Message.printStatus(2, routine, "Did not match TableTSIDColumn \"" + TableTSIDColumn +
                        	"\" as column table so added to table." );
                	}

                	// Print output for troubleshooting.
                	for ( int icolumn = 0; icolumn < includeProperties.length; icolumn++ ) {
                		Message.printStatus ( 2, routine, "iout=" + iout + " includeProperties[" + icolumn + "]=" + includeProperties[icolumn] );
                	}
                	for ( int icolumn = 0; icolumn < tableOutputColumnNames.length; icolumn++ ) {
                		Message.printStatus ( 2, routine, "iout=" + iout + " tableOutputColumnNames[" + icolumn + "]=" + tableOutputColumnNames[icolumn] );
                	}

                	// Other output column types (other than TSID) depend on the time series properties.
                	List<String> nullColumnNames = new ArrayList<>();
                	for ( int i = 0; i < tableOutputColumnNames.length; i++ ) {
                    	String tableOutputColumnName = tableOutputColumnNames[i];
                    	try {
                    		// TODO smalers 2025-01-12 this is handled above.
                        	// Column names are allowed to use time series properties.
                        	//tableOutputColumnName = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                            //	processor, ts, tableOutputColumnName, status, commandPhase);
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
                        		if ( Message.isDebugOn ) {
                        			message = "Time series property type for \"" + tableOutputColumnNames[i] +
                                    	"\" could not be determined since a null value.  Will add table column later when non-null value is found";
                        			Message.printDebug(3, routine, message);
                        		}
                            	for ( String nullColumnName : nullColumnNames ) {
                            		boolean found = false;
                            		if ( nullColumnName.equals(tableOutputColumnNames[i]) ) {
                            			found = true;
                            		}
                            		if ( !found ) {
                            			nullColumnNames.add(tableOutputColumnNames[i]);
                            		}
                            	}
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
                                	"\" (" + propertyValue + ") is not handled - cannot add column to the table.";
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

                	// Add column for properties that had null data:
                	// - null does not allow the data type of the property to be determined
                	// - only add if not already added
                	// - use String type for the column, which will have all nulls
                	for ( String nullColumnName : nullColumnNames ) {
                    	try {
                        	table.getFieldIndex(nullColumnName);
                    	}
                    	catch ( Exception e2 ) {
                    		// Did not have the column so add it.
                        	table.addField(new TableField(TableField.DATA_TYPE_STRING, nullColumnName, -1, -1), null);
                    	}
                	}

                	// Get the table column numbers corresponding to the column names.

                	// Get the columns from the table to be used as output.
                	// TODO SAM 2014-06-09 Why is this done here and not above?
                	int [] tableOutputColumnNumbers = new int[tableOutputColumnNames.length];
                	//String [] tableOutputColumnNamesExpanded = new String[tableOutputColumnNames.length];
                	for ( int i = 0; i < tableOutputColumnNumbers.length; i++ ) {
                		// TODO smalers 2025-01-08 the full property was expanded above so no need to do here.
                    	//tableOutputColumnNamesExpanded[i] = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        //	processor, ts, tableOutputColumnNames[i], status, commandPhase);
                    	try {
                        	//tableOutputColumns[i] = table.getFieldIndex(tableOutputColumnNamesExpanded[i]);
                        	tableOutputColumnNumbers[i] = table.getFieldIndex(tableOutputColumnNames[i]);
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

                	// At this point the following arrays align:
                	//   includeProperties[i] <=> tableOutputColumnNames[i] <=> tableOutputColumns[i]

                	// See if a matching table row exists using the specified TSID column:
                	// - if so will reset existing data or define new data in the table
                	String tsid = null;
                	if ( (TableTSIDFormat != null) && !TableTSIDFormat.equals("") ) {
                    	// Format the TSID using the specified format.
                    	tsid = ts.formatLegend ( TableTSIDFormat );
                	}
                	else {
                		// No TSID format was specified:
                    	// - default is to se the alias if available and then the TSID
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

                	// Loop through the property names:
                	// - the property names will be user-defined if iout=0
                	// - the property names will be built-in if iout=1

                	for ( int icolumn = 0; icolumn < includeProperties.length; icolumn++ ) { // }
                	//    String propertyName = IncludeProperties[icolumn];
                	//    Object propertyValue = ts.getProperty(propertyName);
                	//for ( int icolumn = 0; icolumn < tableOutputColumnNames.length; icolumn++ ) {
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
                        	// Get the table column number to set:
                        	// - make sure that the table has the specified column, but should be OK if above logic is OK
                        	int colNumber = tableOutputColumnNumbers[icolumn];
                        	if ( colNumber < 0 ) {
                            	// TODO SAM 2012-09-30 Should not happen?
                            	message = "Table \"" + TableID +
                            	//"\" does not have column \"" + tableOutputColumnNamesExpanded[icolumn] + "\".";
                            	"\" does not have column \"" + tableOutputColumnNames[icolumn] + "\".";
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
                                	//Message.printDebug(1, routine, "iout=" + iout + " Setting table column [" + icolumn + "]" + tableOutputColumnNamesExpanded[icolumn] + "=\"" +
                                	Message.printDebug(1, routine, "iout=" + iout + " Setting table column [" + icolumn + "]" + tableOutputColumnNames[icolumn] + "=\"" +
                                    	propertyValue + "\"" );
                            	}
                            	//Message.printStatus(2, routine, "iout=" + iout + " Setting table column [" + icolumn + "] " + tableOutputColumnNamesExpanded[icolumn] + "=\"" +
                            	//Message.printStatus(2, routine, "iout=" + iout + " Setting table column [" + icolumn + "] " + tableOutputColumnNames[icolumn] + "=\"" +
                                 	//propertyValue + "\"" );
                            	// TODO SAM 2011-04-27 Evaluate why the column width is necessary in the data table.
                            	// Reset the column width if necessary.
                            	if ( propertyValue instanceof String ) {
                                	// If the incoming string is longer than the column width, reset the column width.
                                	int width = table.getFieldWidth(tableOutputColumnNumbers[icolumn]);
                                	if ( width > 0 ) {
                                    	table.setFieldWidth(tableOutputColumnNumbers[icolumn],
                                        	Math.max(width,((String)propertyValue).length()));
                                	}
                            	}
                        	}
                        	catch ( Exception e ) {
                            	// Blank cell values are allowed - just don't set the property.
                            	message = "Unable to set " + propertyName + "=" + propertyValue + " in table \"" + TableID +
                                	//"\" column \"" + tableOutputColumnNamesExpanded[icolumn] +
                                	"\" column \"" + tableOutputColumnNames[icolumn] +
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
        		if ( iout == 0 ) {
        			message = "Unexpected error processing time series for dynamic properties (" + e + ").";
        		}
        		else {
        			message = "Unexpected error processing time series for built-in properties (" + e + ").";
        		}
            	Message.printWarning ( warning_level,
                	MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            	Message.printWarning ( 3, routine, e );
            	status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                	message, "Check log file for details." ) );
            	throw new CommandException ( message );
        	}
        } // End of 'iout' loop.
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
    	"ExcludeProperties",
    	"IncludeBuiltInProperties",
    	"ExcludeBuiltInProperties",
    	"TableID",
    	"TableTSIDColumn",
    	"TableTSIDFormat",
    	"AllowDuplicates",
    	"NameMap"
    	//"TableOutputColumns"
	};
	return this.toString(parameters, parameterOrder);
}

}