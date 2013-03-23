package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database HDB_AGEN table, used mainly to present
agency choices when writing to the database.
*/
public class ReclamationHDB_Agency extends DMIDataObject
{
   
private int __agenID = DMIUtil.MISSING_INT;
private String __agenName = "";
private String __agenAbbrev = "";

/**
Constructor.
*/
public ReclamationHDB_Agency ()
{   super();
}

public String getAgenAbbrev ()
{
    return __agenAbbrev;
}

public int getAgenID ()
{
    return __agenID;
}

public String getAgenName ()
{
    return __agenName;
}

public void setAgenAbbrev ( String agenAbbrev )
{
    __agenAbbrev = agenAbbrev;
}

public void setAgenID ( int agenID )
{
    __agenID = agenID;
}

public void setAgenName ( String agenName )
{
    __agenName = agenName;
}

}