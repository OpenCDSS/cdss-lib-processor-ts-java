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
//------------------------------------------------------------------------------
// EndHeader

package RTi.TS;

import java.util.Vector;

import DWR.DMI.tstool.TSEngine;

import RTi.Util.IO.Command;
import RTi.Util.IO.CommandFactory;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.startLog_Command;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;

// REVISIT SAM 2005-05-19 When TSEngine is sufficiently clean, merge its code
// into this class and rework the connection to the TSTool application.
/**
This class processes time series commaands and manages the relevant data.
*/
public class TSCommandProcessor implements CommandProcessor, TSSupplier
{

private TSEngine __tsengine = null;	// Map back to existing code until able
					// to transition into a true
					// TSCommandProcessor class, at which
					// time TSEngine will go away.

public TSCommandProcessor ()
{	super();
}

public TSCommandProcessor ( TSEngine tsengine )
{	__tsengine = tsengine;
}

/**
Return data for a named property, required by the CommandProcessor
interface.  See the overloaded version for a list of properties that are
handled.
@param prop Property to set.
@return the named property, or null if a value is not found.
*/
public Prop getProp ( String prop )
{	return __tsengine.getProp ( prop );
}

/**
Return the contents for a named property, required by the CommandProcessor
interface. Currently the following properties are handled:
<table width=100% cellpadding=10 cellspacing=0 border=2>
<tr>
<td><b>Property</b></td>	<td><b>Description</b></td>
</tr>

<tr>
<td><b>DataTestList</b></td>
<td>The Vector of DataTests.</td>
</tr>

<tr>
<td><b>OutputYearType</b></td>
<td>The output year type.
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
<td><b>TSResultsList</b></td>
<td>The Vector of time series results.</td>
</tr>

<tr>
<td><b>WorkingDir</b></td>
<td>The working directory for the processor (initially the same as the
application but may be changed by commands during execution).</td>
</tr>

</table>
@return the contents for a named property, or null if a value is not found.
*/
public Object getPropContents ( String prop )
{	return __tsengine.getPropContents ( prop );
}

/**
Get a date/time from a string.  This is done using the following rules:
<ol>
<li>	If the string is "*" or "", return null.</li>
<li>	If the string uses a standard name OutputStart, OutputEnd, QueryStart,
	QueryEnd, return the corresponding DateTime.</li>
<li>	Check the date/time hash table for user-defined date/times.
<li>	Parse the string.
</ol>
@param date_string Date/time string to parse.
@exception if the date cannot be determined using the defined procedure.
*/
public DateTime getDateTime ( String date_string )
throws Exception
{	return __tsengine.getDateTime ( date_string );
}

/**
Return a time series from either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is returned, it is a new instance from the file
and should be set to null when done.
The search is performed backwards in the
list, assuming that the commands are being processed sequentially and therefore
any reference to a duplicate ID would intuitively be referring to the latest
instance in the list.  For this version of the method, the trace (sequence
number) is ignored.
@param id Time series identifier (either an alias or TSIdent string).
@param command_tag Command number used with messaging.
@return a time series from the requested position or null if none is available.
@exception Exception if there is an error getting the time series.
*/
public TS getTimeSeries ( String command_tag, String id )
throws Exception
{	return __tsengine.getTimeSeries ( command_tag, id );
}

/**
Return a time series from either the __tslist vector or the BinaryTS file,
as appropriate.  If a BinaryTS is returned, it is a new instance from the file
and should be set to null when done.
@param position Position in time series list (0 index).
@return a time series from the requested position or null if none is available.
@exception Exception if there is an error reading the time series.
*/
public TS getTimeSeries ( int position )
throws Exception
{	return __tsengine.getTimeSeries ( position );
}

/**
Return the list of time series to process, based on information that indicates
how the list can be determined.
@param TSList Indicates how the list of time series for processing is to be
determined, with one of the following values:
<ol>
<li>	"AllTS" will result in true being returned.</li>
<li>	"AllMatchingTSID" will use the TSID value to match time series.</li>
<li>	"LastMatchingTSID" will use the TSID value to match time series,
	returning the last match.  This is necessary for backward compatibility.
	</li>
<li>	"SelectedTS" will result in true being returned only if the
	time series is selected.</li>
</ol>
@return A Vector that has as its first element a Vector of TS to process and as
its second element an int[] indicating the positions in the time series list,
to be used to update the time series.  Use the size of the Vector (in the first
element) to determine the number of time series to process.  The order of the
time series will be from first to last.
*/
public Vector getTimeSeriesToProcess ( String TSList, String TSID )
{	return __tsengine.getTimeSeriesToProcess ( TSList, TSID );
}

/**
Return the TSSupplier name.
@return the TSSupplier name ("TSEngine").
*/
public String getTSSupplierName()
{	return __tsengine.getTSSupplierName();
}

/**
Return the position of a time series from either the __tslist vector or the
BinaryTS file, as appropriate.  See the overloaded method for full
documentation.  This version assumes that no sequence number is used.
The search is performed backwards in order to find the time series from the
most recent previous command.
@param string the alias and/or time series identifier to look for.
@return Position in time series list (0 index), or -1 if not in the list.
*/
public int indexOf ( String string )
{	return __tsengine.indexOf ( string );
}

// REVISIT SAM 2006-05-02
// Need to evaluate how to make this more generic.  This was put in place to
// support the runCommands() command.
/**
Process a list of commands.
@param commands a Vector of command strings to process.
@param props Properties to control processing (see the TSEngine documentation).
*/
public void processCommands ( Vector commands, PropList props )
throws Exception
{	__tsengine.processCommands ( commands, props );
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
Process a list of time series after the initial read.  This does NOT add the
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
@param ts Time series to process.
@param tsident_string Time series identifier string.  If null, take from the
time series.
@exception Exception if there is an error processing the time series.
*/
public void readTimeSeries2 ( Vector tslist )
throws Exception
{	__tsengine.readTimeSeries2 ( tslist );
}

/**
Set the data for a named property, required by the CommandProcessor
interface.  See the getPropContents method for a list of properties that are
handled.
@param prop Property to set.
@return the named property, or null if a value is not found.
*/
public void setProp ( Prop prop )
{	__tsengine.setProp ( prop );	
}

/**
Set the contents for a named property, required by the CommandProcessor
interface.  See the getPropContents method for a list of properties that are
handled.
*/
public void setPropContents ( String prop, Object contents )
{	__tsengine.setPropContents ( prop, contents );	
}

/**
Set the time series in memory at the specific position.
@param ts time series to set.
@param position Position in time series list (0 index).
@exception Exception if there is an error saving the time series.
*/
public void setTimeSeries ( TS ts, int position )
throws Exception
{	__tsengine.setTimeSeries ( ts, position );
}

}
