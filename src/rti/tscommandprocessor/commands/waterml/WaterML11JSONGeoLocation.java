// WaterML11JSONGeoLocation - Corresponds to "{ value: { timeseries: sourceInfo : geoLocation" WaterML 1.1 JSON

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

package rti.tscommandprocessor.commands.waterml;

/**
Corresponds to "{ value: { timeseries: sourceInfo : geoLocation" WaterML 1.1 JSON
*/
public class WaterML11JSONGeoLocation
{

WaterML11JSONGeogLocation geogLocation = new WaterML11JSONGeogLocation();
// TODO SAM 2015-09-09 Type is probably wrong but fix later
String [] localSiteXY = new String[0];

/**
Constructor.
*/
public WaterML11JSONGeoLocation ()
{
}

public WaterML11JSONGeogLocation getGeogLocation () {
	return this.geogLocation;
}

public String [] getLocalSiteXY () {
	return this.localSiteXY;
}

public void setGeogLocation ( WaterML11JSONGeogLocation geogLocation ) {
	this.geogLocation = geogLocation;
}

public void setLocalSiteXY ( String [] localSiteXY ) {
	this.localSiteXY = localSiteXY;
}

}
