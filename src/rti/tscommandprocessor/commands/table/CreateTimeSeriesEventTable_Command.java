// CreateTimeSeriesEventTable_Command - This class initializes, checks, and runs the CreateTimeSeriesEventTable() command.

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

package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import RTi.GRTS.TimeSeriesEvent;
import RTi.GRTS.TimeSeriesEventAnnotationCreator;
import RTi.TS.IrregularTSIterator;
import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSIterator;
import RTi.TS.TSLimits;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
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
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeFormatterType;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the CreateTimeSeriesEventTable() command.
*/
public class CreateTimeSeriesEventTable_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
The table that is created.
*/
private DataTable __table = null;

/**
Constructor.
*/
public CreateTimeSeriesEventTable_Command ()
{	super();
	setCommandName ( "CreateTimeSeriesEventTable" );
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
{	String TimeSeriesLocations = parameters.getValue ( "TimeSeriesLocations" );
    String TableID = parameters.getValue ( "TableID" );
    String NewTableID = parameters.getValue ( "NewTableID" );
    String InputTableEventIDColumn = parameters.getValue ( "InputTableEventIDColumn" );
    String InputTableEventTypeColumn = parameters.getValue ( "InputTableEventTypeColumn" );
    String InputTableEventStartColumn = parameters.getValue ( "InputTableEventStartColumn" );
    String InputTableEventEndColumn = parameters.getValue ( "InputTableEventEndColumn" );
    String InputTableEventLocationColumns = parameters.getValue ( "InputTableEventLocationColumns" );
    String InputTableEventLabelColumn = parameters.getValue ( "InputTableEventLabelColumn" );
    String InputTableEventDescriptionColumn = parameters.getValue ( "InputTableEventDescriptionColumn" );
    String OutputTableTSIDColumn = parameters.getValue ( "OutputTableTSIDColumn" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    /*
    if ( (TableID == null) || (TableID.length() == 0) ) {
        // TODO SAM 2013-09-08 This is required now but will be optional when other options are available
        // for creating the time series events.
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }
    */
    
    if ( (TableID != null) && (TableID.length() != 0) && (NewTableID != null) && (NewTableID.length() != 0) &&
        TableID.equalsIgnoreCase(NewTableID) ) {
        message = "The original and new table identifiers are the same.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the new table identifier different from the original table identifier." ) );
    }
    
    if ( (TableID != null) && (TableID.length() > 0) ) {
        // An event table has been specified as input so make sure that all the required table columns are specified.
        if ( (TimeSeriesLocations == null) || (TimeSeriesLocations.length() == 0) ) {
            message = "The time series location parameter must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the time series location parameter." ) );
        }
        if ( (InputTableEventIDColumn == null) || (InputTableEventIDColumn.length() == 0) ) {
            message = "The input table event ID column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event ID column." ) );
        }
        if ( (InputTableEventTypeColumn == null) || (InputTableEventTypeColumn.length() == 0) ) {
            message = "The input table event type column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event type column." ) );
        }
        if ( (InputTableEventStartColumn == null) || (InputTableEventStartColumn.length() == 0) ) {
            message = "The input table event start column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event start column." ) );
        }
        if ( (InputTableEventEndColumn == null) || (InputTableEventEndColumn.length() == 0) ) {
            message = "The input table event end column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event end column." ) );
        }
        if ( (InputTableEventLocationColumns == null) || (InputTableEventLocationColumns.length() == 0) ) {
            message = "The input table event location columns must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event location columns." ) );
        }
        if ( (InputTableEventLabelColumn == null) || (InputTableEventLabelColumn.length() == 0) ) {
            message = "The input table event label column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event label column." ) );
        }
        if ( (InputTableEventDescriptionColumn == null) || (InputTableEventDescriptionColumn.length() == 0) ) {
            message = "The input table event description column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the input table event description column." ) );
        }
    }
    
    if ( (NewTableID == null) || (NewTableID.length() == 0) ) {
        message = "The new table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the new table identifier." ) );
    }
    
    if ( (OutputTableTSIDColumn == null) || (OutputTableTSIDColumn.length() == 0) ) {
        message = "The output table TSID column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the output table TSID column." ) );
    }
 
	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(17);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "TimeSeriesLocations" );
    validList.add ( "TableID" );
    validList.add ( "IncludeColumns" );
    validList.add ( "InputTableEventIDColumn" );
    validList.add ( "InputTableEventTypeColumn" );
    validList.add ( "IncludeInputTableEventTypes" );
    validList.add ( "InputTableEventStartColumn" );
    validList.add ( "InputTableEventEndColumn" );
    validList.add ( "InputTableEventLocationColumns" );
    validList.add ( "InputTableEventLabelColumn" );
    validList.add ( "InputTableEventDescriptionColumn" );
    validList.add ( "NewTableID" );
    validList.add ( "OutputTableTSIDColumn" );
    validList.add ( "OutputTableTSIDFormat" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );    

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Create an event table by analyzing the time series.
@param commandTag used in logging
@param commandPhase the command phase, should be run, rather than discovery
@param status command status, for logging
@param tslist list of time series to process
@param newTableId new table identifier
@param tsidColumn name of the TSID column
@param tsidFormat format for TSID column
@return new DataTable containing events from the analysis
*/
private DataTable createEventTableFromAnalysis (
	String commandTag,
	CommandPhaseType commandPhase,
    CommandStatus status,
	List<TS> tslist,
	String newTableId,
	String tsidColumn,
	String tsidFormat
	) {
	// Used for logging.
	String routine = getClass().getSimpleName() + "createEventTableFromAnalysis";
	int dl = 1;       // Debug level.
	int logLevel = 3; // Level for warning messages.
	int warningCount = 0;
	TSCommandProcessor processor = (TSCommandProcessor)this.getCommandProcessor();

	// Create the new table for output:
	// - hard code the column names for now, other than TSID column
	DataTable newTable = new DataTable();
	newTable.setTableID(newTableId);
   	int tsidColumnNum = newTable.addField(new TableField(TableField.DATA_TYPE_STRING, tsidColumn, -1, -1), null);
   	int eventStartColumnNum = newTable.addField(new TableField(TableField.DATA_TYPE_DATETIME, "EventStartDateTime", -1, -1), null);
   	int eventStartValueColumnNum = newTable.addField ( new TableField(TableField.DATA_TYPE_DOUBLE, "EventStartValue", -1, 9), null );
   	int eventExtremeColumnNum = newTable.addField( new TableField(TableField.DATA_TYPE_DATETIME, "EventExtremeDateTime", -1, -1), null );
   	int eventExtremeValueColumnNum = newTable.addField( new TableField(TableField.DATA_TYPE_DOUBLE, "EventExtremeValue", -1, 9), null );
   	int eventEndColumnNum = newTable.addField(new TableField(TableField.DATA_TYPE_DATETIME, "EventEndDateTime", -1, -1), null );
   	int eventEndValueColumnNum = newTable.addField ( new TableField(TableField.DATA_TYPE_DOUBLE, "EventEndValue", -1, 9), null );
    newTable.setTableID ( newTableId );

    // Data used in processing.
    String tsid = null;             // Time series identifier.
	TSIterator tsi = null;          // Iterators for "current" data point.
	TSIterator tsiStart = null;     // Iterator to position the event start, bounding "tsi".
	TSIterator tsiEnd = null;       // Iterator to position the event end, bounding "tsi".
	TSData tsdata = null;           // Data for a time series point from "tsi", reused during iteration.
	TSData tsdataStart = null;      // Data for a time series point from "tsiStart", reused during iteration.
	TSData tsdataEnd = null;        // Data for a time series point from "tsiEnd", reused during iteration.
	double value;                   // Data value at "tsi".
	double valueStart = 0.0;        // Data value at "tsiStart".
	double valueStartPrev;          // Data value at "tsiStart" for event previous start search iteration.
	double valueEnd = 0.0;          // Data value at "tsiEnd".
	double valueEndPrev;            // Data value at "tsiEnd" for event previous end search iteration.
	TableRecord rec = null;         // New data table record.
	String message = null;          // Message used for logging.
	boolean isRegular = false;      // Is the time series regular interval, used to optimize code?
	int maxEventCount = 0;          // Maximum number of events to process, used for development, 0 to process all.
	boolean outputMinutes = false;  // Used during development to output date/time in minutes
	String dtformat = "%Y-%d-%m %H:%M:%S.%u"; // Used for debugging.
	DateTimeFormatterType ft = DateTimeFormatterType.C;
	if ( outputMinutes ) {
		// Format used for development for minutes format.
		dtformat = "%M.%u";
	}
    // Process all the time series.
	for ( TS ts : tslist ) {
		// Determine whether a regular interval time series.
		isRegular = true;
		if ( ts.getDataIntervalBase() == TimeInterval.IRREGULAR ) {
			isRegular = false;
		}
		// The TSID for output is formatted based on command parameter.
        if ( (tsidFormat != null) && !tsidFormat.isEmpty() ) {
            tsid = TSCommandProcessorUtil.expandTimeSeriesMetadataString( processor, ts, tsidFormat, status, commandPhase);
        }
        else {
            // Use the alias if available and then the TSID.
            tsid = ts.getAlias();
            if ( (tsid == null) || tsid.equals("") ) {
                tsid = ts.getIdentifierString();
            }
        }
		// Get the overall data limits to help evaluate the magnitude of changes:
        // - this will be impacted by large stray data values unless the time series is filtered first
		TSLimits tslimits = ts.getDataLimits();
		if ( Message.isDebugOn ) {
			Message.printDebug(dl, routine, "Limits for: " + tsid + "\n" + tslimits.toString());
		}
		// Iterate through the data one time to determine the 
		// Iterate through the data.
		try {
			// Create an iterator to step through the time series and bounds for events:
			// - initially they are all the same but "tsiStart" and "tsiEnd" will be positioned to bound "tsi"
			tsi = ts.iterator();
			tsiStart = ts.iterator();
			tsiEnd = ts.iterator();
			if ( Message.isDebugOn ) {
				Message.printDebug(dl, routine, "tsi period is      " + tsi.getDate1().toString(ft,dtformat) + " to " + tsi.getDate2().toString(ft,dtformat) );
				Message.printDebug(dl, routine, "tsiStart period is " + tsiStart.getDate1().toString(ft,dtformat) + " to " + tsiStart.getDate2().toString(ft,dtformat) );
				Message.printDebug(dl, routine, "tsiEnd period is   " + tsiEnd.getDate1().toString(ft,dtformat) + " to " + tsiEnd.getDate2().toString(ft,dtformat) );
			}
		}
		catch ( Exception e ) {
			// TODO smalers 2022-03-07 add error handling.
		}
		boolean doneWithAnalysis = false;  // Whether to break out of inner event search loop.
		boolean eventStartFound = false;   // Whether the start of an event was found, an inflection in the data.
		boolean eventEndFound = false;     // Whether the end of an event was found, an inflection in the data.
		int bracketStart = 0;              // The number of values on the left side of current, 1 = one to left of current.
		int bracketEnd = 0;                // The number of values on the right side of current, 1 = one to right of current.
		int loopCount = 0;                 // Count of how many data advancements to process.
		boolean doFlagTimeSeries = true;   // Whether to flag original time series with events - TODO smalers 2022-03-10 need to handle with parameters.
		boolean doAdvanceIterator = true;  // Whether to advance the iterator, used when event has been detected.
		int dataCount = 0;                 // Count of number of data points are processed, should be (close to) the number of points in iteration period.
		int dataCountPrev = -1;            // Used to prevent infinite loops if there is a logic problem.
		// Main loop to iterate through data points forward in time.
		while ( true ) {
			// Advance the main iterator.
			if ( doAdvanceIterator ) {
				// This will not occur after an event because want to use the end of last event to evaluate new event.
				tsdata = tsi.next();
				++dataCount;
				if ( tsdata == null ) {
					// End of data so break out of analysis.
					break;
				}
				value = tsdata.getDataValue();
				if ( Message.isDebugOn ) {
					Message.printDebug(dl, routine, "Advanced main iterator one step to: " + tsi.getDate().toString(ft,dtformat) + " value = " + value + ", dataCount = " + dataCount );
				}
			}
			else {
				if ( tsdata == null ) {
					// End of data so break out of analysis.
					break;
				}
				value = tsdata.getDataValue();
				if ( Message.isDebugOn ) {
					Message.printDebug(dl, routine, "Did not advance main iterator - still at end of last event: " + tsi.getDate().toString(ft,dtformat) + " value = " + value + ", dataCount = " + dataCount );
				}
			}
			
			if ( dataCountPrev == dataCount ) {
				// Iterator has not advanced:
				// - possible logic problem
				// - break to avoid infinite loop
				message = "Iterator did not advance at " + tsi.getDate() + ", dataCount = " + dataCount + ". Possible logic problem.";
				Message.printWarning(logLevel,
					MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.WARNING,
					message, "Report the problem to software support." ) );
				break;
			}
			// Save for the next iteration check.
			dataCountPrev = dataCount;

			// Reset whether to advance:
			// - if an event is detected this will be set to false to allow the end of one event to start another
			doAdvanceIterator = true;

			if ( ts.isDataMissing(value) ) {
				// Missing data value so go to the next point in the outer loop.
				continue;
			}

			++loopCount;
			// Use for testing to run on limited dataset.
			if ( (maxEventCount > 0) && (loopCount > maxEventCount) ) {
				break;
			}
			
			// Reset the iterator event start and end iterators each time an event is analyze:
			// - make sure to use 'goTo' below to position initially to improve performance
			tsiStart.reset();
			tsiEnd.reset();
			
			if ( Message.isDebugOn ) {
				Message.printDebug(dl, routine, "  Evaluating event data at tsi " + tsi.getDate().toString(ft,dtformat));
			}
			
			// Analyze the data for the extreme around the current point:
			// - expand the data window back in time in forward in time to evaluate whether on an extreme value 
			// - the first check is whether the bounding values are <= the current value
			if ( Message.isDebugOn ) {
				Message.printDebug(dl, routine, "  Moving tsiStart to tsi " + tsi.getDate().toString(ft,dtformat));
			}
			if ( isRegular ) {
				tsdataStart = tsiStart.goTo(tsi.getDate());
			}
			else {
				tsdataStart = ((IrregularTSIterator)tsiStart).goTo(tsdata);
			}
			if ( tsdataStart == null ) {
				// Should not happen because 'tsi' was checked above and should have existing point:
				// - break out of the analysis
				message = "    Unable to align tsiStart with tsi";
				Message.printWarning(logLevel,
					MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Report the problem to software support." ) );
				break;
			}
			if ( Message.isDebugOn ) {
				Message.printDebug(dl, routine, "    tsiStart positioned at " + tsdataStart.getDate().toString(ft,dtformat));
			}

			if ( Message.isDebugOn ) {
				Message.printDebug(dl, routine, "  Moving tsiEnd to tsi " + tsi.getDate().toString(ft,dtformat));
			}
			if ( isRegular ) {
				tsdataEnd = tsiEnd.goTo(tsi.getDate());
			}
			else {
				tsdataEnd = ((IrregularTSIterator)tsiEnd).goTo(tsdata);
			}
			if ( tsdataEnd == null ) {
				// Should not happen because 'tsi' was checked above and should have existing point:
				// - break out of the analysis
				message = "    Unable to align tsiEnd with tsi";
				Message.printWarning(logLevel,
					MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
				status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Report the problem to software support." ) );
				break;
			}
			if ( Message.isDebugOn ) {
				Message.printDebug(dl, routine, "    tsiEnd positioned at " + tsdataEnd.getDate().toString(ft,dtformat));
			}

			// Move the start and end iterators on each side of the current point to expand the event time.
			if ( Message.isDebugOn ) {
				Message.printDebug(dl, routine, "  Move tsiStart backward and tsiEnd forward to find event around tsi.");
			}

			doneWithAnalysis = false;
			bracketStart = 0;
			bracketEnd = 0;
			
			// Event depends on finding both a valid start and end below.
			eventStartFound = false;
			eventEndFound = false;
			
			// Position the "tsiStart" at the start of an event:
			// - may hit iterator start which currently is considered an incomplete event
			// - duplicates are handled when positioning the end
			valueStartPrev = value;
			while ( true ) {
				if ( Message.isDebugOn ) {
					Message.printDebug(dl, routine, "    Moving tsiStart using previous() to position before: " + tsiStart.getDate().toString(ft,dtformat) + " valueStart=" + tsiStart.getDataValue());
				}
				++bracketStart;
				tsdataStart = tsiStart.previous();
				if ( tsdataStart == null ) {
					// At the start of the dataset:
					// - done with the analysis so need to step forward
					if ( Message.isDebugOn ) {
						Message.printDebug(dl, routine, "      tsiStart data is null (start of dataset?) - breaking out of event analysis." );
					}
					break;
				}
				if ( Message.isDebugOn ) {
					Message.printDebug(dl, routine, "                                                 now at: " + tsiStart.getDate().toString(ft,dtformat) + " valueStart=" + tsiStart.getDataValue());
				}

				// Get the data value for the iterator:
				// - currently skip over regions where values are missing

				valueStart = tsdataStart.getDataValue();
				if ( ts.isDataMissing(valueStart) ) {
					// Missing data value so don't have a valid start of event.
					break;
				}
				else if ( (bracketStart == 1) && (valueStart >= value) ) {
					// Current point is not >= the previous value so can quit looking immediately
					// because it is not a local extreme.
					if ( Message.isDebugOn ) {
						Message.printDebug(dl, routine, "        Value is not a local extreme at " + tsi.getDate().toString(ft,dtformat) + " value = " + value);
					}
					break;
				}
				//if ( (valueStart >= valueStartPrev) && (valueStart != value) ) { }
				else if ( valueStart >= valueStartPrev ) {
					// The starting value is no longer on the upward leg of the event:
					// - have found the bounding start of the event
					// - go to the next point because went 1 too far
					// - leading flat line is considered part of the previous condition, not part of this event
					if ( Message.isDebugOn ) {
						Message.printDebug(dl, routine, "    Outside event: valueStart=" + valueStart + " >= valueEndPrev=" + valueStartPrev + " at " + tsiStart.getDate().toString(ft,dtformat) );
					}
					tsdataStart = tsiStart.next();
					--bracketStart;
					valueStart = tsdataStart.getDataValue();
					if ( Message.isDebugOn ) {
						Message.printDebug(dl, routine, "      After moving forward to in event, valueStart=" + valueStart + " at " + tsiStart.getDate().toString(ft,dtformat) );
					}
					eventStartFound = true;
					if ( Message.isDebugOn ) {
						Message.printDebug(dl, routine, "      Candidate event start is " + tsiStart.getDate().toString(ft,dtformat) );
					}
					break;
				}
				else {
					// Save the previous value for the next iteration and continue expanding the event start back in time.
					valueStartPrev = valueStart;
				}
			}
			
			if ( !eventStartFound ) {
				// No need to look for event end, just move the main iterator forward and search again.
				continue;
			} // End positioning the "tsiStart" iterator.
			
			// Position the "tsiEnd" iterator.

			valueEndPrev = value; // Start with the peak for comparisons.
			boolean duplicateExtremeValue = true; // Start assuming multiple equal extreme values can occur in sequence.
			int duplicateExtremeValueCount = 1; // To allow flagging time series values.
			while ( true ) {
				if ( Message.isDebugOn ) {
					Message.printDebug(dl, routine, "    Moving tsiEnd using next() to position after: " + tsiEnd.getDate().toString(ft,dtformat) + " valueEnd=" + tsiEnd.getDataValue());
				}
				// tsdataEnd is initially the same as tsi (set above), so advance to expand the candidate event.
				tsdataEnd = tsiEnd.next();
				++bracketEnd;
				if ( tsdataEnd == null ) {
					// At the end of the dataset:
					// - done with the analysis so break out
					doneWithAnalysis = true;
					break;
				}

				valueEnd = tsdataEnd.getDataValue();
				if ( ts.isDataMissing(valueEnd) ) {
					// Missing data value so don't have a valid end of event:
					// - break to move the pointer in the outside loop
					// - TODO smalers 2022-03-10 evaluate whether to allow a number of missing
					break;
				}

				if ( duplicateExtremeValue && (value == valueEnd) ) {
					// Have a value that is a duplicate extreme value:
					// - increment the count and get another value
					// - TODO smalers evaluate whether to not be exactly the initial value
					// - get the next value
					++duplicateExtremeValueCount;
					continue;
				}
				
				// If here have a value that is different from the extreme enough to be evaluated as more of event or break in event.

				if ( (bracketEnd == 1) && (valueEnd > value) ) {
					// Want to find the first point that is not on the decreasing end of the event.
					// All of the following should be handled, where H is tsi and detected as high value.
					//
					//    Event        Event        Event        Event    Event          No event (until tsi moved and later point evaluated)
					//      H           Hoo          H..o         H..o                        H..o
					//     / \         /   \        /    \            \   H...o              /    \
					//        \             \             \ /          \ /     \         o..o
					//         --            --            o            o       \       /
					//                                                                 /
					// Current point is not >= the previous value (working forward) so can quit looking immediately
					// because it is not a local extreme.
					if ( Message.isDebugOn ) {
						Message.printDebug(dl, routine, "      Value is not a local extreme at " + tsi.getDate().toString(ft,dtformat) );
					}
					break;
				}
				//if ( (valueEnd >= valueEndPrev) && (valueEnd != value) ) {  // }
				else if ( valueEnd >= valueEndPrev ) {
					// The ending value is no longer on the descending leg of the event:
					// - have found the bounding end of the event
					// - this handles the case where multiple duplicate values are in sequence
					//   but a change from the duplicates has been detected
					// - trailing flat line is currently considered to NOT be part of the current event
					// - TODO smalers 2022-03-10 could enhance to consider a flat line to be part of an event
					//   if the flat part is followed by a continuation of the event
					if ( Message.isDebugOn ) {
						Message.printDebug(dl, routine, "      Outside event: valueEnd=" + valueEnd + " >= valueEndPrev=" + valueEndPrev + " at " + tsiEnd.getDate().toString(ft,dtformat) );
					}
					tsdataEnd = tsiEnd.previous();
					--bracketEnd;
					valueEnd = tsdataEnd.getDataValue();
					if ( Message.isDebugOn ) {
						Message.printDebug(dl, routine, "      After backing up to inside event, valueEnd=" + valueEnd + " at " + tsiEnd.getDate().toString(ft,dtformat) );
					}
					eventEndFound = true;
					if ( Message.isDebugOn ) {
						Message.printDebug(dl, routine, "      Candidate event end is " + tsiEnd.getDate().toString(ft,dtformat) );
					}
					break;
				}
				else {
					// Save the previous value for the next iteration and continue expanding the event end forward in time.
					valueEndPrev = valueEnd;
				}
			} // End positioning the tsiEnd iterator.
			
			if ( doneWithAnalysis ) {
				// If ran out of data for the event, done with the analysis:
				// - break out of the main loop
				break;
			}

			if ( eventStartFound && eventEndFound ) {
				// Found the start and end of the event and "tsi" is positioned at the extreme:
				// - further evaluate the event to see if it meets the criteria to be added
				// - add the event to the output table
				// - advance the main iterator past the event
				if ( Message.isDebugOn ) {
					Message.printDebug(dl, routine, "  Found a possible event based on peak:");
					Message.printDebug(dl, routine, "    start   = " + tsiStart.getDate().toString(ft,dtformat) + " value = " + valueStart + " bracket = " + bracketStart );
					Message.printDebug(dl, routine, "    extreme = " + tsi.getDate().toString(ft,dtformat) + " value = " + value + " duplicate extreme values = " + duplicateExtremeValueCount );
					Message.printDebug(dl, routine, "    end     = " + tsiEnd.getDate().toString(ft,dtformat) + " value = " + valueEnd + " bracket = " + bracketEnd );
				}
				boolean okToAddEvent = false;
				// Calculate the size of the event and the percent change relative to the starting value.
				double eventSize = Math.abs(valueStart - value);
				eventSize = Math.max(eventSize, Math.abs(valueEnd - value));
				double eventSizePercentOfStart = 0.0; // Event size as percent of the starting value.
				if ( valueStart > 0.0 ) {
					// TODO smalers 2022-03-10 how to handle events that start at zero?  Use overall limits?
					//    Perhaps analyze events that start at zero to see what typical magnitude is?
					eventSizePercentOfStart = Math.abs(eventSize/valueStart)*100.0;
				}
				double eventSizePercentOfStartReq = 1.0;
				if ( eventSizePercentOfStart > eventSizePercentOfStartReq ) {
					// Event is large enough to add.
					okToAddEvent = true;
				}
				else {
					if ( Message.isDebugOn ) {
						// Output to go with 'DOES NOT' message below.
						Message.printDebug(dl, routine, "    Criteria: eventSize = " + eventSize + ", eventSizePercentOfStart = " +
							eventSizePercentOfStart + ", eventSizePercentOfStartReq = " + eventSizePercentOfStartReq );
					}
				}
				if ( okToAddEvent ) {
					if ( Message.isDebugOn ) {
						Message.printDebug(dl, routine, "    Event DOES meet criteria - adding and skipping over event for next search." );
					}
					try {
						rec = newTable.emptyRecord();
						newTable.addRecord(rec);
						rec.setFieldValue(tsidColumnNum, tsid);
						rec.setFieldValue(eventExtremeColumnNum, tsi.getDate());
						rec.setFieldValue(eventExtremeValueColumnNum, new Double(value));
						rec.setFieldValue(eventStartColumnNum, tsiStart.getDate());
						rec.setFieldValue(eventStartValueColumnNum, new Double(valueStart));
						rec.setFieldValue(eventEndColumnNum, tsiStart.getDate());
						rec.setFieldValue(eventEndValueColumnNum, new Double(valueEnd));
					}
					catch ( Exception e ) {
						// TODO smalers 2022-03-07 handle error.
					}
					// Set a flag on the original time series
					if ( doFlagTimeSeries ) {
						tsdataStart.setDataFlag("+S");  // Start.
						tsdata.setDataFlag("+H");       // High.
						// Set other extreme value flags.
						for ( int i = 2; i < duplicateExtremeValueCount; i++ ) {
							TSData tsdata2 = tsdata.getNext();
							if ( tsdata2 == null ) {
								break;
							}
							else {
								tsdata2.setDataFlag("+H");
							}
						}
						tsdataEnd.setDataFlag("+E");    // End.
					}
				}
				else {
					if ( Message.isDebugOn ) {
						Message.printDebug(dl, routine, "    Event DOES NOT meet criteria - not adding and skipping over for next search." );
					}
				}

				// Advance the outer loop over the event:
				// - do this even if no start
				// - no start should only occur at the start of the dataset
				// - do this even if the event was not added so don't get stuck in the loop
				// - increment 'advanceCounter' by the end bracket to indicate points skipped
				if ( Message.isDebugOn ) {
					Message.printDebug(dl, routine, "    Advancing main iterator to end of event to search for next event: " + tsiEnd.getDate().toString(ft,dtformat));
				}
				if ( isRegular ) {
					tsdata = tsi.goTo(tsiEnd.getDate());
					dataCount += bracketEnd;
				}
				else {
					// Use the direct set for irregular because it is faster to avoid locating the date/time.
					tsdata = ((IrregularTSIterator)tsi).goTo(tsdataEnd);
					dataCount += bracketEnd;
				}
				// It is possible for one event to immediately start after another event:
				// - need to allow the end of an event to be considered in the next event
				// - don't advance the data with next() at the top of the loop
				//doAdvanceIterator = false;
				// Because searching for the start goes backward, DO need to move forward one position.
				doAdvanceIterator = true;
			} // End event was found.

			if ( tsdata == null ) {
				// No more data:
				// - done with the analysis.
				break;
			}
			else {
				// The outside loop will call tsi.next() to advance one step,
				// and a check for event can start again.
			}
		} // End of data for a time series.
		if ( Message.isDebugOn ) {
			Message.printDebug(dl, routine, "Done with the analysis for time series: " + tsid);
		}
	} // End processing time series.
	return newTable;
}

/**
Create an event table from an input table.
This is used to match time series to a historical event dataset.
*/
private DataTable createEventTableFromTable (
	CommandPhaseType commandPhase,
    CommandStatus status,
	List<TS> tslist,
	HashMap<String,String> timeSeriesLocations,
    List<String> eventTypes,
	DataTable table,
	String InputTableEventIDColumn,
    HashMap<String,String> inputTableEventLocationColumns,
	String InputTableEventTypeColumn,
	String InputTableEventStartColumn,
	String InputTableEventEndColumn,
	String InputTableEventLabelColumn,
	String InputTableEventDescriptionColumn,
	String NewTableID,
	String OutputTableTSIDColumn,
	String OutputTableTSIDFormat
	) {
	String routine = getClass().getSimpleName() + ".createEventTableFromTable";
	TSCommandProcessor processor = (TSCommandProcessor)this.getCommandProcessor();
    // First create the new table similar to the CopyTable() command.
	// Specify column filters such that no records will actually be copied,
	// since they will only be copied for matching time series locations.
	DataTable newTable = null;
	Hashtable<String,String> columnFilters = new Hashtable<> ();
	columnFilters.put(InputTableEventTypeColumn, "xzyz");
	// TODO SAM 2013-09-08 For now always default to copying the necessary columns.
	String [] includeColumns2 = {
	    InputTableEventIDColumn,
	    InputTableEventTypeColumn,
	    InputTableEventStartColumn,
	    InputTableEventEndColumn,
	    InputTableEventLabelColumn,
	    InputTableEventDescriptionColumn
	};
    newTable = table.createCopy ( table, NewTableID, /* includeColumns */ includeColumns2, null, null, columnFilters, null );
    // Make sure that the output table includes the TSID and other necessary columns.
    // Output table columns should have been created from above copy.
    int outputTableTSIDColumnNumber = -1;
    try {
         outputTableTSIDColumnNumber = newTable.getFieldIndex(OutputTableTSIDColumn);
    }
    catch ( Exception e2 ) {
        outputTableTSIDColumnNumber = newTable.addField(new TableField(TableField.DATA_TYPE_STRING, OutputTableTSIDColumn, -1, -1), null);
        Message.printStatus(2, routine, "Did not match TableTSIDColumn \"" + OutputTableTSIDColumn +
            "\" as column table so added to table." );
    }
    int outputTableEventIDColumnNumber = -1;
    try {
         outputTableEventIDColumnNumber = table.getFieldIndex(InputTableEventIDColumn);
    }
    catch ( Exception e2 ) {
    }
    int outputTableEventTypeColumnNumber = -1;
    try {
         outputTableEventTypeColumnNumber = table.getFieldIndex(InputTableEventTypeColumn);
    }
    catch ( Exception e2 ) {
    }
    int outputTableEventStartColumnNumber = -1;
    try {
         outputTableEventStartColumnNumber = table.getFieldIndex(InputTableEventStartColumn);
    }
    catch ( Exception e2 ) {
    }
    int outputTableEventEndColumnNumber = -1;
    try {
         outputTableEventEndColumnNumber = table.getFieldIndex(InputTableEventEndColumn);
    }
    catch ( Exception e2 ) {
    }
    int outputTableEventLabelColumnNumber = -1;
    try {
         outputTableEventLabelColumnNumber = table.getFieldIndex(InputTableEventLabelColumn);
    }
    catch ( Exception e2 ) {
    }
    int outputTableEventDescriptionColumnNumber = -1;
    try {
         outputTableEventDescriptionColumnNumber = table.getFieldIndex(InputTableEventDescriptionColumn);
    }
    catch ( Exception e2 ) {
    }
    // Create the events from the existing table and the time series being processed.
    List<TimeSeriesEvent> timeSeriesEvents = new ArrayList<>();
    for ( TS ts : tslist ) {
        // Use the original table for matching events, and add the matches to the new table below.
        TimeSeriesEventAnnotationCreator ac = new TimeSeriesEventAnnotationCreator(table, ts);
        DateTime analysisStart = null; // TODO SAM 2013-09-08 Evaluate whether needed.
        DateTime analysisEnd = null; // TODO SAM 2013-09-08 Evaluate whether needed.
        // Expand the time series location properties for the location types, for the specific time series.
        // It is OK if properties are not matched - time series may not have.
        HashMap<String,String> timeSeriesLocationsExpanded = new HashMap<>();
        for ( Map.Entry<String,String> pairs: timeSeriesLocations.entrySet() ) {
            timeSeriesLocationsExpanded.put(pairs.getKey(),
            TSCommandProcessorUtil.expandTimeSeriesMetadataString( processor, ts, pairs.getValue(), null, null));
            Message.printStatus(2, "", "Expanded property for \"" + pairs.getValue() + "\" is \"" +
                timeSeriesLocationsExpanded.get(pairs.getKey()) + "\"");
        }
        timeSeriesEvents = ac.createTimeSeriesEvents(
        	eventTypes,
            InputTableEventIDColumn,
            InputTableEventTypeColumn,
            InputTableEventStartColumn,
            InputTableEventEndColumn,
            InputTableEventLabelColumn,
            InputTableEventDescriptionColumn,
            inputTableEventLocationColumns,
            timeSeriesLocationsExpanded,
            analysisStart,
            analysisEnd );
        // Transfer the events to the output table.
        int row;
        String tsid = "";
        if ( (OutputTableTSIDFormat != null) && !OutputTableTSIDFormat.equals("") ) {
            tsid = TSCommandProcessorUtil.expandTimeSeriesMetadataString( processor, ts, OutputTableTSIDFormat, status, commandPhase);
        }
        else {
            // Use the alias if available and then the TSID.
            tsid = ts.getAlias();
            if ( (tsid == null) || tsid.equals("") ) {
                tsid = ts.getIdentifierString();
            }
        }
        for ( TimeSeriesEvent event: timeSeriesEvents ) {
            row = newTable.getNumberOfRecords(); // Add a new record.
            try {
                newTable.setFieldValue(row, outputTableTSIDColumnNumber, tsid, true);
                newTable.setFieldValue(row, outputTableEventIDColumnNumber, event.getEvent().getEventID(), true);
                newTable.setFieldValue(row, outputTableEventTypeColumnNumber, event.getEvent().getEventType(), true);
                newTable.setFieldValue(row, outputTableEventStartColumnNumber, event.getEvent().getEventStart(), true);
                newTable.setFieldValue(row, outputTableEventEndColumnNumber, event.getEvent().getEventEnd(), true);
                newTable.setFieldValue(row, outputTableEventLabelColumnNumber, event.getEvent().getLabel(), true);
                newTable.setFieldValue(row, outputTableEventDescriptionColumnNumber, event.getEvent().getDescription(), true);
            }
            catch ( Exception e ) {
            	// Error adding data to the row.
            }
        }
    }
    return newTable;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed.
	return (new CreateTimeSeriesEventTable_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable() {
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{   DataTable table = getDiscoveryTable();
    List<T> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new ArrayList<>();
        v.add ( (T)table );
    }
    return v;
}

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
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
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message = "";
	int warning_level = 2;
	int log_level = 3;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

	// Make sure there are time series available to operate on.
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
    String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String TimeSeriesLocations = parameters.getValue ( "TimeSeriesLocations" );
    HashMap<String,String> timeSeriesLocations = new HashMap<>();
    if ( (TimeSeriesLocations != null) && (TimeSeriesLocations.length() > 0) &&
        (TimeSeriesLocations.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(TimeSeriesLocations, ",", 0 );
        // Now break pairs and put in hashmap.  The value may be of form ${TS:property} so need to handle manually.
        for ( String pair : pairs ) {
            int colonPos = pair.indexOf(":");
            if ( colonPos < 0 ) {
                // No value.
                timeSeriesLocations.put(pair.trim(), "" );
            }
            else {
                if ( colonPos == (pair.length() - 1) ) {
                    // Colon was at end.
                    timeSeriesLocations.put(pair.substring(0,colonPos).trim(), "" );
                }
                else {
                    timeSeriesLocations.put(pair.substring(0,colonPos).trim(), pair.substring(colonPos + 1).trim() );
                }
            }
        }
    }
    String TableID = parameters.getValue ( "TableID" );
    String NewTableID = parameters.getValue ( "NewTableID" );
    String IncludeColumns = parameters.getValue ( "IncludeColumns" );
    String [] includeColumns = null;
    if ( (IncludeColumns != null) && !IncludeColumns.equals("") ) {
        includeColumns = IncludeColumns.split(",");
        for ( int i = 0; i < includeColumns.length; i++ ) {
            includeColumns[i] = includeColumns[i].trim();
        }
    }
    String InputTableEventIDColumn = parameters.getValue ( "InputTableEventIDColumn" );
    String InputTableEventTypeColumn = parameters.getValue ( "InputTableEventTypeColumn" );
    String IncludeInputTableEventTypes = parameters.getValue ( "IncludeInputTableEventTypes" );
    List<String> eventTypes = new ArrayList<>();
    if ( (IncludeInputTableEventTypes != null) && !IncludeInputTableEventTypes.equals("") ) {
        eventTypes = StringUtil.breakStringList(IncludeInputTableEventTypes, ",", 0);
        for ( int i = 0; i < eventTypes.size(); i++ ) {
            eventTypes.set(i, eventTypes.get(i).trim() );
        }
    }
    String InputTableEventStartColumn = parameters.getValue ( "InputTableEventStartColumn" );
    String InputTableEventEndColumn = parameters.getValue ( "InputTableEventEndColumn" );
    String InputTableEventLocationColumns = parameters.getValue ( "InputTableEventLocationColumns" );
    HashMap<String,String> inputTableEventLocationColumns = new HashMap<>();
    if ( (InputTableEventLocationColumns != null) && (InputTableEventLocationColumns.length() > 0) &&
        (InputTableEventLocationColumns.indexOf(":") > 0) ) {
        // First break map pairs by comma.
        List<String>pairs = StringUtil.breakStringList(InputTableEventLocationColumns, ",", 0 );
        // Now break pairs and put in hashmap.
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            inputTableEventLocationColumns.put(parts[0].trim(), parts[1].trim() );
        }
    }
    String InputTableEventLabelColumn = parameters.getValue ( "InputTableEventLabelColumn" );
    String InputTableEventDescriptionColumn = parameters.getValue ( "InputTableEventDescriptionColumn" );
    String OutputTableTSIDColumn = parameters.getValue ( "OutputTableTSIDColumn" );
    String OutputTableTSIDFormat = parameters.getValue ( "OutputTableTSIDFormat" );
    
    // Get the time series to process.  Allow TSID to be a pattern or specific time series.

    List<TS> tslist = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command.
        tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, null );
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        PropList request_params = new PropList ( "" );
        request_params.set ( "TSList", TSList );
        request_params.set ( "TSID", TSID );
        request_params.set ( "EnsembleID", EnsembleID );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
            "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
            Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report the problem to software support." ) );
        }
        if ( bean == null ) {
            Message.printStatus ( 2, routine, "Bean is null.");
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
        if ( o_TSList == null ) {
            message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
            Message.printWarning ( log_level, MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
        else {
        	@SuppressWarnings("unchecked")
			List<TS> tslist0 = (List<TS>)o_TSList;
            tslist = tslist0;
            if ( tslist.size() == 0 ) {
                message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
                Message.printWarning ( log_level, MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
                    "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
            }
        }
    }
    
    // Get the input table to process.

    DataTable table = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
    	int nts = tslist.size();
    	if ( nts == 0 ) {
        	message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            	"\", EnsembleID=\"" + EnsembleID + "\".";
        	Message.printWarning ( warning_level,
        	MessageUtil.formatMessageTag(
        	command_tag,++warning_count), routine, message );
        	status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE, message,
            	"Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    	}
    
        PropList request_params = null;
        CommandProcessorRequestResultsBean bean = null;
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
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	try {
    	// Create the event table.
	    if ( commandPhase == CommandPhaseType.RUN ) {
	        DataTable newTable = null;
	        if ( (TableID != null) && !TableID.equals("") ) {
	        	newTable = createEventTableFromTable(
	        		commandPhase,
            		status,
	        		tslist,
	        		timeSeriesLocations,
            		eventTypes,
	        		table,
	        		InputTableEventIDColumn,
            		inputTableEventLocationColumns,
	        		InputTableEventTypeColumn,
	        		InputTableEventStartColumn,
	        		InputTableEventEndColumn,
	        		InputTableEventLabelColumn,
	        		InputTableEventDescriptionColumn,
	        		NewTableID,
	        		OutputTableTSIDColumn,
	        		OutputTableTSIDFormat
				);
	        }
	        else {
	        	// Create the event table by analyzing the time series.
	        	newTable = createEventTableFromAnalysis (
	        		command_tag,
	        		commandPhase,
            		status,
	        		tslist,
	        		NewTableID,
	        		OutputTableTSIDColumn,
	        		OutputTableTSIDFormat
	        	);
	        }
            // Set the table in the processor.
            if ( newTable != null ) {
                PropList request_params = new PropList ( "" );
                request_params.setUsingObject ( "Table", newTable );
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
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create an empty table and set the ID.
            DataTable newTable = new DataTable();
            newTable.setTableID ( NewTableID );
            setDiscoveryTable ( newTable );
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error creating event table (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Report problem to software support." ) );
		throw new CommandWarningException ( message );
	}
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		throw new CommandWarningException ( message );
	}

    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table ) {
    __table = table;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TSList = props.getValue( "TSList" );
    String TSID = props.getValue( "TSID" );
    String EnsembleID = props.getValue( "EnsembleID" );
    String TimeSeriesLocations = props.getValue( "TimeSeriesLocations" );
    String TableID = props.getValue( "TableID" );
	String IncludeColumns = props.getValue( "IncludeColumns" );
	String InputTableEventIDColumn = props.getValue( "InputTableEventIDColumn" );
	String InputTableEventTypeColumn = props.getValue( "InputTableEventTypeColumn" );
	String IncludeInputTableEventTypes = props.getValue( "IncludeInputTableEventTypes" );
	String InputTableEventStartColumn = props.getValue( "InputTableEventStartColumn" );
	String InputTableEventEndColumn = props.getValue( "InputTableEventEndColumn" );
	String InputTableEventLocationColumns = props.getValue( "InputTableEventLocationColumns" );
	String InputTableEventLabelColumn = props.getValue( "InputTableEventLabelColumn" );
	String InputTableEventDescriptionColumn = props.getValue( "InputTableEventDescriptionColumn" );
    String NewTableID = props.getValue( "NewTableID" );
    String OutputTableTSIDColumn = props.getValue ( "OutputTableTSIDColumn" );
    String OutputTableTSIDFormat = props.getValue ( "OutputTableTSIDFormat" );
	StringBuffer b = new StringBuffer ();
    if ( (TSList != null) && (TSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSList=" + TSList );
    }
    if ( (TSID != null) && (TSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSID=\"" + TSID + "\"" );
    }
    if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
    }
    if ( (TimeSeriesLocations != null) && (TimeSeriesLocations.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TimeSeriesLocations=\"" + TimeSeriesLocations + "\"" );
    }
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
	if ( (IncludeColumns != null) && (IncludeColumns.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IncludeColumns=\"" + IncludeColumns + "\"" );
	}
    if ( (InputTableEventIDColumn != null) && (InputTableEventIDColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventIDColumn=\"" + InputTableEventIDColumn + "\"" );
    }
    if ( (InputTableEventTypeColumn != null) && (InputTableEventTypeColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventTypeColumn=\"" + InputTableEventTypeColumn + "\"" );
    }
    if ( (IncludeInputTableEventTypes != null) && (IncludeInputTableEventTypes.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IncludeInputTableEventTypes=\"" + IncludeInputTableEventTypes + "\"" );
    }
    if ( (InputTableEventStartColumn != null) && (InputTableEventStartColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventStartColumn=\"" + InputTableEventStartColumn + "\"" );
    }
    if ( (InputTableEventEndColumn != null) && (InputTableEventEndColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventEndColumn=\"" + InputTableEventEndColumn + "\"" );
    }
    if ( (InputTableEventLocationColumns != null) && (InputTableEventLocationColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventLocationColumns=\"" + InputTableEventLocationColumns + "\"" );
    }
    if ( (InputTableEventLabelColumn != null) && (InputTableEventLabelColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventLabelColumn=\"" + InputTableEventLabelColumn + "\"" );
    }
    if ( (InputTableEventDescriptionColumn != null) && (InputTableEventDescriptionColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputTableEventDescriptionColumn=\"" + InputTableEventDescriptionColumn + "\"" );
    }
    if ( (NewTableID != null) && (NewTableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "NewTableID=\"" + NewTableID + "\"" );
    }
    if ( (OutputTableTSIDColumn != null) && (OutputTableTSIDColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputTableTSIDColumn=\"" + OutputTableTSIDColumn + "\"" );
    }
    if ( (OutputTableTSIDFormat != null) && (OutputTableTSIDFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputTableTSIDFormat=\"" + OutputTableTSIDFormat + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}