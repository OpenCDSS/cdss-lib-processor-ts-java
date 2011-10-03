package rti.tscommandprocessor.commands.reclamationhdb;

import java.util.Date;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database REF_MODEL_RUN table, used mainly to look up the
MODEL_RUN_ID from the MODEL_RUN_NAME, MODEL_ID, HYDROLOGIC_INDICATOR, and RUN_DATE and to give
users choices of model runs.
*/
public class ReclamationHDB_ModelRun extends DMIDataObject
{
   
private int __modelRunID = DMIUtil.MISSING_INT;
private String __modelRunName = "";
private int __modelID = DMIUtil.MISSING_INT;
private Date __dateTimeLoaded = DMIUtil.MISSING_DATE;
private String __userName = "";
private String __extraKeysYN = "";
private Date __runDate = DMIUtil.MISSING_DATE;
private Date __startDate = DMIUtil.MISSING_DATE;
private Date __endDate = DMIUtil.MISSING_DATE;
private String __hydrologicIndicator = "";
private String __modelType = "";
private String __timeStepDescriptor = "";
private String __cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_ModelRun ()
{   super();
}

public String getCmmnt ()
{
    return __cmmnt;
}

public Date getDateTimeLoaded ()
{
    return __dateTimeLoaded;
}

public Date getEndDate ()
{
    return __endDate;
}

public String getExtraKeysYN ()
{
    return __extraKeysYN;
}

public String getHydrologicIndicator ()
{
    return __hydrologicIndicator;
}

public int getModelID ()
{
    return __modelID;
}

public int getModelRunID ()
{
    return __modelRunID;
}

public String getModelRunName ()
{
    return __modelRunName;
}

public String getModelType ()
{
    return __modelType;
}

public Date getRunDate ()
{
    return __runDate;
}

public Date getStartDate ()
{
    return __startDate;
}

public String getTimeStepDescriptor ()
{
    return __timeStepDescriptor;
}

public String getUserName ()
{
    return __userName;
}

public void setCmmnt ( String cmmnt )
{
    __cmmnt = cmmnt;
}

public void setDateTimeLoaded ( Date dateTimeLoaded )
{
    __dateTimeLoaded = dateTimeLoaded;
}

public void setEndDate ( Date endDate )
{
    __endDate = endDate;
}

public void setExtraKeysYN ( String extraKeysYN )
{
    __extraKeysYN = extraKeysYN;
}

public void setHydrologicIndicator ( String hydrologicIndicator )
{
    __hydrologicIndicator = hydrologicIndicator;
}

public void setModelID ( int modelID )
{
    __modelID = modelID;
}

public void setModelRunID ( int modelRunID )
{
    __modelRunID = modelRunID;
}

public void setModelRunName ( String modelRunName )
{
    __modelRunName = modelRunName;
}

public void setModelType ( String modelType )
{
    __modelType = modelType;
}

public void setRunDate ( Date runDate )
{
    __runDate = runDate;
}

public void setStartDate ( Date startDate )
{
    __startDate = startDate;
}

public void setTimeStepDescriptor ( String timeStepDescriptor )
{
    __timeStepDescriptor = timeStepDescriptor;
}

public void setUserName ( String userName )
{
    __userName = userName;
}

}