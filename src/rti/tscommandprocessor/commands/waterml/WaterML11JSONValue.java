// WaterML11JSONValue - Corresponds to "{ value:" WaterML 1.1 JSON

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
Corresponds to "{ value:" WaterML 1.1 JSON
*/
public class WaterML11JSONValue
{

// TODO SAM 2015-09-09 add queryInfo when REST web services implemented

WaterML11JSONQueryInfo queryInfo = new WaterML11JSONQueryInfo();

List<WaterML11JSONTimeSeries> timeSeries = new ArrayList<WaterML11JSONTimeSeries>();

/**
Constructor.
*/
public WaterML11JSONValue ()
{
}

public WaterML11JSONQueryInfo getQueryInfo () {
	return this.queryInfo;
}

public List<WaterML11JSONTimeSeries> getTimeSeries () {
	return this.timeSeries;
}

public void setQueryInfo ( WaterML11JSONQueryInfo queryInfo ) {
	this.queryInfo = queryInfo;
}

public void setTimeSeries ( List<WaterML11JSONTimeSeries> timeSeries ) {
	this.timeSeries = timeSeries;
}

}
