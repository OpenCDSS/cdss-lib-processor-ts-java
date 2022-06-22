// RunCommands_Command - This class initializes, checks, and runs the RunCommands() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatusUtil;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.TS.TS;
import rti.tscommandprocessor.core.TSCommandFileRunner;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

// FIXME SAM 2008-07-15 Need to add ability to inherit the properties of the main processor.

/**
This class initializes, checks, and runs the RunCommands() command.
*/
public class RunCommands_Command extends AbstractCommand implements FileGenerator
{

/**
ExpectedStatus parameter values.
*/
protected final String _Unknown = "Unknown";
protected final String _Success = "Success";
protected final String _Warning = "Warning";
protected final String _Failure = "Failure";

/**
ResetWorkflowProperties parameter values.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
ResetWorkflowProperties parameter values.
*/
private final String __FAIL = "FAIL";
private final String __PASS = "PASS";

/**
*Sharing parameter values.
*/
protected final String _Copy = "Copy";
protected final String _DoNotShare = "DoNotShare";
protected final String _Share = "Share";

/**
AppendResults parameter values.
*/
// TODO SAM 2007-12-13 Need to enable AppendResults.

/**
 * The output file list.
 */
private List<File> outputFileList = new ArrayList<>();

/**
Constructor.
*/
public RunCommands_Command ()
{	super();
	setCommandName ( "RunCommands" );
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
{	String InputFile = parameters.getValue ( "InputFile" );
    String ExpectedStatus = parameters.getValue ( "ExpectedStatus" );
    //String ShareProperties = parameters.getValue ( "ShareProperties" );
    String ShareDataStores = parameters.getValue ( "ShareDataStores" );
    String AppendOutputFiles = parameters.getValue ( "AppendOutputFiles" );
	String warning = "";
    String message;
	
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an input file." ) );
	}
	else if ( InputFile.indexOf("${") < 0 ) {
	    String working_dir = null;
	
		try {
		    Object o = processor.getPropContents ( "WorkingDir" );
			// Working directory is available so use it.
			if ( o != null ) {
				working_dir = (String)o;
			}
		}
		catch ( Exception e ) {
			message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Software error - report problem to support." ) );
		}
	
		try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath ( working_dir, InputFile));
			File f = new File ( adjusted_path );
			if ( !f.exists() ) {
                message = "The input file does not exist: \"" + adjusted_path + "\".";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Verify that the command file to run exists - may be OK if file is created at run time." ) );
            }
			f = null;
		}
		catch ( Exception e ) {
            message = "The input file \"" + InputFile +
            "\" cannot be adjusted to an absolute path using the working directory \"" +
            working_dir + "\".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that command file to run and working directory paths are compatible." ) );
		}
	}
    
    if ( (ExpectedStatus != null) && (ExpectedStatus.length() == 0) &&
        !ExpectedStatus.equalsIgnoreCase(_Unknown) &&
        !ExpectedStatus.equalsIgnoreCase(_Success) &&
        !ExpectedStatus.equalsIgnoreCase(_Warning) &&
        !ExpectedStatus.equalsIgnoreCase(_Failure) ) {
        message = "The expected status is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify an expected status of " + _Unknown + ", " + _Success + ", " +
                _Warning + ", or " + _Failure) );
    }
    /*
    if ( (ShareProperties != null) && (ShareProperties.length() == 0) &&
        //!ShareProperties.equalsIgnoreCase(_Copy) &&
        !ShareProperties.equalsIgnoreCase(_DoNotShare) &&
        !ShareProperties.equalsIgnoreCase(_Share)) {
        message = "The ShareProperties parameter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify ShareProperties as " +// _Copy + ", " +
                _DoNotShare + " (default if blank), " + " or " + _Share) );
    }
    */
    
    if ( (ShareDataStores != null) && (ShareDataStores.length() == 0) &&
        !ShareDataStores.equalsIgnoreCase(_Copy) &&
        !ShareDataStores.equalsIgnoreCase(_DoNotShare) &&
        !ShareDataStores.equalsIgnoreCase(_Share)) {
        message = "The ShareDataStores parameter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify ShareDataStores as " + _Copy + ", " + _DoNotShare +
                " (default if blank), " + ", or " + _Share) );
    }

    if ( (AppendOutputFiles != null) && (AppendOutputFiles.length() == 0) &&
        !AppendOutputFiles.equalsIgnoreCase(_False) &&
        !AppendOutputFiles.equalsIgnoreCase(_True) ) {
        message = "The AppendOutputFiles parameter is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify AppendOutputFiles as " + _False + " (default) or " + _True) );
    }

	// Check for invalid parameters.
    List<String> validList = new ArrayList<>(6);
	validList.add ( "InputFile" );
    validList.add ( "ExpectedStatus" );
    validList.add ( "ShareProperties" );
    validList.add ( "ShareDataStores" );
    validList.add ( "AppendOutputFiles" );
    validList.add ( "WarningCountProperty" );
    validList.add ( "FailureCountProperty" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), warning );
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
{	// The command will be modified if edits are saved.
	return (new RunCommands_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command (from called commands).
@return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList () {
	// Create a new list.
	List<File> files = new ArrayList<>();
	for ( File f : this.outputFileList ) {
		files.add(f);
	}
	return files;
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3; // Level for non-user messages for log file.
	
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // Default.
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
		status.clearLog(CommandPhaseType.RUN);
	}
	PropList parameters = getCommandParameters();

	String InputFile = parameters.getValue ( "InputFile" );
	String RunDiscovery = parameters.getValue ( "RunDiscovery" );
	// TODO SAM 2013-02-17 Enable as a full property (what should be default?
	boolean runDiscovery = true;
	if ( (RunDiscovery != null) && RunDiscovery.equalsIgnoreCase("False") ) {
	    runDiscovery = false;
	}
    String ExpectedStatus = parameters.getValue ( "ExpectedStatus" );
    String ShareProperties = parameters.getValue ( "ShareProperties" );
    if ( (ShareProperties == null) || ShareProperties.equals("") ) {
        ShareProperties = _DoNotShare;
    }
    String ShareDataStores = parameters.getValue ( "ShareDataStores" );
    if ( (ShareDataStores == null) || ShareDataStores.equals("") ) {
        ShareDataStores = _Share;
    }
    // TODO smalers 2022-06-21 this is not enabled.  Need to implement for each major output.  See AppendOutputFiles.
	String AppendResults = parameters.getValue ( "AppendResults" );
	String AppendOutputFiles = parameters.getValue ( "AppendOutputFiles" );
	boolean appendOutputFiles = false; // Default.
	if ( (AppendOutputFiles != null) && AppendOutputFiles.equalsIgnoreCase("true") ) {
		appendOutputFiles = true;
	}
    String WarningCountProperty = parameters.getValue ( "WarningCountProperty" );
    WarningCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, WarningCountProperty);
    String FailureCountProperty = parameters.getValue ( "FailureCountProperty" );
    FailureCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, FailureCountProperty);
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
	        MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	// Get the working directory from the processor that is running the commands.

	String InputFile_full = null;
	try {
		InputFile_full = IOUtil.verifyPathForOS(
	        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        	TSCommandProcessorUtil.expandParameterValue(processor, this,InputFile) ) );
		Message.printStatus ( 2, routine,
			"Processing commands from file \"" + InputFile_full + "\" using command file runner.");
		
		// Create a runner for the commands, which will create a new command processor:
		// - the initial application properties from the current processor are passed to the new processor
		TSCommandFileRunner runner = new TSCommandFileRunner (
			((TSCommandProcessor)processor).getInitialPropList(),
			((TSCommandProcessor)processor).getPluginCommandClasses());
        // This will set the initial working directory of the runner to that of the command file.
		runner.readCommandFile(InputFile_full, runDiscovery );

		// Must set the datastores regardless of whether enabled because they are used by "@enabledif".
        TSCommandProcessor runnerProcessor = runner.getProcessor();
        if ( ShareDataStores.equalsIgnoreCase(_Share) ) {
            // All datastores are transferred.
            runnerProcessor.setPropContents("HydroBaseDMIList", processor.getPropContents("HydroBaseDMIList"));
            runnerProcessor.setDataStores(((TSCommandProcessor)processor).getDataStores(), false);
            // Also share the datastore substitution map.
            runnerProcessor.setDatastoreSubstituteList(((TSCommandProcessor)processor).getDataStoreSubstituteList());
        }

		// If the command file is not enabled, don't need to initialize or process.
		// TODO SAM 2013-04-20 Even if disabled, will still run discovery above - need to disable discovery in this case.
		Message.printStatus ( 2, routine,
			"  Checking whether the command file is enabled: " + InputFile_full );
		boolean isEnabled = runner.isCommandFileEnabled();
		Message.printStatus ( 2, routine, "  Command file is enabled? " + isEnabled);
        // Default expected status of running command file is success.
        String expectedStatus = CommandStatusType.SUCCESS.toString();
        if ( ExpectedStatus != null ) {
            expectedStatus = ExpectedStatus;
        }
		if ( isEnabled ) {
            // Set the database connection information:
            // - TODO SAM 2007-11-25 HydroBase needs to be converted to generic DataStore objects.
			/* TODO SAM 2022-04-19 moved to above because datastores are needed for "@enabledif" checks:
			 * - remove when tested
            TSCommandProcessor runnerProcessor = runner.getProcessor();
            if ( ShareDataStores.equalsIgnoreCase(_Share) ) {
                // All datastores are transferred.
                runnerProcessor.setPropContents("HydroBaseDMIList", processor.getPropContents("HydroBaseDMIList"));
                runnerProcessor.setDataStores(((TSCommandProcessor)processor).getDataStores(), false);
                // Also share the datastore substitution map.
                runnerProcessor.setDatastoreSubstituteList(((TSCommandProcessor)processor).getDataStoreSubstituteList());
            }
            */
            
            /*
             * TODO SAM 2010-09-30 Need to evaluate how to share properties - issue is that built-in properties are
             * handled explicitly whereas user-defined properties are in a list that can be easily shared.
             * Also, some properties like the working directory receive special treatment.
             * For now don't bite off the property issue.
            if ( ShareProperties.equalsIgnoreCase(_Copy) ) {
                setProcessorProperties(processor,runnerProcessor,true);
            }
            else if ( ShareProperties.equalsIgnoreCase(_Share) ) {
                // All data stores are transferred
                setProcessorProperties(processor,runnerProcessor,false);
            }
            */
            
            // Need to share the built-in StartLogEnabled property because it is used in troubleshooting
            // to ensure all logging goes to the main log file.
            Prop prop = processor.getProp("StartLogEnabled");
            if ( prop != null ) {
            	// Will be a Boolean.
            	runner.getProcessor().setPropContents("StartLogEnabled", prop.getContents());
            }

            // Run the commands:
            // - currently, the following will reset the processor properties to initial values; therefore,
            //   don't set properties above
    		runner.runCommands();

    	    // Total runtime for the commands.
            long runTimeTotal = TSCommandProcessorUtil.getRunTimeTotal(runner.getProcessor().getCommands());
    		
    		// Set the CommandStatus for this command:
            // - if have "@expectedStatus", add an additional fail message if the expected status does not
            //   agree with the actual status
            // - if no "@expectedStatus, set to the most severe status of the commands file that was just run
    		CommandStatusType maxSeverity = TSCommandProcessorUtil.getCommandStatusMaxSeverity((TSCommandProcessor)runner.getProcessor());
    		String testPassFail = "????"; // Status for the test, which is not always the same as maxSeverity.
    		if ( ExpectedStatus != null ) {
    		    if ( maxSeverity.toString().equalsIgnoreCase(ExpectedStatus) ) {
                    // User has indicated an expected status and it matches the actual so consider this a success.
                    // This should generally be used only when running a test that we expect to fail (e.g., run
                    // obsolete command or testing handling of errors).
                    status.addToLog(CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.SUCCESS,
                    	"Severity for RunCommands (" + maxSeverity +
                    	") is the maximum for the commands in the command file that was run - matches expected (" +
                    	ExpectedStatus + ") so RunCommands status=Success.",
                        "Additional status messages are omitted to allow test to be success - " +
                        "refer to log file if warning/failure."));
                    // TODO SAM 2008-07-09 Need to evaluate how to append all the log messages but still
                    // have a successful status that shows in the displays.
                    // DO NOT append the messages from the command because their status will cause the
                    // error displays to show problem indicators.
                    testPassFail = __PASS;
    		    }
    		    else {
    		        // User has specified an expected status and it does NOT match the actual status so this is a failure.
                    status.addToLog(CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.FAILURE,
                        "Severity for RunCommands (" + maxSeverity +
                        ") is the maximum for the commands in the command file that was run - does not match expected (" +
                        ExpectedStatus + ") so RunCommands status=Failure.",
                        "Check the command to confirm the expected status."));
                    // TODO SAM 2008-07-09 Need to evaluate how to append all the log messages but still
                    // have a successful status that shows in the displays.
                    // DO NOT append the messages from the command because their status will cause the
                    // error displays to show problem indicators.
                    testPassFail = __FAIL;
    		    }
            }
            else {
                status.addToLog(CommandPhaseType.RUN,new CommandLogRecord(maxSeverity,
    				"Severity for RunCommands (" + maxSeverity + ") is the maximum of the commands in the command file that was run.",
    				"Status messages from commands that were run are appended to RunCommand status messages."));
                // Append the log records from the command file that was run.
                // The status contains lists of CommandLogRecord for each run mode.
                // For RunCommands() the log messages should be associated with the originating command,
                // not this RunCommand command.
                CommandStatusUtil.appendLogRecords ( status, (List)runner.getProcessor().getCommands() );
                if ( maxSeverity.greaterThanOrEqualTo(CommandStatusType.WARNING)) {
                    testPassFail = __FAIL;
                }
                else {
                    testPassFail = __PASS;
                }
            }

    		// Set the properties indicating the number of warnings and failures from running the commands.
    		CommandPhaseType [] phases = { CommandPhaseType.RUN };
        	if ( (WarningCountProperty != null) && !WarningCountProperty.equals("") ) {
        		CommandStatusType [] statuses = { CommandStatusType.WARNING };
            	int warningCount = status.getCommandLog(phases, statuses).size();
            	PropList request_params = new PropList ( "" );
            	request_params.setUsingObject ( "PropertyName", WarningCountProperty );
            	request_params.setUsingObject ( "PropertyValue", new Integer(warningCount) );
            	try {
                	processor.processRequest( "SetProperty", request_params);
            	}
            	catch ( Exception e ) {
                	message = "Error requesting SetProperty(Property=\"" + WarningCountProperty + "\") from processor.";
                	Message.printWarning(log_level,
                    	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                	status.addToLog ( CommandPhaseType.RUN,
                    	new CommandLogRecord(CommandStatusType.FAILURE,
                        	message, "Report the problem to software support." ) );
            	}
        	}
        	if ( (FailureCountProperty != null) && !FailureCountProperty.equals("") ) {
        		CommandStatusType [] statuses = { CommandStatusType.FAILURE };
            	int failureCount = status.getCommandLog(phases, statuses).size();
            	PropList request_params = new PropList ( "" );
            	request_params.setUsingObject ( "PropertyName", FailureCountProperty );
            	request_params.setUsingObject ( "PropertyValue", new Integer(failureCount) );
            	try {
                	processor.processRequest( "SetProperty", request_params);
            	}
            	catch ( Exception e ) {
                	message = "Error requesting SetProperty(Property=\"" + FailureCountProperty + "\") from processor.";
                	Message.printWarning(log_level,
                    	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                	status.addToLog ( CommandPhaseType.RUN,
                    	new CommandLogRecord(CommandStatusType.FAILURE,
                        	message, "Report the problem to software support." ) );
            	}
        	}
        	
        	// Append the runner's output files to this processor so that they will be listed in TSTool results.
        	this.outputFileList.clear();
        	if ( appendOutputFiles ) {
        		// Loop through all the commands in run by the processor.
        		for ( Command command : runner.getProcessor().getCommands() ) {
        			if ( command instanceof FileGenerator ) {
        				// The command had output files:
        				// - add any files that exist to this command's output files.
        				FileGenerator fg = (FileGenerator)command;
        				for ( File file : fg.getGeneratedFileList() ) {
        					this.outputFileList.add(file);
        				}
        			}
        		}
        	}

            // Add a record to the regression report.

            TSCommandProcessorUtil.appendToRegressionTestReport(processor,isEnabled,runTimeTotal,
                 testPassFail,expectedStatus,maxSeverity,InputFile_full);
    
    		// If it was requested to append the results to the calling processor, get
    		// the results from the runner and do so.
    		
    		if ( (AppendResults != null) && AppendResults.equalsIgnoreCase("true")) {
    			TSCommandProcessor processor2 = runner.getProcessor();
    			Object o_tslist = processor2.getPropContents("TSResultsList");
    			PropList requestParams = new PropList ( "" );
    			if ( o_tslist != null ) {
    				@SuppressWarnings("unchecked")
					List<TS> tslist = (List<TS>)o_tslist;
    				int size = tslist.size();
    				TS ts;
    				for ( int i = 0; i < size; i++ ) {
    					ts = tslist.get(i);
    					requestParams.setUsingObject( "TS", ts );
    					processor.processRequest( "AppendTimeSeries", requestParams );
    				}
    			}
    		}
    		
    		Message.printStatus ( 2, routine,"...done processing commands from file." );
	    }
        else {
            // Add a record to the regression report:
        	// - the isEnabled value is what is important for the report
            //   because the test is not actually run
            TSCommandProcessorUtil.appendToRegressionTestReport(processor,isEnabled,0L,
                 "",expectedStatus,CommandStatusType.UNKNOWN,InputFile_full);
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error processing command file \"" + InputFile + "\", full path=\"" +
		    InputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new CommandException ( message );
	}
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String InputFile = props.getValue("InputFile");
    String ExpectedStatus = props.getValue("ExpectedStatus");
    //String ShareProperties = props.getValue("ShareProperties");
    String ShareDataStores = props.getValue("ShareDataStores");
    String AppendOutputFiles = props.getValue("AppendOutputFiles");
    String WarningCountProperty = props.getValue("WarningCountProperty");
    String FailureCountProperty = props.getValue("FailureCountProperty");
	StringBuffer b = new StringBuffer ();
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
    if ( (ExpectedStatus != null) && (ExpectedStatus.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExpectedStatus=" + ExpectedStatus );
    }
    /*
    if ( (ShareProperties != null) && (ShareProperties.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ShareProperties=" + ShareProperties );
    }
    */
    if ( (ShareDataStores != null) && (ShareDataStores.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ShareDataStores=" + ShareDataStores );
    }
    if ( (AppendOutputFiles != null) && (AppendOutputFiles.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AppendOutputFiles=" + AppendOutputFiles );
    }
    if ( (WarningCountProperty != null) && (WarningCountProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WarningCountProperty=\"" + WarningCountProperty + "\"" );
    }
    if ( (FailureCountProperty != null) && (FailureCountProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FailureCountProperty=\"" + FailureCountProperty + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}