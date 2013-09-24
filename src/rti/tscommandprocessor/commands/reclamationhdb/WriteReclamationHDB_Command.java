package rti.tscommandprocessor.commands.reclamationhdb;

import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

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
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

/**
This class initializes, checks, and runs the WriteReclamationHDB() command.
*/
public class WriteReclamationHDB_Command extends AbstractCommand implements Command
{

/**
Constructor.
*/
public WriteReclamationHDB_Command ()
{	super();
	setCommandName ( "WriteReclamationHDB" );
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
{	String TSList = parameters.getValue ( "TSList" );
    String DataStore = parameters.getValue ( "DataStore" );
    String SiteCommonName = parameters.getValue ( "SiteCommonName" );
    String DataTypeCommonName = parameters.getValue ( "DataTypeCommonName" );
    String SiteDataTypeID = parameters.getValue ( "SiteDataTypeID" );
    String ModelName = parameters.getValue ( "ModelName" );
    String ModelRunName = parameters.getValue ( "ModelRunName" );
    String HydrologicIndicator = parameters.getValue ( "HydrologicIndicator" );
    String ModelRunDate = parameters.getValue ( "ModelRunDate" );
    String ModelRunID = parameters.getValue ( "ModelRunID" );
    String EnsembleName = parameters.getValue ( "EnsembleName" );
    String EnsembleModelName = parameters.getValue ( "EnsembleModelName" );
    String EnsembleTraceID = parameters.getValue ( "EnsembleTraceID" );
    String EnsembleModelRunDate = parameters.getValue ( "EnsembleModelRunDate" );
    String ValidationFlag = parameters.getValue ( "ValidationFlag" );
    String DataFlags = parameters.getValue ( "DataFlags" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	// TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
	//String IntervalOverride = parameters.getValue ( "IntervalOverride" );
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
    
    // SiteCommonName or SiteDatatypeID is required.

    if ( ((SiteCommonName == null) || SiteCommonName.equals("")) &&
        ((SiteDataTypeID == null) || SiteDataTypeID.equals("")) ) {
        message = "The SiteCommonName or SiteDataTypeID must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the SiteCommonName or SiteDataTypeID." ) );
    }
    /* Now optional 
    if ( (SiteCommonName == null) || SiteCommonName.equals("") ) {
        message = "The site common name must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the site common name." ) );
    }
    
    if ( (DataTypeCommonName == null) || DataTypeCommonName.equals("") ) {
        message = "The data type common name must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data type common name." ) );
    }
    */
    
    /* Now optional
    if ( (ModelName == null) || ModelName.equals("") ) {
        message = "The model name must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the model name." ) );
    }
    
    if ( (ModelRunName == null) || ModelRunName.equals("") ) {
        message = "The model run name must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the model run name." ) );
    }
    
    if ( (HydrologicIndicator == null) || HydrologicIndicator.equals("") ) {
        message = "The hydrologic indicator must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the hydrologic indicator." ) );
    }
    */
    
    if ( (ModelRunDate != null) && !ModelRunDate.equals("") ) {
        // If the date/time has a trailing .0, remove because the parse code does not handle the hundredths
        // TODO SAM 2013-04-07 Check the precision after parsing to make sure to minute
        int pos = ModelRunDate.indexOf(".0");
        DateTime dt = null;
        if ( pos > 0 ) {
            ModelRunDate = ModelRunDate.substring(0,pos);
        }
        try { 
            dt = DateTime.parse(ModelRunDate);
        }
        catch ( Exception e ) {
            message = "The model run date is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the model run date as YYYY-MM-DD hh:mm." ) );
        }
        if ( dt != null ) {
            if ( dt.getPrecision() != DateTime.PRECISION_MINUTE ) {
                message = "The model run date must be specified to minute precision.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the model run date as YYYY-MM-DD hh:mm." ) );
            }
        }
    }
    
    // Only allow a single time series to be written, or an ensemble
    // TODO SAM 2013-04-07 might allow wildcards in parameters later
    if ( (EnsembleName != null) && !EnsembleName.equals("") ) {
        // Must specify time series with ensemble ID for consistency
        if ( !TSList.equalsIgnoreCase(""+TSListType.ENSEMBLE_ID) ) {
            message = "Specifying an ensemble name requires using TSList=" + TSListType.ENSEMBLE_ID;
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an ensemble ID to write." ) );
        }
        // Only allow either a single time series or ensemble to be written
        if ( (ModelName != null) && !ModelName.equals("")) {
            message = "Both a single model name and ensemble name have been specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a single model name OR ensemble name." ) );
        }
        // Ensemble model name must be specified
        if ( (EnsembleModelName == null) || EnsembleModelName.equals("")) {
            message = "The model name for hte ensemble must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the model name used with the ensemble time series." ) );
        }
    }
    
    if ( (EnsembleModelRunDate != null) && !EnsembleModelRunDate.equals("") ) {
        // If the date/time has a trailing .0, remove because the parse code does not handle the hundredths
        // TODO SAM 2013-04-07 Check the precision after parsing to make sure to minute
        int pos = EnsembleModelRunDate.indexOf(".0");
        DateTime dt = null;
        if ( pos > 0 ) {
            EnsembleModelRunDate = EnsembleModelRunDate.substring(0,pos);
        }
        try { 
            dt = DateTime.parse(EnsembleModelRunDate);
        }
        catch ( Exception e ) {
            message = "The ensemble model run date is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the ensemble model run date as YYYY-MM-DD hh:mm." ) );
        }
        if ( dt != null ) {
            if ( dt.getPrecision() != DateTime.PRECISION_MINUTE ) {
                message = "The ensemble model run date must be specified to minute precision.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the ensemble model run date as YYYY-MM-DD hh:mm." ) );
            }
        }
    }
    
    if ( ValidationFlag != null ) {
        ValidationFlag = ValidationFlag.trim();
        if ( (ValidationFlag.length() != 0) && (!Character.isLetter(ValidationFlag.charAt(0)) ||
            !Character.isUpperCase(ValidationFlag.charAt(0))) ) {
            message = "The validation flag (" + ValidationFlag + ") is not an upper-case letter.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the validation flag as an uppercase letter." ) );
        }
    }
    
    if ( (DataFlags != null) && DataFlags.trim().length() > 20 ) {
        message = "The data flags (" + DataFlags + ") must be <= 20 characters.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data flags as <= 20 characters." ) );
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
	// TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
	/*
	if ( (IntervalOverride != null) && IntervalOverride.equals("") ) {
        try {
            TimeInterval.parseInterval(IntervalOverride);
        }
        catch ( Exception e ) {
            // Should not happen because choices are valid
            message = "The interval override \"" + IntervalOverride + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Specify an interval using the command editor."));
        }
	}
	*/
	// Check for invalid parameters...
	List<String> valid_Vector = new Vector<String>();
	valid_Vector.add ( "DataStore" );
    valid_Vector.add ( "TSList" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "EnsembleID" );
    valid_Vector.add ( "SiteCommonName" );
    valid_Vector.add ( "DataTypeCommonName" );
    valid_Vector.add ( "SiteDataTypeID" );
    valid_Vector.add ( "ModelName" );
    valid_Vector.add ( "ModelRunName" );
    valid_Vector.add ( "HydrologicIndicator" );
    valid_Vector.add ( "ModelRunDate" );
    valid_Vector.add ( "ModelRunID" );
    valid_Vector.add ( "EnsembleName" );
    valid_Vector.add ( "EnsembleTraceID" );
    valid_Vector.add ( "EnsembleModelName" );
    valid_Vector.add ( "EnsembleModelRunDate" );
    valid_Vector.add ( "Agency" );
    valid_Vector.add ( "ValidationFlag" );
    valid_Vector.add ( "OverwriteFlag" );
    valid_Vector.add ( "DataFlags" );
    valid_Vector.add ( "TimeZone" );
	valid_Vector.add ( "OutputStart" );
	valid_Vector.add ( "OutputEnd" );
	// TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
	//valid_Vector.add ( "IntervalOverride" );
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
	return (new WriteReclamationHDB_JDialog ( parent, this )).ok();
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
{	String routine = "WriteReclamationHDB_Command.runCommand", message;
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
    String SiteCommonName = parameters.getValue ( "SiteCommonName" );
    String DataTypeCommonName = parameters.getValue ( "DataTypeCommonName" );
    String SiteDataTypeID = parameters.getValue ( "SiteDataTypeID" );
    Long siteDataTypeID = null;
    if ( StringUtil.isLong(SiteDataTypeID) ) {
        siteDataTypeID = Long.parseLong(SiteDataTypeID);
    }
    String ModelName = parameters.getValue ( "ModelName" );
    String ModelRunName = parameters.getValue ( "ModelRunName" );
    String ModelRunDate = parameters.getValue ( "ModelRunDate" );
    if ( ModelRunDate != null ) {
        // TODO SAM 2013-04-07 Strip off hundredths of second.  Should not be needed now because
        // run date should only go to minute.
        int pos = ModelRunDate.indexOf(".0");
        if ( pos > 0 ) {
            ModelRunDate = ModelRunDate.substring(0,pos);
        }
    }
    String HydrologicIndicator = parameters.getValue ( "HydrologicIndicator" );
    String ModelRunID = parameters.getValue ( "ModelRunID" );
    String EnsembleName = parameters.getValue ( "EnsembleName" );
    if ( (EnsembleName != null) && EnsembleName.equals("") ) {
        EnsembleName = null; // Simplifies logic below when ensemble is not used
    }
    String EnsembleModelName = parameters.getValue ( "EnsembleModelName" );
    String EnsembleTraceID = parameters.getValue ( "EnsembleTraceID" );
    if ( (EnsembleTraceID != null) && EnsembleTraceID.equals("") ) {
        EnsembleTraceID = null; // Simplifies logic below when default is used
    }
    String EnsembleModelRunDate = parameters.getValue ( "EnsembleModelRunDate" );
    if ( (EnsembleModelRunDate != null) && EnsembleModelRunDate.equals("") ) {
        EnsembleModelRunDate = null; // Simplifies logic below when default is used
    }
    DateTime ensembleModelRunDate = null;
    if ( EnsembleModelRunDate != null ) {
        ensembleModelRunDate = DateTime.parse(EnsembleModelRunDate);
    }
    Long modelRunID = null;
    if ( StringUtil.isLong(ModelRunID) ) {
        modelRunID = Long.parseLong(ModelRunID);
    }
    String Agency = parameters.getValue ( "Agency" );
    String ValidationFlag = parameters.getValue ( "ValidationFlag" );
    String OverwriteFlag = parameters.getValue ( "OverwriteFlag" );
    String DataFlags = parameters.getValue ( "DataFlags" );
    String TimeZone = parameters.getValue ( "TimeZone" );
    // TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
    /*
    String IntervalOverride = parameters.getValue ( "IntervalOverride" );
    TimeInterval intervalOverride = null;
    if ( (IntervalOverride != null) && !IntervalOverride.equals("") ) {
        try {
            intervalOverride = TimeInterval.parseInterval(IntervalOverride);
        }
        catch ( Exception e ) {
            intervalOverride = null;
        }
    }
    */

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
	if ( tslist.size() == 0 ) {
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
            DataStore, ReclamationHDBDataStore.class );
        if ( dataStore == null ) {
            message = "Could not get data store for name \"" + DataStore + "\" to write time series.";
            Message.printWarning ( 2, routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a ReclamationHDB database connection has been opened with name \"" +
                    DataStore + "\"." ) );
            throw new RuntimeException ( message );
        }
        ReclamationHDB_DMI dmi = (ReclamationHDB_DMI)((ReclamationHDBDataStore)dataStore).getDMI();
        Message.printStatus ( 2, routine, "Writing ReclamationHDB time series to data store \"" +
            dataStore.getName() + "\"" );
        String loadingApp = "TSTool";
        boolean doWrite = true;
        int traceNumber = 0;
        String traceNumberAsString;
        if ( doWrite && (tslist != null) ) {
            for ( TS ts : tslist ) {
                if ( EnsembleName != null ) {
                    // Writing an ensemble so get the model_run_id for the specific trace using the ensemble information
                    // First get the trace number based on the EnsembleTraceID parameter
                    if ( EnsembleTraceID == null ) {
                        // Use the time series sequence number and if that is not set generate an error
                        String sequenceID = ts.getSequenceID();
                        traceNumber = -1;
                        if ( StringUtil.isInteger(sequenceID) ) {
                            traceNumber = Integer.parseInt(sequenceID);
                        }
                        if ( traceNumber < 0 ) {
                            // No sequence number set so skip the time series
                            message = "Unable to determine trace number for \"" +
                                ts.getIdentifierString() + "\" for EnsembleTraceID=\"" + EnsembleTraceID +
                                "\" - not writing trace.";
                            Message.printWarning ( warning_level, 
                                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
                            status.addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Verify that trace number is available in the time series." ) );
                            continue;
                        }
                    }
                    else {
                        // Try to get the trace number by formatting the data specified by the parameter
                        traceNumberAsString = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                            processor, ts, EnsembleTraceID, status, CommandPhaseType.RUN);
                        try {
                            traceNumber = Integer.valueOf(traceNumberAsString);
                        }
                        catch ( NumberFormatException e ) {
                            message = "Unable to determine time series trace number for \"" +
                                ts.getIdentifierString() + "\" EnsembleTraceID=\"" + EnsembleTraceID +
                                "\" and expanded value \"" + traceNumberAsString + "\" - not writing trace.";
                            Message.printWarning ( warning_level, 
                                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
                            status.addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Verify that the trace number is available in the time series." ) );
                            continue;
                        }
                    }
                    modelRunID = dmi.readModelRunIDForEnsembleTrace ( EnsembleName, traceNumber,
                        EnsembleModelName, ensembleModelRunDate );
                    if ( modelRunID == null ) {
                        message = "Unable to determine HDB MRI number for time series \"" +
                            ts.getIdentifierString() + "\" EnsembleTraceID=\"" + EnsembleTraceID +
                            "\" and trace number " + traceNumber + " - not writing trace.";
                        Message.printWarning ( warning_level, 
                            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
                        status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Verify that the trace number is defined in the database." ) );
                        continue;
                    }
                }
                dmi.writeTimeSeries ( ts, loadingApp,
                    SiteCommonName, DataTypeCommonName, siteDataTypeID,
                    ModelName, ModelRunName, ModelRunDate, HydrologicIndicator, modelRunID,
                    Agency, ValidationFlag, OverwriteFlag, DataFlags,
                    TimeZone, OutputStart_DateTime, OutputEnd_DateTime );//, intervalOverride );
                    // TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
            }
        }
    }
    catch ( Exception e ) {
        message = "Unexpected error writing time series to Reclamation HDB data store \"" +
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
    String SiteCommonName = parameters.getValue( "SiteCommonName" );
    String DataTypeCommonName = parameters.getValue( "DataTypeCommonName" );
    String SiteDataTypeID = parameters.getValue( "SiteDataTypeID" );
    String ModelName = parameters.getValue( "ModelName" );
    String ModelRunName = parameters.getValue( "ModelRunName" );
    String ModelRunDate = parameters.getValue( "ModelRunDate" );
    String HydrologicIndicator = parameters.getValue( "HydrologicIndicator" );
    String ModelRunID = parameters.getValue( "ModelRunID" );
    String EnsembleName = parameters.getValue( "EnsembleName" );
    String EnsembleTraceID = parameters.getValue( "EnsembleTraceID" );
    String EnsembleModelName = parameters.getValue( "EnsembleModelName" );
    String EnsembleModelRunDate = parameters.getValue( "EnsembleModelRunDate" );
    String Agency = parameters.getValue( "Agency" );
    String ValidationFlag = parameters.getValue( "ValidationFlag" );
    String OverwriteFlag = parameters.getValue( "OverwriteFlag" );
    String DataFlags = parameters.getValue( "DataFlags" );
    String TimeZone = parameters.getValue( "TimeZone" );
	String OutputStart = parameters.getValue ( "OutputStart" );
	String OutputEnd = parameters.getValue ( "OutputEnd" );
	// TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
	//String IntervalOverride = parameters.getValue ( "IntervalOverride" );
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
    if ( (SiteCommonName != null) && (SiteCommonName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SiteCommonName=\"" + SiteCommonName + "\"" );
    }
    if ( (DataTypeCommonName != null) && (DataTypeCommonName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataTypeCommonName=\"" + DataTypeCommonName + "\"" );
    }
    if ( (SiteDataTypeID != null) && (SiteDataTypeID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SiteDataTypeID=" + SiteDataTypeID );
    }
    if ( (ModelName != null) && (ModelName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ModelName=\"" + ModelName + "\"" );
    }
    if ( (ModelRunName != null) && (ModelRunName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ModelRunName=\"" + ModelRunName + "\"" );
    }
    if ( (ModelRunDate != null) && (ModelRunDate.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ModelRunDate=\"" + ModelRunDate + "\"" );
    }
    if ( (HydrologicIndicator != null) && (HydrologicIndicator.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "HydrologicIndicator=\"" + HydrologicIndicator + "\"" );
    }
    if ( (ModelRunID != null) && (ModelRunID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ModelRunID=" + ModelRunID );
    }
    if ( (EnsembleName != null) && (EnsembleName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleName=\"" + EnsembleName + "\"" );
    }
    if ( (EnsembleTraceID != null) && (EnsembleTraceID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleTraceID=\"" + EnsembleTraceID + "\"" );
    }
    if ( (EnsembleModelName != null) && (EnsembleModelName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleModelName=\"" + EnsembleModelName + "\"" );
    }
    if ( (EnsembleModelRunDate != null) && (EnsembleModelRunDate.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleModelRunDate=\"" + EnsembleModelRunDate + "\"" );
    }
    if ( (Agency != null) && (Agency.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Agency=\"" + Agency + "\"");
    }
    if ( (ValidationFlag != null) && (ValidationFlag.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ValidationFlag=\"" + ValidationFlag + "\"" );
    }
    if ( (OverwriteFlag != null) && (OverwriteFlag.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OverwriteFlag=\"" + OverwriteFlag + "\"" );
    }
    if ( (DataFlags != null) && (DataFlags.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataFlags=\"" + DataFlags + "\"" );
    }
    if ( (TimeZone != null) && (TimeZone.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TimeZone=\"" + TimeZone + "\"" );
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
    // TODO SAM 2013-04-20 Current thought is irregular data is OK to instantaneous table - remove later
    /*
    if ( (IntervalOverride != null) && (IntervalOverride.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IntervalOverride=\"" + IntervalOverride + "\"" );
    }
    */
	return getCommandName() + "(" + b.toString() + ")";
}

}