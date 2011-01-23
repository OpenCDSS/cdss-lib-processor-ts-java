package rti.tscommandprocessor.commands.rccacis;

import java.util.List;

import RTi.Util.Time.DateTime;

/**
Metadata for station time series joined data.  The data correspond to the MultiStn request metadata.
*/
public class RccAcisStationTimeSeriesMetadataList
{

private List<RccAcisStationTimeSeriesMetadata> data;

/**
Default constructor, required by GSON.
*/
public RccAcisStationTimeSeriesMetadataList ()
{
}

public List<RccAcisStationTimeSeriesMetadata> getData ()
{
    return data;
}

/**
Clean up data after reading - necessary because JSON may have empty arrays, values incompatible with internal
packages, etc.
Make sure that the valid period has two strings, even if empty.
Also adjust dates with year 9999 to be current year.
*/
public void cleanupData ()
{
    List<RccAcisStationTimeSeriesMetadata> data = getData();
    DateTime now = new DateTime(DateTime.DATE_CURRENT);
    RccAcisStationTimeSeriesMetadata metadata;
    for ( int i = 0; i < data.size(); i++ ) {
        metadata = data.get(i);
        String dates[] = metadata.getValid_daterange();
        if ( (dates == null) || (dates.length == 0) ) {
            // Did not have data in the original so insert defaults
            dates = new String[2];
            dates[0] = "";
            dates[1] = "";
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
            if ( dates[0].startsWith("9999") ) {
                dates[0] = "" + now.getYear() + dates[0].substring(4);
            }
            if ( dates[1].startsWith("9999") ) {
                dates[1] = "" + now.getYear() + dates[1].substring(4);
            }
        //}
     }
    setData(data);
}

public void setData ( List<RccAcisStationTimeSeriesMetadata> data2 )
{
    data = data2;
}

}