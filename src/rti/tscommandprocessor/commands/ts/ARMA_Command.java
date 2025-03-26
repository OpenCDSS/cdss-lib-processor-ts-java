// ARMA_Command - This class initializes, checks, and runs the ARMA() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2015 Colorado Department of Natural Resources

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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.TSUtil;
import RTi.TS.TSUtil_ARMA;
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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the ARMA() command.
*/
public class ARMA_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
	
/**
Parameter values requiring sum to 1.
*/
protected final String _False = "False";
protected final String _True = "True";
    
/**
List of time series read during discovery.  These are TS objects but with mainly the metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public ARMA_Command ()
{	super();
	setCommandName ( "ARMA" );
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
	String ARMAInterval = parameters.getValue ( "ARMAInterval" );
	String a = parameters.getValue ( "a" );
	String b = parameters.getValue ( "b" );
	String RequireCoefficientsSumTo1 = parameters.getValue ( "RequireCoefficientsSumTo1" );
	String InputPreviousValues = parameters.getValue ( "InputPreviousValues" );
	String OutputPreviousValues = parameters.getValue ( "OutputPreviousValues" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String OutputMinimum = parameters.getValue ( "OutputMinimum" );
	String OutputMaximum = parameters.getValue ( "OutputMaximum" );
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

    double total = 0.0;
    double [] aArray = new double[0];
    // a is optional
    if ( (a != null) && !a.isEmpty() && (a.indexOf("${") < 0) ) {
        // Make sure coefficients are doubles...
    	List<String> aVector = StringUtil.breakStringList ( a, ", ", StringUtil.DELIM_SKIP_BLANKS );
        int aSize = 0;
        if ( aVector != null ) {
            aSize = aVector.size();
        }
        aArray = new double[aSize];
        String aVal;
        for ( int i = 0; i < aSize; i++ ) {
            aVal = ((String)aVector.get(i)).trim();
            if ( !StringUtil.isDouble(aVal)) {
                message = "The a-coefficient " + aVal + " is not a number.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Correct the value of the a-coefficient." ) );
            }
            else {
            	aArray[i] = StringUtil.atod(aVal);
                total += aArray[i];
            }
        }
    }

    double [] bArray = new double[0];
    if ( (b == null) || b.isEmpty() ) {
        message = "No b-coefficients are specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify b-coefficients." ) );
    }
    else if ( b.indexOf("${") < 0 ) {
        // Make sure coefficients are doubles...
    	List<String> bVector = StringUtil.breakStringList ( b, ", ", StringUtil.DELIM_SKIP_BLANKS );
        int bSize = 0;
        if ( bVector != null ) {
            bSize = bVector.size();
        }
        bArray = new double[bSize];
        String bVal;
        for ( int i = 0; i < bSize; i++ ) {
            bVal = ((String)bVector.get(i)).trim();
            if ( !StringUtil.isDouble(bVal)) {
                message = "The b-coefficient " + bVal + " is not a number.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Correct the value of the b-coefficient." ) );
            }
            else {
            	bArray[i] = StringUtil.atod(bVal);
                total += bArray[i];
            }
        }
    }

    boolean requireCoefficientsSumTo1 = true;
    if ( (RequireCoefficientsSumTo1 != null) && !RequireCoefficientsSumTo1.isEmpty() ) {
    	if ( !RequireCoefficientsSumTo1.equalsIgnoreCase(_False) && !RequireCoefficientsSumTo1.equalsIgnoreCase(_True) ) {
	        message = "The RequireCoefficientsSumTo1 parameter is invalid.";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
	            message, "Specify RequireCoefficientsSumTo1 as " + _False + " or " + _True + " (default).") );
    	}
    	if ( RequireCoefficientsSumTo1.equalsIgnoreCase(_False) ) {
    		requireCoefficientsSumTo1 = false;
    	}
    }

    if ( requireCoefficientsSumTo1 && (a.indexOf("${") < 0) && (b.indexOf("${") < 0)) {
	    String total_String = StringUtil.formatString(total,"%.6f");
	    if ( !total_String.equals("1.000000") ) {
	        message = "\nSum of a and b coefficients (" +
	            StringUtil.formatString(total,"%.6f") + ") does not equal 1.000000.";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Verify that a and b coefficents sum to 1.000000." ) );
	    }
    }

	if ( (ARMAInterval == null) || ARMAInterval.isEmpty() ) {
        message = "The ARMA interval is not specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the ARMA interval (e.g., 2Hour)." ) );
	}
	else if ( ARMAInterval.indexOf("${") < 0 ) {
		try {
		    TimeInterval.parseInterval(ARMAInterval);
		}
		catch ( Exception e ) {
            message = "The ARMA interval \"" + ARMAInterval + "\" is not a valid interval.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid time interval." ) );
		}
	}

	double [] inputPreviousValues = new double[0];
    if ( (InputPreviousValues != null) && !InputPreviousValues.equals("") && (InputPreviousValues.indexOf("${") < 0) ) {
        // Make sure values are doubles...
    	List<String> strings = StringUtil.breakStringList ( InputPreviousValues, ", ", StringUtil.DELIM_SKIP_BLANKS );
        int size = 0;
        if ( strings != null ) {
            size = strings.size();
        }
        inputPreviousValues = new double[size];
        String s;
        for ( int i = 0; i < size; i++ ) {
            s = strings.get(i).trim();
            double val;
            try {
            	val = Double.parseDouble(s);
                inputPreviousValues[i] = val;
            }
            catch ( NumberFormatException e ) {
                message = "The input initial value " + s + " is not a number.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that initial values are numbers." ) );
            }
        }
        // Additional checks
        if ( (b != null) && (b.indexOf("${") < 0) && (bArray.length > 0) && ((bArray.length - 1) != inputPreviousValues.length) ) {
            message = "The number of input previous values (" + inputPreviousValues.length +
            	") is not equal to the number of b-coeffients (" + bArray.length + ") minus 1 (" + (bArray.length - 1) + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Check the number of input previous values." ) );
        }
    }

    double [] outputPreviousValues = new double[0];
    if ( (OutputPreviousValues != null) && !OutputPreviousValues.equals("") && (OutputPreviousValues.indexOf("${") < 0) ) {
        // Make sure values are doubles...
    	List<String> strings = StringUtil.breakStringList ( OutputPreviousValues, ", ", StringUtil.DELIM_SKIP_BLANKS );
        int size = 0;
        if ( strings != null ) {
            size = strings.size();
        }
        outputPreviousValues = new double[size];
        String s;
        for ( int i = 0; i < size; i++ ) {
            s = strings.get(i).trim();
            double val;
            try {
            	val = Double.parseDouble(s);
                outputPreviousValues[i] = val;
            }
            catch ( NumberFormatException e ) {
                message = "The output initial value " + s + " is not a number.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that initial values are numbers." ) );
            }
        }
        // Additional checks
        if ( (a != null) && (a.indexOf("${") < 0) && (aArray.length > 0) && (aArray.length != outputPreviousValues.length) ) {
            message = "The number of output previous values (" + outputPreviousValues.length +
            	") is not equal to the number of a-coeffients (" + aArray.length + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Check the number of output previous values." ) );
        }
    }
    
	if ( (OutputStart != null) && !OutputStart.isEmpty() && !OutputStart.startsWith("${") ) {
		try {	DateTime datetime1 = DateTime.parse(OutputStart);
			if ( datetime1 == null ) {
				throw new Exception ("bad date");
			}
		}
		catch (Exception e) {
			message = "Output start date/time \"" + OutputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify a valid output start date/time." ) );
		}
	}
	if ( (OutputEnd != null) && !OutputEnd.isEmpty() && !OutputEnd.startsWith("${") ) {
		try {	DateTime datetime2 = DateTime.parse(OutputEnd);
			if ( datetime2 == null ) {
				throw new Exception ("bad date");
			}
		}
		catch (Exception e) {
			message = "Output end date/time \"" + OutputEnd + "\" is not a valid date/time.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify a valid output end date/time." ) );
		}
	}
	
	if ( (OutputMinimum != null) && !OutputMinimum.isEmpty() && (OutputMinimum.indexOf("${") < 0) && !StringUtil.isDouble(OutputMinimum) ) {
        message = "The output minimum value is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum value as a floating point number." ) );
	}
	
	if ( (OutputMaximum != null) && !OutputMaximum.isEmpty() && (OutputMaximum.indexOf("${") < 0) && !StringUtil.isDouble(OutputMaximum) ) {
        message = "The output maximum value is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the maximum value as a floating point number." ) );
	}
    
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(16);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "ARMAInterval" );
    validList.add ( "a" );
    validList.add ( "b" );
    validList.add ( "RequireCoefficientsSumTo1" );
    validList.add ( "InputPreviousValues" );
    validList.add ( "OutputPreviousValues" );
    validList.add ( "Alias" );
    validList.add ( "NewTSID" );
    validList.add ( "Description" );
    validList.add ( "OutputStart" );
    validList.add ( "OutputEnd" );
    validList.add ( "OutputMaximum" );
    validList.add ( "OutputMinimum" );
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
	return (new ARMA_JDialog ( parent, this )).ok();
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
The following classes can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
    List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discovery_TS_Vector;
    }
    else {
        return null;
    }
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
	String routine = "ARMA_Command.parseCommand", message;

	if ( (command_string.indexOf('=') > 0) || command_string.endsWith("()") ) {
        // Current syntax...
        super.parseCommand( command_string);
    }
    else {
		// TODO SAM 2009-09-15 This whole block of code needs to be
		// removed as soon as commands have been migrated to the new syntax.
    	List<String> v = StringUtil.breakStringList(command_string,
			"(),\t", StringUtil.DELIM_SKIP_BLANKS |	StringUtil.DELIM_ALLOW_STRINGS );
		int ntokens = 0;
		if ( v != null ) {
			ntokens = v.size();
		}
		if ( ntokens < 6 ) {
			// Command name, TSID, and constant...
			message = "Syntax error in \"" + command_string +
			"\".  Expecting ARMA(TSID,ARMAInterval,pP,a1,...,aP,qQ,b0,...bQ).";
			Message.printWarning ( warning_level, routine, message);
			throw new InvalidCommandSyntaxException ( message );
		}

		// Get the individual tokens of the expression...

		String TSID = ((String)v.get(1)).trim();
		String ARMAInterval = ((String)v.get(2)).trim();
		String p = ((String)v.get(3)).trim();
		StringBuffer a = new StringBuffer();
		int aSize = StringUtil.atoi(p.substring(1));
		for ( int i = 0; i < aSize; i++ ) {
		    if ( i > 0 ) {
		        a.append(",");
		    }
		    a.append((String)v.get(4+i));
		}
		String q = ((String)v.get(4 + aSize)).trim();
        StringBuffer b = new StringBuffer();
        int bSize = StringUtil.atoi(q.substring(1)) + 1;
        for ( int i = 0; i < bSize; i++ ) {
            if ( i > 0 ) {
                b.append(",");
            }
            b.append((String)v.get(4+aSize+1+i));
        }

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
		parameters.set ( "ARMAInterval", ARMAInterval );
		parameters.set ( "a", a.toString() );
		parameters.set ( "b", b.toString() );
		parameters.setHowSet ( Prop.SET_UNKNOWN );
		setCommandParameters ( parameters );
	}
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
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	//int log_level = 3; // Warning message level for non-user messages

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = Boolean.TRUE; // Default.
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
		status.clearLog(commandPhase);
	}

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
	String InputPreviousValues = parameters.getValue ( "InputPreviousValues" );
	double [] inputPreviousValues = new double[0];
	if ( (commandPhase == CommandPhaseType.RUN) && (InputPreviousValues != null) && (InputPreviousValues.indexOf("${") >= 0) ) {
		InputPreviousValues = TSCommandProcessorUtil.expandParameterValue(processor, this, InputPreviousValues);
	}
	if ( (commandPhase == CommandPhaseType.RUN) && (InputPreviousValues != null) && !InputPreviousValues.isEmpty() ) {
		// TODO SAM 2016-02-29 need utility code to help with this - duplicates code in checkCommandParameters due to properties
	    // Make sure values are doubles...
		List<String> strings = StringUtil.breakStringList ( InputPreviousValues, ", ", StringUtil.DELIM_SKIP_BLANKS );
	    int size = 0;
	    if ( strings != null ) {
	        size = strings.size();
	    }
	    String s;
	    inputPreviousValues = new double[size];
	    for ( int i = 0; i < size; i++ ) {
	        s = strings.get(i).trim();
	        double val;
	        try {
	        	val = Double.parseDouble(s);
	        	inputPreviousValues[i] = val;
	        }
	        catch ( NumberFormatException e ) {
	            message = "The input initial value " + s + " is not a number.";
	    		Message.printWarning(warning_level,
	    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
	    				routine, message );
	            status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Check the value." ) );
	        }
	    }
	}
	String OutputPreviousValues = parameters.getValue ( "OutputPreviousValues" );
	double [] outputPreviousValues = new double[0];
	if ( (commandPhase == CommandPhaseType.RUN) && (OutputPreviousValues != null) && (OutputPreviousValues.indexOf("${") >= 0) ) {
		OutputPreviousValues = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputPreviousValues);
	}
	if ( (commandPhase == CommandPhaseType.RUN) && (OutputPreviousValues != null) && !OutputPreviousValues.isEmpty() ) {
		// TODO SAM 2016-02-29 need utility code to help with this - duplicates code in checkCommandParameters due to properties
	    // Make sure values are doubles...
		List<String> strings = StringUtil.breakStringList ( OutputPreviousValues, ", ", StringUtil.DELIM_SKIP_BLANKS );
	    int size = 0;
	    if ( strings != null ) {
	        size = strings.size();
	    }
	    String s;
	    outputPreviousValues = new double[size];
	    for ( int i = 0; i < size; i++ ) {
	        s = strings.get(i).trim();
	        double val;
	        try {
	        	val = Double.parseDouble(s);
	        	outputPreviousValues[i] = val;
	        }
	        catch ( NumberFormatException e ) {
	            message = "The output initial value " + s + " is not a number.";
	    		Message.printWarning(warning_level,
	    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
	    				routine, message );
	            status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Check the value." ) );
	        }
	    }
	}
	double total = 0.0;
	String a = parameters.getValue("a");
	if ( (commandPhase == CommandPhaseType.RUN) && (a != null) && (a.indexOf("${") >= 0) ) {
		a = TSCommandProcessorUtil.expandParameterValue(processor, this, a);
	}
	double [] aArray = new double[0];
	if ( (commandPhase == CommandPhaseType.RUN) && (a != null) && !a.isEmpty() ) {
		// TODO SAM 2016-03-29 Need to consolidate this code with checkCommandParameters...
		List<String> aList = StringUtil.breakStringList ( a, ", ", StringUtil.DELIM_SKIP_BLANKS );
	    int aSize = 0;
	    if ( aList != null ) {
	        aSize = aList.size();
	    }
	    aArray = new double[aSize];
	    String aVal;
	    for ( int i = 0; i < aSize; i++ ) {
	        aVal = aList.get(i).trim();
	        if ( !StringUtil.isDouble(aVal)) {
	            message = "The a coefficient " + aVal + " is not a number.";
	    		Message.printWarning(warning_level,
	    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
	    				routine, message );
	            status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Check the value." ) );
	        }
	        else {
	        	aArray[i] = StringUtil.atod(aVal);
	        	total += aArray[i];
	        }
	    }
	}
	String b = parameters.getValue("b");
	if ( (commandPhase == CommandPhaseType.RUN) && (b != null) && (b.indexOf("${") >= 0) ) {
		b = TSCommandProcessorUtil.expandParameterValue(processor, this, b);
	}
	double [] bArray = new double[0];
	if ( (commandPhase == CommandPhaseType.RUN) && (b != null) && !b.isEmpty() ) {
		// TODO SAM 2016-03-29 Need to consolidate this code with checkCommandParameters...
		List<String> bList = StringUtil.breakStringList ( b, ", ", StringUtil.DELIM_SKIP_BLANKS );
	    int bSize = 0;
	    if ( bList != null ) {
	        bSize = bList.size();
	    }
	    bArray = new double[bSize];
	    String bVal;
	    for ( int i = 0; i < bSize; i++ ) {
	        bVal = bList.get(i).trim();
	        if ( !StringUtil.isDouble(bVal)) {
	            message = "The b coefficient " + bVal + " is not a number.";
	    		Message.printWarning(warning_level,
	    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
	    				routine, message );
	            status.addToLog ( CommandPhaseType.RUN,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Check the value." ) );
	        }
	        else {
	        	bArray[i] = StringUtil.atod(bVal);
	        	total += bArray[i];
	        }
	    }
	}
	String RequireCoefficientsSumTo1 = parameters.getValue ( "RequireCoefficientsSumTo1" );
	if ( (commandPhase == CommandPhaseType.RUN) && (RequireCoefficientsSumTo1 != null) && (RequireCoefficientsSumTo1.indexOf("${") >= 0) ) {
		RequireCoefficientsSumTo1 = TSCommandProcessorUtil.expandParameterValue(processor, this, RequireCoefficientsSumTo1);
	}
	if ( (commandPhase == CommandPhaseType.RUN) && (RequireCoefficientsSumTo1 != null) &&
		(RequireCoefficientsSumTo1.isEmpty() || RequireCoefficientsSumTo1.equalsIgnoreCase("true")) ) {
	    String total_String = StringUtil.formatString(total,"%.6f");
	    if ( !total_String.equals("1.000000") ) {
	        message = "\nSum of a and b coefficients (" + StringUtil.formatString(total,"%.6f") + ") does not equal 1.000000.";
    		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Check the value." ) );
	    }
	}
	String ARMAInterval = parameters.getValue("ARMAInterval");
	if ( (commandPhase == CommandPhaseType.RUN) && (ARMAInterval != null) && (ARMAInterval.indexOf("${") >= 0) ) {
		ARMAInterval = TSCommandProcessorUtil.expandParameterValue(processor, this, ARMAInterval);
	}
	String Alias = parameters.getValue ( "Alias" ); // Expanded below after creating time series
	String NewTSID = parameters.getValue ( "NewTSID" );
	if ( (NewTSID != null) && (NewTSID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		NewTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, NewTSID);
	}
	String Description = parameters.getValue ( "Description" ); // Expanded below
	if ( (Description != null) && (Description.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN)) {
		Description = TSCommandProcessorUtil.expandParameterValue(processor, this, Description);
	}
    String OutputMinimum = parameters.getValue ( "OutputMinimum" );
	if ( (commandPhase == CommandPhaseType.RUN) && (OutputMinimum != null) && (OutputMinimum.indexOf("${") >= 0) ) {
		OutputMinimum = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputMinimum);
	}
    double outputMinimum = Double.NaN;
    if ( (commandPhase == CommandPhaseType.RUN) && (OutputMinimum != null) && !OutputMinimum.isEmpty() ) {
    	try {
    		outputMinimum = Double.parseDouble(OutputMinimum);
    	}
    	catch ( NumberFormatException e ) {
    		message = "The OutputMinimum (" + OutputMinimum + ") is not a number.";
    		Message.printWarning(warning_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Check the OutputMinimum value." ) );
    	}
    }
    String OutputMaximum = parameters.getValue ( "OutputMaximum" );
	if ( (commandPhase == CommandPhaseType.RUN) && (OutputMaximum != null) && (OutputMaximum.indexOf("${") >= 0) ) {
		OutputMaximum = TSCommandProcessorUtil.expandParameterValue(processor, this, OutputMaximum);
	}
    double outputMaximum = Double.NaN;
    if ( (commandPhase == CommandPhaseType.RUN) && (OutputMaximum != null) && !OutputMaximum.isEmpty() ) {
		try {
	    	outputMaximum = Double.parseDouble(OutputMaximum);
		}
		catch ( NumberFormatException e ) {
    		message = "The OutputMaximum (" + OutputMaximum + ") is not a number.";
    		Message.printWarning(warning_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Check the OutputMaximum value." ) );
		}
    }

	// Get the time series to process...

    List<TS> tslist = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        // FIXME - SAM 2011-02-02 This gets all the time series, not just the ones matching the request!
        tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, EnsembleID );
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
    	// Do some runtime checks
    	if ( (inputPreviousValues.length > 0) && (inputPreviousValues.length != (bArray.length - 1)) ) {
	        message = "Number of input previous values (" + inputPreviousValues.length +
	        	") does not match number of b-coefficients (" + bArray.length + ") minus 1 (" + (bArray.length - 1) + ").";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
	        status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Check the number of output previous values." ) );
    	}
    	if ( (outputPreviousValues.length > 0) && (outputPreviousValues.length != aArray.length) ) {
	        message = "Number of output previous values (" + outputPreviousValues.length + ") does not match number of a-coefficients (" + aArray.length + ").";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
	        status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                        message, "Check the number of output previous values." ) );
    	}
    	// Get the time series to process
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
			List<TS> dataList = (List<TS>)o_TSList;
	        tslist = dataList;
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

	String OutputStart = parameters.getValue ( "OutputStart" );
	if ( (OutputStart == null) || OutputStart.isEmpty() ) {
		OutputStart = "${OutputStart}"; // Default global property
	}
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	if ( (OutputEnd == null) || OutputEnd.isEmpty() ) {
		OutputEnd = "${OutputEnd}"; // Default global property
	}
	DateTime OutputStart_DateTime = null;
	DateTime OutputEnd_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			OutputStart_DateTime = TSCommandProcessorUtil.getDateTime ( OutputStart, "OutputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			OutputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( OutputEnd, "OutputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
	}
    
	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Insufficient data to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}

	// Now process the time series...

	TS ts = null;
	List<TS> newtsList = new ArrayList<TS>();
	for ( int its = 0; its < nts; its++ ) {
		ts = tslist.get(its);
		if ( ts == null ) {
			// Skip time series.
            message = "Time series at position [" + its + "] is null - skipping.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
			continue;
		}
		
		notifyCommandProgressListeners ( its, nts, (float)-1.0, "Processing ARMA on " +
            ts.getIdentifier().toStringAliasAndTSID() );
		
		// Do the setting...
		Message.printStatus ( 2, routine, "Processing \"" + ts.getIdentifier()+ "\" using ARMA." );
		TS newts = null;
		try {
			if ( (NewTSID != null) && !NewTSID.isEmpty() ) {
				newts = null;
				try {
				    // Create the time series...
					newts = TSUtil.newTimeSeries ( NewTSID, true );
					newtsList.add(newts); // Need because need to add more than one time series at end
					if ( newts == null ) {
			            message = "Null time series returned when trying to create with NewTSID=\"" + NewTSID + "\"";
			            Message.printWarning ( warning_level,
			                    MessageUtil.formatMessageTag(
			                    command_tag,++warning_count),routine,message );
			            status.addToLog ( commandPhase,
			                    new CommandLogRecord(CommandStatusType.FAILURE,
			                            message, "Verify the NewTSID - contact software support if necessary." ) );
						throw new Exception ( "Null time series." );
					}
				}
				catch ( Exception e ) {
					message = "Unexpected error creating the new time series using NewTSID=\""+	NewTSID + "\".";
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(
						command_tag,++warning_count),routine,message );
					Message.printWarning(3,routine,e);
			        status.addToLog ( commandPhase,
			                new CommandLogRecord(CommandStatusType.FAILURE,
			                        message, "Verify the NewTSID - contact software support if necessary." ) );
					throw new CommandException ( message );
				}
				// Make sure intervals are the same
				if ( !TSUtil.intervalsMatch(ts,newts) ) {
					message = "Intervals for time series \"" + ts.getIdentifierString() + "\" and \"" +
						newts.getIdentifierString() + "\" do not match.";
					Message.printWarning ( warning_level,
						MessageUtil.formatMessageTag(
						command_tag,++warning_count),routine,message );
			        status.addToLog ( commandPhase,
		                new CommandLogRecord(CommandStatusType.FAILURE,
		                    message, "Verify the that NewTSID interval matches input time series." ) );
					throw new CommandException ( message );
				}
		        // Try to fill out the time series.  Allocate memory and set other information...
				newts.setIdentifier ( NewTSID );
				if ( commandPhase == CommandPhaseType.RUN ) {
					if ( (Description != null) && !Description.isEmpty() ) {
						newts.setDescription ( Description );
					}
					newts.setDataUnits(ts.getDataUnits());
					newts.setDataUnitsOriginal(ts.getDataUnitsOriginal());
					DateTime outputStart = new DateTime(ts.getDate1());
					DateTime outputEnd = new DateTime(ts.getDate2());
					// Use set to retain precision
					if ( OutputStart_DateTime != null ) {
						outputStart.setDate(OutputStart_DateTime);
					}
					if ( OutputEnd_DateTime != null ) {
						outputEnd.setDate(OutputEnd_DateTime);
					}
					newts.setDate1(outputStart);
					newts.setDate1Original(outputStart);
					newts.setDate2(outputEnd);
					newts.setDate2Original(outputEnd);
					newts.allocateDataSpace();
				}
			}
			if ( commandPhase == CommandPhaseType.RUN ) {
				// Do full processing
				TSUtil_ARMA tsu = new TSUtil_ARMA();
			    ts = tsu.ARMA ( ts, newts, ARMAInterval, aArray, bArray, inputPreviousValues, outputPreviousValues,
			    	outputMinimum, outputMaximum, OutputStart_DateTime, OutputEnd_DateTime );
			}
		    if ( newts != null ) {
		        if ( (Alias != null) && !Alias.isEmpty() ) {
		            String alias = Alias;
		            if ( commandPhase == CommandPhaseType.RUN ) {
		            	alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(processor, ts, Alias, status, commandPhase);
		            }
		            newts.setAlias ( alias );
		        }
		    }
		}
		catch ( Exception e ) {
			message = "Unexpected error processing time series \"" + ts.getIdentifier() + "\" using ARMA (" + e + ").";
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message);
			Message.printWarning(3,routine,e);
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
		}
	}
	
	if ( newtsList.size() > 0 ) {
		// Add the new time series to the processor
		if ( commandPhase == CommandPhaseType.RUN ) {
	    	// Update the data to the processor so that appropriate actions are taken...
	        TSCommandProcessorUtil.appendTimeSeriesListToResultsList(processor, this, newtsList);
		}
	    else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	        // Set in the discovery list
	        setDiscoveryTSList(newtsList);
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
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TSList",
    	"TSID",
    	"EnsembleID",
		"InputPreviousValues",
		"ARMAInterval",
    	"a",
		"b",
		"RequireCoefficientsSumTo1",
		"NewTSID",
		"Alias",
		"Description",
		"OutputStart",
		"OutputEnd",
		"OutputPreviousValues",
		"OutputMinimum",
		"OutputMaximum"
	};
	return this.toString(parameters, parameterOrder);
}

}