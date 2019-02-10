// WaterML11JSONQueryCriteria - Corresponds to "{ value: { queryInfo { criteria" WaterML 1.1 JSON

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
Corresponds to "{ value: { queryInfo { criteria" WaterML 1.1 JSON
*/
public class WaterML11JSONQueryCriteria
{

// TODO SAM 2015-09-13 Need to determine how to represent as an object
private String locationParam = null;
private String variableParam = null;
private String timeParam = null;
private List<String> parameter = new ArrayList<String>();
private String methodCalled = null;

/**
Constructor.
*/
public WaterML11JSONQueryCriteria ()
{
}

public String getLocationParam () {
	return this.locationParam;
}

public String getMethodCalled () {
	return this.methodCalled;
}

public List<String> parameter () {
	return this.parameter;
}

public String getTimeParam () {
	return this.timeParam;
}

public String getVariableParam () {
	return this.variableParam;
}

public void setLocationParam ( String locationParam ) {
	this.locationParam = locationParam;
}

public void setMethodCalled ( String methodCalled ) {
	this.methodCalled = methodCalled;
}

public void setParameter ( List<String> parameter ) {
	this.parameter = parameter;
}

public void setTimeParam ( String timeParam ) {
	this.timeParam = timeParam;
}

public void setVariableParam ( String variableParam ) {
	this.variableParam = variableParam;
}

}
