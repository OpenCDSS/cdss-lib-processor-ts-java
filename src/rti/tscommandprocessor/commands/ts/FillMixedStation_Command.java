// FillMixedStation_Command - Implement the FillMixedStation() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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

import java.util.List;
import java.util.Vector;

import RTi.TS.TS;

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

import RTi.Util.Math.BestFitIndicatorType;
import RTi.Util.Math.DataTransformationType;
import RTi.Util.Math.NumberOfEquationsType;
import RTi.Util.Math.RegressionType;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Time.DateTime;

/**
Implement the FillMixedStation() command.
This command can run in command "batch" mode or in tool menu.
*/
public class FillMixedStation_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

// Run mode flag. 
private boolean __commandMode = true;

/**
Pointer to the __MixedStationAnalysis object.
This object is used to perform the analyze, create the fill commands and fill the dependent time series. 
*/
protected MixedStationAnalysis __MixedStationAnalysis = null;

/**
The table that is (optionally) created with statistics information.
*/
private DataTable __table = null;

/**
Possible data values for Fill parameter.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Command constructor.
*/
public FillMixedStation_Command ()
{	
	super();
	__commandMode = true;
	setCommandName ( "FillMixedStation" );
}

/**
Command constructor.
@param commandMode true for command mode, false for tool mode.
*/
public FillMixedStation_Command ( boolean commandMode )
{	
	super();
	__commandMode = commandMode;
	setCommandName ( "FillMixedStation" );
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
{   
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// Get the properties from the propList parameters.
    //String DependentTSList = parameters.getValue ( "DependentTSList" );
	//String DependentTSID = parameters.getValue ( "DependentTSID" );
	//String IndependentTSList = parameters.getValue ( "IndependentTSList");
	//String IndependentTSID = parameters.getValue ( "IndependentTSID" );
	String BestFitIndicator = parameters.getValue ( "BestFitIndicator" );
	String AnalysisMethod = parameters.getValue ( "AnalysisMethod" );
	String NumberOfEquations = parameters.getValue ( "NumberOfEquations" );
	String AnalysisMonth = parameters.getValue( "AnalysisMonth" );
	String Transformation = parameters.getValue ( "Transformation" );
	String LEZeroLogValue = parameters.getValue ( "LEZeroLogValue" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String MinimumDataCount	= parameters.getValue ( "MinimumDataCount" );
	String MinimumR = parameters.getValue ( "MinimumR" );
	String Fill = parameters.getValue( "Fill" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	String Intercept = parameters.getValue ( "Intercept" );
	String ConfidenceInterval = parameters.getValue ( "ConfidenceInterval" );
	String FillFlag = parameters.getValue ( "FillFlag" );
	
	// FIXME SAM 2010-06-10 Evaluate whether SEP_TOTAL should be an option.
	if ( (BestFitIndicator != null) && !BestFitIndicator.equals("") &&
	    !BestFitIndicator.equalsIgnoreCase(""+BestFitIndicatorType.SEP) &&
	    !BestFitIndicator.equalsIgnoreCase(""+BestFitIndicatorType.R)
	    // && !BestFitIndicator.equalsIgnoreCase(""+BestFitIndicatorType.SEP_TOTAL)
	    ) {
        message = "The best fit indicator (" + BestFitIndicator + ") is not valid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the best fit indicator as " + BestFitIndicatorType.SEP +
                " (default if blank)" +
                //BestFitIndicatorType.SEP_TOTAL +
                ", or " + BestFitIndicatorType.R) );
    }
	
    if ( (AnalysisMethod != null) && !AnalysisMethod.equals("") ) {
       List<String> analysisMethods =
           StringUtil.breakStringList(AnalysisMethod, ",", StringUtil.DELIM_SKIP_BLANKS);
       for ( int i = 0; i < analysisMethods.size(); i++ ) {
           String analysisMethod = analysisMethods.get(i);
           if ( !analysisMethod.equalsIgnoreCase(""+RegressionType.MOVE2) &&
               !analysisMethod.equalsIgnoreCase(""+RegressionType.OLS_REGRESSION) ) {
               message = "The analysis method (" + analysisMethod + ") is not valid.";
               warning += "\n" + message;
               status.addToLog ( CommandPhaseType.INITIALIZATION,
                   new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Specify the analysis method as " + RegressionType.OLS_REGRESSION +
                       " (default if blank), or " + RegressionType.MOVE2) );
           }
       }
    }
    
    if ( (NumberOfEquations != null) && !NumberOfEquations.equals("") ) {
        List<String> numberofequations =
            StringUtil.breakStringList(NumberOfEquations, ",", StringUtil.DELIM_SKIP_BLANKS);
        for ( int i = 0; i < numberofequations.size(); i++ ) {
            String numberOfEquations = numberofequations.get(i);
            if ( !numberOfEquations.equalsIgnoreCase(""+NumberOfEquationsType.ONE_EQUATION) &&
                !numberOfEquations.equalsIgnoreCase(""+NumberOfEquationsType.MONTHLY_EQUATIONS) ) {
                message = "The number of equations (" + NumberOfEquations + ") is not valid.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the transformation as " + NumberOfEquationsType.ONE_EQUATION +
                        " (default if blank), and/or " + NumberOfEquationsType.MONTHLY_EQUATIONS) );
            }
        }
    }
    
    if ( AnalysisMonth != null ) {
		if ( !StringUtil.isInteger(AnalysisMonth) ) {
            message = "The analysis month: \"" + AnalysisMonth + "\" is not an integer.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an integer 1-12 for the analysis month.") );
		}
		else if((StringUtil.atoi(AnalysisMonth) < 1) ||	(StringUtil.atoi(AnalysisMonth) > 12) ) {
            message = "The analysis month: \"" + AnalysisMonth + "\" must be in the range 1 to 12.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an integer 1-12 for the analysis month.") );
		}
	}
    
    if ( (Transformation != null) && !Transformation.equals("") ) {
        List<String> transformations =
            StringUtil.breakStringList(Transformation, ",", StringUtil.DELIM_SKIP_BLANKS);
        for ( int i = 0; i < transformations.size(); i++ ) {
            String transformation = transformations.get(i);
            if ( !transformation.equalsIgnoreCase(""+DataTransformationType.LOG) &&
                !transformation.equalsIgnoreCase(""+DataTransformationType.NONE) ) {
                message = "The transformation (" + transformation + ") is not valid.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the transformation as " + DataTransformationType.NONE +
                        " (default if blank), or " + DataTransformationType.LOG) );
            }
        }
    }
    
    if ( (LEZeroLogValue != null) && !LEZeroLogValue.equals("") ) {
    	if (!StringUtil.isDouble(LEZeroLogValue) && !LEZeroLogValue.equalsIgnoreCase("Missing")) {
    		message = "The replacement value (" + LEZeroLogValue + ") is not valid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the replacement value as a number or \"missing\"." ));
    	}
    }
	                            
	// Make sure DependentTSID is specified only when the 
	// DependentTSList=_AllMatchingTSID.
    /* FIXME SAM 2009-08-26 Probably not necessary and need to update so not so fragile if code changes.
	if ( (DependentTSList != null) && !DependentTSList.equalsIgnoreCase(""+TSListType.ALL_MATCHING_TSID) &&
	    !DependentTSList.equalsIgnoreCase(""+TSListType.FIRST_MATCHING_TSID) &&
	    !DependentTSList.equalsIgnoreCase(""+TSListType.LAST_MATCHING_TSID)) {
		if ( DependentTSID != null ) {
            message = "The dependent time series identifier should only be specified when "
                + "DependentTSList=" + _AllMatchingTSID + " is specified.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Correct how the dependent time series list is specified." ) );
		}
	}
	
	// Make sure one or more time series are selected when AllMatchingTSID is selected.
	if ( (DependentTSList != null) && DependentTSList.equalsIgnoreCase ( _AllMatchingTSID ) ) {
		if ( DependentTSID != null ) {
			List selectedV = StringUtil.breakStringList (DependentTSID, ",",StringUtil.DELIM_SKIP_BLANKS );
			if ( (selectedV == null) || (selectedV.size() == 0) ) {
                message = "The dependent time series identifier should not be empty when "
                    + "DependentTSList=" + _AllMatchingTSID + " is specified.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Correct how the dependent time series list is specified." ) );
			}
		}
		else {
            message = "The DependentTSList should not be null when DependentTSList="
                + _AllMatchingTSID + " is specified.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Correct how the dependent time series list is specified." ) );
		}
	}
	
	// Make sure IndependentTSID is specified only when the
	// IndependentTSList=_AllMatchingTSID.
	if ( (IndependentTSList != null) && !IndependentTSList.equalsIgnoreCase(_AllMatchingTSID) ) {
		if ( IndependentTSID != null ) {
            message = "The independent time series identifier should only be specified when "
                + "IndependentTSList=" + _AllMatchingTSID + " is specified.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Correct how the independent time series list is specified." ) );
		}
	}
	
	// Make sure one or more time series are selected when AllMatchingTSID is selected.
	if ( (IndependentTSList != null) && IndependentTSList.equalsIgnoreCase ( _AllMatchingTSID ) ) {
		if ( IndependentTSID != null ) {
			List selectedV = StringUtil.breakStringList (
				IndependentTSID, ",", StringUtil.DELIM_SKIP_BLANKS );
			if ( (selectedV == null) || (selectedV.size() == 0) ) {
                message = "The IndependentTSID should not be empty when IndependentTSList="
                    + _AllMatchingTSID + "\" is specified.";
				warning += "\n" + message;

			}
		}
		else { 
			warning += "\n\"Independent TS list\" should not be null when " 
				+ "\"IndependentTSList = " + _AllMatchingTSID + "\" is specified.";
		}
	}
	*/

	// Make sure AnalysisStart, if given, is a valid date
	DateTime AnalysisStartDate = null;
	if ( AnalysisStart != null && !AnalysisStart.equals("") ) {
		try {
			AnalysisStartDate = DateTime.parse( AnalysisStart );
		}
		catch ( Exception e ) {
			message = "The analysis start (" + AnalysisStart + ") is not a valid date/time.";
	        warning += "\n" + message;
	        status.addToLog ( CommandPhaseType.INITIALIZATION,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Specify the analysis start as a valid date/time." ) );
		}
	}

	// Make sure AnalysisEnd, if given, is a valid date
	DateTime AnalysisEndDate = null;
	if ( AnalysisEnd != null && !AnalysisEnd.equals("") ) {
		try {
			AnalysisEndDate = DateTime.parse( AnalysisEnd );
		}
		catch ( Exception e ) {
			message = "The analysis end (" + AnalysisEnd + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the analysis end as a valid date/time." ) );
		}
	}

	// Make sure AnalysisStart precedes AnalysisEnd
	if ( AnalysisStartDate != null && AnalysisEndDate != null ) {
		if ( ! AnalysisEndDate.greaterThanOrEqualTo(AnalysisStartDate) ) {
			message = "The analysis start ("
				+ AnalysisStart + ") should precede the analysis end (" + AnalysisEnd + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the analysis end to be after the analysis start." ) );
		}
	}
	
	// Make sure the fill parameter, if given, is valid
	if ( (Fill != null) && !Fill.equals("") ) {
        if ( !Fill.equalsIgnoreCase(_False) && !Fill.equalsIgnoreCase(_True) ) {
            message = "The Fill (" + Fill +
            ") parameter must be  " + _False + " or " + _True + " (default).";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the Fill parameter as " + _False + " or " + _True + " (default).") );
        }
    }

	// Make sure FillStart, if given, is a valid date
	DateTime FillStartDate = null;
	if ( FillStart != null && !FillStart.equals("") ) {
		try {
			FillStartDate = DateTime.parse( FillStart );
		}
		catch ( Exception e ) {
			message = "The fill start (" + FillStart + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the fill start as a valid date/time." ) );
		}
	}

	// Make sure FillEnd, if given, is a valid date
	DateTime FillEndDate = null;
	if ( FillEnd != null && !FillEnd.equals("") ) {
		try {
			FillEndDate = DateTime.parse( FillEnd );
		}
		catch ( Exception e ) {
			message = "The fill end (" + FillEnd + ") is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the fill end as a valid date/time." ) );
		}
	}
	
	// Make sure FillStart precedes FillEnd
	if ( FillStartDate != null && FillEndDate != null ) {
		if ( ! FillEndDate.greaterThanOrEqualTo(FillStartDate) ) {
			message = "The fill start ("
				+ FillStart + ") should precede the fill end (" + FillEnd + ").";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the fill end to be after the fill start." ) );
		}
	}

	// Make sure MinimumDataCount, if given, is a valid integer
	if ( (MinimumDataCount != null) && !MinimumDataCount.equals("") && !StringUtil.isInteger(MinimumDataCount)) {
		message = "Minimum data count (" + MinimumDataCount + ") is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum data count as an integer." ) );
	}
		
	// Make sure MinimumR, if given, is a valid double
	if ( (MinimumR != null) && !MinimumR.equals("") && !StringUtil.isDouble( MinimumR ) ) {
		message = "The minimum R value (" + MinimumR + ") is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum R value as a number." ) );
	}

	// Make sure Intercept, if given, is a valid integer
	if ( (Intercept != null) && !Intercept.equals("") && !Intercept.equals("0") ) { 
		message = "The intercept (" + Intercept + ") if specified must be 0.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the intercept as 0, or do not specify." ) );
	}
	
	// Make sure confidence level, if given, is a valid number
    if ( (ConfidenceInterval != null) && !ConfidenceInterval.equals("") ) {
        if ( !StringUtil.isDouble(ConfidenceInterval) ) { 
            message = "The confidence level (" + ConfidenceInterval + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the confidence interval as a percent > 0 and < 100 (e.g., 95)." ) );
        }
        else {
            double cl = Double.parseDouble(ConfidenceInterval);
            if ( (cl <= 0.0) || (cl >= 100.0) ) { 
                message = "The confidence level (" + ConfidenceInterval + ") is invalid.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the confidence interval as a percent > 0 and < 100 (e.g., 95)." ) );
            }
        }
    }
    
    // Make sure the fill flag, if given, is valid
    if ( (FillFlag != null) && !(FillFlag.equalsIgnoreCase("Auto")) && (FillFlag.length() != 1) ) {
        message = "The fill flag must be 1 character long or set to Auto.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a 1-character fill flag or Auto." ) );
    }
	
    // Check for invalid parameters...
    List<String> valid_Vector = new Vector<String>();
    valid_Vector.add ( "Arguments" );
    valid_Vector.add ( "InputFile" );
    valid_Vector.add ( "Interpreter" );
    valid_Vector.add ( "DependentTSList" );
    valid_Vector.add ( "DependentTSID" );
    valid_Vector.add ( "IndependentTSList" );
    valid_Vector.add ( "IndependentTSID" );
    valid_Vector.add ( "AnalysisMethod" );
    valid_Vector.add ( "NumberOfEquations" );
    valid_Vector.add ( "AnalysisMonth" );
    valid_Vector.add ( "Transformation" );
    valid_Vector.add ( "LEZeroLogValue" );
    valid_Vector.add ( "AnalysisStart" );
    valid_Vector.add ( "AnalysisEnd" );
    valid_Vector.add ( "MinimumDataCount" );
    valid_Vector.add ( "MinimumR" );
    valid_Vector.add ( "BestFitIndicator" );
    valid_Vector.add ( "Fill" );
    valid_Vector.add ( "FillStart" );
    valid_Vector.add ( "FillEnd" );
    valid_Vector.add ( "Intercept" );
    valid_Vector.add ( "ConfidenceInterval" );
    valid_Vector.add ( "FillFlag" );
    valid_Vector.add ( "FillFlagDesc" );
    valid_Vector.add ( "TableID" );
    valid_Vector.add ( "TableTSIDColumn" );
    valid_Vector.add ( "TableTSIDFormat" );
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
Create the commands needed to fill the dependent time series using the best fit
among the independent time series.
*/
/*protected List<String> createFillCommands ()
{	
	return __MixedStationAnalysis.createFillCommands ();
}*/

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	
	// The command will be modified if changed...
    // This should only get called when in command mode
	List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
	        (TSCommandProcessor)getCommandProcessor(), this);
	return ( new FillMixedStation_JDialog ( parent, this, tableIDChoices ) ).ok();	
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList(Class<T> c) {
	DataTable table = getDiscoveryTable();
    List<T> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector<T>();
        v.add ( (T)table );
    }
    return v;
}

/**
Get the time series to process based on command parameters.
@param processor CommandProcessor to handle data requests.
@param label for messaging, indicates whether time series list is dependent or independent.
@param TSList TSList command parameter.
@param TSID TSID command parameter.
@return a list containing the List<TS> of time series and an array of their positions in the original list (so
that the time series can be updated if necessary).
*/
private List<Object> getTimeSeriesToProcess ( CommandProcessor processor, String label, String TSList, String TSID )
{	String routine = getCommandName() + ".getTimeSeriesToProcess", message;
	int log_level = 3;
	
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try {
	    bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\" from processor.";
		Message.printWarning(log_level, routine, message );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	List<TS> tslist = null;
	if ( o_TSList == null ) {
		message = "Unable to find " + label + " time series to process using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( log_level, routine, message );
	}
	else {
		@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
	    tslist = tslist0;
		if ( tslist.size() == 0 ) {
			message = "Unable to find " + label + " time series to process using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
			Message.printWarning ( log_level, routine, message );
		}
	}
	Object o_Indices = bean_PropList.getContents ( "Indices" );
	int [] indices = null;
	if ( o_Indices == null ) {
		message = "Unable to find indices for " + label + " time series to process using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( log_level, routine, message );
	}
	else {
	    indices = (int [])o_Indices;
		if ( indices.length == 0 ) {
			message = "Unable to find indices for " + label + " time series to process using TSList=\"" +
			TSList + "\" TSID=\"" + TSID + "\".";
			Message.printWarning ( log_level, routine, message );
		}
	}
	// In any case, return the data needed by the calling code and let it further handle errors...
	List<Object> data = new Vector<Object>(2);
	data.add ( tslist );
	data.add ( indices );
	return data;
}

/**
Free memory for garbage collection.
*/
protected boolean isCommandMode ()
{
	return __commandMode;
}

// Use parent parseCommand()

/**
Run the command.
@param command_number Command number in sequence. 
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number ) 
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	runCommandInternal( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery(int commandIndex)
		throws InvalidCommandParameterException, CommandWarningException,
		CommandException {
	runCommandInternal( commandIndex, CommandPhaseType.DISCOVERY );	
}

/**
Run the command.
@param command_number Number of command in sequence (-1 if run from the Mixed Station Analysis tool).
@param commandPhase The phase the command is running in.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String mthd = "FillMixedStation_Command.runCommand", mssg = "";
	int warning_level = 2;
	String command_tag = "" + command_number;           	
	int warning_count = 0;
	
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	PropList parameters = getCommandParameters();
	
	// Get the properties from the propList parameters.
    String DependentTSList = parameters.getValue ( "DependentTSList" );
    if ( (DependentTSList == null) || DependentTSList.equals("") ) {
        DependentTSList = "" + TSListType.ALL_TS; // Default
    }
	String DependentTSID = parameters.getValue ( "DependentTSID" );
	String IndependentTSList = parameters.getValue ( "IndependentTSList" );
    if ( (IndependentTSList == null) || IndependentTSList.equals("") ) {
        IndependentTSList = "" + TSListType.ALL_TS; // Default
    }
	String IndependentTSID = parameters.getValue ( "IndependentTSID" );
	String BestFitIndicator = parameters.getValue ( "BestFitIndicator" );
    if ( (BestFitIndicator == null) || BestFitIndicator.equals("") ) {
        BestFitIndicator = "" + BestFitIndicatorType.SEP; // default
    }
    String ConfidenceInterval = parameters.getValue ( "ConfidenceInterval" );
    Double ConfidenceInterval_Double = null;
    if ( (ConfidenceInterval != null) && !ConfidenceInterval.equals("") ) {
        ConfidenceInterval_Double = Double.parseDouble(ConfidenceInterval);
    }
	String AnalysisMethod = parameters.getValue ( "AnalysisMethod" );
	if ( (AnalysisMethod == null) || AnalysisMethod.equals("") ) {
	    AnalysisMethod = "" + RegressionType.OLS_REGRESSION; // default
	}
	String NumberOfEquations = parameters.getValue ( "NumberOfEquations");
	if ( (NumberOfEquations == null) || NumberOfEquations.equals("") ) {
	    NumberOfEquations = "" + NumberOfEquationsType.ONE_EQUATION; // default
    }
	String AnalysisMonth = parameters.getValue ( "AnalysisMonth" );
	int [] analysisMonths = null;
	if ( (AnalysisMonth != null) && !AnalysisMonth.equals("") ) {
		analysisMonths = StringUtil.parseIntegerSequenceArray(AnalysisMonth, ", ", StringUtil.DELIM_SKIP_BLANKS);
	}
	String Transformation = parameters.getValue ( "Transformation" );
    if ( (Transformation == null) || Transformation.equals("") ) {
        Transformation = "" + DataTransformationType.NONE; // default
    }
    String LEZeroLogValue = parameters.getValue("LEZeroLogValue");
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	Integer MinimumDataCount_int = Integer.valueOf(10); // Default.
	String MinimumDataCount = parameters.getValue ( "MinimumDataCount" );
	if ( (MinimumDataCount != null) && !MinimumDataCount.equals("") ) {
	    MinimumDataCount_int = Integer.parseInt(MinimumDataCount);
	}
	Double MinimumR_double = null; // default
	String MinimumR = parameters.getValue ( "MinimumR" );
    if ( (MinimumR != null) && !MinimumR.equals("") ) {
        MinimumR_double = Double.parseDouble(MinimumR);
    }
    String Fill = parameters.getValue( "Fill" );
    boolean Fill_boolean;
    if ( Fill != null && !Fill.equals("") ) {
    	Fill_boolean = Boolean.parseBoolean(Fill);
    }
    else {
    	//default is true
    	Fill_boolean = true;
    }
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	Double Intercept_double = null; // Required if Intercept is not missing
	String Intercept = parameters.getValue ( "Intercept" );
	if ( (Intercept != null) && !Intercept.equals("") ) {
	    Intercept_double = Double.parseDouble(Intercept);
	}
	String FillFlag = parameters.getValue ( "FillFlag" );
	String FillFlagDesc = parameters.getValue ( "FillFlagDesc" );
	
	String TableID = parameters.getValue ( "TableID" );
    String TableTSIDColumn = parameters.getValue ( "TableTSIDColumn" );
    String TableTSIDFormat = parameters.getValue ( "TableTSIDFormat" );
    
    DataTable table = null;
    boolean newTable = false; // true if a new table had to be created
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be updated
        PropList request_params = new PropList ( "" );
        request_params.set ( "TableID", TableID );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "GetTable", request_params);
        }
        catch ( Exception e ) {
            mssg = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), mthd, mssg );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                mssg, "Report problem to software support." ) );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table == null ) {
            Message.printStatus ( 2, mthd, "Unable to find table to process using TableID=\"" + TableID +
                "\" - creating empty table." );
            // Create an empty table matching the identifier
            table = new DataTable( new Vector<TableField>() );
            table.setTableID ( TableID );
            newTable = true;
        }
        else {
            newTable = false;
            table = (DataTable)o_Table;
        }
    }
    if ( newTable ) {
        if ( commandPhase == CommandPhaseType.RUN ) {
            // Set the table in the processor (its contents will be modified below)...
            
            PropList request_params = new PropList ( "" );
            request_params.setUsingObject ( "Table", table );
            try {
                processor.processRequest( "SetTable", request_params);
            }
            catch ( Exception e ) {
                mssg = "Error requesting SetTable(Table=...) from processor.";
                Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        mthd, mssg );
                status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           mssg, "Report problem to software support." ) );
            }
        }
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create an empty table and set the ID
            table = new DataTable();
            table.setTableID ( TableID );
            setDiscoveryTable ( table );
        }
    }
    else {
    	setDiscoveryTable ( table );
    }
	
	// Get the list of dependent time series to process...
	List<Object> tsdata = getTimeSeriesToProcess( processor, "dependent", DependentTSList, DependentTSID );
	@SuppressWarnings("unchecked")
	List<TS> dependentTSList = (List<TS>)tsdata.get(0);

	// Get the list of independent time series to process...
	List<Object> tsdata2 = getTimeSeriesToProcess( processor, "independent", IndependentTSList, IndependentTSID );
	@SuppressWarnings("unchecked")
	List<TS> independentTSList = (List<TS>)tsdata2.get(0);
	
	// Only allow one FillMixedStation command in a command file.  Otherwise it is difficult to
	// track when filled data are used to compute relationships later in the data flow
	
	List<String> neededCommands = new Vector<String>();
	List<Command> fmsCommands = TSCommandProcessorUtil.getCommandsBeforeIndex(
	    processor.getCommands().size(), (TSCommandProcessor)processor,
	    neededCommands, false);
	if ( fmsCommands.size() > 1 ) {
        mssg = "Found " + neededCommands.size() +
            " FillMixedStation() commands - only one can be used in command file.";
        Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag, ++warning_count),mthd, mssg );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            mssg, "Remove all but one FillMixedStation() command." ) );
	}
	
    if ( warning_count > 0 ) {
        // Input error...
        mssg = "Insufficient data to run command.";
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE, mssg, "Check input to command." ) );
        Message.printWarning(3, mthd, mssg );
        throw new CommandException ( mssg );
    }
	
    if (commandPhase == CommandPhaseType.RUN) {
    	try {
    		// Convert parameters to necessary form for processing
    		BestFitIndicatorType bestFitIndicator = BestFitIndicatorType.valueOfIgnoreCase(BestFitIndicator);

    		List<RegressionType> analysisMethodList = new Vector<RegressionType>();
    		List<String>tokens = StringUtil.breakStringList(AnalysisMethod, ",", StringUtil.DELIM_SKIP_BLANKS);
    		for ( int i = 0; i < tokens.size(); i++ ) {
    			analysisMethodList.add ( RegressionType.valueOfIgnoreCase(tokens.get(i)));
    		}

    		List<NumberOfEquationsType> numberOfEquations = new Vector<NumberOfEquationsType>();
    		tokens = StringUtil.breakStringList(NumberOfEquations, ",", StringUtil.DELIM_SKIP_BLANKS);
    		for ( int i = 0; i < tokens.size(); i++ ) {
    			numberOfEquations.add ( NumberOfEquationsType.valueOfIgnoreCase(tokens.get(i)));
    		}
    		
    		List<DataTransformationType> transformationList = new Vector<DataTransformationType>();
    		tokens = StringUtil.breakStringList(Transformation, ",", StringUtil.DELIM_SKIP_BLANKS);
    		for ( int i = 0; i < tokens.size(); i++ ) {
    			transformationList.add ( DataTransformationType.valueOfIgnoreCase(tokens.get(i)));
    		}

    		DateTime AnalysisStart_DateTime = TSCommandProcessorUtil.getDateTime ( AnalysisStart, "AnalysisStart", processor,
    				status, warning_level, command_tag );

    		DateTime AnalysisEnd_DateTime = TSCommandProcessorUtil.getDateTime ( AnalysisEnd, "AnalysisEnd", processor,
    				status, warning_level, command_tag );

    		DateTime FillStart_DateTime = TSCommandProcessorUtil.getDateTime ( FillStart, "FillStart", processor,
    				status, warning_level, command_tag );

    		DateTime FillEnd_DateTime = TSCommandProcessorUtil.getDateTime ( FillEnd, "FillEnd", processor,
    				status, warning_level, command_tag );

    		// Instantiate/run the MixedStationAnalysis	object
    		__MixedStationAnalysis = new MixedStationAnalysis( dependentTSList, independentTSList,
    				bestFitIndicator, analysisMethodList, numberOfEquations, analysisMonths,
    				AnalysisStart_DateTime, AnalysisEnd_DateTime, FillStart_DateTime, FillEnd_DateTime,
    				transformationList, LEZeroLogValue, Intercept_double, MinimumDataCount_int,
    				MinimumR_double, ConfidenceInterval_Double, FillFlag, FillFlagDesc, getDiscoveryTable(),
    				TableTSIDColumn, TableTSIDFormat );

    		//analysis
    		__MixedStationAnalysis.analyze();

    		// Fill the time series...
    		if (Fill_boolean) {
    			__MixedStationAnalysis.fill();
    		}
    	}
    	catch ( Exception e ) {
    		mssg = "Unexpected error running Mixed Station Analysis (" + e + ").";
    		Message.printWarning ( warning_level, 
    				MessageUtil.formatMessageTag(command_tag, ++warning_count),mthd, mssg );
    		Message.printWarning ( 3, mthd, e );
    		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
    				mssg, "Check log file for details." ) );
    		throw new CommandException ( mthd );
    	}
    }
    
    if ( warning_count > 0 ) {
        mssg = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),mthd,mssg);
        throw new CommandWarningException ( mssg );
    }
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __table = table;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"DependentTSList",
		"DependentTSID",
		"IndependentTSList",
		"IndependentTSID",
		"BestFitIndicator",
		"AnalysisMethod",
		"NumberOfEquations",
		"AnalysisMonth",
		"Transformation",
		"LEZeroLogValue",
		//"IgnoreIndependentZeroes",
		"Intercept",
		"ConfidenceInterval",
		"AnalysisStart",
		"AnalysisEnd",
		"Fill",
		"FillStart",
		"FillEnd",
		"MinimumDataCount",
		"MinimumR",
		"FillFlag",
		"FillFlagDesc",
    	"TableID",
    	"TableTSIDColumn",
    	"TableTSIDFormat"
	};
	return this.toString(parameters, parameterOrder);
}

}