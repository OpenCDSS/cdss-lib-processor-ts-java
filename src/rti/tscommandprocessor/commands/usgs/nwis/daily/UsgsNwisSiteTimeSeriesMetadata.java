package rti.tscommandprocessor.commands.usgs.nwis.daily;

/**
Metadata for site and time series joined data.  The USGS NWIS REST API here:
http://waterservices.usgs.gov/rest/USGS-DV-Service.html
does not have a site query but anticipate that a CUAHSI catalog will be published so put something
temporary in place.
*/
public class UsgsNwisSiteTimeSeriesMetadata
{
    
/**
Default constructor, required by GSON.
*/
public UsgsNwisSiteTimeSeriesMetadata ()
{
}

/**
Data store associated with this record.
*/
private UsgsNwisDailyDataStore __dataStore = null;

/**
Parameter information.
*/
private UsgsNwisParameterType __parameter = null;

/**
Statistic information.
*/
private UsgsNwisStatisticType __statistic = null;

/**
Agency code.
*/
private String __agencyCode = "";

/**
End of data (YYYY-MM-DD).
*/
private String __end = "";

/**
Data interval (e.g., "1Day").
*/
private String __interval = "";

/**
Latitude.
*/
private double __latitude = Double.NaN;

/**
Longitude (decimal degrees).
*/
private double __longitude = Double.NaN;

/**
Location name.
*/
private String __name = "";

/**
Site number (treat as string to ensure zero padding is correct).
*/
private String __siteNum = "";

/**
Start of data (YYYY-MM-DD).
*/
private String __start = "";

/**
Data units.
*/
private String __units = "";

/**
Format the data type for a time series identifier (parameter or parameter-statistic), depending on
whether the statistic is shown.
*/
public String formatDataTypeForTSID ()
{   StringBuffer dataType = new StringBuffer();
    UsgsNwisParameterType parameter = getParameter();
    if ( parameter == null ) {
        return dataType.toString();
    }
    else {
        dataType.append(parameter.getCode());
    }
    UsgsNwisStatisticType statistic = getStatistic();
    if ( (statistic == null) || statistic.getCode().equals("") ) {
        return dataType.toString();
    }
    else {
        dataType.append("-" + statistic.getName() );
        return dataType.toString();
    }
}

/**
Return the agency code.
*/
public String getAgencyCode()
{
    return __agencyCode;
}

/**
Return the ending date string.
*/
public String getEnd()
{
    return __end;
}

/**
Return the data interval (e.g., "1Day").
*/
public String getInterval()
{
    return __interval;
}

/**
Return the latitude.
*/
public double getLatitude ()
{
    return __latitude;
}

/**
Return the longitude (decimal degrees).
*/
public double getLongitude()
{
    return __longitude;
}

/**
Return the name.
*/
public String getName()
{
    return __name;
}

/**
Return the parameter information for the data.
*/
public UsgsNwisParameterType getParameter()
{
    return __parameter;
}

/**
Return the site number.
*/
public String getSiteNum()
{
    return __siteNum;
}

/**
Return the starting date string.
*/
public String getStart()
{
    return __start;
}

/**
Return the statistic information for the data.
*/
public UsgsNwisStatisticType getStatistic()
{
    return __statistic;
}

/**
Return the data units.
*/
public String getUnits()
{
    return __units;
}

/**
Set the data store.
*/
public void setDataStore ( UsgsNwisDailyDataStore dataStore )
{
    __dataStore = dataStore;
}

/**
Set the agency code.
*/
public void setAgencyCode ( String agencyCode )
{
    __agencyCode = agencyCode;
}

/**
Set the data interval (e.g., "1Day").
*/
public void setInterval ( String interval )
{
    __interval = interval;
}

/**
Set the name.
*/
public void setName ( String name )
{
    __name = name;
}

/**
Set the parameter type for the data.
*/
public void setParameter ( UsgsNwisParameterType parameter )
{
    __parameter = parameter;
}

/**
Set the site number.
*/
public void setSiteNum ( String siteNum )
{
    __siteNum = siteNum;
}

/**
Set the statistic type for the data.
*/
public void setStatistic ( UsgsNwisStatisticType statistic )
{
    __statistic = statistic;
}

/**
Set the data units.
*/
public void setUnits ( String units )
{
    __units = units;
}

}