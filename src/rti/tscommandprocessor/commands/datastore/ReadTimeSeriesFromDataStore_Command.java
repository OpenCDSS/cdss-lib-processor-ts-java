// ReadTimeSeriesFromDataStore_Command - This class initializes, checks, and runs the ReadTimeSeriesFromDataStore() command.

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

package rti.tscommandprocessor.commands.datastore;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import riverside.datastore.GenericDatabaseDataStore;
import riverside.datastore.GenericDatabaseDataStore_TimeSeries_InputFilter_JPanel;
import riverside.datastore.TimeSeriesMeta;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.DMI.DatabaseDataStore;
import RTi.TS.TS;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ReadTimeSeriesFromDataStore() command.
*/
public class ReadTimeSeriesFromDataStore_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Number of where clauses shown in the editor and available as parameters.
*/
private int __numFilterGroups = 4;

/**
Data values for boolean parameters.
*/
protected String _False = "False";
protected String _True = "True";

/**
Data values for IfMissing parameter.
*/
protected String _Ignore = "Ignore";
protected String _Warn = "Warn";

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Constructor.
*/
public ReadTimeSeriesFromDataStore_Command ()
{	super();
	setCommandName ( "ReadTimeSeriesFromDataStore" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String warning = "";
    String message;
    
    String DataStore = parameters.getValue ( "DataStore" );
    String DataType = parameters.getValue ( "DataType" );
    String Interval = parameters.getValue ( "Interval" );
    String InputStart = parameters.getValue ( "InputStart" );
    String InputEnd = parameters.getValue ( "InputEnd" );

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (DataStore == null) || DataStore.equals("") ) {
        message = "The datastore must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the datastore." ) );
    }
    
    if ( (DataType == null) || DataType.equals("") ) {
        message = "The data type must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data type." ) );
    }
    
    if ( (Interval == null) || Interval.equals("") ) {
        message = "The data interval must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data interval." ) );
    }

	// TODO SAM 2006-04-24 Need to check the WhereN parameters.

	if ( (InputStart != null) && !InputStart.equals("") &&
		!InputStart.equalsIgnoreCase("InputStart") && !InputStart.equalsIgnoreCase("InputEnd") &&
		(InputStart.indexOf("${") < 0) ) {
		try {
		    DateTime.parse(InputStart);
		}
		catch ( Exception e ) {
            message = "The input start date/time \"" + InputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a date/time or InputStart." ) );
		}
	}
	if ( (InputEnd != null) && !InputEnd.equals("") &&
		!InputEnd.equalsIgnoreCase("InputStart") && !InputEnd.equalsIgnoreCase("InputEnd") &&
		(InputEnd.indexOf("${") < 0)) {
		try {
		    DateTime.parse( InputEnd );
		}
		catch ( Exception e ) {
            message = "The input end date/time \"" + InputEnd + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a date/time or InputEnd." ) );
		}
	}

    // Check for invalid parameters...
    List<String> validList = new ArrayList<>(10+__numFilterGroups);
    validList.add ( "DataStore" );
    validList.add ( "DataType" );
    validList.add ( "Interval" );
    validList.add ( "LocationType" );
    validList.add ( "LocationID" );
    validList.add ( "DataSource" );
    validList.add ( "Scenario" );
    for ( int i = 1; i <= __numFilterGroups; i++ ) { 
        validList.add ( "Where" + i );
    }
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "Alias" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	String routine = getClass().getSimpleName() + ".editCommand";
	if ( Message.isDebugOn ) {
		Message.printDebug(1,routine,"Editing the command...getting active and discovery database datastores.");
	}
	List<DatabaseDataStore> dataStoreList =
		TSCommandProcessorUtil.getDatabaseDataStoresForEditors ( (TSCommandProcessor)this.getCommandProcessor(), this );
	// Reading and writing time series requires GenericDatabaseDataStore so further filter.
	List<GenericDatabaseDataStore> gdataStoreList = new ArrayList<>();
	for ( DatabaseDataStore datastore : dataStoreList ) {
		if ( datastore instanceof GenericDatabaseDataStore ) {
			gdataStoreList.add((GenericDatabaseDataStore)datastore);
		}
	}
	return (new ReadTimeSeriesFromDataStore_JDialog ( parent, this, gdataStoreList )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList () {
    return __discovery_TS_Vector;
}

/**
Return the number of filter groups to display in the editor.
*/
public int getNumFilterGroups () {
    return __numFilterGroups;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c ) {
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Also check the base class
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
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
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = Boolean.TRUE; // Default.
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
    
    boolean readData = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
        readData = false;
    }
    
    String DataStore = parameters.getValue("DataStore");
    String DataType = parameters.getValue("DataType");
    //int pos = DataType.indexOf("-");
    //if ( pos > 0 ) {
    //    DataType = DataType.substring(0,pos);
    //}
    String Interval = parameters.getValue("Interval");
    String LocationType = parameters.getValue("LocationType");
    if ( LocationType == null ) {
        LocationType = ""; // To simplify code below
    }
    String LocationID = parameters.getValue("LocationID");
    if ( LocationID == null ) {
        LocationID = ""; // To simplify code below
    }
    String DataSource = parameters.getValue("DataSource");
    if ( DataSource == null ) {
        DataSource = ""; // To simplify code below
    }
    String Scenario = parameters.getValue("Scenario");
    if ( Scenario == null ) {
        Scenario = ""; // To simplify code below
    }
    String Alias = parameters.getValue("Alias");

    String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}"; // Global default
	}
	String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}"; // Global default
	}
	DateTime InputStart_DateTime = null;
	DateTime InputEnd_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			InputStart_DateTime = TSCommandProcessorUtil.getDateTime ( InputStart, "InputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			InputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( InputEnd, "InputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Now try to read...

	List<TS> tslist = new ArrayList<TS>();	// List for time series results.
					// Will be added to for one time series read or replaced if a list is read.
	try {
        // Find the data store to use...
        DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
            DataStore, GenericDatabaseDataStore.class );
        if ( dataStore == null ) {
            message = "Could not get data store for name \"" + DataStore + "\" to query data.";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the datastore has been configured with name \"" +
                    DataStore + "\" and is available." ) );
            throw new Exception ( message );
        }
        GenericDatabaseDataStore ds = (GenericDatabaseDataStore)dataStore;
        List<TimeSeriesMeta> tsMetaList = null;
	    if ( !LocationID.equals("") ) {
	        // The LocationID is specified so read one time series.  Do this by initializing a single metadata object.
	        tsMetaList = new ArrayList<TimeSeriesMeta>();
	        TimeSeriesMeta tsMetadata = new TimeSeriesMeta ( LocationType, LocationID, DataSource, DataType, Interval,
	            Scenario, "", "", -1 );
	        tsMetaList.add ( tsMetadata );
	        Message.printStatus ( 2, routine, "Single time series was requested for location ID \"" + LocationID + "\"." );
	    }
	    else {
            // Read 1+ time series...
    		List<String> WhereN_Vector = new ArrayList<String>( 6 );
    		String WhereN;
    		int nfg = 0; // Used below.
    		for ( nfg = 0; nfg < 100; nfg++ ) {
    			WhereN = parameters.getValue ( "Where" + (nfg + 1) );
    			if ( WhereN == null ) {
    				break;	// No more where clauses
    			}
    			WhereN_Vector.add ( WhereN );
    		}
    	
    		// Initialize an input filter based on the data type...
    
    		GenericDatabaseDataStore_TimeSeries_InputFilter_JPanel filterPanel =
    		    new GenericDatabaseDataStore_TimeSeries_InputFilter_JPanel(ds, getNumFilterGroups());
    
    		// Populate with the where information from the command...
    
    		String filterDelim = ";";
    		for ( int ifg = 0; ifg < nfg; ifg ++ ) {
    			WhereN = (String)WhereN_Vector.get(ifg);
                if ( WhereN.length() == 0 ) {
                    continue;
                }
    			// Set the filter...
    			try {
                    filterPanel.setInputFilter( ifg, WhereN, filterDelim );
    			}
    			catch ( Exception e ) {
                    message = "Error setting where information using \""+WhereN+"\"";
    				Message.printWarning ( 2, routine,message);
    				Message.printWarning ( 3, routine, e );
    				++warning_count;
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support - also see the log file." ) );
    			}
    		}
    		
    		// Read the list of metadata from which identifiers can be obtained.
    	
    		Message.printStatus ( 2, routine, "Getting the list of time series metadata using filter(s)..." );
    	
    		// The data type in the command is "ObjectType - DataCommonName", which is OK for the following call
            tsMetaList = ds.readTimeSeriesMetaList( DataType, Interval, filterPanel );
	    }
	    
        // Make sure that size is set...
        int size = 0;
        if ( tsMetaList != null ) {
            size = tsMetaList.size();
        }
	
   		if ( size == 0 ) {
			Message.printStatus ( 2, routine, "No datastore time series were found." );
	        // Warn if nothing was retrieved (can be overridden to ignore).
            message = "No time series were read from the datastore.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database." +
                    	"  Previous messages may provide more information." ) );
   		}
   		else {
			// Else, convert meta object to a TSID string and read the time series...

			Message.printStatus ( 2, "", "Reading " + size + " time series..." );

			String tsidentString = null;
			TS ts = null; // Time series to read.
			int i = -1;
			for ( TimeSeriesMeta meta : tsMetaList ) {
			    ++i;
				tsidentString = meta.getTSID(ds);
	
				message = "Reading datastore time series " + (i + 1) + " of " + size + " \"" + tsidentString + "\"";
				Message.printStatus ( 2, routine, message );
				notifyCommandProgressListeners ( i, size, (float)-1.0, message );
				try {
				    ts = ds.readTimeSeries ( tsidentString, InputStart_DateTime, InputEnd_DateTime, readData );
				    // Set the alias to the desired string - this is impacted by the Location parameter
				    if ( (Alias != null) && !Alias.equals("") ) {
                        String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                            processor, ts, Alias, status, commandPhase);
                        ts.setAlias ( alias );
				    }
					// Add the time series to the temporary list.  It will be further processed below...
					tslist.add ( ts );
				}
				catch ( Exception e ) {
					message = "Unexpected error reading datastore time series (" + e + ").";
					Message.printWarning ( 3, routine, message );
					Message.printWarning ( 3, routine, e );
					++warning_count;
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report the problem to software support - also see the log file." ) );
				}
			}
		}
    
        size = 0;
        if ( tslist != null ) {
            size = tslist.size();
        }
        Message.printStatus ( 2, routine, "Read " + size + " datastore time series." );

        if ( commandPhase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing datastore time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                    // Don't throw an exception - probably due to missing data.
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding datastore time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
            }
        }
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            setDiscoveryTSList ( tslist );
        }
        // Warn if nothing was retrieved (can be overridden to ignore).
        if ( (tslist == null) || (size == 0) ) {
            message = "No time series were read from the datastore.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database.  See previous messages." ) );
    }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error reading time series from datastore (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
               message, "Report the problem to software support - also see the log file." ) );
		throw new CommandException ( message );
	}

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		throw new CommandWarningException ( message );
	}
    
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List<TS> discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder1 = {
    	"DataStore",
    	"DataType",
    	"Interval",
    	"LocationType",
    	"LocationID",
    	"DataSource",
    	"Scenario"
	};

	String delim = ";";
    List<String> whereParameters = new ArrayList<>();
    for ( int i = 1; i <= __numFilterGroups; i++ ) {
    	String where = parameters.getValue("Where" + i);
    	if ( (where != null) && (where.length() > 0) && !where.startsWith(delim) ) {
    		whereParameters.add ( "Where" + i );
    	}
    }

	String [] parameterOrder2 = {
		"InputStart",
		"InputEnd",
		"Alias"
	};

	// Form the final property list.
	String [] parameterOrder = new String[parameterOrder1.length + whereParameters.size() + parameterOrder2.length];
	int iparam = 0;
	for ( int i = 0; i < parameterOrder1.length; i++ ) {
		parameterOrder[iparam++] = parameterOrder1[i];
	}
	for ( int i = 0; i < whereParameters.size(); i++ ) {
		parameterOrder[iparam++] = whereParameters.get(i);
	}
	for ( int i = 0; i < parameterOrder2.length; i++ ) {
		parameterOrder[iparam++] = parameterOrder2[i];
	}
	return this.toString(parameters, parameterOrder);
}

}