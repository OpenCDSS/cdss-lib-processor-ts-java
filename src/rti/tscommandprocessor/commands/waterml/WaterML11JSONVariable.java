// WaterML11JSONVariable - Corresponds to "{ value: { timeseries: variable" WaterML 1.1 JSON

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
Corresponds to "{ value: { timeseries: variable" WaterML 1.1 JSON
*/
public class WaterML11JSONVariable
{

List<WaterML11JSONVariableCode> variableCode = new ArrayList<WaterML11JSONVariableCode>();
String variableName = "";
String variableDescription = "";
String valueType = "";
String dataType = "";
String generalCategory = "";
String sampleMedium = "";
WaterML11JSONUnit unit = new WaterML11JSONUnit();
List<WaterML11JSONOption> options = new ArrayList<WaterML11JSONOption>();
String [] note = new String[0];
String related = null;
String extension = null;
String noDataValue = null;
String timeScale = null;
String speciation = null;
String categories = null;
String [] variableProperty = new String[0];
String old = null;
String metaDataTime = null;
List<WaterML11JSONTimeSeriesValues> values = new ArrayList<WaterML11JSONTimeSeriesValues>();
String name = "";

/**
Constructor.
*/
public WaterML11JSONVariable ()
{
}

public WaterML11JSONUnit getUnit () {
	return this.unit;
}

public String getNoDataValue () {
	return this.noDataValue;
}

public String getVariableName () {
	return this.variableName;
}

public void setNoDataValue ( String noDataValue ) {
	this.noDataValue = noDataValue;
}

public void setUnit ( WaterML11JSONUnit unit ) {
	this.unit = unit;
}

public void setVariableName ( String variableName ) {
	this.variableName = variableName;
}

}
