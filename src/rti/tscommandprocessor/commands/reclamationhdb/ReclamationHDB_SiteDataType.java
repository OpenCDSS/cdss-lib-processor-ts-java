package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database HDB_SITE_DATATYPE table, and also join to
HDB_SITE and HDB_DATATYPE to get the common names.  These data are useful for lists and lookups.
*/
public class ReclamationHDB_SiteDataType extends DMIDataObject
{

private int __siteID = DMIUtil.MISSING_INT;
private int __siteDataTypeID = DMIUtil.MISSING_INT;
private int __dataTypeID = DMIUtil.MISSING_INT;
private String __siteCommonName = "";
private String __dataTypeCommonName = "";

/**
Constructor.
*/
public ReclamationHDB_SiteDataType ()
{   super();
}

public String getDataTypeCommonName ()
{
    return __dataTypeCommonName;
}

public int getDataTypeID ()
{
    return __dataTypeID;
}

public String getSiteCommonName ()
{
    return __siteCommonName;
}

public int getSiteDataTypeID ()
{
    return __siteDataTypeID;
}

public int getSiteID ()
{
    return __siteID;
}

public void setDataTypeCommonName ( String dataTypeCommonName )
{
    __dataTypeCommonName = dataTypeCommonName;
}

public void setDataTypeID ( int dataTypeID )
{
    __dataTypeID = dataTypeID;
}

public void setSiteCommonName ( String siteCommonName )
{
    __siteCommonName = siteCommonName;
}

public void setSiteDataTypeID ( int siteDataTypeID )
{
    __siteDataTypeID = siteDataTypeID;
}

public void setSiteID ( int siteID )
{
    __siteID = siteID;
}

}