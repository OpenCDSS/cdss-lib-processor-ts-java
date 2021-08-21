// CompareTimeSeries_Command - This class initializes, checks, and runs the CompareTimeSeries() command.

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

package rti.tscommandprocessor.commands.ts;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSEnsemble;
import RTi.TS.TSIterator;
import RTi.TS.TSUtil;
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
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the CompareTimeSeries() command.
*/
public class CompareTimeSeries_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

/**
Data members used for parameter values.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
The table that is created for discovery mode.
*/
private DataTable __discoveryTable = null;

/**
 * Output table columns, listed in order of the table columns.
 */
private int __tableDateTimeColumnNum = -1;
private int __tableTSID1ColumnNum = -1;
private int __tableTSID2ColumnNum = -1;
private int __tableValue1ColumnNum = -1;
private int __tableFlag1ColumnNum = -1;
private int __tableValue2ColumnNum = -1;
private int __tableFlag2ColumnNum = -1;
private int __tableDiffColumnNum = -1;
private int __tableDiffPercentColumnNum = -1;
private int __tableCommentColumnNum = -1;

/**
Constructor.
*/
public CompareTimeSeries_Command ()
{	super();
	setCommandName ( "CompareTimeSeries" );
}


/**
Add a table record for output difference.
*/
private void addTableRecord ( DataTable table, DateTime dt, TS ts1, TS ts2,
	double value1, String flag1, double value2, String flag2, double diff, String comment ) {
	// Save the results to the table
	// Add a record to the table
	TableRecord rec = table.emptyRecord();
	try {
		if ( __tableDateTimeColumnNum >= 0 ) {
			// Make a copy since iterating
			rec.setFieldValue(__tableDateTimeColumnNum, new DateTime(dt));
		}
		if ( __tableTSID1ColumnNum >= 0 ) {
			String tsid1 = ts1.getAlias();
			if ( tsid1.isEmpty() ) {
				tsid1 = ts1.getIdentifierString();
			}
			rec.setFieldValue(__tableTSID1ColumnNum, tsid1);
		}
		if ( __tableTSID2ColumnNum >= 0 ) {
			String tsid2 = ts2.getAlias();
			if ( tsid2.isEmpty() ) {
				tsid2 = ts2.getIdentifierString();
			}
			rec.setFieldValue(__tableTSID2ColumnNum, tsid2);
		}
		if ( __tableValue1ColumnNum >= 0 ) {
			rec.setFieldValue(__tableValue1ColumnNum, value1);
		}
		if ( __tableFlag1ColumnNum >= 0 ) {
			rec.setFieldValue(__tableFlag1ColumnNum, flag1);
		}
		if ( __tableValue2ColumnNum >= 0 ) {
			rec.setFieldValue(__tableValue2ColumnNum, value2);
		}
		if ( __tableFlag2ColumnNum >= 0 ) {
			rec.setFieldValue(__tableFlag2ColumnNum, flag2);
		}
		if ( __tableDiffColumnNum >= 0 ) {
			rec.setFieldValue(__tableDiffColumnNum, diff);
		}
		if ( __tableDiffPercentColumnNum >= 0 ) {
			if ( !ts1.isDataMissing(value1) && !ts2.isDataMissing(value2) ) {
				rec.setFieldValue(__tableDiffPercentColumnNum, 100*(value2 - value1)/value1);
			}
		}
		if ( __tableCommentColumnNum >= 0 ) {
			rec.setFieldValue(__tableCommentColumnNum, comment);
		}
		table.addRecord(rec);
	}
	catch ( Exception e ) {
		// For now swallow
	}
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
{	String MatchLocation = parameters.getValue ( "MatchLocation" );
	String MatchDataType = parameters.getValue ( "MatchDataType" );
	String Precision = parameters.getValue ( "Precision" );
	String Tolerance = parameters.getValue ( "Tolerance" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String DiffFlag = parameters.getValue ( "DiffFlag" );
	String CreateDiffTS = parameters.getValue ( "CreateDiffTS" );
	String WarnIfDifferent = parameters.getValue ( "WarnIfDifferent" );
	String WarnIfSame = parameters.getValue ( "WarnIfSame" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (MatchLocation != null) && !MatchLocation.equals("") ) {
		if ( !MatchLocation.equals(_False) && !MatchLocation.equals(_True) ) {
            message = "The MatchLocation parameter \"" + MatchLocation + "\" if specified must be False or True.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify False or True (or do not specify to use default of True)." ) );
		}
	}
	if ( (MatchDataType != null) && !MatchDataType.equals("") ) {
		if ( !MatchDataType.equals(_False) && !MatchDataType.equals(_True) ) {
            message = "The MatchDataType parameter \"" + MatchDataType + "\" if specified must be False or True.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify False or True (or do not specify to use default of False)." ) );
		}
	}
	if ( (Precision != null) && !Precision.equals("") ) {
		if ( !StringUtil.isInteger(Precision) ) {
            message = "The precision: \"" + Precision + "\" is not an integer.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the precision as an integer (or blank to not round)." ) );
            
		}
		if ( StringUtil.atoi(Precision) < 0 ) {
            message = "The precision: \"" + Precision + "\" must be >= 0.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the precision as an integer >= 0 (or blank to not round)." ) );
		}
	}
	if ( (Tolerance != null) && !Tolerance.equals("") ) {
		List<String> v = StringUtil.breakStringList(Tolerance,", ",0);
		int size = 0;
		if ( v != null ) {
			size = v.size();
		}
		// Make sure that each tolerance is a number...
		String string;
		for ( int i = 0; i < size; i++ ) {
			string = v.get(i);
			if ( !StringUtil.isDouble(string) ) {
                message = "The tolerance: \"" + string + "\" is not a number.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the tolerance as a number." ) );
                
			}
			if ( StringUtil.atod(string) < 0.0 ) {
                message = "The tolerance: \"" + Tolerance + "\" must be >= 0.0.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify the tolerance as a number >= 0.0." ) );
			}
		}
	}
	if ( (AnalysisStart != null) && !AnalysisStart.equals("") &&
		!AnalysisStart.equalsIgnoreCase("OutputStart") && (AnalysisStart.indexOf("${") < 0) ) {
		try {
		    DateTime.parse(AnalysisStart);
		}
		catch ( Exception e ) {
            message = "The analysis start date/time \"" + AnalysisStart +"\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputStart." ) );
		}
	}
	if ( (AnalysisEnd != null) && !AnalysisEnd.equals("") && !AnalysisEnd.equalsIgnoreCase("OutputEnd") && (AnalysisEnd.indexOf("${") < 0)) {
		try {
		    DateTime.parse( AnalysisEnd);
		}
		catch ( Exception e ) {
            message = "The dependent end date/time \"" + AnalysisEnd + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time or OutputEnd." ) );
		}
	}
	if ( (DiffFlag != null) && (DiffFlag.length() != 1) ) {
        message = "The difference flag \"" + DiffFlag + "\" must be 1 character long.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a 1-character value for the flag." ) );
	}
	if ( (CreateDiffTS != null) && !CreateDiffTS.equals("") ) {
		if ( !CreateDiffTS.equals(_False) &&	!CreateDiffTS.equals(_True) ) {
            message = "The CreateDiffTS parameter \"" + CreateDiffTS + "\" must be False or True.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify False or True (or blank for default of False)." ) );
		}
	}
	if ( (WarnIfDifferent != null) && !WarnIfDifferent.equals("") ) {
		if ( !WarnIfDifferent.equals(_False) &&	!WarnIfDifferent.equals(_True) ) {
            message = "The WarnIfDifferent parameter \"" + WarnIfDifferent + "\" must be False or True.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify False or True (or blank for default of False)." ) );
		}
	}
	if ( (WarnIfSame != null) && !WarnIfSame.equals("") ) {
		if ( !WarnIfSame.equals(_False) && !WarnIfSame.equals(_True) ) {
            message = "The WarnIfSame parameter \"" + WarnIfSame + "\" must be False or True.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify False or True (or blank for default of False)." ) );
		}
	}
	// At least one of WarnIfDifferent or WarnIfSame should be True - otherwise the command does nothing and probably a mistake
	if ( WarnIfSame == null ) {
		WarnIfSame = _False; // Default
	}
	if ( WarnIfDifferent == null ) {
		WarnIfDifferent = _False; // Default
	}
	if ( !WarnIfSame.equals(_True) && !WarnIfDifferent.equals(_True)) {
        message = "At lease one of WarnIfDifferent or WarnIfSame must be True.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the values of WarnIfDifferent and WarnIfSame." ) );
	}
    
	// Check for invalid parameters...
	List<String> validList = new ArrayList<>(17);
	validList.add ( "TSID1" );
	validList.add ( "TSID2" );
	validList.add ( "EnsembleID1" );
	validList.add ( "EnsembleID2" );
	validList.add ( "MatchLocation" );
	validList.add ( "MatchDataType" );
	validList.add ( "Precision" );
	validList.add ( "Tolerance" );
	validList.add ( "CompareFlags" );
	validList.add ( "AnalysisStart" );
	validList.add ( "AnalysisEnd" );
	validList.add ( "DiffFlag" );
	validList.add ( "CreateDiffTS" );
    validList.add ( "TableID" );
    validList.add ( "DiffCountProperty" );
	validList.add ( "WarnIfDifferent" );
	validList.add ( "WarnIfSame" );
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
 * Compare time series values for two time series and generate output.
 * This method is reused depending on how time series are iterated.
 * @param Precision precision to round values (ignored if precision is < 0)
 */
private void compareTimeSeriesValues (
	String routine,
	TS ts1, String loc1, TS ts2,
	boolean compareFlags, String DiffFlag, String Precision, int Precision_int, String value_format, int Tolerance_count, double [] Tolerance_double,
	DateTime date, double value1_orig, TSData tsdata1, String flag1, double value2_orig, TSData tsdata2, String flag2,
	int [] diffcount, double [] difftotal, double [] difftotalabs,
	boolean doCreateDiffTS, TS diffts,
	boolean doTable, DataTable table,
	DiffStats diffStats) {

	double value1, value2;
	// Assume flags that are null are equivalent to empty flag
	if ( flag1 == null ) {
		flag1 = "";
	}
	if ( flag2 == null ) {
		flag2 = "";
	}
	if ( Precision != null ) {
		// Need to round.  For now do with strings, which handles the rounding...
    	if ( Double.isNaN(value1_orig)) {
        	value1 = value1_orig;
    	}
		else {
			value1 = Double.parseDouble(StringUtil.formatString(value1_orig,"%."+Precision_int +"f"));
		}
		if ( Double.isNaN(value2_orig)) {
			value2 = value2_orig;
		}
		else {
			value2 = Double.parseDouble(StringUtil.formatString(value2_orig,"%."+Precision_int +"f"));
		}
	}
	else {
	   	value1 = value1_orig;
		value2 = value2_orig;
	}
	// For troubleshooting...
	//Message.printStatus(2,routine,"Value1="+ value1_orig + " Value2="+ value2_orig);
	// Count of data points that are checked...
	++diffStats.totalcount;
	boolean is_diff = false; // Initialize, set to true below if difference detected
	if ( ts1.isDataMissing(value1_orig) && !ts2.isDataMissing(value2_orig)) {
		// ts1 is missing and ts2 is not missing
		Message.printStatus ( 2, routine, loc1 + " has different data on " + date +
			" TS1 = missing " + flag1 +
			" TS2 = " + StringUtil.formatString(value2,value_format) + " " + flag2 );
		String diffMessage = "TS1 missing, TS2 not missing";
		if ( compareFlags ) {
			// Also check flags
			if ( !flag1.equals(flag2) ) {
				diffMessage += ", flags differ";
			}
		}
		if ( doTable ) {
			addTableRecord ( table, date, ts1, ts2, value1_orig, flag1, value2_orig, flag2, Double.NaN, diffMessage );
		}
		// Indicate as missing at all levels...
		is_diff = true;
		for ( int it = 0; it < Tolerance_count; it++ ) {
			++diffcount[it];
		}
		if ( doCreateDiffTS ) {
			// ts2 has value so make that the difference...
			diffts.setDataValue ( date, value2 );
		}
	}
	else if(!ts1.isDataMissing(value1_orig) && ts2.isDataMissing(value2_orig)) {
		Message.printStatus ( 2, routine, loc1 + " has different data on " + date +
			" TS1 = " + StringUtil.formatString(value1,value_format) + " " + flag1 +
			" TS2 = missing " + flag2);
		String diffMessage = "TS1 not missing, TS2 missing";
		if ( compareFlags ) {
			// Also check flags
			if ( !flag1.equals(flag2) ) {
				diffMessage += ", flags differ";
			}
		}
		if ( doTable ) {
			addTableRecord ( table, date, ts1, ts2, value1_orig, flag1, value2_orig, flag2, Double.NaN, diffMessage );
		}
		// Indicate as missing at all levels...
		is_diff = true;
		for ( int it = 0; it < Tolerance_count; it++ ) {
			++diffcount[it];
		}
		if ( doCreateDiffTS ) {
			// ts1 has value so make that the difference (negative)...
			diffts.setDataValue ( date, -value2 );
		}
	}
	else if(ts1.isDataMissing(value1_orig) && ts2.isDataMissing(value2_orig)) {
		// Both missing so consider the values equal.
		if ( compareFlags ) {
			// Also check flags
			if ( !flag1.equals(flag2) ) {
				String diffMessage = "Both missing but flags differ";
				if ( doTable ) {
					addTableRecord ( table, date, ts1, ts2, value1_orig, flag1, value2_orig, flag2, Double.NaN, diffMessage );
				}
				is_diff = true;
				// No need to set value in diff time series since both missing
			}
		}
	}
	else {
	   	// Analyze differences for each tolerance...
		double diff = value2 - value1;
		double diffabs = Math.abs(diff);
		if ( doCreateDiffTS ) {
			// Set difference (regardless of tolerance).
			diffts.setDataValue ( date, diff );
		}
		if ( diffabs > diffStats.diffmaxabs ) {
			diffStats.diffmaxabs = diffabs;
			diffStats.diffmax = diff;
			diffStats.diffmax_DateTime = new DateTime ( date );
		}
		String flagDiffMessage = null;
		if ( compareFlags ) {
			// Also check flags
			if ( !flag1.equals(flag2) ) {
				flagDiffMessage = "flags differ";
			}
		}
		int diffCount = 0; // Count of differences for tolerance checks
		for ( int it = 0; it < Tolerance_count; it++ ) {
			if ( diffabs > Tolerance_double[it] ) {
				++diffCount;
    			// Report the difference...
    			Message.printStatus ( 2, routine, loc1 + " (tolerance=" + Tolerance_double[it] +
    				") has difference TS2-TS1 of " + StringUtil.formatString(diff,value_format) +
    				" on " + date + " TS2 = " + StringUtil.formatString(value2,value_format) + " " + flag1 +
    				" TS1 = " + StringUtil.formatString(value1,value_format) + " " + flag2 );
    			if ( doTable ) {
    				String diffMessage = "Diff > tolerance " + Tolerance_double[it];
    				if ( flagDiffMessage == null ) {
    					addTableRecord ( table, date, ts1, ts2, value1, flag1, value2, flag2, diff, diffMessage );
    				}
    				else {
    					addTableRecord ( table, date, ts1, ts2, value1, flag1, value2, flag2, diff, diffMessage + ", " + flagDiffMessage );
    				}
    			}
    			difftotal[it] += diff;
    			difftotalabs[it] += diffabs;
    			++diffcount[it];
    			is_diff = true;
			}
		}
		if ( (diffCount == 0) && (flagDiffMessage != null) ) {
			// No numerical value differences were detected but flags are different
   			addTableRecord ( table, date, ts1, ts2, value1, flag1, value2, flag2, diff, flagDiffMessage );
		}
	}
	if ( is_diff && (DiffFlag != null) ) {
		// Append to the data flag...
		TSData tsdata = new TSData(); // Data point from time series.
		tsdata = ts1.getDataPoint (date, tsdata);
		ts1.setDataValue ( date, value1_orig, tsdata.getDataFlag().trim() + DiffFlag, 1 );
		tsdata = ts2.getDataPoint (date, tsdata);
		ts2.setDataValue ( date, value2_orig, tsdata.getDataFlag().trim() + DiffFlag, 1 );
	}
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{   // The command will be modified if changed...
    List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
    return (new CompareTimeSeries_JDialog ( parent, this, tableIDChoices )).ok();
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
        v = new Vector<T>();
        v.add ( (T)table );
    }
    return v;
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
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
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_count = 0;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int log_level = 3; // Level for non-user messages
	int size = 0;
	
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
    
	PropList parameters = getCommandParameters();
	String TSID1 = parameters.getValue ( "TSID1" );
	if ( (TSID1 != null) && (TSID1.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		TSID1 = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID1);
	}
	String TSID2 = parameters.getValue ( "TSID2" );
	if ( (TSID2 != null) && (TSID2.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		TSID2 = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID2);
	}
	String EnsembleID1 = parameters.getValue ( "EnsembleID1" );
	if ( (EnsembleID1 != null) && (EnsembleID1.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		EnsembleID1 = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID1);
	}
	String EnsembleID2 = parameters.getValue ( "EnsembleID2" );
	if ( (EnsembleID2 != null) && (EnsembleID2.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		EnsembleID2 = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID2);
	}
	String MatchLocation = parameters.getValue ( "MatchLocation" );
	String MatchDataType = parameters.getValue ( "MatchDataType" );
	String Precision = parameters.getValue ( "Precision" ); // What to round data to before comparing.
	String Tolerance = parameters.getValue ( "Tolerance" );
	String CompareFlags = parameters.getValue ( "CompareFlags" );
	boolean compareFlags = false; // Default
	if ( (CompareFlags != null) && CompareFlags.equalsIgnoreCase("true") ) {
		compareFlags = true;
	}
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String DiffFlag = parameters.getValue ( "DiffFlag" );
	if ( (DiffFlag != null) && (DiffFlag.length() == 0) ) {
		// Set to null to indicate no flag in checks below...
		DiffFlag = null;
	}
	String CreateDiffTS = parameters.getValue ( "CreateDiffTS" );
	String TableID = parameters.getValue ( "TableID" );
	if ( (TableID != null) && (TableID.indexOf("${") >= 0) && (commandPhase == CommandPhaseType.RUN) ) {
		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
	}
	String DiffCountProperty = parameters.getValue ( "DiffCountProperty" );
	String WarnIfDifferent = parameters.getValue ( "WarnIfDifferent" );
	String WarnIfSame = parameters.getValue ( "WarnIfSame" );
	int Precision_int = 0; // The number of digits after the decimal to use for
						// comparisons.  If Precision is null, no rounding occurs.

	boolean MatchLocation_boolean = true;	// Default
	if ( (MatchLocation != null) && MatchLocation.equalsIgnoreCase(_False)){
		MatchLocation_boolean = false;
	}
	boolean MatchDataType_boolean = false;	// Default
	if ( (MatchDataType != null) && MatchDataType.equalsIgnoreCase(_True)){
		MatchDataType_boolean = true;
	}
	boolean doCreateDiffTS = false;	// Default
	if ( (CreateDiffTS != null) && CreateDiffTS.equalsIgnoreCase(_True)){
		doCreateDiffTS = true;
	}
	boolean WarnIfDifferent_boolean = false;	// Default
	if ( (WarnIfDifferent != null) && WarnIfDifferent.equalsIgnoreCase(_True)){
		WarnIfDifferent_boolean = true;
	}
	boolean WarnIfSame_boolean = false;	// Default
	if ( (WarnIfSame != null) && WarnIfSame.equalsIgnoreCase(_True)){
		WarnIfSame_boolean = true;
	}
	double [] Tolerance_double = null;
	String value_format = "%.6f";	// Default
	if ( (Precision != null) && !Precision.isEmpty() ) {
		Precision_int = Integer.parseInt ( Precision );
		value_format = "%." + Precision_int + "f";
	}
	int Tolerance_count = 0;
	List<String> Tolerance_tokens = null;
	if ( (Tolerance != null) && !Tolerance.isEmpty() ) {
		// The parameter has been specified as a list of one or more numbers...
		Tolerance_tokens = StringUtil.breakStringList(Tolerance,", ",0);
		if ( Tolerance_tokens != null ) {
			Tolerance_count = Tolerance_tokens.size();
		}
		if ( Tolerance_count > 0 ) {
			Tolerance_double = new double[Tolerance_count];
		}
		// Get each tolerance as a number...
		for ( int it = 0; it < Tolerance_count; it++ ) {
			Tolerance_double[it] = Double.parseDouble(Tolerance_tokens.get(it) );
		}
	}
	else {
	    // Default is tolerance of 0.0...
		Tolerance_count = 1;
		Tolerance_double = new double[1];
		Tolerance_double[0] = 0.0;
		Tolerance_tokens = new ArrayList<String>(1);
		Tolerance_tokens.add ("0");
	}

    // Figure out the dates to use for the analysis.
    // Default of null means to analyze the full period.
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
    
    // Get the table to process.  If null will create below.

    DataTable table = null;
    boolean doTable = false;
    if ( (TableID != null) && !TableID.equals("") ) {
        // Get the table to be used as input
    	doTable = true;
    	PropList request_params = new PropList ( "" );
        request_params.set ( "TableID", TableID );
        CommandProcessorRequestResultsBean bean = null;
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
        if ( o_Table != null ) {
            table = (DataTable)o_Table;
        }
    }
    
    if ( warning_count > 0 ) {
        // Input error...
        message = "Insufficient data to run command.";
        status.addToLog ( commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }
	
    // Now process
    
	List<TS> tslist = new ArrayList<TS>();
	List<TS> tslist2 = null; // Used when comparing 2 ensembles
	boolean do2Ts = false;
	boolean do2Ensembles = false;
	if ( commandPhase == CommandPhaseType.RUN ) {
		if ( (TSID1 != null) && !TSID1.isEmpty() && (TSID2 != null) && !TSID2.isEmpty() ) {
			do2Ts = true;
			TS ts = null;
			try {	PropList request_params = new PropList ( "" );
					request_params.set ( "CommandTag", command_tag );
					request_params.set ( "TSID", TSID1 );
					CommandProcessorRequestResultsBean bean = null;
					try {
					    bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
					}
					catch ( Exception e ) {
						message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSID1 + "\") from processor.";
						Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
						Message.printWarning(log_level, routine, e );
		                status.addToLog ( commandPhase,
		                        new CommandLogRecord(CommandStatusType.FAILURE,
		                                message, "Report the problem to software support." ) );
					}
					PropList bean_PropList = bean.getResultsPropList();
					Object o_TS = bean_PropList.getContents ( "TS");
					if ( o_TS == null ) {
						message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID1 + "\") from processor.";
						Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
		                status.addToLog ( commandPhase,
		                    new CommandLogRecord(CommandStatusType.FAILURE,
		                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
					}
					else {
						ts = (TS)o_TS;
					}
			}
			catch ( Exception e ) {
				ts = null;
			}
			if ( ts == null ) {
				message = "Unable to find time series to process using TSID \"" + TSID1 + "\".";
				Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count), routine, message );
		        status.addToLog ( commandPhase,
		            new CommandLogRecord(CommandStatusType.FAILURE,
		                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
				throw new CommandWarningException ( message );
			}
			else {
				tslist.add(ts);
			}
			// Get the second time series
			try {
				PropList request_params = new PropList ( "" );
					request_params.set ( "CommandTag", command_tag );
					request_params.set ( "TSID", TSID2 );
					CommandProcessorRequestResultsBean bean = null;
					try {
					    bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
					}
					catch ( Exception e ) {
						message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSID2 + "\") from processor.";
						Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
						Message.printWarning(log_level, routine, e );
			            status.addToLog ( commandPhase,
			                    new CommandLogRecord(CommandStatusType.FAILURE,
			                            message, "Report the problem to software support." ) );
					}
					PropList bean_PropList = bean.getResultsPropList();
					Object o_TS = bean_PropList.getContents ( "TS");
					if ( o_TS == null ) {
						message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID2 + "\") from processor.";
						Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message );
			            status.addToLog ( commandPhase,
			                new CommandLogRecord(CommandStatusType.FAILURE,
			                    message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
					}
					else {
						ts = (TS)o_TS;
					}
			}
			catch ( Exception e ) {
				ts = null;
			}
			if ( ts == null ) {
				message = "Unable to find time series to process using TSID \"" + TSID2 + "\".";
				Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count), routine, message );
			    status.addToLog ( commandPhase,
			        new CommandLogRecord(CommandStatusType.FAILURE,
			            message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
				throw new CommandWarningException ( message );
			}
			else {
				tslist.add(ts);
			}
		}
		else if ( (EnsembleID1 != null) && !EnsembleID1.isEmpty() && (EnsembleID2 != null) && !EnsembleID2.isEmpty() ) {
			// Get the two ensembles
			TSEnsemble tsensemble1 = null, tsensemble2 = null;
	        PropList request_params = new PropList ( "" );
	        request_params.set ( "CommandTag", command_tag );
	        request_params.set ( "EnsembleID", EnsembleID1 );
	        CommandProcessorRequestResultsBean bean = null;
	        try {
	            bean = processor.processRequest( "GetEnsemble", request_params );
	        }
	        catch ( Exception e ) {
	            message = "Error requesting GetEnsemble(EnsembleID=\"" + EnsembleID1 + "\") from processor.";
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
	            message = "Null ensemble requesting GetEnsemble(EnsembleID=\"" + EnsembleID1 + "\") from processor.";
	            Message.printWarning(log_level,
	                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                    routine, message );
	            status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
	        }
	        else {
	            tsensemble1 = (TSEnsemble)o_TSEnsemble;
	        }
	        if ( tsensemble1 == null ) {
	            message = "Unable to find ensemble to process using EnsembleID \"" + EnsembleID1 + "\".";
	            Message.printWarning ( warning_level,
	            MessageUtil.formatMessageTag(
	            command_tag,++warning_count), routine, message );
	            status.addToLog ( commandPhase,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
	            throw new CommandWarningException ( message );
	        }
	        // Get the second ensemble
	        request_params.set ( "CommandTag", command_tag );
	        request_params.set ( "EnsembleID", EnsembleID2 );
	        try {
	            bean = processor.processRequest( "GetEnsemble", request_params );
	        }
	        catch ( Exception e ) {
	            message = "Error requesting GetEnsemble(EnsembleID=\"" + EnsembleID2 + "\") from processor.";
	            Message.printWarning(log_level,
	                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                    routine, message );
	            status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Report the problem to software support." ) );
	        }
	        bean_PropList = bean.getResultsPropList();
	        o_TSEnsemble = bean_PropList.getContents ( "TSEnsemble");
	        if ( o_TSEnsemble == null ) {
	            message = "Null ensemble requesting GetEnsemble(EnsembleID=\"" + EnsembleID2 + "\") from processor.";
	            Message.printWarning(log_level,
	                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                    routine, message );
	            status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
	        }
	        else {
	            tsensemble2 = (TSEnsemble)o_TSEnsemble;
	        }
	        
	        if ( tsensemble2 == null ) {
	            message = "Unable to find ensemble to process using EnsembleID \"" + EnsembleID2 + "\".";
	            Message.printWarning ( warning_level,
	            MessageUtil.formatMessageTag(
	            command_tag,++warning_count), routine, message );
	            status.addToLog ( commandPhase,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Verify the ensemble identifier.  A previous error may also cause this problem." ) );
	            throw new CommandWarningException ( message );
	        }
			// Add the time series from the ensemble - then iterate through the list and get the matching time series from the second ensemble
			tslist = tsensemble1.getTimeSeriesList(false);
			tslist2 = tsensemble2.getTimeSeriesList(false);
			// Number of time series in ensembles must be the same
			if ( tslist.size() != tslist2.size() ) {
	            message = "Number of time series in first ensemble \"" + EnsembleID1 + "\" (" + tslist.size() +
	            	") is different from the second ensemble \"" + EnsembleID2 + "\" (" + tslist2.size() + ").";
	            Message.printWarning ( warning_level,
	            MessageUtil.formatMessageTag(
	            command_tag,++warning_count), routine, message );
	            status.addToLog ( commandPhase,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Verify that ensembles have the same number of time series." ) );
	            throw new CommandWarningException ( message );
			}
			do2Ensembles = true;
		}
		else {
			// Get all the time series
			try {
			    Object o = processor.getPropContents( "TSResultsList" );
				@SuppressWarnings("unchecked")
				List<TS> tslist0 = (List<TS>)o;
				tslist = tslist0;
			}
			catch ( Exception e ){
				message = "Error requesting TSResultsList from processor.";
				Message.printWarning ( warning_level,
					MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		        status.addToLog ( commandPhase,
		            new CommandLogRecord(CommandStatusType.FAILURE,
		                message, "Report to software support." ) );
			}
		}
		
		if ( (tslist == null) || (tslist.size() < 2) ) {
			message = "Number of matched time series is < 2.  Not comparing.";
			Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(command_tag, ++warning_count),
			routine, message );
	        status.addToLog ( commandPhase,
	            new CommandLogRecord(CommandStatusType.WARNING,
	                message, "Verify that specified time series exist." ) );
			throw new CommandException ( message );
		}
		// Check to make sure that the intervals are the same...
		List<TS> tslist2Check = new ArrayList<TS>();
		tslist2Check.addAll(tslist);
		if ( do2Ensembles ) {
			tslist2Check.addAll(tslist2);
		}
		if ( !TSUtil.areIntervalsSame(tslist2Check) ) {
			message = "Time series intervals are not consistent.  Not able to compare time series.";
			Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(command_tag, ++warning_count),
			routine, message );
	        status.addToLog ( commandPhase,
	              new CommandLogRecord(CommandStatusType.FAILURE,
	                     message, "Verify that the time series intervals are consistent." ) );
			throw new CommandException ( message );
		}
	}
	
	int [] diffcount = null; // Count of differences for each tolerance.
	double [] difftotal = new double[Tolerance_count]; // Total difference for each tolerance.
	double [] difftotalabs = new double[Tolerance_count]; // Total difference for each tolerance, absolute values.
	double [] diffabsavg = null;
	double [] diffavg = null;
	boolean is_diff = false; // Indicates whether a differences has been determined.
	boolean foundLoc1 = false; // Has the time series already been processed?
	boolean foundDatatype1 = false;
	boolean foundMatch = false;
	int tsdiff_count = 0; // The number of time series that are different.
	List<TS> compare_tslist = new ArrayList<TS>(); // Lists of data that were processed for the report.
	List<Integer> compare_numvalues = new ArrayList<Integer>();
	List<Double> compare_diffmax = new ArrayList<Double>();
	List<double []> compare_diffabsavg = new ArrayList<double []>();
	List<double []> compare_diffavg = new ArrayList<double []>();
	List<int []> compare_diffcount = new ArrayList<int []>();
	List<DateTime> compare_diffmaxdate = new ArrayList<DateTime>();
	List<TS> difftsList = new ArrayList<TS>(); // Output time series if CreateDiffTS=True
	int tsComparisonsTried = 0; // Count of the number of comparisons tried - warn if 0
	try {
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
           	if ( doTable ) {
	            if ( table == null ) {
	                // Did not find table so is being created in this command
	                // Create an empty table and set the ID
	                table = new DataTable();
	                table.setTableID ( TableID );
	                setDiscoveryTable ( table );
	            }
        	}
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
        	if ( doTable ) {
	            if ( table == null ) {
	                // Did not find the table above so create it
	                table = new DataTable( /*columnList*/ );
	                table.setTableID ( TableID );
	                setupOutputTable(table);
	                Message.printStatus(2, routine, "Was not able to match existing table \"" + TableID + "\" so created new table.");
	                
	                // Set the table in the processor...
	                PropList request_params = null;
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
        	}
	    // Loop through the time series.  For each time series, find its
		// matching time series, by location, ignoring itself.  When a match is found, do the comparison.
		TS ts1, ts2;
		TS diffts = null;	// Difference time series
		TSIterator tsi;
		if ( do2Ts ) {
			// Directly comparing so only need to process one comparison
			size = 1;
		}
		else {
			size = tslist.size();
		}
		double value1_orig; // Data values from each time series, rounded and original.
		double value2_orig;
		TSData tsdata1 = new TSData(), tsdata2 = new TSData(); // Data points for each time series.
		String flag1, flag2; // Data flags for values in each time series.
		DiffStats diffStats = new DiffStats();
		DateTime date1 = null, date2 = null; // Dates for iteration.
		int j;
		int it; // Counter for tolerances.
		String loc1, datatype1; // Location and data type for the first time series. 
		List<String> locList = new ArrayList<String>();	// Location/datatype
		List<String> datatypeList = new ArrayList<String>(); // pairs that have already been processed.
		if ( doCreateDiffTS ) {
			difftsList = new ArrayList<TS> ( size );
		}
		for ( int i = 0; i < size; i++ ) {
			diffts = null;
			ts1 = tslist.get(i);
			ts2 = null;
			loc1 = ts1.getLocation();
			datatype1 = ts1.getDataType();
			if ( do2Ts ) {
				// Only have two time series so always a match
				foundMatch = true;
				ts1 = tslist.get(0);
				ts2 = tslist.get(1);
			}
			else if ( do2Ensembles ) {
				// Get one time series from each ensemble
				foundMatch = true;
				ts1 = tslist.get(i);
				ts2 = tslist2.get(i);
			}
			else {
				// Try to pair up time series based on location and data type
				// Make sure not to analyze the same combination
				// more than once, based on the location ID and data type...
				// TODO SAM 2005-05-25 Also need to check interval always.
				foundMatch = false;
				boolean alreadyProcessed = false;
				for ( j = 0; j < locList.size(); j++ ) {
					// TODO SAM 2016-03-23 This needs review - is there a more robust way to deal with this?
					// These checks are to see if the location and data type have already been processed
					// Check the parts individually...
					foundLoc1 = false;
					foundDatatype1 = false;
					if ( loc1.equalsIgnoreCase( locList.get(j)) ) {
						foundLoc1 = true;
					}
					if ( datatype1.equalsIgnoreCase(datatypeList.get(j))) {
						foundDatatype1 = true;
					}
					// Now reset found_loc1 depending on whether the location and datatype (one or both) are
					// being matched...
					if ( MatchLocation_boolean && MatchDataType_boolean ) {
						// Need to check both...
						if ( foundLoc1 && foundDatatype1 ) {
							alreadyProcessed = true;
							break;
						}
					}
					else if ( MatchLocation_boolean && foundLoc1 ){
						alreadyProcessed = true;
						break;
					}
					else if ( MatchDataType_boolean && foundDatatype1 ) {
						alreadyProcessed = true;
						break;
					}
				}
				if ( alreadyProcessed ) {
					// Have already processed this time series...
					continue;
				}
				// The time series location and data type have not already been processed
				// Try to find a matching location, skipping the same instance and incompatible intervals...
				for ( j = 0; j < size; j++ ) {
					if ( i == j ) {
						// Don't compare a time series with itself
						continue;
					}
					ts2 = tslist.get(j);
					// Make sure that the interval is the same...
					if ( !((ts1.getDataIntervalBase() == ts2.getDataIntervalBase()) &&
						(ts1.getDataIntervalMult() == ts2.getDataIntervalMult())) ) {
						// Intervals do not match...
						continue;
					}
					// Check the requested information...
					if ( MatchLocation_boolean && !loc1.equalsIgnoreCase(ts2.getLocation())){
						// Not a match...
						continue;
					}
					if ( MatchDataType_boolean && !datatype1.equalsIgnoreCase(ts2.getDataType())){
						// Not a match...
						continue;
					}
					// Have a match
					foundMatch = true;
					break;
				}
			}
			if ( foundMatch ) {
				// Have a match so do the comparison.
				// Currently only compare the data values, not the header information or data flags.
				// Add locations and data types that have been processed so they are not found again
				locList.add ( loc1 );
				datatypeList.add ( datatype1 );
				// If the differences will be flagged, allocate the data flag space in both time series.
				if ( DiffFlag != null ) {
					ts1.allocateDataFlagSpace ( null, true ); // Retain previous.
					ts2.allocateDataFlagSpace ( null, true ); // Retain previous.
				}
				// If a difference time series is to be created, do it...
				if ( doCreateDiffTS ) {
					String diffid = "Diff_" + ts2.getIdentifier().toString();
					diffts = TSUtil.newTimeSeries ( diffid, true );
					diffts.copyHeader ( ts2 );
					diffts.setIdentifier ( diffid );
					if ( diffts.getAlias().isEmpty() ) {
						diffts.setAlias("Diff");
					}
					else {
						diffts.setAlias("Diff_" + diffts.getAlias());
					}
					diffts.allocateDataSpace();
					difftsList.add ( diffts );
				}
				// Initialize for the comparison...
				diffStats.totalcount = 0;
				diffStats.diffmaxabs = -1.0e10;
				// Reallocate in order to have unique references to save for the report...
				diffcount = new int[Tolerance_count];
				diffabsavg = new double[Tolerance_count];
				diffavg = new double[Tolerance_count];
				for ( it = 0; it < Tolerance_count; it++ ) {
					diffcount[it] = 0;
					difftotal[it] = (double)0.0;
					difftotalabs[it] = (double)0.0;
				}
				// Analysis period should encompass the period from both time series
				if ( AnalysisStart_DateTime == null ) {
					date1 = new DateTime (ts1.getDate1());
					if ( ts2.getDate1().lessThan(date1) ) {
						date1 = new DateTime( ts2.getDate1());
					}
				}
				else {
				    date1 = new DateTime ( AnalysisStart_DateTime);
				}
				if ( AnalysisEnd_DateTime == null ) {
					date2 = new DateTime (ts1.getDate2());
					if ( ts2.getDate2().greaterThan(date2)){
						date2 = new DateTime(ts2.getDate2());
					}
				}
				else {
				    date2 = new DateTime ( AnalysisEnd_DateTime);
				}
				Message.printStatus ( 2, routine, "TS1 = " + ts1.getIdentifier().toString(true) );
				Message.printStatus ( 2, routine, "TS2 = " + ts2.getIdentifier().toString(true) );
				Message.printStatus ( 2, routine, "Data differences TS2 - TS1 follow " +
					"(Tolerance=" + Tolerance + ", Period="+ date1 + " to " + date2 + "):" );
				// Increment counter indicating that a test was tried
				++tsComparisonsTried;
				if ( !TimeInterval.isRegularInterval(ts1.getDataIntervalBase()) ||
					!TimeInterval.isRegularInterval(ts2.getDataIntervalBase()) ) {
					// One or both of the time series are irregular.
					// It is difficult to iterate because timestamps may not align.
					// Therefore, retrieve all the date/times from each time series, sort them, and iterate using the overall list of date/times.
					List<TS> tempList = new ArrayList<>();
					tempList.add(ts1);
					tempList.add(ts2);
					List<DateTime> dateTimeList = TSUtil.createTSDateTimeList ( tempList, date1, date2 );
					tsi = ts1.iterator(date1,date2);
					for ( DateTime dt : dateTimeList ) {
						tsdata1 = ts1.getDataPoint ( dt, tsdata1 );
						value1_orig = tsdata1.getDataValue ();
						flag1 = tsdata1.getDataFlag().trim();
						tsdata2 = ts2.getDataPoint ( dt, tsdata2 );
						value2_orig = tsdata2.getDataValue ();
						flag2 = tsdata2.getDataFlag().trim();
						compareTimeSeriesValues(
							routine,
							ts1, loc1, ts2,
							compareFlags, DiffFlag, Precision, Precision_int, value_format, Tolerance_count, Tolerance_double,
							dt, value1_orig, tsdata1, flag1, value2_orig, tsdata2, flag2,
							diffcount, difftotal, difftotalabs,
							doCreateDiffTS, diffts,
							doTable, table,
							diffStats  );
					}
				}
				else {
					// Both time series are regular interval and should align
					// Iterate using the first time series...
					tsi = ts1.iterator(date1,date2);
					DateTime dt = null;
					for ( ; tsi.next() != null; ) {
						dt = tsi.getDate();
						// This is not overly efficient but currently the iterator does not have
						// a way to set a data point...
						tsdata1 = ts1.getDataPoint ( dt, tsdata1 );
						value1_orig = tsdata1.getDataValue ();
						flag1 = tsdata1.getDataFlag().trim();
						tsdata2 = ts2.getDataPoint ( dt, tsdata2 );
						value2_orig = tsdata2.getDataValue ();
						flag2 = tsdata2.getDataFlag().trim();
						compareTimeSeriesValues(
							routine,
							ts1, loc1, ts2,
							compareFlags, DiffFlag, Precision, Precision_int, value_format, Tolerance_count, Tolerance_double,
							dt, value1_orig, tsdata1, flag1, value2_orig, tsdata2, flag2,
							diffcount, difftotal, difftotalabs,
							doCreateDiffTS, diffts,
							doTable, table,
							diffStats );
					}
				}
				// Output status messages for summary of differences
				is_diff = false;
				for ( it = 0; it < Tolerance_count; it++ ) {
					if ( diffcount[it] > 0 ) {
						is_diff = true;
						diffabsavg[it] = difftotalabs[it]/diffcount[it];
						diffavg[it] = difftotal[it]/diffcount[it];
						Message.printStatus ( 2, routine, loc1 + " (tolerance=" + Tolerance_double[it] +
						") has " + diffcount[it] + " differences out of " + diffStats.totalcount + " values." );
						Message.printStatus ( 2, routine, loc1 + " Average difference (tolerance=" +
						Tolerance_double[it] + ")= " + StringUtil.formatString(diffavg[it],"%.6f") );
						Message.printStatus ( 2, routine, loc1 + " Average absolute difference (tolerance=" +
						Tolerance_double[it] + ")= " + StringUtil.formatString(diffabsavg[it],"%.6f") );
					}
				}
				if ( is_diff ) {
					Message.printStatus ( 2, routine, loc1 + " maximum difference = " +
					StringUtil.formatString(diffStats.diffmax, value_format) +" earliest on "+ diffStats.diffmax_DateTime );
					if ( WarnIfDifferent_boolean ) {
						message = "Time series for " + ts1.getIdentifier() + " have differences.";
						Message.printWarning (warning_level,
						MessageUtil.formatMessageTag(command_tag,++warning_count),routine, message );
					}
					++tsdiff_count;
				}
				else {
				    Message.printStatus ( 2, routine, loc1 + " had no differences." );
				}
				// Save information for the report...
				compare_tslist.add ( ts1 );
				compare_numvalues.add (	new Integer(diffStats.totalcount) );
				compare_diffcount.add ( diffcount );
				compare_diffabsavg.add ( diffabsavg );
				compare_diffavg.add ( diffavg );
				if ( is_diff ) {
					compare_diffmax.add (new Double(diffStats.diffmax) );
					compare_diffmaxdate.add (new DateTime(diffStats.diffmax_DateTime) );
				}
				else {
				    compare_diffmax.add (new Double(0.0) );
					compare_diffmaxdate.add (null );
				}
			}
		}
		Message.printStatus ( 2, routine, "" + tsdiff_count + " of " + size + " time series had differences." );
		// else print a warning and throw an exception below.

		// Print a summary of the comparison...

		size = compare_tslist.size();
		StringBuffer b = new StringBuffer(); // Full report
		StringBuffer b2 = new StringBuffer(); // Only differences
		int location_length = 8; // Enough for heading
		int datatype_length = 9; // Enough for heading
		TS ts = null;
		// Figure out the length for some of the string columns...
		for ( int its = 0; its < size; its++ ) {
			ts = compare_tslist.get(its);
			if ( ts.getIdentifier().getLocation().length() > location_length ) {
				location_length = ts.getIdentifier().getLocation().length();
			}
			if ( MatchDataType_boolean ) {
				if ( ts.getIdentifier().getType().length() > datatype_length ) {
					datatype_length = ts.getIdentifier().getType().length();
				}
			}
		}
		String location_format = "%-14.14s";	// Default max
		if ( location_length < 14 ) {
			location_format = "%-" + location_length + "." + location_length + "s";
		}
		String datatype_format = "%-14.14s";	// Default max
		if ( datatype_length < 14 ) {
			datatype_format = "%-" + datatype_length + "." + datatype_length + "s";
		}
		String int_format = "%7d";
		String double_format = "%10.2f";
		String double_blank = "          ";
		String date_format = "%10.10s";
		// Print the headings...
		String nl = System.getProperty ( "line.separator" );
		b.append ( "|" + StringUtil.formatString( "Location", location_format) );
		b2.append ( "|" + StringUtil.formatString( "Location", location_format) );
		if ( MatchDataType_boolean ) {
			b.append ( "|" + StringUtil.formatString( "Data Type", datatype_format));
			b2.append ( "|" + StringUtil.formatString( "Data Type", datatype_format));
		}
		b.append ( "| #Val  " );
		b2.append ( "| #Val  " );
		for ( j = 0; j < Tolerance_count; j++ ) {
			b.append ( "|#D>" + StringUtil.formatString(Tolerance_tokens.get(j),"%-4.4s") );
			b2.append ( "|#D>" + StringUtil.formatString(Tolerance_tokens.get(j),"%-4.4s") );
			b.append ( "|AbsAvgDiff" );
			b2.append ( "|AbsAvgDiff" );
			b.append ( "| AvgDiff  " );
			b2.append ( "| AvgDiff  " );
		}
		b.append ( "| MaxDiff  " );
		b2.append ( "| MaxDiff  " );
		b.append ( "| MaxDate  " );
		b2.append ( "| MaxDate  " );
		b.append ( "|" + nl );
		b2.append ( "|" + nl );
		int [] diffcount_array;
		double [] diffabsavg_array;
		double [] diffavg_array;
		for ( int its = 0; its < size; its++ ) {
			ts = compare_tslist.get(its);
			is_diff = false;
			// Check for difference here so that difference-only summary report can be completed...
			diffcount_array = (int[])compare_diffcount.get(its);
			for ( j = 0; j < Tolerance_count; j++ ) {
				if ( diffcount_array[j] > 0 ) {
					is_diff = true;
				}
			}
			// Always put in the location...
			b.append ( "|" + StringUtil.formatString(ts.getIdentifier().getLocation(),location_format) );
			if ( is_diff ) {
				b2.append ( "|" +StringUtil.formatString(ts.getIdentifier().getLocation(), location_format) );
			}
			if ( MatchDataType_boolean ) {
				b.append ( "|" + StringUtil.formatString(ts.getIdentifier().getType(),datatype_format) );
				if ( is_diff ) {
					b2.append ( "|" + StringUtil.formatString(ts.getIdentifier().getType(),datatype_format) );
				}
			}
			b.append ( "|" + StringUtil.formatString(compare_numvalues.get(its).intValue(),int_format) );
			if ( is_diff ) {
				b2.append ( "|" + StringUtil.formatString(compare_numvalues.get(its).intValue(),int_format) );
			}
			diffabsavg_array=(double[])compare_diffabsavg.get(its);
			diffavg_array=(double[])compare_diffavg.get(its);
			for ( j = 0; j < Tolerance_count; j++ ) {
				b.append ( "|" + StringUtil.formatString(diffcount_array[j], int_format) );
				if ( is_diff ) {
					b2.append ( "|" + StringUtil.formatString(diffcount_array[j], int_format) );
				}
				if ( diffcount_array[j] > 0 ) {
					b.append ( "|" + StringUtil.formatString(diffabsavg_array[j], double_format) );
					b.append ( "|" + StringUtil.formatString(diffavg_array[j], double_format) );
					if ( is_diff ) {
						b2.append ( "|" + StringUtil.formatString(diffabsavg_array[j], double_format) );
						b2.append ( "|" + StringUtil.formatString(diffavg_array[j], double_format) );
					}
				}
				else {
				    b.append ( "|" + double_blank );
					b.append ( "|" + double_blank );
					if ( is_diff ) {
						b2.append ( "|" + double_blank );
						b2.append ( "|" + double_blank );
					}
				}
			}
			if ( is_diff ) {
				b.append ( "|" + StringUtil.formatString(compare_diffmax.get(its).doubleValue(), double_format) );
				b2.append ( "|" + StringUtil.formatString(compare_diffmax.get(its).doubleValue(),double_format) );
				DateTime date = compare_diffmaxdate.get(its);
				if ( date == null ) {
					b.append ( "|" + StringUtil.formatString( "", date_format) );
					b2.append ( "|" + StringUtil.formatString( "", date_format) );
				}
				else {
				    b.append ( "|" +StringUtil.formatString(compare_diffmaxdate.get(its).toString(),date_format) );
					b2.append ( "|" +StringUtil.formatString(compare_diffmaxdate.get(its).toString(),date_format) );
				}
			}
			else {
			    b.append ( "|" + double_blank );
				b.append ( "|" + StringUtil.formatString( "",date_format) );
			}
			b.append ( "|" + nl );	// Last border
			if ( is_diff ) {
				b2.append ( "|" + nl );	// Last border
			}
		}
		Message.printStatus ( 2, "", "Summary of differences only (differences are second time " +
				"series minus first time series):" + nl + b2.toString() );
		Message.printStatus ( 2, "", "" );
		Message.printStatus ( 2, "", "Summary of differences for all time series (differences are second time " +
			"series minus first time series):" + nl + b.toString() );

		if ( doCreateDiffTS ) {
			try {
			    Object o = processor.getPropContents ( "TSResultsList" );
			    @SuppressWarnings("unchecked")
				List<TS> tslist0 = (List<TS>)o;
				tslist = tslist0;
			}
			catch ( Exception e ) {
				// Not fatal, but of use to developers.
				message = "Error requesting TSResultsList from processor - no data.";
				Message.printWarning(3, routine, message );
			}
			// Add the difference time series...
			int diffsize = difftsList.size();
			for ( int its = 0; its < diffsize; its++ ) {
				tslist.add ( difftsList.get(its) );
			}
			try {
			    processor.setPropContents ( "TSResultsList", tslist );
			}
			catch ( Exception e ) {
				message = "Error setting time series list appended with difference time series.";
				Message.printWarning ( warning_level, 
					MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
				Message.printWarning ( 3, routine, e );
                   status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
				throw new CommandException ( message );
			}
			Message.printStatus ( 2, "",
				"Difference time series (second time series minus first) have been appended to results." );
		}
		
		if ( tsComparisonsTried == 0 ) {
		    message = "No time series comparisons were done based on the input parameters.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the location, data type, etc. " +
                    	"parameters result in pairs of time series to compare." ) );
		}
	    // Set the property indicating the number of rows in the table
        if ( (DiffCountProperty != null) && !DiffCountProperty.isEmpty() && (diffcount != null) ) {
            int diffCountTotal = 0;
            for ( int its = 0; its < diffcount.length; its++ ) {
            	diffCountTotal += diffcount[its];
            }
            PropList request_params = new PropList ( "" );
            request_params.setUsingObject ( "PropertyName", DiffCountProperty );
            request_params.setUsingObject ( "PropertyValue", new Integer(diffCountTotal) );
            try {
                processor.processRequest( "SetProperty", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting SetProperty(Property=\"" + DiffCountProperty + "\") from processor.";
                Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
            }
        }
        }
    }
	catch ( Exception e ) {
		message = "Unexpected error comparing time series (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		Message.printWarning ( 3, routine, e );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
		throw new CommandException ( message );
	}
	if ( commandPhase == CommandPhaseType.RUN ) {
		if ( WarnIfDifferent_boolean && (tsdiff_count > 0) ) {
			message = "" + tsdiff_count + " of " + size + " time series had differences.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
	        status.addToLog ( commandPhase,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Verify that time series being different is OK." ) );
	        // TODO smalers 2021-08-18 log is enough - the command was successful in running
			//throw new CommandException ( message );
		}
		if ( WarnIfSame_boolean && (tsdiff_count == 0) ) {
			message = "All " + size + " time series are the same.";
			Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
	        status.addToLog ( commandPhase,
	            new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Verify that all time seres being the same is OK." ) );
	        // TODO smalers 2021-08-18 log is enough - the command was successful in running
			//throw new CommandException ( message );
		}
	}
	
	status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __discoveryTable = table;
}

/**
Check that the output table is set up.  The following columns are included
(might also include flags later):
<ol>
<li>DateTime</li>
<li>TSID1</li>
<li>TSID2</li>
<li>Value1</li>
<li>Flag1</li>
<li>Value2</li>
<li>Flag2</li>
<li>Diff</li>
<li>DiffPercent/</li>
</ol>
@param table table to set up
@return true if table is being used, false if not.
*/
private boolean setupOutputTable ( DataTable table )
{	if ( table == null ) {
		return false;
	}
	String tableDateTimeColumn = "DateTime";
	String tableTSID1Column = "TSID1";
	String tableTSID2Column = "TSID2";
	String tableValue1Column = "Value1";
	String tableFlag1Column = "Flag1";
	String tableValue2Column = "Value2";
	String tableFlag2Column = "Flag2";
	String tableDiffColumn = "Diff";
	String tableDiffPercentColumn = "DiffPercent";
	String tableCommentColumn = "Comment";
	try {
		if ( (tableDateTimeColumn != null) && !tableDateTimeColumn.isEmpty() ) {
			__tableDateTimeColumnNum = table.getFieldIndex(tableDateTimeColumn);
		}
	}
	catch ( Exception e ) {
		// Not found so create it
		__tableDateTimeColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_DATETIME, tableDateTimeColumn, -1, -1), "");
	}
	try {
		if ( (tableTSID1Column != null) && !tableTSID1Column.isEmpty() ) {
			__tableTSID1ColumnNum = table.getFieldIndex(tableTSID1Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it
		__tableTSID1ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_STRING, tableTSID1Column, -1, -1), "");
	}
	try {
		if ( (tableTSID2Column != null) && !tableTSID2Column.isEmpty() ) {
			__tableTSID2ColumnNum = table.getFieldIndex(tableTSID2Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it
		__tableTSID2ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_STRING, tableTSID2Column, -1, -1), "");
	}
	try {
		if ( (tableValue1Column != null) && !tableValue1Column.isEmpty() ) {
			__tableValue1ColumnNum = table.getFieldIndex(tableValue1Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it - use 4 digits of precision for the check output
		__tableValue1ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_DOUBLE, tableValue1Column, -1, 4), "");
	}
	try {
		if ( (tableFlag1Column != null) && !tableFlag1Column.isEmpty() ) {
			__tableFlag1ColumnNum = table.getFieldIndex(tableFlag1Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it
		__tableFlag1ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_STRING, tableFlag1Column, -1, -1), "");
	}
	try {
		if ( (tableValue2Column != null) && !tableValue2Column.isEmpty() ) {
			__tableValue2ColumnNum = table.getFieldIndex(tableValue2Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it - use 4 digits of precision for the check output
		__tableValue2ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_DOUBLE, tableValue2Column, -1, 4), "");
	}
	try {
		if ( (tableFlag2Column != null) && !tableFlag2Column.isEmpty() ) {
			__tableFlag2ColumnNum = table.getFieldIndex(tableFlag2Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it
		__tableFlag2ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_STRING, tableFlag2Column, -1, -1), "");
	}
	try {
		if ( (tableDiffColumn != null) && !tableDiffColumn.isEmpty() ) {
			__tableDiffColumnNum = table.getFieldIndex(tableDiffColumn);
		}
	}
	catch ( Exception e ) {
		// Not found so create it - use 4 digits of precision for the check output
		__tableDiffColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_DOUBLE, tableDiffColumn, -1, 4), "");
	}
	try {
		if ( (tableDiffPercentColumn != null) && !tableDiffPercentColumn.isEmpty() ) {
			__tableDiffPercentColumnNum = table.getFieldIndex(tableDiffPercentColumn);
		}
	}
	catch ( Exception e ) {
		// Not found so create it - use 4 digits of precision for the check output
		__tableDiffPercentColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_DOUBLE, tableDiffPercentColumn, -1, 4), "");
	}
	try {
		if ( (tableCommentColumn != null) && !tableCommentColumn.isEmpty() ) {
			__tableCommentColumnNum = table.getFieldIndex(tableCommentColumn);
		}
	}
	catch ( Exception e ) {
		// Not found so create it
		__tableCommentColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_STRING, tableCommentColumn, -1, -1), "");
	}
	return true;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String TSID1 = props.getValue("TSID1");
	String TSID2 = props.getValue("TSID2");
	String EnsembleID1 = props.getValue("EnsembleID1");
	String EnsembleID2 = props.getValue("EnsembleID2");
	String MatchLocation = props.getValue("MatchLocation");
	String MatchDataType = props.getValue("MatchDataType");
	String Precision = props.getValue("Precision");
	String Tolerance = props.getValue("Tolerance");
	String CompareFlags = props.getValue("CompareFlags");
	String AnalysisStart = props.getValue("AnalysisStart");
	String AnalysisEnd = props.getValue("AnalysisEnd");
	String DiffFlag = props.getValue("DiffFlag");
	String CreateDiffTS = props.getValue("CreateDiffTS");
	String TableID = props.getValue ( "TableID" );
	String DiffCountProperty = props.getValue ( "DiffCountProperty" );
	String WarnIfDifferent = props.getValue("WarnIfDifferent");
	String WarnIfSame = props.getValue("WarnIfSame");
	StringBuffer b = new StringBuffer ();
	if ( (TSID1 != null) && (TSID1.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID1=\"" + TSID1 + "\"" );
	}
	if ( (TSID2 != null) && (TSID2.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID2=\"" + TSID2 + "\"" );
	}
	if ( (EnsembleID1 != null) && (EnsembleID1.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "EnsembleID1=\"" + EnsembleID1 + "\"" );
	}
	if ( (EnsembleID2 != null) && (EnsembleID2.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "EnsembleID2=\"" + EnsembleID2 + "\"" );
	}
	if ( (MatchLocation != null) && (MatchLocation.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "MatchLocation=" + MatchLocation );
	}
	if ( (MatchDataType != null) && (MatchDataType.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "MatchDataType=" + MatchDataType );
	}
	if ( (Precision != null) && (Precision.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Precision=" + Precision );
	}
	if ( (Tolerance != null) && (Tolerance.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Tolerance=\"" + Tolerance + "\"" );
	}
	if ( (CompareFlags != null) && (CompareFlags.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "CompareFlags=\"" + CompareFlags + "\"" );
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
	if ( (DiffFlag != null) && (DiffFlag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DiffFlag=\"" + DiffFlag + "\"" );
	}
	if ( (CreateDiffTS != null) && (CreateDiffTS.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "CreateDiffTS=" + CreateDiffTS );
	}
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (DiffCountProperty != null) && (DiffCountProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DiffCountProperty=\"" + DiffCountProperty + "\"" );
    }
	if ( (WarnIfDifferent != null) && (WarnIfDifferent.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "WarnIfDifferent=" + WarnIfDifferent );
	}
	if ( (WarnIfSame != null) && (WarnIfSame.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "WarnIfSame=" + WarnIfSame );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

/*
 * Internal class to facilitate passing multiple values to/from compareTimeSeriesValues() function.
 */
private class DiffStats {
	int totalcount = 0;
	double diffmaxabs = 0.0;
	double diffmax = 0.0;
	DateTime diffmax_DateTime = null; // Date for max diff.
}

}