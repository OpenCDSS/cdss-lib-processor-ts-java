package rti.tscommandprocessor.commands.wateroneflow.ws;

import RTi.TS.TS;

/**
Generic interface to interact with WaterOneFlow, regardless of the WaterOneFlow version that is being used.
*/
public interface WaterOneFlowAPI
{

/**
Read a time series from the WaterOneFlow web service.
*/
public TS readTimeSeries ();

}