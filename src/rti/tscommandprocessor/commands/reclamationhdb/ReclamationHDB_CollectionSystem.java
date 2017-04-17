package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database HDB_COLLECTION_SYSTEM table, used mainly to present
collection system choices when writing to the database.
*/
public class ReclamationHDB_CollectionSystem extends DMIDataObject
{
   
private int collectionSystemID = DMIUtil.MISSING_INT;
private String collectionSystemName = "";
private String cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_CollectionSystem ()
{   super();
}

public String getCmmnt ()
{
    return this.cmmnt;
}

public String getCollectionSystemName ()
{
    return this.collectionSystemName;
}

public int getCollectionSystemID ()
{
    return this.collectionSystemID;
}

public void setCmmnt ( String cmmnt )
{
    this.cmmnt = cmmnt;
}

public void setCollectionSystemID ( int collectionSystemID )
{
	this.collectionSystemID = collectionSystemID;
}

public void setCollectionSystemName ( String collectionSystemName )
{
	this.collectionSystemName = collectionSystemName;
}

}