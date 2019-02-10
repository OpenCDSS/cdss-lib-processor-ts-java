// ReclamationHDB_ObjectType - Hold data from the Reclamation HDB database HDB_OBJECTTYPE table

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
Hold data from the Reclamation HDB database HDB_OBJECTTYPE table, used mainly to look up the
object type from the HDB_SITE data.
*/
public class ReclamationHDB_ObjectType extends DMIDataObject
{
   
private int __objectTypeID = DMIUtil.MISSING_INT;
private String __objectTypeName = "";
private String __objectTypeTag = "";
private int __objectTypeParentOrder = DMIUtil.MISSING_INT;

/**
Constructor.
*/
public ReclamationHDB_ObjectType ()
{   super();
}

public int getObjectTypeID ()
{
    return __objectTypeID;
}

public String getObjectTypeName ()
{
    return __objectTypeName;
}

public int getObjectTypeParentOrder ()
{
    return __objectTypeParentOrder;
}

public String getObjectTypeTag ()
{
    return __objectTypeTag;
}

public void setObjectTypeID ( int objectTypeID )
{
    __objectTypeID = objectTypeID;
}

public void setObjectTypeName ( String objectTypeName )
{
    __objectTypeName = objectTypeName;
}

public void setObjectTypeParentOrder ( int objectTypeParentOrder )
{
    __objectTypeParentOrder = objectTypeParentOrder;
}

public void setObjectTypeTag ( String objectTypeTag )
{
    __objectTypeTag = objectTypeTag;
}

}
