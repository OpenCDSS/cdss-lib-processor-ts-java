// WaterML11JSONSiteProperty - Corresponds to "{ value: { timeseries: sourceInfo : siteProperty" WaterML 1.1 JSON

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
Corresponds to "{ value: { timeseries: sourceInfo : siteProperty" WaterML 1.1 JSON
*/
public class WaterML11JSONSiteProperty
{

String value = "";
String type = "";
String name = "";
String uri = "";

/**
Constructor.
*/
public WaterML11JSONSiteProperty ()
{
}

public String getName () {
	return this.name;
}

public String getType () {
	return this.type;
}

public String getUri () {
	return this.uri;
}

public String getValue () {
	return this.value;
}

public void setName ( String name ) {
	this.name = name;
}

public void setType ( String type ) {
	this.type = type;
}

public void setUri ( String uri ) {
	this.uri = uri;
}

public void setValue ( String value ) {
	this.value = value;
}

}
