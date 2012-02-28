package rti.tscommandprocessor.commands.usgs.nwis.daily;

/**
Statistic information to allow lookup/translation of numeric statistic code and string type.
This information is not currently available from a web service request but might be - hence why this is not
in an enumeration.
*/
public class UsgsNwisStatisticType
{
    
/**
Code for statistic.  Treat as a string even though most are zero-padded numbers, just to make sure
that formatting is retained.
*/
private String __code;

/**
Statistic name code.
*/
private String __name = "";

/**
Description.
*/
private String __description = "";
   
/**
Constructor.
@param code statistic code
@param name statistic name
@param descirption statistic description
*/
public UsgsNwisStatisticType ( String code, String name, String description )
{
    __code = code;
    __name = name;
    __description = description;
}

/**
Return the statistic code.
*/
public String getCode()
{
    return __code;
}

/**
Return the statistic description.
*/
public String getDescription()
{
    return __description;
}

/**
Return the statistic name.
*/
public String getName()
{
    return __name;
}

}