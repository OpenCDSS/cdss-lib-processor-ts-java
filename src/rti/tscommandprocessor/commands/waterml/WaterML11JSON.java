// WaterML11JSON - This class is the top-level MINIMALIST class for WaterML 1.1 JSON content, corresponding to WaterML 1.1.

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
This class is the top-level MINIMALIST class for WaterML 1.1 JSON content, corresponding to WaterML 1.1.
It is likely that the USGS JSON is output by something like GSON using POJO objects with getters and setters.
Consequently, take a simple approach here in building a minimalist hierarchy of classes to mimic the standard.
This class and classes that it references are used by GSON to write the JSON file.
*/
public class WaterML11JSON
{

// List the data members in the order shown by USGS WaterML 1.1 output
private String name = "ns1:timeSeriesResponseType";

private String declarationType = "org.cuahsi.waterml.TimeSeriesResponseType";

private String scope = "javax.xml.bind.JAXBElement$GlobalScope";

private WaterML11JSONValue value = new WaterML11JSONValue();

private boolean nil = false;

private boolean globalScope = true;

private boolean typeSubstituted = false;

/**
Constructor for content.
*/
public WaterML11JSON ()
{
	
}

/**
Return the declared type.
*/
public String getDeclarationType ()
{
	return this.declarationType;
}

/**
Return the global scope.
*/
public boolean getGlobalScope ()
{
	return this.globalScope;
}

/**
Return the name.
*/
public String getName ()
{
	return this.name;
}

public boolean getNil ()
{
	return this.nil;
}

/**
Return the scope.
*/
public String getScope ()
{
	return this.scope;
}

public boolean getTypeSubstituted ()
{
	return this.typeSubstituted;
}

public WaterML11JSONValue getValue ()
{
	return this.value;
}

/**
Set the declarationType.
*/
public void setDeclarationType ( String declarationType )
{
	this.declarationType = declarationType;
}

/**
Set the name.
*/
public void setName ( String name )
{
	this.name = name;
}

}
