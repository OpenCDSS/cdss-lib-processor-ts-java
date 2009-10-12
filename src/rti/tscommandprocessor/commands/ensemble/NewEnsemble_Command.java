package rti.tscommandprocessor.commands.ensemble;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSUtil_NewEnsemble;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
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
import RTi.Util.IO.PropList;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the NewEnsemble() command.
*/
public class NewEnsemble_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
Values for the CopyTimeSeries parameter.
*/
protected final String _False = "False";
protected final String _True = "True";
    
/**
TSEnsemble created in discovery mode (basically to get the identifier for other commands).
*/
private TSEnsemble __tsensemble = null;

/**
Constructor.
*/
public NewEnsemble_Command ()
{	super();
	setCommandName ( "NewEnsemble" );
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
    String InputStart = parameters.getValue ( "InputStart" );
    String InputEnd = parameters.getValue ( "InputEnd" );
    String NewEnsembleID = parameters.getValue ( "NewEnsembleID" );
    String CopyTimeSeries = parameters.getValue ( "CopyTimeSeries" );
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
    
    /* TODO SAM 2008-01-04 Evaluate need
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
	if ( (NewEnsembleID == null) || NewEnsembleID.equals("") ) {
        message = "The NewEnsembleID is required but has not been specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the NewEnsembleID." ) );
	}
	if ( (InputStart != null) && !InputStart.equals("") ){
		try {
            DateTime.parse(InputStart);
		}
		catch ( Exception e ) {
            message = "The input start date/time \"" + InputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time." ) );
		}
	}
	if ( (InputEnd != null) && !InputEnd.equals("") ) {
		try {
		    DateTime.parse( InputEnd);
		}
		catch ( Exception e ) {
            message = "The input end date/time \"" + InputEnd + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time." ) );
		}
	}
	if ( (InputStart != null) && !InputStart.equals("") ) {
	    message = "The input start parameter is currently disabled due to technical issues.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Change the period before using the command or contact software support if this feature is needed." ) );
	}
    if ( (InputEnd != null) && !InputEnd.equals("") ) {
        message = "The input end parameter is currently disabled due to technical issues.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Change the period before using the command orontact software support if this feature is needed." ) );
    }
	
    if ( (CopyTimeSeries != null) && !CopyTimeSeries.equals("") &&
        !CopyTimeSeries.equalsIgnoreCase(_True) &&
        !CopyTimeSeries.equalsIgnoreCase(_False) ) {
        message = "The CopyTimeSeries parameter (" + CopyTimeSeries + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify as " + _True + " or " + _False + " (default)." ) );
    }
    
    if ( (((InputStart != null) && !InputStart.equals("")) ||
        ((InputEnd != null) && !InputEnd.equals(""))) &&
        (CopyTimeSeries != null) && !CopyTimeSeries.equals("") ) {
        message = "The CopyTimeSeries parameter must be true if the input period is set.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify CopyTimeSeries as " + _True + " or don't set the input period." ) );
    }
    
    if ( (CopyTimeSeries != null) && !CopyTimeSeries.equals("") ) {
        message = "The ability to copy time series into the ensemble is currently disabled due to technical issues.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Change the period before using the command or contact software support if this feature is needed." ) );
    }
    
	// Check for invalid parameters...
	List valid_Vector = new Vector();
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "NewEnsembleID" );
    valid_Vector.add ( "NewEnsembleName" );
    valid_Vector.add ( "InputStart" );
    valid_Vector.add ( "InputEnd" );
    valid_Vector.add ( "CopyTimeSeries" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new NewEnsemble_JDialog ( parent, this )).ok();
}

/**
Return the ensemble that is read by this class when run in discovery mode.
*/
private TSEnsemble getDiscoveryEnsemble()
{
    return __tsensemble;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of TSEnsemble objects.
*/
public List getObjectList ( Class c )
{   TSEnsemble tsensemble = getDiscoveryEnsemble();
    List v = null;
    if ( (tsensemble != null) && (c == tsensemble.getClass()) ) {
        v = new Vector();
        v.add ( tsensemble );
        Message.printStatus ( 2, "", "Added ensemble to object list: " + tsensemble.getEnsembleID());
    }
    return v;
}

// parseCommand() inherited from base class

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
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
@param command_number number of command to run.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "NewEnsemble_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Warning message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryEnsemble ( null );
    }
    
    List<TS> tslist = null; // Time series to add to the new ensemble
    int nts = 0; // Number of time series
    DateTime InputStart_DateTime = null;
    DateTime InputEnd_DateTime = null;
    String NewEnsembleID = parameters.getValue("NewEnsembleID");
    String NewEnsembleName = parameters.getValue("NewEnsembleName");
    if ( NewEnsembleName == null ) {
        NewEnsembleName = ""; // default
    }
    String CopyTimeSeries = parameters.getValue("CopyTimeSeries");
    if ( CopyTimeSeries == null ) {
        CopyTimeSeries = _False; // default
    }
    boolean CopyTimeSeries_boolean = false;
    if ( CopyTimeSeries.equalsIgnoreCase(_True) ) {
        CopyTimeSeries_boolean = true;
    }
    // FIXME SAM 2009-10-10 Always make false until technical issues can be resolved
    CopyTimeSeries_boolean = false;

    if ( commandPhase == CommandPhaseType.RUN ) {
        // Get all the necessary data
    	String TSList = parameters.getValue ( "TSList" );
        if ( (TSList == null) || TSList.equals("") ) {
            TSList = null;  // Default is don't add time series
        }
    	String TSID = parameters.getValue ( "TSID" );
        String EnsembleID = parameters.getValue ( "EnsembleID" );
    
    	// Get the time series to process...
    	
        if ( TSList != null ) {
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
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
        	}
        	PropList bean_PropList = bean.getResultsPropList();
        	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
        	if ( o_TSList == null ) {
                message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
        		Message.printWarning ( warning_level,
        		MessageUtil.formatMessageTag(
        		command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
        	}
        	else {
                tslist = (List)o_TSList;
                /* TODO SAM 2009-10-08 For now this is OK
        		if ( tslist.size() == 0 ) {
        			message = "Unable to find time series to process using TSList=\"" + TSList +
        			"\" TSID=\"" + TSID + "\".";
        			Message.printWarning ( warning_level,
        			    MessageUtil.formatMessageTag(
        			        command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.WARNING,
                            message,
                            "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
        		}
        		*/
        	}
        	if ( tslist != null ) {
        		nts = tslist.size();
        	}
            /* TODO SAM 2009-10-08 For now this is OK
        	if ( nts == 0 ) {
                message = "Unable to find indices for time series to process using TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\".";
        		Message.printWarning ( warning_level,
        		MessageUtil.formatMessageTag(
        		command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                        message,
                        "Verify that the TSList parameter matches one or more time series - may be OK for partial run." ) );
        	}
        	*/
        }
    
    	// Input period...
    
    	String InputStart = parameters.getValue("InputStart");
    	String InputEnd = parameters.getValue("InputEnd");
    
    	// Figure out the dates to use for the analysis...
    
    	try {
    	if ( InputStart != null ) {
    		PropList request_params = new PropList ( "" );
    		request_params.set ( "DateTime", InputStart );
    		CommandProcessorRequestResultsBean bean = null;
    		try {
                bean = processor.processRequest( "DateTime", request_params);
    		}
    		catch ( Exception e ) {
    			message = "Error requesting InputStart DateTime(DateTime=" +	InputStart + ") from processor.";
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
    			message = "Null value for InputStart DateTime(DateTime=" + InputStart + "\") returned from processor.";
    			Message.printWarning(log_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    			throw new InvalidCommandParameterException ( message );
    		}
    		else {	InputStart_DateTime = (DateTime)prop_contents;
    		}
    	}
    	}
    	catch ( Exception e ) {
    		message = "InputStart \"" + InputStart + "\" is invalid.";
    		Message.printWarning(warning_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time." ) );
    		throw new InvalidCommandParameterException ( message );
    	}
    	
    	try {
    	if ( InputEnd != null ) {
    		PropList request_params = new PropList ( "" );
    		request_params.set ( "DateTime", InputEnd );
    		CommandProcessorRequestResultsBean bean = null;
    		try {
                bean = processor.processRequest( "DateTime", request_params);
    		}
    		catch ( Exception e ) {
    			message = "Error requesting InputEnd DateTime(DateTime=" + InputEnd + "\") from processor.";
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
    			message = "Null value for InputEnd DateTime(DateTime=" + InputEnd +	"\") returned from processor.";
    			Message.printWarning(log_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a valid date/time." ) );
    			throw new InvalidCommandParameterException ( message );
    		}
    		else {
    		    InputEnd_DateTime = (DateTime)prop_contents;
    		}
    	}
    	}
    	catch ( Exception e ) {
    		message = "InputEnd \"" + InputEnd + "\" is invalid.";
    		Message.printWarning(warning_level,
    			MessageUtil.formatMessageTag( command_tag, ++warning_count),
    			routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time." ) );
    		throw new InvalidCommandParameterException ( message );
    	}
    }
        
    if ( warning_count > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog ( commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

	// Now process the time series...
    
    try {
        TSEnsemble ensemble = null;
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create a discovery ensemble with ID and name
            ensemble = new TSEnsemble ( NewEnsembleID, NewEnsembleName, null );
            setDiscoveryEnsemble ( ensemble );
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
    		// Convert time series to ensemble...
    		Message.printStatus ( 2, routine, "Using " + nts + " time series to create ensemble \"" +
    		    NewEnsembleID + "\"." );
    		TSUtil_NewEnsemble tsu = new TSUtil_NewEnsemble( NewEnsembleID, NewEnsembleName, tslist,
    		     InputStart_DateTime, InputEnd_DateTime, CopyTimeSeries_boolean );
    		ensemble = tsu.newEnsemble();
    		List<String> problems = tsu.getProblems();
            for ( int iprob = 0; iprob < problems.size(); iprob++ ) {
                message = problems.get(iprob);
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                // No recommendation since it is a user-defined check
                // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING, message, "" ) );
            }
            // Add the ensemble to the processor if created
            if ( ensemble != null ) {
                TSCommandProcessorUtil.appendEnsembleToResultsEnsembleList(processor, this, ensemble);
            }
        }
	}
	catch ( Exception e ) {
		message = "Unexpected error creating new ensemble \"" + NewEnsembleID + "\" (" + e + ").";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		Message.printWarning(3,routine,e);
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "See the log file for details - report the problem to software support." ) );
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
Set the ensemble that is processed by this class in discovery mode.
*/
private void setDiscoveryEnsemble ( TSEnsemble tsensemble )
{
    __tsensemble = tsensemble;
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
	String NewEnsembleID = props.getValue( "NewEnsembleID" );
    String NewEnsembleName = props.getValue( "NewEnsembleName" );
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");
	String CopyTimeSeries = props.getValue("CopyTimeSeries");
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
	if ( (NewEnsembleID != null) && (NewEnsembleID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewEnsembleID=\"" + NewEnsembleID + "\"");
	}
    if ( (NewEnsembleName != null) && (NewEnsembleName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewEnsembleName=\"" + NewEnsembleName + "\"");
    }
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputStart=\"" + InputStart + "\"" );
	}
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputEnd=\"" + InputEnd + "\"" );
	}
    if ( (CopyTimeSeries != null) && (CopyTimeSeries.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CopyTimeSeries=\"" + CopyTimeSeries + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}