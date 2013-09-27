package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database REF_ENSEMBLE_KEYVAL table, which is a free-form list of ensemble properties.
*/
public class ReclamationHDB_EnsembleKeyVal extends DMIDataObject
{
   
private int __ensembleID = DMIUtil.MISSING_INT;
private String __keyName = "";
private String __keyValue = "";

/**
Constructor.
*/
public ReclamationHDB_EnsembleKeyVal ()
{   super();
}

public int getEnsembleID ()
{
    return __ensembleID;
}

public String getKeyName ()
{
    return __keyName;
}

public String getKeyValue ()
{
    return __keyValue;
}

public void setEnsembleID ( int ensembleID )
{
    __ensembleID = ensembleID;
}

public void setKeyName ( String keyName )
{
    __keyName = keyName;
}

public void setKeyValue ( String keyValue )
{
    __keyValue = keyValue;
}

}