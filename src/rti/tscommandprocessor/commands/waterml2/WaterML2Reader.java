// WaterML2Reader - Class to read a WaterML 2 file and return time series.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.waterml2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

// Instances are explicitly declared with full path to avoid conflict with similar Apache classes
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;

//import com.sun.org.apache.xerces.internal.dom.ElementNSImpl;

import rti.tscommandprocessor.commands.wateroneflow.waterml.WaterMLVersion;

import RTi.TS.TS;
import RTi.TS.TSDataFlagMetadata;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.ReaderInputStream;
import RTi.Util.IO.XmlToolkit;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeRange;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;
import net.opengis.gml._3.AbstractFeatureType;
import net.opengis.gml._3.CoordinatesType;
import net.opengis.gml._3.DirectPositionType;
import net.opengis.gml._3.FeatureCollectionType;
import net.opengis.gml._3.FeaturePropertyType;
import net.opengis.gml._3.MetaDataPropertyType;
import net.opengis.gml._3.PointType;
import net.opengis.gml._3.ReferenceType;
import net.opengis.gml._3.StringOrRefType;
import net.opengis.gml._3.TimeInstantPropertyType;
import net.opengis.gml._3.TimeInstantType;
import net.opengis.gml._3.TimePositionType;
import net.opengis.om._2.NamedValuePropertyType;
import net.opengis.om._2.NamedValueType;
import net.opengis.om._2.OMObservationPropertyType;
import net.opengis.om._2.OMObservationType;
import net.opengis.om._2.OMProcessPropertyType;
import net.opengis.om._2.ObservationContextPropertyType;
import net.opengis.om._2.TimeObjectPropertyType;
import net.opengis.sampling._2.SamplingFeatureComplexPropertyType;
import net.opengis.sampling._2.SamplingFeatureComplexType;
import net.opengis.samplingspatial._2.ShapeType;
import net.opengis.swe._2.QualityPropertyType;
import net.opengis.waterml._2.CategoricalTimeseriesType;
import net.opengis.waterml._2.CollectionType;
import net.opengis.waterml._2.DocumentMetadataPropertyType;
import net.opengis.waterml._2.DocumentMetadataType;
import net.opengis.waterml._2.MeasureTVPType;
import net.opengis.waterml._2.MeasureType;
import net.opengis.waterml._2.MeasurementTimeseriesType;
import net.opengis.waterml._2.MeasurementTimeseriesType.Point;
import net.opengis.waterml._2.MonitoringPointType;
import net.opengis.waterml._2.ObservationProcessType;
import net.opengis.waterml._2.TVPMeasurementMetadataPropertyType;
import net.opengis.waterml._2.TVPMeasurementMetadataType;

/**
Class to read a WaterML 2 file and return time series.
*/
public class WaterML2Reader
{
	

/**
WaterML 2.0 name space as URI
*/
private static final String WATERML_2_0_NS_URI = "http://www.opengis.net/waterml/2.0";
    
/**
 * WaterML content as string.
 */
private String watermlString = "";

/**
 * URL used to read the WaterML.
 */
private String url = "";

/**
 * File containing WaterML.
 */
private File watermlFile = null;

/**
 * List of warning messages encountered during read - a simple list of strings handled in calling code
 */
private List<String> warningMessages = new ArrayList<String>();

/**
 * List of failure messages encountered during read - a simple list of strings handled in calling code
 */
private List<String> failureMessages = new ArrayList<String>();

/**
 * XML toolkit that is used to process XML.
 */
private XmlToolkit xmlToolkit = new XmlToolkit();

/**
Constructor.
@param url the full URL query used to make the query (specify as null or empty if read from a file)
@param outputFile the output file to write results (WaterML, etc.) to
@param intervalHint the interval that should be used for the output time series (if null use daily) - this
will be enhanced in the future as more is understood about WaterML
*/
public WaterML2Reader ( URL url ) throws IOException
{
    this.watermlString = null;
    this.url = url.toString();
    this.watermlFile = null;
    
    // Read the content from the URL
    
    this.watermlString = IOUtil.readFromURL(this.url);
    
}

/**
Constructor.
@param watermlFile the WaterML 2 file to read.
*/
public WaterML2Reader ( File watermlFile ) throws IOException
{
    this.watermlString = null;
    this.url = null;
    
    // Read the content from the file
    
    StringBuilder fileContents = new StringBuilder();
    BufferedReader reader = new BufferedReader(new FileReader(watermlFile));
    String ls = System.getProperty ( "line.separator" );
    String line;
    int count = 0;
    while ( (line = reader.readLine()) != null ) {
        ++count;
        if ( count > 1 ) {
            fileContents.append(ls);
        }
        fileContents.append(line);
    }
    this.watermlString = fileContents.toString();
    reader.close();
}

/**
Constructor.
@param waterMLString the string from which to parse the time series (may be result of in-memory
web service call, or text read from WaterML file)
@param outputFile the output file to write results (WaterML, etc.) to
@param intervalHint the interval that should be used for the output time series (if null use daily) - this
will be enhanced in the future as more is understood about WaterML
*/
public WaterML2Reader ( String waterMLString, File outputFile )
{
    this.watermlString = waterMLString;
    this.url = null;
    this.watermlFile = null;
}

/**
Determine the WaterML version.
@param dom DOM from WaterML
@throws IOException
*/
private WaterMLVersion determineWaterMLVersion(Document dom)
throws IOException
{   String routine = getClass().getSimpleName() + "determineWaterMLVersion";
    String xmlns = dom.getDocumentElement().getAttribute("xmlns");
    Message.printStatus ( 2, routine, "DOM xmlns=\"" + xmlns + "\"" );
    String xmlns_ns1 = dom.getDocumentElement().getAttribute("xmlns:wml2");
    WaterMLVersion version = null;

    if ( WATERML_2_0_NS_URI.equals(xmlns) || WATERML_2_0_NS_URI.equals(xmlns_ns1) ) {
        version = WaterMLVersion.STANDARD_2_0;
    }
    else {
    	// TODO smalers 2017-07-01 is this needed?
        TransformerFactory tf = TransformerFactory.newInstance();
        String text = "See WaterML output file";
        try {
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(dom), new StreamResult(writer));
            text = writer.getBuffer().toString().replaceAll("\n|\r", "");
        }
        catch ( TransformerConfigurationException e ) {
        }
        catch ( TransformerException e ) {
        }
        text = text.length() <= 2000 ? text : text.substring(0,2000) + "...";
        throw new IOException("Unable to determine WaterML version based on xmlns \"" + xmlns +
            "\" WaterML version not supported.  Returned content may describe error:  " + text);
    }
    Message.printStatus(2,routine, "WaterML version is " + version);
    return version;
}

/**
Return the list of failure messages from the read.
*/
public List<String> getFailureMessages ()
{
    return this.failureMessages;
}

/**
Return the list of warning messages from the read.
*/
public List<String> getWarningMessages ()
{
    return this.warningMessages;
}

/**
 * Parse a GML TimePeriodType object into the beginning and end times.
 * @param gmlTimePeriodType GML TimePeriodType object
 * @return DateTimeRange with begin and end of the period.
 */
private DateTimeRange parseGMLTimePeriodType ( net.opengis.gml._3.TimePeriodType gmlTimePeriodType ) throws Exception {
	// Parse the begin date/time
	TimePositionType beginTimePosition = gmlTimePeriodType.getBeginPosition();
	List<String> beginTimePositionList = beginTimePosition.getValue();
	StringBuilder beginString = new StringBuilder();
	for ( String s : beginTimePositionList ) {
		// TODO SAM 2017-06-30 not sure what to do if multiple parts - check instantaneous data
		beginString.append(s);
	}
	DateTime beginDateTime = DateTime.parse(beginString.toString(),DateTime.FORMAT_ISO_8601);
	// Parse the end date/time
	TimePositionType endTimePosition = gmlTimePeriodType.getEndPosition();
	List<String> endTimePositionList = endTimePosition.getValue();
	StringBuilder endString = new StringBuilder();
	for ( String s : endTimePositionList ) {
		// TODO SAM 2017-06-30 not sure what to do if multiple parts - check instantaneous data
		endString.append(s);
	}
	DateTime endDateTime = DateTime.parse(endString.toString(),DateTime.FORMAT_ISO_8601);
	// The following seems to return null sometimes?  TODO SAM 2017-06-30 not sure if can use
	//TimeInstantPropertyType beginTimeInstant = gmlTimePeriodType.getBegin();
	return new DateTimeRange ( beginDateTime, endDateTime );
}

/**
 * Parse an instantaneous time.
 * @param GML TimeInstantPropertyType instance.
 * @return 
 */
private DateTime parseOmTimeInstant ( TimeInstantPropertyType omTimeInstantProperty ) throws Exception {
	TimeInstantType gmlTimeInstant = omTimeInstantProperty.getTimeInstant();
	TimePositionType gmlTimePosition = gmlTimeInstant.getTimePosition();
	List<String> timePositionList = gmlTimePosition.getValue();
	StringBuilder sb = new StringBuilder();
	for ( String s : timePositionList ) {
		// TODO SAM 2017-06-30 not sure what to do if multiple parts - check instantaneous data
		sb.append(s);
	}
	// Seems to be null with USGS so can't call getIndeterminatePosition()...
	//TimeIndeterminateValueType gmlTimeIndeterminateValue = gmlTimePosition.getIndeterminatePosition();
	// Parsing does not automatically handle hundredths of a second so trim off . through before time zone sign
	//  <gml:timePosition>2017-06-29T04:34:37.787-04:00</gml:timePosition>
	String timeString = sb.toString();
	return DateTime.parse ( timeString, DateTime.FORMAT_ISO_8601 );
}

/**
Read the time series list from the string WaterML 2.
@param readUsingApi indicate whether the API (compiled classes) should be used, or parse the DOM.
@param interval indicates the interval for data in the file, needed as a hint because there is nothing
in the file that indicates that data are daily values, etc.
@param requireDataToMatchInterval if true, require that all date/times for data fall on the exact interval; if false,
allow the date/time to truncate
@param outputTimeZoneOffset the time zone to align times for interval < day, needed when time zone in date/time varies in format +-NN:NN as per
OffsetDateTime time zone.
@param outputTimeZone the time zone to assign for interval < day, user-preferred text representation
@param readStart starting date/time to read
@param readEnd ending date/time to read
@param readData if true, read all the data values; if false, only initialize the time series header information
*/
public List<TS> readTimeSeriesList ( boolean readUsingApi, TimeInterval interval, boolean requireDataToMatchInterval,
		String outputTimeZoneOffset, String outputTimeZone,
	DateTime readStart, DateTime readEnd, boolean readData )
throws MalformedURLException, IOException, Exception
{   String routine = getClass().getSimpleName() + ".readTimeSeriesList";
    // Create the time series from the WaterML...
	List<TS> tsList = new ArrayList<TS>();
	if ( readUsingApi ) {
		Message.printStatus(2, routine, "Reading WaterML 2 time series using API.");
		//this.failureMessages.add("API implementation is incomplete.  Use ParseDOM instead.");
		tsList = readTimeSeriesListUsingApi ( WaterMLVersion.STANDARD_2_0, interval, readStart, readEnd,
			readData, requireDataToMatchInterval, outputTimeZoneOffset, outputTimeZone, this.url, this.watermlFile );
	}
	else {
		// Read by parsing the DOM
		Message.printStatus(2, routine, "Reading WaterML 2 time series by parsing the DOM.");
	    // The following can allow conflicts in class path parsers to occur...
	    //javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
	    // Specify the class that should be used for the DocumentBuilderFactory implementation (use internal Java factory).
	    DocumentBuilderFactory dbf =
	        DocumentBuilderFactory.newInstance("com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl",
	            null); // Use current thread class loader
	    // Be aware of namespace to transparently handle ns1: in front of element names, but do not
	    // do full validation with setValidating(true)
	    dbf.setNamespaceAware(true);
	    Document dom = dbf.newDocumentBuilder().parse(new ReaderInputStream(new StringReader(this.watermlString)));
	    WaterMLVersion watermlVersion = determineWaterMLVersion(dom);
	    String observationMemberNS = "";
	    String observationMemberTag = "";
	    if ( watermlVersion == WaterMLVersion.STANDARD_2_0 ) { // Might change if 2.1, etc.
	    	//timeSeriesNS = "wml2";
	    	observationMemberNS = WATERML_2_0_NS_URI;
	        observationMemberTag = "observationMember";
	    }
	    else {
	        throw new IOException ( "WaterML version " + watermlVersion + " is not supported (must be version 2).");
	    }
	    NodeList observationMemberElementList = dom.getDocumentElement().getElementsByTagNameNS(observationMemberNS,observationMemberTag);
	    Message.printStatus(2, routine, "Have " + observationMemberElementList.getLength() +
	    	" WaterML time series elements to process based on \"" + observationMemberTag + "\" element." );
	    for (int i = 0; i < observationMemberElementList.getLength(); i++) {
	        // Uncomment for troubleshooting...
	        if ( Message.isDebugOn ) {
	            Node node = observationMemberElementList.item(i);
	            Message.printDebug(1, routine, "NodeLocalName=" + node.getLocalName() +
	                ", namespace=" + node.getNamespaceURI() + ", name=" + node.getNodeName());
	        }
	        tsList.add(readTimeSeriesUsingDOM(watermlVersion, dom.getDocumentElement(), (Element)observationMemberElementList.item(i),
	            interval, requireDataToMatchInterval, outputTimeZoneOffset, outputTimeZone, this.url, this.watermlFile, readStart, readEnd, readData ));
	    }
	}
    return tsList;
}

// USGS NWIS daily example snippets are from:
// https://waterservices.usgs.gov/nwis/dv/?format=waterml,2.0&indent=on&sites=09071750,09070500&startDT=2000-01-01&endDT=2000-03-15&siteStatus=all
/**
Read the time series list from the string WaterML.
@param interval indicates the interval for data in the file, needed as a hint because there is nothing
in the file that indicates that data are daily values, etc.
@param readStart starting date/time to read
@param readEnd ending date/time to read
@param readData if true, read all the data values; if false, only initialize the time series header information
@param requireDataToMatchInterval if true, require that all date/times for data fall on the exact interval; if false,
allow the date/time to truncate
*/
public List<TS> readTimeSeriesListUsingApi ( WaterMLVersion watermlVersion, TimeInterval interval, DateTime readStart, DateTime readEnd,
    boolean readData, boolean requireDataToMatchInterval, String outputTimeZoneOffset, String outputTimeZone, String url, File file )
throws MalformedURLException, IOException, Exception
{   String routine = getClass().getSimpleName() + ".readTimeSeriesList";
	List<TS> tsList = new ArrayList<TS>();
	if ( watermlVersion != WaterMLVersion.STANDARD_2_0 ) {
		this.failureMessages.add("Request is to parse WaterML version " + watermlVersion + " but only " + WaterMLVersion.STANDARD_2_0 + " is supported.");
		return tsList;
	}
	try {
		JAXBContext context = JAXBContext.newInstance("net.opengis.waterml._2");
		StringReader stringReader = new StringReader(this.watermlString);
		Unmarshaller unmarshaller = context.createUnmarshaller();
		// Don't know what the root element will be because, for example, USGS uses FeatureCollection but straight WaterML2 uses Collection
		@SuppressWarnings("rawtypes")
		JAXBElement rootElement = (JAXBElement) unmarshaller.unmarshal(stringReader);
		Message.printStatus(2,routine,"Root element:" + rootElement);
		Message.printStatus(2,routine,"Name:" + rootElement.getName() );
		Message.printStatus(2,routine,"Declared Type:" + rootElement.getDeclaredType() );
		FeatureCollectionType gmlFeatureCollection = null; // Entry point into data hierarchy
		if ( rootElement.getDeclaredType().toString().endsWith(".CollectionType") ) {
			// This agrees with the WaterML 2.0 specification, for example:
			// <?xml version="1.0" encoding="UTF-8"?>
			// <wml2:Collection xmlns:wml2="http://www.opengis.net/waterml/2.0" xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:om="http://www.opengis.net/om/2.0" xmlns:sa="http://www.opengis.net/sampling/2.0" xmlns:sams="http://www.opengis.net/samplingSpatial/2.0" xmlns:swe="http://www.opengis.net/swe/2.0" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.opengis.net/waterml/2.0 http://www.opengis.net/waterml/2.0/waterml2.xsd" gml:id="USBR RWIS">
			Message.printStatus(2,routine,"Read Collection root element from WaterML");
			CollectionType wmlCollection = (CollectionType)rootElement.getValue();
			// Create FeatureCollection to wrap the CollectionType
			net.opengis.waterml._2.ObjectFactory watermlFactory = new net.opengis.waterml._2.ObjectFactory();
			net.opengis.gml._3.ObjectFactory gmlFactory = new net.opengis.gml._3.ObjectFactory();
			// featureCollection = new FeatureCollectionType();
			gmlFeatureCollection = gmlFactory.createFeatureCollectionType();
			List<FeaturePropertyType> featurePropertyTypeList = gmlFeatureCollection.getFeatureMember();
			FeaturePropertyType featureProperty = gmlFactory.createFeaturePropertyType();
			// CollectionType extends AbstractFeatureType
			JAXBElement<CollectionType> jaxbElement = watermlFactory.createCollection(wmlCollection);
			featureProperty.setAbstractFeature(jaxbElement);
			featurePropertyTypeList.add(featureProperty);
		}
		else if ( rootElement.getDeclaredType().toString().endsWith(".FeatureCollectionType") ) {
			// USGS NWIS wraps an additional FeatureCollection list around multiple Collection objects:
			// <gml:FeatureCollection gml:id="USGS.waterservices" xsi:schemaLocation="http://www.opengis.net/waterml/2.0 http://schemas.opengis.net/waterml/2.0/waterml2.xsd" xmlns:gml="http://www.opengis.net/gml/3.2" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:wml2="http://www.opengis.net/waterml/2.0" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:om="http://www.opengis.net/om/2.0" xmlns:sa="http://www.opengis.net/sampling/2.0" xmlns:sams="http://www.opengis.net/samplingSpatial/2.0" xmlns:swe="http://www.opengis.net/swe/2.0">
			//   <gml:featureMember>
			//     <wml2:Collection gml:id="C.USGS.09070500">
			Message.printStatus(2,routine,"Read FeatureCollection root element from WaterML");
			gmlFeatureCollection = (FeatureCollectionType)rootElement.getValue();
		}
		// At this point can access all of the data in the hierarchy in the same way
		// - loop through and demonstrate
		Message.printStatus(2,routine,"Looping through FeatureCollection");
		// FeatureCollectionType.getFeatureMember() actually returns a list
		for ( FeaturePropertyType gmlFeatureMember : gmlFeatureCollection.getFeatureMember() ) {
			// Within USGS FeatureCollection is list of FeatureMember, which themselves
			// contain a list of Collection
			Message.printStatus(2,routine,"Have FeatureMember " + gmlFeatureMember );
			// Each FeatureMember has a Collection
			// Corresponds to "featureMember" in USGS example
			// Dig down one more level to get the Collection (time series plus other data)
			if ( gmlFeatureMember.getAbstractFeature() != null ) {
				// Have some data as a FeatureMember
				AbstractFeatureType jaxbAbstractFeature = gmlFeatureMember.getAbstractFeature().getValue();
				if ( jaxbAbstractFeature instanceof CollectionType ) {
					// USGS NWIS daily example:
					// <wml2:Collection gml:id="C.USGS.09070500">
					Message.printStatus(2,routine,"Have CollectionType " + jaxbAbstractFeature );
					CollectionType wmlCollectionType = (CollectionType)jaxbAbstractFeature;
					// identifier, USGS NWIS daily example
					// <gml:identifier codeSpace="http://waterservices.usgs.gov/nwis/dv">USGS.09070500</gml:identifier>
					if ( wmlCollectionType.getIdentifier() == null ) {
						Message.printStatus(2,routine,"Collection identifier: null" );
					}
					else {
						Message.printStatus(2,routine,"Collection identifier: " + wmlCollectionType.getIdentifier().getValue() );
					}
					// description
					// Not used in NWIS?
					if ( wmlCollectionType.getDescription() == null ) {
						Message.printStatus(2,routine,"Collection description: null");
					}
					else {
						Message.printStatus(2,routine,"Collection description: " + wmlCollectionType.getDescription().getValue() );
					}
					// name (multiple properties), USGS NWIS daily example:
					// <gml:name codeSpace="http://waterservices.usgs.gov/nwis/dv">Timeseries collected at COLORADO RIVER NEAR DOTSERO, CO</gml:name>
					for ( int i = 0; i < wmlCollectionType.getName().size(); i++ ) {
						Message.printStatus(2,routine,"Collection name: " + wmlCollectionType.getName().get(i).getValue() );
					}
					// DocumentMetadata, USGS NWIS daily example:
				    //  <wml2:metadata>
				    //    <wml2:DocumentMetadata gml:id="doc.USGS.MP.USGS.09070500">
				    //      <gml:metaDataProperty about="contact" xlink:href="http://waterservices.usgs.gov"/>
				    //      <wml2:generationDate>2017-06-29T04:34:37.787-04:00</wml2:generationDate>
				    //      <wml2:version xlink:href="http://www.opengis.net/waterml/2.0" xlink:title="WaterML 2.0"/>
				    //    </wml2:DocumentMetadata>
				    //  </wml2:metadata>
					DocumentMetadataPropertyType wmldocumentMetadataProperty = wmlCollectionType.getMetadata();
					DocumentMetadataType wmlDocumentMetadata = wmldocumentMetadataProperty.getDocumentMetadata();
					List<MetaDataPropertyType> gmlMetadataPropertyList = wmlDocumentMetadata.getMetaDataProperty();
					for ( MetaDataPropertyType gmlMetadataProperty : gmlMetadataPropertyList ) {
						Message.printStatus(2, routine, "DocumentMetadatProperty object = " + gmlMetadataProperty );
						// Can get individual properties, list of anything
						@SuppressWarnings("rawtypes")
						JAXBElement jaxbMetadataProperty = gmlMetadataProperty.getAbstractMetaData();
						if ( jaxbMetadataProperty != null ) {
							Object o = jaxbMetadataProperty.getValue();
							Message.printStatus(2, routine, "DocumentMetadata object = " + o + " " + o.getClass().getName() );
							//if ( jaxbMetadataProperty != null ) {
							//	Object o = jaxbPhenomenonTimeElement.getValue();
							//}
						}
					}
					XMLGregorianCalendar xmlGenerationDate = wmlDocumentMetadata.getGenerationDate();
					Message.printStatus(2, routine, "Document generation date = " + xmlGenerationDate );
					String wmlGenerationSystem = wmlDocumentMetadata.getGenerationSystem();
					Message.printStatus(2, routine, "Document generation system = " + wmlGenerationSystem );
					ReferenceType wmlVersion = wmlDocumentMetadata.getVersion();
					String wmlVersionString = wmlVersion.getAtitle();
					Message.printStatus(2, routine, "Document version = " + wmlVersionString );
					// observationMember (essentially list of time series), USGS NWIS daily example (ID includes siteId, parameter, ?, statistic:
				    //  <wml2:observationMember>
				    //    <om:OM_Observation gml:id="obs.USGS.09070500.00060.18624.00003">
					List<OMObservationPropertyType> wmlOMObservationPropertyList = wmlCollectionType.getObservationMember();
					Message.printStatus(2, routine, "Have " + wmlOMObservationPropertyList.size() + " time series in collection");
					for ( OMObservationPropertyType wmlOMObservationProperty : wmlOMObservationPropertyList ) { // Observation member
						// These are essentially time series
						OMObservationType omOMObservation = wmlOMObservationProperty.getOMObservation();
						// Properties that will be set for use in defining the time series
						Double latitude = null;
						Double longitude = null;
						Double x = null;
						Double y = null;
						String siteName = null;
						String statistic = null;
						// Phenomenon time (such as period of reading data from sensor), USGS NWIS daily example:
				        //  <om:phenomenonTime>
				        //    <gml:TimePeriod gml:id="sample_time.USGS.09070500.00060.18624.00003">
				        //      <gml:beginPosition>2000-01-01</gml:beginPosition>
				        //      <gml:endPosition>2000-03-15</gml:endPosition>
				        //    </gml:TimePeriod>
				        //  </om:phenomenonTime>
						TimeObjectPropertyType omPhenomenonTime = omOMObservation.getPhenomenonTime();
						if ( omPhenomenonTime != null ) {
							@SuppressWarnings("rawtypes")
							JAXBElement jaxbPhenomenonTimeElement = omPhenomenonTime.getAbstractTimeObject();
							if ( jaxbPhenomenonTimeElement != null ) {
								Object o = jaxbPhenomenonTimeElement.getValue();
								if ( o instanceof net.opengis.gml._3.TimePeriodType ) {
									Message.printStatus(2, routine, "phenomenonTimeElement=" + jaxbPhenomenonTimeElement + " " +
										jaxbPhenomenonTimeElement.getClass().getName() + " getValue=" + jaxbPhenomenonTimeElement.getValue());
									DateTimeRange phenomenonTimePeriod = parseGMLTimePeriodType ( (net.opengis.gml._3.TimePeriodType)o );
									Message.printStatus(2, routine, "Phenomenon period is " + phenomenonTimePeriod.getStart() + " to " + phenomenonTimePeriod.getEnd() );
								}
								else {
									throw new IOException("No TimePeriod from phenomenonTime - can't determine time series period");
								}
							}
						}
						// resultTime, USGS NWIS daily example:
				        //  <om:resultTime>
				        //    <gml:TimeInstant gml:id="requested_time.USGS.09070500.00060.18624.00003">
				        //      <gml:timePosition>2017-06-29T04:34:37.787-04:00</gml:timePosition>
				        //    </gml:TimeInstant>
				        //  </om:resultTime>
						TimeInstantPropertyType omResultTime = omOMObservation.getResultTime();
						DateTime resultTime = parseOmTimeInstant ( omResultTime );
						Message.printStatus(2, routine, "Result time is " + resultTime );
						// The procedure/process used to collect/generate the data, USGS NWIS daily example:
				        //  <om:procedure>
				        //    <wml2:ObservationProcess gml:id="process.USGS.09070500.00060.18624.00003">
				        //      <wml2:processType xlink:href="http://www.opengis.net/def/waterml/2.0/processType/Sensor" xlink:title="Sensor"/>
				        //      <wml2:parameter xlink:title="Statistic" xlink:href="http://waterdata.usgs.gov/nwisweb/rdf?statCd=00003">
				        //        <om:NamedValue>
				        //          <om:name xlink:title="Mean"/>
				        //          <om:value xsi:type="xs:string" xmlns:xs="http://www.w3.org/2001/XMLSchema">00003</om:value>
				        //        </om:NamedValue>
				        //      </wml2:parameter>
				        //    </wml2:ObservationProcess>
				        //  </om:procedure>
						OMProcessPropertyType omProcedure = omOMObservation.getProcedure();
						//ActuateType omActuateType = omProcedure.getActuate();
						//if ( omActuateType != null ) {
						//	Message.printStatus(2, routine, "Actuate is " + omActuateType + " " + omActuateType.getClass().getName() );
						//}
						//TypeType wmlProcessType =  omProcedure.getType();
						//Message.printStatus(2, routine, "Process type is " + wmlProcessType + " " + wmlProcessType.getClass().getName() );
						Object o = omProcedure.getAny();
						if ( o != null ) {
							//Message.printStatus(2, routine, "Any is " + o + " " + o.getClass().getName() );
							@SuppressWarnings("rawtypes")
							JAXBElement anyElement = (JAXBElement)o;
							Object oAny = anyElement.getValue();
							Message.printStatus(2, routine, "Any is " + oAny + " " + oAny.getClass().getName() );
							ObservationProcessType wmlObservationProcess = (ObservationProcessType)oAny;
							ReferenceType wmlProcessType = wmlObservationProcess.getProcessType();
							Message.printStatus(2, routine, "Process type is " + wmlProcessType + " " + wmlProcessType.getClass().getName() );
							List<NamedValuePropertyType> omParameterNameList = wmlObservationProcess.getParameter();
							for ( NamedValuePropertyType namedValue: omParameterNameList ) {
								String parameterTitle = namedValue.getAtitle();
								Message.printStatus(2, routine, "Parameter titel is " + parameterTitle );
								NamedValueType omNamedValue = namedValue.getNamedValue();
								Message.printStatus(2, routine, "Named value is " + omNamedValue + " " + omNamedValue.getClass().getName() );
								// Following returns "Mean", suitable for statistic name
								ReferenceType omNamedValueName = omNamedValue.getName();
								if ( parameterTitle.equalsIgnoreCase("statistic") ) {
									statistic = omNamedValueName.getAtitle();
								}
								Message.printStatus(2, routine, "Named value name is " + omNamedValueName.getAtitle());
								// Following returns "0003", the raw statistic code
								Object omNamedValueValue = omNamedValue.getValue();
								Message.printStatus(2, routine, "Named value value is " + omNamedValueValue );
							}
						}
						// The parameter/property/data type for the time series, USGS NWIS daily example:
				        //  <om:observedProperty xlink:title="Discharge" xlink:href="http://waterdata.usgs.gov/nwisweb/rdf?parmCd=00060"/>
						ReferenceType observedProperty = omOMObservation.getObservedProperty();
						String observedPropertyTitle = observedProperty.getAtitle();
						Message.printStatus(2, routine, "Observed property title is " + observedPropertyTitle );
						String observedPropertyHref = observedProperty.getHref();
						Message.printStatus(2, routine, "Observed property href is " + observedPropertyHref );
						// The station, USGS NWIS daily example:
				        //  <om:featureOfInterest xlink:title="COLORADO RIVER NEAR DOTSERO, CO">
				        //    <wml2:MonitoringPoint gml:id="USGS.MP.USGS.09070500.00060.18624.00003">
				        //      <gml:descriptionReference xlink:href="http://waterservices.usgs.gov/nwis/site/?sites=09070500&amp;agencyCd=USGS&amp;format=rdb" xlink:title="COLORADO RIVER NEAR DOTSERO, CO"/>
				        //      <sa:sampledFeature xlink:title="COLORADO RIVER NEAR DOTSERO, CO"/>
				        //      <sams:shape>
				        //        <gml:Point gml:id="USGS.P.USGS.09070500.00060.18624.00003">
				        //          <gml:pos srsName="urn:ogc:def:crs:EPSG:4326">39.6446111 -107.0780139</gml:pos>
				        //        </gml:Point>
				        //      </sams:shape>
				        //    </wml2:MonitoringPoint>
				        //  </om:featureOfInterest>
						FeaturePropertyType gmlFeatureOfInterest = omOMObservation.getFeatureOfInterest();
						if ( gmlFeatureOfInterest != null ) {
							@SuppressWarnings("rawtypes")
							JAXBElement jaxbFeatureOfInterest = gmlFeatureOfInterest.getAbstractFeature();
							Object o2 = jaxbFeatureOfInterest.getValue();
							Message.printStatus(2, routine, "Feature of interest is " + o2 + " " + o2.getClass().getName() );
							// Site name from featureOfInterest xlink:title
							siteName = gmlFeatureOfInterest.getAtitle();
							if ( o2 instanceof MonitoringPointType ) {
								MonitoringPointType wmlMonitoringPoint = (MonitoringPointType)o2;
								if ( wmlMonitoringPoint != null ) {
									// gml:id has the station identifier
									// - USGS NWIS daily example:  USGS.MP.USGS.09070500.00060.18624.00003
									// - USGS.MP + agency(USGS) + stationId(09070500) + parameterCode(00060) + unitsCode(18624) + statistic(00003)
									String gmlId = wmlMonitoringPoint.getId();
									Message.printStatus(2, routine, "Monitoring point ID is " + gmlId );
									// Description
									StringOrRefType monitoringPointDesc = wmlMonitoringPoint.getDescription();
									if ( monitoringPointDesc != null ) {
										String monitoringPointTitle = monitoringPointDesc.getAtitle();
										Message.printStatus(2, routine, "Monitoring point description title is " + monitoringPointTitle );
										String monitoringPointDescription = monitoringPointDesc.getValue();
										Message.printStatus(2, routine, "Monitoring point description is " + monitoringPointDescription );
									}
									ReferenceType monitoringPointDesc2 = wmlMonitoringPoint.getDescriptionReference();
									if ( monitoringPointDesc2 != null ) {
										String monitoringPointTitle = monitoringPointDesc2.getAtitle();
										Message.printStatus(2, routine, "Monitoring point description title is " + monitoringPointTitle );
									}
									List<FeaturePropertyType> gmlSampledFeaturePropertyList = wmlMonitoringPoint.getSampledFeature();
									if ( gmlSampledFeaturePropertyList != null ) {
										Message.printStatus(2, routine, "Sampled feature property list size is " + gmlSampledFeaturePropertyList.size() );
										for ( FeaturePropertyType gmlFeatureProperty : gmlSampledFeaturePropertyList ) {
											@SuppressWarnings("rawtypes")
											JAXBElement jaxbFeatureProperty = gmlFeatureProperty.getAbstractFeature();
											if ( jaxbFeatureProperty != null ) {
												Object o3 = jaxbFeatureProperty.getValue();
												if ( o3 != null ) {
													Message.printStatus(2, routine, "Feature property is " + o3 + " " + o3.getClass().getName() );
												}
											}
											else {
												Message.printStatus(2, routine, "Sampled feature property is null");
											}
											String sampledFeatureTitle = gmlFeatureProperty.getAtitle();
											Message.printStatus(2, routine, "Sampled feature title is \"" + sampledFeatureTitle + "\"" );
										}
									}
									else {
										Message.printStatus(2, routine, "Sampled feature property list is null");
									}
									List<SamplingFeatureComplexPropertyType> samSampleFeatureList = wmlMonitoringPoint.getRelatedSamplingFeature();
									if ( samSampleFeatureList != null ) {
										Message.printStatus(2, routine, "Related sampling feature list size is " + samSampleFeatureList.size() );
										for ( SamplingFeatureComplexPropertyType samSampleFeature : samSampleFeatureList ) {
											SamplingFeatureComplexType samSampleFeatureProperty = samSampleFeature.getSamplingFeatureComplex();
											Message.printStatus(2, routine, "Related sampling feature property is " + samSampleFeatureProperty + " " + samSampleFeatureProperty.getClass().getName() );
											samSampleFeatureProperty.getRelatedSamplingFeature();
										}
									}
									else {
										Message.printStatus(2, routine, "Related sampling feature list is null");
									}
									// The spatial data about the monitoring point
									ShapeType samShape = wmlMonitoringPoint.getShape();
									if ( samShape != null ) {
										@SuppressWarnings("rawtypes")
										JAXBElement jaxbShape = samShape.getAbstractGeometry();
										Object o3 = jaxbShape.getValue();
										Message.printStatus(2, routine, "Shape is " + o3 + " " + o3.getClass().getName() );
										if ( o3 instanceof PointType ) {
											PointType gmlPoint = (PointType)o3;
											CoordinatesType gmlCoords = gmlPoint.getCoordinates();
											if ( gmlCoords != null ) {
												String coordSys = gmlCoords.getCs();
												String shapeText = gmlCoords.getValue();
												Message.printStatus(2, routine, "Shape coordinate system is " + coordSys );
												Message.printStatus(2, routine, "Shape text is " + shapeText );
											}
											DirectPositionType gmlPos = gmlPoint.getPos();
											if ( gmlPos != null ) {
												Message.printStatus(2, routine, "pos is " + o3 + " " + o3.getClass().getName() );
												String srsName = gmlPos.getSrsName();
												List<Double> posValues = gmlPos.getValue();
												Message.printStatus(2, routine, "Shape coordinate system is " + srsName );
												for ( Double d : posValues ) {
													Message.printStatus(2, routine, "Point value " + d );
												}
												// If coordinate system is EPSG 4326, can treat as geographic for station properties
												// - otherwise treat as x, y
												if ( (srsName.indexOf("EPSG") >= 0) && (srsName.indexOf("4326") >= 0) ) {
													latitude = posValues.get(0);
													longitude = posValues.get(1);
												}
												else {
													y = posValues.get(0);
													x = posValues.get(1);
												}
											}
										}
									}
								}
							}
						}
						// The object containing the data points, USGS NWIS daily example:
				        //  <om:result>
				        //    <wml2:MeasurementTimeseries gml:id="TS.USGS.09070500.00060.18624.00003">
						String wmlMeasurementTimeSeriesId = omOMObservation.getId();
						Message.printStatus(2, routine, "MeasurementTimeSeries ID=" + wmlMeasurementTimeSeriesId);
						String [] idParts = wmlMeasurementTimeSeriesId.split(Pattern.quote("."));
						String source = idParts[1];
						String siteId = idParts[2];
						String dataType = idParts[3];
						String statisticCode = idParts[5]; // For USGS similar to 00003
						// Not sure if this is used
						List<NamedValuePropertyType> omParameterList = omOMObservation.getParameter();
						if ( omParameterList != null ) {
							Message.printStatus(2, routine, "omParameterList size is " + omParameterList.size());
							for ( NamedValuePropertyType omParameter : omParameterList ) {
								Message.printStatus(2, routine, "omParameter is " + omParameter + " " + omParameter.getClass().getName() );
							}
						}
						else {
							Message.printStatus(2, routine, "omPropertyList is null" );
						}
						List<MetaDataPropertyType> gmlMetaDataPropertyList = omOMObservation.getMetaDataProperty();
						if ( gmlMetaDataPropertyList != null ) {
							Message.printStatus(2, routine, "gmlMetaDataPropertyList size is " + gmlMetaDataPropertyList.size());
							for ( MetaDataPropertyType gmlMetaDataProperty : gmlMetaDataPropertyList ) {
								Message.printStatus(2, routine, "gmlMetaDataProperty is " + gmlMetaDataProperty + " " + gmlMetaDataProperty.getClass().getName() );
							}
						}
						else {
							Message.printStatus(2, routine, "gmlMetaDataPropertyList is null" );
						}
						List<ObservationContextPropertyType> omRelatedObservationList = omOMObservation.getRelatedObservation();
						if ( omRelatedObservationList != null ) {
							Message.printStatus(2, routine, "omRelatedObservationList size is " + omRelatedObservationList.size());
							for ( ObservationContextPropertyType omRelatedObservation : omRelatedObservationList ) {
								Message.printStatus(2, routine, "omRelatedObservation is " + omRelatedObservation + " " + omRelatedObservation.getClass().getName() );
							}
						}
						else {
							Message.printStatus(2, routine, "omRelatedObservationList is null" );
						}
						// Create the time series.
					    TS ts;
					    TSIdent ident = new TSIdent(siteId+"." + source + "."+ dataType + "-" + statisticCode + "." + interval);
					    try {
					        // Create the time series and set basic time series properties
					        ts = TSUtil.newTimeSeries(ident.toString(), true);
					        ts.setIdentifier(ident);
					    }
					    catch (Exception ex) {
					        throw new IOException("Error setting time series identifier ", ex);
					    }
					    
					    // Set the main time series properties
					    
					    try {
					        // Missing data value
					        ts.setMissing ( Double.NaN );
					        // Data units
					        //ts.setDataUnits(readTimeSeries_ParseUnits(watermlVersion,variableElement));
					        ts.setDataUnitsOriginal(ts.getDataUnits());
					        // Description depends on what data are available
					        if ( siteName != null ) {
							    String statistic2 = statistic;
							    if ( statistic2 == null ) {
							    	statistic2 = statisticCode;
							    }
					        	if ( (observedPropertyTitle != null) && !observedPropertyTitle.isEmpty() ) {
					        		siteName = siteName + ", " + observedPropertyTitle;
					        	}
					        	if ( (statistic2 != null) && !statistic2.isEmpty() ) {
					        		siteName = siteName + ", " + statistic2;
					        	}
					            ts.setDescription(siteName);
					        }
					        // Also set properties by passing through XML elements
					        boolean setPropertiesFromMetadata = true;
					        if ( setPropertiesFromMetadata ) {
					            // Set time series properties from the timeSeries elements
					            // From sourceInfo
					            //setTimeSeriesPropertyToElementValue(ts,timeSeriesElement,"siteName");
					            //setTimeSeriesPropertyToElementValue(ts,timeSeriesElement,"siteCode");
					            //setTimeSeriesPropertyToElementAttributeValue(ts,timeSeriesElement,"siteCode","network","network");
					            //setTimeSeriesPropertyToElementAttributeValue(ts,timeSeriesElement,"siteCode","agencyCode","agencyCode");
					            //setTimeSeriesPropertyToElementAttributeValue(
					            //    ts,timeSeriesElement,"timeZoneInfo","siteUsesDaylightSavingsTime","siteUsesDaylightSavingsTime");
					            //setTimeSeriesPropertyToElementAttributeValue(
					            //    ts,timeSeriesElement,"defaultTimeZone","zoneAbbreviation","defaultTimeZone");
					            //setTimeSeriesPropertyToElementAttributeValue(
					            //    ts,timeSeriesElement,"daylightSavingsTimeZone","zoneAbbreviation","dayligthSavingsTimeZone");
					            // From geoLocation
					            ts.setProperty("latitude",latitude);
					            ts.setProperty("longitude",longitude);
					            // Alternative coordinates if latitude and longitude are not know
					            if ( x != null ) {
					            	ts.setProperty("x", x);
					            }
					            if ( x != null ) {
					            	ts.setProperty("y", y);
					            }
					            // Other site properties (USGS only?)
					            //setTimeSeriesPropertyFromGenericPropertyElement(ts,timeSeriesElement,"siteProperty","name","siteTypeCd");
					            //setTimeSeriesPropertyFromGenericPropertyElement(ts,timeSeriesElement,"siteProperty","name","hucCd");
					            //setTimeSeriesPropertyFromGenericPropertyElement(ts,timeSeriesElement,"siteProperty","name","stateCd");
					            //setTimeSeriesPropertyFromGenericPropertyElement(ts,timeSeriesElement,"siteProperty","name","countyCd");
					        }
					        // History
					        if ( (url != null) && !url.equals("") ) {
					            // Set creation information from URL
					            ts.addToGenesis("Create time series from WaterML queried with URL:  " + url );
					        }
					        else {
					            if ( file != null ) {
					                // Set creation information from file
					                ts.addToGenesis("Create time series from contents of file:  " + file.getAbsolutePath() );
					            }
					            // Also extract creation information from the WaterML (probably a file).
					            ts.addToGenesis("Read time series by using WaterML 2 API." );
					        }
					    }
					    catch (Exception ex) {
					        throw new IOException("Error setting time series properties ", ex);
					    }
					    
					    // Add the time series to the list
					    tsList.add(ts);
						
						// Try to process "result", but following code gets into generic XML objects rather than API classes.
						// Why is the result unmarshalling (deserializing) to DOM elements rather than JAXB objects?
						// -see:  https://www.greenbird.com/2016/05/10/jaxb-unmarshalling-and-avoiding-the-dom/
						// -see:  https://stackoverflow.com/questions/5122296/jaxb-not-unmarshalling-xml-any-element-to-jaxbelement
						// -see:  https://stackoverflow.com/questions/26439184/why-is-the-objectfactory-not-used-during-unmarshalling
						Object resultObject = omOMObservation.getResult();
						Message.printStatus(2, routine, "resultObject=" + resultObject + " " + resultObject.getClass().getName());
						Object userData = null;
						if ( resultObject instanceof ElementNSImpl ) {
							// Parsing with built-in internal Xerces
							ElementNSImpl resultElement = (ElementNSImpl)resultObject;
							// User data does not seem like what is used
							/*
							userData = resultElement.getUserData();
							if ( userData != null ) {
								Message.printStatus(2, routine, "userData=" + userData + " " + userData.getClass().getName());
							}
							else {
								Message.printStatus(2, routine, "userData is null");
							}
							*/
							// Child nodes appears to be what is used, lacking effort to change the bindings to get JAXB objects
							NodeList nodeList = resultElement.getChildNodes();
							if ( nodeList != null ) {
								Message.printStatus(2, routine, "Have " + nodeList.getLength() + " child nodes");
								for ( int i = 0; i < nodeList.getLength(); i++ ) {
									Node node = nodeList.item(i);
									Message.printStatus(2, routine, "Node name =" + node.getNodeName() );
									//NodeList pointList = resultElement.getElementsByTagName("wml2:point");
									//if ( pointList != null ) {
									//	Message.printStatus(2, routine, "Have " + pointList.getLength() + " points");
									//}
									if ( node.getNodeName().toUpperCase().indexOf("MEASUREMENTTIMESERIES") >= 0 ) {
										// Call the DOM parsing method since dealing with the same objects
										Element measurementTimeSeriesElement = (Element)node;
										String noDataValue = null;
										readTimeSeriesUsingDOM_ParseValues(watermlVersion, ts, measurementTimeSeriesElement,
										    noDataValue, readStart, readEnd, outputTimeZoneOffset, outputTimeZone,
										    readData, requireDataToMatchInterval );
									}
								}
							}
							else {
								Message.printStatus(2, routine, "Result node list is null");
							}
						}
						else {
							Message.printStatus(2, routine, "Don't know how to handle resultObject type " + resultObject.getClass().getName());
						}
						//if ( resultObject instanceof MeasurementTimeseriesType ) {
						if ( (userData != null) && (userData instanceof MeasurementTimeseriesType) ) {
							Message.printStatus(2, routine, "Results are in MeasurementTimeseriesType");
							MeasurementTimeseriesType measurementTimeSeries = (MeasurementTimeseriesType)resultObject;
							measurementTimeSeries.getDefaultPointMetadata();
							List<Point> pointList = measurementTimeSeries.getPoint();
							Message.printStatus(2, routine, "Have " + pointList.size() + " points");
							for ( Point point : pointList ) {
								MeasureTVPType measureTVP = point.getMeasurementTVP();
								TimePositionType dateTime = measureTVP.getTime();
								JAXBElement<MeasureType> measurementType = measureTVP.getValue();
								Message.printStatus(2, routine, "time="+dateTime);
								Message.printStatus(2, routine, "value="+measurementType.getValue());
								TVPMeasurementMetadataPropertyType measureMetadata = measureTVP.getMetadata();
								// Can get qualifiers (flags)
								TVPMeasurementMetadataType measureMeta = measureMetadata.getTVPMeasurementMetadata();
								List<QualityPropertyType> qualityPropertyList = measureMeta.getQualifier();
							}
						}
						else if ( resultObject instanceof CategoricalTimeseriesType ) {
							// Not handled yet
							Message.printStatus(2, routine, "Results are in CategoricalTimeseriesType - not handled");
						}
						else {
							// Not handled
							Message.printStatus(2, routine, "Results are in unknown TimeseriesType");
						}
					}
				}
				else {
					Message.printStatus(2,routine,"Was expecting CollectionType but have " + jaxbAbstractFeature );
				}			
			}
			else {
				Message.printStatus(2,routine,"FeatureMember has no data");
			}
		}
	}
	catch ( JAXBException e ) {
		System.out.println(e);
	}
    return tsList;
}

/**
Read a single time series from the DOM given an element for the "observationMember" tag.
@param watermlVersion WaterML version being read (element names are different)
@param domElement the top level DOM element
@param observationMemberElement the "observationMember" element that is being processed,
containing one time series and related data
@param interval indicates the interval for data in the file, needed as a hint because there is nothing
in the file that indicates that data are daily values, etc.
@param requireDataToMatchInterval if true, the date/times with data values must align with the interval (if they
don't warnings will be generated)
@param url the original URL used to read the WaterML (can be null or "" if read from a file), used to
populate the history comments in the time series
@param file the original File corresponding to the WaterML file (can be null or "" if read from a file), used to
populate the history comments in the time series
@param readStart starting date/time to read
@param readEnd ending date/time to read
@param readData whether to read data values (if false initialize the period but do not allocate
memory or process the data values)
*/
private TS readTimeSeriesUsingDOM ( WaterMLVersion watermlVersion, Element domElement,
    Element observationMemberElement,
    TimeInterval interval, boolean requireDataToMatchInterval, String outputTimeZoneOffset, String outputTimeZone, String url, File file,
    DateTime readStart, DateTime readEnd, boolean readData )
throws IOException
{   String routine = getClass().getSimpleName() + ".readTimeSeriesUsingDOM";
	TS ts = null;
    String monitoringPointTag = null;
    String observedPropertyTag = null;
    String collectionTag = null;
    String phenomenonTimeTag = null;
    String featureOfInterestTag = null;
    String measurementTimeSeriesTag = null;
    String observationProcessTag = null;
    if ( watermlVersion == WaterMLVersion.STANDARD_2_0 ) {
        monitoringPointTag = "MonitoringPoint"; // Information about monitoring point (time series hook to station)
        observationProcessTag = "ObservationProcess"; // Use to get statistic name
        observedPropertyTag = "observedProperty"; // Information about data type
        collectionTag = "Collection"; // Collection (top WaterML element)
        phenomenonTimeTag = "phenomenonTime"; // Equates to data period
        featureOfInterestTag = "featureOfInterest"; // Equates to station or location
        measurementTimeSeriesTag = "MeasurementTimeseries"; // Equates to time series
    }
    Element monitoringPointElement = this.xmlToolkit.getSingleElement(observationMemberElement, monitoringPointTag);
    String monitoringPointId = "";
    String phenomenonBeginPosition = "";
    String phenomenonEndPosition = "";
	Double latitude = null;
	Double longitude = null;
    if ( monitoringPointElement != null ) {
    	// id attribute is what USGS uses for site ID and other data so pass on as time series property
    	monitoringPointId = this.xmlToolkit.getNodeAttribute("gml:id",monitoringPointElement);
    	// Latitude and longitude for USGS
        //<wml2:MonitoringPoint gml:id="USGS.MP.USGS.09070500.00060.21243.00000">
        //<gml:descriptionReference xlink:href="http://waterservices.usgs.gov/nwis/site/?sites=09070500&amp;agencyCd=USGS&amp;format=rdb" xlink:title="COLORADO RIVER NEAR DOTSERO, CO"/>
        //<sa:sampledFeature xlink:title="COLORADO RIVER NEAR DOTSERO, CO"/>
        //<sams:shape>
        //  <gml:Point gml:id="USGS.P.USGS.09070500.00060.21243.00000">
        //    <gml:pos srsName="urn:ogc:def:crs:EPSG:4326">39.6446111 -107.0780139</gml:pos>
        //  </gml:Point>
        //</sams:shape>
    	Element shapeElement = this.xmlToolkit.getSingleElement(monitoringPointElement, "shape");
    	if ( shapeElement != null ) {
    		// Could be other than Point but for stations the Point will generally be used
    		Element pointElement = this.xmlToolkit.getSingleElement(monitoringPointElement, "Point");
    		if ( pointElement != null ) {
    			Element posElement = this.xmlToolkit.getSingleElement(monitoringPointElement, "pos");
    			if ( posElement != null ) {
    				String srsName = this.xmlToolkit.getNodeAttribute("srsName",posElement);
    				String coords = posElement.getTextContent();
    				String coordsParts [] = coords.trim().split(" ");
					if ( (srsName.indexOf("EPSG") >= 0) && (srsName.indexOf("4326") >= 0) && (coordsParts.length == 2) ) {
						try {
							latitude = Double.parseDouble(coordsParts[0]);
							longitude = Double.parseDouble(coordsParts[1]);
						}
						catch ( Exception e ) {
							this.warningMessages.add("Error parsing location coordinates \"" + coords + "\"" );
						}
					}
    			}
    		}
    	}
    }
    Element measurementTimeSeriesElement = this.xmlToolkit.getSingleElement(observationMemberElement, measurementTimeSeriesTag);
    // Go up 2 levels from MeasurementTimeSeries to find the matching observedProperty
    Element observedPropertyElement = this.xmlToolkit.getSingleElement((Element)measurementTimeSeriesElement.getParentNode().getParentNode(), observedPropertyTag);
    Element observationProcessElement = this.xmlToolkit.getSingleElement((Element)measurementTimeSeriesElement.getParentNode().getParentNode(), observationProcessTag);
    Element collectionElement = this.xmlToolkit.getSingleElementPrevious(observationMemberElement, collectionTag);
    Element phenomenonTimeElement = this.xmlToolkit.getSingleElement(observationMemberElement, phenomenonTimeTag);
    if ( phenomenonTimeElement != null ) {
    	Element timePeriodElement = this.xmlToolkit.getSingleElement(phenomenonTimeElement, "TimePeriod");
    	if ( timePeriodElement != null ) {
    	  	Element beginPositionElement = this.xmlToolkit.getSingleElement(phenomenonTimeElement, "beginPosition");
    	  	if ( beginPositionElement != null ) {
    	  		phenomenonBeginPosition = beginPositionElement.getTextContent();
    	  	}
    	  	Element endPositionElement = this.xmlToolkit.getSingleElement(phenomenonTimeElement, "endPosition");
    	  	if ( endPositionElement != null ) {
    	  		phenomenonEndPosition = endPositionElement.getTextContent();
    	  	}
    	}
    }
    Element featureOfInterestElement = this.xmlToolkit.getSingleElement(observationMemberElement, featureOfInterestTag);

    // Get the time series identifier for the time series in order to initialize the time series
    if ( Message.isDebugOn ) {
        Message.printDebug(1,routine,"Parsing TSIdent...");
    }
    TSIdent ident = readTimeSeriesUsingDOM_ParseIdent(watermlVersion, interval,
        monitoringPointElement, measurementTimeSeriesElement, collectionElement);
    if ( Message.isDebugOn ) {
        Message.printDebug(1,routine,"...back from parsing TSIdent, tsident=" + ident );
    }

    try {
        // Create the time series and set basic time series properties
        ts = TSUtil.newTimeSeries(ident.toString(), true);
        ts.setIdentifier(ident);
    }
    catch (Exception ex) {
        throw new IOException("Error setting time series identifier ", ex);
    }
    
    // Set the main time series properties
    
    try {
        // Missing data value
        ts.setMissing ( Double.NaN );
        // Data units
        ts.setDataUnits(readTimeSeriesUsingDOM_ParseUnits(watermlVersion,measurementTimeSeriesElement));
        ts.setDataUnitsOriginal(ts.getDataUnits());
        // Description - set to feature description observed property title if found, and statistic name if found
        String siteName = this.xmlToolkit.getNodeAttribute("xlink:title",featureOfInterestElement);
        String observedProperty = this.xmlToolkit.getNodeAttribute("xlink:title",observedPropertyElement);
        String statistic = null;
        Element obserationProcessParameterElement = this.xmlToolkit.getSingleElement(observationProcessElement, "parameter");
        if ( obserationProcessParameterElement != null ) {
        	Element statisticNameElement = this.xmlToolkit.getSingleElement(obserationProcessParameterElement, "name");
        	if ( statisticNameElement != null ) {
	        	String processParameter = this.xmlToolkit.getNodeAttribute("xlink:title",statisticNameElement);
	        	if ( processParameter != null ) {
	        		statistic = processParameter;
	        	}
        	}
        }
        if ( siteName != null ) {
        	if ( (observedProperty != null) && !observedProperty.isEmpty() ) {
        		siteName = siteName + ", " + observedProperty;
        	}
        	if ( (statistic != null) && !statistic.isEmpty() ) {
        		siteName = siteName + ", " + statistic;
        	}
            ts.setDescription(siteName);
        }
        // Also set properties by passing through XML elements
        boolean setPropertiesFromMetadata = true;
        if ( setPropertiesFromMetadata ) {
            // Set time series properties from the timeSeries elements
        	// TODO smalers 2017-07-01 enable once an figure out from USGS NWIS and other flavors
        	ts.setProperty("MonitoringPoint.id", monitoringPointId);
        	ts.setProperty("phenomenonTime.beginPosition", phenomenonBeginPosition);
        	ts.setProperty("phenomenonTime.endPosition", phenomenonEndPosition);
            //setTimeSeriesPropertyToElementValue(ts,observationMemberElement,"siteName");
            //setTimeSeriesPropertyToElementValue(ts,observationMemberElement,"siteCode");
            //setTimeSeriesPropertyToElementAttributeValue(ts,observationMemberElement,"siteCode","network","network");
            //setTimeSeriesPropertyToElementAttributeValue(ts,observationMemberElement,"siteCode","agencyCode","agencyCode");
            //setTimeSeriesPropertyToElementAttributeValue(ts,observationMemberElement,"timeZoneInfo","siteUsesDaylightSavingsTime","siteUsesDaylightSavingsTime");
            //setTimeSeriesPropertyToElementAttributeValue(ts,observationMemberElement,"defaultTimeZone","zoneAbbreviation","defaultTimeZone");
            //setTimeSeriesPropertyToElementAttributeValue(ts,observationMemberElement,"daylightSavingsTimeZone","zoneAbbreviation","dayligthSavingsTimeZone");
            // From geoLocation
        	ts.setProperty("latitude", latitude);
        	ts.setProperty("longitude", longitude);
            // Other site properties (USGS only?)
            //setTimeSeriesPropertyFromGenericPropertyElement(ts,observationMemberElement,"siteProperty","name","siteTypeCd");
            //setTimeSeriesPropertyFromGenericPropertyElement(ts,observationMemberElement,"siteProperty","name","hucCd");
            //setTimeSeriesPropertyFromGenericPropertyElement(ts,observationMemberElement,"siteProperty","name","stateCd");
            //setTimeSeriesPropertyFromGenericPropertyElement(ts,observationMemberElement,"siteProperty","name","countyCd");
        }
        // History
        if ( (url != null) && !url.equals("") ) {
            // Set creation information from URL
            ts.addToGenesis("Create time series from WaterML 2 queried with URL:  " + url );
        }
        else {
            if ( file != null ) {
                // Set creation information from file
                ts.addToGenesis("Create time series from contents of file:  " + file.getAbsolutePath() );
            }
            // Also extract creation information from the WaterML (probably a file).
            ts.addToGenesis("Read time series by parsing WaterML 2 XML DOM." );
        }
    }
    catch (Exception ex) {
        throw new IOException("Error setting time series properties ", ex);
    }
    
    // Set the time series period and optionally read the data
    String noDataValue = null;
    /*
    String noDataValue = getSingleElementValue(observedPropertyElement, "noDataValue" );
    Message.printStatus(2,routine,"noDataValue string is \"" + noDataValue + "\"");
    if ( Message.isDebugOn ) {
        Message.printDebug(1,routine,"Parsing values...");
    }
    */
    
    readTimeSeriesUsingDOM_ParseValues ( watermlVersion, ts, measurementTimeSeriesElement, noDataValue, readStart, readEnd,
    	outputTimeZoneOffset, outputTimeZone, readData, requireDataToMatchInterval );
    if ( Message.isDebugOn ) {
        Message.printDebug(1,routine,"...back from parsing values");
    }
    
    return ts;
}

/**
 * Parse the qualifiers for a measurement and return a string suitable to set as time series value data flag.
 * @param measurementTVPElement
 * @param ts
 * @param tsDataFlagMetadataList list of time series flag metadata, checked to make sure redundant flag is not added
 * @param defaultQualifier
 * @return full qualifier string, which can be used for time series data flag, or return null if no qualifiers.
 */
private String readTimeSeriesUsingDOM_ParseDataQualifier ( Element measurementTVPElement, TS ts,
	List<TSDataFlagMetadata> tsDataFlagMetadataList, String defaultQualifier ) throws IOException {
	StringBuilder dataFlagB = new StringBuilder(defaultQualifier);
	// Data flag is read from qualifier, can be more than one qualifier for a value
	// <wml2:MeasurementTVP>
	// <wml2:time>2010-03-14T00:00:00-07:00</wml2:time>
	// <wml2:value>775.0</wml2:value>
	// <wml2:metadata>
	//   <wml2:TVPMeasurementMetadata>
	//     <wml2:qualifier>
	//       <swe:Category definition="http://waterdata.usgs.gov/nwisweb/rdf?agingCd=A">
	//         <swe:value>A</swe:value>
	//       </swe:Category>
	//     </wml2:qualifier>
	//   </wml2:TVPMeasurementMetadata>
	// </wml2:metadata>
	// </wml2:MeasurementTVP>
	if ( measurementTVPElement != null ) {
		// Can be multiple qualifiers, set with separating commas
		Element metadataElement = this.xmlToolkit.getSingleElement(measurementTVPElement, "metadata");
		if ( metadataElement != null ) {
			Element tVPMeasurementMetadataElement = this.xmlToolkit.getSingleElement(metadataElement, "TVPMeasurementMetadata");
			if ( tVPMeasurementMetadataElement != null ) {
				List<Element> qualifierElements = this.xmlToolkit.getElements(tVPMeasurementMetadataElement, "qualifier");
				if ( qualifierElements != null ) {
					for ( Element qualifierElement : qualifierElements ) {
						Element categoryElement = this.xmlToolkit.getSingleElement(qualifierElement, "Category");
						if ( categoryElement != null ) {
							Element valueElement = this.xmlToolkit.getSingleElement(qualifierElement, "value");
							if ( valueElement != null ) {
								String value = valueElement.getTextContent();
								if ( (value != null) && !value.isEmpty() ) {
									if ( dataFlagB.length() > 0 ) {
										dataFlagB.append(",");
									}
									dataFlagB.append(value);
									// Also add to time series flag list if not already added
									// - TODO smalers 2017-07-12 unfortunately need to follow definition xlink to get definition
									boolean found = false;
									for ( TSDataFlagMetadata flagMeta : tsDataFlagMetadataList ) {
										if ( flagMeta.getDataFlag().equals(value) ) {
											found = true;
											break;
										}
									}
									if ( !found ) {
										ts.addDataFlagMetadata(new TSDataFlagMetadata(value, value));
									}
								}
							}
						}
					}
				}
			}
		}
	}
	if ( dataFlagB.length() == 0 ) {
		return null;
	}
	else {
		return dataFlagB.toString();
	}
}

/**
 * Parse the WaterML information necessary to create default qualifier (data flags in time series).
 * @param measurementTimeSeriesElement XML element for MeasurementTimeSeries.
 * @param ts time series being processed, will add the qualifier flag definition
 * @return qualifier string, which can be appended to for final qualifier string associated with time series value
 */
private String readTimeSeriesUsingDOM_ParseDefaultQualifier(Element measurementTimeSeriesElement, TS ts) throws IOException {
	// Parse:
    //<wml2:MeasurementTimeseries gml:id="TS.USGS.09070500.00060.21243.00000">
    //<wml2:defaultPointMetadata>
    //  <wml2:DefaultTVPMeasurementMetadata>
    //    <wml2:qualifier>
    //      <swe:Category definition="http://waterdata.usgs.gov/nwisweb/rdf?agingCd=P">
    //        <swe:value>P</swe:value>
    //      </swe:Category>
    //    </wml2:qualifier>
    //    <wml2:uom xlink:title="ft3/s"/>
    //    <wml2:interpolationType xlink:href="www.opengis.net/def/waterml/2.0/interpolationType/Continuous" xlink:title="Continuous"/>
    //  </wml2:DefaultTVPMeasurementMetadata>
    //</wml2:defaultPointMetadata>
	StringBuilder qualifier = new StringBuilder();
	if ( measurementTimeSeriesElement != null ) {
		Element defaultPointMetadataElement = this.xmlToolkit.getSingleElement(measurementTimeSeriesElement, "defaultPointMetadata");
		if ( defaultPointMetadataElement != null ) {
			Element defaultTVPMeasurementMetadataElement = this.xmlToolkit.getSingleElement(defaultPointMetadataElement, "DefaultTVPMeasurementMetadata");
			if ( defaultTVPMeasurementMetadataElement != null ) {
				List<Element> qualifierElements = this.xmlToolkit.getElements(defaultTVPMeasurementMetadataElement, "qualifier");
				if ( qualifierElements != null ) {
					for ( Element qualifierElement : qualifierElements ) {
						Element categoryElement = this.xmlToolkit.getSingleElement(qualifierElement, "Category");
						if ( categoryElement != null ) {
							Element valueElement = this.xmlToolkit.getSingleElement(qualifierElement, "value");
							if ( valueElement != null ) {
								String value = valueElement.getTextContent();
								if ( (value != null) && !value.isEmpty() ) {
									if ( qualifier.length() > 0 ) {
										qualifier.append(",");
									}
									qualifier.append(value);
									// Also add to time series flag list
									// - TODO smalers 2017-07-12 unfortunately need to follow definition xlink to get definition
									ts.addDataFlagMetadata(new TSDataFlagMetadata(value, value));
								}
							}
						}
					}
				}
			}
		}
	}
	return qualifier.toString();
}

/**
Parse the WaterML information necessary to create a unique TSIdent, which will be used to create
a time series.
@param watermlVersion the WaterML version being used
@param interval indicates the interval for data in the file, needed as a hint because there is nothing
in the file that indicates that data are daily values, etc.
@param monitoringPointElement MonitoringPoint element (within featureOfInterest element).
@param measurementTimeSeriesElemen MeasurementTimeSeries element (within results element).
@param valuesElement
@return TSIdent instance suitable for the time series
@throws IOException
*/
private TSIdent readTimeSeriesUsingDOM_ParseIdent(WaterMLVersion watermlVersion, TimeInterval interval,
    Element monitoringPointElement, Element measurementTimeSeriesElement, Element collectionElement )
throws IOException
{   //String routine = getClass().getSimpleName() + ".readTimeSeries_ParseIdent";
    TSIdent ident = new TSIdent();

    // Location ID for USGS is within the following:
    // <wml2:MonitoringPoint gml:id="USGS.MP.USGS.09070500.00060.18624.00003">
    String id = this.xmlToolkit.getNodeAttribute("gml:id", monitoringPointElement);
    String siteId = null;
    if ( (id == null) || id.isEmpty() ) {
    	// Trouble
    	throw new IOException("Unable to determine site id = no id attribute for MonitoringPoint element");
    }
    else {
    	// Parse the id using period as delimiter
    	String [] siteIdParts = id.split(Pattern.quote("."));
    	if ( siteIdParts.length != 7 ) {
    		throw new IOException("Unable to determine site id = MonitoringPoint element id attribute \"" + id + "\" does not have 7 parts");
    	}
    	siteId = siteIdParts[3];
    }
    ident.setLocation(siteId);

    // Data type is taken from the following:
    //  <wml2:MeasurementTimeseries gml:id="TS.USGS.09070500.00060.18624.00003">
    String id2 = this.xmlToolkit.getNodeAttribute("gml:id", measurementTimeSeriesElement);
    String dataType = null;
    String dataTypeSub = null;
    if ( (id2 == null) || id2.isEmpty() ) {
    	// Trouble
    	throw new IOException("Unable to determine data type = no id attribute for MeasurementTimeSeries element");
    }
    else {
    	// Parse the id using period as delimiter
    	String [] idParts = id2.split(Pattern.quote("."));
    	if ( idParts.length != 6 ) {
    		throw new IOException("Unable to determine data type = MeasurementTimeSeries element id attribute \"" + id2 + "\" does not have 6 parts");
    	}
    	dataType = idParts[3];
    	dataTypeSub = idParts[5];
    }
    if ( dataTypeSub == null ) {
    	ident.setType(dataType);
    }
    else {
    	ident.setType(dataType + "-" + dataTypeSub);
    }
    
    try {
        // Currently the interval is required as a command parameter to ensure that it is correct
        if ( interval != null ) {
            ident.setInterval("" + interval);
        }
        else {
        	// Try to get the interval from the file.
            if ( watermlVersion == WaterMLVersion.STANDARD_2_0 ) {
                ident.setInterval(readTimeSeriesUsingDOM_ParseInterval(collectionElement));
            }
        }
    }
    catch (Exception ex) {
        throw new IOException("Error parsing interval (" + ex + ").", ex);
    }

    // Data source is taken from the following:
    //  <wml2:MeasurementTimeseries gml:id="TS.USGS.09070500.00060.18624.00003">
    if (watermlVersion == WaterMLVersion.STANDARD_2_0) {
    	String id3 = this.xmlToolkit.getNodeAttribute("gml:id", measurementTimeSeriesElement);
        String source = null;
        if ( (id3 == null) || id3.isEmpty() ) {
        	// Trouble
        	throw new IOException("Unable to determine data source = no id attribute for MeasurementTimeSeries element");
        }
        else {
        	// Parse the id using period as delimiter
        	String [] idParts = id3.split(Pattern.quote("."));
        	if ( idParts.length != 6 ) {
        		throw new IOException("Unable to determine data source = MeasurementTimeSeries element id attribute \"" + id3 + "\" does not have 6 parts");
        	}
        	source = idParts[1];
        }
       	ident.setSource(source);
    }
    return ident;
}

/**
 * Return the time series data interval.
 * @param collectionElement CollectionElement (typically first in file),
 * assuming that all WaterML 2.0 time series have the same interval.
 * @return the TSTool-compatible interval
 * @throws IOException
 */
private String readTimeSeriesUsingDOM_ParseInterval(Element collectionElement) throws IOException {
    Element nameElement = this.xmlToolkit.findSingleElement(collectionElement, "name", "codespace");
    if ( nameElement != null ) {
    	String codespace = this.xmlToolkit.getNodeAttribute("codespace", nameElement);
    	if ( codespace != null ) {
    		if ( codespace.indexOf("nwis/dv") >= 0 ) {
    			// TODO smalers 2017-07-01 need to make more generic since this is hard-coded for NWIS
    			return TimeInterval.getName(TimeInterval.DAY,0);
    		}
    		else if ( codespace.indexOf("nwis/iv") >= 0 ) {
    			// TODO smalers 2017-07-01 need to make more generic since this is hard-coded for NWIS
    			return "15" + TimeInterval.getName(TimeInterval.MINUTE,0);
    		}
    	}
    }
    return null;
}

/**
Parse data units from the DOM.
@param measurementTimeSeriesElement the MeasurementTimeSeries element that contains unit information
@return the units as a string
*/
private String readTimeSeriesUsingDOM_ParseUnits ( WaterMLVersion watermlVersion, Element measurementTimeSeriesElement )
throws IOException
{
	// Extract units from the following wml2:uom element:
	//
    //<wml2:MeasurementTimeseries gml:id="TS.USGS.09070500.00060.18624.00003">
    //<wml2:defaultPointMetadata>
    //  <wml2:DefaultTVPMeasurementMetadata>
    //    <wml2:qualifier>
    //      <swe:Category definition="http://waterdata.usgs.gov/nwisweb/rdf?agingCd=P">
    //        <swe:value>P</swe:value>
    //      </swe:Category>
    //    </wml2:qualifier>
    //    <wml2:uom xlink:title="ft3/s"/>
    //    <wml2:interpolationType/>
    //  </wml2:DefaultTVPMeasurementMetadata>
    //</wml2:defaultPointMetadata>

    String units = "";
    Element unitsElement = this.xmlToolkit.findSingleElement(measurementTimeSeriesElement, "uom", "title");
    if ( unitsElement != null ) {
    	units = this.xmlToolkit.getNodeAttribute("xlink:title", unitsElement);
    }
    if ( units == null ) {
    	units = "";
    }
    return units;
}

/**
Parse time series values from the DOM, and also set the period.
@param ts the time series that has been previously created and initialized
@param measurementTimeSeriesElement element containing a list of data values elements
@param noDataValue text that indicates no data value
@param readStart starting date/time to read
@param readEnd ending date/time to read
@param outputTimeZoneOffset the desired output time zone in offset notation (e.g, "-07:00", null or empty to ignore)
@param outputTimeZone the time zone to set after the offset is applied (empty will cause set, null will ignore).
@param readData whether to read data values (if false initialize the period but do not allocate
memory or process the data values)
@param requireDataToMatchInterval if true, the date/times with data values must align with the interval (if they
don't warnings will be generated)
*/
private void readTimeSeriesUsingDOM_ParseValues(WaterMLVersion watermlVersion, TS ts, Element measurementTimeSeriesElement,
    String noDataValue, DateTime readStart, DateTime readEnd, String outputTimeZoneOffset, String outputTimeZone,
    boolean readData, boolean requireDataToMatchInterval )
throws IOException
{   String routine = getClass().getSimpleName() + ".readTimeSeries_ParseValues";

	// Whether using time zone, generally the case for interval finer than day
	boolean doUseTimeZone = false;
    // TODO SAM 2011-01-11 Why not just used beginDateTime and endDateTime for the period?
    NodeList measurementTVPList = measurementTimeSeriesElement.getElementsByTagNameNS("*","MeasurementTVP");
    if ( (measurementTVPList == null) || (measurementTVPList.getLength() == 0) ) {
        // No data to process.  This may occur, for example, if a date range is requested that has no data
        return;
    }
    Message.printStatus(2, routine, "Have " + measurementTVPList.getLength() + " MeasurementTVP elements to process as time series data.");
    Element MeasurementTVPElement = null;
    Element timeElement = null;
    DateTime dataStart = null;
    try {
    	MeasurementTVPElement = (Element)measurementTVPList.item(0);
    	timeElement = this.xmlToolkit.findSingleElement(MeasurementTVPElement, "time");
        dataStart = DateTime.parse( timeElement.getTextContent(), DateTime.FORMAT_ISO_8601);
    }
    catch (Exception ex) {
        throw new IOException("Error parsing first data time \"" + MeasurementTVPElement.getTextContent() + "\"", ex);
    }
    // DO NOT use count data in the WaterML to determine the end date.  Apparently missing values in the original
    // data result in no XML record.  Therefore it is necessary to parse all DateTime strings from the data.
    DateTime dataEnd = null;
    try {
    	MeasurementTVPElement = (Element)measurementTVPList.item(measurementTVPList.getLength() - 1);
    	timeElement = this.xmlToolkit.findSingleElement(MeasurementTVPElement, "time");
    	dataEnd = DateTime.parse( timeElement.getTextContent(), DateTime.FORMAT_ISO_8601);
    }
    catch (Exception ex) {
        throw new IOException("Error parsing last data time \"" + MeasurementTVPElement.getTextContent() + "\"", ex);
    }
    // Reset the time zone for regular interval to ensure consistency
	if ( (dataStart != null) && (dataStart.getPrecision() < DateTime.PRECISION_DAY) ) {
		if ( (outputTimeZoneOffset != null) && !outputTimeZoneOffset.isEmpty() ) {
			dataStart.shiftTimeZone(outputTimeZoneOffset);
		}
		if ( outputTimeZone != null ) {
			dataStart.setTimeZone(outputTimeZone);
		}
	}
	if ( (dataEnd != null) && (dataEnd.getPrecision() < DateTime.PRECISION_DAY) ) {
		if ( (outputTimeZoneOffset != null) && !outputTimeZoneOffset.isEmpty() ) {
			dataEnd.shiftTimeZone(outputTimeZoneOffset);
		}
		if ( outputTimeZone != null ) {
			dataEnd.setTimeZone(outputTimeZone);
		}
	}
    // Problem... if the input period does not align with the data interval, then the dates are off during iteration.
    // Consequently, make sure the readStart and readEnd align with the interval
    if ( readStart == null ) {
        readStart = new DateTime(dataStart);
    }
    else {
        readStart = new DateTime(readStart);
        readStart.round(-1, ts.getDataIntervalBase(), ts.getDataIntervalMult());
    }
    if ( readEnd == null ) {
        readEnd = new DateTime(dataEnd);
    }
    else {
        readEnd = new DateTime(readEnd);
        readEnd.round(1, ts.getDataIntervalBase(), ts.getDataIntervalMult());
    }
    ts.setDate1(readStart);
    ts.setDate1Original(dataStart);
    ts.setDate2(readEnd);
    ts.setDate2Original(dataEnd);
    String dataValueString;
    double dataValue;
    String dataFlag;
    DateTime dateTime;
    String intervalString = ts.getIdentifier().getInterval(); // Used to check alignment
    TimeInterval interval = null;
    try {
        interval = TimeInterval.parseInterval(intervalString);
    }
    catch ( Exception e ) {
        // Should not happen
    }
    if ( interval.getBase() < TimeInterval.DAY ) {
    	// TODO smalers 2017-07-06 need to evaluate future using IrregDay, etc.
    	// -for now assume irregular will include time zone
    	doUseTimeZone = true;
    }
    // noDataValue as string may be something like -999999.0 but -999999 sometimes shows up in file
    // Create an integer version of the string.  This adds a bit of overhead checking values but is necessary.
    /*
    String noDataValueInt = noDataValue;
    try {
        double d = Double.parseDouble(noDataValue);
        if ( d < 0.0 ) {
            d = d - .01;
        }
        else if ( d > 0.0 ) {
            d = d + .01;
        }
        noDataValueInt = "" + (int)d;
        Message.printStatus(2,routine,"Alternative noDataValue string is \"" + noDataValueInt + "\"");
    }
    catch ( NumberFormatException e ){
        noDataValueInt = noDataValue;
    }*/
    if ( readData ) {
        ts.allocateDataSpace();
        // Set the default qualifier, which will be applied to every value (and possibly other qualifiers)
        String defaultQualifier = readTimeSeriesUsingDOM_ParseDefaultQualifier(measurementTimeSeriesElement,ts);
        Element valueElement;
        String firstTimeZone = null; // First time zone encountered
        String dtTimeZone = null; // Time zone from parse date/time
        int maxTimeZoneErrors = 50;
        int timeZoneErrorsCount = 0;
        boolean isRegularInterval = TimeInterval.isRegularInterval(ts.getDataIntervalBase());
        for (int i = 0; i < measurementTVPList.getLength(); i++) {
            // Must parse the dates because missing results in gaps
            try {
                Element measurementTVPElement = (Element) measurementTVPList.item(i);
                timeElement = this.xmlToolkit.findSingleElement(measurementTVPElement, "time");
                if ( timeElement == null ) {
                	Message.printStatus(2, routine, "Skipping data value since no time specified." );
                }
                dateTime = DateTime.parse(timeElement.getTextContent(), DateTime.FORMAT_ISO_8601);
                if ( doUseTimeZone ) {
                	// Time zone applies because interval is < day
                	if ( (outputTimeZoneOffset != null) && !outputTimeZoneOffset.isEmpty() ) {
                		// Desired time zone has been provided so use it to enforce consistency
                		dateTime.shiftTimeZone(outputTimeZoneOffset);
                	}
                	else {
                		// Time zone has not been specified so check to make sure it is consistent - only if regular interval
                		if ( isRegularInterval ) {
			                if ( firstTimeZone == null ) {
			                	// Set the time zone
			                	firstTimeZone = dateTime.getTimeZoneAbbreviation();
			                }
			                else {
			                	// Verify that time zone is consistent
			                	if ( timeZoneErrorsCount < maxTimeZoneErrors ) {
				                	dtTimeZone = dateTime.getTimeZoneAbbreviation();
				                	if ( (dtTimeZone != null) && !dtTimeZone.equalsIgnoreCase(firstTimeZone) )  {
				                		this.failureMessages.add("Time zone for " + timeElement.getTextContent() +
				                			" is different than initial time zone in content \"" + firstTimeZone + "\" - need to set time zone." );
				                	}
				                	++timeZoneErrorsCount;
			                	}
			                }
                		}
                	}
                	if ( outputTimeZone != null ) {
                		dateTime.setTimeZone(outputTimeZone);
                	}
                }
                if ( dateTime.lessThan(readStart) || dateTime.greaterThan(readEnd) ) {
                    // Date/time is not in the requested period
                	Message.printStatus(2, routine, "Setting data time " + dateTime + " is not in the period " + readStart + " to " + readEnd );
                    continue;
                }
                if ( requireDataToMatchInterval ) {
                    // Do a check to see if the date/time aligns exactly with the interval
                    if ( TimeUtil.compareDateTimePrecisionToTimeInterval(dateTime, interval, requireDataToMatchInterval ) != 0 ) {
                        this.warningMessages.add("Date/time " + dateTime + " is not aligned with time series interval " +
                            intervalString );
                        // Ignore if regular interval
                        if ( TimeInterval.isRegularInterval(ts.getDataIntervalBase())) {
                        	continue;
                        }
                    }
                }
                dataFlag = readTimeSeriesUsingDOM_ParseDataQualifier(measurementTVPElement,ts,ts.getDataFlagMetadataList(),defaultQualifier);
                valueElement = this.xmlToolkit.findSingleElement(measurementTVPElement, "value");
                if ( valueElement != null ) {
                	dataValueString = valueElement.getTextContent();
                	// TODO smalers 2017-07-01 need to evaluate how missing value is handled
                	/*
                    if ( dataValueString.equals(noDataValue) || dataValueString.equals(noDataValueInt)) {
                        // Missing
                        dataValue = Double.NaN;
                    }
                    else {*/
                        dataValue = Double.parseDouble(dataValueString);
                    //}
                    if ( (dataFlag != null) && !dataFlag.equals("") ) {
                        ts.setDataValue(dateTime, dataValue, dataFlag, 0);
                    }
                    else {
                        ts.setDataValue(dateTime, dataValue);
                    }
                    if ( Message.isDebugOn ) {
                    	Message.printDebug(1, routine, "Setting data value at " + dateTime + " to " + dataValue );
                    }
                }
            }
            catch ( Exception e ) {
                // Bad record.
                Message.printWarning(3,"","Bad data record (" + e + ") - skipping..." );
                Message.printWarning(3, routine,  e);
            }
        }
    }
}

/**
Set a time series property to a WaterML element's value.  For example, use to set the siteName as a property:
<pre>
<ns1:siteName>CACHE LA POUDRE RIV AT MO OF CN, NR FT COLLINS, CO</ns1:siteName>
</pre>
If the element is not in the DOM, don't set as a time series property.
@param ts time series being processed
@param timeSeriesElement a time series DOM element as the starting point for the element search
@param elementName the element name to set as the property
*/
private void setTimeSeriesPropertyToElementValue ( TS ts, Element timeSeriesElement, String elementName )
{
	if ( timeSeriesElement != null ) {
	    try {
	        Element el = this.xmlToolkit.getSingleElement(timeSeriesElement, elementName);
	        if ( el != null ) {
	            String text = el.getTextContent();
	            ts.setProperty(elementName, (text == null) ? "" : text );
	        }
	    }
	    catch ( IOException e ) {
	        return;
	    }
	}
}

/**
Set a time series property to a WaterML element's attribute value.  For example, use to set the agencyCode as a property:
<pre>
<ns1:siteCode network="NWIS" agencyCode="USGS">06752000</ns1:siteCode>
</pre>
If the element is not in the DOM, don't set as a time series property.
@param ts time series being processed
@param timeSeriesElement a time series DOM element as the starting point for the element search
@param elementName the element name to set as the property
@param propertyName property name to assign (generally either the element or attribute name)
*/
private void setTimeSeriesPropertyToElementAttributeValue ( TS ts, Element timeSeriesElement, String elementName,
    String attributeName, String propertyName )
{	if ( timeSeriesElement != null ) {
	    try {
	        Element el = this.xmlToolkit.getSingleElement(timeSeriesElement, elementName);
	        if ( el != null ) {
	            NodeList nodes = timeSeriesElement.getElementsByTagNameNS("*",elementName);
	            if ( nodes.getLength() == 0 ) {
	                return;
	            }
	            if ( nodes.getLength() == 0 ) {
	                return;
	            }
	            // Now match the attribute name and value
	            for ( int i = 0; i < nodes.getLength(); i++ ) {
	                Node element = nodes.item(i);
	                // Element value is for the element (not attribute)
	                NamedNodeMap nodeMap = element.getAttributes();
	                Node attribute = nodeMap.getNamedItem(attributeName);
	                if ( (attribute != null) && attribute.getNodeName().equalsIgnoreCase(attributeName) ) {
	                    // Found the attribute of interest
	                    String text = attribute.getTextContent();
	                    ts.setProperty(propertyName, (text == null) ? "" : text );
	                }
	            }
	        }
	    }
	    catch ( IOException e ) {
	        return;
	    }
	}
}

/**
Set a time series property from a WaterML element that can be repeated. For example, for the first line below
the property name "siteTypeCd" will be set to the value "ST":
<pre>
<ns1:siteProperty name="siteTypeCd">ST</ns1:siteProperty>
<ns1:siteProperty name="hucCd">10190007</ns1:siteProperty>
<ns1:siteProperty name="stateCd">08</ns1:siteProperty>
<ns1:siteProperty name="countyCd">08069</ns1:siteProperty>
</pre>
If the element is not in the DOM, don't set as a time series property.
@param ts time series being processed
@param timeSeriesElement a time series DOM element as the starting point for the element search
@param elementName the element name to match (e.g., "siteProperty" above)
@param attributeName the attribute name to match (e.g., "name" above)
@param attributeValue the name of the attribute value to match, used for the property name to set (e.g., "siteTypeCd" above)
*/
private void setTimeSeriesPropertyFromGenericPropertyElement ( TS ts, Element timeSeriesElement, String elementName,
    String attributeName, String attributeValue )
{/*
    //try {
        // First get all matching elements
        NodeList nodes = timeSeriesElement.getElementsByTagNameNS("*",elementName);
        if ( nodes.getLength() == 0 ) {
            return;
        }
        else {
            // Now match the attribute name and value
            for ( int i = 0; i < nodes.getLength(); i++ ) {
                Node element = nodes.item(i);
                // Element value is for the element (not attribute)
                String text = element.getTextContent();
                NamedNodeMap nodeMap = element.getAttributes();
                Node attribute = nodeMap.getNamedItem(attributeName);
                if ( (attribute != null) && attribute.getNodeName().equalsIgnoreCase(attributeName) &&
                    attribute.getTextContent().equalsIgnoreCase(attributeValue) ) {
                    // Found the node (attribute) of interest
                    ts.setProperty(attributeValue, (text == null) ? "" : text );
                }
            }
        }
    //}
    //catch ( IOException e ) {
    //    return;
    //}
     */
}

}
