// CheckFile_Command - This class initializes, checks, and runs the CheckFile() command.

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
import RTi.Util.IO.Command;
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
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the CheckFile() command.
*/
public class CheckFile_Command extends AbstractCommand implements Command
{

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
        // Make sure that the statistic is known in general
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
       
        // Additional checks that depend on the statistic
        
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
        // Make sure that it is in the supported list
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

    // Check for invalid parameters...
    List<String> validList = new ArrayList<>(15);
    validList.add ( "InputFile" );
    validList.add ( "IfNotFound" );
    validList.add ( "Statistic" );
    validList.add ( "SearchPattern" );
    validList.add ( "TableID" );
    validList.add ( "TableFilenameColumn" );
    validList.add ( "TableStatisticColumn" );
    validList.add ( "CheckCriteria" );
    validList.add ( "CheckValue1" );
    validList.add ( "CheckValue2" );
    validList.add ( "IfCriteriaMet" );
    validList.add ( "ProblemType" );
    validList.add ( "CheckResultPropertyName" );
    validList.add ( "CriteriaMetPropertyValue" );
    validList.add ( "CriteriaNotMetPropertyValue" );
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
Check the time series statistic.
@param ts the time series to check
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
        // Statistic was not computed so this is definitely an error
        problems.add ( "Statistic was not computed - unable to check its value." );
        return false;
    }
    boolean meetsCriteria = false;
    if ( statisticValue instanceof Integer ) {
        // Do comparisons on integers
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
        // Don't know how to handle
        problems.add ( "Statistic is not a floating point number or integer - unable to check its value." );
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
            (TSCommandProcessor)getCommandProcessor(), this);
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
 * Get the statistics choices for use in the editor.
 */
public List<String> getStatisticChoicesAsStrings() {
	List<String> choices = new ArrayList<>();
	choices.add(this._FileSizeBytes);
	choices.add(this._FileSizeLines);
	choices.add(this._PatternMatchLineCount);
	return choices;
}

// Parse command is in the base class

/**
Run the command.
@param command_number Number of command in sequence.
@exception Exception if there is an error processing the command.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{   String message, routine = getClass().getSimpleName() + ".runCommand";
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    int log_level = 3; // Level for non-user messages for log file.
    
    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    PropList parameters = getCommandParameters();
    
    // Get the input parameters...

	String InputFile = parameters.getValue ( "InputFile" );
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	if ( (IfNotFound == null) || IfNotFound.equals("")) {
	    IfNotFound = _Warn; // Default
	}
    String Statistic = parameters.getValue ( "Statistic" );
    String SearchPattern = parameters.getValue ( "SearchPattern" );
    String TableID = parameters.getValue ( "TableID" );
    String TableFilenameColumn = parameters.getValue ( "TableFilenameColumn" );
    String TableStatisticColumn = parameters.getValue ( "TableStatisticColumn" );
    String CheckCriteria = parameters.getValue ( "CheckCriteria" );
    CheckType checkCriteria = CheckType.valueOfIgnoreCase(CheckCriteria);
    String CheckValue1 = parameters.getValue ( "CheckValue1" );
    Integer CheckValue1_Integer = null;
    if ( (CheckValue1 != null) && !CheckValue1.equals("") ) {
        CheckValue1_Integer = new Integer(CheckValue1);
    }
    String CheckValue2 = parameters.getValue ( "CheckValue2" );
    Integer CheckValue2_Integer = null;
    if ( (CheckValue2 != null) && !CheckValue2.equals("") ) {
        CheckValue2_Integer = new Integer(CheckValue2);
    }
    String IfCriteriaMet = parameters.getValue ( "IfCriteriaMet" );
    if ( (IfCriteriaMet == null) || IfCriteriaMet.equals("") ) {
        IfCriteriaMet = _Warn; // Default
    }
    String ProblemType = parameters.getValue ( "ProblemType" );
    if ( (ProblemType == null) || ProblemType.equals("") ) {
        ProblemType = Statistic + "-" + CheckCriteria; // Default
    }
    String CheckResultPropertyName = parameters.getValue ( "CheckResultPropertyName" );
    String CriteriaMetPropertyValue = parameters.getValue ( "CriteriaMetPropertyValue" );
    String CriteriaNotMetPropertyValue = parameters.getValue ( "CriteriaNotMetPropertyValue" );

    PropList request_params = new PropList ( "" );
    CommandProcessorRequestResultsBean bean = null;
    
    DataTable table = null;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be updated
        request_params = new PropList ( "" );
        request_params.set ( "TableID", TableID );
        try {
            bean = processor.processRequest( "GetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table == null ) {
            message = "Unable to find table to process using TableID=\"" + TableID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a table exists with the requested ID." ) );
        }
        else {
            table = (DataTable)o_Table;
        }
    }

	String InputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
        	TSCommandProcessorUtil.expandParameterValue(processor, this,InputFile) ) );
    File file = new File ( InputFile_full );
	if ( !file.exists() ) {
        message = "File to remove \"" + InputFile_full + "\" does not exist.";
        if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the file exists at the time the command is run."));
        }
        else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
            Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
                    message, "Verify that the file exists at the time the command is run."));
        }
        else {
            Message.printStatus( 2, routine, message + "  Ignoring.");
        }
	}
    
    if ( warning_count > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }
    
    // Now process...
    
    try {
    	// TODO smalers 2021-07-24 loop in case later want to allow more than one file to be processed
    	int nfiles = 1;
        for ( int ifile = 0; ifile < nfiles; ifile++ ) {
            // The the time series to process, from the list that was returned above.
            
            try {
                // Do the statistic calculation...
                notifyCommandProgressListeners ( ifile, nfiles, (float)-1.0, "Checking statistic for " +
                    InputFile_full );
                Integer statisticInt = null;
               	File f = new File(InputFile_full);
                if ( Statistic.equalsIgnoreCase(this._FileSizeBytes) ) {
                	statisticInt = new Integer((int)f.length());
                }
                else if ( Statistic.equalsIgnoreCase(this._FileSizeLines) ) {
                	statisticInt = IOUtil.lineCount(f);
                }
                else if ( Statistic.equalsIgnoreCase(this._PatternMatchLineCount) ) {
                	// Convert the search pattern to Java regular expression.
                	statisticInt = IOUtil.matchCount(f, SearchPattern.replace("*", ".*"), true);
                }
                // Now set in the table
                if ( (TableID != null) && !TableID.equals("") ) {
                    if ( (TableStatisticColumn != null) && !TableStatisticColumn.equals("") ) {
                        // See if a matching row exists using the specified TSID column...
                        String tsid = null;
                        // TODO smalers 2021-07-24 Filename is currently the relative path, may add modifier function to convert to other value.
                        Message.printStatus(2,routine, "Searching column \"" + TableFilenameColumn + "\" for \"" +
                            InputFile + "\"" );
                        TableRecord rec = table.getRecord ( TableFilenameColumn, tsid );
                        Message.printStatus(2,routine, "Searched column \"" + TableFilenameColumn + "\" for \"" +
                            tsid + "\" ... found " + rec );
                        int statisticColumn = -1;
                        try {
                            statisticColumn = table.getFieldIndex(TableStatisticColumn);
                        }
                        catch ( Exception e2 ) {
                            // Automatically add to the table, initialize with null (not nonValue).
                        	// TODO smalers 2021-07-24 may need to use double.
                            table.addField(new TableField(TableField.DATA_TYPE_INT,TableStatisticColumn,-1,-1), null );
                            statisticColumn = table.getFieldIndex(TableStatisticColumn);
                        }
                        if ( rec != null ) {
                            // There is already a row for the TSID so just set the value in the table column...
                            rec.setFieldValue(statisticColumn, statisticInt);
                        }
                        else {
                            // There is no row in the table for the time series so add a row to the table...
                            int filenameColumn = table.getFieldIndex(TableFilenameColumn);
                            table.addRecord(table.emptyRecord().setFieldValue(filenameColumn, InputFile).
                                setFieldValue(statisticColumn, statisticInt));
                        }
                    }
                }
                
                // Do the check by comparing to the statistic...
                List<String> problems = new ArrayList<>();
                // This is similar to TSUtil_CheckTimeSeries but it only needs to check the one statistic
                // value and therefore is much simpler... so include the code in this class for now
                boolean ifCriteriaMet = checkStatistic ( statisticInt, checkCriteria,
                    CheckValue1_Integer, CheckValue2_Integer,
                    IfCriteriaMet, ProblemType, problems );
                if ( ifCriteriaMet ) {
                    // Generate a warning
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
                    b.append ( " for file " + InputFile_full );
                    Message.printWarning ( warning_level,
                        MessageUtil.formatMessageTag(command_tag,++warning_count),routine,b.toString() );
                    if ( !IfCriteriaMet.equalsIgnoreCase(_Ignore) ) {
                        status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(commandStatusType,
                            ProblemType, b.toString(), "File should be treated accordingly." ) );
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
           	   	            status.addToLog ( CommandPhaseType.RUN,
               	   	            new CommandLogRecord(CommandStatusType.FAILURE,
                   		               message, "Report the problem to software support." ) );
      	   	            }
                    }
                }
                else {
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
           	   	            status.addToLog ( CommandPhaseType.RUN,
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
                    // No recommendation since it is a user-defined check
                    // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
                    status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.WARNING, ProblemType, message, "" ) );
                }
            }
            catch ( Exception e ) {
                message = "Unexpected error checking file \""+ InputFile + " (" + e + ").";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
                Message.printWarning(3,routine,e);
                status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for details - report the problem to software support." ) );
            }
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error checking time series (" + e + ").";
        Message.printWarning ( warning_level, 
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check log file for details." ) );
        throw new CommandException ( message );
    }
    
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
        throw new CommandWarningException ( message );
    }
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
@param parameters parameters for the command.
*/
public String toString ( PropList parameters )
{   
    if ( parameters == null ) {
        return getCommandName() + "()";
    }
    
    String InputFile = parameters.getValue( "InputFile" );
    String IfNotFound = parameters.getValue( "IfNotFound" );
    String Statistic = parameters.getValue( "Statistic" );
    String SearchPattern = parameters.getValue( "SearchPattern" );
    String TableID = parameters.getValue ( "TableID" );
    String TableFilenameColumn = parameters.getValue ( "TableFilenameColumn" );
    String TableStatisticColumn = parameters.getValue ( "TableStatisticColumn" );
    String CheckCriteria = parameters.getValue( "CheckCriteria" );
    String CheckValue1 = parameters.getValue( "CheckValue1" );
    String CheckValue2 = parameters.getValue( "CheckValue2" );
    String IfCriteriaMet = parameters.getValue( "IfCriteriaMet" );
    String ProblemType = parameters.getValue( "ProblemType" );
    String CheckResultPropertyName = parameters.getValue( "CheckResultPropertyName" );
    String CriteriaMetPropertyValue = parameters.getValue( "CriteriaMetPropertyValue" );
    String CriteriaNotMetPropertyValue = parameters.getValue( "CriteriaNotMetPropertyValue" );
        
    StringBuffer b = new StringBuffer ();

    if ( (InputFile != null) && (InputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputFile=\"" + InputFile + "\"" );
    }
    if ( (IfNotFound != null) && (IfNotFound.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IfNotFound=\"" + IfNotFound + "\"" );
    }
    if ( (Statistic != null) && (Statistic.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Statistic=\"" + Statistic + "\"" );
    }
    if ( (SearchPattern != null) && (SearchPattern.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SearchPattern=\"" + SearchPattern + "\"" );
    }
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (TableFilenameColumn != null) && (TableFilenameColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableFilenameColumn=\"" + TableFilenameColumn + "\"" );
    }
    if ( (TableStatisticColumn != null) && (TableStatisticColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableStatisticColumn=\"" + TableStatisticColumn + "\"" );
    }
    if ( (CheckCriteria != null) && (CheckCriteria.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CheckCriteria=\"" + CheckCriteria + "\"" );
    }
    if ( (CheckValue1 != null) && (CheckValue1.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CheckValue1=" + CheckValue1 );
    }
    if ( (CheckValue2 != null) && (CheckValue2.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CheckValue2=" + CheckValue2 );
    }
    if ( (IfCriteriaMet != null) && (IfCriteriaMet.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IfCriteriaMet=" + IfCriteriaMet );
    }
    if ( (ProblemType != null) && (ProblemType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ProblemType=\"" + ProblemType + "\"" );
    }
    if ( (CheckResultPropertyName != null) && (CheckResultPropertyName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CheckResultPropertyName=\"" + CheckResultPropertyName + "\"" );
    }
    if ( (CriteriaMetPropertyValue != null) && (CriteriaMetPropertyValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CriteriaMetPropertyValue=\"" + CriteriaMetPropertyValue + "\"" );
    }
    if ( (CriteriaNotMetPropertyValue != null) && (CriteriaNotMetPropertyValue.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CriteriaNotMetPropertyValue=\"" + CriteriaNotMetPropertyValue + "\"" );
    }
    
    return getCommandName() + "(" + b.toString() + ")";
}

}