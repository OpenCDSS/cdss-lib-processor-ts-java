// ReclamationPiscesDataStoreFactory - Factory to instantiate ReclamationPiscesDataStore instances.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package rti.tscommandprocessor.commands.reclamationpisces;

import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreFactory;

/**
Factory to instantiate ReclamationPiscesDataStore instances.
*/
public class ReclamationPiscesDataStoreFactory implements DataStoreFactory//, DataStoreConnectionUIProvider
{

/**
Create a ReclamationPiscesDataStore instance and open the encapsulated ReclamationPiscesDMI using the specified properties.
@param props datastore configuration properties, such as read from the configuration file
*/
public DataStore create ( PropList props ) {
    String routine = getClass().getSimpleName() + ".create";
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
    // Create an initial datastore instance here with null DMI placeholder - it will be recreated below with DMI.
    ReclamationPiscesDataStore ds = new ReclamationPiscesDataStore ( name, description, null );
    // Save the properties for later use (e.g., if changing login) but discard the password since private.
    // The password is set below into the DMI and a reopen of the DMI will use that value.
    PropList props2 = new PropList(props);
    props2.unSet("SystemPassword");
    Message.printStatus(2,routine,"In factory create, description property is \"" + props2.getValue("Description") +
    	"\" from DS=\"" + ds.getProperty("Description") + "\", all properties:");
    Message.printStatus(2, routine, props2.toString());
    ReclamationPiscesDMI dmi = null;
    try {
        dmi = new ReclamationPiscesDMI (
            databaseEngine, // OK if null, will use MySql.
            databaseServer, // Required.
            databaseName, // Required.
            port,
            systemLogin,
            systemPassword );
        // Set the datastore here so it has a DMI instance, but DMI instance will not be open.
        ds = new ReclamationPiscesDataStore ( name, description, dmi );
        ds.setProperties(props2);
        // Open the database connection.
        dmi.open();
    }
    catch ( Exception e ) {
        // Don't rethrow an exception because want datastore to be created with unopened DMI.
        Message.printWarning(3,routine,e);
        ds.setStatus(1);
        ds.setStatusMessage("" + e);
    }
    return ds;
}

/**
Open a connection UI dialog that displays the connection information for the database.
This version is used when a prompt is desired to enter database login credentials at start-up,
using properties from a datastore configuration file.
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
@param datastoreList a list of Pisces datastores that were initially configured but may or may not be active/open.
The user will first pick a datastore to access its properties,
and will then enter a new login and password for the database connection.
Properties for the datastores are used in addition to the login and password specified
interactively to recreate the database connection.
@param frame a JFrame to use as the parent of the editor dialog
*/
/*
public DataStore openDataStoreConnectionUI ( List<? extends DataStore> datastoreList, JFrame frame ) {
	// TODO SAM 2015-03-22 Need to figure out how to handle the generics mapping - is there a better way?
	List<ReclamationPiscesDataStore> datastoreList2 = new ArrayList<ReclamationPiscesDataStore>();
	for ( DataStore datastore : datastoreList ) {
		datastoreList2.add((ReclamationPiscesDataStore)datastore);
	}
	return new ReclamationPiscesConnectionUI ( this, datastoreList2, frame ).getDataStore();
}*/

}