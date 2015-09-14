package rti.tscommandprocessor.commands.waterml;

import java.util.ArrayList;
import java.util.List;

/**
Corresponds to "{ value: { timeseries: variable" WaterML 1.1 JSON
*/
public class WaterML11JSONVariable
{

List<WaterML11JSONVariableCode> variableCode = new ArrayList<WaterML11JSONVariableCode>();
String variableName = "";
String variableDescription = "";
String valueType = "";
String dataType = "";
String generalCategory = "";
String sampleMedium = "";
WaterML11JSONUnit unit = new WaterML11JSONUnit();
List<WaterML11JSONOption> options = new ArrayList<WaterML11JSONOption>();
String [] note = new String[0];
String related = null;
String extension = null;
String noDataValue = null;
String timeScale = null;
String speciation = null;
String categories = null;
String [] variableProperty = new String[0];
String old = null;
String metaDataTime = null;
List<WaterML11JSONTimeSeriesValues> values = new ArrayList<WaterML11JSONTimeSeriesValues>();
String name = "";

/**
Constructor.
*/
public WaterML11JSONVariable ()
{
}

public WaterML11JSONUnit getUnit () {
	return this.unit;
}

public String getNoDataValue () {
	return this.noDataValue;
}

public String getVariableName () {
	return this.variableName;
}

public void setNoDataValue ( String noDataValue ) {
	this.noDataValue = noDataValue;
}

public void setUnit ( WaterML11JSONUnit unit ) {
	this.unit = unit;
}

public void setVariableName ( String variableName ) {
	this.variableName = variableName;
}

}