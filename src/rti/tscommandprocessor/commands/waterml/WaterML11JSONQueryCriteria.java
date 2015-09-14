package rti.tscommandprocessor.commands.waterml;

import java.util.ArrayList;
import java.util.List;

/**
Corresponds to "{ value: { queryInfo { criteria" WaterML 1.1 JSON
*/
public class WaterML11JSONQueryCriteria
{

// TODO SAM 2015-09-13 Need to determine how to represent as an object
private String locationParam = null;
private String variableParam = null;
private String timeParam = null;
private List<String> parameter = new ArrayList<String>();
private String methodCalled = null;

/**
Constructor.
*/
public WaterML11JSONQueryCriteria ()
{
}

public String getLocationParam () {
	return this.locationParam;
}

public String getMethodCalled () {
	return this.methodCalled;
}

public List<String> parameter () {
	return this.parameter;
}

public String getTimeParam () {
	return this.timeParam;
}

public String getVariableParam () {
	return this.variableParam;
}

public void setLocationParam ( String locationParam ) {
	this.locationParam = locationParam;
}

public void setMethodCalled ( String methodCalled ) {
	this.methodCalled = methodCalled;
}

public void setParameter ( List<String> parameter ) {
	this.parameter = parameter;
}

public void setTimeParam ( String timeParam ) {
	this.timeParam = timeParam;
}

public void setVariableParam ( String variableParam ) {
	this.variableParam = variableParam;
}

}