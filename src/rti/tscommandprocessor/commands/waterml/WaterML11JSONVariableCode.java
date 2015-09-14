package rti.tscommandprocessor.commands.waterml;

import com.google.gson.annotations.SerializedName;

/**
Corresponds to "{ value: { timeseries: sourceInfo : siteCode" WaterML 1.1 JSON
*/
public class WaterML11JSONVariableCode
{

String value = "";
String network = "";
String vocabulary = "";
String variableID = "";
@SerializedName("default")
String _default = "";

/**
Constructor.
*/
public WaterML11JSONVariableCode ()
{
}

public String getDefault () {
	return this._default;
}

public String getNetwork () {
	return this.network;
}

public String getValue () {
	return this.value;
}

public String getVariableID () {
	return this.variableID;
}

public String getVocabulary () {
	return this.vocabulary;
}

public void setDefault ( String _default ) {
	this._default = _default;
}

public void setNetwork ( String network ) {
	this.network = network;
}

public void setValue ( String value ) {
	this.value = value;
}

public void setVariableID ( String variableID ) {
	this.variableID = variableID;
}

public void setVocabulary ( String vocabulary ) {
	this.vocabulary = vocabulary;
}

}