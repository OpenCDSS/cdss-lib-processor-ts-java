package rti.tscommandprocessor.commands.wateroneflow.ws;

import java.net.URI;

import RTi.Util.IO.PropList;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreFactory;

/**
 * Factory to instantiate WaterOneFlowDataStore instances.
 * @author sam
 *
 */
public class WaterOneFlowDataStoreFactory implements DataStoreFactory
{

/**
Create a WaterOneFlowDataStore instances.
@param props properties, typically read from a data store configuration file
*/
public DataStore create ( PropList props )
{
    String name = props.getValue ( "Name" );
    String description = props.getValue ( "Description" );
    if ( description == null ) {
        description = "";
    }
    String serviceRootURI = props.getValue ( "ServiceRootURI" );
    String version = props.getValue ( "Version" );
    WaterOneFlowAPI wof = null;
    if ( version.equals("1.0") ) {
        try {
            wof = new WaterOneFlowAPI_1_0(serviceRootURI);
        }
        catch ( Exception e ) {
            throw new RuntimeException ( e );
        }
    }
    else {
        throw new WaterMLVersionNotSupportedException ( "WaterML version is not supported: " + version );
    }
    try {
        return new WaterOneFlowDataStore ( name, description, new URI(serviceRootURI), wof );
    }
    catch ( Exception e ) {
        throw new RuntimeException ( e );
    }
}

}