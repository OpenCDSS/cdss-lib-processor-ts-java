package rti.tscommandprocessor.commands.waterml;

/**
Corresponds to "{ value: { timeseries: sourceInfo : siteProperty" WaterML 1.1 JSON
*/
public class WaterML11JSONSiteProperty
{

String value = "";
String type = "";
String name = "";
String uri = "";

/**
Constructor.
*/
public WaterML11JSONSiteProperty ()
{
}

public String getName () {
	return this.name;
}

public String getType () {
	return this.type;
}

public String getUri () {
	return this.uri;
}

public String getValue () {
	return this.value;
}

public void setName ( String name ) {
	this.name = name;
}

public void setType ( String type ) {
	this.type = type;
}

public void setUri ( String uri ) {
	this.uri = uri;
}

public void setValue ( String value ) {
	this.value = value;
}

}