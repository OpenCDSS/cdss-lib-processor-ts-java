package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database HDB_LOADING_APPLICATION table, used mainly to look up the
LOADING_APPLICATION_ID from the LOADING_APPLICATION_NAME when writing to the database.
*/
public class ReclamationHDB_LoadingApplication extends DMIDataObject
{
   
private int __loadingApplicationID = DMIUtil.MISSING_INT;
private String __loadingApplicationName = "";
private String __manualEditApp = "";
private String __cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_LoadingApplication ()
{   super();
}

public String getCmmnt ()
{
    return __cmmnt;
}

public int getLoadingApplicationID ()
{
    return __loadingApplicationID;
}

public String getLoadingApplicationName ()
{
    return __loadingApplicationName;
}

public String getManualEditApp ()
{
    return __manualEditApp;
}

public void setCmmnt ( String cmmnt )
{
    __cmmnt = cmmnt;
}

public void setLoadingApplicationID ( int loadingApplicationID )
{
    __loadingApplicationID = loadingApplicationID;
}

public void setLoadingApplicationName ( String loadingApplicationName )
{
    __loadingApplicationName = loadingApplicationName;
}

public void setManualEditApp ( String manualEditApp )
{
    __manualEditApp = manualEditApp;
}

}