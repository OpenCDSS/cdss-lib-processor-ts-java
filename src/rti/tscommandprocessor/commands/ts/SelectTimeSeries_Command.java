// SelectTimeSeries_Command - This class initializes, checks, and runs the SelectTimeSeries() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilterStringCriterionType;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import cdss.domain.hydrology.network.HydrologyNode;
import cdss.domain.hydrology.network.HydrologyNodeNetwork;

/**
This class initializes, checks, and runs the SelectTimeSeries() command.
*/
public class SelectTimeSeries_Command extends AbstractCommand implements Command
{

/**
Values for IfNotFound parameter.
*/
protected final String _Ignore = "Ignore";
protected final String _Fail = "Fail";
protected final String _Warn = "Warn";

/**
Values for DeselectAllFirst.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
TSPosition data, zero offset indices
*/
private int [] __TSPositionStart = new int[0];
private int [] __TSPositionEnd = new int[0];

/**
Constructor.
*/
public SelectTimeSeries_Command () {
	super();
	setCommandName ( "SelectTimeSeries" );
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
	//String TSList = parameters.getValue ( "TSList" );
    //String TSID = parameters.getValue ( "TSID" );
    String TSPosition = parameters.getValue ( "TSPosition" );
	String DeselectAllFirst = parameters.getValue ( "DeselectAllFirst" );
	String IfNotFound = parameters.getValue("IfNotFound");
    String PropertyName = parameters.getValue ( "PropertyName" );
    String PropertyCriterion = parameters.getValue ( "PropertyCriterion" );
    String PropertyValue = parameters.getValue ( "PropertyValue" );
    String HasData = parameters.getValue ( "HasData" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (TSPosition != null) && !TSPosition.equals("") ) {
		List<String> tokens = StringUtil.breakStringList ( TSPosition,",", StringUtil.DELIM_SKIP_BLANKS );
        int npos = 0;
        if ( tokens != null ) {
            npos = tokens.size();
        }
        __TSPositionStart = new int[npos];
        __TSPositionEnd = new int[npos];
        for ( int i = 0; i < npos; i++ ) {
            String token = (String)tokens.get(i);
            if ( token.indexOf("-") >= 0 ) {
                // Range.
                String posString = StringUtil.getToken(token, "-",0,0).trim();
                if ( !StringUtil.isInteger(posString) ) {
                    message = "The TSPosition range (" + token + ") contains an invalid position.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the range using integers with value 1+." ) );
                }
                else {
                    __TSPositionStart[i] = StringUtil.atoi( posString ) - 1;
                }
                posString = StringUtil.getToken(token, "-",0,1).trim();
                if ( !StringUtil.isInteger(posString) ) {
                    message = "The TSPosition range (" + token + ") contains an invalid position.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the range using integer with value 1+." ) );
                }
                else {
                    __TSPositionEnd[i] = StringUtil.atoi( posString ) - 1;
                }
            }
            else {
                // Single value.  Treat as a range of 1.
                if ( !StringUtil.isInteger(token) ) {
                    message = "The TSPosition (" + token + ") is invalid.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the position as an integer 1+." ) );
                }
                __TSPositionStart[i] = StringUtil.atoi(token) - 1;
                __TSPositionEnd[i] = __TSPositionStart[i];
            }
            Message.printStatus ( 1, "", "Range " + i + " from " + token + " is " +
                    __TSPositionStart[i] + "," + __TSPositionEnd[i] );
        }
	}

	if ( (HasData != null) && !HasData.equals("") &&
        !HasData.equalsIgnoreCase(_True) && !HasData.equalsIgnoreCase(_False) ) {
        message = "The HasData (" + HasData + ") parameter value is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify as " + _False + ", " + _True + ", or blank to not check." ) );
	}

	if ( (DeselectAllFirst != null) && !DeselectAllFirst.equals("") &&
	        !DeselectAllFirst.equalsIgnoreCase(_True) && !DeselectAllFirst.equalsIgnoreCase(_False) ) {
        message = "The DeselectAllFirst (" + DeselectAllFirst + ") parameter value is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify as " + _False + " or " + _True + "." ) );
	}

    if ( (IfNotFound != null) && !IfNotFound.equals("") && !IfNotFound.equalsIgnoreCase(_Ignore) &&
        !IfNotFound.equalsIgnoreCase(_Fail) && !IfNotFound.equalsIgnoreCase(_Warn) ) {
        message = "Invalid IfNotFound flag \"" + IfNotFound + "\".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the IfNotFound as " + _Ignore + ", " +
                _Warn + ", or " + _Fail + " (default)." ) );
    }

    if ( (PropertyName != null) && !PropertyName.equals("") ) {
        // Check for allowed characters.
        if ( StringUtil.containsAny(PropertyName,"() \t", true)) {
            message = "The property name contains invalid characters () space, tab.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Specify a property name that does not include the characters (), space, or tab." ) );
        }
        if ( (PropertyCriterion == null) || PropertyCriterion.equals("") ) {
            message = "The property condition must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Provide a property condition." ) );
        }
        else {
            // Make sure that the condition is known in general.
            try {
                InputFilterStringCriterionType.valueOfIgnoreCase(PropertyCriterion);
            }
            catch ( Exception e ) {
                message = "The condition (" + PropertyCriterion + ") is not recognized.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Select a supported conditiion using the command editor." ) );
            }
        }
        if ( (PropertyValue == null) || PropertyValue.isEmpty() ) {
            message = "The property value must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Provide a property value." ) );
        }
        else {
            /* How to do a check in discovery?
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
            */
        }
    }

    // Check for invalid parameters.
	List<String> validList = new ArrayList<>(14);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "TSPosition" );
    validList.add ( "DeselectAllFirst" );
    validList.add ( "IfNotFound" );
    validList.add ( "SelectCountProperty" );
    validList.add ( "PropertyName" );
    validList.add ( "PropertyCriterion" );
    validList.add ( "PropertyValue" );
    validList.add ( "HasData" );
    validList.add ( "NetworkID" );
    validList.add ( "DownstreamNodeID" );
    validList.add ( "UpstreamNodeIDs" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return (new SelectTimeSeries_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
This method currently supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax.
        super.parseCommand( command_string);
        // Recently added TSList so handle it properly.
        PropList parameters = getCommandParameters();
        String TSList = parameters.getValue ( "TSList");
        String TSID = parameters.getValue ( "TSID");
        String Pos = parameters.getValue ( "Pos");

        if ( (Pos != null) && (Pos.length() != 0) ) {
            // Legacy Pos property is specified.  Switch to TSPosition.
            parameters.set ( "TSList", TSListType.TSPOSITION.toString() );
            parameters.set ( "TSPosition", Pos );
        }
        else if ( ((TSList == null) || (TSList.length() == 0)) && // TSList not specified.
                ((TSID != null) && (TSID.length() != 0)) ) { // but TSID is specified.
            // Assume old-style where TSList was not specified but TSID was.
            parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
        }
    }
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3; // Level for non-user messages for log file.

	// Make sure there are time series available to operate on.

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
		status.clearLog(CommandPhaseType.RUN);
	}

	PropList parameters = getCommandParameters();

	String TSList = parameters.getValue ( "TSList" );
	if ( (TSList == null) || TSList.equals("") ) {
	    TSList = "" + TSListType.ALL_TS; // Default.
	}
	String TSID = parameters.getValue ( "TSID" );
	TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	String TSPosition = parameters.getValue ( "TSPosition" );
	TSPosition = TSCommandProcessorUtil.expandParameterValue(processor, this, TSPosition);
	String DeselectAllFirst = parameters.getValue ( "DeselectAllFirst" );
	boolean DeselectAllFirst_boolean = false;  // Default.
	if ( (DeselectAllFirst != null) && DeselectAllFirst.equalsIgnoreCase("true") ) {
	    DeselectAllFirst_boolean = true;
	}
    String IfNotFound = parameters.getValue("IfNotFound");
    if ( (IfNotFound == null) || IfNotFound.equals("")) {
        IfNotFound = _Fail; // Default.
    }
	String SelectCountProperty = parameters.getValue ( "SelectCountProperty" );
	SelectCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, SelectCountProperty);
    String PropertyName = parameters.getValue ( "PropertyName" );
	PropertyName = TSCommandProcessorUtil.expandParameterValue(processor, this, PropertyName);
    String PropertyCriterion = parameters.getValue ( "PropertyCriterion" );
    // TODO SAM 2010-09-21 Need to enable numeric property checks.
    InputFilterStringCriterionType propertyStringConditionType = null;
    if ( (PropertyCriterion != null) && !PropertyCriterion.equals("") ) {
        propertyStringConditionType = InputFilterStringCriterionType.valueOfIgnoreCase(PropertyCriterion);
    }
    String PropertyValue = parameters.getValue ( "PropertyValue" );
	PropertyValue = TSCommandProcessorUtil.expandParameterValue(processor, this, PropertyValue);

    String HasData = parameters.getValue ( "HasData" );
	HasData = TSCommandProcessorUtil.expandParameterValue(processor, this, HasData);
	Boolean hasData = null; // Default is to not check whether data are available.
	if ( (HasData != null) && !HasData.isEmpty() ) {
		if ( HasData.equalsIgnoreCase(_False) ) {
			hasData = Boolean.valueOf(false);
		}
		else if ( HasData.equalsIgnoreCase(_True) ) {
			hasData = Boolean.valueOf(true);
		}
	}

    String NetworkID = parameters.getValue ( "NetworkID" );
	NetworkID = TSCommandProcessorUtil.expandParameterValue(processor, this, NetworkID);
    String DownstreamNodeID = parameters.getValue ( "DownstreamNodeID" );
	DownstreamNodeID = TSCommandProcessorUtil.expandParameterValue(processor, this, DownstreamNodeID);
    String UpstreamNodeIDs = parameters.getValue ( "UpstreamNodeIDs" );
	UpstreamNodeIDs = TSCommandProcessorUtil.expandParameterValue(processor, this, UpstreamNodeIDs);
	List<String> upstreamNodeIds = null;
	if ( (UpstreamNodeIDs != null) && !UpstreamNodeIDs.isEmpty() ) {
		String [] parts = UpstreamNodeIDs.split(",");
		upstreamNodeIds = new ArrayList<String>();
		for ( int i = 0; i < parts.length; i++ ) {
			upstreamNodeIds.add(parts[i].trim());
		}
	}

	// If necessary, get the list of all time series.
	List<TS> tslistAll = new ArrayList<>();
	if ( DeselectAllFirst_boolean ) {
	    // Deselect all first.
	    try {
	    	@SuppressWarnings("unchecked")
			List<TS> tslistAll0 = (List<TS>)processor.getPropContents("TSResultsList");
	        tslistAll= tslistAll0;
            Message.printStatus ( 2, routine, "Deselecting all time series first." );
            int ntsAll = 0;
            if ( tslistAll != null ) {
                ntsAll = tslistAll.size();
            }
            for ( int its = 0; its < ntsAll; its++ ) {
                TS ts = (TS)tslistAll.get(its);    // Will throw Exception.
                ts.setSelected ( false );
            }
	    }
	    catch ( Exception e ) {
	        // Should not happen.
	        message = "Unexpected error deselecting all time series first.";
	        Message.printWarning(log_level,
	                MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                routine, message );
	        status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Report the problem to software support." ) );
	    }
	}

	// Get the time series to process.  Allow TSID to be a pattern or specific time series.

	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
    request_params.set ( "TSPosition", TSPosition );
	CommandProcessorRequestResultsBean bean = null;
	try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
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
	List<TS> tslist = null;
	if ( o_TSList == null ) {
		message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( log_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
	}
	else {
		@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
        tslist = tslist0;
		if ( tslist.size() == 0 ) {
			message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
			if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
	             Message.printWarning ( log_level,
                     MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                 status.addToLog ( CommandPhaseType.RUN,
                     new CommandLogRecord(CommandStatusType.WARNING, message,
                         "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
			}
			else if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
    			Message.printWarning ( log_level,
    				MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE, message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
			}
		}
	}

	int nts = tslist.size();
	if ( nts == 0 ) {
		message = "Unable to find time series to select using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
		if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
		    Message.printWarning ( warning_level,
	            MessageUtil.formatMessageTag(
	            command_tag,++warning_count), routine, message );
	            status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.WARNING, message,
	                    "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
		}
		else if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
    		Message.printWarning ( warning_level,
    		MessageUtil.formatMessageTag(
    		command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
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

	// Get the network.

	HydrologyNodeNetwork network = null;
	List<HydrologyNode> foundNetworkNodes = null;
	if ( (NetworkID != null) && !NetworkID.equals("") ) {
	    request_params = null;
	    bean = null;
        // Get the table to be updated.
        request_params = new PropList ( "" );
        request_params.set ( "NetworkID", NetworkID );
        try {
            bean = processor.processRequest( "GetNetwork", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetNetwork(NetworkID=\"" + NetworkID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object o_Network = bean_PropList.getContents ( "Network" );
        if ( o_Network == null ) {
            message = "Unable to find network to process using NetworkID=\"" + NetworkID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a network exists with the requested ID." ) );
        }
        else {
        	if ( o_Network instanceof HydrologyNodeNetwork ) {
        		network = (HydrologyNodeNetwork)o_Network;
        	}
        }
        if ( network != null ) {
    		// First find the requested node.
        	HydrologyNode downstreamNode = null;
        	if ( DownstreamNodeID.startsWith("-") ) {
        		downstreamNode = network.findNode(DownstreamNodeID.substring(1));
        	}
        	else {
        		downstreamNode = network.findNode(DownstreamNodeID);
        	}
        	// Also check the upstream nodes to make sure they exist in the network.
        	if ( upstreamNodeIds != null ) {
	        	for ( String upstreamNodeId : upstreamNodeIds ) {
	        		HydrologyNode upstreamNode = null;
	            	if ( upstreamNodeId.startsWith("-") ) {
	            		upstreamNode = network.findNode(upstreamNodeId.substring(1));
	            	}
	            	else {
	            		upstreamNode = network.findNode(upstreamNodeId);
	            	}
	        		if ( upstreamNode == null ) {
	        			message = "UpstreamNodeID \"" + upstreamNodeId + "\" was not found in the network.";
	        			Message.printWarning(warning_level,
	    					MessageUtil.formatMessageTag( command_tag, ++warning_count),
	    					routine, message );
	                    status.addToLog ( CommandPhaseType.RUN,
	                        new CommandLogRecord(CommandStatusType.FAILURE,
	                            message,
	                            "Verify that the upstream node is in the network." ) );
	        		}
	        	}
        	}
    		if ( downstreamNode == null ) {
    			message = "DownstreamNodeID \"" + DownstreamNodeID + "\" was not found in the network.";
    			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Verify that the downstream node is in the network." ) );
    		}
    		else {
	    		// Get the nodes upstream of the requested node:
    			// - upstream nodes will limit the search if specified
	    		foundNetworkNodes = new ArrayList<HydrologyNode>();
	    		boolean addFirstNode = true;
	    		if ( DownstreamNodeID.startsWith("-") ) {
	    			addFirstNode = false;
	    		}
	    		network.findUpstreamNodes(foundNetworkNodes, downstreamNode, addFirstNode, upstreamNodeIds);
	    		Message.printStatus(2, routine, "For downstream node \"" + downstreamNode.getCommonID() + "\" found " + foundNetworkNodes.size() + " nodes");
	    		for ( HydrologyNode node : foundNetworkNodes ) {
	    			Message.printStatus(2, routine, "For downstream node \"" + downstreamNode.getCommonID() + "\" found node \"" + node.getCommonID() + "\"");
	    		}
    		}
        }
	}

	// Now process the time series returned for the initial selection (nts could be zero if ignoring no match).

	TS ts = null;
	Object o_ts = null;
    int selectCount = 0;
	for ( int its = 0; its < nts; its++ ) {
		// The the time series to process, from the list that was returned above.
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

		try {
			// The time series is initially not selected.
		    boolean selected = false;
		    int filterCount = 0;
		    // Further filter based on the property (property selection is additive to above selection).
		    if ( (PropertyName != null) && !PropertyName.equals("") ) {
		        // Have a property to check.
		    	++filterCount;
		        Object property = ts.getProperty(PropertyName);
		        if ( property != null ) {
		            // Check the property by type.
		            if ( (property instanceof String) && (propertyStringConditionType != null) ) {
		                selected = InputFilter.evaluateCriterion(
		                    (String)property, propertyStringConditionType, PropertyValue );
		            }
		            if ( selected ) {
		                Message.printStatus ( 2, routine, "Selecting \"" + ts.getIdentifier() +
	                    "\" based on " + PropertyName + " " + PropertyCriterion + " " + PropertyValue + "." );
		            }
		        }
		    }
		    if ( (NetworkID != null) && !NetworkID.equals("") && (network != null) ) {
		        // Match nodes in the network.
		    	++filterCount;
		    	if ( (DownstreamNodeID != null) && !DownstreamNodeID.equals("") && (foundNetworkNodes != null) ) {
		    		for ( HydrologyNode node : foundNetworkNodes ) {
		    			if ( node.getCommonID().equalsIgnoreCase(ts.getLocation()) ) {
		    				// Matched the location so select.
		    				selected = true;
		    			}
		    		}
		    	}
		    }
		    if ( hasData != null ) {
		    	// Check whether the time series has data.
		    	++filterCount;
		    	if ( hasData ) {
		    		// Select if the time series has data.
		    		if ( ts.hasData() ) {
		    			selected = true;
		    		}
		    	}
		    	else if ( ! hasData || ((ts.getDate1() == null) && (ts.getDate2() == null)) ) {
		    		// Select if the time series does not have data.
		    		if ( !ts.hasData() ) {
		    			selected = true;
		    		}
		    	}
		    }
		    if ( filterCount == 0 ) {
		        // Only TSList criteria were used to filter to matching time series.
		        Message.printStatus ( 2, routine, "Selecting \"" + ts.getIdentifier() + "\" based on TSList parameter." );
		        selected = true;
		    }
	        // Do the selection.
			ts.setSelected ( selected );
		}
		catch ( Exception e ) {
			message = "Unexpected error selecting time series (" + e + ").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count),routine,message );
			Message.printWarning(3,routine,e);
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
		}
	}

    // Set the SelectCountProperty.
    if ( (SelectCountProperty != null) && !SelectCountProperty.equals("") ) {
        Object o = null;
        try {
            o = processor.getPropContents("TSResultsList");
        }
        catch ( Exception e ) {
            message = "Error requesting Property=\"TSResultsList\" from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
        }
        if ( o != null ) {
            @SuppressWarnings("unchecked")
			List<TS> allTS = (List<TS>)o;
            for ( TS ats: allTS ) {
                if ( (ats != null) && ats.isSelected() ) {
                    ++selectCount;
                }
            }
            request_params = new PropList ( "" );
            request_params.setUsingObject ( "PropertyName", SelectCountProperty );
            request_params.setUsingObject ( "PropertyValue", new Integer(selectCount) );
            try {
                processor.processRequest( "SetProperty", request_params);
                // TODO SAM 2013-12-07 Evaluate whether this should be done in discovery mode.
                //if ( command_phase == CommandPhaseType.DISCOVERY ) {
                //    setDiscoveryProp ( new Prop(PropertyName,Property_Object,"" + Property_Object ) );
                //}
            }
            catch ( Exception e ) {
                message = "Error requesting SetProperty(Property=\"" + SelectCountProperty + "\") from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
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
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
		"TSList",
		"TSID",
		"EnsembleID",
		"TSPosition",
		"DeselectAllFirst",
		"IfNotFound",
		"SelectCountProperty",
		"PropertyName",
		"PropertyCriterion",
		"PropertyValue",
		"HasData",
		"NetworkID",
		"DownstreamNodeID",
		"UpstreamNodeIDs"
	};
	return this.toString(parameters, parameterOrder);
}

}