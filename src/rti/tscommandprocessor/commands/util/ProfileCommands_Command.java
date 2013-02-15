package rti.tscommandprocessor.commands.util;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.List;
import java.util.Vector;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;

/**
This class initializes, checks, and runs the ProfileCommands() command.
*/
public class ProfileCommands_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
The table that is created.
*/
private DataTable __table = null;

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
{	String TableID = parameters.getValue ( "TableID" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }
 
	// Check for invalid parameters...
	List<String> valid_Vector = new Vector();
    valid_Vector.add ( "TableID" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );    

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
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable table = getDiscoveryTable();
    List v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector();
        v.add ( table );
    }
    return v;
}

// Use base class parseCommand()

/**
Fill the output table with command statistics.
@param table the table to fill
@param commanList list of commands to process
*/
private void profileCommands ( DataTable table, List<Command> commandList )
throws Exception
{
    // Loop through the commands once to get a unique list of commands that are used
    List<String> commandNameList = new Vector<String>();
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
    for ( Command c : commandList ) {
        commandName = c.getCommandName();
        // Find the position in the list
        for ( pos = 0; ; ++pos ) {
            cname = commandNameList.get(pos);
            if ( commandName.equalsIgnoreCase(cname) ) {
                break; 
            }
        }
        runTime = c.getRunTime();
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
        table.setFieldValue(pos,col++, new Integer(runTimeTotal[pos]),true);
        if ( runTotalTimeAll == 0 ) {
            table.setFieldValue(pos,col++, new Double(0.0) );
        }
        else {
            table.setFieldValue(pos,col++, new Double(100.0*(double)runTimeTotal[pos]/(double)runTotalTimeAll),true);
        }
        table.setFieldValue(pos,col++, new Integer(runTimeMean[pos]),true);
        table.setFieldValue(pos,col++, new Integer(runTimeMax[pos]),true);
        table.setFieldValue(pos,col++, new Integer(runTimeMin[pos]),true);
    }
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
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String TableID = parameters.getValue ( "TableID" );

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	try {
    	// Create the table...
    
	    List<TableField> columnList = new Vector();
	    DataTable table = null;
        
        if ( command_phase == CommandPhaseType.RUN ) {
            // Create the table with column data
            columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Command", -1, -1) );
            columnList.add ( new TableField(TableField.DATA_TYPE_INT, "Number", -1, -1) );
            columnList.add ( new TableField(TableField.DATA_TYPE_INT, "Total Time (ms)", -1, -1) );
            columnList.add ( new TableField(TableField.DATA_TYPE_DOUBLE, "Total Time (%)", -1, 3) );
            columnList.add ( new TableField(TableField.DATA_TYPE_INT, "Average Time (ms)", -1, -1) );
            columnList.add ( new TableField(TableField.DATA_TYPE_INT, "Maximum Time (ms)", -1, -1) );
            columnList.add ( new TableField(TableField.DATA_TYPE_INT, "Minimum Time (ms)", -1, -1) );
            table = new DataTable( columnList );
            table.setTableID ( TableID );
            
            // Fill the table with command profile
            profileCommands ( table, processor.getCommands() );
            
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
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
            }
        }
        else if ( command_phase == CommandPhaseType.DISCOVERY ) {
            // Create an empty table and set the ID
            table = new DataTable();
            table.setTableID ( TableID );
            setDiscoveryTable ( table );
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error creating new table (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
		throw new CommandWarningException ( message );
	}
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		throw new CommandWarningException ( message );
	}

    status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __table = table;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TableID = props.getValue( "TableID" );
	StringBuffer b = new StringBuffer ();
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}