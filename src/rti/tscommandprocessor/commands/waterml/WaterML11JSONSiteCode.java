package rti.tscommandprocessor.commands.waterml;

import com.google.gson.annotations.SerializedName;

/**
Corresponds to "{ value: { timeseries: sourceInfo : siteCode" WaterML 1.1 JSON
*/
public class WaterML11JSONSiteCode
{

private String value = "";
private String network = "";
private String siteID = "";
private String agencyCode = "";
private String agencyName = "";
@SerializedName("default")
private String _default = "";

/**
Constructor.
*/
public WaterML11JSONSiteCode ()
{
}

public String getAgencyCode () {
	return this.agencyCode;
}

public String getAgencyName () {
	return this.agencyName;
}

public String getDefault () {
	return this._default;
}

public String getNetwork () {
	return this.network;
}

public String getSiteID () {
	return this.siteID;
}

public String getValue () {
	return this.value;
}

public void setAgencyCode ( String agencyCode ) {
	this.agencyCode = agencyCode;
}

public void setAgencyName ( String agencyName ) {
	this.agencyName = agencyName;
}

public void setDefault ( String _default ) {
	this._default = _default;
}

public void setNetwork ( String network ) {
	this.network = network;
}

public void setSiteID ( String siteID ) {
	this.siteID = siteID;
}

public void setValue ( String value ) {
	this.value = value;
}

}