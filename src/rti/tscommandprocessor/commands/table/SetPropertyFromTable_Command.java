// SetPropertyFromTable_Command - This class initializes, checks, and runs the SetPropertyFromTable() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringDictionary;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the SetPropertyFromTable() command.
*/
public class SetPropertyFromTable_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

	/**
	 * Values used with IgnoreCase.
	 */
	protected String _False = "False";
	protected String _True = "True";

/**
Property set during discovery - only name will be available.
*/
private Prop __discovery_Prop = null;

/**
Constructor.
*/
public SetPropertyFromTable_Command () {
	super();
	setCommandName ( "SetPropertyFromTable" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException {
	String TableID = parameters.getValue ( "TableID" );
    String Row = parameters.getValue ( "Row" );
    String IgnoreCase = parameters.getValue ( "IgnoreCase" );
    String PropertyName = parameters.getValue ( "PropertyName" );
    String RowCountProperty = parameters.getValue ( "RowCountProperty" );
    String ColumnCountProperty = parameters.getValue ( "ColumnCountProperty" );
	String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }

    if ( (IgnoreCase != null) && !IgnoreCase.isEmpty() &&
    	!IgnoreCase.equalsIgnoreCase(this._False) && !IgnoreCase.equalsIgnoreCase(this._True) ) {
        message = "The ignore case value (" + IgnoreCase +") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify whether to ignore case as " + this._False + " or " + this._True + " (default)." ) );
    }

    if ( (Row != null) && !Row.isEmpty() ) {
    	// TODO smalers 2021-08-19 need to create validation tools.
    	if ( !StringUtil.isLong(Row) && !Row.equalsIgnoreCase("last") ) {
			message = "The table row (" + Row + ") is invalid.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Provide the row as an integer 1+ or \"last\"." ) );
	  	}
    }

    int propCount = 0;
    if ( (RowCountProperty != null) && !RowCountProperty.isEmpty() ) {
    	++propCount;
    }
    if ( (ColumnCountProperty != null) && !ColumnCountProperty.isEmpty() ) {
    	++propCount;
    }

    if ( (propCount == 0) && ((PropertyName == null) || (PropertyName.length() == 0)) ) {
        message = "The property name, RowCountProperty, or ColumnCountProperty must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the property name, RowCountProperty, or ColumnCountProperty." ) );
    }

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(11);
    validList.add ( "TableID" );
    validList.add ( "Column" );
    validList.add ( "ColumnIncludeFilters" );
    validList.add ( "ColumnExcludeFilters" );
    validList.add ( "IgnoreCase" );
    validList.add ( "Row" );
    validList.add ( "IgnoreCase" );
    validList.add ( "PropertyName" );
    validList.add ( "DefaultValue" );
    validList.add ( "RowCountProperty" );
    validList.add ( "ColumnCountProperty" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}

    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed.
	return (new SetPropertyFromTable_JDialog ( parent, this, tableIDChoices )).ok();
}

// TODO SAM 2015-02-25 may include this in utility code at some point.
/**
Find the table rows that match the include and exclude filters.
@param table the data table to retrieve records (only the first matching row is returned)
@param columnIncludeFilters include filters to match column values
@param columnIncludeOrs include filter ORs to match column values - NOT IMPLEMENTED
@param columnExcludeFilters include filters to match column values
@param columnExcludeOrs exclude filter ORs to match column values - NOT IMPLEMENTED
@param ignoreCase whether to ignore case when checking the filters
@param row the specific row to match, a number 1+ or "last" for the last row
@param errors a list of strings to set errors
@param return the list of matching table records
*/
private List<TableRecord> findTableRecords ( DataTable table,
	StringDictionary columnIncludeFilters,
	//boolean [] columnIncludeOrs,
	StringDictionary columnExcludeFilters,
	//boolean [] columnExcludeOrs,
	boolean ignoreCase,
	String row,
	List<String> errors ) {
	String routine = getClass().getSimpleName() + ".findTableRecords";
	// Check whether the a row is specified.
	boolean doRow = false;
	if ( (row != null) && !row.isEmpty() ) {
		doRow = true;
	}
	// Determine whether any ORs need to be processed:
	// - if so, all filter items need to be checked and at least one OR needs to be matched for an overall match
	// - if not, the first AND that is not matched causes a break
	int columnIncludeOrCount = 0;
	int columnIncludeOrMatchCount = 0;
	/*
	for ( boolean columnIncludeOr : columnIncludeOrs ) {
		if ( columnIncludeOr ) {
			++columnIncludeOrCount;
		}
	}
	*/
	int columnExcludeOrCount = 0;
	int columnExcludeOrMatchCount = 0;
	/*
	for ( boolean columnExcludeOr : columnExcludeOrs ) {
		if ( columnExcludeOr ) {
			++columnExcludeOrCount;
		}
	}
	*/
    // Get include filter columns and glob-style regular expressions.
    int [] columnIncludeFiltersNumbers = new int[0];
    String [] columnIncludeFiltersRegex = null;
    if ( columnIncludeFilters != null ) {
        LinkedHashMap<String, String> map = columnIncludeFilters.getLinkedHashMap();
        columnIncludeFiltersNumbers = new int[map.size()];
        columnIncludeFiltersRegex = new String[map.size()];
        int ikey = -1;
        String key = null;
        for ( Map.Entry<String,String> entry : map.entrySet() ) {
            ++ikey;
            columnIncludeFiltersNumbers[ikey] = -1;
            try {
                key = entry.getKey();
                columnIncludeFiltersNumbers[ikey] = table.getFieldIndex(key);
                columnIncludeFiltersRegex[ikey] = map.get(key);
                // Turn default globbing notation into internal Java regex notation.
                if ( ignoreCase ) {
                	// Use upper case for comparisons.
                	columnIncludeFiltersRegex[ikey] = columnIncludeFiltersRegex[ikey].replace("*", ".*").toUpperCase();
                }
                else {
                	// Use original case for comparisons.
                	columnIncludeFiltersRegex[ikey] = columnIncludeFiltersRegex[ikey].replace("*", ".*");
                }
                if ( Message.isDebugOn ) {
                	Message.printStatus ( 2, routine, "Include filter column \"" + key + "\" [" +
                		columnIncludeFiltersNumbers[ikey] + "] regex \"" + columnIncludeFiltersRegex[ikey] + "\"" );
                }
            }
            catch ( Exception e ) {
                errors.add ( "ColumnIncludeFilters column \"" + key + "\" not found in table.");
            }
        }
    }
    // Get exclude filter columns and glob-style regular expressions.
    int [] columnExcludeFiltersNumbers = new int[0];
    String [] columnExcludeFiltersRegex = null;
    if ( columnExcludeFilters != null ) {
        LinkedHashMap<String, String> map = columnExcludeFilters.getLinkedHashMap();
        columnExcludeFiltersNumbers = new int[map.size()];
        columnExcludeFiltersRegex = new String[map.size()];
        int ikey = -1;
        String key = null;
        for ( Map.Entry<String,String> entry : map.entrySet() ) {
            ++ikey;
            columnExcludeFiltersNumbers[ikey] = -1;
            try {
                key = entry.getKey();
                columnExcludeFiltersNumbers[ikey] = table.getFieldIndex(key);
                columnExcludeFiltersRegex[ikey] = map.get(key);
                // Turn default globbing notation into internal Java regex notation.
                if ( ignoreCase ) {
                	// Use upper case for comparisons.
                	columnExcludeFiltersRegex[ikey] = columnExcludeFiltersRegex[ikey].replace("*", ".*").toUpperCase();
                }
                else {
                	// Use original case for comparisons.
                	columnExcludeFiltersRegex[ikey] = columnExcludeFiltersRegex[ikey].replace("*", ".*");
                }
                if ( Message.isDebugOn ) {
                	Message.printStatus ( 2, routine, "Exclude filter column \"" + key + "\" [" +
                		columnExcludeFiltersNumbers[ikey] + "] regex \"" + columnExcludeFiltersRegex[ikey] + "\"" );
                }
            }
            catch ( Exception e ) {
                errors.add ( "ColumnExcludeFilters column \"" + key + "\" not found in table.");
            }
        }
    }
    // Loop through the table and match rows.
    List<TableRecord> matchedRows = new ArrayList<>();
    if ( doRow ) {
    	// Matching based on the row number.
    	int irow = -1;
    	if ( row.equalsIgnoreCase("Last") ) {
    		irow = table.getNumberOfRecords() - 1;
    	}
    	else {
    		// Command parameter Row has value 1+, but table is 0+.
    		irow = Integer.valueOf(row) - 1;
    	}
    	if ( (irow >= 0) && (irow < table.getNumberOfRecords()) ) {
    		try {
    			matchedRows.add(table.getRecord(irow));
    		}
    		catch ( Exception e ) {
    			errors.add ( "Error getting table row: " + row );
    		}
    	}
    	else if ( irow < 0 ) {
    		errors.add ( "Requested row (" + row + ") is invalid (must be >= 1)." );
    	}
    	else {
    		errors.add ( "Requested row (" + row + ") is invalid (is greater than table number of rows " + table.getNumberOfRecords() + ")." );
    	}
    }
    else {
    	// Try matching rows based on the include and exclude filters.
    	boolean includeFilterMatches;
    	boolean excludeFilterMatches;
    	int icol;
    	Object o;
    	String s;
    	boolean debug = false; // Set to true when troubleshooting code.
    	for ( int irow = 0; irow < table.getNumberOfRecords(); irow++ ) {
        	// Initialize the filter result:
    		// - true is as if the previous item was matched
    		// - if AND the current value must match
    		// - if OR, the previous 'filterMatches' must be true
    		// - also use true if no input filters
        	includeFilterMatches = true;
        	if ( debug ) {
            	Message.printStatus ( 2, routine, "columnIncludeFiltersNumbers.length=" + columnIncludeFiltersNumbers.length );
        	}
        	// Check input filters to see if the row is matched.
        	if ( columnIncludeFiltersNumbers.length > 0 ) {
            	// Filters can be done on any columns so loop through to see if row matches:
        		// - the row is assumed to match until a condition occurs that indicates not a match
        		// - the default is all filters AND'ed, but each item can optionally be OR'ed
        		// - icol will be 0, 1, ...
            	for ( icol = 0; icol < columnIncludeFiltersNumbers.length; icol++ ) {
                	if ( columnIncludeFiltersNumbers[icol] < 0 ) {
                		if ( debug ) {
                    		Message.printStatus ( 2, routine, "No match because columnIncludeFiltersNumbers[" + icol + "] < 0" );
                		}
                    	includeFilterMatches = false;
                    	break;
                	}
                	try {
                		// Have a column to check so get the value.
                    	o = table.getFieldValue(irow, columnIncludeFiltersNumbers[icol]);
                    	if ( o == null ) {
                    		if ( columnIncludeFiltersRegex[icol].isEmpty() ) {
                    			// The filter is an empty string so match null:
                    			// - no need to compare the value below
                    			continue;
                    		}
                    		else {
                    			// Don't match nulls when checking values.
                    			includeFilterMatches = false;
                    			if ( debug ) {
                    				Message.printStatus ( 2, routine, "No match because columnIncludeFiltersNumbers[" + icol + "] object is null." );
                    			}
                    			break;
                    		}
                    	}
                    	else if ( ignoreCase ) {
                    		// Compare using upper case.
                    		s = ("" + o).toUpperCase();
                    	}
                    	else {
                    		// Compare using original case.
                    		s = ("" + o);
                    	}
                    	if ( s.isEmpty() & columnIncludeFiltersRegex[icol].isEmpty() ) {
                    		// Want to include empty values.
                    	}
                    	else if ( s.matches(columnIncludeFiltersRegex[icol]) ) {
                    		// A filter matches.
                    		/*
                    		if ( columnIncludeOrs[icol] ) {
                    			// Increase the count of matching ORs.
                    			++columnIncludeOrMatchCount;
                    		}
                    		*/
                    	}
                    	else {
                        	// A filter did not match so don't include the record.
                    		/*
                    		if ( columnIncludeOrs[icol] ) {
                    			// Evaluating an OR:
                    			// - nothing to do but need to continue evaluating
                    		}
                    		else {
                    		*/
                    			// Evaluating an AND:
                    			// - did not match so can't continue
                    			includeFilterMatches = false;
                    			if ( debug ) {
                    				Message.printStatus ( 2, routine, "Skipping include filter because value \"" + s +
                    					"\" does not match columnIncludeFiltersRegex[" + icol + "] = " + columnIncludeFiltersRegex[icol] );
                    			}
                    			break;
                    		//}
                    	}
                	}
                	catch ( Exception e ) {
                    	errors.add ( "Error getting table data for [" + irow + "][" +
                    		columnIncludeFiltersNumbers[icol] + "] (" + e + ")." );
                    	includeFilterMatches = false;
                	}
            	}
            	if ( includeFilterMatches && (columnIncludeOrCount > 0) && (columnIncludeOrMatchCount == 0) ) {
            		// All AND filters were matched but did not match any OR filters so no match.
            		includeFilterMatches = false;
            	}
            	if ( includeFilterMatches ) {
            		// Row was matched.
            		if ( debug ) {
                		Message.printStatus ( 2, routine, "Include filter(s) match row [" + irow + "] ... not skipping." );
            		}
            	}
            	else {
                	// Skip the record.
            		if ( debug ) {
                		Message.printStatus ( 2, routine, "Include filter(s) do not match row [" + irow + "] ... skipping." );
            		}
                	continue;
            	}
        	}
   			if ( debug ) {
   				Message.printStatus ( 2, routine, "After evaluating include filters, includeFilterMatches=" + includeFilterMatches );
   			}
        	// Check input filter to see if the row is matched:
        	// - this is a copy of the above code with "include" replaced with "exclude"
        	// - initialize the exclude filters to true but check if any were specified at the end for the final result
        	// Set exclude to true by default but check for whether any exclude filters are specified.
        	excludeFilterMatches = true;
        	if ( columnExcludeFiltersNumbers.length > 0 ) {
            	// Filters can be done on any columns so loop through to see if row matches:
        		// - the row is assumed to match until a condition occurs that indicates not a match
        		// - the default is all filters AND'ed, but each item can optionally be OR'ed
        		// - icol will be 0, 1, ...
            	for ( icol = 0; icol < columnExcludeFiltersNumbers.length; icol++ ) {
                	if ( columnExcludeFiltersNumbers[icol] < 0 ) {
                		if ( debug ) {
                    		Message.printStatus ( 2, routine, "No match because columnExcludeFiltersNumbers[" + icol + "] < 0" );
                		}
                    	excludeFilterMatches = false;
                    	break;
                	}
                	try {
                		// Have a column to check so get the value.
                    	o = table.getFieldValue(irow, columnExcludeFiltersNumbers[icol]);
                    	if ( o == null ) {
                    		if ( columnExcludeFiltersRegex[icol].isEmpty() ) {
                    			// The filter is an empty string so match null:
                    			// - no need to compare the value below
                    			continue;
                    		}
                    		else  {
                    			// Don't match nulls when checking values.
                    			excludeFilterMatches = false;
                    			if ( debug ) {
                    				Message.printStatus( 2, routine, "No match because columnExcludeFiltersNumbers[" + icol + "] object is null." );
                    			}
                    			break;
                    		}
                    	}
                    	else if ( ignoreCase ) {
                    		// Compare using upper case.
                    		s = ("" + o).toUpperCase();
                    	}
                    	else {
                    		// Compare using original case.
                    		s = ("" + o);
                    	}
                    	if ( s.isEmpty() & columnExcludeFiltersRegex[icol].isEmpty() ) {
                    		// Want to exclude empty values.
                    	}
                    	else if ( s.matches(columnExcludeFiltersRegex[icol]) ) {
                    		// A filter matches.
                    		/*
                    		if ( columnExcludeOrs[icol] ) {
                    			// Increase the count of matching ORs.
                    			++columnExcludeOrMatchCount;
                    		}
                    		*/
                    	}
                    	else {
                        	// A filter did not match so don't exclude the record.
                    		/*
                    		if ( columnExcludeOrs[icol] ) {
                    			// Evaluating an OR:
                    			// - nothing to do but need to continue evaluating
                    		}
                    		else {
                    		*/
                    			// Evaluating an AND:
                    			// - did not match so can't continue
                    			excludeFilterMatches = false;
                    			if ( debug ) {
                    				Message.printStatus ( 2, routine, "Skipping exclude filter because value \"" + s +
                    					"\" does not match columnExcludeFiltersRegex[" + icol + "] = " + columnExcludeFiltersRegex[icol] );
                    			}
                    			break;
                    		//}
                    	}
                	}
                	catch ( Exception e ) {
                    	errors.add ( "Error getting table data for [" + irow + "][" +
                    		columnExcludeFiltersNumbers[icol] + "] (" + e + ")." );
                    	excludeFilterMatches = false;
                	}
                	if ( debug ) {
   				      	Message.printStatus ( 2, routine, "After evaluating exclude filters, excludeFilterMatches=" + excludeFilterMatches );
   			        }
            	}
            	if ( excludeFilterMatches ) {
            		// Row was matched.
            		if ( debug ) {
                		Message.printStatus ( 2, routine, "Exclude filter(s) match row [" + irow + "] ... skipping." );
            		}
                	continue;
            	}
            	else {
                	// Did not match the row don't omit.
            		if ( debug ) {
                		Message.printStatus ( 2, routine, "Exclude filter(s) do not match row [" + irow + "]." );
            		}
            	}
        	}
        	// Initialize the final filter to the result of the include filters.
        	boolean filterMatches = includeFilterMatches;
        	if ( (columnExcludeFilters.size() > 0) && excludeFilterMatches ) {
        		// The needs to be excluded.
        		filterMatches = false;
        	}
        	if ( filterMatches ) {
        		// If here then the row should be included.
        		try {
        			if ( debug ) {
        				Message.printStatus ( 2, routine, "Matched table row [" + irow + "]" );
        			}
        			matchedRows.add(table.getRecord(irow));
        		}
        		catch ( Exception e ) {
        			// Should not happen since row was accessed above.
        			errors.add ( "Error getting table data for row [" + irow + "] (" + e + ")." );
        		}
        	}
    	}
    }
    return matchedRows;
}

/**
Return the property defined in discovery phase.
@return the property defined in discovery phase
*/
private Prop getDiscoveryProp () {
    return __discovery_Prop;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  Prop
@return the list of data objects read by this object in discovery mode
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
    Prop discovery_Prop = getDiscoveryProp ();
    if ( discovery_Prop == null ) {
        return null;
    }
    Prop prop = new Prop();
    // Check for TS request or class that matches the data.
    if ( c == prop.getClass() ) {
        List<T> v = new ArrayList<>(1);
        v.add ( (T)discovery_Prop );
        return v;
    }
    else {
        return null;
    }
}

// Use base class parseCommand().

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
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommandInternal", message = "";
	int warning_level = 2;
	int log_level = 3; // Level for non-user messages for log file.
	String command_tag = "" + command_number;
	int warning_count = 0;

    CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // Default.
	CommandProcessor processor = getCommandProcessor();
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
        setDiscoveryProp ( null );
    }

	PropList parameters = getCommandParameters();

    String TableID = parameters.getValue ( "TableID" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		TableID = TSCommandProcessorUtil.expandParameterValue(processor, this, TableID);
    }
    String Column = parameters.getValue ( "Column" );
    if ( commandPhase == CommandPhaseType.RUN )  {
   		Column = TSCommandProcessorUtil.expandParameterValue(processor, this, Column);
    }
    String ColumnIncludeFilters = parameters.getValue ( "ColumnIncludeFilters" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		ColumnIncludeFilters = TSCommandProcessorUtil.expandParameterValue(processor, this, ColumnIncludeFilters);
    }
    StringDictionary columnIncludeFilters = new StringDictionary(ColumnIncludeFilters,":",",");
    // Check for OR operator | at the front of values:
    // - set a boolean in the array and remove the vertical bar
    /*
    LinkedHashMap<String,String> includeMap = columnIncludeFilters.getLinkedHashMap();
    boolean [] columnIncludeOrs = new boolean[0];
    if ( (ColumnIncludeFilters != null) && !ColumnIncludeFilters.isEmpty() ) {
    	int i = -1;
        for ( Map.Entry<String,String> entry : includeMap.entrySet() ) {
        	++i;
            String key = entry.getKey();
            String value = entry.getValue();
           	columnIncludeOrs[i] = false;
            if ( key.startsWith("|") ) {
            	if ( value.length() > 1 ) {
            		includeMap.put(key, value.substring(1));
            	}
            	else {
            		includeMap.put(key, "");
            	}
            	columnIncludeOrs[i] = true;
            	// Also adjust the first item.
            	if ( i == 1 ) {
            		// Include matching column [0] or column [1] or ...
            		columnIncludeOrs[0] = true;
            	}
            }
        }
    }
    */
    // Expand the filter information.
    /*
   	 * TODO smalers 2025-01-16 no need for the complexity since expanded above before conversion to map.
    if ( (ColumnIncludeFilters != null) && (ColumnIncludeFilters.indexOf("${") >= 0) ) {
        LinkedHashMap<String, String> map = columnIncludeFilters.getLinkedHashMap();
        String key = null;
        for ( Map.Entry<String,String> entry : map.entrySet() ) {
            key = entry.getKey();
            // Expand the key and the value (from original key).
            String key2 = TSCommandProcessorUtil.expandParameterValue(processor,this,key);
            // Since the property value might contain ., have to escape:
            // - TODO smalers 2022-02-11 there may be other patterns that need to be escaped - comment out for now, need to evaluate.
            String value = map.get(key);
            //if ( value != null ) {
            //	value = value.replace(".","\\.");
            //}
            map.put(key2, TSCommandProcessorUtil.expandParameterValue(processor,this,value));
            // Remove the original unexpanded entry if a different key.
            if ( !key.equals(key2) ) {
            	map.remove(key);
            }
        }
    }
    */
    String ColumnExcludeFilters = parameters.getValue ( "ColumnExcludeFilters" );
    if ( commandPhase == CommandPhaseType.RUN ) {
   		ColumnExcludeFilters = TSCommandProcessorUtil.expandParameterValue(processor, this, ColumnExcludeFilters);
    }
    StringDictionary columnExcludeFilters = new StringDictionary(ColumnExcludeFilters,":",",");
    // Check for OR operator | at the front of values:
    // - set a boolean in the array and remove the vertical bar
    /*
    LinkedHashMap<String,String> excludeMap = columnIncludeFilters.getLinkedHashMap();
    boolean [] columnExcludeOrs = new boolean[0];
    if ( (ColumnExcludeFilters != null) && !ColumnExcludeFilters.isEmpty() ) {
    	int i = -1;
        for ( Map.Entry<String,String> entry : excludeMap.entrySet() ) {
        	++i;
            String key = entry.getKey();
            String value = entry.getValue();
           	columnExcludeOrs[i] = false;
            if ( key.startsWith("|") ) {
            	if ( value.length() > 1 ) {
            		excludeMap.put(key, value.substring(1));
            	}
            	else {
            		excludeMap.put(key, "");
            	}
            	columnExcludeOrs[i] = true;
            	// Also adjust the first item.
            	if ( i == 1 ) {
            		// Exclude column [0] or column [1] ...
            		columnExcludeOrs[0] = true;
            	}
            }
        }
    }
    */
   	/*
   	 * TODO smalers 2025-01-16 no need for the complexity since expanded above before conversion to map.
    if ( (ColumnExcludeFilters != null) && (ColumnExcludeFilters.indexOf("${") >= 0) ) {
        LinkedHashMap<String, String> map = columnExcludeFilters.getLinkedHashMap();
        String key = null;
        for ( Map.Entry<String,String> entry : map.entrySet() ) {
            key = entry.getKey();
            // Expand the key and the value (from original key).
            String key2 = TSCommandProcessorUtil.expandParameterValue(processor,this,key);
            // Since the property value might contain ., have to escape:
            // - TODO smalers 2022-02-11 there may be other patterns that need to be escaped - comment out for now, need to evaluate.
            String value = map.get(key);
            //if ( value != null ) {
            //	value = value.replace(".","\\.");
            //}
            map.put(key2, TSCommandProcessorUtil.expandParameterValue(processor,this,value));
            // Remove the original unexpanded entry if a different key.
            if ( !key.equals(key2) ) {
            	map.remove(key);
            }
        }
    }
    */
    String IgnoreCase = parameters.getValue ( "IgnoreCase" );
    boolean ignoreCase = true; // Default.
    if ( (IgnoreCase != null) && IgnoreCase.equalsIgnoreCase(this._False)) {
    	ignoreCase = false;
    }
    String Row = parameters.getValue ( "Row" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	Row = TSCommandProcessorUtil.expandParameterValue(processor, this, Row);
    }
    String PropertyName = parameters.getValue ( "PropertyName" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	PropertyName = TSCommandProcessorUtil.expandParameterValue(processor, this, PropertyName);
    }
    String DefaultValue = parameters.getValue ( "DefaultValue" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	DefaultValue = TSCommandProcessorUtil.expandParameterValue(processor, this, DefaultValue);
    }
    String RowCountProperty = parameters.getValue ( "RowCountProperty" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	RowCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, RowCountProperty);
    }
    String ColumnCountProperty = parameters.getValue ( "ColumnCountProperty" );
    if ( commandPhase == CommandPhaseType.RUN ) {
    	ColumnCountProperty = TSCommandProcessorUtil.expandParameterValue(processor, this, ColumnCountProperty);
    }

    // Get the table to process.

    DataTable table = null;
    if ( commandPhase == CommandPhaseType.RUN ) {
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
	    Prop prop = null;
	    if ( commandPhase == CommandPhaseType.RUN ) {
		    if ( (PropertyName != null) && !PropertyName.isEmpty() ) {
		    	// Setting a property from a cell value.
		    	String propertyName = TSCommandProcessorUtil.expandParameterValue(processor,this,PropertyName);

		    	// Make sure that include filter columns exist in the table.
		    	for ( Map.Entry<String,String> entry : columnIncludeFilters.getLinkedHashMap().entrySet() ) {
		    		String key = entry.getKey();
		    		try {
		    			table.getFieldIndex(key);
		    		}
		    		catch ( Exception e ) {
		    			message = "Unable to find table column \"" + key + "\" used in ColumnIncludeFilters.";
		    			Message.printWarning ( warning_level,
		    				MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
		    			status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
		    				message, "Verify that a table column exists." ) );
		    		}
		    	}

		    	// Make sure that exclude filter columns exist in the table.
		    	for ( Map.Entry<String,String> entry : columnExcludeFilters.getLinkedHashMap().entrySet() ) {
		    		String key = entry.getKey();
		    		try {
		    			table.getFieldIndex(key);
		    		}
		    		catch ( Exception e ) {
		    			message = "Unable to find table column \"" + key + "\" used in ColumnExcludeFilters.";
		    			Message.printWarning ( warning_level,
		    				MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
		    			status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
		    				message, "Verify that a table column exists." ) );
		    		}
		    	}

		    	// THe filters may match 1+ rows so use the first match to set the property.
	    		List<String> errors = new ArrayList<>();
	        	List<TableRecord> records = findTableRecords (
	        		table,
	        		columnIncludeFilters,
	        		//columnIncludeOrs,
	        		columnExcludeFilters,
	        		//columnExcludeOrs,
	        		ignoreCase,
	        		Row, errors );
	        	for ( String error : errors ) {
	        		Message.printWarning(3, routine, "Error: " + error);
	        	}
	        	Message.printStatus(2, routine, "Found " + records.size() + " matching records.");
	        	if ( records.size() <= 0 ) {
	        		// Set to the default value if specified.
	        		String propValue = null;
	        		if ( (DefaultValue == null) || DefaultValue.isEmpty() ) {
	        			// Unset the property by setting to null.
	        			propValue = null;
	        		}
	        		else if ( DefaultValue.equalsIgnoreCase("Blank") ) {
	        			propValue = "";
	        		}
	        		else if ( DefaultValue.equalsIgnoreCase("Null") ) {
	        			propValue = null;
	        		}
	        		else {
	        			propValue = DefaultValue;
	        		}
	        		if ( propValue == null ) {
	        			prop = new Prop(propertyName, propValue, "", Prop.SET_AT_RUNTIME_BY_USER);
	        		}
	        		else {
	        			prop = new Prop(propertyName, propValue, propValue, Prop.SET_AT_RUNTIME_BY_USER);
	        		}
	        	}
	        	else {
	        		// Have 1+ matching records so set the property based on the column value:
	        		// - use the first matching record
	        		int col = table.getFieldIndex(Column);
	        		if ( col < 0 ) {
		        		message = "Table with TableID=\"" + TableID + "\" does not contain Column=\"" + Column + "\". Can't set property.";
	                	Message.printWarning(warning_level,
	                    	MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
	                	status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                    	message, "Verify that the column exists in the table." ) );
	                	prop = null;
	        		}
	        		else {
	        			// Create an object with the column value.
		        		Object o = records.get(0).getFieldValue(col);
		        		Message.printStatus(2,routine,"Column \"" + Column + "\" col=" + col + " value="+ o);
		        		if ( o == null ) {
		        			prop = new Prop(propertyName, o, "", Prop.SET_AT_RUNTIME_BY_USER);
		        		}
		        		else {
		        			prop = new Prop(propertyName, o, "" + o, Prop.SET_AT_RUNTIME_BY_USER);
		        		}
	        		}
	        	}

	    		// Set the property in the processor.

	    		PropList requestParams = new PropList ( "" );
	    		requestParams.set ( "PropertyName", propertyName );
	    		if ( prop == null ) {
	    			requestParams.setUsingObject ( "PropertyValue", null );
	    		}
	    		else {
	    			// Set using the property contents (not the String value).
	    			requestParams.setUsingObject ( "PropertyValue", prop.getContents() );
	    		}
	    		try {
	            	processor.processRequest( "SetProperty", requestParams);
	    		}
	    		catch ( Exception e ) {
	    			message = "Error requesting SetProperty(Property=\"" + PropertyName + "\") from processor.";
	    			Message.printWarning(log_level,
    					MessageUtil.formatMessageTag( command_tag, ++warning_count),
    					routine, message );
	            	status.addToLog ( CommandPhaseType.RUN,
                    	new CommandLogRecord(CommandStatusType.FAILURE,
                        	message, "Report the problem to software support." ) );
	    		}
		    }

	    	// Set the property indicating the number of rows in the table.
        	if ( (RowCountProperty != null) && !RowCountProperty.equals("") ) {
            	int rowCount = 0;
            	if ( table != null ) {
                	rowCount = table.getNumberOfRecords();
            	}
            	PropList requestParams = new PropList ( "" );
            	requestParams.setUsingObject ( "PropertyName", RowCountProperty );
            	requestParams.setUsingObject ( "PropertyValue", new Integer(rowCount) );
            	try {
                	processor.processRequest( "SetProperty", requestParams);
            	}
            	catch ( Exception e ) {
                	message = "Error requesting SetProperty(Property=\"" + RowCountProperty + "\") from processor.";
                	Message.printWarning(log_level,
                    	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    	routine, message );
                	status.addToLog ( CommandPhaseType.RUN,
                    	new CommandLogRecord(CommandStatusType.FAILURE,
                        	message, "Report the problem to software support." ) );
            	}
        	}

	    	// Set the property indicating the number of columns in the table.
        	if ( (ColumnCountProperty != null) && !ColumnCountProperty.equals("") ) {
            	int colCount = 0;
            	if ( table != null ) {
                	colCount = table.getNumberOfFields();
            	}
            	PropList requestParams = new PropList ( "" );
            	requestParams.setUsingObject ( "PropertyName", ColumnCountProperty );
            	requestParams.setUsingObject ( "PropertyValue", new Integer(colCount) );
            	try {
                	processor.processRequest( "SetProperty", requestParams);
            	}
            	catch ( Exception e ) {
                	message = "Error requesting SetProperty(Property=\"" + ColumnCountProperty + "\") from processor.";
                	Message.printWarning(log_level,
                    	MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    	routine, message );
                	status.addToLog ( CommandPhaseType.RUN,
                    	new CommandLogRecord(CommandStatusType.FAILURE,
                        	message, "Report the problem to software support." ) );
            	}
        	}
        }
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create an empty property.
            prop = new Prop();
            prop.setKey ( PropertyName ); // OK if property name includes ${} in discovery mode.
            prop.setHowSet(Prop.SET_UNKNOWN);
            setDiscoveryProp ( prop );
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error setting property (" + e + ").";
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
Set the property defined in discovery phase.
@param prop Property set during discovery phase.
*/
private void setDiscoveryProp ( Prop prop ) {
    __discovery_Prop = prop;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"TableID",
		"Column",
		"ColumnIncludeFilters",
		"ColumnExcludeFilters",
		"IgnoreCase",
		"Row",
    	"PropertyName",
    	"DefaultValue",
		"RowCountProperty",
		"ColumnCountProperty"
	};
	return this.toString(parameters, parameterOrder);
}

}