package rti.tscommandprocessor.commands.rccacis;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;

import RTi.Util.String.StringUtil;

/**
This class is an input filter for querying RCC ACIS web services.
*/
public class RccAcis_TimeSeries_InputFilter_JPanel extends InputFilter_JPanel //implements ItemListener, KeyListener
{
    
/**
RCC ACIS data store.
*/
private RccAcisDataStore __dataStore = null;

/**
Constructor.
@param dataStore the data store to use to connect to the web services.  Cannot be null.
@param numFilterGroups the number of filter groups to display
*/
public RccAcis_TimeSeries_InputFilter_JPanel( RccAcisDataStore dataStore, int numFilterGroups )
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

    filters.add(new InputFilter("", "", StringUtil.TYPE_STRING, null, null, false)); // Blank

    // Get lists for choices...
    //List<String> geolocCountyList = dmi.getGeolocCountyList();
    //List<String> geolocStateList = dmi.getGeolocStateList();
    
    String [] postalArray = {
        "AL - Alabama",
        "AK - Alaska",
        "AZ - Arizona",
        "AR - Arkansas",
        "CA - California",
        "CO - Colorado",
        "CT - Connecticut",
        "DE - Delaware",
        "DC - District of Columbia",
        "FL - Florida",
        "GA - Georgia",
        "HI - Hawaii",
        "ID - Idaho",
        "IL - Illinois",
        "IN - Indiana",
        "IA - Iowa",
        "KS - Kansas",
        "KY - Kentucky",
        "LA - Louisiana",
        "ME - Maine",
        "MT - Montana",
        "NE - Nebraska",
        "NV - Nevada",
        "NH - New Hampshire",
        "NJ - New Jersey",
        "NM - New Mexico",
        "NY - New York",
        "NC - North Carolina",
        "ND - North Dakota",
        "OH - Ohio",
        "OK - Oklahoma",
        "OR - Oregon",
        "MD - Maryland",
        "MA - Massachusetts",
        "MI - Michigan",
        "MN - Minnesota",
        "MS - Mississippi",
        "MO - Missouri",
        "PA - Pennsylvania",
        "RI - Rhode Island",
        "SC - South Carolina",
        "SD - South Dakota",
        "TN - Tennessee",
        "TX - Texas",
        "UT - Utah",
        "VT - Vermont",
        "VA - Virginia",
        "WA - Washington",
        "WV - West Virginia",
        "WI - Wisconsin",
        "WY - Wyoming"
    };

    filters.add(new InputFilter("Bounding Box",
        "bbox", "bbox",
        StringUtil.TYPE_STRING, null, null, true, "Bounding box in decimal degrees west,south,east,north " +
            " (e.g., -90,40,-88,41)"));
    
    filters.add(new InputFilter("Climate Division",
        "clim_div", "clim_div",
        StringUtil.TYPE_STRING, null, null, true,
        "Specify 2 digits (e.g., 01, 10) if postal is specified or combine here (e.g., NY01, NY10)" +
        " (see: http://www.esrl.noaa.gov/psd/data/usclimate/map.html)"));
    
    filters.add(new InputFilter("Drainage Basin (HUC)",
        "basin", "basin",
        StringUtil.TYPE_STRING, null, null, true,
        "For example 01080205 (see: http://water.usgs.gov/GIS/huc.html)"));
    
    filters.add(new InputFilter("FIPS County",
        "county", "county",
        StringUtil.TYPE_STRING, null, null, true, "Federal Information Processing Standard " +
        "county (e.g., 09001) (see: http://www.epa.gov/enviro/html/codes/state.html)"));
    
    filters.add(new InputFilter("NWS County Warning Area",
        "cwa", "cwa",
        StringUtil.TYPE_STRING, null, null, true,
        "For example BOI (see: http://www.aprs-is.net/WX/NWSZones.aspx)"));
    
    filters.add(new InputFilter("Postal (State) Code",
        "postal", "postal",
        StringUtil.TYPE_STRING, Arrays.asList(postalArray), Arrays.asList(postalArray),
        true, // Allow edits because more than one state can be specified
        "State abbreviation") );
    
    setToolTipText("RCC ACIS queries can be filtered based on location and time series metadata");
    setInputFilters(filters, numFilterGroups, 25);
}

/**
Return the data store corresponding to this input filter panel.
@return the data store corresponding to this input filter panel.
*/
public RccAcisDataStore getDataStore ( )
{
    return __dataStore;
}

}