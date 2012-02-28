package rti.tscommandprocessor.commands.usgs.nwis.daily;

import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;

import RTi.Util.String.StringUtil;

/**
This class is an input filter for querying USGS NWIS web services.
*/
public class UsgsNwisDaily_TimeSeries_InputFilter_JPanel extends InputFilter_JPanel //implements ItemListener, KeyListener
{
    
/**
USGS NWIS data store.
*/
private UsgsNwisDailyDataStore __dataStore = null;

/**
Constructor.
@param dataStore the data store to use to connect to the web services.  Cannot be null.
@param numFilterGroups the number of filter groups to display
*/
public UsgsNwisDaily_TimeSeries_InputFilter_JPanel( UsgsNwisDailyDataStore dataStore, int numFilterGroups )
{   super();
    __dataStore = dataStore;
    setFilters ( numFilterGroups );
}

/**
Set the filter data.  This method is called at setup.
@param numFilterGroups the number of filter groups to display
*/
public void setFilters ( int numFilterGroups )
{   //String routine = getClass().getName() + ".setFilters";

    List<InputFilter> filters = new Vector();
    //List<String> parameterChoices = new Vector();
    List<String> statisticChoices = __dataStore.getStatisticStrings(true);
    statisticChoices.add("");
    List<String> agencyChoices = __dataStore.getAgencyStrings(true);
    agencyChoices.add("");

    filters.add(new InputFilter("", "", StringUtil.TYPE_STRING, null, null, false)); // Blank

    filters.add(new InputFilter("Site number",
        "SiteNum", "SiteNum",
        StringUtil.TYPE_STRING, null, null, true, "Site number (e.g., 01646500)"));
    
    // Parameter is in the data type so do not add in the input filter
    
    filters.add(new InputFilter("Statistic",
        "StatisticCode", "StatisticCode",
        StringUtil.TYPE_STRING, statisticChoices, statisticChoices, true,
        "Statistic code (see: http://waterservices.usgs.gov/rest/USGS-DV-Service.html)"));
    
    filters.add(new InputFilter("Agency",
        "AgencyCode", "AgencyCode",
        StringUtil.TYPE_STRING, agencyChoices, agencyChoices, true,
        "Agency code (see: http://nwis.waterdata.usgs.gov/nwis/help/?read_file=nwis_agency_codes&format=table)"));
    
    setToolTipText("USGS NWIS queries can be filtered based on location and time series metadata");
    setInputFilters(filters, numFilterGroups, 25);
}

/**
Return the data store corresponding to this input filter panel.
@return the data store corresponding to this input filter panel.
*/
public UsgsNwisDailyDataStore getDataStore ( )
{
    return __dataStore;
}

}