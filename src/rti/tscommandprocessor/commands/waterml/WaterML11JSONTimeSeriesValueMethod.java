package rti.tscommandprocessor.commands.waterml;

/**
Corresponds to "{ value: { timeseries: method" WaterML 1.1 JSON
*/
public class WaterML11JSONTimeSeriesValueMethod
{

String methodCode = "";
String methodDescription = "";
String methodLink = "";
String methodID = "";

/**
Constructor.
*/
public WaterML11JSONTimeSeriesValueMethod ()
{
}

public String getMethodCode () {
	return this.methodCode;
}

public String getMethodDescription () {
	return this.methodDescription;
}

public String getMethodID () {
	return this.methodID;
}

public String getMethodLink () {
	return this.methodLink;
}

public void setMethodCode ( String methodCode ) {
	this.methodCode = methodCode;
}

public void setMethodDescription ( String methodDescription ) {
	this.methodDescription = methodDescription;
}

public void setMethodID ( String methodID ) {
	this.methodID = methodID;
}

public void setMethodLink ( String methodLink ) {
	this.methodLink = methodLink;
}

}