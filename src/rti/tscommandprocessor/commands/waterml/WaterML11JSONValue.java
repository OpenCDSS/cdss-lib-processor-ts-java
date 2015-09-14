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