package rti.tscommandprocessor.commands.hydrojson;

import RTi.Util.Time.DateTime;

/**
Definition of time series site quality for HydroJSON format.
*/
public class HydroJSONTimeSeriesSiteQuality
{

// List these in the order of the specification example
/**
Timestamp.
*/
private DateTime timestamp = null;

/**
value.
*/
private Double value = null;

/**
Constructor.
*/
public HydroJSONTimeSeriesSiteQuality()
{
	
}

/**
Return the timestamp.
*/
public DateTime getTimestamp()
{
	return this.timestamp;
}

/**
Return the value.
*/
public Double getValue()
{
	return this.value;
}

}