package rti.tscommandprocessor.commands.waterml;

import com.google.gson.annotations.SerializedName;

/**
Corresponds to "{ value: { timeseries: sourceInfo : timeZoneInfo" WaterML 1.1 JSON
*/
public class WaterML11JSONTimeZoneInfo
{

WaterML11JSONTimeZone defaultTimeZone = new WaterML11JSONTimeZone();
WaterML11JSONTimeZone daylightSavingsTimeZone = new WaterML11JSONTimeZone();
Boolean siteUsesDaylightSavingsTime = null;

/**
Constructor.
*/
public WaterML11JSONTimeZoneInfo ()
{
}

public WaterML11JSONTimeZone getDaylightSavingsTimeZone () {
	return this.daylightSavingsTimeZone;
}

public WaterML11JSONTimeZone getDefaultTimeZone () {
	return this.defaultTimeZone;
}

public Boolean siteUsesDaylightSavingsTime () {
	return this.siteUsesDaylightSavingsTime;
}

public void setDaylightSavingsTimeZone ( WaterML11JSONTimeZone daylightSavingsTimeZone ) {
	this.daylightSavingsTimeZone = daylightSavingsTimeZone;
}

public void setDefaultTimeZone ( WaterML11JSONTimeZone defaultTimeZone ) {
	this.defaultTimeZone = defaultTimeZone;
}

public void setSiteUsesDaylightSavingsTime ( Boolean siteUsesDaylightSavingsTime ) {
	this.siteUsesDaylightSavingsTime = siteUsesDaylightSavingsTime;
}

}