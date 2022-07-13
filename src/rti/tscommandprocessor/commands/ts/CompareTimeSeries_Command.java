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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
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
public class CompareTimeSeries_Command extends AbstractCommand implements CommandDiscoverable, FileGenerator, ObjectListProvider
{

/**
Data members used for parameter values.
*/
protected final String _False = "False";
protected final String _True = "True";
protected final String _IfDifferent = "IfDifferent";

/**
Data members used for IfDifferent and IfSame parameters.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

/**
The table that is created for discovery mode.
*/
private DataTable __discoveryTable = null;

/**
Output files that are created by this command.
*/
private List<File> __outputFileList = new ArrayList<>();

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
	// Save the results to the table.
	// Add a record to the table.
	TableRecord rec = table.emptyRecord();
	try {
		if ( __tableDateTimeColumnNum >= 0 ) {
			// Make a copy since iterating.
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
		// This should not happen.
		String routine = getClass().getSimpleName() + ".addTableRecord";
		Message.printWarning(3, routine, "Error adding record for ts1=\"" + ts1.getIdentifierString() +
			"\" ts2=\"" + ts2.getIdentifierString() + "\" date/time=" + dt );
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
	String MatchAlias = parameters.getValue ( "MatchAlias" );
	String Precision = parameters.getValue ( "Precision" );
	String Tolerance = parameters.getValue ( "Tolerance" );
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String DiffFlag = parameters.getValue ( "DiffFlag" );
	String CreateDiffTS = parameters.getValue ( "CreateDiffTS" );
	String AllowedDiff = parameters.getValue ( "AllowedDiff" );
	//String AllowedDiffPerTS = parameters.getValue ( "AllowedDiffPerTS" );
	String IfDifferent = parameters.getValue ( "IfDifferent" );
	String IfSame = parameters.getValue ( "IfSame" );
	//String WarnIfDifferent = parameters.getValue ( "WarnIfDifferent" );
	//String WarnIfSame = parameters.getValue ( "WarnIfSame" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	if ( (MatchLocation != null) && !MatchLocation.isEmpty() ) {
		if ( !MatchLocation.equals(_False) && !MatchLocation.equals(_True) ) {
            message = "The MatchLocation parameter \"" + MatchLocation + "\" if specified must be False or True.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify False or True (or do not specify to use default of True)." ) );
		}
	}
	if ( (MatchDataType != null) && !MatchDataType.isEmpty() ) {
		if ( !MatchDataType.equals(_False) && !MatchDataType.equals(_True) ) {
            message = "The MatchDataType parameter \"" + MatchDataType + "\" if specified must be False or True.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify False or True (or do not specify to use default of False)." ) );
		}
	}
	if ( (MatchAlias != null) && !MatchAlias.isEmpty() ) {
		if ( !MatchAlias.equals(_False) && !MatchAlias.equals(_True) ) {
            message = "The MatchAlias parameter \"" + MatchAlias + "\" if specified must be False or True.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify False or True (or do not specify to use default of False)." ) );
		}
	}
	if ( (Precision != null) && !Precision.isEmpty() ) {
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
	if ( (Tolerance != null) && !Tolerance.isEmpty() ) {
		List<String> v = StringUtil.breakStringList(Tolerance,", ",0);
		int size = 0;
		if ( v != null ) {
			size = v.size();
		}
		// Make sure that each tolerance is a number.
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
	if ( (AnalysisStart != null) && !AnalysisStart.isEmpty() &&
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
	if ( (AnalysisEnd != null) && !AnalysisEnd.isEmpty() && !AnalysisEnd.equalsIgnoreCase("OutputEnd") && (AnalysisEnd.indexOf("${") < 0)) {
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
	if ( (CreateDiffTS != null) && !CreateDiffTS.isEmpty() ) {
		if ( !CreateDiffTS.equals(_False) && !CreateDiffTS.equals(_True) && !CreateDiffTS.equals(_IfDifferent) ) {
            message = "The CreateDiffTS parameter \"" + CreateDiffTS + "\" must be False or True.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify " + _False + " (default), " + _True + " or " + _IfDifferent ) );
		}
	}
    if ( (AllowedDiff != null) && !AllowedDiff.equals("") && !StringUtil.isInteger(AllowedDiff) ) {
        message = "The number of allowed differences (all time series) \"" + AllowedDiff + "\" is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                 message, "Specify the parameter as an integer."));
    }
    /*
    if ( (AllowedDiffPerTS != null) && !AllowedDiffPerTS.equals("") && !StringUtil.isInteger(AllowedDiffPerTS) ) {
        message = "The number of allowed differences (per time series) \"" + AllowedDiffPerTS + "\" is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                 message, "Specify the parameter as an integer."));
    }
    */
	if ( (IfDifferent != null) && !IfDifferent.equals("") && !IfDifferent.equalsIgnoreCase(_Ignore) &&
		!IfDifferent.equalsIgnoreCase(_Warn) && !IfDifferent.equalsIgnoreCase(_Fail) ) {
			message = "The IfDifferent parameter \"" + IfDifferent + "\" is not a valid value.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the parameter as " + _Ignore + " (default), " +
						_Warn + ", or " + _Fail + "."));
	}
	if ( (IfSame != null) && !IfSame.equals("") && !IfSame.equalsIgnoreCase(_Ignore) &&
		!IfSame.equalsIgnoreCase(_Warn) && !IfSame.equalsIgnoreCase(_Fail) ) {
		message = "The IfSame parameter \"" + IfSame + "\" is not a valid value.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Ignore + " (default), " +
					_Warn + ", or " + _Fail + "."));
	}
	int sameCount = 0;
	int diffCount = 0;
	if ( (IfSame != null) && (IfSame.equalsIgnoreCase(_Warn) || IfSame.equalsIgnoreCase(_Fail)) ) {
		++sameCount;
	}
	if ( (IfDifferent != null) && (IfDifferent.equalsIgnoreCase(_Warn) || IfDifferent.equalsIgnoreCase(_Fail)) ) {
		++diffCount;
	}
	if ( (sameCount + diffCount) == 0 ) {
        message = "At lease one of IfDifferent or IfSame must be " + _Warn + " or + " + _Fail + ".";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Check the values of IfDifferent and IfSame." ) );
	}
	if ( (sameCount + diffCount) > 1 ) {
        message = "Only one of IfDifferent or IfSame can be " + _Warn + " or + " + _Fail + ".";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Check the values of IfDifferent and IfSame." ) );
	}

	/*
	if ( (WarnIfDifferent != null) && !WarnIfDifferent.isEmpty() ) {
		if ( !WarnIfDifferent.equals(_False) &&	!WarnIfDifferent.equals(_True) ) {
            message = "The WarnIfDifferent parameter \"" + WarnIfDifferent + "\" must be False or True.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify False or True (or blank for default of False)." ) );
		}
	}
	if ( (WarnIfSame != null) && !WarnIfSame.isEmpty() ) {
		if ( !WarnIfSame.equals(_False) && !WarnIfSame.equals(_True) ) {
            message = "The WarnIfSame parameter \"" + WarnIfSame + "\" must be False or True.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify False or True (or blank for default of False)." ) );
		}
	}
	// At least one of WarnIfDifferent or WarnIfSame should be True - otherwise the command does nothing and probably a mistake.
	if ( WarnIfSame == null ) {
		WarnIfSame = _False; // Default.
	}
	if ( WarnIfDifferent == null ) {
		WarnIfDifferent = _False; // Default.
	}
	if ( !WarnIfSame.equals(_True) && !WarnIfDifferent.equals(_True)) {
        message = "At lease one of WarnIfDifferent or WarnIfSame must be True.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check the values of WarnIfDifferent and WarnIfSame." ) );
	}
	*/
    
	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(22);
	validList.add ( "TSID1" );
	validList.add ( "TSID2" );
	validList.add ( "EnsembleID1" );
	validList.add ( "EnsembleID2" );
	validList.add ( "MatchLocation" );
	validList.add ( "MatchDataType" );
	validList.add ( "MatchAlias" );
	validList.add ( "Precision" );
	validList.add ( "Tolerance" );
	validList.add ( "CompareFlags" );
	validList.add ( "AnalysisStart" );
	validList.add ( "AnalysisEnd" );
	validList.add ( "AllowedDiff" );
	//validList.add ( "AllowedDiffPerTS" );
	validList.add ( "DiffFlag" );
	validList.add ( "CreateDiffTS" );
    validList.add ( "DifferenceFile" );
    validList.add ( "SummaryFile" );
    validList.add ( "TableID" );
    validList.add ( "DiffCountProperty" );
	validList.add ( "IfDifferent" );
	validList.add ( "IfSame" );
	//validList.add ( "WarnIfDifferent" );
	//validList.add ( "WarnIfSame" );
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
 * @return true if any differences were detected according to the criteria
 */
private boolean compareTimeSeriesValues (
	String routine,
	TS ts1, String loc1, TS ts2,
	boolean compareFlags, String DiffFlag, String Precision, int Precision_int, String value_format, int Tolerance_count, double [] Tolerance_double,
	DateTime date, double value1_orig, TSData tsdata1, String flag1, double value2_orig, TSData tsdata2, String flag2,
	int [] diffcount, double [] difftotal, double [] difftotalabs,
	boolean doCreateDiffTS, boolean doCreateDiffTSAlways, TS diffts,
	boolean doTable, DataTable table,
	DiffStats diffStats) {

	double value1, value2;
	// Assume flags that are null are equivalent to empty flag.
	if ( flag1 == null ) {
		flag1 = "";
	}
	if ( flag2 == null ) {
		flag2 = "";
	}
	if ( Precision != null ) {
		// Need to round.  For now do with strings, which handles the rounding.
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
	// For troubleshooting.
	//Message.printStatus(2,routine,"Value1="+ value1_orig + " Value2="+ value2_orig);
	// Count of data points that are checked.
	++diffStats.totalcount;
	boolean is_diff = false; // Initialize, set to true below if difference detected.
	if ( ts1.isDataMissing(value1_orig) && !ts2.isDataMissing(value2_orig)) {
		// ts1 is missing.
		// ts2 is not missing.
		Message.printStatus ( 2, routine, loc1 + " has different data on " + date +
			" TS1 = missing " + flag1 +
			" TS2 = " + StringUtil.formatString(value2,value_format) + " " + flag2 );
		String diffMessage = "TS1 missing, TS2 not missing";
		if ( compareFlags ) {
			// Also check flags.
			if ( !flag1.equals(flag2) ) {
				diffMessage += ", flags differ";
			}
		}
		if ( doTable ) {
			addTableRecord ( table, date, ts1, ts2, value1_orig, flag1, value2_orig, flag2, Double.NaN, diffMessage );
		}
		// Indicate as missing at all levels.
		is_diff = true;
		for ( int it = 0; it < Tolerance_count; it++ ) {
			++diffcount[it];
		}
		if ( doCreateDiffTS ) {
			// ts2 has value so make that the difference:
			// - allocate the data space in lazy fashion
			if ( !diffts.hasData() ) {
				diffts.setDate1(ts2.getDate1());
				diffts.setDate2(ts2.getDate2());
				diffts.allocateDataSpace();
			}
			diffts.setDataValue ( date, value2 );
		}
	}
	else if(!ts1.isDataMissing(value1_orig) && ts2.isDataMissing(value2_orig)) {
		// ts1 is not missing.
		// ts2 is missing.
		Message.printStatus ( 2, routine, loc1 + " has different data on " + date +
			" TS1 = " + StringUtil.formatString(value1,value_format) + " " + flag1 +
			" TS2 = missing " + flag2);
		String diffMessage = "TS1 not missing, TS2 missing";
		if ( compareFlags ) {
			// Also check flags.
			if ( !flag1.equals(flag2) ) {
				diffMessage += ", flags differ";
			}
		}
		if ( doTable ) {
			addTableRecord ( table, date, ts1, ts2, value1_orig, flag1, value2_orig, flag2, Double.NaN, diffMessage );
		}
		// Indicate as missing at all levels.
		is_diff = true;
		for ( int it = 0; it < Tolerance_count; it++ ) {
			++diffcount[it];
		}
		if ( doCreateDiffTS ) {
			// ts1 has value so make that the difference (negative).
			if ( !diffts.hasData() ) {
				diffts.setDate1(ts2.getDate1());
				diffts.setDate2(ts2.getDate2());
				diffts.allocateDataSpace();
			}
			diffts.setDataValue ( date, -value2 );
		}
	}
	else if(ts1.isDataMissing(value1_orig) && ts2.isDataMissing(value2_orig)) {
		// Both missing so consider the values equal.
		if ( compareFlags ) {
			// Also check flags.
			if ( !flag1.equals(flag2) ) {
				String diffMessage = "Both missing but flags differ";
				if ( doTable ) {
					addTableRecord ( table, date, ts1, ts2, value1_orig, flag1, value2_orig, flag2, Double.NaN, diffMessage );
				}
				is_diff = true;
				// No need to set value in diff time series since both missing.
			}
		}
	}
	else {
		// ts1 is not missing.
		// ts2 is not missing.
	   	// Analyze differences for each tolerance.
		double diff = value2 - value1;
		double diffabs = Math.abs(diff);
		if ( diffabs > diffStats.diffmaxabs ) {
			diffStats.diffmaxabs = diffabs;
			diffStats.diffmax = diff;
			diffStats.diffmax_DateTime = new DateTime ( date );
		}
		String flagDiffMessage = null;
		if ( compareFlags ) {
			// Also check flags.
			if ( !flag1.equals(flag2) ) {
				flagDiffMessage = "flags differ";
			}
		}
		int diffCount = 0; // Count of differences for tolerance checks.
		for ( int it = 0; it < Tolerance_count; it++ ) {
			if ( diffabs > Tolerance_double[it] ) {
				++diffCount;
    			// Report the difference.
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
			// No numerical value differences were detected but flags are different.
   			addTableRecord ( table, date, ts1, ts2, value1, flag1, value2, flag2, diff, flagDiffMessage );
		}
		if ( doCreateDiffTS ) {
			if ( doCreateDiffTSAlways ) {
				// Set difference (regardless of tolerance).
				if ( !diffts.hasData() ) {
					diffts.setDate1(ts2.getDate1());
					diffts.setDate2(ts2.getDate2());
					diffts.allocateDataSpace();
				}
				diffts.setDataValue ( date, diff );
			}
			else if ( is_diff ) {
				// Only set the difference time series value if met the criteria.
				if ( !diffts.hasData() ) {
					diffts.setDate1(ts2.getDate1());
					diffts.setDate2(ts2.getDate2());
					diffts.allocateDataSpace();
				}
				diffts.setDataValue ( date, diff );
			}
		}
	}
	if ( is_diff && (DiffFlag != null) ) {
		// Append to the data flag.
		TSData tsdata = new TSData(); // Data point from time series.
		tsdata = ts1.getDataPoint (date, tsdata);
		ts1.setDataValue ( date, value1_orig, tsdata.getDataFlag().trim() + DiffFlag, 1 );
		tsdata = ts2.getDataPoint (date, tsdata);
		ts2.setDataValue ( date, value2_orig, tsdata.getDataFlag().trim() + DiffFlag, 1 );
	}
	return is_diff;
}

/**
 * Create a difference time series.
 *@param ts2 the time series to use for information to create the difference time series.
 *@return the difference time series, which can hold the difference values.
 */
private TS createDiffTimeSeries ( TS ts2 )
throws Exception {
	TS diffts = null;
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
	// Don't allocate the data space here:
	// - only do that on the first difference
	return diffts;
}

// TODO smalers 2021-08-27 need to just write to the file without StringBuilder:
// - the following code was extracted from runCommandInternal as a first step
/**
 * Create report files.
 */
private void createReportFiles ( List<TS> outputTSList, String DifferenceFile_full, String SummaryFile_full,
	boolean MatchDataType_boolean,
	List<String> matchedList,
	int Tolerance_count,
	List<String> Tolerance_tokens,
	List<int []> compare_diffcount,
	List<Integer> compare_numvalues,
	List<Double> compare_diffmax,
	List<DateTime> compare_diffmaxdate,
	List<double []> compare_diffabsavg,
	List<double []> compare_diffavg,
	List<String> problems
	) {
	int size = outputTSList.size();
	StringBuilder diffBuilder = new StringBuilder(); // Full difference report (longer file).
	StringBuilder summaryBuilder = new StringBuilder(); // Only summary of differences (shorter file).
	int locationLength = 8; // Enough for heading text.
	int datatypeLength = 9; // Enough for heading text.
	int tsidLength = 4; // Enough for "TSID" heading text.
	int tsaliasLength = 7; // Enough for "TSAlias" heading text.
	TS ts = null;
	// Figure out the length for some of the string columns.
	for ( int its = 0; its < size; its++ ) {
		ts = outputTSList.get(its);
		if ( ts.getIdentifier().getLocation().length() > locationLength ) {
			locationLength = ts.getIdentifier().getLocation().length();
		}
		if ( ts.getIdentifierString().length() > tsidLength ) {
			// This is for all time series and may result in spaces at the end for summary.
			tsidLength = ts.getIdentifierString().length();
		}
		if ( ts.getAlias().length() > tsaliasLength ) {
			// This is for all time series and may result in spaces at the end for summary.
			tsaliasLength = ts.getAlias().length();
		}
		if ( MatchDataType_boolean ) {
			if ( ts.getIdentifier().getType().length() > datatypeLength ) {
				datatypeLength = ts.getIdentifier().getType().length();
			}
		}
	}

	String locationFormat = "%-8.8s"; // Default width, reset below.
	String datatypeFormat = "%-9.9s"; // Default width, reset below.
	String descriptionFormat = "%-20.20s"; // Fixed width - reasonable length but avoid wide output.
	String tsidFormat = "%-4.4s"; // Default width, reset below.
	String tsaliasFormat = "%-7.7s"; // Default width, reset below.
	if ( locationLength > 8 ) {
		// Increase the width.
		locationFormat = "%-" + locationLength + "." + locationLength + "s";
	}
	if ( datatypeLength > 9 ) {
		// Increase the width.
		datatypeFormat = "%-" + datatypeLength + "." + datatypeLength + "s";
	}
	if ( tsidLength > 4 ) {
		// Increase the width.
		tsidFormat = "%-" + tsidLength + "." + tsidLength + "s";
	}
	if ( tsaliasLength > 4 ) {
		// Increase the width.
		tsaliasFormat = "%-" + tsaliasLength + "." + tsaliasLength + "s";
	}
	String intFormat = "%7d";
	String intBlank = "       ";
	String doubleFormat = "%10.2f";
	String doubleBlank = "          ";
	String dateFormat = "%10.10s";
	String dateBlank = "          ";
	String nl = System.getProperty ( "line.separator" );
	summaryBuilder.append ( "# Summary of time series with differences only." + nl );
	summaryBuilder.append ( "# Differences are second time series minus first time series." + nl );
	summaryBuilder.append ( "# Time series with a match are listed once." + nl );
	summaryBuilder.append ( "# Time series without a match (if any) are listed with no analysis results." + nl );
	summaryBuilder.append ( "# Time series with > 2 matches are listed with no analysis results." + nl );
	diffBuilder.append ( "# Summary of differences for all time series, including those with no differences." + nl );
	diffBuilder.append ( "# Differences are second time series minus first time series." + nl );
	diffBuilder.append ( "# Time series matches are listed once." + nl );
	diffBuilder.append ( "# Time series without a match (if any) are listed with no analysis results." + nl );
	diffBuilder.append ( "# Time series with > 2 matches are listed with no analysis results." + nl );
	// Print the headings rows.
	diffBuilder.append ( "|  #  " );
	diffBuilder.append ( "|" + StringUtil.formatString( "Location", locationFormat) );
	diffBuilder.append ( "|" + StringUtil.formatString( "Description", descriptionFormat) );
	diffBuilder.append ( "|" + StringUtil.formatString( "TSID", tsidFormat) );
	diffBuilder.append ( "|" + StringUtil.formatString( "TSAlias", tsaliasFormat) );
	summaryBuilder.append ( "|  #  " );
	summaryBuilder.append ( "|" + StringUtil.formatString( "Location", locationFormat) );
	summaryBuilder.append ( "|" + StringUtil.formatString( "Description", descriptionFormat) );
	summaryBuilder.append ( "|" + StringUtil.formatString( "TSID", tsidFormat) );
	summaryBuilder.append ( "|" + StringUtil.formatString( "TSAlias", tsaliasFormat) );
	if ( MatchDataType_boolean ) {
		diffBuilder.append ( "|" + StringUtil.formatString( "Data Type", datatypeFormat));
		summaryBuilder.append ( "|" + StringUtil.formatString( "Data Type", datatypeFormat));
	}
	diffBuilder.append ( "|Matched" );
	summaryBuilder.append ( "|Matched" );
	diffBuilder.append ( "| #Val  " );
	summaryBuilder.append ( "| #Val  " );
	for ( int it = 0; it < Tolerance_count; it++ ) {
		diffBuilder.append ( "|#D>" + StringUtil.formatString(Tolerance_tokens.get(it),"%-4.4s") );
		summaryBuilder.append ( "|#D>" + StringUtil.formatString(Tolerance_tokens.get(it),"%-4.4s") );
		diffBuilder.append ( "|AbsAvgDiff" );
		summaryBuilder.append ( "|AbsAvgDiff" );
		diffBuilder.append ( "| AvgDiff  " );
		summaryBuilder.append ( "| AvgDiff  " );
	}
	diffBuilder.append ( "| MaxDiff  " );
	summaryBuilder.append ( "| MaxDiff  " );
	diffBuilder.append ( "| MaxDate  " );
	summaryBuilder.append ( "| MaxDate  " );
	diffBuilder.append ( "|" + nl );
	summaryBuilder.append ( "|" + nl );
	int [] diffcount_array = null;
	double [] diffabsavg_array = null;
	double [] diffavg_array = null;
	boolean is_diff;
	for ( int its = 0; its < size; its++ ) {
		ts = outputTSList.get(its);
		is_diff = false;
		// Check for difference here so that difference-only summary report can be completed:
		// - the diffcount_array will be null if no comparisons was performed (no matching time series)
		// - show zeros for the difference and match column will indicate no match
		if ( compare_diffcount.get(its) == null ) {
			diffcount_array = new int[Tolerance_count];
			for ( int it = 0; it < Tolerance_count; it++ ) {
				diffcount_array[it] = 0;
			}
			is_diff = true;
		}
		else {
			diffcount_array = (int[])compare_diffcount.get(its);
			if ( diffcount_array != null ) {
				for ( int it = 0; it < Tolerance_count; it++ ) {
					if ( (diffcount_array.length > 0) && (diffcount_array[it] > 0) ) {
						is_diff = true;
					}
				}
			}
		}
		// Difference file includes all time series.
		diffBuilder.append ( "|" + StringUtil.formatString((its + 1),"%5d") );
		diffBuilder.append ( "|" + StringUtil.formatString(ts.getIdentifier().getLocation(),locationFormat) );
		diffBuilder.append ( "|" + StringUtil.formatString(ts.getDescription(),descriptionFormat) );
		diffBuilder.append ( "|" + StringUtil.formatString(ts.getIdentifierString(),tsidFormat) );
		diffBuilder.append ( "|" + StringUtil.formatString(ts.getAlias(),tsaliasFormat) );
		if ( is_diff ) {
			// Summary file only includes differences.
		    summaryBuilder.append ( "|" + StringUtil.formatString((its + 1),"%5d") );
			summaryBuilder.append ( "|" +StringUtil.formatString(ts.getIdentifier().getLocation(), locationFormat) );
			summaryBuilder.append ( "|" +StringUtil.formatString(ts.getDescription(), descriptionFormat) );
			summaryBuilder.append ( "|" + StringUtil.formatString(ts.getIdentifierString(),tsidFormat) );
			summaryBuilder.append ( "|" + StringUtil.formatString(ts.getAlias(),tsaliasFormat) );
		}
		if ( MatchDataType_boolean ) {
			diffBuilder.append ( "|" + StringUtil.formatString(ts.getIdentifier().getType(),datatypeFormat) );
			if ( is_diff ) {
				summaryBuilder.append ( "|" + StringUtil.formatString(ts.getIdentifier().getType(),datatypeFormat) );
			}
		}
		boolean isMatched = false;
		String matched = matchedList.get(its);
		if ( matched.equals("yes") ) {
			isMatched = true;
		}
		else {
			isMatched = false;
		}
		diffBuilder.append ( "|" + StringUtil.formatString(matched, "  %-3.3s  ") );
		if ( is_diff ) {
			summaryBuilder.append ( "|" + StringUtil.formatString(matched, "  %-3.3s  ") );
		}
		
		// If a time series is matched, print out analysis.  Otherwise (not matched or > 1 match) print blanks.
		if ( isMatched ) {
			diffBuilder.append ( "|" + StringUtil.formatString(compare_numvalues.get(its).intValue(),intFormat) );
		}
		else {
			diffBuilder.append ( "|" + intBlank );
		}
		if ( is_diff ) {
			if ( isMatched ) {
				summaryBuilder.append ( "|" + StringUtil.formatString(compare_numvalues.get(its).intValue(),intFormat) );
			}
			else {
				summaryBuilder.append ( "|" + intBlank );
			}
		}
		diffabsavg_array=(double[])compare_diffabsavg.get(its);
		diffavg_array=(double[])compare_diffavg.get(its);
		for ( int it = 0; it < Tolerance_count; it++ ) {
			if ( isMatched ) {
				diffBuilder.append ( "|" + StringUtil.formatString(diffcount_array[it], intFormat) );
			}
			else {
				diffBuilder.append ( "|" + intBlank );
			}
			if ( is_diff ) {
				if ( isMatched ) {
					summaryBuilder.append ( "|" + StringUtil.formatString(diffcount_array[it], intFormat) );
				}
				else {
					summaryBuilder.append ( "|" + intBlank );
				}
			}
			if ( (diffcount_array != null) && (diffcount_array[it] > 0) ) {
				diffBuilder.append ( "|" + StringUtil.formatString(diffabsavg_array[it], doubleFormat) );
				diffBuilder.append ( "|" + StringUtil.formatString(diffavg_array[it], doubleFormat) );
				if ( is_diff ) {
					if ( isMatched ) {
						summaryBuilder.append ( "|" + StringUtil.formatString(diffabsavg_array[it], doubleFormat) );
						summaryBuilder.append ( "|" + StringUtil.formatString(diffavg_array[it], doubleFormat) );
					}
					else {
						summaryBuilder.append ( "|" + doubleBlank );
						summaryBuilder.append ( "|" + doubleBlank );
					}
				}
			}
			else {
			    diffBuilder.append ( "|" + doubleBlank );
				diffBuilder.append ( "|" + doubleBlank );
				if ( is_diff ) {
					summaryBuilder.append ( "|" + doubleBlank );
					summaryBuilder.append ( "|" + doubleBlank );
				}
			}
		}
		if ( is_diff ) {
			if ( compare_diffmax.get(its) == null ) {
				diffBuilder.append ( "|" +  doubleBlank );
				summaryBuilder.append ( "|" + doubleBlank );
			}
			else {
				diffBuilder.append ( "|" + StringUtil.formatString(compare_diffmax.get(its).doubleValue(), doubleFormat) );
				summaryBuilder.append ( "|" + StringUtil.formatString(compare_diffmax.get(its).doubleValue(),doubleFormat) );
			}
			DateTime date = compare_diffmaxdate.get(its);
			if ( date == null ) {
				diffBuilder.append ( "|" + StringUtil.formatString( "", dateFormat) );
				summaryBuilder.append ( "|" + StringUtil.formatString( "", dateFormat) );
			}
			else {
			    diffBuilder.append ( "|" +StringUtil.formatString(compare_diffmaxdate.get(its).toString(),dateFormat) );
				summaryBuilder.append ( "|" +StringUtil.formatString(compare_diffmaxdate.get(its).toString(),dateFormat) );
			}
		}
		else {
		    diffBuilder.append ( "|" + doubleBlank );
			//diffBuilder.append ( "|" + StringUtil.formatString( "",dateFormat) );
			diffBuilder.append ( "|" + dateBlank );
		}
		diffBuilder.append ( "|" + nl ); // Last border.
		if ( is_diff ) {
			summaryBuilder.append ( "|" + nl );	// Last border.
		}
	}
	try {
       	if ( (DifferenceFile_full != null) && !DifferenceFile_full.isEmpty() ) {
			IOUtil.writeFile ( DifferenceFile_full, diffBuilder.toString() );
		}
	}
	catch ( IOException e ) {
		problems.add("Error writing difference file \"" + DifferenceFile_full + "\"");
	}
	try {
       	if ( (SummaryFile_full != null) && !SummaryFile_full.isEmpty() ) {
			IOUtil.writeFile ( SummaryFile_full, summaryBuilder.toString() );
		}
	}
	catch ( IOException e ) {
		problems.add("Error writing summary file \"" + SummaryFile_full + "\"");
	}
	//Message.printStatus ( 2, "", "Summary of differences only (differences are second time " +
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{   // The command will be modified if changed.
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
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
	List<File> list = new ArrayList<>();
	list.addAll ( getOutputFiles() );
	return list;
}

/**
Return the output files generated by this file.  This method is used internally.
*/
private List<File> getOutputFiles ()
{
	return __outputFileList;
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
Parse the command string into a PropList of parameters.
Can't use base class method because of change in parameter names.
@param command A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command parameters are determined to be invalid.
*/
public void parseCommand ( String command )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_level = 2;
	String routine = "CompareFiles_Command.parseCommand", message;

	List<String> tokens = StringUtil.breakStringList ( command, "()", StringUtil.DELIM_SKIP_BLANKS );

	CommandStatus status = getCommandStatus();
	if ( (tokens == null) ) { //|| tokens.size() < 2 ) {}
		message = "Invalid syntax for \"" + command + "\".  Expecting CompareFiles(...).";
		Message.printWarning ( warning_level, routine, message);
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Report the problem to support."));
		throw new InvalidCommandSyntaxException ( message );
	}
	// Get the input needed to process the command...
	if ( tokens.size() > 1 ) {
		try {
		    setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT, tokens.get(1), routine,"," ) );
		}
		catch ( Exception e ) {
			message = "Invalid syntax for \"" + command + "\".  Expecting CompareFiles(...).";
			Message.printWarning ( warning_level, routine, message);
			status.addToLog(CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report the problem to support."));
			throw new InvalidCommandSyntaxException ( message );
		}
	}
	// Update old parameter names to new
	// Change WarnIfDifferent=True to IfDifferent=Warn
	// Change WarnIfSame=True to IfSame=Warn
	PropList props = getCommandParameters();
	String propValue = props.getValue ( "WarnIfDifferent" );
	if ( propValue != null ) {
		if ( propValue.equalsIgnoreCase(_True) ) {
			props.set("IfDifferent",_Warn);
		}
		props.unSet("WarnIfDifferent");
	}
	propValue = props.getValue ( "WarnIfSame" );
	if ( propValue != null ) {
		if ( propValue.equalsIgnoreCase(_True) ) {
			props.set("IfSame",_Warn);
		}
		props.unSet("WarnIfSame");
	}
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
	int log_level = 3; // Level for non-user messages.
	int tslistSize = 0;

	// Clear the output file
	
	this.__outputFileList.clear();
	
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
		status.clearLog(commandPhase);
	}
    
	PropList parameters = getCommandParameters();
	String TSID1 = parameters.getValue ( "TSID1" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TSID1 = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID1);
	}
	String TSID2 = parameters.getValue ( "TSID2" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TSID2 = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID2);
	}
	String EnsembleID1 = parameters.getValue ( "EnsembleID1" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		EnsembleID1 = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID1);
	}
	String EnsembleID2 = parameters.getValue ( "EnsembleID2" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		EnsembleID2 = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID2);
	}
	String MatchLocation = parameters.getValue ( "MatchLocation" );
	String MatchDataType = parameters.getValue ( "MatchDataType" );
	String MatchAlias = parameters.getValue ( "MatchAlias" );
	String Precision = parameters.getValue ( "Precision" ); // What to round data to before comparing.
	String Tolerance = parameters.getValue ( "Tolerance" );
	String CompareFlags = parameters.getValue ( "CompareFlags" );
	boolean compareFlags = false; // Default.
	if ( (CompareFlags != null) && CompareFlags.equalsIgnoreCase("true") ) {
		compareFlags = true;
	}
	String AnalysisStart = parameters.getValue ( "AnalysisStart" );
	String AnalysisEnd = parameters.getValue ( "AnalysisEnd" );
	String DiffFlag = parameters.getValue ( "DiffFlag" );
	if ( (DiffFlag != null) && (DiffFlag.length() == 0) ) {
		// Set to null to indicate no flag in checks below.
		DiffFlag = null;
	}
	String CreateDiffTS = parameters.getValue ( "CreateDiffTS" );
	boolean doCreateDiffTS = false;	// Default.
	boolean doCreateDiffTSAlways = false;	// Default.
	if ( (CreateDiffTS != null) && (CreateDiffTS.equalsIgnoreCase(_True) || CreateDiffTS.equalsIgnoreCase(_IfDifferent)) ) {
		doCreateDiffTS = true;
		doCreateDiffTSAlways = false; // Default if creating diff time series is IfDifferent to avoid bloating memory use.
	    if ( CreateDiffTS.equalsIgnoreCase(_True) ) {
	    	doCreateDiffTSAlways = true;
	    }
	}
	String DifferenceFile = parameters.getValue ( "DifferenceFile" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		DifferenceFile = TSCommandProcessorUtil.expandParameterValue(processor, this, DifferenceFile);
	}
	String SummaryFile = parameters.getValue ( "SummaryFile" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		SummaryFile = TSCommandProcessorUtil.expandParameterValue(processor, this, SummaryFile);
	}
	String TableID = parameters.getValue ( "TableID" );
	if ( commandPhase == CommandPhaseType.RUN ) {
		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
	}
	String DiffCountProperty = parameters.getValue ( "DiffCountProperty" );
	String AllowedDiff = parameters.getValue ( "AllowedDiff" );
	int AllowedDiff_int = 0;
	if ( StringUtil.isInteger(AllowedDiff) ) {
	    AllowedDiff_int = Integer.parseInt(AllowedDiff);
	}
	//String AllowedDiffPerTS = parameters.getValue ( "AllowedDiffPerTS" );
	//int AllowedDiffPerTS_int = 0;
	//if ( StringUtil.isInteger(AllowedDiffPerTS) ) {
	//    AllowedDiffPerTS_int = Integer.parseInt(AllowedDiffPerTS);
	//}
	String IfDifferent = parameters.getValue ( "IfDifferent" );
	if ( (IfDifferent == null) || IfDifferent.isEmpty() ) {
		IfDifferent = _Ignore; // Default.
	}
	String IfSame = parameters.getValue ( "IfSame" );
	if ( (IfSame == null) || IfSame.isEmpty() ) {
		IfSame = _Ignore; // Default.
	}
	//String WarnIfDifferent = parameters.getValue ( "WarnIfDifferent" );
	//String WarnIfSame = parameters.getValue ( "WarnIfSame" );
	// The number of digits after the decimal to use for comparisons:
	// - if Precision is null, no rounding occurs
	int Precision_int = 0;

	boolean MatchLocation_boolean = true; // Default.
	if ( (MatchLocation != null) && MatchLocation.equalsIgnoreCase(_False)){
		MatchLocation_boolean = false;
	}
	boolean MatchDataType_boolean = false; // Default.
	if ( (MatchDataType != null) && MatchDataType.equalsIgnoreCase(_True)){
		MatchDataType_boolean = true;
	}
	boolean MatchAlias_boolean = false; // Default.
	if ( (MatchAlias != null) && MatchAlias.equalsIgnoreCase(_True)){
		MatchAlias_boolean = true;
	}
	/*
	boolean WarnIfDifferent_boolean = false; // Default.
	if ( (WarnIfDifferent != null) && WarnIfDifferent.equalsIgnoreCase(_True)){
		WarnIfDifferent_boolean = true;
	}
	boolean WarnIfSame_boolean = false; // Default.
	if ( (WarnIfSame != null) && WarnIfSame.equalsIgnoreCase(_True)){
		WarnIfSame_boolean = true;
	}
	*/
	double [] Tolerance_double = null;
	String value_format = "%.6f"; // Default.
	if ( (Precision != null) && !Precision.isEmpty() ) {
		Precision_int = Integer.parseInt ( Precision );
		value_format = "%." + Precision_int + "f";
	}
	int Tolerance_count = 0;
	List<String> Tolerance_tokens = null;
	if ( (Tolerance != null) && !Tolerance.isEmpty() ) {
		// The parameter has been specified as a list of one or more numbers.
		Tolerance_tokens = StringUtil.breakStringList(Tolerance,", ",0);
		if ( Tolerance_tokens != null ) {
			Tolerance_count = Tolerance_tokens.size();
		}
		if ( Tolerance_count > 0 ) {
			Tolerance_double = new double[Tolerance_count];
		}
		// Get each tolerance as a number.
		for ( int it = 0; it < Tolerance_count; it++ ) {
			Tolerance_double[it] = Double.parseDouble(Tolerance_tokens.get(it) );
		}
	}
	else {
	    // Default is tolerance of 0.0.
		Tolerance_count = 1;
		Tolerance_double = new double[1];
		Tolerance_double[0] = 0.0;
		Tolerance_tokens = new ArrayList<>(1);
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
			// Warning will have been added above.
			++warning_count;
		}
		try {
			AnalysisEnd_DateTime = TSCommandProcessorUtil.getDateTime ( AnalysisEnd, "AnalysisEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above.
			++warning_count;
		}
    }
    
    // Get the table to process. If null will create below.

    DataTable table = null;
    boolean doTable = false;
    if ( (TableID != null) && !TableID.isEmpty() ) {
        // Get the table to be used as input.
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
        // Input error.
        message = "Insufficient data to run command.";
        status.addToLog ( commandPhase,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }
	
    // Now process.
    
	List<TS> tslist = new ArrayList<>();
	List<TS> tslist2 = null; // Used when comparing 2 ensembles.
	boolean do2Ts = false;
	boolean do2Ensembles = false;
	String DifferenceFile_full = DifferenceFile;
	String SummaryFile_full = SummaryFile;
	if ( commandPhase == CommandPhaseType.RUN ) {
        // Convert to absolute paths.
		if ( (DifferenceFile != null) && !DifferenceFile.isEmpty() ) {
			DifferenceFile_full = IOUtil.verifyPathForOS(
				IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
					TSCommandProcessorUtil.expandParameterValue(processor,this,DifferenceFile)));
		}
		if ( (SummaryFile != null) && !SummaryFile.isEmpty() ) {
			SummaryFile_full = IOUtil.verifyPathForOS(
				IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
					TSCommandProcessorUtil.expandParameterValue(processor,this,SummaryFile)));
		}

		if ( (TSID1 != null) && !TSID1.isEmpty() && (TSID2 != null) && !TSID2.isEmpty() ) {
			// Comparing 2 time series.
			do2Ts = true;
			TS ts = null;
			try {
				PropList request_params = new PropList ( "" );
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
			// Get the second time series.
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
			// Comparing the time series in two ensembles.
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
	        // Get the second ensemble.
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
			// Add the time series from the ensemble - then iterate through the list and get the matching time series from the second ensemble.
			tslist = tsensemble1.getTimeSeriesList(false);
			tslist2 = tsensemble2.getTimeSeriesList(false);
			// Number of time series in ensembles must be the same.
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
			// Comparing all the time series by matching time series with the same identifiers.
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
			message = "Number of time series determined for comparison is < 2.  Not comparing.";
			Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(command_tag, ++warning_count),
			routine, message );
	        status.addToLog ( commandPhase,
	            new CommandLogRecord(CommandStatusType.WARNING,
	                message, "Verify that specified time series exist." ) );
			throw new CommandException ( message );
		}
		// Check to make sure that the intervals are the same.
		List<TS> tslist2Check = new ArrayList<>();
		tslist2Check.addAll(tslist);
		if ( do2Ensembles ) {
			// Also add the list from the second ensemble.
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
	//boolean foundLoc1 = false; // Has the time series already been processed?
	//boolean foundDatatype1 = false;
	//boolean foundAlias1 = false;
	StringBuilder hashKey = new StringBuilder(); // Reused below
	HashMap<String,Boolean> addedMap = new HashMap<>();
	boolean foundMatch = false;
	// If true, add a time series comparison analysis:
	// - first ts1/ts2 match, and ts1 when not matched
	//boolean doAnalysis = false;
	int tsdiff_count = 0; // The number of time series that are different.
	// Lists of data that were processed for the report,
	// matching pairs of time series that are compared and time series that were not matched.
	List<TS> outputTSList = new ArrayList<>();
	// Whether the time series was matched ("yes" or "no")
	List<String> matchedList = new ArrayList<>();
	List<Integer> compare_numvalues = new ArrayList<>();
	List<Double> compare_diffmax = new ArrayList<>();
	List<double []> compare_diffabsavg = new ArrayList<>();
	List<double []> compare_diffavg = new ArrayList<>();
	List<int []> compare_diffcount = new ArrayList<>();
	List<DateTime> compare_diffmaxdate = new ArrayList<>();
	List<TS> difftsList = new ArrayList<>(); // List of difference time series if CreateDiffTS=True|IfDifferent.
	int tsComparisonsTried = 0; // Count of the number of comparisons tried - warn if 0.
	try {
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
           	if ( doTable ) {
	            if ( table == null ) {
	                // Did not find table so is being created in this command.
	                // Create an empty table and set the ID.
	                table = new DataTable();
	                table.setTableID ( TableID );
	                setDiscoveryTable ( table );
	            }
        	}
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
        	if ( doTable ) {
	            if ( table == null ) {
	                // Did not find the table above so create it.
	                table = new DataTable( /*columnList*/ );
	                table.setTableID ( TableID );
	                setupOutputTable(table);
	                Message.printStatus(2, routine, "Was not able to match existing table \"" + TableID + "\" so created new table.");
	                
	                // Set the table in the processor.
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
        	// Loop through the time series:
           	// - for each time series, find its matching time series, by location, ignoring itself
        	// - when a match is found, do the comparison
        	TS ts1, ts2;
		   	TS diffts = null; // Difference time series.
		   	TSIterator tsi;
		   	if ( do2Ts ) {
			   	// Directly comparing so only need to process one comparison.
			   	tslistSize = 1;
		   	}
		   	else {
			   	// Must process all the time series and try to find match for each time series.
			   	tslistSize = tslist.size();
		   	}
		   	double value1_orig; // Data values from each time series, rounded and original.
		   	double value2_orig;
		   	TSData tsdata1 = new TSData(), tsdata2 = new TSData(); // Data points for each time series.
		   	String flag1, flag2; // Data flags for values in each time series.
		   	DiffStats diffStats = new DiffStats();
		   	DateTime date1 = null, date2 = null; // Dates for iteration.
		   	int jts;
		   	// Location, data type, and alias for the first time series in a matched time series pair.
		   	String loc1, loc1Upper, datatype1, datatype1Upper, alias1, alias1Upper;

		   	// TODO smalers 2021-09-09 this is old logic that is fragile if more than 2 time series match
		   	// Keep track of which time series pairs have already been matched:
		   	// - the following three lists are populated when a match is made
		   	// - only the criteria indicated by command parameter MatchLocation,
		   	//   MatchDataType and MatchAlias are actually used to determine the match
		   	//List<String> matchedLocList = new ArrayList<>();
		   	//List<String> matchedDatatypeList = new ArrayList<>();
		   	//List<String> matchedAliasList = new ArrayList<>();
		
		   	// New approach is to populate an integer array with count of matches:
		   	// - match count of 1 is expected (one match for each time series)
		   	// - otherwise will generate warnings
		   	int [] matchCount = new int[tslistSize];
		   	for ( int i = 0; i < tslistSize; i++ ) {
			   	matchCount[i] = 0;
		   	}
		   	
		   	// Prepare strings that are used to match time series:
		   	// - create arrays of upper case strings so that 
		   	String [] locUpperArray = new String[0];
		   	String [] dataTypeUpperArray = new String[0];
		   	String [] aliasUpperArray = new String[0];
		   	if ( !do2Ts ) {
		   		// Have a list of time series that is processed below.
		   		locUpperArray = new String[tslistSize];
		   		dataTypeUpperArray = new String[tslistSize];
		   		aliasUpperArray = new String[tslistSize];
		   		TS ts = null;
		   		for ( int its = 0; its < tslistSize; its++ ) {
		   			ts = tslist.get(its);
		   			locUpperArray[its] = ts.getLocation().toUpperCase();
		   			dataTypeUpperArray[its] = ts.getDataType().toUpperCase();
		   			aliasUpperArray[its] = ts.getAlias();
		   		}
		   	}
		
		   	// Loop through all time series.
		   	for ( int its = 0; its < tslistSize; its++ ) {
			   	diffts = null;
			   	ts1 = tslist.get(its);
			   	ts2 = null;
			   	loc1 = ts1.getLocation();
			   	datatype1 = ts1.getDataType();
			   	alias1 = ts1.getAlias();
			   	if ( do2Ts ) {
				   	// Only have two time series so always a match.
				   	foundMatch = true;
				   	ts1 = tslist.get(0);
				   	ts2 = tslist.get(1);
				   	++matchCount[its];
			   	}
			   	else if ( do2Ensembles ) {
				   	// Get one time series from each ensemble.
				   	foundMatch = true;
				   	ts1 = tslist.get(its);
				   	ts2 = tslist2.get(its);
				   	++matchCount[its];
			   	}
			   	else {
				   	// Processing many time series.
				   	// Try to pair up time series based on location and data type
				   	// Make sure not to analyze the same combination more than once, based on the location ID and data type.
				   	// TODO SAM 2005-05-25 Also need to check interval always.
				   	foundMatch = false;
				   	// Get the upper case strings used for comparisons (use normal strings for output).
				   	loc1Upper = locUpperArray[its];
			   		datatype1Upper = dataTypeUpperArray[its];
			   		alias1Upper = aliasUpperArray[its];
				   	//boolean alreadyProcessed = false;
				
				   	// Determine if the time series has already been matched.
				   	/*
				   	for ( int iMatchedTs = 0; iMatchedTs < matchedLocList.size(); iMatchedTs++ ) {
					   	// TODO SAM 2016-03-23 This needs review - is there a more robust way to deal with this?
					   	// These checks are to see if the time series has already been processed.
					   	// Check the parts individually.
					   	foundLoc1 = false;
					   	foundDatatype1 = false;
					   	foundAlias1 = false;
					   	if ( loc1.equalsIgnoreCase( matchedLocList.get(iMatchedTs)) ) {
						   	foundLoc1 = true;
					   	}
					   	if ( datatype1.equalsIgnoreCase(matchedDatatypeList.get(iMatchedTs))) {
						   	foundDatatype1 = true;
					   	}
					   	if ( alias1.equalsIgnoreCase(matchedAliasList.get(iMatchedTs))) {
						   	foundAlias1 = true;
					   	}
					   	// Now reset found_loc1 depending on whether the location and datatype (one or both) are being matched.
					   	if ( MatchLocation_boolean && MatchDataType_boolean ) {
						   	// Need to check both.
						   	if ( foundLoc1 && foundDatatype1 ) {
							   	alreadyProcessed = true;
							   	break;
						   	}
					   	}
					   	else if ( MatchLocation_boolean && foundLoc1 ) {
						   	alreadyProcessed = true;
						   	break;
					   	}
					   	else if ( MatchDataType_boolean && foundDatatype1 ) {
						   	alreadyProcessed = true;
						   	break;
					   	}
				   	}
				   	if ( alreadyProcessed ) {
					   	// Have already processed this time series.
					   	continue;
				   	}
				   	*/
				   	// Try to find a matching location:
				   	// - search the entire list to catch duplicate matches
				   	for ( jts = 0; jts < tslistSize; jts++ ) {
					   	if ( its == jts ) {
						   	// Don't compare a time series with itself.
						   	continue;
					   	}
					   	TS ts0 = tslist.get(jts);
					   	// Make sure that the interval is the same.
					   	if ( !((ts1.getDataIntervalBase() == ts0.getDataIntervalBase()) &&
						   	(ts1.getDataIntervalMult() == ts0.getDataIntervalMult())) ) {
						   	// Intervals do not match.
						   	continue;
					   	}
					   	// Check the requested criteria for matches.
					   	if ( MatchLocation_boolean && !loc1Upper.equals(locUpperArray[jts]) ) {
						   	// Not a match.
						   	continue;
					   	}
					   	if ( MatchDataType_boolean && !datatype1Upper.equals(dataTypeUpperArray[jts]) ) {
						   	// Not a match.
						   	continue;
					   	}
					   	if ( MatchAlias_boolean && !alias1Upper.equals(aliasUpperArray[jts]) ) {
						   	// Not a match.
						   	continue;
					   	}
					   	// If here, have a match based on the match criteria;
					   	// - further handle cases of more than 2 matches (match criteria are not specific enough)
					   	foundMatch = true;
					   	// Last matching time series will be used:
					   	// - if one match then analysis is done
					   	// - if > 1 match, then no analysis is done
					   	ts2 = ts0;
					   	// Track the matched time series.
					   	++matchCount[its];
					   	/*
				   		if ( matchedIndex[its] >= 0 ) {
				   			// In the second time series group.
				   			// Matched but do not want to add to the analysis because it will be a duplicate (ts2 and ts1).
				   			Message.printStatus(2, routine, "Matched time series its=" + its + " matchedIndex=" + matchedIndex[its] +
				   				" jts=" + jts + " matchedIndex=" + matchedIndex[jts] + " - ts1 already analyzed, tsid=" + ts1.getIdentifierString() );
				   			doAnalysis = false;
				   		}
				   		else if ( (matchedIndex[its] < 0) && (matchedIndex[its] != -size) ) {
				   			// The first time series has already been indicated has having more than one match.
				   			// Matched but do not want to add to the analysis because it will be a duplicate.
				   			// This probably should not happen since it is handled better in the next case.
				   			Message.printStatus(2, routine, "Matched time series its=" + its + " matchedIndex=" + matchedIndex[its] +
				   				" jts=" + jts + " matchedIndex=" + matchedIndex[jts] + " - ts1 already analyzed (duplicate), tsid=" + ts1.getIdentifierString() );
				   			doAnalysis = false;
				   		}
				   		else if ( matchedIndex[jts] >= 0 ) {
					   		// The time series is already involved in a match:
				   			// - typically because in the first group of time series and the match criteria results in duplicates
					   		// - set the match index to negative index of the match
				   			// - this works for all cases except when the index is 0
				   			// - don't add to the analysis results, but the negative index will cause a command warning
				   			Message.printStatus(2, routine, "Matched time series its=" + its + " matchedIndex=" + matchedIndex[its] +
				   				" jts=" + jts + " matchedIndex=" + matchedIndex[jts] + " - ts2 already analyzed (duplicate), tsid=" + ts1.getIdentifierString() );
					   		matchedIndex[its] = -jts;
					   	    doAnalysis = false;
				   		}
				   		else {
					   		// First time a match was found so save the indices of each matched time series.
				   			// This is the only case where an analysis is performed below.
				   			Message.printStatus(2, routine, "Matched time series its=" + its + " matchedIndex=" + matchedIndex[its] +
				   				" jts=" + jts + " matchedIndex=" + matchedIndex[jts] + " - new match so analyzing, tsid=" + ts1.getIdentifierString() );
					   		matchedIndex[its] = jts;
					   		matchedIndex[jts] = its;
					   	    doAnalysis = true;
				   		}
				   		*/
				   		// Break out of loop since have a match.
				   		//if ( foundMatch ) {
				   			//break;
				   		//}
				   	} // End jts search for match
				   	
			   	}
			   	// Figure out if the time series has already been added.
			   	if ( foundMatch ) {
			   		hashKey.setLength(0);
			   		if ( MatchLocation_boolean ) {
			   			hashKey.append(ts1.getLocation());
			   		}
			   		if ( MatchDataType_boolean ) {
			   			if ( hashKey.length() > 0 ) {
			   				hashKey.append("-");
			   			}
			   			hashKey.append(ts1.getDataType());
			   		}
			   		if ( MatchAlias_boolean ) {
			   			if ( hashKey.length() > 0 ) {
			   				hashKey.append("-");
			   			}
			   			hashKey.append(ts1.getAlias());
			   		}
			   		if ( addedMap.get(hashKey.toString()) != null ) {
			   			// Already added so don't do it again.
			   			continue;
			   		}
			   		else {
			   			// Have not added so add to the hash.
			   			addedMap.put(hashKey.toString(), Boolean.TRUE);
			   		}
			   	}
			   	// If here the time series may or may not have been a match:
			   	// - add with analysis if a clean match
			   	// - add with no analysis if no match
			   	// - add with no analysis if > 1 time series matched
			   	if ( foundMatch && (matchCount[its] == 1) ) {
				   	// Have a match so do the comparison:
				   	// - currently only compare the data values and flags but not the metadata
			   		// - only add to outputTSList for ts1 in ts1/ts2 pair or ts1 no match (don't add ts2 as match when ts1 was already added)

				   	// TODO smalers 2021-09-09 old code, remove when checked out.
				   	// Add locations and data types that have been processed so they are not found again.
				   	//matchedLocList.add ( loc1 );
				   	//matchedDatatypeList.add ( datatype1 );
				   	//matchedAliasList.add ( alias1 );
				   	// If the differences will be flagged, allocate the data flag space in both time series.
				   	if ( DiffFlag != null ) {
					   	ts1.allocateDataFlagSpace ( null, true ); // Retain previous.
					   	ts2.allocateDataFlagSpace ( null, true ); // Retain previous.
				   	}
				   	// If a difference time series is to be created, do it up front:
				   	// - this ensures that it is available when differences are detected
				   	// - only add to the output list later if appropriate (discard if CreateDiffTS=IfDifferent and no differences)
				   	// - unused time series will not be allocated memory and will be garbage collected
				   	if ( doCreateDiffTS ) {
					   	diffts = createDiffTimeSeries ( ts2 );
				   	}
				   	// Initialize for the comparison.
				   	diffStats.totalcount = 0;
				   	diffStats.diffmaxabs = -1.0e10;
				   	// Reallocate in order to have unique references to save for the report.
				   	diffcount = new int[Tolerance_count];
				   	diffabsavg = new double[Tolerance_count];
				   	diffavg = new double[Tolerance_count];
				   	for ( int it = 0; it < Tolerance_count; it++ ) {
					   	diffcount[it] = 0;
					   	difftotal[it] = 0.0;
					   	difftotalabs[it] = 0.0;
				   	}
				   	// Analysis period should encompass the period from both time series.
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
				   	ts1.getIdentifierString();
				   	Message.printStatus ( 2, routine, "TS1 = " + ts1.getIdentifier().toString(true) + ", alias = " + ts1.getAlias() );
				   	Message.printStatus ( 2, routine, "TS2 = " + ts2.getIdentifier().toString(true) + ", alias = " + ts2.getAlias() );
				   	Message.printStatus ( 2, routine, "Data differences TS2 - TS1 follow " +
					   	"(Tolerance=" + Tolerance + ", Period="+ date1 + " to " + date2 + "):" );
				   	// Increment counter indicating that a test was tried.
				   	++tsComparisonsTried;
				   	boolean doSetTz1 = false;
				   	boolean doSetTz2 = false;
				   	if ( !TimeInterval.isRegularInterval(ts1.getDataIntervalBase()) ||
					   	!TimeInterval.isRegularInterval(ts2.getDataIntervalBase()) ) {
					   	// One or both of the time series are irregular.
					   	// It is difficult to iterate because timestamps may not align.
					   	// Therefore, retrieve all the date/times from each time series,
					   	// sort them, and iterate using the overall list of date/times.
					   	List<TS> tempList = new ArrayList<>();
					   	tempList.add(ts1);
					   	tempList.add(ts2);
					   	// Time zone is not included in the following list.
					   	List<DateTime> dateTimeList = new ArrayList<>();
					   	if ( (date1 != null) && (date2 != null) ) {
					   		dateTimeList = TSUtil.createTSDateTimeList ( tempList, date1, date2 );
					   	}
					   	// Because value lookups do a time comparison, the time zone in the iterator must be ignored in each time series
					   	// do this by making sure the lookup time zone is the same as in the time series.
					   	String tz1 = "";
					   	if ( ts1.getDate1() != null ) {
					   		tz1 = ts1.getDate1().getTimeZoneAbbreviation();
					   	}
					   	String tz2 = "";
					   	if ( ts2.getDate1() != null ) {
					   		tz2 = ts2.getDate1().getTimeZoneAbbreviation();
					   	}
					   	if ( dateTimeList.size() > 0 ) {
					   		if ( dateTimeList.get(0).getTimeZoneAbbreviation().isEmpty() ) {
					   			if ( !tz1.isEmpty() ) {
					   				doSetTz1 = true;
					   			}
					   			if ( !tz2.isEmpty() ) {
					   				doSetTz2 = true;
					   			}
					   		}
					   	}
					   	if ( Message.isDebugOn ) {
					   		Message.printStatus(2,routine,"Have " + dateTimeList.size() + " irregular time series date/times to compare.");
					   	}
					   	for ( DateTime dt : dateTimeList ) {
						   	if ( doSetTz1 ) {
						   		// Make sure time zone matches the time one so lookup will find data.
						   		dt.setTimeZone(ts1.getDate1().getTimeZoneAbbreviation());
						   	}
						   	tsdata1 = ts1.getDataPoint ( dt, tsdata1 );
						   	value1_orig = tsdata1.getDataValue ();
						   	flag1 = tsdata1.getDataFlag().trim();
						   	if ( Message.isDebugOn ) {
						   		Message.printStatus(2,routine,"DateTime=" + dt + " value1=" + value1_orig);
						   	}
						   	if ( doSetTz2 ) {
						   		// Make sure time zone matches the time zone so lookup will find data.
						   		dt.setTimeZone(ts2.getDate2().getTimeZoneAbbreviation());
						   	}
						   	tsdata2 = ts2.getDataPoint ( dt, tsdata2 );
						   	value2_orig = tsdata2.getDataValue ();
						   	flag2 = tsdata2.getDataFlag().trim();
						   	if ( Message.isDebugOn ) {
						   		Message.printStatus(2,routine,"DateTime=" + dt + " value2=" + value2_orig);
						   	}
						   	compareTimeSeriesValues(
							   	routine,
							   	ts1, loc1, ts2,
							   	compareFlags, DiffFlag, Precision, Precision_int, value_format, Tolerance_count, Tolerance_double,
							   	dt, value1_orig, tsdata1, flag1, value2_orig, tsdata2, flag2,
							   	diffcount, difftotal, difftotalabs,
							   	doCreateDiffTS, doCreateDiffTSAlways, diffts,
							   	doTable, table,
							   	diffStats  );
					   	}
				   	} // End if irregular interval time series.
				   	else {
					   	// Both time series are regular interval and should align.
					   	// Iterate using the first time series.
					   	tsi = ts1.iterator(date1,date2);
					   	// Because value lookups do a time comparison, the time zone in the iterator must be ignored in each time series
					   	// do this by making sure the lookup time zone is the same as in the time series.
					   	String tz1 = "";
					   	if ( ts1.getDate1() != null ) {
					   		tz1 = ts1.getDate1().getTimeZoneAbbreviation();
					   	}
					   	String tz2 = "";
					   	if ( ts2.getDate1() != null ) {
					   		tz2 = ts2.getDate1().getTimeZoneAbbreviation();
					   	}
					   	if ( !tz1.equals(tz2) ) {
					   		// Time zones don't match so will need to set in second time series.
					   		doSetTz2 = true;
					   	}
					   	DateTime dt = null;
					   	for ( ; tsi.next() != null; ) {
						   	dt = tsi.getDate();
						   	// This is not overly efficient but currently the iterator does not have a way to set a data point.
						   	if ( doSetTz2 ) {
						   		// Set the time zone of the iterator to that in the time series to ensure match.
						   		dt.setTimeZone(ts1.getDate1().getTimeZoneAbbreviation());
						   	}
						   	tsdata1 = ts1.getDataPoint ( dt, tsdata1 );
						   	value1_orig = tsdata1.getDataValue ();
						   	flag1 = tsdata1.getDataFlag().trim();
						   	if ( Message.isDebugOn ) {
						   		Message.printStatus(2,routine,"DateTime=" + dt + " value1=" + value1_orig);
						   	}
						   	if ( doSetTz2 ) {
						   		// Set the time zone of the iterator to that in the time series to ensure match.
						   		dt.setTimeZone(ts2.getDate1().getTimeZoneAbbreviation());
						   	}
						   	tsdata2 = ts2.getDataPoint ( dt, tsdata2 );
						   	value2_orig = tsdata2.getDataValue ();
						   	flag2 = tsdata2.getDataFlag().trim();
						   	if ( Message.isDebugOn ) {
						   		Message.printStatus(2,routine,"DateTime=" + dt + " value2=" + value2_orig);
						   	}
						   	compareTimeSeriesValues(
							   	routine,
							   	ts1, loc1, ts2,
							   	compareFlags, DiffFlag, Precision, Precision_int, value_format, Tolerance_count, Tolerance_double,
							   	dt, value1_orig, tsdata1, flag1, value2_orig, tsdata2, flag2,
							   	diffcount, difftotal, difftotalabs,
							   	doCreateDiffTS, doCreateDiffTSAlways, diffts,
							   	doTable, table,
							   	diffStats );
					   	}
				   	} // End if regular interval time series.
				   	// Output status messages for summary of differences.
				   	is_diff = false;
				   	for ( int it = 0; it < Tolerance_count; it++ ) {
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
				   	// Add the difference time series:
				   	// - if always adding
				   	// - if NOT always adding and there is a difference
				   	if ( doCreateDiffTS ) {
					   	if ( doCreateDiffTSAlways || (!doCreateDiffTSAlways && is_diff) ) {
						   	difftsList.add ( diffts );
					   	}
				   	}
				   	if ( is_diff ) {
					   	Message.printStatus ( 2, routine, loc1 + " maximum difference = " +
					   	StringUtil.formatString(diffStats.diffmax, value_format) +" earliest on "+ diffStats.diffmax_DateTime );
					   	if ( IfDifferent.equalsIgnoreCase(_Warn) || IfDifferent.equalsIgnoreCase(_Fail) ) {
						   	message = "Time series for " + ts1.getIdentifier() + " have differences.";
						   	Message.printWarning (warning_level,
						   	MessageUtil.formatMessageTag(command_tag,++warning_count),routine, message );
					   	}
					   	++tsdiff_count;
				   	}
				   	else {
				       	Message.printStatus ( 2, routine, loc1 + " had no differences." );
				   	}
				   	// Save information for the report.
				   	outputTSList.add ( ts1 );
				   	matchedList.add ( "yes" );
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
			   	} // End time series matched.
			   	else if ( ! foundMatch || (matchCount[its] > 1) ) {
				   	// Did not match a time series:
				   	// - add it to the list to make sure that nothing falls through the cracks
				   	// - save information for the report
				   	// - treat as if different
				   	outputTSList.add ( ts1 );
				   	if ( !foundMatch ) {
				   		matchedList.add ( "no" );
				   		Message.printStatus(2,routine, "Did not find match for time series " + (its + 1) + " location=\"" + loc1 +
				   			"\" and datatype=\"" + datatype1 + "\" - added as time series with differences (size=" + outputTSList.size() + ").");
				   	}
				   	else if ( matchCount[its] > 1 ) {
				   		matchedList.add ( "1+" );
				   		Message.printStatus(2,routine, "Found " + matchCount[its] + " matches for time series " + (its + 1) + " location=\"" + loc1 +
				   			"\" and datatype=\"" + datatype1 + "\" - added as time series with differences (size=" + outputTSList.size() + ").");
				   	}
				   	int ndata = ts1.getDataSize();
				   	int [] idata = new int[Tolerance_count];
				   	for ( int it = 0; it < Tolerance_count; it++ ) {
					   	idata[it] = ndata;
				   	}
				   	double [] ddata = new double[Tolerance_count];
				   	compare_numvalues.add (	new Integer(ndata) );
				   	compare_diffcount.add ( diffcount );
				   	compare_diffabsavg.add ( ddata );
				   	compare_diffavg.add ( ddata );
				   	compare_diffmax.add ( null );
				   	compare_diffmaxdate.add ( null );
			   	} // End no match.
		   	} // End loop on time series.

		   	Message.printStatus ( 2, routine, "" + tsdiff_count + " of " + tslistSize + " time series had differences." );
		   	// else print a warning and throw an exception below.

		   	// Print a summary of the comparison.

		   	List<String> problems = new ArrayList<>();
		   	createReportFiles (
			   	outputTSList,
			   	DifferenceFile_full,
			   	SummaryFile_full,
			   	MatchDataType_boolean,
			   	matchedList,
			   	Tolerance_count, Tolerance_tokens,
			   	compare_diffcount, compare_numvalues,
			   	compare_diffmax, compare_diffmaxdate,
			   	compare_diffabsavg, compare_diffavg,
			   	problems
		   	);

		   	// Do the calculation so it can be used in output.
            int diffCountTotal = 0;
            for ( int its = 0; its < diffcount.length; its++ ) {
              	diffCountTotal += diffcount[its];
            }

		   	// Output summary at the top of log messages.

          	// The AllowedDiff value impacts the status.
          	//if ( (tsdiff_count > 0) && (diffCountTotal > AllowedDiff_int) ) {
          	if ( tsdiff_count > 0 ) {
			    CommandStatusType statusType = CommandStatusType.UNKNOWN;
			    if ( IfDifferent.equalsIgnoreCase(_Warn) ) {
			        statusType = CommandStatusType.WARNING;
			    }
			    else if ( IfDifferent.equalsIgnoreCase(_Fail) ) {
			        statusType = CommandStatusType.FAILURE;
			    }
			    else {
			        statusType = CommandStatusType.INFO;
			    }
           	    if ( diffCountTotal <= AllowedDiff_int ) {
           	    	// Allow the difference without errors:
           	    	// - reset to INFO
           	    	message = "" + tsdiff_count + " time series had differences for " +
			        	outputTSList.size() +
			        	" analyzed time series (matches and unmatched time series from " +
			        	tslistSize + " input time series) - not using " + statusType +
			        	" because total number of differences (" + diffCountTotal +
			        	") is <= AllowedDiff (" + AllowedDiff_int + ").";
			        statusType = CommandStatusType.INFO;
           	    }
           	    else {
           	    	message = "" + tsdiff_count + " time series had differences for " +
			        	outputTSList.size() +
			        	" analyzed time series (matches and unmatched time series from " +
			        	tslistSize + " input time series).  Total number of differences = " + diffCountTotal + ".";
           	    }
			    Message.printWarning ( warning_level,
			    MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
	       	    status.addToLog ( commandPhase,
	                new CommandLogRecord(statusType,
	           	        message, "Verify that time series differences are expected." ) );
		    }
          	else if ( tsdiff_count == 0 ) {
			    CommandStatusType statusType = CommandStatusType.UNKNOWN;
			    if ( IfSame.equalsIgnoreCase(_Warn) ) {
			       statusType = CommandStatusType.WARNING;
			    }
			    else if ( IfSame.equalsIgnoreCase(_Fail) ) {
			        statusType = CommandStatusType.FAILURE;
			    }
			    else {
			        statusType = CommandStatusType.INFO;
			    }
			    if ( statusType != CommandStatusType.INFO ) {
			       	message = "All " + tslistSize + " time series are the same.";
			      	Message.printWarning ( warning_level,
			       	MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
	       	       	status.addToLog ( commandPhase,
	                   	new CommandLogRecord(statusType,
	                   		message, "Verify that all time seres being the same is OK." ) );
			    }
		    }

           	// Another summary message.

		   	if ( tsComparisonsTried == 0 ) {
		       	message = "No time series comparisons occurred based on the input parameters.";
               	Message.printWarning ( warning_level, 
                   	MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
               	status.addToLog ( commandPhase,
                   	new CommandLogRecord(CommandStatusType.FAILURE,
                       	message, "Verify that the location, data type, etc. " +
                    	"parameters result in pairs of time series to compare." ) );
		   	}
           	
           	// Output detailed messages.

		   	for ( String problem : problems ) {
			   	Message.printWarning ( warning_level, 
				   	MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, problem );
               	status.addToLog ( commandPhase,
                   	new CommandLogRecord(CommandStatusType.WARNING,
                       	problem, "Check the path to the file and permissions." ) );
		   	}

		   	if ( doCreateDiffTS && (difftsList.size() > 0) ) {
			   	// Have difference time series to add to the processor.
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
			   	// Add the difference time series.
			   	tslist.addAll(difftsList);
			   	try {
				   	// TODO smalers 2021-08-27 need to use an append request.
				   	// Reset the processor results.
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
		   	} // doCreateDiffTS

	       	// Set the property indicating the number of rows in the table.
           	if ( (DiffCountProperty != null) && !DiffCountProperty.isEmpty() && (diffcount != null) ) {
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
           	
           	// Check the matched indices:
           	// - may result in a large number of messages if paired time series were not analyzed
           	// - warn for cases where more than 2 time series in a match
           	// - optionally warn if no match
           	for ( int its = 0; its < matchCount.length; its++ ) {
           		TS ts = tslist.get(its);
           		if ( matchCount[its] == 0 ) {
           			// No match.
                   	message = "Time series " + (its + 1) + " had no match for criteria, TSID=" +
                   		ts.getIdentifier() + " Alias=" + ts.getAlias();
                   	Message.printWarning(log_level,
                       	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                       	routine, message );
                   	status.addToLog ( CommandPhaseType.RUN,
                       	new CommandLogRecord(CommandStatusType.WARNING,
                           	message, "Check time series input to ensure matched datasets." ) );
           		}
           		else if ( matchCount[its] > 1 ) {
           			// More than 2 time series in a match.
                   	message = "Time series " + (its + 1) + " had " + matchCount[its] +
                   		" matches for criteria (1 match is expected), TSID=" +
                   		ts.getIdentifier() + " Alias=" + ts.getAlias();
                   	Message.printWarning(log_level,
                       	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                       	routine, message );
                   	status.addToLog ( CommandPhaseType.RUN,
                       	new CommandLogRecord(CommandStatusType.WARNING,
                           	message, "Check time series input to ensure matched datasets.  Add more specific match criteria to ensure unique identifiers." ) );
           		}
           	}

            // Save the output file names.
        	if ( (DifferenceFile_full != null) && !DifferenceFile_full.isEmpty() ) {
        		this.__outputFileList.add ( new File(DifferenceFile_full));
        	}
        	if ( (SummaryFile_full != null) && !SummaryFile_full.isEmpty() ) {
        		this.__outputFileList.add ( new File(SummaryFile_full));
        	}
        } // If commandPhaseType is RUN.
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
		// Not found so create it.
		__tableDateTimeColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_DATETIME, tableDateTimeColumn, -1, -1), "");
	}
	try {
		if ( (tableTSID1Column != null) && !tableTSID1Column.isEmpty() ) {
			__tableTSID1ColumnNum = table.getFieldIndex(tableTSID1Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it.
		__tableTSID1ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_STRING, tableTSID1Column, -1, -1), "");
	}
	try {
		if ( (tableTSID2Column != null) && !tableTSID2Column.isEmpty() ) {
			__tableTSID2ColumnNum = table.getFieldIndex(tableTSID2Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it.
		__tableTSID2ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_STRING, tableTSID2Column, -1, -1), "");
	}
	try {
		if ( (tableValue1Column != null) && !tableValue1Column.isEmpty() ) {
			__tableValue1ColumnNum = table.getFieldIndex(tableValue1Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it - use 4 digits of precision for the check output.
		__tableValue1ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_DOUBLE, tableValue1Column, -1, 4), "");
	}
	try {
		if ( (tableFlag1Column != null) && !tableFlag1Column.isEmpty() ) {
			__tableFlag1ColumnNum = table.getFieldIndex(tableFlag1Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it.
		__tableFlag1ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_STRING, tableFlag1Column, -1, -1), "");
	}
	try {
		if ( (tableValue2Column != null) && !tableValue2Column.isEmpty() ) {
			__tableValue2ColumnNum = table.getFieldIndex(tableValue2Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it - use 4 digits of precision for the check output.
		__tableValue2ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_DOUBLE, tableValue2Column, -1, 4), "");
	}
	try {
		if ( (tableFlag2Column != null) && !tableFlag2Column.isEmpty() ) {
			__tableFlag2ColumnNum = table.getFieldIndex(tableFlag2Column);
		}
	}
	catch ( Exception e ) {
		// Not found so create it.
		__tableFlag2ColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_STRING, tableFlag2Column, -1, -1), "");
	}
	try {
		if ( (tableDiffColumn != null) && !tableDiffColumn.isEmpty() ) {
			__tableDiffColumnNum = table.getFieldIndex(tableDiffColumn);
		}
	}
	catch ( Exception e ) {
		// Not found so create it - use 4 digits of precision for the check output.
		__tableDiffColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_DOUBLE, tableDiffColumn, -1, 4), "");
	}
	try {
		if ( (tableDiffPercentColumn != null) && !tableDiffPercentColumn.isEmpty() ) {
			__tableDiffPercentColumnNum = table.getFieldIndex(tableDiffPercentColumn);
		}
	}
	catch ( Exception e ) {
		// Not found so create it - use 4 digits of precision for the check output.
		__tableDiffPercentColumnNum = table.addField(-1, new TableField(TableField.DATA_TYPE_DOUBLE, tableDiffPercentColumn, -1, 4), "");
	}
	try {
		if ( (tableCommentColumn != null) && !tableCommentColumn.isEmpty() ) {
			__tableCommentColumnNum = table.getFieldIndex(tableCommentColumn);
		}
	}
	catch ( Exception e ) {
		// Not found so create it.
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
	String MatchAlias = props.getValue("MatchAlias");
	String Precision = props.getValue("Precision");
	String Tolerance = props.getValue("Tolerance");
	String CompareFlags = props.getValue("CompareFlags");
	String AnalysisStart = props.getValue("AnalysisStart");
	String AnalysisEnd = props.getValue("AnalysisEnd");
	String DiffFlag = props.getValue("DiffFlag");
	String CreateDiffTS = props.getValue("CreateDiffTS");
	String DifferenceFile = props.getValue ( "DifferenceFile" );
	String SummaryFile = props.getValue ( "SummaryFile" );
	String TableID = props.getValue ( "TableID" );
	String DiffCountProperty = props.getValue ( "DiffCountProperty" );
	String AllowedDiff = props.getValue("AllowedDiff");
	//String AllowedDiffPerTS = props.getValue("AllowedDiffPerTS");
	String IfDifferent = props.getValue("IfDifferent");
	String IfSame = props.getValue("IfSame");
	//String WarnIfDifferent = props.getValue("WarnIfDifferent");
	//String WarnIfSame = props.getValue("WarnIfSame");
	StringBuilder b = new StringBuilder ();
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
	if ( (MatchAlias != null) && (MatchAlias.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "MatchAlias=" + MatchAlias );
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
    if ( (DifferenceFile != null) && (DifferenceFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DifferenceFile=\"" + DifferenceFile + "\"" );
    }
    if ( (SummaryFile != null) && (SummaryFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SummaryFile=\"" + SummaryFile + "\"" );
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
    if ( (AllowedDiff != null) && (AllowedDiff.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AllowedDiff=\"" + AllowedDiff + "\"" );
    }
    /*
    if ( (AllowedDiffPerTS != null) && (AllowedDiffPerTS.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AllowedDiffPerTS=\"" + AllowedDiffPerTS + "\"" );
    }
    */
	if ( (IfDifferent != null) && (IfDifferent.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfDifferent=" + IfDifferent );
	}
	if ( (IfSame != null) && (IfSame.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfSame=" + IfSame );
	}
	/*
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
	*/
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