// ReclamationHDB_EnsembleKeyVal - Hold data from the Reclamation HDB database REF_ENSEMBLE_KEYVAL table

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
Hold data from the Reclamation HDB database REF_ENSEMBLE_KEYVAL table, which is a free-form list of ensemble properties.
*/
public class ReclamationHDB_EnsembleKeyVal extends DMIDataObject
{
   
private int __ensembleID = DMIUtil.MISSING_INT;
private String __keyName = "";
private String __keyValue = "";

/**
Constructor.
*/
public ReclamationHDB_EnsembleKeyVal ()
{   super();
}

public int getEnsembleID ()
{
    return __ensembleID;
}

public String getKeyName ()
{
    return __keyName;
}

public String getKeyValue ()
{
    return __keyValue;
}

public void setEnsembleID ( int ensembleID )
{
    __ensembleID = ensembleID;
}

public void setKeyName ( String keyName )
{
    __keyName = keyName;
}

public void setKeyValue ( String keyValue )
{
    __keyValue = keyValue;
}

}
