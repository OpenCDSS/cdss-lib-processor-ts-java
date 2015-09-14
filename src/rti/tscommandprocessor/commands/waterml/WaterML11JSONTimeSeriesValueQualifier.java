package rti.tscommandprocessor.commands.waterml;

import com.google.gson.annotations.SerializedName;

/**
Corresponds to "{ value: { timeseries: qualifier" WaterML 1.1 JSON
*/
public class WaterML11JSONTimeSeriesValueQualifier
{

String qualifierCode = "";
String qualifierDescription = "";
String qualifierID = "";
String network = "";
String vocabulary = "";
@SerializedName("default")
String _default = "";

/**
Constructor.
*/
public WaterML11JSONTimeSeriesValueQualifier ()
{
}

public String getDefault () {
	return this._default;
}

public String getQualifierCode () {
	return this.qualifierCode;
}

public String getQualifierDescription () {
	return this.qualifierDescription;
}

public String getQualifierID () {
	return this.qualifierID;
}

public String getNetwork () {
	return this.network;
}

public String getVocabulary () {
	return this.vocabulary;
}

public void setDefault ( String _default ) {
	this._default = _default;
}

public void setQualifierCode ( String qualifierCode ) {
	this.qualifierCode = qualifierCode;
}

public void setQualifierDescription ( String qualifierDescription ) {
	this.qualifierDescription = qualifierDescription;
}

public void setQualifierID ( String qualifierID ) {
	this.qualifierID = qualifierID;
}

public void setNetwork ( String network ) {
	this.network = network;
}

public void setVocabulary ( String vocabulary ) {
	this.vocabulary = vocabulary;
}

}