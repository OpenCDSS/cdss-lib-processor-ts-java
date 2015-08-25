package rti.tscommandprocessor.commands.hydrojson;

/**
Definition of station elevation for HydroJSON format.
*/
public class HydroJSONElevation
{

// List these in the order of the specification example
/**
Elevation value.
*/
private Double value = null;

/**
Accuracy.
*/
private Double accuracy = null;

/**
Elevation datum.
*/
private String datum = "";

/**
Method of determining elevation.
*/
private String method = "";

/**
Constructor.
*/
public HydroJSONElevation()
{
	
}

/**
Return the value.
*/
public Double getValue()
{
	return this.value;
}

/**
Return the accuracy.
*/
public Double getAccuracy()
{
	return this.accuracy;
}

/**
Return the datum.
*/
public String getDatum()
{
	return this.datum;
}

/**
Return the method of determining the elevation.
*/
public String getMethod()
{
	return this.method;
}

/**
Set the value.
*/
public void setValue ( Double value )
{
	this.value = value;
}

/**
Set the accuracy.
*/
public void setAccuracy ( Double accuracy)
{
	this.accuracy = accuracy;
}

/**
Set the datum.
*/
public void setDatum ( String datum )
{
	this.datum = datum;
}

/**
Set the method of determining the elevation.
*/
public void setMethod ( String method)
{
	this.method = method;
}

}