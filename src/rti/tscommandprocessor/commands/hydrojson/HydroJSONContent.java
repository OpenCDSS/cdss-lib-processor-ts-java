// HydroJSONContent - This class stores content to the serialized to/from a HydroJSON file.

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
