package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

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
import RTi.Util.Time.DateTime;

/**
<p>
This class initializes, checks, and runs the FillFromTS() command.
</p>
*/
public class FillFromTS_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public FillFromTS_Command ()
{	super();
	setCommandName ( "FillFromTS" );
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
{	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	//String TransferHow = parameters.getValue ( "TransferHow" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
	if ( (TSList != null) && !TSListType.ALL_MATCHING_TSID.equals(TSList) ) {
		if ( TSID != null ) {
            message = "TSID should only be specified when TSList=" + TSListType.ALL_MATCHING_TSID.toString() + ".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Do not specify the TSID parameter when TList=" + TSListType.ALL_MATCHING_TSID.toString() ) );
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
 	if ( (FillStart != null) && !FillStart.equals("") && !FillStart.equalsIgnoreCase("OutputStart")){
		try {	DateTime.parse(FillStart);
		}
		catch ( Exception e ) {
            message = "The fill start date/time \"" + FillStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	if ( (FillEnd != null) && !FillEnd.equals("") && !FillEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse( FillEnd);
		}
		catch ( Exception e ) {
            message = "The fill end date/time \"" + FillStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
    /*
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
    */
    
	// Check for invalid parameters...
    Vector valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "IndependentTSList" );
    valid_Vector.add ( "IndependentTSID" );
    valid_Vector.add ( "IndependentEnsembleID" );
    valid_Vector.add ( "FillStart" );
    valid_Vector.add ( "FillEnd" );
    //valid_Vector.add ( "TransferHow" );
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
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new FillFromTS_JDialog ( parent, this )).ok();
}

/**
Get the time series to process.
@param its Position in time series array to get time series.
@param tspos Positions in time series processor time series array.
*/
private TS getTimeSeriesToProcess ( int its, int[] tspos, String command_tag, int warning_count )
{   String routine = "FillFromTS_Command.getTimeSeriesToProcess";
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
        message = "Error requesting GetTimeSeries(Index=" + tspos[its] + ") from processor.";
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
        message = "Null value for GetTimeSeries(Index=" + tspos[its] + ") returned from processor.";
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
	String routine = "FillFromTS_Command.parseCommand", message;

	if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()")) {
        // Current syntax...
        super.parseCommand( command_string);
        // Recently added TSList so handle it properly
        PropList parameters = getCommandParameters();
        String TSList = parameters.getValue ( "TSList");
        String TSID = parameters.getValue ( "TSID");
        if ( ((TSList == null) || (TSList.length() == 0)) && // TSList not specified
                ((TSID != null) && (TSID.length() != 0)) ) { // but TSID is specified
            // Assume old-style where TSList was not specified but TSID was...
            parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
        }
    }
    else {
		// TODO SAM 2005-09-08 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new
		// syntax.
		//
		// Old syntax where the paramters are TSID,IndependentTSID,SetStart,SetEnd,TransferHow
		Vector v = StringUtil.breakStringList(command_string,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS |	StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		if ( ntokens != 5 ) {
			// Command name, TSID, and constant...
			message = "Syntax error in \"" + command_string + "\".  4 parameters expected.";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}

		// Get the individual tokens of the expression...

		String TSID = ((String)v.elementAt(1)).trim();
        String IndependentTSID = ((String)v.elementAt(2)).trim();
		String FillStart = ((String)v.elementAt(3)).trim();
        String FillEnd = ((String)v.elementAt(4)).trim();
        // This parameter is of the format TransferData=...
        //String TransferHow = StringUtil.getToken(((String)v.elementAt(5)).trim(),"=",0,1);

		// Set parameters and new defaults...

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID.length() > 0 ) {
			parameters.set ( "TSID", TSID );
			parameters.setHowSet(Prop.SET_AS_RUNTIME_DEFAULT);
			parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
		}
        if ( IndependentTSID.length() > 0 ) {
            parameters.set ( "IndependentTSID", IndependentTSID );
            parameters.setHowSet(Prop.SET_AS_RUNTIME_DEFAULT);
            parameters.set ( "IndependentTSList", TSListType.ALL_MATCHING_TSID.toString() );
        }
        // Phase out * as default, blank is current default.
        if ( !FillStart.equals("*")) {
            parameters.set ( "FillStart", FillStart );
        }
        if ( !FillEnd.equals("*")) {
            parameters.set ( "FillEnd", FillEnd );
        }
        //parameters.set ( "TransferHow", TransferHow );
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
}

/**
Run the command.
@param command_number number of command to run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "FillFromTS_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Warning message level for non-user messgaes

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);

	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );

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
                        message, "Report the problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	Vector tslist = null;
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
        tslist = (Vector)o_TSList;
		if ( tslist.size() == 0 ) {
            message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
			Message.printWarning ( warning_level,
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
	
	int nts = 0;
	if ( tslist != null ) {
		nts = tslist.size();
	}
	if ( nts == 0 ) {
        message = "Unable to find any time series to process using TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
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
    String IndependentEnsembleID = parameters.getValue ( "IndependentEnsembleID" );
    request_params = new PropList ( "" );
    request_params.set ( "TSList", IndependentTSList );
    request_params.set ( "TSID", IndependentTSID );
    request_params.set ( "EnsembleID", IndependentEnsembleID );
    try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + IndependentTSList +
        "\", TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\") from processor.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    Object o_TSList2 = bean_PropList.getContents ( "TSToProcessList" );
    Vector independent_tslist = null;
    if ( o_TSList2 == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + IndependentTSList +
        "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\").";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the IndependentTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    else {
        independent_tslist = (Vector)o_TSList2;
        if ( independent_tslist.size() == 0 ) {
            message = "No independent time series are available from processor GetTimeSeriesToProcess (TSList=\"" + IndependentTSList +
            "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\").";
            Message.printWarning ( warning_level,
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
        message = "Unable to find indices for independent time series to process using TSList=\"" + IndependentTSList +
        "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
    }
    else {
        independent_tspos = (int [])o_Indices2;
        if ( independent_tspos.length == 0 ) {
            message = "Unable to find indices for independent time series to process using TSList=\"" + IndependentTSList +
            "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\".";
            Message.printWarning ( warning_level,
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
        message = "Unable to find any independent time series to process using TSList=\"" + IndependentTSList +
        "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\".";
        Message.printWarning ( warning_level,
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
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the IndependentTSList parameter matches one or more time series - may be OK for partial run." ) );
    }

	// Fill period...

	String FillStart = parameters.getValue("FillStart");
	String FillEnd = parameters.getValue("FillEnd");

	// Figure out the dates to use for the analysis...
	DateTime FillStart_DateTime = null;
	DateTime FillEnd_DateTime = null;

	try {
	if ( FillStart != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", FillStart );
		bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting FillStart DateTime(DateTime=" +	FillStart + ") from processor.";
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
			message = "Null value for FillStart DateTime(DateTime=" + FillStart + "\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	FillStart_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "FillStart \"" + FillStart + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time or OutputStart." ) );
		throw new InvalidCommandParameterException ( message );
	}
	
	try {
	if ( FillEnd != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", FillEnd );
		bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting FillEnd DateTime(DateTime=" + FillEnd + "\") from processor.";
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
			message = "Null value for FillEnd DateTime(DateTime=" + FillEnd +	"\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	FillEnd_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "FillEnd \"" + FillEnd + "\" is invalid.";
		Message.printWarning(warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time or OutputEnd." ) );
		throw new InvalidCommandParameterException ( message );
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
	PropList setprops = new PropList ( "FillFromTS" );
	if ( (TransferHow != null) && !TransferHow.equals("") ) {
		setprops.set ( "TransferHow", TransferHow );
	}
    */

	TS ts = null;
    TS independent_ts = null;
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
		
		// Do the setting...
        int independent_tspos_touse = -1;
        if ( n_independent_ts == 1 ) {
            // Reuse the same independent time series for all transfers...
            independent_tspos_touse = 0;
        }
        else {
            // Ensemble - get the time series matching the loop index...
            independent_tspos_touse = its; 
        }
        independent_ts = getTimeSeriesToProcess ( independent_tspos_touse, independent_tspos, command_tag, warning_count );
        
        if ( independent_ts == null ) {
            // Skip time series.
            message = "Unable to get independent time series at position " + tspos[independent_tspos_touse] + " - null time series.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
            continue;
        }
        
		Message.printStatus ( 2, routine, "Filling \"" + ts.getIdentifier()+ "\" from \"" +
                independent_ts.getIdentifier() + "\"." );
		try {
            TSUtil.fillFromTS ( ts, independent_ts, FillStart_DateTime, FillEnd_DateTime );
		}
		catch ( Exception e ) {
			message = "Unexpected error filling time series \"" + ts.getIdentifier() + "\" from \"" +
                independent_ts.getIdentifier() + "\".";
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
    String TSList = props.getValue( "TSList" );
    String TSID = props.getValue( "TSID" );
    String EnsembleID = props.getValue( "EnsembleID" );
    String IndependentTSList = props.getValue( "IndependentTSList" );
    String IndependentTSID = props.getValue( "IndependentTSID" );
    String IndependentEnsembleID = props.getValue( "IndependentEnsembleID" );
	String SetStart = props.getValue("SetStart");
	String SetEnd = props.getValue("SetEnd");
    String TransferHow = props.getValue( "TransferHow" );
	//String FillFlag = props.getValue("FillFlag");
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
    if ( (TransferHow != null) && (TransferHow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TransferHow=" + TransferHow );
    }

	return getCommandName() + "(" + b.toString() + ")";
}

}
