package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.Util.IO.PropList;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreFactory;

/**
 * Factory to instantiate ReclamationHDBDataStore instances.
 * @author sam
 *
 */
public class ReclamationHDBDataStoreFactory implements DataStoreFactory
{

/**
Create a ReclamationHDBDataStore instance and open the encapsulated ReclamationHDB_DMI using the specified
properties.
*/
public DataStore create ( PropList props )
{
    String name = props.getValue ( "Name" );
    String description = props.getValue ( "Description" );
    if ( description == null ) {
        description = "";
    }
    String databaseEngine = props.getValue ( "DatabaseEngine" );
    String databaseServer = props.getValue ( "DatabaseServer" );
    String databaseName = props.getValue ( "DatabaseName" );
    String systemLogin = props.getValue ( "SystemLogin" );
    String systemPassword = props.getValue ( "SystemPassword" );
    try {
        ReclamationHDB_DMI dmi = new ReclamationHDB_DMI (
            databaseEngine, // OK if null, will use SQL Server
            databaseServer, // Required
            databaseName, // Required
            -1, // Don't use the port number - use the database name instead
            systemLogin,
            systemPassword );
        // Open the database connection
        dmi.open();
        return new ReclamationHDBDataStore ( name, description, dmi );
    }
    catch ( Exception e ) {
        // TODO SAM 2010-09-02 Wrap the exception because need to move from default Exception
        throw new RuntimeException ( "Error opening database connection", e );
    }
}

}