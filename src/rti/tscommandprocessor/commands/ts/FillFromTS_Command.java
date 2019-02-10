// FillFromTS_Command - This class initializes, checks, and runs the FillFromTS() command.

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
This class initializes, checks, and runs the FillFromTS() command.
*/
public class FillFromTS_Command extends AbstractCommand implements Command
{
    
/**
Parameter values used with RecalcLimits.
*/
protected final String _False = "False";
protected final String _True = "True";

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
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String TSList = parameters.getValue ( "TSList" );
	String TSID = parameters.getValue ( "TSID" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	//String TransferHow = parameters.getValue ( "TransferHow" );
	String RecalcLimits = parameters.getValue ( "RecalcLimits" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    if ( (TSList != null) && !TSListType.ALL_MATCHING_TSID.equals(TSList) &&
            !TSListType.FIRST_MATCHING_TSID.equals(TSList) &&
            !TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        if ( TSID != null ) {
            message = "TSID should only be specified when TSList=" +
            TSListType.ALL_MATCHING_TSID.toString() + " or " +
            TSListType.FIRST_MATCHING_TSID.toString() + " or " +
            TSListType.LAST_MATCHING_TSID.toString() + ".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Do not specify the TSID parameter." ) );
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
 	if ( (FillStart != null) && !FillStart.isEmpty() && !FillStart.equalsIgnoreCase("OutputStart") && (FillStart.indexOf("${") < 0) ){
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
	if ( (FillEnd != null) && !FillEnd.equals("") && !FillEnd.equalsIgnoreCase("OutputEnd") && (FillEnd.indexOf("${") < 0)) {
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
	
    if ( (RecalcLimits != null) && !RecalcLimits.equals("") &&
            !RecalcLimits.equalsIgnoreCase( "true" ) && 
            !RecalcLimits.equalsIgnoreCase("false") ) {
        message = "The RecalcLimits parameter must be blank, " + _False + " (default), or " + _True + ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a 1-character fill flag or Auto." ) );
    }
    
	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(11);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "IndependentTSList" );
    validList.add ( "IndependentTSID" );
    validList.add ( "IndependentEnsembleID" );
    validList.add ( "FillStart" );
    validList.add ( "FillEnd" );
    validList.add ( "FillFlag" );
    validList.add ( "FillFlagDesc" );
    //valid_Vector.add ( "TransferHow" );
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
	return (new FillFromTS_JDialog ( parent, this )).ok();
}

/**
Get the time series to process.
@param its Position in time series array to get time series.
@param tspos Positions in time series processor time series array.
*/
private TS getTimeSeriesToProcess ( int its, int[] tspos, String command_tag, int warning_count )
{   String routine = getClass().getName() + ".getTimeSeriesToProcess";
    TS ts = null;
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "Index", new Integer(tspos[its]) );
    CommandProcessorRequestResultsBean bean = null;
    CommandProcessor processor = getCommandProcessor();
    String message;
    CommandStatus status = getCommandStatus();
    //int warning_level = 2;
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
    return ts;
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = getClass().getSimpleName() + ".parseCommand", message;

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
		// Old syntax where the paramters are TSID,IndependentTSID,FillStart,FillEnd,TransferHow
    	List<String> v = StringUtil.breakStringList(command_string,
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

		String TSID = ((String)v.get(1)).trim();
        String IndependentTSID = ((String)v.get(2)).trim();
		String FillStart = ((String)v.get(3)).trim();
        String FillEnd = ((String)v.get(4)).trim();
        // This parameter is of the format TransferData=...
        //String TransferHow = StringUtil.getToken(((String)v.elementAt(5)).trim(),"=",0,1);

		// Set parameters and new defaults...

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID.length() > 0 ) {
			parameters.set ( "TSID", TSID );
			parameters.setHowSet(Prop.SET_AS_RUNTIME_DEFAULT);
            // Legacy behavior was to match last matching TSID if no wildcard
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
    status.clearLog(CommandPhaseType.RUN);
    
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
        Message.printWarning( 3, routine, e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
        return warning_count;
    }
    // Get the calculated limits and set in the original data limits...
    PropList bean_PropList = bean.getResultsPropList();
    Object prop_contents = bean_PropList.getContents ( "TSLimits" );
    if ( prop_contents == null ) {
        message = "Null value from CalculateTSAverageLimits(" + ts.getIdentifierString() + ")";
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
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warningLevel = 2;
	String command_tag = "" + command_number;
	//int log_level = 3;	// Warning message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
	CommandPhaseType commandPhase = CommandPhaseType.RUN;
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
	List<TS> tslist = null;
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
		@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
        tslist = tslist0;
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
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + IndependentTSList +
        "\", TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\") from processor.";
        Message.printWarning(warningLevel,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    Object o_TSList2 = bean_PropList.getContents ( "TSToProcessList" );
    List<TS> independent_tslist = null;
    if ( o_TSList2 == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + IndependentTSList +
        "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\").";
        Message.printWarning ( warningLevel,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Verify that the IndependentTSList parameter matches one or more time series - may be OK for partial run." ) );
    }
    else {
    	@SuppressWarnings("unchecked")
		List<TS> independent_tslist0 = (List<TS>)o_TSList2;
        independent_tslist = independent_tslist0;
        if ( independent_tslist.size() == 0 ) {
            message = "No independent time series are available from processor GetTimeSeriesToProcess (TSList=\"" + IndependentTSList +
            "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\").";
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
        message = "Unable to find indices for independent time series to process using TSList=\"" + IndependentTSList +
        "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\".";
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
            message = "Unable to find indices for independent time series to process using TSList=\"" + IndependentTSList +
            "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\".";
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
        message = "Unable to find any independent time series to process using TSList=\"" + IndependentTSList +
        "\" TSID=\"" + IndependentTSID + "\", EnsembleID=\"" + IndependentEnsembleID + "\".";
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

	// Fill period...

	String FillStart = parameters.getValue("FillStart");
	String FillEnd = parameters.getValue("FillEnd");
    String FillFlag = parameters.getValue("FillFlag");
	if ( (FillFlag != null) && (FillFlag.indexOf("${") >= 0) ) {
		FillFlag = TSCommandProcessorUtil.expandParameterValue(processor, this, FillFlag);
	}
    String FillFlagDesc = parameters.getValue("FillFlagDesc");
	if ( (FillFlagDesc != null) && (FillFlagDesc.indexOf("${") >= 0) ) {
		FillFlagDesc = TSCommandProcessorUtil.expandParameterValue(processor, this, FillFlagDesc);
	}

	// Figure out the dates to use for the analysis...
	DateTime FillStart_DateTime = null;
	DateTime FillEnd_DateTime = null;

	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			FillStart_DateTime = TSCommandProcessorUtil.getDateTime ( FillStart, "FillStart", processor,
				status, warningLevel, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			FillEnd_DateTime = TSCommandProcessorUtil.getDateTime ( FillEnd, "FillEnd", processor,
				status, warningLevel, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
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
			Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
			continue;
		}
		
		// Do the setting...
		notifyCommandProgressListeners ( its, nts, (float)-1.0, "Filling time series " +
            ts.getIdentifier().toStringAliasAndTSID() );
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
            Message.printWarning(warningLevel,
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
            TSUtil.fillFromTS ( ts, independent_ts, FillStart_DateTime, FillEnd_DateTime, FillFlag, FillFlagDesc );
		}
		catch ( Exception e ) {
			message = "Unexpected error filling time series \"" + ts.getIdentifier() + "\" from \"" +
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
		        recalculateLimits( ts, processor, warningLevel, warning_count, command_tag );
		    }
	        catch ( Exception e ) {
	            message = "Unexpected error recalculating limits for time series \"" +
	            ts.getIdentifier() + " (" + e + ").";
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
	String FillStart = props.getValue("FillStart");
	String FillEnd = props.getValue("FillEnd");
	String FillFlag = props.getValue("FillFlag");
	String FillFlagDesc = props.getValue("FillFlagDesc");
    String TransferHow = props.getValue( "TransferHow" );
	//String FillFlag = props.getValue("FillFlag");
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
	if ( (FillStart != null) && (FillStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillStart=\"" + FillStart + "\"" );
	}
	if ( (FillEnd != null) && (FillEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillEnd=\"" + FillEnd + "\"" );
	}
    if ( (FillFlag != null) && (FillFlag.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FillFlag=\"" + FillFlag + "\"" );
    }
    if ( (FillFlagDesc != null) && (FillFlagDesc.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FillFlagDesc=\"" + FillFlagDesc + "\"" );
    }
    if ( (TransferHow != null) && (TransferHow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TransferHow=" + TransferHow );
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
