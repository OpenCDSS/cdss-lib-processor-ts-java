// UsgsNwisDaily_TimeSeries_InputFilter_JPanel - This class is an input filter for querying USGS NWIS web services.

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

package rti.tscommandprocessor.commands.usgs.nwis.daily;

import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;

import RTi.Util.String.StringUtil;

/**
This class is an input filter for querying USGS NWIS web services.
*/
@SuppressWarnings("serial")
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

    List<InputFilter> filters = new Vector<InputFilter>();
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
