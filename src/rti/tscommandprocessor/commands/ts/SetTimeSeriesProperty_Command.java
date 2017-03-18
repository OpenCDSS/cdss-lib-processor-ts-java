package rti.tscommandprocessor.commands.ts;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.TS.TS;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeUtil;
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

/**
This class initializes, checks, and runs the SetTimeSeriesProperty() command.
*/
public class SetTimeSeriesProperty_Command extends AbstractCommand implements Command
{

/**
Protected data members shared with the dialog and other related classes.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Possible value for PropertyType.
*/
protected final String _DateTime = "DateTime";
protected final String _Double = "Double";
protected final String _Integer = "Integer";
protected final String _String = "String";

/**
Constructor.
*/
public SetTimeSeriesProperty_Command ()
{	super();
	setCommandName ( "SetTimeSeriesProperty" );
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
{	String Editable = parameters.getValue ( "Editable" );
    String MissingValue = parameters.getValue("MissingValue" );
    String PropertyName = parameters.getValue ( "PropertyName" );
    String PropertyType = parameters.getValue ( "PropertyType" );
    String PropertyValue = parameters.getValue ( "PropertyValue" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
    
    if ( (MissingValue != null) && !MissingValue.isEmpty() && !MissingValue.startsWith("${") && !StringUtil.isDouble(MissingValue) ) {
        message = "The missing value \"" + MissingValue + "\" is not a number."; 
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                 message, "Specify the missing value as a number." ) );
    }
	
	if ( (Editable != null) && (Editable.length() != 0) &&
        !Editable.equalsIgnoreCase(_False) && !Editable.equalsIgnoreCase(_True)) {
		message = "The value for Editable is invalid.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify blank, " + _False + " (default), or " + _True + "." ) );
	}
	
    if ( (PropertyName != null) && !PropertyName.equals("") ) {
        // Check for allowed characters...
        if ( StringUtil.containsAny(PropertyName,"${}() \t", true)) {
            message = "The property name contains invalid characters ${}() space, tab.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Specify a property name that does not include the characters $(){}, space, or tab." ) );
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
            if ( (PropertyValue.indexOf("%") >= 0) || (PropertyValue.indexOf("${") >= 0) ) {
                // Let it pass because a property will be expanded at run-time
            }
            else if ( PropertyType.equalsIgnoreCase(_DateTime) && !TimeUtil.isDateTime(PropertyValue) ) {
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
    }

	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(10);
    validList.add ( "TSList" );
	validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
	validList.add ( "Description" );
    validList.add ( "Units" );
    validList.add ( "MissingValue" );
	validList.add ( "Editable" );
    validList.add ( "PropertyName" );
    validList.add ( "PropertyType" );
    validList.add ( "PropertyValue" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
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
{	// The command will be modified if changed...
	return (new SetTimeSeriesProperty_JDialog ( parent, this )).ok();
}

// parseCommand from base class

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
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
		status.clearLog(CommandPhaseType.RUN);
	}

	PropList parameters = getCommandParameters();
	String TSList = parameters.getValue ( "TSList" );
	if ( TSList == null ) {
		TSList = TSListType.ALL_TS.toString();
	}
	String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String Description = parameters.getValue ( "Description" ); // Expanded below
    String Units = parameters.getValue ( "Units" );
	if ( (Units != null) && (Units.indexOf("${") >= 0) ) {
		Units = TSCommandProcessorUtil.expandParameterValue(processor, this, Units);
	}
    String MissingValue = parameters.getValue ( "MissingValue" );
	if ( (MissingValue != null) && (MissingValue.indexOf("${") >= 0) ) {
		MissingValue = TSCommandProcessorUtil.expandParameterValue(processor, this, MissingValue);
	}
    boolean Editable_boolean = false; // Default
    String Editable = parameters.getValue ( "Editable" );
    if ( (Editable != null) && Editable.equalsIgnoreCase(_True) ) {
        Editable_boolean = true;
    }
    String PropertyName = parameters.getValue ( "PropertyName" );
    String PropertyType = parameters.getValue ( "PropertyType" );
    String PropertyValue = parameters.getValue ( "PropertyValue" );

	// Get the time series to process...
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
	CommandProcessorRequestResultsBean bean = null;
	try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	if ( o_TSList == null ) {
		message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" +
        TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Confirm that time series are available (may be OK for partial run)." ) );
	}
	@SuppressWarnings("unchecked")
	List<TS> tslist = (List<TS>)o_TSList;
	if ( tslist.size() == 0 ) {
		message = "Zero time series in list to process using TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.WARNING,
						message, "Confirm that time series are available (may be OK for partial run)." ) );
	}
	
	// Now try to process.

    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    TS ts = null;
    for ( int i = 0; i < size; i++ ) {
        ts = tslist.get(i);
        // Now set the data...
        try {
            if ( (Description != null) && (Description.length() > 0) ) {
                ts.setDescription ( TSCommandProcessorUtil.expandTimeSeriesMetadataString (
                    processor, ts, Description, status, CommandPhaseType.RUN) );
                ts.addToGenesis ( "Set description to \"" + ts.getDescription() + "\"" );
            }
            if ( (Units != null) && (Units.length() > 0) ) {
                ts.setDataUnits ( Units );
                ts.addToGenesis ( "Set data units to \"" + ts.getDataUnits() + "\"" );
            }
            if ( (MissingValue != null) && (MissingValue.length() > 0) ) {
                ts.setMissing ( Double.parseDouble(MissingValue) );
                ts.addToGenesis ( "Set missing data value to " + MissingValue );
            }
            ts.setEditable ( Editable_boolean );
            if ( PropertyName != null ) {
                Object Property_Object = null;
                // Expand the property value to utilize %L, ${ts:property}
                PropertyValue = TSCommandProcessorUtil.expandTimeSeriesMetadataString (
                    processor, ts, PropertyValue, status, CommandPhaseType.RUN);
                Message.printStatus(2,routine,"Expanded property value=\"" + PropertyValue + "\"");
                if ( PropertyType.equalsIgnoreCase(_DateTime) ) {
                    Property_Object = DateTime.parse(PropertyValue);
                    ts.addToGenesis ( "Set property \"" + PropertyName + "\" (type " + PropertyType + ") to \"" + PropertyValue + "\"");
                }
                else if ( PropertyType.equalsIgnoreCase(_Double) ) {
                    Property_Object = Double.valueOf(PropertyValue);
                    ts.addToGenesis ( "Set property \"" + PropertyName + "\" (type " + PropertyType + ") to " + PropertyValue );
                }
                else if ( PropertyType.equalsIgnoreCase(_Integer) ) {
                    Property_Object = Integer.valueOf(PropertyValue);
                    ts.addToGenesis ( "Set property \"" + PropertyName + "\" (type " + PropertyType + ") to " + PropertyValue );
                }
                else if ( PropertyType.equalsIgnoreCase(_String) ) {
                    Property_Object = PropertyValue;
                    ts.addToGenesis ( "Set property \"" + PropertyName + "\" (type " + PropertyType + ") to \"" + PropertyValue + "\"");
                }
                ts.setProperty(PropertyName, Property_Object);
            }
        }
        catch ( Exception e ) {
            message = "Unexpected error setting property for time series \"" + ts.getIdentifier() + "\" (" + e + ").";
            Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
            Message.printWarning ( 3, routine, e );
            status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check log file for details." ) );
            throw new CommandException ( message );
        }
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
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String TSList = parameters.getValue ( "TSList" );
    String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String Description = parameters.getValue ( "Description" );
    String Units = parameters.getValue ( "Units" );
    String MissingValue = parameters.getValue("MissingValue");
    String Editable = parameters.getValue ( "Editable" );
    String PropertyName = parameters.getValue( "PropertyName" );
    String PropertyType = parameters.getValue( "PropertyType" );
    String PropertyValue = parameters.getValue( "PropertyValue" );
	StringBuffer b = new StringBuffer ();
	if ( (TSList != null) && (TSList.length() > 0) ) {
		b.append ( "TSList=" + TSList );
	}
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
    if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
    }
	if ( (Description != null) && (Description.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Description=\"" + Description + "\"" );
	}
    if ( (Units != null) && (Units.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Units=\"" + Units + "\"" );
    }
    if ( (MissingValue != null) && (MissingValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MissingValue=" + MissingValue );
    }
    if ( (Editable != null) && (Editable.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Editable=" + Editable );
    }
    if ( (PropertyName != null) && (PropertyName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PropertyName=\"" + PropertyName + "\"" );
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
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}