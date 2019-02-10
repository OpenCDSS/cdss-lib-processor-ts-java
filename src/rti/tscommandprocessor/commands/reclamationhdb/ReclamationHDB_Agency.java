// ReclamationHDB_Agency - Hold data from the Reclamation HDB database HDB_AGEN table

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

package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.DMIDataObject;
import RTi.DMI.DMIUtil;

/**
Hold data from the Reclamation HDB database HDB_AGEN table, used mainly to present
agency choices when writing to the database.
*/
public class ReclamationHDB_Agency extends DMIDataObject
{
   
private int __agenID = DMIUtil.MISSING_INT;
private String __agenName = "";
private String __agenAbbrev = "";

/**
Constructor.
*/
public ReclamationHDB_Agency ()
{   super();
}

public String getAgenAbbrev ()
{
    return __agenAbbrev;
}

public int getAgenID ()
{
    return __agenID;
}

public String getAgenName ()
{
    return __agenName;
}

public void setAgenAbbrev ( String agenAbbrev )
{
    __agenAbbrev = agenAbbrev;
}

public void setAgenID ( int agenID )
{
    __agenID = agenID;
}

public void setAgenName ( String agenName )
{
    __agenName = agenName;
}

}
