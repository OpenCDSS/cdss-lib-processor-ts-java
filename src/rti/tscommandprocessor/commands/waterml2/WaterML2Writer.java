package rti.tscommandprocessor.commands.waterml2;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import RTi.TS.TS;
import RTi.TS.TSData;
import RTi.TS.TSIterator;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Time.DateTime;
import net.opengis.gml._3.FeatureArrayPropertyType;
import net.opengis.gml._3.FeatureCollectionType;
import net.opengis.waterml._2.CollectionType;

/**
 * This class writes WaterML 2 files.
 * @author sam
 *
 */
public class WaterML2Writer {
	
	/**
	 * Values for Dialect parameter.
	 * TODO smalers 2017-07-16 could make an enum.
	*/
	public final static String USGS = "USGS";
	
	/**
	 * Constructor.
	 */
	public WaterML2Writer () {
		
	}
	
	/**
	 * Determine the stations from the time series.
	 * @param tslist list of time series to extract stations.
	 * @return list of stations, at this point just identifiers but in the future could be more complex object.
	 */
	private List<String> getTimeSeriesStations ( List<TS> tslist ) {
		List<String> stationList = new ArrayList<String>();
		for ( TS ts : tslist ) {
			String stationId = ts.getLocation();
			boolean found = false;
			for ( String s : stationList ) {
				if ( stationId.equalsIgnoreCase(s) ) {
					found = true;
					break;
				}
			}
			if ( !found ) {
				stationList.add(stationId);
			}
		}
		return stationList;
	}
	
	/**
	Write the version 2.0 format WaterML by using the API.
	@param fout open PrintWriter to write to
	@param tslist list of time series to write
	@param precision number of digits after decimal point to write
	@param missingValue the value to output for missing values
	@param outputStart output start or null to write full period
	@param outputEnd output end or null to write full period
	@param printNice if true, print JSON in pretty format and output nulls
	@param errors list of error strings to be propagated to calling code
	*/
	public void writeTimeSeriesListUsingAPI ( List<TS> tslist, String outputFile, String dialect, Integer precision, String missingValue,
	    DateTime outputStart, DateTime outputEnd, boolean printNice, List<String> errors ) throws JAXBException {
 		JAXBContext context = JAXBContext.newInstance("net.opengis.waterml._2");
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.opengis.net/waterml/2.0 " + "http://schemas.opengis.net/waterml/2.0/waterml2.xsd");
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); // Should indent output
		// Factories that make it a bit easier to understand scopes of objects related to schema
 		net.opengis.waterml._2.ObjectFactory WaterML_Factory = new net.opengis.waterml._2.ObjectFactory();
 		net.opengis.gml._3.ObjectFactory GML_Factory = new net.opengis.gml._3.ObjectFactory();
 		// Root objects that are marshalled at the end
 		CollectionType collection = null; // USGS dialect wrapper class
		FeatureCollectionType featureCollection = null; // General WaterML 2
		// Collection is the normal WaterML 2.0 root element
		collection = WaterML_Factory.createCollectionType();
		if ( dialect.equalsIgnoreCase(USGS) ) {
			// The USGS adds a FeatureCollection element as a wrapper around the normal WaterML 2.0 Collection root element
			// - seems like this is related to the service information (time generated, etc.?)
			featureCollection = new FeatureCollectionType();
			FeatureArrayPropertyType featureArrayProperty = GML_Factory.createFeatureArrayPropertyType();
			JAXBElement<CollectionType> jaxbCollection = WaterML_Factory.createCollection(collection);
			featureArrayProperty.getAbstractFeature().add(jaxbCollection);
			featureCollection.setFeatureMembers(featureArrayProperty);
			// Now the FeatureCollection is set up and the remainder of data can be added as per normal WaterML 2 API
		}
		// Transfer the time series to the data objects by adding to the Collection and digging in
		// ObservationMember is equivalent to station so have to determine the unique list of stations in the time series
		// - do so by looking for the unique location IDs
		List<String> stationList = getTimeSeriesStations ( tslist );
		for ( String stationId : stationList ) {
			//collection.add
			// TODO smalers 2017-07-16 Need to transfer Kory Clark's example code into product code
		}
		// Output the WaterML file
		File of = new File(outputFile);
		//QName qName = new QName("com.codenotfound.jaxb.model", "car");
		// See:  https://www.codenotfound.com/2013/07/jaxb-marshal-element-missing-xmlrootelement-annotation.html
		// See (better):  https://stackoverflow.com/questions/33823139/jaxb-unmarshalling-without-xmlrootelement-annotation
		//JAXBElement<FeatureCollectionType> root = new JAXBElement<>(qName, FeatureCollectionType.class, root);
		if ( dialect.equalsIgnoreCase(USGS) ) {
			// USGS dialect WaterML 2.0 is to output with the FeatureCollection as the root element
			marshaller.marshal(new JAXBElement<FeatureCollectionType>(
				new QName("", "FeatureCollection"), FeatureCollectionType.class, null, featureCollection), of);
		}
		else {
			// Standard WaterML 2.0 is to output with the Collection as the root element
			marshaller.marshal(new JAXBElement<CollectionType>(
				new QName("", "Collection"), CollectionType.class, null, collection), of);
		}
	}
	
	/**
	Write the version 2.0 format WaterML by creating the DOM as text.
	This code is essentially the same as found in the WriteWaterML class but is expected to be filled out more completely.
	@param fout open PrintWriter to write to
	@param tslist list of time series to write
	@param precision number of digits after decimal point to write
	@param missingValue the value to output for missing values
	@param outputStart output start or null to write full period
	@param outputEnd output end or null to write full period
	@param printNice if true, print JSON in pretty format and output nulls
	@param errors list of error strings to be propagated to calling code
	*/
	public void writeTimeSeriesListUsingDOM ( List<TS> tslist, String outputFile, Integer precision, String missingValue,
	    DateTime outputStart, DateTime outputEnd, boolean printNice, List<String> errors )
	{   String routine = getClass().getSimpleName() + "writeTimeSeriesListUsingDOM";
		PrintWriter fout = null;
    	try {
	        FileOutputStream fos = new FileOutputStream ( outputFile );
	        fout = new PrintWriter ( fos );
			Message.printStatus(2,routine,"Writing " + tslist.size() + " time series to WaterML 2.0 file." );
			// TODO brute force output until WaterML package can be generated in a robust way
			String s2 = "  ";
			String s4 = s2 + "  ";
			String s6 = s4 + "  ";
			String s8 = s6 + "  ";
			String s10 = s8 + "  ";
			String s12 = s10 + "  ";
			String s14 = s12 + "  ";
			String s16 = s14 + "  ";
			String s18 = s16 + "  ";
			// First write the header
			fout.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			fout.println("<wml2:Collection xmlns:wml2=\"http://www.opengis.net/waterml/2.0\" "
					+ "xmlns:gml=\"http://www.opengis.net/gml/3.2\" "
					+ "xmlns:om=\"http://www.opengis.net/om/2.0\" "
					+ "xmlns:sa=\"http://www.opengis.net/sampling/2.0\" "
					+ "xmlns:sams=\"http://www.opengis.net/samplingSpatial/2.0\" "
					+ "xmlns:swe=\"http://www.opengis.net/swe/2.0\" "
					+ "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
					+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
					// TODO SAM 2015-09-13 What is the following?
					//+ "gml:id="C.USGS.01646500" "
					+ "xsi:schemaLocation=\"http://www.opengis.net/waterml/2.0 http://schemas.opengis.net/waterml/2.0/waterml2.xsd\">");
			// TODO SAM 2015-09-13 Need to enable something like the following
			//fout.println(s2+"  <gml:identifier codeSpace=\"http://waterservices.usgs.gov/nwis/dv\">USGS.01646500</gml:identifier>");
			//fout.println(s2+"  <gml:name codeSpace=\"http://waterservices.usgs.gov/nwis/dv\">Timeseries collected at POTOMAC RIVER NEAR WASH, DC LITTLE FALLS PUMP STA</gml:name>");
			fout.println(s2+"<wml2:metadata>");
			//fout.println(s4+"<wml2:DocumentMetadata gml:id=\"doc.USGS.MP.USGS.01646500\">");
			//fout.println(s6+"<gml:metaDataProperty about=\"contact\" xlink:href=\"http://waterservices.usgs.gov\" />");
			//fout.println(s6+"<wml2:generationDate>2015-09-03T15:01:39.566-04:00</wml2:generationDate>");
			//fout.println(s6+"<wml2:version xlink:href="http://www.opengis.net/waterml/2.0" xlink:title="WaterML 2.0" />");
			//fout.println(s4+"</wml2:DocumentMetadata>");
			fout.println(s2+"</wml2:metadata>");
			// Loop through time series
			for ( TS ts : tslist ) {
				if ( (missingValue == null) || missingValue.isEmpty() ) {
					missingValue = "" + ts.getMissing();
				}
				fout.println(s2+"<wml2:observationMember>");
				//      <om:OM_Observation gml:id="obs.USGS.01646500.00010.5.00001">
				DateTime now = new DateTime(DateTime.DATE_CURRENT);
				String tsid = ts.getIdentifierString();
				fout.println(s4+"<om:OM_Observation gml:id=\"" + tsid + "\">" );
				fout.println(s6+"<om:phenomenonTime>");
				fout.println(s8+"<gml:TimePeriod gml:id=\"sample_time." + tsid + "\">" );
				if ( outputStart == null ) {
					fout.println(s10+"<gml:beginPosition>" + ts.getDate1() + "</gml:beginPosition>" );
				}
				else {
					fout.println(s10+"<gml:beginPosition>" + outputStart + "</gml:beginPosition>" );
				}
				if ( outputEnd == null ) {
					fout.println(s10+"<gml:endPosition>" + ts.getDate2() + "</gml:endPosition>" );
				}
				else {
					fout.println(s10+"<gml:endPosition>" + outputEnd + "</gml:endPosition>" );
				}
				fout.println(s8+"</gml:TimePeriod>" );
				fout.println(s6+"</om:phenomenonTime>" );
				fout.println(s6+"<om:resultTime>" );
				fout.println(s8+"<gml:TimeInstant gml:id=\"requested_time" + tsid + "\">" );
				fout.println(s10+"<gml:timePosition>" + now + "</gml:timePosition>" );
				fout.println(s8+"</gml:TimeInstant>" );
				fout.println(s6+"</om:resultTime>" );
				fout.println(s6+"<om:procedure>" );
				fout.println(s8+"<wml2:ObservationProcess gml:id=\"process." + tsid + "\">" );
				fout.println(s10+"<wml2:processType xlink:href=\"http://www.opengis.net/def/waterml/2.0/processType/Sensor\" xlink:title=\"Sensor\" />");
				fout.println(s10+"<wml2:parameter xlink:title=\"Statistic\" xlink:href=\"http://waterdata.usgs.gov/nwisweb/rdf?statCd=00001\">" );
				fout.println(s12+"<om:NamedValue>" );
				fout.println(s14+"<om:name xlink:title=\"Maximum\" />" );
				fout.println(s14+"<om:value xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" xsi:type=\"xs:string\">00001</om:value>" );
				fout.println(s12+"</om:NamedValue>" );
				fout.println(s10+"</wml2:parameter>" );
				fout.println(s8+"</wml2:ObservationProcess>" );
				fout.println(s6+"</om:procedure>" );
				fout.println(s6+"<om:observedProperty xlink:title=\"Temperature, water\" xlink:href=\"http://waterdata.usgs.gov/nwisweb/rdf?parmCd=00010\" />" );
				fout.println(s6+"<om:featureOfInterest xlink:title=\"POTOMAC RIVER NEAR WASH, DC LITTLE FALLS PUMP STA>\"");
				fout.println(s8+"<wml2:MonitoringPoint gml:id=\"" + tsid + "\">" );
				fout.println(s10+"<gml:descriptionReference xlink:href=\"http://waterservices.usgs.gov/nwis/site/?sites=01646500&amp;agencyCd=USGS&amp;format=rdb\" "
						+ "xlink:title=\"POTOMAC RIVER NEAR WASH, DC LITTLE FALLS PUMP STA\" />" );
				fout.println(s10+"<sa:sampledFeature xlink:title=\"POTOMAC RIVER NEAR WASH, DC LITTLE FALLS PUMP STA, 4.1 ft from riverbed (middle)\" />" );
				fout.println(s10+"<sams:shape>" );
				fout.println(s12+"<gml:Point gml:id=\"" + tsid + "\">" );
				fout.println(s14+"<gml:pos srsName=\"urn:ogc:def:crs:EPSG:4326\">38.94977778 -77.12763889</gml:pos>" );
				fout.println(s12+"</gml:Point>" );
				fout.println(s10+"</sams:shape>" );
				fout.println(s8+"</wml2:MonitoringPoint>" );
				fout.println(s6+"</om:featureOfInterest>" );
				fout.println(s6+"<om:result>" );
				fout.println(s8+"<wml2:MeasurementTimeseries gml:id=\"TS." + tsid + "\">" );
				// TODO SAM 2015-09-14 these are flag descriptions - need to handle
				fout.println(s10+"<wml2:defaultPointMetadata>" );
				fout.println(s12+"<wml2:DefaultTVPMeasurementMetadata>" );
				fout.println(s14+"<wml2:qualifier xlink:title=\"Provisional data subject to revision.\">" );
				fout.println(s16+"<swe:Category definition=\"http://waterdata.usgs.gov/nwisweb/rdf?dvQualCd=P\">" );
				fout.println(s18+"<swe:description>Provisional</swe:description>" );
				fout.println(s18+"<swe:value>P</swe:value>" );
				fout.println(s16+"</swe:Category>" );
				fout.println(s14+"</wml2:qualifier>" );
				fout.println(s14+"<wml2:uom xlink:title=\"deg C\" />" );
				fout.println(s14+"<wml2:interpolationType />" );
				fout.println(s12+"</wml2:DefaultTVPMeasurementMetadata>" );
				fout.println(s10+"</wml2:defaultPointMetadata>" );
				TSIterator tsi = null;
				try {
					tsi = ts.iterator(outputStart,outputEnd);
				}
				catch ( Exception e ) {
					Message.printWarning(3,routine,"Error creating iterator for time series (" + e + ").");
					// Skip data output
					continue;
				}
				TSData tsdata = null;
				double value;
				String flag;
				// Precision for numerical values controls the format
				// TODO SAM 2015-09-13 Evaluate whether to default based on units
				String format = "%.4f";
				if ( precision != null ) {
					format = "%." + precision + "f";
				}
				while ( (tsdata = tsi.next()) != null ) {
					fout.println(s10+"<wml2:point>" );
					fout.println(s12+"<wml2:MeasurementTVP>" );
					// TODO SAM 2015-09-13 Need to handle time zone
					fout.println(s14+"<wml2:time>" + tsdata.getDate() + "</wml2:time>" );
					value = tsdata.getDataValue();
					flag = tsdata.getDataFlag();
					if ( ts.isDataMissing(value) ) {
						fout.println(s14+"<wml2:value>" + missingValue + "</wml2:value>" );
					}
					else {
						fout.println(s14+"<wml2:value>" + StringUtil.formatString(value,format) + "</wml2:value>" );
					}
					if ( (flag != null) && !flag.isEmpty() ) {
						// TODO SAM 2015-09-13 Need to add a list of these on the time series if not previously added - metadata for flags
						// TODO SAM 2015-09-14 Need to implement output - need an example
					}
					fout.println(s12+"</wml2:MeasurementTVP>" );
					fout.println(s10+"</wml2:point>" );
				}
				fout.println(s8+"</wml2:MeasurementTimeSeries>" );
				fout.println(s6+"</om:result>" );
				fout.println(s4+"</om:OM_Observation>" );
				fout.println(s2+"</wl2:observationMember>" );
			}
			// Write the footer
			fout.println("</wml2:Collection>");
		}
	    catch ( FileNotFoundException e ) {
	        errors.add ( "Output file \"" + outputFile + "\" could not be created (" + e + ")." );
	    }
	    finally {
	        try {
	            fout.close();
	        }
	        catch ( Exception e ) {
	        }
	    }
	}

}