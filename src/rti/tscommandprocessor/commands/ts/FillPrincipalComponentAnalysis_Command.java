// ----------------------------------------------------------------------------
// FillPrincipalComponentAnalysis_Command - Command class.
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package rti.tscommandprocessor.commands.ts;

import java.io.File;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import java.util.List;
import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSPrincipalComponentAnalysis;
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

import RTi.Util.Math.PrincipalComponentAnalysis;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;

/**
Implement the FillPrincipalComponentAnalysis() command.
This command can run in command "batch" mode or in tool menu.
*/
public class FillPrincipalComponentAnalysis_Command extends AbstractCommand implements Command
{

// Defines used by this class and its FillPrincipalComponentAnalysis_JDialog counterpart.
// Use with DependentTSList and IndependentTSList
protected final String _SelectedTS      = "SelectedTS";
protected final String _AllMatchingTSID = "AllMatchingTSID";
protected final String _AllTS           = "AllTS";
public final int _maxCombinationsDefault = 20;

// Run mode flag. 
private boolean __commandMode = true;

// Pointer to the __PrincipalComponentAnalysis object.
// This object is used to perform the analyze, create the fill commands and fill
// the dependent time series.
protected PrincipalComponentAnalysis __PrincipalComponentAnalysis = null;

/**
Command editor constructor.
*/
public FillPrincipalComponentAnalysis_Command ()
{	
	super();
	__commandMode = true;
	setCommandName ( "FillPrincipalComponentAnalysis" );
}

/**
FillPrincipalComponentAnalysis_Command constructor.
@param commandMode - Indicating the running mode: true for command mode, false
for tool mode.
*/
public FillPrincipalComponentAnalysis_Command ( boolean runMode )
{	
	super();

	__commandMode = runMode;
	setCommandName ( "FillPrincipalComponentAnalysis" );
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
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// Get the properties from the propList parameters.
	String DependentTSList  = parameters.getValue ( "DependentTSList"  );
	String DependentTSID    = parameters.getValue ( "DependentTSID"    );
	String IndependentTSList= parameters.getValue ( "IndependentTSList");
	String IndependentTSID  = parameters.getValue ( "IndependentTSID"  );
	String AnalysisStart	= parameters.getValue ( "AnalysisStart"    );
	String AnalysisEnd	= parameters.getValue ( "AnalysisEnd"      );
	String FillStart 	= parameters.getValue ( "FillStart"        );
	String FillEnd 		= parameters.getValue ( "FillEnd"          );
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
		
	// Make sure one or more time series are selected when AllMatchingTSID is selected.
	if ( IndependentTSList.equalsIgnoreCase ( _AllMatchingTSID ) ) {
		if ( IndependentTSID != null ) {
			List selectedV = StringUtil.breakStringList (
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
	if ( FillEnd != null && !FillEnd.equals("") ) {
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
protected List createFillCommands ()
{	
	return null; // __PrincipalComponentAnalysis.createFillCommands ();
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
		return ( new FillMixedStation_JDialog ( parent, this ) ).ok();
	} else {
		return ( new FillMixedStation_JDialog ( parent, this, 0 ) ).ok();
	}	
}

/**
Fill the dependent time series using the best fit among the independent
time series.
*/
protected void fillDependents()
{
	// __PrincipalComponentAnalysis.fill();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
	__PrincipalComponentAnalysis = null;
	
	super.finalize ();
}

/**
Get the time series to process.
@param processor CommandProcessor to handle data requests.
@param TSList TSList command parameter.
@param TSID TSID command parameter.
*/
private List getTimeSeriesToProcess ( CommandProcessor processor,
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
	List tslist = null;
	if ( o_TSList == null ) {
		message = "Unable to find time series to process using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( log_level, routine, message );
	}
	else {	tslist = (List)o_TSList;
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

	List tokens = StringUtil.breakStringList ( command,
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
			(String)tokens.get(1), mthd, "," ) );
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
	String AnalysisStart	= parameters.getValue ( "AnalysisStart"    );
	String AnalysisEnd	= parameters.getValue ( "AnalysisEnd"      );
	String FillStart 	= parameters.getValue ( "FillStart"        );
	String FillEnd 		= parameters.getValue ( "FillEnd"          );
	String OutputFile	= parameters.getValue ( "OutputFile"       );
    String MaxCombinations = parameters.getValue ( "MaxCombinations" );
    String AnalysisMonths = parameters.getValue("AnalysisMonths");
    DateTime AnalysisStartDateTime = null, AnalysisEndDateTime = null;
	
	List v;
	int [] tspos;
	int tsCount;
	
	CommandProcessor tsCP = getCommandProcessor();
	CommandProcessor processor = tsCP;
    PrintWriter out = null;
	
	// Get the list of dependent time series to process...
    TS dependentTS = null;
	List dependentTSList = null; 
	if ( __commandMode ) {
		
		// Command Mode:
		// Get the time series from the command list.
		// The getTimeSeriesToProcess method will properly return the
		// time series according to the settings of DependentTSList.
        // There should be only 1 dependent time series.
		v = getTimeSeriesToProcess( processor,
			DependentTSList, DependentTSID );
		List tslist = (List)v.get(0);
		tspos = (int [])v.get(1);
		tsCount = tslist.size();
		if ( tsCount == 0 ) {
			mssg = "Unable to find time series using DependentTSID \""
				+ DependentTSID + "\".";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
					command_tag, ++warning_count), mthd, mssg );
		}
		dependentTSList = new Vector( tsCount );
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

		List tsObjects = null;
		try { Object o = processor.getPropContents( "TSResultsList" );
				tsObjects = (List)o;
		}
		catch ( Exception e ){
			String message = "Cannot get time series list to process.";
			Message.printWarning ( warning_level,
					MessageUtil.formatMessageTag(
					command_tag, ++warning_count),
					mthd,message);
		}
		
		List dependentTSID_Vector = StringUtil.breakStringList (
			DependentTSID, ",", StringUtil.DELIM_SKIP_BLANKS );
		dependentTSList = TSUtil.selectTimeSeries ( 
			tsObjects, dependentTSID_Vector, null );
        if ( dependentTSList.size() > 0 )
            dependentTS = (TS) dependentTSList.get(0);
	}
	
	// Get the list of independent time series to process...
	List independentTSList = null;
	if ( __commandMode ) {
		
		// Command Mode:
		// Get the time series from the command list.
		// The getTimeSeriesToProcess method will properly return the
		// time series according to the settings of IndependentTSList.
		v = getTimeSeriesToProcess( processor, IndependentTSList, IndependentTSID);
				
		List tslist = (List)v.get(0);
		tspos = (int [])v.get(1);
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
		
		List tsObjects = null;
		try { Object o = processor.getPropContents( "TSResultsList" );
				tsObjects = (List)o;
		}
		catch ( Exception e ){
			String message = "Cannot get time series list to process.";
			Message.printWarning ( warning_level,
					MessageUtil.formatMessageTag(
					command_tag, ++warning_count),
					mthd,message);
		}
		
		List independentTSID_Vector = StringUtil.breakStringList (
			IndependentTSID, ",", StringUtil.DELIM_SKIP_BLANKS );
		independentTSList = TSUtil.selectTimeSeries ( 
			tsObjects, independentTSID_Vector, null );
	}

	// Do not set these properties if they are "" (empty).
	// PrincipalComponentAnalysis expects "null" when calling getValue()
	// for these properties to set the internal defaults.
	if ( AnalysisStart != null && AnalysisStart.length() > 0  ) {
            try {
                AnalysisStartDateTime = new DateTime(DateFormat.getDateTimeInstance().parse(AnalysisStart));
            } catch (ParseException ex) {
                Logger.getLogger(FillPrincipalComponentAnalysis_Command.class.getName()).log(Level.SEVERE, null, ex);
            }
	}
	
	if ( AnalysisEnd != null && AnalysisEnd.length() > 0  ) {
            try {
                AnalysisEndDateTime = new DateTime(DateFormat.getDateTimeInstance().parse(AnalysisEnd));
            } catch (ParseException ex) {
                Logger.getLogger(FillPrincipalComponentAnalysis_Command.class.getName()).log(Level.SEVERE, null, ex);
            }
	}

    /*
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
	*/

	if ( OutputFile != null && OutputFile.length() > 0  ) {
            try {
                out = new PrintWriter(OutputFile);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FillPrincipalComponentAnalysis_Command.class.getName()).log(Level.SEVERE, null, ex);
            }
	}

    int maxCombinationsInt = MaxCombinations == null? _maxCombinationsDefault :
        Integer.parseInt(MaxCombinations);

	// Run the PrincipalComponentAnalysis.
	try {
		// Instantiate the PrincipalComponentAnalysis	object
		__PrincipalComponentAnalysis = new TSPrincipalComponentAnalysis(
			dependentTS, 
			independentTSList,
			AnalysisStartDateTime,
            AnalysisEndDateTime,
            maxCombinationsInt,
            AnalysisMonths ).getPrincipalComponentAnalysis();
        if ( out != null ) {
            __PrincipalComponentAnalysis.printOutput(out);
            out.flush();
            out.close();
        }
	} catch ( Exception e ) {
		// REVISIT [LT 2005-04-20] Problems throwing an exception
		// from here to be catch by the calling object. This method
		// is called by actionPerformed and it is not allowing me 
		// to declare "Throws Exception". How to fix this?
		mssg = "Unexpected error performing principal component analysis (" + e + ").";
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
	String DependentTSID    = props.getValue ( "DependentTSID"    );
	String IndependentTSList= props.getValue ( "IndependentTSList");
	String IndependentTSID  = props.getValue ( "IndependentTSID"  );
	String AnalysisStart	= props.getValue ( "AnalysisStart"    );
	String AnalysisEnd	= props.getValue ( "AnalysisEnd"      );
	String MinimumDataCount	= props.getValue ( "MinimumDataCount" );
	String FillStart 	= props.getValue ( "FillStart"        );
	String FillEnd 		= props.getValue ( "FillEnd"          );
	String OutputFile	= props.getValue ( "OutputFile"       );

	// Creating the command string
	// This StringBuffer will contain all parameters for the command.
	StringBuffer b = new StringBuffer();

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

	// Adding the OutputFile
	if ( OutputFile != null && OutputFile.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "OutputFile=" + OutputFile );
	}

	return getCommandName() + "(" + b.toString() + ")";
}

}