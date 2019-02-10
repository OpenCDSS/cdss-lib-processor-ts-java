// HydroJSONCoordinates - Definition of station coordinates for HydroJSON format.

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

/**
Definition of station coordinates for HydroJSON format.
*/
public class HydroJSONCoordinates
{

// List these in the order of the specification example
/**
Latitude.
*/
private Double latitude = null;

/**
Longitude.
*/
private Double longitude = null;

/**
Coordinate datum.
*/
private String datum = "";

/**
Constructor.
*/
public HydroJSONCoordinates()
{
	
}

/**
Return the datum.
*/
public String getDatum()
{
	return this.datum;
}

/**
Return the latitude.
*/
public Double getLatitude()
{
	return this.latitude;
}

/**
Return the longitude.
*/
public Double getLongitude()
{
	return this.longitude;
}

/**
Set the datum.
*/
public void setDatum ( String datum )
{
	this.datum = datum;
}

/**
Set the latitude.
*/
public void setLatitude ( Double latitude )
{
	this.latitude = latitude;
}

/**
Set the latitude.
*/
public void setLongitude ( Double longitude )
{
	this.longitude = longitude;
}

}
