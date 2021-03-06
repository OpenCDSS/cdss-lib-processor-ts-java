// WaterML11JSONOption - Corresponds to "{ value: { timeseries: variables : options: option" WaterML 1.1 JSON

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

/**
Corresponds to "{ value: { timeseries: variables : options: option" WaterML 1.1 JSON
*/
public class WaterML11JSONOption
{

String value = "";
String name = "";
String optionID = "";
String optionCode = "";

/**
Constructor.
*/
public WaterML11JSONOption ()
{
}

public String getName () {
	return this.name;
}

public String getOptionID () {
	return this.optionID;
}

public String getOptionCode () {
	return this.optionCode;
}

public String getValue () {
	return this.value;
}

public void setName ( String name ) {
	this.name = name;
}

public void setOptionID ( String optionID ) {
	this.optionID = optionID;
}

public void setOptionCode ( String optionCode ) {
	this.optionCode = optionCode;
}

public void setValue ( String value ) {
	this.value = value;
}

}
