package rti.tscommandprocessor.commands.waterml;

/**
Corresponds to "{ value: { timeseries: sourceInfo : geoLocation" WaterML 1.1 JSON
*/
public class WaterML11JSONGeoLocation
{

WaterML11JSONGeogLocation geogLocation = new WaterML11JSONGeogLocation();
// TODO SAM 2015-09-09 Type is probably wrong but fix later
String [] localSiteXY = new String[0];

/**
Constructor.
*/
public WaterML11JSONGeoLocation ()
{
}

public WaterML11JSONGeogLocation getGeogLocation () {
	return this.geogLocation;
}

public String [] getLocalSiteXY () {
	return this.localSiteXY;
}

public void setGeogLocation ( WaterML11JSONGeogLocation geogLocation ) {
	this.geogLocation = geogLocation;
}

public void setLocalSiteXY ( String [] localSiteXY ) {
	this.localSiteXY = localSiteXY;
}

}