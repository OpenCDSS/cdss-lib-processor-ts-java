// NrcsAwdbNetworkCode - Network code information to allow lookup/translation of network code and description.

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
Network code information to allow lookup/translation of network code and description.
This information is not currently available from a web service request but might be in the future.
*/
public class NrcsAwdbNetworkCode
{
    
/**
Network code.
*/
private String __code;

/**
Network description.
*/
private String __description = "";
   
/**
Constructor.
@param code site type code
@param description site type description
*/
public NrcsAwdbNetworkCode ( String code, String description )
{
    __code = code;
    __description = description;
}

/**
Return the network code.
*/
public String getCode()
{
    return __code;
}

/**
Return the network description.
*/
public String getDescription()
{
    return __description;
}

}
