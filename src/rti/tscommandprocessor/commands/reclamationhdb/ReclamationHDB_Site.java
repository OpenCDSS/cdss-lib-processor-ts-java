// ReclamationHDB_Site - Hold data from the Reclamation HDB database HDB_SITE table.

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

package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database HDB_SITE table.
*/
public class ReclamationHDB_Site extends DMIDataObject
{

// From HDB_SITE
private int __siteID = DMIUtil.MISSING_INT;
private String __siteName = "";
private String __siteCommonName = "";
private String __objectTypeName = "";
private String __stateCode = "";
private int __basinID = DMIUtil.MISSING_INT;
private double __latitude = DMIUtil.MISSING_DOUBLE;
private double __longitude = DMIUtil.MISSING_DOUBLE;
private String __huc = ""; // String in DB
private int __segmentNo = DMIUtil.MISSING_INT;
private float __riverMile = DMIUtil.MISSING_FLOAT;
private float __elevation = DMIUtil.MISSING_FLOAT;
private String __description = "";
private String __nwsCode = "";
private String __scsID = "";
private String __shefCode = "";
private String __usgsID = "";
private String __dbSiteCode = "";

/**
Constructor.
*/
public ReclamationHDB_Site ()
{   super();
}

public int getBasinID ()
{
    return __basinID;
}

public String getDbSiteCode ()
{
    return __dbSiteCode;
}

public String getDescription ()
{
    return __description;
}

public float getElevation ()
{
    return __elevation;
}

public String getHuc ()
{
    return __huc;
}

public double getLatitude ()
{
    return __latitude;
}

public double getLongitude ()
{
    return __longitude;
}

public String getNwsCode ()
{
    return __nwsCode;
}

public String getObjectTypeName ()
{
    return __objectTypeName;
}

public float getRiverMile ()
{
    return __riverMile;
}

public String getScsID ()
{
    return __scsID;
}

public int getSegmentNo ()
{
    return __segmentNo;
}

public String getShefCode ()
{
    return __shefCode;
}

public String getSiteCommonName ()
{
    return __siteCommonName;
}

public int getSiteID ()
{
    return __siteID;
}

public String getSiteName ()
{
    return __siteName;
}

public String getStateCode ()
{
    return __stateCode;
}

public String getUsgsID ()
{
    return __usgsID;
}

public void setBasinID ( int basinID )
{
    __basinID = basinID;
}

public void setDbSiteCode ( String dbSiteCode )
{
    __dbSiteCode = dbSiteCode;
}

public void setDescription ( String description )
{
    __description = description;
}

public void setElevation ( float elevation )
{
    __elevation = elevation;
}

public void setHuc ( String huc )
{
    __huc = huc;
}

public void setLatitude ( double latitude )
{
    __latitude = latitude;
}

public void setLongitude ( double longitude )
{
    __longitude = longitude;
}

public void setNwsCode ( String nwsCode )
{
    __nwsCode = nwsCode;
}

public void setObjectTypeName ( String objectTypeName )
{
    __objectTypeName = objectTypeName;
}

public void setRiverMile ( float riverMile )
{
    __riverMile = riverMile;
}

public void setScsID ( String scsID )
{
    __scsID = scsID;
}

public void setSegmentNo ( int segmentNo )
{
    __segmentNo = segmentNo;
}

public void setShefCode ( String shefCode )
{
    __shefCode = shefCode;
}

public void setSiteCommonName ( String siteCommonName )
{
    __siteCommonName = siteCommonName;
}

public void setSiteID ( int siteID )
{
    __siteID = siteID;
}

public void setSiteName ( String siteName )
{
    __siteName = siteName;
}

public void setStateCode ( String stateCode )
{
    __stateCode = stateCode;
}

public void setUsgsID ( String usgsID )
{
    __usgsID = usgsID;
}

}
