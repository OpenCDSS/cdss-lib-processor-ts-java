// ReclamationHDB_Ensemble - Hold data from the Reclamation HDB database REF_ENSEMBLE table

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
Hold data from the Reclamation HDB database REF_ENSEMBLE table, used mainly to look up the
MODEL_ID from the ENSEMBLE_NAME and MODEL_NAME and to give users choices of ensembles.
*/
public class ReclamationHDB_Ensemble extends DMIDataObject
{
   
private int __ensembleID = DMIUtil.MISSING_INT;
private String __ensembleName = "";
private int __agenID = DMIUtil.MISSING_INT;
private String __traceDomain = "";
private String __cmmnt = "";

/**
Constructor.
*/
public ReclamationHDB_Ensemble ()
{   super();
}

public int getAgenID ()
{
    return __agenID;
}

public String getCmmnt ()
{
    return __cmmnt;
}

public int getEnsembleID ()
{
    return __ensembleID;
}

public String getEnsembleName ()
{
    return __ensembleName;
}

public String getTraceDomain ()
{
    return __traceDomain;
}

public void setAgenID ( int agenID )
{
    __agenID = agenID;
}

public void setCmmnt ( String cmmnt )
{
    __cmmnt = cmmnt;
}

public void setEnsembleID ( int ensembleID )
{
    __ensembleID = ensembleID;
}

public void setEnsembleName ( String ensembleName )
{
    __ensembleName = ensembleName;
}

public void setTraceDomain ( String traceDomain )
{
    __traceDomain = traceDomain;
}

}
