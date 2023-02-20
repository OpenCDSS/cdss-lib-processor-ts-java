// If_Command - This class initializes, checks, and runs the If() command.

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

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
private boolean conditionEval = true;

/**
Constructor.
*/
public If_Command ()
{	super();
	setCommandName ( "If" );
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
{	String routine = getCommandName() + "_checkCommandParameters";
	String Name = parameters.getValue ( "Name" );
	String Condition = parameters.getValue ( "Condition" );
	String CompareAsStrings = parameters.getValue ( "CompareAsStrings" );
	String FileExists = parameters.getValue ( "FileExists" );
	String FileDoesNotExist = parameters.getValue ( "FileDoesNotExist" );
	String ObjectExists = parameters.getValue ( "ObjectExists" );
	String ObjectDoesNotExist = parameters.getValue ( "ObjectDoesNotExist" );
	String PropertyIsNotDefinedOrIsEmpty = parameters.getValue ( "PropertyIsNotDefinedOrIsEmpty" );
	String PropertyIsDefined = parameters.getValue ( "PropertyIsDefined" );
	String TableExists = parameters.getValue ( "TableExists" );
	String TableDoesNotExist = parameters.getValue ( "TableDoesNotExist" );
	String TSExists = parameters.getValue ( "TSExists" );
	String TSDoesNotExist = parameters.getValue ( "TSDoesNotExist" );
	String warning = "";
	String message;
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	boolean conditionProvided = false;
	boolean fileExistsProvided = false;
	boolean objectExistsProvided = false;
	boolean propertyDefinedProvided = false;
	boolean tableExistsProvided = false;
	boolean tsExistsProvided = false;
	
	if ( (Condition != null) && !Condition.isEmpty() ) {
		conditionProvided = true;
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
	if ( ((TableExists != null) && !TableExists.isEmpty()) || ((TableDoesNotExist != null) && !TableDoesNotExist.isEmpty()) ) {
		tableExistsProvided = true;
	}
	if ( ((TSExists != null) && !TSExists.isEmpty()) || ((TSDoesNotExist != null) && !TSDoesNotExist.isEmpty()) ) {
		tsExistsProvided = true;
	}

    if ( (Name == null) || Name.equals("") ) {
        message = "A name for the If() block must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the name." ) );
    }
    if ( !conditionProvided && !fileExistsProvided && !objectExistsProvided &&
   		!propertyDefinedProvided && !tableExistsProvided && !tsExistsProvided ) {
        message = "A condition or check of file, object, property, table, or time series must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the condition." ) );
    }
    if ( CompareAsStrings != null && !CompareAsStrings.isEmpty() &&
    	!CompareAsStrings.equalsIgnoreCase(_False) && !CompareAsStrings.equalsIgnoreCase(_True) ) {
		message = "The property value \"" + CompareAsStrings + "\" is not valid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify CompareAsStrings as " + _False + " (default) or " + _True + "." ));
	}

	// Check for invalid parameters.
    List<String> validList = new ArrayList<>(13);
	validList.add ( "Name" );
	validList.add ( "Condition" );
	validList.add ( "CompareAsStrings" );
	validList.add ( "FileExists" );
	validList.add ( "FileDoesNotExist" );
	validList.add ( "ObjectExists" );
	validList.add ( "ObjectDoesNotExist" );
	validList.add ( "PropertyIsNotDefinedOrIsEmpty" );
	validList.add ( "PropertyIsDefined" );
	validList.add ( "TableExists" );
	validList.add ( "TableDoesNotExist" );
	validList.add ( "TSExists" );
	validList.add ( "TSDoesNotExist" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
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
    return this.conditionEval;
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
throws CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
    CommandProcessor processor = getCommandProcessor();
	PropList parameters = getCommandParameters();
	
	CommandStatus status = getCommandStatus();
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
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
	
	//String Name = parameters.getValue ( "Name" );
	String Condition = parameters.getValue ( "Condition" );
	String conditionUpper = null;
	if ( Condition != null ) {
		conditionUpper = Condition.toUpperCase();
	}
	String CompareAsStrings = parameters.getValue ( "CompareAsStrings" );
	boolean compareAsStrings = false;
	if ( (CompareAsStrings != null) && CompareAsStrings.equalsIgnoreCase(_True) ) {
		compareAsStrings = true;
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

	try {
	    boolean conditionEval = false;
	    setConditionEval ( conditionEval );
	    if ( (Condition != null) && !Condition.isEmpty() ) {
    	    // TODO SAM 2013-12-07 Figure out if there is a more elegant way to do this
    	    // Currently only Value1 Operator Value2 is allowed.  Brute force split by finding the operator
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
            	// Put this before the next "CONTAINS" operator
                pos = conditionUpper.indexOf("!CONTAINS");
                op = "!CONTAINS";
                pos1 = pos;
                pos2 = pos + 9;
                compareAsStrings = true; // "!contains" is only used on strings
            }
            else if ( conditionUpper.indexOf("CONTAINS") > 0 ) {
                pos = conditionUpper.indexOf("CONTAINS");
                op = "CONTAINS";
                pos1 = pos;
                pos2 = pos + 8;
                compareAsStrings = true; // "contains" is only used on strings
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
    	    if ( arg1.indexOf("${") >= 0 ) {
    	    	value1 = TSCommandProcessorUtil.expandParameterValue(this.getCommandProcessor(),this,arg1 );
    	    	if ( Message.isDebugOn ) {
    	    		Message.printStatus(2, routine, "Left side after expansion: " + value1 );
    	    	}
    	    	if ( value1.indexOf("${") >= 0 ) {
    	    		// Expansion did not match property
    	    		int p1 = value1.indexOf("${");
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
    	    if ( arg2.indexOf("${") >= 0 ) {
    	    	value2 = TSCommandProcessorUtil.expandParameterValue(this.getCommandProcessor(),this,arg2 );
    	    	if ( Message.isDebugOn ) {
    	    		Message.printStatus(2, routine, "Right side after expansion: " + value2 );
    	    	}
    	    	if ( value2.indexOf("${") >= 0 ) {
    	    		// Expansion did not match property
    	    		int p1 = value2.indexOf("${");
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
    	    // If the arguments are quoted, then all of the following will be false
            boolean isValue1Integer = StringUtil.isInteger(value1);
            boolean isValue2Integer = StringUtil.isInteger(value2);
            boolean isValue1Double = StringUtil.isDouble(value1);
            boolean isValue2Double = StringUtil.isDouble(value2);
            boolean isValue1Boolean = StringUtil.isBoolean(value1);
            boolean isValue2Boolean = StringUtil.isBoolean(value2);
            // Strip surrounding double quotes for comparisons below - do after above checks for type
            value1 = value1.replace("\"", "");
            value2 = value2.replace("\"", "");
            if ( !compareAsStrings && isValue1Integer && isValue2Integer ) {
            	// Do an integer comparison
	    	    int ivalue1 = Integer.parseInt(value1);
	    	    int ivalue2 = Integer.parseInt(value2);
	    	    if ( op.equals("<=") ) {
	    	        if ( ivalue1 <= ivalue2 ) {
	    	            conditionEval = true;
	    	        }
	    	    }
	    	    else if ( op.equals("<") ) {
	                if ( ivalue1 < ivalue2 ) {
	                    conditionEval = true;
	                } 
	    	    }
	    	    else if ( op.equals(">=") ) {
	                if ( ivalue1 >= ivalue2 ) {
	                    conditionEval = true;
	                }
	    	    }
	    	    else if ( op.equals(">") ) {
	                if ( ivalue1 > ivalue2 ) {
	                    conditionEval = true;
	                }
	    	    }
	    	    else if ( op.equals("==") ) {
	                if ( ivalue1 == ivalue2 ) {
	                    conditionEval = true;
	                }
	    	    }
	    	    else if ( op.equals("!=") ) {
	                if ( ivalue1 != ivalue2 ) {
	                    conditionEval = true;
	                }
	    	    }
            }
            else if ( !compareAsStrings && isValue1Double && isValue2Double ) {
            	// Compare doubles
	    	    double dvalue1 = Double.parseDouble(value1);
	    	    double dvalue2 = Double.parseDouble(value2);
	    	    if ( op.equals("<=") ) {
	    	        if ( dvalue1 <= dvalue2 ) {
	    	            conditionEval = true;
	    	        }
	    	    }
	    	    else if ( op.equals("<") ) {
	                if ( dvalue1 < dvalue2 ) {
	                    conditionEval = true;
	                } 
	    	    }
	    	    else if ( op.equals(">=") ) {
	                if ( dvalue1 >= dvalue2 ) {
	                    conditionEval = true;
	                }
	    	    }
	    	    else if ( op.equals(">") ) {
	                if ( dvalue1 > dvalue2 ) {
	                    conditionEval = true;
	                }
	    	    }
	    	    else if ( op.equals("==") ) {
	                if ( dvalue1 == dvalue2 ) {
	                    conditionEval = true;
	                }
	    	    }
	    	    else if ( op.equals("!=") ) {
	                if ( dvalue1 != dvalue2 ) {
	                    conditionEval = true;
	                }
	    	    }
            }
            else if ( !compareAsStrings && isValue1Boolean && isValue2Boolean ) {
            	// Do a boolean comparison
	    	    boolean bvalue1 = Boolean.parseBoolean(value1);
	    	    boolean bvalue2 = Boolean.parseBoolean(value2);
	    	    if ( op.equals("<=") ) {
	    	        if ( !bvalue1 ) {
	    	        	// false <= false or true
	    	            conditionEval = true;
	    	        }
	    	    }
	    	    else if ( op.equals("<") ) {
	                if ( !bvalue1 && bvalue2 ) {
	                	// false < true
	                    conditionEval = true;
	                } 
	    	    }
	    	    else if ( op.equals(">=") ) {
	                if ( bvalue1 ) {
	                	// true >= false or true
	                    conditionEval = true;
	                }
	    	    }
	    	    else if ( op.equals(">") ) {
	                if ( bvalue1 && !bvalue2 ) {
	                	// true > false
	                    conditionEval = true;
	                }
	    	    }
	    	    else if ( op.equals("==") ) {
	                if ( bvalue1 == bvalue2 ) {
	                    conditionEval = true;
	                }
	    	    }
	    	    else if ( op.equals("!=") ) {
	                if ( bvalue1 != bvalue2 ) {
	                    conditionEval = true;
	                }
	    	    }
            }
            else if ( compareAsStrings || (!isValue1Integer && !isValue2Integer &&
            	!isValue1Double && !isValue2Double && !isValue1Boolean && !isValue2Boolean) ) {
            	// Always compare the string values or the input is not other types so assume strings
 	    	    if ( op.equals("CONTAINS") ) {
 	    	    	if ( value1.indexOf(value2) >= 0 ) {
 	    	    	     conditionEval = true;
 	    	    	}
 	    	    }
 	    	    else if ( op.equals("!CONTAINS") ) {
 	    	    	if ( value1.indexOf(value2) < 0 ) {
 	    	    	     conditionEval = true;
 	    	    	}
 	    	    }
 	    	    else {
 	    	    	int comp = value1.compareTo(value2);
	            	if ( op.equals("<=") ) {
	 	    	        if ( comp <= 0 ) {
	 	    	            conditionEval = true;
	 	    	        }
	 	    	    }
	 	    	    else if ( op.equals("<") ) {
	 	                if ( comp < 0 ) {
	 	                    conditionEval = true;
	 	                } 
	 	    	    }
	 	    	    else if ( op.equals(">=") ) {
	 	                if ( comp >= 0 ) {
	 	                    conditionEval = true;
	 	                }
	 	    	    }
	 	    	    else if ( op.equals(">") ) {
	 	                if ( comp > 0 ) {
	 	                    conditionEval = true;
	 	                }
	 	    	    }
	 	    	    else if ( op.equals("==") ) {
	 	                if ( comp == 0 ) {
	 	                    conditionEval = true;
	 	                }
	 	    	    }
	 	    	    else if ( op.equals("!=") ) {
	 	                if ( comp != 0 ) {
	 	                    conditionEval = true;
	 	                }
	 	    	    }
 	    	    }
            }
            else {
	            message = "Left and right have different type - cannot evaluate condition \"" + Condition + "\"";
	            Message.printWarning(3,
	                MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                routine, message );
	            status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Make sure data types on each side of operator are the same - refer to command editor and documentation." ) );
            }
    	    if ( Condition.indexOf("${") >= 0 ) {
    	        // Show the original
    	        status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.SUCCESS,
                        Condition + " (showing ${Property} notation) evaluates to " + conditionEval, "See also matching EndIf()" ) );
    	    }
    	    // Always also show the expanded
    	    status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.SUCCESS,
                    value1 + " " + op + " " + value2 + " evaluates to " + conditionEval, "See also matching EndIf()" ) );
    	    setConditionEval(conditionEval);
	    }
	    if ( (FileExists != null) && !FileExists.isEmpty() ) {
	        // Check whether a file exists - this is ANDed to the condition.
	    	String FileExists_full = IOUtil.verifyPathForOS(
	    		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	    			TSCommandProcessorUtil.expandParameterValue(processor, this, FileExists) ) );
	    	File f = new File(FileExists_full);
            if ( !f.exists() ) {
               	// Does not matter what the Condition had - the final result is false.
               	conditionEval = false;
            }
            else {
               	if ( (Condition != null) && !Condition.equals("") ) {
                   	conditionEval = conditionEval & true;
               	}
               	else {
                   	conditionEval = true;
               	}
            }
            setConditionEval(conditionEval);
	    }
	    if ( (FileDoesNotExist != null) && !FileDoesNotExist.isEmpty() ) {
	        // Check whether a file does not exist - this is ANDed to the condition.
	    	String FileDoesNotExist_full = IOUtil.verifyPathForOS(
	    		IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	    			TSCommandProcessorUtil.expandParameterValue(processor, this, FileDoesNotExist) ) );
	    	File f = new File(FileDoesNotExist_full);
            if ( f.exists() ) {
               	// Does not matter what the Condition had - the final result is false.
               	conditionEval = false;
            }
            else {
               	if ( (Condition != null) && !Condition.equals("") ) {
                   	conditionEval = conditionEval & true;
               	}
               	else {
                   	conditionEval = true;
               	}
            }
            setConditionEval(conditionEval);
	    }
	    if ( (ObjectExists != null) && !ObjectExists.isEmpty() ) {
	        // Check whether a table exists - this is ANDed to the condition.
	        // Get the table to process.  The table is searched backwards until the first match.
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
            if ( object == null ) {
               	// Does not matter what the Condition had - the final result is false.
               	conditionEval = false;
            }
            else {
               	if ( (Condition != null) && !Condition.equals("") ) {
                   	conditionEval = conditionEval & true;
               	}
               	else {
                   	conditionEval = true;
               	}
            }
            setConditionEval(conditionEval);
	    }
	    if ( (ObjectDoesNotExist != null) && !ObjectDoesNotExist.isEmpty() ) {
	        // Check whether a table exists - this is ANDed to the condition.
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
               	if ( (Condition != null) && !Condition.equals("") ) {
                   	conditionEval = conditionEval & true;
               	}
               	else {
                   	conditionEval = true;
               	}
            }
            else {
               	// Does not matter what the Condition had - the final result is false.
               	conditionEval = false;
            }
            setConditionEval(conditionEval);
	    }
	    if ( (PropertyIsNotDefinedOrIsEmpty != null) && !PropertyIsNotDefinedOrIsEmpty.isEmpty() ) {
	    	// Check to see whether the specified property exists
	    	Object o = processor.getPropContents(PropertyIsNotDefinedOrIsEmpty);
	    	conditionEval = false; // Assume property is defined and is not null
	    	if ( o == null ) {
	    		// Property is null so condition evaluates to true
	    		conditionEval = true;
	    	}
	    	else {
		    	if ( o instanceof String ) {
		    		String s = (String)o;
		    		if ( s.isEmpty() ) {
		    			// Property is empty string so condition evaluates to true
		    			conditionEval = true;
		    		}
		    	}
		    	else if ( o instanceof Double ) {
		    		Double d = (Double)o;
		    		if ( d.isNaN() ) {
		    			// Property is Double NaN so condition evaluates to true
		    			conditionEval = true;
		    		}
		    	}
		    	else if ( o instanceof Float ) {
		    		Float f = (Float)o;
		    		if ( f.isNaN() ) {
		    			// Property is Float NaN so condition evaluates to true
		    			conditionEval = true;
		    		}
		    	}
	    	}
            setConditionEval(conditionEval);
	    }
	    if ( (PropertyIsDefined != null) && !PropertyIsDefined.isEmpty() ) {
	    	// Check to see whether the specified property exists
	    	Object o = processor.getPropContents(PropertyIsDefined);
	    	conditionEval = true; // Assume property is defined and is not null
	    	if ( o == null ) {
	    		// Property is null so condition evaluates to false
	    		conditionEval = false;
	    	}
	    	else {
		    	if ( o instanceof Double ) {
		    		Double d = (Double)o;
		    		if ( d.isNaN() ) {
		    			// Property is Double NaN so condition evaluates to false
		    			conditionEval = false;
		    		}
		    	}
		    	else if ( o instanceof Float ) {
		    		Float f = (Float)o;
		    		if ( f.isNaN() ) {
		    			// Property is Float NaN so condition evaluates to false
		    			conditionEval = false;
		    		}
		    	}
	    	}
            setConditionEval(conditionEval);
	    }
	    if ( (TableExists != null) && !TableExists.isEmpty() ) {
	        // Check whether a table exists - this is ANDed to the condition.
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
               	conditionEval = false;
            }
            else {
               	if ( (Condition != null) && !Condition.equals("") ) {
                   	conditionEval = conditionEval & true;
               	}
               	else {
                   	conditionEval = true;
               	}
            }
            setConditionEval(conditionEval);
	    }
	    if ( (TableDoesNotExist != null) && !TableDoesNotExist.isEmpty() ) {
	        // Check whether a table exists - this is ANDed to the condition.
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
               	if ( (Condition != null) && !Condition.equals("") ) {
                   	conditionEval = conditionEval & true;
               	}
               	else {
                   	conditionEval = true;
               	}
            }
            else {
               	// Does not matter what the Condition had - the final result is false.
               	conditionEval = false;
            }
            setConditionEval(conditionEval);
	    }
	    if ( (TSExists != null) && !TSExists.isEmpty() ) {
	        // Check whether a time series exists - this is ANDed to the condition.
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
            PropList bean_PropList = bean.getResultsPropList();
            Object o_TS = bean_PropList.getContents ( "TS");
            if ( o_TS != null ) {
                ts = (TS)o_TS;
            }
            if ( (TSExists != null) && !TSExists.isEmpty() ) {
            	if ( ts == null ) {
                	// Does not matter what the Condition had - the final result is false.
                	conditionEval = false;
            	}
            	else {
                	if ( (Condition != null) && !Condition.equals("") ) {
                     	conditionEval = conditionEval & true;
                	}
                	else {
                    	conditionEval = true;
                	}
            	}
            }
            setConditionEval(conditionEval);
	    }
	    if ( (TSDoesNotExist != null) && !TSDoesNotExist.isEmpty() ) {
	        // Check whether a time series does not exist - this is ANDed to the condition.
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
            PropList bean_PropList = bean.getResultsPropList();
            Object o_TS = bean_PropList.getContents ( "TS");
            if ( o_TS != null ) {
                ts = (TS)o_TS;
            }
            if ( ts == null ) {
               	if ( (Condition != null) && !Condition.equals("") ) {
                    	conditionEval = conditionEval & true;
               	}
               	else {
                   	conditionEval = true;
               	}
            }
            else {
               	// Does not matter what the Condition had - the final result is false.
               	conditionEval = false;
            }
            setConditionEval(conditionEval);
	    }
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
@param conditionEval result of evaluating the condition
*/
private void setConditionEval ( boolean conditionEval ) {
    this.conditionEval = conditionEval;
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
		"FileExists",
		"FileDoesNotExist",
		"ObjectExists",
		"ObjectDoesNotExist",
		"PropertyIsNotDefinedOrIsEmpty",
		"PropertyIsDefined",
		"TableExists",
		"TableDoesNotExist",
		"TSExists",
		"TSDoesNotExist" };
	return super.toString(parameters,parameterOrder);
}

}