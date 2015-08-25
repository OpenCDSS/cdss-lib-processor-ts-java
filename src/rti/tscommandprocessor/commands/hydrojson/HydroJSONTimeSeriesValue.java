package rti.tscommandprocessor.commands.hydrojson;

/**
Definition of time series value for HydroJSON format.
*/
public class HydroJSONTimeSeriesValue
{

// List these in the order of the specification example
/**
Timestamp - use a string to make sure it formats correctly.
*/
private String timestamp = null;

/**
Value.
*/
private Double value = null;

/**
Quality.
*/
private String quality = "";

/**
Constructor.
*/
public HydroJSONTimeSeriesValue()
{
	
}

/**
Return the timestamp.
*/
public String getTimestamp()
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

/**
Return the quality.
*/
public String getQuality()
{
	return this.quality;
}

/**
Set the timestamp.
*/
public void setTimestamp ( String timestamp )
{
	this.timestamp = timestamp;
}

/**
Set the value.
*/
public void setValue ( Double value )
{
	this.value = value;
}

/**
Set the quality.
*/
public void setQuality ( String quality )
{
	this.quality = quality;
}

}