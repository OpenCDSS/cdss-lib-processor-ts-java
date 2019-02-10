// NewStatisticTimeSeriesFromEnsemble_Command - This class initializes, checks, and runs the NewStatisticTimeSeriesFromEnsemble() command.

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

package rti.tscommandprocessor.commands.ensemble;

import javax.swing.JFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSStatisticType;
import RTi.TS.TSUtil_NewStatisticTimeSeriesFromEnsemble;
import RTi.Util.Math.DistributionType;
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
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the NewStatisticTimeSeriesFromEnsemble() command.
*/
public class NewStatisticTimeSeriesFromEnsemble_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{
    
/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public NewStatisticTimeSeriesFromEnsemble_Command ()
{	super();
	setCommandName ( "NewStatisticTimeSeriesFromEnsemble" );
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
{	String Alias = parameters.getValue ( "Alias" );
	String EnsembleID = parameters.getValue ( "EnsembleID" );
    String Distribution = parameters.getValue ( "Distribution" );
    String ProbabilityUnits = parameters.getValue ( "ProbabilityUnits" );
	String Statistic = parameters.getValue ( "Statistic" );
    String Value1 = parameters.getValue ( "Value1" );
	String AllowMissingCount = parameters.getValue ( "AllowMissingCount" );
	String MinimumSampleSize = parameters.getValue ( "MinimumSampleSize" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
    String OutputStart = parameters.getValue ( "OutputStart" );
    String OutputEnd = parameters.getValue ( "OutputEnd" );
	String warning = "";
    String message;
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	TSUtil_NewStatisticTimeSeriesFromEnsemble tsu = new TSUtil_NewStatisticTimeSeriesFromEnsemble();

	if ( (Alias == null) || Alias.equals("") ) {
		message = "The time series alias must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify an alias." ) );
	}
	if ( (EnsembleID == null) || EnsembleID.equals("") ) {
		message = "The time series ensemble identifier must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify an ensemble identifier." ) );
	}
	// TODO SAM 2005-08-29 Need to decide whether to check NewTSID - it might need to support wildcards.
	DistributionType distType = null;
    if ( (Distribution != null) && !Distribution.equals("") ) {
        List<String> distChoices = tsu.getDistributionChoicesAsStrings();
        for ( String d : distChoices ) {
            if ( d.equalsIgnoreCase(Distribution) ) {
            	distType = DistributionType.valueOfIgnoreCase(d);
                break;
            }
        }
        if ( distType == null ) {
            message = "The distribution (" + Distribution + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a distribution as one of the choices in the command editor." ) );
        }
    }
    
    if ( (ProbabilityUnits != null) && !ProbabilityUnits.equals("") && !ProbabilityUnits.equalsIgnoreCase("Fraction") &&
        !ProbabilityUnits.equalsIgnoreCase("Percent") && !ProbabilityUnits.equalsIgnoreCase("%")) {
        message = "The probability units (" + ProbabilityUnits + ") are invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a the probability units as Fraction, Percent, or %." ) );
    }
    
	if ( (Statistic == null) || Statistic.equals("") ) {
		message = "The statistic must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify a Statistic." ) );
	}
    else {
        // Make sure that the statistic is known in general
        boolean supported = false;
        TSStatisticType statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
        if ( statisticType == null ) {
            message = "The statistic (" + Statistic + ") is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported statistic using the command editor." ) );
        }
        else {
            // Make sure that it is in the supported list
            supported = false;
            List<TSStatisticType> statistics = tsu.getStatisticChoices();
            for ( TSStatisticType statistic : statistics ) {
                if ( statisticType == statistic ) {
                    supported = true;
                }
            }
            if ( !supported ) {
                message = "The statistic (" + Statistic + ") is not supported by this command.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Select a supported statistic using the command editor." ) );
            }
            if ( (distType != null) && !tsu.isStatisticSupportedForDistribution(statisticType, distType) ) {
            	message = "The statistic (" + Statistic + ") is not supported for distribution (" + Distribution + ").";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Don't specify distribution to get simple statistics (e.g., Mean of sample values, rather than Mean for distribution)." ) );
            }
        }
	
	    // Additional checks that depend on the statistic
	    
	    if ( supported ) {
	        int nRequiredValues = -1;
	        try {
	            nRequiredValues = tsu.getRequiredNumberOfValuesForStatistic ( statisticType );
	        }
	        catch ( Exception e ) {
	            message = "Statistic \"" + statisticType + "\" is not recognized.";
	            warning += "\n" + message;
	            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Contact software support." ) );
	        }
	        
	        if ( nRequiredValues >= 1 ) {
	            if ( (Value1 == null) || Value1.equals("") ) {
	                message = "Value1 must be specified for the statistic.";
	                warning += "\n" + message;
	                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Provide Value1." ) );
	            }
	            else if ( !StringUtil.isDouble(Value1) ) {
	                message = "Value1 (" + Value1 + ") is not a number.";
	                warning += "\n" + message;
	                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Specify Value1 as a number." ) );
	            }
	        }
	    }
    }

	// TODO SAM 2005-09-12
	// Need to evaluate whether the test value is needed, depending on the statistic
    if ( (AllowMissingCount != null) && !AllowMissingCount.equals("") ) {
        if ( !StringUtil.isInteger(AllowMissingCount) ) {
            message = "The AllowMissingCount value (" + AllowMissingCount + ") is not an integer.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify an integer for AllowMissingCount." ) );
        }
        else {
            // Make sure it is an allowable value >= 0...
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
            // Make sure it is an allowable value >= 0...
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

	if ( (AnalysisStart != null) && !AnalysisStart.equals("") &&
		!AnalysisStart.equalsIgnoreCase("OutputStart") && !AnalysisStart.equalsIgnoreCase("OutputEnd") &&
		(AnalysisStart.indexOf("${") < 0)) {
		try {	DateTime.parse(AnalysisStart);
		}
		catch ( Exception e ) {
			message = "The analysis start \"" + AnalysisStart + "\" is not a valid date/time.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
		}
	}
	if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") &&
		!AnalysisEnd.equalsIgnoreCase("OutputStart") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") &&
		(AnalysisEnd.indexOf("${") < 0)) {
		try {	DateTime.parse( AnalysisEnd );
		}
		catch ( Exception e ) {
			message = "The analysis end \"" + AnalysisEnd + "\" is not a valid date/time.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
		}
	}
	
    if ( (OutputStart != null) && !OutputStart.equals("") &&
          !OutputStart.equalsIgnoreCase("OutputStart") && !OutputStart.equalsIgnoreCase("OutputEnd") &&
          (OutputStart.indexOf("${") < 0)) {
          try {
              DateTime.parse(OutputStart);
          }
          catch ( Exception e ) {
              message = "The output start \"" + OutputStart + "\" is not a valid date/time.";
              warning += "\n" + message;
              status.addToLog ( CommandPhaseType.INITIALIZATION,
                      new CommandLogRecord(CommandStatusType.FAILURE,
                              message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
          }
      }
      if ( (OutputEnd != null) && !OutputEnd.equals("") &&
          !OutputEnd.equalsIgnoreCase("OutputStart") && !OutputEnd.equalsIgnoreCase("OutputEnd") &&
          (OutputEnd.indexOf("${") < 0)) {
          try {
              DateTime.parse( OutputEnd );
          }
          catch ( Exception e ) {
              message = "The output end \"" + OutputEnd + "\" is not a valid date/time.";
              warning += "\n" + message;
              status.addToLog ( CommandPhaseType.INITIALIZATION,
                      new CommandLogRecord(CommandStatusType.FAILURE,
                              message, "Specify a valid date/time, OutputStart, or OutputEnd." ) );
          }
      }
	
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(15);
	validList.add ( "EnsembleID" );
    validList.add ( "Distribution" );
    validList.add ( "DistributionParameters" );
    validList.add ( "ProbabilityUnits" );
	validList.add ( "Statistic" );
	validList.add ( "Value1" );
	validList.add ( "AllowMissingCount" );
	validList.add ( "MinimumSampleSize" );
	validList.add ( "AnalysisStart" );
	validList.add ( "AnalysisEnd" );
	validList.add ( "NewTSID" );
	validList.add ( "Alias" );
	validList.add ( "Description" );
    validList.add ( "OutputStart" );
    validList.add ( "OutputEnd" );
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
	return (new NewStatisticTimeSeriesFromEnsemble_JDialog ( parent, this )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
    List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
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
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = getClass().getSimpleName() + ".parseCommand", message;

    if ( !command.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation
        super.parseCommand(command);
    }
    else {
    	// Get the part of the command after the TS Alias =...
    	int pos = command.indexOf ( "=" );
    	if ( pos < 0 ) {
    		message = "Syntax error in \"" + command +
    			"\".  Expecting:  TS Alias = NewStatisticTimeSeriesFromEnsemble(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	String token0 = command.substring ( 0, pos ).trim();
    	String token1 = command.substring ( pos + 1 ).trim();
    	if ( (token0 == null) || (token1 == null) ) {
    		message = "Syntax error in \"" + command +
    			"\".  Expecting:  TS Alias = NewStatisticTimeSeriesFromEnsemble(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    
    	List<String> v = StringUtil.breakStringList ( token0, " ",StringUtil.DELIM_SKIP_BLANKS );
    	if ( (v == null) || (v.size() != 2) ) {
    		message = "Syntax error in \"" + command +
    			"\".  Expecting:  TS Alias = NewStatisticTimeSeriesFromEnsemble(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	String Alias = v.get(1);
    	List<String> tokens = StringUtil.breakStringList ( token1, "()", 0 );
    	if ( (tokens == null) || tokens.size() < 2 ) {
    		// Must have at least the command name and its parameters...
    		message = "Syntax error in \"" + command + "\". Expecting:  TS Alias = NewStatisticTimeSeriesFromEnsemble(...)";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	// Get the input needed to process the file...
    	try {
    	    PropList parameters = PropList.parse ( Prop.SET_FROM_PERSISTENT, tokens.get(1), routine, "," );
    		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
    		parameters.set ( "Alias", Alias );
    		parameters.setHowSet ( Prop.SET_UNKNOWN );
    		setCommandParameters ( parameters );
    	}
    	catch ( Exception e ) {
    		message = "Syntax error in \"" + command + "\".  Not enough tokens.";
    		Message.printWarning ( warning_level, routine, message);
    		throw new InvalidCommandSyntaxException ( message );
    	}
    }
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
	int log_level = 3;	// Non-user warning level

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters ();
	CommandProcessor processor = getCommandProcessor();
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
		status.clearLog(commandPhase);
	}
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList(null);
    }

	String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
	String Statistic = parameters.getValue ( "Statistic" );
	TSStatisticType statisticType = TSStatisticType.valueOfIgnoreCase(Statistic);
	String Value1 = parameters.getValue ( "Value1" );
    Double value1 = null;
    if ( (Value1 != null) && !Value1.isEmpty() ) {
        value1 = new Double(Value1);
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
	String AnalysisStart = parameters.getValue ( "AnalysisStart" ); // Default will be TS period
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" ); // Default will be TS period
	String NewTSID = parameters.getValue ( "NewTSID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (NewTSID != null) && (NewTSID.indexOf("${") >= 0) ) {
		NewTSID = TSCommandProcessorUtil.expandParameterValue(processor, this, NewTSID);
	}
	String Alias = parameters.getValue ( "Alias" );
	String Description = parameters.getValue ( "Description" ); // Expand using first time series when read below
    String OutputStart = parameters.getValue ( "OutputStart" );
	if ( (OutputStart == null) || OutputStart.isEmpty() ) {
		OutputStart = "${OutputStart}"; // Default global property
	}
    String OutputEnd = parameters.getValue ( "OutputEnd" );
	if ( (OutputEnd == null) || OutputEnd.isEmpty() ) {
		OutputEnd = "${OutputEnd}"; // Default global property
	}

	// Figure out the dates to use for the analysis...

	DateTime AnalysisStart_DateTime = null;
	DateTime AnalysisEnd_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			AnalysisStart_DateTime = TSCommandProcessorUtil.getDateTime ( AnalysisStart, "AnalysisStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			AnalysisEnd_DateTime = TSCommandProcessorUtil.getDateTime ( AnalysisEnd, "AnalysisEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
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

	// Get the time series to process.  The time series list is searched backwards until the first match...
    
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
    	PropList request_params = new PropList ( "" );
    	request_params.set ( "CommandTag", command_tag );
    	request_params.set ( "EnsembleID", EnsembleID );
    	CommandProcessorRequestResultsBean bean = null;
    	try {
            bean = processor.processRequest( "GetEnsemble", request_params);
    	}
    	catch ( Exception e ) {
    		message = "Error requesting GetEnsemble(EnsembleID=\"" + EnsembleID + "\") from processor.";
    		Message.printWarning(log_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
    		status.addToLog ( commandPhase,
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
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
    	}
    	else {
    		tsensemble = (TSEnsemble)o_TSEnsemble;
    	}
    }
	
	if ( tsensemble == null ) {
		message = "Unable to find ensemble to analyze using EnsembleID \"" + EnsembleID + "\".";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( commandPhase,
		new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
		throw new CommandWarningException ( message );
	}

	// Now process the time series...

	TS stats_ts = null;
	try {
	    boolean createData = false; // Whether to actually process the numbers - false for discovery mode
	    if ( commandPhase == CommandPhaseType.RUN ) {
	        createData = true;
    	    if ( tsensemble.size() == 0 ) {
    	        // Generate a warning to help users know that no data are available as input but create the
    	        // statistic time series as all missing
    	        message = "Ensemble \"" + EnsembleID + "\" has no time series to process for statistic.";
    	        Message.printWarning ( warning_level,
    	        MessageUtil.formatMessageTag(
    	        command_tag,++warning_count), routine, message );
    	        status.addToLog ( commandPhase,
    	        new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify the ensemble identifier and confirm that data are available." ) );
    	    }
	    }
	    String description = null;
	    if ( (commandPhase == CommandPhaseType.RUN) && (Description != null) && !Description.isEmpty() ) {
	    	TS ts = tsensemble.get(0);
	    	if ( ts != null ) {
	    		description = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
	                processor, ts, Description, status, commandPhase);
	    	}
	    }
	    else {
	    	description = Description;
	    }
	    TSUtil_NewStatisticTimeSeriesFromEnsemble tsu = new TSUtil_NewStatisticTimeSeriesFromEnsemble (
	        tsensemble, AnalysisStart_DateTime, AnalysisEnd_DateTime, OutputStart_DateTime, OutputEnd_DateTime,
	        NewTSID, description, statisticType, value1, AllowMissingCount_Integer, MinimumSampleSize_Integer );
	    stats_ts = tsu.newStatisticTimeSeriesFromEnsemble ( createData );
        if ( (Alias != null) && !Alias.equals("") ) {
            String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                processor, stats_ts, Alias, status, commandPhase);
            stats_ts.setAlias ( alias );
        }
		
		if ( commandPhase == CommandPhaseType.RUN ) {
    	    // Update the data to the processor so that appropriate actions are taken...
    
    	    if ( stats_ts != null ) {
    	        warning_count += TSCommandProcessorUtil.appendTimeSeriesToResultsList(processor, this, stats_ts);
    	    }
		}
		else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Set in the discovery list
            if ( stats_ts != null ) {
                List<TS> tslist = new Vector<TS>();
                tslist.add(stats_ts);
                setDiscoveryTSList(tslist);
            }
        }
	}
	catch ( Exception e ) {
		message ="Unexpected error generating the statistic time series from ensemble \""+ EnsembleID + "\" (" + e + ").";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),routine,message );
		Message.printWarning(3,routine,e);
		status.addToLog ( commandPhase,
		new CommandLogRecord(CommandStatusType.FAILURE,
				message, "See the log file for details." ) );
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
*/
private void setDiscoveryTSList ( List<TS> discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
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
	String EnsembleID = props.getValue( "EnsembleID" );
    String Distribution = props.getValue("Distribution");
    String DistributionParameters = props.getValue("DistributionParameters");
    String ProbabilityUnits = props.getValue("ProbabilityUnits");
	String Statistic = props.getValue( "Statistic" );
    String Value1 = props.getValue( "Value1" );
	String AllowMissingCount = props.getValue( "AllowMissingCount" );
	String MinimumSampleSize = props.getValue( "MinimumSampleSize" );
	String AnalysisStart = props.getValue( "AnalysisStart" );
	String AnalysisEnd = props.getValue( "AnalysisEnd" );
	String NewTSID = props.getValue( "NewTSID" );
	String Alias = props.getValue( "Alias" );
	String Description = props.getValue( "Description" );
    String OutputStart = props.getValue( "OutputStart" );
    String OutputEnd = props.getValue( "OutputEnd" );
	StringBuffer b = new StringBuffer ();
	if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
	}
    if ( (Distribution != null) && (Distribution.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Distribution=\"" + Distribution + "\"" );
    }
    if ( (DistributionParameters != null) && (DistributionParameters.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DistributionParameters=\"" + DistributionParameters + "\"");
    }
    if ( (ProbabilityUnits != null) && (ProbabilityUnits.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ProbabilityUnits=\"" + ProbabilityUnits + "\"" );
    }
	if ( (Statistic != null) && (Statistic.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Statistic=" + Statistic );
	}
    if ( (Value1 != null) && (Value1.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Value1=" + Value1 );
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
	if ( (Description != null) && (Description.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Description=\"" + Description + "\"" );
	}
    if ( (OutputStart != null) && (OutputStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputStart=\"" + OutputStart + "\"" );
    }
    if ( (OutputEnd != null) && (OutputEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputEnd=\"" + OutputEnd + "\"" );
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
