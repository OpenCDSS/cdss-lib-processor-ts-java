// ProfileCommands_Command - This class initializes, checks, and runs the ProfileCommands() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.util;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProfile;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusProvider;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ProfileCommands() command.
*/
public class ProfileCommands_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
The detail table that is created.
*/
private DataTable __detailTable = null;

/**
The summary table that is created.
*/
private DataTable __summaryTable = null;

/**
Constructor.
*/
public ProfileCommands_Command ()
{	super();
	setCommandName ( "ProfileCommands" );
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
{	String warning = "";
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// Check for invalid parameters...
	List<String> validParameterNames = new ArrayList<String>();
    validParameterNames.add ( "SummaryTableID" );
    validParameterNames.add ( "DetailTableID" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validParameterNames, this, warning );    

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
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
{	// The command will be modified if changed...
	return (new ProfileCommands_JDialog ( parent, this )).ok();
}

/**
Return the detail table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryDetailTable()
{
    return __detailTable;
}

/**
Return the summary table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoverySummaryTable()
{
    return __summaryTable;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable summaryTable = getDiscoverySummaryTable();
    DataTable detailTable = getDiscoveryDetailTable();
    List<Object> v = new ArrayList<Object>();
    if ( (summaryTable != null) && (c == summaryTable.getClass()) ) {
        v.add ( summaryTable );
    }
    if ( (detailTable != null) && (c == detailTable.getClass()) ) {
        v.add ( detailTable );
    }
    return v;
}

// Use base class parseCommand()

/**
Fill the detail table with command statistics.
@param table the table to fill
@param commandList list of commands to process
*/
private DataTable profileCommandsDetail ( String detailTableID, List<Command> commandList )
throws Exception
{   // Create the summary table...
    List<TableField> columnList = new ArrayList<TableField>();
    columnList.add ( new TableField(TableField.DATA_TYPE_INT, "CommandNum", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Command", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "StartTime", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_INT, "StartTime (ms)", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "EndTime", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "EndTime (ms)", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "RunTime (ms)", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_DOUBLE, "RunTime (%)", -1, 3) );
    columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "StartHeap (bytes)", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "EndHeap (bytes)", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "DeltaHeap (bytes)", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_DOUBLE, "DeltaHeap (%)", -1, 3) );
    columnList.add ( new TableField(TableField.DATA_TYPE_INT, "NumLogRecords", -1, -1) );
    DataTable table = new DataTable( columnList );
    table.setTableID ( detailTableID );
    // Loop through the commands once to get the run times so that a total can be computed and
    // used for percent for each output record
    String commandName;
    long runTimeAll = 0;
    CommandProfile profile;
    for ( Command c : commandList ) {
        profile = c.getCommandProfile(CommandPhaseType.RUN);
        if ( profile.getEndTime() != 0 ) { // Check because ProfileCommands may have start but no end
            runTimeAll += profile.getRunTime();
        }
    }
    // Loop through the commands again and output runtime information to table
    CommandStatus status;
    long runTime;
    long startTimeMs, endTimeMs;
    String startTime, endTime;
    long startHeap, endHeap, deltaHeap;
    DateTime dt;
    int row = -1;
    int logRecordCount;
    boolean okToProcess = true;
    for ( Command c : commandList ) {
        ++row;
        if ( c == this ) {
            // Have found the current command so this command and following commands
            // will not have output for some information
            okToProcess = false;
        }
        // Format cell values
        commandName = c.getCommandName();
        profile = c.getCommandProfile(CommandPhaseType.RUN);
        logRecordCount = 0;
        if ( c instanceof CommandStatusProvider ) {
            status = ((CommandStatusProvider)c).getCommandStatus();
            logRecordCount = status.getCommandLog(CommandPhaseType.RUN).size();
        }
        startTimeMs = profile.getStartTime();
        dt = new DateTime(new Date(startTimeMs));
        startTime = dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm_SS_hh);
        endTimeMs = profile.getEndTime();
        dt = new DateTime(new Date(endTimeMs));
        endTime = dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm_SS_hh);
        runTime = profile.getRunTime();
        startHeap = profile.getStartHeap();
        endHeap = profile.getEndHeap();
        deltaHeap = endHeap - startHeap;
        int col = 0;
        // Add rows to the table
        table.setFieldValue(row,col++, new Integer(row + 1),true); // Command number (1+)
        table.setFieldValue(row,col++, commandName,true);
        if ( okToProcess ) {
            table.setFieldValue(row,col++, startTime,true);
            table.setFieldValue(row,col++, new Long(startTimeMs),true);
            table.setFieldValue(row,col++, endTime,true);
            table.setFieldValue(row,col++, new Long(endTimeMs),true);
            table.setFieldValue(row,col++, new Integer((int)runTime),true);
            if ( runTimeAll == 0 ) {
                table.setFieldValue(row,col++, new Double(0.0), true );
            }
            else {
                table.setFieldValue(row,col++, new Double(100.0*((double)runTime/(double)runTimeAll)),true);
            }
            table.setFieldValue(row,col++, new Long(startHeap),true);
            table.setFieldValue(row,col++, new Long(endHeap),true);
            table.setFieldValue(row,col++, new Long(deltaHeap),true);
            table.setFieldValue(row,col++, new Double(100.0*(double)deltaHeap/(double)endHeap),true);
            table.setFieldValue(row,col++, new Integer((int)(logRecordCount)),true);
        }
        else {
            table.setFieldValue(row,col++, null,true); // startTime...
            table.setFieldValue(row,col++, null,true);
            table.setFieldValue(row,col++, null,true);
            table.setFieldValue(row,col++, null,true);
            table.setFieldValue(row,col++, null,true);
            table.setFieldValue(row,col++, null,true);
            table.setFieldValue(row,col++, null,true);
            table.setFieldValue(row,col++, null,true);
            table.setFieldValue(row,col++, null,true);
            table.setFieldValue(row,col++, null,true);
            table.setFieldValue(row,col++, null,true); // ... through logRecordCount
        }
    }
    return table;
}

/**
Fill the summary table with command statistics.
@param table the table to fill
@param commandList list of commands to process
*/
private DataTable profileCommandsSummary ( String summaryTableID, List<Command> commandList )
throws Exception
{   // Create the summary table...
    List<TableField> columnList = new ArrayList<TableField>();
    columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Command", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_INT, "NumberOfOccurances", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "TotalTime (ms)", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_DOUBLE, "TotalTime (%)", -1, 3) );
    columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "AverageTime (ms)", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "MaximumTime (ms)", -1, -1) );
    columnList.add ( new TableField(TableField.DATA_TYPE_LONG, "MinimumTime (ms)", -1, -1) );
    DataTable table = new DataTable( columnList );
    table.setTableID ( summaryTableID );
    // Loop through the commands once to get a unique list of commands that are used
    List<String> commandNameList = new ArrayList<String>();
    String commandName;
    for ( Command c : commandList ) {
        commandName = c.getCommandName();
        boolean found = false;
        for ( String cname: commandNameList ) {
            if ( commandName.equalsIgnoreCase(cname) ) {
                found = true;
                break;
            }
        }
        if ( !found ) {
            commandNameList.add ( commandName );
        }
    }
    int [] runTimeTotal = new int[commandNameList.size()];
    int runTotalTimeAll = 0; // All commands
    int [] runTimeMean = new int[commandNameList.size()];
    int [] runTimeMax = new int[commandNameList.size()];
    int [] runTimeMin = new int[commandNameList.size()];
    int [] count = new int[commandNameList.size()];
    // Loop through the commands again to determine the profile statistics
    int pos;
    String cname;
    long runTime;
    CommandProfile profile;
    for ( Command c : commandList ) {
        commandName = c.getCommandName();
        profile = c.getCommandProfile(CommandPhaseType.RUN);
        // Find the position in the list
        for ( pos = 0; ; ++pos ) {
            cname = commandNameList.get(pos);
            if ( commandName.equalsIgnoreCase(cname) ) {
                break; 
            }
        }
        if ( profile.getEndTime() != 0 ) { // Check because ProfileCommands may have start but no end
            runTime = profile.getRunTime();
        }
        else {
            runTime = 0;
        }
        runTotalTimeAll += runTime;
        runTimeTotal[pos] += runTime;
        runTimeMin[pos] = (runTimeMin[pos] == 0 ? (int)runTime : Math.min(runTimeMin[pos], (int)runTime) );
        runTimeMax[pos] = Math.max(runTimeMax[pos], (int)runTime);
        ++count[pos];
    }
    // Compute the means
    for ( pos = 0; pos < runTimeTotal.length; pos++ ) {
        runTimeMean[pos] = runTimeTotal[pos]/count[pos];
    }
    // Add the statistics to the table for each command
    // Hard code column positions since not very complicated or dynamic
    // Command
    // Number
    // Total Time (ms)
    // Total Time (%)
    // Average Time (ms)
    // Maximum Time (ms)
    // Minimum Time (ms)
    for ( pos = 0; pos < runTimeTotal.length; ++pos ) {
        int col = 0;
        table.setFieldValue(pos,col++, commandNameList.get(pos),true);
        table.setFieldValue(pos,col++, new Integer(count[pos]),true);
        table.setFieldValue(pos,col++, new Long(runTimeTotal[pos]),true);
        if ( runTotalTimeAll == 0 ) {
            table.setFieldValue(pos,col++, new Double(0.0), true );
        }
        else {
            table.setFieldValue(pos,col++, new Double(100.0*((double)runTimeTotal[pos]/(double)runTotalTimeAll)),true);
        }
        table.setFieldValue(pos,col++, new Long(runTimeMean[pos]),true);
        table.setFieldValue(pos,col++, new Long(runTimeMax[pos]),true);
        table.setFieldValue(pos,col++, new Long(runTimeMin[pos]),true);
    }
    return table;
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
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;

	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // default
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoverySummaryTable ( null );
        setDiscoveryDetailTable ( null );
    }

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();

    String SummaryTableID = parameters.getValue ( "SummaryTableID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (SummaryTableID != null) && (SummaryTableID.indexOf("${") >= 0) ) {
		SummaryTableID = TSCommandProcessorUtil.expandParameterValue(processor, this, SummaryTableID);
	}
    String DetailTableID = parameters.getValue ( "DetailTableID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (DetailTableID != null) && (DetailTableID.indexOf("${") >= 0) ) {
		DetailTableID = TSCommandProcessorUtil.expandParameterValue(processor, this, DetailTableID);
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	try {
	    if ( (SummaryTableID != null) && !SummaryTableID.equals("") ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                DataTable table = profileCommandsSummary ( SummaryTableID, processor.getCommands() );
                
                // Set the table in the processor...
                
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
            else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
                // Create an empty table and set the ID
                DataTable table = new DataTable();
                table.setTableID ( SummaryTableID );
                setDiscoverySummaryTable ( table );
            }
	    }
        if ( (DetailTableID != null) && !DetailTableID.equals("") ) {
            if ( commandPhase == CommandPhaseType.RUN ) {
                DataTable table = profileCommandsDetail ( DetailTableID, processor.getCommands() );
                
                // Set the table in the processor...
                
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
            else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
                // Create an empty table and set the ID
                DataTable table = new DataTable();
                table.setTableID ( DetailTableID );
                setDiscoveryDetailTable ( table );
            }
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error creating command profile (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
		throw new CommandWarningException ( message );
	}
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		throw new CommandWarningException ( message );
	}

    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the detail table that is read by this class in discovery mode.
*/
private void setDiscoveryDetailTable ( DataTable table )
{
    __detailTable = table;
}

/**
Set the summary table that is read by this class in discovery mode.
*/
private void setDiscoverySummaryTable ( DataTable table )
{
    __summaryTable = table;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String DetailTableID = props.getValue( "DetailTableID" );
    String SummaryTableID = props.getValue( "SummaryTableID" );
	StringBuffer b = new StringBuffer ();
    if ( (SummaryTableID != null) && (SummaryTableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SummaryTableID=\"" + SummaryTableID + "\"" );
    }
    if ( (DetailTableID != null) && (DetailTableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DetailTableID=\"" + DetailTableID + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
