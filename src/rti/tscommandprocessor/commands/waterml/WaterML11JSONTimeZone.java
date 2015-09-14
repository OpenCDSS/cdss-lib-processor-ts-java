package rti.tscommandprocessor.commands.waterml;

/**
Corresponds to "{ value: { timeseries: sourceInfo : timeZoneInfo : *timeZone" WaterML 1.1 JSON
*/
public class WaterML11JSONTimeZone
{

String zoneOffset = null;
String zoneAbbrevation = null;

/**
Constructor.
*/
public WaterML11JSONTimeZone ()
{
}

public String getZoneAbbrevation () {
	return this.zoneAbbrevation;
}

public String getZoneOffset () {
	return this.zoneOffset;
}

public void setZoneAbbrevation ( String zoneAbbrevation ) {
	this.zoneAbbrevation = zoneAbbrevation;
}

public void setZoneOffset ( String zoneOffset ) {
	this.zoneOffset = zoneOffset;
}

}