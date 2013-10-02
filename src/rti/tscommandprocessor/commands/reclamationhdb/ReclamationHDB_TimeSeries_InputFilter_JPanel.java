package rti.tscommandprocessor.commands.reclamationhdb;

import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;

import RTi.Util.String.StringUtil;

/**
This class is an input filter for querying ReclamationHDB.
*/
public class ReclamationHDB_TimeSeries_InputFilter_JPanel extends InputFilter_JPanel //implements ItemListener, KeyListener
{
    
/**
ReclamationHDB database connection.
*/
private ReclamationHDBDataStore __dataStore = null;

/**
Constructor.
@param dataStore the data store to use to connect to the Reclamation HDB database.  Cannot be null.
@param numFilterGroups the number of filter groups to display
*/
public ReclamationHDB_TimeSeries_InputFilter_JPanel( ReclamationHDBDataStore dataStore, int numFilterGroups )
{   super();
    __dataStore = dataStore;
    ReclamationHDB_DMI dmi = (ReclamationHDB_DMI)dataStore.getDMI();
    setFilters ( dmi, numFilterGroups );
}

/**
Set the filter data.  This method is called at setup and when refreshing the list with a new subject type.
*/
public void setFilters ( ReclamationHDB_DMI dmi, int numFilterGroups )
{   //String routine = getClass().getName() + ".setFilters";
    //String rd = dmi.getRightIdDelim();
    //String ld = dmi.getLeftIdDelim();

    List<InputFilter> filters = new Vector();

    //String dataTableName = "";//dmi.getSchemaPrefix() + "v" + subjectType + "DataMetaData." + ld;

    filters.add(new InputFilter("", "", StringUtil.TYPE_STRING, null, null, false)); // Blank

    // Get lists for choices...
    //List<String> geolocCountyList = dmi.getGeolocCountyList();
    //List<String> geolocStateList = dmi.getGeolocStateList();
    List<String> realModelList = new Vector();
    realModelList.add("Model");
    realModelList.add("Model and Real");
    realModelList.add("Real");
    
    filters.add(new InputFilter("Real or Model Data",
            "", "",
            StringUtil.TYPE_STRING, realModelList, realModelList, false));
    
    filters.add(new InputFilter("Object - Type ID",
        "HDB_OBJECTTYPE.OBJECTTYPE_ID", "",
        StringUtil.TYPE_INTEGER, null, null, true));
    
    filters.add(new InputFilter("Object - Type Name",
        "HDB_OBJECTTYPE.OBJECTTYPE_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Object - Type Tag",
        "HDB_OBJECTTYPE.OBJECTTYPE_TAG", "",
        StringUtil.TYPE_STRING, null, null, true));

    filters.add(new InputFilter("Site - Common Name",
        "HDB_SITE.SITE_COMMON_NAME", "HDB_SITE.SITE_COMMON_NAME",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - DB Site Code",
        "HDB_SITE.DB_SITE_CODE", "",
        StringUtil.TYPE_STRING, null, null, true));
        
    filters.add(new InputFilter("Site - Description",
        "HDB_SITE.DESCRIPTION", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - Elevation",
        "HDB_SITE.ELEVATION", "",
        StringUtil.TYPE_FLOAT, null, null, true));
    
    filters.add(new InputFilter("Site - HUC",
        "HDB_SITE.HYDROLOGIC_UNIT", "",
        StringUtil.TYPE_STRING, null, null, true));
        
    filters.add(new InputFilter("Site - ID",
        "HDB_SITE.SITE_ID", "",
        StringUtil.TYPE_INTEGER, null, null, true));
    
    filters.add(new InputFilter("Site - Data Type ID",
        "HDB_SITE_DATATYPE.SITE_DATATYPE_ID", "",
        StringUtil.TYPE_INTEGER, null, null, true));
    
    /* FIXME SAM 2010-10-29 Disable for now because in the database these are strings - difficult to filter
    filters.add(new InputFilter("Site - Latitude",
        "HDB_SITE.LAT", "",
        StringUtil.TYPE_DOUBLE, null, null, true));
    
    filters.add(new InputFilter("Site - Longitude",
        "HDB_SITE.LONGI", "",
        StringUtil.TYPE_DOUBLE, null, null, true));
        */
    
    filters.add(new InputFilter("Site - Name",
        "HDB_SITE.SITE_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - NWS Code",
        "HDB_SITE.NWS_CODE", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - River Mile",
        "HDB_SITE.RIVER_MILE", "",
        StringUtil.TYPE_FLOAT, null, null, true));
    
    filters.add(new InputFilter("Site - SCS ID",
        "HDB_SITE.SCS_ID", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - Segment Number",
        "HDB_SITE.SEGMENT_NO", "",
        StringUtil.TYPE_INTEGER, null, null, true));
    
    filters.add(new InputFilter("Site - SHEF Code",
        "HDB_SITE.SHEF_CODE", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - State",
        "HDB_STATE.STATE_CODE", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Site - USGS ID",
        "HDB_SITE.USGS_ID", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    // Datatype Common Name is in the main TSTool query choice so don't put data type name here
    
    filters.add(new InputFilter("Model - ID",
        "HDB_MODEL.MODEL_ID", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Model - Name",
        "HDB_MODEL.MODEL_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Model Run - ID",
            "REF_MODEL_RUN.MODEL_RUN_ID", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Model Run - Name",
        "REF_MODEL_RUN.MODEL_RUN_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Model Run - Hydrologic Indicator",
        "REF_MODEL_RUN.HYDROLOGIC_INDICATOR", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Data - Physical Quantity Name",
        "HDB_DATATYPE.PHYSICAL_QUANTITY_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Data - Units",
        "HDB_UNIT.UNIT_COMMON_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    setToolTipText("<html>Reclamation HDB queries can be filtered based on site and time series metadata.</html>");
    setInputFilters(filters, numFilterGroups, 25);
}

/**
Return the data store corresponding to this input filter panel.
@return the data store corresponding to this input filter panel.
*/
public ReclamationHDBDataStore getDataStore ( )
{
    return __dataStore;
}

}