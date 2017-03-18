package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;

/**
Hold data from the Reclamation HDB database HDB_VALIDATION table, used to provide a list of flags
when writing to the database.
*/
public class ReclamationHDB_Validation extends DMIDataObject
{
   
private String __validation = "";
private String __cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_Validation ()
{   super();
}

public String getCmmnt ()
{
    return __cmmnt;
}

public String getValidation ()
{
    return __validation;
}

public void setCmmnt ( String cmmnt )
{
    __cmmnt = cmmnt;
}

public void setValidation ( String validation )
{
    __validation = validation;
}

}