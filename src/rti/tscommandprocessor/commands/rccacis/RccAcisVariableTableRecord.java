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