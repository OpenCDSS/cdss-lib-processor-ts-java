package rti.tscommandprocessor.commands.reclamationpisces;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreConnectionUIProvider;
import riverside.datastore.DataStoreFactory;

/**
Factory to instantiate ReclamationPiscesDataStore instances.
@author sam
*/
public class ReclamationPiscesDataStoreFactory implements DataStoreFactory//, DataStoreConnectionUIProvider
{

/**
Create a ReclamationPiscesDataStore instance and open the encapsulated ReclamationPiscesDMI using the specified properties.
@param props datastore configuration properties, such as read from the configuration file
*/
public DataStore create ( PropList props )
{   String routine = getClass().getSimpleName() + ".create";
    String name = props.getValue ( "Name" );
    String description = props.getValue ( "Description" );
    if ( description == null ) {
        description = "";
    }
    String databaseEngine = IOUtil.expandPropertyForEnvironment("DatabaseEngine",props.getValue ( "DatabaseEngine" ));
    String databaseServer = IOUtil.expandPropertyForEnvironment("DatabaseServer",props.getValue ( "DatabaseServer" ));
    String databaseName = IOUtil.expandPropertyForEnvironment("DatabaseName",props.getValue ( "DatabaseName" ));
    String databasePort = IOUtil.expandPropertyForEnvironment("DatabasePort",props.getValue("DatabasePort"));
    String systemLogin = IOUtil.expandPropertyForEnvironment("SystemLogin",props.getValue ( "SystemLogin" ));
    String systemPassword = IOUtil.expandPropertyForEnvironment("SystemPassword",props.getValue ( "SystemPassword" ));
    int port = -1;
    if ( (databasePort != null) && !databasePort.equals("") ) {
        try {
            port = Integer.parseInt(databasePort);
        }
        catch ( NumberFormatException e ) {
            port = -1;
        }
    }
    // Create an initial datastore instance here with null DMI placeholder - it will be recreated below with DMI
    ReclamationPiscesDataStore ds = new ReclamationPiscesDataStore ( name, description, null );
    // Save the properties for later use (e.g., if changing login) but discard the password since private.
    // The password is set below into the DMI and a reopen of the DMI will use that value.
    PropList props2 = new PropList(props);
    props2.unSet("SystemPassword");
    ds.setProperties(props2);
    Message.printStatus(2,"","In factory create, description property is \"" + props2.getValue("Description") +
    	"\" from DS=\"" + ds.getProperty("Description") + "\", all properties:");
    Message.printStatus(2, "", props2.toString());
    ReclamationPiscesDMI dmi = null;
    try {
        dmi = new ReclamationPiscesDMI (
            databaseEngine, // OK if null, will use Oracle
            databaseServer, // Required
            databaseName, // Required
            port,
            systemLogin,
            systemPassword );
        // Set the datastore here so it has a DMI instance, but DMI instance will not be open
        ds = new ReclamationPiscesDataStore ( name, description, dmi );
        // Open the database connection
        dmi.open();
    }
    catch ( Exception e ) {
        // Don't rethrow an exception because want datastore to be created with unopened DMI
        Message.printWarning(3,routine,e);
        ds.setStatus(1);
        ds.setStatusMessage("" + e);
    }
    return ds;
}

/**
Open a connection UI dialog that displays the connection information for the database.
This version is used when a prompt is desired to enter database login credentials at start-up, using properties from a datastore configuration file.
@param props properties read from datastore configuration file
@param frame a JFrame to use as the parent of the editor dialog
*/
//public DataStore openDataStoreConnectionUI ( PropList props, JFrame frame )
//{
//	return new ReclamationPiscesConnectionUI ( this, props, frame ).getDataStore();
//}

/**
Open a connection UI dialog that displays the connection information for the database.
This version is used when (re)connecting to a datastore after initial startup, for example to change users.
@param datastoreList a list of ReclamationHDB datastores that were initially configured but may or may not be active/open.
The user will first pick a datastore to access its properties, and will then enter a new login and password for the database connection.
Properties for the datastores are used in addition to the login and password specified interactively to recreate the database connection.
@param frame a JFrame to use as the parent of the editor dialog
*/
/*
public DataStore openDataStoreConnectionUI ( List<? extends DataStore> datastoreList, JFrame frame )
{
	// TODO SAM 2015-03-22 Need to figure out how to handle the generics mapping - is there a better way?
	List<ReclamationPiscesDataStore> datastoreList2 = new ArrayList<ReclamationPiscesDataStore>();
	for ( DataStore datastore : datastoreList ) {
		datastoreList2.add((ReclamationPiscesDataStore)datastore);
	}
	return new ReclamationHDBConnectionUI ( this, datastoreList2, frame ).getDataStore();
}*/

}