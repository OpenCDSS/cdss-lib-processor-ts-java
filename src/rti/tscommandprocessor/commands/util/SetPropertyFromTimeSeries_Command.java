// SetPropertyFromTimeSeries_Command - This class initializes, checks, and runs the SetPropertyFromTimeSeries() command.

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

package rti.tscommandprocessor.commands.util;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.TSData;
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
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the SetPropertyFromTimeSeries() command.
*/
public class SetPropertyFromTimeSeries_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Property set during discovery for the PropertyName/PropertyValue.
*/
private Prop __discovery_Prop = null;

/**
Property set during discovery for the PropertyNameForValue.
*/
private Prop __discoveryNameForValue_Prop = null;

/**
Property set during discovery for the PropertyNameForFlag.
*/
private Prop __discoveryNameForFlag_Prop = null;

/**
Constructor.
*/
public SetPropertyFromTimeSeries_Command () {
	super();
	setCommandName ( "SetPropertyFromTimeSeries" );
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
	String PropertyName = parameters.getValue ( "PropertyName" );
	String PropertyValue = parameters.getValue ( "PropertyValue" );
	String DateTime0 = parameters.getValue ( "DateTime" );
	String PropertyNameForValue = parameters.getValue ( "PropertyNameForValue" );
	String PropertyNameForFlag = parameters.getValue ( "PropertyNameForFlag" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    // Count of ways to set.
    int setCount = 0;
    if ( (PropertyName != null) && !PropertyName.isEmpty() ) {
    	++setCount;
        // Check for allowed characters.
        if ( StringUtil.containsAny(PropertyName,"${}() \t", true)) {
            message = "The property name contain invalid characters.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Specify a property name that does not include the characters $(){}, space, or tab." ) );
        }
        if ( (PropertyValue == null) || PropertyValue.equals("") ) {
            message = "The property value must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Provide a property value." ) );
        }
    }

    if ( (PropertyNameForValue != null) && !PropertyNameForValue.isEmpty() ) {
    	++setCount;
        // Check for allowed characters.
        if ( StringUtil.containsAny(PropertyNameForValue,"() \t", true)) {
            message = "The property name for the data value contains invalid characters.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Specify a property name that does not include the characters (), space, or tab." ) );
        }
        if ( (DateTime0 == null) || DateTime0.isEmpty() ) {
            message = "The DateTime parameter is not specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the DateTime parameter." ) );
        }
    }

    if ( (PropertyNameForFlag != null) && !PropertyNameForFlag.isEmpty() ) {
    	++setCount;
        // Check for allowed characters.
        if ( StringUtil.containsAny(PropertyNameForValue,"() \t", true)) {
            message = "The property name for the data flag contains invalid characters.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Specify a property name that does not include the characters (), space, or tab." ) );
        }
        if ( (DateTime0 == null) || DateTime0.isEmpty() ) {
            message = "The DateTime parameter is not specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the DateTime parameter." ) );
        }
    }

	if ( (DateTime0 != null) && ! DateTime0.equalsIgnoreCase("OutputStart") && !DateTime0.contains("${") ) {
		try {
			DateTime.parse(DateTime0);
		}
		catch ( Exception e ) {
            message = "The date/time \"" + DateTime0 + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time." ) );
		}
    }

    if ( setCount == 0 ) {
        message = "The property name, property name for data value, and/or property name for data flag must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Provide at least one property name to set." ) );
    }

    // Check for invalid parameters.
	List<String> validList = new ArrayList<>(8);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "PropertyName" );
    validList.add ( "PropertyValue" );
    validList.add ( "DateTime" );
    validList.add ( "PropertyNameForValue" );
    validList.add ( "PropertyNameForFlag" );
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
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new SetPropertyFromTimeSeries_JDialog ( parent, this )).ok();
}

/**
Return the property defined in discovery phase.
*/
private Prop getDiscoveryProp () {
    return this.__discovery_Prop;
}

/**
Return the property defined in discovery phase.
*/
private Prop getDiscoveryNameForFlagProp () {
    return this.__discoveryNameForFlag_Prop;
}

/**
Return the property defined in discovery phase.
*/
private Prop getDiscoveryNameForValueProp () {
    return this.__discoveryNameForValue_Prop;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  Prop
@return the list of data objects (property names) read by this object in discovery mode.
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
	// Always create a list makes it easier to deal with logic.
    List<T> v = new ArrayList<>();
    Prop prop = new Prop();
    // Check for TS request or class that matches the data.
    if ( c == prop.getClass() ) {
    	// Request is for 'Prop' class.
    	Prop discovery_Prop = getDiscoveryProp ();
    	if ( discovery_Prop != null ) {
    		v.add ( (T)discovery_Prop );
    	}
    	discovery_Prop = getDiscoveryNameForValueProp ();
    	if ( discovery_Prop != null ) {
    		v.add ( (T)discovery_Prop );
    	}
    	discovery_Prop = getDiscoveryNameForFlagProp ();
    	if ( discovery_Prop != null ) {
    		v.add ( (T)discovery_Prop );
    	}
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
	int log_level = 3;  // Level for non-use messages for log file.

	// Make sure there are time series available to operate on.

	CommandProcessor processor = getCommandProcessor();
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

	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
	String PropertyName = parameters.getValue ( "PropertyName" ); // Expanded below in run mode.
	String PropertyValue = parameters.getValue ( "PropertyValue" ); // Expanded below in run mode.

	String DateTime = parameters.getValue ( "DateTime" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		DateTime = TSCommandProcessorUtil.expandParameterValue(processor, this, DateTime);
	}
	String PropertyNameForValue = parameters.getValue ( "PropertyNameForValue" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		// Will get expanded later with time series properties.
		PropertyNameForValue = TSCommandProcessorUtil.expandParameterValue(processor, this, PropertyNameForValue);
	}
	String PropertyNameForFlag = parameters.getValue ( "PropertyNameForFlag" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		// Will get expanded later with time series properties.
		PropertyNameForFlag = TSCommandProcessorUtil.expandParameterValue(processor, this, PropertyNameForFlag);
	}

	// Get the time series to process.  Allow TSID to be a pattern or specific time series.

	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
	CommandProcessorRequestResultsBean bean = null;
	List<TS> tslist = null;
	DateTime dateTime = null;
	int nts = 0;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
	        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
		}
		catch ( Exception e ) {
			message = "Error processing GetTimeSeriesToProcess(TSList=\"" + TSList + "\", TSID=\"" + TSID + "\") request.";
			if ( EnsembleID != null ) {
				message = "Error processing GetTimeSeriesToProcess(TSList=\"" + TSList + "\", EnsembleID=\"" + EnsembleID + "\") request.";
			}
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
	        status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Report the problem to software support." ) );
		}
	    if ( bean == null ) {
	        Message.printStatus ( 2, routine, "Bean is null.");
	    }
		PropList bean_PropList = bean.getResultsPropList();
		Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
		if ( o_TSList == null ) {
			message = "Null TSToProcessList returned for processor request GetTimeSeriesToProcess(TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\").";
			if ( EnsembleID != null ) {
				message = "Null TSToProcessList returned for processor request GetTimeSeriesToProcess(TSList=\"" + TSList +
					"\" EnsembleID=\"" + EnsembleID + "\").";
			}
			Message.printWarning ( log_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
	        status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                        message,
	                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
		}
		else {
			@SuppressWarnings("unchecked")
			List<TS> tslist0 = (List<TS>)o_TSList;
	        tslist = tslist0;
			if ( tslist.size() == 0 ) {
				message = "No time series are available from processor request GetTimeSeriesToProcess (TSList=\"" + TSList +
				"\" TSID=\"" + TSID + "\").";
				if ( EnsembleID != null ) {
					message = "No time series are available from processor request GetTimeSeriesToProcess (TSList=\"" + TSList +
							"\" EnsembleID=\"" + EnsembleID + "\").";
				}
				Message.printWarning ( log_level,
						MessageUtil.formatMessageTag(
								command_tag,++warning_count), routine, message );
	            status.addToLog ( CommandPhaseType.RUN,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message,
	                            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
			}
		}

		nts = tslist.size();
		if ( nts == 0 ) {
			message = "Unable to find time series using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
			if ( EnsembleID != null ) {
				message = "Unable to find time series using TSList=\"" + TSList + "\" EnsembleID=\"" + EnsembleID + "\".";
			}
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count), routine, message );
	        status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                        message,
	                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
		}

		// Get the date/time.
		try {
			if ( DateTime != null ) {
				request_params = new PropList ( "" );
				request_params.set ( "DateTime", DateTime );
				bean = null;
				try {
            		bean = processor.processRequest( "DateTime", request_params);
				}
				catch ( Exception e ) {
					message = "Error requesting DateTime(DateTime=" + DateTime + ") from processor.";
					Message.printWarning(log_level,
							MessageUtil.formatMessageTag( command_tag, ++warning_count),
							routine, message );
            		status.addToLog ( CommandPhaseType.RUN,
                    		new CommandLogRecord(CommandStatusType.FAILURE,
                            		message, "Report the problem to software support." ) );
					throw new InvalidCommandParameterException ( message );
				}

				bean_PropList = bean.getResultsPropList();
				Object prop_contents = bean_PropList.getContents ( "DateTime" );
				if ( prop_contents == null ) {
					message = "Null value for SetDateTime DateTime(DateTime=" + DateTime + "\") returned from processor.";
					Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
            		status.addToLog ( CommandPhaseType.RUN,
                    		new CommandLogRecord(CommandStatusType.FAILURE,
                            		message, "Report the problem to software support." ) );
					throw new InvalidCommandParameterException ( message );
				}
				else {
					dateTime = (DateTime)prop_contents;
				}
			}
		}
		catch ( Exception e ) {
			message = "DateTime \"" + DateTime + "\" is invalid.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
        	status.addToLog ( CommandPhaseType.RUN,
                	new CommandLogRecord(CommandStatusType.FAILURE,
                        	message, "Specify a valid date/time." ) );
			throw new InvalidCommandParameterException ( message );
		}
	}

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series).
		message = "Command parameter data has errors.  Unable to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		throw new CommandException ( message );
	}

	// Now process the time series.

	TS ts = null;
	try {
		Object o_ts = null;
		for ( int its = 0; its < nts; its++ ) {
			// Get the time series to process, from the list that was returned above.
			o_ts = tslist.get(its);
			if ( o_ts == null ) {
				message = "Time series to process is null.";
				Message.printWarning(warning_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
	            status.addToLog ( CommandPhaseType.RUN,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message,
	                            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
				// Go to next time series.
				continue;
			}
			ts = (TS)o_ts;
			//Message.printStatus(2, routine, "Process ts=" + ts.getIdentifierString());
			// Expand the property here because can make granular with time series properties.
			String PropertyNameForValueExpanded = TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, PropertyNameForValue, status, commandPhase);
			String PropertyNameForFlagExpanded = TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, PropertyNameForFlag, status, commandPhase);

			// Reusable data object.
			TSData tsdata = null;
			if ( (PropertyName != null) && !PropertyName.isEmpty() ) {
				// Set the property in the processor:
				// - TODO smalers 2020-11-01 this always returns a string but could return a non-string property if direct property look-up
				Object Property_Object = null;
				String propertyName = PropertyName;
				if ( commandPhase == CommandPhaseType.RUN ) {
					propertyName = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
						processor, ts, PropertyName, status, commandPhase);
					Property_Object = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
						processor, ts, PropertyValue, status, commandPhase);
					// TODO smalers 2020-11-01 added this check to help with typos, but might need a command parameter like IfPropertyNotFound.
					if ( (Property_Object != null) && (Property_Object instanceof String) &&
						(((String)Property_Object).indexOf("${") >= 0) ) { // }
						// Some property did not expand - still seeing ${} in result.
						message = "Property was not found.  Result is: " + Property_Object;
						Message.printWarning(warning_level,
							MessageUtil.formatMessageTag( command_tag, ++warning_count),
							routine, message );
						status.addToLog ( CommandPhaseType.RUN,
							new CommandLogRecord(CommandStatusType.WARNING,
								message,
									"Verify that the requested property is valid and is set." ) );
					}
				}
				request_params = new PropList ( "" );
				request_params.setUsingObject ( "PropertyName", propertyName );
				request_params.setUsingObject ( "PropertyValue", Property_Object );
				try {
					processor.processRequest( "SetProperty", request_params);
					// Set the property value in discovery mode.
					if ( commandPhase == CommandPhaseType.DISCOVERY ) {
						setDiscoveryProp ( new Prop(PropertyName,Property_Object,"" + Property_Object ) );
					}
				}
				catch ( Exception e ) {
					message = "Error requesting SetProperty(Property=\"" + propertyName + "\") from processor.";
					Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
	    				routine, message );
					status.addToLog ( CommandPhaseType.RUN,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Report the problem to software support." ) );
				}
	    	}
			if ( ((PropertyNameForValueExpanded != null) && !PropertyNameForValueExpanded.isEmpty()) ||
				((PropertyNameForFlagExpanded != null) && !PropertyNameForFlagExpanded.isEmpty())
				) {
				// Setting a property for data value and/or data flag.
				tsdata = ts.getDataPoint(dateTime, tsdata);
				double value = tsdata.getDataValue();
				String flag = tsdata.getDataFlag();
				if ( flag == null ) {
					flag = "";
				}
				if ( Message.isDebugOn ) {
					Message.printStatus (2, routine, "ts=" + ts.getIdentifierString() + " date/time=" + dateTime + " value=" + value + " flag=" + flag );
				}
				// Set the property in the processor:
				// - TODO smalers 2020-11-01 this always returns a string but could return a non-string property if direct property look-up
				Object propertyObject = null;
				if ( (PropertyNameForValueExpanded != null) && !PropertyNameForValueExpanded.isEmpty() ) {
					// Setting a property with the time series data value.
					if ( commandPhase == CommandPhaseType.RUN ) {
						if ( ts.isDataMissing(value) ) {
							// Use NaN.
							propertyObject = Double.NaN;
						}
						else {
							propertyObject = Double.valueOf(value);
						}
						request_params = new PropList ( "" );
						request_params.setUsingObject ( "PropertyName", PropertyNameForValueExpanded );
						request_params.setUsingObject ( "PropertyValue", propertyObject );
						try {
							processor.processRequest( "SetProperty", request_params);
						}
						catch ( Exception e ) {
							message = "Error requesting SetProperty(Property=\"" + PropertyNameForValueExpanded + "\") from processor.";
							Message.printWarning(log_level,
								MessageUtil.formatMessageTag( command_tag, ++warning_count),
	    						routine, message );
							status.addToLog ( CommandPhaseType.RUN,
	                    		new CommandLogRecord(CommandStatusType.FAILURE,
	                            		message, "Report the problem to software support." ) );
						}
						if ( Message.isDebugOn ) {
							Message.printStatus (2, routine, "Set property for value ts=" + ts.getIdentifierString() + " date/time=" + dateTime + " value=" + value + " flag=" + flag );
						}
					}
					else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
						// Set the property value in discovery mode.
						setDiscoveryNameForValueProp ( new Prop(PropertyNameForValueExpanded,propertyObject,"" + propertyObject ) );
					}
	    		} // End PropertyNameForValue.
				if ( (PropertyNameForFlagExpanded != null) && !PropertyNameForFlagExpanded.isEmpty() ) {
					// Setting a property with the time series data flag.
					if ( commandPhase == CommandPhaseType.RUN ) {
						propertyObject = flag;
						request_params = new PropList ( "" );
						request_params.setUsingObject ( "PropertyName", PropertyNameForFlagExpanded );
						request_params.setUsingObject ( "PropertyValue", propertyObject );
						try {
							processor.processRequest( "SetProperty", request_params);
						}
						catch ( Exception e ) {
							message = "Error requesting SetProperty(Property=\"" + PropertyNameForFlagExpanded + "\") from processor.";
							Message.printWarning(log_level,
								MessageUtil.formatMessageTag( command_tag, ++warning_count),
	    						routine, message );
							status.addToLog ( CommandPhaseType.RUN,
	                    		new CommandLogRecord(CommandStatusType.FAILURE,
	                            		message, "Report the problem to software support." ) );
						}
						if ( Message.isDebugOn ) {
							Message.printStatus (2, routine, "Set property for flag ts=" + ts.getIdentifierString() + " date/time=" + dateTime + " value=" + value + " flag=" + flag );
						}
					}
					else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
						// Set the property value in discovery mode.
						setDiscoveryNameForFlagProp ( new Prop(PropertyNameForFlagExpanded,propertyObject,"" + propertyObject ) );
	    			}
				} // End PropertyNameForFlag.
			} // End PropertyNameForValue and/or PropertyNameForFlag.
		} // End ts loop.
	}
	catch ( Exception e ) {
		message = "Unexpected error setting property from time series \""+ ts.getIdentifier() + "\" (" + e + ").";
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
private void setDiscoveryProp ( Prop prop ) {
    this.__discovery_Prop = prop;
}

/**
Set the property defined in discovery phase.
@param prop Property set during discovery phase.
*/
private void setDiscoveryNameForFlagProp ( Prop prop ) {
    this.__discoveryNameForFlag_Prop = prop;
}

/**
Set the property defined in discovery phase.
@param prop Property set during discovery phase.
*/
private void setDiscoveryNameForValueProp ( Prop prop ) {
    this.__discoveryNameForValue_Prop = prop;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TSList",
		"TSID",
    	"EnsembleID",
    	"PropertyName",
    	"PropertyValue",
    	"DateTime",
    	"PropertyNameForValue",
    	"PropertyNameForFlag"
	};
	return this.toString(parameters, parameterOrder);
}

}