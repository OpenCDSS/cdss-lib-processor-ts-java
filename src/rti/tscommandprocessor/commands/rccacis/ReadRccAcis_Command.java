// ReadRccAcis_Command - This class initializes, checks, and runs the ReadRccAcis() command.

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

package rti.tscommandprocessor.commands.rccacis;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;

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
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ReadRccAcis() command.
*/
public class ReadRccAcis_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Number of where clauses shown in the editor and available as parameters - not much offered from ACIS so just put 3.
*/
private int __numFilterGroups = 3;

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
public ReadRccAcis_Command ()
{	super();
	setCommandName ( "ReadRccAcis" );
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
        message = "The data store must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store." ) );
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
		!InputStart.equalsIgnoreCase("InputStart") && !InputStart.equalsIgnoreCase("InputEnd") ) {
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
		!InputEnd.equalsIgnoreCase("InputStart") && !InputEnd.equalsIgnoreCase("InputEnd") ) {
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
    List<String> validList = new ArrayList<String>();
    validList.add ( "DataStore" );
    validList.add ( "DataType" );
    validList.add ( "Interval" );
    validList.add ( "SiteID" );
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
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the number of filter groups to display in the editor.
*/
public int getNumFilterGroups ()
{
    return __numFilterGroups;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be returned:  TS
*/
@SuppressWarnings("unchecked")
public <T> List<T> getObjectList ( Class<T> c )
{
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
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new ReadRccAcis_JDialog ( parent, this )).ok();
}

/**
Parse the command parameters.  Special care is taken here to translate legacy input filter values
to current RCC ACIS API values.
*/
public void parseCommand ( String command )
throws InvalidCommandParameterException, InvalidCommandSyntaxException
{
    super.parseCommand(command);
    // Look for WhereN parameters and translate the parameter names to the current syntax
    PropList parameters = getCommandParameters();
    for ( int i = 1; ; i++ ) {
        String whereN = "Where" + i;
        String propVal = parameters.getValue(whereN);
        if ( propVal == null ) {
            break;
        }
        // Check for Where clauses that need to be updated.
        int pos = propVal.indexOf(";");
        if ( StringUtil.startsWithIgnoreCase(propVal, "postal") ) {
            // Change to "state"
            parameters.set(whereN,"state"+propVal.substring(pos));
        }
        else if ( StringUtil.startsWithIgnoreCase(propVal, "clim_div") ) {
            // Change to "state"
            parameters.set(whereN,"climdiv"+propVal.substring(pos));
        }
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
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ReadRccAcis_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    
    boolean readData = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
        readData = false;
    }
    
    String DataStore = parameters.getValue("DataStore");
    String DataType = parameters.getValue("DataType");
    // This may be of the form "4" or "4 - Precipitation (daily") but only want the number
    int pos = DataType.indexOf("-");
    if ( pos > 0 ) {
        DataType = DataType.substring(0,pos);
    }
    String Interval = parameters.getValue("Interval");
    String SiteID = parameters.getValue("SiteID");
    if ( SiteID == null ) {
        SiteID = ""; // To simplify code below
    }
    String Alias = parameters.getValue("Alias");
    
	String InputStart = parameters.getValue ( "InputStart" );
	DateTime InputStart_DateTime = null;
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", InputStart );
		CommandProcessorRequestResultsBean bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
            message = "Error requesting DateTime(DateTime=" + InputStart + ") from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                   message, "Report problem to software support." ) );
		}
		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
            message = "Null value for DateTime(DateTime=" + InputStart + "\") returned from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a valid InputStart string has been specified." ) );
		}
		else {
		    InputStart_DateTime = (DateTime)prop_contents;
		}
	}
	else {
	    // Get from the processor...
		try {
            Object o = processor.getPropContents ( "InputStart" );
			if ( o != null ) {
				InputStart_DateTime = (DateTime)o;
			}
		}
		catch ( Exception e ) {
			message = "Error requesting InputStart from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report problem to software support." ) );
		}
	}
	String InputEnd = parameters.getValue ( "InputEnd" );
	DateTime InputEnd_DateTime = null;
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", InputEnd );
		CommandProcessorRequestResultsBean bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
            message = "Error requesting DateTime(DateTime=" + InputEnd + ") from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report problem to software support." ) );
		}
		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
            message = "Null value for DateTime(DateTime=" + InputEnd + ") returned from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a valid InputEnd has been specified." ) );
		}
		else {
		    InputEnd_DateTime = (DateTime)prop_contents;
		}
	}
	else {
	    // Get from the processor...
		try { Object o = processor.getPropContents ( "InputEnd" );
			if ( o != null ) {
				InputEnd_DateTime = (DateTime)o;
			}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting InputEnd from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
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

	List<TS> tslist = new Vector<TS>();	// List for time series results.
					// Will be added to for one time series
					// read or replaced if a list is read.
	try {
        // Find the data store to use...
        DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
            DataStore, RccAcisDataStore.class );
        if ( dataStore == null ) {
            message = "Could not get data store for name \"" + DataStore + "\" to query data.";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the RCC ACIS web service has been configured with name \"" +
                    DataStore + "\" and is available." ) );
            throw new Exception ( message );
        }
        RccAcisDataStore rccAcisDataStore = (RccAcisDataStore)dataStore;
        List<RccAcisStationTimeSeriesMetadata> tsMetadataList = null;
	    if ( !SiteID.equals("") ) {
	        // If the SiteID is specified so read one time series.  Do this by initializing a metadata object
	        // as if it were returned from the multi-station web service call
	        tsMetadataList = new Vector<RccAcisStationTimeSeriesMetadata>();
	        RccAcisStationTimeSeriesMetadata tsMetadata = new RccAcisStationTimeSeriesMetadata();
	        // Split the SiteID into parts if a station type is specified
	        String stationType = null;
	        String siteID2 = null; // SiteID with only site (no station type)
	        RccAcisStationType stationTypeO = null;
	        if ( SiteID.indexOf(":") > 0 ) {
	            String [] parts = SiteID.split(":");
	            stationType = parts[0].trim();
	            siteID2 = parts[1].trim();
    	        stationTypeO = rccAcisDataStore.lookupStationTypeFromType(stationType);
    	        if ( stationTypeO == null ) {
    	            message = "Could not get station type code from \"" + stationType + "\" to query data.";
    	            Message.printWarning ( 2, routine, message );
    	            status.addToLog ( commandPhase,
    	                new CommandLogRecord(CommandStatusType.FAILURE,
    	                    message, "Verify that SiteID is prefixed by a valid station type code." ) );
    	            throw new Exception ( message );
    	        }
	        }
	        String [] sids = { siteID2 };
	        if ( stationTypeO != null ) {
	            sids[0] = siteID2 + " " + stationTypeO.getCode();
	        }
	        tsMetadata.setDataStore(rccAcisDataStore);
	        tsMetadata.setSIds(sids);
	        RccAcisVariableTableRecord variable = rccAcisDataStore.lookupVariable(DataType);
            if ( variable == null ) {
                message = "Could not determine variable from \"" + DataType + "\" to query data.";
                Message.printWarning ( 2, routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the variable is valid." ) );
                throw new Exception ( message );
            }
	        tsMetadata.setVariable(variable);
	        tsMetadataList.add ( tsMetadata );
	        Message.printStatus ( 2, routine, "Single ACIS time series was requested for site ID \"" + sids[0] + "\"." );
	    }
	    else {
            // Read 1+ time series...
    		List<String> WhereN_Vector = new Vector<String> ( 6 );
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
    
    		RccAcis_TimeSeries_InputFilter_JPanel filterPanel =
    		    new RccAcis_TimeSeries_InputFilter_JPanel((RccAcisDataStore)dataStore, getNumFilterGroups());
    
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
    	
    		Message.printStatus ( 2, routine, "Getting the list of stations and time series metadata using filter(s)..." );
    	
    		// The data type in the command is "ObjectType - DataCommonName", which is OK for the following call
            tsMetadataList = rccAcisDataStore.readStationTimeSeriesMetadataList( DataType, Interval, filterPanel );
	    }
	    
        // Make sure that size is set...
        int size = 0;
        if ( tsMetadataList != null ) {
            size = tsMetadataList.size();
        }
	
   		if ( size == 0 ) {
			Message.printStatus ( 2, routine,"No RCC ACIS time series were found." );
	        // Warn if nothing was retrieved (can be overridden to ignore).
            message = "No time series were read from the RCC ACIS web service.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database." +
                    	"  Previous messages may provide more information." ) );
   		}
   		else {
			// Else, convert each header object to a TSID string and read the time series...

			Message.printStatus ( 2, "", "Reading " + size + " time series..." );

			String tsidentString = null;
			TS ts = null; // Time series to read.
			int i = -1;
			for ( RccAcisStationTimeSeriesMetadata meta : tsMetadataList ) {
			    ++i;
				tsidentString = meta.getTSID(rccAcisDataStore);
	
				message = "Reading RCC ACIS time series " + (i + 1) + " of " + size + " \"" + tsidentString + "\"...";
				Message.printStatus ( 2, routine, message );
				notifyCommandProgressListeners ( i, size, (float)-1.0, message );
				try {
				    ts = rccAcisDataStore.readTimeSeries ( tsidentString, InputStart_DateTime, InputEnd_DateTime, readData );
				    // Set the alias to the desired string - this is impacted by the Location parameter
                    String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                        processor, ts, Alias, status, commandPhase);
                    ts.setAlias ( alias );
					// Add the time series to the temporary list.  It will be further processed below...
					tslist.add ( ts );
				}
				catch ( Exception e ) {
					message = "Unexpected error reading RCC ACIS time series (" + e + ").";
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
        Message.printStatus ( 2, routine, "Read " + size + " RCC ACIS time series." );

        if ( commandPhase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing RCC ACIS time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                    // Don't throw an exception - probably due to missing data.
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding RCC ACIS time series after read.";
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
            message = "No time series were read from the RCC ACIS web service.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database.  See previous messages." ) );
    }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error reading time series from RCC ACIS web service (" + e + ").";
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
private void setDiscoveryTSList ( List<TS> discovery_TS_Vector ) {
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
		"SiteID"
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
	// Put together the final list of parameters.
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