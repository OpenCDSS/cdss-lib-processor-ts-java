package rti.tscommandprocessor.commands.waterml;

import java.util.ArrayList;
import java.util.List;

/**
Corresponds to "{ value: { timeseries" WaterML 1.1 JSON
*/
public class WaterML11JSONTimeSeries
{

// TODO SAM 2015-09-09 add queryInfo when REST web services implemented
	
private WaterML11JSONSourceInfo sourceInfo = new WaterML11JSONSourceInfo();
private WaterML11JSONVariable variable = new WaterML11JSONVariable();
private List<WaterML11JSONTimeSeriesValue> values = new ArrayList<WaterML11JSONTimeSeriesValue>();
private String name = null;

/**
Constructor.
*/
public WaterML11JSONTimeSeries ()
{
}

public String getName () {
	return this.name;
}

public WaterML11JSONSourceInfo getSourceInfo () {
	return this.sourceInfo;
}

public WaterML11JSONVariable getVariable () {
	return this.variable;
}

public List<WaterML11JSONTimeSeriesValue> getValues () {
	return this.values;
}

public void setName ( String name ) {
	this.name = name;
}

public void setSourceInfo ( WaterML11JSONSourceInfo sourceInfo) {
	this.sourceInfo = sourceInfo;
}

public void setVariable ( WaterML11JSONVariable variable ) {
	this.variable = variable;
}

public void setValues ( List<WaterML11JSONTimeSeriesValue> values ) {
	this.values = values;
}

}