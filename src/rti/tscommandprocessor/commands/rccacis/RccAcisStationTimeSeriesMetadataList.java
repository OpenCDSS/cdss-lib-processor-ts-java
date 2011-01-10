package rti.tscommandprocessor.commands.rccacis;

import java.util.List;

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

public void setData ( List<RccAcisStationTimeSeriesMetadata> data2 )
{
    data = data2;
}

}