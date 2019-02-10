// ReclamationHDB_EnsembleTrace - Hold data from the Reclamation HDB database REF_ENSEMBLE_TRACE table

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
Hold data from the Reclamation HDB database REF_ENSEMBLE_TRACE table, which contains individual identification
information for traces, short of what is stored in the model time series tables.
*/
public class ReclamationHDB_EnsembleTrace extends DMIDataObject
{
   
private int __ensembleID = DMIUtil.MISSING_INT;
private int __traceID = DMIUtil.MISSING_INT;
private int __traceNumeric = DMIUtil.MISSING_INT;
private String __traceName = "";
private int __modelRunID = DMIUtil.MISSING_INT;

/**
Constructor.
*/
public ReclamationHDB_EnsembleTrace ()
{   super();
}

public int getEnsembleID ()
{
    return __ensembleID;
}

public int getModelRunID ()
{
    return __modelRunID;
}

public int getTraceID ()
{
    return __traceID;
}

public int getTraceNumeric ()
{
    return __traceNumeric;
}

public String getTraceName ()
{
    return __traceName;
}

public void setEnsembleID ( int ensembleID )
{
    __ensembleID = ensembleID;
}

public void setModelRunID ( int modelRunID )
{
    __modelRunID = modelRunID;
}

public void setTraceID ( int traceID )
{
    __traceID = traceID;
}

public void setTraceNumeric ( int traceNumeric )
{
    __traceNumeric = traceNumeric;
}

public void setTraceName ( String traceName )
{
    __traceName = traceName;
}

}
