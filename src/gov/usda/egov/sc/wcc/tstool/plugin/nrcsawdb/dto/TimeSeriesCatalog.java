// TimeSeriesCatalog - list of time series

/* NoticeStart

OWF TSTool NRCS AWDB REST API Plugin
Copyright (C) 2026 Open Water Foundation

OWF TSTool NRCS AWDB REST API Plugin is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

OWF TSTool NRCS AWDB REST API Plugin is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with OWF TSTool NRCS ASDB REST API Plugin.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.ArrayList;
import java.util.List;

import RTi.TS.TSIdent;

/**
 * Class to store time series catalog (metadata) for NRCS AWDB TSTool time series list.
 * This is a combination of standard time series properties used in TSTool and NRCS AWDB REST API data.
 * More data may be included and shown in the table model while evaluating the web services and will be removed or disabled later.
 * The types are as one would expect, whereas the 'TimeSeries' object uses strings as per web service JSON types.
 */
public class TimeSeriesCatalog {

	// General data, provided by TSTool, extracted/duplicated from NRCS AWDB REST API services.
	// See below for stationId.
	// private String stationId - use stationId
	//private String dataSource - use stationNetwork
	private String dataInterval = ""; // TSTool data interval (determined from elementDurationName
	// private String dataType - use elementCode
	// private String dataUnits - use elementDataUnits

	// Element data.
	private String elementCode = "";
	private String elementDataUnits = "";
	private String elementDataUnitsOriginal = "";
	private String elementBeginDate = "";
	private String elementEndDate = "";
	private Boolean elementDerivedData = null;
	private Integer elementOrdinal = null;
	private Integer elementHeightDepth = null;
	private String elementDurationName = null;
	private Integer elementDataPrecision = null;

	// Station data.
	private String stationId = "";
	private String stationName = "";
	private String stationTriplet = "";
	private String stationShefId = "";
	private String stationOperator = "";
	private Double stationLongitude = null;
	private Double stationLatitude = null;
	private Double stationElevation = null;
	private Integer stationDataTimeZone = null;

	// Network data.
	private String stationNetwork = "";
	private String stationDco = "";

	// Location data.
	private String stationState = "";
	private String stationCounty = "";
	private String stationHuc = "";
	
	// Extra data objects.
	private Boolean stationIsForecastPoint = null;
	private Boolean stationIsReservoir = null;
	
	// Time series properties.
	private String tsid = "";
	private String datastore = "";

	// List of problems, one string per issue.
	private List<String> problems = null; // Initialize to null to save memory ... must check elsewhere when using.

	/**
	 * Has ReadTSCatalog.checkData() resulted in problems being set?
	 * This is used when there are issues with non-unique time series identifiers.
	 * For example if two catalog are returned for a stationNumId, dataType, and dataInterval,
	 * each of the tscatalog is processed in checkData().  The will each be processed twice.
	 * This data member is set to true the first time so that the 'problems' list is only set once
	 * in TSCatalogDAO.checkData().
	 */
	private boolean haveCheckDataProblemsBeenSet = false;

	/**
	 * Constructor.
	 */
	public TimeSeriesCatalog () {
		//this.dataSource = "NrcsAwdb";
	}

	/**
	 * Copy constructor.
	 * @param timeSeriesCatalog instance to copy
	 */
	public TimeSeriesCatalog ( TimeSeriesCatalog timeSeriesCatalog ) {
		// Do a deep copy by default as per normal Java conventions.
		this(timeSeriesCatalog, true);
	}

	/**
	 * Copy constructor.
	 * @param timeSeriesCatalog instance to copy
	 * @param deepCopy indicates whether an exact deep copy should be made (true)
	 * or a shallow copy that is typically used when defining a derived catalog record.
	 * For example, use deepCopy=false when copying a scaled catalog entry for a rated time series.
	 */
	public TimeSeriesCatalog ( TimeSeriesCatalog timeSeriesCatalog, boolean deepCopy ) {
		// List in the same order as internal data member list.
		//this.dataSource = timeSeriesCatalog.dataSource;
		//this.dataType = timeSeriesCatalog.dataType;
		this.dataInterval = timeSeriesCatalog.dataInterval;

		this.elementDataUnits = timeSeriesCatalog.elementDataUnits;

		// Element data.
		this.elementCode = timeSeriesCatalog.elementCode;
		this.elementDataUnits = timeSeriesCatalog.elementDataUnits;
		this.elementDataUnitsOriginal = timeSeriesCatalog.elementDataUnitsOriginal;
		this.elementBeginDate = timeSeriesCatalog.elementBeginDate;
		this.elementEndDate = timeSeriesCatalog.elementEndDate;
		this.elementDerivedData = timeSeriesCatalog.elementDerivedData;
		this.elementOrdinal = timeSeriesCatalog.elementOrdinal;
		this.elementHeightDepth = timeSeriesCatalog.elementHeightDepth;
		this.elementDataPrecision = timeSeriesCatalog.elementDataPrecision;

		// Station data.
		this.stationId = timeSeriesCatalog.stationId;
		this.stationTriplet = timeSeriesCatalog.stationTriplet;
		this.stationShefId = timeSeriesCatalog.stationShefId;
		this.stationOperator = timeSeriesCatalog.stationOperator;
		this.stationLongitude = timeSeriesCatalog.stationLongitude;
		this.stationLatitude = timeSeriesCatalog.stationLatitude;
		this.stationElevation = timeSeriesCatalog.stationElevation;
		this.stationDataTimeZone = timeSeriesCatalog.stationDataTimeZone;

		// Network data.
		this.stationNetwork = timeSeriesCatalog.stationNetwork;
		this.stationDco = timeSeriesCatalog.stationDco;

		// Location data.
		this.stationState = timeSeriesCatalog.stationState;
		this.stationCounty = timeSeriesCatalog.stationCounty;
		this.stationHuc = timeSeriesCatalog.stationHuc;
		
		// Time series properties.
		this.tsid = timeSeriesCatalog.tsid;
		this.datastore = timeSeriesCatalog.datastore;
		
		if ( deepCopy ) {
			// Time series catalog problems.
			if ( timeSeriesCatalog.problems == null ) {
				this.problems = null;
			}
			else {
				// Create a new list.
				this.problems = new ArrayList<>();
				for ( String s : timeSeriesCatalog.problems ) {
					this.problems.add(s);
				}
			}
		}
		else {
			// Default is null problems list.
		}
	}

	/**
	 * Add a problem to the problem list.
	 * @param problem Single problem string.
	 */
	public void addProblem ( String problem ) {
		if ( this.problems == null ) {
			this.problems = new ArrayList<>();
		}
		this.problems.add(problem);
	}
	
	/**
	 * Clear the problems.
	 * @return
	 */
	public void clearProblems() {
		if ( this.problems != null ) {
			this.problems.clear();
		}
	}

	/**
	 * Find a list of TimeSeriesCatalog given the time series identifier to match.
	 * @param tsidentReq requested time series identifier to match
	 * @return one or more matching time series identifiers
	 */
    public static List<TimeSeriesCatalog> findForTSIdent ( List<TimeSeriesCatalog> tscatalogList, TSIdent tsidentReq ) {
    	List<TimeSeriesCatalog> tscatalogFoundList = new ArrayList<>();
    	String locId = tsidentReq.getLocation();
    	String dataType = tsidentReq.getType();
    	String interval = tsidentReq.getInterval();
    	for ( TimeSeriesCatalog tscatalog : tscatalogList ) {
    		if ( !tscatalog.getStationId().equals(locId) ) {
    			// Location ID is not a match.
    			continue;
    		}
    		if ( !tscatalog.getDataType().equals(dataType) ) {
    			// Data type is not a match.
    			continue;
    		}
    		if ( !tscatalog.getDataInterval().equalsIgnoreCase(interval) ) {
    			// Data interval is not a match:
    			// - comparison ignores case
    			continue;
    		}
    		// If here can add to the list.
    		tscatalogFoundList.add(tscatalog);
    	}
    	return tscatalogFoundList;
    }

	/**
	 * Format problems into a single string.
	 * @return formatted problems.
	 */
	public String formatProblems() {
		if ( this.problems == null ) {
			return "";
		}
		StringBuilder b = new StringBuilder();
		for ( int i = 0; i < problems.size(); i++ ) {
			if ( i > 0 ) {
				b.append("; ");
			}
			b.append(problems.get(i));
		}
		return b.toString();
	}

	public String getDataInterval ( ) {
		return this.dataInterval;
	}

	public String getDataSource ( ) {
		//return this.dataSource;
		return this.stationNetwork;
	}

	public String getDataStore ( ) {
		return this.datastore;
	}

	public String getDataType ( ) {
		//return this.dataType;
		return this.elementCode;
	}
	
	/**
	 * Get the list of distinct data intervals from the catalog, for example "IrregSecond", "15Minute".
	 * @param tscatalogList list of TimeSeriesCatalog to process.
	 * The list may have been filtered by data type previous to calling this method.
	 * @return a list of distinct data interval strings.
	 */
	public static List<String> getDistinctDataIntervals ( List<TimeSeriesCatalog> tscatalogList ) {
	    List<String> dataIntervalsDistinct = new ArrayList<>();
	    String dataInterval;
	    boolean found;
	    for ( TimeSeriesCatalog tscatalog : tscatalogList ) {
	    	// Data interval from the catalog, something like "Hour", "Day", "Month", "Year".
	    	dataInterval = tscatalog.getDataInterval();
	    	if ( dataInterval == null ) {
	    		continue;
	    	}
	    	found = false;
	    	for ( String dataInterval2 : dataIntervalsDistinct ) {
	    		if ( dataInterval2.equals(dataInterval) ) {
	    			found = true;
	    			break;
	    		}
	    	}
	    	if ( !found ) {
	    		// Add to the list of unique data types.
	    		dataIntervalsDistinct.add(dataInterval);
	    	}
	    }
	    return dataIntervalsDistinct;
	}

	/**
	 * Get the list of distinct data types from the catalog.
	 * @param tscatalogList list of TimeSeriesCatalog to process.
	 * @return a list of distinct data type strings.
	 */
	public static List<String> getDistinctDataTypes ( List<TimeSeriesCatalog> tscatalogList ) {
	    List<String> dataTypesDistinct = new ArrayList<>();
	    String dataType;
	    boolean found;
	    for ( TimeSeriesCatalog tscatalog : tscatalogList ) {
	    	// Data type from the catalog, something like "SRVO".
	    	dataType = tscatalog.getDataType();
	    	if ( dataType == null ) {
	    		continue;
	    	}
	    	found = false;
	    	for ( String dataType2 : dataTypesDistinct ) {
	    		if ( dataType2.equals(dataType) ) {
	    			found = true;
	    			break;
	    		}
	    	}
	    	if ( !found ) {
	    		// Add to the list of unique data types.
	    		dataTypesDistinct.add(dataType);
	    	}
	    }
	    return dataTypesDistinct;
	}

	public String getElementBeginDate ( ) {
		return this.elementBeginDate;
	}

	public String getElementCode ( ) {
		return this.elementCode;
	}

	public Integer getElementDataPrecision ( ) {
		return this.elementDataPrecision;
	}

	public String getElementDataUnits ( ) {
		return this.elementDataUnits;
	}

	public String getElementDataUnitsOriginal ( ) {
		return this.elementDataUnitsOriginal;
	}

	public Boolean getElementDerivedData ( ) {
		return this.elementDerivedData;
	}

	public String getElementDurationName ( ) {
		return this.elementDurationName;
	}

	public String getElementEndDate ( ) {
		return this.elementEndDate;
	}

	public Integer getElementHeightDepth ( ) {
		return this.elementHeightDepth;
	}

	public Integer getElementOrdinal ( ) {
		return this.elementOrdinal;
	}

	/**
	 * Return whether checkData() has resulted in problems being set.
	 * @return whether checkData() has resulted in problems being set.
	 */
	public boolean getHaveCheckDataProblemsBeenSet () {
		return this.haveCheckDataProblemsBeenSet;
	}

	public String getStationCounty ( ) {
		return this.stationCounty;
	}

	public String getStationDco ( ) {
		return this.stationDco;
	}
	
	public Double getStationElevation ( ) {
		return this.stationElevation;
	}

	public String getStationHuc ( ) {
		return this.stationHuc;
	}
	
	public String getStationId ( ) {
		return this.stationId;
	}
	
	public Boolean getStationIsForecastPoint ( ) {
		return this.stationIsForecastPoint;
	}

	public Boolean getStationIsReservoir ( ) {
		return this.stationIsReservoir;
	}

	public Double getStationLatitude ( ) {
		return this.stationLatitude;
	}
	
	public Double getStationLongitude ( ) {
		return this.stationLongitude;
	}

	public String getStationName ( ) {
		return this.stationName;
	}

	public String getStationNetwork ( ) {
		return this.stationNetwork;
	}
	
	public String getStationOperator ( ) {
		return this.stationOperator;
	}

	public String getStationShefId ( ) {
		return this.stationShefId;
	}
	
	public String getStationState ( ) {
		return this.stationState;
	}

	public Integer getStationDataTimeZone ( ) {
		return this.stationDataTimeZone;
	}
	
	public String getStationTriplet ( ) {
		return this.stationTriplet;
	}

	public String getTSID ( ) {
		return this.tsid;
	}

	public void setDataInterval ( String dataInterval ) {
		this.dataInterval = dataInterval;
	}
	
	public void setDataSource ( String dataSource ) {
		//this.dataSource = dataSource;
		setStationNetwork ( dataSource );
	}

	public void setDataStore ( String datastore ) {
		this.datastore = datastore;
	}

	public void setDataType ( String dataType ) {
		//this.dataType = dataType;
		setElementCode ( dataType );
	}
	
	public void setElementCode ( String elementCode ) {
		this.elementCode = elementCode;
	}

	public void setElementDataPrecision ( Integer elementDataPrecision ) {
		this.elementDataPrecision = elementDataPrecision;
	}

	public void setElementBeginDate ( String elementBeginDate ) {
		this.elementBeginDate = elementBeginDate;
	}

	public void setElementDataUnits ( String elementDataUnits ) {
		this.elementDataUnits = elementDataUnits;
	}

	public void setElementDataUnitsOriginal ( String elementDataUnitsOriginal ) {
		this.elementDataUnitsOriginal = elementDataUnitsOriginal;
	}

	public void setElementDerivedData ( Boolean elementDerivedData ) {
		this.elementDerivedData = elementDerivedData;
	}

	public void setElementDurationName ( String elementDurationName ) {
		this.elementDurationName = elementDurationName;
	}

	public void setElementEndDate ( String elementEndDate ) {
		this.elementEndDate = elementEndDate;
	}

	public void setElementHeightDepth ( Integer elementHeightDepth ) {
		this.elementHeightDepth = elementHeightDepth;
	}

	public void setElementOrdinal ( Integer elementOrdinal ) {
		this.elementOrdinal = elementOrdinal;
	}

	/**
	 * Set whether checkData() has resulted in problems being set.
	 * - TODO smalers 2020-12-15 not sure this is needed with the latest code.
	 *   Take out once tested out.
	 */
	public void setHaveCheckDataProblemsBeenSet ( boolean haveCheckDataProblemsBeenSet ) {
		this.haveCheckDataProblemsBeenSet = haveCheckDataProblemsBeenSet;
	}

	public void setStationCounty ( String stationCounty ) {
		this.stationCounty = stationCounty;
	}
	
	public void setStationDco ( String stationDco ) {
		this.stationDco = stationDco;
	}
	
	public void setStationElevation ( Double stationElevation ) {
		this.stationElevation = stationElevation;
	}

	public void setStationHuc ( String stationHuc ) {
		this.stationHuc = stationHuc;
	}
	
	public void setStationId ( String stationId ) {
		this.stationId = stationId;
	}

	public void setStationIsForecastPoint ( Boolean stationIsForecastPoint ) {
		this.stationIsForecastPoint = stationIsForecastPoint;
	}

	public void setStationIsReservoir ( Boolean stationIsReservoir ) {
		this.stationIsReservoir = stationIsReservoir;
	}
	
	public void setStationLatitude ( Double stationLatitude ) {
		this.stationLatitude = stationLatitude;
	}
	
	public void setStationLongitude ( Double stationLongitude ) {
		this.stationLongitude = stationLongitude;
	}

	public void setStationName ( String stationName ) {
		this.stationName = stationName;
	}
	
	public void setStationNetwork ( String stationNetwork ) {
		this.stationNetwork = stationNetwork;
	}
	
	public void setStationOperator ( String stationOperator ) {
		this.stationOperator = stationOperator;
	}
	
	public void setStationShefId ( String stationShefId ) {
		this.stationShefId = stationShefId;
	}
	
	public void setStationState ( String stationState ) {
		this.stationState = stationState;
	}
	
	public void setStationDataTimeZone ( Integer stationDataTimeZone ) {
		this.stationDataTimeZone = stationDataTimeZone;
	}
	
	public void setStationTriplet ( String stationTriplet ) {
		this.stationTriplet = stationTriplet;
	}
	
	public void setTSID ( String tsid ) {
		this.tsid = tsid;
	}

	/**
	 * Format the TSID string, without the datastore.
	 * @return the TSID string
	 */
	public String toString ( ) {
		return toString ( false );
	}

	/**
	 * Simple string to identify the time series catalog, for example for logging, using TSID format.
	 * @param includeDataStore whether to include the datastore at the end
	 * @return the TSID string
	 */
	public String toString ( boolean includeDataStore ) {
		return this.stationState + "-" + this.stationId + "." + this.stationNetwork + "." + getDataType() + "." + this.dataInterval;
	}
}