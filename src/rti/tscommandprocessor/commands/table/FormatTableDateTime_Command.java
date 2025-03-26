// FormatTableDateTime_Command - This class initializes, checks, and runs the FormatTableDateTime() command.

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
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeFormatterType;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;
import RTi.Util.Time.YearType;

/**
This class initializes, checks, and runs the FormatTableDateTime() command.
*/
public class FormatTableDateTime_Command extends AbstractCommand implements Command
{

/**
Possible value for OutputType.
*/
protected final String _DateTime = "DateTime";
protected final String _Double = "Double";
protected final String _Integer = "Integer";
protected final String _String = "String";
    
/**
Constructor.
*/
public FormatTableDateTime_Command ()
{   super();
    setCommandName ( "FormatTableDateTime" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{   String TableID = parameters.getValue ( "TableID" );
    String InputColumn = parameters.getValue ( "InputColumn" );
    String IncrementStart = parameters.getValue ( "IncrementStart" );
    String IncrementBaseUnit = parameters.getValue ( "IncrementBaseUnit" );
    String FormatterType = parameters.getValue ( "FormatterType" );
    String DateTimeFormat = parameters.getValue ( "DateTimeFormat" );
    String OutputYearType = parameters.getValue ( "OutputYearType" );
    String OutputColumn = parameters.getValue ( "OutputColumn" );
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
    
    if ( (InputColumn == null) || InputColumn.isEmpty() ) {
        message = "The table input column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the table input column name." ) );
    }
    
    if ( (IncrementStart != null) && !IncrementStart.isEmpty() && (IncrementStart.indexOf("${") < 0) ) {
    	try {
    		DateTime.parse(IncrementStart);
    	}
    	catch ( Exception e ) {
	        message = "The increment start date/time (" + IncrementStart + ") is invalid.";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
	            message, "Specify a valid increment start date/time, for example YYYY-MM-DD hh:mm:ss." ) );
    	}
    }
    
    if ( (IncrementBaseUnit != null) && !IncrementBaseUnit.isEmpty() ) {
    	List<String> units = TimeInterval.getTimeIntervalBaseChoices(TimeInterval.SECOND, TimeInterval.YEAR, 1, false);
    	boolean found = false;
    	for ( String unit: units ) {
    		if ( unit.equalsIgnoreCase(IncrementBaseUnit) ) {
    			found = true;
    			break;
    		}
    	}
        if ( !found ) {
            message = "The IncrementBaseUnit (" + IncrementBaseUnit + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Use the command editor to select a increment base unit." ) );
        }
    }
    
    if ( (FormatterType != null) && !FormatterType.equals("") ) {
        // Check the value given the type - only support types that are enabled in this command.
        if ( !FormatterType.equalsIgnoreCase(""+DateTimeFormatterType.C) ) {
            message = "The date/time formatter \"" + FormatterType + "\" is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the date/time formatter type as " + DateTimeFormatterType.C ));
        }
    }
    
    if ( (DateTimeFormat == null) || DateTimeFormat.isEmpty() ) {
        message = "The date/time format must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a format to process date/time input." ) );
    }

    if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
        YearType type = null;
        try {
            type = YearType.valueOfIgnoreCase(OutputYearType);
        }
        catch ( Exception e ) {
            type = null;
        }
        if ( type == null ) {
            message = "The OutputYearType (" + OutputYearType + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Use the command editor to select a valid year type." ) );
        }
    }

    if ( (OutputColumn == null) || OutputColumn.isEmpty() ) {
        message = "The output column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a column name for output." ) );
    }
    
    // Check for invalid parameters.
    List<String> validList = new ArrayList<>(10);
    validList.add ( "TableID" );
    validList.add ( "InputColumn" );
    validList.add ( "IncrementStart" );
    validList.add ( "IncrementBaseUnit" );
    validList.add ( "FormatterType" );
    validList.add ( "DateTimeFormat" );
    validList.add ( "OutputYearType" );
    validList.add ( "OutputColumn" );
    validList.add ( "OutputType" );
    validList.add ( "InsertBeforeColumn" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );
    
    if ( warning.length() > 0 ) {
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(command_tag,warning_level), warning );
        throw new InvalidCommandParameterException ( warning );
    }
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{   List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    return (new FormatTableDateTime_JDialog ( parent, this, tableIDChoices )).ok();
}

// Parse command is in the base class.

/**
Method to execute the command.
@param command_number Number of command in sequence.
@exception Exception if there is an error processing the command.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   String message, routine = getClass().getSimpleName() + ".runCommand";
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    //int log_level = 3;  // Level for non-use messages for log file.
    
    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
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
    
    // Get the input parameters.
    
    String TableID = parameters.getValue ( "TableID" );
    if ( (TableID != null) && !TableID.isEmpty() && (commandPhase == CommandPhaseType.RUN) ) {
    	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String InputColumn = parameters.getValue ( "InputColumn" );
	InputColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, InputColumn);
    String IncrementStart = parameters.getValue ( "IncrementStart" );
	IncrementStart = TSCommandProcessorUtil.expandParameterValue(processor, this, IncrementStart);
    String IncrementBaseUnit = parameters.getValue ( "IncrementBaseUnit" );
    TimeInterval incrementBaseUnit = null;
    if ( (IncrementBaseUnit != null) && !IncrementBaseUnit.isEmpty() ) {
    	incrementBaseUnit = TimeInterval.parseInterval(IncrementBaseUnit);
    }
    String FormatterType = parameters.getValue ( "FormatterType" );
    if ( (FormatterType == null) || FormatterType.equals("") ) {
        FormatterType = "" + DateTimeFormatterType.C;
    }
    DateTimeFormatterType formatterType = DateTimeFormatterType.valueOfIgnoreCase(FormatterType);
    String DateTimeFormat = parameters.getValue ( "DateTimeFormat" );
	DateTimeFormat = TSCommandProcessorUtil.expandParameterValue(processor, this, DateTimeFormat);
    String OutputYearType = parameters.getValue ( "OutputYearType" );
    YearType yearType = null;
    if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
        yearType = YearType.valueOfIgnoreCase(OutputYearType);
    }
    String OutputColumn = parameters.getValue ( "OutputColumn" );
	OutputColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputColumn);
    String OutputType = parameters.getValue ( "OutputType" );
    if ( (OutputType == null) || OutputType.equals("") ) {
        OutputType = _String;
    }
    String InsertBeforeColumn = parameters.getValue ( "InsertBeforeColumn" );
	InsertBeforeColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, InsertBeforeColumn);

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
    
    // Get the increment start at runtime.
    DateTime IncrementStart_DateTime = null;
	try {
		IncrementStart_DateTime = TSCommandProcessorUtil.getDateTime ( IncrementStart, "IncrementStart", processor,
			status, warning_level, command_tag );
	}
	catch ( InvalidCommandParameterException e ) {
		// Warning will have been added above.
		++warning_count;
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

    List<String> problems = new ArrayList<>();
    try {
        Object output;
        TableRecord rec;
        Object o = null;
        DateTime dt;
        // The insert before column goes first because it may be used as the position when inserting the output column.
        int insertBeforeColumnNum = -1;
        if ( (InsertBeforeColumn != null) && !InsertBeforeColumn.equals("") ) {
            try {
                insertBeforeColumnNum = table.getFieldIndex(InsertBeforeColumn);
            }
            catch ( Exception e ) {
                message = "InsertBeforeColumn \"" + InsertBeforeColumn + "\" not found in table \"" + TableID + "\" - adding output column at end.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
                    message, "Verify that table includes requested column \"" + InsertBeforeColumn + "\"." ) );
            }
        }
        // If wrong output column name is specified, a new one will be added - user will need to notice.
        int outputColumnNum = -1;
        try {
            outputColumnNum = table.getFieldIndex(OutputColumn);
        }
        catch ( Exception e ) {
            // Output column was not matched so add the column (use insert position to indicate where to add).
            if ( OutputType.equalsIgnoreCase(_DateTime) ) {
                outputColumnNum = table.addField(insertBeforeColumnNum,
                    new TableField(TableField.DATA_TYPE_DATETIME, OutputColumn, -1, -1), null);
            }
            else if ( OutputType.equalsIgnoreCase(_Double) ) {
                outputColumnNum = table.addField(insertBeforeColumnNum,
                    new TableField(TableField.DATA_TYPE_DOUBLE, OutputColumn, -1, -1), null);
            }
            else if ( OutputType.equalsIgnoreCase(_Integer) ) {
                outputColumnNum = table.addField(insertBeforeColumnNum,
                    new TableField(TableField.DATA_TYPE_INT, OutputColumn, -1, -1), null);
            }
            else if ( OutputType.equalsIgnoreCase(_String) ) {
                outputColumnNum = table.addField(insertBeforeColumnNum,
                    new TableField(TableField.DATA_TYPE_STRING, OutputColumn, -1, -1), null);
            }
        }
        // Get the input column after getting the output column because the above may insert a column.
        int inputColumnNum = -1;
        boolean fatalError = false;
        try {
            inputColumnNum = table.getFieldIndex(InputColumn);
        }
        catch ( Exception e ) {
            message = "Input column \"" + InputColumn + "\" not found in table \"" + TableID + "\"";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that table includes requested column \"" + InputColumn + "\"." ) );
            fatalError = true;
        }
        if ( !fatalError ) {
	        // Loop through the table records.
        	boolean doIncrement = false; // Whether increment is being processed.
        	DateTime incrementDateTime = null;
        	int incrementBase = 0;
        	if ( (IncrementStart_DateTime != null) && (incrementBaseUnit != null) ) {
        		doIncrement = true;
        		// This date/time will be reset and incremented below:
        		// - use the highest precision because increment may be fractions of seconds
        		// - the resulting DateTime will be reformatted into the final output using the DateTimeFormat specifier
        		incrementDateTime = new DateTime(IncrementStart_DateTime,DateTime.PRECISION_NANOSECOND);
        		incrementBase = incrementBaseUnit.getBase();
        		Message.printStatus(2, routine, "Processing incremental time using start " + incrementDateTime +
        			" and increment base unit " + incrementBaseUnit );
        	}
        	int incrementInteger = 0;
        	double incrementDouble = 0;
	        int n = table.getNumberOfRecords();
	        String dtString;
	        for ( int irec = 0; irec < n; irec++ ) {
		        output = null;
	            try {
	                // Get the date/time.
	                rec = table.getRecord(irec);
	                o = rec.getFieldValue(inputColumnNum);
	                dt = null;
	                if ( o == null ) {
	                    output = null;
	                    continue;
	                }
	                if ( doIncrement ) {
	                	// IncrementStart will have the value corresponding to the first row.
	                	// To optimize performance, reset a copy of the start and then increment.
	                	dt = incrementDateTime; // dt is used below in generic code.
	                	incrementDateTime.setDate(IncrementStart_DateTime);
	                	if ( o instanceof Integer ) {
	                		incrementInteger = (Integer)o;
	                		// TODO SAM 2016-02-01
	                		// Need to see how well this performs since it has to increment a lot of time.
	                		// However, computing an increment from previous value may be more difficult to handle if gaps, nulls, etc.
	                		incrementDateTime.addInterval(incrementBase, incrementInteger);
	                		if ( Message.isDebugOn ) {
	                			Message.printStatus(2, routine, "Row=" + (irec + 1) + " date/time=" + incrementDateTime +
	                				" DateTime precision=" + incrementDateTime.getPrecision());
	                		}
	                	}
	                	else if ( o instanceof Double ) {
	                		incrementDouble = (Double)o;
	                		incrementDateTime.addInterval(incrementBase, incrementDouble);
	                		if ( Message.isDebugOn ) {
	                			Message.printStatus(2, routine, "Row=" + (irec + 1) + " increment=" + incrementDouble +
	                				" incrementBase=" + incrementBase + " date/time=" + incrementDateTime +
	                				" DateTime precision=" + incrementDateTime.getPrecision());
	                		}
	                	}
	                	else {
	                		problems.add("Increment value type is not handled.  Verify that incremental value is an integer or a floating point number.");
	                	}
	                }
	                else {
	                	// Not doing increment so need to parse input column and then reformat in output.
		                // Handle input that is a String, Integer (year) or a DateTime.
		                if ( o instanceof String ) {
		                	// Try to parse the string into DateTime - handle exception in main loop.
		                	//Message.printStatus(2, routine, "Parsing string to DateTime \"" + o + "\"");
		                	dt = DateTime.parse((String)o);
		                }
		                else if ( o instanceof Integer ) {
		                	dt = new DateTime(DateTime.PRECISION_YEAR);
		                	dt.setYear((Integer)o);
		                }
		                else if ( o instanceof DateTime ) {
		                	dt = (DateTime)o;
		                }
	                	//Message.printStatus(2, routine, "After parsing DateTime \"" + dt + "\"");
	                }
	                // Now format the date/time object into the output column.
	                if ( dt == null ) {
	                	output = null;
	                }
	                else {
	                    // First format as a string.
	                    if ( formatterType == DateTimeFormatterType.C ) {
	                    	//Message.printStatus(2, routine, "Internal DateTime before formatting=\"" + dt + "\"");
	                        dtString = TimeUtil.formatDateTime(dt, yearType, DateTimeFormat);
	                    	//Message.printStatus(2, routine, "Internal DateTime string after formatting=\"" + dtString + "\"");
	                    }
	                    else {
	                        // Should not happen.
	                        dtString = null;
	                    }
	                    // Then cast to another object type if requested and set in the table:
	                    // - a new output object is always created
	                    if ( OutputType.equalsIgnoreCase(_DateTime) ) {
	                        output = DateTime.parse(dtString);
	                    	//Message.printStatus(2, routine, "Output DateTime after parsing=\"" + output + "\"");
	                    }
	                    else if ( OutputType.equalsIgnoreCase(_Double) ) {
	                        output = Double.parseDouble(dtString);
	                    }
	                    else if ( OutputType.equalsIgnoreCase(_Integer) ) {
	                        output = Integer.parseInt(dtString);
	                    }
	                    else if ( OutputType.equalsIgnoreCase(_String) ) {
	                        output = dtString;
	                    }
	                }
                	//Message.printStatus(2, routine, "Setting table cell to \"" + output + "\"");
	                rec.setFieldValue(outputColumnNum, output);
                	//Message.printStatus(2, routine, "Table cell after set = \"" + rec.getFieldValueString(outputColumnNum) + "\"");
	            }
	            catch ( Exception e2 ) {
	                problems.add("Error formatting table row " + (irec + 1) + " column \"" + InputColumn + "\" date/time \"" + o + "\" (" + e2 + ").");
	                Message.printWarning(3, routine, e2);
	            }
	        }
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error formatting date/time (" + e + ").";
        Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check log file for details." ) );
        throw new CommandException ( message );
    }
    
    int MaxWarnings_int = 500; // Limit the problems to 500 to prevent command overload.
    int problemsSize = problems.size();
    int problemsSizeOutput = problemsSize;
    String ProblemType = "FormatTableString";
    if ( (MaxWarnings_int > 0) && (problemsSize > MaxWarnings_int) ) {
        // Limit the warnings to the maximum.
        problemsSizeOutput = MaxWarnings_int;
    }
    if ( problemsSizeOutput < problemsSize ) {
        message = "Performing date/time formatting had " + problemsSize + " warnings - only " + problemsSizeOutput + " are listed.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
        // No recommendation since it is a user-defined check.
        // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
        status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.WARNING, ProblemType, message, "" ) );
    }
    for ( int iprob = 0; iprob < problemsSizeOutput; iprob++ ) {
        message = problems.get(iprob);
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
        // No recommendation since it is a user-defined check
        // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
        status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.WARNING, ProblemType, message, "" ) );
    }
    
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
        throw new CommandWarningException ( message );
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
    	"TableID",
    	"InputColumn",
    	"IncrementStart",
    	"IncrementBaseUnit",
    	"FormatterType",
    	"DateTimeFormat",
    	"OutputYearType",
    	"OutputColumn",
    	"OutputType",
    	"InsertBeforeColumn"
	};
	return this.toString(parameters, parameterOrder);
}

}