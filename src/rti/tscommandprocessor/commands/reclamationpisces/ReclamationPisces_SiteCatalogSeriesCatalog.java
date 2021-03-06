// ReclamationPisces_SiteCatalogSeriesCatalog - Hold data from the Reclamation Pisces database that is a join of sitecatalog and seriescatalog tables.

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

package rti.tscommandprocessor.commands.reclamationpisces;

import RTi.DMI.DMIUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;

// In the future it may or may not make sense to add classes for one or both of the
// individual tables and then extend this class from one of those classes.
/**
Hold data from the Reclamation Pisces database that is a join of sitecatalog and seriescatalog tables.
*/
public class ReclamationPisces_SiteCatalogSeriesCatalog
{

// From "sitecatalog"
    
private String siteid = "";
private String description = "";
private String state = "";
private double latitude = DMIUtil.MISSING_DOUBLE;
private double longitude = DMIUtil.MISSING_DOUBLE;
private double elevation = DMIUtil.MISSING_DOUBLE;
private String timezone = "";
private String install = "";
private String horizontal_datum = "";
private String vertical_datum = "";
private double vertical_accuracy = DMIUtil.MISSING_DOUBLE;
private String elevation_method = "";
private String tz_offset = "";
private String active_flag = "";
private String type = "";
private String responsibility = "";
private String agency_region = "";

// From "seriescatalog" table

private int id = DMIUtil.MISSING_INT;
private int parentid = DMIUtil.MISSING_INT;
private int isfolder = DMIUtil.MISSING_INT;
private int sortorder = DMIUtil.MISSING_INT;
private String iconname = "";
private String name = "";
// siteid is key to sitecatalog table and is included above
private String units = "";
private String timeinterval = "";
private String parameter = "";
private String tablename = "";
private String provider = "";
private String connectionstring = "";
private String expression = "";
private String notes = "";
private int enabled = DMIUtil.MISSING_INT;
private String server = "";
private DateTime t1 = null;
private DateTime t2 = null;
private int count = DMIUtil.MISSING_INT;

/**
Constructor.
*/
public ReclamationPisces_SiteCatalogSeriesCatalog ()
{   super();
}

public String getActiveFlag ()
{
	return this.active_flag;
}

public String getAgencyRegion ()
{
	return this.agency_region;
}

public String getConnectionString ()
{
	return this.connectionstring;
}

public int getCount ()
{
	return this.count;
}

public String getDescription ()
{
	return this.description;
}

public double getElevation ()
{
	return this.elevation;
}

public String getElevationMethod ()
{
	return this.elevation_method;
}

public int getEnabled ()
{
	return this.enabled;
}

public String getExpression ()
{
	return this.expression;
}

public String getHorizontalDatum ()
{
	return this.horizontal_datum;
}

public String getIconName ()
{
	return this.iconname;
}

public int getID ()
{
	return this.id;
}

public String getInstall ()
{
	return this.install;
}

public int getIsFolder ()
{
	return this.isfolder;
}

public double getLatitude ()
{
	return this.latitude;
}

public double getLongitude ()
{
	return this.longitude;
}

public String getName ()
{
	return this.name;
}

public String getNotes ()
{
	return this.notes;
}

public String getParameter ()
{
	return this.parameter;
}

public int getParentID ()
{
	return this.parentid;
}

public String getProvider ()
{
	return this.provider;
}

public String getResponsibility ()
{
	return this.responsibility;
}

public String getServer ()
{
	return this.server;
}

public String getSiteID ()
{
	return this.siteid;
}

public int getSortOrder ()
{
	return this.sortorder;
}

public String getState ()
{
	return this.state;
}

public DateTime getT1 ()
{
	return this.t1;
}

public DateTime getT2 ()
{
	return this.t2;
}

public String getTableName ()
{
	return this.tablename;
}

public String getTimeInterval ()
{
	return this.timeinterval;
}

public String getTimeIntervalForTSID ()
{
	if ( timeinterval.equalsIgnoreCase("Hourly") ) {
		return TimeInterval.getName(TimeInterval.HOUR,0);
	}
	else if ( timeinterval.equalsIgnoreCase("Daily") ) {
		return TimeInterval.getName(TimeInterval.DAY,0);
	}
	//else if ( timeinterval.equalsIgnoreCase("Weekly") ) {
	//	return TimeInterval.getName(TimeInterval.WEEK,0);
	//}
	else if ( timeinterval.equalsIgnoreCase("Monthly") ) {
		return TimeInterval.getName(TimeInterval.MONTH,0);
	}
	else if ( timeinterval.equalsIgnoreCase("Yearly") ) {
		return TimeInterval.getName(TimeInterval.YEAR,0);
	}
	else if ( timeinterval.equalsIgnoreCase("Irregular") ) {
		return TimeInterval.getName(TimeInterval.IRREGULAR,0);
	}
	else {
		return null;
	}
}

public String getTimeZone ()
{
	return this.timezone;
}

public String getTimeZoneOffset ()
{
	return this.tz_offset;
}

/**
Get the time series identifier string corresponding to this instance, without the data store name.
*/
public String getTSID ()
{
    return getSiteID().replace('.','?') + "." + getServer().replace('.','?') + "." +
        getParameter().replace('.', '?') + "." + getTimeIntervalForTSID();
}

public String getType ()
{
	return this.type;
}

public String getUnits ()
{
	return this.units;
}

public double getVerticalAccuracy()
{
	return this.vertical_accuracy;
}

public String getVerticalDatum ()
{
	return this.vertical_datum;
}

public void setActiveFlag ( String active_flag )
{
    this.active_flag = active_flag;
}

public void setAgencyRegion ( String agency_region )
{
    this.agency_region = agency_region;
}

public void setConnectionString ( String connectionstring )
{
    this.connectionstring = connectionstring;
}

public void setCount ( int count )
{
    this.count = count;
}

public void setDescription ( String description )
{
    this.description = description;
}

public void setElevation ( double elevation )
{
    this.elevation = elevation;
}

public void setElevationMethod ( String elevation_method )
{
    this.elevation_method = elevation_method;
}

public void setEnabled ( int enabled )
{
    this.enabled = enabled;
}

public void setExpression ( String expression )
{
    this.expression = expression;
}

public void setHorizontalDatum ( String horizontal_datum )
{
    this.horizontal_datum = horizontal_datum;
}

public void setIconName ( String iconname )
{
    this.iconname = iconname;
}

public void setID ( int id )
{
    this.id = id;
}

public void setInstall ( String install )
{
    this.install = install;
}

public void setIsFolder ( int isfolder )
{
    this.isfolder = isfolder;
}

public void setLatitude ( double latitude )
{
    this.latitude = latitude;
}

public void setLongitude ( double longitude )
{
    this.longitude = longitude;
}

public void setName ( String name )
{
    this.name = name;
}

public void setNotes ( String notes )
{
    this.notes = notes;
}

public void setParameter ( String parameter )
{
    this.parameter = parameter;
}

public void setParentID ( int parentid )
{
    this.parentid = parentid;
}

public void setProvider ( String provider )
{
    this.provider = provider;
}

public void setResponsibility ( String responsibility )
{
    this.responsibility = responsibility;
}

public void setServer ( String server )
{
    this.server = server;
}

public void setSiteID ( String siteid )
{
    this.siteid = siteid;
}

public void setSortOrder ( int sortorder )
{
    this.sortorder = sortorder;
}

public void setState ( String state )
{
    this.state = state;
}

public void setT1 ( DateTime t1 )
{
    this.t1 = t1;
}

public void setT2 ( DateTime t2 )
{
    this.t2 = t2;
}

public void setTableName ( String tablename )
{
    this.tablename = tablename;
}

public void setTimeInterval ( String timeinterval )
{
    this.timeinterval = timeinterval;
}

public void setTimeZone ( String timezone )
{
    this.timezone = timezone;
}

public void setTimeZoneOffset ( String tz_offset )
{
    this.tz_offset = tz_offset;
}

public void setType ( String type )
{
    this.type = type;
}

public void setUnits ( String units )
{
    this.units = units;
}

public void setVerticalAccuracy ( double vertical_accuracy )
{
    this.vertical_accuracy = vertical_accuracy;
}

public void setVerticalDatum ( String vertical_datum )
{
    this.vertical_datum = vertical_datum;
}

}
