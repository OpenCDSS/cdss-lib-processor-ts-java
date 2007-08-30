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

import java.util.Vector;

import javax.swing.JFrame;

import RTi.TS.TS;
import RTi.TS.TSIdent;

import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.SkeletonCommand;
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
<p>
This class initializes, checks, and runs the readHydroBase() command.
</p>
<p>The CommandProcessor must return the following properties:
TSResultsList.
</p>
*/
public class readHydroBase_Command extends SkeletonCommand implements Command
{

// Data used here and in the dialog.

protected String _False = "False";
protected String _True = "True";

// Indicates whether the TS Alias version of the command is being used...

protected boolean _use_alias = false;

/**
Constructor.
*/
public readHydroBase_Command ()
{	super();
	setCommandName ( "readHydroBase" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor
dialogs).
*/
public void checkCommandParameters (	PropList parameters, String command_tag,
					int warning_level )
throws InvalidCommandParameterException
{	String warning = "";
	if ( _use_alias ) {
		String Alias = parameters.getValue ( "Alias" );
		if ( (Alias == null) || Alias.equals("") ) {
			warning += 
			"\nA time series alias must be specified.";
		}
		String TSID = parameters.getValue ( "TSID" );
		if ( (TSID == null) || TSID.equals("") ) {
			warning += 
			"\nA time series identifier must be specified.";
		}
		else {	// Check the parts of the TSID...
			TSIdent tsident = null;
			try {	tsident = new TSIdent ( TSID );
				String Location = tsident.getLocation();
				String DataSource = tsident.getSource();
				String DataType = tsident.getType();
				String Interval = tsident.getInterval();
				if ( Location.length() == 0 ) {
					warning +=
					"\nThe location must be specified.";
				}
				if ( DataSource.length() == 0 ) {
					warning +=
					"\nThe data source must be specified.";
				}
				if ( DataType.length() == 0 ) {
					warning +=
					"\nThe data type must be specified.";
				}
				if ( Interval.length() == 0 ) {
					warning +=
					"\nThe interval must be specified.";
				}
				else {	// REVISIT SAM 2006-04-25
					// Most likely the following will not be
					// executed because parsing the TSID
					// will generate an
					// InvalidTimeIntervalException (caught
					// below).  This may cause the user to
					// do two checks to catch all input
					// errors.
					try { TimeInterval.parseInterval (
						Interval);
					}
					catch ( Exception e ) {
						warning +=
						"\nThe data interval \"" +
						Interval + "\" is invalid";
					}
				}
			}
			catch ( InvalidTimeIntervalException e ) {
				// Will not even be able to print it...
				warning +=
					"\nThe data interval is invalid";
			}
			catch ( Exception e ) {
				warning +=
				"Unable to check TSID parts for correctness " +
				"due to command syntax error.";
			}
		}
		// InputName is optional.
	}
	else {	String DataType = parameters.getValue ( "DataType" );
		if ( (DataType == null) || (DataType.length() == 0) ) {
			warning += "\nThe data type must be specified.";
		}
		String Interval = parameters.getValue ( "Interval" );
		if ( (Interval == null) || (Interval.length() == 0) ) {
			warning += "\nThe data interval must be specified.";
		}
		else {	try { TimeInterval.parseInterval (Interval);
			}
			catch ( Exception e ) {
				warning += "\nThe data interval \"" +
					Interval + "\" is invalid";
			}
		}
		// InputName is optional.
		// REVISIT SAM 2006-04-24
		// Need to check the WhereN parameters.
	}

	// Used with both versions of the command...

	String InputStart = parameters.getValue ( "InputStart" );
	String InputEnd = parameters.getValue ( "InputEnd" );

	if (	(InputStart != null) && !InputStart.equals("") &&
		!InputStart.equalsIgnoreCase("InputStart") &&
		!InputStart.equalsIgnoreCase("InputEnd") ) {
		try {	DateTime.parse(InputStart);
		}
		catch ( Exception e ) {
			warning += 
				"\nThe input start date/time \"" + InputStart +
				"\" is not a valid date/time.\n"+
				"Specify a date/time or InputStart.";
		}
	}
	if (	(InputEnd != null) && !InputEnd.equals("") &&
		!InputEnd.equalsIgnoreCase("InputStart") &&
		!InputEnd.equalsIgnoreCase("InputEnd") ) {
		try {	DateTime.parse( InputEnd );
		}
		catch ( Exception e ) {
			warning +=
				"\nThe input end date/time \"" + InputEnd +
				"\" is not a valid date/time.\n"+
				"Specify a date/time or InputEnd.";
		}
	}

	/* REVISIT SAM 2006-04-27
	As per Ray Bennett always do this.
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
	String FillUsingDivComments =
		parameters.getValue ( "FillUsingDivComments" );
	if ((FillUsingDivComments != null) && !FillUsingDivComments.equals("")){
		if (	!FillUsingDivComments.equalsIgnoreCase(_True) &&
			!FillUsingDivComments.equalsIgnoreCase(_False) ) {
			warning +=
				"The FillUsingDivComments parameter must be " +
				"True (blank) or False";
		}
	}
	String FillUsingDivCommentsFlag =
		parameters.getValue ( "FillUsingDivCommentsFlag" );
	if (	(FillUsingDivCommentsFlag != null) &&
		(FillUsingDivCommentsFlag.length() != 1) ) {
		warning +=
		"\nThe FillUsingDivCommentsFlag must be 1 character long.";
	}

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),
		warning );
		throw new InvalidCommandParameterException ( warning );
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
	return (new readHydroBase_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.
@param command_string A string command to parse.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
@exception InvalidCommandSyntaxException if during parsing the command is
determined to have invalid syntax.
syntax of the command are bad.
@exception InvalidCommandParameterException if during parsing the command
parameters are determined to be invalid.
*/
public void parseCommand (	String command_string, String command_tag,
				int warning_level )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	int warning_count = 0;
	String routine = "readHydroBase_Command.parseCommand", message;

	Vector tokens = StringUtil.breakStringList ( command_string,
		"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the command name and something to indicate
		// the read...
		message = "Syntax error in \"" + command_string +
				"\".  Not enough tokens.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag,++warning_count),
			routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}

	// Parse everything after the (, which should be command parameters...
	
	try {	_parameters = PropList.parse ( Prop.SET_FROM_PERSISTENT,
			(String)tokens.elementAt(1), routine, "," );
	}
	catch ( Exception e ) {
		message = "Syntax error in \"" + command_string +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
			command_tag,++warning_count),
			routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}

	// Evaluate whether the command is a TS Alias version...

	String Alias = null;
	_use_alias = false;
	if (StringUtil.startsWithIgnoreCase(command_string, "TS ")) {
		// There is an alias specified.  Extract the alias from the full
		// command...
		String str = command_string.substring(3);	
		int index = str.indexOf("=");
		int index2 = str.indexOf("(");
		if (index2 < index) {
			// No alias specified -- badly-formed command
			message = "No alias was specified, although "
				+ "the command started with \"TS ...\"";
			Message.printWarning(warning_level, 
				MessageUtil.formatMessageTag(
				command_tag, ++warning_count),
				routine, message);
			throw new InvalidCommandSyntaxException(
				message);
		}

		Alias = str.substring(0, index);
	}
	else {
		Alias = null;
	}

	if ( Alias != null ) {
		_parameters.set ( "Alias", Alias.trim() );
		_use_alias = true;
	}

	// Convert QueryStart and QueryEnd to new syntax InputStart and
	// InputEnd...
	String QueryStart = _parameters.getValue ( "QueryStart" );
	if ( (QueryStart != null) && (QueryStart.length() > 0) ) {
		_parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		_parameters.set ( "InputStart", QueryStart );
		_parameters.unSet ( QueryStart );
		_parameters.setHowSet ( Prop.SET_UNKNOWN );
	}
	String QueryEnd = _parameters.getValue ( "QueryEnd" );
	if ( (QueryEnd != null) && (QueryEnd.length() > 0) ) {
		_parameters.setHowSet ( Prop.SET_FROM_PERSISTENT );
		_parameters.set ( "InputEnd", QueryEnd );
		_parameters.unSet ( QueryStart );
		_parameters.setHowSet ( Prop.SET_UNKNOWN );
	}
}
	
/**
Run the commands:
<pre>
readHydroBase(TSID="X",InputStart="X",InputEnd="X")

TS Alias = readHydroBase(TSID="X",InputStart="X",InputEnd="X")
</pre>
@param processor The CommandProcessor that is executing the command, which will
provide necessary data inputs and receive output(s).
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( String command_tag, int warning_level )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "readHydroBase_Command.runCommand", message;
	int warning_count = 0;

	String InputStart = _parameters.getValue ( "InputStart" );
	DateTime InputStart_DateTime = null;
	if ( InputStart != null ) {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", InputStart );
		CommandProcessorRequestResultsBean bean = null;
		try { bean =
			_processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, "Error requesting DateTime(DateTime=" + InputStart +
					"\" from processor." );
		}
		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, "Null value for DateTime(DateTime=" + InputStart +
				"\") returned from processor." );
		}
		else {	InputStart_DateTime = (DateTime)prop_contents;
		}
	}
	else {	// Get from the processor...
		try { Object o = _processor.getPropContents ( "InputStart" );
			if ( o != null ) {
				InputStart_DateTime = (DateTime)o;
			}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting InputStart from processor - not using.";
			Message.printDebug(10, routine, message );
		}
	}
	String InputEnd = _parameters.getValue ( "InputEnd" );
	DateTime InputEnd_DateTime = null;
	if ( InputEnd != null ) {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", InputEnd );
		CommandProcessorRequestResultsBean bean = null;
		try { bean =
			_processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, "Error requesting DateTime(DateTime=" + InputEnd +
					"\" from processor." );
		}
		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, "Null value for DateTime(DateTime=" + InputEnd +
				"\") returned from processor." );
		}
		else {	InputEnd_DateTime = (DateTime)prop_contents;
		}
	}
	else {	// Get from the processor...
		try { Object o = _processor.getPropContents ( "InputEnd" );
			if ( o != null ) {
				InputEnd_DateTime = (DateTime)o;
			}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting InputEnd from processor - not using.";
			Message.printDebug(10, routine, message );
		}
	}

	/* REVISIT SAM 2006-04-27
	As per Ray Bennett always do this.
	String FillDailyDiv = _parameters.getValue ( "FillDailyDiv" );
	if (	(FillDailyDiv == null) || FillDailyDiv.equals("") ) {
		FillDailyDiv = _True;		// Default is to fill
	}
	String FillDailyDivFlag =
		_parameters.getValue ( "FillDailyDivFlag" );
	*/
	String FillUsingDivComments = _parameters.getValue (
		"FillUsingDivComments" );
	if (	(FillUsingDivComments == null) ||
		FillUsingDivComments.equals("") ) {
		FillUsingDivComments = _False;	// Default is NOT to fill
	}
	String FillUsingDivCommentsFlag =
		_parameters.getValue ( "FillUsingDivCommentsFlag" );

	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Set up properties for the read...

	PropList HydroBase_props = new PropList ( "HydroBase" );
	/* REVISIT SAM 2006-04-27
	As per Ray Bennett always do this.
	HydroBase_props.set ( "FillDailyDiv=" + FillDailyDiv );
	if ( (FillDailyDivFlag != null) && !FillDailyDivFlag.equals("") ) {
		HydroBase_props.set ( "FillDailyDivFlag=" + FillDailyDivFlag );
	}
	*/
	HydroBase_props.set ( "FillUsingDivComments=" + FillUsingDivComments );
	if (	(FillUsingDivCommentsFlag != null) &&
		!FillUsingDivCommentsFlag.equals("")){
		HydroBase_props.set ( "FillUsingDivCommentsFlag=" +
		FillUsingDivCommentsFlag );
	}

	// Now try to read...

	Vector tslist = new Vector();	// Vector for time series results.
					// Will be added to for one time series
					// read or replaced if a list is read.
	try {	if ( _use_alias ) {
			// TS Alias = version....
			String Alias = _parameters.getValue ( "Alias" );
			String TSID = _parameters.getValue ( "TSID" );

			Message.printStatus ( 2, routine,
			"Reading HydroBase time series \"" + TSID + "\"" );
			TS ts = null;
			TSIdent tsident = new TSIdent ( TSID );
			// Find the HydroBaseDMI to use...
			Object o = _processor.getPropContents (
				"HydroBaseDMIList" );
			if ( o == null ) {
				message = "Could not get list of HydroBase " +
					"connections to query data.";
				Message.printWarning ( 2, routine, message );
				throw new Exception ( message );
			}
			Vector hbdmi_Vector = (Vector)o;
			HydroBaseDMI hbdmi =
				HydroBase_Util.lookupHydroBaseDMI (
					hbdmi_Vector, tsident.getInputName() );
			if ( hbdmi == null ) {
				message ="Could not find HydroBase connection "+
				"with input name \"" + tsident.getInputName() +
				"\" to query data.";
				Message.printWarning ( 2, routine, message );
				throw new Exception ( message );
			}
			try {	ts = hbdmi.readTimeSeries (
					TSID, InputStart_DateTime,
					InputEnd_DateTime, null,
					true, HydroBase_props );
			}
			catch ( Exception e ) {
				message = "Error reading HydroBase time " +
					"series \"" + TSID + "\".";
				Message.printWarning ( 2, routine, message );
				Message.printWarning ( 2, routine, e );
				throw new Exception ( message );
			}
			if ( ts != null ) {
				// Set the alias...
				ts.setAlias ( Alias );
				tslist.addElement ( ts );
			}
		}
		else {	// Read 1+ time series...
			// Get the input needed to process the file...
			String DataType = _parameters.getValue ( "DataType" );
			String Interval = _parameters.getValue ( "Interval" );
			String InputName = _parameters.getValue ( "InputName" );
			if ( InputName == null ) {
				InputName = "";
			}
			Vector WhereN_Vector = new Vector ( 6 );
			String WhereN;
			int nfg = 0;	// Used below.
			for ( nfg = 0; nfg < 1000; nfg++ ) {
				WhereN = _parameters.getValue (
					"Where" + (nfg + 1) );
				if ( WhereN == null ) {
					break;	// No more where clauses
				}
				WhereN_Vector.addElement ( WhereN );
			}
		
			// Find the HydroBaseDMI to use...
			Object o = _processor.getPropContents (
				"HydroBaseDMIList" );
			if ( o == null ) {
				message = "Could not get list of HydroBase " +
					"connections to query data.";
				Message.printWarning ( 2, routine, message );
				throw new Exception ( message );
			}
			Vector hbdmi_Vector = (Vector)o;
			HydroBaseDMI hbdmi =
				HydroBase_Util.lookupHydroBaseDMI (
					hbdmi_Vector, InputName );
			if ( hbdmi == null ) {
				message ="Could not find HydroBase connection "+
				"with input name \"" + InputName +
				"\" to query data.";
				Message.printWarning ( 2, routine, message );
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

			int wdid_length =
				HydroBase_Util.getPreferredWDIDLength();

			// Create the input filter panel...

			if (	HydroBase_Util.isStationTimeSeriesDataType (
				hbdmi, DataType ) ){
				// Stations...
				is_Station = true;
				filter_panel = new
					HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel (
					hbdmi );
				Message.printStatus ( 2, routine, "Data type \""
					+ DataType + "\" is for station." );
			}
			else if ( HydroBase_Util.
				isStructureSFUTTimeSeriesDataType (
				hbdmi, DataType ) ) {
				// Structures (with SFUT)...
				is_StructureSFUT = true;
				PropList filter_props = new PropList ( "" );
				filter_props.set ( "NumFilterGroups=6" );
				Message.printStatus ( 2, routine,
					"Data type \"" + DataType +
					"\" is for structure SFUT." );
				filter_panel = new
				HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
					hbdmi, true, filter_props );
			}
			else if ( HydroBase_Util.isStructureTimeSeriesDataType (
				hbdmi, DataType ) ) {
				// Structures (no SFUT)...
				is_Structure = true;
				PropList filter_props = new PropList ( "" );
				filter_props.set ( "NumFilterGroups=6" );
				filter_panel = new
				HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
					hbdmi, false, filter_props );
				Message.printStatus ( 2, routine,
					"Data type \"" + DataType +
					"\" is for structure (no SFUT)." );
			}
			else if ( HydroBase_Util.
				isIrrigSummaryTimeSeriesDataType(
				hbdmi, DataType ) ) {
				// Irrig summary TS...
				is_StructureIrrigSummaryTS = true;
				filter_panel = new
				HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel (
					hbdmi );
				Message.printStatus ( 2, routine,
					"Data type \"" + DataType +
					"\" is for structure irrig " +
					"summary ts." );
			}
			else if ( HydroBase_Util.isAgriculturalCASSCropStatsTimeSeriesDataType (
				hbdmi, DataType) ) {
				is_CASS = true;
				filter_panel = new
				HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel (
					hbdmi );
				Message.printStatus ( 2, routine,
					"Data type \"" + DataType +
					"\" is for CASS." );
			}
			else if ( HydroBase_Util.
				isAgriculturalNASSCropStatsTimeSeriesDataType (
				hbdmi, DataType ) ) {
				// Data from agricultural_CASS_crop_statistics
				// or agricultural_NASS_crop_statistics...
				is_NASS = true;
				filter_panel = new
				HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel (
					hbdmi );
				Message.printStatus ( 2, routine,
					"Data type \"" + DataType +
					"\" is for NASS." );
			}
			else if( HydroBase_Util.isWISTimeSeriesDataType (
				hbdmi, DataType ) ) {
				// Sheet name...
				is_SheetName = true;
				filter_panel = new
				HydroBase_GUI_SheetNameWISFormat_InputFilter_JPanel ( hbdmi );
				Message.printStatus ( 2, routine,
					"Data type \"" + DataType +
					"\" is for WIS." );
			}
			else {	message = "Data type \"" + DataType + "\" is " +
				"not recognized as a HydroBase data type.";
				Message.printWarning ( 2, routine, message );
				throw new Exception ( message );
			}

			// Populate with the where information from the
			// command...

			String filter_delim = ";";
			for ( int ifg = 0; ifg < nfg; ifg ++ ) {
				WhereN = (String)WhereN_Vector.elementAt(ifg); 
				// Set the filter...
				try {	filter_panel.setInputFilter( ifg,
					WhereN, filter_delim);
				}
				catch ( Exception e ) {
					Message.printWarning ( 2, routine,
					"Error setting where information " +
					"using \""+WhereN+"\"");
					Message.printWarning ( 2, routine, e );
					++warning_count;
				}
			}
		
			// Read the list of objects from which identifiers can
			// be obtained.  This code is similar to that in
			// TSTool_JFrame.readHydroBaseHeaders...
		
			Message.printStatus ( 2, "",
				"Getting the list of time series..." );
		
			Vector tslist0 = HydroBase_Util.
					readTimeSeriesHeaderObjects ( hbdmi,
					DataType, Interval, filter_panel );
			// Make sure that size is set...
			int size = 0;
			if ( tslist0 != null ) {
				size = tslist0.size();
			}
		
       			if ( (tslist0 == null) || (size == 0) ) {
				Message.printStatus ( 2, routine,
				"No HydroBase time series were found." );
				return;
       			}
		
			// Else, convert each header object to a TSID string and
			// read the time series...

			Message.printStatus ( 2, "", "Reading " + size +
				" time series..." );

			String tsident_string = null;	// TSIdent string
			TS ts;				// Time series to read.
			HydroBase_AgriculturalCASSCropStats cass;
			HydroBase_AgriculturalNASSCropStats nass;
			HydroBase_WISSheetNameWISFormat wis;
			HydroBase_StructureGeolocStructMeasType str;
			HydroBase_StructureIrrigSummaryTS irrigts;
			HydroBase_StationGeolocMeasType sta;
			String input_name = "";	// Input name to add to
						// identifiers, if it has been
						// requested (this includes the
						// tilde character).
			if ( (InputName != null) && (InputName.length() > 0) ) {
				// Include the input name in the returned
				// TSIdent...
				input_name = "~" + InputName;
			}
			for ( int i = 0; i < size; i++ ) {
				// List in order of likelihood...
				if ( is_Station ) {
					// Station TS...
					sta = (HydroBase_StationGeolocMeasType)
						tslist0.elementAt(i);
					tsident_string = 
						sta.getStation_id()
						+ "." + sta.getData_source()
						+ "." + DataType + "." +Interval
						+ "~HydroBase" + input_name;
				}
				else if ( is_Structure ) {
					str = (HydroBase_StructureGeolocStructMeasType)
						tslist0.elementAt(i);
					tsident_string =
						HydroBase_WaterDistrict.
						formWDID( wdid_length,
						str.getWD(),str.getID())
						+ "." + str.getData_source()
						+ "." + DataType + "." +Interval
						+ "~HydroBase" + input_name;
				}
				else if ( is_StructureSFUT ) {
					str = (HydroBase_StructureGeolocStructMeasType)
						tslist0.elementAt(i);
					tsident_string =
						HydroBase_WaterDistrict.
						formWDID( wdid_length,
						str.getWD(),str.getID())
						+ "." + str.getData_source()
						+ "." + DataType + "-"
						+ str.getIdentifier() + "." +
						Interval
						+ "~HydroBase" + input_name;
				}
				else if ( is_StructureIrrigSummaryTS ) {
					// Irrig summary TS...
					irrigts = (HydroBase_StructureIrrigSummaryTS)
						tslist0.elementAt(i);
					tsident_string =HydroBase_WaterDistrict.
						formWDID( wdid_length,
						irrigts.getWD(),irrigts.getID())
						+ ".CDSSGIS" 
						+ "." + DataType + "-"
						+ irrigts.getLand_use()
						+ "." + Interval
						+ "~HydroBase" + input_name;
				}
				else if ( is_CASS ) {
					cass = (HydroBase_AgriculturalCASSCropStats)
						tslist0.elementAt(i);
					tsident_string = cass.getCounty()
						+ ".CASS" 
						+ "." + DataType + "-"
						+ cass.getCommodity() + "-"
						+ cass.getPractice()
						+ "." + Interval
						+ "~HydroBase" + input_name;
				}
				else if ( is_NASS ) {
					nass = (HydroBase_AgriculturalNASSCropStats)
						tslist0.elementAt(i);
					tsident_string = nass.getCounty()
						+ ".NASS" 
						+ "." + DataType + "-"
						+ nass.getCommodity()
						+ "." + Interval
						+ "~HydroBase" + input_name;
				}
				else if ( is_SheetName ) {
					// WIS TS...
					wis = (HydroBase_WISSheetNameWISFormat)
						tslist0.elementAt(i);
					tsident_string = wis.getIdentifier()
						+ ".DWR" 
						+ "." + DataType
						+ "." + Interval
						+ "~HydroBase" + input_name;
				}
	
				Message.printStatus ( 2, "",
					"Reading time series for \"" +
					tsident_string + "\"..." );
				try {	ts = hbdmi.readTimeSeries (
						tsident_string,
						InputStart_DateTime,
						InputEnd_DateTime, null, true,
						HydroBase_props );
					// Add the time series to the temporary
					// list.  It will be further processed
					// below...
					tslist.addElement ( ts );
				}
				catch ( Exception e ) {
					message = "Error reading HydroBase " +
						"time series \"" +
						tsident_string + "\".";
					Message.printWarning ( 2, routine,
					message );
					Message.printWarning ( 2, routine, e );
					++warning_count;
				}
			}
		}

		// Further process the time seies and add the time series to the
		// list being managed in the command processor...

		Object o = null;
		o = _processor.getPropContents ( "TSResultsList");
		if ( o == null ) {
			message = "Unable to get list of time series.  Can't add to list.";
			Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, message );
			throw new CommandException ( message );
		}
		Vector v = (Vector)o;
		int vsize = 0;	// Existing list size.
		if ( v != null ) {
			vsize = v.size();
		}

		// Further process the time series...
		// This makes sure the period is at least as long as the
		// output period...
		int size = 0;
		if ( tslist != null ) {
			size = tslist.size();
		}
		Message.printStatus ( 2, routine,
		"Read " + size + " HydroBase time series." );
		try {	
			PropList request_params = new PropList ( "" );
			request_params.setUsingObject ( "TSList", tslist );
			_processor.processRequest( "ReadTimeSeries2", request_params);
		}
		catch ( Exception e ) {
			message =
			"Error processing time series after read.";
			Message.printWarning ( warning_level, 
			MessageUtil.formatMessageTag(command_tag,
			++warning_count), routine, message );
			throw new CommandException ( message );
		}
		for ( int i = 0; i < size; i++ ) {
			PropList request_params = new PropList ( "" );
			request_params.setUsingObject ( "TS", tslist.elementAt(i) );
			// Index is zero based...
			request_params.setUsingObject ( "Index", new Integer(vsize + i) );
			try {
				_processor.processRequest( "SetTimeSeries", request_params);
			}
			catch ( Exception e ) {
				Message.printWarning(warning_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, "Error requesting SetTimeSeries(Index=" + (vsize + 1) +
						"\") from processor." );
				Message.printWarning ( 3, routine, e );
			}
		}
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error reading time series from HydroBase.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new CommandException ( message );
	}

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count +
			" warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		throw new CommandWarningException ( message );
	}
}

/**
Set whether the alias version of the command is being used.  This method can
be called from an edit dialog in cases where parseCommand() may not have been
called at startup.
@param use_alias Indicate whether an alias is being used with the command.
*/
protected void setUseAlias ( boolean use_alias )
{	_use_alias = use_alias;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	StringBuffer b = new StringBuffer ();
	if ( props == null ) {
		if ( _use_alias ) {
			return "TS Alias = " + getCommandName() + "()";
		}
		else {	return getCommandName() + "()";
		}
	}
	if ( _use_alias ) {
		String TSID = props.getValue("TSID");
		if ( (TSID != null) && (TSID.length() > 0) ) {
			if ( b.length() > 0 ) {
				b.append ( "," );
			}
			b.append ( "TSID=\"" + TSID + "\"" );
		}
	}
	else {	String DataType = props.getValue("DataType");
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
		String InputName = props.getValue("InputName");
		if ( (InputName != null) && (InputName.length() > 0) ) {
			if ( b.length() > 0 ) {
				b.append ( "," );
			}
			b.append ( "InputName=\"" + InputName + "\"" );
		}
		String delim = ";";
		String Where1 = props.getValue("Where1");
		if ( (Where1 != null) && (Where1.length() > 0) &&
			!Where1.startsWith(delim) ) {
			if ( b.length() > 0 ) {
				b.append ( "," );
			}
			b.append ( "Where1=\"" + Where1 + "\"" );
		}
		String Where2 = props.getValue("Where2");
		if ( (Where2 != null) && (Where2.length() > 0) &&
			!Where2.startsWith(delim) ) {
			if ( b.length() > 0 ) {
				b.append ( "," );
			}
			b.append ( "Where2=\"" + Where2 + "\"" );
		}
		String Where3 = props.getValue("Where3");
		if ( (Where3 != null) && (Where3.length() > 0) &&
			!Where3.startsWith(delim) ) {
			if ( b.length() > 0 ) {
				b.append ( "," );
			}
			b.append ( "Where3=\"" + Where3 + "\"" );
		}
		String Where4 = props.getValue("Where4");
		if ( (Where4 != null) && (Where4.length() > 0) &&
			!Where4.startsWith(delim) ) {
			if ( b.length() > 0 ) {
				b.append ( "," );
			}
			b.append ( "Where4=\"" + Where4 + "\"" );
		}
		String Where5 = props.getValue("Where5");
		if ( (Where5 != null) && (Where5.length() > 0) &&
			!Where5.startsWith(delim) ) {
			if ( b.length() > 0 ) {
				b.append ( "," );
			}
			b.append ( "Where5=\"" + Where5 + "\"" );
		}
		String Where6 = props.getValue("Where6");
		if ( (Where6 != null) && (Where6.length() > 0) &&
			!Where6.startsWith(delim) ) {
			if ( b.length() > 0 ) {
				b.append ( "," );
			}
			b.append ( "Where6=\"" + Where6 + "\"" );
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
	/* REVISIT SAM 2006-04-27
	As per Ray Bennett always do this.
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
	if (	(FillUsingDivComments != null) &&
		(FillUsingDivComments.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillUsingDivComments=" + FillUsingDivComments );
	}
	String FillUsingDivCommentsFlag =
		props.getValue("FillUsingDivCommentsFlag");
	if (	(FillUsingDivCommentsFlag != null) &&
		(FillUsingDivCommentsFlag.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "FillUsingDivCommentsFlag=\"" +
			FillUsingDivCommentsFlag + "\"" );
	}
	if ( _use_alias ) {
		String Alias = props.getValue ( "Alias" );
		return "TS " + Alias + " = " + getCommandName() +
			"(" + b.toString() + ")";
	}
	else {	return getCommandName() + "(" + b.toString() + ")";
	}
}

/**
Indicate whether the alias version of the command is being used.  This method
will return a reliable value only after the parseCommandParameters() method is
called.
*/
protected boolean useAlias ()
{	return _use_alias;
}

}
