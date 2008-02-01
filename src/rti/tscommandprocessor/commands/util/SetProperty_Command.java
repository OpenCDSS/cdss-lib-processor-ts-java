package rti.tscommandprocessor.commands.util;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSUtil;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeUtil;

/**
<p>
This class initializes, checks, and runs the SetProperty() command.
</p>
*/
public class SetProperty_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Possible value for ScaleValue, when not a number.
*/
protected final String _DateTime = "DateTime";
protected final String _Double = "Double";
protected final String _Integer = "Integer";
protected final String _String = "String";

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
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor
dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String PropertyName = parameters.getValue ( "PropertyName" );
	String PropertyType = parameters.getValue ( "PropertyType" );
	String PropertyValue = parameters.getValue ( "PropertyValue" );
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
        // Check for allowed characters...
        if ( StringUtil.containsAny(PropertyName,"${}() \t", true)) {
            message = "The property name cannot contains invalid characters.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Specify a property name that does not include the characters $(){}, space, or tab." ) );
        }
    }
    if ( (PropertyType == null) || PropertyType.equals("") ) {
        message = "The property type must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide a property type." ) );
        // Set to blank to be able to do checks below
        PropertyType = "";
    }
	if ( (PropertyValue == null) || PropertyValue.equals("") ) {
        message = "The property value must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide a property value." ) );
	}
	else {
	    // Check the value given the type.
	    if ( PropertyType.equalsIgnoreCase(_DateTime) && !TimeUtil.isDateTime(PropertyValue) ) {
	        message = "The property value \"" + PropertyValue + "\" is not a valid date/time.";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Specify the property value as a date/time (e.g., YYYY-MM-DD if a date)" ));
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
    
    // Check for invalid parameters...
    Vector valid_Vector = new Vector();
    valid_Vector.add ( "PropertyName" );
    valid_Vector.add ( "PropertyType" );
    valid_Vector.add ( "PropertyValue" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );
    
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new SetProperty_JDialog ( parent, this )).ok();
}

/**
Return the property defined in discovery phase.
*/
private Prop getDiscoveryProp ()
{
    return __discovery_Prop;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
    Prop discovery_Prop = getDiscoveryProp ();
    if ( discovery_Prop == null ) {
        return null;
    }
    Prop prop = new Prop();
    // Check for TS request or class that matches the data...
    if ( c == prop.getClass() ) {
        Vector v = new Vector (1);
        v.add ( discovery_Prop );
        return v;
    }
    else {
        return null;
    }
}

// Use the base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
ScaleValue can be "DaysInMonth" or "DaysInMonthInverse".
@param command_number Number of command in sequence.
@param command_phase The command phase that is being run (RUN or DISCOVERY).
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "SetProperty_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;  // Level for non-use messages for log file.

	// Make sure there are time series available to operate on...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String PropertyName = parameters.getValue ( "PropertyName" );
    String PropertyType = parameters.getValue ( "PropertyType" );
	String PropertyValue = parameters.getValue ( "PropertyValue" );
	
	try {

	    Object Property_Object = null;
	    if ( PropertyType.equalsIgnoreCase(_DateTime) ) {
	        Property_Object = DateTime.parse(PropertyValue);
	    }
	    else if ( PropertyType.equalsIgnoreCase(_Double) ) {
	        Property_Object = Double.valueOf(PropertyValue);
	    }
	    else if ( PropertyType.equalsIgnoreCase(_Integer) ) {
	        Property_Object = Integer.valueOf(PropertyValue);
	    }
	    else if ( PropertyType.equalsIgnoreCase(_String) ) {
	        Property_Object = PropertyValue;
	    }
	    
    	// Set the property in the processor
    
    	PropList request_params = new PropList ( "" );
    	request_params.setUsingObject ( "PropertyName", PropertyName );
    	request_params.setUsingObject ( "PropertyValue", Property_Object );
    	try {
            processor.processRequest( "SetProperty", request_params);
            // Set the 
            if ( command_phase == CommandPhaseType.DISCOVERY ) {
                setDiscoveryProp ( new Prop(PropertyName,Property_Object,"" + Property_Object ) );
            }
    	}
    	catch ( Exception e ) {
    		message = "Error requesting SetProperty(Property=\"" + PropertyName + "\") from processor.";
    		Message.printWarning(log_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
    	}
	}
	catch ( Exception e ) {
		message = "Unexpected error setting property \""+ PropertyName + "=" + PropertyValue;
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
private void setDiscoveryProp ( Prop prop )
{
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
	return getCommandName() + "(" + b.toString() + ")";
}

}
