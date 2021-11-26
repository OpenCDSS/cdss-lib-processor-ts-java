// ReclamationHDBDataStore - Data store for Reclamation HDB database.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.reclamationhdb;

import RTi.DMI.AbstractDatabaseDataStore;
import RTi.DMI.DMI;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTime;

/**
Data store for Reclamation HDB database.  This class maintains the database connection information
in a general way.
@author sam
*/
public class ReclamationHDBDataStore extends AbstractDatabaseDataStore
{
    
/**
Construct a data store given a DMI instance, which is assumed to be open.
@param name identifier for the data store
@param description name for the data store
@param dmi DMI instance to use for the data store.
*/
public ReclamationHDBDataStore ( String name, String description, DMI dmi )
{
    setName ( name );
    setDescription ( description );
    setDMI ( dmi );
}

/**
Check the database connection.  Sometimes the connection gets dropped due to timeout from inaction.
A simple, fast query is run and if it fails the connection is re-established.
It is assumed that the DMI instance has been populated with data that can be used for the connection.
Although this method could be called near low-level database statement calls (for example in DMI read/write
methods), for performance it is probably best to call at a higher level before a group of database
statements are executed.
@return true if the connection could be established, false if not.
*/
public boolean checkDatabaseConnection ()
{
	int retries = 5;
	for ( int i = 0; i < retries; i++ ) {
		DMI dmi = getDMI();
		try {
			if ( dmi == null ) {
				// Datastore was never initialized properly when software started:
				// - this is a bigger problem than can be fixed here.
				// - can't set status on DMI since null instance
				return false;
			}
			else {
				ReclamationHDB_DMI rdmi = (ReclamationHDB_DMI)dmi;
				// Will throw an exception if there is an error.
				rdmi.dmiSelect("SELECT * from HDB_STATE");
				// If here the connection is in place and the query was successful.
				return true;
			}
		}
		catch ( Exception e ) {
			// Error running query so try to open the DMI.
			Message.printWarning(3, "", e);
			try {
				dmi.open();
				// If no exception it was successful, but make sure query is OK so go to the top of the loop again.
				setStatus(0);
				DateTime now = new DateTime(DateTime.DATE_CURRENT);
				setStatusMessage("Database connection automatically reopened at " + now );
				continue;
			}
			catch ( Exception e2 ) {
				// Failed to open - try again until max retries is over.
				Message.printWarning(3, "", e);
				continue;
			}
		}
	}
	// Could not establish the connection even with retries.
	setStatusMessage("Unable to open database connection after " + retries + " retries.");
	return false;
}
    
/**
Factory method to construct a data store connection from a properties file.
@param filename name of file containing property strings
*/
/* TODO SAM 2015-03-22 This seems to be not needed - if needed, call the factory.create() method
public static ReclamationHDBDataStore createFromFile ( String filename )
throws IOException, Exception
{
    // Read the properties from the file
    PropList props = new PropList ("");
    props.setPersistentName ( filename );
    props.readPersistent ( false );
    String name = IOUtil.expandPropertyForEnvironment("Name",props.getValue("Name"));
    String description = IOUtil.expandPropertyForEnvironment("Description",props.getValue("Description"));
    String databaseEngine = IOUtil.expandPropertyForEnvironment("DatabaseEngine",props.getValue("DatabaseEngine"));
    String databaseServer = IOUtil.expandPropertyForEnvironment("DatabaseServer",props.getValue("DatabaseServer"));
    String databaseName = IOUtil.expandPropertyForEnvironment("DatabaseName",props.getValue("DatabaseName"));
    String databasePort = IOUtil.expandPropertyForEnvironment("DatabasePort",props.getValue("DatabasePort"));
    String systemLogin = IOUtil.expandPropertyForEnvironment("SystemLogin",props.getValue("SystemLogin"));
    String systemPassword = IOUtil.expandPropertyForEnvironment("SystemPassword",props.getValue("SystemPassword"));
    int port = -1;
    if ( (databasePort != null) && !databasePort.equals("") ) {
        try {
            port = Integer.parseInt(databasePort);
        }
        catch ( NumberFormatException e ) {
            port = -1;
        }
    }
    String keepAliveSql = props.getValue("KeepAliveSQL");
    String keepAliveFrequency = props.getValue("KeepAliveFrequency");
    String tsidStyle = props.getValue("TSIDStyle");
    boolean tsidStyleSDI = true;
    if ( (tsidStyle != null) && tsidStyle.equalsIgnoreCase("CommonName") ) {
        tsidStyleSDI = false;
    }
    String readNHour = props.getValue("ReadNHourEndDateTime");
    boolean readNHourEndDateTime = true;
    if ( (readNHour != null) && readNHour.equalsIgnoreCase("StartDateTimePlusInterval") ) {
        readNHourEndDateTime = false;
    }
    String ConnectTimeout = props.getValue("ConnectTimeout");
    int connectTimeout = 0;
    if ( (ConnectTimeout != null) && !ConnectTimeout.equals("") ) {
        try {
            connectTimeout = Integer.parseInt(ConnectTimeout);
        }
        catch ( Exception e ) {
            connectTimeout = 0;
        }
    }
    String ReadTimeout = props.getValue("ReadTimeout");
    int readTimeout = 0;
    if ( (ReadTimeout != null) && !ReadTimeout.equals("") ) {
        try {
            readTimeout = Integer.parseInt(ReadTimeout);
        }
        catch ( Exception e ) {
            readTimeout = 0;
        }
    }
    
    // Get the properties and create an instance
    ReclamationHDB_DMI dmi = new ReclamationHDB_DMI ( databaseEngine, databaseServer, databaseName, port, systemLogin, systemPassword );
    dmi.setKeepAlive ( keepAliveSql, keepAliveFrequency ); // Needed for remote access to keep connection open
    dmi.setTSIDStyleSDI ( tsidStyleSDI );
    dmi.setReadNHourEndDateTime( readNHourEndDateTime );
    dmi.setLoginTimeout(connectTimeout);
    dmi.setReadTimeout(readTimeout);
    dmi.open();
    ReclamationHDBDataStore ds = new ReclamationHDBDataStore( name, description, dmi );
    return ds;
}
*/

}
