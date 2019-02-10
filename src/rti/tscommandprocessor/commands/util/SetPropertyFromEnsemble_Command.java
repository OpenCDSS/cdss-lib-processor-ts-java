// SetPropertyFromEnsemble_Command - This class initializes, checks, and runs the SetPropertyFromEnsemble() command.

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

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TSEnsemble;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the SetPropertyFromEnsemble() command.
*/
public class SetPropertyFromEnsemble_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{
	
/**
Property set during discovery.
*/
private Prop __discovery_Prop = null;

/**
Constructor.
*/
public SetPropertyFromEnsemble_Command ()
{	super();
	setCommandName ( "SetPropertyFromEnsemble" );
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
    
    if ( (PropertyValue == null) || PropertyValue.equals("") ) {
        message = "The property value must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Provide a property value." ) );
    }
    
    // Check for invalid parameters...
	List<String> validList = new ArrayList<String>(5);
    validList.add ( "EnsembleID" );
    validList.add ( "PropertyName" );
    validList.add ( "PropertyValue" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );
    
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level, MessageUtil.formatMessageTag(command_tag,warning_level), warning );
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
{	// The command will be modified if changed...
	return (new SetPropertyFromEnsemble_JDialog ( parent, this )).ok();
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
        List<Prop> v = new ArrayList<Prop>(1);
        v.add ( discovery_Prop );
        return v;
    }
    else {
        return null;
    }
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
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
	int log_level = 3;  // Level for non-use messages for log file.

	// Make sure there are time series available to operate on...

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
		status.clearLog(commandPhase);
	}
	
	PropList parameters = getCommandParameters();

    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
	String PropertyName = parameters.getValue ( "PropertyName" );
	String PropertyValue = parameters.getValue ( "PropertyValue" ); // Expanded below in run mode

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Command parameter data has errors.  Unable to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		throw new CommandException ( message );
	}

	// Now process the ensembles...

	TSEnsemble ensemble = null;
	try {
		if ( commandPhase == CommandPhaseType.RUN ) {
			// Get the ensemble to process...
			PropList request_params = new PropList ( "" );
			String EnsembleList = "AllMatchingEnsembleID";
			request_params.set ( "EnsembleList", null ); // Will be ignored for now
		    request_params.set ( "EnsembleID", EnsembleID );
			CommandProcessorRequestResultsBean bean = null;
			try {
		        bean = processor.processRequest( "GetEnsemble", request_params);
			}
			catch ( Exception e ) {
				message = "Error requesting GetEnsemble(EnsembleList=\"" + EnsembleList +
				"\", EnsembleID=\"" + EnsembleID + "\") from processor.";
				Message.printWarning(warning_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
				status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Report problem to software support." ) );
			}
			PropList bean_PropList = bean.getResultsPropList();
			Object o_TSEnsemble = bean_PropList.getContents ( "TSEnsemble" );
			if ( o_TSEnsemble == null ) {
				message = "Unable to find time series ensemble to process using EnsembleList=\"" + EnsembleList + "\" EnsembleID=\"" +
		        EnsembleID + "\".";
				Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count), routine, message );
				status.addToLog ( CommandPhaseType.RUN,
						new CommandLogRecord(CommandStatusType.FAILURE,
								message, "Confirm that time series ensemble is available (may be OK for partial run)." ) );
			}
			List<TSEnsemble> ensembleList = new ArrayList<TSEnsemble>(1);
			if ( o_TSEnsemble != null ) {
				ensembleList.add((TSEnsemble)o_TSEnsemble);
			}
			int size = 0;
			if ( ensembleList != null ) {
				size = ensembleList.size();
			}
		    for ( int i = 0; i < size; i++ ) {
		        ensemble = ensembleList.get(i);
	
		    	// Set the property in the processor
				Object Property_Object = null;
				if ( commandPhase == CommandPhaseType.RUN ) {
					Property_Object = TSCommandProcessorUtil.expandTimeSeriesEnsembleMetadataString (
		                    processor, ensemble, PropertyValue, status, CommandPhaseType.RUN);
				}
		    	request_params = new PropList ( "" );
		    	request_params.setUsingObject ( "PropertyName", PropertyName );
		    	request_params.setUsingObject ( "PropertyValue", Property_Object );
		    	try {
		            processor.processRequest( "SetProperty", request_params);
		            // Set the property value in discovery mode
		            if ( commandPhase == CommandPhaseType.DISCOVERY ) {
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
		}
	}
	catch ( Exception e ) {
		message = "Unexpected error setting property from ensemble \""+ ensemble.getEnsembleID() + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "See the log file for details." ) );
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
    String EnsembleID = props.getValue( "EnsembleID" );
    String PropertyName = props.getValue( "PropertyName" );
    String PropertyValue = props.getValue( "PropertyValue" );
	StringBuffer b = new StringBuffer ();
	if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
	}
    if ( (PropertyName != null) && (PropertyName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PropertyName=\"" + PropertyName + "\"");
    }
    if ( (PropertyValue != null) && (PropertyValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PropertyValue=\"" + PropertyValue + "\"");
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
