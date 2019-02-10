// UsgsNwisSiteType - Site type information to allow lookup/translation of site type code and string type.

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
