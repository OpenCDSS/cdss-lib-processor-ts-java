// NrcsAwdbRestApi_TimeSeries_InputFilter_JPanel - panel to filter time series queries

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import RTi.Util.GUI.InputFilter;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.datastore.NrcsAwdbRestApiDataStore;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.TimeSeriesCatalog;

/**
This class is an input filter for querying NRCS AWDB REST API web services.
*/
@SuppressWarnings("serial")
public class NrcsAwdbRestApi_TimeSeries_InputFilter_JPanel extends InputFilter_JPanel {

	/**
	Test datastore, for connection.
	*/
	private NrcsAwdbRestApiDataStore datastore = null;

	/**
	Constructor for case when no datastore is configured - default panel.
	@param label label for the panel
	*/
	public NrcsAwdbRestApi_TimeSeries_InputFilter_JPanel ( String label ) {
		super(label);
	}

	/**
	Constructor.
	@param dataStore the data store to use to connect to the test database.  Cannot be null.
	@param numFilterGroups the number of filter groups to display
	*/
	public NrcsAwdbRestApi_TimeSeries_InputFilter_JPanel( NrcsAwdbRestApiDataStore dataStore, int numFilterGroups ) {
	    super();
	    this.datastore = dataStore;
	    if ( this.datastore != null ) {
	        setFilters ( numFilterGroups );
	    }
	}

	/**
	Set the filter data.  This method is called at setup and when refreshing the list with a new subject type.
	For all cases, use the InputFilter constructor "whereLabelPersistent" to ensure that the TSTool ReadNrcsAwdb command will show a nice.
	*/
	public void setFilters ( int numFilterGroups ) {
		//String routine = getClass().getSimpleName() + ".setFilters";
		
		// Read the data to populate filter choices.

		// The internal names for filters match the /stations web service query parameters.

	    List<InputFilter> filters = new ArrayList<>();

	    // Always add blank to top of filter
	    filters.add(new InputFilter("", "", StringUtil.TYPE_STRING, null, null, false)); // Blank.

	    // Because the list of stations is long, use an empty list but allow the text field to be used for entry.
	    List<String> stationIdChoices = new ArrayList<>();
	    stationIdChoices.add("");
	    List<String> stationNameChoices = new ArrayList<>();
	    stationNameChoices.add("");
	    List<String> stationTripletChoices = new ArrayList<>();
	    stationTripletChoices.add("");

	    // Don't worry about he persistent name yet since the ReadNrcsAwdb command does not have single and multiple time series options.
	    filters.add(new InputFilter("Station - ID",
            "stationId", "stationId", "stationId",
            StringUtil.TYPE_STRING, stationIdChoices, stationIdChoices, true));

	    filters.add(new InputFilter("Station - Name",
            "stationName", "stationName", "stationName",
            StringUtil.TYPE_STRING, stationNameChoices, stationNameChoices, true));

	    filters.add(new InputFilter("Station - Triplet",
            "stationTriplet", "stationTriplet", "stationTriplet",
            StringUtil.TYPE_STRING, stationTripletChoices, stationTripletChoices, true));

	  	setToolTipText("<html>Specify one or more input filters to limit query, will be ANDed.</html>");
	    
	    int numVisible = 14;
	    setInputFilters(filters, numFilterGroups, numVisible);
	}

	/**
	Return the data store corresponding to this input filter panel.
	@return the data store corresponding to this input filter panel.
	*/
	public NrcsAwdbRestApiDataStore getDataStore ( ) {
	    return this.datastore;
	}
}