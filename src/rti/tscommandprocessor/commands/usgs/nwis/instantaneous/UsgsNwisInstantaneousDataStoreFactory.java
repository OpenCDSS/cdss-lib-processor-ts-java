package rti.tscommandprocessor.commands.usgs.nwis.instantaneous;

import java.net.URI;

import RTi.Util.IO.PropList;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreFactory;

/**
 * Factory to instantiate UsgsNwisInstantaneousDataStore instances.
 * @author sam
 *
 */
public class UsgsNwisInstantaneousDataStoreFactory implements DataStoreFactory
{

/**
Create a UsgsNwisInstantaneousDataStore instance.
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
        UsgsNwisInstantaneousDataStore dataStore =
            new UsgsNwisInstantaneousDataStore ( name, description, new URI(serviceRootURI) );
        dataStore.setProperties ( props );
        return dataStore;
    }
    catch ( Exception e ) {
        throw new RuntimeException ( e );
    }
}

}