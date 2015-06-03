package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSUtil;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the Add() command.
*/
public class Add_Command extends AbstractCommand implements Command
{

/**
Values for the HandleMissingHow parameter.
*/
protected final String _IgnoreMissing = "IgnoreMissing";
protected final String _SetMissingIfOtherMissing = "SetMissingIfOtherMissing";
protected final String _SetMissingIfAnyMissing = "SetMissingIfAnyMissing";

/**
Values for IfTSListToAddIsEmpty parameter.
*/
protected final String _Fail = "Fail";
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";

/**
Constructor.
*/
public Add_Command ()
{	super();
	setCommandName ( "Add" );
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
{	String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    //String AddTSList = parameters.getValue ( "AddTSList" );
	//String SetStart = parameters.getValue ( "SetStart" );
	//String SetEnd = parameters.getValue ( "SetEnd" );
	String HandleMissingHow = parameters.getValue ( "HandleMissingHow" );
    String IfTSListToAddIsEmpty = parameters.getValue ( "IfTSListToAddIsEmpty" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    /*
	if ( (TSList != null) && !TSListType.ALL_MATCHING_TSID.equals(TSList) ) {
		if ( TSID != null ) {
            message = "TSID should only be specified when TSList=" + TSListType.ALL_MATCHING_TSID.toString() + ".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Do not specify the TSID parameter when TList=" + TSListType.ALL_MATCHING_TSID.toString() ) );
		}
	}
    */
    /*
	if ( TSList == null ) {
		// Probably legacy command...
		// TODO SAM 2005-05-17 Need to require TSList when legacy
		// commands are safely nonexistent...  At that point the
		// following check can occur in any case.
		if ( (TSID == null) || (TSID.length() == 0) ) {
            message = "A TSID must be specified.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a TSList parameter value." ) );
		}
	}
    */
    
    if ( ((TSID == null) || TSID.equals("")) && ((EnsembleID == null) || EnsembleID.equals("")) ) {
        message = "Neither TSID or EnsembleID have been specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the TSID or EnsembleID to process." ) ); 
    }
    if ( (TSID != null) && !TSID.equals("") && (EnsembleID != null) && !EnsembleID.equals("") ) {
        message = "Only one of the TSID and EnsembleID should be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the TSID or EnsembleID to process." ) ); 
    }

    /*
 	if ( (SetStart != null) && !SetStart.equals("") && !SetStart.equalsIgnoreCase("OutputStart")){
		try {	DateTime.parse(SetStart);
		}
		catch ( Exception e ) {
            message = "The set start date/time \"" + SetStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	if ( (SetEnd != null) && !SetEnd.equals("") && !SetEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse( SetEnd);
		}
		catch ( Exception e ) {
            message = "The set end date/time \"" + SetStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
    */

    if ( (HandleMissingHow != null) && !HandleMissingHow.equals("") &&
            !HandleMissingHow.equalsIgnoreCase(_IgnoreMissing) &&
            !HandleMissingHow.equalsIgnoreCase(_SetMissingIfOtherMissing) &&
            !HandleMissingHow.equalsIgnoreCase(_SetMissingIfAnyMissing) ) {
        message = "The HandleMissingHow parameter (" + HandleMissingHow + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify HandleMissingHow as " + _IgnoreMissing + ", " +
                _SetMissingIfOtherMissing + ", or " + _SetMissingIfAnyMissing) );
    }
    
    if ( (IfTSListToAddIsEmpty != null) && !IfTSListToAddIsEmpty.equals("") &&
        !IfTSListToAddIsEmpty.equalsIgnoreCase(_Ignore) &&  !IfTSListToAddIsEmpty.equalsIgnoreCase(_Fail) &&
        !IfTSListToAddIsEmpty.equalsIgnoreCase(_Warn) ) {
        message = "The IfTSListToAddIsEmpty value (" + IfTSListToAddIsEmpty + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as " + _Ignore + ", " + _Warn + ", or " + _Fail + "." ) );   
    }
    
	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(7);
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "AddTSList" );
    validList.add ( "AddTSID" );
    validList.add ( "AddEnsembleID" );
    //valid_Vector.add ( "SetStart" );
    //valid_Vector.add ( "SetEnd" );
    validList.add ( "HandleMissingHow" );
    validList.add ( "IfTSListToAddIsEmpty" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new Add_JDialog ( parent, this )).ok();
}

/**
Get the time series to process.
@param its Position in time series array to get time series.
@param tspos Positions in time series processor time series array.
*/
private TS getTimeSeriesToProcess ( int its, int[] tspos, String command_tag, int warning_count )
{   String routine = "Add_Command.getTimeSeriesToProcess";
    TS ts = null;
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "Index", new Integer(tspos[its]) );
    CommandProcessorRequestResultsBean bean = null;
    CommandProcessor processor = getCommandProcessor();
    String message;
    CommandStatus status = getCommandStatus();
    int warning_level = 2;
    int log_level = 3;
    try {
        bean = processor.processRequest( "GetTimeSeries", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeries(Index=" + tspos[its] + "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
        return null;
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object prop_contents = bean_PropList.getContents ( "TS" );
    if ( prop_contents == null ) {
        message = "Null value for GetTimeSeries(Index=" + tspos[its] + "\") returned from processor.";
        Message.printWarning(log_level,
        MessageUtil.formatMessageTag( command_tag, ++warning_count),
            routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
        return null;
    }
    else {
        ts = (TS)prop_contents;
    }
    
    if ( ts == null ) {
        // Skip time series.
        message = "Unable to set time series at position " + tspos[its] + " - null time series.";
        Message.printWarning(warning_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
    }
    return ts;
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "Add_Command.parseCommand", message;

	if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()")) {
        // Current syntax...
        super.parseCommand( command_string);
        // Recently added TSList so handle it properly
        PropList parameters = getCommandParameters();
        String TSList = parameters.getValue ( "TSList");
        String AddTSList = parameters.getValue ( "AddTSList");
        String AddTSID = parameters.getValue ( "AddTSID");
        if ( ((AddTSList == null) || (AddTSList.length() == 0)) && ((AddTSID != null) && (AddTSID.length() > 0)) ) {
            // Old command where AddTSID= is specified but AddTSList is not.
            // TSList may be used instead of AddTSList and if so use it
            if ( (TSList != null) && (TSList.length() > 0) ) {
                AddTSList = TSList;
                // Convert to newer syntax...SpecifiedTS replaced with SpecifiedTSID
                if ( AddTSList.equalsIgnoreCase("SpecifiedTS") ) {
                    AddTSList = TSListType.SPECIFIED_TSID.toString();
                }
                parameters.set ( "AddTSList", AddTSList );
            }
            else {
                // Examine AddTSID to figure out what to do...
                if ( AddTSID.indexOf("*") >= 0 ) {
                    AddTSList = TSListType.ALL_TS.toString();
                }
                else {
                    AddTSList = TSListType.SPECIFIED_TSID.toString();
                }
            }
            parameters.set ( "AddTSList", AddTSList );
        }
        else if ( (TSList != null) && (TSList.length() > 0) ) {
            // Convert to newer syntax...SpecifiedTS replaced with SpecifiedTSID
            AddTSList = TSList;
            if ( AddTSList.equalsIgnoreCase("SpecifiedTS") ) {
                AddTSList = TSListType.SPECIFIED_TSID.toString();
            }
            parameters.set ( "AddTSList", AddTSList );
        }
        message = "Automatically updated to current syntax from old command \"" + command_string + "\".";
        CommandStatus status = getCommandStatus();
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.INFO, message, "" ) ); 
    }
    else {
        // TODO SAM 2005-08-24 This whole block of code needs to be
        // removed as soon as commands have been migrated to the new syntax.
        //
        // Old syntax without named parameters.
    	List v = StringUtil.breakStringList ( command_string,"(),", StringUtil.DELIM_ALLOW_STRINGS );
        if ( (v == null) || (v.size() < 4) ) {
            message = "Syntax error in legacy command \"" + command_string +
                "Expecting Add(TSID,HandleMissingHow,AddTSID,...";
            Message.printWarning ( warning_level, routine, message);
            throw new InvalidCommandSyntaxException ( message );
        }
        String TSID = ((String)v.get(1)).trim();
        String HandleMissingHow = ((String)v.get(2)).trim();
        StringBuffer AddTSID = new StringBuffer();
        for ( int i = 3; i < v.size(); i++ ) {
            // Fourth and fifth fields optionally have analysis period...
            if ( i > 3 ) {
                AddTSID.append(",");
            }
            AddTSID.append(((String)v.get(i)).trim());
        }

        // Set parameters and new defaults...

        PropList parameters = new PropList ( getCommandName() );
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        parameters.set ( "TSID", TSID );
        parameters.set ( "TSList", TSListType.SPECIFIED_TSID.toString() );
        if ( HandleMissingHow.length() > 0 ) {
            parameters.set ( "HandleMissingHow", HandleMissingHow );
        }
        parameters.set ( "AddTSID", AddTSID.toString() );
        parameters.setHowSet ( Prop.SET_UNKNOWN );
        setCommandParameters ( parameters );
        
        message = "Automatically updated to current syntax from old command \"" + command_string + "\".";
        CommandStatus status = getCommandStatus();
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.INFO, message, "" ) ); 
	}
}

/**
Run the command.
@param command_number number of command to run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	//int log_level = 3;	// Warning message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);

	// Get the time series to process.  This should be a single matching time series or ensemble time series.
	
	PropList request_params = new PropList ( "" );
    String IfTSListToAddIsEmpty = parameters.getValue ( "IfTSListToAddIsEmpty" );
    if ( (IfTSListToAddIsEmpty == null) || IfTSListToAddIsEmpty.equals("") ) {
        IfTSListToAddIsEmpty = _Warn;
    }
    CommandStatusType ifTSListToAddIsEmptyStatusType = null;
    if ( IfTSListToAddIsEmpty.equalsIgnoreCase(_Warn) ) {
        ifTSListToAddIsEmptyStatusType = CommandStatusType.WARNING;
    }
    else if ( IfTSListToAddIsEmpty.equalsIgnoreCase(_Warn) ) {
        ifTSListToAddIsEmptyStatusType = CommandStatusType.FAILURE;
    }
	// Only one of these will be specified...
    String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String TSList = null;
    if ( (TSID != null) && (TSID.length() > 0) ) {
        TSList = TSListType.ALL_MATCHING_TSID.toString();   // Should only match one?
        request_params.set ( "TSList", TSList );
        request_params.set ( "TSID", TSID );
    }
    else if ( (EnsembleID != null) && (EnsembleID.length() > 0)) {
        TSList = TSListType.ENSEMBLE_ID.toString();
        request_params.set ( "TSList", TSList );
        request_params.set ( "EnsembleID", EnsembleID );
    }
	CommandProcessorRequestResultsBean bean = null;
	try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
		Message.printWarning(warning_level,
		    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	List<TS> tslist = null;
	int nts = 0;
	if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}
	else {
        tslist = (List<TS>)o_TSList;
        nts = tslist.size();
		if ( tslist.size() == 0 ) {
            message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
					command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Verify that the TSList parameter matches one or more time series - " +
                    "may be OK for partial run or special case." ) );
		}
	}
	Object o_Indices = bean_PropList.getContents ( "Indices" );
	int [] tspos = null;
	if ( o_Indices == null ) {
        message = "Unable to find indices for time series to process using TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
	}
	else {
        tspos = (int [])o_Indices;
		if ( tspos.length == 0 ) {
            message = "Unable to find indices for time series to process using TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
			Message.printWarning ( warning_level,
			    MessageUtil.formatMessageTag(
			        command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
		}
	}
	
	if ( nts == 0 ) {
        message = "Unable to find any time series to process using TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.WARNING, message,
                "Verify that the TSList parameter matches one or more time series - " +
                "may be OK for partial run or special case." ) );
	}
    else {
        if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ) {
            // Expecting only a single time series
            if ( nts != 1 ) {
                message = "Expecting to find one time series to process (have " + nts + ") using TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(
                command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.WARNING,
                        message,
                        "Verify that the TSList parameter matches one time series." ) );
            }
        }
    }

	// Time series to add...
    
    String AddTSList = parameters.getValue ( "AddTSList" );
    if ( (AddTSList == null) || AddTSList.equals("") ) {
        AddTSList = TSListType.ALL_TS.toString();
    }
    String AddTSID = parameters.getValue ( "AddTSID" );
	if ( (AddTSID != null) && (AddTSID.indexOf("${") >= 0) ) {
		AddTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, AddTSID);
	}
    String AddEnsembleID = parameters.getValue ( "AddEnsembleID" );
	if ( (AddEnsembleID != null) && (AddEnsembleID.indexOf("${") >= 0) ) {
		AddEnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, AddEnsembleID);
	}
    request_params = new PropList ( "" );
    request_params.set ( "TSList", AddTSList );
    request_params.set ( "TSID", AddTSID );
    request_params.set ( "EnsembleID", AddEnsembleID );
    try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + AddTSList +
        "\", TSID=\"" + AddTSID + "\", EnsembleID=\"" + AddEnsembleID + "\", SpecifiedTSID=\") from processor.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    Object o_TSList2 = bean_PropList.getContents ( "TSToProcessList" );
    List<TS> add_tslist = null;
    int n_add_ts = 0;
    if ( o_TSList2 == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + AddTSList +
        "\" TSID=\"" + AddTSID + "\", EnsembleID=\"" + AddEnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the AddTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    else {
        add_tslist = (List<TS>)o_TSList2;
        n_add_ts = add_tslist.size();
        if ( n_add_ts == 0 ) {
            message = "No time series to add are available from processor GetTimeSeriesToProcess (TSList=\"" + AddTSList +
            "\" TSID=\"" + AddTSID + "\", EnsembleID=\"" + AddEnsembleID + "\"";
            if ( ifTSListToAddIsEmptyStatusType != null ) {
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(
                        command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(ifTSListToAddIsEmptyStatusType, message,
                        "Verify that the AddTSList parameter matches one or more time series - " +
                        "may be OK for partial run or special case." ) );
            }
        }
    }
    Object o_Indices2 = bean_PropList.getContents ( "Indices" );
    int [] add_tspos = null;
    if ( o_Indices2 == null ) {
        message = "Unable to find indices for time series to add using TSList=\"" + AddTSList +
        "\" TSID=\"" + AddTSID + "\", EnsembleID=\"" + AddEnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
    }
    else {
        add_tspos = (int [])o_Indices2;
        if ( (add_tspos.length == 0) && (n_add_ts > 0) ) {
            message = "Unable to find indices for time series to add using TSList=\"" + AddTSList +
            "\" TSID=\"" + AddTSID + "\", EnsembleID=\"" + AddEnsembleID + "\".";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
        }
    }
    
    if ( add_tslist != null ) {
        n_add_ts = add_tslist.size();
    }
    if ( n_add_ts == 0 ) {
        message = "Unable to find any time series to add using TSList=\"" + AddTSList +
        "\" TSID=\"" + AddTSID + "\", EnsembleID=\"" + AddEnsembleID + "\".";
        if ( ifTSListToAddIsEmptyStatusType != null ) {
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(ifTSListToAddIsEmptyStatusType,
                    message,
                    "Verify that the AddTSList parameter matches one or more time series - " +
                    "may be OK for partial run or special case." ) );
        }
    }
    
    // Make sure that the number of dependent and independent time series is consistent
    // Already checked to make sure there is a single time series above if NOT processing enembles so
    // just check ensembles here.
    
    if ( TSListType.ENSEMBLE_ID.equals(AddTSList) ) {
        if ( (n_add_ts != 1) && (n_add_ts != nts) ) {
            message = "The number if time series to add to the ensemble (" + n_add_ts +
                ") must be 1 or the same number in the ensemble (" + nts + ").";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message,
                    "Verify that the number of time series to add is 1 or the same as the ensemble." ) );
        }
    }

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Insufficient data to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}

	// Now process the time series...

    /*
    String TransferHow = parameters.getValue("TransferHow");
	PropList setprops = new PropList ( "Add" );
	if ( (TransferHow != null) && !TransferHow.equals("") ) {
		setprops.set ( "TransferHow", TransferHow );
	}
    */
    
    String HandleMissingHow = parameters.getValue("HandleMissingHow");
    int HandleMissingHow_int = TSUtil.IGNORE_MISSING;
    if ( HandleMissingHow == null ) {
        HandleMissingHow = "IgnoreMissing";
    }
    if ( HandleMissingHow.equalsIgnoreCase( _IgnoreMissing ) ) {
        HandleMissingHow_int = TSUtil.IGNORE_MISSING;
    }
    else if ( HandleMissingHow.equalsIgnoreCase( _SetMissingIfOtherMissing ) ){
        HandleMissingHow_int = TSUtil.SET_MISSING_IF_OTHER_MISSING;
    }
    else if ( HandleMissingHow.equalsIgnoreCase( _SetMissingIfAnyMissing ) ) {
        HandleMissingHow_int=TSUtil.SET_MISSING_IF_ANY_MISSING;
    }

	TS ts = null;  // Time series to be added to
    // Loop through the time series being added...
	for ( int its = 0; its < nts; its++ ) {
		ts = getTimeSeriesToProcess ( its, tspos, command_tag, warning_count );
		if ( ts == null ) {
			// Skip time series.
            message = "Unable to get time series at position " + tspos[its] + " - null time series.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
			continue;
		}
	    notifyCommandProgressListeners ( its, nts, (float)-1.0, "Adding time series " +
	        ts.getIdentifier().toStringAliasAndTSID() );
        
        // TODO SAM 2008-01-06 Phase out if customer does not need or if a more robust way to check
        // for dates can be implemented
        // Special check inspired by CDSS where people tried to add FrostDate time series
        if ( StringUtil.indexOfIgnoreCase(ts.getDataType(), "FrostDate", 0) >= 0 ) {
            // TODO - SAM 2005-05-20
            // This is a special check because the add() command used to
            // be used in TSTool to add frost date time series.  Now the
            // add() command is not suitable.
            message = "The " + getCommandName() + "() command is not suitable for frost dates - skipping processing.";
            Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Use Blend(), SetFromTS(), or similar commands." ) );
        }
		
		// Get the specific time series to add depending on the input parameters...
        
        TS tstoadd = null;  // Single time series to add
        List<TS> tstoadd_list = new Vector(); // List of time series to add
        if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ) {
            // Processing a single time series.  Add all the time series to it
            // Reuse the same independent time series for all transfers...
            tstoadd_list = add_tslist;
            Message.printStatus(2, routine, "Adding " + tstoadd_list.size() +
                    " time series to single time series \"" + ts.getIdentifier() + "\"" );
        }
        else if ( TSListType.ENSEMBLE_ID.equals(TSList) ) {
            // Processing an ensemble.  Need to loop through each time series in the ensemble and add a single
            // time series (either from a single TS or another ensemble)
            // Get the time series matching the loop index...
            if ( TSListType.ENSEMBLE_ID.equals(AddTSList) ) {
                // Adding an ensemble to an ensemble so get the ensemble time series at the position...
                tstoadd = getTimeSeriesToProcess ( its, add_tspos, command_tag, warning_count );
                tstoadd_list.add( tstoadd );
                Message.printStatus(2, routine, "Adding ensemble time series \"" + tstoadd.getIdentifier() +
                        "\" to ensemble time series \"" + ts.getIdentifier() + "\".");
            }
            else {
                // Adding another time series to an ensemble (checks above verified that only one time series is added).
                Message.printStatus(2, routine, "Adding " + tstoadd_list.size() +
                        " time series to ensemble time series \"" + ts.getIdentifier() + "\".");
                tstoadd_list = add_tslist;
            }
        }
        
        int tstoadd_list_size = tstoadd_list.size();
        if ( tstoadd_list_size == 0 ) {
            // Skip time series.
            message = "Zero time series to add for " + ts.getIdentifier();
            if ( ifTSListToAddIsEmptyStatusType != null ) {
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(ifTSListToAddIsEmptyStatusType,
                        message, "May be be OK for partial run." ) );
            }
            continue;
        }
        
        // Remove from the time series list the time series being added to (don't add to itself)...
        
        TS add_ts;
        for ( int icheck = 0; icheck < tstoadd_list_size; icheck++ ) {
            add_ts = (TS)add_tslist.get(icheck);
            if ( add_ts == null ) {
                continue;
            }
            else if ( add_ts == ts ) {
                Message.printStatus(2, routine, "Removing \"" + add_ts.getIdentifier() +
                        "\" from add since it is same as the receiving time series." );
                add_tslist.remove(icheck);
                --icheck;
                --tstoadd_list_size;
            }
        }
        
        // Finally do the add...
        
        try {
            TSUtil.add ( ts, tstoadd_list, HandleMissingHow_int );
        }
        catch ( Exception e ) {
            message = "Unexpected error adding to time series \"" + ts.getIdentifier() + "\" (" + e + ").";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
            Message.printWarning(3,routine,e);
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
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
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TSID = props.getValue( "TSID" );
    String EnsembleID = props.getValue( "EnsembleID" );
    String AddTSList = props.getValue( "AddTSList" );
    String AddTSID = props.getValue( "AddTSID" );
    String AddEnsembleID = props.getValue( "AddEnsembleID" );
    String HandleMissingHow = props.getValue( "HandleMissingHow" );
    String IfTSListToAddIsEmpty = props.getValue ( "IfTSListToAddIsEmpty" );
	//String SetStart = props.getValue("SetStart");
	//String SetEnd = props.getValue("SetEnd");
    //String TransferHow = props.getValue( "TransferHow" );
	StringBuffer b = new StringBuffer ();
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
    if ( (AddTSList != null) && (AddTSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AddTSList=" + AddTSList );
    }
    if ( (AddTSID != null) && (AddTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AddTSID=\"" + AddTSID + "\"" );
    }
    if ( (AddEnsembleID != null) && (AddEnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AddEnsembleID=\"" + AddEnsembleID + "\"" );
    }
    if ( (HandleMissingHow != null) && (HandleMissingHow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "HandleMissingHow=\"" + HandleMissingHow + "\"" );
    }
    if ( (IfTSListToAddIsEmpty != null) && (IfTSListToAddIsEmpty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IfTSListToAddIsEmpty=" + IfTSListToAddIsEmpty );
    }
    /*
	if ( (SetStart != null) && (SetStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SetStart=\"" + SetStart + "\"" );
	}
	if ( (SetEnd != null) && (SetEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "SetEnd=\"" + SetEnd + "\"" );
	}
    if ( (TransferHow != null) && (TransferHow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TransferHow=" + TransferHow );
    }
    */

	return getCommandName() + "(" + b.toString() + ")";
}

}
