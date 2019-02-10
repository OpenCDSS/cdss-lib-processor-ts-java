// HydroJSONTimeSeriesSiteQuality - Definition of time series site quality for HydroJSON format.

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

import RTi.Util.Time.DateTime;

/**
Definition of time series site quality for HydroJSON format.
*/
public class HydroJSONTimeSeriesSiteQuality
{

// List these in the order of the specification example
/**
Timestamp.
*/
private DateTime timestamp = null;

/**
value.
*/
private Double value = null;

/**
Constructor.
*/
public HydroJSONTimeSeriesSiteQuality()
{
	
}

/**
Return the timestamp.
*/
public DateTime getTimestamp()
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

}
