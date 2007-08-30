//------------------------------------------------------------------------------
// fillUsingDiversionComment_Command - 
// handle the fillUsingDiversionComment() command
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2007-01-25	Kurt Tometich, RTi	Initial version
// 2007-01-29	KAT, RTi		Adding support for two new command
//							parameters: "fillUsingCIU" and "fillUsingCIUFlag".
//							Added private methods recalculateLimits(),
//							createFillConstantPropList() and 
//							findNearestDataPoint() to decrease
//							duplication of code needed in the runCommand()
//							method.
// 2007-02-08	SAM, RTi			Remove dependency on TSCommandProcessor,
//						instead using the more general CommandProcessor
//						interface.
// 2007-03-12	SAM, RTi		Fix bug in handling null CIU flag.
//------------------------------------------------------------------------------
// EndHeader

package rti.tscommandprocessor.commands.hydrobase;

import javax.swing.JFrame;

import java.util.Vector;

import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSLimits;
import RTi.TS.TSUtil;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.SkeletonCommand;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

import DWR.DMI.HydroBaseDMI.HydroBaseDMI;
import DWR.DMI.HydroBaseDMI.HydroBase_Structure;
import DWR.DMI.HydroBaseDMI.HydroBase_Util;
import DWR.DMI.HydroBaseDMI.HydroBase_WaterDistrict;

/**
<p>
This class initializes, checks, and runs the fillUsingDiversionComments() command.
</p>
<p>The CommandProcessor must return the following properties:  TSResultsList.
</p>
*/
public class fillUsingDiversionComments_Command extends SkeletonCommand
implements Command
{

/**
Protected data members shared with the dialog and other related classes.
*/
protected final String _AllTS = "AllTS";
protected final String _SelectedTS = "SelectedTS";
protected final String _AllMatchingTSID = "AllMatchingTSID";

/**
Constructor.
*/
public fillUsingDiversionComments_Command ()
{	super();
	setCommandName ( "fillUsingDiversionComments" );
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
{	
	String TSID = parameters.getValue ( "TSID" );
	String FillStart = parameters.getValue ( "FillStart" );
	String FillEnd = parameters.getValue ( "FillEnd" );
	String FillFlag = parameters.getValue ( "FillFlag" );
	String FillUsingCIU = parameters.getValue ( "FillUsingCIU" );
	String FillUsingCIUFlag = parameters.getValue ( "FillUsingCIUFlag" );
	String warning = "";
	
	if ( (TSID == null) || (TSID.length() == 0) ) {
				warning += "\nA TSID must be specified.";
	}
	if ( (FillStart != null) && !FillStart.equals("") &&
		!FillStart.equalsIgnoreCase("OutputStart")){
		try {	DateTime.parse(FillStart);
		}
		catch ( Exception e ) {
			warning += 
				"\nThe fill start date/time \"" + FillStart +
				"\" is not a valid date/time.\n"+
				"Specify a date or OutputStart.";
		}
	}
	if ( (FillEnd != null) && !FillEnd.equals("") &&
		!FillEnd.equalsIgnoreCase("OutputEnd") ) {
		try {	DateTime.parse( FillEnd);
		}
		catch ( Exception e ) {
			warning +=
				"\nThe fill end date/time \"" + FillEnd +
				"\" is not a valid date/time.\n"+
				"Specify a date or OutputEnd.";
		}
	}
	if ( (FillFlag != null) && !(FillFlag.equalsIgnoreCase("Auto")) &&
			(FillFlag.length() != 1) ) {
		warning += "\nThe fill flag must be 1 character long or set to Auto.";
	}
	if ( FillUsingCIU != null && !(FillUsingCIU.equalsIgnoreCase("True")) && 
			!(FillUsingCIU.equalsIgnoreCase("False")) &&
			!(FillUsingCIU.equalsIgnoreCase(""))) {
		warning += "\nFill Using CIU must be true, false or blank.\n";
	}
	if ( FillUsingCIU != null && 
			FillUsingCIU.equalsIgnoreCase( "true" ) && 
			FillUsingCIUFlag != null && 
			!(FillUsingCIUFlag.equalsIgnoreCase("Auto")) &&
			FillUsingCIUFlag.length() != 1 ) {
		warning += "\nThe fill using CIU flag must be 1 character long or set to Auto.";
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
	return (new fillUsingDiversionComments_JDialog ( parent, this )).ok();
}

/**
Parse the command string into a PropList of parameters.  
@param command A string command to parse.
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
public void parseCommand (	String command, String command_tag,
				int warning_level )
throws InvalidCommandSyntaxException, InvalidCommandParameterException
{	
	int warning_count = 0;
	String routine = "fillUsingDiversionComments_Command.parseCommand",
	  message;

	Vector tokens = StringUtil.breakStringList ( command,
			"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the command name, TSID
		message = "Syntax error in \"" + command +
		"\".  Not enough tokens.";
		Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
						command_tag,++warning_count), routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
	// Get the input needed to process the file...
	try {	_parameters = PropList.parse ( Prop.SET_FROM_PERSISTENT,
			(String)tokens.elementAt(1), routine, "," );
	}
	catch ( Exception e ) {
		message = "Syntax error in \"" + command +
		"\".  Not enough tokens.";
		Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
						command_tag,++warning_count), routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
}

/**
Calls TSCommandProcessor to re-calulate limits for this time series.
@param ts Time Series.
@param TSCmdProc CommandProcessor that is using this command.
@param warningLevel Warning level used for displaying warnings.
@param warning_count Number of warnings found.
@param command_tag Reference or identifier for this command.
 */
private int recalculateLimits( TS ts, CommandProcessor TSCmdProc, 
		int warningLevel, int warning_count, String command_tag )
{
	String routine = "fillUsingDiversionComments_Command.recalculateLimits";
	
	PropList request_params = new PropList ( "" );
	request_params.setUsingObject ( "TS", ts );
	CommandProcessorRequestResultsBean bean = null;
	try { bean =
		TSCmdProc.processRequest( "CalculateTSAverageLimits", request_params);
	}
	catch ( Exception e ) {
		Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, "Error recalculating original data " + 
				"limits for \"" + ts.getIdentifierString() + "\""  );
		return warning_count;
	}
	// Get the calculated limits and set in the original data limits...
	PropList bean_PropList = bean.getResultsPropList();
	Object prop_contents = bean_PropList.getContents ( "TSLimits" );
	if ( prop_contents == null ) {
		Message.printWarning(warningLevel,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, "Null value from CalculateTSAverageLimits(" +
			ts.getIdentifierString() + ")" );
		return warning_count;
	}
	// Now set the limits.
	ts.setDataLimitsOriginal ( (TSLimits)prop_contents );
	return warning_count;
}

/**
Method to execute the fillUsingDiversionComments() command.
@param command_tag specfic tag or identifier for this command.
@param warningLevel The message warning level to write to the log file with.
@exception Exception if there is an error processing the command.
*/
public void runCommand ( String command_tag, int warningLevel )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	
	int warning_count = 0;
	int log_level = 3;
	String message, routine = "fillUsingDiversionComments_Command.runCommand";
	String TSID = _parameters.getValue ( "TSID" );
	String FillStart = _parameters.getValue ( "FillStart" );
	String FillEnd = _parameters.getValue ( "FillEnd" );
	String FillFlag = _parameters.getValue ( "FillFlag" );
	String FillUsingCIU = _parameters.getValue ( "FillUsingCIU" );
	String FillUsingCIUFlag = _parameters.getValue ( "FillUsingCIUFlag" );
	String RecalcLimits = _parameters.getValue ( "RecalcLimits" );
	
	DateTime start = null;
	DateTime end = null;
	try {	if ( FillStart != null ) {
			start = DateTime.parse(FillStart);
		}
	}
	catch ( Exception e ) {
		message = "Fill start " + FillStart + " is not a valid date.";
		Message.printWarning ( warningLevel, 
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	try {	if ( FillEnd != null ) {
			end = DateTime.parse(FillEnd);
		}
	}
	catch ( Exception e ) {
		message = "Fill end " + FillEnd + " is not a valid date.";
		Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	// Initialize members for filling time series based on command.
	// The default is to fill all time series ( TSID = "*" )
	TS ts = null;			// Time series instance to update
	HydroBaseDMI hbdmi = null;	// HydroBaseDMI to use
	// Defaults are to process a list of time series returned with TSID="*".
	// This will be reset below to a single time series if a TSID is
	// specified as input.
	int nts = 0;
	Object o = null;
	try { o = _processor.getPropContents ( "TSResultsListSize");
		if ( o == null ) {
			Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
			routine, "Unable to determine number of time series to process.  Assuming 0." );
		}
		else {	nts = ((Integer)o).intValue();
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		message = "Error requesting TSResultsListSize from processor - not using.";
		Message.printDebug(10, routine, message );
	}
	int start_pos = 0;	// starting position TSID iterator
	int end_pos = nts;	// end position for TSID iterator
	
	if( !TSID.equals( "*" )) {
		// A specific TSID was chosen and should be used
		PropList request_params = new PropList ( "" );
		request_params.set ( "TSID", TSID );
		CommandProcessorRequestResultsBean bean = null;
		try { bean =
			_processor.processRequest( "IndexOf", request_params);
		}
		catch ( Exception e ) {
			Message.printWarning(log_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, "Error requesting IndexOf(TSID=" + TSID +
					"\" from processor." );
		}
		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "Index" );
		if ( prop_contents == null ) {
			Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, "Null value for IndexOf(TSID=" + TSID +
				"\") returned from processor." );
		}
		else {	nts = ((Integer)o).intValue();
		}
		if ( nts != 1 ) {
			// Unable to get a list of time series to process.
			message = "Unable to find time series \"" +
			TSID + "\" for " + getCommandName() + "() " +
			"command.";
			Message.printWarning ( warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			try {
				throw new Exception ( message );
			} catch (Exception e) {
				Message.printWarning(warningLevel,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, e.toString());
				// do not update if TSID is not find	
				return;
			}
		}
		// Start is the specific time series to process
		start_pos = nts;
		// End is one more for "<" conditional in loop.
		end_pos = ++nts;
	}
	
	boolean HaveOutputPeriod_boolean = false;
	try { o = _processor.getPropContents ( "HaveOutputPeriod");
		if ( o == null ) {
			Message.printWarning(warningLevel,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, "Unable to whether output period is available.  Assuming False." );
		}
		else {	HaveOutputPeriod_boolean = ((Boolean)o).booleanValue();
		}
	}
	catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting HaveOutputPeriod from processor - not using.";
			Message.printDebug(10, routine, message );
	}
	
	// Loop through and fill data for TSID's chosen
	for ( int its = start_pos; its < end_pos; its++ ) {
		// Get the time series to process...
		
		PropList request_params = new PropList ( "" );
		request_params.setUsingObject ( "Index", new Integer(its) );
		CommandProcessorRequestResultsBean bean = null;
		try { bean =
			_processor.processRequest( "GetTimeSeries", request_params);
		}
		catch ( Exception e ) {
			Message.printWarning(warningLevel,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, "Error requesting GetTimeSeries(Index=" + its +
					"\" from processor.  Skipping." );
			continue;
		}
		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "TS" );
		if ( prop_contents == null ) {
			Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, "Null value for GetTimeSeries(Index=" + its +
				"\") returned from processor.  Skipping time series." );
				continue;
		}
		// Now get the time series from the request results.
		ts = (TS)prop_contents;
		
		// Get the HydroBase connection instance to use for this time series.
		
		request_params = new PropList ( "" );
		request_params.set ( "InputName", "" + ts.getIdentifier().getInputName() );
		try { bean =
			_processor.processRequest( "GetHydroBaseDMI", request_params);
		}
		catch ( Exception e ) {
			Message.printWarning(warningLevel,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, "Error requesting GetHydroBaseDMI(InputName=" +
					ts.getIdentifier().getInputName() +
					"\" from processor.  Skipping time series." );
			continue;
		}
		bean_PropList = bean.getResultsPropList();
		prop_contents = bean_PropList.getContents ( "HydroBaseDMI" );
		if ( prop_contents == null ) {
			Message.printWarning(warningLevel,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, "Null value for GetHydroBaseDMI(InputName=" +
				ts.getIdentifier().getInputName() +
				"\") returned from processor.  Skipping time series." );
				continue;
		}
		hbdmi = (HydroBaseDMI)prop_contents;
		
		// Fill with diversion comments...

		if ( HaveOutputPeriod_boolean ) {
			// No need to extend the period...
			try {
				HydroBase_Util.fillTSUsingDiversionComments (
						hbdmi, ts, start, end, FillFlag, false );
			} catch (Exception e) {
				Message.printWarning(warningLevel,
						MessageUtil.formatMessageTag( command_tag,
						++warning_count), routine, 
						"Could not fill time series:" + ts + " with" +
						"diversion comments.");
				Message.printWarning(3, routine, e );
			}
		}
		else {	// Extend the period if data are available...
			try {
				HydroBase_Util.fillTSUsingDiversionComments (
						hbdmi, ts, start, end, FillFlag, true );
			} catch (Exception e) {
				Message.printWarning(warningLevel,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, "Could not fill time series:" + ts + " with" +
						"diversion comments.");
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
				wdid_parts = HydroBase_WaterDistrict.parseWDID ( 
					TSID_Location_part );
			} catch (Exception e1) {
				Message.printWarning(warningLevel, 
					MessageUtil.formatMessageTag( command_tag,
					++warning_count), routine, e1.toString());
			}
			int wd = wdid_parts[0];
			int id = wdid_parts[1];
			try {
				struct = hbdmi.readStructureViewForWDID ( wd, id );
			} catch (Exception e1) {
				Message.printWarning(warningLevel, 
					MessageUtil.formatMessageTag( command_tag,
					++warning_count), routine, e1.toString());
			}
			// HydroBase currently in use value
			String ciu = struct.getCiu();
			// set the fill value
			String fillValue = "0";
			String fillFlag = "";
			if( (FillUsingCIUFlag != null) && !FillUsingCIUFlag.equals("")) {
				if( FillUsingCIUFlag.equals( "Auto" )) {
					fillFlag = ciu;
				}
				else if( FillUsingCIUFlag.length() == 1 ) {
					fillFlag = FillUsingCIUFlag;
				}
			}
			// Based on CIU string, fill missing values with
			// flag value
			// H = "Historical structure"
			// I = "Inactive structure"
			if( ciu.equalsIgnoreCase( "H" ) || 
					ciu.equalsIgnoreCase( "I" )) {
				// Recalculate TS Limits
				warning_count = 
					recalculateLimits( ts, _processor, warningLevel, 
					warning_count, command_tag );
				// Fill missing data values at end of period with zeros
				try {
					// get the nearest data point from the end of
					// the period
					TSData tmpTSData = 
						TSUtil.findNearestDataPoint(ts, start, 
						end, true);
					if( tmpTSData != null) {
						PropList const_prop = 
							HydroBase_Util.createFillConstantPropList(ts,
							fillValue, fillFlag, 
							tmpTSData.getDate(),
							ts.getDate2());
						// fill time series with zeros from last
						// non-missing value
						// until the end of the period.
						TSUtil.fillConstant(ts, start, end, 0, const_prop);
						//TS tsFilled = TSUtil.fill(ts, const_prop);
						//ts = tsFilled;
					}
				} catch (Exception e) {
					Message.printWarning(warningLevel, 
						MessageUtil.formatMessageTag( command_tag,
						++warning_count), routine, "Could not fill" +
						" time series:" + ts);
					Message.printWarning(3, routine, e );
				}	
			}
			// N = "Non-existent structure"
			else if( ciu.equalsIgnoreCase( "N" )) {
				// Recalculate TS Limits
				warning_count = 
					recalculateLimits( ts, _processor, warningLevel, 
					warning_count, command_tag );
				try {
					TSData tmpTSData = 
						TSUtil.findNearestDataPoint(ts, start, 
						end, false);
					if( tmpTSData != null) {
						// Create propList for fill command
						PropList const_prop = 
							HydroBase_Util.createFillConstantPropList(ts,
							fillValue, fillFlag, ts.getDate1(),
							tmpTSData.getDate());
						// fill time series with zero's from first
						// non-missing value
						// until the beginning of the period.
						TSUtil.fillConstant(ts, start, end, 0, const_prop);
						//TS tsFilled = TSUtil.fill(ts, const_prop);
						//ts = tsFilled;
					}
				}
				catch (Exception e) {
					Message.printWarning(warningLevel, 
						MessageUtil.formatMessageTag( command_tag,
						++warning_count), routine, "Could not fill" +
						" time series:" + ts);
					Message.printWarning(3, routine, e );
				}
			}	
		}
		else if ( RecalcLimits.equalsIgnoreCase( "True" ) ) {
				recalculateLimits( ts, _processor, warningLevel,
					warning_count, command_tag );
		}
		// Update the time series in the processor...

		try {
		request_params = new PropList ( "" );
		request_params.set ( "Action", "Update" );
		request_params.setUsingObject ( "TS", ts );
		request_params.setUsingObject ( "Index", new Integer(its) );
		try { bean =
			_processor.processRequest( "ProcessTimeSeriesAction", request_params);
		}
		catch ( Exception e ) {
			Message.printWarning(warningLevel,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, "Error requesting ProcessTimeSeriesAction(Action=Update" +
					"\" from processor.  Results will not be visible." );
			continue;
		}
		} catch (Exception e) {
			Message.printWarning(warningLevel,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, e.toString());
		}
	}
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String TSID = props.getValue( "TSID" );
	String FillStart = props.getValue( "FillStart" );
	String FillEnd = props.getValue( "FillEnd" );
	String FillFlag = props.getValue( "FillFlag" );
	String FillUsingCIU = props.getValue( "FillUsingCIU" );
	String FillUsingCIUFlag = props.getValue( "FillUsingCIUFlag" );
	String RecalcLimits = props.getValue( "RecalcLimits" );
	StringBuffer b = new StringBuffer ();
	
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
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
	if ( ( RecalcLimits != null) && (RecalcLimits.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "RecalcLimits=" + RecalcLimits );
	}
	
	return getCommandName() + "(" + b.toString() + ")";
}

}
