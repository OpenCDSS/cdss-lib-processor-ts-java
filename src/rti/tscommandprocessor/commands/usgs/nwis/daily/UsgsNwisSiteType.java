package rti.tscommandprocessor.commands.usgs.nwis.daily;

/**
Site type information to allow lookup/translation of site type code and string type.
This information is not currently available from a web service request but might be.
*/
public class UsgsNwisSiteType
{
    
/**
Code for site type.
*/
private String __code;

/**
Site type name.
*/
private String __name = "";

/**
Site type description.
*/
private String __description = "";
   
/**
Constructor.
@param code site type code
@param name site type name
@param description site type description
*/
public UsgsNwisSiteType ( String code, String name, String description )
{
    __code = code;
    __name = name;
    __description = description;
}

/**
Return the site type code.
*/
public String getCode()
{
    return __code;
}

/**
Return the site type description.
*/
public String getDescription()
{
    return __description;
}

/**
Return the site type name.
*/
public String getName()
{
    return __name;
}

}