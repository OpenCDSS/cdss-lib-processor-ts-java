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