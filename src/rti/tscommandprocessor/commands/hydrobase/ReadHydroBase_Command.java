//------------------------------------------------------------------------------
// readHydroBase_Command - handle the readHydroBase() and
//				TS Alias = readHydroBase() commands
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2006-04-02	Steven A. Malers, RTi	* Initial version.  Copy and modify
//					  readStateMod().
//					* Enable parameters to fill daily
//					  diversion data.
//					* Enable parameters to fill diversions
//					  using comments.
// 2006-04-27	SAM, RTi		* As per Ray Bennett, fill with
//					  diversion comments should NOT be on
//					  by default.
//					* As per Ray Bennett, fill daily by
//					  carry forward should ALWAYS be done
//					  and not be an option - comment out the
//					  code in case it needs to be enabled
//					  later.
// 2007-02-11	SAM, RTi		Remove dependence on TSCommandProcessor.
//					Use the more generic CommandProcessor interface instead.
//					Clean up code based on Eclipse feedback.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.hydrobase;

import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

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
import RTi.Util.IO.CommandProgressListener;
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
import DWR.DMI.HydroBaseDMI.HydroBase_AgriculturalCASSCropStats;
import DWR.DMI.HydroBaseDMI.HydroBase_AgriculturalNASSCropStats;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_SheetNameWISFormat_InputFilter_JPanel;
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
    
    String TSID = parameters.getValue ( "TSID" );
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

	String InputStart = parameters.getValue ( "InputStart" );
	String InputEnd = parameters.getValue ( "InputEnd" );

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
	String FillDailyDiv = parameters.getValue ( "FillDailyDiv" );
	if ( (FillDailyDiv != null) && !FillDailyDiv.equals("") ) {
		if (	!FillDailyDiv.equalsIgnoreCase(_True) &&
			!FillDailyDiv.equalsIgnoreCase(_False) ) {
			warning +=
				"The FillDailyDiv parameter must be True " +
				"(blank) or False";
		}
	}
	String FillDailyDivFlag = parameters.getValue ( "FillDailyDivFlag" );
	if ( (FillDailyDivFlag != null) && (FillDailyDivFlag.length() != 1) ) {
		warning += "\nThe FillDailyDivFlag must be 1 character long.";
	}
	*/
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
    List<String> valid_Vector = new Vector();
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "TSID" );
    valid_Vector.add ( "DataType" );
    valid_Vector.add ( "Interval" );
    int numFilters = HydroBaseDMI.getSPFlexMaxParameters() - 2; // Maximum minus data type and interval
    for ( int i = 1; i <= numFilters; i++ ) { 
        valid_Vector.add ( "Where" + i );
    }
    valid_Vector.add ( "InputName" );
    valid_Vector.add ( "InputStart" );
    valid_Vector.add ( "InputEnd" );
    valid_Vector.add ( "FillUsingDivComments" );
    valid_Vector.add ( "FillUsingDivCommentsFlag" );
    valid_Vector.add ( "IfMissing" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

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
	int warning_count = 0;

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
    		++warning_count;
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
    		++warning_count;
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
    			++warning_count;
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
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ReadHydroBase_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
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
	String FillDailyDiv = _parameters.getValue ( "FillDailyDiv" );
	if (	(FillDailyDiv == null) || FillDailyDiv.equals("") ) {
		FillDailyDiv = _True;		// Default is to fill
	}
	String FillDailyDivFlag =
		_parameters.getValue ( "FillDailyDivFlag" );
	*/
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
	HydroBase_props.set ( "FillDailyDiv=" + FillDailyDiv );
	if ( (FillDailyDivFlag != null) && !FillDailyDivFlag.equals("") ) {
		HydroBase_props.set ( "FillDailyDivFlag=" + FillDailyDivFlag );
	}
	*/
	HydroBase_props.set ( "FillUsingDivComments=" + FillUsingDivComments );
	if ( (FillUsingDivCommentsFlag != null) &&!FillUsingDivCommentsFlag.equals("")){
		HydroBase_props.set ( "FillUsingDivCommentsFlag=" + FillUsingDivCommentsFlag );
	}

	// Now try to read...

	List<TS> tslist = new Vector();	// List for time series results.
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
			// Find the HydroBaseDMI to use...
			Object o = processor.getPropContents ("HydroBaseDMIList" );
			if ( o == null ) {
				message = "Could not get list of HydroBase connections to query data.";
				Message.printWarning ( 2, routine, message );
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that a HydroBase database connection has been opened." ) );
				throw new Exception ( message );
			}
			List<HydroBaseDMI> hbdmi_Vector = (List<HydroBaseDMI>)o;
			HydroBaseDMI hbdmi = HydroBase_Util.lookupHydroBaseDMI ( hbdmi_Vector, tsident.getInputName() );
			if ( hbdmi == null ) {
				message = "Could not find HydroBase connection with input name \"" +
                tsident.getInputName() + "\" to query data.";
				Message.printWarning ( 2, routine, message );
                status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that a HydroBase connection with the given name has been opened." ) );
				throw new Exception ( message );
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
                   throw new Exception ( message );
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
			List<String> WhereN_Vector = new Vector ( 6 );
			String WhereN;
			int nfg = 0;	// Used below.
			for ( nfg = 0; nfg < 1000; nfg++ ) {
				WhereN = parameters.getValue ( "Where" + (nfg + 1) );
				if ( WhereN == null ) {
					break;	// No more where clauses
				}
				WhereN_Vector.add ( WhereN );
			}
		
			// Find the HydroBaseDMI to use...
			Object o = processor.getPropContents ( "HydroBaseDMIList" );
			if ( o == null ) {
				message = "Could not get list of HydroBase connections to query data.";
				Message.printWarning ( 2, routine, message );
                status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that a HydroBase database connection has been opened." ) );
				throw new Exception ( message );
			}
			List<HydroBaseDMI> hbdmi_Vector = (List<HydroBaseDMI>)o;
			HydroBaseDMI hbdmi = HydroBase_Util.lookupHydroBaseDMI ( hbdmi_Vector, InputName );
			if ( hbdmi == null ) {
				message ="Could not find HydroBase connection with input name \"" + InputName + "\" to query data.";
				Message.printWarning ( 2, routine, message );
                status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that a HydroBase database connection has been opened." ) );
				throw new Exception ( message );
			}

			// Initialize an input filter based on the data type...

			InputFilter_JPanel filter_panel = null;
			boolean is_CASS = false;
			boolean is_NASS = false;
			boolean is_Station = false;
			boolean is_Structure = false;
			boolean is_StructureSFUT = false;
			boolean is_StructureIrrigSummaryTS = false;
			boolean is_SheetName = false;

			int wdid_length = HydroBase_Util.getPreferredWDIDLength();

			// Create the input filter panel...

			if ( HydroBase_Util.isStationTimeSeriesDataType ( hbdmi, DataType ) ){
				// Stations...
				is_Station = true;
				filter_panel = new HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel ( hbdmi );
				Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for station." );
			}
			else if ( HydroBase_Util.isStructureSFUTTimeSeriesDataType ( hbdmi, DataType ) ) {
				// Structures (with SFUT)...
				is_StructureSFUT = true;
				Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for structure SFUT." );
				filter_panel = new HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
					hbdmi, true, 6, -1 );
			}
			else if ( HydroBase_Util.isStructureTimeSeriesDataType (hbdmi, DataType ) ) {
				// Structures (no SFUT)...
				is_Structure = true;
				filter_panel = new HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
					hbdmi, false, 6, -1 );
				Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for structure (no SFUT)." );
			}
			else if ( HydroBase_Util.isIrrigSummaryTimeSeriesDataType(hbdmi, DataType ) ) {
				// Irrig summary TS...
				is_StructureIrrigSummaryTS = true;
				filter_panel = new HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel ( hbdmi );
				Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for structure irrig summary ts." );
			}
			else if ( HydroBase_Util.isAgriculturalCASSCropStatsTimeSeriesDataType ( hbdmi, DataType) ) {
				is_CASS = true;
				filter_panel = new
				HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel (hbdmi );
				Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for CASS." );
			}
			else if ( HydroBase_Util.isAgriculturalNASSCropStatsTimeSeriesDataType (	hbdmi, DataType ) ) {
				// Data from agricultural_CASS_crop_statistics
				// or agricultural_NASS_crop_statistics...
				is_NASS = true;
				filter_panel = new
				HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel (hbdmi );
				Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for NASS." );
			}
			else if( HydroBase_Util.isWISTimeSeriesDataType (hbdmi, DataType ) ) {
				// Sheet name...
				is_SheetName = true;
				filter_panel = new
				HydroBase_GUI_SheetNameWISFormat_InputFilter_JPanel ( hbdmi );
				Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for WIS." );
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
				WhereN = (String)WhereN_Vector.get(ifg);
                if ( WhereN.length() == 0 ) {
                    continue;
                }
				// Set the filter...
				try {
                    filter_panel.setInputFilter( ifg, WhereN, filter_delim );
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
		
			List tslist0 = HydroBase_Util.readTimeSeriesHeaderObjects ( hbdmi, DataType, Interval, filter_panel );
			// Make sure that size is set...
			int size = 0;
			if ( tslist0 != null ) {
				size = tslist0.size();
			}
		
       		if ( (tslist0 == null) || (size == 0) ) {
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
			HydroBase_AgriculturalCASSCropStats cass;
			HydroBase_AgriculturalNASSCropStats nass;
			HydroBase_WISSheetNameWISFormat wis;
			HydroBase_StructureGeolocStructMeasType str;
			HydroBase_StructureIrrigSummaryTS irrigts;
			HydroBase_StationGeolocMeasType sta;
			String input_name = "";	// Input name to add to identifiers, if it has been
						// requested (this includes the tilde character).
			if ( (InputName != null) && (InputName.length() > 0) ) {
				// Include the input name in the returned TSIdent...
				input_name = "~" + InputName;
			}
			for ( int i = 0; i < size; i++ ) {
				// List in order of likelihood...
				if ( is_Station ) {
					// Station TS...
					sta = (HydroBase_StationGeolocMeasType)tslist0.get(i);
					tsident_string = 
						sta.getStation_id()
						+ "." + sta.getData_source()
						+ "." + DataType + "." + Interval
						+ "~HydroBase" + input_name;
				}
				else if ( is_Structure ) {
					str = (HydroBase_StructureGeolocStructMeasType)tslist0.get(i);
					tsident_string = HydroBase_WaterDistrict.formWDID( wdid_length,str.getWD(),str.getID())
						+ "." + str.getData_source()
						+ "." + DataType + "." + Interval
						+ "~HydroBase" + input_name;
				}
				else if ( is_StructureSFUT ) {
					str = (HydroBase_StructureGeolocStructMeasType)tslist0.get(i);
					tsident_string = HydroBase_WaterDistrict.formWDID( wdid_length,str.getWD(),str.getID())
						+ "." + str.getData_source()
						+ "." + DataType + "-"
						+ str.getIdentifier() + "." +
						Interval + "~HydroBase" + input_name;
				}
				else if ( is_StructureIrrigSummaryTS ) {
					// Irrig summary TS...
					irrigts = (HydroBase_StructureIrrigSummaryTS)tslist0.get(i);
					tsident_string =HydroBase_WaterDistrict.formWDID( wdid_length,irrigts.getWD(),irrigts.getID())
						+ ".CDSSGIS" 
						+ "." + DataType + "-"
						+ irrigts.getLand_use()
						+ "." + Interval
						+ "~HydroBase" + input_name;
				}
				else if ( is_CASS ) {
					cass = (HydroBase_AgriculturalCASSCropStats)tslist0.get(i);
					tsident_string = cass.getCounty()
						+ ".CASS" 
						+ "." + DataType + "-"
						+ cass.getCommodity() + "-"
						+ cass.getPractice()
						+ "." + Interval
						+ "~HydroBase" + input_name;
				}
				else if ( is_NASS ) {
					nass = (HydroBase_AgriculturalNASSCropStats)tslist0.get(i);
					tsident_string = nass.getCounty()
						+ ".NASS" 
						+ "." + DataType + "-"
						+ nass.getCommodity()
						+ "." + Interval
						+ "~HydroBase" + input_name;
				}
				else if ( is_SheetName ) {
					// WIS TS...
					wis = (HydroBase_WISSheetNameWISFormat)tslist0.get(i);
					tsident_string = wis.getIdentifier()
						+ ".DWR" 
						+ "." + DataType
						+ "." + Interval
						+ "~HydroBase" + input_name;
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
	                if ( Alias != null ) {
	                    ts.setAlias ( TSCommandProcessorUtil.expandTimeSeriesMetadataString(
	                        processor, ts, Alias, status, command_phase) );
	                }
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
private void setDiscoveryTSList ( List discovery_TS_Vector )
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
	String InputName = props.getValue("InputName");
	if ( (InputName != null) && (InputName.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputName=\"" + InputName + "\"" );
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
	String FillDailyDiv = props.getValue("FillDailyDiv");
	if ( (FillDailyDiv != null) && (FillDailyDiv.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillDailyDiv=" + FillDailyDiv );
	}
	String FillDailyDivFlag = props.getValue("FillDailyDivFlag");
	if ( (FillDailyDivFlag != null) && (FillDailyDivFlag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillDailyDivFlag=\"" + FillDailyDivFlag + "\"" );
	}
	*/
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