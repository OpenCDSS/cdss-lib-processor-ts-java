// EvaluateExpression_Command - This class initializes, checks, and runs the EvaluateExpression() command.

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

package rti.tscommandprocessor.commands.expression;

import javax.swing.JFrame;

import com.ezylang.evalex.Expression;
import com.ezylang.evalex.data.EvaluationValue;
import com.ezylang.evalex.data.EvaluationValue.DataType;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the EvaluateExpression() command.
*/
public class EvaluateExpression_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

/**
Possible value for PropertyType.
*/
protected final String _Boolean = "Boolean";
protected final String _DateTime = "DateTime";
protected final String _Double = "Double";
protected final String _Integer = "Integer";
protected final String _String = "String";

/**
Data members used for parameter values.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

/**
Property set during discovery.
*/
private Prop discoveryProp = null;

/**
Constructor.
*/
public EvaluateExpression_Command () {
	super();
	setCommandName ( "EvaluateExpression" );
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
	String Expression = parameters.getValue ( "Expression" );
	String PropertyName = parameters.getValue ( "PropertyName" );
	String PropertyType = parameters.getValue ( "PropertyType" );
    if ( PropertyType == null ) {
	    // Set to blank to be able to do checks below.
	    PropertyType = "";
    }
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (Expression == null) || Expression.equals("") ) {
        message = "The expression must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide an expression name." ) );
    }

    if ( (PropertyName == null) || PropertyName.equals("") ) {
        message = "The output property name must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide an output property name." ) );
    }
    else {
        // Check for allowed characters.
        //if ( StringUtil.containsAny(PropertyName,"${}() \t", true)) {
        if ( StringUtil.containsAny(PropertyName,"() \t", true)) {
            message = "The output property name contains invalid characters.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Specify an output property name that does not include the characters (), space, or tab." ) );
        }
    }
    if ( (PropertyType == null) || PropertyType.equals("") ) {
		// The property type is not required.
    	/*
		message = "The property type must be specified unless setting to null or property is being removed.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
             new CommandLogRecord(CommandStatusType.FAILURE,
                 message, "Provide a property value, set to null special value, or indicate to remove." ) );
                 */
    }

    // Check for invalid parameters.
	List<String> validList = new ArrayList<>(3);
    validList.add ( "Expression" );
    validList.add ( "PropertyName" );
    validList.add ( "PropertyType" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new EvaluateExpression_JDialog ( parent, this )).ok();
}

/**
Return the property defined in discovery phase.
@return the property defined in discovery phase
*/
private Prop getDiscoveryProp () {
    return this.discoveryProp;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  Prop
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    Prop discovery_Prop = getDiscoveryProp ();
    if ( discovery_Prop == null ) {
        return null;
    }
    Prop prop = new Prop();
    // Check for property request or class that matches the data.
    if ( c == prop.getClass() ) {
        List<T> v = new ArrayList<>(1);
        v.add ( (T)discovery_Prop );
        return v;
    }
    else {
        return null;
    }
}

// Use the base class parseCommand().

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
@param commandPhase The command phase that is being run (RUN or DISCOVERY).
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3; // Level for non-user messages for log file.

	CommandProcessor processor = getCommandProcessor();
	TSCommandProcessor tsprocessor = (TSCommandProcessor)processor;
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

	// Use Expression0 to not conflict with the Expression class.
	String Expression0 = parameters.getValue ( "Expression" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		Expression0 = TSCommandProcessorUtil.expandParameterValue(processor, this, Expression0);
	}
	String PropertyName = parameters.getValue ( "PropertyName" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		// Allow expansion of the property name to set dynamic names:
		// - the property name will be a string
		PropertyName = TSCommandProcessorUtil.expandParameterValue(processor, this, PropertyName);
	}
    String PropertyType = parameters.getValue ( "PropertyType" );

	try {
	    Object Property_Object = null;
   		if ( commandPhase == CommandPhaseType.RUN ) {
   			// Evaluate the expression:
	    	// - pass in the processor properties as a map
	    	Map<String,Object> propertyMap = new HashMap<>();
        	Collection<String> propertyNames = tsprocessor.getPropertyNameList(true,true);
       		boolean doIncludeProp = false;
       		Object inputPropertyValue = null;
        	for ( String propertyName : propertyNames ) {
           		//Message.printStatus(2, routine, "Adding model property " + propertyName + "=" + tsprocessor.getPropContents(propertyName));
        		doIncludeProp = false;
        		inputPropertyValue = tsprocessor.getPropContents(propertyName);
        		// Only include object types that the EvalEx knows how to handle.
        		if ( inputPropertyValue instanceof Boolean ) {
        			doIncludeProp = true;
        		}
        		else if ( inputPropertyValue instanceof Double ) {
        			doIncludeProp = true;
        		}
        		else if ( inputPropertyValue instanceof Integer ) {
        			doIncludeProp = true;
        		}
        		else if ( inputPropertyValue instanceof Long ) {
        			doIncludeProp = true;
        		}
        		else if ( inputPropertyValue instanceof String ) {
        			doIncludeProp = true;
        		}
        		if ( doIncludeProp ) {
        			propertyMap.put(propertyName, inputPropertyValue );
        		}
        	}
	    	Expression expression = new Expression(Expression0);
	    	EvaluationValue result = null;
	    	boolean evalWasSuccess = true;
	    	try {
	    		result = expression
	    		.withValues(propertyMap)
	    		.evaluate();
	    	}
	    	catch ( Exception e ) {
	    		evalWasSuccess = false;
	    		message = "Error evaluating expression (" + e + ").";
		   		Message.printWarning ( warning_level,
			   		MessageUtil.formatMessageTag(
			   		command_tag,++warning_count),routine,message );
		   		Message.printWarning(3,routine,e);
           		status.addToLog ( CommandPhaseType.RUN,
               		new CommandLogRecord(CommandStatusType.FAILURE,
                   		message, "Check the expression and input.  See the log file for details." ) );
	    	}
	    	if ( evalWasSuccess ) {
	    		// Set the value in the processor.
	    		Object propertyValue = null;
	    		DataType dataType = result.getDataType();
	    		// Handle all the types in case need to do casting below and to enable more complex types later:
	    		// - see: https://ezylang.github.io/EvalEx/concepts/datatypes.html
	    		if ( dataType == DataType.ARRAY ) {
	    			// Will be a list.
	    			propertyValue = result.getArrayValue();
	    		}
	    		else if ( dataType == DataType.BINARY ) {
	    			// java.lang.Object
	    			propertyValue = result.getValue();
	    		}
	    		else if ( dataType == DataType.BOOLEAN ) {
	    			propertyValue = result.getBooleanValue();
	    		}
	    		else if ( dataType == DataType.DATE_TIME ) {
	    			// Convert java.time.Instant to a DateTime:
	    			// - TODO smalers 2025-04-15 need to enable, dealing with time zone, etc.
	    			//propertyValue = new DateTime(result.getDateTimeValue());
	    			propertyValue = result.getDateTimeValue();
	    		}
	    		else if ( dataType == DataType.DURATION ) {
	    			// java.time.Duration
	    			propertyValue = result.getDurationValue();
	    		}
	    		else if ( dataType == DataType.EXPRESSION_NODE ) {
	    			// Expression node:
	    			// - not sure whether this is needed
	    		}
	    		else if ( dataType == DataType.NULL ) {
	    			propertyValue = null;
	    		}
	    		else if ( dataType == DataType.NUMBER ) {
	    			// Convert BigDecimal to a Double.
	    			propertyValue = Double.valueOf(result.getNumberValue().doubleValue());
	    		}
	    		else if ( dataType == DataType.STRING ) {
	    			propertyValue = result.getStringValue();
	    		}
	    		else if ( dataType == DataType.STRUCTURE ) {
	    			// java.util.Map
	    			propertyValue = result.getStructureValue();
	    		}
	    		else {
	    			// Unknown:
	    			// - treat as java.lang.Object
	    			propertyValue = result.getValue();
	    		}
	    		Message.printStatus(2, routine, "The expression result is " + propertyValue);
	    		if ( (PropertyType == null) || PropertyType.isEmpty() ) {
	    			// No property type was specified so just set the result without checking the type.
	    			Property_Object = propertyValue;
	    		}
	    		else {
	    			// Property type was requested so set:
	    			// - not currently handled
	    			/*
			   		if ( PropertyType.equalsIgnoreCase(_Boolean) ) {
			   			if ( !setNull ) {
			   				Property_Object = Boolean.valueOf(propertyValue);
			   			}
			   		}
			   		else if ( PropertyType.equalsIgnoreCase(_DateTime) ) {
			       		// This handles special strings like CurrentToHour.
			        	// Have to specify a PropList to ensure the special syntax is handled.
			    		// TODO SAM 2016-09-18 consider whether parsing should recognize in-memory DateTime properties.
			    		if ( !setNull ) {
			    			Property_Object = DateTime.parse(propertyValue,(PropList)null);
			    			if ( (Add != null) && !Add.isEmpty() ) {
			    				DateTime dt = (DateTime)Property_Object;
			    				TimeInterval interval = TimeInterval.parseInterval(Add);
			    				dt.addInterval(interval.getBase(), interval.getMultiplier());
			    			}
			    			else if ( (Subtract != null) && !Subtract.isEmpty() ) {
			    				DateTime dt = (DateTime)Property_Object;
			    				TimeInterval interval = TimeInterval.parseInterval(Subtract);
			    				dt.addInterval(interval.getBase(), -interval.getMultiplier());
			    			}
			    			Message.printStatus(2, routine, "Parsed date/time from '" + propertyValue + "': " + Property_Object);
			    		}
			    	}
			    	else if ( PropertyType.equalsIgnoreCase(_Double) ) {
			    		if ( setNaN ) {
			    			Property_Object = Double.NaN;
			    		}
			    		else if ( !setNull ) {
			    			// Set a number value.
			    			Property_Object = Double.valueOf(propertyValue);
			    			// Do math operations if specified.
			    			if ( (Add != null) && !Add.isEmpty() ) {
			    				Double d = (Double)Property_Object;
			    				Property_Object = Double.valueOf(d.doubleValue() + Double.parseDouble(Add));
			    			}
			    			else if ( (Subtract != null) && !Subtract.isEmpty() ) {
			    				Double d = (Double)Property_Object;
			    				Property_Object = Double.valueOf(d.doubleValue() - Double.parseDouble(Subtract));
			    			}
			    			else if ( (Multiply != null) && !Multiply.isEmpty() ) {
			    				Double d = (Double)Property_Object;
			    				Property_Object = Double.valueOf(d.doubleValue()*Double.parseDouble(Multiply));
			    			}
			    			else if ( (Divide != null) && !Divide.isEmpty() ) {
			    				Double d = (Double)Property_Object;
			    				if ( d == 0.0 ) {
			    					Property_Object = Double.NaN;
			    					// TODO sam 2017-03-25 should this throw an exception?
			    				}
			    				else {
			    					Property_Object = Double.valueOf(d.doubleValue()/Double.parseDouble(Divide));
			    				}
			    			}
			    		}
			    	}
			    	else if ( PropertyType.equalsIgnoreCase(_Integer) ) {
			    		if ( !setNull ) {
			    			Property_Object = Integer.valueOf(propertyValue);
			    			// Do math operations if specified.
			    			if ( (Add != null) && !Add.isEmpty() ) {
			    				Integer i = (Integer)Property_Object;
			    				Property_Object = Integer.valueOf(i.intValue() + Integer.parseInt(Add));
			    			}
			    			else if ( (Subtract != null) && !Subtract.isEmpty() ) {
			    				Integer i = (Integer)Property_Object;
			    				Property_Object = Integer.valueOf(i.intValue() - Integer.parseInt(Subtract));
			    			}
			    			else if ( (Multiply != null) && !Multiply.isEmpty() ) {
			    				Integer i = (Integer)Property_Object;
			    				Property_Object = Integer.valueOf(i.intValue()*Integer.parseInt(Multiply));
			    			}
			    			else if ( (Divide != null) && !Divide.isEmpty() ) {
			    				Integer i = (Integer)Property_Object;
			    				if ( i == 0 ) {
			    					Property_Object = null;
			    					// TODO sam 2017-03-25 evaluate whether to throw an exception.
			    				}
			    				else {
			    					Property_Object = Integer.valueOf(i.intValue()/Integer.parseInt(Divide));
			    				}
			    			}
			    		}
			    	}
			    	else if ( PropertyType.equalsIgnoreCase(_String) ) {
			    		if ( setEmpty ) {
			    			Property_Object = "";
			    		}
			    		else if ( !setNull ) {
			    			Property_Object = propertyValue;
			    			// Do math operations if specified.
			    			if ( (Add != null) && !Add.isEmpty() ) {
			    				// Concatenate.
			    				String s = (String)Property_Object;
			    				Property_Object = s + Add;
			    			}
			    			else if ( (Multiply != null) && !Multiply.isEmpty() ) {
			    				// Repeat the original string.
			    				String s = (String)Property_Object;
			    				StringBuilder b = new StringBuilder();
			    				Double count = Double.parseDouble(Multiply);
			    				for ( int i = 0; i < count; i++ ) {
			    					b.append(s);
			    				}
			    				Property_Object = b.toString();
			    			}
			    			else if ( (Subtract != null) && !Subtract.isEmpty() ) {
			    				// Remove the string.
			    				String s = (String)Property_Object;
			    				Property_Object = s.replace(Subtract,"");
			    			}
			    		}
			    		Message.printStatus(2,routine,"Setting string property to \"" + Property_Object + "\"");
			    	}
			    	*/
	    		}

    			// Set the property in the processor.

    			PropList request_params = new PropList ( "" );
    			request_params.setUsingObject ( "PropertyName", PropertyName );
    			request_params.setUsingObject ( "PropertyValue", Property_Object );
    			try {
            		processor.processRequest( "SetProperty", request_params);
    			}
    			catch ( Exception e ) {
    				message = "Error calling SetProperty(Property=\"" + PropertyName + "\") in the processor (" + e + ").";
    				Message.printWarning(log_level,
    						MessageUtil.formatMessageTag( command_tag, ++warning_count),
    						routine, message );
            		Message.printWarning(log_level,routine,e);
            		status.addToLog ( CommandPhaseType.RUN,
                    		new CommandLogRecord(CommandStatusType.FAILURE,
                            		message, "Report the problem to software support." ) );
    			}
    		}
		}
    	else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
    		// Default is string but checks should warn about unknown type.
    		Property_Object = null;
    		Prop prop = new Prop(PropertyName, Property_Object, null);
        	prop.setHowSet(Prop.SET_UNKNOWN);
    		setDiscoveryProp ( prop );
    	}
	}
	catch ( Exception e ) {
		message = "Unexpected error evaluating expression (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "See the log file for details - report the problem to software support." ) );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
		throw new CommandWarningException ( message );
	}
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the property defined in discovery phase.
@param prop Property set during discovery phase.
*/
private void setDiscoveryProp ( Prop prop ) {
    this.discoveryProp = prop;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"Expression",
    	"PropertyName",
		"PropertyType"
	};
	return this.toString(parameters, parameterOrder);
}

}