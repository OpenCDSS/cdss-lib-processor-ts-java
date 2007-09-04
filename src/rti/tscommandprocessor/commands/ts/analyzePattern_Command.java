// -----------------------------------------------------------------------------
// analyzePattern_Command - Handle the analyzePattern() command
// -----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// -----------------------------------------------------------------------------
// History:
// 2004-04-30	Luiz Teixeira, RTi	Derived from the fillMixedStation class
// 2005-05-23	Luiz Teixeira, RTi	Copied the original class 
//					analyzePattern_JDialog() from TSTool and
//					split the code into the new
//					analyzePattern_JDialog() and
//					analyzePattern_Command().
// 2005-05-24	Luiz Teixeira, RTi	Clean up and documentation.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-02-27	SAM, RTi		Add check to make sure percentiles are in the
//					range 0 to 1 (not 0 to 100) since analysis code expects 0 to 1.
//					Clarify the check messages.
// 2007-03-02	SAM, RTi		Fix so that the resulting pattern time series
//					DOES NOT have the identifier changed but instead has the
//					data type set to "Pattern".
// -----------------------------------------------------------------------------
package rti.tscommandprocessor.commands.ts;

import java.io.File;
import java.util.Vector;

import javax.swing.JFrame;

import DWR.StateMod.StateMod_TS;

import RTi.TS.TSLimits;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Math.MathUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.TS.TSIdent;
import RTi.TS.StringMonthTS;
import RTi.TS.TS;
import RTi.TS.TSUtil;

public class analyzePattern_Command extends AbstractCommand
	implements Command
{

// Defines used by this class and its analyzePattern_Dialog counterpart.
protected final String _SelectedTS          = "SelectedTS";
protected final String _AllMatchingTSID     = "AllMatchingTSID";
protected final String _AllTS               = "AllTS";
protected final String _ANALYSIS_PERCENTILE = "Percentile";

double [] __percentileArray = null;
String [] __patternIDArray  = null;

/**
analyzePattern_Command constructor.
*/
public analyzePattern_Command ()
{
	super ();
	setCommandName ( "analyzePattern" );
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
public void checkCommandParameters ( PropList parameters,
				     String command_tag,
				     int warning_level )
throws InvalidCommandParameterException
{	String warning = "";
	
	String TSList     = parameters.getValue ( "TSList"     );
	String TSID       = parameters.getValue ( "TSID"       );
	// TODO SAM 2007-02-17 Need to enable Method
	//String Method     = parameters.getValue ( "Method"     );
	String Percentile = parameters.getValue ( "Percentile" );
	String PatternID  = parameters.getValue ( "PatternID"  );
	String OutputFile = parameters.getValue ( "OutputFile" );
	
	// Get the working_dir from the command processor
	String working_dir = null;
	
	CommandProcessor processor = getCommandProcessor();
	
	try { Object o = processor.getPropContents ( "WorkingDir" );
		// Working directory is available so use it...
		if ( o != null ) {
			working_dir = (String)o;
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		String message = "Error requesting WorkingDir from processor - not using.";
		String routine = getCommandName() + ".checkCommandParameters";
		Message.printDebug(10, routine, message );
	}
	
	// Make sure TSID is specified only when the TSList=_AllMatchingTSID.
	if ( (TSList != null) && !TSList.equalsIgnoreCase(_AllMatchingTSID) ) {
		if ( TSID != null ) {
			warning +=
				"\nTSID should only be specified when TSList="
				+ _AllMatchingTSID + ".";
		}
	}
	
	// Make sure one or more time series are selected when
	// AllMatchingTSID is selected.
	if ( TSList.equalsIgnoreCase ( _AllMatchingTSID ) ) {
		if ( TSID != null ) {
			Vector selectedV = StringUtil.breakStringList (
				TSID, ",",
				StringUtil.DELIM_SKIP_BLANKS );
			if ( (selectedV == null) || (selectedV.size() == 0) ) {
				warning +=
				"\n List of time series should not be empty "
				+ " when the AllMatchingTSID is selected. ";
			}
		} else { 
			warning +=
				"\nTSID should should not be null when TSList="
				+ _AllMatchingTSID + " is specified.";
		}
	}

	// Make sure Percentile list is given and the values are valid.
	//     0.25, 0.75          Acceptable input values example.
	// In this example there will be three classes:
	// 	1st - From 0.00 to 0.25
	//	2nd - From 0.25 to 0.75
	// 	3nd - From 0.75 to 1
	// i.e if 2 values are given for the precentiles, three values are
	// needed for the patternID, one for each of the three classes
	// mentioned above.
	int percentileCount = 0;
	if ( Percentile == null || Percentile.equals("") ) {
		warning += "\nPercentile list is required.";
	} else {
		Vector percentileV = StringUtil.breakStringList (
			Percentile, ",",
			StringUtil.DELIM_SKIP_BLANKS );
		if ( percentileV == null || percentileV.size()<= 0 ) {
			warning += "\nError parsing Percentile list \""
				+ Percentile + "\".";
		} else {
			percentileCount = percentileV.size();
			// Make sure to add one for the upper limit of the
			// classes (0.0,1.0).
			__percentileArray = new double [ percentileCount + 1 ];
			String percentileStr;
			double previousP = 0, currentP;
			for ( int n = 0; n < percentileCount; n++ ) {
				percentileStr =(String)percentileV.elementAt(n);
				if ( !StringUtil.isDouble( percentileStr ) ) {
					warning += "\n Percentile \""
						+ percentileStr
						+ "\" is not a number.";
				}
				// Percentile must be between 0 and 1.
				currentP = StringUtil.atof( percentileStr );
				if ( currentP <= 0 || currentP >= 1 ) {
					warning += "\n Percentile \""
						+ percentileStr
						+ "\" is not between (0 and 1).";
				}
				// Current Percentile should always be < than
				// the previous one
				if ( currentP <= previousP ) {
					warning += "\n Percentile \""
						+ percentileStr
						+ "\" must be greater than the"
						+ " previous Percentile \""
						+ previousP
						+ "\".";
				}
				__percentileArray[n] = currentP;
				previousP = currentP;
			}
			__percentileArray[ percentileCount ] = 1.0;
		}
	}

	// Make sure the PatternID list is given and the values are valid.
	// The number of PatternID must be plus one in addition to the
	// number of percentiles.
	int patternCount= 0; 
	if ( PatternID == null || PatternID.equals("")
		&& !StringUtil.isInteger(PatternID) ) {
		warning += "\n Pattern list is required.";
	} else {
		Vector patternV = StringUtil.breakStringList (
			PatternID, ",",
			StringUtil.DELIM_SKIP_BLANKS );
		if ( patternV == null || patternV.size() <= 0 ) {
			warning += "\nError parsing PatternID list \""
				+ PatternID
				+ "\".";
		} else {
			patternCount = patternV.size();
			if ( ( patternCount - percentileCount ) != 1 ) {
				warning += "\nThe PatternID count is \""
					+ patternCount
					+ "\". It must be equal to \""
					+ (percentileCount + 1)
					+ "\" (Percentile count + 1).";
			}
		}
		__patternIDArray = new String [ patternCount ];
		for ( int n = 0; n < patternCount; n++ ) {
			String pattern = (String) patternV.elementAt(n);
			__patternIDArray[n] = pattern;
		}
	}
	
	// Output file
	if ( OutputFile == null || OutputFile.length() == 0 ) {
		warning += "\nThe Output File field is empty.";
	} else {	
		try {
			String adjusted_path = IOUtil.adjustPath (
				 working_dir, OutputFile);
			File f  = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				warning += "\nThe file parent directory "
				+ " does not exist:\n"
				+ adjusted_path + ".";
			}
			f  = null;
			f2 = null;
		}
		catch ( Exception e ) {
			warning += "\nThe working directory:\n"
				+ "    \"" + working_dir
				+ "\"\ncannot be adjusted using:\n"
				+ "    \"" + OutputFile
				+ "\".";
		}
	}

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, warning_level ),
			warning );
		throw new InvalidCommandParameterException ( warning );
	}
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	
	// The command will be modified if changed...
	return ( new analyzePattern_JDialog ( parent, this ) ).ok();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
	super.finalize ();
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
public void parseCommand ( String command,
			   String command_tag,
			   int    warning_level )
throws 	InvalidCommandSyntaxException,
	InvalidCommandParameterException
{
	String routine = "analyzePattern_Command.parseCommand", message;

	int warning_count = 0;

	Vector tokens = StringUtil.breakStringList ( command,
		"()", StringUtil.DELIM_SKIP_BLANKS );
	if ( (tokens == null) || tokens.size() < 2 ) {
		// Must have at least the command name and the InputFile
		message = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag,++warning_count),
			routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}

	// Get the input needed to process the file...
	try {
		setCommandParameters( PropList.parse ( Prop.SET_FROM_PERSISTENT,
			(String)tokens.elementAt(1), routine, "," ) );
	}
	catch ( Exception e ) {
		message = "Syntax error in \"" + command +
			"\".  Not enough tokens.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag,++warning_count),
			routine, message);
		throw new InvalidCommandSyntaxException ( message );
	}
}

/**
Run the command:
<pre>
analyzePattern (TSList="x", Method=Percentile",Pattern="DRY,AVG,WET",
		Percentile="25,75",OutputFile="y")
</pre>
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2).
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more
parameter values are invalid.
*/
public void runCommand ( String command_tag,
			 int warning_level )
throws InvalidCommandParameterException,
       CommandWarningException,
       CommandException
{
	String routine = "analyzePattern_Command.runCommand", message = "";
	
	int warning_count = 0;
	int log_level = 3;		// Level for non-user messages

	// Line separator.
	String __nl = System.getProperty ("line.separator");
	
	CommandProcessor processor = getCommandProcessor();
	PropList parameters = getCommandParameters();

	// Make sure there are time series available to operate on...
	String TSList = parameters.getValue ( "TSList" );
	String TSID   = parameters.getValue ( "TSID"   );

	// Get the time series to process...
	
	PropList request_params = new PropList ( "" );
	request_params.set ( "TSList", TSList );
	request_params.set ( "TSID", TSID );
	CommandProcessorRequestResultsBean bean = null;
	try { bean =
		processor.processRequest( "GetTimeSeriesToProcess", request_params);
	}
	catch ( Exception e ) {
		message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
		"\", TSID=\"" + TSID + "\" from processor.";
		Message.printWarning(log_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
	}
	PropList bean_PropList = bean.getResultsPropList();
	Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
	Vector tslist = null;
	if ( o_TSList == null ) {
		message = "Unable to find time series to process using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( log_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}
	else {	tslist = (Vector)o_TSList;
		if ( tslist.size() == 0 ) {
			message = "Unable to find time series to process using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
			Message.printWarning ( log_level,
					MessageUtil.formatMessageTag(
							command_tag,++warning_count), routine, message );
		}
	}
	int [] tspos = null;
	Object o_Indices = bean_PropList.getContents ( "Indices" );
	if ( o_Indices == null ) {
		message = "Unable to find positions for time series to process using TSList=\"" + TSList +
		"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( log_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
	}
	else {	tspos = (int [])o_Indices;
		if ( tspos.length == 0 ) {
			message = "Unable to find positions for time series to process using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
			Message.printWarning ( log_level,
					MessageUtil.formatMessageTag(
							command_tag,++warning_count), routine, message );
		}
	}
	
	int tsCount = tslist.size();
	if ( tsCount == 0 ) {
		message = "No time series were found to processing using TSList=\"" + TSList +
			"\" TSID=\"" + TSID + "\".";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count), routine, message );
	}
	
	// Get the method. Currently only Percentile method is supported.
	String Method = parameters.getValue ( "Method" );
	if ( Method == null || Method.equals("") ) {
		Method = "Percentile";
	}

	// Percentile list
	// The percentileArray[] is populated while checking the Command
	// Parameters (checkCommandParameters() method)
	
	// Pattern list
	// The patternIDArray[] is populated while checking the Command
	// Parameters (checkCommandParameters() method)
	
	// OutputFile
	String OutputFile = parameters.getValue ("OutputFile");
	if ( OutputFile == null || OutputFile.equals("") ) {
		message = "\nThe OutputFile parameter must be specified. ";
		Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
					command_tag, ++warning_count), routine, message );
	}
	
	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Command parameter data has errors.  Unable to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		throw new CommandException ( message );
	}

	// Get the start and end dates among the time series.
	DateTime startDate = null;
	DateTime endDate   = null;
	TSLimits limits = new TSLimits ();
	try {	
		limits = TSUtil.getPeriodFromTS ( tslist, TSUtil.MAX_POR );
	} catch ( Exception e ) {
	}
	startDate = new DateTime ( limits.getDate1() );
	endDate   = new DateTime ( limits.getDate2() );

	// Monthly Time series
	int smBase = TimeInterval.MONTH;
	int smMult = 1;
	
	TS analysisTS;
	try {
		// Looping through each time series creating a new StringMonthTS
		// And keep the StringMonthTS in a vector of time series
		Vector StringMonthTS_List = new Vector(tsCount);
		StringMonthTS stringMonthTS = null;
		for ( int nTS = 0; nTS < tsCount; nTS++ ) {
			
			// Get the time series object.
			analysisTS = null;
			request_params = new PropList ( "" );
			request_params.setUsingObject ( "Index", new Integer(tspos[nTS]) );
			bean = null;
			try { bean =
				processor.processRequest( "GetTimeSeries", request_params);
			}
			catch ( Exception e ) {
				Message.printWarning(log_level,
						MessageUtil.formatMessageTag( command_tag, ++warning_count),
						routine, "Error requesting GetTimeSeries(Index=" + tspos[nTS] +
						"\" from processor." );
			}
			bean_PropList = bean.getResultsPropList();
			Object prop_contents = bean_PropList.getContents ( "TS" );
			if ( prop_contents == null ) {
				Message.printWarning(warning_level,
					MessageUtil.formatMessageTag( command_tag, ++warning_count),
					routine, "Null value for GetTimeSeries(Index=" + tspos[nTS] +
					"\") returned from processor - skipping." );
					// Skip the time series...
					continue;
			}
			else {	analysisTS = (TS)prop_contents;
			}

		   	// Create stringMonthTS to receive the analysis results
			TSIdent smIdent  = new TSIdent(
				analysisTS.getIdentifier() );
			smIdent.setType ( "Pattern");
			smIdent.setInterval ( smBase, smMult );

			// Create the new time series using the new identifier.
			stringMonthTS = new StringMonthTS(
				smIdent.getIdentifier(), startDate, endDate );
			if ( stringMonthTS == null ) {
				message =
				"Could not create the StringMonth time series.";
				throw new CommandWarningException ( message );
			}
			// Update the stringMonth time series properties with
			// information from the input analysis time series.
			// Notice: CopyHeader() overwrites, among several other
			//	   things, the Identifier, the DataInterval
			//	   (Base and Multiplier).
			//         It also set the dates, from the old time
			//	   series. Make sure to reset these properties
			//	   to the values needed by the new time series.
			stringMonthTS.copyHeader        ( analysisTS );
		// REVISIT What dataType?			
		//	stringMonthTS.setDataType       ( NewDataType );
			stringMonthTS.setIdentifier     ( smIdent  );
			stringMonthTS.setDataInterval   ( smBase, smMult );
			// Finally allocate data space.
			stringMonthTS.allocateDataSpace	();

			// Get the actual startDate end the endDate for this
			// time series. These may different from the dates
			// used to create the stringMonthTS.
			DateTime _startDate =
				new DateTime( analysisTS.getDate1() );
			DateTime _endDate   =
				new DateTime( analysisTS.getDate2() );
			double [] values      = null;
			int    [] sortedOrder = null;
			int nValues;

			for ( int month = 1; month <= 12; month++ ) {

				values = TSUtil.toArrayByMonth (
					analysisTS,
					_startDate,
					_endDate,
					month);

				// Size of the returning array.
				nValues = values.length;
				sortedOrder = new int[nValues];
				MathUtil.sort ( values,
		  	    		MathUtil.SORT_QUICK,
		  	    		MathUtil.SORT_ASCENDING,
		  	    		sortedOrder,
		  	    		true );

				// Find the first year, to use as offset.
				int yearOffset = _startDate.getYear();
				if ( _startDate.getMonth() > month ) {
					yearOffset += 1;
				}

				// Loop throught the sorted data until the first
				// non-missing is found. Then set the counters,
				// the currentUpperLimit, the currentPatternID
				// and break out.
				int     missingCount     = 0;
				int non_missingCount     = 0;
				double currentUpperLimit = -999.9;
				String currentPatternID  = "";
				int    percentileIndex   = 0;
				boolean found = false;
				for( int n=0; n<nValues && found==false; n++ ) {

					// Build the dateTime
					DateTime _date = new DateTime();
					_date.setMonth( month );
					_date.setYear( yearOffset +
						sortedOrder[n] );

					if ( analysisTS.isDataMissing(
					    analysisTS.getDataValue(_date) ) ) {
					    	
					    	if ( Message.isDebugOn ) {
					    		message = n + "  "
					    		+ _date.toString()
					    		+ " "
					    		+ analysisTS.
					    		getDataValue(_date)
					    		+ __nl;
							Message.printDebug(
								2, routine, message );
						}
						
						continue;
					} else {
						missingCount = n;
						non_missingCount = nValues-n;
						currentUpperLimit =
							missingCount +
							non_missingCount *
							__percentileArray[
						  	   percentileIndex ];
						currentPatternID =
						   	   __patternIDArray[
							   percentileIndex ];
						found = true;
					}
				}
				
				if ( Message.isDebugOn ) {
					message = " miss " + missingCount 
						+ " non miss "
						+ non_missingCount + __nl;
					Message.printDebug (2, routine, message );
				}

				// Populate the stringMonthTS time series based
				// on the sorted values and using the petternIDs
				for( int n = missingCount; n<nValues; n++ ){

					// Build the dateTime
					DateTime _date = new DateTime();
					_date.setMonth( month );
					_date.setYear( yearOffset +
						sortedOrder[n] );

					// If beyond the current limits, compute
					// the next
					if ( n >= currentUpperLimit ) {
						percentileIndex +=1;
						currentUpperLimit =
						    missingCount +
						    non_missingCount *
						   	__percentileArray[
							    percentileIndex ];
						currentPatternID =
						    __patternIDArray[
							percentileIndex ];
					}

					// Set the date and value in the TS
					stringMonthTS.setDataValue (
						_date, currentPatternID );
					
					if ( Message.isDebugOn ) {
						message = " " + _date.toString()
	           				+ " " + analysisTS.getDataValue
	           					( _date )
		   				+ " " + stringMonthTS.
		   					getDataValueAsString (
		   						_date )
		   				+ __nl;
						Message.printDebug (
							2, routine, message );
					}

				}
			}
			// Add this time series to the list.
			StringMonthTS_List.addElement( stringMonthTS );
		}

		// Saving results in the output file.
		PropList props = new PropList ( "AnalyzePattern" );
		String CalendarType = "CalendarYear";
	   	try { Object o = processor.getPropContents ( "OutputYearType" );
	   		if ( o != null ) {
	   			CalendarType = (String)o;
	   			// Convert to the format used by code below...
	   			if ( StringUtil.indexOfIgnoreCase(CalendarType, "calendar", 0) >= 0 ){
	   				CalendarType = "CalendarYear";
	   			}
	   			else if ( StringUtil.indexOfIgnoreCase(CalendarType, "water", 0) >= 0 ){
	   				CalendarType = "WaterYear";
	   			}
	   		}
	   	}
	   	catch ( Exception e ) {
	   		// Not fatal, but of use to developers.
	   		message = "Error requesting OutputYearType from processor - using calendar.";
	   		Message.printDebug(10, routine, message );
	   	}
        props.add ( "CalendarType=" + CalendarType );
                
        // Write the results file using the available data, or the global
        // output period if it was specified.
                
        DateTime OutputStart_DateTime = startDate;
        DateTime OutputEnd_DateTime = endDate;
        
    	try { Object o = processor.getPropContents ( "OutputStart" );
			if ( o != null ) {
				OutputStart_DateTime = (DateTime)o;
			}
    	}
    	catch ( Exception e ) {
    		// Not fatal, but of use to developers.
    		message = "Error requesting OutputStart from processor - not using.";
    		Message.printDebug(10, routine, message );
    	}
    	try { Object o = processor.getPropContents ( "OutputEnd" );
			if ( o != null ) {
				OutputEnd_DateTime = (DateTime)o;
			}
    	}
    	catch ( Exception e ) {
    		// Not fatal, but of use to developers.
    		message = "Error requesting OutputEnd from processor - not using.";
    		Message.printDebug(10, routine, message );
    	}
                
		StateMod_TS.writePatternTimeSeriesList(OutputFile, 
							StringMonthTS_List,
							OutputStart_DateTime,
							OutputEnd_DateTime,
							props );

	} catch ( Exception e ) {
		Message.printWarning ( log_level, routine, e );
		message = "Error processing the pattern analysis";
		throw new CommandWarningException ( message);
	}
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

	// Get the properties
	String TSList     = props.getValue ( "TSList"    );
	String TSID       = props.getValue ( "TSID"      );
	String Method     = props.getValue ( "Method"    );
	String Percentile = props.getValue ( "Percentile");
	String PatternID  = props.getValue ( "PatternID" );
	String OutputFile = props.getValue ( "OutputFile");

	// Creating the command string
	// This StringBuffer will contain all parameters for the command.
	StringBuffer b = new StringBuffer();

	// Adding the TSList
	if ( TSList != null && TSList.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "TSList=" + TSList );
	}

	// Adding the TSID
	if ( TSID != null && TSID.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "TSID=\"" + TSID + "\"" );
	}

	// Adding the Method
	if ( Method != null && Method.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "Method=" + Method );
	}

	// Adding the Percentile
	if ( Percentile != null && Percentile.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "Percentile=\"" + Percentile + "\"" );
	}

	// Adding the PatternID
	if ( PatternID != null && PatternID.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "PatternID=\"" + PatternID + "\""  );
	}

	// Adding the OutputFile
	if ( OutputFile != null && OutputFile.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}
	
	return getCommandName() + "(" + b.toString() + ")";
}

} // end analyzePattern_Command