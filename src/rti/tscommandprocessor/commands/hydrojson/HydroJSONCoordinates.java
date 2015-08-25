package rti.tscommandprocessor.commands.hydrojson;

/**
Definition of station coordinates for HydroJSON format.
*/
public class HydroJSONCoordinates
{

// List these in the order of the specification example
/**
Latitude.
*/
private Double latitude = null;

/**
Longitude.
*/
private Double longitude = null;

/**
Coordinate datum.
*/
private String datum = "";

/**
Constructor.
*/
public HydroJSONCoordinates()
{
	
}

/**
Return the datum.
*/
public String getDatum()
{
	return this.datum;
}

/**
Return the latitude.
*/
public Double getLatitude()
{
	return this.latitude;
}

/**
Return the longitude.
*/
public Double getLongitude()
{
	return this.longitude;
}

/**
Set the datum.
*/
public void setDatum ( String datum )
{
	this.datum = datum;
}

/**
Set the latitude.
*/
public void setLatitude ( Double latitude )
{
	this.latitude = latitude;
}

/**
Set the latitude.
*/
public void setLongitude ( Double longitude )
{
	this.longitude = longitude;
}

}