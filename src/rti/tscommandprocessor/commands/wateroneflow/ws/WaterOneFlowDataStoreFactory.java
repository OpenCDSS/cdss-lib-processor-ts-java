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
Create a UsgsNwisDailyDataStore instances.
*/
public DataStore create ( PropList props )
{
    String name = props.getValue ( "Name" );
    String description = props.getValue ( "Description" );
    if ( description == null ) {
        description = "";
    }
    String serviceRootURI = props.getValue ( "ServiceRootURI" );
    try {
        return new WaterOneFlowDataStore ( name, description, new URI(serviceRootURI) );
    }
    catch ( Exception e ) {
        throw new RuntimeException ( e );
    }
}

}