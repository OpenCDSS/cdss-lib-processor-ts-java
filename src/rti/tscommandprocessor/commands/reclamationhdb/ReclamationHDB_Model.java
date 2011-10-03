package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database HDB_MODEL table, used mainly to look up the
MODEL_ID from the MODEL_NAME and to give users choices of models.
*/
public class ReclamationHDB_Model extends DMIDataObject
{
   
private int __modelID = DMIUtil.MISSING_INT;
private String __modelName = "";
private String __coordinated = "";
private String __cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_Model ()
{   super();
}

public String getCmmnt ()
{
    return __cmmnt;
}

public String getCoordinated ()
{
    return __coordinated;
}

public int getModelID ()
{
    return __modelID;
}

public String getModelName ()
{
    return __modelName;
}

public void setCmmnt ( String cmmnt )
{
    __cmmnt = cmmnt;
}


public void setCoordinated ( String coordinated )
{
    __coordinated = coordinated;
}

public void setModelID ( int modelID )
{
    __modelID = modelID;
}

public void setModelName ( String modelName )
{
    __modelName = modelName;
}

}