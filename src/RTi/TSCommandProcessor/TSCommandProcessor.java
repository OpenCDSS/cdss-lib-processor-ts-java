//------------------------------------------------------------------------------
// TSCommandProcessor - a class to process time series commands and manage
//				relevant data
//------------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
//------------------------------------------------------------------------------
// History:
//
// 2005-04-19	Steven A. Malers, RTi	Initial version as a wrapper around
//					TSEngine.
// 2005-09-06	SAM, RTi		Add readTimeSeries2().
// 2005-10-18	SAM, RTi		Implement TSSupplier to allow
//					processing of processTSProduct()
//					commands.
// 2006-05-02	SAM, RTi		Add processCommands() to allow a
//					call from the runCommands() command.
// 2007-01-26	Kurt Tometich, RTi	Moved this class to new product
//							TSCommandProcessor.  Added several methods that
//							call or mimic TSEngine methods to allow generic
//							command implementations to be able to call TSEngine
//							without directly depending or being a member of that
//							class.
// 2007-02-08	SAM, RTi		Change class to TSCommandProcessor package.
//					Explicitly include TS package classes.
//					To remove circular dependencies in commands that are in
//					other lower-level packages, force all interaction with the
//					processor to occur through the properties.
//------------------------------------------------------------------------------
// EndHeader

package RTi.TSCommandProcessor;

import java.util.Vector;

import java.awt.event.WindowListener;	// To know when graph closes to close app

// RTi utility code.

import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.RequestParameterInvalidException;
import RTi.Util.IO.RequestParameterNotFoundException;
import RTi.Util.IO.UnrecognizedRequestException;
import RTi.Util.Time.DateTime;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSLimits;
import RTi.TS.TSSupplier;

// HydroBase commands.

import DWR.DMI.HydroBaseDMI.HydroBaseDMI;

// TS general commands.

// REVISIT SAM 2005-05-19 When TSEngine is sufficiently clean, merge its code
// into this class and rework the connection to the TSTool application.
/**
This class processes time series commands and manages the relevant data.
*/
public class TSCommandProcessor implements CommandProcessor, TSSupplier
{

private TSEngine __tsengine = null;	// Map back to existing code until able
					// to transition into a true
					// TSCommandProcessor class, at which
					// time TSEngine will go away.
public final int	INSERT_TS = 1,	// "ts_action" values
					UPDATE_TS = 2,
					EXIT = 3,
					NONE = 4;

public TSCommandProcessor ()
{	super();
}

public TSCommandProcessor ( TSEngine tsengine )
{	__tsengine = tsengine;
}

/**
Return data for a named property, required by the CommandProcessor
interface.  See getPropcontents() for a list of properties that are
handled.
@param prop Property to set.
@return the named property, or null if a value is not found.
@exception Exception if the property cannot be found or there is an
error determining the property.
*/
public Prop getProp ( String prop ) throws Exception
{	Object o = getPropContents ( prop );
	if ( o == null ) {
		return null;
	}
	else {	// Contents will be a Vector, etc., so convert to a full
		// property.
		// TODO SAM 2005-05-13 This will work seamlessly for strings
		// but may have a side-effects (conversions) for non-strings...
		Prop p = new Prop ( prop, o, o.toString() );
		return p;
	}
}

// TODO SAM 2007-02-18 Need to enable NDFD Adapter
/**
Return the contents for a named property, required by the CommandProcessor
interface. Currently the following properties are handled:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>
</tr>

<td><b>CreateOutput</b></td>
<td>ndicate if output should be created, as Boolean.  If True, commands that create output
should do so.  If False, the commands should be skipped.  This is used to
speed performance during initial testing.
</td>
</tr>

<tr>
<td><b>DataTestList</b></td>
<td>The Vector of DataTests.</td>
</tr>

<tr>
<td><b>HaveOutputPeriod</b></td>
<td>Indicate whether the output period has been specified, as a Boolean.
</td>
</tr>

<tr>
<td><b>HydroBaseDMIList</b></td>
<td>A Vector of open HydroBaseDMI, available for reading.
</td>
</tr>

<tr>
<td><b>InputEnd</b></td>
<td>The input end from the setInputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>InputStart</b></td>
<td>The input start from the setInputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>OutputComments</b></td>
<td>A Vector of String with comments suitable for output.  The comments
DO NOT contain the leading comment character (specific code that writes the
output should add the comment characters).  Currently the comments contain
open HydroBase connection information, if available.
</td>
</tr>

<tr>
<td><b>OutputEnd</b></td>
<td>The output end from the setOutputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>OutputStart</b></td>
<td>The output start from the setOutputPeriod() command, as a DateTime object.
</td>
</tr>

<tr>
<td><b>OutputYearType</b></td>
<td>The output year type, as a String ("Calendar" or "Water", Oct-Nov).
</td>
</tr>

<tr>
<td><b>NDFDAdapterList</b></td>
<td>A Vector of available NDFDAdapter, available for reading.
THIS HAS NOT BEEN IMPLEMENTED.
</td>
</tr>

<tr>
<td><b>TSIDListNoInput</b></td>
<td>The Vector of time series identifiers that are available, without the
input type and name.  The time series identifiers from commands above the
selected command are returned.  This property will normally only be used with
command editor dialogs.</td>
</tr>

<tr>
<td><b>TSProductAnnotationProviderList</b></td>
<td>A Vector of TSProductAnnotationProvider (for example, this is requested
by the processTSProduct() command).
</td>
</tr>

<tr>
<td><b>TSResultsList</b></td>
<td>The Vector of time series results.</td>
</tr>

<tr>
<td><b>TSResultsListSize</b></td>
<td>The size of the TSResultsList as an Integer</td>
</tr>

<tr>
<td><b>TSViewWindowListener</b></td>
<td>The WindowListener that is interested in listing to TSView window events.
This is used when processing a TSProduct in batch mode so that the main
application can close when the TSView window is closed.</td>
</tr>

<tr>
<td><b>WorkingDir</b></td>
<td>The working directory for the processor (initially the same as the
application but may be changed by commands during execution), as a String.</td>
</tr>

</table>
@return the contents for a named property, or null if a value is not found.
@exception UnrecognizedRequestException if an unknown property is requested.
*/
public Object getPropContents ( String prop ) throws Exception
{	// TODO SAM 2007-02-11 Need to start folding TSEngine logic into this class.
	if ( prop.equalsIgnoreCase("CreateOutput") ) {
		return getPropContents_CreateOutput();
	}
	else if ( prop.equalsIgnoreCase("DataTestList") ) {
		return getPropContents_DataTestList();
	}
	else if ( prop.equalsIgnoreCase("HaveOutputPeriod") ) {
		return getPropContents_HaveOutputPeriod();
	}
	else if ( prop.equalsIgnoreCase("InputEnd") ) {
		return getPropContents_InputEnd();
	}
	else if ( prop.equalsIgnoreCase("InputStart") ) {
		return getPropContents_InputStart();
	}
	else if ( prop.equalsIgnoreCase("OutputComments") ) {
		return getPropContents_OutputComments();
	}
	else if ( prop.equalsIgnoreCase("OutputEnd") ) {
		return getPropContents_OutputEnd();
	}
	else if ( prop.equalsIgnoreCase("OutputStart") ) {
		return getPropContents_OutputStart();
	}
	else if ( prop.equalsIgnoreCase("OutputYearType") ) {
		return getPropContents_OutputYearType();
	}
	else if ( prop.equalsIgnoreCase("HydroBaseDMIList") ) {
		return getPropContents_HydroBaseDMIList();
	}
	else if ( prop.equalsIgnoreCase("TSIDListNoInput") ) {
		return getPropContents_TSIDListNoInput();
	}
	else if ( prop.equalsIgnoreCase("TSProductAnnotationProviderList") ) {
		return getPropContents_TSProductAnnotationProviderList();
	}
	else if ( prop.equalsIgnoreCase("TSResultsList") ) {
		return getPropContents_TSResultsList();
	}
	else if ( prop.equalsIgnoreCase("TSResultsListSize") ) {
		return getPropContents_TSResultsListSize();
	}
	else if ( prop.equalsIgnoreCase("TSViewWindowListener") ) {
		return getPropContents_TSViewWindowListener();
	}
	else if ( prop.equalsIgnoreCase("WorkingDir") ) {
		return getPropContents_WorkingDir();
	}
	else {	String warning = "Unknown TSGetContents request \"" + prop + "\"";
			// TODO SAM 2007-02-07 Need to figure out a way to indicate
			// an error and pass back useful information.
			throw new UnrecognizedRequestException ( warning );
	}
}

/**
Handle the CreateOutput property request.
@return Boolean getPropContents_CreateOutput ()
*/
private Boolean getPropContents_CreateOutput ()
{	return new Boolean ( __tsengine.getCreateOutput() );
}

/**
Handle the DataTestList property request.
@return Vector of DataTest instances.
 */
private Vector getPropContents_DataTestList()
{
	return __tsengine.getDataTestList();
}

/**
Handle the HaveOutputPeriod property request.
@return Boolean depending on whether the output period has been set.
 */
private Boolean getPropContents_HaveOutputPeriod()
{
	boolean b = __tsengine.haveOutputPeriod();
	return new Boolean ( b );
}

/**
Handle the HydroBaseDMIList property request.
@return Vector of open HydroBaseDMI instances.
 */
private Vector getPropContents_HydroBaseDMIList()
{
	return __tsengine.getHydroBaseDMIList();
}

/**
Handle the InputEnd property request.
@return DateTime for InputEnd, or null if not set.
 */
private DateTime getPropContents_InputEnd()
{
	return __tsengine.getInputEnd();
}

/**
Handle the InputStart property request.
@return DateTime for InputStart, or null if not set.
 */
private DateTime getPropContents_InputStart()
{
	return __tsengine.getInputStart();
}

/**
Handle the OutputComments property request.  This includes, for example,
HydroBase version information that documents data available for a commands
run.
@return Vector of String containing comments for output.
 */
private Vector getPropContents_OutputComments()
{
	return __tsengine.getOutputComments();
}

/**
Handle the OutputEnd property request.
@return DateTime for OutputEnd, or null if not set.
 */
private DateTime getPropContents_OutputEnd()
{
	return __tsengine.getOutputEnd();
}

/**
Handle the OutputStart property request.
@return DateTime for OutputStart, or null if not set.
 */
private DateTime getPropContents_OutputStart()
{
	return __tsengine.getOutputStart();
}

/**
Handle the OutputYearType property request.
@return DateTime for OutputYearType, or null if not set.
 */
private String getPropContents_OutputYearType()
{
	return __tsengine.getOutputYearType();
}

/**
Handle the TSIDListNoInput property request.
@return The time series results list, as a Vector of TS.
 */
private Vector getPropContents_TSIDListNoInput()
{
	return __tsengine.getTSIDListNoInput();
}

/**
Handle the TSProductAnnotationProviderList property request.
@return The time series product annotation provider list,
as a Vector of TSProductAnnotationProvider.
 */
private Vector getPropContents_TSProductAnnotationProviderList()
{
	return __tsengine.getTSProductAnnotationProviders();
}

/**
Handle the TSResultsList property request.
@return The time series results list, as a Vector of TS.
 */
private Vector getPropContents_TSResultsList()
{
	return __tsengine.getTimeSeriesList(null);
}

/**
Handle the TSResultsListSize property request.
@return Size of the time series results list, as an Integer.
 */
private Integer getPropContents_TSResultsListSize()
{
	return new Integer( __tsengine.getTimeSeriesList(null).size());
}

/**
Handle the TSViewWindowListener property request.
@return TSViewWindowListener that listens for plot windows closing.
 */
private WindowListener getPropContents_TSViewWindowListener()
{
	return __tsengine.getTSViewWindowListener();
}

/**
Handle the WorkingDir property request.
@return The working directory, as a String.
 */
private String getPropContents_WorkingDir()
{
	//	 TODO SAM 2005-05-11 The working directory needs to be
	//	 maintained separately from the processor and the
	//	 application...
	return IOUtil.getProgramWorkingDir();
}

/**
Return the TSSupplier name.
@return the TSSupplier name ("TSEngine").
*/
public String getTSSupplierName()
{	return __tsengine.getTSSupplierName();
}

/**
Process a request, required by the CommandProcessor interface.
This is a generalized way to allow commands to call specialized functionality
through the interface without directly naming a processor.  For example, the
request may involve data that only the TSCommandProcessor has access to and
that a command does not.
Currently the following requests are handled:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Request</b></td>	<td><b>Description</b></td>
</tr>

<tr>
<td><b>CalculateTSAverageLimits</b></td>
<td>Calculate the average data limits for a time series using the averaging period
	if specified (otherwise use the available period).
	Currently only limits for monthly time series are supported.
	Parameters to this request are:
<ol>
<li>	<b>TS</b> Monthly time series to process, as TS (MonthTS) object.</li>
<li>	<b>Index</b> The index (0+) of the time series identifier being processed,
		as an Integer.</l>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TSLimits</b>the average data for a time series.
If a monthly time series, a MonthTSLimits will be returned.</li>
</ol>
</td>
</tr>

<tr>
<td><b>DateTime</b></td>
<td>Get a date/time from a string.  This is done using the following rules:
<ol>
<li>	If the string is "*" or "", return null.</li>
<li>	If the string uses a standard name OutputStart, OutputEnd,
		InputStart (previously QueryStart),
		InputEnd (previously QueryEnd), return the corresponding DateTime.</li>
<li>	Check the date/time hash table for user-defined date/times.
<li>	Parse the string.
</ol>
	Parameters to this request are:
<ol>
<li>	<b>DateTime</b> The date/time to parse, as a String.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>DateTime</b>  The resulting date/time, as a DateTime object.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetHydroBaseDMI</b></td>
<td>	Return the HydroBaseDMI that is being used.
	Parameters for this request are:
	<ol>
	<li><b>InputName</b> Input name for the DMI as a String, can be blank.</li>
	</ol>
	Returned values from this request are:
	<ol>
	<li><b>HydroBaseDMI</b> The HydroBaseDMI instance matching the input name
	 			(may return null).</li>
	</ol>
</td>
</tr>

<tr>
<td><b>GetTimeSeries</b></td>
<td>Return a time series from either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is returned, it is a new instance from the file
and should be set to null when done.  Parameters to this request are:
<ol>
<li>	<b>Index</b> The index (0+) of the time series identifier being requested,
		as an Integer.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TS</b> The requested time series or null if none is available,
		as a TS instance.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetTimeSeriesForTSID</b></td>
<td>Return a time series from either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is returned, it is a new instance from the file
and should be set to null when done.
The search is performed backwards in the
list, assuming that the commands are being processed sequentially and therefore
any reference to a duplicate ID would intuitively be referring to the latest
instance in the list.  For this version of the method, the trace (sequence
number) is ignored.  Parameters to this request are:
<ol>
<li>	<b>TSID</b> The time series identifier of the time series being requested
		(either an alias or TSIdent string), as a String.</li>
<li>	</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TS</b> The requested time series or null if none is available,
		as a TS instance.</li>
</ol>
</td>
</tr>

<tr>
<td><b>GetTimeSeriesToProcess</b></td>
<td>Return the list of time series to process, based on information that indicates
how the list can be determined.  Parameters to this request are:
<ol>
<li>	<b>TSList</b> Indicates how the list of time series for processing is to be
determined, with one of the following values:</li>
	<ol>
	<li>	"AllTS" will result in true being returned.</li>
	<li>	"AllMatchingTSID" will use the TSID value to match time series.</li>
	<li>	"LastMatchingTSID" will use the TSID value to match time series,
		returning the last match.  This is necessary for backward compatibility.
		</li>
	<li>	"SelectedTS" will result in true being returned only if the
		time series is selected.</li>
	</ol>
</ol>
Returned values from this request are:
<ol>
<li>	<b>TSToProcessList</b> The requested time series to process
		or null if none are available,
		as a Vector of TS.  Use the size of the Vector (in the first
		element) to determine the number of time series to process.  The order of
		the time series will be from first to last.</li>
<li>	<b>Indices</b> An int[] indicating the positions in the time series list,
		to be used to update the time series.</li>
</ol>
</td>
</tr>

<tr>
<td><b>IndexOf</b></td>
<td>Return the position of a time series from either the __tslist vector or the
	BinaryTS file, as appropriate.  See the similar method in TSEngine for full
	documentation.  This version assumes that no sequence number is used in the TSID.
	The search is performed backwards in order to find the time series from the
	most recent processed command.  Parameters to this request are:
<ol>
<li>	<b>TSID</b> The time series identifier or alias being requested,
		as a String.</li>
</ol>
Returned values from this request are:
<ol>
<li>	<b>Index</b> The index (0+) of the time series identifier being requested,
		as an Integer.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ProcessCommands</b></td>
<td>Process a list of commands (recursively), for example when called by the
runCommands() command.
	Parameters to this request are:
<ol>
<li>	<b>Commands</b> A list of commands to run, as a Vector of String.</li>
<li>	<b>Properties</b> Properties to control the commands, as a PropList - note
		that all properties should be String, as per the TSEngine properties.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ProcessTimeSeriesAction</b></td>
<td>Process a time series action, meaning insert or update in the list.
	Parameters to this request are:
<ol>
<li>	<b>Action</b> "INSERT" to insert at the position,
		"UPDATE" to update at the position,
		"NONE" to do to nothing, as a String.</li>
<li>	<b>TS</b> The time series to act on, as TS object.</li>
<li>	<b>Index</b> The index (0+) of the time series identifier being processed,
		as an Integer.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>ReadTimeSeries2</b></td>
<td> Process a list of time series after the initial read.  This does NOT add the
time series to the list (use setTimeSeries() or other commands to do this).
This method should be called after time series are read.
This method does the following:
<ol>
<li>	Sets the legend to "" (this unsets legend information that was
	previously required with the old graph package).</li>
<li>	If the description is not set, sets it to the location.</li>
<li>	If a missing data range has been set, indicate it to the time series.
	This may be phased out.</li>
<li>	If the time series identifier needs to be reset to something known to
	the read code, reset it (using the non-null tsident_string parameter
	that is passed in).</li>
<li>	Compute the historic averages for the raw data so that it is available
	later for filling.</li>
<li>	If the output period is specified, make sure that the time series
	period includes the output period.  For important time series, the
	available period may already include the output period.  For time series
	that are being filled, it is likely that the available period will need
	to be extended to include the output period.</li>
</ol>
Parameters to this request are:
<ol>
<li>	<b>TSList</b> List of time series to process, as a Vector of TS.</li>
</ol>
Returned values from this request are:
<ol>
<li>	None.</li>
</ol>
</td>
</tr>

<tr>
<td><b>TSIDListNoInput</b></td>
<td>The Vector of time series identifiers that are available, without the
input type and name.  The time series identifiers from commands above the
selected command are returned.  This property will normally only be used with
command editor dialogs.</td>
</tr>

</table>
@param request_params An optional list of parameters to be used in the request.
@exception Exception if the request cannot be processed.
@return the results of a request, or null if a value is not found.
*/
public CommandProcessorRequestResultsBean processRequest ( String request, PropList request_params )
throws Exception
{	//return __tsengine.getPropContents ( prop );
	if ( request.equalsIgnoreCase("CalculateTSAverageLimits") ) {
		return processRequest_CalculateTSAverageLimits ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("DateTime") ) {
		return processRequest_DateTime ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetHydroBaseDMI") ) {
		return processRequest_GetHydroBaseDMI ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetTimeSeries") ) {
		return processRequest_GetTimeSeries ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetTimeSeriesForTSID") ) {
		return processRequest_GetTimeSeriesForTSID ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("GetTimeSeriesToProcess") ) {
		return processRequest_GetTimeSeriesToProcess ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("IndexOf") ) {
		return processRequest_IndexOf ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("ProcessCommands") ) {
		return processRequest_ProcessCommands ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("ProcessTimeSeriesAction") ) {
		return processRequest_ProcessTimeSeriesAction ( request, request_params );
	}
	else if ( request.equalsIgnoreCase("ReadTimeSeries2") ) {
		return processRequest_ReadTimeSeries2 ( request, request_params );
	}
	else {
		TSCommandProcessorRequestResultsBean bean =
			new TSCommandProcessorRequestResultsBean();
		String warning = "Unknown TSCommandProcessor request \"" +
		request + "\"";
		bean.setWarningText( warning );
		// TODO SAM 2007-02-07 Need to figure out a way to indicate
		// an error and pass back useful information.
		throw new UnrecognizedRequestException ( warning );
	}
}

/**
Process the CalculateTSAverageLimits request.
*/
private CommandProcessorRequestResultsBean processRequest_CalculateTSAverageLimits (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	// Time series...
	Object o_TS = request_params.getContents ( "TS" );
	if ( o_TS == null ) {
			String warning =
				"Request ProcessTimeSeriesAction() does not provide a TS parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	TS ts = (TS)o_TS;
	TSLimits tslimits = __tsengine.calculateTSAverageLimits(ts);
	// Return the limits.
	PropList results = bean.getResultsPropList();
	results.setUsingObject ( "TSLimits", tslimits );
	return bean;
}

/**
Process the DateTime request.
*/
private CommandProcessorRequestResultsBean processRequest_DateTime (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "DateTime" );
	if ( o == null ) {
			String warning =
				"Request DateTime() does not provide a DateTime parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String DateTime = (String)o;
	DateTime dt = __tsengine.getDateTime ( DateTime );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	results.setUsingObject("DateTime", dt );
	return bean;
}

/**
Process the GetHydroBaseDMI request.
*/
private CommandProcessorRequestResultsBean processRequest_GetHydroBaseDMI (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "InputName" );
	if ( o == null ) {
			String warning =
				"Request GetHydroBaseDMI() does not provide an InputName parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String InputName = (String)o;
	HydroBaseDMI dmi = __tsengine.getHydroBaseDMI( InputName );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	results.setUsingObject("HydroBaseDMI", dmi );
	return bean;
}

/**
Process the GetTimeSeries request.
*/
private CommandProcessorRequestResultsBean processRequest_GetTimeSeries (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "Index" );
	if ( o == null ) {
			String warning = "Request IndexOf() does not provide an Index parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Integer Index = (Integer)o;
	TS ts = __tsengine.getTimeSeries ( Index.intValue() );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	results.setUsingObject("TS", ts );
	return bean;
}

/**
Process the GetTimeSeriesForTSID request.
*/
private CommandProcessorRequestResultsBean processRequest_GetTimeSeriesForTSID (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "TSID" );
	if ( o == null ) {
			String warning =
				"Request GetTimeSeriesForTSID() does not provide a TSID parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String TSID = (String)o;
	o = request_params.getContents ( "CommandTag" );
	String CommandTag = "";
	if ( o != null ) {
		CommandTag = (String)o;
	}
	TS ts = __tsengine.getTimeSeries ( CommandTag, TSID );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	results.setUsingObject("TS", ts );
	return bean;
}

/**
Process the GetTimeSeriesToProcess request.
*/
private CommandProcessorRequestResultsBean processRequest_GetTimeSeriesToProcess (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "TSList" );
	if ( o == null ) {
			String warning = "Request GetTimeSeriesToProcess() does not provide a TSList parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	// Else continue...
	String TSList = (String)o;
	// The following can be null.  Let the called code handle it.
	Object o_TSID = request_params.getContents ( "TSID" );
	String TSID = null;
	if ( o_TSID != null ) {
		TSID = (String)o_TSID;
	}
	// Get the information from TSEngine, which is returned as a Vector
	// with the first element being the matching time series list and the second
	// being the indices of those time series in the time series results list.
	Vector tslist = __tsengine.getTimeSeriesToProcess ( TSList, TSID );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	Message.printStatus(2,"From TSEngine",
			((Vector)(tslist.elementAt(0))).toString() );
	results.setUsingObject("TSToProcessList", (Vector)(tslist.elementAt(0)) );
	results.setUsingObject("Indices", (int [])(tslist.elementAt(1)) );
	return bean;
}

/**
Process the IndexOf request.
*/
private CommandProcessorRequestResultsBean processRequest_IndexOf (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "TSID" );
	if ( o == null ) {
			String warning = "Request IndexOf() does not provide a TSID parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String TSID = (String)o;
	int index = __tsengine.indexOf ( TSID );
	PropList results = bean.getResultsPropList();
	// This will be set in the bean because the PropList is a reference...
	results.setUsingObject("Index", new Integer(index));
	return bean;
}

/**
Process the ProcessTimeSeriesAction request.
*/
private CommandProcessorRequestResultsBean processRequest_ProcessCommands (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	// Time series...
	Object o_Commands = request_params.getContents ( "Commands" );
	if ( o_Commands == null ) {
			String warning =
				"Request ProcessCommands() does not provide a Commands parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Vector commands = (Vector)o_Commands;
	Object o_Properties = request_params.getContents ( "Properties" );
	if ( o_Properties == null ) {
			String warning =
				"Request ProcessCommands() does not provide a Properties parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	__tsengine.processCommands( commands, (PropList)o_Properties );
	// No results need to be set in the bean.
	return bean;
}

/**
Process the ProcessTimeSeriesAction request.
*/
private CommandProcessorRequestResultsBean processRequest_ProcessTimeSeriesAction (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	// Time series...
	Object o_TS = request_params.getContents ( "TS" );
	if ( o_TS == null ) {
			String warning =
				"Request ProcessTimeSeriesAction() does not provide a TS parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	TS ts = (TS)o_TS;
	// Action...
	Object o_Action = request_params.getContents ( "Action" );
	if ( o_Action == null ) {
			String warning =
				"Request ProcessTimeSeriesAction() does not provide an Action parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	String Action = (String)o_Action;
	// TODO SAM 2007-02-11 Need to handle actions as strings cleaner.
	int Action_int = NONE;
	if ( Action.equalsIgnoreCase("Insert") ) {
		Action_int = INSERT_TS;
	}
	else if ( Action.equalsIgnoreCase("Update") ) {
		Action_int = UPDATE_TS;
	}
	else { String warning = "Request ProcessTimeSeriesAction() Action value \"" +
			Action + "\" is invalid.";
			throw new RequestParameterInvalidException ( warning );
	}
	// Index...
	Object o_Index = request_params.getContents ( "Index" );
	if ( o_Index == null ) {
			String warning =
				"Request ProcessTimeSeriesAction() does not provide an Index parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Integer Index = (Integer)o_Index;
	__tsengine.processTimeSeriesAction( Action_int, ts, Index.intValue() );
	// No results need to be set in the bean.
	return bean;
}

/**
Process the IndexOf request.
*/
private CommandProcessorRequestResultsBean processRequest_ReadTimeSeries2 (
		String request, PropList request_params )
throws Exception
{	TSCommandProcessorRequestResultsBean bean =
		new TSCommandProcessorRequestResultsBean();
	// Get the necessary parameters...
	Object o = request_params.getContents ( "TSList" );
	if ( o == null ) {
			String warning = "Request ReadTimeSeries2() does not provide a TSList parameter.";
			bean.setWarningText ( warning );
			bean.setWarningRecommendationText (
					"This is likely a software code error.");
			throw new RequestParameterNotFoundException ( warning );
	}
	Vector TSList = (Vector)o;
	__tsengine.readTimeSeries2 ( TSList );
	//PropList results = bean.getResultsPropList();
	// No data are returned in the bean.
	return bean;
}

/**
Method for TSSupplier interface.
Read a time series given a time series identifier string.  The string may be
a file name if the time series are stored in files, or may be a true identifier
string if the time series is stored in a database.  The specified period is
read.  The data are converted to the requested units.
@param tsident_string Time series identifier or file name to read.
@param req_date1 First date to query.  If specified as null the entire period
will be read.
@param req_date2 Last date to query.  If specified as null the entire period
will be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public TS readTimeSeries (	String tsident_string,
				DateTime req_date1, DateTime req_date2,
				String req_units,
				boolean read_data )
throws Exception
{	return __tsengine.readTimeSeries ( tsident_string, req_date1,
		req_date2, req_units, read_data );
}

/**
Method for TSSupplier interface.
Read a time series given an existing time series and a file name.
The specified period is read.
The data are converted to the requested units.
@param req_ts Requested time series to fill.  If null, return a new time series.
If not null, all data are reset, except for the identifier, which is assumed
to have been set in the calling code.  This can be used to query a single
time series from a file that contains multiple time series.
@param fname File name to read.
@param date1 First date to query.  If specified as null the entire period will
be read.
@param date2 Last date to query.  If specified as null the entire period will
be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public TS readTimeSeries (	TS req_ts, String fname,
				DateTime date1, DateTime date2,
				String req_units,
				boolean read_data )
throws Exception
{	return __tsengine.readTimeSeries ( req_ts, fname, date1, date2,
			req_units, read_data );
}

/**
Method for TSSupplier interface.
Read a time series list from a file (this is typically used used where a time
series file can contain one or more time series).
The specified period is
read.  The data are converted to the requested units.
@param fname File to read.
@param date1 First date to query.  If specified as null the entire period will
be read.
@param date2 Last date to query.  If specified as null the entire period will
be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Vector of time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public Vector readTimeSeriesList (	String fname,
					DateTime date1, DateTime date2,
					String req_units,
					boolean read_data )
throws Exception
{	return __tsengine.readTimeSeriesList ( fname, date1, date2, req_units,
				read_data );
}

/**
Method for TSSupplier interface.
Read a time series list from a file or database using the time series identifier
information as a query pattern.
The specified period is
read.  The data are converted to the requested units.
@param tsident A TSIdent instance that indicates which time series to query.
If the identifier parts are empty, they will be ignored in the selection.  If
set to "*", then any time series identifier matching the field will be selected.
If set to a literal string, the identifier field must match exactly to be
selected.
@param fname File to read.
@param date1 First date to query.  If specified as null the entire period will
be read.
@param date2 Last date to query.  If specified as null the entire period will
be read.
@param req_units Requested units to return data.  If specified as null or an
empty string the units will not be converted.
@param read_data if true, the data will be read.  If false, only the time series
header will be read.
@return Vector of time series of appropriate type (e.g., MonthTS, HourTS).
@exception Exception if an error occurs during the read.
*/
public Vector readTimeSeriesList (	TSIdent tsident, String fname,
					DateTime date1, DateTime date2,
					String req_units,
					boolean read_data )
throws Exception {
	return __tsengine.readTimeSeriesList ( tsident, fname, date1, date2,
				req_units, read_data );
}

/**
Set the data for a named property, required by the CommandProcessor
interface.  See the getPropContents method for a list of properties that are
handled.  This method simply calls setPropContents () using the information in
the Prop instance.
@param prop Property to set.
@return the named property, or null if a value is not found.
@exception Exception if there is an error setting the property.
*/
public void setProp ( Prop prop ) throws Exception
{	setPropContents ( prop.getKey(), prop.getContents() );
}

/**
Set the contents for a named property, required by the CommandProcessor
interface.  The following properties are handled.
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>
</tr>

<tr>
<td><b>DataTestList</b></td>
<td>A Vector of DataTest, to be processed when evaluating data.
</td>
</tr>

<tr>
<td><b>HydroBaseDMIList</b></td>
<td>A Vector of open HydroBaseDMI, to be used by other code for reading data.
</td>
</tr>

<tr>
<td><b>InputEnd</b></td>
<td>The date/time for the end of reading data, as a DateTime.
</td>
</tr>

<tr>
<td><b>InputStart</b></td>
<td>The date/time for the start of reading data, as a DateTime.
</td>
</tr>

<tr>
<td><b>TSResultsList</b></td>
<td>The list of time series results, as a Vector of TS.
</td>
</tr>

</table>
@exception Exception if there is an error setting the properties.
*/
public void setPropContents ( String prop, Object contents ) throws Exception
{	if ( prop.equalsIgnoreCase("DataTestList" ) ) {
		__tsengine.setDataTestList ( (Vector)contents );
	}
	else if ( prop.equalsIgnoreCase("HydroBaseDMIList" ) ) {
		__tsengine.setHydroBaseDMIList ( (Vector)contents );
	}
	else if ( prop.equalsIgnoreCase("InputEnd") ) {
		__tsengine.setInputEnd ( (DateTime)contents );
	}
	else if ( prop.equalsIgnoreCase("InputStart") ) {
		__tsengine.setInputStart ( (DateTime)contents );
	}
	else if ( prop.equalsIgnoreCase("TSResultsList") ) {
		__tsengine.setTimeSeriesList ( (Vector)contents );
	}
	else {// Not recognized...
		String message = "Unable to set data for unknown property \"" +
		prop +	"\".";
		throw new UnrecognizedRequestException ( message );
	}
}

}
