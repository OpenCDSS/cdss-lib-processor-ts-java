package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database HDB_OBJECTTYPE table, used mainly to look up the
object type from the HDB_SITE data.
*/
public class ReclamationHDB_ObjectType extends DMIDataObject
{
   
private int __objectTypeID = DMIUtil.MISSING_INT;
private String __objectTypeName = "";
private String __objectTypeTag = "";
private int __objectTypeParentOrder = DMIUtil.MISSING_INT;

/**
Constructor.
*/
public ReclamationHDB_ObjectType ()
{   super();
}

public int getObjectTypeID ()
{
    return __objectTypeID;
}

public String getObjectTypeName ()
{
    return __objectTypeName;
}

public int getObjectTypeParentOrder ()
{
    return __objectTypeParentOrder;
}

public String getObjectTypeTag ()
{
    return __objectTypeTag;
}

public void setObjectTypeID ( int objectTypeID )
{
    __objectTypeID = objectTypeID;
}

public void setObjectTypeName ( String objectTypeName )
{
    __objectTypeName = objectTypeName;
}

public void setObjectTypeParentOrder ( int objectTypeParentOrder )
{
    __objectTypeParentOrder = objectTypeParentOrder;
}

public void setObjectTypeTag ( String objectTypeTag )
{
    __objectTypeTag = objectTypeTag;
}

}