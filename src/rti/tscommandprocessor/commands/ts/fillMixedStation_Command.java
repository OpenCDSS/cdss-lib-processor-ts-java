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
import java.util.Vector;

//import RTi.TS.MixedStationAnalysis;
import RTi.TS.TS;
import RTi.TS.TSUtil;

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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;


/**
Implment the FillMixedStation() command.
This command can run in command "batch" mode or in tool menu.
*/
public class fillMixedStation_Command extends AbstractCommand implements Command
{

// Defines used by this class and its fillMixedStation_JDialog counterpart.
// Use with DependentTSList and IndependentTSList
protected final String _SelectedTS      = "SelectedTS";
protected final String _AllMatchingTSID = "AllMatchingTSID";
protected final String _AllTS           = "AllTS";

protected final String _ANALYSIS_OLS    = "OLSRegression";
protected final String _ANALYSIS_MOVE2  = "MOVE2";

protected final String _TRANSFORMATION_NONE = "None";
protected final String _TRANSFORMATION_LOG  = "Log";

protected final String _NUM_OF_EQUATIONS_ONE_EQUATION      = "OneEquation";
protected final String _NUM_OF_EQUATIONS_MONTHLY_EQUATIONS = "MonthlyEquations";

protected final String _BEST_FIT_SEP      = "SEP";
protected final String _BEST_FIT_R        = "R";
protected final String _BEST_FIT_SEPTOTAL = "SEPTotal";

// Run mode flag. 
private boolean __commandMode = true;

// Pointer to the __MixedStationAnalysis object.
// This object is used to perform the analyze, create the fill commands and fill
// the dependent time series.
protected MixedStationAnalysis __MixedStationAnalysis = null; 

/**
fillMixedStation_Command constructor.
*/
public fillMixedStation_Command ()
{	
	super();
	__commandMode = true;
	setCommandName ( "FillMixedStation" );
}

/**
fillMixedStation_Command constructor.
@param commandMode - Indicating the running mode: true for command mode, false
for tool mode.
*/
public fillMixedStation_Command ( boolean runMode )
{	
	super();

	__commandMode = runMode;
	setCommandName ( "FillMixedStation" );
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
{
	String routine = getCommandName() + ".checkCommandParameters";
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// Get the properties from the propList parameters.
    String DependentTSList  = parameters.getValue ( "DependentTSList"  );
	String DependentTSID    = parameters.getValue ( "DependentTSID"    );
	String IndependentTSList= parameters.getValue ( "IndependentTSList");
	String IndependentTSID  = parameters.getValue ( "IndependentTSID"  );
	// TODO SAM 2007-02-16 Need to add checks for all input parameters
	//String AnalysisMethod	= parameters.getValue ( "AnalysisMethod"   );
	//String NumberOfEquations= parameters.getValue ( "NumberOfEquations");
	//String Transformation	= parameters.getValue ( "Transformation"   );
	String AnalysisStart	= parameters.getValue ( "AnalysisStart"    );
	String AnalysisEnd	= parameters.getValue ( "AnalysisEnd"      );
	String MinimumDataCount	= parameters.getValue ( "MinimumDataCount" );
	String MinimumR		= parameters.getValue ( "MinimumR"         );
	//String BestFitIndicator = parameters.getValue ( "BestFitIndicator" );
	String FillStart 	= parameters.getValue ( "FillStart"        );
	String FillEnd 		= parameters.getValue ( "FillEnd"          );
	String Intercept	= parameters.getValue ( "Intercept"        );
	String OutputFile	= parameters.getValue ( "OutputFile"       );
	
	CommandProcessor processor = getCommandProcessor();
	                            
	// Get the working_dir from the command processor
	String working_dir = null;
	try { Object o = processor.getPropContents ( "WorkingDir" );
		// Working directory is available so use it...
		if ( o != null ) {
			working_dir = (String)o;
		}
	}
	catch ( Exception e ) {
        message = "Error requesting WorkingDir from processor.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Software error - report the problem to support." ) );
	}
		
	// Make sure DependentTSID is specified only when the 
	// DependentTSList=_AllMatchingTSID.
	if ( (DependentTSList != null) && !DependentTSList.equalsIgnoreCase(_AllMatchingTSID) ) {
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
	if ( DependentTSList.equalsIgnoreCase ( _AllMatchingTSID ) ) {
		if ( DependentTSID != null ) {
			Vector selectedV = StringUtil.breakStringList (
				DependentTSID, ",",
				StringUtil.DELIM_SKIP_BLANKS );
			if ( (selectedV == null) || (selectedV.size() == 0) ) {
                message = "The dependent time series identifier should not be empty when "
                    + "DependentTSList=" + _AllMatchingTSID + " is specified.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Correct how the dependent time series list is specified." ) );
			}
		} else {
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
	if ( IndependentTSList.equalsIgnoreCase ( _AllMatchingTSID ) ) {
		if ( IndependentTSID != null ) {
			Vector selectedV = StringUtil.breakStringList (
				IndependentTSID, ",",
				StringUtil.DELIM_SKIP_BLANKS );
			if ( (selectedV == null) || (selectedV.size() == 0) ) {
                message = "The IndependentTSID should not be empty when IndependentTSList="
                    + _AllMatchingTSID 
                    + "\" is specified.";
				warning += "\n" + message;

			}
		} else { 
			warning += "\n\"Independent TS list\" "
				+ "should not be null when " 
				+ "\"IndependentTSList = "
				+ _AllMatchingTSID 
				+ "\" is specified.";
		}
	}

	// Make sure AnalysisStart, if given, is a valid date
	DateTime AnalysisStartDate = null;
	if ( AnalysisStart != null && !AnalysisStart.equals("") ) {
		try {
			AnalysisStartDate = DateTime.parse( AnalysisStart );
		} catch ( Exception e ) {
			warning += "\n Analysis Start period \""
				+ AnalysisStart
				+ "\" is not a valid date.";
		}
	}

	// Make sure AnalysisEnd, if given, is a valid date
	DateTime AnalysisEndDate = null;
	if ( AnalysisEnd != null && !AnalysisEnd.equals("") ) {
		try {
			AnalysisEndDate = DateTime.parse( AnalysisEnd );
		} catch ( Exception e ) {
			warning += "\n Analysis End Period \""
				+ AnalysisEnd
				+ "\" is not a valid date.";
		}
	}

	// Make sure AnalysisStart preceeds AnalysisEnd
	if ( AnalysisStartDate != null && AnalysisEndDate != null ) {
		if ( ! AnalysisEndDate.greaterThanOrEqualTo(AnalysisStartDate) ) {
			warning += "\n Analysis Start \""
				+ AnalysisStart
				+ "\" should proceed Analysis End \""
				+ AnalysisEnd + "\".";
		}
	}

	// Make sure FillStart, if given, is a valid date
	DateTime FillStartDate = null;
	if ( FillStartDate != null && !FillStart.equals("") ) {
		try {
			FillStartDate = DateTime.parse( FillStart );
		} catch ( Exception e ) {
			warning += "\n Fill Start \""
				+ FillStart
				+ "\" is not a valid date.";
		}
	}

	// Make sure FillEnd, if given, is a valid date
	DateTime FillEndDate = null;
	if ( FillEndDate != null && !FillEndDate.equals("") ) {
		try {
			FillEndDate = DateTime.parse( FillEnd );
		} catch ( Exception e ) {
			warning += "\n Fill End \""
				+ FillEnd
				+ "\" is not a valid date.";
		}
	}

	// Make sure AnalysisStart preceeds FillStart
	if ( AnalysisStartDate != null && FillStartDate != null ) {
		if ( ! FillStartDate.greaterThanOrEqualTo( AnalysisStartDate ) ) {
			warning += "\n Analysis Start \""
				+ AnalysisStart
				+ "\" should proceed Fill Start \""
				+ FillStart
				+ "\".";
		}
	}
	
	// Make sure FillEnd preceeds the AnalysisEnd
	if ( AnalysisEndDate != null && FillEndDate != null ) {
		if ( ! AnalysisEndDate.greaterThanOrEqualTo( FillEndDate ) ) {
			warning += "\n Fill End \""
				+ FillEnd
				+ "\" should proceed Analysis End \""
				+ AnalysisEnd
				+ "\".";
		}
	}

	// Make sure MinimumDataCount was given and is a valid integer
	if ( MinimumDataCount == null || MinimumDataCount.equals("") ) {
		warning +=
			"\n Minimum Data Count must be specified.";
	} else {
		if ( !StringUtil.isInteger(MinimumDataCount) ) {
		warning += "\n Minimum Data Count \""
			+ MinimumDataCount
			+ "\" is not an integer.";
		}
	}
		
	// Make sure MinimumR, if given is a valid double. If not given set
	// to the default 0.5.
	if ( MinimumR != null && !MinimumR.equals("") ) {
		if ( !StringUtil.isDouble( MinimumR ) ) {
			warning += "\n Minimum R \""
				+ MinimumR
				+ "\" is not a number.";
		}
	}

	// Make sure Intercept, if given is a valid integer
	if ( Intercept != null && !Intercept.equals("") ) {
		if ( !StringUtil.isInteger(Intercept) ) { 
			warning += "\n Intercept \""
				+ Intercept
				+ "\" is not an integer.";
		}
		if ( StringUtil.atoi(Intercept) != 0 ) {
			warning += "\n Intercept \""
				+ Intercept
				+ "\" is not 0.";	
		}		
	} 

	// Output file
	if ( OutputFile != null && OutputFile.length() != 0 ) {
		try {
			String adjusted_path = IOUtil.adjustPath (
				 working_dir, OutputFile);
			File f  = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				warning += "\nThe file parent directory "
					+ " does not exist:\n"
					+ adjusted_path;
			}
			f  = null;
			f2 = null;
		}
		catch ( Exception e ) {
			warning += "\nThe working directory:\n"
				+ "    \"" + working_dir
				+ "\"\ncannot be adjusted using:\n"
				+ "    \"" + OutputFile;
		}
	} else {
		warning += "\nThe Output File field is empty.";
	}
	
	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, warning_level ),
			warning );
		throw new InvalidCommandParameterException ( warning );
	}	
}

/**
Create the commands needed to fill the dependent time series using the best fit
among the independent time series.
*/
protected Vector createFillCommands ()
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
	if ( isCommandMode() ) {
		return ( new fillMixedStation_JDialog ( parent, this ) ).ok();
	} else {
		return ( new fillMixedStation_JDialog ( parent, this, 0 ) ).ok();
	}	
}

/**
Fill the dependent time series using the best fit among the independent
time series.
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
Get the time series to process.
@param processor CommandProcessor to handle data requests.
@param TSList TSList command parameter.
@param TSID TSID command parameter.
*/
private Vector getTimeSeriesToProcess ( CommandProcessor processor,
		String TSList, String TSID )
{	String routine = getCommandName() + ".getTimeSeriesToProcess", message;
	int log_level = 3;
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try { bean =
		processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\" from processor.";
		Message.printWarning(log_level, routine, message );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	Vector tslist = null;
	if ( o_TSList == null ) {
		message = "Unable to find time series to process using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( log_level, routine, message );
	}
	else {	tslist = (Vector)o_TSList;
		if ( tslist.size() == 0 ) {
			message = "Unable to find time series to process using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
			Message.printWarning ( log_level, routine, message );
		}
	}
	Object o_Indices = bean_PropList.getContents ( "Indices" );
	int [] indices = null;
	if ( o_Indices == null ) {
		message = "Unable to find indices for time series to process using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( log_level, routine, message );
	}
	else {	indices = (int [])o_Indices;
		if ( indices.length == 0 ) {
			message = "Unable to find indices for time series to process using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
			Message.printWarning ( log_level, routine, message );
		}
	}
	// In any case, return the data needed by the calling code and let
	// it further handle errors...
	Vector data = new Vector(2);
	data.addElement ( tslist );
	data.addElement ( indices );
	return data;
}

/**
Free memory for garbage collection.
*/
protected boolean isCommandMode ()
{
	return __commandMode;
}

/**
Parse the command string into a PropList of parameters.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws 	InvalidCommandSyntaxException,
	InvalidCommandParameterException
{	String mthd = "fillMixedStation_Command.parseCommand", mssg;
	int warning_level = 2;

	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the command name and the InputFile
		mssg = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level, mthd, mssg);
		throw new InvalidCommandSyntaxException ( mssg );
	}

	// Get the input needed to process the file...
	try {
		setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
			(String)tokens.elementAt(1), mthd, "," ) );
	}
	catch ( Exception e ) {
		mssg = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level, mthd, mssg);
		throw new InvalidCommandSyntaxException ( mssg );
	}
}

/**
Run the command:
<pre>
fillMixedStation ( DependentTSList="...",
		   DependentTSList="X,Y,...",
		   IndependentTSList="...",
		   IndependentTSList="X,Y,...",
		   AnalysisMethod="OLSRegression",
		   NumberOfEquations="OneEquation",
		   Transformation="None",
		   AnalysisStart="...",
		   AnalysisEnd="...",
		   MinimumDataCount="..."
		   MinimumR="..."
		   BestFitIndicator="SEP",
		   FillStart="...",
		   FillEnd="...",
		   Intercept="..."
		   OutputFile="...")
</pre>
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
       CommandWarningException,
       CommandException
{	String mthd = "fillMixedStation_Command.runCommand", mssg = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int log_level = 3;	// For warnings not shown to user
            	
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
	// Get the properties from the propList parameters.
    String DependentTSList  = parameters.getValue ( "DependentTSList"  );
	String DependentTSID    = parameters.getValue ( "DependentTSID"    );
	String IndependentTSList= parameters.getValue ( "IndependentTSList");
	String IndependentTSID  = parameters.getValue ( "IndependentTSID"  );
	String AnalysisMethod	= parameters.getValue ( "AnalysisMethod"   );
	String NumberOfEquations= parameters.getValue ( "NumberOfEquations");
	String Transformation	= parameters.getValue ( "Transformation"   );
	String AnalysisStart	= parameters.getValue ( "AnalysisStart"    );
	String AnalysisEnd	= parameters.getValue ( "AnalysisEnd"      );
	String MinimumDataCount	= parameters.getValue ( "MinimumDataCount" );
	String MinimumR		= parameters.getValue ( "MinimumR"         );
	String BestFitIndicator = parameters.getValue ( "BestFitIndicator" );
	String FillStart 	= parameters.getValue ( "FillStart"        );
	String FillEnd 		= parameters.getValue ( "FillEnd"          );
	String Intercept	= parameters.getValue ( "Intercept"        );
	String OutputFile	= parameters.getValue ( "OutputFile"       );
	
	Vector v;
	int [] tspos;
	int tsCount;
	
	CommandProcessor tsCP = getCommandProcessor();
	CommandProcessor processor = tsCP;
	
	// Get the list of dependent time series to process...
	Vector dependentTSList = null; 
	if ( __commandMode ) {
		
		// Command Mode:
		// Get the time series from the command list.
		// The getTimeSeriesToProcess method will properly return the
		// time series according to the settings of DependentTSList.
		v = getTimeSeriesToProcess( processor,
			DependentTSList, DependentTSID );
		Vector tslist = (Vector)v.elementAt(0);
		tspos = (int [])v.elementAt(1);
		tsCount = tslist.size();
		if ( tsCount == 0 ) {
			mssg = "Unable to find time series using DependentTSID \""
				+ DependentTSID + "\".";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
					command_tag, ++warning_count), mthd, mssg );
		}
		dependentTSList = new Vector( tsCount );
		TS dependentTS = null;
		for ( int nTS = 0; nTS < tsCount; nTS++ ) {
			// Get the time series object.
			try {
					PropList request_params = new PropList ( "" );
					request_params.setUsingObject ( "Index", new Integer(tspos[nTS]) );
					CommandProcessorRequestResultsBean bean = null;
					try { bean =
						tsCP.processRequest( "GetTimeSeries", request_params);
					}
					catch ( Exception e ) {
						Message.printWarning(log_level,
								MessageUtil.formatMessageTag( command_tag, ++warning_count),
								mthd, "Error requesting GetTimeSeries(Index=" + tspos[nTS] +
								"\" from processor." );
					}
					PropList bean_PropList = bean.getResultsPropList();
					Object prop_contents = bean_PropList.getContents ( "TS" );
					if ( prop_contents == null ) {
						Message.printWarning(warning_level,
							MessageUtil.formatMessageTag( command_tag, ++warning_count),
							mthd, "Null value for GetTimeSeries(Index=" + tspos[nTS] +
							"\") returned from processor." );
					}
					else {	dependentTS = (TS)prop_contents;
					}
							
				dependentTSList.add ( dependentTS );	
			} catch ( Exception e ) {
				// TODO REVISIT SAM 2005-05-17 Ignore?
				continue;
			}
		}
		dependentTS = null;	
		
	} else {
		
		// Tool Mode:
		// Under the tool mode only _AllMatchingTSID can be specified,
		// so the list if DependentTSID time series should contain all
		// the time series needed for processing. Use the full list of
		// resulting time series and the DependentTSID timeseries to
		// get the list os selected time series objects.

		Vector tsObjects = null;
		try { Object o = processor.getPropContents( "TSResultsList" );
				tsObjects = (Vector)o;
		}
		catch ( Exception e ){
			String message = "Cannot get time series list to process.";
			Message.printWarning ( warning_level,
					MessageUtil.formatMessageTag(
					command_tag, ++warning_count),
					mthd,message);
		}
		
		Vector dependentTSID_Vector = StringUtil.breakStringList (
			DependentTSID, ",", StringUtil.DELIM_SKIP_BLANKS );
		dependentTSList = TSUtil.selectTimeSeries ( 
			tsObjects, dependentTSID_Vector, null );
	}
	
	// Get the list of independent time series to process...
	Vector independentTSList = null;
	if ( __commandMode ) {
		
		// Command Mode:
		// Get the time series from the command list.
		// The getTimeSeriesToProcess method will properly return the
		// time series according to the settings of IndependentTSList.
		v = getTimeSeriesToProcess( processor, IndependentTSList, IndependentTSID);
				
		Vector tslist = (Vector)v.elementAt(0);
		tspos = (int [])v.elementAt(1);
		tsCount = tslist.size();
		if ( tsCount == 0 ) {
			mssg = "Unable to find time series using IndependentTSID \""
				+ IndependentTSID + "\".";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
					command_tag, ++warning_count), mthd, mssg );
		}
		independentTSList = new Vector( tsCount );
		TS independentTS = null;
		for ( int nTS = 0; nTS < tsCount; nTS++ ) {
			// Get the time series object.
			try {	
				PropList request_params = new PropList ( "" );
				request_params.setUsingObject ( "Index", new Integer(tspos[nTS]) );
				CommandProcessorRequestResultsBean bean = null;
				try { bean =
					tsCP.processRequest( "GetTimeSeries", request_params);
				}
				catch ( Exception e ) {
					Message.printWarning(log_level,
							MessageUtil.formatMessageTag( command_tag, ++warning_count),
							mthd, "Error requesting GetTimeSeries(Index=" + tspos[nTS] +
							"\" from processor." );
				}
				PropList bean_PropList = bean.getResultsPropList();
				Object prop_contents = bean_PropList.getContents ( "TS" );
				if ( prop_contents == null ) {
					Message.printWarning(warning_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						mthd, "Null value for GetTimeSeries(Index=" + tspos[nTS] +
						"\") returned from processor." );
				}
				else {	independentTS = (TS)prop_contents;
				}
				
				independentTSList.add ( independentTS );	
			} catch ( Exception e ) {
				// TODO SAM 2005-05-17 Ignore?
				continue;
			}
		}
		independentTS = null;
		 
	} else {
		// Tool Mode:
		// Under the tool mode only _AllMatchingTSID can be specified,
		// so the list if IndependentTSID time series should contain all
		// the time series needed for processing. Use the full list of
		// resulting time series and the IndependentTSID timeseries to
		// get the list of selected time series objects.
		
		Vector tsObjects = null;
		try { Object o = processor.getPropContents( "TSResultsList" );
				tsObjects = (Vector)o;
		}
		catch ( Exception e ){
			String message = "Cannot get time series list to process.";
			Message.printWarning ( warning_level,
					MessageUtil.formatMessageTag(
					command_tag, ++warning_count),
					mthd,message);
		}
		
		Vector independentTSID_Vector = StringUtil.breakStringList (
			IndependentTSID, ",", StringUtil.DELIM_SKIP_BLANKS );
		independentTSList = TSUtil.selectTimeSeries ( 
			tsObjects, independentTSID_Vector, null );
	}
	
	// Set the properties for the method ZZZZZ()!
	PropList analysisProperties = new PropList ( "MixedStationAnalysis" );
	
	analysisProperties.set ( "AnalysisMethod",    AnalysisMethod    );
	analysisProperties.set ( "NumberOfEquations", NumberOfEquations );
	analysisProperties.set ( "Transformation",    Transformation    );
	analysisProperties.set ( "MinimumDataCount",  MinimumDataCount  );
	
	// Do not set these properties if they are "" (empty).
	// MixedStationAnalysis expects "null" when calling getValue()
	// for these properties to set the internal defaults.
	if ( AnalysisStart != null && AnalysisStart.length() > 0  ) {
		// REVISIT [LT 2005-04-26] Here I am setting the properties as
		// they are known by the different objects ( FillRegression, 
		// TSRegression and FillMOVE2
		analysisProperties.set (  // FillRegression
			"AnalysisStart", AnalysisStart );
		analysisProperties.set (  // TSRegression Until SAM remove
				          // the period.
			"DependentAnalysisPeriodStart", AnalysisStart );
		analysisProperties.set (  // FillMOVE2
			"DependentAnalysisStart", AnalysisStart );
		// REVISIT [LT 2005-05-27] Looks like, according to SAM's email
		// the final name for this property will be 
		// DependentAnalysisStart.
		// Make sure to delete the other options after all is changed. 			
	}
	
	if ( AnalysisEnd != null && AnalysisEnd.length() > 0  ) {
		// REVISIT [LT 2005-04-26] Here I am setting the properties as
		// they are known by the different objects (FillRegression, 
		// TSRegression and FillMOVE2
		analysisProperties.set (  // FillRegression 
			"AnalysisEnd", AnalysisEnd );
		analysisProperties.set (  // TSRegression
			"DependentAnalysisPeriodEnd", AnalysisEnd );
		analysisProperties.set (  // FillMOVE2
			"DependentAnalysisEnd", AnalysisEnd );
		// REVISIT [LT 2005-05-27] Looks like, according to SAM's email
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
	
	// Run the MixedStationAnalysis.
	try {
		// Instantiate the MixedStationAnalysis	object
		__MixedStationAnalysis = new MixedStationAnalysis( 
			dependentTSList, 
			independentTSList,
			analysisProperties );
	} catch ( Exception e ) {
		// REVISIT [LT 2005-04-20] Problems throwing an exception
		// from here to be catch by the calling object. This method
		// is called by actionPerformed and it is not allowing me 
		// to declare "Throws Exception". How to fix this?
		mssg = "Unexpected error performing mixed station analysis (" + e + ").";
		Message.printWarning (1, mthd, mssg );
	} 

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		mssg = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			mthd, mssg );
		throw new CommandWarningException ( mssg );
	}
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
        String DependentTSList  = props.getValue ( "DependentTSList"  );
	String DependentTSID    = props.getValue ( "DependentTSID"    );
	String IndependentTSList= props.getValue ( "IndependentTSList");
	String IndependentTSID  = props.getValue ( "IndependentTSID"  );
	String AnalysisMethod	= props.getValue ( "AnalysisMethod"   );
	String NumberOfEquations= props.getValue ( "NumberOfEquations");
	String Transformation	= props.getValue ( "Transformation"   );
	String AnalysisStart	= props.getValue ( "AnalysisStart"    );
	String AnalysisEnd	= props.getValue ( "AnalysisEnd"      );
	String MinimumDataCount	= props.getValue ( "MinimumDataCount" );
	String MinimumR		= props.getValue ( "MinimumR"         );
	String BestFitIndicator = props.getValue ( "BestFitIndicator" );
	String FillStart 	= props.getValue ( "FillStart"        );
	String FillEnd 		= props.getValue ( "FillEnd"          );
	String Intercept	= props.getValue ( "Intercept"        );
	String OutputFile	= props.getValue ( "OutputFile"       );

	// Creating the command string
	// This StringBuffer will contain all parameters for the command.
	StringBuffer b = new StringBuffer();

	// Adding the DependentTSList
	if ( DependentTSList != null && DependentTSList.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "DependentTSList=" + DependentTSList );
	}

	// Adding the DependentTSID
	if ( DependentTSID != null && DependentTSID.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "DependentTSID=\"" + DependentTSID + "\"" );
	}

	// Adding the IndependentTSList
	if ( IndependentTSList != null && IndependentTSList.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "IndependentTSList=" + IndependentTSList );
	}

	/// Adding the IndependentTSID
	if ( IndependentTSID != null && IndependentTSID.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ("IndependentTSID=\"" + IndependentTSID + "\"" );
	}

	// Adding the AnalysisMethod
	if ( AnalysisMethod != null && AnalysisMethod.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "AnalysisMethod=\"" + AnalysisMethod + "\"");
	}

	// Adding the NumberOfEquations
	if ( NumberOfEquations != null && NumberOfEquations.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "NumberOfEquations=" + NumberOfEquations );
	}

	// Adding the Transformation
	if ( Transformation != null && Transformation.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "Transformation=\"" + Transformation + "\"");
	}

	// Adding the AnalysisStart
	if ( AnalysisStart != null && AnalysisStart.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "AnalysisStart=" + AnalysisStart );
	}

	// Adding the AnalysisEnd
	if ( AnalysisEnd != null && AnalysisEnd.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "AnalysisEnd=" + AnalysisEnd );
	}

	// Adding the MinimumDataCount
	if ( MinimumDataCount != null && MinimumDataCount.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "MinimumDataCount=" + MinimumDataCount);
	}
	
	// Adding the MinimumR
	if ( MinimumR != null && MinimumR.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "MinimumR="+ MinimumR );
	}

	// Adding the BestFitIndicator
	if ( BestFitIndicator!= null && BestFitIndicator.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "BestFitIndicator=" + BestFitIndicator );
	}

	// Adding the FillStart
	if ( FillStart != null && FillStart.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "FillStart=" + FillStart );
	}

	// Adding the FillEnd
	if ( FillEnd != null && FillEnd.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "FillEnd=" + FillEnd );
	}

	// Adding the Intercept
	if ( Intercept != null && Intercept.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "Intercept=" + Intercept );
	}

	// Adding the OutputFile
	if ( OutputFile != null && OutputFile.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "OutputFile=" + OutputFile );
	}

	return getCommandName() + "(" + b.toString() + ")";
}

}