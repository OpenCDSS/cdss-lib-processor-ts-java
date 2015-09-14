package rti.tscommandprocessor.commands.waterml;

/**
Corresponds to "{ value: { timeseries: variable : unit" WaterML 1.1 JSON
*/
public class WaterML11JSONUnit
{

String unitName = "";
String unitDescription = "";
String unitType = "";
String unitAbbreviation = "";
String unitCode = "";
String unitID = "";

/**
Constructor.
*/
public WaterML11JSONUnit ()
{
}

public String getUnitAbbreviation () {
	return this.unitAbbreviation;
}

public String getUnitCode () {
	return this.unitCode;
}

public String getUnitDescription () {
	return this.unitDescription;
}

public String getUnitID () {
	return this.unitID;
}

public String getUnitName () {
	return this.unitName;
}

public String getUnitType () {
	return this.unitType;
}

public void setUnitAbbreviation ( String unitAbbreviation ) {
	this.unitAbbreviation = unitAbbreviation;
}

public void setUnitCode ( String unitCode ) {
	this.unitCode = unitCode;
}

public void setUnitDescription ( String unitDescription ) {
	this.unitDescription = unitDescription;
}

public void setUnitID ( String unitID ) {
	this.unitID = unitID;
}

public void setUnitName ( String unitName ) {
	this.unitName = unitName;
}

public void setUnitType ( String unitType ) {
	this.unitType = unitType;
}

}