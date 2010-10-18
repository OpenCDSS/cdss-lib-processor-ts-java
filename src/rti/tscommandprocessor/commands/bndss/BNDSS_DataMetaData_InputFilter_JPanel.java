package rti.tscommandprocessor.commands.bndss;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.SimpleJComboBox;

import RTi.Util.Message.Message;

import RTi.Util.String.StringUtil;

/**
This class is an input filter for querying ColoradoBNDSS joined records from Count or Provider or Project.
*/
public class BNDSS_DataMetaData_InputFilter_JPanel extends InputFilter_JPanel implements ItemListener, KeyListener
{
    
/**
ColoradoBNDSS database connection.
*/
private ColoradoBNDSSDataStore __dataStore = null;

/**
Indicate whether ItemStateChanged events should be skipped.  This is used to temporarily limit events
during component initialization.
*/
//private boolean __skipItemEvents = false;

/**
Constructor.
@param dataStore the data store to use to connect to the BNDSS database.  Cannot be null.
@param subjectType the initial subject type to display, or null to default to county.
@param numFilterGroups the number of filter groups to display
*/
public BNDSS_DataMetaData_InputFilter_JPanel( ColoradoBNDSSDataStore dataStore, BNDSSSubjectType subjectType, int numFilterGroups )
{   super();
    __dataStore = dataStore;
    BNDSS_DMI dmi = (BNDSS_DMI)dataStore.getDMI();
    //__skipItemEvents = true;
    //try {
        setFilters ( dmi, subjectType, numFilterGroups );
    //}
    //finally {
    //    __skipItemEvents = false;
    //}
}

/**
Handle item events for item events.  If the Subject choice changes, then refresh the entire contents.
*/
public void itemStateChanged ( ItemEvent event )
{   String routine = getClass().getName() + ".itemStateChanged";
    //if ( __skipItemEvents ) {
    //    return;
    //}
    SimpleJComboBox c = (SimpleJComboBox)event.getSource();
    String selectedChoice = c.getSelected();
    Message.printStatus(2, routine, "Item state changed to choice \"" + selectedChoice + "\"" );
    // If one of the subject types, then assume that a subject was selected.  Since
    // a subject is always defaulted at initialization, selection will indicate a change
    // in value and a need to refresh the list
    BNDSSSubjectType subjectType = null;
    if ( selectedChoice != null ) {
         subjectType = BNDSSSubjectType.valueOfIgnoreCase(selectedChoice);
    }
    if ( (event.getStateChange() == ItemEvent.SELECTED) && (subjectType != null) ) {
        // Different subject was selected so refresh the choices
        //__skipItemEvents = true;
        Message.printStatus(2, routine, "Resetting filters because subject changed to " + subjectType );
        // First remove all the existing listeners
        //removeEventListeners(this);
        //try {
            setFilters ( (BNDSS_DMI)__dataStore.getDMI(), subjectType, getNumFilterGroups() );
        //}
        //finally {
        //    __skipItemEvents = false;
        //}
    }
    else {
        // Pass to parent as normal event
        super.itemStateChanged(event);
    }
}

/**
Handle event for key pressed events (don't do anything - no method in parent either).
*/
public void keyPressed ( KeyEvent e ) {
}

/**
Handle event for key released events (don't do anything - no method in parent either).
*/
public void keyReleased ( KeyEvent e ) {
}

/**
Handle event for key typed events (don't do anything - no method in parent either).
*/
public void keyTyped ( KeyEvent e ) {
}

/**
Set the filter data.  This method is called at setup and when refreshing the list with a new subject type.
@param subjectType the initial subject type to display, or null to default to county.
*/
public void setFilters ( BNDSS_DMI dmi, BNDSSSubjectType subjectType, int numFilterGroups )
{   String routine = getClass().getName() + ".setFilters";
    if ( subjectType == null ) {
        subjectType = BNDSSSubjectType.COUNTY; // Default
    }
    String rd = dmi.getRightIdDelim();
    String ld = dmi.getLeftIdDelim();

    List<InputFilter> filters = new Vector();

    String dataTableName = dmi.getSchemaPrefix() + "v" + subjectType + "DataMetaData." + ld;

    filters.add(new InputFilter("", "", StringUtil.TYPE_STRING, null, null, false)); // Blank

    // Get lists for choices...
    //List<String> geolocCountyList = dmi.getGeolocCountyList();
    //List<String> geolocStateList = dmi.getGeolocStateList();
    List<String> subjectList = dmi.getSubjectList ();
    List<String> subTypeList = dmi.getDataMetaDataSubTypeList ( subjectType );
    List<String> dataTypeList = dmi.getDataMetaDataDataTypeList ( subjectType );
    List<String> methodList = dmi.getDataMetaDataMethodList ( subjectType );
    List<String> sourceList = dmi.getDataMetaDataSourceList( subjectType );
    List<String> scenarioList = dmi.getDataMetaDataScenarioList ( subjectType );
    List<String> subjectIDList = dmi.getDataMetaDataSubjectIDList ( subjectType );
    
    filters.add(new InputFilter("Data Source",
        dataTableName + "source" + rd, "source",
        StringUtil.TYPE_STRING, sourceList, sourceList, false));
    
    filters.add(new InputFilter("Data Type (main)",
        dataTableName + "dataType" + rd, "dataType",
        StringUtil.TYPE_STRING, dataTypeList, dataTypeList, false));
    
    filters.add(new InputFilter("Data Type (subtype)",
        dataTableName + "subType" + rd, "subType",
        StringUtil.TYPE_STRING, subTypeList, subTypeList, false));
    
    filters.add(new InputFilter("Identifier",
        dataTableName + "subjectID" + rd, "subjectID",
        StringUtil.TYPE_STRING, subjectIDList, subjectIDList, true)); // Allow user to specify substring
    
    filters.add(new InputFilter("Method (main)",
        dataTableName + "method" + rd, "method",
        StringUtil.TYPE_STRING, methodList, methodList, false));
    
    filters.add(new InputFilter("Name",
        dataTableName + "name" + rd, "name",
        StringUtil.TYPE_STRING, null, null, false));
    
    filters.add(new InputFilter("Scenario",
        dataTableName + "scenario" + rd, "scenario",
        StringUtil.TYPE_STRING, scenarioList, scenarioList, false));
    
    // Subject type is important because time series for different subject types live in different tables
    filters.add(new InputFilter("Subject",
        null, null, // This field is not used directly for queries
        StringUtil.TYPE_STRING, subjectList, subjectList, false));

    /*

    filters.add(new InputFilter("Data Units",
        measTypeTableName + "units_abbrev" + rd, "units_abbrev",
        StringUtil.TYPE_STRING, measTypeUnitsList, measTypeUnitsList, false));

    filters.add(new InputFilter("Station County",
       geolocTableName + "county" + rd, "county", StringUtil.TYPE_STRING,
       geolocCountyList, geolocCountyList, false));
    filters.add(new InputFilter("Station Elevation",
        geolocTableName + "elevation" + rd, "elevation",
        StringUtil.TYPE_DOUBLE, null, null, false));
    filters.add(new InputFilter("Station Latitude",
        geolocTableName + "latitude" + rd, "latitude",
        StringUtil.TYPE_DOUBLE, null, null, false));
    filters.add(new InputFilter("Station Longitude",
        geolocTableName + "longitude" + rd, "longitude",
        StringUtil.TYPE_DOUBLE, null, null, false));
    filters.add(new InputFilter("Station State",
        geolocTableName + "state" + rd, "state", StringUtil.TYPE_STRING,
        geolocStateList, geolocStateList, false));
    filters.add(new InputFilter("Station Name",
        measLocTableName + "measloc_name" + rd, "measloc_name", 
        StringUtil.TYPE_STRING, null, null, false));
    filters.add(new InputFilter("Station X",
        geolocTableName + "x" + rd, "x",
        StringUtil.TYPE_DOUBLE, null, null, false));
    filters.add(new InputFilter("Station Y",
        geolocTableName + "y" + rd, "y",
        StringUtil.TYPE_DOUBLE, null, null, false));*/

    setToolTipText("<html>ColoradoBNDSS queries can be filtered based on station and time series metadata.</html>");
    setInputFilters(filters, numFilterGroups, 15);
    // Always initially select the first filter to be a subject of County.  This is a good
    // default and if a more specific setting is used, it can be set after this default
    try {
        // TODO SAM 2010-05-20 Need to encode conditions as enum
        Message.printStatus(2, routine, "Setting Subject filter to \"" + subjectType + "\"" );
        setInputFilter ( 0, "Subject;Matches;" + subjectType, null );
    }
    catch ( Exception e ) {
        Message.printWarning(3, routine, "Unable to initialize filter to subject (" + e + ").");
        Message.printWarning(3, routine, e);
    }
    // Add event listeners so that if the subject changes the lists can be refreshed
    // Do this after initial setup so that don't get into an infinite loop.
    addEventListeners ( this );
}

/**
Return the data store corresponding to this input filter panel.
@return the data store corresponding to this input filter panel.
*/
public ColoradoBNDSSDataStore getDataStore ( )
{
    return __dataStore;
}

}