package rti.tscommandprocessor.commands.reclamationpisces;

import java.util.ArrayList;
import java.util.List;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.String.StringUtil;

/**
This class is an input filter for querying ReclamationPisces.
*/
public class ReclamationPisces_TimeSeries_InputFilter_JPanel extends InputFilter_JPanel //implements ItemListener, KeyListener
{

/**
ReclamationPisces database connection.
*/
private ReclamationPiscesDataStore datastore = null;

/**
Constructor.
@param dataStore the data store to use to connect to the Reclamation Pisces database.  Cannot be null.
@param numFilterGroups the number of filter groups to display
*/
public ReclamationPisces_TimeSeries_InputFilter_JPanel( ReclamationPiscesDataStore dataStore, int numFilterGroups )
{   super();
    this.datastore = dataStore;
    if ( this.datastore != null ) {
        ReclamationPiscesDMI dmi = (ReclamationPiscesDMI)dataStore.getDMI();
        setFilters ( dmi, numFilterGroups );
    }
}

/**
Set the filter data.  This method is called at setup and when refreshing the list with a new subject type.
*/
public void setFilters ( ReclamationPiscesDMI dmi, int numFilterGroups )
{   //String routine = getClass().getName() + ".setFilters";
    //String rd = dmi.getRightIdDelim();
    //String ld = dmi.getLeftIdDelim();

    List<InputFilter> filters = new ArrayList<InputFilter>();

    // The database may have timed out so check here
    this.datastore.checkDatabaseConnection();

    // Always add blank to top of filter
    filters.add(new InputFilter("", "", StringUtil.TYPE_STRING, null, null, false)); // Blank

    // Get lists for choices...
    List<String> parameterList = this.datastore.getParameterList();
    parameterList.add(0,"*"); // Query all parameters when don't know what is available for interval, etc
    
    //filters.add(new InputFilter("Series - ",
    //        "HDB_SITE.SITE_COMMON_NAME", "HDB_SITE.SITE_COMMON_NAME",
    //        StringUtil.TYPE_STRING, null, null, true));

    /*
    filters.add(new InputFilter("Object - Type ID",
        "HDB_OBJECTTYPE.OBJECTTYPE_ID", "",
        StringUtil.TYPE_INTEGER, null, null, true));
    
    try {
        List<ReclamationHDB_ObjectType>objectTypeList = dmi.getObjectTypeList();
        List<String> objectTypeNameList = new Vector<String>();
        for ( ReclamationHDB_ObjectType ot : objectTypeList ) {
            objectTypeNameList.add ( ot.getObjectTypeName() );
        }
        objectTypeNameList = StringUtil.sortStringList(objectTypeNameList, StringUtil.SORT_ASCENDING, null, false, true);
        StringUtil.removeDuplicates(objectTypeNameList, true, true);
        filters.add(new InputFilter("Object - Type Name",
            "HDB_OBJECTTYPE.OBJECTTYPE_NAME", "",
            StringUtil.TYPE_STRING, objectTypeNameList, objectTypeNameList, true));
    }
    catch ( Exception e ) {
        // Use text fields.
        filters.add(new InputFilter("Object - Type Name",
            "HDB_OBJECTTYPE.OBJECTTYPE_NAME", "",
            StringUtil.TYPE_STRING, null, null, true));
    }
    
    filters.add(new InputFilter("Object - Type Tag",
            "HDB_OBJECTTYPE.OBJECTTYPE_TAG", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    try {
        List<String> dataTypeCommonNameList = new Vector<String>();
        for ( ReclamationHDB_DataType dt : dataTypeList ) {
            dataTypeCommonNameList.add ( dt.getDataTypeCommonName() );
        }
        dataTypeCommonNameList = StringUtil.sortStringList(dataTypeCommonNameList, StringUtil.SORT_ASCENDING, null, false, true);
        StringUtil.removeDuplicates(dataTypeCommonNameList, true, true);
        filters.add(new InputFilter("Data Type - Common Name",
            "HDB_DATATYPE.DATATYPE_COMMON_NAME", "",
            StringUtil.TYPE_STRING, dataTypeCommonNameList, dataTypeCommonNameList, true));
    }
    catch ( Exception e ) {
        filters.add(new InputFilter("Data Type - Common Name",
            "HDB_DATATYPE.COMMON_NAME", "",
            StringUtil.TYPE_STRING, null, null, true));
    }
    */
    
    /* TODO SAM 2014-04-07 Figure out why list of integers does not work.
    try {
        List<String> dataTypeIdList = new Vector<String>();
        for ( ReclamationHDB_DataType dt : dataTypeList ) {
            dataTypeIdList.add ( "" + dt.getDataTypeID() );
        }
        dataTypeIdList = StringUtil.sortStringList(dataTypeIdList, StringUtil.SORT_ASCENDING, null, false, true);
        StringUtil.removeDuplicates(dataTypeIdList, true, true);
        // Now convert to integers
        List<Integer> iDataTypeIdList = new Vector<Integer>();
        for ( String s : dataTypeIdList ) {
            iDataTypeIdList.add ( new Integer(s));
        }
        filters.add(new InputFilter("Site - Data Type ID",
            "HDB_SITE_DATATYPE.SITE_DATATYPE_ID", "",
            StringUtil.TYPE_INTEGER, dataTypeIdList, dataTypeIdList, false));
    }
    catch ( Exception e ) { */
    /*
        filters.add(new InputFilter("Data Type - ID",
            "HDB_DATATYPE.DATATYPE_ID", "",
            StringUtil.TYPE_INTEGER, null, null, true));
    //}
    

    
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
        */
    
    /* FIXME SAM 2010-10-29 Disable for now because in the database these are strings - difficult to filter
    filters.add(new InputFilter("Site - Latitude",
        "HDB_SITE.LAT", "",
        StringUtil.TYPE_DOUBLE, null, null, true));
    
    filters.add(new InputFilter("Site - Longitude",
        "HDB_SITE.LONGI", "",
        StringUtil.TYPE_DOUBLE, null, null, true));
        */
    /*
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
    
    filters.add(new InputFilter("Model - ID",
        "HDB_MODEL.MODEL_ID", "",
        StringUtil.TYPE_INTEGER, null, null, true));
    
    filters.add(new InputFilter("Model - Name",
        "HDB_MODEL.MODEL_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Model Run - ID",
        "REF_MODEL_RUN.MODEL_RUN_ID", "",
        StringUtil.TYPE_INTEGER, null, null, true));
    
    filters.add(new InputFilter("Model Run - Name",
        "REF_MODEL_RUN.MODEL_RUN_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Model Run - Hydrologic Indicator",
        "REF_MODEL_RUN.HYDROLOGIC_INDICATOR", "",
        StringUtil.TYPE_STRING, null, null, true));
        */
    
    // General data
    /*
    filters.add(new InputFilter("Data - Physical Quantity Name",
        "HDB_DATATYPE.PHYSICAL_QUANTITY_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Data - Units",
        "HDB_UNIT.UNIT_COMMON_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
        */
    
    setToolTipText("<html>Reclamation Pisces queries can be filtered based on site and time series metadata.</html>");
    setInputFilters(filters, numFilterGroups, 32);
}

/**
Return the data store corresponding to this input filter panel.
@return the data store corresponding to this input filter panel.
*/
public ReclamationPiscesDataStore getDataStore ( )
{
    return this.datastore;
}

}