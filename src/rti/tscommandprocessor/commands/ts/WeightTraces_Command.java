package rti.tscommandprocessor.commands.ts;

import javax.swing.JFrame;

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
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the WeightTraces() command.
*/
public class WeightTraces_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
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
private Vector __discovery_TS_Vector = null;

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
    String Year = parameters.getValue ( "Year" );
    String Weight = parameters.getValue ( "Weight" );
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
	Vector Year_Vector = StringUtil.breakStringList ( Year, "\n, ", StringUtil.DELIM_SKIP_BLANKS );
	int Year_size = 0;
	if ( Year_Vector != null ) {
	    Year_size = Year_Vector.size();
	}
	Vector Weight_Vector = StringUtil.breakStringList ( Weight, "\n, ", StringUtil.DELIM_SKIP_BLANKS );
    int Weight_size = 0;
    if ( Weight_Vector != null ) {
        Weight_size = Weight_Vector.size();
    }
    if ( Year_size != Weight_size ) {
        message = "The number of trace years and weights is not equal.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify the same number of trace years and weights."));
    }
    for ( int i = 0; i < Year_size; i++ ) {
        if ( !StringUtil.isInteger((String)Year_Vector.elementAt(i)) ) {
            message = "Trace year \"" + Year_Vector.elementAt(i) + "\" is not an integer.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message, "Specify an integer year."));
        }
    }
    for ( int i = 0; i < Weight_size; i++ ) {
        if ( !StringUtil.isDouble((String)Weight_Vector.elementAt(i)) ) {
            message = "Weight \"" + Weight_Vector.elementAt(i) + "\" is not a number.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message, "Specify a number for the weight."));
        }
    }
    if ( (NewTSID != null) && !NewTSID.equals("") ) {
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
            CommandStatusType.FAILURE, message,
            "Use the command editor to enter required fields."));
        }
    }
    
    // Check for invalid parameters...
    Vector valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "SpecifyWeightsHow" );
    valid_Vector.add ( "Year" );
    valid_Vector.add ( "Weight" );
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
	return (new CopyEnsemble_JDialog ( parent, this )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private Vector getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
    Vector discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    TS datats = (TS)discovery_TS_Vector.elementAt(0);
    // Use the most generic for the base class...
    TS ts = new TS();
    // Check for TS request or class that matches the data...
    if ( (c == ts.getClass()) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Parse the command string into a PropList of parameters.  This method currently
supports very-old syntax (separate commands for different combinations of
parameters), newer syntax (one command but fixed-parameter list), and current
syntax (free-format parameters).
@param command_string A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command_string )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{   int warning_level = 2;
    String routine = "fillMOVE2_Command.parseCommand", message;

    if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
        // TODO SAM 2006-04-16 This whole block of code needs to be
        // removed as soon as commands have been migrated to the new syntax.
        //
        // Old syntax (not free-format parameters)...
        Vector v = StringUtil.breakStringList(command_string,
            "(),\t", StringUtil.DELIM_ALLOW_STRINGS );
        int ntokens = 0;
        if ( v != null ) {
            ntokens = v.size();
        }
        // v[0] is the command name
        if ( ntokens < 11 ) {
            message = "Syntax error in \"" + command_string + "\".  Not enough parameters.";
            Message.printWarning ( warning_level, routine, message);
            throw new InvalidCommandSyntaxException ( message );
        }

        // Get the individual tokens of the expression...

        String TSID = "";
        String IndependentTSID = "";
        String NumberOfEquations = "";
        //String AnalysisMonth = "";
        String Transformation = "";
        //String Intercept = "";
        String DependentAnalysisStart = "";
        String DependentAnalysisEnd = "";
        String IndependentAnalysisStart = "";
        String IndependentAnalysisEnd = "";
        String FillStart = "";
        String FillEnd = "";
        int ic = 1;   // Skip command name
        TSID = ((String)v.elementAt(ic++)).trim();
        IndependentTSID = ((String)v.elementAt(ic++)).trim();
        NumberOfEquations=((String)v.elementAt(ic++)).trim();
        Transformation = ((String)v.elementAt(ic++)).trim();
        DependentAnalysisStart = ((String)v.elementAt(ic++)).trim();
        if ( DependentAnalysisStart.equals("*") ) {
            DependentAnalysisStart = "";// Current default
        }
        DependentAnalysisEnd =((String)v.elementAt(ic++)).trim();
        if ( DependentAnalysisEnd.equals("*") ) {
            DependentAnalysisEnd = "";// Current default
        }
        IndependentAnalysisStart = ((String)v.elementAt(ic++)).trim();
        if ( IndependentAnalysisStart.equals("*") ) {
            IndependentAnalysisStart = "";// Current default
        }
        IndependentAnalysisEnd =((String)v.elementAt(ic++)).trim();
        if ( IndependentAnalysisEnd.equals("*") ) {
            IndependentAnalysisEnd = "";// Current default
        }
        FillStart = ((String)v.elementAt(ic++)).trim();
        if ( FillStart.equals("*") ) {
            FillStart = ""; // Current default.
        }
        FillEnd = ((String)v.elementAt(ic++)).trim();
        if ( FillEnd.equals("*") ) {
            FillEnd = "";   // Current default.
        }

        v = null;
        PropList parameters = new PropList ( getCommandName() );
        parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
        if ( TSID.length() > 0 ) {
            parameters.set ( "TSID", TSID );
        }
        if ( IndependentTSID.length() > 0 ) {
            parameters.set ( "IndependentTSID", IndependentTSID );
        }
        if ( NumberOfEquations.length() > 0 ) {
            parameters.set("NumberOfEquations", NumberOfEquations);
        }
        /* TODO SAM 2006-04-16
            Evaluate whether this can be enabled
        if ( AnalysisMonth.length() > 0 ) {
            _parameters.set ( "AnalysisMonth", AnalysisMonth );
        }
        */
        if ( Transformation.length() > 0 ) {
            parameters.set ( "Transformation", Transformation );
        }
        /* TODO SAM 2006-04-16
            Evaluate whether this can be enabled
        if ( Intercept.length() > 0 ) {
            _parameters.set ( "Intercept", Intercept );
        }
        */
        if ( DependentAnalysisStart.length() > 0 ) {
            parameters.set ( "DependentAnalysisStart",DependentAnalysisStart );
        }
        if ( DependentAnalysisEnd.length() > 0 ) {
            parameters.set ( "DependentAnalysisEnd",DependentAnalysisEnd );
        }
        if ( IndependentAnalysisStart.length() > 0 ) {
            parameters.set ( "IndependentAnalysisStart",IndependentAnalysisStart );
        }
        if ( IndependentAnalysisEnd.length() > 0 ) {
            parameters.set ( "IndependentAnalysisEnd",IndependentAnalysisEnd );
        }
        if ( FillStart.length() > 0 ) {
            parameters.set ( "FillStart", FillStart );
        }
        if ( FillEnd.length() > 0 ) {
            parameters.set ( "FillEnd", FillEnd );
        }
        parameters.setHowSet ( Prop.SET_UNKNOWN );
        setCommandParameters ( parameters );
    }
}

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
        setDiscoveryTSList ( null );
    }
	
    String Alias = parameters.getValue ( "Alias" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	//String SpecifyWeightsHow = parameters.getValue ( "SpecifyWeightsHow" );
	String Year = parameters.getValue ( "Year" );
	String Weight = parameters.getValue ( "Weight" );
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
        
        try {
            // Get the years and weights...
            Vector Year_Vector = StringUtil.breakStringList( Year, ", ", 0);
            Vector Weight_Vector = StringUtil.breakStringList( Weight, ", ", 0);
            
            // Create a new time series to hold the results.
            
            TS newts = null;
            
            // Loop through each requested year and get the time series in the ensemble to process.
            
            int size_ensemble = tsensemble.size();
            for ( int iyear = 0; iyear < Year_Vector.size(); iyear++ ) {
                int year = Integer.parseInt((String)Year_Vector.get(iyear));
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
                        status.addToLog ( command_phase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that ensemble includes trace year " + year ) );
                        continue;
                }
                // If the first time series being added, simply clone the original time series, set the
                // new time series identifier, and clear out the data.
                if ( newts == null ) {
                    newts = (TS)ts.clone();
                    newts.setIdentifier ( NewTSID );
                    newts.allocateDataSpace();
                }
                // Add the time series to the new time series.
                double weight = Double.parseDouble((String)Weight_Vector.get(iyear));
                Vector v = new Vector();
                v.add ( ts );
                double [] factor = new double[1];
                factor[0] = weight;
                TSUtil.add ( newts, v, factor, TSUtil.IGNORE_MISSING );
            }
            
            // Update the data to the processor so that appropriate actions are taken...

            int wc2 = TSCommandProcessorUtil.appendTimeSeriesToResultsList ( processor, this, newts );
            if ( wc2 > 0 ) {
                message = "Error appending new time series to results.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,
                    ++warning_count), routine, message );
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                throw new CommandException ( message );
            }
    	}
    	catch ( Exception e ) {
    		message = "Unexpected error trying to weight ensemble traces \""+ tsensemble.getEnsembleID() + "\".";
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
        // Just want the identifier...
        TS ts = new TS ();
        ts.setAlias ( Alias );
        try {
            ts.setIdentifier( NewTSID );
        }
        catch ( Exception e ) {
            // Should not happen since identifier was previously checked.
        }
        Vector tslist = new Vector();
        tslist.addElement ( ts );
        setDiscoveryTSList ( tslist );
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
private void setDiscoveryTSList ( Vector discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String Alias = props.getValue( "Alias" );
	String EnsembleID = props.getValue( "EnsembleID" );
	String SpecifyWeightsHow = props.getValue( "SpecifyWeightsHow" );
	String Year = props.getValue( "Year" );
	String Weight = props.getValue( "Weight" );
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
    if ( (Year != null) && (Year.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Year=\"" + Year + "\"" );
    }
    if ( (Weight != null) && (Weight.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Weight=\"" + Weight + "\"" );
    }
    if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewTSID=\"" + NewTSID + "\"" );
    }

	return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
}

}
