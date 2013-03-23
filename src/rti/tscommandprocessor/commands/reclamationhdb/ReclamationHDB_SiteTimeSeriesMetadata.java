package rti.tscommandprocessor.commands.reclamationhdb;

import java.util.Date;

import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database that is a join of site, object, data type, and time series metadata.
*/
public class ReclamationHDB_SiteTimeSeriesMetadata extends ReclamationHDB_Site
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

// HDB_SITE data are in base class

// From HDB_DATATYPE
private String __dataTypeName= "";
private String __dataTypeCommonName= "";
private String __physicalQuantityName= "";
private String __unitCommonName= "";
private int __agenID = -1; // Reference to HDB_AGEN - carry around because agency abbreviation might be null
private String __agenAbbrev = ""; // Ideally use this but might be null in database

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

public int getAgenID ()
{
    return __agenID;
}

public String getAgenAbbrev ()
{
    return __agenAbbrev;
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

public String getHydrologicIndicator ()
{
    return __hydrologicIndicator;
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

public int getSiteDataTypeID ()
{
    return __siteDataTypeID;
}

public Date getStartDateTimeMax ()
{
    return __startDateTimeMax;
}

public Date getStartDateTimeMin ()
{
    return __startDateTimeMin;
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

public void setAgenAbbrev ( String agenAbbrev )
{
    __agenAbbrev = agenAbbrev;
}

public void setAgenID ( int agenID )
{
    __agenID = agenID;
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

public void setHydrologicIndicator ( String hydrologicIndicator )
{
    __hydrologicIndicator = hydrologicIndicator;
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

public void setSiteDataTypeID ( int siteDataTypeID )
{
    __siteDataTypeID = siteDataTypeID;
}

public void setStartDateTimeMax ( Date startDateTimeMax )
{
    __startDateTimeMax = startDateTimeMax;
}

public void setStartDateTimeMin ( Date startDateTimeMin )
{
    __startDateTimeMin = startDateTimeMin;
}

public void setUnitCommonName ( String unitCommonName )
{
    __unitCommonName = unitCommonName;
}

}