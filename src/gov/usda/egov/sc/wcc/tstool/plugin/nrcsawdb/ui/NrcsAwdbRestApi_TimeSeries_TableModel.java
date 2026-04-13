// NrcsAwdbRestApi_TimeSeries_TableModel - table model for the time series catalog

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 2026 Colorado Department of Natural Resources

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

package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.ui;

import java.util.List;

import RTi.Util.GUI.JWorksheet_AbstractRowTableModel;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.datastore.NrcsAwdbRestApiDataStore;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.TimeSeriesCatalog;

/**
This class is a table model for time series header information for NRCS AWDB REST API web services time series.
By default the sheet will contain row and column numbers.
*/
@SuppressWarnings({ "serial", "rawtypes" })
public class NrcsAwdbRestApi_TimeSeries_TableModel extends JWorksheet_AbstractRowTableModel {
	
	/**
	Number of columns in the table model.
	*/
	private final int COLUMNS = 31;

	public final int COL_STATION_ID = 0;
	public final int COL_STATION_NAME = 1;
	public final int COL_DATA_SOURCE = 2; // Same as network.
	public final int COL_DATA_TYPE = 3; // Same as element
	public final int COL_DATA_INTERVAL = 4;
	// Ectra objects.
	public final int COL_STATION_IS_RESERVOIR = 5;
	public final int COL_STATION_IS_FORECAST_POINT = 6;
	// Element data.
	public final int COL_ELEMENT_DATA_UNITS = 7;
	public final int COL_ELEMENT_DATA_UNITS_ORIG = 8;
	public final int COL_ELEMENT_DURATION_NAME = 9;
	public final int COL_ELEMENT_BEGIN_DATE = 10;
	public final int COL_ELEMENT_END_DATE = 11;
	public final int COL_ELEMENT_DERIVED_DATA = 12;
	public final int COL_ELEMENT_ORDINAL = 13;
	public final int COL_ELEMENT_HEIGHT_DEPTH = 14;
	public final int COL_ELEMENT_DATA_PRECISION = 15;
	// Station data.
	public final int COL_STATION_TRIPLET = 16;
	public final int COL_STATION_SHEF_ID = 17;
	public final int COL_STATION_OPERATOR = 18;
	public final int COL_STATION_LONGITUDE = 19;
	public final int COL_STATION_LATITUDE = 20;
	public final int COL_STATION_ELEVATION = 21;
	public final int COL_STATION_TIMEZONE = 22;
	// Network data.
	public final int COL_STATION_NETWORK_CODE = 23;
	public final int COL_STATION_DCO_CODE = 24;
	// Location data.
	public final int COL_STATION_STATE_CODE = 25;
	public final int COL_STATION_COUNTY_NAME = 26;
	public final int COL_STATION_HUC = 27;
	//
	public final int COL_TSID = 28;
	public final int COL_PROBLEMS = 29;
	public final int COL_DATASTORE = 30;
	
	/**
	Datastore corresponding to datastore used to retrieve the data.
	*/
	NrcsAwdbRestApiDataStore datastore = null;

	/**
	Data are a list of TimeSeriesCatalog.
	*/
	private List<TimeSeriesCatalog> timeSeriesCatalogList = null;

	/**
	Constructor.  This builds the model for displaying the given NRCS AWDB REST API time series data.
	@param dataStore the data store for the data
	@param data the list of NRCS AWDB REST API TimeSeriesCatalog that will be displayed in the table.
	@throws Exception if an invalid results passed in.
	*/
	@SuppressWarnings("unchecked")
	public NrcsAwdbRestApi_TimeSeries_TableModel ( NrcsAwdbRestApiDataStore dataStore, List<? extends Object> data ) {
		if ( data == null ) {
			_rows = 0;
		}
		else {
		    _rows = data.size();
		}
	    this.datastore = dataStore;
		_data = data; // Generic
		// TODO SAM 2016-04-17 Need to use instanceof here to check.
		this.timeSeriesCatalogList = (List<TimeSeriesCatalog>)data;
	}

	/**
	From AbstractTableModel.  Returns the class of the data stored in a given column.
	@param columnIndex the column for which to return the data class.
	*/
	@SuppressWarnings({ "unchecked" })
	public Class getColumnClass (int columnIndex) {
		switch (columnIndex) {
			// List in the same order as top of the class.
			case COL_STATION_ID: return Integer.class;
			case COL_STATION_LATITUDE: return Double.class;
			case COL_STATION_LONGITUDE: return Double.class;
			case COL_STATION_ELEVATION: return Double.class;
			case COL_STATION_TIMEZONE: return Integer.class;
			case COL_ELEMENT_ORDINAL: return Integer.class;
			case COL_ELEMENT_DERIVED_DATA: return Boolean.class;
			case COL_STATION_IS_RESERVOIR: return Boolean.class;
			case COL_STATION_IS_FORECAST_POINT: return Boolean.class;
			case COL_ELEMENT_HEIGHT_DEPTH: return Integer.class;
			default: return String.class; // All others.
		}
	}

	/**
	From AbstractTableMode.  Returns the number of columns of data.
	@return the number of columns of data.
	*/
	public int getColumnCount() {
		return this.COLUMNS;
	}

	/**
	From AbstractTableMode.  Returns the name of the column at the given position.
	@return the name of the column at the given position.
	*/
	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
			// General.
			case COL_STATION_ID: return "Station ID";
			case COL_STATION_NAME: return "Station Name";
			case COL_DATA_TYPE: return "Data Type";
			case COL_DATA_SOURCE: return "Data Source";
			case COL_DATA_INTERVAL: return "Interval";
			// Extra objects:
			case COL_STATION_IS_RESERVOIR: return "Is Reservoir";
			case COL_STATION_IS_FORECAST_POINT: return "Is Forecast Point";
			// Element data.
			case COL_ELEMENT_DATA_UNITS: return "Units";
			case COL_ELEMENT_DATA_UNITS_ORIG: return "Units (orig)";
			case COL_ELEMENT_DURATION_NAME: return "Duration Name";
			case COL_ELEMENT_BEGIN_DATE: return "Begin Date";
			case COL_ELEMENT_END_DATE: return "End Date";
			case COL_ELEMENT_DERIVED_DATA: return "Derived";
			case COL_ELEMENT_ORDINAL: return "Ordinal";
			case COL_ELEMENT_HEIGHT_DEPTH: return "Height Depth";
			case COL_ELEMENT_DATA_PRECISION: return "Data Precision";
			// Station data.
			case COL_STATION_TRIPLET: return "Station Triplet";
			case COL_STATION_SHEF_ID: return "SHEF ID";
			case COL_STATION_OPERATOR: return "Operator";
			case COL_STATION_LONGITUDE: return "Longitude";
			case COL_STATION_LATITUDE: return "Latitude";
			case COL_STATION_ELEVATION: return "Elevation";
			case COL_STATION_TIMEZONE: return "Time Zone";
			// Network data.
			case COL_STATION_NETWORK_CODE: return "Network";
			case COL_STATION_DCO_CODE: return "DCO";
			// Location data.
			case COL_STATION_STATE_CODE: return "State";
			case COL_STATION_COUNTY_NAME: return "County";
			case COL_STATION_HUC: return "HUC";
			// General.
			case COL_TSID: return "TSTool Time Series ID (TSID)";
			case COL_PROBLEMS: return "Problems";
			case COL_DATASTORE: return "Datastore";

			default: return "";
		}
	}

	/**
	Returns an array containing the column widths (in number of characters).
	@return an integer array containing the widths for each field.
	*/
	public String[] getColumnToolTips() {
	    String[] toolTips = new String[this.COLUMNS];
		// General.
	    toolTips[COL_STATION_ID] = "Station identifier";
	    toolTips[COL_STATION_NAME] = "Station name";
	    toolTips[COL_DATA_TYPE] = "Time series data type (NRCS AWDB element)";
	    toolTips[COL_DATA_SOURCE] = "Data source (NRCS network)";
	    toolTips[COL_DATA_INTERVAL] = "Time series data interval";
		// Extra objects.
		toolTips[COL_STATION_IS_RESERVOIR] = "Is the station a reservoir?";
		toolTips[COL_STATION_IS_FORECAST_POINT] = "Is the station a forecast point?";
		// Element data.
		toolTips[COL_ELEMENT_DATA_UNITS] = "Data units";
		toolTips[COL_ELEMENT_DATA_UNITS_ORIG] = "Data units (original)";
		toolTips[COL_ELEMENT_DURATION_NAME] = "Duration name";
		toolTips[COL_ELEMENT_BEGIN_DATE] = "Begin date for time series";
		toolTips[COL_ELEMENT_END_DATE] = "End date for time series";
		toolTips[COL_ELEMENT_DERIVED_DATA] = "Are the data derived?";
		toolTips[COL_ELEMENT_ORDINAL] = "Ordinal";
		toolTips[COL_ELEMENT_HEIGHT_DEPTH] = "Height/Depth";
		toolTips[COL_ELEMENT_DATA_PRECISION] = "Data Precision";
		// Station data.
		toolTips[COL_STATION_TRIPLET] = "Station Triplet";
		toolTips[COL_STATION_SHEF_ID] = "SHEF ID";
		toolTips[COL_STATION_OPERATOR] = "Operator";
	    toolTips[COL_STATION_LONGITUDE] = "Station longitude, decimal degrees";
	    toolTips[COL_STATION_LATITUDE] = "Station latitude, decimal degrees";
	    toolTips[COL_STATION_ELEVATION] = "Station elevation";
		toolTips[COL_STATION_TIMEZONE] = "Station time zone";
		// Network data.
		toolTips[COL_STATION_NETWORK_CODE] = "Network";
		toolTips[COL_STATION_DCO_CODE] = "DCO";
		// Location data.
		toolTips[COL_STATION_STATE_CODE] = "State";
		toolTips[COL_STATION_COUNTY_NAME] = "County";
		toolTips[COL_STATION_HUC] = "HUC";
	    // General.
		toolTips[COL_TSID] = "Time series identifier";
		toolTips[COL_PROBLEMS] = "Problems";
		toolTips[COL_DATASTORE] = "Datastore name";
	    return toolTips;
	}

	/**
	Returns an array containing the column widths (in number of characters).
	@return an integer array containing the widths for each field.
	*/
	public int[] getColumnWidths() {
		int[] widths = new int[this.COLUMNS];
		// General.
	    widths[COL_STATION_ID] = 12;
	    widths[COL_STATION_NAME] = 20;
	    widths[COL_DATA_SOURCE] = 8;
	    widths[COL_DATA_TYPE] = 8;
	    widths[COL_DATA_INTERVAL] = 6;
		// Extra objects.
		widths[COL_STATION_IS_RESERVOIR] = 8;
		widths[COL_STATION_IS_FORECAST_POINT] = 12;
		// Element data.
		widths[COL_ELEMENT_DATA_UNITS] = 8;
		widths[COL_ELEMENT_DATA_UNITS_ORIG] = 8;
		widths[COL_ELEMENT_DURATION_NAME] = 10;
		widths[COL_ELEMENT_BEGIN_DATE] = 10;
		widths[COL_ELEMENT_END_DATE] = 10;
		widths[COL_ELEMENT_DERIVED_DATA] = 6;
		widths[COL_ELEMENT_ORDINAL] = 6;
		widths[COL_ELEMENT_HEIGHT_DEPTH] = 8;
		widths[COL_ELEMENT_DATA_PRECISION] = 10;
		// Station data.
		widths[COL_STATION_TRIPLET] = 10;
		widths[COL_STATION_SHEF_ID] = 6;
		widths[COL_STATION_OPERATOR] = 8;
	    widths[COL_STATION_LONGITUDE] = 7;
	    widths[COL_STATION_LATITUDE] = 6;
	    widths[COL_STATION_ELEVATION] = 6;
		widths[COL_STATION_TIMEZONE] = 7;
		// Network data.
		widths[COL_STATION_NETWORK_CODE] = 6;
		widths[COL_STATION_DCO_CODE] = 6;
		// Location data.
		widths[COL_STATION_STATE_CODE] = 5;
		widths[COL_STATION_COUNTY_NAME] = 10;
		widths[COL_STATION_HUC] = 8;
	    // General.
	    widths[COL_TSID] = 25;
		widths[COL_PROBLEMS] = 30;
		widths[COL_DATASTORE] = 20;
		return widths;
	}

	/**
	Returns the format to display the specified column.
	@param column column for which to return the format.
	@return the format (as used by StringUtil.formatString()).
	*/
	public String getFormat ( int column ) {
		switch (column) {
			case COL_STATION_LONGITUDE: return "%.6f";
			case COL_STATION_LATITUDE: return "%.6f";
			case COL_STATION_ELEVATION: return "%.2f";
			default: return "%s"; // All else are strings.
		}
	}

	/**
	From AbstractTableMode.  Returns the number of rows of data in the table.
	*/
	public int getRowCount() {
		return _rows;
	}

	/**
	From AbstractTableMode.  Returns the data that should be placed in the JTable at the given row and column.
	@param row the row for which to return data.
	@param col the column for which to return data.
	@return the data that should be placed in the JTable at the given row and column.
	*/
	public Object getValueAt(int row, int col) {
		// Make sure the row numbers are never sorted.
		if (_sortOrder != null) {
			row = _sortOrder[row];
		}

		TimeSeriesCatalog timeSeriesCatalog = this.timeSeriesCatalogList.get(row);
		switch (col) {
			// OK to allow null because will be displayed as blank.
			// General.
			case COL_STATION_ID: return timeSeriesCatalog.getStationId();
			case COL_STATION_NAME: return timeSeriesCatalog.getStationName();
			case COL_DATA_SOURCE: return timeSeriesCatalog.getDataSource();
			case COL_DATA_TYPE: return timeSeriesCatalog.getDataType();
			case COL_DATA_INTERVAL: return timeSeriesCatalog.getDataInterval();
			// Extra objects
			case COL_STATION_IS_RESERVOIR: return timeSeriesCatalog.getStationIsReservoir();
			case COL_STATION_IS_FORECAST_POINT: return timeSeriesCatalog.getStationIsForecastPoint();
			// Element data.
			case COL_ELEMENT_DATA_UNITS: return timeSeriesCatalog.getElementDataUnits();
			case COL_ELEMENT_DATA_UNITS_ORIG: return timeSeriesCatalog.getElementDataUnitsOriginal();
			case COL_ELEMENT_DURATION_NAME: return timeSeriesCatalog.getElementDurationName();
			case COL_ELEMENT_BEGIN_DATE: return timeSeriesCatalog.getElementBeginDate();
			case COL_ELEMENT_END_DATE: return timeSeriesCatalog.getElementEndDate();
			case COL_ELEMENT_DERIVED_DATA: return timeSeriesCatalog.getElementDerivedData();
			case COL_ELEMENT_ORDINAL: return timeSeriesCatalog.getElementOrdinal();
			case COL_ELEMENT_HEIGHT_DEPTH: return timeSeriesCatalog.getElementHeightDepth();
			case COL_ELEMENT_DATA_PRECISION: return timeSeriesCatalog.getElementDataPrecision();
			// Station data.
			case COL_STATION_TRIPLET: return timeSeriesCatalog.getStationTriplet();
			case COL_STATION_SHEF_ID: return timeSeriesCatalog.getStationShefId();
			case COL_STATION_OPERATOR: return timeSeriesCatalog.getStationOperator();
			case COL_STATION_LONGITUDE: return timeSeriesCatalog.getStationLongitude();
			case COL_STATION_LATITUDE: return timeSeriesCatalog.getStationLatitude();
			case COL_STATION_ELEVATION: return timeSeriesCatalog.getStationElevation();
			case COL_STATION_TIMEZONE: return timeSeriesCatalog.getStationDataTimeZone();
			// Network data.
			case COL_STATION_NETWORK_CODE: return timeSeriesCatalog.getStationNetwork();
			case COL_STATION_DCO_CODE: return timeSeriesCatalog.getStationDco();
			// Location data.
			case COL_STATION_STATE_CODE: return timeSeriesCatalog.getStationState();
			case COL_STATION_COUNTY_NAME: return timeSeriesCatalog.getStationCounty();
			case COL_STATION_HUC: return timeSeriesCatalog.getStationHuc();
			// General.
			case COL_TSID: return timeSeriesCatalog.getTSID();
			case COL_PROBLEMS: return timeSeriesCatalog.formatProblems();			
			case COL_DATASTORE: return this.datastore.getName();			
			default: return "";
		}
	}

}