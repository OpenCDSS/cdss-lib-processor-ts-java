package rti.tscommandprocessor.commands.rccacis;

import java.net.URI;

import RTi.Util.IO.PropList;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreFactory;

/**
 * Factory to instantiate RccAcisDataStore instances.
 * @author sam
 *
 */
public class RccAcisDataStoreFactory implements DataStoreFactory
{

/**
Create an RccAcisDataStore instances.
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
        DataStore ds = new RccAcisDataStore ( name, description, new URI(serviceRootURI) );
        ds.setProperties ( props );
        return ds;
    }
    catch ( Exception e ) {
        throw new RuntimeException ( e );
    }
}

}