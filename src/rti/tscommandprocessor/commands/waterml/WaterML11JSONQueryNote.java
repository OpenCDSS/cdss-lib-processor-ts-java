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