// HydroJSONStation - Station definition for HydroJSON format.

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
Station definition for HydroJSON format.
*/
public class HydroJSONStation
{

// List these in the order of the specification example, with spellings that match the specification.
/**
Name of station.
*/
private String name = "";

/**
Responsibility for station.
*/
private String responsibility = "";

/**
Coordinates for station.
*/
private HydroJSONCoordinates coordinates = new HydroJSONCoordinates();

/**
Hydrologic unit code (huc) for station.
*/
private String huc = "";

/**
Elevation for station.
*/
private HydroJSONElevation elevation = new HydroJSONElevation();

/**
Time zone.
*/
private String timezone = "";

/**
Time zone offset.
*/
private String tz_offset = "";

/**
Time format.
*/
private String time_format = "";

/**
Active flag.
*/
private String active_flag = "";

/**
Location type.
*/
private String location_type = "";

/**
List of time series for the station.
*/
private List<HydroJSONTimeSeries> timeseries = new ArrayList<HydroJSONTimeSeries>();

/**
Constructor.
*/
public HydroJSONStation( String name )
{
	this.name = name;
}

/**
Return the station active flag.
*/
public String getActiveFlag()
{
	return this.active_flag;
}

/**
Return the coordinates object.
*/
public HydroJSONCoordinates getCoordinates()
{
	return this.coordinates;
}

/**
Return the elevation object.
*/
public HydroJSONElevation getElevation()
{
	return this.elevation;
}

/**
Return the station HUC.
*/
public String getHuc()
{
	return this.huc;
}

/**
Return the location type.
*/
public String getLocationType()
{
	return this.location_type;
}

/**
Return the station name.
*/
public String getName()
{
	return this.name;
}

/**
Return the station responsibility.
*/
public String getResponsibility()
{
	return this.responsibility;
}

/**
Return the station time format.
*/
public String getTimeFormat()
{
	return this.time_format;
}

/**
Return the station time zone.
*/
public String getTimeZone()
{
	return this.timezone;
}

/**
Return the station time zone offset.
*/
public String getTimeZoneOffset()
{
	return this.tz_offset;
}

/**
Return the time series list.
*/
public List<HydroJSONTimeSeries> getTimeSeriesList()
{
	return this.timeseries;
}

/**
Set the active flag.
*/
public void setActiveFlag ( String active_flag)
{
	this.active_flag = active_flag;
}

/**
Set the HUC.
*/
public void setHuc ( String huc )
{
	this.huc = huc;
}

/**
Set the location type.
*/
public void setLocationType ( String location_type )
{
	this.location_type = location_type;
}

/**
Set the station name.
*/
public void setName ( String name )
{
	this.name = name;
}

/**
Set the station name.
*/
public void setResponsibility ( String responsibility )
{
	this.responsibility = responsibility;
}

/**
Set the time series list.
*/
public void setTimeSeriesList ( List<HydroJSONTimeSeries> timeSeriesList )
{
	this.timeseries = timeSeriesList;
}

/**
Set the time zone.
*/
public void setTimeZone ( String timezone )
{
	this.timezone = timezone;
}

/**
Set the time zone offset.
*/
public void setTimeZoneOffset ( String tz_offset )
{
	this.tz_offset = tz_offset;
}

/**
Set the time format.
*/
public void setTimeFormat ( String time_format)
{
	this.time_format = time_format;
}

}
