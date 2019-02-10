// WaterML11JSONUnit - Corresponds to "{ value: { timeseries: variable : unit" WaterML 1.1 JSON

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
Corresponds to "{ value: { timeseries: variable : unit" WaterML 1.1 JSON
*/
public class WaterML11JSONUnit
{

String unitName = "";
String unitDescription = "";
String unitType = "";
String unitAbbreviation = "";
String unitCode = "";
String unitID = "";

/**
Constructor.
*/
public WaterML11JSONUnit ()
{
}

public String getUnitAbbreviation () {
	return this.unitAbbreviation;
}

public String getUnitCode () {
	return this.unitCode;
}

public String getUnitDescription () {
	return this.unitDescription;
}

public String getUnitID () {
	return this.unitID;
}

public String getUnitName () {
	return this.unitName;
}

public String getUnitType () {
	return this.unitType;
}

public void setUnitAbbreviation ( String unitAbbreviation ) {
	this.unitAbbreviation = unitAbbreviation;
}

public void setUnitCode ( String unitCode ) {
	this.unitCode = unitCode;
}

public void setUnitDescription ( String unitDescription ) {
	this.unitDescription = unitDescription;
}

public void setUnitID ( String unitID ) {
	this.unitID = unitID;
}

public void setUnitName ( String unitName ) {
	this.unitName = unitName;
}

public void setUnitType ( String unitType ) {
	this.unitType = unitType;
}

}
