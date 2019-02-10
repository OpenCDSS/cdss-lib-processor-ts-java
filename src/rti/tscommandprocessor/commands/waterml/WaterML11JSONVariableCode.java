// WaterML11JSONVariableCode - Corresponds to "{ value: { timeseries: sourceInfo : siteCode" WaterML 1.1 JSON

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
