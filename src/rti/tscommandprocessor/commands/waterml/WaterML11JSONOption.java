package rti.tscommandprocessor.commands.waterml;

/**
Corresponds to "{ value: { timeseries: variables : options: option" WaterML 1.1 JSON
*/
public class WaterML11JSONOption
{

String value = "";
String name = "";
String optionID = "";
String optionCode = "";

/**
Constructor.
*/
public WaterML11JSONOption ()
{
}

public String getName () {
	return this.name;
}

public String getOptionID () {
	return this.optionID;
}

public String getOptionCode () {
	return this.optionCode;
}

public String getValue () {
	return this.value;
}

public void setName ( String name ) {
	this.name = name;
}

public void setOptionID ( String optionID ) {
	this.optionID = optionID;
}

public void setOptionCode ( String optionCode ) {
	this.optionCode = optionCode;
}

public void setValue ( String value ) {
	this.value = value;
}

}