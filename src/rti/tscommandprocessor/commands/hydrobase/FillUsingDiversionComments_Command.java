package rti.tscommandprocessor.commands.hydrobase;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSLimits;
import RTi.TS.TSUtil;
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
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import DWR.DMI.HydroBaseDMI.HydroBaseDMI;
import DWR.DMI.HydroBaseDMI.HydroBase_Structure;
import DWR.DMI.HydroBaseDMI.HydroBase_Util;
import DWR.DMI.HydroBaseDMI.HydroBase_WaterDistrict;

/**
This class initializes, checks, and runs the FillUsingDiversionComments() command, which is specific to the HydroBase database.
*/
public class FillUsingDiversionComments_Command extends AbstractCommand
implements Command
{

/**
Parameter values used with RecalcLimits.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Constructor.
*/
public FillUsingDiversionComments_Command ()
{	super();
	setCommandName ( "FillUsingDiversionComments" );
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
{	
	String TSID = parameters.getValue ( "TSID" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	String FillUsingCIU = parameters.getValue ( "FillUsingCIU" );
	String RecalcLimits = parameters.getValue ( "RecalcLimits" );
	String warning = "";
    String message = "";
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	if ( (TSID == null) || (TSID.length() == 0) ) {
        message = "A TSID must be specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a time series identifier." ) );
                
	}
	if ( (FillStart != null) && !FillStart.equals("") && !FillStart.equalsIgnoreCase("OutputStart")){
		try {
		    DateTime.parse(FillStart);
		}
		catch ( Exception e ) {
            message = "The fill start date/time \"" + FillStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a date or OutputStart." ) );

		}
	}
	if ( (FillEnd != null) && !FillEnd.equals("") && !FillEnd.equalsIgnoreCase("OutputEnd") ) {
		try {
		    DateTime.parse( FillEnd);
		}
		catch ( Exception e ) {
            message = "The fill end date/time \"" + FillEnd + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a date or OutputEnd." ) );

		}
	}
	if ( FillUsingCIU != null && !(FillUsingCIU.equalsIgnoreCase("True")) && 
		!(FillUsingCIU.equalsIgnoreCase("False")) && !(FillUsingCIU.equalsIgnoreCase(""))) {
        message = "Fill Using CIU is invalid.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Fill Using CIU must be true, false or blank." ) );
	}
    if ( (RecalcLimits != null) && !RecalcLimits.equals("") && !RecalcLimits.equalsIgnoreCase( "true" ) && 
        !RecalcLimits.equalsIgnoreCase("false") ) {
        message = "The RecalcLimits parameter must be blank, " + _False + " (default), or " + _True + ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a 1-character fill flag or Auto." ) );
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>();
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "FillStart" );
    validList.add ( "FillEnd" );
    validList.add ( "FillFlag" );
    validList.add ( "FillFlagDescription" );
    validList.add ( "RecalcLimits" );
    validList.add ( "FillUsingCIU" );
    validList.add ( "FillUsingCIUFlag" );
    validList.add ( "FillUsingCIUFlagDescription" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );
    
	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), warning );
		throw new InvalidCommandParameterException ( warning );
	}
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Creates a Property List with information needed to fill a Time Series with a constant value.
@param inputTS Time Series to fill.
@param value Constant used to fill the Time Series.
@param fillFlag the flag to use for filled values.
@param fillFlagDesc description for the fill flag
@return PropList List that contains information on filling a constant value for the given Time Series and dates.
 */
public static PropList createFillConstantPropList( TS inputTS, String fillFlag, String fillFlagDesc)
{
	if( inputTS == null ) {
		return null;
	}
	
	// Create the PropList
	PropList prop = new PropList( "List to fill TS with constant value");
	if ( fillFlag != null && !fillFlag.equals("") ) {
		prop.add( "FillFlag=" + fillFlag);
	}
	if ( fillFlagDesc != null && !fillFlagDesc.equals("") ) {
		prop.add( "FillFlagDescription=" + fillFlagDesc);
	}
	
	return prop;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new FillUsingDiversionComments_JDialog ( parent, this )).ok();
}

// Use super parseCommand

/**
Calls TSCommandProcessor to re-calculate limits for this time series.
Only month and year interval data are processed.
@param ts Time Series.
@param TSCmdProc CommandProcessor that is using this command.
@param warningLevel Warning level used for displaying warnings.
@param warning_count Number of warnings found.
@param command_tag Reference or identifier for this command.
 */
private int recalculateLimits( TS ts, CommandProcessor TSCmdProc, int warningLevel, int warning_count, String command_tag )
{
	String routine = "FillUsingDiversionComments_Command.recalculateLimits", message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	
    // Historical limits are only enabled for monthly and annual data
    
    int intervalBase = ts.getDataIntervalBase();
    if ( (intervalBase != TimeInterval.MONTH) && (intervalBase != TimeInterval.YEAR) ) {
    	return 0;
    }
	PropList request_params = new PropList ( "" );
	request_params.setUsingObject ( "TS", ts );
	CommandProcessorRequestResultsBean bean = null;
	try {
	    bean = TSCmdProc.processRequest( "CalculateTSAverageLimits", request_params);
	}
	catch ( Exception e ) {
        message = "Error recalculating original data limits for \"" + ts.getIdentifierString() + "\" (" + e + ").";
		Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message  );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "This may be due to the time series having all missing values." ) );
		return warning_count;
	}
	// Get the calculated limits and set in the original data limits...
	PropList bean_PropList = bean.getResultsPropList();
	Object prop_contents = bean_PropList.getContents ( "TSLimits" );
	if ( prop_contents == null ) {
        message = "Null value from CalculateTSAverageLimits(" + ts.getIdentifierString() + ")";
		Message.printWarning(warningLevel,
			MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
		return warning_count;
	}
	// Now set the limits.
	ts.setDataLimitsOriginal ( (TSLimits)prop_contents );
	return warning_count;
}

/**
Method to execute the fillUsingDiversionComments() command.
@param command_number Command number being run.
@exception Exception if there is an error processing the command.
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	int warningLevel = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;
	String message, routine = "FillUsingDiversionComments_Command.runCommand";
	PropList parameters = getCommandParameters();
	
	String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
	String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	String FillFlag = parameters.getValue ( "FillFlag" );
	String FillFlagDescription = parameters.getValue ( "FillFlagDescription" );
	String RecalcLimits = parameters.getValue ( "RecalcLimits" );
	String FillUsingCIU = parameters.getValue ( "FillUsingCIU" );
	String FillUsingCIUFlag = parameters.getValue ( "FillUsingCIUFlag" );
	String FillUsingCIUFlagDescription = parameters.getValue ( "FillUsingCIUFlagDescription" );
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
    CommandProcessor processor = getCommandProcessor();
	
	DateTime start = null;
	DateTime end = null;
	try {
	    if ( FillStart != null ) {
			start = DateTime.parse(FillStart);
		}
	}
	catch ( Exception e ) {
		message = "Fill start " + FillStart + " is not a valid date.";
		Message.printWarning ( warningLevel, 
			MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid fill start." ) );
		throw new InvalidCommandParameterException ( message );
	}
	try {
	    if ( FillEnd != null ) {
			end = DateTime.parse(FillEnd);
		}
	}
	catch ( Exception e ) {
		message = "Fill end " + FillEnd + " is not a valid date.";
		Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid fill end." ) );
		throw new InvalidCommandParameterException ( message );
	}
	
	// Get the time series to process.  Allow TSID to be a pattern or specific time series...

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
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	}
    if ( bean == null ) {
        Message.printStatus ( 2, routine, "Bean is null.");
    }
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	List<TS> tslist = null;
	if ( o_TSList == null ) {
		message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
		Message.printWarning ( log_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
	}
	else {
        tslist = (List)o_TSList;
		if ( tslist.size() == 0 ) {
			message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
			Message.printWarning ( log_level,
					MessageUtil.formatMessageTag(
							command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message,
                            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
		}
	}
	
	int nts = tslist.size();
	if ( nts == 0 ) {
		message = "Unable to find time series to scale using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
		Message.printWarning ( warningLevel,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
	}
	
	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Command parameter data has errors.  Unable to run command.";
		Message.printWarning ( warningLevel,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		throw new CommandException ( message );
	}
	
	boolean HaveOutputPeriod_boolean = false;
	try {
	    Object o = processor.getPropContents ( "HaveOutputPeriod");
		if ( o == null ) {
            message = "Unable to whether output period is available.  Assuming False.";
			Message.printWarning(warningLevel,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
		}
		else {
		    HaveOutputPeriod_boolean = ((Boolean)o).booleanValue();
		}
	}
	catch ( Exception e ) {
			message = "Error requesting HaveOutputPeriod from processor - not using.";
            Message.printWarning(warningLevel,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
	}
	
	TS ts = null;
	for ( int its = 0; its < nts; its++ ) {
		ts = tslist.get(its);
		if ( ts == null ) {
			continue;
		}
		
		// Get the HydroBase connection instance to use for this time series.
		// TODO SAM 2015-02-22 This could get tricky if time series are not directly read from HydroBase
		
		request_params = new PropList ( "" );
		request_params.set ( "InputName", "" + ts.getIdentifier().getInputName() );
		try {
		    bean = processor.processRequest( "GetHydroBaseDMI", request_params);
		}
		catch ( Exception e ) {
            message = "Error requesting GetHydroBaseDMI(InputName=\"" +
            ts.getIdentifier().getInputName() + "\") from processor.  Skipping time series.";
			Message.printWarning(warningLevel,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Tried to match HydroBase from TSID but could not." ) );
			continue;
		}
		bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "HydroBaseDMI" );
		if ( prop_contents == null ) {
            message = "Null value for GetHydroBaseDMI(InputName=\"" +
            ts.getIdentifier().getInputName() + "\") returned from processor.  Skipping time series.";
			Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a HydroBase database connection is open." ) );
				continue;
		}
		HydroBaseDMI hbdmi = (HydroBaseDMI)prop_contents;
		
		// Fill with diversion comments...

		if ( HaveOutputPeriod_boolean ) {
			// No need to extend the period...
			try {
			    notifyCommandProgressListeners ( its, tslist.size(), (float)-1.0, "Filling time series " +
		            ts.getIdentifier().toStringAliasAndTSID() );
				HydroBase_Util.fillTSUsingDiversionComments ( hbdmi, ts, start, end, FillFlag, FillFlagDescription, false );
			} catch (Exception e) {
                message = "Could not fill time series:" + ts.getIdentifier() + " with diversion comments.";
				Message.printWarning(warningLevel,
						MessageUtil.formatMessageTag( command_tag,
						++warning_count), routine, message );
				Message.printWarning(3, routine, e );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Check the log file for more details." ) );
			}
		}
		else {
		    // Extend the period if data are available...
			try {
				HydroBase_Util.fillTSUsingDiversionComments ( hbdmi, ts, start, end, FillFlag, FillFlagDescription, true );
			} catch (Exception e) {
                message = "Could not fill time series:" + ts.getIdentifier() + " with diversion comments.";
				Message.printWarning(warningLevel,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, message);
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Check the log file for more details." ) );
				Message.printWarning(3, routine, e );
			}
		}
		
		// If FillUsingCIU is set to true then the HydroBase CIU flag
		// will be checked and missing values will be tagged.  
		// Cases:
		// HydroBase CIU Flag = "H" or "I"
		// => Limits of Time Series are recomputed.
		// => Missing data values at the end of the period are filled with
		// zeros and tagged.
		// HydroBase CIU Flag = "N"
		// => Limits of Time Series are recomputed
	    // => Missing data values at the beginning of the period are filled
		// with zeros and tagged.
		if ( FillUsingCIU != null && FillUsingCIU.equalsIgnoreCase( "true" ) ) {
			// get CIU flag value from HydroBase
			String TSID_Location_part = ts.getLocation();
			HydroBase_Structure struct = null;
			int [] wdid_parts = null;
			try {
				wdid_parts = HydroBase_WaterDistrict.parseWDID ( TSID_Location_part );
			}
			catch (Exception e1) {
				message = "The location ID \"" + TSID_Location_part + "\" is not a WDID.";
                Message.printWarning(warningLevel,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message);
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "This command can only be applied to structure time series." ) );
			}
			int wd = wdid_parts[0];
			int id = wdid_parts[1];
			try {
				struct = hbdmi.readStructureViewForWDID ( wd, id );
			}
			catch (Exception e1) {
                message = "Error reading structure information from HydroBase for ID \"" + TSID_Location_part + "\".";
				Message.printWarning(warningLevel, 
					MessageUtil.formatMessageTag( command_tag,
					++warning_count), routine, message);
                status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the structure WDID is valid and data are in HydroBase." ) );
			}
			if ( struct != null ) {
    			// HydroBase currently in use value
    			String ciu = struct.getCiu();
    			String fillFlag = "";
    			String fillFlagDescription = "";
    			if( (FillUsingCIUFlag != null) && !FillUsingCIUFlag.equals("")) {
    				if( FillUsingCIUFlag.equalsIgnoreCase( "Auto" )) {
    					// Use CIU value from HydroBase
    					fillFlag = ciu;
    				}
    				else {
    					fillFlag = FillUsingCIUFlag;
    				}
    			}
    			if( (FillUsingCIUFlagDescription != null) && !FillUsingCIUFlagDescription.equals("")) {
    				if( FillUsingCIUFlag.equalsIgnoreCase( "Auto" )) {
    					// Use CIU value from HydroBase
    					fillFlagDescription = "Filled with zero because HydroBase structure CIU=" + ciu;
    				}
    				else {
    					fillFlagDescription = FillUsingCIUFlagDescription;
    				}
    			}
    			// Based on CIU string, fill missing values with flag value
    			// H = "Historical structure"
    			// I = "Inactive structure"
    			if( ciu.equalsIgnoreCase( "H" ) || ciu.equalsIgnoreCase( "I" )) {
    				// Recalculate TS Limits
    				warning_count = recalculateLimits( ts, processor, warningLevel, warning_count, command_tag );
    				// Fill missing data values at end of period with zeros
    				try {
    					// Get the nearest data point from the end of the period
    					TSData tmpTSData = TSUtil.findNearestDataPoint(ts, start, end, true);
    					if( tmpTSData != null) {
    						// Set the properties for filling with constant
    						PropList const_prop = createFillConstantPropList(ts,fillFlag, fillFlagDescription);
    						// Fill time series with zeros from last non-missing value until the end of the period.
    						if ( end == null ) {
    							TSUtil.fillConstant(ts, tmpTSData.getDate(), ts.getDate2(), 0, const_prop);
    						}
    						else {
    							TSUtil.fillConstant(ts, tmpTSData.getDate(), end, 0, const_prop);
    						}
    					}
    				}
    				catch (Exception e) {
                        message = "Could not fill time series with CIU code: " + ts.getIdentifier();
    					Message.printWarning(warningLevel, 
    						MessageUtil.formatMessageTag( command_tag,
    						++warning_count), routine, message);
    					Message.printWarning(3, routine, e );
                        status.addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Report the problem to software support." ) );
    				}	
    			}
    			// N = "Non-existent structure"
    			else if( ciu.equalsIgnoreCase( "N" )) {
    				// Recalculate TS Limits
    				warning_count = recalculateLimits( ts, processor, warningLevel, warning_count, command_tag );
    				try {
    					// Get first data point from front
    					TSData tmpTSData = TSUtil.findNearestDataPoint(ts, start, end, false);
    					if(tmpTSData != null) {
    						// Create propList for fill command
    						PropList const_prop = createFillConstantPropList(ts, fillFlag, fillFlagDescription);
    						// Fill time series with zero's from first non-missing value until the beginning of the period.
    						TSUtil.fillConstant(ts, ts.getDate1(), tmpTSData.getDate(), 0, const_prop);
    					}
    				}
    				catch (Exception e) {
                        message = "Unexpected error filling time series with CIU code: " + ts.getIdentifier() + "(" + e + ")";
    					Message.printWarning(warningLevel, 
    						MessageUtil.formatMessageTag( command_tag,
    						++warning_count), routine, message);
    					Message.printWarning(3, routine, e );
                        status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
    				}
    			}	
			}
		}
		else if ( RecalcLimits.equalsIgnoreCase( "True" ) ) {
		    // The following method handles exceptions recomputing the limits
	        recalculateLimits( ts, processor, warningLevel, warning_count, command_tag );
		}
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag(
			command_tag, ++warning_count),
			routine,message);
		throw new CommandWarningException ( message );
	}
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
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
	String FillStart = props.getValue( "FillStart" );
	String FillEnd = props.getValue( "FillEnd" );
	String FillFlag = props.getValue( "FillFlag" );
	String FillFlagDescription = props.getValue( "FillFlagDescription" );
	String RecalcLimits = props.getValue( "RecalcLimits" );
	String FillUsingCIU = props.getValue( "FillUsingCIU" );
	String FillUsingCIUFlag = props.getValue( "FillUsingCIUFlag" );
	String FillUsingCIUFlagDescription = props.getValue( "FillUsingCIUFlagDescription" );
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
	if ( (FillStart != null) && (FillStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillStart=\"" + FillStart + "\"" );
	}
	if ( (FillEnd != null) && (FillEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillEnd=\"" + FillEnd + "\"" );
	}
	if ( (FillFlag != null) && (FillFlag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillFlag=\"" + FillFlag + "\"" );
	}
	if ( (FillFlagDescription != null) && (FillFlagDescription.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillFlagDescription=\"" + FillFlagDescription + "\"" );
	}
	if ( ( RecalcLimits != null) && (RecalcLimits.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "RecalcLimits=" + RecalcLimits );
	}
	if ( (FillUsingCIU != null) && (FillUsingCIU.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillUsingCIU=" + FillUsingCIU );
	}
	if ( (FillUsingCIUFlag != null) && (FillUsingCIUFlag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillUsingCIUFlag=\"" + FillUsingCIUFlag + "\"" );
	}
	if ( (FillUsingCIUFlagDescription != null) && (FillUsingCIUFlagDescription.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillUsingCIUFlagDescription=\"" + FillUsingCIUFlagDescription + "\"" );
	}
	
	return getCommandName() + "(" + b.toString() + ")";
}

}