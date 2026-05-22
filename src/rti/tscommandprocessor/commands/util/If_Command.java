// If_Command - This class initializes, checks, and runs the If() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2026 Colorado Department of Natural Resources

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

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.config.ExpressionConfiguration;
import com.ezylang.evalex.data.EvaluationValue;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.TS.TS;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.JSON.JSONObject;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import riverside.datastore.DataStore;

/**
This class initializes, checks, and runs the If() command.
*/
public class If_Command extends AbstractCommand
{

/**
Possible values for CompareAsStrings parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Result of evaluating the condition.
*/
private boolean conditionEvalTotal = true;

/**
Constructor.
*/
public If_Command () {
	super();
	setCommandName ( "If" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages,
to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	String routine = getCommandName() + "_checkCommandParameters";
	String Name = parameters.getValue ( "Name" );
	String Condition = parameters.getValue ( "Condition" );
	String CompareAsStrings = parameters.getValue ( "CompareAsStrings" );
	String CompareAsVersions = parameters.getValue ( "CompareAsVersions" );
	String DataStoreIsOk = parameters.getValue ( "DataStoreIsOk" );
	String DataStoreIsNotOk = parameters.getValue ( "DataStoreIsNotOk" );
	String Expression = parameters.getValue ( "Expression" );
	String FileExists = parameters.getValue ( "FileExists" );
	String FileDoesNotExist = parameters.getValue ( "FileDoesNotExist" );
	String ObjectExists = parameters.getValue ( "ObjectExists" );
	String ObjectDoesNotExist = parameters.getValue ( "ObjectDoesNotExist" );
	String PropertyIsNotDefinedOrIsEmpty = parameters.getValue ( "PropertyIsNotDefinedOrIsEmpty" );
	String PropertyIsDefined = parameters.getValue ( "PropertyIsDefined" );
	String PropertyIsDefinedAndIsNotEmpty = parameters.getValue ( "PropertyIsDefinedAndIsNotEmpty" );
	String TableExists = parameters.getValue ( "TableExists" );
	String TableDoesNotExist = parameters.getValue ( "TableDoesNotExist" );
	String TSExists = parameters.getValue ( "TSExists" );
	String TSDoesNotExist = parameters.getValue ( "TSDoesNotExist" );
	String TSHasData = parameters.getValue ( "TSHasData" );
	String TSHasNoData = parameters.getValue ( "TSHasNoData" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

	boolean conditionProvided = false;
	boolean dataStoreIsOkProvided = false;
	boolean expressionProvided = false;
	boolean fileExistsProvided = false;
	boolean objectExistsProvided = false;
	// Any of the property checks.
	boolean propertyDefinedProvided = false;
	boolean tableExistsProvided = false;
	boolean tsExistsProvided = false;
	boolean tsHasDataProvided = false;

	if ( (Condition != null) && !Condition.isEmpty() ) {
		conditionProvided = true;
	}
	if ( ((DataStoreIsOk != null) && !DataStoreIsOk.isEmpty()) || ((DataStoreIsNotOk != null) && !DataStoreIsNotOk.isEmpty()) ) {
		dataStoreIsOkProvided = true;
	}
	if ( (Expression != null) && !Expression.isEmpty() ) {
		expressionProvided = true;
	}
	if ( ((FileExists != null) && !FileExists.isEmpty()) || ((FileDoesNotExist != null) && !FileDoesNotExist.isEmpty()) ) {
		fileExistsProvided = true;
	}
	if ( ((ObjectExists != null) && !ObjectExists.isEmpty()) || ((ObjectDoesNotExist != null) && !ObjectDoesNotExist.isEmpty()) ) {
		objectExistsProvided = true;
	}
	if ( (PropertyIsNotDefinedOrIsEmpty != null) && !PropertyIsNotDefinedOrIsEmpty.isEmpty() ) {
		propertyDefinedProvided = true;
	}
	if ( (PropertyIsDefined != null) && !PropertyIsDefined.isEmpty() ) {
		propertyDefinedProvided = true;
	}
	if ( (PropertyIsDefinedAndIsNotEmpty != null) && !PropertyIsDefinedAndIsNotEmpty.isEmpty() ) {
		propertyDefinedProvided = true;
	}
	if ( ((TableExists != null) && !TableExists.isEmpty()) || ((TableDoesNotExist != null) && !TableDoesNotExist.isEmpty()) ) {
		tableExistsProvided = true;
	}
	if ( ((TSExists != null) && !TSExists.isEmpty()) || ((TSDoesNotExist != null) && !TSDoesNotExist.isEmpty()) ) {
		tsExistsProvided = true;
	}
	if ( ((TSHasData != null) && !TSHasData.isEmpty()) || ((TSHasNoData != null) && !TSHasNoData.isEmpty()) ) {
		tsHasDataProvided = true;
	}

    if ( (Name == null) || Name.equals("") ) {
        message = "A name for the If() block must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the name." ) );
    }
    if ( !conditionProvided && !expressionProvided && !dataStoreIsOkProvided && !fileExistsProvided && !objectExistsProvided &&
   		!propertyDefinedProvided && !tableExistsProvided && !tsExistsProvided && !tsHasDataProvided ) {
        message = "A condition, expression, or check of datastore, file, object, property, table, or time series must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the condition." ) );
    }
    if ( (CompareAsStrings != null) && !CompareAsStrings.isEmpty() &&
    	!CompareAsStrings.equalsIgnoreCase(_False) && !CompareAsStrings.equalsIgnoreCase(_True) ) {
		message = "The property value \"" + CompareAsStrings + "\" is not valid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify CompareAsStrings as " + _False + " (default) or " + _True + "." ));
	}
    if ( (CompareAsVersions != null) && !CompareAsVersions.isEmpty() &&
    	!CompareAsVersions.equalsIgnoreCase(_False) && !CompareAsVersions.equalsIgnoreCase(_True) ) {
		message = "The property value \"" + CompareAsVersions + "\" is not valid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify CompareAsVersions as " + _False + " (default) or " + _True + "." ));
	}

	if ( (Expression != null) && !Expression.isEmpty() ) {
		// Make sure double quotes are not used.
		if ( Expression.contains("\"") ) {
            message = "Double quotes are not allowed in expressions (use single quotes around strings).";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Use single quotes around string literal values." ));
		}
	}

	// Check for invalid parameters.
    List<String> validList = new ArrayList<>(20);
	validList.add ( "Name" );
	validList.add ( "Condition" );
	validList.add ( "CompareAsStrings" );
	validList.add ( "CompareAsVersions" );
	validList.add ( "DataStoreIsOk" );
	validList.add ( "DataStoreIsNotOk" );
	validList.add ( "Expression" );
	validList.add ( "FileExists" );
	validList.add ( "FileDoesNotExist" );
	validList.add ( "ObjectExists" );
	validList.add ( "ObjectDoesNotExist" );
	validList.add ( "PropertyIsNotDefinedOrIsEmpty" );
	validList.add ( "PropertyIsDefined" );
	validList.add ( "PropertyIsDefinedAndIsNotEmpty" );
	validList.add ( "TableExists" );
	validList.add ( "TableDoesNotExist" );
	validList.add ( "TSExists" );
	validList.add ( "TSDoesNotExist" );
	validList.add ( "TSHasData" );
	validList.add ( "TSHasNoData" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine,
		warning );
		throw new InvalidCommandParameterException ( warning );
	}

	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new If_JDialog ( parent, this )).ok();
}

/**
Return the result of evaluating the condition, which is set when runCommand() is called.
@return the result of evaluating the condition
*/
public boolean getConditionEval () {
    return this.conditionEvalTotal;
}

/**
Return the name of the if command.
@return the name of the if command, should not be null.
*/
public String getName () {
    return getCommandParameters().getValue("Name");
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
    CommandProcessor processor = getCommandProcessor();
	PropList parameters = getCommandParameters();

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

	//String Name = parameters.getValue ( "Name" );
	String Condition = parameters.getValue ( "Condition" );
	String conditionUpper = null;
	if ( Condition != null ) {
		conditionUpper = Condition.toUpperCase();
	}
	String CompareAsStrings = parameters.getValue ( "CompareAsStrings" );
	boolean compareAsStrings = false; // Default.
	if ( (CompareAsStrings != null) && CompareAsStrings.equalsIgnoreCase(_True) ) {
		compareAsStrings = true;
	}
	String CompareAsVersions = parameters.getValue ( "CompareAsVersions" );
	boolean compareAsVersions = false; // Default.
	if ( (CompareAsVersions != null) && CompareAsVersions.equalsIgnoreCase(_True) ) {
		compareAsVersions = true;
	}
	String DataStoreIsOk = parameters.getValue ( "DataStoreIsOk" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		DataStoreIsOk = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreIsOk);
	}
	String DataStoreIsNotOk = parameters.getValue ( "DataStoreIsNotOk" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		DataStoreIsNotOk = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreIsNotOk);
	}
	String Expression = parameters.getValue ( "Expression" );
	String expressionExpanded = Expression;
	if ( commandPhase == CommandPhaseType.RUN ) {
		// Expand the expression to fill in properties, using a single quote for strings.
		String quote = "'";
		expressionExpanded = TSCommandProcessorUtil.expandParameterValue(processor, this, Expression, quote );
	}
	String FileExists = parameters.getValue ( "FileExists" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		FileExists = TSCommandProcessorUtil.expandParameterValue(processor, this, FileExists);
	}
	String FileDoesNotExist = parameters.getValue ( "FileDoesNotExist" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		FileDoesNotExist = TSCommandProcessorUtil.expandParameterValue(processor, this, FileDoesNotExist);
	}
	String ObjectExists = parameters.getValue ( "ObjectExists" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		ObjectExists = TSCommandProcessorUtil.expandParameterValue(processor, this, ObjectExists);
	}
	String ObjectDoesNotExist = parameters.getValue ( "ObjectDoesNotExist" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		ObjectDoesNotExist = TSCommandProcessorUtil.expandParameterValue(processor, this, ObjectDoesNotExist);
	}
	String PropertyIsNotDefinedOrIsEmpty = parameters.getValue ( "PropertyIsNotDefinedOrIsEmpty" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		PropertyIsNotDefinedOrIsEmpty = TSCommandProcessorUtil.expandParameterValue(processor, this, PropertyIsNotDefinedOrIsEmpty);
	}
	String PropertyIsDefined = parameters.getValue ( "PropertyIsDefined" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		PropertyIsDefined = TSCommandProcessorUtil.expandParameterValue(processor, this, PropertyIsDefined);
	}
	String PropertyIsDefinedAndIsNotEmpty = parameters.getValue ( "PropertyIsDefinedAndIsNotEmpty" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		PropertyIsDefinedAndIsNotEmpty = TSCommandProcessorUtil.expandParameterValue(processor, this, PropertyIsDefinedAndIsNotEmpty);
	}
	String TableExists = parameters.getValue ( "TableExists" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TableExists = TSCommandProcessorUtil.expandParameterValue(processor, this, TableExists);
	}
	String TableDoesNotExist = parameters.getValue ( "TableDoesNotExist" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TableDoesNotExist = TSCommandProcessorUtil.expandParameterValue(processor, this, TableDoesNotExist);
	}
	String TSExists = parameters.getValue ( "TSExists" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TSExists = TSCommandProcessorUtil.expandParameterValue(processor, this, TSExists);
	}
	String TSDoesNotExist = parameters.getValue ( "TSDoesNotExist" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TSDoesNotExist = TSCommandProcessorUtil.expandParameterValue(processor, this, TSDoesNotExist);
	}
	String TSHasData = parameters.getValue ( "TSHasData" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TSHasData = TSCommandProcessorUtil.expandParameterValue(processor, this, TSHasData);
	}
	String TSHasNoData = parameters.getValue ( "TSHasNoData" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TSHasNoData = TSCommandProcessorUtil.expandParameterValue(processor, this, TSHasNoData);
	}

	try {
		// A map of results is created:
		// - if a parameter is not specified, there will be no result added
		// - any value that is False at the end will cause an overall false
		Map<String,Boolean> conditionEvalMap = new LinkedHashMap<>();

	    if ( (Condition != null) && !Condition.isEmpty() ) {
	    	// Result from evaluating the condition:
		   	// - initialize to false
		   	// - evaluate all possible cases and set below
	       	boolean conditionEval = false;
    	    // TODO SAM 2013-12-07 Figure out if there is a more elegant way to do this.
    	    // Currently only Value1 Operator Value2 is allowed.  Brute force split by finding the operator.
    	    int pos, pos1 = -1, pos2 = -1;
    	    String value1 = "", value2 = "";
    	    String op = "??";
    	    if ( Condition.indexOf("<=") > 0 ) {
    	        pos = Condition.indexOf("<=");
    	        op = "<=";
    	        pos1 = pos;
    	        pos2 = pos + 2;
    	    }
    	    else if ( Condition.indexOf("<") > 0 ) {
    	        pos = Condition.indexOf("<");
    	        op = "<";
                pos1 = pos;
                pos2 = pos + 1;
    	    }
            else if ( Condition.indexOf(">=") > 0 ) {
                pos = Condition.indexOf(">=");
                op = ">=";
                pos1 = pos;
                pos2 = pos + 2;
            }
            else if ( Condition.indexOf(">") > 0 ) {
                pos = Condition.indexOf(">");
                op = ">";
                pos1 = pos;
                pos2 = pos + 1;
            }
            else if ( Condition.indexOf("==") > 0 ) {
                pos = Condition.indexOf("==");
                op = "==";
                pos1 = pos;
                pos2 = pos + 2;
            }
            else if ( Condition.indexOf("!=") > 0 ) {
                pos = Condition.indexOf("!=");
                op = "!=";
                pos1 = pos;
                pos2 = pos + 2;
            }
            else if ( conditionUpper.indexOf("!CONTAINS") > 0 ) {
            	// Put this before the next "CONTAINS" operator.
                pos = conditionUpper.indexOf("!CONTAINS");
                op = "!CONTAINS";
                pos1 = pos;
                pos2 = pos + 9;
                // "!contains" is only used on strings.
                compareAsStrings = true;
            }
            else if ( conditionUpper.indexOf("CONTAINS") > 0 ) {
                pos = conditionUpper.indexOf("CONTAINS");
                op = "CONTAINS";
                pos1 = pos;
                pos2 = pos + 8;
                // "contains" is only used on strings.
                compareAsStrings = true;
            }
            else if ( Condition.indexOf("=") > 0 ) {
                message = "Bad use of = in condition.";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Use == to check for equality." ) );
            }
            else {
                message = "Unknown condition operator for \"" + Condition + "\"";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Make sure condition operator is supported - refer to command editor and documentation." ) );
            }
    	    String arg1 = Condition.substring(0,pos1).trim();
    	    if ( Message.isDebugOn ) {
    	    	Message.printStatus(2, routine, "Left side: " + arg1 );
    	    }
    	    if ( arg1.contains("${") ) /* } so that editor will match  */ {
    	    	value1 = TSCommandProcessorUtil.expandParameterValue(this.getCommandProcessor(),this,arg1 );
    	    	if ( Message.isDebugOn ) {
    	    		Message.printStatus(2, routine, "Left side after expansion: " + value1 );
    	    	}
    	    	if ( value1.contains("${") ) /* } so that editor will match */ {
    	    		// Expansion did not match property.
    	    		int p1 = value1.indexOf("${"); // Editor will match the following line.
    	    		int p2 = value1.indexOf("}",p1);
    	    		String missingProp = value1.substring(p1,(p2+1));
	                message = "Left side of condition contains property " + missingProp + " that could not be matched.";
	                Message.printWarning(3,
	                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                    routine, message );
	                status.addToLog ( CommandPhaseType.RUN,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Make sure " + missingProp + " is defined." ) );
    	    	}
    	    }
    	    else {
    	    	value1 = arg1;
    	    }
    	    String arg2 = Condition.substring(pos2).trim();
    	    if ( Message.isDebugOn ) {
    	    	Message.printStatus(2, routine, "Right side:" + arg2 );
    	    }
    	    if ( arg2.contains("${") ) /* } so that editor will match.*/ {
    	    	value2 = TSCommandProcessorUtil.expandParameterValue(this.getCommandProcessor(),this,arg2 );
    	    	if ( Message.isDebugOn ) {
    	    		Message.printStatus(2, routine, "Right side after expansion: " + value2 );
    	    	}
    	    	if ( value2.contains("${") ) /* } so that editor will match. */ {
    	    		// Expansion did not match property.
    	    		int p1 = value2.indexOf("${"); // Editor will match the following line.
    	    		int p2 = value2.indexOf("}",p1);
    	    		String missingProp = value2.substring(p1,(p2+1));
	                message = "Right side of condition contains property " + missingProp + " that could not be matched.";
	                Message.printWarning(3,
	                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                    routine, message );
	                status.addToLog ( CommandPhaseType.RUN,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Make sure " + missingProp + " is defined." ) );
    	    	}
    	    }
    	    else {
    	    	value2 = arg2;
    	    }
    	    // If the arguments are quoted, then all of the following will be false.
            boolean isValue1Integer = StringUtil.isInteger(value1);
            boolean isValue2Integer = StringUtil.isInteger(value2);
            boolean isValue1Double = StringUtil.isDouble(value1);
            boolean isValue2Double = StringUtil.isDouble(value2);
            boolean isValue1Boolean = StringUtil.isBoolean(value1);
            boolean isValue2Boolean = StringUtil.isBoolean(value2);
            // Strip surrounding double quotes for comparisons below - do after above checks for type.
            value1 = value1.replace("\"", "");
            value2 = value2.replace("\"", "");
            // Check whether pairs are available:
            // - if not, compare as strings
            if ( compareAsVersions ) {
            }
            else if ( isValue1Integer && isValue2Integer ) {
            }
            else if ( isValue1Double && isValue2Double ) {
            }
            else if ( isValue1Boolean && isValue2Boolean ) {
            }
            else {
            	// Not comparing versions and not have pairs so compare as strings.
            	if ( Message.isDebugOn ) {
            		Message.printStatus(2,routine,"Values are not the same type so compare as strings.");
            	}
            	compareAsStrings = true;
            }
            if ( compareAsVersions ) {
            	// Compare the values as semantic versions (1.3.3.4, etc.):
            	// - make sure values are strings
            	if ( Message.isDebugOn ) {
            		Message.printStatus(2,routine,"Comparing version values.");
            	}
    	    	int maxParts = 0; // No limit on comparison.
    	    	if ( Message.isDebugOn ) {
    	    		Message.printStatus(2, routine, "Comparing semantic versions \"" + value1 + "\" " + op + " \"" + value2 + "\" maxParts=" + maxParts +
    	    		" isValue1Double="+ isValue1Double + " isvalue1Integer=" + isValue1Integer + " isValue1Boolean=" + isValue1Boolean +
    	    		" isValue2Double="+ isValue2Double + " isvalue2Integer=" + isValue2Integer + " isValue2Boolean=" + isValue2Boolean );
    	    	}
    	    	conditionEval = StringUtil.compareSemanticVersions(value1, op, value2, maxParts);
            }
            else if ( compareAsStrings ) {
            	// Always compare the string values or the input is not other understood type so assume strings.
            	if ( Message.isDebugOn ) {
            		Message.printStatus(2,routine,"Comparing string values.");
            	}
 	    	    if ( op.equals("CONTAINS") ) {
 	    	    	if ( value1.contains(value2) ) {
 	    	    	    conditionEval = true;
 	    	    	}
 	    	    	else {
 	    	    	    conditionEval = false;
 	    	    	}
 	    	    }
 	    	    else if ( op.equals("!CONTAINS") ) {
 	    	    	if ( !value1.contains(value2) ) {
 	    	    	    conditionEval = true;
 	    	    	}
 	    	    	else {
 	    	    	    conditionEval = false;
 	    	    	}
 	    	    }
 	    	    else {
 	    	    	int comp = value1.compareTo(value2);
	            	if ( op.equals("<=") ) {
	 	    	        if ( comp <= 0 ) {
	 	    	            conditionEval = true;
	 	    	        }
	 	    	        else {
	 	    	        	conditionEval = false;
 	    	    	    }
	 	    	    }
	 	    	    else if ( op.equals("<") ) {
	 	                if ( comp < 0 ) {
	 	                    conditionEval = true;
	 	                }
	 	    	        else {
	 	    	        	conditionEval = false;
 	    	    	    }
	 	    	    }
	 	    	    else if ( op.equals(">=") ) {
	 	                if ( comp >= 0 ) {
	 	                    conditionEval = true;
	 	                }
	 	    	        else {
	 	    	        	conditionEval = false;
 	    	    	    }
	 	    	    }
	 	    	    else if ( op.equals(">") ) {
	 	                if ( comp > 0 ) {
	 	                    conditionEval = true;
	 	                }
	 	    	        else {
	 	    	        	conditionEval = false;
 	    	    	    }
	 	    	    }
	 	    	    else if ( op.equals("==") ) {
	 	                if ( comp == 0 ) {
	 	                    conditionEval = true;
	 	                }
	 	    	        else {
	 	    	        	conditionEval = false;
 	    	    	    }
	 	    	    }
	 	    	    else if ( op.equals("!=") ) {
	 	                if ( comp != 0 ) {
	 	                    conditionEval = true;
	 	                }
	 	    	        else {
	 	    	        	conditionEval = false;
 	    	    	    }
	 	    	    }
 	    	    }
            }
            else if ( isValue1Integer && isValue2Integer ) {
            	// Do an integer comparison.
            	if ( Message.isDebugOn ) {
            		Message.printStatus(2,routine,"Comparing integer values.");
            	}
	    	    int ivalue1 = Integer.parseInt(value1);
	    	    int ivalue2 = Integer.parseInt(value2);
	    	    if ( op.equals("<=") ) {
	    	        if ( ivalue1 <= ivalue2 ) {
	    	            conditionEval = true;
	    	        }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals("<") ) {
	                if ( ivalue1 < ivalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals(">=") ) {
	                if ( ivalue1 >= ivalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals(">") ) {
	                if ( ivalue1 > ivalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals("==") ) {
	                if ( ivalue1 == ivalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals("!=") ) {
	                if ( ivalue1 != ivalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
            }
            else if ( isValue1Double && isValue2Double ) {
            	// Compare doubles.
            	if ( Message.isDebugOn ) {
            		Message.printStatus(2,routine,"Comparing double values.");
            	}
	    	    double dvalue1 = Double.parseDouble(value1);
	    	    double dvalue2 = Double.parseDouble(value2);
	    	    if ( op.equals("<=") ) {
	    	        if ( dvalue1 <= dvalue2 ) {
	    	            conditionEval = true;
	    	        }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals("<") ) {
	                if ( dvalue1 < dvalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals(">=") ) {
	                if ( dvalue1 >= dvalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals(">") ) {
	                if ( dvalue1 > dvalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals("==") ) {
	                if ( dvalue1 == dvalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals("!=") ) {
	                if ( dvalue1 != dvalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
            }
            else if ( isValue1Boolean && isValue2Boolean ) {
            	// Do a boolean comparison.
            	if ( Message.isDebugOn ) {
            		Message.printStatus(2,routine,"Comparing boolean values.");
            	}
	    	    boolean bvalue1 = Boolean.parseBoolean(value1);
	    	    boolean bvalue2 = Boolean.parseBoolean(value2);
	    	    if ( op.equals("<=") ) {
	    	        if ( !bvalue1 ) {
	    	        	// false <= false or true.
	    	            conditionEval = true;
	    	        }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals("<") ) {
	                if ( !bvalue1 && bvalue2 ) {
	                	// false < true.
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals(">=") ) {
	                if ( bvalue1 ) {
	                	// true >= false or true.
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals(">") ) {
	                if ( bvalue1 && !bvalue2 ) {
	                	// true > false.
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals("==") ) {
	                if ( bvalue1 == bvalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
	    	    else if ( op.equals("!=") ) {
	                if ( bvalue1 != bvalue2 ) {
	                    conditionEval = true;
	                }
 	    	        else {
	    	            conditionEval = false;
    	    	    }
	    	    }
            }
            else {
            	// Check to see if a property exists for left or right.
            	if ( Message.isDebugOn ) {
            		Message.printStatus(2,routine,"Fall through comparison (not version, string, double, integer, or boolean.");
            	}
            	if ( !value1.startsWith("${") && (processor.getProp(value1) != null) ) /* } so editor matches */ {
            		message = "Left and right have different type (is Value1 missing ${ }?) - cannot evaluate condition \"" + Condition + "\""; // } so editor matches.
	            	Message.printWarning(3,
	                	MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                	routine, message );
	            	status.addToLog ( CommandPhaseType.RUN,
	                	new CommandLogRecord(CommandStatusType.FAILURE,
	                    	message, "Make sure data types on each side of operator are the same - refer to command editor and documentation." ) );
            	}
            	else if ( !value2.startsWith("${") && (processor.getProp(value2) != null) ) /* } so editor matches */ {
            		message = "Left and right have different type (is Value2 missing ${ }?) - cannot evaluate condition \"" + Condition + "\""; // } so editor matches.
	            	Message.printWarning(3,
	                	MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                	routine, message );
	            	status.addToLog ( CommandPhaseType.RUN,
	                	new CommandLogRecord(CommandStatusType.FAILURE,
	                    	message, "Make sure data types on each side of operator are the same - refer to command editor and documentation." ) );
            	}
            	else {
            		// Show a general warning.
            		message = "Left and right have different type - cannot evaluate condition \"" + Condition + "\"";
	            	Message.printWarning(3,
	                	MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                	routine, message );
	            	status.addToLog ( CommandPhaseType.RUN,
	                	new CommandLogRecord(CommandStatusType.FAILURE,
	                    	message, "Make sure data types on each side of operator are the same - refer to command editor and documentation." ) );
            	}
            }
    	    if ( Condition.contains("${") ) /* } so editor matches. */ {
    	        // Show the original.
    	        status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.SUCCESS,
                        Condition + " (showing ${Property} notation) evaluates to " + conditionEval, "See also matching EndIf()" ) ); // } so editor matches.
    	    }
    	    // Always also show the expanded.
    	    status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.SUCCESS,
                    value1 + " " + op + " " + value2 + " evaluates to " + conditionEval, "See also matching EndIf()" ) );
   		    conditionEvalMap.put("Condition",Boolean.valueOf(conditionEval));
	    	if ( Message.isDebugOn ) {
        		Message.printStatus(2,routine,"Result from evaluating Condition, result = " + conditionEval);
	    	}
	    } // End evaluating the condition.
	    if ( (DataStoreIsOk != null) && !DataStoreIsOk.isEmpty() ) {
	    	// Get the matching datastore.
	    	DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName ( DataStoreIsOk, null );
	    	// All cases need to be evaluated below to properly set the result.
  			boolean dsConditionEval = false;
	    	if ( dataStore != null ) {
	    		if ( dataStore.getStatus() != 0 ) {
	    			dsConditionEval = false;
	    		}
	    		else {
	    			dsConditionEval = true;
	    		}
	    	}
            else {
            	// Datastore does not exist:
            	// - treat the condition as false
            	// - do not set a warning status on the command since it will interfere with the condition
            	// - do output a warning to the log file
            	dsConditionEval = false;
            	message = "Unable to find datastore \"" + DataStoreIsOk + "\" for DataStareIsOk";
            	Message.printWarning(3,
                	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                	routine, message );
            }
   		    conditionEvalMap.put("DataStoreIsOk",Boolean.valueOf(dsConditionEval));
	    	if ( Message.isDebugOn ) {
        		Message.printStatus(2,routine,"Result from evaluating DataStoreIsOk, result=" + dsConditionEval);
	    	}
	    }
	    if ( (DataStoreIsNotOk != null) && !DataStoreIsNotOk.isEmpty() ) {
	        // Check whether a datastore is OK- this is ANDed to the condition.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean dsConditionEval = false;
	    	DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName ( DataStoreIsNotOk, null );
	    	if ( dataStore != null ) {
	    		if ( dataStore.getStatus() == 0  ) {
	    			// The datastore is OK, so the condition is false.
	    			dsConditionEval = false;
	    		}
	    		else {
	    			dsConditionEval = true;
	    		}
	    	}
            else {
            	// Datastore does not exist:
            	// - so treat the condition as true since it is not OK
            	// - do not set a warning status on the command since it will interfere with the condition
            	// - do output a warning to the log file
    			dsConditionEval = true;
            	message = "Unable to find datastore \"" + DataStoreIsNotOk + "\" for DataStoreIsNotOk";
            	Message.printWarning(3,
                	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                	routine, message );
            }
   		    conditionEvalMap.put("DataStoreIsNotOk",Boolean.valueOf(dsConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating DataStoreIsNotOk, result=" + dsConditionEval);
    	    }
	    }
	    if ( (expressionExpanded != null) && !expressionExpanded.isEmpty() ) {
	    	Message.printStatus(2, routine, "Expanded expression: " + expressionExpanded);
    		// Enable single quotes to indicate strings.
    		ExpressionConfiguration configuration = ExpressionConfiguration.builder()
    			.singleQuoteStringLiteralsAllowed(true)
    			.build();
	    	Expression expression = null;
	    	boolean hadProblem = false;
	    	try {
	    		expression = new Expression ( expressionExpanded, configuration );
	    	}
	    	catch ( Exception e ) {
	    		hadProblem = true;
           		message = "Error parsing the expression (" + e + ").";
            	Message.printWarning(3,
                	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                	routine, message );
            	Message.printWarning(3,routine,e);
            	status.addToLog ( CommandPhaseType.RUN,
                	new CommandLogRecord(CommandStatusType.FAILURE,
                    	message, "Verify that the expression syntax is OK." ) );
	    	}
	    	EvaluationValue result = null;
	    	try {
	    		result = expression.evaluate();
	    	}
	    	catch ( Exception e ) {
	    		hadProblem = true;
           		message = "Error evaluating the expression (" + e + ").";
            	Message.printWarning(3,
                	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                	routine, message );
            	Message.printWarning(3,routine,e);
            	status.addToLog ( CommandPhaseType.RUN,
                	new CommandLogRecord(CommandStatusType.FAILURE,
                    	message, "Verify that the expression syntax is OK." ) );
	    	}
	    	// All cases need to be evaluated below to properly set the result.
	    	if ( !hadProblem ) {
	    		boolean expressionEval = false;
	    			if ( result.isBooleanValue() ) {
	    				// Expression result is a boolean value so can interpret the result.
	    				Message.printStatus(2, routine, "Expanded result is a boolean: " + result.getBooleanValue());
	    				expressionEval = result.getBooleanValue();
	    			}
	    			else {
	    				// Expression result is not a boolean value so cannot interpret the result.
            			message = "Expression result is not a boolean.  Cannot treat as a condition.";
            			Message.printWarning(3,
                			MessageUtil.formatMessageTag( command_tag, ++warning_count),
                			routine, message );
	    			}
   		    		conditionEvalMap.put("Expression",Boolean.valueOf(expressionEval));
            		if ( Message.isDebugOn ) {
       		     		Message.printStatus(2,routine,"After evaluating Expression, result=" + expressionEval);
    	    		}
	    	}
	    }
	    if ( (FileExists != null) && !FileExists.isEmpty() ) {
	        // Check whether a file exists - this is ANDed to the condition..
	    	// All cases need to be evaluated below to properly set the result.
  			boolean fileConditionEval = false;
	    	String FileExists_full = IOUtil.verifyPathForOS(
	    		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	    			TSCommandProcessorUtil.expandParameterValue(processor, this, FileExists) ) );
	    	File f = new File(FileExists_full);
            if ( f.exists() ) {
               	fileConditionEval = true;
            }
            else {
               	fileConditionEval = false;
            }
   		    conditionEvalMap.put("FileExists",Boolean.valueOf(fileConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating FileExists, result=" + fileConditionEval);
    	    }
	    }
	    if ( (FileDoesNotExist != null) && !FileDoesNotExist.isEmpty() ) {
	        // Check whether a file does not exist.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean fileConditionEval = false;
	    	String FileDoesNotExist_full = IOUtil.verifyPathForOS(
	    		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	    			TSCommandProcessorUtil.expandParameterValue(processor, this, FileDoesNotExist) ) );
	    	File f = new File(FileDoesNotExist_full);
            if ( !f.exists() ) {
                fileConditionEval = true;
            }
            else {
               	fileConditionEval = false;
            }
   		    conditionEvalMap.put("FileDoesNotExist",Boolean.valueOf(fileConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating FileDoesNotExist, result=" + fileConditionEval);
    	    }
	    }
	    if ( (ObjectExists != null) && !ObjectExists.isEmpty() ) {
	        // Check whether an object exists.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean objectConditionEval = false;
	        // Get the object to process.
	        JSONObject object = null;
            PropList request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "ObjectID", ObjectExists );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "GetObject", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetObject(ObjectID=\"" + ObjectExists + "\") from processor.";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_Object = bean_PropList.getContents ( "Object");
            if ( o_Object != null ) {
                object = (JSONObject)o_Object;
            }
            if ( object != null ) {
                objectConditionEval = true;
            }
            else {
               	objectConditionEval = false;
            }
   		    conditionEvalMap.put("ObjectExists",Boolean.valueOf(objectConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating ObjectExists, result=" + objectConditionEval);
    	    }
	    }
	    if ( (ObjectDoesNotExist != null) && !ObjectDoesNotExist.isEmpty() ) {
	        // Check whether a table exists.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean objectConditionEval = false;
	        // Get the table to process.  The table is searched backwards until the first match.
	        JSONObject object = null;
            PropList request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "ObjectID", ObjectDoesNotExist );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "GetObject", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetObject(ObjectID=\"" + ObjectDoesNotExist + "\") from processor.";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_object = bean_PropList.getContents ( "Object");
            if ( o_object != null ) {
                object = (JSONObject)o_object;
            }
            if ( object == null ) {
               	objectConditionEval = true;
            }
            else {
               	objectConditionEval = false;
            }
   		    conditionEvalMap.put("ObjectDoesNotExist",Boolean.valueOf(objectConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating ObjectDoesNotExist, result=" + objectConditionEval);
    	    }
	    }
	    if ( (PropertyIsNotDefinedOrIsEmpty != null) && !PropertyIsNotDefinedOrIsEmpty.isEmpty() ) {
	    	// Check to see whether the specified property exists.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean propConditionEval = false;
	    	Object o = processor.getPropContents(PropertyIsNotDefinedOrIsEmpty);
	    	if ( o == null ) {
	    		// Property is null so condition evaluates to true.
	    		propConditionEval = true;
	    	}
	    	else {
		    	if ( o instanceof String ) {
		    		String s = (String)o;
		    		if ( s.isEmpty() ) {
		    			// Property is empty string so condition evaluates to true.
		    			propConditionEval = true;
		    		}
		    		else {
		    			propConditionEval = false;
		    		}
		    	}
		    	else if ( o instanceof Double ) {
		    		Double d = (Double)o;
		    		if ( d.isNaN() ) {
		    			// Property is Double NaN so condition evaluates to true.
		    			propConditionEval = true;
		    		}
		    		else {
		    			propConditionEval = false;
		    		}
		    	}
		    	else if ( o instanceof Float ) {
		    		Float f = (Float)o;
		    		if ( f.isNaN() ) {
		    			// Property is Float NaN so condition evaluates to true.
		    			propConditionEval = true;
		    		}
		    		else {
		    			propConditionEval = false;
		    		}
		    	}
	    	}
   		    conditionEvalMap.put("PropertyIsNotDefinedOrIsEmpty",Boolean.valueOf(propConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating PropertyIsNotDefinedOrIsEmpty, result=" + propConditionEval);
    	    }
	    }
	    if ( (PropertyIsDefined != null) && !PropertyIsDefined.isEmpty() ) {
	    	// Check to see whether the specified property exists.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean propConditionEval = false;
	    	Object o = processor.getPropContents(PropertyIsDefined);
	    	if ( o == null ) {
	    		// Property is null so condition evaluates to false.
	    		propConditionEval = false;
	    	}
	    	else {
		    	if ( o instanceof Double ) {
		    		Double d = (Double)o;
		    		if ( d.isNaN() ) {
		    			// Property is Double NaN so condition evaluates to false.
		    			propConditionEval = false;
		    		}
		    		else {
		    			propConditionEval = true;
		    		}
		    	}
		    	else if ( o instanceof Float ) {
		    		Float f = (Float)o;
		    		if ( f.isNaN() ) {
		    			// Property is Float NaN so condition evaluates to false.
		    			propConditionEval = false;
		    		}
		    		else {
		    			propConditionEval = true;
		    		}
		    	}
	    	}
   		    conditionEvalMap.put("PropertyIsDefined",Boolean.valueOf(propConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating PropertyIsDefined, result=" + propConditionEval);
    	    }
	    }
	    if ( (PropertyIsDefinedAndIsNotEmpty != null) && !PropertyIsDefinedAndIsNotEmpty.isEmpty() ) {
	    	// Check to see whether the specified property exists and is not an empty string.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean propConditionEval = false;
	    	Object o = processor.getPropContents(PropertyIsDefinedAndIsNotEmpty);
	    	if ( o == null ) {
	    		// Property is null so condition evaluates to false.
	    		propConditionEval = false;
	    	}
	    	else {
		    	if ( o instanceof String ) {
		    		String s = (String)o;
		    		if ( s.isEmpty() ) {
		    			propConditionEval = false;
		    		}
		    		else {
		    			propConditionEval = true;
		    		}
		    	}
		    	else if ( o instanceof Double ) {
		    		Double d = (Double)o;
		    		if ( d.isNaN() ) {
		    			// Property is Double NaN so condition evaluates to false.
		    			propConditionEval = false;
		    		}
		    		else {
		    			propConditionEval = true;
		    		}
		    	}
		    	else if ( o instanceof Float ) {
		    		Float f = (Float)o;
		    		if ( f.isNaN() ) {
		    			// Property is Float NaN so condition evaluates to false.
		    			propConditionEval = false;
		    		}
		    		else {
		    			propConditionEval = true;
		    		}
		    	}
	    	}
   		    conditionEvalMap.put("PropertyIsDefinedAndIsNotEmpty",Boolean.valueOf(propConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating PropertyIsDefinedAndIsNotEmpty, result=" + propConditionEval);
    	    }
	    }
	    if ( (TableExists != null) && !TableExists.isEmpty() ) {
	        // Check whether a table exists.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean tableConditionEval = false;
	        // Get the table to process.  The table is searched backwards until the first match.
	        DataTable table = null;
            PropList request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "TableID", TableExists );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "GetTable", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTable(TableID=\"" + TableExists + "\") from processor.";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_Table = bean_PropList.getContents ( "Table");
            if ( o_Table != null ) {
                table = (DataTable)o_Table;
            }
            if ( table == null ) {
               	// Does not matter what the Condition had - the final result is false.
               	tableConditionEval = false;
            }
            else {
                tableConditionEval = true;
            }
   		    conditionEvalMap.put("TableExists",Boolean.valueOf(tableConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating TableExists, result=" + tableConditionEval);
    	    }
	    }
	    if ( (TableDoesNotExist != null) && !TableDoesNotExist.isEmpty() ) {
	        // Check whether a table exists.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean tableConditionEval = false;
	        // Get the table to process.  The table is searched backwards until the first match.
	        DataTable table = null;
            PropList request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "TableID", TableDoesNotExist );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "GetTable", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTable(TableID=\"" + TableDoesNotExist + "\") from processor.";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_Table = bean_PropList.getContents ( "Table");
            if ( o_Table != null ) {
                table = (DataTable)o_Table;
            }
            if ( table == null ) {
                tableConditionEval = true;
            }
            else {
               	tableConditionEval = false;
            }
   		    conditionEvalMap.put("TableDoesNotExist",Boolean.valueOf(tableConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating TableDoesNotExist, result=" + tableConditionEval);
    	    }
	    }
	    if ( (TSExists != null) && !TSExists.isEmpty() ) {
	        // Check whether a time series exists.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean tsConditionEval = false;
	        // Get the time series to process.  The time series list is searched backwards until the first match.
	        TS ts = null;
            PropList request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "TSID", TSExists );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSExists + "\") from processor.";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
            Object o_TS = null;
            if ( bean != null ) {
            	PropList bean_PropList = bean.getResultsPropList();
            	o_TS = bean_PropList.getContents ( "TS");
            }
            if ( o_TS != null ) {
                ts = (TS)o_TS;
            }
            if ( ts == null ) {
               	// Does not matter what the Condition had - the final result is false.
               	tsConditionEval = false;
               	if ( Message.isDebugOn ) {
               		Message.printStatus(2, routine, "Time series \"" + TSExists + "\" is null and therefore does not exist.");
               	}
            }
            else {
               	tsConditionEval = true;
            }
   		    conditionEvalMap.put("TSExists",Boolean.valueOf(tsConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating TSExists, result=" + tsConditionEval);
    	    }
	    }
	    if ( (TSDoesNotExist != null) && !TSDoesNotExist.isEmpty() ) {
	        // Check whether a time series does not exist.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean tsConditionEval = false;
	        // Get the time series to process.  The time series list is searched backwards until the first match.
	        TS ts = null;
            PropList request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "TSID", TSDoesNotExist );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSDoesNotExist + "\") from processor.";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
            Object o_TS = null;
            if ( bean != null ) {
            	PropList bean_PropList = bean.getResultsPropList();
            	o_TS = bean_PropList.getContents ( "TS");
            }
            if ( o_TS != null ) {
                ts = (TS)o_TS;
            }
            if ( ts == null ) {
                tsConditionEval = true;
            }
            else {
               	tsConditionEval = false;
            }
   		    conditionEvalMap.put("TSDoesNotExist",Boolean.valueOf(tsConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating TSDoesNotExist, result=" + tsConditionEval);
    	    }
	    }
	    if ( (TSHasData != null) && !TSHasData.isEmpty() ) {
	        // Check whether a time series has data.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean tsConditionEval = false;
	        // Get the time series to process.  The time series list is searched backwards until the first match.
	        TS ts = null;
            PropList request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "TSID", TSHasData );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSHasData + "\") from processor.";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
            Object o_TS = null;
            if ( bean != null ) {
            	PropList bean_PropList = bean.getResultsPropList();
            	o_TS = bean_PropList.getContents ( "TS");
            }
            if ( o_TS != null ) {
                ts = (TS)o_TS;
            }
            if ( ts == null ) {
               	tsConditionEval = false;
               	if ( Message.isDebugOn ) {
               		Message.printStatus(2, routine, "Time series \"" + TSHasData + "\" is null and therefore does not have data.");
               	}
            }
            else {
           		// Time series exists:
            	// - check whether it has data
            	if ( ts.hasData() ) {
            		if ( Message.isDebugOn ) {
            			Message.printStatus(2, routine, "Time series \"" + TSHasData + "\" has data.");
            		}
            		tsConditionEval = true;
            	}
            	else {
            		// Has no data so condition is false in this case.
            		if ( Message.isDebugOn ) {
            			Message.printStatus(2, routine, "Time series \"" + TSHasData + "\" does not have data.");
            		}
            		tsConditionEval = false;
            	}
            }
   		    conditionEvalMap.put("TSHasData",Boolean.valueOf(tsConditionEval));
            if ( Message.isDebugOn ) {
       		     Message.printStatus(2,routine,"After evaluating TSHasData, result=" + tsConditionEval);
    	    }
	    }
	    if ( (TSHasNoData != null) && !TSHasNoData.isEmpty() ) {
	        // Check whether a time series does not have data.
	    	// All cases need to be evaluated below to properly set the result.
  			boolean tsConditionEval = false;
	        // Get the time series to process.  The time series list is searched backwards until the first match.
	        TS ts = null;
            PropList request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "TSID", TSHasNoData );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSHasNoData + "\") from processor.";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
            Object o_TS = null;
            if ( bean != null ) {
            	PropList bean_PropList = bean.getResultsPropList();
            	o_TS = bean_PropList.getContents ( "TS");
            }
            if ( o_TS != null ) {
                ts = (TS)o_TS;
            }
            if ( ts == null ) {
            	// No time series so it has no data.
           		if ( Message.isDebugOn ) {
           			Message.printStatus(2, routine, "Time series \"" + TSHasNoData + "\" is null and therefore has no data.");
           		}
               	tsConditionEval = true;
            }
            else {
               	// Time series exists but may not have data.
           		if ( Message.isDebugOn ) {
           			Message.printStatus(2, routine, "Time series \"" + TSHasNoData + "\" is not null, checking for data.");
           		}
            	if ( !ts.hasData() ) {
            		// Time series does not have data.
            		if ( Message.isDebugOn ) {
            			Message.printStatus(2, routine, "Time series \"" + TSHasNoData + "\" does not have data.");
            		}
            		// Has no data so the condition is true.
            		tsConditionEval = true;
            	}
            	else {
            		// Has data so condition is false in this case.
            		if ( Message.isDebugOn ) {
            			Message.printStatus(2, routine, "Time series \"" + TSHasNoData + "\" does have data.");
            		}
            		tsConditionEval = false;
            	}
            }
   		    conditionEvalMap.put("TSHasNoData",Boolean.valueOf(tsConditionEval));
            if ( Message.isDebugOn ) {
  		         Message.printStatus(2,routine,"After evaluating TSHasNoData, result=" + tsConditionEval);
  	        }
	    }
	    // The final result will be false if any parameter evaluated to false.
	    setConditionEval ( conditionEvalMap );
	}
	catch ( Exception e ) {
		message = "Unexpected error executing If (" + e + ").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Check the log file or command window for details." ) );
		throw new CommandException ( message );
	}
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the result of evaluating the condition.
@param conditionEvalMap map of the results of evaluating each parameter
*/
private void setConditionEval ( Map<String,Boolean> conditionEvalMap ) {
	int trueCount = 0;
	int falseCount = 0;
	for ( Map.Entry<String, Boolean> entry : conditionEvalMap.entrySet() ) {
		if ( entry.getValue() ) {
			++trueCount;
		}
		else {
			++falseCount;
		}
	}
	if ( falseCount > 0 ) {
		// Have at least one false so the result is false.
		this.conditionEvalTotal = false;
	}
	else if ( trueCount == 0 ) {
		// No true so must be alse.
		this.conditionEvalTotal = false;
	}
	else {
		// Have all true.
		this.conditionEvalTotal = true;
	}
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
   String [] parameterOrder = {
		"Name",
		"Condition",
		"CompareAsStrings",
		"CompareAsVersions",
		"DataStoreIsOk",
		"DataStoreIsNotOk",
		"Expression",
		"FileExists",
		"FileDoesNotExist",
		"ObjectExists",
		"ObjectDoesNotExist",
		"PropertyIsNotDefinedOrIsEmpty",
		"PropertyIsDefined",
		"PropertyIsDefinedAndIsNotEmpty",
		"TableExists",
		"TableDoesNotExist",
		"TSExists",
		"TSDoesNotExist",
		"TSHasData",
		"TSHasNoData" };
	return super.toString(parameters,parameterOrder);
}

}