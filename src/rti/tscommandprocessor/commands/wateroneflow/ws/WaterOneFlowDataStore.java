package rti.tscommandprocessor.commands.wateroneflow.ws;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Vector;

import javax.xml.bind.JAXBElement;

import org.cuahsi.waterml._1.Units;
import org.cuahsi.waterml._1.UnitsType;
import org.cuahsi.waterml._1.VariableCode;
import org.cuahsi.waterml._1.VariableInfoType;
import org.cuahsi.waterml._1.VariableInfoType.TimeSupport;

import riverside.datastore.AbstractWebServiceDataStore;

import RTi.TS.TS;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;

// TODO SAM 2012-02-28 Need to enable
/**
Data store for WaterOneFlow web services.  This class serves as the developer-usable API as the interface
to the API that is auto-generated from the WSDL.
*/
public class WaterOneFlowDataStore extends AbstractWebServiceDataStore
{

/**
API to WaterOneFlow for the appropriate version.
*/
private WaterOneFlowAPI __wofAPI = null;

/**
Constructor for web service.
@param name datastore name
@param description datastore description
@param serviceRootURI the root URI for the web service API
@param wofAPI the WaterOneFlowAPI implementation (instantiated based on the WaterOneFlow version).
*/
public WaterOneFlowDataStore ( String name, String description, URI serviceRootURI, WaterOneFlowAPI wofAPI )
throws URISyntaxException, IOException
{
    setName ( name );
    setDescription ( description );
    setServiceRootURI ( serviceRootURI );
    __wofAPI = wofAPI;
}

/**
Factory method to construct a data store connection from a properties file.
@param filename name of file containing property strings
*/
public static WaterOneFlowDataStore createFromFile ( String filename )
throws IOException, Exception
{
    // Read the properties from the file
    PropList props = new PropList ("");
    props.setPersistentName ( filename );
    props.readPersistent ( false );
    String name = IOUtil.expandPropertyForEnvironment("Name",props.getValue("Name"));
    String description = IOUtil.expandPropertyForEnvironment("Description",props.getValue("Description"));
    String serviceRootURI = IOUtil.expandPropertyForEnvironment("ServiceRootURI",props.getValue("ServiceRootURI"));
    String version = IOUtil.expandPropertyForEnvironment("Version",props.getValue("Version"));
    WaterOneFlowAPI wof = null;
    // TODO SAM 2012-03-08 Need to automatically determine version from WSDL
    if ( version.equals("1.0") ) {
        wof = new WaterOneFlowAPI_1_0(serviceRootURI);
    }
    else {
        throw new WaterMLVersionNotSupportedException ( "WaterML version is not supported: " + version );
    }

    WaterOneFlowDataStore ds = new WaterOneFlowDataStore( name, description, new URI(serviceRootURI), wof );
    return ds;
}

/**
Return the WaterOneFlowAPI instance that is used for queries.
*/
private WaterOneFlowAPI getWaterOneFlowAPI ()
{
    return __wofAPI;
}

/**
Read a single time series.
*/
public TS readTimeSeries ( String networkName, String siteID, String variable, DateTime readStart, DateTime readEnd,
        String outputFile)
{
    WaterOneFlowAPI wofAPI = getWaterOneFlowAPI();
    return wofAPI.readTimeSeries( networkName, siteID, variable, readStart, readEnd, outputFile );
}

/**
Get the variable list for the network in a form suitiable for editor choices.
*/
public List<String> getVariableList ( boolean includeName, int nameMax )
{   // TODO SAM 2012-04-10 For now read every time but need to cache
    WaterOneFlowAPI wofAPI = getWaterOneFlowAPI();
    List<VariableInfoType> variableInfoTypeList = wofAPI.getVariableInfoTypeList();
    StringBuffer b = new StringBuffer();
    List<String> variableNameList = new Vector();
    String network;
    String name;
    List<VariableCode> variableCodeList;
    Units units;
    String unitsAbbrev;
    JAXBElement<TimeSupport> timeSupportElement;
    TimeSupport timeSupport;
    String timeSupportString;
    UnitsType timeSupportUnitsType;
    String intervalString;
    for ( VariableInfoType variableInfoType: variableInfoTypeList ) {
        units = variableInfoType.getUnits();
        unitsAbbrev = null;
        if ( units != null ) {
            unitsAbbrev = units.getUnitsAbbreviation();
        }
        timeSupportElement = variableInfoType.getTimeSupport();
        timeSupport = timeSupportElement.getValue();
        timeSupportUnitsType = timeSupport.getUnit();
        intervalString = timeSupportUnitsType.getUnitAbbreviation();
        timeSupportString = "" + timeSupport.getTimeInterval();
        Message.printStatus(2, "", "dataType=" + variableInfoType.getDataType() +
            ", network=" + variableInfoType.getNetwork() +
            ", noDataValue=" + variableInfoType.getNoDataValue() +
            ", oid=" + variableInfoType.getOid() +
            ", variableDescription=" + variableInfoType.getVariableDescription() +
            ", variableName=" + variableInfoType.getVariableName() +
            ", vocabulary=" + variableInfoType.getVocabulary() +
            ", extension=" + variableInfoType.getExtension() + // Object
            ", generalCategory=" + variableInfoType.getGeneralCategory() + // Enum
            ", metadataDateTime=" + variableInfoType.getMetadataDateTime() +
            ", note=" + variableInfoType.getNote() +
            ", options=" + variableInfoType.getOptions() +
            ", related=" + variableInfoType.getRelated() +
            ", sampleMedium=" + variableInfoType.getSampleMedium() +
            ", timeSupport(unitAbbrevation)=" + intervalString +
            ", units=" + unitsAbbrev +
            ", valueType=" + variableInfoType.getValueType() );
        b.setLength(0);
        network = variableInfoType.getNetwork();
        if ( network != null ) {
            b.append ( network + ":" );
        }
        // The following seems to return a single item for each of the above, at least for the USGS
        // NWIS daily values.
        variableCodeList = variableInfoType.getVariableCode();
        for ( VariableCode variableCode : variableCodeList ) {
            Message.printStatus(2, "", "    variableID=" + variableCode.getVariableID() +
                 ", network=" + variableCode.getNetwork() +
                 ", value=" + variableCode.getValue() +
                 ", vocabulary=" + variableCode.getVocabulary() );
            b.append( "" + variableCode.getValue() );
            if ( includeName ) {
                name = variableInfoType.getVariableName();
                if ( name != null ) {
                    if ( (nameMax > 0) && (name.length() > nameMax) ) {
                        name = name.substring(0,nameMax) + "...";
                    }
                    b.append( " - " + name );
                }
            }
        }
        variableNameList.add(b.toString());
    }
    return variableNameList;
}

}