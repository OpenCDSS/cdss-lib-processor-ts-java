// RccAcisVariableTableRecord - Variable table records taken from:  http://data.rcc-acis.org/doc/VariableTable.html

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

package rti.tscommandprocessor.commands.rccacis;

/**
Variable table records taken from:  http://data.rcc-acis.org/doc/VariableTable.html
These can be used for provide choices when putting together REST calls
*/
public class RccAcisVariableTableRecord
{
    
/**
Element abbreviation corresponding to the major variable.
*/
private String __elem = "";
    
/**
Major variable type.
*/
private int __major = -1;

/**
Minor variable type.
*/
private int __minor = -1;

/**
Variable name.
*/
private String __name = "";

/**
Reduction method.
*/
private String __method = "";

/**
Data interval.
*/
private String __measureInterval = "";

/**
Data interval.
*/
private String __reportInterval = "";

/**
Data units.
*/
private String __units = "";

/**
Source.
*/
private String __source = "";
    
/**
Constructor.
*/
public RccAcisVariableTableRecord ( String elem, int major, int minor, String name, String method,
    String measureInterval, String reportInterval, String units, String source )
{
    __elem = elem;
    __major = major;
    __minor = minor;
    __name = name;
    __method = method;
    __measureInterval = measureInterval;
    __reportInterval = reportInterval;
    __units = units;
    __source = source;
}

/**
Return the name.
*/
public String getElem()
{
    return __elem;
}

/**
Return the major.
*/
public int getMajor()
{
    return __major;
}

/**
Return the minor.
*/
public int getMinor()
{
    return __minor;
}

/**
Return the name.
*/
public String getName()
{
    return __name;
}

/**
Return the measure interval.
*/
public String getMeasureInterval()
{
    return __measureInterval;
}

/**
Return the method.
*/
public String getMethod()
{
    return __method;
}

/**
Return the report interval.
*/
public String getReportInterval()
{
    return __reportInterval;
}

/**
Return the source.
*/
public String getSource()
{
    return __source;
}

/**
Return the units.
*/
public String getUnits()
{
    return __units;
}

}
