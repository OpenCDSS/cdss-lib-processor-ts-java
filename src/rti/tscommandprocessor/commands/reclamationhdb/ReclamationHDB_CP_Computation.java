package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database HDB_COLLECTION_SYSTEM table, used mainly to present
computation choices when writing to the database.
*/
public class ReclamationHDB_CP_Computation extends DMIDataObject
{
   
private int computationID = DMIUtil.MISSING_INT;
private String computationName = "";
private String cmmnt = "";
// TODO sam 2017-04-10 add other columns if needed - only name is needed by TSTool

/**
Constructor.
*/
public ReclamationHDB_CP_Computation ()
{   super();
}

public String getCmmnt ()
{
    return this.cmmnt;
}

public String getComputationName ()
{
    return this.computationName;
}

public int getComputationID ()
{
    return this.computationID;
}

public void setCmmnt ( String cmmnt )
{
    this.cmmnt = cmmnt;
}

public void setComputationID ( int computationID )
{
	this.computationID = computationID;
}

public void setComputationName ( String computationName )
{
	this.computationName = computationName;
}

}