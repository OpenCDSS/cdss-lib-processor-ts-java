package cdss.dmi.hydrobase.rest.commands;

import java.util.ArrayList;
import java.util.List;

import cdss.dmi.hydrobase.rest.dao.Structure;
import cdss.dmi.hydrobase.rest.dao.TelemetryStation;
import cdss.dmi.hydrobase.rest.dao.WaterLevelsWells;

/**
 * Class to provide helper methods to use ColoradoHydroBaseRestDataStore.
 * May move this code into the ColoradoHydroBaseRestDataStore but trying to figure out the
 * right level of separation between 
 * @author sam
 *
 */
public class ColoradoHydroBaseRestDataStoreHelper {
	
	public ColoradoHydroBaseRestDataStoreHelper () {
		
	}
	
	// TODO smalers 2018-06-19 enable when station web services are available
	/**
	 * Return the list of station time series, suitable for display in TSTool browse area.
	 * @param dataType
	 * @param interval
	 * @param filterPanel
	 * @return
	 */
	/*public List<Station> getStationTimeSeriesCatalog ( String dataType, String interval, ColoradoHydroBaseRest_Station_InputFilter_JPanel filterPanel ) {
		List<Station> stationList = new ArrayList<Station>();
		return stationList;
	}*/
	
	// TODO smalers 2018-06-19 the following should return something like StructureTimeSeriesCatalog
	// but go with Structure for now.
	/**
	 * Return the list of structure time series, suitable for display in TSTool browse area.
	 * @param dataType
	 * @param interval
	 * @param filterPanel
	 * @return
	 */
	public List<Structure> getStructureTimeSeriesCatalog ( String dataType, String interval, ColoradoHydroBaseRest_Structure_InputFilter_JPanel filterPanel ) {
		List<Structure> structureList = new ArrayList<Structure>();
		return structureList;
	}
	
	/**
	 * Return the list of telemetry station time series, suitable for display in TSTool browse area.
	 * @param dataType
	 * @param interval
	 * @param filterPanel
	 * @return
	 */
	public List<TelemetryStation> getTelemetryStationTimeSeriesCatalog ( String dataType, String interval, ColoradoHydroBaseRest_TelemetryStation_InputFilter_JPanel filterPanel ) {
		List<TelemetryStation> telemetryStationList = new ArrayList<TelemetryStation>();
		return telemetryStationList;
	}
	
	/**
	 * Return the list of well time series, suitable for display in TSTool browse area.
	 * @param dataType
	 * @param interval
	 * @param filterPanel
	 * @return
	 */
	public List<WaterLevelsWells> getWellTimeSeriesCatalog ( String dataType, String interval, ColoradoHydroBaseRest_Well_InputFilter_JPanel filterPanel ) {
		List<WaterLevelsWells> wellList = new ArrayList<WaterLevelsWells>();
		return wellList;
	}

}