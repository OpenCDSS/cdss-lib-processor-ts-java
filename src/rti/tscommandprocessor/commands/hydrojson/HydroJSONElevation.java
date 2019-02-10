// HydroJSONElevation - Definition of station elevation for HydroJSON format.

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
Definition of station elevation for HydroJSON format.
*/
public class HydroJSONElevation
{

// List these in the order of the specification example
/**
Elevation value.
*/
private Double value = null;

/**
Accuracy.
*/
private Double accuracy = null;

/**
Elevation datum.
*/
private String datum = "";

/**
Method of determining elevation.
*/
private String method = "";

/**
Constructor.
*/
public HydroJSONElevation()
{
	
}

/**
Return the value.
*/
public Double getValue()
{
	return this.value;
}

/**
Return the accuracy.
*/
public Double getAccuracy()
{
	return this.accuracy;
}

/**
Return the datum.
*/
public String getDatum()
{
	return this.datum;
}

/**
Return the method of determining the elevation.
*/
public String getMethod()
{
	return this.method;
}

/**
Set the value.
*/
public void setValue ( Double value )
{
	this.value = value;
}

/**
Set the accuracy.
*/
public void setAccuracy ( Double accuracy)
{
	this.accuracy = accuracy;
}

/**
Set the datum.
*/
public void setDatum ( String datum )
{
	this.datum = datum;
}

/**
Set the method of determining the elevation.
*/
public void setMethod ( String method)
{
	this.method = method;
}

}
