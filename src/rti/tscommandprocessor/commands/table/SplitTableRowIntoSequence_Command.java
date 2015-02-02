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
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the SplitTableRowIntoSequence() command.
*/
public class SplitTableRowIntoSequence_Command extends AbstractCommand implements Command
{

/**
Possible values for DeleteOriginalRow parameter.
*/
protected final String _False = "False";
protected final String _True = "True";
    
/**
Constructor.
*/
public SplitTableRowIntoSequence_Command ()
{	super();
	setCommandName ( "SplitTableRowIntoSequence" );
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
	String MeasureStartColumn = parameters.getValue ( "MeasureStartColumn" );
	String MeasureEndColumn = parameters.getValue ( "MeasureEndColumn" );
	String MeasureIncrement = parameters.getValue ( "MeasureIncrement" );
	String MinimumStartSegmentLength = parameters.getValue ( "MinimumStartSegmentLength" );
	String MinimumEndSegmentLength = parameters.getValue ( "MinimumEndSegmentLength" );
	String DeleteOriginalRow = parameters.getValue ( "DeleteOriginalRow" );
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
    
    if ( (MeasureStartColumn == null) || (MeasureStartColumn.length() == 0) ) {
        message = "The measure start column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the measure start column." ) );
    }
    
    if ( (MeasureEndColumn == null) || (MeasureEndColumn.length() == 0) ) {
        message = "The measure end column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the measure end column." ) );
    }
    
    if ( (MeasureIncrement == null) || !StringUtil.isDouble(MeasureIncrement) ) {
        message = "The measure increment must be specified as a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the measure increment as a number." ) );
    }
    else {
    	double measureIncrement = Double.parseDouble(MeasureIncrement);
		// TODO SAM 2015-01-11 expand until the end values for iteration in runCommand()
    	// so that they are evenly divisible by the increment, within a reasonable tolerance
		if ( measureIncrement > 1.0 ) {
			message = "The measure increment (" + MeasureIncrement + ") is > 1.0";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Specify the measure increment as < 1.0" ) );
		}
    }
    
    if ( (MinimumStartSegmentLength != null) && !MinimumStartSegmentLength.equals("") && !StringUtil.isDouble(MinimumStartSegmentLength)) {
		message = "The minimum start segment length (" + MinimumStartSegmentLength + ") is invalid";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum start segment length as a number." ) );
    }
    
    if ( (MinimumEndSegmentLength != null) && !MinimumEndSegmentLength.equals("") && !StringUtil.isDouble(MinimumEndSegmentLength)) {
		message = "The minimum end segment length (" + MinimumEndSegmentLength + ") is invalid";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum end segment length as a number" ) );
    }

    if ( (DeleteOriginalRow != null) && (DeleteOriginalRow.length() != 0) && !DeleteOriginalRow.equalsIgnoreCase(_False) &&
        !DeleteOriginalRow.equalsIgnoreCase(_True)) {
        message = "The DeleteOriginalRow parameter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify DeleteOriginalRow as " + _False + " (default) or " + _True) );
    }
    
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(8);
    validList.add ( "TableID" );
    validList.add ( "MeasureStartColumn" );
    validList.add ( "MeasureEndColumn" );
    validList.add ( "MeasureIncrement" );
    validList.add ( "MinimumStartSegmentLength" );
    validList.add ( "MinimumEndSegmentLength" );
    validList.add ( "DeleteOriginalRow" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed...
	return (new SplitTableRowIntoSequence_JDialog ( parent, this, tableIDChoices )).ok();
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getName() + ".runCommandInternal",message = "";
	int warning_level = 2;
	int log_level = 3; // Level for non-user messages for log file.
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType command_phase = CommandPhaseType.RUN;
    status.clearLog(command_phase);

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String TableID = parameters.getValue ( "TableID" );
    String MeasureStartColumn = parameters.getValue ( "MeasureStartColumn" );
    String MeasureEndColumn = parameters.getValue ( "MeasureEndColumn" );
    String MeasureIncrement = parameters.getValue ( "MeasureIncrement" );
    double measureIncrement = 0.0;
    if ( MeasureIncrement != null ) {
    	measureIncrement = Double.parseDouble(MeasureIncrement);
    }
    String MinimumStartSegmentLength = parameters.getValue ( "MinimumStartSegmentLength" );
    double minimumStartSegmentLength = 0.0; // Indicates no minimum length to include partial segment
    if ( MinimumStartSegmentLength != null ) {
    	minimumStartSegmentLength = Double.parseDouble(MinimumStartSegmentLength);
    }
    String MinimumEndSegmentLength = parameters.getValue ( "MinimumEndSegmentLength" );
    double minimumEndSegmentLength = 0.0; // Indicates no minimum length to include partial segment
    if ( MinimumEndSegmentLength != null ) {
    	minimumEndSegmentLength = Double.parseDouble(MinimumEndSegmentLength);
    }
    String DeleteOriginalRow = parameters.getValue ( "DeleteOriginalRow" );
    boolean deleteOriginalRow = false;
    if ( (DeleteOriginalRow != null) && DeleteOriginalRow.equalsIgnoreCase(_True) ) {
    	deleteOriginalRow = true;
    }
    
    // Get the table to process.

    DataTable table = null;
    if ( command_phase == CommandPhaseType.RUN ) {
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
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	try {
    	// Make sure that the table has the columns for the measures
		int startCol = -1;
		try {
			startCol = table.getFieldIndex(MeasureStartColumn);
		}
		catch ( Exception e ) {
            message = "Table \"" + TableID + "\" does not contain measure start column \"" + MeasureStartColumn + "\"";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the table contains the requested start column \"" + MeasureStartColumn + "\"" ) );
		}
		int endCol = -1;
		try {
			endCol = table.getFieldIndex(MeasureEndColumn);
		}
		catch ( Exception e ) {
            message = "Table \"" + TableID + "\" does not contain measure end column \"" + MeasureStartColumn + "\"";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the table contains the requested end column \"" + MeasureEndColumn + "\"" ) );
		}
		if ( (startCol >= 0) && (endCol >= 0) ) {
			// Loop through the table to process all of the rows
			int nrec = table.getNumberOfRecords();
			Double startVal, endVal;
			Object val;
			int numRowsAdded = 0;
			for ( int irec = 0; irec < nrec; irec++ ) {
				Message.printStatus(2, routine, "Processing record " + irec );
				numRowsAdded = 0;
				// Get the values for the start and end measure values
				val = table.getFieldValue(irec, startCol);
				if ( (val == null) || !(val instanceof Double) ) {
					continue;
				}
				startVal = (Double)val;
				val = table.getFieldValue(irec, endCol);
				if ( (val == null) || !(val instanceof Double) ) {
					continue;
				}
				endVal = (Double)val;
				// If the start and end are reversed, switch here
				if ( startVal > endVal ) {
					Double tmp = endVal;
					endVal = startVal;
					startVal = tmp;
				}
				// Find the closest integer points to the start and end
				double floor = Math.floor(startVal);
				double ceil = Math.ceil(endVal);
				// Insert new rows after the current row
				double segEnd;
				boolean addSeg = false;
				for ( double segStart = floor; segStart <= ceil; segStart += measureIncrement ) {
					addSeg = false;
					segEnd = segStart + measureIncrement;
					if ( segEnd <= startVal ) {
						// Right edge of first segment is right at boundary but not straddling yet
						continue;
					}
					else if ( segStart >= endVal ) {
						// Done with all segments - check whether need to delete original row and reposition iterator
						if ( deleteOriginalRow ) {
							// Delete the original row and set the row appropriately for the loop iterator
							table.deleteRecord(irec);
							Message.printStatus(2,routine,"Deleted original record " + irec );
							--irec;
							nrec = table.getNumberOfRecords();
							Message.printStatus(2,routine,"Reset record due to deletion, irec=" + irec + ", nrec=" + nrec );
						}
						// Position the record pointer so that the next original record is processed
						irec += numRowsAdded;
						Message.printStatus(2,routine,"Reset record due to " + numRowsAdded + " rows added previously, irec=" + irec );
						break;
					}
					else if ( (startVal >= segStart ) && (segEnd > startVal) ) {
						// First segment straddles start, check whether to include
						if ( (segEnd - startVal) >= minimumStartSegmentLength ) {
							addSeg = true;
						}
						else {
							Message.printStatus(2, routine, "Not adding segment because length " + (segEnd - startVal) + " < " + minimumStartSegmentLength );
						}
					}
					else if ( (segStart < endVal) && (segEnd >= endVal) ) {
						// Last segment straddles end, check whether to include
						if ( (endVal - segStart) >= minimumEndSegmentLength ) {
							addSeg = true;
						}
						else {
							Message.printStatus(2, routine, "Not adding segment because length " + (endVal - segStart) + " < " + minimumEndSegmentLength );
						}
					}
					else {
						// Segment fully in the reach so add
						addSeg = true;
					}
					if ( addSeg ) {
						// Add the segment
						// First copy the original row contents
						TableRecord newRec = new TableRecord(table.getRecord(irec));
						// Next set the values in the new record
						newRec.setFieldValue(startCol, new Double(segStart));
						newRec.setFieldValue(endCol, new Double(segEnd));
						int irecInsert = irec + numRowsAdded + 1;
						table.insertRecord(irecInsert, newRec, true);
						++numRowsAdded;
						nrec = table.getNumberOfRecords();
						Message.printStatus(2, routine, "Inserting segment record " + irecInsert + ", segStart=" + segStart + ", segEnd=" + segEnd +
							", numRowsAdded=" + numRowsAdded + ", nrec=" + nrec );
					}
				}
			}
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error splitting table row (" + e + ").";
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
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TableID = props.getValue( "TableID" );
    String MeasureStartColumn = props.getValue( "MeasureStartColumn" );
    String MeasureEndColumn = props.getValue( "MeasureEndColumn" );
	String MeasureIncrement = props.getValue( "MeasureIncrement" );
	String MinimumStartSegmentLength = props.getValue( "MinimumStartSegmentLength" );
	String MinimumEndSegmentLength = props.getValue( "MinimumEndSegmentLength" );
	String DeleteOriginalRow = props.getValue( "DeleteOriginalRow" );
	StringBuffer b = new StringBuffer ();
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (MeasureStartColumn != null) && (MeasureStartColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MeasureStartColumn=\"" + MeasureStartColumn + "\"" );
    }
    if ( (MeasureEndColumn != null) && (MeasureEndColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MeasureEndColumn=\"" + MeasureEndColumn + "\"" );
    }
	if ( (MeasureIncrement != null) && (MeasureIncrement.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "MeasureIncrement=" + MeasureIncrement );
	}
    if ( (MinimumStartSegmentLength != null) && (MinimumStartSegmentLength.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MinimumStartSegmentLength=\"" + MinimumStartSegmentLength + "\"" );
    }
    if ( (MinimumEndSegmentLength != null) && (MinimumEndSegmentLength.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MinimumEndSegmentLength=\"" + MinimumEndSegmentLength + "\"" );
    }
    if ( (DeleteOriginalRow != null) && (DeleteOriginalRow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DeleteOriginalRow=\"" + DeleteOriginalRow + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}