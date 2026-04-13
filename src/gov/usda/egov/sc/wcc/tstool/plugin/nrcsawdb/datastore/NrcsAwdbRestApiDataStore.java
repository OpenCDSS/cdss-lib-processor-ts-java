// NrcsAwdbRestApiDataStore - class that implements the NrcsAwdbRestApiDataStore plugin datastore

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

package gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.datastore;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import RTi.TS.TS;
import RTi.TS.TSDataFlagMetadata;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JWorksheet_AbstractExcelCellRenderer;
import RTi.Util.GUI.JWorksheet_AbstractRowTableModel;
import RTi.Util.IO.PropList;
import RTi.Util.IO.RequirementCheck;
import RTi.Util.IO.UrlReader;
import RTi.Util.IO.UrlResponse;
import RTi.Util.Message.Message;
import RTi.Util.String.MultiKeyStringDictionary;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.YearType;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.CentralTendencyType;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.Data;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.DataValue;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.Duration;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.DurationType;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.DurationsResponse;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.Element;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.ElementComparator;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.ElementsResponse;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.Forecast;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.ForecastComparator;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.ForecastData;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.ForecastPeriod;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.ForecastPeriodComparator;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.ForecastPeriodsResponse;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.Network;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.NetworkComparator;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.NetworksResponse;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.PeriodRefType;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.ReservoirMetadata;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.Station;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.StationComparator;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.StationData;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.StationElement;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.dto.TimeSeriesCatalog;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.ui.NrcsAwdbRestApi_TimeSeries_CellRenderer;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.ui.NrcsAwdbRestApi_TimeSeries_InputFilter_JPanel;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.ui.NrcsAwdbRestApi_TimeSeries_TableModel;
import gov.usda.egov.sc.wcc.tstool.plugin.nrcsawdb.util.WebUtil;
import riverside.datastore.AbstractWebServiceDataStore;
import riverside.datastore.DataStoreRequirementChecker;

public class NrcsAwdbRestApiDataStore extends AbstractWebServiceDataStore implements DataStoreRequirementChecker { // , PluginDataStore { } // Implement plugin later>

	/**
	 * Properties for the plugin, used to help with application integration.
	 * TODO smalers 2026-03-29 enable later if migrated to a full plugin.
	 */
	//private Map<String,Object> pluginProperties = new LinkedHashMap<>();

	/**
	 * Duration list:
	 * - use getDurationList() to use lazy loading
	 */
	private List<Duration> durationCache = new ArrayList<>();

	/**
	 * Element list:
	 * - use getElementList() to use lazy loading
	 */
	private List<Element> elementCache = new ArrayList<>();

	/**
	 * Forecast period list:
	 * - use getForecastPeriodLoad() to use lazy loading
	 */
	private List<ForecastPeriod> forecastPeriodCache = new ArrayList<>();

	/**
	 * Network list:
	 * - use getNetworkList() to use lazy loading
	 */
	private List<Network> networkCache = new ArrayList<>();

	/**
	 * TimeSeriesCatalog list:
	 * - curently is not cached but may add in the future
	 */
	private List<TimeSeriesCatalog> tscatalogCache = new ArrayList<>();

	/**
	 * Global debug option for datastore, used for development and troubleshooting.
	 * See the 'Debug' datastore configuration file property.
	 */
	private boolean debug = false;

	/**
	Constructor for web service.
	@param name identifier for the data store (will be used in commands)
	@param description name for the data store
	@param serviceRootUrl the service root URL to which specific requests will be appended, must have a trailing slash
	@param props properties to configure the datastore:
	<ul>
	<li> `Debug` - to enable debugging</li>
	<li> `Description` - description, longer than name</li>
	<li> `Enabled` - standard datastore property, indicated whether it is enabled</li>
	<li> `Name` - name of the datastore, same as name</li>
	<li> `ServiceApiDocumentationUrl` - URL for the API documentation landing page</li>
	<li> `ServiceRootUrl` - the URL for the web service API, for example "ServiceRootUrl = "https://iot.campbell-cloud.com/api/v1/"</li>
	<li> `Type` - must be `NrcsAwdbRestApiDataStore`</li>
	</ul>
	*/
	public NrcsAwdbRestApiDataStore ( String name, String description, URI serviceRootUrl, PropList props ) {
		String routine = getClass().getSimpleName() + ".NrcsAwdbRestApiDataStore";

		String prop = props.getValue("Debug");
		if ( (prop != null) && prop.equalsIgnoreCase("true") ) {
			Message.printStatus(2, routine, "Datastore \"" + name + "\" - detected Debug=true");
			this.debug = true;
		}
	    setName ( name );
	    setDescription ( description );
	    // Make sure that the URL ends in a slash.
	    if ( serviceRootUrl.toString().endsWith("/") ) {
	    	setServiceRootURI ( serviceRootUrl );
	    }
	    else {
	    	try {
	    		setServiceRootURI ( new URI(serviceRootUrl.toString() + "/") );
	    	}
	    	catch ( URISyntaxException e ) {
	    		// Should not happen here because it would have happened before.
	    		Message.printWarning(3, routine, "Error setting URL with trailing /.");
	    	}
	    }
	    setProperties ( props );

	    // Set standard plugin properties:
        // - plugin properties can be listed in the main TSTool interface
        // - version is used to create a versioned installer and documentation.
	    /*
        this.pluginProperties.put("Name", "NRCS AWDB REST API web services plugin");
        this.pluginProperties.put("Description", "Plugin to integrate TSTool with NRCS AWDB REST API web services.");
        this.pluginProperties.put("Author", "Open Water Foundation, https://openwaterfoundation.org");
        this.pluginProperties.put("Version", PluginMeta.VERSION);
        // Set the location of the Jar file, which is useful to confirm what was loaded.
        try {
        	this.pluginProperties.put("JarFile",
        		this.getClass()
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
                .getPath() );
        }
        catch ( Exception e ) {
        	// Backup in case the above does not work.
            String classFile = this.getName().replace('.', '/') + ".class";
            String resource = this.getClass().getClassLoader().getResource(classFile).toString();
        	this.pluginProperties.put("JarFile",resource);
        }
        */

	    // Read global data used throughout the session:
	    // - in particular a cache of the TimeSeriesCatalog used for further queries

	    readGlobalData();
	}

	/**
 	* Check the database requirement for DataStoreRequirementChecker interface, for example one of:
 	* <pre>
 	* @require datastore NrcsAwdb version >= 1.5.5
 	* @require datastore NrcsAwedb ?configproperty propname? == Something
 	*
 	* @enabledif datastore nrcsawdb version >= 1.5.5
 	* </pre>
 	* @param check a RequirementCheck object that has been initialized with the check text and
 	* will be updated in this method.
 	* @return whether the requirement condition is met, from call to check.isRequirementMet()
 	*/
	public boolean checkRequirement ( RequirementCheck check ) {
		String routine = getClass().getSimpleName() + ".checkRequirement";
		// Parse the string into parts:
		// - calling code has already interpreted the first 3 parts to be able to do this call
		String requirement = check.getRequirementText();
		Message.printStatus(2, routine, "Checking requirement: " + requirement);
		// Get the annotation that is being checked, so messages are appropriate.
		String annotation = check.getAnnotation();
		String [] requireParts = requirement.split(" ");
		// Datastore name may be an original name but a substitute is used, via TSTool command line.
		String dsName = requireParts[2];
		String dsNameNote = ""; // Note to add on messages to help confirm how substitutions are being handled.
		String checkerName = "CloudFrontDataStore";
		if ( !dsName.equals(this.getName())) {
			// A substitute datastore name is being used, such as in testing.
			dsNameNote = "\nCommand file datastore name '" + dsName + "' substitute that is actually used is '" + this.getName() + "'";
		}
		if ( requireParts.length < 4 ) {
			check.setIsRequirementMet(checkerName, false, "Requirement does not contain check type as one of: version, configuration, "
				+ "for example: " + annotation + " datastore nsdataws-mhfd version...");
			return check.isRequirementMet();
		}
		String checkType = requireParts[3];
		if ( checkType.equalsIgnoreCase("configuration") ) {
			// Checking requirement of form:
			// 0        1         2             3             4         5  6
			// @require datastore nsdataws-mhfd configuration system_id == CO-District-MHFD
			String propertyName = requireParts[4];
			String operator = requireParts[5];
			String checkValue = requireParts[6];
			// Get the configuration table property of interest:
			// - currently only support checking system_id
			if ( propertyName.equals("system_id") ) {
				// Know how to handle "system_id" property.
				if ( (checkValue == null) || checkValue.isEmpty() ) {
					// Unable to do check.
					check.setIsRequirementMet ( checkerName, false, "'system_id' value to check is not specified in the requirement." + dsNameNote );
					return check.isRequirementMet();
				}
				else {
					// TODO smalers 2026-04-13 need to evaluate whether NRCS AWDB has configuration properties.
					//String propertyValue = readConfigurationProperty(propertyName);
					String propertyValue = "";
					if ( (propertyValue == null) || propertyValue.isEmpty() ) {
						// Unable to do check.
						check.setIsRequirementMet ( checkerName, false, "NRCS AWDB REST API configuration 'system_id' value is not defined in the database." + dsNameNote );
						return check.isRequirementMet();
					}
					else {
						if ( StringUtil.compareUsingOperator(propertyValue, operator, checkValue) ) {
							check.setIsRequirementMet ( checkerName, true, "NRCS AWDB REST API configuration property '" + propertyName + "' value (" + propertyValue +
								") does meet the requirement: " + operator + " " + checkValue + dsNameNote );
						}
						else {
							check.setIsRequirementMet ( checkerName, false, "NRCS AWDB REST API configuration property '" + propertyName + "' value (" + propertyValue +
								") does not meet the requirement:" + operator + " " + checkValue + dsNameNote );
						}
						return check.isRequirementMet();
					}
				}
			}
			else {
				// Other properties may not be easy to compare.  Probably need to use "contains" and other operators.
				check.setIsRequirementMet ( checkerName, false, "Check type '" + checkType + "' configuration property '" + propertyName + "' is not supported.");
				return check.isRequirementMet();
			}
		}
		/* TODO smalers 2021-07-29 need to implement, maybe need to define the system ID in the configuration file as a cross check for testing.
		else if ( checkType.equalsIgnoreCase("configproperty") ) {
			if ( parts.length < 7 ) {
				// 'property' requires 7 parts
				throw new RuntimeException( "'configproperty' requirement does not contain at least 7 parts for: " + requirement);
			}
		}
		*/
		else if ( checkType.equalsIgnoreCase("version") ) {
			// Checking requirement of form:
			// 0        1         2             3       4  5
			// @require datastore nsdataws-mhfd version >= 1.5.5
			Message.printStatus(2, routine, "Checking web service version.");
			// Do a web service round trip to check version since it may change with software updates.
			String wsVersion = readVersion();
			if ( (wsVersion == null) || wsVersion.isEmpty() ) {
				// Unable to do check.
				check.setIsRequirementMet ( checkerName, false, "Web service version is unknown (services are down or software problem).");
				return check.isRequirementMet();
			}
			else {
				// Web service versions are strings of format A.B.C.D so can do semantic version comparison:
				// - only compare the first 3 parts
				//Message.printStatus(2, "checkRequirement", "Comparing " + wsVersion + " " + operator + " " + checkValue);
				String operator = requireParts[4];
				String checkValue = requireParts[5];
				boolean verCheck = StringUtil.compareSemanticVersions(wsVersion, operator, checkValue, 3);
				String message = "";
				if ( !verCheck ) {
					message = annotation + " web service version (" + wsVersion + ") does not meet requirement: " + operator + " " + checkValue+dsNameNote;
					check.setIsRequirementMet ( checkerName, verCheck, message );
				}
				else {
					message = annotation + " web service version (" + wsVersion + ") does meet requirement: " + operator + " " + checkValue+dsNameNote;
					check.setIsRequirementMet ( checkerName, verCheck, message );
				}
				return check.isRequirementMet();
			}
		}
		else {
			// Unknown check type.
			check.setIsRequirementMet ( checkerName, false, "Requirement check type '" + checkType + "' is unknown.");
			return check.isRequirementMet();
		}

	}

	/**
	 * Create a time series input filter, used to initialize user interfaces.
	 * @return a time series input filter for NRCS AWDB time series catalog queries
	 */
	public InputFilter_JPanel createTimeSeriesListInputFilterPanel () {
		NrcsAwdbRestApi_TimeSeries_InputFilter_JPanel ifp = new NrcsAwdbRestApi_TimeSeries_InputFilter_JPanel(this, 4);
		return ifp;
	}

	/**
	 * Create a time series list table model given the desired data type, time step (interval), and input filter.
	 * The datastore performs a suitable query and creates objects to manage in the time series list.
	 * @param dataType time series data type to query, controlled by the datastore
	 * @param timeStep time interval to query, controlled by the datastore
	 * @param ifp input filter panel that provides additional filter options
	 * @return a TableModel containing the defined columns and rows.
	 */
	@SuppressWarnings("rawtypes")
	public JWorksheet_AbstractRowTableModel createTimeSeriesListTableModel(String dataType, String timeStep, InputFilter_JPanel ifp ) {
		// First query the database for the specified input.
		List<TimeSeriesCatalog> tsmetaList = readTimeSeriesMeta ( dataType, timeStep, ifp );
		return getTimeSeriesListTableModel(tsmetaList);
	}

	/**
	Format a date/time string for use with the web service calls.
	@return the formatted date/time or null if requested period is null.
	@param dt DateTime object to format
	@param interval interval of time series, which by default controls date/time formatting
	@param formatToSecond if true always format to the second
	@param endPos if 0 the date/time is at the start date/time and if necessary additional information will be added
	to match the start of the month; if 1, add to match the end of the month
	*/
	private String formatDateTime ( DateTime dt, TimeInterval interval, boolean formatToSecond, int endPos ) {
	    if ( dt == null ) {
	        return null;
	    }
	    /*
	    int intervalBase = interval.getBase();
	    if ( formatToSecond ) {
	        if ( endPos == 1 ) {
	            // Make sure day is set to number of days in month
	            dt = (DateTime)dt.clone();
	            if ( intervalBase == TimeInterval.YEAR ) {
	                dt.setMonth(12);
	            }
	            if ( (intervalBase == TimeInterval.YEAR) || (intervalBase == TimeInterval.MONTH) ) {
	                dt.setDay(TimeUtil.numDaysInMonth(dt));
	            }
	        }
	        return dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm_SS);
	    }
	    else if ( intervalBase == TimeInterval.YEAR ) {
	        return dt.toString(DateTime.FORMAT_YYYY);
	    }
	    else if ( intervalBase == TimeInterval.MONTH ) {
	        return dt.toString(DateTime.FORMAT_YYYY_MM);
	    }
	    else if ( intervalBase == TimeInterval.DAY ) {
	        return dt.toString(DateTime.FORMAT_YYYY_MM_DD);
	    }
	    else if ( intervalBase == TimeInterval.HOUR ) {
	        return dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH);
	    }
	    else if ( intervalBase == TimeInterval.IRREGULAR ) {
	        return dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm);
	    }
	    else {
	        return null;
	    }
	    */
	    // For REST API the format is 'yyyy-MM-dd HH:mm' regardless of the interval.
        return dt.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm);
	}

	/**
	Return the unique list of data interval strings available for a data type,
	returning values that are consistent with TSTool ("Day", rather than "daily").
	@param dataType TSTool data type string (currently ignored)
	*/
	public List<String> getDataIntervalStringsForDataType ( String dataType ) {
    	List<String> dataIntervalStrings = new ArrayList<>();
    	// For now hard-code until know more about how the API works.
    	dataIntervalStrings.add("Hour"); // NRCS "HOURLY".
    	dataIntervalStrings.add("Day"); // NRCS "DAILY".
    	// NRCS "SEMIMONTHLY" is not supported.
    	dataIntervalStrings.add("Month"); // NRCS "MONTHLY".
    	dataIntervalStrings.add("Year"); // NRCS "CALENDAR_YEAR".
    	// NRCS "WATER_YEAR" is not supported, but may handle in the read command.
    	return dataIntervalStrings;
	}

	/**
	Return the list of data types that are available.
	Currently this returns "elementCode - elementName".
	@param includeName whether to include the name
	@param includeInterval whether to include the interval as (daily), etc.
	*/
	public List<String> getDataTypeStrings ( boolean includeName, boolean includeInterval ) {
		List<String> dataTypeStrings = new ArrayList<>();
		try {
			dataTypeStrings = getElementStrings ( true );
		}
		catch ( Exception e ) {
			String routine = getClass().getSimpleName() + ".getDataTypeStrings";
			Message.printWarning(3, routine, "Error getting data type strings (" + e + ").");
		}
		return dataTypeStrings;
	}

	/**
	 * Return the cached duration list.  Read the data if not previously read.
	 * Return the duration list.
	 * @exception IOException if there is an error
	 */
	public List<Duration> getDurationList () throws IOException {
		if ( (this.durationCache == null) || this.durationCache.isEmpty() ) {
			this.durationCache = readDurationList();
		}
		return this.durationCache;
	}

	/**
	 * Return the cached element list.  Read the data if not previously read.
	 * Return the element list.
	 * @exception IOException if there is an error
	 */
	public List<Element> getElementList () throws IOException {
		if ( (this.elementCache == null) || this.elementCache.isEmpty() ) {
			this.elementCache = readElementList();
		}
		return this.elementCache;
	}

	/**
	Return the list of elements that are available.  This returns the element code and optionally the name.
	Duplicates in the table are ignored.
	@param includeName whether to include the description (use " - " as separator).
	*/
	public List<String> getElementStrings ( boolean includeName ) {
    	String routine = getClass().getSimpleName() + ".getElementStrings";
	    List<String> elementStrings = new ArrayList<>();
	    try {
	    	List<Element> elements = getElementList();
	    	if ( elements != null ) {
	    		for ( Element element: elements ) {
        			if ( includeName ) {
            			elementStrings.add( "" + element.getCode() + " - " + element.getName() );
        			}
        			else {
            			elementStrings.add( "" + element.getCode() );
        			}
    			}
	    	}
	    	else {
	    		Message.printStatus(2, routine, "The element list is null.");
	    	}
   			Message.printStatus(2, routine, "Have " + elementStrings.size() + " element strings.");
	    }
	    catch ( Exception e ) {
	    	// Will return an empty list below.
	    	Message.printWarning(3, routine, "Error gettign element strings.");
	    }
    	return elementStrings;
	}

	/**
	 * Return the cached forecast period list.  Read the data if not previously read.
	 * Return the forecast period list.
	 * @exception IOException if there is an error
	 */
	public List<ForecastPeriod> getForecastPeriodList () throws IOException {
		if ( (this.forecastPeriodCache == null) || this.forecastPeriodCache.isEmpty() ) {
			this.forecastPeriodCache = readForecastPeriodList();
		}
		return this.forecastPeriodCache;
	}

	/**
	Return the list of forecast periods that are available.
	The list is sorted by name.
	@return forecast period strings
	*/
	public List<String> getForecastPeriodStrings () {
		String routine = getClass().getSimpleName() + ".getForeastPeriodStrings";
	    List<String> forecastPeriodStrings = new ArrayList<>();
	    try {
	    	List<ForecastPeriod> forecastPeriods = getForecastPeriodList();
    		if ( forecastPeriods != null ) {
    			for ( ForecastPeriod forecastPeriod: forecastPeriods ) {
        			forecastPeriodStrings.add( "" + forecastPeriod.getName() );
    			}
    			Collections.sort(forecastPeriodStrings,String.CASE_INSENSITIVE_ORDER);
    			Message.printStatus(2, routine, "Have " + forecastPeriodStrings.size() + " forecast period strings.");
    		}
    		else {
    			Message.printStatus(2, routine, "Have null forecast periods.");
    		}
	    }
	    catch ( Exception e ) {
	    	// Will return an empty list below.
	    	Message.printWarning(3, routine, "Error getting forecast period strings (" + e + ").");
	    }
    	return forecastPeriodStrings;
	}

	/**
	 * Return the cached network list.  Read the data if not previously read.
	 * Return the network list.
	 * @exception IOException if there is an error
	 */
	public List<Network> getNetworkList () throws IOException {
		if ( (this.networkCache == null) || this.networkCache.isEmpty() ) {
			this.networkCache = readNetworkList();
		}
		return this.networkCache;
	}

	/**
	Return the list of network that are available.  This returns the network code and optionally
	the description.  Duplicates in the table are ignored.
	@param includeDesc whether to include the description (use " - " as separator).
	*/
	public List<String> getNetworkStrings ( boolean includeDesc ) {
		String routine = getClass().getSimpleName() + ".getNetworkStrings";
	    List<String> networkStrings = new ArrayList<>();
	    try {
	    	List<Network> networks = getNetworkList();
    		if ( networks != null ) {
    			for ( Network network: networks ) {
        			if ( includeDesc ) {
            			networkStrings.add( "" + network.getCode() + " - " + network.getDescription() );
        			}
        			else {
            			networkStrings.add( "" + network.getCode() );
        			}
    			}
    		}
    		else {
    			Message.printStatus(2, routine, "Have null network list.");
    		}
   			Message.printStatus(2, routine, "Have " + networkStrings.size() + " network strings.");
	    }
	    catch ( Exception e ) {
	    	// Empty list will be returned below.
	    	Message.printWarning(3, routine, "Error getting network list (" + e + ").");
	    }
    	return networkStrings;
	}

	/**
 	* Get the properties for the plugin.
 	* A copy of the properties map is returned so that calling code cannot change the properties for the plugin.
 	* @return plugin properties map.
 	*/
	/*
	public Map<String,Object> getPluginProperties () {
		Map<String,Object> pluginProperties = new LinkedHashMap<>();
		// For now the properties are all strings so it is easy to copy.
    	for (Map.Entry<String, Object> entry : this.pluginProperties.entrySet()) {
        	pluginProperties.put(entry.getKey(),
                    	entry.getValue());
    	}
		return pluginProperties;
	}
	*/

	/**
	 * Return the list of cached Station.
	 * @return the list of cached Station
	 */
	/*
	public List<Station> getStationList () {
		return this.stationList;
	}
	*/

	/**
	 * Return the list of time series catalog.
	 * @param readData if false, return the global cached data, if true read the data and reset in the cache
	 */
	public List<TimeSeriesCatalog> getTimeSeriesCatalog ( boolean readData ) {
		if ( readData ) {
			String dataTypeReq = null;
			String dataIntervalReq = null;
    		InputFilter_JPanel ifp = null;
			this.tscatalogCache = readTimeSeriesCatalogList ( dataTypeReq, dataIntervalReq, ifp );
		}
		return this.tscatalogCache;
	}

	/**
 	* Return the comments for a time series in the table model.
 	* The comments are added as commands prior to the TSID comment.
 	* @param tableModel the table model from which to extract data
 	* @param row the displayed table row, may have been sorted
 	* @return a list of comments (return null or an empty list to not add comments to commands).
 	* A single comment is returned.
 	*/
	public List<String> getTimeSeriesCommentsFromTableModel ( @SuppressWarnings("rawtypes") JWorksheet_AbstractRowTableModel tableModel, int row ) {
    	NrcsAwdbRestApi_TimeSeries_TableModel tm = (NrcsAwdbRestApi_TimeSeries_TableModel)tableModel;
    	// Should not have any nulls.
    	List<String> comments = new ArrayList<>();
    	StringBuilder commentBuilder = new StringBuilder();
    	String stationName = (String)tableModel.getValueAt(row,tm.COL_STATION_NAME);
    	commentBuilder.append(stationName);
    	comments.add(commentBuilder.toString() );
    	return comments;
	}

	/**
	 * This version is required by TSTool UI.
	 * Return the list of time series data interval strings.
	 * @param dataType data type string to filter the list of data intervals.
	 * If null, blank, or "*" the data type is not considered when determining the list of data intervals.
	 */
	public List<String> getTimeSeriesDataIntervalStrings(String dataType) {
		boolean includeWildcards = true;
		return getTimeSeriesDataIntervalStrings(dataType, includeWildcards);
	}

	/**
	 * This version is required by TSTool UI.
	 * Return the list of time series data interval strings.
	 * Interval strings match TSTool conventions such as NewTimeSeries command, which uses "1Hour" rather than "1hour".
	 * This should result from calls like:  TimeInterval.getName(TimeInterval.HOUR, 0)
	 * @param dataType data type string to filter the list of data intervals.
	 * If null, blank, or "*" the data type is not considered when determining the list of data intervals.
	 * @includeWildcards if true, include "*" wildcard.
	 */
	public List<String> getTimeSeriesDataIntervalStrings(String dataType, boolean includeWildcards ) {
		String routine = getClass().getSimpleName() + ".getTimeSeriesDataIntervalStrings";
		List<String> dataIntervals = new ArrayList<>();
		Message.printStatus(2, routine, "Getting interval strings for data type \"" + dataType + "\"");

		// Only check datatype if not a wildcard.
		boolean doCheckDataType = false;
		if ( (dataType != null) && !dataType.isEmpty() && !dataType.equals("*") ) {
			doCheckDataType = true;
			if ( dataType.contains(" - ") ) {
				// Remove the trailing count:
				//   Datatupe - Count
				int pos = dataType.indexOf(" - ");
				if ( pos > 0 ) {
					dataType = dataType.substring(0,pos);
				}
			}
		}

		// Use the cached time series catalog read at startup.
		List<TimeSeriesCatalog> tscatalogList = getTimeSeriesCatalog(false);
		Message.printStatus(2, routine, "  Have " + tscatalogList.size() + " cached time series from the catalog.");
		for ( TimeSeriesCatalog tscatalog : tscatalogList ) {
			if ( doCheckDataType ) {
				// Only check the first part of the data type, which is the 'stationparameter_no'.
				if ( !dataType.equals(tscatalog.getDataType())) {
					// Data type does not match 'stationparameter_no'.
					continue;
				}
			}
			// Only add the interval if not already in the list.
			if ( !StringUtil.isInList(dataIntervals, tscatalog.getDataInterval())) {
				dataIntervals.add(tscatalog.getDataInterval());
			}
		}

		// Sort the intervals:
		// - TODO smalers need to sort by time
		Collections.sort(dataIntervals,String.CASE_INSENSITIVE_ORDER);

		if ( includeWildcards ) {
			// Always allow querying list of time series for all intervals:
			// - always add so that people can get a full list
			// - adding at top makes it easy to explore data without having to scroll to the end

			dataIntervals.add("*");
			if ( dataIntervals.size() > 1 ) {
				// Also add at the beginning to simplify selections:
				// - could check for a small number like 5 but there should always be a few
				dataIntervals.add(0,"*");
			}
		}

		return dataIntervals;
	}

	/**
	 * Return the list of time series data type strings.
	 * This is the version that is required by TSTool UI.
	 * These strings are the same as the datastream field.
	 * @param dataInterval data interval from TimeInterval.getName(TimeInterval.HOUR,0) to filter the list of data types.
	 * If null, blank, or "*" the interval is not considered when determining the list of data types (treat as if "*").
	 */
	public List<String> getTimeSeriesDataTypeStrings(String dataInterval) {
		boolean includeWildcards = true;
		return getTimeSeriesDataTypeStrings(dataInterval, includeWildcards );
	}

	/**
	 * Return the list of time series data type strings.
	 * These strings are the same as the datastream field.
	 */
	public List<String> getTimeSeriesDataTypeStrings(String dataInterval, boolean includeWildcards ) {
		//String routine = getClass().getSimpleName() + ".getTimeSeriesDataTypeStrings";
		// Map to count how many time series for each daa type.
		Map<String,Integer> countMap = new HashMap<>();

		List<String> dataTypes = new ArrayList<>();

		// Get the cached list of time series catalog objects.
		List<TimeSeriesCatalog> tscatalogList = getTimeSeriesCatalog(false);

		// Create the data type list:
		// - use the global TimeSeriesCatalog to get the data type.
		boolean found = false;
		if ( tscatalogList != null ) {
			for ( TimeSeriesCatalog tscatalog : tscatalogList ) {
				String tscatalogDataType = tscatalog.getDataType();
				// Update the count.
				Integer count = countMap.get(tscatalogDataType);
				if ( count == null ) {
					count = Integer.valueOf(1);
				}
				else {
					// Increment the count.
					count = Integer.valueOf(count + 1);
				}
				countMap.put(tscatalogDataType, count);
				found = false;
				for ( String dataType : dataTypes ) {
					//if ( stationParameterName.equals(dataType) ) {
					if ( tscatalogDataType.equals(dataType) ) {
						found = true;
						break;
					}
				}
				if ( !found ) {
					//Message.printStatus(2, routine, "Adding data type \"" + tscatalogDataType + "\"");
					dataTypes.add(tscatalogDataType);
				}
			}
		}

		// Add the count to the data types.
		boolean includeCount = true;
		if ( includeCount ) {
			int i = -1;
			for ( String dataType : dataTypes ) {
				++i;
				Integer count = countMap.get(dataType);
				if ( count == null ) {
					dataType += " - 0";
				}
				else {
					dataType += " - " + count;
				}
				dataTypes.set(i, dataType);
			}
		}

		// Sort the names.
		Collections.sort(dataTypes, String.CASE_INSENSITIVE_ORDER);

		if ( includeWildcards ) {
			// Add wildcard at the front and end - allows querying all data types for the location:
			// - always add so that people can get a full list
			// - adding at the top makes it easy to explore data without having to scroll to the end

			dataTypes.add("*");
			dataTypes.add(0,"*");
		}

		return dataTypes;
	}

	/**
 	* Return the identifier for a time series in the table model.
 	* The TSIdent parts will be uses as TSID commands.
 	* @param tableModel the table model from which to extract data
 	* @param row the displayed table row, may have been sorted
 	* @return a time series identifier that can be used for a TSID command
 	*/
	public TSIdent getTimeSeriesIdentifierFromTableModel( @SuppressWarnings("rawtypes") JWorksheet_AbstractRowTableModel tableModel, int row ) {
		//String routine = getClass().getSimpleName() + ".getTimeSeriesIdentifierFromTableModel";
    	NrcsAwdbRestApi_TimeSeries_TableModel tm = (NrcsAwdbRestApi_TimeSeries_TableModel)tableModel;
    	// Should not have any nulls.
    	//String locId = (String)tableModel.getValueAt(row,tm.COL_LOCATION_ID);
    	String source = (String)tableModel.getValueAt(row,tm.COL_DATA_SOURCE); // Default, may add agency or organization later.
    	String dataType = (String)tableModel.getValueAt(row,tm.COL_DATA_TYPE);
    	String interval = (String)tableModel.getValueAt(row,tm.COL_DATA_INTERVAL);
    	String scenario = "";
    	String inputName = ""; // Only used for files.
    	TSIdent tsid = null;
    	boolean useTsid = false;
		String datastoreName = this.getName();
		String locId = "";
    	if ( useTsid ) {
    		// Use the LocType and ts_id.
   			locId = "ts_id:" + tableModel.getValueAt(row,tm.COL_TSID);
    	}
    	else {
    		// Use the station ID for the location.
   			locId = "" + tableModel.getValueAt(row,tm.COL_STATION_ID);
    	}
    	try {
    		tsid = new TSIdent(locId, source, dataType, interval, scenario, datastoreName, inputName );
    	}
    	catch ( Exception e ) {
    		throw new RuntimeException ( e );
    	}
    	return tsid;
	}

    /**
     * Get the CellRenderer used for displaying the time series in a TableModel.
     */
    @SuppressWarnings("rawtypes")
	public JWorksheet_AbstractExcelCellRenderer getTimeSeriesListCellRenderer(JWorksheet_AbstractRowTableModel tableModel) {
    	return new NrcsAwdbRestApi_TimeSeries_CellRenderer ((NrcsAwdbRestApi_TimeSeries_TableModel)tableModel);
    }

    /**
     * Get the TableModel used for displaying the time series.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public JWorksheet_AbstractRowTableModel getTimeSeriesListTableModel(List<? extends Object> data) {
    	return new NrcsAwdbRestApi_TimeSeries_TableModel(this,(List<TimeSeriesCatalog>)data);
    }

	/**
	Lookup the AWDB duration from the internal TS interval.
	@param internal data interval to read
	@param outputYearType the output year type, for year interval data
	*/
	private DurationType lookupDurationFromInterval ( TimeInterval interval, YearType outputYearType ) {
	    if ( interval.getBase() == TimeInterval.YEAR ) {
	    	if ( outputYearType == null ) {
	    		// Default is calendar year.
	    		return DurationType.CALENDAR_YEAR;
	    	}
	    	else if ( outputYearType == YearType.CALENDAR ) {
	    		return DurationType.CALENDAR_YEAR;
	    	}
	    	else if ( outputYearType == YearType.WATER ) {
	    		return DurationType.WATER_YEAR;
	    	}
	    	else {
	    		// Don't know how to handle.
	    		return null;
	    	}
	    }
	    else if ( interval.getBase() == TimeInterval.MONTH ) {
	        return DurationType.MONTHLY;
	    }
	    //else if ( interval.getBase() == TimeInterval.?? ) {
	        //return DurationType.SEMI_MONTHLY";
	    //}
	    else if ( interval.getBase() == TimeInterval.DAY ) {
	        return DurationType.DAILY;
	    }
	    else if ( interval.getBase() == TimeInterval.HOUR ) {
	        return DurationType.HOURLY;
	    }
	    else {
	        return null;
	    }
	}

	/**
	Lookup the TimeInterval from the AWDB duration.
	@param duration NRCS AWDB duration to look up
	@return the TimeInterval matching the duration
	*/
	private int lookupIntervalFromDuration ( Duration duration ) {
	    if ( duration.getCode().equals("Y") ) {
	    	// Old Duration.ANNUAL
	        return TimeInterval.YEAR;
	    }
	    else if ( duration.getCode().equals("M") ) {
	    	// Old Duration.MONTHLY
	        return TimeInterval.MONTH;
	    }
	    else if ( duration.getCode().equals("D") ) {
	    	// Old Duration.DAILY
	        return TimeInterval.DAY;
	    }
	    else if ( duration.getCode().equals("I") ) {
	        // Old Duration.INSTANTANEOUS
	        return TimeInterval.IRREGULAR;
	    }
	    else {
	        return TimeInterval.UNKNOWN;
	    }
	}

	/**
	Parse the network code from the station triplet ("StationID:State:Network")
	@param stationTriplet the station triplet
	@return the network code from the station triplet
	*/
	private String parseNetworkCodeFromTriplet ( String stationTriplet ) {
    	String [] parts = stationTriplet.split(":");
    	return parts[2];
	}

	/**
	Parse the state abbreviation from the station triplet ("StationID:State:Network")
	@param stationTriplet the station triplet
	@return the state abbreviation from the station triplet
	*/
	private String parseStateFromTriplet ( String stationTriplet ) {
	    String [] parts = stationTriplet.split(":");
	    return parts[1];
	}

	/**
	Parse an AWDB date/time string and return a DateTime instance.
	If the date/time starts with 2100-01-01 (to indicate active station), replace with the current date
	@param s date/time string in format YYYY-MM-DD hh:mm
	*/
	private DateTime parseDateTime ( String s ) {
    	if ( s.startsWith("2100-01-01") ) {
        	// Value of 2100-01-01 00:00:00 is returned for end date for some web service methods.
        	// Since this value does not make sense for historical data, replace with the current date/time.
        	DateTime d = new DateTime(DateTime.DATE_CURRENT); // Any issues with time zone here?
        	s = d.toString(DateTime.FORMAT_YYYY_MM_DD) + s.substring(10);
    	}
    	return DateTime.parse(s);
	}

	/**
	Parse the station ID from the station triplet ("StationID:State:Network")
	@param stationTriplet the station triplet
	@return the station ID from the station triplet
	*/
	private String parseStationIDFromTriplet ( String stationTriplet ) {
    	String [] parts = stationTriplet.split(":");
    	return parts[0];
	}

	/**
	 * Indicate whether the datastore provides a time series input filter.
	 * This datastore does provide an input filter panel.
	 * @return true because the datastore provides a time series list input filter
	 */
	public boolean providesTimeSeriesListInputFilterPanel () {
		return true;
	}

	/**
 	* Read the 'data' list objects from the "/data" service.
 	* The JSON looks like the following:
 	* <pre>
 	*
    ...
 }
 	* </pre>
 	* @param duration the duration type for time series
 	* @param elements the elements to query (required)
 	* @param stationTriplets a list of station triplets to read (required)
 	* @param beginDate starting date/time to read
 	* @param endDate ending date/time to read
 	* @param centralTendencyType the central tendency type to read
 	* @param returnFlags whether to return the flags for the data values
 	* @param returnOrigionalValues whether to return the original values (original data units?)
 	* @param returnSuspectData whether to return suspect data
 	* @return the list of data objects
 	*/
	private List<StationData> readDataList (
		// List the query parameters for filters, alphabetical.
		DurationType duration,
		List<String> elements,
		List<String> stationTriplets,
		// Period.
		DateTime beginDate,
		DateTime endDate,
		DateTime insertOrUpdateBeginDate,
		PeriodRefType periodRefType,
		// Indicate which data objects to include.
		CentralTendencyType centralTendencyType,
		Boolean returnFlags,
		Boolean returnOriginalValues,
		Boolean returnSuspectData
		) throws IOException {
		String routine = getClass().getSimpleName() + ".readDataList";

		// Initialize the URL.
		StringBuilder urlStringBuilder = new StringBuilder(getServiceRootURI() + "data");

		// Add 'beginDate'.
		if ( beginDate != null ) {
			boolean formatToSecond = false;
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "beginDate",
				URLEncoder.encode(formatDateTime(beginDate, null, formatToSecond, 0), StandardCharsets.UTF_8.toString()));
		}

		// Add 'centralTendencyType'.
		if ( centralTendencyType != null ) {
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "centralTendencyType", centralTendencyType.toString() );
		}

		// Add 'duration'.
		if ( duration != null ) {
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "duration", duration.toString() );
		}

		// Add 'elements':
		// - format is elementCode:heightDepth:ordinal
		// - the heightDepth is in inches and indicates the vertical height above ground for atmospheric sensors,
		//   or depth below ground for soil sensors
		// - if heightDepth is not specified, it is assumed to be null
		// - if the ordinal is not specified, it is assumed to be 1
		// - the ordinal is used to uniquely identify sensors when the element and heightDepth are the same for a site
		// - if the second or third are omitted they won't be used to filter
		// - can use * in each
		if ( (elements != null) && !elements.isEmpty() ) {
			// Have one or more element to read:
			// - format as a CSV
			StringBuilder csv = new StringBuilder();
			for ( String element : elements ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(element);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "elements", csv.toString());
		}

		// Add 'endDate'.
		if ( endDate != null ) {
			boolean formatToSecond = false;
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "endDate",
				URLEncoder.encode(formatDateTime(endDate, null, formatToSecond, 0), StandardCharsets.UTF_8.toString()));
		}

		// Add 'insertOrUpdateBeginDate'.
		if ( insertOrUpdateBeginDate != null ) {
			// Format to minute.
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "insertOrUpdateBeginDate", insertOrUpdateBeginDate.toString(DateTime.FORMAT_YYYY_MM_DD_HH_mm) );
		}

		// Add 'periodRef':
		// - will be START or END
		// - for example, if monthly data is computed from daily values, START will be 1 for January, or END will be 31
		// - default is end
		if ( periodRefType != null ) {
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "periodRef", periodRefType.toString() );
		}

		// Add 'returnFlags'.
		if ( returnFlags != null ) {
			if ( returnFlags ) {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnFlags", "true" );
			}
			else {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnFlags", "false" );
			}
		}

		// Add 'returnOriginalValues'.
		if ( returnOriginalValues != null ) {
			if ( returnOriginalValues ) {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnOriginalValues", "true" );
			}
			else {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnOriginalValues", "false" );
			}
		}

		// Add 'returnSuspectData'.
		if ( returnSuspectData != null ) {
			if ( returnSuspectData ) {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnSuspectData", "true" );
			}
			else {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnSuspectData", "false" );
			}
		}

		// Add 'stationTriplets':
		// - format is stationId:state:network
		// - if the second or third are omitted they won't be used to filter
		// - can use * in each
		if ( (elements != null) && !elements.isEmpty() ) {
			// Have one or more element to read:
			// - each part can contain *
			// - format as a CSV
			StringBuilder csv = new StringBuilder();
			for ( String stationTriplet : stationTriplets ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(stationTriplet);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "stationTriplets", csv.toString());
		}

		String urlString = urlStringBuilder.toString();

		Message.printStatus(2, routine, "Reading station data list from: " + urlString);
		List<StationData> stationDataList = new ArrayList<>();

		String urlStringEncoded = urlString;
		MultiKeyStringDictionary requestProperties = null;
		int timeoutSeconds = 300;
		UrlReader urlReader = new UrlReader(urlStringEncoded, requestProperties, null, timeoutSeconds*1000 );
		UrlResponse urlResponse = null;
		try {
			urlResponse = urlReader.read();
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading 'data' using \"" + urlString + "\".");
			Message.printWarning(3, routine, e);
			throw new RuntimeException(e);
		}
		if ( urlResponse.hadError() ) {
			// TODO smalers 2020-06-12 would be nice to not catch this immediately.
			throw new RuntimeException ( "Reading URL returned error (code=" + urlResponse.getResponseCode()
				+ "): " + urlResponse.getResponseError() );
		}
		else if ( urlResponse.getResponseCode() != 200 ) {
			throw new RuntimeException ( "Reading URL returned error code: " + urlResponse.getResponseCode() );
		}
		else {
			// Parse the JSON response into objects.
			ObjectMapper mapper = new ObjectMapper();
			String responseJson = urlResponse.getResponse();
			Message.printStatus(2, routine, "JSON response has length = " + responseJson.length());
			try {
				if ( this.debug ) {
					// Set to true to output fhe full response (can overload text editor).
					boolean doDebugFull = false;
					int debugMaxLength = 20000;
					if ( doDebugFull || (responseJson.length() < debugMaxLength) ) {
						// Output the full response.
						Message.printStatus(2, routine, "Response=" + responseJson);
					}
					else {
						// Output part of the response.
						Message.printStatus(2, routine, "Response (first 20000 characters)=" + responseJson.substring(0,20000));
						Message.printStatus(2, routine, "Response (last 20000 characters)=" + responseJson.substring(responseJson.length() - 20000));
					}
				}
				stationDataList = mapper.readValue(responseJson, new TypeReference<List<StationData>>(){});
				Message.printStatus(2, routine, "Read " + stationDataList.size() + " station data objects.");
			}
			catch ( Exception e ) {
				Message.printWarning(3, routine, "Error reading 'data' using \"" + urlString + "\".");
				Message.printWarning(3, routine, e);
				throw new RuntimeException(e);
			}
		}

		// The data are sorted by 'date', ascending.
		//Collections.sort(stationDataList, new DataComparator());
		return stationDataList;
	}

	/**
	 * Read a forecast list using simple input.
	 */
	public List<Forecast> readForecastList ( String stationTriplet, String elementCode, String forecastPeriod )
		throws IOException {
		String beginPublicationDate = null;
		List<String> elementCodes = new ArrayList<>();
		if ( (elementCode != null) && !elementCode.isEmpty() ) {
			elementCodes.add(elementCode);
		}
		String endPublicationDate = null;
		List<String> exceedenceProbabilities = null;
		List<String> forecastPeriods = new ArrayList<>();
		if ( (forecastPeriod != null) && !forecastPeriod.isEmpty() ) {
			forecastPeriods.add(forecastPeriod);
		}
		List<String> stationTriplets = new ArrayList<>();
		if ( (stationTriplet != null) && !stationTriplet.isEmpty() ) {
			stationTriplets.add(stationTriplet);
		}
		// Call the main method.
		return readForecastList (
			// List the query parameters for filters, alphabetical.
			beginPublicationDate,
			elementCodes,
			endPublicationDate,
			exceedenceProbabilities,
			forecastPeriods,
			stationTriplets
		);
	}

	/**
	 * Read a forecast list using simple input with a publication date range.
	 */
	public List<Forecast> readForecastListForPublicationDate (
		String stationTriplet,
		String elementCode,
		String forecastPeriod,
		String beginPublicationDate,
		String endPublicationDate )
		throws IOException {
		List<String> elementCodes = new ArrayList<>();
		if ( (elementCode != null) && !elementCode.isEmpty() ) {
			elementCodes.add(elementCode);
		}
		List<String> exceedenceProbabilities = null;
		List<String> forecastPeriods = new ArrayList<>();
		if ( (forecastPeriod != null) && !forecastPeriod.isEmpty() ) {
			forecastPeriods.add(forecastPeriod);
		}
		List<String> stationTriplets = new ArrayList<>();
		if ( (stationTriplet != null) && !stationTriplet.isEmpty() ) {
			stationTriplets.add(stationTriplet);
		}
		// Call the main method.
		return readForecastList (
			// List the query parameters for filters, alphabetical.
			beginPublicationDate,
			elementCodes,
			endPublicationDate,
			exceedenceProbabilities,
			forecastPeriods,
			stationTriplets
		);
	}

	/**
	 *
 	* Read the Forecast list objects from the "/forecast" service.
 	* The JSON looks like the following:
 	* <pre>
 	*
 [
  {
    "stationTriplet": "09430500:CO:USGS",
    "forecastPointName": "Gila R at Gila",
    "data": [
      {
        "elementCode": "SRVO",
        "forecastPeriod": [
          "04-01",
          "07-31"
        ],
        "forecastStatus": "final",
        "issueDate": "2023-10-02 07:45:52",
        "periodNormal": 13.6,
        "publicationDate": "2023-04-01",
        "unitCode": "kac_ft",
        "forecastValues": {
          "10": 36.5,
          "30": 28.3,
          "50": 24.1
        }
      }
    ]
  }
]
    ...
 }
 	* </pre>
 	* @return the list of Forecast objects
 	*/
	private List<Forecast> readForecastList (
		// List the query parameters for filters, alphabetical.
		String beginPublicationDate,
		List<String> elementCodes,
		String endPublicationDate,
		List<String> exceedenceProbabilities,
		List<String> forecastPeriods,
		List<String> stationTriplets
		) throws IOException {
		String routine = getClass().getSimpleName() + ".readForecastList";

		// Initialize the URL.
		StringBuilder urlStringBuilder = new StringBuilder(getServiceRootURI() + "forecasts");

		// Add 'beginPublicationDate'.
		if ( (beginPublicationDate != null) && !beginPublicationDate.isEmpty() ) {
			// Have a begin publication date.
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "beginPublicationDate", beginPublicationDate);
		}

		// Add 'endPublicationDate'.
		if ( (endPublicationDate != null) && !endPublicationDate.isEmpty() ) {
			// Have an end publication date.
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "endPublicationDate", endPublicationDate);
		}

		// Add 'elementCodes'.
		StringBuilder csv = new StringBuilder();
		if ( (elementCodes != null) && !elementCodes.isEmpty() ) {
			// Have one or more elementCode to read:
			// - format as a CSV
			for ( String elementCode : elementCodes ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(elementCode);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "elementCodes", csv.toString());
		}

		// Add 'exceedenceProbabilities'.
		csv = new StringBuilder();
		if ( (exceedenceProbabilities != null) && !exceedenceProbabilities.isEmpty() ) {
			// Have one or more exceedenceProbabilitie to read:
			// - format as a CSV
			for ( String exceedanceProbability : exceedenceProbabilities ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(exceedanceProbability);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "exceedenceProbabilities", csv.toString());
		}

		// Add 'forecastPeriods'.
		csv = new StringBuilder();
		if ( (forecastPeriods != null) && !forecastPeriods.isEmpty() ) {
			// Have one or more forecastPeriod to read:
			// - this are the name like APR-SEP so need to convert to beginning and ending dates
			// - TODO what to do if there are multiple names?
			// - format as a CSV
       		List<ForecastPeriod> forecastPeriodList = getForecastPeriodList();
			for ( String forecastPeriod : forecastPeriods ) {
           		ForecastPeriod fp = ForecastPeriod.findForecastPeriodForName ( forecastPeriodList, forecastPeriod );
           		if ( fp != null ) {
           			if ( csv.length() > 0 ) {
		       			csv.append ( "," );
	       			}
           			csv.append(fp.getBeginMonthDay());
	       			csv.append ( "," );
           			csv.append(fp.getEndMonthDay());
           		}
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "forecastPeriods", csv.toString());
		}

		// Add 'stationTriplets'.
		csv = new StringBuilder();
		if ( (stationTriplets != null) && !stationTriplets.isEmpty() ) {
			// Have one or more station triplets to read:
			// - format as a CSV
			for ( String stationTriplet : stationTriplets ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(stationTriplet);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "stationTriplets", csv.toString());
		}

		String urlString = urlStringBuilder.toString();

		Message.printStatus(2, routine, "Reading forecast list from: " + urlString);
		List<Forecast> forecasts = new ArrayList<>();

		String urlStringEncoded = urlString;
		MultiKeyStringDictionary requestProperties = null;
		int timeoutSeconds = 300;
		UrlReader urlReader = new UrlReader(urlStringEncoded, requestProperties, null, timeoutSeconds*1000 );
		UrlResponse urlResponse = null;
		try {
			urlResponse = urlReader.read();
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading 'forecasts' using \"" + urlString + "\".");
			Message.printWarning(3, routine, e);
			throw new RuntimeException(e);
		}
		if ( urlResponse.hadError() ) {
			// TODO smalers 2020-06-12 would be nice to not catch this immediately.
			throw new RuntimeException ( "Reading URL returned error (code=" + urlResponse.getResponseCode()
				+ "): " + urlResponse.getResponseError() );
		}
		else if ( urlResponse.getResponseCode() != 200 ) {
			throw new RuntimeException ( "Reading URL returned error code: " + urlResponse.getResponseCode() );
		}
		else {
			// Parse the JSON response into objects.
			ObjectMapper mapper = new ObjectMapper();
			String responseJson = urlResponse.getResponse();
			Message.printStatus(2, routine, "JSON response has length = " + responseJson.length());
			try {
				if ( this.debug ) {
					Message.printStatus(2, routine, "Response=" + responseJson);
				}
				forecasts = mapper.readValue(responseJson, new TypeReference<List<Forecast>>(){});
				Message.printStatus(2, routine, "Read " + forecasts.size() + " forecasts.");
			}
			catch ( Exception e ) {
				Message.printWarning(3, routine, "Error reading 'forecasts' using \"" + urlString + "\".");
				Message.printWarning(3, routine, e);
				throw new RuntimeException(e);
			}
		}

		// Sort on the forecast point name.
		Collections.sort(forecasts, new ForecastComparator());
		return forecasts;
	}

	/**
	 * Read a list of stations that are forecast points.
	 */
	public List<Station> readForecastPointStationList (
	   	List<String> stationIds,
	   	List<String> stateCds,
	   	List<String> networkCds,
	    List<String> forecastPointNames,
	    List<String> hucs,
	    List<String> forecasters )
		throws IOException {
		// List the query parameters for filters, alphabetical.
		Double [] boundingBox = null;
		List<String> countyNames = null;
		List<String> dcoCodes = null;
		List<String> durations = null;
		List<String> elements = null;
		Double elevationMin = null;
		Double elevationMax = null;
		List<String> stationNames = null;
		List<String> stationTriplets = null;
		if ( (stationIds != null) && !stationIds.isEmpty() ) {
			// Form the list of triplets.
			stationTriplets = new ArrayList<>();
			if ( stateCds == null ) {
				// Create an empty list so the following will work.
				stateCds = new ArrayList<>();
			}
			if ( networkCds == null ) {
				// Create an empty list so the following will work.
				networkCds = new ArrayList<>();
			}
			for ( String stationId : stationIds ) {
				for ( String stateCd : stateCds ) {
					for ( String networkCd : networkCds ) {
						stationTriplets.add(stationId + ":" + stateCd + ":" + networkCd );
					}
				}
			}
		}
		// Indicate which data objects to include.
		Boolean activeOnly = null;
		Boolean returnForecastPointMetadata = null;
		Boolean returnReservoirMetadata = null;
		Boolean returnStationElements = null;
		return readStationList (
			// List the query parameters for filters, alphabetical.
			boundingBox,
			countyNames,
			dcoCodes,
			durations,
			elements,
			elevationMin,
			elevationMax,
			hucs,
			stationNames,
			stationTriplets,
			// Indicate which data objects to include.
			activeOnly,
			returnForecastPointMetadata,
			returnReservoirMetadata,
			returnStationElements
		);
	}

	/**
	 * Read global data that should be kept in memory to increase performance.
	 * This is called from the constructor.
	 * If an error is detected, set on the datastore so that TSTool View / Datastores will show the error.
	 * This is usually an issue with a misconfigured datastore.
	 */
	public void readGlobalData () {
		String routine = getClass().getSimpleName() + ".readGlobalData";
		Message.printWarning ( 2, routine, "Reading global data for datastore \"" + getName() + "\"." );

		// Don't read the global data up front because NRCS AWDB may not be used by every user:
		// - the time series list read is not cached and the other data should read quickly

		boolean doReadGlobal = false;
		if ( doReadGlobal ) {

		// Read the duration list.

		try {
			this.durationCache = readDurationList();
			Message.printStatus(2, routine, "Read " + this.durationCache.size() + " durations." );
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading global duration list (" + e + ")");
			Message.printWarning(3, routine, e );
		}

		// Read the element list.

		try {
			this.elementCache = readElementList();
			Message.printStatus(2, routine, "Read " + this.elementCache.size() + " elements." );
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading global element list (" + e + ")");
			Message.printWarning(3, routine, e );
		}

		// Read the forecast period list.

		try {
			this.forecastPeriodCache = readForecastPeriodList();
			Message.printStatus(2, routine, "Read " + this.forecastPeriodCache.size() + " forecast periods." );
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading global forecast period list (" + e + ")");
			Message.printWarning(3, routine, e );
		}

		// Read the network list.

		try {
			this.networkCache = readNetworkList();
			Message.printStatus(2, routine, "Read " + this.networkCache.size() + " networks." );
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading global network list (" + e + ")");
			Message.printWarning(3, routine, e );
		}
		}
	}

	/**
 	* Read the 'durations' list objects from the "resources" service and cache the data for future use.
 	* The JSON looks like the following:
 	* <pre>
 	*
{
  "durations": [
    {
      "code": "1",
      "name": "1 Calendar Year",
      "durationMinutes": "525600"
    },
    ...
 }
 	* </pre>
 	* @return the list of Duration objects
 	*/
	private List<Duration> readDurationList() throws IOException {
		String routine = getClass().getSimpleName() + ".readDurationList";

		String urlString = getServiceRootURI() + "reference-data?referenceLists=durations";
		Message.printStatus(2, routine, "Reading duration list from: " + urlString);
		List<Duration> durations = new ArrayList<>();

		String urlStringEncoded = urlString;
		MultiKeyStringDictionary requestProperties = null;
		int timeoutSeconds = 300;
		UrlReader urlReader = new UrlReader(urlStringEncoded, requestProperties, null, timeoutSeconds*1000 );
		UrlResponse urlResponse = null;
		try {
			urlResponse = urlReader.read();
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading 'durations' using \"" + urlString + "\".");
			Message.printWarning(3, routine, e);
			throw new RuntimeException(e);
		}
		if ( urlResponse.hadError() ) {
			// TODO smalers 2020-06-12 would be nice to not catch this immediately.
			throw new RuntimeException ( "Reading URL returned error (code=" + urlResponse.getResponseCode()
				+ "): " + urlResponse.getResponseError() );
		}
		else if ( urlResponse.getResponseCode() != 200 ) {
			throw new RuntimeException ( "Reading URL returned error code: " + urlResponse.getResponseCode() );
		}
		else {
			// Parse the JSON response into objects.
			ObjectMapper mapper = new ObjectMapper();
			String responseJson = urlResponse.getResponse();
			Message.printStatus(2, routine, "JSON response has length = " + responseJson.length());
			try {
				if ( this.debug ) {
					Message.printStatus(2, routine, "Response=" + responseJson);
				}
				DurationsResponse durationsResponse = mapper.readValue(responseJson, new TypeReference<DurationsResponse>(){});
				durations = durationsResponse.getDurations();
				Message.printStatus(2, routine, "Read " + durations.size() + " durations.");
			}
			catch ( Exception e ) {
				Message.printWarning(3, routine, "Error reading 'durations' using \"" + urlString + "\".");
				Message.printWarning(3, routine, e);
				throw new RuntimeException(e);
			}
		}

		// Sort on the name.
		//Collections.sort(durations, new DurationComparator());
		return durations;
	}

	/**
 	* Read the 'elements' list objects from the "resources" service and cache the data for future use.
 	* The JSON looks like the following:
 	* <pre>
 	*
 {
  "elements": [
    {
      "code": "TAVG",
      "name": "AIR TEMPERATURE AVERAGE",
      "physicalElementName": "air temperature",
      "functionCode": "V",
      "dataPrecision": 1,
      "description": "Average Air Temperature - Sub-Hourly Sampling Frequency",
      "storedUnitCode": "degF",
      "englishUnitCode": "degF",
      "metricUnitCode": "degC"
    },
    ...
 }
 	* </pre>
 	* @return the list of Element objects
 	*/
	private List<Element> readElementList() throws IOException {
		String routine = getClass().getSimpleName() + ".readElementList";

		String urlString = getServiceRootURI() + "reference-data?referenceLists=elements";
		Message.printStatus(2, routine, "Reading element list from: " + urlString);
		List<Element> elements = new ArrayList<>();

		String urlStringEncoded = urlString;
		MultiKeyStringDictionary requestProperties = null;
		int timeoutSeconds = 300;
		UrlReader urlReader = new UrlReader(urlStringEncoded, requestProperties, null, timeoutSeconds*1000 );
		UrlResponse urlResponse = null;
		try {
			urlResponse = urlReader.read();
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading 'elements' using \"" + urlString + "\".");
			Message.printWarning(3, routine, e);
			throw new RuntimeException(e);
		}
		if ( urlResponse.hadError() ) {
			// TODO smalers 2020-06-12 would be nice to not catch this immediately.
			throw new RuntimeException ( "Reading URL returned error (code=" + urlResponse.getResponseCode()
				+ "): " + urlResponse.getResponseError() );
		}
		else if ( urlResponse.getResponseCode() != 200 ) {
			throw new RuntimeException ( "Reading URL returned error code: " + urlResponse.getResponseCode() );
		}
		else {
			// Parse the JSON response into objects.
			ObjectMapper mapper = new ObjectMapper();
			String responseJson = urlResponse.getResponse();
			Message.printStatus(2, routine, "JSON response has length = " + responseJson.length());
			try {
				if ( this.debug ) {
					Message.printStatus(2, routine, "Response=" + responseJson);
				}
				ElementsResponse elementsResponse = mapper.readValue(responseJson, new TypeReference<ElementsResponse>(){});
				elements = elementsResponse.getElements();
				Message.printStatus(2, routine, "Read " + elements.size() + " elements.");
			}
			catch ( Exception e ) {
				Message.printWarning(3, routine, "Error reading 'elements' using \"" + urlString + "\".");
				Message.printWarning(3, routine, e);
				throw new RuntimeException(e);
			}
		}

		// Sort on the name.
		Collections.sort(elements, new ElementComparator());
		return elements;
	}

	/**
 	* Read the 'forecastPeriods' list objects from the "resources" service and cache the data for future use.
 	* The JSON looks like the following:
 	* <pre>
 	*
{
  "forecastPeriods": [
    {
      "code": "09",
      "name": "OCT-SEP",
      "beginMonthDay": "10-01",
      "endMonthDay": "09-30"
    },
    ...
 }
 	* </pre>
 	* @return the list of ForecastPeriod objects
 	*/
	private List<ForecastPeriod> readForecastPeriodList() throws IOException {
		String routine = getClass().getSimpleName() + ".readForecastPeriodList";

		String urlString = getServiceRootURI() + "reference-data?referenceLists=forecastPeriods";
		Message.printStatus(2, routine, "Reading forecast period list from: " + urlString);
		List<ForecastPeriod> forecastPeriods = new ArrayList<>();

		String urlStringEncoded = urlString;
		MultiKeyStringDictionary requestProperties = null;
		int timeoutSeconds = 300;
		UrlReader urlReader = new UrlReader(urlStringEncoded, requestProperties, null, timeoutSeconds*1000 );
		UrlResponse urlResponse = null;
		try {
			urlResponse = urlReader.read();
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading 'forecastPeriods' using \"" + urlString + "\".");
			Message.printWarning(3, routine, e);
			throw new RuntimeException(e);
		}
		if ( urlResponse.hadError() ) {
			// TODO smalers 2020-06-12 would be nice to not catch this immediately.
			throw new RuntimeException ( "Reading URL returned error (code=" + urlResponse.getResponseCode()
				+ "): " + urlResponse.getResponseError() );
		}
		else if ( urlResponse.getResponseCode() != 200 ) {
			throw new RuntimeException ( "Reading URL returned error code: " + urlResponse.getResponseCode() );
		}
		else {
			// Parse the JSON response into objects.
			ObjectMapper mapper = new ObjectMapper();
			String responseJson = urlResponse.getResponse();
			Message.printStatus(2, routine, "JSON response has length = " + responseJson.length());
			try {
				if ( this.debug ) {
					Message.printStatus(2, routine, "Response=" + responseJson);
				}
				ForecastPeriodsResponse forecastPeriodsResponse = mapper.readValue(responseJson, new TypeReference<ForecastPeriodsResponse>(){});
				forecastPeriods = forecastPeriodsResponse.getForecastPeriods();
				Message.printStatus(2, routine, "Read " + forecastPeriods.size() + " forecast periods.");
			}
			catch ( Exception e ) {
				Message.printWarning(3, routine, "Error reading 'forecastPeriods' using \"" + urlString + "\".");
				Message.printWarning(3, routine, e);
				throw new RuntimeException(e);
			}
		}

		// Sort on the name.
		Collections.sort(forecastPeriods, new ForecastPeriodComparator());
		return forecastPeriods;
	}

	/**
	Read a forecast table.
	@param stationIdList the list of station identifiers to match (can be null or empty)
	@param stateList the list of state abbreviations to match (can be null or empty)
	@param netorkList the list of networks to match (can be null or empty)
	@param hucList the list of HUC basin identifiers to match (can be null or empty)
	@param elementList the list of elements to match (can be null or empty)
	@param forecastPeriod the forecast period name to match (can be null or empty)
	@param forecastPublicationDateStart publication date start, YYYY-MM-DD hh:mm:ss, or null to read all
	@param forecastPublicationDateEnd publication date end, YYYY-MM-DD hh:mm:ss, or null to read all
	@param forecastExceedanceProbabilities exceedance probabilities to return, or null to read all
	@param forecastTableID the name of the forecast table to create
	@param problems problems to output in the calling code
	*/
	public DataTable readForecastTable (
		List<String> stationIdList,
		List<String> stateList,
	    List<Network> networkList,
	    List<String> hucList,
	    List<Element> elementList,
	    String forecastPeriod,
	    String forecastPublicationDateStart,
	    String forecastPublicationDateEnd,
	    int [] forecastExceedanceProbabilities,
	    String forecastTableID,
	    List<String> problems ) {
	    String routine = getClass().getSimpleName() + ".readForecastTable";
	    DataTable table = null;
	    if ( (forecastExceedanceProbabilities != null) && (forecastExceedanceProbabilities.length == 0) ) {
	        // Just set to null to simplify logic below.
	        forecastExceedanceProbabilities = null;
	    }

	    // Translate method parameters into types consistent with web service.

	    List<String> stationIds = stationIdList;
	    List<String> stateCds = stateList;
	    List<String> networkCds = new ArrayList<>();
	    for ( Network network: networkList ) {
	        networkCds.add(network.getCode());
	    }
	    List<String> hucs = hucList;
	    // TODO SAM 2013-11-04 Enable forecast point names and forecasters.
	    List<String> forecastPointNames = new ArrayList<>();
	    List<String> forecasters = new ArrayList<>();

	    // Create an output table with some standard columns containing the forecast information.
	    // Multiple forecasts can be retrieved and will need to be split out later with table manipulation commands.

	    List<TableField> columnList = new ArrayList<>();
	    table = new DataTable( columnList );
	    int stationTripletCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "StationTriplet", -1, -1), null);
	    int stateCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "State", -1, -1), null);
	    int stationIDCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "StationID", -1, -1), null);
	    int networkCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Network", -1, -1), null);
	    int elementCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Element", -1, -1), null);
	    int forecastPeriodCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "ForecastPeriod", -1, -1), null);
	    int publicationDateCol = table.addField(new TableField(TableField.DATA_TYPE_DATETIME, "PublicationDate", -1, -1), null);
	    //int calculationDateCol = table.addField(new TableField(TableField.DATA_TYPE_DATETIME, "CalculationDate", -1, -1), null);
	    int issueDateCol = table.addField(new TableField(TableField.DATA_TYPE_DATETIME, "IssueDate", -1, -1), null);
	    int exceedanceProbabilityCol = table.addField(new TableField(TableField.DATA_TYPE_INT, "ExceedanceProbability", -1, 2), null);
	    int valueCol = table.addField(new TableField(TableField.DATA_TYPE_DOUBLE, "Value", -1, 2), null);
	    int unitsCol = table.addField(new TableField(TableField.DATA_TYPE_STRING, "Units", -1, -1), null);
	    int periodAverageCol = table.addField(new TableField(TableField.DATA_TYPE_DOUBLE, "PeriodAverage", -1, 2), null);
	    table.setTableID ( forecastTableID );

	    // First get the forecast point stations that correspond to the location information.
	    //List<ForecastPoint> forecastPoints = ws.getForecastPoints (
	    List<Station> forecastPointStations = new ArrayList<>();
	    try {
	    	Message.printStatus(periodAverageCol, routine, "Reading foreaast point station list." );
	    	forecastPointStations = readForecastPointStationList (
	    		stationIds,
	    		stateCds,
	    		networkCds,
	       		forecastPointNames,
	       		hucs,
	       		forecasters );
	    }
	    catch ( Exception e ) {
	    	Message.printWarning(3, routine, "Exception reading forecast point stations (" + e + ")." );
	    	Message.printWarning(3, routine, e );
	    }

	    if ( (stationIds != null) && (stationIds.size() != forecastPointStations.size()) ) {
	        // Did not get data for all the forecast points.
	        // Generate warnings for each forecast point that did not have anything returned.
	        for ( String stationID : stationIds ) {
	            boolean found = false;
	            for ( Station fp : forecastPointStations ) {
	                if ( stationID.equalsIgnoreCase(fp.getStationTriplet().split(":")[0])) {
	                    found = true;
	                    break;
	                }
	            }
	            if ( !found ) {
	                String message = "Did not find forecast point for station \"" + stationID + "\"";
	                Message.printWarning(3, routine, message );
	                problems.add ( message );
	            }
	        }
	    }

	    String [] tripletParts;
	    int row = -1;
	    String beginPublicationDate = null, endPublicationDate = null;
	    if ( (forecastPublicationDateStart != null) && !forecastPublicationDateStart.equals("") ) {
	        beginPublicationDate = forecastPublicationDateStart;
	        if ( (forecastPublicationDateEnd == null) || forecastPublicationDateEnd.equals("") ) {
	            // Set end to the same as the begin since both are required.
	            endPublicationDate = beginPublicationDate;
	        }
	    }
	    if ( (forecastPublicationDateEnd != null) && !forecastPublicationDateEnd.equals("") ) {
	        endPublicationDate = forecastPublicationDateEnd;
	        if ( (forecastPublicationDateStart == null) || forecastPublicationDateStart.equals("") ) {
	            // Set begin to the same as the end since both are required.
	            beginPublicationDate = endPublicationDate;
	        }
	    }

	    for ( Element element: elementList ) {
	        // Loop through the element codes of interest.  The API only takes one value.
	        for ( Station fp : forecastPointStations ) {
	            // Loop through the forecast points that matched the initial query.
	            Message.printStatus(2, routine, "Calling readForecastList for triplet=\"" + fp.getStationTriplet() +
	                "\" elementCd=\"" + element.getCode() + "\" forecastPeriod=" + forecastPeriod );
	            List<Forecast> forecasts = null;
	            if ( (beginPublicationDate == null) && (endPublicationDate == null)) {
	                //forecasts = ws.getForecasts(fp.getStationTriplet(), element.getElementCd(), forecastPeriod);
	            	try {
	            		forecasts = readForecastList ( fp.getStationTriplet(), element.getCode(), forecastPeriod );
	            	}
	            	catch ( Exception e ) {
	            		String message = "Excpeption reading forecasts (" + e + ").";
	            		Message.printWarning ( 3, routine, message );
	            		problems.add ( message );
	            		Message.printWarning ( 3, routine, e );
	            	}
	            }
	            else {
	                // forecasts = ws.getForecastsByPubDate(fp.getStationTriplet(), element.getElementCd(), forecastPeriod,
	                //     beginPublicationDate, endPublicationDate);
	            	try {
	            		forecasts = readForecastListForPublicationDate ( fp.getStationTriplet(), element.getCode(), forecastPeriod, beginPublicationDate, endPublicationDate);
	            	}
	            	catch ( Exception e ) {
	            		String message = "Excpeption reading forecasts (" + e + ").";
	            		Message.printWarning ( 3, routine, message );
	            		problems.add ( message );
	            		Message.printWarning ( 3, routine, e );
	            	}
	            }
	            // Should only be one forecast.
	            Forecast forecast = null;
	            if ( (forecasts != null) && !forecasts.isEmpty() ) {
	            	forecast = forecasts.get(0);
	            }
	            if ( forecast != null ) {
		            tripletParts = fp.getStationTriplet().split(":"); // StationID:State:Network
		            int errorCount = 0;
		            Message.printStatus(2, routine, "Have " + forecast.getData() + " ForecastData objects.");
		            for ( ForecastData data : forecast.getData() ) {
		                if ( Message.isDebugOn ) {
		                	/*
		                    Message.printDebug(1, routine, "Forecast calculationDate=" + f.getCalculationDate() +
		                        ", elementCd=" + f.getElementCd() + ", forecastPeriod=" + f.getForecastPeriod() +
		                        ", publicationDate=" + f.getPublicationDate() + ", stationTriplet=" + f.getStationTriplet() +
		                        ", unitCd=" + f.getUnitCd() + ", periodAverage=" + f.getPeriodAverage());
		                        */
		                    Message.printDebug(1, routine, "Forecast " + //calculationDate=" + f.getCalculationDate() +
		                        ", elementCd=" + data.getElementCode() + ", forecastPeriod=" + data.getForecastPeriod() +
		                        ", publicationDate=" + data.getPublicationDate() + ", stationTriplet=" + forecast.getStationTriplet() +
		                        ", unitCd=" + data.getUnitCode() + ", periodNormal=" + data.getPeriodNormal());
		                }
		                List<Integer> eprob = data.getExceedenceProbabilities();
		                List<Double> eval = data.getExceedenceValues();
		                boolean includeProb = false;
		                Message.printStatus(2, routine, "Have " + eprob.size() + " probabilities.");
		                for ( int i = 0; i < eprob.size(); i++ ) {
		                    if ( Message.isDebugOn ) {
		                        Message.printDebug(1,routine,"Probability=" + eprob.get(i) + " value=" + eval.get(i) );
		                    }
		                    // Skip the probability if not requested.
		                    includeProb = true;
		                    if ( forecastExceedanceProbabilities != null ) {
		                        includeProb = false;
		                        for ( int ip = 0; ip < forecastExceedanceProbabilities.length; ip++ ) {
		                            if ( eprob.get(i) == forecastExceedanceProbabilities[ip] ) {
		                                includeProb = true;
		                                break;
		                            }
		                        }
		                    }
		                    if ( !includeProb ) {
		                        continue;
		                    }
		                    // Set the values in the table.
		                    ++row;
		                    try {
		                        table.setFieldValue(row, stationTripletCol, fp.getStationTriplet(), true);
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting station triplet for row " + row );
		                        ++errorCount;
		                    }
		                    try {
		                        table.setFieldValue(row, stateCol, tripletParts[1], true);
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting state for row " + row );
		                        ++errorCount;
		                    }
		                    try {
		                        table.setFieldValue(row, stationIDCol, tripletParts[0], true);
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting station ID for row " + row );
		                        ++errorCount;
		                    }
		                    try {
		                        table.setFieldValue(row, networkCol, tripletParts[2], true);
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting network for row " + row );
		                        ++errorCount;
		                    }
		                    try {
		                        table.setFieldValue(row, elementCol, element.getCode(), true);
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting element for row " + row );
		                        ++errorCount;
		                    }
		                    try {
		                        table.setFieldValue(row, forecastPeriodCol, forecastPeriod, true);
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting forecast period for row " + row );
		                        ++errorCount;
		                    }
		                    try {
		                        table.setFieldValue(row, publicationDateCol, DateTime.parse(data.getPublicationDate()), true);
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting publication date for row " + row );
		                        ++errorCount;
		                    }
		                    try {
		                        //table.setFieldValue(row, calculationDateCol, DateTime.parse(data.getCalculationDate()), true);
		                        table.setFieldValue(row, issueDateCol, DateTime.parse(data.getIssueDate()), true);
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting calculation date for row " + row );
		                        ++errorCount;
		                    }
		                    try {
		                        Double v = eval.get(i);
		                        if ( v == null ) {
		                            table.setFieldValue(row, valueCol, null, true);
		                        }
		                        else {
		                            table.setFieldValue(row, valueCol, v.doubleValue(), true);
		                        }
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting value for row " + row );
		                        ++errorCount;
		                    }
		                    try {
		                        table.setFieldValue(row, unitsCol, data.getUnitCode(), true);
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting units for row " + row );
		                        ++errorCount;
		                    }
		                    try {
		                        // Probability is an integer.
		                        table.setFieldValue(row, exceedanceProbabilityCol, eprob.get(i), true);
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting exceedance probability for row " + row );
		                        ++errorCount;
		                    }
		                    try {
		                        Double ave = data.getPeriodNormal();
		                        if ( ave == null ) {
		                            table.setFieldValue(row, periodAverageCol, ave, true);
		                        }
		                        else {
		                            table.setFieldValue(row, periodAverageCol, ave.doubleValue(), true);
		                        }
		                    }
		                    catch ( Exception e ) {
		                        Message.printWarning(3, routine, "Error setting period normal for row " + row );
		                        ++errorCount;
		                    }
		                }
		            }
		            if ( errorCount > 0 ) {
		            	problems.add ( "There were " + errorCount + " errors setting forecast data in the table.  See the log file." );
		            }
	            }
	            else {
		             Message.printStatus(2, routine, "No forecasts were found." );
	            }
	        }
	    }
	    return table;
	}

	/**
 	* Read the 'networks' list objects from the "resources" service and cache for future use.
 	* The JSON looks like the following:
 	* <pre>
 	*
 {
  "networks": [
    {
      "code": "CLMIND",
      "name": "CLIMATE INDEX",
      "description": "CLIMATE INDICES NETWORK"
    },
    ...
 }
 	* </pre>
 	* @return the list of Network objects
 	*/
	private List<Network> readNetworkList() throws IOException {
		String routine = getClass().getSimpleName() + ".readNetworkList";

		String urlString = getServiceRootURI() + "reference-data?referenceLists=networks";
		Message.printStatus(2, routine, "Reading network list from: " + urlString);
		List<Network> networks = new ArrayList<>();

		String urlStringEncoded = urlString;
		MultiKeyStringDictionary requestProperties = null;
		int timeoutSeconds = 300;
		UrlReader urlReader = new UrlReader(urlStringEncoded, requestProperties, null, timeoutSeconds*1000 );
		UrlResponse urlResponse = null;
		try {
			urlResponse = urlReader.read();
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading 'networks' using \"" + urlString + "\".");
			Message.printWarning(3, routine, e);
			throw new RuntimeException(e);
		}
		if ( urlResponse.hadError() ) {
			// TODO smalers 2020-06-12 would be nice to not catch this immediately.
			throw new RuntimeException ( "Reading URL returned error (code=" + urlResponse.getResponseCode()
				+ "): " + urlResponse.getResponseError() );
		}
		else if ( urlResponse.getResponseCode() != 200 ) {
			throw new RuntimeException ( "Reading URL returned error code: " + urlResponse.getResponseCode() );
		}
		else {
			// Parse the JSON response into objects.
			ObjectMapper mapper = new ObjectMapper();
			String responseJson = urlResponse.getResponse();
			Message.printStatus(2, routine, "JSON response has length = " + responseJson.length());
			try {
				if ( this.debug ) {
					Message.printStatus(2, routine, "Response=" + responseJson);
				}
				NetworksResponse networksResponse = mapper.readValue(responseJson, new TypeReference<NetworksResponse>(){});
				networks = networksResponse.getNetworks();
				Message.printStatus(2, routine, "Read " + networks.size() + " networks.");
			}
			catch ( Exception e ) {
				Message.printWarning(3, routine, "Error reading 'networks' using \"" + urlString + "\".");
				Message.printWarning(3, routine, e);
				throw new RuntimeException(e);
			}
		}

		// Sort on the name.
		Collections.sort(networks, new NetworkComparator());
		return networks;
	}

	/**
 	* Read the 'stations' list objects from the "/stations" service.
 	* The JSON looks like the following:
 	* <pre>
 	*
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
    ...
 }
 	* </pre>
 	* @param stationTriplets a list of station triplets to read (can be null or empty)
 	* @return the list of Station objects
 	*/
	private List<Station> readStationList (
		// List the query parameters for filters, grouped and roughly alphabetical.
		Double[] boundingBox,
		List<String> countyNames,
		List<String> dcoCodes,
		List<String> durations,
		List<String> elements,
		Double elevationMin,
		Double elevationMax,
		List<String> hucs,
		List<String> stationNames,
		List<String> stationTriplets,
		// Indicate which data objects to include.
		Boolean activeOnly,
		Boolean returnForecastPointMetadata,
		Boolean returnReservoirMetadata,
		Boolean returnStationElements
		) throws IOException {
		String routine = getClass().getSimpleName() + ".readStationList";

		// Initialize the URL.
		StringBuilder urlStringBuilder = new StringBuilder(getServiceRootURI() + "stations");

		// Add 'activeOnly'.
		if ( activeOnly != null ) {
			if ( activeOnly ) {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "activeOnly", "true" );
			}
			else {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "activeOnly", "false" );
			}
		}

		// Add 'countyNames'.
		if ( (countyNames != null) && !countyNames.isEmpty() ) {
			// Have one or more county names to read:
			// - format as a CSV
			StringBuilder csv = new StringBuilder();
			for ( String countyName : countyNames ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(countyName);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "countyNames", csv.toString());
		}

		// Add 'dcoCodes'.
		if ( (dcoCodes != null) && !dcoCodes.isEmpty() ) {
			// Have one or more DCO codes to read:
			// - format as a CSV
			StringBuilder csv = new StringBuilder();
			for ( String dcoCode : dcoCodes ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(dcoCode);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "dcoCodes", csv.toString());
		}

		// Add 'durations'.
		if ( (durations != null) && !durations.isEmpty() ) {
			// Have one or more durations to read:
			// - format as a CSV
			StringBuilder csv = new StringBuilder();
			for ( String duration : durations ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(duration);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "durations", csv.toString());
		}

		// Add 'elements':
		// - format is elementCode:heightDepth:ordinal
		// - if the second or third are omitted they won't be used to filter
		// - can use * in each
		if ( (elements != null) && !elements.isEmpty() ) {
			// Have one or more element to read:
			// - format as a CSV
			StringBuilder csv = new StringBuilder();
			for ( String element : elements ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(element);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "elements", csv.toString());
		}

		// Add 'hucs'.
		if ( (hucs != null) && !hucs.isEmpty() ) {
			// Have one or more HUCs to read:
			// - format as a CSV
			StringBuilder csv = new StringBuilder();
			for ( String huc : hucs ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(huc);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "hucs", csv.toString());
		}

		// Add 'returnForecastPointMetadata'.
		if ( returnForecastPointMetadata != null ) {
			if ( returnForecastPointMetadata ) {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnForecastPointMetadata", "true" );
			}
			else {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnForecastPointMetadata", "false" );
			}
		}

		// Add 'returnReservoirMetadata'.
		if ( returnReservoirMetadata != null ) {
			if ( returnReservoirMetadata ) {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnReservoirMetadata", "true" );
			}
			else {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnReservoirMetadata", "false" );
			}
		}

		// Add 'returnStationElements'.
		if ( returnStationElements != null ) {
			if ( returnStationElements ) {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnStationElements", "true" );
			}
			else {
				WebUtil.appendUrlQueryParameter(urlStringBuilder, "returnStationElements", "false" );
			}
		}

		// Add 'stationNames'.
		if ( (stationNames != null) && !stationNames.isEmpty() ) {
			// Have one or more station names to read:
			// - format as a CSV
			StringBuilder csv = new StringBuilder();
			for ( String stationName : stationNames ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(stationName);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "stationNames", csv.toString());
		}

		// Add 'stationTriplets'.
		if ( (stationTriplets != null) && !stationTriplets.isEmpty() ) {
			// Have one or more station triplets to read:
			// - format as a CSV
			StringBuilder csv = new StringBuilder();
			for ( String stationTriplet : stationTriplets ) {
				if ( csv.length() > 0 ) {
					csv.append ( "," );
				}
				csv.append(stationTriplet);
			}
			WebUtil.appendUrlQueryParameter(urlStringBuilder, "stationTriplets", csv.toString());
		}

		String urlString = urlStringBuilder.toString();

		Message.printStatus(2, routine, "Reading station list from: " + urlString);
		// Stations from the API.
		List<Station> stations = new ArrayList<>();

		String urlStringEncoded = urlString;
		MultiKeyStringDictionary requestProperties = null;
		int timeoutSeconds = 300;
		UrlReader urlReader = new UrlReader(urlStringEncoded, requestProperties, null, timeoutSeconds*1000 );
		UrlResponse urlResponse = null;
		try {
			urlResponse = urlReader.read();
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading 'stations' using \"" + urlString + "\".");
			Message.printWarning(3, routine, e);
			throw new RuntimeException(e);
		}
		if ( urlResponse.hadError() ) {
			// TODO smalers 2020-06-12 would be nice to not catch this immediately.
			throw new RuntimeException ( "Reading URL returned error (code=" + urlResponse.getResponseCode()
				+ "): " + urlResponse.getResponseError() );
		}
		else if ( urlResponse.getResponseCode() != 200 ) {
			throw new RuntimeException ( "Reading URL returned error code: " + urlResponse.getResponseCode() );
		}
		else {
			// Parse the JSON response into objects.
			ObjectMapper mapper = new ObjectMapper();
			String responseJson = urlResponse.getResponse();
			Message.printStatus(2, routine, "JSON response has length = " + responseJson.length());
			try {
				if ( this.debug ) {
					// Set to true to output the full response (can overload text editors).
					boolean doDebugFull = false;
					int debugMaxLength = 20000;
					if ( doDebugFull || (responseJson.length() < debugMaxLength) ) {
						// Output the full response.
						Message.printStatus(2, routine, "Response=" + responseJson);
					}
					else {
						// Output part of the response.
						Message.printStatus(2, routine, "Response (first 20000 characters)=" + responseJson.substring(0,20000));
						Message.printStatus(2, routine, "Response (last 20000 characters)=" + responseJson.substring(responseJson.length() - 20000));
					}
				}
				stations = mapper.readValue(responseJson, new TypeReference<List<Station>>(){});

				// Apply additional filters that were previously in the SOAP API.

				boolean doElevationCheck = false;
				boolean doBoundingBoxCheck = false;
				if ( (elevationMax != null) && (elevationMin != null) ) {
					doElevationCheck = true;
				}
				if ( doElevationCheck || doBoundingBoxCheck ) {
					// Have one of the extra filters.
					boolean includeStation = false;
					// Loop backards so that objects can be removed without issues.
					Station station = null;
					Double stationElevation = null;
					Double stationLatitude = null;
					Double stationLongitude = null;
					for ( int i = stations.size() - 1; i >= 0; --i ) {
						station = stations.get(i);
						// Include unless the additional filter says to not include.
						includeStation = true;
						if ( doElevationCheck ) {
							// Check the station elevation.
							stationElevation = station.getElevation();
							if ( (stationElevation == null) || (stationElevation < elevationMin) || (stationElevation > elevationMax) ) {
								includeStation = false;
							}
						}
						if ( doBoundingBoxCheck ) {
							// Check the station location.
							stationLatitude = station.getLatitude();
							stationLongitude = station.getLongitude();
							if ( (stationLatitude == null) || (stationLongitude == null)
								|| (stationLongitude < boundingBox[0]) || (stationLongitude > boundingBox[2])
								|| (stationLatitude < boundingBox[1]) || (stationLatitude > boundingBox[3]) ) {
								includeStation = false;
							}
						}
						if ( !includeStation ) {
							// Remove the station from the list.
							stations.remove(i);
						}
					}
				}

				Message.printStatus(2, routine, "Read " + stations.size() + " stations.");
			}
			catch ( Exception e ) {
				Message.printWarning(3, routine, "Error reading 'stations' using \"" + urlString + "\".");
				Message.printWarning(3, routine, e);
				throw new RuntimeException(e);
			}
		}

		// Sort on the name.
		Collections.sort(stations, new StationComparator());
		return stations;
	}

	/**
	 * Read a list of stations given a list of station triplets.
 	 * @param stationTriplets a list of station triplets to read
 	 * @return the list of Station objects
 	 */
	private List<Station> readStationListForStationTriplets ( List<String> stationTriplets )
		throws IOException {
		// List the query parameters for filters, alphabetical.
		Double [] boundingBox = null;
		List<String> countyNames = null;
		List<String> dcoCodes = null;
		List<String> durations = null;
		List<String> elements = null;
		Double elevationMin = null;
		Double elevationMax = null;
		List<String> hucs = null;
		List<String> stationNames = null;
		// Indicate which data objects to include.
		Boolean activeOnly = null;
		Boolean returnForecastPointMetadata = null;
		Boolean returnReservoirMetadata = null;
		Boolean returnStationElements = null;
		return readStationList (
			// List the query parameters for filters, alphabetical.
			boundingBox,
			countyNames,
			dcoCodes,
			durations,
			elements,
			elevationMin,
			elevationMax,
			hucs,
			stationNames,
			stationTriplets,
			// Indicate which data objects to include.
			activeOnly,
			returnForecastPointMetadata,
			returnReservoirMetadata,
			returnStationElements
		);
	}

	/**
	Read a single time series given the time series identifier (TSID).  The TSID parts are mapped into the SOAP
	query parameters as if a single site has been specified, by calling the readTimeSeriesList() method.
	@param tsid time series identifier string of form State-StationID.NetworkCode.ElementCode.Interval~DataStoreID
	@param readStart the starting date/time to read, or null to read all data.
	@param readEnd the ending date/time to read, or null to read all data.
	@param readData if true, read the data; if false, construct the time series and populate properties but do not read the data
	@return the time series list read from the NRCS AWDB daily web services
	*/
	public TS readTimeSeries (
		String tsid,
		DateTime readStart,
		DateTime readEnd,
		boolean readData )
		throws MalformedURLException, IOException, Exception {
    	String routine = getClass().getSimpleName() + ".readTimeSeries";

		// Initialize empty query parameters to filter the read:
		// - only the values from the TSID will be set below, mainly the station triplet and element
	    List<String> stationIdList = new ArrayList<>();
	    List<String> stateList = new ArrayList<>();
	    List<String> hucList = new ArrayList<>();
	    Double [] boundingBox = null;
	    List<String> countyList = new ArrayList<>();
	    List<Network> networkList = new ArrayList<>();
	    List<Element> elementList = new ArrayList<>();
	    Hashtable<String,String> timeZoneMap = new Hashtable<>();
	    Double elevationMin = null;
	    Double elevationMax = null;
	    YearType outputYearType = null;

	    // Parse the TSID string and set in the query parameters.
	    TSIdent tsident = TSIdent.parseIdentifier(tsid);
	    TimeInterval interval = TimeInterval.parseInterval(tsident.getInterval() );
	    // Location is like "CO-1234".
	    String loc = tsident.getLocation();
	    String [] locParts = loc.split("-");
	    stateList.add ( locParts[0] );
	    stationIdList.add ( locParts[1] );
	    //networkList.add(new NrcsAwdbNetworkCode(tsident.getSource(),""));
	    networkList.add(new Network(tsident.getSource() ));
	    Element element = new Element();
	    //element.setElementCd(tsident.getType());
	    element.setCode(tsident.getType());
	    elementList.add ( element );

		DateTime insertOrUpdateBeginDate = null;
		PeriodRefType periodRefType = null;
		Boolean activeOnly = null;
		Boolean readOriginalValues = null;
		Boolean readSuspectData = null;

	    // The following should return one and only one time series.
	    List<TS> tsList = readTimeSeriesList (
	    	stationIdList,
	    	stateList,
	    	networkList,
	    	hucList,
	    	boundingBox,
	        countyList,
	        elementList,
	        elevationMin,
	        elevationMax,
	        interval,
	        readStart,
	        readEnd,
	        outputYearType,
	        timeZoneMap,
	        readData,
			// Additional REST API parameters.
			insertOrUpdateBeginDate,
			periodRefType,
			activeOnly,
			readOriginalValues,
			readSuspectData );
	    if ( tsList.size() == 1) {
	    	Message.printWarning(3, routine, "Request for TSID \"" + tsid + "\" returned 1 time series as expected.");
	        return tsList.get(0);
	    }
	    else {
	    	Message.printWarning(3, routine, "Request for TSID \"" + tsid + "\" returned " + tsList.size() + " time series.  Was expecting 1.");
	    	// Could return the first one if more than one was returned but return null.
	    	return null;
	    }
	}

	/**
	Read a list of time series from the web service, using query parameters that are supported for the web service.
	@param boundingBox bounding box as WestLong, SouthLat, EastLong, NorthLat (negatives for western hemisphere longitudes).
	@param elementListReq list of requested element codes to match stations -
	if null then the element list is queried for each station as processed
	@param timeZoneMap a map that allows resetting the time zone ID from the NRCS value
	(e.g., "-8" to a more standard representation such as "PST").
	*/
	public List<TS> readTimeSeriesList (
		List<String> stationIdList,
		List<String> stateList,
	    List<Network> networkList,
	    List<String> hucList,
	    Double [] boundingBox,
	    List<String> countyList,
	    List<Element> elementListReq,
	    Double elevationMin,
	    Double elevationMax,
	    TimeInterval interval,
	    DateTime readStartReq,
	    DateTime readEndReq,
	    YearType outputYearType,
	    Hashtable<String,String> timeZoneMap,
	    boolean readData,
		// Additional REST API parameters.
		DateTime insertOrUpdateBeginDate,
		PeriodRefType periodRefType,
	   	Boolean activeOnly,
		Boolean readOriginalValues,
		Boolean readSuspectData ) {
	    String routine = getClass().getSimpleName() + ".readTimeSeriesList";
	    List<TS> tsList = new ArrayList<>();
	    // First translate method parameters into types consistent with web service
	    //AwdbWebService ws = getAwdbWebService ();
	    List<String> stationIds = stationIdList;
	    List<String> stateCds = stateList;
	    List<String> networkCds = new ArrayList<>();
	    for ( Network network: networkList ) {
	        networkCds.add(network.getCode());
	    }
	    List<String> hucs = hucList;
	    List<String> countyNames = countyList;
	    List<String> elementCodes = new ArrayList<>();
	    if ( elementListReq != null ) {
	        for ( Element element: elementListReq ) {
	            elementCodes.add ( element.getCode() );
	        }
	    }
	    List<Integer> ordinals = new ArrayList<>();
	    /** SOAP API.
	     * List<HeightDepth> heightDepths = new ArrayList<>();
	    boolean logicalAnd = true;
	    List<Station> stations = readStations (
	    	stationIds,
	    	stateCds,
	    	networkCds,
	    	hucs,
	    	countyNames,
	        minLatitude,
	    	maxLatitude,
	    	minLongitude,
	    	maxLongitude,
	    	minElevation,
	    	maxElevation,
	        elementCds,
	    	ordinals,
	    	//heightDepths,
	    	logicalAnd );
	    	*/
	    // Old code does not use the same choices.
	    List<String> dcoCodes = null;
	    List<String> durations = null;
	    List<String> stationNames = null; // TODO check how IDs are used
	    List<String> stationTriplets = new ArrayList<>();
	   	Boolean returnForecastPointMetadata = true; // Retrieve for time series properties.
	   	Boolean returnReservoirMetadata = true; // Retrieve for time series properties.
	   	Boolean returnStationElements = true; // Retrieve to match element with time series data type.
	    List<Station> stations = new ArrayList<>();

	    if ( (stationIds.size() > 0) && (stateCds.size() > 0) && (networkCds.size() > 0) ) {
	    	// Form triplets from the station identifiers:
	    	// - allow multiple station IDs
	    	// - use the first state code
	    	// - use the first network code
	    	String state = stateCds.get(0);
	    	String network = networkCds.get(0);
	    	for ( String stationId : stationIds ) {
	    		stationTriplets.add(stationId + ":" + state + ":" + network);
	    	}
	    }
	    try {
			Message.printStatus(2, routine, "Reading stations for the requested time series.");
	    	stations = readStationList (
	    		// List the query parameters for filters, alphabetical.
	    		boundingBox,
    			countyNames,
	   			dcoCodes,
	   			durations,
	   			elementCodes,
	   			elevationMin,
	   			elevationMax,
	   			hucs,
	   			stationNames,
	   			stationTriplets,
	   			// Indicate which data objects to include.
	   			activeOnly,
	   			returnForecastPointMetadata,
	   			returnReservoirMetadata,
	   			returnStationElements );
	    }
	    catch ( Exception e ) {
	    	Message.printWarning(3, routine, "Exception reading stations (" + e + ").");
	    	Message.printWarning(3, routine, e);
	    }

	    // If nothing returned above, try to get reservoirs.
	    int nStations = 0;
	    /*
	    if ( stationTriplets != null ) {
	        nStations = stationTriplets.size();
	    }
	    */
	    if ( stations != null ) {
	        nStations = stations.size();
	    }
	    Message.printStatus(2,routine,"Read " + nStations + " stations from NRCS REST API 'stations' request." );
	    if ( nStations == 0 ) {
	        return tsList;
	    }

	    StringBuilder errorText = new StringBuilder();
	    TS ts;
	    String tsid;
	    String stationTriplet;
	    String state;
	    String stationID;
	    String networkCode;
	    String elementCode;
	    int ordinal = 1;
	    //HeightDepth heightDepth = null;
	    Boolean alwaysReturnDailyFeb29 = false; // Want correct calendar to be used.
	    boolean getFlags = true;
	    // Duration is only used with the 'data' services.
	    DurationType duration = lookupDurationFromInterval ( interval, outputYearType );
	    String beginDateString = null; // Null means no requested period so read all.
	    String endDateString = null;
	    // Date to read data and also to allocate time series.
	    // May be reset below if no requested period.
	    DateTime readStart = null, readEnd = null; // Will be set below to non-null to set time series period.
	    if ( readStartReq != null ) {
	        readStart = new DateTime(readStartReq);
	        beginDateString = formatDateTime(readStartReq,interval,true,0);
	    }
	    if ( readEndReq != null ) {
	        readEnd = new DateTime(readEndReq);
	        endDateString = formatDateTime(readEndReq,interval,true,1);
	    }
	    // Loop through the stations and then the elements for each station:
	    // - each
	    //ReservoirMetadata metaRes;
	    ReservoirMetadata reservoirMetadata = null;
	    for ( Station station : stations ) {
	    	if ( station == null ) {
	    		// Don't process the station;
	    		continue;
	    	}
    		Message.printStatus(2, routine, "Processing station \"" + station.getStationId() + "\".");
	        stationTriplet = station.getStationTriplet();
	        state = parseStateFromTriplet(stationTriplet);
	        stationID = parseStationIDFromTriplet(stationTriplet);
	        networkCode = parseNetworkCodeFromTriplet(stationTriplet);
	    	//List<StationElement> stationElements = Station.getReservoirMetadataList(stations)
	    	List<StationElement> stationElements = station.getStationElements();
		    //for ( StationMetaData meta: stationMetaData ) {
	    	if ( stationElements == null ) {
	    		// No elements for the station.
	    		Message.printStatus(2, routine, "  Have no station elements.");
	    		continue;
	    	}
	    	else {
	    		Message.printStatus(2, routine, "  Have " + stationElements.size() + " station elements. Checking the element code and duration for a match.");
	    	}
	    	// Loop through the station elements:
	    	// - there should be a single match based on the input data
		    for ( StationElement stationElement: stationElements ) {
		    	// The station elements may contain elements and durations that don't match the original request:
		    	// - skip those that don't match
		    	if ( (elementCodes != null) && !elementCodes.isEmpty() ) {
		    		boolean elementMatched = false;
		    		for ( String elementCd : elementCodes ) {
		    			if ( stationElement.getElementCode().equals(elementCd) ) {
		    				elementMatched = true;
		    				break;
		    			}
		    		}
		    		if ( !elementMatched ) {
		    			// Element is not matched so skip the station element.
		    			Message.printStatus(2, routine, "    The station element code \"" + stationElement.getElementCode() + "\" does not match the requested element code.  Skipping.");
		    			continue;
		    		}
		    	}
		    	if ( !duration.equals(stationElement.getDurationName() ) ) {
		    		// The duration does not match so skip.
	    			Message.printStatus(2, routine, "    The station element duration \"" + stationElement.getDurationName() + "\" does not match the requested data interval.  Skipping.");
	    			continue;
		    	}

		    	// If here the station element and duration match the requested so can add a time series.
    			Message.printStatus(2, routine, "    The station element code \"" + stationElement.getElementCode() + "\" matches the requested element code.");
    			Message.printStatus(2, routine, "    The station element duration \"" + stationElement.getDurationName() + "\" matches the requested data interval.");

		        int tscount = 0;
	            elementCode = stationElement.getElementCode();
	            tsid = state + "-" + stationID + "." + networkCode + "." + elementCode + "." + interval;
	            String tzString = "";
	            ++tscount;
	            try {
	                ts = TSUtil.newTimeSeries(tsid,true);
	                ts.setIdentifier(tsid);
	                int intervalBase = ts.getDataIntervalBase();
	                int intervalMult = ts.getDataIntervalMult();
	                ts.setMissing(Double.NaN);
	                ts.setDescription(station.getName());
	                // Time zone is set to the stationDataTimeZone value
	                // If the timeZoneMap is specified and has a key that matches stationDataTimeZone, use the value map value.
	                Integer stationDataTimeZone = station.getDataTimeZone();
	                String tzString0 = "";
	                if ( stationDataTimeZone != null ) {
	                	tzString0 = tzString = "" + stationDataTimeZone; // Default, will be something like "-8" for PST
	                	if ( tzString.startsWith("-") ) {
	                		// GMT in front because something like -8.0 may mess up date/time parser
	                		tzString = "GMT" + tzString;
	                	}
	                }
	                // Always try the lookup because allowing blank key to reset to default value
	                if ( timeZoneMap != null ) {
	                	// Allow lookup on original -8.0 or modified GMT-8.0
	                	String tzString2 = timeZoneMap.get(tzString0);
	                	if ( tzString2 == null ) {
	                		tzString2 = timeZoneMap.get(tzString);
	                	}
	                	if ( tzString2 != null ) {
	                		// Use the requested time zone instead of internal
	                		tzString = tzString2;
	                	}
	                }
	                ts.setDate1Original(parseDateTime(stationElement.getBeginDate())); // Sensor install date.
	                ts.setDate1Original(ts.getDate1Original().setTimeZone(tzString));
	                // Sensor end, or 2100-01-01 00:00 if active (switched to current because don't want 2100)
	                ts.setDate2Original(parseDateTime(stationElement.getEndDate()));
	                ts.setDate2Original(ts.getDate2Original().setTimeZone(tzString));
	                // The following will be reset below if reading data but are OK for discovery mode.
	                if ( readStartReq != null ) {
	                    ts.setDate1(readStartReq);
	                    ts.setDate1(ts.getDate1().setTimeZone(tzString));
	                }
	                else {
	                    // Set the period to read from the data
	                    ts.setDate1(ts.getDate1Original());
	                    ts.setDate1(ts.getDate1().setTimeZone(tzString));
	                }
	                if ( readEndReq != null ) {
	                    ts.setDate2(readEndReq);
	                    ts.setDate2(ts.getDate2().setTimeZone(tzString));
	                }
	                else {
	                    // Set the period to read from the data
	                    ts.setDate2(ts.getDate2Original());
	                    ts.setDate2(ts.getDate2().setTimeZone(tzString));
	                }
	                // Set the data units
	                ts.setDataUnits(stationElement.getStoredUnitCode());
	                ts.setDataUnitsOriginal(stationElement.getOriginalUnitCode());
	                // Set the flag descriptions
	                ts.addDataFlagMetadata(new TSDataFlagMetadata("V", "Valid"));
	                ts.addDataFlagMetadata(new TSDataFlagMetadata("S", "Suspect"));
	                ts.addDataFlagMetadata(new TSDataFlagMetadata("E", "Edited"));
	                // Also set properties from the Station and StationElement objects.
	                boolean setPropertiesFromMetadata = true;
	                String text;
	                Integer i;
	                if ( setPropertiesFromMetadata ) {
	                    // Set time series properties from the timeSeries elements
	                    ts.setProperty("stationTriplet", (stationTriplet == null) ? "" : stationTriplet );
	                    ts.setProperty("state", (state == null) ? "" : state );
	                    ts.setProperty("stationId", (stationID == null) ? "" : stationID );
	                    ts.setProperty("networkCode", (networkCode == null) ? "" : networkCode );
	                    /*
	                    text = meta.getName();
	                    ts.setProperty("name", (text == null) ? "" : text );
	                    text = meta.getActonId();
	                    ts.setProperty("actonId", (text == null) ? "" : text );
	                    text = meta.getShefId();
	                    ts.setProperty("shefId", (text == null) ? "" : text );
	                    text = meta.getBeginDate(); // Date station installed
	                    ts.setProperty("beginDate", (text == null) ? "" : text );
	                    text = meta.getEndDate(); // Date station discontinued
	                    ts.setProperty("endDate", (text == null) ? "" : text );
	                    text = meta.getCountyName();
	                    ts.setProperty("countyName", (text == null) ? "" : text );
	                    bd = meta.getElevation();
	                    ts.setProperty("elevation", (bd == null) ? null : bd.doubleValue() );
	                    text = meta.getFipsCountyCd();
	                    ts.setProperty("fipsCountyCd", (text == null) ? "" : text );
	                    text = meta.getFipsStateNumber();
	                    ts.setProperty("fipsStateNumber", (text == null) ? "" : text );
	                    text = meta.getFipsCountryCd();
	                    ts.setProperty("fipsCountryCd", (text == null) ? "" : text );
	                    text = meta.getHuc();
	                    ts.setProperty("huc", (text == null) ? "" : text );
	                    text = meta.getHud();
	                    ts.setProperty("hud", (text == null) ? "" : text );
	                    bd = meta.getLongitude();
	                    ts.setProperty("longitude", (bd == null) ? null : bd.doubleValue() );
	                    bd = meta.getLatitude();
	                    ts.setProperty("latitude", (bd == null) ? null : bd.doubleValue() );
	                    Integer i = sel.getDataPrecision();
	                    ts.setProperty("dataPrecision", (i == null) ? null : i );
	                    DataSource s = sel.getDataSource();
	                    ts.setProperty("dataSource", (i == null) ? null : "" + s );
	                    int i2 = sel.getOrdinal();
	                    ts.setProperty("ordinal", Integer.valueOf(i2) );
	                    HeightDepth hd = sel.getHeightDepth();
	                    if ( hd == null ) {
	                        ts.setProperty("heightDepthValue", null );
	                        ts.setProperty("heightDepthUnitCd", null );
	                    }
	                    else {
	                        ts.setProperty("heightDepthValue", (hd.getValue() == null) ? null : Double.valueOf(hd.getValue().doubleValue()));
	                        ts.setProperty("heightDepthUnitCd", (hd.getUnitCd() == null) ? "" : hd.getUnitCd() );
	                    }
	                    ts.setProperty("stationDataTimeZone", (stationDataTimeZone == null) ? null : stationDataTimeZone.doubleValue() );
	                    bd = meta.getStationTimeZone();
	                    ts.setProperty("stationTimeZone", (bd == null) ? null : bd.doubleValue() );
	                    if ( metaRes != null ) {
	                        // Set reservoir properties...
	                        bd = metaRes.getElevationAtCapacity();
	                        ts.setProperty("elevationAtCapacity", (bd == null) ? null : bd.doubleValue() );
	                        bd = metaRes.getReservoirCapacity();
	                        ts.setProperty("reservoirCapacity", (bd == null) ? null : bd.doubleValue() );
	                        bd = metaRes.getUsableCapacity();
	                        ts.setProperty("usableCapacity", (bd == null) ? null : bd.doubleValue() );
	                    }
	                    */
	                    text = station.getName();
	                    ts.setProperty("name", (text == null) ? "" : text );
	                    //text = meta.getActonId();
	                    //ts.setProperty("actonId", (text == null) ? "" : text );
	                    text = station.getShefId();
	                    ts.setProperty("shefId", (text == null) ? "" : text );
	                    text = station.getBeginDate(); // Date station installed
	                    ts.setProperty("beginDate", (text == null) ? "" : text );
	                    text = station.getEndDate(); // Date station discontinued
	                    ts.setProperty("endDate", (text == null) ? "" : text );
	                    text = station.getCountyName();
	                    ts.setProperty("countyName", (text == null) ? "" : text );
	                    ts.setProperty("elevation", station.getElevation());
	                    /*
	                    text = meta.getFipsCountyCd();
	                    ts.setProperty("fipsCountyCd", (text == null) ? "" : text );
	                    text = meta.getFipsStateNumber();
	                    ts.setProperty("fipsStateNumber", (text == null) ? "" : text );
	                    text = meta.getFipsCountryCd();
	                    ts.setProperty("fipsCountryCd", (text == null) ? "" : text );
	                    */
	                    text = station.getHuc();
	                    ts.setProperty("huc", (text == null) ? "" : text );
	                    //text = meta.getHud();
	                    //ts.setProperty("hud", (text == null) ? "" : text );
	                    ts.setProperty("longitude", station.getLongitude() );
	                    ts.setProperty("latitude", station.getLatitude() );
	                    ts.setProperty("dataPrecision", stationElement.getDataPrecision());
	                    //DataSource s = sel.getDataSource();
	                    //ts.setProperty("dataSource", (i == null) ? null : "" + s );
	                    ts.setProperty("ordinal", stationElement.getOrdinal());
	                    /*
	                    HeightDepth hd = sel.getHeightDepth();
	                    if ( hd == null ) {
	                        ts.setProperty("heightDepthValue", null );
	                        ts.setProperty("heightDepthUnitCd", null );
	                    }
	                    else {
	                        ts.setProperty("heightDepthValue", (hd.getValue() == null) ? null : Double.valueOf(hd.getValue().doubleValue()));
	                        ts.setProperty("heightDepthUnitCd", (hd.getUnitCd() == null) ? "" : hd.getUnitCd() );
	                    }
	                    */
	                    //ts.setProperty("stationDataTimeZone", (stationDataTimeZone == null) ? null : stationDataTimeZone.doubleValue() );
	                    //bd = meta.getStationTimeZone();
	                    ts.setProperty("stationDataTimeZone", (stationDataTimeZone == null) ? null : stationDataTimeZone );
	                    //ts.setProperty("stationTimeZone", (bd == null) ? null : bd.doubleValue() );
	                    reservoirMetadata = station.getReservoirMetadata();
	                    if ( reservoirMetadata != null ) {
	                        // Set reservoir properties.
	                        ts.setProperty("elevationAtCapacity", reservoirMetadata.getElevationAtCapacity() );
	                        ts.setProperty("reservoirCapacity", reservoirMetadata.getCapacity() );
	                        ts.setProperty("usableCapacity", reservoirMetadata.getUsableCapacity() );
	                    }
	                }
	            }
	            catch ( Exception e ) {
	                Message.printWarning(3,routine,e);
	                errorText.append ( "\n" + e );
	                continue;
	            }
	            if ( readData ) {
	                // Reset the date to read based on the full period available from the StationElement.
	                double missing = ts.getMissing();
	                if ( readStartReq == null ) {
	                    beginDateString = stationElement.getBeginDate();
	                }
	                if ( readEndReq == null ) {
	                    endDateString = stationElement.getEndDate();
	                    if ( endDateString.startsWith("2100-01-01") ) {
	                        // Value of 2100-01-01 00:00:00 is returned for end date for some web service methods.
	                        // Since this value does not make sense for historical data, replace with the current date.
	                        DateTime d = new DateTime(DateTime.DATE_CURRENT); // Any issues with time zone here?
	                        endDateString = d.toString(DateTime.FORMAT_YYYY_MM_DD) +
	                            (endDateString.length() > 10 ? endDateString.substring(10) : "") ;
	                    }
	                }
	                Message.printStatus(2, routine, "    Getting data values for triplet ("+ tscount + " of " +
	                    stationTriplets.size() + ")=\"" + stationTriplet +
	                    "\" elementCode=" + elementCode + " duration=" + duration + " beginDate=" + beginDateString +
	                    " endDate=" + endDateString);
	                // Read the data records.
	                CentralTendencyType centralTendencyType = CentralTendencyType.ALL;
	                // Default for 'periodRef' is END, which is compatible with TSTool (including calendar and water year).
	                if ( periodRefType == null ) {
	                	periodRefType = PeriodRefType.END;
	                }
	                Boolean returnFlags = Boolean.valueOf(true); // Want the flags returned, to add to the time series data flag.
	                // Don't read the o
	                Boolean returnOriginalValues = null; // Default is false, which returns processed values.
	                Boolean returnSuspectData = null; // Default is false (only validated values are returned).
	                // Element codes here are 'ElementCode:HeightDepth:Ordinal':
	                // - omit heightDepth to default to null
	                // - omit ordinal to default to 1
	                List<String> elements = new ArrayList<>();
	                elements.add(elementCode + ":" + ":");
	                try {
	                	List<String> stationTripletsForOne = new ArrayList<>();
	                	stationTripletsForOne.add(stationTriplet);
	                	List<StationData> stationDataList = readDataList (
                			// List the query parameters for filters, alphabetical.
                			duration,
                			elements,
                			stationTripletsForOne,
                			// Period.
                			readStart,
                			readEnd,
                			insertOrUpdateBeginDate, // TODO smalers 2026-04-09 enable later.
                			periodRefType,
                			// Indicate which data objects to include.
                			centralTendencyType,
                			returnFlags,
                			returnOriginalValues,
                			returnSuspectData );
	                	if ( (stationDataList != null) && !stationDataList.isEmpty() ) {
	                		// Process the data.
	                		Double value = null;
	                		String flag = null;
	                		String qaFlag = null;
	                		String qcFlag = null;
	                		String date = null;
	                		DateTime dt = null;
	                		List<Data> dataList = null;
	                		List<DataValue> dataValues = null;
	                		for ( StationData stationData : stationDataList ) {
	                			dataList = stationData.getData();
	                			if ( dataList != null ) {
	                				for ( Data data : dataList ) {
	                					try {
	                						dataValues = data.getValues();
	                						if ( dataValues != null ) {
	                							// Allocate the data space.
	                							String readStartString = dataValues.get(0).getDate();
	                							DateTime readStartFromData = DateTime.parse(readStartString);
	                							String readEndString = dataValues.get(dataValues.size() - 1).getDate();
	                							DateTime readEndFromData = DateTime.parse(readEndString);
	                							ts.setDate1(readStartFromData);
	                							ts.setDate1Original(readStartFromData);
	                							ts.setDate2(readEndFromData);
	                							ts.setDate2Original(readEndFromData);
	                							ts.allocateDataSpace();
	                							for ( DataValue dataValue : dataValues ) {
	                								value = dataValue.getValue();
	                								qaFlag = dataValue.getQaFlag();
	                								qcFlag = dataValue.getQcFlag();
	                								// Set the flag:
	                								// - use qcFlag,qaFlag:
	                								// - use empty strings if null
	                								flag = null;
	                								if ( qcFlag == null ) {
	                									qcFlag = "";
	                								}
	                								if ( qaFlag == null ) {
	                									qaFlag = "";
	                								}
	                								if ( qcFlag.isEmpty() && qaFlag.isEmpty() ) {
	                									// No flags.
	                									flag = null;
	                								}
	                								else {
	                									// Have one or both flags.
	                									flag = qcFlag + "," + qaFlag;
	                								}
	                								date = dataValue.getDate();
	                								dt = DateTime.parse(date);
	                								if ( value == null ) {
	                									// Might still have a flag.
	                									if ( (flag != null) && !flag.isEmpty() ) {
	                										// Have a flag so set.
	                										ts.setDataValue(dt,missing,flag,0);
	                									}
	                									//Message.printStatus(2, routine, "Date " + dt + " value=" + value);
	                								}
	                								else {
	                									// Value is not missing but flag may be null.
	                									if ( flag == null ) {
	                										// No flag to set.
	                										ts.setDataValue(dt, value.doubleValue());
	                									}
	                									else {
	                										// Have a flag to set.
	                										ts.setDataValue(dt, value.doubleValue(),flag,0);
	                									}
	                									//Message.printStatus(2, routine, "Date " + dt + " value=" + value);
	                								}
	                							}
	                						}
	                					}
	                					catch ( Exception e ) {
	                						// Handle exceptions for the single data point.
	                					}
	                            	}
	                			}
	                		}
	                	}
	                }
	                catch ( Exception e ) {
                        String message = "Error getting station data values (" + e + ").";
                        Message.printWarning(3, routine, message);
                        Message.printWarning(3, routine, e);
                        errorText.append("\n"+ message);
	                }
	            }
	            tsList.add(ts);
		    }
	    }
	    if ( errorText.length() > 0 ) {
	        throw new RuntimeException ( errorText.toString() );
	    }
	    return tsList;
	}

	/**
	 * Read time series catalog.
	 * @param dataTypeReq Requested data type, which is the element code, or  "*" to read all data types, or null to use default of "*".
	 * @param dataIntervalReq Requested data interval (e.g., "IrregSecond") or "*" to read all intervals, or null to use default of "*".
	 * @param ifp input filter panel with "where" conditions
	 */
	public List<TimeSeriesCatalog> readTimeSeriesCatalogList ( String dataTypeReq, String dataIntervalReq, InputFilter_JPanel ifp ) {
		String routine = getClass().getSimpleName() + ".readTimeSeriesCatalogList";

		// Filter data.
		Double [] boundingBox = null;
		List<String> countyNames = new ArrayList<>();
		List<String> dcoCodes = new ArrayList<>();
		List<String> durations = new ArrayList<>();
		List<String> elements = new ArrayList<>();
		Double elevationMin = null;
		Double elevationMax = null;
		List<String> hucs = new ArrayList<>();
		List<String> stationNames = new ArrayList<>();
		List<String> stationTriplets = new ArrayList<>();
		// Indicate which data objects to include.
		Boolean activeOnly = null; // Default is true.
		Boolean returnForecastPointMetadata = Boolean.valueOf(true);
		Boolean returnReservoirMetadata = Boolean.valueOf(true);
		Boolean returnStationElements = Boolean.valueOf(true);

		if ( (dataTypeReq != null) && !dataTypeReq.isEmpty() && !dataTypeReq.equals("*") ) {
			// Filter based on the data type:
			// - the data type may contain the name as in "Element - name" so strip off the name
			// - currently do not allow wildcard but may add in the future
			int pos = dataTypeReq.indexOf ( " -" );
			if ( pos > 0 ) {
				dataTypeReq = dataTypeReq.substring(0,pos);
			}
			// The data type in TSTool is the same as the NRCS AWDB element so use the value as is.
			Message.printStatus(2, routine, "Reading NRCS AWDB time series catalog for data type \"" + dataTypeReq + "\".");
			elements.add(dataTypeReq);
		}

		if ( (dataIntervalReq != null) && !dataIntervalReq.isEmpty() && !dataIntervalReq.equals("*") ) {
			// Filter based on the data interval:
			// - must convert TSTool interval to the NRCS AWDB duration
			String duration = translateDataIntervalToDuration(dataIntervalReq);
			Message.printStatus(2, routine, "Reading NRCS AWDB time series catalog for duration \"" + duration + "\".");
			durations.add ( duration );
		}

		/* TODO smalers 2026-04-13 enable later if the input filter is added to the command.
		// Add query parameters based on the input filter:
		// - this includes list type parameters and specific parameters to match database values
		int numFilterWheres = 0; // Number of filter where clauses that are added.
		if ( ifp != null ) {
        	int nfg = ifp.getNumFilterGroups ();
        	InputFilter filter;
        	for ( int ifg = 0; ifg < nfg; ifg++ ) {
            	filter = ifp.getInputFilter ( ifg );
            	//Message.printStatus(2, routine, "IFP whereLabel =\"" + whereLabel + "\"");
            	boolean special = false; // TODO smalers 2022-12-26 might add special filters.
            	if ( special ) {
            	}
            	else {
            		// Add the query parameter to the URL.
			    	filter = ifp.getInputFilter(ifg);
			    	String queryClause = WebUtil.getQueryClauseFromInputFilter(filter,ifp.getOperator(ifg));
			    	if ( Message.isDebugOn ) {
			    		Message.printStatus(2,routine,"Filter group " + ifg + " where is: \"" + queryClause + "\"");
			    }
			    	if ( queryClause != null ) {
			    	requestUrl.append("&" + queryClause);
			    		++numFilterWheres;
			    	}
            	}
        	}
		}
		*/

		// Read the station list:
		// - will only add TimeSeriesCatalog when there are elements

		List<Station> stations = null;
		try {
			stations = readStationList (
				// List the query parameters for filters, alphabetical.
				boundingBox,
				countyNames,
				dcoCodes,
				durations,
				elements,
				elevationMin,
				elevationMax,
				hucs,
				stationNames,
				stationTriplets,
				// Indicate which data objects to include.
				activeOnly,
				returnForecastPointMetadata,
				returnReservoirMetadata,
				returnStationElements
			);
		}
		catch ( Exception e ) {
			Message.printWarning(3, routine, "Error reading statinos (" + e + ")." );
			// Rethrow the exception.
			throw new RuntimeException ( e );
		}

		List<TimeSeriesCatalog> tscatalogList = new ArrayList<>();
		// Set the current time string for the end date for active stations.
		DateTime current = new DateTime ( DateTime.DATE_CURRENT | DateTime.PRECISION_MINUTE );
		String currentDateString = current.toString();
		if ( (stations != null ) && !stations.isEmpty() ) {
			Message.printStatus(2,routine,"Read " + stations.size() + " stations to check for time series catalog.");
			for ( Station station : stations ) {
				// Because the TimeSeriesCatalog is a join of station and element data, the 'stationElements' data are required.
				List<StationElement> stationElements = station.getStationElements();

				if ( (stationElements != null) && !stationElements.isEmpty() ) {
					for ( StationElement stationElement : stationElements ) {
						if ( stationElement != null ) {
							TimeSeriesCatalog tscatalog = new TimeSeriesCatalog();

							// Standard properties expected by TSTool:
							// - datatype - use the requested, which matches the NRCS AWDB element code
							// - interval is as requested, which matches the TSTool interval
							tscatalog.setDataInterval(dataIntervalReq);
							tscatalog.setDataType(dataTypeReq);

							// Set station data.
							tscatalog.setStationId ( station.getStationId() );
							tscatalog.setStationName ( station.getName() );
							tscatalog.setStationTriplet ( station.getStationTriplet() );
							tscatalog.setDataSource ( station.getNetworkCode() );
							tscatalog.setStationState ( station.getStateCode() );
							tscatalog.setStationNetwork ( station.getNetworkCode() );
							tscatalog.setStationDco ( station.getDcoCode() );
							tscatalog.setStationCounty ( station.getCountyName() );
							tscatalog.setStationHuc ( station.getHuc() );
							tscatalog.setStationElevation ( station.getElevation() );
							tscatalog.setStationLatitude ( station.getLatitude() );
							tscatalog.setStationLongitude ( station.getLongitude() );
							tscatalog.setStationDataTimeZone ( station.getDataTimeZone() );
							tscatalog.setStationShefId ( station.getShefId() );
							tscatalog.setStationOperator ( station.getOperator() );

							// Have enough data to set the time series identifier:
							// - this should be the same as the table model getTimeSeriesIdentifierFromTableModel method,
							//   but the table model is not active here
							tscatalog.setTSID(tscatalog.toString());
							tscatalog.setDataStore(this.getName());

							tscatalog.setElementCode(stationElement.getElementCode());
							tscatalog.setElementOrdinal(stationElement.getOrdinal());
							tscatalog.setElementHeightDepth(stationElement.getHeightDepth());
							tscatalog.setElementDurationName(stationElement.getDurationName());
							tscatalog.setElementDataPrecision(stationElement.getDataPrecision());
							tscatalog.setElementDataPrecision(stationElement.getDataPrecision());
							tscatalog.setElementDataUnits(stationElement.getStoredUnitCode());
							tscatalog.setElementDataUnitsOriginal(stationElement.getOriginalUnitCode());
							tscatalog.setElementBeginDate(stationElement.getBeginDate());
							String endDate = stationElement.getEndDate();
							if ( (endDate != null) && endDate.startsWith("2100-01-01") ) {
								// Placeholder for active data:
								// - set to the current date
								endDate = currentDateString;
							}
							else {
								// Actual date.
								tscatalog.setElementEndDate(stationElement.getEndDate());
							}
							tscatalog.setElementDerivedData(stationElement.getDerivedData());

							// Save extra information.
							if ( station.getForecastPoint() != null ) {
								tscatalog.setStationIsForecastPoint(Boolean.valueOf(true));
							}
							else {
								tscatalog.setStationIsForecastPoint(Boolean.valueOf(false));
							}
							if ( station.getReservoirMetadata() != null ) {
								tscatalog.setStationIsReservoir(Boolean.valueOf(true));
							}
							else {
								tscatalog.setStationIsReservoir(Boolean.valueOf(false));
							}

							// Save the catalog in the list.
							tscatalogList.add(tscatalog);
						}
						else {
							Message.printStatus(2, routine, "  Station element is null.");
						}
					}
				}
				else {
					Message.printStatus(2, routine, "Station " + station.getStationId() + " has null or empty 'stationElement'.");
				}
			}
		}

		// Check the catalog list for problems.
		for ( TimeSeriesCatalog tscatalog : tscatalogList ) {
			// Make sure that the time series identifier is unique.
			boolean duplicateTsid = false;
			for ( TimeSeriesCatalog tscatalog2 : tscatalogList ) {
				if ( tscatalog == tscatalog2 ) {
					// Don't compare with itself.
					continue;
				}
				if ( tscatalog.getTSID().equals(tscatalog2.getTSID()) ) {
					// The time series identifier is not unique.
					duplicateTsid = true;
					break;
				}
			}
			if ( duplicateTsid ) {
				tscatalog.addProblem("TSID is not unique.");
			}
		}

		return tscatalogList;
	}

    /**
     * Read time series metadata, which results in a query that joins station, station_type, point, point_class, and point_type.
     */
    List<TimeSeriesCatalog> readTimeSeriesMeta ( String dataTypeReq, String dataIntervalReq, InputFilter_JPanel ifp ) {
    	// Remove note from data type.
	   	int pos = dataTypeReq.indexOf(" - ");
	   	if ( pos > 0 ) {
		   	dataTypeReq = dataTypeReq.substring(0, pos);
	   	}
	   	pos = dataIntervalReq.indexOf(" - ");
	   	if ( pos > 0 ) {
		   	dataIntervalReq = dataIntervalReq.substring(0, pos).trim();
	   	}
	   	// By default all time series are included in the catalog:
	   	// - the filter panel options can be used to constrain
	    return readTimeSeriesCatalogList ( dataTypeReq, dataIntervalReq, ifp );
	}

    /**
     * Read the version from the web service, used when processing #@require commands in TSTool.
     * TODO smalers 2023-01-03 need to figure out if a version is available.
     */
    private String readVersion () {
		//checkTokenExpiration();
    	return "";
    }

    /**
     * Set the time series properties from the TimeSeriesCatalog.
     * @param ts the time series to update
     * @param tscatalog the time series catalog matching the time series
     */
    private void setTimeSeriesProperties ( TS ts, TimeSeriesCatalog tscatalog ) {
    	// Set all the NRCS AWDB Cloud properties that are known for the time series:
    	// - use names that match the NRCS AWDB API to allow using the API documentation

    	//ts.setProperty("station.description", tscatalog.getStationDescription());
    	ts.setProperty("station.elevation", tscatalog.getStationElevation());
    	ts.setProperty("station.id", tscatalog.getStationId());
    	ts.setProperty("station.latitude", tscatalog.getStationLatitude());
    	ts.setProperty("station.longitude", tscatalog.getStationLongitude());
    	ts.setProperty("station.name", tscatalog.getStationName());
    }

    /**
     * Translate a TSTool data interval (e.g., "Hour") to NRCS AWDB duration.
     * SEMIMONTHLY, WATER_YEAR, SEASONAL or not handled.
     * @param dataInterval TSTool data interval
     * @return the NRCS AWDB duration
     */
	public String translateDataIntervalToDuration ( String dataInterval ) {
		if ( dataInterval.equalsIgnoreCase("Hour") ) {
			return "HOURLY";
		}
		else if ( dataInterval.equalsIgnoreCase("Day") ) {
			return "DAILY";
		}
		else if ( dataInterval.equalsIgnoreCase("Month") ) {
			return "MONTHLY";
		}
		else if ( dataInterval.equalsIgnoreCase("Year") ) {
			return "CALENDAR_YEAR";
		}
		else {
			return null;
		}
	}

}