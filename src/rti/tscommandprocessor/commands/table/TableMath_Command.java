// TableMath_Command - This class initializes, checks, and runs the TableMath() command.

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
import RTi.Util.Table.DataTableMath;
import RTi.Util.Table.DataTableMathOperatorType;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRowConditionEvaluator;

/**
This class initializes, checks, and runs the TableMath() command.
*/
public class TableMath_Command extends AbstractCommand implements Command
{

/**
Values for NonValue parameter.
*/
protected final String _NaN = "NaN";
protected final String _Null = "Null";

/**
Constructor.
*/
public TableMath_Command () {
    super();
    setCommandName ( "TableMath" );
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
    String Input1 = parameters.getValue ( "Input1" );
    String ProcessRows = parameters.getValue ( "ProcessRows" );
    String Operator = parameters.getValue ( "Operator" );
    String Input2 = parameters.getValue ( "Input2" );
    String Output = parameters.getValue ( "Output" );
    String OutputType = parameters.getValue ( "OutputType" );
    String NonValue = parameters.getValue ( "NonValue" );
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

    if ( (ProcessRows != null) && !ProcessRows.isEmpty() ) {
    	// TODO smalers 2021-08-19 need to create validation tools.
    	String [] processRows = ProcessRows.split(",");
    	if ( (ProcessRows != null) && !ProcessRows.isEmpty() ) {
	    	for ( String processRow : processRows ) {
	    		processRow = processRow.trim();
	    		if ( processRow.equalsIgnoreCase("first") || processRow.equalsIgnoreCase("last") ) {
	    			// OK.
	    		}
	    		else {
	    			message = "The row to processed (" + processRow + ") is invalid.";
	    			warning += "\n" + message;
	    			status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
	    				message, "Provide the row as \"first\" or \"last\"." ) );
	    		}
	    	}
    	}
    }

    if ( (Input1 == null) || Input1.equals("") ) {
        message = "The Input1 column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a column name as Input1." ) );
    }

    DataTableMathOperatorType operatorType = null;
    if ( (Operator == null) || Operator.equals("") ) {
        message = "The operator must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the operator to process input." ) );
    }
    else {
        // Make sure that the operator is known in general.
        boolean supported = false;
        try {
            operatorType = DataTableMathOperatorType.valueOfIgnoreCase(Operator);
            supported = true;
        }
        catch ( Exception e ) {
            message = "The operator (" + Operator + ") is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported operator using the command editor." ) );
        }

        // Make sure that it is in the supported list.

        if ( supported ) {
            supported = false;
            List<DataTableMathOperatorType> operators = DataTableMath.getOperatorChoices();
            for ( int i = 0; i < operators.size(); i++ ) {
                if ( operatorType == operators.get(i) ) {
                    supported = true;
                }
            }
            if ( !supported ) {
                message = "The operator (" + Operator + ") is not supported by this command.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Select a supported operator using the command editor." ) );
            }
        }

        // Additional checks that depend on the operator.
        /* TODO SAM 2010-09-13 Add this later.
        if ( supported ) {
            int nRequiredValues = -1;
            try {
                TSUtil_CalculateTimeSeriesStatistic.getRequiredNumberOfValuesForStatistic ( operatorType );
            }
            catch ( Exception e ) {
                message = "Statistic \"" + operatorType + "\" is not recognized.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Contact software support." ) );
            }

            if ( nRequiredValues >= 1 ) {
                if ( (Input1 == null) || Input1.equals("") ) {
                    message = "Value1 must be specified for the statistic.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide Value1." ) );
                }
                else if ( !StringUtil.isDouble(Input1) ) {
                    message = "Value1 (" + Input1 + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify Value1 as a number." ) );
                }
            }

            if ( nRequiredValues >= 2 ) {
                if ( (Input2 == null) || Input2.equals("") ) {
                    message = "Value2 must be specified for the statistic.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide Value2." ) );
                }
                else if ( !StringUtil.isDouble(Input2) ) {
                    message = "Value2 (" + Input2 + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify Value2 as a number." ) );
                }
            }

            if ( nRequiredValues == 3 ) {
                if ( (Output == null) || Output.equals("") ) {
                    message = "Value3 must be specified for the statistic.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide Value3." ) );
                }
                else if ( !StringUtil.isDouble(Input2) ) {
                    message = "Value3 (" + Output + ") is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify Value3 as a number." ) );
                }
            }

            if ( nRequiredValues > 3 ) {
                message = "A maximum of 3 values are supported as input to statistic computation.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Refer to documentation for statistic.  Contact software support if necessary." ) );
            }
        }*/
    }

    // Confirm that Input2 is specified when needed.
    if ( operatorType != null ) {
    	if ( DataTableMath.requiresInput2(operatorType) && ((Input2 == null) || Input2.isEmpty()) ) {
            message = "The Input2 column/value must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Provide a column name or numeric constant as Input2." ) );
        }
    	// Check the inverse.
    	if ( ! DataTableMath.requiresInput2(operatorType) && ((Input2 != null) && !Input2.isEmpty()) ) {
            message = "The Input2 column/value should not be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Don't provide a column name or numeric constant as Input2." ) );
        }
    }

    if ( (Output == null) || Output.equals("") ) {
        message = "The output column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a column name for output." ) );
    }

    if ( (OutputType != null) && !OutputType.isEmpty() && (TableField.lookupDataType(OutputType) < 0) ) {
        message = "The output type (" + OutputType + ") is invalid";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify a valid output type using the command editor choices." ) );
    }

    if ( (NonValue != null) && !NonValue.equals("") && !NonValue.equals(_NaN) && !NonValue.equals(_Null) ) {
        message = "The NonValue value is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify NonValue as " + _NaN + " or " + _Null + " (default)." ) );
    }

    // Check for invalid parameters.
    List<String> validList = new ArrayList<>(9);
    validList.add ( "TableID" );
    validList.add ( "Condition" );
    validList.add ( "ProcessRows" );
    validList.add ( "Input1" );
    validList.add ( "Operator" );
    validList.add ( "Input2" );
    validList.add ( "Output" );
    validList.add ( "OutputType" );
    validList.add ( "NonValue" );
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
    return (new TableMath_JDialog ( parent, this, tableIDChoices )).ok();
}

// Parse command is in the base class.

/**
Method to execute the command.
@param command_number Number of command in sequence.
@exception Exception if there is an error processing the command.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException {
    String message, routine = getClass().getSimpleName() + ".runCommand";
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
   	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    String Condition = parameters.getValue ( "Condition" );
   	Condition = TSCommandProcessorUtil.expandParameterValue(processor, this, Condition);
    String ProcessRows = parameters.getValue ( "ProcessRows" );
   	ProcessRows = TSCommandProcessorUtil.expandParameterValue(processor, this, ProcessRows);
    String [] processRows = new String[0];
    boolean processAllRows = false;
    if ( (ProcessRows != null) && !ProcessRows.isEmpty() ) {
    	processRows = ProcessRows.split(",");
	    for ( int i = 0; i < processRows.length; i++ ) {
	    	processRows[i] = processRows[i].trim();
	    	if ( processRows[i].equals("*") ) {
	    		processAllRows = true;
	    		break;
	    	}
	    }
    }
    if ( processAllRows ) {
    	// Set the list of row numbers to process to empty.
    	processRows = new String[0];
    }
    String Input1 = parameters.getValue ( "Input1" );
   	Input1 = TSCommandProcessorUtil.expandParameterValue(processor, this, Input1);
    String Operator = parameters.getValue ( "Operator" );
    DataTableMathOperatorType operator = DataTableMathOperatorType.valueOfIgnoreCase(Operator);
    String Input2 = parameters.getValue ( "Input2" );
   	Input2 = TSCommandProcessorUtil.expandParameterValue(processor, this, Input2);
    String Output = parameters.getValue ( "Output" );
   	Output = TSCommandProcessorUtil.expandParameterValue(processor, this, Output);
    String OutputType = parameters.getValue ( "OutputType" );
    Message.printStatus(2, routine, "Output=\"" + Output + "\" OutputType=\"" + OutputType + "\"");
    int outputType = -1;
    if ( (OutputType != null) && !OutputType.isEmpty() ) {
    	outputType = TableField.lookupDataType(OutputType);
    }
    Message.printStatus(2, routine, "OutputType=\"" + OutputType + "\" outputType=" + outputType);
    String NonValue = parameters.getValue ( "NonValue" );
    Double NonValue_Double = null;
    if ( (NonValue != null) && !NonValue.equals("") ) {
        if ( NonValue.equalsIgnoreCase(_NaN) ) {
            NonValue_Double = Double.NaN;
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
    	// By default will process all rows without using an evaluator.
		TableRowConditionEvaluator evaluator = null;
		if ( ((Condition != null) && !Condition.isEmpty()) || (processRows.length > 0)) {
			// Process rows that match a condition:
			// - currently this is simple logic
			evaluator = new TableRowConditionEvaluator(table, Condition, processRows );
		}
        DataTableMath dtm = new DataTableMath ( table );
        dtm.math ( Input1, operator, Input2, Output, outputType, NonValue_Double, evaluator, problems );
    }
    catch ( Exception e ) {
        message = "Unexpected error performing table math (" + e + ").";
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
    String ProblemType = "TableMath";
    if ( (MaxWarnings_int > 0) && (problemsSize > MaxWarnings_int) ) {
        // Limit the warnings to the maximum.
        problemsSizeOutput = MaxWarnings_int;
    }
    if ( problemsSizeOutput < problemsSize ) {
        message = "Performing table math had " + problemsSize + " warnings - only " + problemsSizeOutput + " are listed.";
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
        // No recommendation since it is a user-defined check.
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
    	"Condition",
    	"ProcessRows",
    	"Input1",
    	"Operator",
    	"Input2",
    	"Output",
    	"OutputType",
    	"NonValue"
	};
	return this.toString(parameters, parameterOrder);
}

}