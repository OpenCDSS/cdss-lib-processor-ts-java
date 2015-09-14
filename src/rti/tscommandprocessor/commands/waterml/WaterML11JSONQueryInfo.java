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