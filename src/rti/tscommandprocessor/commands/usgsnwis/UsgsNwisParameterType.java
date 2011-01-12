package rti.tscommandprocessor.commands.usgsnwis;

/**
Parameter information to allow lookup/translation of numeric statistic code and string type.
This information is not currently available from a web service request but might be - hence why this is not
in an enumeration.  See:
http://nwis.waterdata.usgs.gov/usa/nwis/pmcodes?radio_pm_search=param_group&pm_group=All+--+include+all+parameter+groups&pm_search=&casrn_search=&srsname_search=&format=html_table&show=parameter_group_nm&show=parameter_nm&show=casrn&show=srsname&show=parameter_units
*/
public class UsgsNwisParameterType
{

/**
American Chemical Society CAS registry number (for water quality parameters).
*/
private String __casrn;

/**
Code for parameter.  Treat as a string even though most are zero-padded numbers, just to make sure
that formatting is retained.
*/
private String __code;

/**
Parameter group name.
*/
private String __groupName = "";

/**
Parameter name/description.
*/
private String __name = "";

/**
SRS name (?).
*/
private String __srsName = "";

/**
AData units.
*/
private String __units;
   
/**
Constructor.
@param code parameter code
@param name parameter name
@param descirption parameter description
*/
public UsgsNwisParameterType ( String code, String groupName, String name, String casrn, String srsName, String units )
{
    __code = code;
    __groupName = groupName;
    __name = name;
    __casrn = casrn;
    __srsName = srsName;
    __units = units;
}

/**
Return the parameter code.
*/
public String getCode()
{
    return __code;
}

/**
Return the parameter description.
*/
public String getDescription()
{
    return __srsName;
}

/**
Return the parameter name.
*/
public String getName()
{
    return __name;
}

}