package rti.tscommandprocessor.commands.usgs.nwis.groundwater;

import java.net.URI;

import RTi.Util.IO.PropList;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreFactory;

/**
 * Factory to instantiate UsgsNwisGroundwaterDataStore instances.
 * @author sam
 *
 */
public class UsgsNwisGroundwaterDataStoreFactory implements DataStoreFactory
{

/**
Create a UsgsNwisDailyDataStore instance.
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
        UsgsNwisGroundwaterDataStore dataStore =
            new UsgsNwisGroundwaterDataStore ( name, description, new URI(serviceRootURI) );
        dataStore.setProperties ( props );
        return dataStore;
    }
    catch ( Exception e ) {
        throw new RuntimeException ( e );
    }
}

}