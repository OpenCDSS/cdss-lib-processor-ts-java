// ----------------------------------------------------------------------------
// fillMixedStation_Command - Command class.
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History:
// 2004-04-05	Luiz Teixeira, RTi	Converted from the add_JDialog class to
//					the fill MixedStation_JDialog class.
// 2005-04-11	Luiz Teixeira, RTi	Adding code to support the analysis.
// 2005-04-22	Luiz Teixeira, RTi	Clean up
// 2005-05-26	Luiz Teixeira, RTi	Copied the original class 
//					fillMixedStation_JDialog() from TSTool
//					and split the code into the new
//					fillMixedStation_JDialog() and
//					fillMixedStation_Command().
// 2007-02-16	Steven A. Malers, RTi	Update to new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// ----------------------------------------------------------------------------
package rti.tscommandprocessor.commands.ts;

import java.io.File;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
//import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

//import RTi.TS.MixedStationAnalysis;
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
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
Implement the FillMixedStation() command.
This command can run in command "batch" mode or in tool menu.
*/
public class FillMixedStation_Command extends AbstractCommand implements Command
{

// Defines used by this class and its FillMixedStation_JDialog counterpart.
public static final String _ANALYSIS_OLS = "OLSRegression";
public static final String _ANALYSIS_MOVE2 = "MOVE2";

public static final String _TRANSFORMATION_NONE = "None";
public static final String _TRANSFORMATION_LOG = "Log";

public static final String _NUM_OF_EQUATIONS_ONE_EQUATION = "OneEquation";
public static final String _NUM_OF_EQUATIONS_MONTHLY_EQUATIONS = "MonthlyEquations";

public static final String _BEST_FIT_SEP = "SEP";
public static final String _BEST_FIT_R = "R";
public static final String _BEST_FIT_SEPTOTAL = "SEPTotal";

// Run mode flag. 
private boolean __commandMode = true;

// Pointer to the __MixedStationAnalysis object.
// This object is used to perform the analyze, create the fill commands and fill
// the dependent time series.
protected MixedStationAnalysis __MixedStationAnalysis = null; 

/**
Command editor constructor.
*/
public FillMixedStation_Command ()
{	
	super();
	__commandMode = true;
	setCommandName ( "FillMixedStation" );
}

/**
fillMixedStation_Command constructor.
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
{   String routine = getClass().getName() + ".checkCommandParameters";
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
	String NumberOfEquations = parameters.getValue ( "NumberOfEquations");
	String Transformation = parameters.getValue ( "Transformation" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String MinimumDataCount	= parameters.getValue ( "MinimumDataCount" );
	String MinimumR = parameters.getValue ( "MinimumR" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	String Intercept = parameters.getValue ( "Intercept" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	
	CommandProcessor processor = getCommandProcessor();
	
	if ( (BestFitIndicator != null) && !BestFitIndicator.equals("") &&
	    !BestFitIndicator.equalsIgnoreCase(_BEST_FIT_SEP) &&
	    !BestFitIndicator.equalsIgnoreCase(_BEST_FIT_R) &&
	    !BestFitIndicator.equalsIgnoreCase(_BEST_FIT_SEPTOTAL)) {
        message = "The best fit indicator (" + BestFitIndicator + ") is not valid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the best fit indicator as " + _BEST_FIT_SEP + " (default if blank), " +
                _BEST_FIT_SEPTOTAL + ", or " + _BEST_FIT_R) );
    }
	
    if ( (AnalysisMethod != null) && !AnalysisMethod.equals("") ) {
       List<String> analysisMethods =
           StringUtil.breakStringList(AnalysisMethod, ",", StringUtil.DELIM_SKIP_BLANKS);
       for ( int i = 0; i < analysisMethods.size(); i++ ) {
           String analysisMethod = analysisMethods.get(i);
           if ( !analysisMethod.equalsIgnoreCase(_ANALYSIS_MOVE2) &&
               !analysisMethod.equalsIgnoreCase(_ANALYSIS_OLS) ) {
               message = "The analysis method (" + analysisMethod + ") is not valid.";
               warning += "\n" + message;
               status.addToLog ( CommandPhaseType.INITIALIZATION,
                   new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Specify the analysis method as " + _ANALYSIS_OLS +
                       " (default if blank), or " + _ANALYSIS_MOVE2) );
           }
       }
    }
    
    if ( (NumberOfEquations != null) && !NumberOfEquations.equals("") &&
        !NumberOfEquations.equalsIgnoreCase(_NUM_OF_EQUATIONS_MONTHLY_EQUATIONS) &&
        !NumberOfEquations.equalsIgnoreCase(_NUM_OF_EQUATIONS_ONE_EQUATION) ) {
        message = "The number of equations (" + NumberOfEquations + ") is not valid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the number of equations as " + _NUM_OF_EQUATIONS_ONE_EQUATION +
                " (default if blank), or " + _NUM_OF_EQUATIONS_MONTHLY_EQUATIONS) );
    }
    
    if ( (Transformation != null) && !Transformation.equals("") ) {
        List<String> transformations =
            StringUtil.breakStringList(Transformation, ",", StringUtil.DELIM_SKIP_BLANKS);
        for ( int i = 0; i < transformations.size(); i++ ) {
            String transformation = transformations.get(i);
            if ( !transformation.equalsIgnoreCase(_TRANSFORMATION_LOG) &&
                !transformation.equalsIgnoreCase(_TRANSFORMATION_NONE) ) {
                message = "The transformation (" + transformation + ") is not valid.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the transformation as " + _TRANSFORMATION_NONE +
                        " (default if blank), or " + _TRANSFORMATION_LOG) );
            }
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

	// Make sure FillStart, if given, is a valid date
	DateTime FillStartDate = null;
	if ( FillStartDate != null && !FillStart.equals("") ) {
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
	if ( FillEndDate != null && !FillEndDate.equals("") ) {
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

	// Make sure MinimumDataCount was given and is a valid integer
	if ( (MinimumDataCount != null) && !MinimumDataCount.equals("") && !StringUtil.isInteger(MinimumDataCount)) {
		message = "Minimum data count (" + MinimumDataCount + ") is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum data count as an integer." ) );
	}
		
	// Make sure MinimumR, if given is a valid double. If not given set to the default 0.5.
	if ( (MinimumR != null) && !MinimumR.equals("") && !StringUtil.isDouble( MinimumR ) ) {
		message = "The minimum R value (" + MinimumR + ") is not a number.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the minimum R value as a number." ) );
	}

	// Make sure Intercept, if given is a valid integer
	if ( (Intercept != null) && !Intercept.equals("") && !Intercept.equals("0") ) { 
		message = "The intercept (" + Intercept + ") if specified must be 0.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the intercept as 0, or do not specify." ) );
	} 

    if ( (OutputFile != null) && (OutputFile.length() != 0) ) {
        // Verify that the output file can be written
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
            if ( o != null ) {
                working_dir = (String)o;
            }
            Message.printStatus ( 2, routine, "WorkingDir=\"" + working_dir + "\"" );
        }
        catch ( Exception e ) {
            message = "Error requesting WorkingDir from processor (" + e + ").";
            Message.printWarning(3, routine, e);
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Software error - report the problem to support." ) );
        }

        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The output file parent directory does not exist for: \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Create the output directory." ) );
            }
            f = null;
            f2 = null;
        }
        catch ( Exception e ) {
            message = "The output file:\n" +
            "    \"" + OutputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that output file and working directory paths are compatible." ) );
        }
    }
	
    // Check for invalid parameters...
    List valid_Vector = new Vector();
    valid_Vector.add ( "Arguments" );
    valid_Vector.add ( "InputFile" );
    valid_Vector.add ( "Interpreter" );
    valid_Vector.add ( "DependentTSList" );
    valid_Vector.add ( "DependentTSID" );
    valid_Vector.add ( "IndependentTSList" );
    valid_Vector.add ( "IndependentTSID" );
    valid_Vector.add ( "AnalysisMethod" );
    valid_Vector.add ( "NumberOfEquations" );
    valid_Vector.add ( "Transformation" );
    valid_Vector.add ( "AnalysisStart" );
    valid_Vector.add ( "AnalysisEnd" );
    valid_Vector.add ( "MinimumDataCount" );
    valid_Vector.add ( "MinimumR" );
    valid_Vector.add ( "BestFitIndicator" );
    valid_Vector.add ( "FillStart" );
    valid_Vector.add ( "FillEnd" );
    valid_Vector.add ( "Intercept" );
    valid_Vector.add ( "OutputFile" );
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
protected List createFillCommands ()
{	
	return __MixedStationAnalysis.createFillCommands ();
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	
	// The command will be modified if changed...
    // This should only get called when in command mode
	return ( new FillMixedStation_JDialog ( parent, this ) ).ok();	
}

/**
Fill the dependent time series using the best fit among the independent time series.
*/
protected void fillDependents()
{
	__MixedStationAnalysis.fill();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
	__MixedStationAnalysis = null;
	
	super.finalize ();
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
	    tslist = (List)o_TSList;
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
	List data = new Vector(2);
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
@param command_number Number of command in sequence (-1 if run from the Mixed Station Analysis tool).
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String mthd = "fillMixedStation_Command.runCommand", mssg = "";
	int warning_level = 2;
	String command_tag = "" + command_number;           	
	int warning_count = 0;
	
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	PropList parameters = getCommandParameters();
	
	// Get the properties from the propList parameters.
    String DependentTSList = parameters.getValue ( "DependentTSList" );
	String DependentTSID = parameters.getValue ( "DependentTSID" );
	String IndependentTSList = parameters.getValue ( "IndependentTSList" );
	String IndependentTSID = parameters.getValue ( "IndependentTSID" );
	String AnalysisMethod = parameters.getValue ( "AnalysisMethod" );
	if ( (AnalysisMethod == null) || AnalysisMethod.equals("") ) {
	    AnalysisMethod = _ANALYSIS_OLS; // default
	}
	String NumberOfEquations = parameters.getValue ( "NumberOfEquations");
	if ( (NumberOfEquations == null) || NumberOfEquations.equals("") ) {
	    NumberOfEquations = _NUM_OF_EQUATIONS_ONE_EQUATION; // default
    }
	String Transformation = parameters.getValue ( "Transformation" );
    if ( (Transformation == null) || Transformation.equals("") ) {
        Transformation = _TRANSFORMATION_NONE; // default
    }
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	int MinimumDataCount_int = 1; // default
	String MinimumDataCount = parameters.getValue ( "MinimumDataCount" );
	if ( (MinimumDataCount != null) && !MinimumDataCount.equals("") ) {
	    MinimumDataCount_int = Integer.parseInt(MinimumDataCount);
	}
	double MinimumR_double = 0.5; // default
	String MinimumR = parameters.getValue ( "MinimumR" );
    if ( (MinimumR != null) && !MinimumR.equals("") ) {
        MinimumR_double = Double.parseDouble(MinimumR);
    }
	String BestFitIndicator = parameters.getValue ( "BestFitIndicator" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	double Intercept_double = 0.0; // Required if Intercept is not missing
	String Intercept = parameters.getValue ( "Intercept" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	
	CommandProcessor tsCP = getCommandProcessor();
	CommandProcessor processor = tsCP;
	
	// Get the list of dependent time series to process...
	List<Object> tsdata = getTimeSeriesToProcess( processor, "dependent", DependentTSList, DependentTSID );
	List<TS> dependentTSList = (List)tsdata.get(0);

	// Get the list of independent time series to process...
	List<Object> tsdata2 = getTimeSeriesToProcess( processor, "independent", IndependentTSList, IndependentTSID );
	List<TS> independentTSList = (List)tsdata2.get(0);
	
    if ( warning_count > 0 ) {
        // Input error...
        mssg = "Insufficient data to run command.";
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE, mssg, "Check input to command." ) );
        Message.printWarning(3, mthd, mssg );
        throw new CommandException ( mssg );
    }
	
    try {
    	// Set the properties for the method ZZZZZ()!
    	PropList analysisProperties = new PropList ( "MixedStationAnalysis" );
    	
    	analysisProperties.set ( "AnalysisMethod", AnalysisMethod );
    	analysisProperties.set ( "NumberOfEquations", NumberOfEquations );
    	analysisProperties.set ( "Transformation", Transformation );
    	analysisProperties.set ( "MinimumDataCount", MinimumDataCount );
    	
    	// Do not set these properties if they are "" (empty).
    	// MixedStationAnalysis expects "null" when calling getValue()
    	// for these properties to set the internal defaults.
    	if ( AnalysisStart != null && AnalysisStart.length() > 0  ) {
    		// TODO [LT 2005-04-26] Here I am setting the properties as
    		// they are known by the different objects ( FillRegression, 
    		// TSRegression and FillMOVE2
    		analysisProperties.set (  // FillRegression
    			"AnalysisStart", AnalysisStart );
    		analysisProperties.set (  // TSRegression Until SAM remove the period.
    			"DependentAnalysisPeriodStart", AnalysisStart );
    		analysisProperties.set (  // FillMOVE2
    			"DependentAnalysisStart", AnalysisStart );
    		// TODO [LT 2005-05-27] Looks like, according to SAM's email
    		// the final name for this property will be 
    		// DependentAnalysisStart.
    		// Make sure to delete the other options after all is changed. 			
    	}
    	
    	if ( AnalysisEnd != null && AnalysisEnd.length() > 0  ) {
    		// TODO [LT 2005-04-26] Here I am setting the properties as
    		// they are known by the different objects (FillRegression, 
    		// TSRegression and FillMOVE2
    		analysisProperties.set (  // FillRegression 
    			"AnalysisEnd", AnalysisEnd );
    		analysisProperties.set (  // TSRegression
    			"DependentAnalysisPeriodEnd", AnalysisEnd );
    		analysisProperties.set (  // FillMOVE2
    			"DependentAnalysisEnd", AnalysisEnd );
    		// TODO [LT 2005-05-27] Looks like, according to SAM's email
    		// the final name for this property will be 
    		// IndependentAnalysisStart.
    		// Make sure to delete the other options after all is changed. 		
    	}
    	
    	if ( MinimumR != null && MinimumR.length() > 0  ) {
    		analysisProperties.set ( "MinimumR", MinimumR );
    	} else {
    		analysisProperties.set ( "MinimumR", "0.5" );
    	}
    	analysisProperties.set ( "BestFitIndicator", BestFitIndicator );
    	
    	if ( FillStart != null && FillStart.length() > 0  ) {
    		analysisProperties.set (	// FillRegression and FillMOVE2
    			"FillStart", FillStart );
    		analysisProperties.set (	// TSRegression
    			"FillPeriodStart", FillStart );
    	}
    	
    	if ( FillEnd != null && FillEnd.length() > 0  ) {
    		analysisProperties.set (	// FillRegression and FillMOVE2
    			"FillEnd", FillEnd );
    		analysisProperties.set (	// TSRegression
    			"FillPeriodEnd", FillEnd );
    	}
    	
    	if ( Intercept != null && Intercept.length() > 0  ) {
    		analysisProperties.set ( "Intercept", Intercept );
    	}
    	
    	if ( OutputFile != null && OutputFile.length() > 0  ) {
    		analysisProperties.set ( "OutputFile", OutputFile );
    	}

		// Instantiate/run the MixedStationAnalysis	object
		__MixedStationAnalysis = new MixedStationAnalysis( dependentTSList, independentTSList, analysisProperties );
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
    
    if ( warning_count > 0 ) {
        mssg = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),mthd,mssg);
        throw new CommandWarningException ( mssg );
    }
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}
	
	// Get the properties
    String DependentTSList = props.getValue ( "DependentTSList" );
	String DependentTSID = props.getValue ( "DependentTSID" );
	String IndependentTSList = props.getValue ( "IndependentTSList" );
	String IndependentTSID = props.getValue ( "IndependentTSID" );
	String AnalysisMethod = props.getValue ( "AnalysisMethod" );
	String NumberOfEquations = props.getValue ( "NumberOfEquations" );
	String Transformation = props.getValue ( "Transformation" );
	String AnalysisStart = props.getValue ( "AnalysisStart" );
	String AnalysisEnd = props.getValue ( "AnalysisEnd" );
	String MinimumDataCount = props.getValue ( "MinimumDataCount" );
	String MinimumR = props.getValue ( "MinimumR" );
	String BestFitIndicator = props.getValue ( "BestFitIndicator" );
	String FillStart = props.getValue ( "FillStart" );
	String FillEnd = props.getValue ( "FillEnd" );
	String Intercept = props.getValue ( "Intercept" );
	String OutputFile = props.getValue ( "OutputFile" );

	StringBuffer b = new StringBuffer();

	if ( DependentTSList != null && DependentTSList.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "DependentTSList=" + DependentTSList );
	}

	if ( DependentTSID != null && DependentTSID.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "DependentTSID=\"" + DependentTSID + "\"" );
	}

	if ( IndependentTSList != null && IndependentTSList.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "IndependentTSList=" + IndependentTSList );
	}

	if ( IndependentTSID != null && IndependentTSID.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ("IndependentTSID=\"" + IndependentTSID + "\"" );
	}
	
    if ( BestFitIndicator!= null && BestFitIndicator.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "BestFitIndicator=" + BestFitIndicator );
    }
 
	if ( AnalysisMethod != null && AnalysisMethod.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "AnalysisMethod=\"" + AnalysisMethod + "\"");
	}

	if ( NumberOfEquations != null && NumberOfEquations.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "NumberOfEquations=" + NumberOfEquations );
	}

	if ( Transformation != null && Transformation.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "Transformation=\"" + Transformation + "\"");
	}
	
    if ( Intercept != null && Intercept.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "Intercept=" + Intercept );
    }

	if ( AnalysisStart != null && AnalysisStart.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "AnalysisStart=\"" + AnalysisStart + "\"" );
	}

	if ( AnalysisEnd != null && AnalysisEnd.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "AnalysisEnd=\"" + AnalysisEnd + "\"");
	}
	
    if ( FillStart != null && FillStart.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "FillStart=\"" + FillStart + "\"");
    }

    if ( FillEnd != null && FillEnd.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "FillEnd=\"" + FillEnd + "\"");
    }

	if ( MinimumDataCount != null && MinimumDataCount.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "MinimumDataCount=" + MinimumDataCount);
	}

	if ( MinimumR != null && MinimumR.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "MinimumR="+ MinimumR );
	}

	if ( OutputFile != null && OutputFile.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}

	return getCommandName() + "(" + b.toString() + ")";
}

}