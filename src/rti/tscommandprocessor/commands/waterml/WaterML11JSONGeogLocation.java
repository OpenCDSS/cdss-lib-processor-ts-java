package rti.tscommandprocessor.commands.waterml;

/**
Corresponds to "{ value: { timeseries: sourceInfo : geoLocation : geogLocation" WaterML 1.1 JSON
*/
public class WaterML11JSONGeogLocation
{

private String srs = null;
private Double latitude = null;
private Double longitude = null;

/**
Constructor.
*/
public WaterML11JSONGeogLocation ()
{
}

public Double getLatitude () {
	return this.latitude;
}

public Double getLongitude () {
	return this.longitude;
}

public String getSrs () {
	return this.srs;
}

public void setLatitude ( Double latitude ) {
	this.latitude = latitude;
}

public void setLongitude ( Double longitude ) {
	this.longitude = longitude;
}

}