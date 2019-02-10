// WaterOneFlowAPI - Generic interface to interact with WaterOneFlow, regardless of the WaterOneFlow version that is being used.

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

package rti.tscommandprocessor.commands.wateroneflow.ws;

import java.util.List;

import org.cuahsi.waterml._1.VariableInfoType;

import RTi.TS.TS;
import RTi.Util.Time.DateTime;

/**
Generic interface to interact with WaterOneFlow, regardless of the WaterOneFlow version that is being used.
*/
public interface WaterOneFlowAPI
{

/**
Read a time series from the WaterOneFlow web service.
*/
public TS readTimeSeries ( String networkName, String siteID, String variable, DateTime readStart, DateTime readEnd,
    String outputFile);

/**
Read a list of variables from the WaterOneFlow web service.
*/
public List<VariableInfoType> getVariableInfoTypeList();

}
