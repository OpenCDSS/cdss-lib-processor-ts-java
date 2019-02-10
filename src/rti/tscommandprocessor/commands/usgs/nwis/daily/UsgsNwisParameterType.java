// UsgsNwisParameterType - Parameter information to allow lookup/translation of numeric statistic code and string type.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package rti.tscommandprocessor.commands.usgs.nwis.daily;

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
