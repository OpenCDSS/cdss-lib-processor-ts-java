// HydroJSONTimeSeriesValue - Definition of time series value for HydroJSON format.

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
Definition of time series value for HydroJSON format.
*/
public class HydroJSONTimeSeriesValue
{

// List these in the order of the specification example
/**
Timestamp - use a string to make sure it formats correctly.
*/
private String timestamp = null;

/**
Value.
*/
private Double value = null;

/**
Quality.
*/
private String quality = "";

/**
Constructor.
*/
public HydroJSONTimeSeriesValue()
{
	
}

/**
Return the timestamp.
*/
public String getTimestamp()
{
	return this.timestamp;
}

/**
Return the value.
*/
public Double getValue()
{
	return this.value;
}

/**
Return the quality.
*/
public String getQuality()
{
	return this.quality;
}

/**
Set the timestamp.
*/
public void setTimestamp ( String timestamp )
{
	this.timestamp = timestamp;
}

/**
Set the value.
*/
public void setValue ( Double value )
{
	this.value = value;
}

/**
Set the quality.
*/
public void setQuality ( String quality )
{
	this.quality = quality;
}

}
