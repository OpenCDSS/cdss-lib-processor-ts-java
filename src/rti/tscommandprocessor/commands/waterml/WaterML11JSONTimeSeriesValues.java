// WaterML11JSONTimeSeriesValues - Corresponds to "{ value: { timeseries: variable : values " WaterML 1.1 JSON

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
Corresponds to "{ value: { timeseries: variable : values " WaterML 1.1 JSON
*/
public class WaterML11JSONTimeSeriesValues
{

List<WaterML11JSONTimeSeriesValue> values = new ArrayList<WaterML11JSONTimeSeriesValue>();
/*
String units = null;
List<WaterML11JSONTimeSeriesValueQualifier> qualifier = new ArrayList<WaterML11JSONTimeSeriesValueQualifier>();
String [] qualityControlLevel = new String[0];
List<WaterML11JSONTimeSeriesValueMethod> method = new ArrayList<WaterML11JSONTimeSeriesValueMethod>();
String [] source = new String[0];
String [] offset = new String[0];
String [] sample = new String[0];
String [] censorCode = new String[0];
*/

/**
Constructor.
*/
public WaterML11JSONTimeSeriesValues ()
{
}

public List<WaterML11JSONTimeSeriesValue> getValues ()
{
	return this.values;
}

public void setValues ( List<WaterML11JSONTimeSeriesValue> values ) {
	this.values = values;
}

/*
public String [] getCensorCode () {
	return this.censorCode;
}

public List<WaterML11JSONTimeSeriesValueMethod> getMethod () {
	return this.method;
}

public String [] getOffset () {
	return this.offset;
}

public List<WaterML11JSONTimeSeriesValueQualifier> getQualifier () {
	return this.qualifier;
}

public String [] getQualityControlLevel () {
	return this.qualityControlLevel;
}

public String [] getSample () {
	return this.sample;
}

public String [] getSource () {
	return this.source;
}

public String getUnits () {
	return this.units;
}

public List<WaterML11JSONTimeSeriesValue> getValue () {
	return this.value;
}

public void setCensorCode ( String [] censorCode ) {
	this.censorCode = censorCode;
}

public void setMethod ( List<WaterML11JSONTimeSeriesValueMethod> method ) {
	this.method = method;
}

public void setOffset ( String [] offset ) {
	this.offset = offset;
}

public void setQualifier ( List<WaterML11JSONTimeSeriesValueQualifier> qualifier ) {
	this.qualifier = qualifier;
}

public void setQualityControlLevel ( String [] qualityControlLevel ) {
	this.qualityControlLevel = qualityControlLevel;
}

public void setSample ( String [] sample ) {
	this.sample = sample;
}

public void setSource ( String [] source ) {
	this.source = source;
}

public void setUnits ( String units ) {
	this.units = units;
}

public void setValue ( List<WaterML11JSONTimeSeriesValue> value ) {
	this.value = value;
}
*/

}
