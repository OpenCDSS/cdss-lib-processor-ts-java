package rti.tscommandprocessor.commands.riversidedb;

import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import RTi.DMI.RiversideDB_DMI.RiversideDBDataStore;
import RTi.DMI.RiversideDB_DMI.RiversideDB_DMI;
import RTi.DMI.RiversideDB_DMI.RiversideDB_WriteMethodType;
import RTi.TS.TS;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.InvalidTimeIntervalException;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the WriteRiversideDB() command.
*/
public class WriteRiversideDB_Command extends AbstractCommand implements Command
{

/**
Values for WriteDataFlags
*/
protected String _False = "False";
protected String _True = "True";

/**
Constructor.
*/
public WriteRiversideDB_Command ()
{	super();
	setCommandName ( "WriteRiversideDB" );
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
{	String DataStore = parameters.getValue ( "DataStore" );
    String LocationID = parameters.getValue ( "LocationID" );
    String DataSource = parameters.getValue ( "DataSource" );
    String DataType = parameters.getValue ( "DataType" );
    //String DataSubType = parameters.getValue ( "DataSubType" );
    String Interval = parameters.getValue ( "Interval" );
    //String Scenario = parameters.getValue ( "Scenario" );
    String WriteDataFlags = parameters.getValue ( "WriteDataFlags" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	String WriteMethod = parameters.getValue ( "WriteMethod" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (DataStore == null) || DataStore.equals("") ) {
        message = "The data store must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store." ) );
    }

    if ( (LocationID == null) || LocationID.equals("") ) {
        message = "The location ID must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the location ID." ) );
    }

    if ( (DataSource == null) || DataSource.equals("") ) {
        message = "The data source must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data source." ) );
    }

    if ( (DataType == null) || DataType.equals("") ) {
        message = "The data type must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data type." ) );
    }
    
    // DataSubType may be blank
    
    if ( (Interval != null) && !Interval.equals("") ) {
        try {
            TimeInterval.parseInterval(Interval);
        }
        catch ( Exception e ) {
            message = "The data interval (" + Interval + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Select the interval from the command editor choices." ) );
        }
    }
    
    // Scenario OK to be blank
    // SequenceNumber OK to be blank
    
    if ( (WriteDataFlags != null) && !WriteDataFlags.equals("") ) {
        if ( !WriteDataFlags.equalsIgnoreCase(_False) && !WriteDataFlags.equalsIgnoreCase(_True) ) {
            message = "The WriteDataFlags parameter \"" + WriteDataFlags + "\" must be " + _False +
            " or " + _True + ".";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the parameter as " + _False + " or " + _True + "."));
        }
    }
    
	if ( (OutputStart != null) && !OutputStart.equals("")) {
		try {
		    DateTime datetime1 = DateTime.parse(OutputStart);
			if ( datetime1 == null ) {
				throw new Exception ("bad date");
			}
		}
		catch (Exception e) {
			message = "Output start date/time \"" + OutputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid output start date/time." ) );
		}
	}
	if ( (OutputEnd != null) && !OutputEnd.equals("")) {
		try {
		    DateTime datetime2 = DateTime.parse(OutputEnd);
			if ( datetime2 == null ) {
				throw new Exception ("bad date");
			}
		}
		catch (Exception e) {
			message = "Output end date/time \"" + OutputEnd + "\" is not a valid date/time.";
			warning += "\n" + message;
				status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Specify a valid output end date/time." ) );
		}
	}
	
    if ( (WriteMethod == null) || WriteMethod.equals("") ) {
        message = "The write method must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the parameter as " + RiversideDB_WriteMethodType.DELETE +
                "," + RiversideDB_WriteMethodType.DELETE_INSERT +
                ", or " + RiversideDB_WriteMethodType.TRACK_REVISIONS + ".") );
    }
    else {
        if ( !WriteMethod.equalsIgnoreCase("" + RiversideDB_WriteMethodType.DELETE) &&
            !WriteMethod.equalsIgnoreCase("" + RiversideDB_WriteMethodType.DELETE_INSERT) &&
            !WriteMethod.equalsIgnoreCase("" + RiversideDB_WriteMethodType.TRACK_REVISIONS))  {
            message = "The WriteMethod parameter \"" + WriteMethod + "\" must be " + RiversideDB_WriteMethodType.DELETE +
            "," + RiversideDB_WriteMethodType.DELETE_INSERT + ", or " + RiversideDB_WriteMethodType.TRACK_REVISIONS + ".";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the parameter as " + RiversideDB_WriteMethodType.DELETE +
                    "," + RiversideDB_WriteMethodType.DELETE_INSERT +
                    ", or " + RiversideDB_WriteMethodType.TRACK_REVISIONS + "."));
        }
    }
    
	// Check for invalid parameters...
	List<String> valid_Vector = new Vector();
	valid_Vector.add ( "DataStore" );
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "LocationID" );
    valid_Vector.add ( "DataSource" );
    valid_Vector.add ( "DataType" );
    valid_Vector.add ( "DataSubType" );
    valid_Vector.add ( "Interval" );
    valid_Vector.add ( "Scenario" );
    valid_Vector.add ( "SequenceNumber" );
    valid_Vector.add ( "WriteDataFlags" );
	valid_Vector.add ( "OutputStart" );
	valid_Vector.add ( "OutputEnd" );
	valid_Vector.add ( "WriteMethod" );
	valid_Vector.add ( "ProtectedFlags" );
	valid_Vector.add ( "RevisionDateTime" );
	valid_Vector.add ( "RevisionUser" );
	valid_Vector.add ( "RevisionComment" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new WriteRiversideDB_JDialog ( parent, this )).ok();
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "WriteRiversideDB_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);

	PropList parameters = getCommandParameters();
    String DataStore = parameters.getValue("DataStore");
	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String LocationID = parameters.getValue ( "LocationID" );
    String DataSource = parameters.getValue ( "DataSource" );
    String DataType = parameters.getValue ( "DataType" );
    String DataSubType = parameters.getValue ( "DataSubType" );
    String Interval = parameters.getValue ( "Interval" );
    TimeInterval interval = null;
    try {
        interval = TimeInterval.parseInterval(Interval);
    }
    catch ( InvalidTimeIntervalException e ) {
        // Will have been caught in checkCommandParameters()
    }
    String Scenario = parameters.getValue ( "Scenario" );
    String SequenceNumber = parameters.getValue ( "SequenceNumber" );
    String WriteDataFlags = parameters.getValue ( "WriteDataFlags" );
    boolean writeDataFlags = true;
    if ( (WriteDataFlags != null) && WriteDataFlags.equalsIgnoreCase(_False) ) {
        writeDataFlags = false;
    }
    String WriteMethod = parameters.getValue ( "WriteMethod" );
    RiversideDB_WriteMethodType writeMethod = RiversideDB_WriteMethodType.valueOfIgnoreCase(WriteMethod);
    if ( WriteMethod == null ) {
        writeMethod = null; // Default
    }
    String ProtectedFlags = parameters.getValue ( "ProtectedFlags" );

	// Get the time series to process...
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
		Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report problem to software support." ) );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Report problem to software support." ) );
	}
	List<TS> tslist = (List <TS>)o_TSList;
	if ( (tslist.size() == 0) && (writeMethod != RiversideDB_WriteMethodType.DELETE) ) {
        message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
		status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.WARNING,
						message, "Confirm that time series are available (may be OK for partial run)." ) );
	}

	String OutputStart = parameters.getValue ( "OutputStart" );
	DateTime OutputStart_DateTime = null;
	if ( OutputStart != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", OutputStart );
		try {
		    bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting DateTime(DateTime=" + OutputStart + ") from processor.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report problem to software support." ) );
		}
		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for DateTime(DateTime=" + OutputStart +
				"\") returned from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report problem to software support." ) );
		}
		else {
		    OutputStart_DateTime = (DateTime)prop_contents;
		}
	}
	else {
	    // Get from the processor (can be null)...
		try {
		    Object o_OutputStart = processor.getPropContents ( "OutputStart" );
			if ( o_OutputStart != null ) {
				OutputStart_DateTime = (DateTime)o_OutputStart;
			}
		}
		catch ( Exception e ) {
			message = "Error requesting OutputStart from processor - not using.";
			Message.printDebug(10, routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report problem to software support." ) );
		}
	}
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	DateTime OutputEnd_DateTime = null;
	if ( OutputEnd != null ) {
		request_params = new PropList ( "" );
		request_params.set ( "DateTime", OutputEnd );
		try {
		    bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			message = "Error requesting DateTime(DateTime=" + OutputEnd + ") from processor.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report problem to software support." ) );
		}
		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			message = "Null value for DateTime(DateTime=" + OutputEnd +
			"\") returned from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			status.addToLog ( CommandPhaseType.RUN,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Report problem to software support." ) );
		}
		else {
		    OutputEnd_DateTime = (DateTime)prop_contents;
		}
	}
	else {
	    // Get from the processor...
		try {
		    Object o_OutputEnd = processor.getPropContents ( "OutputEnd" );
			if ( o_OutputEnd != null ) {
				OutputEnd_DateTime = (DateTime)o_OutputEnd;
			}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting OutputEnd from processor - not using.";
			Message.printDebug(10, routine, message );
		}
	}

    // Write the time series

    try {
        // Find the data store to use...
        DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
            DataStore, RiversideDBDataStore.class );
        if ( dataStore == null ) {
            message = "Could not get data store for name \"" + DataStore + "\" to query data.";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a RiversideDB database connection has been opened with name \"" +
                    DataStore + "\"." ) );
            throw new RuntimeException ( message );
        }
        RiversideDB_DMI dmi = (RiversideDB_DMI)((RiversideDBDataStore)dataStore).getDMI();
        Message.printStatus ( 2, routine, "Writing RiversideDB time series to data store \"" +
            dataStore.getName() + "\"" );
        boolean writeSingle = true;
        boolean deleteOnly = false;
        if ( writeMethod == RiversideDB_WriteMethodType.DELETE ) {
            deleteOnly = true;
        }
        if ( writeSingle ) {
            // Only allow one time series to be written
            if ( (tslist.size() != 1) && !deleteOnly ) {
                message = "Only a single time series may be written - not writing time series to RiversideDB database.";
                Message.printWarning(3, routine, message);
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that a single time series is specified for writing." ) );
            }
            else {
                TS ts = null;
                if ( !deleteOnly ) {
                    ts = (TS)tslist.get(0);
                }
                dmi.writeTimeSeries ( ts, LocationID, DataSource, DataType, DataSubType, interval,
                    Scenario, SequenceNumber, writeDataFlags, OutputStart_DateTime, OutputEnd_DateTime, writeMethod,
                    ProtectedFlags );
            }
        }
        else {
            // Writing multiple time series
            //for ( ts : tsList ) {
                // Call the same method as above
            //}
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error writing time series to RiversideDB data store \"" +
            DataStore + "\" (" + e + ")";
        Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Check log file for details." ) );
        throw new CommandException ( message );
    }
	
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String DataStore = parameters.getValue ( "DataStore" );
    String TSList = parameters.getValue ( "TSList" );
    String TSID = parameters.getValue( "TSID" );
    String EnsembleID = parameters.getValue( "EnsembleID" );
    String LocationID = parameters.getValue( "LocationID" );
    String DataType = parameters.getValue( "DataType" );
    String DataSubType = parameters.getValue( "DataSubType" );
    String DataSource = parameters.getValue( "DataSource" );
    String Interval = parameters.getValue( "Interval" );
    String Scenario = parameters.getValue( "Scenario" );
    String WriteDataFlags = parameters.getValue( "WriteDataFlags" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
    String WriteMethod = parameters.getValue ( "WriteMethod" );
    String ProtectedFlags = parameters.getValue ( "ProtectedFlags" );
    String RevisionDateTime = parameters.getValue ( "RevisionDateTime" );
    String RevisionUser = parameters.getValue ( "RevisionUser" );
    String RevisionComment = parameters.getValue ( "RevisionComment" );
	StringBuffer b = new StringBuffer ();
	if ( (DataStore != null) && (DataStore.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DataStore=\"" + DataStore + "\"" );
	}
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
    if ( (LocationID != null) && (LocationID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "LocationID=\"" + LocationID + "\"" );
    }
    if ( (DataSource != null) && (DataSource.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataSource=\"" + DataSource + "\"" );
    }
    if ( (DataType != null) && (DataType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataType=\"" + DataType + "\"" );
    }
    if ( (DataSubType != null) && (DataSubType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataSubType=\"" + DataSubType + "\"" );
    }
    if ( (Interval != null) && (Interval.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Interval=\"" + Interval + "\"" );
    }
    if ( (Scenario != null) && (Scenario.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Scenario=\"" + Scenario + "\"" );
    }
    if ( (WriteDataFlags != null) && (WriteDataFlags.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WriteDataFlags=" + WriteDataFlags );
    }
    if ( (OutputStart != null) && (OutputStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputStart=\"" + OutputStart + "\"" );
    }
    if ( (OutputEnd != null) && (OutputEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputEnd=\"" + OutputEnd + "\"" );
    }
    if ( (WriteMethod != null) && (WriteMethod.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WriteMethod=" + WriteMethod );
    }
    if ( (ProtectedFlags != null) && (ProtectedFlags.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ProtectedFlags=\"" + ProtectedFlags + "\"" );
    }
    if ( (RevisionDateTime != null) && (RevisionDateTime.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "RevisionDateTime=\"" + RevisionDateTime + "\"" );
    }
    if ( (RevisionUser != null) && (RevisionUser.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "RevisionUser=\"" + RevisionUser + "\"" );
    }
    if ( (RevisionComment != null) && (RevisionComment.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "RevisionComment=\"" + RevisionComment + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}