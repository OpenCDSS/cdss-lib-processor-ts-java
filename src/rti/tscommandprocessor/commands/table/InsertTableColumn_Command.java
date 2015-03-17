package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;

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
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableColumnType;
import RTi.Util.Table.TableField;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the InsertTableColumn() command.
*/
public class InsertTableColumn_Command extends AbstractCommand implements Command
{
    
/**
Constructor.
*/
public InsertTableColumn_Command ()
{	super();
	setCommandName ( "InsertTableColumn" );
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
    String InsertColumn = parameters.getValue ( "InsertColumn" );
    String ColumnType = parameters.getValue ( "ColumnType" );
    String ColumnWidth = parameters.getValue ( "ColumnWidth" );
    String ColumnPrecision = parameters.getValue ( "ColumnPrecision" );
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
    if ( (InsertColumn == null) || (InsertColumn.length() == 0) ) {
        message = "The column name to insert must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the column name to insert." ) );
    }
    if ( (ColumnType != null) && (ColumnType.length() > 0) ) {
        TableColumnType t = TableColumnType.valueOfIgnoreCase(ColumnType);
        if ( t == null ) {
            message = "The column type is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Select a colum type using the command editor." ) );
        }
    }
    if ( (ColumnWidth != null) && (ColumnWidth.length() == 0) ) {
        try {
            Integer.parseInt(ColumnWidth);
        }
        catch ( NumberFormatException e ) {
            message = "The column width is invalid (" + e + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the column width as an integer." ) );
        }
    }
    if ( (ColumnPrecision != null) && (ColumnPrecision.length() == 0) ) {
        try {
            Integer.parseInt(ColumnPrecision);
        }
        catch ( NumberFormatException e ) {
            message = "The column precision is invalid (" + e + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the column precision as an integer." ) );
        }
    }
 
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(3);
    validList.add ( "TableID" );
    validList.add ( "InsertColumn" );
    validList.add ( "InsertBeforeColumn" );
    validList.add ( "ColumnType" );
    validList.add ( "InitialValue" );
    validList.add ( "ColumnWidth" );
    validList.add ( "ColumnPrecision" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );    

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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed...
	return (new InsertTableColumn_JDialog ( parent, this, tableIDChoices )).ok();
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String TableID = parameters.getValue ( "TableID" );
    String InsertColumn = parameters.getValue ( "InsertColumn" );
    String InsertBeforeColumn = parameters.getValue ( "InsertBeforeColumn" );
    String ColumnType = parameters.getValue ( "ColumnType" );
    if ( (ColumnType == null) || ColumnType.equals("") ) {
        ColumnType = "" + TableColumnType.STRING;
    }
    String InitialValue = parameters.getValue ( "InitialValue" );
    String ColumnWidth = parameters.getValue ( "ColumnWidth" );
    int columnWidth = -1;
    if ( (ColumnWidth != null) && !ColumnWidth.equals("") ) {
        columnWidth = Integer.parseInt(ColumnWidth);
    }
    String ColumnPrecision = parameters.getValue ( "ColumnPrecision" );
    int columnPrecision = -1;
    if ( (ColumnPrecision != null) && !ColumnPrecision.equals("") ) {
        columnPrecision = Integer.parseInt(ColumnPrecision);
    }
    else if ( ColumnType.equalsIgnoreCase(""+TableColumnType.DOUBLE) ||
        ColumnType.equalsIgnoreCase(""+TableColumnType.FLOAT) ) {
        columnPrecision = 6;
    }
    
    // Get the table to process.

    DataTable table = null;
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
        }
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	// Insert the column...
	
	try {
		// Figure out the initial value
        Object initValue = null;
        int columnType = TableField.lookupDataType(ColumnType);
        if ( (InitialValue != null) && !InitialValue.isEmpty() ) {
        	if ( (columnType == TableField.DATA_TYPE_DATE) || (columnType == TableField.DATA_TYPE_DATETIME) ) {
        		// Try parsing the date string
        		try {
        			initValue = DateTime.parse(InitialValue);
        		}
        		catch ( Exception e ) {
    	        	Message.printWarning ( 3, routine, e );
    	    		message = "Error parsing date/time \"" + InitialValue + "\" (" + e + ").";
    	    		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
    	            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
    	                message, "Verify the initial value." ) );
        		}
        		if ( initValue != null ) {
        			if ( columnType == TableField.DATA_TYPE_DATE ) {
        				// Actually want Date
        				initValue = ((DateTime)initValue).getDate();
        			}
        		}
        	}
    		else if ( columnType == TableField.DATA_TYPE_DOUBLE ) {
    			try {
    				initValue = Double.parseDouble(InitialValue);
    			}
    			catch ( NumberFormatException e ) {
    	    		message = "Error parsing initial value \"" + InitialValue + "\" as type \"" + ColumnType + "\".";
    	    		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
    	            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
    	                message, "Verify the initial value." ) );
    			}
    		}
    		else if ( columnType == TableField.DATA_TYPE_FLOAT ) {
    			try {
    				initValue = Float.parseFloat(InitialValue);
    			}
    			catch ( NumberFormatException e ) {
    	    		message = "Error parsing initial value \"" + InitialValue + "\" as type \"" + ColumnType + "\".";
    	    		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
    	            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
    	                message, "Verify the initial value." ) );
    			}
    		}
    		else if ( columnType == TableField.DATA_TYPE_INT ) {
    			try {
    				initValue = Integer.parseInt(InitialValue);
    			}
    			catch ( NumberFormatException e ) {
    	    		message = "Error parsing initial value \"" + InitialValue + "\" as type \"" + ColumnType + "\".";
    	    		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
    	            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
    	                message, "Verify the initial value." ) );
    			}
    		}
    		else if ( columnType == TableField.DATA_TYPE_LONG ) {
    			try {
    				initValue = Long.parseLong(InitialValue);
    			}
    			catch ( NumberFormatException e ) {
    	    		message = "Error parsing initial value \"" + InitialValue + "\" as type \"" + ColumnType + "\".";
    	    		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
    	            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
    	                message, "Verify the initial value." ) );
    			}
    		}
    		else if ( columnType == TableField.DATA_TYPE_SHORT ) {
    			try {
    				initValue = Short.parseShort(InitialValue);
    			}
    			catch ( NumberFormatException e ) {
    	    		message = "Error parsing initial value \"" + InitialValue + "\" as type \"" + ColumnType + "\".";
    	    		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
    	            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
    	                message, "Verify the initial value." ) );
    			}
    		}
    		else if ( columnType == TableField.DATA_TYPE_STRING ) {
    			initValue = InitialValue;
    		}
        	else {
	    		message = "Do not know how to process column type \"" + ColumnType + "\" for initial value \"" + InitialValue + "\".";
	    		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
	            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Report problem to software support - column type needs to be enabled." ) );
        	}
        }
	    if ( (InsertBeforeColumn != null) && !InsertBeforeColumn.equals("") ) {
	        // Insert before an existing column
	        int col = -1;
	        try {
	            col = table.getFieldIndex(InsertBeforeColumn);
	        }
	        catch ( Exception e ) {
	        	Message.printWarning ( 3, routine, e );
	    		message = "Error determining column number for InsertBeforeColumn \"" + InsertBeforeColumn + "\" (" + e + ").";
	    		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
	            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Verify the column name." ) );
	        }
            table.addField(col,new TableField(columnType,InsertColumn,columnWidth,columnPrecision), initValue);
	    }
        else {
            // Insert the column at the end of the table
            table.addField(new TableField(TableField.lookupDataType(ColumnType),InsertColumn,columnWidth,columnPrecision), initValue);
        }
 	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error inserting column in table (" + e + ").";
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
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TableID = props.getValue( "TableID" );
    String InsertColumn = props.getValue( "InsertColumn" );
    String InsertBeforeColumn = props.getValue( "InsertBeforeColumn" );
    String ColumnType = props.getValue( "ColumnType" );
    String InitialValue = props.getValue( "InitialValue" );
    String ColumnWidth = props.getValue( "ColumnWidth" );
    String ColumnPrecision = props.getValue( "ColumnPrecision" );
	StringBuffer b = new StringBuffer ();
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (InsertColumn != null) && (InsertColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InsertColumn=\"" + InsertColumn + "\"" );
    }
    if ( (InsertBeforeColumn != null) && (InsertBeforeColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InsertBeforeColumn=\"" + InsertBeforeColumn + "\"" );
    }
    if ( (ColumnType != null) && (ColumnType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ColumnType=" + ColumnType );
    }
    if ( (InitialValue != null) && (InitialValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InitialValue=\"" + InitialValue + "\"" );
    }
    if ( (ColumnWidth != null) && (ColumnWidth.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ColumnWidth=" + ColumnWidth );
    }
    if ( (ColumnPrecision != null) && (ColumnPrecision.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ColumnPrecision=" + ColumnPrecision );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}