// RccAcisStationTimeSeriesMetadataList - Metadata for station time series joined data

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

package rti.tscommandprocessor.commands.rccacis;

import java.util.List;
import java.util.Vector;

import RTi.Util.Time.DateTime;

/**
Metadata for station time series joined data.  The data correspond to the MultiStn request in versions 1 and 2 and
StaMeta in version 2 (preferred for retrieving metadata).  GSON sets the data members directly,
where the names must match the JSON elements.
The getMeta() method is used to retrieve the parsed data.  Calling code should use the version 2+ conventions
but the version 1 data will be returned if it was parsed.
*/
public class RccAcisStationTimeSeriesMetadataList
{

/**
The list of meta elements from the JSON data for version 1 API ("data" element).
*/
private List<RccAcisStationTimeSeriesMetadata> data = null;    
    
/**
The list of meta elements from the JSON data ("meta" in version 2).
*/
private List<RccAcisStationTimeSeriesMetadata> meta = null;

/**
Default constructor, required by GSON.
*/
public RccAcisStationTimeSeriesMetadataList ()
{
}

/**
Return the list of "meta" elements, used with version 2+ API, guaranteed to be non-null.
@return the list of "meta" elements
*/
public List<RccAcisStationTimeSeriesMetadata> getMeta ()
{
    List<RccAcisStationTimeSeriesMetadata> metaList = null;
    if ( this.data != null ) {
        metaList = this.data;
    }
    else {
        metaList = this.meta;
    }
    if ( metaList == null ) {
        metaList = new Vector<RccAcisStationTimeSeriesMetadata>();
    }
    return metaList;
}

/**
Clean up data after reading - necessary because JSON may have empty arrays, values incompatible with internal
packages, etc.
Make sure that the valid period has two strings, even if empty.
Also adjust dates with year 9999 to be current year.
*/
public void cleanupData ()
{
    List<RccAcisStationTimeSeriesMetadata> metaList = getMeta();
    DateTime now = new DateTime(DateTime.DATE_CURRENT);
    RccAcisStationTimeSeriesMetadata metadata;
    for ( int i = 0; i < metaList.size(); i++ ) {
        metadata = metaList.get(i);
        String dates[][] = metadata.getValid_daterange();
        if ( (dates == null) || (dates[0].length == 0) ) {
            // Did not have data in the original so insert defaults
            dates = new String[1][2];
            dates[0][0] = "";
            dates[0][1] = "";
            metadata.setValid_daterange(dates);
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

/**
Set the metadata list.
@param meta metadata list
*/
public void setMeta ( List<RccAcisStationTimeSeriesMetadata> meta )
{
    this.meta = meta;
}

}
