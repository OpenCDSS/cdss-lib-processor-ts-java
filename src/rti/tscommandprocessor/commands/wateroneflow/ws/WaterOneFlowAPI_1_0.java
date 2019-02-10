// WaterOneFlowAPI_1_0 - Implementation of WaterOneFlowAPI for version 1.0.

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

import java.net.MalformedURLException;
import java.util.List;
import java.util.Vector;

import org.cuahsi.his._1_0.ws.WaterOneFlowSoap;
import org.cuahsi.waterml._1.TimeSeriesResponseType;
import org.cuahsi.waterml._1.TimeSeriesType;
import org.cuahsi.waterml._1.TsValuesSingleVariableType;
import org.cuahsi.waterml._1.ValueSingleVariable;
import org.cuahsi.waterml._1.VariableInfoType;
import org.cuahsi.waterml._1.Variables;
import org.cuahsi.waterml._1.VariablesResponseType;

import RTi.TS.TS;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;

/**
Implementation of WaterOneFlowAPI for version 1.0.
*/
public class WaterOneFlowAPI_1_0 implements WaterOneFlowAPI
{

/**
SOAP services instance used for queries.
*/
private org.cuahsi.his._1_0.ws.WaterOneFlow __wof = null;

/**
Cached variable information list.
This is set in getVariableList().
*/
private List<VariableInfoType> __variableInfoTypeList = new Vector();

/**
Constructor.
*/
public WaterOneFlowAPI_1_0 ( String wsdlLocation )
throws MalformedURLException
{
    __wof = new org.cuahsi.his._1_0.ws.WaterOneFlow( wsdlLocation );
}

/**
Get the list of variables for the data store.
*/
public List<VariableInfoType> getVariableInfoTypeList ( )
{
    if ( (__variableInfoTypeList == null) || (__variableInfoTypeList.size() == 0) ) {
        __variableInfoTypeList = readVariableInfoTypeList();
    }
    return __variableInfoTypeList;
}

/**
Return the WaterOneFlow instance that is used for queries.
*/
private org.cuahsi.his._1_0.ws.WaterOneFlow getWaterOneFlow ()
{
    return __wof;
}
    
/**
Read a time series from the WaterOneFlow web service.
@param network the network identifier (e.g., "NWIS").
@param locationID the location identifier (e.g., USGS gage ID like "03451500")
@param variable the variable type (e.g., "00060" for USGS streamflow)
@param startDate the starting date/time for the read
@param endDate the ending date/time for the read
@param outputFile the name of the output file to save the WaterML time series payload
*/
public TS readTimeSeries ( String networkName, String siteID, String variable, DateTime readStart, DateTime readEnd,
    String outputFile )
{
    String routine = getClass().getName() + ".readTimeSeries";
    String location = networkName + ":" + siteID; //"03451500";
    String variable2 = networkName + ":" + variable; // 00060";
    String startDate = "2010-01-10";
    String endDate = "2010-03-15";
    String authToken = "";
    org.cuahsi.his._1_0.ws.WaterOneFlow wof = getWaterOneFlow();
    WaterOneFlowSoap wofSoap = wof.getWaterOneFlowSoap12();
    // Get the time series as a String (WaterML?)
    //wofSoap.get
    try {
        String response = wofSoap.getValues(location, variable2, startDate, endDate, authToken);
        Message.printStatus(2, routine, response);
    }
    catch ( Exception e ) {
        Message.printWarning(2, routine, "Error calling getValues() (" + e + ")" );
    }
    // Get the time series as an object
    TimeSeriesResponseType tsResponse = wof.getWaterOneFlowSoap12().getValuesObject(
        location, variable, startDate, endDate, authToken);
    /* TODO SAM 2012-03-08 This code worked with his 1.1 WISDL, but 1.1 is a development release
        that seems to have some compatibility issues

    List<TimeSeriesType> timeSeriesTypeList = tsResponse.getTimeSeries();
    Message.printStatus ( 2, routine, "tsResponse.getTimeSeries().size=" + timeSeriesTypeList.size() );
    for ( TimeSeriesType timeSeriesType: timeSeriesTypeList ) {
        List<TsValuesSingleVariableType> tsValuesSingleVariableTypeList = timeSeriesType.getValues();
        Message.printStatus ( 2, routine, "timeSeriesType.getValues().size=" + tsValuesSingleVariableTypeList.size() );
        for ( TsValuesSingleVariableType tsValuesSingleVariableType: tsValuesSingleVariableTypeList ) {
            List<ValueSingleVariable> valueSingleVariableList = tsValuesSingleVariableType.getValue();
            Message.printStatus ( 2, routine, "tsValuesSingleVariableType.getValue().size=" + tsValuesSingleVariableType.getValue().size() );
            for ( ValueSingleVariable valueSingleVariable: valueSingleVariableList ) {
                Message.printStatus ( 2, routine,
                    "date/time=" + valueSingleVariable.getDateTime() +
                    " value=" + valueSingleVariable.getValue() );
            }
        }
    }
    */
    // The following works with API code generated from 1.0 USGS NWIS
    TimeSeriesType tsType = tsResponse.getTimeSeries();
    TsValuesSingleVariableType values = tsType.getValues();
    List<ValueSingleVariable> valueSingleVariable = values.getValue();
    for ( ValueSingleVariable val: valueSingleVariable ) {
        Message.printStatus ( 2, routine, "date/time=" + val.getDateTime() + " value=" + val.getValue() );
    }
    return null;
}

/**
Read the variable list for the data store.
*/
public List<VariableInfoType> readVariableInfoTypeList ()
{
    org.cuahsi.his._1_0.ws.WaterOneFlow wof = getWaterOneFlow();
    WaterOneFlowSoap wofSoap = wof.getWaterOneFlowSoap12();
    String variable = ""; // Will cause a full list of variables to be returned
    String authToken = "";
    VariablesResponseType variablesResponseType = wofSoap.getVariableInfoObject(variable, authToken);
    Variables variables = variablesResponseType.getVariables();
    List<VariableInfoType> variableInfoTypeList = variables.getVariable();
    Message.printStatus(2,"","Read " + variableInfoTypeList.size() +
        " VariableInfoType (likely multiple variables under each)");
    return variableInfoTypeList;
}

}
