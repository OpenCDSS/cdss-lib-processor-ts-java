package cdss.dmi.hydrobase.rest.commands;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;

import RTi.Util.String.StringUtil;
import cdss.dmi.hydrobase.rest.ColoradoHydroBaseRestDataStore;
import cdss.dmi.hydrobase.rest.dao.ReferenceTablesCounty;
import cdss.dmi.hydrobase.rest.dao.ReferenceTablesWaterDistrict;
import cdss.dmi.hydrobase.rest.dao.ReferenceTablesWaterDivision;

@SuppressWarnings("serial")
public class ColoradoHydroBaseRest_TelemetryStation_InputFilter_JPanel
extends InputFilter_JPanel
implements MouseListener
{
    
/**
Datastore for this panel
*/
private ColoradoHydroBaseRestDataStore datastore = null;

/**
Create an InputFilter_JPanel for ColoradoHydroBaseRest web services telemetry (real-time) station queries.
This is used by TSTool.
Default filter panel properties are used (e.g., 3 filter groups).
@return a JPanel containing InputFilter instances for telemetry station queries.
@param dataStore ColoradoHydroBaseRestDataStore instance.
@exception Exception if there is an error.
*/
public ColoradoHydroBaseRest_TelemetryStation_InputFilter_JPanel (
		ColoradoHydroBaseRestDataStore dataStore )
throws Exception
{	this ( dataStore, -1, -1 );
}

/**
Create an InputFilter_JPanel for ColoradoHydroBaseRest web services telemetry (real-time) station queries.
This is used by TSTool.
@return a JPanel containing InputFilter instances for telemetry station queries.
@param dataStore ColoradoHydroBaseRestDataStore instance.
@param numFilterGroups the number of filter groups to display
@param numWhereChoicesToDisplay the number of where choices to display in each filter
@exception Exception if there is an error.
*/
public ColoradoHydroBaseRest_TelemetryStation_InputFilter_JPanel (
		ColoradoHydroBaseRestDataStore datastore, int numFilterGroups, int numWhereChoicesToDisplay )
throws Exception
{	this.datastore = datastore;
	
	// Now define the input filters

	List<InputFilter> input_filters = new Vector<InputFilter>(8);
	input_filters.add ( new InputFilter ("", "",
	    StringUtil.TYPE_STRING, null, null, true ) ); // Blank to disable filter

	InputFilter filter;
    // Fill in the county for input filters...
	List<ReferenceTablesCounty> countyDataList = datastore.getCounties();
	List<String> countyList = new ArrayList<String> ( countyDataList.size() );
	for ( ReferenceTablesCounty county : countyDataList ) {
		countyList.add ( county.getCounty() ); // TODO smalers 2018-06-19 HydroBase has state + ", CO" );
	}
	filter = new InputFilter ( "County", "county", "county",
		StringUtil.TYPE_STRING, countyList, countyList, false );
	filter.setTokenInfo(",",0);
	input_filters.add ( filter );
	
	List<ReferenceTablesWaterDistrict> districtDataList = datastore.getDistricts();
	List<String> districtList = new ArrayList<String> ( districtDataList.size() );
	List<String> districtInternalList = new ArrayList<String>(districtDataList.size());
	for ( ReferenceTablesWaterDistrict wd : districtDataList ) {
		districtList.add ("" + wd.getWd() + " - " + wd.getWdName());
		districtInternalList.add ("" + wd.getWd() );
	}
	filter = new InputFilter ( "Water District", "waterDistrict", "waterDistrict",
		StringUtil.TYPE_STRING, districtList, districtInternalList, false );
	filter.setTokenInfo("-",0);
	input_filters.add ( filter );

	List<ReferenceTablesWaterDivision> divisionDataList = datastore.getDivisions();
	List<String> divisionList = new ArrayList<String> ( 7 );
	List<String> divisionInternalList = new ArrayList<String> ( 7 );
	for ( ReferenceTablesWaterDivision div: divisionDataList ) {
		divisionList.add ("" + div.getDiv() + " - " + div.getDivName());
		divisionInternalList.add ("" + div.getDiv() );
	}
	filter = new InputFilter ( "Water Division", "waterDivision", "waterDivision",
		StringUtil.TYPE_STRING, divisionList, divisionInternalList, false );
	filter.setTokenInfo("-",0);
	input_filters.add ( filter );
	
	/*
	input_filters.add ( new InputFilter ( "Elevation", "geoloc.elev", "elev",
		StringUtil.TYPE_DOUBLE, null, null, true ) );
		*/
	
	/*
	input_filters.add ( new InputFilter ( "HUC", "geoloc.huc", "huc",
		StringUtil.TYPE_STRING, null, null, true ) );
		*/

	/*
	input_filters.add ( new InputFilter ( "Latitude", "geoloc.latdecdeg", "latdecdeg",
		StringUtil.TYPE_DOUBLE, null, null, true ) );
		
	input_filters.add ( new InputFilter ( "Longitude", "geoloc.longdecdeg", "longdecdeg",
		StringUtil.TYPE_DOUBLE, null, null, true ) );
		*/

	/*
	// create the input filter for the PLSS Location
	filter = new InputFilter(
		HydroBase_GUI_Util._PLSS_LOCATION_LABEL,
		HydroBase_GUI_Util._PLSS_LOCATION, 
		HydroBase_GUI_Util._PLSS_LOCATION, StringUtil.TYPE_STRING,
		null, null, false);
	// all constraints other than EQUALS are removed because PLSS Locations
	// are compared in a special way
	filter.removeConstraint(InputFilter.INPUT_ONE_OF);
	filter.removeConstraint(InputFilter.INPUT_STARTS_WITH);
	filter.removeConstraint(InputFilter.INPUT_ENDS_WITH);
	filter.removeConstraint(InputFilter.INPUT_CONTAINS);
	// the PLSS Location text field is not editable because users must go
	// through the PLSS Location JDialog to build a location
	filter.setInputJTextFieldEditable(false);
	// this listener must be set up so that the location builder dialog
	// can be opened when the PLSS Location text field is clicked on.
	filter.addInputComponentMouseListener(this);
	filter.setInputComponentToolTipText("Click in this field to build a PLSS Location to use as a query constraint.");
	filter.setInputJTextFieldWidth(20);
	input_filters.add(filter);
	*/

	/*
	input_filters.add(new InputFilter("Stream Mile", "str_mile", "str_mile", 
		StringUtil.TYPE_DOUBLE, null, null, true));
		*/
/*
	input_filters.add ( new InputFilter ( "Structure ID", "id", "id",
		StringUtil.TYPE_INTEGER, null, null, true ) );
		
	input_filters.add ( new InputFilter ( "Structure Name", "str_name", "str_name",
		StringUtil.TYPE_STRING, null, null, true ) );
		*/
	
	/*
    input_filters.add ( new InputFilter ( "Structure WDID", "wdid", "wdid",
        StringUtil.TYPE_INTEGER, null, null, true ) );
        */

	/* Not enabled yet
	if ( include_SFUT ) {
		input_filters.add ( new InputFilter ("SFUT", "struct_meas_type.identifier", "identifier",
			StringUtil.TYPE_STRING, null, null, true ) );
	}
	*/

	/*
	input_filters.add ( new InputFilter ( "UTM X", "utm_x", "utm_x",
		StringUtil.TYPE_DOUBLE, null, null, true ) );		

	input_filters.add ( new InputFilter ( "UTM Y", "utm_y", "utm_y",
		StringUtil.TYPE_DOUBLE, null, null, true ) );
		*/

	if ( numFilterGroups < 0 ) {
		// TODO SAM 2010-07-21 need larger default?
		numFilterGroups = 3;
		numWhereChoicesToDisplay = input_filters.size();
	}
	setToolTipText ( "<html>ColoradoHydroBaseRest telemetry station queries can be filtered based on station data.</html>" );
	setInputFilters ( input_filters, numFilterGroups, numWhereChoicesToDisplay );
}

public ColoradoHydroBaseRestDataStore getColoradoHydroBaseRestDataStore ()
{
    return this.datastore;
}

public void mouseClicked(MouseEvent event) {}

public void mouseExited(MouseEvent event) {}

public void mouseEntered(MouseEvent event) {}

/**
Responds to mouse pressed events.
@param event the event that happened.
*/
public void mousePressed(MouseEvent event) {
    /** Not enabled - used for PLSS query
	JFrame temp = new JFrame();
	JGUIUtil.setIcon(temp, JGUIUtil.getIconImage());	
	HydroBase_GUI_Util.buildLocation(temp, (JTextField)event.getSource());
	*/
}

public void mouseReleased(MouseEvent event) {}

}