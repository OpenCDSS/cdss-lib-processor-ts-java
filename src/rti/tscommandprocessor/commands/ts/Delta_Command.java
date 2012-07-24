package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSUtil_Delta;
import RTi.TS.TrendType;

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
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the Delta() command.
*/
public class Delta_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;
    
/**
Constructor.
*/
public Delta_Command ()
{	super();
	setCommandName ( "Delta" );
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
{   String ExpectedTrend = parameters.getValue ( "ExpectedTrend" );
	String ResetMin = parameters.getValue ( "ResetMin" );
	String ResetMax = parameters.getValue ( "ResetMax" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String Alias = parameters.getValue ( "Alias" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    /* TODO SAM 2008-01-03 Evaluate whether need to check combinations
	if ( (TSID == null) || TSID.equals("") ) {
        message = "The time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a time series identifier." ) );
	}
    */
    // Verify that the expected trend is values that can be handled
    if ( (ExpectedTrend != null) && !ExpectedTrend.equals("") && !ExpectedTrend.equalsIgnoreCase("" + TrendType.DECREASING) &&
        !ExpectedTrend.equalsIgnoreCase("" + TrendType.INCREASING) ) {
        message = "The expected trend (" + ExpectedTrend + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the trend as blank, " + TrendType.DECREASING + ", or " + TrendType.INCREASING + "." ) );
    }
    int resetCount = 0;
    Double resetMin = null;
	if ( (ResetMin != null) && !ResetMin.equals("") ) {
        ++resetCount;
	    if  ( !StringUtil.isDouble(ResetMin) ) {
            message = "The reset minimum value (" + ResetMin + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the reset minimum as a number." ) );
	    }
	    else {
	        resetMin = Double.parseDouble(ResetMin);
	    }
	}
    if ( (ResetMax != null) && !ResetMax.equals("") ) {
        ++resetCount;
        if ( !StringUtil.isDouble(ResetMax) ) {
            message = "The reset maximum value (" + ResetMax + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the reset maximum as a number." ) );
        }
        else if ( (resetMin != null) && Double.parseDouble(ResetMax) <= resetMin.doubleValue() ) {
            message = "The reset maximum value (" + ResetMax + ") is <= ResetMin (" + ResetMin + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the reset maximum > the reset minimum." ) );
        }
    }
    if ( resetCount == 1 ) {
        message = "ResetMin and ResetMax must both be specified or specify neither.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify both or neither ResetMax and ResetMin." ) );
    }
    // Make sure that the trend is specified if the reset values are specified
    if ( (resetCount == 2) && ((ExpectedTrend == null) || ExpectedTrend.equals("")) ) {
        message = "ResetMin and ResetMax are specified but Trend is not.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify ExpectedTrend when ResetMax and ResetMin are specified." ) );
    }
	if ( (AnalysisStart != null) && !AnalysisStart.equals("") &&
		!AnalysisStart.equalsIgnoreCase("OutputStart") && !AnalysisStart.equalsIgnoreCase("OutputEnd") ) {
		try {
		    DateTime.parse(AnalysisStart);
		}
		catch ( Exception e ) {
            message = "The analysis start date/time \"" + AnalysisStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time, OutputStart, or output end." ) );
		}
	}
	if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") &&
		!AnalysisEnd.equalsIgnoreCase("OutputStart") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") ) {
		try {
		    DateTime.parse( AnalysisEnd );
		}
		catch ( Exception e ) {
            message = "The analysis end date/time \"" + AnalysisEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
		}
	}
	
    if ( (Alias == null) || Alias.equals("") ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a time series alias." ) );
    }
    
    // Check for invalid parameters...
	List<String> valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "ExpectedTrend" );
    valid_Vector.add ( "ResetMin" );
    valid_Vector.add ( "ResetMax" );
    valid_Vector.add ( "AnalysisStart" );
    valid_Vector.add ( "AnalysisEnd" );
    valid_Vector.add ( "Flag" );
    valid_Vector.add ( "Alias" );
    //valid_Vector.add ( "NewUnits" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );
    
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
	return (new Delta_JDialog ( parent, this )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
    List<TS> matchingDiscoveryTS = new Vector();
    List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return matchingDiscoveryTS;
    }
    for ( TS datats : discoveryTSList ) {
        // Use the most generic for the base class...
        if ( (c == TS.class) || (c == datats.getClass()) ) {
            matchingDiscoveryTS.add(datats);
        }
    }
    return matchingDiscoveryTS;
}

// parseCommand() from the base class

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "Delta_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;  // Level for non-use messages for log file.

	// Make sure there are time series available to operate on...
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String ExpectedTrend = parameters.getValue ( "ExpectedTrend" );
    TrendType trendType = null;
    if ( (ExpectedTrend != null) && !ExpectedTrend.equals("") ) {
        trendType = TrendType.valueOfIgnoreCase(ExpectedTrend);
    }
	String ResetMin = parameters.getValue ( "ResetMin" );
	Double resetMin = null;
	if ( (ResetMin != null) && !ResetMin.equals("") ) {
	    resetMin = Double.parseDouble(ResetMin);
	}
	String ResetMax = parameters.getValue ( "ResetMax" );
    Double resetMax = null;
    if ( (ResetMax != null) && !ResetMax.equals("") ) {
        resetMax = Double.parseDouble(ResetMax);
    }
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String Flag = parameters.getValue ( "Flag" );
	String Alias = parameters.getValue ( "Alias" );
	//String NewUnits = parameters.getValue ( "NewUnits" );

	// Figure out the dates to use for the analysis.
	// Default of null means to analyze the full period.
	DateTime AnalysisStart_DateTime = null;
	DateTime AnalysisEnd_DateTime = null;
	
	try {
	if ( (AnalysisStart != null) && !AnalysisStart.equals("") ) {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", AnalysisStart );
		CommandProcessorRequestResultsBean bean = null;
		try { bean =
			processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting AnalysisStart DateTime(DateTime=" +
			AnalysisStart + ") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for AnalysisStart DateTime(DateTime=" +
			AnalysisStart +	") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	AnalysisStart_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "AnalysisStart \"" + AnalysisStart + "\" is invalid.";
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
		throw new InvalidCommandParameterException ( message );
	}
	
	try {
	if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") ) {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", AnalysisEnd );
		CommandProcessorRequestResultsBean bean = null;
		try { bean =
			processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting AnalysisEnd DateTime(DateTime=" + AnalysisEnd + ") from processor.";
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
			throw new InvalidCommandParameterException ( message );
		}

		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for AnalysisStart DateTime(DateTime=" +
			AnalysisStart +	"\") returned from processor.";
			Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
			throw new InvalidCommandParameterException ( message );
		}
		else {	AnalysisEnd_DateTime = (DateTime)prop_contents;
		}
	}
	}
	catch ( Exception e ) {
		message = "AnalysisEnd \"" + AnalysisEnd + "\" is invalid.";
		Message.printWarning(warning_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, message );
        status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
		throw new InvalidCommandParameterException ( message );
	}

	// Get the time series to process.  Allow TSID to be a pattern or specific time series...

    List<TS> tslist = null;
	if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	    // Get the discovery time series list from all time series above this command
	    tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
	        (TSCommandProcessor)processor, this, TSList, TSID, null, EnsembleID );
	}
	else if ( commandPhase == CommandPhaseType.RUN ) {
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
    		Message.printWarning(log_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
    	}
        if ( bean == null ) {
            Message.printStatus ( 2, routine, "Bean is null.");
        }
    	PropList bean_PropList = bean.getResultsPropList();
    	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
    	if ( o_TSList == null ) {
    		message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
    		"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
    		Message.printWarning ( log_level,
    		MessageUtil.formatMessageTag(
    		command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    	}
    	else {
            tslist = (List)o_TSList;
    		if ( tslist.size() == 0 ) {
    			message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
    			"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
    			Message.printWarning ( log_level,
    					MessageUtil.formatMessageTag(
    							command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message,
                                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    		}
    	}
	}
	
	int nts = tslist.size();
	if ( nts == 0 ) {
		message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
	}

	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Command parameter data has errors.  Unable to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		throw new CommandException ( message );
	}

	// Now process the time series...

	TS ts = null;
	Object o_ts = null;
	List<TS> discoverTSList = new Vector();
    boolean createData = true; // Run full command
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Just want time series headers initialized, but not actually do the processing
        createData = false;
    }
	for ( int its = 0; its < nts; its++ ) {
		// The time series to process, from the list that was returned above.
		o_ts = tslist.get(its);
		if ( o_ts == null ) {
			message = "Time series to process is null.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
			// Go to next time series.
			continue;
		}
		ts = (TS)o_ts;
		
		try {
		    // Create the delta time series...
		    notifyCommandProgressListeners ( its, nts, (float)-1.0, "Creating delta for " +
	            ts.getIdentifier().toStringAliasAndTSID() );
			Message.printStatus ( 2, routine, "Creating delta time series from \"" + ts.getIdentifier()+ "\"." );
			TSUtil_Delta tsu = new TSUtil_Delta ( ts, AnalysisStart_DateTime, AnalysisEnd_DateTime,
			    trendType, resetMin, resetMax, Flag, createData );
			TS newts = tsu.delta();
			if ( (Alias != null) && !Alias.equals("") ) {
			    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                    processor, newts, Alias, status, commandPhase);
                newts.setAlias ( alias );
			}
		
			// Append problems in the low-level code to command status log
            for ( String problem : tsu.getProblems() ) {
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,problem );
                // No recommendation since it is a user-defined check
                // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
                status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.WARNING,
                    problem, "" ) );
            }

            if ( commandPhase == CommandPhaseType.DISCOVERY ) {
                discoverTSList.add ( newts );
            }
            else if ( commandPhase == CommandPhaseType.RUN ) {
                TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, newts );
            }    
		}
		catch ( Exception e ) {
			message = "Unexpected error creating delta time series for \""+ ts.getIdentifier() + "\" (" + e + ").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count),routine,message );
			Message.printWarning(3,routine,e);
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
		}
	}
	
	// Set the discovery time series list
	
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Just want time series headers initialized
        setDiscoveryTSList ( discoverTSList );
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
		throw new CommandWarningException ( message );
	}
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
@param discoveryTSList list of time series created in discovery mode
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
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
    String ExpectedTrend = props.getValue("ExpectedTrend");
	String ResetMin = props.getValue("ResetMin");
	String ResetMax = props.getValue("ResetMax");
	String AnalysisStart = props.getValue("AnalysisStart");
	String AnalysisEnd = props.getValue("AnalysisEnd");
	String Flag = props.getValue("Flag");
	String Alias = props.getValue("Alias");
	//String NewUnits = props.getValue("NewUnits");
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
    if ( (ExpectedTrend != null) && (ExpectedTrend.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExpectedTrend=" + ExpectedTrend );
    }
	if ( (ResetMin != null) && (ResetMin.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ResetMin=" + ResetMin );
	}
    if ( (ResetMax != null) && (ResetMax.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ResetMax=" + ResetMax );
    }
	if ( (AnalysisStart != null) && (AnalysisStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AnalysisStart=\"" + AnalysisStart + "\"" );
	}
	if ( (AnalysisEnd != null) && (AnalysisEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "AnalysisEnd=\"" + AnalysisEnd + "\"" );
	}
    if ( (Flag != null) && (Flag.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Flag=\"" + Flag + "\"" );
    }
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }
	/*
	if ( (NewUnits != null) && (NewUnits.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewUnits=\"" + NewUnits + "\"" );
	}
	*/
	return getCommandName() + "(" + b.toString() + ")";
}

}