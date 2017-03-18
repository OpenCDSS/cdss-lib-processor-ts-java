package rti.tscommandprocessor.commands.ensemble;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSIdent;

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

/**
This class initializes, checks, and runs the CopyEnsemble() command.
*/
public class CopyEnsemble_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
TSEnsemble created in discovery mode (basically to get the identifier for other commands).
*/
private TSEnsemble __tsensemble = null;

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
//private Vector __discovery_TS_Vector = null;
// TODO SAM 2008-07-18 Figure out whether/how to enable

/**
Constructor.
*/
public CopyEnsemble_Command ()
{	super();
	setCommandName ( "CopyEnsemble" );
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
{	String NewEnsembleID = parameters.getValue ( "NewEnsembleID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String NewTSID = parameters.getValue ( "NewTSID" );

	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (NewEnsembleID == null) || NewEnsembleID.equals("") ) {
        message = "The new ensemble identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a new ensemble identifier for the copy."));
	}
	if ( (EnsembleID == null) || EnsembleID.equals("") ) {
        message = "The ensemble identifier to copy must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide an identifier for the ensemble to copy."));
	}
    if ( (NewTSID != null) && !NewTSID.equals("") ) {
        try {
            TSIdent.parseIdentifier( NewTSID, TSIdent.NO_VALIDATION );
        }
        catch ( Exception e ) {
            // TODO SAM 2007-03-12 Need to catch a specific exception like
            // InvalidIntervalException so that more intelligent messages can be
            // generated.
            message = "NewTSID \"" + NewTSID + "\" is not a valid identifier." +
            "Use the command editor to enter required fields.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Use the command editor to enter required fields."));
        }
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(5);
    validList.add ( "NewEnsembleID" );
    validList.add ( "NewEnsembleName" );
    validList.add ( "EnsembleID" );
    validList.add ( "NewAlias" );
    validList.add ( "NewTSID" );
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
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new CopyEnsemble_JDialog ( parent, this )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private TSEnsemble getDiscoveryEnsemble()
{
    return __tsensemble;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   TSEnsemble tsensemble = getDiscoveryEnsemble();
	List<TSEnsemble> v = null;
    if ( (tsensemble != null) && (c == tsensemble.getClass()) ) {
        v = new Vector<TSEnsemble>();
        v.add ( tsensemble );
        Message.printStatus ( 2, "", "Added ensemble to object list: " + tsensemble.getEnsembleID());
    }
    return v;
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "CopyEnsemble_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Level for non-user messages

	// Make sure there are time series available to operate on...

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    
    if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryEnsemble ( null );
    }
	
	String NewEnsembleID = parameters.getValue ( "NewEnsembleID" );
    String NewEnsembleName = parameters.getValue ( "NewEnsembleName" );
	String EnsembleID = parameters.getValue ( "EnsembleID" );
	String NewAlias = parameters.getValue ( "NewAlias" );
	String NewTSID = parameters.getValue ( "NewTSID" );

    if ( command_phase == CommandPhaseType.RUN ) {
    	// Get the time series ensemble to process.

        PropList request_params = new PropList ( "" );
        request_params.set ( "CommandTag", command_tag );
        request_params.set ( "EnsembleID", EnsembleID );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "GetEnsemble", request_params );
        }
        catch ( Exception e ) {
            message = "Error requesting GetEnsemble(EnsembleID=\"" + EnsembleID + "\") from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_TSEnsemble = bean_PropList.getContents ( "TSEnsemble");
        TSEnsemble tsensemble = null;
        if ( o_TSEnsemble == null ) {
            message = "Null ensemble requesting GetEnsemble(EnsembleID=\"" + EnsembleID + "\") from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
        }
        else {
            tsensemble = (TSEnsemble)o_TSEnsemble;
        }
        
        if ( tsensemble == null ) {
            message = "Unable to find ensemble to process using EnsembleID \"" + EnsembleID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
            throw new CommandWarningException ( message );
        }
        
        TSIdent NewTSID_TSIdent = null;
        if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
            try {
                // Don't validate because the interval will be blank.
                NewTSID_TSIdent = new TSIdent ( NewTSID, TSIdent.NO_VALIDATION );
            }
            catch ( Exception e ) {
                message = "NewTSID \"" + NewTSID + "\" cannot be parsed - invalid time series identifier.";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(
                command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the new time series identifier information." ) );
                throw new CommandWarningException ( message );
            }
        }

    	// Now process the time series...
    
    	TSEnsemble copy = null;
    	try {
            // Clone the ensemble (all time series will also be cloned)...
            copy = (TSEnsemble)tsensemble.clone();
            // Set the new information...
    		copy.setEnsembleID ( NewEnsembleID );
            if ( NewEnsembleName != null ) {
                copy.setEnsembleName ( NewEnsembleName );
            }
            // Also need to add each of the time series in the ensemble since these are new instances.
            int size = copy.size();
            TS ts = null;   // Individual time series in ensemble
            for ( int i = 0; i < size; i++ ) {
                ts = copy.get(i);
                if ( NewTSID_TSIdent != null ) {
                     // Reset individual identifier pieces.
                     if ( NewTSID_TSIdent.getLocation().length() > 0 ) {
                         ts.setLocation(NewTSID_TSIdent.getLocation());
                     }
                     if ( NewTSID_TSIdent.getSource().length() > 0 ) {
                         ts.setSource(NewTSID_TSIdent.getSource());
                     }
                     if ( NewTSID_TSIdent.getType().length() > 0 ) {
                         ts.setDataType(NewTSID_TSIdent.getType());
                     }
                     if ( NewTSID_TSIdent.getScenario().length() > 0 ) {
                         ts.getIdentifier().setScenario(NewTSID_TSIdent.getScenario());
                     }
                }
                if ( NewAlias != null ) {
                    // Reset the time series alias
                    ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                            processor, ts, NewAlias, status, command_phase) );
                }
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesToResultsList ( processor, this, ts );
                if ( wc2 > 0 ) {
                    message = "Error adding time series [" + i + "] from new ensemble.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
                        status.addToLog ( command_phase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
            }
            // Update the data to the processor so that appropriate actions are taken...
            
            TSCommandProcessorUtil.appendEnsembleToResultsEnsembleList(processor, this, copy );
    	}
    	catch ( Exception e ) {
    		message = "Unexpected error trying to copy ensemble \""+ tsensemble.getEnsembleID() + "\" (" + e + ").";
    		Message.printWarning ( warning_level,
    			MessageUtil.formatMessageTag(
    			command_tag,++warning_count),routine,message );
    		Message.printWarning(3,routine,e);
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Check the log file - report the problem to software support." ) );
    	}
    }
    else if ( command_phase == CommandPhaseType.DISCOVERY ) {
        // Save the list of new time series...
        // TODO SAM 2008-07-23 Figure out whether/how to enable
        //setDiscoveryTSList ( tslist );
        // Just want the identifier...
        TSEnsemble ensemble = new TSEnsemble ( NewEnsembleID, NewEnsembleName, null );
        setDiscoveryEnsemble ( ensemble );
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
	String NewEnsembleID = props.getValue( "NewEnsembleID" );
    String NewEnsembleName = props.getValue( "NewEnsembleName" );
	String EnsembleID = props.getValue( "EnsembleID" );
	String NewAlias = props.getValue( "NewAlias" );
	String NewTSID = props.getValue( "NewTSID" );
	StringBuffer b = new StringBuffer ();
	if ( (NewEnsembleID != null) && (NewEnsembleID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "NewEnsembleID=\"" + NewEnsembleID + "\"" );
	}
    if ( (NewEnsembleName != null) && (NewEnsembleName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewEnsembleName=\"" + NewEnsembleName + "\"" );
    }
    if ( (NewAlias != null) && (NewAlias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewAlias=\"" + NewAlias + "\"" );
    }
    if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewTSID=\"" + NewTSID + "\"" );
    }
	if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
	}
	return getCommandName() + "("+ b.toString()+")";
}

}
