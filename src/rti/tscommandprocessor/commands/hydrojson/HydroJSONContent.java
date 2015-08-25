package rti.tscommandprocessor.commands.hydrojson;

import java.util.ArrayList;
import java.util.List;

/**
This class stores content to the serialized to/from a HydroJSON file.
Code that uses this class to write HydroJSON should instantiate an instance and then set values.
The contents of the instance can then be written, for example with GSON.
*/
public class HydroJSONContent
{

/**
Station list.  Each station will be output with its properties and time series for the station.
*/
private List<HydroJSONStation> stationList = new ArrayList<HydroJSONStation>();

/**
Constructor.
*/
public HydroJSONContent ()
{
}

/**
Set the station list.
*/
public void setStationList ( List<HydroJSONStation> stationList )
{
	this.stationList = stationList;
}

/**
Return the station list.
*/
public List<HydroJSONStation> getStationList ()
{
	return stationList;
}

}