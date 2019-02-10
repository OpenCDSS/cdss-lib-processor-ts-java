// UsgsNwisStatisticType - Statistic information to allow lookup/translation of numeric statistic code and string type.

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
