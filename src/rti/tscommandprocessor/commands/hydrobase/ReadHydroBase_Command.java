// ReadHydroBase_Command - This class initializes, checks, and runs the ReadHydroBase() command.

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

package rti.tscommandprocessor.commands.hydrobase;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.GR.GRLimits;
import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandSavesMultipleVersions;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.MissingObjectEvent;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.InvalidTimeIntervalException;
import RTi.Util.Time.TimeInterval;
import DWR.DMI.HydroBaseDMI.HydroBaseDMI;
import DWR.DMI.HydroBaseDMI.HydroBaseDataStore;
import DWR.DMI.HydroBaseDMI.HydroBase_AgriculturalCASSCropStats;
import DWR.DMI.HydroBaseDMI.HydroBase_AgriculturalCASSLivestockStats;
import DWR.DMI.HydroBaseDMI.HydroBase_AgriculturalNASSCropStats;
import DWR.DMI.HydroBaseDMI.HydroBase_CUPopulation;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_AgriculturalCASSLivestockStats_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_CUPopulation_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_GroundWater_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_SheetNameWISFormat_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GroundWaterWellsView;
import DWR.DMI.HydroBaseDMI.HydroBase_StationGeolocMeasType;
import DWR.DMI.HydroBaseDMI.HydroBase_StructureGeolocStructMeasType;
import DWR.DMI.HydroBaseDMI.HydroBase_StructureIrrigSummaryTS;
import DWR.DMI.HydroBaseDMI.HydroBase_Util;
import DWR.DMI.HydroBaseDMI.HydroBase_WaterDistrict;
import DWR.DMI.HydroBaseDMI.HydroBase_WISSheetNameWISFormat;

/**
This class initializes, checks, and runs the ReadHydroBase() command.
*/
public class ReadHydroBase_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, CommandSavesMultipleVersions
{

/**
Number of where clauses shown in the editor and available as parameters - the HydroBase SPFlex maximum minus
2 (data type and interval).
*/
private int __numWhere = (HydroBaseDMI.getSPFlexMaxParameters() - 2);

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
public ReadHydroBase_Command ()
{	super();
	setCommandName ( "ReadHydroBase" );
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

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    String InputName = parameters.getValue ( "InputName" );
    String DataStore = parameters.getValue ( "DataStore" );
    String TSID = parameters.getValue ( "TSID" );
    String InputStart = parameters.getValue ( "InputStart" );
    String InputEnd = parameters.getValue ( "InputEnd" );

    if ( (InputName != null) && !InputName.equals("") && (DataStore != null) && !DataStore.equals("")) {
        // Check the parts of the TSID...
        message = "InputName and DataStore cannot both be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify InputName or DataStore but not both." ) );
    }
    
	if ( (TSID != null) && !TSID.equals("") ) {
	    // Check the parts of the TSID...
		TSIdent tsident = null;
		try {
            tsident = new TSIdent ( TSID );
			String Location = tsident.getLocation();
			String DataSource = tsident.getSource();
			String DataType = tsident.getType();
			String Interval = tsident.getInterval();
			if ( Location.length() == 0 ) {
                message = "The location part of the TSID must be specified.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a location for the time series identifier." ) );
			}
			if ( DataSource.length() == 0 ) {
                message = "The data source part of the TSID must be specified.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a data source for the time series identifier." ) );
			}
			if ( DataType.length() == 0 ) {
                message = "The data type must be specified.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a data type for the time series identifier." ) );
			}
			if ( Interval.length() == 0 ) {
                message = "The TSID interval must be specified.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an interval for the time series identifier." ) );
			}
			else {
                // TODO SAM 2006-04-25
				// Most likely the following will not be executed because parsing the TSID will generate an
				// InvalidTimeIntervalException (caught below).  This may cause the user to
				// do two checks to catch all input errors.
				try { TimeInterval.parseInterval (Interval);
				}
				catch ( Exception e ) {
                    message = "The TSID data interval \"" + Interval + "\" is invalid";
					warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid data interval." ) );
				}
			}
		}
		catch ( InvalidTimeIntervalException e ) {
			// Will not even be able to print it...
            message = "The data interval in the time series identifier \"" + TSID + "\" is invalid";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid data interval." ) );
		}
		catch ( Exception e ) {
            message = "Unable to parse TSID \"" + TSID + "\" to check its parts.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify the time series identifier." ) );
		}
	}
	// InputName is optional.
	if ( (TSID == null) || TSID.equals("") ) {
    	String DataType = parameters.getValue ( "DataType" );
    	if ( (DataType == null) || (DataType.length() == 0) ) {
            message = "The data type must be specified.";
    		warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the data type." ) );
    	}
    	String Interval = parameters.getValue ( "Interval" );
    	if ( (Interval == null) || (Interval.length() == 0) ) {
            message = "The data interval must be specified.";
    		warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the data interval." ) );
    	}
    	else {
    	    try { TimeInterval.parseInterval (Interval);
    		}
    		catch ( Exception e ) {
                message = "The data interval \"" + Interval + "\" is invalid";
    			warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid data interval." ) );
    		}
    	}
	}
	// InputName is optional.
	// TODO SAM 2006-04-24 Need to check the WhereN parameters.

	// Used with both versions of the command...

	if (	(InputStart != null) && !InputStart.equals("") &&
		!InputStart.equalsIgnoreCase("InputStart") &&
		!InputStart.equalsIgnoreCase("InputEnd") ) {
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
		!InputEnd.equalsIgnoreCase("InputEnd") ) {
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

	/* TODOD SAM 2006-04-27 As per Ray Bennett always do this.
	*/
	String FillDivRecordsCarryForward = parameters.getValue ( "FillDivRecordsCarryForward" );
	if ((FillDivRecordsCarryForward != null) && !FillDivRecordsCarryForward.equals("")){
		if ( !FillDivRecordsCarryForward.equalsIgnoreCase(_True) &&
			!FillDivRecordsCarryForward.equalsIgnoreCase(_False) ) {
            message = "Invalid FillDivRecordsCarryForward parameter \"" + FillDivRecordsCarryForward + "\"";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify " + _False + " or " + _True + ", or blank for default of " + _True + "." ) );
		}
	}
	String FillUsingDivComments = parameters.getValue ( "FillUsingDivComments" );
	if ((FillUsingDivComments != null) && !FillUsingDivComments.equals("")){
		if (	!FillUsingDivComments.equalsIgnoreCase(_True) &&
			!FillUsingDivComments.equalsIgnoreCase(_False) ) {
            message = "The FillUsingDivComments parameter \"" + FillUsingDivComments +
            "\" must be True (blank) or False";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify False or True, or blank for default of False." ) );
		}
	}
	String FillUsingDivCommentsFlag = parameters.getValue ( "FillUsingDivCommentsFlag" );
	if ( (FillUsingDivCommentsFlag != null) && (FillUsingDivCommentsFlag.length() != 1) ) {
        message = "The FillUsingDivCommentsFlag must be 1 character long.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                  new CommandLogRecord(CommandStatusType.FAILURE,
                          message, "Specify a 1-character flag, or blank to not use flag." ) );
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
    validList.add ( "InputName" );
    validList.add ( "DataStore" );
    validList.add ( "Alias" );
    validList.add ( "TSID" );
    validList.add ( "DataType" );
    validList.add ( "Interval" );
    int numFilters = HydroBaseDMI.getSPFlexMaxParameters() - 2; // Maximum minus data type and interval
    for ( int i = 1; i <= numFilters; i++ ) { 
        validList.add ( "Where" + i );
    }
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "FillDivRecordsCarryForward" );
    validList.add ( "FillDivRecordsCarryForwardFlag" );
    validList.add ( "FillUsingDivComments" );
    validList.add ( "FillUsingDivCommentsFlag" );
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
    return __discovery_TS_Vector;
}

/**
Return the list of data objects read by this object in discovery mode.
The following classes can be requested:  TS
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
	return (new ReadHydroBase_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param commandString A string command to parse.
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand ( String commandString )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	String routine = "ReadHydroBase_Command.parseCommand", message;
	int warning_level = 2;
	//int warning_count = 0;

    if ( !commandString.trim().toUpperCase().startsWith("TS") ) {
        // New style syntax using simple parameter=value notation
        super.parseCommand(commandString);
    }
    else {
        CommandStatus status = getCommandStatus();
    	
        List<String> tokens = StringUtil.breakStringList ( commandString, "()", 0 );
    	if ( tokens == null ) {
    		// Must have at least the command name and something to indicate the read...
    		message = "Syntax error in \"" + commandString + "\".";
    		Message.printWarning ( warning_level, routine, message);
    		//++warning_count;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Edit the command in the command editor." ) );
    		throw new InvalidCommandSyntaxException ( message );
    	}
    
    	// Parse everything after the (, which should be command parameters...
    	
    	try {
            setCommandParameters ( PropList.parse ( Prop.SET_FROM_PERSISTENT,
    			tokens.get(1), routine, "," ) );
    	}
    	catch ( Exception e ) {
    		message = "Syntax error in \"" + commandString + "\".";
    		Message.printWarning ( warning_level, routine, message);
    		//++warning_count;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Edit the command in the command editor." ) );
    		throw new InvalidCommandSyntaxException ( message );
    	}
    	PropList parameters = getCommandParameters();
    
    	// Evaluate whether the command is a TS Alias version...
    
    	String Alias = null;
    	if (StringUtil.startsWithIgnoreCase(commandString, "TS ")) {
    		// There is an alias specified.  Extract the alias from the full command...
    		String str = commandString.substring(3);	
    		int index = str.indexOf("=");
    		int index2 = str.indexOf("(");
    		if (index2 < index) {
    			// No alias specified -- badly-formed command
    			message = "No alias was specified, although the command started with \"TS ...\"";
    			Message.printWarning(warning_level, routine, message);
    			//++warning_count;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Edit the command in the command editor - specify an alias." ) );
    			throw new InvalidCommandSyntaxException(message);
    		}
    
    		Alias = str.substring(0, index);
    	}
    	else {
    		Alias = null;
    	}
    
    	if ( Alias != null ) {
    		parameters.set ( "Alias", Alias.trim() );
    	}
    
    	// Convert QueryStart and QueryEnd to new syntax InputStart and InputEnd...
    	String QueryStart = parameters.getValue ( "QueryStart" );
    	if ( (QueryStart != null) && (QueryStart.length() > 0) ) {
    		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
    		parameters.set ( "InputStart", QueryStart );
    		parameters.unSet ( QueryStart );
    		parameters.setHowSet ( Prop.SET_UNKNOWN );
    	}
    	String QueryEnd = parameters.getValue ( "QueryEnd" );
    	if ( (QueryEnd != null) && (QueryEnd.length() > 0) ) {
    		parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
    		parameters.set ( "InputEnd", QueryEnd );
    		parameters.unSet ( QueryStart );
    		parameters.setHowSet ( Prop.SET_UNKNOWN );
    	}
    }
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
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
	TSCommandProcessor tsprocessor = (TSCommandProcessor)processor;
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    
    boolean read_data = true;
    if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
        read_data = false;
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
            status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
                status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
		}
	}

	/* TODO SAM 2006-04-27 As per Ray Bennett always do this.
	*/
	String FillDivRecordsCarryForward = parameters.getValue ("FillDivRecordsCarryForward" );
	if ( (FillDivRecordsCarryForward == null) || FillDivRecordsCarryForward.isEmpty() ) {
		FillDivRecordsCarryForward = _True; // Default is to carry forward, based on historical behavior
	}
	String FillDivRecordsCarryForwardFlag = parameters.getValue ("FillDivRecordsCarryForwardFlag" );
	String FillUsingDivComments = parameters.getValue ("FillUsingDivComments" );
	if ( (FillUsingDivComments == null) ||FillUsingDivComments.equals("") ) {
		FillUsingDivComments = _False;	// Default is NOT to fill
	}
	String FillUsingDivCommentsFlag = parameters.getValue ( "FillUsingDivCommentsFlag" );
	
    String IfMissing = parameters.getValue ("IfMissing" );
    boolean IfMissingWarn = true;  // Default
    if ( (IfMissing != null) && IfMissing.equalsIgnoreCase(_Ignore) ) {
        IfMissingWarn = false;  // Ignore when time series are not found
    }

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Set up properties for the read...

	PropList HydroBase_props = new PropList ( "HydroBase" );
	/* TODO SAM 2006-04-27 Code cleanup
	As per Ray Bennett always do this.
	*/
	HydroBase_props.set ( "FillDivRecordsCarryForward=" + FillDivRecordsCarryForward );
	if ( (FillDivRecordsCarryForwardFlag != null) && !FillDivRecordsCarryForwardFlag.isEmpty() ) {
		HydroBase_props.set ( "FillDivRecordsCarryForwardFlag=" + FillDivRecordsCarryForwardFlag );
	}
	HydroBase_props.set ( "FillUsingDivComments=" + FillUsingDivComments );
	if ( (FillUsingDivCommentsFlag != null) &&!FillUsingDivCommentsFlag.equals("")){
		HydroBase_props.set ( "FillUsingDivCommentsFlag=" + FillUsingDivCommentsFlag );
	}

	// Now try to read...

	List<TS> tslist = new ArrayList<>();	// List for time series results.
					// Will be added to for one time series
					// read or replaced if a list is read.
	try {
        String Alias = parameters.getValue ( "Alias" );
        String TSID = parameters.getValue ( "TSID" );
        if ( (TSID != null) && !TSID.equals("") ) {
			// Version that reads a single time series using the TSID

			Message.printStatus ( 2, routine,"Reading HydroBase time series \"" + TSID + "\"" );
			TS ts = null;
			TSIdent tsident = new TSIdent ( TSID );
			// Find the HydroBaseDMI to use, first checking the datastore and next the legacy input type.
			HydroBaseDMI hbdmi = null;
			String DataStore = parameters.getValue ( "DataStore" );
			if ( (DataStore != null) && !DataStore.equals("") ) {
			    // User has indicated that a datastore should be used...
    		    HydroBaseDataStore dataStore = (HydroBaseDataStore)((TSCommandProcessor)
    	            getCommandProcessor()).getDataStoreForName( DataStore, HydroBaseDataStore.class );
    	        if ( dataStore != null ) {
    	            Message.printStatus(2, routine, "Selected data store is \"" + dataStore.getName() + "\"." );
    	            hbdmi = (HydroBaseDMI)dataStore.getDMI();
    	        }
    	        else {
                    message = "Cannot get HydroBase data store for \"" + DataStore + "\".";
                    Message.printWarning ( 2, routine, message );
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a HydroBase datastore is properly configured." ) );
                    throw new RuntimeException ( message );
    	        }
			}
			else {
    			// Try to get the input type...
    			Object o = processor.getPropContents ("HydroBaseDMIList" );
    			if ( o == null ) {
    				message = "Could not get list of HydroBase connections to query data.";
    				Message.printWarning ( 2, routine, message );
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a HydroBase database connection has been opened." ) );
    				throw new RuntimeException ( message );
    			}
    			@SuppressWarnings("unchecked")
				List<HydroBaseDMI> hbdmiList = (List<HydroBaseDMI>)o;
    			hbdmi = HydroBase_Util.lookupHydroBaseDMI ( hbdmiList, tsident.getInputName() );
    			if ( hbdmi == null ) {
    				message = "Could not find HydroBase connection with input name \"" +
                    tsident.getInputName() + "\" to query data.";
    				Message.printWarning ( 2, routine, message );
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a HydroBase connection with the given name has been opened." ) );
    				throw new RuntimeException ( message );
    			}
			}
			try {
                ts = hbdmi.readTimeSeries (	TSID, InputStart_DateTime,InputEnd_DateTime, null,read_data, HydroBase_props );
			}
			catch ( Exception e ) {
			    ts = null;
				message = "Unexpected error reading HydroBase time series \"" + TSID + "\" (" + e + ").";
				Message.printWarning ( 2, routine, message );
				Message.printWarning ( 2, routine, e );
				if ( IfMissingWarn ) {
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify the time series identifier." ) );
                   throw new RuntimeException ( message );
				}
				else {
				    // Just show for info purposes
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.INFO,
                            message, "Verify the time series identifier." ) );
				}
			}
			finally {
			    if ( ts == null ) {
			        // Generate an event for listeners
			        notifyCommandProcessorEventListeners(
				        new MissingObjectEvent(TSID,Class.forName("RTi.TS.TS"),"Time Series", this));
			    }
			}
			if ( ts != null ) {
				// Set the alias...
			    if ( Alias != null ) {
			        ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString(
		                    processor, ts, Alias, status, command_phase) );
			    }
				tslist.add ( ts );
			}
		}
		else {
            // Read 1+ time series...
			// Get the input needed to process the file...
			String DataType = parameters.getValue ( "DataType" );
			String Interval = parameters.getValue ( "Interval" );
			String InputName = parameters.getValue ( "InputName" );
			if ( InputName == null ) {
				InputName = "";
			}
			List<String> whereNList = new ArrayList<String> ( 6 );
			String WhereN;
			int nfg = 0; // Used below.
			// User may have skipped where and left a blank so loop over a sufficiently large number of where parameters
			for ( int ifg = 0; ifg < 25; ifg++ ) {
				WhereN = parameters.getValue ( "Where" + (ifg + 1) );
				if ( WhereN != null ) {
					++nfg;
					whereNList.add ( WhereN );
				}
			}
		
			// Find the HydroBaseDMI to use...
			HydroBaseDMI hbdmi = null;
			HydroBaseDataStore hbDataStore = null;
			// First try to get from the DataStore list...
			String DataStore = parameters.getValue ( "DataStore" );
			DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
		         DataStore, HydroBaseDataStore.class );
	        if ( dataStore != null ) {
	            // Found a datastore so use it...
	            hbDataStore = (HydroBaseDataStore)dataStore;
	            hbdmi = (HydroBaseDMI)hbDataStore.getDMI();
	            //Message.printStatus(2,routine,"Using \"HydroBase\" datastore.");
	        }
	        else {
    			// Also try to get DMI from legacy list...
    			Object o = processor.getPropContents ( "HydroBaseDMIList" );
    			if ( o == null ) {
    				message = "Could not get list of legacy HydroBase connections to query data.";
    				Message.printWarning ( 2, routine, message );
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a HydroBase database connection has been opened." ) );
    				throw new Exception ( message );
    			}
    			//Message.printStatus(2,routine,"Using HydroBase input type.");
    			@SuppressWarnings("unchecked")
				List<HydroBaseDMI> hbdmi_Vector = (List<HydroBaseDMI>)o;
    			hbdmi = HydroBase_Util.lookupHydroBaseDMI ( hbdmi_Vector, InputName );
    			// Create a temporary data store to pass to following code
    			hbDataStore = new HydroBaseDataStore ( InputName, "State of Colorado HydroBase", hbdmi );
			}
			if ( hbdmi == null ) {
				message ="Could not find HydroBase data store or input name connection using \"" + InputName +
				    "\" to query data.";
				Message.printWarning ( 2, routine, message );
                status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that a HydroBase database connection has been opened." ) );
				throw new Exception ( message );
			}

			// Initialize an input filter based on the data type...

			InputFilter_JPanel filterPanel = null;
			boolean is_Station = false;
			boolean is_Structure = false;
			boolean is_StructureSFUT = false;
	        boolean is_CASSCrops = false;
            boolean is_CASSLivestock = false;
            boolean is_cuPop = false;
            boolean is_NASS = false;
			boolean is_StructureIrrigSummaryTS = false;
			boolean is_Well = false;
			boolean is_WIS = false;

			int wdid_length = HydroBase_Util.getPreferredWDIDLength();

			// Create the input filter panel...
			// Get the HydroBase "meas type" that corresponds to the time series data type
		    String [] hb_mt = HydroBase_Util.convertToHydroBaseMeasType( DataType, Interval );
		    String hbMeasType = hb_mt[0];
		    String hbTimeStep = hb_mt[2];

			if ( HydroBase_Util.isStationTimeSeriesDataType ( hbdmi, hbMeasType ) ){
				// Stations...
				is_Station = true;
				filterPanel = new HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel ( hbDataStore );
				Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for station." );
			}
			else if ( HydroBase_Util.isStructureSFUTTimeSeriesDataType ( hbdmi, hbMeasType ) ) {
				// Structures (with SFUT)...
				is_StructureSFUT = true;
				Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for structure SFUT." );
				filterPanel = new HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
				        hbDataStore, true, 6, -1 );
			}
			else if ( HydroBase_Util.isStructureTimeSeriesDataType (hbdmi, hbMeasType ) ) {
				// Structures (no SFUT)...
				is_Structure = true;
				filterPanel = new HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
				        hbDataStore, false, 6, -1 );
				Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for structure (no SFUT)." );
			}
            else if ( HydroBase_Util.isAgriculturalCASSCropStatsTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                is_CASSCrops = true;
                filterPanel = new HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel ( hbDataStore );
                Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for CASS crops." );
            }
            else if ( HydroBase_Util.isAgriculturalCASSLivestockStatsTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                is_CASSLivestock = true;
                filterPanel = new HydroBase_GUI_AgriculturalCASSLivestockStats_InputFilter_JPanel ( hbDataStore );
                Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for CASS livestock." );
            }
            else if ( HydroBase_Util.isCUPopulationTimeSeriesDataType ( hbdmi, hbMeasType) ) {
                is_cuPop = true;
                filterPanel = new HydroBase_GUI_CUPopulation_InputFilter_JPanel ( hbDataStore );
                Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for CU population" );
            }
            else if ( HydroBase_Util.isAgriculturalNASSCropStatsTimeSeriesDataType ( hbdmi, hbMeasType ) ) {
                // Data from agricultural_NASS_crop_statistics...
                is_NASS = true;
                filterPanel = new HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel ( hbDataStore );
                Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for NASS." );
            }
			else if ( HydroBase_Util.isIrrigSummaryTimeSeriesDataType(hbdmi, hbMeasType ) ) {
				// Irrig summary TS...
				is_StructureIrrigSummaryTS = true;
				filterPanel = new HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel ( hbDataStore );
				Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for structure irrig summary ts." );
			}
            else if( HydroBase_Util.isGroundWaterWellTimeSeriesDataType (hbdmi, hbMeasType ) ) {
                // Well...
                is_Well = true;
                filterPanel = new HydroBase_GUI_GroundWater_InputFilter_JPanel ( hbDataStore, null, true );
                Message.printStatus ( 2, routine,"Data type \"" + DataType + "\" is for a well." );
            }
			else if( HydroBase_Util.isWISTimeSeriesDataType (hbdmi, hbMeasType ) ) {
				// WIS...
				is_WIS = true;
				filterPanel = new HydroBase_GUI_SheetNameWISFormat_InputFilter_JPanel ( hbDataStore );
				Message.printStatus ( 2, routine,"Data type \"" + DataType + "\" is for WIS." );
			}
			else {
                message = "Data type \"" + DataType + "\" is not recognized as a HydroBase data type.";
				Message.printWarning ( 2, routine, message );
                status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the datatype can be used with HydroBase (see documentation)." ) );
				throw new Exception ( message );
			}

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
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support - also see the log file." ) );
				}
			}
		
			// Read the list of objects from which identifiers can be obtained.  This code is similar to that in
			// TSTool_JFrame.readHydroBaseHeaders...
		
			Message.printStatus ( 2, routine, "Getting the list of time series..." );
			
			// Specific lists returned for time series metadata - create empty lists to avoid null issues
			List<HydroBase_AgriculturalCASSCropStats> cassCropStatsTSCatalogList = new ArrayList<HydroBase_AgriculturalCASSCropStats>();
			List<HydroBase_AgriculturalCASSLivestockStats> cassLivestockStatsTSCatalogList = new ArrayList<HydroBase_AgriculturalCASSLivestockStats>();
			List<HydroBase_StationGeolocMeasType> stationTSCatalogList = new ArrayList<HydroBase_StationGeolocMeasType>();
			List<HydroBase_StructureGeolocStructMeasType> structureTSCatalogList = new ArrayList<HydroBase_StructureGeolocStructMeasType>();
			List<HydroBase_AgriculturalNASSCropStats> nassCropStatsTSCatalogList = new ArrayList<HydroBase_AgriculturalNASSCropStats>();
			List<HydroBase_CUPopulation> cupopTSCatalogList = new ArrayList<HydroBase_CUPopulation>();
			List<HydroBase_StructureIrrigSummaryTS> irrigSummaryTSCatalogList = new ArrayList<HydroBase_StructureIrrigSummaryTS>();
			List<HydroBase_WISSheetNameWISFormat> wisTSCatalogList = new ArrayList<HydroBase_WISSheetNameWISFormat>();
			List<HydroBase_GroundWaterWellsView> groundwaterWellsTSCatalogList = new ArrayList<HydroBase_GroundWaterWellsView>();

			// Start code that should closely match TSTool main window...
			
			// Set up the data so that code closely matches TSTool main interface
			int size = 0;
			InputFilter_JPanel selectedInputFilterJPanel = filterPanel;
			String selectedDataType = DataType;
			String selectedTimeStep = Interval;
			GRLimits grlimits = null; // No spatial query
			String meas_type = hbMeasType;
			String hbtime_step = hbTimeStep;
			if ( HydroBase_Util.isAgriculturalCASSCropStatsTimeSeriesDataType ( hbdmi, selectedDataType ) ) {
				// Data from agricultural_CASS_crop_statistics
	    		cassCropStatsTSCatalogList = hbdmi.readAgriculturalCASSCropStatsList (
					selectedInputFilterJPanel,
					null,		// county
					null,		// commodity
					null,		// practice
					null,		// date1
					null,		// date2,
					true );		// Distinct
	    		size = cassCropStatsTSCatalogList.size();
			}
			else if (HydroBase_Util.isAgriculturalCASSLivestockStatsTimeSeriesDataType ( hbdmi, selectedDataType) ) {
				// Data from CASS livestock stats...
		    	cassLivestockStatsTSCatalogList = hbdmi.readAgriculturalCASSLivestockStatsList (
					selectedInputFilterJPanel,	// From input filter
					null,		// county
					null,		// commodity
					null,		// type
					null,		// date1
					null,		// date2,
					true );		// Distinct
	    		size = cassLivestockStatsTSCatalogList.size();
			}
			else if ( HydroBase_Util.isAgriculturalNASSCropStatsTimeSeriesDataType ( hbdmi, selectedDataType ) ) {
				// Data from agricultural_NASS_crop_statistics
				nassCropStatsTSCatalogList = hbdmi.readAgriculturalNASSCropStatsList (
					selectedInputFilterJPanel,
					null,		// county
					null,		// commodity
					null,		// date1
					null,		// date2,
					true );		// Distinct
	    		size = nassCropStatsTSCatalogList.size();
			}
			else if ( HydroBase_Util.isCUPopulationTimeSeriesDataType( hbdmi, selectedDataType) ) {
		    	cupopTSCatalogList = hbdmi.readCUPopulationList (
					selectedInputFilterJPanel,	// From input filter
					null,		// county
					null,		// commodity
					null,		// type
					null,		// date1
					null,		// date2,
					true );		// Distinct
	    		size = cupopTSCatalogList.size();
			}
			else if ( HydroBase_Util.isIrrigSummaryTimeSeriesDataType( hbdmi, selectedDataType ) ) {
				irrigSummaryTSCatalogList = HydroBase_Util.readStructureIrrigSummaryTSCatalogList(
					hbdmi,
					selectedInputFilterJPanel,
					null,	// orderby
					-999,	// structure_num
					-999,	// wd
					-999,	// id
					null,	// str_name
					null,	// landuse
					null,	// start
					null,	// end
					true);	// distinct
	    		size = irrigSummaryTSCatalogList.size();
			}
			else if ( HydroBase_Util.isStationTimeSeriesDataType(hbdmi, meas_type) ) {
				stationTSCatalogList = HydroBase_Util.readStationGeolocMeasTypeCatalogList(
					hbdmi, selectedInputFilterJPanel, selectedDataType, selectedTimeStep, grlimits );
	    		size = stationTSCatalogList.size();
			}
			else if ( HydroBase_Util.isStructureTimeSeriesDataType(hbdmi, meas_type) ) {
				structureTSCatalogList = hbdmi.readStructureGeolocStructMeasTypeCatalogList(
					selectedInputFilterJPanel, selectedDataType, selectedTimeStep);
	    		size = structureTSCatalogList.size();
			}
			else if (selectedDataType.equalsIgnoreCase( "WellLevel") || selectedDataType.equalsIgnoreCase( "WellLevelElev")||
		    	selectedDataType.equalsIgnoreCase( "WellLevelDepth") ) {
		    	// Well level data...
   				if (selectedTimeStep.equalsIgnoreCase("Day")) {
					groundwaterWellsTSCatalogList =
						HydroBase_Util.readGroundWaterWellsViewTSCatalogList(hbdmi, selectedInputFilterJPanel, meas_type, hbtime_step);
	    			size = groundwaterWellsTSCatalogList.size();
   				}
			}
			else if ( HydroBase_Util.isWISTimeSeriesDataType ( hbdmi, selectedDataType ) ) {
				// WIS TS...
				wisTSCatalogList = hbdmi.readWISSheetNameWISFormatListDistinct(selectedInputFilterJPanel);
	    		size = wisTSCatalogList.size();
			}

			// ...end code that should closely match TSTool main window.
		
       		if ( size == 0 ) {
				Message.printStatus ( 2, routine,"No HydroBase time series were found." );
		        // Warn if nothing was retrieved (can be overridden to ignore).
	            if ( IfMissingWarn ) {
	                message = "No time series were read from HydroBase.";
	                Message.printWarning ( warning_level, 
	                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Data may not be in database." +
                            	"  Previous messages may provide more information." ) );
	            }
	            else {
	                // Ignore the problem.  Call it a success if no other problems occurred.
	                status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
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
	        HydroBase_StationGeolocMeasType sta;
            HydroBase_StructureGeolocStructMeasType str;
			HydroBase_AgriculturalCASSCropStats cassCrops;
			HydroBase_AgriculturalCASSLivestockStats cassLivestock;
			HydroBase_CUPopulation cuPop;
			HydroBase_AgriculturalNASSCropStats nass;
            HydroBase_StructureIrrigSummaryTS irrigts;
            HydroBase_GroundWaterWellsView well;
			HydroBase_WISSheetNameWISFormat wis;
			String inputName = "";	// Input name to add to identifiers, if it has been
						// requested (this includes the tilde character).
			if ( (DataStore != null) && !DataStore.equals("") ) {
			    // Use the datastore for the time series input name (will not include "HydroBase" since
			    // datastore has the connection information)
			    inputName = "~" + DataStore;
			}
			else {
			    // Use the input name
    			if ( (InputName != null) && (InputName.length() > 0) ) {
    				// Include the input name in the returned TSIdent...
    				inputName = "~HydroBase~" + InputName;
    			}
    			else {
    			    inputName = "~HydroBase"; // No input name so use default legacy DMI connection
    			}
			}
			for ( int i = 0; i < size; i++ ) {
				// Check to see if reading time series should be canceled because the command has
				// been canceled.
				if ( tsprocessor.getCancelProcessingRequested() ) {
					// The user has requested that command processing should be canceled.
					// Check here in this command because a very large query could take a long time before a single command finishes.
					Message.printStatus(2, routine, "Cancel processing based on user request.");
					break;
				}
				// List in order of likelihood to improve performance...
				if ( is_Station ) {
					// Station TS...
					sta = stationTSCatalogList.get(i);
					tsident_string = sta.getStation_id()
						+ "." + sta.getData_source()
						+ "." + DataType
						+ "." + Interval
						+ inputName;
				}
				else if ( is_Structure ) {
					str = structureTSCatalogList.get(i);
					tsident_string = HydroBase_WaterDistrict.formWDID( wdid_length,str.getWD(),str.getID())
						+ "." + str.getData_source()
						+ "." + DataType
						+ "." + Interval
						+ inputName;
				}
				else if ( is_StructureSFUT ) {
					// The catalog for SFUT uses the same list given that the datatype returned SFUT in the query
					str = structureTSCatalogList.get(i);
					tsident_string = HydroBase_WaterDistrict.formWDID( wdid_length,str.getWD(),str.getID())
						+ "." + str.getData_source()
						+ "." + DataType + "-" + str.getIdentifier()
						+ "." + Interval
						+ inputName;
				}
                else if ( is_CASSCrops ) {
                    cassCrops = cassCropStatsTSCatalogList.get(i);
                    tsident_string = cassCrops.getCounty()
                        + ".CASS" 
                        + "." + DataType + "-" + cassCrops.getCommodity() + "-" + cassCrops.getPractice()
                        + "." + Interval
                        + inputName;
                }
                else if ( is_CASSLivestock ) {
                    cassLivestock = cassLivestockStatsTSCatalogList.get(i);
                    tsident_string = cassLivestock.getCounty()
                        + ".CASS" 
                        + "." + DataType + "-" + cassLivestock.getCommodity() + "-" + cassLivestock.getType()
                        + "." + Interval
                        + inputName;
                }
                else if ( is_cuPop ) {
                    cuPop = cupopTSCatalogList.get(i);
                    tsident_string = cuPop.getArea_type() + "-" + cuPop.getArea_name()
                        + "" // Blank 
                        + "." + DataType + "-" + cuPop.getPop_type()
                        + "." + Interval
                        + inputName;
                }
                else if ( is_NASS ) {
                    nass = nassCropStatsTSCatalogList.get(i);
                    tsident_string = nass.getCounty()
                        + ".NASS" 
                        + "." + DataType + "-" + nass.getCommodity()
                        + "." + Interval
                        + inputName;
                }
				else if ( is_StructureIrrigSummaryTS ) {
					// Irrig summary TS...
					irrigts = irrigSummaryTSCatalogList.get(i);
					tsident_string =HydroBase_WaterDistrict.formWDID( wdid_length,irrigts.getWD(),irrigts.getID())
						+ ".CDSSGIS" 
						+ "." + DataType + "-" + irrigts.getLand_use()
						+ "." + Interval
						+ inputName;
				}
                else if ( is_Well ) {
                    // Well...
                    well = groundwaterWellsTSCatalogList.get(i);
                    String id;
                    if ( well.getIdentifier().length() > 0 ) {
                        // Well with a different identifier to display.
                        id = well.getIdentifier();
                    }
                    else if ( (well.getWD() > 0) && (well.getID() > 0) ) {
                        // A structure other than wells...
                        id = HydroBase_WaterDistrict.formWDID (wdid_length, well.getWD(), well.getID() );
                    }
                    else {
                        // A structure other than wells...
                        id = well.formatLatLongID();
                    }
                    tsident_string = id 
                        + "." + well.getData_source() 
                        + "." + DataType
                        + "." + Interval
                        + inputName;
                }
				else if ( is_WIS ) {
					// WIS TS...
					wis = wisTSCatalogList.get(i);
					tsident_string = wis.getIdentifier()
						+ ".DWR" 
						+ "." + DataType
						+ "." + Interval
						+ inputName;
				}
	            // Update the progress
				message = "Reading HydroBase time series " + (i + 1) + " of " + size + " \"" + tsident_string + "\"";
                notifyCommandProgressListeners ( i, size, (float)-1.0, message );
				try {
				    ts = hbdmi.readTimeSeries (
						tsident_string,
						InputStart_DateTime,
						InputEnd_DateTime, null, read_data,
						HydroBase_props );
					// Add the time series to the temporary list.  It will be further processed below...
	                if ( (ts != null) && (Alias != null) && !Alias.equals("") ) {
	                    ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString(
	                        processor, ts, Alias, status, command_phase) );
	                }
	                // Allow null to be added here.
					tslist.add ( ts );
				}
				catch ( Exception e ) {
					message = "Unexpected error reading HydroBase time series \"" + tsident_string + "\" (" + e + ").";
					Message.printWarning ( 2, routine, message );
					Message.printWarning ( 2, routine, e );
					++warning_count;
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report the problem to software support - also see the log file." ) );
				}
			}
		}
    
        int size = 0;
        if ( tslist != null ) {
            size = tslist.size();
        }
        Message.printStatus ( 2, routine, "Read " + size + " HydroBase time series." );

        if ( command_phase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing HydroBase time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
                        status.addToLog ( command_phase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding HydroBase time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,
                        ++warning_count), routine, message );
                        status.addToLog ( command_phase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
            }
        }
        else if ( command_phase == CommandPhaseType.DISCOVERY ) {
            setDiscoveryTSList ( tslist );
        }
        // Warn if nothing was retrieved (can be overridden to ignore).
        if ( (tslist == null) || (size == 0) ) {
            if ( IfMissingWarn ) {
                message = "No time series were read from HydroBase.";
                Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( command_phase,
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
		message ="Unexpected error reading time series from HydroBase (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
        status.addToLog ( command_phase,
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
    
    status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
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
@param props parameters for the command
*/
public String toString ( PropList props )
{
    return toString ( props, 10 );
}

/**
Return the string representation of the command.
@param props parameters for the command
@param majorVersion the major version for software - if less than 10, the "TS Alias = " notation is used,
allowing command files to be saved for older software.
*/
public String toString ( PropList props, int majorVersion )
{   if ( props == null ) {
        if ( majorVersion < 10 ) {
            return "TS Alias = " + getCommandName() + "()";
        }
        else {
            return getCommandName() + "()";
        }
    }
	StringBuffer b = new StringBuffer ();
	String Alias = props.getValue("Alias"); // Alias added at end
    String InputName = props.getValue("InputName");
    if ( (InputName != null) && (InputName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputName=\"" + InputName + "\"" );
    }
    String DataStore = props.getValue("DataStore");
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
	String TSID = props.getValue("TSID");
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
	if ( (TSID == null) || TSID.equals("") ) {
	    // The following need to be explicitly specified when not using the TSID
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
    if ( majorVersion >= 10 ) {
        // Add as a parameter
        if ( (Alias != null) && (Alias.length() > 0) ) {
            if ( b.length() > 0 ) {
                b.append ( "," );
            }
            b.append ( "Alias=\"" + Alias + "\"" );
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
	/* TODO SAM 2006-04-27 Code cleanup. As per Ray Bennett always do this so no optional parameter.
	*/
	String FillDivRecordsCarryForward = props.getValue("FillDivRecordsCarryForward");
	if ( (FillDivRecordsCarryForward != null) && (FillDivRecordsCarryForward.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillDivRecordsCarryForward=" + FillDivRecordsCarryForward );
	}
	String FillDivRecordsCarryForwardFlag = props.getValue("FillDivRecordsCarryForwardFlag");
	if ( (FillDivRecordsCarryForwardFlag != null) && (FillDivRecordsCarryForwardFlag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillDivRecordsCarryForwardFlag=" + FillDivRecordsCarryForwardFlag );
	}
	String FillUsingDivComments = props.getValue("FillUsingDivComments");
	if ( (FillUsingDivComments != null) && (FillUsingDivComments.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillUsingDivComments=" + FillUsingDivComments );
	}
	String FillUsingDivCommentsFlag = props.getValue("FillUsingDivCommentsFlag");
	if ( (FillUsingDivCommentsFlag != null) && (FillUsingDivCommentsFlag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillUsingDivCommentsFlag=\"" + FillUsingDivCommentsFlag + "\"" );
	}
    String IfMissing = props.getValue("IfMissing");
    if ( (IfMissing != null) && (IfMissing.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IfMissing=" + IfMissing );
    }
    if ( majorVersion < 10 ) {
        // Old syntax...
        if ( (Alias == null) || Alias.equals("") ) {
            Alias = "Alias";
        }
        return "TS " + Alias + " = " + getCommandName() + "("+ b.toString()+")";
    }
    else {
        return getCommandName() + "("+ b.toString()+")";
    }
}

}
