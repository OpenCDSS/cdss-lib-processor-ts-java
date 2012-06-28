package rti.tscommandprocessor.commands.rccacis;

import RTi.Util.Time.DateTime;

/**
Object to match JSON structure for version 2 API StnMeta method.  GSON sets the data members directly,
where the names must match the JSON elements.
The getMeta() method is used to retrieve the parsed metadata and getData() to retrieve the parsed time series
records.  Calling code should use the version 2+ conventions because version 1 does not use this code.
*/
public class RccAcisStationTimeSeriesMetaAndData
{

/**
The list of meta elements from the JSON data for version 1 API ("data" element).
*/
private String [][] data = null;
    
/**
The list of meta elements from the JSON data ("meta" in version 2).
*/
private RccAcisStationTimeSeriesMetadata meta = null;

/**
Default constructor, required by GSON.
*/
public RccAcisStationTimeSeriesMetaAndData ()
{
}

/**
Return the list of "data" elements, used with version 2+ API, guaranteed to be non-null.
@return the list of "data" elements
*/
public String [][] getData ()
{
    if ( this.data == null ) {
        return new String[0][0];
    }
    else {
        return this.data;
    }
}

/**
Return the list of "meta" elements, used with version 2+ API, guaranteed to be non-null.
@return the list of "meta" elements
*/
public RccAcisStationTimeSeriesMetadata getMeta ()
{
    return this.meta;
}

/**
Clean up data after reading - necessary because JSON may have empty arrays, values incompatible with internal
packages, etc.
Make sure that the valid period has two strings, even if empty.
Also adjust dates with year 9999 to be current year.
*/
public void cleanupData ()
{
    RccAcisStationTimeSeriesMetadata meta = getMeta();
    DateTime now = new DateTime(DateTime.DATE_CURRENT);
    String dates[][] = meta.getValid_daterange();
    if ( (dates == null) || (dates.length == 0) ) {
        // Did not have data in the original so insert defaults
        dates = new String[1][2];
        dates[0][0] = "";
        dates[0][1] = "";
        meta.setValid_daterange(dates);
    }
    // TODO SAM 2011-01-21 Evaluate whether needed - previously had some other code that was causing problems
    /*
    if ( (dates[0].trim().length() == 0) && (dates[1].trim().length() == 0) ) {
        // No period so remove the item and decrement the counter.
        data.remove(metadata);
        --i;
    }
    else {*/
        if ( dates[0][0].startsWith("9999") ) {
            dates[0][0] = "" + now.getYear() + dates[0][0].substring(4);
        }
        if ( dates[0][1].startsWith("9999") ) {
            dates[0][1] = "" + now.getYear() + dates[0][1].substring(4);
        }
    //}
}

}