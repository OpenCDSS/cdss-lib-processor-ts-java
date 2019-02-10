// ReclamationHDB_OverwriteFlag - Hold data from the Reclamation HDB database HDB_OVERWRITE_FLAG table

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

/**
Hold data from the Reclamation HDB database HDB_OVERWRITE_FLAG table, used to provide a list of flags
when writing to the database.
*/
public class ReclamationHDB_OverwriteFlag extends DMIDataObject
{
   
private String __overwriteFlag = "";
private String __overwriteFlagName = "";
private String __cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_OverwriteFlag ()
{   super();
}

public String getCmmnt ()
{
    return __cmmnt;
}

public String getOverwriteFlag ()
{
    return __overwriteFlag;
}

public String getOverwriteFlagName ()
{
    return __overwriteFlagName;
}

public void setCmmnt ( String cmmnt )
{
    __cmmnt = cmmnt;
}

public void setOverwriteFlag ( String overwriteFlag )
{
    __overwriteFlag = overwriteFlag;
}

public void setOverwriteFlagName ( String overwriteFlagName )
{
    __overwriteFlagName = overwriteFlagName;
}

}
