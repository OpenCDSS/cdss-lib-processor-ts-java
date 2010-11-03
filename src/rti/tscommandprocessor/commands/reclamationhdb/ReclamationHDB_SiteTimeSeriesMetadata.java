package rti.tscommandprocessor.commands.reclamationhdb;

import java.util.Date;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database that is a join of site, object, data type, and time series metadata.
*/
public class ReclamationHDB_SiteTimeSeriesMetadata extends DMIDataObject
{
// Data fields that are not in the database but are important metadata
// Whether Real or Model
private String __realModelType = "Real";
// Data interval
private String __dataInterval = "";

// From HDB_OBJECTTYPE
    
private int __objectTypeID = DMIUtil.MISSING_INT;
private String __objectTypeName = "";
private String __objectTypeTag = "";

// From HDB_SITE
private int __siteID = DMIUtil.MISSING_INT;
private String __siteName = "";
private String __siteCommonName = "";
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

// From HDB_DATATYPE
private String __dataTypeName= "";
private String __dataTypeCommonName= "";
private String __physicalQuantityName= "";
private String __unitCommonName= "";

// From HDB_SITE_DATATYPE
private int __siteDataTypeID = DMIUtil.MISSING_INT;

// From HDB_MODEL
private String __modelName = "";

// From REF_MODEL_RUN
private int __modelRunID = DMIUtil.MISSING_INT;
private String __modelRunName = "";
private String __hydrologicIndicator = "";
private Date __modelRunDate = DMIUtil.MISSING_DATE;

// From data tables (dates give user an indication of the period and are used to help allocate time series memory)
private Date __startDateTimeMin = DMIUtil.MISSING_DATE;
private Date __startDateTimeMax = DMIUtil.MISSING_DATE;

/**
Constructor.
*/
public ReclamationHDB_SiteTimeSeriesMetadata ()
{   super();
}

public int getBasinID ()
{
    return __basinID;
}

public String getDataInterval ()
{
    return __dataInterval;
}

public String getDataTypeCommonName ()
{
    return __dataTypeCommonName;
}

public String getDataTypeName ()
{
    return __dataTypeName;
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

public String getHydrologicIndicator ()
{
    return __hydrologicIndicator;
}

public double getLatitude ()
{
    return __latitude;
}

public double getLongitude ()
{
    return __longitude;
}

public String getModelName ()
{
    return __modelName;
}

public Date getModelRunDate ()
{
    return __modelRunDate;
}

public int getModelRunID ()
{
    return __modelRunID;
}

public String getModelRunName ()
{
    return __modelRunName;
}

public String getNwsCode ()
{
    return __nwsCode;
}

public int getObjectTypeID ()
{
    return __objectTypeID;
}

public String getObjectTypeName ()
{
    return __objectTypeName;
}

public String getObjectTypeTag ()
{
    return __objectTypeTag;
}

public String getPhysicalQuantityName ()
{
    return __physicalQuantityName;
}

public String getRealModelType ()
{
    return __realModelType;
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

public int getSiteDataTypeID ()
{
    return __siteDataTypeID;
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

public Date getStartDateTimeMax ()
{
    return __startDateTimeMax;
}

public Date getStartDateTimeMin ()
{
    return __startDateTimeMin;
}

public String getStateCode ()
{
    return __stateCode;
}

/**
Get the time series identifier string corresponding to this instance, without the data store name.
*/
public String getTSID ()
{
    String tsType = getRealModelType();
    String scenario = "";
    if ( tsType.equalsIgnoreCase("Model") ) {
        String modelRunDate = "" + getModelRunDate();
        // Trim off the hundredths of a second since that interferes with the TSID conventions and is not
        // likely needed to uniquely identify the model time series
        int pos = modelRunDate.indexOf(".");
        if ( pos > 0 ) {
            modelRunDate = modelRunDate.substring(0,pos);
        }
        // Replace "." with "?" in the model information so as to not conflict with TSID conventions - will
        // switch again later.
        String modelName = getModelName();
        modelName = modelName.replace('.', '?');
        String modelRunName = getModelRunName();
        modelRunName = modelRunName.replace('.', '?');
        String hydrologicIndicator = getHydrologicIndicator();
        hydrologicIndicator = hydrologicIndicator.replace('.', '?');
        // The following should uniquely identify a model time series (in addition to other TSID parts)
        scenario = "." + modelName + "-" + modelRunName + "-" + hydrologicIndicator + "-" + modelRunDate;
    }
    return tsType + ":" + getSiteCommonName().replace('.','?') + ".HDB." +
        getDataTypeCommonName().replace('.', '?') + "." + getDataInterval() +
        scenario;
}

public String getUnitCommonName ()
{
    return __unitCommonName;
}

public String getUsgsID ()
{
    return __usgsID;
}

public void setBasinID ( int basinID )
{
    __basinID = basinID;
}

public void setDataInterval ( String dataInterval )
{
    __dataInterval = dataInterval;
}

public void setDataTypeCommonName ( String dataTypeCommonName )
{
    __dataTypeCommonName = dataTypeCommonName;
}

public void setDataTypeName ( String dataTypeName )
{
    __dataTypeName = dataTypeName;
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

public void setHydrologicIndicator ( String hydrologicIndicator )
{
    __hydrologicIndicator = hydrologicIndicator;
}

public void setLatitude ( double latitude )
{
    __latitude = latitude;
}

public void setLongitude ( double longitude )
{
    __longitude = longitude;
}

public void setModelName ( String modelName )
{
    __modelName = modelName;
}

public void setModelRunDate ( Date modelRunDate )
{
    __modelRunDate = modelRunDate;
}

public void setModelRunID ( int modelRunID )
{
    __modelRunID = modelRunID;
}

public void setModelRunName ( String modelRunName )
{
    __modelRunName = modelRunName;
}

public void setNwsCode ( String nwsCode )
{
    __nwsCode = nwsCode;
}

public void setObjectTypeID ( int objectTypeID )
{
    __objectTypeID = objectTypeID;
}

public void setObjectTypeName ( String objectTypeName )
{
    __objectTypeName = objectTypeName;
}

public void setObjectTypeTag ( String objectTypeTag )
{
    __objectTypeTag = objectTypeTag;
}

public void setPhysicalQuantityName ( String physicalQuantityName )
{
    __physicalQuantityName = physicalQuantityName;
}

public void setRealModelType ( String realModelType )
{
    __realModelType = realModelType;
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

public void setSiteDataTypeID ( int siteDataTypeID )
{
    __siteDataTypeID = siteDataTypeID;
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

public void setStartDateTimeMax ( Date startDateTimeMax )
{
    __startDateTimeMax = startDateTimeMax;
}

public void setStartDateTimeMin ( Date startDateTimeMin )
{
    __startDateTimeMin = startDateTimeMin;
}

public void setStateCode ( String stateCode )
{
    __stateCode = stateCode;
}

public void setUnitCommonName ( String unitCommonName )
{
    __unitCommonName = unitCommonName;
}

public void setUsgsID ( String usgsID )
{
    __usgsID = usgsID;
}

}