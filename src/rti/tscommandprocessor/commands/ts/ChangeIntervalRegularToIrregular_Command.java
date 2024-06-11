// ChangeIntervalRegularToIrregular_Command - class for ChangeIntervalRegularToIrregular command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSLimits;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_CalculateTimeSeriesStatistic;
import RTi.TS.TSUtil_ChangeInterval;
import RTi.TS.TSUtil_ChangeInterval_HandleEndpointsHowType;
import RTi.TS.TSUtil_ChangeInterval_HandleMissingInputHowType;
import RTi.TS.TSUtil_ChangeInterval_OutputFillMethodType;
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
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeScaleType;
import RTi.Util.Time.YearType;

/**
This command initializes and runs the ChangeIntervalRegularToIrregular() command.
*/
public class ChangeIntervalRegularToIrregular_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{

// Defines used by this class and its changeInterval_Dialog counterpart.
protected final String _Interpolate = "Interpolate";
protected final String _KeepMissing = "KeepMissing";
protected final String _Repeat = "Repeat";
protected final String _SetToZero = "SetToZero";
protected final String _IncludeFirstOnly = "IncludeFirstOnly";
protected final String _AverageEndpoints = "AverageEndpoints";

/**
Possible values for boolean parameters.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
TSEnsemble created in discovery mode (basically to get the identifier for other commands).
*/
private TSEnsemble __tsensemble = null;

/**
List of time series read during discovery.
These are TS objects but with mainly the metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Command constructor.
*/
public ChangeIntervalRegularToIrregular_Command () {
	super();
	setCommandName ( "ChangeIntervalRegularToIrregular" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// Get the properties from the PropList parameters.
	String Alias = parameters.getValue( "Alias" );
	String TSID = parameters.getValue( "TSID" );
	String EnsembleID = parameters.getValue( "EnsembleID" );
	String NewEnsembleID = parameters.getValue( "NewEnsembleID" );
	String NewInterval = parameters.getValue( "NewInterval" );
	String OldTimeScale = parameters.getValue( "OldTimeScale" );
	String NewTimeScale = parameters.getValue( "NewTimeScale" );
    String Statistic = parameters.getValue ( "Statistic" );
	String OutputYearType = parameters.getValue ( "OutputYearType" );
	String Tolerance = parameters.getValue( "Tolerance" );
	String HandleEndpointsHow = parameters.getValue( "HandleEndpointsHow" );
	String AllowMissingCount = parameters.getValue("AllowMissingCount" );
	/* TODO SAM 2005-02-18 may enable later.
	String AllowMissingPercent= parameters.getValue("AllowMissingPercent");
	*/
	String AllowMissingConsecutive = parameters.getValue("AllowMissingConsecutive" );
	String OutputFillMethod = parameters.getValue( "OutputFillMethod" );
	String HandleMissingInputHow = parameters.getValue( "HandleMissingInputHow" );
	String RecalcLimits = parameters.getValue ( "RecalcLimits" );

	// Alias must be specified - for historical command syntax (and generally a good idea).
	// TODO [LT 2005-05-24] How about the __read_one issue (see parseCommand() method).
	if ( Alias == null || Alias.length() == 0 ) {
        message = "The time series alias must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Provide a time series alias when defining the command."));
	}

	// Check if the alias for the new time series is the same as the alias used by one of the time series in memory.
	// If so print a warning.
	// TODO [LT 2005-05-26] This is used in all other command but it is not working here. Why? Temporarily using the alternative below.
	/*	Vector tsids = (Vector) getCommandProcessor().getPropContents ( "TSIDListNoInput" );
	if ( StringUtil.indexOf( tsids, Alias ) >= 0 ) {
		warning += "\nTime series alias \"" + Alias + "\" is already used above.";
	}
	 */
	// Check if the alias for the new time series is the same as the alias used by the original time series.
	// If so print a warning.
	// TODO [LT 2005-05-26] Would this alternative be more appropriated?
	// Notice: The version above is the one used by the others commands.
	if ( (Alias != null) && (TSID != null) && TSID.equalsIgnoreCase( Alias ) ) {
        message = "The alias \"" + Alias
        + "\" for the new time series is equal to the alias of the original time series.";
		warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message,
                "Specify different time series for input and output."));
	}

	// Verify that a NewEnsembleID is specified only if an input EnsembleID is specified.
	if ( (NewEnsembleID != null) && !NewEnsembleID.equals("") && ((EnsembleID == null) || EnsembleID.equals(""))) {
        message = "The NewEnsembleID can only be specified when the input time series are specified using EnsembleID.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Specify input as an ensemble or clear the NewEnsembleID."));
	}

    TimeInterval newInterval = null;
	if ( NewInterval == null || (NewInterval.length() == 0) ) {
		message = "The new interval must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a new interval."));
	}
	else {
	    try {
	        newInterval = TimeInterval.parseInterval(NewInterval);
	    }
	    catch ( Exception e ) {
	        // Should not happen because choices are valid.
	        message = "The new interval \"" + NewInterval + "\" is invalid.";
	        warning += "\n" + message;
	        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a new interval using the command editor."));
	    }
	}

	// OldTimeScale - OldTimeScale will always be set from the changeInterval_JDialog when the OK button is pressed,
	// but the user may edit the command without using the changeInterval_JDialog editor and try to run it,
	// so this method should at least make sure the OldTimeScale property is given.
	if ( OldTimeScale != null && OldTimeScale.length() == 0 ) {
        message = "The old time scale must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify an old time scale."));
	}
	TimeScaleType oldTimeScaleType = null;
	if ( OldTimeScale != null && !OldTimeScale.equals("") ) {
	    try {
	        oldTimeScaleType = TimeScaleType.valueOfIgnoreCase(OldTimeScale);
	    }
	    catch ( Exception e ) {
    		message = "The old time scale (" + OldTimeScale + ") is invalid.";
            warning += "\n" + message;
            StringBuffer b = new StringBuffer();
            List<TimeScaleType> values = TimeScaleType.getTimeScaleChoices();
            for ( TimeScaleType t : values ) {
                if ( b.length() > 0 ) {
                    b.append ( ", " );
                }
                b.append ( t.toString() );
            }
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Valid values are:  " + b.toString() + "."));
	    }
	}

	// NewTimeScale - NewTimeScale will always be set from the ChangeInterval_JDialog when the OK button is pressed,
	// but the user may edit the command without using the changeInterval_JDialog editor and try to run it,
	// so this method should at least make sure the NewTimeScale property is given.
	TimeScaleType newTimeScaleType = null;
	if ( (NewTimeScale != null) && NewTimeScale.length() == 0 ) {
        message = "The new time scale must be specified.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify a new time scale."));
	}
	if ( (NewTimeScale != null) && !NewTimeScale.equals("") ) {
        try {
            newTimeScaleType = TimeScaleType.valueOfIgnoreCase(NewTimeScale);
        }
        catch ( Exception e ) {
            message = "The new time scale (" + NewTimeScale + ") is invalid.";
            warning += "\n" + message;
            StringBuffer b = new StringBuffer();
            List<TimeScaleType> values = TimeScaleType.getTimeScaleChoices();
            for ( TimeScaleType t : values ) {
                if ( b.length() > 0 ) {
                    b.append ( ", " );
                }
                b.append ( t.toString() );
            }
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Valid values are:  " + b.toString() + "."));
        }
	}

    if ( (Statistic != null) && !Statistic.equals("") && (oldTimeScaleType != TimeScaleType.INST)&&
        (newTimeScaleType != TimeScaleType.INST) ) {
        message = "The statistic is only valid when converting from time scale " + TimeScaleType.INST +
        " (small interval) to " + TimeScaleType.INST + " (big interval).";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Do not specify the statistic." ) );
    }
    else if ( (Statistic != null) && !Statistic.equals("") ) {
        // Make sure that the statistic is known in general.
        boolean supported = false;
        TSStatisticType statisticType = null;
        try {
            statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
            supported = true;
        }
        catch ( Exception e ) {
            message = "The statistic (" + Statistic + ") is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported statistic using the command editor." ) );
        }

        // Make sure that it is in the supported list.

        if ( supported ) {
            supported = false;
            List<TSStatisticType> statistics = TSUtil_CalculateTimeSeriesStatistic.getStatisticChoices();
            for ( TSStatisticType statistic : statistics ) {
                if ( statisticType == statistic ) {
                    supported = true;
                    break;
                }
            }
            if ( !supported ) {
                message = "The statistic (" + Statistic + ") is not supported by this command.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Select a supported statistic using the command editor." ) );
            }
        }
    }

    if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
        try {
            YearType.valueOfIgnoreCase(OutputYearType);
        }
        catch ( Exception e ) {
            message = "The output year type (" + OutputYearType + ") is invalid.";
            warning += "\n" + message;
            StringBuffer b = new StringBuffer();
            List<YearType> values = YearType.getYearTypeChoices();
            for ( YearType t : values ) {
                if ( b.length() > 0 ) {
                    b.append ( ", " );
                }
                b.append ( t.toString() );
            }
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Valid values are:  " + b.toString() + "."));
        }
    }

	// If the AllowMissingCount is specified, it should be an integer.
    Integer allowMissingCount = null;
	if ( AllowMissingCount!=null && (AllowMissingCount.length()>0) ) {
	    if ( !StringUtil.isInteger(AllowMissingCount) ) {
            message = "Allow missing count \"" + AllowMissingCount + "\" is not an integer.";
    		warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify the allowed missing count as an interger."));
	    }
	    else {
	        allowMissingCount = new Integer(AllowMissingCount);
	    }
	}

	// If the AllowMissingConsecutive is specified, it should be an integer.
	Integer allowMissingConsecutive = null;
    if ( AllowMissingConsecutive!=null && (AllowMissingConsecutive.length()>0) ) {
        if ( !StringUtil.isInteger(AllowMissingConsecutive) ) {
            message = "Allow missing consecutive value \"" + AllowMissingConsecutive + "\" is not an integer.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify the allowed missing consecutive value as an interger."));
        }
        else {
            allowMissingConsecutive = new Integer(AllowMissingConsecutive);
        }
    }

    if ( (allowMissingCount != null) && (allowMissingConsecutive != null) &&
        (allowMissingCount < allowMissingConsecutive) ) {
        message = "Allow missing consecutive value \"" + AllowMissingConsecutive +
            "\" is > the allowed missing count (" + allowMissingCount + ").";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(
            CommandStatusType.FAILURE, message,
            "Specify the allowed missing consecutive value <= the allowed missing count"));
    }

	// If the Tolerance is specified, it should be a double.
	if ( Tolerance!=null && (Tolerance.length()>0) &&
		( !StringUtil.isDouble(Tolerance) || StringUtil.atod(Tolerance) < 0  ||
        StringUtil.atod(Tolerance) > 1 )) {
        message = "Tolerance \"" + Tolerance + "\" must be a number between 0 and 1 (0.01 = 1 percent).";
		warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify the allowed missing count as an interger."));

	}

    // If the HandleEndpointsHow is specified, make sure it is valid.
	if ( HandleEndpointsHow!=null && HandleEndpointsHow.length()>0 ) {
		if (!HandleEndpointsHow.equalsIgnoreCase(_IncludeFirstOnly)&&
			!HandleEndpointsHow.equalsIgnoreCase(_AverageEndpoints)){
            message = "The HandleEndpointsHow (" + HandleEndpointsHow + ") parameter is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message, "Valid values are \"" + _IncludeFirstOnly
                        + ", and \"" + _AverageEndpoints + "\"."));
		}
		else {
		    // Make sure that it is only specified for INST to MEAN.
		    if ( (oldTimeScaleType == TimeScaleType.INST) && (newTimeScaleType == TimeScaleType.MEAN) &&
		        (newInterval.getBase() <= TimeInterval.DAY) ) {
		        // OK
		        // TODO SAM 2010-04-08 Also would like to check that the new interval is < Day but can't do at initialization.
		    }
		    else {
		        // Combination is not allowed.
	            message = "The HandleEndpointsHow (" + HandleEndpointsHow +
	            ") parameter is not supported with the input combination.";
	            warning += "\n" + message;
	            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message, "Only specify the parameter when changing from " +
                    TimeScaleType.INST + " to " + TimeScaleType.MEAN + " small to larger (day or less) interval."));
		    }
		}
	}

	// If the AllowMissingPercent is specified, it should be an number.
	/* TODO SAM 2005-02-18 may enable later.
	if ( AllowMissingPercent!=null && (AllowMissingPercent.length()>0) &&
		!StringUtil.isDouble(AllowMissingPercent) ) {
		warning += "\nAllow missing percent \"" + AllowMissingPercent
			+ "\" is not a number.";
	}

	// Only one of AllowMissingCount and AllowMissingPercent can be specified.
	if ( (AllowMissingCount.length() > 0) &&
	     (AllowMissingPercent.length() > 0) ) {
		warning += "\nOnly one of AllowMissingCount and "
			+ "AllowMissingPercent can be specified.";
	} */

	// If the OutputFillMethod is specified, make sure it is valid.
	if ( OutputFillMethod != null && OutputFillMethod.length() > 0 ) {
		if ( !OutputFillMethod.equalsIgnoreCase( _Repeat ) &&
			!OutputFillMethod.equalsIgnoreCase( _Interpolate ) ) {
            message = "The OutputFillMethod (" + OutputFillMethod + ") parameter is invalid.";
			warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message, "Valid values are \"" + _Interpolate
                        + "\" and \"" + _Repeat + "\"."));
		}
	}

	// If the HandleMissingInputHow is specified, make sure it is valid.
	if ( HandleMissingInputHow!=null && HandleMissingInputHow.length()>0 ) {
		if ( !HandleMissingInputHow.equalsIgnoreCase(_KeepMissing)&&
			!HandleMissingInputHow.equalsIgnoreCase(_Repeat)&&
			!HandleMissingInputHow.equalsIgnoreCase(_SetToZero )){
            message = "The HandleMissingInputHow (" + HandleMissingInputHow + ") parameter is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message, "Valid values are \"" + _KeepMissing
                        + "\", " + _Repeat + ", and \"" + _SetToZero + "\"."));
		}
	}

    if ( (RecalcLimits != null) && !RecalcLimits.equals("") &&
        !RecalcLimits.equalsIgnoreCase( "true" ) && !RecalcLimits.equalsIgnoreCase("false") ) {
        message = "The RecalcLimits parameter must be blank, " + _False + " (default), or " + _True + ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a RecalcLimits as " + _False + " or " + _True + ".") );
    }

    // Check for invalid parameters.
	List<String> validList = new ArrayList<>(27);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "Alias" );
    validList.add ( "NewEnsembleID" );
    validList.add ( "NewEnsembleName" );
    validList.add ( "NewInterval" );
    validList.add ( "OldTimeScale" );
    validList.add ( "NewTimeScale" );
    validList.add ( "Statistic" );
    validList.add ( "OutputYearType" );
    validList.add ( "NewDataType" );
    validList.add ( "NewUnits" );
    validList.add ( "Tolerance" );
    validList.add ( "HandleEndpointsHow" );
    validList.add ( "AllowMissingCount" );
    validList.add ( "AllowMissingConsecutive" );
    validList.add ( "OutputFillMethod" );
    validList.add ( "HandleMissingInputHow" );
    validList.add ( "RecalcLimits" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, warning_level ), warning );
		throw new InvalidCommandParameterException ( warning );
	}

    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
	return ( new ChangeIntervalRegularToIrregular_JDialog ( parent, this ) ).ok();
}

/**
Return the ensemble that is read by this class when run in discovery mode.
*/
private TSEnsemble getDiscoveryEnsemble() {
    return __tsensemble;
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList () {
    return __discoveryTSList;
}

/**
Return the list of data objects created by this object in discovery mode.
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    TSEnsemble tsensemble = getDiscoveryEnsemble();
    List<TS> discoveryTSList = getDiscoveryTSList ();

    // TODO SAM 2011-03-31 Does the following work as intended?
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS).
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    TS datats = discoveryTSList.get(0);
    // Use the most generic for the base class.
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
            return null;
        }
        else {
            return (List<T>)discoveryTSList;
        }
    }
    else if ( (tsensemble != null) && (c == tsensemble.getClass()) ) {
        List<T> v = new ArrayList<>();
        v.add ( (T)tsensemble );
        return v;
    }
    else {
        return null;
    }
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException {
	String mthd = getClass().getSimpleName() +  "changeInterval_Command.parseCommand";
	String mssg;
	int warning_level = 2;

	if ( Message.isDebugOn ) {
		mssg = "Command to parse is: " + command;
		Message.printDebug ( 10, mthd, mssg );
	}

    if ( !command.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation.
        super.parseCommand(command);
    }
    else {
    	String Alias = "";

        // TODO SAM 2007-11-29 Is this envisioned to process multiple time series?
    	// Since this command is of the type TS X = changeInterval (...), we first need to parse the Alias (the X in the command).
    	String substring = "";
    	if ( command.indexOf('=') >= 0 ) {
    		// Because the parameters contain =, find the first = to break the assignment TS X = changeInterval (...).
    		int pos = -1; // Will be incremented to zero if !__read_one.
			// TS X = changeInterval (...)
			pos = command.indexOf('=');
			substring = command.substring(0,pos).trim();
			List<String> v = StringUtil.breakStringList ( substring, " ", StringUtil.DELIM_SKIP_BLANKS );
			// First field has format "TS X"
			Alias = (v.get(1)).trim();

    		// Substring, eliminating "TS X =" when __read_one is true.
    		// The result substring in any case will contain only the changeInterval (...) part of the command.
    		substring = command.substring(pos + 1).trim();

    		// Split the substring into two parts: the command name and the parameters list within the parenthesis.
    		List<String> tokens = StringUtil.breakStringList ( substring, "()", 0 );
    		if ( tokens == null ) {
    			// Must have at least the command name and the parameter list.
    			mssg = "Syntax error in \"" + command + "\".";
    			Message.printWarning ( warning_level, mthd, mssg);
    			throw new InvalidCommandSyntaxException ( mssg );
    		}

    		// Parse the parameters (second token in the tokens list) needed to process the command.
    		try {
    			setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT, tokens.get(1), mthd, "," ) );
    			// If the Alias was found in the command added it to the parameters propList.
    			if ( Alias != null && Alias.length() > 0 ) {
    				setCommandParameter( "Alias", Alias );

    				if ( Message.isDebugOn ) {
    					mssg = "Alias is: " + Alias;
    					Message.printDebug ( 10, mthd, mssg );
    				}
    			}
    		}
    		catch ( Exception e ) {
    			mssg = "Syntax error in \"" + command + "\".  Not enough tokens.";
    			Message.printWarning ( warning_level, mthd, mssg );
    			throw new InvalidCommandSyntaxException ( mssg );
    		}
    	}
    }
    // Possible because of support of legacy and new format that "TS Alias = TSID" equivalent was parsed but TSID is not specified.
    // Therefore, handle conversion.
    PropList parameters = getCommandParameters();
    String TSID = parameters.getValue("TSID");
    String TSList = parameters.getValue("TSList");
    if ( (TSID != null) && !TSID.equals("") ) {
        if ( (TSList == null) || TSList.equals("") ) {
            // Legacy behavior was to match last matching TSID.
            parameters.set("TSList=" + TSListType.LAST_MATCHING_TSID );
        }
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
    int warningLevel, int warning_count, String command_tag ) {
    String routine = getClass().getSimpleName() + ".recalculateLimits", message;
    CommandStatus status = getCommandStatus();
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
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        return warning_count;
    }
    // Get the calculated limits and set in the original data limits.
    PropList bean_PropList = bean.getResultsPropList();
    Object prop_contents = bean_PropList.getContents ( "TSLimits" );
    if ( prop_contents == null ) {
        message = "Null value for TSLimits from CalculateTSAverageLimits(" + ts.getIdentifierString() + ")";
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
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
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
CommandWarningException, CommandException {
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommandInternal";
	String message = "";
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3; // Warning message level for non-user messages.

    CommandStatus status = getCommandStatus();
    TSCommandProcessor processor = (TSCommandProcessor)getCommandProcessor();
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
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
        setDiscoveryEnsemble ( null );
    }

	PropList parameters = getCommandParameters();
    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = "" + TSListType.ALL_TS;
    }
    String TSID = parameters.getValue ( "TSID" );
	if ( (TSID != null) && (TSID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String Alias = parameters.getValue ( "Alias" );
    String NewEnsembleID = parameters.getValue( "NewEnsembleID" );
    String NewEnsembleName = parameters.getValue( "NewEnsembleName" );
	String NewInterval = parameters.getValue( "NewInterval"  );
	TimeInterval newInterval = null;
	try {
	    newInterval = TimeInterval.parseInterval(NewInterval);
	}
	catch ( Exception e ) {
        message = "New interval \"" + NewInterval + "\" is not valid.";
        Message.printWarning(log_level,
            MessageUtil.formatMessageTag( command_tag, ++warning_count),
            routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid new interval for the time series - see documentation." ) );
	}
	String OldTimeScale = parameters.getValue( "OldTimeScale" );
	TimeScaleType oldTimeScale = TimeScaleType.valueOfIgnoreCase(OldTimeScale);
	String NewTimeScale = parameters.getValue( "NewTimeScale" );
	TimeScaleType newTimeScale = TimeScaleType.valueOfIgnoreCase(NewTimeScale);
    String Statistic = parameters.getValue ( "Statistic" );
    TSStatisticType statisticType = null;
    if ( (Statistic != null) && (Statistic.length() > 0) ) {
        statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
    }
	String OutputYearType = parameters.getValue( "OutputYearType" );
	YearType outputYearType = YearType.CALENDAR;
	if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
	    outputYearType = YearType.valueOfIgnoreCase(OutputYearType);
	}
	String NewDataType = parameters.getValue( "NewDataType" );
	String NewUnits = parameters.getValue( "NewUnits" );
	String Tolerance = parameters.getValue( "Tolerance" );
	Double tolerance = null;
	if ( StringUtil.isDouble(Tolerance) ) {
	    tolerance = Double.parseDouble(Tolerance);
	}
	String HandleEndpointsHow = parameters.getValue( "HandleEndpointsHow" );
	TSUtil_ChangeInterval_HandleEndpointsHowType handleEndpointsHow = null;
	if ( (HandleEndpointsHow != null) && !HandleEndpointsHow.equals("") ) {
	    handleEndpointsHow =
	        TSUtil_ChangeInterval_HandleEndpointsHowType.valueOfIgnoreCase(HandleEndpointsHow);
	}
	String AllowMissingCount = parameters.getValue("AllowMissingCount" );
	Integer allowMissingCount = null;
	if ( StringUtil.isInteger(AllowMissingCount) ) {
	    allowMissingCount = new Integer(AllowMissingCount);
	}
	/* TODO SAM 2005-02-18 may enable later.
	String	AllowMissingPercent= _parameters.getValue("AllowMissingPercent");
	*/
    String AllowMissingConsecutive = parameters.getValue("AllowMissingConsecutive" );
    Integer allowMissingConsecutive = null;
    if ( StringUtil.isInteger(AllowMissingConsecutive) ) {
        allowMissingConsecutive = new Integer(AllowMissingConsecutive);
    }
	String OutputFillMethod = parameters.getValue( "OutputFillMethod" );
	TSUtil_ChangeInterval_OutputFillMethodType outputFillMethod = null;
	if ( (OutputFillMethod != null) && !OutputFillMethod.equals("") ) {
	    outputFillMethod = TSUtil_ChangeInterval_OutputFillMethodType.valueOfIgnoreCase(OutputFillMethod);
	}
	String HandleMissingInputHow = parameters.getValue( "HandleMissingInputHow" );
	TSUtil_ChangeInterval_HandleMissingInputHowType handleMissingInputHow = null;
	if ( (HandleMissingInputHow != null) && !HandleMissingInputHow.equals("") ) {
	    handleMissingInputHow =
	        TSUtil_ChangeInterval_HandleMissingInputHowType.valueOfIgnoreCase(HandleMissingInputHow);
	}
	String RecalcLimits = parameters.getValue ( "RecalcLimits" );
    boolean recalcLimits = false; // Default
    if ( (RecalcLimits != null) && RecalcLimits.equalsIgnoreCase("true") ) {
        recalcLimits = true;
    }

    // Get the time series to process.

    List<TS> tslist = null;
    boolean createData = true; // Whether to fill in the data array.
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command.
        // FIXME - SAM 2011-02-02 This gets all the time series, not just the ones matching the request!
        tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, EnsembleID );
        createData = false;
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
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message,
                            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
        else {
        	@SuppressWarnings("unchecked")
			List<TS> tslist0 = (List<TS>)o_TSList;
            tslist = tslist0;
            if ( tslist.size() == 0 ) {
                message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
                Message.printWarning ( log_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE, message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
            }
        }
    }

    int nts = tslist.size();
    if ( nts == 0 ) {
        message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	// Error is OK, especially with dynamic content.
        }
        else {
	        Message.printWarning ( warning_level,
	        MessageUtil.formatMessageTag(
	        command_tag,++warning_count), routine, message );
	        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
	            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
    }

    if ( warning_count > 0 ) {
        // Input error.
        message = "Insufficient data to run command.";
        status.addToLog ( commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

	// If here, have enough input to attempt the changing the interval.
    TS original_ts = null; // Original (input) time series.
	TS result_ts = null; // Result time series.
	List<TS> resultList = new ArrayList<>();
    for ( int its = 0; its < nts; its++ ) {
        original_ts = tslist.get(its);
        notifyCommandProgressListeners ( its, nts, (float)-1.0, "Changing interval for " +
            original_ts.getIdentifier().toStringAliasAndTSID() );
    	try {
    		// Process the change of interval.
    	    TSUtil_ChangeInterval tsu = new TSUtil_ChangeInterval (
    	    	original_ts,
    	    	newInterval,
                oldTimeScale,
    	    	newTimeScale,
    	    	statisticType,
    	    	outputYearType,
    	    	NewDataType,
    	    	NewUnits,
    	    	tolerance,
                handleEndpointsHow,
    	    	outputFillMethod,
    	    	handleMissingInputHow,
    	    	allowMissingCount,
                null, // AllowMissingPercent (not implemented in command).
                allowMissingConsecutive );
    		result_ts = tsu.changeInterval ( createData );
    		if ( (commandPhase == CommandPhaseType.RUN) && recalcLimits ) {
    		    warning_count = recalculateLimits( result_ts, processor,
    		        warning_level, warning_count, command_tag );
    		}
    		resultList.add(result_ts);

    		// Update the newly created time series alias (alias is required).
            if ( (Alias != null) && !Alias.equals("") ) {
                String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                    processor, result_ts, Alias, status, commandPhase);
                result_ts.setAlias ( alias );
            }

    		// Add the newly created time series to the software memory.
    	    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
    	        // Just want time series headers initialized.
    	        setDiscoveryTSList ( resultList ); // OK to reset each time.
    	    }
    	    if ( commandPhase == CommandPhaseType.RUN ) {
    	        // Add single time series.
    	        TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, result_ts );
    	    }
    	}
        catch ( IllegalArgumentException e ) {
            message = "Error changing the interval for TSID=\"" + TSID + "\" (" + e + ").";
    		Message.printWarning(warning_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
    		Message.printWarning ( log_level, routine, e );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Consult documentation for available parameter combinations." ) );
            throw new CommandWarningException ( message );
        }
    	catch ( Exception e ) {
    		message = "Unexpected error changing the interval for TSID=\"" + TSID + "\" (" + e + ").";
    		Message.printWarning(warning_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
    		Message.printWarning ( log_level, routine, e );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
    		throw new CommandWarningException ( message );
    	}
    }

    // If processing an ensemble, create the new ensemble.

    if ( (NewEnsembleID != null) && !NewEnsembleID.equals("") ) {
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create a discovery ensemble with ID and name.
            TSEnsemble ensemble = new TSEnsemble ( NewEnsembleID, NewEnsembleName, resultList );
            setDiscoveryEnsemble ( ensemble );
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
            // Add the ensemble to the processor if created.
            TSEnsemble ensemble = new TSEnsemble ( NewEnsembleID, NewEnsembleName, resultList );
            TSCommandProcessorUtil.appendEnsembleToResultsEnsembleList(processor, this, ensemble);
        }
    }

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ), routine, message );
		throw new CommandWarningException ( message );
	}
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the ensemble that is processed by this class in discovery mode.
*/
private void setDiscoveryEnsemble ( TSEnsemble tsensemble ) {
    __tsensemble = tsensemble;
}

/**
Set the list of time series read in discovery phase.
@param discoveryTSList list of time series created during discovery phase
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList ) {
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props ) {
    return toString ( props, 10 );
}

/**
Return the string representation of the command.
@param parameters to include in the command
@param majorVersion the major version for software - if less than 10, the "TS Alias = " notation is used,
allowing command files to be saved for older software, no longer supported
@return the string representation of the command
*/
public String toString ( PropList parameters, int majorVersion ) {
	String [] parameterOrder = {
    	"TSList",
    	"TSID",
    	"EnsembleID",
		"NewEnsembleID",
		"NewEnsembleName",
		"Alias",
		"NewInterval",
		"OldTimeScale",
		"NewTimeScale",
		"Statistic",
		"OutputYearType",
		"NewDataType",
		"NewUnits",
		"Tolerance",
		"HandleEndpointsHow",
		"AllowMissingCount",
		// TODO SAM 2005-02-18 may enable later.
		//"AllowMissingPercent",
		"AllowMissingConsecutive",
		"OutputFillMethod",
		"HandleMissingInputHow",
		"RecalcLimits"
	};
	return this.toString(parameters, parameterOrder);
}

}