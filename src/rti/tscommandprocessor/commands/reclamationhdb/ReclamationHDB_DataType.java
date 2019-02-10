// ReclamationHDB_DataType - Hold data from the Reclamation HDB database HDB_DATATYPE table

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
Hold data from the Reclamation HDB database HDB_DATATYPE table, used mainly to look up the
DATATYPE_ID from the DATATYPE_NAME and DATATYPE_COMMON_NAME and to give users choices of data types.
*/
public class ReclamationHDB_DataType extends DMIDataObject
{
   
private int __dataTypeID = DMIUtil.MISSING_INT;
private String __dataTypeName = "";
private String __dataTypeCommonName = "";
private String __physicalQuantityName = "";
private int __unitID = DMIUtil.MISSING_INT;
private String __allowableIntervals = "";
private int __agenID = DMIUtil.MISSING_INT;
private String __cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_DataType ()
{   super();
}

public int getAgenID ()
{
    return __agenID;
}

public String getAllowableIntervals ()
{
    return __allowableIntervals;
}

public String getCmmnt ()
{
    return __cmmnt;
}

public String getDataTypeCommonName ()
{
    return __dataTypeCommonName;
}

public int getDataTypeID ()
{
    return __dataTypeID;
}

public String getDataTypeName ()
{
    return __dataTypeName;
}

public String getPhysicalQuantityName ()
{
    return __physicalQuantityName;
}

public int getUnitID ()
{
    return __unitID;
}

public void setAgenID ( int agenID )
{
    __agenID = agenID;
}

public void setAllowableIntervals ( String allowableIntervals )
{
    __allowableIntervals = allowableIntervals;
}

public void setCmmnt ( String cmmnt )
{
    __cmmnt = cmmnt;
}

public void setDataTypeCommonName ( String dataTypeCommonName )
{
    __dataTypeCommonName = dataTypeCommonName;
}

public void setDataTypeID ( int dataTypeID )
{
    __dataTypeID = dataTypeID;
}

public void setDataTypeName ( String dataTypeName )
{
    __dataTypeName = dataTypeName;
}

public void setPhysicalQuantityName ( String physicalQuantityName )
{
    __physicalQuantityName = physicalQuantityName;
}

public void setUnitID ( int unitID )
{
    __unitID = unitID;
}

}
