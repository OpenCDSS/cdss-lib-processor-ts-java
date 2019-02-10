// WaterML11JSONTimeSeriesValueQualifier - Corresponds to "{ value: { timeseries: qualifier" WaterML 1.1 JSON

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
