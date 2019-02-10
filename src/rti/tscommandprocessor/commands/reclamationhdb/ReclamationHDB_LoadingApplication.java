// ReclamationHDB_LoadingApplication - Hold data from the Reclamation HDB database HDB_LOADING_APPLICATION table

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
Hold data from the Reclamation HDB database HDB_LOADING_APPLICATION table, used mainly to look up the
LOADING_APPLICATION_ID from the LOADING_APPLICATION_NAME when writing to the database.
*/
public class ReclamationHDB_LoadingApplication extends DMIDataObject
{
   
private int __loadingApplicationID = DMIUtil.MISSING_INT;
private String __loadingApplicationName = "";
private String __manualEditApp = "";
private String __cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_LoadingApplication ()
{   super();
}

public String getCmmnt ()
{
    return __cmmnt;
}

public int getLoadingApplicationID ()
{
    return __loadingApplicationID;
}

public String getLoadingApplicationName ()
{
    return __loadingApplicationName;
}

public String getManualEditApp ()
{
    return __manualEditApp;
}

public void setCmmnt ( String cmmnt )
{
    __cmmnt = cmmnt;
}

public void setLoadingApplicationID ( int loadingApplicationID )
{
    __loadingApplicationID = loadingApplicationID;
}

public void setLoadingApplicationName ( String loadingApplicationName )
{
    __loadingApplicationName = loadingApplicationName;
}

public void setManualEditApp ( String manualEditApp )
{
    __manualEditApp = manualEditApp;
}

}
