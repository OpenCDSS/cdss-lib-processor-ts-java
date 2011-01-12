package rti.tscommandprocessor.commands.usgsnwis;

import java.net.URI;

import RTi.Util.IO.PropList;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreFactory;

/**
Factory to instantiate UsgsNwisDataStore instances.
*/
public class UsgsNwisDataStoreFactory implements DataStoreFactory
{

/**
Create an data store instance.
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
        return new UsgsNwisDataStore ( name, description, new URI(serviceRootURI) );
    }
    catch ( Exception e ) {
        throw new RuntimeException ( e );
    }
}

}