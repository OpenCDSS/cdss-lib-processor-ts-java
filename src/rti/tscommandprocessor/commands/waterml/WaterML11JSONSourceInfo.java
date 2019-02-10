// WaterML11JSONSourceInfo - Corresponds to "{ value: { timeseries: sourceInfo" WaterML 1.1 JSON

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

import java.util.ArrayList;
import java.util.List;

/**
Corresponds to "{ value: { timeseries: sourceInfo" WaterML 1.1 JSON
*/
public class WaterML11JSONSourceInfo
{

private String siteName = "";
private List<WaterML11JSONSiteCode> siteCode = new ArrayList<WaterML11JSONSiteCode>();
private WaterML11JSONTimeZoneInfo timeZoneInfo = new WaterML11JSONTimeZoneInfo();
private WaterML11JSONGeoLocation geoLocation = new WaterML11JSONGeoLocation();
private Double elevationM = null;
private String verticalDatum = "";
private String [] note = new String[0];
private String extension = "";
private String altname = "";
private String [] siteType = new String[0];
private List<WaterML11JSONSiteProperty> siteProperty = new ArrayList<WaterML11JSONSiteProperty>();
private String oid = null;
private String metadataTime = null;

/**
Constructor.
*/
public WaterML11JSONSourceInfo ()
{
}

public WaterML11JSONGeoLocation getGeoLocation () {
	return this.geoLocation;
}

public List<WaterML11JSONSiteCode> getSiteCode () {
	return this.siteCode;
}

public WaterML11JSONTimeZoneInfo getTimeZoneInfo () {
	return this.timeZoneInfo;
}

public void setGeoLocation ( WaterML11JSONGeoLocation geoLocation ) {
	this.geoLocation = geoLocation;
}

public void setSiteCode ( List<WaterML11JSONSiteCode> siteCode ) {
	this.siteCode = siteCode;
}

public void setTimeZoneInfo ( WaterML11JSONTimeZoneInfo timeZoneInfo ) {
	this.timeZoneInfo = timeZoneInfo;
}

}
