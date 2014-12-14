package rti.tscommandprocessor.commands.util;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;

/**
This class initializes, checks, and runs the If() command.
*/
public class If_Command extends AbstractCommand implements Command
{

/**
Result of evaluating the condition.
*/
private boolean conditionEval = true;

/**
Constructor.
*/
public If_Command ()
{	super();
	setCommandName ( "If" );
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
{	String routine = getCommandName() + "_checkCommandParameters";
	String Name = parameters.getValue ( "Name" );
	String Condition = parameters.getValue ( "Condition" );
	String TSExists = parameters.getValue ( "TSExists" );
	String warning = "";
	String message;
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (Name == null) || Name.equals("") ) {
        message = "A name for the if block must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the name." ) );
    }
    if ( ((Condition == null) || Condition.equals("")) && ((TSExists == null) || TSExists.equals("")) ) {
        message = "A condition or TSExists must be specified";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE, message, "Specify the condition." ) );
    }

	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(3);
	validList.add ( "Name" );
	validList.add ( "Condition" );
	validList.add ( "TSExists" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );
	
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine,
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
	return (new If_JDialog ( parent, this )).ok();
}

/**
Return the result of evaluating the condition, which is set when runCommand() is called.
@return the result of evaluting the condition
*/
public boolean getConditionEval ()
{
    return this.conditionEval;
}

/**
Return the name of the if command.
@return the name of the if command, should not be null.
*/
public String getName ()
{
    return getCommandParameters().getValue("Name");
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws CommandWarningException, CommandException
{	String routine = "If_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
    CommandProcessor processor = getCommandProcessor();
	PropList parameters = getCommandParameters();
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);
	
	//String Name = parameters.getValue ( "Name" );
	String Condition = parameters.getValue ( "Condition" );
	String TSExists = parameters.getValue ( "TSExists" );

	try {
	    boolean conditionEval = false;
	    setConditionEval ( conditionEval );
	    if ( (Condition != null) && !Condition.equals("") ) {
    	    // TODO SAM 2013-12-07 Figure out if there is a more elegant way to do this
    	    // Currently only Value1 Operator Value2 is allowed.  Brute force split by finding the operator
    	    int pos, pos1 = -1, pos2 = -1;
    	    String value1 = "", value2 = "";
    	    int ivalue1, ivalue2;
    	    String op = "??";
    	    if ( Condition.indexOf("<=") > 0 ) {
    	        pos = Condition.indexOf("<=");
    	        op = "<=";
    	        pos1 = pos;
    	        pos2 = pos + 2;
    	    }
    	    else if ( Condition.indexOf("<") > 0 ) {
    	        pos = Condition.indexOf("<");
    	        op = "<";
                pos1 = pos;
                pos2 = pos + 1;
    	    }
            else if ( Condition.indexOf(">=") > 0 ) {
                pos = Condition.indexOf(">=");
                op = ">=";
                pos1 = pos;
                pos2 = pos + 2;
            }
            else if ( Condition.indexOf(">") > 0 ) {
                pos = Condition.indexOf(">");
                op = ">";
                pos1 = pos;
                pos2 = pos + 1;
            }
            else if ( Condition.indexOf("==") > 0 ) {
                pos = Condition.indexOf("==");
                op = "==";
                pos1 = pos;
                pos2 = pos + 2;
            }
            else if ( Condition.indexOf("!=") > 0 ) {
                pos = Condition.indexOf("!=");
                op = "!=";
                pos1 = pos;
                pos2 = pos + 2;
            }
            else if ( Condition.indexOf("=") > 0 ) {
                message = "Bad use of = in condition.";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Use == to check for equality." ) );
            }
            else {
                message = "Unknown condition operator for \"" + Condition + "\"";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Make sure condition operator is supported - refer to command editor and documentation." ) );
            }
            value1 = TSCommandProcessorUtil.expandParameterValue(
                this.getCommandProcessor(),this,Condition.substring(0,pos1).trim() );
            value2 = TSCommandProcessorUtil.expandParameterValue(
                this.getCommandProcessor(),this,Condition.substring(pos2).trim() );
    	    ivalue1 = Integer.parseInt(value1);
    	    ivalue2 = Integer.parseInt(value2);
    	    if ( op.equals("<=") ) {
    	        if ( ivalue1 <= ivalue2 ) {
    	            conditionEval = true;
    	        }
    	    }
    	    else if ( op.equals("<") ) {
                if ( ivalue1 < ivalue2 ) {
                    conditionEval = true;
                } 
    	    }
    	    else if ( op.equals(">=") ) {
                if ( ivalue1 >= ivalue2 ) {
                    conditionEval = true;
                }
    	    }
    	    else if ( op.equals(">") ) {
                if ( ivalue1 > ivalue2 ) {
                    conditionEval = true;
                }
    	    }
    	    else if ( op.equals("==") ) {
                if ( ivalue1 == ivalue2 ) {
                    conditionEval = true;
                }
    	    }
    	    else if ( op.equals("!=") ) {
                if ( ivalue1 != ivalue2 ) {
                    conditionEval = true;
                }
    	    }
    	    if ( Condition.indexOf("${") >= 0 ) {
    	        // Show the original
    	        status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.SUCCESS,
                        Condition + " (showing properties) evaluates to " + conditionEval, "See also matching EndIf()" ) );
    	    }
    	    // Always show the expanded
    	    status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.SUCCESS,
                    value1 + " " + op + " " + value2 + " evaluates to " + conditionEval, "See also matching EndIf()" ) );
    	    setConditionEval(conditionEval);
	    }
	    if ( (TSExists != null) && !TSExists.equals("") ) {
	        // Want to check whether a time series exists - this is ANDed to the condition
	        // Get the time series to process.  The time series list is searched backwards until the first match...
	        TS ts = null;
            PropList request_params = new PropList ( "" );
            request_params.set ( "CommandTag", command_tag );
            request_params.set ( "TSID", TSExists );
            CommandProcessorRequestResultsBean bean = null;
            try {
                bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSExists + "\") from processor.";
                Message.printWarning(3,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_TS = bean_PropList.getContents ( "TS");
            if ( o_TS != null ) {
                ts = (TS)o_TS;
            }
            if ( ts == null ) {
                // Does not matter what the Condition had - the final result is false
                conditionEval = false;
            }
            else {
                if ( (Condition != null) && !Condition.equals("") ) {
                     conditionEval = conditionEval & true;
                }
                else {
                    conditionEval = true;
                }
            }
            setConditionEval(conditionEval);
	    }
	}
	catch ( Exception e ) {
		message = "Unexpected error executing If (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Check the log file or command window for details." ) );
		throw new CommandException ( message );
	}
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the result of evaluating the condition.
@param conditionEval result of evaluating the condition
*/
private void setConditionEval ( boolean conditionEval )
{
    this.conditionEval = conditionEval;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{   if ( props == null ) {
        return getCommandName() + "()";
    }
    String Name = props.getValue( "Name" );
    String Condition = props.getValue( "Condition" );
    String TSExists = props.getValue( "TSExists" );
    StringBuffer b = new StringBuffer ();
    if ( (Name != null) && (Name.length() > 0) ) {
        b.append ( "Name=\"" + Name + "\"" );
    }
    if ( (Condition != null) && (Condition.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Condition=\"" + Condition + "\"" );
    }
    if ( (TSExists != null) && (TSExists.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSExists=\"" + TSExists + "\"" );
    }
    return getCommandName() + "(" + b.toString() + ")";
}

}