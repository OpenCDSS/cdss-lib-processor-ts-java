// RccAcisStationType - Station type information to allow lookup/translation of numeric station code and string type.

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

package rti.tscommandprocessor.commands.rccacis;

/**
Station type information to allow lookup/translation of numeric station code and string type.
This information is not currently available from a web service request but might be - hence why this is not
in an enumeration.  Currently instances are initialized in the RccAcisDataStore based on the "Station Id Type"
table here:  http://data.rcc-acis.org/doc/index.html
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
