package rti.tscommandprocessor.commands.riversidedb;

import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.DMI.RiversideDB_DMI.RiversideDBDataStore;
import RTi.DMI.RiversideDB_DMI.RiversideDB_DMI;
import RTi.DMI.RiversideDB_DMI.RiversideDB_MeasTypeMeasLocGeoloc;
import RTi.DMI.RiversideDB_DMI.RiversideDB_MeasTypeMeasLocGeoloc_InputFilter_JPanel;
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
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ReadRiversideDB() command.
*/
public class ReadRiversideDB_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
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
Constructor.
*/
public ReadRiversideDB_Command ()
{	super();
	setCommandName ( "ReadRiversideDB" );
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
    List<String> valid_Vector = new Vector();
    valid_Vector.add ( "DataStore" );
    valid_Vector.add ( "DataType" );
    valid_Vector.add ( "Interval" );
    for ( int i = 1; i <= __numFilterGroups; i++ ) { 
        valid_Vector.add ( "Where" + i );
    }
    valid_Vector.add ( "InputStart" );
    valid_Vector.add ( "InputEnd" );
    valid_Vector.add ( "Alias" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

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
*/
public List getObjectList ( Class c )
{
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Also check the base class
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
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
	return (new ReadRiversideDB_JDialog ( parent, this )).ok();
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
{	String routine = getClass().getName() + ".runCommand", message;
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
    // Data type may be XXXXX - Description but query with the first part
    String dataTypeShort = DataType;
    int pos = DataType.indexOf(" -");
    if ( pos > 0 ) {
        dataTypeShort = DataType.substring(0,pos).trim();
    }
    String Interval = parameters.getValue("Interval");
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

	List<TS> tslist = new Vector();
	try {
        // Read 1+ time series...
		// Get the input needed to process the file...
		//String InputName = parameters.getValue ( "InputName" );
		//if ( InputName == null ) {
		//	InputName = "";
		//}
		List<String> WhereN_Vector = new Vector ( 6 );
		String WhereN;
		int nfg = 0;	// Used below.
		for ( nfg = 0; nfg < 100; nfg++ ) {
			WhereN = parameters.getValue ( "Where" + (nfg + 1) );
			if ( WhereN == null ) {
				break;	// No more where clauses
			}
			WhereN_Vector.add ( WhereN );
		}
	
		// Find the data store to use...
		DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
		    DataStore, RiversideDBDataStore.class );
		if ( dataStore == null ) {
			message = "Could not get data store for name \"" + DataStore + "\" to query data.";
			Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a RiversideDB database connection has been opened with name \"" +
                    DataStore + "\"." ) );
			throw new Exception ( message );
		}
		RiversideDB_DMI dmi = (RiversideDB_DMI)((RiversideDBDataStore)dataStore).getDMI();

		// Initialize an input filter based on the data type...

		RiversideDB_MeasTypeMeasLocGeoloc_InputFilter_JPanel filterPanel =
		    new RiversideDB_MeasTypeMeasLocGeoloc_InputFilter_JPanel(
		        (RiversideDBDataStore)dataStore, getNumFilterGroups());

		// Populate with the where information from the command...

		String filterDelim = ";";
		for ( int ifg = 0; ifg < nfg; ifg ++ ) {
			WhereN = WhereN_Vector.get(ifg);
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
		
		// Read the list of objects from which identifiers can be obtained.  This code is similar to that in
		// TSTool_JFrame.readRiversideDBHeaders...
	
		Message.printStatus ( 2, routine, "Getting the list of time series..." );
		
		List<RiversideDB_MeasTypeMeasLocGeoloc> tsMetadataList = null;
        // Data type is shown with name so only use the first part of the choice
        try {
            tsMetadataList = dmi.readMeasTypeMeasLocGeolocList ( dataTypeShort, Interval, filterPanel );
        }
        catch ( Exception e ) {
            message = "Error getting list of time series matching input criteria DataType=\"" + dataTypeShort +
                "\" Interval=\"" + Interval + "\" Where=\"" + filterPanel + "\" (" + e + ").";
            Message.printWarning ( 2, routine,message);
            Message.printWarning ( 3, routine, e );
            ++warning_count;
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "See the log file for more information." ) );
            tsMetadataList = null;
        }

		// Make sure that size is set...
		int size = 0;
		if ( tsMetadataList != null ) {
			size = tsMetadataList.size();
		}
	
   		if ( (tsMetadataList == null) || (size == 0) ) {
			Message.printStatus ( 2, routine, "No RiversideDB time series were found." );
			Message.printStatus ( 2, routine, dmi.getLastSQLString() );
	        // Warn if nothing was retrieved (can be overridden to ignore).
            message = "No time series were read from the RiversideDB database.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database." +
                    	"  Previous messages may provide more information." ) );
   		}
   		else {
			// Else, convert each header object to a TSID string and read the time series...

			Message.printStatus ( 2, "", "Reading " + size + " time series metadata..." );

			String tsidentString = null;
			TS ts = null; // Time series to read.
			RiversideDB_MeasTypeMeasLocGeoloc meta = null;
			for ( int i = 0; i < size; i++ ) {
				meta = (RiversideDB_MeasTypeMeasLocGeoloc)tsMetadataList.get(i);
				tsidentString = meta.toTSIdent().toString(false) + "~" + DataStore;
				Message.printStatus ( 2, routine, "Reading time series for \"" + tsidentString + "\"..." );
				try {
				    ts = dmi.readTimeSeries ( tsidentString, InputStart_DateTime, InputEnd_DateTime,
				         null, // The requested data units
				         readData );
				    if ( (Alias != null) && !Alias.equals("") ) {
                        String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                            processor, ts, Alias, status, commandPhase);
                        ts.setAlias ( alias );
				    }
					// Add the time series to the temporary list.  It will be further processed below...
					tslist.add ( ts );
				}
				catch ( Exception e ) {
					message = "Unexpected error reading RiversideDB time series (" + e + ").";
					Message.printWarning ( 2, routine, message );
					Message.printWarning ( 2, routine, e );
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
        Message.printStatus ( 2, routine, "Read " + size + " RiversideDB time series." );

        if ( commandPhase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing RiversideDB time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                    // Don't throw an exception - probably due to missing data.
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding RiversideDB time series after read.";
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
            message = "No time series were read from the RiversideDB database.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database.  See previous messages." ) );
    }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error reading time series from RiversideDB database (" + e + ").";
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
private void setDiscoveryTSList ( List discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

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
    String DataType = props.getValue("DataType");
    if ( (DataType != null) && (DataType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataType=\"" + DataType + "\"" );
    }
	String Interval = props.getValue("Interval");
	if ( (Interval != null) && (Interval.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Interval=\"" + Interval + "\"" );
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