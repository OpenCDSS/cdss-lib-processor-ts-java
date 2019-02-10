// WaterML11JSONQueryNote - Corresponds to "{ value: { queryInfo { note" WaterML 1.1 JSON

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
Corresponds to "{ value: { queryInfo { note" WaterML 1.1 JSON
*/
public class WaterML11JSONQueryNote
{

// TODO SAM 2015-09-13 Need to determine how to represent as an object
private String value = null;
private String type = null;
private String href = null;
private String title = null;
private String show = null;

/**
Constructor.
*/
public WaterML11JSONQueryNote ()
{
}

public String getHref () {
	return this.href;
}

public String getShow () {
	return this.show;
}

public String getTitle () {
	return this.title;
}

public String getType () {
	return this.type;
}

public String getValue () {
	return this.value;
}

public void setHref ( String href ) {
	this.href = href;
}

public void setShow ( String show ) {
	this.show = show;
}

public void setTitle ( String title ) {
	this.title = title;
}

public void setType ( String type ) {
	this.type = type;
}

public void setValue ( String value ) {
	this.value = value;
}

}
