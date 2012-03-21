package rti.tscommandprocessor.commands.wateroneflow.ws;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import riverside.datastore.AbstractWebServiceDataStore;

import RTi.TS.TS;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;

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
    String name = IOUtil.expandPropertyForEnvironment(props.getValue("Name"));
    String description = IOUtil.expandPropertyForEnvironment(props.getValue("Description"));
    String serviceRootURI = IOUtil.expandPropertyForEnvironment(props.getValue("ServiceRootURI"));
    String version = IOUtil.expandPropertyForEnvironment(props.getValue("Version"));
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
public TS readTimeSeries ()
{
    WaterOneFlowAPI wofAPI = getWaterOneFlowAPI();
    return wofAPI.readTimeSeries();
}

}