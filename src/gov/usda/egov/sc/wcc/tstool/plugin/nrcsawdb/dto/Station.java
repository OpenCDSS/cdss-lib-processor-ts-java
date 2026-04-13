
package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Class to store NRCS AWDB Station objects, for example from:
 * <pre>
[
  {
    "stationTriplet": "302:OR:SNTL",
    "stationId": "302",
    "stateCode": "OR",
    "networkCode": "SNTL",
    "name": "ANEROID LAKE #2",
    "dcoCode": "OR",
    "countyName": "Wallowa",
    "huc": "170601050101",
    "elevation": 7400,
    "latitude": 45.21328,
    "longitude": 45.21328,
    "dataTimeZone": -8,
    "pedonCode": "string",
    "shefId": "ANR03",
    "operator": "NRCS",
    "beginDate": "1980-10-01 00:00",
    "endDate": "2100-01-01",
    "forecastPoint": {
      "name": "Alamosa Ck ab Terrace Reservoir",
      "forecaster": "agoodbody",
      "exceedenceProbabilities": [
        10,
        30,
        50,
        70,
        90
      ]
    },
    "reservoirMetadata": {
      "capacity": 153000,
      "elevationAtCapacity": 350,
      "usableCapacity": 148640
    },
    "stationElements": [
      {
        "elementCode": "WTEQ",
        "ordinal": 1,
        "heightDepth": null,
        "durationName": "DAILY",
        "dataPrecision": 2,
        "storedUnitCode": "in",
        "originalUnitCode": "in",
        "beginDate": "1980-10-30 15:53",
        "endDate": "2100-01-01",
        "derivedData": false
      }
    ],
    "associatedHucs": [
      "170501041601",
      "170501070301",
      "170501070404",
      "170501041603",
      "170501041703",
      "170501041304"
    ]
  }
]
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Station {

	// Alphabetize the elements.

    /**
     * Associated HUCs.
     */
    private List<String> associatedHucs = null;

    /**
     * Begin date for data (e.g., "1980-10-01 00:00").
     */
    private String beginDate = "";

    /**
     * County name (e.g., "Wallowa").
     */
    private String countyName = "";

    /**
     * Data time zone, offset from UTC.
     */
    private Integer dataTimeZone = null;

    /**
     * DCO code (e.g., "OR").
     */
    private String dcoCode = "";

    /**
     * Elevation.
     */
    private Double elevation = null;

    /**
     * End date for data (e.g., "2100-01-01").
     */
    private String endDate = "";

    /**
     * Forecast point.
     */
    private ForecastPoint forecastPoint = null;

    /**
     * HUC 12 (e.g., "170601050101").
     */
    private String huc = "";

    /**
     * Latitude.
     */
    private Double latitude = null;

    /**
     * Longitude.
     */
    private Double longitude = null;

    /**
     * Name (e.g., "AIR TEMPERATURE AVERAGE").
     */
    private String name = "";

    /**
     * Network code (e.g., "SNTL").
     */
    private String networkCode = "";

    /**
     * Operator (e.g., "NRCS").
     */
    private String operator = "";

    /**
     * Pedon code (e.g., "string").
     */
    private String pedonCode = "";

    /**
     * Reservoir metadata.
     */
    private ReservoirMetadata reservoirMetadata = null;

    /**
     * SHEF ID (e.g., "ANR03").
     */
    private String shefId = "";
    
	/**
	 * State code (e.g., "OR").
	 */
	private String stateCode = "";

	/**
	 * Station elements.
	 */
	private List<StationElement> stationElements = null;

	/**
	 * Station triplet (e.g., "302").
	 */
	private String stationId = "";

	/**
	 * Station triplet (e.g., "302:OR:SNTL").
	 */
	private String stationTriplet = "";

    /**
     * Constructor.
     */
    public Station () {
    }

    /**
     * Gets the list of associated HUCs.
     * @return list of associated HUCs
     */
    public List<String> getAssoicatedHucs() {
        return this.associatedHucs;
    }

    /**
     * Gets the begin date as a string
     * @return begin date as a string
     */
    public String getBeginDate() {
        return this.beginDate;
    }

    /**
     * Gets the county name.
     * @return county name
     */
    public String getCountyName() {
        return this.countyName;
    }

    /**
     * Gets the data time zone
     * @return data time zone
     */
    public Integer getDataTimeZone() {
        return this.dataTimeZone;
    }

    /**
     * Gets the DCO code.
     * @return DCO code
     */
    public String getDcoCode() {
        return this.dcoCode;
    }

    /**
     * Gets the elevation.
     * @return elevation
     */
    public Double getElevation() {
        return this.elevation;
    }

    /**
     * Gets the end date as a string
     * @return end date as a string
     */
    public String getEndDate() {
        return this.endDate;
    }

    /**
     * Gets the forecast point.
     * @return forecast point
     */
    public ForecastPoint getForecastPoint() {
        return this.forecastPoint;
    }

    /**
     * Gets the latitude.
     * @return latitude
     */
    public Double getLatitude() {
        return this.latitude;
    }

    /**
     * Gets the longitude.
     * @return longitude
     */
    public Double getLongitude() {
        return this.longitude;
    }

    /**
     * Gets the HUC.
     * @return HUC
     */
    public String getHuc() {
        return this.huc;
    }

    /**
     * Gets the name.
     * @return name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Gets the network code.
     * @return network code
     */
    public String getNetworkCode() {
        return this.networkCode;
    }

    /**
     * Gets the operator.
     * @return operator
     */
    public String getOperator() {
        return this.operator;
    }

    /**
     * Gets the pedon code.
     * @return pedon code
     */
    public String getPedonCode() {
        return this.pedonCode;
    }

    /**
     * Gets the reservoir metadata.
     * @return reservoir metadata
     */
    public ReservoirMetadata getReservoirMetadata() {
        return this.reservoirMetadata;
    }

    /**
     * Gets the SHEF ID.
     * @return SHEF ID
     */
    public String getShefId() {
        return this.shefId;
    }

    /**
     * Gets the state code.
     * @return state code
     */
    public String getStateCode() {
        return this.stateCode;
    }

    /**
     * Gets the station elements.
     * @return station elements
     */
    public List<StationElement> getStationElements() {
        return this.stationElements;
    }

    /**
     * Gets the station ID.
     * @return station ID
     */
    public String getStationId() {
        return this.stationId;
    }

    /**
     * Get the list of RservoirMetadata from a list of stations.
     * @param stations the list of stations to process
     * @return the list of RservoirMetadata from a list of stations.
     * The list is guaranteed to be non-null but may be empty.
     */
    public static List<ReservoirMetadata> getReservoirMetadataList ( List<Station> stations ) {
    	List<ReservoirMetadata> reservoirMetadataList = new ArrayList<>();
    	for ( Station station : stations ) {
    		reservoirMetadataList.add(station.getReservoirMetadata());
    	}
    	return reservoirMetadataList;
    }

    /**
     * Gets the station triplet.
     * @return station triplet
     */
    public String getStationTriplet() {
        return this.stationTriplet;
    }

    /**
     * Set the list of associated HUCs
     * @param associatedHucs List of associated HUCs
     */
    public void setAssociatedHucs ( List<String> associatedHucs ) {
        this.associatedHucs = associatedHucs;
    }

    /**
     * Set the begin date as a string.
     * @param beginDate begin date
     */
    public void setBeginDate ( String beginDate ) {
        this.beginDate = beginDate;
    }

    /**
     * Set the county name.
     * @param countyName county name
     */
    public void setCountyName ( String countyName ) {
        this.countyName = countyName;
    }

    /**
     * Set the data time zone
     * @param dataTimeZone data time zone
     */
    public void setDataTimeZone ( Integer dataTimeZone ) {
        this.dataTimeZone = dataTimeZone;
    }

    /**
     * Set the DCO code.
     * @param dcoCode DCO code
     */
    public void setDcoCode ( String dcoCode ) {
        this.dcoCode = dcoCode;
    }

    /**
     * Set the elevation.
     * @param elevation elevation
     */
    public void setElevation ( Double elevation ) {
        this.elevation = elevation;
    }

    /**
     * Set the end date as a string.
     * @param endDate end date
     */
    public void setEndDate ( String endDate ) {
        this.endDate = endDate;
    }

    /**
     * Set the forecast point.
     * @param forecastPoint forecast point
     */
    public void setForecastPoint ( ForecastPoint forecastPoint ) {
        this.forecastPoint = forecastPoint;
    }

    /**
     * Set the HUC.
     * @param huc HUC
     */
    public void setHuc ( String huc ) {
        this.huc = huc;
    }

    /**
     * Set the latitude.
     * @param latitude latitude
     */
    public void setLatitude ( Double latitude ) {
        this.latitude = latitude;
    }

    /**
     * Set the longitude.
     * @param longitude longitude
     */
    public void setLongitude ( Double longitude ) {
        this.longitude = longitude;
    }

    /**
     * Set the name.
     * @param name element name
     */
    public void setName ( String name ) {
        this.name = name;
    }

    /**
     * Set the network code.
     * @param networkCode network code
     */
    public void setNetworkCode ( String networkCode ) {
        this.networkCode = networkCode;
    }

    /**
     * Set the operator.
     * @param operator operator
     */
    public void setOperator ( String operator ) {
        this.operator = operator;
    }

    /**
     * Set the pedon code.
     * @param pedonCode pedon code
     */
    public void setPedonCode ( String pedonCode ) {
        this.pedonCode = pedonCode;
    }

    /**
     * Set the reservoir metadata.
     * @param reservoirMetadata reservoir metadata
     */
    public void setReservoirMetadata ( ReservoirMetadata reservoirMetadata ) {
        this.reservoirMetadata = reservoirMetadata;
    }

    /**
     * Set the SHEF ID.
     * @param shefId SHEF ID
     */
    public void setShefId ( String shefId ) {
        this.shefId = shefId;
    }

    /**
     * Set the state code.
     * @param stateCode state code
     */
    public void setStateCode ( String stateCode ) {
        this.stateCode = stateCode;
    }

    /**
     * Set the station elements.
     * @param stationElements station elements
     */
    public void setStationElements ( List<StationElement> stationElements ) {
        this.stationElements = stationElements;
    }

    /**
     * Set the station ID.
     * @param stationId station ID
     */
    public void setStationId ( String stationId ) {
        this.stationId = stationId;
    }

    /**
     * Set the station triplet.
     * @param stationTriplet station triplet
     */
    public void setStationTriplet ( String stationTriplet ) {
        this.stationTriplet = stationTriplet;
    }

}