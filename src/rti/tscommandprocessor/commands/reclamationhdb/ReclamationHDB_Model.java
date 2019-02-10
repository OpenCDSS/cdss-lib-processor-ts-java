// ReclamationHDB_Model - Hold data from the Reclamation HDB database HDB_MODEL table

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
Hold data from the Reclamation HDB database HDB_MODEL table, used mainly to look up the
MODEL_ID from the MODEL_NAME and to give users choices of models.
*/
public class ReclamationHDB_Model extends DMIDataObject
{
   
private int __modelID = DMIUtil.MISSING_INT;
private String __modelName = "";
private String __coordinated = "";
private String __cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_Model ()
{   super();
}

public String getCmmnt ()
{
    return __cmmnt;
}

public String getCoordinated ()
{
    return __coordinated;
}

public int getModelID ()
{
    return __modelID;
}

public String getModelName ()
{
    return __modelName;
}

public void setCmmnt ( String cmmnt )
{
    __cmmnt = cmmnt;
}


public void setCoordinated ( String coordinated )
{
    __coordinated = coordinated;
}

public void setModelID ( int modelID )
{
    __modelID = modelID;
}

public void setModelName ( String modelName )
{
    __modelName = modelName;
}

}
