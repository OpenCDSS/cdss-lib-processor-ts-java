package rti.tscommandprocessor.commands.nrcs.awdb;

import java.net.URI;

import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreFactory;

/**
 * Factory to instantiate NrcsAwdbDataStore instances.
 * @author sam
 *
 */
public class NrcsAwdbDataStoreFactory implements DataStoreFactory
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
        DataStore ds = new NrcsAwdbDataStore ( name, description, new URI(serviceRootURI), props );
        return ds;
    }
    catch ( Exception e ) {
        Message.printWarning(3,"",e);
        throw new RuntimeException ( e );
    }
}

}