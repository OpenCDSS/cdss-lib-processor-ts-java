package rti.tscommandprocessor.commands.wateroneflow.ws;

import java.net.MalformedURLException;
import java.util.List;

import org.cuahsi.waterml._1.TimeSeriesResponseType;
import org.cuahsi.waterml._1.TimeSeriesType;
import org.cuahsi.waterml._1.TsValuesSingleVariableType;
import org.cuahsi.waterml._1.ValueSingleVariable;

import RTi.TS.TS;
import RTi.Util.Message.Message;

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
Constructor.
*/
public WaterOneFlowAPI_1_0 ( String wsdlLocation )
throws MalformedURLException
{
    __wof = new org.cuahsi.his._1_0.ws.WaterOneFlow( wsdlLocation );
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
*/
public TS readTimeSeries ( )
{
    String routine = getClass().getName() + ".readTimeSeries";
    String location = "NWIS:03451500";
    String variable = "NWIS:00060";
    String startDate = "2010-01-10";
    String endDate = "2010-03-15";
    String authToken = "";
    org.cuahsi.his._1_0.ws.WaterOneFlow wof = getWaterOneFlow();
    // Get the time series as a String (WaterML?)
    try {
        String response = wof.getWaterOneFlowSoap12().getValues(location, variable, startDate, endDate, authToken);
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

}