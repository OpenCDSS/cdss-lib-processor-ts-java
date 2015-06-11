package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.TSLimits;
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
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the SetFromTS() command.
*/
public class SetFromTS_Command extends AbstractCommand implements Command
{
    
/**
Parameter values used with RecalcLimits.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Values for the HandleMissingHow parameter.
*/
protected final String _IgnoreMissing = "IgnoreMissing";
protected final String _SetMissing = "SetMissing";
protected final String _SetOnlyMissingValues = "SetOnlyMissingValues";

/**
Set window year, since users do not supply this information.
This allows for leap year in case the analysis window start or end is on Feb 29.
*/
private final int __SET_WINDOW_YEAR = 2000;

/**
Constructor.
*/
public SetFromTS_Command ()
{	super();
	setCommandName ( "SetFromTS" );
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
{	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
	String SetStart = parameters.getValue ( "SetStart" );
	String SetEnd = parameters.getValue ( "SetEnd" );
    String SetWindowStart = parameters.getValue ( "SetWindowStart" );
    String SetWindowEnd = parameters.getValue ( "SetWindowEnd" );
	String TransferHow = parameters.getValue ( "TransferHow" );
	String HandleMissingHow = parameters.getValue ( "HandleMissingHow" );
	String SetDataFlags = parameters.getValue ( "SetDataFlags" );
	String RecalcLimits = parameters.getValue ( "RecalcLimits" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
	if ( (TSList != null) && !TSListType.ALL_MATCHING_TSID.equals(TSList) &&
	        !TSListType.FIRST_MATCHING_TSID.equals(TSList) &&
	        !TSListType.LAST_MATCHING_TSID.equals(TSList)) {
		if ( TSID != null ) {
            message = "TSID should only be specified when TSList=" + TSListType.ALL_MATCHING_TSID.toString() +
            ", " + TSListType.FIRST_MATCHING_TSID.toString() + ", or " +
            TSListType.LAST_MATCHING_TSID.toString()+ ".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Only specify the TSID parameter when TList=" +
                    TSListType.ALL_MATCHING_TSID.toString() +
                    ", " + TSListType.FIRST_MATCHING_TSID.toString() + ", or " +
                    TSListType.LAST_MATCHING_TSID.toString()+ ".") );
		}
	}
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
    /*
    if ( alias.equalsIgnoreCase(independent) ) {
        warning += "\nThe time series to fill \"" + alias +
            "\" is the same.\n"+
            "as the independent time series \"" +
            independent + "\".\n" + "Correct or Cancel.";
    }
    */
 	if ( (SetStart != null) && !SetStart.isEmpty() && !SetStart.equalsIgnoreCase("OutputStart") && !SetStart.startsWith("${")){
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
	if ( (SetEnd != null) && !SetEnd.isEmpty() && !SetEnd.equalsIgnoreCase("OutputEnd") && !SetEnd.startsWith("${")) {
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
    if ( (SetWindowStart != null) && !SetWindowStart.equals("") ) {
        String analysisWindowStart = "" + __SET_WINDOW_YEAR + "-" + SetWindowStart;
        try {
            DateTime.parse( analysisWindowStart );
        }
        catch ( Exception e ) {
            message = "The set window start \"" + SetWindowStart + "\" (prepended with " +
            __SET_WINDOW_YEAR + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time using MM, MM-DD, MM-DD hh, or MM-DD hh:mm." ) );
        }
    }
    
    if ( (SetWindowEnd != null) && !SetWindowEnd.equals("") ) {
        String analysisWindowEnd = "" + __SET_WINDOW_YEAR + "-" + SetWindowEnd;
        try {
            DateTime.parse( analysisWindowEnd );
        }
        catch ( Exception e ) {
            message = "The set window end \"" + SetWindowEnd + "\" (prepended with " +
            __SET_WINDOW_YEAR + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time using MM, MM-DD, MM-DD hh, or MM-DD hh:mm." ) );
        }
    }
    if ( (TransferHow != null) && !TransferHow.equals("") &&
            !TransferHow.equalsIgnoreCase(TSUtil.TRANSFER_SEQUENTIALLY) &&
            !TransferHow.equalsIgnoreCase(TSUtil.TRANSFER_BYDATETIME) ) {
        message = "The TransferHow parameter (" + TransferHow + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify TransferHow as " + TSUtil.TRANSFER_SEQUENTIALLY + " or " +
                TSUtil.TRANSFER_BYDATETIME ) );
    }

    if ( (HandleMissingHow != null) && !HandleMissingHow.equals("") &&
            !HandleMissingHow.equalsIgnoreCase(_IgnoreMissing) &&
            !HandleMissingHow.equalsIgnoreCase(_SetMissing) &&
            !HandleMissingHow.equalsIgnoreCase(_SetOnlyMissingValues)) {
        message = "The HandleMissingHow parameter (" + HandleMissingHow + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify HandleMissingHow as " + _IgnoreMissing + ", " +
                _SetMissing + " (default if blank), or " + _SetOnlyMissingValues + ".") );
    }
    
    if ( (SetDataFlags != null) && !SetDataFlags.equals("") &&
        !SetDataFlags.equalsIgnoreCase( "true" ) && !SetDataFlags.equalsIgnoreCase("false") ) {
        message = "The SetDataFlags parameter must be blank, " + _False + " or " + _True + " (default if blank).";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a SetDataFlags as " + _False + " or " + _True + ".") );
    }

    if ( (RecalcLimits != null) && !RecalcLimits.equals("") &&
        !RecalcLimits.equalsIgnoreCase( "true" ) && !RecalcLimits.equalsIgnoreCase("false") ) {
        message = "The RecalcLimits parameter must be blank, " + _False + " (default), or " + _True + ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a RecalcLimits as " + _False + " or " + _True + ".") );
    }
    
	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(16);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "IndependentTSList" );
    validList.add ( "IndependentTSID" );
    validList.add ( "IndependentEnsembleID" );
    validList.add ( "SetStart" );
    validList.add ( "SetEnd" );
    validList.add ( "SetWindowStart" );
    validList.add ( "SetWindowEnd" );
    validList.add ( "TransferHow" );
    validList.add ( "HandleMissingHow" );
    validList.add ( "SetDataFlags" );
    validList.add ( "SetFlag" );
    validList.add ( "SetFlagDesc" );
    validList.add ( "RecalcLimits" );
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
	return (new SetFromTS_JDialog ( parent, this )).ok();
}

/**
Get the time series to process.
@param its Position in time series array to get time series.
@param tspos Positions in time series processor time series array.
*/
private TS getTimeSeriesToProcess ( int its, int[] tspos, String command_tag, int warning_count )
{   String routine = "SetFromTS_Command.getTimeSeriesToProcess";
    TS ts = null;
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "Index", new Integer(tspos[its]) );
    CommandProcessorRequestResultsBean bean = null;
    CommandProcessor processor = getCommandProcessor();
    String message;
    CommandStatus status = getCommandStatus();
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
    
    return ts;
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "SetFromTS_Command.parseCommand", message;

	if ( ((command_string.indexOf('=') > 0) || command_string.endsWith("()")) &&
            (command_string.indexOf("TransferData=")<0 ) ) {    // One parameter in old style had =
        // Current syntax...
        super.parseCommand( command_string);
        // Recently added TSList so handle it properly
        PropList parameters = getCommandParameters();
        String TSList = parameters.getValue ( "TSList");
        String TSID = parameters.getValue ( "TSID");
        if ( ((TSList == null) || (TSList.length() == 0)) && // TSList not specified
                ((TSID != null) && (TSID.length() != 0)) ) { // but TSID is specified
            // Assume old-style where TSList was not specified but TSID was...
            if ( (TSID != null) && TSID.indexOf("*") >= 0 ) {
                parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
            }
            else {
                parameters.set ( "TSList", TSListType.LAST_MATCHING_TSID.toString() );
            }
        }
    }
    else {
		// TODO SAM 2005-09-08 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax where the parameters are TSID,IndependentTSID,SetStart,SetEnd,TransferHow
    	List v = StringUtil.breakStringList(command_string,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS |	StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		int n_expected = 5;
		if ( command_string.indexOf("TransferData=")>0 ) {
		    n_expected = 6;
		}
		if ( ntokens != n_expected ) {
			// Command name, TSID, and constant...
			message = "Syntax error in \"" + command_string + "\".  " + n_expected + " parameters expected.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}

		// Get the individual tokens of the expression...

		String TSID = ((String)v.get(1)).trim();
        String IndependentTSID = ((String)v.get(2)).trim();
		String SetStart = ((String)v.get(3)).trim();
        String SetEnd = ((String)v.get(4)).trim();
        String TransferHow = null;
        if ( n_expected == 6 ) {
            // Old TransferData is now TransferHow
            // This parameter is of the format TransferData=...
            TransferHow = StringUtil.getToken(((String)v.get(5)).trim(),"=",0,1);
        }

		// Set parameters and new defaults...

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID.length() > 0 ) {
			parameters.set ( "TSID", TSID );
			parameters.setHowSet(Prop.SET_AS_RUNTIME_DEFAULT);
            if ( TSID.indexOf("*") >= 0 ) {
                parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
            }
            else {
                parameters.set ( "TSList", TSListType.LAST_MATCHING_TSID.toString() );
            }
		}
        if ( IndependentTSID.length() > 0 ) {
            parameters.set ( "IndependentTSID", IndependentTSID );
            parameters.setHowSet(Prop.SET_AS_RUNTIME_DEFAULT);
            if ( IndependentTSID.indexOf("*") >= 0 ) {
                parameters.set ( "IndependentTSList", TSListType.ALL_MATCHING_TSID.toString() );
            }
            else {
                parameters.set ( "IndependentTSList", TSListType.LAST_MATCHING_TSID.toString() );
            }
        }
        // Phase out * as default, blank is current default.
        if ( !SetStart.equals("*")) {
            parameters.set ( "SetStart", SetStart );
        }
        if ( !SetEnd.equals("*")) {
            parameters.set ( "SetEnd", SetEnd );
        }
        if ( TransferHow != null ) {
            parameters.set ( "TransferHow", TransferHow );
        }
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
}

/**
Calls TSCommandProcessor to re-calculate limits for this time series.
@param ts Time Series.
@param TSCmdProc CommandProcessor that is using this command.
@param warningLevel Warning level used for displaying warnings.
@param warning_count Number of warnings found.
@param command_tag Reference or identifier for this command.
 */
private int recalculateLimits( TS ts, CommandProcessor TSCmdProc, 
    int warningLevel, int warning_count, String command_tag )
{
    String routine = "SetFromTS_Command.recalculateLimits", message;
    
    CommandStatus status = getCommandStatus();
    
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "TS", ts );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = TSCmdProc.processRequest( "CalculateTSAverageLimits", request_params);
    }
    catch ( Exception e ) {
        message = "Error recalculating original data limits for \"" + ts.getIdentifierString() + "\" (" + e + ")";
        Message.printWarning(warningLevel,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message  );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
        return warning_count;
    }
    // Get the calculated limits and set in the original data limits...
    PropList bean_PropList = bean.getResultsPropList();
    Object prop_contents = bean_PropList.getContents ( "TSLimits" );
    if ( prop_contents == null ) {
        message = "Null value for TSLimits from CalculateTSAverageLimits(" + ts.getIdentifierString() + ")";
        Message.printWarning(warningLevel,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
            routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
        return warning_count;
    }
    // Now set the limits.
    ts.setDataLimitsOriginal ( (TSLimits)prop_contents );
    return warning_count;
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
	int warningLevel = 2;
	String command_tag = "" + command_number;
	//int log_level = 3;	// Warning message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
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

	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
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
    String RecalcLimits = parameters.getValue ( "RecalcLimits" );
    String SetWindowStart = parameters.getValue ( "SetWindowStart" );
    String SetWindowEnd = parameters.getValue ( "SetWindowEnd" );
    String HandleMissingHow = parameters.getValue ( "HandleMissingHow" );
    if ( (HandleMissingHow == null) || HandleMissingHow.equals("") ) {
        HandleMissingHow = _SetMissing; // Default
    }
    String SetDataFlags = parameters.getValue ( "SetDataFlags" );
    boolean SetDataFlags_boolean = true; // Default
    if ( (SetDataFlags != null) && SetDataFlags.equalsIgnoreCase("false") ) {
        SetDataFlags_boolean = false;
    }
    boolean RecalcLimits_boolean = false; // Default
    if ( (RecalcLimits != null) && RecalcLimits.equalsIgnoreCase("true") ) {
        RecalcLimits_boolean = true;
    }

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
		Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	List tslist = null;
	if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warningLevel,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}
	else {
        tslist = (List)o_TSList;
		if ( tslist.size() == 0 ) {
            message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
			Message.printWarning ( warningLevel,
				MessageUtil.formatMessageTag(
					command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message,
                    "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
		}
	}
	Object o_Indices = bean_PropList.getContents ( "Indices" );
	int [] tspos = null;
	if ( o_Indices == null ) {
        message = "Unable to find indices for time series to process using TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warningLevel,
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
			Message.printWarning ( warningLevel,
			    MessageUtil.formatMessageTag(
			        command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
		}
	}
	
	int nts = 0;
	if ( tslist != null ) {
		nts = tslist.size();
	}
	if ( nts == 0 ) {
        message = "Unable to find any time series to process using TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warningLevel,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.WARNING,
                message,
                "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
	}

	// Independent time series...
    
    String IndependentTSList = parameters.getValue ( "IndependentTSList" );
    if ( (IndependentTSList == null) || IndependentTSList.equals("") ) {
        IndependentTSList = TSListType.ALL_TS.toString();
    }
    String IndependentTSID = parameters.getValue ( "IndependentTSID" );
	if ( (IndependentTSID != null) && (IndependentTSID.indexOf("${") >= 0) ) {
		IndependentTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, IndependentTSID);
	}
    String IndependentEnsembleID = parameters.getValue ( "IndependentEnsembleID" );
	if ( (IndependentEnsembleID != null) && (IndependentEnsembleID.indexOf("${") >= 0) ) {
		IndependentEnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, IndependentEnsembleID);
	}
    request_params = new PropList ( "" );
    request_params.set ( "TSList", IndependentTSList );
    request_params.set ( "TSID", IndependentTSID );
    request_params.set ( "EnsembleID", IndependentEnsembleID );
    try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(IndependentTSList=\"" + IndependentTSList +
        "\", IndependentTSID=\"" + IndependentTSID + "\", IndependentEnsembleID=\"" + IndependentEnsembleID + "\") from processor.";
        Message.printWarning(warningLevel,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    Object o_TSList2 = bean_PropList.getContents ( "TSToProcessList" );
    List independent_tslist = null;
    if ( o_TSList2 == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(IndependentTSList=\"" + IndependentTSList +
        "\" IndependentTSID=\"" + IndependentTSID + "\", IndependentEnsembleID=\"" + IndependentEnsembleID + "\").";
        Message.printWarning ( warningLevel,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the IndependentTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    else {
        independent_tslist = (List)o_TSList2;
        if ( independent_tslist.size() == 0 ) {
            message = "No independent time series are available from processor GetTimeSeriesToProcess (IndependentTSList=\"" + IndependentTSList +
            "\" IndependentTSID=\"" + IndependentTSID + "\", IndependentEnsembleID=\"" + IndependentEnsembleID + "\").";
            Message.printWarning ( warningLevel,
                MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message,
                    "Verify that the IndependentTSList parameter matches one or more time series - may be OK for partial run." ) );
        }
    }
    Object o_Indices2 = bean_PropList.getContents ( "Indices" );
    int [] independent_tspos = null;
    if ( o_Indices2 == null ) {
        message = "Unable to find indices for independent time series to process using IndependentTSList=\"" + IndependentTSList +
        "\" IndependentTSID=\"" + IndependentTSID + "\", IndependentEnsembleID=\"" + IndependentEnsembleID + "\".";
        Message.printWarning ( warningLevel,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
    }
    else {
        independent_tspos = (int [])o_Indices2;
        if ( independent_tspos.length == 0 ) {
            message = "Unable to find indices for independent time series to process using IndependentTSList=\"" + IndependentTSList +
            "\" IndependentTSID=\"" + IndependentTSID + "\", IndependentEnsembleID=\"" + IndependentEnsembleID + "\".";
            Message.printWarning ( warningLevel,
                MessageUtil.formatMessageTag(
                    command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
        }
    }
    
    int n_independent_ts = 0;
    if ( independent_tslist != null ) {
        n_independent_ts = independent_tslist.size();
    }
    if ( n_independent_ts == 0 ) {
        message = "Unable to find any independent time series to process using IndependentTSList=\"" + IndependentTSList +
        "\" IndependentTSID=\"" + IndependentTSID + "\", IndependentEnsembleID=\"" + IndependentEnsembleID + "\".";
        Message.printWarning ( warningLevel,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.WARNING,
                message,
                "Verify that the IndependentTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    
    // Make sure that the number of dependent and independent time series is consistent
    
    if ( (n_independent_ts > 1) && (n_independent_ts != nts) ) {
        message = "The number if independent time series (" + n_independent_ts +
            ") is > 1 but does not agree with the number of dependent time series (" + nts + ").";
        Message.printWarning ( warningLevel,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the IndependentTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    
	// Set period...

	String SetStart = parameters.getValue("SetStart");
	String SetEnd = parameters.getValue("SetEnd");
	DateTime SetStart_DateTime = null;
	DateTime SetEnd_DateTime = null;
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			SetStart_DateTime = TSCommandProcessorUtil.getDateTime ( SetStart, "SetStart", processor,
				status, warningLevel, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			SetEnd_DateTime = TSCommandProcessorUtil.getDateTime ( SetEnd, "SetEnd", processor,
				status, warningLevel, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
	}
	
    DateTime SetWindowStart_DateTime = null;
    if ( (SetWindowStart != null) && (SetWindowStart.length() > 0) ) {
        try {
            // The following works with ISO formats...
            SetWindowStart_DateTime = DateTime.parse ( "" + __SET_WINDOW_YEAR + "-" + SetWindowStart );
        }
        catch ( Exception e ) {
            message = "SetWindowStart \"" + SetWindowStart + "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
            Message.printWarning ( warningLevel,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            throw new InvalidCommandParameterException ( message );
        }
    }
    DateTime SetWindowEnd_DateTime = null;
    if ( (SetWindowEnd != null) && (SetWindowEnd.length() > 0) ) {
        try {
            // The following works with ISO formats...
            SetWindowEnd_DateTime = DateTime.parse ( "" + __SET_WINDOW_YEAR + "-" + SetWindowEnd );
        }
        catch ( Exception e ) {
            message = "SetWindowEnd \"" + SetWindowEnd + "\" is invalid.  Expecting MM, MM-DD, MM-DD hh, or MM-DD hh:mm";
            Message.printWarning ( warningLevel,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            throw new InvalidCommandParameterException ( message );
        }
    }

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Insufficient data to run command.";
		Message.printWarning ( warningLevel,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}

	// Now process the time series...

    String TransferHow = parameters.getValue("TransferHow");
	PropList setprops = new PropList ( "SetFromTS" );
	if ( (TransferHow != null) && !TransferHow.equals("") ) {
		setprops.set ( "TransferHow", TransferHow );
	}
    setprops.set ( "HandleMissingHow", HandleMissingHow );
    String SetFlag = parameters.getValue("SetFlag");
    if ( (SetFlag != null) && !SetFlag.isEmpty() ) {
    	setprops.set ( "SetFlag", SetFlag );
    }
    String SetFlagDesc = parameters.getValue("SetFlagDesc");
    if ( (SetFlagDesc != null) && !SetFlagDesc.isEmpty() ) {
    	setprops.set ( "SetFlagDescription", SetFlagDesc );
    }

	TS ts = null;
    TS independent_ts = null;
	for ( int its = 0; its < nts; its++ ) {
		ts = getTimeSeriesToProcess ( its, tspos, command_tag, warning_count );
		if ( ts == null ) {
			// Skip time series.
            message = "Unable to get time series at position " + tspos[its] + " - null time series.";
			Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
			continue;
		}
		
		// Do the setting...
		notifyCommandProgressListeners ( its, nts, (float)-1.0, "Setting values in " +
            ts.getIdentifier().toStringAliasAndTSID() );
        if ( n_independent_ts == 1 ) {
            // Reuse the same independent time series for all transfers...
            independent_ts = getTimeSeriesToProcess ( 0, independent_tspos, command_tag, warning_count );
        }
        else {
            // Get the time series matching the loop index...
            independent_ts = getTimeSeriesToProcess ( its, independent_tspos, command_tag, warning_count );
        }
        
        if ( independent_ts == null ) {
            // Skip time series.
            message = "Unable to get independent time series at position " + tspos[its] + " - null time series.";
            Message.printWarning(warningLevel,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
            continue;
        }
        
        // Make sure that independent and dependent time series are not the same
        
        if ( independent_ts == ts ) {
            // Skip time series.
            message = "Independent \"" + independent_ts.getIdentifierString() +
            "\" and dependent time series \"" + ts.getIdentifierString() + "\" are the same - skipping.";
            Message.printWarning(warningLevel,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                    message, "Verify selection of time series." ) );
            continue;
        }
        
		Message.printStatus ( 2, routine, "Setting \"" + ts.getIdentifier()+ "\" from \"" +
                independent_ts.getIdentifier() + "\"." );
		try {
            TSUtil.setFromTS ( ts, independent_ts, SetStart_DateTime, SetEnd_DateTime,
                SetWindowStart_DateTime, SetWindowEnd_DateTime, setprops, SetDataFlags_boolean );
		}
		catch ( Exception e ) {
			message = "Unexpected error setting time series \"" + ts.getIdentifier() + "\" from \"" +
                independent_ts.getIdentifier() + "\" (" + e + ").";
            Message.printWarning ( warningLevel,
                MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
			Message.printWarning(3,routine,e);
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
		}
        if ( RecalcLimits_boolean ) {
            try {
                warning_count = recalculateLimits( ts, processor, warningLevel, warning_count, command_tag );
            }
            catch ( Exception e ) {
                message = "Unexpected error recalculating limits for time series \"" +
                ts.getIdentifier() + "\" (" + e + ").";
                Message.printWarning ( warningLevel,
                    MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
                Message.printWarning(3,routine,e);
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "See the log file for details - report the problem to software support." ) );
            }
        }
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warningLevel,
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
    String TSList = props.getValue( "TSList" );
    String TSID = props.getValue( "TSID" );
    String EnsembleID = props.getValue( "EnsembleID" );
    String IndependentTSList = props.getValue( "IndependentTSList" );
    String IndependentTSID = props.getValue( "IndependentTSID" );
    String IndependentEnsembleID = props.getValue( "IndependentEnsembleID" );
	String SetStart = props.getValue("SetStart");
	String SetEnd = props.getValue("SetEnd");
    String SetWindowStart = props.getValue("SetWindowStart");
    String SetWindowEnd = props.getValue("SetWindowEnd");
    String TransferHow = props.getValue( "TransferHow" );
    String HandleMissingHow = props.getValue( "HandleMissingHow" );
    String SetDataFlags = props.getValue( "SetDataFlags" );
    String SetFlag = props.getValue("SetFlag");
    String SetFlagDesc = props.getValue("SetFlagDesc");
    String RecalcLimits = props.getValue( "RecalcLimits" );
	StringBuffer b = new StringBuffer ();
    if ( (TSList != null) && (TSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
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
    if ( (IndependentTSList != null) && (IndependentTSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IndependentTSList=" + IndependentTSList );
    }
    if ( (IndependentTSID != null) && (IndependentTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IndependentTSID=\"" + IndependentTSID + "\"" );
    }
    if ( (IndependentEnsembleID != null) && (IndependentEnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IndependentEnsembleID=\"" + IndependentEnsembleID + "\"" );
    }
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
    if ( (SetWindowStart != null) && (SetWindowStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SetWindowStart=\"" + SetWindowStart + "\"" );
    }
    if ( (SetWindowEnd != null) && (SetWindowEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SetWindowEnd=\"" + SetWindowEnd + "\"" );
    }
    if ( (TransferHow != null) && (TransferHow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TransferHow=" + TransferHow );
    }
    if ( (HandleMissingHow != null) && (HandleMissingHow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "HandleMissingHow=\"" + HandleMissingHow + "\"" );
    }
    if ( ( SetDataFlags != null) && (SetDataFlags.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SetDataFlags=" + SetDataFlags );
    }
    if ( ( SetFlag != null) && !SetFlag.isEmpty() ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SetFlag=\"" + SetFlag + "\"" );
    }
    if ( (SetFlagDesc != null) && !SetFlagDesc.isEmpty() ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SetFlagDesc=\"" + SetFlagDesc + "\"" );
    }
    if ( ( RecalcLimits != null) && (RecalcLimits.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "RecalcLimits=" + RecalcLimits );
    }

	return getCommandName() + "(" + b.toString() + ")";
}

}