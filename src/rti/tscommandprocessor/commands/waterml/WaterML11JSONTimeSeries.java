// WaterML11JSONTimeSeries - Corresponds to "{ value: { timeseries" WaterML 1.1 JSON

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
Corresponds to "{ value: { timeseries" WaterML 1.1 JSON
*/
public class WaterML11JSONTimeSeries
{

// TODO SAM 2015-09-09 add queryInfo when REST web services implemented
	
private WaterML11JSONSourceInfo sourceInfo = new WaterML11JSONSourceInfo();
private WaterML11JSONVariable variable = new WaterML11JSONVariable();
private List<WaterML11JSONTimeSeriesValue> values = new ArrayList<WaterML11JSONTimeSeriesValue>();
private String name = null;

/**
Constructor.
*/
public WaterML11JSONTimeSeries ()
{
}

public String getName () {
	return this.name;
}

public WaterML11JSONSourceInfo getSourceInfo () {
	return this.sourceInfo;
}

public WaterML11JSONVariable getVariable () {
	return this.variable;
}

public List<WaterML11JSONTimeSeriesValue> getValues () {
	return this.values;
}

public void setName ( String name ) {
	this.name = name;
}

public void setSourceInfo ( WaterML11JSONSourceInfo sourceInfo) {
	this.sourceInfo = sourceInfo;
}

public void setVariable ( WaterML11JSONVariable variable ) {
	this.variable = variable;
}

public void setValues ( List<WaterML11JSONTimeSeriesValue> values ) {
	this.values = values;
}

}
