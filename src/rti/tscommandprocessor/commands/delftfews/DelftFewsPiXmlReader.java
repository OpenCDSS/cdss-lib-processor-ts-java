package rti.tscommandprocessor.commands.delftfews;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import RTi.DMI.DMIUtil;
import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSUtil;
import RTi.Util.IO.XmlToolkit;
import RTi.Util.Message.Message;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;

/**
 * Class to read Delft FEWS PI XML files.
 * @author sam
 *
 */
public class DelftFewsPiXmlReader {

	/**
	 * PI XML file
	 */
	private String inputFile = null;

	/**
	 * List of time series that were read by the reader.
	 */
	private List<TS> tsList = new ArrayList<TS>();

	/**
	 * List of ensembles that were read by the reader.
	 */
	private List<TSEnsemble> ensembleList = new ArrayList<TSEnsemble>();

	/**
	 * Construct a reader.
	 */
	public DelftFewsPiXmlReader ( String inputFile ) throws FileNotFoundException {
		// Open the file
		File f = new File(inputFile);
		if ( !f.exists() ) {
			throw new FileNotFoundException("PI XML file \"" + inputFile + "\" does not exist.");
		}
		this.inputFile = inputFile;
	}
	
	/**
	 * Create and initialize a time series object from the <series><header> element.
	 */
	private TS createTimeSeriesFromHeader ( XmlToolkit xtk, Node headerNode, DateTime inputStart, DateTime inputEnd ) throws IOException {
		TS ts = null;
		NodeList headerNodeChildren = headerNode.getChildNodes();
		String type = xtk.getNodeValue("type", headerNodeChildren);
		String locationId = xtk.getNodeValue("locationId", headerNodeChildren);
		String parameterId = xtk.getNodeValue("parameterId", headerNodeChildren);
		String ensembleId = xtk.getNodeValue("ensembleId", headerNodeChildren);
		String ensembleMemberIndex = xtk.getNodeValue("ensembleMemberIndex", headerNodeChildren);
		Node timeStepNode = xtk.getNode("timeStep", headerNodeChildren);
		String timeStepUnit = xtk.getNodeAttribute("unit", timeStepNode);
		String timeStepMultiplier = xtk.getNodeAttribute("multiplier", timeStepNode);
		Node startDateNode = xtk.getNode("startDate", headerNodeChildren);
		String startDateDate = xtk.getNodeAttribute("date", startDateNode);
		String startDateTime = xtk.getNodeAttribute("time", startDateNode);
		Node endDateNode = xtk.getNode("endDate", headerNodeChildren);
		String endDateDate = xtk.getNodeAttribute("date", endDateNode);
		String endDateTime = xtk.getNodeAttribute("time", endDateNode);
		Node forecastDate = xtk.getNode("forecastDate", headerNodeChildren);
		String forecastDateDate = xtk.getNodeAttribute("date", forecastDate);
		String forecastDateTime0 = xtk.getNodeAttribute("time", forecastDate);
		String missVal = xtk.getNodeValue("missVal", headerNodeChildren);
		String stationName = xtk.getNodeValue("stationName", headerNodeChildren);
		String lat = xtk.getNodeValue("lat", headerNodeChildren);
		String lon = xtk.getNodeValue("lon", headerNodeChildren);
		String x = xtk.getNodeValue("x", headerNodeChildren);
		String y = xtk.getNodeValue("y", headerNodeChildren);
		String z = xtk.getNodeValue("z", headerNodeChildren);
		String units = xtk.getNodeValue("units", headerNodeChildren);
		// Create the time series
		DelftFewsPiXmlToolkit dtk = new DelftFewsPiXmlToolkit();
		String tsInterval = dtk.convertTimestepToInterval(timeStepUnit, timeStepMultiplier);
		try {
			ts = TSUtil.newTimeSeries(tsInterval,false);
			String tsid = locationId + ".FEWS." + parameterId + "." + tsInterval;
			if ( !ensembleMemberIndex.isEmpty() ) {
				tsid = tsid + ".[" + ensembleMemberIndex + "]";
			}
			ts.setIdentifier(tsid);
			DateTime inputStartOrig = DateTime.parse(startDateDate + "T" + startDateTime);
			DateTime inputEndOrig = DateTime.parse(endDateDate + "T" + endDateTime);
			DateTime forecastDateTime = DateTime.parse(forecastDateDate + "T" + forecastDateTime0);
			if ( inputStart != null ) {
				ts.setDate1(inputStart);
			}
			else {
				ts.setDate1(inputStartOrig);
			}
			if ( inputEnd != null ) {
				ts.setDate2(inputEnd);
			}
			else {
				ts.setDate2(inputEndOrig);
			}
			ts.setDate1Original(inputStartOrig);
			ts.setDate2Original(inputEndOrig);
			ts.setDataUnitsOriginal(units);
			ts.setDataUnits(units);
			// Set missing - TODO SAM 2016-01-21 is anything used besides NaN
			if ( missVal.equalsIgnoreCase("NaN") ) {
				ts.setMissing(Double.NaN);
			}
			// Set time series properties
			ts.setProperty("type", DMIUtil.isMissing(lat)? null : type);
			ts.setProperty("locationId", DMIUtil.isMissing(lat)? null : locationId);
			ts.setProperty("parameterId", DMIUtil.isMissing(lat)? null : parameterId);
			ts.setProperty("ensembleId", DMIUtil.isMissing(lat)? null : ensembleId);
			ts.setProperty("ensembleMemberIndex", DMIUtil.isMissing(ensembleMemberIndex)? null : ensembleMemberIndex);
			ts.setProperty("lat", DMIUtil.isMissing(lat)? null : Double.parseDouble(lat));
			ts.setProperty("lon", DMIUtil.isMissing(lon)? null : Double.parseDouble(lon));
			ts.setProperty("forecastDate", (forecastDateTime == null)? null : forecastDateTime);
			// Set missing value as string property so it can be checked against event data value
			ts.setProperty("missVal", DMIUtil.isMissing(missVal)? null : missVal);
			ts.setProperty("stationName", DMIUtil.isMissing(x)? null : stationName);
			ts.setProperty("x", DMIUtil.isMissing(x)? null : Double.parseDouble(x));
			ts.setProperty("y", DMIUtil.isMissing(y)? null : Double.parseDouble(y));
			ts.setProperty("z", DMIUtil.isMissing(z)? null : Double.parseDouble(z));
		}
		catch ( Exception e ) {
			throw new IOException ( "Error creating new time series (" + e + ")" );
		}
		return ts;
	}

	/**
	 * Return the list of time series ensembles read by the reader, guaranteed to be non-null.
	 * @return list of time series ensembles read by the reader
	 */
	public List<TSEnsemble> getEnsembleList () {
		return this.ensembleList;
	}
	
	/**
	 * Return the list of time series read by the reader, guaranteed to be non-null.
	 * @return list of time series read by the reader
	 */
	public List<TS> getTimeSeriesList () {
		return this.tsList;
	}
	
	/**
	 * Read the time series data from XML
	 * @param seriesNode the <series> element for the time series
	 * @param ts time series to process
	 */
	private void readTimeSeriesData ( XmlToolkit xtk, Node seriesNode, TS ts, String missingVal ) {
		List<Node> eventList = xtk.getNodes("event", seriesNode.getChildNodes());
		double missingDouble = ts.getMissing();
		double valueDouble = 0.0; // <event value> as double
		DateTime dt;
		for ( Node event: eventList ) {
			try {
				String eventDate = xtk.getNodeAttribute("date", event);
				String eventTime = xtk.getNodeAttribute("time", event);
				dt = DateTime.parse(eventDate + "T" + eventTime);
				String eventValue = xtk.getNodeAttribute("value", event);
				String eventFlag = xtk.getNodeAttribute("flag", event);
				if ( eventValue.equalsIgnoreCase(missingVal) ) {
					valueDouble = missingDouble;
				}
				else {
					valueDouble = Double.parseDouble(eventValue);
				}
				ts.setDataValue(dt, valueDouble, eventFlag, -1);
			}
			catch ( Exception e ) {
				// For now ignore errors
				continue;
			}
		}
	}
	
	/**
	 * Read time series and/or ensembles from the PI XML file.
	 * Use the get methods to retrieve the time series and ensemble lists.
	 * @param inputFile path to input file to read
	 * @param inputStart start of period to read or null to read all
	 * @param inputEnd end of period to read or null to read all
	 * @param newUnits units for output (currently ignored)
	 * @param output indicate what to output:  "Ensembles", "TimeSeries", or "TimeSeriesAndEnsembles" 
	 */
    public void readTimeSeriesList (
        DateTime inputStart, DateTime inputEnd, String newUnits, String output, boolean readData, List<String> problems ) {
    	// For now parse the XML. In the future may use DELFT jar files, etc. but don't want the dependencies right now
    	readTimeSeriesListParseXml ( inputStart, inputEnd, newUnits, output, readData, problems );
    }
    
	/**
	 * Read time series and/or ensembles from the PI XML file by parsing the XML file directly.
	 * Use the get methods to retrieve the time series and ensemble lists.
	 * @param inputFile path to input file to read
	 * @param inputStart start of period to read or null to read all
	 * @param inputEnd end of period to read or null to read all
	 * @param newUnits units for output (currently ignored)
	 * @param output indicate what to output:  "Ensembles", "TimeSeries", or "TimeSeriesAndEnsembles" 
	 */
    private void readTimeSeriesListParseXml (
        DateTime inputStart, DateTime inputEnd, String newUnits, String output, boolean readData, List<String> problems ) {
    	String routine = getClass().getSimpleName() + ".readTimeSeriesListParseXml";
    	boolean doReadTs = false;
    	boolean doReadEnsembles = false;
    	if ( output.indexOf("TimeSeries") >= 0 ) {
    		doReadTs = true;
    	}
    	if ( output.indexOf("Ensemble") >= 0 ) {
    		doReadEnsembles = true;
    	}
    	XmlToolkit xtk = new XmlToolkit();
    	// Get Document Builder
    	Message.printStatus(2,routine,"Creating document builder factory...");
    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	Message.printStatus(2,routine,"Creating document builder...");
    	DocumentBuilder builder = null;
    	try {
    		builder = factory.newDocumentBuilder();
    	}
    	catch ( ParserConfigurationException e ) {
    		problems.add ( "Error creating XML document builder (" + e + ")." );
    		return;
    	}
    	// Build the document
    	Message.printStatus(2,routine,"Parsing the XML document...");
    	Document document = null;
    	try {
    		document = builder.parse(new File(inputFile));
    	}
    	catch ( Exception e ) {
    		problems.add ( "Error parsing XML document (" + e + ")." );
    		return;
    	}
    	// Normalize the document
    	Message.printStatus(2,routine,"Normalizing the XML document...");
    	document.getDocumentElement().normalize();
    	// Get the root node - should be <TimeSeries>
    	Element root = document.getDocumentElement();
    	if ( !root.getNodeName().equals("TimeSeries") ) {
    		// Don't understand the file
    		problems.add ( "Expecting <TimeSeries> root element - found \"" + root.getNodeName() + "\"." );
    		return;
    	}
    	Message.printStatus(2,routine,"Root node name=\"" + root.getNodeName() + "\"");
    	// Get the elements to use for the table, or children under the root if not specified
    	NodeList seriesNodeList = null;
    	seriesNodeList = document.getElementsByTagName("series");
    	if ( seriesNodeList.getLength() == 0 ) {
    		problems.add("No <series> elements were found.");
    		return;
    	}
    	// Loop through the <series> nodes and read time series for each.
    	Message.printStatus(2, routine, "XML file has " + seriesNodeList.getLength() + " <series> elements to process into time series.");
    	TS ts;
    	for ( int i = 0; i < seriesNodeList.getLength(); i++ ) {
    		Node seriesNode = seriesNodeList.item(i);
    		Node headerNode = xtk.getNode("header", seriesNode.getChildNodes());
    		if ( headerNode == null ) {
    			problems.add("No <header> element in <series> element " + (i + 1) + " - cannot initialize time series");
    		}
    		else {
    			try {
    				ts = createTimeSeriesFromHeader ( xtk, headerNode, inputStart, inputEnd );
    				if ( readData ) {
	    				// Read the data values
    					ts.allocateDataSpace();
	    				readTimeSeriesData ( xtk, seriesNode, ts, (String)ts.getProperty("MissingVal") );
    				}
    				// If reading time series, add to the time series list
    				if ( doReadTs) {
    					this.tsList.add(ts);
    				}
    				if ( doReadEnsembles ) {
    					// Add to the proper ensemble
    					boolean found = false;
    					for ( TSEnsemble ensemble : this.ensembleList ) {
    						// Figure out which ensemble the time series belongs to
    					}
    				}
    			}
    			catch ( Exception e ) {
    				problems.add ( "Error creating time series header (" + e + ")." );
    			}
    		}
    	}
    }
}