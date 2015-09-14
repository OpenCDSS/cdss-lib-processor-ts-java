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