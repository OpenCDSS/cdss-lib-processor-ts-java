package rti.tscommandprocessor.commands.reclamationhdb;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;
import RTi.TS.TSEnsemble;

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
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the ReadReclamationHDB() command.
*/
public class ReadReclamationHDB_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Number of where clauses shown in the editor and available as parameters - enough to allow select on each
time series identifier part, including sub-parts.
*/
private int __numFilterGroups = 6;

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
TSEnsemble created in discovery mode (to provide the identifier for other commands).
*/
private TSEnsemble __tsensemble = null;

/**
Constructor.
*/
public ReadReclamationHDB_Command ()
{	super();
	setCommandName ( "ReadReclamationHDB" );
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
    String DataTypeCommonName = parameters.getValue ( "DataTypeCommonName" );
    String SiteDataTypeID = parameters.getValue ( "SiteDataTypeID" );
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
    // Data type must be specified with filters, but not if SDI or site common name is specified
    if ( ((DataType == null) || DataType.equals("")) &&
        ((DataTypeCommonName == null) || DataTypeCommonName.equals("")) &&
        ((SiteDataTypeID == null) || SiteDataTypeID.equals(""))) {
        message = "The data type must be specified with filters or specify the site data type ID.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data type with filters or specify the site data type ID." ) );
    }
    
    if ( ((DataType != null) && !DataType.equals("")) &&
        ((DataTypeCommonName != null) && !DataTypeCommonName.equals("")) ) {
        message = "The data type cannot be specified for both the filters and reading specific time series.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data type only for the filters or reading specific time series." ) );
    }
    
    if ( (Interval == null) || Interval.equals("") ) {
        message = "The data interval must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data interval." ) );
    }
    else {
        try {
            TimeInterval.parseInterval(Interval);
        }
        catch ( Exception e ) {
            message = "The data interval (" + Interval + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid data interval." ) );
        }
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
    List<String> validList = new ArrayList<String>(19+__numFilterGroups);
    validList.add ( "DataStore" );
    validList.add ( "Interval" );
    validList.add ( "DataType" );
    for ( int i = 1; i <= __numFilterGroups; i++ ) { 
        validList.add ( "Where" + i );
    }
    validList.add ( "SiteCommonName" );
    validList.add ( "DataTypeCommonName" );
    validList.add ( "SiteDataTypeID" );
    validList.add ( "ModelName" );
    validList.add ( "ModelRunName" );
    validList.add ( "HydrologicIndicator" );
    validList.add ( "ModelRunDate" );
    validList.add ( "ModelRunID" );
    validList.add ( "EnsembleName" );
    //valid_Vector.add ( "EnsembleTraceID" );
    validList.add ( "EnsembleModelName" );
    validList.add ( "EnsembleModelRunDate" );
    validList.add ( "EnsembleModelRunID" );
    validList.add ( "Properties" );
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
Return the ensemble that is read by this class when run in discovery mode.
*/
private TSEnsemble getDiscoveryEnsemble()
{
    return __tsensemble;
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
*/
public List getObjectList ( Class c )
{
	List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (discoveryTSList == null) || (discoveryTSList.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discoveryTSList.get(0);
    // Also check the base class
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discoveryTSList;
    }
    else if ( c == TSEnsemble.class ) {
        TSEnsemble ensemble = getDiscoveryEnsemble();
        if ( ensemble == null ) {
            return null;
        }
        else {
            List<TSEnsemble> v = new ArrayList<TSEnsemble>();
            v.add ( ensemble );
            return v;
        }
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
	return (new ReadReclamationHDB_JDialog ( parent, this )).ok();
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
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ReadReclamationHDB_Command.runCommand", message;
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
        setDiscoveryEnsemble ( null );
        readData = false;
    }
    
    String DataStore = parameters.getValue("DataStore");
    String DataType = parameters.getValue("DataType");
    String Interval = parameters.getValue("Interval");
    TimeInterval interval = null;
    interval = TimeInterval.parseInterval(Interval);
    String SiteCommonName = parameters.getValue("SiteCommonName");
    String DataTypeCommonName = parameters.getValue("DataTypeCommonName");
    String SiteDataTypeID = parameters.getValue("SiteDataTypeID");
    int siteDataTypeID = -1;
    if ( (SiteDataTypeID != null) && !SiteDataTypeID.equals("") ) {
        siteDataTypeID = Integer.parseInt(SiteDataTypeID);
    }
    String ModelName = parameters.getValue("ModelName");
    String ModelRunName = parameters.getValue("ModelRunName");
    String HydrologicIndicator = parameters.getValue("HydrologicIndicator");
    if ( HydrologicIndicator == null ) {
        HydrologicIndicator = "";
    }
    String ModelRunDate = parameters.getValue("ModelRunDate");
    DateTime modelRunDate = null;
    if ( (ModelRunDate != null) && !ModelRunDate.equals("") ) {
        modelRunDate = DateTime.parse(ModelRunDate);
    }
    String ModelRunID = parameters.getValue("ModelRunID");
    int modelRunID = -1;
    if ( (ModelRunID != null) && !ModelRunID.equals("") ) {
        modelRunID = Integer.parseInt(ModelRunID);
    }
    String EnsembleName = parameters.getValue("EnsembleName");
    String EnsembleModelName = parameters.getValue("EnsembleModelName");
    String EnsembleModelRunID = parameters.getValue("EnsembleModelRunID");
    int ensembleModelRunID = -1;
    if ( (EnsembleModelRunID != null) && !EnsembleModelRunID.equals("") ) {
        ensembleModelRunID = Integer.parseInt(EnsembleModelRunID);
    }
    String Alias = parameters.getValue("Alias");
    String Properties = parameters.getValue ( "Properties" );
    Hashtable properties = null;
    if ( (Properties != null) && (Properties.length() > 0) && (Properties.indexOf(":") > 0) ) {
        properties = new Hashtable();
        // First break map pairs by comma
        List<String> pairs = new ArrayList<String>();
        if ( Properties.indexOf(",") > 0 ) {
            pairs = StringUtil.breakStringList(Properties, ",", 0 );
        }
        else {
            pairs.add(Properties);
        }
        // Now break pairs and put in hashtable
        for ( String pair : pairs ) {
            String [] parts = pair.split(":");
            properties.put(parts[0].trim(), parts[1].trim() );
        }
    }    
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

	// Now try to read based on the combination of parameters...

	List<TS> tslist = new ArrayList<TS>();	// Time series results.
					// Will be added to for one time series
					// read or replaced if a list is read.
	TSEnsemble ensemble = null;
	try {
	    // Find the data store to use...
        DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
            DataStore, ReclamationHDBDataStore.class );
        if ( dataStore == null ) {
            message = "Could not get data store for name \"" + DataStore + "\" to query data.";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a ReclamationHDB database connection has been opened with name \"" +
                    DataStore + "\"." ) );
            throw new Exception ( message );
        }
        ReclamationHDB_DMI dmi = (ReclamationHDB_DMI)((ReclamationHDBDataStore)dataStore).getDMI();

	    if ( (DataType != null) && !DataType.equals("") ) {
	        Message.printStatus(2,routine,"Reading time series using input filters.");
            // Input filter parameters have been specified so read 1+ time series...
    		// Get the input needed to process the file...
    		//String InputName = parameters.getValue ( "InputName" );
    		//if ( InputName == null ) {
    		//	InputName = "";
    		//}
    		List<String> whereNList = new ArrayList<String> ( 6 );
    		String WhereN;
    		int nfg = 0;	// Used below.
    		for ( nfg = 0; nfg < 100; nfg++ ) {
    			WhereN = parameters.getValue ( "Where" + (nfg + 1) );
    			if ( WhereN == null ) {
    				break;	// No more where clauses
    			}
    			whereNList.add ( WhereN );
    		}
    	
    		// Initialize an input filter based on the data type...
    
    		ReclamationHDB_TimeSeries_InputFilter_JPanel filterPanel =
    		    new ReclamationHDB_TimeSeries_InputFilter_JPanel((ReclamationHDBDataStore)dataStore, getNumFilterGroups());
    
    		// Populate with the where information from the command...
    
    		String filterDelim = ";";
    		for ( int ifg = 0; ifg < nfg; ifg ++ ) {
    			WhereN = whereNList.get(ifg);
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
    		
    		// Extract the 
    
    		// Read the list of objects from which identifiers can be obtained.  This code is similar to that in
    		// TSTool_JFrame.readHydroBaseHeaders...
    	
    		Message.printStatus ( 2, routine, "Getting the list of time series..." );
    	
    		List<ReclamationHDB_SiteTimeSeriesMetadata> tsMetadataList = null;
    
    		// The data type in the command is "ObjectType - DataCommonName", which is OK for the following call
            tsMetadataList = dmi.readSiteTimeSeriesMetadataList(DataType, Interval, filterPanel );
    		// Make sure that size is set...
    		int size = 0;
    		if ( tsMetadataList != null ) {
    			size = tsMetadataList.size();
    		}
    	
       		if ( (tsMetadataList == null) || (size == 0) ) {
    			Message.printStatus ( 2, routine,"No Reclamation HDB time series were found." );
    	        // Warn if nothing was retrieved (can be overridden to ignore).
                message = "No time series were read from the Reclamation HDB database.";
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
    			ReclamationHDB_SiteTimeSeriesMetadata meta = null;
    			for ( int i = 0; i < size; i++ ) {
    				meta = (ReclamationHDB_SiteTimeSeriesMetadata)tsMetadataList.get(i);
    				tsidentString = meta.getTSID() + "~" + DataStore;
    	
    				Message.printStatus ( 2, routine, "Reading time series for \"" + tsidentString + "\"..." );
    				try {
    				    ts = dmi.readTimeSeries ( tsidentString, InputStart_DateTime, InputEnd_DateTime, readData );
    					// Add the time series to the temporary list.  It will be further processed below...
    					tslist.add ( ts );
    				}
    				catch ( Exception e ) {
    					message = "Unexpected error reading Reclamation HDB time series (" + e + ").";
    					Message.printWarning ( 2, routine, message );
    					Message.printWarning ( 2, routine, e );
    					++warning_count;
                        status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                               message, "Report the problem to software support - also see the log file." ) );
    				}
    			}
    		}
	    }
        else if ( (siteDataTypeID >= 0) && (ensembleModelRunID >= 0) ) {
            // Reading a single trace from an ensemble - put before reading full ensemble
            Message.printStatus(2,routine,"Reading 1 time series ensemble trace using SDI=" + siteDataTypeID +
                ", ensemble MRI=" + ensembleModelRunID + ", interval=" + interval );
            try {
                TS ts = dmi.readTimeSeries ( siteDataTypeID, ensembleModelRunID, true, interval,
                    InputStart_DateTime, InputEnd_DateTime, readData );
                // Add the time series to the temporary list.  It will be further processed below...
                tslist.add ( ts );
            }
            catch ( Exception e ) {
                message = "Unexpected error reading Reclamation HDB time series for SDI=" + siteDataTypeID + " MRI=" +
                   ensembleModelRunID + " (" + e + ").";
                Message.printWarning ( 2, routine, message );
                Message.printWarning ( 2, routine, e );
                ++warning_count;
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report the problem to software support - also see the log file." ) );
            }
        }
	    else if ( (siteDataTypeID >= 0) && (EnsembleName != null) && !EnsembleName.equals("") ) {
            // Reading an ensemble of model time series
	        Message.printStatus(2,routine,"Reading time series ensemble for SDI=" + siteDataTypeID + ", ensemble name=\"" +
	            EnsembleName + "\", interval=" + interval + ".");
	        ensemble = dmi.readEnsemble ( siteDataTypeID, EnsembleName, interval, InputStart_DateTime, InputEnd_DateTime, readData );
            if ( ensemble != null ) {
                tslist = ensemble.getTimeSeriesList (false);
                int tscount = 0;
                if ( tslist != null ) {
                    tscount = tslist.size();
                    message = "Read \"" + tscount + "\" ensemble time series from Reclamation HDB";
                    Message.printStatus ( 2, routine, message );
                }
            }
            // Time series are post-processed below, but add ensemble here
            TSCommandProcessorUtil.appendEnsembleToResultsEnsembleList(processor, this, ensemble);
        }
        else if ( (siteDataTypeID >= 0) && (modelRunID >= 0) ) {
            // Reading a single model time series
            Message.printStatus(2,routine,"Reading 1 model time series for SDI=" + siteDataTypeID + ", MRI=" + modelRunID +
                ", interval=" + interval );
            try {
                TS ts = dmi.readTimeSeries ( siteDataTypeID, modelRunID, false, interval,
                    InputStart_DateTime, InputEnd_DateTime, readData );
                // Add the time series to the temporary list.  It will be further processed below...
                tslist.add ( ts );
            }
            catch ( Exception e ) {
                message = "Unexpected error reading Reclamation HDB time series for SDI=" + siteDataTypeID + " MRI=" +
                   modelRunID + " (" + e + ").";
                Message.printWarning ( 2, routine, message );
                Message.printWarning ( 2, routine, e );
                ++warning_count;
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report the problem to software support - also see the log file." ) );
            }
        }
        else if ( (siteDataTypeID >= 0) && (modelRunID < 0) && (ModelName == null) || (ModelName.equals(""))) {
            // Reading a single real time series
            Message.printStatus(2,routine,"Reading 1 real time series for SDI=" + siteDataTypeID + ", interval=" + interval );
            try {
                TS ts = dmi.readTimeSeries ( siteDataTypeID, modelRunID, false, interval,
                    InputStart_DateTime, InputEnd_DateTime, readData );
                // Add the time series to the temporary list.  It will be further processed below...
                tslist.add ( ts );
            }
            catch ( Exception e ) {
                message = "Unexpected error reading Reclamation HDB time series for SDI=" + siteDataTypeID + " (" + e + ").";
                Message.printWarning ( 2, routine, message );
                Message.printWarning ( 2, routine, e );
                ++warning_count;
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report the problem to software support - also see the log file." ) );
            }
        }
	    else if ( siteDataTypeID < 0 ) {
	        // Did not specify a site datatype ID so using site common name is not unique..
	        // Legacy functionality - unique string values are not guaranteed in the database
            message = "Reading time series using old-style TSID that relies on site common name \"" + SiteCommonName +
                "\" is unreliable due to non-unique names.";
             Message.printWarning ( 2, routine, message );
             ++warning_count;
             status.addToLog ( commandPhase,
                 new CommandLogRecord(CommandStatusType.FAILURE,
                     message, "Specify the site datatype ID (SDI) to uniquely identify the time series." ) );
            String tsidentString = null;
            if ( (ModelName != null) && !ModelName.equals("") ) {
                // Single model time series
                tsidentString = "Model:" + SiteCommonName + ".HDB." + DataTypeCommonName + "." + Interval + "." +
                    ModelName + "-" + ModelRunName + "-" + HydrologicIndicator + "-" + ModelRunDate + "~" + DataStore;
            }
            else {
                // Simple real time series
                tsidentString = "Real:" + SiteCommonName + ".HDB." + DataTypeCommonName + "." + Interval +
                    "~" + DataStore;
            }
            try {
                TS ts = dmi.readTimeSeries ( tsidentString, InputStart_DateTime, InputEnd_DateTime, readData );
                // Add the time series to the temporary list.  It will be further processed below...
                tslist.add ( ts );
            }
            catch ( Exception e ) {
                message = "Unexpected error reading Reclamation HDB time series (" + e + ").";
                Message.printWarning ( 2, routine, message );
                Message.printWarning ( 2, routine, e );
                ++warning_count;
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report the problem to software support - also see the log file." ) );
            }
	    }
	    else if ( (siteDataTypeID >= 0) && (ModelName != null) && !ModelName.equals("") ) {
	        // Reading a single time series, either real or model using names for 
	        Message.printStatus(2,routine,"Reading 1 model time series using SDI=" + siteDataTypeID + ", model name=\"" +
	            ModelName + "\".");
            // Need to use the SDI to get metadata because SiteCommonName is not unique
            // Model run ID was not specified (otherwise would have read above).  If the model name is specified,
            // use it to get the model run ID and then use with the SDI to read
            boolean okToRead = true;
            // Get the model corresponding to the model name
            List<ReclamationHDB_Model> models = dmi.readHdbModelList(ModelName);
            int modelID = -1;
            if ( models.size() != 1 ) {
                 message = "Reading model data for ModelName=\"" + ModelName + "\" have " + models.size() + " models.  Expecting exactly 1.  Cannot read time series.";
                 Message.printWarning ( 3, routine, message );
                 ++warning_count;
                 status.addToLog ( commandPhase,
                     new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Check input to verify that parameters match one model." ) );
                 okToRead = false;
            }
            else {
                modelID = models.get(0).getModelID();
            }
            if ( okToRead ) {
                // Get the model runs corresponding to command parameters
                List<ReclamationHDB_ModelRun> runs = dmi.readHdbModelRunList(modelID,null,ModelRunName,HydrologicIndicator,modelRunDate);
                if ( runs.size() != 1 ) {
                    message = "Reading model run data for ModelID=\"" + modelID + "\" ModelRunName=\"" +
                        ModelRunName + "\" HydrologicIndicator=\"" + HydrologicIndicator + "\" RunDate=\"" +
                        ModelRunDate + "\" have " + runs.size() + " runs.  Expecting exactly 1.  Cannot read time series.";
                     Message.printWarning ( 3, routine, message );
                     ++warning_count;
                     status.addToLog ( commandPhase,
                         new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Check input to verify that parameters match one model run." ) );
                     okToRead = false;
                }
                else {
                    modelRunID = runs.get(0).getModelRunID();
                }
            }
            if ( okToRead ) {
	            try {
	                TS ts = dmi.readTimeSeries ( siteDataTypeID, modelRunID, false, interval,
	                    InputStart_DateTime, InputEnd_DateTime, readData );
	                // Add the time series to the temporary list.  It will be further processed below...
	                tslist.add ( ts );
	            }
	            catch ( Exception e ) {
	                message = "Unexpected error reading Reclamation HDB time series for SDI=" + siteDataTypeID + " MRI=" +
	                   modelRunID + " (" + e + ").";
	                Message.printWarning ( 2, routine, message );
	                Message.printWarning ( 2, routine, e );
	                ++warning_count;
	                status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                       message, "Report the problem to software support - also see the log file." ) );
	            }
            }
	    }
	    else {
	        message = "Do not know how to read time series for combination of command parameters.";
             Message.printWarning ( 2, routine, message );
             ++warning_count;
             status.addToLog ( commandPhase,
                 new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support - also see the log file." ) );
	    }
    
        int nts = 0;
        if ( tslist != null ) {
            nts = tslist.size();
        }
        Message.printStatus ( 2, routine, "Read " + nts + " Reclamation HDB time series." );
        
        if ( (Alias != null) && !Alias.equals("") && (tslist != null) ) {
            for ( TS ts: tslist ) {
                // Set the alias to the desired string - this is impacted by the Location parameter
                String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                    processor, ts, Alias, status, commandPhase);
                ts.setAlias ( alias );
            }
        }

        if ( commandPhase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                for ( TS ts: tslist ) {
                    if ( properties != null ) {
                        // Assign properties
                        Enumeration keys = properties.keys();
                        String key = null;
                        while ( keys.hasMoreElements() ) {
                            key = (String)keys.nextElement();
                            ts.setProperty( key, TSCommandProcessorUtil.expandTimeSeriesMetadataString (
                                processor, ts, (String)properties.get(key), status, CommandPhaseType.RUN) );
                        }
                    }
                }
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing Reclamation HDB time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                    // Don't throw an exception - probably due to missing data.
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding Reclamation HDB time series after read.";
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
            // Just want the identifier...
            ensemble = new TSEnsemble ( EnsembleName, EnsembleName, null );
            setDiscoveryEnsemble ( ensemble );
        }
        // Warn if nothing was retrieved (can be overridden to ignore).
        if ( (tslist == null) || (nts == 0) ) {
            message = "No time series were read from the Reclamation HDB database.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database.  See previous messages." ) );
    }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error reading time series from Reclamation HDB database (" + e + ").";
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
Set the ensemble that is processed by this class in discovery mode.
*/
private void setDiscoveryEnsemble ( TSEnsemble tsensemble )
{
    __tsensemble = tsensemble;
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

// FIXME SAM 2010-10-20 Enable correct properties
/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	StringBuffer b = new StringBuffer ();
	if ( props == null ) {
	    return getCommandName() + "()";
	}
    String DataStore = props.getValue("DataStore");
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
	String Interval = props.getValue("Interval");
	if ( (Interval != null) && (Interval.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Interval=\"" + Interval + "\"" );
	}
    String DataType = props.getValue("DataType");
    if ( (DataType != null) && (DataType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataType=\"" + DataType + "\"" );
    }
	String delim = ";";
    for ( int i = 1; i <= __numFilterGroups; i++ ) {
    	String where = props.getValue("Where" + i);
    	if ( (where != null) && (where.length() > 0) && !where.startsWith(delim) ) {
    		if ( b.length() > 0 ) {
    			b.append ( "," );
    		}
    		b.append ( "Where" + i + "=\"" + where + "\"" );
    	}
    }
    String SiteCommonName = props.getValue( "SiteCommonName" );
    if ( (SiteCommonName != null) && (SiteCommonName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SiteCommonName=\"" + SiteCommonName + "\"" );
    }
    String DataTypeCommonName = props.getValue( "DataTypeCommonName" );
    if ( (DataTypeCommonName != null) && (DataTypeCommonName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataTypeCommonName=\"" + DataTypeCommonName + "\"" );
    }
    String SiteDataTypeID = props.getValue( "SiteDataTypeID" );
    if ( (SiteDataTypeID != null) && (SiteDataTypeID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SiteDataTypeID=" + SiteDataTypeID );
    }
    String ModelName = props.getValue( "ModelName" );
    if ( (ModelName != null) && (ModelName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ModelName=\"" + ModelName + "\"" );
    }
    String ModelRunName = props.getValue( "ModelRunName" );
    if ( (ModelRunName != null) && (ModelRunName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ModelRunName=\"" + ModelRunName + "\"" );
    }
    String ModelRunDate = props.getValue( "ModelRunDate" );
    if ( (ModelRunDate != null) && (ModelRunDate.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ModelRunDate=\"" + ModelRunDate + "\"" );
    }
    String HydrologicIndicator = props.getValue( "HydrologicIndicator" );
    if ( (HydrologicIndicator != null) && (HydrologicIndicator.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "HydrologicIndicator=\"" + HydrologicIndicator + "\"" );
    }
    String ModelRunID = props.getValue( "ModelRunID" );
    if ( (ModelRunID != null) && (ModelRunID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ModelRunID=" + ModelRunID );
    }
    String EnsembleName = props.getValue( "EnsembleName" );
    if ( (EnsembleName != null) && (EnsembleName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleName=\"" + EnsembleName + "\"" );
    }
    /*
    String EnsembleTraceID = props.getValue( "EnsembleTraceID" );
    if ( (EnsembleTraceID != null) && (EnsembleTraceID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleTraceID=\"" + EnsembleTraceID + "\"" );
    }
    */
    String EnsembleModelName = props.getValue( "EnsembleModelName" );
    if ( (EnsembleModelName != null) && (EnsembleModelName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleModelName=\"" + EnsembleModelName + "\"" );
    }
    String EnsembleModelRunDate = props.getValue( "EnsembleModelRunDate" );
    if ( (EnsembleModelRunDate != null) && (EnsembleModelRunDate.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleModelRunDate=\"" + EnsembleModelRunDate + "\"" );
    }
    String EnsembleModelRunID = props.getValue( "EnsembleModelRunID" );
    if ( (EnsembleModelRunID != null) && (EnsembleModelRunID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleModelRunID=\"" + EnsembleModelRunID + "\"" );
    }
    String Properties = props.getValue("Properties");
    if ( (Properties != null) && (Properties.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Properties=\"" + Properties + "\"" );
    }
	String InputStart = props.getValue("InputStart");
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputStart=\"" + InputStart + "\"" );
	}
	String InputEnd = props.getValue("InputEnd");
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputEnd=\"" + InputEnd + "\"" );
	}
    String Alias = props.getValue("Alias");
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }

    return getCommandName() + "(" + b.toString() + ")";
}

}