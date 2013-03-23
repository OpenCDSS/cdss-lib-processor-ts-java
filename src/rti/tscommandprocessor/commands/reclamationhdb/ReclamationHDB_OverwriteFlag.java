package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;

/**
Hold data from the Reclamation HDB database HDB_OVERWRITE_FLAG table, used to provide a list of flags
when writing to the database.
*/
public class ReclamationHDB_OverwriteFlag extends DMIDataObject
{
   
private String __overwriteFlag = "";
private String __overwriteFlagName = "";
private String __cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_OverwriteFlag ()
{   super();
}

public String getCmmnt ()
{
    return __cmmnt;
}

public String getOverwriteFlag ()
{
    return __overwriteFlag;
}

public String getOverwriteFlagName ()
{
    return __overwriteFlagName;
}

public void setCmmnt ( String cmmnt )
{
    __cmmnt = cmmnt;
}

public void setOverwriteFlag ( String overwriteFlag )
{
    __overwriteFlag = overwriteFlag;
}

public void setOverwriteFlagName ( String overwriteFlagName )
{
    __overwriteFlagName = overwriteFlagName;
}

}