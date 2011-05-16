package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;

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
import RTi.Util.IO.CommandSavesMultipleVersions;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the WeightTraces() command.
*/
public class WeightTraces_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{

/**
 * Value for SpecifyWeightsHow.
 */
protected String _AbsoluteWeights = "AbsoluteWeights";
  //private String __NORMALIZED_WEIGHTS = "NormalizedWeights";
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Years as array of integers (populated during data check).
*/
private int [] __Year_int = null;

/**
Weights as array of doubles (populated during data check).
*/
private double [] __Weight_double = null;

/**
Constructor.
*/
public WeightTraces_Command ()
{	super();
	setCommandName ( "WeightTraces" );
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
{	String Alias = parameters.getValue ( "Alias" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String SpecifyWeightsHow = parameters.getValue ( "SpecifyWeightsHow" );
    String Weights = parameters.getValue ( "Weights" );
    String NewTSID = parameters.getValue ( "NewTSID" );
    String message;
    String warning = "";
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    if ( (Alias == null) || Alias.equals("") ) {
        message = "An alias must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Provide an alias for the weighted time series."));
    }
    
    if ( (SpecifyWeightsHow != null) && !SpecifyWeightsHow.equals("") &&
            !SpecifyWeightsHow.equalsIgnoreCase(_AbsoluteWeights)) {
        message = "The value of SpecifyWeightsHow must be blank or " + _AbsoluteWeights + ".";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify a valid value."));
    }

	if ( (EnsembleID == null) || EnsembleID.equals("") ) {
        message = "The ensemble identifier to process must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide an identifier for the ensemble to copy."));
	}
	List Weights_Vector = StringUtil.breakStringList ( Weights, ", ", StringUtil.DELIM_SKIP_BLANKS );
    int Weights_size = 0;
    if ( Weights_Vector != null ) {
        Weights_size = Weights_Vector.size();
    }
    if ( Weights_size == 0 ) {
        message = "Pairs of year/weights values are not specifed (no values).";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify pairs of trace years and weights seperated by commas."));   
    }
    else if ( (Weights_size %2) != 0 ) {
        message = "Pairs of year/weights values are not specifed (odd number of values).";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify pairs of trace years and weights seperated by commas."));
    }
    else {
        __Year_int = new int[Weights_size/2];
        __Weight_double = new double[Weights_size/2];
        for ( int i = 0; i < Weights_size; i++ ) {
            String value = (String)Weights_Vector.get(i);
            if ( (i%2) == 0 ) {
                // Year...
                if ( !StringUtil.isInteger(value) ) {
                    message = "Trace year \"" + Weights_Vector.get(i) + "\" is not an integer.";
                    warning += "\n" + message;
                    status.addToLog(CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(
                            CommandStatusType.FAILURE, message, "Specify an integer year."));
                }
                else {
                    __Year_int[i/2] = Integer.parseInt(value);
                }
            }
            else {
                // Weights
                if ( !StringUtil.isDouble(value) ) {
                    message = "Weight \"" + Weights_Vector.get(i) + "\" is not a number.";
                    warning += "\n" + message;
                    status.addToLog(CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(
                            CommandStatusType.FAILURE, message, "Specify a number for the weight."));
                }
                else {
                    __Weight_double[i/2] = Double.parseDouble(value);
                }
            }
        }
    }
    if ( (NewTSID == null) || NewTSID.equals("") ) {
        message = "The new time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a new time series identifier when defining the command."));
    }
    else {
        try {
            TSIdent.parseIdentifier( NewTSID, TSIdent.NO_VALIDATION );
        }
        catch ( Exception e ) {
            // TODO SAM 2007-03-12 Need to catch a specific exception like
            // InvalidIntervalException so that more intelligent messages can be generated.
            message = "NewTSID \"" + NewTSID + "\" is not a valid identifier." +
            "Use the command editor to enter required fields.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message, "Use the command editor to enter required fields."));
        }
    }
    
    // Check for invalid parameters...
    List<String> valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "SpecifyWeightsHow" );
    valid_Vector.add ( "Weights" );
    valid_Vector.add ( "NewTSID" );
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
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new WeightTraces_JDialog ( parent, this )).ok();
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
	List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class...
    // Check for TS request or class that matches the data...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discoveryTSList;
    }
    else {
        return null;
    }
}

/**
Parse the command string into a PropList of parameters.  Only new syntax is
supported because the command has not been used for a long time and now operates
on an EnsembleID (very old used convoluted TSID with sequence number).
@param commandString A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String commandString )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{   int warning_level = 2;
    String routine = "WeightTraces_Command.parseCommand", message;

    if ( !commandString.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation
        super.parseCommand(commandString);
    }
    else {
        // Get the part of the command after the TS Alias =...
        int pos = commandString.indexOf ( "=" );
        if ( pos < 0 ) {
            message = "Syntax error in \"" + commandString + "\".  Expecting:  TS Alias = WeightTraces(...)";
            Message.printWarning ( warning_level, routine, message);
            throw new InvalidCommandSyntaxException ( message );
        }
        String token0 = commandString.substring ( 0, pos ).trim();    // TS Alias
        String token1 = commandString.substring ( pos + 1 ).trim();   // command(...)
        if ( (token0 == null) || (token1 == null) ) {
            message = "Syntax error in \"" + commandString + "\".  Expecting:  TS Alias = WeightTraces(...)";
            Message.printWarning ( warning_level, routine, message);
            throw new InvalidCommandSyntaxException ( message );
        }
        
        // Get the alias from the first token before the equal sign...
        
        String Alias = StringUtil.getToken ( token0, " ", StringUtil.DELIM_SKIP_BLANKS, 1 );
        super.parseCommand( token1 );
        
        PropList parameters = getCommandParameters();
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        parameters.set ( "Alias", Alias );
        parameters.setHowSet ( Prop.SET_UNKNOWN );
        setCommandParameters ( parameters );
    }
}

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
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = "WeightTraces_Command.runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3;	// Level for non-user messages

	// Make sure there are time series available to operate on...

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
    }
    
    String Alias = parameters.getValue ( "Alias" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    // Currently only one option...
	//String SpecifyWeightsHow = parameters.getValue ( "SpecifyWeightsHow" );
	String NewTSID = parameters.getValue ( "NewTSID" );

    TSEnsemble tsensemble = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        // FIXME - SAM 2011-02-02 This gets all the ensembles, not just the ones matching the request!
        List<TSEnsemble> tsEnsembleList =
            TSCommandProcessorUtil.getDiscoveryEnsembleFromCommandsBeforeCommand((TSCommandProcessor)processor,this);
        for ( TSEnsemble tsEnsemble: tsEnsembleList ) {
            if ( tsEnsemble.getEnsembleID().equalsIgnoreCase(EnsembleID) ) {
                tsensemble = tsEnsemble;
                break;
            }
        }
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
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
        if ( o_TSEnsemble == null ) {
            message = "Null TS requesting GetEnsemble(EnsembleID=\"" + EnsembleID + "\") from processor.";
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
    
    if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
        try {
            // Don't validate because the interval will be blank.
            new TSIdent ( NewTSID, TSIdent.NO_VALIDATION );
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
    TS newts = null;
    try {
        // Years and weights were previously determined.
        // Loop through each requested year and get the time series in the ensemble to process.
        
        int size_ensemble = tsensemble.size();
        for ( int iyear = 0; iyear < __Year_int.length; iyear++ ) {
            int year = __Year_int[iyear];   // Year to be weighted
            TS ts = null;   // Time series in ensemble
            boolean found = false;  // Is year trace found?
            for ( int i = 0; i < size_ensemble; i++ ) {
                ts = tsensemble.get(i);
                if ( ts.getIdentifier().getSequenceNumber() == year ) {
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                message = "Unable to find trace year [" + year + "] to add to weighted results.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that ensemble includes trace year " + year ) );
                    continue;
            }
            // If the first time series being added, simply clone the original time series, set the
            // new time series identifier, and clear out the data.
            if ( newts == null ) {
                newts = (TS)ts.clone();
                if ( (Alias != null) && !Alias.equals("") ) {
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, newts, Alias, status, commandPhase);
                    newts.setAlias ( alias );
                }
                newts.setIdentifier ( NewTSID );
                // Set the description to empty since it will be reset in the TSUtil.add call below.
                newts.setDescription("");
                // Set the data values to missing to clear out
                if ( commandPhase == CommandPhaseType.RUN ) {
                    TSUtil.setConstant(newts,newts.getMissing());
                }
            }
            // Add the time series to the new time series.  This will add to the description for each
            // added/scaled value.
            if ( commandPhase == CommandPhaseType.RUN ) {
                List v = new Vector();
                v.add ( ts );
                double [] factor = new double[1];
                factor[0] = __Weight_double[iyear];
                // The missing flag will work here for the first and subsequent time series.
                TSUtil.add ( newts, v, factor, TSUtil.SET_MISSING_IF_OTHER_MISSING );
            }
        }
        
        // Update the data to the processor so that appropriate actions are taken...
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Just want time series headers initialized
            List<TS> discoveryTSList = new Vector();
            discoveryTSList.add ( newts );
            setDiscoveryTSList ( discoveryTSList );
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
            int wc2 = TSCommandProcessorUtil.appendTimeSeriesToResultsList ( processor, this, newts );
            if ( wc2 > 0 ) {
                message = "Error appending new time series to results.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
        }
	}
	catch ( Exception e ) {
		message = "Unexpected error trying to weight ensemble traces \""+ tsensemble.getEnsembleID() + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Check the log file - report the problem to software support." ) );
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
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List discovery_TS_Vector )
{
    __discoveryTSList = discovery_TS_Vector;
}

/**
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props )
{
    return toString ( props, 10 );
}

/**
Return the string representation of the command.
@param props parameters for the command
@param majorVersion the major version for software - if less than 10, the "TS Alias = " notation is used,
allowing command files to be saved for older software.
*/
public String toString ( PropList props, int majorVersion )
{   if ( props == null ) {
        if ( majorVersion < 10 ) {
            return "TS Alias = " + getCommandName() + "()";
        }
        else {
            return getCommandName() + "()";
        }
    }
    String Alias = props.getValue( "Alias" );
	String EnsembleID = props.getValue( "EnsembleID" );
	String SpecifyWeightsHow = props.getValue( "SpecifyWeightsHow" );
	String Weights = props.getValue( "Weights" );
	String NewTSID = props.getValue( "NewTSID" );
	StringBuffer b = new StringBuffer ();
	if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
	}
    if ( (SpecifyWeightsHow != null) && (SpecifyWeightsHow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SpecifyWeightsHow=\"" + SpecifyWeightsHow + "\"" );
    }
    if ( (Weights != null) && (Weights.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Weights=\"" + Weights + "\"" );
    }
    if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewTSID=\"" + NewTSID + "\"" );
    }
    if ( majorVersion >= 10 ) {
        // Add as a parameter
        if ( (Alias != null) && (Alias.length() > 0) ) {
            if ( b.length() > 0 ) {
                b.append ( "," );
            }
            b.append ( "Alias=\"" + Alias + "\"" );
        }
    }
    if ( majorVersion < 10 ) {
        // Old syntax...
        if ( (Alias == null) || Alias.equals("") ) {
            Alias = "Alias";
        }
        return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
    }
    else {
        return getCommandName() + "("+ b.toString()+")";
    }
}

}