// WaterML11JSONTimeSeriesValue - Corresponds to "{ value: { timeseries: variable : values : value : value" WaterML 1.1 JSON

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

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
