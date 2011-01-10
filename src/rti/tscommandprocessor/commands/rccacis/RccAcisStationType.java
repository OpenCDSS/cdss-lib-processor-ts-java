package rti.tscommandprocessor.commands.rccacis;

/**
Station type information to allow lookup/translation of numeric station code and string type.
This information is not currently available from a web service request but might be - hence why this is not
in an enumeration.
*/
public class RccAcisStationType
{
    
/**
Numeric code for station type.
*/
private int __code;

/**
Station type code.
*/
private String __type = "";

/**
Description.
*/
private String __description = "";
   
/**
Constructor.
*/
public RccAcisStationType ( int code, String type, String description )
{
    __code = code;
    __type = type;
    __description = description;
}

/**
Return the code.
*/
public int getCode()
{
    return __code;
}

/**
Return the description.
*/
public String getDescription()
{
    return __description;
}

/**
Return the type.
*/
public String getType()
{
    return __type;
}

}