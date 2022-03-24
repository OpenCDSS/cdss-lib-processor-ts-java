// Cumulate_Command - This class initializes, checks, and runs the Cumulate() command.

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

package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.CumulateMissingType;
import RTi.TS.TS;
import RTi.TS.TSUtil_CumulateTimeSeries;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
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
This class initializes, checks, and runs the Cumulate() command.
*/
public class Cumulate_Command extends AbstractCommand
{

/**
Strings used with the command.
*/
protected final String _CarryForwardIfMissing = "CarryForwardIfMissing";
protected final String _SetMissingIfMissing = "SetMissingIfMissing";

// TODO SAM 2012-07-25 Maybe need a "DataValueOrZero" option in case the data value is missing.
/**
Possible values for the ResetValue parameter.
*/
protected final String _Zero = "0";
protected final String _DataValue = "DataValue";

/**
Year to prepend to reset string (because Reset parameter does not include year).
Use a leap year so that February 29 is valid.
*/
private String __resetYear = "2000";

/**
Constructor.
*/
public Cumulate_Command ()
{	super();
	setCommandName ( "Cumulate" );
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
{	//String TSID = parameters.getValue ( "TSID" );
	String HandleMissingHow = parameters.getValue ( "HandleMissingHow" );
    String Reset = parameters.getValue ( "Reset" );
    String ResetValue = parameters.getValue ( "ResetValue" );
    String AllowMissingCount = parameters.getValue ( "AllowMissingCount" );
    String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    /* TODO SAM 2008-01-03 Evaluate combination with TSList.
	if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide a time series identifier." ) );
	}
    */
	if ( (HandleMissingHow != null) && !HandleMissingHow.equals("") ) {
	    if ( CumulateMissingType.valueOfIgnoreCase(HandleMissingHow) == null ) {
            message = "The HandleMissingHow parameter is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "HandleMissingHow, if specified, must be " +
                    CumulateMissingType.CARRY_FORWARD + " or " + CumulateMissingType.SET_MISSING ) );
	    }
	}
    if ( (Reset != null) && !Reset.isEmpty() && (Reset.indexOf("${") < 0) ) {
    	// Can only check if not a property.
        try {
            DateTime.parse(__resetYear + "-" + Reset);
        }
        catch ( Exception e ) {
            message = "The reset date/time \"" + Reset + "\" (prepended with " +
                __resetYear + "-) is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time (use zeros for year)." ) );
        }
    }
    if ( (ResetValue != null) && !ResetValue.equals("") &&
        !ResetValue.equalsIgnoreCase(_DataValue) &&
        !StringUtil.isDouble(ResetValue)) {
        message = "The ResetValue parameter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "ResetValue, if specified, must be " +
                _DataValue + " or a number." ) );
    }
    
    if ( (AllowMissingCount != null) && !AllowMissingCount.equals("") ) {
        if ( !StringUtil.isInteger(AllowMissingCount) ) {
            message = "The AllowMissingCount value (" + AllowMissingCount + ") is not an integer.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an integer for AllowMissingCount." ) );
        }
        else {
            // Make sure it is an allowable value >= 0.
            int i = Integer.parseInt(AllowMissingCount);
            if ( i < 0 ) {
                message = "The AllowMissingCount value (" + AllowMissingCount + ") must be >= 0.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a value >= 0." ) );
            }
        }
    }
    
    if ( (MinimumSampleSize != null) && !MinimumSampleSize.equals("") ) {
        if ( !StringUtil.isInteger(MinimumSampleSize) ) {
            message = "The MinimumSampleSize value (" + MinimumSampleSize + ") is not an integer.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an integer for MinimumSampleSize." ) );
        }
        else {
            // Make sure it is an allowable value >= 0.
            int i = Integer.parseInt(MinimumSampleSize);
            if ( i <= 0 ) {
                message = "The MinimumSampleSize value (" + MinimumSampleSize + ") must be >= 1.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a value >= 1." ) );
            }
        }
    }
    
    // Check for invalid parameters.
    List<String> validList = new ArrayList<String>(8);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "HandleMissingHow" );
    validList.add ( "Reset" );
    validList.add ( "ResetValue" );
    validList.add ( "AllowMissingCount" );
    validList.add ( "MinimumSampleSize" );
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
	return (new Cumulate_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports old syntax and new parameter-based syntax.
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax.
        super.parseCommand( command_string);
        // Recently added TSList so handle it properly.
        PropList parameters = getCommandParameters();
        String TSList = parameters.getValue ( "TSList");
        String TSID = parameters.getValue ( "TSID");
        if ( ((TSList == null) || (TSList.length() == 0)) && // TSList not specified,
                ((TSID != null) && (TSID.length() != 0)) ) { // but TSID is specified.
            // Assume old-style where TSList was not specified but TSID was.
            if ( TSID.equals("*") ) {
                parameters.set ( "TSList", TSListType.ALL_TS.toString() );
                // TSID is not needed.
                parameters.unSet( "TSID" );
            }
            else {
                parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
                // TSID that was set is OK to leave.
            }
        }
    }
    else {
		//TODO SAM 2005-08-24 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
		//
		// Old syntax without named parameters.
    	List<String> v = StringUtil.breakStringList ( command_string,"(),",StringUtil.DELIM_SKIP_BLANKS );
		String TSID = "";
		String HandleMissingHow = "";
		if ( (v != null) && (v.size() == 3) ) {
			// Second field is identifier.
			TSID = ((String)v.get(1)).trim();
			// Third field has missing data type.
			HandleMissingHow = ((String)v.get(2)).trim();
		}

		// Set parameters and new defaults.

		PropList parameters = new PropList ( getCommandName() );
		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		if ( TSID.length() > 0 ) {
		    if ( TSID.equals("*") ) {
		        parameters.set ( "TSList", TSListType.ALL_TS.toString() );
		    }
		    else {
		        parameters.set ( "TSID", TSID );
		        // Old style was to match the TSID.
		        parameters.set ( "TSList", TSListType.ALL_MATCHING_TSID.toString() );
		    }
		}
		if ( HandleMissingHow.length() > 0 ) {
			parameters.set ( "HandleMissingHow", HandleMissingHow);
		}
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;	// Warning level for non-user messages.

	// Make sure there are time series available to operate on.

	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    Boolean clearStatus = new Boolean(true); // default
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
	if ( (commandPhase == CommandPhaseType.RUN) && (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
	String HandleMissingHow = parameters.getValue ( "HandleMissingHow" );
	CumulateMissingType handleMissingHow = CumulateMissingType.valueOfIgnoreCase(HandleMissingHow);
    String Reset = parameters.getValue ( "Reset" );
	Reset = TSCommandProcessorUtil.expandParameterValue(processor, this, Reset);
    DateTime resetDateTime = null;
    if ( (Reset != null) && !Reset.isEmpty() ) {
    	// Reset uses a property.
        try {
            resetDateTime = DateTime.parse(__resetYear + "-" + Reset);
        }
        catch ( Exception e ) {
            message = "The reset date/time \"" + Reset + "\" (prepended with " +
                __resetYear + "-) is not a valid date/time.";
            Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the reset as MM, MM-DD, for example." ) );
        }
    }
    String ResetValue = parameters.getValue ( "ResetValue" );
    boolean resetValueToDataValue = false;
    Double ResetValue_Double = null;
    if ( ResetValue != null ) {
        if ( StringUtil.isDouble(ResetValue) ) {
            ResetValue_Double = new Double(ResetValue);
        }
        else if ( ResetValue.equalsIgnoreCase(_DataValue) ) {
            resetValueToDataValue = true;
        }
    }
    String AllowMissingCount = parameters.getValue ( "AllowMissingCount" );
    Integer AllowMissingCount_Integer = null;
    if ( (AllowMissingCount != null) && StringUtil.isInteger(AllowMissingCount) ) {
        AllowMissingCount_Integer = new Integer(AllowMissingCount);
    }
    String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
    Integer MinimumSampleSize_Integer = null;
    if ( (MinimumSampleSize != null) && StringUtil.isInteger(MinimumSampleSize) ) {
        MinimumSampleSize_Integer = new Integer(MinimumSampleSize);
    }

	// Get the time series to process.  Allow TSID to be a pattern or specific time series.

	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
	CommandProcessorRequestResultsBean bean = null;
	try { bean =
		processor.processRequest( "GetTimeSeriesToProcess", request_params);
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
	List<TS> tslist = null;
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
		@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
		tslist = tslist0;
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
	else {	tspos = (int [])o_Indices;
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
	
	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series).
		message = "Insufficient data to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		// It is OK if no time series.
	}

	// Now process the time series.

	int nts = 0;
	if ( tslist != null ) {
		nts = tslist.size();
	}
	TS ts = null;
	for ( int its = 0; its < nts; its++ ) {
		request_params = new PropList ( "" );
		request_params.setUsingObject ( "Index", new Integer(tspos[its]) );
		bean = null;
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
		}
		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "TS" );
		if ( prop_contents == null ) {
            message = "Null value for GetTimeSeries(Index=" + tspos[its] + ") returned from processor.";
			Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
		}
		else {	ts = (TS)prop_contents;
		}
		
		// TODO SAM 2007-02-17 Evaluate whether to print warning if null TS.
		
		DateTime analysisStart = null; // Not yet supported.
		DateTime analysisEnd = null;
		try {
            // Do the processing.
            notifyCommandProgressListeners ( its, nts, (float)-1.0, "Cumulating time series " +
                ts.getIdentifier().toStringAliasAndTSID() );
			Message.printStatus ( 2, routine, "Cumulating \"" + ts.getIdentifier() + "\", Reset=" + resetDateTime +
				", ResetValue=" + ResetValue);
			TSUtil_CumulateTimeSeries u = new TSUtil_CumulateTimeSeries( ts, analysisStart, analysisEnd,
			    handleMissingHow, resetDateTime, ResetValue_Double, resetValueToDataValue, AllowMissingCount_Integer,
			    MinimumSampleSize_Integer );
            u.cumulate ();
		}
		catch ( Exception e ) {
			message = "Unexpected error cumulating time series \""+	ts.getIdentifier() + "\" (" + e + ").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count),routine,message );
			Message.printWarning(3,routine,e);
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
		}
	}

	// Resave the data to the processor so that appropriate actions are taken.
	// TODO SAM 2005-08-25 Is this needed?
	//_processor.setPropContents ( "TSResultsList", TSResultsList );

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
	String HandleMissingHow = props.getValue("HandleMissingHow");
	String Reset = props.getValue("Reset");
	String ResetValue = props.getValue("ResetValue");
    String AllowMissingCount = props.getValue( "AllowMissingCount" );
    String MinimumSampleSize = props.getValue( "MinimumSampleSize" );
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
	if ( (HandleMissingHow != null) && (HandleMissingHow.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "HandleMissingHow=" + HandleMissingHow );
	}
	if ( (Reset != null) && (Reset.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Reset=\"" + Reset + "\"" );
	}
    if ( (ResetValue != null) && (ResetValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ResetValue=" + ResetValue );
    }
    if ( (AllowMissingCount != null) && (AllowMissingCount.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AllowMissingCount=" + AllowMissingCount );
    }
    if ( (MinimumSampleSize != null) && (MinimumSampleSize.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MinimumSampleSize=" + MinimumSampleSize );
    }
	
	return getCommandName() + "(" + b.toString() + ")";
}

}