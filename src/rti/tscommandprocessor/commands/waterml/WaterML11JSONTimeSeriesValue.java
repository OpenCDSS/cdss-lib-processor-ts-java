package rti.tscommandprocessor.commands.waterml;

import java.util.ArrayList;
import java.util.List;

/**
Corresponds to "{ value: { timeseries: variable : values : value : value" WaterML 1.1 JSON
*/
public class WaterML11JSONTimeSeriesValue
{

private String value = null;
private String dateTimeAccuracyCd = null;
private List<String> qualifiers = new ArrayList<String>();
private String censorCode = null;
private String dateTime = null;
private String timeOffset = null;
private String dateTimeUTC = null;
private String methodID = null;
private String sourceID = null;
private String accuracyStdDev = null;
private String sampleID = null;
private String methodCode = null;
private String sourceCode = null;
private String labSampleCode = null;
private String offsetValue = null;
private String offsetTypeID = null;
private String offsetTypeCode = null;
private String codeVocabulary = null;
private String codeVocabularyTerm = null;
private String qualityControlLevelCode = null;
private String metadataTime = null;
private String oid = null;

/**
Constructor.
*/
public WaterML11JSONTimeSeriesValue ()
{
}

public String getCensorCode () {
	return this.censorCode;
}

public String getDateTime () {
	return this.dateTime;
}

public String getDateTimeAccuracyCd () {
	return this.dateTimeAccuracyCd;
}

public String getDateTimeUTC () {
	return this.dateTimeUTC;
}

public String getMethodID () {
	return this.methodID;
}

public List<String> getQualifiers () {
	return this.qualifiers;
}

public String getSourceID () {
	return this.sourceID;
}

public String getValue () {
	return this.value;
}

public void setCensorCode ( String censorCode ) {
	this.censorCode = censorCode;
}

public void setDateTime ( String dateTime ) {
	this.dateTime = dateTime;
}

public void setDateTimeAccuracyCd ( String dateTimeAccuracyCd ) {
	this.dateTimeAccuracyCd = dateTimeAccuracyCd;
}

public void setDateTimeUTC ( String dateTimeUTC ) {
	this.dateTimeUTC = dateTimeUTC;
}

public void setMethodID ( String methodID ) {
	this.methodID = methodID;
}

public void setQualifiers( List<String> qualifiers ) {
	this.qualifiers = qualifiers;
}

public void setSourceID ( String sourceID ) {
	this.sourceID = sourceID;
}

public void setValue ( String value ) {
	this.value = value;
}

}