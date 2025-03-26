// CheckFile_Command - This class initializes, checks, and runs the CheckFile() command.

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

package rti.tscommandprocessor.commands.check;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import RTi.TS.CheckType;
import RTi.TS.TSUtil_CheckTimeSeries;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
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
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the CheckFile() command.
*/
public class CheckFile_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

/**
The table that is created in discovery mode.
*/
private DataTable __discoveryTable = null;

/**
Values for IfCriteriaMet parameter.
*/
protected final String _Fail = "Fail";
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";

/**
 * Values for statistics.
 */
protected final String _FileSizeBytes = "FileSizeBytes";
protected final String _FileSizeLines = "FileSizeLines";
protected final String _PatternMatchLineCount = "PatternMatchLineCount";

/**
Constructor.
*/
public CheckFile_Command ()
{   super();
    setCommandName ( "CheckFile" );
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
{   String InputFile = parameters.getValue ( "InputFile" );
    String IfNotFound = parameters.getValue ( "IfNotFound" );
    String Statistic = parameters.getValue ( "Statistic" );
    String SearchPattern = parameters.getValue ( "SearchPattern" );
    String CheckCriteria = parameters.getValue ( "CheckCriteria" );
    String CheckValue1 = parameters.getValue ( "CheckValue1" );
    String CheckValue2 = parameters.getValue ( "CheckValue2" );
    String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
	if ( (InputFile == null) || InputFile.isEmpty() ) {
		message = "The input file to check must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the file to check."));
	}
	if ( (IfNotFound != null) && !IfNotFound.isEmpty() ) {
		if ( !IfNotFound.equalsIgnoreCase(_Ignore) && !IfNotFound.equalsIgnoreCase(_Warn)
		    && !IfNotFound.equalsIgnoreCase(_Fail) ) {
			message = "The IfNotFound parameter \"" + IfNotFound + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Ignore + ", " + _Warn + " (default), or " +
					_Fail + "."));
		}
	}
    if ( (Statistic == null) || Statistic.equals("") ) {
        message = "The statistic must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the statistic to calculate." ) );
    }
    else {
        // Make sure that the statistic is known in general.
        boolean supported = false;
        List<String> statistics = getStatisticChoicesAsStrings();
        for ( String statistic : statistics ) {
            if ( Statistic.equalsIgnoreCase(statistic) ) {
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
       
        // Additional checks that depend on the statistic.
        
        if ( supported ) {
            if ( Statistic.equalsIgnoreCase(this._PatternMatchLineCount) ) {
                if ( (SearchPattern == null) || SearchPattern.isEmpty() ) {
                    message = "SearchPattern must be specified for the " + this._PatternMatchLineCount + " statistic.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Provide SearchPattern." ) );
                }
            }
        }
    }
    
    if ( (CheckCriteria == null) || CheckCriteria.equals("") ) {
        message = "The check criteria must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide the check criteria to evaluate." ) );
    }
    else {
        // Make sure that it is in the supported list.
        CheckType checkType = CheckType.valueOfIgnoreCase(CheckCriteria);
        if ( checkType == null ) {
            message = "The check criteria (" + CheckCriteria + ") is not recognized.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Select a supported criteria using the command editor." ) );
        }

        // Additional checks that depend on the criteria:
        // - OK to use time series code since statistic checks are generic
        
        int nRequiredValues = 0;
        if ( checkType != null ) {
            nRequiredValues = TSUtil_CheckTimeSeries.getRequiredNumberOfValuesForCheckCriteria ( checkType );
        }
        
        if ( nRequiredValues >= 1 ) {
            if ( (CheckValue1 == null) || CheckValue1.equals("") ) {
                message = "CheckValue1 must be specified for the criteria.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Provide CheckValue1." ) );
            }
            else if ( !StringUtil.isDouble(CheckValue1) ) {
                message = "CheckValue1 (" + CheckValue1 + ") is not a number.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify CheckValue1 as a number." ) );
            }
        }
        
        if ( nRequiredValues == 2 ) {
            if ( (CheckValue2 == null) || CheckValue2.equals("") ) {
                message = "CheckValue2 must be specified for the criteria.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Provide CheckValue2." ) );
            }
            else if ( !StringUtil.isDouble(CheckValue2) ) {
                message = "CheckValue2 (" + CheckValue2 + ") is not a number.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify CheckValue2 as a number." ) );
            }
        }
    }

    // Check for invalid parameters.
    List<String> validList = new ArrayList<>(15);
    validList.add ( "InputFile" );
    validList.add ( "IfNotFound" );
    validList.add ( "Statistic" );
    validList.add ( "SearchPattern" );
    validList.add ( "CheckCriteria" );
    validList.add ( "CheckValue1" );
    validList.add ( "CheckValue2" );
    validList.add ( "IfCriteriaMet" );
    validList.add ( "ProblemType" );
    validList.add ( "CheckResultPropertyName" );
    validList.add ( "CriteriaMetPropertyValue" );
    validList.add ( "CriteriaNotMetPropertyValue" );
    validList.add ( "TableID" );
    validList.add ( "TableFilenameColumn" );
    validList.add ( "TableStatisticColumn" );
    validList.add ( "TableStatisticValueColumn" );
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
Check the file statistic.
@param statisticInt the statistic that was computed
@param checkCriteria the check criteria compared to the statistic
@param checkValue1 the value of the check criteria
@param checkValue2 the second value of the check criteria (if needed)
@param ifCriteriaMet action if the criteria is matched (for messaging)
@param problemType the string to use for problem messages, when criteria is met
@param problems a list of problems encountered during processing, to add to command status in calling code
@return true if the check matches the criteria, false if not (or could not be evaluated).
For example, true will be returned if the statistic
is 100 and the check criteria is for values > 90.
*/
public boolean checkStatistic ( Object statisticValue,
    CheckType checkCriteria, Integer checkValue1, Integer checkValue2,
    String ifCriteriaMet, String problemType,
    List<String> problems )
{
    if ( statisticValue == null ) {
        // Statistic was not computed so this is definitely an error.
        problems.add ( "Statistic was not computed - unable to check its value." );
        return false;
    }
    boolean meetsCriteria = false;
    if ( statisticValue instanceof Integer ) {
        // Do comparisons on integers.
        Integer statisticInteger = (Integer)statisticValue;
        if ( checkCriteria == CheckType.IN_RANGE ) {
            if ( (statisticInteger.intValue() >= checkValue1) && (statisticInteger <= checkValue2.intValue()) ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.OUT_OF_RANGE ) {
            if ( (statisticInteger.intValue() < checkValue1) || (statisticInteger > checkValue2.intValue()) ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.LESS_THAN ) {
            if ( statisticInteger.intValue() < checkValue1.intValue() ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.LESS_THAN_OR_EQUAL_TO ) {
            if ( statisticInteger.intValue() <= checkValue1.intValue() ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.GREATER_THAN ) {
            if ( statisticInteger.intValue() > checkValue1.intValue() ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.GREATER_THAN_OR_EQUAL_TO ) {
            if ( statisticInteger.intValue() >= checkValue1.intValue() ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.EQUAL_TO ) {
            if ( statisticInteger.intValue() == (int)checkValue1.intValue() ) {
                meetsCriteria = true;
            }
        }
        else if ( checkCriteria == CheckType.NOT_EQUAL_TO ) {
            if ( statisticInteger.intValue() != (int)checkValue1.intValue() ) {
                meetsCriteria = true;
            }
        }
    }
    else {
        // Don't know how to handle.
        problems.add ( "Statistic is not an integer - unable to check the statistic value." );
    }
    return meetsCriteria;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{   List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this, true);
    return (new CheckFile_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Get the list of check types that can be performed.
*/
public List<CheckType> getCheckCriteriaChoices()
{
    List<CheckType> choices = new ArrayList<>();
    choices.add ( CheckType.IN_RANGE );
    choices.add ( CheckType.OUT_OF_RANGE );
    //choices.add ( CheckType.MISSING );
    choices.add ( CheckType.LESS_THAN );
    choices.add ( CheckType.LESS_THAN_OR_EQUAL_TO );
    choices.add ( CheckType.GREATER_THAN );
    choices.add ( CheckType.GREATER_THAN_OR_EQUAL_TO );
    choices.add ( CheckType.EQUAL_TO );
    choices.add ( CheckType.NOT_EQUAL_TO );
    return choices;
}

/**
Get the list of statistics that can be performed.
@return the statistic display names as strings.
*/
public List<String> getCheckCriteriaChoicesAsStrings()
{
    List<CheckType> choices = getCheckCriteriaChoices();
    List<String> stringChoices = new ArrayList<>();
    for ( int i = 0; i < choices.size(); i++ ) {
        stringChoices.add ( "" + choices.get(i) );
    }
    return stringChoices;
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __discoveryTable;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{   DataTable table = getDiscoveryTable();
    List<T> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new ArrayList<T>();
        v.add ( (T)table );
    }
    return v;
}

/**
 * Get the statistics choices for use in the editor.
 */
public List<String> getStatisticChoicesAsStrings() {
	List<String> choices = new ArrayList<>();
	choices.add(this._FileSizeBytes);
	choices.add(this._FileSizeLines);
	choices.add(this._PatternMatchLineCount);
	return choices;
}

// Parse command is in the base class.

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
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
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   String message, routine = getClass().getSimpleName() + ".runCommand";
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    int log_level = 3; // Level for non-user messages for log file.
    
    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    PropList parameters = getCommandParameters();

    Boolean clearStatus = Boolean.TRUE; // Default.
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
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }
    
    // Get the input parameters.

	String InputFile = parameters.getValue ( "InputFile" ); // Expanded below.
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	if ( (IfNotFound == null) || IfNotFound.equals("")) {
	    IfNotFound = _Warn; // Default.
	}
    String Statistic = parameters.getValue ( "Statistic" );
	Statistic = TSCommandProcessorUtil.expandParameterValue(processor, this, Statistic);
    String SearchPattern = parameters.getValue ( "SearchPattern" );
	SearchPattern = TSCommandProcessorUtil.expandParameterValue(processor, this, SearchPattern);
    String CheckCriteria = parameters.getValue ( "CheckCriteria" );
	CheckCriteria = TSCommandProcessorUtil.expandParameterValue(processor, this, CheckCriteria);
    CheckType checkCriteria = CheckType.valueOfIgnoreCase(CheckCriteria);
    String CheckValue1 = parameters.getValue ( "CheckValue1" );
	CheckValue1 = TSCommandProcessorUtil.expandParameterValue(processor, this, CheckValue1);
    Integer CheckValue1_Integer = null;
    if ( (CheckValue1 != null) && !CheckValue1.equals("") ) {
        CheckValue1_Integer = Integer.valueOf(CheckValue1);
    }
    String CheckValue2 = parameters.getValue ( "CheckValue2" );
	CheckValue2 = TSCommandProcessorUtil.expandParameterValue(processor, this, CheckValue2);
    Integer CheckValue2_Integer = null;
    if ( (CheckValue2 != null) && !CheckValue2.equals("") ) {
        CheckValue2_Integer = Integer.valueOf(CheckValue2);
    }
    String IfCriteriaMet = parameters.getValue ( "IfCriteriaMet" );
    if ( (IfCriteriaMet == null) || IfCriteriaMet.equals("") ) {
        IfCriteriaMet = _Warn; // Default.
    }
    String ProblemType = parameters.getValue ( "ProblemType" );
	ProblemType = TSCommandProcessorUtil.expandParameterValue(processor, this, ProblemType);
    if ( (ProblemType == null) || ProblemType.equals("") ) {
        ProblemType = Statistic + "-" + CheckCriteria; // Default.
    }
    String CheckResultPropertyName = parameters.getValue ( "CheckResultPropertyName" );
	CheckResultPropertyName = TSCommandProcessorUtil.expandParameterValue(processor, this, CheckResultPropertyName);
    String CriteriaMetPropertyValue = parameters.getValue ( "CriteriaMetPropertyValue" );
	CriteriaMetPropertyValue = TSCommandProcessorUtil.expandParameterValue(processor, this, CriteriaMetPropertyValue);
    String CriteriaNotMetPropertyValue = parameters.getValue ( "CriteriaNotMetPropertyValue" );
	CriteriaNotMetPropertyValue = TSCommandProcessorUtil.expandParameterValue(processor, this, CriteriaNotMetPropertyValue);

	// Output table (optional).
    String TableID = parameters.getValue ( "TableID" );
	TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    String TableFilenameColumn = parameters.getValue ( "TableFilenameColumn" );
	TableFilenameColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableFilenameColumn);
    if ( (TableFilenameColumn == null) || TableFilenameColumn.isEmpty() ) {
    	TableFilenameColumn = "File"; // Default.
    }
    String TableStatisticColumn = parameters.getValue ( "TableStatisticColumn" );
	TableStatisticColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableStatisticColumn);
    if ( (TableStatisticColumn == null) || TableStatisticColumn.isEmpty() ) {
    	TableStatisticColumn = "Statistic"; // Default.
    }
    String TableStatisticValueColumn = parameters.getValue ( "TableStatisticValueColumn" );
	TableStatisticValueColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, TableStatisticValueColumn);
    if ( (TableStatisticValueColumn == null) || TableStatisticValueColumn.isEmpty() ) {
    	TableStatisticValueColumn = "StatisticValue"; // Default.
    }

    PropList request_params = new PropList ( "" );
    CommandProcessorRequestResultsBean bean = null;
    
    DataTable table = null;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be updated.
        request_params = new PropList ( "" );
        request_params.set ( "TableID", TableID );
        try {
            bean = processor.processRequest( "GetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table == null ) {
            message = "Unable to find table to process using TableID=\"" + TableID + "\".";
            Message.printStatus(2, routine, "Will create a new table.");
        }
        else {
            table = (DataTable)o_Table;
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
    
    // Now process.
    
    try {
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create an empty table and set the ID.
            table = new DataTable();
            table.setTableID ( TableID );
            setDiscoveryTable ( table );
        }
        else {
        	// Running the command.
        	String InputFile_full = IOUtil.verifyPathForOS(
               	IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
        	       	TSCommandProcessorUtil.expandParameterValue(processor, this,InputFile) ) );
           	File file = new File ( InputFile_full );
	       	if ( !file.exists() ) {
               	message = "File to check \"" + InputFile_full + "\" does not exist.";
               	if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
                   	Message.printWarning ( warning_level,
                       	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                   	status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                           	message, "Verify that the file exists at the time the command is run."));
               	}
               	else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
                   	Message.printWarning ( warning_level,
                       	MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                   	status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                           	message, "Verify that the file exists at the time the command is run."));
               	}
               	else {
                   	Message.printStatus( 2, routine, message + "  Ignoring.");
               	}
	       	}
    
        	boolean newTable = false;
    	   	if ( (table == null) && (TableID != null) && !TableID.isEmpty() &&
           		(TableFilenameColumn != null) && !TableFilenameColumn.equals("") &&
               	(TableStatisticColumn != null) && !TableStatisticColumn.equals("") &&
               	(TableStatisticValueColumn != null) && !TableStatisticValueColumn.equals("") ) {
    		   	// Create a new table.
    		   	table = new DataTable();
    		   	table.setTableID(TableID);
    		   	// Add columns for the filename and statistic.
    		   	table.addField(new TableField(TableField.DATA_TYPE_STRING, TableFilenameColumn, InputFile.length()), "" );
    		   	table.addField(new TableField(TableField.DATA_TYPE_STRING, TableStatisticColumn, Statistic.toString().length()), "" );
    		   	// All statistics are currently integers.
    		   	table.addField(new TableField(TableField.DATA_TYPE_INT, TableStatisticValueColumn), null );
    		   	newTable = true;
    	   	}
    	   	
    	   	// TODO smalers 2021-07-24 loop in case later want to allow more than one file to be processed.
    	   	int nfiles = 1;
           	for ( int ifile = 0; ifile < nfiles; ifile++ ) {
               	// The file to process, from the list that was returned above.
               	
               	try {
                   	// Do the statistic calculation.
                   	notifyCommandProgressListeners ( ifile, nfiles, (float)-1.0, "Checking statistic for " + InputFile_full );
                   	Integer statisticInt = null;
               	   	File f = new File(InputFile_full);
                   	if ( Statistic.equalsIgnoreCase(this._FileSizeBytes) ) {
                	   	statisticInt = Integer.valueOf((int)f.length());
                   	}
                   	else if ( Statistic.equalsIgnoreCase(this._FileSizeLines) ) {
                	   	statisticInt = IOUtil.lineCount(f);
                   	}
                   	else if ( Statistic.equalsIgnoreCase(this._PatternMatchLineCount) ) {
                	   	// Convert the search pattern to Java regular expression.
                	   	String pattern = SearchPattern.replace("*", ".*");
                	   	statisticInt = IOUtil.matchCount(f, pattern, true);
                	   	Message.printStatus(2, routine, "Found " + statisticInt + " occurrances of '" + pattern + "'");
                   	}
                   	// Now set in the table.
                   	if ( (table != null) &&
                   		(TableFilenameColumn != null) && !TableFilenameColumn.equals("") &&
                       	(TableStatisticColumn != null) && !TableStatisticColumn.equals("") &&
                       	(TableStatisticValueColumn != null) && !TableStatisticValueColumn.equals("") ) {
                   		// Have everything needed to insert the result into a table.
                       	// See if a row matching filename and statistic exists.
                       	// TODO smalers 2021-07-24 Filename is currently the relative path, may add modifier function to convert to other value.
                       	Message.printStatus(2,routine, "Searching column \"" + TableFilenameColumn + "\" for file \"" +
                       	InputFile + "\" and statistic \"" + Statistic + "\"" );
                        List<String> columnNames = new ArrayList<>();
                        List<Object> columnValues = new ArrayList<>();
                        columnNames.add(TableFilenameColumn);
                        columnNames.add(TableStatisticColumn);
                        columnValues.add(InputFile);
                        columnValues.add(Statistic);
                        List<TableRecord> records = table.getRecords(columnNames, columnValues);
                        Message.printStatus(2,routine, "Found " + records.size() );
                       	int fileColumn = -1;
                       	int statisticColumn = -1;
                       	int statisticValueColumn = -1;
                        if ( records.size() > 0 ) {
                        	// Should not happen. Should match one or zero records.
                        	message = "Matched > records in table for file \"" + InputFile +
                        		"\" and statistic \"" + Statistic + "\" - cannot save result.";
                        	Message.printWarning ( warning_level,
                           		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                        	status.addToLog(commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
                           		message, "Verify that the table has unique records for file and statistic."));
                        }
                        else if ( records.size() == 0 ) {
                        	// May be that the column does not exist:
                        	// - if does not exist, create the column
                        	try {
                           		fileColumn = table.getFieldIndex(TableFilenameColumn);
                        	}
                        	catch ( Exception e2 ) {
                           		// Automatically add to the table, initialize with null (not nonValue).
                          		// TODO smalers 2021-07-24 may need to use double.
                           		fileColumn = table.addField(new TableField(TableField.DATA_TYPE_STRING,TableFilenameColumn,InputFile.length()), null );
                        	}
                        	try {
                           		statisticColumn = table.getFieldIndex(TableStatisticColumn);
                        	}
                        	catch ( Exception e2 ) {
                           		// Automatically add to the table, initialize with null (not nonValue).
                         		// TODO smalers 2021-07-24 may need to use double.
                           		statisticColumn = table.addField(new TableField(TableField.DATA_TYPE_STRING,TableStatisticColumn,30), null );
                        	}
                        	try {
                           		statisticValueColumn = table.getFieldIndex(TableStatisticValueColumn);
                        	}
                        	catch ( Exception e2 ) {
                           		// Automatically add to the table, initialize with null (not nonValue).
                         		// TODO smalers 2021-07-24 may need to use double.
                           		statisticValueColumn = table.addField(new TableField(TableField.DATA_TYPE_INT,TableStatisticValueColumn,-1,-1), null );
                        	}
                        }
                       	if ( records.size() == 1 ) {
                           	// There is already a row for the file and statistic so just set the value in the table column:
                       		// - use the column numbers determined above
                           	records.get(0).setFieldValue(fileColumn, InputFile);
                           	records.get(0).setFieldValue(statisticColumn, Statistic);
                           	records.get(0).setFieldValue(statisticValueColumn, statisticInt);
                       	}
                       	else {
                           	// There is no matching row in the table so add a new row.
                           	int filenameColumn = table.getFieldIndex(TableFilenameColumn);
                           	table.addRecord(table.emptyRecord().
                           		setFieldValue(filenameColumn, InputFile).
                           		setFieldValue(statisticColumn, Statistic).
                               	setFieldValue(statisticValueColumn, statisticInt));
                       	}
                   	}
                   	
                   	// Do the check by comparing to the statistic.
              	   	List<String> problems = new ArrayList<>();
               	   	// This is similar to TSUtil_CheckTimeSeries but it only needs to check the one statistic
               	   	// value and therefore is much simpler... so include the code in this class for now.
               	   	// For now all statistics are integers.
               	   	boolean ifCriteriaMet = checkStatistic ( statisticInt, checkCriteria,
                   	   	CheckValue1_Integer, CheckValue2_Integer,
                   	   	IfCriteriaMet, ProblemType, problems );
               	   	if ( ifCriteriaMet ) {
                       	// Generate a warning.
                       	CommandStatusType commandStatusType = CommandStatusType.WARNING;
                       	if ( IfCriteriaMet.equals(_Fail) ) {
                           	commandStatusType = CommandStatusType.FAILURE;
                       	}
                       	StringBuffer b = new StringBuffer();
                       	b.append ( "Statistic " + Statistic + " (" + statisticInt + ") meets criteria " + CheckCriteria );
                       	if ( (CheckValue1 != null) && !CheckValue1.equals("") ) {
                           	b.append ( " " + CheckValue1 );
                       	}
                       	if ( (CheckValue2 != null) && !CheckValue2.equals("") ) {
                           	b.append ( ", " + CheckValue2 );
                       	}
                       	b.append ( " for file: " + InputFile_full );
                       	// Add an extra bit of information based on a common mistake.
                       	if ( Statistic.equalsIgnoreCase(this._PatternMatchLineCount) && (statisticInt == 0) ) {
                    	   	b.append("\nIf zero (0) is not expected, may need to use wildcard * in pattern to match substring in each line.");
                       	}
                       	Message.printWarning ( warning_level,
                           	MessageUtil.formatMessageTag(command_tag,++warning_count),routine,b.toString() );
                       	if ( !IfCriteriaMet.equalsIgnoreCase(_Ignore) ) {
                           	status.addToLog ( commandPhase, new CommandLogRecord(commandStatusType,
                               	ProblemType, b.toString(), "File should be treated accordingly." ) );
                       	}

                       	// Set the table in the processor.
            
                       	if ( newTable ) {
                    	   	request_params = new PropList ( "" );
                    	   	request_params.setUsingObject ( "Table", table );
                    	   	try {
                        	   	processor.processRequest( "SetTable", request_params);
                    	   	}
                    	   	catch ( Exception e ) {
                        	   	message = "Error requesting SetTable(Table=...) from processor.";
                        	   	Message.printWarning(warning_level,
                            	   	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            	   	routine, message );
                        	   	status.addToLog ( commandPhase,
                            	   	new CommandLogRecord(CommandStatusType.FAILURE,
                            		   	message, "Report problem to software support." ) );
                    	   	}
                       	}

                       	// Set the requested processor property.
                       	if ( (CheckResultPropertyName != null) && !CheckResultPropertyName.isEmpty() ) {
                    	   	// Allow null and empty value, but following commands will probably have issues.
       	   	               	PropList requestParams = new PropList ( "" );
       	   	               	requestParams.setUsingObject ( "PropertyName", CheckResultPropertyName );
       	   	               	requestParams.setUsingObject ( "PropertyValue", CriteriaMetPropertyValue );
       	   	               	try {
           	   	               	processor.processRequest( "SetProperty", requestParams);
       	   	               	}
       	   	               	catch ( Exception e ) {
           	   	               	message = "Error requesting SetProperty(" + CheckResultPropertyName + "=\""
           	   	            	   	+ CriteriaMetPropertyValue + "\") from processor.";
           	   	               	Message.printWarning(log_level,
               	   	               	MessageUtil.formatMessageTag( command_tag, ++warning_count),
               	   	               	routine, message );
           	   	               	status.addToLog ( commandPhase,
               	   	               	new CommandLogRecord(CommandStatusType.FAILURE,
                   		                  	message, "Report the problem to software support." ) );
      	   	               	}
                       	}
                   	}
                   	else {
                	   	// Add to the log as info to confirm check was evaluated properly.
                       	StringBuilder b = new StringBuilder("Statistic " + Statistic + " (" + statisticInt + ") DOES NOT meet criteria " + CheckCriteria);
                       	if ( (CheckValue1 != null) && !CheckValue1.equals("") ) {
                           	b.append ( " " + CheckValue1 );
                       	}
                       	if ( (CheckValue2 != null) && !CheckValue2.equals("") ) {
                           	b.append ( ", " + CheckValue2 );
                       	}
                       	b.append ( " for file: " + InputFile_full );
      	               	status.addToLog ( commandPhase,
           	               	new CommandLogRecord(CommandStatusType.INFO,
        	                  	b.toString(), "No action will be taken." ) );
                	   	// Set the property if provided.
                       	if ( (CheckResultPropertyName != null) && !CheckResultPropertyName.isEmpty() ) {
                    	   	// Allow null and empty value, but following commands will probably have issues.
       	   	               	PropList requestParams = new PropList ( "" );
       	   	               	requestParams.setUsingObject ( "PropertyName", CheckResultPropertyName );
       	   	               	requestParams.setUsingObject ( "PropertyValue", CriteriaNotMetPropertyValue );
       	   	               	try {
           	   	               	processor.processRequest( "SetProperty", requestParams);
       	   	               	}
       	   	               	catch ( Exception e ) {
           	   	               	message = "Error requesting SetProperty(" + CheckResultPropertyName + "=\""
           	   	            	   	+ CriteriaNotMetPropertyValue + "\") from processor.";
           	   	               	Message.printWarning(log_level,
               	   	               	MessageUtil.formatMessageTag( command_tag, ++warning_count),
               	   	               	routine, message );
           	   	               	status.addToLog ( commandPhase,
               	   	               	new CommandLogRecord(CommandStatusType.FAILURE,
                   		                  	message, "Report the problem to software support." ) );
      	   	               	}
                       	}
                   	}
                   	int problemsSize = problems.size();
                   	for ( int iprob = 0; iprob < problemsSize; iprob++ ) {
                       	message = problems.get(iprob);
                       	Message.printWarning ( warning_level,
                           	MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                   	   	// No recommendation since it is a user-defined check.
                       	// FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
                       	status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.WARNING, ProblemType, message, "" ) );
                   	}
               	}
               	catch ( Exception e ) {
                   	message = "Unexpected error checking file \""+ InputFile + " (" + e + ").";
                   	Message.printWarning ( warning_level,
                       	MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                   	Message.printWarning(3,routine,e);
                   	status.addToLog ( commandPhase,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
               	}
           	}
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error checking file (" + e + ").";
        Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check log file for details." ) );
        throw new CommandException ( message );
    }
    
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
        throw new CommandWarningException ( message );
    }
    
    status.refreshPhaseSeverity(commandPhase, CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __discoveryTable = table;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
    String [] parameterOrder = { 
    	"InputFile",
    	"IfNotFound",
    	"Statistic",
    	"SearchPattern",
    	"CheckCriteria",
    	"CheckValue1",
    	"CheckValue2",
    	"IfCriteriaMet",
    	"ProblemType",
    	"CheckResultPropertyName",
    	"CriteriaMetPropertyValue",
    	"CriteriaNotMetPropertyValue",
    	"TableID",
    	"TableFilenameColumn",
    	"TableStatisticColumn",
    	"TableStatisticValueColumn"
    };
	return this.toString(parameters, parameterOrder);
}

}