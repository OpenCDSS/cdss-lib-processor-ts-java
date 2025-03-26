// FillPrincipalComponentAnalysis_Command - Command class.

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import javax.swing.JFrame;

import RTi.TS.TS;
import RTi.TS.TSException;
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
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSListType;

/**
Implement the FillPrincipalComponentAnalysis() command.
This command can run in command "batch" mode or in tool menu.
*/
public class FillPrincipalComponentAnalysis_Command extends AbstractCommand implements Command
{

// Run mode flag. 
private boolean __commandMode = true;

// This object is used to perform the analyze, create the fill commands and fill
// the dependent time series.
protected TSPrincipalComponentAnalysis __TSPCA = null;
public static final int _maxCombinationsDefault = 20;
protected TS __filledTS;

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
	//String DependentTSList  = parameters.getValue ( "DependentTSList"  );
	//String DependentTSID    = parameters.getValue ( "DependentTSID"    );
	String IndependentTSList= parameters.getValue ( "IndependentTSList");
	String IndependentTSID  = parameters.getValue ( "IndependentTSID"  );
	String AnalysisStart	= parameters.getValue ( "AnalysisStart"    );
	String AnalysisEnd	= parameters.getValue ( "AnalysisEnd"      );
	String FillStart 	= parameters.getValue ( "FillStart"        );
	String FillEnd 		= parameters.getValue ( "FillEnd"          );
	String MaxCombinations        = parameters.getValue ( "MaxCombinations"   );
	String RegressionEquationFill = parameters.getValue ( "RegressionEquationFill"   );
	String PCAOutputFile          = parameters.getValue ( "PCAOutputFile"       );
	String FilledTSOutputFile     = parameters.getValue ( "FilledTSOutputFile"  );
	
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
	if ( IndependentTSList.equalsIgnoreCase ( TSListType.ALL_MATCHING_TSID.toString() ) ||
         IndependentTSList.equalsIgnoreCase(TSListType.FIRST_MATCHING_TSID.toString()) ||
         IndependentTSList.equalsIgnoreCase(TSListType.LAST_MATCHING_TSID.toString()) ||
         IndependentTSList.equalsIgnoreCase(TSListType.SPECIFIED_TSID.toString())
            ) {
		if ( IndependentTSID != null ) {
			List<String> selectedV = StringUtil.breakStringList (
				IndependentTSID, ",",
				StringUtil.DELIM_SKIP_BLANKS );
			if ( (selectedV == null) || (selectedV.size() == 0) ) {
                message = "The IndependentTSID should not be empty when IndependentTSList="
                    + IndependentTSList
                    + "\" is specified.";
				warning += "\n" + message;

			}
		} else { 
			warning += "\n\"Independent TS list\" "
				+ "should not be null when " 
				+ "\"IndependentTSList = "
				+ IndependentTSList
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

	// Make sure AnalysisStart precedes AnalysisEnd
	if ( AnalysisStartDate != null && AnalysisEndDate != null ) {
		if ( ! AnalysisEndDate.greaterThanOrEqualTo(AnalysisStartDate) ) {
			warning += "\n Analysis Start \""
				+ AnalysisStart
				+ "\" should proceed Analysis End \""
				+ AnalysisEnd + "\".";
		}
	}

	// Make sure MaxCombinations > 0
	if ( MaxCombinations != null && Integer.parseInt(MaxCombinations)<=0)  {
			warning += "\n Maximum Combinations \""
				+ MaxCombinations
				+ "\" must be a number greater than 0.";
	}

	

	// Output file
	if ( PCAOutputFile != null && PCAOutputFile.length() != 0 ) {
		try {
			String adjusted_path = IOUtil.adjustPath (
				 working_dir, PCAOutputFile);
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
				+ "    \"" + PCAOutputFile;
		}
	} else {
		warning += "\nThe PCA output file field is empty.";
	}

    if ( __commandMode ) {
        // Make sure FillStart, if given, is a valid date
        DateTime FillStartDate = null;
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

        // Make sure AnalysisStart precedes FillStart
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

        if ( FilledTSOutputFile != null && FilledTSOutputFile.length() != 0 ) {
		try {
			String adjusted_path = IOUtil.adjustPath (
				 working_dir, FilledTSOutputFile);
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
				+ "    \"" + FilledTSOutputFile;
		}
        } else {
            warning += "\nThe filled ts output file field is empty.";
        }

        // Make sure RegressionEquationFill > 0
        if ( RegressionEquationFill != null && Integer.parseInt(RegressionEquationFill)<=0)  {
                warning += "\n Regression Equation \""
                    + RegressionEquationFill
                    + "\" must be a number greater than 0.";
        }
        // Make sure RegressionEquationFill is <= MaxCombinations
        int mcombo = MaxCombinations == null ? _maxCombinationsDefault : Integer.parseInt(MaxCombinations);
        if ( RegressionEquationFill != null && 
                Integer.parseInt(RegressionEquationFill)>mcombo) {
                warning += "\n Regression Equation \""
                    + RegressionEquationFill
                    + "\" must be a number less than or equal to " + mcombo + ".";
        }
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
 * Create the commands needed to fill the dependent time series applying the
 * selected regression equation to the independent time series.
*/
protected List<String> createFillCommands ()
{
    List<String> commandList = new ArrayList<String>();
    String command = "FillPrincipalComponentAnalysis";
    StringBuffer b = new StringBuffer();
    
    PropList parameters = getCommandParameters();
    String DependentTSList  = parameters.getValue ( "DependentTSList"  );
	String DependentTSID    = parameters.getValue ( "DependentTSID"    );
	String IndependentTSList= parameters.getValue ( "IndependentTSList");
	String IndependentTSID  = parameters.getValue ( "IndependentTSID"  );
	String AnalysisStart	= parameters.getValue ( "AnalysisStart"    );
	String AnalysisEnd	= parameters.getValue ( "AnalysisEnd"      );
	String FillStart 	= parameters.getValue ( "FillStart"        );
	String FillEnd 		= parameters.getValue ( "FillEnd"          );
	String PCAOutputFile	= parameters.getValue ( "PCAOutputFile"       );
	String FilledTSOutputFile	= parameters.getValue ( "FilledTSOutputFile"       );
    String MaxCombinations = parameters.getValue ( "MaxCombinations" );
    String RegressionEquationFill = parameters.getValue ( "RegressionEquationFill" );
    String AnalysisMonths = parameters.getValue("AnalysisMonths");
    //DateTime AnalysisStartDateTime = null, AnalysisEndDateTime = null;

    // dependent TS
    if ( DependentTSList != null && DependentTSList.length()>0) {
        b.append("DependentTSList=" + DependentTSList);
    }
    if ( DependentTSID != null && DependentTSID.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DependentTSID=\"" + DependentTSID + "\"" );
    }

    // independent TS
    if ( IndependentTSList != null && IndependentTSList.length()>0) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append("IndependentTSList=" + IndependentTSList);
    }
    if ( IndependentTSID != null && IndependentTSID.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IndependentTSID=\"" + IndependentTSID + "\"" );
    }

    // Analysis start
    if ( AnalysisStart != null && AnalysisStart.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AnalysisStart=\"" + AnalysisStart + "\"" );
    }

	// AnalysisEnd
    if ( AnalysisEnd != null && AnalysisEnd.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AnalysisEnd=\"" + AnalysisEnd + "\"" );
    }

	// FillStart
    if ( FillStart != null && FillStart.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FillStart=\"" + FillStart + "\"" );
    }

	// FillEnd
    if ( FillEnd != null && FillEnd.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FillEnd=\"" + FillEnd + "\"" );
    }

	// PCAOutputFile
    if ( PCAOutputFile != null && PCAOutputFile.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PCAOutputFile=\"" + PCAOutputFile + "\"" );
    }

	// FilledTSOutputFile
    if ( FilledTSOutputFile != null && FilledTSOutputFile.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FilledTSOutputFile=\"" + FilledTSOutputFile + "\"" );
    }

    // MaxCombinations
    if ( MaxCombinations != null && MaxCombinations.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MaxCombinations=" + MaxCombinations );
    }

    // Regression Equation Fill
    if ( RegressionEquationFill != null && RegressionEquationFill.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "RegressionEquationFill=" + RegressionEquationFill );
    }

    // AnalysisMonths
    if ( AnalysisMonths != null && AnalysisMonths.length() > 0 ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AnalysisMonths=\"" + AnalysisMonths + "\"" );
    }

    commandList.add(command + "(" + b + ")");
	return commandList;
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
	//if ( isCommandMode() ) {
		return ( new FillPrincipalComponentAnalysis_JDialog ( parent, this ) ).ok();
	//} else {
		//return ( new FillPrincipalComponentAnalysis_JDialog ( parent, this, 0 ) ).ok();
	//}
}

/**
Fill the dependent time series applying the chosen regression equation to the independent
time series.  PCA must already have been calculated.
*/
protected void fillDependents() throws InvalidCommandParameterException
{
    String rtn = "FillPrincipalComponentAnalysis_Command_fillDependents", message;
    int warning_level = 1;

    if ( getPrincipalComponentAnalysis() == null ) {
        Message.printWarning(1, rtn, "Principal Component Analysis not available.");
        return;
    }

    PropList parameters = getCommandParameters();
    String RegressionEquationFill = parameters.getValue ( "RegressionEquationFill" );
	String FilledTSOutputFile	= parameters.getValue ( "FilledTSOutputFile"       );
    String FillStart 	= parameters.getValue ( "FillStart" );
	String FillEnd 		= parameters.getValue ( "FillEnd" );
    DateTime fillStartDateTime=null, fillEndDateTime=null;

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    
    // Filled TS Output file
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
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Software error - report the problem to support." ) );
	}


    message = "";
	if ( FilledTSOutputFile != null && FilledTSOutputFile.length() != 0 ) {
		try {
			String adjusted_path = IOUtil.adjustPath (
				 working_dir, FilledTSOutputFile);
			File f  = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				message = "\nThe file parent directory "
					+ " does not exist:\n"
					+ adjusted_path;
                Message.printWarning ( 2, rtn, message);
			}
			f  = null;
			f2 = null;
		}
		catch ( Exception e ) {
			message = "\nThe working directory:\n"
				+ "    \"" + working_dir
				+ "\"\ncannot be adjusted using:\n"
				+ "    \"" + FilledTSOutputFile;
             Message.printWarning ( 2, rtn, message);
		}
	} else {
		 message = "\nThe Filled TS output file field is empty.";
         Message.printWarning ( 2, rtn, message);
	}
    if ( message.length() != 0 ) {
        Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				null, warning_level ),
                message );
        throw new InvalidCommandParameterException ( message );
    }

    int regressionEqIndex=1;
    if ( RegressionEquationFill != null && RegressionEquationFill.length() > 0 ) {
        regressionEqIndex = Integer.parseInt(RegressionEquationFill);
    }

	if ( FillStart != null && FillStart.length() > 0  ) {
            try {
                fillStartDateTime = DateTime.parse(FillStart);
            } catch (Exception ex) {
                Message.printWarning ( warning_level, rtn, "Unable to convert fill period (start) " + FillStart + " to DateTime.");
            }
	}

	if ( FillEnd != null && FillEnd.length() > 0  ) {
            try {
                fillEndDateTime = DateTime.parse(FillEnd);
            } catch (Exception ex) {
                Message.printWarning ( warning_level, rtn, "Unable to convert fill period (end) " + FillEnd + " to DateTime.");
            }
	}

    try {
        __filledTS = __TSPCA.fill(regressionEqIndex, fillStartDateTime, fillEndDateTime);
    } catch (Exception ex) {
        Message.printWarning ( 1, rtn, "Unable to fill TS.");
        return;
    }

    try {
        List<TS> tslist = new ArrayList<TS>();
        tslist.add(__filledTS);
        TSUtil.formatOutput(FilledTSOutputFile, tslist, null);
    } catch (TSException ex) {
        Message.printWarning ( 1, rtn, "Unable to print TS.");
    }

}

/**
Get the time series to process.
@param processor CommandProcessor to handle data requests.
@param TSList TSList command parameter.
@param TSID TSID command parameter.
*/
private List<Object> getTimeSeriesToProcess ( CommandProcessor processor,
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
	List<TS> tslist = null;
	if ( o_TSList == null ) {
		message = "Unable to find time series to process using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( log_level, routine, message );
	}
	else {
		tslist = (List<TS>)o_TSList;
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
	List<Object> data = new Vector<Object>(2);
	data.add ( tslist );
	data.add ( indices );
	return data;
}

public PrincipalComponentAnalysis getPrincipalComponentAnalysis() {
    return __TSPCA.getPrincipalComponentAnalysis();
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
{	String rtn = "fillMixedStation_Command.parseCommand", mssg;
	int warning_level = 2;

	List<String> tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the command name and the InputFile
		mssg = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level, rtn, mssg);
		throw new InvalidCommandSyntaxException ( mssg );
	}

	// Get the input needed to process the file...
	try {
		setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
			(String)tokens.get(1), rtn, "," ) );
	}
	catch ( Exception e ) {
		mssg = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level, rtn, mssg);
		throw new InvalidCommandSyntaxException ( mssg );
	}
}

/*
 * Run the PCA Analysis.
 */
public void runAnalysis ( String command_tag )
throws InvalidCommandParameterException,
       CommandWarningException,
       CommandException
{	String rtn = "fillMixedStation_Command.runCommand", mssg = "";

    int warning_level = 2;
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
	String PCAOutputFile	= parameters.getValue ( "PCAOutputFile"       );
    String MaxCombinations = parameters.getValue ( "MaxCombinations" );
    String AnalysisMonths = parameters.getValue("AnalysisMonths");
    DateTime AnalysisStartDateTime = null, AnalysisEndDateTime = null;

	List<Object> v;
	int [] tspos;
	int tsCount;
	
	TSCommandProcessor processor = (TSCommandProcessor) getCommandProcessor();
    PrintWriter out = null;

	String working_dir = null;
	try { Object o = processor.getPropContents ( "WorkingDir" );
		// Working directory is available so use it...
		if ( o != null ) {
			working_dir = (String)o;
		}
	}
	catch ( Exception e ) {
        Message.printWarning(warning_level,
							MessageUtil.formatMessageTag( command_tag, ++warning_count),
							rtn, "Error requesting WorkingDir from processor." );
	}

    //
	// Get the list of DEPENDENT time series to process...
    //
    TS dependentTS = null;
	List<TS> dependentTSList = null; 
		
    // Get the time series from the command list.
    // The getTimeSeriesToProcess method will properly return the
    // time series according to the settings of DependentTSList.
    // It's like Highlander:  "There can be only 1" ... dependent time series.
    v = getTimeSeriesToProcess( processor, DependentTSList, DependentTSID );
    List<TS> tslist = (List<TS>)v.get(0);
    tspos = (int [])v.get(1);
    tsCount = tslist.size();
    if ( tsCount == 0 ) {
        mssg = "Unable to find time series using DependentTSID \""
            + DependentTSID + "\".";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
                command_tag, ++warning_count), rtn, mssg );
    }
    dependentTSList = new Vector<TS>( tsCount );
    for ( int nTS = 0; nTS < tsCount; nTS++ ) {
        // Get the time series object.
        try {
                PropList request_params = new PropList ( "" );
                request_params.setUsingObject ( "Index", Integer.valueOf(tspos[nTS]) );
                CommandProcessorRequestResultsBean bean = null;
                try { bean =
                    processor.processRequest( "GetTimeSeries", request_params);
                }
                catch ( Exception e ) {
                    Message.printWarning(log_level,
                            MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            rtn, "Error requesting GetTimeSeries(Index=" + tspos[nTS] +
                            "\" from processor." );
                }
                PropList bean_PropList = bean.getResultsPropList();
                Object prop_contents = bean_PropList.getContents ( "TS" );
                if ( prop_contents == null ) {
                    Message.printWarning(warning_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        rtn, "Null value for GetTimeSeries(Index=" + tspos[nTS] +
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

    //
	// Get the list of INDEPENDENT time series to process...
    //
	List<TS> independentTSList = null;
		
    // Get the time series from the command list.
    // The getTimeSeriesToProcess method will properly return the
    // time series according to the settings of IndependentTSList.
    v = getTimeSeriesToProcess( processor, IndependentTSList, IndependentTSID);
    tslist = (List<TS>)v.get(0);
    tspos = (int [])v.get(1);
    tsCount = tslist.size();
    if ( tsCount == 0 ) {
        mssg = "Unable to find time series using IndependentTSID \""
            + IndependentTSID + "\".";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
                command_tag, ++warning_count), rtn, mssg );
    }
    independentTSList = new Vector<TS>( tsCount );
    TS independentTS = null;
    for ( int nTS = 0; nTS < tsCount; nTS++ ) {
        // Get the time series object.
        try {
            PropList request_params = new PropList ( "" );
            request_params.setUsingObject ( "Index", Integer.valueOf(tspos[nTS]) );
            CommandProcessorRequestResultsBean bean = null;
            try { bean = processor.processRequest( "GetTimeSeries", request_params);
            }
            catch ( Exception e ) {
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        rtn, "Error requesting GetTimeSeries(Index=" + tspos[nTS] +
                        "\" from processor." );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "TS" );
            if ( prop_contents == null ) {
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    rtn, "Null value for GetTimeSeries(Index=" + tspos[nTS] +
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
		 
	if ( AnalysisStart != null && AnalysisStart.length() > 0  ) {
            try {
                AnalysisStartDateTime = DateTime.parse(AnalysisStart);
            } catch (Exception ex) {
                Message.printWarning (log_level, rtn, "Problems setting start date (" + AnalysisStart + ")");
            }
	}
	
	if ( AnalysisEnd != null && AnalysisEnd.length() > 0  ) {
            try {
                AnalysisEndDateTime = DateTime.parse(AnalysisEnd);
            } catch (Exception ex) {
                Message.printWarning (log_level, rtn, "Problems setting end date (" + AnalysisEnd + ")");
            }
	}

    

	if ( PCAOutputFile != null && PCAOutputFile.length() > 0  ) {
            try {
                // Convert to an absolute path if necessary...
                String adjusted_path = IOUtil.adjustPath ( working_dir, PCAOutputFile);
                out = new PrintWriter(adjusted_path);
            } catch (FileNotFoundException ex) {
                Message.printWarning(2, rtn, "Error opening file " + PCAOutputFile );
            } catch (Exception ex ) {
                Message.printWarning(2, rtn, "Problem with file " + PCAOutputFile );
            }
	}

    int maxCombinationsInt = (MaxCombinations == null || MaxCombinations.length() == 0) ? _maxCombinationsDefault :
        Integer.parseInt(MaxCombinations);

	// Run the PrincipalComponentAnalysis.
	try {
		// Instantiate the TSPrincipalComponentAnalysis	object
        __TSPCA = new TSPrincipalComponentAnalysis(
			(TS) dependentTSList.get(0),
			independentTSList,
			AnalysisStartDateTime,
            AnalysisEndDateTime,
            maxCombinationsInt,
            AnalysisMonths );
        if ( out != null ) {
            __TSPCA.getPrincipalComponentAnalysis().printOutput(out);
            out.flush();
            out.close();
        }
	} catch ( Exception e ) {
		mssg = "Unexpected error performing principal component analysis (" + e + ").";
		Message.printWarning (1, rtn, mssg );
	} 

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		mssg = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			rtn, mssg );
		throw new CommandWarningException ( mssg );
	}
     
}

/**
Run the command:
<pre>
fillPrincipalComponentAnalysis (
           DependentTSList="...",
		   DependentTSList="X,Y,...",
		   IndependentTSList="...",
		   IndependentTSList="X,Y,...",
		   AnalysisStart="...",
		   AnalysisEnd="...",
		   FillStart="...",
		   FillEnd="...",
		   MaxCombinations="..."
		   RegressionEquationFill="..."
		   PCAOutputFile="...",
		   FilledTSOutputFile="...")
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
{	String command_tag = "" + command_number;

    // first run PCA analysis...
    runAnalysis ( command_tag );

    // now fill the dependent time series
    fillDependents();
}

// commandMode true=command; false=tool
public void setCommandMode(boolean commandMode) {
    this.__commandMode = commandMode;
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
		"AnalysisStart",
		"AnalysisEnd",
		"MinimumDataCount",
		"FillStart",
		"FillEnd",
		"MaxCombinations",
		"RegressionEquationFill",
		"PCAOutputFile",
		"FilledTSOutputFile"
	};
	return this.toString(parameters, parameterOrder);
}

}