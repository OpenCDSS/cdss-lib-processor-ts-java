// ReadReclamationPisces_Command - This class initializes, checks, and runs the ReadReclamationPisces() command.

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

package rti.tscommandprocessor.commands.reclamationpisces;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.TS.TS;
import RTi.Util.GUI.InputFilter_JPanel;
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
import RTi.Util.IO.MissingObjectEvent;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ReadReclamationPisces() command.
*/
public class ReadReclamationPisces_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Number of where clauses shown in the editor and available as parameters.
*/
private int __numWhere = 4;

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
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public ReadReclamationPisces_Command ()
{	super();
	setCommandName ( "ReadReclamationPisces" );
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

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    String DataStore = parameters.getValue ( "DataStore" );
    String InputStart = parameters.getValue ( "InputStart" );
    String InputEnd = parameters.getValue ( "InputEnd" );
    
    if ( (DataStore == null) || (DataStore.length() == 0) ) {
        message = "The data store must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store." ) );
    }

	if ( (InputStart != null) && !InputStart.equals("") &&
		!InputStart.equalsIgnoreCase("InputStart") &&
		!InputStart.equalsIgnoreCase("InputEnd") && (InputStart.indexOf("${") < 0) ) {
		try {	DateTime.parse(InputStart);
		}
		catch ( Exception e ) {
            message = "The input start date/time \"" + InputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a date/time or InputStart." ) );
		}
	}
	if (	(InputEnd != null) && !InputEnd.equals("") &&
		!InputEnd.equalsIgnoreCase("InputStart") &&
		!InputEnd.equalsIgnoreCase("InputEnd") && (InputEnd.indexOf("${") < 0) ) {
		try {	DateTime.parse( InputEnd );
		}
		catch ( Exception e ) {
            message = "The input end date/time \"" + InputEnd + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify a date/time or InputEnd." ) );
		}
	}

	String IfMissing = parameters.getValue ( "IfMissing" );
    if ( (IfMissing != null) && !IfMissing.equals("") &&
            !IfMissing.equalsIgnoreCase(_Warn) && !IfMissing.equalsIgnoreCase(_Ignore) ) {
            message = "The IfMissing parameter \"" + IfMissing +
            "\" must be blank, " + _Ignore + ", or " + _Warn;
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify " + _Ignore + ", " + _Warn +
                            ", or blank for default of " + _Warn + "." ) );
        }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>();
    validList.add ( "DataStore" );
    validList.add ( "DataType" );
    validList.add ( "Interval" );
    for ( int i = 1; i <= __numWhere; i++ ) { 
        validList.add ( "Where" + i );
    }
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "Alias" );
    validList.add ( "IfMissing" );
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
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
	List<TS> discovery_TS_List = getDiscoveryTSList ();
    if ( (discovery_TS_List == null) || (discovery_TS_List.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_List.get(0);
    // Also check the base class
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return (List<T>)discovery_TS_List;
    }
    else {
        return null;
    }
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new ReadReclamationPisces_JDialog ( parent, this )).ok();
}

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
    
    boolean read_data = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
        read_data = false;
    }
    
	String InputStart = parameters.getValue ( "InputStart" );
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}";
	}
	String InputEnd = parameters.getValue ( "InputEnd" );
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}";
	}
    String Alias = parameters.getValue ( "Alias" );
    String IfMissing = parameters.getValue ("IfMissing" );
    boolean IfMissingWarn = true;  // Default
    if ( (IfMissing != null) && IfMissing.equalsIgnoreCase(_Ignore) ) {
        IfMissingWarn = false;  // Ignore when time series are not found
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

	List<TS> tslist = new ArrayList<TS>();
	try {
        // Read 1+ time series...
		// Get the input needed to process the file...
		String DataType = parameters.getValue ( "DataType" );
		String Interval = parameters.getValue ( "Interval" );
		List<String> whereNList = new ArrayList<String> ( 6 );
		String WhereN;
		int nfg = 0;	// Used below.
		for ( nfg = 0; nfg < 1000; nfg++ ) {
			WhereN = parameters.getValue ( "Where" + (nfg + 1) );
			if ( WhereN == null ) {
				break; // No more where clauses
			}
			whereNList.add ( WhereN );
		}
	
		// Find the ReclamationPiscesDMI to use...
		ReclamationPiscesDMI dmi = null;
		ReclamationPiscesDataStore ds = null;
		// First try to get from the DataStore list...
		String DataStore = parameters.getValue ( "DataStore" );
		DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
	         DataStore, ReclamationPiscesDataStore.class );
        if ( dataStore != null ) {
            // Found a datastore so use it...
            ds = (ReclamationPiscesDataStore)dataStore;
            dmi = (ReclamationPiscesDMI)ds.getDMI();
        }
		if ( dmi == null ) {
			message ="Could not find Pisces datastore using \"" + DataStore + "\".";
			Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a Pisces database connection has been opened." ) );
			throw new Exception ( message );
		}

		// Initialize an input filter based on the data type...

		InputFilter_JPanel filterPanel = null;

		// Create the input filter panel...

		filterPanel = new ReclamationPisces_TimeSeries_InputFilter_JPanel( ds, __numWhere );

		// Populate with the where information from the command...

		String filter_delim = ";";
		for ( int ifg = 0; ifg < nfg; ifg ++ ) {
			WhereN = whereNList.get(ifg);
            if ( WhereN.length() == 0 ) {
                continue;
            }
			// Set the filter...
			try {
                filterPanel.setInputFilter( ifg, WhereN, filter_delim );
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
	
		// Read the list of objects from which identifiers can be obtained.
	
		Message.printStatus ( 2, routine, "Getting the list of time series..." );
	
		String siteID = null;
		String server = null;
		List<ReclamationPisces_SiteCatalogSeriesCatalog> tsHeaderList =
			dmi.readSiteCatalogSeriesCatalogList(siteID, server, DataType, Interval, filterPanel);
		// Make sure that size is set...
		int size = 0;
		if ( tsHeaderList != null ) {
			size = tsHeaderList.size();
		}
	
   		if ( (tsHeaderList == null) || (size == 0) ) {
			Message.printStatus ( 2, routine,"No Pisces time series were found." );
	        // Warn if nothing was retrieved (can be overridden to ignore).
            if ( IfMissingWarn ) {
                message = "No time series were read from Pisces.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Data may not be in database." +
                        	"  Previous messages may provide more information." ) );
            }
            else {
                // Ignore the problem.  Call it a success if no other problems occurred.
                status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
            }
            // Generate an event for listeners
            // FIXME SAM 2008-08-20 Need to put together a more readable id for reporting
            notifyCommandProcessorEventListeners(
                new MissingObjectEvent(DataType + ", " + Interval + ", see command for user-specified criteria",
                    Class.forName("RTi.TS.TS"),"Time Series", this));
			return;
   		}
	
		// Else, convert each header object to a TSID string and read the time series...

		Message.printStatus ( 2, "", "Reading " + size + " time series..." );

		String tsident_string = null; // TSIdent string
		TS ts; // Time series to read.
		ReclamationPisces_SiteCatalogSeriesCatalog catalog;
		String inputName = null;
	    // Use the datastore for the time series input name
	    inputName = "~" + DataStore;
		for ( int i = 0; i < size; i++ ) {
			catalog = (ReclamationPisces_SiteCatalogSeriesCatalog)tsHeaderList.get(i);
			tsident_string = catalog.getTSID() + inputName;
            // Update the progress
			message = "Reading Pisces time series " + (i + 1) + " of " + size + " \"" + tsident_string + "\"";
            notifyCommandProgressListeners ( i, size, (float)-1.0, message );
			try {
			    ts = dmi.readTimeSeries (
					tsident_string,
					InputStart_DateTime,
					InputEnd_DateTime, read_data );
				// Add the time series to the temporary list.  It will be further processed below...
                if ( (ts != null) && (Alias != null) && !Alias.equals("") ) {
                    ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, Alias, status, commandPhase) );
                }
                // Allow null to be added here.
				tslist.add ( ts );
			}
			catch ( Exception e ) {
				message = "Unexpected error reading Pisces time series \"" + tsident_string + "\" (" + e + ").";
				Message.printWarning ( 2, routine, message );
				Message.printWarning ( 2, routine, e );
				++warning_count;
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report the problem to software support - also see the log file." ) );
			}
		}
    
        size = 0;
        if ( tslist != null ) {
            size = tslist.size();
        }
        Message.printStatus ( 2, routine, "Read " + size + " Pisces time series." );

        if ( commandPhase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing Pisces time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding Pisces time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
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
            if ( IfMissingWarn ) {
                message = "No time series were read from Pisces.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Data may not be in database.  See previous messages." ) );
            }
            // Generate an event for listeners
            // TOD SAM 2008-08-20 Evaluate whether need here
            //notifyCommandProcessorEventListeners(new MissingObjectEvent(DataType + ", " + Interval + filter_panel,this));
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error reading time series from Pisces (" + e + ").";
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
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param props parameters for the command
*/
public String toString ( PropList props )
{   if ( props == null ) {
        return getCommandName() + "()";
    }
	StringBuffer b = new StringBuffer ();
    String DataStore = props.getValue("DataStore");
    String DataType = props.getValue("DataType");
    String Interval = props.getValue("Interval");
	String InputStart = props.getValue("InputStart");
	String InputEnd = props.getValue("InputEnd");
	String Alias = props.getValue("Alias");
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
    if ( (DataType != null) && (DataType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataType=\"" + DataType + "\"" );
    }
    if ( (Interval != null) && (Interval.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Interval=\"" + Interval + "\"" );
    }
	String delim = ";";
    for ( int i = 1; i <= __numWhere; i++ ) {
    	String where = props.getValue("Where" + i);
    	if ( (where != null) && (where.length() > 0) && !where.startsWith(delim) ) {
    		if ( b.length() > 0 ) {
    			b.append ( "," );
    		}
    		b.append ( "Where" + i + "=\"" + where + "\"" );
    	}
    }
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputStart=\"" + InputStart + "\"" );
	}
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputEnd=\"" + InputEnd + "\"" );
	}
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }
    String IfMissing = props.getValue("IfMissing");
    if ( (IfMissing != null) && (IfMissing.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IfMissing=" + IfMissing );
    }
    return getCommandName() + "("+ b.toString()+")";
}

}
