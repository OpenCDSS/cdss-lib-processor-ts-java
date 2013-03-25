package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database REF_ENSEMBLE table, used mainly to look up the
MODEL_ID from the ENSEMBLE_NAME and MODEL_NAME and to give users choices of ensembles.
*/
public class ReclamationHDB_Ensemble extends DMIDataObject
{
   
private int __ensembleID = DMIUtil.MISSING_INT;
private String __ensembleName = "";
private int __agenID = DMIUtil.MISSING_INT;
private String __traceDomain = "";
private String __cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_Ensemble ()
{   super();
}

public int getAgenID ()
{
    return __agenID;
}

public String getCmmnt ()
{
    return __cmmnt;
}

public int getEnsembleID ()
{
    return __ensembleID;
}

public String getEnsembleName ()
{
    return __ensembleName;
}

public String getTraceDomain ()
{
    return __traceDomain;
}

public void setAgenID ( int agenID )
{
    __agenID = agenID;
}

public void setCmmnt ( String cmmnt )
{
    __cmmnt = cmmnt;
}

public void setEnsembleID ( int ensembleID )
{
    __ensembleID = ensembleID;
}

public void setEnsembleName ( String ensembleName )
{
    __ensembleName = ensembleName;
}

public void setTraceDomain ( String traceDomain )
{
    __traceDomain = traceDomain;
}

}