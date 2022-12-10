// SetProperty_Command - This class initializes, checks, and runs the SetProperty() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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
import java.util.List;
import java.util.Vector;

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
This class initializes, checks, and runs the SetProperty() command.
*/
public class SetProperty_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

/**
Possible value for several boolean parameters.
*/
protected final String _True = "True";

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
private Prop __discovery_Prop = null;

/**
Constructor.
*/
public SetProperty_Command ()
{	super();
	setCommandName ( "SetProperty" );
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
{	String PropertyName = parameters.getValue ( "PropertyName" );
	String PropertyType = parameters.getValue ( "PropertyType" );
    if ( PropertyType == null ) {
	    // Set to blank to be able to do checks below.
	    PropertyType = "";
    }
	String PropertyValue = parameters.getValue ( "PropertyValue" );
	String EnvironmentVariable = parameters.getValue ( "EnvironmentVariable" );
	String JavaProperty = parameters.getValue ( "JavaProperty" );
	String IfJavaPropertyUndefined = parameters.getValue ( "IfJavaPropertyUndefined" );
	String SetEmpty = parameters.getValue ( "SetEmpty" );
	String SetNaN = parameters.getValue ( "SetNaN" );
	String SetNull = parameters.getValue ( "SetNull" );
	String RemoveProperty = parameters.getValue ( "RemoveProperty" );
	String Add = parameters.getValue ( "Add" );
	String Subtract = parameters.getValue ( "Subtract" );
	String Multiply = parameters.getValue ( "Multiply" );
	String Divide = parameters.getValue ( "Divide" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (PropertyName == null) || PropertyName.equals("") ) {
        message = "The property name must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide a property name." ) );
    }
    else {
        // Check for allowed characters.
        if ( StringUtil.containsAny(PropertyName,"${}() \t", true)) {
            message = "The property name contains invalid characters.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Specify a property name that does not include the characters $(){}, space, or tab." ) );
        }
    }
    if ( (PropertyType == null) || PropertyType.equals("") ) {
		// The property type is not required for some special values that are independent of type.
		if ( ((SetNull == null) || SetNull.isEmpty()) &&
			((RemoveProperty == null) || RemoveProperty.isEmpty()) ) {
			message = "The property type must be specified unless setting to null or property is being removed.";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Provide a property value, set to null special value, or indicate to remove." ) );
		}
    }
    int setCount = 0;
    String method = "";
	if ( (PropertyValue != null) && !PropertyValue.isEmpty() ) {
		++setCount;
		method += " PropertyValue";
	}
	if ( (EnvironmentVariable != null) && !EnvironmentVariable.isEmpty() ) {
		++setCount;
		method += " EnvironmentVariable";
	}
	if ( (JavaProperty != null) && !JavaProperty.isEmpty() ) {
		++setCount;
		method += " JavaProperty";
	}
	if ( (SetEmpty != null) && !SetEmpty.isEmpty() ) {
		++setCount;
		method += " SetEmpty";
	}
	if ( (SetNaN != null) && !SetNaN.isEmpty() ) {
		++setCount;
		method += " SetNaN";
	}
	if ( (SetNull != null) && !SetNull.isEmpty() ) {
		++setCount;
		method += " SetNull";
	}
	if ( (RemoveProperty != null) && !RemoveProperty.isEmpty() ) {
		++setCount;
		method += " RemoveProperty";
	}
	if ( setCount == 0 ) {
		message = "The property value must be set only one way: value, environment variable, "
				+ "Java property, special value, or remove.";
	    warning += "\n" + message;
	    status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the property value using one of the available choices." ) );
	}
	else if ( setCount > 1 ) {
		message = "The property value must be set only one way: value, environment variable, "
				+ "Java property, special value, or remove (currently using: " + method + ").";
	    warning += "\n" + message;
	    status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a property value using one of the available choices." ) );
	}
	if ( (PropertyValue != null) && !PropertyValue.isEmpty() && (PropertyValue.indexOf("${") < 0) ) {
	    // Check the property value given the type.
	    PropertyValue = TSCommandProcessorUtil.expandParameterValue(getCommandProcessor(), this, PropertyValue);
	    if ( PropertyType.equalsIgnoreCase(_Boolean) && !PropertyValue.equalsIgnoreCase("true") && !PropertyValue.equalsIgnoreCase("false") ) {
    		message = "The property value \"" + PropertyValue + "\" is not a boolean.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the property value as a boolean True or False" ));
		}
	    else if ( PropertyType.equalsIgnoreCase(_DateTime) ) {
	        // Try parsing because the parse method recognizes the special values CurrentToHour, etc.
	        try {
	            // This handles special syntax like "CurrentToHour" and "CurrentToHour - 6Hour".
	            DateTime.parse(PropertyValue, null );
	        }
	        catch ( Exception e ) {
	            message = "The property value \"" + PropertyValue + "\" is not a valid date/time.";
	            warning += "\n" + message;
	            status.addToLog ( CommandPhaseType.INITIALIZATION,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Specify the property value as a date/time (e.g., YYYY-MM-DD if a date)" ));
	        }
	    }
	    else if ( PropertyType.equalsIgnoreCase(_Double) && !StringUtil.isDouble(PropertyValue) ) {
    		message = "The property value \"" + PropertyValue + "\" is not a number.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the property value as a number" ));
		}
		else if ( PropertyType.equalsIgnoreCase(_Integer) && !StringUtil.isInteger(PropertyValue) ) {
            message = "The property value \"" + PropertyValue + "\" is not an integer";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the property value as an integer." ));
		}
	}

	if ( (IfJavaPropertyUndefined != null) && !IfJavaPropertyUndefined.isEmpty() ) {
		if ( !IfJavaPropertyUndefined.equalsIgnoreCase(_Ignore) && !IfJavaPropertyUndefined.equalsIgnoreCase(_Warn)
		    && !IfJavaPropertyUndefined.equalsIgnoreCase(_Fail) ) {
			message = "The IfJavaPropertyUndefined parameter \"" + IfJavaPropertyUndefined + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Ignore + ", " + _Warn + " (default), or " +
					_Fail + "."));
		}
	}
	
	if ( (SetEmpty != null) && !SetEmpty.isEmpty() && !SetEmpty.equalsIgnoreCase(_True)) {
		message = "The SetEmpty parameter \"" + SetEmpty + "\" is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the parameter as " + _True + " or blank (default)."));
	}
	
	if ( (SetNaN != null) && !SetNaN.isEmpty() && !SetNaN.equalsIgnoreCase(_True)) {
		message = "The SetNaN parameter \"" + SetNaN + "\" is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the parameter as " + _True + " or blank (default)."));
	}
	
	if ( (SetNull != null) && !SetNull.isEmpty() && !SetNull.equalsIgnoreCase(_True)) {
		message = "The SetNull parameter \"" + SetNull + "\" is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the parameter as " + _True + " or blank (default)."));
	}
	
	if ( (RemoveProperty != null) && !RemoveProperty.isEmpty() && !RemoveProperty.equalsIgnoreCase(_True)) {
		message = "The RemoveProperty parameter \"" + RemoveProperty + "\" is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the parameter as " + _True + " or blank (default)."));
	}
	
	if ( (Add != null) && !Add.isEmpty() && (Add.indexOf("${") < 0) ) {
		if ( PropertyType.equalsIgnoreCase(_Double) && !StringUtil.isDouble(Add) ) {
			message = "The Add parameter \"" + Add + "\" is invalid for " + _Double + " property type.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as a number."));
		}
		if ( PropertyType.equalsIgnoreCase(_Integer) && !StringUtil.isDouble(Add) ) {
			message = "The Add parameter \"" + Add + "\" is invalid for " + _Integer + " property type.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as an integer."));
		}
	}
	
	if ( (Subtract != null) && !Subtract.isEmpty() && (Subtract.indexOf("${") < 0) ) {
		if ( PropertyType.equalsIgnoreCase(_Double) && !StringUtil.isDouble(Subtract) ) {
			message = "The Subtract parameter \"" + Subtract + "\" is invalid for " + _Double + " property type.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as a number."));
		}
		if ( PropertyType.equalsIgnoreCase(_Integer) && !StringUtil.isDouble(Subtract) ) {
			message = "The Subtract parameter \"" + Subtract + "\" is invalid for " + _Integer + " property type.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as an integer."));
		}
	}
	
	if ( (Multiply != null) && !Multiply.isEmpty() && (Multiply.indexOf("${") < 0) ) {
		if ( PropertyType.equalsIgnoreCase(_Double) && !StringUtil.isDouble(Multiply) ) {
			message = "The Subtract parameter \"" + Subtract + "\" is invalid for " + _Double + " property type.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as a number."));
		}
		if ( PropertyType.equalsIgnoreCase(_Integer) && !StringUtil.isDouble(Multiply) ) {
			message = "The Multiply parameter \"" + Subtract + "\" is invalid for " + _Integer + " property type.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as an integer."));
		}
	}
	
	if ( (Divide != null) && !Divide.isEmpty() && (Divide.indexOf("${") < 0) ) {
		if ( PropertyType.equalsIgnoreCase(_Double) && !StringUtil.isDouble(Divide) ) {
			message = "The Divide parameter \"" + Divide + "\" is invalid for " + _Double + " property type.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as a number."));
		}
		if ( PropertyType.equalsIgnoreCase(_Integer) && !StringUtil.isDouble(Divide) ) {
			message = "The Divide parameter \"" + Divide + "\" is invalid for " + _Integer + " property type.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as an integer."));
		}
	}
    
    // Check for invalid parameters.
	List<String> validList = new ArrayList<>(14);
    validList.add ( "PropertyName" );
    validList.add ( "PropertyType" );
    validList.add ( "PropertyValue" );
    validList.add ( "EnvironmentVariable" );
    validList.add ( "JavaProperty" );
    validList.add ( "IfJavaPropertyUndefined" );
    validList.add ( "SetEmpty" );
    validList.add ( "SetNaN" );
    validList.add ( "SetNull" );
    validList.add ( "RemoveProperty" );
    validList.add ( "Add" );
    validList.add ( "Subtract" );
    validList.add ( "Multiply" );
    validList.add ( "Divide" );
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
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed.
	return (new SetProperty_JDialog ( parent, this )).ok();
}

/**
Return the property defined in discovery phase.
*/
private Prop getDiscoveryProp () {
    return __discovery_Prop;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  Prop
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{   Prop discovery_Prop = getDiscoveryProp ();
    if ( discovery_Prop == null ) {
        return null;
    }
    Prop prop = new Prop();
    // Check for TS request or class that matches the data.
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
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3; // Level for non-user messages for log file.

	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // Default.
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

	String PropertyName = parameters.getValue ( "PropertyName" );
    String PropertyType = parameters.getValue ( "PropertyType" );
	String PropertyValue = parameters.getValue ( "PropertyValue" );
	if ( (commandPhase == CommandPhaseType.RUN) && (PropertyValue != null) && (PropertyValue.indexOf("${") >= 0) ) {
		PropertyValue = TSCommandProcessorUtil.expandParameterValue(processor, this, PropertyValue);
	}
	String EnvironmentVariable = parameters.getValue ( "EnvironmentVariable" );
	if ( (commandPhase == CommandPhaseType.RUN) && (EnvironmentVariable != null) && (EnvironmentVariable.indexOf("${") >= 0) ) {
		EnvironmentVariable = TSCommandProcessorUtil.expandParameterValue(processor, this, EnvironmentVariable);
	}
	String JavaProperty = parameters.getValue ( "JavaProperty" );
	if ( (commandPhase == CommandPhaseType.RUN) && (JavaProperty != null) && (JavaProperty.indexOf("${") >= 0) ) {
		JavaProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, JavaProperty);
	}
	String IfJavaPropertyUndefined = parameters.getValue ( "IfJavaPropertyUndefined" );
	if ( (IfJavaPropertyUndefined == null) || IfJavaPropertyUndefined.isEmpty() ) {
		IfJavaPropertyUndefined = this._Warn; // Default.
	}
	String SetEmpty = parameters.getValue ( "SetEmpty" );
	boolean setEmpty = false;
	if ( (SetEmpty != null) && SetEmpty.equalsIgnoreCase(_True) ) {
		setEmpty = true;
	}
	String SetNaN = parameters.getValue ( "SetNaN" );
	boolean setNaN = false;
	if ( (SetNaN != null) && SetNaN.equalsIgnoreCase(_True) ) {
		setNaN = true;
	}
	String SetNull = parameters.getValue ( "SetNull" );
	boolean setNull = false;
	if ( (SetNull != null) && SetNull.equalsIgnoreCase(_True) ) {
		setNull = true;
	}
	String RemoveProperty = parameters.getValue ( "RemoveProperty" );
	boolean removeProperty = false;
	if ( (RemoveProperty != null) && RemoveProperty.equalsIgnoreCase(_True) ) {
		removeProperty = true;
	}
	// Treat as strings until the math operation is executed and type is handled.
	String Add = parameters.getValue ( "Add" );
	if ( (commandPhase == CommandPhaseType.RUN) && (Add != null) && (Add.indexOf("${") >= 0) ) {
		Add = TSCommandProcessorUtil.expandParameterValue(processor, this, Add);
	}
	String Subtract = parameters.getValue ( "Subtract" );
	if ( (commandPhase == CommandPhaseType.RUN) && (Subtract != null) && (Subtract.indexOf("${") >= 0) ) {
		Subtract = TSCommandProcessorUtil.expandParameterValue(processor, this, Subtract);
	}
	String Multiply = parameters.getValue ( "Multiply" );
	if ( (commandPhase == CommandPhaseType.RUN) && (Multiply != null) && (Multiply.indexOf("${") >= 0) ) {
		Multiply = TSCommandProcessorUtil.expandParameterValue(processor, this, Multiply);
	}
	String Divide = parameters.getValue ( "Divide" );
	if ( (commandPhase == CommandPhaseType.RUN) && (Divide != null) && (Divide.indexOf("${") >= 0) ) {
		Divide = TSCommandProcessorUtil.expandParameterValue(processor, this, Divide);
	}

	try {
	    Object Property_Object = null; // Important to initialize to null because may be using setNull.
	    if ( removeProperty ) {
	    	// Unset/remove property from the processor.
	    	// Only do in run mode for now because not sure of implications in discovery mode.
	    	// TODO SAM 2016-09-18 Evaluate whether a warning should be given if the property is not found.
		    if ( commandPhase == CommandPhaseType.RUN ) {
		    	PropList request_params = new PropList ( "" );
		    	request_params.setUsingObject ( "PropertyName", PropertyName );
		    	try {
		            processor.processRequest( "RemoveProperty", request_params);
		    	}
		    	catch ( Exception e ) {
		    		message = "Error requesting RemoveProperty(Property=\"" + PropertyName + "\") from processor (" + e + ").";
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
	    else {
	    	// Not setting to null so expect to have one of:
	    	// - property value
	    	// - environment variable
	    	// - Java property
	    	// - special value
	    	String propertyValue = null;
	    	if ( (PropertyValue != null) && !PropertyValue.isEmpty() ) {
	    		// Set the property value from PropertyValue.
	    		propertyValue = PropertyValue;
	    	}
	    	else if ( (EnvironmentVariable != null) && !EnvironmentVariable.isEmpty() ) {
	    		// Set the property value from EnvironmentVariable.
	    		propertyValue = System.getenv(EnvironmentVariable);
	    		if ( propertyValue == null ) {
		    		message = "Environment variable \"" + EnvironmentVariable + "\" is not set.";
		    		Message.printWarning(log_level,
	    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
	    				routine, message );
		            status.addToLog ( CommandPhaseType.RUN,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Check that environment variable \"" + EnvironmentVariable + "\" is set." ) );
	    		}
	    	}
	    	else if ( (JavaProperty != null) && !JavaProperty.isEmpty() ) {
	    		// Set the property value from JavaProperty.
	    		propertyValue = System.getProperty(JavaProperty);
	    		if ( propertyValue == null ) {
		    		message = "Java property \"" + JavaProperty + "\" is not defined.";
            		if ( IfJavaPropertyUndefined.equalsIgnoreCase(_Fail) ) {
                		Message.printWarning ( warning_level,
                    		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                		status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                        		message, "Check that Java property \"" + JavaProperty + "\" is defined."));
            		}
            		else if ( IfJavaPropertyUndefined.equalsIgnoreCase(_Warn) ) {
                		Message.printWarning ( warning_level,
                    		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                		status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
                        		message, "Check that Java property \"" + JavaProperty + "\" is defined."));
            		}
            		else {
                		Message.printStatus( 2, routine, message + "  Ignoring.");
            		}
	    		}
	    	}
	    	// Now have the property value.  Set as a property in the processor.
	    	if ( commandPhase == CommandPhaseType.RUN ) {
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
			    		Property_Object = new Double(Double.NaN);
			    	}
			    	else if ( !setNull ) {
			    		// Set a number value.
			    		Property_Object = Double.valueOf(propertyValue);
			    		// Do math operations if specified.
			    		if ( (Add != null) && !Add.isEmpty() ) {
			    			Double d = (Double)Property_Object;
			    			Property_Object = new Double(d.doubleValue() + Double.parseDouble(Add));
			    		}
			    		else if ( (Subtract != null) && !Subtract.isEmpty() ) {
			    			Double d = (Double)Property_Object;
			    			Property_Object = new Double(d.doubleValue() - Double.parseDouble(Subtract));
			    		}
			    		else if ( (Multiply != null) && !Multiply.isEmpty() ) {
			    			Double d = (Double)Property_Object;
			    			Property_Object = new Double(d.doubleValue()*Double.parseDouble(Multiply));
			    		}
			    		else if ( (Divide != null) && !Divide.isEmpty() ) {
			    			Double d = (Double)Property_Object;
			    			if ( d == 0.0 ) {
			    				Property_Object = Double.NaN;
			    				// TODO sam 2017-03-25 should this throw an exception?
			    			}
			    			else {
			    				Property_Object = new Double(d.doubleValue()/Double.parseDouble(Divide));
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
			    			Property_Object = new Integer(i.intValue() + Integer.parseInt(Add));
			    		}
			    		else if ( (Subtract != null) && !Subtract.isEmpty() ) {
			    			Integer i = (Integer)Property_Object;
			    			Property_Object = new Integer(i.intValue() - Integer.parseInt(Subtract));
			    		}
			    		else if ( (Multiply != null) && !Multiply.isEmpty() ) {
			    			Integer i = (Integer)Property_Object;
			    			Property_Object = new Integer(i.intValue()*Integer.parseInt(Multiply));
			    		}
			    		else if ( (Divide != null) && !Divide.isEmpty() ) {
			    			Integer i = (Integer)Property_Object;
			    			if ( i == 0 ) {
			    				Property_Object = null;
			    				// TODO sam 2017-03-25 evaluate whether to throw an exception.
			    			}
			    			else {
			    				Property_Object = new Integer(i.intValue()/Integer.parseInt(Divide));
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
		    
		    	// Set the property in the processor.
		    
		    	PropList request_params = new PropList ( "" );
		    	request_params.setUsingObject ( "PropertyName", PropertyName );
		    	request_params.setUsingObject ( "PropertyValue", Property_Object );
		    	if ( setNull ) {
		    		request_params.setUsingObject ( "SetNull", Boolean.TRUE );
		    	}
		    	try {
		            processor.processRequest( "SetProperty", request_params);
		    	}
		    	catch ( Exception e ) {
		    		message = "Error requesting SetProperty(Property=\"" + PropertyName + "\") from processor (" + e + ").";
		    		Message.printWarning(log_level,
		    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
		    				routine, message );
		            Message.printWarning(log_level,routine,e);
		            status.addToLog ( CommandPhaseType.RUN,
		                    new CommandLogRecord(CommandStatusType.FAILURE,
		                            message, "Report the problem to software support." ) );
		    	}
	    	}
	    	else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	    		// TODO sam 2017-03-18 evaluate whether this is appropriate for discovery mode:
	    		// -the problem is ${Property} notation breaks conversions above
	    		//setDiscoveryProp ( new Prop(PropertyName,Property_Object,"" + Property_Object ) );
	    		// Set the property type according to the request so that code that asks for discovery.
	    		// objects and then filters on type will have the correct type.
	    		if ( PropertyType.equalsIgnoreCase(_Boolean) ) {
	    			Property_Object = new Boolean(true);
	    		}
	    		else if ( PropertyType.equalsIgnoreCase(_DateTime) ) {
	    			Property_Object = new DateTime(DateTime.DATE_CURRENT);
	    		}
	    		else if ( PropertyType.equalsIgnoreCase(_Double) ) {
	    			Property_Object = new Double(1.0);
	    		}
	    		else if ( PropertyType.equalsIgnoreCase(_Integer) ) {
	    			Property_Object = new Integer(1);
	    		}
	    		else {
	    			Property_Object = "";
	    		}
	    		Prop prop = new Prop(PropertyName, Property_Object, PropertyValue);
	            prop.setHowSet(Prop.SET_UNKNOWN);
	    		setDiscoveryProp ( prop );
	    	}
	    }
	}
	catch ( Exception e ) {
		message = "Unexpected error setting property \""+ PropertyName + "\"=\"" + PropertyValue + "\" (" + e + ").";
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
    __discovery_Prop = prop;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String PropertyName = props.getValue( "PropertyName" );
	String PropertyType = props.getValue( "PropertyType" );
    String PropertyValue = props.getValue( "PropertyValue" );
    String EnvironmentVariable = props.getValue( "EnvironmentVariable" );
    String JavaProperty = props.getValue( "JavaProperty" );
    String IfJavaPropertyUndefined = props.getValue( "IfJavaPropertyUndefined" );
    String SetEmpty = props.getValue ( "SetEmpty" );
    String SetNaN = props.getValue ( "SetNaN" );
    String SetNull = props.getValue ( "SetNull" );
    String RemoveProperty = props.getValue ( "RemoveProperty" );
    String Add = props.getValue ( "Add" );
    String Subtract = props.getValue ( "Subtract" );
    String Multiply = props.getValue ( "Multiply" );
    String Divide = props.getValue ( "Divide" );
	StringBuffer b = new StringBuffer ();
    if ( (PropertyName != null) && (PropertyName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PropertyName=\"" + PropertyName + "\"" );
    }
    if ( (PropertyType != null) && (PropertyType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PropertyType=" + PropertyType );
    }
	if ( (PropertyValue != null) && (PropertyValue.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "PropertyValue=\"" + PropertyValue + "\"" );
	}
	if ( (EnvironmentVariable != null) && (EnvironmentVariable.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "EnvironmentVariable=\"" + EnvironmentVariable + "\"" );
	}
	if ( (JavaProperty != null) && (JavaProperty.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "JavaProperty=\"" + JavaProperty + "\"" );
	}
	if ( (IfJavaPropertyUndefined != null) && (IfJavaPropertyUndefined.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfJavaPropertyUndefined=\"" + IfJavaPropertyUndefined + "\"" );
	}
	if ( (SetEmpty != null) && (SetEmpty.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SetEmpty=" + SetEmpty );
	}
	if ( (SetNaN != null) && (SetNaN.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SetNaN=" + SetNaN );
	}
	if ( (SetNull != null) && (SetNull.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SetNull=" + SetNull );
	}
	if ( (RemoveProperty != null) && (RemoveProperty.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "RemoveProperty=" + RemoveProperty );
	}
	if ( (Add != null) && (Add.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Add=\"" + Add + "\"" ); // Need quotes because string could have whitespace.
	}
	if ( (Subtract != null) && (Subtract.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Subtract=\"" + Subtract + "\"" ); // Need quotes because string could have whitespace.
	}
	if ( (Multiply != null) && (Multiply.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Multiply=" + Multiply );
	}
	if ( (Divide != null) && (Divide.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Divide=" + Divide );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}