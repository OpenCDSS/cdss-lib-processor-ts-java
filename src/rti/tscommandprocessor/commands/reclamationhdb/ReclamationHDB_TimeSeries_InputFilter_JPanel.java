package rti.tscommandprocessor.commands.reclamationhdb;

import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;

import RTi.Util.String.StringUtil;

/**
This class is an input filter for querying ReclamationHDB.
Checks for nulls are done in many places because developing with HDB can be difficult due to limited VPN access, etc.
Consequently, sometimes it is necessary to develop off-line.
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
    if ( __dataStore != null ) {
        ReclamationHDB_DMI dmi = (ReclamationHDB_DMI)dataStore.getDMI();
        setFilters ( dmi, numFilterGroups );
    }
}

/**
Set the filter data.  This method is called at setup and when refreshing the list with a new subject type.
*/
public void setFilters ( ReclamationHDB_DMI dmi, int numFilterGroups )
{   //String routine = getClass().getName() + ".setFilters";
    //String rd = dmi.getRightIdDelim();
    //String ld = dmi.getLeftIdDelim();

    List<InputFilter> filters = new Vector();

    // The database may have timed out so check here
    __dataStore.checkDatabaseConnection();
    List<ReclamationHDB_DataType>dataTypeList = dmi.getDataTypeList();

    //String dataTableName = "";//dmi.getSchemaPrefix() + "v" + subjectType + "DataMetaData." + ld;

    filters.add(new InputFilter("", "", StringUtil.TYPE_STRING, null, null, false)); // Blank

    // Get lists for choices...
    //List<String> geolocCountyList = dmi.getGeolocCountyList();
    //List<String> geolocStateList = dmi.getGeolocStateList();
    List<String> realModelList = new Vector<String>(4);
    realModelList.add("Model");
    realModelList.add("Model and Real");
    realModelList.add("Real");
    realModelList.add("EnsembleTrace");

    // If these words are changed also change ReclamationHDB_DMI.readSiteTimeSeriesMetadataList
    filters.add(new InputFilter("Real, Model, Ensemble Data",
        "", "",
        StringUtil.TYPE_STRING, realModelList, realModelList, false));
    
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
        filters.add(new InputFilter("Data Type - ID",
            "HDB_DATATYPE.DATATYPE_ID", "",
            StringUtil.TYPE_INTEGER, null, null, true));
    //}
    
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
    
    // Ensemble data
    
    // Agency list is for ensembles, not the time series.
    // Development database may not have any data and if this is the case, populate with the main agency list
    // Show the agenID and abbreviation because the agenID is in the REF_ENSEMBLE table and needs to be in the query
    try {
        List<Integer>ensembleAgenIDList = dmi.readRefEnsembleAgenIDList();
        List<String>ensembleAgenIDStringList = new Vector<String>();
        // If there are no non-null agencies listed in the ensembles, show the HDB agencies for testing
        List<String> agencyList = new Vector<String>();
        if ( ensembleAgenIDList.size() == 0 ) {
            List<ReclamationHDB_Agency>hdbAgencyList = dmi.getAgencyList();
            String abbrev, s;
            for ( ReclamationHDB_Agency a : hdbAgencyList ) {
                s = "" + a.getAgenID() + " - " + a.getAgenName();
                abbrev = a.getAgenAbbrev();
                if ( (abbrev != null) && !abbrev.equals("") ) {
                    s = s + " (" + abbrev + ")";
                }
                ensembleAgenIDStringList.add("" + a.getAgenID());
                agencyList.add ( s );
            }
        }
        else {
            String abbrev, s;
            ReclamationHDB_Agency a;
            for ( Integer id : ensembleAgenIDList ) {
                a = dmi.lookupAgency(dmi.getAgencyList(), id );
                if ( a == null ) {
                    s = "" + id;
                }
                else {
                    s = "" + a.getAgenID() + " - " + a.getAgenName();
                    abbrev = a.getAgenAbbrev();
                    if ( (abbrev != null) && !abbrev.equals("") ) {
                        s = s + " (" + abbrev + ")";
                    }
                }
                agencyList.add ( s );
                ensembleAgenIDStringList.add("" + a.getAgenID());
            }
        }
        InputFilter inputFilter = new InputFilter("Ensemble - Agency",
                "REF_ENSEMBLE.AGEN_ID", "REF_ENSEMBLE.AGEN_ID",
                StringUtil.TYPE_INTEGER, agencyList, ensembleAgenIDStringList, true);
        inputFilter.setTokenInfo(" ", 0);
        filters.add(inputFilter);
    }
    catch ( Exception e ) {
        // Use text fields.
        filters.add(new InputFilter("Ensemble - Agency",
            "REF_ENSEMBLE.AGEN_ID", "",
            StringUtil.TYPE_INTEGER, null, null, true));
    }
    
    filters.add(new InputFilter("Ensemble - Name",
            "REF_ENSEMBLE.ENSEMBLE_NAME", "",
            StringUtil.TYPE_STRING, null, null, true));
    
    try {
        List<String> traceDomainList = dmi.readRefEnsembleTraceDomainList();
        filters.add(new InputFilter("Ensemble - Trace Domain",
            "REF_ENSEMBLE.TRACE_DOMAIN", "REF_ENSEMBLE.TRACE_DOMAIN",
            StringUtil.TYPE_STRING, traceDomainList, traceDomainList, true));
    }
    catch ( Exception e ) {
        // Use text fields.
        filters.add(new InputFilter("Ensemble - Trace Domain",
            "REF_ENSEMBLE.TRACE_DOMAIN", "",
            StringUtil.TYPE_STRING, null, null, true));
    }
    
    // General data
    
    filters.add(new InputFilter("Data - Physical Quantity Name",
        "HDB_DATATYPE.PHYSICAL_QUANTITY_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    filters.add(new InputFilter("Data - Units",
        "HDB_UNIT.UNIT_COMMON_NAME", "",
        StringUtil.TYPE_STRING, null, null, true));
    
    setToolTipText("<html>Reclamation HDB queries can be filtered based on site and time series metadata.</html>");
    setInputFilters(filters, numFilterGroups, 32);
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