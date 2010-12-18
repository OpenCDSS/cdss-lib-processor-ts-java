package rti.tscommandprocessor.commands.ts;

import java.io.File;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.List;
import java.util.Vector;

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
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;

import RTi.Util.Math.BestFitIndicatorType;
import RTi.Util.Math.DataTransformationType;
import RTi.Util.Math.NumberOfEquationsType;
import RTi.Util.Math.RegressionType;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
Implement the FillMixedStation() command.
This command can run in command "batch" mode or in tool menu.
*/
public class FillMixedStation_Command extends AbstractCommand implements Command, FileGenerator
{

// Run mode flag. 
private boolean __commandMode = true;

// Pointer to the __MixedStationAnalysis object.
// This object is used to perform the analyze, create the fill commands and fill
// the dependent time series.
protected MixedStationAnalysis __MixedStationAnalysis = null;

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

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
	String ConfidenceInterval = parameters.getValue ( "ConfidenceInterval" );
	String FillFlag = parameters.getValue ( "FillFlag" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	
	CommandProcessor processor = getCommandProcessor();
	
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
    
    if ( (NumberOfEquations != null) && !NumberOfEquations.equals("") &&
        !NumberOfEquations.equalsIgnoreCase(""+NumberOfEquationsType.MONTHLY_EQUATIONS) &&
        !NumberOfEquations.equalsIgnoreCase(""+NumberOfEquationsType.ONE_EQUATION) ) {
        message = "The number of equations (" + NumberOfEquations + ") is not valid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the number of equations as " + NumberOfEquationsType.ONE_EQUATION +
                " (default if blank), or " + NumberOfEquationsType.MONTHLY_EQUATIONS) );
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
	
	// Make sure confidence level, if given is a valid number
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
    
    if ( (FillFlag != null) && !(FillFlag.equalsIgnoreCase("Auto")) && (FillFlag.length() != 1) ) {
        message = "The fill flag must be 1 character long or set to Auto.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a 1-character fill flag or Auto." ) );
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
    valid_Vector.add ( "ConfidenceInterval" );
    valid_Vector.add ( "FillFlag" );
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
protected List<String> createFillCommands ()
{	
	return __MixedStationAnalysis.createFillCommands ();
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
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
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
    List<File> list = new Vector();
    if ( getOutputFile() != null ) {
        list.add ( getOutputFile() );
    }
    return list;
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
    return __OutputFile_File;
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
	String Transformation = parameters.getValue ( "Transformation" );
    if ( (Transformation == null) || Transformation.equals("") ) {
        Transformation = "" + DataTransformationType.NONE; // default
    }
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	Integer MinimumDataCount_int = new Integer(10); // default
	String MinimumDataCount = parameters.getValue ( "MinimumDataCount" );
	if ( (MinimumDataCount != null) && !MinimumDataCount.equals("") ) {
	    MinimumDataCount_int = Integer.parseInt(MinimumDataCount);
	}
	double MinimumR_double = 0.5; // default
	String MinimumR = parameters.getValue ( "MinimumR" );
    if ( (MinimumR != null) && !MinimumR.equals("") ) {
        MinimumR_double = Double.parseDouble(MinimumR);
    }
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	Double Intercept_double = null; // Required if Intercept is not missing
	String Intercept = parameters.getValue ( "Intercept" );
	if ( (Intercept != null) && !Intercept.equals("") ) {
	    Intercept_double = Double.parseDouble(Intercept);
	}
	String FillFlag = parameters.getValue ( "FillFlag" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	
	CommandProcessor processor = getCommandProcessor();
	
	// Get the list of dependent time series to process...
	List<Object> tsdata = getTimeSeriesToProcess( processor, "dependent", DependentTSList, DependentTSID );
	List<TS> dependentTSList = (List)tsdata.get(0);

	// Get the list of independent time series to process...
	List<Object> tsdata2 = getTimeSeriesToProcess( processor, "independent", IndependentTSList, IndependentTSID );
	List<TS> independentTSList = (List)tsdata2.get(0);
	
	// Only allow one FillMixedStation command in a command file.  Otherwise it is difficult to
	// track when filled data are used to compute relationships later in the data flow
	
	List<String> neededCommands = new Vector();
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
	
    try {
        // Clear the output file
        setOutputFile ( null );
    	// Convert parameters to necessary form for processing
        BestFitIndicatorType bestFitIndicator = BestFitIndicatorType.valueOfIgnoreCase(BestFitIndicator);
        
        List<RegressionType> analysisMethodList = new Vector();
        List<String>tokens = StringUtil.breakStringList(AnalysisMethod, ",", StringUtil.DELIM_SKIP_BLANKS);
        for ( int i = 0; i < tokens.size(); i++ ) {
            analysisMethodList.add ( RegressionType.valueOfIgnoreCase(tokens.get(i)));
        }
        
        NumberOfEquationsType numberOfEquations = NumberOfEquationsType.valueOfIgnoreCase(NumberOfEquations);
        
        List<DataTransformationType> transformationList = new Vector();
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

    	File outputFileFull = null;
    	if ( OutputFile != null && OutputFile.length() > 0  ) {
    	    outputFileFull = new File(IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                    TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile))));
    	}
    	
        List<String> outputCommentsList = null;
        try {
            Object o = processor.getPropContents ( "OutputComments" );
            // Comments are available so use them...
            if ( o != null ) {
                outputCommentsList = (List)o;
            }
        }
        catch ( Exception e ) {
            // Not fatal, but of use to developers.
            mssg = "Error requesting OutputComments from processor - not using.";
            Message.printDebug(10, mthd, mssg );
        }

		// Instantiate/run the MixedStationAnalysis	object
		__MixedStationAnalysis = new MixedStationAnalysis( dependentTSList, independentTSList,
		    bestFitIndicator, analysisMethodList, numberOfEquations,
		    AnalysisStart_DateTime, AnalysisEnd_DateTime, FillStart_DateTime, FillEnd_DateTime,
		    transformationList, Intercept_double, MinimumDataCount_int, MinimumR_double, ConfidenceInterval_Double,
		    FillFlag );
		
		__MixedStationAnalysis.analyzeAndRank();
		
		Integer maxResultsPerIndependent = null;
		if ( outputFileFull != null ) {
		    // Print the results report
		    __MixedStationAnalysis.createReport ( outputFileFull, outputCommentsList, maxResultsPerIndependent );
            // Save the output file name...
            setOutputFile ( outputFileFull );
		}
		
		// Fill the time series...
		
		__MixedStationAnalysis.fill();
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
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file )
{
    __OutputFile_File = file;
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
	String ConfidenceInterval = props.getValue ( "ConfidenceInterval" );
	String FillFlag = props.getValue( "FillFlag" );
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
    
    if ( ConfidenceInterval != null && ConfidenceInterval.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "ConfidenceInterval=" + ConfidenceInterval );
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
	
    if ( (FillFlag != null) && (FillFlag.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FillFlag=\"" + FillFlag + "\"" );
    }

	if ( OutputFile != null && OutputFile.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}

	return getCommandName() + "(" + b.toString() + ")";
}

}