// WaterML11JSONQueryInfo - Corresponds to "{ value: { queryInfo" WaterML 1.1 JSON

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
Corresponds to "{ value: { queryInfo" WaterML 1.1 JSON
*/
public class WaterML11JSONQueryInfo
{

private String creationTime = null;
private String queryURL = null;
private WaterML11JSONQueryCriteria criteria = new WaterML11JSONQueryCriteria();
private List<WaterML11JSONQueryNote> note = new ArrayList<WaterML11JSONQueryNote>();
private String extension = null;

/**
Constructor.
*/
public WaterML11JSONQueryInfo ()
{
}

public String getCreationTime () {
	return this.creationTime;
}

public WaterML11JSONQueryCriteria getCriteria () {
	return this.criteria;
}

public String getExtension () {
	return this.extension;
}

public List<WaterML11JSONQueryNote> getNote () {
	return this.note;
}

public String getQueryURL () {
	return this.queryURL;
}

public void setCreationTime ( String creationTime ) {
	this.creationTime = creationTime;
}

public void setCriteria ( WaterML11JSONQueryCriteria criteria ) {
	this.criteria = criteria;
}

public void setExtension ( String extension ) {
	this.extension = extension;
}

public void setNote ( List<WaterML11JSONQueryNote> note ) {
	this.note = note;
}

public void setQueryURL ( String queryURL ) {
	this.queryURL = queryURL;
}

}
