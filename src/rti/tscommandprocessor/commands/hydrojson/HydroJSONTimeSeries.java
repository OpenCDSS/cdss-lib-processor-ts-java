package rti.tscommandprocessor.commands.hydrojson;

import java.util.ArrayList;
import java.util.List;

import RTi.Util.Time.DateTime;

/**
Time series definition for HydroJSON format.
*/
public class HydroJSONTimeSeries
{

// List these in the order of the specification example
/**
Time series identifier.
*/
private String tsid = "";

/**
List of time series values (regular time series? where timestamp is 0+ from start?).
*/
private List<HydroJSONTimeSeriesValue> values = new ArrayList<HydroJSONTimeSeriesValue>();

/**
List of time series site quality value (irregular time series? where timestamp is date/time string?).
*/
private List<HydroJSONTimeSeriesSiteQuality> site_quality = new ArrayList<HydroJSONTimeSeriesSiteQuality>();

/**
Hash.
*/
private String hash = "";

/**
Quality type.
*/
private String quality_type = "";

/**
Parameter.
*/
private String parameter = "";

/**
Duration.
*/
private String duration = "";

/**
Interval.
*/
private String interval = "";

/**
Units.
*/
private String units = "";

/**
Count (non-missing?).
*/
private Integer count = null;

/**
Minimum value.
*/
private Double min_value = null;

/**
Maximum value.
*/
private Double max_value = null;

/**
Start timestamp.
*/
private String start_timestamp = null;

/**
End timestamp.
*/
private String end_timestamp = null;

/**
Constructor.
*/
public HydroJSONTimeSeries()
{
	
}

/**
Return the time series identifier.
*/
public String getTsid()
{
	return this.tsid;
}

/**
Return the list of values.
*/
public List<HydroJSONTimeSeriesValue> getValues ()
{
	return this.values;
}

/**
Set the time series value count.
*/
public void setCount ( Integer count ) 
{
	this.count = count;
}

/**
Set the time series duration.
*/
public void setDuration ( String duration ) 
{
	this.duration = duration;
}

/**
Set the time series end timestamp.
*/
public void setEndTimestamp ( String end_timestamp ) 
{
	this.end_timestamp = end_timestamp;
}

/**
Set the time series hash code.
*/
public void setHash ( String hash ) 
{
	this.hash = hash;
}

/**
Set the time series interval.
*/
public void setInterval ( String interval ) 
{
	this.interval = interval;
}

/**
Set the time series maximum value.
*/
public void setMaxValue ( Double max_value ) 
{
	this.max_value = max_value;
}

/**
Set the time series minimum value.
*/
public void setMinValue ( Double min_value ) 
{
	this.min_value = min_value;
}

/**
Set the time series parameter.
*/
public void setParameter ( String parameter ) 
{
	this.parameter = parameter;
}

/**
Set the time series quality type.
*/
public void setQualityType ( String quality_type ) 
{
	this.quality_type = quality_type;
}

/**
Set the time series start timestamp.
*/
public void setStartTimestamp ( String start_timestamp ) 
{
	this.start_timestamp = start_timestamp;
}

/**
Set the time series identifier.
*/
public void setTsid(String tsid)
{
	this.tsid = tsid;
}

/**
Set the time series units.
*/
public void setUnits ( String units ) 
{
	this.units = units;
}

}