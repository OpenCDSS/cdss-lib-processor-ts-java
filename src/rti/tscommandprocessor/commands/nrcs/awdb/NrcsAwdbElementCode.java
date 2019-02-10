// NrcsAwdbElementCode - Element code information to allow lookup/translation of element code and description.

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

package rti.tscommandprocessor.commands.nrcs.awdb;

/**
Element code information to allow lookup/translation of element code and description.
This information is not currently available from a web service request but might be in the future.
*/
public class NrcsAwdbElementCode
{
    
/**
Element code.
*/
private String __code;

/**
Element units.
*/
private String __units = "";

/**
Element name.
*/
private String __name = "";
   
/**
Constructor.
@param code site type code
@param name site type description
@param units units for element
*/
public NrcsAwdbElementCode ( String code, String name, String units )
{
    __code = code;
    __name = name;
    __units = units;
}

/**
Return the element code.
*/
public String getCode()
{
    return __code;
}

/**
Return the element code name
*/
public String getName()
{
    return __name;
}

/**
Return the element units
*/
public String getUnits()
{
    return __units;
}

}
