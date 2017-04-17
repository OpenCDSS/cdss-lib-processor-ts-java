package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database HDB_METHOD table, used mainly to present
method choices when writing to the database.
*/
public class ReclamationHDB_Method extends DMIDataObject
{
   
private int methodID = DMIUtil.MISSING_INT;
private String methodName = "";
private String cmmnt = "";
// TODO sam 2017-04-10 add other columns if needed - only name is needed by TSTool

/**
Constructor.
*/
public ReclamationHDB_Method ()
{   super();
}

public String getCmmnt ()
{
    return this.cmmnt;
}

public String getMethodName ()
{
    return this.methodName;
}

public int getMethodID ()
{
    return this.methodID;
}

public void setCmmnt ( String cmmnt )
{
    this.cmmnt = cmmnt;
}

public void setMethodID ( int methodID )
{
	this.methodID = methodID;
}

public void setMethodName ( String methodName )
{
	this.methodName = methodName;
}

}