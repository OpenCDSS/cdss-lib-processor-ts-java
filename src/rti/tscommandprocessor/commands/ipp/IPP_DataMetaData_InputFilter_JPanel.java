package rti.tscommandprocessor.commands.ipp;

import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;

import RTi.Util.IO.PropList;

import RTi.Util.String.StringUtil;

/**
This class is an input filter for querying ColoradoIPP joined records from Count or Provider or Project.
*/
public class IPP_DataMetaData_InputFilter_JPanel extends InputFilter_JPanel {

/**
Constructor.
@param dmi the dmi to use to connect to the database.  Cannot be null.
@param subjectType the subject type being processed
*/
public IPP_DataMetaData_InputFilter_JPanel(IppDMI dmi, IPPSubjectType subjectType )
{   //String routine = getClass().getName() + ".IPP_DataMetaData_InputFilter_JPanel";
	String rd = dmi.getRightIdDelim();
	String ld = dmi.getLeftIdDelim();

	List filters = new Vector();

	String dataTableName = "v" + subjectType + "DataMetaData." + ld;

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
	
    filters.add(new InputFilter("Data Type (Main)",
        dataTableName + "dataType" + rd, "dataType",
        StringUtil.TYPE_STRING, dataTypeList, dataTypeList, false));
    
    filters.add(new InputFilter("Data Type (subtype)",
        dataTableName + "subType" + rd, "subType",
        StringUtil.TYPE_STRING, subTypeList, subTypeList, false));
    
    filters.add(new InputFilter("Identifier",
        dataTableName + "subjectID" + rd, "subjectID",
        StringUtil.TYPE_STRING, subjectIDList, subjectIDList, true)); // Allow user to specify substring
    
    filters.add(new InputFilter("Name",
        dataTableName + "name" + rd, "name",
        StringUtil.TYPE_STRING, null, null, false));
    
    filters.add(new InputFilter("Method",
        dataTableName + "method" + rd, "method",
        StringUtil.TYPE_STRING, methodList, methodList, false));
    
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
		
	PropList filterProps = new PropList("InputFilter");
	filterProps.set("NumFilterGroups=5");
	filterProps.set("NumWhereRowsToDisplay=15"); // Display all without scrolling
	setToolTipText("<html>ColoradoIPP queries can be filtered " 
		+ "based on station and time series metadata.</html>");
	setInputFilters(filters, filterProps);
}

}